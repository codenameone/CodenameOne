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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.FieldInfo;

/// Shared field-type classifier used by every annotation processor that has
/// to walk POJO / Property mixed types -- the JSON/XML mapper, the component
/// binder, and the ORM dao. Keeping the rules in one place means a new type
/// category (added later, say `OptionalProperty`) gets honoured by every
/// generator without three out-of-sync forks.
///
/// The classifier never reflects: it only looks at the field descriptor and
/// the optional generic signature recorded in the class file.
public final class PropertyTypeKind {

    /// What flavour of value the field exposes.
    public enum Kind {
        STRING, INT, LONG, SHORT, BYTE, CHAR, DOUBLE, FLOAT, BOOLEAN,
        DATE, BYTE_ARRAY, ENUM,
        /// A `com.codename1.properties.Property<T, ?>` field. `elementBinaryName`
        /// holds the dot-form of T.
        PROPERTY,
        /// A `com.codename1.properties.ListProperty<T, ?>` field. `elementBinaryName`
        /// holds the dot-form of T.
        LIST_PROPERTY,
        /// `java.util.List<T>` (or `ArrayList<T>`).
        LIST,
        /// Another @Mapped / @Entity / @Bindable class -- nested object.
        REFERENCE,
        /// Anything else. The processor must emit a clear validation error.
        UNSUPPORTED
    }

    public final Kind kind;
    public final String binaryName;
    public final String elementBinaryName;

    private PropertyTypeKind(Kind kind, String binaryName, String elementBinaryName) {
        this.kind = kind;
        this.binaryName = binaryName;
        this.elementBinaryName = elementBinaryName;
    }

