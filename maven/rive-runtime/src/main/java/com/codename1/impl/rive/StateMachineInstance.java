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
import java.util.Random;

/// A playing instance of a [StateMachine] on one artboard: input values,
/// one current (and mixing-from) state per layer, transitions, listeners
/// and the events reported by the latest advance.
///
/// Port of upstream's StateMachineInstance and StateMachineLayerInstance,
/// without the settle/freeze optimizations and without the animation reset
/// (a transition mixes from the previous state's pose as the older runtime
/// did). Data-binding conditions and listener actions are inert: such a
/// condition never passes.
public class StateMachineInstance {
    static final int INPUT_OTHER = 0;
    static final int INPUT_BOOL = 1;
    static final int INPUT_NUMBER = 2;
    static final int INPUT_TRIGGER = 3;

    static final int FLAG_DISABLED = 1;
    static final int FLAG_DURATION_PERCENT = 1 << 1;
    static final int FLAG_EXIT_TIME = 1 << 2;
    static final int FLAG_EXIT_PERCENT = 1 << 3;
    static final int FLAG_PAUSE_ON_EXIT = 1 << 4;
    static final int FLAG_EARLY_EXIT = 1 << 5;
    static final int STATE_RANDOM = 1;

    static final int LISTENER_ENTER = 0;
    static final int LISTENER_EXIT = 1;
    static final int LISTENER_DOWN = 2;
    static final int LISTENER_UP = 3;
    static final int LISTENER_MOVE = 4;
    static final int LISTENER_EVENT = 5;
    static final int LISTENER_CLICK = 6;
    static final int LISTENER_VIEW_MODEL = 11;

    private static final int EVENT_TRIGGER_KEY = 395;
    private static final int MAX_ITERATIONS = 100;

    /// An event reported by an advance or a pointer event.
    public static final class ReportedEvent {
        public final Event event;
        public final float delaySeconds;

        ReportedEvent(Event event, float delaySeconds) {
            this.event = event;
            this.delaySeconds = delaySeconds;
        }
    }

    private final StateMachine machine;
    private final Artboard artboard;
    private final ArrayList<Core> inputs;
    private final int[] inputType;
    private final boolean[] boolValue;
    private final float[] numberValue;
    private final boolean[] fired;
    private final boolean[][] usedInLayer;
    private final Layer[] layers;
    private final boolean[] hovered;
    private final boolean[] downHit;
    private final ArrayList<ReportedEvent> reported = new ArrayList<ReportedEvent>();
    // fixed seed, as DataConverterFormula does: a weighted random transition
    // must pick the same way on every run so captures and tests are repeatable
    private final Random random = new Random(0x5249564CL);
    private final Mat2D inverse = new Mat2D();
    private boolean changed;
    private int eventDepth;
    /// The state machines of the host artboard this machine's artboard is
    /// nested in, which hear its events (upstream `nestedEventListeners`).
    private final ArrayList<StateMachineInstance> eventListeners = new ArrayList<StateMachineInstance>();
    private NestedArtboard eventSource;
    private final float[] nestedPoint = new float[2];

    public StateMachineInstance(StateMachine machine, Artboard artboard) {
        this.machine = machine;
        this.artboard = artboard;
        inputs = machine.getInputs();
        int n = inputs.size();
        inputType = new int[n];
        boolValue = new boolean[n];
        numberValue = new float[n];
        fired = new boolean[n];
        ArrayList<StateMachine.LayerDef> defs = machine.getLayerDefs();
        usedInLayer = new boolean[n][defs.size()];
        for (int i = 0; i < n; i++) {
            Core c = inputs.get(i);
            if (c instanceof StateMachineBool) {
                inputType[i] = INPUT_BOOL;
                boolValue[i] = ((StateMachineBool) c).isValue();
            } else if (c instanceof StateMachineNumber) {
                inputType[i] = INPUT_NUMBER;
                numberValue[i] = ((StateMachineNumber) c).getValue();
            } else if (c instanceof StateMachineTrigger) {
                inputType[i] = INPUT_TRIGGER;
            }
        }
        layers = new Layer[defs.size()];
        for (int i = 0; i < layers.length; i++) {
            layers[i] = new Layer(defs.get(i), i);
        }
        for (Layer l : layers) {
            l.changeState(l.def.entry);
        }
        int lc = machine.getListenerDefs().size();
        hovered = new boolean[lc];
        downHit = new boolean[lc];
        // Upstream constructor: every nested state machine reports its
        // events to this one, with its nested artboard as the source.
        ArrayList<NestedArtboard> nested = artboard.getNestedArtboards();
        for (int i = 0; i < nested.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            NestedArtboard na = nested.get(i);
            ArrayList<NestedAnimation> anims = na.getNestedAnimations();
            for (int j = 0; j < anims.size(); j++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
                if (anims.get(j) instanceof NestedStateMachine) {
                    StateMachineInstance smi = ((NestedStateMachine) anims.get(j)).getStateMachineInstance();
                    if (smi != null) {
                        smi.eventListeners.add(this);
                        smi.eventSource = na;
                    }
                }
            }
        }
    }

    public StateMachine getStateMachine() {
        return machine;
    }

    // ---------------------------------------------------------------- inputs

    private int inputIndex(String name, int type) {
        for (int i = 0; i < inputs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            if (inputType[i] == type && name.equals(((StateMachineComponent) inputs.get(i)).getName())) {
                return i;
            }
        }
        return -1;
    }

    /// Sets the boolean input `name`; false when there is none.
    public boolean setBoolean(String name, boolean value) {
        int i = inputIndex(name, INPUT_BOOL);
        if (i < 0) {
            return false;
        }
        setBool(i, value);
        return true;
    }

    /// Sets the number input `name`; false when there is none.
    public boolean setNumber(String name, float value) {
        int i = inputIndex(name, INPUT_NUMBER);
        if (i < 0) {
            return false;
        }
        setNum(i, value);
        return true;
    }

