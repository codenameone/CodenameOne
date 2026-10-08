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
package android.animation;

import android.util.Property;

import com.codename1.androidcompat.runtime.AnimatableProperties;

import java.util.ArrayList;

/// One animated property: its name (or [Property]), its keyframes and the
/// evaluator between them. Keyframe lookup follows AOSP's `KeyframeSet`.
public class PropertyValuesHolder implements Cloneable {

    String mPropertyName;
    protected Property mProperty;
    Class mValueType;
    Keyframe[] mKeyframes;
    private TypeEvaluator mEvaluator;
    private Object mAnimatedValue;

    private PropertyValuesHolder(String propertyName) {
        mPropertyName = propertyName;
    }

    private PropertyValuesHolder(Property property) {
        mProperty = property;
        if (property != null) {
            mPropertyName = property.getName();
        }
    }

    public static PropertyValuesHolder ofInt(String propertyName, int... values) {
        PropertyValuesHolder p = new PropertyValuesHolder(propertyName);
        p.setIntValues(values);
        return p;
    }

    public static PropertyValuesHolder ofInt(Property<?, Integer> property, int... values) {
        PropertyValuesHolder p = new PropertyValuesHolder(property);
        p.setIntValues(values);
        return p;
    }

    public static PropertyValuesHolder ofFloat(String propertyName, float... values) {
        PropertyValuesHolder p = new PropertyValuesHolder(propertyName);
        p.setFloatValues(values);
        return p;
    }

    public static PropertyValuesHolder ofFloat(Property<?, Float> property, float... values) {
        PropertyValuesHolder p = new PropertyValuesHolder(property);
        p.setFloatValues(values);
        return p;
    }

    public static PropertyValuesHolder ofObject(String propertyName, TypeEvaluator evaluator, Object... values) {
        PropertyValuesHolder p = new PropertyValuesHolder(propertyName);
        p.setObjectValues(values);
        p.setEvaluator(evaluator);
        return p;
    }

    public static <V> PropertyValuesHolder ofObject(Property property, TypeEvaluator<V> evaluator, V... values) {
        PropertyValuesHolder p = new PropertyValuesHolder(property);
        p.setObjectValues((Object[]) values);
        p.setEvaluator(evaluator);
        return p;
    }

    public static PropertyValuesHolder ofKeyframe(String propertyName, Keyframe... values) {
        PropertyValuesHolder p = new PropertyValuesHolder(propertyName);
        p.setKeyframes(values);
        return p;
    }

    public static PropertyValuesHolder ofKeyframe(Property property, Keyframe... values) {
        PropertyValuesHolder p = new PropertyValuesHolder(property);
        p.setKeyframes(values);
        return p;
    }

    public void setIntValues(int... values) {
        mValueType = Integer.class;
        int n = Math.max(values.length, 2);
        Keyframe[] k = new Keyframe[n];
        if (values.length == 1) {
            k[0] = Keyframe.ofInt(0f);
            k[1] = Keyframe.ofInt(1f, values[0]);
        } else {
            k[0] = Keyframe.ofInt(0f, values[0]);
            for (int i = 1; i < values.length; i++) {
                k[i] = Keyframe.ofInt((float) i / (values.length - 1), values[i]);
            }
        }
        mKeyframes = k;
    }

    public void setFloatValues(float... values) {
        mValueType = Float.class;
        int n = Math.max(values.length, 2);
        Keyframe[] k = new Keyframe[n];
        if (values.length == 1) {
            k[0] = Keyframe.ofFloat(0f);
            k[1] = Keyframe.ofFloat(1f, values[0]);
        } else {
            k[0] = Keyframe.ofFloat(0f, values[0]);
            for (int i = 1; i < values.length; i++) {
                k[i] = Keyframe.ofFloat((float) i / (values.length - 1), values[i]);
            }
        }
        mKeyframes = k;
    }

    public void setObjectValues(Object... values) {
        mValueType = values.length > 0 && values[0] != null ? values[0].getClass() : Object.class;
        int n = Math.max(values.length, 2);
        Keyframe[] k = new Keyframe[n];
        if (values.length == 1) {
            k[0] = Keyframe.ofObject(0f);
            k[1] = Keyframe.ofObject(1f, values[0]);
        } else {
            k[0] = Keyframe.ofObject(0f, values[0]);
            for (int i = 1; i < values.length; i++) {
                k[i] = Keyframe.ofObject((float) i / (values.length - 1), values[i]);
            }
        }
        mKeyframes = k;
    }

    public void setKeyframes(Keyframe... values) {
        int numKeyframes = values.length;
        Keyframe[] k = new Keyframe[Math.max(numKeyframes, 2)];
        mValueType = values[0].getType();
        for (int i = 0; i < numKeyframes; i++) {
            k[i] = values[i];
        }
        if (numKeyframes == 1) {
            k[1] = k[0];
            k[0] = values[0] instanceof Keyframe.IntKeyframe ? Keyframe.ofInt(0f)
                    : values[0] instanceof Keyframe.FloatKeyframe ? Keyframe.ofFloat(0f) : Keyframe.ofObject(0f);
        }
        mKeyframes = k;
    }

    public void setEvaluator(TypeEvaluator evaluator) {
        mEvaluator = evaluator;
    }

