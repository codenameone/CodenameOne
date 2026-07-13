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
import com.codename1.ui.Form;
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

    static List<Tool> builtInTools() {
        List<Tool> tools = new ArrayList<Tool>();

        tools.add(new Tool("ui_snapshot",
                "Returns the accessibility semantics tree of the current screen as JSON: every "
                        + "visible node with its id, role, label, value, state and the ids of the "
                        + "actions that can be performed on it.",
                "{\"type\":\"object\",\"properties\":{}}",
                new ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        return snapshotJson();
                    }
                }));

        tools.add(new Tool("ui_perform_action",
                "Performs an accessibility action on a node from ui_snapshot. actionId is one of the "
                        + "action ids listed on the node (for example activate, setText, increment, focus). "
                        + "Returns whether it succeeded and a fresh snapshot.",
                "{\"type\":\"object\",\"properties\":{"
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
                        Object argument = args == null ? null : args.get("argument");
                        return JSONParser.toJson(performAction(nodeId, actionId, argument));
                    }
                }));

        tools.add(new Tool("ui_activate",
                "Convenience wrapper that performs the activate action on a node (equivalent to a tap "
                        + "or click). Returns whether it succeeded and a fresh snapshot.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"nodeId\":{\"type\":\"integer\",\"description\":\"id of the node to activate\"}"
                        + "},\"required\":[\"nodeId\"]}",
                new ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> args = parse(argumentsJson);
                        return JSONParser.toJson(performAction(longArg(args, "nodeId", -1),
                                AccessibilityAction.ACTIVATE, null));
                    }
                }));

        tools.add(new Tool("ui_set_text",
                "Convenience wrapper that sets the text of an editable node (performs the setText "
                        + "action with the given text). Returns whether it succeeded and a fresh snapshot.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"nodeId\":{\"type\":\"integer\",\"description\":\"id of the editable node\"},"
                        + "\"text\":{\"type\":\"string\",\"description\":\"the text to set\"}"
                        + "},\"required\":[\"nodeId\",\"text\"]}",
                new ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> args = parse(argumentsJson);
                        return JSONParser.toJson(performAction(longArg(args, "nodeId", -1),
                                AccessibilityAction.SET_TEXT, JSONParser.getString(args, "text")));
                    }
                }));

        tools.add(new Tool("ui_find",
                "Finds nodes on the current screen by application identifier, by a case insensitive "
                        + "substring of their label, or by a screen coordinate. Returns a compact JSON array "
                        + "of matches with their id, role, label and available action ids.",
                "{\"type\":\"object\",\"properties\":{"
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
                                (int) longArg(args, "y", Integer.MIN_VALUE));
                    }
                }));

        return tools;
    }

    static String snapshotJson() {
        final String[] holder = new String[1];
        runOnEdt(new Runnable() {
            @Override
            public void run() {
                holder[0] = currentSnapshot().toJson();
            }
        });
        return holder[0];
    }

    static Map<String, Object> performAction(final long nodeId, final String actionId, final Object argument) {
        final Map<String, Object> out = new LinkedHashMap<String, Object>();
        runOnEdt(new Runnable() {
            @Override
            public void run() {
                AccessibilityManager mgr = AccessibilityManager.getInstance();
                AccessibilityTreeSnapshot snap = currentSnapshot();
                AccessibilityNodeSnapshot node = snap.getNode(nodeId);
                AccessibilityAction action = node == null ? null : node.getAction(actionId);
                boolean ok = false;
                if (node != null && action != null && action.isEnabled()) {
                    ok = action.perform(node.getComponent(), argument);
                    mgr.invalidate(node.getComponent(), AccessibilityManager.CHANGE_STATE
                            | AccessibilityManager.CHANGE_VALUE | AccessibilityManager.CHANGE_CONTENT
                            | AccessibilityManager.CHANGE_STRUCTURE);
                }
                out.put("success", Boolean.valueOf(ok));
                if (node == null) {
                    out.put("error", "no node with id " + nodeId);
                } else if (action == null) {
                    out.put("error", "node " + nodeId + " has no action " + actionId);
                }
                out.put("snapshot", JSONParser.rawJson(currentSnapshot().toJson()));
            }
        });
        return out;
    }

    static String findNodes(final String identifier, final String label, final int x, final int y) {
        final List<Object> matches = new ArrayList<Object>();
        runOnEdt(new Runnable() {
            @Override
            public void run() {
                AccessibilityTreeSnapshot snap = currentSnapshot();
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

    /// Renders the current form to a PNG. Returns null when no form is showing or the
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
        Form form = Display.getInstance().getCurrent();
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
        for (int i = 0; i < node.getActions().size(); i++) {
            actions.add(node.getActions().get(i).getId());
        }
        out.put("actions", actions);
        matches.add(out);
    }

    private static AccessibilityTreeSnapshot currentSnapshot() {
        return AccessibilityManager.getInstance().getSnapshot(Display.getInstance().getCurrent());
    }

    private static void runOnEdt(Runnable r) {
        Display display = Display.getInstance();
        if (display.isEdt()) {
            r.run();
        } else {
            display.callSeriallyAndWait(r);
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
