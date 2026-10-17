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

import com.codename1.unitycompat.unityengine.DrawCommand;
import com.codename1.unitycompat.unityengine.DrawList;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;

/// The headless driver for a JVM: reads its settings from the command
/// line, runs [HeadlessTrace], and -- with `--png` -- paints each dumped
/// frame into an image file.
///
/// ```
/// java -Djava.awt.headless=true -cp <classes>:<resources>:<runtime>:<core> headless.HeadlessMain
///     [--seed n] [--frames n] [--size WxH] [--rate fps] [--input file]
///     [--dump f1,f2,...] [--commands n] [--count-every n] [--count Prefix]...
///     [--png dir] [--host class]
/// ```
///
/// `--host` names a [HeadlessTrace.Host] with a public constructor of no
/// arguments. It is found by name here because this is a command line; a
/// translated program names the class in its generated main instead.
///
/// The painting is this file's own and uses the JDK's `BufferedImage`: it
/// is a way to look at a frame without a window, not how a Codename One
/// application draws -- that is `UnityGameView`. It reads the same draw
/// list, so what is wrong in one is, as far as the list goes, wrong in the
/// other.
public final class HeadlessMain {
    private final HashMap<String, BufferedImage> images = new HashMap<String, BufferedImage>();
    private File directory;
    private int width;
    private int height;

    private HeadlessMain() {
    }

