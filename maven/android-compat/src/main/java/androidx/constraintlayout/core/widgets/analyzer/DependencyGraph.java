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

import androidx.constraintlayout.core.widgets.Barrier;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Guideline;
import androidx.constraintlayout.core.widgets.HelperWidget;

import java.util.ArrayList;
import java.util.HashSet;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.FIXED;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_PARENT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.GONE;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_PERCENT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_RATIO;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_SPREAD;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_WRAP;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.UNKNOWN;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;

public class DependencyGraph {
    private static final boolean USE_GROUPS = true;
    private ConstraintWidgetContainer container;
    private boolean mNeedBuildGraph = true;
    private boolean mNeedRedoMeasures = true;
    private ConstraintWidgetContainer mContainer;
    private ArrayList<WidgetRun> mRuns = new ArrayList<>();

    // TODO: Unused, should we delete?
    private ArrayList<RunGroup> runGroups = new ArrayList<>();

    public DependencyGraph(ConstraintWidgetContainer container) {
        this.container = container;
        mContainer = container;
    }

    private BasicMeasure.Measurer mMeasurer = null;
    private BasicMeasure.Measure mMeasure = new BasicMeasure.Measure();

    public void setMeasurer(BasicMeasure.Measurer measurer) {
        mMeasurer = measurer;
    }

    private int computeWrap(ConstraintWidgetContainer container, int orientation) {
        final int count = mGroups.size();
        long wrapSize = 0;
        for (int i = 0; i < count; i++) {
            RunGroup run = mGroups.get(i);
            long size = run.computeWrapSize(container, orientation);
            wrapSize = Math.max(wrapSize, size);
        }
        return (int) wrapSize;
    }

    /**
     * Find and mark terminal widgets (trailing widgets) -- they are the only
     * ones we need to care for wrap_content checks
     * @param horizontalBehavior
     * @param verticalBehavior
     */
    public void defineTerminalWidgets(ConstraintWidget.DimensionBehaviour horizontalBehavior, ConstraintWidget.DimensionBehaviour verticalBehavior) {
        if (mNeedBuildGraph) {
            buildGraph();

            if (USE_GROUPS) {
                boolean hasBarrier = false;
                for (ConstraintWidget widget : container.mChildren) {
                    widget.isTerminalWidget[HORIZONTAL] = true;
                    widget.isTerminalWidget[VERTICAL] = true;
                    if (widget instanceof Barrier) {
                        hasBarrier = true;
                    }
                }
                if (!hasBarrier) {
                    for (RunGroup group : mGroups) {
                        group.defineTerminalWidgets(horizontalBehavior == WRAP_CONTENT, verticalBehavior == WRAP_CONTENT);
                    }
                }
            }
        }
    }

