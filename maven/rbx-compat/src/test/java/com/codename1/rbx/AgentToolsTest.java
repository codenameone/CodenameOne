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
package com.codename1.rbx;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// An agent's session against the studio tools: start from a template,
/// add a part and a script, playtest, export.
class AgentToolsTest {
    private static final class Studio implements AgentTools.Studio {
        Engine engine = new Engine(Engine.SOLO);

        @Override
        public Engine edited() {
            return engine;
        }

        @Override
        public void replace(Engine e) {
            engine = e;
        }

        @Override
        public byte[] screenshot() {
            return null;
        }

        @Override
        public String publish(String notes) {
            return "published: " + notes;
        }
    }

    @Test
    void buildsAndPlaytestsAnExperience() {
        Studio s = new Studio();
        AgentTools t = new AgentTools(s);
        assertTrue(t.call("list_templates", "{}").contains("obby"));
        assertTrue(t.call("apply_template", "{\"id\":\"simulator\"}").contains("report"));
        String part = t.call("create_instance", "{\"class\":\"Part\",\"parent\":\"Workspace\",\"name\":\"Beacon\","
                + "\"properties\":{\"Size\":[2,8,2],\"CFrame\":[10,4,10,0,45,0],\"Color\":\"#ff00aa\","
                + "\"Material\":\"Neon\",\"Anchored\":true},\"tags\":[\"Beacon\"]}");
        assertTrue(part.contains("\"Material\":\"Neon\""), part);
        assertTrue(part.contains("[255,0,170]"), part);
        t.call("add_effect", "{\"kind\":\"fire\",\"target\":\"Workspace.Beacon\"}");
        t.call("add_effect", "{\"kind\":\"bloom\",\"intensity\":1.2}");
        t.call("set_lighting", "{\"preset\":\"dusk\"}");
        String w = t.call("write_script", "{\"parent\":\"ServerScriptService\",\"name\":\"Hello\","
                + "\"source\":\"print('beacon at', workspace.Beacon.Position.X)\"}");
        assertTrue(w.contains("\"syntaxError\":null"), w);
        String bad = t.call("write_script", "{\"parent\":\"ServerScriptService\",\"name\":\"Broken\","
                + "\"source\":\"local x = \"}");
        assertTrue(!bad.contains("\"syntaxError\":null"), bad);
        t.call("delete_instance", "{\"path\":\"ServerScriptService.Broken\"}");
        String run = t.call("run_luau", "{\"source\":\"return #workspace:GetChildren()\"}");
        assertTrue(run.contains("\"results\":["), run);
        String play = t.call("playtest", "{\"seconds\":3}");
        assertTrue(play.contains("beacon at"), play);
        assertTrue(play.contains("TEMPLATE READY simulator"), play);
        assertTrue(play.contains("\"Gems\":"), play);
        assertTrue(play.contains("\"errors\":[]"), play);
        assertTrue(t.call("export_place", "{}").contains("Beacon"));
        assertTrue(t.call("place_tree", "{\"root\":\"Workspace\",\"depth\":1}").contains("Workspace.Beacon"));
        assertTrue(t.call("publish", "{\"notes\":\"v1\"}").contains("published: v1"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> t.call("set_properties", "{\"path\":\"Workspace.Beacon\",\"properties\":{\"Bogus\":1}}"));
        assertTrue(ex.getMessage().contains("Bogus"));
    }
}
