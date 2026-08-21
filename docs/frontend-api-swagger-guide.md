<!-- markdownlint-disable MD013 -->

# 프론트엔드용 Swagger/OpenAPI 읽기 가이드

이 문서는 Marshmello WAS의 자동 생성 API 문서를 **탐색하고 검증하는 방법**을 설명합니다. Swagger 화면은 편리한 계약 탐색기이지만, 프론트엔드 구현의 최종 근거는 아닙니다. 실제 요청·응답과 권한 동작은 컨트롤러, DTO, 보안 설정, 그리고 동작 테스트를 함께 확인해야 합니다.

## 대상 독자와 목적

- 대상 독자: 이 서버와 연동하는 프론트엔드 개발자
- 목적: Swagger UI와 OpenAPI JSON을 열고, 각 HTTP operation의 입력·출력 구조를 안전하게 읽는 방법을 익히기
- 범위: 이 장은 Swagger/OpenAPI 기초와 이 저장소의 문서 노출 계약을 다룹니다. 세션, CSRF, CORS, endpoint별 연동 예제는 뒤의 장에서 다룰 주제입니다.

## 목차

1. [무엇을 계약으로 볼 것인가](#무엇을-계약으로-볼-것인가)
2. [문서 열기와 노출 범위](#문서-열기와-노출-범위)
3. [OpenAPI 화면 읽는 순서](#openapi-화면-읽는-순서)
4. [Try it out의 안전한 사용](#try-it-out의-안전한-사용)
5. [Swagger가 말하지 않는 것](#swagger가-말하지-않는-것)
6. [Swagger에 보이는 endpoint 지도](#swagger에-보이는-endpoint-지도)
7. [대표 요청 예제](#대표-요청-예제)
8. [브라우저 세션과 OIDC 로그인](#브라우저-세션과-oidc-로그인)
9. [CSRF와 fetch 공통 처리](#csrf와-fetch-공통-처리)
10. [CORS와 로그아웃 경계](#cors와-로그아웃-경계)
11. [상태 코드와 오류 응답 읽기](#상태-코드와-오류-응답-읽기)
12. [유지보수 체크리스트](#유지보수-체크리스트)

## 무엇을 계약으로 볼 것인가

같은 정보를 여러 곳에서 보게 되면 다음 우선순위를 따릅니다.

1. **실제 동작 테스트와 실행 중 HTTP 응답**: 상태 코드, 접근 가능 여부, 문서가 켜지고 꺼지는 조건을 검증합니다.
2. **컨트롤러·DTO·보안/CORS 설정**: HTTP method/path, 입력 형태, 권한 및 보안 경계를 확인합니다.
3. **OpenAPI JSON과 Swagger UI**: 위 구현에서 자동으로 파생된 탐색용 표현을 확인합니다.

따라서 Swagger에 보이는 설명·태그·응답 후보만으로 미확인 계약을 확정하지 마세요. 특히 자동 생성 도구가 추론한 metadata는 탐색을 돕는 관찰값이며, 컨트롤러와 테스트로 검증되기 전에는 규범적 계약이 아닙니다.

이 저장소가 명시적으로 설정한 OpenAPI 정보는 제목 `Marshmello WAS API`, 버전 `v1`, 설명뿐입니다. SpringDoc 의존성으로 문서가 생성되며, 기본 경로로 관찰되는 UI/JSON 주소와 컨트롤러에서 자동 파생되는 operation·schema는 별도 설정 항목이 아닙니다.

## 문서 열기와 노출 범위

개발 서버에서 다음 주소를 엽니다.

| 용도 | 주소 | 검증된 관찰 |
| --- | --- | --- |
| Swagger UI | `/swagger-ui/index.html` | 활성화된 테스트에서 HTTP `200` HTML |
| OpenAPI JSON | `/v3/api-docs` | 활성화된 테스트에서 HTTP `200` JSON |

`SWAGGER_ENABLED` 환경 변수는 두 SpringDoc 기능의 공통 스위치입니다. 값을 주지 않으면 설정의 기본값은 `true`입니다. `false`이면 API JSON과 Swagger UI 모두 `404`가 됩니다. 잘못된 boolean 값은 애플리케이션 시작 실패로 이어질 수 있으므로 배포 환경에서는 boolean 값만 사용하세요.

문서에 포함될 controller mapping의 경계는 `springdoc.paths-to-match=/api/**`입니다. 즉 `/api/**`가 아닌 mapping은 OpenAPI 경로 목록에 포함된다고 가정하면 안 됩니다. 동작 테스트는 `/api/swagger-probe`는 보이고 `/internal/swagger-probe`는 보이지 않음을 확인합니다. 이 UI/JSON 주소는 현재 설정에 명시한 custom path가 아니라 SpringDoc의 테스트로 확인된 기본 노출 경로입니다.

## OpenAPI 화면 읽는 순서

Swagger UI에서 endpoint 하나를 열 때는 아래 순서로 읽습니다.

1. **operation**: HTTP method와 path의 조합입니다. 예를 들어 `GET /api/example/{id}`와 `POST /api/example`는 서로 다른 operation입니다.
2. **parameters**: `in: path`는 URL 경로에 반드시 채우는 값이고, `in: query`는 `?key=value` 형식의 조회 조건입니다. 이름·필수 여부·형식을 확인합니다.
3. **requestBody**: 본문이 필요한 operation에 표시됩니다. JSON인지 multipart인지, required 여부와 content type 후보를 확인한 뒤 컨트롤러 선언도 대조합니다.
4. **responses**: 상태 코드별 반환 후보입니다. 성공 코드뿐 아니라 문서화된 오류 코드와 응답 content type을 확인합니다. 자동 문서에 response가 없거나 단순하게 보이면, 그것이 모든 runtime 응답을 뜻하지는 않습니다.
5. **schemas**: 요청·응답 모델의 필드, 자료형, 배열 여부, required 정보를 읽습니다. `$ref`는 공통 `components/schemas` 정의를 가리키므로 링크를 따라가 실제 필드 구조를 확인합니다.

OpenAPI의 일반적인 구조와 용어는 [Swagger의 paths and operations 설명](https://swagger.io/docs/specification/v3_0/paths-and-operations/), [parameters 설명](https://swagger.io/docs/specification/v3_0/describing-parameters/), [request body 설명](https://swagger.io/docs/specification/v3_0/describing-request-body/), [OpenAPI Specification](https://swagger.io/specification/)도 참고할 수 있습니다. UI 자체의 사용법은 [Swagger UI](https://swagger.io/open-source/swagger-ui/) 문서를 참고하세요.

## Try it out의 안전한 사용

`Try it out`은 브라우저에서 선택한 operation을 실제로 호출하므로, 화면에 보이는 schema를 확인하는 것과 서버 요청을 실행하는 것은 다릅니다. 먼저 URL, parameter, request body, 예상 responses를 읽고 실행 여부를 결정하세요.

현재 신뢰할 수 있는 비인증 예시는 공개 endpoint인 `GET /api/csrf`입니다. 보안 동작 테스트에서 이 요청은 `200`으로 확인됩니다. 반면 보호된 읽기 endpoint는 이미 로그인된 브라우저 세션이 있어야 하며, 변경 요청은 CSRF 값이 없으면 실패할 수 있습니다. 세션과 CSRF 획득·재획득 절차는 후속 연동 절에서 확인합니다.

## Swagger가 말하지 않는 것

이 프로젝트의 `OpenAPI` bean은 제목·버전·설명만 명시합니다. 다음 항목은 현재 명시적으로 구성되거나 보장되지 않습니다.

- custom server URL 또는 deployment별 server 목록
- OpenAPI group
- 작성자가 부여한 tag 체계
- 안정적인 operation ID 규칙
- 모든 operation의 완전한 response metadata
- OpenAPI 보안 스키마와 Swagger UI의 인증 대화 상자

따라서 Swagger UI는 인증 제공자가 아닙니다. 현재 문서에는 OpenAPI 보안 스키마가 구성되어 있지 않으며, UI에서 인증을 완료하는 흐름을 전제로 구현하면 안 됩니다. 실제 인증·세션·CSRF 계약은 보안 설정과 HTTP 테스트를 우선해 후속 연동 지침에서 확인하세요.

문서에 노출된 operation과 schema는 현재 코드를 탐색하는 출발점입니다. 프론트엔드 코드에 반영하기 전에는 해당 컨트롤러·DTO와 보안 테스트를 다시 열어 method/path, 입력 위치, 실제 상태 코드를 대조하는 습관을 유지하세요.

## Swagger에 보이는 endpoint 지도

아래 표는 현재 production controller의 class-level과 method-level mapping을 합친 `/api/**` operation 전체입니다. `query`는 URL의 `?key=value`, `path`는 `{name}` 자리에 들어가는 값입니다. 성공 상태와 응답 형식은 controller 반환 선언을 함께 대조한 값이며, 모든 오류 후보를 열거한 표는 아닙니다.

<!-- swagger-routes:start -->
| Method | Path | Parameter 위치 | Body / part | 성공 상태 | 응답 type |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/me` | 없음 | 없음 | `200` | `Map<String, Object>` |
| GET | `/api/model-gate` | 없음 | 없음 | `200` | `Map<String, Object>` |
| GET | `/api/csrf` | 없음 | 없음 | `200` | `CsrfToken` |
| GET | `/api/token-status` | 없음 | 없음 | `200` | `Map<String, Object>` |
| POST | `/api/check-ins/{checkInId}/care-card` | path: `checkInId` | 없음 | 기존 카드 `200`, 새 카드 `201` | `CareCardResponse` |
| GET | `/api/check-ins/{checkInId}/care-card` | path: `checkInId` | 없음 | `200` | `CareCardResponse` |
| GET | `/api/care-cards/latest` | 없음 | 없음 | `200` | `CareCardResponse` |
| PATCH | `/api/care-cards/{careCardId}/feedback` | path: `careCardId` | body: `CareCardFeedbackRequest` | `204` | 본문 없음 (`Void`) |
| POST | `/api/check-ins` | 없음 | body: `CheckInCreateRequest` | `201` | `CheckInResponse` |
| GET | `/api/check-ins` | query: `date` (`yyyy-MM-dd`) | 없음 | `200` | `List<CheckInResponse>` |
| GET | `/api/check-ins/emotions` | query: `month` (`yyyy-MM`) | 없음 | `200` | `List<CheckInEmotionResponse>` |
| GET | `/api/check-ins/count` | query: `month` (`yyyy-MM`) | 없음 | `200` | `MonthlyCheckInCountResponse` |
| GET | `/api/check-ins/body-diaries/top-region` | query: `month` (`yyyy-MM`) | 없음 | `200` | `MostFrequentBodyRegionResponse` |
| POST | `/api/check-ins/images/analyze` | 없음 | part: `image` (`MultipartFile`) | `200` | `ImageAnalysisResponse` |
| GET | `/api/check-ins/images/{imageId}/url` | path: `imageId` | 없음 | `200` | `ImageUrlResponse` |
| POST | `/api/reports` | query: `month` (`yyyy-MM`) | 없음 | `201` | `ReportResponse` |
| GET | `/api/reports` | query: `month` (`yyyy-MM`) | 없음 | `200` | `ReportResponse` |
| GET | `/api/user` | 없음 | 없음 | `200` | `UserProfileResDto` |
| PATCH | `/api/user` | 없음 | body: `UpdateUserProfileReqDto` | `200` | `UserProfileResDto` |
<!-- swagger-routes:end -->

다음 경로는 Spring Security가 제공하는 runtime 인증 경로입니다. `/api/**` controller operation이 아니므로 위 Swagger-visible 표와 분리해서 봅니다.

<!-- framework-routes:start -->
| Method | Path | 역할 |
| --- | --- | --- |
| GET | `/oauth2/authorization/oidc` | OIDC 로그인 시작, 공급자로 redirect |
| GET | `/login/oauth2/code/oidc` | 공급자 callback 처리 |
| POST | `/logout` | 세션 무효화와 쿠키 삭제, 성공 시 `204` |
<!-- framework-routes:end -->

근거 controller는 [Auth](../src/main/java/Marshmello/MarshmelloWas/domain/auth/controller/AuthController.java), [CSRF](../src/main/java/Marshmello/MarshmelloWas/domain/auth/controller/CsrfController.java), [Token status](../src/main/java/Marshmello/MarshmelloWas/domain/auth/controller/TokenStatusController.java), [Care card](../src/main/java/Marshmello/MarshmelloWas/domain/care/controller/CareCardController.java), [Check-in](../src/main/java/Marshmello/MarshmelloWas/domain/checkin/controller/CheckInController.java), [Image](../src/main/java/Marshmello/MarshmelloWas/domain/checkin/controller/CheckInImageController.java), [Report](../src/main/java/Marshmello/MarshmelloWas/domain/report/controller/ReportController.java), [User](../src/main/java/Marshmello/MarshmelloWas/domain/user/controller/UserController.java)입니다.

`CareCardGenerationRequest`, `CareCardCreationResult`, `ReportGenerationRequest` 같은 AI/서비스 DTO는 서비스 내부 경계를 위해 존재하며 현재 프론트엔드 wire payload가 아닙니다. 공용 `PageResponse`도 내부 코드에는 존재하지만 현재 어떤 controller도 반환하지 않으므로 프론트엔드 wire schema가 아닙니다. pagination 계약을 추측해서 추가하지 마세요.

mapping의 content type을 과도하게 일반화하지도 마세요. 현재 controller 중 이미지 분석만이 유일하게 `multipart/form-data` consumes를 명시합니다. 다른 operation 전체가 특정 JSON `consumes` 또는 `produces`를 명시한다고 해석하면 안 됩니다.

## 대표 요청 예제

아래 호출은 앞 절의 `apiFetch`와 CSRF helper를 그대로 사용합니다. `@RequestBody` 예제에서는 브라우저가 보낼 JSON을 명시하지만, 이것을 모든 endpoint의 전역 `consumes`/`produces` 선언으로 확대하지 않습니다.

```javascript
export async function loadCheckInViews() {
  const byDate = await apiFetch('/api/check-ins?date=2026-08-17');
  const emotions = await apiFetch('/api/check-ins/emotions?month=2026-08');
  const imageUrl = await apiFetch('/api/check-ins/images/42/url');
  return { byDate, emotions, imageUrl };
}

export async function updateUser() {
  return apiFetch('/api/user', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      nickname: 'mallow',
      expectedDeliveryDate: '2026-11-20',
    }),
  });
}

export async function createCheckIn(imageId) {
  return apiFetch('/api/check-ins', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      imageId,
      achieved: true,
      diary: '오늘 기록',
      emotion: 3,
      bodyDiaries: [{ bodyRegion: 2, stretchMark: false, comment: '편안함' }],
    }),
  });
}

export async function analyzeImage(file) {
  const form = new FormData();
  form.append('image', file);
  return apiFetch('/api/check-ins/images/analyze', {
    method: 'POST',
    body: form,
  });
  // Content-Type을 직접 설정하지 않습니다. 브라우저가 multipart boundary와 함께 만듭니다.
}

export async function createOrReadCareCard(checkInId) {
  // 새로 생성되면 201, 이미 있던 카드를 반환하면 200이며 응답은 모두 CareCardResponse입니다.
  return apiFetch(`/api/check-ins/${checkInId}/care-card`, { method: 'POST' });
}

export async function sendCareCardFeedback(careCardId, helpfulnessScore) {
  // 성공은 204이며 apiFetch가 response.json() 대신 undefined를 반환합니다.
  return apiFetch(`/api/care-cards/${careCardId}/feedback`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ helpfulnessScore }),
  });
}

export async function loadOrCreateReport() {
  const current = await apiFetch('/api/reports?month=2026-08');
  const created = await apiFetch('/api/reports?month=2026-08', { method: 'POST' });
  return { current, created };
}
```

`FormData` 요청에는 `Content-Type: multipart/form-data`를 직접 넣지 마세요. 직접 넣으면 브라우저가 생성해야 할 boundary가 빠져 서버가 part를 해석하지 못할 수 있습니다. 체크인 생성은 multipart가 아니라 위 `CheckInCreateRequest` 본문 예제처럼 전송하고, 보고서 생성·조회는 date-range 본문이 아니라 `month=yyyy-MM` query를 사용합니다.

## 브라우저 세션과 OIDC 로그인

이 서버의 로그인 시작점은 최상위 경로 `GET /oauth2/authorization/oidc`입니다. API 호출로 로그인 응답을 해석하지 말고, 브라우저를 이 주소로 **이동**시키세요. 서버는 OIDC 공급자로 `302` 리다이렉트하고, 콜백은 프레임워크 경로 `/login/oauth2/code/oidc`에서 처리한 뒤 설정된 성공 URL로 다시 리다이렉트합니다.

```javascript
export function startLogin() {
  window.location.assign('/oauth2/authorization/oidc');
}
```

브라우저는 인증 정보를 읽어 들고 보관하지 않습니다. 인증된 요청은 서버 세션을 가리키는 불투명한 `JSESSIONID` 쿠키로 식별되며, 이 쿠키는 `HttpOnly` 설정이므로 JavaScript에서 값에 접근할 수 없습니다. OIDC의 `state`와 PKCE 관련 요청 정보, 그리고 OIDC access/refresh token은 서버 측 `HttpSession` 저장소에 보관됩니다. 따라서 브라우저 코드에 bearer/JWT 토큰을 만들거나 `localStorage`·`sessionStorage`에 토큰이나 세션 값을 저장하지 마세요.

로그인 성공 시 세션 고정 보호를 위해 세션이 교체될 수 있습니다. 로그인 리다이렉트가 끝나거나 로그아웃·세션 만료 뒤에는 기존 CSRF 값을 재사용한다고 가정하지 말고, 아래 절차로 CSRF를 다시 받아야 합니다.

## CSRF와 fetch 공통 처리

`GET /api/csrf`는 로그인 전에도 호출할 수 있으며, 응답에는 `headerName`, `parameterName`, `token`이 포함됩니다. 변경 메서드에는 고정 문자열 헤더 이름을 추측하지 말고 응답의 `headerName`을 사용합니다. 아래 모듈은 모든 API 요청에 쿠키를 포함하고, 응답의 상태와 content type을 먼저 판별하며 `204 No Content`에는 `response.json()`을 호출하지 않습니다.

```javascript
let csrf;

export class ApiRequestError extends Error {
  constructor(status, contentType, body) {
    super(`API request failed: ${status}`);
    this.name = 'ApiRequestError';
    this.status = status;
    this.contentType = contentType;
    this.body = body;
  }
}

async function readResponseBody(response) {
  if (response.status === 204) {
    return undefined;
  }

  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    return response.json();
  }
  return response.text();
}

export async function acquireCsrf() {
  const response = await fetch('/api/csrf', {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  });
  const body = await readResponseBody(response);

  if (!response.ok || !body?.headerName || !body?.token) {
    throw new ApiRequestError(
      response.status,
      response.headers.get('content-type') ?? '',
      body,
    );
  }

  csrf = body;
  return csrf;
}

export async function refreshCsrfAfterSessionChange() {
  csrf = undefined;
  return acquireCsrf();
}

export async function apiFetch(path, init = {}) {
  const method = (init.method ?? 'GET').toUpperCase();
  const headers = new Headers(init.headers ?? {});

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    csrf ??= await acquireCsrf();
    Object.entries({ [csrf.headerName]: csrf.token })
      .forEach(([name, value]) => headers.set(name, value));
  }

  const response = await fetch(path, {
    ...init,
    method,
    headers,
    credentials: 'include',
  });
  const body = await readResponseBody(response);

  if (!response.ok) {
    throw new ApiRequestError(
      response.status,
      response.headers.get('content-type') ?? '',
      body,
    );
  }
  return body;
}
```

`Object.entries({ [csrf.headerName]: csrf.token })`처럼 동적으로 헤더를 구성하는 이유는 현재 테스트에서 관찰된 `X-CSRF-TOKEN`을 하드코딩된 영구 계약으로 취급하지 않기 위해서입니다. 로그인 완료, 로그아웃 완료, 세션 만료를 감지했거나 CSRF 거부를 받은 뒤에는 `await refreshCsrfAfterSessionChange()`를 실행한 뒤 요청을 다시 시작하세요. 재시도 여부와 중복 변경 요청의 안전성은 각 화면의 업무 규칙에 맞게 결정해야 합니다.

현재 런타임은 `headerName`으로 `X-CSRF-TOKEN`을 반환하고 `CorsConfig`도 정확히 `X-CSRF-TOKEN`을 허용합니다. 앞으로 `headerName`이 바뀌거나 이 값과 다르게 반환되면, 교차 출처 preflight가 실패하지 않도록 백엔드의 `CorsConfig.allowedHeaders`를 같은 이름으로 함께 갱신해야 합니다.

## CORS와 로그아웃 경계

프론트엔드와 WAS가 다른 origin이면 모든 `/api/**` 요청에 `credentials: 'include'`가 필요합니다. 서버의 `APP_CORS_ALLOWED_ORIGINS`는 쉼표로 구분한 **정확한 origin** 목록이며 공백 항목은 제거됩니다. 기본값은 `http://localhost:5173`입니다. 설정된 origin만 `Access-Control-Allow-Origin`으로 그대로 반사받고, credential 허용과 함께 `Content-Type`, `X-CSRF-TOKEN` 요청 헤더 및 `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` 메서드가 허용됩니다. `*` 와일드카드나 origin pattern은 이 credential 계약에 사용할 수 없습니다.

CORS 허용은 쿠키가 모든 교차 출처(cross-origin) 배치에서 자동으로 전송된다는 뜻이 아닙니다. 현재 세션 쿠키의 기본 SameSite 설정은 `lax`이며, 배포의 사이트 구성과 HTTPS 쿠키 설정은 별도로 확인해야 합니다. 가장 단순한 운영 형태는 프론트엔드가 WAS와 같은 origin이거나, 프론트엔드 origin의 리버스 프록시가 `/api/**`와 `/logout`을 WAS로 전달하는 방식입니다.

로그아웃은 `POST /logout`이며 CSRF가 필요합니다. 성공하면 `204 No Content`이고 서버는 세션을 무효화하고 `JSESSIONID`를 삭제합니다. 같은 origin 또는 위 프록시 경로에서만 아래처럼 호출하세요.

```javascript
export async function logout() {
  const result = await apiFetch('/logout', { method: 'POST' });
  csrf = undefined;
  return result; // 204에서는 undefined
}
```

직접적인 교차 출처 로그아웃은 작동하는 연동 경로로 문서화할 수 없습니다. CORS 정책은 `/api/**`에만 등록되어 있어 `/logout`에 CORS grant가 없으므로, 예를 들어 다른 origin의 SPA가 WAS origin의 `/logout`을 직접 `fetch`하는 방식은 브라우저에서 사용할 수 없습니다. `mode: 'no-cors'`는 해결책이 아닙니다. 응답을 읽을 수 없고 CSRF 헤더를 실어 보낼 수 없으므로 사용하지 마세요. 이 경우 동일 origin 프록시를 마련하거나 서버 배치를 변경해야 합니다.

## 상태 코드와 오류 응답 읽기

프론트엔드는 성공 여부를 JSON 여부보다 먼저 상태 코드로 판단해야 합니다. 위 `apiFetch`는 `204`를 본문 없음으로 반환하고, JSON content type일 때만 JSON을 파싱하며, 그 밖에는 텍스트를 보존합니다. 이 순서면 빈 성공 응답, HTML 오류 페이지, 프록시 오류 응답도 `response.json()` 예외로 가리지 않고 화면의 오류 처리 정책으로 전달할 수 있습니다.

컨트롤러 예외가 `GlobalExceptionHandler`를 통과한 경우에는 `ApiErrorResponse` 형태를 받을 수 있습니다. 필드는 `code`, `message`, `retryable`, `fieldErrors`, `committedCheckIn`이고, `fieldErrors`의 각 항목은 `field`, `reason`입니다. 상태 코드는 해당 `ErrorCode`가 가진 상태를 사용합니다. 예를 들어 잘못된 요청은 `400`, 도메인 리소스 부재는 `404`, 충돌은 `409`, 일부 처리 불가 상황은 `422`가 될 수 있으며, 응답의 `retryable`은 재시도가 가능한지 판단하는 단서입니다.

그러나 모든 오류가 이 JSON 형태라는 보장은 없습니다. `/api/**`의 미인증 요청은 보안 필터에서 `401`이 될 수 있고, CSRF 누락 같은 `403`도 필터 단계에서 발생할 수 있습니다. 정적 리소스 또는 매핑되지 않은 경로의 `404`도 빈 응답일 수 있습니다. 그러므로 `401`·`403`·`404`의 본문 구조를 가정하지 말고, 상태 코드와 content type을 먼저 읽은 다음, JSON이면서 필요한 필드가 있을 때에만 `ApiErrorResponse`로 취급하세요.

## 유지보수 체크리스트

컨트롤러 mapping, DTO, 성공 상태, `SecurityConfig`, `CorsConfig` 또는 CSRF 동작이 바뀌면 이 문서의 endpoint 표와 예제를 같은 변경에서 다시 대조하세요. Swagger에서 자동 추론된 표현만 보고 계약을 갱신하지 말고, 관련 동작 테스트까지 확인해야 합니다. 배포 환경 변수와 운영 절차는 여기서 중복 설명하지 않으며 [EC2 배포 가이드](ec2-deployment.md)를 기준으로 관리합니다.
