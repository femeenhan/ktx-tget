# ktx-tget 판매·배포 웹 (tget.bigseasons.co.kr) 설계

작성일: 2026-10-30. 상태: 검토자 질문 반영, 사용자 승인 대기.

## 1. 의도

ktx-tget은 코레일톡 취소표를 감지해 예매 직전까지 자동으로 진행하는 안드로이드 접근성 앱이다. 지금까지 한 번도 판매·홍보한 적이 없고, 배포 방식은 "APK를 카톡으로 보내고 → 구매자가 기기 ID를 보내면 → 판매자가 HMAC 키를 만들어 답장"하는 수작업이다(`docs/buyer_setup_guide.md`).

이 프로젝트의 목적은 **첫 판매를 만들 수 있는 최소한의 공개 창구**를 세우는 것이다. 커뮤니티 글을 보고 들어온 사람이 혼자서 다운로드·설치·활성화 요청까지 끝내고, 판매자는 입금 확인 후 버튼 하나로 승인하면 된다.

## 2. 대상과 유입

- **명절 유입**: 기차표를 놓치고 커뮤니티 글을 타고 급하게 오는 사람. 지금 당장 돌아가게 하는 것이 전부. 월 단위 또는 영구 구매.
- **주말 여행 유입**: 매주 부산·대전·경주행 표를 잡느라 새로고침하는 사람. 반복 수요. 1년 단위가 어울림.
- 둘 다 비개발자이며, 사이드로드 설치를 처음 해 보는 사람으로 가정한다.

## 3. 목표 / 비목표 / 실패 조건

**목표**
1. `https://tget.bigseasons.co.kr` 한 링크로 홍보·다운로드·설치 안내가 끝난다.
2. 구매자는 앱 안에서 「활성화 요청」 한 번으로 요청을 보내고, 판매자 승인 후 자동으로 풀린다. 기기 ID·키를 복사해 주고받지 않는다.
3. 판매자는 관리 화면에서 요청 목록을 보고 승인·만료·회수하고, 새 APK를 올린다.
4. 라이선스는 영구 / 1개월 / 1년 세 종류. 만료 시 같은 흐름으로 연장 요청.
5. 결제는 계좌이체 + 판매자 수동 승인. 승인 전까지 앱은 대기 상태.

**비목표(이번에 하지 않음)**
- PG(토스페이먼츠 등) 결제 연동·자동 승인.
- 회원가입·로그인. 구매자는 계정이 없고 기기 ID로만 식별한다.
- Play 스토어·갤럭시 스토어 등록.
- 무료 체험 기간.
- 매출 통계, 이메일 발송.

**실패 조건(동작해도 실패인 것)**
- 커뮤니티에서 온 사람이 다운로드 버튼을 못 찾거나, 설치 안내가 길어서 이탈한다.
- 승인했는데 구매자 앱이 풀리지 않아 카톡으로 "안 돼요"가 온다.
- 판매자가 APK를 갱신하려면 코드를 만져야 한다.
- 기존 `bigseasons.co.kr` 홈페이지(GitHub Pages)에 영향을 준다.

## 4. 제약

- 기존 홈페이지는 GitHub Pages 정적 호스팅. 건드리지 않는다. 서브도메인만 추가한다.
- 도메인은 가비아에서 등록. DNS 설정도 가비아에서 한다.
- 앱은 Kotlin/Compose, 외부 네트워크 라이브러리 없음, 현재 `INTERNET` 권한 없음.
- 현재 release 빌드가 **debug 키스토어로 서명**된다(`app/build.gradle.kts`). 이 상태로 배포하면 개발 PC가 바뀔 때 기존 설치 위에 업데이트가 안 된다.
- 판매자 혼자 운영한다. 관리 화면 사용자는 1명.
- 문의 창구는 카카오톡 오픈채팅 하나.

## 5. 사용자가 정한 것

| 항목 | 결정 |
|---|---|
| 호스팅 | Vercel, 서브도메인 `tget.bigseasons.co.kr` |
| 결제 | 계좌이체, 판매자 수동 승인. 승인 전까지 대기(체험 없음) |
| 라이선스 종류 | 영구 1회, 1개월, 1년 |
| 문의 | 카카오톡 오픈채팅 링크 + 이메일 bigseasons.dev@gmail.com |
| 유입 | 커뮤니티 글 → 랜딩 페이지 |
| 판매 주체 | 개인사업자 「빅시즌스」로 판매. 사업자 표기·이용약관·개인정보 처리방침을 사이트에 둔다 |
| 환불 | 승인 전 전액 환불, 승인 후 환불 불가. 요청 전에 동의 체크 |

