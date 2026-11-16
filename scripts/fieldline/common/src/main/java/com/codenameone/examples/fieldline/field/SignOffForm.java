package com.codenameone.examples.fieldline.field;

import com.codename1.appkit.Net;
import com.codename1.components.SignatureComponent;
import com.codename1.components.SpanLabel;
import com.codename1.io.Log;
import com.codename1.maps.LatLng;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.Base64;
import com.codenameone.examples.fieldline.Telemetry;
import com.codenameone.examples.fieldline.api.CheckDto;
import com.codenameone.examples.fieldline.api.JobDto;
import com.codenameone.examples.fieldline.api.SignatureDto;
import com.codenameone.examples.fieldline.map.Locator;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Ui;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// The customer signs for the work.
///
/// The screen first shows what they are signing -- the checklist results, in
/// a summary they can read in ten seconds -- then their name and the pad. The
/// server seals the record: it hashes the job, every answer, the photos, the
/// notes, the signer, the time, the place and the signature together, and
/// answers with the hash. Anyone can later ask it to hash the record again
/// and say whether it still matches.
public final class SignOffForm {
    private SignOffForm() {
    }

    public static void show(final Form previous, final JobDto job, final Runnable signed) {
        final Form form = Ui.form("Sign-off", "SignOff");
        Ui.back(form, previous);
        Container page = Ui.page();

        Container summary = Ui.card();
        summary.add(Ui.label(job.title(), "FlCardTitle"));
        summary.add(Ui.label(job.siteName() + Ui.DOT + job.customer(), "FlCardDetail"));
        int pass = 0;
        int fail = 0;
        int na = 0;
        for (CheckDto c : job.checklist()) {
            if ("PASS".equals(c.result())) {
                pass++;
            } else if ("FAIL".equals(c.result())) {
                fail++;
            } else {
                na++;
            }
        }
        Container counts = new Container(new com.codename1.ui.layouts.FlowLayout());
        counts.add(Ui.badge(Lang.tr("{0} passed", String.valueOf(pass)), "FlBadgeDone"));
        if (fail > 0) {
            counts.add(Ui.badge(Lang.tr("{0} failed", String.valueOf(fail)), "FlBadgeHigh"));
        }
        if (na > 0) {
            counts.add(Ui.badge(Lang.tr("{0} not applicable", String.valueOf(na)), "FlBadge"));
        }
        counts.add(Ui.badge(Lang.count(job.photos(), "1 photo", "{0} photos"), "FlBadge"));
        summary.add(counts);
        for (CheckDto c : job.checklist()) {
            if ("FAIL".equals(c.result())) {
                summary.add(Ui.text("\u2022 " + c.label() + (c.note() == null
                        || c.note().length() == 0 ? "" : ": " + c.note()), "FlCardText"));
            }
        }
        page.add(summary);

        final TextField name = Ui.field(page, "Customer's name", "signerName",
                "As they would print it");
        page.add(Ui.label("Signature", "WlLabel"));
        final SignatureComponent pad = new SignatureComponent();
        pad.setName("signaturePad");
        pad.setUIID("FlSignaturePad");
        page.add(pad);
        page.add(Ui.text("By signing, the customer confirms the work above was done. The "
                + "time and place are recorded with it.", "WlMuted"));

        final SpanLabel error = Ui.text("", "WlError");
        error.setName("error");
        final Button submit = Ui.primary("Seal and sign", "submitSignature", null);
        FontImage.setMaterialIcon(submit, FontImage.MATERIAL_VERIFIED, 3.6f);
        submit.addActionListener(e -> {
            Image ink = pad.getSignatureImage();
            if (name.getText().trim().length() == 0) {
                error.setText(Lang.tr("The customer's name is needed."));
                Ui.refresh(form);
                return;
            }
            if (ink == null) {
                error.setText(Lang.tr("Ask the customer to sign in the box."));
                Ui.refresh(form);
                return;
            }
            String png = png(ink);
            if (png == null) {
                error.setText(Lang.tr("The signature could not be read. Try again."));
                Ui.refresh(form);
                return;
            }
            LatLng here = Locator.here();
            SignatureDto request = new SignatureDto(job.id(), name.getText().trim(), png,
                    System.currentTimeMillis(), here == null ? 0 : here.getLatitude(),
                    here == null ? 0 : here.getLongitude(), "", false);
            submit.setEnabled(false);
            error.setText("");
            JobForm.touched();
            Api.field().sign(job.id(), request, Net.to(sealed -> {
                Telemetry.signed();
                Ui.say("Signed and sealed");
                previous.showBack();
                signed.run();
            }, (status, message) -> {
                submit.setEnabled(true);
                error.setText(message);
                Ui.refresh(form);
            }));
        });
        page.add(error).add(submit);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    private static String png(Image ink) {
        ImageIO io = ImageIO.getImageIO();
        if (io == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            io.save(ink, out, ImageIO.FORMAT_PNG, 1f);
            return Base64.encodeNoNewline(out.toByteArray());
        } catch (IOException failed) {
            Log.e(failed);
            return null;
        }
    }
}
