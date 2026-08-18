/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase;

import com.codename1.intents.spi.IntentBridge;
import com.codename1.io.JSONParser;
import com.codename1.io.JSONWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The desktop side of the app intents framework.
///
/// The desktop has no assistant and no system-wide search index to publish
/// into, so this bridge keeps the whole model in memory and lets the simulator
/// show it. That is the point: a developer can build, run and debug an intent
/// with no device at all, and what the simulator lists is whatever the
/// application actually declared, because it is the same generated table a
/// device would read.
///
/// Reporting every capability as supported is deliberate rather than optimistic.
/// The simulator's job is to exercise the code paths a device takes, so
/// `areIntentsSupported` and its siblings return true and the whole dispatch,
/// donation and indexing machinery runs. What differs is only where the data
/// lands: a map here, the platform there.
public class JavaSEIntentBridge implements IntentBridge {

    private final Object lock = new Object();
    private final Map<String, Map<String, String>> index =
            new LinkedHashMap<String, Map<String, String>>();
    private final Map<String, byte[]> indexImages = new LinkedHashMap<String, byte[]>();
    private final List<String> donations = new ArrayList<String>();
    private String declarationsJson;

    public boolean areIntentsSupported() {
        return true;
    }

    public boolean isHeadlessExecutionSupported() {
        return true;
    }

    /// True so the simulator can drive a voice-shaped invocation.
    ///
    /// This is the one answer that differs from a device by more than storage.
    /// A developer needs to test the voice path somewhere, and the desktop is
    /// the only place they can do it without shipping a build. Code that gates
    /// user-facing copy on this should still expect false on Android.
    public boolean isVoiceInvocationSupported() {
        return true;
    }

    public boolean isIndexingSupported() {
        return true;
    }

    public boolean requestForeground() {
        // The simulator's window is the app, and it is already on screen. Answering true keeps
        // the developer seeing what a headless routed result does on the platform that supports
        // it, rather than the diagnostic for the platform that does not.
        foregroundRequests++;
        return true;
    }

    /// How many times a handler asked for the app to be brought forward, for the Intents window.
    public int getForegroundRequestCount() {
        return foregroundRequests;
    }

    private int foregroundRequests;

    public void registerIntents(String json) {
        synchronized (lock) {
            declarationsJson = json;
        }
    }

    public void donate(String intentId, String paramsJson) {
        synchronized (lock) {
            donations.add(intentId + " " + (paramsJson == null ? "{}" : paramsJson));
        }
    }

    /// Records one entry per entity, keyed by the `type:id` uid the platforms use as an
    /// index identity.
    ///
    /// One `Intents.index(List)` call publishes a single document carrying many entities, and
    /// storing it whole meant a removal took every sibling with it -- the simulator hiding
    /// entities a device would still be showing. The unit of storage has to be the unit the
    /// platform addresses, which is the entity.
    public void index(String entitiesJson, Map<String, byte[]> images) {
        synchronized (lock) {
            for (Map<String, Object> entity : entitiesIn(entitiesJson)) {
                Object uid = entity.get("uid");
                if (uid == null) {
                    continue;
                }
                Map<String, String> published = new LinkedHashMap<String, String>();
                published.put("json", JSONWriter.toJson(entity));
                published.put("type", String.valueOf(entity.get("type")));
                index.put(String.valueOf(uid), published);
            }
            if (images != null) {
                indexImages.putAll(images);
            }
        }
    }

    public void removeFromIndex(String idsJson) {
        synchronized (lock) {
            for (String uid : uidsIn(idsJson)) {
                index.remove(uid);
            }
            pruneImages();
        }
    }

    public void clearIndex(String entityType) {
        synchronized (lock) {
            if (entityType == null) {
                index.clear();
                indexImages.clear();
                return;
            }
            List<String> drop = new ArrayList<String>();
            for (Map.Entry<String, Map<String, String>> e : index.entrySet()) {
                if (entityType.equals(e.getValue().get("type"))) {
                    drop.add(e.getKey());
                }
            }
            for (int i = 0; i < drop.size(); i++) {
                index.remove(drop.get(i));
            }
            pruneImages();
        }
    }

