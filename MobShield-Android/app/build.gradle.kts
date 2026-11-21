plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val mobshieldVersion = "1.0.10"

android {
    namespace = "com.poc.mobshield"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.poc.mobshield"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Debug-signed so the release APK installs for the PoC (shows app_debuggable = false path).
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // MobShield runtime shielding: core + each detector module as a separate Maven Central artifact.
    implementation("io.github.inforaam:mobshield-core:$mobshieldVersion")
    implementation("io.github.inforaam:mobshield-detect-root:$mobshieldVersion")
    implementation("io.github.inforaam:mobshield-detect-hooks:$mobshieldVersion")
    implementation("io.github.inforaam:mobshield-detect-debugger:$mobshieldVersion")
    implementation("io.github.inforaam:mobshield-detect-environment:$mobshieldVersion")
    implementation("io.github.inforaam:mobshield-detect-integrity:$mobshieldVersion")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
