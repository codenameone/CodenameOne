package com.codenameone.examples.fieldline.field;

import com.codename1.appkit.server.push.PushSender;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.fieldline.api.ChangeDto;
import com.codenameone.examples.fieldline.api.ScheduleRunDto;
import com.codenameone.examples.fieldline.domain.Preference;
import com.codenameone.examples.fieldline.domain.Technician;
import com.codenameone.examples.fieldline.push.PushDevices;
import com.codenameone.examples.fieldline.repository.PreferenceRepository;
import com.codenameone.examples.fieldline.repository.TechnicianRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Telling technicians, on their phones, that their day changed.
///
/// A push and not only a live frame: the app is in a pocket when a colleague
/// calls in sick. One notification per technician per run, naming how many of
/// their jobs moved, and only to those who kept the setting on.
@Component
public class Notices {
    private final PushSender sender;
    private final PushDevices devices;
    private final TechnicianRepository technicians;
    private final PreferenceRepository preferences;

    public Notices(PushSender sender, PushDevices devices, TechnicianRepository technicians,
            PreferenceRepository preferences) {
        this.sender = sender;
        this.devices = devices;
        this.technicians = technicians;
        this.preferences = preferences;
    }

    /// Sends after the run committed. A failure to reach the push service is
    /// logged and never undoes the plan.
    public void scheduleChanged(ScheduleRunDto run) {
        if (run.changes() == null || run.changes().isEmpty()) {
            return;
        }
        Map<String, Integer> touched = new LinkedHashMap<String, Integer>();
        for (ChangeDto c : run.changes()) {
            count(touched, c.fromTech());
            count(touched, c.toTech());
        }
        try {
            for (Map.Entry<String, List<PushTarget>> to : targets(touched).entrySet()) {
                int n = touched.get(to.getKey()).intValue();
                PushNotification message = PushNotification.builder()
                        .title("Your schedule changed")
                        .body(n == 1 ? "One of your jobs moved. Tap to see your day."
                                : n + " of your jobs moved. Tap to see your day.")
                        .collapseKey("schedule")
                        .deepLink("fieldline://today")
                        .data("run", run.id())
                        .build();
                sender.send(to.getValue(), message, null);
            }
        } catch (RuntimeException failed) {
            System.err.println("Could not send schedule notices: " + failed);
        }
    }

    private static void count(Map<String, Integer> touched, String techId) {
        if (techId == null || techId.length() == 0) {
            return;
        }
        Integer was = touched.get(techId);
        touched.put(techId, Integer.valueOf(was == null ? 1 : was.intValue() + 1));
    }

    @Transactional(readOnly = true)
    Map<String, List<PushTarget>> targets(Map<String, Integer> touched) {
        Map<String, List<PushTarget>> out = new LinkedHashMap<String, List<PushTarget>>();
        for (String techId : touched.keySet()) {
            Technician tech = technicians.findById(techId);
            if (tech == null || tech.getUsername().length() == 0) {
                continue;
            }
            Preference pref = preferences.findById(tech.getUsername());
            if (pref != null && !pref.isNotifyScheduleChanges()) {
                continue;
            }
            List<PushTarget> to = new ArrayList<PushTarget>(devices.targets(tech.getUsername()));
            if (!to.isEmpty()) {
                out.put(techId, to);
            }
        }
        return out;
    }
}
