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
package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.method.MovementMethod;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import com.codename1.androidcompat.runtime.ResTable;
import com.codename1.androidcompat.runtime.TextLayout;

import java.util.ArrayList;

/// Displays text, laid out and drawn by the view itself so measurement
/// follows Android's rules: text appearance from the theme and style, then
/// the view's own attributes; width from the longest line, height from the
/// line count clamped to min/max lines.
public class TextView extends View {

    public enum BufferType { NORMAL, SPANNABLE, EDITABLE }

    public interface OnEditorActionListener {
        boolean onEditorAction(TextView v, int actionId, KeyEvent event);
    }

    public static final int AUTO_SIZE_TEXT_TYPE_NONE = 0;
    public static final int AUTO_SIZE_TEXT_TYPE_UNIFORM = 1;

    /// No inputType attribute was given: a plain TextView, or an EditText
    /// that takes the EditText default.
    static final int INPUT_UNSET = -1;

    protected CharSequence mText = "";
    private CharSequence mHint;
    private ColorStateList mTextColor = ColorStateList.valueOf(0xff000000);
    private ColorStateList mHintTextColor;
    private ColorStateList mLinkTextColor;
    protected final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private int mGravity = Gravity.TOP | Gravity.START;
    private int mMaxLines = Integer.MAX_VALUE;
    private int mMinLines;
    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMinWidthPx;
    private int mMaxHeight = Integer.MAX_VALUE;
    private int mEms = -1;
    private int mMaxEms = -1;
    private int mMinEms = -1;
    private boolean mSingleLine;
    private TextUtils.TruncateAt mEllipsize;
    private boolean mAllCaps;
    private float mSpacingAdd;
    private float mSpacingMult = 1f;
    private int mLineHeight = -1;
    private boolean mIncludePad = true;
    private final Drawable[] mCompound = new Drawable[4];
    private int mCompoundPadding;
    private ColorStateList mDrawableTint;
    private int mInputType = INPUT_UNSET;
    private int mImeOptions;
    private CharSequence mImeActionLabel;
    private OnEditorActionListener mEditorActionListener;
    private ArrayList<TextWatcher> mWatchers;
    private InputFilter[] mFilters = new InputFilter[0];
    private TransformationMethod mTransformation;
    private int mAutoLink;
    private boolean mHorizontallyScrolling;

    private TextLayout mLayout;
    private int mLayoutWidth = -1;
    private String mLayoutText;

    public TextView(Context context) {
        this(context, null);
    }

    public TextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public TextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        float density = context.getResources().getDisplayMetrics().scaledDensity;
        mTextPaint.setTextSize(14 * density);

