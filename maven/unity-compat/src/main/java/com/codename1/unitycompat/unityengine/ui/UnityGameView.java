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
package com.codename1.unitycompat.unityengine.ui;

import com.codename1.gaming.GameInput;
import com.codename1.gaming.GameView;
import com.codename1.gaming.Scene;
import com.codename1.gaming.Sprite;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.PeerComponent;
import com.codename1.unitycompat.unityengine.AudioSource;
import com.codename1.unitycompat.unityengine.PlayerPrefs;
import com.codename1.unitycompat.unityengine.DrawCommand;
import com.codename1.unitycompat.unityengine.DrawList;
import com.codename1.unitycompat.unityengine.UnityRuntime;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/// Shows a running Unity project on a Codename One game surface.
///
/// This is the half of rendering that needs a display, and it is kept
/// thin: everything that decides *where* a sprite goes is in
/// [UnityRuntime#render], which a test can call with no surface at all.
/// What is left here is, once a frame, to tell the runtime the size of the
/// view, hand it the pointer and the keys, step it, and copy the draw list
/// it answers with onto the `com.codename1.gaming.Sprite`s of this view's
/// scene.
///
/// It lives in a package of its own because it is the only class of the
/// runtime that needs `com.codename1.ui`; the rest compiles, and runs,
/// against nothing but the class library and Box2D.
///
/// The project is installed by the caller, before the view is started.
/// The view is made first and asked for its services, because
/// `UnityRuntime.begin()` runs every script's `Awake` and `OnEnable`, and
/// those already read preferences, play sounds and ask where they run:
///
/// ```java
/// UnityRuntime.reset();
/// UnityAppImpl.install();
/// UnityGameView view = new UnityGameView();
/// view.installServices();
/// UnityRuntime.begin();
/// form.add(BorderLayout.CENTER, view);
/// form.show();
/// view.start();
/// ```
///
/// A sprite's image is loaded by its resource name from the root of the
/// application's resources -- `/ball.png` -- the first time it is drawn,
/// and kept; a sprite cut from a sheet is cut once and kept too.
///
/// #### Text
///
/// Canvas text is drawn in the platform's own font, never in the font a
/// Unity scene names -- none is shipped. A piece of text is laid out and
/// painted into an image when it first appears or changes, and that image
/// is then a sprite like any other, so a score that stands still costs
/// nothing a frame.
///
/// #### Keys
///
/// A key reaches Unity's `Input` under Unity's key code. Letters, digits
/// and space are themselves; the directional pad -- and so the arrow keys
/// -- become the arrows, which Unity's default `Horizontal` and `Vertical`
/// axes read beside WASD.
///
/// The fire button is both Space and Return, held and let go together. A
/// port with a keyboard reports the two keys as that one button -- the
/// desktop port does, for the space bar and for Enter -- so which of them
/// was struck is not known here, and a game waits for one or the other:
/// Space to shoot, Return to start. Pressing both lets either game be
/// played; one that gives the two keys different meanings in the same
/// screen sees both happen.
///
/// #### Fingers
///
/// The first finger is the mouse, as it always was. Beside that, every
/// finger reaches Unity's `Input.touches`. Codename One reports a touch as
/// the positions of the fingers and does not number them, and its ports
/// differ in what a report holds, so what is handed on is the one thing
/// they agree on -- where every finger is now:
///
/// - a drag holds every finger, and is taken as it is;
/// - a press holds the fingers that just landed, on some ports only the
///   first of a gesture, so its points are added to the ones already known
///   unless one is already there, within three millimetres;
/// - a release is sent when the last finger lifts, and clears them all.
///
/// A second finger therefore shows up, on a port that reports only the
/// first press, with the first drag that follows it -- a finger never rests
/// that still -- and a finger lifted while another stays is noticed with the
/// next drag too. `Input` turns those positions into numbered touches with
/// phases; how is described there.
///
/// #### Threads
///
/// A port may deliver a key on the event dispatch thread and draw a frame
/// on another: the desktop port paints this view's surface on the AWT
/// thread. Unity's scripts run inside the frame, so keys are not handed to
/// the runtime where they arrive. They are queued, and the frame takes the
/// queue when it starts. So are the fingers, and the mouse the first of
/// them is: nothing a frame reads is a flag another thread sets and the
/// frame clears, because what is set between the reading and the clearing
/// is lost.
///
/// A key that is held is not released by the display's key repeat, which
/// `GameView` ignores: it runs by the wall clock, and would otherwise let
/// go of a held direction 800 ms after it went down.
///
/// Code of the application's own is in the same position: a command of the
/// toolbar or the answer to a network request runs on the event dispatch
/// thread, and must not read a script's fields while a frame may be
/// changing them. It hands the work to [#callInFrame(Runnable)], and the
/// frame runs it before any script:
///
/// ```java
/// toolbar.addCommandToSideMenu("Restart", null,
///         e -> gameView.callInFrame(() -> score.points = 0));
/// ```
///
/// #### Pausing
///
/// [#pause()] stops the simulation and [#resume()] continues it. Scripts
/// are told, inside the frame that notices: `OnApplicationFocus(false)`
/// and `OnApplicationPause(true)`, then the same two the other way round
/// with the opposite values. A paused view still runs what
/// [#callInFrame(Runnable)] is given.
///
/// #### Stepping by hand
///
/// A frame normally simulates as much time as passed since the one before
/// it, which no two runs agree on. [#holdClock()] takes the clock away:
/// from the next frame on nothing is simulated, and the view goes on
/// drawing the scene as it stands -- no script runs, nothing animates and
/// no particle moves. [#advance(int, float)] then runs a number of steps of
/// a length that is given, so the picture after it is the same on every
/// run and every machine:
///
/// ```java
/// view.holdClock();
/// view.start();
/// view.advance(90, 1f / 60);
/// // later, on the event dispatch thread:
/// if (view.framesAdvanced() == 90) {
///     // the ninetieth frame is on the view's scene, and stays there
/// }
/// ```
///
/// Unlike a pause the scripts are not told anything: the game has not lost
/// the focus, it is being played a frame at a time. A held view takes no
/// input; keys and touches that arrive are dropped. [#releaseClock()] gives
/// the clock back.
public class UnityGameView extends GameView {
    private static final int KEY_UP = 273;
    private static final int KEY_DOWN = 274;
    private static final int KEY_RIGHT = 275;
    private static final int KEY_LEFT = 276;
    private static final int KEY_RETURN = 13;
    private static final int KEY_ESCAPE = 27;
    private static final int KEY_SPACE = 32;
    /// Not a Unity key: the fire button, which is two of them.
    private static final int FIRE = -1;
    private static final int[] NO_KEYS = new int[0];

