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

/// A state machine: inputs, layers of states and the listeners that drive
/// inputs from pointer events.
///
/// The objects that follow a state machine in the file are routed the way
/// upstream's importer stack routes them: a layer takes the states after it,
/// a state the transitions (and, for a blend state, the blend animations)
/// after it, a transition the conditions after it, a listener its actions and
/// input types; fire events and scheduled listener actions go to the latest
/// state or transition. Ids are resolved in `initialize`.
public class StateMachine extends StateMachineBase {
    /// A transition with what it owns, resolved against the layer and artboard.
    public static final class TransitionDef {
        public final StateTransition transition;
        public final ArrayList<TransitionCondition> conditions = new ArrayList<TransitionCondition>();
        public final ArrayList<StateMachineFireAction> fireActions = new ArrayList<StateMachineFireAction>();
        public final ArrayList<ListenerAction> actions = new ArrayList<ListenerAction>();
        public StateDef stateTo;
        public KeyFrameInterpolator interpolator;

        TransitionDef(StateTransition transition) {
            this.transition = transition;
        }

        public boolean hasFlag(int flag) {
            return (transition.getFlags() & flag) != 0;
        }
    }

    /// A layer state with what it owns.
    public static final class StateDef {
        public final LayerState state;
        public final ArrayList<TransitionDef> transitions = new ArrayList<TransitionDef>();
        public final ArrayList<BlendAnimation> blendAnimations = new ArrayList<BlendAnimation>();
        public final ArrayList<StateMachineFireAction> fireActions = new ArrayList<StateMachineFireAction>();
        public final ArrayList<ListenerAction> actions = new ArrayList<ListenerAction>();
        /// The animation of an animation state, null when it has none.
        public LinearAnimation animation;
        /// The animation of each blend animation, by index; entries may be null.
        public LinearAnimation[] blendLinear = new LinearAnimation[0];

        StateDef(LayerState state) {
            this.state = state;
        }
    }

    /// A layer with its states.
    public static final class LayerDef {
        public final StateMachineLayer layer;
        public final ArrayList<StateDef> states = new ArrayList<StateDef>();
        public StateDef entry;
        public StateDef any;
        public StateDef exit;

        LayerDef(StateMachineLayer layer) {
            this.layer = layer;
        }
    }

    /// A listener with its actions and the shapes it hit tests.
    public static final class ListenerDef {
        public final StateMachineListener listener;
        public final ArrayList<ListenerAction> actions = new ArrayList<ListenerAction>();
        public final ArrayList<ListenerInputType> inputTypes = new ArrayList<ListenerInputType>();
        public final ArrayList<Shape> shapes = new ArrayList<Shape>();
        public Core target;

        ListenerDef(StateMachineListener listener) {
            this.listener = listener;
        }

        /// Whether this listener reacts to the `ListenerType` value `type`.
        public boolean hasType(int type) {
            if (inputTypes.isEmpty()) {
                return listener instanceof StateMachineListenerSingle
                        && ((StateMachineListenerSingle) listener).getListenerTypeValue() == type;
            }
            for (int i = 0; i < inputTypes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                if (inputTypes.get(i).getListenerTypeValue() == type) {
                    return true;
                }
            }
            return false;
        }
    }

    private final ArrayList<Core> inputs = new ArrayList<Core>();
    private final ArrayList<DataBind> dataBinds = new ArrayList<DataBind>();
    private final ArrayList<Core> layers = new ArrayList<Core>();
    private final ArrayList<Core> listeners = new ArrayList<Core>();
    private final ArrayList<LayerDef> layerDefs = new ArrayList<LayerDef>();
    private final ArrayList<ListenerDef> listenerDefs = new ArrayList<ListenerDef>();
    private LayerDef currentLayer;
    private StateDef currentState;
    private TransitionDef currentTransition;
    private ListenerDef currentListener;
    /// The latest state or transition, upstream's latest
    /// StateMachineLayerComponentImporter.
    private Object latestLayerComponent;