    public static void main(String[] args) throws IOException {
        HeadlessTrace trace = new HeadlessTrace();
        List<String> counted = new ArrayList<String>();
        final HeadlessMain painter = new HeadlessMain();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--seed")) {
                trace.seed = Integer.parseInt(args[++i]);
            } else if (a.equals("--frames")) {
                trace.frames = Integer.parseInt(args[++i]);
            } else if (a.equals("--size")) {
                String[] size = args[++i].split("x");
                trace.width = Integer.parseInt(size[0]);
                trace.height = Integer.parseInt(size[1]);
            } else if (a.equals("--rate")) {
                trace.rate = Integer.parseInt(args[++i]);
            } else if (a.equals("--input")) {
                trace.script = new String(Files.readAllBytes(new File(args[++i]).toPath()), StandardCharsets.UTF_8);
            } else if (a.equals("--dump")) {
                String[] frames = args[++i].split(",");
                trace.dumps = new int[frames.length];
                for (int f = 0; f < frames.length; f++) {
                    trace.dumps[f] = Integer.parseInt(frames[f].trim());
                }
            } else if (a.equals("--commands")) {
                trace.commands = Integer.parseInt(args[++i]);
            } else if (a.equals("--count-every")) {
                trace.countEvery = Integer.parseInt(args[++i]);
            } else if (a.equals("--count")) {
                counted.add(args[++i]);
            } else if (a.equals("--png")) {
                painter.directory = new File(args[++i]);
            } else if (a.equals("--host")) {
                try {
                    trace.host = (HeadlessTrace.Host) Class.forName(args[++i]).getConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new IllegalArgumentException("--host " + args[i] + " is not a HeadlessTrace.Host", e);
                }
            } else {
                System.err.println("usage: HeadlessMain [--seed n] [--frames n] [--size WxH] [--rate fps]"
                        + " [--input file] [--dump f1,f2,...] [--commands n] [--count-every n]"
                        + " [--count Prefix]... [--png dir] [--host class]");
                System.exit(2);
            }
        }
        trace.counted = counted.toArray(new String[0]);
        if (painter.directory != null) {
            painter.directory.mkdirs();
            painter.width = trace.width;
            painter.height = trace.height;
            trace.sink = new HeadlessTrace.Sink() {
                public void frame(int frame, DrawList list) {
                    painter.paint(frame, list);
                }
            };
        }
        trace.run();
    }

    private BufferedImage image(String resource) {
        if (images.containsKey(resource)) {
            return images.get(resource);
        }
        BufferedImage image = null;
        try (InputStream in = HeadlessMain.class.getResourceAsStream("/" + resource)) {
            if (in != null) {
                BufferedImage read = ImageIO.read(in);
                if (read != null) {
                    image = new BufferedImage(read.getWidth(), read.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    image.createGraphics().drawImage(read, 0, 0, null);
                }
            }
        } catch (IOException e) {
            image = null;
        }
        if (image == null) {
            System.err.println("HeadlessMain: no image resource /" + resource);
        }
        images.put(resource, image);
        return image;
    }

    /// The part of an image a command names, multiplied by its colour.
    private static BufferedImage tinted(BufferedImage image, DrawCommand d) {
        int w = Math.min(d.sourceWidth, image.getWidth() - d.sourceX);
        int h = Math.min(d.sourceHeight, image.getHeight() - d.sourceY);
        if (w <= 0 || h <= 0) {
            return null;
        }
        int[] px = image.getRGB(d.sourceX, d.sourceY, w, h, null, 0, w);
        int ca = d.color >>> 24;
        int cr = (d.color >> 16) & 0xff;
        int cg = (d.color >> 8) & 0xff;
        int cb = d.color & 0xff;
        if (d.color != 0xffffffff) {
            for (int i = 0; i < px.length; i++) {
                int p = px[i];
                px[i] = ((p >>> 24) * ca / 255) << 24 | (((p >> 16) & 0xff) * cr / 255) << 16
                        | (((p >> 8) & 0xff) * cg / 255) << 8 | ((p & 0xff) * cb / 255);
            }
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, px, 0, w);
        return out;
    }

    void paint(int frame, DrawList list) {
        BufferedImage surface = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = surface.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(list.hasCamera ? list.backgroundColor & 0xffffff : 0));
        g.fillRect(0, 0, width, height);
        for (int i = 0; i < list.size(); i++) {
            DrawCommand d = list.get(i);
            if (d.text != null) {
                text(g, d);
                continue;
            }
            BufferedImage image = image(d.sprite);
            BufferedImage part = image == null ? null : tinted(image, d);
            if (part == null) {
                continue;
            }
            // The pivot lands on x,y; the image is turned clockwise and
            // mirrored about it, then sized.
            AffineTransform t = new AffineTransform();
            t.translate(d.x, d.y);
            t.rotate(Math.toRadians(d.rotation));
            t.scale(d.flipX ? -1 : 1, d.flipY ? -1 : 1);
            t.translate(-d.anchorX * d.width, -d.anchorY * d.height);
            t.scale(d.width / part.getWidth(), d.height / part.getHeight());
            g.setComposite(AlphaComposite.SrcOver);
            g.drawImage(part, t, null);
        }
        g.dispose();
        File file = new File(directory, "frame-" + frame + ".png");
        try {
            ImageIO.write(surface, "png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> lines(String text, FontMetrics metrics, float width, boolean wrap) {
        List<String> out = new ArrayList<String>();
        for (String paragraph : text.split("\n", -1)) {
            if (!wrap || metrics.stringWidth(paragraph) <= width) {
                out.add(paragraph);
                continue;
            }
            String line = "";
            for (String word : paragraph.split(" ")) {
                String longer = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && metrics.stringWidth(longer) > width) {
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

    private static Font font(DrawCommand d, float size) {
        int style = ((d.fontStyle & 1) != 0 ? Font.BOLD : 0) | ((d.fontStyle & 2) != 0 ? Font.ITALIC : 0);
        return new Font(Font.SANS_SERIF, style, 1).deriveFont(Math.max(1f, size));
    }

    private static boolean fits(Graphics2D g, DrawCommand d, float size) {
        FontMetrics m = g.getFontMetrics(font(d, size));
        List<String> lines = lines(d.text, m, d.width, d.wrap);
        if (lines.size() * m.getHeight() > d.height) {
            return false;
        }
        for (String line : lines) {
            if (m.stringWidth(line) > d.width) {
                return false;
            }
        }
        return true;
    }

    private static void text(Graphics2D g, DrawCommand d) {
        float size = d.fontSize;
        if (d.bestFit) {
            size = d.minFontSize;
            for (float s = d.maxFontSize; s > d.minFontSize; s -= 1f) {
                if (fits(g, d, s)) {
                    size = s;
                    break;
                }
            }
        }
        Font font = font(d, size);
        FontMetrics m = g.getFontMetrics(font);
        List<String> lines = lines(d.text, m, d.width, d.wrap);
        float block = lines.size() * m.getHeight();
        int row = d.alignment / 3;
        int column = d.alignment % 3;
        float top = d.y + (row == 0 ? 0f : row == 1 ? (d.height - block) / 2f : d.height - block);
        g.setFont(font);
        g.setColor(new Color(d.color, true));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float w = m.stringWidth(line);
            float left = d.x + (column == 0 ? 0f : column == 1 ? (d.width - w) / 2f : d.width - w);
            g.drawString(line, left, top + i * m.getHeight() + m.getAscent());
        }
    }
}
