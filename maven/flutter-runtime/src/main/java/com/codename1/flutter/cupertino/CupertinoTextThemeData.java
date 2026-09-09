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

import com.codename1.flutter.TextStyle;

/**
 * The iOS default text styles, mirroring Flutter's {@code CupertinoTextThemeData}.
 * Each getter returns a fresh {@link TextStyle} carrying the default logical
 * size for that role (TextStyle is a mutable write-once config, so fresh
 * instances avoid leaking a call site's mutation).
 */
public class CupertinoTextThemeData {

    private static TextStyle sized(double size) {
        TextStyle t = new TextStyle();
        t.fontSize(size);
        return t;
    }

    public TextStyle textStyle() {
        return sized(17);
    }

    public TextStyle actionTextStyle() {
        return sized(17);
    }

    public TextStyle navTitleTextStyle() {
        return sized(17);
    }

    public TextStyle navLargeTitleTextStyle() {
        return sized(34);
    }

    public TextStyle tabLabelTextStyle() {
        return sized(10);
    }

    public TextStyle pickerTextStyle() {
        return sized(21);
    }
}
