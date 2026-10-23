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
package com.codenameone.examples.bastion.ui;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bastion.api.AdjustmentDto;
import com.codenameone.examples.bastion.api.ApprovalDto;
import com.codenameone.examples.bastion.api.AssetTotalDto;
import com.codenameone.examples.bastion.api.AuditEventDto;
import com.codenameone.examples.bastion.api.BalanceSheetDto;
import com.codenameone.examples.bastion.api.BreakDto;
import com.codenameone.examples.bastion.api.ChainReportDto;
import com.codenameone.examples.bastion.api.ChartAccountDto;
import com.codenameone.examples.bastion.api.ClosePeriodDto;
import com.codenameone.examples.bastion.api.ControlDto;
import com.codenameone.examples.bastion.api.DecisionDto;
import com.codenameone.examples.bastion.api.EvidenceDto;
import com.codenameone.examples.bastion.api.IncomeStatementDto;
import com.codenameone.examples.bastion.api.JournalEntryDto;
import com.codenameone.examples.bastion.api.JournalLegDto;
import com.codenameone.examples.bastion.api.LedgerLineDto;
import com.codenameone.examples.bastion.api.PeriodDto;
import com.codenameone.examples.bastion.api.ReconciliationDto;
import com.codenameone.examples.bastion.api.ReversalRequestDto;
import com.codenameone.examples.bastion.api.StatementLineDto;
import com.codenameone.examples.bastion.api.StatementTotalDto;
import com.codenameone.examples.bastion.api.TAccountDto;
import com.codenameone.examples.bastion.api.TrialBalanceDto;
import com.codenameone.examples.bastion.api.TrialBalanceLineDto;
import com.codenameone.examples.bastion.api.UserDto;
import com.codenameone.examples.bastion.api.VerificationDto;
import com.codenameone.examples.bastion.api.VolumeDayDto;
import com.codenameone.examples.bastion.money.Amounts;
import com.codenameone.examples.bastion.money.Assets;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import com.codenameone.examples.bastion.net.Session;
import com.codenameone.examples.bastion.security.RequestSigner;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/// The back office: the bank's books and its audit log, for its staff.
///
/// One screen for both roles. An auditor reads everything and changes
/// nothing; an administrator also closes periods, raises adjustments and
/// reversals and decides those another administrator raised. What a role
/// may do is the server's rule (`SecurityConfig`, `Approvals`): the buttons
/// shown here follow it, they do not enforce it.
///
/// On a wide window -- a desktop, the web on a monitor, a tablet on its
/// side -- the sections are a sidebar beside the page and the tables use the
/// width. On a phone the sections are a picker over a single column, and a
/// table wider than the phone scrolls sideways.
///
/// Vocabulary is the accountant's: a journal of entries, each a set of debit
/// (Dr) and credit (Cr) legs that sum to zero per currency; a general ledger
/// line per leg with its running balance; a trial balance proving debits
/// equal credits; a balance sheet (assets = liabilities + equity) and an
/// income statement for a period; reconciliation of control accounts to
/// their subledgers, with any break shown in red.
public final class ConsoleForm {
    /// A window at least this wide, in millimetres, gets the sidebar.
    private static final float WIDE_MM = 165f;

    static final String[] SECTIONS = {"Overview", "Journal", "General ledger", "Trial balance",
        "Balance sheet", "Income statement", "Reconciliation", "Period close", "Approvals",
        "Audit chain", "Audit trail", "Access log", "Evidence"};
    private static final char[] ICONS = {FontImage.MATERIAL_DASHBOARD,
        FontImage.MATERIAL_MENU_BOOK, FontImage.MATERIAL_ACCOUNT_TREE,
        FontImage.MATERIAL_BALANCE, FontImage.MATERIAL_ACCOUNT_BALANCE,
        FontImage.MATERIAL_TRENDING_UP, FontImage.MATERIAL_COMPARE_ARROWS,
        FontImage.MATERIAL_EVENT_AVAILABLE, FontImage.MATERIAL_FACT_CHECK,
        FontImage.MATERIAL_LINK, FontImage.MATERIAL_MANAGE_SEARCH,
        FontImage.MATERIAL_BADGE, FontImage.MATERIAL_FILE_DOWNLOAD};

    private static int current;

    private final Form form;
    private final Container pane = new Container(BoxLayout.y());
    private final Container side;
    /// The page of the section on screen, under its title.
    private Container body;
    private final boolean wide;
    private final boolean admin;

    private ConsoleForm(int section) {
        UserDto user = Session.user();
        admin = user != null && user.admin();
        wide = wide();
        form = Nav.form(admin ? "Administration" : "Audit", null);
        form.setName("console");
        pane.setName("console-page");
        if (wide) {
            form.setScrollable(false);
            form.setLayout(new BorderLayout());
            side = new Container(BoxLayout.y(), "BnSide");
            side.setScrollableY(true);
            side.setName("console-sections");
            // The title bar already names the console; the sidebar starts
            // with the sections.
            if (!admin) {
                side.add(new Label(Lang.tr("Read-only"), "BnBadgeWarn"));
            }
            for (int iter = 0; iter < SECTIONS.length; iter++) {
                if (!admin && "Approvals".equals(SECTIONS[iter])) {
                    continue;
                }
                final int index = iter;
                Button item = new Button(Lang.tr(SECTIONS[iter]), "BnSideItem");
                FontImage.setMaterialIcon(item, ICONS[iter], 3.4f);
                item.addActionListener(evt -> open(index));
                side.add(item);
            }
            Container page = new Container(BoxLayout.y(), "BnConsolePage");
            page.setScrollableY(true);
            page.add(pane);
            form.add(BorderLayout.WEST, side);
            form.add(BorderLayout.CENTER, page);
        } else {
            side = null;
            List<String> ids = new ArrayList<String>();
            List<String> labels = new ArrayList<String>();
            for (int iter = 0; iter < SECTIONS.length; iter++) {
                if (!admin && "Approvals".equals(SECTIONS[iter])) {
                    continue;
                }
                ids.add(String.valueOf(iter));
                labels.add(Lang.tr(SECTIONS[iter]));
            }
            final Picker which = Ui.picker(ids.toArray(new String[ids.size()]),
                    labels.toArray(new String[labels.size()]), String.valueOf(section));
            which.setName("console-section");
            which.addActionListener(evt -> {
                String picked = Ui.pickedId(which);
                if (picked != null) {
                    open(Integer.parseInt(picked));
                }
            });
            if (!admin) {
                form.add(FlowLayout.encloseIn(new Label(Lang.tr("Read-only"), "BnBadgeWarn")));
            }
            form.add(which);
            form.add(pane);
        }
        final boolean was = wide;
        form.addSizeChangedListener(evt -> {
            if (wide() != was) {
                CN.callSerially(() -> new ConsoleForm(current).start());
            }
        });
    }

