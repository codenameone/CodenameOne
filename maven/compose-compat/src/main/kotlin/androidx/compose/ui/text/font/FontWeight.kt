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
package androidx.compose.ui.text.font

import androidx.compose.runtime.Immutable

@Immutable
data class FontWeight(val weight: Int) : Comparable<FontWeight> {
    override fun compareTo(other: FontWeight): Int = weight.compareTo(other.weight)

    companion object {
        val Thin: FontWeight = FontWeight(100)
        val ExtraLight: FontWeight = FontWeight(200)
        val Light: FontWeight = FontWeight(300)
        val Normal: FontWeight = FontWeight(400)
        val Medium: FontWeight = FontWeight(500)
        val SemiBold: FontWeight = FontWeight(600)
        val Bold: FontWeight = FontWeight(700)
        val ExtraBold: FontWeight = FontWeight(800)
        val Black: FontWeight = FontWeight(900)
    }
}

@JvmInline
value class FontStyle(val value: Int) {
    companion object {
        val Normal: FontStyle = FontStyle(0)
        val Italic: FontStyle = FontStyle(1)
    }
}

/// A font family. Only the platform's own families exist here: text is drawn
/// with the device's native font.
@Immutable
class FontFamily private constructor(internal val name: String) {
    override fun toString(): String = "FontFamily.$name"

    companion object {
        val Default: FontFamily = FontFamily("Default")
        val SansSerif: FontFamily = FontFamily("SansSerif")
        val Serif: FontFamily = FontFamily("Serif")
        val Monospace: FontFamily = FontFamily("Monospace")
    }
}
