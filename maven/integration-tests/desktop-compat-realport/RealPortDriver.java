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

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Display;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/// Runs a built Codename One application on the real JavaSE port, in a frame
/// set up the way the generated desktop stub sets it up, and plays a script of
/// real input against it: the pointer and the keys are the display server's,
/// sent by `java.awt.Robot`, and nothing is called inside the application.
///
/// It fails -- exit code 1 -- when anything threw, on any thread, or when a
/// step that is input left every pixel of the application as it was: a button
/// that does nothing under a real pointer is the failure this exists to catch,
/// and no test that fires an event at a node can see it.
///
/// Arguments: main class, width, height, script file, output directory.
///
/// The width and height are the size the application's area is opened at. An
/// application that sizes its own window gets the size it asks for, as it does
/// under the desktop stub; the line `REALPORT area:` says what it came to.
///
/// A script line is one step. A position is a fraction of the application's
/// area.
///
/// - `sleep <ms>`
/// - `move <x> <y>`
/// - `click <x> <y>`
/// - `drag <x1> <y1> <x2> <y2> <ms>`
/// - `key <name>` -- a `java.awt.event.KeyEvent` constant without `VK_`
/// - `type <text>` -- letters, digits and spaces
/// - `shot <name>` -- saves the picture, changes nothing
///
/// `click`, `drag`, `key` and `type` have to change the picture within three
/// seconds, outside what was already changing by itself -- a spinner, a caret.
/// Prefix the line with `quiet` for a step that is not meant to.
public final class RealPortDriver {

    private static final List<String> FAILURES = new ArrayList<String>();
    private static final java.util.concurrent.atomic.AtomicInteger THROWN =
            new java.util.concurrent.atomic.AtomicInteger();

    private static Robot robot;
    private static Component canvas;
    private static File out;
    private static int step;

    private RealPortDriver() {
    }

    /// Counts what is printed that reads as a stack trace or names an
    /// exception, and passes everything on.
    private static final class Watch extends OutputStream {
        private final PrintStream to;
        private final StringBuilder line = new StringBuilder();

        Watch(PrintStream to) {
            this.to = to;
        }

        @Override
        public void write(int b) {
            to.write(b);
            if (b == '\n') {
                String s = line.toString();
                line.setLength(0);
                if (s.contains("Exception") || s.startsWith("\tat ")) {
                    THROWN.incrementAndGet();
                }
            } else {
                line.append((char) b);
            }
        }
    }

