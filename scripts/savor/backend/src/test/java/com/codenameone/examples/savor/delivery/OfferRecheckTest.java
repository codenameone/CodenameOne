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
package com.codenameone.examples.savor.delivery;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.account.DemoAccounts;
import com.codenameone.examples.savor.api.CourierStatusDto;
import com.codenameone.examples.savor.api.RegisterDto;
import com.codenameone.examples.savor.api.DeliveryDto;
import com.codenameone.examples.savor.api.DeliveryRequestDto;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.courier.Couriers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// An offer made to a courier who stopped being fit for the delivery after the
/// search chose them.
///
/// Choosing and offering are two transactions, and a courier can go off line,
/// drive away, lose their approval or be blocked in between. Each of those is
/// set up here and the offer made to them anyway, which is the second
/// transaction running after the first read them as they were. It sits in
/// this package because the offer is not part of the service's public face.
@BackendTest
class OfferRecheckTest {
    // Porto.
    private static final double LAT = 41.1579;
    private static final double LNG = -8.6291;

    @Autowired
    private Accounts accounts;
    @Autowired
    private Deliveries deliveries;
    @Autowired
    private Couriers couriers;
    @Autowired
    private DataSource db;

    private String open(String name, boolean courier) throws Exception {
        String username = accounts.register(new RegisterDto(name + "@test.example",
                "a-long-password", name, "+15550111", courier, "Green Fiat 500", "T " + name,
                false, null)).username();
        accounts.markPhoneVerified(username);
        if (courier) {
            accounts.setRoles(DemoAccounts.ADMIN, username, true, false);
        }
        return username;
    }

    private void at(String courier, boolean online, double lat, double lng) throws Exception {
        couriers.report(courier, new CourierStatusDto(online, lat, lng, 90d));
    }

    private void notOffered(String id, String courier, String why) throws Exception {
        assertNull(deliveries.offer(id, courier), why);
        assertEquals(DeliveryState.REQUESTED, deliveries.get("", true, id).state(), why);
    }

    @Test
    void aCourierNoLongerFitForTheDeliveryIsNotOfferedIt() throws Exception {
        String customer = open("opo-customer", false);
        String courier = open("opo-courier", true);
        // Asked for with nobody on line, so it waits. The courier is kept unfit
        // from here until the end, so the scheduled search, which runs
        // alongside, has nobody to offer it to either.
        String id = deliveries.request(customer, new DeliveryRequestDto(LAT, LNG, "Here", LAT + 0.02,
                LNG + 0.02, "There", 0d, 0d, null, null, null, false, false, false, null)).id();
        assertEquals(DeliveryState.REQUESTED, deliveries.get("", true, id).state());

        // On line, a hundred kilometres away.
        at(courier, true, LAT + 1d, LNG);
        notOffered(id, courier, "a courier out of reach");

        // In reach, and blocked.
        accounts.suspend(DemoAccounts.ADMIN, courier, true, "a test");
        at(courier, true, LAT, LNG);
        notOffered(id, courier, "a blocked courier");

        // No longer approved.
        db.execute("UPDATE courier_application SET status = 'rejected' WHERE username = ?",
                new Object[] {courier});
        accounts.suspend(DemoAccounts.ADMIN, courier, false, "a test");
        notOffered(id, courier, "a courier whose approval was withdrawn");

        // Not heard from in a long while, whatever their status says.
        db.execute("UPDATE courier_state SET last_seen = 1 WHERE username = ?",
                new Object[] {courier});
        db.execute("UPDATE courier_application SET status = 'approved' WHERE username = ?",
                new Object[] {courier});
        notOffered(id, courier, "a courier not heard from");

        // Off line.
        at(courier, false, 0d, 0d);
        notOffered(id, courier, "a courier off line");

        // And once fit again, the delivery is theirs: offered here, or by the
        // scheduled search if it got there first.
        at(courier, true, LAT, LNG);
        deliveries.offer(id, courier);
        DeliveryDto now = deliveries.get("", true, id);
        assertEquals(DeliveryState.OFFERED, now.state());
        assertEquals(courier, now.courierUsername());
        deliveries.cancelByAdmin(DemoAccounts.ADMIN, id, "the test is over");
        at(courier, false, 0d, 0d);
    }
}
