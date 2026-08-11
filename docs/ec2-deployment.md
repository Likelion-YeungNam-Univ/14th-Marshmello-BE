# EC2 deployment runbook

This is the operator contract for the current SSH-driven deployment. It has not
been run against a real EC2 host. The supported target is Amazon Linux 2023 on
an x86_64 (amd64) EC2 instance. The published application image must be a
linux/amd64 image in private GHCR.

## Host and access prerequisites

* Install Docker Engine and the Compose v2 plugin from the Amazon Linux 2023
  package/vendor source. The installed client must support `docker compose up
  --wait`; verify with `docker version` and `docker compose version`.
* Create a dedicated deploy user, add it to the Docker group (or equivalent
  Docker access), and run the deployment as that user. Automation must not
  depend on a sudo Docker wrapper; an administrator may use one-time package
  administration, but the deploy command itself has no sudo requirement.
* Require at least 10 GiB free on the filesystem containing the deployment
  root: `df -BG /opt` (or the filesystem selected by `DEPLOY_ROOT`).
* Provision private GHCR pull credentials out of band on the host (for example,
  a short-lived `docker login` performed by an administrator). Do not put a
  registry token in a release bundle, this document, a workflow, or state.
* Security groups and host firewalls must allow SSH only from approved
  administrator/runner source CIDRs. Use explicit allowlists and verified host
  keys; never allow the wildcard source `0.0.0.0/0`. The application is bound
  to loopback only and PostgreSQL has no published host port.

## On-disk contract and secret provisioning

`DEPLOY_ROOT` defaults to `/opt/marshmello-was`. Before the first deployment,
the deploy user (or an administrator preparing the directories) must create:

```
/opt/marshmello-was/
  releases/<40-lowercase-hex-release-id>/compose.yaml
  releases/<40-lowercase-hex-release-id>/deploy-ec2.sh
  state/current                 # created by the script
  state/previous                # created when a prior release exists
  secrets/postgres_password
```

Release directories are immutable, retained, and never edited in place. Each
retained release contains the exact `compose.yaml` bundle and the deployment
script used to deploy it. The script canonicalizes and accepts only
`$DEPLOY_ROOT/releases/$RELEASE_ID/compose.yaml`; bundles must be regular files,
owned by the deploy user, and not group/world writable. The root and `releases`
directories must be owned by the deploy user and not traverse symlinks.

Set `state` and `secrets` ownership to the deploy user and mode `0700`. Create
`secrets/postgres_password` as a regular, non-symlink file owned by the deploy
user with mode `0600`; have an operator enter its value through a protected
out-of-band method. The password is never committed and never copied into a
release. The Compose secret is mounted in the PostgreSQL container as
`/run/secrets/postgres_password`.

## First manual bundle and deployment

The following is a simulated/manual sequence. Replace every angle-bracket
placeholder with an operator-selected value; do not paste credentials into the
shell history. Run the final command as the deploy user with Docker access.

```bash
export DEPLOY_ROOT=/opt/marshmello-was
export RELEASE_ID='<40-lowercase-hex-release-id>'
export APP_IMAGE='ghcr.io/<owner>/<repo>@sha256:<64-lowercase-hex-digest>'
export POSTGRES_IMAGE='postgres:18.4-alpine@sha256:9a8afca54e7861fd90fab5fdf4c42477a6b1cb7d293595148e674e0a3181de15'
export POSTGRES_PASSWORD_FILE=/opt/marshmello-was/secrets/postgres_password
export APP_PORT=8080
export POSTGRES_DB='<database-name>'
export POSTGRES_USER='<database-user>'
export COMPOSE_SOURCE='<path-to-compose.yaml>'
export DEPLOY_SCRIPT_SOURCE='<path-to-deploy-ec2.sh>'

install -d -m 0700 "$DEPLOY_ROOT"/state "$DEPLOY_ROOT"/secrets "$DEPLOY_ROOT/releases/$RELEASE_ID"
install -m 0600 /dev/null "$POSTGRES_PASSWORD_FILE"
# Provision the password out of band; do not echo or commit it.
install -m 0640 "$COMPOSE_SOURCE" "$DEPLOY_ROOT/releases/$RELEASE_ID/compose.yaml"
install -m 0750 "$DEPLOY_SCRIPT_SOURCE" "$DEPLOY_ROOT/releases/$RELEASE_ID/deploy-ec2.sh"

"$DEPLOY_ROOT/releases/$RELEASE_ID/deploy-ec2.sh"
```

