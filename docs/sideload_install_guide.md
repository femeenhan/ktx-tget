# 배포 절차 (판매자용)

> 다른 사람에게 ktx-tget을 설치해 줄 때 판매자(나)가 할 일을 순서대로 정리한 문서입니다.
> 구매자에게 보낼 안내서는 [`buyer_setup_guide.md`](buyer_setup_guide.md)입니다.

## 한눈에 보기

모든 구매자에게 **같은 APK 하나**를 보내고, 사람마다 **라이선스 키만** 따로 만들어 줍니다. 서버는 없습니다.

```text
[나]     ① 배포용 APK 빌드 ──────────────▶ [구매자] ② 설치
                                                     ↓
[나]     ④ generate_key.sh <기기ID>  ◀──── ③ 앱에서 기기 ID 복사해 보냄
            ↓
         라이선스 키 전송 ────────────────▶ ⑤ 키 입력 → 활성화
                                             ⑥ 접근성·알림 설정 → 사용
```

| 단계 | 누가 | 할 일 | 명령·화면 |
|------|------|-------|-----------|
| 1 | 나 | 배포용 APK 빌드 | `./scripts/build_preview_apk.sh` |
| 2 | 나 | APK와 구매자 안내서 전달 | Drive 링크 또는 카톡 zip + `buyer_setup_guide.md` |
| 3 | 구매자 | 설치 → 「라이선스 활성화」 카드의 「복사」 → 기기 ID 보냄 | 앱 |
| 4 | 나 | 키 생성 → 전달 | `./scripts/generate_key.sh <기기ID>` |
| 5 | 구매자 | 키 입력 → 「활성화」 → 접근성·알림 설정 | 앱 |

---

## 0. 처음 한 번만: 준비와 백업

### 0-1. licenseSecret

`local.properties`(git에 올라가지 않음)에 들어 있습니다.

```properties
sdk.dir=/path/to/Android/sdk
licenseSecret=본인만_아는_값
```

- 모든 라이선스 키는 이 값과 기기 ID로 계산됩니다. **바꾸면 이미 나간 키가 전부 무효가 됩니다.**
- 빌드할 때 APK 안에 들어갑니다(`BuildConfig.LICENSE_SECRET`).

### 0-2. 서명 키

배포용(release) APK는 지금 **이 Mac의 디버그 키**(`~/.android/debug.keystore`)로 서명됩니다(`app/build.gradle.kts`의 `signingConfig = signingConfigs.getByName("debug")`).

이 파일이 없어지거나 다른 Mac에서 빌드하면:

- 기존 구매자가 **덮어 설치를 못 하고** 앱을 지운 뒤 다시 설치해야 합니다.
- 안드로이드 8 이상에서는 기기 ID(ANDROID_ID)가 **앱 서명 키마다 다르게** 나옵니다. 서명 키가 바뀌면 기기 ID도 바뀌어 **모든 구매자에게 키를 새로 발급**해야 합니다.

### 0-3. 반드시 백업할 것

| 파일 | 왜 |
|------|-----|
| `local.properties`의 `licenseSecret` 값 | 없으면 키를 더 만들 수 없음 |
| `~/.android/debug.keystore` | 없으면 업데이트 불가, 기기 ID가 전부 바뀜 |

비밀번호 관리자나 개인 클라우드에 둘 다 보관하세요. 공개 저장소에는 올리지 마세요.

> 장기적으로는 배포 전용 키스토어를 만드는 편이 안전합니다.
> `keytool -genkey -v -keystore ktxtget-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ktxtget`
> 단, **이미 구매자가 있으면** 키스토어를 바꾸는 순간 위 0-2 문제가 생깁니다. 바꿀 거라면 구매자가 적을 때 하세요.

---

## 1. 배포용 APK 빌드

### 1-1. 빌드 전 체크

- [ ] 코레일톡 최신 버전에서 실제로 동작 확인했는가 (마지막 확인: **코레일톡 7.0.10**, 2026-10-24)
- [ ] 바뀐 게 있으면 `app/build.gradle.kts`의 `versionName`을 올렸는가 (`versionCode`는 versionName에서 자동 계산)
- [ ] `./gradlew testDebugUnitTest lintDebug` 통과

### 1-2. 빌드

```bash
./scripts/build_preview_apk.sh
# → dist/ktx-tget-release.apk        (Drive용)
# → dist/ktx-tget-release-kakao.zip  (카톡용, .apk는 카톡이 막음)
```

