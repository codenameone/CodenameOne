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
package com.codename1.util;

import com.codename1.testing.AbstractTest;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class StringUtilTests extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        String testStr = "1,2,3,,,,,,5,6,3";
        String expected = "[1, 2, 3, 5, 6, 3]";
        //StringTokenizer strtok = new StringTokenizer(testStr, ",");
        //List<String> toks = new ArrayList<>();
        //while (strtok.hasMoreTokens()) {
        //    toks.add(strtok.nextToken());
        //}

        List<String> toks2 = StringUtil.tokenize(testStr, ",");
        assertEqual(expected, toks2.toString());
        return true;
    }
    
}
