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
package com.codenameone.examples.slate.push;

import com.codename1.appkit.server.push.PushSender;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.slate.Ids;
import com.codenameone.examples.slate.domain.Preference;
import com.codenameone.examples.slate.repository.PreferenceRepository;

import java.io.IOException;
import java.util.List;

/// Everything the business tells a client or a provider, in one place.
///
/// Every message here is about a booking the person has -- a reminder, a
/// change, an offer from the waitlist -- so each is pushed whenever they allow
/// booking notifications, quiet hours or not. There is no marketing message in
/// this application to hold back.
@Component
public class Notifier {
    public static final String REMINDER = "REMINDER";
    public static final String BOOKED = "BOOKED";
    public static final String CHANGED = "CHANGED";
    public static final String CANCELLED = "CANCELLED";
    public static final String OFFER = "OFFER";
    public static final String PAYMENT = "PAYMENT";
    public static final String CALENDAR = "CALENDAR";

    private final PreferenceRepository preferences;
    private final PushDevices devices;
    private final PushSender sender;

    public Notifier(PreferenceRepository preferences, PushDevices devices, PushSender sender) {
        this.preferences = preferences;
        this.devices = devices;
        this.sender = sender;
    }

    /// Pushes a message about appointment `appointmentId` to `account`'s
    /// devices when they allow it. The push leaves after the call returns,
    /// from the sender's own thread; one that fails loses nothing the app
    /// needs, since the app reads its bookings from the API.
    @Transactional(readOnly = true)
    public void tell(String account, String kind, String appointmentId, String title,
            String body) throws IOException {
        Preference wants = preferences.findById(account);
        if (wants != null && !wants.isNotifyBookings()) {
            return;
        }
        List<PushTarget> to = devices.targets(account);
        if (to.isEmpty()) {
            return;
        }
        PushNotification.Builder message = PushNotification.builder().id(Ids.next())
                .title(cut(title, 120)).body(cut(body, 400)).data("kind", kind);
        if (appointmentId != null && appointmentId.length() > 0) {
            message.data("appointmentId", appointmentId)
                    .deepLink("slate://appointment/" + appointmentId);
        }
        sender.send(to, message.build(), null);
    }

    private static String cut(String text, int max) {
        String value = text == null ? "" : text;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
