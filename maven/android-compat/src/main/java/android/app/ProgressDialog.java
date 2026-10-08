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
package android.app;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/// A dialog showing a progress indicator and a message: a spinning wheel
/// beside the message ([#STYLE_SPINNER]), or a bar with the percentage and
/// the count below it ([#STYLE_HORIZONTAL]).
@Deprecated
public class ProgressDialog extends AlertDialog {

    public static final int STYLE_SPINNER = 0;
    public static final int STYLE_HORIZONTAL = 1;

    private int mProgressStyle = STYLE_SPINNER;
    private ProgressBar mProgress;
    private TextView mMessageView;
    private TextView mPercentView;
    private TextView mNumberView;
    private CharSequence mMessage;
    private int mMax = 100;
    private int mProgressVal;
    private int mSecondaryProgressVal;
    private int mIncrementBy;
    private int mIncrementSecondaryBy;
    private boolean mIndeterminate;
    private String mNumberFormat = "%1d/%2d";
    private boolean mBuilt;

    public ProgressDialog(Context context) {
        super(context);
    }

    public ProgressDialog(Context context, int theme) {
        super(context, theme);
    }

    public static ProgressDialog show(Context context, CharSequence title, CharSequence message) {
        return show(context, title, message, false);
    }

    public static ProgressDialog show(Context context, CharSequence title, CharSequence message,
                                      boolean indeterminate) {
        return show(context, title, message, indeterminate, false, null);
    }

    public static ProgressDialog show(Context context, CharSequence title, CharSequence message,
                                      boolean indeterminate, boolean cancelable) {
        return show(context, title, message, indeterminate, cancelable, null);
    }

    public static ProgressDialog show(Context context, CharSequence title, CharSequence message,
                                      boolean indeterminate, boolean cancelable, OnCancelListener cancelListener) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(indeterminate);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        dialog.show();
        return dialog;
    }

    private int dp(float v) {
        return Math.round(v * getContext().getResources().getDisplayMetrics().density);
    }

    private void buildContent() {
        if (mBuilt) {
            return;
        }
        mBuilt = true;
        Context c = getContext();
        if (mProgressStyle == STYLE_HORIZONTAL) {
            LinearLayout box = new LinearLayout(c);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(24), 0, dp(24), dp(8));
            mMessageView = new TextView(c);
            mMessageView.setTextSize(16);
            box.addView(mMessageView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            mProgress = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            barLp.topMargin = dp(12);
            box.addView(mProgress, barLp);
            LinearLayout numbers = new LinearLayout(c);
            numbers.setOrientation(LinearLayout.HORIZONTAL);
            mPercentView = new TextView(c);
            mNumberView = new TextView(c);
            mNumberView.setGravity(Gravity.END);
            numbers.addView(mPercentView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            numbers.addView(mNumberView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            box.addView(numbers, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            setView(box);
        } else {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(24), 0, dp(24), dp(16));
            mProgress = new ProgressBar(c);
            row.addView(mProgress, new LinearLayout.LayoutParams(dp(48), dp(48)));
            mMessageView = new TextView(c);
            mMessageView.setTextSize(16);
            LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            msgLp.leftMargin = dp(16);
            row.addView(mMessageView, msgLp);
            setView(row);
        }
        mProgress.setMax(mMax);
        mProgress.setProgress(mProgressVal);
        mProgress.setSecondaryProgress(mSecondaryProgressVal);
        if (mIncrementBy > 0) {
            incrementProgressBy(mIncrementBy);
        }
        if (mIncrementSecondaryBy > 0) {
            incrementSecondaryProgressBy(mIncrementSecondaryBy);
        }
        mProgress.setIndeterminate(mIndeterminate || mProgressStyle == STYLE_SPINNER);
        mMessageView.setText(mMessage == null ? "" : mMessage);
        updateNumbers();
    }

    @Override
    public void show() {
        buildContent();
        super.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateNumbers();
    }

    private void updateNumbers() {
        if (mProgressStyle != STYLE_HORIZONTAL || mPercentView == null) {
            return;
        }
        if (mIndeterminate) {
            mPercentView.setText("");
            mNumberView.setText("");
            return;
        }
        int progress = getProgress();
        int max = getMax();
        mPercentView.setText((max <= 0 ? 0 : (int) ((long) progress * 100 / max)) + "%");
        mNumberView.setText(formatNumbers(progress, max));
    }

    /// Applies the `%1d/%2d` style format without `String.format`.
    private String formatNumbers(int progress, int max) {
        String f = mNumberFormat == null ? "%1d/%2d" : mNumberFormat;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < f.length(); i++) {
            char ch = f.charAt(i);
            if (ch == '%' && i + 2 < f.length() && f.charAt(i + 2) == 'd'
                    && (f.charAt(i + 1) == '1' || f.charAt(i + 1) == '2')) {
                sb.append(f.charAt(i + 1) == '1' ? progress : max);
                i += 2;
            } else if (ch == '%' && i + 1 < f.length() && f.charAt(i + 1) == '%') {
                sb.append('%');
                i++;
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public void setProgressStyle(int style) {
        mProgressStyle = style;
    }

    public void setProgress(int value) {
        mProgressVal = value;
        if (mProgress != null) {
            mProgress.setProgress(value);
            updateNumbers();
        }
    }

    public void setSecondaryProgress(int secondaryProgress) {
        mSecondaryProgressVal = secondaryProgress;
        if (mProgress != null) {
            mProgress.setSecondaryProgress(secondaryProgress);
        }
    }

    public int getProgress() {
        return mProgress != null ? mProgress.getProgress() : mProgressVal;
    }

    public int getSecondaryProgress() {
        return mProgress != null ? mProgress.getSecondaryProgress() : mSecondaryProgressVal;
    }

    public int getMax() {
        return mProgress != null ? mProgress.getMax() : mMax;
    }

    public void setMax(int max) {
        mMax = max;
        if (mProgress != null) {
            mProgress.setMax(max);
            updateNumbers();
        }
    }

    public void incrementProgressBy(int diff) {
        if (mProgress != null) {
            mProgress.setProgress(mProgress.getProgress() + diff);
            updateNumbers();
        } else {
            mIncrementBy += diff;
        }
    }

    public void incrementSecondaryProgressBy(int diff) {
        if (mProgress != null) {
            mProgress.setSecondaryProgress(mProgress.getSecondaryProgress() + diff);
        } else {
            mIncrementSecondaryBy += diff;
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        mIndeterminate = indeterminate;
        if (mProgress != null) {
            mProgress.setIndeterminate(indeterminate || mProgressStyle == STYLE_SPINNER);
            updateNumbers();
        }
    }

    public boolean isIndeterminate() {
        return mProgress != null ? mProgress.isIndeterminate() : mIndeterminate;
    }

    @Override
    public void setMessage(CharSequence message) {
        mMessage = message;
        if (mMessageView != null) {
            mMessageView.setText(message == null ? "" : message);
        }
    }

    public void setProgressNumberFormat(String format) {
        mNumberFormat = format;
        updateNumbers();
    }
}
