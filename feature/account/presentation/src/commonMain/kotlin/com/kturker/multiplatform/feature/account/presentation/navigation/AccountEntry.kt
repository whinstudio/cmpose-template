package com.kturker.multiplatform.feature.account.presentation.navigation

import com.kturker.multiplatform.core.multilang.language.LanguageManager
import com.kturker.multiplatform.core.multilang.resource.account
import com.kturker.multiplatform.core.navigation.FeatureEntry
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.feature.account.contract.AccountSettingsDestination
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

// Lists the account's settings on a blank app's start screen (GuideScreen). That screen (AccountSettingsDestination)
// holds the account deletion and the privacy policy and terms links, which the stores ask for: a start screen of
// your own keeps a way to it.
@Single(binds = [FeatureEntry::class])
@Named("AccountEntry")
internal class AccountEntry(@Provided private val languageManager: LanguageManager) : FeatureEntry {
    // Read on every use: the title follows a language change in the app
    override val title: String get() = languageManager.currentResources.value.account.accountTitle
    override val destination: NavigationCommand.Destination = AccountSettingsDestination
}
