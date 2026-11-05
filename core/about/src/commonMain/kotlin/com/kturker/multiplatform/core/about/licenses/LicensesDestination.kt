package com.kturker.multiplatform.core.about.licenses

import com.kturker.multiplatform.core.navigation.NavigationCommand
import kotlinx.serialization.Serializable

// The title and the back label come with the route: core/about has no string resources of its own. backLabel
// defaults to English so existing LicensesDestination(title = …) calls keep compiling
@Serializable
data class LicensesDestination(val title: String, val backLabel: String = "Back") : NavigationCommand.Destination
