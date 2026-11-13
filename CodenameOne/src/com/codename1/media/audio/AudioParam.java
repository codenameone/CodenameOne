/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.media.audio;

import com.codename1.util.MathUtil;
import java.util.ArrayList;

/// A value of an [AudioNode] that can be scheduled ahead of time and modulated
/// by other nodes, the Web Audio `AudioParam`.
///
/// The value at any moment is the automation timeline (or the default value
/// when nothing is scheduled), plus the sum of every node output connected to
/// the parameter with [AudioNode#connect(AudioParam)], clamped to
/// [#getMinValue()] and [#getMaxValue()]. Scheduling methods return the
/// parameter so calls can be chained:
///
/// ```java
/// gain.getGain().setValueAtTime(0, now)
///         .linearRampToValueAtTime(1, now + 0.05)
///         .setTargetAtTime(0.2, now + 0.05, 0.1);
/// ```
///
/// Times are in the context's time base, [BaseAudioContext#getCurrentTime()].
/// Every method may be called from any thread; changes reach the renderer at
/// its next render quantum.
public final class AudioParam {
    static final int SET = 0;
    static final int LINEAR = 1;
    static final int EXPONENTIAL = 2;
    static final int TARGET = 3;
    static final int CURVE = 4;

    private static final Event[] NO_EVENTS = new Event[0];

    private final BaseAudioContext context;
    private final String name;
    private final float defaultValue;
    private final float minValue;
    private final float maxValue;
    private final boolean rateFixed;
    private AutomationRate automationRate;
    private Event[] events = NO_EVENTS;

    // ---- render side ----
    Event[] rEvents = NO_EVENTS;
    private int rCursor = -1;
    boolean rKRate;
    final ArrayList<Connection> rInputs = new ArrayList<Connection>();
    private final float[] rValues = new float[BaseAudioContext.RENDER_QUANTUM];
    private final AudioBus rMono = new AudioBus(1);
    private long rQuantum = -1;
    private boolean rConstant;

    AudioParam(BaseAudioContext context, String name, float defaultValue, float minValue,
               float maxValue, AutomationRate rate, boolean rateFixed) {
        this.context = context;
        this.name = name;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.automationRate = rate;
        this.rateFixed = rateFixed;
        this.rKRate = rate == AutomationRate.K_RATE;
    }

    /// The value used when nothing is scheduled.
    ///
    /// #### Returns
    ///
    /// the default value
    public float getDefaultValue() {
        return defaultValue;
    }

    /// The lowest value the parameter can take.
    ///
    /// #### Returns
    ///
    /// the nominal minimum
    public float getMinValue() {
        return minValue;
    }

    /// The highest value the parameter can take.
    ///
    /// #### Returns
    ///
    /// the nominal maximum
    public float getMaxValue() {
        return maxValue;
    }

    /// The automation rate.
    ///
    /// #### Returns
    ///
    /// whether the value is computed per sample or per render quantum
    public AutomationRate getAutomationRate() {
        return automationRate;
    }

    /// Sets the automation rate.
    ///
    /// #### Parameters
    ///
    /// - `rate`: the new rate
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: for a parameter whose rate the specification
    ///   fixes, such as `AudioBufferSourceNode.playbackRate`
    public void setAutomationRate(AutomationRate rate) {
        if (rate == null) {
            throw new IllegalArgumentException("rate is null");
        }
        if (rateFixed && rate != automationRate) {
            throw new IllegalStateException(name + " has a fixed automation rate of " + automationRate);
        }
        automationRate = rate;
        final boolean k = rate == AutomationRate.K_RATE;
        context.post(new Runnable() {
            @Override
            public void run() {
                rKRate = k;
            }
        });
    }

    /// The intrinsic value at the context's current time: the automation
    /// timeline, without the contribution of connected nodes.
    ///
    /// #### Returns
    ///
    /// the value
    public float getValue() {
        return clamp(evaluate(events, events.length, context.getCurrentTime(), defaultValue));
    }

    /// Sets the value now, the same as
    /// `setValueAtTime(value, context.getCurrentTime())`.
    ///
    /// #### Parameters
    ///
    /// - `value`: the new value
    public void setValue(float value) {
        setValueAtTime(value, context.getCurrentTime());
    }

    /// Changes the value at `startTime`.
    ///
    /// #### Parameters
    ///
    /// - `value`: the new value
    ///
    /// - `startTime`: when, in context seconds
    ///
    /// #### Returns
    ///
    /// this parameter
    public AudioParam setValueAtTime(float value, double startTime) {
        checkFinite(value);
        checkTime(startTime);
        insert(new Event(SET, startTime, value, 0, null, 0));
        return this;
    }

