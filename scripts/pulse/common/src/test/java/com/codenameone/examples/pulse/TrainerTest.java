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

import com.codename1.components.Switch;
import java.util.List;
import java.util.Map;

/// A trainer marks a member present from the class roster.
///
/// The class is made for the test, through the owner's API, to start within
/// the hour: attendance can be marked from an hour before a class, and none
/// of the demo timetable is guaranteed to be that close. The member books it the
/// same way, so all the test drives through the screens is the trainer's part.
public class TrainerTest extends E2e {
    @Override
    @SuppressWarnings("unchecked")
    protected boolean run() throws Exception {
        Actor owner = Actor.signIn(OWNER);
        Actor trainer = Actor.signIn(TRAINER);
        Actor member = Actor.signIn(MEMBER);
        String coach = Actor.text(trainer.get("/api/me"), "username");
        String sessionId = schedule(owner, coach, clearTrainersHour(owner, trainer));
        assertTrue(sessionId.length() > 0, "a class for the trainer could be scheduled");
        try {
            Map<String, Object> booking = member.post("/api/bookings",
                    "{\"sessionId\":\"" + sessionId + "\"}");
            assertEqual(200, member.status, "the member books the class");
            final String bookingId = Actor.text(booking, "id");

            onEdt(() -> AppConfig.setMode(""));
            signIn(TRAINER, "Trainer");
            click("class-" + sessionId);
            waitForForm("Roster");
            until(() -> findByName("present-" + bookingId) instanceof Switch, 20000,
                    "the member on the roster");
            shot("trainer-roster");
            onEdt(() -> ((Switch) findByName("present-" + bookingId)).setValue(true));
            until(() -> checkedIn(trainer, sessionId, bookingId), 20000,
                    "the member to be marked present");
            return true;
        } finally {
            owner.post("/api/owner/classes/" + sessionId + "/cancel",
                    "{\"reason\":\"End of the trainer test\"}");
        }
    }

    /// Makes the next hour the trainer's own and answers when a class may
    /// start in it. The demo studio keeps the trainer busy all day, and a
    /// trainer cannot be in two classes at once: the classes still to come in
    /// that hour are cancelled (this server is the test's), and one already
    /// under way can only be waited out, so the new class starts after it.
    /// Attendance is open from an hour before a class, so it starts within it.
    @SuppressWarnings("unchecked")
    private static long clearTrainersHour(Actor owner, Actor trainer) {
        long now = System.currentTimeMillis();
        long start = now + 10 * 60000L;
        for (Object found : trainer.list("/api/trainer/classes")) {
            if (!(found instanceof Map)) {
                continue;
            }
            Map<String, Object> session = (Map<String, Object>) found;
            long begins = ((Number) session.get("startsAt")).longValue();
            long ends = ((Number) session.get("endsAt")).longValue();
            if (!"SCHEDULED".equals(Actor.text(session, "state")) || ends <= now
                    || begins > now + 3 * 3600000L) {
                continue;
            }
            if (begins > now) {
                owner.post("/api/owner/classes/" + Actor.text(session, "id") + "/cancel",
                        "{\"reason\":\"Clearing the trainer's hour for the trainer test\"}");
            } else {
                start = Math.max(start, ends + 2 * 60000L);
            }
        }
        return start;
    }

    @SuppressWarnings("unchecked")
    private static boolean checkedIn(Actor trainer, String sessionId, String bookingId) {
        for (Object entry : trainer.list("/api/trainer/classes/" + sessionId + "/roster")) {
            if (entry instanceof Map && bookingId.equals(Actor.text((Map<String, Object>) entry,
                    "bookingId"))) {
                return "CHECKED_IN".equals(Actor.text((Map<String, Object>) entry, "state"));
            }
        }
        return false;
    }

    /// Schedules one class for `trainer` at `startsAt`, in the first room
    /// that is free then, and answers its id ("" when none was).
    @SuppressWarnings("unchecked")
    static String schedule(Actor owner, String trainer, long startsAt) {
        List<Object> types = owner.list("/api/studio/types");
        List<Object> rooms = owner.list("/api/owner/rooms");
        String type = "";
        for (Object found : types) {
            // A kind of class an adult can book: the demo member is one.
            if (found instanceof Map && number((Map<String, Object>) found, "minAge") <= 18
                    && (number((Map<String, Object>) found, "maxAge") == 0
                    || number((Map<String, Object>) found, "maxAge") >= 60)) {
                type = Actor.text((Map<String, Object>) found, "id");
                break;
            }
        }
        if (type.length() == 0) {
            return "";
        }
        for (Object room : rooms) {
            if (!(room instanceof Map)) {
                continue;
            }
            Map<String, Object> made = owner.post("/api/owner/classes", "{\"classTypeId\":\""
                    + type + "\",\"roomId\":\"" + Actor.text((Map<String, Object>) room, "id")
                    + "\",\"trainer\":\"" + trainer + "\",\"startsAt\":" + startsAt
                    + ",\"capacity\":" + number((Map<String, Object>) room, "capacity")
                    + ",\"weeks\":1}");
            Object list = made.get("root");
            if (owner.status == 200 && list instanceof List && !((List<Object>) list).isEmpty()
                    && ((List<Object>) list).get(0) instanceof Map) {
                return Actor.text((Map<String, Object>) ((List<Object>) list).get(0), "id");
            }
        }
        return "";
    }

    private static int number(Map<String, Object> json, String key) {
        Object value = json.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
}
