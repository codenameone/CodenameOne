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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

/// A point in time on a [Timeline]: the values its targets have reached
/// by then, and a handler that runs when the timeline passes it.
public final class KeyFrame {

    private final Duration time;
    private final Set<KeyValue> values;
    private final EventHandler<ActionEvent> onFinished;
    private final String name;

    /// Creates a key frame.
    public KeyFrame(Duration time, String name, EventHandler<ActionEvent> onFinished, Collection<KeyValue> values) {
        if (time == null) {
            throw new NullPointerException("The time has to be specified");
        }
        if (time.lessThan(Duration.ZERO) || time.isUnknown()) {
            throw new IllegalArgumentException("The time is invalid.");
        }
        this.time = time;
        this.name = name;
        this.onFinished = onFinished;
        LinkedHashSet<KeyValue> set = new LinkedHashSet<KeyValue>();
        if (values != null) {
            for (KeyValue value : values) {
                if (value != null) {
                    set.add(value);
                }
            }
        }
        this.values = Collections.unmodifiableSet(set);
    }

    /// Creates a key frame.
    public KeyFrame(Duration time, String name, EventHandler<ActionEvent> onFinished, KeyValue... values) {
        this(time, name, onFinished, list(values));
    }

    /// Creates a key frame without a name.
    public KeyFrame(Duration time, EventHandler<ActionEvent> onFinished, KeyValue... values) {
        this(time, null, onFinished, list(values));
    }

    /// Creates a key frame without a handler.
    public KeyFrame(Duration time, String name, KeyValue... values) {
        this(time, name, null, list(values));
    }

    /// Creates a key frame without a name or a handler.
    public KeyFrame(Duration time, KeyValue... values) {
        this(time, null, null, list(values));
    }

    private static Collection<KeyValue> list(KeyValue[] values) {
        LinkedHashSet<KeyValue> set = new LinkedHashSet<KeyValue>();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                set.add(values[i]);
            }
        }
        return set;
    }

    /// Returns the time of the key frame, from the start of a cycle.
    public Duration getTime() {
        return time;
    }

    /// Returns the values reached at the key frame; the set cannot be
    /// changed.
    public Set<KeyValue> getValues() {
        return values;
    }

    /// Returns the handler that runs when the timeline passes the key
    /// frame, or `null`.
    public EventHandler<ActionEvent> getOnFinished() {
        return onFinished;
    }

    /// Returns the name of the key frame, or `null`. A named key frame
    /// is a cue point of its timeline.
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "KeyFrame [time=" + time + ", values=" + values + ", onFinished=" + onFinished + ", name=" + name
                + "]";
    }
}
