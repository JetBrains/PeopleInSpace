import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.sqlDelight) apply false
    alias(libs.plugins.gradleVersionsPlugin) apply false
    alias(libs.plugins.shadowPlugin) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
}

// force patched versions of vulnerable transitive npm deps of the wasm webpack tooling
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension>().apply {
        resolution("ws", "8.21.0")
        resolution("serialize-javascript", "7.0.5")
    }
}

allprojects {
    configurations.all {
        resolutionStrategy {
            val kotlin_version: String? by project
            val defaultKotlinVersion = kotlin_version ?: "2.4.10"
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

    tasks.withType<Tar>{
        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    tasks.withType<Zip>{
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}