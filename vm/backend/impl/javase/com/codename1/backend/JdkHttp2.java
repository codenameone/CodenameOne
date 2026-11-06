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
package com.codename1.backend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// HTTP/2 for the Java SE runtime, through the JDK's own `java.net.http`
/// client: what a `WebClient` that requires HTTP/2 uses on the JVM arm, so APNs
/// and anything else that speaks only HTTP/2 can be exercised in the
/// development loop and in tests.
///
/// Reached by reflection because this arm compiles for Java 8 and
/// `java.net.http` arrived in 11. The classes are resolved through the system
/// class loader, never `Class.forName` (see BanClassForName in the pom), and on
/// a JVM without them the request fails saying so.
///
/// The rules HttpURLConnection follows on the other path hold here too: a
/// redirect is followed only for a safe method with no headers, body or client
/// certificate; the response is bounded by `CN1_WEB_MAX_RESPONSE_MB`; header
/// names come back lower case. The client has no read timeout, so a request
/// with no request timeout set is bounded by its read timeout as a whole.
final class JdkHttp2 {
    private static final String P = "java.net.http.";

    /// Closes a response body whose read outlived the request's timeout. The
    /// client's own timeout ends at the response headers, and a blocked read
    /// checks no clock, so closing the stream is what ends a body that trickles.
    private static final java.util.concurrent.ScheduledExecutorService DEADLINES =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cn1-http2-deadline");
                t.setDaemon(true);
                return t;
            });

    private JdkHttp2() {
    }

    /// One `HttpClient`, per redirect policy, for one WebClient: the
    /// connection it keeps is the pool.
    static final class Clients {
        private final Object[] clients = new Object[2];

        synchronized Object get(WebClient client, boolean follow) throws IOException {
            int at = follow ? 1 : 0;
            if (clients[at] == null) {
                clients[at] = build(client, follow);
            }
            return clients[at];
        }

        synchronized void close() {
            for (int iter = 0 ; iter < clients.length ; iter++) {
                if (clients[iter] != null) {
                    // close() exists from JDK 21; before it the client's
                    // connections idle out on their own.
                    try {
                        // Looked up on the public type: the instance's own class
                        // is internal to java.net.http.
                        Method m = type("HttpClient").getMethod("shutdownNow");
                        m.invoke(clients[iter]);
                    } catch (ReflectiveOperationException | IOException | RuntimeException ignored) {
                        // Not on this JDK.
                    }
                    clients[iter] = null;
                }
            }
        }
    }

    private static Class type(String name) throws IOException {
        try {
            return ClassLoader.getSystemClassLoader().loadClass(P + name);
        } catch (ClassNotFoundException err) {
            throw new IOException("An HTTP/2 request on the local Java SE runtime needs "
                    + "Java 11 or newer, whose java.net.http client makes it; this JVM is "
                    + System.getProperty("java.version") + ".", err);
        }
    }

    private static Object call(Object target, Class owner, String name, Class[] params,
                               Object... args) throws IOException {
        try {
            return owner.getMethod(name, params).invoke(target, args);
        } catch (InvocationTargetException err) {
            Throwable cause = err.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw new IOException("The HTTP/2 request was interrupted", err);
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IOException(String.valueOf(cause), err);
        } catch (ReflectiveOperationException err) {
            throw new IOException("java.net.http has no " + owner.getName() + "." + name, err);
        }
    }

    private static Object constant(String enumType, String name) throws IOException {
        Class c = type(enumType);
        for (Object value : c.getEnumConstants()) {
            if (((Enum) value).name().equals(name)) {
                return value;
            }
        }
        throw new IOException("java.net.http has no " + enumType + "." + name);
    }

    private static Object duration(long millis) throws IOException {
        try {
            return java.time.Duration.ofMillis(millis);
        } catch (RuntimeException err) {
            throw new IOException(err.toString(), err);
        }
    }

    private static Object build(WebClient client, boolean follow) throws IOException {
        Class clientType = type("HttpClient");
        Class builderType = type("HttpClient$Builder");
        Object builder = call(null, clientType, "newBuilder", new Class[0]);
        call(builder, builderType, "version", new Class[] {type("HttpClient$Version")},
                constant("HttpClient$Version", "HTTP_2"));
        call(builder, builderType, "followRedirects", new Class[] {type("HttpClient$Redirect")},
                constant("HttpClient$Redirect", follow ? "NORMAL" : "NEVER"));
        if (client.getConnectTimeoutMillis() > 0) {
            call(builder, builderType, "connectTimeout", new Class[] {java.time.Duration.class},
                    duration(client.getConnectTimeoutMillis()));
        }
        call(builder, builderType, "sslContext", new Class[] {javax.net.ssl.SSLContext.class},
                ClientCredentials.context(client.getClientCertificate()));
        return call(builder, builderType, "build", new Class[0]);
    }

    static Web.Result perform(WebClient client, Clients clients, String method, String url,
                              List headers, byte[] body, List trace, long maxBytes)
            throws IOException {
        String verb = method == null ? "GET" : method;
        boolean safe = "GET".equals(verb) || "HEAD".equals(verb);
        boolean follow = (headers == null || headers.isEmpty())
                && (body == null || body.length == 0) && safe
                && client.getClientCertificate() == null;
        Object http = clients.get(client, follow);

        Class requestType = type("HttpRequest");
        Class rbType = type("HttpRequest$Builder");
        Class publisherType = type("HttpRequest$BodyPublisher");
        Class publishers = type("HttpRequest$BodyPublishers");
        Object rb;
        try {
            rb = call(null, requestType, "newBuilder", new Class[] {URI.class}, new URI(url));
        } catch (java.net.URISyntaxException err) {
            throw new IOException("Not a URL: " + Urls.forMessage(url), err);
        }
        Object publisher = body == null || body.length == 0
                ? call(null, publishers, "noBody", new Class[0])
                : call(null, publishers, "ofByteArray", new Class[] {byte[].class}, body);
        call(rb, rbType, "method", new Class[] {String.class, publisherType}, verb, publisher);
        boolean agent = false;
        List lines = new ArrayList();
        if (headers != null) {
            lines.addAll(headers);
        }
        if (trace != null) {
            lines.addAll(trace);
        }
        for (Object line : lines) {
            String header = String.valueOf(line);
            int colon = header.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = header.substring(0, colon).trim();
            String value = header.substring(colon + 1).trim();
            if ("Connection".equalsIgnoreCase(name) || "Content-Length".equalsIgnoreCase(name)
                    || "Host".equalsIgnoreCase(name)) {
                // Connection-specific, and forbidden in HTTP/2 (RFC 9113 8.2.2);
                // the client derives the other two.
                continue;
            }
            agent |= "User-Agent".equalsIgnoreCase(name);
            call(rb, rbType, "header", new Class[] {String.class, String.class}, name, value);
        }
        if (!agent) {
            call(rb, rbType, "header", new Class[] {String.class, String.class},
                    "User-Agent", "codenameone-backend");
        }
        int ceiling = client.getRequestTimeoutMillis() > 0 ? client.getRequestTimeoutMillis()
                : client.getReadTimeoutMillis();
        if (ceiling > 0) {
            call(rb, rbType, "timeout", new Class[] {java.time.Duration.class},
                    duration(ceiling));
        }
        Object request = call(rb, rbType, "build", new Class[0]);

        Class handlers = type("HttpResponse$BodyHandlers");
        Class handlerType = type("HttpResponse$BodyHandler");
        Object handler = call(null, handlers, "ofInputStream", new Class[0]);
        Class responseType = type("HttpResponse");
        Object response = call(http, type("HttpClient"), "send",
                new Class[] {requestType, handlerType}, request, handler);
        InputStream in = (InputStream) call(response, responseType, "body", new Class[0]);
        java.util.concurrent.atomic.AtomicBoolean expired =
                new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.ScheduledFuture<?> deadline = ceiling <= 0 ? null
                : DEADLINES.schedule(() -> {
                    expired.set(true);
                    try {
                        in.close();
                    } catch (IOException ignored) {
                        // the reader sees the closed stream either way
                    }
                }, ceiling, java.util.concurrent.TimeUnit.MILLISECONDS);
        try {
            Object version = call(response, responseType, "version", new Class[0]);
            if (!"HTTP_2".equals(((Enum) version).name())) {
                throw new IOException("Request to " + Urls.forMessage(url) + " failed: the "
                        + "server did not negotiate HTTP/2, which this client requires");
            }
            int status = ((Integer) call(response, responseType, "statusCode",
                    new Class[0])).intValue();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            long total = 0;
            int n;
            while ((n = readBody(in, chunk, expired, url, ceiling)) > 0) {
                total += n;
                if (total > maxBytes) {
                    throw new IOException("Request to " + Urls.forMessage(url) + " failed: the "
                            + "response is larger than the " + (maxBytes / (1024 * 1024))
                            + "MB CN1_WEB_MAX_RESPONSE_MB allows");
                }
                buffer.write(chunk, 0, n);
            }
            Object httpHeaders = call(response, responseType, "headers", new Class[0]);
            Map all = (Map) call(httpHeaders, type("HttpHeaders"), "map", new Class[0]);
            Map values = new LinkedHashMap();
            for (Object entry : all.entrySet()) {
                Map.Entry e = (Map.Entry) entry;
                String name = String.valueOf(e.getKey());
                if (name.startsWith(":")) {
                    continue;   // a pseudo-header, not a field
                }
                values.put(asciiLower(name), new ArrayList((List) e.getValue()));
            }
            return new Web.Result(status, buffer.toByteArray(), null, values, "HTTP/2");
        } finally {
            if (deadline != null) {
                deadline.cancel(false);
            }
            in.close();
        }
    }

    private static int readBody(InputStream in, byte[] chunk, java.util.concurrent.atomic.AtomicBoolean expired,
                                String url, int ceiling) throws IOException {
        try {
            int n = in.read(chunk);
            if (expired.get()) {
                throw new IOException("closed");
            }
            return n;
        } catch (IOException err) {
            if (expired.get()) {
                java.net.SocketTimeoutException timeout = new java.net.SocketTimeoutException(
                        "Request to " + Urls.forMessage(url) + " timed out after " + ceiling
                        + "ms reading the response body");
                timeout.initCause(err);
                throw timeout;
            }
            throw err;
        }
    }

    /// See Web.asciiLower; HTTP/2 names are lower case on the wire already, and
    /// this keeps a JDK that hands them back otherwise from changing that.
    private static String asciiLower(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString();
    }
}
