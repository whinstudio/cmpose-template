package com.kturker.multiplatform.core.update

// How a required update starts on Android: Play's immediate (full-screen) update, or the Play Store page
internal enum class RequiredUpdatePath {
    Immediate,
    Store
}

// Play's in-app flow when it offers an update for this install and allows the immediate type, or when an
// immediate update was left halfway (Play asks to resume it whatever the allowed types say); anything else (no
// update on Play yet, type not allowed) opens the store page. A failed Play check also means Store.
internal fun requiredUpdatePath(
    updateAvailable: Boolean,
    updateInProgress: Boolean,
    immediateAllowed: Boolean
): RequiredUpdatePath = if (updateInProgress || (updateAvailable && immediateAllowed)) {
    RequiredUpdatePath.Immediate
} else {
    RequiredUpdatePath.Store
}
