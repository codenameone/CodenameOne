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
package com.codename1.impl.process;

import com.codename1.io.Process;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/// Host-JDK adapter, also compiled by the JVM backend. No UI dependencies.
/// @hidden
public final class JavaSEProcessSupport extends ProcessSupport {
    public Map<String, String> environment() {
        return new HashMap<String, String>(new java.lang.ProcessBuilder().environment());
    }
    public Process start(String[] command, String directory, Map<String, String> environment, boolean mergeError)
            throws IOException {
        java.lang.ProcessBuilder builder = new java.lang.ProcessBuilder(command);
        if (directory != null) {
            builder.directory(new java.io.File(directory));
        }
        if (environment != null) {
            builder.environment().clear();
            builder.environment().putAll(environment);
        }
        builder.redirectErrorStream(mergeError);
        return new Child(builder.start());
    }
    private static final class Child extends Process {
        private final java.lang.Process child;
        Child(java.lang.Process child) {
            this.child = child;
        }
        public InputStream getInputStream() {
            return child.getInputStream();
        }
        public InputStream getErrorStream() {
            return child.getErrorStream();
        }
        public OutputStream getOutputStream() {
            return child.getOutputStream();
        }
        public int exitValue() {
            return child.exitValue();
        }
        public void destroy() {
            child.destroy();
        }
        public Process destroyForcibly() {
            child.destroyForcibly();
            return this;
        }
    }
}