    /// Pooled, and in draw order: the sprite at index `i` shows command `i`
    /// and has z-order `i`, so the scene never needs sorting again.
    private final ArrayList pool = new ArrayList();
    private final HashMap images = new HashMap();
    /// Names that failed to load, so each is reported once and not retried
    /// sixty times a second.
    private final HashMap missing = new HashMap();
    /// The sprites of a sheet, cut: by image, then by where in it.
    private final HashMap parts = new HashMap();
    /// One painted text for each text command of a frame, in their order.
    private final ArrayList texts = new ArrayList();
    private final HashMap fonts = new HashMap();
    /// Unity key codes held down, so that a release is sent for exactly
    /// what was pressed even if the platform's code maps differently by
    /// then.
    private final HashMap held = new HashMap();
    /// Keys since the last frame, in order: the Unity key code, negative
    /// for a release. Replaced whole and never changed in place, which is
    /// what lets the thread that draws take it from the thread that types
    /// without a lock.
    private final AtomicReference pending = new AtomicReference(NO_KEYS);
    private static final float[][] NO_TOUCHES = new float[0][];
    private static final float[] NO_FINGERS = new float[0];
    /// Reports of where the fingers are since the last frame, in order:
    /// each the x and y of every finger, in pixels of this view. Handed
    /// from the thread that is touched to the one that draws the way the
    /// keys are.
    private final AtomicReference pendingTouches = new AtomicReference(NO_TOUCHES);
    /// The last of them. Only the event dispatch thread reads or writes
    /// this.
    private float[] fingers = NO_FINGERS;
    /// Whether this view has ever been handed a pointer event directly; if
    /// it never is, the one pointer the game input knows is the one finger.
    private final AtomicBoolean touchesArrive = new AtomicBoolean();
    private static final int MOUSE_MOVE = 0;
    private static final int MOUSE_PRESS = 1;
    private static final int MOUSE_RELEASE = 2;
    /// What the first finger -- the mouse -- did since the last frame, in
    /// order: a kind, then its x and y in pixels of this view. Handed over
    /// the way the keys are, and for the reason they are: the game input
    /// this view inherits keeps a press as a flag the frame reads and then
    /// clears, and a press the event dispatch thread set between the two
    /// was cleared without ever having been read. The button then stayed up
    /// in Unity for as long as it was held down.
    private final AtomicReference pendingMouse = new AtomicReference(NO_KEYS);
    /// The frame's own: whether Unity has been told the button is down,
    /// where the mouse last was, and a release that a pause held back.
    private boolean mouseHeld;
    private int mouseAtX;
    private int mouseAtY;
    private boolean mouseReleaseHeldBack;
    /// The frame's own: the report a pause held back, a finger made from
    /// the pointer, and arrays to hand reports on in.
    private float[] heldBack;
    private boolean pointerFinger;
    private float[] touchX = new float[4];
    private float[] touchY = new float[4];
    /// Painted texts (`CachedText`) by everything that decides how they
    /// look, so that a text that comes back -- one that blinks, a score that
    /// returns to a value, a label that alternates among a few strings --
    /// comes back as the image it was, and as the texture the renderer
    /// already has for it.
    ///
    /// A label that never repeats -- a clock, a rising score -- makes an
    /// image for each string, and the sprite renderer a texture for each
    /// image, so the cache is bounded, and what leaves it gives its texture
    /// back (`releaseTexture`). After every frame (`trimTexts`) a text that
    /// was not drawn for `TEXT_KEEP_FRAMES` frames is dropped, and so are
    /// the longest undrawn while more than `TEXT_SPARE` of them are waiting:
    /// a text that changes every frame leaves one behind a frame, and that
    /// many is what it may leave. A text that is on the screen is never
    /// dropped, however many there are.
    ///
    /// TextMesh Pro text is drawn through the same commands and this cache.
    private final HashMap paintedTexts = new HashMap();
    /// The entries of `paintedTexts` again, to walk them without an
    /// iterator: this is done once a frame.
    private final ArrayList cachedTexts = new ArrayList();
    /// Counts the frames shown; an entry records the one it was last drawn in.
    private int textFrame;
    /// How long a painted text that is no longer drawn is kept: two seconds
    /// of a game at sixty frames a second.
    private static final int TEXT_KEEP_FRAMES = 120;
    /// How many painted texts that are not on the screen are kept at most.
    private static final int TEXT_SPARE = 32;

