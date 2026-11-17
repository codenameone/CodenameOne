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
package com.codenameone.examples.brickyard.data;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.api.DataSetDto;
import com.codenameone.examples.brickyard.api.DataStoreApiServer;
import com.codenameone.examples.brickyard.api.DataValueDto;
import com.codenameone.examples.brickyard.api.IncrementDto;

import java.util.List;

/// The data store routes; the work is in [DataStores].
@Component
public class DataStoreEndpoint implements DataStoreApiServer {
    private final DataStores stores;

    public DataStoreEndpoint(DataStores stores) {
        this.stores = stores;
    }

    @Override
    public DataValueDto get(String experienceId, String store, String scope, String key) throws Exception {
        return stores.get(experienceId, store, scope, key);
    }

    @Override
    public DataValueDto set(String experienceId, String store, String scope, String key, DataSetDto body)
            throws Exception {
        return stores.set(experienceId, store, scope, key, body == null ? null : body.value());
    }

    @Override
    public DataValueDto increment(String experienceId, String store, String scope, String key,
            IncrementDto body) throws Exception {
        return stores.increment(experienceId, store, scope, key, body == null ? 0 : body.delta());
    }

    @Override
    public List<DataValueDto> list(String experienceId, String store, String scope) throws Exception {
        return stores.list(experienceId, store, scope);
    }
}
