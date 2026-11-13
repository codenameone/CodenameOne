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

package com.codename1.guibuilder;

import com.codename1.ai.Tool;
import com.codename1.ai.ToolHandler;
import com.codename1.io.JSONParser;
import com.codename1.mcp.MCP;
import com.codename1.mcp.MCPServer;
import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GUI Builder domain tools layered over the portable MCP accessibility tools. */
final class GuiBuilderMcpController {
    private static final int MAX_EVENTS = 500;
    private final CodenameOneGUIBuilder builder;
    private final List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();
    private long sequence;

    GuiBuilderMcpController(CodenameOneGUIBuilder builder) {
        this.builder = builder;
    }

    void register() {
        MCPServer server = MCP.getServer();
        server.setServerInfo("codename-one-gui-builder", "8.0-SNAPSHOT");
        MCP.addTool(new Tool("guibuilder_state",
                "Returns the active GUI document, selected component, preview bounds, layout constraints, "
                        + "undo/redo state and current drag guide.",
                "{\"type\":\"object\",\"properties\":{}}", new ToolHandler() {
                    @Override public String invoke(String argumentsJson) {
                        return JSONParser.toJson(onEdtState());
                    }
                }));
        MCP.addTool(new Tool("guibuilder_actions",
                "Returns GUI Builder actions after a sequence number. timeoutMs optionally waits for live "
                        + "activity (maximum 10000ms). Use latestSequence as the next afterSequence value.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"afterSequence\":{\"type\":\"integer\"},"
                        + "\"timeoutMs\":{\"type\":\"integer\",\"minimum\":0,\"maximum\":10000},"
                        + "\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":500}}}",
                new ToolHandler() {
                    @Override public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> args = parse(argumentsJson);
                        long after = longValue(args.get("afterSequence"), 0);
                        int timeout = (int) Math.max(0, Math.min(10000,
                                longValue(args.get("timeoutMs"), 0)));
                        int limit = (int) Math.max(1, Math.min(MAX_EVENTS,
                                longValue(args.get("limit"), 100)));
                        return JSONParser.toJson(actionsAfter(after, timeout, limit));
                    }
                }));
        MCP.addTool(new Tool("guibuilder_select",
                "Selects a component in the active form by its GUI Builder name and returns fresh state. "
                        + "Set additive to true to add or toggle it in the current multi-selection.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"component\":{\"type\":\"string\"},"
                        + "\"additive\":{\"type\":\"boolean\"}},\"required\":[\"component\"]}",
                new ToolHandler() {
                    @Override public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> args = parse(argumentsJson);
                        final String component = JSONParser.getString(args, "component");
                        final boolean additive = booleanValue(args.get("additive"));
                        final boolean[] success = new boolean[1];
                        runOnEdt(new Runnable() {
                            @Override public void run() { success[0] = builder.mcpSelectComponent(component, additive); }
                        });
                        return resultWithState(success[0], success[0] ? null : "No component named " + component);
                    }
                }));
        MCP.addTool(new Tool("guibuilder_open_form",
                "Opens a project GUI form by its relative or simple name. Refuses to discard unsaved changes.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"form\":{\"type\":\"string\"}},\"required\":[\"form\"]}",
                new ToolHandler() {
                    @Override public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> args = parse(argumentsJson);
                        final String form = JSONParser.getString(args, "form");
                        final String[] error = new String[1];
                        runOnEdt(new Runnable() {
                            @Override public void run() { error[0] = builder.mcpOpenForm(form); }
                        });
                        return resultWithState(error[0] == null, error[0]);
                    }
                }));
        MCP.addTool(new Tool("guibuilder_drag",
                "Performs the GUI Builder's real pointer drag path for a named preview component. Supply "
                        + "absolute x/y coordinates from guibuilder_state, or a target and placement: "
                        + "center, before, after, above, below, leftOf, rightOf.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"component\":{\"type\":\"string\"},"
                        + "\"target\":{\"type\":\"string\"},"
                        + "\"placement\":{\"type\":\"string\",\"enum\":[\"center\",\"before\",\"after\",\"above\",\"below\",\"leftOf\",\"rightOf\"]},"
                        + "\"x\":{\"type\":\"integer\"},\"y\":{\"type\":\"integer\"}},"
                        + "\"required\":[\"component\"]}", new ToolHandler() {
                    @Override public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> args = parse(argumentsJson);
                        final String component = JSONParser.getString(args, "component");
                        final String target = JSONParser.getString(args, "target");
                        final String placement = JSONParser.getString(args, "placement");
                        final Integer x = integerOrNull(args.get("x"));
                        final Integer y = integerOrNull(args.get("y"));
                        final String[] error = new String[1];
                        runOnEdt(new Runnable() {
                            @Override public void run() {
                                error[0] = builder.mcpDragComponent(component, target, placement, x, y);
                            }
                        });
                        // A successful drop schedules a fresh preview. A second EDT turn runs after it.
                        return resultWithState(error[0] == null, error[0]);
                    }
                }));
        MCP.addTool(new Tool("guibuilder_command",
                "Runs a GUI Builder command and returns fresh state.",
                "{\"type\":\"object\",\"properties\":{\"command\":{\"type\":\"string\","
                        + "\"enum\":[\"save\",\"undo\",\"redo\",\"refresh\",\"toggleDarkMode\","
                        + "\"phonePortrait\",\"phoneLandscape\",\"tabletPortrait\",\"desktop\"]}},"
                        + "\"required\":[\"command\"]}", new ToolHandler() {
                    @Override public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> args = parse(argumentsJson);
                        final String command = JSONParser.getString(args, "command");
                        final String[] error = new String[1];
                        runOnEdt(new Runnable() {
                            @Override public void run() { error[0] = builder.mcpCommand(command); }
                        });
                        return resultWithState(error[0] == null, error[0]);
                    }
                }));
        registerEditingTools();
    }

    /**
     * The editing tools. Each is one undo step in the builder, validated exactly as the inspector
     * validates the same change, and answers with the operation's details and fresh state.
     */
    private void registerEditingTools() {
        editTool("guibuilder_component_types", "component_types", false,
                "Describes what the GUI Builder can build: every component type with the properties it accepts"
                        + " (kind, default, range, choices), the layouts and their constraints, guided-layout size"
                        + " policies and align actions, toolbar command placements and binding strategies. Read it"
                        + " before adding components or setting properties.",
                "{\"type\":\"object\",\"properties\":{}}");
        editTool("guibuilder_get_form_xml", "get_form_xml", false,
                "Returns the active form's .gui XML as it is in the editor, unsaved changes included, or one"
                        + " component's subtree when component is given. Attribute names are lower case.",
                "{\"type\":\"object\",\"properties\":{\"component\":{\"type\":\"string\"}}}");
        editTool("guibuilder_add_component", "add_component", true,
                "Adds a component, as the palette does, as one undo step. parent defaults to the form; index is"
                        + " the position among the parent's children (not for TableLayout). Optionally name it and"
                        + " set layout (for a Container: {layout, axis, rows, columns}), properties (see"
                        + " guibuilder_component_types) and constraint (as guibuilder_set_constraint takes) in the"
                        + " same step. A new Container defaults to LayeredLayout. Returns the component's name.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"type\":{\"type\":\"string\",\"enum\":" + json(PALETTE) + "},"
                        + "\"parent\":{\"type\":\"string\"},\"index\":{\"type\":\"integer\",\"minimum\":0},"
                        + "\"name\":{\"type\":\"string\"},\"layout\":{\"type\":\"object\"},"
                        + "\"properties\":{\"type\":\"object\"},\"constraint\":{\"type\":\"object\"}},"
                        + "\"required\":[\"type\"]}");
        editTool("guibuilder_insert_xml", "insert_xml", true,
                "Inserts a .gui fragment -- one <component> element with any nested components -- into parent"
                        + " (default: the form) as one undo step. Builds a whole section in one call. Names that"
                        + " are taken are made unique and references inside the fragment follow; the result lists"
                        + " the names used. Types must be palette types; a table child gets a free cell.",
                "{\"type\":\"object\",\"properties\":{\"xml\":{\"type\":\"string\"},"
                        + "\"parent\":{\"type\":\"string\"},\"index\":{\"type\":\"integer\",\"minimum\":0}},"
                        + "\"required\":[\"xml\"]}");
        editTool("guibuilder_set_properties", "set_properties", true,
                "Sets inspector properties of one component as one undo step, e.g. {\"text\":\"Sign in\","
                        + "\"uiid\":\"PrimaryButton\",\"enabled\":false}. null resets a property to its default."
                        + " Ranges and choices are checked; names, layouts, constraints, handlers and binding have"
                        + " their own tools.",
                "{\"type\":\"object\",\"properties\":{\"component\":{\"type\":\"string\"},"
                        + "\"properties\":{\"type\":\"object\"}},\"required\":[\"component\",\"properties\"]}");
        editTool("guibuilder_rename_component", "rename_component", true,
                "Renames a component and repoints every layout relationship that referred to it. The name"
                        + " becomes the generated Java field. Returns the name applied, which differs when the"
                        + " requested one is taken.",
                "{\"type\":\"object\",\"properties\":{\"component\":{\"type\":\"string\"},"
                        + "\"name\":{\"type\":\"string\"}},\"required\":[\"component\",\"name\"]}");
        editTool("guibuilder_delete_components", "delete_components", true,
                "Deletes components (and everything inside them) as one undo step. The form itself cannot be"
                        + " deleted.",
                "{\"type\":\"object\",\"properties\":{\"components\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},"
                        + "\"minItems\":1}},\"required\":[\"components\"]}");
        editTool("guibuilder_set_layout", "set_layout", true,
                "Sets a container's layout: BoxLayout (axis X or Y), BorderLayout (at most five children),"
                        + " FlowLayout, GridLayout (rows, columns 1-100), TableLayout (rows, columns; never below the"
                        + " cells already used) or LayeredLayout (guided). Omit layout to change only axis, rows or"
                        + " columns.",
                "{\"type\":\"object\",\"properties\":{\"container\":{\"type\":\"string\"},"
                        + "\"layout\":{\"type\":\"string\",\"enum\":" + json(LAYOUTS) + "},"
                        + "\"axis\":{\"type\":\"string\",\"enum\":[\"X\",\"Y\"]},"
                        + "\"rows\":{\"type\":\"integer\"},\"columns\":{\"type\":\"integer\"}},"
                        + "\"required\":[\"container\"]}");
        editTool("guibuilder_set_constraint", "set_constraint", true,
                "Places a component within its parent, by the parent's layout. Any layout except TableLayout:"
                        + " index (order among siblings). BorderLayout: region (North, South, East, West, Center;"
                        + " an occupant swaps places). TableLayout: row, column, rowSpan, columnSpan, columnWidth and"
                        + " rowHeight (percent, -1 automatic); the cells must be free. LayeredLayout (guided):"
                        + " reference (sibling name, null for nearest), insets (\"top right bottom left\", each auto,"
                        + " baseline or a number with px, mm or %), horizontalSize and verticalSize (preferred,"
                        + " fixed, fill, match) and align (alignLeft, alignHCenter, alignRight, alignTop,"
                        + " alignBaseline, alignBottom, matchWidth, matchHeight, fillWidth, fillHeight).",
                "{\"type\":\"object\",\"properties\":{\"component\":{\"type\":\"string\"},"
                        + "\"index\":{\"type\":\"integer\",\"minimum\":0},"
                        + "\"region\":{\"type\":\"string\",\"enum\":[\"North\",\"South\",\"East\",\"West\",\"Center\"]},"
                        + "\"row\":{\"type\":\"integer\"},\"column\":{\"type\":\"integer\"},"
                        + "\"rowSpan\":{\"type\":\"integer\"},\"columnSpan\":{\"type\":\"integer\"},"
                        + "\"columnWidth\":{\"type\":\"integer\"},\"rowHeight\":{\"type\":\"integer\"},"
                        + "\"reference\":{\"type\":[\"string\",\"null\"]},\"insets\":{\"type\":\"string\"},"
                        + "\"horizontalSize\":{\"type\":\"string\",\"enum\":[\"preferred\",\"fixed\",\"fill\",\"match\"]},"
                        + "\"verticalSize\":{\"type\":\"string\",\"enum\":[\"preferred\",\"fixed\",\"fill\",\"match\"]},"
                        + "\"align\":{\"type\":[\"string\",\"array\"],\"items\":{\"type\":\"string\"}}},"
                        + "\"required\":[\"component\"]}");
        editTool("guibuilder_set_event_handler", "set_event_handler", true,
                "Attaches an action handler method to a Button, CheckBox, RadioButton, TextField, TextArea or"
                        + " Slider (handler null removes it). Saving the form adds an empty"
                        + " protected void <handler>(ActionEvent event) to the companion source's user code region;"
                        + " the result names that file. Write the method body there.",
                "{\"type\":\"object\",\"properties\":{\"component\":{\"type\":\"string\"},"
                        + "\"handler\":{\"type\":[\"string\",\"null\"]}},\"required\":[\"component\",\"handler\"]}");
        editTool("guibuilder_toolbar_commands", "toolbar_commands", true,
                "Lists, adds, updates or removes the form's toolbar commands. A command has a label, a placement"
                        + " (left, right, overflow, side) and a handler method. update and remove take the index"
                        + " from list. Returns the commands.",
                "{\"type\":\"object\",\"properties\":{"
                        + "\"action\":{\"type\":\"string\",\"enum\":[\"list\",\"add\",\"update\",\"remove\"]},"
                        + "\"index\":{\"type\":\"integer\",\"minimum\":0},\"label\":{\"type\":\"string\"},"
                        + "\"placement\":{\"type\":\"string\",\"enum\":[\"left\",\"right\",\"overflow\",\"side\"]},"
                        + "\"handler\":{\"type\":\"string\"}}}");
        editTool("guibuilder_set_binding", "set_binding", true,
                "Sets the form's data binding strategy: none, properties (a PropertyBusinessObject model) or"
                        + " bindable (a @Bindable POJO). The <Form>Model class is created on save when missing; an"
                        + " existing model is kept, never overwritten.",
                "{\"type\":\"object\",\"properties\":{\"strategy\":{\"type\":\"string\","
                        + "\"enum\":[\"none\",\"properties\",\"bindable\"]}},\"required\":[\"strategy\"]}");
        editTool("guibuilder_create_form", "create_form", true,
                "Creates a new form (.gui plus its companion Java class), opens it and saves it so it compiles."
                        + " className is fully qualified, e.g. com.example.myapp.LoginForm. type: Form (default),"
                        + " Dialog or Container. layout defaults to BoxLayout (vertical). Refuses while the active"
                        + " form has unsaved changes and never overwrites an existing file.",
                "{\"type\":\"object\",\"properties\":{\"className\":{\"type\":\"string\"},"
                        + "\"type\":{\"type\":\"string\",\"enum\":[\"Form\",\"Dialog\",\"Container\"]},"
                        + "\"layout\":{\"type\":\"string\",\"enum\":" + json(LAYOUTS) + "}},"
                        + "\"required\":[\"className\"]}");
    }

    private static final String[] PALETTE = com.codename1.guibuilder.model.ComponentCatalog.PALETTE_TYPES;
    private static final String[] LAYOUTS = com.codename1.guibuilder.model.ComponentCatalog.LAYOUTS;

    private static String json(String[] values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            out.append('"').append(values[i]).append('"');
        }
        return out.append(']').toString();
    }

    private void editTool(String name, final String operation, final boolean includeState, String description,
            String schema) {
        MCP.addTool(new Tool(name, description, schema, new ToolHandler() {
            @Override public String invoke(String argumentsJson) throws Exception {
                return JSONParser.toJson(edit(operation, parseKeepingNulls(argumentsJson), includeState));
            }
        }));
    }

    /**
     * Runs one editing operation on the EDT.
     *
     * @param operation the builder operation
     * @param args the parsed arguments
     * @param includeState whether to append fresh state, as every mutating tool does
     * @return the result map: success, error, the operation's details and state
     */
    Map<String, Object> edit(final String operation, final Map<String, Object> args, boolean includeState) {
        final Map<String, Object>[] holder = new Map[1];
        runOnEdt(new Runnable() {
            @Override public void run() { holder[0] = builder.mcpEdit(operation, args); }
        });
        Map<String, Object> details = holder[0] == null ? new LinkedHashMap<String, Object>() : holder[0];
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Object error = details.remove("error");
        result.put("success", Boolean.valueOf(error == null));
        if (error != null) result.put("error", error);
        result.putAll(details);
        if (includeState) {
            // The rebuild queues the overlay refresh; cross one EDT turn so state is settled.
            runOnEdt(new Runnable() { @Override public void run() { } });
            result.put("state", onEdtState());
        }
        return result;
    }

    void record(String kind, Map<String, Object> details) {
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        synchronized (this) {
            event.put("sequence", Long.valueOf(++sequence));
            event.put("timestamp", Long.valueOf(System.currentTimeMillis()));
            event.put("kind", kind);
            if (details != null) event.putAll(details);
            events.add(event);
            while (events.size() > MAX_EVENTS) events.remove(0);
            notifyAll();
        }
    }

    long latestSequence() {
        synchronized (this) { return sequence; }
    }

    private Map<String, Object> actionsAfter(long after, int timeout, int limit) throws InterruptedException {
        synchronized (this) {
            if (sequence <= after && timeout > 0) wait(timeout);
            List<Object> outEvents = new ArrayList<Object>();
            // The cursor has to be the last event actually handed over, not the global sequence:
            // clients are told to feed latestSequence back as afterSequence, so reporting the
            // global one would silently skip every event this page could not fit.
            long delivered = after;
            for (int i = 0; i < events.size() && outEvents.size() < limit; i++) {
                Map<String, Object> event = events.get(i);
                if (longValue(event.get("sequence"), 0) > after) {
                    outEvents.add(new LinkedHashMap<String, Object>(event));
                    delivered = longValue(event.get("sequence"), delivered);
                }
            }
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("latestSequence", Long.valueOf(outEvents.isEmpty() ? sequence : delivered));
            out.put("pendingEvents", Boolean.valueOf(delivered < sequence));
            out.put("events", outEvents);
            return out;
        }
    }

    private String resultWithState(boolean success, String error) {
        final Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", Boolean.valueOf(success));
        if (error != null) result.put("error", error);
        // A command queues refreshEditor(), which in turn queues the overlay refresh. Cross one
        // EDT barrier before requesting state so both generations have completed and MCP never
        // reports a new component rectangle with a stale selection rectangle.
        runOnEdt(new Runnable() { @Override public void run() { } });
        result.put("state", onEdtState());
        return JSONParser.toJson(result);
    }

    private Map<String, Object> onEdtState() {
        final Map<String, Object>[] holder = new Map[1];
        runOnEdt(new Runnable() {
            @Override public void run() { holder[0] = builder.mcpState(latestSequence()); }
        });
        return holder[0];
    }

    private static void runOnEdt(Runnable runnable) {
        Display display = Display.getInstance();
        if (display.isEdt()) runnable.run(); else display.callSeriallyAndWait(runnable);
    }

    private static Map<String, Object> parse(String json) throws Exception {
        if (json == null || json.length() == 0) return new LinkedHashMap<String, Object>();
        Map<String, Object> parsed = JSONParser.parseJSON(json);
        return parsed == null ? new LinkedHashMap<String, Object>() : parsed;
    }

    /**
     * Parses arguments with explicit nulls kept. The editing tools give null a meaning -- reset a
     * property, remove a handler, clear a reference -- and the default parser drops null members,
     * which turned "reset text" into an empty request.
     */
    private static Map<String, Object> parseKeepingNulls(String json) throws Exception {
        if (json == null || json.length() == 0) return new LinkedHashMap<String, Object>();
        JSONParser parser = new JSONParser();
        parser.setIncludeNullsInstance(true);
        Map<String, Object> parsed = parser.parseJSON(new java.io.InputStreamReader(
                new java.io.ByteArrayInputStream(json.getBytes("UTF-8")), "UTF-8"));
        return parsed == null ? new LinkedHashMap<String, Object>() : parsed;
    }

    private static long longValue(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    private static boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static Integer integerOrNull(Object value) {
        return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : null;
    }
}
