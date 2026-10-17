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
package com.codenameone.examples.savor;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.api.PushDeviceDto;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.push.PushDevices;
import com.codenameone.examples.savor.push.PushSender;
import com.codenameone.examples.savor.push.RecordingPushSender;
import com.codenameone.examples.savor.push.DeliveryPushes;
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

/// The push notifications a delivery sends, the devices they go to, and the Live
/// Activity a customer follows the courier in.
///
/// The `test` profile keeps every notification instead of sending it, so each
/// test reads what each phone would have been sent.
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
    @Autowired
    private DeliveryPushes pushes;

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

    /// The titles `token` was sent, in order; a silent update counts as "".
    private String titles(String token) {
        StringBuilder out = new StringBuilder();
        for (RecordingPushSender.Sent each : sent.sentTo(token)) {
            Object title = each.message().get("title");
            out.append(out.length() > 0 ? "|" : "").append(title == null ? "" : title);
        }
        return out.toString();
    }

    private static Map data(RecordingPushSender.Sent message) {
        return (Map) message.message().get("data");
    }

    private static Map surface(RecordingPushSender.Sent message) {
        return (Map) message.message().get("surface");
    }

    private static double progress(Map command) throws Exception {
        return ((Number) Json.parseObject((String) command.get("state")).get("progress"))
                .doubleValue();
    }

    private static RecordingPushSender.Sent last(List<RecordingPushSender.Sent> list) {
        return list.get(list.size() - 1);
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
    void customersAndCouriersAreToldOfEveryStepOfADelivery() throws Exception {
        // Reykjavik. Each test has a city to itself.
        String customer = People.customer(accounts, "push-rkv-customer");
        String courier = People.courier(accounts, "push-rkv-courier");
        register(customer, "apns", "rkv-customer-phone");
        register(courier, "fcm", "rkv-courier-phone");
        Trips.online(mvc, courier, 64.1466, -21.9426);

        Map asked = Trips.json(mvc, People.asCustomer(body(post("/api/deliveries"),
                People.delivery(64.1467, -21.9428, 64.1597, -21.9548)), customer));
        String id = (String) asked.get("id");
        // The offer goes to the courier, who has a few seconds to answer it.
        List<RecordingPushSender.Sent> toCourier = sent.sentTo("rkv-courier-phone");
        assertEquals("New delivery request", titles("rkv-courier-phone"));
        assertEquals(id, data(toCourier.get(0)).get("deliveryId"));
        assertEquals("OFFERED", data(toCourier.get(0)).get("state"));
        assertEquals("courier", data(toCourier.get(0)).get("role"));
        assertEquals("fcm", toCourier.get(0).provider());
        assertTrue(((Number) toCourier.get(0).message().get("ttl")).intValue() > 0);
        assertEquals("", titles("rkv-customer-phone"));

        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk());
        List<RecordingPushSender.Sent> toCustomer = sent.sentTo("rkv-customer-phone");
        assertEquals("Your courier is on the way", titles("rkv-customer-phone"));
        String body = (String) toCustomer.get(0).message().get("body");
        assertTrue(body.contains("push-rkv-courier") && body.contains("Green Fiat 500"), body);
        assertEquals("apns", toCustomer.get(0).provider());
        assertEquals("customer", data(toCustomer.get(0)).get("role"));
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/arrived"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/start"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/complete"), courier))
                .andExpect(status().isOk());
        assertEquals("Your courier is on the way|Your courier is here|Your trip has started"
                + "|You've arrived", titles("rkv-customer-phone"));
        // Without a Live Activity there is nothing to move on.
        for (RecordingPushSender.Sent each : sent.sentTo("rkv-customer-phone")) {
            assertNull(surface(each));
            assertEquals(id, data(each).get("deliveryId"));
        }
        assertEquals("New delivery request", titles("rkv-courier-phone"));
    }

    @Test
    void cancellingTellsTheOtherSide() throws Exception {
        // Ljubljana: the courier gives up on the way to the pickup.
        String customer = People.customer(accounts, "push-lju-customer");
        String courier = People.courier(accounts, "push-lju-courier");
        register(customer, "fcm", "lju-customer-phone");
        register(courier, "fcm", "lju-courier-phone");
        Trips.online(mvc, courier, 46.0569, 14.5058);
        String id = (String) Trips.json(mvc, People.asCustomer(body(post("/api/deliveries"),
                People.delivery(46.0570, 14.5054, 46.0670, 14.5174)), customer)).get("id");
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/cancel"), courier))
                .andExpect(status().isOk());
        assertEquals("Your courier is on the way|Your courier canceled", titles("lju-customer-phone"));


        // Bratislava: the customer changes their mind while the offer is out.
        String bratislavaCustomer = People.customer(accounts, "push-bts-customer");
        String bratislavaCourier = People.courier(accounts, "push-bts-courier");
        register(bratislavaCourier, "fcm", "bts-courier-phone");
        Trips.online(mvc, bratislavaCourier, 48.1486, 17.1077);
        String offered = (String) Trips.json(mvc, People.asCustomer(body(post("/api/deliveries"),
                People.delivery(48.1487, 17.1076, 48.1601, 17.0909)), bratislavaCustomer)).get("id");
        mvc.perform(People.asCustomer(post("/api/deliveries/" + offered + "/cancel"), bratislavaCustomer))
                .andExpect(status().isOk());
        assertEquals("New delivery request|Delivery canceled", titles("bts-courier-phone"));
    }

    @Test
    void theLiveActivityFollowsTheCarAndEndsWithTheDelivery() throws Exception {
        String customer = People.customer(accounts, "push-cph-customer");
        String courier = People.courier(accounts, "push-cph-courier");
        String other = People.customer(accounts, "push-cph-other");
        register(customer, "apns", "cph-customer-phone");
        Trips.online(mvc, courier, 55.6761, 12.5683);
        String id = (String) Trips.json(mvc, People.asCustomer(body(post("/api/deliveries"),
                People.delivery(55.6762, 12.5680, 55.6900, 12.5900)), customer)).get("id");
        // No activity before there is a courier on the way, and none on
        // somebody else's delivery.
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + id + "/activity"),
                "{\"activityId\":\"act-1\"}"), customer)).andExpect(status().isConflict());
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + id + "/activity"),
                "{\"activityId\":\"act-1\"}"), other)).andExpect(status().isNotFound());
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + id + "/activity"),
                "{\"activityId\":\"act-1\"}"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityId").value("act-1"));

        // The car moves. The phone has just drawn the activity, so the first
        // update waits its turn...
        int before = sent.sentTo("cph-customer-phone").size();
        Trips.online(mvc, courier, 55.6765, 12.5682);
        assertEquals(before, sent.sentTo("cph-customer-phone").size());
        // ...and once it is due, one report sends one, silently.
        db.execute("UPDATE delivery_activity SET sent_at = 0 WHERE delivery_id = ?", new Object[] {id});
        Trips.online(mvc, courier, 55.6764, 12.5681);
        Trips.online(mvc, courier, 55.6763, 12.5681);
        List<RecordingPushSender.Sent> toCustomer = sent.sentTo("cph-customer-phone");
        assertEquals(before + 1, toCustomer.size());
        RecordingPushSender.Sent update = last(toCustomer);
        assertEquals(Boolean.TRUE, update.message().get("silent"));
        Map command = surface(update);
        assertEquals("live-update", command.get("operation"));
        assertEquals("act-1", command.get("id"));
        Map state = Json.parseObject((String) command.get("state"));
        assertEquals("On the way", state.get("status"));
        assertEquals("push-cph-courier", state.get("courier"));
        assertEquals("Green Fiat 500", state.get("car"));
        assertEquals("T push-cph-courier", state.get("plate"));
        assertEquals(0.25d, ((Number) state.get("progress")).doubleValue(), 0.001d);
        assertTrue(((String) state.get("eta")).endsWith(" min"), state.toString());

        // A change of state moves it on at once, with the notification.
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/arrived"), courier))
                .andExpect(status().isOk());
        command = surface(last(sent.sentTo("cph-customer-phone")));
        assertEquals("live-update", command.get("operation"));
        assertEquals(0.5d, progress(command), 0.001d);
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/start"), courier))
                .andExpect(status().isOk());
        assertEquals(0.75d, progress(surface(last(sent.sentTo("cph-customer-phone")))), 0.001d);
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/complete"), courier))
                .andExpect(status().isOk());
        RecordingPushSender.Sent done = last(sent.sentTo("cph-customer-phone"));
        assertEquals("You've arrived", done.message().get("title"));
        assertEquals("live-end", surface(done).get("operation"));
        assertEquals("act-1", surface(done).get("id"));
        // And it is forgotten: the car goes on reporting, the customer hears no more.
        int after = sent.sentTo("cph-customer-phone").size();
        Trips.online(mvc, courier, 55.6800, 12.5700);
        assertEquals(after, sent.sentTo("cph-customer-phone").size());
    }

    @Test
    void theSettingsDecideWhatIsSaidButNotWhatIsShown() throws Exception {
        // Sofia: a customer who turned delivery updates off hears nothing of the
        // courier, while the lock screen still lets go of the cancelled delivery.
        String customer = People.customer(accounts, "push-sof-customer");
        String courier = People.courier(accounts, "push-sof-courier");
        String quiet = "{\"language\":\"en\",\"theme\":\"system\",\"units\":\"km\","
                + "\"notifyDeliveryUpdates\":false,\"notifyReceipts\":true,"
                + "\"notifyPromotions\":false,\"courierGender\":\"\",\"quietDelivery\":false,"
                + "\"accessibleVehicle\":false,\"petFriendly\":false,"
                + "\"defaultPaymentMethodId\":\"cash\",\"defaultTipPercent\":0,"
                + "\"shareTripsWith\":\"\"}";
        mvc.perform(People.asCustomer(body(put("/api/account/preferences"), quiet), customer))
                .andExpect(status().isOk());
        register(customer, "apns", "sof-customer-phone");
        Trips.online(mvc, courier, 42.6977, 23.3219);
        String id = (String) Trips.json(mvc, People.asCustomer(body(post("/api/deliveries"),
                People.delivery(42.6978, 23.3220, 42.7054, 23.3112)), customer)).get("id");
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + id + "/activity"),
                "{\"activityId\":\"act-sof\"}"), customer)).andExpect(status().isOk());
        assertEquals("", titles("sof-customer-phone"));
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/cancel"), courier))
                .andExpect(status().isOk());
        List<RecordingPushSender.Sent> toCustomer = sent.sentTo("sof-customer-phone");
        assertEquals(1, toCustomer.size());
        assertEquals(Boolean.TRUE, toCustomer.get(0).message().get("silent"));
        assertEquals("live-end", surface(toCustomer.get(0)).get("operation"));
    }

    @Test
    void aPhoneBelongsToWhoeverSignedInOnItLast() throws Exception {
        String first = People.customer(accounts, "push-ams-first");
        String second = People.customer(accounts, "push-ams-second");
        String courier = People.courier(accounts, "push-ams-courier");
        register(first, "fcm", "ams-shared-phone");
        register(second, "fcm", "ams-shared-phone");
        register(first, "fcm", "ams-first-tablet");
        Trips.online(mvc, courier, 52.3676, 4.9041);
        String id = (String) Trips.json(mvc, People.asCustomer(body(post("/api/deliveries"),
                People.delivery(52.3677, 4.9040, 52.3800, 4.9100)), first)).get("id");
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk());
        assertEquals("", titles("ams-shared-phone"));
        assertEquals("Your courier is on the way", titles("ams-first-tablet"));

        // Signing out forgets the device, and only one's own.
        mvc.perform(People.asCustomer(body(post("/api/account/push/remove"),
                device("fcm", "ams-shared-phone")), first)).andExpect(status().isOk());
        mvc.perform(People.asCustomer(body(post("/api/account/push/remove"),
                device("fcm", "ams-first-tablet")), first)).andExpect(status().isOk());
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/arrived"), courier))
                .andExpect(status().isOk());
        assertEquals("Your courier is on the way", titles("ams-first-tablet"));

        // Closing the account forgets every device it had.
        String customer = People.customer(accounts, "push-ams-closing");
        register(customer, "apns", "ams-closing-phone");
        accounts.close(customer, People.PASSWORD);
        assertEquals(0L, devices(customer));
        assertEquals(1L, devices(second));
    }

    @Test
    void anActivityIsRefusedOnceTheDeliveryHasEnded() throws Exception {
        // Helsinki: the delivery is completed. Oslo: the courier cancels it.
        String customer = People.customer(accounts, "push-hel-customer");
        String courier = People.courier(accounts, "push-hel-courier");
        Trips.online(mvc, courier, 60.1699, 24.9384);
        String done = (String) Trips.json(mvc, People.asCustomer(body(post("/api/deliveries"),
                People.delivery(60.1700, 24.9383, 60.1800, 24.9500)), customer)).get("id");
        mvc.perform(People.asCourier(post("/api/courier/offers/" + done + "/accept"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + done + "/arrived"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + done + "/start"), courier))
                .andExpect(status().isOk());
        // Still on the road: the activity is taken.
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + done + "/activity"),
                "{\"activityId\":\"act-hel\"}"), customer)).andExpect(status().isOk());
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + done + "/complete"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + done + "/activity"),
                "{\"activityId\":\"act-hel-2\"}"), customer)).andExpect(status().isConflict());

        String oslo = People.customer(accounts, "push-osl-customer");
        String oslocourier = People.courier(accounts, "push-osl-courier");
        Trips.online(mvc, oslocourier, 59.9139, 10.7522);
        String canceled = (String) Trips.json(mvc, People.asCustomer(body(post("/api/deliveries"),
                People.delivery(59.9140, 10.7521, 59.9250, 10.7600)), oslo)).get("id");
        mvc.perform(People.asCourier(post("/api/courier/offers/" + canceled + "/accept"),
                oslocourier)).andExpect(status().isOk());
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + canceled + "/cancel"),
                oslocourier)).andExpect(status().isOk());
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + canceled + "/activity"),
                "{\"activityId\":\"act-osl\"}"), oslo)).andExpect(status().isConflict());
    }

    /// An end the push provider refused is not lost with the activity: the
    /// delivery's transition is never retried, so the sweep sends it again.
    @Test
    void anEndTheProviderRefusedIsSentAgain() throws Exception {
        String customer = People.customer(accounts, "push-rey-customer");
        String courier = People.courier(accounts, "push-rey-courier");
        register(customer, "apns", "rey-customer-phone");
        Trips.online(mvc, courier, 64.1466, -21.9426);
        String id = (String) Trips.json(mvc, People.asCustomer(body(post("/api/deliveries"),
                People.delivery(64.1467, -21.9425, 64.1500, -21.9000)), customer)).get("id");
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + id + "/activity"),
                "{\"activityId\":\"act-rey\"}"), customer)).andExpect(status().isOk());

        sent.refuse(1);
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/cancel"), courier))
                .andExpect(status().isOk());
        assertEquals(1L, activities(id), "the refused end keeps its activity");
        int before = sent.sentTo("rey-customer-phone").size();

        // Not while the call that ended the delivery may still be sending it...
        pushes.endLeftBehind();
        assertEquals(before, sent.sentTo("rey-customer-phone").size());
        // ...and once that is past, once.
        db.execute("UPDATE delivery_activity SET ended_at = ? WHERE delivery_id = ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 120000L), id});
        pushes.endLeftBehind();
        List<RecordingPushSender.Sent> toCustomer = sent.sentTo("rey-customer-phone");
        assertEquals(before + 1, toCustomer.size());
        Map command = surface(last(toCustomer));
        assertEquals("live-end", command.get("operation"));
        assertEquals("act-rey", command.get("id"));
        assertEquals(0L, activities(id));
        pushes.endLeftBehind();
        assertEquals(before + 1, sent.sentTo("rey-customer-phone").size());
    }

    /// A send that returned is not a send the service took: the real sender
    /// hands the message to another thread and learns later, and only then is
    /// the activity forgotten.
    @Test
    void anEndTheServiceNeverConfirmedIsKept() throws Exception {
        String customer = People.customer(accounts, "push-tll-customer");
        String courier = People.courier(accounts, "push-tll-courier");
        register(customer, "apns", "tll-customer-phone");
        Trips.online(mvc, courier, 59.4370, 24.7536);
        String id = (String) Trips.json(mvc, People.asCustomer(body(post("/api/deliveries"),
                People.delivery(59.4371, 24.7535, 59.4400, 24.7700)), customer)).get("id");
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + id + "/activity"),
                "{\"activityId\":\"act-tll\"}"), customer)).andExpect(status().isOk());

        sent.leaveUnconfirmed(1);
        mvc.perform(People.asCustomer(post("/api/deliveries/" + id + "/cancel"), customer))
                .andExpect(status().isOk());
        assertEquals("live-end", surface(last(sent.sentTo("tll-customer-phone"))).get("operation"));
        assertEquals(1L, activities(id), "sent, but never confirmed");

        db.execute("UPDATE delivery_activity SET ended_at = ? WHERE delivery_id = ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 120000L), id});
        pushes.endLeftBehind();
        assertEquals(0L, activities(id));
    }

    /// An end still undelivered when iOS would have ended the activity itself
    /// is dropped, not sent.
    @Test
    void anEndTooOldToBeOnScreenIsDropped() throws Exception {
        db.execute("INSERT INTO delivery_activity (delivery_id, username, activity_id, sent_at, ended_at) "
                + "VALUES ('old-delivery', 'push-old-customer', 'act-old', 0, 1)", new Object[0]);
        int before = sent.all().size();
        pushes.endLeftBehind();
        assertEquals(0L, activities("old-delivery"));
        assertEquals(before, sent.all().size());
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

    /// The notification of a change that is no longer the delivery's latest says
    /// nothing, in either order: composed after the delivery was cancelled it does
    /// not tell the customer a courier is coming, and composed before the
    /// cancellation's own it leaves the Live Activity for that one to end.
    @Test
    void aStaleChangeSaysNothingAndLeavesTheActivityToTheLaterOne() throws Exception {
        // Valletta: the acceptance is composed after the cancellation was.
        String customer = People.customer(accounts, "push-mla-customer");
        String courier = People.courier(accounts, "push-mla-courier");
        register(customer, "apns", "mla-customer-phone");
        Trips.online(mvc, courier, 35.8989, 14.5146);
        String id = (String) Trips.json(mvc, People.asCustomer(body(post("/api/deliveries"),
                People.delivery(35.8990, 14.5145, 35.9100, 14.5000)), customer)).get("id");
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + id + "/activity"),
                "{\"activityId\":\"act-mla\"}"), customer)).andExpect(status().isOk());
        mvc.perform(People.asCustomer(post("/api/deliveries/" + id + "/cancel"), customer))
                .andExpect(status().isOk());
        int told = sent.sentTo("mla-customer-phone").size();
        assertEquals("live-end", surface(last(sent.sentTo("mla-customer-phone")))
                .get("operation"));
        pushes.deliveryChanged(id, DeliveryState.ACCEPTED, customer, courier);
        assertEquals(told, sent.sentTo("mla-customer-phone").size());

        // Gdansk: the acceptance is composed after the delivery was cancelled but
        // before the cancellation is. It neither speaks nor forgets the
        // activity, and the cancellation then ends it.
        String gdnCustomer = People.customer(accounts, "push-gdn-customer");
        String gdnCourier = People.courier(accounts, "push-gdn-courier");
        register(gdnCustomer, "apns", "gdn-customer-phone");
        Trips.online(mvc, gdnCourier, 54.3520, 18.6466);
        String gdn = (String) Trips.json(mvc, People.asCustomer(body(post("/api/deliveries"),
                People.delivery(54.3521, 18.6465, 54.3650, 18.6300)), gdnCustomer)).get("id");
        mvc.perform(People.asCourier(post("/api/courier/offers/" + gdn + "/accept"), gdnCourier))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + gdn + "/activity"),
                "{\"activityId\":\"act-gdn\"}"), gdnCustomer)).andExpect(status().isOk());
        int before = sent.sentTo("gdn-customer-phone").size();
        db.execute("UPDATE delivery SET state = 'CANCELLED_BY_CUSTOMER' WHERE id = ?",
                new Object[] {gdn});
        pushes.deliveryChanged(gdn, DeliveryState.ACCEPTED, gdnCustomer, gdnCourier);
        assertEquals(before, sent.sentTo("gdn-customer-phone").size());
        assertEquals(1L, activities(gdn));
        pushes.deliveryChanged(gdn, DeliveryState.CANCELLED_BY_CUSTOMER, gdnCustomer, gdnCourier);
        RecordingPushSender.Sent ended = last(sent.sentTo("gdn-customer-phone"));
        assertEquals("live-end", surface(ended).get("operation"));
        assertEquals("act-gdn", surface(ended).get("id"));
        assertEquals(0L, activities(gdn));
    }

    private long activities(String deliveryId) throws Exception {
        Map row = db.queryOne("SELECT COUNT(*) AS n FROM delivery_activity WHERE delivery_id = ?",
                new Object[] {deliveryId});
        return ((Number) row.get("n")).longValue();
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
}
