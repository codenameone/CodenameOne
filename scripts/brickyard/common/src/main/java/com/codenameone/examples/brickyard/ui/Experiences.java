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
package com.codenameone.examples.brickyard.ui;

import com.codename1.io.Preferences;
import com.codenameone.examples.brickyard.api.ExperienceDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// How the home screen arranges the experience list into rows. Pure logic,
/// so it is tested without a display.
public final class Experiences {
    static final int ROW = 10;
    private static final String RECENT = "brickyard.recent";

    private Experiences() {
    }

    /// The share of votes that are up, 0 to 100, or -1 with no votes.
    public static int rating(ExperienceDto e) {
        long total = e.upVotes() + e.downVotes();
        return total == 0 ? -1 : (int) (e.upVotes() * 100 / total);
    }

    /// Best rated first, with a few votes needed to count: an experience with
    /// one up vote does not outrank one with a thousand.
    public static List<ExperienceDto> featured(List<ExperienceDto> all) {
        List<ExperienceDto> out = new ArrayList<>(all);
        Collections.sort(out, new Comparator<ExperienceDto>() {
            @Override
            public int compare(ExperienceDto a, ExperienceDto b) {
                return Long.compare(score(b), score(a));
            }
        });
        return out.size() <= FEATURED ? out : new ArrayList<>(out.subList(0, FEATURED));
    }

    static long score(ExperienceDto e) {
        // Up votes minus down votes, plus a little for favourites.
        return e.upVotes() - e.downVotes() + e.favourites() / 4;
    }

    static final int FEATURED = 6;

    /// `list` without the experiences in `drop`, in order.
    public static List<ExperienceDto> without(List<ExperienceDto> list, List<ExperienceDto> drop) {
        List<ExperienceDto> out = new ArrayList<>();
        for (ExperienceDto e : list) {
            if (!drop.contains(e)) {
                out.add(e);
            }
        }
        return out;
    }

    /// Most visited first.
    public static List<ExperienceDto> trending(List<ExperienceDto> all) {
        List<ExperienceDto> out = new ArrayList<>(all);
        Collections.sort(out, (a, b) -> Long.compare(b.visits(), a.visits()));
        return head(out);
    }

    /// The experiences grouped by genre, genres in the order first seen.
    public static Map<String, List<ExperienceDto>> genres(List<ExperienceDto> all) {
        Map<String, List<ExperienceDto>> out = new LinkedHashMap<>();
        for (ExperienceDto e : all) {
            String g = e.genre() == null || e.genre().length() == 0 ? "Other" : e.genre();
            List<ExperienceDto> row = out.get(g);
            if (row == null) {
                row = new ArrayList<>();
                out.put(g, row);
            }
            row.add(e);
        }
        return out;
    }

    /// The experiences played recently on this device, most recent first,
    /// that are still in `all`.
    public static List<ExperienceDto> recent(List<ExperienceDto> all) {
        List<String> ids = recentIds(Preferences.get(RECENT, ""));
        List<ExperienceDto> out = new ArrayList<>();
        for (String id : ids) {
            for (ExperienceDto e : all) {
                if (id.equals(e.id())) {
                    out.add(e);
                    break;
                }
            }
        }
        return out;
    }

    public static void played(String id) {
        Preferences.set(RECENT, pushRecent(Preferences.get(RECENT, ""), id));
    }

    static String pushRecent(String stored, String id) {
        List<String> ids = recentIds(stored);
        ids.remove(id);
        ids.add(0, id);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < ids.size() && i < ROW; i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(ids.get(i));
        }
        return b.toString();
    }

    static List<String> recentIds(String stored) {
        List<String> out = new ArrayList<>();
        int start = 0;
        while (stored != null && start < stored.length()) {
            int end = stored.indexOf(',', start);
            if (end < 0) {
                end = stored.length();
            }
            if (end > start) {
                out.add(stored.substring(start, end));
            }
            start = end + 1;
        }
        return out;
    }

    private static List<ExperienceDto> head(List<ExperienceDto> list) {
        return list.size() <= ROW ? list : new ArrayList<>(list.subList(0, ROW));
    }
}
