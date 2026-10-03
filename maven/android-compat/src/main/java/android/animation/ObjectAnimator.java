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

/// Animates a property of a target object. A property is named either by
/// an [Property] object or by name; names resolve without reflection to
/// the setters the framework views and drawables expose (see
/// `AnimatableProperties`), so an application class's own property needs
/// a `Property` here.
public final class ObjectAnimator extends ValueAnimator {

    private Object mTarget;
    private String mPropertyName;
    private Property mProperty;
    private boolean mAutoCancel;

    public ObjectAnimator() {
    }

    private ObjectAnimator(Object target, String propertyName) {
        setTarget(target);
        setPropertyName(propertyName);
    }

    private <T> ObjectAnimator(T target, Property<T, ?> property) {
        setTarget(target);
        setProperty(property);
    }

    public static ObjectAnimator ofInt(Object target, String propertyName, int... values) {
        ObjectAnimator anim = new ObjectAnimator(target, propertyName);
        anim.setIntValues(values);
        return anim;
    }

    public static <T> ObjectAnimator ofInt(T target, Property<T, Integer> property, int... values) {
        ObjectAnimator anim = new ObjectAnimator(target, property);
        anim.setIntValues(values);
        return anim;
    }

    public static ObjectAnimator ofArgb(Object target, String propertyName, int... values) {
        ObjectAnimator animator = ofInt(target, propertyName, values);
        animator.setEvaluator(ArgbEvaluator.getInstance());
        return animator;
    }

    public static <T> ObjectAnimator ofArgb(T target, Property<T, Integer> property, int... values) {
        ObjectAnimator animator = ofInt(target, property, values);
        animator.setEvaluator(ArgbEvaluator.getInstance());
        return animator;
    }

    public static ObjectAnimator ofFloat(Object target, String propertyName, float... values) {
        ObjectAnimator anim = new ObjectAnimator(target, propertyName);
        anim.setFloatValues(values);
        return anim;
    }

    public static <T> ObjectAnimator ofFloat(T target, Property<T, Float> property, float... values) {
        ObjectAnimator anim = new ObjectAnimator(target, property);
        anim.setFloatValues(values);
        return anim;
    }

    public static ObjectAnimator ofObject(Object target, String propertyName, TypeEvaluator evaluator,
                                          Object... values) {
        ObjectAnimator anim = new ObjectAnimator(target, propertyName);
        anim.setObjectValues(values);
        anim.setEvaluator(evaluator);
        return anim;
    }

    public static <T, V> ObjectAnimator ofObject(T target, Property<T, V> property, TypeEvaluator<V> evaluator,
                                                 V... values) {
        ObjectAnimator anim = new ObjectAnimator(target, property);
        anim.setObjectValues((Object[]) values);
        anim.setEvaluator(evaluator);
        return anim;
    }

    public static ObjectAnimator ofPropertyValuesHolder(Object target, PropertyValuesHolder... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setValues(values);
        return anim;
    }

    public void setPropertyName(String propertyName) {
        if (mValues != null) {
            PropertyValuesHolder valuesHolder = mValues[0];
            String oldName = valuesHolder.getPropertyName();
            valuesHolder.setPropertyName(propertyName);
            mValuesMap.remove(oldName);
            mValuesMap.put(propertyName, valuesHolder);
        }
        mPropertyName = propertyName;
        mInitialized = false;
    }

    public void setProperty(Property property) {
        if (mValues != null) {
            PropertyValuesHolder valuesHolder = mValues[0];
            String oldName = valuesHolder.getPropertyName();
            valuesHolder.setProperty(property);
            mValuesMap.remove(oldName);
            mValuesMap.put(mPropertyName, valuesHolder);
        }
        if (mProperty != null) {
            mPropertyName = property.getName();
        }
        mProperty = property;
        mInitialized = false;
    }

    public String getPropertyName() {
        String propertyName = null;
        if (mPropertyName != null) {
            propertyName = mPropertyName;
        } else if (mProperty != null) {
            propertyName = mProperty.getName();
        } else if (mValues != null && mValues.length > 0) {
            for (int i = 0; i < mValues.length; ++i) {
                if (i == 0) {
                    propertyName = "";
                } else {
                    propertyName += ",";
                }
                propertyName += mValues[i].getPropertyName();
            }
        }
        return propertyName;
    }

