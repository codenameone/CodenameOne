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
package com.codename1.rbx;

import com.codename1.io.JSONParser;
import com.codename1.rbx.place.CompatReport;
import com.codename1.rbx.place.PlaceFile;
import com.codename1.rbx.place.PlaceLoader;
import com.codename1.rbx.place.PlaceSaver;
import com.codename1.rbx.place.RbxlxWriter;
import com.codename1.rbx.place.Templates;
import com.codename1.util.Base64;
import java.io.CharArrayReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/// The studio's tools for an AI agent: inspect and edit the instance tree,
/// write scripts, run Luau, apply templates and effects, playtest headless,
/// import and export place files. Each tool takes and returns JSON.
///
/// It knows nothing about MCP; [AgentMcp] publishes these over the app's
/// built-in MCP server, and tests call [#call] directly. Every call runs on
/// the caller's thread; the MCP bridge marshals onto the EDT.
public final class AgentTools {
    /// What the app supplies: the place being edited, and the actions the
    /// studio owns (screenshots, publishing).
    public interface Studio {
        /// The engine holding the place being edited.
        Engine edited();

        /// Replaces the edited place (after a template or an import).
        void replace(Engine e);

        /// A PNG of the current studio view, or null when there is none.
        byte[] screenshot();

        /// Publishes the current place; returns a message for the agent.
        String publish(String notes);
    }

