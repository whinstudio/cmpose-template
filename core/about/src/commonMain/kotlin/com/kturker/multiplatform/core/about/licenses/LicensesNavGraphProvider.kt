package com.kturker.multiplatform.core.about.licenses

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kturker.multiplatform.core.navigation.NavGraphProvider
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

// A dotted name: a custom feature module's provider is named "<Module>Provider" and must not replace this one
@Single(binds = [NavGraphProvider::class])
@Named("core.about.licenses")
internal class LicensesNavGraphProvider : NavGraphProvider {
    override fun registerGraph(provider: NavGraphBuilder) {
        provider.composable<LicensesDestination> { entry ->
            val destination = entry.toRoute<LicensesDestination>()
            LicensesRoute(title = destination.title, backLabel = destination.backLabel)
        }
    }
}
