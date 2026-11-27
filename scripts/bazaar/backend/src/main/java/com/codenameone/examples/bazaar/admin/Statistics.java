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
package com.codenameone.examples.bazaar.admin;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.account.Moderation;
import com.codenameone.examples.bazaar.api.AdminStatsDto;
import com.codenameone.examples.bazaar.api.DayStatsDto;
import com.codenameone.examples.bazaar.api.NameCountDto;
import com.codenameone.examples.bazaar.api.StatsSeriesDto;
import com.codenameone.examples.bazaar.api.UserDetailDto;
import com.codenameone.examples.bazaar.api.UserDto;
import com.codenameone.examples.bazaar.domain.Payment;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.repository.PaymentRepository;
import com.codenameone.examples.bazaar.repository.ProfileRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// The numbers behind the admin's dashboard and charts, and the account of one
/// user: accounts, moderation and payments.
///
/// The charts by day and by hour of the day are counted here, from one row per
/// payment of the period. Times are stored as milliseconds and a day is a
/// division ([Days]). A sample's ninety days fit in memory. A service whose do
/// not keeps a table of daily totals, and this class is where it would be read.
@Component
public class Statistics {
    static final int DEFAULT_DAYS = 14;
    static final int MAX_DAYS = 90;

    private final ProfileRepository profiles;
    private final PaymentRepository payments;
    private final Accounts accounts;
    private final Moderation moderation;
    private final String currency;

    public Statistics(ProfileRepository profiles, PaymentRepository payments, Accounts accounts,
            Moderation moderation, @Value("${bazaar.currency:USD}") String currency) {
        this.profiles = profiles;
        this.payments = payments;
        this.accounts = accounts;
        this.moderation = moderation;
        this.currency = currency;
    }

    /// The handful of totals at the top of the admin's dashboard.
    @Transactional(readOnly = true)
    public AdminStatsDto dashboard() throws IOException {
        long now = System.currentTimeMillis();
        Instant today = Instant.ofEpochMilli(Days.start(now));
        int users = (int) profiles.count();
        int usersToday = profiles.signUps(today).size();
        int suspended = 0;
        List<UserDto> everyone = accounts.all();
        for (int iter = 0; iter < everyone.size(); iter++) {
            if (everyone.get(iter).suspended()) {
                suspended++;
            }
        }
        int paymentsToday = payments.findByCreatedAtGreaterThanEqual(today).size();
        long revenue = 0L;
        List<Payment> all = payments.findByCreatedAtGreaterThanEqual(Instant.EPOCH);
        for (int iter = 0; iter < all.size(); iter++) {
            revenue += kept(all.get(iter));
        }
        return new AdminStatsDto(users, usersToday, (int) profiles.countByFlaggedTrue(), suspended,
                paymentsToday, revenue, currency);
    }

    /// The last `days` days, today included and last. Fewer than one is taken
    /// as the default and more than [#MAX_DAYS] as that many.
    @Transactional(readOnly = true)
    public StatsSeriesDto series(int days) throws IOException {
        int span = days < 1 ? DEFAULT_DAYS : Math.min(days, MAX_DAYS);
        long today = Days.start(System.currentTimeMillis());
        long since = today - (span - 1) * Days.MILLIS;
        int[] dayPayments = new int[span];
        int[] dayRefunds = new int[span];
        long[] dayRevenue = new long[span];
        int[] dayNewUsers = new int[span];
        int[] byHour = new int[24];
        Map<String, int[]> byStatus = new TreeMap<String, int[]>();
        long sum = 0L;
        int captured = 0;
        List<Payment> rows = payments.findByCreatedAtGreaterThanEqual(Instant.ofEpochMilli(since));
        for (int iter = 0; iter < rows.size(); iter++) {
            Payment row = rows.get(iter);
            long at = row.getCreatedAt().toEpochMilli();
            byHour[(int) (at % Days.MILLIS / 3600000L)]++;
            int[] count = byStatus.get(row.getStatus());
            if (count == null) {
                count = new int[1];
                byStatus.put(row.getStatus(), count);
            }
            count[0]++;
            long kept = kept(row);
            if (captured(row)) {
                captured++;
                sum += row.getAmountCents();
            }
            int index = (int) ((at - since) / Days.MILLIS);
            if (index < 0 || index >= span) {
                continue;
            }
            dayPayments[index]++;
            if (row.getRefundedCents() > 0L) {
                dayRefunds[index]++;
            }
            dayRevenue[index] += kept;
        }
        List<Instant> signUps = profiles.signUps(Instant.ofEpochMilli(since));
        for (int iter = 0; iter < signUps.size(); iter++) {
            int index = (int) ((signUps.get(iter).toEpochMilli() - since) / Days.MILLIS);
            if (index >= 0 && index < span) {
                dayNewUsers[index]++;
            }
        }
        List<DayStatsDto> daily = new ArrayList<DayStatsDto>();
        for (int iter = 0; iter < span; iter++) {
            daily.add(new DayStatsDto(Days.iso(since + iter * Days.MILLIS), dayPayments[iter],
                    dayRefunds[iter], dayRevenue[iter], dayNewUsers[iter]));
        }
        List<Integer> hours = new ArrayList<Integer>();
        for (int iter = 0; iter < byHour.length; iter++) {
            hours.add(Integer.valueOf(byHour[iter]));
        }
        List<NameCountDto> statuses = new ArrayList<NameCountDto>();
        for (Map.Entry<String, int[]> entry : byStatus.entrySet()) {
            statuses.add(new NameCountDto(entry.getKey(), entry.getValue()[0]));
        }
        Collections.sort(statuses, (a, b) -> Integer.compare(b.count(), a.count()));
        // Tested before dividing: dividing a whole number by zero does not
        // raise an exception on every runtime this runs on.
        long average = captured == 0 ? 0L : sum / captured;
        return new StatsSeriesDto(daily, hours, statuses, average,
                (int) profiles.countByFlaggedTrue(), currency);
    }

    /// One account, with what it has paid and what was done to it.
    @Transactional(readOnly = true)
    public UserDetailDto user(String username) throws IOException {
        UserDto user = accounts.describe(username);
        List<Payment> mine = payments.findByUsernameOrderByCreatedAtDesc(username);
        long spent = 0L;
        for (int iter = 0; iter < mine.size(); iter++) {
            spent += kept(mine.get(iter));
        }
        return new UserDetailDto(user, mine.size(), spent, moderation.events(username));
    }

    /// Whether the money of a payment was taken, whatever was refunded since.
    private static boolean captured(Payment row) {
        String status = row.getStatus();
        return Payments.PAID.equals(status) || Payments.PARTIALLY_REFUNDED.equals(status)
                || Payments.REFUNDED.equals(status) || Payments.REFUND_PENDING.equals(status)
                || Payments.REFUND_FAILED.equals(status);
    }

    /// What a payment brought in: what was taken less what was given back.
    private static long kept(Payment row) {
        return captured(row) ? row.getAmountCents() - row.getRefundedCents() : 0L;
    }
}
