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
package offscreen;

import UnityEngine.Vector3;
import com.codename1.gaming.Scene;
import com.codename1.gaming.Sprite;
import com.codename1.impl.ImplementationFactory;
import com.codename1.impl.javase.OffscreenSurface;
import com.codename1.testing.OffscreenImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.unitycompat.unityengine.DrawCommand;
import com.codename1.unitycompat.unityengine.DrawList;
import com.codename1.unitycompat.unityengine.Input;
import com.codename1.unitycompat.unityengine.UnityRuntime;
import com.codename1.unitycompat.unityengine.ui.UnityGameView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import player.UnityPlayer;

/// Runs `player.UnityPlayer` -- the application `run-unity-project.sh`
/// opens a window for -- with no window, and checks what a window would
/// have shown and done.
///
/// What is real: the player, its form, `UnityGameView`, the sprite
/// renderer of `com.codename1.gaming` and the JavaSE port's software
/// rasteriser, which paints each dumped frame into a PNG. Keys enter where
/// the port's canvas hands them to the core, under the port's key codes,
/// from a thread that is not the event dispatch thread, and frames are
/// stepped from that thread too, as the port does from AWT's.
///
/// What is not: the port itself. It cannot start without a display, so the
/// implementation is the core unit tests' (see `OffscreenImplementation`),
/// and nothing here says a real key press arrives, how often a frame is
/// drawn, or what a high-density screen does to the size.
///
/// ```
/// OffscreenCheck [--seed n] [--frames n] [--size WxH] [--input file] [--dump f1,f2,...]
///     [--png dir] [--trace file] [--count Prefix]... [--reference dir]
/// ```
///
/// `--input` is a script of `headless.HeadlessTrace`'s kind, of which this
/// delivers the keys and the pointer: a `touch`, a `host` or an `app` line
/// is refused, because nothing here is a touch screen or the Java side of a
/// project, and a script played without them would be another script.
/// `--trace` is a trace that class printed for the same seed, size and
/// script: every `frame` line of it must be what this run finds at that
/// frame, which is only so if every key arrived, and on its frame.
/// `--reference` is a directory of `frame-<n>.png` to measure the dumped
/// frames against.
///
/// Nothing here knows the project. What depends on one is asked of it: an
/// axis is pressed with the keys the project's input settings give it, and
/// is left alone, with a line that says so, where they have none; pixels are
/// expected only of a scene that draws; and `Time.time` is held to the time
/// scale the scripts have set, which a game that starts behind a menu has at
/// zero.
///
/// It exits with 1 and a line for each thing that was wrong, and with 1 and
/// a stack trace if anything threw: the event dispatch thread is not a
/// daemon, so an exception left to end `main` would leave the JVM waiting
/// for ever.
public final class OffscreenCheck {
    private static final List<String> FAILURES = new ArrayList<String>();
    // What a line of the input script does.
    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int MOVE = 2;
    /// No key of the port stands for a Unity key code.
    private static final int NO_PORT_KEY = Integer.MIN_VALUE;
    private static OffscreenImplementation impl;

