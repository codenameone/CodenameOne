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

import com.codename1.capture.Capture;
import com.codename1.components.SpanLabel;
import com.codename1.io.FileSystemStorage;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.parley.Telemetry;
import com.codenameone.examples.parley.api.ConversationDto;
import com.codenameone.examples.parley.api.MemberDto;
import com.codenameone.examples.parley.chat.Calls;
import com.codenameone.examples.parley.chat.Messenger;
import com.codenameone.examples.parley.crypto.Codec;
import com.codenameone.examples.parley.store.Message;
import com.codenameone.examples.parley.store.Store;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.Map;

/// One conversation: its history as bubbles, and the composer under it.
///
/// Every bubble is a [Message] from the [Store]; this screen draws them and
/// hands what the user does to the [Messenger]. Nothing here touches the
/// network or a key.
public final class ChatForm extends Form implements Store.Listener, Messenger.TypingListener {
    /// How many of the most recent messages are drawn at first.
    private static final int PAGE = 80;
    private static final String[] REACTIONS = {"\ud83d\udc4d", "\u2764\ufe0f", "\ud83d\ude02",
        "\ud83d\ude2e", "\ud83d\ude22", "\ud83d\ude4f"};

    private final String conversationId;
    private final Form previous;
    private final Container wall = new Container(BoxLayout.y());
    private final Container composer = new Container(new BorderLayout());
    private final Container replyBar = new Container(new BorderLayout());
    private final TextField field = new TextField("", "Message", 20, TextArea.ANY);
    private final Label subtitle = new Label("", "PlChatPreview");
    private final Button send = new Button();
    private Message replyTo;
    private Message editing;
    private int shown = PAGE;
    private Media recorder;
    /// Ticks the recording clock while a voice note is recorded.
    private Timer recordingClock;
    private String recording;
    private long recordingStarted;

    private ChatForm(String conversationId, Form previous) {
        super(Store.title(conversationId), new BorderLayout());
        this.conversationId = conversationId;
        this.previous = previous;
        setUIID("PlForm");
        getContentPane().setUIID("PlWallPane");
        setName("Chat");
        Ui.watch(this);
        Ui.fit(this);
        // Leaving drops a voice note still recording, so the microphone is
        // not left open behind a screen with no way to stop it, and stops
        // the store calling a screen nobody will see again. Not on hiding:
        // a dialog or a screen opened from here comes back to this one.
        Ui.back(this, () -> {
            stopRecording(false);
            Store.unlisten(this);
            previous.showBack();
        });
    }


    public String conversationId() {
        return conversationId;
    }

    /// Opens a conversation over `previous`.
    public static void show(String conversationId, Form previous) {
        ChatForm form = new ChatForm(conversationId, previous);
        form.build();
        form.show();
    }

    private void build() {
        ConversationDto row = Store.conversation(conversationId);
        boolean direct = row == null || "DIRECT".equals(row.kind());
        if (row != null && direct && !Store.bridged(conversationId)) {
            command("videoCall", FontImage.MATERIAL_VIDEOCAM, () -> Calls.start(conversationId, true));
            command("voiceCall", FontImage.MATERIAL_CALL, () -> Calls.start(conversationId, false));
        }
        command("info", FontImage.MATERIAL_INFO_OUTLINE,
                () -> ConversationInfoForm.show(conversationId, this));
        Label title = new Label(Store.title(conversationId), "Title");
        title.setName("chatTitle");
        Label face = Ui.avatar(Store.title(conversationId));
        face.setUIID("PlTitleAvatar");
        if (!direct) {
            FontImage.setMaterialIcon(face, FontImage.MATERIAL_GROUP, 2.6f);
            face.setText("");
        }
        // The name gets whatever the call buttons leave and ends in an
        // ellipsis there, instead of being painted under them.
        Container heading = new Container(new BorderLayout());
        heading.add(BorderLayout.WEST, BoxLayout.encloseYCenter(face));
        // Centred on the avatar and the call buttons; with no subtitle the
        // name alone sits on their centre line rather than above it.
        heading.add(BorderLayout.CENTER, BoxLayout.encloseYCenter(title, subtitle));
        getToolbar().setTitleCentered(false);
        getToolbar().setTitleComponent(heading);

        Container top = new Container(BoxLayout.y());
        if (Store.bridged(conversationId)) {
            SpanLabel plain = new SpanLabel(!isBot(row)
                    ? "Linked to " + network(row) + ". Messages here are not end-to-end "
                    + "encrypted: the server relays them in the clear."
                    : "This is a bot. Messages to it are not end-to-end encrypted.",
                    "PlPlainBanner");
            plain.setName("notEncrypted");
            top.add(plain);
        }
        add(BorderLayout.NORTH, top);

        wall.setUIID("PlWall");
        wall.setScrollableY(true);
        wall.setName("messages");
        add(BorderLayout.CENTER, wall);

        composer.setUIID("PlComposer");
        if (canPost(row)) {
            buildComposer();
        } else {
            composer.add(BorderLayout.CENTER, new Label("Only admins can send messages here",
                    "PlSystem"));
        }
        // The composer, not the content pane, carries the safe area: padding
        // the pane left the home-indicator inset painted in the wallpaper
        // colour under the composer. The dock paints the composer's surface
        // into that inset instead.
        getContentPane().setSafeArea(false);
        Container dock = BoxLayout.encloseY(replyBar, composer);
        dock.setUIID("PlComposerDock");
        dock.setSafeArea(true);
        add(BorderLayout.SOUTH, dock);

        addShowListener(e -> {
            Store.listen(this);
            Messenger.listenTyping(this);
            Messenger.read(conversationId);
            refresh();
        });
        addShowListener(e -> scrollToEnd());
        refresh();
    }

