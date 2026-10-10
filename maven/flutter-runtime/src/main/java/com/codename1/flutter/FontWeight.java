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

/**
 * Font weights w100..w900 with Flutter's {@code normal} (w400) and
 * {@code bold} (w700) aliases. CN1 fonts only distinguish plain/bold, so
 * weights of w600 and up render bold.
 */
public enum FontWeight {
    w100, w200, w300, w400, w500, w600, w700, w800, w900;

    public static final FontWeight normal = w400;
    public static final FontWeight bold = w700;

    /**
     * Whether this weight maps to CN1's bold style.
     */
    public boolean isBold() {
        return ordinal() >= w600.ordinal();
    }
}
