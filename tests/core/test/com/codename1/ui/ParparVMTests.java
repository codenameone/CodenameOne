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

public class ParparVMTests extends AbstractTest {

    static enum TestEnum2 {
        RED,
        GREEN,
        BLUE
    }
    
    @Override
    public boolean runTest() throws Exception {
        Object anonymousClass = new Object() {
            @Override
            public String toString() {
                return "This is an anonymous class";
            }
        };
            
        assertTrue(TestEnum2.class.isEnum(), "TestEnum2 isEnum() returns wrong value");
        assertTrue(!ParparVMTests.class.isEnum(), "MyTest isEnum() returns wrong value");
        assertTrue(!ParparVMTests.class.isSynthetic(), "MyTest isSynthetic returns wrong value");
        assertTrue(!ParparVMTests.class.isInterface(), "MyTest isInterface returns wrong value");
        assertTrue(com.codename1.ui.events.ActionListener.class.isInterface(), "ActionListener is not recognized as interface");
        assertTrue(!ParparVMTests.class.isAnnotation(), "MyTest is incorrectly recognized as annotation.");
        //assertTrue(TestAnnotation.class.isAnnotation(), "TestAnnotation class is not recognized as annotation");
        assertTrue(!ParparVMTests.class.isAnnotation(), "MyTest is incorrectly recognized as annotation");
        assertTrue(anonymousClass.getClass().isAnonymousClass(), "Anonymous class is not recognized");
        assertTrue(!ParparVMTests.class.isAnonymousClass(), "MyTest is incorrectly recognized as anonymous class");
        //assertTrue(!MyTest.class.isPrimitive(), "MyTest is incorrectly recognized as primitive");
        //assertTrue(int.class.isPrimitive(), "int is incorrectly recognized as primitive");


    return true;
    }
    
}
