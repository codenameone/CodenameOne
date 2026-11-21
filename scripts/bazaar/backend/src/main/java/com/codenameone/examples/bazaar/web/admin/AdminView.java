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
package com.codenameone.examples.bazaar.web.admin;

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

    // -------------------------------------------------------------- product
    public String saveAction = "";
    public String title = "";
    public String description = "";
    public String status = "";
    public List<AdminRow> statuses = new ArrayList<AdminRow>();
    public List<AdminRow> variants = new ArrayList<AdminRow>();
    /// Stock per variant and location, each with its adjust form.
    public List<AdminRow> levels = new ArrayList<AdminRow>();
    public List<AdminRow> reasons = new ArrayList<AdminRow>();
    public String adjustAction = "";

    // ------------------------------------------------------------- customer
    public List<AdminRow> facts = new ArrayList<AdminRow>();
    public List<AdminRow> orders = new ArrayList<AdminRow>();
    public List<AdminRow> addresses = new ArrayList<AdminRow>();
}
