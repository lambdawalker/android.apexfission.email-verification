plugins {
    alias(libs.plugins.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.apexfission.android.emailverification"
    compileSdk { version = release(37) }

    defaultConfig { minSdk = 28 }

    buildFeatures { compose = true }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.preview)
    implementation(libs.lifecycle.runtime)
    api(libs.coroutines)
    implementation(libs.ui.common)
    debugImplementation(libs.compose.tooling)
    screenshotTestImplementation(libs.compose.tooling)
    screenshotTestImplementation(libs.screenshot.api)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
