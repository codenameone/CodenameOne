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

/// What `templates/admin.html` draws for one back-office page. `page` picks
/// the section; a list page also has a `table` in the model
/// ([com.codenameone.examples.bazaar.web.table.DataTable]).
public class AdminView {
    /// "dashboard", "orders", "order", "products", "product", "inventory",
    /// "customers" or "customer".
    public String page = "dashboard";
    public String base = "";
    /// The customer page's merge, erase and export targets.
    public String mergeAction = "";
    public String eraseAction = "";
    public String exportUrl = "";
    /// Customers of the store that look like the same person.
    public List<AdminRow> duplicates = new ArrayList<AdminRow>();
    /// A customer's store credit grants, or the store's data requests.
    public List<AdminRow> credits = new ArrayList<AdminRow>();
    public List<AdminRow> requests = new ArrayList<AdminRow>();
    /// The product's translation sheet, from its page.
    public String translationsUrl = "";

    // ------------------------------------------------------------ dashboard
    /// False for a role that may not read the store's figures (fulfilment,
    /// cashier): the dashboard then shows only its links.
    public boolean figures;
    public List<Kpi> kpis = new ArrayList<Kpi>();
    /// The sales line: a polyline, the area under it, and the day labels.
    public String salesLine = "";
    public String salesArea = "";
    public String salesMax = "";
    public String salesSummary = "";
    public List<ChartMark> salesTicks = new ArrayList<ChartMark>();
    /// The sales table the chart stands for, for a screen reader.
    public List<AdminRow> salesDays = new ArrayList<AdminRow>();
    public List<ChartMark> topBars = new ArrayList<ChartMark>();
    public int topHeight = 40;
    public String topBox = "0 0 600 40";

    // ---------------------------------------------------------------- order
    public AdminRow order = new AdminRow();
    public List<AdminRow> lines = new ArrayList<AdminRow>();
    public List<AdminRow> events = new ArrayList<AdminRow>();
    public List<AdminRow> totals = new ArrayList<AdminRow>();
    public List<AdminRow> locations = new ArrayList<AdminRow>();
    public String shipTo = "";
    public String customer = "";
    public String customerUrl = "";
    public String fulfilAction = "";
    public String refundAction = "";
    public String cancelAction = "";
    public boolean canFulfil;
    public boolean canRefund;
    public boolean canCancel;
    /// One-click order actions shown under the title: archive or unarchive,
    /// send a pay link. Each is a post form: title, action, flag = danger.
    public List<AdminRow> orderTools = new ArrayList<AdminRow>();
    /// The order's fulfilments ([Parcels]) and its packing slip.
    public List<ParcelView> parcels = new ArrayList<ParcelView>();
    public String slipUrl = "";

    // -------------------------------------------------------------- product
    public String saveAction = "";
    public String title = "";
    public String description = "";
    public String status = "";
    public List<AdminRow> statuses = new ArrayList<AdminRow>();
    /// The product's taxonomy category as its path, or empty.
    public String category = "";
    /// Every taxonomy category's path, for the picker's suggestions.
    public List<String> taxonomy = new ArrayList<String>();
    /// The units a variant's measure may be in.
    public List<String> units = new ArrayList<String>();
    public List<AdminRow> variants = new ArrayList<AdminRow>();
    /// Stock per variant and location, each with its adjust form.
    public List<AdminRow> levels = new ArrayList<AdminRow>();
    public List<AdminRow> reasons = new ArrayList<AdminRow>();
    public String adjustAction = "";
    /// The product's bundle form: whether it is one, its component rows
    /// (`field` the variant select's name, `field2` the quantity's, `id` the
    /// variant chosen, `quantity` how many), the variants it may pick from
    /// and how many bundles the components make.
    public String bundleAction = "";
    /// The product's gallery entries (pictures, videos, models) in order;
    /// `action` moves one up, `url` moves it down.
    public List<AdminRow> mediaRows = new ArrayList<AdminRow>();
    public String mediaAction = "";
    /// One row per digital variant: its file, limits and licence keys.
    public List<AdminRow> downloadRows = new ArrayList<AdminRow>();
    /// An order's downloads; `action` revokes one.
    public List<AdminRow> grantRows = new ArrayList<AdminRow>();
    public boolean isBundle;
    public String bundleAvailable = "";
    public List<AdminRow> bundleRows = new ArrayList<AdminRow>();
    public List<AdminRow> bundleChoices = new ArrayList<AdminRow>();
    /// The product's publishing window per channel: `id` the channel, `title`
    /// its name, `field`/`field2` the inputs' names, `price`/`cost` their
    /// values (UTC) and `status` whether it shows there now.
    public String scheduleAction = "";
    public List<AdminRow> windows = new ArrayList<AdminRow>();

    // ------------------------------------------------------------- customer
    public List<AdminRow> facts = new ArrayList<AdminRow>();
    public List<AdminRow> orders = new ArrayList<AdminRow>();
    public List<AdminRow> addresses = new ArrayList<AdminRow>();

    // ------------------------------------------------------------------ app
    public com.codenameone.examples.bazaar.web.slots.AppSlot app =
            new com.codenameone.examples.bazaar.web.slots.AppSlot();
    public boolean appNew;
    public String appAction = "";
    public String deleteAction = "";
    public List<AdminRow> places = new ArrayList<AdminRow>();
    public List<AdminRow> categories = new ArrayList<AdminRow>();
}
