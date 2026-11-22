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
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.webrtc.MediaTrack;
import com.codename1.webrtc.VideoView;
import com.codenameone.examples.bazaar.api.CannedReplyDto;
import com.codenameone.examples.bazaar.api.ChatConversationDto;
import com.codenameone.examples.bazaar.api.ChatMessageDto;
import com.codenameone.examples.bazaar.api.ChatReplyDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The storefront chat inbox: conversations newest first with their unread
/// counts, each opening its thread with a reply box, the canned replies as
/// one-tap answers, and buttons to take the conversation and to close it.
///
/// While it is on screen the inbox is connected to the store chat socket
/// ([ChatChannel]), the same one the web inbox uses: new lines arrive live,
/// the open thread is marked read, and a shopper's voice or video call rings
/// here and is answered with WebRTC ([ShopperCall], [RtcMedia]). Without the
/// socket it still works over REST, refreshed by hand.
final class InboxPane extends MerchantPane {
    private static final String CALL_FORM = "merchant-call";
    private static final long TYPING_MILLIS = 3000;

    private final InboxBoard board = new InboxBoard();
    private ChatChannel channel;
    private String channelStore = "";
    private List iceServers = new ArrayList();
    private ShopperCall call;
    private RtcMedia media;
    private boolean muted;
    private Form callForm;
    private Form beforeCall;
    private VideoView remoteView;
    private VideoView localView;
    private Container list;
    private Container threadLines;
    private Container threadCall;
    private long typedAt;

