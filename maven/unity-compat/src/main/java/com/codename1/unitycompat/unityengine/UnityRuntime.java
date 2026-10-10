/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.unitycompat.unityengine;

import UnityEngine.Quaternion;
import UnityEngine.Vector3;
import com.codename1.unitycompat.system.collections.IEnumerator;
import com.codename1.unitycompat.system.collections.generic.List_1;
import com.codename1.unitycompat.unityengine.eventsystems.EventSystem;
import com.codename1.unitycompat.unityengine.ui.Button;
import com.codename1.unitycompat.unityengine.ui.CanvasScaler;
import com.codename1.unitycompat.unityengine.ui.Graphic;
import com.codename1.unitycompat.unityengine.ui.GraphicRaycaster;
import com.codename1.unitycompat.unityengine.ui.Image;
import com.codename1.unitycompat.unityengine.ui.Selectable;
import com.codename1.unitycompat.unityengine.ui.Text;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/// The player loop: what Unity's engine does between a scene being loaded
/// and a frame being drawn, and then [#render], which says what to draw
/// without drawing it.
///
/// #### One frame
///
/// In Unity's order: `Start` for whatever is new, then as many fixed steps
/// as the frame's time covers -- `FixedUpdate`, physics, the collision and
/// trigger messages physics produced -- then `Update`, `Invoke`d methods
/// and coroutines, `LateUpdate`, coroutines waiting for the end of the
/// frame, and finally the objects that asked to be destroyed. `Destroy`
/// only asks: the object is there, and answers, until the frame ends,
/// which scripts rely on.
///
/// Scripts run in the order their objects were created. Unity promises no
/// order between scripts, so a project that depends on one says so in its
/// script execution order settings, which are not read.
///
/// #### Objects come in groups
///
/// A scene, a prefab being instantiated and a copy of a live object are
/// each built whole -- every object, component and reference -- before any
/// of it wakes, so that an `Awake` finds its neighbours. [#$begin] and
/// [#$end] bracket such a group. Outside one, a `new GameObject()` in a
/// script joins the scene at once.
///
/// #### Prefabs
///
/// The scene compiler turns each prefab into a method that builds a fresh
/// copy of it. What a script field holds is a *template*: one such copy,
/// kept out of the scene, never woken and never drawn, so that the script
/// can read it as it would the asset. `Instantiate` of a template runs the
/// method again. `Instantiate` of anything else -- an object already in the
/// scene -- copies it component by component through
/// [Component#$new()] and [Component#$copyFrom(Component)], which every
/// built-in component and every translated script provides; nothing here
/// uses reflection or `Object.clone()`, neither of which a device has.
///
/// #### Code from outside the frame
///
/// Everything above runs inside [#step], on whichever thread the host
/// steps the game from, and nothing here is safe to touch from another.
/// A host draws a frame on a thread of its choosing -- the desktop port on
/// the one that paints -- so a button's listener or a network callback, on
/// the event dispatch thread, may be running beside a frame. Such code
/// hands a `Runnable` to [#callInFrame(Runnable)], and the next frame runs
/// it before any script, where reading a script's field or calling its
/// method is as safe as it is from another script.
///
/// #### Objects that outlive a scene
///
/// `DontDestroyOnLoad` marks a root object. A scene load destroys every
/// root but those, with everything below them, and what was waiting on
/// behalf of a kept script -- a coroutine, an `Invoke` -- goes on waiting.
///
/// World units are Box2D's metres and y points up in both, so positions
/// pass between a [Transform] and a body unchanged.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class UnityRuntime {
    /// What the scene compiler generates for a project.
    public interface App {
        /// Applies the project's settings: physics, time, input, layers.
        void settings();

        int sceneCount();

        /// The path of a scene as the build settings list it.
        String scenePath(int index);

        /// Creates the objects of a scene.
        void buildScene(int index);

        /// Builds a fresh copy of a prefab and returns its root.
        GameObject instantiatePrefab(int index);

        /// A new component of a script type, or null for a type the
        /// project does not define.
        Component newComponent(Class type);

        /// The asset `Resources.Load` finds at a path, or null. `type` is
        /// the class asked for, or null for whatever is there.
        java.lang.Object loadResource(String path, Class type);
    }

    /// `RuntimePlatform.OSXPlayer`.
    public static final int PLATFORM_MAC = 1;
    /// `RuntimePlatform.WindowsPlayer`.
    public static final int PLATFORM_WINDOWS = 2;
    /// `RuntimePlatform.IPhonePlayer`: iOS, a phone or a tablet.
    public static final int PLATFORM_IOS = 8;
    /// `RuntimePlatform.Android`.
    public static final int PLATFORM_ANDROID = 11;
    /// `RuntimePlatform.LinuxPlayer`.
    public static final int PLATFORM_LINUX = 13;
    /// `RuntimePlatform.WebGLPlayer`: a browser.
    public static final int PLATFORM_WEB = 17;

    private static final ArrayList objects = new ArrayList();
    private static final ArrayList behaviours = new ArrayList();
    private static final ArrayList unstarted = new ArrayList();
    private static final ArrayList renderers = new ArrayList();
    private static final ArrayList cameras = new ArrayList();
    private static final ArrayList canvases = new ArrayList();
    /// The components that are advanced once a frame after `Update`: the
    /// animators and the particle systems.
    private static final ArrayList tickers = new ArrayList();
    /// The components that are advanced after `LateUpdate`: what places the
    /// camera, which has to see where everything ended up.
    private static final ArrayList lateTickers = new ArrayList();
    /// The components other than a [SpriteRenderer] that draw in the world.
    private static final ArrayList drawers = new ArrayList();
    private static final DrawView drawView = new DrawView();
    private static final ArrayList eventSystems = new ArrayList();
    /// The control the pointer went down on, the one last pressed, and
    /// the one the pointer is over.
    private static Selectable pressedControl;
    private static Selectable selectedControl;
    private static Selectable overControl;
    private static boolean overInterface;
    private static final ArrayList coroutines = new ArrayList();
    private static final ArrayList invocations = new ArrayList();
    private static final ArrayList doomed = new ArrayList();
    private static final ArrayList delayed = new ArrayList();
    /// Objects created inside a group that has not ended.
    private static final ArrayList pending = new ArrayList();
    private static int depth;
    /// While a live object is being copied: each original, then its copy.
    private static final ArrayList copies = new ArrayList();
    private static App app;
    private static int scene = -1;
    private static int sceneToLoad = -1;
    private static int[] sortingLayers = new int[0];
    private static float accumulator;
    private static boolean running;
    private static boolean listsDirty;
    private static final DrawList drawList = new DrawList();
    private static final Runnable[] NO_CALLS = new Runnable[0];
    /// What other threads asked to have run inside a frame, in order.
    /// Replaced whole and never changed in place, so that the thread that
    /// steps takes it from the thread that asks without a lock.
    private static final AtomicReference calls = new AtomicReference(NO_CALLS);
    /// The object the pointer went down on, which is owed `OnMouseUp`.
    private static GameObject mouseObject;
    private static final Vector3 mousePoint = new Vector3();
    private static final float[] rect = new float[6];

    private static final class Invocation {
        MonoBehaviour owner;
        String method;
        float at;
        float repeat;
        boolean cancelled;
    }

    private static final class Delayed {
        Object target;
        float at;
    }

    private UnityRuntime() {
    }

    /// Forgets everything. A project is installed, or objects are created,
    /// after this and before [#begin].
    public static void reset() {
        objects.clear();
        behaviours.clear();
        unstarted.clear();
        renderers.clear();
        cameras.clear();
        canvases.clear();
        tickers.clear();
        lateTickers.clear();
        drawers.clear();
        eventSystems.clear();
        pressedControl = null;
        selectedControl = null;
        overControl = null;
        overInterface = false;
        coroutines.clear();
        invocations.clear();
        doomed.clear();
        delayed.clear();
        pending.clear();
        copies.clear();
        depth = 0;
        app = null;
        scene = -1;
        sceneToLoad = -1;
        sortingLayers = new int[0];
        accumulator = 0f;
        running = false;
        listsDirty = false;
        mouseObject = null;
        calls.set(NO_CALLS);
        Time.reset();
        Time.fixedDeltaTime = 0.02f;
        Input.reset();
        LayerMask.reset();
        PhysicsWorld.reset();
        ParticleSystem.reset();
        AudioSource.resetAll();
    }

    /// What `Resources.Load` answers.
    public static java.lang.Object $resource(String path, Class type) {
        return app == null || path == null ? null : app.loadResource(path, type);
    }

    /// Makes a compiled project the one that runs, and applies its
    /// settings. [#begin] then loads its first scene.
    public static void $install(App project) {
        app = project;
        project.settings();
    }

    /// The ids of the project's sorting layers, back to front.
    public static void $sortingLayers(int[] ids) {
        int[] copy = new int[ids.length];
        System.arraycopy(ids, 0, copy, 0, ids.length);
        sortingLayers = copy;
    }

    static int sortingLayer(int id) {
        for (int i = 0; i < sortingLayers.length; i++) {
            if (sortingLayers[i] == id) {
                return i;
            }
        }
        return 0;
    }

    /// The fixed timestep of the project's time settings.
    public static void $fixedTimestep(float seconds) {
        if (seconds > 0f) {
            Time.fixedDeltaTime = seconds;
        }
    }

    // ---------------------------------------------------------------- groups

    /// Starts a group of objects that wake together, and returns what
    /// [#$end(int)] needs to end it.
    public static int $begin() {
        depth++;
        return pending.size();
    }

    /// Ends a group: its objects join the scene, in the order they were
    /// created, and then wake.
    public static void $end(int mark) {
        depth--;
        int n = pending.size() - mark;
        if (n <= 0) {
            return;
        }
        GameObject[] group = new GameObject[n];
        for (int i = 0; i < n; i++) {
            group[i] = (GameObject) pending.get(mark + i);
        }
        for (int i = pending.size() - 1; i >= mark; i--) {
            pending.remove(i);
        }
        for (int i = 0; i < n; i++) {
            register(group[i]);
        }
        if (running) {
            for (int i = 0; i < n; i++) {
                wake(group[i]);
            }
        }
    }

    /// Starts the building of a prefab's template.
    public static int $beginAsset() {
        return $begin();
    }

    /// Ends a template: its objects stay out of the scene for good, and
    /// its root remembers which prefab it is.
    public static void $endAsset(int mark, GameObject root, int prefabIndex) {
        depth--;
        for (int i = pending.size() - 1; i >= mark; i--) {
            ((GameObject) pending.remove(i)).asset = true;
        }
        if (root != null) {
            root.prefabIndex = prefabIndex;
        }
    }

    static void created(GameObject go) {
        if (depth > 0) {
            pending.add(go);
            return;
        }
        register(go);
    }

    private static void register(GameObject go) {
        go.registered = true;
        objects.add(go);
        int n = go.components.size();
        for (int i = 0; i < n; i++) {
            Component c = (Component) go.components.get(i);
            c.registered();
            index(go, c);
        }
    }

    private static void index(GameObject go, Component c) {
        if (c instanceof SpriteRenderer) {
            renderers.add(c);
        } else if (c instanceof Camera) {
            cameras.add(c);
        } else if (c instanceof Canvas) {
            canvases.add(c);
        } else if (c instanceof EventSystem) {
            eventSystems.add(c);
        } else if (c instanceof Collider2D || c instanceof Rigidbody2D) {
            PhysicsWorld.changed(go);
        }
        int roles = c.$roles();
        if ((roles & Component.TICKS) != 0) {
            tickers.add(c);
        }
        if ((roles & Component.TICKS_LATE) != 0) {
            lateTickers.add(c);
        }
        if ((roles & Component.DRAWS) != 0) {
            drawers.add(c);
        }
    }

    /// The components advanced after `LateUpdate`, for the one among them
    /// that has to choose between the others: a camera brain and its
    /// virtual cameras. The list is the runtime's own and is not to be
    /// changed.
    public static ArrayList $lateTickers() {
        return lateTickers;
    }

    private static void tick(ArrayList list, boolean late) {
        float dt = Time.deltaTime;
        for (int i = 0; i < list.size(); i++) { // NOPMD ForLoopCanBeForeach
            Component c = (Component) list.get(i);
            if (c.live()) {
                if (late) {
                    c.$lateTick(dt);
                } else {
                    c.$tick(dt);
                }
            }
        }
    }

    /// A component was added to an object that is already in the scene.
    static void attached(GameObject go, Component c) {
        index(go, c);
        if (running && c instanceof MonoBehaviour && go.activeInHierarchy()) {
            wake((MonoBehaviour) c);
        }
    }

    private static void wake(GameObject go) {
        if (go.destroyed || !go.activeInHierarchy()) {
            return;
        }
        java.lang.Object[] all = go.components.toArray(); // NOPMD UnnecessaryFullyQualifiedName
        for (int i = 0; i < all.length; i++) { // NOPMD ForLoopCanBeForeach
            if (all[i] instanceof MonoBehaviour) {
                wake((MonoBehaviour) all[i]);
            }
        }
    }

    /// `Awake`, once, and `OnEnable` if the script is on. An object that
    /// starts inactive wakes when it is first activated, as in Unity.
    private static void wake(MonoBehaviour m) {
        if (m.destroyed) {
            return;
        }
        if (!m.awake) {
            m.awake = true;
            m.$awake();
        }
        if (!m.enabledSeen && !m.destroyed && m.live()) {
            m.enabledSeen = true;
            m.$onEnable();
        }
    }

    static void adopt(MonoBehaviour m) {
        behaviours.add(m);
        unstarted.add(m);
    }

    static void forget(MonoBehaviour m) {
        listsDirty = true;
    }

    /// A script was switched, or its object was.
    static void enabledChanged(MonoBehaviour m, boolean nowLive) {
        if (!running || m.destroyed) {
            return;
        }
        if (nowLive) {
            wake(m);
        } else if (m.enabledSeen) {
            m.enabledSeen = false;
            m.$onDisable();
        }
    }

    /// An object was activated, deactivated or moved to another parent.
    /// `before` holds the behaviours under it that were live until then.
    static void activeChanged(GameObject go, ArrayList before) {
        ArrayList now = new ArrayList();
        GameObject.collectLive(go, now, before);
        for (int i = 0; i < before.size(); i++) { // NOPMD ForLoopCanBeForeach
            Behaviour b = (Behaviour) before.get(i);
            if (!b.live()) {
                b.activeChanged(false);
            }
        }
        for (int i = 0; i < now.size(); i++) { // NOPMD ForLoopCanBeForeach
            Behaviour b = (Behaviour) now.get(i);
            if (b.live()) {
                b.activeChanged(true);
            }
        }
        PhysicsWorld.subtreeChanged(go);
    }

    // ---------------------------------------------------------------- scenes

    /// Loads the project's first scene, if one is installed, and wakes
    /// everything created so far.
    public static void begin() {
        running = true;
        if (app != null && scene < 0 && app.sceneCount() > 0) {
            load(0);
        }
        for (int i = 0; i < objects.size(); i++) { // NOPMD ForLoopCanBeForeach
            wake((GameObject) objects.get(i));
        }
    }

    private static void load(int index) {
        scene = index;
        // Before the scene is built: its scripts' Awake already reads zero.
        Time.levelLoaded();
        int mark = $begin();
        app.buildScene(index);
        $end(mark);
    }

    public static int $sceneCount() {
        return app == null ? 0 : app.sceneCount();
    }

    public static String $scenePath(int index) {
        return app.scenePath(index);
    }

    public static int $activeScene() {
        return scene;
    }

    /// Asks for a scene; it replaces the current one when the frame ends.
    public static void $loadScene(int index) {
        if (app == null || index < 0 || index >= app.sceneCount()) {
            throw new IllegalArgumentException("Scene " + index + " is not in the build settings");
        }
        sceneToLoad = index;
    }

    private static void changeScene() {
        int index = sceneToLoad;
        sceneToLoad = -1;
        java.lang.Object[] all = objects.toArray(); // NOPMD UnnecessaryFullyQualifiedName
        for (int i = 0; i < all.length; i++) { // NOPMD ForLoopCanBeForeach
            GameObject go = (GameObject) all[i];
            if (!go.destroyed && go.transform.parent == null && !go.keptOnLoad) {
                destroyObject(go, true);
            }
        }
        compact();
        // What waited for a script that is gone goes with it; what waits
        // for one that was kept goes on waiting.
        for (int i = coroutines.size() - 1; i >= 0; i--) {
            Coroutine c = (Coroutine) coroutines.get(i);
            if (c.done || c.owner.destroyed) {
                c.done = true;
                coroutines.remove(i);
            }
        }
        for (int i = invocations.size() - 1; i >= 0; i--) {
            Invocation v = (Invocation) invocations.get(i);
            if (v.cancelled || v.owner.destroyed) {
                invocations.remove(i);
            }
        }
        for (int i = delayed.size() - 1; i >= 0; i--) {
            if (((Delayed) delayed.get(i)).target.destroyed) {
                delayed.remove(i);
            }
        }
        doomed.clear();
        PhysicsWorld.flush();
        load(index);
    }

    /// `DontDestroyOnLoad`: keeps a root object, or the object of a
    /// component on one, through every scene load from now on. Asked of
    /// anything below a root it does nothing but say so, as in Unity, where
    /// such an object goes with the root above it.
    static void keepOnLoad(Object target) {
        GameObject go = target instanceof GameObject ? (GameObject) target
                : target instanceof Component ? ((Component) target).gameObject : null;
        if (go == null || go.destroyed) {
            return;
        }
        if (go.transform.parent != null) {
            Debug.LogWarning("DontDestroyOnLoad only works for root GameObjects or components on root GameObjects: "
                    + go.name + " has a parent and was not kept");
            return;
        }
        go.keptOnLoad = true;
    }

    // ------------------------------------------------- from outside the frame

    /// Asks for `call` to be run inside the game's frame: at the start of
    /// the next one, before any script's `Start`, `FixedUpdate` or
    /// `Update`, on the thread that steps the game. Safe from any thread,
    /// the event dispatch thread included, and from inside a frame, where
    /// it means the next one.
    ///
    /// Calls run in the order they were asked for. One that throws ends
    /// the frame as a script that throws does, and the calls after it run
    /// in the frame that follows. While the game is paused no frame runs
    /// and the calls wait.
    ///
    /// The other direction needs nothing of its own: a script, or Java
    /// code it calls, reaches the event dispatch thread with
    /// `CN.callSerially`.
    public static void callInFrame(Runnable call) {
        if (call == null) {
            throw new IllegalArgumentException("callInFrame: no call");
        }
        while (true) {
            Runnable[] before = (Runnable[]) calls.get();
            Runnable[] after = new Runnable[before.length + 1];
            System.arraycopy(before, 0, after, 0, before.length);
            after[before.length] = call;
            if (calls.compareAndSet(before, after)) {
                return;
            }
        }
    }

    /// Runs what [#callInFrame(Runnable)] was given since the last time.
    /// [#step] starts with it; a host that skips a step -- a paused game
    /// -- may call it alone to keep the calls from waiting.
    public static void runFrameCalls() {
        // No more than were waiting when the frame began: a call that asks
        // for another has asked for the next frame, not for this one. Each
        // is taken off before it runs, so one that throws leaves the rest
        // where they were.
        int waiting = ((Runnable[]) calls.get()).length;
        for (int i = 0; i < waiting; i++) {
            Runnable next = null;
            while (next == null) {
                Runnable[] before = (Runnable[]) calls.get();
                if (before.length == 0) {
                    return;
                }
                Runnable[] after = new Runnable[before.length - 1];
                System.arraycopy(before, 1, after, 0, after.length);
                if (calls.compareAndSet(before, after)) {
                    next = before[0];
                }
            }
            next.run();
        }
    }

    /// The application went to the background, or came back. Scripts are
    /// told as Unity tells them on a phone: `OnApplicationFocus(false)`
    /// then `OnApplicationPause(true)` on the way out,
    /// `OnApplicationPause(false)` then `OnApplicationFocus(true)` on the
    /// way back. Called by the host from the thread that steps the game.
    public static void applicationPaused(boolean paused) {
        if (!running) {
            return;
        }
        for (int pass = 0; pass < 2; pass++) {
            boolean focus = (pass == 0) == paused;
            for (int i = 0; i < behaviours.size(); i++) { // NOPMD ForLoopCanBeForeach
                MonoBehaviour b = (MonoBehaviour) behaviours.get(i);
                if (b.destroyed || !b.awake || !b.live()) {
                    continue;
                }
                if (focus) {
                    b.$onApplicationFocus(!paused);
                } else {
                    b.$onApplicationPause(paused);
                }
            }
        }
    }

    public static void resize(int width, int height) {
        Screen.width = width;
        Screen.height = height;
    }

    // ----------------------------------------------------------------- input

    public static void pointerPressed(float x, float y) {
        Input.pointerPressed(x, y);
    }

    public static void pointerReleased(float x, float y) {
        Input.pointerReleased(x, y);
    }

    public static void pointerMoved(float x, float y) {
        Input.pointerMoved(x, y);
    }

    /// Says where the game runs, which is what `Application.platform` and
    /// `Application.isMobilePlatform` answer: one of the `PLATFORM_`
    /// constants, which are the numbers of Unity's `RuntimePlatform`. A host
    /// calls it before [#begin], so that a script's `Awake` already reads it;
    /// [#reset] leaves it alone, because it is a fact about the process and
    /// not about the project. Until a host says, the runtime answers
    /// [#PLATFORM_LINUX]: it has no display to ask and is most often a
    /// headless run, and a desktop player is the answer under which a script
    /// expects neither a touch screen nor an editor.
    public static void platform(int runtimePlatform) {
        Application.platform(runtimePlatform);
    }

    /// Says whether the device has a touch screen, which is what
    /// `Input.touchSupported` answers.
    public static void touchSupported(boolean supported) {
        Input.touchSupported(supported);
    }

    /// Where every finger on the screen now is, in Unity's screen
    /// coordinates -- pixels, y upwards from the bottom: the first `count`
    /// entries of `x` and `y`, and zero for none. A host calls this each
    /// time a finger lands, moves or lifts, and need not say which did;
    /// [Input] tells them apart. The first finger is the mouse too, and is
    /// reported through [#pointerPressed] and its siblings as before.
    public static void touches(float[] x, float[] y, int count) {
        Input.touches(x, y, count);
    }

    /// A key went down. `unityKeyCode` is a value of Unity's `KeyCode`,
    /// which for a printable key is its lower-case ASCII code.
    public static void keyPressed(int unityKeyCode) {
        Input.keyPressed(unityKeyCode);
    }

    public static void keyReleased(int unityKeyCode) {
        Input.keyReleased(unityKeyCode);
    }

    /// True once a script has called `Application.Quit()`.
    public static boolean quitRequested() {
        return Application.quitRequested();
    }

    // --------------------------------------------------------------- finding

    static Camera camera(boolean tagged) {
        int n = cameras.size();
        for (int i = 0; i < n; i++) {
            Camera c = (Camera) cameras.get(i);
            if (c.live() && (!tagged || "MainCamera".equals(c.gameObject.tag))) {
                return c;
            }
        }
        return null;
    }

    /// The first active object with this name or this tag; with `out`,
    /// all of them.
    ///
    /// A name with a `/` in it is a path, as `GameObject.Find` documents:
    /// `Canvas/Panel/Button` is a `Button` whose parent is a `Panel` whose
    /// parent is a `Canvas`, wherever that is, and a leading `/` asks for
    /// the first of them to have no parent. Only the object found has to be
    /// active, which says the same of everything above it.
    static GameObject find(String name, String tag, ArrayList out) {
        int n = objects.size();
        boolean path = name != null && name.indexOf('/') >= 0;
        for (int i = 0; i < n; i++) {
            GameObject go = (GameObject) objects.get(i);
            if (go.destroyed || !go.activeInHierarchy()) {
                continue;
            }
            if ((name != null && !go.name.equals(name) && !(path && atPath(go.transform, name)))
                    || (tag != null && !go.tag.equals(tag))) {
                continue;
            }
            if (out == null) {
                return go;
            }
            out.add(go);
        }
        return null;
    }

    /// Whether an object is where a path says: read from its last name
    /// back to its first, up the parents, so that two objects of one name
    /// with different children are told apart and nothing is allocated.
    private static boolean atPath(Transform t, String path) {
        int end = path.length();
        Transform at = t;
        while (at != null) {
            int slash = path.lastIndexOf('/', end - 1);
            int length = end - slash - 1;
            String name = at.gameObject.name;
            if (name.length() != length || !path.regionMatches(slash + 1, name, 0, length)) {
                return false;
            }
            if (slash <= 0) {
                // The whole path is matched; a leading `/` wants a root.
                return slash < 0 || at.parent == null;
            }
            end = slash;
            at = at.parent;
        }
        return false;
    }

    /// The first component of a type on an active object; with `out`, all
    /// of them. A script that is switched off is found, an object that is
    /// inactive is not, as Unity's documentation says.
    static java.lang.Object findOfType(Class type, ArrayList out) {
        int n = objects.size();
        boolean wantObjects = type == GameObject.class;
        for (int i = 0; i < n; i++) {
            GameObject go = (GameObject) objects.get(i);
            if (go.destroyed || !go.activeInHierarchy()) {
                continue;
            }
            if (wantObjects) {
                if (out == null) {
                    return go;
                }
                out.add(go);
                continue;
            }
            int m = go.components.size();
            for (int j = 0; j < m; j++) {
                java.lang.Object c = go.components.get(j);
                if (type.isInstance(c)) {
                    if (out == null) {
                        return c;
                    }
                    out.add(c);
                }
            }
        }
        return null;
    }

    /// How many objects are in the scene, for a test or a trace.
    public static int $objectCount() {
        int n = 0;
        for (int i = 0; i < objects.size(); i++) { // NOPMD ForLoopCanBeForeach
            if (!((GameObject) objects.get(i)).destroyed) {
                n++;
            }
        }
        return n;
    }

    /// How many objects in the scene have a name that starts this way.
    public static int $objectCount(String namePrefix) {
        int n = 0;
        for (int i = 0; i < objects.size(); i++) { // NOPMD ForLoopCanBeForeach
            GameObject go = (GameObject) objects.get(i);
            if (!go.destroyed && go.name.startsWith(namePrefix)) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------- creating

    static Component newComponent(Class type) {
        if (type == Animator.class) {
            return new Animator();
        }
        if (type == ParticleSystem.class) {
            return new ParticleSystem();
        }
        if (type == ParticleSystemRenderer.class) {
            return new ParticleSystemRenderer();
        }
        if (type == PlatformEffector2D.class) {
            return new PlatformEffector2D();
        }
        if (type == CompositeCollider2D.class) {
            return new CompositeCollider2D();
        }
        if (type == Grid.class) {
            return new Grid();
        }
        if (type == com.codename1.unitycompat.unityengine.tilemaps.Tilemap.class) {
            return new com.codename1.unitycompat.unityengine.tilemaps.Tilemap();
        }
        if (type == com.codename1.unitycompat.unityengine.tilemaps.TilemapRenderer.class) {
            return new com.codename1.unitycompat.unityengine.tilemaps.TilemapRenderer();
        }
        if (type == com.codename1.unitycompat.unityengine.tilemaps.TilemapCollider2D.class) {
            return new com.codename1.unitycompat.unityengine.tilemaps.TilemapCollider2D();
        }
        if (type == com.codename1.unitycompat.tmpro.TextMeshProUGUI.class) {
            return new com.codename1.unitycompat.tmpro.TextMeshProUGUI();
        }
        if (type == com.codename1.unitycompat.tmpro.TextMeshPro.class) {
            return new com.codename1.unitycompat.tmpro.TextMeshPro();
        }
        if (type == com.codename1.unitycompat.cinemachine.CinemachineVirtualCamera.class) {
            return new com.codename1.unitycompat.cinemachine.CinemachineVirtualCamera();
        }
        if (type == com.codename1.unitycompat.cinemachine.CinemachineBrain.class) {
            return new com.codename1.unitycompat.cinemachine.CinemachineBrain();
        }
        if (type == com.codename1.unitycompat.cinemachine.CinemachineFramingTransposer.class) {
            return new com.codename1.unitycompat.cinemachine.CinemachineFramingTransposer();
        }
        if (type == com.codename1.unitycompat.cinemachine.CinemachineTransposer.class) {
            return new com.codename1.unitycompat.cinemachine.CinemachineTransposer();
        }
        if (type == Rigidbody2D.class) {
            return new Rigidbody2D();
        }
        if (type == BoxCollider2D.class) {
            return new BoxCollider2D();
        }
        if (type == CircleCollider2D.class) {
            return new CircleCollider2D();
        }
        if (type == PolygonCollider2D.class) {
            return new PolygonCollider2D();
        }
        if (type == SpriteRenderer.class) {
            return new SpriteRenderer();
        }
        if (type == Camera.class) {
            return new Camera();
        }
        if (type == Canvas.class) {
            return new Canvas();
        }
        if (type == CanvasRenderer.class) {
            return new CanvasRenderer();
        }
        if (type == AudioListener.class) {
            return new AudioListener();
        }
        if (type == Text.class) {
            return new Text();
        }
        if (type == CanvasScaler.class) {
            return new CanvasScaler();
        }
        if (type == Image.class) {
            return new Image();
        }
        if (type == Button.class) {
            return new Button();
        }
        if (type == Selectable.class) {
            return new Selectable();
        }
        if (type == GraphicRaycaster.class) {
            return new GraphicRaycaster();
        }
        if (type == EventSystem.class) {
            return new EventSystem();
        }
        return app == null ? null : app.newComponent(type);
    }

    /// While a live object is being copied, the copy of `original` if it
    /// is part of what is being copied; otherwise `original` itself. A
    /// `$copyFrom` passes every reference to a Unity object through this,
    /// so that a reference inside the copied tree points inside the copy
    /// and one that leads out of it still leads to the same place.
    public static java.lang.Object $remap(java.lang.Object original) {
        for (int i = 0; i + 1 < copies.size(); i += 2) {
            if (copies.get(i) == original) { // NOPMD CompareObjectsWithEquals
                return copies.get(i + 1);
            }
        }
        return original;
    }

    /// A copy of an array a script field holds, for a `$copyFrom`: the
    /// same elements in a new array, with references to Unity objects
    /// redirected as [#$remap(java.lang.Object)] does.
    public static java.lang.Object $copyArray(java.lang.Object array) {
        if (array instanceof java.lang.Object[]) { // NOPMD UnnecessaryFullyQualifiedName
            java.lang.Object[] from = (java.lang.Object[]) array; // NOPMD UnnecessaryFullyQualifiedName
            java.lang.Object[] to = (java.lang.Object[]) from.clone(); // NOPMD UnnecessaryFullyQualifiedName
            for (int i = 0; i < to.length; i++) {
                if (to[i] instanceof Object) {
                    to[i] = $remap(to[i]);
                }
            }
            return to;
        }
        if (array instanceof int[]) {
            return ((int[]) array).clone();
        }
        if (array instanceof float[]) {
            return ((float[]) array).clone();
        }
        if (array instanceof boolean[]) {
            return ((boolean[]) array).clone();
        }
        if (array instanceof double[]) {
            return ((double[]) array).clone();
        }
        if (array instanceof long[]) {
            return ((long[]) array).clone();
        }
        if (array instanceof byte[]) {
            return ((byte[]) array).clone();
        }
        if (array instanceof char[]) {
            return ((char[]) array).clone();
        }
        if (array instanceof short[]) {
            return ((short[]) array).clone();
        }
        return array;
    }

    /// A copy of a `List<T>` a script field holds, likewise.
    public static java.lang.Object $copyList(java.lang.Object list) {
        if (!(list instanceof List_1)) {
            return list;
        }
        List_1 from = (List_1) list;
        int n = from.get_Count();
        List_1 to = new List_1(n);
        for (int i = 0; i < n; i++) {
            java.lang.Object item = from.get_Item(i);
            to.Add(item instanceof Object ? $remap(item) : item);
        }
        return to;
    }

    private static GameObject copyTree(GameObject source, ArrayList pairs) {
        Component t = source.transform.$new();
        GameObject copy = new GameObject(source.name, (Transform) t);
        copy.active = source.active;
        copy.tag = source.tag;
        copy.layer = source.layer;
        copies.add(source);
        copies.add(copy);
        copies.add(source.transform);
        copies.add(copy.transform);
        pairs.add(source.transform);
        pairs.add(copy.transform);
        int n = source.components.size();
        for (int i = 1; i < n; i++) {
            Component c = (Component) source.components.get(i);
            Component made = c.$new();
            if (made == null) {
                Debug.LogWarning("Instantiate: a " + c.getClass().getName() + " cannot be copied and was left out");
                continue;
            }
            copy.$attach(made);
            copies.add(c);
            copies.add(made);
            pairs.add(c);
            pairs.add(made);
        }
        ArrayList children = source.transform.children;
        if (children != null) {
            for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach
                GameObject child = copyTree(((Transform) children.get(i)).gameObject, pairs);
                child.transform.$parent(copy.transform);
            }
        }
        return copy;
    }

    /// Where a component is in its tree, as the indexes of the children to
    /// go down through from the root, last first, then which component.
    private static java.lang.Object samePlace(GameObject sourceRoot, GameObject copyRoot, Object original) {
        if (original instanceof GameObject && original == sourceRoot) { // NOPMD CompareObjectsWithEquals
            return copyRoot;
        }
        GameObject at = original instanceof GameObject ? (GameObject) original : ((Component) original).gameObject;
        ArrayList path = new ArrayList();
        Transform t = at.transform;
        while (t.gameObject != sourceRoot && t.parent != null) { // NOPMD CompareObjectsWithEquals
            path.add(Integer.valueOf(t.parent.children.indexOf(t)));
            t = t.parent;
        }
        Transform into = copyRoot.transform;
        for (int i = path.size() - 1; i >= 0; i--) {
            into = (Transform) into.children.get(((Integer) path.get(i)).intValue());
        }
        if (original instanceof GameObject) {
            return into.gameObject;
        }
        return into.gameObject.components.get(at.components.indexOf(original));
    }

    static Object instantiate(Object original, Transform parent, boolean hasParent, Vector3 position,
            Quaternion rotation, boolean worldSpace) {
        if (original == null || original.destroyed) {
            throw new IllegalArgumentException("Instantiate: the object to copy is null or was destroyed");
        }
        if (!(original instanceof GameObject) && !(original instanceof Component)) {
            throw new IllegalArgumentException("Instantiate: only objects and components can be copied");
        }
        GameObject source = original instanceof GameObject ? (GameObject) original
                : ((Component) original).gameObject;
        GameObject top = source;
        while (top.transform.parent != null) {
            top = top.transform.parent.gameObject;
        }
        int mark = $begin();
        GameObject copy;
        Object result;
        if (top.asset && top.prefabIndex >= 0 && app != null && source == top) { // NOPMD CompareObjectsWithEquals
            copy = app.instantiatePrefab(top.prefabIndex);
            result = (Object) samePlace(top, copy, original);
        } else {
            copies.clear();
            ArrayList pairs = new ArrayList();
            copy = copyTree(source, pairs);
            for (int i = 0; i + 1 < pairs.size(); i += 2) {
                ((Component) pairs.get(i + 1)).$copyFrom((Component) pairs.get(i));
            }
            result = (Object) $remap(original);
            copies.clear();
        }
        copy.name = source.name + "(Clone)";
        Transform t = copy.transform;
        if (hasParent && parent != null) {
            t.SetParent(parent, position == null && worldSpace);
        }
        if (position != null) {
            t.setWorldPosition(position.x, position.y, position.z);
            t.setWorldRotation(Transform.zAngle(rotation));
        }
        $end(mark);
        return result;
    }

    // ----------------------------------------------------------- destroying

    static void destroyLater(Object obj, float delay) {
        if (obj.destroyed) {
            return;
        }
        if (delay > 0f) {
            Delayed d = new Delayed();
            d.target = obj;
            d.at = Time.time + delay;
            delayed.add(d);
            return;
        }
        if (!obj.doomed) {
            obj.doomed = true;
            doomed.add(obj);
        }
    }

    static void destroyNow(Object obj) {
        if (obj.destroyed) {
            return;
        }
        if (obj instanceof GameObject) {
            destroyObject((GameObject) obj, true);
        } else if (obj instanceof Component) {
            destroyComponent((Component) obj);
        } else {
            obj.destroyed = true;
        }
    }

    private static void retire(MonoBehaviour m) {
        if (m.enabledSeen) {
            m.enabledSeen = false;
            m.$onDisable();
        }
        if (m.awake) {
            m.$onDestroy();
        }
    }

    private static void destroyObject(GameObject go, boolean detach) {
        if (go.destroyed) {
            return;
        }
        ArrayList children = go.transform.children;
        if (children != null) {
            java.lang.Object[] below = children.toArray(); // NOPMD UnnecessaryFullyQualifiedName
            for (int i = 0; i < below.length; i++) { // NOPMD ForLoopCanBeForeach
                destroyObject(((Transform) below[i]).gameObject, false);
            }
        }
        java.lang.Object[] all = go.components.toArray(); // NOPMD UnnecessaryFullyQualifiedName
        if (go.registered && running) {
            for (int i = 0; i < all.length; i++) { // NOPMD ForLoopCanBeForeach
                if (all[i] instanceof MonoBehaviour && !((MonoBehaviour) all[i]).destroyed) {
                    retire((MonoBehaviour) all[i]);
                }
            }
        }
        go.destroyed = true;
        for (int i = 0; i < all.length; i++) { // NOPMD ForLoopCanBeForeach
            Component c = (Component) all[i];
            c.destroyed = true;
            c.unregistered();
        }
        Transform parent = go.transform.parent;
        if (detach && parent != null && parent.children != null) {
            parent.children.remove(go.transform);
        }
        if (go.registered) {
            PhysicsWorld.removed(go);
        }
        listsDirty = true;
    }

    private static void destroyComponent(Component c) {
        if (c.destroyed || c instanceof Transform) {
            // A transform goes only with its object.
            return;
        }
        GameObject go = c.gameObject;
        boolean inScene = go != null && go.registered;
        if (inScene && running && c instanceof MonoBehaviour) {
            retire((MonoBehaviour) c);
        }
        c.destroyed = true;
        c.unregistered();
        if (go != null) {
            go.components.remove(c);
        }
        if (inScene && c instanceof Collider2D) {
            GameObject owner = ((Collider2D) c).owner;
            PhysicsWorld.changed(owner != null ? owner : go);
        } else if (inScene && c instanceof Rigidbody2D) {
            PhysicsWorld.subtreeChanged(go);
            PhysicsWorld.changed(go);
        }
        listsDirty = true;
    }

    private static void flushDestroyed() {
        for (int i = 0; i < delayed.size(); i++) {
            Delayed d = (Delayed) delayed.get(i);
            if (Time.time >= d.at) {
                delayed.remove(i--);
                doomed.add(d.target);
            }
        }
        // A script's OnDestroy may destroy more.
        while (!doomed.isEmpty()) {
            java.lang.Object[] batch = doomed.toArray(); // NOPMD UnnecessaryFullyQualifiedName
            doomed.clear();
            for (int i = 0; i < batch.length; i++) { // NOPMD ForLoopCanBeForeach
                destroyNow((Object) batch[i]);
            }
        }
        if (listsDirty) {
            compact();
        }
    }

    private static void compact(ArrayList list) {
        int kept = 0;
        int n = list.size();
        for (int i = 0; i < n; i++) {
            Object o = (Object) list.get(i);
            if (!o.destroyed) {
                if (kept != i) {
                    list.set(kept, o);
                }
                kept++;
            }
        }
        for (int i = n - 1; i >= kept; i--) {
            list.remove(i);
        }
    }

    private static void compact() {
        listsDirty = false;
        compact(objects);
        compact(behaviours);
        compact(unstarted);
        compact(renderers);
        compact(cameras);
        compact(canvases);
        compact(tickers);
        compact(lateTickers);
        compact(drawers);
        compact(eventSystems);
    }

    // --------------------------------------------------------------- frames

    private static void runStarts() {
        if (unstarted.isEmpty()) {
            return;
        }
        // A Start may create objects, whose scripts join the end of the
        // list and start in this same pass.
        int kept = 0;
        for (int i = 0; i < unstarted.size(); i++) { // NOPMD ForLoopCanBeForeach
            MonoBehaviour m = (MonoBehaviour) unstarted.get(i);
            if (m.destroyed || m.started) {
                continue;
            }
            if (m.awake && m.live()) {
                m.started = true;
                m.$start();
            } else {
                unstarted.set(kept++, m);
            }
        }
        for (int i = unstarted.size() - 1; i >= kept; i--) {
            unstarted.remove(i);
        }
    }

    /// Runs one frame of `dt` seconds.
    public static void step(float dt) {
        Time.unscaledDeltaTime = dt;
        Time.unscaledTime += dt;
        float scaled = dt * Time.timeScale;
        Time.deltaTime = scaled;
        Time.frameCount++;
        Input.beginFrame(dt);
        runFrameCalls();
        runStarts();
        routePointer();
        routeMouse();
        accumulator += scaled;
        float fixed = Time.fixedDeltaTime;
        while (accumulator >= fixed) {
            accumulator -= fixed;
            Time.fixedTime += fixed;
            // A step is `fixed` seconds of game time, which the scale
            // stretched out of this much real time. No step runs at a scale
            // of zero, since nothing reaches the accumulator then; the
            // check is for a step left over from before the scale dropped.
            Time.fixedUnscaledDeltaTime = Time.timeScale > 0f ? fixed / Time.timeScale : fixed;
            Time.fixedUnscaledTime += Time.fixedUnscaledDeltaTime;
            Time.inFixedUpdate = true;
            // Indexed, not iterated: a script may add a behaviour while
            // these run.
            for (int i = 0; i < behaviours.size(); i++) { // NOPMD ForLoopCanBeForeach
                MonoBehaviour b = (MonoBehaviour) behaviours.get(i);
                if (b.started && b.live()) {
                    b.$fixedUpdate();
                }
            }
            PhysicsWorld.step(fixed);
            PhysicsWorld.dispatch();
            runCoroutines(1);
            Time.inFixedUpdate = false;
            runStarts();
        }
        Time.time += scaled;
        for (int i = 0; i < behaviours.size(); i++) { // NOPMD ForLoopCanBeForeach
            MonoBehaviour b = (MonoBehaviour) behaviours.get(i);
            if (b.started && b.live()) {
                b.$update();
            }
        }
        runInvocations();
        runCoroutines(0);
        tick(tickers, false);
        for (int i = 0; i < behaviours.size(); i++) { // NOPMD ForLoopCanBeForeach
            MonoBehaviour b = (MonoBehaviour) behaviours.get(i);
            if (b.started && b.live()) {
                b.$lateUpdate();
            }
        }
        tick(lateTickers, true);
        runCoroutines(2);
        flushDestroyed();
        if (sceneToLoad >= 0) {
            changeScene();
        }
    }

    // -------------------------------------------------------------- pointer

    /// The event system in charge, or null: the first that is active.
    public static EventSystem $eventSystem() {
        for (int i = 0; i < eventSystems.size(); i++) { // NOPMD ForLoopCanBeForeach
            EventSystem e = (EventSystem) eventSystems.get(i);
            if (!gone(e) && ((Behaviour) e).live()) {
                return e;
            }
        }
        return null;
    }

    // A class of another package cannot be asked for these through its
    // own type, only through the type that declares them.
    private static boolean gone(Object o) {
        return o.destroyed;
    }

    private static GameObject owner(Component c) {
        return c.gameObject;
    }

    public static boolean $pointerOverInterface() {
        return overInterface;
    }

    public static GameObject $selectedObject() {
        return selectedControl == null || gone(selectedControl) ? null : owner(selectedControl);
    }

    public static void $select(GameObject go) {
        Selectable before = selectedControl;
        selectedControl = go == null ? null : control(go);
        show(before);
        show(selectedControl);
    }

    /// The control an event on `go` belongs to: the nearest at or above
    /// it. That is what makes a click on a button's label a click on the
    /// button.
    private static Selectable control(GameObject go) {
        Transform t = go.transform;
        while (t != null) {
            ArrayList all = t.gameObject.components;
            for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach
                java.lang.Object c = all.get(i);
                if (c instanceof Selectable && ((Behaviour) c).live()) {
                    return (Selectable) c;
                }
            }
            t = t.parent;
        }
        return null;
    }

    /// The graphic of `go` or below it that a pointer at a point of the
    /// canvas lands on. What is drawn last is on top, so the search runs
    /// against the order of drawing: last child first, children before
    /// the object itself.
    private static Graphic hit(GameObject go, float canvasWidth, float canvasHeight, float x, float y) {
        ArrayList children = go.transform.children;
        if (children != null) {
            for (int i = children.size() - 1; i >= 0; i--) {
                GameObject child = ((Transform) children.get(i)).gameObject;
                if (child.active && !child.destroyed) {
                    Graphic found = hit(child, canvasWidth, canvasHeight, x, y);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        for (int i = go.components.size() - 1; i >= 0; i--) {
            java.lang.Object c = go.components.get(i);
            if (!(c instanceof Graphic) || !((Behaviour) c).live() || !((Graphic) c).get_raycastTarget()) {
                continue;
            }
            Transform t = go.transform;
            if (!(t instanceof RectTransform)) {
                continue;
            }
            ((RectTransform) t).$rect(canvasWidth, canvasHeight, rect);
            if (x >= rect[0] && y >= rect[1] && x < rect[0] + rect[2] && y < rect[1] + rect[3]) {
                return (Graphic) c;
            }
        }
        return null;
    }

    private static Graphic pointerGraphic() {
        Graphic best = null;
        int bestOrder = 0;
        for (int i = 0; i < canvases.size(); i++) { // NOPMD ForLoopCanBeForeach
            Canvas canvas = (Canvas) canvases.get(i);
            if (canvas.destroyed || !canvas.live() || canvas.renderMode == 2
                    || canvas.gameObject.GetComponent(GraphicRaycaster.class) == null
                    || (canvas.gameObject.transform.parent != null
                    && canvas.gameObject.transform.parent.gameObject.GetComponentInParent(Canvas.class) != null)) {
                continue;
            }
            if (best != null && canvas.sortingOrder < bestOrder) {
                continue;
            }
            float s = canvasScale(canvas);
            Graphic found = hit(canvas.gameObject, Screen.width / s, Screen.height / s, Input.mouseX / s,
                    Input.mouseY / s);
            if (found != null) {
                best = found;
                bestOrder = canvas.sortingOrder;
            }
        }
        return best;
    }

    private static void show(Selectable control) {
        if (control != null && !gone(control)) {
            control.$pointer(control == pressedControl && control == overControl, control == selectedControl, // NOPMD CompareObjectsWithEquals
                    control == overControl); // NOPMD CompareObjectsWithEquals
        }
    }

    /// Turns the frame's pointer input into what the controls see. Runs
    /// before any script's `Update`, and only while the scene has an
    /// active event system, as in Unity.
    private static void routePointer() {
        if (eventSystems.isEmpty()) {
            return;
        }
        Selectable wasPressed = pressedControl;
        Selectable wasSelected = selectedControl;
        Selectable wasOver = overControl;
        Selectable clicked = null;
        if ($eventSystem() == null) {
            pressedControl = null;
            overControl = null;
            overInterface = false;
        } else {
            Graphic under = pointerGraphic();
            overInterface = under != null;
            overControl = under == null ? null : control(owner(under));
            if (Input.GetMouseButtonDown(0)) {
                pressedControl = overControl;
                selectedControl = overControl != null && overControl.IsInteractable() ? overControl : null;
            }
            if (Input.GetMouseButtonUp(0)) {
                if (pressedControl != null && pressedControl == overControl) { // NOPMD CompareObjectsWithEquals
                    clicked = pressedControl;
                }
                pressedControl = null;
            }
        }
        if (selectedControl != null && gone(selectedControl)) { // NOPMD NonThreadSafeSingleton
            selectedControl = null;
        }
        if (wasPressed != pressedControl || wasSelected != selectedControl || wasOver != overControl) { // NOPMD CompareObjectsWithEquals
            show(wasPressed);
            show(wasSelected);
            show(wasOver);
            show(pressedControl);
            show(selectedControl);
            show(overControl);
        }
        if (clicked != null && !gone(clicked)) {
            clicked.$click();
        }
    }

    /// `OnMouseDown` and `OnMouseUp`: the scripts of the object whose 2D
    /// collider the pointer went down on are told so, and told again when
    /// it comes up, wherever it is by then. The pointer is placed in the
    /// world by the main camera, and of several colliders at that point
    /// the one nearest the camera has it. A press on a control of a canvas
    /// belongs to the control and reaches no collider.
    private static void routeMouse() {
        if (Input.GetMouseButtonDown(0)) {
            mouseObject = null;
            Camera camera = overInterface ? null : camera(true);
            if (camera != null) {
                mousePoint.x = Input.mouseX;
                mousePoint.y = Input.mouseY;
                mousePoint.z = 0f;
                camera.ScreenToWorldPoint(mousePoint, mousePoint);
                Collider2D under = PhysicsWorld.at(mousePoint.x, mousePoint.y);
                if (under != null) {
                    mouseObject = under.gameObject;
                    sendMouse(mouseObject, true);
                }
            }
        }
        if (Input.GetMouseButtonUp(0) && mouseObject != null) {
            GameObject target = mouseObject;
            mouseObject = null;
            sendMouse(target, false);
        }
    }

    private static void sendMouse(GameObject go, boolean down) {
        if (go.destroyed || !go.activeInHierarchy()) {
            return;
        }
        java.lang.Object[] all = go.components.toArray(); // NOPMD UnnecessaryFullyQualifiedName
        for (int i = 0; i < all.length; i++) { // NOPMD ForLoopCanBeForeach
            if (!(all[i] instanceof MonoBehaviour)) {
                continue;
            }
            MonoBehaviour m = (MonoBehaviour) all[i];
            if (m.destroyed || !m.awake) {
                continue;
            }
            if (down) {
                m.$onMouseDown();
            } else {
                m.$onMouseUp();
            }
        }
    }

    private static float canvasScale(Canvas canvas) {
        java.lang.Object scaler = canvas.gameObject.GetComponent(CanvasScaler.class);
        if (scaler != null) {
            canvas.scaleFactor = ((CanvasScaler) scaler).$scale(Screen.width, Screen.height);
        }
        return canvas.scaleFactor > 0f ? canvas.scaleFactor : 1f;
    }

    // ------------------------------------------------------------ coroutines

    /// How many nested enumerators one pass of a coroutine may run to their
    /// end without any of them yielding, before it is made to wait a frame.
    /// See `advance`.
    private static final int EMPTY_CHILDREN = 1000000;

    static Coroutine startCoroutine(MonoBehaviour owner, IEnumerator routine) {
        Coroutine c = new Coroutine();
        c.owner = owner;
        c.routine = routine;
        // Unity runs a coroutine up to its first yield before StartCoroutine
        // returns.
        advance(c);
        if (!c.done) {
            coroutines.add(c);
        }
        return c;
    }

    static void stopCoroutines(MonoBehaviour owner) {
        for (int i = 0; i < coroutines.size(); i++) { // NOPMD ForLoopCanBeForeach
            Coroutine c = (Coroutine) coroutines.get(i);
            if (c.owner == owner) { // NOPMD CompareObjectsWithEquals
                c.done = true;
            }
        }
    }

    /// Steps a coroutine to the next thing it waits for.
    ///
    /// A routine that yields an enumerator -- `yield return Child()`, with
    /// no `StartCoroutine` -- waits for all of it, as Unity has it: the
    /// child is stepped in the parent's place, whatever the child waits for
    /// is what the coroutine waits for, and the parent goes on from the
    /// point the child ends at, in that same pass. They are one coroutine,
    /// so stopping it, or deactivating its object, stops the child too.
    ///
    /// A child that ends without having yielded once costs nothing: Unity
    /// runs a nested enumerator at once, so the parent goes on in the same
    /// frame, and a routine that calls ten such children in a row has run
    /// them all before `StartCoroutine` returns.
    ///
    /// That makes `while (true) { yield return Empty(); }` a loop that never
    /// reaches the end of its frame, which is what it is in Unity too, where
    /// it stops the player for good. Here it would be a device that stopped
    /// answering with nothing to say why. So a pass that has entered
    /// `EMPTY_CHILDREN` nested enumerators without one thing to wait for
    /// makes the coroutine wait for the next frame, and the log says so,
    /// once for a coroutine. Any yield of something else ends the pass, so only such a loop
    /// gets there; and the count is of children and not of time, so it is
    /// the same on every target.
    private static void advance(Coroutine c) {
        int children = 0;
        for (;;) {
            boolean more = c.routine.MoveNext();
            if (c.done) {
                // It stopped itself from inside the step.
                return;
            }
            if (more) {
                java.lang.Object yielded = c.routine.get_Current();
                if (!(yielded instanceof IEnumerator)) {
                    waitFor(c, yielded);
                    return;
                }
                children++;
                if (children == EMPTY_CHILDREN) {
                    // Not stepped: it is the next thing the coroutine runs.
                    if (!c.spun) {
                        c.spun = true;
                        String of = c.owner == null ? "no script" : c.owner.get_name();
                        Debug.LogError("A coroutine of " + of + " ran " + EMPTY_CHILDREN + " nested enumerators in"
                                + " one frame and none of them yielded; it goes on in the next frame.");
                    }
                    push(c, (IEnumerator) yielded);
                    waitFor(c, null);
                    return;
                }
                push(c, (IEnumerator) yielded);
                continue;
            }
            if (c.depth == 0) {
                c.done = true;
                return;
            }
            c.routine = c.outer[--c.depth];
            c.outer[c.depth] = null;
        }
    }

    /// Makes `child` the enumerator a coroutine steps, and keeps the one it
    /// was stepping to go back to.
    private static void push(Coroutine c, IEnumerator child) {
        if (c.depth == c.outer.length) {
            IEnumerator[] grown = new IEnumerator[Math.max(4, c.depth * 2)];
            System.arraycopy(c.outer, 0, grown, 0, c.depth);
            c.outer = grown;
        }
        c.outer[c.depth++] = c.routine;
        c.routine = child;
    }

    private static void waitFor(Coroutine c, java.lang.Object yielded) { // NOPMD UnnecessaryFullyQualifiedName
        c.waitingFor = null;
        c.resumeAt = 0f;
        c.phase = 0;
        // Whatever it waits for after `Update`, it waits at least for the
        // `Update` of another frame. Unity documents `yield return null` as
        // resuming "after all Update functions have been called on the next
        // frame"; without this a coroutine started from `Start` or `Update`
        // ran to its first yield and was resumed by this same frame's pass,
        // a few lines further down in [#step]. The two other phases are left
        // alone: the end of the frame a coroutine was started in is the one
        // `WaitForEndOfFrame` means.
        c.notBefore = Time.frameCount + 1;
        if (yielded instanceof WaitForSeconds) {
            c.resumeAt = Time.time + ((WaitForSeconds) yielded).seconds;
        } else if (yielded instanceof Coroutine) {
            c.waitingFor = (Coroutine) yielded;
        } else if (yielded instanceof WaitForFixedUpdate) {
            c.phase = 1;
        } else if (yielded instanceof WaitForEndOfFrame) {
            c.phase = 2;
        }
        // Anything else, null included, means the next frame.
    }

    /// Resumes the coroutines waiting for this point of the frame: 0 after
    /// `Update`, 1 after a fixed step, 2 at the end of the frame. Unity
    /// stops a coroutine when its object is deactivated or destroyed, and
    /// not when its script is merely switched off.
    private static void runCoroutines(int phase) {
        int n = coroutines.size();
        if (n == 0) {
            return;
        }
        boolean finished = false;
        for (int i = 0; i < n; i++) {
            Coroutine c = (Coroutine) coroutines.get(i);
            if (!c.done && (c.owner.destroyed || !c.owner.gameObject.activeInHierarchy())) {
                c.done = true;
            }
            if (c.done) {
                finished = true;
                continue;
            }
            if (c.phase != phase || (c.waitingFor != null && !c.waitingFor.done) || Time.time < c.resumeAt
                    || (phase == 0 && Time.frameCount < c.notBefore)) {
                continue;
            }
            advance(c);
            finished |= c.done;
        }
        if (finished) {
            for (int i = coroutines.size() - 1; i >= 0; i--) {
                if (((Coroutine) coroutines.get(i)).done) {
                    coroutines.remove(i);
                }
            }
        }
    }

    // --------------------------------------------------------------- Invoke

    static void invokeLater(MonoBehaviour owner, String method, float time, float repeat) {
        Invocation v = new Invocation();
        v.owner = owner;
        v.method = method;
        v.at = Time.time + (time > 0f ? time : 0f);
        v.repeat = repeat;
        invocations.add(v);
    }

    static void cancelInvoke(MonoBehaviour owner, String method) {
        for (int i = 0; i < invocations.size(); i++) { // NOPMD ForLoopCanBeForeach
            Invocation v = (Invocation) invocations.get(i);
            if (v.owner == owner && (method == null || method.equals(v.method))) { // NOPMD CompareObjectsWithEquals
                v.cancelled = true;
            }
        }
    }

    static boolean isInvoking(MonoBehaviour owner, String method) {
        for (int i = 0; i < invocations.size(); i++) { // NOPMD ForLoopCanBeForeach
            Invocation v = (Invocation) invocations.get(i);
            if (!v.cancelled && v.owner == owner && (method == null || method.equals(v.method))) { // NOPMD CompareObjectsWithEquals
                return true;
            }
        }
        return false;
    }

    private static void runInvocations() {
        int n = invocations.size();
        if (n == 0) {
            return;
        }
        for (int i = 0; i < n; i++) {
            Invocation v = (Invocation) invocations.get(i);
            if (v.owner.destroyed) {
                v.cancelled = true;
            }
            if (v.cancelled || Time.time < v.at) {
                continue;
            }
            if (v.repeat > 0f) {
                v.at += v.repeat;
            } else {
                v.cancelled = true;
            }
            if (!v.owner.$invoke(v.method)) {
                Debug.LogWarning("Invoke: " + v.owner.getClass().getName() + " has no method " + v.method
                        + " without parameters");
                v.cancelled = true;
            }
        }
        for (int i = invocations.size() - 1; i >= 0; i--) {
            if (((Invocation) invocations.get(i)).cancelled) {
                invocations.remove(i);
            }
        }
    }

    // ------------------------------------------------------------- rendering

    /// Works out what the camera sees: every visible sprite and every
    /// piece of canvas text, in surface pixels, back to front. The list is
    /// reused from call to call and is valid until the next one.
    ///
    /// A sprite wholly outside the surface is left out. With a camera that
    /// follows a ship through a field of a thousand stars, most of them
    /// are, and what paints never hears of them.
    public static DrawList render() {
        drawList.clear();
        Camera camera = camera(true);
        if (camera == null) {
            camera = camera(false);
        }
        if (camera == null) {
            return drawList;
        }
        drawList.hasCamera = true;
        drawList.backgroundColor = camera.backgroundArgb();
        Transform eye = camera.gameObject.transform;
        eye.update();
        DrawView view = drawView;
        view.list = drawList;
        view.viewWidth = Screen.width;
        view.viewHeight = Screen.height;
        view.scale = camera.scale(Screen.height);
        view.centreX = view.viewWidth / 2f;
        view.centreY = view.viewHeight / 2f;
        view.eyeX = eye.wx;
        view.eyeY = eye.wy;
        view.eyeZ = eye.wz;
        view.eyeCos = eye.wcos;
        view.eyeSin = eye.wsin;
        view.eyeTurn = eye.wrot;
        int n = renderers.size();
        for (int i = 0; i < n; i++) {
            SpriteRenderer r = (SpriteRenderer) renderers.get(i);
            Sprite sprite = r.sprite;
            if (!r.enabled || r.destroyed || sprite == null || !r.gameObject.activeInHierarchy()) {
                continue;
            }
            Transform t = r.gameObject.transform;
            t.update();
            view.$sprite(sprite, t.wx, t.wy, t.wz, t.wsx, t.wsy, t.wrot, r.argb(), r.flipX, r.flipY, r.sortingOrder,
                    r.sortingLayerID);
        }
        int drawn = drawers.size();
        for (int i = 0; i < drawn; i++) {
            Component c = (Component) drawers.get(i);
            if (!c.destroyed && c.gameObject.activeInHierarchy()) {
                c.$draw(view);
            }
        }
        int m = canvases.size();
        for (int i = 0; i < m; i++) {
            Canvas canvas = (Canvas) canvases.get(i);
            // A canvas inside another is drawn as part of it.
            if (!canvas.live() || canvas.renderMode == 2
                    || (canvas.gameObject.transform.parent != null
                    && canvas.gameObject.transform.parent.gameObject.GetComponentInParent(Canvas.class) != null)) {
                continue;
            }
            drawCanvas(canvas, canvas.gameObject, canvasScale(canvas));
        }
        drawList.sort();
        return drawList;
    }

    private static void drawCanvas(Canvas canvas, GameObject go, float s) {
        int n = go.components.size();
        for (int i = 0; i < n; i++) {
            java.lang.Object c = go.components.get(i);
            if (!(c instanceof Graphic) || !((Behaviour) c).live()) {
                continue;
            }
            float canvasWidth = Screen.width / s;
            float canvasHeight = Screen.height / s;
            Transform t = go.transform;
            if (t instanceof RectTransform) {
                ((RectTransform) t).$rect(canvasWidth, canvasHeight, rect);
            } else {
                rect[0] = 0f;
                rect[1] = 0f;
                rect[2] = canvasWidth;
                rect[3] = canvasHeight;                rect[4] = 1f;
                rect[5] = 1f;
            }
            DrawCommand d = drawList.next();
            d.sprite = null;
            d.sourceX = 0;
            d.sourceY = 0;
            d.sourceWidth = 0;
            d.sourceHeight = 0;
            float top = rect[1] + rect[3];
            d.x = rect[0] * s;
            d.y = Screen.height - top * s;
            d.width = rect[2] * s;
            d.height = rect[3] * s;
            d.anchorX = 0f;
            d.anchorY = 0f;
            d.rotation = 0f;
            d.flipX = false;
            d.flipY = false;
            d.sortingOrder = canvas.sortingOrder;
            boolean overlay = canvas.renderMode == 0 || canvas.worldCamera == null;
            d.group = overlay ? 1 : 0;
            d.sortingLayer = overlay ? 0 : sortingLayer(canvas.sortingLayerID);
            d.depth = canvas.planeDistance;
            if (c instanceof Text) {
                ((Text) c).$fill(d, s * rect[5]);
            } else if (c instanceof Image) {
                if (!((Image) c).$fill(d)) {
                    drawList.drop();
                }
            } else if (!((Graphic) c).$paint(d, s * rect[5])) {
                drawList.drop();
            }
        }
        ArrayList children = go.transform.children;
        if (children != null) {
            for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach
                GameObject child = ((Transform) children.get(i)).gameObject;
                if (child.active && !child.destroyed) {
                    drawCanvas(canvas, child, s);
                }
            }
        }
    }
}
