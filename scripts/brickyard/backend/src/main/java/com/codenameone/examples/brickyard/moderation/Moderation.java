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
package com.codenameone.examples.brickyard.moderation;

import com.codename1.appkit.server.Caller;
import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.Roles;
import com.codenameone.examples.brickyard.Staff;
import com.codenameone.examples.brickyard.account.Accounts;
import com.codenameone.examples.brickyard.api.ActionDto;
import com.codenameone.examples.brickyard.api.AppealDto;
import com.codenameone.examples.brickyard.api.AuditDto;
import com.codenameone.examples.brickyard.api.FeaturedDto;
import com.codenameone.examples.brickyard.api.FraudFlagDto;
import com.codenameone.examples.brickyard.api.KpiDto;
import com.codenameone.examples.brickyard.api.ReportDto;
import com.codenameone.examples.brickyard.api.RoleDto;
import com.codenameone.examples.brickyard.api.SanctionDto;
import com.codenameone.examples.brickyard.domain.AnalyticsEvent;
import com.codenameone.examples.brickyard.domain.Appeal;
import com.codenameone.examples.brickyard.domain.AuditLog;
import com.codenameone.examples.brickyard.domain.ChatMessage;
import com.codenameone.examples.brickyard.domain.Experience;
import com.codenameone.examples.brickyard.domain.Featured;
import com.codenameone.examples.brickyard.domain.FraudFlag;
import com.codenameone.examples.brickyard.domain.Presence;
import com.codenameone.examples.brickyard.domain.Report;
import com.codenameone.examples.brickyard.domain.Sanction;
import com.codenameone.examples.brickyard.domain.StorePurchase;
import com.codenameone.examples.brickyard.repository.AnalyticsEventRepository;
import com.codenameone.examples.brickyard.repository.AppealRepository;
import com.codenameone.examples.brickyard.repository.AuditLogRepository;
import com.codenameone.examples.brickyard.repository.ChatMessageRepository;
import com.codenameone.examples.brickyard.repository.ExperienceRepository;
import com.codenameone.examples.brickyard.repository.FeaturedRepository;
import com.codenameone.examples.brickyard.repository.FraudFlagRepository;
import com.codenameone.examples.brickyard.repository.PresenceRepository;
import com.codenameone.examples.brickyard.repository.ReportRepository;
import com.codenameone.examples.brickyard.repository.SanctionRepository;
import com.codenameone.examples.brickyard.repository.StorePurchaseRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/// The admin console's backend: reports and the moderator queue, sanctions,
/// appeals, staff roles, featuring and the platform KPIs. Every change
/// writes an [Audit] row in its own transaction.
@Component
public class Moderation {
    public static final String OPEN = "OPEN";
    private static final String[] TARGETS = {"EXPERIENCE", "ASSET", "CHAT", "USER"};
    private static final String[] STAFF_ROLES = {Roles.ADMIN, Roles.MODERATOR, Roles.SUPPORT, Roles.CREATOR};

    private final Accounts accounts;
    private final Audit audit;
    private final ReportRepository reports;
    private final SanctionRepository sanctions;
    private final AppealRepository appeals;
    private final AuditLogRepository log;
    private final FeaturedRepository featured;
    private final FraudFlagRepository fraud;
    private final ExperienceRepository experiences;
    private final ChatMessageRepository chat;
    private final PresenceRepository presence;
    private final StorePurchaseRepository purchases;
    private final AnalyticsEventRepository events;
    private final JdbcUserDetailsManager users;

    public Moderation(Accounts accounts, Audit audit, ReportRepository reports, SanctionRepository sanctions,
            AppealRepository appeals, AuditLogRepository log, FeaturedRepository featured,
            FraudFlagRepository fraud, ExperienceRepository experiences, ChatMessageRepository chat,
            PresenceRepository presence, StorePurchaseRepository purchases, AnalyticsEventRepository events,
            JdbcUserDetailsManager users) {
        this.accounts = accounts;
        this.audit = audit;
        this.reports = reports;
        this.sanctions = sanctions;
        this.appeals = appeals;
        this.log = log;
        this.featured = featured;
        this.fraud = fraud;
        this.experiences = experiences;
        this.chat = chat;
        this.presence = presence;
        this.purchases = purchases;
        this.events = events;
        this.users = users;
    }

    private static String oneOf(String value, String[] allowed, String what) {
        for (String a : allowed) {
            if (a.equalsIgnoreCase(value)) {
                return a;
            }
        }
        throw new ResponseStatusException(400, "That is not " + what);
    }

