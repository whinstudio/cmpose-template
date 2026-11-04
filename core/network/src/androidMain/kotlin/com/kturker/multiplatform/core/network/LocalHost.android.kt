package com.kturker.multiplatform.core.network

// In apps targeting API 37, Android 17 requires the runtime ACCESS_LOCAL_NETWORK permission for the emulator's
// 10.0.2.2 alias, which the template does not request; loopback is not local network.
actual fun localServerHost(): String = "localhost"

// The emulator and a physical phone both reach the Mac's server through adb reverse, which tunnels the device's
// localhost to the Mac
actual val localServerUsesAdbReverse: Boolean = true
