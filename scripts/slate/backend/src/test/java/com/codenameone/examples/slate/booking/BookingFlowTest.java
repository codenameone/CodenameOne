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
package com.codenameone.examples.slate.booking;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.slate.People;
import com.codenameone.examples.slate.account.Accounts;
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.CancelDto;
import com.codenameone.examples.slate.api.CardDto;
import com.codenameone.examples.slate.api.ConfirmDto;
import com.codenameone.examples.slate.api.DaySlotsDto;
import com.codenameone.examples.slate.api.HoldRequestDto;
import com.codenameone.examples.slate.api.PaymentSetupDto;
import com.codenameone.examples.slate.api.RescheduleDto;
import com.codenameone.examples.slate.domain.Service;
import com.codenameone.examples.slate.pay.Payments;
import com.codenameone.examples.slate.repository.ProviderServiceRepository;
import com.codenameone.examples.slate.repository.ServiceRepository;
import com.codenameone.examples.slate.repository.WorkingHoursRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// A booking from hold to cancel, against the real database: the slot claims
/// that stop a double booking, the deposit, and the slot coming back.
@BackendTest
class BookingFlowTest {
    @Autowired
    private Bookings bookings;
    @Autowired
    private Availability availability;
    @Autowired
    private Payments payments;
    @Autowired
    private Accounts accounts;
    @Autowired
    private ServiceRepository services;
    @Autowired
    private ProviderServiceRepository offers;
    @Autowired
    private WorkingHoursRepository hours;

    private Service service;
    private String provider;

    private void setUp(int depositPercent) throws Exception {
        service = People.service(services, 60);
        if (depositPercent > 0) {
            service.setDepositPercent(depositPercent);
            People.tx(() -> { services.save(service); return null; });
        }
        provider = People.provider(accounts, offers, hours, service);
    }

    private String client() throws Exception {
        return People.account(accounts, "Client", false);
    }

    private HoldRequestDto ask(long start) {
        return new HoldRequestDto(service.getId(), People.LOCATION, start,
                Collections.singletonList(provider));
    }

    private void card(String client) throws Exception {
        PaymentSetupDto setup = payments.startSetup(client, "Client");
        payments.completeSetup(client, setup.id(),
                new CardDto("4242424242424242", 12, 2040, "123", "Client"));
    }

    private static int status(Exception e) {
        return e instanceof ResponseStatusException ? ((ResponseStatusException) e).getStatus() : -1;
    }

    private boolean offered(String client, long start, int days) {
        DaySlotsDto day = availability.day(client, service.getId(), People.LOCATION,
                People.dayAhead(days), provider);
        for (int iter = 0; iter < day.slots().size(); iter++) {
            if (day.slots().get(iter).start() == start) {
                return true;
            }
        }
        return false;
    }

    @Test
    void aHeldSlotIsNotOfferedOrHeldAgainUntilItIsLetGo() throws Exception {
        setUp(0);
        String first = client();
        String second = client();
        long start = People.daysAhead(3);
        assertTrue(offered(second, start, 3));

        AppointmentDto held = bookings.hold(first, "First", ask(start));
        assertEquals(States.HELD, held.state());
        assertFalse(offered(second, start, 3));
        try {
            bookings.hold(second, "Second", ask(start));
            fail("One slot was held twice");
        } catch (ResponseStatusException e) {
            assertEquals(409, e.getStatus());
        }
        // Half an hour later overlaps it too.
        try {
            bookings.hold(second, "Second", ask(start + 30 * 60000L));
            fail("An overlapping slot was held");
        } catch (ResponseStatusException e) {
            assertEquals(409, e.getStatus());
        }

        AppointmentDto booked = bookings.confirm(first, held.id(), new ConfirmDto("", "", null));
        assertEquals(States.CONFIRMED, booked.state());
        assertEquals(States.PAY_NONE, booked.paymentState());

        AppointmentDto cancelled = bookings.cancelByClient(first, held.id(), new CancelDto(""));
        assertEquals(States.CANCELLED, cancelled.state());
        assertTrue(offered(second, start, 3));
        assertEquals(States.HELD, bookings.hold(second, "Second", ask(start)).state());
    }

