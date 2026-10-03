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

import com.codename1.flutter.testsupport.AltBox;
import com.codename1.flutter.testsupport.ProbeBox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanUpdateTest {

    @Test
    void sameTypeNoKeysCanUpdate() {
        assertTrue(Widget.canUpdate(new ProbeBox(1, 1), new ProbeBox(2, 2)));
    }

    @Test
    void differentTypeCannotUpdate() {
        assertFalse(Widget.canUpdate(new ProbeBox(1, 1), new AltBox(1, 1)));
    }

    @Test
    void sameTypeEqualValueKeysCanUpdate() {
        ProbeBox a = new ProbeBox(1, 1);
        a.key(new ValueKey<String>("k"));
        ProbeBox b = new ProbeBox(2, 2);
        b.key(new ValueKey<String>("k"));
        assertTrue(Widget.canUpdate(a, b));
    }

    @Test
    void sameTypeDifferentKeysCannotUpdate() {
        ProbeBox a = new ProbeBox(1, 1);
        a.key(new ValueKey<String>("k1"));
        ProbeBox b = new ProbeBox(1, 1);
        b.key(new ValueKey<String>("k2"));
        assertFalse(Widget.canUpdate(a, b));
    }

    @Test
    void keyOnOnlyOneSideCannotUpdate() {
        ProbeBox a = new ProbeBox(1, 1);
        a.key(new ValueKey<Long>(7L));
        assertFalse(Widget.canUpdate(a, new ProbeBox(1, 1)));
        assertFalse(Widget.canUpdate(new ProbeBox(1, 1), a));
    }

    @Test
    void nullsNeverUpdate() {
        assertFalse(Widget.canUpdate(null, new ProbeBox(1, 1)));
        assertFalse(Widget.canUpdate(new ProbeBox(1, 1), null));
        assertFalse(Widget.canUpdate(null, null));
    }
}
