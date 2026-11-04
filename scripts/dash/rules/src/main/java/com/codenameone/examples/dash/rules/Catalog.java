/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.dash.rules;

import java.util.HashMap;
import java.util.Map;

/// Every obstacle and pickup a level may name, by id. [Content#catalog] holds the
/// built-in ones; a community pack adds its own under `u:<pack>:<id>` so it can
/// never shadow one.
///
/// Lookup only: the simulation never iterates the maps, whose order is not the
/// same on every runtime.
public final class Catalog {
    private final Map<String, ObstacleDef> obstacles = new HashMap<String, ObstacleDef>();
    private final Map<String, PickupDef> pickups = new HashMap<String, PickupDef>();

    public Catalog add(ObstacleDef d) {
        obstacles.put(d.id, d);
        return this;
    }

    public Catalog add(PickupDef d) {
        pickups.put(d.id, d);
        return this;
    }

    public ObstacleDef obstacle(String id) {
        return obstacles.get(id);
    }

    public PickupDef pickup(String id) {
        return pickups.get(id);
    }

    public boolean has(String id) {
        return obstacles.containsKey(id) || pickups.containsKey(id);
    }

    /// A catalog holding this one's entries plus `other`'s.
    public Catalog with(Catalog other) {
        Catalog c = new Catalog();
        c.obstacles.putAll(obstacles);
        c.pickups.putAll(pickups);
        c.obstacles.putAll(other.obstacles);
        c.pickups.putAll(other.pickups);
        return c;
    }
}
