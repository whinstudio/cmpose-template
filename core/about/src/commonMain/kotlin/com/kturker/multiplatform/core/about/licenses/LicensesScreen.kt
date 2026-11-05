package com.kturker.multiplatform.core.about.licenses

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject

private val log = Logger.withTag("About")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LicensesRoute(
    title: String,
    backLabel: String,
    source: LicensesSource = koinInject(),
    navigationManager: NavigationManager = koinInject()
) {
    val libraries by produceState<Libs?>(initialValue = null, source) {
        value = try {
            Libs.Builder().withJson(source.read()).build()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w { "Licenses unavailable: ${e::class.simpleName}" }
            null
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = { navigationManager.navigate(NavigationCommand.NavigateUp) }) {
                        Text(backLabel)
                    }
                }
            )
        }
    ) { padding ->
        LibrariesContainer(libraries = libraries, modifier = Modifier.fillMaxSize().padding(padding))
    }
}
