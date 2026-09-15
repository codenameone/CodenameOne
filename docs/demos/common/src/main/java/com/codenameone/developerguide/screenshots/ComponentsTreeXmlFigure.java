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

import com.codename1.io.CharArrayReader;
import com.codename1.io.Log;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.tree.Tree;
import com.codename1.ui.tree.TreeModel;
import com.codename1.xml.Element;
import com.codename1.xml.XMLParser;

import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

/// The XML tree the components chapter builds, opened two levels deep.
class ComponentsTreeXmlFigure implements GuideFigure {

    private Tree xmlTree;

    @Override
    public String id() {
        return "components-tree-xml";
    }

    /// The tagged regions are what the chapter includes, so the two listings
    /// beside the picture are the code that drew it.
    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-183[]
        Form hi = new Form("XML Tree", new BorderLayout());
        // Parsed inline so the sample runs as it stands; in an application this
        // would come from the network or from a packaged resource.
        String xml = "<project name=\"demo\">"
                + "<target name=\"compile\"><javac srcdir=\"src\"/></target>"
                + "<target name=\"jar\"><zip destfile=\"demo.jar\"/></target>"
                + "</project>";
        try (Reader r = new CharArrayReader(xml.toCharArray())) {
            Element e = new XMLParser().parse(r);
            Tree tree = new Tree(new XMLTreeModel(e)) {
                @Override
                protected String childToDisplayLabel(Object child) {
                    if (child instanceof Element) {
                        Element el = (Element) child;
                        // getTagName() throws for the elements that carry text
                        if (el.isTextElement()) {
                            return el.getText();
                        }
                        return el.getTagName();
                    }
                    return child.toString();
                }
            };
            hi.add(BorderLayout.CENTER, tree);
            xmlTree = tree;
        } catch (IOException err) {
            Log.e(err);
        }
        hi.show();
        // end::the-components-of-codename-one-java-183[]
        return hi;
    }

    /// A tree draws its root and nothing else until something opens it, and it
    /// cannot be opened before it has been shown -- so this belongs here rather
    /// than in build(). Without the animation, which a still cannot show.
    @Override
    public void afterShow(Form form) {
        if (xmlTree == null) {
            throw new IllegalStateException("the XML did not parse, so there is nothing to photograph");
        }
        Element root = (Element) new Vector(xmlTree.getModel().getChildren(null)).elementAt(0);
        xmlTree.expandPath(false, new Object[] {root});
        xmlTree.expandPath(false, new Object[] {root, root.getChildAt(0)});
    }

    // tag::the-components-of-codename-one-java-184[]
    /// Walks a parsed XML document as a tree: the document element is the single
    /// root, and a node's children are the element's children.
    static class XMLTreeModel implements TreeModel {
        private Element root;

        XMLTreeModel(Element e) {
            root = e;
        }

        @Override
        public Vector getChildren(Object parent) {
            if (parent == null) {
                Vector c = new Vector();
                c.addElement(root);
                return c;
            }
            Vector result = new Vector();
            Element e = (Element) parent;
            for (int iter = 0; iter < e.getNumChildren(); iter++) {
                result.addElement(e.getChildAt(iter));
            }
            return result;
        }

        @Override
        public boolean isLeaf(Object node) {
            Element e = (Element) node;
            return e.getNumChildren() == 0;
        }
    }
    // end::the-components-of-codename-one-java-184[]
}
