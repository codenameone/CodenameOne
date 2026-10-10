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
package headless;

import com.codename1.generated.unity.UnityAppImpl;
import com.codename1.unitycompat.unityengine.AudioSource;
import com.codename1.unitycompat.unityengine.DrawCommand;
import com.codename1.unitycompat.unityengine.DrawList;
import com.codename1.unitycompat.unityengine.Input;
import com.codename1.unitycompat.unityengine.PlayerPrefs;
import com.codename1.unitycompat.unityengine.Random;
import com.codename1.unitycompat.unityengine.UnityRuntime;
import java.util.ArrayList;

/// Runs any compiled Unity project with no display and prints what
/// happened, as lines of integers.
///
/// It is the part of the headless driver that every target can run: it
/// uses nothing but the runtime and `System.out`, so the same class goes
/// through a JVM, ParparVM's C target and its JavaScript one, and the three
/// traces can be compared line for line. Everything it prints is an
/// integer -- pixels and degrees in hundredths -- because how a float is
/// *printed* is not what is being compared.
///
/// The keyboard is scripted: one line for each event,
/// `<frame> <key> down|up`, the key by the name Unity's input settings
/// use (`up`, `left`, `space`, `a`). An event of frame *n* is delivered
/// before frame *n* is stepped. A `#` starts a comment.
///
/// The pointer is scripted the same way, with `pointer:<x>,<y>` where a
/// key's name would be: whole pixels from the view's top left, which is
/// how a host reports a touch. Beside `down` and `up` a pointer line may
/// end in `move`, the pointer going somewhere while it is held.
///
/// Fingers are scripted as a host reports them: where all of them now
/// are. `<frame> touch <x>,<y>;<x>,<y>` puts fingers at those pixels,
/// one pair for each finger and in any order, and `<frame> touch none`
/// lifts the last. A script with such a line runs on a host that has a
/// touch screen, and the first finger of each report is the pointer too,
/// pressed, moved and released, as it is on a device.
///
/// Two more lines drive what a host application does to a game rather
/// than what a player does. `<frame> app pause` and `<frame> app resume`
/// are the application going to the background and coming back.
/// `<frame> host <word>` hands the word to the [Host], through
/// `UnityRuntime.callInFrame` -- the way code outside the frame reaches a
/// script -- so the host's answer runs inside frame *n*.
public final class HeadlessTrace {
    /// Receives each dumped frame's draw list, for a driver that also
    /// wants to paint it.
    public interface Sink {
        void frame(int frame, DrawList list);
    }

    /// The Java side of a project that has one: what an application's
    /// main class would do around the game.
    public interface Host {
        /// The project is installed and no scene has been built: the
        /// place `UnityApplication.onProjectInstalled` gives an
        /// application.
        void installed();

        /// A `host` line of the script, called inside the frame.
        void call(String word);
    }

    public int seed = 1;
    public int frames = 600;
    public int width = 960;
    public int height = 540;
    /// Frames a second; the step is its reciprocal.
    public int rate = 60;
    public String script = "";
    /// The frames whose draw list is printed.
    public int[] dumps = new int[0];
    /// How many commands of a dumped frame are printed in full.
    public int commands = 12;
    /// Every this many frames, a line of object counts.
    public int countEvery = 60;
    /// Name prefixes counted on that line, beside the total.
    public String[] counted = new String[0];
    public Sink sink;
    /// Null for a project that is only scripts and scenes.
    public Host host;

    private int[] eventFrames = new int[0];
    private int[] eventKeys = new int[0];
    private boolean[] eventDown = new boolean[0];
    /// Where a pointer event is, in pixels from the top left; unused for
    /// a key, whose code is not zero.
    private int[] eventX = new int[0];
    private int[] eventY = new int[0];
    /// The word of a `host` or an `app` line, null for a key or a pointer.
    private String[] eventWord = new String[0];
    private boolean[] eventHost = new boolean[0];
    private boolean[] eventMove = new boolean[0];
    /// The fingers of a `touch` line as x and y pairs, null for any other.
    private int[][] eventTouch = new int[0][];
    private boolean touchScreen;
    private boolean fingerDown;
    private int fingerX;
    private int fingerY;

