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

/// A value at a fraction of an animation, with the interpolator used on
/// the way to it from the previous keyframe.
public abstract class Keyframe implements Cloneable {

    boolean mValueWasSetOnStart;
    float mFraction;
    Class mValueType;
    private TimeInterpolator mInterpolator;
    boolean mHasValue;

    public static Keyframe ofInt(float fraction, int value) {
        return new IntKeyframe(fraction, value);
    }

    public static Keyframe ofInt(float fraction) {
        return new IntKeyframe(fraction);
    }

    public static Keyframe ofFloat(float fraction, float value) {
        return new FloatKeyframe(fraction, value);
    }

    public static Keyframe ofFloat(float fraction) {
        return new FloatKeyframe(fraction);
    }

    public static Keyframe ofObject(float fraction, Object value) {
        return new ObjectKeyframe(fraction, value);
    }

    public static Keyframe ofObject(float fraction) {
        return new ObjectKeyframe(fraction, null);
    }

    public boolean hasValue() {
        return mHasValue;
    }

    boolean valueWasSetOnStart() {
        return mValueWasSetOnStart;
    }

    void setValueWasSetOnStart(boolean valueWasSetOnStart) {
        mValueWasSetOnStart = valueWasSetOnStart;
    }

    public abstract Object getValue();

    public abstract void setValue(Object value);

    public float getFraction() {
        return mFraction;
    }

    public void setFraction(float fraction) {
        mFraction = fraction;
    }

    public TimeInterpolator getInterpolator() {
        return mInterpolator;
    }

    public void setInterpolator(TimeInterpolator interpolator) {
        mInterpolator = interpolator;
    }

    public Class getType() {
        return mValueType;
    }

    @Override
    public abstract Keyframe clone();

    static class ObjectKeyframe extends Keyframe {
        Object mValue;

        ObjectKeyframe(float fraction, Object value) {
            mFraction = fraction;
            mValue = value;
            mHasValue = value != null;
            mValueType = mHasValue ? value.getClass() : Object.class;
        }

        @Override
        public Object getValue() {
            return mValue;
        }

        @Override
        public void setValue(Object value) {
            mValue = value;
            mHasValue = value != null;
        }

        @Override
        public ObjectKeyframe clone() {
            ObjectKeyframe kfClone = new ObjectKeyframe(getFraction(), hasValue() ? mValue : null);
            kfClone.mValueWasSetOnStart = mValueWasSetOnStart;
            kfClone.setInterpolator(getInterpolator());
            return kfClone;
        }
    }

    static class IntKeyframe extends Keyframe {
        int mValue;

        IntKeyframe(float fraction, int value) {
            mFraction = fraction;
            mValue = value;
            mValueType = Integer.class;
            mHasValue = true;
        }

        IntKeyframe(float fraction) {
            mFraction = fraction;
            mValueType = Integer.class;
        }

        public int getIntValue() {
            return mValue;
        }

        @Override
        public Object getValue() {
            return Integer.valueOf(mValue);
        }

        @Override
        public void setValue(Object value) {
            if (value instanceof Number) {
                mValue = ((Number) value).intValue();
                mHasValue = true;
            }
        }

        @Override
        public IntKeyframe clone() {
            IntKeyframe kfClone = mHasValue ? new IntKeyframe(getFraction(), mValue) : new IntKeyframe(getFraction());
            kfClone.setInterpolator(getInterpolator());
            kfClone.mValueWasSetOnStart = mValueWasSetOnStart;
            return kfClone;
        }
    }

    static class FloatKeyframe extends Keyframe {
        float mValue;

        FloatKeyframe(float fraction, float value) {
            mFraction = fraction;
            mValue = value;
            mValueType = Float.class;
            mHasValue = true;
        }

        FloatKeyframe(float fraction) {
            mFraction = fraction;
            mValueType = Float.class;
        }

        public float getFloatValue() {
            return mValue;
        }

        @Override
        public Object getValue() {
            return Float.valueOf(mValue);
        }

        @Override
        public void setValue(Object value) {
            if (value instanceof Number) {
                mValue = ((Number) value).floatValue();
                mHasValue = true;
            }
        }

        @Override
        public FloatKeyframe clone() {
            FloatKeyframe kfClone = mHasValue ? new FloatKeyframe(getFraction(), mValue)
                    : new FloatKeyframe(getFraction());
            kfClone.setInterpolator(getInterpolator());
            kfClone.mValueWasSetOnStart = mValueWasSetOnStart;
            return kfClone;
        }
    }
}