    private void command(String name, char icon, final Runnable go) {
        com.codename1.ui.Command command = getToolbar().addMaterialCommandToRightBar("", icon,
                e -> go.run());
        Button button = getToolbar().findCommandComponent(command);
        if (button != null) {
            button.setName(name);
        }
    }

    /// The demo bot is the server itself, so its chats are plain text too:
    /// the server marks them bridged to "BOT".
    private static boolean isBot(ConversationDto row) {
        return row != null && "BOT".equals(row.bridge());
    }

    private static String network(ConversationDto row) {
        String bridge = row == null || row.bridge() == null ? "" : row.bridge();
        return "WHATSAPP".equals(bridge) ? "WhatsApp" : "TELEGRAM".equals(bridge) ? "Telegram"
                : "another network";
    }

    private static boolean canPost(ConversationDto row) {
        if (row == null) {
            return true;
        }
        boolean adminsOnly = row.adminsOnly() || "CHANNEL".equals(row.kind());
        return !adminsOnly || "OWNER".equals(row.myRole()) || "ADMIN".equals(row.myRole());
    }

    private void buildComposer() {
        field.setUIID("PlComposerField");
        field.setName("composer");
        field.setSingleLineTextArea(false);
        field.setRows(1);
        field.setGrowByContent(true);
        field.setMaxSize(4096);
        field.addDataChangedListener((type, index) -> {
            sendIcon();
            if (field.getText().length() > 0) {
                Messenger.typing(conversationId);
            }
        });
        field.setDoneListener(e -> submit());

        Button attach = new Button("", "PlComposerTool");
        attach.setName("attach");
        FontImage.setMaterialIcon(attach, FontImage.MATERIAL_ATTACH_FILE, 4.4f);
        attach.addActionListener(e -> attach(attach));
        if (Store.bridged(conversationId)) {
            attach.setEnabled(false);
            attach.setVisible(false);
        }

        send.setUIID("PlSend");
        send.setName("send");
        send.addActionListener(e -> {
            if (recording != null) {
                stopRecording(true);
            } else if (field.getText().trim().length() > 0 || editing != null) {
                submit();
            } else if (!Store.bridged(conversationId)) {
                startRecording();
            }
        });
        sendIcon();
        composer.add(BorderLayout.WEST, attach);
        composer.add(BorderLayout.CENTER, field);
        composer.add(BorderLayout.EAST, send);
        replyBar.setUIID("PlComposer");
        replyBar.setHidden(true);
    }

    private void sendIcon() {
        boolean text = field.getText().trim().length() > 0 || editing != null
                || Store.bridged(conversationId);
        FontImage.setMaterialIcon(send, recording != null ? FontImage.MATERIAL_SEND
                : text ? FontImage.MATERIAL_SEND : FontImage.MATERIAL_MIC, 4f);
    }

    private void submit() {
        String text = field.getText().trim();
        if (text.length() == 0) {
            return;
        }
        if (editing != null) {
            Messenger.edit(conversationId, editing, text);
            editing = null;
        } else {
            Messenger.sendText(conversationId, text, replyTo);
            ConversationDto row = Store.conversation(conversationId);
            Telemetry.messageSent(Message.TEXT, row != null && !"DIRECT".equals(row.kind()));
        }
        field.setText("");
        clearReply();
        sendIcon();
        scrollToEnd();
    }

