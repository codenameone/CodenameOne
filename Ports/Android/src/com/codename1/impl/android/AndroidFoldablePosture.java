/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import com.codename1.ui.Display;
import com.codename1.ui.DevicePosture;
import com.codename1.ui.geom.Rectangle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Reads the device fold posture from androidx.window entirely through reflection so the core
 * Android port carries no compile-time dependency on androidx.window. The dependency is added to
 * the generated build only when the app opts in with the build hint
 * {@code android.foldableSupport=true} (see AndroidGradleBuilder). When androidx.window is not on
 * the classpath, or the device is not foldable, every query degrades safely to "not foldable".
 *
 * <p>This avoids the Kotlin coroutine based WindowInfoTracker flow by using the Java friendly
 * androidx.window.java.layout.WindowInfoTrackerCallbackAdapter and an androidx.core.util.Consumer
 * implemented with a dynamic proxy.</p>
 */
class AndroidFoldablePosture {
    private static volatile boolean foldable;
    private static volatile int posture = DevicePosture.POSTURE_UNKNOWN;
    private static volatile int foldOrientation = DevicePosture.FOLD_ORIENTATION_NONE;
    private static volatile boolean separating;
    private static volatile boolean hasBounds;
    private static volatile int boundsX;
    private static volatile int boundsY;
    private static volatile int boundsW;
    private static volatile int boundsH;
    private static boolean started;

    static boolean isFoldable() {
        return foldable;
    }

    static int getPosture() {
        return foldable ? posture : DevicePosture.POSTURE_UNKNOWN;
    }

    static int getFoldOrientation() {
        return foldable ? foldOrientation : DevicePosture.FOLD_ORIENTATION_NONE;
    }

    static boolean isSeparating() {
        return separating;
    }

    static Rectangle getFoldBounds(Rectangle rect) {
        if (!hasBounds) {
            return null;
        }
        return new Rectangle(boundsX, boundsY, boundsW, boundsH);
    }

    /**
     * Starts the androidx.window layout listener if it has not been started yet. Safe to call
     * repeatedly and safe to call when androidx.window is absent (it simply does nothing).
     */
    static synchronized void start(final Activity activity) {
        if (started || activity == null) {
            return;
        }
        try {
            Class<?> trackerCls = Class.forName("androidx.window.layout.WindowInfoTracker");
            Object tracker = trackerCls.getMethod("getOrCreate", Context.class).invoke(null, activity);
            if (tracker == null) {
                return;
            }
            Class<?> adapterCls = Class.forName("androidx.window.java.layout.WindowInfoTrackerCallbackAdapter");
            Object adapter = adapterCls.getConstructor(trackerCls).newInstance(tracker);
            final Class<?> foldingFeatureCls = Class.forName("androidx.window.layout.FoldingFeature");
            Class<?> consumerCls = Class.forName("androidx.core.util.Consumer");
            Executor executor = new Executor() {
                public void execute(Runnable command) {
                    activity.runOnUiThread(command);
                }
            };
            InvocationHandler handler = new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("accept".equals(name) && args != null && args.length == 1 && args[0] != null) {
                        try {
                            onLayoutInfo(args[0], foldingFeatureCls);
                        } catch (Throwable t) {
                            // ignore - leave posture unchanged on a parse failure
                        }
                        return null;
                    }
                    if ("toString".equals(name)) {
                        return "CN1FoldConsumer";
                    }
                    if ("hashCode".equals(name)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(name)) {
                        return proxy == args[0];
                    }
                    return null;
                }
            };
            Object consumer = Proxy.newProxyInstance(adapterCls.getClassLoader(),
                    new Class[]{consumerCls}, handler);
            adapterCls.getMethod("addWindowLayoutInfoListener", Activity.class, Executor.class, consumerCls)
                    .invoke(adapter, activity, executor, consumer);
            started = true;
        } catch (Throwable t) {
            // androidx.window not present or an API mismatch: stay non-foldable.
        }
    }

    private static void onLayoutInfo(Object info, Class<?> foldingFeatureCls) throws Exception {
        List<?> features = (List<?>) info.getClass().getMethod("getDisplayFeatures").invoke(info);
        boolean foundFold = false;
        int newPosture = DevicePosture.POSTURE_FLAT;
        int newOrientation = DevicePosture.FOLD_ORIENTATION_NONE;
        boolean newSeparating = false;
        Rect bounds = null;
        if (features != null) {
            for (Object feature : features) {
                if (foldingFeatureCls.isInstance(feature)) {
                    foundFold = true;
                    Object state = foldingFeatureCls.getMethod("getState").invoke(feature);
                    Object orientation = foldingFeatureCls.getMethod("getOrientation").invoke(feature);
                    newSeparating = Boolean.TRUE.equals(
                            foldingFeatureCls.getMethod("isSeparating").invoke(feature));
                    Object rect = foldingFeatureCls.getMethod("getBounds").invoke(feature);
                    if (rect instanceof Rect) {
                        bounds = (Rect) rect;
                    }
                    String s = String.valueOf(state);
                    newPosture = s.contains("HALF") ? DevicePosture.POSTURE_HALF_OPENED
                            : DevicePosture.POSTURE_FLAT;
                    String o = String.valueOf(orientation);
                    if (o.contains("VERTICAL")) {
                        newOrientation = DevicePosture.FOLD_ORIENTATION_VERTICAL;
                    } else if (o.contains("HORIZONTAL")) {
                        newOrientation = DevicePosture.FOLD_ORIENTATION_HORIZONTAL;
                    }
                }
            }
        }
        if (foundFold) {
            // Once a folding feature is observed, the device is foldable for the rest of the session.
            foldable = true;
            posture = newPosture;
            foldOrientation = newOrientation;
            separating = newSeparating;
            if (bounds != null && newSeparating) {
                boundsX = bounds.left;
                boundsY = bounds.top;
                boundsW = bounds.width();
                boundsH = bounds.height();
                hasBounds = true;
            } else {
                hasBounds = false;
            }
        } else if (foldable) {
            // A known foldable currently reporting no folding feature is open and flat.
            posture = DevicePosture.POSTURE_FLAT;
            foldOrientation = DevicePosture.FOLD_ORIENTATION_NONE;
            separating = false;
            hasBounds = false;
        }
        Display.getInstance().postureChanged();
    }
}
