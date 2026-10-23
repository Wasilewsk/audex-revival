import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.isFile &&
    keystoreProperties.getProperty("storeFile")?.isNotBlank() == true
val defaultThemeStoreUrl =
    "https://raw.githubusercontent.com/Wasilewsk/audex-themes/main/index.json"

android {
    namespace = "com.infinity.audex_revivel"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.infinity.audex_revivel"
        minSdk = 23
        targetSdk = 36
        versionCode = 1024
        versionName = "7.0"
        buildConfigField("String", "THEME_STORE_URL", "\"$defaultThemeStoreUrl\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Install a development copy without replacing the release-signed app.
            if (providers.gradleProperty("studioTest").orNull == "true") {
                applicationIdSuffix = ".studiotest"
                resValue("string", "app_name", "Audex Revival 7.0 Test")
            }
            // Optional local store index for development: ./gradlew -PthemeStoreUrl=http://10.0.2.2:8000/index.json
            val override = (project.findProperty("themeStoreUrl") as? String)?.trim()
            if (!override.isNullOrEmpty()) {
                buildConfigField("String", "THEME_STORE_URL", "\"$override\"")
            }
        }
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
