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
package com.codenameone.examples.pulse.engagement;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.pulse.People;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.api.AwayDto;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.domain.Preference;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import com.codenameone.examples.pulse.repository.NudgeRepository;
import com.codenameone.examples.pulse.repository.PreferenceRepository;
import com.codenameone.examples.pulse.repository.RoomRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What reaches a member and what does not: nothing marketing while they
/// are away, nothing twice, and billing frozen when they ask.
@BackendTest
class EngagementTest {
    @Autowired
    private Accounts accounts;
    @Autowired
    private MemberRepository members;
    @Autowired
    private MembershipRepository memberships;
    @Autowired
    private RoomRepository rooms;
    @Autowired
    private ClassSessionRepository sessions;
    @Autowired
    private PreferenceRepository preferences;
    @Autowired
    private NudgeRepository nudges;
    @Autowired
    private Engagement engagement;
    @Autowired
    private Nudger nudger;

    /// A member who takes marketing at any hour, so the test does not depend
    /// on the time of day it runs at.
    private String reachable(String name) throws Exception {
        String account = People.member(accounts, members, memberships, name);
        Preference row = new Preference();
        row.setUsername(account);
        row.setTheme("system");
        row.setUnits("metric");
        row.setNotifyClasses(true);
        row.setNotifyPromotions(true);
        row.setQuietFrom(0);
        row.setQuietTo(0);
        People.tx(() -> {
            preferences.insert(row);
            return null;
        });
        return account;
    }

    private static String today(int plus) {
        return LocalDate.now(ZoneOffset.UTC).plusDays(plus).toString();
    }

    @Test
    void someoneAwayIsNotAskedToFillAClass() throws Exception {
        String trainer = People.trainer(accounts, "Coach");
        ClassSession session = People.session(sessions, "type-yoga", People.room(rooms, 20),
                trainer, 20, System.currentTimeMillis() + 6L * 3600000L);
        String here = reachable("Here");
        String away = reachable("Away");
        engagement.addAway(away, new AwayDto(null, today(0), today(3), "Travel", false));
        Instant now = Instant.now();
        assertFalse(nudger.offer(away, session.getId(), now));
        assertTrue(nudger.offer(here, session.getId(), now));
        // Once per class, and not again within two days for another class.
        assertFalse(nudger.offer(here, session.getId(), now));
        assertTrue(People.tx(() -> nudges.findByAccountOrderBySentAtDesc(away)).isEmpty());
        assertEquals(1, People.tx(() -> nudges.findByAccountOrderBySentAtDesc(here)).size());
    }

    @Test
    void someoneAwayIsNotToldTheyAreMissed() throws Exception {
        String here = reachable("Missed");
        String away = reachable("Travelling");
        engagement.addAway(away, new AwayDto(null, today(0), today(20), "", false));
        Instant now = Instant.now();
        assertTrue(nudger.miss(here, now));
        assertFalse(nudger.miss(here, now), "not twice in three weeks");
        assertFalse(nudger.miss(away, now));
    }

    @Test
    void awayWithBillingFrozenFreezesTheMembership() throws Exception {
        String account = reachable("Freezer");
        AwayDto made = engagement.addAway(account, new AwayDto(null, today(0), today(13),
                "Knee surgery", true));
        List<Membership> held = People.tx(() -> memberships.findByAccountOrderByStartedAtDesc(account));
        assertEquals("FROZEN", held.get(0).getState());
        engagement.removeAway(account, made.id());
        assertEquals("ACTIVE", People.tx(() -> memberships.findByAccountOrderByStartedAtDesc(account)).get(0)
                .getState());
    }
}
