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
package com.codenameone.examples.bazaar.routing;

import com.codenameone.examples.bazaar.geo.Geo;

import java.util.ArrayList;
import java.util.List;

/// Decides which location sends each line of an online order. Pure.
///
/// A location that can send the whole line always comes before one that
/// cannot. Among those, the store's rules apply in order, each breaking the
/// ties the ones before it left:
///
/// - **CLOSEST**: nearer the delivery address first (no effect without a
///   point on the map).
/// - **PRIORITY**: the rule's locations first, in its order; the rest after.
/// - **MOST_STOCK**: the location with more of the line available first.
///
/// and the store's own location order breaks what is left. With no rules
/// this is exactly the first location, in the store's order, that holds the
/// units, which is how orders were placed before routing existed.
///
/// When splitting is not allowed and one location can send every line, the
/// whole order goes there (the rules ranking the locations by the whole
/// order). When none can, the order still splits: refusing it would turn a
/// preference into lost sales.
public final class Router {
    public static final String CLOSEST = "CLOSEST";
    public static final String PRIORITY = "PRIORITY";
    public static final String MOST_STOCK = "MOST_STOCK";
    /// The rule kinds, as the back office offers them.
    public static final String[] KINDS = {CLOSEST, PRIORITY, MOST_STOCK};

    private Router() {
    }

    /// A location that may fulfil online orders.
    public static final class Place {
        public final String id;
        public final double lat;
        public final double lng;

        public Place(String id, double lat, double lng) {
            this.id = id;
            this.lat = lat;
            this.lng = lng;
        }
    }

    /// One rule: its kind and, for PRIORITY, its locations in order.
    public static final class Rule {
        public final String kind;
        public final List<String> locationIds;

        public Rule(String kind, List<String> locationIds) {
            this.kind = kind;
            this.locationIds = locationIds == null ? new ArrayList<String>() : locationIds;
        }
    }

    /// One line: how many units, and what each place could send of it
    /// (`available[i]` for `places[i]`; [Integer#MAX_VALUE] when untracked).
    public static final class Need {
        public final int quantity;
        public final int[] available;

        public Need(int quantity, int[] available) {
            this.quantity = quantity;
            this.available = available;
        }
    }

    /// The decision: a location per line (null when the store has no place
    /// at all), and whether the order was split although that was not
    /// allowed.
    public static final class Plan {
        public final String[] locations;
        public final boolean forcedSplit;

        Plan(String[] locations, boolean forcedSplit) {
            this.locations = locations;
            this.forcedSplit = forcedSplit;
        }
    }

    /// Routes `needs` among `places` (in the store's location order).
    public static Plan route(List<Place> places, List<Need> needs, List<Rule> rules,
            boolean allowSplit, boolean hasPoint, double lat, double lng) {
        String[] out = new String[needs.size()];
        if (places.isEmpty()) {
            return new Plan(out, false);
        }
        if (!allowSplit && needs.size() > 1) {
            int[] total = new int[places.size()];
            boolean[] all = new boolean[places.size()];
            boolean any = false;
            for (int p = 0; p < places.size(); p++) {
                all[p] = true;
                for (int n = 0; n < needs.size(); n++) {
                    Need need = needs.get(n);
                    all[p] &= need.available[p] >= need.quantity;
                    total[p] = (int) Math.min(Integer.MAX_VALUE,
                            (long) total[p] + Math.max(0, need.available[p]));
                }
                any |= all[p];
            }
            if (any) {
                int best = best(places, all, total, rules, hasPoint, lat, lng);
                for (int n = 0; n < out.length; n++) {
                    out[n] = places.get(best).id;
                }
                return new Plan(out, false);
            }
        }
        boolean split = false;
        String first = null;
        for (int n = 0; n < needs.size(); n++) {
            Need need = needs.get(n);
            boolean[] can = new boolean[places.size()];
            for (int p = 0; p < places.size(); p++) {
                can[p] = need.available[p] >= need.quantity;
            }
            out[n] = places.get(best(places, can, need.available, rules, hasPoint, lat, lng)).id;
            if (first == null) {
                first = out[n];
            } else if (!first.equals(out[n])) {
                split = true;
            }
        }
        return new Plan(out, split && !allowSplit);
    }

    /// The index of the best place: able first, then the rules, then order.
    static int best(List<Place> places, boolean[] able, int[] stock, List<Rule> rules,
            boolean hasPoint, double lat, double lng) {
        int best = 0;
        for (int p = 1; p < places.size(); p++) {
            if (compare(places, p, best, able, stock, rules, hasPoint, lat, lng) < 0) {
                best = p;
            }
        }
        return best;
    }

    /// Below zero when place `a` is preferred to place `b`.
    private static int compare(List<Place> places, int a, int b, boolean[] able, int[] stock,
            List<Rule> rules, boolean hasPoint, double lat, double lng) {
        if (able[a] != able[b]) {
            return able[a] ? -1 : 1;
        }
        for (int iter = 0; rules != null && iter < rules.size(); iter++) {
            Rule rule = rules.get(iter);
            int c = 0;
            if (CLOSEST.equals(rule.kind) && hasPoint) {
                c = Double.compare(Geo.meters(lat, lng, places.get(a).lat, places.get(a).lng),
                        Geo.meters(lat, lng, places.get(b).lat, places.get(b).lng));
            } else if (PRIORITY.equals(rule.kind)) {
                c = Integer.compare(rank(rule.locationIds, places.get(a).id),
                        rank(rule.locationIds, places.get(b).id));
            } else if (MOST_STOCK.equals(rule.kind)) {
                c = Integer.compare(stock[b], stock[a]);
            }
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(a, b);
    }

    private static int rank(List<String> ids, String id) {
        int at = ids.indexOf(id);
        return at < 0 ? ids.size() : at;
    }
}
