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
package com.codenameone.examples.savor.customer;

import com.codenameone.examples.savor.api.CartDto;
import com.codenameone.examples.savor.api.CartLineDto;
import com.codenameone.examples.savor.api.DishDto;
import com.codenameone.examples.savor.api.DishOptionDto;
import com.codenameone.examples.savor.api.RestaurantDto;

import java.util.ArrayList;
import java.util.List;

/// What the customer is about to order: dishes from one restaurant.
///
/// Kept in memory and nowhere else. The prices here are for showing while the
/// customer chooses; what is charged is the server's quote, which prices every
/// line again from the menu as it is then.
public final class Cart {
    /// One dish, with the options picked for it and how many.
    public static final class Line {
        public final DishDto dish;
        public final List<DishOptionDto> options;
        public int quantity;

        Line(DishDto dish, List<DishOptionDto> options, int quantity) {
            this.dish = dish;
            this.options = options;
            this.quantity = quantity;
        }

        /// The price of one, with its options.
        public long unitCents() {
            long cents = dish.priceCents();
            for (int iter = 0; iter < options.size(); iter++) {
                cents += options.get(iter).priceCents();
            }
            return cents;
        }

        /// The options, as a line under the dish's name.
        public String optionNames() {
            StringBuilder names = new StringBuilder();
            for (int iter = 0; iter < options.size(); iter++) {
                names.append(iter == 0 ? "" : ", ").append(options.get(iter).name());
            }
            return names.toString();
        }

        boolean same(DishDto other, List<DishOptionDto> picked) {
            if (!dish.id().equals(other.id()) || picked.size() != options.size()) {
                return false;
            }
            for (int iter = 0; iter < picked.size(); iter++) {
                if (!picked.get(iter).id().equals(options.get(iter).id())) {
                    return false;
                }
            }
            return true;
        }
    }

    /// Told whenever the cart changes, so the bar that shows it can follow.
    public interface Listener {
        void changed();
    }

    private static RestaurantDto restaurant;
    private static final List<Line> LINES = new ArrayList<Line>();
    private static Listener listener;

    private Cart() {
    }

    public static void listen(Listener current) {
        listener = current;
    }

    /// The restaurant the cart is from, or null while it is empty.
    public static RestaurantDto restaurant() {
        return LINES.isEmpty() ? null : restaurant;
    }

    public static List<Line> lines() {
        return LINES;
    }

    /// Whether adding from `other` would mean starting again: a cart is from
    /// one kitchen, because one courier carries it.
    public static boolean fromElsewhere(RestaurantDto other) {
        return !LINES.isEmpty() && !restaurant.id().equals(other.id());
    }

    /// The most of one line the server takes (Quotes.MAX_QUANTITY). Adding the
    /// same dish again, or stepping it up, stops here rather than building a
    /// cart that can be neither priced nor placed.
    public static final int MAX_QUANTITY = 50;

    /// Adds `quantity` of `dish` with `options`, emptying a cart from another
    /// restaurant first. The same dish with the same options is one line.
    public static void add(RestaurantDto from, DishDto dish, List<DishOptionDto> options,
            int quantity) {
        if (fromElsewhere(from)) {
            LINES.clear();
        }
        restaurant = from;
        for (int iter = 0; iter < LINES.size(); iter++) {
            if (LINES.get(iter).same(dish, options)) {
                LINES.get(iter).quantity = Math.min(MAX_QUANTITY,
                        LINES.get(iter).quantity + quantity);
                changed();
                return;
            }
        }
        LINES.add(new Line(dish, options, Math.min(MAX_QUANTITY, quantity)));
        changed();
    }

    /// Changes how many of `line` there are; none takes it out.
    public static void set(Line line, int quantity) {
        // Callers may ask for more; MAX_QUANTITY is what one line can hold.
        if (quantity <= 0) {
            LINES.remove(line);
        } else {
            line.quantity = Math.min(MAX_QUANTITY, quantity);
        }
        changed();
    }

    public static void clear() {
        LINES.clear();
        changed();
    }

    public static int count() {
        int count = 0;
        for (int iter = 0; iter < LINES.size(); iter++) {
            count += LINES.get(iter).quantity;
        }
        return count;
    }

    /// The food, before fees, as the menu last said.
    public static long subtotalCents() {
        long cents = 0;
        for (int iter = 0; iter < LINES.size(); iter++) {
            cents += LINES.get(iter).unitCents() * LINES.get(iter).quantity;
        }
        return cents;
    }

    /// The cart as the server prices and places it.
    public static CartDto dto(double lat, double lng, String address, String note, String promo,
            long tipCents, String paymentMethodId) {
        List<CartLineDto> lines = new ArrayList<CartLineDto>();
        for (int iter = 0; iter < LINES.size(); iter++) {
            Line line = LINES.get(iter);
            List<String> ids = new ArrayList<String>();
            for (int opt = 0; opt < line.options.size(); opt++) {
                ids.add(line.options.get(opt).id());
            }
            lines.add(new CartLineDto(line.dish.id(), line.quantity, ids));
        }
        return new CartDto(restaurant == null ? "" : restaurant.id(), lines, lat, lng, address,
                note, promo, tipCents, paymentMethodId);
    }

    private static void changed() {
        if (listener != null) {
            listener.changed();
        }
    }
}
