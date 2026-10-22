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
package com.codenameone.examples.parley.ui;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.parley.api.CallDto;
import com.codenameone.examples.parley.chat.Calls;
import com.codenameone.examples.parley.chat.Messenger;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;
import com.codenameone.examples.parley.store.Store;
import java.util.List;

/// The calls this account made and received, newest first. A row calls back.
public final class CallsForm {
    private CallsForm() {
    }

    public static void show(Form previous) {
        final Form form = Ui.form("Calls", "Calls");
        Ui.back(form, previous);
        final Container list = Ui.list("calls");
        form.add(BorderLayout.CENTER, list);
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.calls().history(Net.to(calls -> fill(list, calls),
                Ui.failed(list, load[0])));
        form.addShowListener(e -> load[0].run());
        form.show();
    }

    private static void fill(Container list, List<CallDto> calls) {
        list.removeAll();
        String me = Messenger.me();
        for (int iter = 0; calls != null && iter < calls.size(); iter++) {
            final CallDto call = calls.get(iter);
            boolean outgoing = me.equals(call.caller());
            boolean missed = "MISSED".equals(call.state()) || "DECLINED".equals(call.state());
            String other = outgoing ? call.callee() : call.caller();
            String title = Store.conversation(call.conversationId()) != null
                    ? Store.title(call.conversationId()) : other;
            long length = call.answeredAt() > 0 && call.endedAt() > call.answeredAt()
                    ? (call.endedAt() - call.answeredAt()) / 1000 : 0;
            String detail = (outgoing ? "Outgoing" : missed ? "Missed" : "Incoming")
                    + (length > 0 ? Ui.DOT + length / 60 + ":" + (length % 60 < 10 ? "0" : "")
                    + length % 60 : "") + Ui.DOT + Ui.listTime(call.startedAt());
            Label kind = new Label("", missed && !outgoing ? "PlTickFailed" : "PlTick");
            FontImage.setMaterialIcon(kind, call.video() ? FontImage.MATERIAL_VIDEOCAM
                    : FontImage.MATERIAL_CALL, 3.4f);
            Container row = Ui.row(outgoing ? FontImage.MATERIAL_CALL_MADE
                    : missed ? FontImage.MATERIAL_CALL_MISSED : FontImage.MATERIAL_CALL_RECEIVED,
                    title, detail, kind);
            list.add(Ui.tap(row, "call-" + call.id(),
                    e -> Calls.start(call.conversationId(), call.video())));
        }
        if (list.getComponentCount() == 0) {
            list.add(Ui.empty(FontImage.MATERIAL_CALL, "No calls yet",
                    "Calls are end-to-end encrypted, like messages."));
        }
        list.revalidate();
    }
}
