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
package com.codename1.health;

/// A bounded activity that groups child data -- a workout, a night's
/// sleep, a logged meal.
///
/// Sessions are always intervals: [HealthSample#isInstantaneous()] is
/// false for every subclass.
public abstract class SessionSample extends HealthSample {

    private String title;
    private String notes;

    /// Creates a session spanning `[startMillis, endMillis]`.
    protected SessionSample(HealthDataType type, long startMillis,
            long endMillis) {
        super(type, startMillis, endMillis);
    }

    /// A user-visible title, or null. Health Connect stores this natively;
    /// on iOS it is carried in sample metadata.
    public final String getTitle() {
        return title;
    }

    /// Sets the user-visible title.
    public final void setTitle(String title) {
        this.title = title;
    }

    /// Free-form user notes, or null.
    public final String getNotes() {
        return notes;
    }

    /// Sets free-form user notes.
    public final void setNotes(String notes) {
        this.notes = notes;
    }
}
