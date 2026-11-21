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
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Primary;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.TestConfiguration;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.AudienceDto;
import com.codenameone.examples.bazaar.api.CampaignDto;
import com.codenameone.examples.bazaar.api.JourneyDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.ReceiptSendDto;
import com.codenameone.examples.bazaar.api.TranslationDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.Profile;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.journey.JourneyCatalog;
import com.codenameone.examples.bazaar.journey.Journeys;
import com.codenameone.examples.bazaar.market.Translations;
import com.codenameone.examples.bazaar.marketing.Audiences;
import com.codenameone.examples.bazaar.marketing.Campaigns;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.pos.Pos;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.ProfileRepository;
import com.codenameone.examples.bazaar.sms.SmsSender;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.StoreText;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Merchant-written messages in the reader's language: a campaign's
/// translations ([Translations#CAMPAIGN]), a journey step's language
/// variants (`body@es`), the till receipt texted to a shopper, and the
/// low-stock mail to staff.
@BackendTest
class MessageLanguageTest {
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
    private MockMvc mvc;
    @Autowired
    private DataSource db;
    @Autowired
    private Audiences audiences;
    @Autowired
    private Campaigns campaigns;
    @Autowired
    private Translations translations;
    @Autowired
    private Journeys journeys;
    @Autowired
    private com.codenameone.examples.bazaar.order.Orders orderService;
    @Autowired
    private Pos pos;
    @Autowired
    private Emails emails;
    @Autowired
    private ProfileRepository profiles;
    @Autowired
    private RecordingSms sms;
    private final MailSender mail = MailSender.current();

    /// Texts that are recorded instead of sent, and that count as delivered
    /// so the senders that check [SmsSender#delivers] send them.
    static final class RecordingSms implements SmsSender {
        final List<String[]> sent = new ArrayList<String[]>();

        @Override
        public synchronized void send(String phone, String text) {
            sent.add(new String[] {phone, text});
        }

        @Override
        public boolean delivers() {
            return true;
        }

        synchronized String to(String phone) {
            String found = null;
            for (int iter = 0; iter < sent.size(); iter++) {
                if (sent.get(iter)[0].equals(phone)) {
                    found = sent.get(iter)[1];
                }
            }
            return found;
        }
    }

    @TestConfiguration
    static class Texts {
        @Bean
        @Primary
        RecordingSms recordingSms() {
            return new RecordingSms();
        }
    }

    interface Call {
        void run() throws Exception;
    }

    /// The status `call` was refused with, or 200.
    private static int refusal(Call call) throws Exception {
        try {
            call.run();
            return 200;
        } catch (com.codename1.backend.ResponseStatusException refused) {
            return refused.getStatus();
        }
    }

