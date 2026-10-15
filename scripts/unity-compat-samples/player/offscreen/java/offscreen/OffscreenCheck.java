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
import com.codename1.ui.Image;
import com.codename1.unitycompat.unityengine.DrawCommand;
import com.codename1.unitycompat.unityengine.DrawList;
import com.codename1.unitycompat.unityengine.Input;
import com.codename1.unitycompat.unityengine.UnityRuntime;
import com.codename1.unitycompat.unityengine.ui.Text;
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
            frame(view, dt);
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
        texts(view, surface);
        audio(view);
        multiKey(view);
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
            frame(view, dt);
            check(held(c[1]) && Input.GetKeyDown(c[1]), "port key " + c[0] + " did not press Unity key " + c[1]);
            frame(view, dt);
            check(held(c[1]) && !Input.GetKeyDown(c[1]), "Unity key " + c[1] + " is not held, or went down twice");
            key(c[0], false);
            frame(view, dt);
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
        frame(view, dt);
        check(held(276) && held(273) && held(32), "three keys held together are not all down");
        key(OffscreenImplementation.GAME_KEY_CODE_LEFT, false);
        frame(view, dt);
        check(!held(276) && held(273) && held(32), "left is stuck: its release, with two keys pressed after it, "
                + "was lost");
        key(OffscreenImplementation.GAME_KEY_CODE_UP, false);
        key(OffscreenImplementation.GAME_KEY_CODE_FIRE, false);
        frame(view, dt);
        check(!Input.get_anyKey(), "a key of three is stuck");
        // Shift let go before the letter: down as `D`, up as `d`.
        key('D', true);
        frame(view, dt);
        check(held('d'), "`D` did not press d");
        key('d', false);
        frame(view, dt);
        check(!held('d'), "d is stuck after going down as `D` and up as `d`");
        // A key that repeats while held.
        key('a', true);
        frame(view, dt);
        key('a', true);
        key('a', true);
        frame(view, dt);
        check(held('a') && !Input.GetKeyDown('a'), "a repeating key went down again");
        key('a', false);
        frame(view, dt);
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
        frame(view, dt);
        check(held(275) && Input.GetKeyDown(275), "right did not go down");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (int i = 0; i < 5; i++) {
            settle();
            frame(view, dt);
            check(held(275), "right was let go by the display's key repeat, in frame " + i + " after it began");
            check(!Input.GetKeyDown(275) && !Input.GetKeyUp(275), "the display's key repeat pressed or released "
                    + "right, which is held, in frame " + i + " after it began");
        }
        key(OffscreenImplementation.GAME_KEY_CODE_RIGHT, false);
        frame(view, dt);
        check(!held(275) && Input.GetKeyUp(275), "right is stuck after repeating");
        // Down and up between two frames is one frame of down.
        key(OffscreenImplementation.GAME_KEY_CODE_FIRE, true);
        key(OffscreenImplementation.GAME_KEY_CODE_FIRE, false);
        frame(view, dt);
        check(Input.GetKeyDown(32) && !held(32), "a tap between two frames was lost");
        frame(view, dt);
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
            frame(view, dt);
            check(Input.GetAxisRaw(name) == want, name + " is " + Input.GetAxisRaw(name) + " with Unity key "
                    + keys[i] + " held, which the project's input settings make " + want);
            key(port, false);
            frame(view, dt);
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
            frame(view, dt);
            check(Input.get_touchCount() == 1, "a pointer press is " + Input.get_touchCount() + " touches");
            check(!Input.GetMouseButton(0), "a finger pressed mouse button 0 with Input.simulateMouseWithTouches "
                    + "off");
            impl.portPointerReleased(x, y);
            settle();
            // The frame that reports the touch as ended, and the one after.
            frame(view, dt);
            frame(view, dt);
            check(Input.get_touchCount() == 0, "a finger is stuck after its release: " + Input.get_touchCount());
            check(view.getComponentForm().getFocused() == view, "a touch moved the focus off the game view, to "
                    + view.getComponentForm().getFocused());
            key('w', true);
            frame(view, dt);
            check(held('w'), "keys no longer arrive after a touch");
            key('w', false);
            frame(view, dt);
            System.out.println("pointer: touch, release, focus checked; the mouse skipped (the project turned "
                    + "Input.simulateMouseWithTouches off, and this display is a touch screen)");
            return;
        }
        impl.portPointerPressed(x, y);
        settle();
        frame(view, dt);
        Vector3 at = Input.get_mousePosition(new Vector3());
        check(Input.GetMouseButton(0), "a pointer press did not press mouse button 0");
        check(at.x == 100f && at.y == view.getHeight() - 40f, "the pointer is at " + at.x + "," + at.y
                + " for Unity, and was pressed at 100,40 from the top of a view " + view.getHeight() + " high");
        impl.portPointerReleased(x, y);
        settle();
        frame(view, dt);
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
        frame(view, dt);
        frame(view, dt);
        check(Input.GetMouseButton(0) && Input.GetMouseButtonDown(0), "a press that arrived during a frame was lost");
        frame(view, dt);
        check(Input.GetMouseButton(0) && !Input.GetMouseButtonDown(0), "a held button is not held, or went down twice");
        view.callInFrame(new Runnable() {
            public void run() {
                target.pointerReleased(px, py);
            }
        });
        frame(view, dt);
        frame(view, dt);
        check(!Input.GetMouseButton(0) && Input.GetMouseButtonUp(0), "a release that arrived during a frame was lost");
        // And a whole click inside one frame is one frame of down.
        view.callInFrame(new Runnable() {
            public void run() {
                target.pointerPressed(px, py);
                target.pointerReleased(px, py);
            }
        });
        frame(view, dt);
        frame(view, dt);
        check(Input.GetMouseButtonDown(0) && !Input.GetMouseButton(0), "a click inside one frame was lost");
        frame(view, dt);
        check(!Input.GetMouseButton(0) && !Input.GetMouseButtonDown(0), "the button is stuck after a click inside "
                + "one frame");
        // The finger of it is a touch that began and then one that ended.
        frame(view, dt);
        check(Input.get_touchCount() == 0, "a finger is stuck after a click inside one frame: "
                + Input.get_touchCount());
        key('w', true);
        frame(view, dt);
        check(held('w'), "keys no longer arrive after a click");
        key('w', false);
        frame(view, dt);
        System.out.println("pointer: press, position, release, focus and events during a frame checked");
    }

    /// The window grows: the view follows, and so does what Unity is told.
    private static void resized(UnityGameView view, OffscreenSurface surface, File png) throws Exception {
        int width = view.getWidth() * 4 / 3;
        int height = view.getHeight() * 4 / 3;
        impl.portSizeChanged(width, height);
        settle();
        frame(view, 1f / 60);
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

    /// The image the view shows for the text command that reads `text`, or
    /// null if no command of this frame does.
    /// A frame of the view's logic, on the event dispatch thread. What the
    /// checks read after it -- a sprite for every command, a text's image
    /// the size of its rectangle -- is of a frame that is complete, and a
    /// view whose frame is on another thread has its texts painted on this
    /// one and shows them a frame later; `SceneShots` holds that path.
    private static void frame(final UnityGameView view, final double dt) {
        if (Display.getInstance().isEdt()) {
            view.frame(dt);
            return;
        }
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                view.frame(dt);
            }
        });
    }

    private static Image shown(UnityGameView view, String text) {
        DrawList list = UnityRuntime.render();
        Scene scene = view.getScene();
        int visible = 0;
        for (int i = 0; i < scene.size() && visible < list.size(); i++) {
            Sprite s = scene.get(i);
            if (!s.isVisible()) {
                continue;
            }
            DrawCommand d = list.get(visible++);
            if (text.equals(d.text)) {
                return s.getImage();
            }
        }
        return null;
    }

    /// A text that never repeats must not cost an image and a texture for
    /// each string it ever read. The view paints a text into an image and
    /// the sprite renderer keeps a texture for every image it draws, so
    /// both are counted here over three thousand frames of a label that
    /// changes in every one of them: each frame is the renderer's own, which
    /// steps the scripts, shows the frame and draws it. Then the same label
    /// goes round four strings, which must be the same four images every
    /// time round; and then it is left alone, and what it made must be gone.
    ///
    /// It takes the first UI text of the scene, and says so where there is
    /// none or where the text is not drawn -- behind a canvas that is off,
    /// or written by a script in every frame.
    private static void texts(UnityGameView view, OffscreenSurface surface) throws Exception {
        java.lang.Object found = com.codename1.unitycompat.unityengine.Object.FindObjectOfType(Text.class);
        if (!(found instanceof Text)) {
            System.out.println("texts: the scene has no UI text; the cache of painted texts was not checked");
            return;
        }
        Text label = (Text) found;
        String was = label.get_text();
        int width = view.getWidth();
        int height = view.getHeight();
        label.set_text("offscreen probe");
        surface.frame(view.getRenderer(), width, height);
        if (shown(view, "offscreen probe") == null) {
            label.set_text(was);
            System.out.println("texts: the first UI text of the scene is not drawn as it is set; the cache of "
                    + "painted texts was not checked");
            return;
        }
        int cachedBefore = view.paintedTextCount();
        int texturesBefore = view.getTextureCount();
        int cachedMost = 0;
        int texturesMost = 0;
        int frames = 3000;
        for (int i = 0; i < frames; i++) {
            label.set_text("offscreen " + i);
            surface.frame(view.getRenderer(), width, height);
            cachedMost = Math.max(cachedMost, view.paintedTextCount());
            texturesMost = Math.max(texturesMost, view.getTextureCount());
        }
        // What may wait off the screen is the view's own number, 32; the
        // rest is room for what the scripts of the project did meanwhile.
        int room = 32 + 16;
        check(cachedMost <= cachedBefore + room, "a text that changed in each of " + frames + " frames left "
                + cachedMost + " painted texts in the view's cache, which had " + cachedBefore);
        check(texturesMost <= texturesBefore + room, "a text that changed in each of " + frames + " frames left "
                + "the renderer with " + texturesMost + " textures, which had " + texturesBefore);

        String[] round = {"offscreen north", "offscreen east", "offscreen south", "offscreen west"};
        Image[] first = new Image[round.length];
        boolean same = true;
        for (int i = 0; i < 400; i++) {
            int at = i % round.length;
            label.set_text(round[at]);
            surface.frame(view.getRenderer(), width, height);
            Image image = shown(view, round[at]);
            if (i < round.length) {
                first[at] = image;
            } else {
                same &= image != null && image == first[at];
            }
        }
        check(same, "a text going round four strings was painted again when a string came back");

        // Left alone for longer than the view keeps a text it does not draw.
        label.set_text(was);
        for (int i = 0; i < 200; i++) {
            surface.frame(view.getRenderer(), width, height);
        }
        int cachedAfter = view.paintedTextCount();
        int texturesAfter = view.getTextureCount();
        check(cachedAfter <= cachedBefore + 16 && texturesAfter <= texturesBefore + 16, "200 frames after the text "
                + "stopped changing the view keeps " + cachedAfter + " painted texts and the renderer "
                + texturesAfter + " textures; they were " + cachedBefore + " and " + texturesBefore);
        System.out.println("texts: " + frames + " frames of a text that never repeats: at most " + cachedMost
                + " painted texts (from " + cachedBefore + ") and " + texturesMost + " textures (from "
                + texturesBefore + "); " + cachedAfter + " and " + texturesAfter + " once it was left alone");
    }

    /// The `Media` the core unit tests' implementation hands out for every
    /// sound, which writes down what was asked of it.
    private static final class RecordingMedia implements com.codename1.media.Media {
        final List<String> events = new ArrayList<String>();

        public void play() {
            events.add("play");
        }

        public void pause() {
            events.add("pause");
        }

        public void prepare() {
        }

        public void cleanup() {
            events.add("cleanup");
        }

        public int getTime() {
            return 0;
        }

        public void setTime(int time) {
            events.add("time " + time);
        }

        public int getDuration() {
            return -1;
        }

        public int getVolume() {
            return 100;
        }

        public void setVolume(int vol) {
            events.add("volume " + vol);
        }

        public boolean isPlaying() {
            return false;
        }

        public com.codename1.ui.Component getVideoComponent() {
            return null;
        }

        public boolean isVideo() {
            return false;
        }

        public boolean isFullScreen() {
            return false;
        }

        public void setFullScreen(boolean fullScreen) {
        }

        public boolean isNativePlayerMode() {
            return false;
        }

        public void setNativePlayerMode(boolean nativePlayer) {
        }

        public void setVariable(String key, Object value) {
        }

        public Object getVariable(String key) {
            return null;
        }
    }

    /// A view taken off its form silences its sound and is given a new
    /// output when it is shown again, while a looping source goes on
    /// playing in the game's account. The new output must be given that
    /// source's voice again, where the clip had got to; it used to get
    /// nothing, and the music of a game shown again was silent with
    /// `isPlaying` true. Before `multiKey`, which takes the view away for
    /// good.
    private static void audio(final UnityGameView view) {
        java.lang.Object found = com.codename1.unitycompat.unityengine.Object.FindObjectOfType(
                com.codename1.unitycompat.unityengine.AudioSource.class);
        if (!(found instanceof com.codename1.unitycompat.unityengine.AudioSource)
                || ((com.codename1.unitycompat.unityengine.AudioSource) found).get_clip() == null) {
            System.out.println("audio: the scene has no audio source with a clip; a view shown again was not "
                    + "checked for its sound");
            return;
        }
        final com.codename1.unitycompat.unityengine.AudioSource source =
                (com.codename1.unitycompat.unityengine.AudioSource) found;
        final float length = source.get_clip().get_length();
        final int at = (int) (length * 500f);
        final RecordingMedia media = new RecordingMedia();
        final Display display = Display.getInstance();
        final com.codename1.ui.Container parent = view.getParent();
        final boolean[] seen = new boolean[3];
        final String[] heard = new String[2];
        display.callSeriallyAndWait(new Runnable() {
            public void run() {
                impl.setMedia(media);
                boolean wasLooping = source.get_loop();
                source.set_loop(true);
                source.Play();
                source.set_time(at / 1000f);
                seen[0] = media.events.contains("play");
                heard[0] = media.events.toString();
                parent.removeComponent(view);
                seen[1] = media.events.contains("cleanup");
                media.events.clear();
                parent.add(com.codename1.ui.layouts.BorderLayout.CENTER, view);
                parent.revalidate();
                heard[1] = media.events.toString();
                seen[2] = source.get_isPlaying();
                source.Stop();
                source.set_loop(wasLooping);
                impl.setMedia(null);
            }
        });
        if (!seen[0]) {
            System.out.println("audio: a Play on the view's output made no media (" + heard[0] + "): the clip is "
                    + "not a resource here, or the listener is paused; a view shown again was not checked for its "
                    + "sound");
            return;
        }
        check(seen[1], "the view taken off its form did not silence its output");
        check(seen[2], "a looping source is not playing after its view was shown again");
        check(heard[1].contains("play") && heard[1].contains("time " + at), "a looping source playing at " + at
                + " ms when its view was taken away was not given a voice at that place when the view was shown "
                + "again; its new output heard " + heard[1]);
        System.out.println("audio: a looping source at " + at + " ms, its view taken away and shown again: " + heard[1]);
    }

    /// Multi key mode is the whole application's: a view turns it on while
    /// it is shown and off when it is taken away, and leaves alone a mode
    /// the host had on. Last, since the view is taken off its form.
    private static void multiKey(final UnityGameView view) {
        final Display display = Display.getInstance();
        final com.codename1.ui.Container parent = view.getParent();
        final boolean[] seen = new boolean[4];
        display.callSeriallyAndWait(new Runnable() {
            public void run() {
                seen[0] = display.isMultiKeyMode();
                parent.removeComponent(view);
                seen[1] = display.isMultiKeyMode();
                display.setMultiKeyMode(true);
                parent.add(com.codename1.ui.layouts.BorderLayout.CENTER, view);
                parent.revalidate();
                seen[2] = display.isMultiKeyMode();
                parent.removeComponent(view);
                seen[3] = display.isMultiKeyMode();
                display.setMultiKeyMode(false);
            }
        });
        check(seen[0], "multi key mode is off while the view is shown");
        check(!seen[1], "multi key mode the view turned on is still on after the view was taken away");
        check(seen[2] && seen[3], "the view turned off multi key mode the host had on");
        System.out.println("multi key: shown " + seen[0] + ", taken away " + seen[1] + ", the host's kept "
                + (seen[2] && seen[3]));
    }
}
