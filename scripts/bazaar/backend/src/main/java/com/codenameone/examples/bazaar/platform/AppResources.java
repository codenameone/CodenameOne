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
package com.codenameone.examples.bazaar.platform;

import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.api.AdjustDto;
import com.codenameone.examples.bazaar.api.CustomerDto;
import com.codenameone.examples.bazaar.api.CustomerEditDto;
import com.codenameone.examples.bazaar.api.DiscountDto;
import com.codenameone.examples.bazaar.api.LevelDto;
import com.codenameone.examples.bazaar.api.MetafieldValueDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.content.Metafields;
import com.codenameone.examples.bazaar.customer.Customers;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.events.StoreEvents;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Stock;
import com.codenameone.examples.bazaar.rates.RateSettings;
import com.codenameone.examples.bazaar.repository.StoreRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The store resources a key or an app reaches beyond products and orders,
/// shared by the REST routes ([AppResourceApi]) and the GraphQL mutations
/// ([AppGraphQl]) so that both hold the same rules. Each operation runs the
/// merchant's own service as the store's owner -- the key was already held
/// to its store and scope -- so the domain rules are the dashboard's; an
/// app's writes are audited under its installation.
///
/// Webhooks are not sent from here: the callers send them once the write
/// committed ([#afterWrites]), so a rolled back batch announces nothing.
///
/// | Operation | Scope | Webhook |
/// |---|---|---|
/// | `productCreate`, `productUpdate` | `write_products` | `products/update` on an update |
/// | `inventoryAdjust` | `write_inventory` | `inventory/low` at the reorder point |
/// | `discountCreate` | `write_discounts` | none |
/// | `fulfillmentCreate` | `write_fulfillments` | `orders/fulfilled` when the order is |
/// | `metafieldsSet` | `write_metafields`, an app's own namespace | none |
/// | `customerUpdate` | `write_customers` | none |
@Component
public class AppResources {
    private final StoreRepository stores;
    private final Catalog catalog;
    private final Stock stock;
    private final RateSettings rates;
    private final Fulfillments fulfillments;
    private final Customers customers;
    private final Metafields metafields;
    private final StoreEvents events;
    private final Apps apps;

    public AppResources(StoreRepository stores, Catalog catalog, Stock stock, RateSettings rates,
            Fulfillments fulfillments, Customers customers, Metafields metafields,
            StoreEvents events, Apps apps) {
        this.stores = stores;
        this.catalog = catalog;
        this.stock = stock;
        this.rates = rates;
        this.fulfillments = fulfillments;
        this.customers = customers;
        this.metafields = metafields;
        this.events = events;
        this.apps = apps;
    }

    /// What a write leaves for [#afterWrites]: the products changed and the
    /// stock levels adjusted.
    public static final class Writes {
        final List<String> products = new ArrayList<String>();
        final List<LevelDto> levels = new ArrayList<LevelDto>();
    }

    /// The scope a mutation needs, or null for one there is not.
    public static String scopeOfMutation(String name) {
        if ("productCreate".equals(name) || "productUpdate".equals(name)) {
            return StoreApiKeys.WRITE_PRODUCTS;
        }
        if ("inventoryAdjust".equals(name)) {
            return StoreApiKeys.WRITE_INVENTORY;
        }
        if ("discountCreate".equals(name)) {
            return StoreApiKeys.WRITE_DISCOUNTS;
        }
        if ("fulfillmentCreate".equals(name)) {
            return StoreApiKeys.WRITE_FULFILLMENTS;
        }
        if ("metafieldsSet".equals(name)) {
            return StoreApiKeys.WRITE_METAFIELDS;
        }
        if ("customerUpdate".equals(name)) {
            return StoreApiKeys.WRITE_CUSTOMERS;
        }
        return null;
    }

    String owner(String storeId) throws IOException {
        Store s = stores.findById(storeId);
        if (s == null) {
            throw new ResponseStatusException(404, "No such store");
        }
        return s.getOwnerUsername();
    }

    private void audit(StoreApiKeys.Key k, String action, String detail) throws IOException {
        if (k.appId != null) {
            apps.audit(k.storeId, k.owner(), action, k.appId, detail);
        }
    }

    /// `input` as a `type`, through its JSON: the DTOs are records the JSON
    /// layer reads already.
    static <T> T as(Object input, Class<T> type) throws IOException {
        if (!(input instanceof Map)) {
            throw new ResponseStatusException(400, "The input is an object");
        }
        return Json.read(Json.write(input), type);
    }

