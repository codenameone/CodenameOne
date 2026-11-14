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
/// Sim -- drive a running Codename One app (simulator, iOS simulator, Android
/// emulator or device) from a shell, for AI agents and CI.
///
/// Java 17 single-file source program; needs nothing but a JDK:
///
///   java Sim.java wait [timeoutSeconds]        block until the app answers
///   java Sim.java snapshot                     the screen as JSON (ui_snapshot)
///   java Sim.java find <label text>            nodes whose label contains the text
///   java Sim.java tap <nodeId | label text>    tap a node (first label match)
///   java Sim.java type <nodeId | label> <text> type into an editable node
///   java Sim.java screenshot <out.png>         PNG of the current form
///   java Sim.java record-start <out.mp4|.gif>  start a video (simulator only, needs ffmpeg)
///   java Sim.java caption <text>               caption over the recording
///   java Sim.java record-stop                  finish the video, prints its path
///   java Sim.java tools                        list every tool the app publishes
///   java Sim.java call <tool> ['<json args>']  call any tool
///   java Sim.java device-screenshot <ios|android> <out.png>
///   java Sim.java device-record <ios|android> <out.mp4> <seconds>
///
/// The app must serve MCP on 127.0.0.1:<port>: start it with `./mvnw cn1:agent`
/// (Gradle: ./gradlew cn1Agent), or call MCP.startSocketServer(8765) in a
/// development build on a device (Android: adb forward tcp:8765 tcp:8765 first).
/// The port is --port, else CN1_MCP_PORT, else what cn1:agent wrote to
/// common/target/cn1-agent.port (Gradle: build/cn1-agent.port; run Sim from
/// the project root), else 8765.
///
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sim {
    private static int port = 8765;
    private static int nextId = 1;

    public static void main(String[] argv) throws Exception {
        List<String> args = new ArrayList<>(List.of(argv));
        String env = System.getenv("CN1_MCP_PORT");
        if (env != null && !env.isBlank()) {
            port = Integer.parseInt(env.trim());
        } else {
            // cn1:agent records the port it chose (8765, or a free one when 8765 is taken).
            for (String f : new String[] {"common/target/cn1-agent.port", "target/cn1-agent.port", "build/cn1-agent.port"}) {
                Path pf = Path.of(f);
                if (Files.isRegularFile(pf)) {
                    port = Integer.parseInt(Files.readString(pf).trim());
                    break;
                }
            }
        }
        int p = args.indexOf("--port");
        if (p >= 0 && p + 1 < args.size()) {
            port = Integer.parseInt(args.get(p + 1));
            args.remove(p + 1);
            args.remove(p);
        }
        if (args.isEmpty()) {
            usage();
            return;
        }
        try {
            dispatch(args);
        } catch (java.net.ConnectException ex) {
            System.err.println("Nothing is listening on 127.0.0.1:" + port + ". Start `./mvnw cn1:agent &` first"
                    + " (a cn1-agent.port file left by a run that was killed can point at a dead port; delete it).");
            System.exit(1);
        } catch (RpcError ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    /** A JSON-RPC error reply; {@code wait} retries on it, every other command reports it. */
    private static final class RpcError extends Exception {
        RpcError(String reply) {
            super(reply);
        }
    }

    private static void dispatch(List<String> args) throws Exception {
        String cmd = args.get(0);
        switch (cmd) {
            case "wait" -> waitReady(args.size() > 1 ? Integer.parseInt(args.get(1)) : 180);
            case "snapshot" -> print(callTool("ui_snapshot", "{}"));
            case "find" -> print(callTool("ui_find", "{\"label\":" + q(rest(args, 1)) + "}"));
            case "tap" -> print(callTool("ui_activate", "{\"nodeId\":" + resolveNode(need(args, 1)) + "}"));
            case "type" -> print(callTool("ui_set_text", "{\"nodeId\":" + resolveNode(need(args, 1))
                    + ",\"text\":" + q(rest(args, 2)) + "}"));
            case "screenshot" -> screenshot(need(args, 1));
            case "record-start" -> print(callTool("sim_record_start",
                    "{\"path\":" + q(Path.of(need(args, 1)).toAbsolutePath().toString()) + "}"));
            case "caption" -> print(callTool("sim_record_caption", "{\"text\":" + q(rest(args, 1)) + "}"));
            case "record-stop" -> print(callTool("sim_record_stop", "{}"));
            case "tools" -> print(request("tools/list", "{}"));
            case "call" -> print(callTool(need(args, 1), args.size() > 2 ? args.get(2) : "{}"));
            case "device-screenshot" -> deviceScreenshot(need(args, 1), need(args, 2));
            case "device-record" -> deviceRecord(need(args, 1), need(args, 2),
                    Integer.parseInt(need(args, 3)));
            default -> {
                usage();
                System.exit(2);
            }
        }
    }

    private static void usage() {
        System.err.println("usage: java Sim.java [--port N] wait|snapshot|find|tap|type|screenshot|"
                + "record-start|caption|record-stop|tools|call|device-screenshot|device-record ...");
        System.err.println("See the comment at the top of Sim.java for every command.");
    }

    private static String need(List<String> args, int i) {
        if (args.size() <= i) {
            usage();
            System.exit(2);
        }
        return args.get(i);
    }

    private static String rest(List<String> args, int from) {
        need(args, from);
        return String.join(" ", args.subList(from, args.size()));
    }

    /** A numeric id is used as is; anything else is looked up by label. */
    private static String resolveNode(String idOrLabel) throws Exception {
        if (idOrLabel.matches("\\d+")) {
            return idOrLabel;
        }
        String found = unwrap(callTool("ui_find", "{\"label\":" + q(idOrLabel) + "}"));
        Matcher m = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(found);
        if (!m.find()) {
            System.err.println("No node has a label containing \"" + idOrLabel + "\". Run `snapshot` to see the screen.");
            System.exit(1);
        }
        return m.group(1);
    }

    private static void waitReady(int seconds) throws Exception {
        long end = System.currentTimeMillis() + seconds * 1000L;
        Exception last = null;
        while (System.currentTimeMillis() < end) {
            try {
                // The server is up before the first form is shown: wait for a node on screen.
                String snap = callTool("ui_snapshot", "{}");
                if (!snap.contains("\"isError\":true") && unwrap(snap).contains("\"id\"")) {
                    System.out.println("ready on 127.0.0.1:" + port);
                    return;
                }
            } catch (Exception ex) {
                last = ex;
            }
            Thread.sleep(1000);
        }
        System.err.println("Nothing answered on 127.0.0.1:" + port + " within " + seconds + "s"
                + (last == null ? "" : " (" + last.getMessage() + ")")
                + ". Is `./mvnw cn1:agent` running? Read its output for a build error.");
        System.exit(1);
    }

    private static void screenshot(String out) throws Exception {
        String res = request("resources/read", "{\"uri\":\"cn1://screen.png\"}");
        Matcher m = Pattern.compile("\"blob\"\\s*:\\s*\"([^\"]+)\"").matcher(res);
        if (!m.find()) {
            System.err.println("No screenshot in the reply: " + res);
            System.exit(1);
        }
        Path path = Path.of(out).toAbsolutePath();
        Files.write(path, Base64.getDecoder().decode(m.group(1).replace("\\/", "/")));
        System.out.println(path);
    }

    private static void deviceScreenshot(String platform, String out) throws Exception {
        String path = Path.of(out).toAbsolutePath().toString();
        if (platform.equals("ios")) {
            run("xcrun", "simctl", "io", "booted", "screenshot", path);
        } else {
            ProcessBuilder pb = new ProcessBuilder("adb", "exec-out", "screencap", "-p");
            pb.redirectOutput(new java.io.File(path));
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            check(pb.start().waitFor(), "adb exec-out screencap");
        }
        System.out.println(path);
    }

    private static void deviceRecord(String platform, String out, int seconds) throws Exception {
        String path = Path.of(out).toAbsolutePath().toString();
        Process p;
        if (platform.equals("ios")) {
            p = new ProcessBuilder("xcrun", "simctl", "io", "booted", "recordVideo", "--force", path)
                    .inheritIO().start();
            Thread.sleep(seconds * 1000L);
            // simctl finishes the file on SIGINT, which destroy() does not send.
            new ProcessBuilder("kill", "-INT", String.valueOf(p.pid())).start().waitFor();
            p.waitFor();
        } else {
            String remote = "/sdcard/cn1-record.mp4";
            run("adb", "shell", "screenrecord", "--time-limit", String.valueOf(seconds), remote);
            run("adb", "pull", remote, path);
            run("adb", "shell", "rm", remote);
        }
        System.out.println(path);
    }

    private static void run(String... cmd) throws Exception {
        check(new ProcessBuilder(cmd).inheritIO().start().waitFor(), String.join(" ", cmd));
    }

    private static void check(int status, String what) {
        if (status != 0) {
            System.err.println(what + " exited with " + status);
            System.exit(status);
        }
    }

    private static String callTool(String name, String argsJson) throws Exception {
        return request("tools/call", "{\"name\":" + q(name) + ",\"arguments\":" + argsJson + "}");
    }

    /** One short-lived connection per call; the server accepts reconnects. */
    private static String request(String method, String paramsJson) throws Exception {
        try (Socket s = new Socket("127.0.0.1", port)) {
            s.setSoTimeout(120_000);
            OutputStream out = s.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            send(out, "{\"jsonrpc\":\"2.0\",\"id\":" + (nextId++) + ",\"method\":\"initialize\",\"params\":"
                    + "{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},"
                    + "\"clientInfo\":{\"name\":\"cn1-sim-cli\",\"version\":\"1\"}}}");
            in.readLine();
            send(out, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
            int id = nextId++;
            send(out, "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":" + q(method) + ",\"params\":" + paramsJson + "}");
            String line = in.readLine();
            if (line == null) {
                throw new java.io.IOException("connection closed without a reply");
            }
            if (line.contains("\"error\"") && !line.contains("\"result\"")) {
                throw new RpcError(line);
            }
            return line;
        }
    }

    private static void send(OutputStream out, String json) throws Exception {
        out.write((json + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /** tools/call wraps the tool's own JSON in content[0].text; this returns that JSON. */
    private static String unwrap(String reply) {
        Matcher m = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]++|\\\\.)*+)\"").matcher(reply);
        if (reply.contains("\"content\"") && m.find()) {
            return unescape(m.group(1));
        }
        return reply;
    }

    private static void print(String reply) {
        // Print the tool's answer, not the envelope, so the agent reads the screen.
        System.out.println(unwrap(reply));
        if (reply.contains("\"isError\":true")) {
            System.exit(1);
        }
    }

    private static String unescape(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 >= s.length()) {
                b.append(c);
                continue;
            }
            char n = s.charAt(++i);
            switch (n) {
                case 'n' -> b.append('\n');
                case 't' -> b.append('\t');
                case 'r' -> b.append('\r');
                case 'u' -> {
                    b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> b.append(n);
            }
        }
        return b.toString();
    }

    private static String q(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.append('"').toString();
    }
}
