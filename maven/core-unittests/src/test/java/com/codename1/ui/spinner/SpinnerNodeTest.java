/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.ui.spinner;

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.list.DefaultListModel;
import static org.junit.jupiter.api.Assertions.*;

public class SpinnerNodeTest extends UITestBase {

    @FormTest
    public void testSpinnerNodeRenderer() {
        TestSpinnerNode node = new TestSpinnerNode();
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addItem("Item 1");
        model.addItem("Item 2");
        node.setListModel(model);

        node.setWidth(200);
        node.doLayout();

        // Verify node has children (overlay + items)
        assertTrue(node.getChildCount() > 0, "SpinnerNode should have children after layout");
    }

    @FormTest
    public void testSelectedIndexRoundTripsForEveryRow() {
        // Issue #6165: with 365 rows, selecting "7" (index 6) read back as index 5.
        SpinnerNode node = new SpinnerNode();
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 1; i <= 365; i++) {
            model.addItem(Integer.toString(i));
        }
        node.setListModel(model);
        for (int i = 0; i < 365; i++) {
            node.setSelectedIndex(i);
            assertEquals(i, node.getSelectedIndex(), "row " + (i + 1));
        }
    }

    public static class TestSpinnerNode extends SpinnerNode {
        public void doLayout() {
            layoutChildren();
        }
        public void setWidth(double w) {
             this.boundsInLocal.get().setWidth(w);
        }
    }
}
