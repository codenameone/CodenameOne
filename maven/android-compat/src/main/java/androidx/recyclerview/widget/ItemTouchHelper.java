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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.List;

/// Swipe-to-dismiss and drag-to-reorder for a [RecyclerView]'s items, driven
/// by a [Callback] that says which directions each item allows and what a
/// move or a swipe means to the data.
public class ItemTouchHelper extends RecyclerView.ItemDecoration
        implements RecyclerView.OnChildAttachStateChangeListener {

    public static final int UP = 1;
    public static final int DOWN = 1 << 1;
    public static final int LEFT = 1 << 2;
    public static final int RIGHT = 1 << 3;
    public static final int START = LEFT << 2;
    public static final int END = RIGHT << 2;
    public static final int ACTION_STATE_IDLE = 0;
    public static final int ACTION_STATE_SWIPE = 1;
    public static final int ACTION_STATE_DRAG = 2;
    public static final int ANIMATION_TYPE_SWIPE_SUCCESS = 1 << 1;
    public static final int ANIMATION_TYPE_SWIPE_CANCEL = 1 << 2;
    public static final int ANIMATION_TYPE_DRAG = 1 << 3;
    static final int DIRECTION_FLAG_COUNT = 8;
    private static final int ACTION_MODE_IDLE_MASK = (1 << DIRECTION_FLAG_COUNT) - 1;
    static final int ACTION_MODE_SWIPE_MASK = ACTION_MODE_IDLE_MASK << DIRECTION_FLAG_COUNT;
    static final int ACTION_MODE_DRAG_MASK = ACTION_MODE_SWIPE_MASK << DIRECTION_FLAG_COUNT;

    /// Lets a layout manager keep a dragged item in place while the
    /// adapter moves it.
    public interface ViewDropHandler {
        void prepareForDrop(View view, View target, int x, int y);
    }

    final Callback mCallback;
    RecyclerView mRecyclerView;
    RecyclerView.ViewHolder mSelected;
    int mActionState = ACTION_STATE_IDLE;
    private int mSelectedFlags;
    float mInitialTouchX;
    float mInitialTouchY;
    float mDx;
    float mDy;
    float mSelectedStartX;
    float mSelectedStartY;
    private int mSlop;
    private VelocityTracker mVelocityTracker;
    private RecyclerView.ViewHolder mPressed;
    private boolean mLongPressPosted;
    private final List<View> mPendingCleanup = new ArrayList<View>();

    private final Runnable mLongPress = new Runnable() {
        @Override
        public void run() {
            mLongPressPosted = false;
            if (mPressed != null && mSelected == null && mCallback.isLongPressDragEnabled()
                    && mPressed.itemView.getParent() == mRecyclerView) {
                int flags = mCallback.getAbsoluteMovementFlags(mRecyclerView, mPressed);
                if ((flags & ACTION_MODE_DRAG_MASK) != 0) {
                    select(mPressed, ACTION_STATE_DRAG);
                }
            }
        }
    };

    private final RecyclerView.OnItemTouchListener mOnItemTouchListener = new RecyclerView.OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                mInitialTouchX = event.getX();
                mInitialTouchY = event.getY();
                obtainVelocityTracker();
                if (mSelected == null) {
                    View child = mRecyclerView.findChildViewUnder(event.getX(), event.getY());
                    mPressed = child == null || mRecyclerView.isHidden(child) ? null
                            : mRecyclerView.getChildViewHolder(child);
                    if (mPressed != null && mCallback.isLongPressDragEnabled()) {
                        mLongPressPosted = true;
                        mRecyclerView.postDelayed(mLongPress, ViewConfiguration.getLongPressTimeout());
                    }
                }
            } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                cancelLongPress();
                if (mSelected != null) {
                    release(event);
                }
                mPressed = null;
            } else if (action == MotionEvent.ACTION_MOVE && mSelected == null) {
                float dx = event.getX() - mInitialTouchX;
                float dy = event.getY() - mInitialTouchY;
                if (Math.abs(dx) > mSlop || Math.abs(dy) > mSlop) {
                    cancelLongPress();
                    checkSelectForSwipe(dx, dy);
                }
            }
            if (mVelocityTracker != null) {
                mVelocityTracker.addMovement(event);
            }
            return mSelected != null;
        }

        @Override
        public void onTouchEvent(RecyclerView recyclerView, MotionEvent event) {
            if (mVelocityTracker != null) {
                mVelocityTracker.addMovement(event);
            }
            if (mSelected == null) {
                return;
            }
            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    updateDxDy(event);
                    if (mActionState == ACTION_STATE_DRAG) {
                        moveIfNecessary();
                    }
                    mRecyclerView.invalidate();
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    release(event);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            if (disallowIntercept) {
                cancelLongPress();
            }
        }
    };

    public ItemTouchHelper(Callback callback) {
        mCallback = callback;
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        if (mRecyclerView == recyclerView) {
            return;
        }
        if (mRecyclerView != null) {
            mRecyclerView.removeItemDecoration(this);
            mRecyclerView.removeOnItemTouchListener(mOnItemTouchListener);
            mRecyclerView.removeOnChildAttachStateChangeListener(this);
            cancelLongPress();
        }
        mRecyclerView = recyclerView;
        if (recyclerView != null) {
            mSlop = ViewConfiguration.get(recyclerView.getContext()).getScaledTouchSlop();
            mRecyclerView.addItemDecoration(this);
            mRecyclerView.addOnItemTouchListener(mOnItemTouchListener);
            mRecyclerView.addOnChildAttachStateChangeListener(this);
        }
    }

    private void cancelLongPress() {
        if (mLongPressPosted && mRecyclerView != null) {
            mRecyclerView.removeCallbacks(mLongPress);
        }
        mLongPressPosted = false;
    }

    private void obtainVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
        }
        mVelocityTracker = VelocityTracker.obtain();
    }

    public void startDrag(RecyclerView.ViewHolder viewHolder) {
        if (!mCallback.hasDragFlag(mRecyclerView, viewHolder) || viewHolder.itemView.getParent() != mRecyclerView) {
            return;
        }
        obtainVelocityTracker();
        mDx = 0;
        mDy = 0;
        select(viewHolder, ACTION_STATE_DRAG);
    }

    public void startSwipe(RecyclerView.ViewHolder viewHolder) {
        if (!mCallback.hasSwipeFlag(mRecyclerView, viewHolder) || viewHolder.itemView.getParent() != mRecyclerView) {
            return;
        }
        obtainVelocityTracker();
        mDx = 0;
        mDy = 0;
        select(viewHolder, ACTION_STATE_SWIPE);
    }

    private void checkSelectForSwipe(float dx, float dy) {
        if (mPressed == null || !mCallback.isItemViewSwipeEnabled()
                || mRecyclerView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING) {
            return;
        }
        int swipeFlags = (mCallback.getAbsoluteMovementFlags(mRecyclerView, mPressed) & ACTION_MODE_SWIPE_MASK)
                >> (ACTION_STATE_SWIPE * DIRECTION_FLAG_COUNT);
        if (swipeFlags == 0) {
            return;
        }
        float absDx = Math.abs(dx);
        float absDy = Math.abs(dy);
        if (absDx > absDy) {
            if ((dx < 0 && (swipeFlags & LEFT) == 0) || (dx > 0 && (swipeFlags & RIGHT) == 0)) {
                return;
            }
        } else {
            if ((dy < 0 && (swipeFlags & UP) == 0) || (dy > 0 && (swipeFlags & DOWN) == 0)) {
                return;
            }
        }
        mDx = 0;
        mDy = 0;
        select(mPressed, ACTION_STATE_SWIPE);
    }

    void select(RecyclerView.ViewHolder selected, int actionState) {
        if (selected == mSelected && actionState == mActionState) {
            return;
        }
        mActionState = actionState;
        mSelected = selected;
        mSelectedFlags = (mCallback.getAbsoluteMovementFlags(mRecyclerView, selected)
                & ((1 << (DIRECTION_FLAG_COUNT + DIRECTION_FLAG_COUNT * actionState)) - 1))
                >> (actionState * DIRECTION_FLAG_COUNT);
        mSelectedStartX = selected.itemView.getLeft();
        mSelectedStartY = selected.itemView.getTop();
        mRecyclerView.getParent().requestDisallowInterceptTouchEvent(true);
        mCallback.onSelectedChanged(selected, actionState);
        mRecyclerView.invalidate();
    }

    void updateDxDy(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        mDx = x - mInitialTouchX;
        mDy = y - mInitialTouchY;
        if ((mSelectedFlags & LEFT) == 0) {
            mDx = Math.max(0, mDx);
        }
        if ((mSelectedFlags & RIGHT) == 0) {
            mDx = Math.min(0, mDx);
        }
        if ((mSelectedFlags & UP) == 0) {
            mDy = Math.max(0, mDy);
        }
        if ((mSelectedFlags & DOWN) == 0) {
            mDy = Math.min(0, mDy);
        }
    }

    private float selectedDx() {
        return mSelectedStartX + mDx - mSelected.itemView.getLeft();
    }

    private float selectedDy() {
        return mSelectedStartY + mDy - mSelected.itemView.getTop();
    }

    void moveIfNecessary() {
        RecyclerView.ViewHolder viewHolder = mSelected;
        if (mRecyclerView.isLayoutRequested() || viewHolder.getAbsoluteAdapterPosition() == RecyclerView.NO_POSITION) {
            return;
        }
        int left = (int) (mSelectedStartX + mDx);
        int top = (int) (mSelectedStartY + mDy);
        View view = viewHolder.itemView;
        float threshold = mCallback.getMoveThreshold(viewHolder);
        if (Math.abs(top - view.getTop()) < view.getHeight() * threshold
                && Math.abs(left - view.getLeft()) < view.getWidth() * threshold) {
            return;
        }
        int centerX = left + view.getWidth() / 2;
        int centerY = top + view.getHeight() / 2;
        RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
        List<RecyclerView.ViewHolder> candidates = new ArrayList<RecyclerView.ViewHolder>();
        for (int i = 0; i < lm.getChildCount(); i++) {
            View other = lm.getChildAt(i);
            if (other == view) {
                continue;
            }
            if (centerX >= other.getLeft() && centerX < other.getRight() && centerY >= other.getTop()
                    && centerY < other.getBottom()) {
                RecyclerView.ViewHolder otherVh = mRecyclerView.getChildViewHolder(other);
                if (mCallback.canDropOver(mRecyclerView, viewHolder, otherVh)) {
                    candidates.add(otherVh);
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        RecyclerView.ViewHolder target = mCallback.chooseDropTarget(viewHolder, candidates, left, top);
        if (target == null) {
            return;
        }
        int toPosition = target.getAbsoluteAdapterPosition();
        int fromPosition = viewHolder.getAbsoluteAdapterPosition();
        if (mCallback.onMove(mRecyclerView, viewHolder, target)) {
            mCallback.onMoved(mRecyclerView, viewHolder, fromPosition, target, toPosition, left, top);
        }
    }

    private void release(MotionEvent event) {
        final RecyclerView.ViewHolder prev = mSelected;
        final int prevActionState = mActionState;
        if (prev == null) {
            return;
        }
        int swipeDir = 0;
        if (prevActionState == ACTION_STATE_SWIPE) {
            swipeDir = swipeIfNecessary(prev);
        }
        mSelected = null;
        mActionState = ACTION_STATE_IDLE;
        final View view = prev.itemView;
        float targetX = 0;
        float targetY = 0;
        final int finalSwipeDir = swipeDir;
        if (swipeDir != 0) {
            if (swipeDir == LEFT || swipeDir == RIGHT) {
                targetX = swipeDir == LEFT ? -mRecyclerView.getWidth() : mRecyclerView.getWidth();
            } else {
                targetY = swipeDir == UP ? -mRecyclerView.getHeight() : mRecyclerView.getHeight();
            }
        }
        long duration = mCallback.getAnimationDuration(mRecyclerView,
                prevActionState == ACTION_STATE_DRAG ? ANIMATION_TYPE_DRAG
                        : swipeDir != 0 ? ANIMATION_TYPE_SWIPE_SUCCESS : ANIMATION_TYPE_SWIPE_CANCEL,
                targetX - view.getTranslationX(), targetY - view.getTranslationY());
        prev.setIsRecyclable(false);
        view.animate().translationX(targetX).translationY(targetY).setDuration(duration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.animate().setListener(null);
                        prev.setIsRecyclable(true);
                        if (finalSwipeDir != 0) {
                            mPendingCleanup.add(view);
                            mCallback.onSwiped(prev, finalSwipeDir);
                        } else {
                            mCallback.clearView(mRecyclerView, prev);
                        }
                    }
                }).start();
        mCallback.onSelectedChanged(null, ACTION_STATE_IDLE);
        mRecyclerView.invalidate();
    }

    private int swipeIfNecessary(RecyclerView.ViewHolder viewHolder) {
        int originalMovementFlags = mCallback.getMovementFlags(mRecyclerView, viewHolder);
        int absoluteMovementFlags = Callback.convertToAbsoluteDirection(originalMovementFlags,
                mRecyclerView.getLayoutDirection());
        int flags = (absoluteMovementFlags & ACTION_MODE_SWIPE_MASK) >> (ACTION_STATE_SWIPE * DIRECTION_FLAG_COUNT);
        if (flags == 0) {
            return 0;
        }
        float velocityX = 0;
        float velocityY = 0;
        if (mVelocityTracker != null) {
            mVelocityTracker.computeCurrentVelocity(1000,
                    mCallback.getSwipeVelocityThreshold(ViewConfiguration.get(mRecyclerView.getContext())
                            .getScaledMaximumFlingVelocity()));
            velocityX = mVelocityTracker.getXVelocity();
            velocityY = mVelocityTracker.getYVelocity();
        }
        float escape = mCallback.getSwipeEscapeVelocity(
                ViewConfiguration.get(mRecyclerView.getContext()).getScaledMinimumFlingVelocity() * 4f);
        if (Math.abs(mDx) > Math.abs(mDy)) {
            int dir = mDx > 0 ? RIGHT : LEFT;
            if ((flags & dir) == 0) {
                return 0;
            }
            if (Math.abs(velocityX) >= escape && (velocityX > 0) == (dir == RIGHT)) {
                return dir;
            }
            float threshold = mRecyclerView.getWidth() * mCallback.getSwipeThreshold(viewHolder);
            return Math.abs(mDx) > threshold ? dir : 0;
        }
        int dir = mDy > 0 ? DOWN : UP;
        if ((flags & dir) == 0) {
            return 0;
        }
        if (Math.abs(velocityY) >= escape && (velocityY > 0) == (dir == DOWN)) {
            return dir;
        }
        float threshold = mRecyclerView.getHeight() * mCallback.getSwipeThreshold(viewHolder);
        return Math.abs(mDy) > threshold ? dir : 0;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mSelected != null) {
            mCallback.onChildDraw(c, parent, mSelected, selectedDx(), selectedDy(), mActionState, true);
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mSelected != null) {
            mCallback.onChildDrawOver(c, parent, mSelected, selectedDx(), selectedDy(), mActionState, true);
        }
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        if (mPendingCleanup.remove(view)) {
            RecyclerView.ViewHolder holder = RecyclerView.getChildViewHolderInt(view);
            if (holder != null) {
                mCallback.clearView(mRecyclerView, holder);
            } else {
                view.setTranslationX(0);
                view.setTranslationY(0);
            }
        }
        if (mSelected != null && view == mSelected.itemView) {
            mSelected = null;
            mActionState = ACTION_STATE_IDLE;
        }
    }

    @Override
    public void getItemOffsets(android.graphics.Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        outRect.set(0, 0, 0, 0);
    }

    // ------------------------------------------------------------ Callback

    /// What an item may do and what moving or swiping it means.
    public abstract static class Callback {
        public static final int DEFAULT_DRAG_ANIMATION_DURATION = 200;
        public static final int DEFAULT_SWIPE_ANIMATION_DURATION = 250;
        static final int RELATIVE_DIR_FLAGS = START | END | ((START | END) << DIRECTION_FLAG_COUNT)
                | ((START | END) << (2 * DIRECTION_FLAG_COUNT));
        private static final int ABS_HORIZONTAL_DIR_FLAGS = LEFT | RIGHT
                | ((LEFT | RIGHT) << DIRECTION_FLAG_COUNT) | ((LEFT | RIGHT) << (2 * DIRECTION_FLAG_COUNT));

        public static int convertToRelativeDirection(int flags, int layoutDirection) {
            int masked = flags & ABS_HORIZONTAL_DIR_FLAGS;
            if (masked == 0) {
                return flags;
            }
            flags &= ~masked;
            if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
                flags |= masked << 2;
                return flags;
            } else {
                flags |= ((masked << 1) & ~ABS_HORIZONTAL_DIR_FLAGS);
                flags |= ((masked << 1) & ABS_HORIZONTAL_DIR_FLAGS) << 2;
            }
            return flags;
        }

        public static int makeMovementFlags(int dragFlags, int swipeFlags) {
            return makeFlag(ACTION_STATE_IDLE, swipeFlags | dragFlags)
                    | makeFlag(ACTION_STATE_SWIPE, swipeFlags)
                    | makeFlag(ACTION_STATE_DRAG, dragFlags);
        }

        public static int makeFlag(int actionState, int directions) {
            return directions << (actionState * DIRECTION_FLAG_COUNT);
        }

        public static int convertToAbsoluteDirection(int flags, int layoutDirection) {
            int masked = flags & RELATIVE_DIR_FLAGS;
            if (masked == 0) {
                return flags;
            }
            flags &= ~masked;
            if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
                flags |= masked >> 2;
                return flags;
            } else {
                flags |= ((masked >> 1) & ~RELATIVE_DIR_FLAGS);
                flags |= ((masked >> 1) & RELATIVE_DIR_FLAGS) >> 2;
            }
            return flags;
        }

        public abstract int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder);

        final int getAbsoluteMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int flags = getMovementFlags(recyclerView, viewHolder);
            return convertToAbsoluteDirection(flags, recyclerView.getLayoutDirection());
        }

        boolean hasDragFlag(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return (getAbsoluteMovementFlags(recyclerView, viewHolder) & ACTION_MODE_DRAG_MASK) != 0;
        }

        boolean hasSwipeFlag(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return (getAbsoluteMovementFlags(recyclerView, viewHolder) & ACTION_MODE_SWIPE_MASK) != 0;
        }

        public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder current,
                                   RecyclerView.ViewHolder target) {
            return true;
        }

        public abstract boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                       RecyclerView.ViewHolder target);

        public boolean isLongPressDragEnabled() {
            return true;
        }

        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        public int getBoundingBoxMargin() {
            return 0;
        }

        public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
            return .5f;
        }

        public float getMoveThreshold(RecyclerView.ViewHolder viewHolder) {
            return .5f;
        }

        public float getSwipeEscapeVelocity(float defaultValue) {
            return defaultValue;
        }

        public float getSwipeVelocityThreshold(float defaultValue) {
            return defaultValue;
        }

        public RecyclerView.ViewHolder chooseDropTarget(RecyclerView.ViewHolder selected,
                                                        List<RecyclerView.ViewHolder> dropTargets, int curX,
                                                        int curY) {
            int right = curX + selected.itemView.getWidth();
            int bottom = curY + selected.itemView.getHeight();
            RecyclerView.ViewHolder winner = null;
            int winnerScore = -1;
            int dx = curX - selected.itemView.getLeft();
            int dy = curY - selected.itemView.getTop();
            for (int i = 0; i < dropTargets.size(); i++) {
                RecyclerView.ViewHolder target = dropTargets.get(i);
                if (dx > 0) {
                    int diff = target.itemView.getRight() - right;
                    if (diff < 0 && target.itemView.getRight() > selected.itemView.getRight()) {
                        int score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
                if (dx < 0) {
                    int diff = target.itemView.getLeft() - curX;
                    if (diff > 0 && target.itemView.getLeft() < selected.itemView.getLeft()) {
                        int score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
                if (dy < 0) {
                    int diff = target.itemView.getTop() - curY;
                    if (diff > 0 && target.itemView.getTop() < selected.itemView.getTop()) {
                        int score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
                if (dy > 0) {
                    int diff = target.itemView.getBottom() - bottom;
                    if (diff < 0 && target.itemView.getBottom() > selected.itemView.getBottom()) {
                        int score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
            }
            return winner;
        }

        public abstract void onSwiped(RecyclerView.ViewHolder viewHolder, int direction);

        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        }

        public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos,
                            RecyclerView.ViewHolder target, int toPos, int x, int y) {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof ViewDropHandler) {
                ((ViewDropHandler) layoutManager).prepareForDrop(viewHolder.itemView, target.itemView, x, y);
            }
        }

        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX,
                                float dY, int actionState, boolean isCurrentlyActive) {
            viewHolder.itemView.setTranslationX(dX);
            viewHolder.itemView.setTranslationY(dY);
        }

        public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
        }

        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            viewHolder.itemView.setTranslationX(0);
            viewHolder.itemView.setTranslationY(0);
        }

        public long getAnimationDuration(RecyclerView recyclerView, int animationType, float animateDx,
                                         float animateDy) {
            RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
            if (itemAnimator == null) {
                return animationType == ANIMATION_TYPE_DRAG ? DEFAULT_DRAG_ANIMATION_DURATION
                        : DEFAULT_SWIPE_ANIMATION_DURATION;
            }
            return animationType == ANIMATION_TYPE_DRAG ? itemAnimator.getMoveDuration()
                    : itemAnimator.getRemoveDuration();
        }

        public int interpolateOutOfBoundsScroll(RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds,
                                                int totalSize, long msSinceStartScroll) {
            return 0;
        }
    }

    /// A callback with fixed drag and swipe directions.
    public abstract static class SimpleCallback extends Callback {
        private int mDefaultSwipeDirs;
        private int mDefaultDragDirs;

        public SimpleCallback(int dragDirs, int swipeDirs) {
            mDefaultSwipeDirs = swipeDirs;
            mDefaultDragDirs = dragDirs;
        }

        public void setDefaultSwipeDirs(int defaultSwipeDirs) {
            mDefaultSwipeDirs = defaultSwipeDirs;
        }

        public void setDefaultDragDirs(int defaultDragDirs) {
            mDefaultDragDirs = defaultDragDirs;
        }

        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return mDefaultSwipeDirs;
        }

        public int getDragDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return mDefaultDragDirs;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(getDragDirs(recyclerView, viewHolder), getSwipeDirs(recyclerView, viewHolder));
        }
    }
}