    /// Opens the console on the section shown last.
    public static void show() {
        Assets.load(() -> new ConsoleForm(current).start());
    }

    static boolean wide() {
        return CN.getDisplayWidth() >= CN.convertToPixels(WIDE_MM);
    }

    private void start() {
        Ui.show(form, true);
        open(current);
    }

    private void open(int section) {
        current = section;
        if (side != null) {
            for (int iter = 0; iter < side.getComponentCount(); iter++) {
                Component c = side.getComponentAt(iter);
                if (c instanceof Button) {
                    Button b = (Button) c;
                    boolean on = b.getText().equals(Lang.tr(SECTIONS[section]));
                    b.setUIID(on ? "BnSideItemOn" : "BnSideItem");
                    FontImage.setMaterialIcon(b, ICONS[indexOf(b.getText())], 3.4f);
                }
            }
        }
        String name = SECTIONS[section];
        pane.removeAll();
        pane.add(new Label(Lang.tr(name), "BnConsoleTitle"));
        Container body = new Container(BoxLayout.y());
        this.body = body;
        pane.add(body);
        Ui.loading(body);
        form.revalidate();
        if ("Overview".equals(name)) {
            overview(body);
        } else if ("Journal".equals(name)) {
            journal(body);
        } else if ("General ledger".equals(name)) {
            ledger(body, "2000", Assets.REPORTING);
        } else if ("Trial balance".equals(name)) {
            trialBalance(body);
        } else if ("Balance sheet".equals(name)) {
            balanceSheet(body);
        } else if ("Income statement".equals(name)) {
            incomeStatement(body);
        } else if ("Reconciliation".equals(name)) {
            reconciliation(body);
        } else if ("Period close".equals(name)) {
            periods(body);
        } else if ("Approvals".equals(name)) {
            approvals(body);
        } else if ("Audit chain".equals(name)) {
            chain(body);
        } else if ("Audit trail".equals(name)) {
            trail(body, "");
        } else if ("Access log".equals(name)) {
            accessLog(body);
        } else {
            evidence(body);
        }
    }

    private static int indexOf(String translated) {
        for (int iter = 0; iter < SECTIONS.length; iter++) {
            if (Lang.tr(SECTIONS[iter]).equals(translated)) {
                return iter;
            }
        }
        return 0;
    }

    private void done(Container body) {
        body.revalidate();
        form.revalidate();
    }

    private Net.Failed failed(final Container body, final Runnable retry) {
        return (status, message) -> {
            Ui.failed(body, message, retry);
            form.revalidate();
        };
    }

    // ------------------------------------------------------------- overview

    private void overview(final Container body) {
        body.removeAll();
        final Container tiles = new Container(wide ? new GridLayout(1, 4) : new GridLayout(2, 2));
        final Label trial = tile(tiles, "Trial balance");
        final Label recon = tile(tiles, "Reconciliation");
        final Label chain = tile(tiles, "Audit chain");
        final Label pending = tile(tiles, admin ? "Awaiting approval" : "Journal entries");
        body.add(tiles);
        final Container charts = new Container(wide ? new GridLayout(1, 2) : BoxLayout.y());
        body.add(charts);
        Container checks = new Container(wide ? new GridLayout(1, 2) : BoxLayout.y());
        final Container auditBox = new Container(BoxLayout.y());
        final Container ledgerBox = new Container(BoxLayout.y());
        Button verifyAudit = Ui.primary("Verify the audit log");
        verifyAudit.addActionListener(evt -> {
            Ui.loading(auditBox);
            Api.admin().verifyAudit(Net.to((VerificationDto v) -> verified(auditBox, v,
                    "The audit chain is intact."), failed(auditBox, () -> { })));
        });
        Button verifyLedger = Ui.primary("Verify the ledger");
        verifyLedger.addActionListener(evt -> {
            Ui.loading(ledgerBox);
            Api.admin().verifyLedger(Net.to((VerificationDto v) -> verified(ledgerBox, v,
                    "Every entry balances and every balance matches its postings."),
                    failed(ledgerBox, () -> { })));
        });
        if (admin) {
            body.add(Ui.section("Integrity checks"));
            checks.add(BoxLayout.encloseY(verifyAudit, auditBox));
            checks.add(BoxLayout.encloseY(verifyLedger, ledgerBox));
            body.add(checks);
        }
        done(body);
        Api.books().trialBalance(null, Net.to((TrialBalanceDto tb) -> state(trial,
                tb.balanced(), "Balanced", "Out of balance"), (s, m) -> state(trial, false,
                "", "Unavailable")));
        Api.books().reconciliation(Net.to((ReconciliationDto r) -> state(recon, r.clean(),
                "No breaks", Lang.tr("{0} breaks", String.valueOf(r.breaks().size()))),
                (s, m) -> state(recon, false, "", "Unavailable")));
        Api.audit().chain(Net.to((ChainReportDto c) -> state(chain, c.intact(), "Intact",
                Lang.tr("Broken at #{0}", String.valueOf(c.firstBrokenSeq()))),
                (s, m) -> state(chain, false, "", "Unavailable")));
        if (admin) {
            Api.admin().approvals(Net.to((List<ApprovalDto> list) -> {
                int open = 0;
                for (ApprovalDto a : list) {
                    if ("PENDING".equals(a.status())) {
                        open++;
                    }
                }
                pending.setText(String.valueOf(open));
                pending.setUIID(open == 0 ? "BnTileValue" : "BnTileValueWarn");
                tiles.revalidate();
            }, (s, m) -> pending.setText("-")));
        }
        Api.books().volume("30", Net.to((List<VolumeDayDto> days) -> {
            volumeCharts(charts, days, pending);
        }, (s, m) -> { }));
    }

