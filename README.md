# AI 기반 멀티테넌트 예약 관리 SaaS

AI 자연어 예약 자동 등록 기능이 포함된 B2B 예약 관리 SaaS  
Spring Boot + PostgreSQL + AWS 기반 실전 SaaS 아키텍처 구현

---

## 프로젝트 목표

- 멀티테넌트 SaaS 구조 설계 및 구현
- 예약 동시성 처리 및 트랜잭션 전략 설계
- LLM 기반 자연어 예약 자동 등록
- AWS 배포 및 실제 결제 연동
- 실제 유료 전환 테스트

---

## 전체 아키텍처
Client (Web)
↓
Spring Boot API Server
↓
PostgreSQL (RDS)
↓
OpenAI API


- 멀티테넌트 구조 (organization_id 기반 논리 분리)
- JWT 인증 + Role 기반 접근 제어
- AI 요청 로그 및 파싱 실패 케이스 저장
- 결제 모듈 (토스 / Stripe)

---

## 핵심 기술 포인트

### 1. 멀티테넌트 구조

- Shared DB + Shared Schema
- 모든 주요 테이블에 organization_id 포함
- (organization_id, ...) 인덱스 설계
- 조직 단위 데이터 완전 분리

### 2. 예약 동시성 처리

- 트랜잭션 기반 예약 충돌 방지
- `SELECT ... FOR UPDATE` 전략 적용
- 슬롯락 테이블 기반 유니크 제약 설계

### 3. AI 자동 등록 설계

- OpenAI JSON Schema 강제 출력
- AI 요청/응답 로그 테이블 설계
- confidence 기반 승인 흐름
- 프롬프트 버전 관리

### 4. 결제 무결성

- provider_payment_key UNIQUE 제약
- order_id UNIQUE 제약
- 웹훅 중복 처리 방지 설계

---

## ERD 개요

### 주요 테이블

- users
- organizations
- organization_memberships
- customers
- services
- reservations
- ai_requests
- subscription_plans
- organization_subscriptions
- payments

(ERD 다이어그램 이미지 삽입 예정)

---

## 기술 스택

### Backend
- Java 21
- Spring Boot 3.x
- JPA (Hibernate)

### Database
- PostgreSQL
- DBeaver

### AI
- OpenAI API (JSON Schema 응답)

### Infra
- AWS EC2
- AWS RDS
- Nginx
- HTTPS

---

## 주요 기능

### 인증 및 인가
- JWT 기반 인증
- 조직별 Role 기반 접근 제어

### 예약 관리
- 예약 생성 / 수정 / 취소
- 예약 충돌 방지 로직
- 상태 관리 (CONFIRMED / CANCELLED 등)

### AI 예약 자동 등록
- 자연어 입력을 예약 데이터로 파싱
- 실패 케이스 로그 저장
- 관리자 승인 프로세스

### 결제 시스템
- 구독 플랜 기반 과금
- 결제 상태 추적
- 웹훅 처리

---

## 트랜잭션 전략

- 예약 생성 시 동시성 제어
- AI 요청 → 예약 생성 원자성 보장
- 결제 성공 시 구독 상태 갱신 트랜잭션 처리

---

## 수익화 전략

- Free / Basic / Pro 구독 모델
- AI 요청 수 제한
- 예약 개수 제한
- 실제 유료 전환 테스트 진행

---

## 배포 구조
AWS EC2
├─ Spring Boot (Docker)
├─ Nginx Reverse Proxy
└─ HTTPS 적용

AWS RDS (PostgreSQL)

---

## 향후 개선 계획

- 예약 시간 EXCLUDE 제약 기반 DB 레벨 충돌 방지
- Redis 캐싱 도입
- 관리자 대시보드 고도화
- AI 비용 최적화 전략 적용
- SaaS 멀티 리전 확장

---

## 프로젝트에서 배운 점

- 멀티테넌트 설계에서 데이터 격리 전략의 중요성
- 트랜잭션 설계가 SaaS 안정성에 미치는 영향
- LLM을 실제 서비스에 적용할 때 필요한 실패 케이스 관리
- 결제 시스템 무결성 설계의 중요성

---

## 한 줄 요약

멀티테넌트 SaaS 구조 설계부터 AI 연동, AWS 배포, 실제 결제 연동까지 전 과정을 구현한 실전형 백엔드 프로젝트
