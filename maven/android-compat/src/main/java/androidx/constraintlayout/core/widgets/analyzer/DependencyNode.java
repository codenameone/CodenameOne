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

import java.util.ArrayList;
import java.util.List;

public class DependencyNode implements Dependency {
    public Dependency updateDelegate = null;
    public boolean delegateToWidgetRun = false;
    public boolean readyToSolve = false;

    enum Type { UNKNOWN, HORIZONTAL_DIMENSION, VERTICAL_DIMENSION, LEFT, RIGHT, TOP, BOTTOM, BASELINE }

    WidgetRun run;
    Type type = Type.UNKNOWN;
    int margin;
    public int value;
    int marginFactor = 1;
    DimensionDependency marginDependency = null;
    public boolean resolved = false;

    public DependencyNode(WidgetRun run) {
        this.run = run;
    }
    List<Dependency> dependencies = new ArrayList<>();
    List<DependencyNode> targets = new ArrayList<>();

    @Override
    public String toString() {
        return run.widget.getDebugName() + ":" + type + "("
            + (resolved? value : "unresolved") + ") <t=" + targets.size() + ":d=" + dependencies.size() + ">";
    }

    public void resolve(int value) {
        if (resolved) {
            return;
        }

        this.resolved = true;
        this.value = value;
        for (Dependency node : dependencies) {
            node.update(node);
         }
    }

    public void update(Dependency node) {
        for (DependencyNode target : targets) {
            if (!target.resolved) {
                return;
            }
        }
        readyToSolve = true;
        if (updateDelegate != null) {
            updateDelegate.update(this);
        }
        if (delegateToWidgetRun) {
            run.update(this);
            return;
        }
        DependencyNode target = null;
        int numTargets = 0;
        for (DependencyNode t : targets) {
            if (t instanceof DimensionDependency) {
                continue;
            }
            target = t;
            numTargets++;
        }
        if (target != null && numTargets == 1 && target.resolved) {
            if (marginDependency != null) {
                if (marginDependency.resolved) {
                    margin = marginFactor * marginDependency.value;
                } else {
                    return;
                }
            }
            resolve(target.value + margin);
        }
        if (updateDelegate != null) {
            updateDelegate.update(this);
        }
    }

    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
        if (resolved) {
            dependency.update(dependency);
        }
    }

    public String name() {
        String definition = run.widget.getDebugName();
        if (type == Type.LEFT
                || type == Type.RIGHT) {
            definition += "_HORIZONTAL";
        } else {
            definition += "_VERTICAL";
        }
        definition += ":" + type.name();
        return definition;
    }

    public void clear() {
        targets.clear();
        dependencies.clear();
        resolved = false;
        value = 0;
        readyToSolve = false;
        delegateToWidgetRun = false;
    }
}
