import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.triplet.play")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.agentickitchen.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.agentickitchen.android"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

play {
    useApplicationDefaultCredentials = true
    System.getenv("ANDROID_PUBLISHER_IMPERSONATE_SERVICE_ACCOUNT")
        ?.takeIf { it.isNotBlank() }
        ?.let { impersonateServiceAccount = it }
    defaultToAppBundles.set(true)
    track.set("internal")
    releaseStatus.set(ReleaseStatus.DRAFT)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("app.cash.sqldelight:android-driver:2.0.0")

    val ktorVersion = "3.0.3"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    val firebaseBom = platform("com.google.firebase:firebase-bom:34.18.0")
    implementation(firebaseBom)
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-appcheck")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")
    releaseImplementation("com.google.firebase:firebase-appcheck-playintegrity")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
