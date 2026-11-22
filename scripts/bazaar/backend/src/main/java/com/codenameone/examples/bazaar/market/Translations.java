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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.TranslationDto;
import com.codenameone.examples.bazaar.api.TranslationFieldDto;
import com.codenameone.examples.bazaar.domain.Collection;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Translation;
import com.codenameone.examples.bazaar.repository.CollectionRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.TranslationRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Translations of a store's product, collection and page fields, one row
/// per field and locale.
///
/// [#fields] answers the best text for a locale field by field: the exact
/// locale (`de-CH`), then its language (`de`), then nothing -- and the
/// caller keeps the original. A field never translated in `de-CH` but
/// translated in `de` is therefore German, not the original. Pages are the
/// store's policies page for now ([#PAGES]); the kind is open for more.
@Component
public class Translations {
    public static final String PRODUCT = "PRODUCT";
    public static final String COLLECTION = "COLLECTION";
    public static final String PAGE = "PAGE";
    /// A page or policy of the online store (`content.Pages`).
    public static final String CONTENT = "CONTENT";
    /// A blog post.
    public static final String POST = "POST";
    /// A navigation menu link's label (`content.Navigation`).
    public static final String MENU_LINK = "MENU_LINK";
    /// A metafield definition's name, shown to shoppers as a spec label.
    public static final String METAFIELD = "METAFIELD";
    /// A metaobject type's name.
    public static final String METAOBJECT_TYPE = "METAOBJECT_TYPE";
    /// A metaobject entry's title: what a shopper reads as the value of a
    /// reference metafield.
    public static final String METAOBJECT_ENTRY = "METAOBJECT_ENTRY";
    /// A marketing campaign's message: its subject (and A/B subject) and
    /// body, sent to each subscriber in their language
    /// (`marketing.Campaigns`). A message, not storefront text, so it never
    /// decides the language a storefront is shown in ([#pick]).
    public static final String CAMPAIGN = "CAMPAIGN";
    /// A product's options, by position: `name1`/`values1` is the first
    /// option's name and its values, comma separated in the original's
    /// order. Keyed by the product, because an option row is replaced
    /// whenever the product is saved; the stamp ([#stamp]) then tells the
    /// editor when the values changed under a translation.
    public static final String OPTION = "OPTION";
    /// A product picture's alternative text, keyed by the gallery row.
    public static final String IMAGE = "IMAGE";
    /// The value of a text metafield on a product, keyed by the value row.
    public static final String METAFIELD_VALUE = "METAFIELD_VALUE";
    /// `TranslationFieldDto.status`: never translated.
    public static final String MISSING = "MISSING";
    /// `TranslationFieldDto.status`: the original changed after it was translated.
    public static final String OUTDATED = "OUTDATED";
    /// `TranslationFieldDto.status`: translated from the current original.
    public static final String DONE = "DONE";
    /// The translatable fields of each kind.
    static final String[][] FIELDS = {
        {PRODUCT, "title", "description", "seoTitle", "seoDescription"},
        {OPTION, "name1", "values1", "name2", "values2", "name3", "values3"},
        {IMAGE, "alt"},
        {METAFIELD_VALUE, "value"},
        {COLLECTION, "title", "description"},
        {PAGE, "body"},
        {CONTENT, "title", "body"},
        {POST, "title", "excerpt", "body"},
        {MENU_LINK, "title"},
        {METAFIELD, "name"},
        {METAOBJECT_TYPE, "name"},
        {METAOBJECT_ENTRY, "title"},
        {CAMPAIGN, "subjectA", "subjectB", "body"},
    };
    /// The longest translated campaign subject, as the original's.
    static final int MAX_SUBJECT = 160;
    /// The pages a store has: the privacy and cookie policy.
    public static final String[] PAGES = {"privacy"};
    static final int MAX_BODY = 20000;

    private final TranslationRepository rows;
    private final ProductRepository products;
    private final CollectionRepository collections;
    private final StoreAccess access;
    private final com.codenameone.examples.bazaar.repository.ContentPageRepository pages;
    private final com.codenameone.examples.bazaar.repository.BlogPostRepository posts;

    private final com.codename1.backend.DataSource db;

