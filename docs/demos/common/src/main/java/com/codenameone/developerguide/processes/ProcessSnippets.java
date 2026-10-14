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
package com.codenameone.developerguide.processes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProcessSnippets {
    // tag::process-version[]
    public void readToolVersion() {
        // Check isSupported() before enabling this action in your UI.
        new Thread(() -> {
            try (com.codename1.io.Process child = new com.codename1.io.ProcessBuilder("my-tool", "--version")
                    .redirectErrorStream(true).start()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                Thread reader = new Thread(() -> {
                    try {
                        byte[] buffer = new byte[4096];
                        int count;
                        while ((count = child.getInputStream().read(buffer)) != -1) {
                            output.write(buffer, 0, count);
                        }
                    } catch (IOException error) {
                        // Closing the process also cancels an outstanding native read.
                        com.codename1.io.Log.e(error);
                    }
                });
                reader.start();
                child.getOutputStream().close(); // No input; send EOF to the tool.
                if (!child.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    child.destroyForcibly();
                }
                child.waitFor();
                reader.join();
                String version = new String(output.toByteArray(), "UTF-8");
                // Use Display.callSerially(...) for any UI update using version.
            } catch (Exception error) {
                com.codename1.io.Log.e(error);
            }
        }).start();
    }
    // end::process-version[]
}
