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
package com.codenameone.examples.brickyard.rbx.place;

import com.codenameone.examples.brickyard.rbx.BrickColor;
import com.codenameone.examples.brickyard.rbx.CFrame;
import com.codenameone.examples.brickyard.rbx.ClassDb;
import com.codenameone.examples.brickyard.rbx.Color3;
import com.codenameone.examples.brickyard.rbx.Engine;
import com.codenameone.examples.brickyard.rbx.Enums;
import com.codenameone.examples.brickyard.rbx.Instance;
import com.codenameone.examples.brickyard.rbx.NumberRange;
import com.codenameone.examples.brickyard.rbx.QueryParams;
import com.codenameone.examples.brickyard.rbx.Ray;
import com.codenameone.examples.brickyard.rbx.Sequence;
import com.codenameone.examples.brickyard.rbx.UDim;
import com.codenameone.examples.brickyard.rbx.UDim2;
import com.codenameone.examples.brickyard.rbx.Vector2;
import com.codenameone.examples.brickyard.rbx.Vector3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Captures an engine's edit-time tree as a [PlaceFile]: the services a place
/// saves, without players or their characters. Properties go back under the
/// names and types Studio files use, the inverse of [PlaceLoader].
public final class PlaceSaver {
    /// The services a place file carries, in Studio's order.
    private static final String[] SAVED_SERVICES = {"Workspace", "Lighting", "ReplicatedFirst", "ReplicatedStorage",
        "ServerScriptService", "ServerStorage", "StarterGui", "StarterPack", "StarterPlayer", "SoundService",
        "Teams", "Chat", "TextChatService", "MaterialService"};

    private static final String[] INTS = {"ZIndex", "LayoutOrder", "MaxPlayers", "MaxVisibleGraphemes",
        "TextSize", "Rate", "Priority", "RespawnTime", "PlaceId", "GameId", "BadgeId"};

    static final String[] CONTENT = {"SoundId", "Texture", "TextureID", "TextureId", "MeshId", "Image",
        "AnimationId", "SkyboxBk", "SkyboxDn", "SkyboxFt", "SkyboxLf", "SkyboxRt", "SkyboxUp", "SunTextureId",
        "MoonTextureId", "ShirtTemplate", "PantsTemplate", "Graphic", "HoverImage", "PressedImage", "LinkedSource",
        "ColorMap", "NormalMap", "RoughnessMap", "MetalnessMap"};

    private final Map<Instance, PlaceNode> nodes = new IdentityHashMap<Instance, PlaceNode>();
    private final List<Object[]> refs = new ArrayList<Object[]>();
    private final Set<Instance> skip = new HashSet<Instance>();
    private final Map<String, String> types;

    @SuppressWarnings("unchecked")
    private PlaceSaver(Engine e) {
        Object t = e.extra(PlaceLoader.TYPES_KEY);
        types = t instanceof Map ? (Map<String, String>) t : new java.util.HashMap<String, String>();
        Instance players = e.findService("Players");
        if (players != null) {
            for (Instance p : players.getChildren()) {
                Instance c = p.ref("Character");
                if (c != null) {
                    skip.add(c);
                }
            }
        }
    }

    public static PlaceFile save(Engine e) {
        PlaceSaver s = new PlaceSaver(e);
        PlaceFile f = new PlaceFile();
        for (String name : SAVED_SERVICES) {
            Instance svc = e.findService(name);
            if (svc != null) {
                PlaceNode n = s.node(svc);
                n.service = true;
                f.roots.add(n);
            }
        }
        s.resolve();
        return f;
    }

    /// One instance and its subtree, as a model file holds it.
    public static PlaceFile saveModel(Instance root) {
        PlaceSaver s = new PlaceSaver(root.getEngine());
        PlaceFile f = new PlaceFile();
        f.roots.add(s.node(root));
        s.resolve();
        return f;
    }

    private void resolve() {
        for (Object[] r : refs) {
            ((PlaceNode) r[0]).put((String) r[1], "Ref", nodes.get(r[2]));
        }
    }

