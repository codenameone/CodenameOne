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
package com.codename1.impl.android.intents;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.intents.spi.IntentBridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/// Android `IntentBridge`.
///
/// #### What Android can and cannot do here
///
/// Android has no contract by which an assistant invokes an application capability and receives
/// a typed result back, so `isVoiceInvocationSupported()` answers **false** and the framework
/// documents phrases, system disambiguation and spoken results as iOS-only. Saying otherwise
/// would be the kind of promise that only fails on a user's device.
///
/// What Android does deliver is real and useful: launcher shortcuts, dynamic shortcuts the
/// system learns to suggest from donation, and genuinely headless execution -- the last of which
/// is actually easier here than on iOS, because the port already boots without an Activity for
/// background fetch.
///
/// Indexing maps onto long-lived dynamic shortcuts rather than a separate search index. That is
/// Google's own current recommendation for surfacing app content, and it needs no additional
/// dependency, which keeps the "zero cost when unused" promise intact for every app that does
/// not use this package.
public class AndroidIntentBridge implements IntentBridge {

    private static final String TAG = "CN1Intents";
    /// The scheme the generated shortcuts and the trampoline agree on.
    public static final String SCHEME = "cn1intent";
    private static final int MAX_SHORTCUTS = 10;
    /// API levels named as literals rather than through `Build.VERSION_CODES`, because the port
    /// compiles against an older `android.jar` than these releases; the constants do not exist
    /// there even though the runtime values are fixed and public.
    private static final int API_LONG_LIVED = 29;
    private static final int API_PUSH_DYNAMIC = 30;

    private final List<String> indexed = new ArrayList<String>();

