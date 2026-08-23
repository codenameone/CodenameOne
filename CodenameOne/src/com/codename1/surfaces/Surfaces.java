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
package com.codename1.surfaces;

import com.codename1.io.Log;
import com.codename1.surfaces.spi.SurfaceBridge;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The static entry point for external surfaces: home-screen widgets and live activities -- the
/// two faces of one concept, a live source of information that resides outside your app. Declare
/// your widget kinds and register an action handler in `init()`, then publish content whenever
/// your data changes:
///
/// ```java
/// Surfaces.registerWidgetKind(new WidgetKind("delivery_status")
///         .setDisplayName("Delivery").setDescription("Track your order"));
/// Surfaces.setActionHandler(evt -> showOrder(evt.getParams()));
/// ...
/// Surfaces.publish("delivery_status", new WidgetTimeline()
///         .setContent(layout).addEntry(new Date(), state));
/// ```
///
/// #### How surfaces render
///
/// Surfaces render while your app process may be dead: the published timeline is serialized (a
/// JSON descriptor plus PNG blobs) and persisted where the platform renderer can reach it -- the
/// iOS widget extension, the Android widget provider or a desktop surface window. Layouts embed
/// `${key}` placeholders resolved from each timeline entry's state map, and `SurfaceDynamicText`
/// countdowns tick natively on the OS clock with no app wakeups. To refresh content periodically
/// implement `com.codename1.background.BackgroundFetch` and re-publish there.
///
/// Widget kinds must also be declared at build time in the project's `surfaces.json` resource --
/// the platform widget galleries are compiled into the native app. See the package documentation.
///
/// #### Zero cost when unused
///
/// Merely referencing this package makes the build inject the native plumbing (the WidgetKit
/// extension and app group on iOS, the widget receivers on Android). Apps that never touch
/// `com.codename1.surfaces` get none of it. On the simulator the Widgets preview window renders
/// published surfaces; on unsupported ports the API is an inert no-op.
public final class Surfaces {
    private static SurfaceBridge bridge;
    private static boolean bridgeOverridden;
    private static SurfaceActionHandler actionHandler;
    private static final List<SurfaceActionEvent> pendingActions =
            new ArrayList<SurfaceActionEvent>();
    private static final List<WidgetKind> registeredKinds = new ArrayList<WidgetKind>();

    private Surfaces() {
    }

    /// Returns true when this platform can render home-screen (or desktop) widgets.
    ///
    /// #### Returns
    ///
    /// true when widgets are supported
    public static boolean areWidgetsSupported() {
        SurfaceBridge b = bridgeInternal();
        return b != null && b.areWidgetsSupported();
    }

    /// Declares a widget kind at runtime. Call once per kind, typically from `init()`. The id must
    /// match a kind declared in the project's `surfaces.json` build-time manifest; a mismatch logs
    /// a prominent warning on supporting platforms.
    ///
    /// #### Parameters
    ///
    /// - `kind`: the kind declaration
    public static void registerWidgetKind(WidgetKind kind) {
        if (kind == null) {
            return;
        }
        // The whole API is callable from any thread, so a registration racing a publish (or
        // another registration) must not leave a reader walking a list that is being mutated
        // underneath it. Every touch of registeredKinds holds this lock, and readers copy out
        // rather than iterate the live list.
        synchronized (registeredKinds) {
            for (WidgetKind k : registeredKinds) {
                if (k.getId().equals(kind.getId())) {
                    registeredKinds.remove(k);
                    break;
                }
            }
            registeredKinds.add(kind);
        }
        SurfaceBridge b = bridgeInternal();
        if (b != null) {
            b.registerWidgetKind(SurfaceSerializer.serializeKind(kind));
        }
    }

    /// Returns the widget kinds registered so far.
    public static List<WidgetKind> getRegisteredKinds() {
        synchronized (registeredKinds) {
            return new ArrayList<WidgetKind>(registeredKinds);
        }
    }

