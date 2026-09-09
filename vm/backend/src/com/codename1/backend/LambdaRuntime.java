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

/**
 * The AWS Lambda custom-runtime loop.
 *
 * Why this is the first server-side target: the Lambda Runtime API is a
 * CLIENT-side HTTP/1.1 poll over plaintext loopback, and the host serialises
 * invocations one per instance. So a runtime needs no listening socket, no
 * event loop, no TLS and no virtual threads - exactly the four things a general
 * server runtime needs and this VM does not yet have. What it does need is fast
 * start-up, which is what a translated binary is good at.
 *
 * Protocol (2018-06-01): long-poll GET .../invocation/next, which blocks until an
 * invocation arrives and returns the payload plus a Lambda-Runtime-Aws-Request-Id
 * header; then POST the result to .../invocation/{id}/response, or the failure to
 * .../invocation/{id}/error.
 */
public final class LambdaRuntime {
    private static final String API_VERSION = "/2018-06-01/runtime";
    private static final String REQUEST_ID_HEADER = "Lambda-Runtime-Aws-Request-Id";

    private LambdaRuntime() {
    }

    /**
     * Runs the invocation loop until the process is killed, which is how a Lambda
     * runtime is supposed to end - the host freezes or terminates the instance.
     */
    public static void run(Handler handler) {
        String endpoint = System.getenv("AWS_LAMBDA_RUNTIME_API");
        if(endpoint == null) {
            System.err.println("AWS_LAMBDA_RUNTIME_API is not set; not running under a Lambda host");
            return;
        }
        String host = endpoint;
        int port = 80;
        int colon = endpoint.indexOf(':');
        if(colon > 0) {
            host = endpoint.substring(0, colon);
            try {
                port = Integer.parseInt(endpoint.substring(colon + 1));
            } catch (NumberFormatException err) {
                System.err.println("Malformed AWS_LAMBDA_RUNTIME_API: " + endpoint);
                return;
            }
        }
        while(true) {
            if(!pumpOnce(handler, host, port)) {
                return;
            }
        }
    }

    /**
     * One poll/dispatch/report cycle. Returns false when the loop should stop,
     * which currently means the control connection itself failed - there is no
     * useful recovery from that, and spinning would burn the instance's budget.
     */
    static boolean pumpOnce(Handler handler, String host, int port) {
        Http.Response next;
        try {
            next = Http.get(host, port, API_VERSION + "/invocation/next");
        } catch (Exception err) {
            System.err.println("Failed to poll for the next invocation: " + err);
            return false;
        }
        String requestId = next.getHeader(REQUEST_ID_HEADER);
        if(requestId == null) {
            System.err.println("Invocation carried no " + REQUEST_ID_HEADER + "; cannot report a result");
            return false;
        }
        String result;
        try {
            result = handler.handle(next.getBodyAsString(), requestId);
        } catch (Exception err) {
            // The same rule the response path below takes, and for the same
            // reason: an invocation the host was never told about stays
            // outstanding until it times out, and polling for another one while
            // that is true just strands them one after the next. If the failure
            // could not even be reported, nothing this process says is reaching
            // the host, so it stops rather than collecting more.
            if(!reportError(host, port, requestId, err)) {
                System.err.println("The runtime API is unreachable, so this runtime is "
                        + "stopping rather than collecting invocations it cannot answer.");
                return false;
            }
            return true;
        }
        try {
            byte[] payload = (result == null ? "null" : result).getBytes("UTF-8");
            // The status matters: the Runtime API REJECTS a result it will not take
            // -- 413 for a payload over the response limit is the ordinary case --
            // and answers rather than throwing. Discarding it meant the handler's
            // work was dropped and the loop went straight back to polling, with the
            // caller left waiting for a reply that was never accepted and nothing
            // anywhere saying why.
            Http.Response posted = Http.post(host, port,
                    API_VERSION + "/invocation/" + requestId + "/response", payload);
            if(posted == null || posted.getStatus() < 200 || posted.getStatus() >= 300) {
                System.err.println("The Lambda runtime API refused the response for "
                        + requestId + " with status "
                        + (posted == null ? "none" : String.valueOf(posted.getStatus()))
                        + "; the result of " + payload.length + " byte(s) was not "
                        + "delivered. Reporting it as an error so the invocation "
                        + "does not simply hang.");
                reportError(host, port, requestId, new java.io.IOException(
                        "the runtime API refused the response with status "
                        + (posted == null ? "none" : String.valueOf(posted.getStatus()))));
            }
        } catch (Exception err) {
            // The result is GONE -- it existed only in the request that just
            // failed -- so this invocation has to be resolved here or it stays
            // outstanding until the host times it out, while this loop cheerfully
            // takes the next one. Reporting the failure is what lets the host
            // fail it now instead.
            System.err.println("Failed to post the response for " + requestId + ": " + err
                    + "; reporting it as an error so the invocation is resolved rather "
                    + "than left outstanding.");
            if(!reportError(host, port, requestId, err)) {
                // Not even the error reached the host, so nothing this process
                // says is getting through. Stop polling: collecting further
                // invocations only strands them the same way, and an exited
                // runtime is something Lambda knows how to recover from.
                System.err.println("The runtime API is unreachable, so this runtime is "
                        + "stopping rather than collecting invocations it cannot answer.");
                return false;
            }
        }
        return true;
    }

    /** @return whether the host accepted the report, so a caller can stop. */
    private static boolean reportError(String host, int port, String requestId, Exception cause) {
        try {
            // The host parses this shape; a plain string body is reported as a
            // malformed error and masks the real failure.
            String json = "{\"errorType\":\"" + escape(cause.getClass().getName())
                    + "\",\"errorMessage\":" + quote(cause.getMessage()) + "}";
            Http.Response posted = Http.post(host, port,
                    API_VERSION + "/invocation/" + requestId + "/error",
                    json.getBytes("UTF-8"));
            // Nothing left to escalate to if even this is refused, but a silent
            // failure here is how an invocation disappears without a trace.
            if(posted == null || posted.getStatus() < 200 || posted.getStatus() >= 300) {
                System.err.println("The Lambda runtime API refused the error report for "
                        + requestId + " with status "
                        + (posted == null ? "none" : String.valueOf(posted.getStatus())));
                return false;
            }
            return true;
        } catch (Exception err) {
            System.err.println("Failed to report the error for " + requestId + ": " + err);
            return false;
        }
    }

    private static String quote(String value) {
        if(value == null) {
            return "null";
        }
        return "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder();
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            switch(c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if(c < 0x20) {
                        out.append("\\u").append(hex(c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    private static String hex(char c) {
        String h = Integer.toHexString(c);
        StringBuilder out = new StringBuilder();
        for(int iter = h.length() ; iter < 4 ; iter++) {
            out.append('0');
        }
        return out.append(h).toString();
    }
}