## 6. 설계자(Claude)가 정한 것

사용자가 위임한 부분이며, 검토 시 바꿔도 된다.

- **코드 위치**: 이 저장소의 `web/` 디렉터리에 Next.js(App Router, TypeScript) 프로젝트. Vercel 프로젝트의 Root Directory를 `web`으로 지정. 앱과 서버의 API 계약이 한 저장소에 있어 같이 바뀐다.
- **DB**: Vercel Marketplace의 Neon Postgres 무료 티어. 테이블 2개(`devices`, `releases`)뿐이라 ORM 없이 `@neondatabase/serverless`로 SQL 직접 작성.
- **APK 저장**: Vercel Blob. 관리 화면에서 업로드한다(저장소가 비공개라 GitHub Release 링크는 쓸 수 없음).
- **관리자 인증**: 환경변수 `ADMIN_PASSWORD` 하나. 로그인 폼 → 서명된 httpOnly 쿠키(만료 30일). 사용자 1명이라 계정 테이블을 두지 않는다.
- **앱-서버 식별**: 기기 ID(`ANDROID_ID`)만으로 식별한다. 별도 토큰을 두지 않는다. 앱을 지우고 다시 설치해도 기기 ID가 같아 그대로 풀리고, 남의 기기 ID로 상태를 조회하는 위험은 실익이 거의 없어 감수한다. 공장 초기화·기기 교체는 기기 ID가 바뀌므로 관리 화면에서 "기존 행 회수 + 새 기기 승인"으로 처리하고 `/guide`에 적는다. (ANDROID_ID는 앱 서명 키별로 달라지므로 §10의 1단계 키스토어 교체가 먼저다.)
- **만료 전 연장**: 활성 상태에서도 「연장 요청」을 보낼 수 있다. `devices.pending_plan`에 요청 기간을 적고, 관리 화면에서 승인하면 현재 만료일에 기간을 더한다(영구로 승인하면 만료 없음).
- **테스트 환경**: release 빌드도 `local.properties`의 `apiBaseUrl`로 서버 주소를 바꿀 수 있다. Vercel Preview 배포(별도 Neon 브랜치)를 테스트 서버로 쓴다. 관리 화면에 행 「삭제」 버튼을 둔다.
- **가격·계좌·공지**: `settings` 테이블(key/value)에 두고 관리 화면 「설정」에서 수정. 앱은 `GET /api/config`로 가격·계좌·오픈채팅 링크를 받아 요청 카드에 표시한다.
- **APK 업로드**: Vercel 함수 본문 한도(4.5MB)를 넘으므로 `@vercel/blob/client`로 브라우저에서 Blob에 직접 올린다.
- **1개월 계산**: 달력 기준 +1개월(1/31 → 2/28). 1년은 +12개월.
- **입금자명 보관**: 승인 또는 회수 30일 뒤(연장 요청 중인 행은 제외, 연장 승인 시 다시 셈), 승인되지 않은 대기 행은 요청 30일 뒤 `depositor`를 지운다. 만료·회수 1년 뒤에는 행을 지운다(Vercel Cron 하루 한 번).
- **오프라인 유예**: 앱은 마지막으로 서버에서 받은 상태와 만료일을 저장하고, 서버에 닿지 못해도 **마지막 성공 확인으로부터 72시간**은 저장된 상태를 믿는다. 명절 새벽에 서버가 잠깐 죽어도 매크로가 멈추지 않게 하기 위함.
- **기존 HMAC 키 방식 제거**: 판매 이력이 없으므로 호환 유지 없이 `DeviceLicense`를 서버 방식으로 교체한다. `LICENSE_SECRET`, `scripts/generate_key.sh`, `build_customer_apk.sh`는 삭제.
- **release 서명 키**: 정식 키스토어를 만들어 `local.properties`(또는 환경변수)로 경로·비밀번호를 읽도록 바꾼다. 키스토어 파일은 커밋하지 않는다(`.gitignore`에 이미 `*.jks` 있음). 이 키를 잃어버리면 기존 구매자 기기에 업데이트를 덮어쓸 수 없으므로, 백업 위치를 README에 적는다.
- **입금 매칭**: 활성화 요청 화면에서 **입금자명**(필수)과 **원하는 기간**(영구/1개월/1년)을 입력받는다. 관리 화면에서 입금자명으로 통장 내역과 대조한다.
- **버전 체크**: 앱이 시작할 때 `/api/version`을 읽어 더 높은 `versionCode`가 있으면 홈 상단에 「새 버전 받기」 배너. 강제 업데이트는 하지 않는다.

