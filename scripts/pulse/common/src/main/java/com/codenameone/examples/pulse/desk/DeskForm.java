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
package com.codenameone.examples.pulse.desk;

import com.codename1.codescan.CodeScanner;
import com.codename1.codescan.ScanResult;
import com.codename1.nfc.Nfc;
import com.codename1.nfc.NfcReadOptions;
import com.codename1.nfc.Tag;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.pulse.Telemetry;
import com.codenameone.examples.pulse.api.CheckInRequestDto;
import com.codenameone.examples.pulse.api.CheckInResultDto;
import com.codenameone.examples.pulse.api.DeskDayDto;
import com.codenameone.examples.pulse.api.MemberSummaryDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.live.LiveChannel;
import com.codenameone.examples.pulse.member.Classes;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Menu;
import com.codenameone.examples.pulse.ui.Nav;
import com.codenameone.examples.pulse.ui.StatRow;
import com.codenameone.examples.pulse.ui.Ui;

/// The front desk, as a kiosk: a tablet at the door that members scan their
/// pass at or tap their key fob on.
///
/// Three ways in, all answered by the server's `/api/desk/checkin`:
///
/// - the QR code on the member's pass, read with `com.codename1.codescan`;
/// - the NFC fob, read with `com.codename1.nfc` -- its id, as hex, is what
///   the server knows the fob by;
/// - a name looked up by the staff member at the desk.
///
/// Admitted or refused, the answer fills the screen with the reason the server
/// gave (past due, frozen, no class booked, a pass already used), so the
/// person at the door knows what to say. Below it is today: who came in, the
/// walk-ins and the next classes, kept fresh by the live channel.
public class DeskForm extends LiveChannel.Adapter implements Nav.Home {
    private final Form form = Ui.home("Desk");
    private final Menu menu = new Menu(form, Nav.DESK);
    private final Container verdict = Ui.card();
    private final Container today = new Container(BoxLayout.y());
    private String locationId = "";

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        menu.item(FontImage.MATERIAL_QR_CODE_SCANNER, "Check in", "desk", () -> form.showBack());
        menu.item(FontImage.MATERIAL_PERSON_ADD, "Walk-in", "walkin",
                () -> WalkInForm.show(form, locationId, this::loadToday));
        menu.item(FontImage.MATERIAL_SEARCH, "Find a member", "lookup", () -> lookup());

        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, menu.flatButton());
        bar.add(BorderLayout.CENTER, Ui.label("Front desk", "SvTitle"));

        Container page = Ui.page();
        page.setName("desk");
        // Letting people in is the desk's whole job, so the two ways to do it
        // are the biggest things on the screen; scanning, the common one, is
        // the filled one.
        Container buttons = new Container(new GridLayout(1, 2));
        buttons.add(deskAction(FontImage.MATERIAL_QR_CODE_SCANNER, "Scan pass", "scan",
                "SvFrontScan", e -> scan()));
        buttons.add(deskAction(FontImage.MATERIAL_NFC, "Tap fob", "nfc", "SvFrontFob",
                e -> readFob()));
        page.add(buttons);

        // For a kiosk with no camera, and for the simulator: the code typed
        // in, as the scanner would have read it.
        final TextField typed = Ui.field(page, "Or type the pass", "code", "Pass number");
        typed.addActionListener(e -> {
            String text = typed.getText().trim();
            typed.setText("");
            if (text.length() > 0) {
                checkIn(new CheckInRequestDto(text, null, null, locationId), "qr");
            }
        });
        verdict.setName("verdict");
        verdict.add(Ui.text("Waiting for the next member.", "SvMuted"));
        today.setName("today");
        page.add(verdict).add(today);
        page.add(Ui.secondary("Walk-in", "walkin-button",
                e -> WalkInForm.show(form, locationId, this::loadToday)));

        Container shell = new Container(new BorderLayout());
        shell.add(BorderLayout.NORTH, bar);
        shell.add(BorderLayout.CENTER, page);
        form.add(shell);
        menu.install();
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            loadToday();
        });
        Api.studio().locations(Net.to(locations -> {
            if (locations != null && !locations.isEmpty()) {
                locationId = locations.get(0).id();
                loadToday();
            }
        }, Ui::fail));
    }

    @Override
    public void checkedIn(String location, String name, boolean admitted) {
        if (locationId.length() == 0 || locationId.equals(location)) {
            loadToday();
        }
    }

    private void scan() {
        CodeScanner scanner = CodeScanner.getInstance();
        if (scanner == null) {
            Ui.say("This device has no scanner. Type the pass instead");
            return;
        }
        scanner.scanQRCode(new ScanResult() {
            @Override
            public void scanCompleted(String contents, String formatName, byte[] rawBytes) {
                CN.callSerially(() -> checkIn(new CheckInRequestDto(contents, null, null,
                        locationId), "qr"));
            }

            @Override
            public void scanCanceled() {
            }

            @Override
            public void scanError(int errorCode, String message) {
                CN.callSerially(() -> Ui.say("The scan failed. Try again"));
            }
        });
    }

    private void readFob() {
        final Nfc nfc = Nfc.getInstance();
        if (nfc == null || !nfc.isSupported() || !nfc.canRead()) {
            Ui.say("This device cannot read NFC");
            return;
        }
        nfc.readTag(new NfcReadOptions().setAlertMessage(Lang.tr("Hold the fob near the top")))
                .ready(tag -> CN.callSerially(() -> fob(tag)))
                .except(failed -> CN.callSerially(() -> Ui.say("The fob could not be read")));
    }

    private void fob(Tag tag) {
        if (tag == null || tag.getId() == null) {
            return;
        }
        checkIn(new CheckInRequestDto(null, hex(tag.getId()), null, locationId), "nfc");
    }

    /// Lower-case hex, as the server writes fob ids.
    static String hex(byte[] bytes) {
        String digits = "0123456789abcdef";
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(digits.charAt((b >> 4) & 0xf)).append(digits.charAt(b & 0xf));
        }
        return out.toString();
    }

    private void checkIn(CheckInRequestDto request, final String how) {
        Api.desk().checkIn(request, Net.to(result -> {
            Telemetry.checkIn(how, result.admitted());
            show(result);
            loadToday();
        }, Ui::fail));
    }

    private void show(CheckInResultDto result) {
        verdict.removeAll();
        verdict.add(Ui.banner(result.admitted() ? FontImage.MATERIAL_CHECK_CIRCLE
                : FontImage.MATERIAL_CANCEL, result.admitted() ? Lang.tr("Welcome in")
                : Lang.tr("Not admitted"), result.admitted()
                ? (result.memberName() == null ? "" : result.memberName())
                : result.reason(), result.admitted() ? "SvBannerIconGood" : "SvBannerIconBad"));
        if (result.memberName() != null) {
            verdict.add(Ui.fact(Lang.tr("Member"), result.memberName()));
        }
        if (result.planName() != null) {
            verdict.add(Ui.fact(Lang.tr("Plan"), result.planName()));
        }
        if (result.sessionName() != null && result.sessionName().length() > 0) {
            verdict.add(Ui.fact(Lang.tr("Class"), result.sessionName() + " "
                    + Ui.when(result.sessionStartsAt())));
        }
        if (result.admitted()) {
            verdict.add(Ui.fact(Lang.tr("Visits this month"),
                    String.valueOf(result.visitsThisMonth())));
        }
        Ui.refresh(form);
    }

    private static Button deskAction(char icon, String text, String name, String uiid,
            ActionListener<ActionEvent> onClick) {
        Button button = Ui.action(icon, text, name, onClick);
        button.setUIID(uiid);
        Ui.icon(button, icon, 7f);
        button.setGap(CN.convertToPixels(1.4f));
        return button;
    }

    private void loadToday() {
        Api.desk().today(locationId, Net.to(this::showToday, (status, message) -> { }));
    }

    private void showToday(DeskDayDto day) {
        if (day == null || !Nav.isHome(form)) {
            return;
        }
        today.removeAll();
        Ui.section(today, "Today");
        StatRow stats = Ui.stats(3);
        stats.stat(String.valueOf(day.checkIns()), Lang.tr("Check-ins"), "checkIns");
        stats.stat(String.valueOf(day.walkIns()), Lang.tr("Walk-ins"), "walkIns");
        stats.stat(Ui.moneyShort(day.walkInCents(), null), Lang.tr("Takings"), "takings");
        today.add(stats);
        if (day.upcoming() != null && !day.upcoming().isEmpty()) {
            Ui.section(today, "Coming up");
        }
        for (int iter = 0; day.upcoming() != null && iter < day.upcoming().size()
                && iter < 4; iter++) {
            SessionDto session = day.upcoming().get(iter);
            // The card already says how many places are left.
            today.add(Classes.card(session, null, null));
        }
        if (day.recent() != null && !day.recent().isEmpty()) {
            Ui.section(today, "Just in");
        }
        for (int iter = 0; day.recent() != null && iter < day.recent().size()
                && iter < 6; iter++) {
            CheckInResultDto recent = day.recent().get(iter);
            String name = recent.memberName() == null ? "" : recent.memberName();
            Container row = Ui.row(FontImage.MATERIAL_PERSON, name,
                    recent.admitted() ? Lang.tr("Checked in") : recent.reason(),
                    Ui.badge(recent.admitted() ? Lang.tr("In") : Lang.tr("Turned away"),
                            recent.admitted() ? "SvBadgeGood" : "SvBadgeBad"));
            BorderLayout layout = (BorderLayout) row.getLayout();
            Component icon = layout.getWest();
            if (icon != null) {
                row.replace(icon, Ui.avatar(name), null);
            }
            today.add(row);
        }
        Ui.refresh(form);
    }

    /// Staff at the desk look a member up by name and check them in by hand,
    /// for a member whose phone is flat.
    private void lookup() {
        final Form search = Ui.form("Find a member", "Lookup");
        Ui.back(search, form);
        Container page = Ui.page();
        final TextField query = Ui.field(page, "Name or e-mail", "query", "");
        final Container results = new Container(com.codename1.ui.layouts.BoxLayout.y());
        query.addActionListener(e -> Api.desk().find(query.getText().trim(), Net.to(found -> {
            results.removeAll();
            for (int iter = 0; found != null && iter < found.size(); iter++) {
                final MemberSummaryDto member = found.get(iter);
                results.add(Ui.tap(Ui.row(FontImage.MATERIAL_PERSON, member.name(),
                        member.planName() == null ? member.membershipState()
                        : member.planName() + Ui.DOT + member.membershipState(), null),
                        "found-" + iter, ev -> {
                            form.showBack();
                            checkIn(new CheckInRequestDto(null, null, member.id(), locationId),
                                    "lookup");
                        }));
            }
            Ui.refresh(search);
        }, Ui::fail)));
        page.add(results);
        search.add(BorderLayout.CENTER, page);
        search.show();
    }
}
