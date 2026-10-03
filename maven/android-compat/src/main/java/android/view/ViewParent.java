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

/// The parent of a view: in practice always a [ViewGroup].
public interface ViewParent {
    void requestLayout();

    boolean isLayoutRequested();

    ViewParent getParent();

    void requestDisallowInterceptTouchEvent(boolean disallowIntercept);

    void requestChildFocus(View child, View focused);

    void clearChildFocus(View child);

    void focusableViewAvailable(View v);

    void childDrawableStateChanged(View child);

    boolean getChildVisibleRect(View child, android.graphics.Rect r, android.graphics.Point offset);

    void bringChildToFront(View child);

    void invalidateChild(View child, android.graphics.Rect r);

    void recomputeViewAttributes(View child);

    boolean showContextMenuForChild(View originalView);

    void requestTransparentRegion(View child);

    void childHasTransientStateChanged(View child, boolean hasTransientState);

    void requestFitSystemWindows();

    ViewParent getParentForAccessibility();

    void notifySubtreeAccessibilityStateChanged(View child, View source, int changeType);

    boolean canResolveLayoutDirection();

    boolean isLayoutDirectionResolved();

    int getLayoutDirection();
}
