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
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.ops.Attribution;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The operations features end to end: the warehouse board, forecasts,
/// the report builder, attribution, permission sets, the chat inbox and the
/// marketplace import, each through its REST route and (where it has one)
/// its back-office page.
@BackendTest
class OpsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private DataSource db;
    @Autowired
    private Inventory inventory;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private Attribution attribution;
    @Autowired
    private com.codenameone.examples.bazaar.ops.ChatInbox chatInbox;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 300, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static String api(Shop s, String path) {
        return "/api/merchant/stores/" + s.storeId + path;
    }

    private MockRequestBuilder as(MockRequestBuilder b, String user) {
        return People.asMerchant(b, user);
    }

    private Map json(String user, MockRequestBuilder b, String body) throws Exception {
        return Rest.json(mvc, as(body == null ? b : Rest.body(b, body), user));
    }

    private List list(String user, MockRequestBuilder b, String body) throws Exception {
        return Rest.list(mvc, as(body == null ? b : Rest.body(b, body), user));
    }

    private int status(String user, MockRequestBuilder b, String body) throws Exception {
        return Rest.statusOf(mvc, as(body == null ? b : Rest.body(b, body), user));
    }

    /// A member of `s`'s staff with `role`; answers the username.
    private String staff(Shop s, String name, String role) throws Exception {
        String user = People.customer(accounts, name);
        json(s.owner, post(api(s, "/staff")), "{\"username\":\"" + user + "\",\"role\":\"" + role
                + "\",\"pin\":\"\"}");
        return user;
    }

    private String staffId(Shop s, String user) throws Exception {
        Map r = db.queryOne("SELECT id FROM store_staff WHERE store_id = ? AND username = ?",
                new Object[] {s.storeId, user});
        return Rows.text(r.get("id"));
    }

    private Browser signedIn(String user) throws Exception {
        Browser b = new Browser(mvc);
        b.signIn(user, People.PASSWORD);
        return b;
    }

    private String handle(Shop s) throws Exception {
        return Rows.text(db.queryOne("SELECT handle FROM store WHERE id = ?",
                new Object[] {s.storeId}).get("handle"));
    }

    @Test
    void anOrderCarriesTheCartsAttribution() throws Exception {
        Shop s = shop("ops-attribution");
        String orderId = (String) s.buy(People.customer(accounts, "ops-attr"), 1).id();
        db.execute("INSERT INTO attribution (id, store_id, subject_kind, subject_id, first_source, "
                + "first_medium, first_campaign, last_source, last_medium, last_campaign, created_at) "
                + "VALUES ('opsattrcart', ?, 'CART', 'opscart', 'news', 'email', 'fall', 'ads', "
                + "'cpc', '', 0)", new Object[] {s.storeId});
        attribution.stampOrder(s.storeId, "opscart", orderId);
        List rows = list(s.owner, get(api(s, "/attribution")), null);
        boolean first = false;
        boolean last = false;
        for (Object o : rows) {
            Map m = (Map) o;
            first |= "FIRST".equals(m.get("touch")) && "news".equals(m.get("source"));
            last |= "LAST".equals(m.get("touch")) && "ads".equals(m.get("source"));
        }
        assertTrue(first && last, rows.toString());
        assertEquals("ads", Rows.text(db.queryOne("SELECT last_source FROM attribution WHERE "
                + "subject_kind = 'ORDER' AND subject_id = ?", new Object[] {orderId}).get("last_source")));
    }

    @Test
    void theMarketplaceImportPreviewsThenCombinesColours() throws Exception {
        Shop s = shop("ops-import");
        String bad = "listing_id,title,sku,price,color\\n1,Tee,T1,abc,Red\\n";
        Map dry = json(s.owner, post(api(s, "/imports/marketplace")),
                "{\"csv\":\"" + bad + "\",\"commit\":true}");
        assertEquals(Boolean.FALSE, dry.get("committed"));
        assertEquals(1, ((List) dry.get("errors")).size());
        String good = "listing_id,title,sku,price,color,size,quantity\\n"
                + "9,Ops Tee,OT-R-S,12.50,Red,S,3\\n9,Ops Tee,OT-R-M,12.50,Red,M,3\\n"
                + "9,Ops Tee,OT-B-S,12.50,Blue,S,3\\n";
        Map preview = json(s.owner, post(api(s, "/imports/marketplace")),
                "{\"csv\":\"" + good + "\",\"commit\":false}");
        assertEquals(Boolean.FALSE, preview.get("committed"));
        assertEquals(2L, Rest.number(preview, "products"));
        Map done = json(s.owner, post(api(s, "/imports/marketplace")),
                "{\"csv\":\"" + good + "\",\"commit\":true}");
        assertEquals(Boolean.TRUE, done.get("committed"), done.toString());
        List combined = list(s.owner, get(api(s, "/combined-listings")), null);
        assertEquals(1, combined.size());
        assertEquals(2, ((List) ((Map) combined.get(0)).get("members")).size());
        Map again = json(s.owner, post(api(s, "/imports/marketplace")),
                "{\"csv\":\"" + good + "\",\"commit\":true}");
        assertEquals(Boolean.FALSE, again.get("committed"), "the SKUs exist now");
    }
}
