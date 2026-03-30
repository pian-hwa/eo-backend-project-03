# 🚀 33Chat - 종합 AI 포털 서비스

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)

사용자의 멤버십(플랜) 등급에 따라 일반 AI 대화, 웹 페이지 요약, 유튜브 영상 요약 등 다양한 인공지능 기능을 제공하는 종합 AI 포털 플랫폼 백엔드 프로젝트입니다.

---

## 👨‍👩‍👦 팀 소개 (Team 삼삼드래)
| 역할 | 이름 |
| :---: | :--- |
| **👑 팀장** | **윤상이** |
| **🧑‍💻 팀원** | **현길용** |
| **🧑‍💻 팀원** | **김희성** |

---

## 🛠 기술 스택 (Tech Stack)

### Backend
* **Language:** Java
* **Framework:** Spring Boot 3.x
* **Security:** Spring Security, JWT (JSON Web Token), OAuth2
* **Database:** JPA (Hibernate), H2 Database (개발용)

### APIs & External Services
* **AI Service:** Alan AI API (KDT API - Azure Functions 연동)
* **Payment:** PortOne V2 API (카카오페이 등 간편결제 연동)
* **Email:** Spring Boot Starter Mail (SMTP)
* **API Docs:** Swagger (Springdoc OpenAPI)

### Tool
* **IDE:** IntelliJ IDEA
* **Build Tool:** Gradle

---

## 💡 핵심 기능 (Key Features)

### 1. 사용자 인증 및 인가 (Auth & Security)
* **자체 로그인 및 소셜 로그인(구글):** OAuth2와 JWT(HttpOnly 쿠키)를 활용한 안전한 인증 처리
* **이메일 인증 시스템:** 무분별한 가입 방지를 위한 회원가입 전 이메일 인증 번호(6자리) 발송 및 1분 쿨타임(Rate Limit) 적용
* **비밀번호 재설정:** 가입된 이메일로 비밀번호 재설정 전용 토큰(UUID) 발송
* **소셜 계정 방어 로직:** 소셜 가입자의 무분별한 비밀번호 변경 시도 원천 차단 (`UserProvider` Enum 적용)

### 2. 구독형 멤버십 시스템 (Plans & Payment)
* **플랜 등급:** `BASIC`(무료), `PRO`(9,900원), `PREMIUM`(19,900원) 3단계 플랜 제공
* **결제 연동:** 포트원(PortOne) V2 브라우저 SDK를 활용한 실제 결제 처리 및 백엔드 금액 위변조 검증 로직 구현
* **스케줄러 자동화 (`@Scheduled`):** 매일 자정 사용자별 잔여 AI 토큰 초기화 및 30일 구독 만료 시 `BASIC` 플랜 자동 강등 처리

### 3. 지능형 AI 채팅 (AI Chat)
* **프롬프트 엔지니어링:** 질문 유형(일반 대화, 웹 요약, 유튜브 요약)에 따른 동적 프롬프트 생성 (`ChatService`)
* **일반 대화:** gpt-3.5-turbo, gpt-4o-mini 등 모델 선택 기능 (BASIC 플랜은 gpt-4 사용 제한)
* **웹 페이지 요약:** 복잡한 웹 문서를 3문장 내외로 핵심만 요약 번역 (PRO 플랜 이상 전용)
* **유튜브 영상 요약:** 유튜브 자막(Transcript)을 직접 추출하여 영상 내용 요약 (PREMIUM 플랜 전용)
* **세션 관리:** `sessionId`를 통한 대화 기록 분리 및 과거 대화 내역 조회 기능

### 4. 관리자 대시보드 (Admin)
* **유저 관리:** 전체 가입 유저 조회 및 악성 유저 상태(ACTIVE / LOCKED / WITHDRAWN) 강제 제어
* **플랜 관리:** 서비스 운영 중 각 플랜의 가격, 하루 제공 토큰량, 사용 가능 모델 등 즉각 수정 기능
* **통계 기능:** 누적 결제 매출 총액, 플랜별 가입자 통계, AI 모델별 누적 사용 토큰량 등 대시보드 데이터 제공

---

## ⚙️ 실행 방법 (Getting Started)

1. 프로젝트 클론
```bash
git clone [프로젝트 레포지토리 주소]
```

2.application-secret.yml 파일 설정
```bash
src/main/resources/ 경로에 아래 형식의 파일을 생성하고 본인의 API 키를 입력하세요.

mail:
  username: "본인_구글_이메일@gmail.com"
  password: "앱_비밀번호_16자리"

jwt:
  secret-key: "본인이_생성한_길고_안전한_랜덤_문자열"

portone:
  api:
    store-id: "포트원_상점_아이디"
    kakao-channel-key: "카카오페이_채널_키"
    secret: "포트원_V2_시크릿_키"

alan:
  api:
    key: "발급받은_Alan_API_키"
```

3.IntelliJ IDEA 실행 및 빌드
```bash
IntelliJ IDEA에서 프로젝트를 열고 Gradle을 동기화(Sync) 합니다.
ChatApplication.java의 Main 메서드를 실행합니다. (bootRun)
```