    /// Reads the script into parallel arrays. A line that is not an event
    /// is an error: a typo that silently pressed nothing would produce a
    /// trace that looks like a result.
    private void parse() {
        ArrayList lines = new ArrayList();
        int at = 0;
        while (at <= script.length()) {
            int end = script.indexOf('\n', at);
            if (end < 0) {
                end = script.length();
            }
            String line = script.substring(at, end);
            int comment = line.indexOf('#');
            if (comment >= 0) {
                line = line.substring(0, comment);
            }
            line = line.trim();
            if (line.length() > 0) {
                lines.add(line);
            }
            at = end + 1;
        }
        int n = lines.size();
        eventFrames = new int[n];
        eventKeys = new int[n];
        eventDown = new boolean[n];
        eventX = new int[n];
        eventY = new int[n];
        eventWord = new String[n];
        eventHost = new boolean[n];
        eventMove = new boolean[n];
        eventTouch = new int[n][];
        for (int i = 0; i < n; i++) {
            String line = (String) lines.get(i);
            int a = line.indexOf(' ');
            int b = line.lastIndexOf(' ');
            if (a < 0 || b <= a) {
                throw new IllegalArgumentException("input script: not `<frame> <key> down|up`: " + line);
            }
            String key = line.substring(a + 1, b).trim();
            String what = line.substring(b + 1);
            eventFrames[i] = Integer.parseInt(line.substring(0, a));
            if (key.equals("host") || key.equals("app")) {
                eventHost[i] = key.equals("host");
                eventWord[i] = what;
                if (eventHost[i] ? host == null : !what.equals("pause") && !what.equals("resume")) {
                    throw new IllegalArgumentException("input script: not `<frame> app pause|resume`, or a"
                            + " `host` line with no host: " + line);
                }
                continue;
            }
            if (key.equals("touch")) {
                eventTouch[i] = fingers(what, line);
                touchScreen = true;
                continue;
            }
            eventDown[i] = what.equals("down");
            eventMove[i] = what.equals("move") && key.startsWith("pointer:");
            int comma = key.indexOf(',');
            boolean pointer = key.startsWith("pointer:") && comma > 8;
            if (pointer) {
                eventX[i] = Integer.parseInt(key.substring(8, comma));
                eventY[i] = Integer.parseInt(key.substring(comma + 1));
            } else {
                eventKeys[i] = Input.$keyCode(key);
            }
            if ((!pointer && eventKeys[i] == 0) || (!eventDown[i] && !eventMove[i] && !what.equals("up"))) {
                throw new IllegalArgumentException("input script: not `<frame> <key> down|up`: " + line);
            }
        }
    }

    /// The fingers of a `touch` line: `none`, or `x,y` pairs with `;`
    /// between them.
    private static int[] fingers(String what, String line) {
        if (what.equals("none")) {
            return new int[0];
        }
        int count = 1;
        for (int i = 0; i < what.length(); i++) {
            if (what.charAt(i) == ';') {
                count++;
            }
        }
        int[] out = new int[count * 2];
        int at = 0;
        for (int i = 0; i < count; i++) {
            int end = what.indexOf(';', at);
            if (end < 0) {
                end = what.length();
            }
            int comma = what.indexOf(',', at);
            if (comma < 0 || comma > end) {
                throw new IllegalArgumentException("input script: not `<frame> touch <x>,<y>;...|none`: " + line);
            }
            out[i * 2] = Integer.parseInt(what.substring(at, comma));
            out[i * 2 + 1] = Integer.parseInt(what.substring(comma + 1, end));
            at = end + 1;
        }
        return out;
    }

