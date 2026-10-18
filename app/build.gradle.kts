plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.miglycal"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.miglycal"
        minSdk = 26
        targetSdk = 35
        versionCode = 126
        versionName = "1.2.6"
    }

    signingConfigs {
        create("release") {
            // A chave é fornecida apenas pelo GitHub Actions Secrets.
            // Nunca guardar o keystore ou as passwords no repositório.
            val keystorePath = System.getenv("MIGLYCAL_KEYSTORE_PATH")
            val keystorePassword = System.getenv("MIGLYCAL_KEYSTORE_PASSWORD")
            val keyAliasValue = System.getenv("MIGLYCAL_KEY_ALIAS")
            val keyPasswordValue = System.getenv("MIGLYCAL_KEY_PASSWORD")

            if (!keystorePath.isNullOrBlank()) storeFile = file(keystorePath)
            if (!keystorePassword.isNullOrBlank()) storePassword = keystorePassword
            if (!keyAliasValue.isNullOrBlank()) keyAlias = keyAliasValue
            if (!keyPasswordValue.isNullOrBlank()) keyPassword = keyPasswordValue
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
            )
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    implementation("io.coil-kt:coil-compose:2.7.0")

    // Leitura/escrita de etiquetas (ID3 e Vorbis/FLAC) - fork compatível com Android
    implementation("com.github.AdrienPoupa:jaudiotagger:2.2.3")
}
