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
package com.codename1.backend.graphql;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// One named type of a schema.
final class SchemaType {
    static final String SCALAR = "SCALAR";
    static final String OBJECT = "OBJECT";
    static final String INTERFACE = "INTERFACE";
    static final String UNION = "UNION";
    static final String ENUM = "ENUM";
    static final String INPUT_OBJECT = "INPUT_OBJECT";

    final String kind;
    final String name;
    String description;
    List<Parser.Directive> directives;
    final Map<String, FieldDef> fields = new LinkedHashMap<String, FieldDef>();
    final List<String> interfaces = new ArrayList<String>();
    final List<String> unionMembers = new ArrayList<String>();
    final Map<String, EnumValueDef> enumValues = new LinkedHashMap<String, EnumValueDef>();
    final Map<String, InputValueDef> inputFields = new LinkedHashMap<String, InputValueDef>();

    SchemaType(String kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    boolean isLeaf() {
        return SCALAR.equals(kind) || ENUM.equals(kind);
    }

    boolean isAbstract() {
        return INTERFACE.equals(kind) || UNION.equals(kind);
    }

    boolean isComposite() {
        return OBJECT.equals(kind) || isAbstract();
    }

    boolean isInput() {
        return isLeaf() || INPUT_OBJECT.equals(kind);
    }
}
