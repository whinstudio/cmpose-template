package com.kturker.multiplatform.feature.account.presentation.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.kturker.multiplatform.core.auth.AuthService
import com.kturker.multiplatform.core.auth.SessionTokens
import com.kturker.multiplatform.core.multilang.language.LanguageManager
import com.kturker.multiplatform.core.multilang.resource.account
import com.kturker.multiplatform.core.network.BACKEND_CLIENT
import com.kturker.multiplatform.feature.account.presentation.data.AccountRepository
import com.kturker.multiplatform.feature.account.presentation.data.BackendProfileSync
import com.kturker.multiplatform.feature.account.presentation.data.DataStorePendingDisplayName
import com.kturker.multiplatform.feature.account.presentation.data.KtorAccountRepository
import com.kturker.multiplatform.feature.account.presentation.data.PendingDisplayName
import com.kturker.multiplatform.feature.account.presentation.data.PersonalSpaceName
import com.kturker.multiplatform.feature.account.presentation.data.ProfileSync
import com.kturker.multiplatform.feature.sharing.contract.SelectedSpace
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.kturker.multiplatform.feature.account.presentation")
class AccountModule {

    @Single
    internal fun provideAccountRepository(
        @Provided @Named(BACKEND_CLIENT) client: HttpClient
    ): AccountRepository = KtorAccountRepository(client)

    @Single
    internal fun providePersonalSpaceName(
        @Provided languageManager: LanguageManager
    ): PersonalSpaceName =
        PersonalSpaceName { languageManager.currentResources.value.account.personalSpace }

    @Single
    internal fun providePendingDisplayName(
        @Provided dataStore: DataStore<Preferences>
    ): PendingDisplayName = DataStorePendingDisplayName(dataStore)

    // Its own scope: the sync outlives the sign-in screen, which closes as soon as the user is signed in
    @Single
    internal fun provideProfileSync(
        repository: AccountRepository,
        @Provided auth: AuthService,
        personalSpaceName: PersonalSpaceName,
        @Provided tokens: SessionTokens,
        @Provided selectedSpace: SelectedSpace,
        pendingName: PendingDisplayName
    ): ProfileSync = BackendProfileSync(
        repository = repository,
        auth = auth,
        personalSpaceName = personalSpaceName,
        tokens = tokens,
        selectedSpace = selectedSpace,
        pendingName = pendingName,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )
}
