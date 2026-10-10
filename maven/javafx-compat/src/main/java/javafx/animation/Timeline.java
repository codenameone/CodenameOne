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

import java.util.ArrayList;

import javafx.beans.value.WritableBooleanValue;
import javafx.beans.value.WritableDoubleValue;
import javafx.beans.value.WritableFloatValue;
import javafx.beans.value.WritableIntegerValue;
import javafx.beans.value.WritableLongValue;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

/// An animation described by key frames: at the time of each key frame
/// its targets have the values it names, and in between they are
/// interpolated.
///
/// A target with a key frame at time zero starts from that value. One
/// without starts from the value it has when the timeline is played,
/// which is read once, at that moment.
///
/// The handler of a key frame runs when the playhead passes the key
/// frame, once per cycle and in either direction. A frame that takes
/// the playhead past several key frames runs all their handlers, in the
/// order the playhead passed them, and each sees the targets as they
/// are at its own key frame.
///
/// The cycle lasts until the last key frame. The key frames are read
/// when the timeline starts; changing them while it runs takes effect
/// the next time it is played.
///
/// Differences from JavaFX: a timeline whose last key frame is at time
/// zero is played like any other, so its values are written, its
/// handlers run and it finishes, all on the next frame, where JavaFX
/// only runs the finished handler from inside `play()`. A frame rate
/// cannot be asked for; the timeline follows the display.
public final class Timeline extends Animation {

    /// One target and its values over the cycle.
    private static final class Track {
        WritableValue<?> target;
        Object startValue;
        final ArrayList<KeyValue> values = new ArrayList<KeyValue>();
        final ArrayList<Double> times = new ArrayList<Double>();
    }

    private final ObservableList<KeyFrame> keyFrames = FXCollections.observableArrayList();
    private KeyFrame[] frames = new KeyFrame[0];
    private double[] frameTimes = new double[0];
    private final ArrayList<Track> tracks = new ArrayList<Track>();

    /// Creates a timeline of key frames.
    public Timeline(KeyFrame... keyFrames) {
        this.keyFrames.addListener(new ListChangeListener<KeyFrame>() {
            @Override
            public void onChanged(Change<? extends KeyFrame> change) {
                keyFramesChanged(change);
            }
        });
        if (keyFrames != null) {
            this.keyFrames.setAll(keyFrames);
        }
    }

    /// Creates an empty timeline.
    public Timeline() {
        this((KeyFrame[]) null);
    }

    /// Returns the key frames. A key frame with a name is also a cue
    /// point of the timeline.
    public final ObservableList<KeyFrame> getKeyFrames() {
        return keyFrames;
    }

    private void keyFramesChanged(ListChangeListener.Change<? extends KeyFrame> change) {
        while (change.next()) {
            for (KeyFrame removed : change.getRemoved()) {
                String name = removed.getName();
                if (name != null) {
                    getCuePoints().remove(name);
                }
            }
            for (KeyFrame added : change.getAddedSubList()) {
                String name = added.getName();
                if (name != null) {
                    getCuePoints().put(name, added.getTime());
                }
            }
        }
        syncDuration();
    }

    @Override
    void syncDuration() {
        Duration longest = Duration.ZERO;
        for (int i = 0; i < keyFrames.size(); i++) {
            KeyFrame frame = keyFrames.get(i);
            if (frame != null && frame.getTime().greaterThan(longest)) {
                longest = frame.getTime();
            }
        }
        setCycleDuration(longest);
    }

    private Track track(WritableValue<?> target) {
        for (int i = 0; i < tracks.size(); i++) {
            Track t = tracks.get(i);
            if (t.target == target) {
                return t;
            }
        }
        Track t = new Track();
        t.target = target;
        t.startValue = target.getValue();
        tracks.add(t);
        return t;
    }