    private PlaceNode node(Instance i) {
        PlaceNode n = new PlaceNode(i.getClassName());
        nodes.put(i, n);
        n.put("Name", "string", i.getName());
        for (Map.Entry<String, Object> e : i.storedProps().entrySet()) {
            prop(i, n, e.getKey(), e.getValue());
        }
        if (!i.attributes().isEmpty()) {
            n.put("AttributesSerialize", "BinaryString", Attributes.encode(i.attributes()));
        }
        List<String> tags = i.tags();
        if (!tags.isEmpty()) {
            StringBuilder b = new StringBuilder();
            for (String t : tags) {
                if (b.length() > 0) {
                    b.append('\0');
                }
                b.append(t);
            }
            n.put("Tags", "BinaryString", Utf.bytes(b.toString()));
        }
        for (Instance c : i.getChildren()) {
            if (!skip.contains(c) && !c.isA("Player")) {
                n.add(node(c));
            }
        }
        return n;
    }

    private static boolean in(String[] list, String s) {
        for (String x : list) {
            if (x.equals(s)) {
                return true;
            }
        }
        return false;
    }

    private void prop(Instance i, PlaceNode n, String k, Object v) {
        if ("Name".equals(k)) {
            return;
        }
        boolean part = i.isA("BasePart");
        String known = types.get(i.getClassName() + "." + k);
        if (v instanceof Instance) {
            refs.add(new Object[] {n, k, v});
        } else if (v == null) {
            if (i.getDesc().defaultOf(k) == ClassDb.REF) { // NOPMD CompareObjectsWithEquals - sentinel
                n.put(k, "Ref", null);
            }
        } else if (v instanceof Boolean) {
            n.put(k, "bool", v);
        } else if (v instanceof Double) {
            String t = known != null ? known : in(INTS, k) ? "int" : "float";
            if ("Health".equals(k) && i.isA("Humanoid")) {
                n.put("Health_XML", "float", v);
            } else {
                n.put(k, t, v);
            }
        } else if (v instanceof String) {
            String t = "Source".equals(k) ? "ProtectedString" : in(CONTENT, k) ? "Content" : "string";
            n.put(k, known != null && !"string".equals(known) ? known : t, v);
        } else if (v instanceof Enums.EnumItem) {
            n.put(part && "Shape".equals(k) ? "shape" : k, "token", Integer.valueOf(((Enums.EnumItem) v).value));
        } else if (v instanceof Color3) {
            if (part && "Color".equals(k)) {
                n.put("Color3uint8", "Color3uint8", v);
            } else {
                n.put(k, "Color3", v);
            }
        } else if (v instanceof BrickColor) {
            n.put(k, "BrickColor", Integer.valueOf(((BrickColor) v).number));
        } else if (v instanceof Vector3) {
            n.put(part && "Size".equals(k) ? "size" : k, "Vector3", v);
        } else if (v instanceof CFrame) {
            if ("WorldPivot".equals(k)) {
                n.put("WorldPivotData", "OptionalCoordinateFrame", v);
            } else {
                n.put(k, "CoordinateFrame", v);
            }
        } else {
            typed(n, k, v);
        }
    }

    private static void typed(PlaceNode n, String k, Object v) {
        if (v instanceof Vector2) {
            n.put(k, "Vector2", v);
        } else if (v instanceof UDim) {
            n.put(k, "UDim", v);
        } else if (v instanceof UDim2) {
            n.put(k, "UDim2", v);
        } else if (v instanceof Ray) {
            n.put(k, "Ray", v);
        } else if (v instanceof NumberRange) {
            n.put(k, "NumberRange", v);
        } else if (v instanceof Sequence) {
            n.put(k, ((Sequence) v).color ? "ColorSequence" : "NumberSequence", v);
        } else if (v instanceof QueryParams.Rect) {
            n.put(k, "Rect2D", v);
        } else if (v instanceof QueryParams.PhysicalProperties) {
            n.put(k, "PhysicalProperties", v);
        } else if (v instanceof QueryParams.FontFace) {
            n.put(k, "Font", new String[] {((QueryParams.FontFace) v).family, "400", "Normal", ""});
        }
    }

    /// UTF-8 for the tag blob.
    static final class Utf {
        private Utf() {
        }

        static byte[] bytes(String s) {
            try {
                return s.getBytes("UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 is required", e);
            }
        }
    }
}
