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
package com.codename1.impl.ios;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.surfaces.spi.SurfaceBridge;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/// iOS `SurfaceBridge` backing the `com.codename1.surfaces` API with WidgetKit and ActivityKit.
///
/// Published payloads are persisted into the shared App Group container (the group id comes from
/// the `CN1SurfacesAppGroup` Info.plist key injected by the build) where the generated CN1Widgets
/// extension reads them while the app process is dead:
///
/// - widget timelines: `<container>/cn1surfaces/<kindId>/timeline.json` plus `<name>.png` blobs
/// - live activity art: `<container>/cn1surfaces/activities/<name>.png`
///
/// timeline.json is written write-then-rename so the extension never observes a partial file.
/// WidgetCenter reloads and the ActivityKit lifecycle go through the `surfaces*` natives in
/// `IOSNative`, which trampoline into the Swift `CN1SurfaceBridge` class compiled into the app
/// target (WidgetKit/ActivityKit are Swift-only frameworks).
///
/// All file IO goes through `FileSystemStorage` (which tolerates the container's plain absolute
/// paths): `java.io.File`'s mutating methods are unimplemented natives on the ParparVM iOS
/// runtime -- referencing them fails the native link.
///
/// This whole class is dead code unless the build linked the surfaces natives (the
/// `CN1_USE_WIDGETS` define the builder flips when the app references `com.codename1.surfaces`);
/// without it every native answers unsupported and the public API no-ops.
final class IOSSurfaceBridge implements SurfaceBridge {
    private final IOSNative nativeInstance;
    private final FileSystemStorage fs = FileSystemStorage.getInstance();
    private boolean warnedNoContainer;

    IOSSurfaceBridge(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
    }

    public boolean areWidgetsSupported() {
        return nativeInstance.surfacesWidgetsSupported();
    }

    public boolean isLiveActivitySupported() {
        return nativeInstance.surfacesActivitiesSupported();
    }

    public void registerWidgetKind(String kindJson) {
        String container = containerPath();
        if (container == null || kindJson == null) {
            return;
        }
        try {
            Map<String, Object> kind = JSONParser.parseJSON(kindJson);
            Object id = kind.get("id");
            if (!(id instanceof String) || ((String) id).length() == 0) {
                return;
            }
            String kindsDir = container + "/cn1surfaces/kinds";
            mkdirs(container, "cn1surfaces/kinds");
            writeAtomically(kindsDir, id + ".json", kindJson.getBytes("UTF-8"));
        } catch (IOException e) {
            Log.e(e);
        }
    }

    public void publishWidgetTimeline(String kindId, String timelineJson,
            Map<String, byte[]> images) {
        String container = containerPath();
        if (container == null) {
            return;
        }
        try {
            String kindDir = container + "/cn1surfaces/" + kindId;
            mkdirs(container, "cn1surfaces/" + kindId);
            writeImages(kindDir, images);
            writeAtomically(kindDir, "timeline.json", timelineJson.getBytes("UTF-8"));
            // GC after the replacement timeline is in place: content-hash names mean
            // frequently changing art would otherwise grow the container without bound
            deleteUnreferencedImages(kindDir, timelineJson);
        } catch (IOException e) {
            Log.e(e);
            return;
        }
        nativeInstance.surfacesReloadTimelines(kindId);
    }

    public void reloadWidgets(String kindId) {
        nativeInstance.surfacesReloadTimelines(kindId == null ? "" : kindId);
    }

    public int getInstalledWidgetCount(String kindId) {
        // WidgetCenter.getCurrentConfigurations is async; the Swift bridge caches the last
        // answer and the native returns 0 while it is still unknown.
        return nativeInstance.surfacesInstalledCount(kindId);
    }

    public String startLiveActivity(String descriptorJson, Map<String, byte[]> images) {
        String container = containerPath();
        if (container != null && images != null && !images.isEmpty()) {
            // Image names are content hashes so a shared directory dedups across activities;
            // the descriptor references them by name exactly like a widget timeline does.
            try {
                String actDir = container + "/cn1surfaces/activities";
                mkdirs(container, "cn1surfaces/activities");
                writeImages(actDir, images);
            } catch (IOException e) {
                Log.e(e);
            }
        }
        String id = nativeInstance.surfacesStartActivity(descriptorJson);
        if (id == null || id.length() == 0) {
            return null;
        }
        return id;
    }