    public static void main(final String[] a) throws Exception {
        final int w = Integer.parseInt(a[1]);
        final int h = Integer.parseInt(a[2]);
        out = new File(a[4]);
        if (!out.isDirectory() && !out.mkdirs()) {
            throw new IllegalStateException("cannot create " + out);
        }
        System.setOut(new PrintStream(new Watch(System.out), true));
        System.setErr(new PrintStream(new Watch(System.err), true));
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.out.println("Uncaught Exception on " + t.getName());
                e.printStackTrace(System.out);
            }
        });
        // A desktop build packages the theme of its platform as /NativeTheme.res; the
        // classes of a project carry none, so the one a Linux build gets is named.
        JavaSEPort.setNativeTheme(System.getProperty("realport.theme", "/GnomeAdwaitaTheme.res"));
        JavaSEPort.blockMonitors();
        JavaSEPort.setAppHomeDir(".cn1-realport");
        JavaSEPort.setExposeFilesystem(true);
        JavaSEPort.setTablet(true);
        JavaSEPort.setUseNativeInput(true);
        JavaSEPort.setShowEDTViolationStacks(false);
        JavaSEPort.setShowEDTWarnings(false);
        final JFrame frame = new JFrame("cn1-realport");
        JavaSEPort.setDefaultPixelMilliRatio(Toolkit.getDefaultToolkit().getScreenResolution() / 25.4
                * JavaSEPort.getRetinaScale());
        Display.init(frame.getContentPane());
        Display.getInstance().addEdtErrorHandler(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                System.out.println("Exception on the event thread: " + e.getSource());
                if (e.getSource() instanceof Throwable) {
                    ((Throwable) e.getSource()).printStackTrace(System.out);
                }
                e.consume();
            }
        });
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.setUndecorated(true);
                frame.getContentPane().setPreferredSize(new Dimension(w, h));
                frame.pack();
            }
        });
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Class<?> c = Class.forName(a[0]);
                    Object app = c.getConstructor().newInstance();
                    c.getMethod("init", Object.class).invoke(app, new Object[] {null});
                    c.getMethod("start").invoke(app);
                } catch (Throwable t) {
                    System.out.println("Exception starting " + a[0]);
                    t.printStackTrace(System.out);
                }
            }
        });
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                // Shown at whatever size it has by now, as the desktop stub shows it:
                // the frame was packed to the size asked for before the application
                // started, and an application that sizes its own window -- a packed
                // JFrame, a Stage with a Scene of a given size -- has since asked the
                // port for that. Sizing the frame again here took that size away from
                // an application that asked while it started, and not from one that
                // asked a moment later, on the next cycle of the event thread.
                frame.setLocation(40, 40);
                frame.setVisible(true);
                frame.validate();
                frame.toFront();
                canvas = frame.getContentPane().getComponent(0);
                canvas.requestFocus();
            }
        });
        robot = new Robot();
        robot.setAutoDelay(0);
        Thread.sleep(2500);
        // What the fractions of a script are fractions of.
        System.out.println("REALPORT area: " + area().width + "x" + area().height);
        shot("start");
        BufferedReader in = new BufferedReader(new FileReader(a[3]));
        try {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#') {
                    play(line);
                }
            }
        } finally {
            in.close();
        }
        Thread.sleep(500);
        shot("end");
        if (THROWN.get() > 0) {
            FAILURES.add(THROWN.get() + " line(s) of output name an exception");
        }
        for (String f : FAILURES) {
            System.out.println("REALPORT FAIL: " + f);
        }
        System.out.println("REALPORT " + (FAILURES.isEmpty() ? "OK" : "FAILED") + ": " + step + " steps, pictures in "
                + out);
        Runtime.getRuntime().halt(FAILURES.isEmpty() ? 0 : 1);
    }

    private static Rectangle area() {
        Point p = canvas.getLocationOnScreen();
        return new Rectangle(p.x, p.y, canvas.getWidth(), canvas.getHeight());
    }

    private static int x(String fraction) {
        Rectangle r = area();
        return r.x + (int) Math.round(Double.parseDouble(fraction) * r.width);
    }

    private static int y(String fraction) {
        Rectangle r = area();
        return r.y + (int) Math.round(Double.parseDouble(fraction) * r.height);
    }

    private static BufferedImage grab() {
        return robot.createScreenCapture(area());
    }

    private static void shot(String name) throws Exception {
        String n = (step < 10 ? "0" : "") + step + "-" + name.replaceAll("[^A-Za-z0-9_.-]", "_") + ".png";
        ImageIO.write(grab(), "png", new File(out, n));
    }

    /// The pixels that change with nobody touching anything -- a progress
    /// indicator, a blinking caret, a running animation -- found by watching for
    /// longer than a caret's blink and widened by a few pixels. Input is judged
    /// on the rest of the picture, or a click on nothing at all would pass on any
    /// screen that has a spinner on it.
    private static boolean[] moving() throws Exception {
        BufferedImage last = grab();
        int w = last.getWidth();
        int h = last.getHeight();
        boolean[] raw = new boolean[w * h];
        for (int i = 0; i < 7; i++) {
            Thread.sleep(200);
            BufferedImage now = grab();
            if (now.getWidth() != w || now.getHeight() != h) {
                return new boolean[0];
            }
            for (int yy = 0; yy < h; yy++) {
                for (int xx = 0; xx < w; xx++) {
                    if (differs(last.getRGB(xx, yy), now.getRGB(xx, yy))) {
                        raw[yy * w + xx] = true;
                    }
                }
            }
            last = now;
        }
        boolean[] wide = new boolean[w * h];
        int r = 6;
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                if (raw[yy * w + xx]) {
                    for (int dy = Math.max(0, yy - r); dy <= Math.min(h - 1, yy + r); dy++) {
                        for (int dx = Math.max(0, xx - r); dx <= Math.min(w - 1, xx + r); dx++) {
                            wide[dy * w + dx] = true;
                        }
                    }
                }
            }
        }
        return wide;
    }

    private static boolean differs(int p, int q) {
        int d = Math.abs((p >> 16 & 255) - (q >> 16 & 255)) + Math.abs((p >> 8 & 255) - (q >> 8 & 255))
                + Math.abs((p & 255) - (q & 255));
        return d > 24;
    }

    private static int differing(BufferedImage a, BufferedImage b, boolean[] ignored) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return Integer.MAX_VALUE;
        }
        int w = a.getWidth();
        boolean masked = ignored.length == w * a.getHeight();
        int n = 0;
        for (int yy = 0; yy < a.getHeight(); yy++) {
            for (int xx = 0; xx < w; xx++) {
                if (masked && ignored[yy * w + xx]) {
                    continue;
                }
                if (differs(a.getRGB(xx, yy), b.getRGB(xx, yy))) {
                    n++;
                }
            }
        }
        return n;
    }

    private static void play(String line) throws Exception {
        boolean quiet = line.startsWith("quiet ");
        if (quiet) {
            line = line.substring(6).trim();
        }
        String[] t = line.split("\\s+");
        String op = t[0];
        if ("sleep".equals(op)) {
            Thread.sleep(Long.parseLong(t[1]));
            return;
        }
        step++;
        if ("shot".equals(op)) {
            shot(t[1]);
            return;
        }
        if ("move".equals(op)) {
            robot.mouseMove(x(t[1]), y(t[2]));
            Thread.sleep(300);
            return;
        }
        BufferedImage before;
        boolean[] ignored;
        if ("click".equals(op)) {
            robot.mouseMove(x(t[1]), y(t[2]));
            // What hovering changes is not what the click changes.
            Thread.sleep(400);
            ignored = moving();
            before = grab();
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(80);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } else if ("drag".equals(op)) {
            int x1 = x(t[1]);
            int y1 = y(t[2]);
            int x2 = x(t[3]);
            int y2 = y(t[4]);
            long ms = Long.parseLong(t[5]);
            robot.mouseMove(x1, y1);
            Thread.sleep(400);
            ignored = moving();
            before = grab();
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            int n = 12;
            for (int i = 1; i <= n; i++) {
                robot.mouseMove(x1 + (x2 - x1) * i / n, y1 + (y2 - y1) * i / n);
                Thread.sleep(Math.max(1, ms / n));
            }
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } else if ("key".equals(op)) {
            ignored = moving();
            before = grab();
            int code = KeyEvent.class.getField("VK_" + t[1]).getInt(null);
            robot.keyPress(code);
            Thread.sleep(60);
            robot.keyRelease(code);
        } else if ("type".equals(op)) {
            ignored = moving();
            before = grab();
            String text = line.substring(4).trim();
            for (int i = 0; i < text.length(); i++) {
                int code = KeyEvent.getExtendedKeyCodeForChar(text.charAt(i));
                robot.keyPress(code);
                Thread.sleep(40);
                robot.keyRelease(code);
                Thread.sleep(60);
            }
        } else {
            FAILURES.add("step " + step + ": unknown step '" + line + "'");
            return;
        }
        int changed = 0;
        long until = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < until) {
            Thread.sleep(100);
            changed = differing(before, grab(), ignored);
            if (changed > 0) {
                break;
            }
        }
        // Let what the input started come to rest before the picture is kept.
        Thread.sleep(600);
        shot(op);
        System.out.println("REALPORT step " + step + " '" + line + "': " + changed + " pixels changed");
        if (changed == 0 && !quiet) {
            FAILURES.add("step " + step + " '" + line + "': no pixel changed after the input");
        }
    }
}
