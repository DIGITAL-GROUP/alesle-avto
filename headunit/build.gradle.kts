// A desktop head unit: shows a phone's projection in a window, with touch, keys, audio,
// microphone and a simulated drive. For testing Alesle Avto (and other phone implementations).
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

application {
    mainClass.set("uz.alesle.avto.headunit.MainKt")
    applicationName = "alesle-headunit"
    applicationDefaultJvmArgs = listOf("-Xmx384m", "-Dsun.java2d.opengl=false")
}

dependencies {
    implementation(project(":aap"))
    testImplementation(libs.junit)
}
