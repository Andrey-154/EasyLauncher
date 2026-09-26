import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("androidx.room")
}

// Every commit raises the version code, so each new APK installs as an update
val gitCommitCount: Int = try {
    providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
        .standardOutput.asText.get().trim().toInt()
} catch (_: Exception) {
    1
}

android {
    namespace = "com.minimo.launcher"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.easy.launcher"
        minSdk = 26
        targetSdk = 37
        versionCode = gitCommitCount
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    // Sign only when every field is present; an incomplete file must not break debug builds
    val hasReleaseKeystore = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        .all { !keystoreProps.getProperty(it).isNullOrBlank() }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    flavorDimensions += "store"
    productFlavors {
        create("foss") {
            dimension = "store"
        }
        create("playStore") {
            dimension = "store"
        }
    }

    buildTypes {
        debug {
        }

        release {
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
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

    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
        includeInApk = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))

    implementation("androidx.compose.material3:material3")
    // Icons for home screen quick buttons (unused icons are removed by R8)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui-graphics")

    implementation("androidx.appcompat:appcompat:1.8.0")
    // Fingerprint / screen lock check for hidden apps
    implementation("androidx.biometric:biometric:1.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.navigation:navigation-compose:2.10.0")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0")

    implementation("androidx.room:room-runtime:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")

    implementation("androidx.datastore:datastore-preferences:1.2.1")

    implementation("com.jakewharton.timber:timber:5.0.1")
}
