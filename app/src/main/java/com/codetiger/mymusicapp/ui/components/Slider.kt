package com.codetiger.mymusicapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.Size

/**
 * The seek bar and volume bar: an 8dp `fill` track, the played part and a 32dp disc in
 * `accent`. The whole 64dp row is touchable. [onChange] fires while dragging, [onDone] at the end.
 */
@Composable
fun MusicSlider(
    value: Float,
    onChange: (Float) -> Unit,
    label: String,
    valueText: String,
    modifier: Modifier = Modifier,
    onDone: (Float) -> Unit = onChange,
    steps: Int = 20,
) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = (dragging ?: value).coerceIn(0f, 1f)
    val change by rememberUpdatedState(onChange)
    val done by rememberUpdatedState(onDone)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(Size.Target)
            .semantics {
                contentDescription = label
                stateDescription = valueText
                progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f, steps)
                setProgress { target -> done(target.coerceIn(0f, 1f)); true }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset -> done((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragging = (offset.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragging?.let(done); dragging = null },
                    onDragCancel = { dragging = null },
                ) { change, _ ->
                    val v = (change.position.x / size.width).coerceIn(0f, 1f)
                    dragging = v
                    change(v)
                }
            },
    ) {
        val handle = Size.Handle.toPx()
        val track = Size.Track.toPx()
        val left = handle / 2
        val width = size.width - handle
        val y = size.height / 2
        val radius = CornerRadius(track / 2)
        drawRoundRect(MusicColors.Fill, Offset(left, y - track / 2), GeoSize(width, track), radius)
        drawRoundRect(MusicColors.Accent, Offset(left, y - track / 2), GeoSize(width * shown, track), radius)
        drawCircle(MusicColors.Accent, handle / 2, Offset(left + width * shown, y))
    }
}
