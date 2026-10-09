/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.constraintlayout.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.View;


import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;

/**
 * Defines non standard Attributes
 *
 * @hide
 */
public class ConstraintAttribute {
    private static final String TAG = "TransitionLayout";
    private boolean mMethod = false;
    String mName;
    private AttributeType mType;
    private int mIntegerValue;
    private float mFloatValue;
    private String mStringValue;
    boolean mBooleanValue;
    private int mColorValue;

    public enum AttributeType {
        INT_TYPE,
        FLOAT_TYPE,
        COLOR_TYPE,
        COLOR_DRAWABLE_TYPE,
        STRING_TYPE,
        BOOLEAN_TYPE,
        DIMENSION_TYPE,
        REFERENCE_TYPE
    }

    public String getName() {
        return mName;
    }

    public boolean isMethod() {
        return mMethod;
    }

    public int getIntegerValue() {
        return mIntegerValue;
    }

    public float getFloatValue() {
        return mFloatValue;
    }

    public String getStringValue() {
        return mStringValue;
    }

    public boolean isBooleanValue() {
        return mBooleanValue;
    }

    public int getColorValue() {
        return mColorValue;
    }

    public AttributeType getType() {
        return mType;
    }

    /**
     * Continuous types are interpolated they are fired only at
     * @return
     */
    public boolean isContinuous() {
       switch (mType) {
           case REFERENCE_TYPE:
           case BOOLEAN_TYPE:
           case STRING_TYPE:
               return false;
           default:
               return true;
       }
    }

    public void setFloatValue(float value) {
        mFloatValue = value;
    }

    public void setColorValue(int value) {
        mColorValue = value;
    }

    public void setIntValue(int value) {
        mIntegerValue = value;
    }

    public void setStringValue(String value) {
        mStringValue = value;
    }

    /**
     * The number of interpolation values that need to be interpolated
     * Typically 1 but 3 for colors.
     *
     * @return Typically 1 but 3 for colors.
     */
    public int numberOfInterpolatedValues() {
        switch (mType) {
            case COLOR_TYPE:
            case COLOR_DRAWABLE_TYPE:
                return 4;
            default:
                return 1;
        }
    }

    /**
     * Transforms value to a float for the purpose of interpolation
     *
     * @return interpolation value
     */
    public float getValueToInterpolate() {
        switch (mType) {
            case INT_TYPE:
                return mIntegerValue;
            case FLOAT_TYPE:
                return mFloatValue;
            case COLOR_TYPE:
            case COLOR_DRAWABLE_TYPE:
                throw new RuntimeException("Color does not have a single color to interpolate");
            case STRING_TYPE:
                throw new RuntimeException("Cannot interpolate String");
            case BOOLEAN_TYPE:
                return mBooleanValue ? 1 : 0;
            case DIMENSION_TYPE:
                return mFloatValue;
        }
        return Float.NaN;
    }

    public void getValuesToInterpolate(float[] ret) {
        switch (mType) {
            case INT_TYPE:
                ret[0] = mIntegerValue;
                break;
            case FLOAT_TYPE:
                ret[0] = mFloatValue;
                break;
            case COLOR_DRAWABLE_TYPE:
            case COLOR_TYPE:
                int a = 0xFF & (mColorValue >> 24);
                int r = 0xFF & (mColorValue >> 16);
                int g = 0xFF & (mColorValue >> 8);
                int b = 0xFF & (mColorValue);
                float f_r = (float) com.codename1.util.MathUtil.pow(r / 255.0f, 2.2);
                float f_g = (float) com.codename1.util.MathUtil.pow(g / 255.0f, 2.2);
                float f_b = (float) com.codename1.util.MathUtil.pow(b / 255.0f, 2.2);
                ret[0] = f_r;
                ret[1] = f_g;
                ret[2] = f_b;
                ret[3] = a / 255f;
                break;
            case STRING_TYPE:
                throw new RuntimeException("Color does not have a single color to interpolate");
            case BOOLEAN_TYPE:
                ret[0] = mBooleanValue ? 1 : 0;
                break;
            case DIMENSION_TYPE:
                ret[0] = mFloatValue;
                break;
        }
    }

