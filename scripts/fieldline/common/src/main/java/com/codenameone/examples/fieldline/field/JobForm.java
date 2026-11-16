package com.codenameone.examples.fieldline.field;

import com.codename1.appkit.Net;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.util.Base64;
import com.codenameone.examples.fieldline.Prefs;
import com.codenameone.examples.fieldline.Telemetry;
import com.codenameone.examples.fieldline.api.CheckDto;
import com.codenameone.examples.fieldline.api.ChecklistDto;
import com.codenameone.examples.fieldline.api.DiagnosisDto;
import com.codenameone.examples.fieldline.api.JobDto;
import com.codenameone.examples.fieldline.api.PhotoDto;
import com.codenameone.examples.fieldline.api.PhotoUploadDto;
import com.codenameone.examples.fieldline.api.ReportDto;
import com.codenameone.examples.fieldline.api.SignatureDto;
import com.codenameone.examples.fieldline.live.LiveChannel;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Photos;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// One job, from the drive there to the customer's signature.
///
/// Top to bottom it is the order the work is done in: where and who, what was
/// reported, the checklist, photos -- each looked at by the assistant, which
/// answers with what it sees and what to do -- a drafted report, and the
/// sign-off. The bar at the bottom always holds the one next step, and says
/// what is still missing when that step is finishing the job.
public final class JobForm {
    private static final String[] RESULTS = {"PASS", "FAIL", "NA"};
    private static final String[] RESULT_NAMES = {"Pass", "Fail", "N/A"};

    private final Form previous;
    private final Form form;
    private final Container page = Ui.page();
    private final Container bar = new Container(new BorderLayout());
    private JobDto job;
    private List<CheckDto> checks = new ArrayList<CheckDto>();
    private final Container photoGrid = new Container(new GridLayout(1, 3));
    private final Container findings = new Container(BoxLayout.y());
    private final Container report = new Container(BoxLayout.y());
    private final Container signature = new Container(BoxLayout.y());
    private TextArea notes;
    /// What a pane tells the screen it is on when the job changes; null on a
    /// screen of its own, which goes back to `previous` instead.
    private Runnable changed;
    /// Whether the cards go in two columns, where a pane is wide enough.
    private boolean twoColumns;
    private final Container root = new Container(new BorderLayout());

    /// The job as a pane of a screen that is already up: the tablet's day,
    /// beside the route.
    ///
    /// @param on the screen it is on, which the signature and the assistant
    ///     come back to
    /// @param changed run after the job's status changes, for the route
    /// @param twoColumns whether there is room for the cards side by side
    public static Container pane(Form on, String jobId, Runnable changed, boolean twoColumns) {
        final JobForm pane = new JobForm(on, changed, twoColumns);
        Api.field().job(jobId, Net.to(pane::fill, Ui::fail));
        return pane.root;
    }

    private JobForm(Form on, Runnable changed, boolean twoColumns) {
        previous = null;
        form = on;
        this.changed = changed;
        this.twoColumns = twoColumns;
        bar.setUIID("FlActionBar");
        root.setName("jobPane");
        root.add(BorderLayout.CENTER, page);
        root.add(BorderLayout.SOUTH, bar);
        page.add(Ui.label("Loading...", "WlMuted"));
        live(root);
    }

    private JobForm(Form previous) {
        this.previous = previous;
        form = Ui.form("Job", "Job");
        Ui.back(form, () -> {
            saveChecklist(null);
            close();
        });
        form.addShowListener(e -> {
            // Listening again is harmless when it never stopped: a second
            // listen for the same owner replaces the first.
            live(form);
            if (stale && job != null) {
                stale = false;
                check(form);
            }
        });
        bar.setUIID("FlActionBar");
        bar.setSafeArea(true);
        form.add(BorderLayout.CENTER, page);
        form.add(BorderLayout.SOUTH, bar);
        page.add(Ui.label("Loading...", "WlMuted"));
        live(form);
    }