    /// A painted text of the cache.
    private static final class CachedText {
        String key;
        Image image;
        int drawn;
        /// Cleared when the entry leaves the cache: a text command that
        /// still has it must not show its image, which the renderer would
        /// make a texture for that nothing would ever release.
        boolean kept = true;
    }

    /// A text command as it was last painted, and the image of it.
    private static final class PaintedText {
        String text;
        int width;
        int height;
        int color;
        int alignment;
        int style;
        float size;
        float min;
        float max;
        boolean fit;
        boolean wrap;
        CachedText cached;

        boolean shows(DrawCommand d, int w, int h) {
            return cached != null && cached.kept && width == w && height == h && color == d.color && alignment == d.alignment
                    && style == d.fontStyle && size == d.fontSize && min == d.minFontSize && max == d.maxFontSize
                    && fit == d.bestFit && wrap == d.wrap && text.equals(d.text);
        }
    }

    /// What [#pause()] and [#resume()] ask for, from any thread.
    private final AtomicBoolean pauseWanted = new AtomicBoolean();
    /// What the frame last saw of it, and told the scripts. Only the frame
    /// reads or writes this.
    private boolean pauseSeen;

    /// Whether the clock is held; see [#holdClock()]. Asked from any thread.
    private final AtomicBoolean clockHeld = new AtomicBoolean();
    /// The steps [#advance(int, float)] asked for and no frame has taken
    /// yet: their number, and the bits of their length in seconds. An
    /// immutable pair, replaced whole, since the two belong together.
    private final AtomicReference stepsWanted = new AtomicReference(NO_KEYS);
    /// How many steps asked for have been run and put on the scene.
    private final AtomicInteger stepsRun = new AtomicInteger();

    private UnityGameAudio audio;
    private final UnityGamePrefs prefs = new UnityGamePrefs();

    @Override
    protected void deinitialize() {
        if (audio != null) {
            audio.stopAll();
            AudioSource.$output(null);
            audio = null;
        }
        if (PlayerPrefs.$store() == prefs) { // NOPMD CompareObjectsWithEquals
            PlayerPrefs.$store(null);
        }
        super.deinitialize();
    }

    /// Gives the runtime what only a host with a display has: somewhere
    /// for `PlayerPrefs` to stay, a device for `AudioSource` to play on,
    /// the platform `Application.platform` names and whether there is a
    /// touch screen.
    ///
    /// Call it before `UnityRuntime.begin()`. That call runs the `Awake`
    /// and `OnEnable` of the first scene, long before this view is shown,
    /// and without these a high score read in `Awake` is the default, one
    /// written there goes to a memory nobody reads again, and a source set
    /// to Play On Awake plays to nothing. `UnityApplication` does; a host
    /// that drives the view itself must.
    ///
    /// Calling it again does nothing new, and the view calls it itself when
    /// it is shown, which also brings the services back after the view was
    /// taken off a form and they were withdrawn.
    public final void installServices() {
        if (audio == null) {
            audio = new UnityGameAudio();
        }
        AudioSource.$output(audio);
        PlayerPrefs.$store(prefs);
        Display display = Display.getInstance();
        UnityRuntime.platform(platform(display.getPlatformName()));
        UnityRuntime.touchSupported(display.isTouchScreenDevice());
    }

    /// Unity's `RuntimePlatform` for a Codename One platform name.
    ///
    /// The name is the signal because it is the one thing every port
    /// answers and answers exactly: `ios`, `and`, `HTML5`, `mac`, `win`,
    /// `linux`. `isDesktop()` and `isTablet()` describe a form factor and
    /// are a guess on some ports; they cannot tell iOS from Android, which
    /// is what a script compares the platform for. The simulator is the
    /// desktop it runs on, as a game run from Unity's editor reports the
    /// editor's machine and not the phone it targets.
    ///
    /// Compared as written, never case folded: the names are constants of
    /// the ports, and folding by the device's locale would misread the `I`
    /// of a name on a Turkish one. A name no port is known to give is taken
    /// for a desktop, where a script expects least of the device.
    static int platform(String name) {
        if ("ios".equals(name)) {
            return UnityRuntime.PLATFORM_IOS;
        }
        if ("and".equals(name)) {
            return UnityRuntime.PLATFORM_ANDROID;
        }
        if ("HTML5".equals(name)) {
            return UnityRuntime.PLATFORM_WEB;
        }
        if ("mac".equals(name)) {
            return UnityRuntime.PLATFORM_MAC;
        }
        if ("win".equals(name)) {
            return UnityRuntime.PLATFORM_WINDOWS;
        }
        return UnityRuntime.PLATFORM_LINUX;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        // A game holds keys together -- turn, thrust and fire. Unless told
        // that keys overlap, the display takes the release of any key but
        // the last two pressed for a stray one and drops it, and the key
        // it belonged to stays down for good.
        Display.getInstance().setMultiKeyMode(true);
        // Sound has a device to play on and preferences somewhere to stay.
        // A host has usually asked already, before the first scene woke;
        // this is for the view shown again, and for a host that did not.
        installServices();
        PeerComponent surface = getPeer();
        if (surface != null) {
            // The surface is the whole of the view: a margin would draw
            // the frame smaller than the coordinates it was laid out in.
            surface.getAllStyles().setMargin(0, 0, 0, 0);
            surface.getAllStyles().setPadding(0, 0, 0, 0);
            // And it never takes the focus. A click lands on it, and if
            // the focus followed, the keys would go to it and stop here.
            surface.setFocusable(false);
            revalidate();
        }
        requestFocus();
    }

