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

import java.util.LinkedHashMap;
import java.util.Map;

import com.codename1.backend.Db;
import com.codename1.backend.Handler;
import com.codename1.backend.Json;
import com.codename1.backend.LambdaRuntime;

/**
 * The MVP function. Note what is NOT here: no route table, no path parsing, no
 * argument extraction, no DTO marshalling. GreeterApiDispatcher and PetJson are
 * generated from the shared GreeterApi contract, so adding a parameter to the
 * contract breaks this build instead of returning the wrong thing at runtime.
 *
 * What is left is the transport glue: decode the host's event envelope, hand the
 * dispatcher a decoded body, encode whatever comes back.
 */
public class Greeter {
    public static void main(String[] args) {
        final GreeterApiDispatcher dispatcher;
        try {
            // CN1_DB_PATH lets a test point at a scratch file; a real function would
            // use the writable path its host gives it (/tmp on Lambda), and ":memory:"
            // is the honest default for a stateless invocation.
            String dbPath = System.getenv("CN1_DB_PATH");
            Db db = Db.open(dbPath == null ? ":memory:" : dbPath);
            dispatcher = new GreeterApiDispatcher(new GreeterService(db));
        } catch (Exception err) {
            System.err.println("Could not start: " + err);
            return;
        }
        LambdaRuntime.run(new Handler() {
            public String handle(String event, String requestId) throws Exception {
                Map envelope;
                try {
                    envelope = Json.parseObject(event);
                } catch (Exception err) {
                    return error(400, "malformed event: " + err.getMessage());
                }
                String method = string(envelope.get("httpMethod"));
                String path = string(envelope.get("path"));
                if(method == null || path == null) {
                    return error(400, "expected httpMethod and path");
                }
                Map headers = envelope.get("headers") instanceof Map
                        ? (Map)envelope.get("headers") : null;
                Object body = decodeBody(envelope.get("body"));

                if(!dispatcher.hasRoute(method, path)) {
                    return error(404, "no route for " + method + " " + path);
                }
                Object result;
                try {
                    result = dispatcher.dispatch(method, path, headers, body);
                } catch (SecurityException err) {
                    // Authentication or authorisation failed; not a server fault.
                    return error(401, err.getMessage());
                } catch (IllegalArgumentException err) {
                    // The handler rejected the input; that is a 400, not a 500.
                    return error(400, err.getMessage());
                } catch (Exception err) {
                    System.err.println("[" + requestId + "] " + err);
                    return error(500, err.getClass().getName());
                }
                Map out = new LinkedHashMap();
                out.put("statusCode", new Integer(result == null ? 404 : 200));
                out.put("body", result == null ? "not found" : Json.write(result));
                return Json.write(out);
            }
        });
    }

    /**
     * The envelope carries the body as a JSON string, so it is decoded here rather
     * than in the dispatcher - the dispatcher deals in values, not transport.
     */
    private static Object decodeBody(Object raw) {
        if(raw == null) {
            return null;
        }
        String text = String.valueOf(raw);
        if(text.length() == 0) {
            return null;
        }
        try {
            return Json.parse(text);
        } catch (Exception err) {
            // Not JSON: hand it through as text so a @Body String still works.
            return text;
        }
    }

    private static String string(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static String error(int status, String message) {
        Map out = new LinkedHashMap();
        out.put("statusCode", new Integer(status));
        out.put("body", message);
        return Json.write(out);
    }
}
