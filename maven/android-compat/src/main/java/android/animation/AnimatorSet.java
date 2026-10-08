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
package android.animation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// Plays animators in a specified order: together, in sequence, or by the
/// `with`/`before`/`after` relationships of a [Builder]. An animator starts
/// once every animator it depends on has started (`with`) or ended
/// (`after`); the set ends when all of them have.
public final class AnimatorSet extends Animator {

    private final ArrayList<Node> mNodes = new ArrayList<Node>();
    private long mStartDelay;
    private long mDuration = -1;
    private TimeInterpolator mInterpolator;
    private boolean mStarted;
    private boolean mTerminating;
    private int mEnded;
    private ValueAnimator mDelayAnim;

    public AnimatorSet() {
    }

    public void playTogether(Animator... items) {
        if (items != null && items.length > 0) {
            Builder builder = play(items[0]);
            for (int i = 1; i < items.length; ++i) {
                builder.with(items[i]);
            }
        }
    }

    public void playTogether(Collection<Animator> items) {
        if (items != null && !items.isEmpty()) {
            Builder builder = null;
            for (Animator anim : items) {
                if (builder == null) {
                    builder = play(anim);
                } else {
                    builder.with(anim);
                }
            }
        }
    }

    public void playSequentially(Animator... items) {
        if (items != null) {
            if (items.length == 1) {
                play(items[0]);
            } else {
                for (int i = 0; i < items.length - 1; ++i) {
                    play(items[i]).before(items[i + 1]);
                }
            }
        }
    }

    public void playSequentially(List<Animator> items) {
        if (items != null && !items.isEmpty()) {
            playSequentially(items.toArray(new Animator[items.size()]));
        }
    }

    public ArrayList<Animator> getChildAnimations() {
        ArrayList<Animator> childList = new ArrayList<Animator>();
        for (Node node : mNodes) {
            childList.add(node.animation);
        }
        return childList;
    }

    @Override
    public void setTarget(Object target) {
        for (Node node : mNodes) {
            node.animation.setTarget(target);
        }
    }

    @Override
    public void setInterpolator(TimeInterpolator interpolator) {
        mInterpolator = interpolator;
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return mInterpolator;
    }

    public Builder play(Animator anim) {
        if (anim != null) {
            return new Builder(anim);
        }
        return null;
    }

    private Node nodeFor(Animator anim) {
        for (Node n : mNodes) {
            if (n.animation == anim) {
                return n;
            }
        }
        Node n = new Node(anim);
        mNodes.add(n);
        return n;
    }

    @Override
    public long getStartDelay() {
        return mStartDelay;
    }

    @Override
    public void setStartDelay(long startDelay) {
        mStartDelay = Math.max(0, startDelay);
    }

    @Override
    public long getDuration() {
        return mDuration;
    }

    @Override
    public AnimatorSet setDuration(long duration) {
        if (duration < 0) {
            throw new IllegalArgumentException("duration must be a value of zero or greater");
        }
        mDuration = duration;
        return this;
    }

    @Override
    public long getTotalDuration() {
        long total = 0;
        for (Node n : mNodes) {
            long end = endTime(n, 0);
            if (end == DURATION_INFINITE) {
                return DURATION_INFINITE;
            }
            total = Math.max(total, end);
        }
        return mStartDelay + total;
    }

    /// When `n` ends, measured from the set's start (after its delay).
    private long endTime(Node n, int depth) {
        long d = mDuration >= 0 ? mDuration : n.animation.getTotalDuration() - n.animation.getStartDelay();
        if (depth > mNodes.size() || d == DURATION_INFINITE) {
            return DURATION_INFINITE;
        }
        long start = 0;
        for (Dependency dep : n.dependencies) {
            long t = dep.rule == Dependency.AFTER ? endTime(dep.node, depth + 1) : startTime(dep.node, depth + 1);
            if (t == DURATION_INFINITE) {
                return DURATION_INFINITE;
            }
            start = Math.max(start, t);
        }
        return start + n.animation.getStartDelay() + d;
    }

    private long startTime(Node n, int depth) {
        long start = 0;
        for (Dependency dep : n.dependencies) {
            long t = dep.rule == Dependency.AFTER ? endTime(dep.node, depth + 1) : startTime(dep.node, depth + 1);
            if (t == DURATION_INFINITE) {
                return DURATION_INFINITE;
            }
            start = Math.max(start, t);
        }
        return start;
    }

