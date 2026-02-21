# ETF 구성종목 조회 서비스 - Backend

**TIME ETF** 5종의 구성종목 데이터를 `timeetf.co.kr`에서 스크래핑하여 H2 데이터베이스에 저장하고, REST API로 프론트엔드에 제공하는 Spring Boot 백엔드 서버입니다.

---

## 목차

- [주요 기능](#주요-기능)
- [지원 ETF](#지원-etf)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [아키텍처 개요](#아키텍처-개요)
- [API 명세](#api-명세)
- [데이터 모델](#데이터-모델)
- [스크래핑 동작 방식](#스크래핑-동작-방식)
- [시작하기](#시작하기)
- [설정](#설정)
- [배포](#배포)

---

## 주요 기능

### 1. 자동 스크래핑 (스케줄러)
- 평일(월~금) 매일 **오후 6시**에 5개 ETF 구성종목을 자동 수집
- 당일 데이터가 이미 존재하면 중복 저장 없이 스킵
- 스케줄 cron 표현식은 `application.yml`에서 변경 가능

### 2. 수동 스크래핑 (API)
- `POST /api/etf/scrape` 호출 시 즉시 오늘 날짜 데이터 수집
- ETF별 스크래핑 성공 / 이미존재 / 데이터없음 / 실패 상태를 상세 응답으로 반환

### 3. 구성종목 조회 API
- 수집된 날짜 목록, 특정 날짜의 ETF 목록, ETF별 구성종목 조회 제공
- 종목코드, 종목명, 수량, 평가금액, 비중(%) 반환

### 4. 페이지 프록시 API
- 프론트엔드가 `timeetf.co.kr`을 직접 요청할 때 발생하는 CORS 문제를 서버 사이드에서 우회
- `GET /api/etf/proxy/page`를 통해 외부 HTML을 클라이언트에 전달

### 5. CORS 설정
- 로컬 개발(`localhost:5173`, `localhost:3000`) 및 프로덕션 도메인(`stocksheetproject.vercel.app`) 허용

---

## 지원 ETF

| ETF명 | 분류 | timeetf idx |
|---|---|---|
| TIME Korea플러스배당액티브 | 배당 | 12 |
| TIME K신재생에너지액티브 | 신재생에너지 | 16 |
| TIME K이노베이션액티브 | 이노베이션 | 17 |
| TIME 코리아밸류업액티브 | 밸류업 | 15 |
| TIME 코스피액티브 | 코스피 | 11 |

ETF URL 및 표시명은 `EtfInfo.java` enum에서 관리합니다.

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.4.1 |
| ORM | Spring Data JPA / Hibernate |
| 데이터베이스 | H2 (파일 모드, `./data/etfdb`) |
| 스크래핑 | Jsoup 1.18.3 |
| 스케줄러 | Spring `@Scheduled` |
| 빌드 도구 | Gradle |
| 배포 환경 | AWS EC2 (Ubuntu), 포트 8088 |

---

## 프로젝트 구조

```
etf-backend/
├── src/
│   └── main/
│       ├── java/com/etf/
│       │   ├── EtfBackendApplication.java      # 애플리케이션 진입점 (@EnableScheduling)
│       │   ├── config/
│       │   │   └── WebConfig.java              # CORS 허용 도메인 설정
│       │   ├── controller/
│       │   │   └── EtfController.java          # REST API 컨트롤러
│       │   ├── domain/
│       │   │   ├── EtfHolding.java             # 구성종목 JPA 엔티티
│       │   │   └── EtfInfo.java                # ETF 정보 enum (이름, URL)
│       │   ├── dto/
│       │   │   └── ScrapeResult.java           # 스크래핑 결과 DTO
│       │   ├── repository/
│       │   │   └── EtfHoldingRepository.java   # JPA 레포지토리
│       │   └── service/
│       │       ├── ScrapingService.java        # 스크래핑 핵심 로직
│       │       └── ScrapingScheduler.java      # 자동 스케줄러
│       └── resources/
│           └── application.yml                 # 데이터소스, JPA, 스케줄 설정
├── data/
│   └── etfdb.mv.db                             # H2 파일 DB (런타임 생성)
├── build.gradle
├── settings.gradle
├── gradlew
└── deploy.sh                                   # EC2 빌드 & 배포 스크립트
```

---

## 아키텍처 개요

```
[timeetf.co.kr]
      │
      │ Jsoup HTTP 스크래핑
      ▼
[ScrapingScheduler]  ←── 평일 18:00 자동 실행
[ScrapingService  ]  ←── POST /api/etf/scrape 수동 실행
      │
      │ JPA saveAll()
      ▼
[H2 Database (etfdb.mv.db)]
      │
      │ JPA 조회
      ▼
[EtfController] ──── REST API ────▶ [Frontend (Vercel)]
```

---

## API 명세

### Base URL

- 로컬: `http://localhost:8088`
- 프로덕션: `http://13.125.139.31:8088`

---

### 1. 수집된 날짜 목록 조회

```
GET /api/etf/dates
```

**Response** `200 OK`

```json
["2025-01-20", "2025-01-17", "2025-01-16"]
```

날짜 내림차순 정렬로 반환합니다.

---

### 2. 특정 날짜의 ETF 목록 조회

```
GET /api/etf/dates/{date}/etfs
```

**Path Parameter**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `date` | `LocalDate` (ISO 형식: `yyyy-MM-dd`) | 조회할 날짜 |

**Response** `200 OK`

```json
[
  "TIME Korea플러스배당액티브",
  "TIME K신재생에너지액티브",
  "TIME K이노베이션액티브",
  "TIME 코리아밸류업액티브",
  "TIME 코스피액티브"
]
```

---

### 3. 특정 ETF의 구성종목 조회

```
GET /api/etf/dates/{date}/holdings?etfName={etfName}
```

**Path / Query Parameter**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `date` | `LocalDate` (ISO 형식) | 조회할 날짜 |
| `etfName` | `String` | ETF 표시명 (예: `TIME 코스피액티브`) |

**Response** `200 OK`

```json
[
  {
    "id": 1,
    "etfName": "TIME 코스피액티브",
    "orderNo": 1,
    "stockCode": "005930",
    "stockName": "삼성전자",
    "quantity": 100000,
    "valuationAmount": 7500000000,
    "weight": 25.30,
    "scrapeDate": "2025-01-20"
  },
  ...
]
```

> `orderNo`가 `-1`인 항목은 현금·원화 등 비종목 항목입니다.

---

### 4. 수동 스크래핑 트리거

```
POST /api/etf/scrape
```

오늘 날짜 기준으로 5개 ETF를 즉시 스크래핑합니다. 당일 데이터가 이미 존재하면 해당 ETF는 스킵됩니다.

**Response** `200 OK`

```json
{
  "message": "스크래핑 완료",
  "date": "2025-01-20",
  "totalNewHoldings": 142,
  "details": [
    {
      "etfName": "TIME 코스피액티브",
      "status": "SCRAPED",
      "holdingsCount": 30,
      "message": "30건 스크래핑 완료"
    },
    {
      "etfName": "TIME Korea플러스배당액티브",
      "status": "ALREADY_EXISTS",
      "holdingsCount": 0,
      "message": "오늘(2025-01-20) 데이터가 이미 존재합니다 (28건)"
    }
  ]
}
```

**스크래핑 상태 코드**

| status | 설명 |
|---|---|
| `SCRAPED` | 새로운 데이터 수집 성공 |
| `ALREADY_EXISTS` | 당일 데이터 이미 존재 (스킵) |
| `EMPTY` | 외부 사이트에서 데이터 없음 |
| `FAILED` | 스크래핑 중 오류 발생 |

---

### 5. 외부 페이지 프록시

```
GET /api/etf/proxy/page?idx={idx}&cate={cate}&pdfDate={pdfDate}
```

**Query Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `idx` | `int` | Y | ETF 페이지 인덱스 |
| `cate` | `String` | Y | 카테고리 코드 |
| `pdfDate` | `String` | N | 조회 기준일 |

**Response**

- `200 OK`: `text/html` 형식의 외부 페이지 HTML
- `502 Bad Gateway`: 외부 요청 실패 시

---

## 데이터 모델

### EtfHolding (테이블: `etf_holding`)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | `BIGINT` (PK, AUTO_INCREMENT) | 기본키 |
| `etf_name` | `VARCHAR` (NOT NULL) | ETF 표시명 |
| `order_no` | `INTEGER` | 종목 순번 (`-1`이면 현금성 항목) |
| `stock_code` | `VARCHAR` | 종목코드 |
| `stock_name` | `VARCHAR` (NOT NULL) | 종목명 |
| `quantity` | `BIGINT` | 보유 수량 |
| `valuation_amount` | `BIGINT` | 평가금액 (원) |
| `weight` | `DOUBLE` | 비중 (%) |
| `scrape_date` | `DATE` (NOT NULL) | 수집 날짜 |

**인덱스**

| 인덱스명 | 컬럼 | 용도 |
|---|---|---|
| `idx_scrape_date` | `scrapeDate` | 날짜 목록 조회 성능 |
| `idx_date_etf` | `scrapeDate, etfName` | 날짜+ETF명 복합 조회 성능 |

---

## 스크래핑 동작 방식

1. `EtfInfo` enum에 정의된 5개 ETF URL에 Jsoup으로 HTTP GET 요청
2. `table.moreList1 tbody tr` 셀렉터로 구성종목 테이블 파싱
3. 각 행에서 종목코드, 종목명, 수량, 평가금액, 비중 추출
4. 종목명이 비어있으면 스킵, "현금" / "원화" 포함 항목은 `orderNo = -1` 처리
5. 파싱된 데이터를 `EtfHolding` 엔티티로 변환 후 `saveAll()` 일괄 저장

---

## 시작하기

### 요구 사항

- Java 17 이상
- Gradle (또는 `./gradlew` Wrapper 사용)

### 로컬 실행

```bash
# 저장소 클론
git clone https://github.com/<your-username>/etf-backend.git
cd etf-backend

# 빌드 없이 바로 실행
./gradlew bootRun
```

서버가 `http://localhost:8088` 에서 시작됩니다.

### 빌드

```bash
./gradlew clean bootJar -x test
# 결과물: build/libs/etf-backend-0.0.1-SNAPSHOT.jar
```

### H2 콘솔

로컬 실행 중 DB를 직접 확인하려면 브라우저에서 접속합니다.

```
http://localhost:8088/h2-console
JDBC URL : jdbc:h2:file:./data/etfdb
Username : sa
Password : (공백)
```

---

## 설정

`src/main/resources/application.yml`에서 주요 설정을 변경할 수 있습니다.

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/etfdb;AUTO_SERVER=TRUE   # H2 파일 DB 경로
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update    # 스키마 자동 생성/업데이트
    show-sql: false
  h2:
    console:
      enabled: true
      path: /h2-console

etf:
  scrape:
    cron: "0 0 18 * * MON-FRI"   # 평일 18:00 자동 스크래핑
```

스케줄 cron 형식은 Spring `@Scheduled` 6-field 표현식을 따릅니다 (`초 분 시 일 월 요일`).

---

## 배포

AWS EC2 (Ubuntu, `13.125.139.31:8088`) 에 `deploy.sh` 스크립트로 배포합니다.

### 사전 준비

- EC2 접속용 PEM 키 파일: `~/topcomEc2Key.pem`
- EC2 보안 그룹에서 **8088 포트 인바운드** 허용 필요

### 배포 실행

```bash
chmod +x deploy.sh
./deploy.sh
```

스크립트 실행 순서:

```
[1/4] Gradle clean bootJar (테스트 제외)
[2/4] 원격 서버 디렉토리 생성 (mkdir -p ~/etf-backend/data)
[3/4] SCP로 JAR 파일 전송
[4/4] 기존 프로세스 종료 → nohup으로 백그라운드 재시작
```

### 로그 확인

배포 후 서버에서 실시간 로그를 확인합니다.

```bash
ssh -i ~/topcomEc2Key.pem ubuntu@13.125.139.31
tail -f /home/ubuntu/etf-backend/app.log
```

---

## 라이선스

This project is for personal use. All rights reserved.
