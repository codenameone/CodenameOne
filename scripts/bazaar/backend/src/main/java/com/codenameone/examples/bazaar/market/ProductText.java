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
package com.codenameone.examples.bazaar.market;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.api.CollectionDto;
import com.codenameone.examples.bazaar.api.OptionDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.ProductPhotoDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.web.StoreText;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Product and collection text as a shopper reads it: the store's
/// translations ([Translations]) laid over the originals, field by field.
///
/// Only what a shopper is shown goes through here -- the storefront, the
/// `/api/shop` calls, cart and order lines on the way to the screen or the
/// mail. The merchant's catalog calls never do: an editor that loaded a
/// translated title would save it back as the original.
///
/// An option's values translate by position ([Translations#OPTION]) and
/// only while the original has as many values as the translation, so a
/// value added after translating shows untranslated rather than misplaced.
/// A variant's choices follow its options; its title is translated only when
/// it is the choices joined (`S / Red`), never a title the merchant typed.
@Component
public class ProductText {
    private final Translations translations;

    public ProductText(Translations translations) {
        this.translations = translations;
    }

    /// The locale an API caller reads products in, among those the store has
    /// translations in ([Translations#pick]): its `?lang=`, else the `lang`
    /// cookie, else its `Accept-Language` (what the app sends). Null when the
    /// store has none of them, or no request is being served.
    public String shopperLocale(String storeId) throws IOException {
        SecurityExchange ex = SecurityExchange.current();
        HttpServer.Request request = ex == null ? null : ex.getRequest();
        if (request == null) {
            return null;
        }
        List<String> wanted = new ArrayList<String>();
        String asked;
        try {
            asked = Locales.orNull(request.param("lang"));
        } catch (RuntimeException malformed) {
            asked = null;
        }
        if (asked == null) {
            asked = StoreText.cookie(request);
        }
        if (asked != null) {
            wanted.add(asked);
        }
        wanted.addAll(Locales.accepted(request.getHeader("Accept-Language")));
        return translations.pick(storeId, wanted);
    }

