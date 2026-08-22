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
package com.codename1.impl.android.surfaces;

import android.content.Context;
import android.util.Log;

import com.codename1.wearable.WearableConnection;
import com.codename1.wearable.WearableMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Carries a phone-published surface timeline to the paired watch, so a complication can show it.
 *
 * <p>A watch app has its own storage: nothing the phone writes is visible there. So a phone-side
 * {@code Surfaces.publish()} reaches a complication only if the descriptor actually travels, and
 * this is that transport -- over the Wearable Data Layer, using the same
 * {@code com.codename1.wearable} API an app would use by hand.</p>
 *
 * <p><b>Why the port and not the core.</b> {@code Executor.scanClassesForPermissions} scans the
 * app's own merged classes, not the Codename One core, so a core-level reference from
 * {@code com.codename1.surfaces} to {@code com.codename1.wearable} would not turn the Data Layer
 * glue on -- the mirror would be injected nowhere and silently do nothing. The port can reference
 * the wearable API freely, and the builder forces the glue on when watch families are declared.</p>
 *
 * <p>Best-effort by contract, and always after the local write has succeeded: nothing here can
 * leave the phone's own widget wrong. Every refusal is logged once under {@code CN1Surfaces} and
 * nothing throws.</p>
 */
public final class CN1SurfaceMirror {

    private static final String TAG = "CN1Surfaces";

    /**
     * Reserved application path. {@code CN1WearableBridge} namespaces this into a single opaque
     * segment under {@code /cn1}, so it cannot collide with a file transfer or an
     * acknowledgement -- only with an app that literally uses this string, which the guide
     * reserves.
     */
    private static final String PATH_PREFIX = "/cn1surface/";

    /**
     * A Data Layer item's inline payload is capped near 100KB and the whole put is rejected on
     * overflow, so the descriptor is held well under it. Complication art is a few dozen points
     * square; anything approaching this is a phone widget's artwork that a watch face would never
     * show anyway.
     */
    private static final int MAX_JSON_BYTES = 64 * 1024;

    private static final int MAX_IMAGE_BYTES = 256 * 1024;
    private static final int MAX_IMAGES = 8;
    private static final int MAX_TOTAL_IMAGE_BYTES = 1024 * 1024;

    private CN1SurfaceMirror() {
    }

    /**
     * Mirrors a freshly published timeline, when there is a watch that could show it.
     *
     * @param ctx any context
     * @param kindId the widget kind
     * @param timelineJson the serialized timeline
     * @param images the imagery the timeline references
     */
    public static void onPublished(Context ctx, String kindId, String timelineJson,
            Map<String, byte[]> images) {
        try {
            if (ctx == null || kindId == null || timelineJson == null) {
                return;
            }
            if (com.codename1.ui.CN.isWatch()) {
                // The watch's own publish is authoritative. Sending it back would hand the phone
                // a timeline it never asked for and, when the phone mirrored in the first place,
                // loop.
                return;
            }
            if (!CN1WatchSurface.isWatchKind(ctx, kindId)) {
                return;
            }
            if (!WearableConnection.isSupported()) {
                return;
            }
            byte[] json = timelineJson.getBytes("UTF-8");
            if (json.length > MAX_JSON_BYTES) {
                Log.w(TAG, "Widget kind \"" + kindId + "\" is too large to mirror to the watch ("
                        + json.length + " bytes, cap " + MAX_JSON_BYTES + "); the watch keeps its "
                        + "previous timeline");
                return;
            }
            // Imagery first, so the descriptor is never live against art that has not landed.
            sendImages(kindId, images);
            WearableMessage message = new WearableMessage(PATH_PREFIX + kindId);
            message.put("v", 1);
            message.put("json", json);
            WearableConnection.putData(message);
        } catch (Throwable t) {
            // The timeline is already persisted and the phone's own widget already updated. A
            // watch that does not hear about it is a degraded surface, not a failed publish.
            Log.w(TAG, "Could not mirror widget kind " + kindId + " to the watch", t);
        }
    }

