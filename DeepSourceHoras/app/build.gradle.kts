import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    jacoco
}

// Datos de la firma del APK release (archivo keystore.properties en la raíz del proyecto)
val firma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

// IP donde corren los microservicios. 10.0.2.2 es el "localhost" del PC visto desde el emulador.
// Para un teléfono físico: gradlew assembleDebug -Papi.host=192.168.1.X
val apiHost: String = providers.gradleProperty("api.host").getOrElse("10.0.2.2")

android {
    namespace = "com.example.deepsourcehoras"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.deepsourcehoras"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "URL_USUARIOS", "\"http://$apiHost:8081/\"")
        buildConfigField("String", "URL_HORAS", "\"http://$apiHost:8082/\"")
        buildConfigField("String", "URL_FERIADOS", "\"https://date.nager.at/\"")
    }

    signingConfigs {
        create("release") {
            if (!firma.isEmpty) {
                storeFile = rootProject.file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Genera los datos de cobertura al correr las pruebas unitarias
            enableUnitTestCoverage = true
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            // R8 apagado a propósito: la optimización parcial (packageScope) del template
            // dejaba el APK release cayéndose al abrir (IllegalAccessError / WorkDatabase).
            // El APK igual queda firmado con la llave .jks.
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // Las clases de Android que no se simulan devuelven valores por defecto en vez de fallar
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.glance:glance-appwidget:1.1.1")

    // Consumo de los microservicios y de la API externa
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// ---------- Cobertura de la lógica ----------
// gradlew coberturaLogica  ->  app/build/reports/jacoco/coberturaLogica/html/index.html
// Mide solo las capas con lógica (ViewModel, Repository, validaciones, utilidades, modelos);
// las pantallas Compose y las clases que dependen del teléfono se prueban en el emulador.
val paquete = "com/example/deepsourcehoras"
tasks.register<JacocoReport>("coberturaLogica") {
    group = "verification"
    description = "Reporte de cobertura de las pruebas unitarias sobre la lógica de la app"
    dependsOn("testDebugUnitTest")

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
            include(
                "$paquete/ui/viewmodel/**",
                "$paquete/data/repository/**",
                "$paquete/data/remote/ErroresRed*",
                "$paquete/data/remote/dto/**",
                "$paquete/domain/**",
                "$paquete/util/**",
                "$paquete/model/**"
            )
        }
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("outputs/unit_test_code_coverage/debugUnitTest/*.exec")
        }
    )
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}
