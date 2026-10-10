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
package com.codename1.desktopcompat.com.formdev.flatlaf;

/// FlatLaf's "FlatLaf Darcula" look: on this layer, the dark palette. See
/// [FlatLaf][com.codename1.desktopcompat.com.formdev.flatlaf.FlatLaf].
public class FlatDarculaLaf extends FlatDarkLaf {

    public static final String NAME = "FlatLaf Darcula";

    public FlatDarculaLaf() {
    }

    /// Sets this look and feel and answers whether that worked.
    public static boolean setup() {
        return setup(new FlatDarculaLaf());
    }

    /// The same as [#setup], under its older name.
    public static boolean install() {
        return setup();
    }

    /// Adds this look and feel to those `UIManager` lists as installed.
    public static void installLafInfo() {
        installLafInfo(NAME, FlatDarculaLaf.class);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "FlatLaf Darcula Look and Feel";
    }
}
