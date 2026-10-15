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
package com.codenameone.examples.wayline.driver;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.session.Identifier;
import com.codenameone.examples.wayline.Days;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.Rows;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.api.DayAmountDto;
import com.codenameone.examples.wayline.api.EarningsSummaryDto;
import com.codenameone.examples.wayline.api.PayoutAccountDto;
import com.codenameone.examples.wayline.api.PayoutDto;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.domain.DriverHours;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Payout;
import com.codenameone.examples.wayline.domain.PayoutAccount;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.pay.Payments;
import com.codenameone.examples.wayline.repository.DriverHoursRepository;
import com.codenameone.examples.wayline.repository.DriverStateRepository;
import com.codenameone.examples.wayline.repository.PayoutAccountRepository;
import com.codenameone.examples.wayline.repository.PayoutRepository;
import com.codenameone.examples.wayline.repository.ProfileRepository;
import com.codenameone.examples.wayline.repository.RideEventRepository;
import com.codenameone.examples.wayline.repository.RidePassedRepository;
import com.codenameone.examples.wayline.repository.RideRepository;
import com.codenameone.examples.wayline.ride.Fares;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// What a driver has earned, and what has been paid out to them.
///
/// A completed ride earns its driver the fare less the service's commission,
/// fixed when the ride was asked for, plus the whole of any tip. The balance
/// is everything earned less everything paid out, and cashing out moves all
/// of it.
///
/// Payouts are a ledger and no more: a row says the money was sent, and no
/// bank is asked to send it. For the same reason a cash ride is counted like
/// any other, although its driver was handed the money in the car. A service
/// that moves real money settles cash rides the other way, and that sum
/// belongs with the transfer this sample does not make.
@Component
public class Earnings {
    private static final int DAYS = 14;

    private final PayoutRepository payouts;
    private final PayoutAccountRepository accounts;
    private final RideRepository rides;
    private final RideEventRepository events;
    private final RidePassedRepository passes;
    private final DriverStateRepository drivers;
    private final DriverHoursRepository hours;
    private final ProfileRepository profiles;
    private final Fares fares;

    public Earnings(PayoutRepository payouts, PayoutAccountRepository accounts,
            RideRepository rides, RideEventRepository events, RidePassedRepository passes,
            DriverStateRepository drivers, DriverHoursRepository hours,
            ProfileRepository profiles, Fares fares) {
        this.payouts = payouts;
        this.accounts = accounts;
        this.rides = rides;
        this.events = events;
        this.passes = passes;
        this.drivers = drivers;
        this.hours = hours;
        this.profiles = profiles;
        this.fares = fares;
    }

    @Transactional(readOnly = true)
    public EarningsSummaryDto summary(String driver) throws IOException {
        long now = System.currentTimeMillis();
        long today = Days.start(now);
        long month = 0L;
        long week = 0L;
        long earnedToday = 0L;
        // Each day's money and trips, oldest first.
        long[] dayAmount = new long[DAYS];
        int[] dayTrips = new int[DAYS];
        // Thirty days of rides, added up here: "group by day" is spelt
        // differently by every database, and a driver's month is a few hundred
        // rows.
        long monthStart = today - 29 * Days.MILLIS;
        long weekStart = today - 6 * Days.MILLIS;
        List<Ride> done = rides.findByDriverAndStateAndUpdatedAtGreaterThanEqual(driver,
                RideState.COMPLETED, monthStart);
        // A card tip is on its ride from the moment it is asked for, and is
        // still there when the processor never answered. Only one whose charge
        // is on record is earned, here as in the balance: otherwise a day's
        // total would show money that cannot be cashed out.
        Set<String> charged = new HashSet<String>(
                rides.tipChargedSince(driver, monthStart));
        for (int iter = 0; iter < done.size(); iter++) {
            Ride ride = done.get(iter);
            long at = ride.getUpdatedAt();
            boolean tipped = "cash".equals(ride.getMethodId()) || charged.contains(ride.getId());
            // The fare likewise counts only once it is paid, as in the balance:
            // a card charge that failed, was refunded or has not come back is
            // no money of the driver's. The trip still counts as driven.
            boolean paid = Payments.PAID.equals(ride.getPaymentStatus());
            long earned = (paid ? ride.getDriverCents() : 0L) + (tipped ? ride.getTipCents() : 0L);
            month += earned;
            if (at >= weekStart) {
                week += earned;
            }
            if (at >= today) {
                earnedToday += earned;
            }
            int index = DAYS - 1 - (int) (Days.number(today) - Days.number(at));
            if (index >= 0 && index < DAYS) {
                dayAmount[index] += earned;
                dayTrips[index]++;
            }
        }
        List<DayAmountDto> daily = new ArrayList<DayAmountDto>();
        for (int iter = 0; iter < DAYS; iter++) {
            daily.add(new DayAmountDto(Days.iso(today - (DAYS - 1 - iter) * Days.MILLIS),
                    dayAmount[iter], dayTrips[iter]));
        }
        int trips = (int) rides.countByDriverAndState(driver, RideState.COMPLETED);
        DriverHours day = hours.findById(Identifier.of(driver, Long.valueOf(Days.number(now))));
        int onlineMinutes = day == null ? 0 : (int) (day.getMillis() / 60000L);
        DriverState car = drivers.findById(driver);
        double rating = car == null ? 5d : car.rating();
        double acceptanceRate = Drivers.acceptanceRate(events.countByActorAndState(driver,
                RideState.ACCEPTED), passes.countByDriver(driver));
        return new EarningsSummaryDto(fares.currency(), earnedToday, week, month,
                balance(driver), trips, tips(driver), onlineMinutes, rating, acceptanceRate,
                daily);
    }

