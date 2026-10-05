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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;

/// Base of the scrolling, recycling adapter views ([ListView], [GridView]).
///
/// Only the children that are on screen exist. Scrolling moves them and
/// fills the gap left behind with views for the next positions; views that
/// leave the screen go to a recycle bin keyed by the adapter's view type and
/// come back as `convertView`. Touches scroll the list with the finger and
/// fling on release; a tap selects the item under it.
public abstract class AbsListView extends AdapterView<ListAdapter> {

    public static final int CHOICE_MODE_NONE = 0;
    public static final int CHOICE_MODE_SINGLE = 1;
    public static final int CHOICE_MODE_MULTIPLE = 2;
    public static final int CHOICE_MODE_MULTIPLE_MODAL = 3;
    public static final int TRANSCRIPT_MODE_DISABLED = 0;
    public static final int TRANSCRIPT_MODE_NORMAL = 1;
    public static final int TRANSCRIPT_MODE_ALWAYS_SCROLL = 2;

    public interface OnScrollListener {
        int SCROLL_STATE_IDLE = 0;
        int SCROLL_STATE_TOUCH_SCROLL = 1;
        int SCROLL_STATE_FLING = 2;

        void onScrollStateChanged(AbsListView view, int scrollState);

        void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount);
    }

    public interface RecyclerListener {
        void onMovedToScrapHeap(View view);
    }

    public interface MultiChoiceModeListener {
    }

    /// Layout params of list children: they remember their view type.
    public static class LayoutParams extends ViewGroup.LayoutParams {
        int viewType;
        public boolean forceAdd;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(int w, int h, int viewType) {
            super(w, h);
            this.viewType = viewType;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    private static final int TOUCH_MODE_REST = -1;
    private static final int TOUCH_MODE_DOWN = 0;
    private static final int TOUCH_MODE_SCROLL = 3;
    private static final int TOUCH_MODE_FLING = 4;

    ListAdapter mAdapter;
    private AdapterDataSetObserver mDataSetObserver;
    final RecycleBin mRecycler = new RecycleBin();
    private Drawable mSelector;
    private boolean mDrawSelectorOnTop = true;
    private int mChoiceMode = CHOICE_MODE_NONE;
    private SparseBooleanArray mCheckStates;
    private int mCheckedItemCount;
    private int mTranscriptMode;
    private boolean mStackFromBottom;
    private boolean mTextFilterEnabled;
    private boolean mFastScrollEnabled;
    private int mCacheColorHint;
    private OnScrollListener mOnScrollListener;
    private RecyclerListener mRecyclerListener;
    private int mLastScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    /// A pending setSelection: the position to show at the top, and its offset.
    int mSyncPosition = INVALID_POSITION;
    int mSyncTop;

    private int mTouchMode = TOUCH_MODE_REST;
    private float mDownY;
    private float mLastY;
    private int mMotionPosition = INVALID_POSITION;
    private View mPressedChild;
    private Runnable mPendingLongPress;
    private boolean mLongPressed;
    private final long[] mSampleTime = new long[4];
    private final float[] mSampleY = new float[4];
    private int mSamples;
    private final FlingRunnable mFling = new FlingRunnable();
    private final Rect mSelectorRect = new Rect();

    public AbsListView(Context context) {
        super(context);
        init();
    }

    public AbsListView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.listViewStyle);
    }

    public AbsListView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AbsListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.AbsListView, defStyleAttr, defStyleRes);
        Drawable selector = a.getDrawable(android.R.styleable.AbsListView_listSelector);
        if (selector != null) {
            setSelector(selector);
        }
        mDrawSelectorOnTop = a.getBoolean(android.R.styleable.AbsListView_drawSelectorOnTop, true);
        mStackFromBottom = a.getBoolean(android.R.styleable.AbsListView_stackFromBottom, false);
        mTextFilterEnabled = a.getBoolean(android.R.styleable.AbsListView_textFilterEnabled, false);
        mTranscriptMode = a.getInt(android.R.styleable.AbsListView_transcriptMode, TRANSCRIPT_MODE_DISABLED);
        mCacheColorHint = a.getColor(android.R.styleable.AbsListView_cacheColorHint, 0);
        mFastScrollEnabled = a.getBoolean(android.R.styleable.AbsListView_fastScrollEnabled, false);
        setChoiceMode(a.getInt(android.R.styleable.AbsListView_choiceMode, CHOICE_MODE_NONE));
        a.recycle();
    }

    private void init() {
        setClickable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);
        setClipChildren(true);
    }

    // ------------------------------------------------------------ adapter

    @Override
    public ListAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
        resetList();
        mRecycler.clear();
        mAdapter = adapter;
        if (adapter != null) {
            mDataSetObserver = new AdapterDataSetObserver();
            adapter.registerDataSetObserver(mDataSetObserver);
            mItemCount = adapter.getCount();
        } else {
            mItemCount = 0;
        }
        if (mCheckStates != null) {
            mCheckStates.clear();
            mCheckedItemCount = 0;
        }
        mDataChanged = true;
        updateEmptyStatus();
        requestLayout();
        invalidate();
    }

    /// Drops every child and returns to the top.
    void resetList() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View c = getChildAt(i);
            removeViewInLayout(c);
        }
        mFirstPosition = 0;
        mSyncPosition = INVALID_POSITION;
        mFling.stop();
    }

    @Override
    void handleDataChanged() {
        if (mFirstPosition >= mItemCount) {
            mFirstPosition = Math.max(0, mItemCount - 1);
        }
        if (mTranscriptMode == TRANSCRIPT_MODE_ALWAYS_SCROLL && mItemCount > 0) {
            mSyncPosition = mItemCount - 1;
            mSyncTop = Integer.MIN_VALUE;
        }
    }

    // ------------------------------------------------------------ recycling

    /// Scrapped views by view type.
    final class RecycleBin {
        private final HashMap<Integer, ArrayList<View>> scrap = new HashMap<Integer, ArrayList<View>>();

        void addScrap(View v, int type) {
            if (type < 0) {
                return;
            }
            Integer key = Integer.valueOf(type);
            ArrayList<View> list = scrap.get(key);
            if (list == null) {
                list = new ArrayList<View>();
                scrap.put(key, list);
            }
            if (!list.contains(v)) {
                list.add(v);
            }
            if (mRecyclerListener != null) {
                mRecyclerListener.onMovedToScrapHeap(v);
            }
        }

        View getScrap(int type) {
            ArrayList<View> list = scrap.get(Integer.valueOf(type));
            if (list == null || list.isEmpty()) {
                return null;
            }
            return list.remove(list.size() - 1);
        }

        void clear() {
            scrap.clear();
        }
    }

    public void setRecyclerListener(RecyclerListener listener) {
        mRecyclerListener = listener;
    }

    /// The view for `position`, recycling a scrapped one of its type.
    View obtainView(int position) {
        int type = mAdapter.getItemViewType(position);
        View scrap = type >= 0 ? mRecycler.getScrap(type) : null;
        View child = mAdapter.getView(position, scrap, this);
        if (scrap != null && child != scrap) {
            mRecycler.addScrap(scrap, type);
        }
        if (child == null) {
            throw new IllegalStateException("Adapter.getView returned null for position " + position);
        }
        ViewGroup.LayoutParams vlp = child.getLayoutParams();
        LayoutParams lp;
        if (vlp == null) {
            lp = (LayoutParams) generateDefaultLayoutParams();
        } else if (!(vlp instanceof LayoutParams)) {
            lp = new LayoutParams(vlp);
        } else {
            lp = (LayoutParams) vlp;
        }
        lp.viewType = type;
        child.setLayoutParams(lp);
        return child;
    }

    /// Removes `child` and puts it in the recycle bin.
    void recycleChild(View child) {
        removeViewInLayout(child);
        child.setPressed(false);
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        if (lp instanceof LayoutParams) {
            mRecycler.addScrap(child, ((LayoutParams) lp).viewType);
        }
    }

    void recycleAllChildren() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            recycleChild(getChildAt(i));
        }
    }

    /// Applies the checked state of `position` to its view.
    void applyCheckState(View child, int position) {
        if (mChoiceMode != CHOICE_MODE_NONE && mCheckStates != null) {
            boolean checked = mCheckStates.get(position);
            if (child instanceof Checkable) {
                ((Checkable) child).setChecked(checked);
            } else {
                child.setActivated(checked);
            }
        }
    }

    // ------------------------------------------------------------ layout

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).forceLayout();
            }
        }
        layoutChildren();
        mDataChanged = false;
        invokeOnItemScrollListener();
    }

    /// Lays out the visible children for the current first position.
    protected abstract void layoutChildren();

    /// Fills the space a scroll uncovered, below the last child when `down`.
    abstract void fillGap(boolean down);

    /// How many children share a row: 1 for a list, the column count for a grid.
    int rowSize() {
        return 1;
    }

    int listTop() {
        return getPaddingTop();
    }

    int listBottom() {
        return getHeight() - getPaddingBottom();
    }

    /// Scrolls the children by `deltaY` pixels, recycling those that leave
    /// and filling the gap. Returns true when an edge stopped the scroll.
    boolean trackMotionScroll(int deltaY) {
        int count = getChildCount();
        if (count == 0 || deltaY == 0) {
            return true;
        }
        int firstTop = getChildAt(0).getTop();
        int lastBottom = maxBottom(count - rowSizeOfLastRow(count), count);
        int top = listTop();
        int bottom = listBottom();
        boolean edge = false;
        if (deltaY > 0 && mFirstPosition == 0) {
            int room = top - firstTop;
            if (room <= 0) {
                return true;
            }
            if (deltaY > room) {
                deltaY = room;
                edge = true;
            }
        }
        if (deltaY < 0 && mFirstPosition + count >= mItemCount) {
            int room = lastBottom - bottom;
            if (room <= 0) {
                return true;
            }
            if (-deltaY > room) {
                deltaY = -room;
                edge = true;
            }
        }
        offsetChildrenTopAndBottom(deltaY);
        removeOffscreen();
        fillGap(deltaY < 0);
        invokeOnItemScrollListener();
        invalidate();
        return edge;
    }

    private int rowSizeOfLastRow(int count) {
        int rs = rowSize();
        int rem = count % rs;
        return rem == 0 ? Math.min(rs, count) : rem;
    }

    int maxBottom(int from, int to) {
        int b = Integer.MIN_VALUE;
        for (int i = Math.max(0, from); i < to; i++) {
            b = Math.max(b, getChildAt(i).getBottom());
        }
        return b;
    }

    public void offsetChildrenTopAndBottom(int offset) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            getChildAt(i).offsetTopAndBottom(offset);
        }
    }

    /// Recycles whole rows that scrolled completely out of view.
    void removeOffscreen() {
        int rs = rowSize();
        int top = listTop();
        int bottom = listBottom();
        while (getChildCount() > rs) {
            if (maxBottom(0, rs) > top) {
                break;
            }
            for (int i = 0; i < rs; i++) {
                recycleChild(getChildAt(0));
            }
            mFirstPosition += rs;
        }
        while (getChildCount() > rs) {
            int count = getChildCount();
            int lastRowStart = ((count - 1) / rs) * rs;
            if (getChildAt(lastRowStart).getTop() < bottom) {
                break;
            }
            for (int i = count - 1; i >= lastRowStart; i--) {
                recycleChild(getChildAt(i));
            }
        }
    }

    public void setSelection(int position) {
        setSelectionFromTop(position, 0);
    }

    public void setSelectionFromTop(int position, int y) {
        if (mAdapter == null) {
            return;
        }
        mFling.stop();
        mSyncPosition = Math.max(0, Math.min(position, mItemCount - 1));
        mSyncTop = y;
        requestLayout();
    }

    @Override
    public View getSelectedView() {
        if (mSelectedPosition >= mFirstPosition && mSelectedPosition < mFirstPosition + getChildCount()) {
            return getChildAt(mSelectedPosition - mFirstPosition);
        }
        return null;
    }

    public void smoothScrollToPosition(int position) {
        if (position >= mFirstPosition && position < mFirstPosition + getChildCount()) {
            View v = getChildAt(position - mFirstPosition);
            if (v.getTop() >= listTop() && v.getBottom() <= listBottom()) {
                return;
            }
            int delta = v.getTop() < listTop() ? listTop() - v.getTop() : listBottom() - v.getBottom();
            smoothScrollBy(-delta, 250);
            return;
        }
        setSelection(position);
    }

    public void smoothScrollToPositionFromTop(int position, int offset) {
        setSelectionFromTop(position, offset);
    }

    public void smoothScrollToPositionFromTop(int position, int offset, int duration) {
        setSelectionFromTop(position, offset);
    }

    /// Scrolls by `distance` pixels (positive moves the content up) over
    /// roughly `duration` milliseconds.
    public void smoothScrollBy(final int distance, final int duration) {
        mFling.stop();
        final long start = System.currentTimeMillis();
        final int[] done = {0};
        post(new Runnable() {
            @Override
            public void run() {
                float f = duration <= 0 ? 1f : Math.min(1f, (System.currentTimeMillis() - start) / (float) duration);
                int target = Math.round(distance * f);
                int step = target - done[0];
                done[0] = target;
                boolean edge = trackMotionScroll(-step);
                if (f < 1f && !edge) {
                    postDelayed(this, 16);
                }
            }
        });
    }

    public void scrollListBy(int y) {
        trackMotionScroll(-y);
    }

    public boolean canScrollList(int direction) {
        return canScrollVertically(direction);
    }

    @Override
    public boolean canScrollVertically(int direction) {
        int count = getChildCount();
        if (count == 0) {
            return false;
        }
        if (direction < 0) {
            return mFirstPosition > 0 || getChildAt(0).getTop() < listTop();
        }
        return mFirstPosition + count < mItemCount || maxBottom(0, count) > listBottom();
    }

    public int pointToPosition(int x, int y) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View c = getChildAt(i);
            if (c.getVisibility() == VISIBLE && x >= c.getLeft() && x < c.getRight() && y >= c.getTop()
                    && y < c.getBottom()) {
                return mFirstPosition + i;
            }
        }
        return INVALID_POSITION;
    }

    public long pointToRowId(int x, int y) {
        int p = pointToPosition(x, y);
        return p >= 0 && mAdapter != null ? mAdapter.getItemId(p) : INVALID_ROW_ID;
    }

    // ------------------------------------------------------------ scroll listener

    public void setOnScrollListener(OnScrollListener l) {
        mOnScrollListener = l;
        invokeOnItemScrollListener();
    }

    void invokeOnItemScrollListener() {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(this, mFirstPosition, getChildCount(), mItemCount);
        }
    }

    void reportScrollStateChange(int newState) {
        if (newState != mLastScrollState) {
            mLastScrollState = newState;
            if (mOnScrollListener != null) {
                mOnScrollListener.onScrollStateChanged(this, newState);
            }
        }
    }

    // ------------------------------------------------------------ touch

    private void addSample(float y) {
        if (mSamples == mSampleY.length) {
            System.arraycopy(mSampleY, 1, mSampleY, 0, mSamples - 1);
            System.arraycopy(mSampleTime, 1, mSampleTime, 0, mSamples - 1);
            mSamples--;
        }
        mSampleY[mSamples] = y;
        mSampleTime[mSamples] = System.currentTimeMillis();
        mSamples++;
    }

    /// Finger velocity in pixels per second over the recent samples.
    private float velocity() {
        if (mSamples < 2) {
            return 0;
        }
        long dt = mSampleTime[mSamples - 1] - mSampleTime[0];
        if (dt <= 0) {
            return 0;
        }
        if (System.currentTimeMillis() - mSampleTime[mSamples - 1] > 100) {
            return 0;
        }
        return (mSampleY[mSamples - 1] - mSampleY[0]) * 1000f / dt;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            boolean wasFlinging = mTouchMode == TOUCH_MODE_FLING;
            mFling.stop();
            mDownY = ev.getY();
            mLastY = mDownY;
            mSamples = 0;
            addSample(mDownY);
            mTouchMode = TOUCH_MODE_DOWN;
            if (wasFlinging) {
                mTouchMode = TOUCH_MODE_SCROLL;
                return true;
            }
            return false;
        }
        if (action == MotionEvent.ACTION_MOVE && mTouchMode == TOUCH_MODE_DOWN) {
            if (Math.abs(ev.getY() - mDownY) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                startScroll(ev.getY());
                return true;
            }
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (mTouchMode != TOUCH_MODE_SCROLL) {
                mTouchMode = TOUCH_MODE_REST;
            }
        }
        return mTouchMode == TOUCH_MODE_SCROLL;
    }

    private void startScroll(float y) {
        mTouchMode = TOUCH_MODE_SCROLL;
        mLastY = y;
        unpress();
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return isClickable();
        }
        float x = ev.getX();
        float y = ev.getY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mFling.stop();
                mDownY = y;
                mLastY = y;
                mSamples = 0;
                addSample(y);
                mTouchMode = TOUCH_MODE_DOWN;
                mLongPressed = false;
                mMotionPosition = pointToPosition((int) x, (int) y);
                if (mMotionPosition >= 0 && mAdapter != null && mAdapter.isEnabled(mMotionPosition)) {
                    press(getChildAt(mMotionPosition - mFirstPosition));
                    scheduleLongPress();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                addSample(y);
                if (mTouchMode == TOUCH_MODE_DOWN
                        && Math.abs(y - mDownY) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    startScroll(y);
                }
                if (mTouchMode == TOUCH_MODE_DOWN && mPressedChild != null && !pointInList(x, y)) {
                    // Dragged off the list without scrolling it: as on
                    // Android the press and the pending long press end, so
                    // the release clicks nothing. Android's list would still
                    // click if the pointer came back before the release;
                    // this follows a plain View instead, which does not, so
                    // a press once abandoned stays abandoned.
                    cancelLongPress();
                    unpress();
                }
                if (mTouchMode == TOUCH_MODE_SCROLL) {
                    int dy = (int) (y - mLastY);
                    if (dy != 0) {
                        trackMotionScroll(dy);
                        mLastY += dy;
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                addSample(y);
                cancelLongPress();
                if (mTouchMode == TOUCH_MODE_DOWN) {
                    final View child = mPressedChild;
                    final int position = mMotionPosition;
                    // A release beside the list clicks nothing, as on Android,
                    // which checks the release against the list's padding.
                    boolean inList = x >= getPaddingLeft() && x < getWidth() - getPaddingRight();
                    if (inList && child != null && position >= 0 && !mLongPressed && mAdapter != null
                            && position < mItemCount && mAdapter.isEnabled(position)) {
                        performItemClick(child, position, mAdapter.getItemId(position));
                    }
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            unpress();
                        }
                    }, ViewConfiguration.getPressedStateDuration());
                    mTouchMode = TOUCH_MODE_REST;
                } else if (mTouchMode == TOUCH_MODE_SCROLL) {
                    float v = velocity();
                    float min = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
                    if (Math.abs(v) > min) {
                        mTouchMode = TOUCH_MODE_FLING;
                        reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
                        mFling.start(v);
                    } else {
                        mTouchMode = TOUCH_MODE_REST;
                        reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancelLongPress();
                unpress();
                if (mTouchMode == TOUCH_MODE_SCROLL) {
                    reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                }
                mTouchMode = TOUCH_MODE_REST;
                return true;
            default:
                return true;
        }
    }

    /// Whether `(x, y)` is within the list, widened by the touch slop on
    /// every side as Android's `pointInView(x, y, slop)` is.
    private boolean pointInList(float x, float y) {
        float slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        return x >= -slop && y >= -slop && x < getWidth() + slop && y < getHeight() + slop;
    }

    private void press(View child) {
        unpress();
        if (child != null) {
            mPressedChild = child;
            child.setPressed(true);
            invalidate();
        }
    }

    private void unpress() {
        if (mPressedChild != null) {
            mPressedChild.setPressed(false);
            mPressedChild = null;
            invalidate();
        }
    }

    private void scheduleLongPress() {
        cancelLongPress();
        if (!isLongClickable()) {
            return;
        }
        final int position = mMotionPosition;
        mPendingLongPress = new Runnable() {
            @Override
            public void run() {
                if (mTouchMode == TOUCH_MODE_DOWN && mPressedChild != null && mAdapter != null
                        && position < mItemCount) {
                    if (performItemLongClick(mPressedChild, position, mAdapter.getItemId(position))) {
                        mLongPressed = true;
                        unpress();
                    }
                }
            }
        };
        postDelayed(mPendingLongPress, ViewConfiguration.getLongPressTimeout());
    }

    private void cancelLongPress() {
        if (mPendingLongPress != null) {
            removeCallbacks(mPendingLongPress);
            mPendingLongPress = null;
        }
    }

    /// Decelerating scroll after the finger lifts.
    private final class FlingRunnable implements Runnable {
        private float velocity;
        private long last;
        private boolean running;

        void start(float v) {
            velocity = v;
            last = System.currentTimeMillis();
            running = true;
            postDelayed(this, 16);
        }

        void stop() {
            if (running) {
                running = false;
                removeCallbacks(this);
                if (mTouchMode == TOUCH_MODE_FLING) {
                    mTouchMode = TOUCH_MODE_REST;
                }
                reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            }
        }

        @Override
        public void run() {
            if (!running) {
                return;
            }
            long now = System.currentTimeMillis();
            float dt = Math.max(1, now - last) / 1000f;
            last = now;
            int dy = Math.round(velocity * dt);
            boolean edge = dy != 0 && trackMotionScroll(dy);
            float density = getResources().getDisplayMetrics().density;
            float decel = 2000f * density * dt;
            if (velocity > 0) {
                velocity = Math.max(0, velocity - decel);
            } else {
                velocity = Math.min(0, velocity + decel);
            }
            if (edge || Math.abs(velocity) < 20 * density) {
                stop();
            } else {
                postDelayed(this, 16);
            }
        }
    }

    // ------------------------------------------------------------ selector

    public void setSelector(Drawable sel) {
        if (mSelector != null) {
            mSelector.setCallback(null);
        }
        mSelector = sel;
        if (sel != null) {
            sel.setCallback(this);
        }
        invalidate();
    }

    public void setSelector(int resID) {
        setSelector(getContext().getDrawable(resID));
    }

    public Drawable getSelector() {
        return mSelector;
    }

    public void setDrawSelectorOnTop(boolean onTop) {
        mDrawSelectorOnTop = onTop;
    }

    @Override
    protected boolean verifyDrawable(Drawable dr) {
        return dr == mSelector || super.verifyDrawable(dr);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        View c = mPressedChild;
        if (mSelector != null && c != null && c.getParent() == this) {
            mSelectorRect.set(c.getLeft(), c.getTop(), c.getRight(), c.getBottom());
            mSelector.setBounds(mSelectorRect);
            mSelector.setState(new int[] {android.R.attr.state_pressed, android.R.attr.state_enabled});
            mSelector.draw(canvas);
        }
    }

    // ------------------------------------------------------------ choice

    public void setChoiceMode(int choiceMode) {
        mChoiceMode = choiceMode;
        if (choiceMode != CHOICE_MODE_NONE && mCheckStates == null) {
            mCheckStates = new SparseBooleanArray(0);
        }
    }

    public int getChoiceMode() {
        return mChoiceMode;
    }

    public void setMultiChoiceModeListener(MultiChoiceModeListener listener) {
    }

    @Override
    public boolean performItemClick(View view, int position, long id) {
        if (mChoiceMode == CHOICE_MODE_MULTIPLE || mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
            setItemChecked(position, !mCheckStates.get(position));
        } else if (mChoiceMode == CHOICE_MODE_SINGLE) {
            setItemChecked(position, true);
        }
        return super.performItemClick(view, position, id);
    }

    public void setItemChecked(int position, boolean value) {
        if (mChoiceMode == CHOICE_MODE_NONE) {
            return;
        }
        if (mChoiceMode == CHOICE_MODE_SINGLE) {
            if (value) {
                mCheckStates.clear();
                mCheckStates.put(position, true);
                mCheckedItemCount = 1;
            } else if (mCheckStates.get(position)) {
                mCheckStates.clear();
                mCheckedItemCount = 0;
            }
        } else {
            boolean old = mCheckStates.get(position);
            mCheckStates.put(position, value);
            if (old != value) {
                mCheckedItemCount += value ? 1 : -1;
            }
        }
        for (int i = 0; i < getChildCount(); i++) {
            applyCheckState(getChildAt(i), mFirstPosition + i);
        }
        invalidate();
    }

    public boolean isItemChecked(int position) {
        return mCheckStates != null && mCheckStates.get(position);
    }

    public int getCheckedItemPosition() {
        if (mChoiceMode == CHOICE_MODE_SINGLE && mCheckStates != null && mCheckStates.size() == 1) {
            return mCheckStates.keyAt(0);
        }
        return INVALID_POSITION;
    }

    public SparseBooleanArray getCheckedItemPositions() {
        return mChoiceMode != CHOICE_MODE_NONE ? mCheckStates : null;
    }

    public long[] getCheckedItemIds() {
        if (mCheckStates == null || mAdapter == null) {
            return new long[0];
        }
        ArrayList<Long> ids = new ArrayList<Long>();
        for (int i = 0; i < mCheckStates.size(); i++) {
            if (mCheckStates.valueAt(i)) {
                ids.add(Long.valueOf(mAdapter.getItemId(mCheckStates.keyAt(i))));
            }
        }
        long[] out = new long[ids.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = ids.get(i).longValue();
        }
        return out;
    }

    public int getCheckedItemCount() {
        return mCheckedItemCount;
    }

    public void clearChoices() {
        if (mCheckStates != null) {
            mCheckStates.clear();
        }
        mCheckedItemCount = 0;
        for (int i = 0; i < getChildCount(); i++) {
            applyCheckState(getChildAt(i), mFirstPosition + i);
        }
    }

    // ------------------------------------------------------------ misc API

    public void setTranscriptMode(int mode) {
        mTranscriptMode = mode;
    }

    public int getTranscriptMode() {
        return mTranscriptMode;
    }

    public void setStackFromBottom(boolean stackFromBottom) {
        mStackFromBottom = stackFromBottom;
    }

    public boolean isStackFromBottom() {
        return mStackFromBottom;
    }

    public void setTextFilterEnabled(boolean textFilterEnabled) {
        mTextFilterEnabled = textFilterEnabled;
    }

    public boolean isTextFilterEnabled() {
        return mTextFilterEnabled;
    }

    public void setFilterText(String filterText) {
        if (mAdapter instanceof Filterable) {
            ((Filterable) mAdapter).getFilter().filter(filterText);
        }
    }

    public void clearTextFilter() {
        setFilterText(null);
    }

    public void setFastScrollEnabled(boolean enabled) {
        mFastScrollEnabled = enabled;
    }

    public boolean isFastScrollEnabled() {
        return mFastScrollEnabled;
    }

    public void setFastScrollAlwaysVisible(boolean alwaysShow) {
    }

    public void setSmoothScrollbarEnabled(boolean enabled) {
    }

    public void setScrollingCacheEnabled(boolean enabled) {
    }

    public void setCacheColorHint(int color) {
        mCacheColorHint = color;
    }

    public int getCacheColorHint() {
        return mCacheColorHint;
    }

    public void invalidateViews() {
        mDataChanged = true;
        requestLayout();
        invalidate();
    }

    public void reclaimViews(java.util.List<View> views) {
        for (int i = 0; i < getChildCount(); i++) {
            views.add(getChildAt(i));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFling.stop();
        cancelLongPress();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return AbsListView.class.getName();
    }
}
