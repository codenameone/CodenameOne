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
package com.codenameone.examples.savor.courier;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.session.Identifier;
import com.codenameone.examples.savor.Days;
import com.codenameone.examples.savor.Ids;
import com.codenameone.examples.savor.Rows;
import com.codenameone.examples.savor.Text;
import com.codenameone.examples.savor.api.DayAmountDto;
import com.codenameone.examples.savor.api.EarningsSummaryDto;
import com.codenameone.examples.savor.api.PayoutAccountDto;
import com.codenameone.examples.savor.api.PayoutDto;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.domain.CourierHours;
import com.codenameone.examples.savor.domain.CourierState;
import com.codenameone.examples.savor.domain.Payout;
import com.codenameone.examples.savor.domain.PayoutAccount;
import com.codenameone.examples.savor.domain.Delivery;
import com.codenameone.examples.savor.pay.Payments;
import com.codenameone.examples.savor.repository.CourierHoursRepository;
import com.codenameone.examples.savor.repository.CourierStateRepository;
import com.codenameone.examples.savor.repository.PayoutAccountRepository;
import com.codenameone.examples.savor.repository.PayoutRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;
import com.codenameone.examples.savor.repository.DeliveryEventRepository;
import com.codenameone.examples.savor.repository.DeliveryPassedRepository;
import com.codenameone.examples.savor.repository.DeliveryRepository;
import com.codenameone.examples.savor.order.Quotes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// What a courier has earned, and what has been paid out to them.
///
/// A completed delivery earns its courier the fare less the service's commission,
/// fixed when the delivery was asked for, plus the whole of any tip. The balance
/// is everything earned less everything paid out, and cashing out moves all
/// of it.
///
/// Payouts are a ledger and no more: a row says the money was sent, and no
/// bank is asked to send it. For the same reason a cash delivery is counted like
/// any other, although its courier was handed the money in the car. A service
/// that moves real money settles cash deliveries the other way, and that sum
/// belongs with the transfer this sample does not make.
@Component
public class Earnings {
    private static final int DAYS = 14;

    private final PayoutRepository payouts;
    private final PayoutAccountRepository accounts;
    private final DeliveryRepository deliveries;
    private final DeliveryEventRepository events;
    private final DeliveryPassedRepository passes;
    private final CourierStateRepository couriers;
    private final CourierHoursRepository hours;
    private final ProfileRepository profiles;
    private final Quotes fares;

    public Earnings(PayoutRepository payouts, PayoutAccountRepository accounts,
            DeliveryRepository deliveries, DeliveryEventRepository events, DeliveryPassedRepository passes,
            CourierStateRepository couriers, CourierHoursRepository hours,
            ProfileRepository profiles, Quotes fares) {
        this.payouts = payouts;
        this.accounts = accounts;
        this.deliveries = deliveries;
        this.events = events;
        this.passes = passes;
        this.couriers = couriers;
        this.hours = hours;
        this.profiles = profiles;
        this.fares = fares;
    }

