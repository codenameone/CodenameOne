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
import com.codenameone.examples.savor.account.DemoAccounts;
import com.codenameone.examples.savor.api.PushDeviceDto;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.push.PushDevices;
import com.codenameone.examples.savor.push.PushSender;
import com.codenameone.examples.savor.push.RecordingPushSender;
import com.codenameone.examples.savor.push.OrderPushes;
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

/// The push notifications an order sends, the devices they go to, and the Live
/// Activity a customer follows it in.
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
    private OrderPushes pushes;

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

    private void accept(Trips.Trip trip, String id) throws Exception {
        Trips.Kitchen at = Trips.kitchen(mvc, trip.fromLat, trip.fromLng);
        mvc.perform(People.asMerchant(body(post("/api/merchant/orders/" + id + "/accept"),
                "{\"prepMinutes\":15}"), at.merchant)).andExpect(status().isOk());
    }

    private void ready(Trips.Trip trip, String id) throws Exception {
        Trips.Kitchen at = Trips.kitchen(mvc, trip.fromLat, trip.fromLng);
        mvc.perform(People.asMerchant(post("/api/merchant/orders/" + id + "/ready"), at.merchant))
                .andExpect(status().isOk());
    }

    private void courier(String courier, String id, String step) throws Exception {
        mvc.perform(People.asCourier(post(("accept".equals(step) ? "/api/courier/offers/"
                : "/api/courier/deliveries/") + id + "/" + step), courier)).andExpect(status().isOk());
    }

    private void follow(String customer, String id, String activity, int expected) throws Exception {
        mvc.perform(People.asCustomer(body(post("/api/orders/" + id + "/activity"),
                "{\"activityId\":\"" + activity + "\"}"), customer))
                .andExpect(status().is(expected));
    }

    @Test
    void theCustomerTheKitchenAndTheCourierAreEachToldTheirPart() throws Exception {
        // Reykjavik. Each test has a city to itself.
        String customer = People.customer(accounts, "push-rkv-customer");
        String courier = People.courier(accounts, "push-rkv-courier");
        Trips.Trip trip = Trips.trip(64.1467, -21.9428, 64.1597, -21.9548);
        Trips.Kitchen kitchen = Trips.kitchen(mvc, trip.fromLat, trip.fromLng);
        register(customer, "apns", "rkv-customer-phone");
        register(courier, "fcm", "rkv-courier-phone");
        register(kitchen.merchant, "fcm", "rkv-kitchen-tablet");
        Trips.online(mvc, courier, 64.1466, -21.9426);

        // The kitchen hears of the order, and nobody else does yet.
        String id = (String) Trips.place(mvc, customer, trip).get("id");
        List<RecordingPushSender.Sent> toKitchen = sent.sentTo("rkv-kitchen-tablet");
        assertEquals("New order", titles("rkv-kitchen-tablet"));
        assertEquals(id, data(toKitchen.get(0)).get("orderId"));
        assertEquals("merchant", data(toKitchen.get(0)).get("role"));
        assertTrue(((String) toKitchen.get(0).message().get("body")).contains("push-rkv-customer"));
        assertEquals("", titles("rkv-customer-phone"));

        // Accepting it sends for a courier, who has a few seconds to answer.
        accept(trip, id);
        assertEquals("Kitchen " + kitchenNumber(kitchen) + " is preparing your order",
                titles("rkv-customer-phone"));
        List<RecordingPushSender.Sent> toCourier = sent.sentTo("rkv-courier-phone");
        assertEquals("New delivery", titles("rkv-courier-phone"));
        assertEquals("OFFERED", data(toCourier.get(0)).get("state"));
        assertEquals("courier", data(toCourier.get(0)).get("role"));
        assertEquals("fcm", toCourier.get(0).provider());
        assertTrue(((Number) toCourier.get(0).message().get("ttl")).intValue() > 0);

        courier(courier, id, "accept");
        courier(courier, id, "arrived");
        // The courier is told the food is ready; the customer is not bothered.
        ready(trip, id);
        assertEquals("New delivery|Order ready", titles("rkv-courier-phone"));
        courier(courier, id, "start");
        courier(courier, id, "complete");
        assertEquals("Kitchen " + kitchenNumber(kitchen) + " is preparing your order"
                + "|Your order is on its way|Your order has arrived", titles("rkv-customer-phone"));
        String body = (String) sent.sentTo("rkv-customer-phone").get(1).message().get("body");
        assertTrue(body.contains("push-rkv-courier"), body);
        // Without a Live Activity there is nothing to move on.
        for (RecordingPushSender.Sent each : sent.sentTo("rkv-customer-phone")) {
            assertNull(surface(each));
            assertEquals(id, data(each).get("orderId"));
            assertEquals("customer", data(each).get("role"));
        }
        assertEquals("New order", titles("rkv-kitchen-tablet"));
    }

    private static String kitchenNumber(Trips.Kitchen kitchen) {
        return kitchen.merchant.substring("kitchen".length(), kitchen.merchant.indexOf('@'));
    }

    @Test
    void anOrderThatEndsEarlyTellsWhoeverWasWaiting() throws Exception {
        // Ljubljana: the customer changes their mind; the kitchen is told.
        String customer = People.customer(accounts, "push-lju-customer");
        Trips.Trip trip = Trips.trip(46.0570, 14.5054, 46.0670, 14.5174);
        Trips.Kitchen kitchen = Trips.kitchen(mvc, trip.fromLat, trip.fromLng);
        register(customer, "fcm", "lju-customer-phone");
        register(kitchen.merchant, "fcm", "lju-kitchen-tablet");
        String id = (String) Trips.place(mvc, customer, trip).get("id");
        mvc.perform(People.asCustomer(post("/api/orders/" + id + "/cancel"), customer))
                .andExpect(status().isOk());
        assertEquals("New order|Order canceled", titles("lju-kitchen-tablet"));
        assertEquals("", titles("lju-customer-phone"));

        // Bratislava: an admin calls off an order a courier has; the customer,
        // the kitchen and the courier are each told.
        String btsCustomer = People.customer(accounts, "push-bts-customer");
        String btsCourier = People.courier(accounts, "push-bts-courier");
        Trips.Trip bts = Trips.trip(48.1487, 17.1076, 48.1601, 17.0909);
        register(btsCustomer, "fcm", "bts-customer-phone");
        register(btsCourier, "fcm", "bts-courier-phone");
        Trips.online(mvc, btsCourier, 48.1486, 17.1077);
        String taken = Trips.ask(mvc, btsCustomer, bts);
        courier(btsCourier, taken, "accept");
        mvc.perform(People.asAdmin(body(post("/api/admin/orders/" + taken + "/cancel"),
                "{\"reason\":\"Test\"}"), DemoAccounts.ADMIN)).andExpect(status().isOk());
        assertTrue(titles("bts-courier-phone").endsWith("|Delivery canceled"),
                titles("bts-courier-phone"));
        assertTrue(titles("bts-customer-phone").endsWith("|Your order was canceled"),
                titles("bts-customer-phone"));
    }

    @Test
    void theLiveActivityFollowsTheOrderToTheDoor() throws Exception {
        String customer = People.customer(accounts, "push-cph-customer");
        String courier = People.courier(accounts, "push-cph-courier");
        String other = People.customer(accounts, "push-cph-other");
        register(customer, "apns", "cph-customer-phone");
        Trips.online(mvc, courier, 55.6761, 12.5683);
        Trips.Trip trip = Trips.trip(55.6762, 12.5680, 55.6900, 12.5900);
        String id = (String) Trips.place(mvc, customer, trip).get("id");
        // Followed from the moment it is placed, and never somebody else's.
        follow(other, id, "act-1", 404);
        follow(customer, id, "act-1", 200);

        accept(trip, id);
        Map command = surface(last(sent.sentTo("cph-customer-phone")));
        assertEquals("live-update", command.get("operation"));
        assertEquals("act-1", command.get("id"));
        Map state = Json.parseObject((String) command.get("state"));
        assertEquals("Preparing your order", state.get("status"));
        assertEquals(0.35d, ((Number) state.get("progress")).doubleValue(), 0.001d);
        assertTrue(((String) state.get("restaurant")).startsWith("Kitchen "));
        courier(courier, id, "accept");
        ready(trip, id);
        assertEquals(0.55d, progress(surface(last(sent.sentTo("cph-customer-phone")))), 0.001d);
        courier(courier, id, "arrived");
        courier(courier, id, "start");
        command = surface(last(sent.sentTo("cph-customer-phone")));
        assertEquals(0.8d, progress(command), 0.001d);
        state = Json.parseObject((String) command.get("state"));
        assertEquals("On its way", state.get("status"));
        assertEquals("push-cph-courier", state.get("courier"));
        assertEquals("Green Vespa", state.get("car"));
        assertEquals("T push-cph-courier", state.get("plate"));

        // The courier rides. The phone has just been updated, so the next
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
        state = Json.parseObject((String) surface(update).get("state"));
        assertTrue(((String) state.get("eta")).endsWith(" min"), state.toString());

        courier(courier, id, "complete");
        RecordingPushSender.Sent done = last(sent.sentTo("cph-customer-phone"));
        assertEquals("Your order has arrived", done.message().get("title"));
        assertEquals("live-end", surface(done).get("operation"));
        assertEquals("act-1", surface(done).get("id"));
        // And it is forgotten: the courier goes on reporting, the customer
        // hears no more.
        int after = sent.sentTo("cph-customer-phone").size();
        Trips.online(mvc, courier, 55.6800, 12.5700);
        assertEquals(after, sent.sentTo("cph-customer-phone").size());
    }

    @Test
    void theSettingsDecideWhatIsSaidButNotWhatIsShown() throws Exception {
        // Sofia: a customer who turned order updates off hears nothing of the
        // kitchen, while the lock screen still lets go of the order when an
        // admin calls it off.
        String customer = People.customer(accounts, "push-sof-customer");
        String quiet = "{\"language\":\"en\",\"theme\":\"system\",\"units\":\"km\","
                + "\"notifyDeliveryUpdates\":false,\"notifyReceipts\":true,"
                + "\"notifyPromotions\":false,\"defaultPaymentMethodId\":\"cash\","
                + "\"defaultTipPercent\":0}";
        mvc.perform(People.asCustomer(body(put("/api/account/preferences"), quiet), customer))
                .andExpect(status().isOk());
        register(customer, "apns", "sof-customer-phone");
        Trips.Trip trip = Trips.trip(42.6978, 23.3220, 42.7054, 23.3112);
        String id = (String) Trips.place(mvc, customer, trip).get("id");
        follow(customer, id, "act-sof", 200);
        accept(trip, id);
        List<RecordingPushSender.Sent> toCustomer = sent.sentTo("sof-customer-phone");
        assertEquals(1, toCustomer.size());
        assertEquals(Boolean.TRUE, toCustomer.get(0).message().get("silent"));
        assertEquals("live-update", surface(toCustomer.get(0)).get("operation"));
        mvc.perform(People.asAdmin(body(post("/api/admin/orders/" + id + "/cancel"),
                "{\"reason\":\"Test\"}"), DemoAccounts.ADMIN)).andExpect(status().isOk());
        toCustomer = sent.sentTo("sof-customer-phone");
        assertEquals(2, toCustomer.size());
        assertEquals(Boolean.TRUE, toCustomer.get(1).message().get("silent"));
        assertEquals("live-end", surface(toCustomer.get(1)).get("operation"));
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
        Trips.Trip trip = Trips.trip(52.3677, 4.9040, 52.3800, 4.9100);
        String id = (String) Trips.place(mvc, first, trip).get("id");
        accept(trip, id);
        String preparing = titles("ams-first-tablet");
        assertEquals("", titles("ams-shared-phone"));
        assertTrue(preparing.endsWith(" is preparing your order"), preparing);

        // Signing out forgets the device, and only one's own.
        mvc.perform(People.asCustomer(body(post("/api/account/push/remove"),
                device("fcm", "ams-shared-phone")), first)).andExpect(status().isOk());
        mvc.perform(People.asCustomer(body(post("/api/account/push/remove"),
                device("fcm", "ams-first-tablet")), first)).andExpect(status().isOk());
        courier(courier, id, "accept");
        ready(trip, id);
        courier(courier, id, "arrived");
        courier(courier, id, "start");
        assertEquals(preparing, titles("ams-first-tablet"));

        // Closing the account forgets every device it had.
        String customer = People.customer(accounts, "push-ams-closing");
        register(customer, "apns", "ams-closing-phone");
        accounts.close(customer, People.PASSWORD);
        assertEquals(0L, devices(customer));
        assertEquals(1L, devices(second));
    }

    @Test
    void anActivityIsRefusedOnceTheOrderHasEnded() throws Exception {
        // Helsinki: the order is delivered. Oslo: the kitchen turns it down.
        String customer = People.customer(accounts, "push-hel-customer");
        String courier = People.courier(accounts, "push-hel-courier");
        Trips.online(mvc, courier, 60.1699, 24.9384);
        String done = Trips.ask(mvc, customer, Trips.trip(60.1700, 24.9383, 60.1800, 24.9500));
        courier(courier, done, "accept");
        courier(courier, done, "arrived");
        courier(courier, done, "start");
        // Still on the road: the activity is taken.
        follow(customer, done, "act-hel", 200);
        courier(courier, done, "complete");
        follow(customer, done, "act-hel-2", 409);

        String oslo = People.customer(accounts, "push-osl-customer");
        Trips.Trip trip = Trips.trip(59.9140, 10.7521, 59.9250, 10.7600);
        Trips.Kitchen kitchen = Trips.kitchen(mvc, trip.fromLat, trip.fromLng);
        String rejected = (String) Trips.place(mvc, oslo, trip).get("id");
        mvc.perform(People.asMerchant(body(post("/api/merchant/orders/" + rejected + "/reject"),
                "{\"reason\":\"Out of dough\"}"), kitchen.merchant)).andExpect(status().isOk());
        follow(oslo, rejected, "act-osl", 409);
    }

    /// An end the push provider refused is not lost with the activity: the
    /// delivery's transition is never retried, so the sweep sends it again.
    @Test
    void anEndTheProviderRefusedIsSentAgain() throws Exception {
        String customer = People.customer(accounts, "push-rey-customer");
        String courier = People.courier(accounts, "push-rey-courier");
        register(customer, "apns", "rey-customer-phone");
        Trips.online(mvc, courier, 64.1466, -21.9426);
        String id = Trips.ask(mvc, customer, Trips.trip(64.1467, -21.9425, 64.1500, -21.9000));
        courier(courier, id, "accept");
        follow(customer, id, "act-rey", 200);

        sent.refuse(1);
        mvc.perform(People.asAdmin(body(post("/api/admin/orders/" + id + "/cancel"),
                "{\"reason\":\"Test\"}"), DemoAccounts.ADMIN)).andExpect(status().isOk());
        assertEquals(1L, activities(id), "the refused end keeps its activity");
        int before = sent.sentTo("rey-customer-phone").size();

        // Not while the call that ended the order may still be sending it...
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
        String id = (String) Trips.place(mvc, customer,
                Trips.trip(59.4371, 24.7535, 59.4400, 24.7700)).get("id");
        follow(customer, id, "act-tll", 200);

        sent.leaveUnconfirmed(1);
        mvc.perform(People.asCustomer(post("/api/orders/" + id + "/cancel"), customer))
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

    /// The notification of a change that is no longer the order's latest says
    /// nothing, in either order: composed after the order was cancelled it does
    /// not tell the customer their food is cooking, and composed before the
    /// cancellation's own it leaves the Live Activity for that one to end.
    @Test
    void aStaleChangeSaysNothingAndLeavesTheActivityToTheLaterOne() throws Exception {
        // Valletta: the acceptance is composed after the cancellation was.
        String customer = People.customer(accounts, "push-mla-customer");
        register(customer, "apns", "mla-customer-phone");
        String id = (String) Trips.place(mvc, customer,
                Trips.trip(35.8990, 14.5145, 35.9100, 14.5000)).get("id");
        follow(customer, id, "act-mla", 200);
        mvc.perform(People.asCustomer(post("/api/orders/" + id + "/cancel"), customer))
                .andExpect(status().isOk());
        int told = sent.sentTo("mla-customer-phone").size();
        assertEquals("live-end", surface(last(sent.sentTo("mla-customer-phone")))
                .get("operation"));
        pushes.orderChanged(id, OrderState.ACCEPTED);
        assertEquals(told, sent.sentTo("mla-customer-phone").size());

        // Gdansk: the acceptance is composed after the order was cancelled but
        // before the cancellation is. It neither speaks nor forgets the
        // activity, and the cancellation then ends it.
        String gdnCustomer = People.customer(accounts, "push-gdn-customer");
        register(gdnCustomer, "apns", "gdn-customer-phone");
        String gdn = (String) Trips.place(mvc, gdnCustomer,
                Trips.trip(54.3521, 18.6465, 54.3650, 18.6300)).get("id");
        follow(gdnCustomer, gdn, "act-gdn", 200);
        int before = sent.sentTo("gdn-customer-phone").size();
        db.execute("UPDATE customer_order SET state = 'CANCELLED_BY_CUSTOMER' WHERE id = ?",
                new Object[] {gdn});
        pushes.orderChanged(gdn, OrderState.ACCEPTED);
        assertEquals(before, sent.sentTo("gdn-customer-phone").size());
        assertEquals(1L, activities(gdn));
        pushes.orderChanged(gdn, OrderState.CANCELLED_BY_CUSTOMER);
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
