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

/// Input and result coercion, as the specification's "Coercion" sections
/// define it for each type.
///
/// Every failure is a [GraphQLException] whose message says what was wrong
/// and where, as `Variable "$id" got invalid value ...`.
final class Values {
    private final GraphQLSchema schema;
    private final Map<String, GraphQLScalar> scalars;

    Values(GraphQLSchema schema, Map<String, GraphQLScalar> scalars) {
        this.schema = schema;
        this.scalars = scalars;
    }

    // ------------------------------------------------------- input coercion

    /// Coerces a value from the variables JSON. `where` describes it for errors.
    Object coerceInput(Object value, TypeRef type, String where) {
        if (type.kind == TypeRef.NON_NULL) {
            if (value == null) {
                throw new GraphQLException(where + ": expected non-nullable type \"" + type
                        + "\" not to be null.");
            }
            return coerceInput(value, type.ofType, where);
        }
        if (value == null) {
            return null;
        }
        if (type.kind == TypeRef.LIST) {
            List<Object> out = new ArrayList<Object>();
            if (value instanceof List) {
                int i = 0;
                for (Object o : (List) value) {
                    out.add(coerceInput(o, type.ofType, where + "[" + i + "]"));
                    i++;
                }
            } else {
                out.add(coerceInput(value, type.ofType, where));
            }
            return out;
        }
        SchemaType t = schema.type(type.name);
        if (SchemaType.INPUT_OBJECT.equals(t.kind)) {
            if (!(value instanceof Map)) {
                throw new GraphQLException(where + ": expected type \"" + t.name
                        + "\" to be an object.");
            }
            Map in = (Map) value;
            for (Object k : in.keySet()) {
                if (!t.inputFields.containsKey(String.valueOf(k))) {
                    throw new GraphQLException(where + ": field \"" + k
                            + "\" is not defined by type \"" + t.name + "\".");
                }
            }
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            for (InputValueDef f : t.inputFields.values()) {
                String fw = where + "." + f.name;
                if (in.containsKey(f.name)) {
                    out.put(f.name, coerceInput(in.get(f.name), f.type, fw));
                } else if (f.hasDefault) {
                    out.put(f.name, coerceLiteral(f.defaultValue, f.type, null, fw));
                } else if (f.type.isNonNull()) {
                    throw new GraphQLException(fw + ": field of required type \"" + f.type
                            + "\" was not provided.");
                }
            }
            checkOneOf(t, out, where);
            return out;
        }
        return coerceLeaf(value, t, where, false);
    }

    private static void checkOneOf(SchemaType t, Map<String, Object> out, String where) {
        if (t.directives == null) {
            return;
        }
        for (Parser.Directive d : t.directives) {
            if ("oneOf".equals(d.name)) {
                if (out.size() != 1 || out.values().iterator().next() == null) {
                    throw new GraphQLException(where + ": exactly one field of OneOf input "
                            + "type \"" + t.name + "\" must be given and not null.");
                }
            }
        }
    }

