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
package com.codenameone.examples.pulse.push;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.account.Preferences;
import com.codenameone.examples.pulse.domain.Nudge;
import com.codenameone.examples.pulse.domain.Preference;
import com.codenameone.examples.pulse.repository.NudgeRepository;
import com.codenameone.examples.pulse.repository.PreferenceRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/// Everything the studio tells a member, in one place: each message is kept
/// in the member's inbox (a [Nudge] row, which is also what campaign
/// statistics count) and pushed to their devices when they want it.
///
/// Two kinds of message, treated differently:
///
/// - **Service**: a class was cancelled, a waitlist place came through. The
///   member needs it, so it is pushed whenever they allow class
///   notifications, quiet hours or not.
/// - **Marketing**: a class needs filling, we miss you, a campaign. Pushed
///   only when they allow promotions and outside their quiet hours; kept in
///   the inbox either way, so the app shows it next time it is opened.
@Component
public class Notifier {
    /// Message kinds. The marketing ones are those [#marketing] answers for.
    public static final String CANCELLED = "CANCELLED";
    public static final String PROMOTED = "PROMOTED";
    public static final String FILL = "FILL";
    public static final String MISS_YOU = "MISS_YOU";
    public static final String CAMPAIGN = "CAMPAIGN";
    public static final String INVITE = "INVITE";

    private final NudgeRepository nudges;
    private final PreferenceRepository preferences;
    private final PushDevices devices;
    private final PushSender sender;
    private final ZoneId zone;

    public Notifier(NudgeRepository nudges, PreferenceRepository preferences,
            PushDevices devices, PushSender sender,
            @Value("${pulse.timezone:America/New_York}") String zone) {
        this.nudges = nudges;
        this.preferences = preferences;
        this.devices = devices;
        this.sender = sender;
        this.zone = ZoneId.of(zone);
    }

    public static boolean marketing(String kind) {
        return FILL.equals(kind) || MISS_YOU.equals(kind) || CAMPAIGN.equals(kind);
    }

    /// Whether a marketing message may be pushed to `account` now. False also
    /// when the member turned promotions off.
    @Transactional(readOnly = true)
    public boolean reachable(String account, Instant now) {
        Preference row = preferences.findById(account);
        if (row == null) {
            return !Preferences.quiet(Preferences.QUIET_FROM, Preferences.QUIET_TO,
                    now.atZone(zone).getHour());
        }
        return row.isNotifyPromotions()
                && !Preferences.quiet(row.getQuietFrom(), row.getQuietTo(), now.atZone(zone).getHour());
    }

    /// Records a message for `account` and pushes it when [#marketing] and
    /// the member's preferences allow. Must be called inside a transaction;
    /// the push itself leaves after the call returns, from the sender's own
    /// thread, and a push that fails loses nothing -- the inbox has it.
    public Nudge tell(String account, String kind, String campaignId, String sessionId,
            String title, String body, Instant now) throws IOException {
        Nudge row = new Nudge();
        row.setId(Ids.next());
        row.setAccount(account);
        row.setKind(kind);
        row.setCampaignId(campaignId == null ? "" : campaignId);
        row.setSessionId(sessionId == null ? "" : sessionId);
        row.setTitle(cut(title, 120));
        row.setBody(cut(body, 400));
        row.setSentAt(now);
        nudges.insert(row);
        Preference wants = preferences.findById(account);
        boolean push = marketing(kind) ? reachable(account, now)
                : wants == null || wants.isNotifyClasses();
        if (push) {
            List<PushTarget> to = devices.targets(account);
            if (!to.isEmpty()) {
                PushNotification.Builder message = PushNotification.builder().id(row.getId())
                        .title(row.getTitle()).body(row.getBody()).data("kind", kind);
                if (row.getSessionId().length() > 0) {
                    message.data("sessionId", row.getSessionId())
                            .deepLink("pulse://class/" + row.getSessionId());
                }
                sender.send(to, message.build(), null);
            }
        }
        return row;
    }

    private static String cut(String text, int max) {
        String value = text == null ? "" : text;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