    @Override
    void doStart(boolean capture) {
        if (!capture) {
            return;
        }
        // Sorted by time; key frames of the same time keep their order.
        ArrayList<KeyFrame> sorted = new ArrayList<KeyFrame>();
        for (int i = 0; i < keyFrames.size(); i++) {
            KeyFrame frame = keyFrames.get(i);
            if (frame == null) {
                continue;
            }
            int at = sorted.size();
            while (at > 0 && sorted.get(at - 1).getTime().greaterThan(frame.getTime())) {
                at--;
            }
            sorted.add(at, frame);
        }
        frames = sorted.toArray(new KeyFrame[sorted.size()]);
        frameTimes = new double[frames.length];
        tracks.clear();
        for (int i = 0; i < frames.length; i++) {
            double time = frames[i].getTime().toMillis();
            frameTimes[i] = time;
            for (KeyValue value : frames[i].getValues()) {
                Track t = track(value.getTarget());
                int last = t.values.size() - 1;
                if (last >= 0 && Double.compare(t.times.get(last).doubleValue(), time) == 0) {
                    // Two values for one target at one time: the later wins.
                    t.values.set(last, value);
                } else {
                    t.values.add(value);
                    t.times.add(Double.valueOf(time));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void write(WritableValue<?> target, Object from, KeyValue to, double fraction) {
        Interpolator interpolator = to.getInterpolator();
        Object end = to.getEndValue();
        boolean numbers = from instanceof Number && end instanceof Number;
        if (numbers && target instanceof WritableDoubleValue) {
            ((WritableDoubleValue) target).set(interpolator.interpolate(((Number) from).doubleValue(),
                    ((Number) end).doubleValue(), fraction));
        } else if (numbers && target instanceof WritableFloatValue) {
            ((WritableFloatValue) target).set((float) interpolator.interpolate(((Number) from).doubleValue(),
                    ((Number) end).doubleValue(), fraction));
        } else if (numbers && target instanceof WritableIntegerValue) {
            ((WritableIntegerValue) target).set(interpolator.interpolate(((Number) from).intValue(),
                    ((Number) end).intValue(), fraction));
        } else if (numbers && target instanceof WritableLongValue) {
            ((WritableLongValue) target).set(interpolator.interpolate(((Number) from).longValue(),
                    ((Number) end).longValue(), fraction));
        } else if (from instanceof Boolean && end instanceof Boolean && target instanceof WritableBooleanValue) {
            ((WritableBooleanValue) target).set(interpolator.interpolate(((Boolean) from).booleanValue(),
                    ((Boolean) end).booleanValue(), fraction));
        } else {
            ((WritableValue<Object>) target).setValue(interpolator.interpolate(from, end, fraction));
        }
    }

    /// Writes every target as it is at a time of the cycle.
    private void apply(double time) {
        for (int i = 0; i < tracks.size(); i++) {
            Track t = tracks.get(i);
            int count = t.values.size();
            int next = 0;
            while (next < count && t.times.get(next).doubleValue() <= time) {
                next++;
            }
            if (next == count) {
                KeyValue last = t.values.get(count - 1);
                write(t.target, last.getEndValue(), last, 1.0);
                continue;
            }
            double leftTime = next == 0 ? 0 : t.times.get(next - 1).doubleValue();
            Object leftValue = next == 0 ? t.startValue : t.values.get(next - 1).getEndValue();
            double length = t.times.get(next).doubleValue() - leftTime;
            double fraction = length <= 0 ? 1.0 : (time - leftTime) / length;
            write(t.target, leftValue, t.values.get(next), fraction < 0 ? 0 : fraction);
        }
    }

    private boolean visit(int index, Animation top, int started) {
        EventHandler<ActionEvent> handler = frames[index].getOnFinished();
        if (handler != null) {
            apply(frameTimes[index]);
            handler.handle(new ActionEvent(frames[index], null));
        }
        return top.token == started;
    }

    @Override
    void doPlayTo(double from, double to, boolean forward, boolean atStart) {
        Animation top = root();
        int started = top.token;
        if (forward) {
            for (int i = 0; i < frames.length; i++) {
                double time = frameTimes[i];
                boolean passed = time > from && time <= to;
                if ((passed || atStart && Double.compare(time, from) == 0) && !visit(i, top, started)) {
                    return;
                }
            }
        } else {
            for (int i = frames.length - 1; i >= 0; i--) {
                double time = frameTimes[i];
                boolean passed = time < from && time >= to;
                if ((passed || atStart && Double.compare(time, from) == 0) && !visit(i, top, started)) {
                    return;
                }
            }
        }
        apply(to);
    }

    @Override
    void doJumpTo(double time) {
        apply(time);
    }
}
