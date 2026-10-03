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

package androidx.constraintlayout.core;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Implements a row containing goals taking in account priorities.
 */
public class PriorityGoalRow extends ArrayRow {
    private static final float epsilon = 0.0001f;
    private static final boolean DEBUG = false;

    private int TABLE_SIZE = 128;
    private SolverVariable[] arrayGoals = new SolverVariable[TABLE_SIZE];
    private SolverVariable[] sortArray = new SolverVariable[TABLE_SIZE];
    private int numGoals = 0;
    GoalVariableAccessor accessor = new GoalVariableAccessor(this);

    class GoalVariableAccessor {
        SolverVariable variable;
        PriorityGoalRow row;

        public GoalVariableAccessor(PriorityGoalRow row) {
            this.row = row;
        }

        public void init(SolverVariable variable) {
            this.variable = variable;
        }

        public boolean addToGoal(SolverVariable other, float value) {
            if (variable.inGoal) {
                boolean empty = true;
                for (int i = 0; i < SolverVariable.MAX_STRENGTH; i++) {
                    variable.goalStrengthVector[i] += other.goalStrengthVector[i] * value;
                    float v = variable.goalStrengthVector[i];
                    if (Math.abs(v) < epsilon) {
                        variable.goalStrengthVector[i] = 0;
                    } else {
                        empty = false;
                    }
                }
                if (empty) {
                    removeGoal(variable);
                }
            } else {
                for (int i = 0; i < SolverVariable.MAX_STRENGTH; i++) {
                    float strength = other.goalStrengthVector[i];
                    if (strength != 0) {
                        float v = value * strength;
                        if (Math.abs(v) < epsilon) {
                            v = 0;
                        }
                        variable.goalStrengthVector[i] = v;
                    } else {
                        variable.goalStrengthVector[i] = 0;
                    }
                }
                return true;
            }
            return false;
        }

        public void add(SolverVariable other) {
            for (int i = 0; i < SolverVariable.MAX_STRENGTH; i++) {
                variable.goalStrengthVector[i] += other.goalStrengthVector[i];
                float value = variable.goalStrengthVector[i];
                if (Math.abs(value) < epsilon) {
                    variable.goalStrengthVector[i] = 0;
                }
            }
        }

        public final boolean isNegative() {
            for (int i = SolverVariable.MAX_STRENGTH - 1; i >= 0; i--) {
                float value = variable.goalStrengthVector[i];
                if (value > 0) {
                    return false;
                }
                if (value < 0) {
                    return true;
                }
            }
            return false;
        }

        public final boolean isSmallerThan(SolverVariable other) {
            for (int i = SolverVariable.MAX_STRENGTH - 1; i >= 0 ; i--) {
                float comparedValue = other.goalStrengthVector[i];
                float value = variable.goalStrengthVector[i];
                if (value == comparedValue) {
                    continue;
                }
                if (value < comparedValue) {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }

        public final boolean isNull() {
            for (int i = 0; i < SolverVariable.MAX_STRENGTH; i++) {
                if (variable.goalStrengthVector[i] != 0) {
                    return false;
                }
            }
            return true;
        }

        public void reset() {
            Arrays.fill(variable.goalStrengthVector, 0);
        }

        public String toString() {
            String result = "[ ";
            if (variable != null) {
                for (int i = 0; i < SolverVariable.MAX_STRENGTH; i++) {
                    result += variable.goalStrengthVector[i] + " ";
                }
            }
            result += "] " + variable;
            return result;
        }

    }

    @Override
    public void clear() {
        numGoals = 0;
        constantValue = 0;
    }

    Cache mCache;

    public PriorityGoalRow(Cache cache) {
        super(cache);
        mCache = cache;
    }

    @Override
    public boolean isEmpty() {
        return numGoals == 0;
    }

    final static int NOT_FOUND = -1;

    @Override
    public SolverVariable getPivotCandidate(LinearSystem system, boolean[] avoid) {
        int pivot = NOT_FOUND;
        for (int i = 0; i < numGoals; i++) {
            SolverVariable variable = arrayGoals[i];
            if (avoid[variable.id]) {
                continue;
            }
            accessor.init(variable);
            if (pivot == NOT_FOUND) {
                if (accessor.isNegative())  {
                    pivot = i;
                }
            } else if (accessor.isSmallerThan(arrayGoals[pivot])) {
                pivot = i;
            }
        }
        if (pivot == NOT_FOUND) {
            return null;
        }
        return arrayGoals[pivot];
    }

    @Override
    public void addError(SolverVariable error) {
        accessor.init(error);
        accessor.reset();
        error.goalStrengthVector[error.strength] = 1;
        addToGoal(error);
    }

    private final void addToGoal(SolverVariable variable) {
        if (numGoals + 1> arrayGoals.length) {
            arrayGoals = copyVariables(arrayGoals, arrayGoals.length * 2);
            sortArray = copyVariables(arrayGoals, arrayGoals.length * 2);
        }
        arrayGoals[numGoals] = variable;
        numGoals++;

        if (numGoals > 1 && arrayGoals[numGoals - 1].id > variable.id) {
            for (int i = 0; i < numGoals; i++) {
                sortArray[i] = arrayGoals[i];
            }
            Arrays.sort(sortArray, 0, numGoals, new Comparator<SolverVariable>() {
                @Override
                public int compare(SolverVariable variable1, SolverVariable variable2) {
                    return variable1.id - variable2.id;
                }
            });
            for (int i = 0; i < numGoals; i++) {
                arrayGoals[i] = sortArray[i];
            }
        }

        variable.inGoal = true;
        variable.addToRow(this);
    }

    private final void removeGoal(SolverVariable variable) {
        for (int i = 0; i < numGoals; i++) {
            if (arrayGoals[i] == variable) {
                for (int j = i; j < numGoals - 1; j++) {
                    arrayGoals[j] = arrayGoals[j + 1];
                }
                numGoals--;
                variable.inGoal = false;
                return;
            }
        }
    }

    @Override
    public void updateFromRow(LinearSystem system, ArrayRow definition, boolean removeFromDefinition) {
        SolverVariable goalVariable = definition.variable;
        if (goalVariable == null) {
            return;
        }

        ArrayRowVariables rowVariables = definition.variables;
        int currentSize = rowVariables.getCurrentSize();
        for (int i = 0; i < currentSize; i++) {
            SolverVariable solverVariable = rowVariables.getVariable(i);
            float value = rowVariables.getVariableValue(i);
            accessor.init(solverVariable);
            if (accessor.addToGoal(goalVariable, value)) {
                addToGoal(solverVariable);
            }
            constantValue += definition.constantValue * value;
        }
        removeGoal(goalVariable);
    }

    @Override
    public String toString() {
        String result = "";
        result += " goal -> (" + constantValue + ") : ";
        for (int i = 0; i < numGoals; i++) {
            SolverVariable v = arrayGoals[i];
            accessor.init(v);
            result += accessor + " ";
        }
        return result;
    }

    // Codename One: an explicit copy instead of the generic Arrays.copyOf,
    // which on the iOS VM goes through reflective array creation.
    static SolverVariable[] copyVariables(SolverVariable[] a, int length) {
        SolverVariable[] c = new SolverVariable[length];
        System.arraycopy(a, 0, c, 0, Math.min(a.length, length));
        return c;
    }
}
