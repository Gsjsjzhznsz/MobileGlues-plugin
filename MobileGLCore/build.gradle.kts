// mg-3backends: Gradle wrapper module for the MobileGL core + unified
// dispatcher. The CMake invocation is the MobileGL repo's own root build,
// which produces in one pass:
//   - libMobileGL.so    : MobileGL core (DirectVulkan / DirectGLES backends)
//   - libmobileglues.so : the unified entry dispatcher (Air 6.0 "mg" entry;
//                         routes to libMobileGL.so / libmg_gles.so by
//                         MOBILEGL_BACKEND_TYPE, falling back to the
//                         /sdcard/MG/config.json backendType key the
//                         MobileGlues settings UI writes)
// The MobileGlues core itself (libmg_gles.so) is built by the :MobileGlues
// module; both trees vendor their own glslang/SPIRV-Cross, so they MUST stay
// separate CMake invocations.

plugins {
    id("com.android.library")
}

fun mobileGlAbiFilters(): List<String> {
    val abiList = (findProperty("mobilegl.abis") ?: System.getenv("MOBILEGL_ABIS") ?: "arm64-v8a").toString()
    return if (abiList.equals("all", ignoreCase = true)) {
        listOf("arm64-v8a", "x86_64")
    } else {
        abiList.split(',').map(String::trim).filter(String::isNotEmpty)
    }
}

android {
    namespace = "top.mobilegl.mobileglcore"
    compileSdk = 36

    ndkVersion = "27.3.13750724"

    defaultConfig {
        minSdk = 26

        ndk {
            abiFilters += mobileGlAbiFilters()
        }

        externalNativeBuild {
            cmake {
                arguments += listOf("-DCMAKE_BUILD_TYPE=Release")
            }
        }
    }

    externalNativeBuild {
        cmake {
            // The MobileGL submodule mounts the repo root; its root CMakeLists
            // wires core + dispatcher + all 3rdparty submodules itself.
            path = file("../MobileGL/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
