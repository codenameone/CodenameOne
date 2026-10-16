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

import com.codename1.flutter.material.TextEditingController;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A RestorableTextEditingController owns its controller; disposing the property left
 * the controller's listeners live, so code that kept the controller went on calling
 * back into the disposed state.
 */
class RestorableControllerDisposeTest {

    @Test
    void disposingThePropertyDisposesItsController() {
        RestorableTextEditingController p = new RestorableTextEditingController();
        TextEditingController c = p.value();
        final int[] calls = {0};
        c.addListener(() -> calls[0]++);
        c.setText("a");
        assertEquals(1, calls[0]);

        p.dispose();
        c.setText("b");
        assertEquals(1, calls[0], "the disposed state's listener is not called again");
    }
}
