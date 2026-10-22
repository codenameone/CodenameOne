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
package com.codenameone.examples.skirmish;

import com.codenameone.examples.skirmish.forge.PackEditor;
import com.codenameone.examples.skirmish.rules.forge.Samples;

/// The Forge: the official pack, its first mission with the story stopping
/// the clock, and a new pack in the editor -- the pack, an arena, a card and
/// a mission.
public class ForgeTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(PLAYER);
        waitForForm("home");
        click("forgeButton");
        waitForForm("forge");
        until(() -> has("pack-" + Samples.OFFICIAL_ID), 20000, "the official pack");
        shot("forge-home");
        click("pack-" + Samples.OFFICIAL_ID);
        waitForForm("pack");
        until(() -> has("mission-hold-the-ford"), 20000, "the campaign");
        shot("forge-pack");
        click("mission-hold-the-ford");
        until(() -> has("mission-next"), 30000, "the first line of the story");
        shot("forge-mission-story");

        onEdt(() -> PackEditor.create("Frost Pass"));
        waitForForm("packEditor");
        shot("forge-editor");
        click("addArena");
        waitForForm("arenaEditor");
        shot("forge-arena");
        click("arenaDone");
        waitForForm("packEditor");
        click("addCard");
        waitForForm("cardEditor");
        shot("forge-card");
        click("cardDone");
        waitForForm("packEditor");
        click("mission-hold-the-ford");
        waitForForm("missionEditor");
        shot("forge-mission-editor");
        return true;
    }
}
