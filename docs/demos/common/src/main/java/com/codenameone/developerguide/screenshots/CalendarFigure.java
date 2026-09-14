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
package com.codenameone.developerguide.screenshots;

import com.codename1.io.Log;
import com.codename1.ui.Calendar;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import java.util.Date;

/// Figure for the Components chapter.
///
/// The chapter's sample uses `new Calendar()`, which opens on today. A figure
/// has to be reproducible, so this one pins the month -- otherwise the golden
/// would change on the first of every month and the guide's screenshot check
/// would fail for reasons that have nothing to do with the code.
public final class CalendarFigure implements GuideFigure {

    /// 2024-06-15T12:00:00Z, chosen only for being fixed.
    private static final long PINNED = 1718452800000L;

    @Override
    public String id() {
        return "components-calendar";
    }

    @Override
    public Form build() {
        Form hi = new Form("Calendar", new BorderLayout());
        Calendar cld = new Calendar(PINNED);
        cld.addActionListener((e) -> Log.p("You picked: " + new Date(cld.getSelectedDay())));
        hi.add(BorderLayout.CENTER, cld);
        hi.show();
        return hi;
    }
}
