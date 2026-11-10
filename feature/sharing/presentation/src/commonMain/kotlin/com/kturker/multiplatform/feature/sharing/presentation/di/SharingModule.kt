package com.kturker.multiplatform.feature.sharing.presentation.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.kturker.multiplatform.core.multilang.language.LanguageManager
import com.kturker.multiplatform.core.multilang.resource.shareInviteText
import com.kturker.multiplatform.core.multilang.resource.sharing
import com.kturker.multiplatform.core.network.BACKEND_CLIENT
import com.kturker.multiplatform.feature.sharing.contract.SelectedSpace
import com.kturker.multiplatform.feature.sharing.presentation.data.DataStorePendingInviteTokenStore
import com.kturker.multiplatform.feature.sharing.presentation.data.KtorSpacesRepository
import com.kturker.multiplatform.feature.sharing.presentation.data.PendingInviteTokenStore
import com.kturker.multiplatform.feature.sharing.presentation.data.PersistedSelectedSpace
import com.kturker.multiplatform.feature.sharing.presentation.data.SpacesRepository
import com.kturker.multiplatform.feature.sharing.presentation.share.InviteText
import com.kturker.multiplatform.feature.sharing.presentation.share.ShareSheet
import com.kturker.multiplatform.feature.sharing.presentation.share.TextSharer
import com.kturker.multiplatform.feature.sharing.presentation.share.appDisplayName
import io.ktor.client.HttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.kturker.multiplatform.feature.sharing.presentation")
class SharingModule {

    @Single
    fun provideSpacesRepository(
        @Provided @Named(BACKEND_CLIENT) client: HttpClient
    ): SpacesRepository = KtorSpacesRepository(client)

    // One for the app: every screen shows the same space
    @Single
    fun provideSelectedSpace(
        repository: SpacesRepository,
        @Provided dataStore: DataStore<Preferences>
    ): SelectedSpace = PersistedSelectedSpace(repository, dataStore)

    @Single
    fun providePendingInviteTokenStore(
        @Provided dataStore: DataStore<Preferences>
    ): PendingInviteTokenStore = DataStorePendingInviteTokenStore(dataStore)

    @Single
    fun provideTextSharer(): TextSharer = ShareSheet()

    // The app's name and the language are read when the link is shared: both follow the device and the app
    @Single
    fun provideInviteText(@Provided languageManager: LanguageManager): InviteText =
        InviteText { url ->
            languageManager.currentResources.value.sharing.shareInviteText(appName = appDisplayName(), url = url)
        }
}