    /// One tool: its name, description and JSON schema of the arguments.
    private static final String[][] TOOLS = {
        {"place_tree", "Lists the instance tree as path, class and name. Args: root (path, default 'game'), depth (default 3).",
                "{\"type\":\"object\",\"properties\":{\"root\":{\"type\":\"string\"},\"depth\":{\"type\":\"integer\"}}}"},
        {"get_instance", "Returns the class, properties, attributes, tags and children of an instance. Args: path.",
                "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}"},
        {"create_instance", "Creates an instance. Args: class, parent (path), name, properties (object). Vector3/Color3 "
                + "as [x,y,z]/[r,g,b 0-255] or \"#rrggbb\", CFrame as [x,y,z] or [x,y,z,rxDeg,ryDeg,rzDeg], UDim2 as "
                + "[xs,xo,ys,yo], enums by item name, references by path.",
                "{\"type\":\"object\",\"properties\":{\"class\":{\"type\":\"string\"},\"parent\":{\"type\":\"string\"},"
                + "\"name\":{\"type\":\"string\"},\"properties\":{\"type\":\"object\"}},\"required\":[\"class\",\"parent\"]}"},
        {"set_properties", "Sets properties (same value forms as create_instance), attributes and tags. Args: path, "
                + "properties, attributes, tags (array).",
                "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"properties\":{\"type\":\"object\"},"
                + "\"attributes\":{\"type\":\"object\"},\"tags\":{\"type\":\"array\"}},\"required\":[\"path\"]}"},
        {"delete_instance", "Destroys an instance and its descendants. Args: path.",
                "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}"},
        {"write_script", "Creates or replaces a Script, LocalScript or ModuleScript. Args: parent (path), name, class, "
                + "source (Luau).",
                "{\"type\":\"object\",\"properties\":{\"parent\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"},"
                + "\"class\":{\"type\":\"string\"},\"source\":{\"type\":\"string\"}},\"required\":[\"parent\",\"name\","
                + "\"source\"]}"},
        {"read_script", "Returns a script's source. Args: path.",
                "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}"},
        {"run_luau", "Runs Luau in the edit session (command bar) and returns its results and output.",
                "{\"type\":\"object\",\"properties\":{\"source\":{\"type\":\"string\"}},\"required\":[\"source\"]}"},
        {"playtest", "Plays a copy of the place headless with physics and N players for some seconds and returns the "
                + "output, errors, each player's leaderstats and the budget kills. Args: seconds (default 10), players "
                + "(default 1).",
                "{\"type\":\"object\",\"properties\":{\"seconds\":{\"type\":\"number\"},\"players\":{\"type\":\"integer\"}}}"},
        {"list_templates", "Lists the starter templates.", "{\"type\":\"object\",\"properties\":{}}"},
        {"apply_template", "Replaces the place with a starter template. Args: id.",
                "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}},\"required\":[\"id\"]}"},
        {"add_effect", "Adds a visual effect. Args: kind (bloom, sunrays, atmosphere, depth_of_field, color_grade, "
                + "fire, smoke, sparkles, particles, light, neon, forcefield, explosion), target (path; parts for "
                + "fire/smoke/sparkles/particles/light/neon/forcefield), intensity.",
                "{\"type\":\"object\",\"properties\":{\"kind\":{\"type\":\"string\"},\"target\":{\"type\":\"string\"},"
                + "\"intensity\":{\"type\":\"number\"}},\"required\":[\"kind\"]}"},
        {"set_lighting", "Sets the time of day and the mood. Args: clockTime (0-24), brightness, preset (noon, dusk, "
                + "night, overcast, neon).",
                "{\"type\":\"object\",\"properties\":{\"clockTime\":{\"type\":\"number\"},\"brightness\":{\"type\":\"number\"},"
                + "\"preset\":{\"type\":\"string\"}}}"},
        {"compat_report", "Loads the current place the way an import does and returns its compatibility report.",
                "{\"type\":\"object\",\"properties\":{}}"},
        {"import_place", "Replaces the place with a .rbxlx or .rbxl file the creator owns, base64 encoded; returns the "
                + "compatibility report.",
                "{\"type\":\"object\",\"properties\":{\"base64\":{\"type\":\"string\"}},\"required\":[\"base64\"]}"},
        {"export_place", "Returns the place as .rbxlx text.", "{\"type\":\"object\",\"properties\":{}}"},
        {"screenshot", "Returns a PNG of the studio view, base64 encoded.", "{\"type\":\"object\",\"properties\":{}}"},
        {"publish", "Publishes the place as a new version. Args: notes.",
                "{\"type\":\"object\",\"properties\":{\"notes\":{\"type\":\"string\"}}}"},
    };

    private final Studio studio;

    public AgentTools(Studio studio) {
        this.studio = studio;
    }

    /// Runs tool `name` with JSON arguments and returns its JSON answer. A
    /// failure is thrown as an [IllegalArgumentException] with a message the
    /// agent can act on.
    public String call(String name, String argsJson) {
        Map<String, Object> a = parse(argsJson);
        Engine e = studio.edited();
        switch (name) {
            case "place_tree":
                return tree(resolve(e, str(a, "root", "game")), (int) num(a, "depth", 3));
            case "get_instance":
                return describe(resolve(e, str(a, "path", null)));
            case "create_instance":
                return create(e, a);
            case "set_properties": {
                Instance i = resolve(e, str(a, "path", null));
                apply(e, i, a);
                return describe(i);
            }
            case "delete_instance": {
                Instance i = resolve(e, str(a, "path", null));
                if (i.getDesc().isService() || i == e.getGame()) { // NOPMD CompareObjectsWithEquals - identity
                    throw new IllegalArgumentException("Services cannot be deleted");
                }
                i.destroy();
                return "{\"ok\":true}";
            }
            case "write_script":
                return writeScript(e, a);
            case "read_script":
                return quote(resolve(e, str(a, "path", null)).str("Source"));
            case "run_luau":
                return runLuau(e, str(a, "source", ""));
            case "playtest":
                return playtest(e, num(a, "seconds", 10), (int) num(a, "players", 1));
            case "list_templates":
                return list(Templates.ids());
            case "apply_template":
                return applyTemplate(str(a, "id", null));
            case "add_effect":
                return addEffect(e, a);
            case "set_lighting":
                return setLighting(e, a);
            case "compat_report":
                return report(PlaceLoader.loadPlace(new Engine(Engine.SOLO), PlaceSaver.save(e)));
            case "import_place":
                return importPlace(str(a, "base64", ""));
            case "export_place":
                return quote(RbxlxWriter.write(PlaceSaver.save(e)));
            case "screenshot": {
                byte[] png = studio.screenshot();
                if (png == null) {
                    throw new IllegalArgumentException("No studio view is showing");
                }
                return quote(Base64.encodeNoNewline(png));
            }
            case "publish":
                return quote(studio.publish(str(a, "notes", "")));
            default:
                throw new IllegalArgumentException("Unknown tool " + name);
        }
    }

    // -- arguments --------------------------------------------------------

    @SuppressWarnings("unchecked")
    static Map<String, Object> parse(String json) {
        try {
            JSONParser p = new JSONParser();
            p.setUseBooleanInstance(true);
            p.setUseLongsInstance(false);
            String s = json == null || json.trim().length() == 0 ? "{}" : json;
            return p.parseJSON(new CharArrayReader(s.toCharArray()));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Arguments are not JSON: " + ex.getMessage(), ex);
        }
    }

    private static String str(Map<String, Object> a, String k, String def) {
        Object v = a.get(k);
        if (v == null) {
            if (def == null) {
                throw new IllegalArgumentException("Missing argument " + k);
            }
            return def;
        }
        return v.toString();
    }

    private static double num(Map<String, Object> a, String k, double def) {
        Object v = a.get(k);
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }

    /// `game`, a service name, or a dotted path from either.
    static Instance resolve(Engine e, String path) {
        String[] parts = split(path);
        int i = 0;
        Instance at = e.getGame();
        if (parts.length > 0 && "game".equals(parts[0])) {
            i = 1;
        }
        if (i < parts.length && "workspace".equals(parts[i])) {
            at = e.getWorkspace();
            i++;
        } else if (i < parts.length) {
            Instance s = e.getGame().findFirstChild(parts[i]);
            if (s == null && ClassDb.get(parts[i]) != null && ClassDb.get(parts[i]).isService()) {
                s = e.service(parts[i]);
            }
            if (s == null) {
                throw new IllegalArgumentException(parts[i] + " is not a service or child of game");
            }
            at = s;
            i++;
        }
        for (; i < parts.length; i++) {
            Instance c = at.findFirstChild(parts[i]);
            if (c == null) {
                throw new IllegalArgumentException(parts[i] + " is not a child of " + at.fullName());
            }
            at = c;
        }
        return at;
    }

    private static String[] split(String path) {
        List<String> out = new ArrayList<String>();
        int s = 0;
        for (int i = 0; i <= path.length(); i++) {
            if (i == path.length() || path.charAt(i) == '.') {
                if (i > s) {
                    out.add(path.substring(s, i));
                }
                s = i + 1;
            }
        }
        return out.toArray(new String[0]);
    }

    // -- edits ------------------------------------------------------------

    private String create(Engine e, Map<String, Object> a) {
        String cls = str(a, "class", null);
        ClassDb.Desc d = ClassDb.get(cls);
        if (d == null || !d.isCreatable()) {
            throw new IllegalArgumentException(cls + " is not a class that can be created");
        }
        Instance parent = resolve(e, str(a, "parent", null));
        Instance i = e.create(cls);
        if (a.get("name") != null) {
            i.setName(a.get("name").toString());
        }
        apply(e, i, a);
        i.setParent(parent);
        return describe(i);
    }

    @SuppressWarnings("unchecked")
    private static void apply(Engine e, Instance i, Map<String, Object> a) {
        Object props = a.get("properties");
        if (props instanceof Map) {
            for (Map.Entry<String, Object> p : ((Map<String, Object>) props).entrySet()) {
                setProp(e, i, p.getKey(), p.getValue());
            }
        }
        Object attrs = a.get("attributes");
        if (attrs instanceof Map) {
            for (Map.Entry<String, Object> p : ((Map<String, Object>) attrs).entrySet()) {
                Object v = p.getValue();
                i.setAttribute(p.getKey(), v instanceof Number ? Double.valueOf(((Number) v).doubleValue()) : v);
            }
        }
        Object tags = a.get("tags");
        if (tags instanceof Collection) {
            for (Object t : (Collection<Object>) tags) {
                e.addTag(i, t.toString());
            }
        }
    }

    static void setProp(Engine e, Instance i, String k, Object v) {
        if ("Name".equals(k)) {
            i.setName(String.valueOf(v));
            return;
        }
        if ("Parent".equals(k)) {
            i.setParent(resolve(e, String.valueOf(v)));
            return;
        }
        Object d = i.getDesc().defaultOf(k);
        if (d == ClassDb.MISSING) { // NOPMD CompareObjectsWithEquals - sentinel
            throw new IllegalArgumentException(k + " is not a property of " + i.getClassName());
        }
        i.put(k, convert(e, d, k, v));
    }

    @SuppressWarnings("unchecked")
    static Object convert(Engine e, Object def, String k, Object v) {
        if (def == ClassDb.REF) { // NOPMD CompareObjectsWithEquals - sentinel
            return v == null ? null : resolve(e, v.toString());
        }
        if (def instanceof Boolean) {
            return Boolean.valueOf(v instanceof Boolean ? ((Boolean) v).booleanValue() : "true".equals(v));
        }
        if (def instanceof Number) {
            if (!(v instanceof Number)) {
                throw new IllegalArgumentException(k + " takes a number");
            }
            return Double.valueOf(((Number) v).doubleValue());
        }
        if (def instanceof String) {
            return String.valueOf(v);
        }
        if (def instanceof Enums.EnumItem) {
            return Enums.item(((Enums.EnumItem) def).type.name, String.valueOf(v));
        }
        if (def instanceof Color3 && v instanceof String) {
            return Color3.hex((String) v);
        }
        double[] n = numbers(k, v);
        if (def instanceof Vector3 && n.length == 3) {
            return new Vector3(n[0], n[1], n[2]);
        }
        if (def instanceof Vector2 && n.length == 2) {
            return new Vector2(n[0], n[1]);
        }
        if (def instanceof Color3 && n.length == 3) {
            return Color3.rgb((int) n[0], (int) n[1], (int) n[2]);
        }
        if (def instanceof UDim2 && n.length == 4) {
            return new UDim2(n[0], n[1], n[2], n[3]);
        }
        if (def instanceof UDim && n.length == 2) {
            return new UDim(n[0], n[1]);
        }
        if (def instanceof CFrame && (n.length == 3 || n.length == 6)) {
            CFrame at = new CFrame(n[0], n[1], n[2], 1, 0, 0, 0, 1, 0, 0, 0, 1);
            if (n.length == 6) {
                at = at.mul(CFrame.angles(Math.toRadians(n[3]), Math.toRadians(n[4]), Math.toRadians(n[5])));
            }
            return at;
        }
        throw new IllegalArgumentException("Cannot convert " + v + " for " + k);
    }

    @SuppressWarnings("unchecked")
    private static double[] numbers(String k, Object v) {
        if (!(v instanceof List)) {
            throw new IllegalArgumentException(k + " takes an array of numbers");
        }
        List<Object> l = (List<Object>) v;
        double[] out = new double[l.size()];
        for (int i = 0; i < out.length; i++) {
            Object o = l.get(i);
            if (!(o instanceof Number)) {
                throw new IllegalArgumentException(k + " takes an array of numbers");
            }
            out[i] = ((Number) o).doubleValue();
        }
        return out;
    }

    private String writeScript(Engine e, Map<String, Object> a) {
        Instance parent = resolve(e, str(a, "parent", null));
        String name = str(a, "name", null);
        String cls = str(a, "class", "Script");
        if (!"Script".equals(cls) && !"LocalScript".equals(cls) && !"ModuleScript".equals(cls)) {
            throw new IllegalArgumentException("class must be Script, LocalScript or ModuleScript");
        }
        String src = str(a, "source", null);
        String syntax = syntaxError(e, src);
        Instance old = parent.findFirstChild(name);
        if (old != null && old.isA(cls)) {
            old.put("Source", src);
        } else {
            Instance s = e.create(cls);
            s.setName(name);
            s.put("Source", src);
            s.setParent(parent);
        }
        return "{\"ok\":true,\"syntaxError\":" + (syntax == null ? "null" : quote(syntax)) + "}";
    }

    private static String syntaxError(Engine e, String src) {
        try {
            e.getInterp().load(com.codename1.luau.Utf8.fromJava(src), "check", e.getInterp().getGlobals());
            return null;
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }

    private static String runLuau(Engine e, String src) {
        final List<String> out = new ArrayList<String>();
        Scheduler.Output prev = e.getScheduler().getOutput();
        e.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                out.add(type + ": " + text);
            }
        });
        try {
            Object[] r = e.runCommand(src);
            List<String> vals = new ArrayList<String>();
            if (r != null) {
                for (Object o : r) {
                    vals.add(value(o));
                }
            }
            return "{\"results\":" + rawList(vals) + ",\"output\":" + list(out.toArray(new String[0])) + "}";
        } catch (RuntimeException ex) {
            return "{\"error\":" + quote(String.valueOf(ex.getMessage())) + ",\"output\":"
                    + list(out.toArray(new String[0])) + "}";
        } finally {
            e.getScheduler().setOutput(prev);
        }
    }

    /// Plays a saved copy, so the edited place is never changed by its scripts.
    static String playtest(Engine edited, double seconds, int players) {
        final List<String> out = new ArrayList<String>();
        final List<String> errors = new ArrayList<String>();
        Engine e = new Engine(Engine.SOLO);
        e.setPhysics(new RigidBodies());
        e.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                if (type.startsWith("error")) {
                    errors.add(text);
                } else if (out.size() < 200) {
                    out.add(type + ": " + text);
                }
            }
        });
        PlaceLoader.loadPlace(e, PlaceSaver.save(edited));
        for (int p = 0; p < Math.max(1, Math.min(8, players)); p++) {
            e.addPlayer(p + 1, "Player" + (p + 1), null);
        }
        int frames = (int) (Math.max(0.1, Math.min(120, seconds)) * 60);
        for (int i = 0; i < frames; i++) {
            e.step(1 / 60.0);
        }
        StringBuilder stats = new StringBuilder("{");
        boolean first = true;
        for (Instance p : e.service("Players").getChildren()) {
            Instance ls = p.findFirstChild("leaderstats");
            if (!first) {
                stats.append(',');
            }
            first = false;
            stats.append(quote(p.getName())).append(":{");
            if (ls != null) {
                boolean f2 = true;
                for (Instance v : ls.getChildren()) {
                    if (!f2) {
                        stats.append(',');
                    }
                    f2 = false;
                    stats.append(quote(v.getName())).append(':').append(value(v.prop("Value")));
                }
            }
            stats.append('}');
        }
        stats.append('}');
        return "{\"seconds\":" + frames / 60.0 + ",\"output\":" + list(out.toArray(new String[0])) + ",\"errors\":"
                + list(errors.toArray(new String[0])) + ",\"leaderstats\":" + stats + ",\"unsupported\":"
                + list(e.getUnsupported().toArray(new String[0])) + ",\"instances\":"
                + count(e.getWorkspace()) + "}";
    }

    private static int count(Instance i) {
        int n = 1;
        for (Instance c : i.getChildren()) {
            n += count(c);
        }
        return n;
    }

    private String applyTemplate(String id) {
        Engine e = new Engine(Engine.SOLO);
        e.setStudio(true);
        try {
            CompatReport r = Templates.load(e, id);
            studio.replace(e);
            return report(r);
        } catch (IOException ex) {
            throw new IllegalArgumentException("No template " + id + "; see list_templates", ex);
        }
    }

    private String importPlace(String b64) {
        byte[] data = Base64.decode(utf8(b64));
        Engine e = new Engine(Engine.SOLO);
        e.setStudio(true);
        CompatReport r = PlaceLoader.loadPlace(e, PlaceFile.read(data));
        studio.replace(e);
        return report(r);
    }

    private static String report(CompatReport r) {
        return "{\"report\":" + quote(r.toString()) + "}";
    }

    private static String addEffect(Engine e, Map<String, Object> a) {
        String kind = str(a, "kind", null);
        double k = num(a, "intensity", 1);
        Instance lighting = e.service("Lighting");
        Instance made;
        switch (kind) {
            case "bloom":
                made = child(e, lighting, "BloomEffect");
                made.put("Intensity", Double.valueOf(k));
                made.put("Threshold", Double.valueOf(0.9));
                break;
            case "sunrays":
                made = child(e, lighting, "SunRaysEffect");
                made.put("Intensity", Double.valueOf(0.25 * k));
                break;
            case "atmosphere":
                made = child(e, lighting, "Atmosphere");
                made.put("Density", Double.valueOf(0.35 * k));
                break;
            case "depth_of_field":
                made = child(e, lighting, "DepthOfFieldEffect");
                made.put("FarIntensity", Double.valueOf(0.5 * k));
                break;
            case "color_grade":
                made = child(e, lighting, "ColorCorrectionEffect");
                made.put("Contrast", Double.valueOf(0.15 * k));
                made.put("Saturation", Double.valueOf(0.2 * k));
                break;
            case "explosion": {
                Instance t = resolve(e, str(a, "target", "workspace"));
                made = e.create("Explosion");
                made.put("Position", t.isA("BasePart") ? Behaviors.cf(t).position() : Vector3.ZERO);
                made.put("BlastRadius", Double.valueOf(6 * k));
                made.setParent(e.getWorkspace());
                break;
            }
            case "neon":
            case "forcefield": {
                Instance t = part(e, a);
                t.put("Material", Enums.item("Material", "neon".equals(kind) ? "Neon" : "ForceField"));
                made = t;
                break;
            }
            case "fire":
            case "smoke":
            case "sparkles":
            case "particles":
            case "light": {
                Instance t = part(e, a);
                String cls = "fire".equals(kind) ? "Fire" : "smoke".equals(kind) ? "Smoke"
                        : "sparkles".equals(kind) ? "Sparkles" : "light".equals(kind) ? "PointLight" : "ParticleEmitter";
                made = e.create(cls);
                if ("PointLight".equals(cls)) {
                    made.put("Brightness", Double.valueOf(2 * k));
                } else if ("ParticleEmitter".equals(cls)) {
                    made.put("Rate", Double.valueOf(20 * k));
                }
                made.setParent(t);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown effect " + kind);
        }
        return describe(made);
    }

    private static Instance part(Engine e, Map<String, Object> a) {
        Instance t = resolve(e, str(a, "target", null));
        if (!t.isA("BasePart")) {
            throw new IllegalArgumentException(t.fullName() + " is not a part");
        }
        return t;
    }

    private static Instance child(Engine e, Instance parent, String cls) {
        Instance c = parent.findFirstChildOfClass(cls, false);
        if (c == null) {
            c = e.create(cls);
            c.setParent(parent);
        }
        return c;
    }

    private static String setLighting(Engine e, Map<String, Object> a) {
        Instance l = e.service("Lighting");
        String preset = a.get("preset") == null ? null : a.get("preset").toString();
        if (preset != null) {
            double[] p;
            switch (preset) {
                case "noon":
                    p = new double[] {12, 3};
                    break;
                case "dusk":
                    p = new double[] {18.2, 2};
                    break;
                case "night":
                    p = new double[] {0, 1};
                    break;
                case "overcast":
                    p = new double[] {13, 1.2};
                    child(e, l, "Atmosphere").put("Density", Double.valueOf(0.5));
                    break;
                case "neon":
                    p = new double[] {21, 1.5};
                    Instance b = child(e, l, "BloomEffect");
                    b.put("Intensity", Double.valueOf(1.5));
                    b.put("Threshold", Double.valueOf(0.6));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown preset " + preset);
            }
            l.put("ClockTime", Double.valueOf(p[0]));
            l.put("Brightness", Double.valueOf(p[1]));
        }
        if (a.get("clockTime") instanceof Number) {
            l.put("ClockTime", Double.valueOf(((Number) a.get("clockTime")).doubleValue()));
        }
        if (a.get("brightness") instanceof Number) {
            l.put("Brightness", Double.valueOf(((Number) a.get("brightness")).doubleValue()));
        }
        return describe(l);
    }

    // -- answers ----------------------------------------------------------

    private static String tree(Instance root, int depth) {
        StringBuilder b = new StringBuilder("[");
        tree(b, root, Math.max(0, Math.min(12, depth)), true);
        return b.append(']').toString();
    }

    private static boolean tree(StringBuilder b, Instance i, int depth, boolean first) {
        if (!first) {
            b.append(',');
        }
        b.append("{\"path\":").append(quote(i.fullName())).append(",\"class\":").append(quote(i.getClassName()))
                .append(",\"children\":").append(i.getChildren().size()).append('}');
        if (depth > 0) {
            for (Instance c : i.getChildren()) {
                tree(b, c, depth - 1, false);
            }
        }
        return false;
    }

    static String describe(Instance i) {
        StringBuilder b = new StringBuilder("{\"path\":").append(quote(i.fullName())).append(",\"class\":")
                .append(quote(i.getClassName())).append(",\"properties\":{");
        boolean first = true;
        for (Map.Entry<String, Object> p : i.storedProps().entrySet()) {
            if ("Source".equals(p.getKey())) {
                continue;
            }
            if (!first) {
                b.append(',');
            }
            first = false;
            b.append(quote(p.getKey())).append(':').append(value(p.getValue()));
        }
        b.append("},\"attributes\":{");
        first = true;
        for (Map.Entry<String, Object> p : i.attributes().entrySet()) {
            if (!first) {
                b.append(',');
            }
            first = false;
            b.append(quote(p.getKey())).append(':').append(value(p.getValue()));
        }
        b.append("},\"children\":[");
        first = true;
        for (Instance c : i.getChildren()) {
            if (!first) {
                b.append(',');
            }
            first = false;
            b.append(quote(c.getName()));
        }
        return b.append("]}").toString();
    }

    static String value(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Boolean) {
            return v.toString();
        }
        if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            return Double.isNaN(d) || Double.isInfinite(d) ? quote(String.valueOf(d)) : num(d);
        }
        if (v instanceof Vector3) {
            Vector3 x = (Vector3) v;
            return "[" + num(x.x) + "," + num(x.y) + "," + num(x.z) + "]";
        }
        if (v instanceof Color3) {
            Color3 c = (Color3) v;
            return "[" + Math.round(c.r * 255) + "," + Math.round(c.g * 255) + "," + Math.round(c.b * 255) + "]";
        }
        if (v instanceof CFrame) {
            CFrame c = (CFrame) v;
            return "[" + num(c.x) + "," + num(c.y) + "," + num(c.z) + "]";
        }
        if (v instanceof Instance) {
            return quote(((Instance) v).fullName());
        }
        if (v instanceof Enums.EnumItem) {
            return quote(((Enums.EnumItem) v).name);
        }
        if (v instanceof RbxType) {
            return quote(((RbxType) v).str());
        }
        return quote(String.valueOf(v));
    }

    private static String num(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }

    static String list(String[] items) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(quote(items[i]));
        }
        return b.append(']').toString();
    }

    private static String rawList(List<String> items) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(items.get(i));
        }
        return b.append(']').toString();
    }

    static String quote(String s) {
        StringBuilder b = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    b.append("\\\"");
                    break;
                case '\\':
                    b.append("\\\\");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                case '\t':
                    b.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        String h = Integer.toHexString(c);
                        b.append("\\u").append("0000".substring(h.length())).append(h);
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.append('"').toString();
    }

    /// How many tools there are.
    public static int toolCount() {
        return TOOLS.length;
    }

    /// Tool `i` as {name, description, schema}; a copy.
    public static String[] tool(int i) {
        String[] t = TOOLS[i];
        return new String[] {t[0], t[1], t[2]};
    }

    /// A property value as the JSON the tools take and return.
    public static String json(Object v) {
        return value(v);
    }

    /// `s` as a JSON string literal.
    public static String jsonString(String s) {
        return quote(s);
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
