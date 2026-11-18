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

/// The introspection types and their data: `__schema`, `__type(name:)` and the
/// types they return, as the specification defines them.
///
/// The data is a tree of maps built from the schema once and read by the
/// default resolver like any other map; only the fields that take
/// `includeDeprecated` have resolvers of their own, which filter it.
final class Introspection {
    static final String SDL =
            "type __Schema { description: String types: [__Type!]! queryType: __Type!\n"
            + "  mutationType: __Type subscriptionType: __Type directives: [__Directive!]! }\n"
            + "type __Type { kind: __TypeKind! name: String description: String\n"
            + "  specifiedByURL: String fields(includeDeprecated: Boolean = false): [__Field!]\n"
            + "  interfaces: [__Type!] possibleTypes: [__Type!]\n"
            + "  enumValues(includeDeprecated: Boolean = false): [__EnumValue!]\n"
            + "  inputFields(includeDeprecated: Boolean = false): [__InputValue!]\n"
            + "  ofType: __Type isOneOf: Boolean }\n"
            + "type __Field { name: String! description: String\n"
            + "  args(includeDeprecated: Boolean = false): [__InputValue!]! type: __Type!\n"
            + "  isDeprecated: Boolean! deprecationReason: String }\n"
            + "type __InputValue { name: String! description: String type: __Type!\n"
            + "  defaultValue: String isDeprecated: Boolean! deprecationReason: String }\n"
            + "type __EnumValue { name: String! description: String isDeprecated: Boolean!\n"
            + "  deprecationReason: String }\n"
            + "type __Directive { name: String! description: String isRepeatable: Boolean!\n"
            + "  locations: [__DirectiveLocation!]!\n"
            + "  args(includeDeprecated: Boolean = false): [__InputValue!]! }\n"
            + "enum __TypeKind { SCALAR OBJECT INTERFACE UNION ENUM INPUT_OBJECT LIST NON_NULL }\n"
            + "enum __DirectiveLocation { QUERY MUTATION SUBSCRIPTION FIELD FRAGMENT_DEFINITION\n"
            + "  FRAGMENT_SPREAD INLINE_FRAGMENT VARIABLE_DEFINITION SCHEMA SCALAR OBJECT\n"
            + "  FIELD_DEFINITION ARGUMENT_DEFINITION INTERFACE UNION ENUM ENUM_VALUE\n"
            + "  INPUT_OBJECT INPUT_FIELD_DEFINITION }\n";

    private final GraphQLSchema schema;
    private final Map<String, Map<String, Object>> named = new LinkedHashMap<String, Map<String, Object>>();
    private Map<String, Object> root;

    private Introspection(GraphQLSchema schema) {
        this.schema = schema;
    }

    static void define(GraphQLSchema schema) {
        new Parser(SDL).parseSchema(schema);
    }

    /// The `__schema` value of `schema`.
    static Map<String, Object> of(GraphQLSchema schema) {
        Introspection i = new Introspection(schema);
        i.build();
        return i.root;
    }

