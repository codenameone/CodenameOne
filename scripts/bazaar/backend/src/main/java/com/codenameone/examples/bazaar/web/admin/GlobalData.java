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
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.api.CustomsCodeDto;
import com.codenameone.examples.bazaar.api.DisputeDto;
import com.codenameone.examples.bazaar.api.DisputeEvidenceDto;
import com.codenameone.examples.bazaar.api.DutyRateDto;
import com.codenameone.examples.bazaar.api.FxRateDto;
import com.codenameone.examples.bazaar.api.MarketDto;
import com.codenameone.examples.bazaar.api.MarketPriceDto;
import com.codenameone.examples.bazaar.api.OrderRiskDto;
import com.codenameone.examples.bazaar.api.TaxExemptionDto;
import com.codenameone.examples.bazaar.api.TaxReportRowDto;
import com.codenameone.examples.bazaar.api.TranslationDto;
import com.codenameone.examples.bazaar.api.TranslationFieldDto;
import com.codenameone.examples.bazaar.dispute.Disputes;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.market.Translations;
import com.codenameone.examples.bazaar.risk.Fraud;
import com.codenameone.examples.bazaar.taxes.TaxExemptions;
import com.codenameone.examples.bazaar.web.Money;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Builds the back office's selling-abroad and trust pages -- markets and
/// exchange rates, translations, tax exemptions and the tax report, the
/// fraud review queue and disputes -- from the services that own each rule.
/// It only lays their answers out and turns form fields into their DTOs.
@Component
public class GlobalData {
    private final Markets markets;
    private final Translations translations;
    private final TaxExemptions taxes;
    private final Fraud fraud;
    private final Disputes disputes;
    private final AdminData admin;
    private final com.codenameone.examples.bazaar.ops.Reports reports;

    public GlobalData(Markets markets, Translations translations, TaxExemptions taxes,
            Fraud fraud, Disputes disputes, AdminData admin,
            com.codenameone.examples.bazaar.ops.Reports reports) {
        this.reports = reports;
        this.markets = markets;
        this.translations = translations;
        this.taxes = taxes;
        this.fraud = fraud;
        this.disputes = disputes;
        this.admin = admin;
    }

    private static String me() {
        return Caller.name();
    }

    private static String f(HttpServer.Request request, String name) {
        return AdminForms.field(request, name);
    }

    /// A form field as sent, trimmed, or null when the form did not send it.
    private static String sent(HttpServer.Request request, String name) {
        String v;
        try {
            v = request.param(name);
        } catch (RuntimeException malformed) {
            v = null;
        }
        return v == null ? null : v.trim();
    }

    private static AdminRow row(String a, String b, String c, String d) {
        AdminRow r = new AdminRow();
        r.title = a;
        r.detail = b;
        r.meta = c;
        r.amount = d;
        return r;
    }

    private static AdminRow link(String title, String url) {
        AdminRow r = new AdminRow();
        r.title = title;
        r.url = url;
        return r;
    }

