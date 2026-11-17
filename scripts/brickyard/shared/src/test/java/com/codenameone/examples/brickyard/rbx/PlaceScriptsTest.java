/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codenameone.examples.brickyard.rbx;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// Runs each Luau script under `src/test/resources/rbx` as a server Script in
/// a solo engine with one player, for up to 20 simulated seconds. A script
/// passes by printing DONE with no error on the way.
class PlaceScriptsTest {
    static String read(String name) throws IOException {
        InputStream in = PlaceScriptsTest.class.getResourceAsStream("/rbx/" + name);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) {
            b.write(buf, 0, n);
        }
        in.close();
        return new String(b.toByteArray(), "UTF-8");
    }

    static List<String> run(String source) {
        final List<String> out = new ArrayList<String>();
        Engine e = new Engine(Engine.SOLO);
        e.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                out.add(type + ": " + text);
            }
        });
        e.addPlayer(1, "Tester", null);
        Instance s = e.create("Script");
        s.setName("Test");
        s.put("Source", source);
        s.setParent(e.service("ServerScriptService"));
        for (int i = 0; i < 1200 && !done(out); i++) {
            e.step(1 / 60.0);
        }
        return out;
    }

    static boolean done(List<String> out) {
        for (String s : out) {
            if (s.endsWith(": DONE") || s.startsWith("error")) {
                return true;
            }
        }
        return false;
    }

    @ParameterizedTest
    @ValueSource(strings = {"datatypes.luau", "instances.luau", "scheduler.luau", "services.luau", "characters.luau"})
    void scriptPasses(String name) throws IOException {
        List<String> out = run(read(name));
        boolean ok = false;
        for (String s : out) {
            if (s.endsWith(": DONE")) {
                ok = true;
            }
        }
        for (String s : out) {
            if (s.startsWith("error")) {
                ok = false;
            }
        }
        assertTrue(ok, name + " output: " + out);
    }
}
