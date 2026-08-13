# 도메인 계층 현황

각 도메인은 `controller`, `service`, `repository`를 기본 계층으로 사용한다.
구현되지 않은 기본 계층은 `.gitkeep`으로 위치만 보존한다.

확장 가능한 외부 경계가 실제로 존재할 때만 `port`와 `adapter`를 함께 둔다.
별도 `model`, `policy` 패키지는 사용하지 않는다.

## 기본 계층 현황

| 도메인 | Controller | Service | Repository |
| --- | --- | --- | --- |
| `auth` | 구현됨 | `ModelAccessPolicy` | 미구현 |
| `checkin` | 미구현 | `CheckInCommandService`, `CheckInQueryService` | 구현됨 |
| `care` | 미구현 | 미구현 | 구현됨 |
| `analysis` | 미구현 | 미구현 | 구현됨 |
| `report` | 미구현 | 미구현 | 구현됨 |

## Port와 Adapter 현황

| 도메인 | Port | Adapter |
| --- | --- | --- |
| `auth` | `CurrentUserIdProvider` | `ModelGateAuthorizationManager`, `UnauthenticatedCurrentUserIdProvider` |
| `care` | `CareCardGenerator` | `OpenAiCareCardGenerator`, `UnavailableCareCardGenerator` |
| `report` | `ReportGenerator` | `OpenAiReportGenerator`, `UnavailableReportGenerator` |
| `checkin` | 없음 | 없음 |
| `analysis` | 없음 | 없음 |

## 정리 기준

- 실제 구현체나 런타임 소비자가 없는 미래용 Port는 미리 선언하지 않는다.
- OAuth/OpenAI처럼 교체 가능한 외부 시스템 경계에서만 Port와 Adapter를 함께 사용한다.
- Port의 요청·응답·예외 타입은 별도 Model 패키지 대신 해당 Port 내부 타입으로 둔다.
- 인증 접근 규칙과 관련 값 타입은 `ModelAccessPolicy`에 모아 Service 패키지에서 관리한다.
- `checkin`은 내부 Repository를 조합하는 유스케이스이므로 별도 Port/어댑터 없이 Service가 담당한다.
- `checkin`의 `BodyDiaryId`는 JPA 복합키이므로 `entity`에 둔다.
- `CheckInPage`는 조회 결과 DTO이므로 `dto`에 둔다.
- `analysis`는 현재 엔티티와 Repository만 사용하므로 Port와 Adapter를 두지 않는다.
- OpenAI 클라이언트 설정, 속성, 응답 파싱처럼 도메인 공통인 기술 코드는 `infrastructure/ai/openai`에 유지한다.
- Spring Data Repository 구현체는 프레임워크가 생성하므로 별도 Adapter 파일을 만들지 않는다.
- `.gitkeep`은 빈 디렉터리를 Git에 남기기 위한 표식이며 Java 구현이나 Spring Bean이 아니다.

## 추후 구현 대상

- `auth`: 인증 유스케이스 서비스와 영속화 요구가 확정될 때 Repository 구현
- `checkin`: 기존 Command/Query Service를 호출하는 HTTP Controller
- `care`: `CareCardGenerator`를 사용하는 생성 Service와 HTTP Controller
- `analysis`: 이미지 분석 유스케이스와 외부 분석 경계가 확정될 때 Service·Port·Adapter
- `report`: `ReportGenerator`를 사용하는 생성 Service와 HTTP Controller
