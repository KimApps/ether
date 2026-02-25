plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kimapps.ether"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.kimapps.ether"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.bundles.general)
    // compose
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    // for testing
    testImplementation(libs.bundles.test.unit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.bundles.test)
    // for debug
    debugImplementation(libs.bundles.debug)
    // hilt
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.compiler)
    // navigation
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    // app modules:
    // core
    implementation(project(":modules:core:network"))
    implementation(project(":modules:core:utils"))
    implementation(project(":modules:core:local-storage"))
    implementation(project(":modules:core:navigation"))
    // shared
    implementation(project(":modules:shared:user"))
    implementation(project(":modules:shared:signing"))
    // features
    implementation(project(":modules:features:home"))
    implementation(project(":modules:features:withdraw"))
}