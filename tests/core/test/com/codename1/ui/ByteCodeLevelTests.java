/*
 * Copyright (c) 2018, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import java.util.ArrayList;
import java.util.Arrays;

public class ByteCodeLevelTests extends AbstractTest {

    
    @Override
    public boolean runTest() throws Exception {
        assertTrue("" instanceof Object, "String should be instanceof Object");
        assertTrue(new ArrayList() instanceof java.util.List, "ArrayList should be instanceof List");
        assertTrue(new ArrayList[0] instanceof java.util.List[], "ArrayList[] should be instanceof List");
        assertTrue(!(new java.util.List[0] instanceof ArrayList[]), "List[] should not be instanceof ArrayList[]");
        assertTrue(new String[0] instanceof Object[], "String[] should be instanceof Object[]");
        assertTrue(new int[0] instanceof Object, "int[] should be instanceof Object");
        assertTrue(new String[0] instanceof Object, "String[] should be instanceof Object");
        assertTrue(new int[0] instanceof int[], "int[] should be instanceof int[]");
        assertTrue(new String[0] instanceof String[], "String[] should be instanceof String[]");
        assertTrue(new int[1][1] instanceof Object[], "int[][] should be instanceof Object[]");
        assertTrue(new int[1][1][1] instanceof Object[][], "int[][][] should be instanceof Object[][]");
        
        int[][] a = {{0,0,0},{0,0,0}};
        a[0][1]++;
        TestUtils.assertEqual("[[0, 1, 0], [0, 0, 0]]", Arrays.deepToString(a), "deepToString(int[][]) incorrect result");
        
        return true;

    }

}
