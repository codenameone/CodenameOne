/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.scheduler;

import com.codename1.ui.Display;

import dart.core.Duration;
import dart.runtime.Funcs;

/**
 * The singleton that drives frame scheduling — Flutter's {@code SchedulerBinding}.
 * new_gallery reaches it through {@code SchedulerBinding.instance} to register
 * post-frame callbacks (e.g. feature-discovery overlays that must measure a
 * widget after its first layout).
 *
 * <p>Callbacks are dispatched on the Codename One EDT after the current event
 * loop via {@link Display#callSerially}, which is the closest analogue to
 * "after the current frame". The {@code FrameTiming} argument Flutter passes is
 * approximated with a zero {@link Duration}.</p>
 */
public final class SchedulerBinding {

    /**
     * Dart's {@code SchedulerBinding.instance}. A Dart {@code static get}
     * accessor is emitted as a Java static field reference, so this is a field
     * rather than a method.
     */
    public static final SchedulerBinding instance = new SchedulerBinding();

    private int nextCallbackId = 1;

    private SchedulerBinding() {
    }

    /** Dart's {@code addPostFrameCallback}: run {@code callback} after this frame. */
    public void addPostFrameCallback(final Funcs.VoidFunc1<Object> callback) {
        dispatch(callback);
    }

    /**
     * Dart's {@code scheduleFrameCallback}: schedule a transient frame callback
     * and return its id. {@code rescheduling} is accepted for API shape.
     */
    public int scheduleFrameCallback(final Object callback, Boolean rescheduling) {
        dispatch(callback);
        return nextCallbackId++;
    }

    /** Dart's {@code scheduleFrame}: request a new frame. A no-op in this pass. */
    public void scheduleFrame() {
    }

    @SuppressWarnings("unchecked")
    private void dispatch(final Object callback) {
        if (callback == null) {
            return;
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (callback instanceof Funcs.VoidFunc1) {
                    ((Funcs.VoidFunc1<Object>) callback).call(Duration.zero);
                } else if (callback instanceof Runnable) {
                    ((Runnable) callback).run();
                }
            }
        };
        try {
            if (Display.isInitialized()) {
                Display.getInstance().callSerially(r);
            } else {
                r.run();
            }
        } catch (Throwable t) {
            // Best effort: a callback throwing must not abort scheduling.
        }
    }
}
