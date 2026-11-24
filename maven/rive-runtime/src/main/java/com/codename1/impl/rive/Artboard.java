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

/// The root of a scene: owns every component of the scene by index, its
/// animations and state machines, updates components in dependency order and
/// draws its drawables.
public class Artboard extends ArtboardBase {
    private final ArrayList<Core> objects = new ArrayList<Core>();
    private final ArrayList<LinearAnimation> animations = new ArrayList<LinearAnimation>();
    private final ArrayList<StateMachine> stateMachines = new ArrayList<StateMachine>();
    private final ArrayList<Drawable> drawables = new ArrayList<Drawable>();
    private final ArrayList<ShapePaint> backgroundPaints = new ArrayList<ShapePaint>();
    private Component[] graph = new Component[0];
    private int dirtDepth = Integer.MAX_VALUE;
    private int invalidObjects;
    private int unresolvedKeyedObjects;
    private boolean dirty = true;
    private final Mat2D originTransform = new Mat2D();
    private final RivePath bounds = new RivePath();
    private RiveFileContext file;
    private final ArrayList<DataBind> dataBinds = new ArrayList<DataBind>();
    private ViewModelInstance viewModelInstance;
    private DataContext dataContext;
    private final ArrayList<NestedArtboard> nestedArtboards = new ArrayList<NestedArtboard>();

    public Artboard() {
        artboard = this;
    }

    /// Objects dropped at load because their parent did not resolve; a file
    /// read correctly has none.
    public int getInvalidObjectCount() {
        return invalidObjects;
    }

    /// Animated objects dropped at load because their target did not resolve.
    public int getUnresolvedKeyedObjectCount() {
        return unresolvedKeyedObjects;
    }

    public void addObject(Core o) {
        objects.add(o);
    }

    public ArrayList<Core> getObjects() {
        return objects;
    }

    private final ArrayList<Component> runtimeComponents = new ArrayList<Component>();

    /// Registers a component the runtime made (not read from the file), so
    /// it takes part in the update order.
    void addRuntimeComponent(Component c) {
        runtimeComponents.add(c);
    }

    public void addAnimation(LinearAnimation a) {
        animations.add(a);
    }

    public void addStateMachine(StateMachine s) {
        stateMachines.add(s);
    }

    public ArrayList<LinearAnimation> getAnimations() {
        return animations;
    }

    public ArrayList<StateMachine> getStateMachines() {
        return stateMachines;
    }

    /// Adds a data bind on one of this artboard's objects.
    /// The binds targeting this artboard's components.
    public ArrayList<DataBind> getDataBinds() {
        return dataBinds;
    }

    public void addDataBind(DataBind b) {
        dataBinds.add(b);
    }

    /// The view model instance bound to this artboard, or null.
    public ViewModelInstance getViewModelInstance() {
        return viewModelInstance;
    }

    /// Binds `instance` to this artboard's data binds and to its state
    /// machines' (upstream `Artboard::bindViewModelInstance`).
    public void bindViewModelInstance(ViewModelInstance instance) {
        bindDataContext(instance == null ? null : new DataContext(instance, null));
    }

    /// The context of the latest bind, or null.
    public DataContext getDataContext() {
        return dataContext;
    }

    /// Binds this artboard, its state machines and its nested artboards to
    /// `context` (upstream `bindViewModelInstance` with a parent context). A
    /// nested artboard with a data bind path binds the instance that path
    /// names, with this context as its parent; one without shares this context.
    public void bindDataContext(DataContext context) {
        dataContext = context;
        viewModelInstance = context == null ? null : context.getMain();
        for (int i = 0; i < dataBinds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            dataBinds.get(i).bind(context);
        }
        for (int s = 0; s < stateMachines.size(); s++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            ArrayList<DataBind> binds = stateMachines.get(s).getDataBinds();
            for (int i = 0; i < binds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
                binds.get(i).bind(context);
            }
        }
        for (int i = 0; i < objects.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            Core o = objects.get(i);
            if (o instanceof NestedArtboard && o != this) { // NOPMD CompareObjectsWithEquals - identity is the intent
                ((NestedArtboard) o).bindDataContext(context);
            }
        }
        updateDataBinds();
    }

    /// Advances the converters of every bind (upstream `advanceDataBinds`),
    /// so a time based converter animates its output.
    private void advanceDataBinds(float elapsedSeconds) {
        if (viewModelInstance == null) {
            return;
        }
        for (int i = 0; i < dataBinds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            dataBinds.get(i).advance(elapsedSeconds);
        }
        for (int s = 0; s < stateMachines.size(); s++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            ArrayList<DataBind> binds = stateMachines.get(s).getDataBinds();
            for (int i = 0; i < binds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                binds.get(i).advance(elapsedSeconds);
            }
        }
    }