    // ----------------------------------------------------------------- draw

    @Override
    public void changed(String id) {
        if (id.length() > 0 && !id.equals(conversationId)) {
            return;
        }
        if (CN.getCurrentForm() != this) {
            return;
        }
        if (id.equals(conversationId)) {
            Messenger.read(conversationId);
        }
        boolean atEnd = atEnd();
        refresh();
        if (atEnd) {
            scrollToEnd();
        }
    }

    @Override
    public void typing(String id) {
        if (id.equals(conversationId) && CN.getCurrentForm() == this) {
            describe();
        }
    }

    private void describe() {
        List<String> typists = Messenger.typists(conversationId);
        ConversationDto row = Store.conversation(conversationId);
        if (!typists.isEmpty()) {
            subtitle.setText(row != null && !"DIRECT".equals(row.kind())
                    ? name(typists.get(0)) + " is typing..." : "typing...");
            subtitle.setUIID("PlTyping");
        } else {
            subtitle.setUIID("PlChatPreview");
            if (row == null) {
                subtitle.setText("");
            } else if ("DIRECT".equals(row.kind())) {
                subtitle.setText(row.disappearingSeconds() > 0
                        ? "Disappearing messages: " + PrivacyForm.timer(row.disappearingSeconds())
                        : "");
            } else {
                subtitle.setText(row.members() == null ? "" : row.members().size() + " members");
            }
        }
        subtitle.setHidden(subtitle.getText().length() == 0);
        getToolbar().revalidate();
    }

    private void refresh() {
        describe();
        wall.removeAll();
        List<Message> all = Store.messages(conversationId);
        int from = Math.max(0, all.size() - shown);
        if (from > 0) {
            wall.add(FlowLayout.encloseCenter(Ui.link("Show earlier messages", "earlier", e -> {
                shown += PAGE;
                refresh();
            })));
        }
        long lastDay = -1;
        String lastFrom = null;
        String me = Messenger.me();
        boolean group = !"DIRECT".equals(Store.kind(conversationId));
        for (int iter = from; iter < all.size(); iter++) {
            Message each = all.get(iter);
            long day = Ui.dayNumber(each.at);
            if (day != lastDay) {
                wall.add(FlowLayout.encloseCenter(new Label(Ui.dayLabel(each.at), "PlDay")));
                lastDay = day;
                lastFrom = null;
            }
            if (Message.SYSTEM.equals(each.kind)) {
                SpanLabel note = new SpanLabel(each.text, "PlSystem");
                note.setTextUIID("PlSystem");
                wall.add(note);
                lastFrom = null;
                continue;
            }
            wall.add(bubble(each, me, group && !each.from.equals(lastFrom)));
            lastFrom = each.from;
        }
        if (all.isEmpty()) {
            wall.add(new SpanLabel(Store.bridged(conversationId) ? "No messages yet."
                    : "Messages and calls are end-to-end encrypted. Nobody outside this "
                    + "chat, not even the server, can read them.", "PlSystem"));
        }
        revalidate();
        // A wrapped bubble only knows how tall it is once it knows how wide
        // it was laid out; the second pass measures it at that width, so
        // lines past the first are not cut off.
        revalidate();
    }

