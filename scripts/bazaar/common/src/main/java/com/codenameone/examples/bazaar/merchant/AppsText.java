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

import com.codenameone.examples.bazaar.api.AppChargeDto;
import com.codenameone.examples.bazaar.api.AppInstallationDto;
import com.codenameone.examples.bazaar.api.AppListingDto;
import com.codenameone.examples.bazaar.ops.Amounts;
import com.codenameone.examples.bazaar.ops.AppStoreText;

import java.util.List;

/// What the Apps screens show, worked out apart from them: a rating, a
/// charge's terms, a typed star count and what the store has installed.
/// No UI and no translation, so the tests run it as it is; the screen hands
/// in the words it translated.
final class AppsText {
    private AppsText() {
    }

    /// `4.7 (12)`, or empty before any review.
    static String rating(AppListingDto l) {
        if (l.reviews() <= 0) {
            return "";
        }
        return AppStoreText.rating(l.ratingTenths(), l.reviews()) + " (" + l.reviews() + ")";
    }

    /// A charge's terms in the screen's words: `$9.99 every 30 days, 14 days free`.
    static String terms(AppChargeDto c, String once, String every, String daysFree) {
        String money = Amounts.format(c.amount(), c.currency());
        if (!AppStoreText.RECURRING.equals(c.kind())) {
            return money + " " + once;
        }
        String out = money + " " + every;
        return c.trialDays() > 0 ? out + ", " + c.trialDays() + " " + daysFree : out;
    }

    /// The rating typed in, or 0 when it is not a whole number from 1 to 5.
    static int typedRating(String typed) {
        String t = typed == null ? "" : typed.trim();
        if (t.length() != 1) {
            return 0;
        }
        char c = t.charAt(0);
        return c >= '1' && c <= '5' ? c - '0' : 0;
    }

    /// The scopes an app was granted, as one line.
    static String scopes(List<String> scopes) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; scopes != null && iter < scopes.size(); iter++) {
            b.append(iter == 0 ? "" : ", ").append(scopes.get(iter));
        }
        return b.toString();
    }

    /// Whether the app is installed in the store now.
    static boolean isInstalled(List<AppInstallationDto> installed, String appId) {
        for (int iter = 0; installed != null && iter < installed.size(); iter++) {
            AppInstallationDto i = installed.get(iter);
            if (i.appId().equals(appId) && "INSTALLED".equals(i.status())) {
                return true;
            }
        }
        return false;
    }

    /// Whether the store has ever installed the app, which is what lets it review it.
    static boolean hadInstalled(List<AppInstallationDto> installed, String appId) {
        for (int iter = 0; installed != null && iter < installed.size(); iter++) {
            if (installed.get(iter).appId().equals(appId)) {
                return true;
            }
        }
        return false;
    }
}
