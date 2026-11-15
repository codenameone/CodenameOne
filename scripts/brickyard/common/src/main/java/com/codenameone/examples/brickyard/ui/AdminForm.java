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
package com.codenameone.examples.brickyard.ui;

import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.brickyard.api.AccountDto;
import com.codenameone.examples.brickyard.api.ActionDto;
import com.codenameone.examples.brickyard.api.AdjustDto;
import com.codenameone.examples.brickyard.api.AppealDto;
import com.codenameone.examples.brickyard.api.AuditDto;
import com.codenameone.examples.brickyard.api.FeaturedDto;
import com.codenameone.examples.brickyard.api.FraudFlagDto;
import com.codenameone.examples.brickyard.api.PayoutDto;
import com.codenameone.examples.brickyard.api.ReportDto;
import com.codenameone.examples.brickyard.api.SanctionDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.net.Session;
import java.util.List;

/// The staff console: the report queue, sanctions and appeals, the audit log,
/// KPIs, fraud flags, payout decisions, home rows, Bricks adjustments and
/// the scheduled jobs. The server checks every call against the caller's
/// role; this screen only decides whether to offer itself.
public final class AdminForm {
    static final String[] STAFF = {"ADMIN", "MODERATOR", "SUPPORT"};
    static final String[] REPORT_ACTIONS = {"DISMISS", "WARN", "TEMP_BAN", "PERM_BAN", "MUTE", "UNPUBLISH"};
    static final long TEMP_BAN_MINUTES = 24 * 60;

    private AdminForm() {
    }

    /// True when the signed-in account holds a staff role.
    public static boolean allowed() {
        AccountDto me = Session.user();
        if (me == null || me.roles() == null) {
            return false;
        }
        for (String r : STAFF) {
            if (me.roles().contains(r)) {
                return true;
            }
        }
        return false;
    }

    public static void show() {
        final Form previous = com.codename1.ui.CN.getCurrentForm();
        final Form f = Ui.form("Admin", "admin");
        Ui.back(f, () -> {
            if (previous != null) {
                previous.showBack();
            } else {
                Nav.home();
            }
        });
        f.add(Ui.label("KPIs", "RowTitle"));
        final Container kpis = box("kpis", f);
        f.add(Ui.label("Report queue", "RowTitle"));
        final Container reports = box("reportQueue", f);
        f.add(Ui.label("Sanctions", "RowTitle"));
        final TextField who = Ui.field("Username", TextField.ANY, "sanctionUser");
        f.add(who);
        final Container sanctions = box("sanctions", f);
        f.add(Ui.secondary("Look up sanctions", "sanctionsButton", e -> loadSanctions(who.getText().trim(),
                sanctions, f)));
        f.add(Ui.label("Appeals", "RowTitle"));
        final Container appeals = box("appeals", f);
        f.add(Ui.label("Payouts", "RowTitle"));
        final Container payouts = box("payouts", f);
        f.add(Ui.label("Fraud flags", "RowTitle"));
        final Container fraud = box("fraudFlags", f);
        f.add(Ui.label("Home rows", "RowTitle"));
        final Container featured = box("featured", f);
        f.add(Ui.secondary("Feature an experience", "featureButton", e -> feature(featured, f)));
        f.add(Ui.label("Bricks", "RowTitle"));
        f.add(Ui.secondary("Grant or revoke Bricks", "adjustButton", e -> adjust()));
        f.add(Ui.label("Jobs", "RowTitle"));
        f.add(Ui.secondary("Run engagement payouts", "jobEngagementButton", e -> job("engagement-payouts")));
        f.add(Ui.secondary("Run Plus stipends", "jobStipendsButton", e -> job("plus-stipends")));
        f.add(Ui.label("Audit log", "RowTitle"));
        final Container audit = box("auditLog", f);
        f.show();

        Api.moderation().kpis(Net.to(k -> {
            kpis.add(Ui.text("DAU " + k.dau() + ", CCU " + k.ccu() + ", sessions " + k.sessions()
                    + ", purchases " + k.purchases() + ", revenue " + k.revenueBricks() + " Bricks, script errors "
                    + k.scriptErrors() + " (" + k.errorsPerThousandSessions() + " per 1,000 sessions)", "Body"));
            f.revalidate();
        }, Ui::failed));
        loadReports(reports, f);
        loadAppeals(appeals, f);
        loadPayouts(payouts, f);
        Api.moderation().fraudFlags(Net.to(rows -> {
            empty(fraud, rows);
            if (rows != null) {
                for (FraudFlagDto x : rows) {
                    fraud.add(Ui.label(x.username() + ": " + x.kind() + " - " + x.detail(), "Body"));
                }
            }
            f.revalidate();
        }, Ui::failed));
        loadFeatured(featured, f);
        Api.moderation().audit(Net.to(rows -> {
            empty(audit, rows);
            if (rows != null) {
                for (AuditDto a : rows) {
                    audit.add(Ui.label(a.actor() + " " + a.action() + " " + a.target()
                            + (a.detail() == null ? "" : " - " + a.detail()), "CardMeta"));
                }
            }
            f.revalidate();
        }, Ui::failed));
    }

