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

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.api.CheckoutFieldDto;
import com.codenameone.examples.bazaar.api.CheckoutSettingsDto;
import com.codenameone.examples.bazaar.api.PaymentTypeDto;
import com.codenameone.examples.bazaar.api.TipPresetDto;
import com.codenameone.examples.bazaar.checkout.CheckoutSettings;
import com.codenameone.examples.bazaar.pay.PaymentTypes;
import com.codenameone.examples.bazaar.web.Money;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.table.DataTable;

import java.util.ArrayList;
import java.util.List;

/// The back office's checkout settings ([CheckoutSettings]): the payment
/// methods beside cards the store takes, its tip presets, and the fields
/// shoppers fill in at checkout.
///
/// Presets are typed as one line, `15%, 20%, 5.00`; fields one per line, as
/// `Label | text, checkbox or select | required | choice; choice`.
@Controller
public class CheckoutSettingsPages {
    private final WebPages pages;
    private final CheckoutSettings settings;

    public CheckoutSettingsPages(WebPages pages, CheckoutSettings settings) {
        this.pages = pages;
        this.settings = settings;
    }

    @GetMapping("/admin/{storeId}/checkout-settings")
    public String show(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        Shell shell = pages.admin(request, storeId, "Checkout settings");
        CheckoutSettingsDto s = settings.read(Caller.name(), storeId);
        String action = "/admin/" + storeId + "/checkout-settings";
        MarketingView v = new MarketingView();
        v.page = "checkout-settings";
        FormView methods = new FormView("methods", "Payment methods", action, "Save");
        List<PaymentTypeDto> all = PaymentTypes.all();
        for (int iter = 0; iter < all.size(); iter++) {
            PaymentTypeDto t = all.get(iter);
            methods.check("m-" + t.type(), t.label() + (t.async() ? " (paid later)" : ""),
                    s.methods().contains(t.type()));
        }
        methods.check("tipsEnabled", "Ask for a tip at checkout", s.tipsEnabled());
        methods.add("tips", "Tip presets", "text", presets(s.tips()))
                .hint("Up to four: a percent of the goods after discounts (15%) or an amount "
                    + "(5.00). Tips are kept outside the order total and never taxed.");
        methods.add("fields", "Checkout fields", "textarea", fields(s.fields()))
                .hint("One per line: Label | text, checkbox or select | required | choice; "
                    + "choice");
        methods.title = "Payment methods, tips and checkout fields";
        v.forms.add(methods);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        v.base = "/admin/" + storeId;
        v.heading = "Checkout settings";
        model.addAttribute("view", v);
        model.addAttribute("table", new DataTable());
        return WebPages.view(model, shell, "marketing");
    }

    @PostMapping("/admin/{storeId}/checkout-settings")
    public String save(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        pages.admin(request, storeId, "");
        String back = "/admin/" + storeId + "/checkout-settings";
        try {
            List<String> methods = new ArrayList<String>();
            List<PaymentTypeDto> all = PaymentTypes.all();
            for (int iter = 0; iter < all.size(); iter++) {
                if ("1".equals(AdminForms.field(request, "m-" + all.get(iter).type()))) {
                    methods.add(all.get(iter).type());
                }
            }
            settings.save(Caller.name(), storeId, new CheckoutSettingsDto(methods,
                    "1".equals(AdminForms.field(request, "tipsEnabled")),
                    parsePresets(AdminForms.field(request, "tips")),
                    parseFields(AdminForms.field(request, "fields"))));
            WebPages.toast(request, "Checkout settings saved");
        } catch (ResponseStatusException e) {
            int status = e.getStatus();
            if (status == 401 || status == 403 || status == 404) {
                throw e;
            }
            WebPages.toast(request, e.getReason() == null ? "That did not work" : e.getReason());
        }
        return "redirect:" + back;
    }

    /// `15%, 5.00` as presets; a blank line is none.
    static List<TipPresetDto> parsePresets(String text) {
        List<TipPresetDto> out = new ArrayList<TipPresetDto>();
        String t = text == null ? "" : text;
        int start = 0;
        while (start <= t.length()) {
            int comma = t.indexOf(',', start);
            String one = t.substring(start, comma < 0 ? t.length() : comma).trim();
            if (one.endsWith("%")) {
                out.add(new TipPresetDto(OrderOpsPages.basisPoints(one), 0));
            } else if (one.length() > 0) {
                out.add(new TipPresetDto(0, AdminForms.cents(one, "A tip amount")));
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        return out;
    }

    static String presets(List<TipPresetDto> presets) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < presets.size(); iter++) {
            TipPresetDto p = presets.get(iter);
            b.append(iter == 0 ? "" : ", ");
            if (p.percentBp() > 0) {
                b.append(OrderOpsPages.percent(p.percentBp())).append('%');
            } else {
                b.append(Money.plain(p.fixedCents()));
            }
        }
        return b.toString();
    }

    /// The fields textarea, one field per line.
    static List<CheckoutFieldDto> parseFields(String text) {
        List<CheckoutFieldDto> out = new ArrayList<CheckoutFieldDto>();
        String t = text == null ? "" : text.replace("\r", "");
        int start = 0;
        while (start <= t.length()) {
            int nl = t.indexOf('\n', start);
            String line = t.substring(start, nl < 0 ? t.length() : nl).trim();
            if (line.length() > 0) {
                List<String> parts = parts(line, '|');
                String kind = parts.size() > 1 ? parts.get(1) : CheckoutSettings.TEXT;
                boolean required = parts.size() > 2 && "required".equalsIgnoreCase(parts.get(2));
                StringBuilder choices = new StringBuilder();
                List<String> cs = parts.size() > 3 ? parts(parts.get(3), ';')
                        : new ArrayList<String>();
                for (int iter = 0; iter < cs.size(); iter++) {
                    choices.append(iter == 0 ? "" : "\n").append(cs.get(iter));
                }
                out.add(new CheckoutFieldDto(null, parts.get(0), kind.length() == 0
                        ? CheckoutSettings.TEXT : kind, choices.toString(), required));
            }
            if (nl < 0) {
                break;
            }
            start = nl + 1;
        }
        return out;
    }

    static String fields(List<CheckoutFieldDto> fields) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < fields.size(); iter++) {
            CheckoutFieldDto f = fields.get(iter);
            b.append(iter == 0 ? "" : "\n").append(f.label()).append(" | ").append(f.kind())
                    .append(" | ").append(f.required() ? "required" : "optional");
            List<String> cs = CheckoutSettings.options(f.options());
            for (int c = 0; c < cs.size(); c++) {
                b.append(c == 0 ? " | " : "; ").append(cs.get(c));
            }
        }
        return b.toString();
    }

    private static List<String> parts(String line, char separator) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        while (start <= line.length()) {
            int at = line.indexOf(separator, start);
            out.add(line.substring(start, at < 0 ? line.length() : at).trim());
            if (at < 0) {
                break;
            }
            start = at + 1;
        }
        return out;
    }
}
