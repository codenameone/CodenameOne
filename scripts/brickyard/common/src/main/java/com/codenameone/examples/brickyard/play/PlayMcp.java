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
package com.codenameone.examples.brickyard.play;

import com.codename1.ai.Tool;
import com.codename1.ai.ToolHandler;
import com.codename1.io.JSONParser;
import com.codename1.mcp.MCP;
import com.codename1.rbx.CFrame;
import com.codename1.rbx.Engine;
import com.codename1.rbx.Input;
import com.codename1.rbx.Instance;
import com.codename1.rbx.PlayView;
import com.codename1.rbx.Vector3;
import com.codename1.ui.CN;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.brickyard.api.ExperienceDto;
import com.codenameone.examples.brickyard.ui.ExperienceForm;
import java.util.List;
import java.util.Map;

/// Play-session tools for an agent attached over the app's MCP server: read
/// where the local player is and what the place holds, and drive the
/// character the way a controller does. An agent can then play a game
/// itself -- run a course, test a level a creator just built, or record a
/// demo -- through the same input path as a thumbstick and a jump button.
///
/// The tools exist only while an MCP server runs, which is the simulator
/// and debug builds; [MCP] refuses release builds.
public final class PlayMcp {
    private static Engine current;
    private static PlayView view;
    private static boolean registered;
    private static List<ExperienceDto> listed;

    private PlayMcp() {
    }

    /// The engine a play session shows, or null when it ends.
    static void setEngine(Engine e, PlayView v) {
        current = e;
        view = v;
        if (e != null) {
            install();
        }
    }

    /// The experiences Home last listed, which `brickyard_open` finds a
    /// title in; registers the tools the first time the agent is running.
    public static void catalog(List<ExperienceDto> all) {
        if (all != null) {
            listed = all;
        }
        install();
    }

    private static void install() {
        if (!registered && MCP.isRunning()) {
            registered = true;
            register();
        }
    }

    private static void register() {
        MCP.addTool(new Tool("brickyard_open",
                "Opens the page of the experience with this title (as Home lists it), from any screen; a "
                        + "card scrolled out of view has no position a tap can reach.",
                "{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\"}},\"required\":[\"title\"]}",
                new ToolHandler() {
                    @Override
                    public String invoke(String args) {
                        final String title;
                        try {
                            Object t = JSONParser.parseJSON(args == null || args.length() == 0 ? "{}" : args)
                                    .get("title");
                            title = t == null ? "" : t.toString();
                        } catch (java.io.IOException err) {
                            return "{\"error\":\"bad arguments\"}";
                        }
                        final String[] out = {"{\"opened\":false}"};
                        CN.callSeriallyAndWait(new Runnable() {
                            @Override
                            public void run() {
                                List<ExperienceDto> all = listed;
                                if (all == null) {
                                    return;
                                }
                                for (ExperienceDto x : all) {
                                    if (x.title().equalsIgnoreCase(title)) {
                                        ExperienceForm.show(x.id());
                                        out[0] = "{\"opened\":true}";
                                        return;
                                    }
                                }
                            }
                        });
                        return out[0];
                    }
                }));
        MCP.addTool(new Tool("brickyard_play_state",
                "The local player in the running experience: position, whether the character is alive, "
                        + "the camera's facing and the leaderstats. Answers {\"playing\":false} outside a game.",
                "{\"type\":\"object\",\"properties\":{}}", new ToolHandler() {
                    @Override
                    public String invoke(String args) {
                        return onEdt(new Call() {
                            @Override
                            public String run(Engine e, Map<String, Object> a) {
                                return state(e);
                            }
                        }, args);
                    }
                }));
        MCP.addTool(new Tool("brickyard_play_parts",
                "Position, size and facing (rx/rz: the part's X axis on the ground) of every part in the workspace whose name starts with one of the given "
                        + "prefixes, e.g. [\"Stage\",\"Checkpoint\",\"Finish\"].",
                "{\"type\":\"object\",\"properties\":{\"prefixes\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},"
                        + "\"required\":[\"prefixes\"]}", new ToolHandler() {
                    @Override
                    public String invoke(String args) {
                        return onEdt(new Call() {
                            @Override
                            public String run(Engine e, Map<String, Object> a) {
                                return parts(e, a.get("prefixes"));
                            }
                        }, args);
                    }
                }));
        MCP.addTool(new Tool("brickyard_play_input",
                "Controller input for the local character. dx and dz are a WORLD direction to walk in (any "
                        + "length; zero stops), converted to the camera space a thumbstick uses. jump presses the "
                        + "jump button for a moment.",
                "{\"type\":\"object\",\"properties\":{\"dx\":{\"type\":\"number\"},\"dz\":{\"type\":\"number\"},"
                        + "\"jump\":{\"type\":\"boolean\"}}}", new ToolHandler() {
                    @Override
                    public String invoke(String args) {
                        return onEdt(new Call() {
                            @Override
                            public String run(Engine e, Map<String, Object> a) {
                                return input(e, a);
                            }
                        }, args);
                    }
                }));
    }

    private interface Call {
        String run(Engine e, Map<String, Object> args);
    }