    /// Fires the trigger input `name`; false when there is none.
    public boolean fireTrigger(String name) {
        int i = inputIndex(name, INPUT_TRIGGER);
        if (i < 0) {
            return false;
        }
        fire(i);
        return true;
    }

    /// The boolean input `name`, false when there is none.
    public boolean getBoolean(String name) {
        int i = inputIndex(name, INPUT_BOOL);
        return i >= 0 && boolValue[i];
    }

    /// The number input `name`, 0 when there is none.
    public float getNumber(String name) {
        int i = inputIndex(name, INPUT_NUMBER);
        return i < 0 ? 0 : numberValue[i];
    }

    /// Whether the trigger `name` fired since the latest advance.
    public boolean didFire(String name) {
        int i = inputIndex(name, INPUT_TRIGGER);
        return i >= 0 && fired[i];
    }

    /// The number of inputs.
    public int getInputCount() {
        return inputs.size();
    }

    /// The input at `index` (a StateMachineBool, Number or Trigger).
    public Core getInput(int index) {
        return inputs.get(index);
    }

    /// Sets input `index` by its kind: a bool to `value != 0`, a number to
    /// `value`, a trigger fires.
    public void pokeInput(int index, float value) {
        switch (inputType[index]) {
            case INPUT_BOOL:
                setBool(index, value != 0);
                break;
            case INPUT_NUMBER:
                setNum(index, value);
                break;
            case INPUT_TRIGGER:
                fire(index);
                break;
            default:
                break;
        }
    }

    private void setBool(int i, boolean v) {
        if (boolValue[i] != v) {
            boolValue[i] = v;
            changed = true;
        }
    }

    private void setNum(int i, float v) {
        if (Float.floatToIntBits(numberValue[i]) != Float.floatToIntBits(v)) {
            numberValue[i] = v;
            changed = true;
        }
    }

    private void fire(int i) {
        fired[i] = true;
        changed = true;
    }

    private int validInput(int id, int type) {
        return id >= 0 && id < inputType.length && inputType[id] == type ? id : -1;
    }

    // --------------------------------------------------------------- advance

    /// Whether an input changed or a listener ran since the latest advance.
    public boolean needsAdvance() {
        return changed;
    }

    /// Advances every layer by `seconds` and applies the result to the
    /// artboard (the caller advances the artboard after). Returns whether the
    /// machine keeps going: a layer is mixing, waiting for an exit time or
    /// playing an animation that has not finished.
    public boolean advance(float seconds) {
        reported.clear();
        artboard.updateDataBinds();
        checkViewModelListeners();
        boolean keepGoing = false;
        for (Layer l : layers) {
            if (l.advance(seconds)) {
                keepGoing = true;
            }
        }
        for (int i = 0; i < fired.length; i++) {
            fired[i] = false;
            boolean[] used = usedInLayer[i];
            for (int l = 0; l < used.length; l++) {
                used[l] = false;
            }
        }
        changed = false;
        return keepGoing;
    }

    /// The events reported since the latest advance started.
    public ArrayList<ReportedEvent> getReportedEvents() {
        return reported;
    }

    /// The state the layer at `index` is in, null if none.
    public LayerState getLayerState(int index) {
        Layer l = layers[index];
        return l.current == null ? null : l.current.def.state;
    }

    /// The number of layers.
    public int getLayerCount() {
        return layers.length;
    }

    // ---------------------------------------------------------------- events

    void reportEvent(Event event, float delay) {
        reported.add(new ReportedEvent(event, delay));
        if (event instanceof AudioEvent) {
            ((AudioEvent) event).play();
        }
        if (eventDepth > 8) {
            return;
        }
        eventDepth++;
        notifyEventListeners(event, null);
        // Upstream bubbles the event to the host artboard's state machines
        // immediately, naming the nested artboard it came from.
        for (int i = 0; i < eventListeners.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            StateMachineInstance host = eventListeners.get(i);
            host.eventDepth++;
            host.notifyEventListeners(event, eventSource);
            host.eventDepth--;
        }
        eventDepth--;
    }

    /// Upstream `notifyEventListeners`: runs the event listeners that hear
    /// `event`. With a `source`, the event was reported inside that nested
    /// artboard: only listeners targeting it hear it, and their event id is
    /// resolved in the nested artboard's instance.
    private void notifyEventListeners(Event event, NestedArtboard source) {
        Artboard sourceArtboard = source == null ? artboard : source.getArtboardInstance();
        if (sourceArtboard == null) {
            return;
        }
        ArrayList<StateMachine.ListenerDef> defs = machine.getListenerDefs();
        for (int i = 0; i < defs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            StateMachine.ListenerDef d = defs.get(i);
            if (!d.hasType(LISTENER_EVENT) || !(d.listener instanceof StateMachineListenerSingle)) {
                continue;
            }
            if (source != null && d.target != source) { // NOPMD CompareObjectsWithEquals - identity is the intent
                continue;
            }
            if (source == null && d.target != null && d.target != artboard // NOPMD CompareObjectsWithEquals - identity is the intent
                    && !(d.target instanceof Event)) {
                continue;
            }
            if (sourceArtboard.resolve(((StateMachineListenerSingle) d.listener).getEventId()) == event) { // NOPMD CompareObjectsWithEquals - identity is the intent
                performAll(d.actions, 0, 0);
            }
        }
    }

    private void reportEventId(int id) {
        Core c = artboard.resolve(id);
        if (c instanceof Event) {
            reportEvent((Event) c, 0);
        }
    }

    void fireActions(ArrayList<StateMachineFireAction> actions, int occurs) {
        for (int i = 0; i < actions.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            StateMachineFireAction a = actions.get(i);
            if (a.getOccursValue() == occurs && a instanceof StateMachineFireEvent) {
                reportEventId(((StateMachineFireEvent) a).getEventId());
            }
        }
    }

    void scheduledActions(ArrayList<ListenerAction> actions, int occurs) {
        for (int i = 0; i < actions.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            ListenerAction a = actions.get(i);
            if ((a.getFlags() & 1) == occurs) {
                perform(a, 0, 0);
            }
        }
    }