    /// Runs `call` inside the game's frame: at the start of the next one,
    /// before any script, on the thread that draws. This is how code on
    /// the event dispatch thread -- a command's listener, a callback --
    /// reads or changes anything a script owns. Calls run in the order
    /// they were given; while the view is paused they still run, and only
    /// the simulation waits.
    ///
    /// The other direction needs nothing: a script, or Java it calls,
    /// reaches the event dispatch thread with `CN.callSerially`.
    public void callInFrame(Runnable call) {
        UnityRuntime.callInFrame(call);
    }

    /// Stops the simulation: no script runs and `Time.time` stands still
    /// until [#resume()]. The scripts hear of it first, as
    /// `OnApplicationFocus(false)` and `OnApplicationPause(true)`.
    @Override
    public void pause() {
        pauseWanted.set(true);
    }

    /// Continues after [#pause()]; the scripts get
    /// `OnApplicationPause(false)` and `OnApplicationFocus(true)` before
    /// their next `Update`.
    @Override
    public void resume() {
        pauseWanted.set(false);
    }

    @Override
    public boolean isPaused() {
        return pauseWanted.get();
    }

    @Override
    public boolean handlesInput() {
        return isRunning() && !pauseWanted.get() && !clockHeld.get();
    }

    /// Takes the clock away from the game: from the next frame on, a frame
    /// simulates nothing but what [#advance(int, float)] asks for, and draws
    /// the scene as it stands. Safe from any thread, before or after
    /// [#start()]; held before it, not even the first frame simulates
    /// anything.
    public void holdClock() {
        clockHeld.set(true);
    }

    /// Gives the clock back after [#holdClock()]: the next frame simulates
    /// the time since the one before it again. Steps asked for and not yet
    /// run are forgotten.
    public void releaseClock() {
        clockHeld.set(false);
        stepsWanted.set(NO_KEYS);
    }

    /// Whether [#holdClock()] is in force.
    public boolean isClockHeld() {
        return clockHeld.get();
    }

    /// Asks for `frames` steps of `deltaSeconds` each, all run by the next
    /// frame the view draws, whatever the time that frame really took.
    /// Holds the clock if it is not held. Asked again before a frame took
    /// the first request, the numbers add up and the newer length is used
    /// for all of them.
    ///
    /// Safe from any thread. The steps have been run, and the scene shows
    /// the last of them, once [#framesAdvanced()] has grown by `frames`.
    public void advance(int frames, float deltaSeconds) {
        if (frames < 0 || deltaSeconds < 0f || Float.isNaN(deltaSeconds)) {
            throw new IllegalArgumentException("advance: " + frames + " frames of " + deltaSeconds + " seconds");
        }
        clockHeld.set(true);
        int bits = Float.floatToIntBits(deltaSeconds);
        while (true) {
            int[] before = (int[]) stepsWanted.get();
            int[] after = new int[] {before.length == 0 ? frames : before[0] + frames, bits};
            if (stepsWanted.compareAndSet(before, after)) {
                return;
            }
        }
    }

    /// The number of steps [#advance(int, float)] asked for that have been
    /// run and put on the view's scene, since the view was made.
    public int framesAdvanced() {
        return stepsRun.get();
    }

    /// A frame of a view whose clock is held: the steps that were asked
    /// for, at the length they were asked at, and the scene as it then is.
    private void heldFrame() {
        // What arrived is dropped, not kept for later: a press delivered
        // twenty frames after it was made is not the press that was made.
        pending.set(NO_KEYS);
        pendingTouches.set(NO_TOUCHES);
        pendingMouse.set(NO_KEYS);
        int[] wanted = (int[]) stepsWanted.getAndSet(NO_KEYS);
        int frames = wanted.length == 0 ? 0 : wanted[0];
        if (frames == 0) {
            // As a paused view does: what the application handed over
            // must not wait for a step that may never be asked for.
            UnityRuntime.runFrameCalls();
        } else {
            float dt = Float.intBitsToFloat(wanted[1]);
            for (int i = 0; i < frames; i++) {
                UnityRuntime.step(dt);
            }
        }
        show(UnityRuntime.render());
        stepsRun.addAndGet(frames);
    }

