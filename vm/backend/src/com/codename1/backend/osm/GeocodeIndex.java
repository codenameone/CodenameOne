/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend.osm;

import java.util.ArrayList;
import java.util.List;

/// The places, addresses and named streets of an extract, searchable by
/// name and by position.
final class GeocodeIndex {
    private static final String[] POI_KEYS = {
        "place", "amenity", "shop", "tourism", "leisure", "historic", "office", "railway",
        "public_transport", "aeroway", "building"
    };
    private static final int MAX_CANDIDATES = 50;
    private static final int IN_NAME = 1000;

    /// One searchable place. Mutable only while the extract loads.
    static final class Entry {
        char osmType;
        long osmId;
        String name;
        String cls;
        String type;
        String houseNumber;
        String street;
        String city;
        String postcode;
        String display;
        String[] tokens = new String[0];
        // The leading tokens that come from the place's own names.
        int nameWords;
        int importance;
        int lat;
        int lon;
    }

    private final Entry[] entries;
    private final SpatialGrid grid;

    GeocodeIndex(List list) {
        entries = (Entry[]) list.toArray(new Entry[list.size()]);
        int[] lat = new int[entries.length];
        int[] lon = new int[entries.length];
        int[] ids = new int[entries.length];
        for (int i = 0; i < entries.length; i++) {
            lat[i] = entries[i].lat;
            lon[i] = entries[i].lon;
            ids[i] = i;
        }
        grid = new SpatialGrid(lat, lon, ids);
    }

    int size() {
        return entries.length;
    }

    static boolean isStreet(String highway) {
        return !"steps".equals(highway) && !"corridor".equals(highway)
                && !"proposed".equals(highway) && !"construction".equals(highway)
                && !"platform".equals(highway) && !"bus_stop".equals(highway);
    }

    static Entry street(long wayId, String name, String highway) {
        Entry e = new Entry();
        e.osmType = 'w';
        e.osmId = wayId;
        e.name = name;
        e.cls = "highway";
        e.type = highway;
        e.street = name;
        e.importance = 30;
        e.display = name;
        e.tokens = tokens(name);
        e.nameWords = e.tokens.length;
        return e;
    }

    /// The place a tagged node or way describes, or null when it names none.
    static Entry fromTags(boolean node, long id, String[] tags, int count) {
        String name = OsmExtract.tag(tags, count, "name");
        String number = OsmExtract.tag(tags, count, "addr:housenumber");
        String street = OsmExtract.tag(tags, count, "addr:street");
        if (street == null) {
            street = OsmExtract.tag(tags, count, "addr:place");
        }
        String cls = null;
        String type = null;
        for (String key : POI_KEYS) {
            String v = OsmExtract.tag(tags, count, key);
            if (v != null && !"no".equals(v)) {
                cls = key;
                type = v;
                break;
            }
        }
        boolean address = number != null && street != null;
        if (name == null && !address || name != null && cls == null && !address) {
            return null;
        }
        if (OsmExtract.tag(tags, count, "highway") != null) {
            return null;
        }
        Entry e = new Entry();
        e.osmType = node ? 'n' : 'w';
        e.osmId = id;
        e.name = name;
        e.houseNumber = number;
        e.street = street;
        e.city = OsmExtract.tag(tags, count, "addr:city");
        e.postcode = OsmExtract.tag(tags, count, "addr:postcode");
        if (name == null || cls == null) {
            e.cls = "place";
            e.type = "house";
        } else {
            e.cls = cls;
            e.type = type;
        }
        e.importance = importance(e.cls, e.type, name != null && cls != null);
        StringBuilder display = new StringBuilder();
        if (name != null) {
            display.append(name);
        }
        if (address) {
            if (display.length() > 0) {
                display.append(", ");
            }
            display.append(number).append(' ').append(street);
        }
        if (e.city != null) {
            display.append(", ").append(e.city);
        }
        e.display = display.toString();
        StringBuilder words = new StringBuilder();
        append(words, name);
        append(words, OsmExtract.tag(tags, count, "name:en"));
        append(words, OsmExtract.tag(tags, count, "alt_name"));
        append(words, OsmExtract.tag(tags, count, "short_name"));
        int nameWords = tokens(words.toString()).length;
        append(words, number);
        append(words, street);
        append(words, e.city);
        append(words, e.postcode);
        e.tokens = tokens(words.toString());
        e.nameWords = nameWords;
        return e;
    }

    private static void append(StringBuilder sb, String s) {
        if (s != null) {
            sb.append(' ').append(s);
        }
    }

    private static int importance(String cls, String type, boolean named) {
        if ("place".equals(cls) && named) {
            if ("city".equals(type)) {
                return 90;
            }
            if ("town".equals(type)) {
                return 80;
            }
            if ("village".equals(type) || "suburb".equals(type)) {
                return 70;
            }
            return 60;
        }
        if ("aeroway".equals(cls) || "railway".equals(cls) || "public_transport".equals(cls)) {
            return 55;
        }
        if ("tourism".equals(cls) || "historic".equals(cls)) {
            return 45;
        }
        return named ? 40 : 20;
    }

