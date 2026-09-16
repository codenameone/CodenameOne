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
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.list.MultiList;

import java.util.HashMap;
import java.util.Map;

/// A MultiList over a million entries, scrolled a long way in.
class ComponentsMillionEntriesFigure implements GuideFigure {

    private static final int ENTRIES = 1000000;

    private MultiList list;

    @Override
    public String id() {
        return "components-millionbooks";
    }

    /// The tagged region is what the chapter includes, so the listing beside the
    /// picture is the code that drew it.
    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-169[]
        MultiList ml = new MultiList(new GRMMModel());
        Form hi = new Form("Million Entries", new BorderLayout());
        hi.add(BorderLayout.CENTER, ml);
        hi.show();
        // end::the-components-of-codename-one-java-169[]
        list = ml;
        return hi;
    }

    /// The point of the sample is that nothing is held: a model of a million
    /// entries builds each one as the list asks for it, so scrolling costs the
    /// same at the end as at the start.
    static class GRMMModel implements ListModel<Map<String, Object>> {

        private int selected;

        @Override
        public Map<String, Object> getItemAt(int index) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("Line1", "Entry " + index);
            entry.put("Line2", "One of a million");
            return entry;
        }

        @Override
        public int getSize() {
            return ENTRIES;
        }

        @Override
        public int getSelectedIndex() {
            return selected;
        }

        @Override
        public void setSelectedIndex(int index) {
            selected = index;
        }

        @Override
        public void addDataChangedListener(DataChangedListener l) {
        }

        @Override
        public void removeDataChangedListener(DataChangedListener l) {
        }

        @Override
        public void addSelectionListener(SelectionListener l) {
        }

        @Override
        public void removeSelectionListener(SelectionListener l) {
        }

        @Override
        public void addItem(Map<String, Object> item) {
        }

        @Override
        public void removeItem(int index) {
        }
    }

    /// The caption is about having scrolled a long way in, which is the only
    /// thing that makes the picture say anything -- and a list will not move
    /// before it has a size.
    @Override
    public void afterShow(Form form) {
        list.setSelectedIndex(524288);
        // The scroll that follows the selection is animated, so the picture has
        // to wait for it the way the swipeable rows do.
        GuideFigure.settleAnimations(400);
    }

    @Override
    public boolean fillsViewport() {
        return true;
    }
}