The script interface is exact: `RELEASE_ID` is required and exactly 40
lowercase hexadecimal characters; `APP_IMAGE` is required and must match
`ghcr.io/<owner>/<repo>@sha256:<64-lowercase-hex-digest>`; `POSTGRES_IMAGE` is
fixed to the digest shown above; `DEPLOY_ROOT` defaults to
`/opt/marshmello-was`; `POSTGRES_PASSWORD_FILE` defaults to
`/opt/marshmello-was/secrets/postgres_password`; and `APP_PORT` defaults to
`8080` and must be an integer from 1 through 65535. `POSTGRES_DB` and
`POSTGRES_USER` are required by `compose.yaml` and have no script defaults.
The script exports `BUILD_ID=$RELEASE_ID` for Compose interpolation and uses
the stable project name `marshmello-was`.

## Verification and state

Success means the script reports the release and loopback port, and all of the
following checks pass:

```bash
docker compose -p marshmello-was -f "$DEPLOY_ROOT/releases/$RELEASE_ID/compose.yaml" ps
docker inspect --format '{{.State.Status}}' "$(docker compose -p marshmello-was -f "$DEPLOY_ROOT/releases/$RELEASE_ID/compose.yaml" ps -q app)"
curl --fail --silent --show-error --max-time 10 "http://127.0.0.1:$APP_PORT/" -o /dev/null -w '%{http_code}'
```

The app container must be running, its image config ID must equal the ID for
the requested immutable `APP_IMAGE`, its `BUILD_ID` label must equal
`RELEASE_ID`, and its only host mapping must be exactly
`127.0.0.1:$APP_PORT -> 8080/tcp`. The readiness probe is expected to return
HTTP `404` (the application has no `/` route). PostgreSQL remains unpublished.

`state/current` and, after a successful upgrade, `state/previous` are regular
deploy-user-owned mode `0600` files with exactly these five lines (no extras and
never sourced as shell code):

```
RELEASE_ID=<40-lowercase-hex-release-id>
BUNDLE_PATH=/opt/marshmello-was/releases/<same-release-id>/compose.yaml
APP_IMAGE=ghcr.io/<owner>/<repo>@sha256:<64-lowercase-hex-digest>
COMPOSE_SHA256=<64-lowercase-hex-sha256>
APP_PORT=<1-65535>
```

The script validates ownership, modes, release path containment, bundle hash,
image digest, release ID, and port before using either record.

## Failure and app-only rollback

The deployment takes a lock at `$DEPLOY_ROOT/deploy.lock`, pulls the new bundle,
then runs `docker compose ... up -d --wait`. A failed pull exits with the
reported new-release error. If new-app startup, readiness, image identity,
`BUILD_ID`, port mapping, or bundle-integrity verification fails and a valid
`state/current` exists, the script restores the recorded prior bundle and
prior digest/`BUILD_ID`/`APP_PORT` with `up -d --no-deps app`, then repeats the
identity, exact loopback-port, hash, and HTTP-404 checks. This is app-only
recovery: PostgreSQL is never rolled back, recreated, or deleted by this path.

Without a valid prior bundle/state, the new release failure is terminal. If
rollback itself fails, the script reports the rollback failure distinctly; do
not tear down the Compose project or delete state. Investigate the exact failure,
preserve both retained bundles, and use the operator procedures below for the
database and storage.

## Database, storage, and disaster recovery boundaries

Application rollback does not roll back schema or data. Database upgrades,
PostgreSQL credential rotation, and any migration are explicit, reviewed
operator procedures with a tested restore plan. Schedule EBS/volume snapshots
or another host-level backup for the volume containing `DEPLOY_ROOT`; test
restore and document retention. The Compose named volume alone is not EC2/EBS
durability and is not a disaster-recovery plan. Volume restore, instance
replacement, and disaster recovery are separate operator runbooks.

## GitHub activation checklist (future workflow use)

Before enabling automation, confirm:

1. The repository and GitHub environment are the intended production targets.
2. The runner has a verified EC2 host key and SSH source allowlisting is in
   place; no wildcard ingress is permitted.
3. SSH host/user/key material and registry credentials are configured as
   GitHub secrets, while non-secret image coordinates, approved ports, and the
   lowercase shell-safe `POSTGRES_DB` and `POSTGRES_USER` identifiers are
   GitHub variables. No password value is committed.
4. The GHCR application image is immutable and published for `linux/amd64`.
5. An operator has completed and recorded one successful manual deployment and
   its verification before activating a workflow.

## Explicit future scope

Publishing an arm64 image, adding public ingress/TLS or a reverse proxy, and
performing validation on a real EC2 host are future work. This document does
not claim that any EC2 deployment has occurred.
