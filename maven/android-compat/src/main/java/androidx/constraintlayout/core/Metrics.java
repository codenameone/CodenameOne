/*
 * Copyright (C) 2018 The Android Open Source Project
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
package androidx.constraintlayout.core;

import java.util.ArrayList;

/**
 * @hide
 *
 * Utility class to track metrics during the system resolution
 */
public class Metrics {
    public long measuresWidgetsDuration;
    public long measuresLayoutDuration;
    public long measuredWidgets;
    public long measuredMatchWidgets;
    public long measures;
    public long additionalMeasures;
    public long resolutions;
    public long tableSizeIncrease;
    public long minimize;
    public long constraints;
    public long simpleconstraints;
    public long optimize;
    public long iterations;
    public long pivots;
    public long bfs;
    public long variables;
    public long errors;
    public long slackvariables;
    public long extravariables;
    public long maxTableSize;
    public long fullySolved;
    public long graphOptimizer;
    public long graphSolved;
    public long linearSolved;
    public long resolvedWidgets;
    public long minimizeGoal;
    public long maxVariables;
    public long maxRows;
    public long centerConnectionResolved;
    public long matchConnectionResolved;
    public long chainConnectionResolved;
    public long barrierConnectionResolved;
    public long oldresolvedWidgets;
    public long nonresolvedWidgets;
    public ArrayList<String> problematicLayouts = new ArrayList<>();
    public long lastTableSize;
    public long widgets;
    public long measuresWrap;
    public long measuresWrapInfeasible;
    public long infeasibleDetermineGroups;
    public long determineGroups;
    public long layouts;
    public long grouping;

    public String toString() {
        return "\n*** Metrics ***\n"
                + "measures: " + measures + "\n"
                + "measuresWrap: " + measuresWrap + "\n"
                + "measuresWrapInfeasible: " + measuresWrapInfeasible + "\n"
                + "determineGroups: " + determineGroups + "\n"
                + "infeasibleDetermineGroups: " + infeasibleDetermineGroups + "\n"
                + "graphOptimizer: " + graphOptimizer + "\n"
                + "widgets: " + widgets + "\n"
                + "graphSolved: " + graphSolved + "\n"
                + "linearSolved: " + linearSolved + "\n"
/*
                + "measures: " + measures + "\n"
                + "additionalMeasures: " + additionalMeasures + "\n"
                + "resolutions passes: " + resolutions + "\n"
                + "table increases: " + tableSizeIncrease + "\n"
                + "maxTableSize: " + maxTableSize + "\n"
                + "maxVariables: " + maxVariables + "\n"
                + "maxRows: " + maxRows + "\n\n"
                + "minimize: " + minimize + "\n"
                + "minimizeGoal: " + minimizeGoal + "\n"
                + "constraints: " + constraints + "\n"
                + "simpleconstraints: " + simpleconstraints + "\n"
                + "optimize: " + optimize + "\n"
                + "iterations: " + iterations + "\n"
                + "pivots: " + pivots + "\n"
                + "bfs: " + bfs + "\n"
                + "variables: " + variables + "\n"
                + "errors: " + errors + "\n"
                + "slackvariables: " + slackvariables + "\n"
                + "extravariables: " + extravariables + "\n"
                + "fullySolved: " + fullySolved + "\n"
                + "graphOptimizer: " + graphOptimizer + "\n"
                + "resolvedWidgets: " + resolvedWidgets + "\n"
                + "oldresolvedWidgets: " + oldresolvedWidgets + "\n"
                + "nonresolvedWidgets: " + nonresolvedWidgets + "\n"
                + "centerConnectionResolved: " + centerConnectionResolved + "\n"
                + "matchConnectionResolved: " + matchConnectionResolved + "\n"
                + "chainConnectionResolved: " + chainConnectionResolved + "\n"
                + "barrierConnectionResolved: " + barrierConnectionResolved + "\n"
                + "problematicsLayouts: " + problematicLayouts + "\n"
                */
                ;
    }
    public void reset() {
        measures = 0;
        widgets = 0;
        additionalMeasures = 0;
        resolutions = 0;
        tableSizeIncrease = 0;
        maxTableSize = 0;
        lastTableSize = 0;
        maxVariables = 0;
        maxRows = 0;
        minimize = 0;
        minimizeGoal = 0;
        constraints = 0;
        simpleconstraints = 0;
        optimize = 0;
        iterations = 0;
        pivots = 0;
        bfs = 0;
        variables = 0;
        errors = 0;
        slackvariables = 0;
        extravariables = 0;
        fullySolved = 0;
        graphOptimizer = 0;
        graphSolved = 0;
        resolvedWidgets = 0;
        oldresolvedWidgets = 0;
        nonresolvedWidgets = 0;
        centerConnectionResolved = 0;
        matchConnectionResolved = 0;
        chainConnectionResolved = 0;
        barrierConnectionResolved = 0;
        problematicLayouts.clear();
    }
}