    public void updateLiveActivity(String activityId, String stateJson) {
        nativeInstance.surfacesUpdateActivity(activityId, stateJson);
    }

    public void endLiveActivity(String activityId, String finalStateJson,
            boolean dismissImmediately) {
        nativeInstance.surfacesEndActivity(activityId, finalStateJson, dismissImmediately);
    }

    // --- internals ------------------------------------------------------------

    /// Resolves the App Group container path, or null (with a one-time log) when the build has
    /// no usable app group -- e.g. the CN1_USE_WIDGETS define is off or the group is missing from
    /// the provisioning profile so containerURLForSecurityApplicationGroupIdentifier fails.
    private String containerPath() {
        String container = nativeInstance.getSurfacesContainerPath();
        if (container == null || container.length() == 0) {
            if (!warnedNoContainer) {
                warnedNoContainer = true;
                Log.p("Surfaces: no App Group container is available; check that the build "
                        + "declared surfaces.json and that the app group in the "
                        + "CN1SurfacesAppGroup Info.plist key exists on the App ID");
            }
            return null;
        }
        if (container.endsWith("/")) {
            container = container.substring(0, container.length() - 1);
        }
        return container;
    }

    private void writeImages(String dir, Map<String, byte[]> images) throws IOException {
        if (images == null) {
            return;
        }
        for (Map.Entry<String, byte[]> e : images.entrySet()) {
            String png = dir + "/" + e.getKey() + ".png";
            if (fs.exists(png)) {
                // Names derive from a content hash, so an existing file is the same bytes.
                continue;
            }
            write(png, e.getValue());
        }
    }

    /// Writes `<name>.tmp` then renames over the target so the widget extension, which may read
    /// concurrently from another process, never sees a partially written document.
    private void writeAtomically(String dir, String name, byte[] data) throws IOException {
        String target = dir + "/" + name;
        String tmp = target + ".tmp";
        write(tmp, data);
        if (fs.exists(target)) {
            fs.delete(target);
        }
        // a relative new name renames within the same directory
        fs.rename(tmp, name);
        if (!fs.exists(target)) {
            throw new IOException("Failed to rename " + tmp + " to " + target);
        }
    }

    private void write(String path, byte[] data) throws IOException {
        OutputStream os = fs.openOutputStream(path);
        try {
            os.write(data);
        } finally {
            os.close();
        }
    }

    /// Creates the nested directories of `relative` (slash-separated) under `base` one level at
    /// a time: `FileSystemStorage.mkdir` has no mkdirs equivalent.
    private void mkdirs(String base, String relative) {
        String current = base;
        int start = 0;
        while (start < relative.length()) {
            int slash = relative.indexOf('/', start);
            String segment = slash < 0 ? relative.substring(start)
                    : relative.substring(start, slash);
            if (segment.length() > 0) {
                current = current + "/" + segment;
                if (!fs.exists(current)) {
                    fs.mkdir(current);
                }
            }
            if (slash < 0) {
                break;
            }
            start = slash + 1;
        }
    }

    /// Deletes `<name>.png` blobs in the kind directory that the freshly published timeline no
    /// longer references. The document's `images` list is the complete reference set (the
    /// serializer includes registered-name references, not just newly shipped blobs). Runs after
    /// the new `timeline.json` is in place so a concurrently rendering extension re-reads the
    /// replacement document first.
    private void deleteUnreferencedImages(String kindDir, String timelineJson) {
        try {
            Map<String, Object> doc = new JSONParser()
                    .parseJSON(new java.io.StringReader(timelineJson));
            Object names = doc.get("images");
            java.util.Set<String> referenced = new java.util.HashSet<String>();
            if (names instanceof java.util.Collection) {
                for (Object o : (java.util.Collection<?>) names) {
                    referenced.add(String.valueOf(o));
                }
            }
            String[] files = fs.listFiles(kindDir);
            if (files == null) {
                return;
            }
            for (String name : files) {
                if (name == null) {
                    continue;
                }
                // listFiles returns child names; directories carry a trailing slash on some ports
                if (name.endsWith("/")) {
                    continue;
                }
                if (name.endsWith(".png")
                        && !referenced.contains(name.substring(0, name.length() - 4))) {
                    fs.delete(kindDir + "/" + name);
                }
            }
        } catch (Exception e) {
            Log.e(e);
        }
    }
}