    /// Reports the fingers of a `touch` line the way a host does: all of
    /// them as touches, and the first as the pointer.
    private void touch(int frame, int[] points) {
        int n = points.length / 2;
        float[] xs = new float[n];
        float[] ys = new float[n];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            xs[i] = points[i * 2];
            // A host counts down from the top, Unity up from the bottom.
            ys[i] = height - points[i * 2 + 1];
            sb.append(i == 0 ? "" : ";").append(points[i * 2]).append(',').append(points[i * 2 + 1]);
        }
        UnityRuntime.touches(xs, ys, n);
        if (n > 0) {
            fingerX = points[0];
            fingerY = points[1];
            if (fingerDown) {
                UnityRuntime.pointerMoved(fingerX, height - fingerY);
            } else {
                UnityRuntime.pointerPressed(fingerX, height - fingerY);
            }
            fingerDown = true;
        } else if (fingerDown) {
            UnityRuntime.pointerReleased(fingerX, height - fingerY);
            fingerDown = false;
        }
        System.out.println("input frame=" + frame + " touch=" + (n == 0 ? "none" : sb.toString()));
    }

    private boolean dumped(int frame) {
        for (int i = 0; i < dumps.length; i++) {
            if (dumps[i] == frame) {
                return true;
            }
        }
        return false;
    }

    public void run() {
        parse();
        UnityRuntime.reset();
        // Nothing makes a sound here; what would have played, and when, is
        // part of the trace instead.
        AudioSource.$record(true);
        // And nothing is kept: every run starts with no preferences.
        PlayerPrefs.$store(null);
        PlayerPrefs.$resetMemory();
        Random.InitState(seed);
        UnityAppImpl.install();
        if (host != null) {
            host.installed();
        }
        UnityRuntime.resize(width, height);
        UnityRuntime.touchSupported(touchScreen);
        UnityRuntime.begin();
        System.out.println("headless seed=" + seed + " frames=" + frames + " view=" + width + "x" + height
                + " rate=" + rate + " scenes=" + UnityAppImpl.SCENE_COUNT);
        float dt = 1f / rate;
        for (int frame = 1; frame <= frames; frame++) {
            for (int i = 0; i < eventFrames.length; i++) {
                if (eventFrames[i] == frame && eventWord[i] != null) {
                    final String word = eventWord[i];
                    if (eventHost[i]) {
                        final Host to = host;
                        UnityRuntime.callInFrame(new Runnable() {
                            public void run() {
                                to.call(word);
                            }
                        });
                    } else {
                        UnityRuntime.applicationPaused(word.equals("pause"));
                    }
                    System.out.println("input frame=" + frame + (eventHost[i] ? " host=" : " app=") + word);
                } else if (eventFrames[i] == frame && eventTouch[i] != null) {
                    touch(frame, eventTouch[i]);
                } else if (eventFrames[i] == frame && eventKeys[i] == 0) {
                    // A host counts down from the top, Unity up from the
                    // bottom.
                    if (eventMove[i]) {
                        UnityRuntime.pointerMoved(eventX[i], height - eventY[i]);
                    } else if (eventDown[i]) {
                        UnityRuntime.pointerPressed(eventX[i], height - eventY[i]);
                    } else {
                        UnityRuntime.pointerReleased(eventX[i], height - eventY[i]);
                    }
                    System.out.println("input frame=" + frame + " pointer=" + eventX[i] + "," + eventY[i]
                            + (eventMove[i] ? " move" : eventDown[i] ? " down" : " up"));
                } else if (eventFrames[i] == frame) {
                    if (eventDown[i]) {
                        UnityRuntime.keyPressed(eventKeys[i]);
                    } else {
                        UnityRuntime.keyReleased(eventKeys[i]);
                    }
                    System.out.println("input frame=" + frame + " key=" + eventKeys[i] + (eventDown[i] ? " down"
                            : " up"));
                }
            }
            UnityRuntime.step(dt);
            String[] played = AudioSource.$drainLog();
            for (int i = 0; i < played.length; i++) {
                System.out.println("audio frame=" + frame + " " + played[i]);
            }
            boolean dump = dumped(frame);
            if (dump || (countEvery > 0 && frame % countEvery == 0) || frame == frames) {
                StringBuilder sb = new StringBuilder();
                sb.append("frame ").append(frame).append(" objects=").append(UnityRuntime.$objectCount());
                for (int i = 0; i < counted.length; i++) {
                    sb.append(' ').append(counted[i]).append('=').append(UnityRuntime.$objectCount(counted[i]));
                }
                DrawList list = UnityRuntime.render();
                sb.append(" drawn=").append(list.size());
                for (int i = 0; i < list.size(); i++) {
                    DrawCommand d = list.get(i);
                    if (d.text != null) {
                        sb.append(" text=").append(quote(d.text));
                    }
                }
                System.out.println(sb.toString());
                if (dump) {
                    print(frame, list);
                    if (sink != null) {
                        sink.frame(frame, list);
                    }
                }
            }
        }
    }

    private void print(int frame, DrawList list) {
        System.out.println("draw frame=" + frame + " commands=" + list.size() + " background="
                + Integer.toHexString(list.backgroundColor));
        // How many of each sprite, in the order each is first drawn.
        ArrayList names = new ArrayList();
        int[] counts = new int[16];
        for (int i = 0; i < list.size(); i++) {
            DrawCommand d = list.get(i);
            String name = d.text != null ? "(text)" : d.sprite + "@" + d.sourceX + "," + d.sourceY + "+"
                    + d.sourceWidth + "x" + d.sourceHeight;
            int at = names.indexOf(name);
            if (at < 0) {
                at = names.size();
                names.add(name);
                if (at == counts.length) {
                    int[] grown = new int[at * 2];
                    System.arraycopy(counts, 0, grown, 0, at);
                    counts = grown;
                }
            }
            counts[at]++;
        }
        for (int i = 0; i < names.size(); i++) {
            System.out.println("  count " + counts[i] + " " + names.get(i));
        }
        int shown = 0;
        for (int i = 0; i < list.size(); i++) {
            DrawCommand d = list.get(i);
            if (d.text != null) {
                System.out.println("  text " + quote(d.text) + " layer=" + d.sortingLayer + " order="
                        + d.sortingOrder + " at=" + c(d.x) + "," + c(d.y) + " size=" + c(d.width) + "x" + c(d.height)
                        + " font=" + c(d.fontSize) + " fit=" + (d.bestFit ? 1 : 0) + " min=" + c(d.minFontSize)
                        + " max=" + c(d.maxFontSize) + " align=" + d.alignment + " style=" + d.fontStyle + " wrap="
                        + (d.wrap ? 1 : 0) + " color=" + Integer.toHexString(d.color));
            } else if (shown < commands) {
                shown++;
                System.out.println("  " + d.sprite + " src=" + d.sourceX + "," + d.sourceY + "+" + d.sourceWidth
                        + "x" + d.sourceHeight + " layer=" + d.sortingLayer + " order=" + d.sortingOrder + " at="
                        + c(d.x) + "," + c(d.y) + " size=" + c(d.width) + "x" + c(d.height) + " anchor="
                        + c(d.anchorX) + "," + c(d.anchorY) + " rotation=" + c(d.rotation) + " color="
                        + Integer.toHexString(d.color) + " flip=" + (d.flipX ? 1 : 0) + (d.flipY ? 1 : 0));
            }
        }
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\n') {
                sb.append("\\n");
            } else if (ch == '"' || ch == '\\') {
                sb.append('\\').append(ch);
            } else {
                sb.append(ch);
            }
        }
        return sb.append('"').toString();
    }

    /// Hundredths, rounded half up by hand: `Math.round` is not what is
    /// being tested here.
    private static int c(float value) {
        float scaled = value * 100f;
        return (int) (scaled < 0f ? scaled - 0.5f : scaled + 0.5f);
    }
}
