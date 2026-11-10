import config.AppConfig

// Not the kmp.library convention plugin: it adds Koin and :core:logging, which plain API models do not need.
plugins {
    // build-logic on the classpath (AppConfig, the serialization plugin) without applying it: the detekt plugin
    // below is the only other build-logic plugin here, and a project without detekt drops it
    id("com.kturker.multiplatform.kmp.library") apply false
    id("com.kturker.multiplatform.detekt")
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

// On the classpath through build-logic; a plugins {} request would need its marker artifact.
apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

kotlin {
    android {
        namespace = "${AppConfig.applicationId}.core.api"
        compileSdk = AppConfig.compileSdkVersion
        minSdk = AppConfig.minSdkVersion
        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// ApiContractTest checks the models and routes against the server's API description, which its writeOpenApi
// task writes; without the file the test is skipped. In a generated project the server is an included build
// named "server": the host test then writes the description first and fails, not skips, when it is missing.
val openApiFile = rootProject.file("server/build/openapi/openapi.json")
val serverIsIncludedBuild = gradle.includedBuilds.any { it.name == "server" }

// ApiContractTest also reads the client's call sites for their HTTP methods: the features that call the API, the
// ones this project has (a project without sharing has no feature/sharing).
val clientSourceDirs = listOf("feature/account", "feature/sharing").map(rootProject::file).filter { it.isDirectory }

// Its inputs are only the main source sets (<module>/src/<name>Main) in them: a tree over the whole feature directory
// would take in the modules' build/ outputs, which other tasks write.
val clientMainSourceDirs = clientSourceDirs.flatMap { dir ->
    dir.walkTopDown()
        .onEnter { it.name != "build" && it.parentFile?.parentFile?.name != "src" }
        .filter { it.isDirectory && it.name.endsWith("Main") && it.parentFile?.name == "src" }
        .toList()
}

tasks.withType<Test>().configureEach {
    systemProperty("cmpose.openapi", openApiFile.absolutePath)
    systemProperty("cmpose.openapi.required", serverIsIncludedBuild.toString())
    inputs.files(openApiFile).withPropertyName("openApi").withPathSensitivity(PathSensitivity.NONE)
    systemProperty("cmpose.clientSources", clientSourceDirs.joinToString(File.pathSeparator) { it.absolutePath })
    inputs.files(clientMainSourceDirs.map { dir -> fileTree(dir) { include("**/*.kt") } })
        .withPropertyName("clientSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    if (serverIsIncludedBuild) {
        dependsOn(gradle.includedBuild("server").task(":writeOpenApi"))
    }
}
