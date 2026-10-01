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
package com.codename1.build;

/// A [Log] that prints to standard output and standard error, for tests and
/// command-line use. Debug output is off.
public class SystemStreamLog implements Log {
    private static void print(java.io.PrintStream out, String level, CharSequence content, Throwable error) {
        out.println("[" + level + "] " + (content == null ? "" : content));
        if (error != null) {
            error.printStackTrace(out);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(CharSequence content) {
        debug(content, null);
    }

    @Override
    public void debug(CharSequence content, Throwable error) {
        if (isDebugEnabled()) {
            print(System.out, "debug", content, error);
        }
    }

    @Override
    public void debug(Throwable error) {
        debug(null, error);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(CharSequence content) {
        info(content, null);
    }

    @Override
    public void info(CharSequence content, Throwable error) {
        print(System.out, "info", content, error);
    }

    @Override
    public void info(Throwable error) {
        info(null, error);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(CharSequence content) {
        warn(content, null);
    }

    @Override
    public void warn(CharSequence content, Throwable error) {
        print(System.out, "warning", content, error);
    }

    @Override
    public void warn(Throwable error) {
        warn(null, error);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(CharSequence content) {
        error(content, null);
    }

    @Override
    public void error(CharSequence content, Throwable error) {
        print(System.err, "error", content, error);
    }

    @Override
    public void error(Throwable error) {
        error(null, error);
    }
}