    @Override
    protected void update(double deltaSeconds) {
        int height = getHeight();
        UnityRuntime.resize(getWidth(), height);
        if (clockHeld.get()) {
            heldFrame();
            return;
        }
        // The scripts are told of a pause here, inside the frame, rather
        // than where it was asked for, which may be another thread.
        boolean paused = pauseWanted.get();
        if (paused != pauseSeen) {
            pauseSeen = paused;
            UnityRuntime.applicationPaused(paused);
        }
        float[][] reports = (float[][]) pendingTouches.getAndSet(NO_TOUCHES);
        if (paused) {
            // Nothing reads the fingers while paused; where they ended up
            // is what the first frame after it is told.
            if (reports.length > 0) {
                heldBack = reports[reports.length - 1];
            }
            // Nor the mouse. A press made while paused never happened; a
            // button that was down before and has come up since is let go
            // in the first frame after.
            int[] ignored = (int[]) pendingMouse.getAndSet(NO_KEYS);
            for (int i = 0; i + 2 < ignored.length; i += 3) {
                mouseReleaseHeldBack |= mouseHeld && ignored[i] == MOUSE_RELEASE;
            }
            UnityRuntime.runFrameCalls();
            return;
        }
        UnityRuntime.touchSupported(Display.getInstance().isTouchScreenDevice());
        if (heldBack != null) {
            report(heldBack, height);
            heldBack = null;
        }
        for (int i = 0; i < reports.length; i++) { // NOPMD ForLoopCanBeForeach
            report(reports[i], height);
        }
        int[] keys = (int[]) pending.getAndSet(NO_KEYS);
        for (int i = 0; i < keys.length; i++) { // NOPMD ForLoopCanBeForeach
            if (keys[i] > 0) {
                UnityRuntime.keyPressed(keys[i]);
            } else {
                UnityRuntime.keyReleased(-keys[i]);
            }
        }
        GameInput input = getInput();
        // Read before the queue is taken: an event that arrives in between
        // is then in the queue, and never in neither.
        boolean direct = touchesArrive.get();
        if (mouseReleaseHeldBack) {
            mouseReleaseHeldBack = false;
            mouse(MOUSE_RELEASE, mouseAtX, mouseAtY, height);
        }
        int[] mouse = (int[]) pendingMouse.getAndSet(NO_KEYS);
        for (int i = 0; i + 2 < mouse.length; i += 3) {
            mouse(mouse[i], mouse[i + 1], mouse[i + 2], height);
        }
        if (!direct) {
            // No port has handed this view a pointer event itself: all there
            // is to go by is what the form's listeners left in the game
            // input, on whichever thread they ran. Whether the pointer is
            // down is a state, and is believed over the two flags, either of
            // which may have been cleared before it was read here.
            boolean down = input.isPointerDown();
            boolean pressed = input.wasPointerPressed();
            boolean released = input.wasPointerReleased();
            if (down || pressed || released) {
                mouse(MOUSE_MOVE, input.getPointerX(), input.getPointerY(), height);
            }
            if (pressed || down) {
                mouse(MOUSE_PRESS, input.getPointerX(), input.getPointerY(), height);
            }
            if (released || !down) {
                mouse(MOUSE_RELEASE, input.getPointerX(), input.getPointerY(), height);
            }
        } else if (mouseHeld && mouse.length == 0 && !input.isPointerDown()) {
            // The form gave the release to another component -- the finger
            // had left the view -- and only its listener heard it. That the
            // pointer is up is a state and not a flag, so nothing clears it
            // unread.
            mouse(MOUSE_RELEASE, mouseAtX, mouseAtY, height);
        }
        if (!direct) {
            // No port has handed this view a touch itself, so all that is
            // known is the one pointer: it is the one finger.
            if (input.isPointerDown()) {
                pointerFinger = true;
                touchX[0] = input.getPointerX();
                touchY[0] = height - input.getPointerY();
                UnityRuntime.touches(touchX, touchY, 1);
            } else if (pointerFinger) {
                pointerFinger = false;
                UnityRuntime.touches(touchX, touchY, 0);
            }
        }
        UnityRuntime.step((float) deltaSeconds);
        show(UnityRuntime.render());
    }

    /// Tells the runtime of one thing the mouse did, at a point in pixels
    /// of this view: the view counts y down from its top and Unity up from
    /// its bottom. A button is pressed once and released once, whatever is
    /// reported twice -- the first event of a view is heard by the game
    /// input and by the queue both.
    private void mouse(int kind, int x, int y, int height) {
        mouseAtX = x;
        mouseAtY = y;
        if (kind == MOUSE_RELEASE && !mouseHeld) {
            return;
        }
        UnityRuntime.pointerMoved(x, height - y);
        if (kind == MOUSE_PRESS && !mouseHeld) {
            mouseHeld = true;
            UnityRuntime.pointerPressed(x, height - y);
        } else if (kind == MOUSE_RELEASE) {
            mouseHeld = false;
            UnityRuntime.pointerReleased(x, height - y);
        }
    }

    /// Adds what the first finger did to the queue the next frame takes.
    private void postMouse(int kind, int[] x, int[] y) {
        if (x.length == 0 || y.length == 0) {
            return;
        }
        int lx = x[0] - getAbsoluteX();
        int ly = y[0] - getAbsoluteY();
        while (true) {
            int[] before = (int[]) pendingMouse.get();
            int[] after = new int[before.length + 3];
            System.arraycopy(before, 0, after, 0, before.length);
            after[before.length] = kind;
            after[before.length + 1] = lx;
            after[before.length + 2] = ly;
            if (pendingMouse.compareAndSet(before, after)) {
                return;
            }
        }
    }

    /// Hands one report of the fingers to the runtime, in Unity's
    /// coordinates: y up from the bottom of the view.
    private void report(float[] points, int height) {
        int n = points.length / 2;
        if (n > touchX.length) {
            touchX = new float[n];
            touchY = new float[n];
        }
        for (int i = 0; i < n; i++) {
            touchX[i] = points[i * 2];
            touchY[i] = height - points[i * 2 + 1];
        }
        UnityRuntime.touches(touchX, touchY, n);
    }

