/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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

    public AccountCleanup(RideRepository rides, DriverStateRepository drivers,
            DriverHoursRepository hours, PreferenceRepository preferences,
            PayoutAccountRepository payoutAccounts, PhoneCodeRepository codes,
            LiveTicketRepository tickets) {
        this.rides = rides;
        this.drivers = drivers;
        this.hours = hours;
        this.preferences = preferences;
        this.payoutAccounts = payoutAccounts;
        this.codes = codes;
        this.tickets = tickets;
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
    /// hours it drove, its place on the map, a code it was texted and the
    /// tickets it held for the live connection.
    public void removeEverythingOf(String username) {
        preferences.deleteByUsername(username);
        payoutAccounts.deleteByUsername(username);
        hours.deleteByUsername(username);
        drivers.deleteByUsername(username);
        codes.deleteByUsername(username);
        tickets.deleteByUsername(username);
    }
}
