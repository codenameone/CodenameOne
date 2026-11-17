/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard.studio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.codenameone.examples.brickyard.rbx.Engine;
import com.codenameone.examples.brickyard.rbx.Instance;
import com.codenameone.examples.brickyard.rbx.Scheduler;
import com.codenameone.examples.brickyard.rbx.place.PlaceLoader;
import com.codenameone.examples.brickyard.rbx.place.PlaceSaver;
import org.junit.jupiter.api.Test;

/// The "Goal and win" snippet puts the objective on the player's screen and
/// keeps it current as coins are counted.
class GoalSnippetTest {
    @Test
    void goalLabelFollowsTheCoins() {
        final StringBuilder errors = new StringBuilder();
        Engine e = new Engine(Engine.SOLO);
        e.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                if (type.startsWith("error")) {
                    errors.append(text).append('\n');
                }
            }
        });
        Engine draft = new Engine(Engine.SOLO);
        Instance sss = draft.service("ServerScriptService");
        for (String name : new String[] {"Leaderstats", "Goal and win"}) {
            for (int i = 0; i < Models.snippetCount(); i++) {
                if (Models.snippetName(i).equals(name)) {
                    Instance s = draft.create("Script");
                    s.setName(name);
                    s.put("Source", Models.snippet(i));
                    s.setParent(sss);
                }
            }
        }
        PlaceLoader.loadPlace(e, PlaceSaver.save(draft));
        Instance p = e.addPlayer(1, "Mia", null);
        for (int i = 0; i < 30; i++) {
            e.step(1 / 60.0);
        }
        assertEquals("", errors.toString());
        Instance gui = p.findFirstChild("PlayerGui").findFirstChildOfClass("ScreenGui", false);
        assertNotNull(gui, "no ScreenGui");
        Instance label = gui.findFirstChildOfClass("TextLabel", false);
        assertEquals("Goal: collect 10 coins (0/10)", label.prop("Text"));
        p.findFirstChild("leaderstats").findFirstChild("Coins").put("Value", 10.0);
        for (int i = 0; i < 5; i++) {
            e.step(1 / 60.0);
        }
        assertEquals("You win! 10 coins", label.prop("Text"));
    }
}