    /**
     * Try to measure the layout by solving the graph of constraints directly
     *
     * @param optimizeWrap use the wrap_content optimizer
     * @return true if all widgets have been resolved
     */
    public boolean directMeasure(boolean optimizeWrap) {
        optimizeWrap &= USE_GROUPS;

        if (mNeedBuildGraph || mNeedRedoMeasures) {
            for (ConstraintWidget widget : container.mChildren) {
                widget.ensureWidgetRuns();
                widget.measured = false;
                widget.horizontalRun.reset();
                widget.verticalRun.reset();
            }
            container.ensureWidgetRuns();
            container.measured = false;
            container.horizontalRun.reset();
            container.verticalRun.reset();
            mNeedRedoMeasures = false;
        }

        boolean avoid = basicMeasureWidgets(mContainer);
        if (avoid) {
            return false;
        }

        container.setX(0);
        container.setY(0);

        ConstraintWidget.DimensionBehaviour originalHorizontalDimension = container.getDimensionBehaviour(HORIZONTAL);
        ConstraintWidget.DimensionBehaviour originalVerticalDimension = container.getDimensionBehaviour(VERTICAL);

        if (mNeedBuildGraph) {
            buildGraph();
        }

        int x1 = container.getX();
        int y1 = container.getY();

        container.horizontalRun.start.resolve(x1);
        container.verticalRun.start.resolve(y1);

        // Let's do the easy steps first -- anything that can be immediately measured
        // Whatever is left for the dimension will be match_constraints.
        measureWidgets();

        // If we have to support wrap, let's see if we can compute it directly
        if (originalHorizontalDimension == WRAP_CONTENT || originalVerticalDimension == WRAP_CONTENT) {
            if (optimizeWrap) {
                for (WidgetRun run : mRuns) {
                    if (!run.supportsWrapComputation()) {
                        optimizeWrap = false;
                        break;
                    }
                }
            }

            if (optimizeWrap && originalHorizontalDimension == WRAP_CONTENT) {
                container.setHorizontalDimensionBehaviour(FIXED);
                container.setWidth(computeWrap(container, HORIZONTAL));
                container.horizontalRun.dimension.resolve(container.getWidth());
            }
            if (optimizeWrap && originalVerticalDimension == WRAP_CONTENT) {
                container.setVerticalDimensionBehaviour(FIXED);
                container.setHeight(computeWrap(container, VERTICAL));
                container.verticalRun.dimension.resolve(container.getHeight());
            }
        }

        boolean checkRoot = false;

        // Now, depending on our own dimension behavior, we may want to solve
        // one dimension before the other

        if (container.mListDimensionBehaviors[HORIZONTAL] == ConstraintWidget.DimensionBehaviour.FIXED
                || container.mListDimensionBehaviors[HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {

            // solve horizontal dimension
            int x2 = x1 + container.getWidth();
            container.horizontalRun.end.resolve(x2);
            container.horizontalRun.dimension.resolve(x2 - x1);
            measureWidgets();
            if (container.mListDimensionBehaviors[VERTICAL] == FIXED
                    || container.mListDimensionBehaviors[VERTICAL] == MATCH_PARENT) {
                int y2 = y1 + container.getHeight();
                container.verticalRun.end.resolve(y2);
                container.verticalRun.dimension.resolve(y2 - y1);
            }
            measureWidgets();
            checkRoot = true;
        } else {
            // we'll bail out to the solver...
        }

        // Let's apply what we did resolve
        for (WidgetRun run : mRuns) {
            if (run.widget == container && !run.resolved) {
                continue;
            }
            run.applyToWidget();
        }

        boolean allResolved = true;
        for (WidgetRun run : mRuns) {
            if (!checkRoot && run.widget == container) {
                continue;
            }
            if (!run.start.resolved) {
                allResolved = false;
                break;
            }
            if (!run.end.resolved && !(run instanceof GuidelineReference)) {
                allResolved = false;
                break;
            }
            if (!run.dimension.resolved && !(run instanceof ChainRun) && !(run instanceof GuidelineReference)) {
                allResolved = false;
                break;
            }
        }

        container.setHorizontalDimensionBehaviour(originalHorizontalDimension);
        container.setVerticalDimensionBehaviour(originalVerticalDimension);

        return allResolved;
    }

    public boolean directMeasureSetup(boolean optimizeWrap) {
        if (mNeedBuildGraph) {
            for (ConstraintWidget widget : container.mChildren) {
                widget.ensureWidgetRuns();
                widget.measured = false;
                widget.horizontalRun.dimension.resolved = false;
                widget.horizontalRun.resolved = false;
                widget.horizontalRun.reset();
                widget.verticalRun.dimension.resolved = false;
                widget.verticalRun.resolved = false;
                widget.verticalRun.reset();
            }
            container.ensureWidgetRuns();
            container.measured = false;
            container.horizontalRun.dimension.resolved = false;
            container.horizontalRun.resolved = false;
            container.horizontalRun.reset();
            container.verticalRun.dimension.resolved = false;
            container.verticalRun.resolved = false;
            container.verticalRun.reset();
            buildGraph();
        }

        boolean avoid = basicMeasureWidgets(mContainer);
        if (avoid) {
            return false;
        }

        container.setX(0);
        container.setY(0);
        container.horizontalRun.start.resolve(0);
        container.verticalRun.start.resolve(0);
        return true;
    }

    public boolean directMeasureWithOrientation(boolean optimizeWrap, int orientation) {
        optimizeWrap &= USE_GROUPS;

        ConstraintWidget.DimensionBehaviour originalHorizontalDimension = container.getDimensionBehaviour(HORIZONTAL);
        ConstraintWidget.DimensionBehaviour originalVerticalDimension = container.getDimensionBehaviour(VERTICAL);

        int x1 = container.getX();
        int y1 = container.getY();

        // If we have to support wrap, let's see if we can compute it directly
        if (optimizeWrap && (originalHorizontalDimension == WRAP_CONTENT || originalVerticalDimension == WRAP_CONTENT)) {
            for (WidgetRun run : mRuns) {
                if (run.orientation == orientation
                        && !run.supportsWrapComputation()) {
                    optimizeWrap = false;
                    break;
                }
            }

            if (orientation == HORIZONTAL) {
                if (optimizeWrap && originalHorizontalDimension == WRAP_CONTENT) {
                    container.setHorizontalDimensionBehaviour(FIXED);
                    container.setWidth(computeWrap(container, HORIZONTAL));
                    container.horizontalRun.dimension.resolve(container.getWidth());
                }
            } else {
                if (optimizeWrap && originalVerticalDimension == WRAP_CONTENT) {
                    container.setVerticalDimensionBehaviour(FIXED);
                    container.setHeight(computeWrap(container, VERTICAL));
                    container.verticalRun.dimension.resolve(container.getHeight());
                }
            }
        }

        boolean checkRoot = false;

        // Now, depending on our own dimension behavior, we may want to solve
        // one dimension before the other

        if (orientation == HORIZONTAL) {
            if (container.mListDimensionBehaviors[HORIZONTAL] == FIXED
                    || container.mListDimensionBehaviors[HORIZONTAL] == MATCH_PARENT) {
                int x2 = x1 + container.getWidth();
                container.horizontalRun.end.resolve(x2);
                container.horizontalRun.dimension.resolve(x2 - x1);
                checkRoot = true;
            }
        } else {
            if (container.mListDimensionBehaviors[VERTICAL] == FIXED
                    || container.mListDimensionBehaviors[VERTICAL] == MATCH_PARENT) {
                int y2 = y1 + container.getHeight();
                container.verticalRun.end.resolve(y2);
                container.verticalRun.dimension.resolve(y2 - y1);
                checkRoot = true;
            }
        }
        measureWidgets();

        // Let's apply what we did resolve
        for (WidgetRun run : mRuns) {
            if (run.orientation != orientation) {
                continue;
            }
            if (run.widget == container && !run.resolved) {
                continue;
            }
            run.applyToWidget();
        }

        boolean allResolved = true;
        for (WidgetRun run : mRuns) {
            if (run.orientation != orientation) {
                continue;
            }
            if (!checkRoot && run.widget == container) {
                continue;
            }
            if (!run.start.resolved) {
                allResolved = false;
                break;
            }
            if (!run.end.resolved) {
                allResolved = false;
                break;
            }
            if (!(run instanceof ChainRun) && !run.dimension.resolved) {
                allResolved = false;
                break;
            }
        }

        container.setHorizontalDimensionBehaviour(originalHorizontalDimension);
        container.setVerticalDimensionBehaviour(originalVerticalDimension);

        return allResolved;
    }

    /**
     * Convenience function to fill in the measure spec
     *
     * @param widget the widget to measure
     * @param horizontalBehavior
     * @param horizontalDimension
     * @param verticalBehavior
     * @param verticalDimension
     */
    private void measure(ConstraintWidget widget,
                         ConstraintWidget.DimensionBehaviour horizontalBehavior, int horizontalDimension,
                         ConstraintWidget.DimensionBehaviour verticalBehavior, int verticalDimension) {
        mMeasure.horizontalBehavior = horizontalBehavior;
        mMeasure.verticalBehavior = verticalBehavior;
        mMeasure.horizontalDimension = horizontalDimension;
        mMeasure.verticalDimension = verticalDimension;
        mMeasurer.measure(widget, mMeasure);
        widget.setWidth(mMeasure.measuredWidth);
        widget.setHeight(mMeasure.measuredHeight);
        widget.setHasBaseline(mMeasure.measuredHasBaseline);
        widget.setBaselineDistance(mMeasure.measuredBaseline);
    }

    private boolean basicMeasureWidgets(ConstraintWidgetContainer constraintWidgetContainer) {
        for (ConstraintWidget widget : constraintWidgetContainer.mChildren) {
            ConstraintWidget.DimensionBehaviour horizontal = widget.mListDimensionBehaviors[HORIZONTAL];
            ConstraintWidget.DimensionBehaviour vertical = widget.mListDimensionBehaviors[VERTICAL];

            if (widget.getVisibility() == GONE) {
                widget.measured = true;
                continue;
            }

            // Basic validation
            // TODO: might move this earlier in the process
            if (widget.mMatchConstraintPercentWidth < 1 && horizontal == MATCH_CONSTRAINT) {
                widget.mMatchConstraintDefaultWidth = MATCH_CONSTRAINT_PERCENT;
            }
            if (widget.mMatchConstraintPercentHeight < 1 && vertical == MATCH_CONSTRAINT) {
                widget.mMatchConstraintDefaultHeight = MATCH_CONSTRAINT_PERCENT;
            }
            if (widget.getDimensionRatio() > 0) {
                if (horizontal == MATCH_CONSTRAINT && (vertical == WRAP_CONTENT || vertical == FIXED)) {
                    widget.mMatchConstraintDefaultWidth = MATCH_CONSTRAINT_RATIO;
                } else if (vertical == MATCH_CONSTRAINT && (horizontal == WRAP_CONTENT || horizontal == FIXED)) {
                    widget.mMatchConstraintDefaultHeight = MATCH_CONSTRAINT_RATIO;
                } else if (horizontal == MATCH_CONSTRAINT && vertical == MATCH_CONSTRAINT) {
                    if (widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD) {
                        widget.mMatchConstraintDefaultWidth = MATCH_CONSTRAINT_RATIO;
                    }
                    if (widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD) {
                        widget.mMatchConstraintDefaultHeight = MATCH_CONSTRAINT_RATIO;
                    }
                }
            }

            if (horizontal == MATCH_CONSTRAINT && widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_WRAP) {
                if (widget.mLeft.mTarget == null || widget.mRight.mTarget == null) {
                    horizontal = WRAP_CONTENT;
                }
            }
            if (vertical == MATCH_CONSTRAINT && widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_WRAP) {
                if (widget.mTop.mTarget == null || widget.mBottom.mTarget == null) {
                    vertical = WRAP_CONTENT;
                }
            }

            widget.horizontalRun.dimensionBehavior = horizontal;
            widget.horizontalRun.matchConstraintsType = widget.mMatchConstraintDefaultWidth;
            widget.verticalRun.dimensionBehavior = vertical;
            widget.verticalRun.matchConstraintsType = widget.mMatchConstraintDefaultHeight;

            if ((horizontal == MATCH_PARENT || horizontal == FIXED || horizontal == WRAP_CONTENT)
                    && (vertical == MATCH_PARENT || vertical == FIXED || vertical == WRAP_CONTENT)) {
                int width = widget.getWidth();
                if (horizontal == MATCH_PARENT) {
                    width = constraintWidgetContainer.getWidth() - widget.mLeft.mMargin - widget.mRight.mMargin;
                    horizontal = FIXED;
                }
                int height = widget.getHeight();
                if (vertical == MATCH_PARENT) {
                    height = constraintWidgetContainer.getHeight() - widget.mTop.mMargin - widget.mBottom.mMargin;
                    vertical = FIXED;
                }
                measure(widget, horizontal, width, vertical, height);
                widget.horizontalRun.dimension.resolve(widget.getWidth());
                widget.verticalRun.dimension.resolve(widget.getHeight());
                widget.measured = true;
                continue;
            }

            if (horizontal == MATCH_CONSTRAINT && (vertical == WRAP_CONTENT || vertical == FIXED)) {
                if (widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_RATIO) {
                    if (vertical == WRAP_CONTENT) {
                        measure(widget, WRAP_CONTENT, 0, WRAP_CONTENT, 0);
                    }
                    int height = widget.getHeight();
                    int width = (int) (height * widget.mDimensionRatio + 0.5f);
                    measure(widget, FIXED, width, FIXED, height);
                    widget.horizontalRun.dimension.resolve(widget.getWidth());
                    widget.verticalRun.dimension.resolve(widget.getHeight());
                    widget.measured = true;
                    continue;
                } else if (widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_WRAP) {
                    measure(widget, WRAP_CONTENT, 0, vertical, 0);
                    widget.horizontalRun.dimension.wrapValue = widget.getWidth();
                    continue;
                } else if (widget.mMatchConstraintDefaultWidth == ConstraintWidget.MATCH_CONSTRAINT_PERCENT) {
                    if (constraintWidgetContainer.mListDimensionBehaviors[HORIZONTAL] == FIXED
                            || constraintWidgetContainer.mListDimensionBehaviors[HORIZONTAL] == MATCH_PARENT) {
                        float percent = widget.mMatchConstraintPercentWidth;
                        int width = (int) (0.5f + percent * constraintWidgetContainer.getWidth());
                        int height = widget.getHeight();
                        measure(widget, FIXED, width, vertical, height);
                        widget.horizontalRun.dimension.resolve(widget.getWidth());
                        widget.verticalRun.dimension.resolve(widget.getHeight());
                        widget.measured = true;
                        continue;
                    }
                } else {
                    // let's verify we have both constraints
                    if (widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].mTarget == null
                            || widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].mTarget == null) {
                        measure(widget, WRAP_CONTENT, 0, vertical, 0);
                        widget.horizontalRun.dimension.resolve(widget.getWidth());
                        widget.verticalRun.dimension.resolve(widget.getHeight());
                        widget.measured = true;
                        continue;
                    }
                }
            }
            if (vertical == MATCH_CONSTRAINT && (horizontal == WRAP_CONTENT || horizontal == FIXED)) {
                if (widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_RATIO) {
                    if (horizontal == WRAP_CONTENT) {
                        measure(widget, WRAP_CONTENT, 0, WRAP_CONTENT, 0);
                    }
                    int width = widget.getWidth();
                    float ratio = widget.mDimensionRatio;
                    if (widget.getDimensionRatioSide() == UNKNOWN) {
                        ratio = 1f / ratio;
                    }
                    int height = (int) (width * ratio + 0.5f);

                    measure(widget, FIXED, width, FIXED, height);
                    widget.horizontalRun.dimension.resolve(widget.getWidth());
                    widget.verticalRun.dimension.resolve(widget.getHeight());
                    widget.measured = true;
                    continue;
                } else if (widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_WRAP) {
                    measure(widget, horizontal, 0, WRAP_CONTENT, 0);
                    widget.verticalRun.dimension.wrapValue = widget.getHeight();
                    continue;
                } else if (widget.mMatchConstraintDefaultHeight == ConstraintWidget.MATCH_CONSTRAINT_PERCENT) {
                    if (constraintWidgetContainer.mListDimensionBehaviors[VERTICAL] == FIXED
                            || constraintWidgetContainer.mListDimensionBehaviors[VERTICAL] == MATCH_PARENT) {
                        float percent = widget.mMatchConstraintPercentHeight;
                        int width = widget.getWidth();
                        int height = (int) (0.5f + percent * constraintWidgetContainer.getHeight());
                        measure(widget, horizontal, width, FIXED, height);
                        widget.horizontalRun.dimension.resolve(widget.getWidth());
                        widget.verticalRun.dimension.resolve(widget.getHeight());
                        widget.measured = true;
                        continue;
                    }
                } else {
                    // let's verify we have both constraints
                    if (widget.mListAnchors[ConstraintWidget.ANCHOR_TOP].mTarget == null
                            || widget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM].mTarget == null) {
                        measure(widget, WRAP_CONTENT, 0, vertical, 0);
                        widget.horizontalRun.dimension.resolve(widget.getWidth());
                        widget.verticalRun.dimension.resolve(widget.getHeight());
                        widget.measured = true;
                        continue;
                    }
                }
            }
            if (horizontal == MATCH_CONSTRAINT && vertical == MATCH_CONSTRAINT) {
                if (widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_WRAP
                        || widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_WRAP) {
                    measure(widget, WRAP_CONTENT, 0, WRAP_CONTENT, 0);
                    widget.horizontalRun.dimension.wrapValue = widget.getWidth();
                    widget.verticalRun.dimension.wrapValue = widget.getHeight();
                } else if (widget.mMatchConstraintDefaultHeight == ConstraintWidget.MATCH_CONSTRAINT_PERCENT
                        && widget.mMatchConstraintDefaultWidth == ConstraintWidget.MATCH_CONSTRAINT_PERCENT
                        && constraintWidgetContainer.mListDimensionBehaviors[HORIZONTAL] == FIXED
                        && constraintWidgetContainer.mListDimensionBehaviors[VERTICAL] == FIXED) {
                    float horizPercent = widget.mMatchConstraintPercentWidth;
                    float vertPercent = widget.mMatchConstraintPercentHeight;
                    int width = (int) (0.5f + horizPercent * constraintWidgetContainer.getWidth());
                    int height = (int) (0.5f + vertPercent * constraintWidgetContainer.getHeight());
                    measure(widget, FIXED, width, FIXED, height);
                    widget.horizontalRun.dimension.resolve(widget.getWidth());
                    widget.verticalRun.dimension.resolve(widget.getHeight());
                    widget.measured = true;
                }
            }
        }
        return false;
    }

    public void measureWidgets() {
        for (ConstraintWidget widget : container.mChildren) {
            if (widget.measured) {
                continue;
            }
            ConstraintWidget.DimensionBehaviour horiz = widget.mListDimensionBehaviors[HORIZONTAL];
            ConstraintWidget.DimensionBehaviour vert = widget.mListDimensionBehaviors[VERTICAL];
            int horizMatchConstraintsType = widget.mMatchConstraintDefaultWidth;
            int vertMatchConstraintsType = widget.mMatchConstraintDefaultHeight;

            boolean horizWrap = horiz == WRAP_CONTENT
                    || (horiz == MATCH_CONSTRAINT && horizMatchConstraintsType == MATCH_CONSTRAINT_WRAP);

            boolean vertWrap = vert == WRAP_CONTENT
                    || (vert == MATCH_CONSTRAINT && vertMatchConstraintsType == MATCH_CONSTRAINT_WRAP);

            boolean horizResolved = widget.horizontalRun.dimension.resolved;
            boolean vertResolved = widget.verticalRun.dimension.resolved;

            if (horizResolved && vertResolved) {
                measure(widget, FIXED, widget.horizontalRun.dimension.value,
                        FIXED, widget.verticalRun.dimension.value);
                widget.measured = true;
            } else if (horizResolved && vertWrap) {
                measure(widget, FIXED, widget.horizontalRun.dimension.value,
                        WRAP_CONTENT, widget.verticalRun.dimension.value);
                if (vert == MATCH_CONSTRAINT) {
                    widget.verticalRun.dimension.wrapValue = widget.getHeight();
                } else {
                    widget.verticalRun.dimension.resolve(widget.getHeight());
                    widget.measured = true;
                }
            } else if (vertResolved && horizWrap) {
                measure(widget, WRAP_CONTENT, widget.horizontalRun.dimension.value,
                        FIXED, widget.verticalRun.dimension.value);
                if (horiz == MATCH_CONSTRAINT) {
                    widget.horizontalRun.dimension.wrapValue = widget.getWidth();
                } else {
                    widget.horizontalRun.dimension.resolve(widget.getWidth());
                    widget.measured = true;
                }
            }
            if (widget.measured && widget.verticalRun.baselineDimension != null) {
                widget.verticalRun.baselineDimension.resolve(widget.getBaselineDistance());
            }
        }
    }

    /**
     * Invalidate the graph of constraints
     */
    public void invalidateGraph() {
        mNeedBuildGraph = true;
    }

    /**
     * Mark the widgets as needing to be remeasured
     */
    public void invalidateMeasures() {
        mNeedRedoMeasures = true;
    }

    ArrayList<RunGroup> mGroups = new ArrayList<>();

    public void buildGraph() {
        // First, let's identify the overall dependency graph
        buildGraph(mRuns);

        if (USE_GROUPS) {
            mGroups.clear();
            // Then get the horizontal and vertical groups
            RunGroup.index = 0;
            findGroup(container.horizontalRun, HORIZONTAL, mGroups);
            findGroup(container.verticalRun, VERTICAL, mGroups);
        }
        mNeedBuildGraph = false;
    }

    public void buildGraph(ArrayList<WidgetRun> runs) {
        runs.clear();
        mContainer.horizontalRun.clear();
        mContainer.verticalRun.clear();
        runs.add(mContainer.horizontalRun);
        runs.add(mContainer.verticalRun);
        HashSet<ChainRun> chainRuns = null;
        for (ConstraintWidget widget : mContainer.mChildren) {
            if (widget instanceof Guideline) {
                runs.add(new GuidelineReference(widget));
                continue;
            }
            if (widget.isInHorizontalChain()) {
                if (widget.horizontalChainRun == null) {
                    // build the horizontal chain
                    widget.horizontalChainRun = new ChainRun(widget, HORIZONTAL);
                }
                if (chainRuns == null) {
                    chainRuns = new HashSet<>();
                }
                chainRuns.add(widget.horizontalChainRun);
            } else {
                runs.add(widget.horizontalRun);
            }
            if (widget.isInVerticalChain()) {
                if (widget.verticalChainRun == null) {
                    // build the vertical chain
                    widget.verticalChainRun = new ChainRun(widget, VERTICAL);
                }
                if (chainRuns == null) {
                    chainRuns = new HashSet<>();
                }
                chainRuns.add(widget.verticalChainRun);
            } else {
                runs.add(widget.verticalRun);
            }
            if (widget instanceof HelperWidget) {
                runs.add(new HelperReferences(widget));
            }
        }
        if (chainRuns != null) {
            runs.addAll(chainRuns);
        }
        for (WidgetRun run : runs) {
            run.clear();
        }
        for (WidgetRun run : runs) {
            if (run.widget == mContainer) {
                continue;
            }
            run.apply();
        }

//        displayGraph();
    }



    private void displayGraph() {
        String content = "digraph {\n";
        for (WidgetRun run : mRuns) {
            content = generateDisplayGraph(run, content);
        }
        content += "\n}\n";
        System.out.println("content:<<\n" + content + "\n>>");
    }

    private void applyGroup(DependencyNode node, int orientation, int direction, DependencyNode end, ArrayList<RunGroup> groups, RunGroup group) {
        WidgetRun run = node.run;
        if (run.runGroup != null || run == container.horizontalRun || run == container.verticalRun) {
            return;
        }

        if (group == null) {
            group = new RunGroup(run, direction);
            groups.add(group);
        }

        run.runGroup = group;
        group.add(run);
        for (Dependency dependent : run.start.dependencies) {
            if (dependent instanceof DependencyNode) {
                applyGroup((DependencyNode) dependent, orientation, RunGroup.START, end, groups, group);
            }
        }
        for (Dependency dependent : run.end.dependencies) {
            if (dependent instanceof DependencyNode) {
                applyGroup((DependencyNode) dependent, orientation, RunGroup.END, end, groups, group);
            }
        }
        if (orientation == VERTICAL && run instanceof VerticalWidgetRun) {
            for (Dependency dependent : ((VerticalWidgetRun) run).baseline.dependencies) {
                if (dependent instanceof DependencyNode) {
                    applyGroup((DependencyNode) dependent, orientation, RunGroup.BASELINE, end, groups, group);
                }
            }
        }
        for (DependencyNode target : run.start.targets) {
            if (target == end) {
                group.dual = true;
            }
            applyGroup(target, orientation, RunGroup.START, end, groups, group);
        }
        for (DependencyNode target : run.end.targets) {
            if (target == end) {
                group.dual = true;
            }
            applyGroup(target, orientation, RunGroup.END, end, groups, group);
        }
        if (orientation == VERTICAL && run instanceof VerticalWidgetRun) {
            for (DependencyNode target : ((VerticalWidgetRun) run).baseline.targets) {
                applyGroup(target, orientation, RunGroup.BASELINE, end, groups, group);
            }
        }
    }

    private void findGroup(WidgetRun run, int orientation, ArrayList<RunGroup> groups) {
        for (Dependency dependent : run.start.dependencies) {
            if (dependent instanceof DependencyNode) {
                DependencyNode node = (DependencyNode) dependent;
                applyGroup(node, orientation, RunGroup.START, run.end, groups, null);
            } else if (dependent instanceof WidgetRun) {
                WidgetRun dependentRun = (WidgetRun) dependent;
                applyGroup(dependentRun.start, orientation, RunGroup.START, run.end, groups, null);
            }
        }
        for (Dependency dependent : run.end.dependencies) {
            if (dependent instanceof DependencyNode) {
                DependencyNode node = (DependencyNode) dependent;
                applyGroup(node, orientation, RunGroup.END, run.start, groups, null);
            } else if (dependent instanceof WidgetRun) {
                WidgetRun dependentRun = (WidgetRun) dependent;
                applyGroup(dependentRun.end, orientation, RunGroup.END, run.start, groups, null);
            }
        }
        if (orientation == VERTICAL) {
            for (Dependency dependent : ((VerticalWidgetRun) run).baseline.dependencies) {
                if (dependent instanceof DependencyNode) {
                    DependencyNode node = (DependencyNode) dependent;
                    applyGroup(node, orientation, RunGroup.BASELINE, null, groups, null);
                }
            }
        }
    }


    private String generateDisplayNode(DependencyNode node, boolean centeredConnection, String content) {
        StringBuilder contentBuilder = new StringBuilder(content);
        for (DependencyNode target : node.targets) {
            String constraint = "\n" + node.name();
            constraint += " -> " + target.name();
            if (node.margin > 0 || centeredConnection || node.run instanceof HelperReferences) {
                constraint += "[";
                if (node.margin > 0) {
                    constraint += "label=\"" + node.margin + "\"";
                    if (centeredConnection) {
                        constraint += ",";
                    }
                }
                if (centeredConnection) {
                    constraint += " style=dashed ";
                }
                if (node.run instanceof HelperReferences) {
                    constraint += " style=bold,color=gray ";
                }
                constraint += "]";
            }
            constraint += "\n";
            contentBuilder.append(constraint);
        }
        content = contentBuilder.toString();
//        for (DependencyNode dependency : node.dependencies) {
//            content = generateDisplayNode(dependency, content);
//        }
        return content;
    }

    private String nodeDefinition(WidgetRun run) {
        int orientation = run instanceof VerticalWidgetRun ? VERTICAL : HORIZONTAL;
        String name = run.widget.getDebugName();
        StringBuilder definition = new StringBuilder(name);
        ConstraintWidget.DimensionBehaviour behaviour = orientation == HORIZONTAL ? run.widget.getHorizontalDimensionBehaviour()
                : run.widget.getVerticalDimensionBehaviour();
        RunGroup runGroup = run.runGroup;

        if (orientation == HORIZONTAL) {
            definition.append("_HORIZONTAL");
        } else {
            definition.append("_VERTICAL");
        }
        definition.append(" [shape=none, label=<");
        definition.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\" CELLPADDING=\"2\">");
        definition.append("  <TR>");
        if (orientation == HORIZONTAL) {
            definition.append("    <TD ");
            if (run.start.resolved) {
                definition.append(" BGCOLOR=\"green\"");
            }
            definition.append(" PORT=\"LEFT\" BORDER=\"1\">L</TD>");
        } else {
            definition.append("    <TD ");
            if (run.start.resolved) {
                definition.append(" BGCOLOR=\"green\"");
            }
            definition.append(" PORT=\"TOP\" BORDER=\"1\">T</TD>");
        }
        definition.append("    <TD BORDER=\"1\" ");
        if (run.dimension.resolved && !run.widget.measured) {
            definition.append(" BGCOLOR=\"green\" ");
        } else if (run.dimension.resolved) {
            definition.append(" BGCOLOR=\"lightgray\" ");
        } else if (run.widget.measured) {
            definition.append(" BGCOLOR=\"yellow\" ");
        }
        if (behaviour == MATCH_CONSTRAINT) {
            definition.append("style=\"dashed\"");
        }
        definition.append(">");
        definition.append(name);
        if (runGroup != null) {
            definition.append(" [");
            definition.append(runGroup.groupIndex + 1);
            definition.append("/");
            definition.append(RunGroup.index);
            definition.append("]");
        }
        definition.append(" </TD>");
        if (orientation == HORIZONTAL) {
            definition.append("    <TD ");
            if (run.end.resolved) {
                definition.append(" BGCOLOR=\"green\"");
            }
            definition.append(" PORT=\"RIGHT\" BORDER=\"1\">R</TD>");
        } else {
            definition.append("    <TD ");
            if (((VerticalWidgetRun) run).baseline.resolved) {
                definition.append(" BGCOLOR=\"green\"");
            }
            definition.append(" PORT=\"BASELINE\" BORDER=\"1\">b</TD>");
            definition.append("    <TD ");
            if (run.end.resolved) {
                definition.append(" BGCOLOR=\"green\"");
            }
            definition.append(" PORT=\"BOTTOM\" BORDER=\"1\">B</TD>");
        }
        definition.append("  </TR></TABLE>");
        definition.append(">];\n");
        return definition.toString();
    }

    private String generateChainDisplayGraph(ChainRun chain, String content) {
        int orientation = chain.orientation;
        StringBuilder subgroup = new StringBuilder("subgraph ");
        subgroup.append("cluster_");
        subgroup.append(chain.widget.getDebugName());
        if (orientation == HORIZONTAL) {
            subgroup.append("_h");
        } else {
            subgroup.append("_v");
        }
        subgroup.append(" {\n");
        String definitions = "";
        for (WidgetRun run : chain.widgets) {
            subgroup.append(run.widget.getDebugName());
            if (orientation == HORIZONTAL) {
                subgroup.append("_HORIZONTAL");
            } else {
                subgroup.append("_VERTICAL");
            }
            subgroup.append(";\n");
            definitions = generateDisplayGraph(run, definitions);
        }
        subgroup.append("}\n");
        return content + definitions + subgroup;
    }

    private boolean isCenteredConnection(DependencyNode start, DependencyNode end) {
        int startTargets = 0;
        int endTargets = 0;
        for (DependencyNode s : start.targets) {
            if (s != end) {
                startTargets++;
            }
        }
        for (DependencyNode e : end.targets) {
            if (e != start) {
                endTargets++;
            }
        }
        return startTargets > 0 && endTargets > 0;
    }

    private String generateDisplayGraph(WidgetRun root, String content) {
        DependencyNode start = root.start;
        DependencyNode end = root.end;
        StringBuilder sb = new StringBuilder(content);

        if (!(root instanceof HelperReferences) && start.dependencies.isEmpty() && end.dependencies.isEmpty() & start.targets.isEmpty() && end.targets.isEmpty()) {
            return content;
        }
        sb.append(nodeDefinition(root));

        boolean centeredConnection = isCenteredConnection(start, end);
        content = generateDisplayNode(start, centeredConnection, content);
        content = generateDisplayNode(end, centeredConnection, content);
        if (root instanceof VerticalWidgetRun) {
            DependencyNode baseline = ((VerticalWidgetRun) root).baseline;
            content = generateDisplayNode(baseline, centeredConnection, content);
        }

        if (root instanceof HorizontalWidgetRun
                || (root instanceof ChainRun && ((ChainRun) root).orientation == HORIZONTAL)) {
            ConstraintWidget.DimensionBehaviour behaviour = root.widget.getHorizontalDimensionBehaviour();
            if (behaviour == ConstraintWidget.DimensionBehaviour.FIXED
                    || behaviour == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                if (!start.targets.isEmpty() && end.targets.isEmpty()) {
                    sb.append("\n");
                    sb.append(end.name());
                    sb.append(" -> ");
                    sb.append(start.name());
                    sb.append("\n");
                } else if (start.targets.isEmpty() && !end.targets.isEmpty()) {
                    sb.append("\n");
                    sb.append(start.name());
                    sb.append(" -> ");
                    sb.append(end.name());
                    sb.append("\n");
                }
            } else {
                if (behaviour == MATCH_CONSTRAINT && root.widget.getDimensionRatio() > 0) {
                    sb.append("\n");
                    sb.append(root.widget.getDebugName());
                    sb.append("_HORIZONTAL -> ");
                    sb.append(root.widget.getDebugName());
                    sb.append("_VERTICAL;\n");
                }
            }
        } else if (root instanceof VerticalWidgetRun
                || (root instanceof ChainRun && ((ChainRun) root).orientation == VERTICAL)) {
            ConstraintWidget.DimensionBehaviour behaviour = root.widget.getVerticalDimensionBehaviour();
            if (behaviour == ConstraintWidget.DimensionBehaviour.FIXED
                    || behaviour == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                if (!start.targets.isEmpty() && end.targets.isEmpty()) {
                    sb.append("\n");
                    sb.append(end.name());
                    sb.append(" -> ");
                    sb.append(start.name());
                    sb.append("\n");
                } else if (start.targets.isEmpty() && !end.targets.isEmpty()) {
                    sb.append("\n");
                    sb.append(start.name());
                    sb.append(" -> ");
                    sb.append(end.name());
                    sb.append("\n");
                }
            } else {
                if (behaviour == MATCH_CONSTRAINT && root.widget.getDimensionRatio() > 0) {
                    sb.append("\n");
                    sb.append(root.widget.getDebugName());
                    sb.append("_VERTICAL -> ");
                    sb.append(root.widget.getDebugName());
                    sb.append("_HORIZONTAL;\n");
                }
            }
        }
        if (root instanceof ChainRun) {
            return generateChainDisplayGraph((ChainRun) root, content);
        }
        return sb.toString();
    }

}
