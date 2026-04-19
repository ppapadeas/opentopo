plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val _localProps: Map<String, String> by lazy {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.readLines()
            .filter { it.contains("=") && !it.startsWith("#") }
            .associate {
                val (k, v) = it.split("=", limit = 2)
                k.trim() to v.trim()
            }
    } else emptyMap()
}
fun localProp(key: String): String? = _localProps[key]

android {
    namespace = "org.opentopo.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.opentopo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = providers.exec {
            commandLine("git", "describe", "--tags", "--always", "--dirty")
        }.standardOutput.asText.get().trim()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val sf = localProp("RELEASE_STORE_FILE")
            if (sf != null) {
                storeFile = file(sf)
                storePassword = localProp("RELEASE_STORE_PASSWORD")
                keyAlias = localProp("RELEASE_KEY_ALIAS")
                keyPassword = localProp("RELEASE_KEY_PASSWORD")
            }
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
    implementation("androidx.compose.ui:ui-text-google-fonts")
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
