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

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.savor.Days;
import com.codenameone.examples.savor.Ids;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.api.PricingDto;
import com.codenameone.examples.savor.domain.CourierState;
import com.codenameone.examples.savor.domain.CustomerOrder;
import com.codenameone.examples.savor.domain.Delivery;
import com.codenameone.examples.savor.domain.DeliveryEvent;
import com.codenameone.examples.savor.domain.Dish;
import com.codenameone.examples.savor.domain.OrderEvent;
import com.codenameone.examples.savor.domain.OrderLine;
import com.codenameone.examples.savor.domain.Payment;
import com.codenameone.examples.savor.domain.Payout;
import com.codenameone.examples.savor.domain.PayoutAccount;
import com.codenameone.examples.savor.domain.Restaurant;
import com.codenameone.examples.savor.pay.Payments;
import com.codenameone.examples.savor.repository.CourierStateRepository;
import com.codenameone.examples.savor.repository.CustomerOrderRepository;
import com.codenameone.examples.savor.repository.DeliveryEventRepository;
import com.codenameone.examples.savor.repository.DeliveryRepository;
import com.codenameone.examples.savor.repository.DishRepository;
import com.codenameone.examples.savor.repository.OrderEventRepository;
import com.codenameone.examples.savor.repository.OrderLineRepository;
import com.codenameone.examples.savor.repository.PaymentRepository;
import com.codenameone.examples.savor.repository.PayoutAccountRepository;
import com.codenameone.examples.savor.repository.PayoutRepository;
import com.codenameone.examples.savor.repository.RestaurantRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// A fortnight of finished orders for a development server, so the dashboards,
/// the histories and the couriers' earnings have something in them from the
/// first start. Written straight to the tables, as the services would have left
/// them, with a fixed sequence so every runtime writes the same fortnight.
@Component
public class DemoOrders {
    private static final int DAYS = 14;
    /// Where the demo customers have food brought to.
    private static final Object[][] DOORS = {
        {"Coit Tower", 37.8024, -122.4058},
        {"Palace of Fine Arts", 37.8029, -122.4484},
        {"Union Square", 37.7880, -122.4075},
        {"Fort Mason", 37.8066, -122.4310},
        {"Transamerica Pyramid", 37.7952, -122.4028},
        {"Russian Hill", 37.8011, -122.4194},
        {"North Beach", 37.8061, -122.4103}};
    /// Lunch and dinner, the busy hours more than once.
    private static final int[] HOURS = {8, 11, 12, 12, 13, 13, 15, 18, 18, 19, 19, 20, 21, 22};

    private final RestaurantRepository restaurants;
    private final DishRepository dishes;
    private final CustomerOrderRepository orders;
    private final OrderLineRepository lines;
    private final OrderEventRepository orderEvents;
    private final DeliveryRepository deliveries;
    private final DeliveryEventRepository deliveryEvents;
    private final PaymentRepository paid;
    private final CourierStateRepository couriersState;
    private final PayoutAccountRepository payoutAccounts;
    private final PayoutRepository payouts;
    private final Quotes quotes;
    private final Payments payments;
    private long sequence = 20240229L;

    public DemoOrders(RestaurantRepository restaurants, DishRepository dishes,
            CustomerOrderRepository orders, OrderLineRepository lines,
            OrderEventRepository orderEvents, DeliveryRepository deliveries,
            DeliveryEventRepository deliveryEvents, PaymentRepository paid,
            CourierStateRepository couriersState, PayoutAccountRepository payoutAccounts,
            PayoutRepository payouts, Quotes quotes, Payments payments) {
        this.restaurants = restaurants;
        this.dishes = dishes;
        this.orders = orders;
        this.lines = lines;
        this.orderEvents = orderEvents;
        this.deliveries = deliveries;
        this.deliveryEvents = deliveryEvents;
        this.paid = paid;
        this.couriersState = couriersState;
        this.payoutAccounts = payoutAccounts;
        this.payouts = payouts;
        this.quotes = quotes;
        this.payments = payments;
    }

