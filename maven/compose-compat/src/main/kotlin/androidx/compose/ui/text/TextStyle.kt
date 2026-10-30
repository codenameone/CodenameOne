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
package androidx.compose.ui.text

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

/// How text is drawn. Unspecified values fall back to the style it is merged
/// into, as in Compose.
@Immutable
data class TextStyle(
    val color: Color = Color.Unspecified,
    val fontSize: TextUnit = TextUnit.Unspecified,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontFamily: FontFamily? = null,
    val letterSpacing: TextUnit = TextUnit.Unspecified,
    val textDecoration: TextDecoration? = null,
    val textAlign: TextAlign = TextAlign.Unspecified,
    val lineHeight: TextUnit = TextUnit.Unspecified
) {
    fun merge(other: TextStyle?): TextStyle {
        if (other == null) return this
        return TextStyle(
            color = if (other.color != Color.Unspecified) other.color else color,
            fontSize = if (other.fontSize.isSpecified) other.fontSize else fontSize,
            fontWeight = other.fontWeight ?: fontWeight,
            fontStyle = other.fontStyle ?: fontStyle,
            fontFamily = other.fontFamily ?: fontFamily,
            letterSpacing = if (other.letterSpacing.isSpecified) other.letterSpacing else letterSpacing,
            textDecoration = other.textDecoration ?: textDecoration,
            textAlign = if (other.textAlign != TextAlign.Unspecified) other.textAlign else textAlign,
            lineHeight = if (other.lineHeight.isSpecified) other.lineHeight else lineHeight
        )
    }

    operator fun plus(other: TextStyle): TextStyle = merge(other)

    companion object {
        val Default: TextStyle = TextStyle()
    }
}
