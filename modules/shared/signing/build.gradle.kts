plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kimapps.signing"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        namespace = "com.kimapps.signing"
        minSdk = libs.versions.minSdk.get().toInt()

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
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.bundles.test.unit)
    androidTestImplementation(libs.bundles.test)
    // compose
    implementation(libs.bundles.general)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.bundles.compose)
    // hilt
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.compiler)
    // wallet connect
    implementation(libs.walletkit)
    implementation(libs.walletkit.android.core)

    // internal modules
    // core:
    implementation(project(":modules:core:network"))
    implementation(project(":modules:core:local-storage"))
    implementation(project(":modules:core:navigation"))
    implementation(project(":modules:core:error-logger"))
    // shared
    implementation(project(":modules:shared:ui"))
}