    private void volumeCharts(Container charts, List<VolumeDayDto> days, Label entries) {
        List<Long> dayList = new ArrayList<Long>();
        for (VolumeDayDto d : days) {
            Long day = Long.valueOf(d.day());
            if (!dayList.contains(day)) {
                dayList.add(day);
            }
        }
        Collections.sort(dayList);
        long[] at = new long[dayList.size()];
        double[] volume = new double[at.length];
        double[] count = new double[at.length];
        int total = 0;
        for (int iter = 0; iter < at.length; iter++) {
            at[iter] = dayList.get(iter).longValue();
        }
        for (VolumeDayDto d : days) {
            int i = dayList.indexOf(Long.valueOf(d.day()));
            count[i] += d.entries();
            total += d.entries();
            if (Assets.REPORTING.equals(d.asset())) {
                volume[i] += d.volume() / 100.0;
            }
        }
        if (!admin) {
            entries.setText(String.valueOf(total));
        }
        charts.removeAll();
        charts.add(panel("Dollar volume, 30 days", Charts.bars("volume-chart", at, volume,
                "BnChartLine", "$", 1.5f)));
        charts.add(panel("Journal entries a day", Charts.bars("entries-chart", at, count,
                "BnChartIn", "", 1.5f)));
        charts.revalidate();
        form.revalidate();
    }

    private static Container panel(String title, Component chart) {
        Container box = new Container(BoxLayout.y(), "BnPanel");
        box.add(new Label(Lang.tr(title), "BnPanelTitle"));
        box.add(chart);
        return box;
    }

    private static Label tile(Container tiles, String caption) {
        Container tile = new Container(BoxLayout.y(), "BnTile");
        Label value = new Label("...", "BnTileValue");
        tile.add(value);
        tile.add(new Label(Lang.tr(caption), "BnTileLabel"));
        tiles.add(tile);
        return value;
    }

    private void state(Label value, boolean good, String yes, String no) {
        value.setText(Lang.tr(good ? yes : no));
        value.setUIID(good ? "BnTileValueGood" : "BnTileValueBad");
        value.getParent().revalidate();
    }

    private void verified(Container box, VerificationDto v, String good) {
        box.removeAll();
        if (v.intact()) {
            // The sentence wraps; a row's title would cut it off.
            Container line = new Container(new BorderLayout(), "BnRow");
            SpanLabel says = new SpanLabel(Lang.tr(good));
            says.setTextUIID("BnRowTitle");
            line.add(BorderLayout.CENTER, says);
            line.add(BorderLayout.EAST, BorderLayout.centerCenter(
                    new Label(Lang.tr("Intact"), "BnBadgeGood")));
            box.add(line);
        } else {
            box.add(Ui.row(Lang.tr("Problems found"), null, String.valueOf(v.problems().size()),
                    "BnBadgeBad", null));
            for (String problem : v.problems()) {
                box.add(Ui.muted(problem));
            }
        }
        done(box);
    }

    // -------------------------------------------------------------- journal

    private void journal(final Container body) {
        Api.books().journal(null, null, "60", Net.to((List<JournalEntryDto> entries) -> {
            body.removeAll();
            body.add(Ui.muted(Lang.tr("Every entry, newest first. Debits and credits of an "
                    + "entry sum to the same amount in each currency. Tap an entry for its legs.")));
            Table table = new Table(new String[] {"Date", "Entry", "Account", "Debit (Dr)",
                "Credit (Cr)"}, new int[] {4, 3, 8, 3, 3},
                    new boolean[] {false, false, false, true, true});
            for (final JournalEntryDto e : entries) {
                table.add(new String[] {when(e.at()), shortId(e.id()), kindName(e.kind())
                    + (e.memo() == null || e.memo().length() == 0 ? "" : " - " + e.memo()),
                    "", ""}, "BnTableGroup", null, () -> entry(body, e.id()));
                for (JournalLegDto leg : e.legs()) {
                    boolean credit = leg.credit() != 0;
                    table.add(new String[] {"", "", (credit ? "      " : "") + leg.code() + " "
                        + leg.accountName() + owner(leg.owner()),
                        dr(leg.debit(), leg.asset()), dr(leg.credit(), leg.asset())}, null);
                }
            }
            if (entries.isEmpty()) {
                table.note(Lang.tr("No entries yet."));
            }
            body.add(table.component());
            done(body);
        }, failed(body, () -> journal(body))));
    }

    /// One journal entry with all of its legs.
    private void entry(final Container body, final String id) {
        Ui.loading(body);
        Api.books().entry(id, Net.to((JournalEntryDto e) -> {
            body.removeAll();
            Button back = Ui.secondary("All entries");
            back.addActionListener(evt -> journal(body));
            body.add(FlowLayout.encloseIn(back));
            body.add(new Label(Lang.tr("Journal entry {0}", shortId(e.id())), "BnSubTitle"));
            Container facts = new Container(new GridLayout(wide ? 2 : 1, wide ? 3 : 1));
            facts.add(fact("Posted", when(e.at())));
            facts.add(fact("Type", kindName(e.kind())));
            facts.add(fact("Initiated by", e.initiator()));
            facts.add(fact("Memo", e.memo()));
            facts.add(fact("Reverses", e.reverses() == null ? "-" : shortId(e.reverses())));
            facts.add(fact("Reversed by", e.reversedBy() == null ? "-" : shortId(e.reversedBy())));
            body.add(facts);
            Table legs = new Table(new String[] {"Code", "Account", "Owner", "Asset",
                "Debit (Dr)", "Credit (Cr)"}, new int[] {2, 6, 4, 2, 3, 3},
                    new boolean[] {false, false, false, false, true, true});
            List<String> assets = new ArrayList<String>();
            for (JournalLegDto leg : e.legs()) {
                legs.add(new String[] {leg.code(), leg.accountName(), leg.owner(), leg.asset(),
                    dr(leg.debit(), leg.asset()), dr(leg.credit(), leg.asset())}, null);
                if (!assets.contains(leg.asset())) {
                    assets.add(leg.asset());
                }
            }
            boolean balanced = true;
            for (String asset : assets) {
                long d = 0;
                long c = 0;
                for (JournalLegDto leg : e.legs()) {
                    if (asset.equals(leg.asset())) {
                        d += leg.debit();
                        c += leg.credit();
                    }
                }
                balanced &= d == c;
                legs.add(new String[] {"", Lang.tr("Total"), "", asset,
                    Amounts.format(d, asset), Amounts.format(c, asset)}, "BnTableTotal", null, null);
            }
            body.add(legs.component());
            body.add(FlowLayout.encloseIn(new Label(Lang.tr(balanced ? "Balanced" : "Out of balance"),
                    balanced ? "BnBadgeGood" : "BnBadgeBad")));
            if (admin && e.reversedBy() == null && !"REVERSAL".equals(e.kind())) {
                Button reverse = Ui.secondary("Reverse this entry");
                reverse.addActionListener(evt -> reverse(body, e));
                body.add(reverse);
            }
            Button trail = Ui.secondary("Audit trail of this entry");
            trail.addActionListener(evt -> {
                current = indexOfSection("Audit trail");
                openTrail(e.id());
            });
            body.add(trail);
            done(body);
        }, failed(body, () -> entry(body, id))));
    }