    public Translations(TranslationRepository rows, ProductRepository products,
            CollectionRepository collections, StoreAccess access,
            com.codenameone.examples.bazaar.repository.ContentPageRepository pages,
            com.codenameone.examples.bazaar.repository.BlogPostRepository posts,
            com.codename1.backend.DataSource db) {
        this.db = db;
        this.pages = pages;
        this.posts = posts;
        this.rows = rows;
        this.products = products;
        this.collections = collections;
        this.access = access;
    }

    /// The field names of `kind`, or 400.
    public static List<String> fieldsOf(String kind) {
        for (int iter = 0; iter < FIELDS.length; iter++) {
            if (FIELDS[iter][0].equals(kind)) {
                List<String> out = new ArrayList<String>();
                for (int f = 1; f < FIELDS[iter].length; f++) {
                    out.add(FIELDS[iter][f]);
                }
                return out;
            }
        }
        throw new ResponseStatusException(400, "Translations are of a PRODUCT, OPTION, IMAGE, METAFIELD_VALUE, "
                + "COLLECTION, PAGE, CONTENT, POST, MENU_LINK, METAFIELD, METAOBJECT_TYPE, METAOBJECT_ENTRY or CAMPAIGN");
    }

    /// Every translation of one thing, for the editor.
    @Transactional(readOnly = true)
    public List<TranslationDto> of(String username, String storeId, String kind, String refId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        fieldsOf(kind);
        requireRef(storeId, kind, refId);
        List<Translation> rs = rows.findByStoreIdAndKindAndRefId(storeId, kind, refId);
        List<TranslationDto> out = new ArrayList<TranslationDto>();
        for (int iter = 0; iter < rs.size(); iter++) {
            Translation t = rs.get(iter);
            out.add(new TranslationDto(t.getKind(), t.getRefId(), t.getLocale(), t.getField(),
                    t.getBody()));
        }
        return out;
    }

