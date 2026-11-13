/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.demo;

import com.codename1.backend.Base64;
import com.codename1.backend.ServerSocket;
import com.codename1.backend.aws.Credentials;
import com.codename1.backend.mail.MailAuthenticationException;
import com.codename1.backend.mail.MailSendException;
import com.codename1.backend.mail.ses.SesTransport;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/// The SES transport against an endpoint of this process: what it asks for,
/// how it signs, how it splits a long recipient list, and what each refusal
/// becomes. Nothing here reaches Amazon.
final class SesCheck implements Runnable {
    private final ServerSocket socket;
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicInteger status = new AtomicInteger(200);
    private final List<String> requests = new ArrayList<String>();
    private final List<String> bodies = new ArrayList<String>();
    private final List<String> failures;
    private int passed;

    private SesCheck(List<String> failures) throws IOException {
        this.failures = failures;
        socket = ServerSocket.bind("127.0.0.1", 0, 16);
        Thread t = new Thread(this, "fake-ses");
        t.setDaemon(true);
        t.start();
    }

    /// Runs the checks; answers how many passed.
    static int run(List<String> failures) throws Exception {
        SesCheck check = new SesCheck(failures);
        try {
            check.checks();
        } finally {
            check.stopped.set(true);
            check.socket.close();
        }
        return check.passed;
    }

