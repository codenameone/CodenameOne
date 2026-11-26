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
package com.codenameone.examples.bazaar.merchant;

import com.codenameone.examples.bazaar.api.RoutingRuleDto;
import com.codenameone.examples.bazaar.api.ShippingProfileDto;
import com.codenameone.examples.bazaar.api.ShippingZoneDto;
import java.util.ArrayList;
import java.util.List;

/// The shipping and routing editors' arithmetic, kept apart from the UI so
/// that it is tested without a display: which zones belong to a profile,
/// which profile holds a product, and the comma separated location lists of
/// routing rules (parsed by hand: no regular expressions on the device).
final class ShippingSetup {
    /// Routing rule kinds, in the order the editor offers them.
    static final String[] RULE_KINDS = {"CLOSEST", "PRIORITY", "MOST_STOCK"};
    /// Fulfilment services the editor offers; empty for none.
    static final String[] SERVICES = {"", "SIMULATED"};

    private ShippingSetup() {
    }

    /// The zones of `profileId`, empty or null being the general profile.
    static List<ShippingZoneDto> zonesOf(List<ShippingZoneDto> zones, String profileId) {
        String want = profileId == null ? "" : profileId;
        List<ShippingZoneDto> out = new ArrayList<ShippingZoneDto>();
        for (int iter = 0; zones != null && iter < zones.size(); iter++) {
            String has = zones.get(iter).profileId();
            if (want.equals(has == null ? "" : has)) {
                out.add(zones.get(iter));
            }
        }
        return out;
    }

    /// The profile holding `productId`, or null when it ships under the
    /// general profile.
    static ShippingProfileDto profileOf(List<ShippingProfileDto> profiles, String productId) {
        for (int iter = 0; profiles != null && iter < profiles.size(); iter++) {
            List<String> ids = profiles.get(iter).productIds();
            if (ids != null && ids.contains(productId)) {
                return profiles.get(iter);
            }
        }
        return null;
    }

    /// The ids of a comma separated list, trimmed, blanks dropped.
    static List<String> ids(String list) {
        List<String> out = new ArrayList<String>();
        if (list == null) {
            return out;
        }
        int start = 0;
        while (start <= list.length()) {
            int comma = list.indexOf(',', start);
            int end = comma < 0 ? list.length() : comma;
            String one = list.substring(start, end).trim();
            if (one.length() > 0 && !out.contains(one)) {
                out.add(one);
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        return out;
    }

    /// `ids` as the comma separated list a rule stores.
    static String join(List<String> ids) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; ids != null && iter < ids.size(); iter++) {
            if (b.length() > 0) {
                b.append(',');
            }
            b.append(ids.get(iter));
        }
        return b.toString();
    }

    /// The `ticked` ids with those already in `before` first, in its order,
    /// and the newly ticked after them.
    static List<String> keepOrder(List<String> before, List<String> ticked) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; before != null && iter < before.size(); iter++) {
            if (ticked.contains(before.get(iter))) {
                out.add(before.get(iter));
            }
        }
        for (int iter = 0; iter < ticked.size(); iter++) {
            if (!out.contains(ticked.get(iter))) {
                out.add(ticked.get(iter));
            }
        }
        return out;
    }

    /// The position a new rule takes: after every rule there is.
    static int nextPosition(List<RoutingRuleDto> rules) {
        int max = -1;
        for (int iter = 0; rules != null && iter < rules.size(); iter++) {
            max = Math.max(max, rules.get(iter).position());
        }
        return max + 1;
    }
}