    /// Writes changed view model values to their targets.
    public void updateDataBinds() {
        if (viewModelInstance == null) {
            return;
        }
        for (int i = 0; i < dataBinds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            dataBinds.get(i).updateSourceToTarget();
        }
        for (int s = 0; s < stateMachines.size(); s++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            ArrayList<DataBind> binds = stateMachines.get(s).getDataBinds();
            for (int i = 0; i < binds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                binds.get(i).updateSourceToTarget();
            }
        }
    }

    private void updateDataBindsToSource() {
        if (viewModelInstance == null) {
            return;
        }
        for (int i = 0; i < dataBinds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            dataBinds.get(i).updateTargetToSource();
        }
    }

    public RiveFileContext getFile() {
        return file;
    }

    void setFile(RiveFileContext f) {
        file = f;
    }

    @Override
    public float getWidth() {
        return mWidth;
    }

    @Override
    public float getHeight() {
        return mHeight;
    }

    /// The object at artboard index `id`, or null.
    public Core resolve(int id) {
        if (id < 0 || id >= objects.size()) {
            return null;
        }
        return objects.get(id);
    }

    /// The artboard is the root of its own space: its x and y place it on the
    /// editor's canvas and must not move its contents.
    @Override
    public void updateTransform() {
        transform.identity();
    }

    @Override
    public void updateWorldTransform() {
        worldTransform.identity();
    }

    public void addBackgroundPaint(ShapePaint p) {
        backgroundPaints.add(p);
    }

    /// Wires every object up once the artboard is fully read.
    public int initialize() {
        for (int i = 0; i < objects.size(); i++) {
            Core o = objects.get(i);
            if (o instanceof Component) {
                Component c = (Component) o;
                if (c.onAddedDirty(this) != STATUS_OK) {
                    objects.set(i, null);
                    invalidObjects++;
                }
            }
        }
        for (int i = 0; i < objects.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Core o = objects.get(i);
            if (o instanceof Component) {
                ((Component) o).onAddedClean(this);
            }
        }
        for (int i = 0; i < animations.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            LinearAnimation a = animations.get(i);
            int before = a.getKeyedObjectCount();
            a.initialize(this);
            unresolvedKeyedObjects += before - a.getKeyedObjectCount();
        }
        for (int i = 0; i < stateMachines.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            stateMachines.get(i).initialize(this);
        }
        for (int i = 0; i < objects.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Core o = objects.get(i);
            if (o instanceof Component) {
                ((Component) o).buildDependencies();
            }
            if (o instanceof Drawable && o != this) { // NOPMD CompareObjectsWithEquals - identity is the intent
                drawables.add((Drawable) o);
            }
            if (o instanceof NestedArtboard) {
                nestedArtboards.add((NestedArtboard) o);
            }
        }
        for (int i = 0; i < runtimeComponents.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            runtimeComponents.get(i).buildDependencies();
        }
        prepareLayout(objects, drawables);
        sortDependencies();
        return STATUS_OK;
    }

    private void sortDependencies() {
        ArrayList<Component> order = new ArrayList<Component>();
        java.util.HashMap<Component, Boolean> marks = new java.util.HashMap<Component, Boolean>();
        visit(this, marks, order);
        // Components outside the parent graph (paints, constraints' helpers)
        // still update, after everything they read.
        for (int i = 0; i < objects.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Core o = objects.get(i);
            if (o instanceof Component && !marks.containsKey(o)) {
                visit((Component) o, marks, order);
            }
        }
        for (int i = 0; i < runtimeComponents.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            visit(runtimeComponents.get(i), marks, order);
        }
        graph = new Component[order.size()];
        for (int i = 0; i < graph.length; i++) {
            graph[i] = order.get(order.size() - 1 - i);
            graph[i].graphOrder = i;
        }
    }

    /// Depth-first post-order; a cycle (which a valid file has not) is cut.
    private static void visit(Component c, java.util.HashMap<Component, Boolean> marks, ArrayList<Component> order) {
        Boolean m = marks.get(c);
        if (m != null) {
            return;
        }
        marks.put(c, Boolean.FALSE);
        ArrayList<Component> deps = c.getDependents();
        if (deps != null) {
            for (int i = 0; i < deps.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                visit(deps.get(i), marks, order);
            }
        }
        marks.put(c, Boolean.TRUE);
        order.add(c);
    }

