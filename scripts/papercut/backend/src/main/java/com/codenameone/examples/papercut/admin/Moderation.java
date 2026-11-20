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
package com.codenameone.examples.papercut.admin;

import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.papercut.Clock;
import com.codenameone.examples.papercut.Ids;
import com.codenameone.examples.papercut.account.Accounts;
import com.codenameone.examples.papercut.api.AdminActionDto;
import com.codenameone.examples.papercut.api.AuditDto;
import com.codenameone.examples.papercut.api.PlayerDto;
import com.codenameone.examples.papercut.domain.AdminAction;
import com.codenameone.examples.papercut.domain.Profile;
import com.codenameone.examples.papercut.domain.ScoreRun;
import com.codenameone.examples.papercut.repository.AdminActionRepository;
import com.codenameone.examples.papercut.repository.ProfileRepository;
import com.codenameone.examples.papercut.repository.ScoreRunRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// What an admin does to a player, every step written to the audit log in
/// the same transaction as the change, so the log cannot miss one.
@Component
public class Moderation {
    private final Accounts accounts;
    private final ProfileRepository profiles;
    private final ScoreRunRepository runs;
    private final AdminActionRepository audit;

    public Moderation(Accounts accounts, ProfileRepository profiles, ScoreRunRepository runs,
            AdminActionRepository audit) {
        this.accounts = accounts;
        this.profiles = profiles;
        this.runs = runs;
        this.audit = audit;
    }

    @Transactional(rollbackFor = IOException.class)
    public PlayerDto act(String admin, String username, AdminActionDto action) throws IOException {
        if (action == null) {
            throw new ResponseStatusException(400, "No action");
        }
        String kind = Text.oneOf(action.action(),
                new String[] {"WARN", "SUSPEND", "BAN", "UNBAN", "RESET_SCORES"}, "", "the action");
        String reason = Text.optional(action.reason(), 255, "the reason");
        Profile p = accounts.profile(username);
        if (admin.equals(username) && !"WARN".equals(kind)) {
            // An admin who bans themselves loses the console that would undo it.
            throw new ResponseStatusException(400, "Not on your own account");
        }
        if ("SUSPEND".equals(kind)) {
            int days = action.days() <= 0 ? 1 : Math.min(action.days(), 365);
            p.setSuspendedUntil(Instant.ofEpochMilli(Clock.now() + days * Clock.DAY));
            accounts.setEnabled(username, false);
            kind = "SUSPEND " + days + "d";
        } else if ("BAN".equals(kind)) {
            p.setBanned(true);
            accounts.setEnabled(username, false);
        } else if ("UNBAN".equals(kind)) {
            p.setBanned(false);
            p.setSuspendedUntil(Instant.EPOCH);
            accounts.setEnabled(username, true);
        } else if ("RESET_SCORES".equals(kind)) {
            for (ScoreRun r : runs.of(username)) {
                r.setStruck(true);
                runs.save(r);
            }
            p.setBestClassic(0);
            p.setBestFold(0);
            p.setBestDaily(0);
        }
        profiles.save(p);
        record(admin, username, kind, reason == null ? "" : reason);
        return accounts.toDto(p);
    }

    /// Writes one audit row, inside the caller's transaction.
    public void record(String admin, String username, String action, String reason) throws IOException {
        AdminAction a = new AdminAction();
        a.setId(Ids.next());
        a.setAdmin(admin);
        a.setUsername(username);
        a.setAction(action);
        a.setReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
        a.setCreatedAt(Instant.now());
        audit.insert(a);
    }

    @Transactional(readOnly = true)
    public List<AuditDto> audit() {
        List<AuditDto> out = new ArrayList<AuditDto>();
        for (AdminAction a : audit.newest(new PageRequest(0, 200))) {
            out.add(new AuditDto(a.getId(), a.getAdmin(), a.getUsername(), a.getAction(), a.getReason(),
                    a.getCreatedAt().toEpochMilli()));
        }
        return out;
    }

    /// Lifts suspensions that have run their course.
    @Scheduled(fixedDelay = 60000, initialDelay = 60000, lock = "papercut-suspensions")
    @Transactional
    public void liftExpired() {
        for (Profile p : profiles.expired(Instant.EPOCH, Instant.ofEpochMilli(Clock.now()))) {
            p.setSuspendedUntil(Instant.EPOCH);
            profiles.save(p);
            accounts.setEnabled(p.getUsername(), true);
        }
    }
}
