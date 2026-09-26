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
package com.codename1.flutter.cupertino;

/**
 * The iOS system colors, mirroring Flutter's {@code CupertinoColors}. Each is
 * a {@link CupertinoDynamicColor}; this single-appearance runtime uses the
 * light-mode value (the demos resolve them via {@code resolveFrom(context)}
 * but never switch appearance).
 */
public final class CupertinoColors {

    private CupertinoColors() {
    }

    public static final CupertinoDynamicColor systemBackground = new CupertinoDynamicColor(0xFFFFFFFFL);
    public static final CupertinoDynamicColor label = new CupertinoDynamicColor(0xFF000000L);
    public static final CupertinoDynamicColor inactiveGray = new CupertinoDynamicColor(0xFF999999L);
    public static final CupertinoDynamicColor systemBlue = new CupertinoDynamicColor(0xFF007AFFL);
    public static final CupertinoDynamicColor systemGrey = new CupertinoDynamicColor(0xFF8E8E93L);
    public static final CupertinoDynamicColor activeBlue = new CupertinoDynamicColor(0xFF007AFFL);
    public static final CupertinoDynamicColor activeGreen = new CupertinoDynamicColor(0xFF34C759L);
    public static final CupertinoDynamicColor destructiveRed = new CupertinoDynamicColor(0xFFFF3B30L);
    public static final CupertinoDynamicColor white = new CupertinoDynamicColor(0xFFFFFFFFL);
    public static final CupertinoDynamicColor black = new CupertinoDynamicColor(0xFF000000L);
}