    private static String onEdt(final Call c, String args) {
        final Map<String, Object> a;
        try {
            a = JSONParser.parseJSON(args == null || args.length() == 0 ? "{}" : args);
        } catch (java.io.IOException err) {
            return "{\"error\":\"bad arguments\"}";
        }
        final String[] out = new String[1];
        CN.callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                Engine e = current;
                out[0] = e == null ? "{\"playing\":false}" : c.run(e, a);
            }
        });
        return out[0];
    }

    private static String state(Engine e) {
        Instance me = e.getLocalPlayer();
        Instance ch = me == null ? null : me.ref("Character");
        Instance root = ch == null ? null : ch.findFirstChild("HumanoidRootPart");
        Instance hum = ch == null ? null : ch.findFirstChildOfClass("Humanoid", false);
        StringBuilder sb = new StringBuilder("{\"playing\":true");
        if (root != null && root.prop("CFrame") instanceof CFrame) {
            CFrame f = (CFrame) root.prop("CFrame");
            sb.append(",\"x\":").append(r(f.x)).append(",\"y\":").append(r(f.y)).append(",\"z\":").append(r(f.z));
        }
        Object hp = hum == null ? null : hum.prop("Health");
        sb.append(",\"alive\":").append(hp instanceof Double && ((Double) hp).doubleValue() > 0);
        CFrame cam = camera(e);
        Vector3 look = cam.look();
        sb.append(",\"cameraLook\":[").append(r(look.x)).append(',').append(r(look.y)).append(',')
                .append(r(look.z)).append(']');
        sb.append(",\"leaderstats\":{");
        Instance ls = me == null ? null : me.findFirstChild("leaderstats");
        if (ls != null) {
            boolean first = true;
            for (Instance v : ls.getChildren()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(esc(v.getName())).append("\":");
                Object val = v.prop("Value");
                sb.append(val instanceof Number ? String.valueOf(val) : "\"" + esc(String.valueOf(val)) + "\"");
            }
        }
        return sb.append("}}").toString();
    }

    private static String parts(Engine e, Object prefixes) {
        StringBuilder sb = new StringBuilder("{\"parts\":[");
        if (!(prefixes instanceof List)) {
            return sb.append("]}").toString();
        }
        List<?> pre = (List<?>) prefixes;
        boolean first = true;
        for (Instance d : e.getWorkspace().descendants()) {
            if (!d.isA("BasePart") || !matches(d.getName(), pre)) {
                continue;
            }
            Object cf = d.prop("CFrame");
            Object sz = d.prop("Size");
            if (!(cf instanceof CFrame) || !(sz instanceof Vector3)) {
                continue;
            }
            CFrame f = (CFrame) cf;
            Vector3 s = (Vector3) sz;
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"name\":\"").append(esc(d.getName())).append("\",\"x\":").append(r(f.x))
                    .append(",\"y\":").append(r(f.y)).append(",\"z\":").append(r(f.z))
                    .append(",\"sx\":").append(r(s.x)).append(",\"sy\":").append(r(s.y))
                    .append(",\"sz\":").append(r(s.z)).append(",\"rx\":").append(r(f.right().x))
                    .append(",\"rz\":").append(r(f.right().z)).append('}');
        }
        return sb.append("]}").toString();
    }

    private static boolean matches(String name, List<?> prefixes) {
        for (Object p : prefixes) {
            if (p instanceof String && name.startsWith((String) p)) {
                return true;
            }
        }
        return false;
    }

    private static String input(final Engine e, Map<String, Object> a) {
        double dx = num(a.get("dx"));
        double dz = num(a.get("dz"));
        CFrame cam = camera(e);
        Vector3 fwd = new Vector3(cam.look().x, 0, cam.look().z);
        Vector3 right = new Vector3(cam.right().x, 0, cam.right().z);
        if (fwd.magnitude() < 1e-6) {
            fwd = new Vector3(0, 0, -1);
            right = new Vector3(1, 0, 0);
        }
        fwd = fwd.unit();
        right = right.unit();
        Vector3 d = new Vector3(dx, 0, dz);
        if (d.magnitude() > 1) {
            d = d.unit();
        }
        if (view != null) {
            view.setStick(d.dot(right), d.dot(fwd));
        } else {
            Input.move(e, d.dot(right), d.dot(fwd));
        }
        // JSONParser answers a JSON boolean as a String unless asked otherwise.
        if ("true".equals(String.valueOf(a.get("jump")))) {
            Input.key(e, "Space", true);
            UITimer.timer(120, false, CN.getCurrentForm(), new Runnable() {
                @Override
                public void run() {
                    Input.key(e, "Space", false);
                }
            });
        }
        return "{\"ok\":true}";
    }

    private static CFrame camera(Engine e) {
        Instance cam = e.getWorkspace().findFirstChildOfClass("Camera", false);
        Object cf = cam == null ? null : cam.prop("CFrame");
        return cf instanceof CFrame ? (CFrame) cf : CFrame.IDENTITY;
    }

    private static double num(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        try {
            return o == null ? 0 : Double.parseDouble(o.toString());
        } catch (NumberFormatException err) {
            return 0;
        }
    }

    private static String r(double v) {
        return String.valueOf(Math.round(v * 100) / 100.0);
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c < 0x20 ? ' ' : c);
        }
        return sb.toString();
    }
}
