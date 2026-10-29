/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.mixpanel;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import dart.async.Future;
import dart.core.DartMap;

/**
 * mixpanel_flutter's {@code Mixpanel}, over Mixpanel's HTTP ingestion API: events are
 * queued and POSTed as one JSON array to {@code /track}, profile updates to
 * {@code /engage}, on {@code flush()} or once the queue reaches the flush batch size
 * ({@code setServerURL} picks the region, e.g. {@code https://api-eu.mixpanel.com}). The
 * distinct id is kept in Preferences per token. Nothing is sent without a display: the
 * queue simply waits. {@code trackAutomaticEvents} is accepted and not applied, as are
 * {@code config}, logging, groups and IP geolocation: only the app's own events are sent.
 */
public class Mixpanel {
    private static final Random RANDOM = new Random();
    private final String token;
    private String serverUrl = "https://api.mixpanel.com";
    private String distinctId;
    private boolean optedOut;
    private long flushBatchSize = 50;
    private final Map<String, Object> superProperties = new LinkedHashMap<String, Object>();
    private final Map<String, Long> timedEvents = new LinkedHashMap<String, Long>();
    private final List<Object> events = new ArrayList<Object>();
    private final List<Object> profileUpdates = new ArrayList<Object>();
    private boolean flushing;

    Mixpanel(String token, boolean optedOut) {
        this.token = token;
        this.optedOut = optedOut;
        String stored = Display.isInitialized() ? Preferences.get(key("distinct_id"), (String) null) : null;
        distinctId = stored != null ? stored : randomId();
        if (stored == null && Display.isInitialized()) {
            Preferences.set(key("distinct_id"), distinctId);
        }
    }

    public static Future<Mixpanel> init(String token, Boolean optOutTrackingDefault, Boolean trackAutomaticEvents,
            Map<?, ?> superProperties, Map<?, ?> config) {
        Mixpanel m = new Mixpanel(token, Boolean.TRUE.equals(optOutTrackingDefault));
        if (superProperties != null) {
            m.registerSuperProperties(superProperties);
        }
        return Future.value(m);
    }

    private String key(String what) {
        return "mixpanel." + token + "." + what;
    }

    private static String randomId() {
        return "$device:" + Long.toHexString(RANDOM.nextLong() & Long.MAX_VALUE)
                + Long.toHexString(System.currentTimeMillis());
    }

    static String isoNow() {
        return dart.core.DateTime.now().toUtc().toIso8601String();
    }

    /** The events and profile updates waiting for the next flush; not part of the plugin's API. */
    public long cn1PendingCount() {
        return events.size() + profileUpdates.size();
    }

    public void setServerURL(String url) {
        if (url != null) {
            serverUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        }
    }

    public void setLoggingEnabled(boolean enabled) {
    }

    public void setUseIpAddressForGeolocation(boolean use) {
    }

    public void setFlushBatchSize(long size) {
        flushBatchSize = Math.max(1, size);
    }

    public Future<Boolean> hasOptedOutTracking() {
        return Future.value(Boolean.valueOf(optedOut));
    }

    public void optInTracking() {
        optedOut = false;
    }

    public void optOutTracking() {
        optedOut = true;
        events.clear();
        profileUpdates.clear();
    }

    public Future<Object> identify(String id) {
        if (id != null && id.length() > 0 && !id.equals(distinctId)) {
            Map<String, Object> props = new LinkedHashMap<String, Object>();
            props.put("$anon_distinct_id", distinctId);
            String previous = distinctId;
            distinctId = id;
            if (Display.isInitialized()) {
                Preferences.set(key("distinct_id"), id);
            }
            if (previous.startsWith("$device:")) {
                queueEvent("$identify", props);
            }
        }
        return Future.value(null);
    }

    public void alias(String alias, String original) {
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("alias", alias);
        props.put("distinct_id", original);
        queueEvent("$create_alias", props);
    }

    public Future<String> getDistinctId() {
        return Future.value(distinctId);
    }

    public Future<Object> track(String eventName, Map<?, ?> properties) {
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        Long started = timedEvents.remove(eventName);
        if (started != null) {
            props.put("$duration", Double.valueOf((System.currentTimeMillis() - started.longValue()) / 1000.0));
        }
        if (properties != null) {
            for (Map.Entry<?, ?> e : properties.entrySet()) {
                props.put(String.valueOf(e.getKey()), e.getValue());
            }
        }
        queueEvent(eventName, props);
        return Future.value(null);
    }

