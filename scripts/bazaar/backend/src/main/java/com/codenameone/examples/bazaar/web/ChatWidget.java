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
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.ops.ChatInbox;
import com.codenameone.examples.bazaar.ops.ChatLive;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.security.Throttle;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// The floating chat's way in. Storefront pages are public and cached, so
/// nothing about a visitor is rendered into them: `chat.js` posts here, with
/// the conversation token it keeps in the browser, and gets back a
/// single-use ticket for `/ws/chat`
/// ([com.codenameone.examples.bazaar.ops.ChatSocket]).
///
/// A signed-in customer's conversation follows the account: a guest
/// conversation is linked to it, and a device with no token resumes the
/// customer's latest one. Refused from another site like every storefront
/// post ([Storefront#sameSite]), and never cached.
@Controller
public class ChatWidget {
    private final Stores stores;
    private final ChatInbox inbox;
    private final ChatLive live;

    public ChatWidget(Stores stores, ChatInbox inbox, ChatLive live) {
        this.stores = stores;
        this.inbox = inbox;
        this.live = live;
    }

    @PostMapping("/s/{handle}/chat/session")
    public HttpServer.Response session(@PathVariable("handle") String handle,
            HttpServer.Request request) throws IOException {
        Storefront.sameSite(request);
        Store store = stores.activeStore(handle);
        String customer = CheckoutPages.signedIn();
        Map c = inbox.byToken(store.getId(), StorefrontPages.param(request, "token"));
        if (c == null) {
            c = inbox.latestOf(store.getId(), customer);
        }
        String chat = c == null ? "" : Rows.text(c.get("id"));
        if (chat.length() > 0 && customer != null) {
            inbox.link(chat, customer);
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("ticket", live.issue(store.getId(), store.getName(), chat, customer,
                Throttle.client(request)));
        out.put("token", c == null ? "" : Rows.text(c.get("token")));
        return HttpServer.Response.json(200, Json.write(out)).header("Cache-Control", "no-store");
    }
}