    private Component bubble(final Message message, String me, boolean showAuthor) {
        final boolean mine = message.mine(me);
        Container bubble = new Container(BoxLayout.y());
        bubble.setUIID(mine ? "PlBubbleOut" : "PlBubbleIn");
        bubble.setName("bubble-" + message.clientId);
        if (showAuthor && !mine) {
            bubble.add(new Label(name(message.from), "PlBubbleAuthor"));
        }
        if (message.replyTo.length() > 0 && !message.deleted) {
            SpanLabel quote = new SpanLabel(message.replyText, "PlQuote");
            quote.setTextUIID("PlQuote");
            bubble.add(quote);
        }
        if (message.deleted) {
            bubble.add(new Label(mine ? "You deleted this message" : "This message was deleted",
                    "PlBubbleDeleted"));
        } else {
            if (message.media != null) {
                bubble.add(media(message));
            }
            if (message.text.length() > 0) {
                SpanLabel text = new SpanLabel(message.text, "PlBubbleText");
                text.setTextUIID("PlBubbleText");
                text.setName("text-" + message.clientId);
                bubble.add(text);
            }
        }
        Container meta = new Container(new FlowLayout(Component.RIGHT));
        String time = (message.edited ? "edited  " : "") + Ui.clock(message.at);
        if (message.expiresAt > 0) {
            Label timer = new Label("", "PlBubbleMeta");
            FontImage.setMaterialIcon(timer, FontImage.MATERIAL_TIMER, 2.2f);
            meta.add(timer);
        }
        meta.add(new Label(time, "PlBubbleMeta"));
        if (mine && !message.plain) {
            meta.add(tick(message));
        }
        bubble.add(meta);
        if (!message.reactions.isEmpty()) {
            String all = "";
            for (String emoji : message.reactions.values()) {
                if (all.indexOf(emoji) < 0) {
                    all += emoji;
                }
            }
            if (message.reactions.size() > 1) {
                all += " " + message.reactions.size();
            }
            Label reactions = new Label(all, "PlReactions");
            reactions.setName("reactions-" + message.clientId);
            bubble.add(mine ? FlowLayout.encloseRight(reactions) : FlowLayout.encloseIn(reactions));
        }
        Button lead = new Button();
        lead.addLongPressListener(e -> options(message));
        lead.addActionListener(e -> {
            if (message.status == Message.FAILED && mine) {
                Messenger.retry(conversationId, message);
            } else if (message.media != null && !message.deleted) {
                open(message);
            }
        });
        bubble.add(lead);
        lead.setHidden(true);
        bubble.setLeadComponent(lead);
        Container line = new Container(new BorderLayout());
        line.add(mine ? BorderLayout.EAST : BorderLayout.WEST, bubble);
        return line;
    }

    private static Label tick(Message message) {
        Label tick = new Label("", message.status == Message.READ ? "PlTickRead"
                : message.status == Message.FAILED ? "PlTickFailed" : "PlTick");
        char icon = message.status == Message.SENDING ? FontImage.MATERIAL_SCHEDULE
                : message.status == Message.SENT ? FontImage.MATERIAL_DONE
                : message.status == Message.FAILED ? FontImage.MATERIAL_ERROR_OUTLINE
                : FontImage.MATERIAL_DONE_ALL;
        FontImage.setMaterialIcon(tick, icon, 2.6f);
        tick.setName("tick-" + message.clientId + "-" + message.status);
        return tick;
    }

    private Component media(final Message message) {
        Map<String, Object> media = message.media;
        if (Message.IMAGE.equals(message.kind) || Message.VIDEO.equals(message.kind)) {
            Image thumb = com.codenameone.examples.parley.chat.Media.thumbImage(media);
            Label picture = new Label("", "PlMediaThumb");
            if (thumb != null) {
                int width = Math.min(CN.convertToPixels(55), Display.getInstance().getDisplayWidth() / 2);
                picture.setIcon(thumb.scaledWidth(width));
            } else {
                FontImage.setMaterialIcon(picture, FontImage.MATERIAL_IMAGE, 12);
            }
            if (Message.VIDEO.equals(message.kind)) {
                FontImage.setMaterialIcon(picture, FontImage.MATERIAL_PLAY_CIRCLE_FILLED, 12);
                picture.setText(duration(Codec.num(media.get("duration"))));
            }
            return picture;
        }
        if (Message.VOICE.equals(message.kind)) {
            Label voice = new Label("Voice message  " + duration(Codec.num(media.get("duration"))),
                    "PlBubbleText");
            FontImage.setMaterialIcon(voice, FontImage.MATERIAL_PLAY_ARROW, 4);
            voice.setName("voice-" + message.clientId);
            return voice;
        }
        Label file = new Label(Codec.str(media.get("name")), "PlBubbleText");
        FontImage.setMaterialIcon(file, FontImage.MATERIAL_INSERT_DRIVE_FILE, 4);
        return file;
    }

