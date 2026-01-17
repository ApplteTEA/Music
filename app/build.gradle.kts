plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    //### Compose
    alias(libs.plugins.compose.compiler)

    //### Hilt
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.test.music"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.test.music"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    hilt {
        enableAggregatingTask = false
    }
}

dependencies {
    implementation(project(":presentation"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":playback"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    //### Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    //### Navigation
    implementation(libs.androidx.navigation.compose)

    //### Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
