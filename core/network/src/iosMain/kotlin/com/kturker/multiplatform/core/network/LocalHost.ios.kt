package com.kturker.multiplatform.core.network

// The iOS simulator shares the Mac's network, loopback included
actual fun localServerHost(): String = "localhost"

actual val localServerUsesAdbReverse: Boolean = false
