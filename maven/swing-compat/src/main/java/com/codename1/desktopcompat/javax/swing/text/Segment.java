/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.desktopcompat.javax.swing.text;

/// A run of characters inside an array: `count` characters of `array`
/// starting at `offset`. A document fills one in for
/// `getText(int, int, Segment)`; the array then belongs to the caller.
///
/// The character iterator of the JDK class is not part of this layer.
public class Segment implements CharSequence {

    /// The array the characters are in.
    public char[] array;

    /// Where in the array the run starts.
    public int offset;

    /// How many characters the run has.
    public int count;

    private boolean partialReturn;

    public Segment() {
        this(null, 0, 0);
    }

    public Segment(char[] array, int offset, int count) {
        this.array = array;
        this.offset = offset;
        this.count = count;
    }

    /// Asks the document for whatever part of a request it can give
    /// without copying. The documents here always fill the whole request.
    public void setPartialReturn(boolean p) {
        partialReturn = p;
    }

    public boolean isPartialReturn() {
        return partialReturn;
    }

    @Override
    public String toString() {
        if (array != null) {
            return new String(array, offset, count);
        }
        return "";
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return array[offset + index];
    }

    @Override
    public int length() {
        return count;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (start < 0) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (end > count) {
            throw new StringIndexOutOfBoundsException(end);
        }
        if (start > end) {
            throw new StringIndexOutOfBoundsException(end - start);
        }
        Segment segment = new Segment();
        segment.array = this.array;
        segment.offset = this.offset + start;
        segment.count = end - start;
        return segment;
    }
}
