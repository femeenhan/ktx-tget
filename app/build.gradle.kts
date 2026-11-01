plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

val apiBaseUrl: String = (
    localProperties.getProperty("apiBaseUrl") ?: "https://tget.bigseasons.co.kr"
    ).trim()

val releaseSigningKeys = listOf("keystorePath", "keystorePassword", "keyAlias", "keyPassword")
val hasReleaseSigning = releaseSigningKeys.all { !localProperties.getProperty(it).isNullOrBlank() }
// 서명 키 없이 release를 만들면 debug 키로 서명된 APK가 조용히 나가 구매자 기기에 덮어쓸 수 없게 된다.
// debug 빌드·단위 테스트는 키 없이 돌아가야 하므로 release 산출 작업만 실행 시점에 막는다.
if (!hasReleaseSigning) {
    tasks.matching {
        it.name.startsWith("assembleRelease") || it.name.startsWith("bundleRelease") || it.name == "packageRelease"
    }.configureEach {
        doFirst {
            throw GradleException("release 서명 키가 local.properties에 없습니다 (keystorePath/keystorePassword/keyAlias/keyPassword)")
        }
    }
}

/** X.Y.Z 꼴 versionName을 단조 증가하는 versionCode로 바꾼다. 각 자리는 0~99. */
fun versionCodeOf(versionName: String): Int {
    val parts: List<Int> = versionName.split(".").map { part: String ->
        part.toIntOrNull() ?: error("versionName은 X.Y.Z 숫자 꼴이어야 합니다: $versionName")
    }
    require(parts.size == 3 && parts.all { it in 0..99 }) { "versionName은 X.Y.Z (각 0~99) 꼴이어야 합니다: $versionName" }
    return parts[0] * 10000 + parts[1] * 100 + parts[2]
}

android {
    namespace = "dev.ktxtget"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.ktxtget"
        minSdk = 26
        targetSdk = 35
        // versionCode는 versionName에서 자동으로 만든다: X.Y.Z → X*10000 + Y*100 + Z (0.3.3 → 303).
        // 올릴 때는 versionName 한 줄만 바꾼다. 판매 웹 관리자 폼도 같은 공식으로 계산한다(web/lib/version-code.ts).
        versionName = "0.3.4"
        versionCode = versionCodeOf(versionName!!)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(localProperties.getProperty("keystorePath").trim())
                storePassword = localProperties.getProperty("keystorePassword").trim()
                keyAlias = localProperties.getProperty("keyAlias").trim()
                keyPassword = localProperties.getProperty("keyPassword").trim()
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    testImplementation("junit:junit:4.13.2")
    // JVM 단위 테스트에서 android.jar의 org.json 스텁 대신 쓰는 실제 구현(LicenseApi.parseSnapshot 테스트용)
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
