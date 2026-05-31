import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    id("local.i18n-auto-merge")
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val generatedRestorableRegistryDir = layout.buildDirectory.dir("generated/restorableScreenRegistry/commonMain/kotlin")
        val generatedI18nKotlinDir = layout.buildDirectory.dir("generated/i18n/kotlin")
        commonMain {
            kotlin.srcDir(generatedRestorableRegistryDir)
            kotlin.srcDir(generatedI18nKotlinDir)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.work.runtime.ktx)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.coil3.compose)
            implementation(libs.coil3.gif)
            implementation(libs.coil3.network.ktor3)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.yamibo.api)
            implementation(libs.ksoup)
            implementation(projects.shared)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}

compose.resources {
    customDirectory("commonMain", layout.buildDirectory.dir("generated/i18n/composeResources"))
}

i18nAutoMerge {
    scanDirs.set(listOf("composeApp/src", "shared/src"))
    glossary.set(rootProject.layout.projectDirectory.file("i18n/glossary.csv"))
    baseTranslations.set(rootProject.layout.projectDirectory.file("i18n/base.csv"))
    existingComposeResourcesDir.set(rootProject.layout.projectDirectory.dir("composeApp/src/commonMain/composeResources"))
    outputComposeResources.set(layout.buildDirectory.dir("generated/i18n/composeResources"))
    outputKotlin.set(layout.buildDirectory.dir("generated/i18n/kotlin"))
    reportDir.set(layout.buildDirectory.dir("reports/i18n"))
    defaultLanguage.set("zh-tw")
    fallbackToSource.set(true)
    failOnPlaceholderMismatch.set(true)
    failOnMissingTranslation.set(false)
    apiFunctionName.set("i18n")
    runtimePackage.set("me.thenano.yamibo.yamibo_app.i18n")
    resImportPackage.set("yamibo_app.composeapp.generated.resources")
}

val generateRestorableScreenRegistry by tasks.registering(GenerateRestorableScreenRegistryTask::class) {
    val sourceDir = layout.projectDirectory.dir("src/commonMain/kotlin")
    this.sourceDir.set(sourceDir)
    outputFile.set(layout.buildDirectory.file("generated/restorableScreenRegistry/commonMain/kotlin/me/thenano/yamibo/yamibo_app/navigation/GeneratedRestorableScreenRegistry.kt"))
}

tasks.matching { task ->
    task.name.startsWith("compile") && task.name.contains("Kotlin")
}.configureEach {
    dependsOn(generateRestorableScreenRegistry)
}

android {
    namespace = "me.thenano.yamibo.yamibo_app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "me.thenano.yamibo.yamibo_app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies { debugImplementation(compose.uiTooling) }
val scriptPath = rootProject.rootDir.absolutePath + "/unlock_build.ps1"

val autoUnlockTask = tasks.register<Exec>("autoUnlockResources") {
    commandLine("powershell.exe", "-ExecutionPolicy", "Bypass", "-NoProfile", "-File", scriptPath)
    isIgnoreExitValue = true
}

tasks.whenTaskAdded {
    if (name == "processDebugResources" || name == "processReleaseResources" || name == "mergeDebugResources") {
        dependsOn(autoUnlockTask)
    }
}