    /// The locales a store has any translation in, sorted.
    @Transactional(readOnly = true)
    public List<String> locales(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<Translation> rs = rows.findByStoreIdOrderByLocaleAsc(storeId);
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < rs.size(); iter++) {
            String l = rs.get(iter).getLocale();
            if (!out.contains(l)) {
                out.add(l);
            }
        }
        return out;
    }

    /// Writes one translated field; an empty value removes it.
    @Transactional(rollbackFor = IOException.class)
    public List<TranslationDto> put(String username, String storeId, TranslationDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null) {
            throw new ResponseStatusException(400, "Say what to translate");
        }
        List<String> fields = fieldsOf(in.kind());
        if (in.field() == null || !fields.contains(in.field())) {
            throw new ResponseStatusException(400, "A " + in.kind() + " has no field " + in.field());
        }
        requireRef(storeId, in.kind(), in.refId());
        String locale = Locales.normalize(in.locale());
        if (CAMPAIGN.equals(in.kind())) {
            // A message is sent by the reader's language, never a region.
            locale = Locales.language(locale);
        }
        String value = in.value() == null ? "" : in.value().trim();
        if ("body".equals(in.field()) && (CONTENT.equals(in.kind()) || POST.equals(in.kind()))) {
            // Rich text, drawn on the storefront: held to the same allow-list
            // as the original (content.RichText), so a translation cannot
            // carry what the original could not.
            value = com.codenameone.examples.bazaar.content.RichText.sanitize(value).trim();
        }
        if (CAMPAIGN.equals(in.kind()) && in.field().startsWith("subject")
                && value.length() > MAX_SUBJECT) {
            throw new ResponseStatusException(400, "A subject is at most " + MAX_SUBJECT
                    + " characters");
        }
        if (CAMPAIGN.equals(in.kind()) && "body".equals(in.field()) && value.length() > 480) {
            Map row = db.queryOne("SELECT channel FROM campaign WHERE id = ?",
                    new Object[] {in.refId()});
            if (row != null && "SMS".equals(com.codenameone.examples.bazaar.Rows.text(
                    row.get("channel")))) {
                throw new ResponseStatusException(400, "A text message is at most 480 characters");
            }
        }
        if (value.length() > MAX_BODY) {
            throw new ResponseStatusException(400, "A translation is at most " + MAX_BODY
                    + " characters");
        }
        Map<String, String> originals = originals(storeId, in.kind(), in.refId(), null);
        String original = originals.get(in.field());
        if (OPTION.equals(in.kind()) && value.length() > 0) {
            value = optionText(in.field(), value, original);
        }
        if (IMAGE.equals(in.kind()) && value.length() > 200) {
            throw new ResponseStatusException(400, "Alternative text is at most 200 characters");
        }
        Translation t = rows.findByStoreIdAndKindAndRefIdAndLocaleAndField(storeId, in.kind(),
                in.refId(), locale, in.field());
        if (value.length() == 0) {
            if (t != null) {
                rows.delete(t);
            }
        } else if (t == null) {
            t = new Translation();
            t.setId(Ids.next());
            t.setStoreId(storeId);
            t.setKind(in.kind());
            t.setRefId(in.refId());
            t.setLocale(locale);
            t.setField(in.field());
            t.setBody(value);
            t.setUpdatedAt(Instant.now());
            t.setSourceHash(stamp(original));
            rows.insert(t);
        } else {
            t.setBody(value);
            t.setUpdatedAt(Instant.now());
            t.setSourceHash(stamp(original));
            rows.save(t);
        }
        return of(username, storeId, in.kind(), in.refId());
    }

    /// The original text of each field of one thing, for the editor's
    /// left-hand column.
    @Transactional(readOnly = true)
    public Map<String, String> originals(String storeId, String kind, String refId,
            String privacyText) throws IOException {
        Map<String, String> out = new HashMap<String, String>();
        if (PRODUCT.equals(kind)) {
            Product p = products.findById(refId);
            out.put("title", p.getTitle());
            out.put("description", p.getDescription());
            out.put("seoTitle", p.getSeoTitle());
            out.put("seoDescription", p.getSeoDescription());
        } else if (COLLECTION.equals(kind)) {
            Collection c = collections.findById(refId);
            out.put("title", c.getTitle());
            out.put("description", c.getDescription());
        } else if (CONTENT.equals(kind)) {
            com.codenameone.examples.bazaar.domain.ContentPage c = pages.findById(refId);
            out.put("title", c.getTitle());
            out.put("body", c.getBody());
        } else if (POST.equals(kind)) {
            com.codenameone.examples.bazaar.domain.BlogPost b = posts.findById(refId);
            out.put("title", b.getTitle());
            out.put("excerpt", b.getExcerpt());
            out.put("body", b.getBody());
        } else if (OPTION.equals(kind)) {
            List rs = db.query("SELECT name, option_values FROM product_option WHERE product_id = ? "
                    + "ORDER BY position", new Object[] {refId});
            for (int iter = 0; iter < rs.size() && iter < 3; iter++) {
                Map r = (Map) rs.get(iter);
                out.put("name" + (iter + 1), com.codenameone.examples.bazaar.Rows.text(
                        r.get("name")));
                out.put("values" + (iter + 1), listText(com.codenameone.examples.bazaar.catalog
                        .Catalog.tags(com.codenameone.examples.bazaar.Rows.text(
                        r.get("option_values")))));
            }
        } else if (IMAGE.equals(kind)) {
            Map row = db.queryOne("SELECT alt FROM product_photo WHERE id = ?",
                    new Object[] {refId});
            out.put("alt", row == null ? "" : com.codenameone.examples.bazaar.Rows.text(
                    row.get("alt")));
        } else if (METAFIELD_VALUE.equals(kind)) {
            Map row = db.queryOne("SELECT text_value FROM metafield_value WHERE id = ?",
                    new Object[] {refId});
            out.put("value", row == null ? "" : com.codenameone.examples.bazaar.Rows.text(
                    row.get("text_value")));
        } else if (CAMPAIGN.equals(kind)) {
            Map row = db.queryOne("SELECT subject_a, subject_b, body FROM campaign WHERE id = ?",
                    new Object[] {refId});
            out.put("subjectA", row == null ? "" : com.codenameone.examples.bazaar.Rows.text(
                    row.get("subject_a")));
            out.put("subjectB", row == null ? "" : com.codenameone.examples.bazaar.Rows.text(
                    row.get("subject_b")));
            out.put("body", row == null ? "" : com.codenameone.examples.bazaar.Rows.text(
                    row.get("body")));
        } else if (NAMED.containsKey(kind)) {
            String[] t = NAMED.get(kind);
            Map row = db.queryOne("SELECT " + t[1] + " AS v FROM " + t[0] + " WHERE id = ?",
                    new Object[] {refId});
            out.put(fieldsOf(kind).get(0), row == null ? "" : com.codenameone.examples.bazaar.Rows
                    .text(row.get("v")));
        } else {
            out.put("body", privacyText == null ? "" : privacyText);
        }
        return out;
    }

    /// The table and column holding the original of each single-field kind.
    private static final Map<String, String[]> NAMED = new HashMap<String, String[]>();

    static {
        NAMED.put(MENU_LINK, new String[] {"menu_link", "title"});
        NAMED.put(METAFIELD, new String[] {"metafield_def", "name"});
        NAMED.put(METAOBJECT_TYPE, new String[] {"metaobject_type", "name"});
        NAMED.put(METAOBJECT_ENTRY, new String[] {"metaobject_entry", "title"});
    }

    /// Every translation of every thing of `kind` in a store for `locale`,
    /// with the language fallback of [#fields]: ref id to field to text.
    /// One query, for lists such as a menu of many links.
    @Transactional(readOnly = true)
    public Map<String, Map<String, String>> allOf(String storeId, String kind, String locale)
            throws IOException {
        Map<String, Map<String, String>> out = new HashMap<String, Map<String, String>>();
        if (locale == null || locale.length() == 0) {
            return out;
        }
        String language = Locales.language(locale);
        List rs = db.query("SELECT ref_id, locale, field, body FROM translation WHERE store_id = ? "
                + "AND kind = ? AND (locale = ? OR locale = ?)", new Object[] {storeId, kind, locale,
                    language});
        for (int pass = 0; pass < 2; pass++) {
            for (int iter = 0; iter < rs.size(); iter++) {
                Map r = (Map) rs.get(iter);
                String l = com.codenameone.examples.bazaar.Rows.text(r.get("locale"));
                if (!l.equals(pass == 0 ? language : locale)) {
                    continue;
                }
                String ref = com.codenameone.examples.bazaar.Rows.text(r.get("ref_id"));
                Map<String, String> m = out.get(ref);
                if (m == null) {
                    m = new HashMap<String, String>();
                    out.put(ref, m);
                }
                m.put(com.codenameone.examples.bazaar.Rows.text(r.get("field")),
                        com.codenameone.examples.bazaar.Rows.text(r.get("body")));
            }
        }
        return out;
    }

    /// The languages (`de`, never `de-CH`) a store translated storefront
    /// text into, sorted; a campaign message is not storefront text.
    @Transactional(readOnly = true)
    public List<String> languages(String storeId) throws IOException {
        List<Translation> rs = rows.findByStoreIdOrderByLocaleAsc(storeId);
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < rs.size(); iter++) {
            String l = Locales.language(rs.get(iter).getLocale());
            if (!CAMPAIGN.equals(rs.get(iter).getKind()) && !out.contains(l)) {
                out.add(l);
            }
        }
        return out;
    }

    /// Whether a store has any translation of `kind`.
    @Transactional(readOnly = true)
    public boolean has(String storeId, String kind) throws IOException {
        return db.queryOne("SELECT id FROM translation WHERE store_id = ? AND kind = ? LIMIT 1",
                new Object[] {storeId, kind}) != null;
    }

    // ------------------------------------------------------------- the sheet

    /// A stamp of an original, kept on each translation of it so the editor
    /// can tell when the original changed afterwards: the first 16 hex digits
    /// of its SHA-256.
    public static String stamp(String original) throws IOException {
        return Ids.sha256(original == null ? "" : original).substring(0, 16);
    }

    /// `MISSING`, `OUTDATED` or `DONE` for translation `t` (null when there is
    /// none) of `original`. A translation without a stamp predates stamps and
    /// counts as done.
    public static String status(Translation t, String original) throws IOException {
        if (t == null) {
            return MISSING;
        }
        String had = t.getSourceHash();
        return had.length() > 0 && !had.equals(stamp(original)) ? OUTDATED : DONE;
    }

    static String listText(List<String> parts) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < parts.size(); iter++) {
            if (iter > 0) {
                out.append(", ");
            }
            out.append(parts.get(iter));
        }
        return out.toString();
    }

    /// An option's translated name or values, checked: values come as many as
    /// the original has, in its order, so each lines up with the one it
    /// translates.
    static String optionText(String field, String value, String original) {
        if (original == null) {
            throw new ResponseStatusException(404, "The product has no such option");
        }
        if (field.startsWith("name")) {
            if (value.length() > 60 || value.indexOf(',') >= 0) {
                throw new ResponseStatusException(400, "An option's name is at most 60 "
                        + "characters, with no commas");
            }
            return value;
        }
        List<String> want = com.codenameone.examples.bazaar.catalog.Catalog.tags(original);
        List<String> got = com.codenameone.examples.bazaar.catalog.Catalog.tags(value);
        if (got.size() != want.size()) {
            throw new ResponseStatusException(400, "Give " + want.size() + " values, comma "
                    + "separated, in the order of the original");
        }
        String text = listText(got);
        if (text.length() > 500) {
            throw new ResponseStatusException(400, "An option's values are at most 500 "
                    + "characters");
        }
        return text;
    }

    /// Every translatable field of one thing beside its translation into
    /// `locale` and its status ([#status]). A product's sheet carries its
    /// options, its pictures' alternative text and its text metafields too.
    @Transactional(readOnly = true)
    public List<TranslationFieldDto> sheet(String username, String storeId, String kind,
            String refId, String locale) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        fieldsOf(kind);
        requireRef(storeId, kind, refId);
        String loc = Locales.normalize(locale);
        List<TranslationFieldDto> out = new ArrayList<TranslationFieldDto>();
        if (PRODUCT.equals(kind)) {
            productSheet(out, storeId, refId, loc);
        } else {
            addSheet(out, storeId, kind, refId, loc, "");
        }
        return out;
    }

    private void productSheet(List<TranslationFieldDto> out, String storeId, String productId,
            String locale) throws IOException {
        addSheet(out, storeId, PRODUCT, productId, locale, "");
        addSheet(out, storeId, OPTION, productId, locale, "");
        List rs = db.query("SELECT id FROM product_photo WHERE product_id = ? ORDER BY position",
                new Object[] {productId});
        for (int iter = 0; iter < rs.size(); iter++) {
            addSheet(out, storeId, IMAGE, com.codenameone.examples.bazaar.Rows.text(
                    ((Map) rs.get(iter)).get("id")), locale, "Picture " + (iter + 1) + " ");
        }
        List ms = db.query("SELECT v.id AS id, d.name AS name FROM metafield_value v "
                + "JOIN metafield_def d ON d.id = v.def_id WHERE v.store_id = ? AND v.owner_id = ? "
                + "AND d.field_type = 'text' ORDER BY d.position", new Object[] {storeId,
                    productId});
        for (int iter = 0; iter < ms.size(); iter++) {
            Map m = (Map) ms.get(iter);
            addSheet(out, storeId, METAFIELD_VALUE, com.codenameone.examples.bazaar.Rows.text(
                    m.get("id")), locale, com.codenameone.examples.bazaar.Rows.text(m.get("name"))
                    + " ");
        }
    }

    private void addSheet(List<TranslationFieldDto> out, String storeId, String kind,
            String refId, String locale, String labelPrefix) throws IOException {
        Map<String, String> orig = originals(storeId, kind, refId, null);
        List<Translation> done = rows.findByStoreIdAndKindAndRefId(storeId, kind, refId);
        List<String> fields = fieldsOf(kind);
        for (int iter = 0; iter < fields.size(); iter++) {
            String field = fields.get(iter);
            if (!orig.containsKey(field)) {
                // An option the product does not have.
                continue;
            }
            String o = orig.get(field) == null ? "" : orig.get(field);
            Translation t = null;
            for (int d = 0; d < done.size(); d++) {
                if (done.get(d).getField().equals(field) && done.get(d).getLocale().equals(locale)) {
                    t = done.get(d);
                }
            }
            out.add(new TranslationFieldDto(kind, refId, field, labelPrefix + label(field), o,
                    t == null ? "" : t.getBody(), status(t, o)));
        }
    }

    /// What the editor calls a field.
    static String label(String field) {
        String[][] names = {{"title", "Title"}, {"description", "Description"},
            {"seoTitle", "SEO title"}, {"seoDescription", "SEO description"},
            {"name1", "Option 1 name"}, {"values1", "Option 1 values"},
            {"name2", "Option 2 name"}, {"values2", "Option 2 values"},
            {"name3", "Option 3 name"}, {"values3", "Option 3 values"},
            {"alt", "alt text"}, {"value", "value"}, {"body", "Body"}, {"excerpt", "Excerpt"},
            {"name", "Name"}, {"subjectA", "Subject"}, {"subjectB", "Subject B"}};
        for (int iter = 0; iter < names.length; iter++) {
            if (names[iter][0].equals(field)) {
                return names[iter][1];
            }
        }
        return field;
    }

    // ------------------------------------------------------------------- CSV

    /// The columns of [#exportCsv], which [#importCsv] reads back by name.
    static final String[] CSV_COLUMNS = {"kind", "ref_id", "field", "locale", "status", "label",
        "original", "translation"};
    /// The most an import reads.
    static final int MAX_CSV = 2 * 1024 * 1024;
    static final int MAX_CSV_ROWS = 20000;

    /// Every product (with its options, pictures and text metafields) and
    /// collection of a store as CSV, one row per field, for a translator to
    /// fill the `translation` column and send back through [#importCsv].
    @Transactional(readOnly = true)
    public String exportCsv(String username, String storeId, String locale) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        String loc = Locales.normalize(locale);
        List<TranslationFieldDto> all = new ArrayList<TranslationFieldDto>();
        List<Product> ps = products.findByStoreIdOrderByTitleAsc(storeId);
        for (int iter = 0; iter < ps.size(); iter++) {
            productSheet(all, storeId, ps.get(iter).getId(), loc);
        }
        List<Collection> cs = collections.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < cs.size(); iter++) {
            addSheet(all, storeId, COLLECTION, cs.get(iter).getId(), loc, "");
        }
        StringBuilder sb = new StringBuilder();
        csvLine(sb, CSV_COLUMNS);
        for (int iter = 0; iter < all.size(); iter++) {
            TranslationFieldDto f = all.get(iter);
            csvLine(sb, new String[] {f.kind(), f.refId(), f.field(), loc, f.status(), f.label(),
                f.original(), f.value()});
        }
        return sb.toString();
    }

    /// What [#importCsv] did: how many fields it wrote and, per refused
    /// row, why.
    public static final class ImportResult {
        public int saved;
        public int unchanged;
        public final List<String> errors = new ArrayList<String>();
    }

    /// Reads a translation CSV back, row by row through [#put] -- so each row
    /// is held to the same checks as the editor, and a bad row is reported
    /// and skipped rather than failing the rest. A row whose translation is
    /// empty, or the same as the one stored, is left alone: an exported sheet
    /// sent back untouched changes nothing, and does not mark an outdated
    /// translation current.
    public ImportResult importCsv(String username, String storeId, String text)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (text == null || text.trim().length() == 0) {
            throw new ResponseStatusException(400, "Paste or upload the CSV");
        }
        if (text.length() > MAX_CSV) {
            throw new ResponseStatusException(400, "A translation CSV is at most 2 MB");
        }
        List<List<String>> table = parseCsv(text);
        if (table.isEmpty()) {
            throw new ResponseStatusException(400, "The CSV has no header");
        }
        if (table.size() > MAX_CSV_ROWS + 1) {
            throw new ResponseStatusException(400, "A translation CSV is at most " + MAX_CSV_ROWS
                    + " rows");
        }
        List<String> head = table.get(0);
        int kind = head.indexOf("kind");
        int ref = head.indexOf("ref_id");
        int field = head.indexOf("field");
        int locale = head.indexOf("locale");
        int value = head.indexOf("translation");
        if (kind < 0 || ref < 0 || field < 0 || locale < 0 || value < 0) {
            throw new ResponseStatusException(400, "The CSV needs the columns kind, ref_id, field, "
                    + "locale and translation");
        }
        ImportResult result = new ImportResult();
        for (int iter = 1; iter < table.size(); iter++) {
            List<String> row = table.get(iter);
            String v = cell(row, value).trim();
            if (v.length() == 0) {
                continue;
            }
            String k = cell(row, kind);
            String r = cell(row, ref);
            String f = cell(row, field);
            try {
                String loc = Locales.normalize(cell(row, locale));
                fieldsOf(k);
                requireRef(storeId, k, r);
                Translation had = rows.findByStoreIdAndKindAndRefIdAndLocaleAndField(storeId, k, r,
                        loc, f);
                if (had != null && had.getBody().equals(v)) {
                    result.unchanged++;
                    continue;
                }
                put(username, storeId, new TranslationDto(k, r, loc, f, v));
                result.saved++;
            } catch (ResponseStatusException bad) {
                if (result.errors.size() < 50) {
                    result.errors.add("Row " + (iter + 1) + ": " + bad.getMessage());
                }
            }
        }
        return result;
    }

    private static String cell(List<String> row, int at) {
        String c = at < row.size() && row.get(at) != null ? row.get(at) : "";
        // Undo the export's guard against a spreadsheet reading a formula.
        // A plain number is never guarded, so "'-5" is the author's own text.
        if (c.length() > 1 && c.charAt(0) == '\'' && "=+-@\t\r".indexOf(c.charAt(1)) >= 0
                && !com.codenameone.examples.bazaar.web.table.Grid.plainNumber(c.substring(1))) {
            c = c.substring(1);
        }
        return c;
    }

    static void csvLine(StringBuilder sb, String[] cells) {
        for (int iter = 0; iter < cells.length; iter++) {
            String c = cells[iter] == null ? "" : cells[iter];
            // A leading = + - @, tab or CR would be read as a formula by a spreadsheet.
            if (c.length() > 0 && "=+-@\t\r".indexOf(c.charAt(0)) >= 0
                    && !com.codenameone.examples.bazaar.web.table.Grid.plainNumber(c)) {
                c = "'" + c;
            }
            sb.append(iter == 0 ? "" : ",").append('"');
            for (int k = 0; k < c.length(); k++) {
                char ch = c.charAt(k);
                if (ch == '"') {
                    sb.append('"');
                }
                sb.append(ch);
            }
            sb.append('"');
        }
        sb.append("\r\n");
    }

    /// RFC 4180 CSV: quoted cells may hold commas, quotes (doubled) and line
    /// breaks; a byte order mark at the start is skipped; blank lines are
    /// dropped.
    static List<List<String>> parseCsv(String text) {
        List<List<String>> out = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        int start = text.length() > 0 && text.charAt(0) == (char) 0xfeff ? 1 : 0;
        for (int iter = start; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (quoted) {
                if (c == '"') {
                    if (iter + 1 < text.length() && text.charAt(iter + 1) == '"') {
                        cell.append('"');
                        iter++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
                any = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
                any = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && iter + 1 < text.length() && text.charAt(iter + 1) == '\n') {
                    iter++;
                }
                if (any || cell.length() > 0) {
                    row.add(cell.toString());
                    out.add(row);
                }
                row = new ArrayList<String>();
                cell.setLength(0);
                any = false;
            } else {
                cell.append(c);
                any = true;
            }
        }
        if (any || cell.length() > 0) {
            row.add(cell.toString());
            out.add(row);
        }
        return out;
    }

    /// Removes every translation of one thing; called when it is deleted,

    /// so [#pick] never chooses a locale that only orphans are in.
    @Transactional(rollbackFor = IOException.class)
    public void forget(String storeId, String kind, String refId) throws IOException {
        db.execute("DELETE FROM translation WHERE store_id = ? AND kind = ? AND ref_id = ?",
                new Object[] {storeId, kind, refId});
    }

    private void requireRef(String storeId, String kind, String refId) throws IOException {
        boolean ok;
        if (refId == null || refId.length() == 0) {
            ok = false;
        } else if (PRODUCT.equals(kind) || OPTION.equals(kind)) {
            Product p = refId.length() > 32 ? null : products.findById(refId);
            ok = p != null && storeId.equals(p.getStoreId());
        } else if (IMAGE.equals(kind)) {
            // A gallery row has no store of its own: it is the product's.
            Map row = refId.length() > 32 ? null : db.queryOne("SELECT p.store_id AS store_id "
                    + "FROM product_photo g JOIN product p ON p.id = g.product_id WHERE g.id = ?",
                    new Object[] {refId});
            ok = row != null && storeId.equals(com.codenameone.examples.bazaar.Rows.text(
                    row.get("store_id")));
        } else if (METAFIELD_VALUE.equals(kind)) {
            // Only text is words; a number, a date or a reference is not.
            Map row = refId.length() > 32 ? null : db.queryOne("SELECT v.store_id AS store_id "
                    + "FROM metafield_value v JOIN metafield_def d ON d.id = v.def_id "
                    + "WHERE v.id = ? AND d.field_type = 'text'", new Object[] {refId});
            ok = row != null && storeId.equals(com.codenameone.examples.bazaar.Rows.text(
                    row.get("store_id")));
        } else if (COLLECTION
.equals(kind)) {
            Collection c = collections.findById(refId);
            ok = c != null && storeId.equals(c.getStoreId());
        } else if (CONTENT.equals(kind)) {
            com.codenameone.examples.bazaar.domain.ContentPage c =
                    refId.length() > 32 ? null : pages.findById(refId);
            ok = c != null && storeId.equals(c.getStoreId());
        } else if (POST.equals(kind)) {
            com.codenameone.examples.bazaar.domain.BlogPost b =
                    refId.length() > 32 ? null : posts.findById(refId);
            ok = b != null && storeId.equals(b.getStoreId());
        } else if (CAMPAIGN.equals(kind)) {
            Map row = refId.length() > 32 ? null : db.queryOne(
                    "SELECT store_id FROM campaign WHERE id = ?", new Object[] {refId});
            ok = row != null && storeId.equals(com.codenameone.examples.bazaar.Rows.text(
                    row.get("store_id")));
        } else if (NAMED.containsKey(kind)) {
            Map row = refId.length() > 32 ? null : db.queryOne("SELECT store_id FROM "
                    + NAMED.get(kind)[0] + " WHERE id = ?", new Object[] {refId});
            ok = row != null && storeId.equals(com.codenameone.examples.bazaar.Rows.text(
                    row.get("store_id")));
        } else {
            ok = false;
            for (int iter = 0; iter < PAGES.length; iter++) {
                ok |= PAGES[iter].equals(refId);
            }
        }
        if (!ok) {
            throw new ResponseStatusException(404, "Nothing to translate there");
        }
    }

    /// The translated fields of one thing for `locale`, with the fallback
    /// described on the class: field name to text, missing fields absent.
    @Transactional(readOnly = true)
    public Map<String, String> fields(String storeId, String kind, String refId, String locale)
            throws IOException {
        Map<String, String> out = new HashMap<String, String>();
        if (locale == null || locale.length() == 0 || refId == null) {
            return out;
        }
        List<Translation> rs = rows.findByStoreIdAndKindAndRefId(storeId, kind, refId);
        String language = Locales.language(locale);
        for (int iter = 0; iter < rs.size(); iter++) {
            Translation t = rs.get(iter);
            if (t.getLocale().equals(language) && !out.containsKey(t.getField())) {
                out.put(t.getField(), t.getBody());
            }
        }
        for (int iter = 0; iter < rs.size(); iter++) {
            Translation t = rs.get(iter);
            if (t.getLocale().equals(locale)) {
                out.put(t.getField(), t.getBody());
            }
        }
        return out;
    }

    /// The translated fields of a message for `locale` when every one of
    /// `needed` is translated, else null: the caller then sends the whole
    /// original rather than a translated body under an untranslated subject.
    @Transactional(readOnly = true)
    public Map<String, String> complete(String storeId, String kind, String refId, String locale,
            String... needed) throws IOException {
        Map<String, String> got = fields(storeId, kind, refId, locale);
        for (int iter = 0; iter < needed.length; iter++) {
            String v = got.get(needed[iter]);
            if (v == null || v.trim().length() == 0) {
                return null;
            }
        }
        return got;
    }

    /// The first of `wanted` (best first) that the store has translations
    /// in, exactly or by language; null when none.
    @Transactional(readOnly = true)
    public String pick(String storeId, List<String> wanted) throws IOException {
        if (wanted.isEmpty()) {
            return null;
        }
        List<Translation> rs = rows.findByStoreIdOrderByLocaleAsc(storeId);
        if (rs.isEmpty()) {
            return null;
        }
        List<String> have = new ArrayList<String>();
        for (int iter = 0; iter < rs.size(); iter++) {
            if (!CAMPAIGN.equals(rs.get(iter).getKind()) && !have.contains(rs.get(iter).getLocale())) {
                have.add(rs.get(iter).getLocale());
            }
        }
        for (int iter = 0; iter < wanted.size(); iter++) {
            String w = wanted.get(iter);
            if (have.contains(w)) {
                return w;
            }
            String lang = Locales.language(w);
            for (int h = 0; h < have.size(); h++) {
                if (Locales.language(have.get(h)).equals(lang)) {
                    return have.get(h);
                }
            }
        }
        return null;
    }
}