    private void openTrail(String subject) {
        open(current);
        trail(body, subject);
    }

    private static int indexOfSection(String name) {
        for (int iter = 0; iter < SECTIONS.length; iter++) {
            if (SECTIONS[iter].equals(name)) {
                return iter;
            }
        }
        return 0;
    }

    private void reverse(final Container body, final JournalEntryDto e) {
        TextField memo = Ui.field("Reason", TextField.ANY);
        TextField password = password();
        Command go = new Command(Lang.tr("Request reversal"));
        if (Dialog.show(Lang.tr("Reverse entry {0}", shortId(e.id())),
                BoxLayout.encloseY(Ui.muted(Lang.tr("A second administrator must approve it.")),
                        memo, password), go, new Command(Lang.tr("Cancel"))) != go) {
            return;
        }
        Api.admin().reverse(e.id(), RequestSigner.idempotencyKey(),
                new ReversalRequestDto(memo.getText(), password.getText()),
                Net.to((ApprovalDto a) -> {
                    Ui.toast("Sent for approval.");
                    entry(body, e.id());
                }, (s, m) -> Dialog.show(Lang.tr("Not done"), Lang.tr(m), Lang.tr("OK"), null)));
    }

    // ------------------------------------------------------- general ledger

    private static final int LEDGER_ROWS = 60;

    private void ledger(final Container body, final String code, final String asset) {
        Api.books().chart(Net.to((List<ChartAccountDto> chart) -> Api.books().generalLedger(code,
                asset, null, null, Net.to((List<LedgerLineDto> lines) -> {
                    body.removeAll();
                    String[] ids = new String[chart.size()];
                    String[] names = new String[chart.size()];
                    for (int iter = 0; iter < ids.length; iter++) {
                        ids[iter] = chart.get(iter).code();
                        names[iter] = chart.get(iter).code() + " " + chart.get(iter).name();
                    }
                    final Picker account = Ui.picker(ids, names, code);
                    Assets.Asset[] all = Assets.all();
                    String[] codes = new String[all.length];
                    for (int iter = 0; iter < codes.length; iter++) {
                        codes[iter] = all[iter].code;
                    }
                    final Picker currency = Ui.picker(codes, codes, asset);
                    account.addActionListener(evt -> ledger(body, Ui.pickedId(account),
                            Ui.pickedId(currency)));
                    currency.addActionListener(evt -> ledger(body, Ui.pickedId(account),
                            Ui.pickedId(currency)));
                    Container pickers = new Container(new BorderLayout());
                    pickers.add(BorderLayout.CENTER, account);
                    pickers.add(BorderLayout.EAST, currency);
                    body.add(pickers);
                    Table table = new Table(new String[] {"Date", "Entry", "Memo", "Owner",
                        "Debit (Dr)", "Credit (Cr)", "Balance"}, new int[] {3, 2, 5, 3, 3, 3, 3},
                            new boolean[] {false, false, false, false, true, true, true});
                    // A busy control account has thousands of postings; the
                    // page shows the latest, which is where a reviewer starts.
                    int first = Math.max(0, lines.size() - LEDGER_ROWS);
                    if (first > 0) {
                        table.note(Lang.tr("Showing the latest {0} of {1} postings.",
                                String.valueOf(LEDGER_ROWS), String.valueOf(lines.size())));
                    }
                    for (final LedgerLineDto line : lines.subList(first, lines.size())) {
                        table.add(new String[] {when(line.at()), shortId(line.entryId()),
                            line.memo() == null || line.memo().length() == 0
                                    ? kindName(line.kind()) : line.memo(), line.owner(),
                            dr(line.debit(), line.asset()), dr(line.credit(), line.asset()),
                            Amounts.format(line.balance(), line.asset())}, () -> {
                                current = indexOfSection("Journal");
                                open(current);
                                entry(this.body, line.entryId());
                            });
                    }
                    if (lines.isEmpty()) {
                        table.note(Lang.tr("No postings to this account in this currency."));
                    }
                    body.add(table.component());
                    if (!lines.isEmpty()) {
                        final String accountId = lines.get(0).accountId();
                        Button t = Ui.secondary("Show as a T-account");
                        t.addActionListener(evt -> tAccount(body, accountId, code, asset));
                        Container actions = new Container(new FlowLayout());
                        actions.add(t);
                        // Adjustments are to customer money only (`Approvals`):
                        // the bank's own accounts move by the entries that
                        // explain them.
                        if (admin && code.startsWith("2") && !"2200".equals(code)) {
                            Button adjust = Ui.secondary("Raise an adjustment");
                            adjust.addActionListener(evt -> adjust(accountId, asset));
                            actions.add(adjust);
                        }
                        body.add(actions);
                    }
                    done(body);
                }, failed(body, () -> ledger(body, code, asset)))),
                failed(body, () -> ledger(body, code, asset))));
    }

