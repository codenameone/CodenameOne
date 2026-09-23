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
package com.codename1.ui.animations;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ContainerTransformNoSourceTest extends UITestBase {

    @Test
    void aTransitionWithNoSourceFormDoesNotThrow() {
        // The first Form shown has nothing to transition from; the Transition contract
        // allows a null source, and dereferencing it made that first show throw.
        Form destination = new Form();
        destination.setWidth(200);
        destination.setHeight(300);
        final ContainerTransformTransition t = ContainerTransformTransition.create("anything", 200);
        t.init(null, destination);
        assertDoesNotThrow(t::initTransition);
        assertFalse(t.animate(), "with nothing to transform from there is no animation");
    }
}
