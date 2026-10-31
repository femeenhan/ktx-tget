# ktx-tget

Kotlin(Android 네이티브)로 개발하는 **ktx-tget** 저장소입니다. Flutter Android는 이 프로젝트 범위에 포함하지 않습니다.

## 요구 사항

- [Android Studio](https://developer.android.com/studio) Koala 이상 또는 동일 버전의 Android SDK / 빌드 도구
- JDK 17
- (선택) Homebrew 등으로 설치한 Gradle — 저장소에 포함된 Wrapper로도 빌드 가능

## 빠른 시작

1. 저장소를 클론합니다.
   ```bash
   git clone https://github.com/femeenhan/ktx-tget.git
   cd ktx-tget
   ```
2. Android Studio에서 **Open**으로 이 디렉터리를 엽니다.
3. `local.properties`에 SDK 경로가 없으면 생성합니다. (Android Studio가 보통 자동 생성)
   ```properties
   sdk.dir=/path/to/Android/sdk
   ```
4. 디버그 빌드:
   ```bash
   ./gradlew :app:assembleDebug
   ```

## 프로젝트 구조

| 경로 | 설명 |
|------|------|
| `app/` | Android 애플리케이션 모듈 |
| `app/src/main/java/dev/ktxtget/` | Kotlin 소스 (패키지 `dev.ktxtget`) |
| `app/src/main/res/` | 리소스(XML, drawable 등) |
| `web/` | 판매 사이트·APK 배포·라이선스 API·관리자 (Next.js, Vercel) |
| `scripts/build_preview_apk.sh` | release 서명 APK → `dist/ktx-tget-<버전>.apk` |

- UI: **Jetpack Compose** + **Material 3** (XML 레이아웃·ViewBinding 미사용)
- 루트 및 `app` 모듈은 Kotlin DSL(`*.gradle.kts`)을 사용합니다.

## 버전 정보 (초기 스캐폴드)

- `compileSdk` / `targetSdk`: 35  
- `minSdk`: 26  
- AGP 8.8.x · Kotlin 2.0.x · JVM 17  

## 원격 저장소

- GitHub: [https://github.com/femeenhan/ktx-tget](https://github.com/femeenhan/ktx-tget)

## web (tget.bigseasons.co.kr)

`web/`은 Next.js 앱입니다. 랜딩(`/`), 다운로드(`/download`), 사용 안내(`/guide`), 약관·개인정보(`/terms`, `/privacy`), 관리자(`/admin`), 앱이 부르는 API(`/api/device/*`, `/api/config`, `/api/version`), 매일 도는 cron(`/api/cron/cleanup`)이 들어 있습니다. 로컬 개발·테스트 명령은 `web/README.md`를 보세요.

### Vercel 프로젝트

- 팀 `femeenhans-projects`, 프로젝트 **`tget-web`**. 함수 리전 `sin1`(싱가포르, Neon DB와 같은 곳), `web/vercel.json`에 cron(매일 18:00 UTC = 03:00 KST)이 있습니다.
- 배포는 **CLI**로 합니다. GitHub 연동은 하지 않았습니다.
  ```bash
  cd web
  vercel link --yes --project tget-web --scope femeenhans-projects   # 새 PC에서 처음 한 번
  vercel deploy --prod
  ```
- 기본 주소: https://tget-web.vercel.app (커스텀 도메인 연결 전에도 동작)

### 환경변수

| 이름 | 출처 | 비고 |
|------|------|------|
| `DATABASE_URL` (+ `PG*`, `POSTGRES_*`) | Vercel Marketplace **Neon** (`tget-db`, 무료, sin1) | 연동이 자동 주입. Production·Preview·Development 모두 같은 DB |
| `BLOB_READ_WRITE_TOKEN` | Vercel **Blob** 스토어 `tget-apk` (public, sin1) | 연동이 자동 주입 |
| `ADMIN_PASSWORD` | 직접 생성 | `/admin` 로그인 비밀번호 |
| `SESSION_SECRET` | 직접 생성 | 관리자 쿠키 서명 키. 바꾸면 모든 로그인 해제 |
| `CRON_SECRET` | 직접 생성 | Vercel Cron이 `Authorization: Bearer`로 보냄 |

- 직접 생성한 세 값은 **`~/keystores/tget-web-secrets.txt`**(권한 600)에 있습니다. 1Password 등에 옮겨 두세요.
- 이 셋은 Vercel에서 민감(sensitive) 변수라 `vercel env pull`로 값이 내려오지 않습니다(빈 값). 로컬에서 쓰려면 위 파일에서 복사합니다.
- 값을 바꿀 때: `printf '%s' "새값" | vercel env add ADMIN_PASSWORD production --force` (preview도 같은 방식) 후 `vercel deploy --prod`.

### 가비아 DNS (tget.bigseasons.co.kr)

`bigseasons.co.kr`의 네임서버는 가비아입니다. Vercel 프로젝트에는 도메인을 이미 추가해 두었고, 가비아에 레코드 하나만 넣으면 됩니다.

1. 가비아 로그인 → **My가비아** → **DNS 관리툴** → `bigseasons.co.kr` **설정**
2. **레코드 추가**:
   - 타입 **CNAME**
   - 호스트 **`tget`**
   - 값/위치 **`d2c360c23f09c1f9.vercel-dns-017.com.`** (Vercel이 이 프로젝트에 권장한 값. 일반 값 `cname.vercel-dns.com.`도 됨)
   - TTL 기본값(3600)
3. **저장**. 반영되면 Vercel이 자동으로 확인하고 HTTPS 인증서를 발급합니다(보통 수 분~1시간).
4. 확인: `dig +short tget.bigseasons.co.kr CNAME` → 위 값, `cd web && vercel domains verify tget.bigseasons.co.kr`, `curl -I https://tget.bigseasons.co.kr` → 200.

앱의 기본 서버 주소(`BuildConfig.API_BASE_URL`)가 `https://tget.bigseasons.co.kr`이므로, **이 CNAME이 없으면 배포한 APK가 서버에 닿지 못합니다.**

### 운영 전 할 일

- [ ] 가비아 CNAME 추가(위 절차)
- [ ] 은평구청 **통신판매업 신고** → 신고번호를 `/admin/settings`에 입력(푸터에 표시)
- [ ] `/admin/settings`에 **가격 3종**(1개월·1년·영구)과 **입금 계좌** 입력. 가격이 0이면 앱·랜딩에 「문의」로 보입니다
- [ ] 랜딩 스크린샷 교체: `web/public/screens/01.png` ~ `04.png`를 실제 화면으로 바꾸고 `vercel deploy --prod`
- [ ] 오픈채팅 주소 확인(초기값 `https://open.kakao.com/o/s1cF7MUi`, `/admin/settings`에서 수정)
- [ ] (선택) Preview용 Neon 브랜치 분리 — 지금은 Preview도 운영 DB를 씁니다. Preview 배포로 시험할 때 주의

### 새 버전(APK) 올리기

1. `app/build.gradle.kts`의 `versionName`을 바꿉니다(예: 0.3.4). `versionCode`는 여기서 자동 계산됩니다(X.Y.Z → X×10000+Y×100+Z, 0.3.4 → 304).
2. `./scripts/build_preview_apk.sh` → `dist/ktx-tget-<versionName>.apk`
3. https://tget.bigseasons.co.kr/admin/releases 에서 APK 파일, versionName, 변경 내용을 넣고 업로드. versionCode는 폼이 같은 공식으로 계산해 넣습니다. 올리면 자동으로 최신이 되고, 앱 홈에 「새 버전 받기」 배너가 뜹니다.
4. 문제가 있으면 같은 화면에서 이전 버전을 「최신으로」 되돌립니다.
5. 확인: `curl https://tget.bigseasons.co.kr/api/version`

### 기기 교체·초기화 운영

라이선스는 기기의 `ANDROID_ID`(앱 서명 키마다 다름)에 묶입니다. 구매자가 휴대폰을 바꾸거나 초기화하면 기기 ID가 바뀝니다.

1. 구매자가 새 기기에서 앱을 설치하고 「활성화 요청」을 보냅니다(입금 없이, 입금자명은 기존 이름).
2. `/admin/devices`에서 기존 행(입금자명·메모로 찾기)의 만료일을 확인하고, 새 행을 **승인**합니다. 남은 기간에 맞춰 기간을 고르고 메모에 「기기 교체, 이전 <앞 6자>」를 적습니다.
3. 기존 행은 **회수** 후 **삭제**합니다.

## release 서명

`~/keystores/ktxtget-release.jks`와 `local.properties`의 `keystorePassword`/`keyPassword`는 함께 백업하세요. 둘 중 하나라도 잃으면 같은 서명으로 업데이트를 배포할 수 없습니다. 설정 키는 `local.properties.example`을 참고하고, `scripts/build_preview_apk.sh`로 `dist/ktx-tget-<버전>.apk`를 만듭니다. 서명 키가 바뀌면 기기의 `ANDROID_ID`도 바뀌어 모든 구매자의 승인이 풀리므로, 키스토어는 웹 비밀값(`~/keystores/tget-web-secrets.txt`)과 함께 PC 밖(1Password·외장 디스크 등)에 꼭 백업하세요.

## 라이선스

미정 — 필요 시 `LICENSE` 파일을 추가하세요.
