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
package androidx.fragment.app;

import android.app.FragmentManagerImpl;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/// The container AndroidX recommends for fragments. With `android:name` (or
/// `class`) it adds that fragment when it is inflated, through the manager
/// of the activity or fragment whose layout holds it; on recreation the
/// restored fragment goes back in instead.
public final class FragmentContainerView extends FrameLayout {

    private android.app.FragmentManager mManager;

    public FragmentContainerView(Context context) {
        super(context);
    }

    public FragmentContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FragmentContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null && fragmentName(context, attrs) != null) {
            throw new UnsupportedOperationException("FragmentContainerView must be within a FragmentActivity to use "
                    + "android:name=\"" + fragmentName(context, attrs) + "\"");
        }
    }

    /// Runtime use: inflated by `manager`, which adds the named fragment.
    public FragmentContainerView(Context context, AttributeSet attrs, FragmentManagerImpl manager) {
        super(context, attrs);
        mManager = manager;
        String name = fragmentName(context, attrs);
        if (name != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.Fragment);
            String tag = a.getString(android.R.styleable.Fragment_tag);
            a.recycle();
            manager.addToInflatingContainer(this, name, tag, context, attrs);
        }
    }

    private static String fragmentName(Context context, AttributeSet attrs) {
        String name = attrs.getAttributeValue("", "class");
        if (name == null) {
            TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.Fragment);
            name = a.getString(android.R.styleable.Fragment_name);
            a.recycle();
        }
        return name;
    }

    /// The fragment most recently added to this container.
    @SuppressWarnings("unchecked")
    public <F extends Fragment> F getFragment() {
        if (mManager == null) {
            return null;
        }
        android.app.Fragment f = mManager.findFragmentById(getId());
        return f instanceof Fragment ? (F) f : null;
    }
}
