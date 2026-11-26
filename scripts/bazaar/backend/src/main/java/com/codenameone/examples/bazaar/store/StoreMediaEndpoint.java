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
package com.codenameone.examples.bazaar.store;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.api.PhotoDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.api.StoreMediaApiServer;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.StoreRepository;

import java.io.IOException;
import java.time.Instant;

/// The server's half of `StoreMediaApi`: a store's logo and banner, kept as
/// photos like a product's ([Catalog#storePhoto]) and served from the same
/// public photo path.
@Component
public class StoreMediaEndpoint implements StoreMediaApiServer {
    private final StoreAccess access;
    private final StoreRepository stores;
    private final Catalog catalog;

    public StoreMediaEndpoint(StoreAccess access, StoreRepository stores, Catalog catalog) {
        this.access = access;
        this.stores = stores;
        this.catalog = catalog;
    }

    @Override
    public StoreDto uploadLogo(String storeId, PhotoDto body) throws Exception {
        return upload(Caller.name(), storeId, body, true);
    }

    @Override
    public StoreDto uploadHero(String storeId, PhotoDto body) throws Exception {
        return upload(Caller.name(), storeId, body, false);
    }

    @Override
    public StoreDto removeLogo(String storeId) throws Exception {
        return set(Caller.name(), storeId, "", true);
    }

    @Override
    public StoreDto removeHero(String storeId) throws Exception {
        return set(Caller.name(), storeId, "", false);
    }

    private StoreDto upload(String username, String storeId, PhotoDto photo, boolean logo)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return set(username, storeId, catalog.storePhoto(storeId, photo), logo);
    }

    @Transactional(rollbackFor = IOException.class)
    private StoreDto set(String username, String storeId, String photoId, boolean logo)
            throws IOException {
        Store store = access.requireStore(username, storeId, StoreAccess.MANAGERS);
        if (logo) {
            store.setLogoPhotoId(photoId);
        } else {
            store.setHeroPhotoId(photoId);
        }
        store.setUpdatedAt(Instant.now());
        stores.save(store);
        return Stores.toDto(store);
    }
}