    /// Drops thumbnails no surviving entry references.
    ///
    /// Removing a document left its blob behind, so a desktop session that repeatedly replaces
    /// indexed content grew forever and getIndexedImages() reported thumbnails belonging to
    /// entries that no longer exist -- a preview disagreeing with the device about what is
    /// published. Always called while holding the lock.
    private void pruneImages() {
        if (indexImages.isEmpty()) {
            return;
        }
        List<String> orphans = new ArrayList<String>();
        for (String name : indexImages.keySet()) {
            boolean referenced = false;
            for (Map<String, String> entry : index.values()) {
                String json = entry.get("json");
                if (json != null && json.contains("\"" + name + "\"")) {
                    referenced = true;
                    break;
                }
            }
            if (!referenced) {
                orphans.add(name);
            }
        }
        for (int i = 0; i < orphans.size(); i++) {
            indexImages.remove(orphans.get(i));
        }
    }

    /// The entity objects inside a published document, or empty when it cannot be read.
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> entitiesIn(String entitiesJson) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        Map<String, Object> doc = parse(entitiesJson);
        Object list = doc == null ? null : doc.get("entities");
        if (list instanceof List) {
            for (Object o : (List<Object>) list) {
                if (o instanceof Map) {
                    out.add((Map<String, Object>) o);
                }
            }
        }
        return out;
    }

    /// The uids named by a removal request.
    @SuppressWarnings("unchecked")
    private static List<String> uidsIn(String idsJson) {
        List<String> out = new ArrayList<String>();
        Map<String, Object> doc = parse(idsJson);
        Object list = doc == null ? null : doc.get("refs");
        if (list instanceof List) {
            for (Object o : (List<Object>) list) {
                if (o instanceof Map) {
                    Object uid = ((Map<String, Object>) o).get("uid");
                    if (uid != null) {
                        out.add(String.valueOf(uid));
                    }
                }
            }
        }
        return out;
    }

    private static Map<String, Object> parse(String json) {
        if (json == null || json.length() == 0) {
            return null;
        }
        try {
            return new JSONParser().parseJSON(new java.io.StringReader(json));
        } catch (Throwable t) {
            return null;
        }
    }

    public void completeInvocation(String token, String resultJson, Map<String, byte[]> images) {
        // The desktop has nobody waiting on the other side of a token: the
        // simulator window holds the IntentCompletion it passed in and reads the
        // result from there, so there is nothing to hand back here.
    }

    // ------------------------------------------------------------------
    // Simulator inspection
    // ------------------------------------------------------------------

    /// The intent catalogue the application published at startup, or null.
    public String getDeclarationsJson() {
        synchronized (lock) {
            return declarationsJson;
        }
    }

    /// Every donation made this session, oldest first.
    public List<String> getDonations() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<String>(donations));
        }
    }

    /// Every entity currently in the index, one JSON object each, in publication order.
    ///
    /// A removed entity is gone rather than flagged, which is what a device index looks like:
    /// there is no state in which an entry is both indexed and not.
    public List<String> getIndexedDocuments() {
        synchronized (lock) {
            List<String> out = new ArrayList<String>();
            for (Map<String, String> e : index.values()) {
                out.add(e.get("json"));
            }
            return Collections.unmodifiableList(out);
        }
    }

    /// Thumbnails published alongside indexed entities, keyed by wire name.
    public Map<String, byte[]> getIndexedImages() {
        synchronized (lock) {
            return Collections.unmodifiableMap(
                    new LinkedHashMap<String, byte[]>(indexImages));
        }
    }

    /// Clears everything this bridge recorded. Used by the simulator's reset and
    /// by tests.
    public void reset() {
        synchronized (lock) {
            index.clear();
            indexImages.clear();
            donations.clear();
            declarationsJson = null;
        }
    }

}
