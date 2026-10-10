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

/// How heavy the strokes of a font are, on the 100 to 900 scale.
public enum FontWeight {
    /// Weight 100.
    THIN(100, "Thin"),
    /// Weight 200.
    EXTRA_LIGHT(200, "Extra Light", "Ultra Light"),
    /// Weight 300.
    LIGHT(300, "Light"),
    /// Weight 400, the ordinary weight.
    NORMAL(400, "Normal", "Regular"),
    /// Weight 500.
    MEDIUM(500, "Medium"),
    /// Weight 600.
    SEMI_BOLD(600, "Semi Bold", "Demi Bold"),
    /// Weight 700.
    BOLD(700, "Bold"),
    /// Weight 800.
    EXTRA_BOLD(800, "Extra Bold", "Ultra Bold"),
    /// Weight 900.
    BLACK(900, "Black", "Heavy");

    private final int weight;
    private final String[] names;

    FontWeight(int weight, String... names) {
        this.weight = weight;
        this.names = names;
    }

    /// Returns the weight on the 100 to 900 scale.
    public int getWeight() {
        return weight;
    }

    /// Returns the weight with a name, compared without regard to case, or
    /// `null`.
    public static FontWeight findByName(String name) {
        if (name == null) {
            return null;
        }
        FontWeight[] all = values();
        for (int i = 0; i < all.length; i++) {
            for (int j = 0; j < all[i].names.length; j++) {
                if (all[i].names[j].equalsIgnoreCase(name)) {
                    return all[i];
                }
            }
        }
        return null;
    }

    /// Returns the weight nearest to a number on the 100 to 900 scale.
    public static FontWeight findByWeight(int weight) {
        if (weight <= 150) {
            return THIN;
        } else if (weight <= 250) {
            return EXTRA_LIGHT;
        } else if (weight < 350) {
            return LIGHT;
        } else if (weight <= 450) {
            return NORMAL;
        } else if (weight <= 550) {
            return MEDIUM;
        } else if (weight < 650) {
            return SEMI_BOLD;
        } else if (weight <= 750) {
            return BOLD;
        } else if (weight <= 850) {
            return EXTRA_BOLD;
        }
        return BLACK;
    }
}
