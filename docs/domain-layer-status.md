# 도메인 계층 현황

각 도메인은 `domain/<도메인>/controller`, `service`, `repository`를 기본 계층으로 사용한다.
구현되지 않은 기본 계층은 `.gitkeep`으로 위치만 보존한다.

`model`, `port`, `policy`와 인프라 어댑터는 모든 도메인에 일괄적으로 만들지 않는다.
현재 동작하는 도메인 규칙이나 외부 경계가 있을 때만 유지한다.

## 기본 계층 현황

| 도메인 | Controller | Service | Repository |
| --- | --- | --- | --- |
| `auth` | 구현됨 | 미구현 | 미구현 |
| `checkin` | 미구현 | `CheckInCommandService`, `CheckInQueryService` | 구현됨 |
| `care` | 미구현 | 유스케이스 미구현 (`service/port`만 존재) | 구현됨 |
| `analysis` | 미구현 | 미구현 | 구현됨 |
| `report` | 미구현 | 유스케이스 미구현 (`service/port`만 존재) | 구현됨 |

## 선택 구조 현황

| 도메인 | Model | Port | Policy | 인프라 어댑터 |
| --- | --- | --- | --- | --- |
| `auth` | 인증 주체와 모델 접근 규칙 | `CurrentUserIdProvider` | `ModelAccessPolicy` | Spring Security/OIDC 권한 검사와 현재 사용자 제공 구현 |
| `checkin` | 별도 패키지 없음 | 없음 | 없음 | 없음 |
| `care` | AI 생성 결과와 오류 | `service/port/CareCardGenerator` | 없음 | OpenAI 생성기와 비활성화 대체 구현 |
| `analysis` | 없음 | 없음 | 없음 | 없음 |
| `report` | AI 생성 결과와 오류 | `service/port/ReportGenerator` | 없음 | OpenAI 생성기와 비활성화 대체 구현 |

## 정리 기준

- 실제 구현체나 런타임 소비자가 없는 미래용 Port는 미리 선언하지 않는다.
- OAuth/OpenAI처럼 교체 가능한 외부 시스템 경계에서만 Port와 인프라 어댑터를 사용한다.
- Service가 사용하는 외부 기능 계약은 최상위 `port`가 아니라 해당 도메인의 `service/port`에 둔다.
- 구현할 유스케이스가 확정되면 필요한 메서드와 데이터 형식을 기준으로 Port를 추가한다.
- `checkin`은 내부 Repository를 조합하는 유스케이스이므로 별도 Port/어댑터 없이 Service가 담당한다.
- `checkin`의 `BodyDiaryId`는 JPA 복합키이므로 `entity`에 둔다.
- `CheckInPage`는 조회 결과 DTO이므로 `dto`에 둔다.
- `analysis`는 현재 엔티티와 Repository만 사용하므로 미사용 `ImageAnalyzer` 계약을 비롯한 Model, Port, 어댑터를 두지 않는다.
- `.gitkeep`은 빈 디렉터리를 Git에 남기기 위한 표식이며 Java 구현이나 Spring Bean이 아니다.

## 추후 구현 대상

- `auth`: `CurrentUserIdProvider`를 사용하는 인증 애플리케이션 서비스와 영속화 요구가 확정될 때 Repository 구현
- `checkin`: 기존 Command/Query Service를 호출하는 HTTP Controller
- `care`: 케어 카드 생성 Service와 HTTP Controller, 필요 시 조회·저장 Port
- `analysis`: 이미지 분석 유스케이스가 확정될 때 Service, Port, 실제 분석 어댑터
- `report`: 리포트 생성 Service와 HTTP Controller, 필요 시 조회·저장 Port