    public void setValue(float[] value) {
        switch (mType) {
            case REFERENCE_TYPE:
            case INT_TYPE:
                mIntegerValue = (int) value[0];
                break;
            case FLOAT_TYPE:
                mFloatValue = value[0];
                break;
            case COLOR_DRAWABLE_TYPE:
            case COLOR_TYPE:
                mColorValue = Color.HSVToColor(value);
                mColorValue = (mColorValue & 0xFFFFFF) | (clamp((int) (0xFF * value[3])) << 24);
                break;
            case STRING_TYPE:
                throw new RuntimeException("Color does not have a single color to interpolate");
            case BOOLEAN_TYPE:
                mBooleanValue = value[0] > 0.5;
                break;
            case DIMENSION_TYPE:
                mFloatValue = value[0];

        }
    }

    /**
     * test if the two attributes are different
     *
     * @param constraintAttribute
     * @return
     */
    public boolean diff(ConstraintAttribute constraintAttribute) {
        if (constraintAttribute == null || mType != constraintAttribute.mType) {
            return false;
        }
        switch (mType) {
            case INT_TYPE:
            case REFERENCE_TYPE:
                return mIntegerValue == constraintAttribute.mIntegerValue;
            case FLOAT_TYPE:
                return mFloatValue == constraintAttribute.mFloatValue;
            case COLOR_TYPE:
            case COLOR_DRAWABLE_TYPE:
                return mColorValue == constraintAttribute.mColorValue;
            case STRING_TYPE:
                return mIntegerValue == constraintAttribute.mIntegerValue;
            case BOOLEAN_TYPE:
                return mBooleanValue == constraintAttribute.mBooleanValue;
            case DIMENSION_TYPE:
                return mFloatValue == constraintAttribute.mFloatValue;
        }
        return false;
    }

    public ConstraintAttribute(String name, AttributeType attributeType) {
        mName = name;
        mType = attributeType;
    }

    public ConstraintAttribute(String name, AttributeType attributeType, Object value, boolean method) {
        mName = name;
        mType = attributeType;
        mMethod = method;
        setValue(value);
    }

    public ConstraintAttribute(ConstraintAttribute source, Object value) {
        mName = source.mName;
        mType = source.mType;
        setValue(value);

    }

    public void setValue(Object value) {
        switch (mType) {
            case REFERENCE_TYPE:
            case INT_TYPE:
                mIntegerValue = (Integer) value;
                break;
            case FLOAT_TYPE:
                mFloatValue = (Float) value;
                break;
            case COLOR_TYPE:
            case COLOR_DRAWABLE_TYPE:
                mColorValue = (Integer) value;
                break;
            case STRING_TYPE:
                mStringValue = (String) value;
                break;
            case BOOLEAN_TYPE:
                mBooleanValue = (Boolean) value;
                break;
            case DIMENSION_TYPE:
                mFloatValue = (Float) value;
                break;
        }
    }

    // Codename One: Android finds a custom attribute's setter or getter by
    // reflection. There is none on Codename One, so the standard View and
    // TextView properties are dispatched by name; any other name is reported
    // through CompatReport and skipped.

    public static HashMap<String, ConstraintAttribute> extractAttributes(
            HashMap<String, ConstraintAttribute> base, View view) {
        HashMap<String, ConstraintAttribute> ret = new HashMap<>();
        for (String name : base.keySet()) {
            ConstraintAttribute constraintAttribute = base.get(name);
            Object val = readProperty(view, name);
            if (val != null) {
                ret.put(name, new ConstraintAttribute(constraintAttribute, val));
            } else {
                unsupported(view, "getMap" + name);
            }
        }
        return ret;
    }

    public static void setAttributes(View view, HashMap<String, ConstraintAttribute> map) {
        for (String name : map.keySet()) {
            map.get(name).applyCustom(view);
        }
    }