    /// Adds a report of the fingers to the queue the next frame takes.
    private void postTouches(float[] points) {
        fingers = points;
        touchesArrive.set(true);
        while (true) {
            float[][] before = (float[][]) pendingTouches.get();
            float[][] after = new float[before.length + 1][];
            System.arraycopy(before, 0, after, 0, before.length);
            after[before.length] = points;
            if (pendingTouches.compareAndSet(before, after)) {
                return;
            }
        }
    }

    /// The points of a pointer event, in pixels of this view.
    private float[] local(int[] x, int[] y) {
        int ox = getAbsoluteX();
        int oy = getAbsoluteY();
        int n = x.length < y.length ? x.length : y.length;
        float[] points = new float[n * 2];
        for (int i = 0; i < n; i++) {
            points[i * 2] = x[i] - ox;
            points[i * 2 + 1] = y[i] - oy;
        }
        return points;
    }

    @Override
    public void pointerPressed(int[] x, int[] y) {
        super.pointerPressed(x, y);
        float[] landed = local(x, y);
        float near = Display.getInstance().convertToPixels(3f);
        float[] all = new float[fingers.length + landed.length];
        System.arraycopy(fingers, 0, all, 0, fingers.length);
        int n = fingers.length;
        for (int i = 0; i + 1 < landed.length; i += 2) {
            boolean known = false;
            for (int j = 0; j + 1 < n && !known; j += 2) {
                float dx = landed[i] - all[j];
                float dy = landed[i + 1] - all[j + 1];
                known = dx <= near && dx >= -near && dy <= near && dy >= -near;
            }
            if (!known) {
                all[n++] = landed[i];
                all[n++] = landed[i + 1];
            }
        }
        float[] now = new float[n];
        System.arraycopy(all, 0, now, 0, n);
        postTouches(now);
        postMouse(MOUSE_PRESS, x, y);
    }

    @Override
    public void pointerDragged(int[] x, int[] y) {
        super.pointerDragged(x, y);
        postTouches(local(x, y));
        postMouse(MOUSE_MOVE, x, y);
    }

    @Override
    public void pointerReleased(int[] x, int[] y) {
        super.pointerReleased(x, y);
        postTouches(NO_FINGERS);
        postMouse(MOUSE_RELEASE, x, y);
    }

    /// The Unity `KeyCode` for a Codename One key code, [#FIRE] for the
    /// fire button, or 0 for a key Unity has no name for.
    ///
    /// The character keys are tried first: on a keyboard the game action
    /// of a letter can be a direction too, and a game that reads `W` wants
    /// the letter.
    static int unityKey(int keyCode) {
        if (keyCode >= 'A' && keyCode <= 'Z') {
            return keyCode - 'A' + 'a';
        }
        if ((keyCode >= 'a' && keyCode <= 'z') || (keyCode >= '0' && keyCode <= '9') || keyCode == ' ') {
            return keyCode;
        }
        if (keyCode == '\n' || keyCode == '\r') {
            return KEY_RETURN;
        }
        if (keyCode == KEY_ESCAPE) {
            return KEY_ESCAPE;
        }
        if (keyCode == 8 || keyCode == 9 || keyCode == 127) {
            return keyCode;
        }
        switch (Display.getInstance().getGameAction(keyCode)) {
            case Display.GAME_UP:
                return KEY_UP;
            case Display.GAME_DOWN:
                return KEY_DOWN;
            case Display.GAME_LEFT:
                return KEY_LEFT;
            case Display.GAME_RIGHT:
                return KEY_RIGHT;
            case Display.GAME_FIRE:
                return FIRE;
            default:
                return 0;
        }
    }

    /// Adds a key event to the queue the next frame takes.
    private void post(int event) {
        while (true) {
            int[] before = (int[]) pending.get();
            int[] after = new int[before.length + 1];
            System.arraycopy(before, 0, after, 0, before.length);
            after[before.length] = event;
            if (pending.compareAndSet(before, after)) {
                return;
            }
        }
    }

    private void press(int key) {
        if (key == FIRE) {
            // See the class comment: Space and Return are one button.
            post(KEY_SPACE);
            post(KEY_RETURN);
        } else {
            post(key);
        }
    }

    private void release(int key) {
        if (key == FIRE) {
            post(-KEY_SPACE);
            post(-KEY_RETURN);
        } else {
            post(-key);
        }
    }

