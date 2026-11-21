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
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.FulfillmentDto;
import com.codenameone.examples.bazaar.api.GiftCardDto;
import com.codenameone.examples.bazaar.api.LabelRequestDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PackDto;
import com.codenameone.examples.bazaar.api.RateQuoteDto;
import com.codenameone.examples.bazaar.api.ReceiptSendDto;
import com.codenameone.examples.bazaar.api.ReturnDto;
import com.codenameone.examples.bazaar.api.ReturnReceiveDto;
import com.codenameone.examples.bazaar.api.ReturnRefundDto;
import com.codenameone.examples.bazaar.api.ReturnRequestDto;
import com.codenameone.examples.bazaar.cart.AbandonedCarts;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.giftcard.GiftCards;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pos.Pos;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.returns.Returns;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The mail a store sends, read back from the memory transport the test
/// profile runs on: who it went to, what it says on the subject line and what
/// it carries.
@BackendTest
class EmailsTest {
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private Inventory inventory;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private Fulfillments fulfillments;
    @Autowired
    private Returns returns;
    @Autowired
    private Pos pos;
    @Autowired
    private GiftCards giftCards;
    @Autowired
    private AbandonedCarts abandoned;
    @Autowired
    private CustomerRepository customers;
    @Autowired
    private StoreRepository storeRows;
    @Autowired
    private OrderLineRepository lines;
    private final MailSender mail = MailSender.current();
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;

    private Shop shop(String name, int stock) throws Exception {
        return new Shop(name, stock, accounts, stores, pricing, db, inventory, carts, checkouts,
                mvc);
    }

    /// The one mail to `to` whose subject starts with `subject`.
    private Mail find(String to, String subject) throws Exception {
        Mail found = null;
        for (Mail m : mail.sent()) {
            if (m.getTo().contains(to) && m.getSubject().startsWith(subject)) {
                if (found != null) {
                    fail("two mails to " + to + ": " + subject);
                }
                found = m;
            }
        }
        if (found == null) {
            StringBuilder had = new StringBuilder();
            for (Mail m : mail.sent()) {
                had.append(m.getTo()).append(' ').append(m.getSubject()).append("; ");
            }
            fail("no mail to " + to + ": " + subject + " (sent: " + had + ")");
        }
        return found;
    }

    private static void assertPdf(Mail m, String prefix) {
        assertEquals(1, m.getAttachmentNames().size(), m.getSubject());
        String name = m.getAttachmentNames().get(0);
        assertTrue(name.startsWith(prefix) && name.endsWith(".pdf"), name);
        byte[] pdf = m.getAttachment(name);
        assertTrue(pdf.length > 4 && pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D'
                && pdf[3] == 'F', "a PDF");
    }

    private FulfillmentDto fulfil(Shop s, OrderDto o, int qty, String method) throws Exception {
        List<FulfilLineDto> ls = new ArrayList<FulfilLineDto>();
        ls.add(new FulfilLineDto(lines.findByOrderId(o.id()).get(0).getId(), null, null, qty));
        return fulfillments.create(s.owner, s.storeId, o.id(),
                new NewFulfillmentDto(s.locationId, method, ls));
    }

    @Test
    void anOrderIsConfirmedAndItsParcelTracked() throws Exception {
        Shop s = shop("mail-ship", 5);
        String user = People.customer(accounts, "mail-ship-buyer");
        String to = user;
        mail.clearSent();
        OrderDto o = s.buy(user, 1, to);
        Mail confirmed = find(to, "Order #" + o.number() + " confirmed");
        assertPdf(confirmed, "invoice-");
        String accent = Integer.toHexString(storeRows.findById(s.storeId).getAccentColor() & 0xffffff);
        assertTrue(confirmed.getHtml().contains("background:#" + accent)
                || confirmed.getHtml().contains("background:#4f46e5"), "in the store's accent");
        assertTrue(confirmed.getText().contains("Mug"), confirmed.getText());

        FulfillmentDto f = fulfil(s, o, 1, "SHIP");
        fulfillments.pack(s.owner, s.storeId, f.id(), new PackDto(0));
        List<RateQuoteDto> rates = fulfillments.rates(s.owner, s.storeId, f.id());
        FulfillmentDto labelled = fulfillments.buyLabel(s.owner, s.storeId, f.id(),
                new LabelRequestDto(rates.get(0).service()));
        fulfillments.ship(s.owner, s.storeId, f.id());
        Mail shipped = find(to, "Order #" + o.number() + " has shipped");
        assertTrue(shipped.getText().contains(labelled.trackingNumber()), shipped.getText());
        assertTrue(shipped.getHtml().contains("/track/" + o.number()), "a tracking link");
    }

