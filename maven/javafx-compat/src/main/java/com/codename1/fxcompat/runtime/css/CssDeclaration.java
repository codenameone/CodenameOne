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
package com.codename1.fxcompat.runtime.css;

/// One declaration of a rule or of an inline style: a property, its parsed
/// value, and whether it was marked `!important`.
public final class CssDeclaration {

    private final String name;
    private final int kind;
    private final CssValue value;
    private final boolean important;

    /// Creates a declaration. `kind` is the kind of the property in
    /// [CssProperties], [CssProperties#LOOKUP_DEFINITION] for a name that
    /// defines a looked-up colour.
    public CssDeclaration(String name, int kind, CssValue value, boolean important) {
        this.name = name;
        this.kind = kind;
        this.value = value;
        this.important = important;
    }

    /// The property name, in lower case.
    public String name() {
        return name;
    }

    /// The kind of the property; see [CssProperties].
    public int kind() {
        return kind;
    }

    /// The parsed value.
    public CssValue value() {
        return value;
    }

    /// Whether the declaration was marked `!important`.
    public boolean important() {
        return important;
    }

    @Override
    public String toString() {
        return name + ": " + value + (important ? " !important" : "");
    }
}
