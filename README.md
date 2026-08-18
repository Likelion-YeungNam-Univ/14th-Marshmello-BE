gi# 14th-Marshmello-was

|                              BE / Leader                             |                                   BE                                   |                                   FE                                  |                                      FE                                      |                                   FE                                  |                                     P&D                                     |
| :------------------------------------------------------------------: | :--------------------------------------------------------------------: | :-------------------------------------------------------------------: | :--------------------------------------------------------------------------: | :-------------------------------------------------------------------: | :-------------------------------------------------------------------------: |
| <img src="https://github.com/6rmkhj.png" width="100" height="100" /> | <img src="https://github.com/psj-1228.png" width="100" height="100" /> | <img src="https://github.com/Pdar124.png" width="100" height="100" /> | <img src="https://github.com/parkjinacosmos.png" width="100" height="100" /> | <img src="https://github.com/Hengjju.png" width="100" height="100" /> | <img src="https://github.com/22320139-code.png" width="100" height="100" /> |
|                   [김형준](https://github.com/6rmkhj)                   |                   [박성준](https://github.com/psj-1228)                   |                   [박다래](https://github.com/Pdar124)                   |                   [박진아](https://github.com/parkjinacosmos)                   |                   [정형주](https://github.com/Hengjju)                   |                   [최혜선](https://github.com/22320139-code)                   |


# 📌 컨벤션
### 📚 커밋 메시지

| message | description                                           |
| ------- | ----------------------------------------------------- |
| feat    | 🥥 새로운 기능 추가, 기존 기능을 요구 사항에 맞추어 수정 |
| fix     | 🐛 기능에 대한 버그 수정                                 |
| docs    | 📝 문서(주석) 수정                                       |
| refactor| ✏️ 기능 변화가 아닌 코드 리팩터링                        |
| test    | ⚙️ 테스트 코드 추가/수정                                 |
| chore   | 🛠️ 패키지 매니저 수정, 그 외 기타 수정 ex) .gitignore    |

💡 **커밋 메시지 예시**  
- feat : 로그인 폼 완성  
- fix : 회원가입 에러 해결
- docs : 회원가입 로직 주석 수정  
---

### 🌳 Branch Naming 규칙

우리 팀은 기능 개발 중심으로 브랜치 네이밍을 다음과 같이 사용합니다:

- **feature/**: 새로운 기능 개발용 브랜치  
  예) `feature/login-page`, `feature/user-profile`

- **fix/**: 버그 수정용 브랜치  
  예) `fix/login-error`, `fix/signup-validation`

- **hotfix/**: 긴급 수정용 브랜치 (주로 main에서 바로 생성)  
  예) `hotfix/security-patch`

- **release/**: 배포 준비용 브랜치  
  예) `release/v1.0.0`

- **develop**: 다음 릴리스용 개발 기본 브랜치  
  - 모든 feature, fix 브랜치는 develop으로 병합

- **main**: 항상 배포 가능한 안정된 코드 유지 브랜치

## Localhost frontend 연동

백엔드는 `http://localhost:8080`, 프런트엔드는 `http://localhost:5173`에서 실행합니다. API 호출에는 `credentials: 'include'`를 사용하세요.

```js
const csrf = await fetch('http://localhost:8080/api/csrf', {
  credentials: 'include'
}).then(response => response.json()); // GET /api/csrf

const mutationHeaders = {
  'Content-Type': 'application/json',
  [csrf.headerName]: csrf.token
}; // use these headers on POST/PUT/PATCH/DELETE requests
```

로그인은 최상위 이동으로 `http://localhost:8080/oauth2/authorization/oidc`를 여세요. 로그인 전 보호 API의 `401`은 정상입니다. `APP_CORS_ALLOWED_ORIGINS`에는 쉼표로 구분한 정확한 Origin만 지정하고 와일드카드는 사용하지 마세요. 절대 `mode: 'no-cors'`를 사용하지 말고, 인증 토큰을 클라이언트에 저장하지 마세요. 인증은 세션 쿠키 흐름을 사용하세요.

로컬 프런트엔드에서 EC2 인증을 테스트할 때 API 주소는 `https://marshmello-be.duckdns.org`를 사용하고 모든 요청에 `credentials: 'include'`를 적용하세요. EC2 배포에서 로컬 프런트엔드와 Amplify 프런트엔드를 모두 허용하려면 `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,https://dev.dia8lj4ohc0fh.amplifyapp.com`, `APP_LOGIN_SUCCESS_URL=https://dev.dia8lj4ohc0fh.amplifyapp.com`, `SESSION_COOKIE_SAME_SITE=none`, `SESSION_COOKIE_SECURE=true`를 설정합니다. 로그인 시작 요청의 허용된 `Origin` 또는 `Referer`에 따라 로그인 완료 후 해당 프런트엔드로 돌아가며, 출처를 확인할 수 없으면 `APP_LOGIN_SUCCESS_URL`로 돌아갑니다. OAuth 공급자에는 `https://marshmello-be.duckdns.org/login/oauth2/code/oidc`를 콜백 URL로 등록해야 합니다.

---
