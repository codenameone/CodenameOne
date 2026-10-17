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
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.orm.session.Identifier;
import com.codenameone.examples.savor.api.CartDto;
import com.codenameone.examples.savor.api.CartLineDto;
import com.codenameone.examples.savor.api.OrderLineDto;
import com.codenameone.examples.savor.api.PricingDto;
import com.codenameone.examples.savor.api.QuoteDto;
import com.codenameone.examples.savor.domain.Dish;
import com.codenameone.examples.savor.domain.DishOption;
import com.codenameone.examples.savor.domain.Pricing;
import com.codenameone.examples.savor.domain.Promo;
import com.codenameone.examples.savor.domain.Restaurant;
import com.codenameone.examples.savor.geo.Geo;
import com.codenameone.examples.savor.repository.DishOptionRepository;
import com.codenameone.examples.savor.repository.DishRepository;
import com.codenameone.examples.savor.repository.PricingRepository;
import com.codenameone.examples.savor.repository.PromoRepository;
import com.codenameone.examples.savor.repository.PromoUseRepository;
import com.codenameone.examples.savor.repository.RestaurantRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// What an order costs, worked out on the server from the menu as it stands:
/// the client sends dish and option ids and quantities, never prices.
///
/// The delivery fee is a base plus a price per kilometre from the restaurant to
/// the door, times the surge; an order under the small-order threshold pays a
/// flat surcharge; the service fee and tax are percentages of the subtotal; a
/// promotion takes its discount off the subtotal. The platform keeps its
/// commission from the restaurant's share, and the courier is paid the delivery
/// fee and the tip.
@Component
public class Quotes {
    /// Roads are longer than the straight line between two points.
    public static final double ROAD_FACTOR = 1.3d;
    /// About 30 km/h, a courier in town.
    public static final double METERS_PER_SECOND = 8.33d;
    /// The furthest a restaurant delivers.
    public static final double MAX_METERS = 15000d;
    /// The most of one line anyone orders.
    static final int MAX_QUANTITY = 50;

    private final PricingRepository prices;
    private final RestaurantRepository restaurants;
    private final DishRepository dishes;
    private final DishOptionRepository options;
    private final PromoRepository promos;
    private final PromoUseRepository promoUses;
    private final PricingDto defaults;

    public Quotes(PricingRepository prices, RestaurantRepository restaurants,
            DishRepository dishes, DishOptionRepository options, PromoRepository promos,
            PromoUseRepository promoUses,
            @Value("${savor.currency:USD}") String currency,
            @Value("${savor.fee.baseCents:199}") int baseCents,
            @Value("${savor.fee.perKmCents:60}") int perKmCents,
            @Value("${savor.fee.smallOrderCents:1200}") int smallOrderCents,
            @Value("${savor.fee.smallOrderFeeCents:200}") int smallOrderFeeCents,
            @Value("${savor.fee.serviceFeePercent:10}") int serviceFeePercent,
            @Value("${savor.fee.taxPercent:8}") int taxPercent,
            @Value("${savor.fee.commissionPercent:20}") int commissionPercent) {
        this.prices = prices;
        this.restaurants = restaurants;
        this.dishes = dishes;
        this.options = options;
        this.promos = promos;
        this.promoUses = promoUses;
        defaults = new PricingDto(currency, baseCents, perKmCents, 1d, smallOrderCents,
                smallOrderFeeCents, serviceFeePercent, taxPercent, commissionPercent);
    }

    public String currency() {
        return defaults.currency();
    }

    @Transactional(readOnly = true)
    public PricingDto pricing() throws IOException {
        return toDto(prices.findById(PricingRepository.ONLY));
    }

    private PricingDto toDto(Pricing row) {
        if (row == null || row.getUpdatedAt() == 0L) {
            return defaults;
        }
        return new PricingDto(defaults.currency(), row.getBaseCents(), row.getPerKmCents(),
                row.getSurge(), row.getSmallOrderCents(), row.getSmallOrderFeeCents(),
                row.getFeePercent(), row.getTaxPercent(), row.getCommissionPercent());
    }

