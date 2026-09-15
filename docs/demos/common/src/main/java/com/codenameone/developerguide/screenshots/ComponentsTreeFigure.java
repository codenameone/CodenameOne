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
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.tree.Tree;
import com.codename1.ui.tree.TreeModel;

import java.util.Vector;

/// The array-backed tree the components chapter opens with, expanded once.
class ComponentsTreeFigure implements GuideFigure {

    private Tree tree;

    @Override
    public String id() {
        return "tree";
    }

    /// The tagged region is what the chapter includes, so the listing beside the
    /// picture is the code that drew it.
    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-182[]
        class StringArrayTreeModel implements TreeModel {
            String[][] arr = new String[][] {
                {"Colors", "Letters", "Numbers"},
                {"Red", "Green", "Blue"},
                {"A", "B", "C"},
                {"1", "2", "3"}
            };

            @Override
            public Vector getChildren(Object parent) {
                Vector v = new Vector();
                if (parent == null) {
                    for (int iter = 0; iter < arr[0].length; iter++) {
                        v.addElement(arr[0][iter]);
                    }
                    return v;
                }
                for (int iter = 0; iter < arr[0].length; iter++) {
                    if (parent.equals(arr[0][iter]) && arr.length > iter + 1 && arr[iter + 1] != null) {
                        for (int i = 0; i < arr[iter + 1].length; i++) {
                            v.addElement(arr[iter + 1][i]);
                        }
                    }
                }
                return v;
            }

            @Override
            public boolean isLeaf(Object node) {
                Vector v = getChildren(node);
                return v == null || v.size() == 0;
            }
        }

        Tree dt = new Tree(new StringArrayTreeModel());
        Form hi = new Form("Tree", new BorderLayout());
        hi.add(BorderLayout.CENTER, dt);
        hi.show();
        // end::the-components-of-codename-one-java-182[]
        tree = dt;
        return hi;
    }

    /// See ComponentsTreeXmlFigure.afterShow: a tree cannot be opened until it
    /// has been shown.
    @Override
    public void afterShow(Form form) {
        tree.expandPath(false, new Object[] {"Colors"});
    }
}