    /// Keeps the open job current: a dispatcher moving it, the planner
    /// re-timing it or a colleague closing it is pushed over the live
    /// channel, and the job is fetched again. It is redrawn only when
    /// something the dispatcher controls changed, so a technician halfway
    /// through the checklist does not lose what they typed. Stops listening
    /// once the job is no longer on screen.
    private void live(final Component owner) {
        LiveChannel.listen(owner, new LiveChannel.Adapter() {
            @Override
            public void jobChanged(String id, String status) {
                if (job != null && job.id().equals(id)) {
                    check(owner);
                }
            }

            @Override
            public void scheduleChanged(String runId, String summary) {
                if (job != null) {
                    check(owner);
                }
            }
        });
    }

    /// When this device last changed a job itself. The server pushes that
    /// change back like any other, often before the request's own response
    /// arrives, and the technician should not be told about their own tap.
    private static long localChangeAt;

    /// A live frame arrived while another form covered this one.
    private boolean stale;

    /// Leaves the standalone job form for good.
    private void close() {
        LiveChannel.unlisten(form);
        previous.showBack();
    }

    /// Records that this device is about to change a job.
    static void touched() {
        localChangeAt = System.currentTimeMillis();
    }

    private void check(final Component owner) {
        if (owner == form) {
            // Not current: covered by sign-off, the assistant or the camera,
            // or left for good (sign-out, a notification). Which one cannot
            // be told here, so stop listening either way and remember the
            // frame: the show listener listens again and re-reads the job if
            // the form comes back, and nothing is left registered if not.
            if (CN.getCurrentForm() != form) {
                LiveChannel.unlisten(owner);
                stale = true;
                return;
            }
        } else if (owner.getComponentForm() == null) {
            LiveChannel.unlisten(owner);
            return;
        }
        final JobDto before = job;
        Api.field().job(before.id(), Net.to(now -> {
            if (moved(before, now)) {
                fill(now);
                if (System.currentTimeMillis() - localChangeAt > 8000L) {
                    Ui.say(Lang.tr("This job was updated"));
                }
            }
        }, (status, message) -> { }));
    }

