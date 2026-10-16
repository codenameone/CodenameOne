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
 * A restorable {@code DateTime} -- Flutter's {@code RestorableDateTime}, a
 * {@link RestorableValue}{@code <DateTime>} as the Dart stub declares, so it can be
 * passed wherever a {@code RestorableValue<DateTime>} is expected. The value is held
 * as a {@link dart.core.DateTime} so callers can read date components (e.g.
 * {@code value.millisecondsSinceEpoch}). Restoration is not persisted.
 */
public class RestorableDateTime extends RestorableValue<DateTime> {

    private final DateTime defaultValue;

    public RestorableDateTime(DateTime defaultValue) {
        this.defaultValue = defaultValue;
    }

    /** Notifies only for a different instant, as before. */
    @Override
    public void value(DateTime v) {
        DateTime current = value();
        if (current == null ? v != null : !current.equals(v)) {
            super.value(v);
        }
    }

    @Override
    public DateTime createDefaultValue() {
        return defaultValue;
    }
}
