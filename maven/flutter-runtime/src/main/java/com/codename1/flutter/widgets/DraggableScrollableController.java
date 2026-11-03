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
package com.codename1.flutter.widgets;

import com.codename1.flutter.animation.Curve;

import dart.async.Future;
import dart.core.Duration;
import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/// Flutter's {@code DraggableScrollableController}: reads and moves the size of
/// the {@link DraggableScrollableSheet} it is attached to, as a fraction of the
/// sheet's parent height.
public class DraggableScrollableController {

    /// The sheet a controller drives.
    interface Sheet {
        double size();

        double availableHeight();

        void moveTo(double size);
    }

    private Sheet sheet;
    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();
    private com.codename1.flutter.animation.AnimationController motion;

    public DraggableScrollableController() {
    }

    public boolean isAttached() {
        return sheet != null;
    }

    private Sheet attached() {
        if (sheet == null) {
            throw new dart.core.StateError("DraggableScrollableController is not attached to a sheet.");
        }
        return sheet;
    }

    public double size() {
        return attached().size();
    }

    public double pixels() {
        Sheet s = attached();
        return s.size() * s.availableHeight();
    }

    public double sizeToPixels(double size) {
        return size * attached().availableHeight();
    }

    public double pixelsToSize(double pixels) {
        double h = attached().availableHeight();
        return h <= 0 ? 0 : pixels / h;
    }

    public void jumpTo(double size) {
        stop();
        attached().moveTo(size);
    }

    public Future<Object> animateTo(final double size, Duration duration, final Curve curve) {
        stop();
        final Sheet s = attached();
        if (duration == null || duration.inMilliseconds() <= 0) {
            s.moveTo(size);
            return Future.value(null);
        }
        final double from = s.size();
        final com.codename1.flutter.animation.AnimationController run =
                new com.codename1.flutter.animation.AnimationController();
        run.duration(duration);
        run.addListener(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (motion == run && sheet == s) {
                    double v = run.value();
                    double t = curve == null ? v : curve.transform(v);
                    s.moveTo(from + (size - from) * t);
                }
            }
        });
        motion = run;
        return run.forward().then(new Funcs.Func1<Object, Object>() {
            @Override
            public Object call(Object ignored) {
                if (motion == run) {
                    if (sheet == s) {
                        s.moveTo(size);
                    }
                    stop();
                }
                return null;
            }
        });
    }

    private void stop() {
        com.codename1.flutter.animation.AnimationController run = motion;
        motion = null;
        if (run != null) {
            run.dispose();
        }
    }

    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Funcs.VoidFunc0 listener) {
        dart.runtime.DartRuntime.removeEq(listeners, listener);
    }

    public void dispose() {
        stop();
        listeners.clear();
        sheet = null;
    }

    void attach(Sheet s) {
        this.sheet = s;
    }

    void detach(Sheet s) {
        if (sheet == s) {
            stop();
            sheet = null;
        }
    }

    void notifyListeners() {
        List<Funcs.VoidFunc0> copy = new ArrayList<Funcs.VoidFunc0>(listeners);
        for (int i = 0; i < copy.size(); i++) {
            copy.get(i).call();
        }
    }
}