    @Override
    public void keyPressed(int keyCode) {
        super.keyPressed(keyCode);
        int key = unityKey(keyCode);
        Integer code = Integer.valueOf(keyCode);
        // A key that repeats while it is held is pressed once.
        if (key != 0 && !held.containsKey(code)) {
            held.put(code, Integer.valueOf(key));
            press(key);
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        super.keyReleased(keyCode);
        Object key = held.remove(Integer.valueOf(keyCode));
        if (key instanceof Integer) {
            release(((Integer) key).intValue());
            return;
        }
        // A letter goes down as `A` and comes up as `a` when Shift is let
        // go first, and the code that went down is then never released:
        // release whatever is held as the same Unity key.
        int unity = unityKey(keyCode);
        if (unity == 0) {
            return;
        }
        Iterator it = held.values().iterator();
        while (it.hasNext()) {
            if (((Integer) it.next()).intValue() == unity) {
                it.remove();
                release(unity);
                return;
            }
        }
    }

    private void show(DrawList list) {
        textFrame++;
        if (list.hasCamera) {
            setClearColor(list.backgroundColor);
        }
        Scene scene = getScene();
        int n = list.size();
        int text = 0;
        for (int i = 0; i < n; i++) {
            DrawCommand d = list.get(i);
            Sprite s;
            if (i < pool.size()) {
                s = (Sprite) pool.get(i);
            } else {
                s = new Sprite();
                s.setZOrder(i);
                pool.add(s);
                scene.add(s);
            }
            if (d.text != null) {
                Image painted = text(text++, d);
                s.setImage(painted);
                s.setVisible(painted != null);
                s.setPosition(d.x, d.y);
                if (painted != null) {
                    s.setSize(painted.getWidth(), painted.getHeight());
                }
                s.setScale(1f, 1f);
                s.setAnchor(0, 0);
                s.setRotation(0f);
                s.setColor(0xffffffff);
                continue;
            }
            Image image = part(d);
            s.setImage(image);
            s.setVisible(image != null);
            s.setPosition(d.x, d.y);
            s.setSize(d.width, d.height);
            // The gaming renderer mirrors a negatively scaled quad about
            // its anchor, which is what a Unity flip does about its pivot.
            s.setScale(d.flipX ? -1f : 1f, d.flipY ? -1f : 1f);
            s.setAnchor(d.anchorX, d.anchorY);
            s.setRotation(d.rotation);
            s.setColor(d.color);
        }
        for (int i = n; i < pool.size(); i++) {
            ((Sprite) pool.get(i)).setVisible(false);
        }
        trimTexts();
    }

    /// Drops the painted texts that have not been drawn for a while, and
    /// the longest undrawn of them while there are too many. Run after the
    /// frame's texts were all looked up, so that one drawn in this frame is
    /// known by its mark and is never dropped; it allocates nothing.
    private void trimTexts() {
        int idle = 0;
        // Backwards, since dropping one moves the last into its place.
        for (int i = cachedTexts.size() - 1; i >= 0; i--) { // NOPMD ForLoopCanBeForeach
            CachedText c = (CachedText) cachedTexts.get(i);
            if (c.drawn == textFrame) {
                continue;
            }
            // A difference, not a comparison: the counter may wrap.
            if (textFrame - c.drawn > TEXT_KEEP_FRAMES) {
                dropText(i);
            } else {
                idle++;
            }
        }
        while (idle > TEXT_SPARE) {
            int oldest = -1;
            int age = 0;
            for (int i = cachedTexts.size() - 1; i >= 0; i--) { // NOPMD ForLoopCanBeForeach
                CachedText c = (CachedText) cachedTexts.get(i);
                if (textFrame - c.drawn > age) {
                    age = textFrame - c.drawn;
                    oldest = i;
                }
            }
            if (oldest < 0) {
                return;
            }
            dropText(oldest);
            idle--;
        }
    }

    /// Takes an entry out of the cache and gives its texture back. The
    /// renderer disposes it at the start of its next frame, in which this
    /// image is not drawn.
    private void dropText(int index) {
        CachedText c = (CachedText) cachedTexts.get(index);
        int last = cachedTexts.size() - 1;
        cachedTexts.set(index, cachedTexts.get(last));
        cachedTexts.remove(last);
        paintedTexts.remove(c.key);
        c.kept = false;
        releaseTexture(c.image);
        c.image = null;
    }

    /// The number of painted texts kept at the moment, for a check to hold
    /// against: the texts on the screen and at most `TEXT_SPARE` more.
    public int paintedTextCount() {
        return cachedTexts.size();
    }

    private Image image(String resource) {
        Image image = (Image) images.get(resource);
        if (image != null || missing.containsKey(resource)) {
            return image;
        }
        // By stream: `Image.createImage(String)` takes a path in the file
        // system, and only some ports look among the resources for it.
        InputStream in = Display.getInstance().getResourceAsStream(getClass(), "/" + resource);
        if (in != null) {
            try {
                image = Image.createImage(in);
            } catch (IOException e) {
                Log.e(e);
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(e);
                }
            }
        }
        if (image == null) {
            missing.put(resource, resource);
            Log.p("UnityGameView: no image resource /" + resource);
            return null;
        }
        images.put(resource, image);
        return image;
    }

    /// The image of a command: the whole of its resource, or the part of
    /// it a sheet's sprite is.
    private Image part(DrawCommand d) {
        Image whole = image(d.sprite);
        if (whole == null || (d.sourceX == 0 && d.sourceY == 0 && d.sourceWidth == whole.getWidth()
                && d.sourceHeight == whole.getHeight())) {
            return whole;
        }
        HashMap cut = (HashMap) parts.get(d.sprite);
        if (cut == null) {
            cut = new HashMap();
            parts.put(d.sprite, cut);
        }
        int w = Math.min(d.sourceWidth, whole.getWidth() - d.sourceX);
        int h = Math.min(d.sourceHeight, whole.getHeight() - d.sourceY);
        if (w <= 0 || h <= 0) {
            return null;
        }
        // An image is narrower than 65536 pixels, so the four numbers of a
        // part fit one key. Where it starts is not enough to name it: a
        // filled image -- a health bar -- is cut from the same corner at
        // every width it has had, and keyed by its corner alone it went on
        // being drawn from the first width it was asked for, stretched.
        Long key = Long.valueOf((long) d.sourceX << 48 | (long) d.sourceY << 32 | (long) w << 16 | h);
        Image image = (Image) cut.get(key);
        if (image == null) {
            image = whole.subImage(d.sourceX, d.sourceY, w, h, true);
            cut.put(key, image);
        }
        return image;
    }