    @Transactional
    public PricingDto save(String by, PricingDto wanted) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "No prices to save");
        }
        amount(wanted.baseCents(), "the base delivery fee");
        amount(wanted.perKmCents(), "the price per kilometre");
        amount(wanted.smallOrderCents(), "the small-order threshold");
        amount(wanted.smallOrderFeeCents(), "the small-order fee");
        between(wanted.surge(), 1d, 5d, "the surge multiplier");
        between(wanted.serviceFeePercent(), 0d, 50d, "the service fee");
        between(wanted.taxPercent(), 0d, 50d, "the tax");
        between(wanted.commissionPercent(), 0d, 90d, "the commission");
        if (prices.change(PricingRepository.ONLY.intValue(), wanted.baseCents(),
                wanted.perKmCents(), wanted.surge(), wanted.smallOrderCents(),
                wanted.smallOrderFeeCents(), wanted.serviceFeePercent(), wanted.taxPercent(),
                wanted.commissionPercent(), System.currentTimeMillis(), by) == 0) {
            throw new IllegalStateException("The pricing row is missing; the V6 migration "
                    + "creates it");
        }
        return new PricingDto(defaults.currency(), wanted.baseCents(), wanted.perKmCents(),
                wanted.surge(), wanted.smallOrderCents(), wanted.smallOrderFeeCents(),
                wanted.serviceFeePercent(), wanted.taxPercent(), wanted.commissionPercent());
    }

    private static void amount(long cents, String what) {
        if (cents < 0 || cents > 100000) {
            throw new ResponseStatusException(400, "Check " + what);
        }
    }

    private static void between(double value, double low, double high, String what) {
        if (!(value >= low && value <= high)) {
            throw new ResponseStatusException(400, "Check " + what);
        }
    }

    /// The road distance from a restaurant to a door, estimated.
    public static double roadMeters(double fromLat, double fromLng, double toLat, double toLng) {
        return Geo.meters(fromLat, fromLng, toLat, toLng) * ROAD_FACTOR;
    }

    /// Minutes of travel for a road distance, at least one.
    public static int travelMinutes(double roadMeters) {
        return (int) Math.max(1L, Math.round(roadMeters / METERS_PER_SECOND / 60d));
    }

    /// The delivery fee for a road distance.
    public static long deliveryFee(PricingDto pricing, double roadMeters) {
        return Math.round((pricing.baseCents() + roadMeters / 1000d * pricing.perKmCents())
                * pricing.surge());
    }

    /// The platform's share of a subtotal.
    public static long commission(PricingDto pricing, long subtotalCents) {
        return Math.round(subtotalCents * pricing.commissionPercent() / 100d);
    }

    /// Whether a restaurant takes orders at this minute of the day.
    public static boolean open(Restaurant restaurant, int minuteOfDay) {
        if (restaurant.isPaused() || !"APPROVED".equals(restaurant.getStatus())) {
            return false;
        }
        int from = restaurant.getOpensAt();
        int to = restaurant.getClosesAt();
        if (from == to) {
            return true;
        }
        if (from < to) {
            return minuteOfDay >= from && minuteOfDay < to;
        }
        return minuteOfDay >= from || minuteOfDay < to;
    }

    /// The minute of the day now, in the server's clock. A sample with one
    /// city keeps one time zone; a real platform keeps one per restaurant.
    public static int minuteOfDay(long now) {
        return (int) ((now % 86400000L) / 60000L);
    }

    /// A worked-out order, kept together for [Orders] to store.
    public static final class Priced {
        public Restaurant restaurant;
        public QuoteDto quote;
        public List<OrderLineDto> lines;
        public long commissionCents;
        public Promo promo;
        public double roadMeters;
    }

    /// Prices a cart against the menu as it stands.
    ///
    /// @param username who is ordering, for one-use promotions; null skips that check
    @Transactional(readOnly = true)
    public Priced quote(String username, CartDto cart) throws IOException {
        if (cart == null || cart.restaurantId() == null || cart.lines() == null
                || cart.lines().isEmpty()) {
            throw new ResponseStatusException(400, "The cart is empty");
        }
        if (cart.lines().size() > 100) {
            throw new ResponseStatusException(400, "That is too many lines for one order");
        }
        if (!Geo.isPoint(cart.dropoffLat(), cart.dropoffLng())) {
            throw new ResponseStatusException(400, "Choose where the order goes");
        }
        Restaurant restaurant = restaurants.findById(cart.restaurantId());
        if (restaurant == null || !"APPROVED".equals(restaurant.getStatus())) {
            throw new ResponseStatusException(404, "That restaurant is not taking orders");
        }
        double meters = roadMeters(restaurant.getLat(), restaurant.getLng(), cart.dropoffLat(),
                cart.dropoffLng());
        if (meters > MAX_METERS) {
            throw new ResponseStatusException(400, restaurant.getName()
                    + " does not deliver that far");
        }
        Map<String, Dish> menu = new HashMap<String, Dish>();
        List<String> ids = new ArrayList<String>();
        for (Dish dish : dishes.findByRestaurantIdOrderByPositionAsc(restaurant.getId())) {
            menu.put(dish.getId(), dish);
            ids.add(dish.getId());
        }
        Map<String, DishOption> extras = new HashMap<String, DishOption>();
        if (!ids.isEmpty()) {
            for (DishOption option : options.findByDishIdIn(ids)) {
                extras.put(option.getId(), option);
            }
        }
        List<OrderLineDto> lines = new ArrayList<OrderLineDto>();
        long subtotal = 0L;
        for (CartLineDto line : cart.lines()) {
            Dish dish = line == null ? null : menu.get(line.dishId());
            if (dish == null) {
                throw new ResponseStatusException(400, "A dish in the cart is no longer on the "
                        + "menu");
            }
            if (!dish.isAvailable()) {
                throw new ResponseStatusException(409, dish.getName() + " is sold out");
            }
            if (line.quantity() < 1 || line.quantity() > MAX_QUANTITY) {
                throw new ResponseStatusException(400, "Check how many " + dish.getName());
            }
            long unit = dish.getPriceCents();
            StringBuilder names = new StringBuilder();
            if (line.optionIds() != null) {
                for (String optionId : line.optionIds()) {
                    DishOption option = extras.get(optionId);
                    if (option == null || !option.getDishId().equals(dish.getId())) {
                        throw new ResponseStatusException(400, "An extra for " + dish.getName()
                                + " is no longer offered");
                    }
                    unit += option.getPriceCents();
                    if (names.length() > 0) {
                        names.append(", ");
                    }
                    names.append(option.getName());
                }
            }
            long cents = unit * line.quantity();
            subtotal += cents;
            lines.add(new OrderLineDto(dish.getId(), dish.getName(), names.toString(),
                    line.quantity(), unit, cents));
        }
        PricingDto pricing = pricing();
        long delivery = deliveryFee(pricing, meters);
        long small = subtotal < pricing.smallOrderCents() ? pricing.smallOrderFeeCents() : 0L;
        long service = Math.round(subtotal * pricing.serviceFeePercent() / 100d);
        long discount = 0L;
        String message = "";
        Promo promo = null;
        String code = cart.promoCode() == null ? "" : cart.promoCode().trim();
        if (code.length() > 0) {
            promo = promos.findById(promoCode(code));
            if (promo == null || !promo.isActive()) {
                message = "That code is not valid";
                promo = null;
            } else if (subtotal < promo.getMinimumCents()) {
                message = "That code needs a bigger order";
                promo = null;
            } else if (username != null
                    && promoUses.findById(Identifier.of(promo.getCode(), username)) != null) {
                message = "You have already used that code";
                promo = null;
            } else {
                discount = Math.min(subtotal, promo.getAmountOffCents()
                        + Math.round(subtotal * promo.getPercentOff() / 100d));
                message = "Code applied";
            }
        }
        long tax = Math.round((subtotal - discount) * pricing.taxPercent() / 100d);
        long tip = cart.tipCents();
        if (tip < 0 || tip > 100000) {
            throw new ResponseStatusException(400, "Check the tip");
        }
        long total = subtotal - discount + delivery + small + service + tax + tip;
        int eta = restaurant.getPrepMinutes() + travelMinutes(meters);
        Priced priced = new Priced();
        priced.restaurant = restaurant;
        priced.lines = lines;
        priced.promo = promo;
        priced.roadMeters = meters;
        priced.commissionCents = commission(pricing, subtotal - discount);
        priced.quote = new QuoteDto(pricing.currency(), lines, subtotal, delivery, service, small,
                tax, discount, tip, total, eta, message);
        return priced;
    }

    /// A promotion code in its stored form: ASCII upper case, folded by hand
    /// because `toUpperCase()` follows the server's locale.
    public static String promoCode(String code) {
        char[] chars = code.trim().toCharArray();
        for (int iter = 0; iter < chars.length; iter++) {
            if (chars[iter] >= 'a' && chars[iter] <= 'z') {
                chars[iter] = (char) (chars[iter] - 32);
            }
        }
        return new String(chars);
    }
}