    private void tAccount(final Container body, final String accountId, final String code,
            final String asset) {
        Ui.loading(body);
        Api.books().tAccount(accountId, null, null, Net.to((TAccountDto t) -> {
            body.removeAll();
            Button back = Ui.secondary("General ledger");
            back.addActionListener(evt -> ledger(body, code, asset));
            body.add(FlowLayout.encloseIn(back));
            body.add(new Label(t.code() + " " + t.name() + " (" + t.asset() + ")", "BnSubTitle"));
            body.add(Ui.muted(Lang.tr("{0} account, normal balance {1}.",
                    className(t.accountClass()), side(t.normalBalance()))));
            Container head = new Container(new GridLayout(1, 2), "BnTHead");
            head.add(new Label(Lang.tr("Debit (Dr)"), "BnTSide"));
            head.add(new Label(Lang.tr("Credit (Cr)"), "BnTSide"));
            body.add(head);
            Container columns = new Container(new GridLayout(1, 2));
            Container debits = new Container(BoxLayout.y(), "BnTDebit");
            Container credits = new Container(BoxLayout.y(), "BnTCredit");
            boolean debitNormal = "DEBIT".equals(t.normalBalance());
            Container opening = tLine(Lang.tr("Opening balance"),
                    Amounts.format(Math.abs(t.openingBalance()), t.asset()), "BnTLineTotal");
            (debitNormal ? debits : credits).add(opening);
            for (LedgerLineDto line : t.debits()) {
                debits.add(tLine(when(line.at()) + "  " + shortId(line.entryId()),
                        Amounts.format(line.debit(), line.asset()), "BnTLine"));
            }
            for (LedgerLineDto line : t.credits()) {
                credits.add(tLine(when(line.at()) + "  " + shortId(line.entryId()),
                        Amounts.format(line.credit(), line.asset()), "BnTLine"));
            }
            columns.add(debits);
            columns.add(credits);
            body.add(columns);
            Container totals = new Container(new GridLayout(1, 2), "BnTFoot");
            totals.add(tLine(Lang.tr("Total debits"), Amounts.format(t.totalDebits(), t.asset()),
                    "BnTLineTotal"));
            totals.add(tLine(Lang.tr("Total credits"), Amounts.format(t.totalCredits(), t.asset()),
                    "BnTLineTotal"));
            body.add(totals);
            body.add(Ui.row(Lang.tr("Closing balance"), side(t.normalBalance()),
                    Amounts.format(t.closingBalance(), t.asset()), "BnRowValue", null));
            done(body);
        }, failed(body, () -> tAccount(body, accountId, code, asset))));
    }

    private static Container tLine(String left, String right, String uiid) {
        Container line = new Container(new BorderLayout(), uiid);
        line.add(BorderLayout.CENTER, new Label(left, "BnCell"));
        line.add(BorderLayout.EAST, new Label(right, "BnCellNum"));
        return line;
    }

    private void adjust(final String accountId, final String asset) {
        TextField amount = Ui.field("Amount (negative to debit)", TextField.ANY);
        TextField memo = Ui.field("Reason", TextField.ANY);
        TextField password = password();
        Command go = new Command(Lang.tr("Request adjustment"));
        if (Dialog.show(Lang.tr("Adjustment"), BoxLayout.encloseY(
                Ui.muted(Lang.tr("A second administrator must approve it.")), amount, memo,
                password), go, new Command(Lang.tr("Cancel"))) != go) {
            return;
        }
        String text = amount.getText().trim();
        boolean negative = text.startsWith("-");
        long units = Amounts.parse(negative ? text.substring(1) : text, asset);
        if (units <= 0) {
            Dialog.show(Lang.tr("Amount"), Ui.amountHint(), Lang.tr("OK"), null);
            return;
        }
        Api.admin().adjust(RequestSigner.idempotencyKey(), new AdjustmentDto(accountId,
                negative ? -units : units, memo.getText(), password.getText()),
                Net.to((ApprovalDto a) -> Ui.toast("Sent for approval."),
                        (s, m) -> Dialog.show(Lang.tr("Not done"), Lang.tr(m), Lang.tr("OK"),
                                null)));
    }

    // ----------------------------------------------------------- statements

    private void trialBalance(final Container body) {
        Api.books().trialBalance(null, Net.to((TrialBalanceDto tb) -> {
            body.removeAll();
            body.add(FlowLayout.encloseIn(new Label(Lang.tr(tb.balanced() ? "Balanced"
                    : "Out of balance"), tb.balanced() ? "BnBadgeGood" : "BnBadgeBad"),
                    new Label(Lang.tr("As of {0}", when(tb.asOf())), "BnCellMuted")));
            Table table = new Table(new String[] {"Code", "Account", "Class", "Asset",
                "Debit (Dr)", "Credit (Cr)"}, new int[] {2, 7, 3, 2, 3, 3},
                    new boolean[] {false, false, false, false, true, true});
            for (TrialBalanceLineDto line : tb.lines()) {
                table.add(new String[] {line.code(), line.name(), className(line.accountClass()),
                    line.asset(), dr(line.debit(), line.asset()), dr(line.credit(), line.asset())},
                        null);
            }
            for (AssetTotalDto total : tb.totals()) {
                table.add(new String[] {"", Lang.tr("Total"), "", total.asset(),
                    Amounts.format(total.debits(), total.asset()),
                    Amounts.format(total.credits(), total.asset())},
                        total.balanced() ? "BnTableTotal" : "BnTableRowBad", null, null);
            }
            body.add(table.component());
            done(body);
        }, failed(body, () -> trialBalance(body))));
    }

    private void balanceSheet(final Container body) {
        Api.books().balanceSheet(null, Net.to((BalanceSheetDto bs) -> {
            body.removeAll();
            body.add(FlowLayout.encloseIn(new Label(Lang.tr(bs.balanced()
                    ? "Assets = liabilities + equity" : "Out of balance"),
                    bs.balanced() ? "BnBadgeGood" : "BnBadgeBad"),
                    new Label(Lang.tr("As of {0}", when(bs.asOf())), "BnCellMuted")));
            body.add(Ui.row(Lang.tr("Total assets"), Lang.tr("Valued in dollars"),
                    Amounts.format(bs.totalAssetsUsdCents(), "USD"), "BnRowValue", null));
            statement(body, bs.lines(), bs.totals(), new String[] {"ASSETS", "LIABILITIES", "EQUITY"});
            done(body);
        }, failed(body, () -> balanceSheet(body))));
    }

