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
package com.codenameone.examples.savor.catalog;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.savor.Caller;
import com.codenameone.examples.savor.api.CatalogApiServer;
import com.codenameone.examples.savor.api.PhotoDto;
import com.codenameone.examples.savor.api.RestaurantDto;
import com.codenameone.examples.savor.api.RestaurantEditDto;
import com.codenameone.examples.savor.api.RestaurantSummaryDto;
import java.util.List;

/// Browsing restaurants, and applying to list one.
@Component
public class CatalogEndpoint implements CatalogApiServer {
    private final Catalog catalog;
    private final Restaurants restaurants;

    public CatalogEndpoint(Catalog catalog, Restaurants restaurants) {
        this.catalog = catalog;
        this.restaurants = restaurants;
    }

    @Override
    public List<RestaurantSummaryDto> restaurants(double lat, double lng, String cuisine,
            String text, String sort) throws Exception {
        return catalog.nearby(Caller.name(), lat, lng, cuisine, text, sort);
    }

    @Override
    public RestaurantDto restaurant(String id, double lat, double lng) throws Exception {
        return catalog.restaurant(Caller.name(), id, lat, lng);
    }

    @Override
    public PhotoDto photo(String id) throws Exception {
        Caller.name();
        return catalog.photo(id);
    }

    @Override
    public RestaurantDto apply(RestaurantEditDto application) throws Exception {
        return restaurants.apply(Caller.name(), application);
    }

    @Override
    public RestaurantDto application() throws Exception {
        return restaurants.application(Caller.name());
    }

    @Override
    public PhotoDto applicationPhoto(PhotoDto photo) throws Exception {
        return restaurants.applicationPhoto(Caller.name(), photo);
    }
}
