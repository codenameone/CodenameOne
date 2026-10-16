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
package com.codename1.flutter.vectormath;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * vector_math's setRotationZ replaces the whole upper 3x3 and the perspective row;
 * only the translation survives. Setting just the four sine/cosine entries kept a
 * reused matrix's scale (diagonal3Values(2, 3, 4)..setRotationZ(0) stayed scaled by 4
 * in z).
 */
class Matrix4RotationTest {

    @Test
    void setRotationZReplacesScaleAndPerspectiveButKeepsTranslation() {
        Matrix4 m = Matrix4.diagonal3Values(2, 3, 4);
        m.setEntry(3, 0, 0.5);   // a perspective term
        m.setEntry(0, 3, 7);     // translation x
        m.setRotationZ(0);
        double[] want = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 7, 0, 0, 1};
        for (int i = 0; i < 16; i++) {
            assertEquals(want[i], m.storage().get(i).doubleValue(), 1e-12, "entry " + i);
        }
    }
}