- **디버그 APK(`app-debug.apk`)는 절대 보내지 마세요.** 디버그 빌드는 라이선스 검사를 건너뜁니다(`DEV_OPEN`). 개발용 폰에 `run_app.sh`나 `assembleDebug`로 깐 것이 이것입니다.
- 빌드 결과가 최신인지는 `dist/` 파일 날짜로 확인합니다.

### 1-3. 내 폰에서 먼저 확인 (권장)

```bash
adb install -r dist/ktx-tget-release.apk
```

릴리스 APK는 라이선스가 필요하므로 「잠김」 화면이 보이면 정상입니다. 내 기기 ID로 키를 만들어 넣어 보면 전 과정을 한 번에 검증할 수 있습니다.

---

## 2. 전달

| 방법 | 보낼 파일 | 비고 |
|------|-----------|------|
| **Google Drive 링크** | `ktx-tget-release.apk` | 가장 권장 |
| 카카오톡 | `ktx-tget-release-kakao.zip` | 구매자가 내 파일에서 압축 해제 |
| Gmail 첨부 | — | apk·zip 모두 막히는 경우가 많음 |

구매자 안내서 [`buyer_setup_guide.md`](buyer_setup_guide.md)도 같이 보냅니다.

---

## 3. 라이선스 키 발급

구매자가 앱에서 「복사」로 보낸 16자리 기기 ID를 받으면:

```bash
./scripts/generate_key.sh a1b2c3d4e5f67890
# Device ID  : a1b2c3d4e5f67890
# License Key: XXXX-XXXX-XXXX-XXXX
```

- 출력된 **License Key**를 보내면 끝입니다. APK를 다시 빌드할 필요 없습니다.
- 같은 기기 ID에는 항상 같은 키가 나옵니다. 구매자가 키를 잃어버려도 다시 만들어 주면 됩니다.
- 누구에게 발급했는지 기억하려면 아래 표 같은 기록을 따로 남겨 두세요.

| 날짜 | 구매자 | 기기 ID | 비고 |
|------|--------|---------|------|
| | | | |

> ⚠️ `adb shell settings get secure android_id`로 얻는 값은 **앱이 보는 기기 ID와 다릅니다**(안드로이드 8 이상은 앱 서명 키마다 값이 다름). 반드시 앱 화면의 값을 받으세요.

---

## 4. 구매자 지원

구매자가 막히는 곳은 거의 이 셋입니다.

1. **설치가 막힘:** Play 프로텍트 / Auto Blocker를 끄지 않음 → 안내서 1-2
2. **시작해도 아무 일 없음:** 접근성이 「제한된 설정」에 걸려 꺼져 있음 → 안내서 4-1 ①부터
3. **키가 안 맞음:** 기기 ID를 잘못 보냄(IMEI 등) 또는 폰 초기화로 ID가 바뀜 → 앱의 「복사」 값을 다시 받아 재발급

그 밖의 문제는 구매자에게 앱의 **「기록」 화면 캡처**와 **코레일톡 버전**을 받아 보면 원인을 찾기 쉽습니다.

---

## 5. 업데이트 배포

1. 1단계와 같이 `versionName`을 올리고 `./scripts/build_preview_apk.sh`로 빌드합니다. **같은 Mac, 같은 서명 키**여야 합니다.
2. 새 APK를 보냅니다. 구매자는 덮어 설치만 하면 되고 키는 다시 넣지 않습니다.
3. 업데이트 뒤 접근성이 꺼질 수 있으니 확인하라고 같이 알려 줍니다.

---

## 6. 알고 있어야 할 한계

- **비밀값 노출 가능성:** licenseSecret이 APK 안에 들어 있어, APK를 분석하면 꺼낼 수 있고 누구나 키를 만들 수 있습니다. 소수에게 나눠 주는 용도로는 괜찮지만 널리 팔 계획이면 서버 검증 등 보완이 필요합니다.
- **코레일톡 의존:** 코레일톡 화면 구조가 바뀌면 동작이 깨질 수 있습니다. 코레일톡 업데이트 소식이 있으면 내 폰에서 먼저 확인하세요.
- **이용약관:** 코레일 이용약관은 자동화 프로그램 사용을 제한할 수 있어, 구매자 계정이 제재될 위험이 있습니다. 구매자에게 미리 알려 주세요.