    /// Product id to its title in `locale`, for the products that have one.
    public Map<String, String> titles(String storeId, String locale) throws IOException {
        Map<String, String> out = new HashMap<String, String>();
        if (!translations.has(storeId, Translations.PRODUCT)) {
            return out;
        }
        Map<String, Map<String, String>> all = translations.allOf(storeId, Translations.PRODUCT,
                locale);
        for (Map.Entry<String, Map<String, String>> e : all.entrySet()) {
            String t = e.getValue().get("title");
            if (t != null && t.length() > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    /// Collection id to its translated fields in `locale`.
    public Map<String, Map<String, String>> collections(String storeId, String locale)
            throws IOException {
        return translations.allOf(storeId, Translations.COLLECTION, locale);
    }

    /// `c` with the translated title and description in `tr` (null keeps it).
    public static CollectionDto localize(CollectionDto c, Map<String, String> tr) {
        if (tr == null || tr.isEmpty()) {
            return c;
        }
        return new CollectionDto(c.id(), c.handle(), text(tr, "title", c.title()),
                text(tr, "description", c.description()), c.photoId(), c.ruleText(),
                c.position(), c.published(), c.productIds(), c.productCount());
    }

    /// `p` in `locale` (null or empty keeps the originals): its title,
    /// description and SEO text, its options' names and values, its
    /// variants' choices and its pictures' alternative text.
    public ProductDto localize(ProductDto p, String locale) throws IOException {
        if (p == null || locale == null || locale.length() == 0) {
            return p;
        }
        Map<String, String> tr = translations.fields(p.storeId(), Translations.PRODUCT, p.id(),
                locale);
        Map<String, String> op = translations.fields(p.storeId(), Translations.OPTION, p.id(),
                locale);
        Map<String, Map<String, String>> alts = p.photos() == null || p.photos().isEmpty()
                || !translations.has(p.storeId(), Translations.IMAGE)
                ? new HashMap<String, Map<String, String>>()
                : translations.allOf(p.storeId(), Translations.IMAGE, locale);
        if (tr.isEmpty() && op.isEmpty() && alts.isEmpty()) {
            return p;
        }
        List<OptionDto> options = new ArrayList<OptionDto>();
        List<Map<String, String>> values = valueMaps(p.options(), op);
        for (int iter = 0; p.options() != null && iter < p.options().size(); iter++) {
            OptionDto o = p.options().get(iter);
            List<String> vs = new ArrayList<String>();
            for (int v = 0; o.values() != null && v < o.values().size(); v++) {
                vs.add(text(values.get(iter), o.values().get(v), o.values().get(v)));
            }
            options.add(new OptionDto(o.id(), text(op, "name" + (iter + 1), o.name()), vs,
                    o.position()));
        }
        List<VariantDto> variants = new ArrayList<VariantDto>();
        for (int iter = 0; p.variants() != null && iter < p.variants().size(); iter++) {
            variants.add(variant(p.variants().get(iter), values));
        }
        List<ProductPhotoDto> photos = new ArrayList<ProductPhotoDto>();
        for (int iter = 0; p.photos() != null && iter < p.photos().size(); iter++) {
            ProductPhotoDto ph = p.photos().get(iter);
            photos.add(new ProductPhotoDto(ph.id(), ph.photoId(), text(alts.get(ph.id()), "alt",
                    ph.alt()), ph.position(), ph.kind(), ph.url()));
        }
        String title = text(tr, "title", p.title());
        String description = text(tr, "description", p.description());
        return new ProductDto(p.id(), p.storeId(), p.handle(), title, description, p.vendor(),
                p.productType(), p.tags(), p.status(), text(tr, "seoTitle", p.seoTitle()),
                text(tr, "seoDescription", p.seoDescription()), p.photoId(), p.rating(),
                p.ratingCount(), options, variants, photos, p.collectionIds(), p.categoryId());
    }

    /// Per option position, original value to translated value; an option
    /// whose translation has a different number of values maps nothing.
    static List<Map<String, String>> valueMaps(List<OptionDto> options, Map<String, String> op) {
        List<Map<String, String>> out = new ArrayList<Map<String, String>>();
        for (int iter = 0; options != null && iter < options.size(); iter++) {
            Map<String, String> m = new HashMap<String, String>();
            List<String> orig = options.get(iter).values();
            List<String> tr = Catalog.tags(op.get("values" + (iter + 1)));
            if (orig != null && tr.size() == orig.size()) {
                for (int v = 0; v < orig.size(); v++) {
                    m.put(orig.get(v), tr.get(v));
                }
            }
            out.add(m);
        }
        return out;
    }

    private static VariantDto variant(VariantDto d, List<Map<String, String>> values) {
        String o1 = choice(values, 0, d.option1());
        String o2 = choice(values, 1, d.option2());
        String o3 = choice(values, 2, d.option3());
        String title = d.title();
        if (title != null && title.equals(join(d.option1(), d.option2(), d.option3()))) {
            title = join(o1, o2, o3);
        }
        return new VariantDto(d.id(), d.productId(), title, o1, o2, o3, d.sku(), d.barcode(),
                d.priceCents(), d.compareAtCents(), d.costCents(), d.weightGrams(), d.taxable(),
                d.trackInventory(), d.allowBackorder(), d.position(), d.available(), d.stock(),
                d.digital(), d.hsCode(), d.originCountry(), d.unitQuantity(), d.unitName(),
                d.unitReference(), d.unitReferenceName());
    }

    private static String choice(List<Map<String, String>> values, int at, String original) {
        if (original == null || at >= values.size()) {
            return original;
        }
        return text(values.get(at), original, original);
    }

    /// The choices joined as a variant's derived title is.
    static String join(String a, String b, String c) {
        StringBuilder out = new StringBuilder();
        String[] parts = {a, b, c};
        for (int iter = 0; iter < parts.length; iter++) {
            if (parts[iter] != null && parts[iter].length() > 0) {
                if (out.length() > 0) {
                    out.append(" / ");
                }
                out.append(parts[iter]);
            }
        }
        return out.length() == 0 ? "Default" : out.toString();
    }

    /// A cart or order line's product title and variant title in `locale`,
    /// for the screen or the mail; the line keeps what it recorded (the
    /// original at the time), which is also the fallback.
    public String[] line(String storeId, String productId, String title, String variantTitle,
            String locale) throws IOException {
        String[] out = {title, variantTitle};
        if (locale == null || locale.length() == 0 || productId == null
                || productId.length() == 0) {
            return out;
        }
        Map<String, String> tr = translations.fields(storeId, Translations.PRODUCT, productId,
                locale);
        out[0] = text(tr, "title", title);
        if (variantTitle != null && variantTitle.length() > 0) {
            Map<String, String> op = translations.fields(storeId, Translations.OPTION, productId,
                    locale);
            if (!op.isEmpty()) {
                out[1] = variantTitle(storeId, productId, variantTitle, op);
            }
        }
        return out;
    }

    private String variantTitle(String storeId, String productId, String variantTitle,
            Map<String, String> op) throws IOException {
        Map<String, String> orig = translations.originals(storeId, Translations.OPTION, productId,
                null);
        List<OptionDto> options = new ArrayList<OptionDto>();
        for (int iter = 1; iter <= 3 && orig.containsKey("values" + iter); iter++) {
            options.add(new OptionDto("", "", Catalog.tags(orig.get("values" + iter)), iter));
        }
        List<Map<String, String>> values = valueMaps(options, op);
        // A derived title: each part is the choice of the option at its place.
        StringBuilder out = new StringBuilder();
        int start = 0;
        int at = 0;
        while (start <= variantTitle.length()) {
            int sep = variantTitle.indexOf(" / ", start);
            String part = sep < 0 ? variantTitle.substring(start) : variantTitle.substring(start,
                    sep);
            String shown = part;
            for (int iter = at; iter < values.size(); iter++) {
                if (values.get(iter).containsKey(part)) {
                    shown = values.get(iter).get(part);
                    at = iter + 1;
                    break;
                }
            }
            if (out.length() > 0) {
                out.append(" / ");
            }
            out.append(shown);
            if (sep < 0) {
                break;
            }
            start = sep + 3;
        }
        return out.toString();
    }

    /// The language of an order or a mail as a content locale the store
    /// translated into, or null: `de` finds the store's `de-CH` rows.
    public String locale(String storeId, String language) throws IOException {
        if (language == null || language.length() == 0) {
            return null;
        }
        List<String> wanted = new ArrayList<String>();
        wanted.add(language);
        return translations.pick(storeId, wanted);
    }

    static String text(Map<String, String> fields, String field, String original) {
        String v = fields == null ? null : fields.get(field);
        return v == null || v.length() == 0 ? original : v;
    }
}
