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

/// A creator makes a title and gives it a video, through the studio's own
/// screens; the server then has both, as a draft nobody else can see.
public class StudioFlowTest extends E2e {
    @Override
    protected String mode() {
        return Nav.STUDIO;
    }

    @Override
    protected boolean run() throws Exception {
        signIn(CREATOR, "Studio");
        String name = "E2E Title " + System.currentTimeMillis();
        click("newTitle");
        waitForForm("NewTitle");
        type("name", name);
        type("synopsis", "Made by the end-to-end test.");
        type("year", "2026");
        click("create");
        waitForForm("StudioTitle");
        shot("studio-title");
        click("addVideo");
        waitForForm("AddVideo");
        type("videoName", "The feature");
        click("add");
        waitForForm("StudioTitle");
        until(() -> findByName("video-0") != null, 20000, "the new video");
        boolean found = false;
        for (Object each : Actor.signIn(CREATOR).list("/api/studio/titles")) {
            if (each instanceof java.util.Map && name.equals(Actor.text((java.util.Map<String, Object>) each,
                    "name"))) {
                found = "DRAFT".equals(Actor.text((java.util.Map<String, Object>) each, "status"));
            }
        }
        assertTrue(found, "the title is a draft on the server");
        back();
        waitForForm("Studio");
        signOut();
        return true;
    }
}
