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
package com.codenameone.examples.parley.admin;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.parley.Days;
import com.codenameone.examples.parley.Rows;
import com.codenameone.examples.parley.account.Accounts;
import com.codenameone.examples.parley.account.Moderation;
import com.codenameone.examples.parley.api.AdminStatsDto;
import com.codenameone.examples.parley.api.DayStatsDto;
import com.codenameone.examples.parley.api.NameCountDto;
import com.codenameone.examples.parley.api.StatsSeriesDto;
import com.codenameone.examples.parley.api.UserDetailDto;
import com.codenameone.examples.parley.chat.Conversations;
import com.codenameone.examples.parley.repository.BridgeLinkRepository;
import com.codenameone.examples.parley.repository.CallRecordRepository;
import com.codenameone.examples.parley.repository.ConversationRepository;
import com.codenameone.examples.parley.repository.DeviceRepository;
import com.codenameone.examples.parley.repository.MembershipRepository;
import com.codenameone.examples.parley.repository.MessageLogRepository;
import com.codenameone.examples.parley.repository.ProfileRepository;
import com.codenameone.examples.parley.repository.ReportRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// The figures on the admin's dashboard.
///
/// Every one of them is a count. The server cannot read a message, and the
/// dashboard does not pretend otherwise: it knows that a message was sent, by
/// whom and when, because routing it needed that much, and nothing about
/// what it said. Receipts, typing and key traffic are not counted as
/// messages.
@Component
public class Statistics {
    static final int DEFAULT_DAYS = 14;
    static final int MAX_DAYS = 90;
    static final String OPEN = "OPEN";

    private final ProfileRepository profiles;
    private final DeviceRepository devices;
    private final ConversationRepository conversations;
    private final MembershipRepository members;
    private final MessageLogRepository messages;
    private final CallRecordRepository calls;
    private final ReportRepository reports;
    private final BridgeLinkRepository bridges;
    private final Accounts accounts;
    private final Moderation moderation;

    public Statistics(ProfileRepository profiles, DeviceRepository devices,
            ConversationRepository conversations, MembershipRepository members,
            MessageLogRepository messages, CallRecordRepository calls, ReportRepository reports,
            BridgeLinkRepository bridges, Accounts accounts, Moderation moderation) {
        this.profiles = profiles;
        this.devices = devices;
        this.conversations = conversations;
        this.members = members;
        this.messages = messages;
        this.calls = calls;
        this.reports = reports;
        this.bridges = bridges;
        this.accounts = accounts;
        this.moderation = moderation;
    }

    /// The handful of totals at the top of the admin's dashboard.
    @Transactional(readOnly = true)
    public AdminStatsDto dashboard() throws IOException {
        long today = Days.start(System.currentTimeMillis());
        return new AdminStatsDto((int) profiles.count(), (int) devices.count(),
                (int) conversations.count(),
                (int) (conversations.countByKind(Conversations.GROUP)
                        + conversations.countByKind(Conversations.CHANNEL)),
                (int) messages.countBySentAtGreaterThanEqual(Instant.ofEpochMilli(today)),
                (int) calls.countByStartedAtGreaterThanEqual(Instant.ofEpochMilli(today)),
                (int) reports.countByStatus(OPEN), (int) bridges.count());
    }

    /// The last `days` days, today included and last. Fewer than one is taken
    /// as the default and more than [#MAX_DAYS] as that many.
    @Transactional(readOnly = true)
    public StatsSeriesDto series(int days) throws IOException {
        int span = days < 1 ? DEFAULT_DAYS : Math.min(days, MAX_DAYS);
        long today = Days.start(System.currentTimeMillis());
        long since = today - (span - 1) * Days.MILLIS;
        int[] dayMessages = new int[span];
        int[] dayCalls = new int[span];
        int[] dayUsers = new int[span];
        List<Set<String>> active = new ArrayList<>();
        for (int iter = 0; iter < span; iter++) {
            active.add(new HashSet<>());
        }
        int[] byHour = new int[24];
        for (Object[] row : messages.since(Instant.ofEpochMilli(since))) {
            long at = Rows.millis(row[0]);
            byHour[(int) (at % Days.MILLIS / 3600000L)]++;
            int index = index(at, since, span);
            if (index >= 0) {
                dayMessages[index]++;
                active.get(index).add(Rows.text(row[1]));
            }
        }
        for (Instant at : calls.startedSince(Instant.ofEpochMilli(since))) {
            int index = index(Rows.millis(at), since, span);
            if (index >= 0) {
                dayCalls[index]++;
            }
        }
        for (Instant at : profiles.signUps(Instant.ofEpochMilli(since))) {
            int index = index(Rows.millis(at), since, span);
            if (index >= 0) {
                dayUsers[index]++;
            }
        }
        List<DayStatsDto> out = new ArrayList<>();
        for (int iter = 0; iter < span; iter++) {
            out.add(new DayStatsDto(Days.iso(since + iter * Days.MILLIS), dayMessages[iter],
                    dayCalls[iter], dayUsers[iter], active.get(iter).size()));
        }
        List<Integer> hours = new ArrayList<>();
        for (int count : byHour) {
            hours.add(count);
        }
        return new StatsSeriesDto(out, hours, named(conversations.byKind()),
                named(devices.byPlatform()), (int) profiles.countByFlaggedTrue(),
                (int) reports.countByStatus(OPEN));
    }

    /// One account as the admins see it: its counts and its moderation
    /// history, never its conversations' titles or members.
    @Transactional(readOnly = true)
    public UserDetailDto user(String username) throws IOException {
        return new UserDetailDto(accounts.describe(username),
                (int) devices.countByUsername(username), (int) members.countByUsername(username),
                (int) messages.countBySender(username), (int) reports.countBySubject(username),
                moderation.events(username));
    }

    private static int index(long at, long since, int span) {
        if (at < since) {
            return -1;
        }
        int index = (int) ((at - since) / Days.MILLIS);
        return index < span ? index : -1;
    }

    private static List<NameCountDto> named(List<Object[]> rows) {
        List<NameCountDto> out = new ArrayList<>();
        for (Object[] row : rows) {
            out.add(new NameCountDto(Rows.text(row[0]), (int) Rows.number(row[1])));
        }
        return out;
    }
}
