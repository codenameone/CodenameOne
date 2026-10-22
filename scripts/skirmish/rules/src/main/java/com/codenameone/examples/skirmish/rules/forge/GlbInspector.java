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
package com.codenameone.examples.skirmish.rules.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Checks an uploaded binary glTF (`.glb`) before anyone loads it.
///
/// The app renders these on the GPU and the server stores them, so a file is held
/// to the structure a character needs and to fixed limits, and nothing in it is
/// trusted: the header's lengths are checked against the bytes actually present,
/// and the JSON chunk is parsed with the same bounded parser as a pack. What the
/// file *draws* is a moderator's question; whether it is safe to open is this one.
public final class GlbInspector {
    public static final int MAX_JSON_CHARS = 256 * 1024;
    public static final int MAX_NODES = 256;
    public static final int MAX_MESHES = 32;
    public static final int MAX_ACCESSORS = 1024;
    public static final int MAX_JOINTS = 128;
    public static final int MAX_ANIMATIONS = 48;
    /// Vertices across all meshes; a phone draws dozens of these at once.
    public static final int MAX_VERTICES = 60000;

    private GlbInspector() {
    }

    /// What was found: the animation names when the file is usable, or the reason not.
    public static final class Report {
        public final String problem;
        public final List<String> animations;
        public final boolean skinned;
        public final int vertices;

        Report(String problem, List<String> animations, boolean skinned, int vertices) {
            this.problem = problem;
            this.animations = animations;
            this.skinned = skinned;
            this.vertices = vertices;
        }

        public boolean ok() {
            return problem == null;
        }
    }

    private static Report bad(String why) {
        return new Report(why, new ArrayList<String>(), false, 0);
    }

    private static int u32(byte[] b, int at) {
        return (b[at] & 0xff) | (b[at + 1] & 0xff) << 8 | (b[at + 2] & 0xff) << 16 | (b[at + 3] & 0xff) << 24;
    }

    public static Report inspect(byte[] glb) {
        if (glb == null || glb.length < 20) {
            return bad("not a glTF binary");
        }
        if (glb.length > Validator.MAX_MODEL_BYTES) {
            return bad("larger than " + (Validator.MAX_MODEL_BYTES / (1024 * 1024)) + " MB");
        }
        // "glTF", version 2, total length, then the JSON chunk.
        if (u32(glb, 0) != 0x46546c67 || u32(glb, 4) != 2) {
            return bad("not a glTF 2 binary");
        }
        if (u32(glb, 8) != glb.length) {
            return bad("the file is truncated or padded");
        }
        int jsonLength = u32(glb, 12);
        if (u32(glb, 16) != 0x4e4f534a || jsonLength <= 0 || jsonLength > glb.length - 20
                || jsonLength > MAX_JSON_CHARS) {
            return bad("the description chunk is missing or too large");
        }
        int binAt = 20 + jsonLength;
        if (binAt + 8 <= glb.length) {
            int binLength = u32(glb, binAt);
            if (binLength < 0 || binLength > glb.length - binAt - 8 || u32(glb, binAt + 4) != 0x004e4942) {
                return bad("the data chunk is malformed");
            }
        }
        StringBuilder sb = new StringBuilder(jsonLength);
        for (int i = 0; i < jsonLength; i++) {
            int c = glb[20 + i] & 0xff;
            if (c >= 0x80) {
                // Names may be UTF-8; the structure is ASCII. Keep a placeholder.
                c = '?';
            }
            sb.append((char) c);
        }
        Object doc;
        try {
            doc = Json.parseLenient(sb.toString().trim(), MAX_JSON_CHARS);
        } catch (IllegalArgumentException e) {
            return bad("the description is not valid JSON");
        }
        if (!(doc instanceof Map)) {
            return bad("the description is not an object");
        }
        Map<?, ?> root = (Map<?, ?>) doc;
        if (root.containsKey("extensionsRequired")) {
            return bad("the model needs glTF extensions the game does not support");
        }
        if (root.containsKey("buffers") && hasExternalUri(root.get("buffers"))) {
            return bad("the model refers to files outside itself");
        }
        if (root.containsKey("images") && hasExternalUri(root.get("images"))) {
            return bad("the model refers to images outside itself");
        }
        if (count(root, "nodes") > MAX_NODES || count(root, "meshes") > MAX_MESHES
                || count(root, "accessors") > MAX_ACCESSORS || count(root, "animations") > MAX_ANIMATIONS) {
            return bad("the model is too complex");
        }
        if (count(root, "meshes") == 0) {
            return bad("the model has nothing to draw");
        }
        boolean skinned = false;
        Object skins = root.get("skins");
        if (skins instanceof List) {
            List<?> l = (List<?>) skins;
            for (int i = 0; i < l.size(); i++) {
                Object s = l.get(i);
                if (s instanceof Map && ((Map<?, ?>) s).get("joints") instanceof List) {
                    if (((List<?>) ((Map<?, ?>) s).get("joints")).size() > MAX_JOINTS) {
                        return bad("the skeleton has too many bones");
                    }
                    skinned = true;
                }
            }
        }
        int vertices = vertices(root);
        if (vertices > MAX_VERTICES) {
            return bad("the model has too many vertices (" + vertices + ")");
        }
        List<String> names = new ArrayList<String>();
        Object anims = root.get("animations");
        if (anims instanceof List) {
            List<?> l = (List<?>) anims;
            for (int i = 0; i < l.size(); i++) {
                Object a = l.get(i);
                Object n = a instanceof Map ? ((Map<?, ?>) a).get("name") : null;
                names.add(n instanceof String ? (String) n : "Animation " + (i + 1));
            }
        }
        return new Report(null, names, skinned, vertices);
    }

