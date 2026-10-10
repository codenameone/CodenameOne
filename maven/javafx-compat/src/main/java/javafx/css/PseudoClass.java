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
package javafx.css;

import java.util.HashMap;

/// A state a node can be in that a style sheet can select on, such as
/// `hover` or `focused`. There is one instance per name.
public abstract class PseudoClass {

    private static final HashMap<String, PseudoClass> KNOWN = new HashMap<String, PseudoClass>();

    /// Creates a pseudo-class; applications use [#getPseudoClass(String)].
    public PseudoClass() {
    }

    /// Returns the pseudo-class with a name, creating it the first time.
    public static PseudoClass getPseudoClass(String pseudoClass) {
        if (pseudoClass == null || pseudoClass.trim().length() == 0) {
            throw new IllegalArgumentException("pseudoClass cannot be null or empty String");
        }
        final String name = pseudoClass.trim();
        PseudoClass known = KNOWN.get(name);
        if (known == null) {
            known = new PseudoClass() {
                @Override
                public String getPseudoClassName() {
                    return name;
                }

                @Override
                public String toString() {
                    return name;
                }
            };
            KNOWN.put(name, known);
        }
        return known;
    }

    /// Returns the name, as written after the colon in a selector.
    public abstract String getPseudoClassName();
}
