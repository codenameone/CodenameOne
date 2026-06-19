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
package com.codename1.gaming.level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Process-wide registry of pluggable `Material`s, keyed by id. Built-in surfaces (grass, road,
/// stone, sand, water, dirt) are registered on first use; applications add their own via
/// `#register(Material)`. Unknown ids resolve to a neutral grey placeholder so a level that
/// references a not-yet-registered material still loads.
public final class MaterialRegistry {
    /// Well-known built-in material ids.
    public static final String GRASS = "grass";
    public static final String ROAD = "road";
    public static final String STONE = "stone";
    public static final String SAND = "sand";
    public static final String WATER = "water";
    public static final String DIRT = "dirt";

    private static final Map<String, Material> MATERIALS = new LinkedHashMap<String, Material>();
    private static final Material UNKNOWN = new Material("unknown", "Unknown", 0x808080);

    private MaterialRegistry() {
    }

    private static void ensureDefaults() {
        if (!MATERIALS.isEmpty()) {
            return;
        }
        register(new Material(GRASS, "Grass", 0x3f7d3a));
        register(new Material(ROAD, "Road", 0x3b3e46));
        register(new Material(STONE, "Stone", 0x6f6a62));
        register(new Material(SAND, "Sand", 0xc2a86a));
        register(new Material(WATER, "Water", 0x2f6fa8).setSolid(true));
        register(new Material(DIRT, "Dirt", 0x7a5a38));
    }

    /// Registers (or replaces) a material by its id.
    public static void register(Material m) {
        if (m != null && m.getId() != null) {
            MATERIALS.put(m.getId(), m);
        }
    }

    /// Resolves a material by id, never null (returns a grey placeholder for unknown ids).
    public static Material get(String id) {
        ensureDefaults();
        Material m = MATERIALS.get(id);
        return m != null ? m : UNKNOWN;
    }

    public static boolean contains(String id) {
        ensureDefaults();
        return MATERIALS.containsKey(id);
    }

    /// All registered materials in registration order.
    public static Collection<Material> all() {
        ensureDefaults();
        return new ArrayList<Material>(MATERIALS.values());
    }

    /// The ids of all registered materials, in registration order.
    public static List<String> ids() {
        ensureDefaults();
        return new ArrayList<String>(MATERIALS.keySet());
    }
}