    public void addInput(Core input) {
        inputs.add(input);
    }

    public void addLayer(Core layer) {
        importObject(layer);
    }

    public void addListener(Core listener) {
        importObject(listener);
    }

    public ArrayList<Core> getInputs() {
        return inputs;
    }

    public ArrayList<Core> getLayers() {
        return layers;
    }

    public ArrayList<Core> getListeners() {
        return listeners;
    }

    public ArrayList<LayerDef> getLayerDefs() {
        return layerDefs;
    }

    public ArrayList<ListenerDef> getListenerDefs() {
        return listenerDefs;
    }

    /// Takes an object that follows this state machine in the file.
    private BindableProperty lastBindable;
    private final ArrayList<TransitionViewModelCondition> selfConditions = new ArrayList<TransitionViewModelCondition>();

    public void importObject(Core object) {
        if (object instanceof StateMachineInput) {
            inputs.add(object);
        } else if (object instanceof StateMachineLayer) {
            layers.add(object);
            currentLayer = new LayerDef((StateMachineLayer) object);
            layerDefs.add(currentLayer);
            currentState = null;
            currentTransition = null;
            latestLayerComponent = null;
        } else if (object instanceof LayerState) {
            if (currentLayer == null) {
                return;
            }
            currentState = new StateDef((LayerState) object);
            currentLayer.states.add(currentState);
            if (object instanceof EntryState) {
                currentLayer.entry = currentState;
            } else if (object instanceof AnyState) {
                currentLayer.any = currentState;
            } else if (object instanceof ExitState) {
                currentLayer.exit = currentState;
            }
            currentTransition = null;
            latestLayerComponent = currentState;
        } else if (object instanceof StateTransition) {
            if (currentState == null) {
                return;
            }
            currentTransition = new TransitionDef((StateTransition) object);
            currentState.transitions.add(currentTransition);
            latestLayerComponent = currentTransition;
        } else if (object instanceof TransitionCondition) {
            if (currentTransition != null) {
                currentTransition.conditions.add((TransitionCondition) object);
            }
        } else if (object instanceof BlendAnimation) {
            if (currentState != null) {
                currentState.blendAnimations.add((BlendAnimation) object);
            }
        } else if (object instanceof StateMachineFireAction) {
            if (latestLayerComponent instanceof StateDef) {
                ((StateDef) latestLayerComponent).fireActions.add((StateMachineFireAction) object);
            } else if (latestLayerComponent instanceof TransitionDef) {
                ((TransitionDef) latestLayerComponent).fireActions.add((StateMachineFireAction) object);
            }
        } else if (object instanceof StateMachineListener) {
            listeners.add(object);
            currentListener = new ListenerDef((StateMachineListener) object);
            listenerDefs.add(currentListener);
        } else if (object instanceof ListenerInputType) {
            if (currentListener != null) {
                currentListener.inputTypes.add((ListenerInputType) object);
            }
        } else if (object instanceof ListenerAction) {
            importAction((ListenerAction) object);
        } else if (object instanceof BindableProperty) {
            importBindable((BindableProperty) object);
        } else if (object instanceof TransitionComparator) {
            if (object instanceof TransitionPropertyViewModelComparator) {
                ((TransitionPropertyViewModelComparator) object).setBindable(lastBindable);
            }
            TransitionViewModelCondition c = latestViewModelCondition();
            if (c != null) {
                c.setComparator((TransitionComparator) object);
                if (c.isSelf()) {
                    selfConditions.add(c);
                    for (int i = 0; i < dataBinds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
                        if (dataBinds.get(i).getTarget() == c.getBindable()) { // NOPMD CompareObjectsWithEquals - identity is the intent
                            c.setSelfBind(dataBinds.get(i));
                        }
                    }
                }
            }
        }
    }

