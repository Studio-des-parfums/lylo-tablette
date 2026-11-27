import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// ── Charge local.properties pour injecter les config en BuildConfig ────────
// Equivalent Android de NEXT_PUBLIC_API_URL / NEXT_PUBLIC_GOOGLE_CLIENT_ID / NEXT_PUBLIC_DEV_MODE.
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(FileInputStream(f))
}

fun localProp(key: String, default: String): String =
    (localProperties.getProperty(key) ?: System.getenv(key) ?: default)

val apiBaseUrl = localProp("LYLO_API_BASE_URL", "https://lylo-back-production-78a9.up.railway.app")
val devMode = localProp("LYLO_DEV_MODE", "false")
// Marque active (équivalent de NEXT_PUBLIC_BRAND) : slug envoyé au back (questionnaire, session,
// formules) + thème visuel si la marque en a un dans l'app ("lylo", "help"), sinon Lylo.
val brand = localProp("LYLO_BRAND", "lylo").trim().lowercase().ifEmpty { "lylo" }
val googleWebClientId = localProp(
    "LYLO_GOOGLE_WEB_CLIENT_ID",
    "51707488835-jh0q9u70r28f2qu9ab5ee8k6abttp9d0.apps.googleusercontent.com"
)

android {
    namespace = "com.lylo.tablet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lylo.tablet"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("boolean", "DEV_MODE", devMode)
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "BRAND", "\"$brand\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Core / Compose ──────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // ── Navigation ───────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // ── Coil (images distantes : notes de choix, QR code, avatar) ───────
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")

    // ── Réseau : appels API backend (session, questions, formules...) ───
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ── LiveKit Android SDK (équivalent livekit-client + @livekit/components-react) ──
    implementation("io.livekit:livekit-android:2.13.0")

    // ── Google OAuth : Credential Manager + Google Identity Services ─────
    // Remplace @react-oauth/google côté web.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // ── QR code génération locale (FormulaQrCode) ────────────────────────
    implementation("com.google.zxing:core:3.5.3")

    // ── SDK robot OrionStar (navigation vers un point de la carte) ───────
    implementation(files("libs/robotservice.jar"))
    // Requis par robotservice.jar (PersonApi) mais non embarqué dans le jar.
    implementation("com.google.code.gson:gson:2.11.0")

    // ── DataStore (persistance clé/valeur, équivalent localStorage) ─────
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