    private void performAll(ArrayList<ListenerAction> actions, float x, float y) {
        for (int i = 0; i < actions.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            perform(actions.get(i), x, y);
        }
    }

    private void perform(ListenerAction a, float x, float y) {
        if (a instanceof ListenerBoolChange) {
            int i = validInput(((ListenerBoolChange) a).getInputId(), INPUT_BOOL);
            if (i >= 0) {
                int v = ((ListenerBoolChange) a).getValue();
                setBool(i, v != 0 && (v == 1 || !boolValue[i]));
            }
        } else if (a instanceof ListenerNumberChange) {
            int i = validInput(((ListenerNumberChange) a).getInputId(), INPUT_NUMBER);
            if (i >= 0) {
                setNum(i, ((ListenerNumberChange) a).getValue());
            }
        } else if (a instanceof ListenerTriggerChange) {
            int i = validInput(((ListenerTriggerChange) a).getInputId(), INPUT_TRIGGER);
            if (i >= 0) {
                fire(i);
            }
        } else if (a instanceof ListenerFireEvent) {
            reportEventId(((ListenerFireEvent) a).getEventId());
        } else if (a instanceof ListenerAlignTarget) {
            Core t = artboard.resolve(((ListenerAlignTarget) a).getTargetId());
            if (t instanceof Node) {
                Node node = (Node) t;
                float lx = x;
                float ly = y;
                ContainerComponent parent = node.getParent();
                if (parent instanceof WorldTransformComponent
                        && inverse.invert(((WorldTransformComponent) parent).getWorldTransform())) {
                    lx = inverse.mapX(x, y);
                    ly = inverse.mapY(x, y);
                }
                node.setX(lx);
                node.setY(ly);
                changed = true;
            }
        } else if (a instanceof ListenerViewModelChange) {
            performViewModelChange((ListenerViewModelChange) a);
        }
    }

    // Upstream ListenerViewModelChange::perform: the bindable's value goes to
    // the view model through its to-source bind (a trigger fires).
    private void performViewModelChange(ListenerViewModelChange a) {
        BindableProperty p = a.getBindable();
        if (p == null) {
            return;
        }
        ArrayList<DataBind> binds = machine.getDataBinds();
        DataBind toSource = null;
        DataBind toTarget = null;
        for (int i = 0; i < binds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            DataBind b = binds.get(i);
            if (b.getTarget() != p) { // NOPMD CompareObjectsWithEquals - identity is the intent
                continue;
            }
            ViewModelInstanceValue src = b.getSource();
            if (src instanceof ViewModelInstanceTrigger && b.toSource()) {
                ((ViewModelInstanceTrigger) src).fire();
                changed = true;
                continue;
            }
            if (b.toSource() && toSource == null) {
                toSource = b;
            }
            if (b.toTarget() && toTarget == null) {
                toTarget = b;
            }
        }
        if (toSource != null) {
            // "My ViewModel" resolves to no property: the data context's own
            // instance is the value.
            if (p instanceof BindablePropertyViewModel && toTarget instanceof DataBindContext
                    && ((DataBindContext) toTarget).sourcePathLength() == 1) {
                ((BindablePropertyViewModel) p).setViewModelInstanceValue(artboard.getViewModelInstance());
            }
            toSource.updateTargetToSource(true);
            changed = true;
        }
    }

    // ------------------------------------------------- view model listeners

    private ViewModelInstance vmListenerRoot;
    private ViewModelInstanceValue[] vmListenerValues;
    private int[] vmListenerSeen;

    // Listeners of type viewModel run when the property they name changed
    // since the previous advance (upstream ListenerViewModelPropertyBinding).
    private void checkViewModelListeners() {
        ViewModelInstance root = artboard.getViewModelInstance();
        ArrayList<StateMachine.ListenerDef> defs = machine.getListenerDefs();
        if (root != vmListenerRoot) { // NOPMD CompareObjectsWithEquals - identity is the intent
            vmListenerRoot = root;
            vmListenerValues = new ViewModelInstanceValue[defs.size()];
            vmListenerSeen = new int[defs.size()];
            for (int i = 0; i < defs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
                int[] path = viewModelPath(defs.get(i));
                if (root != null && path != null) {
                    DataContext ctx = artboard.getDataContext();
                    vmListenerValues[i] = ctx != null ? ctx.resolve(path) : DataBindContext.resolvePath(root, path);
                    if (vmListenerValues[i] != null) {
                        vmListenerSeen[i] = vmListenerValues[i].getChangeCount();
                    }
                }
            }
            return;
        }
        if (vmListenerValues == null) {
            return;
        }
        for (int i = 0; i < vmListenerValues.length; i++) {
            ViewModelInstanceValue v = vmListenerValues[i];
            if (v != null && v.getChangeCount() != vmListenerSeen[i]) {
                vmListenerSeen[i] = v.getChangeCount();
                performAll(defs.get(i).actions, 0, 0);
                changed = true;
            }
        }
    }

    private static int[] viewModelPath(StateMachine.ListenerDef d) {
        for (int i = 0; i < d.inputTypes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            if (d.inputTypes.get(i) instanceof ListenerInputTypeViewModel) {
                return ((ListenerInputTypeViewModel) d.inputTypes.get(i)).getPath();
            }
        }
        if (d.inputTypes.isEmpty() && d.listener instanceof StateMachineListenerSingle
                && ((StateMachineListenerSingle) d.listener).getListenerTypeValue() == LISTENER_VIEW_MODEL) {
            byte[] ids = ((StateMachineListenerSingle) d.listener).getViewModelPathIds();
            return ids == null || ids.length == 0 ? null : DataBindContext.decodePath(ids);
        }
        return null;
    }

    // -------------------------------------------------------------- pointers

    /// A pointer went down at `x`, `y` in artboard coordinates.
    public void pointerDown(float x, float y) {
        pointer(LISTENER_DOWN, x, y);
    }

    /// A pointer went up at `x`, `y` in artboard coordinates.
    public void pointerUp(float x, float y) {
        pointer(LISTENER_UP, x, y);
    }

