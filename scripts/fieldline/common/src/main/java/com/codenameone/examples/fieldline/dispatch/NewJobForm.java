package com.codenameone.examples.fieldline.dispatch;

import com.codename1.appkit.Net;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.fieldline.api.NewJobDto;
import com.codenameone.examples.fieldline.api.SiteDto;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// A call comes in: what, where, how urgent. The planner places it the moment
/// it is saved -- with the technician who has the skill and the least
/// disruption to the rest of the day -- and the screen says who got it.
public final class NewJobForm {
    private static final String[] KINDS = {"REPAIR", "INSPECTION", "MAINTENANCE", "INSTALL"};
    private static final String[] KIND_NAMES = {"Repair", "Inspection", "Maintenance", "Install"};
    private static final String[] SKILLS = {"HVAC", "ELECTRICAL", "PLUMBING", "FIRE", "ELEVATOR",
        "GAS", "SOLAR", "CONTROLS"};

    private NewJobForm() {
    }

    public static void show(final Form previous, final Runnable created) {
        Api.dispatch().sites(Net.to(sites -> build(previous, created, sites), Ui::fail));
    }

    private static void build(final Form previous, final Runnable created,
            final List<SiteDto> sites) {
        final Form form = Ui.form("New job", "NewJob");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField title = Ui.field(page, "What is wrong", "jobTitle",
                "Rooftop unit not cooling");
        page.add(Ui.label("Site", "WlLabel"));
        final Picker site = new Picker();
        site.setName("jobSite");
        site.setUIID("FlTimePicker");
        String[] names = new String[sites.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = sites.get(i).name();
        }
        site.setStrings(names);
        if (names.length > 0) {
            site.setSelectedString(names[0]);
        }
        page.add(site);
        final String[] kind = {KINDS[0]};
        page.add(Ui.label("Kind", "WlLabel"));
        page.add(Ui.choice("jobKind", KIND_NAMES, KINDS, kind[0], v -> kind[0] = v, 2));
        final String[] skill = {SKILLS[0]};
        page.add(Ui.label("Skill needed", "WlLabel"));
        page.add(Ui.choice("jobSkill", new String[] {"HVAC", "Electrical", "Plumbing", "Fire",
            "Elevator", "Gas", "Solar", "Controls"}, SKILLS, skill[0], v -> skill[0] = v, 4));
        final String[] priority = {"3"};
        page.add(Ui.label("Priority", "WlLabel"));
        page.add(Ui.choice("jobPriority", new String[] {"Low", "Normal", "High", "Emergency"},
                new String[] {"2", "3", "4", "5"}, priority[0], v -> priority[0] = v));
        final String[] hours = {"90"};
        page.add(Ui.label("Expected time on site", "WlLabel"));
        page.add(Ui.choice("jobDuration", new String[] {"45 min", "1 h 30", "3 h", "4 h"},
                new String[] {"45", "90", "180", "240"}, hours[0], v -> hours[0] = v));
        final String[] due = {"0"};
        page.add(Ui.label("Due", "WlLabel"));
        page.add(Ui.choice("jobDue", new String[] {"By the SLA", "Today", "Within 3 days"},
                new String[] {"0", "today", "3d"}, due[0], v -> due[0] = v));
        page.add(Ui.text("Gas, high-voltage, roof and fire work goes only to someone holding a "
                + "valid certificate; the site's equipment decides which.", "WlMuted"));
        page.add(Ui.label("Details", "WlLabel"));
        final TextArea details = new TextArea("", 3, 30);
        details.setUIID("WlField");
        details.setName("jobDetails");
        details.setHint(Lang.tr("What the customer reported"));
        page.add(details);

        final SpanLabel result = Ui.text("", "WlError");
        result.setName("error");
        final Button save = Ui.primary("Create and place", "createJob", null);
        save.addActionListener(e -> {
            int index = 0;
            String chosen = site.getSelectedString();
            for (int i = 0; i < sites.size(); i++) {
                if (sites.get(i).name().equals(chosen)) {
                    index = i;
                }
            }
            if (sites.isEmpty()) {
                return;
            }
            List<String> skills = new ArrayList<String>();
            skills.add(skill[0]);
            long now = System.currentTimeMillis();
            NewJobDto job = new NewJobDto(title.getText().trim(), kind[0],
                    Integer.parseInt(priority[0]), sites.get(index).id(), details.getText(),
                    Integer.parseInt(hours[0]), skills, now, now + 3L * 24 * 3600000L,
                    dueAt(due[0], now, Integer.parseInt(hours[0])), null, null, null);
            save.setEnabled(false);
            Api.dispatch().createJob(job, Net.to(placed -> {
                if (placed.techName() != null && placed.techName().length() > 0) {
                    Ui.say(Lang.tr("Assigned to {0} at {1}", placed.techName(),
                            Ui.when(placed.plannedStart())));
                } else {
                    Ui.say("Created. Nobody can take it yet; it waits on the board.");
                }
                previous.showBack();
                created.run();
            }, (status, message) -> {
                save.setEnabled(true);
                result.setText(message);
                Ui.refresh(form);
            }));
        });
        page.add(result).add(save);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// When the dispatcher wants the job done: 0 leaves it to the SLA the
    /// server applies.
    private static long dueAt(String choice, long now, int minutes) {
        if ("today".equals(choice)) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTime(new java.util.Date(now));
            c.set(java.util.Calendar.HOUR_OF_DAY, 18);
            c.set(java.util.Calendar.MINUTE, 0);
            return Math.max(now + minutes * 60000L, c.getTime().getTime());
        }
        if ("3d".equals(choice)) {
            return now + 3L * 24 * 3600000L;
        }
        return 0;
    }
}
