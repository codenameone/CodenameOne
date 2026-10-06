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
package com.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.codename1.backend.Backend;
import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Web;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.apikey.ApiKeyGenerator;
import com.codename1.backend.security.ratelimit.InMemoryRateLimiter;

/**
 * What a request knows about where it came from, on a real connection.
 *
 * The address of a connection's other end comes from getpeername in the translated binary and
 * from the socket channel on the JVM; both hand over raw bytes and one piece of Java formats
 * them. This starts a server, calls it over loopback with the runtime's own HTTP client, and
 * prints what the handler was told -- with and without the forwarding headers a proxy adds.
 */
final class AuthCheckServer {
    private AuthCheckServer() {
    }

    static void run() throws Exception {
        Properties settings = new Properties();
        settings.setProperty("cn1.server.port", "0");
        settings.setProperty("cn1.server.forwardHeaders", "true");
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet().host("127.0.0.1")
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return HttpServer.Response.text(200, request.getRemoteAddress() + "|"
                                + request.getPeerAddress() + "|" + request.isSecure());
                    }
                }).start();
        try {
            String url = "http://127.0.0.1:" + backend.getServer().getPort() + "/who";
            AuthCheck.value("who, directly", ask(url, null, null));
            AuthCheck.check("the peer is loopback", "200 127.0.0.1|127.0.0.1|false", ask(url, null, null));
            AuthCheck.value("who, through proxies", ask(url,
                    "198.51.100.9, 203.0.113.7, 10.1.2.3", "https"));
            AuthCheck.check("the rightmost address that is not a proxy is the client",
                    "200 203.0.113.7|127.0.0.1|true",
                    ask(url, "198.51.100.9, 203.0.113.7, 10.1.2.3", "https"));
            AuthCheck.value("who, over IPv6", ask(url, "2001:DB8::1, ::ffff:10.0.0.9", "http"));
            AuthCheck.value("who, with a header that is not addresses", ask(url, "unknown", null));
        } finally {
            backend.stop();
        }
        limits();
    }

    private static String ask(String url, String forwardedFor, String forwardedProto)
            throws Exception {
        List headers = new ArrayList();
        if(forwardedFor != null) {
            headers.add("X-Forwarded-For: " + forwardedFor);
        }
        if(forwardedProto != null) {
            headers.add("X-Forwarded-Proto: " + forwardedProto);
        }
        Web.Result result = Web.request("GET", url, headers, null);
        return result.getStatus() + " " + String.valueOf(result.getBodyAsString()).trim();
    }

    /** The token bucket is floating point; both runtimes must count alike. */
    private static void limits() {
        final long[] now = {1700000000000L};
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(3, 60);
        limiter.setClock(new Clock() {
            public long currentTimeMillis() {
                return now[0];
            }
        });
        StringBuilder trace = new StringBuilder();
        long[] steps = {0, 0, 0, 0, 19000, 1000, 0, 7000, 13000, 600000, 0, 0, 0, 1};
        for(int iter = 0 ; iter < steps.length ; iter++) {
            now[0] += steps[iter];
            trace.append(limiter.tryAcquire("a") ? 'y' : 'n').append(limiter.retryAfterSeconds("a"))
                    .append(' ');
        }
        AuthCheck.value("rate limit trace", trace.toString().trim());
        AuthCheck.value("api key hash", ApiKeyGenerator.hash("cn1_not-a-real-key"));
        String made = new ApiKeyGenerator().generate("ci-bot", new String[] {"deploy"}).getPlaintext();
        AuthCheck.check("a generated key has its prefix and length", "cn1_ 47",
                made.substring(0, 4) + " " + made.length());
    }
}
