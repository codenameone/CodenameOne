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
package com.codenameone.examples.bazaar.web.commerce;

import java.util.ArrayList;
import java.util.List;

/// What `templates/marketing.html` draws for the shopper's subscription and
/// company pages ([CommerceAccountPages]): some [FormView]s, a few facts, a
/// list of cards with their own buttons and a table of rows.
public class MarketingView {
    /// "subscriptions", "subscribe", "companies", "company", "my-quotes",
    /// "my-quote" or "my-invoices".
    public String page = "";
    public String base = "";
    public String heading = "";
    public String intro = "";
    /// The list page this one belongs to.
    public String back = "";
    public String backLabel = "";
    /// A search form above the cards (`archived`): where it submits, the
    /// words searched and the "placed within" choices, `flag` on the chosen.
    public String searchAction = "";
    public String q = "";
    public List<AdminRow> sinceOptions = new ArrayList<AdminRow>();
    /// The live inbox (`app.js` with `inbox-live`): the store whose chat
    /// socket the page joins, the conversation it shows (empty on the
    /// list), the address of its socket ticket, and the thread itself.
    public String liveStore = "";
    public String liveChat = "";
    public String liveTicket = "";
    public List<AdminRow> thread = new ArrayList<AdminRow>();
    public boolean hasTable;
    /// Buttons at the top: `url` and `title`.
    public List<AdminRow> links = new ArrayList<AdminRow>();
    public List<FormView> forms = new ArrayList<FormView>();
    /// Label and value pairs: `title` and `detail`.
    public List<AdminRow> facts = new ArrayList<AdminRow>();
    /// A second table: up to four columns, `title`, `detail`, `meta`,
    /// `amount`, with their headings.
    public String rowsTitle = "";
    public String rowsEmpty = "Nothing yet.";
    public String headA = "";
    public String headB = "";
    public String headC = "";
    public String headD = "";
    public List<AdminRow> rows = new ArrayList<AdminRow>();

    /// A list whose entries carry their own buttons, under `cardsTitle`.
    public String cardsTitle = "";
    public List<ActionCard> cards = new ArrayList<ActionCard>();
    /// What the shopper must fix, above the page (a typo in a quick order,
    /// refused CSV lines), one title each.
    public List<AdminRow> problems = new ArrayList<AdminRow>();
}
