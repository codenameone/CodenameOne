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
package com.codenameone.examples.bazaar.ops;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.CombinedListingDto;
import com.codenameone.examples.bazaar.api.CsvErrorDto;
import com.codenameone.examples.bazaar.api.ImportDto;
import com.codenameone.examples.bazaar.api.ImportResultDto;
import com.codenameone.examples.bazaar.api.ListingMemberDto;
import com.codenameone.examples.bazaar.api.OptionDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Combined listings -- separate products shown together as the colour
/// options of one -- and the import of a generic marketplace CSV.
///
/// **Import** has one code path: [MarketplaceRows#plan] reads and checks
/// every row, and only when nothing is wrong and `commit` is set are the
/// products made, through [Catalog#create], as drafts. A listing with more
/// than one colour becomes a product per colour, grouped into a combined
/// listing; sizes become the variants of each.
@Component
public class Listings {
    static final int MAX_MEMBERS = 20;

    private final DataSource db;
    private final StoreAccess access;
    private final Catalog catalog;

    public Listings(DataSource db, StoreAccess access, Catalog catalog) {
        this.db = db;
        this.access = access;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public List<CombinedListingDto> list(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.PRODUCTS, false, StoreAccess.MANAGERS);
        List rows = db.query("SELECT l.id, l.title, m.product_id, m.color, p.title AS ptitle FROM "
                + "combined_listing l LEFT JOIN combined_listing_member m ON m.listing_id = l.id LEFT JOIN "
                + "product p ON p.id = m.product_id WHERE l.store_id = ? ORDER BY l.title, l.id, m.position",
                new Object[] {storeId});
        List<CombinedListingDto> out = new ArrayList<CombinedListingDto>();
        Map<String, List<ListingMemberDto>> members = new HashMap<String, List<ListingMemberDto>>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            String id = Rows.text(r.get("id"));
            List<ListingMemberDto> m = members.get(id);
            if (m == null) {
                m = new ArrayList<ListingMemberDto>();
                members.put(id, m);
                out.add(new CombinedListingDto(id, Rows.text(r.get("title")), m));
            }
            if (Rows.text(r.get("product_id")).length() > 0) {
                m.add(new ListingMemberDto(Rows.text(r.get("product_id")), Rows.text(r.get("ptitle")),
                        Rows.text(r.get("color"))));
            }
        }
        return out;
    }

    /// Makes a combined listing of the store's products, each with its colour.
    @Transactional(rollbackFor = IOException.class)
    public CombinedListingDto save(String username, String storeId, CombinedListingDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.PRODUCTS, true, StoreAccess.MANAGERS);
        String title = wanted == null || wanted.title() == null ? "" : wanted.title().trim();
        if (title.length() == 0 || title.length() > 200) {
            throw new ResponseStatusException(400, "A combined listing has a title");
        }
        List<ListingMemberDto> members = wanted.members() == null ? new ArrayList<ListingMemberDto>()
                : wanted.members();
        if (members.size() < 2 || members.size() > MAX_MEMBERS) {
            throw new ResponseStatusException(400, "A combined listing groups 2 to " + MAX_MEMBERS
                    + " products");
        }
        List<ListingMemberDto> saved = new ArrayList<ListingMemberDto>();
        for (int iter = 0; iter < members.size(); iter++) {
            ListingMemberDto m = members.get(iter);
            Map p = m == null || m.productId() == null || m.productId().length() > 32 ? null
                    : db.queryOne("SELECT store_id, title FROM product WHERE id = ?",
                            new Object[] {m.productId()});
            if (p == null || !storeId.equals(Rows.text(p.get("store_id")))) {
                throw new ResponseStatusException(400, "No such product");
            }
            Map taken = db.queryOne("SELECT id FROM combined_listing_member WHERE product_id = ?",
                    new Object[] {m.productId()});
            if (taken != null) {
                throw new ResponseStatusException(409, Rows.text(p.get("title"))
                        + " is already in a combined listing");
            }
            String color = m.color() == null || m.color().trim().length() == 0
                    ? Rows.text(p.get("title")) : m.color().trim();
            saved.add(new ListingMemberDto(m.productId(), Rows.text(p.get("title")), Codes.cut(color, 60)));
        }
        String id = Ids.next();
        insert(storeId, id, title, saved);
        return new CombinedListingDto(id, title, saved);
    }

    private void insert(String storeId, String id, String title, List<ListingMemberDto> members)
            throws IOException {
        db.execute("INSERT INTO combined_listing (id, store_id, title, created_at) VALUES (?, ?, ?, ?)",
                new Object[] {id, storeId, title, Long.valueOf(System.currentTimeMillis())});
        for (int iter = 0; iter < members.size(); iter++) {
            ListingMemberDto m = members.get(iter);
            db.execute("INSERT INTO combined_listing_member (id, store_id, listing_id, product_id, color, "
                    + "position) VALUES (?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(), storeId, id,
                        m.productId(), m.color(), Integer.valueOf(iter)});
        }
    }

    /// Dry run or commit of a marketplace CSV.
    @Transactional(rollbackFor = IOException.class)
    public ImportResultDto importCsv(String username, String storeId, ImportDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.PRODUCTS, true, StoreAccess.MANAGERS);
        MarketplaceRows.Plan plan = MarketplaceRows.plan(wanted == null || wanted.csv() == null ? ""
                : wanted.csv());
        List<CsvErrorDto> errors = new ArrayList<CsvErrorDto>();
        for (int iter = 0; iter < plan.errors.size(); iter++) {
            Object[] e = plan.errors.get(iter);
            errors.add(new CsvErrorDto(((Integer) e[0]).intValue(), String.valueOf(e[1])));
        }
        for (int iter = 0; iter < plan.listings.size(); iter++) {
            List<MarketplaceRows.Item> items = plan.listings.get(iter).items;
            for (int k = 0; k < items.size(); k++) {
                Map clash = db.queryOne("SELECT v.id FROM variant v JOIN product p ON p.id = v.product_id "
                        + "WHERE p.store_id = ? AND v.sku = ?", new Object[] {storeId, items.get(k).sku});
                if (clash != null) {
                    errors.add(new CsvErrorDto(items.get(k).line, "SKU " + Codes.cut(items.get(k).sku, 40)
                            + " is already in the catalog"));
                }
            }
        }
        List<String> preview = new ArrayList<String>();
        int products = 0;
        int variants = 0;
        for (int iter = 0; iter < plan.listings.size(); iter++) {
            MarketplaceRows.Listing l = plan.listings.get(iter);
            List<String> colors = colors(l);
            products += Math.max(1, colors.size());
            variants += l.items.size();
            preview.add(l.title + ": " + l.items.size() + " variants"
                    + (colors.size() > 1 ? ", " + colors.size() + " colours combined" : ""));
        }
        boolean commit = wanted != null && wanted.commit() && errors.isEmpty() && plan.rows > 0;
        if (commit) {
            for (int iter = 0; iter < plan.listings.size(); iter++) {
                create(username, storeId, plan.listings.get(iter));
            }
        }
        return new ImportResultDto(plan.rows, products, variants, commit, errors, preview);
    }

    private static List<String> colors(MarketplaceRows.Listing l) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < l.items.size(); iter++) {
            String c = l.items.get(iter).color;
            if (c.length() > 0 && !out.contains(c)) {
                out.add(c);
            }
        }
        return out;
    }

    private void create(String username, String storeId, MarketplaceRows.Listing l) throws IOException {
        List<String> colors = colors(l);
        if (colors.size() <= 1) {
            catalog.create(username, storeId, product(l, l.title, l.items));
            return;
        }
        List<ListingMemberDto> members = new ArrayList<ListingMemberDto>();
        for (int c = 0; c < colors.size(); c++) {
            List<MarketplaceRows.Item> mine = new ArrayList<MarketplaceRows.Item>();
            for (int iter = 0; iter < l.items.size(); iter++) {
                if (colors.get(c).equals(l.items.get(iter).color)) {
                    mine.add(l.items.get(iter));
                }
            }
            String title = Codes.cut(l.title + " - " + colors.get(c), 200);
            ProductDto made = catalog.create(username, storeId, product(l, title, mine));
            members.add(new ListingMemberDto(made.id(), title, Codes.cut(colors.get(c), 60)));
        }
        insert(storeId, Ids.next(), Codes.cut(l.title, 200), members);
    }

    private static ProductDto product(MarketplaceRows.Listing l, String title,
            List<MarketplaceRows.Item> items) {
        boolean sized = false;
        for (int iter = 0; iter < items.size(); iter++) {
            sized |= items.get(iter).size.length() > 0;
        }
        List<OptionDto> options = new ArrayList<OptionDto>();
        List<VariantDto> variants = new ArrayList<VariantDto>();
        List<String> sizes = new ArrayList<String>();
        for (int iter = 0; iter < items.size(); iter++) {
            MarketplaceRows.Item it = items.get(iter);
            String size = sized ? (it.size.length() == 0 ? "One size" : it.size) : null;
            if (size != null && !sizes.contains(size)) {
                sizes.add(size);
            }
            variants.add(new VariantDto(null, null, size == null ? "Default" : size, size, null, null,
                    it.sku, null, it.priceCents, 0, 0, 0, true, true, false, iter, it.quantity, null));
        }
        if (sized) {
            options.add(new OptionDto(null, "Size", sizes, 0));
        }
        List<String> tags = new ArrayList<String>();
        if (l.category.length() > 0) {
            tags.add(Codes.cut(l.category, 60));
        }
        return new ProductDto(null, null, null, Codes.cut(title, 200), l.description, l.brand,
                l.category, tags, null, null, null, null, 0, 0, options, variants, null, null);
    }
}
