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

import com.codename1.ui.Display;
import com.codenameone.examples.reel.viewer.TitleForm;

import java.util.List;
import java.util.Map;

/// What watching looks like: a film's page with its rating and advisories,
/// sharing it, the player's controls and its Audio and subtitles sheet, and
/// a course with lessons, quizzes and captions in four languages.
///
/// The player is captured over the title's art. A run without packaged
/// media (CI) cannot start the stream; the controls stay up over the art
/// with the reason, which is the screen this captures there.
public class PlaybackTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(VIEWER, "Home");
        Actor me = Actor.signIn(VIEWER);
        String film = firstOfRow(me, "trending");
        String course = firstOfRow(me, "courses");

        if (course != null) {
            reveal("row-courses");
            shot("viewer-home-courses");
        }

        if (film != null) {
            open(film);
            shot("viewer-title-details");
            click("share");
            waitForForm("Share");
            until(() -> text("shareLink").indexOf("/watch/") > 0, 20000, "the shared link");
            shot("viewer-share");
            back();
            waitForForm("Title");
            click("play");
            waitForForm("Player");
            until(() -> has("tracks"), 30000, "the player's controls");
            shot("viewer-player");
            click("tracks");
            until(() -> findByName("tracksSheet") != null, 10000, "the Audio and subtitles sheet");
            shot("viewer-player-tracks");
            click("close");
            waitForForm("Title");
            back();
            waitForForm("Home");
        }

        if (course != null) {
            // Courses are in the subscription; start the demo viewer's trial
            // with the simulated card that always works, as a viewer would.
            if (Actor.text(me.get("/api/billing/subscription"), "planCode").length() == 0) {
                String setup = Actor.text(me.post("/api/payments/methods/setup", null), "id");
                me.post("/api/payments/methods/setup/" + setup + "/complete", "{\"number\":\"4242424242424242\","
                        + "\"expMonth\":12,\"expYear\":2040,\"cvc\":\"123\",\"holder\":\"Demo Viewer\"}");
                me.post("/api/billing/subscribe", "{\"planCode\":\"standard\"}");
            }
            open(course);
            until(() -> findByName("courseExplainer") != null, 20000, "the course explainer");
            reveal("courseProgress");
            shot("viewer-course");
            click("episode-1");
            waitForForm("Player");
            until(() -> has("tracks"), 30000, "the player's controls");
            click("tracks");
            until(() -> findByName("subtitle-es") != null, 10000, "the lesson's Spanish subtitles");
            click("subtitle-es");
            shot("viewer-lesson-subtitles");
            click("close");
            waitForForm("Title");
            back();
            waitForForm("Home");
        }
        signOut();
        return true;
    }

    private void open(final String titleId) {
        onEdt(() -> TitleForm.show(Display.getInstance().getCurrent(), titleId));
        waitForForm("Title");
        until(() -> has("myList"), 20000, "the title's page");
    }

    /// The first title of the home row called `row`, or null without one.
    private static String firstOfRow(Actor me, String row) {
        Object rows = me.get("/api/home?profile=").get("rows");
        if (!(rows instanceof List)) {
            return null;
        }
        for (Object each : (List<Object>) rows) {
            if (each instanceof Map && row.equals(Actor.text((Map<String, Object>) each, "id"))) {
                Object titles = ((Map<String, Object>) each).get("titles");
                if (titles instanceof List && !((List<Object>) titles).isEmpty()) {
                    return Actor.text((Map<String, Object>) ((List<Object>) titles).get(0), "id");
                }
            }
        }
        return null;
    }
}
