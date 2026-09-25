pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MobileGlues-plugin"
include(":app")
include(":MobileGlues")
// mg-3backends: MobileGL core + unified dispatcher (libMobileGL.so +
// libmobileglues.so) built from the MobileGL submodule's own root CMake.
include(":MobileGLCore")
