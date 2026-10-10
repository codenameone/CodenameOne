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
import com.codenameone.examples.wayline.Days;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.api.DayAmountDto;
import com.codenameone.examples.wayline.api.EarningsSummaryDto;
import com.codenameone.examples.wayline.api.PayoutAccountDto;
import com.codenameone.examples.wayline.api.PayoutDto;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Payout;
import com.codenameone.examples.wayline.domain.PayoutAccount;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.ride.Fares;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private final PayoutRepository repository;
    private final Fares fares;

    public Earnings(PayoutRepository repository, Fares fares) {
        this.repository = repository;
        this.fares = fares;
    }

    @Transactional(readOnly = true)
    public EarningsSummaryDto summary(String driver) throws IOException {
        long now = System.currentTimeMillis();
        long today = Days.start(now);
        EarningsSummaryDto dto = new EarningsSummaryDto();
        dto.currency = fares.currency();
        dto.daily = new ArrayList<DayAmountDto>();
        for (int iter = DAYS - 1; iter >= 0; iter--) {
            DayAmountDto day = new DayAmountDto();
            day.date = Days.iso(today - iter * Days.MILLIS);
            dto.daily.add(day);
        }
        // Thirty days of rides, added up here: "group by day" is spelt
        // differently by every database, and a driver's month is a few hundred
        // rows.
        long monthStart = today - 29 * Days.MILLIS;
        long weekStart = today - 6 * Days.MILLIS;
        List<Ride> rides = repository.completedSince(driver, monthStart);
        for (int iter = 0; iter < rides.size(); iter++) {
            Ride ride = rides.get(iter);
            long at = ride.updatedAt;
            long earned = ride.driverCents + ride.tipCents;
            dto.month += earned;
            if (at >= weekStart) {
                dto.week += earned;
            }
            if (at >= today) {
                dto.today += earned;
            }
            int index = DAYS - 1 - (int) (Days.number(today) - Days.number(at));
            if (index >= 0 && index < DAYS) {
                DayAmountDto day = dto.daily.get(index);
                day.amount += earned;
                day.trips++;
            }
        }
        dto.trips = (int) repository.trips(driver);
        dto.tips = repository.tips(driver);
        dto.balance = balance(driver);
        dto.onlineMinutes = (int) (repository.onlineMillis(driver, Days.number(now)) / 60000L);
        DriverState car = repository.state(driver);
        dto.rating = car == null ? 5d : car.rating();
        dto.acceptanceRate = Drivers.acceptanceRate(repository.accepted(driver),
                repository.passed(driver));
        return dto;
    }

    /// The payouts made to a driver, newest first.
    @Transactional(readOnly = true)
    public List<PayoutDto> payouts(String driver) throws IOException {
        List<Payout> rows = repository.payouts(driver);
        List<PayoutDto> out = new ArrayList<PayoutDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter)));
        }
        return out;
    }

    /// Pays out the whole balance to the driver's account. Answers 409 when
    /// there is no account to pay it to, or nothing to pay.
    ///
    /// Not one transaction, on purpose. Two requests at once both read the
    /// same balance, and neither would see the other's payout before it was
    /// committed. So the payout is committed first, and the ledger, which is
    /// what is true, is then read again: the request that finds it overdrawn
    /// takes its own row back.
    public PayoutDto cashOut(String driver) throws IOException {
        PayoutDto paid = pay(driver);
        if (takeBackIfOverdrawn(driver, paid.id)) {
            throw new ResponseStatusException(409, "There is nothing to cash out");
        }
        return paid;
    }

    @Transactional
    private PayoutDto pay(String driver) throws IOException {
        PayoutAccount account = repository.account(driver);
        if (account == null) {
            throw new ResponseStatusException(409, "Add the account to be paid into first");
        }
        long amount = balance(driver);
        if (amount <= 0L) {
            throw new ResponseStatusException(409, "There is nothing to cash out");
        }
        Payout row = new Payout();
        row.id = Ids.next();
        row.driver = driver;
        row.amountCents = amount;
        row.status = "paid";
        row.createdAt = System.currentTimeMillis();
        row.destination = account.bankName + " ****" + account.last4;
        repository.add(row);
        return toDto(row);
    }

    @Transactional
    private boolean takeBackIfOverdrawn(String driver, String payoutId) {
        if (balance(driver) >= 0L) {
            return false;
        }
        Payout row = repository.payout(payoutId);
        if (row != null) {
            repository.remove(row);
        }
        return true;
    }

    /// Where a driver is paid; every field empty until they have said.
    @Transactional(readOnly = true)
    public PayoutAccountDto account(String driver) throws IOException {
        return toDto(repository.account(driver));
    }

    /// Saves where a driver is paid. Only the last four digits of the account
    /// are taken, which is all a ledger with no bank behind it has a use for.
    @Transactional
    public PayoutAccountDto saveAccount(String driver, PayoutAccountDto wanted)
            throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String last4 = wanted.accountLast4 == null ? "" : wanted.accountLast4.trim();
        boolean digits = last4.length() == 4;
        for (int iter = 0; digits && iter < 4; iter++) {
            digits = last4.charAt(iter) >= '0' && last4.charAt(iter) <= '9';
        }
        if (!digits) {
            throw new ResponseStatusException(400, "Give the last four digits of the account");
        }
        String holder = Text.required(wanted.holder, 120, "the account holder's name");
        String bankName = Text.required(wanted.bankName, 80, "the bank's name");
        PayoutAccount row = repository.account(driver);
        boolean added = row == null;
        if (added) {
            row = new PayoutAccount();
            row.username = driver;
        }
        row.holder = holder;
        row.bankName = bankName;
        row.last4 = last4;
        if (added) {
            repository.add(row);
        }
        return toDto(row);
    }

    /// Everything earned less everything paid out.
    private long balance(String driver) {
        return repository.fares(driver) + repository.tips(driver) - repository.paidOut(driver);
    }

    private static PayoutAccountDto toDto(PayoutAccount row) {
        PayoutAccountDto dto = new PayoutAccountDto();
        dto.holder = row == null ? "" : row.holder;
        dto.bankName = row == null ? "" : row.bankName;
        dto.accountLast4 = row == null ? "" : row.last4;
        return dto;
    }

    private static PayoutDto toDto(Payout row) {
        PayoutDto dto = new PayoutDto();
        dto.id = row.id;
        dto.amount = row.amountCents;
        dto.status = row.status;
        dto.createdAt = row.createdAt;
        dto.destination = row.destination;
        return dto;
    }
}
