plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.histopgambling.fixture.luckymirror"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.histopgambling.fixture.luckymirror"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