    private OffscreenCheck() {
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            FAILURES.add(what);
            System.out.println("FAILED: " + what);
        }
    }

    /// Lets the event dispatch thread finish what was handed to it.
    private static void settle() {
        for (int i = 0; i < 3; i++) {
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                public void run() {
                }
            });
        }
    }

    /// The JavaSE port's code for a key of the input script.
    private static int portCode(String key) {
        if (key.equals("left")) {
            return OffscreenImplementation.GAME_KEY_CODE_LEFT;
        } else if (key.equals("right")) {
            return OffscreenImplementation.GAME_KEY_CODE_RIGHT;
        } else if (key.equals("up")) {
            return OffscreenImplementation.GAME_KEY_CODE_UP;
        } else if (key.equals("down")) {
            return OffscreenImplementation.GAME_KEY_CODE_DOWN;
        } else if (key.equals("space") || key.equals("return") || key.equals("enter")) {
            return OffscreenImplementation.GAME_KEY_CODE_FIRE;
        } else if (key.equals("escape")) {
            return 27;
        } else if (key.length() == 1) {
            return key.charAt(0);
        }
        throw new IllegalArgumentException("input script: no JavaSE key code known for " + key);
    }

    private static void key(int code, boolean down) {
        if (down) {
            impl.portKeyPressed(code);
        } else {
            impl.portKeyReleased(code);
        }
        settle();
    }

    private static void write(BufferedImage image, File file) throws Exception {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.createGraphics().drawImage(image, 0, 0, null);
        ImageIO.write(rgb, "png", file);
    }

    /// The share of pixels that differ visibly between two images, in
    /// hundredths of a percent, or -1 if they are not the same size.
    private static int difference(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return -1;
        }
        long differing = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                int p = a.getRGB(x, y);
                int q = b.getRGB(x, y);
                int d = Math.max(Math.abs(((p >> 16) & 0xff) - ((q >> 16) & 0xff)), Math.max(
                        Math.abs(((p >> 8) & 0xff) - ((q >> 8) & 0xff)), Math.abs((p & 0xff) - (q & 0xff))));
                if (d > 48) {
                    differing++;
                }
            }
        }
        return (int) (differing * 10000 / ((long) a.getWidth() * a.getHeight()));
    }

    /// Pixels that are not the background, which is the colour of a corner.
    private static int ink(BufferedImage image) {
        int background = image.getRGB(0, 0) & 0xffffff;
        int n = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xffffff) != background) {
                    n++;
                }
            }
        }
        return n;
    }

    public static void main(String[] args) {
        int status = 1;
        try {
            status = run(args);
        } catch (Throwable t) { // NOPMD - anything at all, or the JVM never exits
            System.out.println("FAILED: the check threw " + t);
            t.printStackTrace(System.out);
            System.out.println("OFFSCREEN FAILED: threw");
        }
        System.out.flush();
        System.exit(status);
    }

    private static int run(String[] args) throws Exception {
        int seed = 1;
        int frames = 600;
        int width = 960;
        int height = 540;
        String script = "";
        int[] dumps = new int[0];
        File png = null;
        File reference = null;
        List<String> trace = new ArrayList<String>();
        final List<String> counted = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--seed")) {
                seed = Integer.parseInt(args[++i]);
            } else if (a.equals("--frames")) {
                frames = Integer.parseInt(args[++i]);
            } else if (a.equals("--size")) {
                String[] size = args[++i].split("x");
                width = Integer.parseInt(size[0]);
                height = Integer.parseInt(size[1]);
            } else if (a.equals("--input")) {
                script = new String(Files.readAllBytes(new File(args[++i]).toPath()), StandardCharsets.UTF_8);
            } else if (a.equals("--dump")) {
                String[] f = args[++i].split(",");
                dumps = new int[f.length];
                for (int k = 0; k < f.length; k++) {
                    dumps[k] = Integer.parseInt(f[k].trim());
                }
            } else if (a.equals("--png")) {
                png = new File(args[++i]);
                png.mkdirs();
            } else if (a.equals("--reference")) {
                reference = new File(args[++i]);
            } else if (a.equals("--trace")) {
                trace = Files.readAllLines(new File(args[++i]).toPath(), StandardCharsets.UTF_8);
            } else if (a.equals("--count")) {
                counted.add(args[++i]);
            } else {
                System.err.println("OffscreenCheck: unknown option " + a);
                return 2;
            }
        }
        List<int[]> events = new ArrayList<int[]>();
        for (String line : script.split("\n")) {
            int comment = line.indexOf('#');
            line = (comment >= 0 ? line.substring(0, comment) : line).trim();
            if (line.length() == 0) {
                continue;
            }
            String[] parts = line.split(" +");
            if (parts.length != 3) {
                throw new IllegalArgumentException("input script: not `<frame> <key> down|up`: " + line);
            }
            if (parts[1].equals("touch") || parts[1].equals("host") || parts[1].equals("app")) {
                throw new IllegalArgumentException("input script: a `" + parts[1] + "` line is not one this check can "
                        + "deliver; run the project without --input, or with a script of keys and the pointer: "
                        + line);
            }
            boolean pointer = parts[1].startsWith("pointer:");
            int kind = parts[2].equals("down") ? DOWN : parts[2].equals("up") ? UP : parts[2].equals("move") && pointer
                    ? MOVE : -1;
            if (kind < 0) {
                throw new IllegalArgumentException("input script: not `<frame> <key> down|up`: " + line);
            }
            if (pointer) {
                // A pointer event, in pixels from the view's top left.
                String[] at = parts[1].substring(8).split(",");
                events.add(new int[] {Integer.parseInt(parts[0]), 0, kind, Integer.parseInt(at[0]),
                    Integer.parseInt(at[1])});
            } else {
                events.add(new int[] {Integer.parseInt(parts[0]), portCode(parts[1]), kind});
            }
        }

        impl = new OffscreenImplementation();
        impl.setDisplaySize(width, height);
        final OffscreenImplementation created = impl;
        ImplementationFactory.setInstance(new ImplementationFactory() {
            @Override
            public Object createImplementation() {
                return created;
            }
        });
        Display.init(null);
        final Display display = Display.getInstance();
        display.setProperty("AppName", "Offscreen");
        display.setProperty("unity.player.seed", String.valueOf(seed));
        final UnityPlayer app = new UnityPlayer();
        display.callSeriallyAndWait(new Runnable() {
            public void run() {
                app.init(null);
                app.start();
            }
        });
        settle();
        final UnityGameView view = app.getView();
        Form form = app.getForm();
        check(display.getCurrent() == form, "the player's form is not the current form");
        check(impl.peersCreated == 1, "the view asked for " + impl.peersCreated + " GPU peers, not 1");
        check(view.getPeer() != null, "the view has no GPU peer");
        check(view.isRunning() && view.handlesInput(), "the view is not running and taking input");
        check(form.getFocused() == view, "the focus is on " + form.getFocused() + ", not on the game view");
        check(view.getWidth() == width && view.getHeight() == height, "the view is " + view.getWidth() + "x"
                + view.getHeight() + " in a " + width + "x" + height + " display: it does not fill the form");
        System.out.println("offscreen view=" + view.getWidth() + "x" + view.getHeight() + " at " + view.getAbsoluteX()
                + "," + view.getAbsoluteY() + " focused=" + (form.getFocused() == view));

        OffscreenSurface surface = new OffscreenSurface();
        double dt = 1f / 60;
        int traced = 0;
        for (int frame = 1; frame <= frames; frame++) {
            for (int[] e : events) {
                if (e[0] == frame && e.length > 3) {
                    if (e[2] == DOWN) {
                        impl.portPointerPressed(view.getAbsoluteX() + e[3], view.getAbsoluteY() + e[4]);
                    } else if (e[2] == MOVE) {
                        impl.portPointerDragged(view.getAbsoluteX() + e[3], view.getAbsoluteY() + e[4]);
                    } else {
                        impl.portPointerReleased(view.getAbsoluteX() + e[3], view.getAbsoluteY() + e[4]);
                    }
                    settle();
                } else if (e[0] == frame) {
                    key(e[1], e[2] == DOWN);
                }
            }
            view.frame(dt);
            String expected = null;
            for (String line : trace) {
                // The line the trace player prints of a frame, and not a
                // line of a script's own that begins the same way.
                if (line.startsWith("frame " + frame + " objects=")) {
                    expected = line;
                }
            }
            boolean dump = false;
            for (int d : dumps) {
                dump |= d == frame;
            }
            if (expected != null || dump) {
                DrawList list = UnityRuntime.render();
                StringBuilder sb = new StringBuilder();
                sb.append("frame ").append(frame).append(" objects=").append(UnityRuntime.$objectCount());
                for (String c : counted) {
                    sb.append(' ').append(c).append('=').append(UnityRuntime.$objectCount(c));
                }
                sb.append(" drawn=").append(list.size());
                if (expected != null) {
                    traced++;
                    check(expected.equals(sb.toString()) || expected.startsWith(sb + " "), "frame " + frame
                            + " is `" + sb + "` here and `" + expected + "` in the trace");
                }
                if (dump) {
                    dump(frame, list, view, surface, png, reference);
                }
            }
        }
        if (!trace.isEmpty()) {
            check(traced > 0, "the trace has no frame line this run reached");
            System.out.println("trace: " + traced + " frame lines compared");
        }
        keys(view);
        pointer(view);
        resized(view, surface, png);
        clock(view, surface);
        System.out.println(FAILURES.isEmpty() ? "OFFSCREEN OK" : "OFFSCREEN FAILED: " + FAILURES.size());
        return FAILURES.isEmpty() ? 0 : 1;
    }

    /// Renders a frame that was already stepped: paused, so that the
    /// renderer draws the scene as it stands and does not step it again.
    private static BufferedImage render(UnityGameView view, OffscreenSurface surface) {
        view.pause();
        BufferedImage image = surface.frame(view.getRenderer(), view.getWidth(), view.getHeight());
        view.resume();
        return image;
    }

    private static void dump(int frame, DrawList list, UnityGameView view, OffscreenSurface surface, File png,
            File reference) throws Exception {
        // What the view put on its scene is what the list said.
        Scene scene = view.getScene();
        int visible = 0;
        int texts = 0;
        for (int i = 0; i < scene.size(); i++) {
            Sprite s = scene.get(i);
            if (!s.isVisible()) {
                continue;
            }
            check(visible < list.size(), "frame " + frame + ": more sprites are visible than were drawn");
            if (visible >= list.size()) {
                break;
            }
            DrawCommand d = list.get(visible++);
            check(s.getImage() != null, "frame " + frame + ": a visible sprite has no image: " + d.sprite);
            if (s.getImage() == null) {
                continue;
            }
            check(Math.abs(s.getX() - d.x) < 0.01 && Math.abs(s.getY() - d.y) < 0.01, "frame " + frame
                    + ": a sprite is not where its command put it");
            if (d.text != null) {
                texts++;
                check(s.getImage().getWidth() == (int) d.width && s.getImage().getHeight() == (int) d.height,
                        "frame " + frame + ": the image of text `" + d.text + "` is not the size of its rectangle");
                continue;
            }
            check(s.getImage().getWidth() == d.sourceWidth && s.getImage().getHeight() == d.sourceHeight, "frame "
                    + frame + ": " + d.sprite + " is drawn from an image " + s.getImage().getWidth() + "x"
                    + s.getImage().getHeight() + ", and its part of the sheet is " + d.sourceWidth + "x"
                    + d.sourceHeight);
            check(Math.abs(s.getRotation() - d.rotation) < 0.01 && Math.abs(s.getRenderWidth() - d.width) < 0.01
                    && Math.abs(s.getRenderHeight() - d.height) < 0.01, "frame " + frame + ": " + d.sprite
                    + " has not the rotation or size of its command");
        }
        check(visible == list.size(), "frame " + frame + ": " + visible + " sprites are visible and " + list.size()
                + " were drawn");
        BufferedImage image = render(view, surface);
        int ink = ink(image);
        // Of a scene that draws: one with no renderer in it is a blank frame,
        // and rightly.
        check(ink > 0 || list.size() == 0, "frame " + frame + ": " + list.size() + " commands were drawn and "
                + "nothing was rasterised");
        StringBuilder sb = new StringBuilder();
        sb.append("dump ").append(frame).append(" sprites=").append(visible).append(" texts=").append(texts)
                .append(" ink=").append(ink);
        if (png != null) {
            write(image, new File(png, "frame-" + frame + ".png"));
        }
        File ref = reference == null ? null : new File(reference, "frame-" + frame + ".png");
        if (ref != null && ref.isFile()) {
            BufferedImage other = ImageIO.read(ref);
            int diff = difference(image, other);
            sb.append(" reference-ink=").append(ink(other)).append(" differing=").append(diff / 100).append('.')
                    .append(diff % 100 / 10).append(diff % 10).append('%');
            check(diff >= 0, "frame " + frame + ": the reference image is not the size of the frame");
        }
        System.out.println(sb);
    }

    private static boolean held(int unityKey) {
        return Input.GetKey(unityKey);
    }

    /// The keys a desktop has, each through the port's entry and under
    /// the port's code, read back from Unity's `Input` after a frame.
    private static void keys(UnityGameView view) {
        double dt = 1f / 60;
        int[][] cases = {
            // port code, Unity key code
            {OffscreenImplementation.GAME_KEY_CODE_UP, 273}, {OffscreenImplementation.GAME_KEY_CODE_DOWN, 274},
            {OffscreenImplementation.GAME_KEY_CODE_RIGHT, 275}, {OffscreenImplementation.GAME_KEY_CODE_LEFT, 276},
            {'w', 'w'}, {'a', 'a'}, {'s', 's'}, {'d', 'd'}, {'W', 'w'}, {'p', 'p'}, {'1', '1'},
            {OffscreenImplementation.GAME_KEY_CODE_FIRE, 32}, {OffscreenImplementation.GAME_KEY_CODE_FIRE, 13},
            {27, 27},
        };
        for (int[] c : cases) {
            key(c[0], true);
            view.frame(dt);
            check(held(c[1]) && Input.GetKeyDown(c[1]), "port key " + c[0] + " did not press Unity key " + c[1]);
            view.frame(dt);
            check(held(c[1]) && !Input.GetKeyDown(c[1]), "Unity key " + c[1] + " is not held, or went down twice");
            key(c[0], false);
            view.frame(dt);
            check(!held(c[1]), "port key " + c[0] + " did not release Unity key " + c[1]);
        }
        // The two axes a game steers with, by the keys this project gives
        // them. Unity's own settings give the arrows and WASD; a project with
        // settings of its own has what it kept of those, which may be one
        // axis, other keys, or neither.
        String axes = axis(view, "Horizontal") + ", " + axis(view, "Vertical");
        // Three keys held together, let go in the order they went down:
        // turning while thrusting while firing.
        key(OffscreenImplementation.GAME_KEY_CODE_LEFT, true);
        key(OffscreenImplementation.GAME_KEY_CODE_UP, true);
        key(OffscreenImplementation.GAME_KEY_CODE_FIRE, true);
        view.frame(dt);
        check(held(276) && held(273) && held(32), "three keys held together are not all down");
        key(OffscreenImplementation.GAME_KEY_CODE_LEFT, false);
        view.frame(dt);
        check(!held(276) && held(273) && held(32), "left is stuck: its release, with two keys pressed after it, "
                + "was lost");
        key(OffscreenImplementation.GAME_KEY_CODE_UP, false);
        key(OffscreenImplementation.GAME_KEY_CODE_FIRE, false);
        view.frame(dt);
        check(!Input.get_anyKey(), "a key of three is stuck");
        // Shift let go before the letter: down as `D`, up as `d`.
        key('D', true);
        view.frame(dt);
        check(held('d'), "`D` did not press d");
        key('d', false);
        view.frame(dt);
        check(!held('d'), "d is stuck after going down as `D` and up as `d`");
        // A key that repeats while held.
        key('a', true);
        view.frame(dt);
        key('a', true);
        key('a', true);
        view.frame(dt);
        check(held('a') && !Input.GetKeyDown('a'), "a repeating key went down again");
        key('a', false);
        view.frame(dt);
        check(!held('a'), "a is stuck after repeating");
        // A key held for longer than the display waits before it calls a
        // held key repeating, which is 800 ms by the wall clock. The repeat
        // is the display's own, timed on the event dispatch thread, so it
        // is the one thing here that depends on how long the frames took:
        // a component's answer to it is a press and a release, and a view
        // that passed those on let go of a direction the player still held.
        // The only trace of it was a run on a slow machine that parted from
        // its trace part of the way through a long hold.
        key(OffscreenImplementation.GAME_KEY_CODE_RIGHT, true);
        view.frame(dt);
        check(held(275) && Input.GetKeyDown(275), "right did not go down");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (int i = 0; i < 5; i++) {
            settle();
            view.frame(dt);
            check(held(275), "right was let go by the display's key repeat, in frame " + i + " after it began");
            check(!Input.GetKeyDown(275) && !Input.GetKeyUp(275), "the display's key repeat pressed or released "
                    + "right, which is held, in frame " + i + " after it began");
        }
        key(OffscreenImplementation.GAME_KEY_CODE_RIGHT, false);
        view.frame(dt);
        check(!held(275) && Input.GetKeyUp(275), "right is stuck after repeating");
        // Down and up between two frames is one frame of down.
        key(OffscreenImplementation.GAME_KEY_CODE_FIRE, true);
        key(OffscreenImplementation.GAME_KEY_CODE_FIRE, false);
        view.frame(dt);
        check(Input.GetKeyDown(32) && !held(32), "a tap between two frames was lost");
        view.frame(dt);
        check(!Input.get_anyKey(), "a key is still held when none is");
        System.out.println("keys: " + cases.length + " keys, shift, repeat, held past the repeat delay and tap "
                + "checked; " + axes);
    }

    /// The port's code for a Unity key code, or [#NO_PORT_KEY].
    private static int portCodeOfUnity(int unityKey) {
        switch (unityKey) {
            case 273:
                return OffscreenImplementation.GAME_KEY_CODE_UP;
            case 274:
                return OffscreenImplementation.GAME_KEY_CODE_DOWN;
            case 275:
                return OffscreenImplementation.GAME_KEY_CODE_RIGHT;
            case 276:
                return OffscreenImplementation.GAME_KEY_CODE_LEFT;
            case 13:
                return '\n';
            case 27:
                return 27;
            default:
                return (unityKey >= 'a' && unityKey <= 'z') || (unityKey >= '0' && unityKey <= '9') || unityKey == ' '
                        ? unityKey : NO_PORT_KEY;
        }
    }

    /// Presses every key the project's input settings give an axis, each
    /// through the port, and reads the axis back: -1 for a negative key, 1
    /// for a positive one, and 0 again once it is let go. Answers what it
    /// did, for the line that reports the keys.
    private static String axis(UnityGameView view, String name) {
        if (!Input.$hasAxis(name)) {
            return name + " skipped (the project's input settings have no such axis)";
        }
        double dt = 1f / 60;
        int[] keys = Input.$axisKeys(name);
        int pressed = 0;
        for (int i = 0; i < keys.length; i++) {
            int port = keys[i] == 0 ? NO_PORT_KEY : portCodeOfUnity(keys[i]);
            if (port == NO_PORT_KEY) {
                // No key in this place, or one a keyboard here cannot press:
                // a joystick button, a mouse button.
                continue;
            }
            float want = i % 2 == 0 ? -1f : 1f;
            key(port, true);
            view.frame(dt);
            check(Input.GetAxisRaw(name) == want, name + " is " + Input.GetAxisRaw(name) + " with Unity key "
                    + keys[i] + " held, which the project's input settings make " + want);
            key(port, false);
            view.frame(dt);
            check(Input.GetAxisRaw(name) == 0f, name + " is stuck at " + Input.GetAxisRaw(name) + " after Unity key "
                    + keys[i] + " was let go");
            pressed++;
        }
        return pressed == 0 ? name + " skipped (no key of a keyboard drives it in this project)"
                : name + " by its " + pressed + " keys";
    }

    private static void pointer(UnityGameView view) {
        double dt = 1f / 60;
        int x = view.getAbsoluteX() + 100;
        int y = view.getAbsoluteY() + 40;
        if (Input.get_touchSupported() && !Input.get_simulateMouseWithTouches()) {
            // The project has told Unity that a finger is not the mouse, and
            // this display is a touch screen: a press is a touch and nothing
            // else, and a mouse button that went down would be the bug.
            impl.portPointerPressed(x, y);
            settle();
            view.frame(dt);
            check(Input.get_touchCount() == 1, "a pointer press is " + Input.get_touchCount() + " touches");
            check(!Input.GetMouseButton(0), "a finger pressed mouse button 0 with Input.simulateMouseWithTouches "
                    + "off");
            impl.portPointerReleased(x, y);
            settle();
            // The frame that reports the touch as ended, and the one after.
            view.frame(dt);
            view.frame(dt);
            check(Input.get_touchCount() == 0, "a finger is stuck after its release: " + Input.get_touchCount());
            check(view.getComponentForm().getFocused() == view, "a touch moved the focus off the game view, to "
                    + view.getComponentForm().getFocused());
            key('w', true);
            view.frame(dt);
            check(held('w'), "keys no longer arrive after a touch");
            key('w', false);
            view.frame(dt);
            System.out.println("pointer: touch, release, focus checked; the mouse skipped (the project turned "
                    + "Input.simulateMouseWithTouches off, and this display is a touch screen)");
            return;
        }
        impl.portPointerPressed(x, y);
        settle();
        view.frame(dt);
        Vector3 at = Input.get_mousePosition(new Vector3());
        check(Input.GetMouseButton(0), "a pointer press did not press mouse button 0");
        check(at.x == 100f && at.y == view.getHeight() - 40f, "the pointer is at " + at.x + "," + at.y
                + " for Unity, and was pressed at 100,40 from the top of a view " + view.getHeight() + " high");
        impl.portPointerReleased(x, y);
        settle();
        view.frame(dt);
        check(!Input.GetMouseButton(0), "a pointer release did not release mouse button 0");
        check(view.getComponentForm().getFocused() == view, "a click moved the focus off the game view, to "
                + view.getComponentForm().getFocused());
        // A press that lands while a frame is running: after the frame has
        // read the pointer and before it ends. On a port that draws on a
        // thread of its own that is where the event dispatch thread puts one
        // press in every few, and a frame that cleared the flag of a press it
        // had not read left the button up in Unity for as long as it was
        // held. A call made in the frame runs at exactly that point.
        final UnityGameView target = view;
        final int[] px = {x};
        final int[] py = {y};
        view.callInFrame(new Runnable() {
            public void run() {
                target.pointerPressed(px, py);
            }
        });
        view.frame(dt);
        view.frame(dt);
        check(Input.GetMouseButton(0) && Input.GetMouseButtonDown(0), "a press that arrived during a frame was lost");
        view.frame(dt);
        check(Input.GetMouseButton(0) && !Input.GetMouseButtonDown(0), "a held button is not held, or went down twice");
        view.callInFrame(new Runnable() {
            public void run() {
                target.pointerReleased(px, py);
            }
        });
        view.frame(dt);
        view.frame(dt);
        check(!Input.GetMouseButton(0) && Input.GetMouseButtonUp(0), "a release that arrived during a frame was lost");
        // And a whole click inside one frame is one frame of down.
        view.callInFrame(new Runnable() {
            public void run() {
                target.pointerPressed(px, py);
                target.pointerReleased(px, py);
            }
        });
        view.frame(dt);
        view.frame(dt);
        check(Input.GetMouseButtonDown(0) && !Input.GetMouseButton(0), "a click inside one frame was lost");
        view.frame(dt);
        check(!Input.GetMouseButton(0) && !Input.GetMouseButtonDown(0), "the button is stuck after a click inside "
                + "one frame");
        // The finger of it is a touch that began and then one that ended.
        view.frame(dt);
        check(Input.get_touchCount() == 0, "a finger is stuck after a click inside one frame: "
                + Input.get_touchCount());
        key('w', true);
        view.frame(dt);
        check(held('w'), "keys no longer arrive after a click");
        key('w', false);
        view.frame(dt);
        System.out.println("pointer: press, position, release, focus and events during a frame checked");
    }

    /// The window grows: the view follows, and so does what Unity is told.
    private static void resized(UnityGameView view, OffscreenSurface surface, File png) throws Exception {
        int width = view.getWidth() * 4 / 3;
        int height = view.getHeight() * 4 / 3;
        impl.portSizeChanged(width, height);
        settle();
        view.frame(1f / 60);
        check(view.getWidth() == width && view.getHeight() == height, "the view is " + view.getWidth() + "x"
                + view.getHeight() + " after the display became " + width + "x" + height);
        check(com.codename1.unitycompat.unityengine.Screen.get_width() == width
                && com.codename1.unitycompat.unityengine.Screen.get_height() == height, "Screen is "
                + com.codename1.unitycompat.unityengine.Screen.get_width() + "x"
                + com.codename1.unitycompat.unityengine.Screen.get_height() + " after the resize");
        int drawn = UnityRuntime.render().size();
        BufferedImage image = render(view, surface);
        check(image.getWidth() == width && image.getHeight() == height, "the frame is " + image.getWidth() + "x"
                + image.getHeight() + " after the resize");
        // A scene with nothing to draw -- one that is all physics, or all
        // scripts -- is a blank frame at any size.
        check(ink(image) > 0 || drawn == 0, drawn + " commands were drawn and nothing was rasterised at the new "
                + "size");
        if (png != null) {
            write(image, new File(png, "resized-" + width + "x" + height + ".png"));
        }
        System.out.println("resized: " + width + "x" + height + " ink=" + ink(image) + (drawn == 0
                ? " (the scene draws nothing: pixels not expected)" : ""));
    }

    /// Frames as the port drives them: the renderer steps the view, by
    /// the clock. The clock is Unity's unscaled time; `Time.time` follows it
    /// at the scale the scripts have set, which is nothing at all for a game
    /// waiting behind its menu with `Time.timeScale = 0`.
    private static void clock(UnityGameView view, OffscreenSurface surface) throws Exception {
        float before = com.codename1.unitycompat.unityengine.Time.get_time();
        float realBefore = com.codename1.unitycompat.unityengine.Time.get_unscaledTime();
        float scale = com.codename1.unitycompat.unityengine.Time.get_timeScale();
        long spent = 0;
        long began = System.nanoTime();
        for (int i = 0; i < 5; i++) {
            long from = System.nanoTime();
            surface.frame(view.getRenderer(), view.getWidth(), view.getHeight());
            spent += System.nanoTime() - from;
            Thread.sleep(20);
        }
        float moved = com.codename1.unitycompat.unityengine.Time.get_time() - before;
        float real = com.codename1.unitycompat.unityengine.Time.get_unscaledTime() - realBefore;
        // Held to the time that really went by, and not to a guess at how
        // long five frames take: on a loaded machine they take what they
        // take. The first of them also counts what passed since the frame
        // before these, which the renderer caps at a quarter of a second.
        float wall = (System.nanoTime() - began) / 1e9f;
        check(real > 0.05f && real < wall + 0.4f, "five frames by the clock, 20 ms apart, moved Time.unscaledTime "
                + "by " + real + " in " + wall + " s");
        if (scale != com.codename1.unitycompat.unityengine.Time.get_timeScale()) {
            // A script changed the scale during these frames; what Time.time
            // should have done is the script's business.
            System.out.println("clock: Time.timeScale changed during the frames; Time.time not checked");
        } else if (scale == 0f) {
            check(moved == 0f, "Time.time moved by " + moved + " with Time.timeScale at 0");
        } else {
            check(moved > 0.05f * scale && moved < (wall + 0.4f) * scale, "five frames by the clock, 20 ms apart, "
                    + "moved Time.time by " + moved + " in " + wall + " s with Time.timeScale at " + scale);
        }
        System.out.println("clock: 5 renderer-driven frames advanced Time.unscaledTime by " + (int) (real * 1000)
                + " ms and Time.time, at a scale of " + scale + ", by " + (int) (moved * 1000) + " ms; a frame of "
                + view.getWidth() + "x" + view.getHeight() + " took the software rasteriser " + spent / 5000000
                + " ms");
    }
}
