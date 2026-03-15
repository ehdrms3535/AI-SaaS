# Instagram Live Checklist

현재 `InstagramDmChannelSender`는 live 모드에서 아래 가정을 기준으로 동작합니다.

## 현재 가정

- `senderChannelId`는 Instagram recipient id 입니다.
- `app.dm.instagram.base-url`은 메시지 전송 endpoint 입니다.
- 요청 본문은 아래 형태입니다.

```json
{
  "recipient": {
    "id": "ig-user-123"
  },
  "message": {
    "text": "답장 문구"
  }
}
```

- 인증은 `Authorization: Bearer <accessToken>` 헤더로 보냅니다.
- `quickReplies`는 아직 실제 요청 본문에 포함하지 않고 `replyPayload`에만 남깁니다.

## 환경변수

```powershell
$env:DM_INSTAGRAM_ENABLED="true"
$env:DM_INSTAGRAM_DRY_RUN="false"
$env:DM_INSTAGRAM_BASE_URL="https://example.com/send"
$env:DM_INSTAGRAM_ACCESS_TOKEN="real-token"
```

## 서버 실행

```powershell
$env:GRADLE_USER_HOME='c:\Users\Kim\AI-SaaS\saas\saas\.gradle-tmp'
./gradlew bootRun
```

## webhook 호출 예시

```powershell
$body = @{
  organizationId = "8d077fa9-a27f-4c65-8d72-e3bdd9992eb3"
  channel = "INSTAGRAM"
  senderChannelId = "ig-user-123"
  senderName = "김고객"
  senderPhone = "010-1111-2222"
  serviceHint = "커트"
  message = "김고객 4월 1일 19시 커트 예약"
} | ConvertTo-Json -Depth 3

$response = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/webhooks/dm/inbound" `
  -ContentType "application/json; charset=utf-8" `
  -Headers @{ "X-DM-Webhook-Secret" = "1234" } `
  -Body $body

$response | ConvertTo-Json -Depth 10
```

## 기대 결과

- live 성공이면:

```json
"dispatch": {
  "accepted": true,
  "providerMessage": "INSTAGRAM_LIVE_SENT"
}
```

- provider가 4xx/5xx를 돌려주면:

```json
"dispatch": {
  "accepted": false,
  "providerMessage": "INSTAGRAM_HTTP_XXX"
}
```

- 네트워크/호스트 오류면:

```json
"dispatch": {
  "accepted": false,
  "providerMessage": "INSTAGRAM_SEND_ERROR"
}
```

## live 전 확인할 것

- recipient id가 실제 Instagram scoped id 인지
- base URL이 실제 발송 endpoint 인지
- access token scope가 메시지 발송 권한을 포함하는지
- provider가 `recipient/message` 구조를 실제로 받는지
- quick replies를 provider 스펙에 맞게 따로 붙여야 하는지

## 다음 구현 포인트

- Meta 공식 endpoint/field 기준으로 request body 확정
- provider 응답 body를 `dispatch`에 요약해서 반영
- quick replies를 채널별 포맷으로 매핑
