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

/// What `templates/marketing.html` draws: fulfilment board, promotions,
/// calendar, loyalty, clubs, segments, campaigns and journeys. `page` picks
/// the section; most pages are a list (`table` in the model), some
/// [FormView]s, a few facts and a table of rows.
public class MarketingView {
    /// "board", "sales", "sale", "calendar", "discounts", "discount",
    /// "loyalty", "clubs", "club", "segments", "segment", "campaigns",
    /// "campaign", "journeys" or "journey".
    public String page = "";
    public String base = "";
    public String heading = "";
    public String intro = "";
    /// The list page this one belongs to.
    public String back = "";
    public String backLabel = "";
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

    // ---------------------------------------------------------------- board
    public List<BoardColumn> columns = new ArrayList<BoardColumn>();

    // ------------------------------------------------------------- calendar
    public String month = "";
    public String prevUrl = "";
    public String nextUrl = "";
    public List<CalendarWeek> weeks = new ArrayList<CalendarWeek>();

    // -------------------------------------------------------------- segment
    public String segAction = "";
    public String segUrl = "";
    public String segName = "";
    public String segOp = "AND";
    public String segKeep = "";
    public String segRows = "";
    public String segCount = "";
    public String segError = "";
    public boolean segNew;
    public List<RuleRow> rules = new ArrayList<RuleRow>();
    public List<AdminRow> sample = new ArrayList<AdminRow>();

    // -------------------------------------------------------------- journey
    public String flowBox = "0 0 100 100";
    public List<FlowNode> nodes = new ArrayList<FlowNode>();
    public List<FlowEdge> edges = new ArrayList<FlowEdge>();
    public List<AdminRow> problems = new ArrayList<AdminRow>();
}
