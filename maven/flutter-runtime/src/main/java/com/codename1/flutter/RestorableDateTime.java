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

import dart.core.DateTime;

/**
 * A restorable {@code DateTime} — Flutter's {@code RestorableDateTime} (a
 * {@code RestorableValue<DateTime>}). The value is held as a
 * {@link dart.core.DateTime} so callers can read date components (e.g.
 * {@code value.millisecondsSinceEpoch}). Restoration is not persisted.
 */
public class RestorableDateTime extends RestorableProperty<DateTime> {

    private DateTime current;

    public RestorableDateTime(DateTime defaultValue) {
        this.current = defaultValue;
    }

    public DateTime value() {
        return current;
    }

    public void value(DateTime v) {
        if (current == null ? v != null : !current.equals(v)) {
            current = v;
            notifyListeners();
        }
    }

    @Override
    public DateTime createDefaultValue() {
        return current;
    }

    @Override
    public void initWithValue(DateTime value) {
        this.current = value;
    }
}
