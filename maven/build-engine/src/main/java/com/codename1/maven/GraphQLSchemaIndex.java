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
package com.codename1.maven;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// What the backend's GraphQL build checks resolvers against: the object
/// types of a schema, their fields, and each field's arguments with whether
/// the argument is non-null.
public final class GraphQLSchemaIndex {
    private final Map<String, Map<String, Map<String, Boolean>>> types;
    private final String query;
    private final String mutation;
    private final String subscription;
    private final java.util.Set<String> interfaces;

    private GraphQLSchemaIndex(Map<String, Map<String, Map<String, Boolean>>> types,
                               java.util.Set<String> interfaces,
                               String query, String mutation, String subscription) {
        this.types = types;
        this.interfaces = interfaces;
        this.query = query;
        this.mutation = mutation;
        this.subscription = subscription;
    }

    /// Reads `sdl`, every schema file of the module joined.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: the text does not parse; the message says where
    public static GraphQLSchemaIndex parse(String sdl, String file) {
        GraphQLSchemaModel.Schema s;
        try {
            s = new GraphQLSchemaModel.Parser(sdl, file).parse();
        } catch (GraphQLSchemaModel.ParseException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        if (!s.duplicates.isEmpty()) {
            throw new IllegalArgumentException("there can be only one type named "
                    + s.duplicates.iterator().next() + "; use extend type to add fields to it "
                    + "in another file");
        }
        if (!s.extendedOnly.isEmpty()) {
            throw new IllegalArgumentException("cannot extend type "
                    + s.extendedOnly.iterator().next() + " because it is not defined");
        }
        java.util.Set<String> interfaces = new java.util.HashSet<String>();
        Map<String, Map<String, Map<String, Boolean>>> types =
                new LinkedHashMap<String, Map<String, Map<String, Boolean>>>();
        for (GraphQLSchemaModel.ObjectTypeDef o : s.objects.values()) {
            Map<String, Map<String, Boolean>> fields = new LinkedHashMap<String, Map<String, Boolean>>();
            for (GraphQLSchemaModel.FieldDef f : o.fields) {
                Map<String, Boolean> args = new LinkedHashMap<String, Boolean>();
                for (GraphQLSchemaModel.ArgDef a : f.args) {
                    args.put(a.name, Boolean.valueOf(a.type != null && a.type.nonNull));
                }
                fields.put(f.name, args);
            }
            types.put(o.name, fields);
            if (o.isInterface) {
                interfaces.add(o.name);
            }
        }
        return new GraphQLSchemaIndex(types, interfaces, s.queryType, s.mutationType,
                s.subscriptionType);
    }

    /// The name of the root type of `operation` (`query`, `mutation` or
    /// `subscription`).
    public String rootType(String operation) {
        return "mutation".equals(operation) ? mutation
                : "subscription".equals(operation) ? subscription : query;
    }

    /// Whether `type` is an interface type of the schema.
    public boolean isInterface(String type) {
        return interfaces.contains(type);
    }

    /// Whether `type` is an object or interface type of the schema.
    public boolean hasType(String type) {
        return types.containsKey(type);
    }

    /// The fields of `type`, or null.
    public List<String> fields(String type) {
        Map<String, Map<String, Boolean>> f = types.get(type);
        return f == null ? null : new java.util.ArrayList<String>(f.keySet());
    }

    /// The arguments of `type.field`, each with whether it is non-null; null
    /// when the schema has no such field.
    public Map<String, Boolean> arguments(String type, String field) {
        Map<String, Map<String, Boolean>> f = types.get(type);
        return f == null ? null : f.get(field);
    }
}
