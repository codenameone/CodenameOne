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
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.ExceptionHandler;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.ModelAttribute;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.mvc.Model;

import java.io.IOException;

/// The server-rendered storefront: the pages search engines index and a
/// shared link opens, as compiled HTML views (`src/main/resources/templates`).
/// The cart page leads to the HTML checkout ([CheckoutController]), which
/// places the same session cart.
///
/// Every page works without JavaScript. The cart forms are plain posts
/// answered with a redirect (post/redirect/get), and the cart they change is
/// the app's own `Cart` row, found by the token kept in this visitor's
/// session.
///
/// No security chain covers these paths -- they are public -- so the forms
/// carry no CSRF token: a token in the product page would make every copy of
/// it private to one session and uncacheable, for a cart that holds nothing
/// of value. A post from another site is refused instead (see [#sameSite]),
/// and nothing here reads or changes an account.
@Controller
public class Storefront {
    private final StorefrontPages pages;
    private final com.codenameone.examples.bazaar.ops.Attribution attribution;

    public Storefront(StorefrontPages pages,
            com.codenameone.examples.bazaar.ops.Attribution attribution) {
        this.pages = pages;
        this.attribution = attribution;
    }

    @GetMapping("/")
    public String index(Model model, HttpServer.Request request) {
        pages.index(model, request);
        return "index";
    }

    @GetMapping("/s/{handle}")
    public String home(@PathVariable("handle") String handle, Model model,
            HttpServer.Request request) throws IOException {
        attribution.capture(request, handle);
        pages.home(handle, model, request);
        return "home";
    }

    @GetMapping("/s/{handle}/collections/{collection}")
    public String collection(@PathVariable("handle") String handle,
            @PathVariable("collection") String collection,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", required = false) String page,
            Model model, HttpServer.Request request) throws IOException {
        attribution.capture(request, handle);
        pages.collection(handle, collection, sort, number(page), model, request);
        return "listing";
    }

    @GetMapping("/s/{handle}/search")
    public String search(@PathVariable("handle") String handle,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", required = false) String page,
            Model model, HttpServer.Request request) throws IOException {
        attribution.capture(request, handle);
        pages.search(handle, q, sort, number(page), model, request);
        return "listing";
    }

    @GetMapping("/s/{handle}/products/{product}")
    public String product(@PathVariable("handle") String handle,
            @PathVariable("product") String product,
            @RequestParam(value = "variant", required = false) String variant,
            Model model, HttpServer.Request request) throws IOException {
        attribution.capture(request, handle);
        pages.product(handle, product, variant, model, request);
        return "product";
    }

    @GetMapping("/s/{handle}/cart")
    public String cart(@PathVariable("handle") String handle, Model model,
            HttpServer.Request request) throws IOException {
        pages.cart(handle, model, request);
        return "cart";
    }

    @PostMapping("/s/{handle}/cart/add")
    public String add(@PathVariable("handle") String handle,
            @ModelAttribute("form") CartForm form, HttpServer.Request request)
            throws IOException {
        sameSite(request);
        String plan;
        try {
            plan = request.param("plan");
        } catch (RuntimeException malformed) {
            plan = null;
        }
        if (plan != null && plan.length() > 0) {
            // Subscribing goes to its own checkout in the account (signed in,
            // CSRF, a saved card); the plan, item and quantity go with it.
            return "redirect:/account/subscriptions/start?shop=" + SiteUrls.encode(handle)
                    + "&plan=" + SiteUrls.encode(plan) + "&variant="
                    + SiteUrls.encode(form.variantId) + "&quantity="
                    + Math.max(1, Math.min(100, form.quantity));
        }
        pages.add(handle, form, request);
        return "redirect:" + StorefrontPages.storeUrl(handle) + "/cart";
    }

    @PostMapping("/s/{handle}/cart/update")
    public String update(@PathVariable("handle") String handle,
            @ModelAttribute("form") CartForm form, HttpServer.Request request)
            throws IOException {
        sameSite(request);
        pages.update(handle, form, false, request);
        return "redirect:" + StorefrontPages.storeUrl(handle) + "/cart";
    }

    @PostMapping("/s/{handle}/cart/remove")
    public String remove(@PathVariable("handle") String handle,
            @ModelAttribute("form") CartForm form, HttpServer.Request request)
            throws IOException {
        sameSite(request);
        pages.update(handle, form, true, request);
        return "redirect:" + StorefrontPages.storeUrl(handle) + "/cart";
    }

    /// The store's privacy and cookie policy, with the consent choice.
    /// The old chat page's address, kept for links already out there: the
    /// store's front page with the floating chat opened (`#chat`, which
    /// `chat.js` reads). The conversation itself lives in the widget.
    @GetMapping("/s/{handle}/chat")
    public String chat(@PathVariable("handle") String handle) {
        return "redirect:" + StorefrontPages.storeUrl(handle) + "#chat";
    }

    private static String param(HttpServer.Request request, String name) {
        try {
            String v = request.param(name);
            return v == null ? "" : v;
        } catch (RuntimeException malformed) {
            return "";
        }
    }

    @GetMapping("/s/{handle}/privacy")
    public String privacy(@PathVariable("handle") String handle, Model model,
            HttpServer.Request request) throws IOException {
        pages.privacy(handle, model, request);
        return "privacy";
    }

    @GetMapping("/track/{number}")
    public String track(@PathVariable("number") String number, Model model,
            HttpServer.Request request) {
        pages.track(number, model, request);
        return "track";
    }

    /// A missing store, product, collection or shipment answers the 404 page;
    /// anything else its own status, as the API would.
    @ExceptionHandler(ResponseStatusException.class)
    public HttpServer.Response refused(ResponseStatusException e) throws IOException {
        if (e.getStatus() == 404) {
            return NotFoundPage.response();
        }
        String reason = e.getReason() == null ? "" : e.getReason();
        return HttpServer.Response.text(e.getStatus(), reason);
    }

    /// Refuses a cart post another site's page made. Browsers say where a
    /// form was posted from in `Sec-Fetch-Site` and `Origin`; a request with
    /// neither is a script or a test, which a session cookie does not follow
    /// across sites anyway.
    public static void sameSite(HttpServer.Request request) {
        String site = request.getHeader("Sec-Fetch-Site");
        if (site != null && "cross-site".equalsIgnoreCase(site.trim())) {
            throw new ResponseStatusException(403, "Posted from another site");
        }
        String origin = request.getHeader("Origin");
        String host = request.getHeader("Host");
        if (origin != null && origin.length() > 0 && !"null".equals(origin) && host != null) {
            int scheme = origin.indexOf("://");
            String from = scheme < 0 ? origin : origin.substring(scheme + 3);
            if (!from.equalsIgnoreCase(host)) {
                throw new ResponseStatusException(403, "Posted from another site");
            }
        }
    }

    private static int number(String text) {
        if (text == null || text.length() == 0 || text.length() > 6) {
            return 0;
        }
        int out = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                return 0;
            }
            out = out * 10 + (c - '0');
        }
        return out;
    }
}
