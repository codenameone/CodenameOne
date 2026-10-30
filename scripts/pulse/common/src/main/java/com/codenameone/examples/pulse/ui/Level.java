/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.pulse.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/// A ring that fills as a member's points climb toward the next level, with
/// the level's number in the middle.
///
/// The filled part takes `SvLevel`'s foreground, the rest `SvLevelTrack`'s,
/// and the number `SvLevel`'s font. It fills from empty when it is first shown,
/// over most of a second, so earning points feels like something.
public final class Level extends Component {
    private final int level;
    private final double share;
    private long shown;

    /// @param share how far from this level to the next, 0 to 1
    public Level(int level, double share) {
        setUIID("SvLevel");
        this.level = level;
        this.share = Math.max(0d, Math.min(1d, share));
        setFocusable(false);
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        shown = System.currentTimeMillis();
        if (getComponentForm() != null) {
            getComponentForm().registerAnimated(this);
        }
    }

    @Override
    protected void deinitialize() {
        if (getComponentForm() != null) {
            getComponentForm().deregisterAnimated(this);
        }
        super.deinitialize();
    }

    @Override
    public boolean animate() {
        // One last frame at the full share, then nothing more to draw.
        if (System.currentTimeMillis() - shown > 900L && getComponentForm() != null) {
            getComponentForm().deregisterAnimated(this);
        }
        return true;
    }

    @Override
    protected Dimension calcPreferredSize() {
        int side = CN.convertToPixels(19);
        return new Dimension(side, side);
    }

    @Override
    public void paint(Graphics g) {
        Style style = getStyle();
        int side = Math.min(getWidth() - style.getHorizontalPadding(),
                getHeight() - style.getVerticalPadding());
        if (side <= 0) {
            return;
        }
        int stroke = Math.max(3, CN.convertToPixels(1.6f));
        int size = side - stroke;
        int x = getX() + (getWidth() - size) / 2;
        int y = getY() + (getHeight() - size) / 2;
        double t = Math.min(1d, (System.currentTimeMillis() - shown) / 900d);
        double eased = 1 - (1 - t) * (1 - t) * (1 - t);
        int sweep = (int) Math.round(360 * share * eased);
        int track = UIManager.getInstance().getComponentStyle("SvLevelTrack").getFgColor();
        g.setAntiAliased(true);
        if (g.isShapeSupported()) {
            Stroke pen = new Stroke(stroke, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 1f);
            g.setColor(track);
            g.drawShape(arc(x, y, size, 0, 360), pen);
            if (sweep > 0) {
                g.setColor(style.getFgColor());
                g.drawShape(arc(x, y, size, 90, -sweep), pen);
            }
        } else {
            g.setColor(track);
            g.fillArc(x - stroke / 2, y - stroke / 2, size + stroke, size + stroke, 0, 360);
            g.setColor(style.getFgColor());
            g.fillArc(x - stroke / 2, y - stroke / 2, size + stroke, size + stroke, 90, -sweep);
            g.setColor(style.getBgColor());
            g.fillArc(x + stroke / 2, y + stroke / 2, size - stroke, size - stroke, 0, 360);
        }
        String text = String.valueOf(level);
        Font font = style.getFont();
        g.setFont(font);
        g.setColor(style.getFgColor());
        g.drawString(text, getX() + (getWidth() - font.stringWidth(text)) / 2,
                getY() + (getHeight() - font.getHeight()) / 2);
    }

    private static GeneralPath arc(int x, int y, int size, int start, int sweep) {
        GeneralPath path = new GeneralPath();
        path.arc(x, y, size, size, Math.toRadians(start), Math.toRadians(sweep));
        return path;
    }
}
