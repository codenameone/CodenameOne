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

import com.codename1.flutter.material.TimeOfDay;

/**
 * A restorable {@link TimeOfDay} property ({@code RestorableTimeOfDay} in
 * Flutter) — new_gallery's picker demo holds the selected time in one. The value
 * lives in a field; setting it notifies listeners. Restoration is not persisted.
 */
public class RestorableTimeOfDay extends RestorableProperty<TimeOfDay> {

    private TimeOfDay current;

    public RestorableTimeOfDay(TimeOfDay defaultValue) {
        this.current = defaultValue;
    }

    public TimeOfDay value() {
        return current;
    }

    public void value(TimeOfDay v) {
        boolean changed = current == null ? v != null : !current.equals(v);
        if (changed) {
            current = v;
            notifyListeners();
        }
    }

    @Override
    public TimeOfDay createDefaultValue() {
        return current;
    }

    @Override
    public void initWithValue(TimeOfDay value) {
        this.current = value;
    }
}