    /**
     * Asks a paired watch to re-render a kind it already has.
     *
     * <p>{@code reloadWidgets} means "draw the descriptor you already hold again", and on the
     * watch that is a watch-local operation -- but in a COMPANION build the call runs in the phone
     * APK, and the complication and Tile services live in the wear module, so the notifier's
     * reflective lookups find nothing and the reload was a no-op for every mirrored surface. The
     * phone cannot reach into the other process; it can only ask.</p>
     *
     * <p>Asking is a re-send of the descriptor the watch already stored, which its receiver
     * applies exactly as it applies a fresh one -- and its notifier runs THERE, where the
     * generated services are. The nonce is what makes it arrive: the Data Layer suppresses a
     * DataItem whose payload is unchanged, which is the behaviour a publish wants and the one a
     * reload has to defeat. No images: their names are content hashes, so whatever the descriptor
     * references is already beside it.</p>
     *
     * @param ctx any context
     * @param kindId the widget kind to re-render
     */
    public static void requestWatchReload(Context ctx, String kindId) {
        try {
            if (com.codename1.ui.CN.isWatch() || !CN1WatchSurface.isWatchKind(ctx, kindId)
                    || !WearableConnection.isSupported()) {
                return;
            }
            String json = CN1SurfaceStore.readWidgetTimeline(ctx, kindId);
            if (json == null || json.length() == 0) {
                // Nothing published yet, so there is nothing for the watch to redraw.
                return;
            }
            byte[] bytes = json.getBytes("UTF-8");
            if (bytes.length > MAX_JSON_BYTES) {
                return;
            }
            // The artwork too, and not as an optimisation to skip. A reload is also how a watch
            // app installed AFTER the publish gets its first copy of anything, and a descriptor
            // whose content-hash images have never existed on that device renders as permanent
            // gaps until the app happens to publish again. Sent before the descriptor, for the
            // same reason a publish does.
            sendImages(kindId, storedImages(ctx, kindId));
            WearableMessage message = new WearableMessage(PATH_PREFIX + kindId);
            message.put("v", 1);
            message.put("json", bytes);
            message.put("nonce", System.currentTimeMillis());
            WearableConnection.putData(message);
        } catch (Throwable t) {
            // A watch that does not hear about a reload keeps showing what it had, which is the
            // same content: this is a refresh, not a change.
            Log.w(TAG, "Could not ask the watch to reload widget kind " + kindId, t);
        }
    }

    /**
     * The image blobs a kind has on disk, keyed by the name its descriptor references.
     *
     * <p>Read back rather than remembered, because a reload can be minutes or restarts away from
     * the publish that produced them, and the store is where they live in the meantime.</p>
     *
     * @param ctx any context
     * @param kindId the widget kind
     * @return the blobs, possibly empty
     */
    private static Map<String, byte[]> storedImages(Context ctx, String kindId) {
        Map<String, byte[]> out = new java.util.LinkedHashMap<String, byte[]>();
        File dir = CN1SurfaceStore.kindDir(ctx, kindId);
        File[] files = dir.listFiles();
        if (files == null) {
            return out;
        }
        for (File f : files) {
            String name = f.getName();
            if (!name.endsWith(".png")) {
                continue;
            }
            try {
                java.io.FileInputStream in = new java.io.FileInputStream(f);
                try {
                    byte[] blob = new byte[(int) f.length()];
                    int read = 0;
                    while (read < blob.length) {
                        int n = in.read(blob, read, blob.length - read);
                        if (n < 0) {
                            break;
                        }
                        read += n;
                    }
                    if (read == blob.length) {
                        out.put(name.substring(0, name.length() - 4), blob);
                    }
                } finally {
                    in.close();
                }
            } catch (Throwable t) {
                Log.w(TAG, "Could not read " + f + " to re-send it to the watch", t);
            }
        }
        return out;
    }

