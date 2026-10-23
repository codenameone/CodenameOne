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
package com.codenameone.examples.reel.admin;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Kinds;
import com.codenameone.examples.reel.account.Devices;
import com.codenameone.examples.reel.account.Grants;
import com.codenameone.examples.reel.api.SharingAlertDto;
import com.codenameone.examples.reel.api.SharingDecisionDto;
import com.codenameone.examples.reel.domain.SharingAlert;
import com.codenameone.examples.reel.playback.Playbacks;
import com.codenameone.examples.reel.repository.SharingAlertRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Password-sharing alerts ([Playbacks] raises them) and what an admin does
/// about one: dismiss it (a family on holiday), or sign the account out
/// everywhere, which ends its streams and its sign-ins so the owner has to
/// sign in again -- and whoever they shared the password with does too.
@Component
public class Sharing {
    public static final String DISMISS = "DISMISS";
    public static final String SIGN_OUT_ALL = "SIGN_OUT_ALL";

    private final SharingAlertRepository alerts;
    private final Playbacks playbacks;
    private final Grants grants;
    private final Devices devices;

    public Sharing(SharingAlertRepository alerts, Playbacks playbacks, Grants grants, Devices devices) {
        this.alerts = alerts;
        this.playbacks = playbacks;
        this.grants = grants;
        this.devices = devices;
    }

    @Transactional(readOnly = true)
    public List<SharingAlertDto> list(String status) {
        List<SharingAlert> rows = status == null || status.length() == 0
                ? alerts.findByCreatedAtGreaterThanOrderByCreatedAtDesc(0L)
                : alerts.findByStatusOrderByCreatedAtDesc(status);
        return dtos(rows);
    }

    @Transactional(readOnly = true)
    public List<SharingAlertDto> forUser(String username) {
        return dtos(alerts.findByUsernameOrderByCreatedAtDesc(username));
    }

    @Transactional(rollbackFor = IOException.class)
    public SharingAlertDto decide(String by, String id, SharingDecisionDto decision) throws IOException {
        SharingAlert a = alerts.findById(id);
        if (a == null) {
            throw new ResponseStatusException(404, "No such alert");
        }
        if (!Kinds.OPEN.equals(a.getStatus())) {
            throw new ResponseStatusException(409, "Already decided");
        }
        String action = decision == null ? "" : decision.action();
        if (SIGN_OUT_ALL.equals(action)) {
            playbacks.endAll(a.getUsername(), "SIGNED_OUT");
            grants.endAll(a.getUsername());
            devices.signOutAll(a.getUsername());
            a.setStatus(Kinds.UPHELD);
        } else if (DISMISS.equals(action)) {
            a.setStatus(Kinds.DISMISSED);
        } else {
            throw new ResponseStatusException(400, "Dismiss, or sign the account out everywhere");
        }
        a.setDecidedBy(by);
        String note = decision.note() == null ? "" : decision.note().trim();
        a.setNote(note.length() > 255 ? note.substring(0, 255) : note);
        alerts.save(a);
        return dto(a);
    }

    private static List<SharingAlertDto> dtos(List<SharingAlert> rows) {
        List<SharingAlertDto> out = new ArrayList<SharingAlertDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    static SharingAlertDto dto(SharingAlert a) {
        return new SharingAlertDto(a.getId(), a.getUsername(), a.getKind(), a.getDetail(), a.getDistinctIps(),
                a.getDistinctCountries(), a.getStatus(), a.getCreatedAt());
    }
}
