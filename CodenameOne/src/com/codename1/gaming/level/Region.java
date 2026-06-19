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

/// A loadable area of a large world: its own streaming `Terrain`, placed `GameElement`s and a
/// world-space origin (`#getOriginX()`,`#getOriginZ()` in tile units). Regions are linked to
/// neighbours by named edges (`#link(String,String)`, e.g. "east" -> "desert-2"), so a
/// `GameWorld` can stream the adjacent regions in and out for seamless transitions as the
/// player crosses a boundary. A region serializes independently as its own document.
public class Region {
    private String id;
    private String name;
    private double originX;
    private double originZ;
    private double width;     // span in tile units (0 = unbounded)
    private double depth;
    private final List<GameElement> elements = new ArrayList<GameElement>();
    private final Map<String, String> neighbors = new LinkedHashMap<String, String>();
    private final Map<String, Object> props = new LinkedHashMap<String, Object>();
    private transient StreamingTerrain terrain;

    public Region() {
    }

    public Region(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public Region setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Region setName(String name) {
        this.name = name;
        return this;
    }

    public double getOriginX() {
        return originX;
    }

    public double getOriginZ() {
        return originZ;
    }

    public Region setOrigin(double x, double z) {
        this.originX = x;
        this.originZ = z;
        return this;
    }

    public double getWidth() {
        return width;
    }

    public double getDepth() {
        return depth;
    }

    public Region setSpan(double width, double depth) {
        this.width = width;
        this.depth = depth;
        return this;
    }

    /// The region's streaming terrain (created lazily; chunk-paged so a region can be huge).
    public StreamingTerrain terrain() {
        if (terrain == null) {
            terrain = new StreamingTerrain();
        }
        return terrain;
    }

    public Region setTerrain(StreamingTerrain terrain) {
        this.terrain = terrain;
        return this;
    }

    public List<GameElement> elements() {
        return elements;
    }

    /// Returns true if the world point (tile units) falls inside this region's bounds; an
    /// unbounded region (width/depth 0) contains everything at/after its origin only loosely
    /// (callers use neighbor links for transitions instead).
    public boolean contains(double worldX, double worldZ) {
        if (width <= 0 || depth <= 0) {
            return false;
        }
        return worldX >= originX && worldX < originX + width
                && worldZ >= originZ && worldZ < originZ + depth;
    }

    /// Links a named edge of this region to a neighbouring region id.
    public Region link(String edge, String neighborRegionId) {
        neighbors.put(edge, neighborRegionId);
        return this;
    }

    public Map<String, String> neighbors() {
        return neighbors;
    }

    public Map<String, Object> props() {
        return props;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\":");
        Json.writeString(sb, id);
        sb.append(",\"name\":");
        Json.writeString(sb, name);
        sb.append(",\"originX\":");
        Json.writeNumber(sb, originX);
        sb.append(",\"originZ\":");
        Json.writeNumber(sb, originZ);
        sb.append(",\"width\":");
        Json.writeNumber(sb, width);
        sb.append(",\"depth\":");
        Json.writeNumber(sb, depth);
        sb.append(",\"neighbors\":");
        Json.writeValue(sb, neighbors);
        if (!props.isEmpty()) {
            sb.append(",\"props\":");
            Json.writeValue(sb, props);
        }
        sb.append(",\"elements\":[");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            elements.get(i).write(sb);
        }
        sb.append("],\"chunks\":[");
        if (terrain != null) {
            // persist resident chunks inline (small regions); large ones use a ChunkProvider
            boolean first = true;
            for (TerrainChunk c : terrain.residentChunks()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                c.write(sb);
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    public static Region fromJson(String json) {
        try {
            return fromMap(com.codename1.io.JSONParser.parseJSON(json));
        } catch (java.io.IOException ex) {
            throw new RuntimeException("bad region json", ex);
        }
    }

    static Region fromMap(Map<String, Object> root) {
        Region r = new Region(Json.str(root.get("id"), null), Json.str(root.get("name"), null));
        r.setOrigin(Json.num(root.get("originX"), 0), Json.num(root.get("originZ"), 0));
        r.setSpan(Json.num(root.get("width"), 0), Json.num(root.get("depth"), 0));
        Map<String, Object> nb = Json.asMap(root.get("neighbors"));
        if (nb != null) {
            for (Map.Entry<String, Object> e : nb.entrySet()) {
                r.neighbors.put(e.getKey(), Json.str(e.getValue(), null));
            }
        }
        Map<String, Object> p = Json.asMap(root.get("props"));
        if (p != null) {
            r.props.putAll(p);
        }
        List<Object> els = Json.asList(root.get("elements"));
        if (els != null) {
            for (Object elementEntry : els) {
                Map<String, Object> em = Json.asMap(elementEntry);
                if (em != null) {
                    r.elements.add(GameElement.read(em));
                }
            }
        }
        List<Object> chunks = Json.asList(root.get("chunks"));
        if (chunks != null && !chunks.isEmpty()) {
            StreamingTerrain.MemoryChunkProvider mem = new StreamingTerrain.MemoryChunkProvider();
            for (Object chunkEntry : chunks) {
                Map<String, Object> cm = Json.asMap(chunkEntry);
                if (cm != null) {
                    mem.saveChunk(TerrainChunk.read(cm));
                }
            }
            r.terrain = new StreamingTerrain(mem, 64);
        }
        return r;
    }
}
