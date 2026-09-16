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

import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.GenericListCellRenderer;

import java.util.HashMap;
import java.util.Map;

/// A list drawn by a GenericListCellRenderer built from real components.
class ComponentsGenericRendererFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-generic-list-cell-renderer";
    }

    @Override
    public Form build() {
        Form hi = new Form("Generic Renderer", new BorderLayout());
        List<Map<String, Object>> list = new List<>(new DefaultListModel<>(
                entry("Eddard", "Stark", Boolean.TRUE),
                entry("Catelyn", "Tully", Boolean.FALSE),
                entry("Robb", "Stark", Boolean.FALSE),
                entry("Sansa", "Stark", Boolean.TRUE),
                entry("Arya", "Stark", Boolean.FALSE),
                entry("Jon", "Snow", Boolean.TRUE)));
        // tag::the-components-of-codename-one-java-060[]
        list.setRenderer(new GenericListCellRenderer(createGenericRendererContainer(), createGenericRendererContainer()));
        // end::the-components-of-codename-one-java-060[]
        hi.add(BorderLayout.CENTER, list);
        hi.show();
        return hi;
    }

    private static Map<String, Object> entry(String name, String surname, Boolean selected) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("Name", name);
        entry.put("Surname", surname);
        entry.put("Selected", selected);
        return entry;
    }

    /// The renderer container the chapter builds a few lines above: a component
    /// per entry key, named so the renderer can match them up.
    private static Container createGenericRendererContainer() {
        Label name = new Label();
        name.setFocusable(true);
        name.setName("Name");
        Label surname = new Label();
        surname.setFocusable(true);
        surname.setName("Surname");
        CheckBox selected = new CheckBox();
        selected.setName("Selected");
        selected.setFocusable(true);
        Container c = BorderLayout.center(name).
                add(BorderLayout.SOUTH, surname).
                add(BorderLayout.WEST, selected);
        c.setUIID("ListRenderer");
        return c;
    }

    @Override
    public boolean fillsViewport() {
        return true;
    }
}
