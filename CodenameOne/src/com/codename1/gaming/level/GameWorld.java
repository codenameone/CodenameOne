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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A large game space made of linked `Region`s, streamed in and out around the player so the
/// world can exceed memory while transitions stay seamless. Hold the regions directly (small
/// worlds) or supply a `RegionProvider` to page them by id. Call `#update(double,double)` each
/// frame with the player's world position: the world keeps the containing region active, loads
/// its linked neighbours, and unloads (saving) regions beyond `#getKeepRadius()` links away.
public class GameWorld {
    private final Map<String, Region> regions = new LinkedHashMap<String, Region>();
    private RegionProvider provider;
    private String activeRegionId;
    private int keepRadius = 1;

    public GameWorld() {
    }

    public GameWorld(RegionProvider provider) {
        this.provider = provider;
    }

    public RegionProvider getProvider() {
        return provider;
    }

    public GameWorld setProvider(RegionProvider provider) {
        this.provider = provider;
        return this;
    }

    /// How many neighbour links away from the active region stay resident.
    public int getKeepRadius() {
        return keepRadius;
    }

    public GameWorld setKeepRadius(int keepRadius) {
        this.keepRadius = Math.max(0, keepRadius);
        return this;
    }

    /// Serializes the resident regions + active region as a JSON object. (A provider-backed
    /// world persists individual regions through its `RegionProvider`; this inline form suits
    /// small/medium worlds embedded in a level file.)
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"active\":");
        Json.writeString(sb, activeRegionId);
        sb.append(",\"keepRadius\":").append(keepRadius);
        sb.append(",\"regions\":[");
        boolean first = true;
        for (Region r : regions.values()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(r.toJson());
        }
        sb.append("]}");
        return sb.toString();
    }

    public static GameWorld fromMap(Map<String, Object> root) {
        GameWorld w = new GameWorld();
        w.keepRadius = Json.intval(root.get("keepRadius"), 1);
        java.util.List<Object> rs = Json.asList(root.get("regions"));
        if (rs != null) {
            for (int i = 0; i < rs.size(); i++) {
                Map<String, Object> rm = Json.asMap(rs.get(i));
                if (rm != null) {
                    w.addRegion(Region.fromMap(rm));
                }
            }
        }
        String active = Json.str(root.get("active"), null);
        if (active != null) {
            w.activeRegionId = active;
        }
        return w;
    }

    public GameWorld addRegion(Region r) {
        if (r != null && r.getId() != null) {
            regions.put(r.getId(), r);
            if (activeRegionId == null) {
                activeRegionId = r.getId();
            }
        }
        return this;
    }

    /// Returns a resident region, paging it in through the provider if needed.
    public Region getRegion(String id) {
        if (id == null) {
            return null;
        }
        Region r = regions.get(id);
        if (r == null && provider != null) {
            r = provider.loadRegion(id);
            if (r != null) {
                regions.put(id, r);
            }
        }
        return r;
    }

    public Region getActiveRegion() {
        return getRegion(activeRegionId);
    }

    public GameWorld setActiveRegion(String id) {
        this.activeRegionId = id;
        return this;
    }

    /// Currently-resident regions.
    public List<Region> residentRegions() {
        return new ArrayList<Region>(regions.values());
    }

    /// Drives streaming: picks the region containing the player (preferring a neighbour of the
    /// current active region for seamless crossings), loads neighbours within `#getKeepRadius()`
    /// and unloads (persisting) the rest.
    public void update(double playerWorldX, double playerWorldZ) {
        Region active = getActiveRegion();
        // cross into a neighbour whose bounds contain the player
        if (active != null && !active.contains(playerWorldX, playerWorldZ)) {
            for (String nid : active.neighbors().values()) {
                Region n = getRegion(nid);
                if (n != null && n.contains(playerWorldX, playerWorldZ)) {
                    activeRegionId = nid;
                    active = n;
                    break;
                }
            }
        }
        if (active == null) {
            return;
        }
        // compute the keep-set: active + neighbours up to keepRadius links
        Map<String, Boolean> keep = new LinkedHashMap<String, Boolean>();
        List<String> frontier = new ArrayList<String>();
        keep.put(active.getId(), Boolean.TRUE);
        frontier.add(active.getId());
        for (int depth = 0; depth < keepRadius; depth++) {
            List<String> next = new ArrayList<String>();
            for (int i = 0; i < frontier.size(); i++) {
                Region r = getRegion(frontier.get(i));
                if (r == null) {
                    continue;
                }
                for (String nid : r.neighbors().values()) {
                    if (nid != null && !keep.containsKey(nid)) {
                        keep.put(nid, Boolean.TRUE);
                        next.add(nid);
                        getRegion(nid);   // page it in
                    }
                }
            }
            frontier = next;
        }
        // evict (saving) regions outside the keep-set when we have a provider to reload them
        if (provider != null) {
            List<String> toEvict = new ArrayList<String>();
            for (String id : regions.keySet()) {
                if (!keep.containsKey(id)) {
                    toEvict.add(id);
                }
            }
            for (int i = 0; i < toEvict.size(); i++) {
                Region r = regions.remove(toEvict.get(i));
                if (r != null) {
                    provider.saveRegion(r);
                }
            }
        }
    }
}