    private static void sendImages(String kindId, Map<String, byte[]> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        int sent = 0;
        int total = 0;
        for (Map.Entry<String, byte[]> e : images.entrySet()) {
            byte[] blob = e.getValue();
            if (blob == null || blob.length == 0) {
                continue;
            }
            if (blob.length > MAX_IMAGE_BYTES) {
                Log.w(TAG, "Skipping image \"" + e.getKey() + "\" of widget kind \"" + kindId
                        + "\" when mirroring to the watch: " + blob.length + " bytes exceeds the "
                        + MAX_IMAGE_BYTES + " byte cap. It renders as a gap on the watch face.");
                continue;
            }
            if (sent >= MAX_IMAGES || total + blob.length > MAX_TOTAL_IMAGE_BYTES) {
                Log.w(TAG, "Widget kind \"" + kindId + "\" references more imagery than is worth "
                        + "carrying to a watch face; the rest render as gaps.");
                return;
            }
            // A file transfer rather than a data item: the Data Layer streams these in the
            // background and they routinely exceed the inline payload cap. Names are content
            // hashes, so an unchanged image sends identical bytes and the receiver overwrites in
            // place.
            WearableConnection.transferFile(PATH_PREFIX + kindId, e.getKey() + ".png", blob);
            sent++;
            total += blob.length;
        }
    }

    /**
     * Applies a mirrored descriptor on the watch and re-renders whatever shows it.
     *
     * <p>Called from the injected listener service, which may be running with no Codename One
     * runtime at all: the Data Layer starts the app's process to deliver, and the whole point is
     * to refresh a complication rather than to bring an application forward. So this is a file
     * write and an update request, touching no framework state.</p>
     *
     * @param ctx any context
     * @param path the reserved application path the item arrived on
     * @param payload the item's payload
     */
    public static void receive(Context ctx, String path, byte[] payload) {
        try {
            String kindId = kindOf(path);
            if (kindId == null || payload == null) {
                return;
            }
            WearableMessage message = WearableMessage.fromByteArray(path, payload);
            byte[] json = message.getBytes("json", null);
            if (json == null) {
                return;
            }
            File kindDir = CN1SurfaceStore.kindDir(ctx, kindId);
            mkdirs(kindDir);
            writeAtomically(new File(kindDir, "timeline.json"), json);
            // AFTER the replacement is safely on disk, and with the same reference set the
            // publish path uses. Blob names are content hashes, so without this every changed
            // image leaves its predecessor behind for ever in the watch app's storage. Artwork
            // for the new descriptor that has not arrived yet is simply absent rather than
            // unreferenced, so this cannot delete an image the timeline is waiting for.
            CN1SurfaceStore.deleteUnreferencedImages(kindDir, new String(json, "UTF-8"));
            // A mirrored kind was never published by THIS process, so nothing else records it --
            // and reloadWidgets(null) walks the remembered set, so a reload-all on a watch whose
            // content only ever arrived from the phone skipped the complication entirely.
            CN1SurfaceStore.rememberKind(ctx, kindId);
            CN1WatchSurfaceNotifier.requestUpdate(ctx, kindId);
        } catch (Throwable t) {
            Log.w(TAG, "Could not apply a mirrored surface from " + path, t);
        }
    }

