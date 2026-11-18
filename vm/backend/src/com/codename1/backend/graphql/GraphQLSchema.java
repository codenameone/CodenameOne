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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A schema read from GraphQL schema definition language, the `.graphqls`
/// files Spring for GraphQL reads from `src/main/resources/graphql`.
///
/// The schema is checked when it is parsed: every type a field names must be
/// defined, an object must declare every field of the interfaces it
/// implements, and a union may only list object types. Each failure is a
/// [GraphQLException] naming the type.
///
/// ```java
/// GraphQLSchema schema = GraphQLSchema.parse(
///     "type Query { book(id: ID!): Book }\n"
///   + "type Book { id: ID! title: String }");
/// ```
public final class GraphQLSchema {
    final Map<String, SchemaType> types = new LinkedHashMap<String, SchemaType>();
    /// Types extended before any definition was read, with the line of the
    /// extension; emptied as their definitions arrive.
    final Map<String, Integer> undefinedExtensions = new LinkedHashMap<String, Integer>();
    final Map<String, String> rootTypeNames = new LinkedHashMap<String, String>();
    final Map<String, DirectiveDef> directives = new LinkedHashMap<String, DirectiveDef>();
    /// Object types implementing each interface, and the members of each union.
    final Map<String, List<SchemaType>> possibleTypes = new LinkedHashMap<String, List<SchemaType>>();

    private GraphQLSchema() {
    }

    /// Parses and checks a schema from one or more SDL documents, which are
    /// read as one: a type defined in one may be extended in another.
    public static GraphQLSchema parse(String... sdl) {
        GraphQLSchema schema = new GraphQLSchema();
        schema.builtIns();
        for (String s : sdl) {
            new Parser(s).parseSchema(schema);
        }
        for (Map.Entry<String, Integer> e : schema.undefinedExtensions.entrySet()) {
            throw new GraphQLException("Cannot extend type \"" + e.getKey()
                    + "\" because it is not defined.", e.getValue().intValue(), 0);
        }
        schema.check();
        return schema;
    }

    private void builtIns() {
        String[][] scalars = {
            {"Int", "The `Int` scalar type represents non-fractional signed whole numeric values."},
            {"Float", "The `Float` scalar type represents signed double-precision fractional values."},
            {"String", "The `String` scalar type represents textual data."},
            {"Boolean", "The `Boolean` scalar type represents `true` or `false`."},
            {"ID", "The `ID` scalar type represents a unique identifier."},
        };
        for (String[] s : scalars) {
            SchemaType t = new SchemaType(SchemaType.SCALAR, s[0]);
            t.description = s[1];
            types.put(s[0], t);
        }
        new Parser("directive @skip(if: Boolean!) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT\n"
                + "directive @include(if: Boolean!) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT\n"
                + "directive @deprecated(reason: String = \"No longer supported\") on "
                + "FIELD_DEFINITION | ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION | ENUM_VALUE\n"
                + "directive @specifiedBy(url: String!) on SCALAR\n"
                + "directive @oneOf on INPUT_OBJECT\n").parseSchema(this);
        Introspection.define(this);
    }

