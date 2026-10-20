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
import com.codenameone.examples.savor.api.AcceptDto;
import com.codenameone.examples.savor.api.DishDto;
import com.codenameone.examples.savor.api.MenuSectionDto;
import com.codenameone.examples.savor.api.MerchantApiServer;
import com.codenameone.examples.savor.api.MerchantSummaryDto;
import com.codenameone.examples.savor.api.OrderDto;
import com.codenameone.examples.savor.api.PhotoDto;
import com.codenameone.examples.savor.api.ReasonDto;
import com.codenameone.examples.savor.api.RestaurantDto;
import com.codenameone.examples.savor.api.RestaurantEditDto;
import com.codenameone.examples.savor.order.Orders;
import java.util.List;

/// The restaurant's own side: its details, its orders and its menu. Every
/// call acts for the restaurant the signed-in merchant works for.
@Component
public class MerchantEndpoint implements MerchantApiServer {
    private final Restaurants restaurants;
    private final Orders orders;

    public MerchantEndpoint(Restaurants restaurants, Orders orders) {
        this.restaurants = restaurants;
        this.orders = orders;
    }

    private String kitchen(String who) {
        return restaurants.mine(who).getId();
    }

    @Override
    public RestaurantDto restaurant() throws Exception {
        return restaurants.view(Caller.name());
    }

    @Override
    public RestaurantDto save(RestaurantEditDto details) throws Exception {
        return restaurants.save(Caller.name(), details);
    }

    @Override
    public RestaurantDto pause(boolean paused) throws Exception {
        return restaurants.pause(Caller.name(), paused);
    }

    @Override
    public MerchantSummaryDto summary() throws Exception {
        return restaurants.summary(Caller.name());
    }

    @Override
    public List<OrderDto> orders() throws Exception {
        String who = Caller.name();
        return orders.queue(kitchen(who), who);
    }

    @Override
    public List<OrderDto> history() throws Exception {
        String who = Caller.name();
        return orders.kitchenHistory(kitchen(who), who);
    }

    @Override
    public OrderDto accept(String id, AcceptDto accept) throws Exception {
        String who = Caller.name();
        return orders.accept(who, kitchen(who), id, accept == null ? 0 : accept.prepMinutes());
    }

    @Override
    public OrderDto reject(String id, ReasonDto reason) throws Exception {
        String who = Caller.name();
        return orders.reject(who, kitchen(who), id, reason == null ? null : reason.reason());
    }

    @Override
    public OrderDto ready(String id) throws Exception {
        String who = Caller.name();
        return orders.ready(who, kitchen(who), id);
    }

    @Override
    public MenuSectionDto addSection(MenuSectionDto section) throws Exception {
        return restaurants.addSection(Caller.name(), section);
    }

    @Override
    public MenuSectionDto saveSection(String id, MenuSectionDto section) throws Exception {
        return restaurants.saveSection(Caller.name(), id, section);
    }

    @Override
    public MenuSectionDto removeSection(String id) throws Exception {
        return restaurants.removeSection(Caller.name(), id);
    }

    @Override
    public DishDto addDish(DishDto dish) throws Exception {
        return restaurants.addDish(Caller.name(), dish);
    }

    @Override
    public DishDto saveDish(String id, DishDto dish) throws Exception {
        return restaurants.saveDish(Caller.name(), id, dish);
    }

    @Override
    public DishDto removeDish(String id) throws Exception {
        return restaurants.removeDish(Caller.name(), id);
    }

    @Override
    public DishDto available(String id, boolean available) throws Exception {
        return restaurants.available(Caller.name(), id, available);
    }

    @Override
    public PhotoDto upload(PhotoDto photo) throws Exception {
        return restaurants.upload(Caller.name(), photo);
    }
}
