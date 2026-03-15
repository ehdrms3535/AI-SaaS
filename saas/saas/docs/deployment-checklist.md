# Deployment Checklist

## 목적

이 문서는 Instagram 수신형 예약 SaaS를 로컬 테스트 환경에서 실제 배포 환경으로 옮기기 전에 확인할 항목을 정리합니다.

현재 기준:

- Instagram DM 수신 가능
- 자동 처리 / DM Inbox / 운영자 수동 처리 가능
- 실제 Instagram send live 발송은 Meta capability 이슈로 보류

## 1. 도메인 / 네트워크

- 고정 HTTPS 도메인 준비
- 프론트/백엔드 접근 경로 결정
- 더 이상 `trycloudflare.com` 주소에 의존하지 않기

권장 예시:

- App: `https://app.your-domain.com`
- API: `https://api.your-domain.com`

최소 요구:

- Meta OAuth redirect URI용 고정 HTTPS URL
- Meta webhook callback용 고정 HTTPS URL
- legal 문서용 공개 URL

## 2. 환경변수 정리

배포 환경에 필요한 핵심 값:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
DM_INSTAGRAM_CLIENT_ID
DM_INSTAGRAM_CLIENT_SECRET
DM_INSTAGRAM_REDIRECT_URI
DM_INSTAGRAM_WEBHOOK_VERIFY_TOKEN
DM_INSTAGRAM_SCOPES
DM_INSTAGRAM_ENABLED
DM_INSTAGRAM_DRY_RUN
```

현재 권장 운영값:

```text
DM_INSTAGRAM_ENABLED=true
DM_INSTAGRAM_DRY_RUN=true
```

이유:

- 수신형 MVP 기준 운영 가능
- Meta send capability 확보 전까지 안전

## 3. Meta 설정 반영

배포 도메인이 정해지면 아래 값을 모두 다시 맞춘다.

### Basic

- App Domains
- Contact Email
- Privacy Policy URL
- Terms URL
- Data Deletion Callback URL

### OAuth / Instagram Business Login

- Valid OAuth Redirect URI
- Redirect URI

### Webhooks

- Callback URL
- Verify Token
- 필드 구독 상태 확인

## 4. 서버 보안 / 접근 제어

- `/webhooks/meta/instagram` 공개 접근 허용 유지
- `/webhooks/meta/data-deletion` 공개 접근 허용 유지
- `/legal/**` 공개 접근 허용 유지
- 나머지 관리 API는 JWT 보호 유지

## 5. 데이터베이스

배포 전 확인:

- `connected_channels` 테이블 생성 확인
- `dm_messages` 테이블 생성 확인
- `customers`, `reservations`, `services`, `organizations` 정상 연결 확인

운영 데이터 관점 확인:

- DM 메시지 저장량 증가 시 정리 전략
- 로그/메시지 retention 정책

## 6. 운영 시나리오 점검

배포 전 최소 확인 시나리오:

1. 로그인
2. Instagram 채널 연결
3. Instagram 동기화
4. 실제 다른 계정에서 DM 전송
5. DM Inbox 반영
6. 자동 예약 처리 확인
7. 수동 확정 / 수정 / 취소 확인

## 7. 관측성

운영 로그에서 확인 가능한 항목:

- `[META_WEBHOOK_VERIFY]`
- `[META_WEBHOOK_POST]`
- `[META_WEBHOOK_INBOUND]`
- `[META_WEBHOOK_ECHO]`
- Instagram dry-run / live send 로그

권장:

- 서버 로그 수집 위치 정리
- 오류 로그 보존
- 운영자용 최근 이벤트 확인 UI 개선 검토

## 8. 법적 / 정책 문서

배포 전 공개 가능한 상태여야 하는 문서:

- Privacy Policy
- Terms of Service
- Data Deletion Instructions / Callback

현재 프로젝트 내 정적 문서:

- `/legal/privacy.html`
- `/legal/terms.html`
- `/legal/data-deletion.html`

## 9. 현재 외부 블로커

Meta Send API live 발송은 아직 아래 이유로 막힘:

```text
(#3) Application does not have the capability to make this API call.
```

따라서 현재 배포 전략:

- 수신형 MVP 중심 배포
- 자동 발신은 dry-run 유지

## 10. 이후 운영 전환 단계

배포 후 또는 사업/비즈니스 조건이 준비되면:

1. Meta App Review 제출
2. 필요한 권한 Advanced Access 확보
3. 필요 시 비즈니스 포트폴리오 / 비즈니스 인증 진행
4. `DM_INSTAGRAM_DRY_RUN=false` 전환
5. 실제 send live 재검증

## 최종 체크

배포 직전 아래 질문에 모두 `예`라고 답할 수 있어야 합니다.

- 고정 HTTPS 도메인이 있는가
- OAuth redirect URI가 고정 도메인으로 설정되었는가
- Webhook callback URL이 고정 도메인으로 설정되었는가
- legal 문서가 공개 URL에서 실제 열리는가
- DM 수신/Inbox/운영자 수동 처리 흐름이 정상 동작하는가
- 현재 발신 모드가 `DRY_RUN`인지 운영자가 UI에서 알 수 있는가
