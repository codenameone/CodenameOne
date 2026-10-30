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
package com.codenameone.examples.pulse.member;

import com.codename1.qrcode.QrCode;
import com.codename1.qrcode.QrCodeImage;
import com.codename1.security.Hash;
import com.codename1.security.Otp;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.pulse.api.MemberDto;
import com.codenameone.examples.pulse.api.PassDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ring;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The membership pass: a QR code the front desk scans, which changes every
/// period so a screenshot of it opens the door for half a minute at most.
///
/// The phone and the server share a secret per member, and both work the code
/// out the same way: `com.codename1.security.Otp.hotp` over the number of
/// periods since the epoch, SHA-256, as many digits as the pass says (the
/// server's `studio/Passes.code`). The phone needs no network to show a fresh
/// one; it fetched the secret once. The QR code carries `pulse:<member>:<code>`,
/// which is all the desk sends back.
///
/// The pass also shows the member's NFC id, the one a key fob at the door is
/// written with. Rotating the pass makes the server forget the old secret and
/// fob id, for a phone that was lost.
public final class PassForm {
    /// What the QR code starts with, as the server's `Passes.PREFIX`.
    public static final String PREFIX = "pulse:";

    private final Form form = Ui.form("Membership pass", "Pass");
    private final Container body = Ui.page();
    private final Label qr = new Label("", "SvPassQr");
    private final Label digits = new Label("", "SvPassCode");
    private final Ring ring = new Ring();
    private PassDto pass;
    private byte[] secret;
    private long shownCounter = -1;
    private UITimer timer;

    private PassForm() {
    }

    /// @param memberId whose pass, or null for the caller's own
    public static void show(final Form previous, final String memberId) {
        new PassForm().open(previous, memberId);
    }

    private void open(final Form previous, final String memberId) {
        Ui.back(form, () -> {
            stop();
            previous.showBack();
        });
        qr.setName("pass-qr");
        digits.setName("pass-code");
        form.add(BorderLayout.CENTER, body);
        body.add(new com.codenameone.examples.pulse.ui.Skeleton(3));
        form.show();
        if (memberId != null) {
            load(memberId);
            return;
        }
        Api.household().members(Net.to(members -> load(self(members)), Ui::fail));
    }

    private static String self(List<MemberDto> members) {
        for (int iter = 0; members != null && iter < members.size(); iter++) {
            if ("SELF".equals(members.get(iter).relation())) {
                return members.get(iter).id();
            }
        }
        return members == null || members.isEmpty() ? "" : members.get(0).id();
    }

    private void load(String memberId) {
        Api.household().pass(memberId, Net.to(this::showPass, Ui::fail));
    }

    private void showPass(PassDto found) {
        pass = found;
        secret = bytes(found.secret());
        shownCounter = -1;
        body.removeAll();
        // The pass is a card, like the one in a wallet: who it is for, the
        // code on a white panel so any scanner reads it, and the digits for a
        // desk whose scanner is down.
        Container card = new Container(BoxLayout.y(), "SvInk");
        card.setName("pass-card");
        Container head = new Container(new BorderLayout(), "SvInkRow");
        head.add(BorderLayout.CENTER, Ui.label("PULSE MEMBER", "SvInkEyebrow"));
        Label mark = new Label("", "SvWelcomeIcon");
        Ui.icon(mark, FontImage.MATERIAL_MONITOR_HEART, 4f);
        head.add(BorderLayout.EAST, mark);
        card.add(head);
        card.add(Ui.label(found.memberName(), "SvInkTitle"));
        Container panel = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE), "SvPassPanel");
        panel.add(BorderLayout.CENTER, qr);
        card.add(panel);
        Container code = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER),
                "SvInkRow");
        code.add(BorderLayout.CENTER, digits);
        code.add(BorderLayout.EAST, ring);
        card.add(code);
        body.add(card);
        body.add(Ui.text(Lang.tr("Show this at the front desk. It changes every {0} seconds, "
                + "so a screenshot of it is no use to anyone.",
                Integer.valueOf(found.periodSeconds())), "SvMuted"));
        body.add(Ui.text(Lang.tr("Key fob {0}", fob(found.nfcId())), "SvMuted"));
        body.add(Ui.link("Lost your phone or fob? Replace the pass", "rotate",
                e -> Api.household().rotatePass(found.memberId(), Net.to(rotated -> {
                    Ui.say("Your old pass and fob no longer open the door");
                    showPass(rotated);
                }, Ui::fail))));
        tick();
        if (timer == null) {
            timer = UITimer.timer(1000, true, form, this::tick);
        }
        Ui.refresh(form);
    }

    private void tick() {
        if (pass == null) {
            return;
        }
        long now = System.currentTimeMillis();
        int period = pass.periodSeconds() <= 0 ? 30 : pass.periodSeconds();
        long counter = now / 1000L / period;
        if (counter != shownCounter) {
            shownCounter = counter;
            String code = code(secret, counter, pass.digits());
            qr.setIcon(QrCodeImage.create(QrCode.encode(PREFIX + pass.memberId() + ":" + code,
                    QrCode.ECC_MEDIUM), Math.max(4, CN.convertToPixels(1.2f))));
            digits.setText(code.length() == 8 ? code.substring(0, 4) + " " + code.substring(4)
                    : code);
            ring.start((int) (period - (now / 1000L) % period), period);
            form.revalidate();
        }
    }

    private void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    // The fob's id is long; its first eight characters are what the desk
    // reads off a fob's label to match it.
    private static String fob(String id) {
        if (id == null || id.length() <= 8) {
            return id == null ? "" : id;
        }
        return id.substring(0, 4) + " " + id.substring(4, 8) + "...";
    }

    /// The code a pass shows for `counter`, exactly as the server works it out.
    public static String code(byte[] secret, long counter, int digits) {
        return Otp.hotp(secret, counter, digits <= 0 ? 8 : digits, Hash.SHA256);
    }

    /// Hex, as the server writes the secret, to bytes.
    static byte[] bytes(String hex) {
        if (hex == null) {
            return new byte[0];
        }
        byte[] out = new byte[hex.length() / 2];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) ((nibble(hex.charAt(iter * 2)) << 4)
                    | nibble(hex.charAt(iter * 2 + 1)));
        }
        return out;
    }

    private static int nibble(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return 0;
    }
}
