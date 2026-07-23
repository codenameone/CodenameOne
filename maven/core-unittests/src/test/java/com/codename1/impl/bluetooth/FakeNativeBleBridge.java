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
package com.codename1.impl.bluetooth;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A scripted {@link NativeBleBridge} for deterministic, hardware-free tests of
 * {@link NativeBleBackend} -- the in-process seam that replaces the old
 * subprocess {@code MockHelperTransport}. Inbound engine events are supplied
 * programmatically via {@link #feed(String)} (and terminated with
 * {@link #crash()}), and every typed command the backend issues is recorded so
 * a test can await it and echo its {@code requestId} back on the answering
 * event.
 *
 * <p>Honours the {@link NativeBleBridge} contract: {@link #pollEvent(long)}
 * blocks up to the timeout and returns {@code null} when nothing is queued (so
 * the backend's reader thread loops), the next queued event otherwise, and the
 * empty string once the engine has closed ({@link #close()} or
 * {@link #crash()}).</p>
 */
public final class FakeNativeBleBridge implements NativeBleBridge {

    /** One issued command: its protocol name and correlation id. */
    private static final class Cmd {
        private final String name;
        private final long id;

        Cmd(String name, long id) {
            this.name = name;
            this.id = id;
        }
    }

    /** Queued engine events; the empty string signals "engine closed". */
    private final LinkedBlockingQueue<String> events =
            new LinkedBlockingQueue<String>();
    private final LinkedBlockingQueue<Cmd> issued =
            new LinkedBlockingQueue<Cmd>();

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean alive = new AtomicBoolean(true);
    private final AtomicLong lastId = new AtomicLong();

    @Override
    public boolean start() {
        started.set(true);
        return true;
    }

    @Override
    public boolean isAlive() {
        return alive.get();
    }

    @Override
    public String pollEvent(long timeoutMillis) {
        String item;
        try {
            item = events.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "";
        }
        if (item == null) {
            // nothing queued: keep looping while alive, report closed after
            return alive.get() ? null : "";
        }
        // a real (non-empty) event, or the empty-string close marker
        return item;
    }

    @Override
    public void scanStart(long id, String serviceCsv) {
        record("scanStart", id);
    }

    @Override
    public void scanStop(long id) {
        record("scanStop", id);
    }

    @Override
    public void connect(long id, String address) {
        record("connect", id);
    }

    @Override
    public void disconnect(long id, String address) {
        record("disconnect", id);
    }

    @Override
    public void discover(long id, String address) {
        record("discover", id);
    }

    @Override
    public void read(long id, String address, String service,
            String characteristic) {
        record("read", id);
    }

    @Override
    public void write(long id, String address, String service,
            String characteristic, byte[] value, boolean noResponse) {
        record("write", id);
    }

    @Override
    public void subscribe(long id, String address, String service,
            String characteristic, boolean enable) {
        record("subscribe", id);
    }

    @Override
    public void readDescriptor(long id, String address, String service,
            String characteristic, String descriptor) {
        record("readDescriptor", id);
    }

    @Override
    public void writeDescriptor(long id, String address, String service,
            String characteristic, String descriptor, byte[] value) {
        record("writeDescriptor", id);
    }

    @Override
    public void readRssi(long id, String address) {
        record("readRssi", id);
    }

    @Override
    public void close() {
        signalClosed();
    }

    // ------------------------------------------------------------------
    // scripting API (test side)
    // ------------------------------------------------------------------

    /** Queues one engine event line for the backend's reader thread. */
    public void feed(String eventJson) {
        events.add(eventJson);
    }

    /** Simulates the native engine dying: pollEvent goes to "" / not-alive. */
    public void crash() {
        signalClosed();
    }

    /** The correlation id of the most recently issued command. */
    public long lastId() {
        return lastId.get();
    }

    /**
     * Blocks up to {@code timeoutMs} for the next issued command named
     * {@code name} and returns its correlation id. Non-matching commands seen
     * first are consumed, so a test observes commands in issue order.
     */
    public long awaitCommandId(String name, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Cmd cmd;
            try {
                cmd = issued.poll(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted awaiting command " + name);
            }
            if (cmd != null && name.equals(cmd.name)) {
                return cmd.id;
            }
        }
        throw new AssertionError("timed out waiting for command " + name);
    }

    private void record(String name, long id) {
        lastId.set(id);
        issued.add(new Cmd(name, id));
    }

    private void signalClosed() {
        if (alive.getAndSet(false)) {
            // wake a blocked pollEvent immediately with the close marker
            events.add("");
        }
    }
}