    public void timeEvent(String eventName) {
        timedEvents.put(eventName, Long.valueOf(System.currentTimeMillis()));
    }

    public Future<Double> eventElapsedTime(String eventName) {
        Long started = timedEvents.get(eventName);
        return Future.value(Double.valueOf(started == null ? 0
                : (System.currentTimeMillis() - started.longValue()) / 1000.0));
    }

    public void registerSuperProperties(Map<?, ?> properties) {
        if (properties != null) {
            for (Map.Entry<?, ?> e : properties.entrySet()) {
                superProperties.put(String.valueOf(e.getKey()), e.getValue());
            }
        }
    }

    public void registerSuperPropertiesOnce(Map<?, ?> properties) {
        if (properties != null) {
            for (Map.Entry<?, ?> e : properties.entrySet()) {
                String k = String.valueOf(e.getKey());
                if (!superProperties.containsKey(k)) {
                    superProperties.put(k, e.getValue());
                }
            }
        }
    }

    public void unregisterSuperProperty(String name) {
        superProperties.remove(name);
    }

    public Future<DartMap<String, Object>> getSuperProperties() {
        DartMap<String, Object> out = new DartMap<String, Object>();
        out.putAll(superProperties);
        return Future.value(out);
    }

    public void clearSuperProperties() {
        superProperties.clear();
    }

    public People getPeople() {
        return new People(this);
    }

    /** A new anonymous id; the queue and super properties are cleared, as in the plugin. */
    public void reset() {
        events.clear();
        profileUpdates.clear();
        superProperties.clear();
        timedEvents.clear();
        distinctId = randomId();
        if (Display.isInitialized()) {
            Preferences.set(key("distinct_id"), distinctId);
        }
    }

    private void queueEvent(String name, Map<String, Object> props) {
        if (optedOut) {
            return;
        }
        Map<String, Object> all = new LinkedHashMap<String, Object>();
        all.putAll(superProperties);
        all.putAll(props);
        all.put("token", token);
        if (!all.containsKey("distinct_id")) {
            all.put("distinct_id", distinctId);
        }
        all.put("time", Long.valueOf(new Date().getTime()));
        all.put("$insert_id", Long.toHexString(RANDOM.nextLong() & Long.MAX_VALUE));
        all.put("mp_lib", "flutter");
        all.put("$os", Display.isInitialized() ? Display.getInstance().getPlatformName() : "headless");
        Map<String, Object> ev = new LinkedHashMap<String, Object>();
        ev.put("event", name);
        ev.put("properties", all);
        events.add(ev);
        autoFlush();
    }

    void engage(Map<String, Object> operation) {
        if (optedOut) {
            return;
        }
        Map<String, Object> u = new LinkedHashMap<String, Object>();
        u.put("$token", token);
        u.put("$distinct_id", distinctId);
        u.putAll(operation);
        profileUpdates.add(u);
        autoFlush();
    }

    private void autoFlush() {
        if (cn1PendingCount() >= flushBatchSize) {
            flush();
        }
    }

    public Future<Object> flush() {
        if (!Display.isInitialized() || flushing || cn1PendingCount() == 0) {
            return Future.value(null);
        }
        if (!events.isEmpty()) {
            send("/track", events);
        }
        if (!profileUpdates.isEmpty()) {
            send("/engage", profileUpdates);
        }
        return Future.value(null);
    }

    /** POSTs the queued batch; it leaves the queue only once Mixpanel accepted it. */
    private void send(String path, final List<Object> queue) {
        final List<Object> batch = new ArrayList<Object>(queue);
        flushing = true;
        ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void postResponse() {
                flushing = false;
                queue.removeAll(batch);
            }

            @Override
            protected void handleException(Exception err) {
                flushing = false;
                Log.p("mixpanel: flush failed, kept for the next one: " + err);
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                flushing = false;
                Log.p("mixpanel: " + code + " " + message);
                if (code >= 400 && code < 500) {
                    // a rejected batch is never accepted later; resending it would block the queue
                    queue.removeAll(batch);
                }
            }
        };
        req.setUrl(serverUrl + path + "?ip=1");
        req.setPost(true);
        req.setHttpMethod("POST");
        req.setContentType("application/json");
        req.addRequestHeader("Accept", "text/plain");
        req.setRequestBody(Json.write(batch));
        req.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(req);
    }
}
