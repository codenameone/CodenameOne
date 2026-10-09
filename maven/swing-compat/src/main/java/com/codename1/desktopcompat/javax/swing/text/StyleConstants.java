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
package com.codename1.desktopcompat.javax.swing.text;

/// The well known attribute names of styled text. The documents of this
/// layer carry plain text, so the two an element really has are
/// [#NameAttribute] and [#ResolveAttribute]; they are the same objects as
/// the constants of [AttributeSet].
public class StyleConstants {

    /// The name of an element that stands for a component.
    public static final String ComponentElementName = "component";

    /// The name of an element that stands for an icon.
    public static final String IconElementName = "icon";

    /// The attribute that holds an element's name.
    public static final Object NameAttribute = AttributeSet.NameAttribute;

    /// The attribute that holds the set attributes are inherited from.
    public static final Object ResolveAttribute = AttributeSet.ResolveAttribute;

    private final String representation;

    StyleConstants(String representation) {
        this.representation = representation;
    }

    @Override
    public String toString() {
        return representation;
    }
}