## 7. 구조

```
tget.bigseasons.co.kr
├── /                 랜딩(홍보)
├── /download         다운로드 + 설치 안내
├── /guide            사용 안내(설치 후 설정·사용법, 현재 buyer_setup_guide 4~5단계)
├── /terms            이용약관(환불 규정 포함)
├── /privacy          개인정보 처리방침
├── /admin            관리(로그인 필요)
│   ├── /admin/devices    요청·라이선스 목록
│   ├── /admin/releases   APK 업로드·버전 목록
│   └── /admin/settings   가격·계좌·오픈채팅·공지
└── /api
    ├── POST /api/device/request   활성화 요청
    ├── GET  /api/device/status    상태 조회
    ├── GET  /api/version          최신 APK 정보(공개)
    ├── GET  /api/config           가격·계좌·오픈채팅 링크(공개)
    └── (admin 전용) 승인·연장·회수·삭제·업로드·설정
```

### 7.1 랜딩 `/`

커뮤니티에서 온 사람이 30초 안에 "뭘 해주는지 / 얼마인지 / 지금 어떻게 받는지"를 알게 한다. 위에서부터:

1. 한 문장 헤드라인: 취소표가 뜨면 알아서 예매 직전까지 가 준다. 결제는 직접 한다.
2. 「APK 다운로드」 버튼(→ `/download`)과 「문의」 버튼(→ 오픈채팅).
3. 실제 동작 화면 캡처 3~4장(코레일톡 목록 → 시트 열림 → 결제 화면 알림).
4. 가격표: 영구 / 1개월 / 1년. 계좌이체 안내 한 줄.
5. 설치가 어려운 이유(접근성 앱이라 스토어에 없음)와 안전함을 짧게.
6. 주의: 코레일 약관상 제재 가능성은 사용자 판단. 안드로이드 전용(iOS 불가).
7. 푸터(모든 페이지 공통): 상호 빅시즌스 · 대표 한철민 · 사업자등록번호 674-66-00675 · 사업장 소재지 서울특별시 은평구 통일로 796(동·호수는 사용자 결정으로 생략) · 통신판매업 신고번호(신고 후 기입, 그 전에는 「신고 준비 중」) · 이메일 bigseasons.dev@gmail.com · 오픈채팅 링크 · 이용약관 · 개인정보 처리방침.

### 7.1a 이용약관 `/terms`·개인정보 처리방침 `/privacy`

- 약관 핵심: 디지털 상품이며 **승인 즉시 제공이 시작되어 승인 후에는 청약철회·환불이 불가**함을 명시. 승인 전에는 입금액 전액 환불. 기기 1대당 1라이선스, 기기 교체 시 관리자 확인 후 이전. 코레일 약관 위반에 따른 불이익은 구매자 책임. 코레일톡 변경으로 동작이 중단될 수 있고, 대응 업데이트는 제공하되 기간을 보장하지 않음.
- 처리방침 핵심: 수집 항목은 기기 식별값(ANDROID_ID), 입금자명, 앱 버전, 접속 시각. 목적은 라이선스 발급·입금 확인. 입금자명은 승인·회수 또는 요청 후 30일 뒤 삭제, 기기 식별값·이용 기록은 만료·회수 후 1년 뒤 삭제. 처리 위탁: Vercel(호스팅), Neon(DB). 문의 이메일.
- 앱의 활성화 요청 카드에 「승인 후 환불 불가에 동의합니다」 체크박스(필수)와 약관 링크를 둔다. 동의 시각을 `devices.agreed_at`에 저장한다.

디자인은 앱과 같은 sage/ink 톤(ktx_tget 0.2.0 UI). 구현 전에 `ui-ux` 스킬을 따른다(사용자 전역 지시).

### 7.2 다운로드 `/download`

- 최신 APK 버튼(파일명 `ktx-tget-<version>.apk`, 크기, 올린 날짜).
- 그 아래 사이드로드 안내를 **그림 위주로** 3단계: Play 프로텍트/Auto Blocker 끄기 → 출처 허용 → 설치. 현재 `buyer_setup_guide.md` 1단계 내용을 옮기되 글자를 절반 이하로 줄인다.
- 카톡 zip 배포는 더 이상 필요 없으므로 안내에서 뺀다.

### 7.3 관리 `/admin/devices`

표 한 장. 열: 요청 시각, 입금자명, 원하는 기간, 기기 ID(앞 6자), 상태(대기/활성/만료/회수), 만료일, 메모, 동작 버튼.

