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
import android.view.View;
import android.view.ViewPropertyAnimator;

import java.util.ArrayList;
import java.util.List;

/// The default item animations: removed items fade out, then the items
/// that moved slide to their new places, then added items fade in.
public class DefaultItemAnimator extends SimpleItemAnimator {

    private final ArrayList<RecyclerView.ViewHolder> mPendingRemovals = new ArrayList<RecyclerView.ViewHolder>();
    private final ArrayList<RecyclerView.ViewHolder> mPendingAdditions = new ArrayList<RecyclerView.ViewHolder>();
    private final ArrayList<MoveInfo> mPendingMoves = new ArrayList<MoveInfo>();
    private final ArrayList<ChangeInfo> mPendingChanges = new ArrayList<ChangeInfo>();

    final ArrayList<RecyclerView.ViewHolder> mAddAnimations = new ArrayList<RecyclerView.ViewHolder>();
    final ArrayList<RecyclerView.ViewHolder> mMoveAnimations = new ArrayList<RecyclerView.ViewHolder>();
    final ArrayList<RecyclerView.ViewHolder> mRemoveAnimations = new ArrayList<RecyclerView.ViewHolder>();
    final ArrayList<RecyclerView.ViewHolder> mChangeAnimations = new ArrayList<RecyclerView.ViewHolder>();

    private static final class MoveInfo {
        final RecyclerView.ViewHolder holder;
        final int fromX;
        final int fromY;
        final int toX;
        final int toY;

        MoveInfo(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
            this.holder = holder;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }
    }

    private static final class ChangeInfo {
        RecyclerView.ViewHolder oldHolder;
        RecyclerView.ViewHolder newHolder;
        final int fromX;
        final int fromY;
        final int toX;
        final int toY;

        ChangeInfo(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromX, int fromY,
                   int toX, int toY) {
            this.oldHolder = oldHolder;
            this.newHolder = newHolder;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }
    }

    @Override
    public void runPendingAnimations() {
        boolean removalsPending = !mPendingRemovals.isEmpty();
        boolean movesPending = !mPendingMoves.isEmpty();
        boolean changesPending = !mPendingChanges.isEmpty();
        boolean additionsPending = !mPendingAdditions.isEmpty();
        if (!removalsPending && !movesPending && !additionsPending && !changesPending) {
            return;
        }
        for (RecyclerView.ViewHolder holder : mPendingRemovals) {
            animateRemoveImpl(holder);
        }
        mPendingRemovals.clear();
        long removeDelay = removalsPending ? getRemoveDuration() : 0;
        for (MoveInfo m : new ArrayList<MoveInfo>(mPendingMoves)) {
            animateMoveImpl(m.holder, m.fromX, m.fromY, m.toX, m.toY, removeDelay);
        }
        mPendingMoves.clear();
        for (ChangeInfo c : new ArrayList<ChangeInfo>(mPendingChanges)) {
            animateChangeImpl(c, removeDelay);
        }
        mPendingChanges.clear();
        long moveDuration = movesPending ? getMoveDuration() : 0;
        long changeDuration = changesPending ? getChangeDuration() : 0;
        long addDelay = removeDelay + Math.max(moveDuration, changeDuration);
        for (RecyclerView.ViewHolder holder : new ArrayList<RecyclerView.ViewHolder>(mPendingAdditions)) {
            animateAddImpl(holder, addDelay);
        }
        mPendingAdditions.clear();
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        resetAnimation(holder);
        mPendingRemovals.add(holder);
        return true;
    }

