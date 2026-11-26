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
import com.codenameone.examples.bazaar.ops.AppStoreText;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The Apps screens' wording and rules, without loading UI or translation.
class AppsLogicTest {
    private static AppListingDto listing(int tenths, int reviews) {
        return new AppListingDto("l", "a", "h", "c", "N", "T", "D", "marketing", "",
                new ArrayList<String>(), "", "", "", "https://app.example", "dev", "APPROVED",
                "", tenths, reviews, 0, 0);
    }

    private static AppChargeDto charge(String kind, long amount, String currency, int trial) {
        return new AppChargeDto("c", "a", "App", "Plan", kind, amount, currency, trial,
                "PENDING", "", "", 0, 0, 0);
    }

    private static AppInstallationDto inst(String appId, String status) {
        return new AppInstallationDto("i", appId, "App", "", "", new ArrayList<String>(), status,
                "owner", 0, 0);
    }

    @Test
    void averagesRoundHalfUpInTenths() {
        assertEquals(47, AppStoreText.tenths(14, 3));
        assertEquals(35, AppStoreText.tenths(7, 2));
        assertEquals(0, AppStoreText.tenths(0, 0));
        assertEquals("4.7", AppStoreText.rating(47, 3));
        assertEquals("", AppStoreText.rating(0, 0));
        assertEquals("4.7 (3)", AppsText.rating(listing(47, 3)));
        assertEquals("", AppsText.rating(listing(0, 0)));
        assertEquals("*****", AppStoreText.stars(46));
        assertEquals("****-", AppStoreText.stars(42));
    }

    @Test
    void chargeTermsUseTheCurrencysOwnDigits() {
        assertEquals("$9.99 every 30 days, 14 days free", AppsText.terms(
                charge(AppStoreText.RECURRING, 999, "USD", 14), "once", "every 30 days",
                "days free"));
        assertEquals("$25.00 once", AppsText.terms(charge(AppStoreText.ONE_TIME, 2500, "USD", 0),
                "once", "every 30 days", "days free"));
        assertTrue(AppsText.terms(charge(AppStoreText.ONE_TIME, 500, "JPY", 0), "once", "x", "y")
                .indexOf("500") >= 0);
        assertEquals("$9.99 every 30 days, 1 day free",
                AppStoreText.terms(AppStoreText.RECURRING, 999, "USD", 1));
    }

    @Test
    void handlesAndInstallLinks() {
        assertEquals("review-flow-b", AppStoreText.handle("Review Flow <b>&\""));
        assertEquals("app", AppStoreText.handle("!!!"));
        assertEquals("https://a.example/s?store_id=abc&client_id=12",
                AppStoreText.installUrl("https://a.example/s", "abc", "12"));
        assertEquals("https://a.example/s?x=1&store_id=abc&client_id=12",
                AppStoreText.installUrl("https://a.example/s?x=1", "a&b=c", "12"));
    }

    @Test
    void typedRatingsAndWhatIsInstalled() {
        assertEquals(5, AppsText.typedRating(" 5 "));
        assertEquals(0, AppsText.typedRating("6"));
        assertEquals(0, AppsText.typedRating("45"));
        assertEquals(0, AppsText.typedRating(null));
        List<AppInstallationDto> all = Arrays.asList(inst("a", "INSTALLED"),
                inst("b", "UNINSTALLED"));
        assertTrue(AppsText.isInstalled(all, "a"));
        assertFalse(AppsText.isInstalled(all, "b"));
        assertTrue(AppsText.hadInstalled(all, "b"));
        assertFalse(AppsText.hadInstalled(all, "c"));
        assertFalse(AppsText.hadInstalled(null, "a"));
        assertEquals("read_orders, write_products",
                AppsText.scopes(Arrays.asList("read_orders", "write_products")));
    }
}