    /// `base` with the members `input` names replaced: an update says only
    /// what changes.
    static Map<String, Object> merged(Object base, Object input) throws IOException {
        if (!(input instanceof Map)) {
            throw new ResponseStatusException(400, "The input is an object");
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        Object current = Json.parse(Json.write(base));
        if (current instanceof Map) {
            out.putAll((Map<String, Object>) current);
        }
        out.putAll((Map<String, Object>) input);
        return out;
    }

    // -------------------------------------------------------------- reads

    public List<LevelDto> levels(String storeId, String locationId, String productId)
            throws IOException {
        return stock.levels(owner(storeId), storeId, locationId, productId, false);
    }

    public List<DiscountDto> discounts(String storeId) throws IOException {
        return rates.discounts(owner(storeId), storeId);
    }

    public Object fulfillmentsOf(String storeId, String orderId) throws IOException {
        if (orderId == null || orderId.length() == 0) {
            throw new ResponseStatusException(400, "Say which order (orderId)");
        }
        return fulfillments.ofOrder(owner(storeId), storeId, orderId);
    }

    public List<CustomerDto> customers(String storeId, int max) throws IOException {
        List<CustomerDto> all = customers.list(owner(storeId), storeId, new Customers.Filter());
        return all.size() <= max ? all : new ArrayList<CustomerDto>(all.subList(0, max));
    }

    public Object customer(String storeId, String customerId) throws IOException {
        return customers.detail(owner(storeId), storeId, customerId).customer();
    }

    // ------------------------------------------------------------- writes

    /// Runs one mutation; `args` are its GraphQL arguments, or the REST
    /// route's body under `input` and its path ids.
    Object write(StoreApiKeys.Key k, String name, Map<String, Object> args, Writes w)
            throws IOException {
        String storeId = k.storeId;
        Object input = args.get("input");
        if ("productCreate".equals(name)) {
            ProductDto made = catalog.create(owner(storeId), storeId, as(input, ProductDto.class));
            audit(k, "APP_PRODUCT_CREATED", made.id());
            return made;
        }
        if ("productUpdate".equals(name)) {
            String id = text(args, "id");
            String owner = owner(storeId);
            ProductDto now = catalog.merchantProduct(owner, storeId, id);
            ProductDto saved = catalog.update(owner, storeId, id,
                    as(merged(now, input), ProductDto.class));
            w.products.add(id);
            audit(k, "APP_PRODUCT_UPDATED", id);
            return saved;
        }
        if ("inventoryAdjust".equals(name)) {
            LevelDto level = stock.adjust(owner(storeId), storeId, as(input, AdjustDto.class));
            w.levels.add(level);
            audit(k, "APP_INVENTORY_ADJUSTED", level.variantId());
            return level;
        }
        if ("discountCreate".equals(name)) {
            Map<String, Object> fresh = merged(null, input);
            fresh.remove("id");
            fresh.remove("usedCount");
            DiscountDto made = rates.saveDiscount(owner(storeId), storeId,
                    as(fresh, DiscountDto.class));
            audit(k, "APP_DISCOUNT_CREATED", made.id());
            return made;
        }
        if ("fulfillmentCreate".equals(name)) {
            String orderId = text(args, "orderId");
            Object made = fulfillments.create(owner(storeId), storeId, orderId,
                    as(input, NewFulfillmentDto.class));
            audit(k, "APP_FULFILLMENT_CREATED", orderId);
            return made;
        }
        if ("metafieldsSet".equals(name)) {
            if (k.appId == null) {
                throw new ResponseStatusException(403, "Only an app's access token may do this");
            }
            Object list = args.get("metafields");
            if (!(list instanceof List) || ((List) list).isEmpty()) {
                throw new ResponseStatusException(400, "Name the metafields to set");
            }
            List<MetafieldValueDto> out = new ArrayList<MetafieldValueDto>();
            for (int iter = 0; iter < ((List) list).size(); iter++) {
                MetafieldValueDto in = as(((List) list).get(iter), MetafieldValueDto.class);
                List<MetafieldValueDto> now = metafields.putForApp(storeId, k.appId, in);
                for (int i = 0; i < now.size(); i++) {
                    MetafieldValueDto v = now.get(i);
                    if (v.defId() != null && v.defId().equals(in.defId())) {
                        out.add(v);
                    }
                }
                audit(k, "APP_METAFIELD_SET", in.defId() + " on " + in.ownerId());
            }
            return out;
        }
        if ("customerUpdate".equals(name)) {
            String id = text(args, "id");
            String owner = owner(storeId);
            CustomerDto now = customers.detail(owner, storeId, id).customer();
            Map<String, Object> base = new LinkedHashMap<String, Object>();
            base.put("note", now.note());
            base.put("tags", now.tags());
            base.put("acceptsMarketing", Boolean.valueOf(now.acceptsMarketing()));
            CustomerDto saved = customers.edit(owner, storeId, id,
                    as(merged(base, input), CustomerEditDto.class));
            audit(k, "APP_CUSTOMER_UPDATED", id);
            return saved;
        }
        throw new ResponseStatusException(400, "No mutation " + name);
    }

    /// One write, committed on its own.
    @Transactional(rollbackFor = IOException.class)
    public Object writeOne(StoreApiKeys.Key k, String name, Map<String, Object> args, Writes w)
            throws IOException {
        return write(k, name, args, w);
    }

    /// A GraphQL mutation's fields, all or none: one failing rolls back the
    /// ones before it. Answers each field's result by its alias.
    @Transactional(rollbackFor = IOException.class)
    public Map<String, Object> writeAll(StoreApiKeys.Key k, List<MiniGraphQl.Field> fields,
            Writes w) throws IOException {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        for (int iter = 0; iter < fields.size(); iter++) {
            MiniGraphQl.Field f = fields.get(iter);
            data.put(f.alias, write(k, f.name, f.args, w));
        }
        return data;
    }

    /// The webhooks for writes that committed.
    public void afterWrites(String storeId, Writes w) {
        for (int iter = 0; iter < w.products.size(); iter++) {
            events.productUpdated(storeId, w.products.get(iter));
        }
        for (int iter = 0; iter < w.levels.size(); iter++) {
            events.stockAdjusted(storeId, w.levels.get(iter));
        }
    }

    private static String text(Map<String, Object> args, String name) {
        Object v = args.get(name);
        if (!(v instanceof String) || ((String) v).length() == 0) {
            throw new ResponseStatusException(400, "Say which one (" + name + ")");
        }
        return (String) v;
    }
}
