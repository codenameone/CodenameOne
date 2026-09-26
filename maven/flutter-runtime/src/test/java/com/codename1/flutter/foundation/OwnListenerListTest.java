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
package com.codename1.flutter.foundation;

import com.codename1.flutter.scopedmodel.Model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// A notifier with its own listener list leaves nothing in the shared map: a listener
/// that closes over the notifier would otherwise keep it alive through the map's value.
class OwnListenerListTest {

    @Test
    void aNotifierThatClosesOverItselfLeavesNoSharedEntry() {
        final Model model = new Model();
        final int[] heard = {0};
        model.addListener(() -> heard[0] += model.hashCode() == 0 ? 0 : 1);   // closes over the model
        assertNull(ChangeNotifier.LISTENERS.get(model), "no shared-map entry leading back to the model");
        model.notifyListeners();
        assertEquals(1, heard[0]);
        model.dispose();
        model.notifyListeners();
        assertEquals(1, heard[0], "disposed: nothing is told any more");
    }
}