    @Test
    void deliveryReturnAndRefundAreMailed() throws Exception {
        Shop s = shop("mail-return", 5);
        String user = People.customer(accounts, "mail-return-buyer");
        String to = user;
        OrderDto o = s.buy(user, 2, to);
        mail.clearSent();
        FulfillmentDto f = fulfil(s, o, 2, "LOCAL");
        fulfillments.ship(s.owner, s.storeId, f.id());
        fulfillments.delivered(s.owner, s.storeId, f.id());
        find(to, "Order #" + o.number() + " was delivered");
        Mail review = find(to, "Tell us about order #" + o.number());
        assertTrue(review.getHtml().contains("#reviews"), "links to the product's reviews");

        List<FulfilLineDto> one = new ArrayList<FulfilLineDto>();
        one.add(new FulfilLineDto(lines.findByOrderId(o.id()).get(0).getId(), null, null, 1));
        ReturnDto r = returns.request(user, o.id(), new ReturnRequestDto("Too big", "", one));
        returns.approve(s.owner, s.storeId, r.id());
        find(to, "Return approved for order #" + o.number());
        returns.receive(s.owner, s.storeId, r.id(), new ReturnReceiveDto(true));
        returns.refund(s.owner, s.storeId, r.id(), new ReturnRefundDto(0));
        Mail refund = find(to, "Refund for order #" + o.number());
        assertPdf(refund, "credit-note-");
        assertTrue(refund.getText().contains("$10.40"), refund.getText());
    }

    @Test
    void receiptsGiftCardsAndCartReminders() throws Exception {
        Shop s = shop("mail-pos", 5);
        String user = People.customer(accounts, "mail-pos-buyer");
        String to = user;
        OrderDto o = s.buy(user, 1, to);
        mail.clearSent();

        pos.sendReceipt(s.owner, s.storeId, o.id(), new ReceiptSendDto("till@example.com", ""));
        assertPdf(find("till@example.com", "Your receipt from"), "receipt-");

        Customer c = customers.findByStoreIdAndEmail(s.storeId, to);
        assertNotNull(c, "the buyer is a customer of the store");
        GiftCardDto card = giftCards.issue(s.owner, s.storeId,
                new GiftCardDto(null, null, null, 2500, 0, true, "", 0, c.getId(), 0));
        Mail gift = find(to, "Your mail-pos gift card");
        assertTrue(gift.getText().contains(card.lastFour()), gift.getText());
        assertTrue(gift.getHtml().contains("cid:giftcard-qr"), "shows the QR code");
        assertTrue(gift.getText().contains("$25.00"), gift.getText());

        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        abandoned.sweepAbandoned(System.currentTimeMillis() + 3 * AbandonedCarts.IDLE_MILLIS);
        Mail reminder = find(to, "Your cart at mail-pos is waiting");
        assertTrue(reminder.getHtml().contains("store="), "opens the cart");
        // The button is the absolute web cart with the recovery token.
        String token = carts.cart(s.storeId, user, null).getToken();
        String path = "/s/mail-pos/cart?r=" + token;
        assertTrue(reminder.getHtml().contains("http://localhost" + path)
                || reminder.getHtml().contains("https://") && reminder.getHtml().contains(path),
                reminder.getHtml());
        String title = carts.get(s.storeId, user, null).lines().get(0).title();
        Browser stranger = new Browser(mvc);
        assertFalse(stranger.get("/s/mail-pos/cart").getContentAsString().contains(title),
                "a fresh session has an empty cart");
        Browser back = new Browser(mvc);
        back.assertPage(back.get(path), title);
        back.assertPage(back.get("/s/mail-pos/cart"), title);
        Browser forged = new Browser(mvc);
        assertFalse(forged.get("/s/mail-pos/cart?r=nope").getContentAsString().contains(title),
                "an unknown token restores nothing");
    }
}
