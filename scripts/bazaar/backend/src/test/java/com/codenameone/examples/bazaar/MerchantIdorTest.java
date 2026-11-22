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
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.CompanyBuyerDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.marketing.DemoMarketing;
import com.codenameone.examples.bazaar.subscribe.Subscriptions;
import com.codenameone.examples.bazaar.trade.Trade;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Every route under `/api/merchant/stores/{storeId}` refuses a member of
/// another store's staff with 404 -- the same answer as a store that does not
/// exist -- and an id of another store's object under one's own store is not
/// found either.
///
/// [#ROUTES] is every such route the shared API interfaces declare.
/// [#theListIsEveryRouteTheInterfacesDeclare] reads the interfaces when the
/// sources are beside the test (a JVM run from the module) and fails on a
/// route missing here, so a new endpoint is a new line in this list.
///
/// The nested ids are real where store B has such a row: a handler that loaded
/// an object by id without asking whose store it is would answer it. A
/// placeholder with no row of store B falls back to an id nothing has, and the
/// test prints which tables those were.
@BackendTest
class MerchantIdorTest {
    /// Method and path after `/api/merchant/stores/{storeId}`.
    static final String[] ROUTES = {
        "DELETE /automations/{automationId}", "DELETE /collections/{collectionId}",
        "DELETE /discounts/{discountId}", "DELETE /hero", "DELETE /locations/{locationId}",
        "DELETE /logo", "DELETE /products/{productId}/photos/{photoId}",
        "DELETE /shipping/rates/{rateId}", "DELETE /shipping/zones/{zoneId}",
        "DELETE /staff/{staffId}", "DELETE /suppliers/{supplierId}", "DELETE /tax-rates/{rateId}",
        "DELETE /webhooks/{webhookId}", "GET ", "GET /analytics", "GET /analytics/funnel",
        "GET /automation-catalog", "GET /automations", "GET /automations/{automationId}",
        "GET /automations/{automationId}/activity", "GET /collections", "GET /customer-export",
        "GET /customer-segments", "GET /customers", "GET /customers/{customerId}",
        "GET /discounts", "GET /fulfillments/{id}/rates", "GET /fulfilment/board",
        "GET /fulfilment/queue", "GET /gift-cards", "GET /inventory", "GET /inventory/low",
        "GET /inventory/moves", "GET /locations", "GET /lookup", "GET /orders",
        "GET /orders/{orderId}", "GET /orders/{orderId}/fulfillments",
        "GET /orders/{orderId}/refunds", "GET /pos/customers", "GET /pos/orders/{orderId}/receipt",
        "GET /pos/registers", "GET /pos/registers/{registerId}/catalog",
        "GET /pos/shifts/{shiftId}", "GET /pos/shifts/{shiftId}/cash", "GET /products",
        "GET /products/{productId}", "GET /purchase-orders", "GET /purchase-orders/{orderId}",
        "GET /returns", "GET /reviews", "GET /shipping", "GET /staff", "GET /suppliers",
        "GET /tax-rates", "GET /transfers", "GET /webhook-topics", "GET /webhooks",
        "GET /webhooks/{webhookId}/deliveries", "POST /automations", "POST /automations-validate",
        "POST /automations/{automationId}", "POST /collections", "POST /customers/{customerId}",
        "POST /customers/{customerId}/credit", "POST /discounts", "POST /drafts",
        "POST /drafts/{orderId}/convert", "POST /drafts/{orderId}/invoice",
        "POST /fulfillments/{id}/cancel", "POST /fulfillments/{id}/delivered",
        "POST /fulfillments/{id}/label", "POST /fulfillments/{id}/pack",
        "POST /fulfillments/{id}/picked-up", "POST /fulfillments/{id}/pickup-ready",
        "POST /fulfillments/{id}/ship", "POST /fulfilment/board/move", "POST /gift-cards",
        "POST /gift-cards/{cardId}/disable", "POST /hero", "POST /inventory/adjust",
        "POST /inventory/count", "POST /inventory/reorder-point", "POST /locations", "POST /logo",
        "POST /orders/{orderId}/cancel", "POST /orders/{orderId}/fulfillments",
        "POST /orders/{orderId}/mark-paid", "POST /orders/{orderId}/note",
        "POST /orders/{orderId}/refunds", "POST /pos/orders/{orderId}/receipt",
        "POST /pos/registers", "POST /pos/registers/{registerId}",
        "POST /pos/registers/{registerId}/orders/{orderId}/return",
        "POST /pos/registers/{registerId}/remove", "POST /pos/registers/{registerId}/sales",
        "POST /pos/registers/{registerId}/shifts", "POST /pos/registers/{registerId}/sign-in",
        "POST /pos/shifts/{shiftId}/cash", "POST /pos/shifts/{shiftId}/close", "POST /products",
        "GET /pos/policy", "POST /pos/policy", "GET /pos/staff-rates", "POST /pos/staff-rates",
        "GET /pos/commissions", "POST /pos/registers/{registerId}/approvals",
        "GET /pos/registers/{registerId}/parked", "POST /pos/registers/{registerId}/parked",
        "POST /pos/registers/{registerId}/parked/{parkedId}/resume",
        "POST /pos/registers/{registerId}/parked/{parkedId}/discard",
        "GET /pos/locations/{locationId}/grid", "POST /pos/locations/{locationId}/grid",
        "POST /orders/{orderId}/edit", "GET /orders/{orderId}/edits",
        "POST /pos/registers/{registerId}/layaways", "GET /layaways", "GET /layaways/{layawayId}",
        "POST /layaways/{layawayId}/payments", "POST /layaways/{layawayId}/cancel",
        "POST /products/bulk", "POST /products/{productId}/archive",
        "POST /products/{productId}/duplicate", "POST /products/{productId}/photos",
        "POST /purchase-orders", "POST /purchase-orders/{orderId}/cancel",
        "POST /purchase-orders/{orderId}/order", "POST /purchase-orders/{orderId}/receive",
        "POST /returns/{id}/approve", "POST /returns/{id}/decline", "POST /returns/{id}/receive",
        "POST /returns/{id}/refund", "POST /reviews/{reviewId}/publish",
        "POST /reviews/{reviewId}/reject", "POST /shipping/rates", "POST /shipping/zones",
        "POST /staff", "POST /suppliers", "POST /tax-rates", "POST /transfers",
        "POST /transfers/{transferId}/cancel", "POST /transfers/{transferId}/receive",
        "POST /transfers/{transferId}/send", "POST /webhook-deliveries/{deliveryId}/redeliver",
        "POST /webhooks", "POST /webhooks/{webhookId}", "POST /webhooks/{webhookId}/test",
        "PUT ", "PUT /collections/{collectionId}", "PUT /locations/{locationId}",
        "PUT /products/{productId}",
        // The growth features: loyalty, clubs, sales, alerts, marketing, journeys.
        "DELETE /clubs/{clubId}/items/{itemId}", "DELETE /segments/{segmentId}",
        "GET /campaigns", "GET /campaigns/{campaignId}/stats", "GET /clubs",
        "GET /clubs/{clubId}/items", "GET /clubs/{clubId}/members",
        "GET /discounts/{discountId}/rules", "GET /journey-catalog", "GET /journeys",
        "GET /journeys/{journeyId}", "GET /journeys/{journeyId}/stats",
        "GET /loyalty/cards/{token}", "GET /loyalty/members",
        "GET /loyalty/members/{memberId}/ledger", "GET /loyalty/program", "GET /preorders",
        "GET /promotions/calendar", "GET /sales", "GET /sales/{saleId}", "GET /segments",
        "GET /signup-forms", "GET /stock-alerts", "POST /campaigns",
        "POST /campaigns/{campaignId}", "POST /campaigns/{campaignId}/cancel",
        "POST /campaigns/{campaignId}/schedule", "POST /campaigns/{campaignId}/test",
        "POST /clubs", "POST /clubs/{clubId}",
        "POST /clubs/{clubId}/items", "POST /discounts/{discountId}/rules", "POST /journeys",
        "POST /journeys/{journeyId}", "POST /loyalty/members/{memberId}/adjust",
        "POST /loyalty/members/{memberId}/redeem", "POST /loyalty/program",
        "POST /promotions/calendar/reschedule", "POST /sales", "POST /sales/{saleId}/cancel",
        "POST /segments", "POST /segments-preview", "POST /segments/{segmentId}",
        "POST /signup-forms", "POST /signup-forms/{formId}", "POST /variants/{variantId}/preorder",
        // Commerce depth: subscriptions and B2B.
        "DELETE /companies/{companyId}/buyers/{buyerId}",
        "DELETE /price-lists/{priceListId}/entries/{entryId}", "GET /companies",
        "GET /companies/{companyId}/buyers", "GET /companies/{companyId}/sites", "GET /net-terms",
        "GET /price-lists", "GET /price-lists/{priceListId}/entries", "GET /quotes",
        "GET /quotes/{quoteId}", "GET /selling-plans", "GET /subscription-calendar",
        "GET /subscriptions", "GET /subscriptions/{subscriptionId}", "POST /companies",
        "POST /companies/{companyId}", "POST /companies/{companyId}/buyers",
        "POST /companies/{companyId}/sites", "POST /net-terms/{orderId}/paid", "POST /price-lists",
        "POST /price-lists/{priceListId}", "POST /price-lists/{priceListId}/entries",
        "POST /quotes", "POST /quotes/{quoteId}/move", "POST /selling-plans",
        "POST /selling-plans/{planId}", "POST /subscriptions/{subscriptionId}/cancel",
        "POST /subscriptions/{subscriptionId}/pause", "POST /subscriptions/{subscriptionId}/resume",
        "POST /subscriptions/{subscriptionId}/skip", "POST /vat-check",
        "GET /pos/labels.pdf",
        // Markets, translations, tax, fraud review and disputes.
        "GET /markets", "POST /markets", "POST /markets/{marketId}",
        "DELETE /markets/{marketId}", "GET /markets/{marketId}/prices",
        "POST /markets/{marketId}/prices", "GET /fx-rates", "POST /fx-rates", "GET /duty-rates",
        "POST /duty-rates", "DELETE /duty-rates/{dutyId}", "GET /customs-codes",
        "POST /customs-codes", "GET /translations", "POST /translations",
        "GET /tax-exemptions", "POST /tax-exemptions", "DELETE /tax-exemptions/{exemptionId}",
        "GET /tax-report", "GET /order-risks", "GET /order-risks/{orderId}",
        "POST /order-risks/{orderId}/approve", "POST /order-risks/{orderId}/cancel",
        "GET /disputes", "GET /disputes/{disputeId}", "GET /disputes/{disputeId}/evidence",
        "POST /disputes/{disputeId}/evidence", "POST /disputes/{disputeId}/submit",
        "POST /disputes/{disputeId}/accept",
        // Operations: warehouse, timelines, forecast, reports, permissions, chat, listings.
        "GET /warehouse/{locationId}/board", "POST /warehouse/{locationId}/sync",
        "POST /warehouse/move", "GET /warehouse/{locationId}/bins",
        "POST /warehouse/{locationId}/bins", "GET /warehouse/{locationId}/pick-list",
        "GET /transfers/{transferId}/timeline", "GET /purchase-orders/{orderId}/timeline",
        "GET /forecast", "GET /forecast/suggestions", "POST /forecast/reorder",
        "POST /forecast/rebalance", "POST /reports/run", "GET /reports", "POST /reports",
        "GET /reports/{reportId}", "GET /cohorts", "GET /attribution",
        "GET /permission-sets", "POST /permission-sets", "POST /permission-sets/{setId}",
        "GET /staff-permissions", "POST /staff-permissions", "GET /chats", "GET /chats/{chatId}",
        "POST /chats/{chatId}/reply", "POST /chats/{chatId}/assign", "POST /chats/{chatId}/close",
        "GET /canned-replies", "POST /canned-replies", "GET /combined-listings",
        "POST /combined-listings", "POST /imports/marketplace",
        // Online store: theme, pages, blog, menus, redirects, metafields, metaobjects.
        "GET /theme", "POST /theme", "POST /theme/publish", "GET /pages", "POST /pages",
        "POST /pages/{pageId}", "DELETE /pages/{pageId}", "GET /blog-posts", "POST /blog-posts",
        "POST /blog-posts/{postId}", "DELETE /blog-posts/{postId}", "GET /menu-links",
        "POST /menu-links", "POST /menu-links/{linkId}/move", "DELETE /menu-links/{linkId}",
        "GET /redirects", "POST /redirects", "DELETE /redirects/{redirectId}", "GET /metafields",
        "POST /metafields", "DELETE /metafields/{fieldId}", "GET /metafield-values",
        "POST /metafield-values", "GET /metaobject-types", "POST /metaobject-types",
        "GET /metaobject-types/{typeId}/objects", "POST /metaobject-types/{typeId}/objects",
        "DELETE /metaobjects/{objectId}",
        // Live marketplace sync.
        "GET /channels", "POST /channels", "POST /channels/{channelId}/remove",
        "POST /channels/{channelId}/sync", "GET /channels/{channelId}/listings",
        "POST /channels/{channelId}/listings", "GET /channels/{channelId}/log",
        // Inventory states, serials and lots.
        "GET /inventory/states", "POST /inventory/unavailable", "GET /tracking",
        "POST /tracking", "POST /tracking/receive", "GET /serials", "GET /lots",
        "POST /orders/{orderId}/serials",
        // Archiving, bulk actions, return rules, pay links and templates.
        "POST /orders/{orderId}/archive", "POST /orders/{orderId}/unarchive",
        "GET /archived-orders", "POST /bulk-orders", "GET /order-settings",
        "POST /order-settings", "POST /drafts/{orderId}/pay-link", "GET /templates",
        "POST /templates", "POST /templates/reset", "POST /templates/preview",
        // Label sheets and bulk packing slips.
        "GET /label-sheets.pdf", "GET /purchase-orders/{purchaseOrderId}/labels.pdf",
        "GET /bulk-orders/packing-slips.pdf",
    };

