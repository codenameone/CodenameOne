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
import com.codename1.ui.Form;
import com.codename1.ui.tree.Tree;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.tree.TreeModel;
import com.codename1.xml.Element;
import com.codename1.xml.XMLParser;

import com.codename1.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

/// The XML Tree figure for the Components chapter.
public final class XmlTreeFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-tree-xml";
    }

    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-183[]
        Form hi = new Form("XML Tree", new BorderLayout());
        // parsed inline here so the sample runs as it stands; in an application
        // this would come from the network or a packaged resource
        String xml = "<project name=\"demo\">"
                + "  <target name=\"compile\"><javac srcdir=\"src\"/></target>"
                + "  <target name=\"jar\"><zip destfile=\"demo.jar\"/></target>"
                + "</project>";
        try(Reader r = new CharArrayReader(xml.toCharArray())) {
            Element e = new XMLParser().parse(r);
            Tree xmlTree = new Tree(new XMLTreeModel(e)) {
                @Override
                protected String childToDisplayLabel(Object child) {
                    if(child instanceof Element) {
                        Element e = (Element)child;
                        // getTagName() throws for the elements that carry text
                        if(e.isTextElement()) {
                            return e.getText();
                        }
                        return e.getTagName();
                    }
                    return child.toString();
                }
            };
            hi.add(BorderLayout.CENTER, xmlTree);
        } catch(IOException err) {
            Log.e(err);
        }
        // end::the-components-of-codename-one-java-183[]
        return hi;
    }

    /// The model the chapter has the reader write, included here so the
    /// picture shows a populated tree. The compile-only fixture beside the
    /// listing is a stub that reports every node a leaf, which would have
    /// rendered an empty one.
    // tag::the-components-of-codename-one-java-184[]
    static class XMLTreeModel implements TreeModel {
        private Element root;
        public XMLTreeModel(Element e) {
            root = e;
        }

        public Vector getChildren(Object parent) {
            if(parent == null) {
                Vector c = new Vector();
                c.addElement(root);
                return c;
            }
            Vector result = new Vector();
            Element e = (Element)parent;
            for(int iter = 0 ; iter < e.getNumChildren() ; iter++) {
                result.addElement(e.getChildAt(iter));
            }
            return result;
        }

        public boolean isLeaf(Object node) {
            Element e = (Element)node;
            return e.getNumChildren() == 0;
        }
    }
    // end::the-components-of-codename-one-java-184[]
}
