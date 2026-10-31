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
package com.codenameone.examples.pulse;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.pulse.account.DemoAccounts;
import com.codenameone.examples.pulse.api.InsightsDto;
import com.codenameone.examples.pulse.api.MemberDetailDto;
import com.codenameone.examples.pulse.api.OwnerMemberDto;
import com.codenameone.examples.pulse.api.TrainerStatsDto;
import com.codenameone.examples.pulse.api.TypeFillDto;
import com.codenameone.examples.pulse.owner.Insights;
import com.codenameone.examples.pulse.trainer.Trainers;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The owner's insights and member list, and a trainer's own numbers, over
/// the demo studio the tests start with.
@BackendTest
class InsightsTest {
    @Autowired
    private Insights insights;
    @Autowired
    private Trainers trainers;

    @Test
    void insightsHaveTheShapeTheAppDraws() throws Exception {
        InsightsDto dto = insights.insights();
        assertTrue(dto.firstHour() < dto.lastHour());
        assertEquals(7 * (dto.lastHour() - dto.firstHour()), dto.busyHours().size());
        assertEquals(12, dto.weeks().size());
        int visits = 0;
        for (int iter = 0; iter < dto.busyHours().size(); iter++) {
            assertTrue(dto.busyHours().get(iter).intValue() >= 0);
            visits += dto.busyHours().get(iter).intValue();
        }
        assertTrue(visits > 0, "the demo studio has visits");
        for (int iter = 1; iter < dto.weeks().size(); iter++) {
            assertTrue(dto.weeks().get(iter).weekOf().compareTo(
                    dto.weeks().get(iter - 1).weekOf()) > 0, "oldest first");
        }
        assertFalse(dto.classes().isEmpty());
        for (int iter = 0; iter < dto.classes().size(); iter++) {
            TypeFillDto type = dto.classes().get(iter);
            assertPercent(type.fillPercent());
            assertPercent(type.noShowPercent());
        }
        assertFalse(dto.plans().isEmpty());
        assertFalse(dto.coaches().isEmpty());
        for (int iter = 0; iter < dto.coaches().size(); iter++) {
            assertPercent(dto.coaches().get(iter).fillPercent());
            assertPercent(dto.coaches().get(iter).showPercent());
        }
        assertPercent(dto.retention90());
        assertPercent(dto.noShowPercent());
        assertTrue(dto.revenuePerMemberCents() > 0);
    }

    @Test
    void theFiltersFindThePeopleTheyName() throws Exception {
        assertTrue(has(insights.members("PAST_DUE"), DemoAccounts.FLAGGED_MEMBER));
        assertFalse(has(insights.members("PAST_DUE"), DemoAccounts.MEMBER));
        assertTrue(has(insights.members("AT_RISK"), DemoAccounts.LAPSED_MEMBER));
        List<OwnerMemberDto> running = insights.members("");
        assertTrue(has(running, DemoAccounts.MEMBER));
        assertTrue(has(running, DemoAccounts.FLAGGED_MEMBER));
        for (int iter = 1; iter < running.size(); iter++) {
            assertTrue(running.get(iter - 1).name().compareToIgnoreCase(
                    running.get(iter).name()) <= 0, "by name");
        }
        List<OwnerMemberDto> cancelled = insights.members("CANCELLED");
        for (int iter = 0; iter < cancelled.size(); iter++) {
            assertEquals("CANCELLED", cancelled.get(iter).state());
        }
    }

    @Test
    void aMembersDetailTellsTheirStory() throws Exception {
        OwnerMemberDto riley = null;
        List<OwnerMemberDto> running = insights.members("");
        for (int iter = 0; iter < running.size(); iter++) {
            if (DemoAccounts.MEMBER.equals(running.get(iter).account())
                    && "Riley Park".equals(running.get(iter).name())) {
                riley = running.get(iter);
            }
        }
        assertNotNull(riley);
        MemberDetailDto detail = insights.member(riley.id());
        assertEquals(riley.id(), detail.member().id());
        assertEquals(12, detail.visitsByWeek().size());
        assertFalse(detail.invoices().isEmpty());
        assertTrue(detail.lifetimeCents() > 0);
        for (int iter = 1; iter < detail.favourites().size(); iter++) {
            assertTrue(detail.favourites().get(iter - 1).count()
                    >= detail.favourites().get(iter).count());
        }
        ResponseStatusException missing = assertThrows(ResponseStatusException.class,
                () -> insights.member("no-such-membership"));
        assertEquals(404, missing.getStatus());
    }

    @Test
    void aTrainerSeesTheirOwnMonth() throws Exception {
        TrainerStatsDto tess = trainers.stats(DemoAccounts.TRAINER);
        assertTrue(tess.classes() > 0, "Tess has taught");
        assertEquals(8, tess.attendeesByWeek().size());
        assertPercent(tess.fillPercent());
        assertPercent(tess.showPercent());
        TrainerStatsDto nobody = trainers.stats("nobody@pulse.example");
        assertEquals(0, nobody.classes());
        assertEquals(8, nobody.attendeesByWeek().size());
    }

    private static boolean has(List<OwnerMemberDto> list, String account) {
        for (int iter = 0; iter < list.size(); iter++) {
            if (account.equals(list.get(iter).account())) {
                return true;
            }
        }
        return false;
    }

    private static void assertPercent(int value) {
        assertTrue(value >= 0 && value <= 100, value + " is not a percent");
    }
}
