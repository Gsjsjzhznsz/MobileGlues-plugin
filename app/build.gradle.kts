plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// mg-3backends: fork CI passes the SIGNING_* secrets as EMPTY strings, and a
// release must stay installable (stable signature across runs), so blank
// counts as absent and the committed fork-owned PKCS12 kicks in — the same
// keystore the MobileGL android-plugin signs with. Env/property overrides
// remain for the upstream side and for rotation.
fun signingEnvOr(name: String, fallback: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
        ?: fallback

val forkSigningReady = file("../keystore-air.p12").exists()

android {
    namespace = "com.fcl.plugin.mobileglues"
    compileSdk = 37

    ndkVersion = "27.3.13750724"

    defaultConfig {
        applicationId = "com.fcl.plugin.mobileglues"
        minSdk = 26
        targetSdk = 37
        versionCode = 2001
        versionName = "2.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // mg-3backends: keep the APK's ABI set identical across every native
        // module (:MobileGLCore restricts itself with the same key). Default
        // arm64-v8a; override with -Pmobilegl.abis=... or MOBILEGL_ABIS.
        ndk {
            abiFilters += (findProperty("mobilegl.abis") ?: System.getenv("MOBILEGL_ABIS") ?: "arm64-v8a")
                .toString().split(',').map(String::trim).filter(String::isNotEmpty)
        }
    }

    signingConfigs {
        create("release") {
            if (forkSigningReady) {
                storeFile = file("../keystore-air.p12")
                storePassword = signingEnvOr("SIGNING_STORE_PASSWORD", "mgair-air-3backends")
                keyAlias = signingEnvOr("SIGNING_KEY_ALIAS", "mgair")
                keyPassword = signingEnvOr("SIGNING_KEY_PASSWORD", "mgair-air-3backends")
            } else {
                storeFile = file("../keystore.jks")
                storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: project.findProperty("SIGNING_STORE_PASSWORD") as String?
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: project.findProperty("SIGNING_KEY_ALIAS") as String?
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: project.findProperty("SIGNING_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        configureEach {
            resValue("string","app_name","MobileGlues")

            manifestPlaceholders["des"] = "MobileGlues (OpenGL 4.0, 1.17+)"
            manifestPlaceholders["renderer"] = "MobileGlues:libmobileglues.so:libmobileglues.so"

            manifestPlaceholders["minMCVer"] = "1.17"
            manifestPlaceholders["maxMCVer"] = "" //为空则不限制 No restriction if empty

            // MG_COUNT_LAUNCH：只有经启动器起来的这一次才算「一次启动」。
            // 基准测试、各种工具、以及本插件自己 dlopen 一次去读 GL 信息，都不该计数。
            manifestPlaceholders["boatEnv"] = mutableMapOf<String,String>().apply {
                put("LIBGL_ES", "3")
                put("MG_COUNT_LAUNCH", "1")
            }.run {
                var env = ""
                forEach { (key, value) ->
                    env += "$key=$value:"
                }
                env.dropLast(1)
            }
            manifestPlaceholders["pojavEnv"] = mutableMapOf<String,String>().apply {
                put("LIBGL_ES", "3")
                put("POJAV_RENDERER", "opengles3")
                                put("POJAVEXEC_EGL", "libmobileglues.so")
                                put("LIBGL_EGL", "libmobileglues.so")
                put("MG_COUNT_LAUNCH", "1")
            }.run {
                var env = ""
                forEach { (key, value) ->
                    env += "$key=$value:"
                }
                env.dropLast(1)
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        buildConfig = true
        aidl = true
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            // mg-3backends: defensive — the MobileGL tree vendors SPIRV-Tools;
            // a shared build of it must never slip into the APK.
            excludes += "**/libSPIRV-Tools-shared.so"
        }
    }
}

dependencies {
    implementation(libs.gson)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.preference)
    // 协程和 lifecycleScope 以前是从 appcompat 传递依赖里蹭来的，这里显式声明。
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(project(":MobileGlues"))
    // mg-3backends: the unified renderer entry (dispatcher libmobileglues.so)
    // plus the MobileGL core (libMobileGL.so, DirectVulkan / DirectGLES) ride
    // in the APK next to the MobileGlues core (libmg_gles.so, :MobileGlues).
    implementation(project(":MobileGLCore"))

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
