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

/// The model of every account page (`templates/account.html`). One template
/// draws them all, choosing its section by [#page]; each page fills the fields
/// it shows.
public class AccountView {
    /// "home", "orders", "order", "addresses", "wishlist", "reviews", "loyalty",
    /// "clubs", "alerts", "marketing" or "security".
    public String page = "home";
    /// The page's main list.
    public List<AccountItem> items = new ArrayList<AccountItem>();
    /// Paging links, empty at either end.
    public String prevUrl = "";
    public String nextUrl = "";
    public String pageLabel = "";

    // --------------------------------------------------------- order detail
    public AccountItem order = new AccountItem();
    public List<AccountItem> lines = new ArrayList<AccountItem>();
    /// The order's downloads and licence keys.
    public List<com.codenameone.examples.bazaar.web.DownloadView> downloads =
            new ArrayList<com.codenameone.examples.bazaar.web.DownloadView>();
    public List<AccountItem> events = new ArrayList<AccountItem>();
    public List<AccountItem> tracking = new ArrayList<AccountItem>();
    public List<AccountItem> returns = new ArrayList<AccountItem>();
    public String shipTo = "";
    public String invoiceUrl = "";
    public String cancelAction = "";
    public String returnAction = "";
    public boolean canCancel;
    public boolean canReturn;
    // ---------------------------------------------------- self-serve returns
    /// The store's return rules as shown: the window, the restocking fee and
    /// the return shipping fee ("" when the store charges none).
    public String returnWindow = "";
    public String restockingFee = "";
    public String returnShipping = "";
    /// Why nothing can be sent back now, "" when returns are open.
    public String returnsClosed = "";
    /// The variants an exchange may swap to: id, title and price.
    public List<AccountItem> exchanges = new ArrayList<AccountItem>();
    public String quoteAction = "";
    /// The form as posted, kept when the page shows a quote.
    public String chosenReason = "";
    public String chosenNote = "";
    public String chosenResolution = "REFUND";
    public String chosenExchange = "";
    /// What the return would come to, once previewed.
    public boolean quoted;
    public String quoteValue = "";
    public String quoteFee = "";
    public String quoteExchange = "";
    public String quoteDifference = "";
    /// "back" (paid back or credited), "owed" (the shopper pays the
    /// difference) or "even".
    public String quoteDirection = "";
    public String quoteError = "";

    // ------------------------------------------------------------ addresses
    public List<AddressRow> addresses = new ArrayList<AddressRow>();
    public String addAction = "/account/addresses";

    // -------------------------------------------------------------- loyalty
    public List<LoyaltyCard> cards = new ArrayList<LoyaltyCard>();

    // ------------------------------------------------------------- security
    public boolean totpEnabled;
    /// An enrolment waiting for its first code: the secret and its QR code.
    public boolean enrolling;
    public String secret = "";
    public String qrPath = "";
    public String qrBox = "0 0 1 1";
    /// Recovery codes, filled only on the response that made them.
    public List<String> recoveryCodes = new ArrayList<String>();
    public String recoveryLeft = "0";
    public List<AccountItem> sessions = new ArrayList<AccountItem>();
}
