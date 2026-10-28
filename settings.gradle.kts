rootProject.name = "Multiplatform"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Lets Gradle download the JDK that gradle/gradle-daemon-jvm.properties asks for when it is missing
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                // Cronet, a dependency of the Next-Gen Google Mobile Ads SDK, is published on Google Maven only
                includeGroupAndSubgroups("org.chromium.net")
            }
        }
        mavenCentral()
    }
}

include(":shared")
include(":androidApp")
include(":core:about")
// cmpose:backend
include(":core:api")
include(":core:auth")
// /cmpose:backend
include(":core:ads")
include(":core:insights")
include(":core:database")
include(":core:designsystem")
include(":core:domain")
include(":core:image")
include(":core:logging")
include(":core:multilang")
include(":core:navigation")
include(":core:network")
include(":core:presentation")
include(":core:purchases")
include(":core:review")
include(":core:update")
include(":core:room")
include(":feature:monetization")
include(":feature:monetization:contract")
include(":feature:monetization:presentation")
// cmpose:backend
include(":feature:account")
include(":feature:account:contract")
include(":feature:account:presentation")
include(":feature:sharing")
include(":feature:sharing:contract")
include(":feature:sharing:presentation")
// /cmpose:backend
include(":feature:home")
include(":feature:home:contract")
include(":feature:home:data")
include(":feature:home:domain")
include(":feature:home:presentation")
include(":feature:onboarding")
include(":feature:onboarding:contract")
include(":feature:onboarding:data")
include("feature:onboarding:domain")
include("feature:onboarding:presentation")
include(":feature:notes")
include(":feature:notes:contract")
include(":feature:notes:presentation")
include(":feature:sample")
include(":feature:sample:contract")
include(":feature:sample:data")
include(":feature:sample:domain")
include(":feature:sample:presentation")
// cmpose:backend
// server/ is generated from the cmpose backend template (an included build); the template repository has none
if (file("server/settings.gradle.kts").exists()) includeBuild("server")
// /cmpose:backend
includeBuild("build-logic")
