/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import java.util.ArrayList;

/// Rive `ArtboardComponentList`: one artboard instance per item of the view
/// model list bound to `listSource`, each bound to its item's view model
/// instance. The artboard of an item is the first artboard of the file whose
/// view model is the item's (upstream `ArtboardComponentList::findArtboard`;
/// the editor's per-view-model map rules are not read).
///
/// Approximation: upstream lays the rows out through the layout engine (and
/// virtualizes, scrolls and animates them); here they are stacked vertically
/// from the list's origin with no gap.
public class ArtboardComponentList extends ArtboardComponentListBase {
    private static final int MAX_DEPTH = 3;
    private static int depth;

    private final ArrayList<Artboard> rows = new ArrayList<Artboard>();
    private final ArrayList<ViewModelInstance> rowInstances = new ArrayList<ViewModelInstance>();
    private final ArrayList<StateMachineInstance> rowMachines = new ArrayList<StateMachineInstance>();
    private final Mat2D rowInverse = new Mat2D();
    private ViewModelInstanceList source;
    private int seenChange = -1;
    private final Mat2D rowTransform = new Mat2D();

    @Override
    public int onAddedClean(Artboard context) {
        int code = super.onAddedClean(context);
        if (context != null) {
            context.addComponentList(this);
        }
        return code;
    }

    /// The rows built for the list's items, in item order.
    public ArrayList<Artboard> getRows() {
        return rows;
    }

    /// Called by the list's data bind with the list it reads.
    public void setListSourceValue(ViewModelInstanceValue v) {
        ViewModelInstanceList list = v instanceof ViewModelInstanceList ? (ViewModelInstanceList) v : null;
        if (list == source && (list == null || list.getChangeCount() == seenChange)) { // NOPMD CompareObjectsWithEquals - identity is the intent
            return;
        }
        source = list;
        seenChange = list == null ? -1 : list.getChangeCount();
        rebuild();
    }

    private static int rowGeneration;

    /// Changes every time any list rebuilds its rows; lets a caller cache
    /// what it learned from the rows (see
    /// [StateMachineInstance#hasPointerListeners()]).
    static int getRowGeneration() {
        return rowGeneration;
    }

    private void rebuild() {
        rowGeneration++;
        Artboard host = getArtboard();
        RiveFileContext file = host == null ? null : host.getFile();
        ArrayList<ViewModelInstance> items = source == null ? null : source.getInstances();
        ArrayList<Artboard> old = new ArrayList<Artboard>(rows);
        ArrayList<ViewModelInstance> oldInstances = new ArrayList<ViewModelInstance>(rowInstances);
        ArrayList<StateMachineInstance> oldMachines = new ArrayList<StateMachineInstance>(rowMachines);
        rows.clear();
        rowInstances.clear();
        rowMachines.clear();
        if (file == null || items == null) {
            addDirt(DIRT_PAINT);
            return;
        }
        for (int i = 0; i < items.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            ViewModelInstance item = items.get(i);
            int reuse = oldInstances.indexOf(item);
            Artboard row;
            StateMachineInstance machine = null;
            if (reuse >= 0) {
                row = old.get(reuse);
                machine = oldMachines.get(reuse);
                oldInstances.set(reuse, null);
            } else {
                row = null;
                // A row whose artboard holds a list of rows of itself would
                // build forever; rows stop a few levels down.
                if (depth < MAX_DEPTH) {
                    depth++;
                    try {
                        int index = file.artboardIndexForViewModel(item.getViewModelId());
                        row = index < 0 ? null : file.instanceArtboard(index);
                        if (row != null) {
                            row.bindViewModelInstance(item);
                            row.advance(0);
                        }
                    } finally {
                        depth--;
                    }
                }
                if (row == null) {
                    continue;
                }
            }
            if (reuse < 0) {
                machine = createStateMachineInstance(row);
            }
            rows.add(row);
            rowInstances.add(item);
            rowMachines.add(machine);
        }
        addDirt(DIRT_PAINT);
    }

    /// Upstream `createStateMachineInstance`: every row runs its artboard's
    /// default state machine, or its first when it names none.
    private static StateMachineInstance createStateMachineInstance(Artboard row) {
        ArrayList<StateMachine> machines = row.getStateMachines();
        if (machines.isEmpty()) {
            return null;
        }
        int index = row.getDefaultStateMachineId();
        if (index < 0 || index >= machines.size()) {
            index = 0;
        }
        return new StateMachineInstance(machines.get(index), row);
    }

    /// The state machine of row `index`, null if it has none.
    public StateMachineInstance getStateMachineInstance(int index) {
        return index < 0 || index >= rowMachines.size() ? null : rowMachines.get(index);
    }

    /// Upstream `worldToLocal(world, local, index)`: maps the host world point
    /// `x`, `y` into row `index`'s world space into `out`; false when the
    /// transform cannot be inverted.
    boolean worldToLocal(float x, float y, int index, float[] out) {
        float offset = 0;
        for (int i = 0; i < index; i++) {
            offset += rows.get(i).getHeight();
        }
        rowTransform.set(1, 0, 0, 1, 0, offset);
        rowInverse.multiply(worldTransform, rowTransform);
        if (!rowTransform.invert(rowInverse)) {
            return false;
        }
        out[0] = rowTransform.mapX(x, y);
        out[1] = rowTransform.mapY(x, y);
        return true;
    }

    /// Advances every row and its state machine; answers whether one keeps
    /// going.
    public boolean advanceRows(float seconds) {
        if (source != null && source.getChangeCount() != seenChange) {
            seenChange = source.getChangeCount();
            rebuild();
        }
        boolean keepGoing = false;
        for (int i = 0; i < rows.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            StateMachineInstance m = rowMachines.get(i);
            if (m != null && m.advance(seconds)) {
                keepGoing = true;
            }
            if (rows.get(i).advance(seconds)) {
                keepGoing = true;
            }
        }
        return keepGoing;
    }

    @Override
    public void draw(RiveRenderer renderer) {
        if (isHidden() || rows.isEmpty()) {
            return;
        }
        float offset = 0;
        for (int i = 0; i < rows.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Artboard a = rows.get(i);
            renderer.save();
            renderer.transform(worldTransform);
            rowTransform.set(1, 0, 0, 1, 0, offset);
            offset += a.getHeight();
            renderer.transform(rowTransform);
            a.drawHosted(renderer);
            renderer.restore();
        }
    }
}