    private void animateRemoveImpl(final RecyclerView.ViewHolder holder) {
        final View view = holder.itemView;
        final ViewPropertyAnimator animation = view.animate();
        mRemoveAnimations.add(holder);
        animation.setDuration(getRemoveDuration()).alpha(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchRemoveStarting(holder);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animation.setListener(null);
                view.setAlpha(1);
                if (mRemoveAnimations.remove(holder)) {
                    dispatchRemoveFinished(holder);
                }
                dispatchFinishedWhenDone();
            }
        }).start();
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        resetAnimation(holder);
        holder.itemView.setAlpha(0);
        mPendingAdditions.add(holder);
        return true;
    }

    void animateAddImpl(final RecyclerView.ViewHolder holder, long delay) {
        final View view = holder.itemView;
        final ViewPropertyAnimator animation = view.animate();
        mAddAnimations.add(holder);
        animation.alpha(1).setStartDelay(delay).setDuration(getAddDuration()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchAddStarting(holder);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                view.setAlpha(1);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animation.setListener(null);
                view.setAlpha(1);
                if (mAddAnimations.remove(holder)) {
                    dispatchAddFinished(holder);
                }
                dispatchFinishedWhenDone();
            }
        }).start();
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        View view = holder.itemView;
        fromX += (int) holder.itemView.getTranslationX();
        fromY += (int) holder.itemView.getTranslationY();
        resetAnimation(holder);
        int deltaX = toX - fromX;
        int deltaY = toY - fromY;
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder);
            return false;
        }
        if (deltaX != 0) {
            view.setTranslationX(-deltaX);
        }
        if (deltaY != 0) {
            view.setTranslationY(-deltaY);
        }
        mPendingMoves.add(new MoveInfo(holder, fromX, fromY, toX, toY));
        return true;
    }

    void animateMoveImpl(final RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY, long delay) {
        final View view = holder.itemView;
        final int deltaX = toX - fromX;
        final int deltaY = toY - fromY;
        if (deltaX != 0) {
            view.animate().translationX(0);
        }
        if (deltaY != 0) {
            view.animate().translationY(0);
        }
        final ViewPropertyAnimator animation = view.animate();
        mMoveAnimations.add(holder);
        animation.setStartDelay(delay).setDuration(getMoveDuration()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchMoveStarting(holder);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                if (deltaX != 0) {
                    view.setTranslationX(0);
                }
                if (deltaY != 0) {
                    view.setTranslationY(0);
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animation.setListener(null);
                view.setTranslationX(0);
                view.setTranslationY(0);
                if (mMoveAnimations.remove(holder)) {
                    dispatchMoveFinished(holder);
                }
                dispatchFinishedWhenDone();
            }
        }).start();
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromX,
                                 int fromY, int toX, int toY) {
        if (oldHolder == newHolder) {
            return animateMove(oldHolder, fromX, fromY, toX, toY);
        }
        float prevTranslationX = oldHolder.itemView.getTranslationX();
        float prevTranslationY = oldHolder.itemView.getTranslationY();
        float prevAlpha = oldHolder.itemView.getAlpha();
        resetAnimation(oldHolder);
        int deltaX = (int) (toX - fromX - prevTranslationX);
        int deltaY = (int) (toY - fromY - prevTranslationY);
        oldHolder.itemView.setTranslationX(prevTranslationX);
        oldHolder.itemView.setTranslationY(prevTranslationY);
        oldHolder.itemView.setAlpha(prevAlpha);
        if (newHolder != null) {
            resetAnimation(newHolder);
            newHolder.itemView.setTranslationX(-deltaX);
            newHolder.itemView.setTranslationY(-deltaY);
            newHolder.itemView.setAlpha(0);
        }
        mPendingChanges.add(new ChangeInfo(oldHolder, newHolder, fromX, fromY, toX, toY));
        return true;
    }

    void animateChangeImpl(final ChangeInfo changeInfo, long delay) {
        final RecyclerView.ViewHolder holder = changeInfo.oldHolder;
        final View view = holder == null ? null : holder.itemView;
        final RecyclerView.ViewHolder newHolder = changeInfo.newHolder;
        final View newView = newHolder != null ? newHolder.itemView : null;
        if (view != null) {
            final ViewPropertyAnimator oldViewAnim = view.animate().setDuration(getChangeDuration()).setStartDelay(delay);
            mChangeAnimations.add(changeInfo.oldHolder);
            oldViewAnim.translationX(changeInfo.toX - changeInfo.fromX);
            oldViewAnim.translationY(changeInfo.toY - changeInfo.fromY);
            oldViewAnim.alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    dispatchChangeStarting(changeInfo.oldHolder, true);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    oldViewAnim.setListener(null);
                    view.setAlpha(1);
                    view.setTranslationX(0);
                    view.setTranslationY(0);
                    if (mChangeAnimations.remove(changeInfo.oldHolder)) {
                        dispatchChangeFinished(changeInfo.oldHolder, true);
                    }
                    dispatchFinishedWhenDone();
                }
            }).start();
        }
        if (newView != null) {
            final ViewPropertyAnimator newViewAnimation = newView.animate();
            mChangeAnimations.add(changeInfo.newHolder);
            newViewAnimation.translationX(0).translationY(0).setStartDelay(delay).setDuration(getChangeDuration())
                    .alpha(1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                            dispatchChangeStarting(changeInfo.newHolder, false);
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            newViewAnimation.setListener(null);
                            newView.setAlpha(1);
                            newView.setTranslationX(0);
                            newView.setTranslationY(0);
                            if (mChangeAnimations.remove(changeInfo.newHolder)) {
                                dispatchChangeFinished(changeInfo.newHolder, false);
                            }
                            dispatchFinishedWhenDone();
                        }
                    }).start();
        }
    }

    private void resetAnimation(RecyclerView.ViewHolder holder) {
        holder.itemView.animate().cancel();
        endAnimation(holder);
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        View view = item.itemView;
        view.animate().cancel();
        for (int i = mPendingMoves.size() - 1; i >= 0; i--) {
            if (mPendingMoves.get(i).holder == item) {
                view.setTranslationY(0);
                view.setTranslationX(0);
                dispatchMoveFinished(item);
                mPendingMoves.remove(i);
            }
        }
        for (int i = mPendingChanges.size() - 1; i >= 0; i--) {
            ChangeInfo c = mPendingChanges.get(i);
            if (c.oldHolder == item || c.newHolder == item) {
                view.setAlpha(1);
                view.setTranslationX(0);
                view.setTranslationY(0);
                dispatchChangeFinished(item, c.oldHolder == item);
                mPendingChanges.remove(i);
            }
        }
        if (mPendingRemovals.remove(item)) {
            view.setAlpha(1);
            dispatchRemoveFinished(item);
        }
        if (mPendingAdditions.remove(item)) {
            view.setAlpha(1);
            dispatchAddFinished(item);
        }
        if (mRemoveAnimations.remove(item)) {
            view.setAlpha(1);
            dispatchRemoveFinished(item);
        }
        if (mAddAnimations.remove(item)) {
            view.setAlpha(1);
            dispatchAddFinished(item);
        }
        if (mMoveAnimations.remove(item)) {
            view.setTranslationX(0);
            view.setTranslationY(0);
            dispatchMoveFinished(item);
        }
        if (mChangeAnimations.remove(item)) {
            view.setAlpha(1);
            view.setTranslationX(0);
            view.setTranslationY(0);
            dispatchChangeFinished(item, true);
        }
        dispatchFinishedWhenDone();
    }

    @Override
    public boolean isRunning() {
        return !mPendingAdditions.isEmpty() || !mPendingChanges.isEmpty() || !mPendingMoves.isEmpty()
                || !mPendingRemovals.isEmpty() || !mMoveAnimations.isEmpty() || !mRemoveAnimations.isEmpty()
                || !mAddAnimations.isEmpty() || !mChangeAnimations.isEmpty();
    }

    void dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }

    @Override
    public void endAnimations() {
        List<RecyclerView.ViewHolder> all = new ArrayList<RecyclerView.ViewHolder>();
        all.addAll(mPendingRemovals);
        all.addAll(mPendingAdditions);
        for (MoveInfo m : mPendingMoves) {
            all.add(m.holder);
        }
        for (ChangeInfo c : mPendingChanges) {
            if (c.oldHolder != null) {
                all.add(c.oldHolder);
            }
            if (c.newHolder != null) {
                all.add(c.newHolder);
            }
        }
        all.addAll(mRemoveAnimations);
        all.addAll(mAddAnimations);
        all.addAll(mMoveAnimations);
        all.addAll(mChangeAnimations);
        for (RecyclerView.ViewHolder h : all) {
            endAnimation(h);
        }
        dispatchAnimationsFinished();
    }

    @Override
    public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder, List<Object> payloads) {
        return !payloads.isEmpty() || super.canReuseUpdatedViewHolder(viewHolder, payloads);
    }
}