    public void applyCustom(View view) {
        String methodName = mMethod ? mName : "set" + mName;
        if (!writeProperty(view, methodName)) {
            unsupported(view, methodName);
        }
    }

    private static void unsupported(View view, String methodName) {
        com.codename1.androidcompat.runtime.CompatReport.unsupported("ConstraintLayout custom attribute",
                view.getClass().getName() + "." + methodName);
    }

    private static Object readProperty(View view, String name) {
        if (name.equals("BackgroundColor")) {
            Drawable bg = view.getBackground();
            return bg instanceof ColorDrawable ? Integer.valueOf(((ColorDrawable) bg).getColor()) : null;
        }
        if (name.equals("Alpha")) {
            return Float.valueOf(view.getAlpha());
        }
        if (name.equals("Elevation")) {
            return Float.valueOf(view.getElevation());
        }
        if (name.equals("Rotation")) {
            return Float.valueOf(view.getRotation());
        }
        if (name.equals("RotationX")) {
            return Float.valueOf(view.getRotationX());
        }
        if (name.equals("RotationY")) {
            return Float.valueOf(view.getRotationY());
        }
        if (name.equals("ScaleX")) {
            return Float.valueOf(view.getScaleX());
        }
        if (name.equals("ScaleY")) {
            return Float.valueOf(view.getScaleY());
        }
        if (name.equals("TranslationX")) {
            return Float.valueOf(view.getTranslationX());
        }
        if (name.equals("TranslationY")) {
            return Float.valueOf(view.getTranslationY());
        }
        if (view instanceof android.widget.TextView) {
            android.widget.TextView tv = (android.widget.TextView) view;
            if (name.equals("TextColor")) {
                return Integer.valueOf(tv.getCurrentTextColor());
            }
            if (name.equals("TextSize")) {
                return Float.valueOf(tv.getTextSize());
            }
            if (name.equals("Text")) {
                return String.valueOf(tv.getText());
            }
        }
        return null;
    }

    private boolean writeProperty(View view, String methodName) {
        float f = mFloatValue;
        switch (mType) {
            case COLOR_DRAWABLE_TYPE:
                if (methodName.equals("setBackground") || methodName.equals("setBackgroundDrawable")) {
                    ColorDrawable drawable = new ColorDrawable();
                    drawable.setColor(mColorValue);
                    view.setBackground(drawable);
                    return true;
                }
                return false;
            case COLOR_TYPE:
                if (methodName.equals("setBackgroundColor")) {
                    view.setBackgroundColor(mColorValue);
                    return true;
                }
                if (methodName.equals("setTextColor") && view instanceof android.widget.TextView) {
                    ((android.widget.TextView) view).setTextColor(mColorValue);
                    return true;
                }
                return false;
            case STRING_TYPE:
                if (methodName.equals("setText") && view instanceof android.widget.TextView) {
                    ((android.widget.TextView) view).setText(mStringValue);
                    return true;
                }
                return false;
            case BOOLEAN_TYPE:
                if (methodName.equals("setEnabled")) {
                    view.setEnabled(mBooleanValue);
                } else if (methodName.equals("setClickable")) {
                    view.setClickable(mBooleanValue);
                } else if (methodName.equals("setSelected")) {
                    view.setSelected(mBooleanValue);
                } else if (methodName.equals("setActivated")) {
                    view.setActivated(mBooleanValue);
                } else {
                    return false;
                }
                return true;
            case INT_TYPE:
            case REFERENCE_TYPE:
                if (methodName.equals("setVisibility")) {
                    view.setVisibility(mIntegerValue);
                    return true;
                }
                if (methodName.equals("setBackgroundResource")) {
                    view.setBackgroundResource(mIntegerValue);
                    return true;
                }
                if (methodName.equals("setTextColor") && view instanceof android.widget.TextView) {
                    ((android.widget.TextView) view).setTextColor(mIntegerValue);
                    return true;
                }
                return false;
            case FLOAT_TYPE:
            case DIMENSION_TYPE:
                if (methodName.equals("setAlpha")) {
                    view.setAlpha(f);
                } else if (methodName.equals("setElevation")) {
                    view.setElevation(f);
                } else if (methodName.equals("setRotation")) {
                    view.setRotation(f);
                } else if (methodName.equals("setRotationX")) {
                    view.setRotationX(f);
                } else if (methodName.equals("setRotationY")) {
                    view.setRotationY(f);
                } else if (methodName.equals("setScaleX")) {
                    view.setScaleX(f);
                } else if (methodName.equals("setScaleY")) {
                    view.setScaleY(f);
                } else if (methodName.equals("setTranslationX")) {
                    view.setTranslationX(f);
                } else if (methodName.equals("setTranslationY")) {
                    view.setTranslationY(f);
                } else if (methodName.equals("setTranslationZ")) {
                    view.setTranslationZ(f);
                } else if (methodName.equals("setPivotX")) {
                    view.setPivotX(f);
                } else if (methodName.equals("setPivotY")) {
                    view.setPivotY(f);
                } else if (methodName.equals("setTextSize") && view instanceof android.widget.TextView) {
                    ((android.widget.TextView) view).setTextSize(f);
                } else {
                    return false;
                }
                return true;
            default:
                return false;
        }
    }

