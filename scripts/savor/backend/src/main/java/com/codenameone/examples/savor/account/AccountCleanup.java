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
package com.codenameone.examples.savor.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.catalog.Catalog;
import com.codenameone.examples.savor.domain.Restaurant;
import com.codenameone.examples.savor.domain.RestaurantStaff;
import com.codenameone.examples.savor.repository.RestaurantRepository;
import com.codenameone.examples.savor.repository.RestaurantStaffRepository;
import com.codenameone.examples.savor.repository.SavedAddressRepository;
import com.codenameone.examples.savor.repository.FavoriteRepository;
import com.codenameone.examples.savor.repository.CustomerOrderRepository;
import com.codenameone.examples.savor.repository.CourierHoursRepository;
import com.codenameone.examples.savor.repository.CourierStateRepository;
import com.codenameone.examples.savor.repository.LiveTicketRepository;
import com.codenameone.examples.savor.repository.PayoutAccountRepository;
import com.codenameone.examples.savor.repository.PhoneCodeRepository;
import com.codenameone.examples.savor.repository.PreferenceRepository;
import com.codenameone.examples.savor.repository.PushDeviceRepository;
import com.codenameone.examples.savor.repository.DeliveryActivityRepository;
import com.codenameone.examples.savor.repository.DeliveryRepository;

import java.time.Instant;
import java.util.List;

/// What closing or restricting an account does to the rows other parts of the
/// server keep under its name.
///
/// These are questions and statements over whole sets of rows -- everything
/// of one account in a table -- and not the work of the services that own
/// those tables, which is why they are gathered here. Each deletion is a bulk
/// statement, so whatever the session had loaded before it is stale
/// afterwards and must be read again before it is used. It has no
/// transaction of its own: [Accounts] calls it inside one.
@Component
public class AccountCleanup {
    /// The states of a delivery that somebody is still waiting on.
    private static final DeliveryState[] UNDER_WAY = {DeliveryState.REQUESTED, DeliveryState.OFFERED,
        DeliveryState.ACCEPTED, DeliveryState.ARRIVED, DeliveryState.IN_PROGRESS};
    /// The states of an order that has not reached the door or ended.
    private static final OrderState[] ORDERED = {OrderState.PLACED, OrderState.ACCEPTED,
        OrderState.READY, OrderState.PICKED_UP};
    /// The states of a delivery a courier has taken and has yet to finish.
    private static final DeliveryState[] TAKEN = {DeliveryState.ACCEPTED, DeliveryState.ARRIVED,
        DeliveryState.IN_PROGRESS};

    private final DeliveryRepository deliveries;
    private final CourierStateRepository couriers;
    private final CourierHoursRepository hours;
    private final PreferenceRepository preferences;
    private final PayoutAccountRepository payoutAccounts;
    private final PhoneCodeRepository codes;
    private final LiveTicketRepository tickets;
    private final PushDeviceRepository pushDevices;
    private final DeliveryActivityRepository activities;
    private final CustomerOrderRepository orders;
    private final FavoriteRepository favorites;
    private final SavedAddressRepository addresses;
    private final RestaurantStaffRepository staff;
    private final RestaurantRepository restaurants;

    public AccountCleanup(DeliveryRepository deliveries, CourierStateRepository couriers,
            CourierHoursRepository hours, PreferenceRepository preferences,
            PayoutAccountRepository payoutAccounts, PhoneCodeRepository codes,
            LiveTicketRepository tickets, PushDeviceRepository pushDevices,
            DeliveryActivityRepository activities, CustomerOrderRepository orders,
            FavoriteRepository favorites, SavedAddressRepository addresses,
            RestaurantStaffRepository staff, RestaurantRepository restaurants) {
        this.restaurants = restaurants;
        this.deliveries = deliveries;
        this.couriers = couriers;
        this.hours = hours;
        this.preferences = preferences;
        this.payoutAccounts = payoutAccounts;
        this.codes = codes;
        this.tickets = tickets;
        this.pushDevices = pushDevices;
        this.activities = activities;
        this.orders = orders;
        this.favorites = favorites;
        this.addresses = addresses;
        this.staff = staff;
    }

    /// Whether the account is the customer or the courier of a delivery that has not
    /// ended.
    /// An order not yet delivered counts too: one the kitchen has not
    /// answered has no delivery yet, and the card is held for it.
    public boolean hasDeliveryUnderWay(String username) {
        return orders.countByCustomerAndStateIn(username, ORDERED) > 0
                || deliveries.existsByCustomerAndStateInOrCourierAndStateIn(username, UNDER_WAY,
                        username, UNDER_WAY);
    }

