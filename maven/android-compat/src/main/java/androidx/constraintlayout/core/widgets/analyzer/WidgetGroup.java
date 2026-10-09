/*
 * Copyright (C) 2020 The Android Open Source Project
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

import androidx.constraintlayout.core.LinearSystem;
import androidx.constraintlayout.core.widgets.Chain;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.BOTH;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;

/**
 * Represents a group of widget for the grouping mechanism.
 */
public class WidgetGroup {
    private static final boolean DEBUG = false;
    ArrayList<ConstraintWidget> widgets = new ArrayList<>();
    static int count = 0;
    int id = -1;
    boolean authoritative = false;
    int orientation = HORIZONTAL;
    ArrayList<MeasureResult> results = null;
    private int moveTo = -1;

    public WidgetGroup(int orientation) {
        id = count++;
        this.orientation = orientation;
    }

    public int getOrientation() { return orientation; }
    public int getId() { return id; }

    public boolean add(ConstraintWidget widget) {
        if (widgets.contains(widget)) {
            return false;
        }
        widgets.add(widget);
        return true;
    }

    public void setAuthoritative(boolean isAuthoritative) { authoritative = isAuthoritative; }
    public boolean isAuthoritative() { return authoritative; }

    private String getOrientationString() {
        if (orientation == HORIZONTAL) {
            return "Horizontal";
        } else if (orientation == VERTICAL) {
            return "Vertical";
        } else if (orientation == BOTH) {
            return "Both";
        }
        return "Unknown";
    }

    @Override
    public String toString() {
        String ret = getOrientationString() + " [" + id + "] <";
        for (ConstraintWidget widget : widgets) {
            ret += " " + widget.getDebugName();
        }
        ret += " >";
        return ret;
    }

    public void moveTo(int orientation, WidgetGroup widgetGroup) {
        if (DEBUG) {
            System.out.println("Move all widgets (" + this + ") from " + id + " to " + widgetGroup.getId() + "(" + widgetGroup + ")");
        }
        for (ConstraintWidget widget : widgets) {
            widgetGroup.add(widget);
            if (orientation == HORIZONTAL) {
                widget.horizontalGroup = widgetGroup.getId();
            } else {
                widget.verticalGroup = widgetGroup.getId();
            }
        }
        moveTo = widgetGroup.id;
    }

    public void clear() {
        widgets.clear();
    }

    private int measureWrap(int orientation, ConstraintWidget widget) {
        ConstraintWidget.DimensionBehaviour behaviour = widget.getDimensionBehaviour(orientation);
        if (behaviour == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
            || behaviour == ConstraintWidget.DimensionBehaviour.MATCH_PARENT
            || behaviour == ConstraintWidget.DimensionBehaviour.FIXED) {
            int dimension;
            if (orientation == HORIZONTAL) {
                dimension = widget.getWidth();
            } else {
                dimension = widget.getHeight();
            }
            return dimension;
        }
        return -1;
    }

    public int measureWrap(LinearSystem system, int orientation) {
        int count = widgets.size();
        if (count == 0) {
            return 0;
        }
        // TODO: add direct wrap computation for simpler cases instead of calling the solver
        return solverMeasure(system, widgets, orientation);
    }

    private int solverMeasure(LinearSystem system, ArrayList<ConstraintWidget> widgets, int orientation) {
        ConstraintWidgetContainer container = (ConstraintWidgetContainer) widgets.get(0).getParent();
        system.reset();
        boolean prevDebug = LinearSystem.FULL_DEBUG;
        container.addToSolver(system, false);
        for (int i = 0; i < widgets.size(); i++) {
            ConstraintWidget widget = widgets.get(i);
            widget.addToSolver(system, false);
        }
        if (orientation == HORIZONTAL) {
            if (container.mHorizontalChainsSize > 0) {
                Chain.applyChainConstraints(container, system, widgets, HORIZONTAL);
            }
        }
        if (orientation == VERTICAL) {
            if (container.mVerticalChainsSize > 0) {
                Chain.applyChainConstraints(container, system, widgets, VERTICAL);
            }
        }

        try {
            system.minimize();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // save results
        results = new ArrayList<>();
        for (int i = 0; i < widgets.size(); i++) {
            ConstraintWidget widget = widgets.get(i);
            MeasureResult result = new MeasureResult(widget, system, orientation);
            results.add(result);
        }

        if (orientation == HORIZONTAL) {
            int left = system.getObjectVariableValue(container.mLeft);
            int right = system.getObjectVariableValue(container.mRight);
            system.reset();
            return right - left;
        } else {
            int top = system.getObjectVariableValue(container.mTop);
            int bottom = system.getObjectVariableValue(container.mBottom);
            system.reset();
            return bottom - top;
        }
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public void apply() {
        if (results == null) {
            return;
        }
        if (!authoritative) {
            return;
        }
        for (int i = 0; i < results.size(); i++) {
            MeasureResult result = results.get(i);
            result.apply();
        }
    }

    public boolean intersectWith(WidgetGroup group) {
        for (int i = 0; i < widgets.size(); i++) {
            ConstraintWidget widget = widgets.get(i);
            if (group.contains(widget)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(ConstraintWidget widget) {
        return widgets.contains(widget);
    }

    public int size() {
        return widgets.size();
    }

    public void cleanup(ArrayList<WidgetGroup> dependencyLists) {
        final int count = widgets.size();
        if (moveTo != -1 && count > 0) {
            for (int i = 0; i < dependencyLists.size(); i++) {
                WidgetGroup group = dependencyLists.get(i);
                if (moveTo == group.id) {
                    moveTo(orientation, group);
                }
            }
        }
        if (count == 0) {
            dependencyLists.remove(this);
            return;
        }
    }


    class MeasureResult {
        WeakReference widgetRef;
        int left;
        int top;
        int right;
        int bottom;
        int baseline;
        int orientation;

        public MeasureResult(ConstraintWidget widget, LinearSystem system, int orientation) {
            widgetRef = new WeakReference(widget);
            left = system.getObjectVariableValue(widget.mLeft);
            top = system.getObjectVariableValue(widget.mTop);
            right = system.getObjectVariableValue(widget.mRight);
            bottom = system.getObjectVariableValue(widget.mBottom);
            baseline = system.getObjectVariableValue(widget.mBaseline);
            this.orientation = orientation;
        }

        public void apply() {
            Object ref = widgetRef.get();
            ConstraintWidget widget = ref instanceof ConstraintWidget ? (ConstraintWidget) ref : null;
            if (widget != null) {
                widget.setFinalFrame(left, top, right, bottom, baseline, orientation);
            }
        }
    }
}