    static int intField(String text, int fallback) {
        if (text == null || text.trim().length() == 0) {
            return fallback;
        }
        String t = text.trim();
        boolean negative = t.startsWith("-");
        long out = 0;
        for (int iter = negative ? 1 : 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c < '0' || c > '9' || out > 100000000L) {
                throw new ResponseStatusException(400, "Enter a whole number");
            }
            out = out * 10 + (c - '0');
        }
        return (int) (negative ? -out : out);
    }

    /// "1.0850" as micros (1085000): up to six decimals, no floating point.
    static long micros(String text) {
        String t = text == null ? "" : text.trim();
        int dot = t.indexOf('.');
        String whole = dot < 0 ? t : t.substring(0, dot);
        String frac = dot < 0 ? "" : t.substring(dot + 1);
        if (whole.length() == 0 && frac.length() == 0 || whole.length() > 6 || frac.length() > 6) {
            throw new ResponseStatusException(400, "Enter a rate like 1.0850");
        }
        long out = 0;
        String all = whole + (frac + "000000").substring(0, 6);
        for (int iter = 0; iter < all.length(); iter++) {
            char c = all.charAt(iter);
            if (c < '0' || c > '9') {
                throw new ResponseStatusException(400, "Enter a rate like 1.0850");
            }
            out = out * 10 + (c - '0');
        }
        return out;
    }

    static String rate(long micros) {
        String frac = Long.toString(1000000 + micros % 1000000).substring(1);
        return (micros / 1000000) + "." + frac;
    }

    // ============================================================== markets

    void markets(String storeId, MarketingView v) throws IOException {
        String base = "/admin/" + storeId + "/markets";
        String currency = admin.currency(storeId);
        List<MarketDto> all = markets.list(me(), storeId);
        v.rowsTitle = "Markets";
        v.rowsEmpty = "No markets yet: every shopper sees the store's own prices in "
                + currency + ".";
        v.headA = "Market";
        v.headB = "Countries";
        v.headC = "Currency";
        v.headD = "Adjustment";
        for (int iter = 0; iter < all.size(); iter++) {
            MarketDto m = all.get(iter);
            AdminRow r = row(m.name() + (m.active() ? "" : " (off)"), join(m.countries()),
                    m.currency() + ", " + m.rounding(), CommerceData.percent(m.adjustBps()) + "%");
            r.url = base + "/" + m.id();
            v.rows.add(r);
        }
        v.forms.add(marketForm("new-market", "Add a market", base, "Add market", null, currency));
        List<FxRateDto> rates = markets.rates(me(), storeId);
        v.cardsTitle = "Exchange rates (1 " + currency + " in each currency)";
        for (int iter = 0; iter < rates.size(); iter++) {
            ActionCard k = new ActionCard();
            k.title = rates.get(iter).currency();
            FxRateDto r = rates.get(iter);
            k.detail = rate(r.rateMicros()) + (r.feedMicros() > 0 ? ", ECB " + rate(r.feedMicros())
                    + " on " + com.codenameone.examples.bazaar.Days.iso(r.feedAt()) : "");
            v.cards.add(k);
        }
        FormView fx = new FormView("fx", "Set an exchange rate", "/admin/" + storeId
                + "/fx-rates", "Save rate");
        fx.text = "Your own rates. A market can use them or the European Central Bank's "
                + "daily rates instead, fetched by the server; a manual market in another "
                + "currency needs its rate first.";
        fx.add("currency", "Currency", "text", "").required = true;
        fx.add("rate", "Rate", "text", "").hint("How much 1 " + currency + " is, e.g. 0.9200");
        v.forms.add(fx);
        List<DutyRateDto> duties = markets.duties(me(), storeId);
        FormView duty = new FormView("duty", "Add a duty rate", "/admin/" + storeId
                + "/duty-rates", "Add rate");
        duty.text = duties.size() + " duty rates. The checkout estimates import duties from "
                + "each product's HS code and the longest matching prefix here.";
        duty.add("country", "Country (two letters)", "text", "").required = true;
        duty.add("hsPrefix", "HS code prefix", "text", "").hint("e.g. 6109 for T-shirts");
        duty.add("rateBps", "Rate (basis points)", "text", "").hint("1200 is 12%");
        v.forms.add(duty);
        for (int iter = 0; iter < duties.size(); iter++) {
            DutyRateDto d = duties.get(iter);
            ActionCard k = new ActionCard();
            k.title = "Duty " + d.country() + " " + (d.hsPrefix().length() == 0 ? "(any)"
                    : d.hsPrefix());
            k.detail = CommerceData.percent(d.rateBps()) + "%";
            k.button("Remove", "/admin/" + storeId + "/duty-rates/" + d.id() + "/remove", true);
            v.cards.add(k);
        }
        FormView hs = new FormView("hs", "Set a product's HS code", "/admin/" + storeId
                + "/customs-codes", "Save code");
        hs.add("productId", "Product id", "text", "").required = true;
        hs.add("hsCode", "HS code", "text", "");
        hs.text = markets.customsCodes(me(), storeId).size() + " products have an HS code.";
        v.forms.add(hs);
    }

    static String join(List<String> items) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; items != null && iter < items.size(); iter++) {
            if (iter > 0) {
                out.append(", ");
            }
            out.append(items.get(iter));
        }
        return out.toString();
    }

    static List<String> list(String text) {
        List<String> out = new ArrayList<String>();
        String t = text == null ? "" : text;
        int start = 0;
        while (start <= t.length()) {
            int comma = t.indexOf(',', start);
            int end = comma < 0 ? t.length() : comma;
            String part = t.substring(start, end).trim();
            if (part.length() > 0) {
                out.add(part);
            }
            start = end + 1;
        }
        return out;
    }

    private static FormView marketForm(String id, String title, String action, String submit,
            MarketDto m, String storeCurrency) {
        FormView form = new FormView(id, title, action, submit);
        form.add("code", "Code", "text", m == null ? "" : m.code()).hint("Lower case, e.g. eu");
        form.add("name", "Name", "text", m == null ? "" : m.name()).required = true;
        form.add("countries", "Countries", "text", m == null ? "" : join(m.countries()))
                .hint("Two-letter codes, comma separated");
        form.add("currency", "Currency", "text", m == null ? storeCurrency : m.currency());
        form.add("locale", "Language", "text", m == null ? "" : m.locale())
                .hint("e.g. de or fr-CA; translations in it are shown");
        form.add("rounding", "Rounding", "select", m == null ? Markets.NONE : m.rounding())
                .option(Markets.NONE, "To the cent")
                .option(Markets.WHOLE, "Up to a whole amount")
                .option(Markets.NINETY_NINE, "Up to .99");
        form.add("adjustBps", "Price adjustment (basis points)", "text",
                m == null ? "0" : Integer.toString(m.adjustBps())).hint("500 is 5% more");
        form.add("fxSource", "Exchange rate", "select", m == null ? Markets.MANUAL
                : m.fxSource())
                .option(Markets.MANUAL, "The rate you set")
                .option(Markets.ECB, "European Central Bank, daily");
        form.add("active", "Active", "checkbox", m == null || m.active() ? "true" : "");
        CommerceData.selectChosen(form);
        return form;
    }

    String saveMarket(String storeId, String marketId, HttpServer.Request request)
            throws IOException {
        MarketDto in = new MarketDto(marketId, f(request, "code"), f(request, "name"),
                f(request, "currency"), list(f(request, "countries")), f(request, "locale"),
                f(request, "rounding"), intField(f(request, "adjustBps"), 0),
                f(request, "active").length() > 0, f(request, "fxSource"));
        return markets.save(me(), storeId, marketId, in).id();
    }

    String market(String storeId, String marketId, MarketingView v) throws IOException {
        MarketDto m = markets.detail(me(), storeId, marketId);
        String base = "/admin/" + storeId + "/markets/" + m.id();
        v.back = "/admin/" + storeId + "/markets";
        v.backLabel = "Markets";
        v.forms.add(marketForm("market", "Market", base, "Save", m, admin.currency(storeId)));
        List<MarketPriceDto> ps = markets.prices(me(), storeId, marketId);
        v.rowsTitle = "Fixed prices";
        v.rowsEmpty = "No fixed prices: every variant is converted.";
        v.headA = "Variant";
        v.headB = "";
        v.headC = "";
        v.headD = "Price";
        for (int iter = 0; iter < ps.size(); iter++) {
            v.rows.add(row(ps.get(iter).variantId(), "", "", Money.format(ps.get(iter)
                    .priceCents(), m.currency())));
        }
        FormView price = new FormView("price", "Fix a variant's price", base + "/prices",
                "Save price");
        price.add("variantId", "Variant id", "text", "").required = true;
        price.add("price", "Price in " + m.currency(), "text", "").hint("Empty or 0 removes it");
        v.forms.add(price);
        v.cardsTitle = "Danger zone";
        ActionCard k = new ActionCard();
        k.title = "Remove this market";
        k.button("Remove", base + "/remove", true);
        v.cards.add(k);
        return m.name();
    }

    void putPrice(String storeId, String marketId, HttpServer.Request request)
            throws IOException {
        String p = f(request, "price");
        long cents = p.length() == 0 ? 0 : AdminForms.cents(p, "The price");
        markets.putPrice(me(), storeId, marketId, new MarketPriceDto(null, f(request, "variantId"),
                cents));
    }

    void removeMarket(String storeId, String marketId) throws IOException {
        markets.remove(me(), storeId, marketId);
    }

    void putRate(String storeId, HttpServer.Request request) throws IOException {
        markets.putRate(me(), storeId, new FxRateDto(f(request, "currency"),
                micros(f(request, "rate")), 0, "", 0));
    }

    void addDuty(String storeId, HttpServer.Request request) throws IOException {
        markets.addDuty(me(), storeId, new DutyRateDto(null, f(request, "country"),
                f(request, "hsPrefix"), intField(f(request, "rateBps"), 0)));
    }

    void removeDuty(String storeId, String dutyId) throws IOException {
        markets.removeDuty(me(), storeId, dutyId);
    }

    void putCustomsCode(String storeId, HttpServer.Request request) throws IOException {
        markets.putCustomsCode(me(), storeId, new CustomsCodeDto(f(request, "productId"),
                f(request, "hsCode")));
    }

    // ========================================================= translations

    void translations(String storeId, String kind, String refId, String locale,
            MarketingView v) throws IOException {
        String base = "/admin/" + storeId + "/translations";
        List<String> locales = translations.locales(me(), storeId);
        v.links.add(link("Privacy policy", base + "?kind=" + Translations.PAGE + "&refId="
                + Translations.PAGES[0]));
        FormView pick = new FormView("pick", "Translate", base + "/open", "Open");
        pick.add("kind", "What", "select", kind == null ? Translations.PRODUCT : kind)
                .option(Translations.PRODUCT, "Product").option(Translations.COLLECTION,
                "Collection").option(Translations.PAGE, "Page").option(Translations.CONTENT,
                "Online store page").option(Translations.POST, "Blog post")
                .option(Translations.MENU_LINK, "Menu link").option(Translations.METAFIELD,
                "Metafield name").option(Translations.METAOBJECT_TYPE, "Metaobject type")
                .option(Translations.METAOBJECT_ENTRY, "Metaobject entry")
                .option(Translations.CAMPAIGN, "Campaign message");
        pick.add("refId", "Id", "text", refId == null ? "" : refId)
                .hint("The id of the product, collection, page, post, menu link, metafield or "
                + "metaobject, the campaign, or privacy");
        pick.add("locale", "Language", "text", locale == null ? "" : locale)
                .hint(locales.isEmpty() ? "e.g. de or fr-CA" : "In use: " + join(locales));
        CommerceData.selectChosen(pick);
        v.forms.add(pick);
        // Bulk: one CSV per language, every product and collection, with
        // the original and the status beside each translation.
        for (int iter = 0; iter < locales.size(); iter++) {
            v.links.add(link("Export CSV, " + locales.get(iter), base + "/export.csv?locale="
                    + com.codenameone.examples.bazaar.web.SiteUrls.encode(locales.get(iter))));
        }
        FormView bulk = new FormView("import", "Import translations", base + "/import",
                "Import");
        bulk.text = "Paste a CSV exported here, with its translation column filled in. Only "
                + "the rows whose translation changed are saved.";
        bulk.add("csv", "CSV", "textarea", "");
        v.forms.add(bulk);
        if (kind == null || refId == null || refId.length() == 0) {
            return;
        }
        if (locale != null && locale.length() > 0) {
            sheet(storeId, kind, refId, locale, v);
            return;
        }
        Map<String, String> originals = translations.originals(storeId, kind, refId, null);
        List<TranslationDto> done = translations.of(me(), storeId, kind, refId);
        v.rowsTitle = "Original and translations";
        v.rowsEmpty = "Nothing to translate.";
        v.headA = "Field";
        v.headB = "Original";
        v.headC = "Language";
        v.headD = "";
        List<String> fields = Translations.fieldsOf(kind);
        for (int iter = 0; iter < fields.size(); iter++) {
            String field = fields.get(iter);
            v.rows.add(row(field, cut(originals.get(field)), "original", ""));
            for (int t = 0; t < done.size(); t++) {
                if (done.get(t).field().equals(field)) {
                    v.rows.add(row(field, cut(done.get(t).value()), done.get(t).locale(), ""));
                }
            }
        }
    }

    /// One thing's sheet in one language: each field's original, its
    /// translation and its status, side by side, and the form that edits
    /// them. A product's sheet has its options, pictures and text
    /// metafields too.
    private void sheet(String storeId, String kind, String refId, String locale,
            MarketingView v) throws IOException {
        String base = "/admin/" + storeId + "/translations";
        List<TranslationFieldDto> rows = translations.sheet(me(), storeId, kind, refId, locale);
        v.rowsTitle = "Original and translation to " + locale;
        v.rowsEmpty = "Nothing to translate.";
        v.headA = "Field";
        v.headB = "Original";
        v.headC = "Translation";
        v.headD = "Status";
        int missing = 0;
        int outdated = 0;
        for (int iter = 0; iter < rows.size(); iter++) {
            TranslationFieldDto f = rows.get(iter);
            v.rows.add(row(f.label(), cut(f.original()), cut(f.value()), words(f.status())));
            missing += Translations.MISSING.equals(f.status()) ? 1 : 0;
            outdated += Translations.OUTDATED.equals(f.status()) ? 1 : 0;
        }
        FormView edit = new FormView("translation", "Translation to " + locale, base, "Save");
        edit.text = rows.size() + " fields, " + missing + " missing, " + outdated
                + " outdated. The original is beside each field; an empty field shows the "
                + "original. Option values are comma separated, in the original's order.";
        edit.add("kind", "", "hidden", kind);
        edit.add("refId", "", "hidden", refId);
        edit.add("locale", "", "hidden", locale);
        for (int iter = 0; iter < rows.size(); iter++) {
            TranslationFieldDto f = rows.get(iter);
            String label = f.label() + (Translations.DONE.equals(f.status()) ? ""
                    : " (" + words(f.status()) + ")");
            edit.add(sheetField(f), label, "textarea", f.value()).hint("Original: "
                    + cut(f.original()));
        }
        v.forms.add(edit);
    }

    /// The form field of one sheet row.
    private static String sheetField(TranslationFieldDto f) {
        return "t." + f.kind() + "." + f.refId() + "." + f.field();
    }

    /// `MISSING` as `Missing`.
    private static String words(String status) {
        return status.length() == 0 ? status : status.charAt(0)
                + status.substring(1).toLowerCase(java.util.Locale.ROOT);
    }

    private static String cut(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 160 ? s.substring(0, 157) + "..." : s;
    }

    /// Saves the sheet's form: only the fields that changed, so a field left
    /// as it was keeps its status -- an outdated translation is not marked
    /// current by saving the page around it.
    void putTranslations(String storeId, HttpServer.Request request) throws IOException {
        String kind = f(request, "kind");
        String refId = f(request, "refId");
        String locale = f(request, "locale");
        List<TranslationFieldDto> rows = translations.sheet(me(), storeId, kind, refId, locale);
        for (int iter = 0; iter < rows.size(); iter++) {
            TranslationFieldDto row = rows.get(iter);
            // A field the form did not send is left alone, never emptied.
            String now = sent(request, sheetField(row));
            if (now == null && row.kind().equals(kind) && row.refId().equals(refId)) {
                // A form or script naming the thing's own fields plainly.
                now = sent(request, row.field());
            }
            if (now == null || now.trim().equals(row.value())) {
                continue;
            }
            translations.put(me(), storeId, new TranslationDto(row.kind(), row.refId(), locale,
                    row.field(), now));
        }
    }

    /// The store's catalog as a translation CSV.
    String exportTranslations(String storeId, String locale) throws IOException {
        return translations.exportCsv(me(), storeId, locale);
    }

    /// Reads a translation CSV back; answers what it did, for the flash.
    String importTranslations(String storeId, HttpServer.Request request) throws IOException {
        Translations.ImportResult r = translations.importCsv(me(), storeId, f(request, "csv"));
        StringBuilder out = new StringBuilder();
        out.append(r.saved).append(" saved, ").append(r.unchanged).append(" unchanged");
        if (!r.errors.isEmpty()) {
            out.append("; ").append(r.errors.size()).append(" refused: ");
            for (int iter = 0; iter < r.errors.size() && iter < 5; iter++) {
                out.append(iter > 0 ? "; " : "").append(r.errors.get(iter));
            }
        }
        return out.toString();
    }


    // ================================================================= taxes

    void exemptions(String storeId, String from, String to, MarketingView v)
            throws IOException {
        String base = "/admin/" + storeId + "/tax";
        List<TaxExemptionDto> all = taxes.list(me(), storeId);
        v.cardsTitle = "Tax exemptions";
        for (int iter = 0; iter < all.size(); iter++) {
            TaxExemptionDto e = all.get(iter);
            ActionCard k = new ActionCard();
            k.title = e.username();
            k.detail = "Certificate " + e.certificateId() + ", " + (e.regions().isEmpty()
                    ? "every region" : join(e.regions())) + (e.expiresAt() > 0 ? ", until "
                    + Days.iso(e.expiresAt()) : "");
            k.button("Remove", base + "/exemptions/" + e.id() + "/remove", true);
            v.cards.add(k);
        }
        FormView add = new FormView("exemption", "Add a tax exemption", base + "/exemptions",
                "Add exemption");
        add.text = "Orders by this customer to a covered region carry no tax.";
        add.add("username", "Customer (email)", "text", "").required = true;
        add.add("certificateId", "Certificate id", "text", "").required = true;
        add.add("regions", "Regions", "text", "").hint("e.g. US-NY, DE; empty for every one");
        add.add("expires", "Expires", "date", "");
        v.forms.add(add);
        from = periodStart(from, to);
        to = periodEnd(to);
        FormView period = new FormView("period", "Tax report", base + "/period", "Show");
        period.add("from", "From", "date", from == null ? "" : from);
        period.add("to", "To", "date", to == null ? "" : to);
        v.forms.add(period);
        v.links.add(link("Download CSV", base + "/report.csv?from=" + (from == null ? ""
                : from) + "&to=" + (to == null ? "" : to)));
        v.links.add(link("Profit and loss CSV", base + "/profit-loss.csv?from=" + (from == null
                ? "" : from) + "&to=" + (to == null ? "" : to)));
        List<TaxReportRowDto> rows = taxes.report(me(), storeId, from, to);
        String currency = admin.currency(storeId);
        v.rowsTitle = "Tax by region";
        v.rowsEmpty = "No orders in that period.";
        v.headA = "Region";
        v.headB = "Orders";
        v.headC = "Sales";
        v.headD = "Tax";
        for (int iter = 0; iter < rows.size(); iter++) {
            TaxReportRowDto r = rows.get(iter);
            v.rows.add(row(r.country() + (r.region().length() > 0 ? "-" + r.region() : ""),
                    r.orders() + (r.exemptOrders() > 0 ? " (" + r.exemptOrders() + " exempt)" : ""),
                    Money.format(r.salesCents(), currency), Money.format(r.taxCents(), currency)));
        }
    }

    /// The report's last day: `to`, or today when the page names none.
    private static String periodEnd(String to) {
        return Days.isIso(to) ? to : Days.iso(System.currentTimeMillis());
    }

    /// The report's first day: `from`, or thirty days before its end.
    private static String periodStart(String from, String to) {
        if (Days.isIso(from)) {
            return from;
        }
        return Days.iso(TaxExemptions.dayStart(periodEnd(to)) - 30 * Days.MILLIS);
    }

    String reportCsv(String storeId, String from, String to) throws IOException {
        from = periodStart(from, to);
        to = periodEnd(to);
        List<TaxReportRowDto> rows = taxes.report(me(), storeId, from, to);
        StringBuilder out = new StringBuilder("country,region,orders,exempt_orders,sales,tax\r\n");
        for (int iter = 0; iter < rows.size(); iter++) {
            TaxReportRowDto r = rows.get(iter);
            // Region is what the shopper typed: quoted and formula-guarded.
            out.append(com.codenameone.examples.bazaar.web.table.Grid.csvCell(r.country())).append(',')
                    .append(com.codenameone.examples.bazaar.web.table.Grid.csvCell(r.region())).append(',')
                    .append(r.orders()).append(',').append(r.exemptOrders()).append(',')
                    .append(Money.plain(r.salesCents())).append(',')
                    .append(Money.plain(r.taxCents())).append("\r\n");
        }
        return out.toString();
    }

    /// The profit and loss statement for the same period as the tax report, as CSV.
    String profitLossCsv(String storeId, String from, String to) throws IOException {
        return com.codenameone.examples.bazaar.ops.Reports.profitLossCsv(
                reports.profitLoss(me(), storeId, periodStart(from, to), periodEnd(to)));
    }

    void addExemption(String storeId, HttpServer.Request request) throws IOException {
        String e = f(request, "expires");
        if (e.length() > 0 && !Days.isIso(e)) {
            throw new ResponseStatusException(400, "Enter the date as YYYY-MM-DD");
        }
        long expires = e.length() == 0 ? 0 : TaxExemptions.dayStart(e);
        taxes.add(me(), storeId, new TaxExemptionDto(null, f(request, "username"),
                f(request, "certificateId"), list(f(request, "regions")), expires));
    }

    void removeExemption(String storeId, String id) throws IOException {
        taxes.remove(me(), storeId, id);
    }

    // ================================================================== risk

    void risks(String storeId, MarketingView v) throws IOException {
        String base = "/admin/" + storeId + "/risk/";
        String currency = admin.currency(storeId);
        List<OrderRiskDto> all = fraud.list(me(), storeId, false);
        v.rowsTitle = "Scored orders";
        v.rowsEmpty = "No orders scored yet.";
        v.headA = "Order";
        v.headB = "Why";
        v.headC = "Risk";
        v.headD = "Total";
        v.cardsTitle = "Held for review";
        for (int iter = 0; iter < all.size(); iter++) {
            OrderRiskDto r = all.get(iter);
            AdminRow row = row("#" + r.orderNumber(), join(r.reasons()), r.level() + " ("
                    + r.score() + "), " + r.status(), Money.format(r.totalCents(), currency));
            row.url = "/admin/" + storeId + "/orders/" + r.orderId();
            v.rows.add(row);
            if (Fraud.HELD.equals(r.status())) {
                ActionCard k = new ActionCard();
                k.title = "Order #" + r.orderNumber() + ", score " + r.score();
                k.detail = join(r.reasons());
                k.button("Approve", base + r.orderId() + "/approve", false);
                k.button("Cancel order", base + r.orderId() + "/cancel", true);
                v.cards.add(k);
            }
        }
    }

    void approve(String storeId, String orderId) throws IOException {
        fraud.approve(me(), storeId, orderId);
    }

    void cancel(String storeId, String orderId) throws IOException {
        fraud.cancel(me(), storeId, orderId);
    }

    // ============================================================== disputes

    void disputes(String storeId, MarketingView v) throws IOException {
        String base = "/admin/" + storeId + "/disputes/";
        String currency = admin.currency(storeId);
        List<DisputeDto> all = disputes.list(me(), storeId);
        AdminRow held = new AdminRow();
        held.title = "Funds held";
        held.detail = Money.format(disputes.heldCents(storeId), currency);
        v.facts.add(held);
        v.rowsTitle = "Disputes";
        v.rowsEmpty = "No disputes.";
        v.headA = "Order";
        v.headB = "Reason";
        v.headC = "Status";
        v.headD = "Amount";
        for (int iter = 0; iter < all.size(); iter++) {
            DisputeDto d = all.get(iter);
            AdminRow r = row("#" + d.orderNumber(), d.reason(), d.status()
                    + (Disputes.NEEDS_RESPONSE.equals(d.status()) ? ", respond by "
                    + Days.iso(d.dueAt()) : ""), Money.format(d.amountCents(), d.currency()));
            r.url = base + d.id();
            v.rows.add(r);
        }
    }

    String dispute(String storeId, String disputeId, MarketingView v) throws IOException {
        DisputeDto d = disputes.detail(me(), storeId, disputeId);
        String base = "/admin/" + storeId + "/disputes/" + d.id();
        v.back = "/admin/" + storeId + "/disputes";
        v.backLabel = "Disputes";
        AdminRow f1 = new AdminRow();
        f1.title = "Status";
        f1.detail = d.status();
        v.facts.add(f1);
        AdminRow f2 = new AdminRow();
        f2.title = "Respond by";
        f2.detail = Days.iso(d.dueAt());
        v.facts.add(f2);
        AdminRow f3 = new AdminRow();
        f3.title = "Amount held";
        f3.detail = Money.format(d.amountCents(), d.currency());
        v.facts.add(f3);
        List<DisputeEvidenceDto> ev = disputes.evidence(me(), storeId, disputeId);
        v.rowsTitle = "Evidence";
        v.rowsEmpty = "No evidence yet.";
        v.headA = "Kind";
        v.headB = "Evidence";
        v.headC = "By";
        v.headD = "";
        for (int iter = 0; iter < ev.size(); iter++) {
            v.rows.add(row(ev.get(iter).kind(), cut(ev.get(iter).body()),
                    ev.get(iter).createdBy(), ""));
        }
        if (Disputes.NEEDS_RESPONSE.equals(d.status())) {
            FormView text = new FormView("evidence", "Add evidence", base + "/evidence",
                    "Add");
            text.add("body", "What happened", "textarea", "").required = true;
            v.forms.add(text);
            v.cardsTitle = "Respond";
            ActionCard order = new ActionCard();
            order.title = "Attach the order record";
            order.detail = "Lines, totals, delivery address and tracking.";
            order.button("Attach", base + "/evidence/order", false);
            v.cards.add(order);
            ActionCard submit = new ActionCard();
            submit.title = "Submit to the processor";
            submit.button("Submit", base + "/submit", false);
            v.cards.add(submit);
            ActionCard accept = new ActionCard();
            accept.title = "Accept the dispute";
            accept.detail = "The buyer keeps the money.";
            accept.button("Accept", base + "/accept", true);
            v.cards.add(accept);
        }
        return "Dispute on order #" + d.orderNumber();
    }

    void addEvidence(String storeId, String disputeId, String kind, HttpServer.Request request)
            throws IOException {
        disputes.addEvidence(me(), storeId, disputeId, new DisputeEvidenceDto(null, kind,
                request == null ? "" : f(request, "body"), null, 0));
    }

    void submit(String storeId, String disputeId) throws IOException {
        disputes.submit(me(), storeId, disputeId);
    }

    void accept(String storeId, String disputeId) throws IOException {
        disputes.accept(me(), storeId, disputeId);
    }
}