    /// Whether the job changed in a way the screen shows: who, when, status.
    static boolean moved(JobDto a, JobDto b) {
        return !eq(a.status(), b.status()) || !eq(a.techId(), b.techId())
                || a.plannedStart() != b.plannedStart() || a.plannedEnd() != b.plannedEnd()
                || a.signed() != b.signed() || a.photos() != b.photos();
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    public static void show(Form previous, String jobId) {
        final JobForm screen = new JobForm(previous);
        screen.form.show();
        Api.field().job(jobId, Net.to(screen::fill, Ui::fail));
    }

    private void fill(JobDto loaded) {
        job = loaded;
        checks = new ArrayList<CheckDto>(loaded.checklist());
        if (changed == null) {
            form.setTitle(Lang.tr(TodayForm.kind(loaded.kind())));
        }
        page.removeAll();
        // The photo grid, findings, report and signature outlive a refill (a
        // status change fills the page again) and are still inside the cards
        // of the last one: they are taken out before they go into the new.
        photoGrid.remove();
        findings.remove();
        report.remove();
        signature.remove();
        page.add(hero());
        page.add(stepper(loaded.status()));
        if (loaded.description() != null && loaded.description().length() > 0) {
            Container reported = Ui.card();
            reported.add(Ui.label("Reported problem", "FlCardCaption"));
            reported.add(Ui.text(loaded.description(), "FlCardText"));
            reported.add(Ui.label(loaded.assetType() + (loaded.assetModel().length() == 0 ? ""
                    : Ui.DOT + loaded.assetModel()), "FlCardDetail"));
            page.add(reported);
        }

        // Where the cards go: down the page, or the checklist and the notes on
        // the left and the photos, the assistant and the sign-off on the right.
        Container left = page;
        Container right = page;
        if (twoColumns) {
            Container cols = new Container(new com.codename1.ui.layouts.GridLayout(1, 2));
            cols.setUIID("FlJobColumns");
            left = new Container(BoxLayout.y());
            left.setUIID("FlJobColumn");
            right = new Container(BoxLayout.y());
            right.setUIID("FlJobColumn");
            cols.add(left).add(right);
            page.add(cols);
        }
        Ui.section(left, "Checklist");
        left.add(checklist());

        Ui.section(right, "Photos");
        Container photos = Ui.card();
        photoGrid.removeAll();
        photos.add(photoGrid);
        findings.removeAll();
        photos.add(findings);
        Container add = new Container(new GridLayout(1, 2));
        add.add(Ui.secondary("Take photo", "takePhoto", e -> Photos.camera(this::upload)));
        add.add(Ui.secondary("From gallery", "pickPhoto", e -> Photos.gallery(this::upload)));
        photos.add(add);
        if (!Prefs.aiAssist()) {
            photos.add(Ui.text("AI diagnosis is off in Settings: photos are kept with the "
                    + "job but not looked at.", "WlMuted"));
        }
        right.add(photos);
        loadPhotos();

        Ui.section(left, "Notes");
        notes = new TextArea(loaded.notes() == null ? "" : loaded.notes(), 3, 30);
        notes.setUIID("WlField");
        notes.setHint(Lang.tr("What you found and what you did"));
        notes.setName("notes");
        notes.setGrowByContent(true);
        notes.addActionListener(e -> saveChecklist(null));
        left.add(notes);

        Ui.section(right, "Assistant");
        Container ai = Ui.card();
        ai.add(Ui.row(FontImage.MATERIAL_AUTO_AWESOME, "Ask about this job",
                "The assistant knows the equipment, the checklist and your photos", null));
        Container aiButtons = new Container(new GridLayout(1, 2));
        aiButtons.add(Ui.secondary("Ask", "askJob", e -> AssistantForm.show(form, job.id())));
        aiButtons.add(Ui.secondary("Draft report", "draftReport", e -> draftReport()));
        ai.add(aiButtons);
        report.removeAll();
        ai.add(report);
        right.add(ai);

        Ui.section(right, "Sign-off");
        signature.removeAll();
        right.add(signature);
        showSignature();
        page.add(Ui.label(" ", "FlSpacer"));
        updateBar();
        Ui.relayout(form, null);
    }

    private Container hero() {
        Container hero = new Container(BoxLayout.y());
        hero.setUIID("FlHero");
        Container chips = new Container(new FlowLayout());
        chips.add(Ui.badge(Ui.status(job.status()), Ui.statusBadge(job.status())));
        if (job.priority() >= 4) {
            chips.add(Ui.badge("Urgent", "FlBadgeUrgent"));
        }
        chips.add(Ui.badge(job.id(), "FlBadgeKind"));
        hero.add(chips);
        hero.add(Ui.text(job.title(), "FlHeroTitle"));
        hero.add(Ui.label(job.siteName(), "FlHeroSite"));
        hero.add(Ui.label(job.address(), "FlHeroDetail"));
        if (job.plannedStart() > 0) {
            hero.add(Ui.label(Ui.clock(job.plannedStart()) + " - " + Ui.clock(job.plannedEnd())
                    + Ui.DOT + Ui.duration(job.durationMinutes()), "FlHeroDetail"));
        }
        Container actions = new Container(new GridLayout(1, 3));
        actions.setUIID("FlHeroActions");
        actions.add(Ui.action(FontImage.MATERIAL_CALL, "Call", "callCustomer", e -> {
            if (job.contactPhone() != null && job.contactPhone().length() > 0) {
                CN.dial(job.contactPhone());
            }
        }));
        actions.add(Ui.action(FontImage.MATERIAL_DIRECTIONS, "Directions", "directions",
                e -> CN.execute("https://www.google.com/maps/dir/?api=1&destination="
                        + job.lat() + "," + job.lng())));
        actions.add(Ui.action(FontImage.MATERIAL_AUTO_AWESOME, "Assistant", "heroAssistant",
                e -> AssistantForm.show(form, job.id())));
        hero.add(actions);
        Container who = Ui.row(FontImage.MATERIAL_PERSON, job.customer(),
                job.contactPhone(), null);
        hero.add(who);
        return hero;
    }

    /// Scheduled, en route, on site, done: where the job is, as four dots on a
    /// line, the ones passed filled in.
    private static Container stepper(String status) {
        String[] steps = {"SCHEDULED", "EN_ROUTE", "ON_SITE", "COMPLETED"};
        int at = 0;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i].equals(status)) {
                at = i;
            }
        }
        Container row = new Container(new GridLayout(1, steps.length));
        row.setUIID("FlStepper");
        row.setName("stepper");
        for (int i = 0; i < steps.length; i++) {
            Container step = new Container(BoxLayout.y());
            Label dot = new Label("", i < at ? "FlStepDone" : i == at ? "FlStepNow" : "FlStep");
            Ui.icon(dot, i < at ? FontImage.MATERIAL_CHECK_CIRCLE
                    : i == at ? FontImage.MATERIAL_RADIO_BUTTON_CHECKED
                            : FontImage.MATERIAL_RADIO_BUTTON_UNCHECKED, 3.6f);
            step.add(FlowLayout.encloseCenter(dot));
            step.add(FlowLayout.encloseCenter(Ui.label(Ui.status(steps[i]),
                    i <= at ? "FlStepLabelNow" : "FlStepLabel")));
            row.add(step);
        }
        return row;
    }

    private Label progress;

    private Container checklist() {
        Container list = Ui.card();
        list.setName("checklist");
        boolean locked = job.signed() || "COMPLETED".equals(job.status());
        progress = Ui.label(answeredText(), "FlCardCaption");
        list.add(progress);
        for (int i = 0; i < checks.size(); i++) {
            final int index = i;
            CheckDto c = checks.get(i);
            Container line = new Container(BoxLayout.y());
            line.setUIID("FlCheck");
            line.add(Ui.text(c.label(), "FlCheckLabel"));
            String current = c.result() == null ? "" : c.result();
            if (locked) {
                line.add(FlowLayout.encloseIn(Ui.badge(current.length() == 0 ? "-"
                        : name(current), badge(current))));
            } else {
                line.add(Ui.choice("check-" + i, RESULT_NAMES, RESULTS, current,
                        value -> answer(index, value)));
            }
            if ("FAIL".equals(current) || c.note() != null && c.note().length() > 0) {
                final TextArea note = new TextArea(c.note() == null ? "" : c.note(), 1, 30);
                note.setUIID("WlField");
                note.setHint(Lang.tr("What failed?"));
                note.setName("checkNote-" + i);
                note.setEditable(!locked);
                note.addActionListener(e -> {
                    CheckDto was = checks.get(index);
                    checks.set(index, new CheckDto(was.index(), was.label(), was.result(),
                            note.getText()));
                    saveChecklist(null);
                });
                line.add(note);
            }
            list.add(line);
        }
        return list;
    }

    private String answeredText() {
        int answered = 0;
        for (CheckDto c : checks) {
            if (c.result() != null && c.result().length() > 0) {
                answered++;
            }
        }
        return Lang.tr("{0} of {1} answered", String.valueOf(answered),
                String.valueOf(checks.size()));
    }

    private void answer(int index, String value) {
        CheckDto was = checks.get(index);
        checks.set(index, new CheckDto(was.index(), was.label(), value, was.note()));
        // The count above the lines is read when the card is built; an answer
        // that does not rebuild it (anything but a change to or from Fail)
        // updates it here, or it keeps saying "0 of 5" after every tap.
        if (progress != null) {
            progress.setText(answeredText());
        }
        saveChecklist(() -> {
            // A failed line opens a note under it, so the screen is redrawn.
            if ("FAIL".equals(value) != "FAIL".equals(was.result())) {
                refill();
            } else {
                updateBar();
            }
        });
    }

    private void refill() {
        stale = false;
        Api.field().job(job.id(), Net.to(this::fill, Ui::fail));
    }

    private void saveChecklist(final Runnable done) {
        if (job == null || job.signed() || "COMPLETED".equals(job.status())) {
            return;
        }
        String text = notes == null ? job.notes() : notes.getText();
        touched();
        Api.field().saveChecklist(job.id(), new ChecklistDto(checks, text), Net.to(saved -> {
            job = saved;
            if (done != null) {
                done.run();
            }
        }, (status, message) -> {
            if (status != 409) {
                Ui.fail(status, message);
            }
        }));
    }

    private void loadPhotos() {
        Api.field().photos(job.id(), Net.to(list -> {
            photoGrid.removeAll();
            findings.removeAll();
            int shown = 0;
            for (PhotoDto p : list) {
                if (shown < 6) {
                    photoGrid.add(thumb(p));
                    shown++;
                }
                if (p.diagnosis() != null) {
                    findings.add(diagnosis(p.diagnosis()));
                }
            }
            photoGrid.setLayout(new GridLayout(Math.max(1, (shown + 2) / 3), 3));
            form.revalidate();
        }, Ui::fail));
    }

    private Component thumb(PhotoDto p) {
        Label image = new Label("", "FlThumb");
        if (p.data() != null && p.data().length() > 0) {
            try {
                byte[] bytes = Base64.decode(p.data().getBytes());
                Image img = Image.createImage(bytes, 0, bytes.length);
                int side = CN.convertToPixels(24f);
                image.setIcon(img.fill(side, side));
            } catch (RuntimeException bad) {
                Ui.icon(image, FontImage.MATERIAL_BROKEN_IMAGE, 6f);
            }
        } else {
            Ui.icon(image, FontImage.MATERIAL_IMAGE, 6f);
        }
        return image;
    }

    private void upload(byte[] jpeg, Image picture) {
        final Label pending = new Label("", "FlThumb");
        int side = CN.convertToPixels(24f);
        pending.setIcon(picture.fill(side, side));
        photoGrid.add(pending);
        final Container thinking = Ui.card();
        thinking.setUIID("FlDiagnosisPending");
        thinking.add(Ui.row(FontImage.MATERIAL_AUTO_AWESOME, "Looking at your photo...",
                "The assistant checks it against the equipment and the checklist", null));
        if (Prefs.aiAssist()) {
            findings.addComponent(0, thinking);
        }
        form.revalidate();
        String data = Base64.encodeNoNewline(jpeg);
        touched();
        Api.field().addPhoto(job.id(), new PhotoUploadDto(data, "image/jpeg", ""),
                Net.to(photo -> {
                    thinking.remove();
                    Telemetry.photoTaken(photo.diagnosis() != null);
                    if (photo.diagnosis() != null) {
                        Component card = diagnosis(photo.diagnosis());
                        findings.addComponent(0, card);
                    }
                    form.revalidate();
                    updateBar();
                }, (status, message) -> {
                    thinking.remove();
                    pending.remove();
                    form.revalidate();
                    Ui.fail(status, message);
                }));
    }

    /// What the assistant saw in a photo: the issue in large type, a severity
    /// band down the side, then cause, action and parts, and a safety note in
    /// its own colour when there is one.
    static Component diagnosis(DiagnosisDto d) {
        String severity = d.severity() == null ? "LOW" : d.severity();
        Container card = new Container(new BorderLayout());
        card.setUIID("FlDiagnosis");
        card.setName("diagnosis");
        Label band = new Label("", "FlSeverity" + severityStyle(severity));
        card.add(BorderLayout.WEST, band);
        Container body = new Container(BoxLayout.y());
        Container head = new Container(new BorderLayout());
        head.add(BorderLayout.CENTER, Ui.text(d.issue(), "FlDiagnosisIssue"));
        head.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(
                Ui.badge(severityName(severity), "FlBadge" + severityStyle(severity))));
        body.add(head);
        if (d.likelyCause() != null && d.likelyCause().length() > 0) {
            body.add(Ui.label("Likely cause", "FlCardCaption"));
            body.add(Ui.text(d.likelyCause(), "FlCardText"));
        }
        if (d.recommendedAction() != null && d.recommendedAction().length() > 0) {
            body.add(Ui.label("Recommended", "FlCardCaption"));
            body.add(Ui.text(d.recommendedAction(), "FlCardText"));
        }
        if (d.parts() != null && !d.parts().isEmpty()) {
            Container parts = new Container(new FlowLayout());
            for (String part : d.parts()) {
                parts.add(Ui.chip(part, FontImage.MATERIAL_BUILD));
            }
            body.add(parts);
        }
        if (d.safetyNote() != null && d.safetyNote().length() > 0) {
            Container safety = new Container(new BorderLayout());
            safety.setUIID("FlSafety");
            Label icon = new Label("", "FlSafetyIcon");
            Ui.icon(icon, FontImage.MATERIAL_WARNING, 3.4f);
            safety.add(BorderLayout.WEST, icon);
            safety.add(BorderLayout.CENTER, Ui.text(d.safetyNote(), "FlSafetyText"));
            body.add(safety);
        }
        body.add(confidence(d.confidence(), d.source()));
        card.add(BorderLayout.CENTER, body);
        return card;
    }

    private static Component confidence(final int pct, String source) {
        Container row = new Container(new BorderLayout());
        row.setUIID("FlConfidenceRow");
        Component meter = new Component() {
            @Override
            public void paint(com.codename1.ui.Graphics g) {
                int h = Math.max(4, getHeight() / 2);
                int y = getY() + (getHeight() - h) / 2;
                g.setAntiAliased(true);
                g.setColor(getStyle().getBgColor());
                g.fillRoundRect(getX(), y, getWidth(), h, h, h);
                g.setColor(getStyle().getFgColor());
                g.fillRoundRect(getX(), y, Math.max(h, getWidth() * pct / 100), h, h, h);
            }

            @Override
            protected com.codename1.ui.geom.Dimension calcPreferredSize() {
                return new com.codename1.ui.geom.Dimension(CN.convertToPixels(20f),
                        CN.convertToPixels(2.4f));
            }
        };
        meter.setUIID("FlConfidence");
        row.add(BorderLayout.CENTER, meter);
        String who = "demo".equals(source) ? Lang.tr("demo") : Lang.tr("AI");
        row.add(BorderLayout.EAST, Ui.label(Lang.tr("{0}% sure", String.valueOf(pct)) + Ui.DOT
                + who, "FlCardDetail"));
        return row;
    }

    private static String severityStyle(String severity) {
        if ("CRITICAL".equals(severity) || "HIGH".equals(severity)) {
            return "High";
        }
        return "MEDIUM".equals(severity) ? "Medium" : "Low";
    }

    private static String severityName(String severity) {
        if ("CRITICAL".equals(severity)) {
            return "Critical";
        }
        if ("HIGH".equals(severity)) {
            return "High";
        }
        return "MEDIUM".equals(severity) ? "Medium" : "Low";
    }

    private void draftReport() {
        saveChecklist(null);
        report.removeAll();
        report.add(Ui.label("Drafting...", "WlMuted"));
        form.revalidate();
        Api.field().draftReport(job.id(), Net.to(this::showReport, Ui::fail));
    }

    private void showReport(ReportDto r) {
        report.removeAll();
        Container card = new Container(BoxLayout.y());
        card.setUIID("FlReport");
        card.setName("report");
        card.add(Ui.label("Draft report", "FlCardCaption"));
        card.add(Ui.text(r.summary(), "FlCardText"));
        if (r.findings() != null && !r.findings().isEmpty()) {
            card.add(Ui.label("Findings", "FlCardCaption"));
            for (String f : r.findings()) {
                card.add(Ui.text("\u2022 " + f, "FlCardText"));
            }
        }
        if (r.followUps() != null && !r.followUps().isEmpty()) {
            card.add(Ui.label("Follow-up", "FlCardCaption"));
            for (String f : r.followUps()) {
                card.add(Ui.text("\u2022 " + f, "FlCardText"));
            }
        }
        card.add(Ui.link("Use as my notes", "useReport", e -> {
            StringBuilder text = new StringBuilder(r.summary());
            if (r.findings() != null) {
                for (String f : r.findings()) {
                    text.append("\n- ").append(f);
                }
            }
            notes.setText(text.toString());
            saveChecklist(null);
            Ui.say("Added to your notes");
        }));
        report.add(card);
        Ui.relayout(form, null);
    }

    private void showSignature() {
        if (job.signed()) {
            Api.field().signature(job.id(), Net.to(this::signed, Ui::fail));
            return;
        }
        Container card = Ui.card();
        boolean onSite = "ON_SITE".equals(job.status());
        card.add(Ui.row(FontImage.MATERIAL_DRAW, "Customer signature",
                onSite ? "The customer signs on your screen; the record is sealed with a hash"
                        : "Available once you have checked in on site", null));
        Button sign = Ui.primary("Get signature", "getSignature",
                e -> SignOffForm.show(form, job, this::refill));
        sign.setEnabled(onSite);
        card.add(sign);
        signature.add(card);
    }

    private void signed(SignatureDto s) {
        signature.removeAll();
        Container card = new Container(BoxLayout.y());
        card.setUIID("FlSigned");
        card.setName("signed");
        Container head = new Container(new BorderLayout());
        head.add(BorderLayout.CENTER, Ui.label(Lang.tr("Signed by {0}", s.signerName()),
                "FlSignedTitle"));
        head.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(s.verified()
                ? Ui.badge("Verified", "FlBadgeDone") : Ui.badge("Altered", "FlBadgeHigh")));
        card.add(head);
        if (s.png() != null && s.png().length() > 0) {
            try {
                byte[] bytes = Base64.decode(s.png().getBytes());
                Label ink = new Label(Image.createImage(bytes, 0, bytes.length), "FlInk");
                card.add(ink);
            } catch (RuntimeException bad) {
                // The record stands without its picture.
            }
        }
        card.add(Ui.label(Ui.when(s.signedAt()), "FlCardDetail"));
        Label hash = Ui.label("SHA-256 " + shortHash(s.reportHash()), "FlHash");
        hash.setName("reportHash");
        card.add(hash);
        card.add(Ui.text(s.verified()
                ? "The checklist, photos, notes and signature still hash to this value."
                : "The record no longer matches what was signed.", "WlMuted"));
        signature.add(card);
        Ui.relayout(form, null);
    }

    private static String shortHash(String hash) {
        if (hash == null || hash.length() < 16) {
            return hash == null ? "" : hash;
        }
        return hash.substring(0, 8) + "..." + hash.substring(hash.length() - 8);
    }

    private void updateBar() {
        bar.removeAll();
        String status = job.status();
        if ("COMPLETED".equals(status)) {
            bar.add(BorderLayout.CENTER, Ui.label(Lang.tr("Completed at {0}",
                    Ui.clock(job.actualEnd() > 0 ? job.actualEnd() : System.currentTimeMillis())),
                    "FlBarDone"));
            form.revalidate();
            return;
        }
        String missing = missing();
        Container text = new Container(BoxLayout.y());
        if ("ON_SITE".equals(status) && missing != null) {
            text.add(Ui.label(missing, "FlBarHint"));
        }
        String label = "ON_SITE".equals(status) ? "Complete job" : TodayForm.action(status);
        Button go = Ui.primary(label, "jobAction", e -> {
            saveChecklist(null);
            String to = "ON_SITE".equals(job.status()) ? "COMPLETED"
                    : "EN_ROUTE".equals(job.status()) ? "ON_SITE" : "EN_ROUTE";
            JobActions.set(job.id(), to, updated -> {
                if (changed != null) {
                    fill(updated);
                    changed.run();
                } else if ("COMPLETED".equals(to)) {
                    close();
                } else {
                    fill(updated);
                }
            });
        });
        go.setEnabled(!"ON_SITE".equals(status) || missing == null);
        bar.add(BorderLayout.NORTH, text);
        bar.add(BorderLayout.CENTER, go);
        form.revalidate();
    }

    /// What still stands between an on-site job and finishing it, in words.
    private String missing() {
        int open = 0;
        for (CheckDto c : checks) {
            if (c.result() == null || c.result().length() == 0) {
                open++;
            }
        }
        if (open > 0) {
            return Lang.count(open, "1 checklist item to answer", "{0} checklist items to answer");
        }
        if ("INSPECTION".equals(job.kind()) && !job.signed()) {
            return Lang.tr("Needs the customer's signature");
        }
        return null;
    }

    private static String name(String result) {
        for (int i = 0; i < RESULTS.length; i++) {
            if (RESULTS[i].equals(result)) {
                return RESULT_NAMES[i];
            }
        }
        return result;
    }

    private static String badge(String result) {
        if ("PASS".equals(result)) {
            return "FlBadgeDone";
        }
        return "FAIL".equals(result) ? "FlBadgeHigh" : "FlBadge";
    }
}
