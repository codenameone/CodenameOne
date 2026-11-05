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
package com.codenameone.examples.slate;

import com.codename1.backend.Transactions;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.slate.account.Accounts;
import com.codenameone.examples.slate.domain.ProviderService;
import com.codenameone.examples.slate.domain.Service;
import com.codenameone.examples.slate.domain.WorkingHours;
import com.codenameone.examples.slate.repository.ProviderServiceRepository;
import com.codenameone.examples.slate.repository.ServiceRepository;
import com.codenameone.examples.slate.repository.WorkingHoursRepository;

import java.util.concurrent.atomic.AtomicInteger;

import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.jwt;

/// The people and services a test needs, made fresh for it.
///
/// Test classes share a running server and its database for as long as they
/// can, so a test that borrowed a demo account or a demo provider would meet
/// whatever the test before it left behind: a slot already taken, a daily cap
/// already reached. Each test makes its own instead: a provider of its own
/// who alone offers a service of its own, so nothing it books can clash with
/// anything another test booked.
public final class People {
    public static final String PASSWORD = "a-long-password";
    public static final String LOCATION = "loc-main";
    private static final AtomicInteger NEXT = new AtomicInteger();
    private static final long DAY = 86400000L;

    private People() {
    }

    /// Work a test does on the database directly, which has to be inside a
    /// transaction as any repository call has.
    public interface Step<T> {
        T run() throws Exception;
    }

    /// Runs `step` in a transaction of its own and answers what it answered.
    public static <T> T tx(Step<T> step) throws Exception {
        final Object[] out = new Object[1];
        Transactions.execute(() -> {
            out[0] = step.run();
            return null;
        });
        @SuppressWarnings("unchecked")
        T answer = (T) out[0];
        return answer;
    }

    /// A name no other test has used.
    public static String unique(String stem) {
        return stem + System.nanoTime() % 1000000000L + "-" + NEXT.incrementAndGet();
    }

    /// Opens an account, a provider's when `provider` is set, and returns its name.
    public static String account(Accounts accounts, String name, boolean provider)
            throws Exception {
        String username = unique(name) + "@test.example";
        accounts.create(username, PASSWORD, name, "+15550111", true, provider, false,
                System.currentTimeMillis());
        return username;
    }

    /// A service of the test's own: `minutes` long, one provider, no buffers,
    /// no notice and no deposit, so a test states the rule it is about.
    public static Service service(ServiceRepository services, int minutes) throws Exception {
        Service s = new Service();
        s.setId(unique("svc-t"));
        s.setName(unique("Service "));
        s.setCategory("Test");
        s.setMinutes(minutes);
        s.setPriceCents(4000);
        s.setProvidersNeeded(1);
        s.setCapacity(1);
        s.setMinNoticeMinutes(0);
        s.setDepositPercent(0);
        s.setColor("#888888");
        s.setActive(true);
        s.setSortOrder(1000);
        tx(() -> { services.insert(s); return null; });
        return s;
    }

    /// A provider of the test's own who offers `service` at the main
    /// location from 09:00 to 17:00, every day of the week.
    public static String provider(Accounts accounts, ProviderServiceRepository offers,
            WorkingHoursRepository hours, Service service) throws Exception {
        String username = account(accounts, "Provider", true);
        ProviderService offer = new ProviderService();
        offer.setId(Ids.next());
        offer.setUsername(username);
        offer.setServiceId(service.getId());
        tx(() -> { offers.insert(offer); return null; });
        for (int day = 1; day <= 7; day++) {
            WorkingHours w = new WorkingHours();
            w.setId(Ids.next());
            w.setUsername(username);
            w.setLocationId(LOCATION);
            w.setWeekday(day);
            w.setStartMinute(9 * 60);
            w.setEndMinute(17 * 60);
            tx(() -> { hours.insert(w); return null; });
        }
        return username;
    }

    /// The day `days` days ahead, as the API spells it.
    public static String dayAhead(int days) {
        return java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(days).toString();
    }

    /// A time `days` days ahead at 11:00 UTC.
    public static long daysAhead(int days) {
        long now = System.currentTimeMillis();
        return (now / DAY + days) * DAY + 11L * 3600000L;
    }

    /// The request, made as a client. The authorities are stated here and not
    /// read from the account: these are the tests of what a role may do, and
    /// `AccessTest` is the one that signs in for real.
    public static MockRequestBuilder asClient(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_CLIENT"));
    }

    public static MockRequestBuilder asProvider(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_CLIENT", "ROLE_PROVIDER"));
    }

    public static MockRequestBuilder asOwner(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_CLIENT", "ROLE_OWNER"));
    }
}
