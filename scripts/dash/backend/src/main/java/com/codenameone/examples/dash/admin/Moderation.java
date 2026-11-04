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
package com.codenameone.examples.dash.admin;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.dash.Clock;
import com.codenameone.examples.dash.Ids;
import com.codenameone.examples.dash.Text;
import com.codenameone.examples.dash.account.Accounts;
import com.codenameone.examples.dash.api.AdminActionDto;
import com.codenameone.examples.dash.api.AuditDto;
import com.codenameone.examples.dash.api.PlayerDto;
import com.codenameone.examples.dash.domain.AdminAction;
import com.codenameone.examples.dash.domain.Profile;
import com.codenameone.examples.dash.game.Rooms;
import com.codenameone.examples.dash.repository.AdminActionRepository;
import com.codenameone.examples.dash.repository.ProfileRepository;
import com.codenameone.examples.dash.shop.Catalog;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// What an admin can do to a player, and the audit log that records it.
///
/// Every action is written to `admin_action` in the same transaction as the
/// change itself, so the log cannot miss one.
// tag::dash-moderation[]
@Component
public class Moderation {
    public static final String WARN = "WARN";
    public static final String MUTE = "MUTE";
    public static final String UNMUTE = "UNMUTE";
    public static final String SUSPEND = "SUSPEND";
    public static final String BAN = "BAN";
    public static final String UNBAN = "UNBAN";
    public static final String RESET_TROPHIES = "RESET_TROPHIES";
    public static final String REVOKE_ITEMS = "REVOKE_ITEMS";
    static final String[] ACTIONS = {WARN, MUTE, UNMUTE, SUSPEND, BAN, UNBAN, RESET_TROPHIES, REVOKE_ITEMS};

    private final Accounts accounts;
    private final ProfileRepository profiles;
    private final AdminActionRepository audit;
    private final Rooms rooms;

    public Moderation(Accounts accounts, ProfileRepository profiles, AdminActionRepository audit, Rooms rooms) {
        this.accounts = accounts;
        this.profiles = profiles;
        this.audit = audit;
        this.rooms = rooms;
    }

    public PlayerDto act(String admin, String username, AdminActionDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Choose an action");
        }
        String action = Text.oneOf(request.action(), ACTIONS, "", "the action");
        if (action.length() == 0) {
            throw new ResponseStatusException(400, "Choose an action");
        }
        if (admin.equals(username) && !WARN.equals(action)) {
            throw new ResponseStatusException(400, "Not on your own account");
        }
        String reason = Text.required(request.reason(), 255, "the reason");
        boolean closes = apply(admin, username, action, reason, request.days());
        if (closes) {
            rooms.kick(username);
        }
        return accounts.describe(username);
    }

    @Transactional(rollbackFor = IOException.class)
    boolean apply(String admin, String username, String action, String reason, int days) throws IOException {
        if (profiles.hold(username) == 0) {
            throw new ResponseStatusException(404, "No such account");
        }
        Profile p = profiles.findById(username);
        boolean closes = false;
        if (MUTE.equals(action) || UNMUTE.equals(action)) {
            p.setMuted(MUTE.equals(action));
        } else if (SUSPEND.equals(action)) {
            if (days < 1 || days > 365) {
                throw new ResponseStatusException(400, "Suspend for 1 to 365 days");
            }
            p.setSuspendedUntil(Instant.now().plusMillis(days * Clock.DAY));
            accounts.setEnabled(username, false);
            closes = true;
        } else if (BAN.equals(action)) {
            p.setBanned(true);
            accounts.setEnabled(username, false);
            closes = true;
        } else if (UNBAN.equals(action)) {
            p.setBanned(false);
            p.setSuspendedUntil(Instant.EPOCH);
            accounts.setEnabled(username, true);
        } else if (RESET_TROPHIES.equals(action)) {
            p.setTrophies(0);
        } else if (REVOKE_ITEMS.equals(action)) {
            p.setOwned(Catalog.DEFAULT_OUTFIT + "," + Catalog.DEFAULT_EMOTES);
            p.setOutfit(Catalog.DEFAULT_OUTFIT);
        }
        profiles.save(p);
        AdminAction a = new AdminAction();
        a.setId(Ids.next());
        a.setAdmin(admin);
        a.setUsername(username);
        a.setAction(SUSPEND.equals(action) ? SUSPEND + " " + days + "d" : action);
        a.setReason(reason);
        a.setCreatedAt(Instant.now());
        audit.insert(a);
        return closes;
    }

    /// Lifts suspensions that have run their course.
    @Scheduled(fixedDelay = 60000, initialDelay = 60000, lock = "dash-suspensions")
    public void liftExpired() {
        lift(Clock.now());
    }

    @Transactional
    public int lift(long now) {
        int lifted = 0;
        for (Profile p : profiles.findAll()) {
            if (!p.isBanned() && p.getSuspendedUntil().isAfter(Instant.EPOCH)
                    && p.getSuspendedUntil().toEpochMilli() <= now) {
                p.setSuspendedUntil(Instant.EPOCH);
                profiles.save(p);
                accounts.setEnabled(p.getUsername(), true);
                lifted++;
            }
        }
        return lifted;
    }

    @Transactional(readOnly = true)
    public List<AuditDto> newest() {
        return toDtos(audit.newest(new PageRequest(0, 200)));
    }

    @Transactional(readOnly = true)
    public List<AuditDto> of(String username) {
        return toDtos(audit.of(username, new PageRequest(0, 50)));
    }

    private static List<AuditDto> toDtos(List<AdminAction> rows) {
        List<AuditDto> out = new ArrayList<AuditDto>();
        for (AdminAction a : rows) {
            out.add(new AuditDto(a.getId(), a.getAdmin(), a.getUsername(), a.getAction(), a.getReason(),
                    a.getCreatedAt().toEpochMilli()));
        }
        return out;
    }
}
// end::dash-moderation[]
