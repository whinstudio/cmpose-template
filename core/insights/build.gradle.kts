plugins {
    alias(libs.plugins.kmp.library.plugin)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kermit.test)
        }
        androidMain.dependencies {
            // On iOS the Firebase SDK is not a dependency: the app links Crashlytics and Analytics through SPM and
            // hands them to Kotlin through IosInsightsBridge
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.analytics)
        }
        iosMain.dependencies {
            implementation(libs.crashkios.crashlytics)
        }
    }
}

// CrashKiOS references Crashlytics' FIRCLSExceptionRecordNSException, which the app gets from FirebaseCrashlytics
// (SPM). This module's iOS test executable links no Crashlytics, so the symbol is left to resolve at run time.
kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
    binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable>().configureEach {
        linkerOpts("-U", "_FIRCLSExceptionRecordNSException")
    }
}