    private static int clamp(int c) {
        int N = 255;
        c &= ~(c >> 31);
        c -= N;
        c &= (c >> 31);
        c += N;
        return c;
    }

    public static void parse(Context context, XmlPullParser parser, HashMap<String, ConstraintAttribute> custom) {
        AttributeSet attributeSet = Xml.asAttributeSet(parser);
        TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.CustomAttribute);
        String name = null;
        boolean method = false;
        Object value = null;
        AttributeType type = null;
        final int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.CustomAttribute_attributeName) {
                name = a.getString(attr);
                if (name != null && name.length() > 0) {
                    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                }
            } else if (attr == R.styleable.CustomAttribute_methodName) {
                method = true;
                name = a.getString(attr);
            } else if (attr == R.styleable.CustomAttribute_customBoolean) {
                value = a.getBoolean(attr, false);
                type = AttributeType.BOOLEAN_TYPE;
            } else if (attr == R.styleable.CustomAttribute_customColorValue) {
                type = AttributeType.COLOR_TYPE;
                value = a.getColor(attr, 0);
            } else if (attr == R.styleable.CustomAttribute_customColorDrawableValue) {
                type = AttributeType.COLOR_DRAWABLE_TYPE;
                value = a.getColor(attr, 0);
            } else if (attr == R.styleable.CustomAttribute_customPixelDimension) {
                type = AttributeType.DIMENSION_TYPE;
                value = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        a.getDimension(attr, 0),
                        context.getResources().getDisplayMetrics());
            } else if (attr == R.styleable.CustomAttribute_customDimension) {
                type = AttributeType.DIMENSION_TYPE;
                value = a.getDimension(attr, 0);
            } else if (attr == R.styleable.CustomAttribute_customFloatValue) {
                type = AttributeType.FLOAT_TYPE;
                value = a.getFloat(attr, Float.NaN);
            } else if (attr == R.styleable.CustomAttribute_customIntegerValue) {
                type = AttributeType.INT_TYPE;
                value = a.getInteger(attr, -1);
            } else if (attr == R.styleable.CustomAttribute_customStringValue) {
                type = AttributeType.STRING_TYPE;
                value = a.getString(attr);
            } else if (attr == R.styleable.CustomAttribute_customReference) {
                type = AttributeType.REFERENCE_TYPE;
                int tmp = a.getResourceId(attr, -1);
                if (tmp == -1) {
                    tmp = a.getInt(attr, -1);
                }
                value = tmp;
            }
        }
        if (name != null && value != null) {
            custom.put(name, new ConstraintAttribute(name, type, value, method));
        }
        a.recycle();
    }

}
