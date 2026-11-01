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
package com.codenameone.examples.skirmish.social;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.Ids;
import com.codenameone.examples.skirmish.api.BroadcastDto;
import com.codenameone.examples.skirmish.api.InviterDto;
import com.codenameone.examples.skirmish.api.PromoteDto;
import com.codenameone.examples.skirmish.api.SpotlightDto;
import com.codenameone.examples.skirmish.domain.AdminAction;
import com.codenameone.examples.skirmish.domain.Broadcast;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.domain.Referral;
import com.codenameone.examples.skirmish.domain.Spotlight;
import com.codenameone.examples.skirmish.push.GamePushes;
import com.codenameone.examples.skirmish.repository.AdminActionRepository;
import com.codenameone.examples.skirmish.repository.BroadcastRepository;
import com.codenameone.examples.skirmish.repository.ProfileRepository;
import com.codenameone.examples.skirmish.repository.ReferralRepository;
import com.codenameone.examples.skirmish.repository.SpotlightRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The operator's side of growing the game: who brought whom in, pushes to a
/// segment of players, and the banner on every home screen.
///
/// Engagement itself -- daily players, retention, the invite funnel, revenue
/// per campaign -- is measured by Codename One analytics, from the events the
/// app reports, and read in the Codename One console. This class keeps only
/// what the game needs to act on: the invites it paid for, the pushes it sent
/// and the banner. Pushes go out through Codename One push ([GamePushes]),
/// with the campaign in their data so an open is attributed in analytics.
@Component
public class Promotion {
    public static final String ALL = "all";
    public static final String LAPSED = "lapsed";
    public static final String NEW = "new";
    public static final String INVITED = "invited";
    /// What a broadcast or the banner may open in the app.
    public static final String[] SCREENS = {"", "shop", "forge", "friends", "deck", "battle"};
    public static final int MAX_BROADCASTS_PER_DAY = 5;
    public static final int MAX_AUDIENCE = 20000;
    private static final String HOME = "home";
    private static final int PAGE = 500;
    private static final int TOP_INVITERS = 5;
    private static final int RECENT = 10;

    private final ProfileRepository profiles;
    private final ReferralRepository referrals;
    private final BroadcastRepository broadcasts;
    private final SpotlightRepository spotlights;
    private final AdminActionRepository audit;
    private final GamePushes pushes;

    public Promotion(ProfileRepository profiles, ReferralRepository referrals, BroadcastRepository broadcasts,
            SpotlightRepository spotlights, AdminActionRepository audit, GamePushes pushes) {
        this.profiles = profiles;
        this.referrals = referrals;
        this.broadcasts = broadcasts;
        this.spotlights = spotlights;
        this.audit = audit;
        this.pushes = pushes;
    }

    @Transactional(readOnly = true)
    public PromoteDto overview() throws IOException {
        long now = Clock.now();
        Map<String, int[]> by = new HashMap<>();
        for (Referral r : referrals.newest(new PageRequest(0, PAGE))) {
            int[] n = by.get(r.getInviter());
            if (n == null) {
                n = new int[2];
                by.put(r.getInviter(), n);
            }
            n[0]++;
            if (Referrals.PLAYED.equals(r.getState())) {
                n[1]++;
            }
        }
        List<InviterDto> top = new ArrayList<>();
        List<String> names = new ArrayList<>(by.keySet());
        while (top.size() < TOP_INVITERS && !names.isEmpty()) {
            String best = names.get(0);
            for (String n : names) {
                int[] a = by.get(n);
                int[] b = by.get(best);
                if (a[0] > b[0] || a[0] == b[0] && n.compareTo(best) < 0) {
                    best = n;
                }
            }
            names.remove(best);
            Profile p = profiles.findById(best);
            top.add(new InviterDto(p == null ? best : p.getDisplayName(), by.get(best)[0], by.get(best)[1]));
        }
        List<BroadcastDto> sent = new ArrayList<>();
        for (Broadcast b : broadcasts.newest(new PageRequest(0, RECENT))) {
            sent.add(dto(b));
        }
        return new PromoteDto((int) profiles.count(),
                profiles.audienceSize(Instant.ofEpochMilli(Long.MAX_VALUE), Instant.ofEpochMilli(now - 7 * Clock.DAY)).intValue(),
                (int) referrals.count(), referrals.inState(Referrals.PLAYED).intValue(), top, sent, spotlightOf());
    }

    /// The broadcast as it would go: checked, and with the number of players
    /// it would reach in `targets`.
    @Transactional(readOnly = true)
    public BroadcastDto audience(BroadcastDto b) throws IOException {
        check(b, false);
        return with(b, audienceOf(b).size(), "", 0);
    }

