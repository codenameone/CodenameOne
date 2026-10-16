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
package com.codename1.flutter.material;

import com.codename1.flutter.Key;
import com.codename1.flutter.ValueKey;

/**
 * One of the standard, individually-keyed components a scaffold builds (the
 * back / close / drawer / more buttons) — Flutter's {@code StandardComponentType}.
 * Each value exposes a stable {@link #key()} the app can target for tests or
 * theming.
 */
public final class StandardComponentType {

    public static final StandardComponentType backButton =
            new StandardComponentType("backButton");
    public static final StandardComponentType closeButton =
            new StandardComponentType("closeButton");
    public static final StandardComponentType drawerButton =
            new StandardComponentType("drawerButton");
    public static final StandardComponentType moreButton =
            new StandardComponentType("moreButton");

    private final Key key;

    private StandardComponentType(String name) {
        this.key = new ValueKey<String>("StandardComponentType." + name);
    }

    /** The stable key identifying this component in the widget tree. */
    public Key key() {
        return key;
    }
}
