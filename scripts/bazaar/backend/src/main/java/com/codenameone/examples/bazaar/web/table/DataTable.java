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
package com.codenameone.examples.bazaar.web.table;

import com.codenameone.examples.bazaar.web.NavItem;

import java.util.ArrayList;
import java.util.List;

/// What `templates/table.html` draws: one server-rendered data table, the
/// same for every grid in the web app -- orders, products, inventory,
/// customers, an account's orders. Built by [Grid]; a page puts it in its
/// model as `table` and includes `table :: grid`.
///
/// Sorting, filtering and paging are links and a GET form, so they work with
/// no script at all; htmx swaps only the table when it is there. The column
/// chooser is a list of checkboxes the page script applies (and remembers);
/// CSV export is the same address with `format=csv`.
public class DataTable {
    public String id = "grid";
    /// The page's own address, without a query.
    public String base = "";
    public String q = "";
    public String searchLabel = "Search";
    public List<Column> columns = new ArrayList<Column>();
    public List<Row> rows = new ArrayList<Row>();
    /// Extra filters: a select per filter.
    public List<Filter> filters = new ArrayList<Filter>();
    /// Values kept across sorting and paging, as hidden inputs of the search form.
    public List<Param> keep = new ArrayList<Param>();
    public int total;
    public int from;
    public int to;
    public int page;
    public int pages;
    public String prevUrl = "";
    public String nextUrl = "";
    public String csvUrl = "";
    /// Bulk actions: a checkbox per row, and a button per action, posted to
    /// `bulkUrl` with `action` and every checked `ids`.
    public boolean selectable;
    public String bulkUrl = "";
    public List<NavItem> actions = new ArrayList<NavItem>();
    public String empty = "Nothing here yet.";

    public static class Column {
        public String key = "";
        public String label = "";
        public boolean num;
        public boolean sortable = true;
        /// "none", "ascending" or "descending" (the `aria-sort` value).
        public String sort = "none";
        public String sortUrl = "";
    }

    public static class Row {
        public String id = "";
        public String href = "";
        public List<Cell> cells = new ArrayList<Cell>();
    }

    public static class Cell {
        public String col = "";
        public String text = "";
        public String href = "";
        public boolean num;
        /// A status shown as a pill, with its tone ("ok", "warn", "bad" or "").
        public boolean pill;
        public String tone = "";
        /// An inline edit: a number field posted to `editUrl` as `value`.
        public boolean edit;
        public String editUrl = "";
        public String editLabel = "";
    }

    public static class Filter {
        public String name = "";
        public String label = "";
        public List<Option> options = new ArrayList<Option>();
    }

    public static class Option {
        public String value = "";
        public String label = "";
        public boolean selected;

        public Option() {
        }

        public Option(String value, String label, boolean selected) {
            this.value = value;
            this.label = label;
            this.selected = selected;
        }
    }

    public static class Param {
        public String name = "";
        public String value = "";

        public Param() {
        }

        public Param(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
