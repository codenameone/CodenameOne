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

import com.codename1.ui.Form;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.list.MultiList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/// A MultiList over a million books, scrolled a long way in.
class ComponentsMillionEntriesFigure implements GuideFigure {

    private MultiList list;

    @Override
    public String id() {
        return "components-millionbooks";
    }

    /// Both tagged regions are what the chapter includes -- the model and the
    /// line that uses it -- so the two listings around the picture are the code
    /// that drew it.
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

    // tag::the-components-of-codename-one-java-168[]
    static class GRMMModel implements ListModel<Map<String, Object>> {
        private int selection;
        private final java.util.List<SelectionListener> selectionListeners = new ArrayList<>();

        @Override
        public Map<String, Object> getItemAt(int index) {
            int idx = index % 7;
            switch (idx) {
                case 0:
                    return createListEntry("A Game of Thrones " + index, "1996");
                case 1:
                    return createListEntry("A Clash Of Kings " + index, "1998");
                case 2:
                    return createListEntry("A Storm Of Swords " + index, "2000");
                case 3:
                    return createListEntry("A Feast For Crows " + index, "2005");
                case 4:
                    return createListEntry("A Dance With Dragons " + index, "2011");
                case 5:
                    return createListEntry("The Winds of Winter " + index, "2016 (please, please, please)");
                default:
                    return createListEntry("A Dream of Spring " + index, "Ugh");
            }
        }

        @Override
        public int getSize() {
            return 1000000;
        }

        @Override
        public int getSelectedIndex() {
            return selection;
        }

        @Override
        public void setSelectedIndex(int index) {
            int old = selection;
            selection = index;
            for (SelectionListener l : selectionListeners) {
                l.selectionChanged(old, index);
            }
        }

        @Override
        public void addDataChangedListener(DataChangedListener l) {
        }

        @Override
        public void removeDataChangedListener(DataChangedListener l) {
        }

        @Override
        public void addSelectionListener(SelectionListener l) {
            selectionListeners.add(l);
        }

        @Override
        public void removeSelectionListener(SelectionListener l) {
            selectionListeners.remove(l);
        }

        @Override
        public void addItem(Map<String, Object> item) {
        }

        @Override
        public void removeItem(int index) {
        }
    }
    // end::the-components-of-codename-one-java-168[]

    static Map<String, Object> createListEntry(String name, String date) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("Line1", name);
        entry.put("Line2", date);
        return entry;
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
