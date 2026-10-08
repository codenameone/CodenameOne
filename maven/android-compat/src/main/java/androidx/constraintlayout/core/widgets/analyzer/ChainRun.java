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
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;

import java.util.ArrayList;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.GONE;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_WRAP;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;

public class ChainRun extends WidgetRun {
    ArrayList<WidgetRun> widgets = new ArrayList<>();
    private int chainStyle;

    public ChainRun(ConstraintWidget widget, int orientation) {
        super(widget);
        this.orientation = orientation;
        build();
    }

    @Override
    public String toString() {
        StringBuilder log = new StringBuilder("ChainRun ");
        log.append((orientation == HORIZONTAL ? "horizontal : " : "vertical : "));
        for (WidgetRun run : widgets) {
            log.append("<");
            log.append(run);
            log.append("> ");
        }
        return log.toString();
    }

    @Override
    boolean supportsWrapComputation() {
        final int count = widgets.size();
        for (int i = 0; i < count; i++) {
            WidgetRun run = widgets.get(i);
            if (!run.supportsWrapComputation()) {
                return false;
            }
        }
        return true;
    }

    public long getWrapDimension() {
        final int count = widgets.size();
        long wrapDimension = 0;
        for (int i = 0; i < count; i++) {
            WidgetRun run = widgets.get(i);
            wrapDimension += run.start.margin;
            wrapDimension += run.getWrapDimension();
            wrapDimension += run.end.margin;
        }
        return wrapDimension;
    }

    private void build() {
        ConstraintWidget current = widget;
        ConstraintWidget previous = current.getPreviousChainMember(orientation);
        while (previous != null) {
            current = previous;
            previous = current.getPreviousChainMember(orientation);
        }
        widget = current; // first element of the chain
        widgets.add(current.getRun(orientation));
        ConstraintWidget next = current.getNextChainMember(orientation);
        while (next != null) {
            current = next;
            widgets.add(current.getRun(orientation));
            next = current.getNextChainMember(orientation);
        }
        for (WidgetRun run : widgets) {
            if (orientation == HORIZONTAL) {
                run.widget.horizontalChainRun = this;
            } else if (orientation == ConstraintWidget.VERTICAL) {
                run.widget.verticalChainRun = this;
            }
        }
        boolean isInRtl = (orientation == HORIZONTAL) && ((ConstraintWidgetContainer) widget.getParent()).isRtl();
        if (isInRtl && widgets.size() > 1) {
            widget = widgets.get(widgets.size() - 1).widget;
        }
        chainStyle = orientation == HORIZONTAL ? widget.getHorizontalChainStyle() : widget.getVerticalChainStyle();
    }


    @Override
    void clear() {
        runGroup = null;
        for (WidgetRun run : widgets) {
            run.clear();
        }
    }

    @Override
    void reset() {
        start.resolved = false;
        end.resolved = false;
    }