    static boolean isKindRegistered(String kindId) {
        synchronized (registeredKinds) {
            for (WidgetKind k : registeredKinds) {
                if (k.getId().equals(kindId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Overrides whether the simulator-only surface diagnostics run. They are on in the simulator
    /// and off everywhere else, which is almost always what you want: they catch usage that works
    /// in the simulator but stalls or silently does nothing on a device (rasterizing a surface
    /// image on the EDT, publishing to a kind that was never registered, republishing far past the
    /// platform's reload budget), and they cost nothing in a shipped build because they never run
    /// there. Diagnostics that are certain to misbehave on a device throw `IllegalStateException`;
    /// the rest log a one-time warning.
    ///
    /// Pass null to restore the default behaviour. Turning them off is a last resort for a case a
    /// check gets wrong -- please report it if you hit one.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: true to force diagnostics on, false to force them off, null for the default
    public static void setDiagnosticsEnabled(Boolean enabled) {
        SurfaceDiagnostics.setEnabled(enabled);
    }

    /// Returns true when the simulator-only surface diagnostics are currently active.
    ///
    /// #### Returns
    ///
    /// true when diagnostics run for this process
    public static boolean isDiagnosticsEnabled() {
        return SurfaceDiagnostics.enabled();
    }

    /// Publishes a widget kind's content, atomically replacing any previously published timeline
    /// and asking the platform to re-render the kind's widget instances. A no-op on platforms
    /// without widget support.
    ///
    /// #### Threading
    ///
    /// Callable from any thread -- including
    /// `com.codename1.background.BackgroundFetch#performBackgroundFetch(long, com.codename1.util.Callback)`
    /// callbacks while the app UI is not running (on Android the fetch runs in a background
    /// service with no Activity at all). Publishing is data-only: the timeline is serialized,
    /// persisted where the platform renderer can reach it and the renderer is poked
    /// asynchronously. Implementing background fetch and re-publishing there is the intended way
    /// to keep widgets fresh; see the `com.codename1.surfaces.spi` package documentation for the
    /// per-platform background update story.
    ///
    /// A background thread is the RIGHT thread, not merely a permitted one. On a device this
    /// writes the payload into the shared container and makes a synchronous native call, and any
    /// `SurfaceImage` holding an `Image` that is not an `EncodedImage` is rasterized here -- on
    /// iOS that encode blocks the caller on the platform UI thread while the pixels are read back
    /// off the GPU. Publishing on the EDT therefore stalls the UI on hardware while looking
    /// instantaneous in the simulator. Pass `EncodedImage`s and publish off the EDT; the simulator
    /// diagnostics flag both mistakes (see [#setDiagnosticsEnabled(Boolean)]).
    ///
    /// #### Parameters
    ///
    /// - `kindId`: the widget kind id
    /// - `timeline`: the content to publish
    public static void publish(String kindId, WidgetTimeline timeline) {
        SurfaceDiagnostics.requireRegisteredKind(kindId);
        SurfaceDiagnostics.offEdtPreferred("Surfaces.publish");
        SurfaceDiagnostics.noteRepublish("kind:" + kindId, "widget kind \"" + kindId + "\"");
        SurfaceBridge b = bridgeInternal();
        if (b == null || !b.areWidgetsSupported()) {
            return;
        }
        Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
        String json = SurfaceSerializer.serializeTimeline(kindId, timeline, images);
        synchronized (publishLock(kindId)) {
            b.publishWidgetTimeline(kindId, json, images);
        }
    }

    /// One monitor per kind, created on demand and never removed. Kind ids come from
    /// surfaces.json, so the set is bounded by the app's own declaration.
    private static final Map<String, Object> PUBLISH_LOCKS = new HashMap<String, Object>();

    /// The monitor that serializes publishes of a single kind.
    ///
    /// A publish is a WRITE FOLLOWED BY A HAND-OFF, and the two are only meaningful as a pair:
    /// the platform replaces the timeline in its container and then gives the same descriptor to
    /// the watch. Let two publishes of one kind interleave and the later write can be paired with
    /// the earlier hand-off, so the watch is left holding a descriptor the phone has already
    /// replaced -- and left holding it for good, because nothing publishes again to correct it.
    /// The imagery is worse than stale rather than merely old: both platforms read the blobs back
    /// off disk at hand-off time, so the descriptor of one publish can be sent with the artwork of
    /// another, which is a pairing neither publish ever produced.
    ///
    /// publish() documents itself as callable from any thread, so two threads publishing one kind
    /// is a supported way to call this rather than an abuse of it.
    ///
    /// Per KIND rather than one global monitor: a publish is file I/O plus a synchronous native
    /// call, and two different kinds have nothing to say to each other.
    private static Object publishLock(String kindId) {
        synchronized (PUBLISH_LOCKS) {
            Object lock = PUBLISH_LOCKS.get(kindId);
            if (lock == null) {
                lock = new Object();
                PUBLISH_LOCKS.put(kindId, lock);
            }
            return lock;
        }
    }

    /// Push-framework entry point for a server-rendered timeline descriptor. The descriptor uses
    /// the same wire format as `publish()`. The descriptor is persisted directly once the
    /// Codename One runtime receives it. A platform that doesn't run application code for a
    /// background push applies it when the application next starts or resumes.
    ///
    /// Equivalent to [#publishRemote(String,String,Map)] with no imagery. A descriptor that
    /// references an image by name renders a gap where it should be, so prefer the overload
    /// whenever the artwork travelled with the descriptor.
    public static void publishRemote(String kindId, String timelineJson) {
        publishRemote(kindId, timelineJson, Collections.<String, byte[]>emptyMap());
    }

    /// As [#publishRemote(String,String)], with the imagery the descriptor references.
    ///
    /// A timeline's node tree names its images rather than embedding them -- `SurfaceSerializer`
    /// hashes the bytes and puts the hash on the wire -- so a descriptor that arrived from
    /// somewhere else is only complete if its side-map arrived too. Without this overload
    /// `publishRemote` discarded the imagery unconditionally and every referenced image rendered
    /// as a gap.
    ///
    /// The two callers are a server push and the phone-to-watch mirror, which forwards a
    /// phone-side `publish()` of a watch-bearing kind to the watch. Both are the same operation:
    /// a descriptor produced elsewhere, applied here.
    ///
    /// #### Parameters
    ///
    /// - `kindId`: the widget kind id
    /// - `timelineJson`: the serialized timeline, in the same wire format `publish()` produces
    /// - `images`: the referenced images by name, or an empty map when the descriptor names none
    public static void publishRemote(String kindId, String timelineJson,
            Map<String, byte[]> images) {
        SurfaceBridge b = bridgeInternal();
        if (b == null || !b.areWidgetsSupported() || kindId == null || timelineJson == null) {
            return;
        }
        // The KIND is input here too, and a worse one to get wrong than an image name: every
        // platform composes it into a directory path -- iOS as container + "/cn1surfaces/" +
        // kindId -- so "../activities/foo" writes the timeline AND its imagery outside the kind
        // directory, over whatever is there. publish() cannot produce such an id because
        // WidgetKind refuses it at construction; a descriptor that arrived from a server or from
        // the watch mirror never passed through that check, so it gets it here. The same
        // validator, not a second copy of the grammar.
        if (!WidgetKind.isValidId(kindId)) {
            Log.p("Surfaces: refusing a remote publish for a kind id that is not [a-z][a-z0-9_]*: "
                    + kindId);
            return;
        }
        // The same monitor publish() uses: a remote descriptor and a local one race exactly the
        // same way, and a push landing while the app publishes is the ordinary way it happens.
        synchronized (publishLock(kindId)) {
            b.publishWidgetTimeline(kindId, timelineJson, safeImageNames(images));
        }
    }

    /// The image side-map with anything that is not a plain blob name removed.
    ///
    /// A name here is a content hash produced by `SurfaceSerializer`, and every platform turns it
    /// into a file inside the kind's own directory. This descriptor did NOT come from this
    /// process, though -- a server push and the watch mirror both arrive from outside -- so the
    /// names are input, not something the app computed. A name carrying a separator or a parent
    /// segment escapes that directory: on iOS the path is composed as `dir + "/" + key + ".png"`
    /// with no sanitizing of its own, so `../other_kind/hash` plants a blob under a different
    /// kind, where a later legitimate publish will not replace it -- content-hash names are
    /// assumed to already hold the right bytes.
    ///
    /// Dropped rather than rejected wholesale: a descriptor referencing an image that did not
    /// arrive renders a gap, which every renderer already tolerates, and refusing the whole
    /// publish would let one bad name suppress a timeline that is otherwise fine.
    /// The prefix `SurfaceSerializer.registerImageBytes` puts in front of a content hash.
    private static final String CONTENT_HASH_PREFIX = "img";

    /// Whether a name has the shape SurfaceSerializer gives a content hash: the `img` prefix and
    /// sixteen lowercase hex digits. The prefix is the point -- checking for bare hex matched
    /// nothing the framework produces, so the integrity check below never ran on a real payload
    /// at all, and would have compared a prefixed name against an unprefixed hash if it had.
    ///
    /// Only names of this shape are verified, so one that was never a hash -- a registered image
    /// the app named itself -- is passed through rather than refused for failing a test that does
    /// not apply to it.
    private static boolean looksLikeContentHash(String name) {
        if (name.length() != CONTENT_HASH_PREFIX.length() + 16
                || !name.startsWith(CONTENT_HASH_PREFIX)) {
            return false;
        }
        for (int i = CONTENT_HASH_PREFIX.length(); i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, byte[]> safeImageNames(Map<String, byte[]> images) {
        if (images == null || images.isEmpty()) {
            return Collections.<String, byte[]>emptyMap();
        }
        Map<String, byte[]> safe = new LinkedHashMap<String, byte[]>();
        for (Map.Entry<String, byte[]> e : images.entrySet()) {
            String name = e.getKey();
            if (name == null || name.length() == 0 || name.indexOf('/') >= 0
                    || name.indexOf('\\') >= 0 || name.indexOf(':') >= 0
                    || name.indexOf('\0') >= 0 || ".".equals(name) || "..".equals(name)) {
                Log.p("Surfaces: dropping a remote image whose name is not a plain blob name: "
                        + name);
                continue;
            }
            if (looksLikeContentHash(name) && e.getValue() != null
                    && !name.equals(CONTENT_HASH_PREFIX + SurfaceSerializer.fnv1a(e.getValue()))) {
                // The name is a CLAIM about the bytes, and this descriptor came from outside the
                // process. iOS skips writing a blob whose file already exists, on the strength of
                // that claim -- so bad bytes landing first cannot be repaired by any later
                // legitimate publish, and the surface shows wrong artwork for good. Checking the
                // claim costs one pass over bytes that are about to be written anyway.
                Log.p("Surfaces: dropping a remote image whose bytes do not match its name: "
                        + name);
                continue;
            }
            if (e.getValue() == null) {
                // A name with no bytes -- one attachment of several failing to decode is the
                // ordinary way to get one. Android skips a null value; the iOS bridge writes it
                // straight to an OutputStream and the NullPointerException escapes its IOException
                // catch, so one missing blob aborted a publish whose timeline was otherwise fine.
                Log.p("Surfaces: dropping a remote image with no bytes: " + name);
                continue;
            }
            safe.put(name, e.getValue());
        }
        return safe;
    }

    /// Asks the platform to re-render widgets from their already-published timelines.
    ///
    /// #### Parameters
    ///
    /// - `kindId`: the kind to reload, or null for all kinds
    public static void reloadWidgets(String kindId) {
        SurfaceBridge b = bridgeInternal();
        if (b != null) {
            b.reloadWidgets(kindId);
        }
    }

    /// Returns the number of widget instances of a kind the user placed on the platform surface,
    /// or 0 when none exist or the platform cannot tell. Useful to skip publishing work when no
    /// widget is installed.
    ///
    /// #### Parameters
    ///
    /// - `kindId`: the widget kind id
    ///
    /// #### Returns
    ///
    /// the installed instance count, or 0
    public static int getInstalledWidgetCount(String kindId) {
        SurfaceBridge b = bridgeInternal();
        return b == null ? 0 : b.getInstalledWidgetCount(kindId);
    }

    /// Registers the single handler receiving surface action events on the EDT. Registration
    /// flushes any actions queued before it (e.g. the tap that cold-started the app), in arrival
    /// order, with their cold-start flag set.
    ///
    /// #### Parameters
    ///
    /// - `handler`: the handler, or null to clear
    public static void setActionHandler(SurfaceActionHandler handler) {
        // Install the handler and drain the cold-start queue atomically under the same lock
        // dispatchAction() uses, so an in-flight dispatch cannot observe a null handler and then
        // enqueue an event after this method already flushed an empty queue (which would strand
        // it forever). Either dispatch's read-and-enqueue happens entirely before this install
        // (the event is drained here) or entirely after (dispatch sees the handler and delivers).
        List<SurfaceActionEvent> queued = null;
        synchronized (pendingActions) {
            actionHandler = handler;
            if (handler != null && !pendingActions.isEmpty()) {
                queued = new ArrayList<SurfaceActionEvent>(pendingActions);
                pendingActions.clear();
            }
        }
        if (queued != null) {
            for (SurfaceActionEvent evt : queued) {
                deliver(handler, evt);
            }
        }
    }

    // --- framework/port entry points -----------------------------------------

    /// Framework/port entry point: delivers a surface action to the app. Ports call this after
    /// decoding their platform payload (deep link, intent extras, window click). Handles EDT
    /// marshaling; when no handler is registered yet the event is queued and flagged cold start.
    ///
    /// #### Parameters
    ///
    /// - `source`: the widget kind id or live activity type
    /// - `actionId`: the action id of the tapped node
    /// - `params`: the action parameters, may be null
    public static void dispatchAction(String source, String actionId, Map<String, Object> params) {
        SurfaceActionEvent evt = new SurfaceActionEvent(source, actionId, params);
        SurfaceActionHandler h;
        // Read the handler and decide queue-vs-deliver atomically under the same lock
        // setActionHandler() installs it and drains under -- see the note there.
        synchronized (pendingActions) {
            h = actionHandler;
            if (h == null) {
                evt.setColdStart(true);
                pendingActions.add(evt);
                return;
            }
        }
        deliver(h, evt);
    }

    /// Framework/port/test entry point: overrides the bridge resolved from the platform port.
    /// Passing null restores platform resolution.
    ///
    /// #### Parameters
    ///
    /// - `b`: the bridge, or null to resolve from the platform again
    public static void setBridge(SurfaceBridge b) {
        bridge = b;
        bridgeOverridden = b != null;
    }

    static SurfaceBridge bridgeInternal() {
        if (bridgeOverridden) {
            return bridge;
        }
        if (!Display.isInitialized()) {
            return null;
        }
        try {
            return Display.getInstance().getSurfaceBridge();
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    /// Test seam: clears the bridge override, handler, queued actions and registered kinds.
    static void reset() {
        bridge = null;
        bridgeOverridden = false;
        synchronized (pendingActions) {
            actionHandler = null;
            pendingActions.clear();
        }
        synchronized (registeredKinds) {
            registeredKinds.clear();
        }
        SurfaceDiagnostics.reset();
    }

    private static void deliver(final SurfaceActionHandler h, final SurfaceActionEvent evt) {
        if (h == null) {
            return;
        }
        if (Display.isInitialized()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    h.onSurfaceAction(evt);
                }
            });
        } else {
            h.onSurfaceAction(evt);
        }
    }
}