    private static int count(Map<?, ?> root, String key) {
        Object v = root.get(key);
        return v instanceof List ? ((List<?>) v).size() : 0;
    }

    private static boolean hasExternalUri(Object list) {
        if (!(list instanceof List)) {
            return false;
        }
        List<?> l = (List<?>) list;
        for (int i = 0; i < l.size(); i++) {
            Object o = l.get(i);
            if (o instanceof Map && ((Map<?, ?>) o).get("uri") != null) {
                Object uri = ((Map<?, ?>) o).get("uri");
                // An embedded data: URI is self-contained; anything else is a fetch.
                if (!(uri instanceof String) || !((String) uri).startsWith("data:")) {
                    return true;
                }
            }
        }
        return false;
    }

    /// The vertex count, read from each primitive's POSITION accessor.
    private static int vertices(Map<?, ?> root) {
        Object meshes = root.get("meshes");
        Object accessors = root.get("accessors");
        if (!(meshes instanceof List) || !(accessors instanceof List)) {
            return 0;
        }
        List<?> acc = (List<?>) accessors;
        long total = 0;
        List<?> ml = (List<?>) meshes;
        for (int i = 0; i < ml.size(); i++) {
            Object m = ml.get(i);
            Object prims = m instanceof Map ? ((Map<?, ?>) m).get("primitives") : null;
            if (!(prims instanceof List)) {
                continue;
            }
            List<?> pl = (List<?>) prims;
            for (int k = 0; k < pl.size(); k++) {
                Object p = pl.get(k);
                Object attrs = p instanceof Map ? ((Map<?, ?>) p).get("attributes") : null;
                Object pos = attrs instanceof Map ? ((Map<?, ?>) attrs).get("POSITION") : null;
                if (pos instanceof Long) {
                    long idx = ((Long) pos).longValue();
                    if (idx >= 0 && idx < acc.size() && acc.get((int) idx) instanceof Map) {
                        Object c = ((Map<?, ?>) acc.get((int) idx)).get("count");
                        if (c instanceof Long) {
                            total += ((Long) c).longValue();
                        }
                    }
                }
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /// The clip of `part` ("idle", "run", "attack", "death") in a card's
    /// [com.codenameone.examples.skirmish.rules.CardDef#clips()] mapping, or null.
    public static String clip(String clips, String part) {
        if (clips == null) {
            return null;
        }
        String key = part + "=";
        int at = 0;
        while (at < clips.length()) {
            int end = clips.indexOf(';', at);
            if (end < 0) {
                end = clips.length();
            }
            if (clips.startsWith(key, at)) {
                return clips.substring(at + key.length(), end);
            }
            at = end + 1;
        }
        return null;
    }
}
