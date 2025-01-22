plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.rvoc.cvorapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rvoc.cvorapp"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_20
        targetCompatibility = JavaVersion.VERSION_20
    }

    lint {
        ignoreWarnings = false
    }

    buildFeatures {
        viewBinding = true
    }
    /*
    kapt {
        correctErrorTypes = true
    }*/
}

dependencies {
    // Core Libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.splashscreen)
    implementation(libs.constraintlayout)
    implementation(libs.cardView)

    // Lifecycle Components
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Navigation Components
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // CameraX Libraries
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    //PDF Libraries
    implementation(libs.pdfbox.android)

    // Image loading/caching libraries
    implementation(libs.glide)
    ksp(libs.glide.compiler)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines for Async Operations
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // WorkManager for Background Tasks
    implementation(libs.work.runtime.ktx)

    // Permissions Dispatcher
    implementation(libs.permissions.dispatcher)
    ksp(libs.permissions.dispatcher.processor)

    // Hilt for Dependency Injection
    implementation(libs.kotlinx.metadata.jvm)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Testing Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

hilt {
    enableAggregatingTask = true
}