    /// Routes of [#ROUTES] served as documents by `DocumentPages` rather than
    /// declared by a shared API interface.
    static final String[] DOCUMENT_ROUTES = {"GET /pos/labels.pdf",
        "GET /label-sheets.pdf", "GET /purchase-orders/{purchaseOrderId}/labels.pdf",
        "GET /bulk-orders/packing-slips.pdf"};

    /// The table an id placeholder names, by the path segment before it.
    private static final String[][] TABLES = {
        {"automations", "automation"}, {"collections", "collection"},
        {"discounts", "discount"}, {"locations", "location"}, {"photos", "product_photo"},
        {"parked", "parked_cart"}, {"layaways", "layaway"},
        {"products", "product"}, {"rates", "shipping_rate"}, {"zones", "shipping_zone"},
        {"staff", "store_staff"}, {"suppliers", "supplier"}, {"tax-rates", "tax_rate"},
        {"webhooks", "webhook"}, {"customers", "customer"}, {"fulfillments", "fulfillment"},
        {"orders", "store_order"}, {"drafts", "store_order"}, {"registers", "register"},
        {"shifts", "shift"}, {"purchase-orders", "purchase_order"},
        {"returns", "return_request"}, {"reviews", "review"}, {"gift-cards", "gift_card"},
        {"transfers", "stock_transfer"}, {"webhook-deliveries", "webhook_delivery"},
        {"clubs", "club"}, {"items", "club_item"}, {"segments", "audience"},
        {"campaigns", "campaign"}, {"journeys", "journey"}, {"members", "loyalty_member"},
        {"sales", "sale_event"}, {"signup-forms", "signup_form"}, {"variants", "variant"},
        {"selling-plans", "selling_plan"}, {"subscriptions", "subscription"},
        {"companies", "company"}, {"buyers", "company_buyer"}, {"price-lists", "price_list"},
        {"entries", "price_list_entry"}, {"quotes", "trade_quote"}, {"net-terms", "net_term"},
        {"markets", "market"}, {"duty-rates", "duty_rate"},
        {"tax-exemptions", "tax_exemption"}, {"order-risks", "store_order"},
        {"disputes", "dispute"}, {"warehouse", "location"}, {"reports", "saved_report"},
        {"permission-sets", "permission_set"}, {"chats", "chat_conversation"},
        {"disputes", "dispute"}, {"pages", "content_page"}, {"blog-posts", "blog_post"},
        {"menu-links", "menu_link"}, {"redirects", "url_redirect"},
        {"metafields", "metafield_def"}, {"metaobject-types", "metaobject_type"},
        {"metaobjects", "metaobject_entry"}, {"channels", "sales_channel"},
    };

