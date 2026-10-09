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
package androidx.recyclerview.widget;

/// Merges consecutive updates of the same kind into ranges before passing
/// them on; call [#dispatchLastEvent()] when done.
public class BatchingListUpdateCallback implements ListUpdateCallback {

    private static final int TYPE_NONE = 0;
    private static final int TYPE_ADD = 1;
    private static final int TYPE_REMOVE = 2;
    private static final int TYPE_CHANGE = 3;

    final ListUpdateCallback mWrapped;
    int mLastEventType = TYPE_NONE;
    int mLastEventPosition = -1;
    int mLastEventCount = -1;
    Object mLastEventPayload;

    public BatchingListUpdateCallback(ListUpdateCallback callback) {
        mWrapped = callback;
    }

    public void dispatchLastEvent() {
        if (mLastEventType == TYPE_NONE) {
            return;
        }
        switch (mLastEventType) {
            case TYPE_ADD:
                mWrapped.onInserted(mLastEventPosition, mLastEventCount);
                break;
            case TYPE_REMOVE:
                mWrapped.onRemoved(mLastEventPosition, mLastEventCount);
                break;
            case TYPE_CHANGE:
                mWrapped.onChanged(mLastEventPosition, mLastEventCount, mLastEventPayload);
                break;
            default:
                break;
        }
        mLastEventPayload = null;
        mLastEventType = TYPE_NONE;
    }

    @Override
    public void onInserted(int position, int count) {
        if (mLastEventType == TYPE_ADD && position >= mLastEventPosition
                && position <= mLastEventPosition + mLastEventCount) {
            mLastEventCount += count;
            mLastEventPosition = Math.min(position, mLastEventPosition);
            return;
        }
        dispatchLastEvent();
        mLastEventPosition = position;
        mLastEventCount = count;
        mLastEventType = TYPE_ADD;
    }

    @Override
    public void onRemoved(int position, int count) {
        if (mLastEventType == TYPE_REMOVE && mLastEventPosition >= position
                && mLastEventPosition <= position + count) {
            mLastEventCount += count;
            mLastEventPosition = position;
            return;
        }
        dispatchLastEvent();
        mLastEventPosition = position;
        mLastEventCount = count;
        mLastEventType = TYPE_REMOVE;
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
        dispatchLastEvent();
        mWrapped.onMoved(fromPosition, toPosition);
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
        if (mLastEventType == TYPE_CHANGE
                && !(position > mLastEventPosition + mLastEventCount
                || position + count < mLastEventPosition || mLastEventPayload != payload)) {
            int previousEnd = mLastEventPosition + mLastEventCount;
            mLastEventPosition = Math.min(position, mLastEventPosition);
            mLastEventCount = Math.max(previousEnd, position + count) - mLastEventPosition;
            return;
        }
        dispatchLastEvent();
        mLastEventPosition = position;
        mLastEventCount = count;
        mLastEventPayload = payload;
        mLastEventType = TYPE_CHANGE;
    }
}
