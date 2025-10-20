plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "fpt.edu.vn.smartpantrychef"
    compileSdk = 36

    defaultConfig {
        applicationId = "fpt.edu.vn.smartpantrychef"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    // CameraX
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")

    // ML Kit Image Labeling
    implementation("com.google.mlkit:image-labeling:17.0.9")

    // Gemini
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata:2.9.4")

    // Material Design 3
    implementation("com.google.android.material:material:1.13.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Activity and Fragment KTX
    implementation("androidx.activity:activity:1.11.0")
    implementation("androidx.fragment:fragment:1.8.9")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}