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
package com.codename1.cli;

import java.util.concurrent.atomic.AtomicBoolean;

/// The process's own terminal: [TerminalImpl] for the primitives,
/// `System.out` and `System.err` for output.
final class SystemTerminal implements TerminalBackend {
    private final AtomicBoolean prepared = new AtomicBoolean();

    @Override
    public boolean isTerminal(int fd) {
        return TerminalImpl.isTerminal(fd);
    }

    @Override
    public boolean isWindows() {
        return TerminalImpl.isWindows();
    }

    @Override
    public int size() {
        return TerminalImpl.size();
    }

    @Override
    public boolean enterRaw() {
        prepare();
        return TerminalImpl.enterRaw();
    }

    @Override
    public void restore() {
        TerminalImpl.restore();
    }

    @Override
    public int readByte(int timeoutMs) {
        return TerminalImpl.readByte(timeoutMs);
    }

    @Override
    public void write(String text) {
        prepare();
        System.out.print(text);
        System.out.flush();
    }

    @Override
    public void writeError(String text) {
        prepare();
        System.err.print(text);
        System.err.flush();
    }

    @Override
    public String getenv(String name) {
        return System.getenv(name);
    }

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private void prepare() {
        if (prepared.compareAndSet(false, true)) {
            TerminalImpl.prepareOutput();
        }
    }

    @Override
    public String readHidden() {
        return TerminalImpl.readHidden();
    }

    @Override
    public void cursorHidden(boolean hidden) {
        TerminalImpl.cursorHidden(hidden);
    }
}