    private Shop shop(String name) throws Exception {
        return new Shop(name, 20, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static String t(String lang, String key, Object... args) {
        return StoreText.format(StoreText.table(lang), key, args);
    }

    private Mail only(String to) throws Exception {
        Mail found = null;
        for (Mail m : mail.sent()) {
            if (m.getTo().contains(to)) {
                if (found != null) {
                    fail("two mails to " + to + ": " + found.getSubject() + ", " + m.getSubject());
                }
                found = m;
            }
        }
        if (found == null) {
            fail("no mail to " + to);
        }
        return found;
    }

    private void speaks(Shop s, String email, String lang, String phone) throws Exception {
        db.execute("UPDATE customer SET language = ?, phone = ?, accepts_marketing = ? "
                + "WHERE store_id = ? AND email = ?", new Object[] {lang, phone, Boolean.TRUE,
                    s.storeId, email});
    }

    private void translate(Shop s, String kind, String ref, String lang, String field,
            String value) throws Exception {
        translations.put(s.owner, s.storeId, new TranslationDto(kind, ref, lang, field, value));
    }

    @Test
    void aCampaignGoesOutInEachSubscribersLanguageAndFallsBackWhole() throws Exception {
        Shop s = shop("ml-camp");
        String[] who = {"ml-camp-es", "ml-camp-he", "ml-camp-fr", "ml-camp-de"};
        String[] lang = {"es", "he", "fr", "de"};
        for (int iter = 0; iter < who.length; iter++) {
            String user = People.customer(accounts, who[iter]);
            s.buy(user, 1, who[iter] + "@example.com");
            speaks(s, who[iter] + "@example.com", lang[iter], "");
        }
        AudienceDto seg = audiences.write(s.storeId, null, new AudienceDto(null, "Buyers",
                "{\"op\":\"AND\",\"rules\":[{\"field\":\"ordersCount\",\"op\":\">=\",\"value\":1}]}",
                0));
        CampaignDto c = campaigns.write(s.storeId, null, new CampaignDto(null, "Sale",
                Campaigns.EMAIL, seg.id(), "Summer sale", "", "Everything is half price", 0, 0, "",
                0, "", "#000000", new ArrayList<String>()));
        translate(s, Translations.CAMPAIGN, c.id(), "es", "subjectA", "Rebajas de verano");
        translate(s, Translations.CAMPAIGN, c.id(), "es", "body", "Todo a mitad de precio");
        translate(s, Translations.CAMPAIGN, c.id(), "he", "subjectA",
                "\u05de\u05d1\u05e6\u05e2 \u05e7\u05d9\u05e5");
        translate(s, Translations.CAMPAIGN, c.id(), "he", "body",
                "\u05d4\u05db\u05d5\u05dc \u05d1\u05d7\u05e6\u05d9 \u05de\u05d7\u05d9\u05e8");
        // French has a body and no subject: the whole original goes out.
        translate(s, Translations.CAMPAIGN, c.id(), "fr", "body", "Tout est a moitie prix");
        mail.clearSent();
        campaigns.schedule(s.owner, s.storeId, c.id(), 0);
        campaigns.tick(Instant.now());

        Mail es = only("ml-camp-es@example.com");
        assertEquals("Rebajas de verano", es.getSubject());
        assertTrue(es.getText().contains("Todo a mitad de precio"), es.getText());
        Mail he = only("ml-camp-he@example.com");
        assertEquals("\u05de\u05d1\u05e6\u05e2 \u05e7\u05d9\u05e5", he.getSubject());
        assertTrue(he.getHtml().contains("dir=\"rtl\""), "a Hebrew page");
        Mail fr = only("ml-camp-fr@example.com");
        assertEquals("Summer sale", fr.getSubject());
        assertTrue(fr.getText().contains("Everything is half price"), fr.getText());
        assertTrue(!fr.getText().contains("moitie"), "never half translated");
        Mail de = only("ml-camp-de@example.com");
        assertEquals("Summer sale", de.getSubject());

        // A campaign's translation never decides a storefront's language.
        assertEquals(null, translations.pick(s.storeId, java.util.Arrays.asList("he")));

        // Another store's owner can neither read nor translate this campaign.
        Shop other = shop("ml-camp-other");
        assertEquals(404, refusal(() -> translations.put(other.owner, other.storeId,
                new TranslationDto(Translations.CAMPAIGN, c.id(), "es", "body", "x"))));
        assertEquals(404, refusal(() -> translations.of(other.owner, other.storeId,
                Translations.CAMPAIGN, c.id())));
    }

    @Test
    void aJourneyTextIsSentInTheCustomersLanguage() throws Exception {
        Shop s = shop("ml-journey");
        String graph = "{\"nodes\":[{\"id\":\"e\",\"data\":{\"type\":\"" + JourneyCatalog.ENTRY_TRIGGER
                + "\",\"event\":\"order.paid\"}},{\"id\":\"m\",\"data\":{\"type\":\""
                + JourneyCatalog.SEND_SMS + "\",\"body\":\"Thanks for your order\"}},"
                + "{\"id\":\"x\",\"data\":{\"type\":\"" + JourneyCatalog.EXIT + "\"}}],"
                + "\"edges\":[{\"from\":\"e\",\"fromPort\":\"out\",\"to\":\"m\"},"
                + "{\"from\":\"m\",\"fromPort\":\"out\",\"to\":\"x\"}]}";
        JourneyDto j = journeys.write(s.storeId, null, new JourneyDto(null, "Thanks", graph,
                Journeys.ACTIVE, 0, null));
        Map<String, String> es = new HashMap<String, String>();
        es.put("body", "Gracias por tu pedido");
        final String jid = j.id();
        Shop stranger = shop("ml-journey-other");
        assertEquals(404, refusal(() -> journeys.translate(stranger.owner, stranger.storeId, jid, "m",
                "es", es)));
        assertEquals(400, refusal(() -> journeys.translate(s.owner, s.storeId, jid, "e", "es",
                es)), "only a message step");
        j = journeys.translate(s.owner, s.storeId, j.id(), "m", "es", es);
        assertTrue(j.graphJson().contains("body@es"), j.graphJson());
        assertTrue(j.problems().isEmpty(), j.problems().toString());

        String user = People.customer(accounts, "ml-journey-es");
        OrderDto o = s.buy(user, 1, "ml-journey-es@example.com");
        speaks(s, "ml-journey-es@example.com", "es", "+15550001001");
        String other = People.customer(accounts, "ml-journey-de");
        OrderDto p = s.buy(other, 1, "ml-journey-de@example.com");
        speaks(s, "ml-journey-de@example.com", "de", "+15550001002");
        orderService.capturePayment(o.id(), s.owner);
        orderService.capturePayment(p.id(), s.owner);
        journeys.tick(Instant.now());
        assertEquals("Gracias por tu pedido", sms.to("+15550001001"));
        assertEquals("Thanks for your order", sms.to("+15550001002"), "German falls back");
    }

    @Test
    void theTillReceiptIsTextedInTheShoppersLanguage() throws Exception {
        Shop s = shop("ml-pos");
        String user = People.customer(accounts, "ml-pos-buyer");
        OrderDto o = s.buy(user, 1, user);
        db.execute("UPDATE store_order SET language = ? WHERE id = ?", new Object[] {"es", o.id()});
        pos.sendReceipt(s.owner, s.storeId, o.id(), new ReceiptSendDto("", "+15550002001"));
        String text = sms.to("+15550002001");
        assertNotNull(text, "texted");
        assertTrue(text.contains(t("es", "receipt.thanks")), text);
        assertTrue(text.contains(t("es", "receipt.order", o.number())), text);
        assertTrue(!text.contains("Thank you"), text);
    }

    @Test
    void lowStockMailIsInEachStaffMembersLanguage() throws Exception {
        Shop s = shop("ml-low");
        Profile p = profiles.findById(s.owner);
        assertNotNull(p, "the owner has a profile");
        p.setLanguage("he");
        profiles.save(p);
        mail.clearSent();
        emails.lowStock(s.storeId, "ML-SKU", 2, 5);
        Mail m = only(s.owner);
        assertEquals(t("he", "mail.low.subject", "ML-SKU"), m.getSubject());
        assertTrue(m.getText().contains(t("he", "mail.low.body", "ML-SKU", 2, 5)), m.getText());

        p.setLanguage("");
        profiles.save(p);
        mail.clearSent();
        emails.lowStock(s.storeId, "ML-SKU", 1, 5);
        assertEquals("Low stock: ML-SKU", only(s.owner).getSubject(), "English by default");
    }
}
