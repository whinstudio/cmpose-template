package com.kturker.multiplatform.core.network

/**
 * Host the debug build reaches the Mac's local server on: localhost everywhere. The Android emulator and a
 * physical Android phone reach it through `adb reverse tcp:18001 tcp:18001`; the iOS simulator shares the Mac's
 * loopback directly.
 */
expect fun localServerHost(): String
