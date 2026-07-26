plugins {
    id("com.android.application") version "8.1.4" apply false
    kotlin("android") version "1.9.21" apply false
    kotlin("jvm") version "1.9.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21" apply false
    id("app.cash.sqldelight") version "2.0.0" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