    private static final int MAX_QUERY_WORDS = 8;

    /// Up to limit entries matching every word of the query, best first. A
    /// word matches a word of the place that starts with it, after both are
    /// folded to lower case without accents.
    List search(String query, int limit) {
        String[] q = tokens(query);
        List out = new ArrayList();
        if (q.length == 0) {
            return out;
        }
        // A scan of every entry per word: bound the words, so a query of a
        // hundred one-letter words cannot multiply the work.
        if (q.length > MAX_QUERY_WORDS) {
            String[] fewer = new String[MAX_QUERY_WORDS];
            System.arraycopy(q, 0, fewer, 0, MAX_QUERY_WORDS);
            q = fewer;
        }
        int scanned = 0;
        int keep = Math.max(1, Math.min(limit, MAX_CANDIDATES));
        Entry[] best = new Entry[keep];
        int[] bestScore = new int[keep];
        int found = 0;
        for (Entry e : entries) {
            // Generated code has no safepoint polls; a scan of a country must
            // let the collector in.
            scanned++;
            if (scanned % 65536 == 0) {
                Thread.yield();
            }
            int bonus = 0;
            boolean all = true;
            boolean allInName = true;
            for (String word : q) {
                int m = match(e.tokens, e.nameWords, word);
                if (m == 0) {
                    all = false;
                    break;
                }
                allInName &= m > IN_NAME;
                bonus += m % IN_NAME;
            }
            if (!all) {
                continue;
            }
            // A query found entirely in the place's own name outweighs any
            // importance: "boulevard albert" is the boulevard before the hotels
            // whose address is on it.
            int score = (allInName ? 10000 : 0) + e.importance * 64 + bonus - Math.min(15, Math.max(0, e.tokens.length - q.length));
            int at = found < keep ? found++ : keep;
            if (at == keep) {
                if (score <= bestScore[keep - 1]) {
                    continue;
                }
                at = keep - 1;
            }
            while (at > 0 && bestScore[at - 1] < score) {
                best[at] = best[at - 1];
                bestScore[at] = bestScore[at - 1];
                at--;
            }
            best[at] = e;
            bestScore[at] = score;
        }
        for (int i = 0; i < found; i++) {
            out.add(best[i]);
        }
        return out;
    }

    // 0 when no token starts with the word; otherwise a bonus that is larger
    // for an exact word than a prefix, and larger in the name than the address.
    private static int match(String[] tokens, int nameWords, String word) {
        int result = 0;
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            int score = 0;
            if (t.equals(word)) {
                score = 32;
            } else if (t.startsWith(word)) {
                score = 16;
            }
            if (score > 0 && i < nameWords) {
                score += IN_NAME;
            }
            result = Math.max(result, score);
        }
        return result;
    }

    /// The place nearest to a position: an address or a named place within
    /// 75 meters, otherwise the nearest street within a kilometre, or null.
    Entry reverse(int latE7, int lonE7) {
        int at = grid.nearest(latE7, lonE7, 75, new SpatialGrid.Filter() {
            @Override
            public boolean accept(int id) {
                return !"highway".equals(entries[id].cls);
            }
        }, null);
        if (at < 0) {
            at = grid.nearest(latE7, lonE7, 1000, null, null);
        }
        return at < 0 ? null : entries[at];
    }

    /// Lower case, Latin accents removed, split on everything that is not a
    /// letter or a digit.
    static String[] tokens(String s) {
        List words = new ArrayList();
        if (s == null) {
            return new String[0];
        }
        StringBuilder w = new StringBuilder();
        for (int i = 0; i <= s.length(); i++) {
            char c = i < s.length() ? s.charAt(i) : ' ';
            if (Character.isLetterOrDigit(c)) {
                w.append(fold(c));
            } else if (w.length() > 0) {
                words.add(w.toString());
                w.setLength(0);
            }
        }
        return (String[]) words.toArray(new String[words.size()]);
    }

    private static char fold(char c) {
        if (c < 0xc0 || c > 0xff) {
            return Character.toLowerCase(c);
        }
        if (c <= 0xc5 || c >= 0xe0 && c <= 0xe5) {
            return 'a';
        }
        if (c == 0xc7 || c == 0xe7) {
            return 'c';
        }
        if (c >= 0xc8 && c <= 0xcb || c >= 0xe8 && c <= 0xeb) {
            return 'e';
        }
        if (c >= 0xcc && c <= 0xcf || c >= 0xec && c <= 0xef) {
            return 'i';
        }
        if (c == 0xd1 || c == 0xf1) {
            return 'n';
        }
        if (c >= 0xd2 && c <= 0xd6 || c == 0xd8 || c >= 0xf2 && c <= 0xf6 || c == 0xf8) {
            return 'o';
        }
        if (c >= 0xd9 && c <= 0xdc || c >= 0xf9 && c <= 0xfc) {
            return 'u';
        }
        if (c == 0xdd || c == 0xfd || c == 0xff) {
            return 'y';
        }
        return Character.toLowerCase(c);
    }
}