    @Test
    void ofManyClientsHoldingOneSlotAtOnceExactlyOneGetsIt() throws Exception {
        setUp(0);
        final long start = People.daysAhead(4);
        final int count = 8;
        final List<String> clients = new ArrayList<String>();
        for (int iter = 0; iter < count; iter++) {
            clients.add(client());
        }
        final AtomicInteger won = new AtomicInteger();
        final AtomicInteger refused = new AtomicInteger();
        final List<Exception> other = Collections.synchronizedList(new ArrayList<Exception>());
        Thread[] threads = new Thread[count];
        for (int iter = 0; iter < count; iter++) {
            final String who = clients.get(iter);
            threads[iter] = new Thread(() -> {
                try {
                    bookings.hold(who, "Racer", ask(start));
                    won.incrementAndGet();
                } catch (Exception e) {
                    if (status(e) == 409) {
                        refused.incrementAndGet();
                    } else {
                        other.add(e);
                    }
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join(30000L);
        }
        assertTrue(other.isEmpty(), () -> "Unexpected: " + other);
        assertEquals(1, won.get());
        assertEquals(count - 1, refused.get());
    }

    @Test
    void aDepositIsHeldWhenTheBookingIsNearAndReleasedOnAFreeCancel() throws Exception {
        setUp(50);
        String client = client();
        card(client);
        AppointmentDto held = bookings.hold(client, "Client", ask(People.daysAhead(3)));
        assertEquals(2000, held.depositCents());

        AppointmentDto booked = bookings.confirm(client, held.id(), new ConfirmDto("", "", null));
        assertEquals(States.PAY_AUTHORIZED, booked.paymentState());
        // Three days out is well before the cancellation window closes.
        assertTrue(booked.freeCancelUntil() > System.currentTimeMillis());
        AppointmentDto cancelled = bookings.cancelByClient(client, held.id(), new CancelDto(""));
        assertEquals(States.PAY_RELEASED, cancelled.paymentState());
    }

    @Test
    void aDepositFarAheadWaitsAndCashHoldsNothing() throws Exception {
        setUp(50);
        String client = client();
        card(client);
        AppointmentDto later = bookings.hold(client, "Client", ask(People.daysAhead(10)));
        assertEquals(States.PAY_PENDING,
                bookings.confirm(client, later.id(), new ConfirmDto("", "", null)).paymentState());

        String other = client();
        AppointmentDto cash = bookings.hold(other, "Other", ask(People.daysAhead(5)));
        AppointmentDto paid = bookings.confirm(other, cash.id(),
                new ConfirmDto(Payments.CASH, "", null));
        assertEquals(States.PAY_NONE, paid.paymentState());
        assertEquals(0, paid.depositCents());
    }

    @Test
    void aRescheduleFreesTheOldTimeAndTakesTheNew() throws Exception {
        setUp(0);
        String client = client();
        String other = client();
        long from = People.daysAhead(6);
        long to = from + 2 * 3600000L;
        AppointmentDto held = bookings.hold(client, "Client", ask(from));
        bookings.confirm(client, held.id(), new ConfirmDto("", "", null));

        AppointmentDto moved = bookings.reschedule(client, held.id(),
                new RescheduleDto(to, Collections.singletonList(provider)));
        assertEquals(to, moved.start());
        assertEquals(States.CONFIRMED, moved.state());
        assertTrue(offered(other, from, 6));
        assertFalse(offered(other, to, 6));
    }

    @Test
    void aClientCannotTouchSomeoneElsesBooking() throws Exception {
        setUp(0);
        String owner = client();
        AppointmentDto held = bookings.hold(owner, "Owner", ask(People.daysAhead(7)));
        try {
            bookings.cancelByClient(client(), held.id(), new CancelDto(""));
            fail("Another client cancelled the booking");
        } catch (ResponseStatusException e) {
            assertEquals(404, e.getStatus());
        }
    }
}