    @Transactional(readOnly = true)
    public EarningsSummaryDto summary(String courier) throws IOException {
        long now = System.currentTimeMillis();
        long today = Days.start(now);
        long month = 0L;
        long week = 0L;
        long earnedToday = 0L;
        // Each day's money and trips, oldest first.
        long[] dayAmount = new long[DAYS];
        int[] dayTrips = new int[DAYS];
        // Thirty days of deliveries, added up here: "group by day" is spelt
        // differently by every database, and a courier's month is a few hundred
        // rows.
        long monthStart = today - 29 * Days.MILLIS;
        long weekStart = today - 6 * Days.MILLIS;
        List<Delivery> done = deliveries.findByCourierAndStateAndUpdatedAtGreaterThanEqual(courier,
                DeliveryState.COMPLETED, monthStart);
        // A card tip is on its delivery from the moment it is asked for, and is
        // still there when the processor never answered. Only one whose charge
        // is on record is earned, here as in the balance: otherwise a day's
        // total would show money that cannot be cashed out.
        Set<String> charged = new HashSet<String>(
                deliveries.tipChargedSince(courier, monthStart));
        for (int iter = 0; iter < done.size(); iter++) {
            Delivery delivery = done.get(iter);
            long at = delivery.getUpdatedAt();
            boolean tipped = "cash".equals(delivery.getMethodId()) || charged.contains(delivery.getId());
            // The fare likewise counts only once it is paid, as in the balance:
            // a card charge that failed, was refunded or has not come back is
            // no money of the courier's. The trip still counts as driven.
            boolean paid = Payments.PAID.equals(delivery.getPaymentStatus());
            long earned = (paid ? delivery.getCourierCents() : 0L) + (tipped ? delivery.getTipCents() : 0L);
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
        int trips = (int) deliveries.countByCourierAndState(courier, DeliveryState.COMPLETED);
        CourierHours day = hours.findById(Identifier.of(courier, Long.valueOf(Days.number(now))));
        int onlineMinutes = day == null ? 0 : (int) (day.getMillis() / 60000L);
        CourierState car = couriers.findById(courier);
        double rating = car == null ? 5d : car.rating();
        double acceptanceRate = Couriers.acceptanceRate(events.countByActorAndState(courier,
                DeliveryState.ACCEPTED), passes.countByCourier(courier));
        return new EarningsSummaryDto(fares.currency(), earnedToday, week, month,
                balance(courier), trips, tips(courier), onlineMinutes, rating, acceptanceRate,
                daily);
    }

    /// The payouts made to a courier, newest first.
    @Transactional(readOnly = true)
    public List<PayoutDto> payouts(String courier) throws IOException {
        List<Payout> rows = payouts.findTop100ByCourierOrderByCreatedAtDescIdAsc(courier);
        List<PayoutDto> out = new ArrayList<PayoutDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter)));
        }
        return out;
    }

    /// Pays out the whole balance to the courier's account. Answers 409 when
    /// there is no account to pay it to, or nothing to pay.
    ///
    /// Two requests at once would both read the same balance and both pay
    /// it. So the first thing the transaction does is hold the courier's
    /// payout account, the one row every cash-out of theirs needs: the second
    /// request waits there until the first has committed, then reads a
    /// balance that already has the first payout taken off it, and is told
    /// there is nothing left. The hold has to come before any read, or the
    /// second request would have looked at the ledger before it waited.
    @Transactional
    public PayoutDto cashOut(String courier) throws IOException {
        if (accounts.hold(courier) == 0) {
            throw new ResponseStatusException(409, "Add the account to be paid into first");
        }
        PayoutAccount account = accounts.findById(courier);
        long amount = balance(courier);
        if (amount <= 0L) {
            throw new ResponseStatusException(409, "There is nothing to cash out");
        }
        Payout row = new Payout();
        row.setId(Ids.next());
        row.setCourier(courier);
        row.setAmountCents(amount);
        row.setStatus("paid");
        row.setCreatedAt(System.currentTimeMillis());
        row.setDestination(account.getBankName() + " ****" + account.getLast4());
        payouts.insert(row);
        return toDto(row);
    }

    /// Where a courier is paid; every field empty until they have said.
    @Transactional(readOnly = true)
    public PayoutAccountDto account(String courier) throws IOException {
        return toDto(accounts.findById(courier));
    }

    /// Saves where a courier is paid. Only the last four digits of the account
    /// are taken, which is all a ledger with no bank behind it has a use for.
    @Transactional
    public PayoutAccountDto saveAccount(String courier, PayoutAccountDto wanted)
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
        // this inserts it there is no row. With no profile to hold the account
        // was closed meanwhile and its clean-up has run: a row inserted now
        // would keep the closed account's bank details with nobody able to
        // remove them.
        if (profiles.hold(courier) == 0) {
            throw new ResponseStatusException(404, "No such account");
        }
        PayoutAccount row = accounts.findById(courier);
        boolean added = row == null;
        if (added) {
            row = new PayoutAccount();
            row.setUsername(courier);
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
    /// The dashboard's totals count the same deliveries as [#earned]. Closing an
    /// account asks it too, and is refused while it is above zero.
    public long balance(String courier) {
        return earned(courier) - Rows.number(payouts.paidOut(courier));
    }

    /// Everything a courier has earned, before anything was paid out: their
    /// share of the fares of every delivery they completed and that was paid for,
    /// plus their tips as [#tips] counts them.
    ///
    /// The administrator's view of an account shows this figure, and takes it
    /// from here rather than from a sum of its own, so the two can never
    /// disagree about a charge that failed, a refund or a tip whose charge has
    /// not come back.
    ///
    /// @param courier the courier's user name
    /// @return the cents earned
    public long earned(String courier) {
        return Rows.number(deliveries.courierShare(courier)) + tips(courier);
    }

    /// The tips a courier has been given: those handed over in cash, and those
    /// put on a card whose charge has come back.
    ///
    /// A card tip sits on its delivery from the moment it is asked for, and stays
    /// there with no charge recorded when the processor did not answer. It is
    /// not money yet -- the card may never have been charged -- so it is not
    /// counted, and so cannot be cashed out, until the charge is on record.
    private long tips(String courier) {
        return Rows.number(deliveries.cashTips(courier)) + Rows.number(deliveries.chargedTips(courier));
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
