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

import java.util.ArrayList;
import java.util.List;

import com.codename1.backend.Web;

/**
 * Talks to a running backend using the PACKAGED outbound client, and reports what
 * came back.
 *
 * Every other test of this server drives it from JUnit over a raw socket or an
 * HttpURLConnection, so the client half of the packaged runtime -- Web, and the
 * libcurl behind it -- was only ever exercised against dead ports and mocks. Two
 * things follow from testing it this way instead. The verbs are proved end to
 * end: PATCH is the one Java SE cannot send at all, so it can be verified HERE
 * and nowhere else. And the two halves meet over a real socket, which is the
 * only place a disagreement between them can actually show up.
 *
 * CN1_WEBCHECK_BASE names the server, for example http://127.0.0.1:8080.
 */
public class WebCheck {
    private static int passed;
    private static final List failures = new ArrayList();

    public static void main(String[] args) throws Exception {
        String base = System.getenv("CN1_WEBCHECK_BASE");
        if(base == null || base.length() == 0) {
            System.out.println("CN1_WEBCHECK_BASE is not set");
            System.out.println("WEBCHECK FAILED");
            System.exit(1);
        }

        // The server echoes the method it saw, so this compares what ARRIVED
        // against what was asked for rather than trusting the client's own idea.
        check("GET arrives as GET", "method=GET len=0", methodSeenBy(base, "GET"));
        check("POST arrives as POST", "method=POST len=0", methodSeenBy(base, "POST"));
        check("PUT arrives as PUT", "method=PUT len=0", methodSeenBy(base, "PUT"));
        check("DELETE arrives as DELETE", "method=DELETE len=0", methodSeenBy(base, "DELETE"));
        // The one the local runtime refuses outright. If the packaged client ever
        // stops sending it, this is the only test that would notice.
        check("PATCH arrives as PATCH", "method=PATCH len=0", methodSeenBy(base, "PATCH"));

        // A body, over a real socket, measured by the server rather than by the
        // client -- so what is proved is that the bytes ARRIVED, not that they
        // were handed to the transport.
        Web.Result posted = Web.request("POST", base + "/echo",
                header("Content-Type: application/json"), utf8("{\"a\":1}"));
        check("a body arrives whole", "method=POST len=7",
                posted == null ? "no result" : posted.getBodyAsString());

        // And one big enough to cross the server's buffer growth several times.
        StringBuilder big = new StringBuilder();
        for(int iter = 0 ; iter < 100000 ; iter++) {
            big.append('x');
        }
        Web.Result large = Web.request("POST", base + "/echo", null, utf8(big.toString()));
        check("a large body arrives whole", "method=POST len=100000",
                large == null ? "no result" : large.getBodyAsString());

        // A response header the client must be able to read back.
        Web.Result health = Web.request("GET", base + "/healthz", null, null);
        check("a response carries its content type", "true",
                String.valueOf(health != null
                        && health.getHeader("content-type") != null));

        System.out.println("passed=" + passed + " failed=" + failures.size());
        for(int iter = 0 ; iter < failures.size() ; iter++) {
            System.out.println("FAIL " + failures.get(iter));
        }
        System.out.println(failures.isEmpty() ? "WEBCHECK OK" : "WEBCHECK FAILED");
        if(!failures.isEmpty()) {
            System.exit(1);
        }
    }

    /** What the server says it received, or the transport error that stopped it. */
    private static String methodSeenBy(String base, String method) {
        try {
            Web.Result r = Web.request(method, base + "/echo", null, null);
            if(r == null) {
                return "no result";
            }
            if(r.getStatus() != 200) {
                return "status " + r.getStatus() + " " + r.getError();
            }
            return r.getBodyAsString();
        } catch (Exception err) {
            return "threw " + err.getMessage();
        }
    }

    private static List header(String line) {
        List out = new ArrayList();
        out.add(line);
        return out;
    }

    private static byte[] utf8(String value) throws Exception {
        return value.getBytes("UTF-8");
    }

    private static void check(String name, String expected, String actual) {
        if(expected.equals(actual)) {
            passed++;
        } else {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
