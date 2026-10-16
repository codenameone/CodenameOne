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
package com.codenameone.examples.wayline;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.api.PushDeviceDto;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.push.PushDevices;
import com.codenameone.examples.wayline.push.PushSender;
import com.codenameone.examples.wayline.push.RecordingPushSender;
import com.codenameone.examples.wayline.push.RidePushes;
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

/// The push notifications a ride sends, the devices they go to, and the Live
/// Activity a rider follows the driver in.
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
    private RidePushes pushes;

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
        mvc.perform(People.asRider(body(post("/api/account/push"), device(provider, token)),
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
        String rider = People.rider(accounts, "push-reg-rider");
        mvc.perform(People.asRider(body(post("/api/account/push"),
                device("gcm", "reg-token-1")), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("fcm"))
                .andExpect(jsonPath("$.token").value("reg-token-1"));
        // An alias is the same device, not a second one.
        mvc.perform(People.asRider(body(post("/api/account/push"),
                device("fcm", "reg-token-1")), rider)).andExpect(status().isOk());
        mvc.perform(People.asRider(body(post("/api/account/push"),
                device("pigeon", "reg-token-2")), rider)).andExpect(status().isBadRequest());
        mvc.perform(People.asRider(body(post("/api/account/push"),
                device("apns", " ")), rider)).andExpect(status().isBadRequest());
    }

    @Test
    void ridersAndDriversAreToldOfEveryStepOfARide() throws Exception {
        // Reykjavik. Each test has a city to itself.
        String rider = People.rider(accounts, "push-rkv-rider");
        String driver = People.driver(accounts, "push-rkv-driver");
        register(rider, "apns", "rkv-rider-phone");
        register(driver, "fcm", "rkv-driver-phone");
        Trips.online(mvc, driver, 64.1466, -21.9426);

        Map asked = Trips.json(mvc, People.asRider(body(post("/api/rides"),
                People.ride(64.1467, -21.9428, 64.1597, -21.9548)), rider));
        String id = (String) asked.get("id");
        // The offer goes to the driver, who has a few seconds to answer it.
        List<RecordingPushSender.Sent> toDriver = sent.sentTo("rkv-driver-phone");
        assertEquals("New ride request", titles("rkv-driver-phone"));
        assertEquals(id, data(toDriver.get(0)).get("rideId"));
        assertEquals("OFFERED", data(toDriver.get(0)).get("state"));
        assertEquals("driver", data(toDriver.get(0)).get("role"));
        assertEquals("fcm", toDriver.get(0).provider());
        assertTrue(((Number) toDriver.get(0).message().get("ttl")).intValue() > 0);
        assertEquals("", titles("rkv-rider-phone"));

        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), driver))
                .andExpect(status().isOk());
        List<RecordingPushSender.Sent> toRider = sent.sentTo("rkv-rider-phone");
        assertEquals("Your driver is on the way", titles("rkv-rider-phone"));
        String body = (String) toRider.get(0).message().get("body");
        assertTrue(body.contains("push-rkv-driver") && body.contains("Green Fiat 500"), body);
        assertEquals("apns", toRider.get(0).provider());
        assertEquals("rider", data(toRider.get(0)).get("role"));
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/arrived"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/start"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/complete"), driver))
                .andExpect(status().isOk());
        assertEquals("Your driver is on the way|Your driver is here|Your trip has started"
                + "|You've arrived", titles("rkv-rider-phone"));
        // Without a Live Activity there is nothing to move on.
        for (RecordingPushSender.Sent each : sent.sentTo("rkv-rider-phone")) {
            assertNull(surface(each));
            assertEquals(id, data(each).get("rideId"));
        }
        assertEquals("New ride request", titles("rkv-driver-phone"));
    }

    @Test
    void cancellingTellsTheOtherSide() throws Exception {
        // Ljubljana: the driver gives up on the way to the pickup.
        String rider = People.rider(accounts, "push-lju-rider");
        String driver = People.driver(accounts, "push-lju-driver");
        register(rider, "fcm", "lju-rider-phone");
        register(driver, "fcm", "lju-driver-phone");
        Trips.online(mvc, driver, 46.0569, 14.5058);
        String id = (String) Trips.json(mvc, People.asRider(body(post("/api/rides"),
                People.ride(46.0570, 14.5054, 46.0670, 14.5174)), rider)).get("id");
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/cancel"), driver))
                .andExpect(status().isOk());
        assertEquals("Your driver is on the way|Your driver canceled", titles("lju-rider-phone"));


        // Bratislava: the rider changes their mind while the offer is out.
        String bratislavaRider = People.rider(accounts, "push-bts-rider");
        String bratislavaDriver = People.driver(accounts, "push-bts-driver");
        register(bratislavaDriver, "fcm", "bts-driver-phone");
        Trips.online(mvc, bratislavaDriver, 48.1486, 17.1077);
        String offered = (String) Trips.json(mvc, People.asRider(body(post("/api/rides"),
                People.ride(48.1487, 17.1076, 48.1601, 17.0909)), bratislavaRider)).get("id");
        mvc.perform(People.asRider(post("/api/rides/" + offered + "/cancel"), bratislavaRider))
                .andExpect(status().isOk());
        assertEquals("New ride request|Ride canceled", titles("bts-driver-phone"));
    }

    @Test
    void theLiveActivityFollowsTheCarAndEndsWithTheRide() throws Exception {
        String rider = People.rider(accounts, "push-cph-rider");
        String driver = People.driver(accounts, "push-cph-driver");
        String other = People.rider(accounts, "push-cph-other");
        register(rider, "apns", "cph-rider-phone");
        Trips.online(mvc, driver, 55.6761, 12.5683);
        String id = (String) Trips.json(mvc, People.asRider(body(post("/api/rides"),
                People.ride(55.6762, 12.5680, 55.6900, 12.5900)), rider)).get("id");
        // No activity before there is a driver on the way, and none on
        // somebody else's ride.
        mvc.perform(People.asRider(body(post("/api/rides/" + id + "/activity"),
                "{\"activityId\":\"act-1\"}"), rider)).andExpect(status().isConflict());
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asRider(body(post("/api/rides/" + id + "/activity"),
                "{\"activityId\":\"act-1\"}"), other)).andExpect(status().isNotFound());
        mvc.perform(People.asRider(body(post("/api/rides/" + id + "/activity"),
                "{\"activityId\":\"act-1\"}"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityId").value("act-1"));

        // The car moves. The phone has just drawn the activity, so the first
        // update waits its turn...
        int before = sent.sentTo("cph-rider-phone").size();
        Trips.online(mvc, driver, 55.6765, 12.5682);
        assertEquals(before, sent.sentTo("cph-rider-phone").size());
        // ...and once it is due, one report sends one, silently.
        db.execute("UPDATE ride_activity SET sent_at = 0 WHERE ride_id = ?", new Object[] {id});
        Trips.online(mvc, driver, 55.6764, 12.5681);
        Trips.online(mvc, driver, 55.6763, 12.5681);
        List<RecordingPushSender.Sent> toRider = sent.sentTo("cph-rider-phone");
        assertEquals(before + 1, toRider.size());
        RecordingPushSender.Sent update = last(toRider);
        assertEquals(Boolean.TRUE, update.message().get("silent"));
        Map command = surface(update);
        assertEquals("live-update", command.get("operation"));
        assertEquals("act-1", command.get("id"));
        Map state = Json.parseObject((String) command.get("state"));
        assertEquals("On the way", state.get("status"));
        assertEquals("push-cph-driver", state.get("driver"));
        assertEquals("Green Fiat 500", state.get("car"));
        assertEquals("T push-cph-driver", state.get("plate"));
        assertEquals(0.25d, ((Number) state.get("progress")).doubleValue(), 0.001d);
        assertTrue(((String) state.get("eta")).endsWith(" min"), state.toString());

        // A change of state moves it on at once, with the notification.
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/arrived"), driver))
                .andExpect(status().isOk());
        command = surface(last(sent.sentTo("cph-rider-phone")));
        assertEquals("live-update", command.get("operation"));
        assertEquals(0.5d, progress(command), 0.001d);
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/start"), driver))
                .andExpect(status().isOk());
        assertEquals(0.75d, progress(surface(last(sent.sentTo("cph-rider-phone")))), 0.001d);
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/complete"), driver))
                .andExpect(status().isOk());
        RecordingPushSender.Sent done = last(sent.sentTo("cph-rider-phone"));
        assertEquals("You've arrived", done.message().get("title"));
        assertEquals("live-end", surface(done).get("operation"));
        assertEquals("act-1", surface(done).get("id"));
        // And it is forgotten: the car goes on reporting, the rider hears no more.
        int after = sent.sentTo("cph-rider-phone").size();
        Trips.online(mvc, driver, 55.6800, 12.5700);
        assertEquals(after, sent.sentTo("cph-rider-phone").size());
    }

    @Test
    void theSettingsDecideWhatIsSaidButNotWhatIsShown() throws Exception {
        // Sofia: a rider who turned ride updates off hears nothing of the
        // driver, while the lock screen still lets go of the cancelled ride.
        String rider = People.rider(accounts, "push-sof-rider");
        String driver = People.driver(accounts, "push-sof-driver");
        String quiet = "{\"language\":\"en\",\"theme\":\"system\",\"units\":\"km\","
                + "\"notifyRideUpdates\":false,\"notifyReceipts\":true,"
                + "\"notifyPromotions\":false,\"driverGender\":\"\",\"quietRide\":false,"
                + "\"accessibleVehicle\":false,\"petFriendly\":false,"
                + "\"defaultPaymentMethodId\":\"cash\",\"defaultTipPercent\":0,"
                + "\"shareTripsWith\":\"\"}";
        mvc.perform(People.asRider(body(put("/api/account/preferences"), quiet), rider))
                .andExpect(status().isOk());
        register(rider, "apns", "sof-rider-phone");
        Trips.online(mvc, driver, 42.6977, 23.3219);
        String id = (String) Trips.json(mvc, People.asRider(body(post("/api/rides"),
                People.ride(42.6978, 23.3220, 42.7054, 23.3112)), rider)).get("id");
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asRider(body(post("/api/rides/" + id + "/activity"),
                "{\"activityId\":\"act-sof\"}"), rider)).andExpect(status().isOk());
        assertEquals("", titles("sof-rider-phone"));
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/cancel"), driver))
                .andExpect(status().isOk());
        List<RecordingPushSender.Sent> toRider = sent.sentTo("sof-rider-phone");
        assertEquals(1, toRider.size());
        assertEquals(Boolean.TRUE, toRider.get(0).message().get("silent"));
        assertEquals("live-end", surface(toRider.get(0)).get("operation"));
    }

    @Test
    void aPhoneBelongsToWhoeverSignedInOnItLast() throws Exception {
        String first = People.rider(accounts, "push-ams-first");
        String second = People.rider(accounts, "push-ams-second");
        String driver = People.driver(accounts, "push-ams-driver");
        register(first, "fcm", "ams-shared-phone");
        register(second, "fcm", "ams-shared-phone");
        register(first, "fcm", "ams-first-tablet");
        Trips.online(mvc, driver, 52.3676, 4.9041);
        String id = (String) Trips.json(mvc, People.asRider(body(post("/api/rides"),
                People.ride(52.3677, 4.9040, 52.3800, 4.9100)), first)).get("id");
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), driver))
                .andExpect(status().isOk());
        assertEquals("", titles("ams-shared-phone"));
        assertEquals("Your driver is on the way", titles("ams-first-tablet"));

        // Signing out forgets the device, and only one's own.
        mvc.perform(People.asRider(body(post("/api/account/push/remove"),
                device("fcm", "ams-shared-phone")), first)).andExpect(status().isOk());
        mvc.perform(People.asRider(body(post("/api/account/push/remove"),
                device("fcm", "ams-first-tablet")), first)).andExpect(status().isOk());
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/arrived"), driver))
                .andExpect(status().isOk());
        assertEquals("Your driver is on the way", titles("ams-first-tablet"));

        // Closing the account forgets every device it had.
        String rider = People.rider(accounts, "push-ams-closing");
        register(rider, "apns", "ams-closing-phone");
        accounts.close(rider, People.PASSWORD);
        assertEquals(0L, devices(rider));
        assertEquals(1L, devices(second));
    }

    @Test
    void anActivityIsRefusedOnceTheRideHasEnded() throws Exception {
        // Helsinki: the ride is completed. Oslo: the driver cancels it.
        String rider = People.rider(accounts, "push-hel-rider");
        String driver = People.driver(accounts, "push-hel-driver");
        Trips.online(mvc, driver, 60.1699, 24.9384);
        String done = (String) Trips.json(mvc, People.asRider(body(post("/api/rides"),
                People.ride(60.1700, 24.9383, 60.1800, 24.9500)), rider)).get("id");
        mvc.perform(People.asDriver(post("/api/driver/offers/" + done + "/accept"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asDriver(post("/api/driver/rides/" + done + "/arrived"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asDriver(post("/api/driver/rides/" + done + "/start"), driver))
                .andExpect(status().isOk());
        // Still on the road: the activity is taken.
        mvc.perform(People.asRider(body(post("/api/rides/" + done + "/activity"),
                "{\"activityId\":\"act-hel\"}"), rider)).andExpect(status().isOk());
        mvc.perform(People.asDriver(post("/api/driver/rides/" + done + "/complete"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asRider(body(post("/api/rides/" + done + "/activity"),
                "{\"activityId\":\"act-hel-2\"}"), rider)).andExpect(status().isConflict());

        String oslo = People.rider(accounts, "push-osl-rider");
        String oslodriver = People.driver(accounts, "push-osl-driver");
        Trips.online(mvc, oslodriver, 59.9139, 10.7522);
        String canceled = (String) Trips.json(mvc, People.asRider(body(post("/api/rides"),
                People.ride(59.9140, 10.7521, 59.9250, 10.7600)), oslo)).get("id");
        mvc.perform(People.asDriver(post("/api/driver/offers/" + canceled + "/accept"),
                oslodriver)).andExpect(status().isOk());
        mvc.perform(People.asDriver(post("/api/driver/rides/" + canceled + "/cancel"),
                oslodriver)).andExpect(status().isOk());
        mvc.perform(People.asRider(body(post("/api/rides/" + canceled + "/activity"),
                "{\"activityId\":\"act-osl\"}"), oslo)).andExpect(status().isConflict());
    }

    /// An end the push provider refused is not lost with the activity: the
    /// ride's transition is never retried, so the sweep sends it again.
    @Test
    void anEndTheProviderRefusedIsSentAgain() throws Exception {
        String rider = People.rider(accounts, "push-rey-rider");
        String driver = People.driver(accounts, "push-rey-driver");
        register(rider, "apns", "rey-rider-phone");
        Trips.online(mvc, driver, 64.1466, -21.9426);
        String id = (String) Trips.json(mvc, People.asRider(body(post("/api/rides"),
                People.ride(64.1467, -21.9425, 64.1500, -21.9000)), rider)).get("id");
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asRider(body(post("/api/rides/" + id + "/activity"),
                "{\"activityId\":\"act-rey\"}"), rider)).andExpect(status().isOk());

        sent.refuse(1);
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/cancel"), driver))
                .andExpect(status().isOk());
        assertEquals(1L, activities(id), "the refused end keeps its activity");
        int before = sent.sentTo("rey-rider-phone").size();

        // Not while the call that ended the ride may still be sending it...
        pushes.endLeftBehind();
        assertEquals(before, sent.sentTo("rey-rider-phone").size());
        // ...and once that is past, once.
        db.execute("UPDATE ride_activity SET ended_at = ? WHERE ride_id = ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 120000L), id});
        pushes.endLeftBehind();
        List<RecordingPushSender.Sent> toRider = sent.sentTo("rey-rider-phone");
        assertEquals(before + 1, toRider.size());
        Map command = surface(last(toRider));
        assertEquals("live-end", command.get("operation"));
        assertEquals("act-rey", command.get("id"));
        assertEquals(0L, activities(id));
        pushes.endLeftBehind();
        assertEquals(before + 1, sent.sentTo("rey-rider-phone").size());
    }

    /// A send that returned is not a send the service took: the real sender
    /// hands the message to another thread and learns later, and only then is
    /// the activity forgotten.
    @Test
    void anEndTheServiceNeverConfirmedIsKept() throws Exception {
        String rider = People.rider(accounts, "push-tll-rider");
        String driver = People.driver(accounts, "push-tll-driver");
        register(rider, "apns", "tll-rider-phone");
        Trips.online(mvc, driver, 59.4370, 24.7536);
        String id = (String) Trips.json(mvc, People.asRider(body(post("/api/rides"),
                People.ride(59.4371, 24.7535, 59.4400, 24.7700)), rider)).get("id");
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asRider(body(post("/api/rides/" + id + "/activity"),
                "{\"activityId\":\"act-tll\"}"), rider)).andExpect(status().isOk());

        sent.leaveUnconfirmed(1);
        mvc.perform(People.asRider(post("/api/rides/" + id + "/cancel"), rider))
                .andExpect(status().isOk());
        assertEquals("live-end", surface(last(sent.sentTo("tll-rider-phone"))).get("operation"));
        assertEquals(1L, activities(id), "sent, but never confirmed");

        db.execute("UPDATE ride_activity SET ended_at = ? WHERE ride_id = ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 120000L), id});
        pushes.endLeftBehind();
        assertEquals(0L, activities(id));
    }

    /// An end still undelivered when iOS would have ended the activity itself
    /// is dropped, not sent.
    @Test
    void anEndTooOldToBeOnScreenIsDropped() throws Exception {
        db.execute("INSERT INTO ride_activity (ride_id, username, activity_id, sent_at, ended_at) "
                + "VALUES ('old-ride', 'push-old-rider', 'act-old', 0, 1)", new Object[0]);
        int before = sent.all().size();
        pushes.endLeftBehind();
        assertEquals(0L, activities("old-ride"));
        assertEquals(before, sent.all().size());
    }

    @Test
    void aTokenTakenFromAnotherAccountStillRespectsTheCap() throws Exception {
        String full = People.rider(accounts, "push-cap-full");
        String other = People.rider(accounts, "push-cap-other");
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
        String rider = People.rider(accounts, "push-gone-rider");
        accounts.close(rider, People.PASSWORD);
        try {
            pushDevices.register(rider, new PushDeviceDto("fcm", "gone-rider-phone", "and"));
            fail("a device was registered for a closed account");
        } catch (ResponseStatusException refused) {
            assertEquals(404, refused.getStatus());
        }
        assertEquals(0L, tokens("gone-rider-phone"));
        assertEquals(0L, devices(rider));
    }

    /// Registrations one after another for a full account each make their
    /// own room, oldest first. Taken together, the registrations wait for one
    /// another behind the account's profile row, so this is also what two of
    /// them sent at once come to.
    @Test
    void eachNewDeviceOfAFullAccountReplacesTheOldest() throws Exception {
        String full = People.rider(accounts, "push-cap2-full");
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

    /// The notification of a change that is no longer the ride's latest says
    /// nothing, in either order: composed after the ride was cancelled it does
    /// not tell the rider a driver is coming, and composed before the
    /// cancellation's own it leaves the Live Activity for that one to end.
    @Test
    void aStaleChangeSaysNothingAndLeavesTheActivityToTheLaterOne() throws Exception {
        // Valletta: the acceptance is composed after the cancellation was.
        String rider = People.rider(accounts, "push-mla-rider");
        String driver = People.driver(accounts, "push-mla-driver");
        register(rider, "apns", "mla-rider-phone");
        Trips.online(mvc, driver, 35.8989, 14.5146);
        String id = (String) Trips.json(mvc, People.asRider(body(post("/api/rides"),
                People.ride(35.8990, 14.5145, 35.9100, 14.5000)), rider)).get("id");
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asRider(body(post("/api/rides/" + id + "/activity"),
                "{\"activityId\":\"act-mla\"}"), rider)).andExpect(status().isOk());
        mvc.perform(People.asRider(post("/api/rides/" + id + "/cancel"), rider))
                .andExpect(status().isOk());
        int told = sent.sentTo("mla-rider-phone").size();
        assertEquals("live-end", surface(last(sent.sentTo("mla-rider-phone")))
                .get("operation"));
        pushes.rideChanged(id, RideState.ACCEPTED, rider, driver);
        assertEquals(told, sent.sentTo("mla-rider-phone").size());

        // Gdansk: the acceptance is composed after the ride was cancelled but
        // before the cancellation is. It neither speaks nor forgets the
        // activity, and the cancellation then ends it.
        String gdnRider = People.rider(accounts, "push-gdn-rider");
        String gdnDriver = People.driver(accounts, "push-gdn-driver");
        register(gdnRider, "apns", "gdn-rider-phone");
        Trips.online(mvc, gdnDriver, 54.3520, 18.6466);
        String gdn = (String) Trips.json(mvc, People.asRider(body(post("/api/rides"),
                People.ride(54.3521, 18.6465, 54.3650, 18.6300)), gdnRider)).get("id");
        mvc.perform(People.asDriver(post("/api/driver/offers/" + gdn + "/accept"), gdnDriver))
                .andExpect(status().isOk());
        mvc.perform(People.asRider(body(post("/api/rides/" + gdn + "/activity"),
                "{\"activityId\":\"act-gdn\"}"), gdnRider)).andExpect(status().isOk());
        int before = sent.sentTo("gdn-rider-phone").size();
        db.execute("UPDATE ride SET state = 'CANCELLED_BY_RIDER' WHERE id = ?",
                new Object[] {gdn});
        pushes.rideChanged(gdn, RideState.ACCEPTED, gdnRider, gdnDriver);
        assertEquals(before, sent.sentTo("gdn-rider-phone").size());
        assertEquals(1L, activities(gdn));
        pushes.rideChanged(gdn, RideState.CANCELLED_BY_RIDER, gdnRider, gdnDriver);
        RecordingPushSender.Sent ended = last(sent.sentTo("gdn-rider-phone"));
        assertEquals("live-end", surface(ended).get("operation"));
        assertEquals("act-gdn", surface(ended).get("id"));
        assertEquals(0L, activities(gdn));
    }

    private long activities(String rideId) throws Exception {
        Map row = db.queryOne("SELECT COUNT(*) AS n FROM ride_activity WHERE ride_id = ?",
                new Object[] {rideId});
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
