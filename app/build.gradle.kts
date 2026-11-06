plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
        versionCode = 7
        versionName = "1.1.5-standalone"

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
    compileOnly("de.robv.android.xposed:api:82")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.17")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
