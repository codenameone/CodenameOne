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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.CannedReplyDto;
import com.codenameone.examples.bazaar.api.ChatConversationDto;
import com.codenameone.examples.bazaar.api.ChatMessageDto;
import com.codenameone.examples.bazaar.api.ChatReplyDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The storefront chat inbox: conversations newest first, each opening its
/// thread with a reply box, the canned replies as one-tap answers, and
/// buttons to take the conversation and to close it.
final class InboxPane extends MerchantPane {
    InboxPane() {
        super("Inbox", "merchant-inbox");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "inbox-refresh", this::load);
        Api.ops().chats(MerchantState.storeId(), Net.to(chats -> {
            Container list = new Container(BoxLayout.y());
            list.setName("inbox-list");
            if (chats.isEmpty()) {
                list.add(Ui.plain(Lang.tr("No conversations yet"), "SvMuted"));
            }
            for (int iter = 0; iter < chats.size(); iter++) {
                final ChatConversationDto c = chats.get(iter);
                Container row = Ui.row((char) 0, c.name() + "  " + c.subject(), Lang.tr(Words.of(
                        c.status())) + (c.assignee().length() == 0 ? "" : "  " + c.assignee()),
                        Ui.plain(String.valueOf(c.messages()), "SvRowValue"));
                row.setLeadComponent(Ui.secondary(Lang.tr("Open"), "inbox-open-" + iter, e -> open(c)));
                list.add(row);
            }
            root(list);
        }, Ui::fail));
    }

    private void open(final ChatConversationDto c) {
        Api.ops().chatMessages(MerchantState.storeId(), c.id(), Net.to(messages ->
                Api.ops().cannedReplies(MerchantState.storeId(), Net.to(canned ->
                        push(c.name(), thread(c, messages, canned)), Ui::fail)), Ui::fail));
    }

    private Container thread(final ChatConversationDto c, List<ChatMessageDto> messages,
            List<CannedReplyDto> canned) {
        Container page = new Container(BoxLayout.y());
        page.setName("inbox-thread");
        for (int iter = 0; iter < messages.size(); iter++) {
            ChatMessageDto m = messages.get(iter);
            page.add(Ui.row((char) 0, m.author(), m.body(), Ui.plain(Period.iso(m.at()),
                    m.staff() ? "SvRowValue" : "SvMuted")));
        }
        final TextField reply = MUi.text(page, Lang.tr("Reply"), "inbox-reply", "");
        for (int iter = 0; iter < canned.size(); iter++) {
            final CannedReplyDto r = canned.get(iter);
            page.add(Ui.secondary(r.title(), "inbox-canned-" + iter, e -> reply.setText(r.body())));
        }
        page.add(MUi.buttons(
                Ui.primary(Lang.tr("Send"), "inbox-send", e -> Api.ops().replyChat(
                        MerchantState.storeId(), c.id(), new ChatReplyDto(reply.getText()),
                        Net.to(x -> {
                            pop();
                            load();
                        }, Ui::fail))),
                Ui.secondary(Lang.tr("Take it"), "inbox-assign", e -> Api.ops().assignChat(
                        MerchantState.storeId(), c.id(), new ChatReplyDto("me"),
                        Net.to(x -> Ui.say(Lang.tr("Assigned to you")), Ui::fail))),
                Ui.danger(Lang.tr("Close"), "inbox-close", e -> Api.ops().closeChat(
                        MerchantState.storeId(), c.id(), Net.to(x -> {
                            pop();
                            load();
                        }, Ui::fail)))));
        return page;
    }
}