    public void onComponentDirty(Component c) {
        dirty = true;
        if (c.graphOrder < dirtDepth) {
            dirtDepth = c.graphOrder;
        }
    }

    /// Updates every dirty component in dependency order; a port of
    /// `Artboard::updateComponents`. When an update dirties a component
    /// earlier in the order, the pass starts over from the top. Answers
    /// whether anything was updated.
    public boolean updateComponents() {
        if (!dirty) {
            return false;
        }
        boolean didUpdate = false;
        int step = 0;
        while (dirty && step < 100) {
            dirty = false;
            for (int i = 0; i < graph.length; i++) {
                Component c = graph[i];
                dirtDepth = i;
                int d = c.peekDirt();
                if ((d & DIRT_COLLAPSED) != 0 || d == DIRT_NONE) {
                    continue;
                }
                c.takeDirt();
                c.update(d);
                didUpdate = true;
                if (dirtDepth < i) {
                    break;
                }
            }
            step++;
        }
        return didUpdate;
    }

    /// Upstream `advance` -> `advanceInternal` then `updatePass`: the advancing
    /// components (nested artboards, component list rows) advance FIRST, so
    /// what they changed is picked up by this artboard's update in the same
    /// frame rather than the next one.
    public boolean advance(float elapsedSeconds) {
        boolean didUpdate = false;
        for (int i = 0; i < nestedArtboards.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            if (nestedArtboards.get(i).advanceNested(elapsedSeconds)) {
                didUpdate = true;
            }
        }
        for (int i = 0; i < componentLists.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            if (componentLists.get(i).advanceRows(elapsedSeconds)) {
                didUpdate = true;
            }
        }
        updateLayout(elapsedSeconds);
        advanceDataBinds(elapsedSeconds);
        updateDataBinds();
        if (updateComponents()) {
            didUpdate = true;
        }
        updateDataBindsToSource();
        return didUpdate;
    }

    private final ArrayList<ArtboardComponentList> componentLists = new ArrayList<ArtboardComponentList>();

    /// The nested artboards of this artboard, in draw order.
    ArrayList<NestedArtboard> getNestedArtboards() {
        return nestedArtboards;
    }

    /// The component lists registered with this artboard.
    ArrayList<ArtboardComponentList> getComponentLists() {
        return componentLists;
    }

    /// Registers a list whose rows advance with this artboard.
    void addComponentList(ArtboardComponentList l) {
        if (!componentLists.contains(l)) {
            componentLists.add(l);
        }
    }

    /// Draws this artboard as the content of a [NestedArtboard]: its origin
    /// sits at the host's transform rather than at its frame's corner.
    public void drawHosted(RiveRenderer renderer) {
        drawInternal(renderer, false);
    }

    @Override
    public void draw(RiveRenderer renderer) {
        drawInternal(renderer, true);
    }

    private void drawInternal(RiveRenderer renderer, boolean frameOrigin) {
        renderer.save();
        // A hug artboard is sized by its layout, not its authored frame.
        float lw = getLayoutWidth();
        float lh = getLayoutHeight();
        if (!frameOrigin) {
            // Hosted: content is placed by its origin, so the frame is
            // shifted back by it.
            originTransform.set(1, 0, 0, 1, -lw * mOriginX, -lh * mOriginY);
            renderer.transform(originTransform);
        }
        originTransform.set(1, 0, 0, 1, lw * mOriginX, lh * mOriginY);
        if (mClip) {
            bounds.rewind();
            bounds.moveTo(0, 0);
            bounds.lineTo(lw, 0);
            bounds.lineTo(lw, lh);
            bounds.lineTo(0, lh);
            bounds.close();
            renderer.clipPath(bounds);
        }
        if (!backgroundPaints.isEmpty()) {
            bounds.rewind();
            bounds.moveTo(0, 0);
            bounds.lineTo(lw, 0);
            bounds.lineTo(lw, lh);
            bounds.lineTo(0, lh);
            bounds.close();
            for (int i = 0; i < backgroundPaints.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                ShapePaint p = backgroundPaints.get(i);
                if (p.isVisible()) {
                    p.updatePaint(1f);
                    renderer.drawPath(bounds, p.getRenderPaint());
                }
            }
        }
        renderer.transform(originTransform);
        for (int i = drawables.size() - 1; i >= 0; i--) {
            drawables.get(i).draw(renderer);
        }
        renderer.restore();
    }
}
