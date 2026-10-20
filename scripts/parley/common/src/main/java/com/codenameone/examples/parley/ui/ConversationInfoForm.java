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

import com.codename1.components.SpanLabel;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.parley.Telemetry;
import com.codenameone.examples.parley.api.BridgeRequestDto;
import com.codenameone.examples.parley.api.ConversationDto;
import com.codenameone.examples.parley.api.ConversationEditDto;
import com.codenameone.examples.parley.api.DeviceAddressDto;
import com.codenameone.examples.parley.api.MemberChangeDto;
import com.codenameone.examples.parley.api.MemberDto;
import com.codenameone.examples.parley.api.ReportDto;
import com.codenameone.examples.parley.chat.Messenger;
import com.codenameone.examples.parley.crypto.Identity;
import com.codenameone.examples.parley.crypto.SafetyNumber;
import com.codenameone.examples.parley.crypto.Sessions;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;
import com.codenameone.examples.parley.store.Message;
import com.codenameone.examples.parley.store.Store;
import java.util.List;

/// Everything about one conversation that is not its messages: who is in it
/// and what they may do, the disappearing timer, the invite link, the safety
/// numbers, a bridge, and leaving, blocking or reporting.
public final class ConversationInfoForm {
    private ConversationInfoForm() {
    }

    public static void show(final String id, final Form previous) {
        final ConversationDto row = Store.conversation(id);
        if (row == null) {
            return;
        }
        final Form form = Ui.form(row.title(), "ConversationInfo");
        Ui.back(form, previous);
        final Container page = Ui.page();
        page.setName("info");
        final boolean direct = "DIRECT".equals(row.kind());
        final boolean admin = "OWNER".equals(row.myRole()) || "ADMIN".equals(row.myRole());
        final String peer = Store.peer(id, Messenger.me());

        page.add(FlowLayout.encloseCenter(Ui.avatar(row.title())));
        page.add(FlowLayout.encloseCenter(Ui.label(row.title(), "PlHeading")));
        if (row.about() != null && row.about().length() > 0) {
            page.add(Ui.text(row.about(), "PlMuted"));
        }

        Ui.section(page, "Disappearing messages");
        page.add(Ui.text("New messages vanish from every device this long after they are "
                + "read. Anyone in the chat can still copy or photograph them first.",
                "PlMuted"));
        Container timer = Ui.choice("timer", PrivacyForm.TIMER_LABELS, PrivacyForm.TIMER_VALUES,
                String.valueOf(row.disappearingSeconds()), value -> {
                    int seconds = Integer.parseInt(value);
                    edit(row, row.title(), row.about(), seconds, row.adminsOnly(), () ->
                            Telemetry.timerChanged(seconds));
                }, 3);
        timer.setEnabled(direct || admin);
        page.add(timer);

        if (!direct) {
            List<MemberDto> members = row.members();
            Ui.section(page, Lang.tr("{0} members", String.valueOf(members == null ? 0
                    : members.size())));
            if (admin) {
                page.add(Ui.row(FontImage.MATERIAL_CAMPAIGN, "Only admins can send",
                        null, Ui.toggle("adminsOnly", row.adminsOnly(), on ->
                                edit(row, row.title(), row.about(), row.disappearingSeconds(), on,
                                        null))));
                page.add(Ui.tap(Ui.row(FontImage.MATERIAL_LINK, "Invite with a link",
                        "Anyone with the link can join", null), "invite", e -> invite(id)));
                page.add(Ui.tap(Ui.row(FontImage.MATERIAL_PERSON_ADD, "Add people", null,
                        null), "addMember", e -> addMember(id, form)));
            }
            for (int iter = 0; members != null && iter < members.size(); iter++) {
                final MemberDto member = members.get(iter);
                Label role = Ui.badge("OWNER".equals(member.role()) ? "Owner"
                        : "ADMIN".equals(member.role()) ? "Admin" : "Member", "PlBadge");
                Container line = Ui.person(member.displayName(), member.username(), role);
                if (admin && !member.username().equals(Messenger.me())
                        && !"OWNER".equals(member.role())) {
                    page.add(Ui.tap(line, "member-" + member.username(),
                            e -> memberOptions(id, member, form)));
                } else {
                    page.add(line);
                }
            }
        }

        Ui.section(page, "Privacy");
        if (!Store.bridged(id)) {
            page.add(Ui.tap(Ui.row(FontImage.MATERIAL_VERIFIED_USER, "Safety numbers",
                    "Check that nobody is in the middle", null), "safety",
                    e -> safety(id, form)));
        }
        if (admin && !direct && !"BOT".equals(row.bridge())) {
            page.add(Ui.tap(Ui.row(FontImage.MATERIAL_LINK, Store.bridged(id)
                    ? "Linked to " + row.bridge() : "Link to Telegram",
                    "A bridged chat is not end-to-end encrypted", null), "bridge",
                    e -> bridge(row, form)));
        }

        Ui.section(page, "");
        if (direct && peer != null) {
            page.add(Ui.tap(Ui.row(FontImage.MATERIAL_BLOCK, "Block", null, null), "block",
                    e -> Api.contacts().block(peer, Net.to(done -> {
                        Telemetry.blocked(true);
                        Ui.say("Blocked");
                    }, Ui::fail))));
        }
        page.add(Ui.tap(Ui.row(FontImage.MATERIAL_REPORT, "Report", null, null), "report",
                e -> report(id, peer, form)));
        page.add(Ui.tap(Ui.row(FontImage.MATERIAL_DELETE_SWEEP, "Clear history",
                "On this device only", null), "clear", e -> {
                    Store.wipe(id);
                    Ui.say("Cleared");
                }));
        if (!direct) {
            page.add(Ui.tap(Ui.row(FontImage.MATERIAL_LOGOUT, "Leave group", null, null),
                    "leave", e -> Api.conversations().leave(id, Net.to(left -> {
                        Store.wipe(id);
                        Messenger.refreshConversations();
                        Nav.home();
                    }, Ui::fail))));
        }
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    private static void edit(ConversationDto row, String title, String about, int seconds,
            boolean adminsOnly, final Runnable done) {
        Api.conversations().edit(row.id(), new ConversationEditDto(title, about, seconds,
                adminsOnly), Net.to(saved -> {
                    Store.conversation(saved, true);
                    if (done != null) {
                        done.run();
                    }
                }, Ui::fail));
    }

    private static void invite(String id) {
        Api.conversations().invite(id, Net.to(made -> {
            String link = "parley://join/" + made.code();
            Display.getInstance().copyToClipboard(link);
            Telemetry.inviteShared();
            Display.getInstance().share(Lang.tr("Join {0} on Parley: {1}", made.title(), link));
        }, Ui::fail));
    }

    private static void addMember(final String id, final Form previous) {
        final Form form = Ui.form("Add people", "AddMember");
        Ui.back(form, previous);
        Container page = Ui.page();
        final com.codename1.ui.TextField who = Ui.field(page, "Username", "username",
                "Their Parley username");
        page.add(Ui.primary("Add", "add", e -> Api.conversations().addMember(id,
                new MemberChangeDto(who.getText().trim(), "MEMBER"), Net.to(saved -> {
                    Store.conversation(saved, true);
                    previous.showBack();
                }, Ui::fail))));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    private static void memberOptions(final String id, final MemberDto member,
            final Form previous) {
        final Form form = Ui.form(member.displayName(), "Member");
        Ui.back(form, previous);
        Container page = Ui.page();
        boolean isAdmin = "ADMIN".equals(member.role());
        page.add(Ui.secondary(isAdmin ? "Remove admin" : "Make admin", "toggleAdmin",
                e -> Api.conversations().setRole(id, member.username(),
                        new MemberChangeDto(member.username(), isAdmin ? "MEMBER" : "ADMIN"),
                        Net.to(saved -> {
                            Store.conversation(saved, true);
                            previous.showBack();
                        }, Ui::fail))));
        page.add(Ui.danger("Remove from group", "removeMember",
                e -> Api.conversations().removeMember(id, member.username(), Net.to(saved -> {
                    Store.conversation(saved, true);
                    previous.showBack();
                }, Ui::fail))));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// One number per device of everyone else in the chat that this device has
    /// a session with: compared in person, or read out on a call, a match
    /// means the keys are the ones the other phone really holds.
    private static void safety(final String id, final Form previous) {
        final Form form = Ui.form("Safety numbers", "Safety");
        Ui.back(form, previous);
        final Container page = Ui.page();
        page.setName("safetyNumbers");
        page.add(Ui.text("Compare these with the other person's screen. If they match, "
                + "nobody can read your messages but the two of you.", "PlMuted"));
        form.add(BorderLayout.CENTER, page);
        Telemetry.safetyNumberViewed();
        Api.conversations().devices(id, Net.to(devices -> {
            String me = Messenger.me();
            int shown = 0;
            for (DeviceAddressDto device : devices) {
                if (device.username().equals(me)) {
                    continue;
                }
                byte[] theirs = Sessions.identity(device.username(), device.deviceId());
                if (theirs == null) {
                    continue;
                }
                shown++;
                Ui.section(page, device.username());
                SpanLabel number = new SpanLabel(SafetyNumber.of(me, Identity.publicKey(),
                        device.username(), theirs), "PlSafety");
                number.setTextUIID("PlSafety");
                number.setName("safety-" + device.username() + "-" + device.deviceId());
                page.add(number);
            }
            if (shown == 0) {
                page.add(Ui.text("Send a message first: the numbers come from the keys the "
                        + "chat was set up with.", "PlMuted"));
            }
            form.revalidate();
        }, Ui::fail));
        form.show();
    }

    private static void report(final String id, final String peer, final Form previous) {
        Ui.reason(previous, "Report", "The admins see the reason, and the last few messages "
                + "of this chat as your device shows them. Nothing else of it ever leaves "
                + "your device.", "Report", why -> {
                    StringBuilder evidence = new StringBuilder();
                    List<Message> recent = Store.messages(id);
                    for (int iter = Math.max(0, recent.size() - 5); iter < recent.size(); iter++) {
                        Message each = recent.get(iter);
                        evidence.append(each.from).append(": ").append(Store.preview(each))
                                .append('\n');
                    }
                    Api.contacts().report(new ReportDto(peer, id, why, evidence.toString()),
                            Net.to(done -> {
                                Telemetry.reported();
                                Ui.say("Reported. Thank you.");
                                previous.showBack();
                            }, Ui::fail));
                });
    }

    private static void bridge(final ConversationDto row, final Form previous) {
        final Form form = Ui.form("Bridge", "Bridge");
        Ui.back(form, previous);
        final Container page = Ui.page();
        page.add(Ui.callout(FontImage.MATERIAL_WARNING, "Not end-to-end encrypted",
                "Once linked, messages in this chat go through the server as plain text so "
                + "it can pass them to the other network. Earlier messages stay encrypted."));
        form.add(BorderLayout.CENTER, page);
        Api.bridges().status(Net.to(status -> {
            if (!status.telegram()) {
                page.add(Ui.text("Bridges are not set up on this server.", "PlMuted"));
                form.revalidate();
                return;
            }
            page.add(Ui.text(Lang.tr("Add @{0} to the Telegram group, send it /id, and type "
                    + "the number it answers with here.", status.telegramBot()), "PlText"));
            final com.codename1.ui.TextField chat = Ui.field(page, "Telegram chat id", "chatId",
                    "-100...");
            page.add(Ui.danger("Link", "link", e -> Api.bridges().link(new BridgeRequestDto(
                    row.id(), "TELEGRAM", chat.getText().trim(), row.title()), Net.to(link -> {
                        Telemetry.bridgeLinked("telegram");
                        Messenger.refreshConversations();
                        previous.showBack();
                    }, Ui::fail))));
            form.revalidate();
        }, Ui::fail));
        form.show();
    }
}