    private static final String NO_SUCH = "no-such-id";

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
    private DemoMarketing marketing;
    @Autowired
    private Subscriptions subscriptions;
    @Autowired
    private Trade trade;

    private final Map<String, String> found = new HashMap<String, String>();
    private final List<String> fake = new ArrayList<String>();

    /// An id of a row of `table` belonging to `storeId`, or one nothing has.
    private String idOf(String table, String storeId) {
        String key = table + "/" + storeId;
        String id = found.get(key);
        if (id != null) {
            return id;
        }
        id = NO_SUCH;
        try {
            List rows = db.query("SELECT id FROM " + table + " WHERE store_id = ?",
                    new Object[] {storeId});
            if (rows != null && !rows.isEmpty()) {
                Object v = ((Map) rows.get(0)).get("id");
                if (v != null) {
                    id = v.toString();
                }
            }
        } catch (Exception noStoreColumn) {
            // A table keyed some other way: the id nothing has.
        }
        if (NO_SUCH.equals(id) && !fake.contains(table)) {
            fake.add(table);
        }
        found.put(key, id);
        return id;
    }

    private static String tableFor(String segment) {
        for (int iter = 0; iter < TABLES.length; iter++) {
            if (TABLES[iter][0].equals(segment)) {
                return TABLES[iter][1];
            }
        }
        return null;
    }

