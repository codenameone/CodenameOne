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
package javafx.animation;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;

/// Plays its children one after the other, in the order of the list.
///
/// The cycle lasts as long as all children together, each counted with
/// its own delay, cycles and rate. A child starts from what the
/// children before it left behind. The finished handler of a child
/// runs when the playhead leaves it.
///
/// The children are read when the transition starts.
public final class SequentialTransition extends Transition {

    private final ObjectProperty<Node> node = new SimpleObjectProperty<Node>(this, "node");
    private final ObservableList<Animation> children = FXCollections.observableArrayList();
    private final ChildClips clips = new ChildClips(this, true);

    /// Creates a transition of children for a node.
    public SequentialTransition(Node node, Animation... children) {
        setInterpolator(Interpolator.LINEAR);
        this.children.addListener(new ListChangeListener<Animation>() {
            @Override
            public void onChanged(Change<? extends Animation> change) {
                childrenChanged(change);
            }
        });
        this.node.set(node);
        if (children != null) {
            this.children.setAll(children);
        }
    }

    /// Creates a transition of children.
    public SequentialTransition(Animation... children) {
        this(null, children);
    }

    /// Creates an empty transition for a node.
    public SequentialTransition(Node node) {
        this(node, (Animation[]) null);
    }

    /// Creates an empty transition.
    public SequentialTransition() {
        this((Node) null, (Animation[]) null);
    }

    /// Sets the node the children animate when they name none
    /// themselves.
    public final void setNode(Node value) {
        node.set(value);
    }

    /// Returns the node the children animate when they name none
    /// themselves.
    public final Node getNode() {
        return node.get();
    }

    /// The node the children animate when they name none themselves.
    public final ObjectProperty<Node> nodeProperty() {
        return node;
    }

    /// Returns the children, in the order they are played. An animation has one
    /// parent at most: adding one that is a child elsewhere throws
    /// `IllegalArgumentException`.
    public final ObservableList<Animation> getChildren() {
        return children;
    }

    private void childrenChanged(ListChangeListener.Change<? extends Animation> change) {
        while (change.next()) {
            for (Animation removed : change.getRemoved()) {
                if (removed != null && removed.parent == this) {
                    removed.parent = null;
                }
            }
        }
        for (int i = 0; i < children.size(); i++) {
            Animation child = children.get(i);
            if (child == null) {
                continue;
            }
            if (child.parent != null && child.parent != this) {
                throw new IllegalArgumentException("Attempting to add a child that is already in another animation");
            }
            if (child.parent == null) {
                if (child.getStatus() != Status.STOPPED) {
                    child.stop();
                }
                child.parent = this;
            }
        }
        if (root().getStatus() == Status.STOPPED) {
            syncDuration();
        }
    }

    @Override
    protected Node getParentTargetNode() {
        Node own = getNode();
        return own != null ? own : super.getParentTargetNode();
    }

    /// Does nothing: the children do the animating.
    @Override
    protected void interpolate(double frac) {
    }

    @Override
    void syncDuration() {
        double length = clips.layout(children);
        setCycleDuration(Double.isInfinite(length) ? javafx.util.Duration.INDEFINITE
                : javafx.util.Duration.millis(length));
    }

    @Override
    void doStart(boolean capture) {
        // The layout was made by the snapshot that precedes every start.
        clips.start(localMillis(), cycleMillis(), capture);
    }

    @Override
    void doPlayTo(double from, double to, boolean forward, boolean atStart) {
        if (atStart) {
            clips.rewind(forward);
        }
        clips.playTo(to, forward);
    }

    @Override
    void doJumpTo(double time) {
        clips.jumpTo(time);
    }
}
