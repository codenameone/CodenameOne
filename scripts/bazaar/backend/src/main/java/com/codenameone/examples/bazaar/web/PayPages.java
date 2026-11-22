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
import com.codename1.backend.mvc.Model;
import com.codenameone.examples.bazaar.api.CardDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderLineDto;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.order.PayLinks;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.repository.StoreRepository;

import java.io.IOException;
import java.util.Map;

/// The storefront page a checkout link opens.
///
/// A merchant sends a draft order or an unpaid invoice as a signed link
/// ([PayLinks]); the mail points at `/pay/{token}`, which answers 303 to the
/// order's own store at `/s/{handle}/checkout/pay/{token}`. That address sits
/// in the checkout's chain -- CSRF on the post, a nonce CSP, the checkout's
/// rate limit, no sign-in needed -- and the page pays through the same card
/// path the checkout uses ([CheckoutPages#typedCard], then
/// [PayLinks#pay] and [Orders#payOnline]). A forged, expired, replaced or used
/// token is the 404 page, the same for each, as [PayLinks#resolve] answers.
@Controller
public class PayPages {
    private final PayLinks links;
    private final Orders orders;
    private final StoreRepository storeRows;
    private final StorefrontPages pages;
    private final Payments payments;
    private final Backorders backorders;

    public PayPages(PayLinks links, Orders orders, StoreRepository storeRows,
            StorefrontPages pages, Payments payments, Backorders backorders) {
        this.links = links;
        this.orders = orders;
        this.storeRows = storeRows;
        this.pages = pages;
        this.payments = payments;
        this.backorders = backorders;
    }

    /// Where `/pay/{token}` sends the browser: the link's page in its order's
    /// store; null for a link that does not resolve.
    public String address(String token) throws IOException {
        try {
            StoreOrder o = links.resolve(token);
            Store store = storeRows.findById(o.getStoreId());
            return store == null ? null : address(store, token);
        } catch (ResponseStatusException refused) {
            if (refused.getStatus() == 404) {
                return null;
            }
            throw refused;
        }
    }

    @GetMapping("/s/{handle}/checkout/pay/{token}")
    public String page(@PathVariable("handle") String handle,
            @PathVariable("token") String token, Model model, HttpServer.Request request)
            throws IOException {
        Store store = storeOf(handle, token);
        render(store, links.resolve(token), token, new PayView(), model, request);
        return "pay";
    }

    @PostMapping("/s/{handle}/checkout/pay/{token}")
    public String pay(@PathVariable("handle") String handle,
            @PathVariable("token") String token, @ModelAttribute("form") CheckoutForm form,
            Model model, HttpServer.Request request) throws IOException {
        Store store = storeOf(handle, token);
        StoreOrder order = links.resolve(token);
        PayView view = new PayView();
        CheckoutView check = new CheckoutView();
        CardDto card = CheckoutPages.typedCard(clean(form), check);
        if (card != null) {
            try {
                links.pay(token, CheckoutPages.signedIn(), card);
                view.paid = true;
            } catch (ResponseStatusException refused) {
                if (refused.getStatus() == 404) {
                    throw refused;
                }
                check.cardError = refused.getReason() == null ? "The card was declined"
                        : refused.getReason();
            }
        }
        view.cardError = check.cardError;
        render(store, order, token, view, model, request);
        return "pay";
    }

    /// A missing store, order or link is the 404 page; anything else its status.
    @ExceptionHandler(ResponseStatusException.class)
    public HttpServer.Response refused(ResponseStatusException e) throws IOException {
        if (e.getStatus() == 404) {
            return NotFoundPage.response();
        }
        return HttpServer.Response.text(e.getStatus(), e.getReason() == null ? "" : e.getReason());
    }

    private static String address(Store store, String token) {
        return "/s/" + SiteUrls.encode(store.getHandle()) + "/checkout/pay/"
                + SiteUrls.encode(token);
    }

    /// The store of the link's order, which must be the one in the address.
    private Store storeOf(String handle, String token) throws IOException {
        StoreOrder o = links.resolve(token);
        Store store = storeRows.findById(o.getStoreId());
        if (store == null || !store.getHandle().equals(handle)) {
            throw new ResponseStatusException(404, "No such order");
        }
        return store;
    }

    private static CheckoutForm clean(CheckoutForm f) {
        CheckoutForm out = f == null ? new CheckoutForm() : f;
        out.cardNumber = cut(out.cardNumber, 32);
        out.cardExpiry = cut(out.cardExpiry, 8);
        out.cardCvc = cut(out.cardCvc, 4);
        out.name = cut(out.name, 80);
        return out;
    }

    private static String cut(String s, int max) {
        String t = s == null ? "" : s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private void render(Store store, StoreOrder row, String token, PayView view, Model model,
            HttpServer.Request request) throws IOException {
        OrderDto o = orders.detailOf(row.getId());
        String cur = o.currency();
        view.number = o.number();
        view.email = o.email() == null ? "" : o.email();
        view.storeName = store.getName();
        view.storeUrl = StorefrontPages.storeUrl(store.getHandle());
        view.action = address(store, token);
        view.typedCards = payments.takesTypedCards();
        Map<String, Integer> late = backorders.ofOrder(row.getId());
        for (int iter = 0; iter < o.lines().size(); iter++) {
            OrderLineDto l = o.lines().get(iter);
            CartLineView lv = new CartLineView();
            lv.id = l.id();
            lv.title = l.title();
            lv.variant = l.variantTitle() == null ? "" : l.variantTitle();
            lv.quantity = l.quantity();
            lv.unit = Money.format(l.priceCents(), cur);
            lv.total = Money.format(l.priceCents() * l.quantity(), cur);
            lv.backordered = late.containsKey(l.id())
                    || backorders.wouldBackorder(l.variantId(), l.quantity());
            view.lines.add(lv);
        }
        view.subtotal = Money.format(o.subtotalCents(), cur);
        view.discount = o.discountCents() > 0 ? "-" + Money.format(o.discountCents(), cur) : "";
        view.shipping = Money.format(o.shippingCents(), cur);
        view.tax = Money.format(o.taxCents(), cur);
        view.total = Money.format(o.totalCents(), cur);
        long paidBefore = 0;
        if (o.tenders() != null) {
            for (int iter = 0; iter < o.tenders().size(); iter++) {
                paidBefore += o.tenders().get(iter).amountCents();
            }
        }
        view.due = Money.format(Math.max(0, o.totalCents() - paidBefore), cur);
        PageMeta meta = pages.meta(store, request, view.action);
        meta.title = meta.t.get("pay.title") + " #" + o.number() + " - " + store.getName();
        meta.noindex = true;
        view.cardError = StoreText.translate(meta.t, view.cardError);
        model.addAttribute("page", meta).addAttribute("pay", view);
    }
}