    @Transactional(rollbackFor = IOException.class)
    public ReportDto report(String reporter, ReportDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to report");
        }
        Report r = new Report();
        r.setId(Ids.next());
        r.setReporter(reporter);
        r.setTargetType(oneOf(request.targetType(), TARGETS, "a report target"));
        r.setTargetId(Text.required(request.targetId(), 190, "what you report").trim());
        if ("USER".equals(r.getTargetType()) && r.getTargetId().equals(reporter)) {
            throw new ResponseStatusException(400, "You cannot report yourself");
        }
        r.setReason(Text.required(request.reason(), 400, "a reason").trim());
        r.setStatus(OPEN);
        r.setCreatedAt(Clock.now());
        reports.insert(r);
        return toDto(r);
    }

    public List<ReportDto> queue(String status) {
        Staff.require(Staff.VIEW);
        List<ReportDto> out = new ArrayList<ReportDto>();
        for (Report r : reports.withStatus(status == null || status.length() == 0 ? OPEN : status)) {
            out.add(toDto(r));
        }
        return out;
    }

    /// Resolves a report with DISMISS, or with a sanction (WARN, TEMP_BAN,
    /// PERM_BAN, MUTE) on the account behind it, or UNPUBLISH for an
    /// experience. The account is the action's target when given, else the
    /// reported user, the chat line's sender or the experience's owner.
    @Transactional(rollbackFor = IOException.class)
    public ReportDto resolve(String reportId, ActionDto action) throws IOException {
        Staff.require(Staff.MODERATE);
        Report r = reportId == null ? null : reports.findById(reportId);
        if (r == null) {
            throw new ResponseStatusException(404, "No such report");
        }
        if (!OPEN.equals(r.getStatus())) {
            throw new ResponseStatusException(409, "That report was already resolved");
        }
        if (action == null || action.kind() == null) {
            throw new ResponseStatusException(400, "Say what to do");
        }
        String kind = oneOf(action.kind(), new String[] {"DISMISS", Sanctions.WARN, Sanctions.TEMP_BAN,
            Sanctions.PERM_BAN, Sanctions.MUTE, "UNPUBLISH"}, "an action");
        String reason = action.reason() == null || action.reason().length() == 0 ? r.getReason() : action.reason();
        if ("UNPUBLISH".equals(kind)) {
            if (!"EXPERIENCE".equals(r.getTargetType())) {
                throw new ResponseStatusException(400, "Only an experience can be unpublished");
            }
            unpublish(r.getTargetId(), reason);
        } else if (!"DISMISS".equals(kind)) {
            sanction(new SanctionDto(null, accountBehind(r, action.target()), kind, reason,
                    action.minutes(), 0));
        }
        r.setStatus("DISMISS".equals(kind) ? "DISMISSED" : "RESOLVED");
        r.setResolvedBy(Caller.name());
        reports.save(r);
        audit.record("REPORT_" + r.getStatus(), r.getId(), kind + "; " + reason);
        return toDto(r);
    }

    private String accountBehind(Report r, String target) {
        if (target != null && target.length() > 0) {
            return target;
        }
        if ("USER".equals(r.getTargetType())) {
            return r.getTargetId();
        }
        if ("CHAT".equals(r.getTargetType())) {
            ChatMessage m = chat.findById(r.getTargetId());
            if (m != null) {
                return m.getSender();
            }
        }
        if ("EXPERIENCE".equals(r.getTargetType())) {
            Experience e = experiences.findById(r.getTargetId());
            if (e != null) {
                return e.getOwner();
            }
        }
        throw new ResponseStatusException(400, "Name the account to act on");
    }

    /// A sanction's `untilAt` in the request is read as minutes: a TEMP_BAN
    /// or MUTE needs them (a mute defaults to a day), a PERM_BAN never ends,
    /// and a WARN is over as it is given.
    @Transactional(rollbackFor = IOException.class)
    public SanctionDto sanction(SanctionDto request) throws IOException {
        Staff.require(Staff.MODERATE);
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to do");
        }
        accounts.account(request.username());
        String kind = oneOf(request.kind(), new String[] {Sanctions.WARN, Sanctions.TEMP_BAN,
            Sanctions.PERM_BAN, Sanctions.MUTE}, "a sanction");
        long now = Clock.now();
        long minutes = request.untilAt();
        long until;
        if (Sanctions.PERM_BAN.equals(kind)) {
            until = Long.MAX_VALUE;
        } else if (Sanctions.WARN.equals(kind)) {
            until = now;
        } else {
            if (minutes <= 0 && Sanctions.MUTE.equals(kind)) {
                minutes = 24 * 60;
            }
            if (minutes <= 0 || minutes > 365L * 24 * 60) {
                throw new ResponseStatusException(400, "A temporary ban lasts 1 minute to a year");
            }
            until = now + minutes * Clock.MINUTE;
        }
        Sanction s = new Sanction();
        s.setId(Ids.next());
        s.setUsername(request.username());
        s.setKind(kind);
        s.setReason(Text.required(request.reason(), 400, "a reason"));
        s.setUntilAt(until);
        s.setIssuedBy(Caller.name());
        s.setCreatedAt(now);
        sanctions.insert(s);
        audit.record(kind, s.getUsername(), s.getReason());
        return toDto(s);
    }

    public List<SanctionDto> sanctionsOf(String username, boolean staff) {
        if (staff) {
            Staff.require(Staff.VIEW);
        }
        List<SanctionDto> out = new ArrayList<SanctionDto>();
        for (Sanction s : sanctions.of(username)) {
            out.add(toDto(s));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public AuditDto unpublish(String experienceId, String reason) throws IOException {
        Staff.require(Staff.MODERATE);
        Experience e = experienceId == null ? null : experiences.findById(experienceId);
        if (e == null) {
            throw new ResponseStatusException(404, "No such experience");
        }
        String why = Text.required(reason, 400, "a reason");
        e.setPublishedVersion(0);
        experiences.save(e);
        return audit.record("UNPUBLISH", e.getId(), why);
    }

    /// One open appeal per sanction, by the sanctioned account.
    @Transactional(rollbackFor = IOException.class)
    public AppealDto appeal(String username, AppealDto request) throws IOException {
        Sanction s = request == null || request.sanctionId() == null ? null : sanctions.findById(request.sanctionId());
        if (s == null || !s.getUsername().equals(username)) {
            throw new ResponseStatusException(404, "No such sanction");
        }
        for (Appeal a : appeals.withStatus(OPEN)) {
            if (a.getSanctionId().equals(s.getId())) {
                throw new ResponseStatusException(409, "That sanction is already under appeal");
            }
        }
        Appeal a = new Appeal();
        a.setId(Ids.next());
        a.setSanctionId(s.getId());
        a.setUsername(username);
        a.setBody(Text.required(request.body(), 1000, "what you want to say").trim());
        a.setStatus(OPEN);
        a.setCreatedAt(Clock.now());
        appeals.insert(a);
        return toDto(a);
    }

    public List<AppealDto> openAppeals() {
        Staff.require(Staff.VIEW);
        List<AppealDto> out = new ArrayList<AppealDto>();
        for (Appeal a : appeals.withStatus(OPEN)) {
            out.add(toDto(a));
        }
        return out;
    }

    /// GRANT lifts the sanction; DENY leaves it.
    @Transactional(rollbackFor = IOException.class)
    public AppealDto decide(String appealId, ActionDto action) throws IOException {
        Staff.require(Staff.MODERATE);
        Appeal a = appealId == null ? null : appeals.findById(appealId);
        if (a == null || !OPEN.equals(a.getStatus())) {
            throw new ResponseStatusException(404, "No such open appeal");
        }
        String kind = oneOf(action == null ? null : action.kind(), new String[] {"GRANT", "DENY"}, "GRANT or DENY");
        a.setStatus("GRANT".equals(kind) ? "GRANTED" : "DENIED");
        a.setDecidedBy(Caller.name());
        appeals.save(a);
        if ("GRANT".equals(kind)) {
            Sanction s = sanctions.findById(a.getSanctionId());
            s.setLifted(1);
            sanctions.save(s);
        }
        audit.record("APPEAL_" + a.getStatus(), a.getUsername(), action.reason());
        return toDto(a);
    }

    public List<AuditDto> audit() {
        Staff.require(Staff.VIEW);
        List<AuditDto> out = new ArrayList<AuditDto>();
        for (AuditLog a : log.since(Clock.now() - 30 * Clock.DAY)) {
            out.add(Audit.toDto(a));
        }
        return out;
    }

    /// Grants or revokes a staff role (or CREATOR). An admin cannot revoke
    /// their own ADMIN, so the platform never loses its last one by accident.
    @Transactional(rollbackFor = IOException.class)
    public AuditDto setRole(RoleDto request) throws IOException {
        Staff.require(Staff.ADMINISTER);
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to do");
        }
        accounts.account(request.username());
        String role = oneOf(request.role(), STAFF_ROLES, "a role");
        boolean grant = request.grant() != 0;
        if (!grant && Roles.ADMIN.equals(role) && request.username().equals(Caller.name())) {
            throw new ResponseStatusException(409, "You cannot revoke your own ADMIN");
        }
        UserDetails signIn = users.loadUserByUsername(request.username());
        List<String> roles = new ArrayList<String>();
        Iterator<? extends GrantedAuthority> it = signIn.getAuthorities().iterator();
        while (it.hasNext()) {
            String authority = it.next().getAuthority();
            roles.add(authority.startsWith("ROLE_") ? authority.substring(5) : authority);
        }
        roles.remove(role);
        if (grant) {
            roles.add(role);
        }
        users.updateUser(User.withUserDetails(signIn).roles(roles.toArray(new String[0])).build());
        return audit.record(grant ? "ROLE_GRANT" : "ROLE_REVOKE", request.username(), role);
    }

    @Transactional(rollbackFor = IOException.class)
    public FeaturedDto feature(FeaturedDto request) throws IOException {
        Staff.require(Staff.ADMINISTER);
        Experience e = request == null || request.experienceId() == null ? null
                : experiences.findById(request.experienceId());
        if (e == null || e.getPublishedVersion() < 1) {
            throw new ResponseStatusException(404, "No such published experience");
        }
        Featured f = new Featured();
        f.setId(Ids.next());
        f.setRowName(Text.required(request.rowName(), 64, "a row").trim());
        f.setExperienceId(e.getId());
        f.setOrdinal(request.ordinal());
        featured.insert(f);
        audit.record("FEATURE", e.getId(), f.getRowName());
        return toDto(f);
    }

    @Transactional(rollbackFor = IOException.class)
    public FeaturedDto unfeature(String id) throws IOException {
        Staff.require(Staff.ADMINISTER);
        Featured f = id == null ? null : featured.findById(id);
        if (f == null) {
            throw new ResponseStatusException(404, "Nothing featured with that id");
        }
        featured.delete(f);
        audit.record("UNFEATURE", f.getExperienceId(), f.getRowName());
        return toDto(f);
    }

    /// The home rows, by row name and then place in the row; an experience
    /// unpublished since it was featured drops out.
    public List<FeaturedDto> home() {
        List<Featured> all = new ArrayList<Featured>(featured.findAll());
        Collections.sort(all, (a, b) -> {
            int c = a.getRowName().compareTo(b.getRowName());
            return c != 0 ? c : Integer.compare(a.getOrdinal(), b.getOrdinal());
        });
        List<FeaturedDto> out = new ArrayList<FeaturedDto>();
        for (Featured f : all) {
            Experience e = experiences.findById(f.getExperienceId());
            if (e != null && e.getPublishedVersion() > 0) {
                out.add(toDto(f));
            }
        }
        return out;
    }

    /// The last day: DAU is everyone with presence or a visit; CCU is live
    /// presence (there is no servers table yet); revenue is store Bricks not
    /// refunded; the error rate is script errors per thousand sessions.
    public KpiDto kpis() {
        Staff.require(Staff.ADMINISTER);
        long now = Clock.now();
        long dayAgo = now - Clock.DAY;
        Set<String> daily = new HashSet<String>();
        long ccu = 0;
        for (Presence p : presence.since(dayAgo)) {
            daily.add(p.getId());
            if (p.getUpdatedAt() >= now - 2 * Clock.MINUTE && !"OFFLINE".equals(p.getStatus())) {
                ccu++;
            }
        }
        long since = Clock.dayOf(dayAgo);
        for (AnalyticsEvent v : events.kindSince("VISIT", since)) {
            if (v.getCreatedAt() >= dayAgo) {
                daily.add(v.getUsername());
            }
        }
        long revenue = 0;
        long count = 0;
        for (StorePurchase p : purchases.since(dayAgo)) {
            if (p.getRefunded() == 0) {
                revenue += p.getBricks();
                count++;
            }
        }
        long sessions = countSince(events.kindSince("SESSION", since), dayAgo);
        long errors = countSince(events.kindSince("ERROR", since), dayAgo);
        return new KpiDto(daily.size(), ccu, revenue, count, sessions, errors,
                sessions == 0 ? 0 : errors * 1000 / sessions);
    }

    private static long countSince(List<AnalyticsEvent> list, long from) {
        long n = 0;
        for (AnalyticsEvent e : list) {
            if (e.getCreatedAt() >= from) {
                n++;
            }
        }
        return n;
    }

    public List<FraudFlagDto> fraudFlags() {
        Staff.require(Staff.VIEW);
        List<FraudFlagDto> out = new ArrayList<FraudFlagDto>();
        for (FraudFlag f : fraud.since(Clock.now() - 30 * Clock.DAY)) {
            out.add(new FraudFlagDto(f.getId(), f.getUsername(), f.getKind(), f.getDetail(), f.getCreatedAt()));
        }
        return out;
    }

    static ReportDto toDto(Report r) {
        return new ReportDto(r.getId(), r.getReporter(), r.getTargetType(), r.getTargetId(), r.getReason(),
                r.getStatus());
    }

    static SanctionDto toDto(Sanction s) {
        return new SanctionDto(s.getId(), s.getUsername(), s.getKind(), s.getReason(), s.getUntilAt(), s.getLifted());
    }

    static AppealDto toDto(Appeal a) {
        return new AppealDto(a.getId(), a.getSanctionId(), a.getBody(), a.getStatus());
    }

    static FeaturedDto toDto(Featured f) {
        return new FeaturedDto(f.getId(), f.getRowName(), f.getExperienceId(), f.getOrdinal());
    }
}
