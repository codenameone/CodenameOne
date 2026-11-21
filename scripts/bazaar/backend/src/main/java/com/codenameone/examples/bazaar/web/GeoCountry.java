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
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Component;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The visitor's country, for suggesting a market -- never for choosing one.
///
/// Sources, in order:
///
/// 1. A header a CDN in front of the server sets, named in
///    `bazaar.geo.trusted-headers` (comma separated; e.g.
///    `CF-IPCountry,CloudFront-Viewer-Country,X-Country-Code`). It is **empty
///    by default**, because without a proxy that overwrites it anyone can send
///    the header. `XX` and `T1` (Cloudflare's unknown and Tor) count as none.
/// 2. A [Lookup] by client address: `bazaar.geo.ranges` names a CSV of
///    `first-ip,last-ip,CC` IPv4 rows (the shape of the free country
///    databases) loaded once at start; or a lookup of your own installed with
///    [#setLookup], such as an HTTP service.
///
/// [StorefrontMarkets] uses the answer only for a banner offering the
/// matching market, and only when the shopper has neither `?market=` nor the
/// `market` cookie -- an explicit choice always wins -- and never for a
/// crawler ([#crawler]). The page's address and canonical stay the same, so
/// search engines keep one URL per market.
@Component
public class GeoCountry {
    /// Answers the country of a client address, or null; the address is null
    /// for a request that came on no connection.
    public interface Lookup {
        String country(String address);
    }

    private String[] headers;
    private Lookup lookup;

    public GeoCountry(Config config) throws IOException {
        this.headers = names(config.get("bazaar.geo.trusted-headers", ""));
        String file = config.get("bazaar.geo.ranges", "").trim();
        this.lookup = file.length() == 0 ? null : RangeTable.load(file);
    }

    /// Replaces the trusted headers (comma separated; empty for none).
    public void setTrustedHeaders(String list) {
        this.headers = names(list == null ? "" : list);
    }

    /// Replaces the address lookup (null for none).
    public void setLookup(Lookup lookup) {
        this.lookup = lookup;
    }

    /// The visitor's two-letter country, upper case, or null.
    public String country(HttpServer.Request request) {
        if (request == null) {
            return null;
        }
        for (int iter = 0; iter < headers.length; iter++) {
            String cc = cc(request.getHeader(headers[iter]));
            if (cc != null) {
                return cc;
            }
        }
        Lookup l = lookup;
        if (l == null) {
            return null;
        }
        String addr;
        try {
            addr = request.getRemoteAddress();
        } catch (RuntimeException none) {
            addr = null;
        }
        return cc(l.country(addr));
    }

    /// A normalized country code, or null for none, unknown or Tor.
    static String cc(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        if (t.length() != 2) {
            return null;
        }
        char a = up(t.charAt(0));
        char b = up(t.charAt(1));
        if (a < 'A' || a > 'Z' || b < 'A' || b > 'Z') {
            return null;
        }
        String code = "" + a + b;
        return "XX".equals(code) || "T1".equals(code) ? null : code;
    }

    private static char up(char c) {
        return c >= 'a' && c <= 'z' ? (char) (c - 32) : c;
    }

    /// Whether the user agent is a search engine or other crawler, which gets
    /// no suggestion so every crawler sees the same page.
    public static boolean crawler(String agent) {
        if (agent == null || agent.length() == 0) {
            return true;
        }
        String[] marks = {"bot", "crawl", "spider", "slurp", "facebookexternalhit",
            "preview", "curl", "wget"};
        for (int iter = 0; iter < marks.length; iter++) {
            String m = marks[iter];
            for (int at = 0; at + m.length() <= agent.length(); at++) {
                if (agent.regionMatches(true, at, m, 0, m.length())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String[] names(String list) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int iter = 0; iter <= list.length(); iter++) {
            if (iter == list.length() || list.charAt(iter) == ',') {
                String n = list.substring(start, iter).trim();
                if (n.length() > 0) {
                    out.add(n);
                }
                start = iter + 1;
            }
        }
        return out.toArray(new String[0]);
    }

    /// IPv4 ranges sorted by their first address, searched by halving.
    public static final class RangeTable implements Lookup {
        private final long[] first;
        private final long[] last;
        private final String[] code;

        public RangeTable(List<String> lines) {
            List<long[]> spans = new ArrayList<long[]>();
            List<String> codes = new ArrayList<String>();
            for (int iter = 0; iter < lines.size(); iter++) {
                String[] f = fields(lines.get(iter));
                if (f == null) {
                    continue;
                }
                long a = ip(f[0]);
                long b = ip(f[1]);
                String c = cc(f[2]);
                if (a < 0 || b < a || c == null) {
                    continue;
                }
                spans.add(new long[] {a, b});
                codes.add(c);
            }
            int n = spans.size();
            Integer[] order = new Integer[n];
            for (int iter = 0; iter < n; iter++) {
                order[iter] = Integer.valueOf(iter);
            }
            final List<long[]> s = spans;
            java.util.Arrays.sort(order, (x, y) -> Long.compare(s.get(x.intValue())[0],
                    s.get(y.intValue())[0]));
            first = new long[n];
            last = new long[n];
            code = new String[n];
            for (int iter = 0; iter < n; iter++) {
                int k = order[iter].intValue();
                first[iter] = spans.get(k)[0];
                last[iter] = spans.get(k)[1];
                code[iter] = codes.get(k);
            }
        }

        static RangeTable load(String file) throws IOException {
            List<String> lines = new ArrayList<String>();
            try (BufferedReader r = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = r.readLine()) != null) {
                    lines.add(line);
                }
            }
            return new RangeTable(lines);
        }

        /// The number of ranges loaded.
        public int size() {
            return first.length;
        }

        @Override
        public String country(String address) {
            long v = ip(address);
            if (v < 0) {
                return null;
            }
            int lo = 0;
            int hi = first.length - 1;
            int found = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (first[mid] <= v) {
                    found = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            return found >= 0 && v <= last[found] ? code[found] : null;
        }

        private static String[] fields(String line) {
            String[] out = new String[3];
            int n = 0;
            int start = 0;
            for (int iter = 0; iter <= line.length() && n < 3; iter++) {
                if (iter == line.length() || line.charAt(iter) == ',') {
                    String f = line.substring(start, iter).trim();
                    if (f.length() > 1 && f.charAt(0) == '"' && f.charAt(f.length() - 1) == '"') {
                        f = f.substring(1, f.length() - 1);
                    }
                    out[n++] = f;
                    start = iter + 1;
                }
            }
            return n == 3 ? out : null;
        }

        /// A dotted IPv4 address as a number, or -1.
        static long ip(String s) {
            if (s == null) {
                return -1;
            }
            long v = 0;
            int part = 0;
            int digits = 0;
            int dots = 0;
            for (int iter = 0; iter < s.length(); iter++) {
                char c = s.charAt(iter);
                if (c >= '0' && c <= '9') {
                    part = part * 10 + (c - '0');
                    if (++digits > 3 || part > 255) {
                        return -1;
                    }
                } else if (c == '.' && digits > 0 && dots < 3) {
                    v = v * 256 + part;
                    part = 0;
                    digits = 0;
                    dots++;
                } else {
                    return -1;
                }
            }
            return dots == 3 && digits > 0 ? v * 256 + part : -1;
        }
    }
}