    public void setPropertyName(String propertyName) {
        mPropertyName = propertyName;
    }

    public String getPropertyName() {
        return mPropertyName;
    }

    public void setProperty(Property property) {
        mProperty = property;
    }

    /// Picks the default evaluator for the value type.
    void init() {
        if (mEvaluator == null) {
            if (mValueType == Integer.class) {
                mEvaluator = new IntEvaluator();
            } else if (mValueType == Float.class) {
                mEvaluator = new FloatEvaluator();
            }
        }
    }

    /// Fills the keyframes that have no value from the target's current
    /// value, as AOSP's setupSetterAndGetter does.
    void setupStartValues(Object target) {
        Object current = null;
        boolean read = false;
        for (Keyframe kf : mKeyframes) {
            if (!kf.hasValue() || kf.valueWasSetOnStart()) {
                if (!read) {
                    current = currentValue(target);
                    read = true;
                }
                if (current != null) {
                    kf.setValue(convert(current));
                    kf.setValueWasSetOnStart(true);
                }
            }
        }
    }

    void setupStartValue(Object target) {
        setupValue(target, mKeyframes[0]);
    }

    void setupEndValue(Object target) {
        setupValue(target, mKeyframes[mKeyframes.length - 1]);
    }

    private void setupValue(Object target, Keyframe kf) {
        Object current = currentValue(target);
        if (current != null) {
            kf.setValue(convert(current));
        }
    }

    private Object currentValue(Object target) {
        if (target == null) {
            return null;
        }
        if (mProperty != null) {
            return mProperty.get(target);
        }
        return mPropertyName == null ? null : AnimatableProperties.get(target, mPropertyName);
    }

    private Object convert(Object v) {
        if (v instanceof Number) {
            if (mValueType == Integer.class) {
                return Integer.valueOf(((Number) v).intValue());
            }
            if (mValueType == Float.class) {
                return Float.valueOf(((Number) v).floatValue());
            }
        }
        return v;
    }

    /// Applies the current animated value to `target`.
    @SuppressWarnings("unchecked")
    void setAnimatedValue(Object target) {
        if (target == null) {
            return;
        }
        Object value = getAnimatedValue();
        if (mProperty != null) {
            mProperty.set(target, value);
        } else if (mPropertyName != null) {
            AnimatableProperties.set(target, mPropertyName, value);
        }
    }

    void calculateValue(float fraction) {
        mAnimatedValue = getValue(fraction);
    }

    Object getAnimatedValue() {
        return mAnimatedValue;
    }

    private Object evaluate(float fraction, Object start, Object end) {
        if (mEvaluator != null) {
            return mEvaluator.evaluate(fraction, start, end);
        }
        // An object holder with no evaluator snaps, as Android would fail to.
        return fraction < 1f ? start : end;
    }

    private Object getValue(float fraction) {
        Keyframe[] k = mKeyframes;
        int n = k.length;
        Keyframe first = k[0];
        Keyframe last = k[n - 1];
        if (n == 2) {
            TimeInterpolator interpolator = last.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            return evaluate(fraction, first.getValue(), last.getValue());
        }
        if (fraction <= 0f) {
            Keyframe next = k[1];
            TimeInterpolator interpolator = next.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            float prevFraction = first.getFraction();
            float intervalFraction = (fraction - prevFraction) / (next.getFraction() - prevFraction);
            return evaluate(intervalFraction, first.getValue(), next.getValue());
        } else if (fraction >= 1f) {
            Keyframe prev = k[n - 2];
            TimeInterpolator interpolator = last.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            float prevFraction = prev.getFraction();
            float intervalFraction = (fraction - prevFraction) / (last.getFraction() - prevFraction);
            return evaluate(intervalFraction, prev.getValue(), last.getValue());
        }
        Keyframe prev = first;
        for (int i = 1; i < n; i++) {
            Keyframe next = k[i];
            if (fraction < next.getFraction()) {
                TimeInterpolator interpolator = next.getInterpolator();
                float prevFraction = prev.getFraction();
                float intervalFraction = (fraction - prevFraction) / (next.getFraction() - prevFraction);
                if (interpolator != null) {
                    intervalFraction = interpolator.getInterpolation(intervalFraction);
                }
                return evaluate(intervalFraction, prev.getValue(), next.getValue());
            }
            prev = next;
        }
        return last.getValue();
    }

    @Override
    public PropertyValuesHolder clone() {
        PropertyValuesHolder c = new PropertyValuesHolder(mPropertyName);
        c.mProperty = mProperty;
        c.mValueType = mValueType;
        c.mEvaluator = mEvaluator;
        c.mAnimatedValue = mAnimatedValue;
        Keyframe[] k = new Keyframe[mKeyframes.length];
        for (int i = 0; i < k.length; i++) {
            k[i] = mKeyframes[i].clone();
        }
        c.mKeyframes = k;
        return c;
    }

    @Override
    public String toString() {
        ArrayList<Object> values = new ArrayList<Object>();
        for (Keyframe kf : mKeyframes) {
            values.add(kf.hasValue() ? kf.getValue() : "<unset>");
        }
        return mPropertyName + ": " + values;
    }
}
