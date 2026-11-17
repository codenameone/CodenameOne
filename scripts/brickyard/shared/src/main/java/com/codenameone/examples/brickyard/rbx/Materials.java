/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codenameone.examples.brickyard.rbx;

/// Physical constants of the built-in materials.
final class Materials {
    private Materials() {
    }

    /// Density in the engine's mass units per cubic stud.
    static double density(Instance p) {
        Object m = p.prop("Material");
        String n = m instanceof Enums.EnumItem ? ((Enums.EnumItem) m).name : "Plastic";
        switch (n) {
            case "Wood":
            case "WoodPlanks":
                return 0.35;
            case "Metal":
            case "DiamondPlate":
            case "CorrodedMetal":
                return 7.85;
            case "Concrete":
            case "Brick":
            case "Cobblestone":
            case "Pavement":
                return 2.4;
            case "Granite":
            case "Marble":
            case "Slate":
            case "Basalt":
            case "Rock":
                return 2.7;
            case "Ice":
            case "Glacier":
                return 0.92;
            case "Glass":
                return 2.4;
            case "Foil":
                return 2.7;
            case "Fabric":
            case "Carpet":
                return 0.7;
            case "Grass":
            case "Ground":
            case "Mud":
                return 0.9;
            case "Sand":
                return 1.6;
            case "ForceField":
            case "Neon":
            case "SmoothPlastic":
            case "Plastic":
            default:
                return 0.7;
        }
    }

    /// Bounciness, 0..1.
    static double elasticity(Instance p) {
        Object m = p.prop("Material");
        String n = m instanceof Enums.EnumItem ? ((Enums.EnumItem) m).name : "Plastic";
        return "Rubber".equals(n) ? 0.8 : 0.3;
    }

    /// Friction coefficient.
    static double friction(Instance p) {
        Object m = p.prop("Material");
        String n = m instanceof Enums.EnumItem ? ((Enums.EnumItem) m).name : "Plastic";
        switch (n) {
            case "Ice":
            case "Glacier":
                return 0.02;
            case "Metal":
            case "Glass":
                return 0.25;
            case "Sand":
            case "Grass":
            case "Ground":
            case "Fabric":
                return 0.4;
            case "Rubber":
                return 0.9;
            default:
                return 0.3;
        }
    }
}