    /// `route`'s path at `storeId`, with the rows of store `owner` as the nested ids.
    private String path(String route, String storeId, String owner) {
        String p = route.substring(route.indexOf(' ') + 1);
        StringBuilder out = new StringBuilder("/api/merchant/stores/").append(storeId);
        String previous = "";
        int start = 0;
        while (start < p.length()) {
            int slash = p.indexOf('/', start + 1);
            String seg = p.substring(start + 1, slash < 0 ? p.length() : slash);
            if (seg.startsWith("{")) {
                String table = tableFor(previous);
                out.append('/').append(table == null ? NO_SUCH : idOf(table, owner));
            } else {
                out.append('/').append(seg);
                previous = seg;
            }
            start = slash < 0 ? p.length() : slash;
        }
        return out.toString();
    }

    private int as(String user, String route, String path) throws Exception {
        String method = route.substring(0, route.indexOf(' '));
        MockRequestBuilder b = "GET".equals(method) ? get(path) : "PUT".equals(method) ? put(path)
                : "DELETE".equals(method) ? delete(path) : post(path);
        if ("POST".equals(method) || "PUT".equals(method)) {
            b = Rest.body(b, "{}");
        }
        return Rest.statusOf(mvc, People.asMerchant(b, user));
    }

    @Test
    void staffOfOneStoreFindNothingAtAnother() throws Exception {
        Shop a = new Shop("idor-a", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        Shop b = new Shop("idor-b", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        String orderId = b.buy(People.customer(accounts, "idor-buyer"), 1).id();
        // More of B's objects, each made the way its owner would; one that
        // does not take is simply left to the id nothing has.
        String base = "/api/merchant/stores/" + b.storeId;
        String[][] seeds = {
            {"/collections", "{\"title\":\"Idor collection\",\"published\":true}"},
            {"/suppliers", "{\"name\":\"Idor supplier\"}"},
            {"/gift-cards", "{\"initialCents\":1000,\"note\":\"idor\"}"},
            {"/orders/" + orderId + "/fulfillments", "{\"locationId\":\"" + b.locationId
                + "\",\"method\":\"SHIP\",\"lines\":[]}"},
            {"/pos/registers", "{\"name\":\"Idor till\",\"locationId\":\"" + b.locationId
                + "\"}"},
            {"/purchase-orders", "{\"supplierId\":\"\",\"locationId\":\"" + b.locationId
                + "\",\"lines\":[{\"variantId\":\"" + b.variantId + "\",\"quantity\":1,"
                + "\"costCents\":100}]}"},
        };
        for (int iter = 0; iter < seeds.length; iter++) {
            Rest.statusOf(mvc, People.asMerchant(Rest.body(post(base + seeds[iter][0]),
                    seeds[iter][1]), b.owner));
        }
        // A parked cart and a layaway of B's, written straight to the tables:
        // both need a signed-in register to make the usual way, and the test
        // only needs rows whose ids belong to B.
        String bRegister = idOf("register", b.storeId);
        long now = System.currentTimeMillis();
        db.execute("INSERT INTO parked_cart (id, store_id, register_id, client_id, label, "
                + "customer_id, staff_username, status, note, created_at, resumed_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {"idorparkedb",
                    b.storeId, bRegister, "idor-parked-b", "Idor", "", b.owner, "PARKED", "", now,
                    0L});
        db.execute("INSERT INTO layaway (id, store_id, order_id, register_id, customer_id, status, "
                + "total_cents, paid_cents, cancel_fee_bp, refunded_cents, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {"idorlayawayb",
                    b.storeId, orderId, bRegister, "", "OPEN", 1000L, 200L, 0L, 0L, now, now});
        // B's loyalty programme, club, segments, campaigns, form and journey.
        marketing.seed(b.storeId);
        new CommerceSeed(b, "idorcd", accounts, mvc, subscriptions, trade);
        GlobalSeed.seed(b, orderId, db);
        // A manager of A: the merchant role and a place on A's staff, not the owner.
        String manager = People.customer(accounts, "idor-manager");
        Rest.json(mvc, People.asMerchant(Rest.body(post("/api/merchant/stores/" + a.storeId
                + "/staff"), "{\"username\":\"" + manager + "\",\"role\":\"MANAGER\",\"pin\":\"\"}"),
                a.owner));
        // Not a false pass: the same caller reaches its own store.
        assertEquals(200, as(manager, "GET ", "/api/merchant/stores/" + a.storeId));
        assertEquals(200, as(manager, "GET /products", "/api/merchant/stores/" + a.storeId
                + "/products"));

        List<String> leaks = new ArrayList<String>();
        for (int iter = 0; iter < ROUTES.length; iter++) {
            String route = ROUTES[iter];
            int status = as(manager, route, path(route, b.storeId, b.storeId));
            if (status != 404) {
                leaks.add(route + " at B answered " + status);
            }
            if (route.indexOf('}') > 0) {
                // B's object under A's own store: never found, never changed.
                int mixed = as(manager, route, path(route, a.storeId, b.storeId));
                if (mixed >= 200 && mixed < 300) {
                    leaks.add(route + " at A with B's ids answered " + mixed);
                }
            }
        }
        assertTrue(leaks.isEmpty(), leaks.toString());
        System.out.println("MerchantIdorTest: " + ROUTES.length + " routes; no row of store B in "
                + fake);
    }

