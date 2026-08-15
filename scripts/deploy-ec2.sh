#!/usr/bin/env bash
set -Eeuo pipefail

readonly EXPECTED_POSTGRES_IMAGE='postgres:18.4-alpine@sha256:9a8afca54e7861fd90fab5fdf4c42477a6b1cb7d293595148e674e0a3181de15'
readonly COMPOSE_PROJECT='marshmello-was'
readonly COMMAND_TIMEOUT='120s'

die() {
  local exit_code="$1"
  shift
  printf 'deploy-ec2: %s\n' "$*" >&2
  exit "$exit_code"
}

is_release_id() {
  [[ "$1" =~ ^[0-9a-f]{40}$ ]]
}

is_app_image() {
  [[ "$1" =~ ^ghcr\.io/[a-z0-9]+([._-][a-z0-9]+)*/[a-z0-9]+([._-][a-z0-9]+)*@sha256:[0-9a-f]{64}$ ]]
}

is_port() {
  [[ "$1" =~ ^[0-9]+$ ]] && (( 10#$1 >= 1 && 10#$1 <= 65535 ))
}

owner_uid() {
  stat -c '%u' -- "$1"
}

require_owned_directory() {
  local path="$1"
  local expected_mode="${2:-}"
  local mode
  [[ -d "$path" && ! -L "$path" ]] || die 2 "unsafe or missing directory: $path"
  [[ "$(owner_uid "$path")" == "$DEPLOY_UID" ]] || die 2 "directory is not owned by deploy user: $path"
  mode="$(stat -c '%a' -- "$path")"
  if [[ -n "$expected_mode" ]]; then
    [[ "$mode" == "$expected_mode" ]] || die 2 "directory mode must be $expected_mode: $path"
  else
    (( (8#${mode: -3} & 8#022) == 0 )) || die 2 "directory is group/world writable: $path"
  fi
}

sha256_file() {
  sha256sum -- "$1" | awk '{print $1}'
}

canonical_release_bundle() {
  local release_id="$1"
  local bundle="$2"
  local expected="$RELEASES_DIR/$release_id/compose.yaml"
  local canonical mode

  [[ "$bundle" == "$expected" ]] || return 34
  [[ -e "$bundle" ]] || return 31
  [[ -f "$bundle" && ! -L "$bundle" ]] || return 32
  [[ "$(owner_uid "$bundle")" == "$DEPLOY_UID" ]] || return 32
  mode="$(stat -c '%a' -- "$bundle")"
  (( (8#${mode: -3} & 8#022) == 0 )) || return 32
  canonical="$(realpath -e -- "$bundle")" || return 31
  [[ "$canonical" == "$expected" ]] || return 34
  [[ "$canonical" == "$RELEASES_CANONICAL/"* ]] || return 34
  printf '%s\n' "$canonical"
}

STATE_RELEASE_ID=''
STATE_BUNDLE_PATH=''
STATE_APP_IMAGE=''
STATE_COMPOSE_SHA256=''
STATE_APP_PORT=''
STATE_ERROR=''
validate_state_record() {
  local record="$1"
  local line key value count=0 canonical actual_hash
  local seen_release=0 seen_bundle=0 seen_image=0 seen_hash=0 seen_port=0
  STATE_RELEASE_ID=''
  STATE_BUNDLE_PATH=''
  STATE_APP_IMAGE=''
  STATE_COMPOSE_SHA256=''
  STATE_APP_PORT=''
  STATE_ERROR=''

  if [[ ! -e "$record" ]]; then
    STATE_ERROR='missing-record'
    return 31
  fi
  if [[ ! -f "$record" || -L "$record" || "$(owner_uid "$record")" != "$DEPLOY_UID" || "$(stat -c '%a' -- "$record")" != '600' ]]; then
    STATE_ERROR='invalid-record-file'
    return 32
  fi

  while IFS= read -r line || [[ -n "$line" ]]; do
    ((count += 1))
    [[ "$line" == *=* ]] || { STATE_ERROR='invalid-schema'; return 32; }
    key="${line%%=*}"
    value="${line#*=}"
    case "$key" in
      RELEASE_ID)
        (( seen_release == 0 )) || { STATE_ERROR='duplicate-release-id'; return 32; }
        seen_release=1; STATE_RELEASE_ID="$value" ;;
      BUNDLE_PATH)
        (( seen_bundle == 0 )) || { STATE_ERROR='duplicate-bundle-path'; return 32; }
        seen_bundle=1; STATE_BUNDLE_PATH="$value" ;;
      APP_IMAGE)
        (( seen_image == 0 )) || { STATE_ERROR='duplicate-app-image'; return 32; }
        seen_image=1; STATE_APP_IMAGE="$value" ;;
      COMPOSE_SHA256)
        (( seen_hash == 0 )) || { STATE_ERROR='duplicate-compose-hash'; return 32; }
        seen_hash=1; STATE_COMPOSE_SHA256="$value" ;;
      APP_PORT)
        (( seen_port == 0 )) || { STATE_ERROR='duplicate-app-port'; return 32; }
        seen_port=1; STATE_APP_PORT="$value" ;;
      *) STATE_ERROR='unknown-field'; return 32 ;;
    esac
  done < "$record"

  [[ "$count" == 5 && "$seen_release$seen_bundle$seen_image$seen_hash$seen_port" == '11111' ]] || { STATE_ERROR='invalid-field-count'; return 32; }
  is_release_id "$STATE_RELEASE_ID" || { STATE_ERROR='invalid-release-id'; return 32; }
  is_app_image "$STATE_APP_IMAGE" || { STATE_ERROR='invalid-app-image'; return 32; }
  [[ "$STATE_COMPOSE_SHA256" =~ ^[0-9a-f]{64}$ ]] || { STATE_ERROR='invalid-compose-hash'; return 32; }
  is_port "$STATE_APP_PORT" || { STATE_ERROR='invalid-prior-port'; return 35; }

  canonical="$(canonical_release_bundle "$STATE_RELEASE_ID" "$STATE_BUNDLE_PATH")"
  case "$?" in
    0) ;;
    31) STATE_ERROR='missing-prior-bundle'; return 31 ;;
    34) STATE_ERROR='out-of-root-prior-bundle'; return 34 ;;
    *) STATE_ERROR='invalid-prior-bundle'; return 32 ;;
  esac
  STATE_BUNDLE_PATH="$canonical"
  actual_hash="$(sha256_file "$STATE_BUNDLE_PATH")" || { STATE_ERROR='compose-hash-read-failed'; return 32; }
  [[ "$actual_hash" == "$STATE_COMPOSE_SHA256" ]] || { STATE_ERROR='tampered-prior-bundle'; return 33; }
}

