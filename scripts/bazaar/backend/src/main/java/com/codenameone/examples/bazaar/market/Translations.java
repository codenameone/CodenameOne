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
    /// The translatable fields of each kind.
    static final String[][] FIELDS = {
        {PRODUCT, "title", "description", "seoTitle", "seoDescription"},
        {COLLECTION, "title", "description"},
        {PAGE, "body"},
        {CONTENT, "title", "body"},
        {POST, "title", "excerpt", "body"},
        {MENU_LINK, "title"},
        {METAFIELD, "name"},
        {METAOBJECT_TYPE, "name"},
        {METAOBJECT_ENTRY, "title"},
    };
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
        throw new ResponseStatusException(400, "Translations are of a PRODUCT, COLLECTION, PAGE, CONTENT, POST, "
                + "MENU_LINK, METAFIELD, METAOBJECT_TYPE or METAOBJECT_ENTRY");
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
        String value = in.value() == null ? "" : in.value().trim();
        if ("body".equals(in.field()) && (CONTENT.equals(in.kind()) || POST.equals(in.kind()))) {
            // Rich text, drawn on the storefront: held to the same allow-list
            // as the original (content.RichText), so a translation cannot
            // carry what the original could not.
            value = com.codenameone.examples.bazaar.content.RichText.sanitize(value).trim();
        }
        if (value.length() > MAX_BODY) {
            throw new ResponseStatusException(400, "A translation is at most " + MAX_BODY
                    + " characters");
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
            rows.insert(t);
        } else {
            t.setBody(value);
            t.setUpdatedAt(Instant.now());
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

    /// Whether a store has any translation of `kind`.
    @Transactional(readOnly = true)
    public boolean has(String storeId, String kind) throws IOException {
        return db.queryOne("SELECT id FROM translation WHERE store_id = ? AND kind = ? LIMIT 1",
                new Object[] {storeId, kind}) != null;
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
        } else if (PRODUCT.equals(kind)) {
            Product p = products.findById(refId);
            ok = p != null && storeId.equals(p.getStoreId());
        } else if (COLLECTION.equals(kind)) {
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
            if (!have.contains(rs.get(iter).getLocale())) {
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
