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
package com.codenameone.examples.bazaar.marketing;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Review;
import com.codenameone.examples.bazaar.domain.ReviewRequest;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.ReviewRepository;
import com.codenameone.examples.bazaar.shopper.ReviewEndpoint;
import com.codenameone.examples.bazaar.web.Storefront;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// The page a review request's one-click link opens: every product of the
/// order with a rating and a few words, no sign-in needed.
///
/// The link is signed with the store's secret, so it speaks only for the
/// order it was mailed about. Opening it (a GET, which mail scanners
/// prefetch) only shows the forms; each form posts back to the same signed
/// link, so nothing is written until the shopper presses the button. Reviews
/// written here are verified purchases and wait for the store's moderation
/// like every other ([ReviewEndpoint#PENDING]); a product already reviewed
/// by the shopper shows as done.
@Controller
public class ReviewLinks {
    /// Where the links point; the request id follows.
    public static final String PATH = "/review/";

    private final MarketingMails automations;
    private final OrderLineRepository lines;
    private final ProductRepository products;
    private final ReviewRepository reviews;

    public ReviewLinks(MarketingMails automations, OrderLineRepository lines,
            ProductRepository products, ReviewRepository reviews) {
        this.automations = automations;
        this.lines = lines;
        this.products = products;
        this.reviews = reviews;
    }

    @GetMapping("/review/{requestId}")
    public HttpServer.Response show(@PathVariable("requestId") String requestId,
            HttpServer.Request request) throws IOException {
        ReviewRequest r = automations.verified(requestId, param(request, "s"));
        if (r == null) {
            return MarketingLinks.page(404, "That review link is not valid.");
        }
        return MarketingLinks.page(200, forms(r, param(request, "s"), ""));
    }

    @PostMapping("/review/{requestId}")
    public HttpServer.Response submit(@PathVariable("requestId") String requestId,
            HttpServer.Request request) throws IOException {
        Storefront.sameSite(request);
        String signature = param(request, "s");
        ReviewRequest r = automations.verified(requestId, signature);
        if (r == null) {
            return MarketingLinks.page(404, "That review link is not valid.");
        }
        String note = save(r, param(request, "product"), param(request, "rating"),
                param(request, "title"), param(request, "body"));
        return MarketingLinks.page(200, forms(r, signature, note));
    }

    /// Writes one review from the request's link; answers what to tell the
    /// shopper.
    @Transactional(rollbackFor = IOException.class)
    public String save(ReviewRequest r, String productId, String rating, String title,
            String body) throws IOException {
        if (!inOrder(r, productId)) {
            return "That product is not in this order.";
        }
        int stars = rating != null && rating.length() == 1 && rating.charAt(0) >= '1'
                && rating.charAt(0) <= '5' ? rating.charAt(0) - '0' : 0;
        if (stars == 0) {
            return "Choose one to five stars.";
        }
        String who = reviewer(r);
        if (reviews.findByProductIdAndUsername(productId, who) != null) {
            return "You already reviewed that product.";
        }
        Product p = products.findById(productId);
        Review w = new Review();
        w.setId(Ids.next());
        w.setStoreId(r.getStoreId());
        w.setProductId(productId);
        w.setUsername(who);
        int at = r.getEmail().indexOf('@');
        w.setAuthorName(Codes.cut(at > 0 ? r.getEmail().substring(0, at) : "Shopper", 80));
        w.setRating(stars);
        w.setTitle(Codes.cut(title == null ? "" : title.trim(), 120));
        w.setBody(Codes.cut(body == null ? "" : body.trim(), 2000));
        w.setStatus(ReviewEndpoint.PENDING);
        w.setVerified(true);
        w.setCreatedAt(Instant.now());
        reviews.insert(w);
        return "Thank you: your review of " + p.getTitle() + " is with the store.";
    }

    /// Who the review is by: the account that ordered, or the order's address
    /// for a guest order (one review per product either way).
    static String reviewer(ReviewRequest r) {
        return r.getUsername().length() > 0 ? r.getUsername() : Tokens.mailbox(r.getEmail());
    }

    private boolean inOrder(ReviewRequest r, String productId) {
        if (productId == null || productId.length() == 0) {
            return false;
        }
        List<OrderLine> ls = lines.findByOrderId(r.getOrderId());
        for (int iter = 0; iter < ls.size(); iter++) {
            if (productId.equals(ls.get(iter).getProductId())) {
                return true;
            }
        }
        return false;
    }

    private String forms(ReviewRequest r, String signature, String note) {
        StringBuilder out = new StringBuilder();
        if (note.length() > 0) {
            out.append("<p role=\"status\"><strong>").append(esc(note)).append("</strong></p>");
        }
        out.append("<h1>How did we do?</h1>");
        List<OrderLine> ls = lines.findByOrderId(r.getOrderId());
        List<String> seen = new ArrayList<String>();
        String who = reviewer(r);
        String action = PATH + esc(r.getId()) + "?s=" + esc(signature);
        for (int iter = 0; iter < ls.size(); iter++) {
            String id = ls.get(iter).getProductId();
            if (seen.contains(id)) {
                continue;
            }
            seen.add(id);
            Product p = products.findById(id);
            if (p == null) {
                continue;
            }
            out.append("<h2>").append(esc(p.getTitle())).append("</h2>");
            if (reviews.findByProductIdAndUsername(id, who) != null) {
                out.append("<p>Reviewed. Thank you.</p>");
                continue;
            }
            out.append("<form method=\"post\" action=\"").append(action).append("\">")
                    .append("<input type=\"hidden\" name=\"s\" value=\"").append(esc(signature))
                    .append("\"><input type=\"hidden\" name=\"product\" value=\"")
                    .append(esc(id)).append("\"><p><label>Stars <select name=\"rating\" required>")
                    .append("<option value=\"5\">5</option><option value=\"4\">4</option>")
                    .append("<option value=\"3\">3</option><option value=\"2\">2</option>")
                    .append("<option value=\"1\">1</option></select></label></p>")
                    .append("<p><label>Title<br><input name=\"title\" maxlength=\"120\"></label></p>")
                    .append("<p><label>Review<br><textarea name=\"body\" rows=\"4\" cols=\"40\" ")
                    .append("maxlength=\"2000\"></textarea></label></p>")
                    .append("<p><button type=\"submit\">Send review</button></p></form>");
        }
        return out.toString();
    }

    static String esc(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            switch (c) {
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '&': out.append("&amp;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }

    private static String param(HttpServer.Request request, String name) {
        try {
            String v = request.param(name);
            return v == null ? "" : v;
        } catch (RuntimeException malformed) {
            return "";
        }
    }
}
