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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.rbx.AgentTools;
import com.codename1.rbx.Engine;
import com.codename1.rbx.Instance;
import org.junit.jupiter.api.Test;

/// Every toolbox model and every snippet runs as written: a lesson that
/// throws teaches nothing.
class ModelsTest {
    private static final class Host implements AgentTools.Studio {
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
            return notes;
        }
    }

    @Test
    void everyModelBuildsAndEveryScriptRuns() {
        Host h = new Host();
        AgentTools t = new AgentTools(h);
        t.call("apply_template", "{\"id\":\"baseplate\"}");
        for (int i = 0; i < Models.count(); i++) {
            String r = t.call("run_luau", "{\"source\":" + AgentTools.jsonString(Models.luau(i, 8 * i, 0, 0)) + "}");
            assertFalse(r.contains("\"error\""), Models.name(i) + ": " + r);
            Instance m = h.engine.getWorkspace().findFirstChild(Models.name(i).replace(" ", ""));
            assertNotNull(m, Models.name(i));
            assertFalse(m.getChildren().isEmpty(), Models.name(i));
        }
        Instance kill = h.engine.getWorkspace().findFirstChild("Waterpool").findFirstChild("Rim");
        for (int i = 0; i < Models.snippetCount(); i++) {
            String parent = Models.snippetName(i).startsWith("Kill") || Models.snippetName(i).startsWith("Moving")
                    ? kill.fullName() : "ServerScriptService";
            t.call("write_script", "{\"parent\":" + AgentTools.jsonString(parent) + ",\"name\":\"S" + i
                    + "\",\"source\":" + AgentTools.jsonString(Models.snippet(i)) + "}");
        }
        String run = t.call("playtest", "{\"seconds\":6,\"players\":1}");
        assertTrue(run.contains("\"errors\":[]"), run);
        assertTrue(run.contains("Welcome to the game"), run);
        assertTrue(run.contains("Go!"), run);
    }
}
