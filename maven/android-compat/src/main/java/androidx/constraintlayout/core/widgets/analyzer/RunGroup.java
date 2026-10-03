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

import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;

import java.util.ArrayList;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;

class RunGroup {
    public final static int START = 0;
    public final static int END = 1;
    public final static int BASELINE = 2;

    public static int index;

    public int position = 0;
    public boolean dual = false;

    WidgetRun firstRun = null;
    WidgetRun lastRun = null;
    ArrayList<WidgetRun> runs = new ArrayList<>();

    int groupIndex = 0;
    int direction;

    public RunGroup(WidgetRun run, int dir) {
        groupIndex = index;
        index++;
        firstRun = run;
        lastRun = run;
        direction = dir;
    }

    public void add(WidgetRun run) {
        runs.add(run);
        lastRun = run;
    }

    private long traverseStart(DependencyNode node, long startPosition) {
        WidgetRun run = node.run;
        if (run instanceof HelperReferences) {
            return startPosition;
        }
        long position = startPosition;

        // first, compute stuff dependent on this node.

        final int count = node.dependencies.size();
        for (int i = 0; i < count; i++) {
                Dependency dependency = node.dependencies.get(i);
                if (dependency instanceof DependencyNode) {
                    DependencyNode nextNode = (DependencyNode) dependency;
                    if (nextNode.run == run) {
                        // skip our own sibling node
                        continue;
                    }
                    position = Math.max(position, traverseStart(nextNode, startPosition + nextNode.margin));
                }
        }

        if (node == run.start) {
            // let's go for our sibling
            long dimension = run.getWrapDimension();
            position = Math.max(position, traverseStart(run.end, startPosition + dimension));
            position = Math.max(position, startPosition + dimension - run.end.margin);
        }

        return position;
    }

    private long traverseEnd(DependencyNode node, long startPosition) {
        WidgetRun run = node.run;
        if (run instanceof HelperReferences) {
            return startPosition;
        }
        long position = startPosition;

        // first, compute stuff dependent on this node.

        final int count = node.dependencies.size();
        for (int i = 0; i < count; i++) {
            Dependency dependency = node.dependencies.get(i);
            if (dependency instanceof DependencyNode) {
                DependencyNode nextNode = (DependencyNode) dependency;
                if (nextNode.run == run) {
                    // skip our own sibling node
                    continue;
                }
                position = Math.min(position, traverseEnd(nextNode, startPosition + nextNode.margin));
            }
        }

        if (node == run.end) {
            // let's go for our sibling
            long dimension = run.getWrapDimension();
            position = Math.min(position, traverseEnd(run.start, startPosition - dimension));
            position = Math.min(position, startPosition - dimension - run.start.margin);
        }

        return position;
    }

    public long computeWrapSize(ConstraintWidgetContainer container, int orientation) {
        if (firstRun instanceof ChainRun) {
            ChainRun chainRun = (ChainRun) firstRun;
            if (chainRun.orientation != orientation) {
                return 0;
            }
        } else {
            if (orientation == HORIZONTAL) {
                if (!(firstRun instanceof HorizontalWidgetRun)) {
                    return 0;
                }
            } else {
                if (!(firstRun instanceof VerticalWidgetRun)) {
                    return 0;
                }
            }
        }
        DependencyNode containerStart = orientation == HORIZONTAL ? container.horizontalRun.start : container.verticalRun.start;
        DependencyNode containerEnd = orientation == HORIZONTAL ? container.horizontalRun.end : container.verticalRun.end;

        boolean runWithStartTarget = firstRun.start.targets.contains(containerStart);
        boolean runWithEndTarget = firstRun.end.targets.contains(containerEnd);

        long dimension = firstRun.getWrapDimension();

        if (runWithStartTarget && runWithEndTarget) {
            long maxPosition = traverseStart(firstRun.start, 0);
            long minPosition = traverseEnd(firstRun.end, 0);

            // to compute the gaps, we subtract the margins
            long endGap = maxPosition - dimension;
            if (endGap >= -firstRun.end.margin) {
                endGap += firstRun.end.margin;
            }
            long startGap = -minPosition - dimension - firstRun.start.margin;
            if (startGap >= firstRun.start.margin) {
                startGap -= firstRun.start.margin;
            }
            float bias = firstRun.widget.getBiasPercent(orientation);
            long gap = 0;
            if (bias > 0) {
                gap = (long) ((startGap / bias) + (endGap / (1f - bias)));
            }

            startGap = (long) (0.5f + (gap * bias));
            endGap = (long) (0.5f + (gap * (1f - bias)));

            long runDimension = startGap + dimension + endGap;
            dimension = firstRun.start.margin + runDimension - firstRun.end.margin;

        } else if (runWithStartTarget) {
            long maxPosition = traverseStart(firstRun.start, firstRun.start.margin);
            long runDimension = firstRun.start.margin + dimension;
            dimension = Math.max(maxPosition, runDimension);
        } else if (runWithEndTarget) {
            long minPosition = traverseEnd(firstRun.end, firstRun.end.margin);
            long runDimension = -firstRun.end.margin + dimension;
            dimension = Math.max(-minPosition, runDimension);
        } else {
            dimension = firstRun.start.margin + firstRun.getWrapDimension() - firstRun.end.margin;
        }

        return dimension;
    }

    private boolean defineTerminalWidget(WidgetRun run, int orientation) {
        if (!run.widget.isTerminalWidget[orientation]) {
            return false;
        }
        for (Dependency dependency : run.start.dependencies) {
            if (dependency instanceof DependencyNode) {
                DependencyNode node = (DependencyNode) dependency;
                if (node.run == run) {
                    continue;
                }
                if (node == node.run.start) {
                    if (run instanceof ChainRun) {
                        ChainRun chainRun = (ChainRun) run;
                        for (WidgetRun widgetChainRun : chainRun.widgets) {
                            defineTerminalWidget(widgetChainRun, orientation);
                        }
                    } else {
                        if (!(run instanceof HelperReferences)) {
                            run.widget.isTerminalWidget[orientation] = false;
                        }
                    }
                    defineTerminalWidget(node.run, orientation);
                }
            }
        }
        for (Dependency dependency : run.end.dependencies) {
            if (dependency instanceof DependencyNode) {
                DependencyNode node = (DependencyNode) dependency;
                if (node.run == run) {
                    continue;
                }
                if (node == node.run.start){
                    if (run instanceof ChainRun) {
                        ChainRun chainRun = (ChainRun) run;
                        for (WidgetRun widgetChainRun : chainRun.widgets) {
                            defineTerminalWidget(widgetChainRun, orientation);
                        }
                    } else {
                        if (!(run instanceof HelperReferences)) {
                           run.widget.isTerminalWidget[orientation] = false;
                        }
                    }
                    defineTerminalWidget(node.run, orientation);
                }
            }
        }
        return false;
    }


    public void defineTerminalWidgets(boolean horizontalCheck, boolean verticalCheck) {
        if (horizontalCheck && firstRun instanceof HorizontalWidgetRun) {
            defineTerminalWidget(firstRun, HORIZONTAL);
        }
        if (verticalCheck && firstRun instanceof VerticalWidgetRun) {
            defineTerminalWidget(firstRun, VERTICAL);
        }
    }
}
