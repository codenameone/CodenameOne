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
package com.codename1.maven.help;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ToolingHelpClientTest {

    private StubServer server;
    private String endpoint;

    @Before
    public void setUp() throws IOException {
        server = new StubServer();
        server.start();
        endpoint = "http://127.0.0.1:" + server.port() + "/api/v2/tooling/help";
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void pathA_withEmail_reportsEmailedWithTicketAndChatUrl() {
        server.respond(200, "{\"ticketId\": 4213, \"token\": \"b1f0-uuid\", "
                + "\"chatUrl\": \"https://cloud.codenameone.com/tooling-help.html\"}");
        ToolingHelpReport report = ToolingHelpReport.builder()
                .component("maven-plugin")
                .step("create_project")
                .email("dev@example.com")
                .message("Trying to make my first project")
                .build();

        ToolingHelpResponse response = new ToolingHelpClient(endpoint).submit(report);

        assertEquals(ToolingHelpResponse.Outcome.EMAILED, response.getOutcome());
        assertTrue(response.wasDelivered());
        assertEquals(Integer.valueOf(4213), response.getTicketId());
        assertEquals("b1f0-uuid", response.getToken());
        assertEquals("dev@example.com", response.getEmail());
        assertEquals("https://cloud.codenameone.com/tooling-help.html", response.getChatUrl());
        // Optional support-chat deep link carries the token as a fragment.
        assertEquals("https://cloud.codenameone.com/tooling-help.html#t=b1f0-uuid",
                response.getChatDeepLink());

        // The POST body must match the wire contract and actually carry the fields.
        assertNotNull(server.capturedBody);
        assertTrue(server.capturedRequest.contains("Content-Type: application/json"));
        assertTrue(server.capturedRequest.startsWith("POST "));
        // A non-default User-Agent must be sent; the production WAF 403s "Java/<ver>".
        assertTrue("must send a product User-Agent, not the JDK default",
                server.capturedRequest.contains("User-Agent: CodenameOne-Tooling-Help"));
        assertFalse(server.capturedRequest.contains("User-Agent: Java/"));
        assertTrue(server.capturedBody.contains("\"component\":\"maven-plugin\""));
        assertTrue(server.capturedBody.contains("\"step\":\"create_project\""));
        assertTrue(server.capturedBody.contains("\"email\":\"dev@example.com\""));
        assertTrue(server.capturedBody.contains("\"message\":\"Trying to make my first project\""));
    }

    @Test
    public void pathB_withoutEmail_reportsNoEmailWithTokenDeepLinkAndOmitsEmailOnWire() {
        server.respond(200, "{\"ticketId\": 99, \"token\": \"tok-xyz\", "
                + "\"chatUrl\": \"https://cloud.codenameone.com/tooling-help.html\"}");
        ToolingHelpReport report = ToolingHelpReport.builder()
                .component("maven-plugin")
                .step("install")
                .build();

        ToolingHelpResponse response = new ToolingHelpClient(endpoint).submit(report);

        assertEquals(ToolingHelpResponse.Outcome.NO_EMAIL, response.getOutcome());
        assertTrue(response.wasDelivered());
        assertNull(response.getEmail());
        assertEquals("tok-xyz", response.getToken());
        // The no-email path opens exactly this: chatUrl#t=token.
        assertEquals("https://cloud.codenameone.com/tooling-help.html#t=tok-xyz",
                response.getChatDeepLink());
        assertNotNull(server.capturedBody);
        assertFalse(server.capturedBody.contains("\"email\""));
    }

    @Test
    public void deepLinkFallsBackToBareChatUrlWhenNoToken() {
        server.respond(200, "{\"ticketId\": 99, \"chatUrl\": \"https://cloud.codenameone.com/tooling-help.html\"}");
        ToolingHelpResponse response = new ToolingHelpClient(endpoint)
                .submit(ToolingHelpReport.builder().step("install").build());
        assertEquals("https://cloud.codenameone.com/tooling-help.html", response.getChatDeepLink());
    }

    @Test
    public void serverError_degradesToUnreachable() {
        server.respond(500, "boom");
        ToolingHelpReport report = ToolingHelpReport.builder().step("build_submit").email("a@b.com").build();

        ToolingHelpResponse response = new ToolingHelpClient(endpoint).submit(report);

        assertEquals(ToolingHelpResponse.Outcome.UNREACHABLE, response.getOutcome());
        assertFalse(response.wasDelivered());
        assertEquals(ToolingHelpClient.helpArticleUrl("build_submit"), response.getHelpArticleUrl());
        assertEquals(ToolingHelpClient.DEFAULT_CHAT_URL, response.getChatUrl());
    }

    @Test
    public void networkFailure_neverThrows_returnsUnreachable() {
        ToolingHelpClient client = new ToolingHelpClient("http://127.0.0.1:1/api/v2/tooling/help");
        ToolingHelpResponse response = client.submit(
                ToolingHelpReport.builder().step("local_run").email("a@b.com").build());
        assertEquals(ToolingHelpResponse.Outcome.UNREACHABLE, response.getOutcome());
    }

    @Test
    public void helpArticleUrl_mapsKnownStepsAndFallsBack() {
        assertTrue(ToolingHelpClient.helpArticleUrl("configure").contains("codenameone.com"));
        assertEquals(ToolingHelpClient.helpArticleUrl("other"),
                ToolingHelpClient.helpArticleUrl("does-not-exist"));
        assertNotNull(ToolingHelpClient.helpArticleUrl(null));
    }

    @Test
    public void extractInt_and_extractString_parseFlatJson() {
        String json = "{\"ticketId\": 42, \"chatUrl\": \"https://x/y\"}";
        assertEquals(Integer.valueOf(42), ToolingHelpClient.extractInt(json, "ticketId"));
        assertEquals("https://x/y", ToolingHelpClient.extractString(json, "chatUrl"));
        assertNull(ToolingHelpClient.extractInt(json, "missing"));
        assertNull(ToolingHelpClient.extractString(json, "missing"));
    }

    /**
     * A minimal single-request-at-a-time HTTP/1.1 server over a raw socket. Avoids
     * depending on com.sun.net.httpserver (which can trip the compiler symbol-file
     * restriction at older -target levels).
     */
    private static final class StubServer {
        private ServerSocket socket;
        private Thread thread;
        private volatile int status = 200;
        private volatile String body = "{}";
        volatile String capturedRequest;
        volatile String capturedBody;

        void respond(int status, String body) {
            this.status = status;
            this.body = body;
        }

        int port() {
            return socket.getLocalPort();
        }

        void start() throws IOException {
            socket = new ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"));
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    serveLoop();
                }
            });
            thread.setDaemon(true);
            thread.start();
        }

        private void serveLoop() {
            while (!socket.isClosed()) {
                Socket client = null;
                try {
                    client = socket.accept();
                    handle(client);
                } catch (IOException ex) {
                    return;
                } finally {
                    closeQuietly(client);
                }
            }
        }

        private void handle(Socket client) throws IOException {
            InputStream in = client.getInputStream();
            // Read the request headers.
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            int prev1 = -1, prev2 = -1, prev3 = -1, c;
            while ((c = in.read()) != -1) {
                header.write(c);
                if (prev3 == '\r' && prev2 == '\n' && prev1 == '\r' && c == '\n') {
                    break;
                }
                prev3 = prev2;
                prev2 = prev1;
                prev1 = c;
            }
            String headerText = new String(header.toByteArray(), StandardCharsets.UTF_8);
            capturedRequest = headerText;
            int contentLength = parseContentLength(headerText);
            byte[] bodyBytes = new byte[contentLength];
            int read = 0;
            while (read < contentLength) {
                int n = in.read(bodyBytes, read, contentLength - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
            capturedBody = new String(bodyBytes, 0, read, StandardCharsets.UTF_8);

            byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
            OutputStream out = client.getOutputStream();
            StringBuilder resp = new StringBuilder();
            resp.append("HTTP/1.1 ").append(status).append(" X\r\n");
            resp.append("Content-Type: application/json\r\n");
            resp.append("Content-Length: ").append(responseBody.length).append("\r\n");
            resp.append("Connection: close\r\n\r\n");
            out.write(resp.toString().getBytes(StandardCharsets.UTF_8));
            out.write(responseBody);
            out.flush();
        }

        private static int parseContentLength(String headerText) {
            String lower = headerText.toLowerCase();
            int idx = lower.indexOf("content-length:");
            if (idx < 0) {
                return 0;
            }
            int start = idx + "content-length:".length();
            int end = lower.indexOf('\n', start);
            if (end < 0) {
                end = lower.length();
            }
            try {
                return Integer.parseInt(headerText.substring(start, end).trim());
            } catch (NumberFormatException ex) {
                return 0;
            }
        }

        void stop() {
            closeQuietly(socket);
        }

        private static void closeQuietly(java.io.Closeable c) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }

        private static void closeQuietly(Socket s) {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }
}
