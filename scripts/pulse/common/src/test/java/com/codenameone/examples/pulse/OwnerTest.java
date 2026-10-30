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
package com.codenameone.examples.pulse;

import com.codename1.ui.spinner.Picker;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/// The owner opens the dashboard and puts a class on the timetable from the
/// screens; the server is then asked whether the class is there.
public class OwnerTest extends E2e {
    @Override
    @SuppressWarnings("unchecked")
    protected boolean run() throws Exception {
        Actor owner = Actor.signIn(OWNER);
        // Early on a day weeks away, so the class clashes with nothing the
        // demo timetable or an earlier run put there.
        Calendar at = Calendar.getInstance();
        at.setTime(new Date(System.currentTimeMillis()
                + (40 + System.currentTimeMillis() / 60000L % 30) * 24L * 3600000L));
        at.set(Calendar.HOUR_OF_DAY, 5);
        at.set(Calendar.MINUTE, 15);
        at.set(Calendar.SECOND, 0);
        at.set(Calendar.MILLISECOND, 0);
        final long startsAt = at.getTime().getTime();

        signIn(OWNER, "Owner");
        until(() -> findByName("revenueChart") != null, 20000, "the dashboard's charts");
        shot("owner-dashboard");
        click("menu-open");
        click("menu-timetable");
        waitForForm("Timetable");
        click("scheduleClass");
        waitForForm("ScheduleClass");
        until(() -> findByName("startsAt") instanceof Picker, 20000, "the class's time");
        onEdt(() -> ((Picker) findByName("startsAt")).setDate(new Date(startsAt)));
        type("capacity", "1");
        shot("owner-schedule");
        click("schedule");
        waitForForm("Timetable");

        String made = "";
        for (Object found : owner.list("/api/studio/classes?location=&from=" + (startsAt - 60000L)
                + "&days=1")) {
            if (found instanceof Map && ((Map<String, Object>) found).get("startsAt")
                    instanceof Number && ((Number) ((Map<String, Object>) found).get("startsAt"))
                    .longValue() == startsAt) {
                made = Actor.text((Map<String, Object>) found, "id");
            }
        }
        assertTrue(made.length() > 0, "the class is on the timetable");
        owner.post("/api/owner/classes/" + made + "/cancel", "{\"reason\":\"End of the owner test\"}");
        return true;
    }
}
