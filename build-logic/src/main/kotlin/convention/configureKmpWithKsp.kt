package convention

import com.google.devtools.ksp.gradle.KspAATask
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

internal fun Project.configureKmpWithKsp(
    ext: KotlinMultiplatformExtension
) {

    pluginManager.apply("com.google.devtools.ksp")

    ext.sourceSets.named("commonMain").configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }

    extensions.configure(KspExtension::class.java) {
        arg("KOIN_CONFIG_CHECK", "true")
    }

    dependencies.add("kspCommonMainMetadata", libs.koin.ksp.compiler)

    tasks.withType(KotlinCompilationTask::class.java).configureEach {
        if (name != "kspCommonMainKotlinMetadata") {
            dependsOn("kspCommonMainKotlinMetadata")
        }
    }

    // The other targets' KSP tasks read commonMain too, which holds the generated metadata sources
    tasks.withType(KspAATask::class.java).configureEach {
        if (name != "kspCommonMainKotlinMetadata") {
            dependsOn("kspCommonMainKotlinMetadata")
        }
    }
}