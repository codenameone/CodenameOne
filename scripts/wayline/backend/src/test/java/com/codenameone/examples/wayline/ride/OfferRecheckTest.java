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
package com.codenameone.examples.wayline.ride;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.account.DemoAccounts;
import com.codenameone.examples.wayline.api.DriverStatusDto;
import com.codenameone.examples.wayline.api.RegisterDto;
import com.codenameone.examples.wayline.api.RideDto;
import com.codenameone.examples.wayline.api.RideRequestDto;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.driver.Drivers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// An offer made to a driver who stopped being fit for the ride after the
/// search chose them.
///
/// Choosing and offering are two transactions, and a driver can go off line,
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
    private Rides rides;
    @Autowired
    private Drivers drivers;
    @Autowired
    private DataSource db;

    private String open(String name, boolean driver) throws Exception {
        String username = accounts.register(new RegisterDto(name + "@test.example",
                "a-long-password", name, "+15550111", driver, "Green Fiat 500", "T " + name,
                false, null)).username();
        accounts.markPhoneVerified(username);
        if (driver) {
            accounts.setRoles(DemoAccounts.ADMIN, username, true, false);
        }
        return username;
    }

    private void at(String driver, boolean online, double lat, double lng) throws Exception {
        drivers.report(driver, new DriverStatusDto(online, lat, lng, 90d));
    }

    private void notOffered(String id, String driver, String why) throws Exception {
        assertNull(rides.offer(id, driver), why);
        assertEquals(RideState.REQUESTED, rides.get("", true, id).state(), why);
    }

    @Test
    void aDriverNoLongerFitForTheRideIsNotOfferedIt() throws Exception {
        String rider = open("opo-rider", false);
        String driver = open("opo-driver", true);
        // Asked for with nobody on line, so it waits. The driver is kept unfit
        // from here until the end, so the scheduled search, which runs
        // alongside, has nobody to offer it to either.
        String id = rides.request(rider, new RideRequestDto(LAT, LNG, "Here", LAT + 0.02,
                LNG + 0.02, "There", 0d, 0d, null, null, null, false, false, false, null)).id();
        assertEquals(RideState.REQUESTED, rides.get("", true, id).state());

        // On line, a hundred kilometres away.
        at(driver, true, LAT + 1d, LNG);
        notOffered(id, driver, "a driver out of reach");

        // In reach, and blocked.
        accounts.suspend(DemoAccounts.ADMIN, driver, true, "a test");
        at(driver, true, LAT, LNG);
        notOffered(id, driver, "a blocked driver");

        // No longer approved.
        db.execute("UPDATE driver_application SET status = 'rejected' WHERE username = ?",
                new Object[] {driver});
        accounts.suspend(DemoAccounts.ADMIN, driver, false, "a test");
        notOffered(id, driver, "a driver whose approval was withdrawn");

        // Not heard from in a long while, whatever their status says.
        db.execute("UPDATE driver_state SET last_seen = 1 WHERE username = ?",
                new Object[] {driver});
        db.execute("UPDATE driver_application SET status = 'approved' WHERE username = ?",
                new Object[] {driver});
        notOffered(id, driver, "a driver not heard from");

        // Off line.
        at(driver, false, 0d, 0d);
        notOffered(id, driver, "a driver off line");

        // And once fit again, the ride is theirs: offered here, or by the
        // scheduled search if it got there first.
        at(driver, true, LAT, LNG);
        rides.offer(id, driver);
        RideDto now = rides.get("", true, id);
        assertEquals(RideState.OFFERED, now.state());
        assertEquals(driver, now.driverUsername());
        rides.cancelByAdmin(DemoAccounts.ADMIN, id, "the test is over");
        at(driver, false, 0d, 0d);
    }
}
