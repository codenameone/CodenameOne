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
package com.codename1.ui.tree;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Whether a shown tree can be opened and closed from code.
class TreeExpandPathTest extends UITestBase {

    /// html -> head, body -> h1. Everything else is a leaf.
    private static Tree documentTree() {
        return new Tree(new TreeModel() {
            @Override
            public Vector getChildren(Object parent) {
                Vector out = new Vector();
                if (parent == null) {
                    out.addElement("html");
                } else if ("html".equals(parent)) {
                    out.addElement("head");
                    out.addElement("body");
                } else if ("body".equals(parent)) {
                    out.addElement("h1");
                }
                return out;
            }

            @Override
            public boolean isLeaf(Object node) {
                return !"html".equals(node) && !"body".equals(node);
            }
        });
    }

    private static Tree shownTree() {
        Form form = new Form("Tree", new BorderLayout());
        Tree tree = documentTree();
        form.add(BorderLayout.CENTER, tree);
        form.show();
        return tree;
    }

    @FormTest
    void expandPathOpensTheNode() {
        Tree tree = shownTree();
        assertNull(tree.findNodeComponent("body", null), "body should start hidden");

        tree.expandPath(false, new Object[] {"html"});

        assertNotNull(tree.findNodeComponent("body", null),
                "expandPath left the branch closed, so body never appeared");
    }

    @FormTest
    void expandPathWalksSeveralLevels() {
        Tree tree = shownTree();

        tree.expandPath(false, new Object[] {"html", "body"});

        assertNotNull(tree.findNodeComponent("h1", null),
                "the second path element did not expand");
    }

    @FormTest
    void collapsePathClosesTheNode() {
        Tree tree = shownTree();
        tree.expandPath(false, new Object[] {"html", "body"});
        assertNotNull(tree.findNodeComponent("h1", null), "precondition: the tree is open");

        tree.collapsePath("html", "body");

        assertNull(tree.findNodeComponent("h1", null), "collapsePath left the branch open");
    }

    private static String glyphOf(Component node) {
        return ((FontImage) ((com.codename1.ui.Label) node).getIcon()).getText();
    }

    @FormTest
    void anOpenBranchLooksDifferentFromAClosedOne() {
        Tree tree = shownTree();
        Component html = tree.findNodeComponent("html", null);
        String closed = glyphOf(html);

        tree.expandPath(false, new Object[] {"html"});

        assertEquals(String.valueOf(FontImage.MATERIAL_FOLDER_OPEN), glyphOf(html),
                "an expanded branch kept the closed folder icon");

        tree.collapsePath("html");

        assertEquals(closed, glyphOf(html), "collapsing did not restore the closed folder icon");
    }
}
