pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // Multiple Maven repository URLs to handle connection issues
        maven { url = uri("https://repo1.maven.org/maven2/") }
        maven { url = uri("https://maven.google.com/") }
        mavenCentral()
        gradlePluginPortal()
        // JCenter as fallback
        jcenter()
        // JitPack for GitHub hosted libraries
        maven { url = uri("https://jitpack.io") }
        // Maven Local as fallback
        mavenLocal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Multiple Maven repository URLs to handle connection issues
        maven { url = uri("https://repo1.maven.org/maven2/") }
        maven { url = uri("https://maven.google.com/") }
        mavenCentral()
        // JCenter as fallback
        jcenter()
        // JitPack for GitHub hosted libraries (e.g., WebRTC)
        maven { url = uri("https://jitpack.io") }
        // Maven Local as fallback
        mavenLocal()
    }
}

rootProject.name = "Verbyflow"
include(":app")

// Uncomment the line below if you continue to have connection issues
// apply(from = "fix-gradle-connection.gradle")

// Uncomment the line below to use local repositories if online ones are unavailable
// apply(from = "offline-repo.gradle")