    private void incomeStatement(final Container body) {
        Api.books().incomeStatement(null, null, Net.to((IncomeStatementDto is) -> {
            body.removeAll();
            body.add(new Label(Lang.tr("{0} to {1}", when(is.from()), when(is.to())),
                    "BnCellMuted"));
            body.add(Ui.row(Lang.tr("Net income"), Lang.tr("Valued in dollars"),
                    Amounts.format(is.netIncomeUsdCents(), "USD"),
                    is.netIncomeUsdCents() >= 0 ? "BnAmountIn" : "BnAmountNegative", null));
            statement(body, is.lines(), is.totals(), new String[] {"REVENUE", "EXPENSES"});
            done(body);
        }, failed(body, () -> incomeStatement(body))));
    }

    private void statement(Container body, List<StatementLineDto> lines,
            List<StatementTotalDto> totals, String[] sections) {
        for (String section : sections) {
            body.add(Ui.section(className(section)));
            Table table = new Table(new String[] {"Code", "Account", "Asset", "Amount",
                "In dollars"}, new int[] {2, 8, 2, 4, 4},
                    new boolean[] {false, false, false, true, true});
            for (StatementLineDto line : lines) {
                if (section.equals(line.section())) {
                    table.add(new String[] {line.code(), line.name(), line.asset(),
                        Amounts.format(line.amount(), line.asset()),
                        Amounts.format(line.usdCents(), "USD")}, null);
                }
            }
            for (StatementTotalDto total : totals) {
                if (section.equals(total.section())) {
                    table.add(new String[] {"", Lang.tr("Total"), total.asset(),
                        Amounts.format(total.amount(), total.asset()),
                        Amounts.format(total.usdCents(), "USD")}, "BnTableTotal", null, null);
                }
            }
            if (table.size() == 0) {
                table.note(Lang.tr("Nothing in this section."));
            }
            body.add(table.component());
        }
    }

    private void reconciliation(final Container body) {
        Api.books().reconciliation(Net.to((ReconciliationDto r) -> {
            body.removeAll();
            body.add(FlowLayout.encloseIn(new Label(r.clean() ? Lang.tr("No breaks")
                    : Lang.tr("{0} breaks", String.valueOf(r.breaks().size())),
                    r.clean() ? "BnBadgeGood" : "BnBadgeBad"),
                    new Label(Lang.tr("As of {0}", when(r.at())), "BnCellMuted")));
            body.add(Ui.muted(Lang.tr("Each control account against the sum of its subledger. "
                    + "A difference is a break and is shown in red.")));
            Table controls = new Table(new String[] {"Asset", "Code", "Account", "Control",
                "Subledger", "Difference"}, new int[] {2, 2, 6, 3, 3, 3},
                    new boolean[] {false, false, false, true, true, true});
            for (ControlDto c : r.controls()) {
                controls.add(new String[] {c.asset(), c.code(), c.name(),
                    Amounts.format(c.control(), c.asset()), Amounts.format(c.subledger(), c.asset()),
                    Amounts.format(c.difference(), c.asset())},
                        c.difference() == 0 ? "BnTableRow" : "BnTableRowBad", null, null);
            }
            body.add(controls.component());
            body.add(Ui.section("Breaks"));
            Table breaks = new Table(new String[] {"Type", "Asset", "Reference", "Expected",
                "Actual", "Difference"}, new int[] {3, 2, 5, 3, 3, 3},
                    new boolean[] {false, false, false, true, true, true});
            for (BreakDto b : r.breaks()) {
                breaks.add(new String[] {b.type(), b.asset(), b.reference(),
                    Amounts.format(b.expected(), b.asset()), Amounts.format(b.actual(), b.asset()),
                    Amounts.format(b.difference(), b.asset())}, "BnTableRowBad", null, null);
            }
            if (r.breaks().isEmpty()) {
                breaks.note(Lang.tr("Every control account agrees with its subledger."));
            }
            body.add(breaks.component());
            done(body);
        }, failed(body, () -> reconciliation(body))));
    }

    // ---------------------------------------------------------- period close

    private void periods(final Container body) {
        Api.books().periods(Net.to((List<PeriodDto> periods) -> {
            body.removeAll();
            body.add(Ui.muted(Lang.tr("Closing a period moves revenue and expense into "
                    + "retained earnings and locks every entry up to its date.")));
            Table table = new Table(new String[] {"Period", "Closed through", "Closed by",
                "Closed at", "Entries"}, new int[] {2, 4, 5, 4, 2},
                    new boolean[] {false, false, false, false, true});
            for (PeriodDto p : periods) {
                table.add(new String[] {"#" + p.seq(), when(p.closedThrough()), p.closedBy(),
                    when(p.closedAt()), String.valueOf(p.entries() == null ? 0
                            : p.entries().size())}, null);
            }
            if (periods.isEmpty()) {
                table.note(Lang.tr("No period has been closed yet."));
            }
            body.add(table.component());
            if (admin) {
                Button close = Ui.primary("Close the books through now");
                close.addActionListener(evt -> {
                    TextField password = password();
                    Command go = new Command(Lang.tr("Close period"));
                    if (Dialog.show(Lang.tr("Close period"), BoxLayout.encloseY(password), go,
                            new Command(Lang.tr("Cancel"))) != go) {
                        return;
                    }
                    Api.admin().closePeriod(new ClosePeriodDto(System.currentTimeMillis(),
                            password.getText()), Net.to((PeriodDto p) -> periods(body),
                            (s, m) -> Dialog.show(Lang.tr("Not done"), Lang.tr(m), Lang.tr("OK"),
                                    null)));
                });
                body.add(close);
            }
            done(body);
        }, failed(body, () -> periods(body))));
    }

    // ------------------------------------------------------------ approvals

