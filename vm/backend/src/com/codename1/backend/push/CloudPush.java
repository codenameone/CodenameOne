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
package com.codename1.backend.push;

import com.codename1.backend.Json;
import com.codename1.backend.Web;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Sends push notifications through the Codename One push service.
///
/// ```java
/// CloudPush push = new CloudPush(appId, serverApiKey);
/// PushResult result = push.send(message, PushTarget.fromDeviceKey(deviceKey));
/// if (!result.isAccepted()) {
///     log("push refused: " + result.getError());
/// }
/// ```
///
/// `appId` is the application UUID the Console shows for the push application,
/// and `serverApiKey` a server API key created with the `push` scope. The key is
/// a credential: keep it in configuration, not in source, and never ship it in
/// the app.
///
/// A send is one HTTPS request, made with [Web] on the calling thread. Every
/// answer the service gives comes back as a [PushResult], refusals included;
/// only a request that got no answer at all throws. Nothing here logs, so the
/// key and the device tokens reach no log through this class.
///
/// Instances are immutable and can be shared between threads.
public final class CloudPush {
    /// The production endpoint of the push service.
    public static final String DEFAULT_ENDPOINT =
            "https://cloud.codenameone.com/api/v3/push/messages";

    private final String appId;
    private final String serverApiKey;
    private final String endpoint;

    /// A sender for one push application.
    ///
    /// @param appId the application UUID from the Console's push overview
    /// @param serverApiKey a server API key with the `push` scope
    /// @throws IllegalArgumentException if either is null or blank
    public CloudPush(String appId, String serverApiKey) {
        this(appId, serverApiKey, DEFAULT_ENDPOINT);
    }

    private CloudPush(String appId, String serverApiKey, String endpoint) {
        PushNotification.requireText(appId, "The push application id");
        PushNotification.requireText(serverApiKey, "The server API key");
        this.appId = appId;
        this.serverApiKey = serverApiKey;
        this.endpoint = endpoint;
    }

    /// A copy that sends to another URL: a stub in a test, or a gateway that
    /// forwards to the service.
    ///
    /// @param url the full URL of the messages endpoint
    /// @return a sender with the same application and key
    /// @throws IllegalArgumentException if the URL is null or blank
    public CloudPush withEndpoint(String url) {
        PushNotification.requireText(url, "The push endpoint");
        return new CloudPush(appId, serverApiKey, url);
    }

    /// @return the URL messages are sent to
    public String getEndpoint() {
        return endpoint;
    }

    /// @return the application UUID messages are sent for
    public String getAppId() {
        return appId;
    }

    /// Sends one message to one or more devices.
    ///
    /// @param message the message
    /// @param targets the devices
    /// @return the service's answer, accepted or not
    /// @throws IllegalArgumentException if the message is null or has no body,
    /// data or surface command, or if there are no targets; checked before
    /// any request is made
    /// @throws IOException if the request failed without an answer: the service
    /// couldn't be reached, the connection broke, or it timed out
    public PushResult send(PushNotification message, PushTarget... targets) throws IOException {
        if (targets == null) {
            throw new IllegalArgumentException("A push needs at least one target");
        }
        return send(message, Arrays.asList(targets));
    }

