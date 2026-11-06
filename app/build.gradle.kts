plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Supplied only to the signing step in CI; debug/local unsigned builds need no key.
val releaseSigningNames = listOf(
    "ANDROID_KEYSTORE_PATH", "ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD"
)
val releaseSigningValues = releaseSigningNames.associateWith { providers.environmentVariable(it).orNull }
val hasReleaseSigning = releaseSigningValues.values.all { !it.isNullOrBlank() }
require(releaseSigningValues.values.all { it.isNullOrBlank() } || hasReleaseSigning) {
    "Release signing requires all four ANDROID_KEYSTORE_PATH / PASSWORD / KEY_ALIAS / KEY_PASSWORD variables."
}

android {
    namespace = "com.justinlin099.pdx206statusbarhookyaap"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.justinlin099.xperiarecentsclearall"
        minSdk = 36
        targetSdk = 36
        versionCode = 8
        versionName = "1.1.6-standalone"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseSigningValues["ANDROID_KEYSTORE_PATH"]))
                storePassword = releaseSigningValues["ANDROID_KEYSTORE_PASSWORD"]
                keyAlias = releaseSigningValues["ANDROID_KEY_ALIAS"]
                keyPassword = releaseSigningValues["ANDROID_KEY_PASSWORD"]
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            test.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED")
            test.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            listOf("http.proxyHost", "http.proxyPort", "https.proxyHost", "https.proxyPort").forEach { key ->
                System.getProperty(key)?.let { test.systemProperty(key, it) }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    compileOnly(libs.xposed.api)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
