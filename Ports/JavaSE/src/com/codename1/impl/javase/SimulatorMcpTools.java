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
package com.codename1.impl.javase;

import com.codename1.ai.Tool;
import com.codename1.ai.ToolHandler;
import com.codename1.impl.javase.record.RecordingOptions;
import com.codename1.impl.javase.record.SimulatorRecorder;
import com.codename1.impl.javase.record.TouchOverlay;
import com.codename1.impl.javase.vkb.KeyboardController;
import com.codename1.io.JSONParser;
import com.codename1.mcp.MCPServer;
import com.codename1.ui.Display;
import com.codename1.ui.TopLevelContainer;
import com.codename1.ui.accessibility.AccessibilityManager;
import com.codename1.ui.accessibility.AccessibilityNodeSnapshot;
import com.codename1.ui.accessibility.AccessibilityTreeSnapshot;
import com.codename1.ui.geom.Rectangle;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// The simulator's own MCP tools: the simulated keyboard, touches that go through
/// the real pointer path, rotation and screen recording. Together with the core
/// `ui_*` tools they let an agent script a feature video end to end: start a
/// recording, tap and type with visible touches and keyboard, add captions, stop.
///
/// Registered only by the JavaSE port, so none of these exist on a device.
final class SimulatorMcpTools {

    private final JavaSEPort port;

    private SimulatorMcpTools(JavaSEPort port) {
        this.port = port;
    }

    static void register(JavaSEPort port, MCPServer server) {
        new SimulatorMcpTools(port).addAll(server);
    }

