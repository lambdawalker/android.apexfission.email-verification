plugins {
    alias(libs.plugins.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.apexfission.android.emailverification.demo"
    compileSdk { version = release(37) }

    defaultConfig {
        applicationId = "com.apexfission.android.emailverification.demo"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-dev"
    }

    buildFeatures { compose = true }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":email-verification"))
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.compose.material3)
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    screenshotTestImplementation(libs.compose.tooling)
    screenshotTestImplementation(libs.screenshot.api)
}