    /// A pointer moved to `x`, `y` in artboard coordinates.
    public void pointerMove(float x, float y) {
        pointer(LISTENER_MOVE, x, y);
    }

    private void pointer(int type, float ax, float ay) {
        pointerWorld(type, ax - artboard.getWidth() * artboard.getOriginX(),
                ay - artboard.getHeight() * artboard.getOriginY());
    }

    /// The pointer event at `x`, `y` in this artboard's world space: this
    /// machine's listeners, then (upstream `HitNestedArtboard` and
    /// `HitComponentList`) the machines of its nested artboards and of its
    /// component list rows, each at the point mapped into its own space.
    private boolean listenersCached;
    private int listenersGeneration;
    private boolean hasListeners;

    /// True when this machine, a machine of one of its nested artboards or a
    /// machine of a component list row has pointer listeners: a view hosting
    /// it then owns a drag instead of letting a parent scroll. Cached until a
    /// list rebuilds its rows.
    public boolean hasPointerListeners() {
        int gen = ArtboardComponentList.getRowGeneration();
        if (!listenersCached || gen != listenersGeneration) {
            listenersCached = true;
            listenersGeneration = gen;
            hasListeners = computeHasListeners(0);
        }
        return hasListeners;
    }

    private boolean computeHasListeners(int depth) {
        if (!machine.getListenerDefs().isEmpty()) {
            return true;
        }
        if (depth > 16) {
            return false;
        }
        ArrayList<NestedArtboard> nested = artboard.getNestedArtboards();
        for (int i = 0; i < nested.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            ArrayList<NestedAnimation> anims = nested.get(i).getNestedAnimations();
            for (int j = 0; j < anims.size(); j++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                if (anims.get(j) instanceof NestedStateMachine) {
                    StateMachineInstance smi = ((NestedStateMachine) anims.get(j)).getStateMachineInstance();
                    if (smi != null && smi.computeHasListeners(depth + 1)) {
                        return true;
                    }
                }
            }
        }
        ArrayList<ArtboardComponentList> lists = artboard.getComponentLists();
        for (int i = 0; i < lists.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            ArtboardComponentList list = lists.get(i);
            int rowCount = list.getRows().size();
            for (int r = 0; r < rowCount; r++) {
                StateMachineInstance smi = list.getStateMachineInstance(r);
                if (smi != null && smi.computeHasListeners(depth + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void pointerWorld(int type, float x, float y) {
        pointerListeners(type, x, y);
        ArrayList<NestedArtboard> nested = artboard.getNestedArtboards();
        for (int i = 0; i < nested.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            NestedArtboard na = nested.get(i);
            if (na.isCollapsed() || na.isHidden() || na.isPaused()
                    || !inverse.invert(na.getWorldTransform())) {
                continue;
            }
            float nx = inverse.mapX(x, y);
            float ny = inverse.mapY(x, y);
            ArrayList<NestedAnimation> anims = na.getNestedAnimations();
            for (int j = 0; j < anims.size(); j++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                if (anims.get(j) instanceof NestedStateMachine) {
                    StateMachineInstance smi = ((NestedStateMachine) anims.get(j)).getStateMachineInstance();
                    if (smi != null) {
                        smi.pointerWorld(type, nx, ny);
                    }
                }
            }
        }
        ArrayList<ArtboardComponentList> lists = artboard.getComponentLists();
        for (int i = 0; i < lists.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            ArtboardComponentList list = lists.get(i);
            if (list.isCollapsed() || list.isHidden()) {
                continue;
            }
            int rowCount = list.getRows().size();
            for (int r = 0; r < rowCount; r++) {
                StateMachineInstance smi = list.getStateMachineInstance(r);
                if (smi != null && list.worldToLocal(x, y, r, nestedPoint)) {
                    smi.pointerWorld(type, nestedPoint[0], nestedPoint[1]);
                }
            }
        }
    }

    private void pointerListeners(int type, float x, float y) {
        ArrayList<StateMachine.ListenerDef> defs = machine.getListenerDefs();
        for (int i = 0; i < defs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            StateMachine.ListenerDef d = defs.get(i);
            if (d.target == null) {
                continue;
            }
            boolean hit = hitTest(d, x, y);
            if (hit != hovered[i]) {
                hovered[i] = hit;
                if (d.hasType(hit ? LISTENER_ENTER : LISTENER_EXIT)) {
                    performAll(d.actions, x, y);
                }
            }
            if (!hit) {
                if (type == LISTENER_UP) {
                    downHit[i] = false;
                }
                continue;
            }
            if (d.hasType(type)) {
                performAll(d.actions, x, y);
            }
            if (type == LISTENER_DOWN) {
                downHit[i] = true;
            } else if (type == LISTENER_UP) {
                if (downHit[i] && d.hasType(LISTENER_CLICK)) {
                    performAll(d.actions, x, y);
                }
                downHit[i] = false;
            }
        }
    }

    private boolean hitTest(StateMachine.ListenerDef d, float x, float y) {
        if (d.target instanceof Artboard) {
            float ox = artboard.getWidth() * artboard.getOriginX();
            float oy = artboard.getHeight() * artboard.getOriginY();
            return x >= -ox && y >= -oy && x <= artboard.getWidth() - ox && y <= artboard.getHeight() - oy;
        }
        for (int i = 0; i < d.shapes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Shape s = d.shapes.get(i);
            if (s.isHidden() || !inverse.invert(s.getWorldTransform())) {
                continue;
            }
            if (contains(s.getComposedPath(), inverse.mapX(x, y), inverse.mapY(x, y))) {
                return true;
            }
        }
        return false;
    }

    /// Non-zero winding containment of `px`, `py` in `path`, with cubics
    /// flattened and every contour closed.
    static boolean contains(RivePath path, float px, float py) {
        int winding = 0;
        float[] p = path.points;
        int pi = 0;
        float sx = 0;
        float sy = 0;
        float cx = 0;
        float cy = 0;
        for (int v = 0; v < path.verbCount; v++) {
            switch (path.verbs[v]) {
                case RivePath.MOVE:
                    winding += edge(cx, cy, sx, sy, px, py);
                    sx = p[pi];
                    sy = p[pi + 1];
                    cx = sx;
                    cy = sy;
                    pi += 2;
                    break;
                case RivePath.LINE:
                    winding += edge(cx, cy, p[pi], p[pi + 1], px, py);
                    cx = p[pi];
                    cy = p[pi + 1];
                    pi += 2;
                    break;
                case RivePath.CUBIC: {
                    float lx = cx;
                    float ly = cy;
                    for (int k = 1; k <= 12; k++) {
                        float t = k / 12f;
                        float u = 1 - t;
                        float b0 = u * u * u;
                        float b1 = 3 * u * u * t;
                        float b2 = 3 * u * t * t;
                        float b3 = t * t * t;
                        float nx = b0 * cx + b1 * p[pi] + b2 * p[pi + 2] + b3 * p[pi + 4];
                        float ny = b0 * cy + b1 * p[pi + 1] + b2 * p[pi + 3] + b3 * p[pi + 5];
                        winding += edge(lx, ly, nx, ny, px, py);
                        lx = nx;
                        ly = ny;
                    }
                    cx = p[pi + 4];
                    cy = p[pi + 5];
                    pi += 6;
                    break;
                }
                case RivePath.CLOSE:
                    winding += edge(cx, cy, sx, sy, px, py);
                    cx = sx;
                    cy = sy;
                    break;
                default:
                    break;
            }
        }
        winding += edge(cx, cy, sx, sy, px, py);
        return winding != 0;
    }

    private static int edge(float x0, float y0, float x1, float y1, float px, float py) {
        if (y0 <= py) {
            if (y1 > py && (x1 - x0) * (py - y0) - (px - x0) * (y1 - y0) > 0) {
                return 1;
            }
        } else if (y1 <= py && (x1 - x0) * (py - y0) - (px - x0) * (y1 - y0) < 0) {
            return -1;
        }
        return 0;
    }

    // ------------------------------------------------------------ animations

    /// Advances `a` by `elapsed` and reports the event keyframes it crossed,
    /// as upstream's LinearAnimationInstance::advance does with a reporter.
    boolean advanceAnimation(LinearAnimationInstance a, boolean callbacks, float elapsed) {
        LinearAnimation anim = a.getAnimation();
        float last = a.getTime();
        float speedDir = anim.getSpeed() * a.getDirection();
        float unclamped = last + elapsed * speedDir;
        boolean keepGoing = a.advance(elapsed);
        if (callbacks && elapsed != 0) {
            reportCallbacks(anim, last, unclamped, speedDir, false);
            if (a.didLoop() && anim.getLoopValue() == LinearAnimation.LOOP_PING_PONG) {
                reportPingPong(anim, unclamped, elapsed * speedDir < 0 ? -1 : 1, speedDir);
            } else if (a.didLoop() && anim.getLoopValue() == LinearAnimation.LOOP_LOOP) {
                if (speedDir >= 0) {
                    reportCallbacks(anim, 0f, a.getTime(), speedDir, false);
                } else {
                    reportCallbacks(anim, anim.endSeconds(), a.getTime(), speedDir, false);
                }
            }
        }
        return keepGoing;
    }

    static boolean hasCallbacks(LinearAnimation anim) {
        if (anim == null) {
            return false;
        }
        for (int o = 0; o < anim.getKeyedObjectCount(); o++) {
            KeyedObject ko = anim.getKeyedObject(o);
            for (int p = 0; p < ko.getKeyedPropertyCount(); p++) {
                if (ko.getKeyedProperty(p).getPropertyKey() == EVENT_TRIGGER_KEY) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Reports each reflected leg of a ping-pong advance, from the edge it
    /// bounced off to where it went, as upstream LinearAnimationInstance's
    /// pingPong loop does (fromPong alternating, starting true).
    private void reportPingPong(LinearAnimation anim, float unclamped, int dir, float speedDir) {
        float st = anim.startSeconds();
        float en = anim.endSeconds();
        if (en <= st) {
            return;
        }
        float t = unclamped;
        boolean fromPong = true;
        while (true) {
            float edge;
            if (dir == 1 && t >= en) {
                edge = en;
                t = en + (en - t);
            } else if (dir == -1 && t < st) {
                edge = st;
                t = st + (st - t);
            } else {
                break;
            }
            dir = -dir;
            reportCallbacks(anim, edge, t, speedDir, fromPong);
            fromPong = !fromPong;
        }
    }

    private void reportCallbacks(LinearAnimation anim, float from, float to, float speedDir, boolean fromPong) {
        if (Float.floatToIntBits(from) == Float.floatToIntBits(to)) {
            return;
        }
        float start = speedDir >= 0 ? anim.startSeconds() : anim.endSeconds();
        boolean atStart = Float.floatToIntBits(start) == Float.floatToIntBits(from);
        if (atStart && fromPong) {
            return;
        }
        boolean forward = from <= to;
        int fromOffset = forward ? (atStart ? 0 : 1) : (atStart ? 1 : 0);
        int toOffset = forward ? 1 : 0;
        for (int o = 0; o < anim.getKeyedObjectCount(); o++) {
            KeyedObject ko = anim.getKeyedObject(o);
            for (int p = 0; p < ko.getKeyedPropertyCount(); p++) {
                KeyedProperty kp = ko.getKeyedProperty(p);
                if (kp.getPropertyKey() != EVENT_TRIGGER_KEY) {
                    continue;
                }
                int idx = kp.closestFrameIndex(from, fromOffset);
                int idxTo = kp.closestFrameIndex(to, toOffset);
                if (idxTo < idx) {
                    int swap = idx;
                    idx = idxTo;
                    idxTo = swap;
                }
                Core target = artboard.resolve(ko.getObjectId());
                while (idxTo > idx && idx < kp.getKeyFrameCount()) {
                    if (target instanceof Event) {
                        reportEvent((Event) target, to - kp.getKeyFrame(idx).getSeconds());
                    }
                    idx++;
                }
            }
        }
    }

    // ---------------------------------------------------------------- states

    /// The playing side of a layer state.
    abstract static class StateInst {
        final StateMachine.StateDef def;

        StateInst(StateMachine.StateDef def) {
            this.def = def;
        }

        abstract void advance(float seconds);

        abstract void apply(float mix);

        abstract boolean keepGoing();

        void clearSpilledTime() {
        }

        float spilledTime() {
            return 0;
        }

        /// The animation an exit time is measured on, null if none.
        LinearAnimationInstance exitAnimation(StateMachine.TransitionDef t) {
            return null;
        }
    }

    static final class SystemInst extends StateInst {
        SystemInst(StateMachine.StateDef def) {
            super(def);
        }

        @Override
        void advance(float seconds) {
        }

        @Override
        void apply(float mix) {
        }

        @Override
        boolean keepGoing() {
            return false;
        }
    }

    final class AnimInst extends StateInst {
        final LinearAnimationInstance instance;
        final boolean callbacks;
        boolean keepGoing = true;
        boolean spillCleared;

        AnimInst(StateMachine.StateDef def) {
            super(def);
            instance = def.animation == null ? null : new LinearAnimationInstance(def.animation, artboard);
            callbacks = hasCallbacks(def.animation);
        }

        @Override
        void advance(float seconds) {
            if (instance == null) {
                keepGoing = false;
                return;
            }
            spillCleared = false;
            keepGoing = advanceAnimation(instance, callbacks, seconds * ((AnimationState) def.state).getSpeed());
        }

        @Override
        void apply(float mix) {
            if (instance != null) {
                instance.apply(mix);
            }
        }

        @Override
        boolean keepGoing() {
            return keepGoing;
        }

        @Override
        void clearSpilledTime() {
            spillCleared = true;
        }

        @Override
        float spilledTime() {
            return instance == null || spillCleared ? 0 : instance.getSpilledTime();
        }

        @Override
        LinearAnimationInstance exitAnimation(StateMachine.TransitionDef t) {
            return instance;
        }
    }

    final class BlendInst extends StateInst {
        final LinearAnimationInstance[] instances;
        final boolean[] callbacks;
        final float[] mixes;
        boolean keepGoing = true;

        BlendInst(StateMachine.StateDef def) {
            super(def);
            int n = def.blendLinear.length;
            instances = new LinearAnimationInstance[n];
            callbacks = new boolean[n];
            mixes = new float[n];
            for (int i = 0; i < n; i++) {
                if (def.blendLinear[i] != null) {
                    instances[i] = new LinearAnimationInstance(def.blendLinear[i], artboard);
                    callbacks[i] = hasCallbacks(def.blendLinear[i]);
                }
            }
        }

        @Override
        void advance(float seconds) {
            keepGoing = false;
            for (int i = 0; i < instances.length; i++) {
                if (instances[i] != null && advanceAnimation(instances[i], callbacks[i], seconds)) {
                    keepGoing = true;
                }
            }
            if (def.state instanceof BlendStateDirect) {
                advanceDirect();
            } else {
                advance1D();
            }
        }

        private void advanceDirect() {
            for (int i = 0; i < mixes.length; i++) {
                BlendAnimation b = def.blendAnimations.get(i);
                if (!(b instanceof BlendAnimationDirect)) {
                    continue;
                }
                BlendAnimationDirect d = (BlendAnimationDirect) b;
                float value;
                if (d.getBlendSource() == 1) {
                    value = d.getMixValue();
                } else if (d.getBlendSource() == 0) {
                    int in = validInput(d.getInputId(), INPUT_NUMBER);
                    value = in < 0 ? 0 : numberValue[in];
                } else {
                    continue;
                }
                mixes[i] = Math.min(1f, Math.max(0f, value / 100f));
            }
        }

        private float value1D(int i) {
            BlendAnimation b = def.blendAnimations.get(i);
            return b instanceof BlendAnimation1D ? ((BlendAnimation1D) b).getValue() : 0;
        }

        private void advance1D() {
            float value = 0;
            if (def.state instanceof BlendState1DInput) {
                int in = validInput(((BlendState1DInput) def.state).getInputId(), INPUT_NUMBER);
                if (in >= 0) {
                    value = numberValue[in];
                }
            } else if (def.state instanceof BlendState1DViewModel) {
                value = ((BlendState1DViewModel) def.state).getBindableValue();
            }
            int count = mixes.length;
            int idx = 0;
            int start = 0;
            int end = count - 1;
            while (start <= end) {
                int mid = (start + end) >> 1;
                float closest = value1D(mid);
                if (closest < value) {
                    start = mid + 1;
                } else if (closest > value) {
                    end = mid - 1;
                } else {
                    idx = mid;
                    break;
                }
                idx = start;
            }
            boolean hasTo = idx >= 0 && idx < count;
            boolean hasFrom = idx - 1 >= 0 && idx - 1 < count;
            float toValue = hasTo ? value1D(idx) : 0;
            float fromValue = hasFrom ? value1D(idx - 1) : 0;
            float mix;
            float mixFrom;
            if (!hasTo || !hasFrom || Float.floatToIntBits(toValue) == Float.floatToIntBits(fromValue)) {
                mix = 1f;
                mixFrom = 1f;
            } else {
                mix = (value - fromValue) / (toValue - fromValue);
                mixFrom = 1f - mix;
            }
            for (int i = 0; i < count; i++) {
                float v = value1D(i);
                if (hasTo && Float.floatToIntBits(v) == Float.floatToIntBits(toValue)) {
                    mixes[i] = mix;
                } else if (hasFrom && Float.floatToIntBits(v) == Float.floatToIntBits(fromValue)) {
                    mixes[i] = mixFrom;
                } else {
                    mixes[i] = 0;
                }
            }
        }

        @Override
        void apply(float mix) {
            for (int i = 0; i < instances.length; i++) {
                if (instances[i] != null) {
                    instances[i].apply(mixes[i] * mix);
                }
            }
        }

        @Override
        boolean keepGoing() {
            return keepGoing;
        }

        @Override
        LinearAnimationInstance exitAnimation(StateMachine.TransitionDef t) {
            if (t.transition instanceof BlendStateTransition) {
                int i = ((BlendStateTransition) t.transition).getExitBlendAnimationId();
                if (i >= 0 && i < instances.length) {
                    return instances[i];
                }
            }
            return null;
        }
    }

    StateInst makeInstance(StateMachine.StateDef def) {
        if (def.state instanceof AnimationState) {
            return new AnimInst(def);
        }
        if (def.state instanceof BlendState) {
            return new BlendInst(def);
        }
        return new SystemInst(def);
    }

    // ---------------------------------------------------------------- layers

    final class Layer {
        final StateMachine.LayerDef def;
        final int index;
        StateInst any;
        StateInst current;
        StateInst from;
        StateMachine.TransitionDef transition;
        LinearAnimation holdAnimation;
        float holdTime;
        float mix = 1f;
        float mixFrom = 1f;
        boolean transitionCompleted;
        boolean holdAnimationFrom;
        boolean waitingForExit;
        /// Upstream `m_animationReset`; `resetActive` stands for non-null.
        final AnimationReset reset = new AnimationReset();
        boolean resetActive;

        Layer(StateMachine.LayerDef def, int index) {
            this.def = def;
            this.index = index;
            if (def.any != null) {
                any = makeInstance(def.any);
            }
        }

        float mixTime() {
            int dur = transition.transition.getDuration();
            if (dur == 0) {
                return 0;
            }
            if (transition.hasFlag(FLAG_DURATION_PERCENT)) {
                float animDuration = 0;
                if (from != null && from.def.animation != null) {
                    animDuration = from.def.animation.durationSeconds();
                }
                return dur / 100f * animDuration;
            }
            return dur / 1000f;
        }

        void updateMix(float seconds) {
            if (transition != null && from != null && transition.transition.getDuration() != 0) {
                float mt = mixTime();
                if (mt == 0f) {
                    mix = 1f;
                } else {
                    mix = Math.min(1f, Math.max(0f, mix + seconds / mt));
                }
                if (mix == 1f && !transitionCompleted) {
                    transitionCompleted = true;
                    clearAnimationReset();
                    fireActions(transition.fireActions, 1);
                    scheduledActions(transition.actions, 1);
                }
            } else {
                mix = 1f;
            }
        }

        boolean isTransitioning() {
            return transition != null && from != null && transition.transition.getDuration() != 0 && mix < 1f;
        }

        boolean advance(float seconds) {
            if (current != null) {
                current.advance(seconds);
            }
            updateMix(seconds);
            if (from != null && mix < 1f && !holdAnimationFrom) {
                from.advance(seconds);
            }
            apply();
            boolean changedState = false;
            for (int i = 0; updateState(); i++) {
                changedState = true;
                apply();
                if (i == MAX_ITERATIONS) {
                    return false;
                }
            }
            if (current != null) {
                current.clearSpilledTime();
            }
            return changedState || mix != 1f || resetActive || waitingForExit
                    || current != null && current.keepGoing();
        }

        boolean updateState() {
            if (isTransitioning() && !transition.hasFlag(FLAG_EARLY_EXIT)) {
                return false;
            }
            waitingForExit = false;
            return tryChangeState(any) || tryChangeState(current);
        }

        boolean canChangeState(StateMachine.StateDef to) {
            return (current == null ? null : current.def) != to; // NOPMD CompareObjectsWithEquals - identity is the intent
        }

        void changeState(StateMachine.StateDef to) {
            if ((current == null ? null : current.def) == to) { // NOPMD CompareObjectsWithEquals - identity is the intent
                return;
            }
            if (current != null) {
                fireActions(current.def.fireActions, 1);
                scheduledActions(current.def.actions, 1);
            }
            current = to == null ? null : makeInstance(to);
            if (current != null) {
                fireActions(current.def.fireActions, 0);
                scheduledActions(current.def.actions, 0);
            }
        }

        // 0 no, 1 yes, 2 waiting for exit
        int allowed(StateMachine.TransitionDef t, StateInst stateFrom) {
            if (t.hasFlag(FLAG_DISABLED)) {
                return 0;
            }
            for (int i = 0; i < t.conditions.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                if (!evaluate(t.conditions.get(i))) {
                    return 0;
                }
            }
            if (t.hasFlag(FLAG_EXIT_TIME)) {
                LinearAnimationInstance exit = stateFrom.exitAnimation(t);
                if (exit != null) {
                    float lastTime = exit.getLastTotalTime();
                    float time = exit.getTotalTime();
                    float exitTime = exitTimeSeconds(t, stateFrom, false);
                    LinearAnimation animFrom = exit.getAnimation();
                    float duration = animFrom.durationSeconds();
                    if (exitTime <= duration && animFrom.getLoopValue() != LinearAnimation.LOOP_ONE_SHOT
                            && duration > 0) {
                        exitTime += (float) Math.floor(lastTime / duration) * duration;
                    }
                    if (time < exitTime) {
                        return 2;
                    }
                }
            }
            return 1;
        }

        float exitTimeSeconds(StateMachine.TransitionDef t, StateInst stateFrom, boolean absolute) {
            if (t.hasFlag(FLAG_EXIT_PERCENT)) {
                float duration = 0;
                float start = 0;
                LinearAnimationInstance exit = stateFrom.exitAnimation(t);
                if (exit != null) {
                    start = absolute ? exit.getAnimation().startSeconds() : 0;
                    duration = exit.getAnimation().durationSeconds();
                }
                return start + t.transition.getExitTime() / 100f * duration;
            }
            return t.transition.getExitTime() / 1000f;
        }

        boolean evaluate(TransitionCondition c) {
            if (c instanceof TransitionTriggerCondition) {
                int id = ((TransitionTriggerCondition) c).getInputId();
                int i = validInput(id, INPUT_TRIGGER);
                if (i < 0) {
                    return true;
                }
                return fired[i] && !usedInLayer[i][index];
            }
            if (c instanceof TransitionBoolCondition) {
                int i = validInput(((TransitionBoolCondition) c).getInputId(), INPUT_BOOL);
                if (i < 0) {
                    return true;
                }
                int op = ((TransitionBoolCondition) c).getOpValue();
                return boolValue[i] && op == 0 || !boolValue[i] && op == 1;
            }
            if (c instanceof TransitionNumberCondition) {
                int i = validInput(((TransitionNumberCondition) c).getInputId(), INPUT_NUMBER);
                if (i < 0) {
                    return true;
                }
                float v = numberValue[i];
                float target = ((TransitionNumberCondition) c).getValue();
                switch (((TransitionNumberCondition) c).getOpValue()) {
                    case 0:
                        return v == target;
                    case 1:
                        return v != target;
                    case 2:
                        return v <= target;
                    case 3:
                        return v >= target;
                    case 4:
                        return v < target;
                    case 5:
                        return v > target;
                    default:
                        return false;
                }
            }
            if (c instanceof TransitionViewModelCondition) {
                return ((TransitionViewModelCondition) c).evaluate();
            }
            // Artboard and focus conditions never pass.
            return false;
        }

        void useInLayer(StateMachine.TransitionDef t) {
            for (int i = 0; i < t.conditions.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                TransitionCondition c = t.conditions.get(i);
                if (c instanceof TransitionTriggerCondition) {
                    int in = validInput(((TransitionTriggerCondition) c).getInputId(), INPUT_TRIGGER);
                    if (in >= 0) {
                        usedInLayer[in][index] = true;
                    }
                }
            }
        }

        StateMachine.TransitionDef findAllowed(StateInst stateFrom) {
            ArrayList<StateMachine.TransitionDef> list = stateFrom.def.transitions;
            boolean isRandom = (stateFrom.def.state.getFlags() & STATE_RANDOM) != 0;
            int totalWeight = 0;
            int[] weights = isRandom ? new int[list.size()] : null;
            for (int i = 0; i < list.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                StateMachine.TransitionDef t = list.get(i);
                if (!canChangeState(t.stateTo)) {
                    continue;
                }
                int allowed = allowed(t, stateFrom);
                if (allowed == 1) {
                    if (weights == null) {
                        useInLayer(t);
                        return t;
                    }
                    weights[i] = t.transition.getRandomWeight();
                    totalWeight += weights[i];
                } else if (allowed == 2) {
                    waitingForExit = true;
                }
            }
            if (weights != null && totalWeight > 0) {
                double r = random.nextFloat() * (double) totalWeight;
                double cur = 0;
                for (int i = 0; i < list.size(); i++) {
                    if (cur + weights[i] > r) {
                        useInLayer(list.get(i));
                        return list.get(i);
                    }
                    cur += weights[i];
                }
            }
            return null;
        }

        boolean tryChangeState(StateInst stateFrom) {
            if (stateFrom == null) {
                return false;
            }
            StateInst outState = current;
            StateMachine.TransitionDef t = findAllowed(stateFrom);
            if (t == null) {
                return false;
            }
            clearAnimationReset();
            changeState(t.stateTo);
            transition = t;
            fireActions(t.fireActions, 0);
            scheduledActions(t.actions, 0);
            if (t.transition.getDuration() == 0) {
                transitionCompleted = true;
                fireActions(t.fireActions, 1);
                scheduledActions(t.actions, 1);
            } else {
                transitionCompleted = false;
            }
            from = outState;
            if (!transitionCompleted) {
                buildAnimationResetForTransition();
            }
            if (outState != null && applyExitCondition(t, outState)) {
                LinearAnimationInstance inst = outState.exitAnimation(t);
                if (inst != null) {
                    holdAnimation = inst.getAnimation();
                    holdTime = inst.getTime();
                }
            }
            // An interrupted mix's blend is already in the reset values.
            mixFrom = !transitionCompleted && mix < 1f ? 0f : mix;
            if (mix != 0f) {
                holdAnimationFrom = t.hasFlag(FLAG_PAUSE_ON_EXIT);
            }
            if (current != null) {
                float advanceTime = from instanceof AnimInst ? from.spilledTime() : 0;
                current.advance(advanceTime);
            }
            mix = 0f;
            updateMix(0f);
            waitingForExit = false;
            return true;
        }

        boolean applyExitCondition(StateMachine.TransitionDef t, StateInst stateFrom) {
            boolean useExitTime = t.hasFlag(FLAG_EXIT_TIME) && stateFrom instanceof AnimInst;
            if (t.hasFlag(FLAG_PAUSE_ON_EXIT) && useExitTime) {
                LinearAnimationInstance inst = stateFrom.exitAnimation(t);
                if (inst != null) {
                    inst.setTime(exitTimeSeconds(t, stateFrom, true));
                }
                return true;
            }
            return useExitTime;
        }

        /// Upstream `buildAnimationResetForTransition`: snapshots what the
        /// from and to states' animations key, as they stand now.
        void buildAnimationResetForTransition() {
            reset.build(from == null ? null : from.def.animation,
                    current == null ? null : current.def.animation, artboard);
            resetActive = true;
        }

        void clearAnimationReset() {
            if (resetActive) {
                reset.clear();
                resetActive = false;
            }
        }

        void apply() {
            if (resetActive) {
                reset.apply();
            }
            if (holdAnimation != null) {
                holdAnimation.apply(artboard, holdTime, mixFrom);
                holdAnimation = null;
            }
            KeyFrameInterpolator interp = transition == null ? null : transition.interpolator;
            if (from != null && mix < 1f) {
                float fromMix = interp != null && mixFrom != 0f ? interp.transform(mixFrom) : mixFrom;
                from.apply(fromMix);
            }
            if (current != null) {
                current.apply(interp != null ? interp.transform(mix) : mix);
            }
        }
    }
}
