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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The last requests the server answered, kept for the development MCP tools:
 * what came in, what went out, how long it took and what was thrown.
 *
 * <p>Off unless the development tools turn it on. Off, recording is a static
 * read per request and nothing else.
 */
public final class RequestLog {
    static boolean enabled;
    private static Map[] ring = new Map[0];
    private static int next;
    private static int size;

    private RequestLog() {
    }

    /** Starts keeping the last {@code capacity} requests. */
    public static synchronized void enable(int capacity) {
        ring = new Map[capacity < 1 ? 1 : capacity];
        next = 0;
        size = 0;
        enabled = true;
    }

    static void record(HttpServer.Request request, int status, long startedMillis,
                       Throwable error) {
        if(!enabled) {
            return;
        }
        Map entry = new LinkedHashMap();
        entry.put("time", new Long(startedMillis));
        entry.put("method", request.getMethod());
        entry.put("target", request.getTarget());
        entry.put("status", new Integer(status));
        entry.put("millis", new Long(System.currentTimeMillis() - startedMillis));
        if(error != null) {
            entry.put("error", String.valueOf(error));
            StringBuilder where = new StringBuilder();
            Throwable cause = error;
            int depth = 0;
            while(cause != null && depth < 4) {
                if(depth > 0) {
                    where.append(" <- caused by ").append(cause);
                }
                cause = cause.getCause();
                depth++;
            }
            if(where.length() > 0) {
                entry.put("causes", where.toString());
            }
        }
        synchronized(RequestLog.class) {
            ring[next] = entry;
            next = (next + 1) % ring.length;
            if(size < ring.length) {
                size++;
            }
        }
    }

    /** The newest {@code limit} requests, newest first; only failures when asked. */
    public static synchronized List recent(int limit, boolean failuresOnly) {
        List out = new ArrayList();
        for(int iter = 0 ; iter < size && out.size() < limit ; iter++) {
            int at = (next - 1 - iter + ring.length) % ring.length;
            Map entry = ring[at];
            if(entry == null) {
                continue;
            }
            if(failuresOnly) {
                Object status = entry.get("status");
                boolean failed = entry.get("error") != null
                        || (status instanceof Integer && ((Integer)status).intValue() >= 500);
                if(!failed) {
                    continue;
                }
            }
            out.add(entry);
        }
        return out;
    }
}
