plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.talsec.plugin)
}

android {
    namespace = "com.poc.freerasp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.poc.freerasp"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            // prod = false: debug, simulator, app-integrity and unofficial-store checks stay quiet
            buildConfigField("boolean", "RASP_PROD", "false")
        }
        release {
            // prod = true: the full check set reports
            buildConfigField("boolean", "RASP_PROD", "true")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // PoC only: sign release with the debug key so it installs without a keystore
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    talsec {
        sdkVersion = "20.0.1"
    }
}
