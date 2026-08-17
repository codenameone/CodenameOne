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

    public void index(String entitiesJson, Map<String, byte[]> images) {
        synchronized (lock) {
            // Stored by the raw document rather than parsed apart: the simulator
            // window presents what was published, and re-deriving the entities
            // here would let the preview drift from the wire format the device
            // actually receives.
            Map<String, String> published = new LinkedHashMap<String, String>();
            published.put("json", entitiesJson);
            index.put(String.valueOf(index.size()), published);
            if (images != null) {
                indexImages.putAll(images);
            }
        }
    }

    public void removeFromIndex(String idsJson) {
        synchronized (lock) {
            for (Map.Entry<String, Map<String, String>> e : index.entrySet()) {
                String json = e.getValue().get("json");
                if (json != null && idsJson != null && overlaps(json, idsJson)) {
                    e.getValue().put("removed", "true");
                }
            }
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
                String json = e.getValue().get("json");
                if (json != null && json.contains("\"" + entityType + "\"")) {
                    drop.add(e.getKey());
                }
            }
            for (int i = 0; i < drop.size(); i++) {
                index.remove(drop.get(i));
            }
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

    /// Every entity document published to the index this session.
    public List<String> getIndexedDocuments() {
        synchronized (lock) {
            List<String> out = new ArrayList<String>();
            for (Map<String, String> e : index.values()) {
                if (!"true".equals(e.get("removed"))) {
                    out.add(e.get("json"));
                }
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

    /// True when a published document mentions every `{type, id}` pair named in a
    /// removal request. Crude on purpose -- this is a preview, not an index.
    private static boolean overlaps(String publishedJson, String refsJson) {
        int idAt = refsJson.indexOf("\"id\"");
        if (idAt < 0) {
            return false;
        }
        int quote = refsJson.indexOf('"', refsJson.indexOf(':', idAt) + 1);
        if (quote < 0) {
            return false;
        }
        int end = refsJson.indexOf('"', quote + 1);
        if (end < 0) {
            return false;
        }
        String id = refsJson.substring(quote + 1, end);
        return publishedJson.contains("\"" + id + "\"");
    }
}
