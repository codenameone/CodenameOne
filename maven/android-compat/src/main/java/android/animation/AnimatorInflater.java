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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.animation.AnimationUtils;

import com.codename1.androidcompat.runtime.CompatReport;
import com.codename1.androidcompat.runtime.CompiledAttributeSet;
import com.codename1.androidcompat.runtime.XmlNode;

import java.util.ArrayList;

/// Builds animators from compiled `res/animator` XML: `<set>`,
/// `<objectAnimator>`, `<animator>`, `<propertyValuesHolder>` and
/// `<keyframe>`, read the way AOSP's AnimatorInflater reads them.
public class AnimatorInflater {

    private static final int TOGETHER = 0;
    private static final int VALUE_TYPE_FLOAT = 0;
    private static final int VALUE_TYPE_INT = 1;
    private static final int VALUE_TYPE_PATH = 2;
    private static final int VALUE_TYPE_COLOR = 3;
    private static final int VALUE_TYPE_UNDEFINED = 4;

    public static Animator loadAnimator(Context context, int id) throws Resources.NotFoundException {
        XmlNode node;
        try {
            node = context.getResources().getXmlNode(id);
        } catch (Resources.NotFoundException e) {
            throw new Resources.NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(id), e);
        }
        return createAnimatorFromXml(context, node, null, 0);
    }

    private static Animator createAnimatorFromXml(Context c, XmlNode node, AnimatorSet parent, int sequenceOrdering) {
        Animator anim;
        String name = node.tag;
        AttributeSet attrs = new CompiledAttributeSet(node);
        boolean gotValues = false;
        if (name.equals("objectAnimator")) {
            anim = loadAnimator(c, attrs, new ObjectAnimator(), true);
        } else if (name.equals("animator")) {
            anim = loadAnimator(c, attrs, new ValueAnimator(), false);
        } else if (name.equals("set")) {
            AnimatorSet set = new AnimatorSet();
            TypedArray a = c.obtainStyledAttributes(attrs, android.R.styleable.AnimatorSet);
            int ordering = a.getInt(android.R.styleable.AnimatorSet_ordering, TOGETHER);
            a.recycle();
            ArrayList<Animator> children = new ArrayList<Animator>();
            for (XmlNode child : node.children) {
                children.add(createAnimatorFromXml(c, child, set, ordering));
            }
            Animator[] array = children.toArray(new Animator[children.size()]);
            if (ordering == TOGETHER) {
                set.playTogether(array);
            } else {
                set.playSequentially(array);
            }
            anim = set;
        } else if (name.equals("propertyValuesHolder")) {
            throw new RuntimeException("<propertyValuesHolder> belongs inside an <objectAnimator> or <animator>");
        } else {
            throw new RuntimeException("Unknown animator name: " + name);
        }
        if (anim instanceof ValueAnimator) {
            PropertyValuesHolder[] values = loadValues(c, node);
            if (values != null) {
                ((ValueAnimator) anim).setValues(values);
                gotValues = true;
            }
            if (!gotValues && ((ValueAnimator) anim).getValues() == null) {
                // AOSP leaves an animator with no values inert; so do we.
                ((ValueAnimator) anim).setFloatValues(0f, 0f);
            }
        }
        return anim;
    }

    private static PropertyValuesHolder[] loadValues(Context c, XmlNode node) {
        ArrayList<PropertyValuesHolder> values = null;
        for (XmlNode child : node.children) {
            if (!child.tag.equals("propertyValuesHolder")) {
                continue;
            }
            AttributeSet attrs = new CompiledAttributeSet(child);
            TypedArray a = c.obtainStyledAttributes(attrs, android.R.styleable.PropertyValuesHolder);
            String propertyName = a.getString(android.R.styleable.PropertyValuesHolder_propertyName);
            int valueType = a.getInt(android.R.styleable.PropertyValuesHolder_valueType, VALUE_TYPE_UNDEFINED);
            PropertyValuesHolder pvh = loadPvh(c, child, propertyName, valueType);
            if (pvh == null) {
                pvh = getPVH(a, valueType, android.R.styleable.PropertyValuesHolder_valueFrom,
                        android.R.styleable.PropertyValuesHolder_valueTo, propertyName);
            }
            a.recycle();
            if (pvh != null) {
                if (values == null) {
                    values = new ArrayList<PropertyValuesHolder>();
                }
                values.add(pvh);
            }
        }
        return values == null ? null : values.toArray(new PropertyValuesHolder[values.size()]);
    }

    /// A holder from `<keyframe>` children, or null when there are none.
    private static PropertyValuesHolder loadPvh(Context c, XmlNode node, String propertyName, int valueType) {
        ArrayList<Keyframe> keyframes = new ArrayList<Keyframe>();
        for (XmlNode child : node.children) {
            if (!child.tag.equals("keyframe")) {
                continue;
            }
            if (valueType == VALUE_TYPE_UNDEFINED) {
                valueType = inferValueTypeOfKeyframe(c, child);
            }
            Keyframe keyframe = loadKeyframe(c, child, valueType);
            if (keyframe != null) {
                keyframes.add(keyframe);
            }
        }
        int count = keyframes.size();
        if (count == 0) {
            return null;
        }
        Keyframe firstKeyframe = keyframes.get(0);
        Keyframe lastKeyframe = keyframes.get(count - 1);
        float endFraction = lastKeyframe.getFraction();
        if (endFraction < 1) {
            if (endFraction < 0) {
                lastKeyframe.setFraction(1);
            } else {
                keyframes.add(keyframes.size(), createNewKeyframe(lastKeyframe, 1));
                ++count;
            }
        }
        float startFraction = firstKeyframe.getFraction();
        if (startFraction != 0) {
            if (startFraction < 0) {
                firstKeyframe.setFraction(0);
            } else {
                keyframes.add(0, createNewKeyframe(firstKeyframe, 0));
                ++count;
            }
        }
        Keyframe[] keyframeArray = keyframes.toArray(new Keyframe[count]);
        for (int i = 0; i < count; ++i) {
            Keyframe keyframe = keyframeArray[i];
            if (keyframe.getFraction() < 0) {
                if (i == 0) {
                    keyframe.setFraction(0);
                } else if (i == count - 1) {
                    keyframe.setFraction(1);
                } else {
                    int startIndex = i;
                    int endIndex = i;
                    for (int j = startIndex + 1; j < count - 1; ++j) {
                        if (keyframeArray[j].getFraction() >= 0) {
                            break;
                        }
                        endIndex = j;
                    }
                    float gap = keyframeArray[endIndex + 1].getFraction() - keyframeArray[startIndex - 1].getFraction();
                    distributeKeyframes(keyframeArray, gap, startIndex, endIndex);
                }
            }
        }
        PropertyValuesHolder value = PropertyValuesHolder.ofKeyframe(propertyName, keyframeArray);
        if (valueType == VALUE_TYPE_COLOR) {
            value.setEvaluator(ArgbEvaluator.getInstance());
        }
        return value;
    }

    private static Keyframe createNewKeyframe(Keyframe sampleKeyframe, float fraction) {
        if (sampleKeyframe.getType() == Float.class) {
            return Keyframe.ofFloat(fraction);
        }
        if (sampleKeyframe.getType() == Integer.class) {
            return Keyframe.ofInt(fraction);
        }
        return Keyframe.ofObject(fraction);
    }

    private static void distributeKeyframes(Keyframe[] keyframes, float gap, int startIndex, int endIndex) {
        int count = endIndex - startIndex + 2;
        float increment = gap / count;
        for (int i = startIndex; i <= endIndex; ++i) {
            keyframes[i].setFraction(keyframes[i - 1].getFraction() + increment);
        }
    }

    private static int inferValueTypeOfKeyframe(Context c, XmlNode node) {
        TypedArray a = c.obtainStyledAttributes(new CompiledAttributeSet(node), android.R.styleable.Keyframe);
        TypedValue keyframeValue = a.peekValue(android.R.styleable.Keyframe_value);
        boolean hasValue = keyframeValue != null;
        int valueType = hasValue && isColorType(keyframeValue.type) ? VALUE_TYPE_COLOR : VALUE_TYPE_FLOAT;
        a.recycle();
        return valueType;
    }

    private static Keyframe loadKeyframe(Context c, XmlNode node, int valueType) {
        TypedArray a = c.obtainStyledAttributes(new CompiledAttributeSet(node), android.R.styleable.Keyframe);
        float fraction = a.getFloat(android.R.styleable.Keyframe_fraction, -1);
        TypedValue keyframeValue = a.peekValue(android.R.styleable.Keyframe_value);
        boolean hasValue = keyframeValue != null;
        if (valueType == VALUE_TYPE_UNDEFINED) {
            valueType = hasValue && isColorType(keyframeValue.type) ? VALUE_TYPE_COLOR : VALUE_TYPE_FLOAT;
        }
        Keyframe keyframe;
        if (hasValue) {
            switch (valueType) {
                case VALUE_TYPE_FLOAT:
                    keyframe = Keyframe.ofFloat(fraction, a.getFloat(android.R.styleable.Keyframe_value, 0));
                    break;
                case VALUE_TYPE_COLOR:
                    keyframe = Keyframe.ofInt(fraction, a.getColor(android.R.styleable.Keyframe_value, 0));
                    break;
                default:
                    keyframe = Keyframe.ofInt(fraction, a.getInt(android.R.styleable.Keyframe_value, 0));
                    break;
            }
        } else {
            keyframe = valueType == VALUE_TYPE_FLOAT ? Keyframe.ofFloat(fraction) : Keyframe.ofInt(fraction);
        }
        int resId = a.getResourceId(android.R.styleable.Keyframe_interpolator, 0);
        if (resId > 0) {
            keyframe.setInterpolator(AnimationUtils.loadInterpolator(c, resId));
        }
        a.recycle();
        return keyframe;
    }

    private static ValueAnimator loadAnimator(Context c, AttributeSet attrs, ValueAnimator anim, boolean object) {
        TypedArray arrayAnimator = c.obtainStyledAttributes(attrs, android.R.styleable.Animator);
        TypedArray arrayObjectAnimator = object
                ? c.obtainStyledAttributes(attrs, android.R.styleable.PropertyAnimator) : null;
        parseAnimatorFromTypeArray(c, anim, arrayAnimator, arrayObjectAnimator);
        int resId = arrayAnimator.getResourceId(android.R.styleable.Animator_interpolator, 0);
        if (resId > 0) {
            anim.setInterpolator(AnimationUtils.loadInterpolator(c, resId));
        }
        arrayAnimator.recycle();
        if (arrayObjectAnimator != null) {
            arrayObjectAnimator.recycle();
        }
        return anim;
    }

    private static void parseAnimatorFromTypeArray(Context c, ValueAnimator anim, TypedArray arrayAnimator,
                                                   TypedArray arrayObjectAnimator) {
        long duration = arrayAnimator.getInt(android.R.styleable.Animator_duration, 300);
        long startDelay = arrayAnimator.getInt(android.R.styleable.Animator_startOffset, 0);
        int valueType = arrayAnimator.getInt(android.R.styleable.Animator_valueType, VALUE_TYPE_UNDEFINED);
        if (valueType == VALUE_TYPE_UNDEFINED) {
            valueType = inferValueTypeFromValues(arrayAnimator, android.R.styleable.Animator_valueFrom,
                    android.R.styleable.Animator_valueTo);
        }
        PropertyValuesHolder pvh = getPVH(arrayAnimator, valueType, android.R.styleable.Animator_valueFrom,
                android.R.styleable.Animator_valueTo, "");
        if (pvh != null) {
            anim.setValues(pvh);
        }
        anim.setDuration(duration);
        anim.setStartDelay(startDelay);
        if (arrayAnimator.hasValue(android.R.styleable.Animator_repeatCount)) {
            anim.setRepeatCount(arrayAnimator.getInt(android.R.styleable.Animator_repeatCount, 0));
        }
        if (arrayAnimator.hasValue(android.R.styleable.Animator_repeatMode)) {
            anim.setRepeatMode(arrayAnimator.getInt(android.R.styleable.Animator_repeatMode, ValueAnimator.RESTART));
        }
        if (arrayObjectAnimator != null && anim instanceof ObjectAnimator) {
            ObjectAnimator oa = (ObjectAnimator) anim;
            if (arrayObjectAnimator.hasValue(android.R.styleable.PropertyAnimator_pathData)) {
                CompatReport.unsupported("AnimatorInflater", "objectAnimator pathData (motion along a path)");
            }
            String propertyName = arrayObjectAnimator.getString(android.R.styleable.PropertyAnimator_propertyName);
            if (propertyName != null) {
                oa.setPropertyName(propertyName);
            }
        }
    }

    private static int inferValueTypeFromValues(TypedArray styledAttributes, int valueFromId, int valueToId) {
        TypedValue tvFrom = styledAttributes.peekValue(valueFromId);
        boolean hasFrom = tvFrom != null;
        int fromType = hasFrom ? tvFrom.type : 0;
        TypedValue tvTo = styledAttributes.peekValue(valueToId);
        boolean hasTo = tvTo != null;
        int toType = hasTo ? tvTo.type : 0;
        if ((hasFrom && isColorType(fromType)) || (hasTo && isColorType(toType))) {
            return VALUE_TYPE_COLOR;
        }
        return VALUE_TYPE_FLOAT;
    }

    private static boolean isColorType(int type) {
        return type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT;
    }

    private static PropertyValuesHolder getPVH(TypedArray styledAttributes, int valueType, int valueFromId,
                                               int valueToId, String propertyName) {
        TypedValue tvFrom = styledAttributes.peekValue(valueFromId);
        boolean hasFrom = tvFrom != null;
        int fromType = hasFrom ? tvFrom.type : 0;
        TypedValue tvTo = styledAttributes.peekValue(valueToId);
        boolean hasTo = tvTo != null;
        int toType = hasTo ? tvTo.type : 0;
        if (valueType == VALUE_TYPE_UNDEFINED) {
            valueType = (hasFrom && isColorType(fromType)) || (hasTo && isColorType(toType))
                    ? VALUE_TYPE_COLOR : VALUE_TYPE_FLOAT;
        }
        if (valueType == VALUE_TYPE_PATH) {
            CompatReport.unsupported("AnimatorInflater", "valueType=\"pathType\" (path morphing)");
            return null;
        }
        if (!hasFrom && !hasTo) {
            return null;
        }
        PropertyValuesHolder returnValue;
        if (valueType == VALUE_TYPE_FLOAT) {
            float valueFrom;
            float valueTo;
            if (hasFrom) {
                valueFrom = fromType == TypedValue.TYPE_DIMENSION ? styledAttributes.getDimension(valueFromId, 0f)
                        : styledAttributes.getFloat(valueFromId, 0f);
                if (hasTo) {
                    valueTo = toType == TypedValue.TYPE_DIMENSION ? styledAttributes.getDimension(valueToId, 0f)
                            : styledAttributes.getFloat(valueToId, 0f);
                    returnValue = PropertyValuesHolder.ofFloat(propertyName, valueFrom, valueTo);
                } else {
                    returnValue = PropertyValuesHolder.ofFloat(propertyName, valueFrom);
                }
            } else {
                valueTo = toType == TypedValue.TYPE_DIMENSION ? styledAttributes.getDimension(valueToId, 0f)
                        : styledAttributes.getFloat(valueToId, 0f);
                returnValue = PropertyValuesHolder.ofFloat(propertyName, valueTo);
            }
        } else {
            int valueFrom;
            int valueTo;
            if (hasFrom) {
                valueFrom = intValue(styledAttributes, valueFromId, fromType);
                if (hasTo) {
                    valueTo = intValue(styledAttributes, valueToId, toType);
                    returnValue = PropertyValuesHolder.ofInt(propertyName, valueFrom, valueTo);
                } else {
                    returnValue = PropertyValuesHolder.ofInt(propertyName, valueFrom);
                }
            } else {
                valueTo = intValue(styledAttributes, valueToId, toType);
                returnValue = PropertyValuesHolder.ofInt(propertyName, valueTo);
            }
        }
        if (valueType == VALUE_TYPE_COLOR) {
            returnValue.setEvaluator(ArgbEvaluator.getInstance());
        }
        return returnValue;
    }

    private static int intValue(TypedArray a, int index, int type) {
        if (type == TypedValue.TYPE_DIMENSION) {
            return (int) a.getDimension(index, 0f);
        }
        if (isColorType(type)) {
            return a.getColor(index, 0);
        }
        return a.getInt(index, 0);
    }
}
