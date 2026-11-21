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

import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.image.PngCodec;
import com.codename1.backend.image.RasterImage;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.account.DemoAccounts;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.ErrorPages;
import com.codenameone.examples.bazaar.web.security.Uploads;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The web app's error pages say nothing about the request, and an uploaded
/// image is kept only as the server re-encoded it.
@BackendTest
class ErrorPagesTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;

    private static void plain(MockResponse r, int status, String marker) throws Exception {
        assertEquals(status, r.getStatus());
        assertTrue(r.getContentType().startsWith("text/html"), r.getContentType());
        String html = r.getContentAsString();
        assertTrue(html.contains(marker), html);
        assertFalse(html.contains("<script"), html);
        assertFalse(html.contains("Exception"), html);
    }

    @Test
    void aMissingPageNamesNothingOfTheRequest() throws Exception {
        MockResponse r = mvc.perform(get("/s/no-such-shop-x1y2")).andReturn().getResponse();
        plain(r, 404, "We could not find that page");
        assertFalse(r.getContentAsString().contains("no-such-shop-x1y2"));
    }

    @Test
    void aMissingCsrfTokenIsThe403Page() throws Exception {
        Browser b = new Browser(mvc);
        b.signIn(DemoAccounts.CUSTOMER, DemoAccounts.PASSWORD);
        MockResponse r = b.postWithoutToken("/account/addresses", "name", "x");
        plain(r, 403, "You cannot do that here");
        assertEquals("no-store", r.getHeader("Cache-Control"));
        assertFalse(r.getContentAsString().toLowerCase().contains("csrf"));
    }

    @Test
    void aFilterAnswerIsSwappedForThePageButTheApiKeepsItsOwn() throws Exception {
        // What the rate-limit filter answers: plain text with Retry-After.
        com.codename1.backend.security.SecurityFilter f = ErrorPages.filter();
        com.codename1.backend.HttpServer.Response r = f.doFilter(
                com.codename1.backend.HttpServer.Request.of("POST", "/auth/sms/send",
                        new java.util.HashMap<String, String>(), new byte[0]),
                req -> com.codename1.backend.HttpServer.Response.text(429, "Too Many Requests")
                        .header("Retry-After", "7"));
        assertEquals(429, r.getStatus());
        String html = new String(r.getBody(), "UTF-8");
        assertTrue(html.contains("Slow down"), html);
        // A failure behind the filter is the 500 page, its message unseen.
        r = f.doFilter(com.codename1.backend.HttpServer.Request.of("GET", "/account",
                new java.util.HashMap<String, String>(), new byte[0]), req -> {
                    throw new IllegalStateException("secret-db-detail");
                });
        assertEquals(500, r.getStatus());
        assertFalse(new String(r.getBody(), "UTF-8").contains("secret-db-detail"));
        // The API's JSON is left alone.
        r = f.doFilter(com.codename1.backend.HttpServer.Request.of("GET", "/api/x",
                new java.util.HashMap<String, String>(), new byte[0]),
                req -> com.codename1.backend.HttpServer.Response.text(429, "{}"));
        assertEquals("{}", new String(r.getBody(), "UTF-8"));
    }

    @Test
    void aRefusalDoesNotRepeatItsReason() throws Exception {
        MockResponse unused = null;
        String html = new String(new ErrorPages().refused(
                new ResponseStatusException(409, "secret-internal-reason")).getBody(), "UTF-8");
        assertFalse(html.contains("secret-internal-reason"));
        assertEquals(500, ErrorPages.response(503).getStatus());
        assertTrue(new String(ErrorPages.response(500).getBody(), "UTF-8")
                .contains("Something went wrong"));
        assertEquals(null, unused);
    }

    private static byte[] png(int w, int h) {
        RasterImage img = new RasterImage(w, h);
        img.fill(0xff336699);
        return PngCodec.encode(img);
    }

    private static int status(Runnable r) {
        try {
            r.run();
        } catch (ResponseStatusException e) {
            return e.getStatus();
        }
        return 200;
    }

    @Test
    void anUploadIsSniffedCappedAndReEncoded() throws Exception {
        byte[] good = png(8, 6);
        // A script appended after the image does not survive.
        byte[] tail = "<script>alert(1)</script>".getBytes("UTF-8");
        byte[] polyglot = new byte[good.length + tail.length];
        System.arraycopy(good, 0, polyglot, 0, good.length);
        System.arraycopy(tail, 0, polyglot, good.length, tail.length);
        Uploads.Image clean = Uploads.image(polyglot, "image/png", 1 << 20);
        assertEquals("image/png", clean.contentType);
        assertEquals(8, clean.width);
        assertFalse(new String(clean.bytes, "ISO-8859-1").contains("<script>"));

        // The bytes decide the type, and must agree with what was declared.
        assertEquals(400, status(() -> Uploads.image(good, "image/jpeg", 1 << 20)));
        assertEquals(415, status(() -> Uploads.image("GIF89a........".getBytes(), null, 1 << 20)));
        assertEquals(415, status(() -> Uploads.image("<html>hello</html>".getBytes(), "image/png",
                1 << 20)));
        // Too many bytes, or a header claiming a huge canvas, before decoding.
        assertEquals(413, status(() -> Uploads.image(good, null, 16)));
        byte[] huge = good.clone();
        // IHDR width at offset 16: claim 20000 pixels wide.
        huge[16] = 0;
        huge[17] = 0;
        huge[18] = 0x4e;
        huge[19] = 0x20;
        assertEquals(413, status(() -> Uploads.image(huge, null, 1 << 20)));
        // A header with a body that does not decode.
        byte[] cut = new byte[40];
        System.arraycopy(good, 0, cut, 0, 40);
        assertEquals(400, status(() -> Uploads.image(cut, null, 1 << 20)));
    }

    @Test
    void thePhotoEndpointRefusesWhatIsNotAnImage() throws Exception {
        Shops shop = Shops.open(accounts, stores, "upload-shop");
        Map product = shop.product(mvc, "Upload Mug", "ACTIVE", "Mugs", "M", "", 1000);
        String path = shop.base() + "/products/" + product.get("id") + "/photos";
        String text = Base64.encode("<html><script>x</script></html>".getBytes("UTF-8"));
        int refused = Rest.statusOf(mvc, People.asMerchant(Rest.body(post(path),
                "{\"contentType\":\"image/png\",\"data\":\"" + text + "\"}"), shop.owner));
        assertEquals(415, refused);
        String ok = Base64.encode(png(4, 4));
        int accepted = Rest.statusOf(mvc, People.asMerchant(Rest.body(post(path),
                "{\"contentType\":\"image/png\",\"data\":\"" + ok + "\"}"), shop.owner));
        if (accepted != 200) {
            fail("a real PNG answered " + accepted);
        }
    }
}
