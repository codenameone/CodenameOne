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
package javafx.scene.layout;

/// How eagerly a node, a row or a column takes space its container has
/// left over.
public enum Priority {
    /// Takes extra space, sharing it with the others that always do.
    ALWAYS,
    /// Takes extra space only when nothing asks for it [#ALWAYS].
    SOMETIMES,
    /// Keeps its preferred size.
    NEVER;

    /// Returns the more eager of two priorities.
    public static Priority max(Priority a, Priority b) {
        if (a == ALWAYS || b == ALWAYS) {
            return ALWAYS;
        } else if (a == SOMETIMES || b == SOMETIMES) {
            return SOMETIMES;
        }
        return NEVER;
    }

    /// Returns the less eager of two priorities.
    public static Priority min(Priority a, Priority b) {
        if (a == NEVER || b == NEVER) {
            return NEVER;
        } else if (a == SOMETIMES || b == SOMETIMES) {
            return SOMETIMES;
        }
        return ALWAYS;
    }
}
