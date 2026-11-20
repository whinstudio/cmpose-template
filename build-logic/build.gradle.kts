import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.multiplatform.gradle.plugin)

    implementation(libs.gradle)

    implementation(libs.compose.gradle.plugin)

    implementation(libs.kotlin.serialization)

    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.detekt.plugin)

    implementation(libs.ksplib)

    // The catalog's kotlin-test; on the JUnit Platform it resolves to its JUnit 5 variant
    testImplementation(libs.kotlin.test)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "com.kturker.multiplatform.kmp.library"
            implementationClass = "convention.KmpLibraryConventionPlugin"
        }

        register("composeMultiplatform") {
            id = "com.kturker.multiplatform.compose.multiplatform"
            implementationClass = "convention.ComposeMultiplatformConventionPlugin"
        }

        register("multiplatformApplication") {
            id = "com.kturker.multiplatform.multiplatformApplication"
            implementationClass = "convention.ComposeMultiplatformAppConventionPlugin"
        }

        register("featureContract") {
            id = "com.kturker.multiplatform.feature.contract"
            implementationClass = "convention.FeatureContractConventionPlugin"
        }

        register("featureData") {
            id = "com.kturker.multiplatform.feature.data"
            implementationClass = "convention.FeatureDataConventionPlugin"
        }

        register("featureDomain") {
            id = "com.kturker.multiplatform.feature.domain"
            implementationClass = "convention.FeatureDomainConventionPlugin"
        }

        register("featurePresentation") {
            id = "com.kturker.multiplatform.feature.presentation"
            implementationClass = "convention.FeaturePresentationConventionPlugin"
        }

        register("detektConvention") {
            id = "com.kturker.multiplatform.detekt"
            implementationClass = "convention.DetektConventionPlugin"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform()
}