    @Override
    public void setupStartValues() {
        for (Node node : mNodes) {
            node.animation.setupStartValues();
        }
    }

    @Override
    public void setupEndValues() {
        for (Node node : mNodes) {
            node.animation.setupEndValues();
        }
    }

    @Override
    public void pause() {
        boolean wasPaused = mPaused;
        super.pause();
        if (!wasPaused && mPaused) {
            if (mDelayAnim != null) {
                mDelayAnim.pause();
            }
            for (Node node : mNodes) {
                node.animation.pause();
            }
        }
    }

    @Override
    public void resume() {
        boolean wasPaused = mPaused;
        super.resume();
        if (wasPaused && !mPaused) {
            if (mDelayAnim != null) {
                mDelayAnim.resume();
            }
            for (Node node : mNodes) {
                node.animation.resume();
            }
        }
    }

    @Override
    public boolean isRunning() {
        for (Node node : mNodes) {
            if (node.animation.isRunning()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public void start() {
        if (mStarted) {
            return;
        }
        mTerminating = false;
        mStarted = true;
        mPaused = false;
        mEnded = 0;
        for (Node node : mNodes) {
            node.started = false;
            node.ended = false;
            if (mDuration >= 0) {
                node.animation.setDuration(mDuration);
            }
            if (mInterpolator != null) {
                node.animation.setInterpolator(mInterpolator);
            }
            node.animation.addListener(node.listener);
        }
        ArrayList<AnimatorListener> l = listenersCopy();
        if (l != null) {
            for (AnimatorListener a : l) {
                a.onAnimationStart(this);
                if (!mStarted || mTerminating) {
                    return;
                }
            }
        }
        if (!mStarted || mTerminating) {
            return;
        }
        if (mNodes.isEmpty()) {
            finish(false);
            return;
        }
        if (mStartDelay > 0) {
            mDelayAnim = ValueAnimator.ofFloat(0f, 1f);
            mDelayAnim.setDuration(mStartDelay);
            mDelayAnim.addListener(new AnimatorListenerAdapter() {
                private boolean canceled;

                @Override
                public void onAnimationCancel(Animator animation) {
                    canceled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mDelayAnim = null;
                    if (!canceled && mStarted && !mTerminating) {
                        startReadyNodes();
                    }
                }
            });
            mDelayAnim.start();
        } else {
            startReadyNodes();
        }
    }

    /// Starts every node whose dependencies are all satisfied.
    private void startReadyNodes() {
        boolean progress = true;
        while (progress && mStarted && !mTerminating) {
            progress = false;
            for (Node node : new ArrayList<Node>(mNodes)) {
                if (!mStarted || mTerminating) {
                    return;
                }
                if (!node.started && node.ready()) {
                    node.started = true;
                    node.animation.start();
                    progress = true;
                }
            }
        }
    }

    @Override
    public void cancel() {
        if (!mStarted) {
            return;
        }
        mTerminating = true;
        ArrayList<AnimatorListener> l = listenersCopy();
        if (l != null) {
            for (AnimatorListener a : l) {
                a.onAnimationCancel(this);
            }
        }
        if (mDelayAnim != null) {
            mDelayAnim.cancel();
            mDelayAnim = null;
        }
        for (Node node : new ArrayList<Node>(mNodes)) {
            if (node.animation.isStarted()) {
                node.animation.cancel();
            }
        }
        finish(true);
    }

    @Override
    public void end() {
        if (!mStarted) {
            start();
        }
        mTerminating = true;
        if (mDelayAnim != null) {
            mDelayAnim.cancel();
            mDelayAnim = null;
        }
        // Values land on their end states in dependency order, so a later
        // animator's end value wins over an earlier one on the same property.
        for (Node node : sortedNodes()) {
            node.animation.end();
        }
        finish(false);
    }

    private ArrayList<Node> sortedNodes() {
        ArrayList<Node> sorted = new ArrayList<Node>();
        ArrayList<Node> pending = new ArrayList<Node>(mNodes);
        while (!pending.isEmpty()) {
            boolean progress = false;
            for (Node n : new ArrayList<Node>(pending)) {
                boolean depsDone = true;
                for (Dependency d : n.dependencies) {
                    if (!sorted.contains(d.node)) {
                        depsDone = false;
                        break;
                    }
                }
                if (depsDone) {
                    sorted.add(n);
                    pending.remove(n);
                    progress = true;
                }
            }
            if (!progress) {
                sorted.addAll(pending);
                break;
            }
        }
        return sorted;
    }

    private void finish(boolean canceled) {
        if (!mStarted) {
            return;
        }
        mStarted = false;
        mTerminating = false;
        mPaused = false;
        for (Node node : mNodes) {
            node.animation.removeListener(node.listener);
        }
        ArrayList<AnimatorListener> l = listenersCopy();
        if (l != null) {
            for (AnimatorListener a : l) {
                a.onAnimationEnd(this);
            }
        }
    }

    @Override
    public boolean canReverse() {
        return false;
    }

    @Override
    public AnimatorSet clone() {
        return copyNodesFrom(this);
    }

    private AnimatorSet copyNodesFrom(AnimatorSet src) {
        ArrayList<Node> copies = new ArrayList<Node>();
        for (Node n : src.mNodes) {
            copies.add(new Node(n.animation.clone()));
        }
        for (int i = 0; i < src.mNodes.size(); i++) {
            for (Dependency d : src.mNodes.get(i).dependencies) {
                copies.get(i).dependencies.add(new Dependency(copies.get(src.mNodes.indexOf(d.node)), d.rule));
            }
        }
        return new AnimatorSet(this, copies);
    }

    private AnimatorSet(AnimatorSet template, ArrayList<Node> nodes) {
        mNodes.addAll(nodes);
        for (Node n : mNodes) {
            n.owner = this;
        }
        mStartDelay = template.mStartDelay;
        mDuration = template.mDuration;
        mInterpolator = template.mInterpolator;
        mListeners = template.mListeners == null ? null : new ArrayList<AnimatorListener>(template.mListeners);
        mPauseListeners = template.mPauseListeners == null ? null
                : new ArrayList<AnimatorPauseListener>(template.mPauseListeners);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AnimatorSet@").append(Integer.toHexString(hashCode())).append("{");
        for (Node node : mNodes) {
            sb.append("\n    ").append(node.animation.toString());
        }
        return sb.append("\n}").toString();
    }

    private static final class Dependency {
        static final int WITH = 0;
        static final int AFTER = 1;
        final Node node;
        final int rule;

        Dependency(Node node, int rule) {
            this.node = node;
            this.rule = rule;
        }
    }

    private final class Node {
        final Animator animation;
        final ArrayList<Dependency> dependencies = new ArrayList<Dependency>();
        AnimatorSet owner = AnimatorSet.this;
        boolean started;
        boolean ended;
        final AnimatorListener listener = new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                owner.startReadyNodes();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (ended) {
                    return;
                }
                ended = true;
                owner.mEnded++;
                if (!owner.mTerminating) {
                    owner.startReadyNodes();
                    if (owner.mEnded >= owner.mNodes.size()) {
                        owner.finish(false);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        };

        Node(Animator animation) {
            this.animation = animation;
        }

        boolean ready() {
            for (Dependency d : dependencies) {
                if (d.rule == Dependency.WITH ? !d.node.started : !d.node.ended) {
                    return false;
                }
            }
            return true;
        }
    }

    /// Declares how animators relate to the one given to [#play(Animator)].
    public class Builder {
        private final Node mCurrentNode;

        Builder(Animator anim) {
            mCurrentNode = nodeFor(anim);
        }

        public Builder with(Animator anim) {
            Node node = nodeFor(anim);
            // A sibling is started together with the current animator, so
            // whatever the current one later waits for (`after`) delays both.
            node.dependencies.add(new Dependency(mCurrentNode, Dependency.WITH));
            return this;
        }

        public Builder before(Animator anim) {
            Node node = nodeFor(anim);
            node.dependencies.add(new Dependency(mCurrentNode, Dependency.AFTER));
            return this;
        }

        public Builder after(Animator anim) {
            Node node = nodeFor(anim);
            mCurrentNode.dependencies.add(new Dependency(node, Dependency.AFTER));
            return this;
        }

        public Builder after(long delay) {
            ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(delay);
            after(anim);
            return this;
        }
    }
}
