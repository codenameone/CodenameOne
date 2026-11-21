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

/// The HTML checkout and the order confirmation ([CheckoutPages]).
///
/// These paths are in the web app's security chain
/// (`web.security.WebSecurity`), open to guests: every post needs the
/// session's CSRF token, which the framework writes into each form, the
/// pages carry a per-response CSP nonce, and placing an order or applying a
/// code is rate limited (`Throttle` rule `checkout`). Nothing here is cached:
/// the pages hold an address, and the confirmation an order (see
/// `StorefrontCaching`).
///
/// - `GET /s/{handle}/checkout` -- the form.
/// - `POST /s/{handle}/checkout` -- the rates and totals again, for a changed
///   address or delivery choice; htmx swaps the summary out of this full page.
/// - `POST /s/{handle}/checkout/codes` -- a discount code or gift card.
/// - `POST /s/{handle}/checkout/place` -- the order; 303 to its page.
/// - `GET /s/{handle}/orders/{number}?t=` -- the confirmation, and
///   `.../invoice.pdf?t=` its invoice.
@Controller
public class CheckoutController {
    private final CheckoutPages pages;

    public CheckoutController(CheckoutPages pages) {
        this.pages = pages;
    }

    @GetMapping("/s/{handle}/checkout")
    public String show(@PathVariable("handle") String handle, Model model,
            HttpServer.Request request) throws IOException {
        pages.show(handle, model, request);
        return "checkout";
    }

    @PostMapping("/s/{handle}/checkout")
    public String refresh(@PathVariable("handle") String handle,
            @ModelAttribute("form") CheckoutForm form, Model model, HttpServer.Request request)
            throws IOException {
        pages.refresh(handle, form, model, request);
        return "checkout";
    }

    @PostMapping("/s/{handle}/checkout/codes")
    public String codes(@PathVariable("handle") String handle,
            @ModelAttribute("form") CheckoutForm form, Model model, HttpServer.Request request)
            throws IOException {
        pages.codes(handle, form, model, request);
        return "checkout";
    }

    @PostMapping("/s/{handle}/checkout/place")
    public String place(@PathVariable("handle") String handle,
            @ModelAttribute("form") CheckoutForm form, Model model, HttpServer.Request request)
            throws IOException {
        String placed = pages.place(handle, form, model, request);
        return placed == null ? "checkout" : "redirect:" + placed;
    }

    /// A company buyer's "place on net terms": see [CheckoutPages#terms].
    @PostMapping("/s/{handle}/checkout/terms")
    public String terms(@PathVariable("handle") String handle,
            @ModelAttribute("form") CheckoutForm form, Model model, HttpServer.Request request)
            throws IOException {
        String placed = pages.terms(handle, form, model, request);
        return placed == null ? "checkout" : "redirect:" + placed;
    }

    @GetMapping("/s/{handle}/orders/{number}")
    public String order(@PathVariable("handle") String handle,
            @PathVariable("number") String number,
            @RequestParam(value = "t", required = false) String token, Model model,
            HttpServer.Request request) throws IOException {
        pages.confirmation(handle, number, token, model, request);
        return "order";
    }

    @GetMapping("/s/{handle}/orders/{number}/invoice.pdf")
    public HttpServer.Response invoice(@PathVariable("handle") String handle,
            @PathVariable("number") String number,
            @RequestParam(value = "t", required = false) String token) throws IOException {
        return pages.invoice(handle, number, token);
    }

    /// A missing store or order answers the 404 page; anything else its own
    /// status.
    @ExceptionHandler(ResponseStatusException.class)
    public HttpServer.Response refused(ResponseStatusException e) throws IOException {
        if (e.getStatus() == 404) {
            return NotFoundPage.response();
        }
        String reason = e.getReason() == null ? "" : e.getReason();
        return HttpServer.Response.text(e.getStatus(), reason)
                .header("Cache-Control", "no-store");
    }
}
