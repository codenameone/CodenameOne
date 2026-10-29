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
package androidx.compose.ui.graphics.vector

import androidx.compose.runtime.Immutable

/// A vector icon. The icons shipped here are glyphs of the material design font
/// every Codename One application already carries, drawn at any size.
@Immutable
class ImageVector internal constructor(val name: String, internal val glyph: Char) {
    override fun equals(other: Any?): Boolean = other is ImageVector && other.glyph == glyph

    override fun hashCode(): Int = glyph.code

    override fun toString(): String = "ImageVector($name)"

    companion object {
        /// An icon from any glyph of the material design font.
        fun cn1Material(name: String, glyph: Char): ImageVector = ImageVector(name, glyph)
    }
}
