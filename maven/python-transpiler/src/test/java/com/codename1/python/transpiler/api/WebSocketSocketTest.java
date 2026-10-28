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
package com.codename1.python.transpiler.api;

import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Compiles the generated server's websocket `Socket` against a fake server
/// and drives its handshake and message state machine.
class WebSocketSocketTest {
    private static final String STUBS = String.join("\n",
            "import java.util.*;",
            "interface WebSocket {}",
            "interface WebSocketSession {",
            "    void sendText(String s) throws java.io.IOException;",
            "    void sendBinary(byte[] b) throws java.io.IOException;",
            "    void close(int c, String r) throws java.io.IOException;",
            "    void abort();",
            "}",
            "class HttpServer {",
            "    static final WebSocket REFUSED = new WebSocket() {};",
            "    static WebSocket refuseUpgrade(int a, String b) { return REFUSED; }",
            "    static WebSocket refuseUpgrade(Response r) { return REFUSED; }",
            "    static class Response { Response(int s, String t, byte[] b, Map m) {} }",
            "}",
            "class PyServer {",
            "    interface WebSocketSink {}",
            "    static final StringBuffer LOG = new StringBuffer();",
            "    static volatile Object sink;",
            "    static volatile boolean closeAtOnce;",
            "    static Object openWebSocket(Object a, String b, String[] c, Object d) {",
            "        sink = d;",
            "        if (closeAtOnce) { ((Outer.Socket) d).close(1000, \"\"); }",
            "        return \"conn\";",
            "    }",
            "    static void webSocketClosed(Object c, int code, String r) { LOG.append(\"closed \" + code + \";\"); }",
            "    static void webSocketText(Object c, String m) {}",
            "    static void webSocketBinary(Object c, byte[] b) {}",
            "    static void pauseWaiter(int a) { Thread.yield(); }",
            "    static RuntimeException webSocketGone(String why) { return new IllegalStateException(why); }",
            "}",
            "class Outer {");

    private static final String DRIVER = String.join("\n",
            "public class Driver {",
            "    static String gone(Runnable r) {",
            "        try { r.run(); return \"no error\"; } catch (IllegalStateException e) { return e.getMessage(); }",
            "    }",
            "    public static String run() throws Exception {",
            "        StringBuilder out = new StringBuilder();",
            // Early messages keep their order across onOpen, sent concurrently.
            "        final List<String> sent = Collections.synchronizedList(new ArrayList<String>());",
            "        final WebSocketSession session = new WebSocketSession() {",
            "            public void sendText(String s) { sent.add(s); }",
            "            public void sendBinary(byte[] b) {}",
            "            public void close(int c, String r) { sent.add(\"close\" + c); }",
            "            public void abort() {}",
            "        };",
            "        for (int round = 0; round < 50; round++) {",
            "            sent.clear();",
            "            final Outer.Socket s = new Outer.Socket(null, \"/\", new String[0]);",
            "            Thread t = new Thread(new Runnable() { public void run() { s.handshake(); } });",
            "            t.start();",
            "            while (PyServer.sink != s) { Thread.yield(); }",
            "            s.accept(null, new String[0]);",
            "            t.join();",
            "            s.text(\"0\");",
            "            Thread opener = new Thread(new Runnable() { public void run() { s.onOpen(session); } });",
            "            opener.start();",
            "            for (int i = 1; i < 20; i++) { s.text(String.valueOf(i)); }",
            "            s.close(1000, \"\");",
            "            opener.join();",
            "            StringBuilder want = new StringBuilder();",
            "            for (int i = 0; i < 20; i++) { want.append(i).append(','); }",
            "            want.append(\"close1000,\");",
            "            StringBuilder got = new StringBuilder();",
            "            for (String m : sent) { got.append(m).append(','); }",
            "            if (!want.toString().equals(got.toString())) { return \"out of order: \" + got; }",
            "        }",
            "        out.append(\"ordered;\");",
            // A late accept after the handshake timed out.
            "        PyServer.LOG.setLength(0);",
            "        final Outer.Socket late = new Outer.Socket(null, \"/\", new String[0]);",
            "        late.handshakeMillis = 30;",
            "        out.append(late.handshake() == HttpServer.REFUSED).append(';').append(PyServer.LOG);",
            "        out.append(gone(new Runnable() { public void run() { late.accept(null, new String[0]); } })).append(';');",
            "        out.append(gone(new Runnable() { public void run() { late.text(\"x\"); } })).append(';');",
            // Closed before accepting: refused at once, sends raise.
            "        PyServer.closeAtOnce = true;",
            "        PyServer.LOG.setLength(0);",
            "        final Outer.Socket shut = new Outer.Socket(null, \"/\", new String[0]);",
            "        out.append(shut.handshake() == HttpServer.REFUSED).append(';').append(PyServer.LOG);",
            "        out.append(gone(new Runnable() { public void run() { shut.text(\"x\"); } }));",
            "        return out.toString();",
            "    }",
            "}");

    @Test
    void stateMachine() throws Exception {
        Path dir = Files.createTempDirectory("ws-socket");
        String src = STUBS + "\n" + PythonTranspiler.webSocketSocketSource() + "}\n" + DRIVER + "\n";
        Path file = dir.resolve("Driver.java");
        Files.write(file, src.getBytes(StandardCharsets.UTF_8));
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertEquals(0, javac.run(null, null, null, "-nowarn", "-d", dir.toString(), file.toString()));
        try (URLClassLoader loader = new URLClassLoader(new URL[] {dir.toUri().toURL()}, null)) {
            Object r = loader.loadClass("Driver").getMethod("run").invoke(null);
            assertEquals("ordered;true;closed 1006;websocket handshake timed out before accept;"
                    + "websocket handshake timed out;true;closed 1006;websocket was refused", r);
        }
    }
}
