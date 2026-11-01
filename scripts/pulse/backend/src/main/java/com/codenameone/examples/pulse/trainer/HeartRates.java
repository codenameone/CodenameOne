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
package com.codenameone.examples.pulse.trainer;

import com.codename1.backend.annotations.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/// The latest heart rate of each member in each class under way, as their
/// watches send it.
///
/// Kept in memory and nowhere else: a reading is worth something for a few
/// seconds, to the trainer looking at the roster, and the member's workout
/// -- what is kept -- arrives whole when it ends. A restart loses only the
/// readings since the last one each watch sent.
@Component
public class HeartRates {
    /// A reading older than this is not shown.
    static final long FRESH_MILLIS = 60000L;

    private final Map<String, int[]> latest = new HashMap<String, int[]>();
    private final Map<String, Long> at = new HashMap<String, Long>();

    /// The zone, 1 to 5, a heart rate is in for someone of `age`: the share
    /// of an estimated maximum of 220 less their age, in fifths from 50%.
    public static int zone(int bpm, int age) {
        int max = Math.max(120, 220 - age);
        int percent = bpm * 100 / max;
        if (percent < 60) {
            return 1;
        }
        if (percent < 70) {
            return 2;
        }
        if (percent < 80) {
            return 3;
        }
        return percent < 90 ? 4 : 5;
    }

    public synchronized void put(String sessionId, String memberId, int bpm, int zone,
            long now) {
        String key = sessionId + "/" + memberId;
        latest.put(key, new int[] {bpm, zone});
        at.put(key, Long.valueOf(now));
        if (latest.size() > 5000) {
            forgetStale(now);
        }
    }

    /// `{bpm, zone}`, or null when there is no fresh reading.
    public synchronized int[] get(String sessionId, String memberId, long now) {
        String key = sessionId + "/" + memberId;
        Long when = at.get(key);
        if (when == null || now - when.longValue() > FRESH_MILLIS) {
            return null;
        }
        return latest.get(key);
    }

    private void forgetStale(long now) {
        Iterator<Map.Entry<String, Long>> it = at.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue().longValue() > FRESH_MILLIS) {
                latest.remove(entry.getKey());
                it.remove();
            }
        }
    }
}
