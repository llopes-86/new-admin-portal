plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

import java.util.Properties

// =============================================================================
// VERSÃO DO APP — derivada do GIT (fonte da verdade = tag `apk/vX.Y.Z`)
// =============================================================================
//   tag em HEAD e árvore limpa ....... versionName = "1.2.3"
//   N commits à frente da tag ........ versionName = "1.2.3-devN"
//   árvore suja (alterações locais) .. versionName = "1.2.3-devN.dirty"
//   sem git (ex.: CI sem repositório)  versionName = "0.0.0-dev"
//   versionCode = número de commits (monótono e único por build)
// Tags de release: `tools/bump-app-version.ps1 -Version 1.2.3` cria `apk/v1.2.3`.
//
// Observação: o configuration cache do Gradle fica DESLIGADO (ver gradle.properties)
// porque a versão depende do estado do git, que muda a cada commit — com o cache
// ligado o Gradle reutilizaria uma versão antiga.
// =============================================================================
fun gitOutput(vararg args: String): String? =
    runCatching {
        providers
            .exec {
                commandLine(listOf("git") + args)
                workingDir(rootProject.projectDir.parentFile)
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim()
      }
      .getOrNull()
      ?.ifEmpty { null }

val gitDescribe: String? = gitOutput("describe", "--tags", "--match", "apk/v*", "--long", "--dirty")
val gitCommitCount: Int = gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 0
val gitShortSha: String? = gitOutput("rev-parse", "--short", "HEAD")

data class AppVersion(val name: String, val code: Int, val tag: String?, val commits: Int, val dirty: Boolean)

val appVersion: AppVersion =
    if (gitDescribe == null) {
        AppVersion(name = "0.0.0-dev", code = maxOf(gitCommitCount, 1), tag = null, commits = gitCommitCount, dirty = false)
    } else {
        val dirty = gitDescribe.endsWith("-dirty")
        val withoutDirty = gitDescribe.removeSuffix("-dirty")
        // formato: apk/v1.2.3-4-gabc1234
            val match = Regex("apk/v(\\d+)\\.(\\d+)\\.(\\d+)(?:-(\\d+)-g([0-9a-f]+))?").find(withoutDirty)
        if (match == null) {
            AppVersion(name = "0.0.0-dev", code = maxOf(gitCommitCount, 1), tag = null, commits = gitCommitCount, dirty = dirty)
        } else {
            val major = match.groupValues[1]
            val minor = match.groupValues[2]
            val patch = match.groupValues[3]
            val distance = match.groupValues[4].toIntOrNull() ?: 0
            val base = "$major.$minor.$patch"
            val name =
                when {
                    distance == 0 && !dirty -> base
                    distance == 0 -> "$base-dev0.dirty"
                    else -> "$base-dev$distance" + if (dirty) ".dirty" else ""
                }
            AppVersion(
                name = name,
                code = maxOf(gitCommitCount, 1),
                tag = "apk/v$base",
                commits = distance,
                dirty = dirty,
            )
        }
    }

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.bodasmel.adminml"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.bodasmel.adminml"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersion.code
        versionName = appVersion.name

        buildConfigField("String", "VERSION_NAME", "\"${appVersion.name}\"")
        buildConfigField("int", "VERSION_CODE", "${appVersion.code}")
        buildConfigField("String", "GIT_COMMIT", "\"${gitShortSha ?: "desconhecido"}\"")
        buildConfigField("String", "GIT_TAG", "\"${appVersion.tag ?: "sem tag"}\"")
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    testOptions {
      unitTests {
        isIncludeAndroidResources = true
        isReturnDefaultValues = true
      }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Rede (PocketBase + Gotify WebSocket)
  implementation(libs.okhttp)

  // Armazenamento seguro (token PocketBase / client token Gotify)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.datastore.preferences)

  // JSON (modelos do PocketBase e mensagens do Gotify)
  implementation(libs.kotlinx.serialization.json)
}
