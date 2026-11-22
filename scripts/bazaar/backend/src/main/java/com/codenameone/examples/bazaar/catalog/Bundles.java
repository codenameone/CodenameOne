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
package com.codenameone.examples.bazaar.catalog;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Fixed bundles: a product of kind [#BUNDLE] is made of other variants, each
/// a quantity of one bundle. The bundle has a price of its own (its variant's)
/// and no stock of its own: what is available is the fewest bundles its
/// components make, and selling one moves the components. `Inventory` expands
/// a bundle variant into its components for every stock operation, so
/// checkout, the register, fulfilment, cancellations and returns need no
/// knowledge of bundles at all.
@Component
public class Bundles {
    public static final String STANDARD = "STANDARD";
    public static final String BUNDLE = "BUNDLE";
    /// The most components a bundle may have.
    public static final int MAX_PARTS = 20;
    /// The most units of one component in a bundle.
    public static final int MAX_QUANTITY = 99;
    /// What [#available] answers when no component's stock is tracked.
    public static final int UNLIMITED = 1000000;

    /// One component: a variant and how many of it make one bundle.
    public static final class Part {
        public final String variantId;
        public final int quantity;

        public Part(String variantId, int quantity) {
            this.variantId = variantId;
            this.quantity = quantity;
        }
    }

    private final DataSource db;

    public Bundles(DataSource db) {
        this.db = db;
    }

    /// The components of the bundle `variantId` belongs to; empty for a
    /// variant that is not a bundle's.
    @Transactional(readOnly = true)
    public List<Part> partsOfVariant(String variantId) throws IOException {
        List<Part> out = new ArrayList<Part>();
        if (variantId == null) {
            return out;
        }
        List rs = db.query("SELECT bc.variant_id, bc.quantity FROM bundle_component bc JOIN variant v "
                + "ON v.product_id = bc.product_id WHERE v.id = ? ORDER BY bc.position",
                new Object[] {variantId});
        for (int iter = 0; iter < rs.size(); iter++) {
            Map r = (Map) rs.get(iter);
            out.add(new Part(Rows.text(r.get("variant_id")), (int) Rows.number(r.get("quantity"))));
        }
        return out;
    }

    /// The components of a bundle product, in order.
    @Transactional(readOnly = true)
    public List<Part> parts(String productId) throws IOException {
        List<Part> out = new ArrayList<Part>();
        List rs = db.query("SELECT variant_id, quantity FROM bundle_component WHERE product_id = ? "
                + "ORDER BY position", new Object[] {productId});
        for (int iter = 0; iter < rs.size(); iter++) {
            Map r = (Map) rs.get(iter);
            out.add(new Part(Rows.text(r.get("variant_id")), (int) Rows.number(r.get("quantity"))));
        }
        return out;
    }

    /// Every bundle of a store: product id to its components.
    @Transactional(readOnly = true)
    public Map<String, List<Part>> byProduct(String storeId) throws IOException {
        Map<String, List<Part>> out = new HashMap<String, List<Part>>();
        List rs = db.query("SELECT product_id, variant_id, quantity FROM bundle_component WHERE "
                + "store_id = ? ORDER BY position", new Object[] {storeId});
        for (int iter = 0; iter < rs.size(); iter++) {
            Map r = (Map) rs.get(iter);
            String p = Rows.text(r.get("product_id"));
            List<Part> list = out.get(p);
            if (list == null) {
                list = new ArrayList<Part>();
                out.put(p, list);
            }
            list.add(new Part(Rows.text(r.get("variant_id")), (int) Rows.number(r.get("quantity"))));
        }
        return out;
    }

    /// Replaces a bundle's components. Each must be a variant of the same
    /// store, not of this product and not of another bundle (a bundle of
    /// bundles would make stock a recursion); the same variant twice is
    /// merged.
    @Transactional(rollbackFor = IOException.class)
    public void set(String storeId, String productId, List<Part> wanted) throws IOException {
        if (wanted.size() > MAX_PARTS) {
            throw new ResponseStatusException(400, "A bundle has at most " + MAX_PARTS
                    + " components");
        }
        List<String> ids = new ArrayList<String>();
        List<Integer> qtys = new ArrayList<Integer>();
        for (int iter = 0; iter < wanted.size(); iter++) {
            Part p = wanted.get(iter);
            if (p.quantity < 1 || p.quantity > MAX_QUANTITY) {
                throw new ResponseStatusException(400, "A component's quantity is 1 to "
                        + MAX_QUANTITY);
            }
            Map v = p.variantId == null ? null : db.queryOne("SELECT v.store_id, v.product_id, "
                    + "pr.kind FROM variant v JOIN product pr ON pr.id = v.product_id WHERE v.id = ?",
                    new Object[] {p.variantId});
            if (v == null || !storeId.equals(Rows.text(v.get("store_id")))) {
                throw new ResponseStatusException(404, "No such variant");
            }
            if (productId.equals(Rows.text(v.get("product_id")))) {
                throw new ResponseStatusException(400, "A bundle cannot contain itself");
            }
            if (BUNDLE.equals(Rows.text(v.get("kind")))) {
                throw new ResponseStatusException(400, "A bundle cannot contain another bundle");
            }
            int at = ids.indexOf(p.variantId);
            if (at >= 0) {
                int sum = qtys.get(at).intValue() + p.quantity;
                if (sum > MAX_QUANTITY) {
                    throw new ResponseStatusException(400, "A component's quantity is 1 to "
                            + MAX_QUANTITY);
                }
                qtys.set(at, Integer.valueOf(sum));
            } else {
                ids.add(p.variantId);
                qtys.add(Integer.valueOf(p.quantity));
            }
        }
        db.execute("DELETE FROM bundle_component WHERE product_id = ?", new Object[] {productId});
        for (int iter = 0; iter < ids.size(); iter++) {
            db.execute("INSERT INTO bundle_component (id, store_id, product_id, variant_id, quantity, "
                    + "position) VALUES (?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(), storeId,
                        productId, ids.get(iter), qtys.get(iter), Integer.valueOf(iter)});
        }
    }

    /// Whether a variant is a component of any bundle, which keeps it from
    /// becoming a bundle itself.
    @Transactional(readOnly = true)
    public boolean isComponent(String productId) throws IOException {
        return db.queryOne("SELECT bc.id FROM bundle_component bc JOIN variant v ON v.id = "
                + "bc.variant_id WHERE v.product_id = ? LIMIT 1", new Object[] {productId}) != null;
    }

    /// Every bundle variant of a store: variant id to its bundle's
    /// components. A variant missing from the map is not a bundle's.
    @Transactional(readOnly = true)
    public Map<String, List<Part>> byVariant(String storeId) throws IOException {
        Map<String, List<Part>> out = new HashMap<String, List<Part>>();
        List rs = db.query("SELECT v.id AS bundle_variant, bc.variant_id, bc.quantity FROM "
                + "bundle_component bc JOIN variant v ON v.product_id = bc.product_id WHERE "
                + "bc.store_id = ? ORDER BY bc.position", new Object[] {storeId});
        for (int iter = 0; iter < rs.size(); iter++) {
            Map r = (Map) rs.get(iter);
            String b = Rows.text(r.get("bundle_variant"));
            List<Part> list = out.get(b);
            if (list == null) {
                list = new ArrayList<Part>();
                out.put(b, list);
            }
            list.add(new Part(Rows.text(r.get("variant_id")), (int) Rows.number(r.get("quantity"))));
        }
        return out;
    }

    /// Whether a variant is a component of any bundle: it cannot be deleted
    /// while it is, or every sale of the bundle would fail.
    @Transactional(readOnly = true)
    public boolean isVariantComponent(String variantId) throws IOException {
        return db.queryOne("SELECT id FROM bundle_component WHERE variant_id = ? LIMIT 1",
                new Object[] {variantId}) != null;
    }

    /// How many whole bundles the components make over `locationIds` (every
    /// location when null). A bundle is reserved from one location, so the
    /// fewest per location is counted and those are added up; a component
    /// whose stock is not tracked never limits it.
    @Transactional(readOnly = true)
    public int available(List<Part> parts, Set<String> locationIds) throws IOException {
        if (parts.isEmpty()) {
            return 0;
        }
        StringBuilder marks = new StringBuilder();
        Object[] ids = new Object[parts.size()];
        for (int iter = 0; iter < parts.size(); iter++) {
            marks.append(iter == 0 ? "?" : ", ?");
            ids[iter] = parts.get(iter).variantId;
        }
        List untracked = db.query("SELECT id FROM variant WHERE track_inventory = 0 AND id IN ("
                + marks + ")", ids);
        Map<String, Integer> always = new HashMap<String, Integer>();
        for (int iter = 0; iter < untracked.size(); iter++) {
            always.put(Rows.text(((Map) untracked.get(iter)).get("id")),
                    Integer.valueOf(Integer.MAX_VALUE));
        }
        if (always.size() == parts.size()) {
            return UNLIMITED;
        }
        List rs = db.query("SELECT variant_id, location_id, on_hand, committed FROM "
                + "inventory_level WHERE variant_id IN (" + marks + ")", ids);
        Map<String, Map<String, Integer>> byLocation = new HashMap<String, Map<String, Integer>>();
        for (int iter = 0; iter < rs.size(); iter++) {
            Map r = (Map) rs.get(iter);
            String at = Rows.text(r.get("location_id"));
            if (locationIds != null && !locationIds.contains(at)) {
                continue;
            }
            Map<String, Integer> here = byLocation.get(at);
            if (here == null) {
                here = new HashMap<String, Integer>(always);
                byLocation.put(at, here);
            }
            String v = Rows.text(r.get("variant_id"));
            if (!always.containsKey(v)) {
                here.put(v, Integer.valueOf((int) (Rows.number(r.get("on_hand"))
                        - Rows.number(r.get("committed")))));
            }
        }
        long total = 0;
        for (Map<String, Integer> here : byLocation.values()) {
            total += make(parts, here);
        }
        return (int) Math.min(UNLIMITED, total);
    }

    /// The components of a bundle product as the storefront lists them:
    /// "2 x Mug - Blue", in order.
    @Transactional(readOnly = true)
    public List<String> describe(String productId) throws IOException {
        List<String> out = new ArrayList<String>();
        List rs = db.query("SELECT bc.quantity, v.title AS vtitle, p.title AS ptitle FROM "
                + "bundle_component bc JOIN variant v ON v.id = bc.variant_id JOIN product p ON "
                + "p.id = v.product_id WHERE bc.product_id = ? ORDER BY bc.position",
                new Object[] {productId});
        for (int iter = 0; iter < rs.size(); iter++) {
            Map r = (Map) rs.get(iter);
            out.add(Rows.number(r.get("quantity")) + " x " + title(Rows.text(r.get("ptitle")),
                    Rows.text(r.get("vtitle"))));
        }
        return out;
    }

    /// A variant's name in a list: the product's title, then the variant's
    /// unless it is the default one.
    public static String title(String product, String variant) {
        return variant == null || variant.length() == 0 || "Default".equals(variant)
                || "Default Title".equals(variant) ? product : product + " - " + variant;
    }

    /// How many whole bundles `available` units of each component make;
    /// never below zero.
    public static int make(List<Part> parts, Map<String, Integer> available) {
        if (parts.isEmpty()) {
            return 0;
        }
        int fewest = Integer.MAX_VALUE;
        for (int iter = 0; iter < parts.size(); iter++) {
            Part p = parts.get(iter);
            Integer n = available.get(p.variantId);
            int have = n == null ? 0 : Math.max(0, n.intValue());
            fewest = Math.min(fewest, have / p.quantity);
        }
        return fewest;
    }
}