    private void build() {
        for (SchemaType t : schema.types.values()) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            named.put(t.name, m);
        }
        for (SchemaType t : schema.types.values()) {
            fill(t, named.get(t.name));
        }
        root = new LinkedHashMap<String, Object>();
        root.put("description", null);
        List<Object> all = new ArrayList<Object>(named.values());
        root.put("types", all);
        root.put("queryType", named.get(schema.getRootTypeName("query")));
        String m = schema.getRootTypeName("mutation");
        root.put("mutationType", m == null ? null : named.get(m));
        String s = schema.getRootTypeName("subscription");
        root.put("subscriptionType", s == null ? null : named.get(s));
        List<Object> directives = new ArrayList<Object>();
        for (DirectiveDef d : schema.directives.values()) {
            Map<String, Object> dm = new LinkedHashMap<String, Object>();
            dm.put("name", d.name);
            dm.put("description", d.description);
            dm.put("isRepeatable", Boolean.valueOf(d.repeatable));
            dm.put("locations", new ArrayList<Object>(d.locations));
            dm.put("args", inputValues(d.args));
            directives.add(dm);
        }
        root.put("directives", directives);
    }

    /// The `__type(name:)` value, or null.
    static Map<String, Object> type(Map<String, Object> schemaValue, String name) {
        Object types = schemaValue.get("types");
        if (types instanceof List) {
            for (Object t : (List) types) {
                if (t instanceof Map && name.equals(((Map) t).get("name"))) {
                    return castMap(t);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object o) {
        return (Map<String, Object>) o;
    }

    private void fill(SchemaType t, Map<String, Object> m) {
        m.put("kind", t.kind);
        m.put("name", t.name);
        m.put("description", t.description);
        m.put("specifiedByURL", specifiedBy(t));
        m.put("ofType", null);
        m.put("isOneOf", SchemaType.INPUT_OBJECT.equals(t.kind) ? Boolean.valueOf(oneOf(t)) : null);
        if (t.isComposite() && !SchemaType.UNION.equals(t.kind)) {
            List<Object> fields = new ArrayList<Object>();
            for (FieldDef f : t.fields.values()) {
                Map<String, Object> fm = new LinkedHashMap<String, Object>();
                fm.put("name", f.name);
                fm.put("description", f.description);
                fm.put("args", inputValues(f.args));
                fm.put("type", ref(f.type));
                fm.put("isDeprecated", Boolean.valueOf(f.deprecationReason != null));
                fm.put("deprecationReason", f.deprecationReason);
                fields.add(fm);
            }
            m.put("fields", fields);
            List<Object> interfaces = new ArrayList<Object>();
            for (String i : t.interfaces) {
                interfaces.add(named.get(i));
            }
            m.put("interfaces", interfaces);
        } else {
            m.put("fields", null);
            m.put("interfaces", null);
        }
        if (t.isAbstract()) {
            List<Object> possible = new ArrayList<Object>();
            for (SchemaType p : schema.possibleTypes(t)) {
                possible.add(named.get(p.name));
            }
            m.put("possibleTypes", possible);
        } else {
            m.put("possibleTypes", null);
        }
        if (SchemaType.ENUM.equals(t.kind)) {
            List<Object> values = new ArrayList<Object>();
            for (EnumValueDef v : t.enumValues.values()) {
                Map<String, Object> vm = new LinkedHashMap<String, Object>();
                vm.put("name", v.name);
                vm.put("description", v.description);
                vm.put("isDeprecated", Boolean.valueOf(v.deprecationReason != null));
                vm.put("deprecationReason", v.deprecationReason);
                values.add(vm);
            }
            m.put("enumValues", values);
        } else {
            m.put("enumValues", null);
        }
        m.put("inputFields", SchemaType.INPUT_OBJECT.equals(t.kind)
                ? inputValues(t.inputFields) : null);
    }

    private static String specifiedBy(SchemaType t) {
        if (t.directives != null) {
            for (Parser.Directive d : t.directives) {
                if ("specifiedBy".equals(d.name) && d.arguments.get("url") instanceof String) {
                    return (String) d.arguments.get("url");
                }
            }
        }
        return null;
    }

    private static boolean oneOf(SchemaType t) {
        if (t.directives != null) {
            for (Parser.Directive d : t.directives) {
                if ("oneOf".equals(d.name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Object> inputValues(Map<String, InputValueDef> defs) {
        List<Object> out = new ArrayList<Object>();
        for (InputValueDef a : defs.values()) {
            Map<String, Object> am = new LinkedHashMap<String, Object>();
            am.put("name", a.name);
            am.put("description", a.description);
            am.put("type", ref(a.type));
            am.put("defaultValue", a.hasDefault ? printValue(a.defaultValue) : null);
            am.put("isDeprecated", Boolean.valueOf(a.deprecationReason != null));
            am.put("deprecationReason", a.deprecationReason);
            out.add(am);
        }
        return out;
    }

    private Map<String, Object> ref(TypeRef r) {
        if (r.kind == TypeRef.NAMED) {
            return named.get(r.name);
        }
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("kind", r.kind == TypeRef.LIST ? "LIST" : "NON_NULL");
        m.put("name", null);
        m.put("description", null);
        m.put("specifiedByURL", null);
        m.put("fields", null);
        m.put("interfaces", null);
        m.put("possibleTypes", null);
        m.put("enumValues", null);
        m.put("inputFields", null);
        m.put("isOneOf", null);
        m.put("ofType", ref(r.ofType));
        return m;
    }

    /// A value as GraphQL source text, for `defaultValue`.
    static String printValue(Object v) {
        StringBuilder sb = new StringBuilder();
        print(v, sb);
        return sb.toString();
    }

    private static void print(Object v, StringBuilder sb) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            sb.append('"');
            String s = (String) v;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '"' || c == '\\') {
                    sb.append('\\').append(c);
                } else if (c == '\n') {
                    sb.append("\\n");
                } else if (c < 0x20) {
                    String hex = Integer.toHexString(c);
                    sb.append("\\u");
                    for (int p = hex.length(); p < 4; p++) {
                        sb.append('0');
                    }
                    sb.append(hex);
                } else {
                    sb.append(c);
                }
            }
            sb.append('"');
        } else if (v instanceof List) {
            sb.append('[');
            boolean first = true;
            for (Object o : (List) v) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                print(o, sb);
            }
            sb.append(']');
        } else if (v instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Object o : ((Map) v).entrySet()) {
                Map.Entry e = (Map.Entry) o;
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(e.getKey()).append(": ");
                print(e.getValue(), sb);
            }
            sb.append('}');
        } else {
            sb.append(v);
        }
    }

    /// Whether `includeDeprecated` asked for `item`, an introspection map with
    /// an `isDeprecated` entry.
    static List<Object> filterDeprecated(Object list, Object includeDeprecated) {
        if (!(list instanceof List)) {
            return null;
        }
        List<Object> out = new ArrayList<Object>();
        boolean all = Boolean.TRUE.equals(includeDeprecated);
        for (Object o : (List) list) {
            if (all || !(o instanceof Map) || !Boolean.TRUE.equals(((Map) o).get("isDeprecated"))) {
                out.add(o);
            }
        }
        return out;
    }
}