    /// A signed-in shopper with no merchant role is refused every route,
    /// and a guest is asked to sign in, before any store is looked at.
    @Test
    void shoppersAndGuestsReachNoMerchantRoute() throws Exception {
        Shop a = new Shop("idor-shopper", 1, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        marketing.seed(a.storeId);
        String shopper = People.customer(accounts, "idor-plain-shopper");
        List<String> leaks = new ArrayList<String>();
        for (int iter = 0; iter < ROUTES.length; iter++) {
            String route = ROUTES[iter];
            String method = route.substring(0, route.indexOf(' '));
            String path = path(route, a.storeId, a.storeId);
            MockRequestBuilder b = "GET".equals(method) ? get(path) : "PUT".equals(method) ? put(path)
                    : "DELETE".equals(method) ? delete(path) : post(path);
            if ("POST".equals(method) || "PUT".equals(method)) {
                b = Rest.body(b, "{}");
            }
            int signedIn = Rest.statusOf(mvc, People.asCustomer(b, shopper));
            if (signedIn != 403) {
                leaks.add(route + " answered a shopper " + signedIn);
            }
        }
        // A guest, on a sample of reads and writes.
        String[] sample = {"GET /loyalty/program", "GET /campaigns", "POST /clubs", "GET /sales",
            "GET /journeys", "GET /stock-alerts", "POST /segments"};
        for (int iter = 0; iter < sample.length; iter++) {
            String route = sample[iter];
            String path = path(route, a.storeId, a.storeId);
            int guest = Rest.statusOf(mvc, route.startsWith("GET") ? get(path) : Rest.body(post(path), "{}"));
            if (guest != 401) {
                leaks.add(route + " answered a guest " + guest);
            }
        }
        assertTrue(leaks.isEmpty(), leaks.toString());
    }

    @Test
    void theListIsEveryRouteTheInterfacesDeclare() throws Exception {
        File dir = new File("../shared/src/main/java/"
                + CompanyBuyerDto.class.getPackage().getName().replace('.', '/'));
        String[] names = dir.list();
        if (names == null) {
            // A compiled test binary does not run beside the sources.
            return;
        }
        String marker = "/api/merchant/stores/{storeId}";
        List<String> declared = new ArrayList<String>();
        for (int iter = 0; iter < names.length; iter++) {
            if (!names[iter].endsWith(".java")) {
                continue;
            }
            String text = read(new File(dir, names[iter]));
            int at = text.indexOf("(\"" + marker);
            while (at >= 0) {
                int open = text.lastIndexOf('@', at);
                String method = text.substring(open + 1, at);
                int end = text.indexOf('"', at + 2);
                String route = method + " " + text.substring(at + 2 + marker.length(), end);
                if (!declared.contains(route)) {
                    declared.add(route);
                }
                at = text.indexOf("(\"" + marker, end);
            }
        }
        List<String> mine = new ArrayList<String>();
        for (int iter = 0; iter < ROUTES.length; iter++) {
            if (!java.util.Arrays.asList(DOCUMENT_ROUTES).contains(ROUTES[iter])) {
                mine.add(ROUTES[iter]);
            }
        }
        List<String> missing = new ArrayList<String>();
        for (int iter = 0; iter < declared.size(); iter++) {
            if (!mine.contains(declared.get(iter))) {
                missing.add(declared.get(iter));
            }
        }
        assertTrue(missing.isEmpty(), "Add to MerchantIdorTest.ROUTES: " + missing);
        assertEquals(declared.size(), ROUTES.length - DOCUMENT_ROUTES.length,
                "a route listed that no interface declares");
    }

    private static String read(File f) throws Exception {
        InputStream in = new FileInputStream(f);
        try {
            StringBuilder out = new StringBuilder();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.append(new String(buf, 0, n, "UTF-8"));
            }
            return out.toString();
        } finally {
            in.close();
        }
    }
}
