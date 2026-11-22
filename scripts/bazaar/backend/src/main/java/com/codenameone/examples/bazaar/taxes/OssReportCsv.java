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
package com.codenameone.examples.bazaar.taxes;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;

/// The One-Stop Shop report as a CSV download, for the staff API; the web
/// admin's tax page links its own copy. `from` and `to` are epoch
/// milliseconds or `YYYY-MM-DD` (UTC), `to` exclusive.
@RestController
public class OssReportCsv {
    private final TaxRules rules;

    public OssReportCsv(TaxRules rules) {
        this.rules = rules;
    }

    @GetMapping("/api/merchant/stores/{storeId}/oss-report.csv")
    public HttpServer.Response csv(@PathVariable("storeId") String storeId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) throws Exception {
        return response(TaxRules.ossCsv(rules.ossReport(Caller.name(), storeId, millis(from, 0),
                millis(to, Long.MAX_VALUE))));
    }

    /// A CSV download named for the report.
    public static HttpServer.Response response(String csv) throws Exception {
        return new HttpServer.Response(200, "text/csv; charset=utf-8", csv.getBytes("UTF-8"))
                .header("Cache-Control", "no-store")
                .header("Content-Disposition", "attachment; filename=\"oss-report.csv\"");
    }

    /// `value` as epoch milliseconds: digits as they are, `YYYY-MM-DD` as
    /// that day's UTC midnight; `fallback` for anything else.
    public static long millis(String value, long fallback) {
        String v = value == null ? "" : value.trim();
        if (v.length() == 10 && v.charAt(4) == '-' && v.charAt(7) == '-') {
            int y = digits(v, 0, 4);
            int m = digits(v, 5, 7);
            int d = digits(v, 8, 10);
            if (y > 1969 && m >= 1 && m <= 12 && d >= 1 && d <= 31) {
                return java.time.LocalDate.of(y, m, Math.min(d,
                        java.time.YearMonth.of(y, m).lengthOfMonth())).toEpochDay() * 86400000L;
            }
            return fallback;
        }
        if (v.length() == 0 || v.length() > 15) {
            return fallback;
        }
        return longDigits(v, fallback);
    }

    private static int digits(String s, int from, int to) {
        int n = 0;
        for (int iter = from; iter < to; iter++) {
            char c = s.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            n = n * 10 + (c - '0');
            if (n > 100000000) {
                return -1;
            }
        }
        return n;
    }

    private static long longDigits(String s, long fallback) {
        long n = 0;
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c < '0' || c > '9') {
                return fallback;
            }
            n = n * 10 + (c - '0');
        }
        return n;
    }
}
