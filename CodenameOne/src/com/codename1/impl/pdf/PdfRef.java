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
package com.codename1.impl.pdf;

/// An indirect reference, `n g R`.
@com.codename1.impl.SharedWithBackend
public final class PdfRef {
    /// The object number.
    public final int number;
    /// The generation number.
    public final int generation;

    /// A reference to object `number`, generation `generation`.
    public PdfRef(int number, int generation) {
        this.number = number;
        this.generation = generation;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PdfRef && ((PdfRef) o).number == number && ((PdfRef) o).generation == generation;
    }

    @Override
    public int hashCode() {
        return number * 31 + generation;
    }

    @Override
    public String toString() {
        return number + " " + generation + " R";
    }
}
