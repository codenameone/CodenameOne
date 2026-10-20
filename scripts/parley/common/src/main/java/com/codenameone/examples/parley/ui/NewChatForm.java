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

import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.parley.Telemetry;
import com.codenameone.examples.parley.api.ContactDto;
import com.codenameone.examples.parley.api.NewConversationDto;
import com.codenameone.examples.parley.chat.Messenger;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;
import com.codenameone.examples.parley.store.Message;
import com.codenameone.examples.parley.store.Store;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Starting something: a chat with one person, a group, or a message
/// forwarded into a chat that already exists.
///
/// People are found by the server's contact search -- by name, username or
/// phone number -- since there is no address book upload: the server is never
/// told who a user knows.
public final class NewChatForm {
    private static final int SEARCH_DELAY = 350;

    private NewChatForm() {
    }

    /// A chat with one person: pick them and the conversation opens.
    public static void direct(final Form previous) {
        final Form form = Ui.form("New chat", "NewChat");
        Ui.back(form, previous);
        final Container results = Ui.list("contacts");
        TextField search = searchField(form, results, contact -> open(previous, contact), null);
        form.add(BorderLayout.NORTH, wrap(search));
        form.add(BorderLayout.CENTER, results);
        Container top = new Container(BoxLayout.y());
        top.add(Ui.tap(Ui.row(FontImage.MATERIAL_GROUP_ADD, "New group", null, null), "newGroup",
                e -> group(previous)));
        results.add(top);
        form.show();
        search.startEditingAsync();
    }

    /// A group: pick the members, name it, and it is created.
    public static void group(final Form previous) {
        final Form form = Ui.form("New group", "NewGroup");
        Ui.back(form, previous);
        final Map<String, ContactDto> picked = new LinkedHashMap<String, ContactDto>();
        final Container results = Ui.list("contacts");
        Container top = Ui.page();
        top.setScrollableY(false);
        final TextField title = Ui.field(top, "Group name", "groupName", "What is it about?");
        final Container chosen = new Container(new FlowLayout());
        chosen.setName("chosen");
        top.add(chosen);
        final Runnable[] redraw = new Runnable[1];
        redraw[0] = () -> {
            chosen.removeAll();
            for (ContactDto each : picked.values()) {
                chosen.add(Ui.chip(each.displayName(), FontImage.MATERIAL_PERSON));
            }
            form.revalidate();
        };
        TextField search = searchField(form, results, null, (contact, box) -> {
            if (box.isSelected()) {
                picked.put(contact.username(), contact);
            } else {
                picked.remove(contact.username());
            }
            redraw[0].run();
        });
        top.add(search);
        form.add(BorderLayout.NORTH, top);
        form.add(BorderLayout.CENTER, results);
        form.add(BorderLayout.SOUTH, wrap(Ui.primary("Create group", "createGroup", e -> {
            String name = title.getText().trim();
            if (name.length() == 0) {
                Ui.say("Give the group a name");
                return;
            }
            List<String> members = new ArrayList<String>(picked.keySet());
            Api.conversations().create(new NewConversationDto("GROUP", name, members),
                    Net.to(created -> {
                        Store.conversation(created, true);
                        Telemetry.groupCreated(members.size() + 1);
                        ChatForm.show(created.id(), previous);
                    }, Ui::fail));
        })));
        form.show();
    }

    /// Sends a copy of `message` into a chat the user picks. A forward is a
    /// new message, encrypted afresh for the chat it goes to; the media key
    /// travels with it, so the blob is not uploaded again.
    public static void forward(final Form previous, final Message message) {
        final Form form = Ui.form("Forward to", "Forward");
        Ui.back(form, previous);
        Container list = Ui.list("targets");
        List<String> ids = Store.ids(false);
        for (final String id : ids) {
            if (Store.bridged(id)) {
                continue;
            }
            list.add(Ui.tap(Ui.row(FontImage.MATERIAL_CHAT, Store.title(id), null, null),
                    "target-" + id, e -> {
                        if (message.media != null) {
                            Messenger.sendMedia(id, message.kind, message.media, message.text);
                        } else {
                            Messenger.sendText(id, message.text, null);
                        }
                        Ui.say("Forwarded");
                        ChatForm.show(id, previous);
                    }));
        }
        form.add(BorderLayout.CENTER, list);
        form.show();
    }

    private static void open(Form previous, ContactDto contact) {
        List<String> members = new ArrayList<String>();
        members.add(contact.username());
        Api.conversations().create(new NewConversationDto("DIRECT", null, members),
                Net.to(created -> {
                    Store.conversation(created, true);
                    ChatForm.show(created.id(), previous);
                }, Ui::fail));
    }

    interface Picked {
        void picked(ContactDto contact);
    }

    interface Toggled {
        void toggled(ContactDto contact, CheckBox box);
    }

    private static Container wrap(com.codename1.ui.Component c) {
        Container box = new Container(new BorderLayout());
        box.setUIID("PlPage");
        box.add(BorderLayout.CENTER, c);
        return box;
    }

    private static TextField searchField(final Form form, final Container results,
            final Picked picked, final Toggled toggled) {
        final TextField search = new TextField("", Lang.tr("Name, username or phone"), 24,
                TextField.ANY);
        search.setUIID("PlField");
        search.getHintLabel().setUIID("PlFieldHint");
        search.setName("contactSearch");
        final UITimer[] pending = new UITimer[1];
        search.addDataChangedListener((type, index) -> {
            if (pending[0] != null) {
                pending[0].cancel();
            }
            pending[0] = UITimer.timer(SEARCH_DELAY, false, form, () -> {
                final String query = search.getText().trim();
                if (query.length() < 2) {
                    return;
                }
                Api.contacts().search(query, Net.to(found -> {
                    if (!query.equals(search.getText().trim())) {
                        return;
                    }
                    results.removeAll();
                    for (final ContactDto each : found) {
                        if (each.username().equals(Messenger.me()) || each.blocked()) {
                            continue;
                        }
                        Container row = Ui.row((char) 0, each.displayName(),
                                each.about() == null || each.about().length() == 0
                                ? each.username() : each.about(), null);
                        row.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(
                                Ui.avatar(each.displayName())));
                        if (toggled != null) {
                            final CheckBox box = new CheckBox();
                            box.setName("pick-" + each.username());
                            box.addActionListener(e -> toggled.toggled(each, box));
                            row.add(BorderLayout.EAST, box);
                            results.add(row);
                        } else {
                            results.add(Ui.tap(row, "contact-" + each.username(),
                                    e -> picked.picked(each)));
                        }
                    }
                    if (results.getComponentCount() == 0) {
                        results.add(Ui.empty(FontImage.MATERIAL_PERSON_SEARCH,
                                "Nobody by that name", null));
                    }
                    form.revalidate();
                }, (status, message) -> { }));
            });
        });
        return search;
    }
}