    private void check() {
        if (!rootTypeNames.containsKey("query") && types.containsKey("Query")) {
            rootTypeNames.put("query", "Query");
        }
        if (!rootTypeNames.containsKey("mutation") && types.containsKey("Mutation")) {
            rootTypeNames.put("mutation", "Mutation");
        }
        if (!rootTypeNames.containsKey("subscription") && types.containsKey("Subscription")) {
            rootTypeNames.put("subscription", "Subscription");
        }
        if (!rootTypeNames.containsKey("query")) {
            throw new GraphQLException("The schema has no query type: define type Query.");
        }
        for (Map.Entry<String, String> root : rootTypeNames.entrySet()) {
            SchemaType t = types.get(root.getValue());
            if (t == null || !SchemaType.OBJECT.equals(t.kind)) {
                throw new GraphQLException("The " + root.getKey() + " root type \""
                        + root.getValue() + "\" must be a defined object type.");
            }
        }
        for (SchemaType t : types.values()) {
            for (FieldDef f : t.fields.values()) {
                SchemaType ft = requireType(f.type, t.name + "." + f.name);
                if (!ft.isComposite() && !ft.isLeaf()) {
                    throw new GraphQLException("The type of " + t.name + "." + f.name
                            + " must be an output type, but " + ft.name + " is an input type.");
                }
                for (InputValueDef a : f.args.values()) {
                    checkInput(a, t.name + "." + f.name + "(" + a.name + ":)");
                }
            }
            for (InputValueDef a : t.inputFields.values()) {
                checkInput(a, t.name + "." + a.name);
            }
            if (SchemaType.UNION.equals(t.kind)) {
                List<SchemaType> members = possibleList(t.name);
                for (String m : t.unionMembers) {
                    SchemaType mt = types.get(m);
                    if (mt == null || !SchemaType.OBJECT.equals(mt.kind)) {
                        throw new GraphQLException("Union " + t.name + " can only include "
                                + "object types, but it includes " + m + ".");
                    }
                    members.add(mt);
                }
            }
            for (String i : t.interfaces) {
                SchemaType it = types.get(i);
                if (it == null || !SchemaType.INTERFACE.equals(it.kind)) {
                    throw new GraphQLException(t.name + " implements " + i
                            + ", which is not a defined interface.");
                }
                for (FieldDef inf : it.fields.values()) {
                    FieldDef own = t.fields.get(inf.name);
                    if (own == null) {
                        throw new GraphQLException("Interface field " + i + "." + inf.name
                                + " expected but " + t.name + " does not provide it.");
                    }
                }
                if (SchemaType.OBJECT.equals(t.kind)) {
                    possibleList(i).add(t);
                }
            }
        }
        for (DirectiveDef d : directives.values()) {
            for (InputValueDef a : d.args.values()) {
                checkInput(a, "@" + d.name + "(" + a.name + ":)");
            }
        }
    }

    private List<SchemaType> possibleList(String name) {
        List<SchemaType> l = possibleTypes.get(name);
        if (l == null) {
            l = new ArrayList<SchemaType>();
            possibleTypes.put(name, l);
        }
        return l;
    }

    private SchemaType requireType(TypeRef ref, String where) {
        SchemaType t = types.get(ref.namedType());
        if (t == null) {
            throw new GraphQLException("Unknown type \"" + ref.namedType() + "\" used by "
                    + where + ".");
        }
        return t;
    }

    private void checkInput(InputValueDef a, String where) {
        SchemaType t = requireType(a.type, where);
        if (!t.isInput()) {
            throw new GraphQLException("The type of " + where + " must be an input type, but "
                    + t.name + " is an output type.");
        }
    }

    SchemaType type(String name) {
        return types.get(name);
    }

    SchemaType rootType(String operation) {
        String n = rootTypeNames.get(operation);
        return n == null ? null : types.get(n);
    }

    List<SchemaType> possibleTypes(SchemaType abstractType) {
        List<SchemaType> l = possibleTypes.get(abstractType.name);
        return l == null ? Collections.<SchemaType>emptyList() : l;
    }

    boolean isPossibleType(SchemaType abstractOrObject, SchemaType object) {
        if (abstractOrObject == object) { // NOPMD CompareObjectsWithEquals - one instance per type
            return true;
        }
        return possibleTypes(abstractOrObject).contains(object);
    }

    /// Whether this schema defines `typeName.fieldName`; the build uses this to
    /// match every resolver against the schema.
    public boolean hasField(String typeName, String fieldName) {
        SchemaType t = types.get(typeName);
        return t != null && t.fields.containsKey(fieldName);
    }

    /// The names of the types this schema defines, the built-in ones included.
    public List<String> getTypeNames() {
        return new ArrayList<String>(types.keySet());
    }

    /// The name of the root type for `operation` (`query`, `mutation` or
    /// `subscription`), or null when the schema has none.
    public String getRootTypeName(String operation) {
        return rootTypeNames.get(operation);
    }
}
