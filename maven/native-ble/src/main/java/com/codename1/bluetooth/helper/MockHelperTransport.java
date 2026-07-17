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
package com.codename1.bluetooth.helper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A scripted {@link HelperTransport} for deterministic, subprocess-free
 * tests of {@link HelperBleBackend} -- the seam that lets any module drive
 * the transport-agnostic backend end to end without launching the real
 * {@code cn1-ble-helper}. Incoming helper lines are supplied programmatically
 * via {@link #feedLine(String)} (and terminated with {@link #end()}); every
 * command the backend writes is recorded and can be observed via
 * {@link #writtenCommands()} or awaited with {@link #takeWritten(long)}, so a
 * test can respond to exactly what the backend sent.
 *
 * <p>This is test scaffolding shipped in {@code src/main/java} so tests in
 * other modules (the JavaSE simulator and the native ports) can reuse it; it
 * is not part of the translated runtime.</p>
 */
public final class MockHelperTransport implements HelperTransport {

    /** Identity sentinel enqueued to make {@link #readLine()} return null. */
    private static final String EOF = new String("<<mock-helper-eof>>");

    private final LinkedBlockingQueue<String> incoming =
            new LinkedBlockingQueue<String>();
    private final LinkedBlockingQueue<String> written =
            new LinkedBlockingQueue<String>();
    private final CopyOnWriteArrayList<String> writtenLog =
            new CopyOnWriteArrayList<String>();

    private volatile boolean started;
    private volatile boolean alive;

    public void start(List<String> command) throws IOException {
        started = true;
        alive = true;
    }

    public String readLine() throws IOException {
        try {
            String line = incoming.take();
            if (line == EOF) {
                alive = false;
                return null;
            }
            return line;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public void writeLine(String line) throws IOException {
        if (!alive) {
            throw new IOException("mock helper transport is closed");
        }
        writtenLog.add(line);
        written.add(line);
    }

    public void close() {
        alive = false;
        incoming.add(EOF);
    }

    public boolean isAlive() {
        return alive;
    }

    /** True once {@link #start(List)} has been called. */
    public boolean isStarted() {
        return started;
    }

    // ------------------------------------------------------------------
    // scripting API (test side)
    // ------------------------------------------------------------------

    /** Queues one helper output line for the backend's reader thread. */
    public void feedLine(String line) {
        incoming.add(line);
    }

    /** Queues several helper output lines in order. */
    public void feedLines(String... lines) {
        for (int i = 0; i < lines.length; i++) {
            incoming.add(lines[i]);
        }
    }

    /** Signals end of stream: the helper exited / closed its stdout. */
    public void end() {
        incoming.add(EOF);
    }

    /** Snapshot of every command the backend has written so far. */
    public List<String> writtenCommands() {
        return new CopyOnWriteArrayList<String>(writtenLog);
    }

    /**
     * Blocks up to {@code timeoutMs} for the next command the backend writes
     * and returns it, or {@code null} on timeout. Each written command is
     * returned once, in order, so a test can respond to each in turn.
     */
    public String takeWritten(long timeoutMs) {
        try {
            return written.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
