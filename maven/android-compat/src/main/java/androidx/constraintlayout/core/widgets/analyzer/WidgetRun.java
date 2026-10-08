/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.constraintlayout.core.widgets.analyzer;

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_PERCENT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_RATIO;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_SPREAD;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_WRAP;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;

public abstract class WidgetRun implements Dependency {
    public int matchConstraintsType;
    ConstraintWidget widget;
    RunGroup runGroup;
    protected ConstraintWidget.DimensionBehaviour dimensionBehavior;
    DimensionDependency dimension = new DimensionDependency(this);

    public int orientation = HORIZONTAL;
    boolean resolved = false;
    public DependencyNode start = new DependencyNode(this);
    public DependencyNode end = new DependencyNode(this);

    protected RunType mRunType = RunType.NONE;

    public WidgetRun(ConstraintWidget widget) {
        this.widget = widget;
    }

    abstract void clear();
    abstract void apply();
    abstract void applyToWidget();
    abstract void reset();

    abstract boolean supportsWrapComputation();

    public boolean isDimensionResolved() {
        return dimension.resolved;
    }

    public boolean isCenterConnection() {
        int connections = 0;
        int count = start.targets.size();
        for (int i = 0; i < count; i++) {
            DependencyNode dependency = start.targets.get(i);
            if (dependency.run != this) {
                connections++;
            }
        }
        count = end.targets.size();
        for (int i = 0; i < count; i++) {
            DependencyNode dependency = end.targets.get(i);
            if (dependency.run != this) {
                connections++;
            }
        }
        return connections >= 2;
    }

    public long wrapSize(int direction) {
        if (dimension.resolved) {
            long size = dimension.value;
            if (isCenterConnection()) { //start.targets.size() > 0 && end.targets.size() > 0) {
                size += start.margin - end.margin;
            } else {
                if (direction == RunGroup.START) {
                    size += start.margin;
                } else {
                    size -= end.margin;
                }
            }
            return size;
        }
        return 0;
    }

    protected final DependencyNode getTarget(ConstraintAnchor anchor) {
        if (anchor.mTarget == null) {
            return null;
        }
        DependencyNode target = null;
        ConstraintWidget targetWidget = anchor.mTarget.mOwner;
        ConstraintAnchor.Type targetType = anchor.mTarget.mType;
        switch (targetType) {
            case LEFT: {
                HorizontalWidgetRun run = targetWidget.horizontalRun;
                target = run.start;
            } break;
            case RIGHT: {
                HorizontalWidgetRun run = targetWidget.horizontalRun;
                target = run.end;
            } break;
            case TOP: {
                VerticalWidgetRun run = targetWidget.verticalRun;
                target = run.start;
            } break;
            case BASELINE: {
                VerticalWidgetRun run = targetWidget.verticalRun;
                target = run.baseline;
            } break;
            case BOTTOM: {
                VerticalWidgetRun run = targetWidget.verticalRun;
                target = run.end;
            } break;
            default: break;
        }
        return target;
    }

    protected void updateRunCenter(Dependency dependency, ConstraintAnchor startAnchor, ConstraintAnchor endAnchor, int orientation) {
        DependencyNode startTarget = getTarget(startAnchor);
        DependencyNode endTarget = getTarget(endAnchor);

        if (!(startTarget.resolved && endTarget.resolved)) {
            return;
        }

        int startPos = startTarget.value + startAnchor.getMargin();
        int endPos = endTarget.value - endAnchor.getMargin();
        int distance = endPos - startPos;

        if (!dimension.resolved
                && dimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            resolveDimension(orientation, distance);
        }

        if (!dimension.resolved) {
            return;
        }

        if (dimension.value == distance) {
            start.resolve(startPos);
            end.resolve(endPos);
            return;
        }

        // Otherwise, we have to center
        float bias = orientation == HORIZONTAL ? widget.getHorizontalBiasPercent()
                : widget.getVerticalBiasPercent();

        if (startTarget == endTarget) {
            startPos = startTarget.value;
            endPos = endTarget.value;
            // TODO: taking advantage of bias here would be a nice feature to support,
            // but for now let's stay compatible with 1.1
            bias = 0.5f;
        }

        int availableDistance = (endPos - startPos - dimension.value);
        start.resolve((int) (0.5f + startPos + availableDistance * bias));
        end.resolve(start.value + dimension.value);
    }