write_state_record() {
  local destination="$1"
  local release_id="$2"
  local bundle="$3"
  local image="$4"
  local compose_hash="$5"
  local port="$6"
  local temporary

  temporary="$(mktemp "$STATE_DIR/.record.XXXXXX")"
  chmod 600 -- "$temporary"
  printf 'RELEASE_ID=%s\nBUNDLE_PATH=%s\nAPP_IMAGE=%s\nCOMPOSE_SHA256=%s\nAPP_PORT=%s\n' \
    "$release_id" "$bundle" "$image" "$compose_hash" "$port" > "$temporary"
  mv -f -- "$temporary" "$destination"
}

compose() {
  timeout --foreground "$COMMAND_TIMEOUT" docker compose -p "$COMPOSE_PROJECT" -f "$1" "${@:2}"
}

VERIFY_ERROR=''
verify_app() {
  local bundle="$1"
  local image="$2"
  local release_id="$3"
  local port="$4"
  local cid_output cid expected_image_id actual_image_id build_id status app_binding http_code

  VERIFY_ERROR=''
  cid_output="$(compose "$bundle" ps -q app)" || { VERIFY_ERROR='cannot-resolve-app-container'; return 1; }
  [[ -n "$cid_output" && "$cid_output" != *$'\n'* && "$cid_output" != *[$'\t\r ']* ]] || { VERIFY_ERROR='app-container-id-is-not-exactly-one'; return 1; }
  cid="$cid_output"
  expected_image_id="$(timeout --foreground "$COMMAND_TIMEOUT" docker image inspect --format '{{.Id}}' "$image")" || { VERIFY_ERROR='requested-image-not-present'; return 1; }
  actual_image_id="$(timeout --foreground "$COMMAND_TIMEOUT" docker inspect --format '{{.Image}}' "$cid")" || { VERIFY_ERROR='cannot-inspect-app-image'; return 1; }
  [[ -n "$expected_image_id" && "$actual_image_id" == "$expected_image_id" ]] || { VERIFY_ERROR='app-image-id-mismatch'; return 1; }
  build_id="$(timeout --foreground "$COMMAND_TIMEOUT" docker inspect --format '{{ index .Config.Labels "BUILD_ID" }}' "$cid")" || { VERIFY_ERROR='cannot-inspect-build-id'; return 1; }
  [[ "$build_id" == "$release_id" ]] || { VERIFY_ERROR='build-id-label-mismatch'; return 1; }
  status="$(timeout --foreground "$COMMAND_TIMEOUT" docker inspect --format '{{.State.Status}}' "$cid")" || { VERIFY_ERROR='cannot-inspect-app-status'; return 1; }
  [[ "$status" == 'running' ]] || { VERIFY_ERROR='app-is-not-running'; return 1; }
  app_binding="$(timeout --foreground "$COMMAND_TIMEOUT" docker inspect --format '{{with index .NetworkSettings.Ports "8080/tcp"}}{{with index . 0}}{{.HostIp}}:{{.HostPort}}{{end}}{{end}}' "$cid")" || { VERIFY_ERROR='cannot-inspect-app-port-binding'; return 1; }
  [[ "$app_binding" == "0.0.0.0:$port" ]] || { VERIFY_ERROR='app-port-mapping-mismatch'; return 1; }
  http_code="$(timeout --foreground "$COMMAND_TIMEOUT" curl --silent --show-error --output /dev/null --write-out '%{http_code}' --max-time 10 "http://127.0.0.1:$port/actuator/health")" || { VERIFY_ERROR='readiness-request-failed'; return 1; }
  [[ "$http_code" == '200' ]] || { VERIFY_ERROR="readiness-status-is-$http_code"; return 1; }
}

