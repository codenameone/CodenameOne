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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.brickyard.rbx.CFrame;
import com.codenameone.examples.brickyard.rbx.Engine;
import com.codenameone.examples.brickyard.rbx.Instance;
import com.codenameone.examples.brickyard.rbx.Scheduler;
import com.codenameone.examples.brickyard.rbx.Vector3;
import com.codenameone.examples.brickyard.rbx.place.PlaceFile;
import com.codenameone.examples.brickyard.rbx.place.PlaceLoader;
import com.codenameone.examples.brickyard.rbx.place.PlaceSaver;
import com.codenameone.examples.brickyard.rbx.place.RbxlxWriter;
import org.junit.jupiter.api.Test;

/// The Logic tab's graph compiles to Luau, round-trips through its text form
/// and a saved place, and the compiled script runs in the ordinary sandbox.
class LogicGraphTest {
    @Test
    void generatesLuauCarryingTheGraph() {
        String luau = LogicGraph.collectible().toLuau();
        assertTrue(luau.startsWith(LogicGraph.HEADER + "touched|player|stat:Coins:1|flash|destroy]]\n"), luau);
        assertTrue(luau.contains("part.Touched:Connect(function(hit)"), luau);
        assertTrue(luau.contains("GetPlayerFromCharacter(hit.Parent)"), luau);
        assertTrue(luau.contains("value.Value += 1"), luau);
        assertTrue(luau.contains("part:Destroy()"), luau);
    }

    @Test
    void serializeParseRoundTrip() {
        LogicGraph g = new LogicGraph();
        assertTrue(g.addStat("Gems 2", 25));
        assertTrue(g.add(LogicGraph.FLASH));
        LogicGraph back = LogicGraph.parse(g.serialize());
        assertNotNull(back);
        assertEquals(g.serialize(), back.serialize());
        assertEquals(g.toLuau(), back.toLuau());
        assertNotNull(LogicGraph.fromSource(g.toLuau()));
        // Saving the generated text unchanged from the Script tab must not
        // detach it, even if the editor rewrote line endings or the tail.
        assertNotNull(LogicGraph.fromSource(g.toLuau().replace("\n", "\r\n")));
        assertNotNull(LogicGraph.fromSource(g.toLuau().trim()));
    }

    @Test
    void hostileOrEditedSourcesFallBackToText() {
        String[] bad = {null, "", "print('hi')", LogicGraph.HEADER, LogicGraph.HEADER + "touched|nuke]]\n",
            LogicGraph.HEADER + "touched|stat:\"..os.exit()..\":1]]\n", LogicGraph.HEADER + "touched|stat:Coins:x]]\n",
            LogicGraph.HEADER + "touched|stat:Coins:999999]]\n", LogicGraph.HEADER + "touched||]]\n",
            LogicGraph.HEADER + "touchedX]]\n", LogicGraph.HEADER + "touched|stat]]\n"};
        for (String s : bad) {
            assertNull(LogicGraph.fromSource(s), String.valueOf(s));
        }
        // A header that parses but a body that does not match it: a hand edit.
        String edited = LogicGraph.collectible().toLuau().replace("+= 1", "+= 100");
        assertNull(LogicGraph.fromSource(edited));
        assertTrue(LogicGraph.isDetached(edited));
        assertFalse(LogicGraph.isDetached("print('plain')"));
        assertFalse(LogicGraph.collectible().addStat("a\"b", 1));
    }

    @Test
    void placeSaveLoadKeepsTheGraph() {
        Engine draft = new Engine(Engine.SOLO);
        Instance coin = coin(draft);
        byte[] bytes = RbxlxWriter.writeBytes(PlaceSaver.save(draft));
        Engine loaded = new Engine(Engine.SOLO);
        PlaceLoader.loadPlace(loaded, PlaceFile.read(bytes));
        Instance back = loaded.getWorkspace().findFirstChild(coin.getName()).findFirstChild(LogicGraph.SCRIPT_NAME);
        assertNotNull(back);
        LogicGraph g = LogicGraph.fromSource((String) back.prop("Source"));
        assertNotNull(g);
        assertEquals(LogicGraph.collectible().serialize(), g.serialize());
    }

    @Test
    void touchingTheCoinAwardsIt() {
        final StringBuilder errors = new StringBuilder();
        Engine draft = new Engine(Engine.SOLO);
        coin(draft);
        Engine e = new Engine(Engine.SOLO);
        e.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                if (type.startsWith("error")) {
                    errors.append(text).append('\n');
                }
            }
        });
        PlaceLoader.loadPlace(e, PlaceSaver.save(draft));
        Instance p = e.addPlayer(1, "Mia", null);
        for (int i = 0; i < 30; i++) {
            e.step(1 / 60.0);
        }
        Instance root = p.ref("Character").findFirstChild("HumanoidRootPart");
        assertNotNull(e.getWorkspace().findFirstChild("Coin"));
        for (int i = 0; i < 30 && e.getWorkspace().findFirstChild("Coin") != null; i++) {
            root.put("CFrame", CFrame.at(20, 2.5, 0));
            e.step(1 / 60.0);
        }
        assertEquals("", errors.toString());
        Instance coins = p.findFirstChild("leaderstats").findFirstChild("Coins");
        assertEquals(1.0, ((Number) coins.prop("Value")).doubleValue(), 1e-9);
        assertNull(e.getWorkspace().findFirstChild("Coin"), "the collectible is destroyed once taken");
    }

    private static Instance coin(Engine draft) {
        Instance coin = draft.create("Part");
        coin.setName("Coin");
        coin.put("Anchored", Boolean.TRUE);
        coin.put("CanCollide", Boolean.FALSE);
        coin.put("Size", new Vector3(4, 4, 4));
        coin.put("CFrame", CFrame.at(20, 2.5, 0));
        coin.setParent(draft.getWorkspace());
        Instance s = draft.create("Script");
        s.setName(LogicGraph.SCRIPT_NAME);
        s.put("Source", LogicGraph.collectible().toLuau());
        s.setParent(coin);
        return coin;
    }
}
