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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Online meeting metadata associated with an event.
public class CalendarConference {

    private String provider;

    private String id;

    private String joinUrl;

    private boolean createRequested;

    private final List<String> phoneNumbers = new ArrayList<String>();

    public String getProvider() {
        return provider;
    }

    public CalendarConference setProvider(String v) {
        provider = v;
        return this;
    }

    public String getId() {
        return id;
    }

    public CalendarConference setId(String v) {
        id = v;
        return this;
    }

    public String getJoinUrl() {
        return joinUrl;
    }

    public CalendarConference setJoinUrl(String v) {
        joinUrl = v;
        return this;
    }

    public boolean isCreateRequested() {
        return createRequested;
    }

    public CalendarConference setCreateRequested(boolean v) {
        createRequested = v;
        return this;
    }

    public CalendarConference addPhoneNumber(String v) {
        if (v != null) {
            phoneNumbers.add(v);
        }
        return this;
    }

    public List<String> getPhoneNumbers() {
        return Collections.unmodifiableList(phoneNumbers);
    }
}
