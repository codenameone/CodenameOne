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
package com.codename1.android.rescompiler;

import java.util.ArrayList;
import java.util.List;

/// A resource XML file with every attribute encoded: what the runtime's
/// `XmlResourceParser` and `AttributeSet` walk. Attribute names keep their
/// namespace key and local name next to the attr id, because
/// `AttributeSet.getAttributeValue(namespace, name)` is asked by name.
public final class XmlTree {

    public static final class Attr {
        public final int attrId;
        public final int ns;
        public final String name;
        public final Value value;

        public Attr(int attrId, int ns, String name, Value value) {
            this.attrId = attrId;
            this.ns = ns;
            this.name = name;
            this.value = value;
        }
    }

    public final String tag;
    public final int line;
    public final List<Attr> attrs = new ArrayList<Attr>();
    public final List<XmlTree> children = new ArrayList<XmlTree>();
    /// Trimmed own text, or null when there is none.
    public String text;

    public XmlTree(String tag, int line) {
        this.tag = tag;
        this.line = line;
    }
}
