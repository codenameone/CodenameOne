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
package com.codenameone.examples.skirmish.social;

import com.codename1.backend.ResponseStatusException;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.api.ConditionDto;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.domain.Referral;
import com.codenameone.examples.skirmish.domain.StorePurchase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The player query the console's query tool and every engagement pipeline
/// share: a list of conditions such as `daysAway >= 7` and `purchases = 0`,
/// all of which a player must meet.
///
/// The fields are what the game itself knows about a player -- their
/// account, battles, purchases and invites -- never events tracked for
/// analytics, which live in Codename One analytics.
///
/// It reads every account into memory, which is right for a sample and a
/// game of a few thousand players. A larger game turns the conditions into
/// a `where` clause instead; the list of fields stays the same.
// tag::skirmish-audience[]
public final class Audience {
    /// Every field a condition may name.
    public static final String[] FIELDS = {"daysAway", "daysSinceJoin", "trophies", "battles", "wins",
        "gems", "coins", "purchases", "friends"};
    public static final String[] OPS = {">=", "<=", "=", ">", "<", "!="};
    public static final int MAX_CONDITIONS = 6;
    /// Every field is a count of something, never negative.
    public static final int MAX_VALUE = 1000000;

    private final List<ConditionDto> conditions;
    private final long now;
    private final Map<String, Integer> purchases;
    private final Map<String, Integer> friends;

    private Audience(List<ConditionDto> conditions, long now, Map<String, Integer> purchases,
            Map<String, Integer> friends) {
        this.conditions = conditions;
        this.now = now;
        this.purchases = purchases;
        this.friends = friends;
    }

    /// Checks the conditions and gathers what they need to be tested.
    public static Audience of(List<ConditionDto> conditions, List<StorePurchase> sales, List<Referral> invites) {
        check(conditions);
        Map<String, Integer> paid = new HashMap<>();
        for (StorePurchase s : sales) {
            add(paid, s.getUsername());
        }
        Map<String, Integer> brought = new HashMap<>();
        for (Referral r : invites) {
            add(brought, r.getInviter());
        }
        return new Audience(conditions, Clock.now(), paid, brought);
    }

    /// Refuses an unknown field or operator, and more than [#MAX_CONDITIONS].
    public static void check(List<ConditionDto> conditions) {
        if (conditions == null || conditions.size() > MAX_CONDITIONS) {
            throw new ResponseStatusException(400, "Up to " + MAX_CONDITIONS + " conditions");
        }
        for (ConditionDto c : conditions) {
            if (c == null || indexOf(FIELDS, c.field()) < 0 || indexOf(OPS, c.op()) < 0 || c.value() < 0
                    || c.value() > MAX_VALUE) {
                throw new ResponseStatusException(400, "Unknown condition "
                        + (c == null ? "" : c.field() + " " + c.op()));
            }
        }
    }

    /// True when the player meets every condition. Banned players never do.
    public boolean matches(Profile p) {
        if (p.isBanned()) {
            return false;
        }
        for (ConditionDto c : conditions) {
            if (!test(value(p, c.field()), c.op(), c.value())) {
                return false;
            }
        }
        return true;
    }

    /// The players who match, in the order given.
    public List<Profile> filter(List<Profile> all) {
        List<Profile> out = new ArrayList<>();
        for (Profile p : all) {
            if (matches(p)) {
                out.add(p);
            }
        }
        return out;
    }

    private int value(Profile p, String field) {
        switch (field) {
            case "daysAway":
                return days(now - p.getLastSeen().toEpochMilli());
            case "daysSinceJoin":
                return days(now - p.getCreatedAt().toEpochMilli());
            case "trophies":
                return p.getTrophies();
            case "battles":
                return p.getWins() + p.getLosses() + p.getDraws();
            case "wins":
                return p.getWins();
            case "gems":
                return p.getGems();
            case "coins":
                return p.getCoins();
            case "purchases":
                return count(purchases, p.getUsername());
            default:
                return count(friends, p.getUsername());
        }
    }

    private static boolean test(int v, String op, int against) {
        switch (op) {
            case ">=":
                return v >= against;
            case "<=":
                return v <= against;
            case "=":
                return v == against;
            case ">":
                return v > against;
            case "<":
                return v < against;
            default:
                return v != against;
        }
    }
    // end::skirmish-audience[]

    /// Conditions as one line, `daysAway >= 7; purchases = 0`, for the database.
    public static String encode(List<ConditionDto> conditions) {
        StringBuilder sb = new StringBuilder();
        for (ConditionDto c : conditions) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(c.field()).append(' ').append(c.op()).append(' ').append(c.value());
        }
        return sb.toString();
    }

    public static List<ConditionDto> decode(String line) {
        List<ConditionDto> out = new ArrayList<>();
        if (line == null) {
            return out;
        }
        // String.split is a JDK method the compiled backend does not have.
        for (String part : words(line, ';')) {
            List<String> w = words(part, ' ');
            if (w.size() == 3) {
                out.add(new ConditionDto(w.get(0), w.get(1), Integer.parseInt(w.get(2))));
            }
        }
        return out;
    }

    private static List<String> words(String s, char sep) {
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == sep) {
                String w = s.substring(start, i).trim();
                if (w.length() > 0) {
                    out.add(w);
                }
                start = i + 1;
            }
        }
        return out;
    }

    private static int days(long millis) {
        return millis <= 0 ? 0 : (int) (millis / Clock.DAY);
    }

    private static int count(Map<String, Integer> m, String key) {
        Integer n = m.get(key);
        return n == null ? 0 : n;
    }

    private static void add(Map<String, Integer> m, String key) {
        m.put(key, count(m, key) + 1);
    }

    private static int indexOf(String[] all, String s) {
        for (int i = 0; i < all.length; i++) {
            if (all[i].equals(s)) {
                return i;
            }
        }
        return -1;
    }
}