    private static String duration(long millis) {
        long seconds = Math.max(0, millis / 1000);
        return (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
    }

    private String name(String username) {
        ConversationDto row = Store.conversation(conversationId);
        if (row != null && row.members() != null) {
            for (MemberDto each : row.members()) {
                if (each.username().equals(username)) {
                    return each.displayName();
                }
            }
        }
        return username;
    }

    private boolean atEnd() {
        return wall.getScrollY() + wall.getHeight() >= wall.getScrollDimension().getHeight() - 40;
    }

    private void scrollToEnd() {
        CN.callSerially(() -> {
            if (wall.getComponentCount() > 0) {
                wall.scrollComponentToVisible(wall.getComponentAt(wall.getComponentCount() - 1));
            }
        });
    }

    // -------------------------------------------------------------- actions

    private void options(final Message message) {
        if (message.deleted && !message.mine(Messenger.me())) {
            return;
        }
        final Dialog sheet = new Dialog("", new BoxLayout(BoxLayout.Y_AXIS));
        sheet.setName("messageOptions");
        if (!message.deleted && !message.plain) {
            Container row = new Container(new FlowLayout(Component.CENTER));
            for (int iter = 0; iter < REACTIONS.length; iter++) {
                final String emoji = REACTIONS[iter];
                Button react = new Button(emoji, "PlComposerTool");
                react.setName("react-" + iter);
                react.addActionListener(e -> {
                    sheet.dispose();
                    Messenger.react(conversationId, message, emoji);
                });
                row.add(react);
            }
            sheet.add(row);
        }
        if (!message.deleted) {
            sheet.add(Ui.action(FontImage.MATERIAL_REPLY, "Reply", "reply", e -> {
                sheet.dispose();
                reply(message);
            }));
        }
        if (message.text.length() > 0) {
            sheet.add(Ui.action(FontImage.MATERIAL_CONTENT_COPY, "Copy", "copy", e -> {
                sheet.dispose();
                Display.getInstance().copyToClipboard(message.text);
                Ui.say("Copied");
            }));
        }
        if (!message.deleted && !message.plain) {
            sheet.add(Ui.action(FontImage.MATERIAL_FORWARD, "Forward", "forward", e -> {
                sheet.dispose();
                NewChatForm.forward(this, message);
            }));
        }
        if (message.mine(Messenger.me()) && !message.deleted && !message.plain) {
            if (Message.TEXT.equals(message.kind)) {
                sheet.add(Ui.action(FontImage.MATERIAL_EDIT, "Edit", "edit", e -> {
                    sheet.dispose();
                    editing = message;
                    field.setText(message.text);
                    showBar("Editing", message.text);
                    field.startEditingAsync();
                }));
            }
            sheet.add(Ui.action(FontImage.MATERIAL_DELETE, "Delete for everyone", "deleteForAll",
                    e -> {
                        sheet.dispose();
                        Messenger.deleteForEveryone(conversationId, message);
                    }));
        }
        sheet.setDisposeWhenPointerOutOfBounds(true);
        sheet.showPopupDialog(wall);
    }

    private void reply(Message message) {
        replyTo = message;
        editing = null;
        showBar(message.mine(Messenger.me()) ? "You" : name(message.from), Store.preview(message));
        field.startEditingAsync();
    }

    private void showBar(String who, String text) {
        replyBar.removeAll();
        SpanLabel quote = new SpanLabel(who + ": " + text, "PlQuote");
        quote.setTextUIID("PlQuote");
        Button close = Ui.flat(FontImage.MATERIAL_CLOSE, "cancelReply", e -> {
            if (editing != null) {
                field.setText("");
            }
            clearReply();
        });
        replyBar.add(BorderLayout.CENTER, quote).add(BorderLayout.EAST, close);
        replyBar.setHidden(false);
        sendIcon();
        revalidate();
    }

    private void clearReply() {
        replyTo = null;
        editing = null;
        replyBar.removeAll();
        replyBar.setHidden(true);
        sendIcon();
        revalidate();
    }

    private void attach(Component from) {
        final Dialog sheet = new Dialog("", new BoxLayout(BoxLayout.Y_AXIS));
        sheet.add(Ui.action(FontImage.MATERIAL_PHOTO_CAMERA, "Camera", "camera", e -> {
            sheet.dispose();
            Capture.capturePhoto(picked -> {
                if (picked != null && picked.getSource() != null) {
                    upload((String) picked.getSource(), "image/jpeg", Message.IMAGE, "photo.jpg", 0);
                }
            });
        }));
        sheet.add(Ui.action(FontImage.MATERIAL_PHOTO_LIBRARY, "Photo or video", "gallery", e -> {
            sheet.dispose();
            Display.getInstance().openGallery(picked -> {
                if (picked == null || !(picked.getSource() instanceof String)) {
                    return;
                }
                String path = (String) picked.getSource();
                boolean video = isVideo(path);
                upload(path, video ? "video/mp4" : "image/jpeg", video ? Message.VIDEO : Message.IMAGE,
                        video ? "video.mp4" : "photo.jpg", 0);
            }, Display.GALLERY_ALL);
        }));
        sheet.add(Ui.action(FontImage.MATERIAL_VIDEOCAM, "Record a video", "recordVideo", e -> {
            sheet.dispose();
            Capture.captureVideo(null, picked -> {
                if (picked != null && picked.getSource() instanceof String) {
                    upload((String) picked.getSource(), "video/mp4", Message.VIDEO, "video.mp4", 0);
                }
            });
        }));
        sheet.setDisposeWhenPointerOutOfBounds(true);
        sheet.showPopupDialog(from);
    }

    // Matched by hand without folding case: see CLAUDE.md on toLowerCase.
    private static boolean isVideo(String path) {
        String[] endings = {".mp4", ".mov", ".m4v", ".3gp", ".webm"};
        for (String ending : endings) {
            if (path.length() >= ending.length() && path.regionMatches(true,
                    path.length() - ending.length(), ending, 0, ending.length())) {
                return true;
            }
        }
        return false;
    }

    private void upload(String path, String mime, final String kind, String name, long duration) {
        final Label sending = new Label("Encrypting and sending...", "PlSystem");
        wall.add(sending);
        revalidate();
        scrollToEnd();
        com.codenameone.examples.parley.chat.Media.upload(path, mime, name, duration, media -> {
            wall.removeComponent(sending);
            Messenger.sendMedia(conversationId, kind, media, "");
            ConversationDto row = Store.conversation(conversationId);
            Telemetry.messageSent(kind, row != null && !"DIRECT".equals(row.kind()));
        }, (status, message) -> {
            wall.removeComponent(sending);
            Ui.fail(status, message);
            revalidate();
        });
    }

    private void open(final Message message) {
        if (Message.IMAGE.equals(message.kind)) {
            MediaViewer.image(this, message.media);
        } else if (Message.VIDEO.equals(message.kind)) {
            MediaViewer.video(this, message.media);
        } else if (Message.VOICE.equals(message.kind)) {
            MediaViewer.voice(message.media);
        } else {
            com.codenameone.examples.parley.chat.Media.file(message.media,
                    path -> Display.getInstance().execute(path), Ui::fail);
        }
    }

    // ---------------------------------------------------------- voice notes

    private void startRecording() {
        try {
            String dir = FileSystemStorage.getInstance().getAppHomePath() + "parley-open/";
            if (!FileSystemStorage.getInstance().exists(dir)) {
                FileSystemStorage.getInstance().mkdir(dir);
            }
            recording = dir + "voice-" + System.currentTimeMillis() + ".m4a";
            recorder = MediaManager.createMediaRecorder(new MediaRecorderBuilder()
                    .path(recording).mimeType(recordingMime()));
            recorder.play();
            recordingStarted = System.currentTimeMillis();
        } catch (IOException | RuntimeException noMicrophone) {
            recording = null;
            Ui.say("Could not start recording");
            return;
        }
        replyBar.removeAll();
        final Label clock = new Label("Recording 0:00", "PlTyping");
        clock.setName("recording");
        Button cancel = Ui.flat(FontImage.MATERIAL_DELETE, "cancelRecording",
                e -> stopRecording(false));
        replyBar.add(BorderLayout.CENTER, clock).add(BorderLayout.EAST, cancel);
        replyBar.setHidden(false);
        sendIcon();
        revalidate();
        recordingClock = CN.setInterval(500, new Runnable() {
            @Override
            public void run() {
                CN.callSerially(() -> clock.setText("Recording "
                        + duration(System.currentTimeMillis() - recordingStarted)));
            }
        });
    }

    private static String recordingMime() {
        String[] types = MediaManager.getAvailableRecordingMimeTypes();
        for (int iter = 0; types != null && iter < types.length; iter++) {
            if (types[iter].indexOf("aac") >= 0 || types[iter].indexOf("mp4") >= 0) {
                return types[iter];
            }
        }
        return types != null && types.length > 0 ? types[0] : "audio/aac";
    }

    private void stopRecording(boolean keep) {
        if (recorder == null) {
            return;
        }
        if (recordingClock != null) {
            recordingClock.cancel();
            recordingClock = null;
        }
        String path = recording;
        long length = System.currentTimeMillis() - recordingStarted;
        try {
            recorder.pause();
            recorder.cleanup();
        } catch (RuntimeException alreadyStopped) {
            // Nothing more to stop.
        }
        recorder = null;
        recording = null;
        clearReply();
        if (keep && length > 700) {
            upload(path, recordingMime(), Message.VOICE, "voice.m4a", length);
        } else {
            FileSystemStorage.getInstance().delete(path);
        }
    }
}
