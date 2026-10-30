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
package com.codenameone.examples.pulse.studio;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.live.LiveHub;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.trainer.HeartRates;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/// Simulated straps for the dev profile: every five seconds, each member
/// checked in to a class that is running now gets a heart rate, so the
/// trainer's live screen moves without anyone wearing anything. Readings
/// drift smoothly around a target that differs per member, which spreads the
/// room across zones 2 to 5. Nothing happens outside the dev profile.
@Component
public class DemoHeartRates {
    private final Config config;
    private final ClassSessionRepository sessions;
    private final BookingRepository bookings;
    private final MemberRepository members;
    private final HeartRates rates;
    private final LiveHub live;

    public DemoHeartRates(Config config, ClassSessionRepository sessions,
            BookingRepository bookings, MemberRepository members, HeartRates rates,
            LiveHub live) {
        this.config = config;
        this.sessions = sessions;
        this.bookings = bookings;
        this.members = members;
        this.rates = rates;
        this.live = live;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    @Transactional(readOnly = true)
    public void beat() {
        if (!config.isDevelopmentProfile()) {
            return;
        }
        long now = System.currentTimeMillis();
        Instant at = Instant.ofEpochMilli(now);
        // A class runs at most a couple of hours, so one that started in the
        // last three and has not ended is the whole set.
        List<ClassSession> started = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                at.minusMillis(3L * 3600000L), at);
        LocalDate today = LocalDate.now();
        for (int iter = 0; iter < started.size(); iter++) {
            ClassSession session = started.get(iter);
            if (!session.getEndsAt().isAfter(at) || Studio.CANCELLED.equals(session.getState())) {
                continue;
            }
            List<Booking> roster = bookings.findBySessionIdAndStateOrderByQueuePositionAsc(
                    session.getId(), Bookings.CHECKED_IN);
            for (int each = 0; each < roster.size(); each++) {
                Member member = members.findById(roster.get(each).getMemberId());
                if (member == null) {
                    continue;
                }
                int age = Accounts.age(member.getBirthDate(), today);
                int bpm = bpm(member.getId(), age, now);
                int zone = HeartRates.zone(bpm, age);
                rates.put(session.getId(), member.getId(), bpm, zone, now);
                live.heartRate(session.getTrainer(), session.getId(), member.getId(), bpm, zone);
            }
        }
    }

    /// A heart rate that wanders slowly around a share of the member's
    /// maximum: the member's id picks the share (60% to 92%) and the phase of
    /// two slow waves, so neighbours do not move in step.
    static int bpm(String memberId, int age, long now) {
        int hash = memberId.hashCode() & 0x7fffffff;
        int max = Math.max(120, 220 - age);
        double share = 0.60 + (hash % 5) * 0.08;
        double phase = (hash % 997) / 997.0 * 2 * Math.PI;
        double seconds = now / 1000.0;
        double wave = Math.sin(seconds / 40.0 + phase) * 0.05
                + Math.sin(seconds / 11.0 + phase * 3) * 0.02;
        return (int) Math.round(max * Math.min(0.97, share + wave));
    }
}
