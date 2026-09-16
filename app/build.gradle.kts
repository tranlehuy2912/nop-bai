import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Mat khau con dau nam trong keystore.properties, file do khong bao gio vao git.
// Chua tao thi van build duoc ban go loi, chi ban release la chua ky duoc.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}
val hasKeystore = keystorePropsFile.exists()

// Token bot, API key Gemini va ma PIN nam trong local.properties, file do khong bao
// gio vao git. Chua dien thi van build duoc: BuildConfig nhan chuoi rong, app chay
// len se hoi Ba Huy dien tay o man hinh Cai dat. Xem local.properties.mau.
val biMatFile = rootProject.file("local.properties")
val biMat = Properties().apply {
    if (biMatFile.exists()) {
        biMatFile.inputStream().use { load(it) }
    }
}

// Doc mot dong trong local.properties ra dang chuoi Kotlin da boc san nhay kep,
// vi buildConfigField nhan nguyen van doan ma chu khong nhan gia tri.
fun chuoiBiMat(ten: String): String {
    val v = (biMat.getProperty(ten) ?: "").trim()
    return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

// Plugin google-services chi bat khi da co google-services.json.
//
// Bat vo dieu kien thi may nao chua tai file ve la build do ngay tu dau. Thieu file
// thi app van chay day du duong Telegram nhu truoc, chi la khong noi duoc sang app
// Bang dieu khien ben dien thoai Ba Huy. Xem ../homework-gate-3/CAI_DAT_FIREBASE.md.
val coFirebase = file("google-services.json").exists()
if (coFirebase) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn("Chua co app/google-services.json - ban build nay khong noi duoc app Bang dieu khien.")
}

android {
    namespace = "vn.huytl.homeworkgate"
    compileSdk = 37

    defaultConfig {
        applicationId = "vn.huytl.homeworkgate"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BOT_THAT", chuoiBiMat("BOT_THAT"))
        buildConfigField("String", "BOT_MAY_AO", chuoiBiMat("BOT_MAY_AO"))
        buildConfigField("String", "AI_KEYS", chuoiBiMat("AI_KEYS"))
        buildConfigField("String", "PIN", chuoiBiMat("PIN"))
    }

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasKeystore) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        // Chi de biet dang la ban go loi hay ban that, cho [Defaults] chon token bot.
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.coroutines.android)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit)
}
