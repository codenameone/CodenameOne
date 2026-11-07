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
package com.codenameone.examples.slate.sync;

import com.codename1.backend.Json;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Google Calendar, read through its free/busy query: one request answers
/// every calendar the link names, and answers only times.
final class GoogleCalendar extends OAuthCalendar {
    private static final String FREE_BUSY = "https://www.googleapis.com/calendar/v3/freeBusy";

    GoogleCalendar(String clientId, String clientSecret) {
        super("https://oauth2.googleapis.com/token", clientId, clientSecret);
    }

    @Override
    public String kind() {
        return CalendarLinks.GOOGLE;
    }

    // tag::slate-google-freebusy[]
    @Override
    public List<long[]> busy(String credential, String calendars, long from, long to)
            throws IOException {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        List<String> ids = calendars(calendars);
        for (int iter = 0; iter < ids.size(); iter++) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", ids.get(iter));
            items.add(item);
        }
        Map<String, Object> ask = new HashMap<String, Object>();
        ask.put("timeMin", Instant.ofEpochMilli(from).toString());
        ask.put("timeMax", Instant.ofEpochMilli(to).toString());
        ask.put("items", items);
        Map answer = call("POST", FREE_BUSY, access(credential), Json.write(ask));
        List<long[]> out = new ArrayList<long[]>();
        Object all = answer.get("calendars");
        if (!(all instanceof Map)) {
            return out;
        }
        for (Object one : ((Map) all).values()) {
            if (!(one instanceof Map) || !(((Map) one).get("busy") instanceof List)) {
                continue;
            }
            List busy = (List) ((Map) one).get("busy");
            for (int iter = 0; iter < busy.size(); iter++) {
                if (busy.get(iter) instanceof Map) {
                    Map b = (Map) busy.get(iter);
                    long[] span = span(b.get("start"), b.get("end"));
                    if (span != null) {
                        out.add(span);
                    }
                }
            }
        }
        return out;
    }
    // end::slate-google-freebusy[]

    static long[] span(Object start, Object end) {
        if (!(start instanceof String) || !(end instanceof String)) {
            return null;
        }
        try {
            return new long[] {Instant.parse((String) start).toEpochMilli(),
                Instant.parse((String) end).toEpochMilli()};
        } catch (RuntimeException bad) {
            return null;
        }
    }
}
