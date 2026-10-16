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
package com.codenameone.examples.wayline.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.repository.DriverHoursRepository;
import com.codenameone.examples.wayline.repository.DriverStateRepository;
import com.codenameone.examples.wayline.repository.LiveTicketRepository;
import com.codenameone.examples.wayline.repository.PayoutAccountRepository;
import com.codenameone.examples.wayline.repository.PhoneCodeRepository;
import com.codenameone.examples.wayline.repository.PreferenceRepository;
import com.codenameone.examples.wayline.repository.PushDeviceRepository;
import com.codenameone.examples.wayline.repository.RideActivityRepository;
import com.codenameone.examples.wayline.repository.RideRepository;

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
    /// The states of a ride that somebody is still waiting on.
    private static final RideState[] UNDER_WAY = {RideState.REQUESTED, RideState.OFFERED,
        RideState.ACCEPTED, RideState.ARRIVED, RideState.IN_PROGRESS};
    /// The states of a ride a driver has taken and has yet to finish.
    private static final RideState[] TAKEN = {RideState.ACCEPTED, RideState.ARRIVED,
        RideState.IN_PROGRESS};

    private final RideRepository rides;
    private final DriverStateRepository drivers;
    private final DriverHoursRepository hours;
    private final PreferenceRepository preferences;
    private final PayoutAccountRepository payoutAccounts;
    private final PhoneCodeRepository codes;
    private final LiveTicketRepository tickets;
    private final PushDeviceRepository pushDevices;
    private final RideActivityRepository activities;

    public AccountCleanup(RideRepository rides, DriverStateRepository drivers,
            DriverHoursRepository hours, PreferenceRepository preferences,
            PayoutAccountRepository payoutAccounts, PhoneCodeRepository codes,
            LiveTicketRepository tickets, PushDeviceRepository pushDevices,
            RideActivityRepository activities) {
        this.rides = rides;
        this.drivers = drivers;
        this.hours = hours;
        this.preferences = preferences;
        this.payoutAccounts = payoutAccounts;
        this.codes = codes;
        this.tickets = tickets;
        this.pushDevices = pushDevices;
        this.activities = activities;
    }

    /// Whether the account is the rider or the driver of a ride that has not
    /// ended.
    public boolean hasRideUnderWay(String username) {
        return rides.existsByRiderAndStateInOrDriverAndStateIn(username, UNDER_WAY, username,
                UNDER_WAY);
    }

    /// Whether the account is driving a ride it has taken and not finished. An
    /// offer it has not answered is not one.
    public boolean drivesRideUnderWay(String username) {
        return rides.existsByDriverAndStateIn(username, TAKEN);
    }

    /// Whether the account is in a ride that has not ended: as its rider, from
    /// the moment it is asked for, or as its driver, once taken. An offer the
    /// driver has not answered is not one; it passes to the next driver.
    public boolean inRideUnderWay(String username) {
        return rides.existsByRiderAndStateInOrDriverAndStateIn(username, UNDER_WAY, username,
                TAKEN);
    }

    /// Takes a driver off line, when the account stops being allowed to drive.
    /// Does nothing for an account that never drove.
    public void takeOffLine(String username) {
        drivers.takeOffLine(username);
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
        drivers.deleteByUsername(username);
        codes.deleteByUsername(username);
        tickets.deleteByUsername(username);
        pushDevices.deleteByUsername(username);
        activities.deleteByUsername(username);
    }
}