    /**
     * Stores one mirrored image beside the descriptor that references it.
     *
     * <p>The payload is the serialized {@code WearableMessage} a file transfer carries -- the
     * name and the bytes together -- rather than the raw file, which is what the delivery path
     * hands every other listener too.</p>
     *
     * @param ctx any context
     * @param path the reserved application path
     * @param payload the transfer payload
     */
    public static void receiveFile(Context ctx, String path, byte[] payload) {
        try {
            String kindId = kindOf(path);
            if (kindId == null || payload == null) {
                return;
            }
            WearableMessage transfer = WearableMessage.fromByteArray(path, payload);
            String name = transfer.getString("name", null);
            byte[] contents = transfer.getBytes("contents", null);
            if (name == null || contents == null) {
                return;
            }
            if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
                // A name is a content hash, never a path. Refusing one that looks like a path
                // keeps a malformed payload from writing outside the kind's own directory.
                Log.w(TAG, "Refusing a mirrored image with a suspicious name: " + name);
                return;
            }
            File dir = CN1SurfaceStore.kindDir(ctx, kindId);
            // Written whatever the descriptor on disk currently says, and deliberately so.
            //
            // onPublished sends the images BEFORE the descriptor that names them, precisely so a
            // descriptor is never live against art that has not landed -- so the normal case is an
            // image arriving while the PREVIOUS descriptor is still stored, and refusing anything
            // it does not name rejects exactly the art the next descriptor is waiting for. The
            // transfer is then acknowledged and gone, and the new descriptor references a blob
            // that will never exist.
            //
            // The opposite hazard -- art from publication N arriving after N+1's descriptor has
            // already collected -- leaves an orphan, but a bounded one: every descriptor collects
            // what it does not reference, so the next publish removes it, and only the art in
            // flight during the last publish of all can linger. That is a fixed cost, not the
            // unbounded growth it looks like at first glance, and it is the cheaper of the two
            // failures by a wide margin.
            mkdirs(dir);
            writeAtomically(new File(dir, name), contents);
            // A file transfer is asynchronous and unordered against the descriptor, so artwork
            // routinely lands AFTER the timeline that references it. The descriptor's own arrival
            // already asked for a refresh, but that render saw a gap where this image belongs --
            // and nothing else would ask again until the next publish. So each arriving image
            // asks too.
            CN1WatchSurfaceNotifier.requestUpdate(ctx, kindId);
        } catch (Throwable t) {
            Log.w(TAG, "Could not store a mirrored image from " + path, t);
        }
    }

    /**
     * Withdraws a mirrored surface the phone has removed.
     *
     * <p>The Data Layer announces an unpublish as a deletion of the item, and a mirror never
     * entered the replication cache that ordinarily handles one -- so without this the descriptor
     * stayed on disk and the complication went on showing content the phone had already taken
     * down. Deleting the whole kind directory rather than the descriptor alone: its images exist
     * only to serve it, and the reference set that would tell them apart has just gone away.</p>
     *
     * <p>The watch face is asked to re-read afterwards, which is what makes the slot go back to
     * whatever it shows for a source with no data.</p>
     *
     * @param ctx any context
     * @param path the reserved application path that was deleted
     */
    public static void remove(Context ctx, String path) {
        try {
            String kindId = kindOf(path);
            if (kindId == null) {
                return;
            }
            // kindDir always answers a File -- it composes a path and never looks at the disk --
            // so listFiles() returning null is how "there is nothing here" arrives, and there is
            // no directory to guard against.
            File kindDir = CN1SurfaceStore.kindDir(ctx, kindId);
            File[] files = kindDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.delete()) {
                        Log.w(TAG, "Could not delete " + f + " while withdrawing a mirror");
                    }
                }
            }
            if (kindDir.exists() && !kindDir.delete()) {
                Log.w(TAG, "Could not delete " + kindDir + " while withdrawing a mirror");
            }
            CN1WatchSurfaceNotifier.requestUpdate(ctx, kindId);
        } catch (Throwable t) {
            Log.w(TAG, "Could not withdraw a mirrored surface from " + path, t);
        }
    }

    /** True when a Data Layer path belongs to this framework rather than to the app. */
    public static boolean isMirrorPath(String path) {
        return path != null && path.startsWith(PATH_PREFIX);
    }

    private static String kindOf(String path) {
        if (!isMirrorPath(path)) {
            return null;
        }
        String kindId = path.substring(PATH_PREFIX.length());
        return kindId.length() == 0 ? null : kindId;
    }

    /**
     * Creates a directory, failing loudly when it could not be.
     *
     * <p>The return value of {@code mkdirs()} alone is the wrong test: it answers false both when
     * the directory could not be created AND when it already exists, which here is the common
     * case. Existence afterwards is what the caller actually needs, and the callers turn a
     * failure into a logged warning rather than a lost timeline.</p>
     */
    private static void mkdirs(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("could not create " + dir);
        }
    }

    private static void writeAtomically(File target, byte[] bytes) throws IOException {
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        FileOutputStream out = new FileOutputStream(tmp);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
        if (!tmp.renameTo(target)) {
            // A rename across the same directory should not fail, but a partial descriptor is
            // worse than a stale one, so the half-written file goes rather than the good one.
            if (!tmp.delete()) {
                Log.w(TAG, "Could not remove a partial mirrored file at " + tmp);
            }
            throw new IOException("could not replace " + target);
        }
    }
}
