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
package com.codename1.flutter;

import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.vectormath.Matrix4;
import com.codename1.flutter.widgets.AsyncSnapshot;
import com.codename1.flutter.widgets.ConnectionState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Value types hash consistently with their equality, a matrix's storage is the
/// matrix, and requireData refuses to answer null.
class ValueSemanticsTest {

    private static void equalAndSameHash(Object a, Object b) {
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode(), "equal values must hash alike: " + a + " / " + b);
    }

    @Test
    void negativeZeroHashesLikeZeroInEveryValueType() {
        equalAndSameHash(new Offset(0.0, 1), new Offset(-0.0, 1));
        equalAndSameHash(new Size(0.0, 1), new Size(-0.0, 1));
        equalAndSameHash(new Alignment(-0.0, 0.0), new Alignment(0.0, -0.0));
        equalAndSameHash(Radius.circular(0.0), Radius.circular(-0.0));
        equalAndSameHash(Rect.fromLTRB(0.0, 0, 1, 1), Rect.fromLTRB(-0.0, 0, 1, 1));
        equalAndSameHash(EdgeInsets.fromLTRB(0.0, 0, 1, 1), EdgeInsets.fromLTRB(-0.0, 0, 1, 1));
        equalAndSameHash(new BoxConstraints(0.0, 1, 0, 1), new BoxConstraints(-0.0, 1, 0, 1));
        java.util.Set<Offset> set = new java.util.HashSet<Offset>();
        set.add(new Offset(0.0, 5));
        assertEquals(true, set.contains(new Offset(-0.0, 5)));
    }

    @Test
    void writingThroughStorageMovesTheMatrix() {
        Matrix4 m = Matrix4.identity();
        m.storage().set(12, 20.0);
        assertEquals(20.0, m.storage().get(12), 0.0);
        assertSame(m.storage(), m.storage(), "the same backing list every time, as vector_math returns");
        Matrix4 copy = Matrix4.identity();
        copy.storage().set(12, 20.0);
        assertEquals(m.storage().get(12), copy.storage().get(12), 0.0);
    }

    @Test
    void requireDataThrowsInsteadOfAnsweringNull() {
        final IllegalStateException failure = new IllegalStateException("load failed");
        AsyncSnapshot<String> failed = new AsyncSnapshot<String>(ConnectionState.done, null, failure, null);
        assertSame(failure, assertThrows(IllegalStateException.class, failed::requireData),
                "the stored error surfaces, not a null");
        AsyncSnapshot<String> empty = new AsyncSnapshot<String>(ConnectionState.waiting, null, null, null);
        assertThrows(dart.core.StateError.class, empty::requireData);
        assertEquals("v", new AsyncSnapshot<String>(ConnectionState.done, "v", null, null).requireData());
    }
}
