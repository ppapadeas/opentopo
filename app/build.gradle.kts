plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "org.opentopo.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.opentopo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // For development, use debug keystore
            // Replace with proper keystore for Play Store release
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Workaround: AGP lint detectors crash with IncompatibleClassChangeError
        // when used with material3 1.5.0-alpha. Allow build to continue.
        abortOnError = false
    }
}

dependencies {
    implementation(project(":lib-transform"))

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2026.03.01"))

    // Material 3 Expressive (override BOM to get expressive APIs)
    implementation("androidx.compose.material3:material3:1.5.0-alpha17")
    implementation("androidx.compose.material3:material3-window-size-class:1.5.0-alpha17")
    implementation("androidx.compose.material:material-icons-extended")

    // Graphics shapes for MaterialShapes / RoundedPolygon
    implementation("androidx.graphics:graphics-shapes:1.0.1")

    // Compose core
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // MapLibre
    implementation("org.maplibre.gl:android-sdk:11.8.4")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // USB Serial
    implementation("com.github.mik3y:usb-serial-for-android:3.10.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // DataStore (preferences persistence)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Core AndroidX
    implementation("androidx.core:core-ktx:1.15.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.03.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
