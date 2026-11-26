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
import com.codenameone.examples.bazaar.api.DigitalApiServer;
import com.codenameone.examples.bazaar.api.DigitalAssetDto;
import com.codenameone.examples.bazaar.api.DigitalUploadDto;
import com.codenameone.examples.bazaar.api.DownloadGrantDto;
import com.codenameone.examples.bazaar.api.LicenseKeysDto;
import com.codenameone.examples.bazaar.api.MediaUploadDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.events.StoreEvents;

import java.util.List;

/// The server's half of `DigitalApi`.
@Component
public class DigitalEndpoint implements DigitalApiServer {
    private final Downloads downloads;
    private final Catalog catalog;
    private final StoreEvents storeEvents;

    public DigitalEndpoint(Downloads downloads, Catalog catalog, StoreEvents storeEvents) {
        this.downloads = downloads;
        this.catalog = catalog;
        this.storeEvents = storeEvents;
    }

    @Override
    public DigitalAssetDto download(String storeId, String variantId) throws Exception {
        return downloads.asset(Caller.name(), storeId, variantId);
    }

    @Override
    public DigitalAssetDto setDownload(String storeId, String variantId, DigitalUploadDto upload)
            throws Exception {
        return downloads.setAsset(Caller.name(), storeId, variantId, upload);
    }

    @Override
    public DigitalAssetDto addLicenseKeys(String storeId, String variantId, LicenseKeysDto keys)
            throws Exception {
        return downloads.addKeys(Caller.name(), storeId, variantId, keys);
    }

    @Override
    public List<DownloadGrantDto> orderDownloads(String storeId, String orderId) throws Exception {
        return downloads.ofOrder(Caller.name(), storeId, orderId);
    }

    @Override
    public DownloadGrantDto revokeGrant(String storeId, String grantId) throws Exception {
        return downloads.revoke(Caller.name(), storeId, grantId);
    }

    @Override
    public ProductDto addMedia(String storeId, String productId, MediaUploadDto media)
            throws Exception {
        ProductDto p = catalog.addMedia(Caller.name(), storeId, productId, media);
        storeEvents.productUpdated(storeId, productId);
        return p;
    }

    @Override
    public ProductDto moveMedia(String storeId, String productId, String photoId,
            String direction) throws Exception {
        ProductDto p = catalog.moveMedia(Caller.name(), storeId, productId, photoId,
                "up".equals(direction));
        storeEvents.productUpdated(storeId, productId);
        return p;
    }

    @Override
    public List<DownloadGrantDto> myDownloads(String orderId) throws Exception {
        return downloads.mine(Caller.name(), orderId);
    }
}
