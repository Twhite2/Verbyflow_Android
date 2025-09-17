import org.gradle.api.artifacts.repositories.MavenArtifactRepository

pluginManagement {
    repositories {
        // Maven Central first - hosts most dependencies including JavaPoet
        mavenCentral() 
        
        // Explicit Maven Central URL as fallback with longer timeout
        maven {
            url = uri("https://repo1.maven.org/maven2/")
            setAllowInsecureProtocol(true)
        }
        
        // Google's Maven repository for Android components
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        
        // Explicit Google Maven URL as fallback
        maven { 
            url = uri("https://maven.google.com/") 
            setAllowInsecureProtocol(true)
        }
        
        // JetBrains Compose repository (for org.jetbrains.compose plugin)
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        
        // Gradle plugin portal for Gradle plugins
        gradlePluginPortal()
        
        // JitPack for GitHub hosted libraries
        maven { url = uri("https://jitpack.io") }
        
        // Maven Local as fallback for offline use
        mavenLocal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Maven Central first - critical for JavaPoet and most Java dependencies
        mavenCentral()
        
        // Explicit Maven Central URL with longer timeout
        maven {
            url = uri("https://repo1.maven.org/maven2/")
            setAllowInsecureProtocol(true)
        }
        
        // Google's Maven repository - needed for Android dependencies
        google {
            // Ensure we can access the KSP artifacts for Dagger/Hilt
            content {
                includeGroup("com.google.dagger")
                includeGroupByRegex("com.android.*")
                includeGroupByRegex("com.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        
        // Explicit Google Maven URL as fallback
        maven { 
            url = uri("https://maven.google.com/") 
            setAllowInsecureProtocol(true)
        }
        
        // JetBrains Compose repository (for org.jetbrains.compose dependencies)
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        
        // Sonatype repositories for Dagger/Hilt (snapshot and release)
        maven { 
            url = uri("https://oss.sonatype.org/content/repositories/releases/") 
            setAllowInsecureProtocol(true)
        }
        maven { 
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/") 
            setAllowInsecureProtocol(true)
        }
        
        // JitPack for GitHub hosted libraries (e.g., WebRTC)
        maven { url = uri("https://jitpack.io") }
        
        // Maven Local for offline development
        mavenLocal()
    }
}

// SSL verification handling for repository connections
fun org.gradle.api.artifacts.repositories.MavenArtifactRepository.relaxSslVerification() {
    val repoUrl = this.url.toString()
    if (repoUrl.contains("repo.maven.apache.org") || 
        repoUrl.contains("repo1.maven.org") || 
        repoUrl.contains("maven.google.com") || 
        repoUrl.contains("oss.sonatype.org")) {
        println("Repository at $repoUrl using relaxed SSL verification")
        try {
            this.setAllowInsecureProtocol(true)
        } catch (e: Exception) {
            println("Failed to set allowInsecureProtocol: ${e.message}")
        }
    }
}

// Apply relaxed SSL verification to all Maven repositories
pluginManagement.repositories.withType<MavenArtifactRepository>().configureEach { relaxSslVerification() }
dependencyResolutionManagement.repositories.withType<MavenArtifactRepository>().configureEach { relaxSslVerification() }

// Project configuration
rootProject.name = "Verbyflow"
include(":app")

// Uncomment the line below to use local repositories if online ones are unavailable
// apply(from = "offline-repo.gradle")
