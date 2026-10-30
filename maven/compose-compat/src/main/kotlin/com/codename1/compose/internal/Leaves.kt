/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.compose.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.codename1.ui.CN
import com.codename1.ui.Font
import com.codename1.ui.Graphics

internal object Theme {
    fun isDark(): Boolean = try {
        CN.isDarkMode() == true
    } catch (t: Throwable) {
        false
    }
}

/// Native fonts by weight and pixel size, cached.
internal object Fonts {
    private val cache = HashMap<Long, Font>()

    fun get(sizePx: Int, weight: FontWeight?): Font {
        val w = weight?.weight ?: 400
        val key = (sizePx.toLong() shl 16) or w.toLong()
        cache[key]?.let { return it }
        val f = create(sizePx, w)
        cache[key] = f
        return f
    }

    private fun create(sizePx: Int, w: Int): Font {
        try {
            if (Font.isNativeFontSchemeSupported()) {
                val name = when {
                    w <= 200 -> "native:MainThin"
                    w <= 300 -> "native:MainLight"
                    w <= 500 -> "native:MainRegular"
                    w <= 700 -> "native:MainBold"
                    else -> "native:MainBlack"
                }
                val base = Font.createTrueTypeFont(name, name)
                if (base != null) {
                    return base.derive(sizePx.toFloat(), if (w >= 600) Font.STYLE_BOLD else Font.STYLE_PLAIN)
                }
            }
        } catch (t: Throwable) {
            // Fall through to a system font.
        }
        val size = when {
            sizePx < Density.px(androidx.compose.ui.unit.Dp(13f)) -> Font.SIZE_SMALL
            sizePx > Density.px(androidx.compose.ui.unit.Dp(18f)) -> Font.SIZE_LARGE
            else -> Font.SIZE_MEDIUM
        }
        return Font.createSystemFont(Font.FACE_SYSTEM, if (w >= 600) Font.STYLE_BOLD else Font.STYLE_PLAIN, size)
    }
}

/// Text: wrapped at word boundaries to the width it is given, cut at
/// `maxLines` with an ellipsis when asked.
internal class TextNode : UiNode() {
    var text: String = ""
        set(v) { if (field != v) { field = v; invalidateMeasure(); repaint() } }
    var style: TextStyle = TextStyle.Default
        set(v) { if (field != v) { field = v; invalidateMeasure(); repaint() } }
    var maxLines: Int = Int.MAX_VALUE
        set(v) { if (field != v) { field = v; invalidateMeasure() } }
    var softWrap: Boolean = true
        set(v) { if (field != v) { field = v; invalidateMeasure() } }
    var overflow: TextOverflow = TextOverflow.Clip
        set(v) { if (field != v) { field = v; invalidateMeasure() } }

    private var lines: List<String> = emptyList()
    private var font: Font? = null

    init {
        policy = LeafPolicy
    }

    private fun font(): Font {
        val sp = if (style.fontSize.isSpecified) style.fontSize.value else 14f
        val f = Fonts.get(Math.max(1, Math.round(sp * Density.density)), style.fontWeight)
        font = f
        return f
    }

    private fun lineHeight(f: Font): Int =
        if (style.lineHeight.isSpecified && style.lineHeight.isSp) Math.max(f.height, Math.round(style.lineHeight.value * Density.density))
        else f.height

    override fun measureContent(c: Constraints): Long {
        val f = font()
        val maxW = c.maxWidth
        val out = ArrayList<String>()
        for (para in text.split('\n')) {
            if (!softWrap || maxW == Constraints.INF || f.stringWidth(para) <= maxW) {
                out.add(para)
            } else {
                wrap(para, f, maxW, out)
            }
            if (out.size > maxLines) break
        }
        if (out.size > maxLines) {
            while (out.size > maxLines) out.removeAt(out.size - 1)
            if (overflow == TextOverflow.Ellipsis && out.isNotEmpty()) {
                out[out.size - 1] = ellipsize(out[out.size - 1] + "\u2026", f, maxW)
            }
        } else if (overflow == TextOverflow.Ellipsis && maxW != Constraints.INF && out.size == 1 && f.stringWidth(out[0]) > maxW) {
            out[0] = ellipsize(out[0], f, maxW)
        }
        lines = out
        var w = 0
        for (l in out) w = Math.max(w, f.stringWidth(l))
        return size(Math.min(w, maxW), lineHeight(f) * out.size)
    }

