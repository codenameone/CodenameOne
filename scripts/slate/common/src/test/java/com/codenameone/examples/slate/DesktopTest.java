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
package com.codenameone.examples.slate;

import com.codenameone.examples.slate.client.Times;
import com.codenameone.examples.slate.ui.Timeline;

import java.util.Map;

/// The app as a desktop window: the owner's console, the provider's diary and
/// a client booking, each captured, in the run of the tests with the simulator
/// as a desktop. On a window this wide the menu is docked down the side and
/// the screens are laid out side by side.
public class DesktopTest extends E2e {
    @Override
    protected boolean desktop() {
        return true;
    }

    @Override
    protected boolean run() throws Exception {
        onEdt(() -> AppConfig.setMode(""));
        signIn(OWNER, "Owner");
        until(() -> findByName("chart-booked") != null, 20000, "the dashboard's chart");
        shot("desktop-owner-dashboard");
        click("open-board");
        waitForForm("Board");
        until(() -> findByName("board-grid") instanceof Timeline
                && OwnerTest.booked((Timeline) findByName("board-grid")), 20000,
                "the day's bookings on the board");
        shot("desktop-owner-board");
        place("menu-services", "Catalog");
        // The list on the left, the first row's editor beside it.
        until(() -> findByName("detail") != null && has("save"), 20000,
                "the first service's editor beside the list");
        shot("desktop-owner-catalog");
        place("menu-policy", "Policy");
        until(() -> has("run-preview"), 20000, "the rules");
        shot("desktop-owner-policy");
        signOut();

        onEdt(() -> AppConfig.setMode(""));
        signIn(PROVIDER, "Provider");
        until(() -> findByName("timeline-grid") instanceof Timeline
                && OwnerTest.booked((Timeline) findByName("timeline-grid")), 20000,
                "today's bookings");
        shot("desktop-provider-day");
        click("view-week");
        until(() -> findByName("timeline-grid") instanceof Timeline
                && ((Timeline) findByName("timeline-grid")).lanes().size() == 7, 20000, "the week");
        shot("desktop-provider-week");
        place("menu-calendars", "Calendars");
        until(() -> has("connect-ics"), 20000, "the ways to connect a calendar");
        shot("desktop-provider-calendars");
        place("menu-hours", "Hours");
        until(() -> has("save"), 20000, "the week");
        shot("desktop-provider-hours");
        signOut();

        Actor client = Actor.signIn(CLIENT);
        String serviceId = "";
        for (Object s : client.list("/api/services")) {
            Object capacity = s instanceof Map ? ((Map<?, ?>) s).get("capacity") : null;
            if (serviceId.length() == 0 && capacity instanceof Number
                    && ((Number) capacity).intValue() == 1 && s instanceof Map) {
                Object id = ((Map<?, ?>) s).get("id");
                serviceId = id == null ? "" : String.valueOf(id);
            }
        }
        assertTrue(serviceId.length() > 0, "the demo business offers a one-to-one service");
        onEdt(() -> AppConfig.setMode(""));
        signIn(CLIENT, "Client");
        shot("desktop-client-home");
        click("service-" + serviceId);
        waitForForm("Book");
        boolean found = false;
        for (int d = 1; d <= 7 && !found; d++) {
            click("day-" + Times.iso(Times.addDays(System.currentTimeMillis(), d)));
            long deadline = System.currentTimeMillis() + 8000;
            while (!found && System.currentTimeMillis() < deadline) {
                found = firstNamed("slot-").length() > 0;
                waitFor(100);
            }
        }
        assertTrue(found, "a day in the coming week has a free start");
        shot("desktop-client-book");
        // No screen of its own on a desktop: the start is held and its price,
        // terms and button are drawn beside the times.
        click(firstNamed("slot-"));
        until(() -> findByName("confirm-pane") != null && has("confirm"), 20000,
                "the hold beside the times");
        shot("desktop-client-confirm");
        click("confirm");
        waitForForm("Booked");
        return true;
    }

    /// Opens a place from the menu: docked beside the screen on a wide window,
    /// behind its button on a narrow one.
    private void place(String item, String form) {
        clickIfThere("menu-open");
        click(item);
        waitForForm(form);
    }
}
