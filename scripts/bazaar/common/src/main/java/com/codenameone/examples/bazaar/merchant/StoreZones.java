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

import com.codenameone.examples.bazaar.api.CustomDomainDto;

/// The text behind the store clock and custom domain screens: the curated
/// time zones a merchant picks from and how each is named, and how a typed
/// domain and a claim's state read. Plain strings only, so the tests need
/// no display.
final class StoreZones {
    /// The zones the picker offers, west to east within each area. The class
    /// library cannot list the zones it knows, so the list is curated; a zone
    /// saved some other way (the API) is added to it by [#choices].
    static final String[] ZONES = {
        "UTC",
        "Pacific/Honolulu", "America/Anchorage", "America/Los_Angeles", "America/Vancouver",
        "America/Denver", "America/Phoenix", "America/Chicago", "America/Mexico_City",
        "America/New_York", "America/Toronto", "America/Bogota", "America/Lima",
        "America/Halifax", "America/Santiago", "America/Sao_Paulo",
        "America/Argentina/Buenos_Aires", "Atlantic/Reykjavik", "Europe/Lisbon",
        "Europe/London", "Europe/Dublin", "Europe/Paris", "Europe/Madrid", "Europe/Berlin",
        "Europe/Rome", "Europe/Amsterdam", "Europe/Stockholm", "Europe/Warsaw",
        "Africa/Lagos", "Africa/Cairo", "Africa/Johannesburg", "Africa/Nairobi",
        "Europe/Athens", "Europe/Helsinki", "Asia/Jerusalem", "Europe/Istanbul",
        "Europe/Moscow", "Asia/Dubai", "Asia/Karachi", "Asia/Kolkata", "Asia/Dhaka",
        "Asia/Bangkok", "Asia/Jakarta", "Asia/Singapore", "Asia/Shanghai", "Asia/Hong_Kong",
        "Asia/Manila", "Australia/Perth", "Asia/Seoul", "Asia/Tokyo", "Australia/Brisbane",
        "Australia/Adelaide", "Australia/Sydney", "Australia/Melbourne", "Pacific/Auckland",
    };

    private StoreZones() {
    }

    /// [#ZONES], with `current` added at the front when the store has a zone
    /// the list does not hold, so the picker can show what is saved.
    static String[] choices(String current) {
        if (current == null || current.length() == 0 || indexOf(ZONES, current) >= 0) {
            return ZONES;
        }
        String[] out = new String[ZONES.length + 1];
        out[0] = current;
        System.arraycopy(ZONES, 0, out, 1, ZONES.length);
        return out;
    }

    /// Where `zone` is in `zones`, or -1.
    static int indexOf(String[] zones, String zone) {
        for (int iter = 0; iter < zones.length; iter++) {
            if (zones[iter].equals(zone)) {
                return iter;
            }
        }
        return -1;
    }

    /// How a zone reads in the picker: its city with spaces and its area,
    /// `New York (America)`, then the IANA name for anyone who knows it by
    /// that. `UTC` stays as it is.
    static String label(String zone) {
        if (zone == null || zone.length() == 0) {
            return "UTC";
        }
        int slash = zone.indexOf('/');
        if (slash < 0) {
            return zone;
        }
        String city = zone.substring(zone.lastIndexOf('/') + 1).replace('_', ' ');
        return city + " (" + zone.substring(0, slash) + ")" + " - " + zone;
    }

    /// The labels of `zones`, in order.
    static String[] labels(String[] zones) {
        String[] out = new String[zones.length];
        for (int iter = 0; iter < zones.length; iter++) {
            out[iter] = label(zones[iter]);
        }
        return out;
    }

    /// A domain as typed, made into the host name the server expects: no
    /// scheme, path, port or trailing dot, and lower case (ASCII only: a host
    /// name is ASCII, and a locale's case folding would bend an `I`).
    static String host(String typed) {
        if (typed == null) {
            return "";
        }
        String t = typed.trim();
        int scheme = t.indexOf("://");
        if (scheme >= 0) {
            t = t.substring(scheme + 3);
        }
        int end = t.length();
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c == '/' || c == '?' || c == '#' || c == ':') {
                end = iter;
                break;
            }
        }
        t = t.substring(0, end);
        while (t.endsWith(".")) {
            t = t.substring(0, t.length() - 1);
        }
        StringBuilder out = new StringBuilder(t.length());
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return out.toString();
    }

    /// No domain claimed.
    static final String NONE = "UNCLAIMED";
    /// Claimed, its token not found yet.
    static final String PENDING = "PENDING";
    /// Proved: the domain serves the store.
    static final String VERIFIED = "VERIFIED";

    /// The state of a claim, one of [#NONE], [#PENDING] and [#VERIFIED].
    static String state(CustomDomainDto d) {
        if (d == null || d.domain() == null || d.domain().length() == 0) {
            return NONE;
        }
        return d.verified() ? VERIFIED : PENDING;
    }

    /// The TXT record proof as one line a DNS console takes: name, type and
    /// value in quotes.
    static String txtLine(CustomDomainDto d) {
        if (d == null || d.txtName() == null || d.txtName().length() == 0) {
            return "";
        }
        return d.txtName() + " TXT \"" + (d.txtValue() == null ? "" : d.txtValue()) + "\"";
    }
}
