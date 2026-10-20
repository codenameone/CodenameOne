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

import com.codename1.components.FloatingActionButton;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.parley.api.ConversationDto;
import com.codenameone.examples.parley.api.ConversationFlagsDto;
import com.codenameone.examples.parley.api.UserDto;
import com.codenameone.examples.parley.chat.Messenger;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;
import com.codenameone.examples.parley.net.Session;
import com.codenameone.examples.parley.store.Store;

import java.util.List;

/// The home screen: every conversation, pinned ones first and then the most
/// recent, each with its last message and how many are unread.
///
/// What it shows comes from the [Store] on this device -- the last message
/// line is decrypted text the server never had -- so it opens instantly and
/// offline, and it is rebuilt whenever the store says something changed.
public final class ChatsForm implements Nav.Home, Store.Listener {
    private Form form;
    private Container list;
    private TextField search;
    private boolean archived;

    @Override
    public void start() {
        form = Ui.form("Parley", "Chats");
        form.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_SEARCH, e -> {
            search.setVisible(!search.isVisible());
            search.setHidden(!search.isVisible());
            if (search.isVisible()) {
                search.startEditingAsync();
            } else {
                search.setText("");
                rebuild();
            }
            form.revalidate();
        });
        form.getToolbar().addMaterialCommandToOverflowMenu("New group", FontImage.MATERIAL_GROUP_ADD,
                e -> NewChatForm.group(form));
        form.getToolbar().addMaterialCommandToOverflowMenu("Join with a link",
                FontImage.MATERIAL_LINK, e -> join());
        form.getToolbar().addMaterialCommandToOverflowMenu("Calls", FontImage.MATERIAL_CALL,
                e -> CallsForm.show(form));
        form.getToolbar().addMaterialCommandToOverflowMenu("Settings", FontImage.MATERIAL_SETTINGS,
                e -> SettingsForm.show(form));
        UserDto user = Session.user();
        if (user != null && user.admin()) {
            form.getToolbar().addMaterialCommandToOverflowMenu("Admin console",
                    FontImage.MATERIAL_ADMIN_PANEL_SETTINGS, e -> Nav.switchTo(Nav.ADMIN));
        }

        search = new TextField("", "Search", 20, TextField.ANY);
        search.setUIID("PlSearchField");
        search.setName("search");
        search.setVisible(false);
        search.setHidden(true);
        search.addDataChangedListener((type, index) -> rebuild());

        list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        list.setName("chats");
        Ui.pull(list, () -> {
            Messenger.refreshConversations();
            Messenger.sync();
        });

        Container top = BoxLayout.encloseY(search);
        form.add(BorderLayout.NORTH, top);
        form.add(BorderLayout.CENTER, list);

        FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_CHAT);
        fab.setName("newChat");
        fab.addActionListener(e -> NewChatForm.direct(form));
        fab.bindFabToContainer(form.getContentPane());

        form.addShowListener(e -> {
            Store.listen(this);
            rebuild();
        });
        rebuild();
    }

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void changed(String conversationId) {
        if (!Nav.isHome(form)) {
            Store.unlisten(this);
            return;
        }
        if (CN.getCurrentForm() == form) {
            rebuild();
        }
    }

    private void rebuild() {
        list.removeAll();
        String filter = search.getText().trim();
        List<String> ids = Store.ids(archived);
        if (archived) {
            list.add(Ui.tap(Ui.row(FontImage.MATERIAL_ARROW_BACK, "Back to chats", null, null),
                    "unarchived", e -> {
                        archived = false;
                        rebuild();
                    }));
        } else if (!Store.ids(true).isEmpty()) {
            list.add(Ui.tap(Ui.row(FontImage.MATERIAL_ARCHIVE, "Archived",
                    String.valueOf(Store.ids(true).size()), null), "archived", e -> {
                        archived = true;
                        rebuild();
                    }));
        }
        int shown = 0;
        for (final String id : ids) {
            String title = Store.title(id);
            if (filter.length() > 0 && !contains(title, filter) && !contains(Store.lastText(id), filter)) {
                continue;
            }
            list.add(row(id, title));
            shown++;
        }
        if (shown == 0) {
            list.add(Ui.empty(FontImage.MATERIAL_FORUM,
                    filter.length() > 0 ? "Nothing matches" : "No chats yet",
                    filter.length() > 0 ? null : "Start one with the button below."));
        }
        list.revalidate();
    }

    private static boolean contains(String text, String part) {
        return text != null && text.length() >= part.length()
                && indexIgnoringCase(text, part) >= 0;
    }

    // regionMatches ignores case without folding the locale: see CLAUDE.md
    // on toLowerCase.
    private static int indexIgnoringCase(String text, String part) {
        for (int iter = 0; iter + part.length() <= text.length(); iter++) {
            if (text.regionMatches(true, iter, part, 0, part.length())) {
                return iter;
            }
        }
        return -1;
    }

    private Component row(final String id, String title) {
        Container row = new Container(new BorderLayout());
        row.setUIID("PlChatRow");
        Label avatar = Ui.avatar(title);
        if (!"DIRECT".equals(Store.kind(id))) {
            FontImage.setMaterialIcon(avatar, FontImage.MATERIAL_GROUP, 3.4f);
            avatar.setText("");
        }
        Label name = new Label(title, "PlChatTitle");
        String preview = Store.lastText(id);
        List<String> typing = Messenger.typists(id);
        Label last = new Label(!typing.isEmpty() ? "typing..." : preview, "PlChatPreview");
        Label time = new Label(Ui.listTime(Store.lastAt(id)), "PlChatTime");
        Container right = BoxLayout.encloseY(time);
        int unread = Store.unread(id);
        if (unread > 0) {
            Label badge = new Label(String.valueOf(unread), "PlUnread");
            badge.setName("unread-" + id);
            right.add(BoxLayout.encloseXRight(badge));
        } else if (Store.flag(id, "pinned")) {
            Label pin = new Label("", "PlChatTime");
            FontImage.setMaterialIcon(pin, FontImage.MATERIAL_PUSH_PIN, 2.6f);
            right.add(BoxLayout.encloseXRight(pin));
        }
        if (Store.flag(id, "muted")) {
            Label muted = new Label("", "PlChatTime");
            FontImage.setMaterialIcon(muted, FontImage.MATERIAL_VOLUME_OFF, 2.6f);
            name.setIcon(muted.getIcon());
            name.setTextPosition(Component.LEFT);
        }
        row.add(BorderLayout.WEST, avatar);
        row.add(BorderLayout.CENTER, BoxLayout.encloseY(name, last));
        row.add(BorderLayout.EAST, right);
        Button lead = new Button();
        lead.setName("chat-" + Store.title(id));
        lead.addActionListener(e -> ChatForm.show(id, form));
        lead.addLongPressListener(e -> options(id));
        row.add(BorderLayout.SOUTH, lead);
        lead.setHidden(true);
        row.setLeadComponent(lead);
        return row;
    }

    private void options(final String id) {
        final ConversationDto row = Store.conversation(id);
        if (row == null) {
            return;
        }
        final Dialog sheet = new Dialog(Store.title(id), new BoxLayout(BoxLayout.Y_AXIS));
        sheet.add(Ui.action(FontImage.MATERIAL_PUSH_PIN, row.pinned() ? "Unpin" : "Pin", "pin",
                e -> {
                    sheet.dispose();
                    flags(row, row.muted(), row.archived(), !row.pinned());
                }));
        sheet.add(Ui.action(FontImage.MATERIAL_VOLUME_OFF, row.muted() ? "Unmute" : "Mute", "mute",
                e -> {
                    sheet.dispose();
                    flags(row, !row.muted(), row.archived(), row.pinned());
                }));
        sheet.add(Ui.action(FontImage.MATERIAL_ARCHIVE, row.archived() ? "Unarchive" : "Archive",
                "archive", e -> {
                    sheet.dispose();
                    flags(row, row.muted(), !row.archived(), row.pinned());
                }));
        sheet.add(Ui.action(FontImage.MATERIAL_DELETE_SWEEP, "Clear history on this phone",
                "clear", e -> {
                    sheet.dispose();
                    Store.wipe(id);
                }));
        sheet.setDisposeWhenPointerOutOfBounds(true);
        sheet.showPopupDialog(list);
    }

    private static void flags(ConversationDto row, boolean muted, boolean archived, boolean pinned) {
        Api.conversations().flags(row.id(), new ConversationFlagsDto(muted, archived, pinned),
                Net.to(fresh -> Store.conversation(fresh, true), Ui::fail));
    }

    private void join() {
        final TextField code = new TextField("", "Invite link or code", 30, TextField.ANY);
        if (Dialog.show("Join a group", code, new com.codename1.ui.Command("Cancel"),
                new com.codename1.ui.Command("Join")).getCommandName().equals("Join")) {
            String text = code.getText().trim();
            int slash = text.lastIndexOf('/');
            final String invite = slash >= 0 ? text.substring(slash + 1) : text;
            if (invite.length() == 0) {
                return;
            }
            Api.conversations().join(invite, Net.to(joined -> {
                Store.conversation(joined, true);
                ChatForm.show(joined.id(), form);
            }, Ui::fail));
        }
    }
}
