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

// !! infrastructure for builds as a Kotlin user project
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach kotlinCompilationTask@ {
        // ("unconditionally pass the -Xreport-all-warnings CLI option to the Kotlin compiler")
        // ("unconditionally pass the -Xrender-internal-diagnostic-names CLI option to the Kotlin compiler")
        compilerOptions {
            // output reported warnings even in the presence of reported errors
            freeCompilerArgs.add("-Xreport-all-warnings")
            logger.info("<KUP> ${this@kotlinCompilationTask.path} : added -Xreport-all-warnings")
            // output kotlin.git-searchable names of reported diagnostics
            freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
            logger.info("<KUP> ${this@kotlinCompilationTask.path} : added -Xrender-internal-diagnostic-names")
        }
    }

// !! infrastructure for builds as a Kotlin user project
// ("implement a Gradle property for passing additional CLI options to the Kotlin compiler")
    val kotlinAdditionalCliOptions = providers.gradleProperty("kotlin_additional_cli_options")
        .map { if (it.isNotBlank()) it.split(" ") else emptyList() } // i sure hope that this level of parsing is in fact enough
        .orNull
    if (kotlinAdditionalCliOptions != null) {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach kotlinCompilationTask@ {
            compilerOptions {
                freeCompilerArgs.addAll(kotlinAdditionalCliOptions)
                logger.info("<KUP> ${this@kotlinCompilationTask.path} : added ${kotlinAdditionalCliOptions.joinToString(" ")}")
            }
        }
    }

// !! infrastructure for builds as a Kotlin user project
// ("unconditionally disable the -Werror compiler CLI option")
    afterEvaluate {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach kotlinCompilationTask@ {
            compilerOptions {
                allWarningsAsErrors.set(false)
                freeCompilerArgs.set(
                    freeCompilerArgs.getOrElse(emptyList()).filterNot { it == "-Werror" }
                )
                logger.info("<KUP> ${this@kotlinCompilationTask.path} : disabled -Werror")
            }
        }
    }

// !! infrastructure for builds as a Kotlin user project
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach kotlinCompilationTask@ {
        // ("pass the -Wextra CLI option to the Kotlin compiler when the -Werror compiler CLI option is disabled")
        // ("pass the -Xuse-fir-experimental-checkers CLI option to the Kotlin compiler when the -Werror compiler CLI option is disabled")
        compilerOptions {
            // opt into additional warning-reporting compilation checks
            freeCompilerArgs.add("-Wextra")
            logger.info("<KUP> ${this@kotlinCompilationTask.path} : added -Wextra")
            // opt into additional compilation checks hidden from users
            freeCompilerArgs.add("-Xuse-fir-experimental-checkers")
            logger.info("<KUP> ${this@kotlinCompilationTask.path} : added -Xuse-fir-experimental-checkers")
        }
    }
}