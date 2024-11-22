import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.sqlDelight) apply false
    alias(libs.plugins.gradleVersionsPlugin) apply false
    alias(libs.plugins.shadowPlugin) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
}

allprojects {
    configurations.all {
        resolutionStrategy {
            val kotlin_version: String? by project
            val defaultKotlinVersion = kotlin_version ?: "2.2.21"
            force("org.jetbrains.kotlin:kotlin-test:$defaultKotlinVersion")
            force("org.jetbrains.kotlin:kotlin-test-common:$defaultKotlinVersion")
            force("org.jetbrains.kotlin:kotlin-test-annotations-common:$defaultKotlinVersion")
        }
    }
}

fun HasConfigurableKotlinCompilerOptions<*>.configureCompilerOptions() {
    compilerOptions {
        val kotlin_api_version: String? by project
        val kotlin_language_version: String? by project

        kotlin_language_version?.let {
            println("[${project.name}] Custom LanguageVersion: $it")
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(it))
        }

        kotlin_api_version?.let {
            println("[${project.name}] Custom ApiVersion: $it")
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(it))
        }

        freeCompilerArgs.add("-Xpartial-linkage-loglevel=ERROR")
    }
}

subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        configure<KotlinJvmExtension> {
            configureCompilerOptions()
        }
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        configure<KotlinMultiplatformExtension> {
            configureCompilerOptions()
        }
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        configure<KotlinAndroidExtension> {
            configureCompilerOptions()
        }
    }

//    tasks.withType(com.google.devtools.ksp.gradle.KspTaskNative::class.java).configureEach {
//        val ksp_api_version: String? by project
//        val ksp_language_version: String? by project
//
//        compilerOptions {
//            ksp_language_version?.let {
//                println("[${project.name}] Custom KSP LanguageVersion: $it")
//                languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(it))
//            }
//
//            ksp_api_version?.let {
//                println("[${project.name}] Custom KSP ApiVersion: $it")
//                apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(it))
//            }
//        }
//    }
}