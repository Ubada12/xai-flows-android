import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ─── local.properties-backed secrets ──────────────────────────────────────────
// See AppConfig.kt's "ADDING SECRETS" doc comment for the full recipe this
// implements. local.properties is git-ignored, so nothing here ever reaches
// version control — only the resulting BuildConfig field does, at build time.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(FileInputStream(file))
    }
}

/** Cloudflare Turnstile public site key. Safe to be in the APK (it's the
 *  public half of the key pair), but still config-driven rather than
 *  hardcoded so dev/test/production keys never require a source change.
 *  Empty string (not null) when unset, so AppConfig.Turnstile.SITE_KEY's
 *  .ifBlank { } fallback can do the rest. */
val turnstileSiteKey: String = localProperties.getProperty("TURNSTILE_SITE_KEY") ?: ""

/** Real hosted page the Turnstile widget's WebView navigates to (Cloudflare's
 *  documented mobile pattern — see AppConfig.kt's Turnstile.WIDGET_URL doc
 *  comment for the full "why"). Defaults to the frontend repo's deployed
 *  public/mobile-turnstile.html so a fresh checkout still builds without any
 *  local.properties setup, same fallback philosophy as the site key above. */
val turnstileWidgetUrl: String = localProperties.getProperty("TURNSTILE_WIDGET_URL")
    ?: "https://app.ubada.org/mobile-turnstile.html"

android {
    namespace = "org.ubada.xaiflows"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.ubada.xaiflows"
        // Bumped 21 -> 23: androidx.security:security-crypto (used by
        // TokenStore for the encrypted refresh-token cookie) requires API 23
        // — it relies on KeyGenParameterSpec symmetric AndroidKeyStore support
        // that doesn't exist below Android 6.0. Below 23 the manifest merger
        // fails outright ("uses-sdk:minSdkVersion 21 cannot be smaller than
        // version 23 declared in library [androidx.security:security-crypto]").
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TURNSTILE_SITE_KEY", "\"$turnstileSiteKey\"")
        buildConfigField("String", "TURNSTILE_WIDGET_URL", "\"$turnstileWidgetUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.core:core-splashscreen:1.0.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Lifecycle + ViewModel — versions come from the catalog (lifecycleRuntimeKtx)
    // so this can't drift from androidx-lifecycle-runtime-ktx again.
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Coil for images
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.google.code.gson:gson:2.10.1")

    // OkHttp (Logging)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation(libs.androidx.material.icons.extended)

    implementation("androidx.compose.foundation:foundation:1.6.1")

    implementation ("com.google.accompanist:accompanist-flowlayout:0.30.1")

    implementation ("androidx.media3:media3-exoplayer:1.3.1")
    implementation ("androidx.media3:media3-ui:1.3.1")

    implementation ("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    // Encrypted storage for the persisted refresh-token cookie (auth feature).
    // See core/auth/TokenStore.kt.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
