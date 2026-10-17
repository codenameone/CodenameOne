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
package com.codenameone.examples.morsel.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.morsel.api.DeliveryState;
import com.codenameone.examples.morsel.repository.CourierHoursRepository;
import com.codenameone.examples.morsel.repository.CourierStateRepository;
import com.codenameone.examples.morsel.repository.LiveTicketRepository;
import com.codenameone.examples.morsel.repository.PayoutAccountRepository;
import com.codenameone.examples.morsel.repository.PhoneCodeRepository;
import com.codenameone.examples.morsel.repository.PreferenceRepository;
import com.codenameone.examples.morsel.repository.PushDeviceRepository;
import com.codenameone.examples.morsel.repository.DeliveryActivityRepository;
import com.codenameone.examples.morsel.repository.DeliveryRepository;

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

    public AccountCleanup(DeliveryRepository deliveries, CourierStateRepository couriers,
            CourierHoursRepository hours, PreferenceRepository preferences,
            PayoutAccountRepository payoutAccounts, PhoneCodeRepository codes,
            LiveTicketRepository tickets, PushDeviceRepository pushDevices,
            DeliveryActivityRepository activities) {
        this.deliveries = deliveries;
        this.couriers = couriers;
        this.hours = hours;
        this.preferences = preferences;
        this.payoutAccounts = payoutAccounts;
        this.codes = codes;
        this.tickets = tickets;
        this.pushDevices = pushDevices;
        this.activities = activities;
    }

    /// Whether the account is the customer or the courier of a delivery that has not
    /// ended.
    public boolean hasDeliveryUnderWay(String username) {
        return deliveries.existsByCustomerAndStateInOrCourierAndStateIn(username, UNDER_WAY, username,
                UNDER_WAY);
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
        return deliveries.existsByCustomerAndStateInOrCourierAndStateIn(username, UNDER_WAY, username,
                TAKEN);
    }

    /// Takes a courier off line, when the account stops being allowed to drive.
    /// Does nothing for an account that never drove.
    public void takeOffLine(String username) {
        couriers.takeOffLine(username);
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
    }
}
