plugins {
    alias(libs.plugins.feature.presentation.plugin)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.database)
            implementation(projects.core.about)
            // InsightsConsentSettings: the privacy switches (usage data, crash reports)
            implementation(projects.core.insights)
            implementation(projects.feature.home.contract)
            implementation(projects.feature.home.domain)
            implementation(projects.feature.monetization.contract)
            implementation(projects.feature.onboarding.contract)
            implementation(projects.feature.onboarding.domain)
            // cmpose:backend
            // The Account row: the account settings (sign-out, account deletion, the privacy links)
            implementation(projects.feature.account.contract)
            // /cmpose:backend
        }
    }
}