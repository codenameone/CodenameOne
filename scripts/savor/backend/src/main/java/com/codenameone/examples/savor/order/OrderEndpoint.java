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
package com.codenameone.examples.savor.order;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.savor.Caller;
import com.codenameone.examples.savor.Roles;
import com.codenameone.examples.savor.api.CartDto;
import com.codenameone.examples.savor.api.DeliveryActivityDto;
import com.codenameone.examples.savor.api.OrderApiServer;
import com.codenameone.examples.savor.api.OrderDto;
import com.codenameone.examples.savor.api.QuoteDto;
import com.codenameone.examples.savor.api.RatingDto;
import com.codenameone.examples.savor.api.RestaurantSummaryDto;
import com.codenameone.examples.savor.api.SavedAddressDto;
import com.codenameone.examples.savor.catalog.Catalog;
import com.codenameone.examples.savor.push.OrderPushes;
import java.util.List;

/// A customer's orders, favourites and saved addresses.
@Component
public class OrderEndpoint implements OrderApiServer {
    private final OrderPushes pushes;
    private final Orders orders;
    private final Catalog catalog;
    private final Addresses addresses;

    public OrderEndpoint(Orders orders, Catalog catalog, Addresses addresses, OrderPushes pushes) {
        this.pushes = pushes;
        this.orders = orders;
        this.catalog = catalog;
        this.addresses = addresses;
    }

    @Override
    public DeliveryActivityDto activity(String id, DeliveryActivityDto activity) throws Exception {
        return pushes.attach(Caller.name(), id, activity);
    }

    @Override
    public QuoteDto quote(CartDto cart) throws Exception {
        return orders.quote(Caller.name(), cart);
    }

    @Override
    public OrderDto place(CartDto cart) throws Exception {
        return orders.place(Caller.name(), cart);
    }

    @Override
    public List<OrderDto> active() throws Exception {
        return orders.active(Caller.name());
    }

    @Override
    public List<OrderDto> history() throws Exception {
        return orders.history(Caller.name());
    }

    @Override
    public OrderDto order(String id) throws Exception {
        return orders.get(Caller.name(), Caller.hasRole(Roles.ADMIN), id);
    }

    @Override
    public OrderDto cancel(String id) throws Exception {
        return orders.cancel(Caller.name(), id);
    }

    @Override
    public OrderDto rate(String id, RatingDto rating) throws Exception {
        if (rating == null) {
            throw new ResponseStatusException(400, "Give between one and five stars");
        }
        return orders.rate(Caller.name(), id, rating.stars());
    }

    @Override
    public List<RestaurantSummaryDto> favorites(double lat, double lng) throws Exception {
        return catalog.favorites(Caller.name(), lat, lng);
    }

    @Override
    public RestaurantSummaryDto favorite(String restaurantId) throws Exception {
        return catalog.favorite(Caller.name(), restaurantId, true);
    }

    @Override
    public RestaurantSummaryDto unfavorite(String restaurantId) throws Exception {
        return catalog.favorite(Caller.name(), restaurantId, false);
    }

    @Override
    public List<SavedAddressDto> addresses() throws Exception {
        return addresses.list(Caller.name());
    }

    @Override
    public SavedAddressDto saveAddress(SavedAddressDto address) throws Exception {
        return addresses.save(Caller.name(), address);
    }

    @Override
    public SavedAddressDto removeAddress(String id) throws Exception {
        return addresses.remove(Caller.name(), id);
    }
}