    /// Ramps linearly from the previous scheduled value to `value`, reaching it
    /// at `endTime`.
    ///
    /// #### Parameters
    ///
    /// - `value`: the value to reach
    ///
    /// - `endTime`: when it is reached
    ///
    /// #### Returns
    ///
    /// this parameter
    public AudioParam linearRampToValueAtTime(float value, double endTime) {
        checkFinite(value);
        checkTime(endTime);
        insert(new Event(LINEAR, endTime, value, 0, null, 0));
        return this;
    }

    /// Ramps exponentially from the previous scheduled value to `value`,
    /// reaching it at `endTime`. If the previous value is 0 or of the other
    /// sign the value holds until `endTime`, as the specification requires.
    ///
    /// #### Parameters
    ///
    /// - `value`: the value to reach; must not be 0
    ///
    /// - `endTime`: when it is reached
    ///
    /// #### Returns
    ///
    /// this parameter
    public AudioParam exponentialRampToValueAtTime(float value, double endTime) {
        checkFinite(value);
        if (value == 0f) {
            throw new IllegalArgumentException("An exponential ramp cannot reach 0");
        }
        checkTime(endTime);
        insert(new Event(EXPONENTIAL, endTime, value, 0, null, 0));
        return this;
    }

    /// Starts an exponential approach to `target` at `startTime`. After
    /// `timeConstant` seconds about 63% of the distance has been covered.
    ///
    /// #### Parameters
    ///
    /// - `target`: the value approached
    ///
    /// - `startTime`: when the approach starts
    ///
    /// - `timeConstant`: the time constant in seconds, 0 or more; 0 jumps
    ///
    /// #### Returns
    ///
    /// this parameter
    public AudioParam setTargetAtTime(float target, double startTime, double timeConstant) {
        checkFinite(target);
        checkTime(startTime);
        if (!BaseAudioContext.nonNegativeFinite(timeConstant)) {
            throw new IllegalArgumentException("timeConstant must be 0 or more: " + timeConstant);
        }
        insert(new Event(TARGET, startTime, target, timeConstant, null, 0));
        return this;
    }

