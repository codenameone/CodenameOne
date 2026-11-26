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
package com.codenameone.examples.bazaar.web.account;

import java.util.ArrayList;
import java.util.List;

/// One store's loyalty programme as the shopper sees it: balance, tier progress,
/// referral link, the member card's QR code and the points history.
public class LoyaltyCard {
    public String storeId = "";
    public String storeName = "";
    /// Whether the shopper has joined; when not, the card offers [#joinAction].
    public boolean member;
    public String joinAction = "";
    public String balance = "0";
    public String tierName = "";
    public String nextTierName = "";
    public String toNext = "";
    /// The tier progress bar's value and maximum, as text for the attributes.
    public String progress = "0";
    public String progressMax = "1";
    public String referralCode = "";
    public String shareUrl = "";
    /// The member card's QR code: an SVG path and its view box (see [QrSvg]).
    public String qrPath = "";
    public String qrBox = "0 0 1 1";
    public List<AccountItem> history = new ArrayList<AccountItem>();
}
