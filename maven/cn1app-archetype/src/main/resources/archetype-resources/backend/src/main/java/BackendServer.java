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
package ${package};

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.Signals;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The server side of this app.
 *
 * Run it with `mvn -pl backend -Dcodename1.platform=backend cn1:backend` while
 * developing: it starts on this
 * JVM in a couple of seconds against the minute and a half a native build takes,
 * and the protocol layer underneath is the same source that ships. Package it with
 * `mvn -pl backend -Dcodename1.platform=backend cn1:backend-package` to get a
 * single native binary with no JVM
 * to install beneath it.
 *
 * The local run deliberately does not terminate TLS, and therefore does not serve
 * HTTP/2. Build the binary when those are what you need to exercise.
 */
public class BackendServer {
    public static void main(String[] args) throws Exception {
        // Turns SIGTERM and SIGINT into the shutdown below, so a container stop
        // lets in-flight requests finish instead of cutting them off.
        Signals.installShutdownHandler();

        int port = envInt("PORT", 8080);
        int workers = envInt("WORKERS", 16);

        final HttpServer server = HttpServer.start(null, port, 512, workers,
                new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                if ("/healthz".equals(request.getTarget())) {
                    return HttpServer.Response.json(200, "{\"status\":\"ok\"}");
                }
                Map out = new LinkedHashMap();
                out.put("method", request.getMethod());
                out.put("target", request.getTarget());
                return HttpServer.Response.json(200, Json.write(out));
            }
        }, null);

        System.out.println("listening on port " + server.getPort()
                + " with " + workers + " workers");

        Signals.onShutdown(new Runnable() {
            public void run() {
                // Stops accepting, lets in-flight requests finish, then closes.
                server.stop(10000);
                System.exit(0);
            }
        });
        // The reactor and its workers are detached threads, so a main that returned
        // would end the process with status 0 and no message.
        server.awaitTermination();
    }

    private static int envInt(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null || value.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException err) {
            return fallback;
        }
    }
}