    @Override
    public void setIntValues(int... values) {
        if (mValues == null || mValues.length == 0) {
            if (mProperty != null) {
                setValues(PropertyValuesHolder.ofInt(mProperty, values));
            } else {
                setValues(PropertyValuesHolder.ofInt(mPropertyName, values));
            }
        } else {
            super.setIntValues(values);
        }
    }

    @Override
    public void setFloatValues(float... values) {
        if (mValues == null || mValues.length == 0) {
            if (mProperty != null) {
                setValues(PropertyValuesHolder.ofFloat(mProperty, values));
            } else {
                setValues(PropertyValuesHolder.ofFloat(mPropertyName, values));
            }
        } else {
            super.setFloatValues(values);
        }
    }

    @Override
    public void setObjectValues(Object... values) {
        if (mValues == null || mValues.length == 0) {
            if (mProperty != null) {
                setValues(PropertyValuesHolder.ofObject(mProperty, (TypeEvaluator) null, values));
            } else {
                setValues(PropertyValuesHolder.ofObject(mPropertyName, null, values));
            }
        } else {
            super.setObjectValues(values);
        }
    }

    public void setAutoCancel(boolean cancel) {
        mAutoCancel = cancel;
    }

    private boolean hasSameTargetAndProperties(Animator anim) {
        if (anim instanceof ObjectAnimator) {
            PropertyValuesHolder[] theirValues = ((ObjectAnimator) anim).getValues();
            if (((ObjectAnimator) anim).getTarget() == getTarget() && mValues != null && theirValues != null
                    && mValues.length == theirValues.length) {
                for (int i = 0; i < mValues.length; ++i) {
                    String a = mValues[i].getPropertyName();
                    String b = theirValues[i].getPropertyName();
                    if (a == null ? b != null : !a.equals(b)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static final java.util.ArrayList<ObjectAnimator> AUTO_CANCEL = new java.util.ArrayList<ObjectAnimator>();

    @Override
    public void start() {
        if (mAutoCancel) {
            for (ObjectAnimator other : new java.util.ArrayList<ObjectAnimator>(AUTO_CANCEL)) {
                if (other != this && other.isStarted() && other.hasSameTargetAndProperties(this)) {
                    other.cancel();
                }
            }
            AUTO_CANCEL.add(this);
        }
        super.start();
    }

    @Override
    void initAnimation() {
        if (!mInitialized) {
            Object target = getTarget();
            if (target != null && mValues != null) {
                for (PropertyValuesHolder v : mValues) {
                    v.setupStartValues(target);
                }
            }
            super.initAnimation();
        }
    }

    @Override
    public ObjectAnimator setDuration(long duration) {
        super.setDuration(duration);
        return this;
    }

    public Object getTarget() {
        return mTarget;
    }

    @Override
    public void setTarget(Object target) {
        if (mTarget != target) {
            if (isStarted()) {
                cancel();
            }
            mTarget = target;
            mInitialized = false;
        }
    }

    @Override
    public void setupStartValues() {
        initAnimation();
        Object target = getTarget();
        if (target != null && mValues != null) {
            for (PropertyValuesHolder v : mValues) {
                v.setupStartValue(target);
            }
        }
    }

    @Override
    public void setupEndValues() {
        initAnimation();
        Object target = getTarget();
        if (target != null && mValues != null) {
            for (PropertyValuesHolder v : mValues) {
                v.setupEndValue(target);
            }
        }
    }

    @Override
    void animateValue(float fraction) {
        Object target = getTarget();
        super.animateValue(fraction);
        if (target != null && mValues != null) {
            for (PropertyValuesHolder v : mValues) {
                v.setAnimatedValue(target);
            }
        }
        if (!isStarted()) {
            AUTO_CANCEL.remove(this);
        }
    }

    @Override
    public void end() {
        super.end();
        AUTO_CANCEL.remove(this);
    }

    @Override
    public void cancel() {
        super.cancel();
        AUTO_CANCEL.remove(this);
    }

    @Override
    public ObjectAnimator clone() {
        ObjectAnimator anim = new ObjectAnimator();
        anim.copyValueAnimatorFrom(this);
        anim.mTarget = mTarget;
        anim.mPropertyName = mPropertyName;
        anim.mProperty = mProperty;
        anim.mAutoCancel = mAutoCancel;
        return anim;
    }

    @Override
    public String toString() {
        return "ObjectAnimator@" + Integer.toHexString(hashCode()) + ", target " + getTarget() + " "
                + getPropertyName();
    }
}
