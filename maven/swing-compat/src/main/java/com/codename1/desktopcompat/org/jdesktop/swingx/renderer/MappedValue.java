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
package com.codename1.desktopcompat.org.jdesktop.swingx.renderer;

import com.codename1.desktopcompat.javax.swing.Icon;

/// A text converter, an icon converter and a boolean converter in one
/// object. A missing converter answers the empty string, no icon and
/// `false`.
public class MappedValue implements StringValue, IconValue, BooleanValue {

    private final StringValue stringDelegate;
    private final IconValue iconDelegate;
    private final BooleanValue booleanDelegate;

    public MappedValue(StringValue stringDelegate, IconValue iconDelegate) {
        this(stringDelegate, iconDelegate, null);
    }

    public MappedValue(StringValue stringDelegate, IconValue iconDelegate, BooleanValue booleanDelegate) {
        this.stringDelegate = stringDelegate;
        this.iconDelegate = iconDelegate;
        this.booleanDelegate = booleanDelegate;
    }

    @Override
    public String getString(Object value) {
        return stringDelegate != null ? stringDelegate.getString(value) : "";
    }

    @Override
    public Icon getIcon(Object value) {
        return iconDelegate != null ? iconDelegate.getIcon(value) : null;
    }

    @Override
    public boolean getBoolean(Object value) {
        return booleanDelegate != null && booleanDelegate.getBoolean(value);
    }
}
