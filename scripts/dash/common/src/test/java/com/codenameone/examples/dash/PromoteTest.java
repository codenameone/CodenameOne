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
package com.codenameone.examples.dash;

import com.codename1.ui.Component;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Tabs;

/// The console in a desktop window: the reports and their charts, a query
/// over the players, the engagement pipelines and one opened for editing,
/// then the Promote tab -- a push broadcast counted against its audience,
/// and a home spotlight that every player then sees.
public class PromoteTest extends E2e {
    @Override
    protected boolean desktop() {
        return true;
    }

    @Override
    protected boolean run() throws Exception {
        signIn(ADMIN);
        waitForForm("home");
        click("adminButton");
        waitForForm("admin");
        until(() -> has("stat-Open flags") && has("chartPlayers"), 20000, "the numbers and the charts");
        waitFor(800);
        shot("desktop-admin-overview");
        reveal("chartGems");
        shot("desktop-admin-reports");
        tab(6);
        until(() -> has("queryRun"), 20000, "the query tool");
        click("queryRun");
        until(() -> has("queryCount"), 20000, "the players the query found");
        shot("desktop-admin-query");
        tab(7);
        until(() -> has("pipeline-Win back"), 20000, "the pipelines");
        shot("desktop-admin-pipelines");
        click("pipelineEdit-Win back");
        until(() -> has("pipelineSave"), 20000, "the pipeline editor");
        click("pipelineCountButton");
        waitFor(1500);
        shot("desktop-admin-pipeline-edit");
        click("pipelineCancel");
        tab(8);
        until(() -> has("promoteTitle"), 20000, "the Promote tab");
        shot("desktop-admin-promote");
        type("promoteTitle", "Season two is here");
        type("promoteBody", "A new level is up. Come and race!");
        type("promoteCampaign", "season-two");
        reveal("promoteTitle");
        click("promoteCount");
        waitFor(1500);
        shot("desktop-admin-broadcast");
        // Send asks first; the shot is the question, and the answer is no.
        click("promoteSend");
        until(() -> Display.getInstance().getCurrent() instanceof Dialog, 20000, "the confirmation");
        shot("desktop-admin-broadcast-confirm");
        onEdt(() -> ((Dialog) Display.getInstance().getCurrent()).dispose());
        waitForForm("admin");
        reveal("spotlightText");
        type("spotlightText", "Season two: the Frost Arena is open");
        click("spotlightSave");
        waitFor(1500);
        reveal("linkCampaign");
        click("linkMake");
        waitFor(1500);
        shot("desktop-admin-spotlight");
        onEdt(() -> com.codenameone.examples.dash.ui.Nav.home());
        waitForForm("home");
        until(() -> has("spotlight"), 20000, "the spotlight banner");
        shot("desktop-home-spotlight");
        return true;
    }

    private void tab(final int index) {
        onEdt(() -> ((Tabs) findByName("adminTabs")).setSelectedIndex(index));
        waitFor(600);
    }

    private void reveal(final String name) {
        onEdt(() -> {
            Component c = findByName(name);
            c.getComponentForm().scrollComponentToVisible(c);
        });
        waitFor(600);
    }
}