    static Container box(String name, Form f) {
        Container c = new Container(BoxLayout.y());
        c.setName(name);
        f.add(c);
        return c;
    }

    static void empty(Container c, List<?> rows) {
        if (rows == null || rows.isEmpty()) {
            c.add(Ui.label("Nothing here.", "Muted"));
        }
    }

    static void loadReports(final Container reports, final Form f) {
        Api.moderation().queue("OPEN", Net.to(rows -> {
            reports.removeAll();
            empty(reports, rows);
            if (rows != null) {
                for (final ReportDto r : rows) {
                    reports.add(Ui.panel(Ui.label(r.targetType() + " " + r.targetId(), "CardTitle"),
                            Ui.text(r.reason() + " (from " + r.reporter() + ")", "Body"),
                            Ui.secondary("Resolve", "resolve" + r.id(), e -> resolve(r, reports, f))));
                }
            }
            f.revalidate();
        }, Ui::failed));
    }

    static void resolve(final ReportDto r, final Container reports, final Form f) {
        String kind = choose("Resolve report", REPORT_ACTIONS);
        if (kind == null) {
            return;
        }
        long minutes = "TEMP_BAN".equals(kind) ? TEMP_BAN_MINUTES : 0;
        Api.moderation().resolveReport(r.id(), new ActionDto(kind, null, r.targetId(), minutes),
                Net.to(done -> loadReports(reports, f), Ui::failed));
    }

    /// One choice of `options`, or null when cancelled.
    static String choose(String title, String[] options) {
        com.codename1.ui.Command[] cmds = new com.codename1.ui.Command[options.length + 1];
        for (int i = 0; i < options.length; i++) {
            cmds[i] = new com.codename1.ui.Command(options[i]);
        }
        cmds[options.length] = new com.codename1.ui.Command("Cancel");
        com.codename1.ui.Command c = Dialog.show(title, Ui.label("Choose an action.", "Body"), cmds);
        if (c == null || c == cmds[options.length]) {
            return null;
        }
        return c.getCommandName();
    }

    static void loadSanctions(String username, final Container sanctions, final Form f) {
        if (username.length() == 0) {
            return;
        }
        Api.moderation().sanctionsOf(username, Net.to(rows -> {
            sanctions.removeAll();
            empty(sanctions, rows);
            if (rows != null) {
                for (SanctionDto s : rows) {
                    sanctions.add(Ui.label(s.kind() + ": " + s.reason() + (s.lifted() != 0 ? " (lifted)" : ""),
                            "Body"));
                }
            }
            f.revalidate();
        }, Ui::failed));
    }

    static void loadAppeals(final Container appeals, final Form f) {
        Api.moderation().appeals(Net.to(rows -> {
            appeals.removeAll();
            empty(appeals, rows);
            if (rows != null) {
                for (final AppealDto a : rows) {
                    appeals.add(Ui.panel(Ui.text(a.body() + " (" + a.status() + ")", "Body"),
                            Ui.secondary("Grant", "grantAppeal" + a.id(), e -> decideAppeal(a, "GRANT", appeals, f)),
                            Ui.link("Deny", "denyAppeal" + a.id(), e -> decideAppeal(a, "DENY", appeals, f))));
                }
            }
            f.revalidate();
        }, Ui::failed));
    }

