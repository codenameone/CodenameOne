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
package com.codenameone.examples.bazaar.extensions;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.AppExtensionApiServer;
import com.codenameone.examples.bazaar.api.AppSessionDto;
import com.codenameone.examples.bazaar.api.ExtensionActionDto;
import com.codenameone.examples.bazaar.api.ExtensionActionResultDto;
import com.codenameone.examples.bazaar.api.ExtensionBlockDto;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.StoreRepository;

import java.util.List;

/// The merchant's and the shopper's half of app extensions ([Extensions]).
@Component
public class AppExtensionEndpoint implements AppExtensionApiServer {
    private final Extensions extensions;
    private final StoreRepository stores;

    public AppExtensionEndpoint(Extensions extensions, StoreRepository stores) {
        this.extensions = extensions;
        this.stores = stores;
    }

    @Override
    public List<ExtensionBlockDto> blocks(String storeId, String area, String resourceId)
            throws Exception {
        return extensions.blocks(Caller.name(), storeId, area, resourceId);
    }

    @Override
    public ExtensionActionResultDto action(String storeId, String extensionId,
            ExtensionActionDto action) throws Exception {
        return extensions.action(Caller.name(), storeId, extensionId, action);
    }

    @Override
    public AppSessionDto session(String storeId, String installationId) throws Exception {
        return extensions.session(Caller.name(), storeId, installationId);
    }

    @Override
    public List<ExtensionBlockDto> shopBlocks(String handle, String target) throws Exception {
        Store store = handle == null || handle.length() > 64 ? null : stores.findByHandle(handle);
        if (store == null) {
            throw new ResponseStatusException(404, "No such store");
        }
        return extensions.shop(store.getId(), target);
    }
}