rollback() {
  local failure="$1"
  [[ "$HAVE_PRIOR" == 1 ]] || die 30 "new release failed ($failure); no previous release is available"
  [[ "$(sha256_file "$PRIOR_BUNDLE_PATH")" == "$PRIOR_COMPOSE_SHA256" ]] || die 36 "new release failed ($failure); rollback bundle changed before recovery"

  export APP_IMAGE="$PRIOR_APP_IMAGE"
  export BUILD_ID="$PRIOR_RELEASE_ID"
  export APP_PORT="$PRIOR_APP_PORT"
  export POSTGRES_IMAGE POSTGRES_PASSWORD_FILE APP_UID APP_GID
  if ! compose "$PRIOR_BUNDLE_PATH" up -d --wait --no-deps app; then
    die 36 "new release failed ($failure); rollback command failed"
  fi
  if ! verify_app "$PRIOR_BUNDLE_PATH" "$PRIOR_APP_IMAGE" "$PRIOR_RELEASE_ID" "$PRIOR_APP_PORT"; then
    die 36 "new release failed ($failure); rollback verification failed: $VERIFY_ERROR"
  fi
  [[ "$(sha256_file "$PRIOR_BUNDLE_PATH")" == "$PRIOR_COMPOSE_SHA256" ]] || die 36 "new release failed ($failure); rollback bundle changed during recovery"
  die 20 "new release failed ($failure); prior app release restored"
}