    // tag::skirmish-promotion[]
    @Transactional(rollbackFor = IOException.class)
    public BroadcastDto broadcast(String admin, BroadcastDto b) throws IOException {
        check(b, true);
        long now = Clock.now();
        if (broadcasts.countBySentAtGreaterThan(Instant.ofEpochMilli(now - Clock.DAY)) >= MAX_BROADCASTS_PER_DAY) {
            throw new ResponseStatusException(429, "At most " + MAX_BROADCASTS_PER_DAY
                    + " broadcasts a day: players who are pushed too often switch notifications off");
        }
        String id = Ids.next();
        String campaign = b.campaign() == null || b.campaign().trim().length() == 0
                ? "broadcast-" + id.substring(0, 8) : b.campaign().trim();
        int reached = pushes.broadcast(audienceOf(b), id, b.title().trim(), b.body().trim(), screen(b.screen()),
                campaign);
        Broadcast row = new Broadcast();
        row.setId(id);
        row.setSegment(b.segment());
        row.setDays(b.days());
        row.setTitle(b.title().trim());
        row.setBody(b.body().trim());
        row.setScreen(screen(b.screen()));
        row.setCampaign(campaign.length() > 40 ? campaign.substring(0, 40) : campaign);
        row.setTargets(reached);
        row.setSentBy(admin);
        row.setSentAt(Instant.ofEpochMilli(now));
        broadcasts.insert(row);
        record(admin, "BROADCAST", b.segment() + ": " + b.title().trim());
        return dto(row);
    }
    // end::skirmish-promotion[]

    @Transactional(readOnly = true)
    public SpotlightDto spotlight() throws IOException {
        return spotlightOf();
    }

    @Transactional(rollbackFor = IOException.class)
    public SpotlightDto spotlight(String admin, SpotlightDto s) throws IOException {
        String text = s == null || s.text() == null ? "" : s.text().trim();
        if (text.length() > 140) {
            throw new ResponseStatusException(400, "The banner is at most 140 characters");
        }
        Spotlight row = spotlights.findById(HOME);
        boolean fresh = row == null;
        if (fresh) {
            row = new Spotlight();
            row.setId(HOME);
        }
        row.setMessage(text);
        row.setPackId(s.packId() == null ? "" : s.packId().trim());
        row.setScreen(screen(s.screen()));
        row.setUpdatedBy(admin);
        row.setUpdatedAt(Instant.now());
        if (fresh) {
            spotlights.insert(row);
        } else {
            spotlights.save(row);
        }
        record(admin, "SPOTLIGHT", text.length() == 0 ? "(cleared)" : text);
        return spotlightOf();
    }

    private SpotlightDto spotlightOf() {
        Spotlight s = spotlights.findById(HOME);
        return s == null ? new SpotlightDto("", "", "", 0)
                : new SpotlightDto(s.getMessage(), s.getPackId(), s.getScreen(), s.getUpdatedAt().toEpochMilli());
    }

    private List<String> audienceOf(BroadcastDto b) {
        List<String> out = new ArrayList<>();
        if (INVITED.equals(b.segment())) {
            for (int page = 0; out.size() < MAX_AUDIENCE; page++) {
                List<Referral> rows = referrals.newest(new PageRequest(page, PAGE));
                for (Referral r : rows) {
                    out.add(r.getInvitee());
                }
                if (rows.size() < PAGE) {
                    break;
                }
            }
            return out;
        }
        long now = Clock.now();
        Instant seen = Instant.ofEpochMilli(LAPSED.equals(b.segment()) ? now - b.days() * Clock.DAY : Long.MAX_VALUE);
        Instant joined = Instant.ofEpochMilli(NEW.equals(b.segment()) ? now - b.days() * Clock.DAY : Long.MIN_VALUE);
        for (int page = 0; out.size() < MAX_AUDIENCE; page++) {
            List<Profile> rows = profiles.audience(seen, joined, new PageRequest(page, PAGE));
            for (Profile p : rows) {
                out.add(p.getUsername());
            }
            if (rows.size() < PAGE) {
                break;
            }
        }
        return out;
    }

    private static void check(BroadcastDto b, boolean sending) {
        if (b == null || b.segment() == null) {
            throw new ResponseStatusException(400, "Choose who to send to");
        }
        String s = b.segment();
        if (!ALL.equals(s) && !LAPSED.equals(s) && !NEW.equals(s) && !INVITED.equals(s)) {
            throw new ResponseStatusException(400, "Unknown segment " + s);
        }
        if ((LAPSED.equals(s) || NEW.equals(s)) && (b.days() < 1 || b.days() > 365)) {
            throw new ResponseStatusException(400, "Days must be between 1 and 365");
        }
        if (sending) {
            int title = b.title() == null ? 0 : b.title().trim().length();
            int body = b.body() == null ? 0 : b.body().trim().length();
            if (title == 0 || title > 60 || body == 0 || body > 200) {
                throw new ResponseStatusException(400, "A title of up to 60 characters and a message of up to 200");
            }
        }
        screen(b.screen());
    }

    private static String screen(String s) {
        String v = s == null ? "" : s;
        for (String ok : SCREENS) {
            if (ok.equals(v)) {
                return v;
            }
        }
        throw new ResponseStatusException(400, "Unknown screen " + v);
    }

    private static BroadcastDto with(BroadcastDto b, int targets, String by, long at) {
        return new BroadcastDto(b.id(), b.segment(), b.days(), b.title(), b.body(), b.screen(), b.campaign(),
                targets, by, at);
    }

    private static BroadcastDto dto(Broadcast b) {
        return new BroadcastDto(b.getId(), b.getSegment(), b.getDays(), b.getTitle(), b.getBody(), b.getScreen(),
                b.getCampaign(), b.getTargets(), b.getSentBy(), b.getSentAt().toEpochMilli());
    }

    private void record(String admin, String action, String reason) throws IOException {
        AdminAction a = new AdminAction();
        a.setId(Ids.next());
        a.setAdmin(admin);
        a.setUsername("promote");
        a.setAction(action);
        a.setReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
        a.setCreatedAt(Instant.now());
        audit.insert(a);
    }
}
