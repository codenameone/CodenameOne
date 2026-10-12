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

import com.codename1.io.Process;
import com.codename1.io.ProcessBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/** Same real-child contract on the JVM and translated desktop/backend runtime. */
public final class ProcessConformance {
    private static String fixture;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes = new byte[4096]; int n;
        while ((n = in.read(bytes)) != -1) out.write(bytes, 0, n);
        return out.toByteArray();
    }
    private static String output(ProcessBuilder builder) throws Exception {
        Process p = builder.start();
        try {
            p.getOutputStream().close();
            String text = new String(read(p.getInputStream()), "UTF-8");
            check(p.waitFor(10000), "exit timeout");
            check(p.exitValue() == 0, "exit status");
            return text;
        } finally { p.close(); }
    }
    private static final class Drain extends Thread {
        final InputStream in;
        byte[] bytes;
        Throwable error;
        Drain(InputStream in) { this.in = in; }
        public void run() { try { bytes = read(in); } catch (Throwable ex) { error = ex; } }
        void finish() throws Exception { join(15000); check(!isAlive() && error == null, "drain failed: " + error); }
    }
    public static void main(String[] args) throws Exception {
        fixture = args[0];
        check(ProcessBuilder.isSupported(), "support");
        String[] literals = {"", "two words", "quote\"here", "\\\\trailing\\", "a\\\"b", "שלום-🙂", "$HOME;|&<>*?"};
        String[] command = new String[literals.length + 2]; command[0] = fixture; command[1] = "args";
        String expected = "";
        for (int i = 0; i < literals.length; ++i) { command[i + 2] = literals[i]; expected += literals[i] + '\0'; }
        check(expected.equals(output(new ProcessBuilder(command))), "literal/Unicode arguments");
        ProcessBuilder env = new ProcessBuilder(fixture, "env");
        env.environment().put("CN1_PROCESS_TEST", "value with spaces");
        check("value with spaces".equals(output(env)), "environment override");
        env.environment().remove("CN1_PROCESS_TEST");
        check("<absent>".equals(output(env)), "environment removal");
        env.environment().clear();
        check("<absent>".equals(output(env)), "empty environment");
        check(output(new ProcessBuilder(fixture, "cwd").directory(args[1])).equals(args[1]), "working directory");
        try { new ProcessBuilder(fixture + "-missing").start(); throw new AssertionError("missing executable accepted"); }
        catch (IOException expectedFailure) { }
        try { new ProcessBuilder(fixture, "bad\0argument").start(); throw new AssertionError("NUL accepted"); }
        catch (IllegalArgumentException expectedFailure) { }
        try { new ProcessBuilder(fixture, "exit").directory(args[1] + "/missing-dir").start(); throw new AssertionError("bad cwd accepted"); }
        catch (IOException expectedFailure) { }
        for (int merged = 0; merged < 2; ++merged) {
            Process p = new ProcessBuilder(fixture, "duplex").redirectErrorStream(merged == 1).start();
            try {
                Drain out = new Drain(p.getInputStream()), err = new Drain(p.getErrorStream());
                out.start(); err.start();
                byte[] payload = new byte[262144];
                for (int i = 0; i < payload.length; ++i) payload[i] = (byte)i;
                p.getOutputStream().write(payload); p.getOutputStream().close();
                check(p.waitFor(15, TimeUnit.SECONDS), "duplex timeout");
                out.finish(); err.finish();
                int prefix = 524288 * (merged + 1);
                check(out.bytes.length == prefix + payload.length, "stdout length");
                check(err.bytes.length == (merged == 1 ? 0 : 524288), "stderr length");
                for (int i = 0; i < prefix; ++i) check(out.bytes[i] == 37, "output corruption");
                for (int i = 0; i < payload.length; ++i) check(out.bytes[prefix + i] == payload[i], "binary roundtrip");
            } finally { p.close(); }
        }
        Process sleeping = new ProcessBuilder(fixture, "sleep").start();
        try {
            check(!sleeping.waitFor(30), "timed wait");
            check(sleeping.isAlive(), "timeout killed child");
            Thread.currentThread().interrupt();
            try { sleeping.waitFor(); throw new AssertionError("interrupt ignored"); }
            catch (InterruptedException expectedInterrupt) { }
            check(sleeping.isAlive(), "interrupt killed child");
            final Process interruptTarget = sleeping;
            final boolean[] interrupted = {false};
            Thread waiter = new Thread(new Runnable() {
                public void run() {
                    try { interruptTarget.waitFor(10000); }
                    catch (InterruptedException expectedInterrupt) { interrupted[0] = true; }
                }
            });
            waiter.start(); Thread.sleep(30); waiter.interrupt(); waiter.join(1000);
            check(!waiter.isAlive() && interrupted[0], "interrupt during timed wait");
            check(sleeping.isAlive(), "delayed interrupt killed child");
            sleeping.destroy(); check(sleeping.waitFor(5000), "destroy");
        } finally { sleeping.close(); }
        sleeping = new ProcessBuilder(fixture, "sleep").start();
        sleeping.close(); sleeping.close();
        check(sleeping.waitFor(5000), "close did not terminate");
        for (int i = 0; i < 40; ++i) {
            Process p = new ProcessBuilder(fixture, "exit", "7").start();
            try { check(p.waitFor(5000) && p.exitValue() == 7, "nonzero/repeated launch"); }
            finally { p.close(); }
        }
        System.out.println("PROCESS_CONFORMANCE_OK");
    }
}
