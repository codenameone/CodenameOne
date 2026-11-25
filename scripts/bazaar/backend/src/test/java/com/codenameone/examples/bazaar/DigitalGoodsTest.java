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
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.DownloadGrantDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.LicenseKeysDto;
import com.codenameone.examples.bazaar.api.MediaUploadDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.catalog.CatalogFiles;
import com.codenameone.examples.bazaar.catalog.Downloads;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Digital goods end to end: an all-digital checkout with no address, the
/// signed download links a payment issues and every way one stops working,
/// licence keys, and a product's video and 3D model.
@BackendTest
class DigitalGoodsTest {
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
    private Catalog catalog;
    @Autowired
    private CatalogFiles files;
    @Autowired
    private Downloads downloads;
    @Autowired
    private Orders orders;

    private final MailSender mail = MailSender.current();

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    /// A $12 "Field guide" e-book with its PDF; answers the variant id.
    private String ebook(Shop s, int maxDownloads) throws Exception {
        String productId = Ids.next();
        db.execute("INSERT INTO product (id, store_id, handle, title, description, vendor, "
                + "product_type, tags, status, seo_title, seo_description, photo_id, rating_total, "
                + "rating_count, created_at, updated_at) VALUES (?, ?, ?, 'Field guide', '', '', '', "
                + "'', 'ACTIVE', '', '', '', 0, 0, 0, 0)",
                new Object[] {productId, s.storeId, s.storeId + "-guide"});
        String variantId = Ids.next();
        db.execute("INSERT INTO variant (id, product_id, store_id, title, option1, option2, "
                + "option3, sku, barcode, price_cents, compare_at_cents, cost_cents, weight_grams, "
                + "taxable, track_inventory, allow_backorder, position, digital) VALUES "
                + "(?, ?, ?, 'PDF', '', '', '', ?, '', 1200, 0, 0, 0, 1, 0, 0, 0, 1)",
                new Object[] {variantId, productId, s.storeId, s.sku + "-PDF"});
        String fileId = files.store(s.storeId, CatalogFiles.DOWNLOAD, "field guide.pdf",
                "application/pdf", "%PDF-1.4 guide".getBytes("UTF-8"));
        downloads.saveAsset(s.storeId, variantId, fileId, maxDownloads, 30);
        return variantId;
    }