    private void approvals(final Container body) {
        Api.admin().approvals(Net.to((List<ApprovalDto> list) -> {
            body.removeAll();
            body.add(Ui.muted(Lang.tr("Maker-checker: an adjustment or a reversal raised by one "
                    + "administrator is posted only when another approves it.")));
            Table table = new Table(new String[] {"Raised", "Type", "Maker", "Amount", "Memo",
                "Status"}, new int[] {3, 3, 5, 3, 5, 3},
                    new boolean[] {false, false, false, true, false, false});
            for (final ApprovalDto a : list) {
                String amount = a.amount() == 0 ? (a.entryId() == null || a.entryId().length() == 0
                        ? "" : shortId(a.entryId())) : Amounts.format(a.amount(), a.asset());
                boolean open = "PENDING".equals(a.status());
                table.add(new String[] {when(a.createdAt()), kindName(a.kind()), a.maker(), amount,
                    a.memo(), statusName(a.status())}, open ? "BnTableRowWarn" : "BnTableRow",
                        null, open ? () -> decide(body, a) : null);
            }
            if (list.isEmpty()) {
                table.note(Lang.tr("Nothing waiting."));
            }
            body.add(table.component());
            done(body);
        }, failed(body, () -> approvals(body))));
    }

    private void decide(final Container body, final ApprovalDto a) {
        TextField reason = Ui.field("Reason", TextField.ANY);
        TextField password = password();
        Command approve = new Command(Lang.tr("Approve"));
        Command reject = new Command(Lang.tr("Reject"));
        Command picked = Dialog.show(Lang.tr("Decide"), BoxLayout.encloseY(
                Ui.muted(Lang.tr("{0} raised by {1}", kindName(a.kind()), a.maker())),
                Ui.muted(a.memo() == null ? "" : a.memo()), reason, password),
                new Command[] {approve, reject, new Command(Lang.tr("Cancel"))});
        if (picked != approve && picked != reject) {
            return;
        }
        DecisionDto decision = new DecisionDto(password.getText(), reason.getText());
        Net.Ok<ApprovalDto> ok = done -> approvals(body);
        Net.Failed no = (s, m) -> Dialog.show(Lang.tr("Not done"), Lang.tr(m), Lang.tr("OK"), null);
        if (picked == approve) {
            Api.admin().approve(a.id(), decision, Net.to(ok, no));
        } else {
            Api.admin().reject(a.id(), decision, Net.to(ok, no));
        }
    }

    // ---------------------------------------------------------------- audit

    private void chain(final Container body) {
        Api.audit().chain(Net.to((final ChainReportDto c) -> Api.audit().events(null, "80",
                Net.to((List<AuditEventDto> events) -> {
                    body.removeAll();
                    body.add(FlowLayout.encloseIn(new Label(c.intact() ? Lang.tr("Intact")
                            : Lang.tr("Broken at #{0}", String.valueOf(c.firstBrokenSeq())),
                            c.intact() ? "BnBadgeGood" : "BnBadgeBad")));
                    Container facts = new Container(new GridLayout(wide ? 1 : 3, wide ? 3 : 1));
                    facts.add(fact("Links checked", String.valueOf(c.checked())));
                    facts.add(fact("Head", "#" + c.headSeq()));
                    facts.add(fact("Head hash", shortHash(c.headHash())));
                    body.add(facts);
                    for (String problem : c.problems()) {
                        body.add(Ui.row(Lang.tr("Problem"), problem, null, null, null));
                    }
                    body.add(Ui.muted(Lang.tr("Each event's hash covers its own fields and the "
                            + "hash before it, so changing any row breaks every link after it.")));
                    events(body, events, c.intact() ? -1 : c.firstBrokenSeq());
                    done(body);
                }, failed(body, () -> chain(body)))), failed(body, () -> chain(body))));
    }

    private void events(Container body, List<AuditEventDto> events, long broken) {
        Table table = new Table(new String[] {"#", "Time", "Actor", "Action", "Subject",
            "Previous hash", "Hash"}, new int[] {1, 3, 4, 4, 4, 3, 3},
                new boolean[] {true, false, false, false, false, false, false});
        for (AuditEventDto e : events) {
            table.add(new String[] {String.valueOf(e.seq()), when(e.at()), e.actor(), e.action(),
                e.subject(), shortHash(e.prevHash()), shortHash(e.hash())},
                    e.seq() == broken ? "BnTableRowBad" : "BnTableRow", null, null);
        }
        if (events.isEmpty()) {
            table.note(Lang.tr("No events."));
        }
        body.add(table.component());
    }

    private void trail(final Container body, final String subject) {
        body.removeAll();
        final TextField field = Ui.field("Entry id, account id or e-mail", TextField.ANY);
        field.setText(subject);
        Button find = Ui.primary("Show trail");
        Container search = new Container(new BorderLayout());
        search.add(BorderLayout.CENTER, field);
        search.add(BorderLayout.EAST, find);
        body.add(search);
        final Container results = new Container(BoxLayout.y());
        body.add(results);
        find.addActionListener(evt -> loadTrail(results, field.getText().trim()));
        if (subject.length() > 0) {
            loadTrail(results, subject);
        } else {
            results.add(Ui.muted(Lang.tr("Every event that names the subject, oldest first.")));
        }
        done(body);
    }

    private void loadTrail(final Container results, final String subject) {
        Ui.loading(results);
        Api.audit().trail(subject, Net.to((List<AuditEventDto> events) -> {
            results.removeAll();
            events(results, events, -1);
            done(results);
        }, failed(results, () -> loadTrail(results, subject))));
    }

    private void accessLog(final Container body) {
        Api.audit().accessLog("100", Net.to((List<AuditEventDto> events) -> {
            body.removeAll();
            body.add(Ui.muted(Lang.tr("Every read of the books and the audit log by staff is "
                    + "itself an audit event.")));
            Table table = new Table(new String[] {"Time", "Staff member", "Action", "Subject",
                "Address"}, new int[] {3, 5, 4, 5, 3},
                    new boolean[] {false, false, false, false, false});
            for (AuditEventDto e : events) {
                table.add(new String[] {when(e.at()), e.actor(), e.action(), e.subject(),
                    e.address()}, null);
            }
            if (events.isEmpty()) {
                table.note(Lang.tr("No events."));
            }
            body.add(table.component());
            done(body);
        }, failed(body, () -> accessLog(body))));
    }

