/*
 * Copyright (c) 2019, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.testnatives;

import com.codename1.system.NativeLookup;
import com.codename1.testing.AbstractTest;
import java.util.Arrays;


public class TestNativesTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        MyNativeInterface ni = (MyNativeInterface)NativeLookup.create(MyNativeInterface.class);
        assertArrayEqual(new byte[]{1, 2, 3, -1}, ni.getBytes());
        assertArrayEqual(new int[]{1, 2, 3, -1}, ni.getInts());
        assertEqual(Arrays.toString(new double[]{1, 2, 3, -1}), Arrays.toString(ni.getDouble()));
        assertArrayEqual(new int[]{3, 2, 1}, ni.setInts(new int[]{3, 2, 1}));
        assertArrayEqual(new byte[]{4, 5, 6}, ni.setBytes(new byte[]{4, 5, 6}));
        assertEqual(Arrays.toString(new double[]{7, 8, 9}), Arrays.toString(ni.setDoubles(new double[]{7, 8, 9})));
        return true;
    }
    
}