- 대기 행: 「승인」 누르면 기간 선택(요청한 기간이 기본값) → 확인. 승인 시각 + 기간 = 만료일. 영구는 만료 없음.
- 활성 행: 「연장」(기간 더하기; `pending_plan`이 있으면 그 기간이 기본값이고 행에 「연장 요청」 배지), 「회수」(즉시 잠금).
- 모든 행: 「삭제」(테스트 행 정리용, 확인 창).
- 메모 칸은 자유 입력(구매자 카톡 닉네임 등).
- 기본 정렬: 대기 → 활성 → 나머지, 최신순. 대기 건수는 상단에 크게.

### 7.4 관리 `/admin/releases`

- 업로드 폼: APK 파일, versionCode, versionName, 변경 내용 한 줄. 업로드하면 Vercel Blob에 저장하고 `releases`에 행 추가, 자동으로 최신이 된다.
- 목록에서 이전 버전을 최신으로 되돌릴 수 있다(코레일톡 대응이 잘못됐을 때).

### 7.5 DB

```sql
devices (
  device_id     text primary key,     -- ANDROID_ID, 소문자
  depositor     text,                  -- 입금자명(승인/회수 30일 뒤 null)
  requested_plan text not null,        -- 'lifetime' | 'month' | 'year'
  pending_plan  text,                  -- 활성 중 연장 요청 기간(없으면 null)
  status        text not null,         -- 'pending' | 'active' | 'expired' | 'revoked'
  expires_at    timestamptz,           -- null = 영구
  agreed_at     timestamptz not null,  -- 환불 불가 동의 시각
  note          text,
  app_version   int,                   -- 마지막 확인 때 보낸 versionCode
  requested_at  timestamptz not null,
  approved_at   timestamptz,
  last_seen_at  timestamptz
)
releases (
  id            serial primary key,
  version_code  int not null unique,
  version_name  text not null,
  blob_url      text not null,
  size_bytes    bigint not null,
  notes         text,
  is_latest     boolean not null default false,
  uploaded_at   timestamptz not null
)
settings (
  key           text primary key,      -- price_lifetime, price_month, price_year, bank_account, openchat_url, notice
  value         text not null
)
```

`expired`는 저장하지 않고 조회 시 `status='active' and expires_at < now()`로 계산해도 된다(구현자 선택).

### 7.6 API 계약

모든 응답 JSON. 앱은 `HttpURLConnection`으로 호출(의존성 추가 없음).

**POST /api/device/request**
요청 `{ deviceId, depositor, plan, versionCode, agreed: true }`
- 처음이면 행 생성(`pending`). 이미 있으면: `pending`이면 입금자명·기간만 갱신, `active`면 `pending_plan`에 기간을 적고(연장 요청) 상태는 그대로, `expired`/`revoked`면 `pending`으로 되돌리고 갱신.
- `agreed`가 true가 아니면 `400`.
- 응답: 아래 status와 같은 형태.

**GET /api/device/status?deviceId=&versionCode=**
- 응답 `{ status: 'none'|'pending'|'active'|'expired'|'revoked', expiresAt: string|null, renewalPending: boolean, serverTime: string, notice: string|null }`
- `last_seen_at`, `app_version` 갱신.

**GET /api/config**
- 응답 `{ prices: { lifetime, month, year }, bankAccount, openchatUrl, termsUrl }`.
- `notice`는 관리 설정의 공지(예: "코레일톡 7.2 대응 중"). 앱 홈에 그대로 표시.

**GET /api/version**
- 응답 `{ versionCode, versionName, url, sizeBytes, notes }` (최신 릴리스).

**admin** (쿠키 필요): `POST /api/admin/devices/:id/approve {plan}`, `/extend {plan}`(pending_plan 승인 포함), `/revoke`, `/note`, `DELETE /api/admin/devices/:id`, `POST /api/admin/releases`(Blob 클라이언트 업로드 완료 후 메타데이터 등록), `/api/admin/releases/:id/latest`, `PUT /api/admin/settings`.

**cron** `GET /api/cron/cleanup` (Vercel Cron, 매일): 승인/회수 30일 지난 행의 `depositor`를 null로.

### 7.7 앱 변경 (`app/`)

- `AndroidManifest.xml`에 `INTERNET` 권한.
- `BuildConfig`에 `API_BASE_URL`(기본 `https://tget.bigseasons.co.kr`, debug·release 모두 `local.properties`의 `apiBaseUrl`로 덮어쓰기 가능).
- `DeviceLicense` → 서버 상태 기반으로 재작성. 저장: 마지막 상태, `expires_at`, `last_checked_at`. `isLicensed()` 판단:
  - 상태 `active`이고 (`expires_at` 없음 또는 미래) 이고 `last_checked_at`이 72시간 이내 → true.
  - debug 빌드는 지금처럼 항상 true.
