plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.rvoc.cvorapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rvoc.cvorapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

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
        buildConfig = true
        dataBinding = true
    }

    packaging {
        jniLibs.pickFirsts.add("lib/arm64-v8a/libc++_shared.so")
        jniLibs.pickFirsts.add("lib/x86_64/libc++_shared.so")
        jniLibs.pickFirsts.add("lib/x86/libc++_shared.so")
        jniLibs.pickFirsts.add("lib/armeabi-v7a/libc++_shared.so")
    }

    /*
    kapt {
        correctErrorTypes = true
    }*/
}

ksp {
    arg("android.databinding.incremental", "true")
}

dependencies {
    // Core Libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.splashscreen)
    implementation(libs.constraintlayout)
    implementation(libs.cardView)
    // implementation(libs.git.repo)
    implementation(libs.concurrent.futures)
    implementation(libs.guava)

    // Lifecycle Components
    implementation(libs.lifecycle.runtime.ktx)
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
    implementation(libs.pdf.viewer)

    //OpenCV Libraries
    // implementation(libs.opencv)

    // Image loading/caching libraries
    implementation(libs.glide)
    implementation(libs.ucrop)
    implementation(libs.photoView)
    ksp(libs.glide.compiler)

    //Animation libraries
    implementation(libs.lottie)

    // Ad libraries
    // implementation(libs.adMob)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines for Async Operations
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Fix for - error: cannot access KSerializer
    implementation(libs.serialization.json)

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
