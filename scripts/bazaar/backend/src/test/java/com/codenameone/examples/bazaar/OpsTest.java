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
    void theWarehouseBoardWalksAnOrderThroughPickPackAndDispatch() throws Exception {
        Shop s = shop("ops-warehouse");
        s.buy(People.customer(accounts, "ops-wh-buyer"), 2);
        String loc = "/warehouse/" + s.locationId;
        List board = list(s.owner, post(api(s, loc + "/sync")), "{}");
        assertEquals(1, board.size());
        Map task = (Map) board.get(0);
        assertEquals("PICK", task.get("stage"));
        assertEquals(1, list(s.owner, post(api(s, loc + "/sync")), "{}").size(), "no duplicate");
        list(s.owner, post(api(s, loc + "/bins")), "{\"code\":\"A-01\",\"aisle\":1,\"shelf\":1,"
                + "\"position\":1,\"variantId\":\"" + s.variantId + "\"}");
        Map pick = (Map) list(s.owner, get(api(s, loc + "/pick-list")), null).get(0);
        assertEquals("A-01", pick.get("binCode"));
        assertEquals(2L, Rest.number(pick, "quantity"));
        String id = (String) task.get("id");
        assertEquals(409, status(s.owner, post(api(s, "/warehouse/move")),
                "{\"taskId\":\"" + id + "\",\"stage\":\"DISPATCH\"}"), "one step at a time");
        assertEquals("PACK", json(s.owner, post(api(s, "/warehouse/move")),
                "{\"taskId\":\"" + id + "\",\"stage\":\"PACK\"}").get("stage"));
        Shop other = shop("ops-warehouse-o");
        assertEquals(404, status(other.owner, get(api(s, loc + "/board")), null));
        assertEquals(404, status(other.owner, post(api(other, "/warehouse/move")),
                "{\"taskId\":\"" + id + "\",\"stage\":\"DISPATCH\"}"));
        Browser web = signedIn(s.owner);
        web.assertPage(web.get("/admin/" + s.storeId + "/warehouse"), "Pick list, in walking order");
        assertEquals(403, web.postWithoutToken("/admin/" + s.storeId + "/warehouse/sync",
                "location", s.locationId).getStatus());
    }

    @Test
    void forecastsAndReportsReadTheSales() throws Exception {
        Shop s = shop("ops-reports");
        s.buy(People.customer(accounts, "ops-rep-buyer"), 7);
        Map f = (Map) list(s.owner, get(api(s, "/forecast")), null).get(0);
        assertEquals(25L, Rest.number(f, "dailyX100"), "7 units over 28 days");
        Map r = json(s.owner, post(api(s, "/reports/run")),
                "{\"dimension\":\"channel\",\"metrics\":\"orders,units\"}");
        assertEquals(1L, Rest.number(r, "total"));
        assertEquals(400, status(s.owner, post(api(s, "/reports/run")),
                "{\"dimension\":\"password\"}"));
        Map saved = json(s.owner, post(api(s, "/reports")),
                "{\"name\":\"By day\",\"dimension\":\"day\",\"metrics\":\"sales\"}");
        assertEquals(1, list(s.owner, get(api(s, "/reports")), null).size());
        assertTrue(Rest.number(json(s.owner, get(api(s, "/reports/" + saved.get("id"))), null),
                "total") > 0);
        assertEquals(1, list(s.owner, get(api(s, "/cohorts")), null).size());
        Browser web = signedIn(s.owner);
        MockResponse csv = web.get("/admin/" + s.storeId + "/reports/" + saved.get("id") + "/csv");
        assertEquals(200, csv.getStatus());
        assertTrue(csv.getContentAsString().startsWith("\"Day\",\"Sales\""));
        web.assertPage(web.get("/admin/" + s.storeId + "/cohorts"), "Retention by first-order month");
        Shop other = shop("ops-reports-o");
        assertEquals(404, status(other.owner, get(api(other, "/reports/" + saved.get("id"))), null));
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
    void permissionSetsReplaceTheRoleOnlyWhenAssigned() throws Exception {
        Shop s = shop("ops-perms");
        String picker = staff(s, "ops-picker", "FULFILMENT");
        String board = api(s, "/warehouse/" + s.locationId + "/board");
        // The legacy answer while nothing is assigned.
        assertEquals(200, status(picker, get(board), null));
        assertEquals(403, status(picker, get(api(s, "/reports")), null));
        assertEquals(403, status(picker, get(api(s, "/analytics")), null));
        assertEquals(200, status(picker, get(api(s, "/inventory")), null));
        assertEquals(403, status(picker, get(api(s, "/permission-sets")), null));
        Map set = json(s.owner, post(api(s, "/permission-sets")),
                "{\"name\":\"Analyst\",\"grants\":\"reports=R\"}");
        assertEquals("reports=R", set.get("grants"));
        list(s.owner, post(api(s, "/staff-permissions")), "{\"staffId\":\"" + staffId(s, picker)
                + "\",\"locationId\":\"\",\"setId\":\"" + set.get("id") + "\"}");
        assertEquals(200, status(picker, get(api(s, "/reports")), null));
        assertEquals(403, status(picker, get(board), null), "inventory is not in the set");
        // The stock and analytics routes answer to the set too, both ways.
        assertEquals(403, status(picker, get(api(s, "/inventory")), null), "stock follows the set");
        assertEquals(200, status(picker, get(api(s, "/analytics")), null), "reports=R reads analytics");
        assertEquals(200, status(picker, get(api(s, "/analytics/funnel")), null));
        assertEquals(200, status(s.owner, get(api(s, "/inventory")), null), "the owner keeps it all");
        assertEquals(403, status(picker, post(api(s, "/reports")),
                "{\"name\":\"x\",\"dimension\":\"day\"}"), "read is not write");
        // The owner is never restricted, and cannot be given a set.
        assertEquals(400, status(s.owner, post(api(s, "/staff-permissions")), "{\"staffId\":\""
                + staffId(s, s.owner) + "\",\"setId\":\"" + set.get("id") + "\"}"));
        // Removing the assignment restores the role.
        list(s.owner, post(api(s, "/staff-permissions")), "{\"staffId\":\"" + staffId(s, picker)
                + "\",\"locationId\":\"\",\"setId\":\"\"}");
        assertEquals(200, status(picker, get(board), null));
        Shop other = shop("ops-perms-o");
        assertEquals(404, status(other.owner, post(api(other, "/permission-sets/" + set.get("id"))),
                "{\"name\":\"Mine\",\"grants\":\"\"}"));
        Browser web = signedIn(s.owner);
        web.assertPage(web.get("/admin/" + s.storeId + "/permissions"), "Analyst");
        assertEquals(403, web.postWithoutToken("/admin/" + s.storeId + "/permissions",
                "name", "x").getStatus());
    }

    @Test
    void aShopperChatsAndTheStoreReplies() throws Exception {
        Shop s = shop("ops-chat");
        String h = handle(s);
        // The page offers the floating chat; what the visitor has is fetched.
        Browser shopper = new Browser(mvc);
        String page = shopper.get("/s/" + h).getContentAsString();
        assertTrue(page.contains("id=\"chat\"") && page.contains("data-store=\"" + h + "\""), "widget");
        MockResponse session = shopper.send(Browser.form("/s/" + h + "/chat/session", "token", ""));
        assertEquals(200, session.getStatus(), session.getContentAsString());
        assertTrue(session.getContentAsString().contains("\"ticket\":\""), session.getContentAsString());
        assertEquals("no-store", session.getHeader("Cache-Control"));
        // The old chat page now opens the widget.
        MockResponse old = shopper.get("/s/" + h + "/chat");
        assertTrue(old.getHeader("Location").endsWith("/s/" + h + "#chat"), old.getHeader("Location"));

        String id = chatInbox.start(s.storeId, "ops-chat-client", null, "ada-chat@example.com",
                "Ada", "Where is my order?");
        String token = Rows.text(chatInbox.row(id).get("token"));
        MockResponse again = shopper.send(Browser.form("/s/" + h + "/chat/session", "token", token));
        assertTrue(again.getContentAsString().contains(token), "the token resumes the conversation");
        try {
            chatInbox.start(s.storeId, "ops-chat-client", null, "not-an-address", "Ada", "hi");
            fail("a malformed address is refused");
        } catch (ResponseStatusException refused) {
            assertEquals(400, refused.getStatus());
        }
        Map chat = (Map) list(s.owner, get(api(s, "/chats")), null).get(0);
        assertEquals(id, chat.get("id"));
        assertEquals(1, ((Number) chat.get("unread")).intValue(), "the shopper's message is unread");
        int before = MailSender.current().sent().size();
        List messages = list(s.owner, post(api(s, "/chats/" + id + "/reply")),
                "{\"body\":\"It ships today\"}");
        assertEquals(2, messages.size());
        boolean mailed = false;
        List<Mail> mails = MailSender.current().sent();
        for (int iter = before; iter < mails.size(); iter++) {
            mailed |= mails.get(iter).getTo().contains("ada-chat@example.com");
        }
        assertTrue(mailed, "a shopper with the chat closed is mailed the reply");
        Map answered = (Map) list(s.owner, get(api(s, "/chats")), null).get(0);
        assertEquals(0, ((Number) answered.get("unread")).intValue(), "a reply reads the thread");
        Thread.sleep(5); // the read mark is the reply's millisecond
        chatInbox.say(s.storeId, id, "Thanks!");
        Map more = (Map) list(s.owner, get(api(s, "/chats")), null).get(0);
        assertEquals(1, ((Number) more.get("unread")).intValue(), "a new shopper line is unread again");
        chatInbox.seen(id, true, System.currentTimeMillis() + 1);
        Map seen = (Map) list(s.owner, get(api(s, "/chats")), null).get(0);
        assertEquals(0, ((Number) seen.get("unread")).intValue(), "seen clears it");
        assertEquals("CLOSED", json(s.owner, post(api(s, "/chats/" + id + "/close")), "{}").get("status"));
        Shop other = shop("ops-chat-o");
        assertEquals(404, status(other.owner, get(api(other, "/chats/" + id)), null));
        assertEquals(null, chatInbox.byToken(other.storeId, token), "another store's thread");
        Browser web = signedIn(s.owner);
        String inbox = web.get("/admin/" + s.storeId + "/inbox/" + id).getContentAsString();
        assertTrue(inbox.contains("It ships today") && inbox.contains("id=\"inbox-live\""), inbox);
        MockResponse live = web.get("/admin/" + s.storeId + "/chat-ticket");
        assertEquals(200, live.getStatus());
        assertTrue(live.getContentAsString().contains("\"ticket\":\""));
        assertFalse(web.get("/admin/" + other.storeId + "/chat-ticket").getStatus() == 200,
                "no ticket for another store's inbox");
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
