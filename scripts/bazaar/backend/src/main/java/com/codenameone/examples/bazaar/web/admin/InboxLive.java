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
package com.codenameone.examples.bazaar.web.admin;

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.appkit.server.Caller;
import com.codenameone.examples.bazaar.live.LiveTickets;
import com.codenameone.examples.bazaar.ops.ChatInbox;
import com.codenameone.examples.bazaar.web.WebPages;
import java.util.LinkedHashMap;
import java.util.Map;

/// The web inbox's way onto the chat socket: a single-use ticket for
/// `/ws/chat?staff=...&store=...`, the same kind the app's live channel takes
/// ([LiveTickets]). Only for staff who may work the store's inbox -- the
/// socket checks again, since a ticket names an account, not a store.
@Controller
public class InboxLive {
    private final WebPages pages;
    private final ChatInbox chat;
    private final LiveTickets tickets;

    public InboxLive(WebPages pages, ChatInbox chat, LiveTickets tickets) {
        this.pages = pages;
        this.chat = chat;
        this.tickets = tickets;
    }

    @GetMapping("/admin/{storeId}/chat-ticket")
    public HttpServer.Response ticket(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "");
        chat.requireInbox(Caller.name(), storeId);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("ticket", tickets.issue(Caller.name()).ticket());
        return HttpServer.Response.json(200, Json.write(out)).header("Cache-Control", "no-store");
    }
}
