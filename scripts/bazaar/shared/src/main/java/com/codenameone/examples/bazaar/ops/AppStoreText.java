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
package com.codenameone.examples.bazaar.ops;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// The app directory's small rules and wording, shared by the server, its
/// pages and the client: the categories, a listing's handle, an average
/// rating as text and a charge's terms in one line. Nothing here touches UI
/// or translation, so the client's tests run it without either.
public final class AppStoreText {
    /// The directory's categories.
    public static final List<String> CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            "marketing", "sales", "shipping", "inventory", "customer-service", "analytics",
            "finance", "productivity"));

    /// A charge billed once.
    public static final String ONE_TIME = "ONE_TIME";
    /// A charge billed every [#PERIOD_DAYS] days.
    public static final String RECURRING = "RECURRING";
    /// The length of a recurring charge's period.
    public static final int PERIOD_DAYS = 30;
    /// The longest trial an app may offer.
    public static final int MAX_TRIAL_DAYS = 90;

    private AppStoreText() {
    }

    /// A handle made from a name: lowercase ASCII letters and digits, runs of
    /// anything else as one `-`, no `-` at either end, at most 60 characters.
    public static String handle(String name) {
        StringBuilder b = new StringBuilder();
        boolean dash = false;
        for (int iter = 0; name != null && iter < name.length() && b.length() < 60; iter++) {
            char c = name.charAt(iter);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + ('a' - 'A'));
            }
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                if (dash && b.length() > 0) {
                    b.append('-');
                }
                b.append(c);
                dash = false;
            } else {
                dash = true;
            }
        }
        return b.length() == 0 ? "app" : b.toString();
    }

    /// The average of `count` ratings summing to `sum`, times ten and rounded
    /// half up: 4, 5 and 5 make 47.
    public static int tenths(long sum, long count) {
        if (count <= 0) {
            return 0;
        }
        return (int) ((sum * 20 + count) / (count * 2));
    }

    /// An average in tenths as a person reads it: `4.7`, or empty for none.
    public static String rating(int tenths, int count) {
        if (count <= 0) {
            return "";
        }
        return (tenths / 10) + "." + (tenths % 10);
    }

    /// Five stars, filled up to the nearest whole star: `****-` for 4.2.
    public static String stars(int tenths) {
        int full = (tenths + 5) / 10;
        StringBuilder b = new StringBuilder(5);
        for (int iter = 0; iter < 5; iter++) {
            b.append(iter < full ? '*' : '-');
        }
        return b.toString();
    }

    /// A charge's terms: `$9.99 every 30 days, 14 days free` or `$25.00 once`.
    public static String terms(String kind, long amount, String currency, int trialDays) {
        String money = Amounts.format(amount, currency);
        if (!RECURRING.equals(kind)) {
            return money + " once";
        }
        String every = money + " every " + PERIOD_DAYS + " days";
        return trialDays > 0 ? every + ", " + trialDays + (trialDays == 1 ? " day" : " days")
                + " free" : every;
    }

    /// Where installing from the directory starts, for one store: the app's
    /// own address with `store_id` and `client_id` added, from which the app
    /// sends the owner to the consent page with its PKCE challenge. The
    /// platform cannot start the consent itself: only the app holds the
    /// PKCE verifier. Ids are letters and digits, so they need no escaping;
    /// anything else in them is dropped.
    public static String installUrl(String appUrl, String storeId, String clientId) {
        StringBuilder b = new StringBuilder(appUrl == null ? "" : appUrl);
        b.append(b.toString().indexOf('?') < 0 ? '?' : '&').append("store_id=");
        safe(b, storeId);
        b.append("&client_id=");
        safe(b, clientId);
        return b.toString();
    }

    private static void safe(StringBuilder b, String id) {
        for (int iter = 0; id != null && iter < id.length(); iter++) {
            char c = id.charAt(iter);
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '-' || c == '_') {
                b.append(c);
            }
        }
    }

    /// When a recurring charge approved at `approvedAt` first bills.
    public static long billingStarts(long approvedAt, int trialDays) {
        return approvedAt + trialDays * 24L * 60 * 60 * 1000;
    }
}