        // Text appearance first: the style's or theme's textAppearance.
        TypedArray ap = context.obtainStyledAttributes(attrs, new int[] {android.R.attr.textAppearance},
                defStyleAttr, defStyleRes);
        int appearance = ap.getResourceId(0, 0);
        ap.recycle();
        Appearance look = new Appearance();
        if (appearance != 0) {
            TypedArray a = context.obtainStyledAttributes(appearance, android.R.styleable.TextAppearance);
            look.read(a, false);
            a.recycle();
        }
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.TextView, defStyleAttr, defStyleRes);
        look.read(a, true);
        CharSequence text = "";
        Drawable dl = null;
        Drawable dt = null;
        Drawable dr = null;
        Drawable db = null;
        Drawable ds = null;
        Drawable de = null;
        int maxLength = -1;
        boolean password = false;
        int lines = -1;
        for (int i = 0, n = a.getIndexCount(); i < n; i++) {
            int attr = a.getIndex(i);
            if (attr == android.R.styleable.TextView_text) {
                text = a.getText(attr);
            } else if (attr == android.R.styleable.TextView_hint) {
                mHint = a.getText(attr);
            } else if (attr == android.R.styleable.TextView_gravity) {
                mGravity = a.getInt(attr, mGravity);
            } else if (attr == android.R.styleable.TextView_maxLines) {
                mMaxLines = a.getInt(attr, Integer.MAX_VALUE);
            } else if (attr == android.R.styleable.TextView_minLines) {
                mMinLines = a.getInt(attr, 0);
            } else if (attr == android.R.styleable.TextView_lines) {
                lines = a.getInt(attr, -1);
            } else if (attr == android.R.styleable.TextView_maxWidth) {
                mMaxWidth = a.getDimensionPixelSize(attr, Integer.MAX_VALUE);
            } else if (attr == android.R.styleable.TextView_maxHeight) {
                mMaxHeight = a.getDimensionPixelSize(attr, Integer.MAX_VALUE);
            } else if (attr == android.R.styleable.TextView_ems) {
                mEms = a.getInt(attr, -1);
            } else if (attr == android.R.styleable.TextView_maxEms) {
                mMaxEms = a.getInt(attr, -1);
            } else if (attr == android.R.styleable.TextView_minEms) {
                mMinEms = a.getInt(attr, -1);
            } else if (attr == android.R.styleable.TextView_singleLine) {
                mSingleLine = a.getBoolean(attr, false);
            } else if (attr == android.R.styleable.TextView_ellipsize) {
                int e = a.getInt(attr, 0);
                mEllipsize = e == 1 ? TextUtils.TruncateAt.START : e == 2 ? TextUtils.TruncateAt.MIDDLE
                        : e == 3 ? TextUtils.TruncateAt.END : e == 4 ? TextUtils.TruncateAt.MARQUEE : null;
            } else if (attr == android.R.styleable.TextView_lineSpacingExtra) {
                mSpacingAdd = a.getDimensionPixelSize(attr, 0);
            } else if (attr == android.R.styleable.TextView_lineSpacingMultiplier) {
                mSpacingMult = a.getFloat(attr, 1f);
            } else if (attr == android.R.styleable.TextView_lineHeight) {
                mLineHeight = a.getDimensionPixelSize(attr, -1);
            } else if (attr == android.R.styleable.TextView_includeFontPadding) {
                mIncludePad = a.getBoolean(attr, true);
            } else if (attr == android.R.styleable.TextView_drawableLeft) {
                dl = a.getDrawable(attr);
            } else if (attr == android.R.styleable.TextView_drawableTop) {
                dt = a.getDrawable(attr);
            } else if (attr == android.R.styleable.TextView_drawableRight) {
                dr = a.getDrawable(attr);
            } else if (attr == android.R.styleable.TextView_drawableBottom) {
                db = a.getDrawable(attr);
            } else if (attr == android.R.styleable.TextView_drawableStart) {
                ds = a.getDrawable(attr);
            } else if (attr == android.R.styleable.TextView_drawableEnd) {
                de = a.getDrawable(attr);
            } else if (attr == android.R.styleable.TextView_drawablePadding) {
                mCompoundPadding = a.getDimensionPixelSize(attr, 0);
            } else if (attr == android.R.styleable.TextView_drawableTint) {
                mDrawableTint = a.getColorStateList(attr);
            } else if (attr == android.R.styleable.TextView_inputType) {
                mInputType = a.getInt(attr, INPUT_UNSET);
            } else if (attr == android.R.styleable.TextView_imeOptions) {
                mImeOptions = a.getInt(attr, 0);
            } else if (attr == android.R.styleable.TextView_imeActionLabel) {
                mImeActionLabel = a.getText(attr);
            } else if (attr == android.R.styleable.TextView_maxLength) {
                maxLength = a.getInt(attr, -1);
            } else if (attr == android.R.styleable.TextView_password) {
                password = a.getBoolean(attr, false);
            } else if (attr == android.R.styleable.TextView_autoLink) {
                mAutoLink = a.getInt(attr, 0);
            } else if (attr == android.R.styleable.TextView_enabled) {
                setEnabled(a.getBoolean(attr, true));
            }
        }
        a.recycle();
        look.apply(this, context);
        if (lines > 0) {
            mMinLines = lines;
            mMaxLines = lines;
        }
        if (mSingleLine) {
            mMaxLines = 1;
        }
        if (maxLength >= 0) {
            mFilters = new InputFilter[] {new InputFilter.LengthFilter(maxLength)};
        }
        if (password && mInputType == INPUT_UNSET) {
            mInputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        }
        boolean rtl = isLayoutRtl();
        if (ds != null) {
            if (rtl) {
                dr = ds;
            } else {
                dl = ds;
            }
        }
        if (de != null) {
            if (rtl) {
                dl = de;
            } else {
                dr = de;
            }
        }
        setCompoundDrawablesWithIntrinsicBounds(dl, dt, dr, db);
        setText(text, BufferType.NORMAL);
    }

    /// The text appearance attributes, gathered from the appearance style and
    /// then overridden by the view's own.
    private static final class Appearance {
        ColorStateList textColor;
        ColorStateList hintColor;
        ColorStateList linkColor;
        float textSize = -1;
        int typefaceIndex = -1;
        int textStyle = -1;
        String fontFamily;
        int fontResource;
        int fontWeight = -1;
        boolean allCaps;
        boolean allCapsSet;
        float letterSpacing = Float.NaN;

        void read(TypedArray a, boolean textView) {
            int color = textView ? android.R.styleable.TextView_textColor : android.R.styleable.TextAppearance_textColor;
            int hint = textView ? android.R.styleable.TextView_textColorHint
                    : android.R.styleable.TextAppearance_textColorHint;
            int link = textView ? android.R.styleable.TextView_textColorLink
                    : android.R.styleable.TextAppearance_textColorLink;
            int size = textView ? android.R.styleable.TextView_textSize : android.R.styleable.TextAppearance_textSize;
            int tf = textView ? android.R.styleable.TextView_typeface : android.R.styleable.TextAppearance_typeface;
            int st = textView ? android.R.styleable.TextView_textStyle : android.R.styleable.TextAppearance_textStyle;
            int ff = textView ? android.R.styleable.TextView_fontFamily : android.R.styleable.TextAppearance_fontFamily;
            int fw = textView ? android.R.styleable.TextView_textFontWeight
                    : android.R.styleable.TextAppearance_textFontWeight;
            int caps = textView ? android.R.styleable.TextView_textAllCaps
                    : android.R.styleable.TextAppearance_textAllCaps;
            int ls = textView ? android.R.styleable.TextView_letterSpacing
                    : android.R.styleable.TextAppearance_letterSpacing;
            if (a.hasValue(color)) {
                textColor = a.getColorStateList(color);
            }
            if (a.hasValue(hint)) {
                hintColor = a.getColorStateList(hint);
            }
            if (a.hasValue(link)) {
                linkColor = a.getColorStateList(link);
            }
            if (a.hasValue(size)) {
                textSize = a.getDimension(size, textSize);
            }
            if (a.hasValue(tf)) {
                typefaceIndex = a.getInt(tf, -1);
            }
            if (a.hasValue(st)) {
                textStyle = a.getInt(st, -1);
            }
            if (a.hasValue(ff)) {
                int res = a.getResourceId(ff, 0);
                if (res != 0) {
                    fontResource = res;
                    fontFamily = null;
                } else {
                    fontFamily = a.getString(ff);
                    fontResource = 0;
                }
            }
            if (a.hasValue(fw)) {
                fontWeight = a.getInt(fw, -1);
            }
            if (a.hasValue(caps)) {
                allCaps = a.getBoolean(caps, false);
                allCapsSet = true;
            }
            if (a.hasValue(ls)) {
                letterSpacing = a.getFloat(ls, 0);
            }
        }

        void apply(TextView v, Context context) {
            if (textColor != null) {
                v.mTextColor = textColor;
            }
            if (hintColor != null) {
                v.mHintTextColor = hintColor;
            }
            if (linkColor != null) {
                v.mLinkTextColor = linkColor;
            }
            if (textSize >= 0) {
                v.mTextPaint.setTextSize(textSize);
            }
            if (allCapsSet) {
                v.mAllCaps = allCaps;
            }
            if (!Float.isNaN(letterSpacing)) {
                v.mTextPaint.setLetterSpacing(letterSpacing);
            }
            Typeface base = null;
            if (fontResource != 0) {
                base = loadFont(context, fontResource);
            } else if (fontFamily != null) {
                base = Typeface.create(fontFamily, Typeface.NORMAL);
            } else if (typefaceIndex == 2) {
                base = Typeface.SERIF;
            } else if (typefaceIndex == 3) {
                base = Typeface.MONOSPACE;
            }
            int style = textStyle < 0 ? Typeface.NORMAL : textStyle;
            Typeface tf = base == null ? Typeface.defaultFromStyle(style) : Typeface.create(base, style);
            if (fontWeight > 0) {
                tf = Typeface.create(tf, fontWeight, (style & Typeface.ITALIC) != 0);
            } else if (base != null && style == Typeface.NORMAL) {
                tf = base;
            }
            v.mTextPaint.setTypeface(tf);
        }
    }

    /// A `res/font` resource: a font file, or the first font of a font family.
    static Typeface loadFont(Context context, int id) {
        Object it = context.getResources().item(id);
        if (it instanceof ResTable.FileRef) {
            String name = context.getResources().getResourceEntryName(id);
            return Typeface.fromFile(name, ((ResTable.FileRef) it).name);
        }
        if (it instanceof com.codename1.androidcompat.runtime.XmlNode) {
            com.codename1.androidcompat.runtime.XmlNode n = (com.codename1.androidcompat.runtime.XmlNode) it;
            for (com.codename1.androidcompat.runtime.XmlNode f : n.children) {
                com.codename1.androidcompat.runtime.ResValue v = f.value(com.codename1.androidcompat.runtime.XmlNode.NS_ANDROID, "font");
                if (v == null) {
                    v = f.value(com.codename1.androidcompat.runtime.XmlNode.NS_APP, "font");
                }
                if (v != null && v.data != 0) {
                    return loadFont(context, v.data);
                }
            }
        }
        return Typeface.DEFAULT;
    }

    // ------------------------------------------------------------ text

    public final void setText(CharSequence text) {
        setText(text, BufferType.NORMAL);
    }

    public void setText(CharSequence text, BufferType type) {
        if (text == null) {
            text = "";
        }
        CharSequence old = mText;
        if (type == BufferType.EDITABLE || this instanceof EditText) {
            SpannableStringBuilder sb = new SpannableStringBuilder(text);
            sb.setFilters(mFilters);
            text = sb;
        }
        sendBeforeTextChanged(old, 0, old == null ? 0 : old.length(), text.length());
        mText = text;
        onTextReplaced(text);
        sendOnTextChanged(text, 0, old == null ? 0 : old.length(), text.length());
        sendAfterTextChanged();
        textChanged();
    }

    /// Hook for EditText to push the new text into its native field.
    void onTextReplaced(CharSequence text) {
    }

    public final void setText(int resid) {
        setText(getContext().getResources().getText(resid));
    }

    public final void setText(int resid, BufferType type) {
        setText(getContext().getResources().getText(resid), type);
    }

    public final void setText(char[] text, int start, int len) {
        setText(new String(text, start, len));
    }

    public CharSequence getText() {
        return mText;
    }

    public int length() {
        return mText.length();
    }

    public final void append(CharSequence text) {
        append(text, 0, text.length());
    }

    public void append(CharSequence text, int start, int end) {
        if (mText instanceof Editable) {
            ((Editable) mText).append(text, start, end);
            textChanged();
        } else {
            setText(mText.toString() + text.subSequence(start, end));
        }
    }

    public Editable getEditableText() {
        return mText instanceof Editable ? (Editable) mText : null;
    }

    public void setHint(CharSequence hint) {
        mHint = hint;
        textChanged();
    }

    public final void setHint(int resid) {
        setHint(getContext().getResources().getText(resid));
    }

    public CharSequence getHint() {
        return mHint;
    }

    /// Runtime use: the hint this view shows. Like Android's drawing code it
    /// reads the field, not getHint(), which a subclass may override to
    /// report a hint shown elsewhere (TextInputEditText reports its layout's
    /// floating label).
    public final CharSequence getShownHint() {
        return mHint;
    }

    /// Called whenever the displayed text may have changed size.
    protected void textChanged() {
        mLayout = null;
        requestLayout();
        invalidate();
    }

    // ------------------------------------------------------------ watchers & editing

    public void addTextChangedListener(TextWatcher watcher) {
        if (mWatchers == null) {
            mWatchers = new ArrayList<TextWatcher>();
        }
        mWatchers.add(watcher);
    }

    public void removeTextChangedListener(TextWatcher watcher) {
        if (mWatchers != null) {
            mWatchers.remove(watcher);
        }
    }

    void sendBeforeTextChanged(CharSequence text, int start, int before, int after) {
        if (mWatchers != null) {
            for (TextWatcher w : new ArrayList<TextWatcher>(mWatchers)) {
                w.beforeTextChanged(text, start, before, after);
            }
        }
    }

    void sendOnTextChanged(CharSequence text, int start, int before, int after) {
        if (mWatchers != null) {
            for (TextWatcher w : new ArrayList<TextWatcher>(mWatchers)) {
                w.onTextChanged(text, start, before, after);
            }
        }
    }

    void sendAfterTextChanged() {
        if (mWatchers != null && mText instanceof Editable) {
            for (TextWatcher w : new ArrayList<TextWatcher>(mWatchers)) {
                w.afterTextChanged((Editable) mText);
            }
        }
    }

    public void setFilters(InputFilter[] filters) {
        mFilters = filters == null ? new InputFilter[0] : filters;
        if (mText instanceof Editable) {
            ((Editable) mText).setFilters(mFilters);
        }
    }

    public InputFilter[] getFilters() {
        return mFilters;
    }

    public void setInputType(int type) {
        mInputType = type;
        if ((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0 && (type & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
                && this instanceof EditText) {
            mSingleLine = true;
            mMaxLines = 1;
        }
        textChanged();
    }

    public int getInputType() {
        return mInputType == INPUT_UNSET ? InputType.TYPE_NULL : mInputType;
    }

    /// The raw input type, or [EditorInfo#TYPE_NULL_COMPAT] when never set.
    int rawInputType() {
        return mInputType;
    }

    public void setRawInputType(int type) {
        mInputType = type;
    }

    public void setImeOptions(int imeOptions) {
        mImeOptions = imeOptions;
    }

    public int getImeOptions() {
        return mImeOptions;
    }

    public void setImeActionLabel(CharSequence label, int actionId) {
        mImeActionLabel = label;
    }

    public CharSequence getImeActionLabel() {
        return mImeActionLabel;
    }

    public void setOnEditorActionListener(OnEditorActionListener l) {
        mEditorActionListener = l;
    }

    public void onEditorAction(int actionCode) {
        if (mEditorActionListener != null && mEditorActionListener.onEditorAction(this, actionCode, null)) {
            return;
        }
        if (actionCode == EditorInfo.IME_ACTION_DONE || actionCode == EditorInfo.IME_ACTION_GO
                || actionCode == EditorInfo.IME_ACTION_SEARCH || actionCode == EditorInfo.IME_ACTION_SEND) {
            android.view.inputmethod.InputMethodManager imm = new android.view.inputmethod.InputMethodManager();
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    public void setKeyListener(KeyListener input) {
        if (input != null) {
            setInputType(input.getInputType());
        }
    }

    public void setTransformationMethod(TransformationMethod method) {
        mTransformation = method;
        textChanged();
    }

    public final TransformationMethod getTransformationMethod() {
        return mTransformation;
    }

    public final void setMovementMethod(MovementMethod movement) {
    }

    public final void setLinksClickable(boolean whether) {
    }

    public final void setAutoLinkMask(int mask) {
        mAutoLink = mask;
    }

    public void setTextIsSelectable(boolean selectable) {
    }

    public void setCursorVisible(boolean visible) {
    }

    public void setSelectAllOnFocus(boolean selectAllOnFocus) {
    }

    public void setHorizontallyScrolling(boolean whether) {
        mHorizontallyScrolling = whether;
        textChanged();
    }

    public void setError(CharSequence error) {
        setError(error, null);
    }

    public void setError(CharSequence error, Drawable icon) {
        mError = error;
        invalidate();
    }

    private CharSequence mError;

    public CharSequence getError() {
        return mError;
    }

    public int getSelectionStart() {
        return mText.length();
    }

    public int getSelectionEnd() {
        return mText.length();
    }

    public boolean hasSelection() {
        return false;
    }

    // ------------------------------------------------------------ appearance

    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    public void setTextSize(int unit, float size) {
        mTextPaint.setTextSize(TypedValue.applyDimension(unit, size, getResources().getDisplayMetrics()));
        textChanged();
    }

    public float getTextSize() {
        return mTextPaint.getTextSize();
    }

    public float getTextScaleX() {
        return mTextPaint.getTextScaleX();
    }

    public void setTextScaleX(float size) {
        mTextPaint.setTextScaleX(size);
        textChanged();
    }

    public void setTextColor(int color) {
        mTextColor = ColorStateList.valueOf(color);
        invalidate();
    }

    public void setTextColor(ColorStateList colors) {
        if (colors == null) {
            throw new NullPointerException();
        }
        mTextColor = colors;
        invalidate();
    }

    public final ColorStateList getTextColors() {
        return mTextColor;
    }

    public final int getCurrentTextColor() {
        return mTextColor.getColorForState(getDrawableState(), mTextColor.getDefaultColor());
    }

    public final void setHintTextColor(int color) {
        mHintTextColor = ColorStateList.valueOf(color);
        invalidate();
    }

    public final void setHintTextColor(ColorStateList colors) {
        mHintTextColor = colors;
        invalidate();
    }

    public final ColorStateList getHintTextColors() {
        return mHintTextColor;
    }

    public final int getCurrentHintTextColor() {
        return mHintTextColor == null ? 0x61000000
                : mHintTextColor.getColorForState(getDrawableState(), mHintTextColor.getDefaultColor());
    }

    public final void setLinkTextColor(int color) {
        mLinkTextColor = ColorStateList.valueOf(color);
    }

    public final void setLinkTextColor(ColorStateList colors) {
        mLinkTextColor = colors;
    }

    public void setTypeface(Typeface tf) {
        mTextPaint.setTypeface(tf);
        textChanged();
    }

    public void setTypeface(Typeface tf, int style) {
        if (style > 0) {
            tf = tf == null ? Typeface.defaultFromStyle(style) : Typeface.create(tf, style);
            setTypeface(tf);
            int need = style & ~tf.getStyle();
            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
        } else {
            mTextPaint.setFakeBoldText(false);
            setTypeface(tf);
        }
    }

    public Typeface getTypeface() {
        return mTextPaint.getTypeface();
    }

    public void setTextAppearance(int resId) {
        TypedArray a = getContext().obtainStyledAttributes(resId, android.R.styleable.TextAppearance);
        Appearance look = new Appearance();
        look.read(a, false);
        a.recycle();
        look.apply(this, getContext());
        textChanged();
    }

    public void setTextAppearance(Context context, int resId) {
        setTextAppearance(resId);
    }

    public void setGravity(int gravity) {
        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.START;
        }
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.TOP;
        }
        mGravity = gravity;
        invalidate();
    }

    public int getGravity() {
        return mGravity;
    }

    public void setAllCaps(boolean allCaps) {
        mAllCaps = allCaps;
        textChanged();
    }

    public boolean isAllCaps() {
        return mAllCaps;
    }

    public void setSingleLine() {
        setSingleLine(true);
    }

    public void setSingleLine(boolean singleLine) {
        mSingleLine = singleLine;
        mMaxLines = singleLine ? 1 : Integer.MAX_VALUE;
        textChanged();
    }

    public boolean isSingleLine() {
        return mSingleLine;
    }

    public void setMaxLines(int maxLines) {
        mMaxLines = maxLines;
        textChanged();
    }

    public int getMaxLines() {
        return mMaxLines;
    }

    public void setMinLines(int minLines) {
        mMinLines = minLines;
        textChanged();
    }

    public int getMinLines() {
        return mMinLines;
    }

    public void setLines(int lines) {
        mMinLines = lines;
        mMaxLines = lines;
        textChanged();
    }

    public void setMaxWidth(int maxPixels) {
        mMaxWidth = maxPixels;
        textChanged();
    }

    public int getMaxWidth() {
        return mMaxWidth;
    }

    public void setMinWidth(int minPixels) {
        mMinWidthPx = minPixels;
        textChanged();
    }

    public void setMaxHeight(int maxPixels) {
        mMaxHeight = maxPixels;
        textChanged();
    }

    public void setEms(int ems) {
        mEms = ems;
        textChanged();
    }

    public void setMaxEms(int maxEms) {
        mMaxEms = maxEms;
        textChanged();
    }

    public void setMinEms(int minEms) {
        mMinEms = minEms;
        textChanged();
    }

    public void setEllipsize(TextUtils.TruncateAt where) {
        mEllipsize = where;
        textChanged();
    }

    public TextUtils.TruncateAt getEllipsize() {
        return mEllipsize;
    }

    public void setLineSpacing(float add, float mult) {
        mSpacingAdd = add;
        mSpacingMult = mult;
        textChanged();
    }

    public float getLineSpacingExtra() {
        return mSpacingAdd;
    }

    public float getLineSpacingMultiplier() {
        return mSpacingMult;
    }

    public void setLineHeight(int lineHeight) {
        mLineHeight = lineHeight;
        textChanged();
    }

    public void setLetterSpacing(float letterSpacing) {
        mTextPaint.setLetterSpacing(letterSpacing);
        textChanged();
    }

    public float getLetterSpacing() {
        return mTextPaint.getLetterSpacing();
    }

    public void setIncludeFontPadding(boolean includepad) {
        mIncludePad = includepad;
        textChanged();
    }

    public void setShadowLayer(float radius, float dx, float dy, int color) {
        mTextPaint.setShadowLayer(radius, dx, dy, color);
    }

    public void setPaintFlags(int flags) {
        mTextPaint.setFlags(flags);
        invalidate();
    }

    public int getPaintFlags() {
        return mTextPaint.getFlags();
    }

    public TextPaint getPaint() {
        return mTextPaint;
    }

    public void setBreakStrategy(int breakStrategy) {
    }

    public void setHyphenationFrequency(int hyphenationFrequency) {
    }

    public void setAutoSizeTextTypeWithDefaults(int autoSizeTextType) {
    }

    public void setTextLocale(java.util.Locale locale) {
    }

    public void setFontFeatureSettings(String fontFeatureSettings) {
    }

    // ------------------------------------------------------------ compound drawables

    public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        Drawable[] n = {left, top, right, bottom};
        for (int i = 0; i < 4; i++) {
            if (mCompound[i] != null) {
                mCompound[i].setCallback(null);
            }
            mCompound[i] = n[i];
            if (n[i] != null) {
                n[i].setCallback(this);
                if (mDrawableTint != null) {
                    n[i].setTintList(mDrawableTint);
                }
                if (n[i].isStateful()) {
                    n[i].setState(getDrawableState());
                }
            }
        }
        textChanged();
    }

    public void setCompoundDrawablesWithIntrinsicBounds(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        for (Drawable d : new Drawable[] {left, top, right, bottom}) {
            if (d != null) {
                d.setBounds(0, 0, Math.max(0, d.getIntrinsicWidth()), Math.max(0, d.getIntrinsicHeight()));
            }
        }
        setCompoundDrawables(left, top, right, bottom);
    }

    public void setCompoundDrawablesWithIntrinsicBounds(int left, int top, int right, int bottom) {
        Context c = getContext();
        setCompoundDrawablesWithIntrinsicBounds(left != 0 ? c.getDrawable(left) : null,
                top != 0 ? c.getDrawable(top) : null, right != 0 ? c.getDrawable(right) : null,
                bottom != 0 ? c.getDrawable(bottom) : null);
    }

    public void setCompoundDrawablesRelative(Drawable start, Drawable top, Drawable end, Drawable bottom) {
        if (isLayoutRtl()) {
            setCompoundDrawables(end, top, start, bottom);
        } else {
            setCompoundDrawables(start, top, end, bottom);
        }
    }

    public void setCompoundDrawablesRelativeWithIntrinsicBounds(Drawable start, Drawable top, Drawable end,
                                                                Drawable bottom) {
        if (isLayoutRtl()) {
            setCompoundDrawablesWithIntrinsicBounds(end, top, start, bottom);
        } else {
            setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
        }
    }

    public void setCompoundDrawablesRelativeWithIntrinsicBounds(int start, int top, int end, int bottom) {
        Context c = getContext();
        setCompoundDrawablesRelativeWithIntrinsicBounds(start != 0 ? c.getDrawable(start) : null,
                top != 0 ? c.getDrawable(top) : null, end != 0 ? c.getDrawable(end) : null,
                bottom != 0 ? c.getDrawable(bottom) : null);
    }

    public Drawable[] getCompoundDrawables() {
        return mCompound.clone();
    }

    public Drawable[] getCompoundDrawablesRelative() {
        return isLayoutRtl() ? new Drawable[] {mCompound[2], mCompound[1], mCompound[0], mCompound[3]}
                : mCompound.clone();
    }

    public void setCompoundDrawablePadding(int pad) {
        mCompoundPadding = pad;
        textChanged();
    }

    public int getCompoundDrawablePadding() {
        return mCompoundPadding;
    }

    public void setCompoundDrawableTintList(ColorStateList tint) {
        mDrawableTint = tint;
        for (Drawable d : mCompound) {
            if (d != null) {
                d.setTintList(tint);
            }
        }
    }

    private int cw(int i) {
        return mCompound[i] == null ? 0 : mCompound[i].getBounds().width();
    }

    private int ch(int i) {
        return mCompound[i] == null ? 0 : mCompound[i].getBounds().height();
    }

    public int getCompoundPaddingLeft() {
        return getPaddingLeft() + (mCompound[0] == null ? 0 : cw(0) + mCompoundPadding);
    }

    public int getCompoundPaddingRight() {
        return getPaddingRight() + (mCompound[2] == null ? 0 : cw(2) + mCompoundPadding);
    }

    public int getCompoundPaddingTop() {
        return getPaddingTop() + (mCompound[1] == null ? 0 : ch(1) + mCompoundPadding);
    }

    public int getCompoundPaddingBottom() {
        return getPaddingBottom() + (mCompound[3] == null ? 0 : ch(3) + mCompoundPadding);
    }

    public int getTotalPaddingLeft() {
        return getCompoundPaddingLeft();
    }

    public int getTotalPaddingRight() {
        return getCompoundPaddingRight();
    }

    public int getTotalPaddingTop() {
        return getCompoundPaddingTop();
    }

    public int getTotalPaddingBottom() {
        return getCompoundPaddingBottom();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        if (super.verifyDrawable(who)) {
            return true;
        }
        if (mCompound == null) {
            return false;
        }
        for (Drawable d : mCompound) {
            if (d == who) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mCompound == null) {
            // Called from View's constructor, before this class's fields exist.
            return;
        }
        int[] state = getDrawableState();
        for (Drawable d : mCompound) {
            if (d != null && d.isStateful()) {
                d.setState(state);
            }
        }
        if (mTextColor != null && mTextColor.isStateful()) {
            invalidate();
        }
    }

    // ------------------------------------------------------------ layout

    /// The text as displayed: transformed (password), capitalized.
    String displayText() {
        CharSequence t = mText;
        if (t.length() == 0) {
            return "";
        }
        if (mTransformation != null) {
            t = mTransformation.getTransformation(t, this);
        } else if (isPasswordInput()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < t.length(); i++) {
                sb.append('\u2022');
            }
            t = sb;
        }
        String s = t.toString();
        if (mAllCaps) {
            s = s.toUpperCase();
        }
        return s;
    }

    boolean isPasswordInput() {
        int variation = mInputType & (InputType.TYPE_MASK_CLASS | InputType.TYPE_MASK_VARIATION);
        return mInputType != INPUT_UNSET
                && (variation == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                || variation == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
                || variation == (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));
    }

    private String shownText() {
        String s = displayText();
        if (s.length() == 0 && mHint != null) {
            s = mHint.toString();
        }
        return s;
    }

    /// The layout for a content width, rebuilt when the text or width changed.
    /// Package private for the tests that check what a line shows.
    TextLayout layoutFor(int width) {
        String s = shownText();
        if (mLayout == null || mLayoutWidth != width || !s.equals(mLayoutText)) {
            boolean single = mSingleLine || mHorizontallyScrolling;
            int maxLines = mMaxLines == Integer.MAX_VALUE ? 0 : mMaxLines;
            int ellipsize = TextLayout.ELLIPSIZE_NONE;
            if (mEllipsize == TextUtils.TruncateAt.END || mEllipsize == TextUtils.TruncateAt.MARQUEE) {
                ellipsize = TextLayout.ELLIPSIZE_END;
            } else if (mEllipsize != null && single) {
                ellipsize = mEllipsize == TextUtils.TruncateAt.START ? TextLayout.ELLIPSIZE_START
                        : TextLayout.ELLIPSIZE_MIDDLE;
            }
            mLayout = TextLayout.layout(s, mTextPaint,
                    single ? (ellipsize != TextLayout.ELLIPSIZE_NONE ? width : 0) : width, single, maxLines, ellipsize);
            mLayoutWidth = width;
            mLayoutText = s;
        }
        return mLayout;
    }

    public int getLineHeight() {
        if (mLineHeight > 0) {
            return mLineHeight;
        }
        return Math.round(mTextPaint.getFontMetricsInt(null) * mSpacingMult + mSpacingAdd);
    }

    public int getLineCount() {
        return mLayout == null ? 0 : mLayout.lines.size();
    }

    @Override
    public int getBaseline() {
        int top = getExtendedPaddingTop();
        return top + Math.round(-mTextPaint.ascent());
    }

    public int getExtendedPaddingTop() {
        return getCompoundPaddingTop();
    }

    public int getExtendedPaddingBottom() {
        return getCompoundPaddingBottom();
    }

    private int ems(int n) {
        return n * getLineHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int hpad = getCompoundPaddingLeft() + getCompoundPaddingRight();
        int vpad = getCompoundPaddingTop() + getCompoundPaddingBottom();
        int width;
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            TextLayout unbounded = TextLayout.layout(shownText(), mTextPaint, 0, mSingleLine, 0, false);
            int desired = (int) Math.ceil(unbounded.maxLineWidth);
            if (mEms > 0) {
                desired = ems(mEms);
            }
            if (mMaxEms > 0) {
                desired = Math.min(desired, ems(mMaxEms));
            }
            if (mMinEms > 0) {
                desired = Math.max(desired, ems(mMinEms));
            }
            desired += hpad;
            desired = Math.max(desired, Math.max(cw(1), cw(3)) + getPaddingLeft() + getPaddingRight());
            desired = Math.min(desired, mMaxWidth);
            desired = Math.max(desired, Math.max(mMinWidthPx, getSuggestedMinimumWidth()));
            width = widthMode == MeasureSpec.AT_MOST ? Math.min(widthSize, desired) : desired;
        }
        int avail = Math.max(0, width - hpad);
        TextLayout l = layoutFor(avail);
        int lineCount = Math.max(1, l.lines.size());
        lineCount = Math.max(lineCount, mMinLines);
        if (mMaxLines != Integer.MAX_VALUE) {
            lineCount = Math.min(lineCount, Math.max(1, mMaxLines));
        }
        int textHeight = textHeight(lineCount);
        int height;
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            int desired = textHeight + vpad;
            desired = Math.max(desired, Math.max(ch(0), ch(2)) + getPaddingTop() + getPaddingBottom());
            desired = Math.min(desired, mMaxHeight);
            desired = Math.max(desired, getSuggestedMinimumHeight());
            height = heightMode == MeasureSpec.AT_MOST ? Math.min(heightSize, desired) : desired;
        }
        setMeasuredDimension(width, height);
    }

    private int textHeight(int lineCount) {
        int fontHeight = mTextPaint.getFontMetricsInt(null);
        int lineHeight = getLineHeight();
        // The last line does not take the extra spacing, as in StaticLayout.
        return lineHeight * (lineCount - 1) + fontHeight;
    }

    // ------------------------------------------------------------ draw

    @Override
    protected void onDraw(Canvas canvas) {
        drawCompoundDrawables(canvas);
        drawText(canvas);
    }

    void drawCompoundDrawables(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        int pl = getPaddingLeft();
        int pt = getPaddingTop();
        int pr = getPaddingRight();
        int pb = getPaddingBottom();
        int innerTop = getCompoundPaddingTop();
        int innerBottom = h - getCompoundPaddingBottom();
        int innerLeft = getCompoundPaddingLeft();
        int innerRight = w - getCompoundPaddingRight();
        if (mCompound[0] != null) {
            canvas.save();
            canvas.translate(mScrollX + pl, mScrollY + innerTop + (innerBottom - innerTop - ch(0)) / 2);
            mCompound[0].draw(canvas);
            canvas.restore();
        }
        if (mCompound[2] != null) {
            canvas.save();
            canvas.translate(mScrollX + w - pr - cw(2), mScrollY + innerTop + (innerBottom - innerTop - ch(2)) / 2);
            mCompound[2].draw(canvas);
            canvas.restore();
        }
        if (mCompound[1] != null) {
            canvas.save();
            canvas.translate(mScrollX + innerLeft + (innerRight - innerLeft - cw(1)) / 2, mScrollY + pt);
            mCompound[1].draw(canvas);
            canvas.restore();
        }
        if (mCompound[3] != null) {
            canvas.save();
            canvas.translate(mScrollX + innerLeft + (innerRight - innerLeft - cw(3)) / 2, mScrollY + h - pb - ch(3));
            mCompound[3].draw(canvas);
            canvas.restore();
        }
    }

    void drawText(Canvas canvas) {
        int left = getCompoundPaddingLeft();
        int right = getWidth() - getCompoundPaddingRight();
        int top = getCompoundPaddingTop();
        int bottom = getHeight() - getCompoundPaddingBottom();
        TextLayout l = layoutFor(Math.max(0, right - left));
        boolean hint = displayText().length() == 0;
        int color = hint ? getCurrentHintTextColor() : getCurrentTextColor();
        mTextPaint.setColor(color);
        int lines = l.lines.size();
        if (mMaxLines != Integer.MAX_VALUE) {
            lines = Math.min(lines, Math.max(1, mMaxLines));
        }
        int textHeight = textHeight(Math.max(1, lines));
        int gravity = Gravity.getAbsoluteGravity(mGravity, getLayoutDirection());
        int vg = gravity & Gravity.VERTICAL_GRAVITY_MASK;
        int y = top;
        if (vg == Gravity.CENTER_VERTICAL) {
            y = top + (bottom - top - textHeight) / 2;
        } else if (vg == Gravity.BOTTOM) {
            y = bottom - textHeight;
        }
        int hg = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        int textAlign = getTextAlignment();
        float ascent = -mTextPaint.ascent();
        int lineHeight = getLineHeight();
        canvas.save();
        canvas.clipRect(left + mScrollX, top + mScrollY, right + mScrollX, bottom + mScrollY);
        for (int i = 0; i < lines; i++) {
            String s = l.lines.get(i);
            float w = mTextPaint.measureText(s);
            float x = left;
            if (textAlign == TEXT_ALIGNMENT_CENTER) {
                x = left + (right - left - w) / 2;
            } else if (textAlign == TEXT_ALIGNMENT_TEXT_END || textAlign == TEXT_ALIGNMENT_VIEW_END) {
                x = isLayoutRtl() ? left : right - w;
            } else if (textAlign == TEXT_ALIGNMENT_GRAVITY || textAlign == TEXT_ALIGNMENT_INHERIT) {
                if (hg == Gravity.CENTER_HORIZONTAL) {
                    x = left + (right - left - w) / 2;
                } else if (hg == Gravity.RIGHT) {
                    x = right - w;
                }
            } else if (isLayoutRtl()) {
                x = right - w;
            }
            canvas.drawText(s, x, y + ascent + i * lineHeight, mTextPaint);
        }
        canvas.restore();
    }

    @Override
    public void findViewsWithText(java.util.ArrayList<View> outViews, CharSequence searched, int flags) {
        super.findViewsWithText(outViews, searched, flags);
        if (searched != null && mText.toString().indexOf(searched.toString()) >= 0 && !outViews.contains(this)) {
            outViews.add(this);
        }
    }

    public CharSequence getAccessibilityClassName() {
        return TextView.class.getName();
    }

    @Override
    public String toString() {
        return super.toString() + " text=\"" + mText + "\"";
    }
}
