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

import com.codename1.generated.unity.UnityAppImpl;
import com.codename1.impl.ImplementationFactory;
import com.codename1.impl.javase.OffscreenSurface;
import com.codename1.testing.OffscreenImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.unitycompat.unityengine.Random;
import com.codename1.unitycompat.unityengine.Time;
import com.codename1.unitycompat.unityengine.UnityRuntime;
import com.codename1.unitycompat.unityengine.ui.UnityGameView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/// Shows every scene of a project by itself, one after the other in one
/// JVM, each stepped by hand to the same frame and then held there, and
/// writes what the view shows as `<Scene>.png`.
///
/// This is what a screenshot test of a scene does on a device -- the
/// `UnityCompat*` tests of `scripts/hellocodenameone` do it for the
/// project in `scripts/unity-compat-samples/gallery` -- with the pieces
/// `OffscreenCheck` describes: the real view and renderer, the JavaSE
/// port's software rasteriser, and no window. It holds the three things
/// such a test relies on:
///
/// - a scene stepped with `UnityGameView.advance` shows the same pixels
///   whenever it is run: with `--twice` every scene is run a second time,
///   after all the others, and must give the same image;
/// - a view whose clock is held stands still: frames drawn after the steps
///   were run change nothing, of the image or of `Time.time`;
/// - `UnityRuntime.reset()` leaves nothing of a scene for the next one: no
///   object is left, and the clock is back at zero.
///
/// ```
/// SceneShots [--seed n] [--frames n] [--size WxH] [--png dir] [--twice]
/// ```
///
/// It exits with 1 and a line for each thing that was wrong.
public final class SceneShots {
    private static final List<String> FAILURES = new ArrayList<String>();