    /// Coerces a literal from the document, resolving variables from
    /// `variables` (already coerced). With `variables` null a variable is an
    /// error, as in a default value.
    Object coerceLiteral(Object value, TypeRef type, Map<String, Object> variables,
                         String where) {
        if (value instanceof Parser.Variable) {
            String n = ((Parser.Variable) value).name;
            if (variables == null || !variables.containsKey(n)) {
                if (type.isNonNull()) {
                    throw new GraphQLException(where + ": variable \"$" + n
                            + "\" of required type \"" + type + "\" was not provided.");
                }
                return null;
            }
            Object v = variables.get(n);
            if (v == null && type.isNonNull()) {
                throw new GraphQLException(where + ": variable \"$" + n
                        + "\" must not be null.");
            }
            return v;
        }
        if (type.kind == TypeRef.NON_NULL) {
            if (value == null) {
                throw new GraphQLException(where + ": expected value of type \"" + type
                        + "\", found null.");
            }
            return coerceLiteral(value, type.ofType, variables, where);
        }
        if (value == null) {
            return null;
        }
        if (type.kind == TypeRef.LIST) {
            List<Object> out = new ArrayList<Object>();
            if (value instanceof List) {
                for (Object o : (List) value) {
                    out.add(coerceLiteral(o, type.ofType, variables, where));
                }
            } else {
                out.add(coerceLiteral(value, type.ofType, variables, where));
            }
            return out;
        }
        SchemaType t = schema.type(type.name);
        if (SchemaType.INPUT_OBJECT.equals(t.kind)) {
            if (!(value instanceof Map)) {
                throw new GraphQLException(where + ": expected value of type \"" + t.name
                        + "\", found " + Introspection.printValue(literalToPlain(value)) + ".");
            }
            Map in = (Map) value;
            for (Object k : in.keySet()) {
                if (!t.inputFields.containsKey(String.valueOf(k))) {
                    throw new GraphQLException(where + ": field \"" + k
                            + "\" is not defined by type \"" + t.name + "\".");
                }
            }
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            for (InputValueDef f : t.inputFields.values()) {
                String fw = where + "." + f.name;
                Object fv = in.get(f.name);
                boolean given = in.containsKey(f.name);
                if (given && fv instanceof Parser.Variable
                        && (variables == null
                        || !variables.containsKey(((Parser.Variable) fv).name))) {
                    given = false;
                }
                if (given) {
                    out.put(f.name, coerceLiteral(fv, f.type, variables, fw));
                } else if (f.hasDefault) {
                    out.put(f.name, coerceLiteral(f.defaultValue, f.type, null, fw));
                } else if (f.type.isNonNull()) {
                    throw new GraphQLException(fw + ": field of required type \"" + f.type
                            + "\" was not provided.");
                }
            }
            checkOneOf(t, out, where);
            return out;
        }
        return coerceLeaf(value, t, where, true);
    }

    private Object coerceLeaf(Object value, SchemaType t, String where, boolean literal) {
        String found = literal ? Introspection.printValue(literalToPlain(value))
                : String.valueOf(value);
        if (SchemaType.ENUM.equals(t.kind)) {
            String name = null;
            if (literal ? value instanceof Parser.EnumLiteral : value instanceof String) {
                name = value.toString();
            }
            if (name == null || !t.enumValues.containsKey(name)) {
                throw new GraphQLException(where + ": value " + found
                        + " does not exist in \"" + t.name + "\" enum.");
            }
            return name;
        }
        if (value instanceof Parser.EnumLiteral || value instanceof List
                || value instanceof Map) {
            if (scalars.containsKey(t.name) || !isBuiltIn(t.name)) {
                return custom(t, literalToPlain(value), where);
            }
            throw invalid(t, found, where);
        }
        String n = t.name;
        if ("Int".equals(n)) {
            if (value instanceof Long || value instanceof Integer || isIntegral(value)) {
                double d = ((Number) value).doubleValue();
                if (d > Integer.MAX_VALUE || d < Integer.MIN_VALUE) {
                    throw new GraphQLException(where + ": Int cannot represent non 32-bit "
                            + "signed integer value: " + found);
                }
                return Integer.valueOf(((Number) value).intValue());
            }
            throw invalid(t, found, where);
        }
        if ("Float".equals(n)) {
            if (value instanceof Number) {
                return Double.valueOf(((Number) value).doubleValue());
            }
            throw invalid(t, found, where);
        }
        if ("String".equals(n)) {
            if (value instanceof String) {
                return value;
            }
            throw invalid(t, found, where);
        }
        if ("Boolean".equals(n)) {
            if (value instanceof Boolean) {
                return value;
            }
            throw invalid(t, found, where);
        }
        if ("ID".equals(n)) {
            if (value instanceof String) {
                return value;
            }
            if (value instanceof Long || value instanceof Integer || isIntegral(value)) {
                return String.valueOf(((Number) value).longValue());
            }
            throw invalid(t, found, where);
        }
        return custom(t, value, where);
    }