    @Override
    public void update(Dependency dependency) {
        if (!(start.resolved && end.resolved)) {
            return;
        }

        ConstraintWidget parent = widget.getParent();
        boolean isInRtl = false;
        if (parent instanceof ConstraintWidgetContainer) {
            isInRtl = ((ConstraintWidgetContainer) parent).isRtl();
        }
        int distance = end.value - start.value;
        int size = 0;
        int numMatchConstraints = 0;
        float weights = 0;
        int numVisibleWidgets = 0;
        final int count = widgets.size();
        // let's find the first visible widget...
        int firstVisibleWidget = -1;
        for (int i = 0; i < count; i++) {
            WidgetRun run = widgets.get(i);
            if (run.widget.getVisibility() == GONE) {
                continue;
            }
            firstVisibleWidget = i;
            break;
        }
        // now the last visible widget...
        int lastVisibleWidget = -1;
        for (int i = count - 1; i >= 0; i--) {
            WidgetRun run = widgets.get(i);
            if (run.widget.getVisibility() == GONE) {
                continue;
            }
            lastVisibleWidget = i;
            break;
        }
        for (int j = 0; j< 2; j++ ){
            for (int i = 0; i < count; i++) {
                WidgetRun run = widgets.get(i);
                if (run.widget.getVisibility() == GONE) {
                    continue;
                }
                numVisibleWidgets++;
                if (i > 0 && i >= firstVisibleWidget) {
                    size += run.start.margin;
                }
                int dimension = run.dimension.value;
                boolean treatAsFixed = run.dimensionBehavior != MATCH_CONSTRAINT;
                if (treatAsFixed) {
                    if (orientation == HORIZONTAL && !run.widget.horizontalRun.dimension.resolved) {
                        return;
                    }
                    if (orientation == VERTICAL && !run.widget.verticalRun.dimension.resolved) {
                        return;
                    }
                } else if (run.matchConstraintsType == MATCH_CONSTRAINT_WRAP && j == 0) {
                    treatAsFixed = true;
                    dimension = run.dimension.wrapValue;
                    numMatchConstraints++;
                } else if (run.dimension.resolved) {
                    treatAsFixed = true;
                }
                if (!treatAsFixed) { // only for the first pass
                    numMatchConstraints++;
                    float weight = run.widget.mWeight[orientation];
                    if (weight >= 0) {
                        weights += weight;
                    }
                } else {
                    size += dimension;
                }
                if (i < count - 1 && i < lastVisibleWidget) {
                    size += -run.end.margin;
                }
            }
            if (size < distance || numMatchConstraints == 0) {
                break; // we are good to go!
            }
            // otherwise, let's do another pass with using match_constraints
            numVisibleWidgets = 0;
            numMatchConstraints = 0;
            size = 0;
            weights = 0;
        }

        int position = start.value;
        if (isInRtl) {
            position = end.value;
        }
        if (size > distance) {
            if (isInRtl) {
                position += (int) (0.5f + (size - distance) / 2f);
            } else {
                position -= (int) (0.5f + (size - distance) / 2f);
            }
        }
        int matchConstraintsDimension = 0;
        if (numMatchConstraints > 0) {
            matchConstraintsDimension = (int) (0.5f + (distance - size) / (float) numMatchConstraints);

            int appliedLimits = 0;
            for (int i = 0; i < count; i++) {
                WidgetRun run = widgets.get(i);
                if (run.widget.getVisibility() == GONE) {
                    continue;
                }
                if (run.dimensionBehavior == MATCH_CONSTRAINT && !run.dimension.resolved) {
                    int dimension = matchConstraintsDimension;
                    if (weights > 0) {
                        float weight = run.widget.mWeight[orientation];
                        dimension = (int) (0.5f + weight * (distance - size) / weights);
                    }
                    int max;
                    int min;
                    int value = dimension;
                    if (orientation == HORIZONTAL) {
                        max = run.widget.mMatchConstraintMaxWidth;
                        min = run.widget.mMatchConstraintMinWidth;
                    } else {
                        max = run.widget.mMatchConstraintMaxHeight;
                        min = run.widget.mMatchConstraintMinHeight;
                    }
                    if (run.matchConstraintsType == MATCH_CONSTRAINT_WRAP) {
                        value = Math.min(value, run.dimension.wrapValue);
                    }
                    value = Math.max(min, value);
                    if (max > 0) {
                        value = Math.min(max, value);
                    }
                    if (value != dimension) {
                        appliedLimits++;
                        dimension = value;
                    }
                    run.dimension.resolve(dimension);
                }
            }
            if (appliedLimits > 0) {
                numMatchConstraints -= appliedLimits;
                // we have to recompute the sizes
                size = 0;
                for (int i = 0; i < count; i++) {
                    WidgetRun run = widgets.get(i);
                    if (run.widget.getVisibility() == GONE) {
                        continue;
                    }
                    if (i > 0 && i >= firstVisibleWidget) {
                        size += run.start.margin;
                    }
                    size += run.dimension.value;
                    if (i < count - 1 && i < lastVisibleWidget) {
                        size += -run.end.margin;
                    }
                }
            }
            if (chainStyle == ConstraintWidget.CHAIN_PACKED && appliedLimits == 0) {
                chainStyle = ConstraintWidget.CHAIN_SPREAD;
            }
        }

        if (size > distance) {
            chainStyle = ConstraintWidget.CHAIN_PACKED;
        }

        if (numVisibleWidgets > 0 && numMatchConstraints == 0 && firstVisibleWidget == lastVisibleWidget) {
            // only one widget of fixed size to display...
            chainStyle = ConstraintWidget.CHAIN_PACKED;
        }

        if (chainStyle == ConstraintWidget.CHAIN_SPREAD_INSIDE) {
            int gap = 0;
            if (numVisibleWidgets > 1) {
                gap = (distance - size) / (numVisibleWidgets - 1);
            } else if (numVisibleWidgets == 1) {
                gap = (distance - size) / 2;
            }
            if (numMatchConstraints > 0) {
                gap = 0;
            }
            for (int i = 0; i < count; i++) {
                int index = i;
                if (isInRtl) {
                    index = count - (i + 1);
                }
                WidgetRun run = widgets.get(index);
                if (run.widget.getVisibility() == GONE) {
                    run.start.resolve(position);
                    run.end.resolve(position);
                    continue;
                }
                if (i > 0) {
                    if (isInRtl) {
                        position -= gap;
                    } else {
                        position += gap;
                    }
                }
                if (i > 0 && i >= firstVisibleWidget) {
                    if (isInRtl) {
                        position -= run.start.margin;
                    } else {
                        position += run.start.margin;
                    }
                }

                if (isInRtl) {
                    run.end.resolve(position);
                } else {
                    run.start.resolve(position);
                }

                int dimension = run.dimension.value;
                if (run.dimensionBehavior == MATCH_CONSTRAINT
                        && run.matchConstraintsType == MATCH_CONSTRAINT_WRAP) {
                    dimension = run.dimension.wrapValue;
                }
                if (isInRtl) {
                    position -= dimension;
                } else {
                    position += dimension;
                }

                if (isInRtl) {
                    run.start.resolve(position);
                } else {
                    run.end.resolve(position);
                }
                run.resolved = true;
                if (i < count - 1 && i < lastVisibleWidget) {
                    if (isInRtl) {
                        position -= -run.end.margin;
                    } else {
                        position += -run.end.margin;
                    }
                }
            }
        } else if (chainStyle == ConstraintWidget.CHAIN_SPREAD) {
            int gap = (distance - size) / (numVisibleWidgets + 1);
            if (numMatchConstraints > 0) {
                gap = 0;
            }
            for (int i = 0; i < count; i++) {
                int index = i;
                if (isInRtl) {
                    index = count - (i + 1);
                }
                WidgetRun run = widgets.get(index);
                if (run.widget.getVisibility() == GONE) {
                    run.start.resolve(position);
                    run.end.resolve(position);
                    continue;
                }
                if (isInRtl) {
                    position -= gap;
                } else {
                    position += gap;
                }
                if (i > 0 && i >= firstVisibleWidget) {
                    if (isInRtl) {
                        position -= run.start.margin;
                    } else {
                        position += run.start.margin;
                    }
                }

                if (isInRtl) {
                    run.end.resolve(position);
                } else {
                    run.start.resolve(position);
                }

                int dimension = run.dimension.value;
                if (run.dimensionBehavior == MATCH_CONSTRAINT
                        && run.matchConstraintsType == MATCH_CONSTRAINT_WRAP) {
                    dimension = Math.min(dimension, run.dimension.wrapValue);
                }

                if (isInRtl) {
                    position -= dimension;
                } else {
                    position += dimension;
                }

                if (isInRtl) {
                    run.start.resolve(position);
                } else {
                    run.end.resolve(position);
                }
                if (i < count - 1 && i < lastVisibleWidget) {
                    if (isInRtl) {
                        position -= -run.end.margin;
                    } else {
                        position += -run.end.margin;
                    }
                }
            }
        } else if (chainStyle == ConstraintWidget.CHAIN_PACKED) {
            float bias = (orientation == HORIZONTAL) ? widget.getHorizontalBiasPercent()
                    : widget.getVerticalBiasPercent();
            if (isInRtl) {
                bias = 1 - bias;
            }
            int gap = (int) (0.5f + (distance - size) * bias);
            if (gap < 0 || numMatchConstraints > 0) {
                gap = 0;
            }
            if (isInRtl) {
                position -= gap;
            } else {
                position += gap;
            }
            for (int i = 0; i < count; i++) {
                int index = i;
                if (isInRtl) {
                    index = count - (i + 1);
                }
                WidgetRun run = widgets.get(index);
                if (run.widget.getVisibility() == GONE) {
                    run.start.resolve(position);
                    run.end.resolve(position);
                    continue;
                }
                if (i > 0 && i >= firstVisibleWidget) {
                    if (isInRtl) {
                        position -= run.start.margin;
                    } else {
                        position += run.start.margin;
                    }
                }
                if (isInRtl) {
                    run.end.resolve(position);
                } else {
                    run.start.resolve(position);
                }

                int dimension = run.dimension.value;
                if (run.dimensionBehavior == MATCH_CONSTRAINT
                        && run.matchConstraintsType == MATCH_CONSTRAINT_WRAP) {
                    dimension = run.dimension.wrapValue;
                }
                if (isInRtl) {
                    position -= dimension;
                } else {
                    position += dimension;
                }

                if (isInRtl) {
                    run.start.resolve(position);
                } else {
                    run.end.resolve(position);
                }
                if (i < count - 1 && i < lastVisibleWidget) {
                    if (isInRtl) {
                        position -= -run.end.margin;
                    } else {
                        position += -run.end.margin;
                    }
                }
            }
        }
    }

