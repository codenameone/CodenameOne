package com.codenameone.examples.fieldline.field;

import com.codename1.appkit.Net;
import com.codename1.maps.LatLng;
import com.codename1.ui.Form;
import com.codenameone.examples.fieldline.Telemetry;
import com.codenameone.examples.fieldline.api.JobDto;
import com.codenameone.examples.fieldline.api.StatusDto;
import com.codenameone.examples.fieldline.map.Locator;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.Ui;

/// Moving a job along: leave for it, arrive at it, finish it. The position
/// goes with every step, because the server notes a check-in made far from
/// the site and the signed record carries where it was signed.
public final class JobActions {
    private JobActions() {
    }

    /// The next step for `job` from where it stands. A job already on site
    /// opens instead, since finishing it needs the checklist and sign-off.
    public static void advance(Form from, JobDto job, Runnable done) {
        String status = job.status();
        if ("ON_SITE".equals(status)) {
            JobForm.show(from, job.id());
            return;
        }
        set(job.id(), "EN_ROUTE".equals(status) ? "ON_SITE" : "EN_ROUTE", updated -> done.run());
    }

    public interface Updated {
        void updated(JobDto job);
    }

    public static void set(String jobId, final String status, final Updated updated) {
        LatLng here = Locator.here();
        double lat = here == null ? 0 : here.getLatitude();
        double lng = here == null ? 0 : here.getLongitude();
        JobForm.touched();
        Api.field().setStatus(jobId, new StatusDto(status, lat, lng), Net.to(job -> {
            Telemetry.jobStatus(status);
            if ("ON_SITE".equals(status)) {
                Ui.say("Checked in at " + job.siteName());
            } else if ("COMPLETED".equals(status)) {
                Ui.say("Job completed");
            }
            Tracker.start(!"COMPLETED".equals(status));
            updated.updated(job);
        }, Ui::fail));
    }
}