    static void decideAppeal(AppealDto a, String kind, final Container appeals, final Form f) {
        Api.moderation().decideAppeal(a.id(), new ActionDto(kind, null, null, 0),
                Net.to(done -> loadAppeals(appeals, f), Ui::failed));
    }

    static void loadPayouts(final Container payouts, final Form f) {
        Api.moderation().payouts(Net.to(rows -> {
            payouts.removeAll();
            empty(payouts, rows);
            if (rows != null) {
                for (final PayoutDto p : rows) {
                    payouts.add(Ui.panel(Ui.label(p.username() + ": " + p.bricks() + " Bricks", "Body"),
                            Ui.secondary("Approve", "approvePayout" + p.id(),
                                    e -> decidePayout(p, "APPROVE", payouts, f)),
                            Ui.link("Reject", "rejectPayout" + p.id(), e -> decidePayout(p, "REJECT", payouts, f))));
                }
            }
            f.revalidate();
        }, Ui::failed));
    }

    static void decidePayout(PayoutDto p, String kind, final Container payouts, final Form f) {
        Api.moderation().decidePayout(p.id(), new ActionDto(kind, "", null, 0),
                Net.to(done -> loadPayouts(payouts, f), Ui::failed));
    }

    static void loadFeatured(final Container featured, final Form f) {
        Api.moderation().featured(Net.to(rows -> {
            featured.removeAll();
            empty(featured, rows);
            if (rows != null) {
                for (final FeaturedDto x : rows) {
                    featured.add(Ui.panel(Ui.label(x.rowName() + " #" + x.ordinal() + ": " + x.experienceId(), "Body"),
                            Ui.link("Remove", "unfeature" + x.id(), e -> Api.moderation().unfeature(x.id(),
                                    Net.to(done -> loadFeatured(featured, f), Ui::failed)))));
                }
            }
            f.revalidate();
        }, Ui::failed));
    }

    static void feature(final Container featured, final Form f) {
        TextField row = Ui.field("Row name", TextField.ANY, "featureRow");
        TextField experience = Ui.field("Experience id", TextField.ANY, "featureExperience");
        TextField ordinal = Ui.field("Position", TextField.NUMERIC, "featureOrdinal");
        if (!confirm("Feature an experience", BoxLayout.encloseY(row, experience, ordinal), "Feature")) {
            return;
        }
        Api.moderation().feature(new FeaturedDto(null, row.getText().trim(), experience.getText().trim(),
                (int) DashboardForm.parse(ordinal.getText())), Net.to(done -> loadFeatured(featured, f), Ui::failed));
    }

    static void adjust() {
        TextField user = Ui.field("Username", TextField.ANY, "adjustUser");
        TextField amount = Ui.field("Amount (negative to revoke)", TextField.ANY, "adjustAmount");
        TextField reason = Ui.field("Reason", TextField.ANY, "adjustReason");
        if (!confirm("Grant or revoke Bricks", BoxLayout.encloseY(user, amount, reason), "Apply")) {
            return;
        }
        Api.moderation().adjust(new AdjustDto(user.getText().trim(), DashboardForm.parse(amount.getText()),
                reason.getText().trim()), Net.to(w -> Ui.toast("Balance now " + w.balance()), Ui::failed));
    }

    static void job(String name) {
        Api.moderation().runJob(name, Net.to(j -> Ui.toast(j.job() + ": " + j.processed() + " processed, "
                + j.bricks() + " Bricks"), Ui::failed));
    }

    static boolean confirm(String title, com.codename1.ui.Component body, String ok) {
        com.codename1.ui.Command yes = new com.codename1.ui.Command(ok);
        return Dialog.show(title, body, new com.codename1.ui.Command("Cancel"), yes) == yes;
    }
}
