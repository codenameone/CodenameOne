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
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.account.DemoAccounts;
import com.codenameone.examples.savor.pay.Payments;
import com.codenameone.examples.savor.delivery.Deliveries;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Paying: the cards an account keeps, the charge when a delivery completes, the
/// tip after it, the refund an admin can make, and what all of it earns the
/// courier. Against the simulated processor, which is what a server with no
/// key configured runs.
@BackendTest
class PaymentsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Deliveries deliveries;
    @Autowired
    private Payments payments;
    @Autowired
    private DataSource db;

    private MockRequestBuilder admin(MockRequestBuilder request) {
        return People.asAdmin(request, DemoAccounts.ADMIN);
    }

    private long setupsOf(String username) throws Exception {
        Map row = db.queryOne("SELECT COUNT(*) AS n FROM payment_setup WHERE username = ?",
                new Object[] {username});
        return ((Number) row.get("n")).longValue();
    }

    /// Sets what the delivery `id` and its order say of their payment, as a
    /// write the server lost would leave them.
    private void paymentStatus(String id, String status) throws Exception {
        db.execute("UPDATE delivery SET payment_status = ? WHERE id = ?", new Object[] {status, id});
        db.execute("UPDATE customer_order SET payment_status = ? WHERE id = ?",
                new Object[] {status, id});
    }

    @Test
    void openSetupsAreCappedAndTheFinishedAndAbandonedAreSwept() throws Exception {
        String customer = People.customer(accounts, "setup-looper");
        // Five open at once is as many as an account may have; each is a
        // session made at the processor.
        String first = null;
        for (int iter = 0; iter < 5; iter++) {
            Map setup = Trips.json(mvc, People.asCustomer(post("/api/payments/methods/setup"),
                    customer));
            first = first == null ? (String) setup.get("id") : first;
        }
        mvc.perform(People.asCustomer(post("/api/payments/methods/setup"), customer))
                .andExpect(status().is(429));
        // Finishing one makes room for another.
        mvc.perform(People.asCustomer(Trips.body(post("/api/payments/methods/setup/" + first
                        + "/complete"), Trips.card(Trips.VISA)), customer))
                .andExpect(status().isOk());
        String last = (String) Trips.json(mvc, People.asCustomer(
                post("/api/payments/methods/setup"), customer)).get("id");
        assertEquals(6L, setupsOf(customer));
        // So does an hour going by: abandoned setups do not count.
        db.execute("UPDATE payment_setup SET created_at = ? WHERE username = ? AND id <> ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 90L * 60L * 1000L),
                    customer, last});
        Trips.json(mvc, People.asCustomer(post("/api/payments/methods/setup"), customer));

        // The sweep deletes the finished one, and an abandoned one once it is
        // two hours old; younger ones, and the open ones, stay.
        payments.sweepSetups();
        assertEquals(6L, setupsOf(customer));
        db.execute("UPDATE payment_setup SET created_at = ? WHERE username = ? AND created_at < ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 3L * 60L * 60L * 1000L),
                    customer, Long.valueOf(System.currentTimeMillis() - 60L * 60L * 1000L)});
        payments.sweepSetups();
        assertEquals(2L, setupsOf(customer));
        mvc.perform(People.asCustomer(Trips.body(post("/api/payments/methods/setup/" + last
                        + "/complete"), Trips.card(Trips.MASTERCARD)), customer))
                .andExpect(status().isOk());
    }

    @Test
    void todaysTripsCountTodaysDeliveriesAlone() throws Exception {
        // Tallinn.
        String customer = People.customer(accounts, "tll-pay-customer");
        String courier = People.courier(accounts, "tll-pay-courier");
        Trips.online(mvc, courier, 59.4370, 24.7536);
        String yesterday = (String) Trips.complete(mvc, customer, courier, Trips.trip(59.4370,
                24.7536, 59.4470, 24.7636)).get("id");
        Trips.complete(mvc, customer, courier, Trips.trip(59.4370, 24.7536, 59.4470, 24.7636));
        long now = System.currentTimeMillis();
        db.execute("UPDATE delivery SET updated_at = ? WHERE id = ?", new Object[] {
            Long.valueOf(now - now % (24L * 60L * 60L * 1000L) - 60L * 60L * 1000L), yesterday});
        Map earned = Trips.json(mvc, People.asCourier(get("/api/courier/earnings"), courier));
        // Both are trips the courier made; one of them was today's, and the
        // header's count is of the deliveries its amount is made of.
        assertEquals(2L, Trips.number(earned, "deliveries"));
        assertEquals(1L, Trips.number(earned, "todayDeliveries"));
        Trips.offline(mvc, courier);
    }

    @Test
    void cardsAreSavedChosenAndRemoved() throws Exception {
        String customer = People.customer(accounts, "card-holder");
        String other = People.customer(accounts, "card-other");
        mvc.perform(People.asCustomer(get("/api/payments/config"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("simulated"))
                .andExpect(jsonPath("$.currency").value("USD"));
        // Cash, always, and the default until there is a card.
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("cash"))
                .andExpect(jsonPath("$[0].kind").value("cash"))
                .andExpect(jsonPath("$[0].isDefault").value(true));

        // A card that is refused is not saved, and says why; the same setup
        // then takes a card that is good.
        Map setup = Trips.json(mvc, People.asCustomer(post("/api/payments/methods/setup"), customer));
        assertEquals(Boolean.FALSE, setup.get("hosted"));
        String complete = "/api/payments/methods/setup/" + setup.get("id") + "/complete";
        mvc.perform(People.asCustomer(Trips.body(post(complete), Trips.card(Trips.DECLINED)), customer))
                .andExpect(status().is(402));
        mvc.perform(People.asCustomer(Trips.body(post(complete), Trips.card("4242424242424241")),
                        customer))
                .andExpect(status().is(402));
        // Somebody else cannot finish it for them.
        mvc.perform(People.asCustomer(Trips.body(post(complete), Trips.card(Trips.VISA)), other))
                .andExpect(status().isNotFound());
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(1));
        Map visa = Trips.json(mvc, People.asCustomer(Trips.body(post(complete),
                Trips.card(Trips.VISA)), customer));
        assertEquals("card", visa.get("kind"));
        assertEquals("Visa", visa.get("brand"));
        assertEquals("4242", visa.get("last4"));
        assertEquals("Visa 4242", visa.get("label"));
        assertEquals(12L, Trips.number(visa, "expMonth"));
        // The first card saved is the one to pay with.
        assertEquals(Boolean.TRUE, visa.get("isDefault"));
        // A setup is used once.
        mvc.perform(People.asCustomer(Trips.body(post(complete), Trips.card(Trips.VISA)), customer))
                .andExpect(status().isNotFound());

        Map second = Trips.addCard(mvc, customer, Trips.MASTERCARD);
        assertEquals("Mastercard", second.get("brand"));
        assertEquals(Boolean.FALSE, second.get("isDefault"));
        String visaId = (String) visa.get("id");
        String secondId = (String) second.get("id");
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + secondId + "/default"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(visaId))
                .andExpect(jsonPath("$[0].isDefault").value(false))
                .andExpect(jsonPath("$[1].isDefault").value(true))
                .andExpect(jsonPath("$[2].id").value("cash"))
                .andExpect(jsonPath("$[2].isDefault").value(false));

        // A card is its owner's: nobody else sees it, chooses it or removes it.
        mvc.perform(People.asCustomer(get("/api/payments/methods"), other))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + visaId + "/default"), other))
                .andExpect(status().isNotFound());
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + visaId + "/remove"), other))
                .andExpect(status().isNotFound());
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders"), Trips.cart(mvc, Trips.trip(52.52,
                        13.405, 52.53, 13.42, "\"paymentMethodId\":\"" + visaId + "\""))), other))
                .andExpect(status().isBadRequest());

        // Removing the default leaves cash as the default; cash stays.
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + secondId + "/remove"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secondId));
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].id").value("cash"))
                .andExpect(jsonPath("$[1].isDefault").value(true));
        mvc.perform(People.asCustomer(post("/api/payments/methods/cash/remove"), customer))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asCustomer(post("/api/payments/methods/cash/default"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void aDeliveryIsChargedTippedAndRefundedAndTheCourierIsPaid() throws Exception {
        // Berlin.
        String customer = People.customer(accounts, "ber-customer");
        String courier = People.courier(accounts, "ber-courier");
        String stranger = People.customer(accounts, "ber-stranger");
        Trips.addCard(mvc, customer, Trips.VISA);
        Trips.online(mvc, courier, 52.5200, 13.4050);

        // No method named: the default, which is now the card.
        Map done = Trips.complete(mvc, customer, courier, Trips.trip(52.5200, 13.4050, 52.5300,
                13.4200));
        String id = (String) done.get("id");
        // The courier is shown the delivery fee, which is theirs; the
        // customer paid the order's total, held at checkout and taken now.
        long fare = Trips.number(done, "fareCents");
        Map order = Trips.json(mvc, People.asCustomer(get("/api/orders/" + id), customer));
        long total = Trips.number(order, "totalCents");
        assertEquals(fare, Trips.number(order, "deliveryFeeCents"));
        assertEquals("PAID", done.get("paymentStatus"));
        assertEquals("PAID", order.get("paymentStatus"));
        assertEquals("Visa 4242", done.get("paymentMethodLabel"));
        assertEquals(Boolean.FALSE, done.get("cash"));
        assertEquals("B", done.get("courierPhotoInitials"));

        // The receipt adds up, part by part.
        Map receipt = Trips.receipt(mvc, customer, id);
        assertEquals("paid", receipt.get("status"));
        assertEquals("Visa 4242", receipt.get("methodLabel"));
        assertEquals(Trips.DISH_CENTS, Trips.number(receipt, "subtotal"));
        assertEquals(fare, Trips.number(receipt, "deliveryFee"));
        assertEquals(0L, Trips.number(receipt, "tip"));
        assertEquals(total, Trips.number(receipt, "subtotal") + Trips.number(receipt, "deliveryFee")
                + Trips.number(receipt, "serviceFee") + Trips.number(receipt, "smallOrderFee")
                + Trips.number(receipt, "tax") - Trips.number(receipt, "discount"));
        assertEquals(total, Trips.number(receipt, "total"));
        assertTrue(Trips.number(receipt, "paidAt") > 0L);
        assertEquals("There", receipt.get("dropoffName"));
        assertEquals("ber-courier", receipt.get("courierName"));
        // It is the customer's, and nobody else's -- the courier's included.
        mvc.perform(People.asCustomer(get("/api/payments/receipts/" + id), stranger))
                .andExpect(status().isNotFound());
        mvc.perform(People.asCourier(get("/api/payments/receipts/" + id), courier))
                .andExpect(status().isNotFound());
        mvc.perform(People.asCustomer(get("/api/payments/receipts"), stranger))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(People.asCustomer(get("/api/payments/receipts"), customer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(id));

        // A tip, once, by the customer.
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders/" + id + "/tip"),
                        "{\"amount\":300}"), stranger))
                .andExpect(status().isNotFound());
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders/" + id + "/tip"),
                        "{\"amount\":0}"), customer))
                .andExpect(status().isBadRequest());
        Map tipped = Trips.json(mvc, People.asCustomer(Trips.body(post("/api/orders/" + id + "/tip"),
                "{\"amount\":300}"), customer));
        assertEquals(300L, Trips.number(tipped, "tip"));
        assertEquals(total + 300L, Trips.number(tipped, "total"));
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders/" + id + "/tip"),
                        "{\"amount\":300}"), customer))
                .andExpect(status().isConflict());
        mvc.perform(People.asCustomer(get("/api/orders/" + id), customer))
                .andExpect(jsonPath("$.delivery.tip").value(300));

        // The courier earns the delivery fee and all the tip: the service's
        // commission is taken from the kitchen's side of the order.
        long earned = fare + 300L;
        Map summary = Trips.json(mvc, People.asCourier(get("/api/courier/earnings/summary"),
                courier));
        assertEquals("USD", summary.get("currency"));
        assertEquals(earned, Trips.number(summary, "today"));
        assertEquals(earned, Trips.number(summary, "week"));
        assertEquals(earned, Trips.number(summary, "month"));
        assertEquals(earned, Trips.number(summary, "balance"));
        assertEquals(1L, Trips.number(summary, "trips"));
        assertEquals(300L, Trips.number(summary, "tips"));
        assertEquals(1d, ((Number) summary.get("acceptanceRate")).doubleValue());
        List daily = (List) summary.get("daily");
        assertEquals(14, daily.size());
        Map today = (Map) daily.get(13);
        assertEquals(Days.iso(System.currentTimeMillis()), today.get("date"));
        assertEquals(earned, Trips.number(today, "amount"));
        assertEquals(1L, Trips.number(today, "trips"));
        assertEquals(0L, Trips.number((Map) daily.get(0), "amount"));
        // The older call reports the same share, without the tip.
        mvc.perform(People.asCourier(get("/api/courier/earnings"), courier))
                .andExpect(jsonPath("$.totalCents").value((int) (earned - 300L)));

        // Nor can an account that is owed money be closed: nobody could cash
        // it out afterwards.
        mvc.perform(People.asCourier(Trips.body(post("/api/account/delete"),
                        "{\"password\":\"" + People.PASSWORD + "\"}"), courier))
                .andExpect(status().isConflict());

        // Cashing out needs somewhere to send it, and empties the balance.
        mvc.perform(People.asCourier(get("/api/courier/payouts"), courier))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(People.asCourier(post("/api/courier/payouts"), courier))
                .andExpect(status().isConflict());
        mvc.perform(People.asCourier(get("/api/courier/payout-account"), courier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountLast4").value(""));
        mvc.perform(People.asCourier(Trips.body(put("/api/courier/payout-account"),
                        "{\"holder\":\"B Courier\",\"bankName\":\"Spree Bank\","
                                + "\"accountLast4\":\"12a4\"}"), courier))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asCourier(Trips.body(put("/api/courier/payout-account"),
                        "{\"holder\":\"B Courier\",\"bankName\":\"Spree Bank\","
                                + "\"accountLast4\":\"1234\"}"), courier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankName").value("Spree Bank"));
        Map payout = Trips.json(mvc, People.asCourier(post("/api/courier/payouts"), courier));
        assertEquals(earned, Trips.number(payout, "amount"));
        assertEquals("paid", payout.get("status"));
        assertEquals("Spree Bank ****1234", payout.get("destination"));
        mvc.perform(People.asCourier(post("/api/courier/payouts"), courier))
                .andExpect(status().isConflict());
        mvc.perform(People.asCourier(get("/api/courier/payouts"), courier))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((String) payout.get("id")));
        summary = Trips.json(mvc, People.asCourier(get("/api/courier/earnings/summary"), courier));
        assertEquals(0L, Trips.number(summary, "balance"));
        assertEquals(earned, Trips.number(summary, "today"));

        // An admin sees every payment, and returns this one, once, saying why.
        List all = Trips.list(mvc, admin(get("/api/admin/payments")));
        boolean listed = false;
        for (int iter = 0; iter < all.size(); iter++) {
            Map row = (Map) all.get(iter);
            if (id.equals(row.get("orderId"))) {
                listed = true;
                assertEquals(customer, row.get("customerUsername"));
            }
        }
        assertTrue(listed, "the admin's list has the order");
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + id + "/refund"), "{}")))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + id + "/refund"),
                        "{\"reason\":\"It arrived cold\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("refunded"));
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + id + "/refund"),
                        "{\"reason\":\"Twice\"}")))
                .andExpect(status().isConflict());
        assertEquals("refunded", Trips.receipt(mvc, customer, id).get("status"));
        mvc.perform(People.asCustomer(get("/api/orders/" + id), customer))
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));

        // The refund took the fare and the tip back, so neither is earned any
        // more: not in the day's totals, and not in the balance, which now owes
        // what was cashed out. The trip was still driven.
        summary = Trips.json(mvc, People.asCourier(get("/api/courier/earnings/summary"), courier));
        assertEquals(0L, Trips.number(summary, "today"));
        assertEquals(0L, Trips.number(summary, "month"));
        assertEquals(0L, Trips.number((Map) ((List) summary.get("daily")).get(13), "amount"));
        assertEquals(1L, Trips.number((Map) ((List) summary.get("daily")).get(13), "trips"));
        assertEquals(0L, Trips.number(summary, "tips"));
        assertEquals(-earned, Trips.number(summary, "balance"));
        mvc.perform(People.asCourier(post("/api/courier/payouts"), courier))
                .andExpect(status().isConflict());
        Trips.offline(mvc, courier);
    }

    /// A card tip whose charge nobody can vouch for is retried with the
    /// delivery's card, so that card stays, and so does its account, until the
    /// tip is settled one way or the other.
    @Test
    void aCardWithATipNobodyCanVouchForStays() throws Exception {
        // Bergen.
        String customer = People.customer(accounts, "bgo-tip-customer");
        String courier = People.courier(accounts, "bgo-tip-courier");
        String card = (String) Trips.addCard(mvc, customer, Trips.VISA).get("id");
        Trips.online(mvc, courier, 60.3913, 5.3221);
        String id = (String) Trips.complete(mvc, customer, courier, Trips.trip(60.3913, 5.3221,
                60.4013, 5.3321)).get("id");
        Trips.offline(mvc, courier);
        db.execute("UPDATE delivery SET tip_cents = 300 WHERE id = ?", new Object[] {id});
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + card + "/remove"), customer))
                .andExpect(status().isConflict());
        mvc.perform(People.asCustomer(Trips.body(post("/api/account/delete"),
                        "{\"password\":\"" + People.PASSWORD + "\"}"), customer))
                .andExpect(status().isConflict());

        // The charge came back: nothing is left to ask about.
        db.execute("UPDATE payment SET tip_ref = 'sim_ch_tip' WHERE delivery_id = ?",
                new Object[] {id});
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + card + "/remove"), customer))
                .andExpect(status().isOk());
    }

    @Test
    void aDeliveryIsNotRefundedWhileItsTipIsBeingCharged() throws Exception {
        // Oslo.
        String customer = People.customer(accounts, "osl-customer");
        String courier = People.courier(accounts, "osl-courier");
        Trips.addCard(mvc, customer, Trips.VISA);
        Trips.online(mvc, courier, 59.9139, 10.7522);
        String id = (String) Trips.complete(mvc, customer, courier, Trips.trip(59.9139, 10.7522,
                59.9239, 10.7622)).get("id");

        // A tip as it stands between being claimed and its charge coming back:
        // on the delivery, and with no charge recorded for it.
        db.execute("UPDATE delivery SET tip_cents = 300 WHERE id = ?", new Object[] {id});
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + id + "/refund"),
                        "{\"reason\":\"Too soon\"}")))
                .andExpect(status().isConflict());
        // Refused whole: the delivery is still paid, so the refund can be asked
        // for again and the tip's charge has a paid delivery to land on.
        mvc.perform(People.asCustomer(get("/api/orders/" + id), customer))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        // Nor is it the courier's to cash out: nobody knows it was charged.
        Map before = Trips.json(mvc, People.asCourier(get("/api/courier/earnings/summary"),
                courier));
        assertEquals(0L, Trips.number(before, "tips"));
        long fareOnly = Trips.number(before, "balance");
        // Nor earned today: the day's total is what the balance grew by.
        assertEquals(fareOnly, Trips.number(before, "today"));
        assertEquals(fareOnly, Trips.number(before, "month"));

        // The charge came back.
        db.execute("UPDATE payment SET tip_ref = 'sim_ch_tip' WHERE delivery_id = ?",
                new Object[] {id});
        Map after = Trips.json(mvc, People.asCourier(get("/api/courier/earnings/summary"),
                courier));
        assertEquals(300L, Trips.number(after, "tips"));
        assertEquals(fareOnly + 300L, Trips.number(after, "balance"));
        assertEquals(fareOnly + 300L, Trips.number(after, "today"));
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + id + "/refund"),
                        "{\"reason\":\"Now\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("refunded"));
        Trips.offline(mvc, courier);
    }

    @Test
    void theAdminSeesWhatTheCourierEarnedAndNoMore() throws Exception {
        // Vilnius.
        String customer = People.customer(accounts, "vno-customer");
        String courier = People.courier(accounts, "vno-courier");
        Trips.addCard(mvc, customer, Trips.VISA);
        Trips.online(mvc, courier, 54.6872, 25.2797);
        String paid = (String) Trips.complete(mvc, customer, courier, Trips.trip(54.6872, 25.2797,
                54.6972, 25.2897)).get("id");
        String pending = (String) Trips.complete(mvc, customer, courier, Trips.trip(54.6872,
                25.2797, 54.6972, 25.2897)).get("id");
        String refunded = (String) Trips.complete(mvc, customer, courier, Trips.trip(54.6872,
                25.2797, 54.6972, 25.2897)).get("id");
        // A charge nobody has answered yet, a delivery refunded, and a tip on the
        // paid delivery whose charge has not come back.
        paymentStatus(pending, "PENDING");
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + refunded + "/refund"),
                        "{\"reason\":\"Wrong address\"}")))
                .andExpect(status().isOk());
        db.execute("UPDATE delivery SET tip_cents = 300 WHERE id = ?", new Object[] {paid});

        long share = ((Number) db.queryOne("SELECT courier_cents FROM delivery WHERE id = ?",
                new Object[] {paid}).get("courier_cents")).longValue();
        Map summary = Trips.json(mvc, People.asCourier(get("/api/courier/earnings/summary"),
                courier));
        Map detail = Trips.json(mvc, admin(get("/api/admin/users/" + courier)));
        // The paid delivery's share alone, on both screens.
        assertEquals(share, Trips.number(summary, "balance"));
        assertEquals(share, Trips.number(summary, "month"));
        assertEquals(share, Trips.number(detail, "earned"));

        // Once the tip's charge is on record it is earned on both.
        db.execute("UPDATE payment SET tip_ref = 'sim_ch_tip' WHERE delivery_id = ?",
                new Object[] {paid});
        summary = Trips.json(mvc, People.asCourier(get("/api/courier/earnings/summary"), courier));
        detail = Trips.json(mvc, admin(get("/api/admin/users/" + courier)));
        assertEquals(share + 300L, Trips.number(summary, "balance"));
        assertEquals(share + 300L, Trips.number(detail, "earned"));
        Trips.offline(mvc, courier);
    }

    @Test
    void aRefundTheServerNeverRecordedIsAskedForAgain() throws Exception {
        // Bergen.
        String customer = People.customer(accounts, "bgo-customer");
        String courier = People.courier(accounts, "bgo-courier");
        Trips.addCard(mvc, customer, Trips.VISA);
        Trips.online(mvc, courier, 60.3913, 5.3221);
        String id = (String) Trips.complete(mvc, customer, courier, Trips.trip(60.3913, 5.3221,
                60.4013, 5.3321)).get("id");

        // A refund as a server that stopped half way leaves it: the delivery
        // marked, the processor perhaps asked, and nothing written down.
        paymentStatus(id, "REFUNDED");
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + id + "/refund"),
                        "{\"reason\":\"Again\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("refunded"));
        // And once it is recorded, it is refunded once like any other.
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + id + "/refund"),
                        "{\"reason\":\"Twice\"}")))
                .andExpect(status().isConflict());
        Trips.offline(mvc, courier);
    }

    @Test
    void aRefundTheProcessorHasNotFinishedIsRecordedOnlyOnceItEnds() throws Exception {
        // Gothenburg.
        String customer = People.customer(accounts, "got-customer");
        String courier = People.courier(accounts, "got-courier");
        String card = (String) Trips.addCard(mvc, customer, Trips.SLOW_REFUND).get("id");
        String method = "\"paymentMethodId\":\"" + card + "\"";
        Trips.online(mvc, courier, 57.7089, 11.9746);
        String id = (String) Trips.complete(mvc, customer, courier, Trips.trip(57.7089, 11.9746,
                57.7189, 11.9846, method)).get("id");

        // Taken, and not yet money back: pending, and not to be asked for twice.
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + id + "/refund"),
                        "{\"reason\":\"Slow\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("refund_pending"));
        mvc.perform(People.asCustomer(get("/api/orders/" + id), customer))
                .andExpect(jsonPath("$.paymentStatus").value("REFUND_PENDING"));
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + id + "/refund"),
                        "{\"reason\":\"Again\"}")))
                .andExpect(status().isConflict());
        // The processor has finished it by the time it is asked.
        payments.settleRefunds();
        assertEquals("refunded", Trips.receipt(mvc, customer, id).get("status"));

        // And one the processor then fails is recorded as failed, never as
        // refunded, and is not tried again from here.
        db.execute("UPDATE payment_method SET token = 'sim_pm_lost_card' WHERE id = ?",
                new Object[] {card});
        String lost = (String) Trips.complete(mvc, customer, courier, Trips.trip(57.7089, 11.9746,
                57.7189, 11.9846, method)).get("id");
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + lost + "/refund"),
                        "{\"reason\":\"Lost\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("refund_pending"));
        payments.settleRefunds();
        assertEquals("refund_failed", Trips.receipt(mvc, customer, lost).get("status"));
        mvc.perform(People.asCustomer(get("/api/orders/" + lost), customer))
                .andExpect(jsonPath("$.paymentStatus").value("REFUND_FAILED"));
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + lost + "/refund"),
                        "{\"reason\":\"Again\"}")))
                .andExpect(status().isConflict());
        Trips.offline(mvc, courier);
    }

    @Test
    void aDeliveryLeftCompletedAndUnpaidIsSettledLater() throws Exception {
        // Lisbon.
        String customer = People.customer(accounts, "lis-pay-customer");
        String courier = People.courier(accounts, "lis-pay-courier");
        Trips.addCard(mvc, customer, Trips.VISA);
        Trips.online(mvc, courier, 38.7223, -9.1393);
        Map done = Trips.complete(mvc, customer, courier, Trips.trip(38.7223, -9.1393, 38.7323,
                -9.1293));
        String id = (String) done.get("id");
        long total = Trips.number(Trips.json(mvc, People.asCustomer(get("/api/orders/" + id),
                customer)), "totalCents");

        // The server stopped after the delivery completed and before its payment
        // was written.
        db.execute("DELETE FROM payment WHERE delivery_id = ?", new Object[] {id});
        paymentStatus(id, "PENDING");

        // Completed a moment ago, it may still be at the processor: left alone.
        deliveries.settleLeftBehind();
        mvc.perform(People.asCustomer(get("/api/orders/" + id), customer))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        db.execute("UPDATE delivery SET updated_at = ? WHERE id = ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 5L * 60L * 1000L), id});
        deliveries.settleLeftBehind();
        mvc.perform(People.asCustomer(get("/api/orders/" + id), customer))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        Map receipt = Trips.receipt(mvc, customer, id);
        assertEquals("paid", receipt.get("status"));
        assertEquals(total, Trips.number(receipt, "total"));

        // And once: a second pass finds nothing left to settle.
        deliveries.settleLeftBehind();
        assertEquals(total, Trips.number(Trips.receipt(mvc, customer, id), "total"));
        Trips.offline(mvc, courier);
    }

    @Test
    void aChargeNobodyCanVouchForIsLeftPendingAndAskedForAgain() throws Exception {
        // Prague.
        String customer = People.customer(accounts, "prg-pay-customer");
        String courier = People.courier(accounts, "prg-pay-courier");
        String card = (String) Trips.addCard(mvc, customer, Trips.NO_ANSWER_WHEN_CHARGED).get("id");
        Trips.online(mvc, courier, 50.0755, 14.4378);
        Map done = Trips.complete(mvc, customer, courier, Trips.trip(50.0755, 14.4378, 50.0855,
                14.4478, "\"paymentMethodId\":\"" + card + "\""));
        String id = (String) done.get("id");
        long total = Trips.number(Trips.json(mvc, People.asCustomer(get("/api/orders/" + id),
                customer)), "totalCents");

        // The processor failed without saying whether it took the money: not
        // paid, and not failed either.
        assertEquals("COMPLETED", done.get("state"));
        assertEquals("PENDING", done.get("paymentStatus"));
        // Not money yet, so not the courier's to cash out.
        assertEquals(0L, Trips.number(Trips.json(mvc, People.asCourier(
                get("/api/courier/earnings/summary"), courier)), "balance"));
        // The card is what the delivery will be asked about with, so it stays,
        // and so does the account it belongs to.
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + card + "/remove"), customer))
                .andExpect(status().isConflict());
        mvc.perform(People.asCustomer(Trips.body(post("/api/account/delete"),
                        "{\"password\":\"" + People.PASSWORD + "\"}"), customer))
                .andExpect(status().isConflict());
        db.execute("UPDATE delivery SET updated_at = ? WHERE id = ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 5L * 60L * 1000L), id});
        deliveries.settleLeftBehind();
        mvc.perform(People.asCustomer(get("/api/orders/" + id), customer))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        // The processor answers again, and the same delivery is asked about.
        // The order is taken from the hold made at checkout, so the hold is
        // what answers now.
        db.execute("UPDATE customer_order SET authorization_ref = ? WHERE id = ?",
                new Object[] {"sim_ch_" + id, id});
        deliveries.settleLeftBehind();
        mvc.perform(People.asCustomer(get("/api/orders/" + id), customer))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        assertEquals(total, Trips.number(Trips.receipt(mvc, customer, id), "total"));
        // Paid now, and the courier's share is in the balance.
        assertEquals(Trips.number(done, "fareCents"), Trips.number(Trips.json(mvc,
                People.asCourier(get("/api/courier/earnings/summary"), courier)), "balance"));
        // Settled, and the card can go.
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + card + "/remove"), customer))
                .andExpect(status().isOk());
        Trips.offline(mvc, courier);
    }

    @Test
    void aTipNobodyCanVouchForStaysOnTheDeliveryUntilItIsAskedForAgain() throws Exception {
        // Vienna.
        String customer = People.customer(accounts, "vie-pay-customer");
        String courier = People.courier(accounts, "vie-pay-courier");
        String card = (String) Trips.addCard(mvc, customer, Trips.VISA).get("id");
        Trips.online(mvc, courier, 48.2082, 16.3738);
        String id = (String) Trips.complete(mvc, customer, courier, Trips.trip(48.2082, 16.3738,
                48.2182, 16.3838)).get("id");

        // The processor stops answering, and a tip is asked for.
        db.execute("UPDATE payment_method SET token = 'sim_pm_fault_now' WHERE id = ?",
                new Object[] {card});
        assertEquals(502, Trips.statusOf(mvc, People.asCustomer(Trips.body(
                post("/api/orders/" + id + "/tip"), "{\"amount\":300}"), customer)));
        // It may have been charged, so it stays on the delivery, which cannot be
        // refunded around it, and cannot be swapped for another amount.
        assertEquals(300L, Trips.number(Trips.receipt(mvc, customer, id), "tip"));
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + id + "/refund"),
                        "{\"reason\":\"Too soon\"}")))
                .andExpect(status().isConflict());
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders/" + id + "/tip"),
                        "{\"amount\":500}"), customer))
                .andExpect(status().isConflict());

        // Asked for again with the same amount once the processor is back.
        db.execute("UPDATE payment_method SET token = 'sim_pm_recovered' WHERE id = ?",
                new Object[] {card});
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders/" + id + "/tip"),
                        "{\"amount\":300}"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tip").value(300));
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + id + "/refund"),
                        "{\"reason\":\"Now\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("refunded"));
        Trips.offline(mvc, courier);
    }

    @Test
    void theCardLimitHoldsForSetupsStartedWhileThereWasRoom() throws Exception {
        String customer = People.customer(accounts, "card-collector");
        for (int iter = 0; iter < 7; iter++) {
            Trips.addCard(mvc, customer, Trips.VISA);
        }
        // Two setups started with room for one card.
        Map first = Trips.json(mvc, People.asCustomer(post("/api/payments/methods/setup"), customer));
        Map second = Trips.json(mvc, People.asCustomer(post("/api/payments/methods/setup"), customer));
        mvc.perform(People.asCustomer(Trips.body(post("/api/payments/methods/setup/"
                        + first.get("id") + "/complete"), Trips.card(Trips.MASTERCARD)), customer))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(Trips.body(post("/api/payments/methods/setup/"
                        + second.get("id") + "/complete"), Trips.card(Trips.MASTERCARD)), customer))
                .andExpect(status().isConflict());
        // Cash and eight cards.
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(9));
    }

    @Test
    void cashIsRecordedAndACardThatFailsIsReported() throws Exception {
        // Dublin.
        String customer = People.customer(accounts, "dub-customer");
        String courier = People.courier(accounts, "dub-courier");
        String failing = (String) Trips.addCard(mvc, customer, Trips.FAILS_WHEN_CHARGED).get("id");
        Trips.online(mvc, courier, 53.3498, -6.2603);

        // Cash, asked for by name although a card is the default.
        Map cash = Trips.complete(mvc, customer, courier, Trips.trip(53.3498, -6.2603, 53.3600,
                -6.2500, "\"paymentMethodId\":\"cash\",\"note\":\"Blue door\""));
        String cashId = (String) cash.get("id");
        assertEquals("PAID", cash.get("paymentStatus"));
        assertEquals("Cash", cash.get("paymentMethodLabel"));
        assertEquals(Boolean.TRUE, cash.get("cash"));
        assertEquals("Blue door", cash.get("note"));
        assertEquals("cash", Trips.receipt(mvc, customer, cashId).get("status"));
        // Nothing went to a card, so there is nothing to send back to one.
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + cashId + "/refund"),
                        "{\"reason\":\"Asked nicely\"}")))
                .andExpect(status().isConflict());
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders/" + cashId + "/tip"),
                        "{\"amount\":150}"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tip").value(150))
                .andExpect(jsonPath("$.status").value("cash"));

        // The card that is refused when charged: the delivery still ends, and
        // says it was not paid for.
        Map unpaid = Trips.complete(mvc, customer, courier, Trips.trip(53.3498, -6.2603, 53.3600,
                -6.2500, "\"paymentMethodId\":\"" + failing + "\""));
        String unpaidId = (String) unpaid.get("id");
        assertEquals("COMPLETED", unpaid.get("state"));
        assertEquals("FAILED", unpaid.get("paymentStatus"));
        assertEquals("failed", Trips.receipt(mvc, customer, unpaidId).get("status"));
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders/" + unpaidId + "/tip"),
                        "{\"amount\":150}"), customer))
                .andExpect(status().isConflict());
        mvc.perform(admin(Trips.body(post("/api/admin/orders/" + unpaidId + "/refund"),
                        "{\"reason\":\"Nothing to return\"}")))
                .andExpect(status().isConflict());
        // A card an order is held on is not removed until the order ends. It
        // is called off before the kitchen takes it: once cooking, only an
        // admin can.
        String waiting = (String) Trips.place(mvc, customer, Trips.trip(53.3498, -6.2603, 53.3600,
                -6.2500, "\"paymentMethodId\":\"" + failing + "\"")).get("id");
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + failing + "/remove"), customer))
                .andExpect(status().isConflict());
        mvc.perform(People.asCustomer(post("/api/orders/" + waiting + "/cancel"), customer))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + failing + "/remove"), customer))
                .andExpect(status().isOk());
        Trips.offline(mvc, courier);
    }

    @Test
    void theHostedSetupEndsOnAPageThatSaysSo() throws Exception {
        String done = mvc.perform(get("/pay/return?result=done")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(done.indexOf("Card added") > 0, done);
        String cancelled = mvc.perform(get("/pay/return?result=%3Cscript%3E"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        // Nothing from the address is written into the page.
        assertTrue(cancelled.indexOf("No card was added") > 0, cancelled);
        assertEquals(-1, cancelled.indexOf("<script"));
    }
}