- 확인 시점: 앱 열 때, 「시작」 누를 때, 매크로 실행 중 30분마다(포그라운드 서비스에서).
- 홈 상단 상태 카드:
  - `none`: 「활성화 요청」 카드 — 입금자명, 기간 선택(가격은 `/api/config`에서), 계좌번호(복사 버튼), 「승인 후 환불 불가에 동의」 체크(약관 링크), 「요청 보내기」.
  - `pending`: 「승인 대기 중」 + 입금 안내 + 오픈채팅 버튼 + 「다시 확인」. 이 카드가 떠 있는 동안 1분마다 상태 폴링.
  - `active`: 지금의 「대기 중/시작」. 만료 7일 전부터 "N일 남음" 줄과 「연장 요청」 버튼. 연장 요청 후에는 "연장 승인 대기" 표시.
  - `expired`/`revoked`: 잠김 + 「연장 요청」(= 같은 요청 카드).
- 「새 버전 받기」 배너(§6).
- 설정 화면의 「화면 축소」 등 기존 기능은 그대로.
- 삭제: HMAC 키 입력 UI, `LICENSE_SECRET`, `generate_key.sh`, `build_customer_apk.sh`, `package_apk_for_share.sh`(zip 배포 불필요).

### 7.8 DNS·배포

- 가비아 DNS 관리에서 `tget` CNAME → `cname.vercel-dns.com` 추가. Vercel 프로젝트 Domains에 `tget.bigseasons.co.kr` 등록. 인증서는 Vercel이 자동 발급.
- Vercel 환경변수: `DATABASE_URL`(Neon), `BLOB_READ_WRITE_TOKEN`, `ADMIN_PASSWORD`, `SESSION_SECRET`, `CRON_SECRET`. 가격·계좌·오픈채팅은 `settings` 테이블.
- Preview 환경은 별도 Neon 브랜치의 `DATABASE_URL`을 쓴다.
- 배포는 `web/` 변경을 main에 푸시하면 Vercel이 자동 빌드. 로컬에서는 `vercel dev`.

## 7.9 운영 전 사용자가 할 일(코드 밖)

- 구청(은평구)에 통신판매업 신고. 일반과세자라 면제 대상이 아니다. 신고번호가 나오면 관리 「설정」 또는 푸터 상수에 기입.
- 오픈채팅 링크: `https://open.kakao.com/o/s1cF7MUi` (관리 「설정」 초기값으로 넣는다).
- 사업자 계좌번호, 가격 3종을 관리 「설정」에 입력.
- 랜딩용 스크린샷 3~4장 전달.
- 현금영수증 발급 여부는 사용자 판단(의무 발급 업종은 아니나 요청 시 발급).

## 8. 문서 변경

- `docs/buyer_setup_guide.md` → 웹 `/download`·`/guide`로 내용이 옮겨가므로, 문서는 "웹 주소 안내 + 사이드로드 요약" 1쪽으로 줄인다.
- `README.md`에 web 디렉터리, 환경변수, 키스토어 백업, 가비아 DNS 절차를 적는다.

## 9. 구현자에게 맡기는 것

- Next.js 세부 구조(라우트 핸들러 vs 서버 액션), 쿠키 서명 방식, 폼 검증 라이브러리 유무.
- 관리 화면의 스타일(최소한으로, 기능 우선). 랜딩·다운로드는 `ui-ux` 스킬을 따른다.
- 앱 네트워크 호출의 타임아웃·재시도 값(제안: 타임아웃 8초, 재시도 없음, 다음 확인 시점에 다시).
- 테스트 범위: 서버는 API 핸들러 단위 테스트(요청→승인→상태 전이, 활성 중 연장 요청, 만료 계산, 동의 없는 요청 거부), 앱은 `DeviceLicense` 판단 로직 단위 테스트(기존 테스트 58개 유지).

## 10. 순서 제안

1. 키스토어 생성·서명 설정(모든 배포의 전제).
2. `web/` 스캐폴드 + DB + API + 관리 화면(기능만).
3. 앱 라이선스 교체 + 실기기(플립5) 검증: 요청 → 승인 → 자동 활성 → 회수 → 잠김.
4. 랜딩·다운로드·가이드 페이지(`ui-ux` 스킬).
5. 가비아 DNS·Vercel 도메인·환경변수, 첫 APK 업로드.
6. 문서 정리.
