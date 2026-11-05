// Thu vien de app Nop bai noi chuyen voi Jokers Permissions (ban build rieng cua Shizuku, ~/Documents/jokers-permissions),
// de tam dung app khac khi tablet tiet kiem pin (xem pin/TietKiemPin.kt). Chep nguyen tu module jokers-api cua kho
// lich ngay 5/11/2026. Ma nguon goc la Shizuku-API (MIT, xem LICENSE) da doi ten, nhung cho doi ghi trong NOTICE.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "rikka.shizuku.api"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        aidl = true
        buildConfig = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.annotation)
}
