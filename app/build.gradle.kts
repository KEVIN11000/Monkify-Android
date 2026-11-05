import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.k11000.monkify"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.k11000.monkify"
        minSdk = 26
        targetSdk = 37
        versionCode = 8
        versionName = "0.4.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_FILE")
                ?: System.getenv("KEYSTORE_PATH")
                ?: keystoreProperties.getProperty("storeFile")
            val targetKeystore = keystorePath?.let { rootProject.file(it).takeIf { f -> f.exists() } ?: file(it).takeIf { f -> f.exists() } }
                ?: file("keystore/monkify-release.jks").takeIf { it.exists() }
                ?: rootProject.file("app/keystore/monkify-release.jks").takeIf { it.exists() }

            val resolvedStorePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: keystoreProperties.getProperty("storePassword")
                ?: "monkify_release_2026"
            val resolvedKeyAlias = System.getenv("KEY_ALIAS")
                ?: keystoreProperties.getProperty("keyAlias")
                ?: "monkify"
            val resolvedKeyPassword = System.getenv("KEY_PASSWORD")
                ?: keystoreProperties.getProperty("keyPassword")
                ?: "monkify_release_2026"

            if (targetKeystore != null && !resolvedStorePassword.isNullOrBlank() && !resolvedKeyAlias.isNullOrBlank() && !resolvedKeyPassword.isNullOrBlank()) {
                storeFile = targetKeystore
                storePassword = resolvedStorePassword
                keyAlias = resolvedKeyAlias
                keyPassword = resolvedKeyPassword
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "BASE_URL", "\"https://monkify-backend.monkify.workers.dev/api/v1/\"")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "BASE_URL", "\"https://monkify-backend.monkify.workers.dev/api/v1/\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        disable += setOf("PropertyEscape", "GradleDependency", "AndroidGradlePluginVersion")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material.icons.extended)
    
    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    
    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    
    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // DataStore
    implementation(libs.datastore.preferences)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    
    // Navigation
    implementation(libs.navigation.compose)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Rive Animation Runtime
    implementation(libs.rive.android)

    // Location (Coarse Location for Weather)
    implementation(libs.play.services.location)
    
    testImplementation(libs.junit)

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
