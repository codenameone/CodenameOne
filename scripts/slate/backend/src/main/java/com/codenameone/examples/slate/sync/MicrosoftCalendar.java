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

/// Outlook and Microsoft 365, read through Graph's `getSchedule`, which
/// answers busy times and, without a broader scope, nothing else.
final class MicrosoftCalendar extends OAuthCalendar {
    private static final String SCHEDULE =
            "https://graph.microsoft.com/v1.0/me/calendar/getSchedule";
    private static final String ME = "https://graph.microsoft.com/v1.0/me";

    MicrosoftCalendar(String clientId, String clientSecret) {
        super("https://login.microsoftonline.com/common/oauth2/v2.0/token", clientId,
                clientSecret);
    }

    @Override
    public String kind() {
        return CalendarLinks.MICROSOFT;
    }

    @Override
    public List<long[]> busy(String credential, String calendars, long from, long to)
            throws IOException {
        String token = access(credential);
        Map me = call("GET", ME, token, null);
        Object address = me.get("mail");
        if (!(address instanceof String)) {
            address = me.get("userPrincipalName");
        }
        if (!(address instanceof String)) {
            throw new IOException("The account has no mailbox to read a schedule from");
        }
        List<String> who = new ArrayList<String>();
        who.add((String) address);
        Map<String, Object> ask = new HashMap<String, Object>();
        ask.put("schedules", who);
        ask.put("startTime", time(from));
        ask.put("endTime", time(to));
        ask.put("availabilityViewInterval", Integer.valueOf(15));
        Map answer = call("POST", SCHEDULE, token, Json.write(ask));
        List<long[]> out = new ArrayList<long[]>();
        if (!(answer.get("value") instanceof List)) {
            return out;
        }
        List value = (List) answer.get("value");
        for (int iter = 0; iter < value.size(); iter++) {
            if (!(value.get(iter) instanceof Map)
                    || !(((Map) value.get(iter)).get("scheduleItems") instanceof List)) {
                continue;
            }
            List items = (List) ((Map) value.get(iter)).get("scheduleItems");
            for (int i = 0; i < items.size(); i++) {
                if (!(items.get(i) instanceof Map)) {
                    continue;
                }
                Map item = (Map) items.get(i);
                // Free and working-elsewhere entries are not busy.
                Object status = item.get("status");
                if ("free".equals(status) || "workingElsewhere".equals(status)) {
                    continue;
                }
                long[] span = span(item.get("start"), item.get("end"));
                if (span != null) {
                    out.add(span);
                }
            }
        }
        return out;
    }

    private static Map<String, Object> time(long millis) {
        Map<String, Object> out = new HashMap<String, Object>();
        String text = Instant.ofEpochMilli(millis).toString();
        out.put("dateTime", text.endsWith("Z") ? text.substring(0, text.length() - 1) : text);
        out.put("timeZone", "UTC");
        return out;
    }

    /// Graph's `{dateTime, timeZone}` pairs, which the `Prefer` header asked
    /// for in UTC.
    private static long[] span(Object start, Object end) {
        if (!(start instanceof Map) || !(end instanceof Map)) {
            return null;
        }
        Object s = ((Map) start).get("dateTime");
        Object e = ((Map) end).get("dateTime");
        if (!(s instanceof String) || !(e instanceof String)) {
            return null;
        }
        return GoogleCalendar.span(utc((String) s), utc((String) e));
    }

    private static String utc(String local) {
        String text = local;
        int dot = text.indexOf('.');
        if (dot > 0) {
            text = text.substring(0, dot);
        }
        return text.endsWith("Z") ? text : text + "Z";
    }
}
