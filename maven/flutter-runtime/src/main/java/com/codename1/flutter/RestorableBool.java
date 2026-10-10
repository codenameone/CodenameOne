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
 * A restorable non-null boolean ({@code RestorableBool} in Flutter). The value
 * lives in a field; setting it notifies listeners. Restoration is not persisted.
 */
public class RestorableBool extends RestorableProperty<Boolean> {

    private boolean current;

    public RestorableBool(boolean defaultValue) {
        this.current = defaultValue;
    }

    public boolean value() {
        return current;
    }

    public void value(boolean v) {
        if (current != v) {
            current = v;
            notifyListeners();
        }
    }

    @Override
    public Boolean createDefaultValue() {
        return current;
    }

    @Override
    public void initWithValue(Boolean value) {
        this.current = value != null && value.booleanValue();
    }
}