    public void applyToWidget() {
        for (int i = 0; i < widgets.size(); i++) {
            WidgetRun run = widgets.get(i);
            run.applyToWidget();
        }
    }

    private ConstraintWidget getFirstVisibleWidget() {
        for (int i = 0; i < widgets.size(); i++) {
            WidgetRun run = widgets.get(i);
            if (run.widget.getVisibility() != GONE) {
                return run.widget;
            }
        }
        return null;
    }

    private ConstraintWidget getLastVisibleWidget() {
        for (int i = widgets.size() -1; i >= 0; i--) {
            WidgetRun run = widgets.get(i);
            if (run.widget.getVisibility() != GONE) {
                return run.widget;
            }
        }
        return null;
    }


    @Override
    void apply() {
        for (WidgetRun run : widgets) {
            run.apply();
        }
        int count = widgets.size();
        if (count < 1) {
            return;
        }

        // get the first and last element of the chain
        ConstraintWidget firstWidget = widgets.get(0).widget;
        ConstraintWidget lastWidget = widgets.get(count - 1).widget;

        if (orientation == HORIZONTAL) {
            ConstraintAnchor startAnchor = firstWidget.mLeft;
            ConstraintAnchor endAnchor = lastWidget.mRight;
            DependencyNode startTarget = getTarget(startAnchor, HORIZONTAL);
            int startMargin = startAnchor.getMargin();
            ConstraintWidget firstVisibleWidget = getFirstVisibleWidget();
            if (firstVisibleWidget != null) {
                startMargin = firstVisibleWidget.mLeft.getMargin();
            }
            if (startTarget != null) {
                addTarget(start, startTarget, startMargin);
            }
            DependencyNode endTarget = getTarget(endAnchor, HORIZONTAL);
            int endMargin = endAnchor.getMargin();
            ConstraintWidget lastVisibleWidget = getLastVisibleWidget();
            if (lastVisibleWidget != null) {
                endMargin = lastVisibleWidget.mRight.getMargin();
            }
            if (endTarget != null) {
                addTarget(end, endTarget, -endMargin);
            }
        } else {
            ConstraintAnchor startAnchor = firstWidget.mTop;
            ConstraintAnchor endAnchor = lastWidget.mBottom;
            DependencyNode startTarget = getTarget(startAnchor, VERTICAL);
            int startMargin = startAnchor.getMargin();
            ConstraintWidget firstVisibleWidget = getFirstVisibleWidget();
            if (firstVisibleWidget != null) {
                startMargin = firstVisibleWidget.mTop.getMargin();
            }
            if (startTarget != null) {
                addTarget(start, startTarget, startMargin);
            }
            DependencyNode endTarget = getTarget(endAnchor, VERTICAL);
            int endMargin = endAnchor.getMargin();
            ConstraintWidget lastVisibleWidget = getLastVisibleWidget();
            if (lastVisibleWidget != null) {
                endMargin = lastVisibleWidget.mBottom.getMargin();
            }
            if (endTarget != null) {
                addTarget(end, endTarget, -endMargin);
            }
        }
        start.updateDelegate = this;
        end.updateDelegate = this;
    }

}
