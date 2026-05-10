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

- UI: **XML + ViewBinding**, **Material 3**, **ConstraintLayout** (Compose 미사용)
- 루트 및 `app` 모듈은 Kotlin DSL(`*.gradle.kts`)을 사용합니다.

## 버전 정보 (초기 스캐폴드)

- `compileSdk` / `targetSdk`: 35  
- `minSdk`: 26  
- AGP 8.8.x · Kotlin 2.0.x · JVM 17  

## 원격 저장소

- GitHub: [https://github.com/femeenhan/ktx-tget](https://github.com/femeenhan/ktx-tget)

## 라이선스

미정 — 필요 시 `LICENSE` 파일을 추가하세요.
