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
package javafx.scene.text;

/// Whether a font is upright or slanted.
public enum FontPosture {
    /// Upright.
    REGULAR("", "regular"),
    /// Slanted.
    ITALIC("italic");

    private final String[] names;

    FontPosture(String... names) {
        this.names = names;
    }

    /// Returns the posture with a name, compared without regard to case,
    /// or `null`.
    public static FontPosture findByName(String name) {
        if (name == null) {
            return null;
        }
        FontPosture[] all = values();
        for (int i = 0; i < all.length; i++) {
            for (int j = 0; j < all[i].names.length; j++) {
                if (all[i].names[j].equalsIgnoreCase(name)) {
                    return all[i];
                }
            }
        }
        return null;
    }
}
