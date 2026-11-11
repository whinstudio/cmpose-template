package com.kturker.multiplatform

import com.kturker.multiplatform.core.navigation.NavigationCommand
import kotlinx.serialization.Serializable

// The app's start destination (MainScreen.kt): the guide (GuideScreen.kt) is registered under it until your own home
// screen takes its place. Its own file, so deleting GuideScreen.kt leaves the start in place.
// cmpose:backend
// A Destination: the sign-in gate (SessionGate.kt) navigates back to it after a sign-in
// /cmpose:backend
@Serializable
internal data object GuideDestination : NavigationCommand.Destination
