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
package com.codename1.calendar;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/// Immutable set of capabilities advertised by a source or calendar.
public final class CalendarCapabilities {

    private static final CalendarCapabilities NONE = new CalendarCapabilities(new HashSet<CalendarCapability>());

    private final Set<CalendarCapability> values;

    private CalendarCapabilities(Set<CalendarCapability> values) {
        HashSet<CalendarCapability> copy = new HashSet<CalendarCapability>();
        copy.addAll(values);
        this.values = Collections.unmodifiableSet(copy);
    }

    public static CalendarCapabilities none() {
        return NONE;
    }

    public static CalendarCapabilities of(CalendarCapability... values) {
        HashSet<CalendarCapability> out = new HashSet<CalendarCapability>();
        if (values != null) {
            for (CalendarCapability value : values) {
                if (value != null) {
                    out.add(value);
                }
            }
        }
        return out.isEmpty() ? NONE : new CalendarCapabilities(out);
    }

    public boolean supports(CalendarCapability capability) {
        return values.contains(capability);
    }

    public Set<CalendarCapability> asSet() {
        return values;
    }
}