    public boolean areIntentsSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
    }

    public boolean isHeadlessExecutionSupported() {
        // AndroidImplementation.startContext boots Display with a Service context, which is the
        // same path background fetch already uses in production.
        return true;
    }

    public boolean isVoiceInvocationSupported() {
        return false;
    }

    public boolean isIndexingSupported() {
        return areIntentsSupported();
    }

    public void registerIntents(String declarationsJson) {
        // The static shortcuts were compiled into res/xml at build time from the same manifest,
        // so there is nothing to register at runtime. Kept as a no-op rather than removed from
        // the SPI: iOS genuinely needs it, and a bridge method that exists everywhere is easier
        // to reason about than one that does not.
    }

    public void donate(String intentId, String paramsJson) {
        if (!areIntentsSupported()) {
            return;
        }
        Context ctx = context();
        if (ctx == null || intentId == null) {
            return;
        }
        try {
            // A shortcut outlives the process, but a parameterization lives only in memory. So
            // the shortcut has to carry what the build-time registry can actually run: the base
            // intent, with the bound values merged in. Donating the runtime id would produce a
            // shortcut that works until the app is killed and reports an unknown intent after.
            com.codename1.intents.DynamicIntent dyn = com.codename1.intents.Intents
                    .getDynamicIntent(intentId);
            String targetId = intentId;
            String effectiveParams = paramsJson;
            String label = intentId;
            if (dyn != null) {
                targetId = dyn.getBaseIntentId();
                label = dyn.getTitle();
                effectiveParams = com.codename1.intents.IntentSerializer.mergeParams(
                        dyn.getBoundParameters(), paramsJson);
            }
            pushShortcut(ctx, intentId, label, label,
                    uriFor(targetId, effectiveParams, ctx));
        } catch (Throwable t) {
            Log.w(TAG, "Could not donate " + intentId, t);
        }
    }

    public void index(String entitiesJson, Map<String, byte[]> images) {
        if (!isIndexingSupported()) {
            return;
        }
        Context ctx = context();
        if (ctx == null || entitiesJson == null) {
            return;
        }
        // Deliberately shallow parsing: the payload is a known shape produced by the framework's
        // own serializer, and pulling in a JSON dependency for the port would cost every app.
        List<String[]> entries = CN1IntentJson.entities(entitiesJson);
        int published = 0;
        for (String[] entry : entries) {
            if (published >= MAX_SHORTCUTS) {
                // The launcher caps how many it will show. Truncating silently would look like
                // indexing randomly failing, so it is reported once.
                Log.i(TAG, "Indexed the first " + MAX_SHORTCUTS
                        + " items; Android limits how many shortcuts an app may publish");
                break;
            }
            String uid = entry[0];
            String title = entry[1];
            String subtitle = entry[2];
            try {
                String openUri = SCHEME + "://open?uid=" + Uri.encode(uid);
                String nonce = CN1IntentNonce.get(ctx);
                if (nonce != null) {
                    openUri += "&n=" + Uri.encode(nonce);
                }
                pushShortcut(ctx, uid, title, subtitle, Uri.parse(openUri));
                synchronized (indexed) {
                    if (!indexed.contains(uid)) {
                        indexed.add(uid);
                    }
                }
                published++;
            } catch (Throwable t) {
                Log.w(TAG, "Could not index " + uid, t);
            }
        }
    }

    public void removeFromIndex(String idsJson) {
        if (!isIndexingSupported()) {
            return;
        }
        Context ctx = context();
        if (ctx == null) {
            return;
        }
        List<String> uids = CN1IntentJson.refs(idsJson);
        if (uids.isEmpty()) {
            return;
        }
        removeShortcuts(ctx, uids);
        synchronized (indexed) {
            indexed.removeAll(uids);
        }
    }

    public void clearIndex(String entityType) {
        Context ctx = context();
        if (ctx == null) {
            return;
        }
        // Asks the platform what is actually published rather than trusting this instance's
        // memory. Shortcuts outlive the process, so after a restart the in-memory list is empty
        // while the launcher still shows every previously indexed item -- and a clear that
        // quietly removed nothing is worse than one that fails loudly.
        List<String> drop = publishedIds(ctx, entityType);
        synchronized (indexed) {
            for (String uid : indexed) {
                if (matchesType(uid, entityType) && !drop.contains(uid)) {
                    drop.add(uid);
                }
            }
            indexed.removeAll(drop);
        }
        if (!drop.isEmpty()) {
            removeShortcuts(ctx, drop);
        }
    }

    /// The ids this app has published that belong to `entityType`, or all of them when null.
    @TargetApi(Build.VERSION_CODES.N_MR1)
    private List<String> publishedIds(Context ctx, String entityType) {
        List<String> out = new ArrayList<String>();
        if (!areIntentsSupported()) {
            return out;
        }
        // Only the platform call is guarded. A cast inside a catch(Throwable) block reads as
        // relying on ClassCastException, which ParparVM never throws -- and the repo's gate
        // rejects the shape wherever it appears, so the iteration stays outside.
        ShortcutManager manager = (ShortcutManager) ctx.getSystemService(ShortcutManager.class);
        if (manager == null) {
            return out;
        }
        List<ShortcutInfo> live;
        try {
            live = manager.getDynamicShortcuts();
        } catch (Throwable t) {
            Log.w(TAG, "Could not read the published shortcuts", t);
            return out;
        }
        if (live == null) {
            return out;
        }
        for (ShortcutInfo info : live) {
            String id = info.getId();
            if (id != null && matchesType(id, entityType)) {
                out.add(id);
            }
        }
        return out;
    }

    /// An indexed id is `type:id`; an intent shortcut is a bare intent id and never matches a
    /// type, which is what keeps clearIndex from removing the app's own launcher actions.
    private static boolean matchesType(String uid, String entityType) {
        if (entityType == null) {
            return uid.indexOf(':') > 0;
        }
        return uid.startsWith(entityType + ":");
    }

    public void completeInvocation(String token, String resultJson, Map<String, byte[]> images) {
        // Nothing is waiting on a token here. Android has no continuation to resume: an
        // invocation either ran in the foreground, where the app itself shows the outcome, or in
        // CN1IntentService, which surfaces the spoken line itself.
    }

    // ------------------------------------------------------------------
    // Shortcut plumbing
    // ------------------------------------------------------------------

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private void pushShortcut(Context ctx, String id, String shortLabel, String longLabel,
                               Uri data) {
        ShortcutManager manager = (ShortcutManager) ctx.getSystemService(ShortcutManager.class);
        if (manager == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, data);
        intent.setClass(ctx, CN1IntentTrampolineActivity.class);

        ShortcutInfo.Builder b = new ShortcutInfo.Builder(ctx, id)
                .setShortLabel(safeLabel(shortLabel, id))
                .setLongLabel(safeLabel(longLabel, shortLabel))
                .setIntent(intent);
        if (Build.VERSION.SDK_INT >= API_LONG_LIVED) {
            // Long-lived is what lets the system keep suggesting a shortcut after the app drops
            // it from the dynamic set, which is the whole point of donating. Reflective because
            // the compile-time android.jar predates the method; a device that has it uses it,
            // and one that does not simply gets an ordinary dynamic shortcut.
            invokeQuietly(b, "setLongLived", new Class[]{boolean.class},
                    new Object[]{Boolean.TRUE});
        }
        try {
            Icon icon = Icon.createWithResource(ctx, ctx.getApplicationInfo().icon);
            b.setIcon(icon);
        } catch (Throwable ignored) {
            // An icon is optional; a shortcut without one is still usable.
        }
        ShortcutInfo info = b.build();
        // pushDynamicShortcut makes room by evicting the least-used shortcut instead of failing
        // once the app is at the platform cap, which is what a donation wants. Same story as
        // above: reflective, with the older API as the fallback rather than an error.
        if (Build.VERSION.SDK_INT < API_PUSH_DYNAMIC
                || !invokeQuietly(manager, "pushDynamicShortcut",
                        new Class[]{ShortcutInfo.class}, new Object[]{info})) {
            try {
                manager.addDynamicShortcuts(Arrays.asList(info));
            } catch (IllegalArgumentException e) {
                // Thrown once the app is at the platform's shortcut cap. Losing a suggestion is
                // not worth failing the caller's action over.
                Log.i(TAG, "At the shortcut limit; not publishing " + id);
            }
        }
        try {
            manager.reportShortcutUsed(id);
        } catch (Throwable ignored) {
            // Usage reporting is a hint to the launcher, never load bearing.
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private void removeShortcuts(Context ctx, List<String> ids) {
        ShortcutManager manager = (ShortcutManager) ctx.getSystemService(ShortcutManager.class);
        if (manager == null) {
            return;
        }
        try {
            manager.removeDynamicShortcuts(ids);
            // Removing a long-lived shortcut is not enough on its own: the system keeps a cached
            // copy it can still surface, so it also has to be disabled or the user goes on
            // seeing content the app has said is gone.
            manager.disableShortcuts(ids);
        } catch (Throwable t) {
            Log.w(TAG, "Could not remove shortcuts", t);
        }
    }

    /// Calls a method that may not exist on this device's platform version. Returns false when
    /// it was unavailable, so the caller can fall back rather than treating absence as failure.
    private static boolean invokeQuietly(Object target, String method, Class[] signature,
                                          Object[] args) {
        try {
            java.lang.reflect.Method m = target.getClass().getMethod(method, signature);
            m.invoke(target, args);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String safeLabel(String preferred, String fallback) {
        String s = preferred != null && preferred.length() > 0 ? preferred : fallback;
        if (s == null) {
            return "";
        }
        // The launcher truncates hard; trimming here keeps the visible text predictable.
        return s.length() > 40 ? s.substring(0, 40) : s;
    }

    /// Builds the shortcut URI. The headless flag rides along so the trampoline can route a
    /// cold-start tap without the declaration table, which does not exist yet at that point.
    private static Uri uriFor(String intentId, String paramsJson, Context ctx) {
        String uri = SCHEME + "://run?id=" + Uri.encode(intentId);
        if (paramsJson != null && paramsJson.length() > 0) {
            uri += "&p=" + Uri.encode(paramsJson);
        }
        com.codename1.intents.IntentDeclaration decl =
                com.codename1.intents.Intents.getDeclaration(intentId);
        if (decl != null && decl.isHeadless()) {
            uri += "&h=1";
        }
        // Marks the URI as one this application published, which is what lets the trampoline
        // run it without the restrictions an unauthenticated caller is held to.
        String nonce = CN1IntentNonce.get(ctx);
        if (nonce != null) {
            uri += "&n=" + Uri.encode(nonce);
        }
        return Uri.parse(uri);
    }

    private static Context context() {
        Context ctx = AndroidImplementation.getContext();
        return ctx == null ? null : ctx.getApplicationContext();
    }
}
