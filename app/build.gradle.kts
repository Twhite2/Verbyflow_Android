import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ksp) // Using KSP exclusively for all annotation processing
}

// Use a straightforward approach for JVM configuration

// Force all Kotlin compile tasks to use Java 17
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

// Using KSP for all annotation processing (more efficient than kapt)

// Force specific versions for compatibility with Kotlin 1.8.22 and Hilt 2.44.2
configurations.all {
    resolutionStrategy.eachDependency {
        // Force a consistent version of JavaPoet
        if (requested.group == "com.squareup" && requested.name == "javapoet") {
            useVersion("1.13.0")
        }
        
        // Force consistent versions of Dagger/Hilt
        if (requested.group == "com.google.dagger") {
            useVersion(libs.versions.hilt.get())
        }
        
        // Make sure we use a compatible Kotlin metadata version
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-")) {
            useVersion(libs.versions.kotlin.get())
        }
    }
}

android {
    namespace = "com.example.verbyflow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.verbyflow"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // ML model settings
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
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
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }
    
    compileOptions {
        // We're using Java 17 for the project
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        // Target Java 17 bytecode
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xskip-prerelease-check",
            "-Xjvm-default=all"  // For better compatibility with older code
        )
    }
    
    // Additional Kotlin/Java settings are configured above
    
    // Configure KSP for Hilt 2.51+ and Room
    ksp {
        // Room options
        arg("room.generateKotlin", "true")  // Generate Room code in Kotlin
        arg("room.schemaLocation", "$projectDir/schemas") // Store Room schema
        
        // Basic KSP settings
        arg("ksp.incremental", "true")
    }
    
    // No need for module access setup since KSP doesn't require JDK module access
    
    // Configure JavaCompile tasks with standard memory settings
    tasks.withType<JavaCompile>().configureEach {
        // Increase memory for Java compilation
        options.forkOptions.jvmArgs = listOf(
            "-Xmx1g"
        )
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    
    // Direct task configuration handled by the project.afterEvaluate block
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Exclude specific conflicting files
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

// JVM targets are configured at the top of the file

// Protobuf configuration for gRPC
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.12"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.56.1"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.3.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
                create("grpckt")
            }
            task.builtins {
                create("java")
                create("kotlin")
            }
        }
    }
    // Set proto location to avoid collisions
    @Suppress("DEPRECATION")
    generatedFilesBaseDir = "$projectDir/build/generated/source/proto"
}

dependencies {
    // Enable desugaring for JDK APIs on older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    // AndroidX & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Compose compiler compatible with Kotlin 1.8.22
    implementation(libs.androidx.compose.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    
    // WebRTC - using Infobip's version which is available in Maven Central
    implementation(libs.webrtc)
    // Standard audio dependencies for WebRTC
    implementation("androidx.media:media:1.7.0")
    // implementation(files("libs/libwebrtc.aar"))
    
    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.json)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    
    // Hilt dependencies with KSP (modern annotation processing)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler) // Using KSP for Hilt 2.51+
    implementation(libs.hilt.navigation.compose)
    
    // Add explicit KSP API dependency to ensure correct API is used
    compileOnly("com.google.devtools.ksp:symbol-processing-api:${libs.versions.ksp.get()}")
    
    // Room Database with KSP support
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler) // Using KSP for Room
    
    // gRPC and Protobuf
    implementation("io.grpc:grpc-okhttp:1.56.1")
    implementation("io.grpc:grpc-stub:1.56.1")
    implementation("io.grpc:grpc-protobuf:1.56.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.google.protobuf:protobuf-java:3.21.12")
    
    // Kotlin implementations
    implementation("io.grpc:grpc-kotlin-stub:1.3.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.21.12")
    
    // DataStore
    implementation(libs.datastore.preferences)
    
    // AI Models
    implementation(libs.tensorflowlite)
    implementation(libs.tensorflowlite.gpu)
    implementation(libs.tensorflowlite.support)
    implementation(libs.tensorflowlite.metadata)
    
    // Audio/Visual
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    
    // Material icons (for KeyboardVoice, CallEnd, etc.)
    implementation("androidx.compose.material:material-icons-extended:1.6.1")
    
    // Animation
    implementation("androidx.compose.animation:animation:1.6.1")
    
    // Accompanist for permissions
    implementation("com.google.accompanist:accompanist-permissions:0.30.1")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}