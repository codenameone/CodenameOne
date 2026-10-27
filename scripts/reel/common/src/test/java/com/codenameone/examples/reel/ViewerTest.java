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

import java.util.List;
import java.util.Map;

/// A viewer signs in, finds a title on Home, opens it, puts it on their list
/// and finds it again by searching.
public class ViewerTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(VIEWER, "Home");
        until(() -> firstTitle() != null, 30000, "a title on Home");
        shot("viewer-home");
        final String name = firstTitle().getName();
        String titleId = name.substring("title-".length());
        Actor me = Actor.signIn(VIEWER);
        me.send("DELETE", "/api/library/list/" + titleId + "?profile=", null);

        click(name);
        waitForForm("Title");
        click("myList");
        until(() -> listed(me, titleId), 20000, "the title on the list");
        shot("viewer-title");
        back();
        waitForForm("Home");

        click("search");
        waitForForm("Search");
        Map<String, Object> title = me.get("/api/titles/" + titleId);
        String word = Actor.text(title, "name");
        type("query", word.length() > 4 ? word.substring(0, 4) : word);
        until(() -> findByName(name) != null, 20000, "the title in the results");
        back();
        waitForForm("Home");
        signOut();
        return true;
    }

    private com.codename1.ui.Component firstTitle() {
        return first(com.codename1.ui.Display.getInstance().getCurrent());
    }

    private static com.codename1.ui.Component first(com.codename1.ui.Container c) {
        if (c == null) {
            return null;
        }
        for (int iter = 0; iter < c.getComponentCount(); iter++) {
            com.codename1.ui.Component each = c.getComponentAt(iter);
            if (each.getName() != null && each.getName().startsWith("title-")) {
                return each;
            }
            if (each instanceof com.codename1.ui.Container) {
                com.codename1.ui.Component found = first((com.codename1.ui.Container) each);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean listed(Actor me, String titleId) {
        List<Object> list = me.list("/api/library/list?profile=");
        for (Object each : list) {
            if (each instanceof Map && titleId.equals(Actor.text((Map<String, Object>) each, "id"))) {
                return true;
            }
        }
        return false;
    }
}
