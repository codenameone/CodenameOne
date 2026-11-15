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
package com.codenameone.examples.brickyard.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;

/// Draws an [Avatar] from blocks: a square head, a torso, two arms, two legs,
/// each shaded on its right side so it reads as a solid block.
public class AvatarView extends Component {
    private Avatar avatar;
    private final boolean small;

    public AvatarView(Avatar avatar, boolean small) {
        this.avatar = avatar;
        this.small = small;
        setName("avatarPreview");
    }

    public void setAvatar(Avatar avatar) {
        this.avatar = avatar;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        // The figure is 8 units wide and 13 tall; scale to fit.
        int unit = Math.max(1, Math.min(getWidth() / 8, getHeight() / 13));
        int x = getX() + (getWidth() - unit * 8) / 2;
        int y = getY() + (getHeight() - unit * 13) / 2;
        g.setAntiAliased(true);
        block(g, Avatar.HEAD, x + unit * 2, y, unit * 4, unit * 3);
        block(g, Avatar.TORSO, x + unit * 2, y + unit * 3, unit * 4, unit * 5);
        block(g, Avatar.LEFT_ARM, x, y + unit * 3, unit * 2, unit * 5);
        block(g, Avatar.RIGHT_ARM, x + unit * 6, y + unit * 3, unit * 2, unit * 5);
        block(g, Avatar.LEFT_LEG, x + unit * 2, y + unit * 8, unit * 2, unit * 5);
        block(g, Avatar.RIGHT_LEG, x + unit * 4, y + unit * 8, unit * 2, unit * 5);
        // Eyes and a smile on the head.
        g.setColor(0x1a1a1a);
        g.fillRect(x + unit * 3, y + unit, Math.max(1, unit / 2), Math.max(1, unit / 2));
        g.fillRect(x + unit * 4 + unit / 2, y + unit, Math.max(1, unit / 2), Math.max(1, unit / 2));
        g.fillRect(x + unit * 3, y + unit * 2, unit * 2, Math.max(1, unit / 4));
    }

    private void block(Graphics g, int part, int x, int y, int w, int h) {
        int c = avatar.color(part);
        g.setColor(c);
        g.fillRect(x, y, w, h);
        g.setColor(shade(c));
        g.fillRect(x + w - Math.max(1, w / 5), y, Math.max(1, w / 5), h);
        g.setColor(0x10131f);
        g.drawRect(x, y, w - 1, h - 1);
    }

    static int shade(int c) {
        int r = ((c >> 16) & 0xff) * 3 / 4;
        int gr = ((c >> 8) & 0xff) * 3 / 4;
        int b = (c & 0xff) * 3 / 4;
        return (r << 16) | (gr << 8) | b;
    }

    @Override
    protected Dimension calcPreferredSize() {
        int h = CN.convertToPixels(small ? 22 : 45);
        return new Dimension(h * 8 / 13, h);
    }
}
