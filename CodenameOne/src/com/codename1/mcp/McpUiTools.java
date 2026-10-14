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
package com.codename1.mcp;

import com.codename1.ai.Tool;
import com.codename1.ai.ToolHandler;
import com.codename1.io.JSONParser;
import com.codename1.ui.Display;
import com.codename1.ui.Desktop;
import com.codename1.ui.Window;
import com.codename1.ui.Component;
import com.codename1.ui.TopLevelContainer;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityManager;
import com.codename1.ui.accessibility.AccessibilityNodeSnapshot;
import com.codename1.ui.accessibility.AccessibilityTreeSnapshot;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.util.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Built in MCP tools that turn any Codename One application into an agent drivable
/// surface with no application code. They wrap the portable accessibility semantics
/// tree: {@link AccessibilityTreeSnapshot#toJson()} reads the current screen and
/// {@link AccessibilityAction} entries drive it. All tree access is marshalled onto
/// the Codename One EDT because the live component hierarchy must never be walked off
/// thread.
final class McpUiTools {
    private McpUiTools() {
    }

    private static final String WINDOW_PROPERTY = "\"windowId\":{\"type\":\"integer\",\"description\":\"Optional window id from ui_windows; 0 is the main form, omitted uses current focus\"}";

    static List<Tool> builtInTools() {
        List<Tool> tools = new ArrayList<Tool>();

        tools.add(new Tool("ui_windows", "List application windows with ids, titles, visibility, focus and modal blocking. Use windowId with UI tools to address a dialog explicitly.",
                "{\"type\":\"object\",\"properties\":{}}", new ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        final List<Object> windows = new ArrayList<Object>();
                        runOnEdt(new Runnable() {
                            @Override
                            public void run() {
                                TopLevelContainer main = Display.getInstance().getCurrent();
                                if (main != null) {
                                    windows.add(windowInfo(0, main));
                                }
                                if (Desktop.isSupported()) {
                                    for (Window window : Desktop.getInstance().getWindows()) {
                                        windows.add(windowInfo(window.getWindowId(), window));
                                    }
                                }
                            }
                        });
                        return JSONParser.toJson(windows);
                    }
                }));

        tools.add(new Tool("ui_snapshot",
                "Returns the accessibility semantics tree of the current screen as JSON: every "
                        + "visible node with its id, role, label, value, state and the ids of the "
                        + "actions that can be performed on it.",
                "{\"type\":\"object\",\"properties\":{" + WINDOW_PROPERTY + "}}",
                new ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) throws Exception {
                        return snapshotJson((int) longArg(parse(argumentsJson), "windowId", -1));
                    }
                }));

        tools.add(new Tool("ui_perform_action",
                "Performs an accessibility action on a node from ui_snapshot. actionId is one of the "
                        + "action ids listed on the node (for example activate, setText, increment, focus). "
                        + "Returns success and a fresh snapshot. If a modal action is still running, "
                        + "returns pending:true without a completed success result; use subsequent UI calls to interact with the dialog.",
                "{\"type\":\"object\",\"properties\":{" + WINDOW_PROPERTY + ","
                        + "\"nodeId\":{\"type\":\"integer\",\"description\":\"id of the target node\"},"
                        + "\"actionId\":{\"type\":\"string\",\"description\":\"id of the action to perform\"},"
                        + "\"argument\":{\"type\":\"string\",\"description\":\"optional argument, e.g. text for setText\"}"
                        + "},\"required\":[\"nodeId\",\"actionId\"]}",
                new ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> args = parse(argumentsJson);
                        long nodeId = longArg(args, "nodeId", -1);
                        String actionId = JSONParser.getString(args, "actionId");
                        Object argument = args.get("argument");
                        return JSONParser.toJson(performAction(nodeId, actionId, argument, (int) longArg(args, "windowId", -1)));
                    }
                }));

        tools.add(new Tool("ui_activate",
                "Convenience wrapper that performs the activate action on a node (equivalent to a tap "
                        + "or click). Returns success and a fresh snapshot. Modal actions return pending:true "
                        + "without a completed success result so subsequent calls can interact with the dialog.",
                "{\"type\":\"object\",\"properties\":{" + WINDOW_PROPERTY + ","
                        + "\"nodeId\":{\"type\":\"integer\",\"description\":\"id of the node to activate\"}"
                        + "},\"required\":[\"nodeId\"]}",
                new ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> args = parse(argumentsJson);
                        return JSONParser.toJson(performAction(longArg(args, "nodeId", -1),
                                AccessibilityAction.ACTIVATE, null, (int) longArg(args, "windowId", -1)));
                    }
                }));

        tools.add(new Tool("ui_set_text",
                "Convenience wrapper that sets the text of an editable node (performs the setText "
                        + "action with the given text). Returns whether it succeeded and a fresh snapshot.",
                "{\"type\":\"object\",\"properties\":{" + WINDOW_PROPERTY + ","
                        + "\"nodeId\":{\"type\":\"integer\",\"description\":\"id of the editable node\"},"
                        + "\"text\":{\"type\":\"string\",\"description\":\"the text to set\"}"
                        + "},\"required\":[\"nodeId\",\"text\"]}",
                new ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> args = parse(argumentsJson);
                        return JSONParser.toJson(performAction(longArg(args, "nodeId", -1),
                                AccessibilityAction.SET_TEXT, JSONParser.getString(args, "text"), (int) longArg(args, "windowId", -1)));
                    }
                }));

        tools.add(new Tool("ui_find",
                "Finds nodes on the current screen by application identifier, by a case insensitive "
                        + "substring of their label, or by a screen coordinate. Returns a compact JSON array "
                        + "of matches with their id, role, label and available action ids.",
                "{\"type\":\"object\",\"properties\":{" + WINDOW_PROPERTY + ","
                        + "\"identifier\":{\"type\":\"string\",\"description\":\"exact application identifier\"},"
                        + "\"label\":{\"type\":\"string\",\"description\":\"label substring to search for\"},"
                        + "\"x\":{\"type\":\"integer\",\"description\":\"screen x coordinate\"},"
                        + "\"y\":{\"type\":\"integer\",\"description\":\"screen y coordinate\"}"
                        + "}}",
                new ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> args = parse(argumentsJson);
                        return findNodes(JSONParser.getString(args, "identifier"),
                                JSONParser.getString(args, "label"),
                                (int) longArg(args, "x", Integer.MIN_VALUE),
                                (int) longArg(args, "y", Integer.MIN_VALUE), (int) longArg(args, "windowId", -1));
                    }
                }));

        return tools;
    }

    static String snapshotJson() {
        return snapshotJson(-1);
    }

    private static String snapshotJson(final int windowId) {
        final String[] holder = new String[1];
        runOnEdt(new Runnable() {
            @Override
            public void run() {
                holder[0] = currentSnapshot(windowId).toJson();
            }
        });
        return holder[0];
    }

    static Map<String, Object> performAction(final long nodeId, final String actionId, final Object argument, final int windowId) {
        final Map<String, Object> outcome = new LinkedHashMap<String, Object>();
        final boolean[] complete = new boolean[1];
        Runnable perform = new Runnable() {
            @Override
            public void run() {
                try {
                    AccessibilityManager mgr = AccessibilityManager.getInstance();
                    AccessibilityTreeSnapshot snap = currentSnapshot(windowId);
                    AccessibilityNodeSnapshot node = snap.getNode(nodeId);
                    AccessibilityAction action = node == null ? null : node.getAction(actionId);
                    boolean ok = false;
                    TopLevelContainer top = resolveWindow(windowId);
                    int targetId = top instanceof Window ? ((Window) top).getWindowId() : 0;
                    if (Desktop.isSupported() && Desktop.getInstance().isWindowInputBlocked(targetId)) {
                        throw new IllegalStateException("Window is blocked by a modal dialog; inspect ui_windows");
                    }
                    if (node != null && action != null && action.isEnabled()) {
                        ok = action.perform(node.getComponent(), argument);
                        mgr.invalidate(node.getComponent(), AccessibilityManager.CHANGE_STATE
                                | AccessibilityManager.CHANGE_VALUE | AccessibilityManager.CHANGE_CONTENT
                                | AccessibilityManager.CHANGE_STRUCTURE);
                    }
                    outcome.put("success", Boolean.valueOf(ok));
                    if (node == null) {
                        outcome.put("error", "no node with id " + nodeId);
                    } else if (action == null) {
                        outcome.put("error", "node " + nodeId + " has no action " + actionId);
                    }
                } catch (RuntimeException ex) {
                    outcome.put("success", Boolean.FALSE);
                    outcome.put("error", ex.toString());
                } finally {
                    complete[0] = true;
                }
            }
        };
        final Display display = Display.getInstance();
        final Map<String, Object> out = new LinkedHashMap<String, Object>();
        final Object lock = new Object();
        final boolean[] ready = new boolean[1];
        final Runnable snapshot = new Runnable() {
            @Override
            public void run() {
                try {
                    out.putAll(outcome);
                    out.put("pending", Boolean.valueOf(!complete[0]));
                    if (!complete[0]) {
                        out.put("success", null);
                    }
                    int snapshotWindow = windowId;
                    if (snapshotWindow > 0) {
                        Window window = Desktop.getInstance().windowById(snapshotWindow);
                        if (window == null || !window.isWindowShowing()) {
                            snapshotWindow = -1;
                        }
                    }
                    out.put("snapshot", JSONParser.rawJson(currentSnapshot(snapshotWindow).toJson()));
                } catch (RuntimeException ex) {
                    out.put("success", Boolean.FALSE);
                    out.put("error", ex.toString());
                } finally {
                    synchronized (lock) {
                        ready[0] = true;
                        lock.notifyAll();
                    }
                }
            }
        };
        if (display.isEdt()) {
            perform.run();
            snapshot.run();
        } else {
            final Runnable action = perform;
            display.callSerially(new Runnable() {
                @Override
                public void run() {
                    // Queue from INSIDE the action turn: otherwise both callbacks
                    // can land in one serial-call batch and the snapshot gets stuck
                    // behind the suspended modal action. Dialog's nested EDT loop
                    // can now run the snapshot and release the MCP reader.
                    display.callSerially(snapshot);
                    action.run();
                }
            });
            synchronized (lock) {
                while (!ready[0]) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ex) {
                        throw new RuntimeException("Interrupted while waiting for UI action", ex);
                    }
                }
            }
        }
        return out;
    }

    static String findNodes(final String identifier, final String label, final int x, final int y, final int windowId) {
        final List<Object> matches = new ArrayList<Object>();
        runOnEdt(new Runnable() {
            @Override
            public void run() {
                AccessibilityTreeSnapshot snap = currentSnapshot(windowId);
                if (identifier != null && identifier.length() > 0) {
                    addNode(matches, snap.getNodeByIdentifier(identifier));
                } else if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) {
                    addNode(matches, snap.getNodeAt(x, y));
                } else if (label != null && label.length() > 0) {
                    String needle = label.toLowerCase();
                    for (AccessibilityNodeSnapshot node : snap.getNodes().values()) {
                        String candidate = node.getLabel();
                        if (candidate != null && candidate.toLowerCase().indexOf(needle) >= 0) {
                            addNode(matches, node);
                        }
                    }
                }
            }
        });
        return JSONParser.toJson(matches);
    }

    /// Renders the focused top level to a PNG. Returns null when none is showing or the
    /// platform cannot encode PNG images. Runs on the EDT.
    static byte[] screenshotPng() {
        final byte[][] holder = new byte[1][];
        runOnEdt(new Runnable() {
            @Override
            public void run() {
                holder[0] = renderCurrentFormPng();
            }
        });
        return holder[0];
    }

    private static byte[] renderCurrentFormPng() {
        TopLevelContainer top = Display.getInstance().getCurrentTopLevel();
        Component form = top == null ? null : top.asContainer();
        ImageIO io = ImageIO.getImageIO();
        if (form == null || io == null) {
            return null;
        }
        int w = form.getWidth();
        int h = form.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        Image image = Image.createImage(w, h, 0xffffffff);
        Graphics g = image.getGraphics();
        form.paintComponent(g, true);
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            io.save(image, bo, ImageIO.FORMAT_PNG, 1);
            return bo.toByteArray();
        } catch (Exception ex) {
            return null;
        }
    }

    private static void addNode(List<Object> matches, AccessibilityNodeSnapshot node) {
        if (node == null) {
            return;
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", Long.valueOf(node.getId()));
        out.put("role", node.getRole().name());
        out.put("label", node.getLabel());
        out.put("value", node.getValue());
        out.put("identifier", node.getIdentifier());
        Rectangle b = node.getBounds();
        List<Object> bounds = new ArrayList<Object>();
        bounds.add(Integer.valueOf(b.getX()));
        bounds.add(Integer.valueOf(b.getY()));
        bounds.add(Integer.valueOf(b.getWidth()));
        bounds.add(Integer.valueOf(b.getHeight()));
        out.put("bounds", bounds);
        List<Object> actions = new ArrayList<Object>();
        for (AccessibilityAction action : node.getActions()) {
            actions.add(action.getId());
        }
        out.put("actions", actions);
        matches.add(out);
    }

    private static TopLevelContainer resolveWindow(int windowId) {
        if (windowId == -1) {
            return Display.getInstance().getCurrentTopLevel();
        }
        if (windowId == 0) {
            return Display.getInstance().getCurrent();
        }
        Window window = Desktop.getInstance().windowById(windowId);
        if (window == null || !window.isWindowShowing()) {
            throw new IllegalArgumentException("No visible window with id " + windowId);
        }
        return window;
    }

    private static Map<String, Object> windowInfo(int id, TopLevelContainer top) {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        info.put("windowId", Integer.valueOf(id));
        info.put("title", top.getTitle());
        info.put("visible", Boolean.valueOf(!(top instanceof Window) || ((Window) top).isWindowShowing()));
        info.put("focused", Boolean.valueOf(top == Display.getInstance().getCurrentTopLevel())); // NOPMD CompareObjectsWithEquals - focus identifies the exact window instance
        info.put("blocked", Boolean.valueOf(Desktop.isSupported() && Desktop.getInstance().isWindowInputBlocked(id)));
        return info;
    }

    private static AccessibilityTreeSnapshot currentSnapshot(int windowId) {
        return AccessibilityManager.getInstance().getSnapshot(resolveWindow(windowId));
    }

    private static void runOnEdt(final Runnable r) {
        Display display = Display.getInstance();
        if (display.isEdt()) {
            r.run();
        } else {
            final RuntimeException[] error = new RuntimeException[1];
            display.callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        r.run();
                    } catch (RuntimeException ex) {
                        error[0] = ex;
                    }
                }
            });
            if (error[0] != null) {
                throw error[0];
            }
        }
    }

    private static Map<String, Object> parse(String json) throws Exception {
        if (json == null || json.length() == 0) {
            return new LinkedHashMap<String, Object>();
        }
        Map<String, Object> parsed = JSONParser.parseJSON(json);
        return parsed == null ? new LinkedHashMap<String, Object>() : parsed;
    }

    private static long longArg(Map<String, Object> args, String key, long defaultValue) {
        if (args == null) {
            return defaultValue;
        }
        Object v = args.get(key);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        if (v instanceof String) {
            try {
                return Long.parseLong(((String) v).trim());
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