    private void check(String what, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            passed++;
        } else {
            failures.add("ses " + what + ": expected " + expected + " but was " + actual);
        }
    }

    private synchronized String request(int i) {
        return i < requests.size() ? requests.get(i) : "";
    }

    private synchronized String body(int i) {
        return i < bodies.size() ? bodies.get(i) : "";
    }

    private synchronized int count() {
        return requests.size();
    }

    private synchronized void clear() {
        requests.clear();
        bodies.clear();
    }

    private static String header(String request, String name) {
        // By hand: the device's String has no split.
        int at = request.indexOf("\r\n");
        while (at >= 0) {
            int next = request.indexOf("\r\n", at + 2);
            String line = request.substring(at + 2, next < 0 ? request.length() : next);
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase(name)) {
                return line.substring(colon + 1).trim();
            }
            at = next;
        }
        return null;
    }

    private void checks() throws Exception {
        Credentials who = new Credentials("AKIDEXAMPLE", "secret-of-the-example", null);
        byte[] mime = WireBuffer.ascii("Subject: Hello\r\n\r\nA body\r\n");
        SesTransport ses = new SesTransport("ses://127.0.0.1:" + socket.getPort(),
                "eu-west-1", "receipts", who);
        check("name", "ses", ses.getName());

        ses.send("sender@example.com", new String[] {"a@example.com", "b@example.com"}, mime);
        check("one request", Integer.valueOf(1), Integer.valueOf(count()));
        String request = request(0);
        check("request line", "POST /v2/email/outbound-emails HTTP/1.1",
                request.substring(0, Math.max(0, request.indexOf("\r\n"))));
        String auth = String.valueOf(header(request, "Authorization"));
        check("signed for ses in the region", Boolean.TRUE, Boolean.valueOf(
                auth.startsWith("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/")
                        && auth.indexOf("/eu-west-1/ses/aws4_request") > 0
                        && auth.indexOf("Signature=") > 0));
        check("dated", Boolean.TRUE, Boolean.valueOf(header(request, "X-Amz-Date") != null));
        check("json", "application/json", header(request, "Content-Type"));
        String body = body(0);
        check("sender", Boolean.TRUE, Boolean.valueOf(
                body.indexOf("\"FromEmailAddress\":\"sender@example.com\"") >= 0));
        check("recipients", Boolean.TRUE, Boolean.valueOf(
                body.indexOf("\"ToAddresses\":[\"a@example.com\",\"b@example.com\"]") >= 0));
        check("the mail as it was built", Boolean.TRUE, Boolean.valueOf(
                body.indexOf("\"Raw\":{\"Data\":\"" + Base64.encode(mime) + "\"}") >= 0));
        check("configuration set", Boolean.TRUE, Boolean.valueOf(
                body.indexOf("\"ConfigurationSetName\":\"receipts\"") >= 0));
        check("healthy", Boolean.TRUE, Boolean.valueOf(ses.isHealthy()));

        // More recipients than one request takes.
        clear();
        String[] many = new String[120];
        for (int i = 0; i < many.length; i++) {
            many[i] = "r" + i + "@example.com";
        }
        ses.send("sender@example.com", many, mime);
        check("three requests for 120", Integer.valueOf(3), Integer.valueOf(count()));
        check("the last holds the rest", Boolean.TRUE, Boolean.valueOf(
                body(2).indexOf("[\"r100@example.com\"") > 0
                        && body(2).indexOf("\"r119@example.com\"]") > 0));

        // Refusals.
        clear();
        status.set(403);
        try {
            ses.send("sender@example.com", new String[] {"a@example.com"}, mime);
            check("403 is refused", "an exception", "none");
        } catch (MailAuthenticationException err) {
            check("403 is the credentials", Integer.valueOf(403), Integer.valueOf(err.getCode()));
        }
        status.set(400);
        try {
            ses.send("sender@example.com", new String[] {"a@example.com"}, mime);
            check("400 is refused", "an exception", "none");
        } catch (MailSendException err) {
            check("400 is permanent", Boolean.TRUE, Boolean.valueOf(err.isPermanent()));
        }
        status.set(429);
        try {
            ses.send("sender@example.com", new String[] {"a@example.com"}, mime);
            check("429 is refused", "an exception", "none");
        } catch (MailSendException err) {
            check("429 is worth another attempt", Boolean.FALSE,
                    Boolean.valueOf(err.isPermanent()));
        }
        status.set(503);
        try {
            ses.send("sender@example.com", new String[] {"a@example.com"}, mime);
            check("503 is refused", "an exception", "none");
        } catch (MailSendException err) {
            check("503 is worth another attempt", Boolean.FALSE,
                    Boolean.valueOf(err.isPermanent()));
            check("503 is unhealthy", Boolean.FALSE, Boolean.valueOf(ses.isHealthy()));
        }
        status.set(200);
        ses.send("sender@example.com", new String[] {"a@example.com"}, mime);
        check("healthy again", Boolean.TRUE, Boolean.valueOf(ses.isHealthy()));

        // No configuration set, no member for one.
        clear();
        SesTransport plain = new SesTransport("ses://127.0.0.1:" + socket.getPort(), null,
                null, who);
        plain.send("sender@example.com", new String[] {"a@example.com"}, mime);
        check("no configuration set", Boolean.TRUE,
                Boolean.valueOf(body(0).length() > 0
                        && body(0).indexOf("ConfigurationSetName") < 0));
        check("default region", Boolean.TRUE, Boolean.valueOf(
                String.valueOf(header(request(0), "Authorization"))
                        .indexOf("/us-east-1/ses/aws4_request") > 0));
        plain.close();
        ses.close();
        try {
            ses.send("sender@example.com", new String[] {"a@example.com"}, mime);
            check("closed", "an exception", "none");
        } catch (IOException err) {
            check("closed", Boolean.TRUE, Boolean.TRUE);
        }
        try {
            new SesTransport("ses://", null, null);
            check("no region", "an exception", "none");
        } catch (IOException err) {
            check("no region names the fix", Boolean.TRUE,
                    Boolean.valueOf(err.getMessage().indexOf("ses://eu-west-1") >= 0));
        }
    }

    // ------------------------------------------------------------ the server

    public void run() {
        while (!stopped.get()) {
            final int fd = socket.accept();
            if (fd < 0) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException err) {
                    return;
                }
                continue;
            }
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        ServerSocket.setBlocking(fd, true);
                        ServerSocket.setTimeout(fd, 20000);
                        serve(fd);
                    } catch (IOException | RuntimeException err) {
                        // The client went away; the checks say what was missing.
                    } finally {
                        ServerSocket.closeFd(fd);
                    }
                }
            }, "fake-ses-conn");
            t.setDaemon(true);
            t.start();
        }
    }

    /// Requests on one connection, each answered with the status set.
    private void serve(int fd) throws IOException {
        byte[] buffer = new byte[65536];
        StringBuilder in = new StringBuilder();
        while (true) {
            int end;
            while ((end = in.indexOf("\r\n\r\n")) < 0) {
                int n = ServerSocket.read(fd, buffer, 0, buffer.length);
                if (n <= 0) {
                    return;
                }
                for (int i = 0; i < n; i++) {
                    in.append((char) (buffer[i] & 0xff));
                }
            }
            String head = in.substring(0, end);
            String declared = header(head, "Content-Length");
            int length = declared == null ? 0 : Integer.parseInt(declared);
            while (in.length() < end + 4 + length) {
                int n = ServerSocket.read(fd, buffer, 0, buffer.length);
                if (n <= 0) {
                    return;
                }
                for (int i = 0; i < n; i++) {
                    in.append((char) (buffer[i] & 0xff));
                }
            }
            synchronized (this) {
                requests.add(head);
                bodies.add(in.substring(end + 4, end + 4 + length));
            }
            in.delete(0, end + 4 + length);
            int code = status.get();
            String answer = code == 200 ? "{\"MessageId\":\"0100-example\"}"
                    : "{\"message\":\"refused by the check\"}";
            byte[] out = WireBuffer.ascii("HTTP/1.1 " + code + " Status\r\n"
                    + "Content-Type: application/json\r\nContent-Length: " + answer.length()
                    + "\r\n\r\n" + answer);
            ServerSocket.write(fd, out, 0, out.length);
        }
    }
}
