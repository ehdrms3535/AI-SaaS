# Instagram DM MVP Runbook

## 목적

이 문서는 현재 구현된 Instagram 수신형 DM 예약 MVP의 운영 상태를 정리합니다.

현재 범위:

- Instagram 비즈니스 계정 연결
- 연결된 Facebook Page / Instagram 계정 동기화
- Meta webhook verify / webhook 수신
- 실제 Instagram DM 수신
- DM Inbox 적재
- 자동 예약 처리
- 운영자 수동 확정 / 수정 / 취소

현재 제외:

- Instagram 실제 답장 발송 live 운영

## 현재 상태

구현 완료:

- Instagram OAuth 연결
- ConnectedChannel 저장
- 실제 Instagram 비즈니스 계정 식별
- Meta webhook verify endpoint
- Meta Instagram webhook 수신 endpoint
- 실제 DM inbound 처리
- echo 이벤트 분리
- DM Inbox UI
- 채널 카드 send mode 표시

현재 외부 블로커:

- Instagram Send API live 발송 시 Meta 응답:
  - `(#3) Application does not have the capability to make this API call.`
- 즉 receive는 가능하지만 send capability는 Meta 운영/검수/비즈니스 인증 영역에 묶여 있음

## 권장 운영 모드

현재 권장값:

```powershell
$env:DM_INSTAGRAM_ENABLED="true"
$env:DM_INSTAGRAM_DRY_RUN="true"
```

의미:

- 실제 DM 수신은 정상 동작
- 자동 처리 / 수동 처리 가능
- 답장은 실제 전송하지 않고 dry-run 로그만 남김

## 실행 환경변수

기본 실행 예시:

```powershell
$env:DM_INSTAGRAM_CLIENT_ID="1285764643426507"
$env:DM_INSTAGRAM_CLIENT_SECRET="<APP_SECRET>"
$env:DM_INSTAGRAM_REDIRECT_URI="https://<YOUR_DOMAIN>/api/channels/instagram/callback"
$env:DM_INSTAGRAM_WEBHOOK_VERIFY_TOKEN="saas-instagram-verify-1234"
$env:DM_INSTAGRAM_SCOPES="pages_show_list,pages_read_engagement,pages_manage_metadata,business_management,instagram_basic,instagram_manage_comments,instagram_manage_messages"
$env:DM_INSTAGRAM_ENABLED="true"
$env:DM_INSTAGRAM_DRY_RUN="true"
$env:GRADLE_USER_HOME="c:\Users\Kim\AI-SaaS\saas\saas\.gradle-tmp"

./gradlew bootRun
```

## Meta 설정값

현재 도메인을 `<YOUR_DOMAIN>`으로 두고 아래 값들을 맞춰야 합니다.

App Domains:

```text
<YOUR_DOMAIN>
```

Valid OAuth Redirect URI:

```text
https://<YOUR_DOMAIN>/api/channels/instagram/callback
```

Webhook Callback URL:

```text
https://<YOUR_DOMAIN>/webhooks/meta/instagram
```

Webhook Verify Token:

```text
saas-instagram-verify-1234
```

Data Deletion Callback URL:

```text
https://<YOUR_DOMAIN>/webhooks/meta/data-deletion
```

Privacy Policy URL:

```text
https://<YOUR_DOMAIN>/legal/privacy.html
```

Terms URL:

```text
https://<YOUR_DOMAIN>/legal/terms.html
```

Data Deletion Instructions URL:

```text
https://<YOUR_DOMAIN>/legal/data-deletion.html
```

## 정상 동작 확인 방법

### 1. 채널 연결

- UI에서 `Instagram 연결 시작`
- 연결 완료 후 채널 카드 확인
- `Instagram 동기화` 실행
- 아래 값이 채워지면 정상:
  - Instagram username
  - external account id
  - page name

### 2. Webhook verify

서버 콘솔 확인:

- `[META_WEBHOOK_VERIFY]`

### 3. 실제 DM 수신

다른 Instagram 계정에서 연결된 비즈니스 계정으로 DM 전송

정상 로그:

- `[META_WEBHOOK_POST]`
- `[META_WEBHOOK_INBOUND]`

예시:

```text
[META_WEBHOOK_INBOUND] recipient=<business_account_id> sender=<user_id> text=안녕하세요
```

### 4. echo 이벤트

정상 로그:

```text
[META_WEBHOOK_ECHO] recipient=<user_id> sender=<business_account_id> text=...
```

이 이벤트는 발신 반사 이벤트이므로 처리 대상이 아님

### 5. DM Inbox

UI에서 확인:

- 새 DM 카드 생성
- 상태 배지 표시
- 자동응답 / 처리결과 / 수신시각 표시
- 상세 열기 후 수동 처리 가능

## 운영 체크포인트

운영자가 확인해야 할 것:

- 채널 카드 `send mode`
  - `SEND OFF`
  - `DRY RUN`
  - `LIVE SEND`
- DM Inbox 상태
  - `RECEIVED`
  - `NEEDS_REVIEW`
  - `RESERVED`

현재 권장 상태:

- Instagram 채널은 `DRY RUN`

## Known Issues

### 1. 실제 발송 live 호출 불가

현재 Meta send API 호출은 아래 에러로 막힘:

```text
(#3) Application does not have the capability to make this API call.
```

해석:

- 코드 문제 아님
- Meta 앱 capability / 검수 / 비즈니스 인증 이슈

### 2. Quick Tunnel 주소 변동

`trycloudflare.com` 주소는 재시작 시 바뀔 수 있음

영향:

- OAuth redirect URI
- webhook callback URL
- legal URL

를 같이 다시 맞춰야 함

### 3. message=null 이벤트

Instagram webhook는 텍스트 메시지 외 부가 이벤트도 보낼 수 있음

현재 처리:

- 텍스트 없는 이벤트는 무시

## 권장 다음 단계

### 바로 이어서 할 일

- 수신형 Instagram 예약 운영 SaaS 완성도 높이기
- DM Inbox / 운영자 UX 정리
- 고정 HTTPS 도메인 준비
- 배포 환경 정리

### 이후 할 일

- Meta App Review 제출
- 필요 시 비즈니스 포트폴리오 / 비즈니스 인증 준비
- send capability 확보 후 live 발송 재시도

## 현재 판단

현재 MVP는 다음 형태로 보는 것이 적절합니다.

- `Instagram 수신형 예약 운영 SaaS`

즉:

- DM 수신 가능
- 자동 처리 가능
- 운영자 수동 개입 가능
- 실제 자동 답장 발송은 Meta 외부 승인 이후 연결