    /// Writes the fortnight, shared out among these customers, couriers and
    /// every approved restaurant. It ends yesterday, so today's numbers are
    /// only what is ordered today.
    @Transactional
    public void seed(String[] customers, String[] couriers) throws IOException {
        List<Restaurant> kitchens = restaurants.findByStatusOrderByCreatedAtDesc("APPROVED");
        if (kitchens.isEmpty()) {
            return;
        }
        List<List<Dish>> menus = new ArrayList<List<Dish>>();
        for (int iter = 0; iter < kitchens.size(); iter++) {
            menus.add(dishes.findByRestaurantIdOrderByPositionAsc(kitchens.get(iter).getId()));
        }
        long now = System.currentTimeMillis();
        long today = Days.start(now) - Days.MILLIS;
        PricingDto pricing = quotes.pricing();
        long[] stars = new long[couriers.length];
        long[] rated = new long[couriers.length];
        long[] earnedEarlier = new long[couriers.length];
        long[] lastSeen = new long[couriers.length];
        long[] kitchenStars = new long[kitchens.size()];
        long[] kitchenRated = new long[kitchens.size()];
        for (int day = DAYS - 1; day >= 0; day--) {
            int placed = 4 + next(5);
            for (int number = 0; number < placed; number++) {
                String customer = customers[next(customers.length)];
                int courierIndex = next(couriers.length);
                String courier = couriers[courierIndex];
                int kitchenIndex = next(kitchens.size());
                Restaurant kitchen = kitchens.get(kitchenIndex);
                List<Dish> menu = menus.get(kitchenIndex);
                if (menu.isEmpty()) {
                    continue;
                }
                int door = next(DOORS.length);
                double meters = Quotes.roadMeters(kitchen.getLat(), kitchen.getLng(),
                        real(door, 1), real(door, 2));
                long at = today - day * Days.MILLIS + HOURS[next(HOURS.length)] * 3600000L
                        + next(3600) * 1000L;
                long prep = kitchen.getPrepMinutes() * 60000L;
                long ride = Quotes.travelMinutes(meters) * 60000L;

                int roll = next(20);
                OrderState state = roll < 16 ? OrderState.DELIVERED
                        : roll == 16 ? OrderState.REJECTED
                        : roll == 17 ? OrderState.NO_COURIERS : OrderState.CANCELLED_BY_CUSTOMER;
                boolean delivered = OrderState.DELIVERED == state;
                String[] method = next(4) == 0 ? new String[] {Payments.CASH, Payments.CASH_LABEL}
                        : payments.choose(customer, null);
                boolean cash = Payments.CASH.equals(method[0]);

                String id = Ids.next();
                long subtotal = 0L;
                int count = 1 + next(3);
                for (int line = 0; line < count; line++) {
                    Dish dish = menu.get(next(menu.size()));
                    int quantity = 1 + next(2);
                    OrderLine row = new OrderLine();
                    row.setId(Ids.next());
                    row.setOrderId(id);
                    row.setDishId(dish.getId());
                    row.setName(dish.getName());
                    row.setOptions("");
                    row.setQuantity(quantity);
                    row.setUnitCents(dish.getPriceCents());
                    row.setLineCents(dish.getPriceCents() * quantity);
                    row.setPosition(line);
                    subtotal += row.getLineCents();
                    lines.insert(row);
                }
                long fee = Quotes.deliveryFee(pricing, meters);
                long small = subtotal < pricing.smallOrderCents() ? pricing.smallOrderFeeCents()
                        : 0L;
                long service = Math.round(subtotal * pricing.serviceFeePercent() / 100d);
                long tax = Math.round(subtotal * pricing.taxPercent() / 100d);
                long tip = delivered && !cash && next(3) != 0 ? subtotal * 15L / 100L : 0L;
                long total = subtotal + fee + small + service + tax + tip;
                long accepted = at + (60 + next(120)) * 1000L;
                long ready = accepted + prep;
                long picked = ready + (60 + next(240)) * 1000L;
                long ended = delivered ? picked + ride
                        : OrderState.REJECTED == state ? at + 120000L
                        : OrderState.NO_COURIERS == state ? accepted + 900000L : at + 45000L;
                int given = 0;
                if (delivered && next(4) != 0) {
                    int liked = next(10);
                    given = liked == 0 ? 3 : liked < 4 ? 4 : 5;
                    stars[courierIndex] += given;
                    rated[courierIndex]++;
                    kitchenStars[kitchenIndex] += given;
                    kitchenRated[kitchenIndex]++;
                }

                CustomerOrder order = new CustomerOrder();
                order.setId(id);
                order.setCustomer(customer);
                order.setRestaurantId(kitchen.getId());
                order.setRestaurantName(kitchen.getName());
                order.setState(state);
                order.setSubtotalCents(subtotal);
                order.setDeliveryFeeCents(fee);
                order.setServiceFeeCents(service);
                order.setSmallOrderCents(small);
                order.setTaxCents(tax);
                order.setTipCents(tip);
                order.setTotalCents(total);
                order.setCommissionCents(Quotes.commission(pricing, subtotal));
                order.setCurrency(pricing.currency());
                order.setPromoCode("");
                order.setDropoffLat(real(door, 1));
                order.setDropoffLng(real(door, 2));
                order.setDropoffAddress(String.valueOf(DOORS[door][0]));
                order.setNote("");
                order.setMethodId(method[0]);
                order.setMethodLabel(method[1]);
                order.setAuthorizationRef(cash ? Payments.CASH : "sim_ch_order-" + id);
                order.setPaymentStatus(delivered ? Payments.PAID
                        : cash ? Payments.PENDING : Payments.RELEASED);
                order.setPrepMinutes(kitchen.getPrepMinutes());
                order.setPlacedAt(Instant.ofEpochMilli(at));
                order.setUpdatedAt(Instant.ofEpochMilli(ended));
                order.setAcceptedAt(OrderState.REJECTED == state
                        || OrderState.CANCELLED_BY_CUSTOMER == state ? Instant.EPOCH : Instant.ofEpochMilli(accepted));
                order.setReadyAt(delivered ? Instant.ofEpochMilli(ready) : Instant.EPOCH);
                order.setDeliveredAt(delivered ? Instant.ofEpochMilli(ended) : Instant.EPOCH);
                order.setReason(OrderState.REJECTED == state ? "Out of ingredients"
                        : OrderState.NO_COURIERS == state ? "No courier was free to bring it"
                        : OrderState.CANCELLED_BY_CUSTOMER == state ? "Cancelled by the customer"
                        : "");
                order.setStars(given);
                orders.insert(order);
                orderEvent(id, OrderState.PLACED, customer, at);
                if (OrderState.REJECTED == state) {
                    orderEvent(id, state, "", ended);
                    continue;
                }
                if (OrderState.CANCELLED_BY_CUSTOMER == state) {
                    orderEvent(id, state, customer, ended);
                    continue;
                }
                orderEvent(id, OrderState.ACCEPTED, "", accepted);

                Delivery delivery = new Delivery();
                delivery.setId(id);
                delivery.setState(delivered ? DeliveryState.COMPLETED : DeliveryState.NO_COURIERS);
                delivery.setCustomer(customer);
                delivery.setCourier(delivered ? courier : "");
                delivery.setPickupLat(kitchen.getLat());
                delivery.setPickupLng(kitchen.getLng());
                delivery.setPickupAddress(kitchen.getName() + ", " + kitchen.getAddress());
                delivery.setDropoffLat(real(door, 1));
                delivery.setDropoffLng(real(door, 2));
                delivery.setDropoffAddress(String.valueOf(DOORS[door][0]));
                delivery.setFareCents(fee);
                delivery.setFeeCents(fee);
                // As Deliveries.dispatch writes it: the checkout tip is the
                // order's, and the delivery's own tip is one added after it
                // arrived -- a receipt adds that one on top.
                delivery.setTipCents(0L);
                delivery.setCourierCents(fee + tip);
                delivery.setCurrency(pricing.currency());
                delivery.setDistanceMeters(meters);
                delivery.setDurationSeconds(meters / Quotes.METERS_PER_SECOND);
                delivery.setRequestedAt(Instant.ofEpochMilli(accepted));
                delivery.setUpdatedAt(Instant.ofEpochMilli(ended));
                delivery.setSearchUntil(Instant.ofEpochMilli(accepted + 900000L));
                delivery.setStars(given);
                delivery.setPaymentStatus(delivered ? Payments.PAID : Payments.PENDING);
                delivery.setMethodId(method[0]);
                delivery.setMethodLabel(method[1]);
                delivery.setNote("");
                deliveries.insert(delivery);
                deliveryEvent(id, DeliveryState.REQUESTED, "", accepted);
                if (!delivered) {
                    deliveryEvent(id, DeliveryState.NO_COURIERS, "", ended);
                    orderEvent(id, OrderState.NO_COURIERS, "", ended);
                    continue;
                }
                deliveryEvent(id, DeliveryState.OFFERED, "", accepted + 2000L);
                deliveryEvent(id, DeliveryState.ACCEPTED, courier, accepted + 20000L);
                orderEvent(id, OrderState.READY, "", ready);
                deliveryEvent(id, DeliveryState.ARRIVED, courier, ready - 60000L);
                deliveryEvent(id, DeliveryState.IN_PROGRESS, courier, picked);
                orderEvent(id, OrderState.PICKED_UP, courier, picked);
                deliveryEvent(id, DeliveryState.COMPLETED, courier, ended);
                orderEvent(id, OrderState.DELIVERED, courier, ended);
                Payment payment = new Payment();
                payment.setDeliveryId(id);
                payment.setAmountCents(total);
                payment.setCurrency(pricing.currency());
                payment.setStatus(Payments.PAID);
                payment.setProviderRef(cash ? Payments.CASH : "sim_ch_order-" + id);
                payment.setTipRef("");
                payment.setCreatedAt(Instant.ofEpochMilli(ended));
                paid.insert(payment);
                lastSeen[courierIndex] = Math.max(lastSeen[courierIndex], ended);
                if (ended < now - 7L * Days.MILLIS) {
                    earnedEarlier[courierIndex] += fee + tip;
                }
            }
        }
        for (int iter = 0; iter < kitchens.size(); iter++) {
            if (kitchenRated[iter] > 0L) {
                Restaurant kitchen = restaurants.findById(kitchens.get(iter).getId());
                kitchen.setRatingTotal(kitchen.getRatingTotal() + kitchenStars[iter]);
                kitchen.setRatingCount(kitchen.getRatingCount() + kitchenRated[iter]);
            }
        }
        for (int iter = 0; iter < couriers.length; iter++) {
            CourierState state = couriersState.findById(couriers[iter]);
            boolean first = state == null;
            if (first) {
                state = new CourierState();
                state.setUsername(couriers[iter]);
            }
            state.setLat(real(iter % DOORS.length, 1));
            state.setLng(real(iter % DOORS.length, 2));
            state.setLastSeen(Instant.ofEpochMilli(lastSeen[iter]));
            state.setRatingSum(stars[iter]);
            state.setRatingCount(rated[iter]);
            if (first) {
                couriersState.insert(state);
            }
            if (earnedEarlier[iter] > 0L) {
                // What the first week earned was cashed out at the end of it.
                PayoutAccount account = new PayoutAccount();
                account.setUsername(couriers[iter]);
                account.setHolder(couriers[iter]);
                account.setBankName("Bay Credit Union");
                account.setLast4("67" + (10 + iter));
                payoutAccounts.insert(account);
                Payout payout = new Payout();
                payout.setId(Ids.next());
                payout.setCourier(couriers[iter]);
                payout.setAmountCents(earnedEarlier[iter]);
                payout.setStatus("paid");
                payout.setCreatedAt(Instant.ofEpochMilli(now - 7L * Days.MILLIS));
                payout.setDestination("Bay Credit Union ****67" + (10 + iter));
                payouts.insert(payout);
            }
        }
    }

    private void orderEvent(String order, OrderState state, String actor, long at)
            throws IOException {
        OrderEvent event = new OrderEvent();
        event.setId(Ids.next());
        event.setOrderId(order);
        event.setState(state);
        event.setActor(actor);
        event.setCreatedAt(Instant.ofEpochMilli(at));
        orderEvents.insert(event);
    }

    private void deliveryEvent(String delivery, DeliveryState state, String actor, long at)
            throws IOException {
        DeliveryEvent event = new DeliveryEvent();
        event.setId(Ids.next());
        event.setDeliveryId(delivery);
        event.setState(state);
        event.setActor(actor);
        event.setCreatedAt(Instant.ofEpochMilli(at));
        deliveryEvents.insert(event);
    }

    private static double real(int place, int column) {
        Object value = DOORS[place][column];
        return value instanceof Number ? ((Number) value).doubleValue() : 0d;
    }

    /// The next number of the sequence, from 0 to `below` less one: a linear
    /// congruential generator, which is all a made-up fortnight needs and is
    /// the same on every runtime.
    private int next(int below) {
        sequence = (sequence * 1103515245L + 12345L) & 0x7fffffffL;
        return (int) ((sequence >> 8) % below);
    }
}