    private fun wrap(para: String, f: Font, maxW: Int, out: MutableList<String>) {
        var line = StringBuilder()
        for (word in para.split(' ')) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (f.stringWidth(candidate) <= maxW || line.isEmpty()) {
                line = StringBuilder(candidate)
            } else {
                out.add(line.toString())
                line = StringBuilder(word)
            }
        }
        out.add(line.toString())
    }

    private fun ellipsize(s: String, f: Font, maxW: Int): String {
        val e = "\u2026"
        var base = if (s.endsWith(e)) s.substring(0, s.length - 1) else s
        if (maxW == Constraints.INF || f.stringWidth(base + e) <= maxW && s.endsWith(e)) return base + e
        while (base.isNotEmpty() && f.stringWidth(base + e) > maxW) base = base.substring(0, base.length - 1)
        return base.trimEnd() + e
    }

    override fun paintContent(g: Graphics) {
        val f = font ?: font()
        val color = if (style.color == Color.Unspecified) Color.Black else style.color
        if (color.alpha <= 0f) return
        val old = g.alpha
        g.font = f
        g.color = color.argb and 0xffffff
        if (color.alpha < 1f) g.concatenateAlpha(Math.round(color.alpha * 255))
        g.isAntiAliased = true
        g.isAntiAliasedText = true
        val lh = lineHeight(f)
        var y = absY() + contentY + (lh - f.height) / 2
        val x0 = absX() + contentX
        for (l in lines) {
            val lw = f.stringWidth(l)
            val dx = when (style.textAlign) {
                TextAlign.Center -> (contentW - lw) / 2
                TextAlign.End, TextAlign.Right -> contentW - lw
                else -> 0
            }
            g.drawString(l, x0 + dx, y)
            if (style.textDecoration == TextDecoration.Underline) {
                g.drawLine(x0 + dx, y + f.ascent + 1, x0 + dx + lw, y + f.ascent + 1)
            } else if (style.textDecoration == TextDecoration.LineThrough) {
                g.drawLine(x0 + dx, y + f.height / 2, x0 + dx + lw, y + f.height / 2)
            }
            y += lh
        }
        g.alpha = old
    }

    fun cn1Lines(): List<String> = lines
}

internal fun UiNode.absX(): Int = x
internal fun UiNode.absY(): Int = y

/// A bitmap, scaled into its content box.
internal class ImageNode : UiNode() {
    var bitmap: ImageBitmap? = null
        set(v) { if (field !== v) { field = v; invalidateMeasure(); repaint() } }
    var contentScale: ContentScale = ContentScale.Fit
    var tint: Color = Color.Unspecified

    init {
        policy = LeafPolicy
    }

    override fun measureContent(c: Constraints): Long {
        val b = bitmap ?: return size(c.minWidth, c.minHeight)
        val iw = b.width
        val ih = b.height
        if (c.hasFixedWidth && c.hasFixedHeight) return size(c.maxWidth, c.maxHeight)
        var w = c.constrainWidth(iw)
        var h = c.constrainHeight(ih)
        if (iw > 0 && ih > 0) {
            val sw = w.toFloat() / iw
            val sh = h.toFloat() / ih
            val s = Math.min(sw, sh)
            if (!c.hasFixedWidth) w = c.constrainWidth(Math.round(iw * s))
            if (!c.hasFixedHeight) h = c.constrainHeight(Math.round(ih * s))
        }
        return size(w, h)
    }

