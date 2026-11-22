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

import com.codename1.backend.mvc.Model;
import com.codenameone.examples.bazaar.web.NavItem;
import com.codenameone.examples.bazaar.web.StoreText;
import com.codenameone.examples.bazaar.web.table.DataTable;
import java.util.List;
import java.util.Map;

/// Puts a back-office page's words into the reader's language, at the one
/// place every back-office page passes (`WebPages.view`): the data table's
/// frame, column headings, filters, bulk actions and empty message, and a
/// marketing-style page's headings and forms (title, text, button, field
/// labels and hints).
///
/// Each English string goes through [StoreText#translate], so a word with a
/// row in `storetext.py` is translated and one without stays English. Data
/// (product names, customers, amounts) is never a key and is left alone.
public final class AdminText {
    private AdminText() {
    }

    /// Translates what `model` holds for a back-office page into `t`.
    public static void localize(Model model, Map<String, String> t) {
        if (t == null) {
            return;
        }
        Object table = model.getAttribute("table");
        if (table instanceof DataTable) {
            table((DataTable) table, t);
        }
        Object view = model.getAttribute("view");
        if (view instanceof MarketingView) {
            MarketingView m = (MarketingView) view;
            m.heading = StoreText.translate(t, m.heading);
            m.intro = StoreText.translate(t, m.intro);
            m.backLabel = StoreText.translate(t, m.backLabel);
            m.rowsTitle = StoreText.translate(t, m.rowsTitle);
            m.rowsEmpty = StoreText.translate(t, m.rowsEmpty);
            m.headA = StoreText.translate(t, m.headA);
            m.headB = StoreText.translate(t, m.headB);
            m.headC = StoreText.translate(t, m.headC);
            m.headD = StoreText.translate(t, m.headD);
            m.cardsTitle = StoreText.translate(t, m.cardsTitle);
            m.chartTitle = StoreText.translate(t, m.chartTitle);
            rows(m.sinceOptions, t);
            for (int iter = 0; m.forms != null && iter < m.forms.size(); iter++) {
                form(m.forms.get(iter), t);
            }
        }
    }

    static void table(DataTable d, Map<String, String> t) {
        d.searchLabel = StoreText.translate(t, d.searchLabel);
        d.empty = StoreText.translate(t, d.empty);
        d.applyLabel = StoreText.translate(t, d.applyLabel);
        d.columnsLabel = StoreText.translate(t, d.columnsLabel);
        d.saveLabel = StoreText.translate(t, d.saveLabel);
        d.ofLabel = StoreText.translate(t, d.ofLabel);
        d.prevLabel = StoreText.translate(t, d.prevLabel);
        d.nextLabel = StoreText.translate(t, d.nextLabel);
        for (int iter = 0; iter < d.columns.size(); iter++) {
            DataTable.Column c = d.columns.get(iter);
            c.label = StoreText.translate(t, c.label);
        }
        for (int iter = 0; iter < d.filters.size(); iter++) {
            DataTable.Filter f = d.filters.get(iter);
            f.label = StoreText.translate(t, f.label);
            for (int o = 0; o < f.options.size(); o++) {
                f.options.get(o).label = StoreText.translate(t, f.options.get(o).label);
            }
        }
        List<NavItem> actions = d.actions;
        for (int iter = 0; iter < actions.size(); iter++) {
            actions.get(iter).label = StoreText.translate(t, actions.get(iter).label);
        }
    }

    static void form(FormView f, Map<String, String> t) {
        f.title = StoreText.translate(t, f.title);
        f.text = StoreText.translate(t, f.text);
        f.submit = StoreText.translate(t, f.submit);
        for (int iter = 0; iter < f.fields.size(); iter++) {
            FormField field = f.fields.get(iter);
            field.label = StoreText.translate(t, field.label);
            field.hint = StoreText.translate(t, field.hint);
        }
    }

    private static void rows(List<AdminRow> rows, Map<String, String> t) {
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            AdminRow r = rows.get(iter);
            r.title = StoreText.translate(t, r.title);
        }
    }
}
