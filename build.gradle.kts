import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.11"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.ui)
    implementation(compose.uiTooling)
    implementation(files("libs/jcard.jar"))
    
    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Firebase Admin SDK
    implementation("com.google.firebase:firebase-admin:9.2.0")
    
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "GymSmartcardApp"
            packageVersion = "1.0.0"
        }
    }
}
