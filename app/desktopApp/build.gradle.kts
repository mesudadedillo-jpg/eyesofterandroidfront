import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":app:shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.example.eyesofterapp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.example.eyesofterapp"
            packageVersion = "1.0.0"
        }
    }
}
// Capturas de las pantallas con datos de ejemplo (ver capturas/Capturas.kt).
tasks.register<JavaExec>("capturas") {
    group = "documentation"
    description = "Genera PNG de las pantallas en build/capturas/"
    mainClass.set("com.example.eyesofterapp.capturas.CapturasKt")
    classpath = sourceSets["main"].runtimeClasspath
    args(layout.buildDirectory.dir("capturas").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}
