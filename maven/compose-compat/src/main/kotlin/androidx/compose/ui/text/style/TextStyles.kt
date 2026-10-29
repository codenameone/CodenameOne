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
package androidx.compose.ui.text.style

@JvmInline
value class TextAlign(val value: Int) {
    companion object {
        val Left: TextAlign = TextAlign(1)
        val Right: TextAlign = TextAlign(2)
        val Center: TextAlign = TextAlign(3)
        val Justify: TextAlign = TextAlign(4)
        val Start: TextAlign = TextAlign(5)
        val End: TextAlign = TextAlign(6)
        val Unspecified: TextAlign = TextAlign(0)
    }
}

@JvmInline
value class TextOverflow(val value: Int) {
    companion object {
        val Clip: TextOverflow = TextOverflow(1)
        val Ellipsis: TextOverflow = TextOverflow(2)
        val Visible: TextOverflow = TextOverflow(3)
    }
}

@JvmInline
value class TextDecoration(val mask: Int) {
    companion object {
        val None: TextDecoration = TextDecoration(0)
        val Underline: TextDecoration = TextDecoration(1)
        val LineThrough: TextDecoration = TextDecoration(2)
    }
}
