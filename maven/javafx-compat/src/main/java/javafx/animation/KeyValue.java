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
package javafx.animation;

import javafx.beans.value.WritableValue;

/// The value a target has at a key frame, and the pacing with which it
/// gets there from the key frame before.
public final class KeyValue {

    private final WritableValue<?> target;
    private final Object endValue;
    private final Interpolator interpolator;

    /// Creates a key value.
    public <T> KeyValue(WritableValue<T> target, T endValue, Interpolator interpolator) {
        if (target == null) {
            throw new NullPointerException("The target has to be specified");
        }
        if (interpolator == null) {
            throw new NullPointerException("The interpolator has to be specified");
        }
        this.target = target;
        this.endValue = endValue;
        this.interpolator = interpolator;
    }

    /// Creates a key value that is reached at a constant speed.
    public <T> KeyValue(WritableValue<T> target, T endValue) {
        this(target, endValue, Interpolator.LINEAR);
    }

    /// Returns the target that is written.
    public WritableValue<?> getTarget() {
        return target;
    }

    /// Returns the value the target has at the key frame.
    public Object getEndValue() {
        return endValue;
    }

    /// Returns the pacing of the way to the value.
    public Interpolator getInterpolator() {
        return interpolator;
    }

    @Override
    public String toString() {
        return "KeyValue [target=" + target + ", endValue=" + endValue + ", interpolator=" + interpolator + "]";
    }
}
