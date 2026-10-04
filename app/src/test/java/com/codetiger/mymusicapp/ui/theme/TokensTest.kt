package com.codetiger.mymusicapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** design-system/tokens.json is the source of truth; the Kotlin theme must match it. */
class TokensTest {
    // Unit tests run with the module (app/) as the working directory.
    private val tokens = JSONObject(File("../design-system/tokens.json").readText())

    private fun group(name: String): Map<String, String> {
        val list = tokens.getJSONObject(name).getJSONArray("tokens")
        return (0 until list.length()).associate { list.getJSONObject(it).let { t -> t.getString("name") to t.getString("value") } }
    }

    private fun px(value: String) = value.removeSuffix("px").toFloat()

    private fun hex(color: Color): String {
        val argb = (color.value shr 32).toLong()
        return "#%06x".format(argb and 0xFFFFFF)
    }

    @Test
    fun colours() {
        val c = group("color")
        assertEquals(c["surface"], hex(MusicColors.Surface))
        assertEquals(c["fill"], hex(MusicColors.Fill))
        assertEquals(c["ink"], hex(MusicColors.Ink))
        assertEquals(c["accent"], hex(MusicColors.Accent))
    }

    @Test
    fun spacing() {
        val s = group("spacing")
        listOf(Space.S1, Space.S2, Space.S3, Space.S4, Space.S5, Space.S6, Space.S7).forEachIndexed { i, dp ->
            assertEquals("space-${i + 1}", px(s.getValue("space-${i + 1}")), dp.value)
        }
    }

    @Test
    fun radius() {
        val r = group("radius")
        fun corner(shape: RoundedCornerShape) = shape.topStart.toPx(GeoSize(100f, 100f), Density(1f))
        assertEquals(px(r.getValue("radius-sm")), corner(Radius.Sm))
        assertEquals(px(r.getValue("radius-md")), corner(Radius.Md))
        assertEquals(px(r.getValue("radius-lg")), corner(Radius.Lg))
    }

    @Test
    fun sizes() {
        val expected: Map<String, Dp> = mapOf(
            "size-target" to Size.Target, "size-play" to Size.Play, "size-transport" to Size.Transport,
            "size-avatar" to Size.Avatar, "size-avatar-lg" to Size.AvatarLg, "size-art-row" to Size.ArtRow, "size-art-card" to Size.ArtCard,
            "size-art-hero" to Size.ArtHero, "size-icon-sm" to Size.IconSm, "size-icon" to Size.Icon,
            "size-icon-md" to Size.IconMd, "size-icon-lg" to Size.IconLg, "size-icon-xl" to Size.IconXl,
            "size-row" to Size.Row, "size-row-tall" to Size.RowTall, "size-handle" to Size.Handle,
            "size-track" to Size.Track, "size-progress" to Size.Progress, "size-tile" to Size.Tile,
            "focus-width" to Size.FocusWidth,
        )
        val json = group("size")
        assertEquals("every size token has a Kotlin value", json.keys, expected.keys)
        expected.forEach { (name, dp) -> assertEquals(name, px(json.getValue(name)), dp.value) }
    }

    @Test
    fun typeScale() {
        val expected: Map<String, TextStyle> = mapOf(
            "song-hero" to MusicType.SongHero, "title" to MusicType.Title, "input" to MusicType.Input,
            "heading" to MusicType.Heading, "body" to MusicType.Body, "body-strong" to MusicType.BodyStrong,
            "time" to MusicType.Time, "button" to MusicType.Button, "button-hero" to MusicType.ButtonHero,
            "control-label" to MusicType.ControlLabel,
        )
        val groups = tokens.getJSONObject("type").getJSONArray("groups")
        val seen = mutableSetOf<String>()
        for (g in 0 until groups.length()) {
            val styles = groups.getJSONObject(g).getJSONArray("styles")
            for (i in 0 until styles.length()) {
                val s = styles.getJSONObject(i)
                val name = s.getString("name")
                val style = expected[name] ?: error("No MusicType for $name")
                assertEquals("$name size", px(s.getString("fontSize")), style.fontSize.value)
                assertEquals("$name line height", px(s.getString("lineHeight")), style.lineHeight.value)
                assertEquals("$name weight", s.getInt("fontWeight"), style.fontWeight?.weight)
                seen += name
            }
        }
        assertEquals(expected.keys, seen)
    }

    @Test
    fun textScale() {
        val t = group("textScale")
        assertEquals(t.getValue("text-normal").toFloat(), TextSize.Normal.factor)
        assertEquals(t.getValue("text-large").toFloat(), TextSize.Large.factor)
        assertEquals(t.getValue("text-extra-large").toFloat(), TextSize.ExtraLarge.factor)
        assertEquals(t.getValue("text-cap").toFloat(), TEXT_SCALE_CAP)
    }
}