    InboxPane() {
        super("Inbox", "merchant-inbox");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "inbox-refresh", this::load);
        connect();
        Api.ops().chats(MerchantState.storeId(), Net.to(chats -> {
            List<InboxBoard.Row> rows = new ArrayList<InboxBoard.Row>();
            for (int iter = 0; iter < chats.size(); iter++) {
                ChatConversationDto c = chats.get(iter);
                InboxBoard.Row r = new InboxBoard.Row(c.id(), c.name(), c.subject(), c.status(),
                        c.assignee(), c.messages(), c.unread(), c.lastAt());
                InboxBoard.Row old = board.row(c.id());
                if (old != null) {
                    r.ringing = old.ringing;
                }
                rows.add(r);
            }
            board.reset(rows);
            list = new Container(BoxLayout.y());
            list.setName("inbox-list");
            paintList();
            root(list);
        }, Ui::fail));
    }

    // ------------------------------------------------------------------ socket

    private void connect() {
        String store = MerchantState.storeId();
        if (channel != null && store.equals(channelStore)) {
            channel.start();
            return;
        }
        leave();
        channelStore = store;
        channel = new ChatChannel(store, new ChatChannel.Listener() {
            @Override
            public void frame(String type, Map<String, Object> frame) {
                heard(type, frame);
            }

            @Override
            public void lost() {
                paintState();
            }
        });
        channel.start();
    }

    private void send(Map<String, Object> frame) {
        if (channel != null) {
            channel.send(frame);
        }
    }

    /// Forgets the open thread once its screen is gone (the bar's back, the
    /// phone's back), so its lines count as unread again.
    private void syncOpen() {
        if (board.open().length() == 0) {
            return;
        }
        Form f = threadLines == null ? null : threadLines.getComponentForm();
        Form current = CN.getCurrentForm();
        boolean showing = f != null && (f == current
                || current != null && CALL_FORM.equals(current.getName()));
        if (!showing) {
            board.close();
            threadLines = null;
            threadCall = null;
        }
    }

    private void heard(String type, Map<String, Object> frame) {
        syncOpen();
        if ("hello".equals(type)) {
            iceServers = ChatFrames.list(frame, "ice");
            if (board.open().length() > 0) {
                send(ChatFrames.watch(board.open()));
            }
            paintState();
        } else if ("message".equals(type)) {
            int outcome = board.message(frame);
            if (outcome == InboxBoard.UNKNOWN) {
                if (board.open().length() == 0) {
                    load();
                }
                return;
            }
            if (outcome == InboxBoard.SHOWN) {
                paintThread();
                if (!ChatFrames.truth(frame, "staff")) {
                    send(ChatFrames.seen(board.open(), board.newestAt()));
                }
            }
            if (outcome != InboxBoard.IGNORED) {
                paintList();
            }
        } else if ("thread".equals(type)) {
            if (board.thread(frame)) {
                paintThread();
                if (board.newestAt() > 0) {
                    send(ChatFrames.seen(board.open(), board.newestAt()));
                }
            }
        } else if ("call".equals(type)) {
            callFrame(frame);
        } else if ("error".equals(type)) {
            // An ICE candidate or a typing frame over the signal limit is
            // not worth a message; a refused reply is.
            if (ChatFrames.number(frame, "status") != 429) {
                Ui.say(ChatFrames.text(frame, "reason"));
            }
        }
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (channel != null) {
            channel.start();
        }
        CN.callSerially(() -> {
            syncOpen();
            paintList();
        });
    }

    @Override
    protected void deinitialize() {
        super.deinitialize();
        // A detail or call screen of this pane's own replaces its form on a
        // phone; the socket stays for those, and goes once the merchant has
        // left the inbox altogether.
        CN.callSerially(() -> {
            Form f = CN.getCurrentForm();
            String name = f == null ? "" : f.getName();
            if (f != getComponentForm() && !"merchant-detail".equals(name)
                    && !CALL_FORM.equals(name)) {
                leave();
            }
        });
    }

    private void leave() {
        if (call != null) {
            call.hangUp();
            call = null;
        }
        closeCallScreen();
        if (channel != null) {
            channel.stop();
        }
    }

    // ------------------------------------------------------------------ the list

    private void paintList() {
        if (list == null) {
            return;
        }
        list.removeAll();
        Container state = new Container(BoxLayout.y());
        state.setName("inbox-state");
        list.add(state);
        List<InboxBoard.Row> rows = board.rows();
        if (rows.isEmpty()) {
            list.add(Ui.plain(Lang.tr("No conversations yet"), "SvMuted"));
        }
        for (int iter = 0; iter < rows.size(); iter++) {
            final InboxBoard.Row c = rows.get(iter);
            String detail = Lang.tr(Words.of(c.status))
                    + (c.assignee.length() == 0 ? "" : "  " + c.assignee);
            Container end = new Container(BoxLayout.x());
            if (c.ringing.length() > 0) {
                end.add(Ui.badge(Lang.tr("video".equals(c.ringing) ? "Video call" : "Voice call"),
                        "SvBadgeWarn"));
            }
            end.add(c.unread > 0 ? Ui.badge(String.valueOf(c.unread), "SvBadgeBad")
                    : Ui.plain(String.valueOf(c.messages), "SvRowValue"));
            Container row = Ui.row((char) 0, c.name + "  " + c.subject, detail, end);
            list.add(Ui.tap(row, "inbox-open-" + iter, e -> open(c.id, c.name)));
        }
        paintState();
        Ui.relayout(list);
    }

    /// The line over the list: whether the inbox is live, and any call ringing.
    private void paintState() {
        Container state = list == null ? null : (Container) findNamed(list, "inbox-state");
        if (state == null) {
            return;
        }
        state.removeAll();
        boolean live = channel != null && channel.isOpen();
        int unread = board.unread();
        state.add(Ui.plain(Lang.tr(live ? "Live" : "Offline, reconnecting")
                + (unread > 0 ? "  " + unread + " " + Lang.tr("unread") : ""),
                live ? "SvRowValue" : "SvMuted"));
        if (call != null && call.state() == ShopperCall.RINGING) {
            state.add(ringBanner());
        }
        Ui.relayout(state);
    }

    private static com.codename1.ui.Component findNamed(Container in, String name) {
        for (int iter = 0; iter < in.getComponentCount(); iter++) {
            if (name.equals(in.getComponentAt(iter).getName())) {
                return in.getComponentAt(iter);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ a thread

    private void open(final String id, final String name) {
        Api.ops().chatMessages(MerchantState.storeId(), id, Net.to(messages ->
                Api.ops().cannedReplies(MerchantState.storeId(), Net.to(canned -> {
                    List<InboxBoard.Line> lines = new ArrayList<InboxBoard.Line>();
                    for (int iter = 0; iter < messages.size(); iter++) {
                        ChatMessageDto m = messages.get(iter);
                        lines.add(new InboxBoard.Line(m.id(), m.author(), m.staff(), m.body(),
                                m.at()));
                    }
                    board.open(id, lines);
                    push(name, thread(id, canned));
                    paintThread();
                    paintList();
                    // The socket answers with the live thread, any ringing
                    // call and its signalling, and the read mark follows.
                    send(ChatFrames.watch(id));
                }, Ui::fail)), Ui::fail));
    }

    private Container thread(final String id, List<CannedReplyDto> canned) {
        Container page = new Container(BoxLayout.y());
        page.setName("inbox-thread");
        threadCall = new Container(BoxLayout.y());
        threadCall.setName("inbox-thread-call");
        page.add(threadCall);
        threadLines = new Container(BoxLayout.y());
        threadLines.setName("inbox-lines");
        page.add(threadLines);
        final TextField reply = MUi.text(page, Lang.tr("Reply"), "inbox-reply", "");
        reply.addDataChangedListener((kind, index) -> {
            long now = System.currentTimeMillis();
            if (reply.getText().length() > 0 && now - typedAt > TYPING_MILLIS) {
                typedAt = now;
                send(ChatFrames.typing(id));
            }
        });
        for (int iter = 0; iter < canned.size(); iter++) {
            final CannedReplyDto r = canned.get(iter);
            page.add(Ui.secondary(r.title(), "inbox-canned-" + iter, e -> reply.setText(r.body())));
        }
        page.add(MUi.buttons(
                Ui.primary(Lang.tr("Send"), "inbox-send", e -> reply(id, reply)),
                Ui.secondary(Lang.tr("Take it"), "inbox-assign", e -> Api.ops().assignChat(
                        MerchantState.storeId(), id, new ChatReplyDto("me"),
                        Net.to(x -> Ui.say(Lang.tr("Assigned to you")), Ui::fail))),
                Ui.danger(Lang.tr("Close"), "inbox-close", e -> Api.ops().closeChat(
                        MerchantState.storeId(), id, Net.to(x -> back(), Ui::fail)))));
        return page;
    }

    private void back() {
        board.close();
        threadLines = null;
        threadCall = null;
        pop();
        load();
    }

    /// Sends a reply: over the socket when it is up -- the line comes back as
    /// a `message` frame, to this inbox as to every other -- or over REST.
    private void reply(String id, TextField field) {
        String body = field.getText().trim();
        if (body.length() == 0) {
            return;
        }
        if (channel != null && channel.send(ChatFrames.send(id, body))) {
            field.setText("");
            return;
        }
        Api.ops().replyChat(MerchantState.storeId(), id, new ChatReplyDto(body),
                Net.to(messages -> {
                    field.setText("");
                    List<InboxBoard.Line> lines = new ArrayList<InboxBoard.Line>();
                    for (int iter = 0; iter < messages.size(); iter++) {
                        ChatMessageDto m = messages.get(iter);
                        lines.add(new InboxBoard.Line(m.id(), m.author(), m.staff(), m.body(),
                                m.at()));
                    }
                    board.open(id, lines);
                    paintThread();
                }, Ui::fail));
    }

    private void paintThread() {
        if (threadLines == null) {
            return;
        }
        threadLines.removeAll();
        List<InboxBoard.Line> lines = board.lines();
        for (int iter = 0; iter < lines.size(); iter++) {
            InboxBoard.Line m = lines.get(iter);
            threadLines.add(Ui.row((char) 0, m.author, m.body, Ui.plain(Period.iso(m.at),
                    m.staff ? "SvRowValue" : "SvMuted")));
        }
        paintThreadCall();
        Ui.relayout(threadLines);
    }

    private void paintThreadCall() {
        if (threadCall == null) {
            return;
        }
        threadCall.removeAll();
        if (call != null && call.chat().equals(board.open())
                && call.state() == ShopperCall.RINGING) {
            threadCall.add(ringBanner());
        }
        Ui.relayout(threadCall);
    }

    // ------------------------------------------------------------------ calls

    private void callFrame(Map<String, Object> frame) {
        String chat = ChatFrames.text(frame, "chat");
        String kind = ChatFrames.text(frame, "kind");
        board.call(frame);
        if (ChatFrames.RING.equals(kind)) {
            if (call == null || call.state() == ShopperCall.ENDED) {
                call = new ShopperCall(chat, ChatFrames.truth(frame, "video"), this::send);
                InboxBoard.Row r = board.row(chat);
                Ui.say(Lang.tr(call.video() ? "Incoming video call" : "Incoming voice call")
                        + (r == null ? "" : ": " + r.name));
            }
        } else if (call != null && call.chat().equals(chat)) {
            if (call.signal(frame)) {
                call = null;
                closeCallScreen();
            }
        }
        paintList();
        paintThreadCall();
        paintCallScreen();
    }

    private Container ringBanner() {
        final ShopperCall ringing = call;
        InboxBoard.Row r = board.row(ringing.chat());
        String who = r == null ? "" : r.name;
        Container banner = new Container(BoxLayout.y());
        banner.setName("inbox-ring");
        banner.setUIID("SvBanner");
        banner.add(Ui.plain(Lang.tr(ringing.video() ? "Incoming video call" : "Incoming voice call")
                + (who.length() == 0 ? "" : ": " + who), "SvRowTitle"));
        if (!RtcMedia.supported()) {
            banner.add(Ui.plain(Lang.tr("Calls need a device with WebRTC"), "SvMuted"));
        }
        com.codename1.ui.Button accept = Ui.primary(Lang.tr("Accept"), "inbox-accept",
                e -> accept(ringing));
        accept.setEnabled(ringing.canAccept() && RtcMedia.supported());
        banner.add(MUi.buttons(accept, Ui.secondary(Lang.tr("Decline"), "inbox-decline", e -> {
            ringing.decline();
            if (call == ringing) {
                call = null;
            }
            paintList();
            paintThreadCall();
        })));
        return banner;
    }

    private void accept(final ShopperCall ringing) {
        if (call != ringing) {
            return;
        }
        muted = false;
        media = new RtcMedia(iceServers, this::paintCallScreen);
        if (ringing.accept(media)) {
            showCallScreen();
        }
    }

    private void showCallScreen() {
        beforeCall = CN.getCurrentForm();
        callForm = Ui.form(Lang.tr("Call"), CALL_FORM);
        Ui.back(callForm, this::hangUp);
        paintCallScreen();
        callForm.show();
    }

    private void hangUp() {
        if (call != null) {
            call.hangUp();
            call = null;
        }
        closeCallScreen();
        paintList();
        paintThreadCall();
    }

    private void paintCallScreen() {
        if (callForm == null) {
            return;
        }
        if (call == null || call.state() == ShopperCall.ENDED) {
            closeCallScreen();
            return;
        }
        callForm.removeAll();
        Container stage = new Container(new LayeredLayout());
        stage.setName("call-stage");
        MediaTrack remote = media == null ? null : media.remoteVideo();
        if (remote == null) {
            release(true);
        } else if (remoteView == null) {
            remoteView = new VideoView(remote, false);
        }
        if (remoteView != null) {
            stage.add(remoteView);
        }
        MediaTrack local = media == null ? null : media.localVideo();
        if (local == null) {
            release(false);
        } else if (localView == null) {
            localView = new VideoView(local, true);
        }
        InboxBoard.Row r = board.row(call.chat());
        Container who = BoxLayout.encloseY(
                Ui.plain(r == null ? "" : r.name, "SvRowTitle"),
                Ui.plain(Lang.tr(call.connected() ? "On the call" : "Connecting..."), "SvMuted"));
        stage.add(BorderLayout.north(who));
        if (localView != null) {
            Container corner = new Container(new BorderLayout());
            corner.add(BorderLayout.SOUTH, BorderLayout.east(localView));
            stage.add(corner);
        }
        callForm.add(BorderLayout.CENTER, stage);
        callForm.add(BorderLayout.SOUTH, MUi.buttons(
                Ui.secondary(Lang.tr(muted ? "Unmute" : "Mute"), "call-mute", e -> {
                    muted = !muted;
                    if (media != null) {
                        media.mute(muted);
                    }
                    paintCallScreen();
                }),
                Ui.danger(Lang.tr("Hang up"), "call-hangup", e -> hangUp())));
        callForm.revalidate();
    }

    private void release(boolean remote) {
        if (remote && remoteView != null) {
            remoteView.release();
            remoteView = null;
        } else if (!remote && localView != null) {
            localView.release();
            localView = null;
        }
    }

    private void closeCallScreen() {
        release(true);
        release(false);
        media = null;
        Form shown = callForm;
        callForm = null;
        Form back = beforeCall != null ? beforeCall : getComponentForm();
        beforeCall = null;
        if (shown != null && CN.getCurrentForm() == shown && back != null) {
            back.showBack();
        }
    }
}
