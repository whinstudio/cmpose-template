package com.kturker.multiplatform.core.network

/**
 * Host the debug build reaches the Mac's local server on: localhost everywhere. The iOS simulator shares the Mac's
 * loopback directly; Android needs a tunnel ([localServerUsesAdbReverse]).
 */
expect fun localServerHost(): String

/**
 * Whether the device reaches the Mac's localhost only through `adb reverse tcp:18001 tcp:18001`: the Android
 * emulator and a physical Android phone do, and the tunnel is lost whenever the emulator, the USB link or adb
 * restarts. The iOS simulator needs none.
 */
expect val localServerUsesAdbReverse: Boolean
