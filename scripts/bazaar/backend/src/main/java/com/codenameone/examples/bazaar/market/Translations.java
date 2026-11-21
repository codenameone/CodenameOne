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
    /// The translatable fields of each kind.
    static final String[][] FIELDS = {
        {PRODUCT, "title", "description", "seoTitle", "seoDescription"},
        {COLLECTION, "title", "description"},
        {PAGE, "body"},
        {CONTENT, "title", "body"},
        {POST, "title", "excerpt", "body"},
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

    public Translations(TranslationRepository rows, ProductRepository products,
            CollectionRepository collections, StoreAccess access,
            com.codenameone.examples.bazaar.repository.ContentPageRepository pages,
            com.codenameone.examples.bazaar.repository.BlogPostRepository posts) {
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
        throw new ResponseStatusException(400, "Translations are of a PRODUCT, COLLECTION, PAGE, CONTENT or POST");
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
        } else {
            out.put("body", privacyText == null ? "" : privacyText);
        }
        return out;
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
