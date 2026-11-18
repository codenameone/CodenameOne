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

/// A type as written in a field, argument or variable: a named type, or a
/// list or non-null wrapper around one.
final class TypeRef {
    static final int NAMED = 0;
    static final int LIST = 1;
    static final int NON_NULL = 2;

    final int kind;
    final String name;
    final TypeRef ofType;

    private TypeRef(int kind, String name, TypeRef ofType) {
        this.kind = kind;
        this.name = name;
        this.ofType = ofType;
    }

    static TypeRef named(String name) {
        return new TypeRef(NAMED, name, null);
    }

    static TypeRef list(TypeRef of) {
        return new TypeRef(LIST, null, of);
    }

    static TypeRef nonNull(TypeRef of) {
        return new TypeRef(NON_NULL, null, of);
    }

    boolean isNonNull() {
        return kind == NON_NULL;
    }

    /// The type without a non-null wrapper.
    TypeRef nullable() {
        return kind == NON_NULL ? ofType : this;
    }

    /// The named type at the bottom of the wrappers.
    String namedType() {
        TypeRef t = this;
        while (t.kind != NAMED) {
            t = t.ofType;
        }
        return t.name;
    }

    /// Whether a value of `this` (a variable's type) may be used where `expected`
    /// (an argument's type) is wanted -- the spec's AreTypesCompatible.
    boolean compatibleWith(TypeRef expected) {
        if (expected.kind == NON_NULL) {
            return kind == NON_NULL && ofType.compatibleWith(expected.ofType);
        }
        if (kind == NON_NULL) {
            return ofType.compatibleWith(expected);
        }
        if (expected.kind == LIST) {
            return kind == LIST && ofType.compatibleWith(expected.ofType);
        }
        return kind == NAMED && name.equals(expected.name);
    }

    @Override
    public String toString() {
        switch (kind) {
            case LIST: return "[" + ofType + "]";
            case NON_NULL: return ofType + "!";
            default: return name;
        }
    }
}
