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

import com.codename1.components.MultiButton;
import com.codename1.ui.Form;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.MultiList;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/// The MultiList figure for the Components chapter.
public final class MultiListFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-multilist";
    }

    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-167[]
        Form hi = new Form("MultiList", new BorderLayout());

        ArrayList<Map<String, Object>> data = new ArrayList<>();

        data.add(createListEntry("A Game of Thrones", "1996"));
        data.add(createListEntry("A Clash Of Kings", "1998"));
        data.add(createListEntry("A Storm Of Swords", "2000"));
        data.add(createListEntry("A Feast For Crows", "2005"));
        data.add(createListEntry("A Dance With Dragons", "2011"));
        data.add(createListEntry("The Winds of Winter", "2016 (please, please, please)"));
        data.add(createListEntry("A Dream of Spring", "Ugh"));

        DefaultListModel<Map<String, Object>> model = new DefaultListModel<>(data);
        MultiList ml = new MultiList(model);
        hi.add(BorderLayout.CENTER, ml);
        // end::the-components-of-codename-one-java-167[]
        return hi;
    }

    private static Map<String, Object> createListEntry(String name, String date) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("Line1", name);
        entry.put("Line2", date);
        return entry;
    }
}