    // ------------------------------------------------------------- evidence

    private void evidence(final Container body) {
        body.removeAll();
        body.add(Ui.muted(Lang.tr("A signed-off copy for an external auditor: the file, who "
                + "made it and when, and its SHA-256 so it can be checked later.")));
        final Container result = new Container(BoxLayout.y());
        String[] names = {"journal", "trial-balance", "audit-chain"};
        String[] labels = {"Journal", "Trial balance", "Audit chain"};
        for (int iter = 0; iter < names.length; iter++) {
            final String name = names[iter];
            Button csv = Ui.secondary("CSV");
            csv.addActionListener(evt -> export(result, name, "csv"));
            Button json = Ui.secondary("JSON");
            json.addActionListener(evt -> export(result, name, "json"));
            Container row = new Container(new BorderLayout(), "BnRow");
            row.add(BorderLayout.CENTER, new Label(Lang.tr(labels[iter]), "BnRowTitle"));
            row.add(BorderLayout.EAST, BoxLayout.encloseX(csv, json));
            body.add(row);
        }
        body.add(result);
        done(body);
    }

    private void export(final Container result, final String name, final String format) {
        Ui.loading(result);
        Api.audit().evidence(name, format, null, Net.to((EvidenceDto e) -> {
            result.removeAll();
            String file = e.name() + "." + e.format();
            result.add(Ui.row(file, Lang.tr("Made by {0}, {1}", e.generatedBy(),
                    when(e.generatedAt())), Lang.tr("Saved"), "BnBadgeGood", null));
            result.add(fact("SHA-256", e.sha256()));
            TextArea preview = new TextArea(head(e.content()));
            preview.setEditable(false);
            preview.setUIID("BnCode");
            preview.setRows(8);
            result.add(preview);
            save(file, e.content());
            done(result);
        }, failed(result, () -> export(result, name, format))));
    }

    /// Writes the file where the platform keeps the app's files and hands
    /// it to the platform: a browser downloads it, a phone offers to share
    /// or open it.
    private static void save(String file, String content) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String path = fs.getAppHomePath() + file;
        OutputStream out = null;
        try {
            out = fs.openOutputStream(path);
            out.write(content.getBytes("UTF-8"));
        } catch (Exception err) {
            Ui.toast("Could not save the file.");
            return;
        } finally {
            Util.cleanup(out);
        }
        CN.execute(path);
    }

    private static String head(String content) {
        int lines = 0;
        for (int iter = 0; iter < content.length(); iter++) {
            if (content.charAt(iter) == '\n' && ++lines == 12) {
                return content.substring(0, iter) + "\n...";
            }
        }
        return content;
    }

    // -------------------------------------------------------------- helpers

    private static Component fact(String label, String value) {
        Container box = new Container(BoxLayout.y(), "BnFact");
        box.add(new Label(Lang.tr(label), "BnFactLabel"));
        Label v = new Label(value == null || value.length() == 0 ? "-" : value, "BnFactValue");
        v.setEndsWith3Points(true);
        box.add(v);
        return box;
    }

    private static TextField password() {
        TextField field = new TextField("", Lang.tr("Your password"), 20, TextArea.PASSWORD);
        field.setUIID("BnField");
        return field;
    }

    private static String dr(long units, String asset) {
        return units == 0 ? "" : Amounts.format(units, asset);
    }

    private static String owner(String owner) {
        return owner == null || owner.length() == 0 || owner.startsWith("@") ? ""
                : " (" + owner + ")";
    }

    static String shortId(String id) {
        return id == null ? "" : id.length() <= 8 ? id : id.substring(0, 8);
    }

    private static String shortHash(String hash) {
        return hash == null ? "" : hash.length() <= 10 ? hash : hash.substring(0, 10);
    }

    static String when(long millis) {
        return millis <= 0 ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(millis));
    }

    private static String side(String normal) {
        return Lang.tr("DEBIT".equals(normal) ? "debit" : "credit");
    }

    static String className(String c) {
        if ("ASSET".equals(c) || "ASSETS".equals(c)) {
            return Lang.tr("Assets");
        }
        if ("LIABILITY".equals(c) || "LIABILITIES".equals(c)) {
            return Lang.tr("Liabilities");
        }
        if ("EQUITY".equals(c)) {
            return Lang.tr("Equity");
        }
        if ("REVENUE".equals(c)) {
            return Lang.tr("Revenue");
        }
        if ("EXPENSE".equals(c) || "EXPENSES".equals(c)) {
            return Lang.tr("Expenses");
        }
        return c;
    }

    static String statusName(String s) {
        if ("PENDING".equals(s)) {
            return Lang.tr("Pending");
        }
        if ("EXECUTED".equals(s)) {
            return Lang.tr("Approved");
        }
        if ("REJECTED".equals(s)) {
            return Lang.tr("Rejected");
        }
        return s;
    }

    static String kindName(String k) {
        if (k == null) {
            return "";
        }
        if ("TRANSFER".equals(k)) {
            return Lang.tr("Transfer");
        }
        if ("DEPOSIT".equals(k)) {
            return Lang.tr("Deposit");
        }
        if ("WITHDRAWAL".equals(k)) {
            return Lang.tr("Withdrawal");
        }
        if ("TRADE".equals(k)) {
            return Lang.tr("Trade");
        }
        if ("ADJUSTMENT".equals(k)) {
            return Lang.tr("Adjustment");
        }
        if ("REVERSAL".equals(k)) {
            return Lang.tr("Reversal");
        }
        if ("CLOSING".equals(k)) {
            return Lang.tr("Closing entry");
        }
        if ("CAPITAL".equals(k)) {
            return Lang.tr("Capital");
        }
        if ("ORDER_HOLD".equals(k)) {
            return Lang.tr("Order hold");
        }
        if ("ORDER_RELEASE".equals(k)) {
            return Lang.tr("Order release");
        }
        if ("CRYPTO_OUT".equals(k)) {
            return Lang.tr("Crypto withdrawal");
        }
        return k;
    }
}