    /// The payouts made to a driver, newest first.
    @Transactional(readOnly = true)
    public List<PayoutDto> payouts(String driver) throws IOException {
        List<Payout> rows = payouts.findTop100ByDriverOrderByCreatedAtDescIdAsc(driver);
        List<PayoutDto> out = new ArrayList<PayoutDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter)));
        }
        return out;
    }

    /// Pays out the whole balance to the driver's account. Answers 409 when
    /// there is no account to pay it to, or nothing to pay.
    ///
    /// Two requests at once would both read the same balance and both pay
    /// it. So the first thing the transaction does is hold the driver's
    /// payout account, the one row every cash-out of theirs needs: the second
    /// request waits there until the first has committed, then reads a
    /// balance that already has the first payout taken off it, and is told
    /// there is nothing left. The hold has to come before any read, or the
    /// second request would have looked at the ledger before it waited.
    @Transactional
    public PayoutDto cashOut(String driver) throws IOException {
        if (accounts.hold(driver) == 0) {
            throw new ResponseStatusException(409, "Add the account to be paid into first");
        }
        PayoutAccount account = accounts.findById(driver);
        long amount = balance(driver);
        if (amount <= 0L) {
            throw new ResponseStatusException(409, "There is nothing to cash out");
        }
        Payout row = new Payout();
        row.setId(Ids.next());
        row.setDriver(driver);
        row.setAmountCents(amount);
        row.setStatus("paid");
        row.setCreatedAt(System.currentTimeMillis());
        row.setDestination(account.getBankName() + " ****" + account.getLast4());
        payouts.insert(row);
        return toDto(row);
    }

    /// Where a driver is paid; every field empty until they have said.
    @Transactional(readOnly = true)
    public PayoutAccountDto account(String driver) throws IOException {
        return toDto(accounts.findById(driver));
    }

    /// Saves where a driver is paid. Only the last four digits of the account
    /// are taken, which is all a ledger with no bank behind it has a use for.
    @Transactional
    public PayoutAccountDto saveAccount(String driver, PayoutAccountDto wanted)
            throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String last4 = wanted.accountLast4() == null ? "" : wanted.accountLast4().trim();
        boolean digits = last4.length() == 4;
        for (int iter = 0; digits && iter < 4; iter++) {
            digits = last4.charAt(iter) >= '0' && last4.charAt(iter) <= '9';
        }
        if (!digits) {
            throw new ResponseStatusException(400, "Give the last four digits of the account");
        }
        String holder = Text.required(wanted.holder(), 120, "the account holder's name");
        String bankName = Text.required(wanted.bankName(), 80, "the bank's name");
        // Before the row is looked for. Two first saves at once would both
        // find none and both insert one under the same name, and one would
        // fail; behind the profile's hold the second finds the first's row
        // and updates it. The payout account itself cannot be held: until
        // this inserts it there is no row.
        profiles.hold(driver);
        PayoutAccount row = accounts.findById(driver);
        boolean added = row == null;
        if (added) {
            row = new PayoutAccount();
            row.setUsername(driver);
        }
        row.setHolder(holder);
        row.setBankName(bankName);
        row.setLast4(last4);
        if (added) {
            accounts.insert(row);
        }
        return toDto(row);
    }

    /// Everything earned less everything paid out.
    ///
    /// Earned is the driver's share of the fares of every ride they completed
    /// and that was paid for, plus their tips. The dashboard's totals count
    /// the same rides.
    private long balance(String driver) {
        return Rows.number(rides.driverShare(driver)) + tips(driver)
                - Rows.number(payouts.paidOut(driver));
    }

    /// The tips a driver has been given: those handed over in cash, and those
    /// put on a card whose charge has come back.
    ///
    /// A card tip sits on its ride from the moment it is asked for, and stays
    /// there with no charge recorded when the processor did not answer. It is
    /// not money yet -- the card may never have been charged -- so it is not
    /// counted, and so cannot be cashed out, until the charge is on record.
    private long tips(String driver) {
        return Rows.number(rides.cashTips(driver)) + Rows.number(rides.chargedTips(driver));
    }

    private static PayoutAccountDto toDto(PayoutAccount row) {
        if (row == null) {
            return new PayoutAccountDto("", "", "");
        }
        return new PayoutAccountDto(row.getHolder(), row.getBankName(), row.getLast4());
    }

    private static PayoutDto toDto(Payout row) {
        return new PayoutDto(row.getId(), row.getAmountCents(), row.getStatus(),
                row.getCreatedAt(), row.getDestination());
    }
}
