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
package com.codename1.rbx.place;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.rbx.Engine;
import com.codename1.rbx.Instance;
import com.codename1.rbx.RigidBodies;
import com.codename1.rbx.Scheduler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// Plays every starter template headless with one player and physics on: a
/// driver script under `src/test/resources/templates` walks the player
/// through the template's loop and checks the leaderstats it earns.
class TemplatesTest {
    private static String text(String name) throws IOException {
        InputStream in = TemplatesTest.class.getResourceAsStream("/templates/" + name);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) {
            b.write(buf, 0, n);
        }
        in.close();
        return new String(b.toByteArray(), "UTF-8");
    }

    @ParameterizedTest
    @ValueSource(strings = {"obby", "tycoon", "arena", "simulator", "racing"})
    void plays(String id) throws IOException {
        final List<String> out = new ArrayList<String>();
        Engine e = new Engine(Engine.SOLO);
        e.setPhysics(new RigidBodies());
        e.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                out.add(type + ": " + text);
            }
        });
        CompatReport rep = Templates.load(e, id);
        assertTrue(rep.unknownClasses.isEmpty() && rep.unknownProperties.isEmpty() && rep.badValues.isEmpty(),
                rep.toString());
        e.addPlayer(1, "Tester", null);
        Instance s = e.create("Script");
        s.setName("Driver");
        s.put("Source", text("common.lua") + text(id + ".lua"));
        s.setParent(e.service("ServerScriptService"));
        for (int i = 0; i < 60 * 30 && !finished(out); i++) {
            e.step(1 / 60.0);
        }
        String all = out.toString();
        assertTrue(all.contains("TEMPLATE READY " + id), all);
        assertTrue(all.contains(": DONE") && !all.contains("error"), all);
    }

    /// A character dropped onto a kill brick dies on it and its body stays
    /// on the surface. It once hovered over the obby's lava pool, standing on
    /// a pad 11 studs away whose side-turned axis was never bounds-checked.
    @org.junit.jupiter.api.Test
    void aBodyDroppedOnLavaDiesAndStaysOnIt() throws IOException {
        final List<String> out = new ArrayList<String>();
        Engine e = new Engine(Engine.SOLO);
        e.setPhysics(new RigidBodies());
        e.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                out.add(type + ": " + text);
            }
        });
        Templates.load(e, "obby");
        e.addPlayer(1, "Tester", null);
        Instance s = e.create("Script");
        s.setName("Drop");
        s.put("Source", "local P = game:GetService('Players')\n"
                + "task.wait(1)\n"
                + "local c = P:GetPlayers()[1].Character\n"
                + "local lava = workspace:FindFirstChild('Lava1', true)\n"
                + "c:PivotTo(CFrame.new(lava.Position + Vector3.new(9, 6, 0)))\n"
                + "local rp = RaycastParams.new() rp.FilterDescendantsInstances = {c} rp.FilterType = Enum.RaycastFilterType.Exclude\n"
                + "local r = workspace:Raycast(c.HumanoidRootPart.Position, Vector3.new(0, -50, 0), rp)\n"
                + "print('HIT', r and r.Instance.Name, r and math.floor(r.Distance))\n"
                + "local h = c:FindFirstChildOfClass('Humanoid')\n"
                + "local low = 1e9\n"
                + "for i = 1, 90 do task.wait() low = math.min(low, c.Head.Position.Y) end\n"
                + "print('DROP', h.Health, math.floor(low), math.floor(lava.Position.Y))\n");
        s.setParent(e.service("ServerScriptService"));
        for (int i = 0; i < 60 * 4; i++) {
            e.step(1 / 60.0);
        }
        String all = out.toString();
        String line = null;
        for (String o : out) {
            if (o.contains("DROP")) {
                line = o;
            }
        }
        assertTrue(line != null, all);
        // A raycast that hits reports its part and distance: building the
        // result once cast the hit's material to a distance and threw.
        assertTrue(all.contains("HIT Lava1 5"), all);
        String[] f = line.substring(line.indexOf("DROP")).split(" ");
        assertTrue(Double.parseDouble(f[1]) == 0, "still alive on the lava: " + line);
        assertTrue(Double.parseDouble(f[2]) >= Double.parseDouble(f[3]), "sank through the lava: " + line);
    }

    private static boolean finished(List<String> out) {
        for (String s : out) {
            if (s.endsWith(": DONE") || s.startsWith("error")) {
                return true;
            }
        }
        return false;
    }
}