    // ------------------------------------------------------------------ text

    private Font font(int style, int pixels) {
        Integer key = Integer.valueOf(pixels << 2 | (style & 3));
        Font f = (Font) fonts.get(key);
        if (f == null) {
            boolean bold = (style & 1) != 0;
            boolean italic = (style & 2) != 0;
            if (Font.isNativeFontSchemeSupported()) {
                String name = bold ? (italic ? "native:ItalicBold" : "native:MainBold")
                        : (italic ? "native:ItalicRegular" : "native:MainRegular");
                f = Font.createTrueTypeFont(name, name).derive(pixels, Font.STYLE_PLAIN);
            } else {
                f = Font.createSystemFont(Font.FACE_SYSTEM, (bold ? Font.STYLE_BOLD : 0)
                        | (italic ? Font.STYLE_ITALIC : 0), Font.SIZE_LARGE);
            }
            fonts.put(key, f);
        }
        return f;
    }

    /// Breaks a text into lines: at each line feed, and, when wrapping, at
    /// the last space that still fits.
    private static ArrayList lines(String text, Font font, int width, boolean wrap) {
        ArrayList out = new ArrayList();
        int at = 0;
        while (at <= text.length()) {
            int end = text.indexOf('\n', at);
            if (end < 0) {
                end = text.length();
            }
            String paragraph = text.substring(at, end);
            at = end + 1;
            if (!wrap || font.stringWidth(paragraph) <= width) {
                out.add(paragraph);
                continue;
            }
            String line = "";
            int from = 0;
            while (from <= paragraph.length()) {
                int space = paragraph.indexOf(' ', from);
                if (space < 0) {
                    space = paragraph.length();
                }
                String word = paragraph.substring(from, space);
                from = space + 1;
                String longer = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && font.stringWidth(longer) > width) {
                    out.add(line);
                    line = word;
                } else {
                    line = longer;
                }
            }
            out.add(line);
        }
        return out;
    }

    private boolean fits(DrawCommand d, int pixels, int width, int height) {
        Font f = font(d.fontStyle, pixels);
        ArrayList lines = lines(d.text, f, width, d.wrap);
        if (lines.size() * f.getHeight() > height) {
            return false;
        }
        for (int i = 0; i < lines.size(); i++) { // NOPMD ForLoopCanBeForeach
            if (f.stringWidth((String) lines.get(i)) > width) {
                return false;
            }
        }
        return true;
    }

    /// The image of the `index`th text of this frame, painted again only
    /// if anything about it changed.
    private Image text(int index, DrawCommand d) {
        while (texts.size() <= index) {
            texts.add(new PaintedText());
        }
        PaintedText p = (PaintedText) texts.get(index);
        int width = (int) d.width;
        int height = (int) d.height;
        if (width <= 0 || height <= 0) {
            return null;
        }
        if (p.shows(d, width, height)) {
            p.cached.drawn = textFrame;
            return p.cached.image;
        }
        String key = width + "x" + height + "/" + d.color + "/" + d.alignment + "/" + d.fontStyle + "/" + d.fontSize
                + "/" + d.minFontSize + "/" + d.maxFontSize + "/" + d.bestFit + "/" + d.wrap + "/" + d.text;
        CachedText cached = (CachedText) paintedTexts.get(key);
        if (cached == null) {
            cached = new CachedText();
            cached.key = key;
            cached.image = paint(d, width, height);
            paintedTexts.put(key, cached);
            cachedTexts.add(cached);
        }
        cached.drawn = textFrame;
        p.text = d.text;
        p.width = width;
        p.height = height;
        p.color = d.color;
        p.alignment = d.alignment;
        p.style = d.fontStyle;
        p.size = d.fontSize;
        p.min = d.minFontSize;
        p.max = d.maxFontSize;
        p.fit = d.bestFit;
        p.wrap = d.wrap;
        p.cached = cached;
        return cached.image;
    }

    /// Lays a text out in its rectangle and paints it into a new image.
    private Image paint(DrawCommand d, int width, int height) {
        int pixels = Math.max(1, (int) d.fontSize);
        if (d.bestFit) {
            // Unity's best fit: the largest size of the range at which the
            // text is inside its rectangle, and the smallest if none is.
            int low = Math.max(1, (int) d.minFontSize);
            pixels = low;
            for (int s = Math.max(low, (int) d.maxFontSize); s > low; s--) {
                if (fits(d, s, width, height)) {
                    pixels = s;
                    break;
                }
            }
        }
        Font f = font(d.fontStyle, pixels);
        ArrayList lines = lines(d.text, f, width, d.wrap);
        int block = lines.size() * f.getHeight();
        int row = d.alignment / 3;
        int column = d.alignment % 3;
        int top = row == 0 ? 0 : row == 1 ? (height - block) / 2 : height - block;
        Image image = Image.createImage(width, height, 0);
        Graphics g = image.getGraphics();
        g.setAntiAliasedText(true);
        g.setFont(f);
        g.setColor(d.color & 0xffffff);
        g.setAlpha(d.color >>> 24);
        for (int i = 0; i < lines.size(); i++) {
            String line = (String) lines.get(i);
            int w = f.stringWidth(line);
            g.drawString(line, column == 0 ? 0 : column == 1 ? (width - w) / 2 : width - w, top + i * f.getHeight());
        }
        return image;
    }
}
