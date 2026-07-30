/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.designer.css;

/**
 * CSSTheme.load touches Display / Util, which need a CodenameOneImplementation
 * to be installed or they hand back nulls. Tests that drive the compiler
 * directly install the same minimal headless stub the no-cef CLI uses.
 *
 * @see NoCefCSSCLI
 */
final class HeadlessTestSupport {

    private HeadlessTestSupport() {}

    /**
     * Installs {@link HeadlessCssCompilerImplementation} into Display and Util.
     * Idempotent, so it is safe to call from every test class in the module.
     */
    static void installHeadlessImplementation() throws Exception {
        // Display.impl is package-private and there is no public installer, so
        // reflect into the field once. Util keeps its own copy of the
        // implementation reference which is settable through a public method.
        HeadlessCssCompilerImplementation stub = new HeadlessCssCompilerImplementation();
        Class<?> displayCls = Class.forName("com.codename1.ui.Display");
        java.lang.reflect.Field implField = displayCls.getDeclaredField("impl");
        implField.setAccessible(true);
        if (implField.get(null) == null) {
            implField.set(null, stub);
        }
        com.codename1.io.Util.setImplementation(stub);
    }
}
