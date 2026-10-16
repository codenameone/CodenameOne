/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.developerguide.backend;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.CloudPush;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushResult;
import com.codename1.backend.push.PushTarget;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends a notification and keeps a Live Activity current. Referenced from the
 * backend chapter's push section.
 */
@Component
public class RideNotifications {
    // tag::backend-push-send[]
    private final CloudPush push;

    /**
     * The application UUID and a push-scoped server API key, read from
     * cn1.push.appId and cn1.push.serverKey -- or from the environment
     * variables CN1_PUSH_APPID and CN1_PUSH_SERVERKEY, which override them.
     */
    public RideNotifications(@Value("${cn1.push.appId}") String appId,
                             @Value("${cn1.push.serverKey}") String serverKey) {
        this.push = new CloudPush(appId, serverKey);
    }

    /** deviceKey is what the app registered, such as "cn1-fcm-..." */
    public boolean driverArrived(String deviceKey, String tripId) throws IOException {
        PushNotification message = PushNotification.builder()
                .title("Your driver is here")
                .body("Look for the blue sedan")
                .deepLink("ride://trip/" + tripId)
                .collapseKey("trip-" + tripId)
                .data("tripId", tripId)
                .build();
        PushResult result = push.send(message, PushTarget.fromDeviceKey(deviceKey));
        if (result.getStatus() == 429) {
            // Not admitted; try again after result.getRetryAfterSeconds()
            return false;
        }
        return result.isAccepted();
    }
    // end::backend-push-send[]

    // tag::backend-push-live-activity[]
    public PushResult updateEta(PushTarget device, String tripId, int minutes)
            throws IOException {
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("minutes", Integer.valueOf(minutes));
        state.put("status", minutes == 0 ? "arrived" : "on the way");
        PushNotification message = PushNotification.builder()
                .silent(true)
                .surface(PushNotification.liveUpdate("trip-" + tripId, state))
                .build();
        return push.send(message, device);
    }

    public PushResult tripEnded(PushTarget device, String tripId) throws IOException {
        PushNotification message = PushNotification.builder()
                .silent(true)
                .surface(PushNotification.liveEnd("trip-" + tripId, null, true))
                .build();
        return push.send(message, device);
    }
    // end::backend-push-live-activity[]
}
