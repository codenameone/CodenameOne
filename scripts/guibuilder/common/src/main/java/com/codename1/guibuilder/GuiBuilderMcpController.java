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
            for (int i = 0; i < events.size() && outEvents.size() < limit; i++) {
                Map<String, Object> event = events.get(i);
                if (longValue(event.get("sequence"), 0) > after) {
                    outEvents.add(new LinkedHashMap<String, Object>(event));
                }
            }
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("latestSequence", Long.valueOf(sequence));
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
