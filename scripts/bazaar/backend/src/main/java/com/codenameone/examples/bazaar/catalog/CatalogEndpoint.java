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

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.BulkEditDto;
import com.codenameone.examples.bazaar.api.BundleDto;
import com.codenameone.examples.bazaar.api.ChannelScheduleDto;
import com.codenameone.examples.bazaar.api.CatalogApiServer;
import com.codenameone.examples.bazaar.api.CollectionDto;
import com.codenameone.examples.bazaar.api.CountDto;
import com.codenameone.examples.bazaar.api.FacetsDto;
import com.codenameone.examples.bazaar.api.NameCountDto;
import com.codenameone.examples.bazaar.api.PhotoDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.ProductPageDto;
import com.codenameone.examples.bazaar.events.StoreEvents;

import java.util.List;

/// The server's half of `CatalogApi`. The `/api/shop` calls need no sign-in
/// and never read the caller.
@Component
public class CatalogEndpoint implements CatalogApiServer {
    private final Catalog catalog;
    private final StoreEvents storeEvents;

    /// Shop products in the caller's language (the app sends
    /// `Accept-Language`). Field-injected so the constructor stays as it was.
    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.market.ProductText productText;

    public CatalogEndpoint(Catalog catalog, StoreEvents storeEvents) {
        this.catalog = catalog;
        this.storeEvents = storeEvents;
    }

    @Override
    public ProductPageDto products(String handle, String q, String collection, String tag,
            String type, String vendor, String min, String max, String sort, String page,
            String size, String category) throws Exception {
        Search search = Search.of(q, tag, type, vendor, min, max, sort, page, size);
        search.category = Search.category(category);
        return catalog.products(handle, collection, search);
    }

    @Override
    public FacetsDto facets(String handle, String q, String collection, String tag, String type,
            String vendor, String min, String max, String category) throws Exception {
        Search search = Search.of(q, tag, type, vendor, min, max, null, null, null);
        search.category = Search.category(category);
        return catalog.facets(handle, collection, search);
    }

    @Override
    public List<CollectionDto> collections(String handle) throws Exception {
        return catalog.publicCollections(handle);
    }

    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.repository.StoreRepository storeRepository;

    /// Predictive search for the app, from the same [Catalog#suggest] the
    /// storefront's search box reads; nothing for a store behind its
    /// pre-launch password.
    @Override
    public List<com.codenameone.examples.bazaar.api.SearchSuggestionDto> suggest(String handle,
            String q) throws Exception {
        List<com.codenameone.examples.bazaar.api.SearchSuggestionDto> out =
                new java.util.ArrayList<com.codenameone.examples.bazaar.api.SearchSuggestionDto>();
        com.codenameone.examples.bazaar.domain.Store store = storeRepository.findByHandle(handle);
        if (store == null || store.getStorefrontPassword().length() > 0) {
            return out;
        }
        List<String[]> found = catalog.suggest(handle, q == null ? "" : q, 8);
        for (int iter = 0; iter < found.size(); iter++) {
            String[] s = found.get(iter);
            boolean product = "product".equals(s[0]);
            out.add(new com.codenameone.examples.bazaar.api.SearchSuggestionDto(s[0], s[1], s[2],
                    product ? com.codenameone.examples.bazaar.web.StorefrontPages.productUrl(
                            handle, s[2])
                            : com.codenameone.examples.bazaar.web.StorefrontPages.collectionUrl(
                                    handle, s[2])));
        }
        return out;
    }

    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.marketing.MarketingMails automations;

    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.analytics.Views views;

    @Override
    public java.util.List<com.codenameone.examples.bazaar.api.ProductCardDto> together(String id)
            throws Exception {
        return catalog.together(id, 4);
    }

    @Override
    public ProductDto product(String id) throws Exception {
        ProductDto p = catalog.publicProduct(id);
        automations.viewed(p.storeId(), com.codenameone.examples.bazaar.cart.Codes.callerOrNull(),
                p.id());
        views.viewed(p.storeId(), p.id(), com.codenameone.examples.bazaar.cart.Codes.callerOrNull());
        return productText.localize(p, productText.shopperLocale(p.storeId()));
    }

    @Override
    public ProductDto productByHandle(String handle, String productHandle) throws Exception {
        ProductDto p = catalog.publicProductByHandle(handle, productHandle);
        automations.viewed(p.storeId(), com.codenameone.examples.bazaar.cart.Codes.callerOrNull(),
                p.id());
        views.viewed(p.storeId(), p.id(), com.codenameone.examples.bazaar.cart.Codes.callerOrNull());
        return productText.localize(p, productText.shopperLocale(p.storeId()));

    }

