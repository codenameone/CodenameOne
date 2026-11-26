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
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.cart.AbandonedCarts;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.CartRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.StoreText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Shopper mail in the shopper's language: the order and the cart record the
/// language they were placed or filled in, and the mail about each is
/// written in it, English when nothing is known.
@BackendTest
class MailLanguageTest {
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
    private AbandonedCarts abandoned;
    @Autowired
    private CartRepository cartRows;
    @Autowired
    private StoreOrderRepository orderRows;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;
    private final MailSender mail = MailSender.current();

    private Shop shop(String name) throws Exception {
        return new Shop(name, 9, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    /// `user`'s cart at `s`, filled in `lang` (what a storefront page or the
    /// app's Accept-Language records when the shopper adds to it).
    private void cartIn(Shop s, String user, String lang) throws Exception {
        Cart c = carts.cart(s.storeId, user, null);
        c.setLanguage(lang);
        cartRows.save(c);
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

    private static String t(String lang, String key, Object... args) {
        return StoreText.format(StoreText.table(lang), key, args);
    }

    @Test
    void anOrderPlacedInSpanishIsConfirmedInSpanish() throws Exception {
        Shop s = shop("lang-es");
        String user = People.customer(accounts, "lang-es-buyer");
        cartIn(s, user, "es");
        mail.clearSent();
        OrderDto o = s.buy(user, 1, user);
        assertEquals("es", orderRows.findById(o.id()).getLanguage(), "the order keeps it");
        Mail m = only(user);
        assertEquals(t("es", "mail.order.subject", o.number()), m.getSubject());
        assertTrue(m.getText().contains(t("es", "mail.order.title")), m.getText());
        assertTrue(m.getHtml().contains("lang=\"es\" dir=\"ltr\""), "a Spanish page");
    }

    @Test
    void anOrderPlacedInHebrewIsConfirmedInHebrewRightToLeft() throws Exception {
        Shop s = shop("lang-he");
        String user = People.customer(accounts, "lang-he-buyer");
        cartIn(s, user, "he");
        mail.clearSent();
        OrderDto o = s.buy(user, 1, user);
        Mail m = only(user);
        assertEquals(t("he", "mail.order.subject", o.number()), m.getSubject());
        assertTrue(m.getHtml().contains("lang=\"he\" dir=\"rtl\""), "turned right to left");
        assertTrue(m.getHtml().contains("<body dir=\"rtl\""), m.getHtml());
    }

    @Test
    void anOrderInNoKnownLanguageIsConfirmedInEnglish() throws Exception {
        Shop s = shop("lang-none");
        String user = People.customer(accounts, "lang-none-buyer");
        mail.clearSent();
        OrderDto o = s.buy(user, 1, user);
        Mail m = only(user);
        assertEquals("Order #" + o.number() + " confirmed", m.getSubject());
        assertTrue(m.getHtml().contains("lang=\"en\" dir=\"ltr\""), "an English page");
    }

    @Test
    void aCartReminderIsInTheCartsLanguage() throws Exception {
        Shop s = shop("lang-cart");
        String user = People.customer(accounts, "lang-cart-buyer");
        s.buy(user, 1, user);
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        cartIn(s, user, "de");
        mail.clearSent();
        abandoned.sweepAbandoned(System.currentTimeMillis() + 3 * AbandonedCarts.IDLE_MILLIS);
        Mail m = only(user);
        assertEquals(t("de", "mail.cart.subject", "lang-cart"), m.getSubject());
        assertTrue(m.getText().contains(t("de", "mail.cart.button")), m.getText());
    }
}