    public static PropertyTypeKind of(FieldInfo field) {
        String desc = field.getDescriptor();
        if (desc == null || desc.length() == 0) {
            return new PropertyTypeKind(Kind.UNSUPPORTED, "?", null);
        }
        // Primitives.
        if (desc.length() == 1) {
            switch (desc.charAt(0)) {
                case 'I': return new PropertyTypeKind(Kind.INT, "int", null);
                case 'J': return new PropertyTypeKind(Kind.LONG, "long", null);
                case 'S': return new PropertyTypeKind(Kind.SHORT, "short", null);
                case 'B': return new PropertyTypeKind(Kind.BYTE, "byte", null);
                case 'C': return new PropertyTypeKind(Kind.CHAR, "char", null);
                case 'D': return new PropertyTypeKind(Kind.DOUBLE, "double", null);
                case 'F': return new PropertyTypeKind(Kind.FLOAT, "float", null);
                case 'Z': return new PropertyTypeKind(Kind.BOOLEAN, "boolean", null);
                default:  return new PropertyTypeKind(Kind.UNSUPPORTED, "?", null);
            }
        }
        if ("[B".equals(desc)) {
            return new PropertyTypeKind(Kind.BYTE_ARRAY, "byte[]", null);
        }
        if (desc.startsWith("L") && desc.endsWith(";")) {
            String binary = desc.substring(1, desc.length() - 1).replace('/', '.');
            // Boxed scalars.
            if ("java.lang.String".equals(binary)) {
                return new PropertyTypeKind(Kind.STRING, binary, null);
            }
            if ("java.lang.Integer".equals(binary)) {
                return new PropertyTypeKind(Kind.INT, binary, null);
            }
            if ("java.lang.Long".equals(binary)) {
                return new PropertyTypeKind(Kind.LONG, binary, null);
            }
            if ("java.lang.Short".equals(binary)) {
                return new PropertyTypeKind(Kind.SHORT, binary, null);
            }
            if ("java.lang.Byte".equals(binary)) {
                return new PropertyTypeKind(Kind.BYTE, binary, null);
            }
            if ("java.lang.Character".equals(binary)) {
                return new PropertyTypeKind(Kind.CHAR, binary, null);
            }
            if ("java.lang.Double".equals(binary)) {
                return new PropertyTypeKind(Kind.DOUBLE, binary, null);
            }
            if ("java.lang.Float".equals(binary)) {
                return new PropertyTypeKind(Kind.FLOAT, binary, null);
            }
            if ("java.lang.Boolean".equals(binary)) {
                return new PropertyTypeKind(Kind.BOOLEAN, binary, null);
            }
            if ("java.util.Date".equals(binary)) {
                return new PropertyTypeKind(Kind.DATE, binary, null);
            }
            // Property<T, K> / ListProperty<T, K>.
            if ("com.codename1.properties.Property".equals(binary)
                    || "com.codename1.properties.StringProperty".equals(binary)
                    || "com.codename1.properties.IntProperty".equals(binary)
                    || "com.codename1.properties.LongProperty".equals(binary)
                    || "com.codename1.properties.DoubleProperty".equals(binary)
                    || "com.codename1.properties.FloatProperty".equals(binary)
                    || "com.codename1.properties.BooleanProperty".equals(binary)
                    || "com.codename1.properties.ByteProperty".equals(binary)
                    || "com.codename1.properties.CharProperty".equals(binary)) {
                String elem = firstGenericArg(field.getSignature());
                if (elem == null) {
                    // Pre-erasure-only inference for typed subclasses:
                    if ("com.codename1.properties.StringProperty".equals(binary)) elem = "java.lang.String";
                    else if ("com.codename1.properties.IntProperty".equals(binary)) elem = "java.lang.Integer";
                    else if ("com.codename1.properties.LongProperty".equals(binary)) elem = "java.lang.Long";
                    else if ("com.codename1.properties.DoubleProperty".equals(binary)) elem = "java.lang.Double";
                    else if ("com.codename1.properties.FloatProperty".equals(binary)) elem = "java.lang.Float";
                    else if ("com.codename1.properties.BooleanProperty".equals(binary)) elem = "java.lang.Boolean";
                    else if ("com.codename1.properties.ByteProperty".equals(binary)) elem = "java.lang.Byte";
                    else if ("com.codename1.properties.CharProperty".equals(binary)) elem = "java.lang.Character";
                    else elem = "java.lang.String";
                }
                return new PropertyTypeKind(Kind.PROPERTY, binary, elem);
            }
            if ("com.codename1.properties.ListProperty".equals(binary)
                    || "com.codename1.properties.SetProperty".equals(binary)) {
                String elem = firstGenericArg(field.getSignature());
                if (elem == null) elem = "java.lang.String";
                return new PropertyTypeKind(Kind.LIST_PROPERTY, binary, elem);
            }
            if ("java.util.List".equals(binary) || "java.util.ArrayList".equals(binary)) {
                String elem = firstGenericArg(field.getSignature());
                if (elem == null) elem = "java.lang.String";
                return new PropertyTypeKind(Kind.LIST, binary, elem);
            }
            // Anything else: treat as a reference to another mapped type.
            // The caller still validates that the referenced class actually
            // carries @Mapped / @Entity / @Bindable.
            return new PropertyTypeKind(Kind.REFERENCE, binary, null);
        }
        return new PropertyTypeKind(Kind.UNSUPPORTED, "?", null);
    }

    /// Extracts the first type argument from a generic signature string. The
    /// signature for `Property<String, User>` is
    /// `Lcom/codename1/properties/Property<Ljava/lang/String;Lpkg/User;>;`.
    /// Returns the dot-form of the first type or null when the signature is
    /// missing / malformed.
    static String firstGenericArg(String signature) {
        if (signature == null) return null;
        int lt = signature.indexOf('<');
        if (lt < 0) return null;
        // Scan to the first balanced `;` at depth 1 (we entered `<`).
        int depth = 1;
        int i = lt + 1;
        if (i >= signature.length()) return null;
        char first = signature.charAt(i);
        if (first != 'L') {
            // Wildcards, type variables (T...), primitives -- bail out; the
            // caller picks a sensible default.
            return null;
        }
        int start = i + 1;
        for (i = start; i < signature.length(); i++) {
            char c = signature.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ';' && depth == 1) {
                return signature.substring(start, i).replace('/', '.');
            }
        }
        return null;
    }

    /// True when the value can be emitted directly into JSON / SQL with no
    /// further mapping (it has a printable form and a primitive-typed parser
    /// path).
    public boolean isScalar() {
        switch (kind) {
            case STRING: case INT: case LONG: case SHORT: case BYTE:
            case CHAR: case DOUBLE: case FLOAT: case BOOLEAN:
            case DATE: case BYTE_ARRAY: case ENUM:
                return true;
            default:
                return false;
        }
    }
}
