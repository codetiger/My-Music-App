package com.codetiger.mymusicapp.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.Size
import androidx.compose.ui.geometry.Size as GeoSize

/**
 * The design-system focus state: a `focus-width` `focus-ring` outline, `focus-width` clear of
 * [shape], while the control (or the text field inside it) has keyboard or Switch Access focus.
 * Put it before `clip` and `clickable` so the ring is drawn outside the shape.
 */
fun Modifier.focusRing(shape: Shape): Modifier = this then FocusRingElement(shape)

private data class FocusRingElement(val shape: Shape) : ModifierNodeElement<FocusRingNode>() {
    override fun create() = FocusRingNode(shape)

    override fun update(node: FocusRingNode) {
        node.shape = shape
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "focusRing"
        properties["shape"] = shape
    }
}

private class FocusRingNode(var shape: Shape) : Modifier.Node(), FocusEventModifierNode, DrawModifierNode {
    private var focused = false

    override fun onFocusEvent(focusState: FocusState) {
        if (focused != focusState.hasFocus) {
            focused = focusState.hasFocus
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!focused) return
        val width = Size.FocusWidth.toPx()
        // The stroke is centred on its path: a clear gap of one width, then the ring.
        val inset = width * 1.5f
        val ring = GeoSize(size.width + 2 * inset, size.height + 2 * inset)
        translate(-inset, -inset) {
            drawOutline(shape.createOutline(ring, layoutDirection, this), MusicColors.FocusRing, style = Stroke(width))
        }
    }
}