    /// Follows `values` from `startTime` for `duration` seconds, linearly
    /// interpolating between them, then holds the last one.
    ///
    /// #### Parameters
    ///
    /// - `values`: at least two values; copied
    ///
    /// - `startTime`: when the curve starts
    ///
    /// - `duration`: how long it lasts, more than 0
    ///
    /// #### Returns
    ///
    /// this parameter
    public AudioParam setValueCurveAtTime(float[] values, double startTime, double duration) {
        if (values == null || values.length < 2) {
            throw new IllegalArgumentException("A value curve needs at least two values");
        }
        for (float v : values) {
            checkFinite(v);
        }
        checkTime(startTime);
        if (!BaseAudioContext.positiveFinite(duration)) {
            throw new IllegalArgumentException("duration must be more than 0: " + duration);
        }
        float[] copy = new float[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        insert(new Event(CURVE, startTime, copy[copy.length - 1], 0, copy, duration));
        return this;
    }

    /// Removes every event scheduled at or after `cancelTime`.
    ///
    /// #### Parameters
    ///
    /// - `cancelTime`: the first time to clear
    ///
    /// #### Returns
    ///
    /// this parameter
    public AudioParam cancelScheduledValues(double cancelTime) {
        checkTime(cancelTime);
        Event[] ev = events;
        int n = 0;
        while (n < ev.length && ev[n].time < cancelTime) {
            n++;
        }
        Event[] next = new Event[n];
        System.arraycopy(ev, 0, next, 0, n);
        publish(next);
        return this;
    }

    /// Like [#cancelScheduledValues(double)], but the value the timeline would
    /// have had at `cancelTime` is held from then on, so a ramp in progress
    /// stops where it was rather than jumping back.
    ///
    /// #### Parameters
    ///
    /// - `cancelTime`: when to hold
    ///
    /// #### Returns
    ///
    /// this parameter
    public AudioParam cancelAndHoldAtTime(double cancelTime) {
        checkTime(cancelTime);
        Event[] ev = events;
        float held = evaluate(ev, ev.length, cancelTime, defaultValue);
        int n = 0;
        while (n < ev.length && ev[n].time <= cancelTime) {
            n++;
        }
        ArrayList<Event> keep = new ArrayList<Event>();
        for (int i = 0; i < n; i++) {
            Event e = ev[i];
            if (e.type == CURVE && e.time + e.duration > cancelTime) {
                break;
            }
            keep.add(e);
        }
        if (n < ev.length && (ev[n].type == LINEAR || ev[n].type == EXPONENTIAL)
                && ev[n].t0 < cancelTime) {
            keep.add(new Event(ev[n].type, cancelTime, held, 0, null, 0));
        } else {
            keep.add(new Event(SET, cancelTime, held, 0, null, 0));
        }
        publish(keep.toArray(new Event[keep.size()]));
        return this;
    }

    // ---------------------------------------------------------------- control

    private void insert(Event e) {
        Event[] ev = events;
        for (Event o : ev) {
            if (o.type == CURVE && e.time > o.time && e.time < o.time + o.duration) {
                throw new IllegalStateException("An event cannot be scheduled inside a value curve");
            }
            if (e.type == CURVE && o.time > e.time && o.time < e.time + e.duration) {
                throw new IllegalStateException("A value curve cannot overlap a scheduled event");
            }
        }
        e.callTime = context.getCurrentTime();
        int pos = ev.length;
        while (pos > 0 && ev[pos - 1].time > e.time) {
            pos--;
        }
        Event[] next = new Event[ev.length + 1];
        System.arraycopy(ev, 0, next, 0, pos);
        next[pos] = e;
        System.arraycopy(ev, pos, next, pos + 1, ev.length - pos);
        publish(next);
    }

    /// Drops events the timeline can no longer reach, fills in each event's
    /// start state and hands the new array to the renderer.
    private void publish(Event[] ev) {
        if (ev == events) { // NOPMD CompareObjectsWithEquals - identity: the published array itself
            Event[] fresh = new Event[ev.length];
            System.arraycopy(ev, 0, fresh, 0, ev.length);
            ev = fresh;
        }
        double now = context.getCurrentTime();
        int last = -1;
        for (int i = 0; i < ev.length; i++) {
            if (ev[i].time <= now) {
                last = i;
            }
        }
        if (last > 0) {
            Event[] pruned = new Event[ev.length - last];
            System.arraycopy(ev, last, pruned, 0, pruned.length);
            ev = pruned;
        }
        for (int k = 0; k < ev.length; k++) {
            Event e = ev[k];
            // An event's start follows its predecessor, so it is worked out
            // again whenever one is inserted before it. Only the first
            // event keeps what it had: its predecessor may have been pruned
            // as past, and the start it gave is what the ramp still needs.
            if (k == 0 && e.t0Fixed) {
                continue;
            }
            if (e.t0Fixed) {
                // Published events are read by the render thread as they
                // are, so a start worked out again goes into a copy rather
                // than into the shared one, which never changes once out.
                Event c = new Event(e.type, e.time, e.value, e.timeConstant, e.curve, e.duration);
                c.callTime = e.callTime;
                ev[k] = c;
                e = c;
            }
            if (k == 0) {
                e.t0 = Math.min(e.callTime, e.time);
                e.v0 = defaultValue;
            } else {
                Event p = ev[k - 1];
                switch (p.type) {
                    case TARGET:
                        e.t0 = Math.max(p.time, Math.min(e.callTime, e.time));
                        e.v0 = evaluate(ev, k, e.t0, defaultValue);
                        break;
                    case CURVE:
                        e.t0 = p.time + p.duration;
                        e.v0 = p.curve[p.curve.length - 1];
                        break;
                    default:
                        e.t0 = p.time;
                        e.v0 = p.value;
                        break;
                }
            }
            if (e.type == TARGET) {
                e.v0 = evaluate(ev, k, e.time, defaultValue);
            }
            e.t0Fixed = true;
        }
        events = ev;
        final Event[] snapshot = ev;
        context.post(new Runnable() {
            @Override
            public void run() {
                rEvents = snapshot;
                rCursor = -1;
            }
        });
    }

    /// The timeline value at `t`, considering only the first `count` events.
    static float evaluate(Event[] ev, int count, double t, float def) {
        int i = -1;
        while (i + 1 < count && ev[i + 1].time <= t) {
            i++;
        }
        return valueAt(ev, count, i, t, def);
    }

    private static float valueAt(Event[] ev, int count, int i, double t, float def) {
        if (i + 1 < count) {
            Event n = ev[i + 1];
            if ((n.type == LINEAR || n.type == EXPONENTIAL) && t >= n.t0) {
                double span = n.time - n.t0;
                if (span <= 0) {
                    return n.value;
                }
                double f = (t - n.t0) / span;
                if (n.type == LINEAR) {
                    return (float) (n.v0 + (n.value - n.v0) * f);
                }
                if (n.v0 == 0f || (n.v0 < 0) != (n.value < 0)) {
                    return n.v0;
                }
                return (float) (n.v0 * MathUtil.pow(n.value / (double) n.v0, f));
            }
        }
        if (i < 0) {
            return def;
        }
        Event e = ev[i];
        switch (e.type) {
            case TARGET:
                if (e.timeConstant == 0) {
                    return e.value;
                }
                return (float) (e.value + (e.v0 - e.value)
                        * MathUtil.exp(-(t - e.time) / e.timeConstant));
            case CURVE: {
                double end = e.time + e.duration;
                float[] c = e.curve;
                if (t >= end) {
                    return c[c.length - 1];
                }
                double k = (c.length - 1) * (t - e.time) / e.duration;
                int idx = (int) Math.floor(k);
                if (idx >= c.length - 1) {
                    return c[c.length - 1];
                }
                double frac = k - idx;
                return (float) (c[idx] + (c[idx + 1] - c[idx]) * frac);
            }
            default:
                return e.value;
        }
    }

    private float clamp(float v) {
        if (v < minValue) {
            return minValue;
        }
        if (v > maxValue) {
            return maxValue;
        }
        return v;
    }

    private static void checkFinite(float v) {
        if (Float.isNaN(v) || Float.isInfinite(v)) {
            throw new IllegalArgumentException("Value must be finite: " + v);
        }
    }

    private static void checkTime(double t) {
        if (!BaseAudioContext.nonNegativeFinite(t)) {
            throw new IllegalArgumentException("Time must be 0 or more and finite: " + t);
        }
    }

    // ---------------------------------------------------------------- render

    /// The parameter's values for render quantum `q`. Render thread only.
    float[] compute(long q) {
        if (rQuantum == q) {
            return rValues;
        }
        rQuantum = q;
        float sr = context.getSampleRate();
        long frame0 = q * BaseAudioContext.RENDER_QUANTUM;
        // An event lands on the frame it names even when its float time is a
        // hair late; see BaseAudioContext.snapFrames.
        double tol = 1e-3 / sr;
        Event[] ev = rEvents;
        int n = ev.length;
        int cur = rCursor;
        if (cur >= n) {
            cur = -1;
        }
        float[] out = rValues;
        if (n == 0) {
            for (int i = 0; i < out.length; i++) {
                out[i] = defaultValue;
            }
        } else if (rKRate) {
            double t = frame0 / (double) sr;
            while (cur + 1 < n && ev[cur + 1].time <= t + tol) {
                cur++;
            }
            float v = valueAt(ev, n, cur, t, defaultValue);
            for (int i = 0; i < out.length; i++) {
                out[i] = v;
            }
        } else {
            for (int i = 0; i < out.length; i++) {
                double t = (frame0 + i) / (double) sr;
                while (cur + 1 < n && ev[cur + 1].time <= t + tol) {
                    cur++;
                }
                out[i] = valueAt(ev, n, cur, t, defaultValue);
            }
        }
        rCursor = cur;
        int inputs = rInputs.size();
        if (inputs > 0) {
            AudioBus mono = rMono;
            mono.setChannels(1);
            for (int c = 0; c < inputs; c++) {
                Connection con = rInputs.get(c);
                AudioBus.sumInto(con.source.pull(con.output, q), mono, ChannelInterpretation.SPEAKERS);
            }
            float[] m = mono.data[0];
            if (rKRate) {
                float add = m[0];
                for (int i = 0; i < out.length; i++) {
                    out[i] += add;
                }
            } else {
                for (int i = 0; i < out.length; i++) {
                    out[i] += m[i];
                }
            }
        }
        for (int i = 0; i < out.length; i++) {
            float v = out[i];
            if (Float.isNaN(v)) {
                v = defaultValue;
            }
            if (v < minValue) {
                v = minValue;
            } else if (v > maxValue) {
                v = maxValue;
            }
            out[i] = v;
        }
        boolean constant = true;
        float first = out[0];
        for (int i = 1; i < out.length; i++) {
            if (out[i] != first) {
                constant = false;
                break;
            }
        }
        rConstant = constant;
        return out;
    }

    /// Whether every value of the last computed quantum is the same.
    boolean isConstant() {
        return rConstant;
    }

    static final class Event {
        final int type;
        final double time;
        final float value;
        final double timeConstant;
        final float[] curve;
        final double duration;
        double callTime;
        double t0;
        float v0;
        boolean t0Fixed;

        Event(int type, double time, float value, double timeConstant, float[] curve, double duration) {
            this.type = type;
            this.time = time;
            this.value = value;
            this.timeConstant = timeConstant;
            this.curve = curve;
            this.duration = duration;
        }
    }
}