    private void addAll(MCPServer server) {
        server.addTool(new Tool("sim_keyboard_set",
                "Configures the simulated on-screen keyboard. mode: off (no keyboard, the desktop "
                        + "behaviour), auto (follows the skin's platform), ios or android. "
                        + "androidSoftInput: auto (decide per field like the Android port), resize "
                        + "(the app gets sizeChanged with the height above the keyboard) or pan (the "
                        + "screen shifts up). The keyboard appears when a text field starts editing.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"mode\":{\"type\":\"string\",\"enum\":[\"off\",\"auto\",\"ios\",\"android\"]},"
                        + "\"androidSoftInput\":{\"type\":\"string\",\"enum\":[\"auto\",\"resize\",\"pan\"]}}}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> a = parse(argumentsJson);
                        final KeyboardController kc = port.getKeyboardController();
                        final String mode = str(a, "mode");
                        final String soft = str(a, "androidSoftInput");
                        onEdt(new Runnable() {
                            public void run() {
                                if (mode != null) {
                                    kc.setMode(KeyboardController.Mode.valueOf(upper(mode)));
                                }
                                if (soft != null) {
                                    kc.setSoftInput(KeyboardController.SoftInput.valueOf(upper(soft)));
                                }
                            }
                        });
                        port.saveKeyboardPreferences();
                        return keyboardState();
                    }
                }));

        server.addTool(new Tool("sim_keyboard_state",
                "Returns the simulated keyboard state: mode, platform, whether it is showing, its "
                        + "height, layout, the Android soft input decision (resize or pan) and the "
                        + "screen size the application currently sees.",
                "{\"type\":\"object\",\"properties\":{}}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) {
                        return keyboardState();
                    }
                }));

        server.addTool(new Tool("sim_keyboard_type",
                "Types text into the field being edited by tapping the simulated keyboard, one "
                        + "character at a time, so a recording shows each key press. A field must be "
                        + "editing first (tap it with sim_tap). \\n presses return.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"text\":{\"type\":\"string\"},"
                        + "\"perKeyDelayMs\":{\"type\":\"integer\",\"description\":\"Pause between keys, default 90\"}},"
                        + "\"required\":[\"text\"]}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> a = parse(argumentsJson);
                        String text = str(a, "text");
                        if (text == null) {
                            throw new IllegalArgumentException("text is required");
                        }
                        long delay = num(a, "perKeyDelayMs", 90);
                        final KeyboardController kc = requireEditing();
                        for (int i = 0; i < text.length(); i++) {
                            final String ch = text.substring(i, i + 1);
                            onEdt(new Runnable() {
                                public void run() {
                                    kc.typeChar(ch);
                                }
                            });
                            Thread.sleep(Math.max(0, delay));
                        }
                        return keyboardState();
                    }
                }));

        server.addTool(new Tool("sim_keyboard_key",
                "Presses a special key on the simulated keyboard: return, next, done, backspace, "
                        + "shift or space. done ends editing (the iOS accessory bar's Done on number "
                        + "pads, the action key otherwise).",
                "{\"type\":\"object\",\"properties\":{\"key\":{\"type\":\"string\","
                        + "\"enum\":[\"return\",\"next\",\"done\",\"backspace\",\"shift\",\"space\"]}},"
                        + "\"required\":[\"key\"]}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        final String key = str(parse(argumentsJson), "key");
                        final KeyboardController kc = requireEditing();
                        final boolean[] ok = new boolean[1];
                        onEdt(new Runnable() {
                            public void run() {
                                ok[0] = kc.pressNamed(key == null ? "" : key);
                            }
                        });
                        if (!ok[0]) {
                            throw new IllegalArgumentException("No such key on this layout: " + key);
                        }
                        return keyboardState();
                    }
                }));

        server.addTool(new Tool("sim_tap",
                "Taps the simulated screen through the same path as a mouse click, so touch "
                        + "indicators, tap sounds and the simulated keyboard all react (ui_activate "
                        + "does not). Give x and y in screen pixels, or nodeId from ui_snapshot / "
                        + "ui_find to tap the centre of that node. holdMs makes it a long press.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"x\":{\"type\":\"integer\"},\"y\":{\"type\":\"integer\"},"
                        + "\"nodeId\":{\"type\":\"integer\"},"
                        + "\"holdMs\":{\"type\":\"integer\",\"description\":\"Default 60\"}}}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> a = parse(argumentsJson);
                        int[] p = point(a, "x", "y", "nodeId");
                        port.dispatchSimulatedPointer(MouseEvent.MOUSE_PRESSED, p[0], p[1]);
                        Thread.sleep(Math.max(0, num(a, "holdMs", 60)));
                        port.dispatchSimulatedPointer(MouseEvent.MOUSE_RELEASED, p[0], p[1]);
                        return ok("x", p[0], "y", p[1]);
                    }
                }));

        server.addTool(new Tool("sim_drag",
                "Drags (swipes) across the simulated screen through the real pointer path, from "
                        + "(x1, y1) to (x2, y2) in screen pixels over durationMs, with intermediate "
                        + "moves so scrolling has velocity and the touch trail shows.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"x1\":{\"type\":\"integer\"},\"y1\":{\"type\":\"integer\"},"
                        + "\"x2\":{\"type\":\"integer\"},\"y2\":{\"type\":\"integer\"},"
                        + "\"durationMs\":{\"type\":\"integer\",\"description\":\"Default 300\"}},"
                        + "\"required\":[\"x1\",\"y1\",\"x2\",\"y2\"]}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> a = parse(argumentsJson);
                        int x1 = (int) num(a, "x1", -1);
                        int y1 = (int) num(a, "y1", -1);
                        int x2 = (int) num(a, "x2", -1);
                        int y2 = (int) num(a, "y2", -1);
                        long duration = Math.max(16, num(a, "durationMs", 300));
                        int steps = (int) Math.max(2, duration / 16);
                        port.dispatchSimulatedPointer(MouseEvent.MOUSE_PRESSED, x1, y1);
                        for (int i = 1; i <= steps; i++) {
                            Thread.sleep(duration / steps);
                            int x = x1 + (x2 - x1) * i / steps;
                            int y = y1 + (y2 - y1) * i / steps;
                            port.dispatchSimulatedPointer(MouseEvent.MOUSE_DRAGGED, x, y);
                        }
                        port.dispatchSimulatedPointer(MouseEvent.MOUSE_RELEASED, x2, y2);
                        return ok("x", x2, "y", y2);
                    }
                }));

        server.addTool(new Tool("sim_rotate",
                "Rotates the simulated device to portrait or landscape, as the Rotate menu does.",
                "{\"type\":\"object\",\"properties\":{\"orientation\":{\"type\":\"string\","
                        + "\"enum\":[\"portrait\",\"landscape\"]}},\"required\":[\"orientation\"]}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        String o = str(parse(argumentsJson), "orientation");
                        port.rotateSimulator(!"landscape".equals(o));
                        return ok("portrait", port.isSimulatorPortrait() ? 1 : 0, "width",
                                Display.getInstance().getDisplayWidth());
                    }
                }));

        server.addTool(new Tool("sim_touch_indicators",
                "Configures how touches are drawn. style: none, dot or ripple. live: also draw them "
                        + "in the simulator window (recordings always get them unless style is none). "
                        + "color: #RRGGBB or #AARRGGBB. radius in screen pixels. trail: draw drag trails.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"style\":{\"type\":\"string\",\"enum\":[\"none\",\"dot\",\"ripple\"]},"
                        + "\"live\":{\"type\":\"boolean\"},\"color\":{\"type\":\"string\"},"
                        + "\"radius\":{\"type\":\"integer\"},\"trail\":{\"type\":\"boolean\"}}}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> a = parse(argumentsJson);
                        RecordingOptions o = new RecordingOptions();
                        TouchOverlay t = port.getTouchOverlay();
                        Map<String, Object> mapped = new LinkedHashMap<String, Object>();
                        if (a.get("style") != null) {
                            mapped.put("touches", a.get("style"));
                        }
                        if (a.get("color") != null) {
                            mapped.put("touchColor", a.get("color"));
                        }
                        o.apply(mapped);
                        if (a.get("style") != null) {
                            t.setStyle(o.getTouches());
                        }
                        if (a.get("color") != null) {
                            t.setColor(o.getTouchColor());
                        }
                        if (a.get("radius") instanceof Number) {
                            t.setRadius(((Number) a.get("radius")).intValue());
                        }
                        if (a.get("trail") != null) {
                            t.setShowTrail(RecordingOptions.bool(a.get("trail")));
                        }
                        if (a.get("live") != null) {
                            t.setLiveVisible(RecordingOptions.bool(a.get("live")));
                            port.saveKeyboardPreferences();
                        }
                        Map<String, Object> out = new LinkedHashMap<String, Object>();
                        out.put("style", t.getStyle().name());
                        out.put("live", Boolean.valueOf(t.isLiveVisible()));
                        out.put("radius", Integer.valueOf(t.getRadius()));
                        out.put("trail", Boolean.valueOf(t.isShowTrail()));
                        return JSONParser.toJson(out);
                    }
                }));

        server.addTool(new Tool("sim_record_start",
                "Starts recording the simulator screen to a video. format: mp4 (default), mov, webm "
                        + "or gif. frame: screen (the device screen only) or skin (inside the device "
                        + "frame). fps (default 30), scale (default 1). touches: none, dot or ripple "
                        + "(default ripple), touchTrail, touchColor, touchRadius. tapSound: mix a "
                        + "click into the audio track at every tap. audio: none or microphone. The "
                        + "application's own audio output is not captured. maxSeconds stops a "
                        + "forgotten recording (default 600). path defaults to "
                        + "~/CodenameOneRecordings. Needs ffmpeg; without it PNG frames are written.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"path\":{\"type\":\"string\"},"
                        + "\"format\":{\"type\":\"string\",\"enum\":[\"mp4\",\"mov\",\"webm\",\"gif\"]},"
                        + "\"frame\":{\"type\":\"string\",\"enum\":[\"screen\",\"skin\"]},"
                        + "\"fps\":{\"type\":\"integer\"},\"scale\":{\"type\":\"number\"},"
                        + "\"touches\":{\"type\":\"string\",\"enum\":[\"none\",\"dot\",\"ripple\"]},"
                        + "\"touchTrail\":{\"type\":\"boolean\"},\"touchColor\":{\"type\":\"string\"},"
                        + "\"touchRadius\":{\"type\":\"integer\"},\"tapSound\":{\"type\":\"boolean\"},"
                        + "\"audio\":{\"type\":\"string\",\"enum\":[\"none\",\"microphone\"]},"
                        + "\"maxSeconds\":{\"type\":\"integer\"}}}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        RecordingOptions o = new RecordingOptions();
                        o.apply(parse(argumentsJson));
                        TouchOverlay t = port.getTouchOverlay();
                        t.setStyle(o.getTouches());
                        t.setShowTrail(o.isTouchTrail());
                        t.setColor(o.getTouchColor());
                        t.setRadius(o.getTouchRadius());
                        SimulatorRecorder rec = port.getSimulatorRecorder();
                        rec.start(o);
                        Map<String, Object> out = new LinkedHashMap<String, Object>();
                        out.put("recording", Boolean.TRUE);
                        out.put("path", rec.getOutput().getAbsolutePath());
                        return JSONParser.toJson(out);
                    }
                }));

        server.addTool(new Tool("sim_record_stop",
                "Stops the recording and finishes the file. Returns its path, duration, frame "
                        + "count, size, whether it has audio, and a note when something was degraded "
                        + "(no ffmpeg, no microphone).",
                "{\"type\":\"object\",\"properties\":{}}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws IOException {
                        SimulatorRecorder.Result r = port.getSimulatorRecorder().stop();
                        Map<String, Object> out = new LinkedHashMap<String, Object>();
                        out.put("path", r.file.getAbsolutePath());
                        out.put("durationMs", Long.valueOf(r.durationMs));
                        out.put("frames", Integer.valueOf(r.frames));
                        out.put("width", Integer.valueOf(r.width));
                        out.put("height", Integer.valueOf(r.height));
                        out.put("audio", Boolean.valueOf(r.audio));
                        if (r.note != null) {
                            out.put("note", r.note);
                        }
                        return JSONParser.toJson(out);
                    }
                }));

        server.addTool(new Tool("sim_record_status",
                "Returns whether a recording is running, its path, elapsed time and frame count.",
                "{\"type\":\"object\",\"properties\":{}}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) {
                        SimulatorRecorder rec = port.getSimulatorRecorder();
                        Map<String, Object> out = new LinkedHashMap<String, Object>();
                        out.put("recording", Boolean.valueOf(rec.isRecording()));
                        if (rec.getOutput() != null) {
                            out.put("path", rec.getOutput().getAbsolutePath());
                        }
                        out.put("elapsedMs", Long.valueOf(rec.elapsed()));
                        out.put("frames", Integer.valueOf(rec.getFrameCount()));
                        return JSONParser.toJson(out);
                    }
                }));

        server.addTool(new Tool("sim_record_caption",
                "Shows a caption over the bottom of the recording from now for durationMs "
                        + "(default 2500). Only the video shows it, not the simulator window.",
                "{\"type\":\"object\",\"properties\":{\"text\":{\"type\":\"string\"},"
                        + "\"durationMs\":{\"type\":\"integer\"}},\"required\":[\"text\"]}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> a = parse(argumentsJson);
                        String text = str(a, "text");
                        if (text == null) {
                            throw new IllegalArgumentException("text is required");
                        }
                        port.getSimulatorRecorder().caption(text, num(a, "durationMs", 2500));
                        return ok("shown", 1, "durationMs", (int) num(a, "durationMs", 2500));
                    }
                }));

        server.addTool(new Tool("sim_wait",
                "Waits, so a scripted recording can pace itself (an animation finishing, a pause "
                        + "for the viewer). At most 30000 ms.",
                "{\"type\":\"object\",\"properties\":{\"ms\":{\"type\":\"integer\"}},\"required\":[\"ms\"]}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        long ms = Math.min(30000, Math.max(0, num(parse(argumentsJson), "ms", 0)));
                        Thread.sleep(ms);
                        return ok("waitedMs", (int) ms, "", 0);
                    }
                }));
    }

    private KeyboardController requireEditing() {
        KeyboardController kc = port.getKeyboardController();
        if (!kc.isEnabled()) {
            throw new IllegalStateException("The simulated keyboard is off; call sim_keyboard_set first");
        }
        if (!kc.isShowing()) {
            throw new IllegalStateException("No field is being edited; tap a text field with sim_tap first");
        }
        return kc;
    }

    private String keyboardState() {
        final KeyboardController kc = port.getKeyboardController();
        final Map<String, Object> out = new LinkedHashMap<String, Object>();
        onEdt(new Runnable() {
            public void run() {
                out.put("mode", kc.getMode().name());
                out.put("platform", kc.platform().name());
                out.put("showing", Boolean.valueOf(kc.isShowing()));
                out.put("height", Integer.valueOf(kc.getHeight()));
                out.put("layout", kc.getKeyboard().getLayout().name());
                out.put("androidSoftInput", kc.getSoftInput().name());
                if (kc.isShowing() && kc.platform() == com.codename1.impl.javase.vkb.SimulatedKeyboard.Platform.ANDROID) {
                    out.put("androidDecision", kc.isResizeMode() ? "RESIZE" : "PAN");
                    out.put("panOffset", Integer.valueOf(kc.getPanOffset()));
                }
                out.put("invisibleAreaUnderVKB", Integer.valueOf(Display.getInstance().getInvisibleAreaUnderVKB()));
                out.put("displayWidth", Integer.valueOf(Display.getInstance().getDisplayWidth()));
                out.put("displayHeight", Integer.valueOf(Display.getInstance().getDisplayHeight()));
            }
        });
        return JSONParser.toJson(out);
    }

    /// A tap point in screen pixels from x/y or from the centre of a node. Node
    /// bounds are in application coordinates, which the pan shift moves on screen.
    private int[] point(Map<String, Object> a, String xKey, String yKey, String nodeKey) {
        if (a.get(nodeKey) instanceof Number) {
            final long id = ((Number) a.get(nodeKey)).longValue();
            final Rectangle[] bounds = new Rectangle[1];
            onEdt(new Runnable() {
                public void run() {
                    TopLevelContainer top = Display.getInstance().getCurrentTopLevel();
                    AccessibilityTreeSnapshot snap = AccessibilityManager.getInstance().getSnapshot(top);
                    AccessibilityNodeSnapshot node = snap == null ? null : snap.getNode(id);
                    if (node != null) {
                        bounds[0] = node.getBounds();
                    }
                }
            });
            if (bounds[0] == null) {
                throw new IllegalArgumentException("No node " + id + " on the current screen");
            }
            Rectangle b = bounds[0];
            return new int[]{b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2 - port.simulatorPanOffset()};
        }
        if (!(a.get(xKey) instanceof Number) || !(a.get(yKey) instanceof Number)) {
            throw new IllegalArgumentException("Give x and y, or nodeId");
        }
        return new int[]{((Number) a.get(xKey)).intValue(), ((Number) a.get(yKey)).intValue()};
    }

    private static void onEdt(final Runnable r) {
        Display d = Display.getInstance();
        if (d.isEdt()) {
            r.run();
            return;
        }
        final RuntimeException[] err = new RuntimeException[1];
        d.callSeriallyAndWait(new Runnable() {
            public void run() {
                try {
                    r.run();
                } catch (RuntimeException e) {
                    err[0] = e;
                }
            }
        });
        if (err[0] != null) {
            throw err[0];
        }
    }

    private static Map<String, Object> parse(String json) throws IOException {
        if (json == null || json.trim().length() == 0) {
            return new LinkedHashMap<String, Object>();
        }
        Map<String, Object> m = JSONParser.parseJSON(json);
        return m == null ? new LinkedHashMap<String, Object>() : m;
    }

    private static String str(Map<String, Object> a, String key) {
        Object v = a.get(key);
        return v == null ? null : v.toString();
    }

    private static long num(Map<String, Object> a, String key, long def) {
        Object v = a.get(key);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return def;
    }

    /// ASCII upper-casing for enum names; locale independent on purpose.
    private static String upper(String s) {
        char[] c = s.trim().toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'a' && c[i] <= 'z') {
                c[i] = (char) (c[i] - ('a' - 'A'));
            }
        }
        return new String(c);
    }

    private static String ok(String k1, int v1, String k2, int v2) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("ok", Boolean.TRUE);
        out.put(k1, Integer.valueOf(v1));
        if (k2.length() > 0) {
            out.put(k2, Integer.valueOf(v2));
        }
        return JSONParser.toJson(out);
    }
}
