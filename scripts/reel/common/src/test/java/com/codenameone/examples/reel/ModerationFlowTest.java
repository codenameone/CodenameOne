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
package com.codenameone.examples.reel;

import com.codenameone.examples.reel.ui.Nav;

/// A moderator opens the queue and adds a word to the blocklist, then takes it
/// off again.
public class ModerationFlowTest extends E2e {
    @Override
    protected String mode() {
        return Nav.MODERATION;
    }

    @Override
    protected boolean run() throws Exception {
        signIn(MODERATOR, "Moderation");
        until(() -> findByName("queue") != null, 20000, "the queue");
        shot("moderation-queue");
        click("menu-open");
        click("menu-blocklist");
        final String word = "e2eword" + (System.currentTimeMillis() % 100000);
        type("blockValue", word);
        type("blockNote", "End-to-end test");
        click("block");
        final Actor me = Actor.signIn(MODERATOR);
        until(() -> blocked(me, word) != null, 20000, "the word on the blocklist");
        until(() -> findByName("unblock-0") != null, 20000, "the blocklist");
        String id = blocked(me, word);
        me.send("DELETE", "/api/moderation/blocklist/" + id, null);
        signOut();
        return true;
    }

    private static String blocked(Actor me, String word) {
        for (Object each : me.list("/api/moderation/blocklist")) {
            if (each instanceof java.util.Map
                    && word.equals(Actor.text((java.util.Map<String, Object>) each, "value"))) {
                return Actor.text((java.util.Map<String, Object>) each, "id");
            }
        }
        return null;
    }
}
