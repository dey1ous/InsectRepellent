
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// 1. Load the local.properties file (Kotlin syntax)
val properties = Properties()
if (rootProject.file("local.properties").exists()) {
    properties.load(rootProject.file("local.properties").inputStream())
}

android {
    namespace = "com.example.capstone2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.capstone2"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // This line adds the key to BuildConfig
        buildConfigField("String", "AIO_KEY", "\"${properties.getProperty("AIO_KEY")}\"")
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
    buildFeatures {
        viewBinding = true

        // ⭐ FIX: Add this line to enable the BuildConfig feature
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.fragment)
    implementation(libs.room.common.jvm)
    implementation(libs.room.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.mpandroidchart)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.localbroadcastmanager)
    implementation(libs.mlkit.barcode)
    annotationProcessor(libs.room.compiler)
    implementation(libs.paho.mqtt.client)
    implementation(libs.paho.android.service)
}