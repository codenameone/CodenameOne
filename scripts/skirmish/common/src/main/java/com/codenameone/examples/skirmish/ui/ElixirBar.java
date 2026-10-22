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
package com.codenameone.examples.skirmish.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.skirmish.rules.PlayerState;

/// The elixir meter under the hand. The trough is the `ElixirBar` UIID and the
/// liquid the `ElixirFill` UIID, both from `theme.css`, so a fork recolours or
/// re-textures the meter without touching this class. Whole units are marked
/// with notches, the way a player counts them.
public final class ElixirBar extends Component {
    private int elixir;

    public ElixirBar() {
        setUIID("ElixirBar");
        setName("elixir");
    }

    public void setElixir(int value) {
        if (value != elixir) {
            elixir = value;
            repaint();
        }
    }

    public int elixir() {
        return elixir;
    }

    @Override
    public void paint(Graphics g) {
        Style s = getStyle();
        int x = getX() + s.getPaddingLeftNoRTL();
        int y = getY() + s.getPaddingTop();
        int w = getWidth() - s.getHorizontalPadding();
        int h = getHeight() - s.getVerticalPadding();
        int full = w * elixir / PlayerState.MAX_ELIXIR;
        Style fill = UIManager.getInstance().getComponentStyle("ElixirFill");
        g.setColor(fill.getBgColor());
        g.fillRoundRect(x, y, full, h, h, h);
        g.setColor(fill.getFgColor());
        for (int i = 1; i < 10; i++) {
            int nx = x + w * i / 10;
            g.drawLine(nx, y + h / 4, nx, y + h - h / 4);
        }
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(CN.convertToPixels(40), CN.convertToPixels(3));
    }
}