    /// Buys `qty` of a digital variant with a card, giving no address.
    private OrderDto buyDigital(Shop s, String user, String variantId, int qty) throws Exception {
        carts.add(s.storeId, user, null, new CartLineChangeDto(variantId, qty));
        CartDto c = carts.estimate(s.storeId, user, null, new EstimateDto(null, null));
        assertFalse(c.shippingRequired(), "nothing in the cart ships");
        assertTrue(c.shippingOptions().isEmpty());
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        return checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(null, null,
                (String) card.get("id"), user, "", null, 0, null));
    }

    private MockResponse fetch(String url) throws Exception {
        String path = url.substring(url.indexOf("/download/"));
        return mvc.perform(get(path)).andReturn().getResponse();
    }

    private Mail mailTo(String to, String text) throws Exception {
        for (Mail m : mail.sent()) {
            if (m.getTo().contains(to) && m.getText().contains(text)) {
                return m;
            }
        }
        return null;
    }

    @Test
    void anAllDigitalOrderChecksOutWithoutAnAddressAndIsDeliveredOnPayment() throws Exception {
        Shop s = shop("dg-buy");
        String guide = ebook(s, 5);
        String user = People.customer(accounts, "dg-buyer");
        OrderDto o = buyDigital(s, user, guide, 1);
        assertEquals(0, o.shippingCents());
        Map row = db.queryOne("SELECT financial, fulfilment FROM store_order WHERE id = ?",
                new Object[] {o.id()});
        assertEquals("PAID", row.get("financial"), "delivering the download took the payment");
        List<DownloadGrantDto> gs = downloads.mine(user, o.id());
        assertEquals(1, gs.size());
        DownloadGrantDto g = gs.get(0);
        assertTrue(g.url().contains("/download/" + g.id() + "."), g.url());
        assertEquals("field guide.pdf", g.fileName());
        Mail confirmation = mailTo(user, "Field guide");
        assertTrue(confirmation != null, "a confirmation went out");
        assertTrue(confirmation.getHtml().contains("/download/" + g.id() + "."),
                "the confirmation carries the link");
        MockResponse r = fetch(g.url());
        assertEquals(200, r.getStatus());
        assertEquals("%PDF-1.4 guide", new String(r.getContentAsByteArray(), "UTF-8"));
        assertTrue(r.getHeader("Content-Disposition").startsWith("attachment; filename=\""),
                r.getHeader("Content-Disposition"));
        assertEquals("no-store", r.getHeader("Cache-Control"));
        assertEquals(1, downloads.mine(user, o.id()).get(0).downloads());
        // The order page shows the link too.
        assertTrue(downloads.forOrder(o.id()).get(0).url().length() > 0);
        // Issuing again is a no-op.
        assertEquals(0, downloads.issue(o.id()));
    }

    @Test
    void aLinkStopsAfterItsDownloadCount() throws Exception {
        Shop s = shop("dg-count");
        String guide = ebook(s, 2);
        String user = People.customer(accounts, "dg-count-buyer");
        OrderDto o = buyDigital(s, user, guide, 1);
        String url = downloads.mine(user, o.id()).get(0).url();
        assertEquals(200, fetch(url).getStatus());
        assertEquals(200, fetch(url).getStatus());
        assertEquals(404, fetch(url).getStatus());
        assertEquals("", downloads.mine(user, o.id()).get(0).url(), "a used-up grant has no link");
    }

    @Test
    void anExpiredLinkIsNotFound() throws Exception {
        Shop s = shop("dg-expire");
        String guide = ebook(s, 5);
        String user = People.customer(accounts, "dg-expire-buyer");
        OrderDto o = buyDigital(s, user, guide, 1);
        DownloadGrantDto g = downloads.mine(user, o.id()).get(0);
        db.execute("UPDATE download_grant SET expires_at = ? WHERE id = ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 1000), g.id()});
        assertEquals(404, fetch(g.url()).getStatus());
        // A token minted for a past expiry is refused before the row is read.
        String token = g.url().substring(g.url().indexOf("/download/") + 10);
        String past = g.id() + "." + (System.currentTimeMillis() - 1000)
                + token.substring(token.lastIndexOf('.'));
        assertEquals(404, mvc.perform(get("/download/" + past)).andReturn().getResponse()
                .getStatus());
    }

    @Test
    void aForgedOrForeignTokenIsNotFound() throws Exception {
        Shop s = shop("dg-forge");
        String guide = ebook(s, 5);
        String a = People.customer(accounts, "dg-forge-a");
        String b = People.customer(accounts, "dg-forge-b");
        OrderDto oa = buyDigital(s, a, guide, 1);
        OrderDto ob = buyDigital(s, b, guide, 1);
        DownloadGrantDto ga = downloads.mine(a, oa.id()).get(0);
        DownloadGrantDto gb = downloads.mine(b, ob.id()).get(0);
        String ta = ga.url().substring(ga.url().indexOf("/download/") + 10);
        String tb = gb.url().substring(gb.url().indexOf("/download/") + 10);
        String sigA = ta.substring(ta.lastIndexOf('.') + 1);
        String flipped = ta.substring(0, ta.length() - 1) + (ta.endsWith("0") ? "1" : "0");
        // B's id and expiry under A's signature: each grant has its own secret.
        String foreign = tb.substring(0, tb.lastIndexOf('.') + 1) + sigA;
        for (String t : Arrays.asList(flipped, foreign, "nonsense", Ids.next() + ".1.ab",
                ta + "x", "")) {
            int status = mvc.perform(get("/download/" + t)).andReturn().getResponse().getStatus();
            assertEquals(404, status, t);
        }
        // Another shopper cannot list the order's downloads.
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> downloads.mine(b, oa.id()));
        assertEquals(404, e.getStatus());
        assertEquals(0, downloads.mine(a, oa.id()).get(0).downloads(), "nothing was counted");
    }

    @Test
    void aFullRefundAndTheOwnerRevokeLinks() throws Exception {
        Shop s = shop("dg-refund");
        String guide = ebook(s, 5);
        String user = People.customer(accounts, "dg-refund-buyer");
        OrderDto o = buyDigital(s, user, guide, 1);
        DownloadGrantDto g = downloads.mine(user, o.id()).get(0);
        orders.refund(s.owner, s.storeId, o.id(), new RefundRequestDto(
                Arrays.asList(new RefundLineDto(g.lineId(), 1)), 0, false, "changed mind"));
        assertEquals(404, fetch(g.url()).getStatus());
        assertTrue(downloads.ofOrder(s.owner, s.storeId, o.id()).get(0).revoked());

        String other = People.customer(accounts, "dg-revoke-buyer");
        OrderDto o2 = buyDigital(s, other, guide, 1);
        DownloadGrantDto g2 = downloads.mine(other, o2.id()).get(0);
        assertTrue(downloads.revoke(s.owner, s.storeId, g2.id()).revoked());
        assertEquals(404, fetch(g2.url()).getStatus());
        // A stranger's store cannot revoke it.
        Shop t = shop("dg-refund-other");
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> downloads.revoke(t.owner, t.storeId, g2.id())).getStatus());
    }

    @Test
    void eachUnitGetsItsOwnLicenceKey() throws Exception {
        Shop s = shop("dg-keys");
        String guide = ebook(s, 5);
        downloads.addKeys(s.owner, s.storeId, guide, new LicenseKeysDto("K-1\nK-2\n\nK-2\n K-3 "));
        assertEquals(3, downloads.asset(s.owner, s.storeId, guide).keysFree());
        String user = People.customer(accounts, "dg-keys-buyer");
        OrderDto o = buyDigital(s, user, guide, 2);
        List<String> keys = downloads.mine(user, o.id()).get(0).licenseKeys();
        assertEquals(Arrays.asList("K-1", "K-2"), keys);
        assertEquals(1, downloads.asset(s.owner, s.storeId, guide).keysFree());
        Mail m = mailTo(user, "Field guide");
        assertTrue(m != null && m.getHtml().contains("K-2"), "the keys are in the mail");
    }

    @Test
    void concurrentSalesNeverShareALicenceKey() throws Exception {
        Shop s = shop("dg-race");
        String guide = ebook(s, 5);
        StringBuilder pasted = new StringBuilder();
        for (int iter = 0; iter < 20; iter++) {
            pasted.append("RACE-").append(iter).append('\n');
        }
        downloads.addKeys(s.owner, s.storeId, guide, new LicenseKeysDto(pasted.toString()));
        final AtomicInteger taken = new AtomicInteger();
        List<Thread> threads = new ArrayList<Thread>();
        for (int iter = 0; iter < 8; iter++) {
            final String line = "race-line-" + iter;
            Thread th = new Thread(() -> {
                for (int attempt = 0; attempt < 20; attempt++) {
                    try {
                        taken.addAndGet(downloads.claimKeys(s.storeId, guide, "race-order", line,
                                3));
                        return;
                    } catch (Exception busy) {
                        // A locked database under the race: try again.
                    }
                }
            });
            threads.add(th);
        }
        for (Thread th : threads) {
            th.start();
        }
        for (Thread th : threads) {
            th.join();
        }
        List rows = db.query("SELECT key_text, line_id FROM license_key WHERE variant_id = ? "
                + "AND order_id <> ''", new Object[] {guide});
        assertEquals(taken.get(), rows.size());
        assertTrue(taken.get() <= 20);
        Set<String> seen = new HashSet<String>();
        for (Object o : rows) {
            assertTrue(seen.add((String) ((Map) o).get("key_text")), "a key went to two lines");
        }
    }

    @Test
    void aMixedCartStillShipsItsPhysicalLines() throws Exception {
        Shop s = shop("dg-mixed");
        String guide = ebook(s, 5);
        String user = People.customer(accounts, "dg-mixed-buyer");
        carts.add(s.storeId, user, null, new CartLineChangeDto(guide, 1));
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        CartDto c = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        assertTrue(c.shippingRequired());
        assertFalse(c.shippingOptions().isEmpty());
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        assertThrows(ResponseStatusException.class, () -> checkouts.place(user, s.storeId,
                Ids.next(), new PlaceOrderDto(null, null, (String) card.get("id"),
                    user, "", null, 0, null)), "a parcel needs an address");
        OrderDto o = checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(Shop.NY,
                c.shippingOptions().get(0).rateId(), (String) card.get("id"),
                user, "", null, 0, null));
        assertEquals(1, downloads.mine(user, o.id()).size());
        assertEquals(9, inventory.available(s.variantId, s.locationId), "the mug was taken");
    }

    // ================================================================= media

    private static final byte[] MP4 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm',
        0, 0, 2, 0, 'i', 's', 'o', 'm', 'm', 'p', '4', '1'};

    @Test
    void uploadsAreCheckedByTheirBytesAndSize() throws Exception {
        Shop s = shop("dg-upload");
        String b64 = Base64.getEncoder().encodeToString(MP4);
        ProductDto p = catalog.addMedia(s.owner, s.storeId, s.productId,
                new MediaUploadDto("VIDEO", "clip.mp4", "video/mp4", b64, null));
        assertEquals("VIDEO", p.photos().get(p.photos().size() - 1).kind());
        assertEquals(415, assertThrows(ResponseStatusException.class,
                () -> catalog.addMedia(s.owner, s.storeId, s.productId, new MediaUploadDto("VIDEO",
                    "fake.mp4", "video/mp4", Base64.getEncoder().encodeToString(
                        "<html>".getBytes("UTF-8")), null))).getStatus());
        assertEquals(415, assertThrows(ResponseStatusException.class,
                () -> catalog.addMedia(s.owner, s.storeId, s.productId, new MediaUploadDto("MODEL",
                    "clip.glb", "model/gltf-binary", b64, null))).getStatus(),
                "an MP4 is not a model");
        assertEquals(413, assertThrows(ResponseStatusException.class,
                () -> files.store(s.storeId, CatalogFiles.VIDEO, "big.mp4", "video/mp4",
                    new byte[files.maxBytes() + 1])).getStatus());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> catalog.addMedia(s.owner, s.storeId, s.productId, new MediaUploadDto("VIDEO",
                    null, null, null, "http://example.com/v.mp4"))).getStatus());
        // A linked video is fine over https.
        p = catalog.addMedia(s.owner, s.storeId, s.productId,
                new MediaUploadDto("VIDEO", null, null, null, "https://video.example.com/v.mp4"));
        assertEquals("https://video.example.com/v.mp4",
                p.photos().get(p.photos().size() - 1).url());
    }

    @Test
    void aVideoIsServedInRangesAndPlaysOnTheProductPage() throws Exception {
        Shop s = shop("dg-video");
        ProductDto p = catalog.addMedia(s.owner, s.storeId, s.productId, new MediaUploadDto(
                "VIDEO", "clip.mp4", "video/mp4", Base64.getEncoder().encodeToString(MP4), null));
        String url = p.photos().get(p.photos().size() - 1).url();
        assertTrue(url.startsWith("/media/file/"), url);
        MockResponse whole = mvc.perform(get(url)).andReturn().getResponse();
        assertEquals(200, whole.getStatus());
        String type = whole.getHeader("Content-Type");
        assertEquals("video/mp4", type.indexOf(';') < 0 ? type : type.substring(0, type.indexOf(';')));
        assertEquals("bytes", whole.getHeader("Accept-Ranges"));
        MockResponse part = mvc.perform(get(url).header("Range", "bytes=4-7")).andReturn()
                .getResponse();
        assertEquals(206, part.getStatus());
        assertEquals("bytes 4-7/" + MP4.length, part.getHeader("Content-Range"));
        assertEquals("ftyp", new String(part.getContentAsByteArray(), "US-ASCII"));
        assertEquals(416, mvc.perform(get(url).header("Range", "bytes=999-")).andReturn()
                .getResponse().getStatus());
        // A download is never public, whatever its id.
        String pdf = files.store(s.storeId, CatalogFiles.DOWNLOAD, "a.pdf", "application/pdf",
                new byte[] {1, 2, 3});
        assertEquals(404, mvc.perform(get("/media/file/" + pdf)).andReturn().getResponse()
                .getStatus());

        catalog.addMedia(s.owner, s.storeId, s.productId, new MediaUploadDto("MODEL", "chair.glb",
                "model/gltf-binary", Base64.getEncoder().encodeToString(
                    new byte[] {'g', 'l', 'T', 'F', 2, 0, 0, 0, 12, 0, 0, 0}), null));
        MockResponse page = mvc.perform(get("/s/dg-video/products/" + productHandle(s)))
                .andReturn().getResponse();
        String html = page.getContentAsString();
        assertTrue(html.contains("<video") && html.contains("controls"), html);
        assertTrue(html.contains("src=\"" + url + "\""), html);
        assertTrue(html.contains("View the 3D model"), html);
        assertTrue(page.getHeader("Content-Security-Policy").contains("media-src 'self'"));
        assertFalse(html.contains("<script src=\"http"), "no third-party viewer");
    }

    @Test
    void theStorefrontSaysADownloadShipsNothing() throws Exception {
        Shop s = shop("dg-page");
        ebook(s, 5);
        String html = mvc.perform(get("/s/dg-page/products/" + s.storeId + "-guide")).andReturn()
                .getResponse().getContentAsString();
        assertTrue(html.contains("Digital download"), html);
    }

    @Test
    void movingAVideoFirstKeepsAPictureAsTheMainPhoto() throws Exception {
        Shop s = shop("dg-move");
        String photo = Ids.next();
        db.execute("INSERT INTO product_photo (id, product_id, photo_id, alt, position, kind, url) "
                + "VALUES (?, ?, ?, '', 0, 'IMAGE', '')", new Object[] {Ids.next(), s.productId,
                    photo});
        db.execute("UPDATE product SET photo_id = ? WHERE id = ?", new Object[] {photo,
            s.productId});
        ProductDto p = catalog.addMedia(s.owner, s.storeId, s.productId, new MediaUploadDto(
                "VIDEO", null, null, null, "https://video.example.com/v.mp4"));
        String video = p.photos().get(p.photos().size() - 1).id();
        p = catalog.moveMedia(s.owner, s.storeId, s.productId, video, true);
        assertEquals(video, p.photos().get(0).id());
        assertEquals(photo, p.photoId(), "the main photo is still the picture");
    }

    private String productHandle(Shop s) throws Exception {
        return (String) db.queryOne("SELECT handle FROM product WHERE id = ?",
                new Object[] {s.productId}).get("handle");
    }
}
