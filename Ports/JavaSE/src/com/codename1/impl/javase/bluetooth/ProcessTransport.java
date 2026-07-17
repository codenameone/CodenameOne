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
package com.codename1.impl.javase.bluetooth;

import com.codename1.bluetooth.helper.HelperTransport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

/**
 * The JavaSE simulator's {@link HelperTransport}: the one place the real
 * {@code cn1-ble-helper} child process is launched, via
 * {@link ProcessBuilder}. It owns the child process's stdin/stdout streams,
 * pumps its stderr to the console for diagnostics and registers a JVM
 * shutdown hook so the process is always torn down. The native
 * Windows/Linux ports supply their own {@code HelperTransport} over a
 * native subprocess bridge instead; the transport-agnostic
 * {@link com.codename1.bluetooth.helper.HelperBleBackend} is shared.
 */
class ProcessTransport implements HelperTransport {

    private final List<String> configuredCommand;

    private final Object lock = new Object();
    private Process process;
    private Writer stdin;
    private BufferedReader stdout;
    private Thread shutdownHook;

    /**
     * @param configuredCommand the launch command line this transport runs
     *                          when {@link #start(List)} is passed
     *                          {@code null} (the resolved helper binary, or
     *                          an explicit test command)
     */
    ProcessTransport(List<String> configuredCommand) {
        this.configuredCommand = configuredCommand;
    }

    public void start(List<String> command) throws IOException {
        List<String> launch = command != null ? command : configuredCommand;
        if (launch == null || launch.isEmpty()) {
            throw new IOException("no cn1-ble-helper launch command");
        }
        synchronized (lock) {
            Process p = new ProcessBuilder(launch).start();
            process = p;
            stdin = new BufferedWriter(new OutputStreamWriter(
                    p.getOutputStream(), "UTF-8"));
            stdout = new BufferedReader(new InputStreamReader(
                    p.getInputStream(), "UTF-8"));
            startStderrPump(p);
            final Process hooked = p;
            shutdownHook = new Thread("cn1ble-helper-shutdown") {
                public void run() {
                    destroyQuietly(hooked);
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    public String readLine() throws IOException {
        BufferedReader r;
        synchronized (lock) {
            r = stdout;
        }
        if (r == null) {
            return null;
        }
        return r.readLine();
    }

    public void writeLine(String line) throws IOException {
        synchronized (lock) {
            if (stdin == null) {
                throw new IOException("helper stdin closed");
            }
            stdin.write(line);
            stdin.write('\n');
            stdin.flush();
        }
    }

    public boolean isAlive() {
        synchronized (lock) {
            return process != null && process.isAlive();
        }
    }

    public void close() {
        Process p;
        Thread hook;
        Writer in;
        synchronized (lock) {
            p = process;
            hook = shutdownHook;
            in = stdin;
            stdin = null;
            stdout = null;
            process = null;
            shutdownHook = null;
        }
        if (hook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (IllegalStateException ignored) {
                // VM already exiting
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
        if (p != null) {
            try {
                // grace period for the helper to honor the shutdown command
                long deadline = System.currentTimeMillis() + 2000;
                while (p.isAlive()
                        && System.currentTimeMillis() < deadline) {
                    Thread.sleep(20);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            if (p.isAlive()) {
                p.destroy();
            }
        }
    }

    private static void destroyQuietly(Process p) {
        if (p != null && p.isAlive()) {
            p.destroy();
        }
    }

    private void startStderrPump(final Process p) {
        Thread t = new Thread("cn1ble-helper-stderr") {
            public void run() {
                try {
                    BufferedReader r = new BufferedReader(
                            new InputStreamReader(p.getErrorStream(),
                                    "UTF-8"));
                    String line;
                    while ((line = r.readLine()) != null) {
                        System.err.println("[Cn1BleHelper] " + line);
                    }
                } catch (IOException ignored) {
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }
}