    /// A data bind targeting a bindable property or a transition.
    public void addDataBind(DataBind b) {
        dataBinds.add(b);
        for (int i = 0; i < selfConditions.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            TransitionViewModelCondition c = selfConditions.get(i);
            if (c.getBindable() == b.getTarget()) { // NOPMD CompareObjectsWithEquals - identity is the intent
                c.setSelfBind(b);
            }
        }
    }

    public ArrayList<DataBind> getDataBinds() {
        return dataBinds;
    }

    private TransitionViewModelCondition latestViewModelCondition() {
        if (currentTransition == null || currentTransition.conditions.isEmpty()) {
            return null;
        }
        TransitionCondition c = currentTransition.conditions.get(currentTransition.conditions.size() - 1);
        return c instanceof TransitionViewModelCondition ? (TransitionViewModelCondition) c : null;
    }

    // A bindable property is followed by what reads it: a property comparator
    // of a view model condition, a ListenerViewModelChange, or (for a 1D blend
    // state) nothing, in which case it belongs to that state.
    private void importBindable(BindableProperty p) {
        lastBindable = p;
        if (latestLayerComponent instanceof StateDef
                && ((StateDef) latestLayerComponent).state instanceof BlendState1DViewModel) {
            ((BlendState1DViewModel) ((StateDef) latestLayerComponent).state).setBindable(p);
        }
    }

    private void importAction(ListenerAction action) {
        if (action instanceof ListenerViewModelChange) {
            ((ListenerViewModelChange) action).setBindable(lastBindable);
        }
        int kind = (action.getFlags() >> 1) & 0x3;
        if (kind == 1 || kind == 2) {
            if (latestLayerComponent instanceof StateDef) {
                ((StateDef) latestLayerComponent).actions.add(action);
            } else if (latestLayerComponent instanceof TransitionDef) {
                ((TransitionDef) latestLayerComponent).actions.add(action);
            }
        } else if (currentListener != null) {
            currentListener.actions.add(action);
        }
    }

    public void initialize(Artboard artboard) {
        ArrayList<LinearAnimation> anims = artboard.getAnimations();
        for (int l = 0; l < layerDefs.size(); l++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            LayerDef layer = layerDefs.get(l);
            for (int s = 0; s < layer.states.size(); s++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                StateDef state = layer.states.get(s);
                if (state.state instanceof AnimationState) {
                    state.animation = animation(anims, ((AnimationState) state.state).getAnimationId());
                }
                state.blendLinear = new LinearAnimation[state.blendAnimations.size()];
                for (int b = 0; b < state.blendLinear.length; b++) {
                    state.blendLinear[b] = animation(anims, state.blendAnimations.get(b).getAnimationId());
                }
                for (int t = 0; t < state.transitions.size(); t++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                    TransitionDef tr = state.transitions.get(t);
                    int to = tr.transition.getStateToId();
                    tr.stateTo = to >= 0 && to < layer.states.size() ? layer.states.get(to) : null;
                    Core interp = artboard.resolve(tr.transition.getInterpolatorId());
                    tr.interpolator = interp instanceof KeyFrameInterpolator ? (KeyFrameInterpolator) interp : null;
                }
            }
        }
        ArrayList<Core> objects = artboard.getObjects();
        for (int i = 0; i < listenerDefs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            ListenerDef def = listenerDefs.get(i);
            def.target = artboard.resolve(def.listener.getTargetId());
            if (def.target == null || def.target instanceof Artboard) {
                continue;
            }
            for (int o = 0; o < objects.size(); o++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                Core c = objects.get(o);
                if (c instanceof Shape && isDescendant((Component) c, def.target)) {
                    def.shapes.add((Shape) c);
                }
            }
        }
    }

    private static boolean isDescendant(Component c, Core target) {
        Component p = c;
        while (p != null) {
            if (p == target) { // NOPMD CompareObjectsWithEquals - identity is the intent
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    private static LinearAnimation animation(ArrayList<LinearAnimation> anims, int id) {
        return id >= 0 && id < anims.size() ? anims.get(id) : null;
    }
}