    /// Sends one message to one or more devices.
    ///
    /// @param message the message
    /// @param targets the devices, each a [PushTarget]
    /// @return the service's answer, accepted or not
    /// @throws IllegalArgumentException if the message is null or has no body,
    /// data or surface command, or if the list is empty or holds something
    /// other than a [PushTarget]; checked before any request is made
    /// @throws IOException if the request failed without an answer: the service
    /// couldn't be reached, the connection broke, or it timed out
    public PushResult send(PushNotification message, List<PushTarget> targets) throws IOException {
        if (message == null) {
            throw new IllegalArgumentException("No push message");
        }
        if (!message.hasContent()) {
            throw new IllegalArgumentException("A push message needs a body, data or a "
                    + "surface command");
        }
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("A push needs at least one target");
        }
        List<Object> targetList = new ArrayList<Object>();
        // Read as Object and tested: a raw List can hold anything, and on the
        // native runtime a cast that fails is not an exception, it is a crash.
        for (Object entry : (List<?>) targets) {
            if (!(entry instanceof PushTarget)) {
                throw new IllegalArgumentException("Push targets must be PushTarget "
                        + "instances; found " + (entry == null ? "null"
                        : entry.getClass().getName()));
            }
            PushTarget target = (PushTarget) entry;
            Map<String, Object> json = new LinkedHashMap<String, Object>();
            json.put("provider", target.getProvider());
            json.put("token", target.getToken());
            targetList.add(json);
        }
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("appId", appId);
        request.put("targets", targetList);
        request.put("message", message.envelope());

        List<String> headers = new ArrayList<String>();
        headers.add("Authorization: Bearer " + serverApiKey);
        headers.add("Content-Type: application/json");
        headers.add("Accept: application/json");
        Web.Result response = Web.request("POST", endpoint, headers,
                Json.write(request).getBytes("UTF-8"));
        int status = response.getStatus();
        if (status < 0) {
            // Both arms of Web throw for this already; the check keeps the
            // contract here rather than in a comment about another class.
            throw new IOException("The push service could not be reached");
        }
        return toResult(status, response.getBodyAsString(), response.getHeader("Retry-After"));
    }

    static PushResult toResult(int status, String body, String retryAfter) {
        Map<?, ?> json = parseObject(body);
        List<String> deliveryIds = new ArrayList<String>();
        String error = null;
        boolean accepted = status >= 200 && status < 300;
        if (json != null) {
            Object ids = json.get("deliveryIds");
            if (ids instanceof List) {
                for (Object id : (List<?>) ids) {
                    if (id != null) {
                        deliveryIds.add(String.valueOf(id));
                    }
                }
            }
            if (!accepted) {
                error = errorText(json);
            }
        }
        if (!accepted && error == null) {
            error = "HTTP " + status;
        }
        return new PushResult(status, deliveryIds, error, parseRetryAfter(retryAfter));
    }

    private static Map<?, ?> parseObject(String body) {
        if (body == null || body.length() == 0) {
            return null;
        }
        try {
            Object value = Json.parse(body);
            return value instanceof Map ? (Map<?, ?>) value : null;
        } catch (IOException notJson) {
            // A proxy's HTML error page, say. The status still says what
            // happened; the body just has nothing to add.
            return null;
        }
    }

    /// `error` is the service's code and `message` its explanation; either can
    /// be missing.
    private static String errorText(Map<?, ?> json) {
        Object code = json.get("error");
        Object message = json.get("message");
        String codeText = code instanceof String && ((String) code).length() > 0
                ? (String) code : null;
        String messageText = message instanceof String && ((String) message).length() > 0
                ? (String) message : null;
        if (codeText != null && messageText != null && !codeText.equals(messageText)) {
            return codeText + ": " + messageText;
        }
        return messageText != null ? messageText : codeText;
    }

    /// Delta seconds only. The HTTP-date form is legal but the service sends
    /// seconds, and reading a date would need a clock the caller's retry logic
    /// is better placed to own; it answers -1 like an absent header.
    static int parseRetryAfter(String value) {
        if (value == null) {
            return -1;
        }
        String text = value.trim();
        if (text.length() == 0 || text.length() > 9) {
            return -1;
        }
        int seconds = 0;
        for (int iter = 0 ; iter < text.length() ; iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            seconds = seconds * 10 + (c - '0');
        }
        return seconds;
    }

    /// The application only. The key is a credential, and a gateway URL can carry
    /// one in its query string, so neither goes anywhere a log might.
    @Override
    public String toString() {
        return "CloudPush[appId=" + appId + "]";
    }
}