RELEASE_ID="${RELEASE_ID:-}"
APP_IMAGE="${APP_IMAGE:-}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-$EXPECTED_POSTGRES_IMAGE}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/marshmello-was}"
POSTGRES_PASSWORD_FILE="${POSTGRES_PASSWORD_FILE:-/opt/marshmello-was/secrets/postgres_password}"
APP_PORT="${APP_PORT:-8080}"
DEPLOY_UID="$(id -u)"
DEPLOY_GID="$(id -g)"
OIDC_CLIENT_ID="${OIDC_CLIENT_ID:-}"
OIDC_CLIENT_SECRET="${OIDC_CLIENT_SECRET:-}"
AWS_REGION="${AWS_REGION:-}"
AWS_S3_BUCKET="${AWS_S3_BUCKET:-}"

[[ "$DEPLOY_UID" != 0 ]] || die 2 'deployment must run as a non-root deploy user'
APP_UID="$DEPLOY_UID"
APP_GID="$DEPLOY_GID"

[[ -n "$RELEASE_ID" ]] || die 2 'RELEASE_ID is required'
is_release_id "$RELEASE_ID" || die 2 'RELEASE_ID must be exactly 40 lowercase hexadecimal characters'
[[ -n "$APP_IMAGE" ]] || die 2 'APP_IMAGE is required'
is_app_image "$APP_IMAGE" || die 2 'APP_IMAGE must be an immutable lowercase ghcr.io owner/repo sha256 digest reference'
[[ -n "$OIDC_CLIENT_ID" ]] || die 2 'OIDC_CLIENT_ID is required'
[[ -n "$OIDC_CLIENT_SECRET" ]] || die 2 'OIDC_CLIENT_SECRET is required'
[[ "$AWS_REGION" =~ ^[a-z0-9-]+$ ]] || die 2 'AWS_REGION must contain only lowercase letters, digits, and hyphens'
[[ "$AWS_S3_BUCKET" =~ ^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$ ]] || die 2 'AWS_S3_BUCKET must be a valid lowercase S3 bucket name'
[[ "$POSTGRES_IMAGE" == "$EXPECTED_POSTGRES_IMAGE" ]] || die 2 'POSTGRES_IMAGE does not match the fixed deployment digest'
is_port "$APP_PORT" || die 2 'APP_PORT must be an integer from 1 through 65535'
[[ "$DEPLOY_ROOT" == /* ]] || die 2 'DEPLOY_ROOT must be absolute'
[[ "$POSTGRES_PASSWORD_FILE" == /* ]] || die 2 'POSTGRES_PASSWORD_FILE must be absolute'

require_owned_directory "$DEPLOY_ROOT"
DEPLOY_ROOT_CANONICAL="$(realpath -e -- "$DEPLOY_ROOT")"
[[ "$DEPLOY_ROOT_CANONICAL" == "${DEPLOY_ROOT%/}" ]] || die 2 'deploy root must not traverse symlinks'
DEPLOY_ROOT="$DEPLOY_ROOT_CANONICAL"
RELEASES_DIR="$DEPLOY_ROOT/releases"
require_owned_directory "$RELEASES_DIR"
RELEASES_CANONICAL="$(realpath -e -- "$RELEASES_DIR")"
[[ "$RELEASES_CANONICAL" == "$RELEASES_DIR" ]] || die 2 'releases directory must not traverse symlinks'

[[ -f "$POSTGRES_PASSWORD_FILE" && ! -L "$POSTGRES_PASSWORD_FILE" ]] || die 2 'postgres password file must be a regular non-symlink file'
[[ "$(owner_uid "$POSTGRES_PASSWORD_FILE")" == "$DEPLOY_UID" ]] || die 2 'postgres password file is not owned by deploy user'
[[ "$(stat -c '%a' -- "$POSTGRES_PASSWORD_FILE")" == '600' ]] || die 2 'postgres password file mode must be 0600'

NEW_BUNDLE_PATH="$RELEASES_DIR/$RELEASE_ID/compose.yaml"
NEW_BUNDLE_PATH="$(canonical_release_bundle "$RELEASE_ID" "$NEW_BUNDLE_PATH")" || die 2 'new release bundle is missing, unsafe, or outside releases'
NEW_COMPOSE_SHA256="$(sha256_file "$NEW_BUNDLE_PATH")"
[[ "$NEW_COMPOSE_SHA256" =~ ^[0-9a-f]{64}$ ]] || die 2 'could not hash new release compose bundle'

umask 077
STATE_DIR="$DEPLOY_ROOT/state"
if [[ ! -e "$STATE_DIR" ]]; then
  mkdir -- "$STATE_DIR"
  chmod 700 -- "$STATE_DIR"
fi
require_owned_directory "$STATE_DIR" 700
CURRENT_STATE="$STATE_DIR/current"
PREVIOUS_STATE="$STATE_DIR/previous"
LOCK_FILE="$DEPLOY_ROOT/deploy.lock"
[[ ! -e "$LOCK_FILE" || ( -f "$LOCK_FILE" && ! -L "$LOCK_FILE" ) ]] || die 4 'deploy lock is unsafe'
exec 9>"$LOCK_FILE"
[[ "$(owner_uid "$LOCK_FILE")" == "$DEPLOY_UID" ]] || die 4 'deploy lock is not owned by deploy user'
chmod 600 -- "$LOCK_FILE"
flock -n 9 || die 4 'another deployment holds the deploy lock'

HAVE_PRIOR=0
PRIOR_RELEASE_ID=''
PRIOR_BUNDLE_PATH=''
PRIOR_APP_IMAGE=''
PRIOR_COMPOSE_SHA256=''
PRIOR_APP_PORT=''
if [[ -e "$CURRENT_STATE" ]]; then
  validate_state_record "$CURRENT_STATE" || {
    rc="$?"
    die "$rc" "current release record rejected: $STATE_ERROR"
  }
  HAVE_PRIOR=1
  PRIOR_RELEASE_ID="$STATE_RELEASE_ID"
  PRIOR_BUNDLE_PATH="$STATE_BUNDLE_PATH"
  PRIOR_APP_IMAGE="$STATE_APP_IMAGE"
  PRIOR_COMPOSE_SHA256="$STATE_COMPOSE_SHA256"
  PRIOR_APP_PORT="$STATE_APP_PORT"
  if [[ -e "$PREVIOUS_STATE" ]]; then
    validate_state_record "$PREVIOUS_STATE" || {
      rc="$?"
      die "$rc" "previous release record rejected: $STATE_ERROR"
    }
  fi
  [[ "$RELEASE_ID" != "$PRIOR_RELEASE_ID" ]] || die 3 "release $RELEASE_ID is already current; stale no-op rejected"
elif [[ -e "$PREVIOUS_STATE" ]]; then
  die 32 'previous record exists without a current record'
fi

export APP_IMAGE POSTGRES_IMAGE POSTGRES_PASSWORD_FILE APP_PORT APP_UID APP_GID
export OIDC_CLIENT_ID OIDC_CLIENT_SECRET AWS_REGION AWS_S3_BUCKET
export BUILD_ID="$RELEASE_ID"

compose "$NEW_BUNDLE_PATH" pull || die 20 'new release image pull failed'
if ! compose "$NEW_BUNDLE_PATH" up -d --wait; then
  rollback 'compose-up-or-wait-failed'
fi
if ! verify_app "$NEW_BUNDLE_PATH" "$APP_IMAGE" "$RELEASE_ID" "$APP_PORT"; then
  rollback "$VERIFY_ERROR"
fi

[[ "$(sha256_file "$NEW_BUNDLE_PATH")" == "$NEW_COMPOSE_SHA256" ]] || rollback 'new-bundle-changed-before-commit'
if (( HAVE_PRIOR == 1 )); then
  write_state_record "$PREVIOUS_STATE" "$PRIOR_RELEASE_ID" "$PRIOR_BUNDLE_PATH" "$PRIOR_APP_IMAGE" "$PRIOR_COMPOSE_SHA256" "$PRIOR_APP_PORT"
fi
write_state_record "$CURRENT_STATE" "$RELEASE_ID" "$NEW_BUNDLE_PATH" "$APP_IMAGE" "$NEW_COMPOSE_SHA256" "$APP_PORT"
printf 'deploy-ec2: deployed release %s on 127.0.0.1:%s\n' "$RELEASE_ID" "$APP_PORT"