    private void resolveDimension(int orientation, int distance) {
        switch (matchConstraintsType) {
            case MATCH_CONSTRAINT_SPREAD: {
                dimension.resolve(getLimitedDimension(distance, orientation));
            }
            break;
            case MATCH_CONSTRAINT_PERCENT: {
                ConstraintWidget parent = widget.getParent();
                if (parent != null) {
                    WidgetRun run = orientation == HORIZONTAL ?
                            parent.horizontalRun
                            : parent.verticalRun;
                    if (run.dimension.resolved) {
                        float percent = orientation == HORIZONTAL ?
                                widget.mMatchConstraintPercentWidth
                                : widget.mMatchConstraintPercentHeight;
                        int targetDimensionValue = run.dimension.value;
                        int size = (int) (0.5f + targetDimensionValue * percent);
                        dimension.resolve(getLimitedDimension(size, orientation));
                    }
                }
            }
            break;
            case MATCH_CONSTRAINT_WRAP: {
                int wrapValue = getLimitedDimension(dimension.wrapValue, orientation);
                dimension.resolve(Math.min(wrapValue, distance));
            }
            break;
            case MATCH_CONSTRAINT_RATIO: {
                if (widget.horizontalRun.dimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                        && widget.horizontalRun.matchConstraintsType == MATCH_CONSTRAINT_RATIO
                        && widget.verticalRun.dimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                        && widget.verticalRun.matchConstraintsType == MATCH_CONSTRAINT_RATIO) {
                    // pof
                } else {
                    WidgetRun run = (orientation == HORIZONTAL) ? widget.verticalRun : widget.horizontalRun;
                    if (run.dimension.resolved) {
                        float ratio = widget.getDimensionRatio();
                        int value;
                        if (orientation == VERTICAL) {
                            value = (int) (0.5f + run.dimension.value / ratio);
                        } else {
                            value = (int) (0.5f + ratio * run.dimension.value);
                        }
                        dimension.resolve(value);
                    }
                }
            }
            break;
            default: break;
        }
    }

    protected void updateRunStart(Dependency dependency) {

    }

    protected void updateRunEnd(Dependency dependency) {

    }

    public void update(Dependency dependency) {}

    final protected int getLimitedDimension(int dimension, int orientation) {
        if (orientation == HORIZONTAL) {
            int max = widget.mMatchConstraintMaxWidth;
            int min = widget.mMatchConstraintMinWidth;
            int value = Math.max(min, dimension);
            if (max > 0) {
                value = Math.min(max, dimension);
            }
            if (value != dimension) {
                dimension = value;
            }
        } else {
            int max = widget.mMatchConstraintMaxHeight;
            int min = widget.mMatchConstraintMinHeight;
            int value = Math.max(min, dimension);
            if (max > 0) {
                value = Math.min(max, dimension);
            }
            if (value != dimension) {
                dimension = value;
            }
        }
        return dimension;
    }

    final protected DependencyNode getTarget(ConstraintAnchor anchor, int orientation) {
        if (anchor.mTarget == null) {
            return null;
        }
        DependencyNode target = null;
        ConstraintWidget targetWidget = anchor.mTarget.mOwner;
        WidgetRun run = (orientation == ConstraintWidget.HORIZONTAL) ?
                targetWidget.horizontalRun : targetWidget.verticalRun;
        ConstraintAnchor.Type targetType = anchor.mTarget.mType;
        switch (targetType) {
            case TOP:
            case LEFT: {
                target = run.start;
            } break;
            case BOTTOM:
            case RIGHT: {
                target = run.end;
            } break;
            default: break;
        }
        return target;
    }

    final protected void addTarget(DependencyNode node, DependencyNode target, int margin) {
        node.targets.add(target);
        node.margin = margin;
        target.dependencies.add(node);
    }

    final protected void addTarget(DependencyNode node, DependencyNode target, int marginFactor, DimensionDependency dimensionDependency) {
        node.targets.add(target);
        node.targets.add(dimension);
        node.marginFactor = marginFactor;
        node.marginDependency = dimensionDependency;
        target.dependencies.add(node);
        dimensionDependency.dependencies.add(node);
    }

    public long getWrapDimension() {
        if (dimension.resolved) {
            return dimension.value;
        }
        return 0;
    }

    public boolean isResolved() { return resolved; }

    enum RunType { NONE, START, END, CENTER }
}