    private Object custom(SchemaType t, Object value, String where) {
        GraphQLScalar s = scalars.get(t.name);
        if (s == null) {
            return value;
        }
        try {
            return s.parseValue(value);
        } catch (GraphQLException e) {
            throw new GraphQLException(where + ": " + e.getMessage()); // NOPMD PreserveStackTrace - the message is the whole report
        }
    }

    private static boolean isBuiltIn(String n) {
        return "Int".equals(n) || "Float".equals(n) || "String".equals(n)
                || "Boolean".equals(n) || "ID".equals(n);
    }

    private static boolean isIntegral(Object v) {
        if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            return d == Math.floor(d) && !Double.isInfinite(d);
        }
        return v instanceof Short || v instanceof Byte;
    }

    private static GraphQLException invalid(SchemaType t, String found, String where) {
        return new GraphQLException(where + ": " + t.name + " cannot represent value: "
                + found);
    }

    /// A literal with its enum values and variables as plain strings, for
    /// messages and custom scalars.
    static Object literalToPlain(Object v) {
        if (v instanceof Parser.EnumLiteral) {
            return ((Parser.EnumLiteral) v).name;
        }
        if (v instanceof Parser.Variable) {
            return "$" + ((Parser.Variable) v).name;
        }
        if (v instanceof List) {
            List<Object> out = new ArrayList<Object>();
            for (Object o : (List) v) {
                out.add(literalToPlain(o));
            }
            return out;
        }
        if (v instanceof Map) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            for (Object o : ((Map) v).entrySet()) {
                Map.Entry e = (Map.Entry) o;
                out.put(String.valueOf(e.getKey()), literalToPlain(e.getValue()));
            }
            return out;
        }
        return v;
    }

    // ------------------------------------------------------ result coercion

    /// The value written for a leaf field. Throws a [GraphQLException] for a
    /// value the type cannot represent.
    Object serialize(SchemaType t, Object value) {
        if (SchemaType.ENUM.equals(t.kind)) {
            String name = value instanceof Enum ? ((Enum) value).name() : String.valueOf(value);
            if (!t.enumValues.containsKey(name)) {
                throw new GraphQLException("Enum \"" + t.name + "\" cannot represent value: "
                        + name);
            }
            return name;
        }
        String n = t.name;
        if ("Int".equals(n)) {
            if (value instanceof Boolean) {
                return Integer.valueOf(((Boolean) value).booleanValue() ? 1 : 0);
            }
            if (value instanceof Number && (isIntegral(value) || value instanceof Long
                    || value instanceof Integer)) {
                double d = ((Number) value).doubleValue();
                if (d <= Integer.MAX_VALUE && d >= Integer.MIN_VALUE) {
                    return Integer.valueOf(((Number) value).intValue());
                }
            }
            throw new GraphQLException("Int cannot represent value: " + value);
        }
        if ("Float".equals(n)) {
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                if (!Double.isNaN(d) && !Double.isInfinite(d)) {
                    return Double.valueOf(d);
                }
            }
            if (value instanceof Boolean) {
                return Double.valueOf(((Boolean) value).booleanValue() ? 1 : 0);
            }
            throw new GraphQLException("Float cannot represent value: " + value);
        }
        if ("String".equals(n) || "ID".equals(n)) {
            if (value instanceof String || value instanceof Number || value instanceof Boolean
                    || value instanceof Character || value instanceof Enum) {
                if (value instanceof Double || value instanceof Float) {
                    if (isIntegral(value) && "ID".equals(n)) {
                        return String.valueOf(((Number) value).longValue());
                    }
                }
                return value instanceof Enum ? ((Enum) value).name() : String.valueOf(value);
            }
            throw new GraphQLException(n + " cannot represent value: " + value);
        }
        if ("Boolean".equals(n)) {
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof Number && isIntegral(value) || value instanceof Long
                    || value instanceof Integer) {
                return Boolean.valueOf(((Number) value).longValue() != 0);
            }
            throw new GraphQLException("Boolean cannot represent value: " + value);
        }
        GraphQLScalar s = scalars.get(n);
        return s == null ? value : s.serialize(value);
    }
}
