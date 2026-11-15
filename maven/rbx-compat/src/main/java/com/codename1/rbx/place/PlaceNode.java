/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.rbx.place;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// One instance of a place or model file: its class, its serialized
/// properties in file order, and its children. Readers build this tree and
/// writers consume it; [PlaceLoader] turns it into live instances.
public final class PlaceNode {
    public final String className;
    /// The file's own identifier for this node (`RBX...` in XML, a number in
    /// the binary format); only meaningful within one file.
    public String referent;
    public final Map<String, PlaceValue> props = new LinkedHashMap<String, PlaceValue>();
    public final List<PlaceNode> children = new ArrayList<PlaceNode>();
    public PlaceNode parent;
    /// Set by the binary format for the services a place declares.
    public boolean service;

    public PlaceNode(String className) {
        this.className = className;
    }

    public void add(PlaceNode child) {
        child.parent = this;
        children.add(child);
    }

    public void put(String name, String type, Object value) {
        props.put(name, new PlaceValue(type, value));
    }

    /// The `Name` property, or the class name when there is none.
    public String name() {
        PlaceValue v = props.get("Name");
        return v != null && v.value instanceof String ? (String) v.value : className;
    }

    /// This node and every descendant, depth first.
    public void walk(List<PlaceNode> out) {
        out.add(this);
        for (PlaceNode c : children) {
            c.walk(out);
        }
    }
}
