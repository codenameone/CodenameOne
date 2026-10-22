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
package com.codenameone.examples.parley;

import com.codename1.ui.Button;
import com.codenameone.examples.parley.net.Session;
import com.codenameone.examples.parley.ui.Nav;

import com.codename1.ui.Component;
import com.codename1.components.Switch;
import com.codenameone.examples.parley.ui.Nav;
import java.util.Map;

/// The admin screens: the numbers, the seeded report resolved, and an
/// account made an admin and blocked -- each checked against what the
/// server then does with that account's token.
public class AdminTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        final Actor dan = Actor.signIn("dan@parley.example");
        signIn(ADMIN, "Chats");
        onEdt(() -> Nav.switchTo(Nav.ADMIN));
        waitForForm("Admin");
        until(() -> text("statUsers").length() > 0 && !"0".equals(text("statUsers")), 20000,
                "the dashboard's numbers");
        shot("admin-dashboard");

        // The report the demo seeds, resolved with a reason.
        until(() -> has("alert-reports"), 20000, "the dashboard to say a report is waiting");
        click("alert-reports");
        waitForForm("Reports");
        until(this::hasReport, 20000, "the seeded report");
        clickReport();
        waitForForm("Report");
        until(() -> findByName("evidence") != null, 20000, "the evidence the reporter attached");
        shot("admin-report");
        click("resolve");
        waitForForm("Reason");
        type("reason", "Warned the account");
        click("confirm");
        waitForForm("Reports");
        back();
        waitForForm("Admin");

        // Make Dan an admin, and take it away.
        click("users");
        waitForForm("Users");
        click("user-dan@parley.example");
        waitForForm("User");
        onEdt(() -> {
            Component toggle = findByName("admin");
            ((Switch) toggle).setValue(true);
        });
        until(() -> {
            dan.get("/api/admin/stats");
            return dan.status == 200;
        }, 20000, "the grant to reach dan's existing token");
        Map<String, Object> me = dan.get("/api/me");
        assertEqual("true", String.valueOf(me.get("admin")), "/api/me reports the new role");
        onEdt(() -> ((Switch) findByName("admin")).setValue(false));
        until(() -> {
            dan.get("/api/admin/stats");
            return dan.status == 403;
        }, 20000, "the role to be taken away again");

        // Blocking refuses the account's token, and lifting it lets it back.
        click("block");
        waitForForm("Reason");
        click("confirm");
        until(() -> text("error").length() > 0, 20000, "a block with no reason to be refused");
        type("reason", "Asked to be locked while travelling");
        click("confirm");
        untilText("standing", "Blocked");
        dan.get("/api/me");
        assertEqual(401, dan.status, "a blocked account's token");
        click("unblock");
        untilText("standing", "In good standing");
        until(() -> {
            dan.get("/api/me");
            return dan.status == 200;
        }, 20000, "the block to be lifted");
        back();
        waitForForm("Users");
        back();
        waitForForm("Admin");

        click("bridges");
        waitForForm("Bridges");
        back();
        waitForForm("Admin");
        onEdt(() -> Nav.switchTo(Nav.CHATS));
        waitForForm("Chats");
        onEdt(Nav::signOut);
        waitForForm("Welcome");
        return true;
    }

    private boolean hasReport() {
        return firstReport() != null;
    }

    private void clickReport() {
        final String name = firstReport();
        click(name);
    }

    private String firstReport() {
        final String[] found = new String[1];
        onEdt(() -> found[0] = named(com.codename1.ui.Display.getInstance().getCurrent()));
        return found[0];
    }

    private static String named(com.codename1.ui.Container in) {
        for (int iter = 0; in != null && iter < in.getComponentCount(); iter++) {
            Component each = in.getComponentAt(iter);
            String name = each.getName();
            if (name != null && name.startsWith("report-")) {
                return name;
            }
            if (each instanceof com.codename1.ui.Container) {
                String inner = named((com.codename1.ui.Container) each);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }
}
