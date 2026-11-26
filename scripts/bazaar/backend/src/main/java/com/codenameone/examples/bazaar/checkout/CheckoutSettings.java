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
package com.codenameone.examples.bazaar.checkout;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.CheckoutFieldDto;
import com.codenameone.examples.bazaar.api.CheckoutOptionsDto;
import com.codenameone.examples.bazaar.api.CheckoutSettingsDto;
import com.codenameone.examples.bazaar.api.FieldAnswerDto;
import com.codenameone.examples.bazaar.api.OrderFieldDto;
import com.codenameone.examples.bazaar.api.PaymentTypeDto;
import com.codenameone.examples.bazaar.api.TipOptionDto;
import com.codenameone.examples.bazaar.api.TipPresetDto;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.ops.Tips;
import com.codenameone.examples.bazaar.pay.PaymentTypes;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A store's checkout settings: the payment method types it offers beside
/// cards ([PaymentTypes]), its tip presets, and the fields shoppers fill in.
///
/// - **Tips** are a share of the goods after discounts, or a fixed amount
///   ([Tips]). The shopper picks a preset by its number; the server works the
///   amount out again, so a client cannot name its own. The tip is kept on
///   the order beside the total, never taxed or discounted, and added to what
///   the card is charged.
/// - **Fields** are text, a checkbox or a select, each optionally required.
///   The answers are kept on the order with the label as it read then.
@Component
public class CheckoutSettings {
    public static final String TEXT = "text";
    public static final String CHECKBOX = "checkbox";
    public static final String SELECT = "select";
    /// The most fields a store may ask for.
    public static final int MAX_FIELDS = 10;
    /// The largest fixed tip, in cents.
    static final long MAX_FIXED_TIP = 100000L;
    /// How long a text answer may be.
    public static final int MAX_ANSWER = 500;

    private final DataSource db;
    private final StoreAccess access;

    public CheckoutSettings(DataSource db, StoreAccess access) {
        this.db = db;
        this.access = access;
    }

