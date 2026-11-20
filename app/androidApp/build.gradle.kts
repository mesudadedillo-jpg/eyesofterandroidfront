import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// Firebase (push, M-RI-02): se activa al poner aquí el google-services.json del proyecto de
// Firebase (consola → Configuración del proyecto → Tus apps → Android). Sin él la app compila
// igual y los avisos llegan por la bandeja y el correo (M-RFNN-04).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(project(":app:shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.androidx.graphics.path)
    implementation(libs.maplibre.android)
    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.clientOkhttp)
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.kotlinx.serializationJson)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.fragment)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.example.eyesofterapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.eyesofterapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        // C-RC-03: la versión que se publica solo habla HTTPS. HTTP solo en debug (servidor local).
        debug { manifestPlaceholders["permitirHttp"] = "true" }
        release { manifestPlaceholders["permitirHttp"] = "false" }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
