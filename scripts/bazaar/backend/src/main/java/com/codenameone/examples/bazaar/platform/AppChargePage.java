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
package com.codenameone.examples.bazaar.platform;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.security.CsrfToken;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codenameone.examples.bazaar.api.AppChargeDto;
import com.codenameone.examples.bazaar.ops.AppStoreText;

import java.util.Map;

/// The web page where a store's owner approves or declines an app's charge:
/// `GET /account/apps/charges/{chargeId}?store=<storeId>`, the charge's
/// `confirmUrl`. Under the signed-in web pages, so the session, CSRF and the
/// sign-in redirect come with the path. Deciding sends the browser to the
/// app's `returnUrl` with `charge_id` and `status`, or back to this page
/// when the app gave none.
@RestController
public class AppChargePage {
    public static final String PATH = "/account/apps/charges";

    private final AppBilling billing;

    public AppChargePage(AppBilling billing) {
        this.billing = billing;
    }

    @GetMapping(PATH + "/{chargeId}")
    public HttpServer.Response show(@PathVariable("chargeId") String chargeId, CsrfToken csrf,
            @RequestParam(value = "store", required = false) String store) throws Exception {
        AppChargeDto c;
        try {
            c = billing.charge(Caller.name(), store, chargeId);
        } catch (ResponseStatusException refused) {
            return AppDirectoryPages.html(404, "No such charge", "", "<p>"
                    + AppConsentPage.esc(refused.getReason()) + "</p>", false);
        }
        StringBuilder b = new StringBuilder();
        b.append("<p><strong>").append(AppConsentPage.esc(c.appName())).append("</strong> asks: ")
                .append(AppConsentPage.esc(c.name())).append("</p><p>")
                .append(AppConsentPage.esc(AppStoreText.terms(c.kind(), c.amount(),
                        c.currency(), c.trialDays()))).append("</p>");
        if (AppBilling.PENDING.equals(c.status())) {
            b.append("<form method=\"post\" action=\"").append(PATH).append('/')
                    .append(AppConsentPage.esc(c.id())).append("\">");
            hidden(b, csrf.getParameterName(), csrf.getToken());
            hidden(b, "store", store);
            b.append("<button type=\"submit\" name=\"decision\" value=\"approve\">Approve"
                    + "</button> <button type=\"submit\" name=\"decision\" value=\"decline\">"
                    + "Decline</button></form>");
        } else {
            b.append("<p>Status: ").append(AppConsentPage.esc(c.status())).append("</p>");
        }
        return AppDirectoryPages.html(200, "Approve a charge", "", b.toString(), false);
    }

    @PostMapping(PATH + "/{chargeId}")
    public HttpServer.Response decide(@PathVariable("chargeId") String chargeId,
            HttpServer.Request request) throws Exception {
        Map<String, String> f = OAuth2Parameters.parse(request.getBody() == null ? ""
                : request.getBody());
        String store = f.get("store");
        AppChargeDto c;
        try {
            c = billing.decide(Caller.name(), store, chargeId,
                    "approve".equals(f.get("decision")));
        } catch (ResponseStatusException refused) {
            return AppDirectoryPages.html(refused.getStatus() == 409 ? 409 : 404,
                    "This charge cannot be answered", "", "<p>"
                    + AppConsentPage.esc(refused.getReason()) + "</p>", false);
        }
        String to;
        if (c.returnUrl().length() > 0) {
            Map<String, String> q = new java.util.LinkedHashMap<String, String>();
            q.put("charge_id", c.id());
            q.put("status", c.status());
            to = OAuth2Parameters.append(c.returnUrl(), q);
        } else {
            to = AppBilling.confirmPath(store, c.id());
        }
        return HttpServer.Response.text(303, "").header("Location", to)
                .header("Cache-Control", "no-store");
    }

    private static void hidden(StringBuilder b, String name, String value) {
        b.append("<input type=\"hidden\" name=\"").append(AppConsentPage.esc(name))
                .append("\" value=\"").append(AppConsentPage.esc(value)).append("\">");
    }
}
