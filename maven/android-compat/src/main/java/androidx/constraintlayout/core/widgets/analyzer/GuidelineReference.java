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

import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.Guideline;

class GuidelineReference extends WidgetRun {

    public GuidelineReference(ConstraintWidget widget) {
        super(widget);
        widget.horizontalRun.clear();
        widget.verticalRun.clear();
        this.orientation = ((Guideline) widget).getOrientation();
    }

    @Override
    void clear() {
        start.clear();
    }

    @Override
    void reset() {
        start.resolved = false;
        end.resolved = false;
    }

    @Override
    boolean supportsWrapComputation() {
        return false;
    }

    private void addDependency(androidx.constraintlayout.core.widgets.analyzer.DependencyNode node) {
        start.dependencies.add(node);
        node.targets.add(start);
    }

    @Override
    public void update(Dependency dependency) {
        if (!start.readyToSolve) {
            return;
        }
        if (start.resolved) {
            return;
        }
        // ready to solve, centering.
        androidx.constraintlayout.core.widgets.analyzer.DependencyNode startTarget = start.targets.get(0);
        Guideline guideline = (Guideline) widget;
        int startPos = (int) (0.5f + startTarget.value * guideline.getRelativePercent());
        start.resolve(startPos);
    }

    @Override
    void apply() {
        Guideline guideline = (Guideline) widget;
        int relativeBegin = guideline.getRelativeBegin();
        int relativeEnd = guideline.getRelativeEnd();
        float percent = guideline.getRelativePercent();
        if (guideline.getOrientation() == ConstraintWidget.VERTICAL) {
            if (relativeBegin != -1) {
                start.targets.add(widget.mParent.horizontalRun.start);
                widget.mParent.horizontalRun.start.dependencies.add(start);
                start.margin = relativeBegin;
            } else if (relativeEnd != -1) {
                start.targets.add(widget.mParent.horizontalRun.end);
                widget.mParent.horizontalRun.end.dependencies.add(start);
                start.margin = -relativeEnd;
            } else {
                start.delegateToWidgetRun = true;
                start.targets.add(widget.mParent.horizontalRun.end);
                widget.mParent.horizontalRun.end.dependencies.add(start);
            }
            // FIXME -- if we move the DependencyNode directly in the ConstraintAnchor we'll be good.
            addDependency(widget.horizontalRun.start);
            addDependency(widget.horizontalRun.end);
        } else {
            if (relativeBegin != -1) {
                start.targets.add(widget.mParent.verticalRun.start);
                widget.mParent.verticalRun.start.dependencies.add(start);
                start.margin = relativeBegin;
            } else if (relativeEnd != -1) {
                start.targets.add(widget.mParent.verticalRun.end);
                widget.mParent.verticalRun.end.dependencies.add(start);
                start.margin = -relativeEnd;
            } else {
                start.delegateToWidgetRun = true;
                start.targets.add(widget.mParent.verticalRun.end);
                widget.mParent.verticalRun.end.dependencies.add(start);
            }
            // FIXME -- if we move the DependencyNode directly in the ConstraintAnchor we'll be good.
            addDependency(widget.verticalRun.start);
            addDependency(widget.verticalRun.end);
        }
    }

    @Override
    public void applyToWidget() {
        Guideline guideline = (Guideline) widget;
        if (guideline.getOrientation() == ConstraintWidget.VERTICAL) {
            widget.setX(start.value);
        } else {
            widget.setY(start.value);
        }
    }
}
