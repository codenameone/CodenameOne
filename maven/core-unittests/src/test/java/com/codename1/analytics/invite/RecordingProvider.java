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
package com.codename1.analytics.invite;

import com.codename1.analytics.AbstractAnalyticsProvider;
import com.codename1.analytics.AnalyticsCapability;
import com.codename1.analytics.AnalyticsEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures whole AnalyticsEvent objects rather than the rendered strings
 * LoggingAnalyticsProvider keeps, so a test can assert on the category and on
 * individual parameter values.
 */
class RecordingProvider extends AbstractAnalyticsProvider {
    private final List<AnalyticsEvent> events = new ArrayList<AnalyticsEvent>();

    @Override
    public String getName() {
        return "recording";
    }

    @Override
    public void trackEvent(AnalyticsEvent event) {
        events.add(event);
    }

    @Override
    public boolean supports(AnalyticsCapability capability) {
        return true;
    }

    List<AnalyticsEvent> events() {
        return events;
    }

    void clear() {
        events.clear();
    }

    AnalyticsEvent first(String name) {
        for (AnalyticsEvent e : events) {
            if (name.equals(e.getName())) {
                return e;
            }
        }
        return null;
    }

    int count(String name) {
        int n = 0;
        for (AnalyticsEvent e : events) {
            if (name.equals(e.getName())) {
                n++;
            }
        }
        return n;
    }

    List<String> names() {
        List<String> out = new ArrayList<String>();
        for (AnalyticsEvent e : events) {
            out.add(e.getName());
        }
        return out;
    }
}
