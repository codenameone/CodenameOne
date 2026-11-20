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
import com.codenameone.examples.bazaar.api.CatalogApiServer;
import com.codenameone.examples.bazaar.api.CollectionDto;
import com.codenameone.examples.bazaar.api.CountDto;
import com.codenameone.examples.bazaar.api.FacetsDto;
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

    public CatalogEndpoint(Catalog catalog, StoreEvents storeEvents) {
        this.catalog = catalog;
        this.storeEvents = storeEvents;
    }

    @Override
    public ProductPageDto products(String handle, String q, String collection, String tag,
            String type, String vendor, String min, String max, String sort, String page,
            String size) throws Exception {
        return catalog.products(handle, collection,
                Search.of(q, tag, type, vendor, min, max, sort, page, size));
    }

    @Override
    public FacetsDto facets(String handle, String q, String collection, String tag, String type,
            String vendor, String min, String max) throws Exception {
        return catalog.facets(handle, collection,
                Search.of(q, tag, type, vendor, min, max, null, null, null));
    }

    @Override
    public List<CollectionDto> collections(String handle) throws Exception {
        return catalog.publicCollections(handle);
    }

    @Override
    public ProductDto product(String id) throws Exception {
        return catalog.publicProduct(id);
    }

    @Override
    public ProductDto productByHandle(String handle, String productHandle) throws Exception {
        return catalog.publicProductByHandle(handle, productHandle);
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
    public CollectionDto deleteCollection(String storeId, String collectionId) throws Exception {
        return catalog.deleteCollection(Caller.name(), storeId, collectionId);
    }
}
