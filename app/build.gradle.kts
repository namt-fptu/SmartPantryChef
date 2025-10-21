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

    buildFeatures {
        viewBinding = true
        dataBinding = false
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

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    // ✅ Gemini SDK (version ổn định)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // ✅ Reactive Streams (cần cho Gemini SDK)
    implementation("org.reactivestreams:reactive-streams:1.0.4")

    // ✅ Kotlin Coroutines (cần cho async operations)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")

    // ML Kit
    implementation("com.google.mlkit:image-labeling:17.0.9")
    implementation("com.google.guava:guava:32.1.2-android") // ⚠️ Đổi sang -android

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata:2.9.4")

    // Material Design
    implementation("com.google.android.material:material:1.13.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}