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

import com.codename1.backend.Backend;
import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Tasks;
import com.codename1.backend.VirtualThread;
import com.codename1.io.Process;
import com.codename1.io.ProcessBuilder;
import java.util.Properties;

/** A blocked process pipe/wait must let another task run on the sole host. */
public final class ProcessVirtualConformance {
    private static volatile boolean entered;
    private static volatile boolean marker;
    private static volatile boolean finished;
    private static volatile boolean virtual;
    public static void main(String[] args) throws Exception {
        Backend backend = Backend.builder(Config.of(new Properties(), "test"))
                .quiet().host("127.0.0.1").port(0).workers(1)
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return HttpServer.Response.text(200, "ok");
                    }
                }).start();
        try {
            for (int mode = 0; mode < 2; ++mode) {
                entered = marker = finished = virtual = false;
                System.out.println("process virtual mode=" + mode);
                final boolean pipe = mode == 0;
                final Process child = new ProcessBuilder(args[0], "sleep").start();
                try {
                    Tasks.virtual(new Runnable() {
                        public void run() {
                            virtual = VirtualThread.isVirtual();
                            entered = true;
                            try {
                                if (pipe) child.getInputStream().read();
                                else child.waitFor();
                            } catch (Exception expectedOnClose) { System.out.println("process task closed: " + expectedOnClose); }
                            finally { finished = true; System.out.println("process task finished"); }
                        }
                    });
                    long deadline = System.currentTimeMillis() + 5000;
                    while (!entered && System.currentTimeMillis() < deadline) Thread.sleep(1);
                    if (!entered || !virtual) throw new AssertionError("not running on a virtual thread");
                    Tasks.virtual(new Runnable() { public void run() { marker = true; } });
                    deadline = System.currentTimeMillis() + 2000;
                    while (!marker && System.currentTimeMillis() < deadline) Thread.sleep(1);
                    System.out.println("process marker=" + marker);
                    if (!marker) throw new AssertionError("process operation pinned the sole host");
                } finally {
                    child.close();
                    long deadline = System.currentTimeMillis() + 5000;
                    while (!finished && System.currentTimeMillis() < deadline) Thread.sleep(1);
                }
                if (!finished) throw new AssertionError("task did not finish after close");
            }
        } finally { backend.stop(); }
        System.out.println("PROCESS_VIRTUAL_CONFORMANCE_OK");
    }
}
