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
package android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;

/// An invisible, zero-sized placeholder that inflates a layout in its place
/// when [#inflate()] is called or it is made visible.
public final class ViewStub extends View {

    public interface OnInflateListener {
        void onInflate(ViewStub stub, View inflated);
    }

    private int mLayoutResource;
    private int mInflatedId;
    private LayoutInflater mInflater;
    private OnInflateListener mInflateListener;
    private java.lang.ref.WeakReference<View> mInflated;

    public ViewStub(Context context) {
        this(context, 0);
    }

    public ViewStub(Context context, int layoutResource) {
        super(context);
        mLayoutResource = layoutResource;
        setVisibility(GONE);
        setWillNotDraw(true);
    }

    public ViewStub(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewStub(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ViewStub(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.ViewStub, defStyleAttr, defStyleRes);
        mInflatedId = a.getResourceId(android.R.styleable.ViewStub_inflatedId, NO_ID);
        mLayoutResource = a.getResourceId(android.R.styleable.ViewStub_layout, 0);
        setId(a.getResourceId(android.R.styleable.ViewStub_id, NO_ID));
        a.recycle();
        setVisibility(GONE);
        setWillNotDraw(true);
    }

    public int getInflatedId() {
        return mInflatedId;
    }

    public void setInflatedId(int inflatedId) {
        mInflatedId = inflatedId;
    }

    public int getLayoutResource() {
        return mLayoutResource;
    }

    public void setLayoutResource(int layoutResource) {
        mLayoutResource = layoutResource;
    }

    public void setLayoutInflater(LayoutInflater inflater) {
        mInflater = inflater;
    }

    public LayoutInflater getLayoutInflater() {
        return mInflater;
    }

    public void setOnInflateListener(OnInflateListener listener) {
        mInflateListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }

    @Override
    public void draw(Canvas canvas) {
    }

    @Override
    public void setVisibility(int visibility) {
        if (mInflated != null) {
            View inflated = mInflated.get();
            if (inflated == null) {
                throw new IllegalStateException("ViewStub inflated view is no longer available");
            }
            inflated.setVisibility(visibility);
            return;
        }
        super.setVisibility(visibility);
        if (visibility == VISIBLE || visibility == INVISIBLE) {
            if (getParent() != null) {
                inflate().setVisibility(visibility);
            }
        }
    }

    public View inflate() {
        ViewParent viewParent = getParent();
        if (!(viewParent instanceof ViewGroup)) {
            throw new IllegalStateException("ViewStub must have a non-null ViewGroup viewParent");
        }
        if (mLayoutResource == 0) {
            throw new IllegalArgumentException("ViewStub must have a valid layoutResource");
        }
        ViewGroup parent = (ViewGroup) viewParent;
        LayoutInflater inflater = mInflater != null ? mInflater : LayoutInflater.from(getContext());
        View view = inflater.inflate(mLayoutResource, parent, false);
        if (mInflatedId != NO_ID) {
            view.setId(mInflatedId);
        }
        int index = parent.indexOfChild(this);
        parent.removeViewInLayout(this);
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp != null) {
            parent.addView(view, index, lp);
        } else {
            parent.addView(view, index);
        }
        mInflated = new java.lang.ref.WeakReference<View>(view);
        if (mInflateListener != null) {
            mInflateListener.onInflate(this, view);
        }
        return view;
    }
}
