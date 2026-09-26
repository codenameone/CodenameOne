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
package com.codename1.flutter.services;

/**
 * Describes the status/navigation-bar appearance, mirroring Flutter's
 * {@code SystemUiOverlayStyle}. Inert here (CN1 manages system chrome
 * separately); the {@code light}/{@code dark} presets are provided for API
 * shape.
 */
public final class SystemUiOverlayStyle {

    /** Overlays suited to a light (bright) background. */
    public static final SystemUiOverlayStyle light = new SystemUiOverlayStyle("light");

    /** Overlays suited to a dark background. */
    public static final SystemUiOverlayStyle dark = new SystemUiOverlayStyle("dark");

    private final String name;

    private SystemUiOverlayStyle(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "SystemUiOverlayStyle." + name;
    }
}
