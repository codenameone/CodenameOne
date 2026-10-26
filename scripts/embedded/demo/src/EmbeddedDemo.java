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

import com.codename1.hardware.GpioPin;
import com.codename1.hardware.Hardware;
import com.codename1.hardware.HardwareException;
import com.codename1.impl.linux.LinuxImplementation;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Slider;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.UIManager;

/// A small appliance HMI: a live gauge, a GPIO toggle, the backlight and the
/// buses the board exposes. It is also the benchmark workload: started with
/// the environment variable `CN1_BENCH_SECONDS`, it prints the time to its
/// first painted frame, animates the gauge for that many seconds, prints the
/// frame rate and the resident memory, and exits.
public class EmbeddedDemo {

    static long startNanos;
    static int benchSeconds;
    static int frames;
    static boolean firstFramePrinted;

    public static void main(String[] args) {
        startNanos = System.nanoTime();
        String bench = System.getenv("CN1_BENCH_SECONDS");
        benchSeconds = bench == null ? 0 : Integer.parseInt(bench.trim());
        Display.init(null);
        if (Display.getInstance().getResourceAsStream(EmbeddedDemo.class, "/theme.res") != null) {
            UIManager.initFirstTheme("/theme");
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                show();
            }
        });
        LinuxImplementation.runMainEventLoop();
    }

    static void show() {
        Form f = new Form("Codename One Embedded", new BorderLayout());
        final Gauge gauge = new Gauge();
        f.add(BorderLayout.CENTER, gauge);

        Container controls = new Container(new GridLayout(1, 3));
        final Hardware hw = Hardware.getInstance();
        final Button led = new Button("LED off");
        led.addActionListener(e -> toggleLed(hw, led));
        Slider backlight = new Slider();
        backlight.setEditable(true);
        backlight.setMinValue(0);
        backlight.setMaxValue(100);
        backlight.setProgress(100);
        backlight.addDataChangedListener((type, index) -> setBacklight(hw, index));
        Label buses = new Label(busSummary(hw));
        controls.add(led).add(backlight).add(buses);
        f.add(BorderLayout.SOUTH, controls);
        f.show();
        f.registerAnimated(gauge);
        if (benchSeconds > 0) {
            final long benchStart = System.currentTimeMillis();
            final long cpuStart = cpuTicks();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(benchSeconds * 1000L);
                    } catch (InterruptedException ignored) {
                        return;
                    }
                    long elapsed = System.currentTimeMillis() - benchStart;
                    long cpu = (cpuTicks() - cpuStart) * 10L;
                    System.out.println("CN1BENCH fps=" + (frames * 1000L / Math.max(1, elapsed))
                            + " frames=" + frames + " rss_kb=" + rssKb()
                            + " cpu_pct=" + (cpu * 100L / Math.max(1, elapsed)));
                    System.exit(0);
                }
            }, "bench").start();
        }
    }

    static GpioPin ledPin;

    static void toggleLed(Hardware hw, Button led) {
        try {
            if (ledPin == null) {
                ledPin = hw.openGpioOutput("gpiochip0", 17, false);
            }
            ledPin.setHigh(!ledPin.isHigh());
            led.setText(ledPin.isHigh() ? "LED on" : "LED off");
        } catch (HardwareException e) {
            led.setText("No GPIO");
        }
    }

    static void setBacklight(Hardware hw, int percent) {
        int max = hw.getMaxBacklightBrightness();
        if (max > 0) {
            try {
                hw.setBacklightBrightness(max * percent / 100);
            } catch (HardwareException ignored) {
                // A board without a writable backlight keeps its brightness.
            }
        }
    }

    static String busSummary(Hardware hw) {
        return "GPIO " + hw.listGpioChips().length + "  I2C " + hw.listI2cBuses().length
                + "  SPI " + hw.listSpiDevices().length + "  UART " + hw.listSerialPorts().length;
    }

    /// User plus system time of this process in clock ticks (100 per second),
    /// fields 14 and 15 of `/proc/self/stat`.
    static long cpuTicks() {
        String s = readProc("file:///proc/self/stat");
        if (s == null) {
            return 0;
        }
        String[] f = com.codename1.util.StringUtil.tokenize(s.substring(s.lastIndexOf(')') + 2), ' ').toArray(new String[0]);
        return Long.parseLong(f[11]) + Long.parseLong(f[12]);
    }

    static String readProc(String url) {
        try {
            java.io.InputStream in = com.codename1.io.FileSystemStorage.getInstance().openInputStream(url);
            byte[] b = new byte[1024];
            int n = in.read(b);
            in.close();
            return new String(b, 0, Math.max(0, n), "UTF-8").trim();
        } catch (Exception e) {
            return null;
        }
    }

    static long rssKb() {
        try {
            java.io.InputStream in = com.codename1.io.FileSystemStorage.getInstance().openInputStream("file:///proc/self/statm");
            byte[] b = new byte[128];
            int n = in.read(b);
            in.close();
            String s = new String(b, 0, Math.max(0, n), "UTF-8").trim();
            long pages = Long.parseLong(s.substring(s.indexOf(' ') + 1, s.indexOf(' ', s.indexOf(' ') + 1)));
            return pages * 4;
        } catch (Exception e) {
            return -1;
        }
    }

    /// A needle gauge that sweeps continuously: enough drawing per frame
    /// (arcs, text, antialiased lines) to be a fair frame-rate workload.
    static class Gauge extends Component {
        double angle;
        long last;

        public boolean animate() {
            long now = System.currentTimeMillis();
            if (now - last < 1) {
                return false;
            }
            last = now;
            angle += 0.04;
            return true;
        }

        public void paint(Graphics g) {
            frames++;
            if (!firstFramePrinted) {
                firstFramePrinted = true;
                // Wall-clock, so the harness measures from exec rather than from main:
                // the same start every framework in scripts/embedded/bench is held to.
                System.out.println("CN1BENCH first_frame_epoch_ms=" + System.currentTimeMillis()
                        + " first_frame_ms=" + ((System.nanoTime() - startNanos) / 1000000L));
            }
            int w = getWidth();
            int h = getHeight();
            int cx = getX() + w / 2;
            int cy = getY() + h / 2;
            int r = Math.min(w, h) / 2 - 10;
            g.setAntiAliased(true);
            g.setColor(0x101820);
            g.fillRect(getX(), getY(), w, h);
            g.setColor(0x2a3a4a);
            g.fillArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
            g.setColor(0x33aaff);
            int sweep = (int) ((Math.sin(angle) + 1) * 135);
            g.fillArc(cx - r, cy - r, r * 2, r * 2, 225 - sweep, sweep);
            g.setColor(0x101820);
            g.fillArc(cx - r + 24, cy - r + 24, r * 2 - 48, r * 2 - 48, 0, 360);
            double a = Math.toRadians(225 - sweep);
            g.setColor(0xffffff);
            g.drawLine(cx, cy, cx + (int) (Math.cos(a) * (r - 30)), cy - (int) (Math.sin(a) * (r - 30)));
            g.setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
            String v = String.valueOf(sweep * 100 / 270) + " %";
            g.drawString(v, cx - g.getFont().stringWidth(v) / 2, cy + r / 3);
        }
    }
}
