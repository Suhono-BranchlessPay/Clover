plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.util.Properties

android {
    namespace = "com.branchlesspay.auditshield.clover"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.branchlesspay.auditshield.clover"
        minSdk = 21
        targetSdk = 34
        versionCode = 5
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val localProps = Properties()
            val localFile = rootProject.file("local.properties")
            if (localFile.exists()) {
                localFile.inputStream().use { localProps.load(it) }
            }
            val keystorePath = localProps.getProperty("release.keystore.file", "")
            if (keystorePath.isNotBlank()) {
                storeFile = rootProject.file(keystorePath)
                storePassword = localProps.getProperty("release.keystore.password", "")
                keyAlias = localProps.getProperty("release.key.alias", "bp-audit-shield")
                keyPassword = localProps.getProperty("release.key.password", "")
            }
        }
    }

    val localProps = Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { localProps.load(it) }
    }
    val cloverAppId = localProps.getProperty("clover.app.id", "")

    buildTypes {
        debug {
            val devKey = localProps.getProperty("bp.license.key", "")
            buildConfigField("String", "DEFAULT_LICENSE_KEY", "\"$devKey\"")
            buildConfigField("String", "CLOVER_APP_ID", "\"$cloverAppId\"")
        }
        release {
            buildConfigField("String", "DEFAULT_LICENSE_KEY", "\"\"")
            buildConfigField("String", "CLOVER_APP_ID", "\"$cloverAppId\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
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
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.9.1")

    // Clover POS SDK — A920 Pro, Flex, Mini (Maven Central latest: 323)
    implementation("com.clover.sdk:clover-android-sdk:323")
    implementation("com.clover.sdk:clover-android-connector-sdk:323")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:core:3.5.1")
}