    @Override
    public ProductPageDto merchantProducts(String storeId, String status, String q,
            String collection, String tag, String type, String vendor, String sort, String page,
            String size) throws Exception {
        return catalog.merchantProducts(Caller.name(), storeId, status, collection,
                Search.of(q, tag, type, vendor, null, null, sort, page, size));
    }

    @Override
    public ProductDto merchantProduct(String storeId, String productId) throws Exception {
        return catalog.merchantProduct(Caller.name(), storeId, productId);
    }

    /// Each product's margin in basis points, for the back office's list; not
    /// part of the shared API, which answers costs only inside a product.
    public java.util.Map<String, Integer> margins(String storeId) throws Exception {
        return catalog.margins(Caller.name(), storeId);
    }

    /// The variants a bundle of the store may be made of, id and name, for
    /// the back office's picker; not part of the shared API.
    public List<String[]> bundleChoices(String storeId) throws Exception {
        return catalog.bundleChoices(Caller.name(), storeId);
    }

    @Override
    public BundleDto bundle(String storeId, String productId) throws Exception {
        return catalog.bundle(Caller.name(), storeId, productId);
    }

    @Override
    public BundleDto setBundle(String storeId, String productId, BundleDto bundle)
            throws Exception {
        return catalog.setBundle(Caller.name(), storeId, productId, bundle);
    }

    @Override
    public List<ChannelScheduleDto> schedule(String storeId, String productId) throws Exception {
        return catalog.schedule(Caller.name(), storeId, productId);
    }

    @Override
    public List<ChannelScheduleDto> setSchedule(String storeId, String productId,
            ChannelScheduleDto window) throws Exception {
        return catalog.setSchedule(Caller.name(), storeId, productId, window);
    }

    @Override
    public ProductDto createProduct(String storeId, ProductDto product) throws Exception {
        return catalog.create(Caller.name(), storeId, product);
    }

    @Override
    public ProductDto updateProduct(String storeId, String productId, ProductDto product)
            throws Exception {
        ProductDto saved = catalog.update(Caller.name(), storeId, productId, product);
        storeEvents.productUpdated(storeId, productId);
        return saved;
    }

    @Override
    public ProductDto archiveProduct(String storeId, String productId) throws Exception {
        return catalog.archive(Caller.name(), storeId, productId);
    }

    @Override
    public ProductDto duplicateProduct(String storeId, String productId) throws Exception {
        return catalog.duplicate(Caller.name(), storeId, productId);
    }

    @Override
    public CountDto bulkEdit(String storeId, BulkEditDto edit) throws Exception {
        return catalog.bulkEdit(Caller.name(), storeId, edit);
    }

    @Override
    public ProductDto addPhoto(String storeId, String productId, PhotoDto photo)
            throws Exception {
        return catalog.addPhoto(Caller.name(), storeId, productId, photo);
    }

    @Override
    public ProductDto removePhoto(String storeId, String productId, String photoId)
            throws Exception {
        return catalog.removePhoto(Caller.name(), storeId, productId, photoId);
    }

    @Override
    public PhotoDto photo(String photoId) throws Exception {
        return catalog.photo(photoId);
    }

    @Override
    public List<CollectionDto> merchantCollections(String storeId) throws Exception {
        return catalog.merchantCollections(Caller.name(), storeId);
    }

    @Override
    public CollectionDto createCollection(String storeId, CollectionDto collection)
            throws Exception {
        return catalog.createCollection(Caller.name(), storeId, collection);
    }

    @Override
    public CollectionDto updateCollection(String storeId, String collectionId,
            CollectionDto collection) throws Exception {
        return catalog.updateCollection(Caller.name(), storeId, collectionId, collection);
    }

    @Override
    public List<NameCountDto> taxonomy(String q) throws Exception {
        String text = q == null ? "" : q.trim();
        if (text.length() > 120) {
            return new java.util.ArrayList<NameCountDto>();
        }
        int id = Taxonomy.parse(text);
        if (id > 0) {
            List<NameCountDto> one = new java.util.ArrayList<NameCountDto>();
            one.add(new NameCountDto(Taxonomy.path(id), id));
            return one;
        }
        return Taxonomy.search(text, 40);
    }

    @Override
    public CollectionDto deleteCollection(String storeId, String collectionId) throws Exception {
        return catalog.deleteCollection(Caller.name(), storeId, collectionId);
    }
}
