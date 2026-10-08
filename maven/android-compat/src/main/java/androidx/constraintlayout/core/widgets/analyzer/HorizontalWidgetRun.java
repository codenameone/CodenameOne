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
import androidx.constraintlayout.core.widgets.Helper;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.FIXED;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_PARENT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_PERCENT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_RATIO;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_SPREAD;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_WRAP;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.UNKNOWN;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;
import static androidx.constraintlayout.core.widgets.analyzer.WidgetRun.RunType.CENTER;

public class HorizontalWidgetRun extends WidgetRun {

    private static int[] tempDimensions = new int[2];

    public HorizontalWidgetRun(ConstraintWidget widget) {
        super(widget);
        start.type = DependencyNode.Type.LEFT;
        end.type = DependencyNode.Type.RIGHT;
        this.orientation = HORIZONTAL;
    }

    @Override
    public String toString() {
        return "HorizontalRun " + widget.getDebugName();
    }

    @Override
    void clear() {
        runGroup = null;
        start.clear();
        end.clear();
        dimension.clear();
        resolved = false;
    }

    @Override
    void reset() {
        resolved = false;
        start.clear();
        start.resolved = false;
        end.clear();
        end.resolved = false;
        dimension.resolved = false;
    }

    @Override
    boolean supportsWrapComputation() {
        if (super.dimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            if (super.widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD) {
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    void apply() {
        if (widget.measured) {
            dimension.resolve(widget.getWidth());
        }
        if (!dimension.resolved) {
            super.dimensionBehavior = widget.getHorizontalDimensionBehaviour();
            if (super.dimensionBehavior != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                if (dimensionBehavior == MATCH_PARENT) {
                    ConstraintWidget parent = widget.getParent();
                    if (parent != null
                            && (parent.getHorizontalDimensionBehaviour() == FIXED
                            || parent.getHorizontalDimensionBehaviour() == MATCH_PARENT)) {
                        int resolvedDimension = parent.getWidth() - widget.mLeft.getMargin() - widget.mRight.getMargin();
                        addTarget(start, parent.horizontalRun.start, widget.mLeft.getMargin());
                        addTarget(end, parent.horizontalRun.end, -widget.mRight.getMargin());
                        dimension.resolve(resolvedDimension);
                        return;
                    }
                }
                if (dimensionBehavior == FIXED) {
                    dimension.resolve(widget.getWidth());
                }
            }
        } else {
            if (dimensionBehavior == MATCH_PARENT) {
                ConstraintWidget parent = widget.getParent();
                if (parent != null
                        && (parent.getHorizontalDimensionBehaviour() == FIXED
                        || parent.getHorizontalDimensionBehaviour() == MATCH_PARENT)) {
                    addTarget(start, parent.horizontalRun.start, widget.mLeft.getMargin());
                    addTarget(end, parent.horizontalRun.end, -widget.mRight.getMargin());
                    return;
                }
            }
        }

        // three basic possibilities:
        // <-s-e->
        // <-s-e
        //   s-e->
        // and a variation if the dimension is not yet known:
        // <-s-d-e->
        // <-s<-d<-e
        //   s->d->e->

        if (dimension.resolved && widget.measured) {
            if (widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].mTarget != null && widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].mTarget != null) { // <-s-e->
                if (widget.isInHorizontalChain()) {
                    start.margin = widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].getMargin();
                    end.margin = -widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].getMargin();
                } else {
                    DependencyNode startTarget = getTarget(widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT]);
                    if (startTarget != null) {
                        addTarget(start, startTarget, widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].getMargin());
                    }
                    DependencyNode endTarget = getTarget(widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT]);
                    if (endTarget != null) {
                        addTarget(end, endTarget, -widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].getMargin());
                    }
                    start.delegateToWidgetRun = true;
                    end.delegateToWidgetRun = true;
                }
            } else if (widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].mTarget != null) { // <-s-e
                DependencyNode target = getTarget(widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT]);
                if (target != null) {
                    addTarget(start, target, widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].getMargin());
                    addTarget(end, start, dimension.value);
                }
            } else if (widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].mTarget != null) {   //   s-e->
                DependencyNode target = getTarget(widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT]);
                if (target != null) {
                    addTarget(end, target, -widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].getMargin());
                    addTarget(start, end, -dimension.value);
                }
            } else {
                // no connections, nothing to do.
                if (!(widget instanceof Helper) && widget.getParent() != null
                        && widget.getAnchor(ConstraintAnchor.Type.CENTER).mTarget == null) {
                    DependencyNode left = widget.getParent().horizontalRun.start;
                    addTarget(start, left, widget.getX());
                    addTarget(end, start, dimension.value);
                }
            }
        } else {
            if (dimensionBehavior == MATCH_CONSTRAINT) {
                switch (widget.mMatchConstraintDefaultWidth) {
                    case MATCH_CONSTRAINT_RATIO: {
                        if (widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_RATIO
                        ) {
                            // need to look into both side
                            start.updateDelegate = this;
                            end.updateDelegate = this;
                            widget.verticalRun.start.updateDelegate = this;
                            widget.verticalRun.end.updateDelegate = this;
                            dimension.updateDelegate = this;

                            if (widget.isInVerticalChain()) {
                                dimension.targets.add(widget.verticalRun.dimension);
                                widget.verticalRun.dimension.dependencies.add(dimension);
                                widget.verticalRun.dimension.updateDelegate = this;
                                dimension.targets.add(widget.verticalRun.start);
                                dimension.targets.add(widget.verticalRun.end);
                                widget.verticalRun.start.dependencies.add(dimension);
                                widget.verticalRun.end.dependencies.add(dimension);
                            } else if (widget.isInHorizontalChain()) {
                                widget.verticalRun.dimension.targets.add(dimension);
                                dimension.dependencies.add(widget.verticalRun.dimension);
                            } else {
                                widget.verticalRun.dimension.targets.add(dimension);
                            }
                            break;
                        }
                        // we have a ratio, but we depend on the other side computation
                        DependencyNode targetDimension = widget.verticalRun.dimension;
                        dimension.targets.add(targetDimension);
                        targetDimension.dependencies.add(dimension);
                        widget.verticalRun.start.dependencies.add(dimension);
                        widget.verticalRun.end.dependencies.add(dimension);
                        dimension.delegateToWidgetRun = true;
                        dimension.dependencies.add(start);
                        dimension.dependencies.add(end);
                        start.targets.add(dimension);
                        end.targets.add(dimension);
                    }
                    break;
                    case MATCH_CONSTRAINT_PERCENT: {
                        // we need to look up the parent dimension
                        ConstraintWidget parent = widget.getParent();
                        if (parent == null) {
                            break;
                        }
                        DependencyNode targetDimension = parent.verticalRun.dimension;
                        dimension.targets.add(targetDimension);
                        targetDimension.dependencies.add(dimension);
                        dimension.delegateToWidgetRun = true;
                        dimension.dependencies.add(start);
                        dimension.dependencies.add(end);
                    }
                    break;
                    case MATCH_CONSTRAINT_SPREAD: {
                        // the work is done in the update()
                    }
                    break;
                    default: break;
                }
            }
            if (widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].mTarget != null && widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].mTarget != null) { // <-s-d-e->

                if (widget.isInHorizontalChain()) {
                    start.margin = widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].getMargin();
                    end.margin = -widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].getMargin();
                } else {
                    DependencyNode startTarget = getTarget(widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT]);
                    DependencyNode endTarget = getTarget(widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT]);
                    if (false) {
                        if (startTarget != null) {
                            addTarget(start, startTarget, widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].getMargin());
                        }
                        if (endTarget != null) {
                            addTarget(end, endTarget, -widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].getMargin());
                        }
                    } else {
                        if (startTarget != null) {
                            startTarget.addDependency(this);
                        }
                        if (endTarget != null) {
                            endTarget.addDependency(this);
                        }
                    }
                    mRunType = CENTER;
                }
            } else if (widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].mTarget != null) { // <-s<-d<-e
                DependencyNode target = getTarget(widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT]);
                if (target != null) {
                    addTarget(start, target, widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].getMargin());
                    addTarget(end, start, 1, dimension);
                }
            } else if (widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].mTarget != null) {   //   s->d->e->
                DependencyNode target = getTarget(widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT]);
                if (target != null) {
                    addTarget(end, target, -widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].getMargin());
                    addTarget(start, end, -1, dimension);
                }
            } else {
                // no connections, nothing to do.
                if (!(widget instanceof Helper) && widget.getParent() != null) {
                    DependencyNode left = widget.getParent().horizontalRun.start;
                    addTarget(start, left, widget.getX());
                    addTarget(end, start, 1, dimension);
                }
            }
        }
    }

    private void computeInsetRatio(int[] dimensions, int x1, int x2, int y1, int y2, float ratio, int side) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        switch (side) {
            case UNKNOWN: {
                int candidateX1 = (int) (0.5f + dy * ratio);
                int candidateY1 = dy;
                int candidateX2 = dx;
                int candidateY2 = (int) (0.5f + dx / ratio);
                if (candidateX1 <= dx && candidateY1 <= dy) {
                    dimensions[HORIZONTAL] = candidateX1;
                    dimensions[VERTICAL] = candidateY1;
                } else if (candidateX2 <= dx && candidateY2 <= dy) {
                    dimensions[HORIZONTAL] = candidateX2;
                    dimensions[VERTICAL] = candidateY2;
                }
            }
            break;
            case HORIZONTAL: {
                int horizontalSide = (int) (0.5f + dy * ratio);
                dimensions[HORIZONTAL] = horizontalSide;
                dimensions[VERTICAL] = dy;
            }
            break;
            case VERTICAL: {
                int verticalSide = (int) (0.5f + dx * ratio);
                dimensions[HORIZONTAL] = dx;
                dimensions[VERTICAL] = verticalSide;
            }
            break;
            default: break;
        }
    }

    @Override
    public void update(Dependency dependency) {
        switch (mRunType) {
            case START: {
                updateRunStart(dependency);
            }
            break;
            case END: {
                updateRunEnd(dependency);
            }
            break;
            case CENTER: {
                updateRunCenter(dependency, widget.mLeft, widget.mRight, HORIZONTAL);
                return;
            }
            default: break;
        }

        if (!dimension.resolved) {
            if (dimensionBehavior == MATCH_CONSTRAINT) {
                switch (widget.mMatchConstraintDefaultWidth) {
                    case MATCH_CONSTRAINT_RATIO: {
                        if (widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD
                                || widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_RATIO) {
                            DependencyNode secondStart = widget.verticalRun.start;
                            DependencyNode secondEnd = widget.verticalRun.end;
                            boolean s1 = widget.mLeft.mTarget != null;
                            boolean s2 = widget.mTop.mTarget != null;
                            boolean e1 = widget.mRight.mTarget != null;
                            boolean e2 = widget.mBottom.mTarget != null;

                            int definedSide = widget.getDimensionRatioSide();

                            if (s1 && s2 && e1 && e2) {
                                float ratio = widget.getDimensionRatio();
                                if (secondStart.resolved && secondEnd.resolved) {
                                    if (!(start.readyToSolve && end.readyToSolve)) {
                                        return;
                                    }
                                    int x1 = start.targets.get(0).value + start.margin;
                                    int x2 = end.targets.get(0).value - end.margin;
                                    int y1 = secondStart.value + secondStart.margin;
                                    int y2 = secondEnd.value - secondEnd.margin;
                                    computeInsetRatio(tempDimensions, x1, x2, y1, y2, ratio, definedSide);
                                    dimension.resolve(tempDimensions[HORIZONTAL]);
                                    widget.verticalRun.dimension.resolve(tempDimensions[VERTICAL]);
                                    return;
                                }
                                if (start.resolved && end.resolved) {
                                    if (!(secondStart.readyToSolve && secondEnd.readyToSolve)) {
                                        return;
                                    }
                                    int x1 = start.value + start.margin;
                                    int x2 = end.value - end.margin;
                                    int y1 = secondStart.targets.get(0).value + secondStart.margin;
                                    int y2 = secondEnd.targets.get(0).value - secondEnd.margin;
                                    computeInsetRatio(tempDimensions, x1, x2, y1, y2, ratio, definedSide);
                                    dimension.resolve(tempDimensions[HORIZONTAL]);
                                    widget.verticalRun.dimension.resolve(tempDimensions[VERTICAL]);
                                }
                                if (!(start.readyToSolve && end.readyToSolve
                                        && secondStart.readyToSolve
                                        && secondEnd.readyToSolve)) {
                                    return;
                                }
                                int x1 = start.targets.get(0).value + start.margin;
                                int x2 = end.targets.get(0).value - end.margin;
                                int y1 = secondStart.targets.get(0).value + secondStart.margin;
                                int y2 = secondEnd.targets.get(0).value - secondEnd.margin;
                                computeInsetRatio(tempDimensions, x1, x2, y1, y2, ratio, definedSide);
                                dimension.resolve(tempDimensions[HORIZONTAL]);
                                widget.verticalRun.dimension.resolve(tempDimensions[VERTICAL]);
                            } else if (s1 && e1) {
                                if (!(start.readyToSolve && end.readyToSolve)) {
                                    return;
                                }
                                float ratio = widget.getDimensionRatio();
                                int x1 = start.targets.get(0).value + start.margin;
                                int x2 = end.targets.get(0).value - end.margin;

                                switch (definedSide) {
                                    case UNKNOWN:
                                    case HORIZONTAL: {
                                        int dx = x2 - x1;
                                        int ldx = getLimitedDimension(dx, HORIZONTAL);
                                        int dy = (int) (0.5f + ldx * ratio);
                                        int ldy = getLimitedDimension(dy, VERTICAL);
                                        if (dy != ldy) {
                                            ldx = (int) (0.5f + ldy / ratio);
                                        }
                                        dimension.resolve(ldx);
                                        widget.verticalRun.dimension.resolve(ldy);
                                    }
                                    break;
                                    case VERTICAL: {
                                        int dx = x2 - x1;
                                        int ldx = getLimitedDimension(dx, HORIZONTAL);
                                        int dy = (int) (0.5f + ldx / ratio);
                                        int ldy = getLimitedDimension(dy, VERTICAL);
                                        if (dy != ldy) {
                                            ldx = (int) (0.5f + ldy * ratio);
                                        }
                                        dimension.resolve(ldx);
                                        widget.verticalRun.dimension.resolve(ldy);
                                    }
                                    break;
                                    default: break;
                                }
                            } else if (s2 && e2) {
                                if (!(secondStart.readyToSolve && secondEnd.readyToSolve)) {
                                    return;
                                }
                                float ratio = widget.getDimensionRatio();
                                int y1 = secondStart.targets.get(0).value + secondStart.margin;
                                int y2 = secondEnd.targets.get(0).value - secondEnd.margin;

                                switch (definedSide) {
                                    case UNKNOWN:
                                    case VERTICAL: {
                                        int dy = y2 - y1;
                                        int ldy = getLimitedDimension(dy, VERTICAL);
                                        int dx = (int) (0.5f + ldy / ratio);
                                        int ldx = getLimitedDimension(dx, HORIZONTAL);
                                        if (dx != ldx) {
                                            ldy = (int) (0.5f + ldx * ratio);
                                        }
                                        dimension.resolve(ldx);
                                        widget.verticalRun.dimension.resolve(ldy);
                                    }
                                    break;
                                    case HORIZONTAL: {
                                        int dy = y2 - y1;
                                        int ldy = getLimitedDimension(dy, VERTICAL);
                                        int dx = (int) (0.5f + ldy * ratio);
                                        int ldx = getLimitedDimension(dx, HORIZONTAL);
                                        if (dx != ldx) {
                                            ldy = (int) (0.5f + ldx / ratio);
                                        }
                                        dimension.resolve(ldx);
                                        widget.verticalRun.dimension.resolve(ldy);
                                    }
                                    break;
                                    default: break;
                                }
                            }
                        } else {
                            int size = 0;
                            int ratioSide = widget.getDimensionRatioSide();
                            switch (ratioSide) {
                                case HORIZONTAL: {
                                    size = (int) (0.5f + widget.verticalRun.dimension.value / widget.getDimensionRatio());
                                }
                                break;
                                case ConstraintWidget.VERTICAL: {
                                    size = (int) (0.5f + widget.verticalRun.dimension.value * widget.getDimensionRatio());
                                }
                                break;
                                case ConstraintWidget.UNKNOWN: {
                                    size = (int) (0.5f + widget.verticalRun.dimension.value * widget.getDimensionRatio());
                                }
                                break;
                                default: break;
                            }
                            dimension.resolve(size);
                        }
                    }
                    break;
                    case MATCH_CONSTRAINT_PERCENT: {
                        ConstraintWidget parent = widget.getParent();
                        if (parent != null) {
                            if (parent.horizontalRun.dimension.resolved) {
                                float percent = widget.mMatchConstraintPercentWidth;
                                int targetDimensionValue = parent.horizontalRun.dimension.value;
                                int size = (int) (0.5f + targetDimensionValue * percent);
                                dimension.resolve(size);
                            }
                        }
                    }
                    break;
                    default: break;
                }
            }
        }

        if (!(start.readyToSolve && end.readyToSolve)) {
            return;
        }

        if (start.resolved && end.resolved && dimension.resolved) {
            return;
        }

        if (!dimension.resolved
                && dimensionBehavior == MATCH_CONSTRAINT
                && widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD
                && !widget.isInHorizontalChain()) {

            DependencyNode startTarget = start.targets.get(0);
            DependencyNode endTarget = end.targets.get(0);
            int startPos = startTarget.value + start.margin;
            int endPos = endTarget.value + end.margin;

            int distance = endPos - startPos;
            start.resolve(startPos);
            end.resolve(endPos);
            dimension.resolve(distance);
            return;
        }

        if (!dimension.resolved
                && dimensionBehavior == MATCH_CONSTRAINT
                && matchConstraintsType == MATCH_CONSTRAINT_WRAP) {
            if (start.targets.size() > 0 && end.targets.size() > 0) {
                DependencyNode startTarget = start.targets.get(0);
                DependencyNode endTarget = end.targets.get(0);
                int startPos = startTarget.value + start.margin;
                int endPos = endTarget.value + end.margin;
                int availableSpace = endPos - startPos;
                int value = Math.min(availableSpace, dimension.wrapValue);
                int max = widget.mMatchConstraintMaxWidth;
                int min = widget.mMatchConstraintMinWidth;
                value = Math.max(min, value);
                if (max > 0) {
                    value = Math.min(max, value);
                }
                dimension.resolve(value);
            }
        }

        if (!dimension.resolved) {
            return;
        }
        // ready to solve, centering.
        DependencyNode startTarget = start.targets.get(0);
        DependencyNode endTarget = end.targets.get(0);
        int startPos = startTarget.value + start.margin;
        int endPos = endTarget.value + end.margin;
        float bias = widget.getHorizontalBiasPercent();
        if (startTarget == endTarget) {
            startPos = startTarget.value;
            endPos = endTarget.value;
            // TODO: this might be a nice feature to support, but I guess for now let's stay
            // compatible with 1.1
            bias = 0.5f;
        }
        int distance = (endPos - startPos - dimension.value);
        start.resolve((int) (0.5f + startPos + distance * bias));
        end.resolve(start.value + dimension.value);
    }

    public void applyToWidget() {
        if (start.resolved) {
            widget.setX(start.value);
        }
    }

}