    /// The store's settings, for its staff.
    @Transactional(readOnly = true)
    public CheckoutSettingsDto read(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, false, StoreAccess.MANAGERS);
        return of(storeId);
    }

    /// The store's settings with no access check, for the checkout applying
    /// them. A store that saved none offers cards only, no tip and no fields.
    @Transactional(readOnly = true)
    public CheckoutSettingsDto of(String storeId) throws IOException {
        Map r = db.queryOne("SELECT * FROM checkout_settings WHERE store_id = ?",
                new Object[] {storeId});
        List<String> methods = new ArrayList<String>();
        List<TipPresetDto> tips = new ArrayList<TipPresetDto>();
        boolean tipsOn = false;
        if (r != null) {
            split(Rows.text(r.get("methods")), methods);
            tipsOn = Rows.number(r.get("tips_enabled")) != 0;
            tips = presets(Rows.text(r.get("tip_presets")));
        }
        return new CheckoutSettingsDto(methods, tipsOn, tips, fields(storeId));
    }

    /// Every payment method type the store may switch on, for the staff who
    /// may read its settings.
    @Transactional(readOnly = true)
    public List<PaymentTypeDto> types(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, false, StoreAccess.MANAGERS);
        return PaymentTypes.all();
    }

    @Transactional(rollbackFor = IOException.class)
    public CheckoutSettingsDto save(String username, String storeId, CheckoutSettingsDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, true, StoreAccess.MANAGERS);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Send the settings");
        }
        StringBuilder methods = new StringBuilder();
        List<String> asked = wanted.methods() == null ? new ArrayList<String>() : wanted.methods();
        for (int iter = 0; iter < asked.size(); iter++) {
            String type = asked.get(iter) == null ? "" : asked.get(iter).trim();
            if (!PaymentTypes.known(type)) {
                throw new ResponseStatusException(400, "Not a payment method type: " + type);
            }
            if (("," + methods + ",").indexOf("," + type + ",") < 0) {
                methods.append(methods.length() == 0 ? "" : ",").append(type);
            }
        }
        List<TipPresetDto> tips = wanted.tips() == null ? new ArrayList<TipPresetDto>()
                : wanted.tips();
        if (tips.size() > Tips.MAX_PRESETS) {
            throw new ResponseStatusException(400, "At most " + Tips.MAX_PRESETS
                    + " tip presets");
        }
        if (wanted.tipsEnabled() && tips.isEmpty()) {
            throw new ResponseStatusException(400, "Tips need at least one preset");
        }
        StringBuilder presets = new StringBuilder();
        for (int iter = 0; iter < tips.size(); iter++) {
            TipPresetDto t = tips.get(iter);
            boolean percent = t != null && t.percentBp() > 0 && t.fixedCents() <= 0;
            boolean fixed = t != null && t.fixedCents() > 0 && t.percentBp() <= 0;
            if (!percent && !fixed) {
                throw new ResponseStatusException(400, "A tip preset is a percentage or an amount");
            }
            if (percent && t.percentBp() > Tips.MAX_PERCENT_BP
                    || fixed && t.fixedCents() > MAX_FIXED_TIP) {
                throw new ResponseStatusException(400, "That tip preset is too large");
            }
            presets.append(presets.length() == 0 ? "" : ",")
                    .append(percent ? "p" + t.percentBp() : "f" + t.fixedCents());
        }
        List<CheckoutFieldDto> fields = wanted.fields() == null
                ? new ArrayList<CheckoutFieldDto>() : wanted.fields();
        if (fields.size() > MAX_FIELDS) {
            throw new ResponseStatusException(400, "At most " + MAX_FIELDS + " checkout fields");
        }
        for (int iter = 0; iter < fields.size(); iter++) {
            check(fields.get(iter));
        }
        db.execute("DELETE FROM checkout_settings WHERE store_id = ?", new Object[] {storeId});
        db.execute("INSERT INTO checkout_settings (store_id, methods, tips_enabled, tip_presets, "
                + "updated_at) VALUES (?, ?, ?, ?, ?)", new Object[] {storeId, methods.toString(),
                    Integer.valueOf(wanted.tipsEnabled() ? 1 : 0), presets.toString(),
                    Long.valueOf(System.currentTimeMillis())});
        // The fields are replaced as a list; a field keeps its id when it is
        // sent back with it, which is all an order's answers never needed.
        db.execute("DELETE FROM checkout_field WHERE store_id = ?", new Object[] {storeId});
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < fields.size(); iter++) {
            CheckoutFieldDto f = fields.get(iter);
            String id = f.id() != null && f.id().length() == 32 && hex(f.id()) ? f.id() : Ids.next();
            if (db.queryOne("SELECT id FROM checkout_field WHERE id = ?", new Object[] {id})
                    != null) {
                id = Ids.next();
            }
            db.execute("INSERT INTO checkout_field (id, store_id, sort_order, label, kind, "
                    + "choices, required, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[] {id, storeId, Integer.valueOf(iter), f.label().trim(), f.kind(),
                        choices(f), Integer.valueOf(f.required() ? 1 : 0), Long.valueOf(now)});
        }
        return of(storeId);
    }

    private static void check(CheckoutFieldDto f) {
        if (f == null || f.label() == null || f.label().trim().length() == 0
                || f.label().trim().length() > 120) {
            throw new ResponseStatusException(400, "A checkout field needs a label of up to 120 "
                    + "characters");
        }
        if (!TEXT.equals(f.kind()) && !CHECKBOX.equals(f.kind()) && !SELECT.equals(f.kind())) {
            throw new ResponseStatusException(400, "A checkout field is text, a checkbox or a "
                    + "select");
        }
        if (SELECT.equals(f.kind()) && options(f.options()).isEmpty()) {
            throw new ResponseStatusException(400, f.label().trim() + " needs its choices, one "
                    + "per line");
        }
        if (f.options() != null && f.options().length() > 1000) {
            throw new ResponseStatusException(400, "The choices are too long");
        }
    }

    private static String choices(CheckoutFieldDto f) {
        if (!SELECT.equals(f.kind())) {
            return "";
        }
        List<String> o = options(f.options());
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < o.size(); iter++) {
            b.append(iter == 0 ? "" : "\n").append(o.get(iter));
        }
        return b.toString();
    }

    /// A select's choices, one per line, trimmed, blank lines dropped.
    public static List<String> options(String text) {
        List<String> out = new ArrayList<String>();
        if (text == null) {
            return out;
        }
        int start = 0;
        while (start <= text.length()) {
            int nl = text.indexOf('\n', start);
            String line = text.substring(start, nl < 0 ? text.length() : nl).trim();
            if (line.length() > 0 && line.length() <= 120 && !out.contains(line)) {
                out.add(line);
            }
            if (nl < 0) {
                break;
            }
            start = nl + 1;
        }
        return out;
    }

    private List<CheckoutFieldDto> fields(String storeId) throws IOException {
        List rows = db.query("SELECT * FROM checkout_field WHERE store_id = ? ORDER BY sort_order",
                new Object[] {storeId});
        List<CheckoutFieldDto> out = new ArrayList<CheckoutFieldDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new CheckoutFieldDto(Rows.text(r.get("id")), Rows.text(r.get("label")),
                    Rows.text(r.get("kind")), Rows.text(r.get("choices")),
                    Rows.number(r.get("required")) != 0));
        }
        return out;
    }

    // ---------------------------------------------------------------- tips

    /// The tip for preset `choice` (0 none) on `baseCents` of goods after
    /// discounts. A choice the store does not offer is a 400; with tips off,
    /// only "none" is.
    public static long tip(CheckoutSettingsDto s, int choice, long baseCents) {
        if (choice == 0) {
            return 0;
        }
        long tip = s.tipsEnabled() ? Tips.forChoice(s.tips(), choice, Math.max(0, baseCents)) : -1;
        if (tip < 0) {
            throw new ResponseStatusException(400, "That tip is not on offer");
        }
        return tip;
    }

    /// The tips on offer on `baseCents` of goods, labelled with `money` for
    /// a fixed one. Empty with tips off.
    public static List<TipOptionDto> tipOptions(CheckoutSettingsDto s, long baseCents,
            String currency) {
        List<TipOptionDto> out = new ArrayList<TipOptionDto>();
        if (!s.tipsEnabled()) {
            return out;
        }
        for (int iter = 0; iter < s.tips().size(); iter++) {
            TipPresetDto t = s.tips().get(iter);
            String label = Tips.percentLabel(t);
            if (label == null) {
                label = com.codenameone.examples.bazaar.web.Money.format(t.fixedCents(),
                        currency);
            }
            out.add(new TipOptionDto(iter + 1, label, Tips.amount(t, Math.max(0, baseCents))));
        }
        return out;
    }

    /// `p1500,f500` as presets; anything unreadable is skipped.
    static List<TipPresetDto> presets(String text) {
        List<String> parts = new ArrayList<String>();
        split(text, parts);
        List<TipPresetDto> out = new ArrayList<TipPresetDto>();
        for (int iter = 0; iter < parts.size(); iter++) {
            String p = parts.get(iter);
            long n = digits(p.substring(1));
            if (n <= 0) {
                continue;
            }
            if (p.charAt(0) == 'p') {
                out.add(new TipPresetDto((int) Math.min(n, Tips.MAX_PERCENT_BP), 0));
            } else if (p.charAt(0) == 'f') {
                out.add(new TipPresetDto(0, n));
            }
        }
        return out;
    }

    // -------------------------------------------------------------- fields

    /// The shopper's answers checked against the store's fields: each
    /// required one answered (a checkbox ticked), a select answered with one
    /// of its choices, text cut to [#MAX_ANSWER]. Answers the order's rows in
    /// field order, unanswered optional fields left out.
    public static List<OrderFieldDto> answers(CheckoutSettingsDto s, List<FieldAnswerDto> given) {
        List<OrderFieldDto> out = new ArrayList<OrderFieldDto>();
        for (int iter = 0; iter < s.fields().size(); iter++) {
            CheckoutFieldDto f = s.fields().get(iter);
            String value = "";
            for (int a = 0; given != null && a < given.size(); a++) {
                FieldAnswerDto g = given.get(a);
                if (g != null && f.id().equals(g.fieldId()) && g.value() != null) {
                    value = g.value().trim();
                }
            }
            if (CHECKBOX.equals(f.kind())) {
                value = value.length() == 0 || "no".equals(value) || "false".equals(value) ? ""
                        : "yes";
            } else if (SELECT.equals(f.kind())) {
                if (value.length() > 0 && !options(f.options()).contains(value)) {
                    throw new ResponseStatusException(400, "Choose one of the choices for "
                            + f.label());
                }
            } else if (value.length() > MAX_ANSWER) {
                value = value.substring(0, MAX_ANSWER);
            }
            if (value.length() == 0) {
                if (f.required()) {
                    throw new ResponseStatusException(400, (CHECKBOX.equals(f.kind())
                            ? "Tick " : "Fill in ") + f.label());
                }
                continue;
            }
            out.add(new OrderFieldDto(f.label(), value));
        }
        return out;
    }

    /// Keeps an order's answers; inside the order's transaction.
    public void keep(String orderId, String storeId, List<OrderFieldDto> answers)
            throws IOException {
        for (int iter = 0; answers != null && iter < answers.size(); iter++) {
            OrderFieldDto a = answers.get(iter);
            db.execute("INSERT INTO order_field (id, order_id, store_id, sort_order, label, "
                    + "answer) VALUES (?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(), orderId,
                        storeId, Integer.valueOf(iter), a.label(), a.value()});
        }
    }

    /// An order's answers in the order the fields were asked.
    @Transactional(readOnly = true)
    public List<OrderFieldDto> answersOf(String orderId) throws IOException {
        List rows = db.query("SELECT label, answer FROM order_field WHERE order_id = ? "
                + "ORDER BY sort_order", new Object[] {orderId});
        List<OrderFieldDto> out = new ArrayList<OrderFieldDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new OrderFieldDto(Rows.text(r.get("label")), Rows.text(r.get("answer"))));
        }
        return out;
    }

    /// What the checkout offers now; see `CheckoutApi.options`.
    public CheckoutOptionsDto options(String storeId, String provider, String currency,
            String country, long baseCents, String webCheckoutUrl) throws IOException {
        CheckoutSettingsDto s = of(storeId);
        // A processor with no browser half (the simulated one has one of its
        // own) offers nothing beyond cards.
        List<String> enabled = s.methods();
        return new CheckoutOptionsDto(provider, PaymentTypes.offered(enabled, currency, country),
                tipOptions(s, baseCents, currency), s.fields(), webCheckoutUrl);
    }

    // ------------------------------------------------------------- helpers

    private static void split(String text, List<String> into) {
        if (text == null) {
            return;
        }
        int start = 0;
        while (start < text.length()) {
            int comma = text.indexOf(',', start);
            String part = text.substring(start, comma < 0 ? text.length() : comma).trim();
            if (part.length() > 0) {
                into.add(part);
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
    }

    private static long digits(String text) {
        if (text.length() == 0 || text.length() > 12) {
            return -1;
        }
        long n = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            n = n * 10 + (c - '0');
        }
        return n;
    }

    private static boolean hex(String id) {
        for (int iter = 0; iter < id.length(); iter++) {
            char c = id.charAt(iter);
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }
        return true;
    }
}