    override fun paintContent(g: Graphics) {
        val b = bitmap ?: return
        val bw = contentW
        val bh = contentH
        val iw = b.width.toFloat()
        val ih = b.height.toFloat()
        if (iw <= 0 || ih <= 0) return
        val s = when (contentScale) {
            ContentScale.Crop -> Math.max(bw / iw, bh / ih)
            ContentScale.FillBounds -> -1f
            ContentScale.None -> 1f
            ContentScale.FillWidth -> bw / iw
            else -> Math.min(bw / iw, bh / ih)
        }
        val x0 = x + contentX
        val y0 = y + contentY
        if (s < 0) {
            g.drawImage(b.image, x0, y0, bw, bh)
            return
        }
        val dw = Math.round(iw * s)
        val dh = Math.round(ih * s)
        g.pushClip()
        g.clipRect(x0, y0, bw, bh)
        g.drawImage(b.image, x0 + (bw - dw) / 2, y0 + (bh - dh) / 2, dw, dh)
        g.popClip()
    }
}

/// A material icon glyph from the material design font.
internal class IconNode : UiNode() {
    var glyph: Char = ' '
        set(v) { if (field != v) { field = v; repaint() } }
    var tint: Color = Color.Black
        set(v) { if (field != v) { field = v; repaint() } }

    init {
        policy = LeafPolicy
    }

    override fun measureContent(c: Constraints): Long {
        val d = Density.px(androidx.compose.ui.unit.Dp(24f))
        return size(c.constrainWidth(d), c.constrainHeight(d))
    }

    override fun paintContent(g: Graphics) {
        val s = Math.min(contentW, contentH)
        if (s <= 0 || tint.alpha <= 0f) return
        val f = com.codename1.ui.FontImage.getMaterialDesignFont().derive(s.toFloat(), Font.STYLE_PLAIN)
        val old = g.alpha
        g.font = f
        g.color = tint.argb and 0xffffff
        if (tint.alpha < 1f) g.concatenateAlpha(Math.round(tint.alpha * 255))
        g.isAntiAliasedText = true
        val str = glyph.toString()
        val gw = f.stringWidth(str)
        g.drawString(str, x + contentX + (contentW - gw) / 2, y + contentY + (contentH - f.height) / 2)
        g.alpha = old
    }
}

/// Hosts a Codename One component as a node's only child, sized to the node's
/// content box: how text input gets the platform's own editing.
internal class NativeNode<T : com.codename1.ui.Component>(val component: T) : UiNode() {
    init {
        policy = BoxPolicy(androidx.compose.ui.Alignment.TopStart, true)
        addComponent(component)
    }
}

/// A single line or multi line text input over Codename One's `TextArea`.
internal class TextInputNode : UiNode() {
    val field: com.codename1.ui.TextArea
    private var syncing = false
    var onValueChange: (String) -> Unit = {}
    var textColor: Color = Color.Black
        set(v) { field = v; applyColors() }
    var font: Font? = null
        set(v) { field = v; if (v != null) this.field.allStyles.font = v; invalidateMeasure() }

    init {
        policy = BoxPolicy(androidx.compose.ui.Alignment.TopStart, true)
        val input = com.codename1.ui.TextField()
        field = input
        cn1StripStyle(input, allStates = true)
        input.addDataChangedListener { _, _ ->
            if (!syncing) onValueChange(input.text)
        }
        addComponent(field)
    }

    fun setValue(v: String) {
        if (field.text != v) {
            syncing = true
            try {
                field.text = v
            } finally {
                syncing = false
            }
        }
    }

    var singleLine: Boolean = true
        set(v) { field = v; this.field.isSingleLineTextArea = v; invalidateMeasure() }

    var hint: String = ""
        set(v) { field = v; this.field.hint = v }

    var enabledInput: Boolean = true
        set(v) { field = v; this.field.isEnabled = v }

    private fun applyColors() {
        field.allStyles.fgColor = textColor.argb and 0xffffff
        field.hintLabel?.allStyles?.fgColor = textColor.argb and 0xffffff
        field.hintLabel?.allStyles?.opacity = 140
    }
}