    /// Whether the account is onboarding a delivery it has taken and not finished. An
    /// offer it has not answered is not one.
    public boolean drivesDeliveryUnderWay(String username) {
        return deliveries.existsByCourierAndStateIn(username, TAKEN);
    }

    /// Whether the account is in a delivery that has not ended: as its customer, from
    /// the moment it is asked for, or as its courier, once taken. An offer the
    /// courier has not answered is not one; it passes to the next courier.
    public boolean inDeliveryUnderWay(String username) {
        return orders.countByCustomerAndStateIn(username, ORDERED) > 0
                || deliveries.existsByCustomerAndStateInOrCourierAndStateIn(username, UNDER_WAY,
                        username, TAKEN);
    }

    /// Takes a courier off line, when the account stops being allowed to deliver.
    /// Does nothing for an account that never drove.
    public void takeOffLine(String username) {
        couriers.takeOffLine(username);
    }

    /// True when the account is the only staff of an approved restaurant:
    /// closing it would leave the restaurant listed and taking orders with
    /// nobody to cook them, so the closure is refused until an admin adds
    /// somebody else or the restaurant is no longer approved.
    /// Whether a restaurant `username` is the only merchant of still has orders
    /// the kitchen has to see through. Closing that account would take it off
    /// the app (see [#removeEverythingOf]) with nobody left to cook them.
    public boolean soleMerchantWithOpenOrders(String username) {
        List<RestaurantStaff> mine = staff.findByUsername(username);
        for (int iter = 0; iter < mine.size(); iter++) {
            String restaurantId = mine.get(iter).getRestaurantId();
            if (staff.findByRestaurantId(restaurantId).size() < 2
                    && !orders.findByRestaurantIdAndStateInOrderByPlacedAtAsc(restaurantId, OPEN).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static final OrderState[] OPEN = open();

    private static OrderState[] open() {
        java.util.List<OrderState> out = new java.util.ArrayList<OrderState>();
        OrderState[] all = OrderState.values();
        for (int iter = 0; iter < all.length; iter++) {
            if (all[iter].isActive()) {
                out.add(all[iter]);
            }
        }
        return out.toArray(new OrderState[out.size()]);
    }

    public boolean soleMerchantOfListed(String username) {
        List<RestaurantStaff> mine = staff.findByUsername(username);
        for (int iter = 0; iter < mine.size(); iter++) {
            String restaurantId = mine.get(iter).getRestaurantId();
            Restaurant restaurant = restaurants.findById(restaurantId);
            if (restaurant != null && Catalog.APPROVED.equals(restaurant.getStatus())
                    && staff.findByRestaurantId(restaurantId).size() < 2) {
                return true;
            }
        }
        return false;
    }

    /// Deletes what an account leaves in the tables that are keyed by it and
    /// belong to nobody's `forget`: its preferences, where it is paid, the
    /// hours it drove, its place on the map, a code it was texted, the
    /// tickets it held for the live connection, and the phones it was sent
    /// notifications on.
    public void removeEverythingOf(String username) {
        preferences.deleteByUsername(username);
        payoutAccounts.deleteByUsername(username);
        hours.deleteByUsername(username);
        couriers.deleteByUsername(username);
        codes.deleteByUsername(username);
        tickets.deleteByUsername(username);
        pushDevices.deleteByUsername(username);
        activities.deleteByUsername(username);
        favorites.deleteByUsername(username);
        addresses.deleteByUsername(username);
        // A restaurant somebody else still works at stays listed, as its orders
        // and its menu do. One left with nobody is taken off the app: there is
        // no screen that hands a restaurant to another account, and
        // Restaurants.suspend will not list it again until it has staff who can
        // sign in. Its orders and menu stay for the records.
        List<RestaurantStaff> mine = staff.findByUsername(username);
        for (int iter = 0; iter < mine.size(); iter++) {
            String restaurantId = mine.get(iter).getRestaurantId();
            Restaurant restaurant = restaurants.findById(restaurantId);
            if (restaurant != null && Catalog.APPROVED.equals(restaurant.getStatus())
                    && staff.findByRestaurantId(restaurantId).size() < 2) {
                restaurant.setStatus(Catalog.SUSPENDED);
                restaurant.setReason("Its only merchant closed their account");
                restaurant.setUpdatedAt(Instant.now());
            }
        }
        staff.deleteByUsername(username);
    }
}
