plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.poc.androidrasp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.poc.androidrasp"
        minSdk = 24 // Android RASP requires minSdk 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 on, to prove the library's consumer rules keep the JNI entry points intact.
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // PoC only: sign release with the debug key so the non-debuggable build can be installed
            // and the debugger checks can be compared against the debug build.
            signingConfig = signingConfigs.getByName("debug")
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
        viewBinding = true
    }
    packaging {
        jniLibs {
            // Keep libnative.so uncompressed & page-aligned (default for minSdk >= 23).
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.securevale.rasp)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
}
