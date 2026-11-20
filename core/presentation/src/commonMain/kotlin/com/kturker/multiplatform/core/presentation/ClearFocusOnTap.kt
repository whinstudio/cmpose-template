package com.kturker.multiplatform.core.presentation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Clears focus, and so closes the keyboard, on a tap that nothing below this node handles: a blank area or plain
 * text such as a screen title. Buttons, text fields and scrolling lists handle their own taps and drags, so those
 * keep working. The iOS number pad has no key that closes it, so without this the keyboard stays open until the
 * screen changes. The app's root Scaffold (MainScreen) already sets it.
 */
fun Modifier.clearFocusOnTap(): Modifier = this then ClearFocusOnTapElement

private data object ClearFocusOnTapElement : ModifierNodeElement<ClearFocusOnTapNode>() {
    override fun create(): ClearFocusOnTapNode = ClearFocusOnTapNode()

    override fun update(node: ClearFocusOnTapNode) = Unit
}

private class ClearFocusOnTapNode :
    DelegatingNode(),
    CompositionLocalConsumerModifierNode {
    init {
        delegate(
            SuspendingPointerInputModifierNode {
                detectTapGestures(onTap = { currentValueOf(LocalFocusManager).clearFocus() })
            }
        )
    }
}