    private SceneShots() {
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            FAILURES.add(what);
            System.out.println("FAILED: " + what);
        }
    }

    private static void settle() {
        for (int i = 0; i < 3; i++) {
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                public void run() {
                }
            });
        }
    }

    public static void main(String[] args) {
        int status = 1;
        try {
            status = run(args);
        } catch (Throwable t) { // NOPMD - anything at all, or the JVM never exits
            System.out.println("FAILED: the check threw " + t);
            t.printStackTrace(System.out);
            System.out.println("SCENES FAILED: threw");
        }
        System.out.flush();
        System.exit(status);
    }

    /// The name of a scene: its file's, without the directory and `.unity`.
    static String name(String path) {
        String n = path.substring(path.lastIndexOf('/') + 1);
        return n.endsWith(".unity") ? n.substring(0, n.length() - 6) : n;
    }

    private static boolean same(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BufferedImage copy(BufferedImage image) {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.createGraphics().drawImage(image, 0, 0, null);
        return rgb;
    }

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

    private static int run(String[] args) throws Exception {
        int seed = 1;
        int frames = 90;
        int width = 400;
        int height = 300;
        File png = null;
        boolean twice = false;
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
            } else if (a.equals("--png")) {
                png = new File(args[++i]);
                png.mkdirs();
            } else if (a.equals("--twice")) {
                twice = true;
            } else {
                System.err.println("SceneShots: unknown option " + a);
                return 2;
            }
        }
        final OffscreenImplementation impl = new OffscreenImplementation();
        impl.setDisplaySize(width, height);
        ImplementationFactory.setInstance(new ImplementationFactory() {
            @Override
            public Object createImplementation() {
                return impl;
            }
        });
        Display.init(null);
        // The project's scenes are asked of it once it is installed.
        UnityRuntime.reset();
        UnityAppImpl.install();
        int scenes = UnityRuntime.$sceneCount();
        String[] names = new String[scenes];
        for (int i = 0; i < scenes; i++) {
            names[i] = name(UnityRuntime.$scenePath(i));
        }
        UnityRuntime.reset();
        check(scenes > 0, "the project has no scene in its build settings");
        BufferedImage[] first = new BufferedImage[scenes];
        for (int pass = 0; pass < (twice ? 2 : 1); pass++) {
            for (int i = 0; i < scenes; i++) {
                BufferedImage image = shoot(i, names[i], seed, frames);
                if (pass == 0) {
                    first[i] = image;
                    if (png != null) {
                        ImageIO.write(image, "png", new File(png, names[i] + ".png"));
                    }
                    System.out.println("scene " + names[i] + " frame=" + frames + " ink=" + ink(image));
                } else {
                    check(same(first[i], image), names[i] + ": run again after the other scenes, it shows another image");
                }
            }
        }
        System.out.println(FAILURES.isEmpty() ? "SCENES OK: " + scenes : "SCENES FAILED: " + FAILURES.size());
        return FAILURES.isEmpty() ? 0 : 1;
    }

    /// One scene, from an empty runtime to an empty runtime.
    private static BufferedImage shoot(final int index, String name, final int seed, final int frames)
            throws Exception {
        final Display display = Display.getInstance();
        final UnityGameView[] made = new UnityGameView[1];
        final Form[] shown = new Form[1];
        display.callSeriallyAndWait(new Runnable() {
            public void run() {
                UnityRuntime.reset();
                Random.InitState(seed);
                UnityAppImpl.install();
                UnityGameView view = new UnityGameView();
                view.installServices();
                UnityRuntime.resize(display.getDisplayWidth(), display.getDisplayHeight());
                UnityRuntime.begin(index);
                Form form = new Form(new BorderLayout());
                form.getTitleArea().setHidden(true);
                if (form.getToolbar() != null) {
                    form.getToolbar().setHidden(true);
                }
                form.setScrollable(false);
                form.getContentPane().getAllStyles().setPadding(0, 0, 0, 0);
                form.getContentPane().getAllStyles().setMargin(0, 0, 0, 0);
                form.add(BorderLayout.CENTER, view);
                form.show();
                // Held before it starts: the frames a display draws before
                // the steps are asked for must not move anything.
                view.holdClock();
                view.start();
                made[0] = view;
                shown[0] = form;
            }
        });
        settle();
        UnityGameView view = made[0];
        check(UnityRuntime.$activeScene() == index, name + ": scene " + UnityRuntime.$activeScene() + " is loaded");
        OffscreenSurface surface = new OffscreenSurface();
        // Frames before the steps are asked for, as a display draws them.
        surface.frame(view.getRenderer(), view.getWidth(), view.getHeight());
        surface.frame(view.getRenderer(), view.getWidth(), view.getHeight());
        check(Time.get_frameCount() == 0, name + ": a held view stepped " + Time.get_frameCount()
                + " frames nobody asked for");
        int before = view.framesAdvanced();
        view.advance(frames, 1f / 60);
        BufferedImage image = copy(surface.frame(view.getRenderer(), view.getWidth(), view.getHeight()));
        check(view.framesAdvanced() - before == frames, name + ": " + (view.framesAdvanced() - before)
                + " frames were advanced, not " + frames);
        check(Time.get_frameCount() == frames, name + ": Time.frameCount is " + Time.get_frameCount() + " after "
                + frames + " steps");
        float time = Time.get_time();
        check(ink(image) > 0, name + ": nothing was drawn");
        for (int i = 0; i < 5; i++) {
            BufferedImage later = surface.frame(view.getRenderer(), view.getWidth(), view.getHeight());
            check(same(image, copy(later)), name + ": the held view changed, " + (i + 1) + " frames after its steps");
        }
        check(Time.get_time() == time && Time.get_frameCount() == frames, name + ": time passed in a held view");
        final UnityGameView done = view;
        display.callSeriallyAndWait(new Runnable() {
            public void run() {
                done.stop();
                done.remove();
                UnityRuntime.reset();
            }
        });
        settle();
        check(UnityRuntime.$objectCount() == 0, name + ": " + UnityRuntime.$objectCount()
                + " objects are left after the reset");
        check(Time.get_frameCount() == 0 && Time.get_time() == 0f, name + ": the clock is not back at zero");
        return image;
    }
}
