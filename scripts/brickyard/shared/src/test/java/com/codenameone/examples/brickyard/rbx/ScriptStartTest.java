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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/// A server's scripts start before its first player, and a part's
/// Position tweens like a property although it is the CFrame's.
class ScriptStartTest {
    private static List<String> capture(Engine e) {
        final List<String> out = new ArrayList<String>();
        e.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                out.add(type + ": " + text);
            }
        });
        return out;
    }

    @Test
    void playerAddedSeesAPlayerWhoJoinsBeforeTheFirstFrame() {
        Engine e = new Engine(Engine.SOLO);
        List<String> out = capture(e);
        Instance s = e.create("Script");
        s.put("Source", "game.Players.PlayerAdded:Connect(function(p) print('hi ' .. p.Name) end)\n"
                + "print('players at start', #game.Players:GetPlayers())");
        s.setParent(e.service("ServerScriptService"));
        e.addPlayer(1, "First", null);
        e.step(1 / 60.0);
        assertEquals("[output: players at start 0, output: hi First]", out.toString());
    }

    @Test
    void tweeningPositionMovesThePartAndKeepsItsRotation() {
        Engine e = new Engine(Engine.SOLO);
        List<String> out = capture(e);
        Instance s = e.create("Script");
        s.put("Source", "local p = Instance.new('Part')\n"
                + "p.Anchored = true\n"
                + "p.CFrame = CFrame.new(0, 5, 0) * CFrame.Angles(0, math.rad(90), 0)\n"
                + "p.Parent = workspace\n"
                + "local t = game:GetService('TweenService'):Create(p, TweenInfo.new(1), { Position = Vector3.new(10, 5, 0) })\n"
                + "t:Play()\n"
                + "t.Completed:Wait()\n"
                + "print(math.floor(p.Position.X + 0.5), math.floor(p.Orientation.Y + 0.5))");
        s.setParent(e.service("ServerScriptService"));
        for (int i = 0; i < 120; i++) {
            e.step(1 / 60.0);
        }
        assertTrue(out.contains("output: 10 90"), out.toString());
    }
}
