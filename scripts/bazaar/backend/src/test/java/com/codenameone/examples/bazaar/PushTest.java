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
package com.codenameone.examples.bazaar;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.account.DemoAccounts;
import com.codenameone.examples.bazaar.api.PushDeviceDto;
import com.codenameone.examples.bazaar.push.PushDevices;
import com.codename1.appkit.server.push.PushSender;
import com.codename1.appkit.server.push.RecordingPushSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The devices push notifications go to: registered in the provider's own
/// spelling, one account per phone, capped per account, and forgotten with the
/// account. What is sent to them is the job of whatever owns the event.
///
/// The `test` profile keeps every notification instead of sending it.
@BackendTest
class PushTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private PushSender sender;
    @Autowired
    private DataSource db;
    @Autowired
    private PushDevices pushDevices;

    private RecordingPushSender sent;

    @BeforeEach
    void recording() {
        assertTrue(sender instanceof RecordingPushSender,
                "the test profile records notifications: " + sender);
        sent = (RecordingPushSender) sender;
    }

    private static MockRequestBuilder body(MockRequestBuilder request, String json) {
        return request.contentType("application/json").content(json);
    }

    private static String device(String provider, String token) {
        return "{\"provider\":\"" + provider + "\",\"token\":\"" + token
                + "\",\"platform\":\"and\"}";
    }

    private void register(String username, String provider, String token) throws Exception {
        mvc.perform(People.asCustomer(body(post("/api/account/push"), device(provider, token)),
                username)).andExpect(status().isOk());
    }

    private long tokens(String token) throws Exception {
        Map row = db.queryOne("SELECT COUNT(*) AS n FROM push_device WHERE token = ?",
                new Object[] {token});
        return ((Number) row.get("n")).longValue();
    }

    private long devices(String username) throws Exception {
        Map row = db.queryOne("SELECT COUNT(*) AS n FROM push_device WHERE username = ?",
                new Object[] {username});
        return ((Number) row.get("n")).longValue();
    }

    @Test
    void aDeviceIsRegisteredInTheProvidersOwnSpelling() throws Exception {
        String customer = People.customer(accounts, "push-reg-customer");
        mvc.perform(People.asCustomer(body(post("/api/account/push"),
                device("gcm", "reg-token-1")), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("fcm"))
                .andExpect(jsonPath("$.token").value("reg-token-1"));
        // An alias is the same device, not a second one.
        mvc.perform(People.asCustomer(body(post("/api/account/push"),
                device("fcm", "reg-token-1")), customer)).andExpect(status().isOk());
        mvc.perform(People.asCustomer(body(post("/api/account/push"),
                device("pigeon", "reg-token-2")), customer)).andExpect(status().isBadRequest());
        mvc.perform(People.asCustomer(body(post("/api/account/push"),
                device("apns", " ")), customer)).andExpect(status().isBadRequest());
    }

    @Test
    void aPhoneBelongsToWhoeverSignedInOnItLast() throws Exception {
        String first = People.customer(accounts, "push-ams-first");
        String second = People.customer(accounts, "push-ams-second");
        register(first, "fcm", "ams-shared-phone");
        register(second, "fcm", "ams-shared-phone");
        register(first, "fcm", "ams-first-tablet");
        assertEquals(1L, devices(first));
        assertEquals(1L, devices(second));

        // Signing out forgets the device, and only one's own.
        mvc.perform(People.asCustomer(body(post("/api/account/push/remove"),
                device("fcm", "ams-shared-phone")), first)).andExpect(status().isOk());
        assertEquals(1L, devices(second));
        mvc.perform(People.asCustomer(body(post("/api/account/push/remove"),
                device("fcm", "ams-first-tablet")), first)).andExpect(status().isOk());
        assertEquals(0L, devices(first));

        // Closing the account forgets every device it had.
        String customer = People.customer(accounts, "push-ams-closing");
        register(customer, "apns", "ams-closing-phone");
        accounts.close(customer, People.PASSWORD);
        assertEquals(0L, devices(customer));
        assertEquals(1L, devices(second));
    }

    @Test
    void aTokenTakenFromAnotherAccountStillRespectsTheCap() throws Exception {
        String full = People.customer(accounts, "push-cap-full");
        String other = People.customer(accounts, "push-cap-other");
        for (int i = 0; i < 8; i++) {
            register(full, "fcm", "cap-full-" + i);
        }
        assertEquals(8L, devices(full));
        // Make one device clearly the one heard from longest ago.
        db.execute("UPDATE push_device SET updated_at = 1 WHERE token = ?",
                new Object[] {"cap-full-3"});
        register(other, "fcm", "cap-moving-phone");
        register(full, "fcm", "cap-moving-phone");
        assertEquals(8L, devices(full));
        assertEquals(0L, devices(other));
        assertEquals(0L, tokens("cap-full-3"));
        assertEquals(1L, tokens("cap-moving-phone"));
    }

    /// A registration that reaches the database after the account was closed
    /// is refused, and leaves no device behind: the profile it holds first is
    /// gone. This is the order a registration authorized just before the
    /// closure and run just after it would see.
    @Test
    void aClosedAccountRegistersNoDevice() throws Exception {
        String customer = People.customer(accounts, "push-gone-customer");
        accounts.close(customer, People.PASSWORD);
        try {
            pushDevices.register(customer, new PushDeviceDto("fcm", "gone-customer-phone", "and"));
            fail("a device was registered for a closed account");
        } catch (ResponseStatusException refused) {
            assertEquals(404, refused.getStatus());
        }
        assertEquals(0L, tokens("gone-customer-phone"));
        assertEquals(0L, devices(customer));
    }

    /// Registrations one after another for a full account each make their
    /// own room, oldest first. Taken together, the registrations wait for one
    /// another behind the account's profile row, so this is also what two of
    /// them sent at once come to.
    @Test
    void eachNewDeviceOfAFullAccountReplacesTheOldest() throws Exception {
        String full = People.customer(accounts, "push-cap2-full");
        for (int i = 0; i < 8; i++) {
            pushDevices.register(full, new PushDeviceDto("fcm", "cap2-full-" + i, "and"));
        }
        db.execute("UPDATE push_device SET updated_at = 1 WHERE token = ?",
                new Object[] {"cap2-full-5"});
        db.execute("UPDATE push_device SET updated_at = 2 WHERE token = ?",
                new Object[] {"cap2-full-2"});
        pushDevices.register(full, new PushDeviceDto("fcm", "cap2-new-a", "and"));
        pushDevices.register(full, new PushDeviceDto("apns", "cap2-new-b", "ios"));
        assertEquals(8L, devices(full));
        assertEquals(0L, tokens("cap2-full-5"));
        assertEquals(0L, tokens("cap2-full-2"));
        assertEquals(1L, tokens("cap2-new-a"));
        assertEquals(1L, tokens("cap2-new-b"));
    }
}
