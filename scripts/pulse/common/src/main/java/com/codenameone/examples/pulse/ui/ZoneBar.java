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
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import java.util.List;

/// The minutes of a workout in each heart rate zone, as one bar split in the
/// zones' colours, easy to hard from left to right.
public final class ZoneBar extends Component {
    /// Zone 1 to 5: easy grey, blue, green, orange, red.
    private static final int[] COLORS = {0x9aa3ad, 0x3d8bfd, 0x2bb673, 0xff9f1c, 0xe5383b};
    private final int[] minutes = new int[COLORS.length];
    private int total;

    public ZoneBar(List<Integer> zones) {
        setUIID("SvZoneBar");
        for (int iter = 0; zones != null && iter < zones.size() && iter < COLORS.length; iter++) {
            Integer value = zones.get(iter);
            minutes[iter] = value == null ? 0 : Math.max(0, value.intValue());
            total += minutes[iter];
        }
        setAccessibilityText(total == 0 ? "" : describe());
    }

    /// Whether there is anything to draw.
    public boolean hasData() {
        return total > 0;
    }

    private String describe() {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < minutes.length; iter++) {
            if (minutes[iter] > 0) {
                if (out.length() > 0) {
                    out.append(", ");
                }
                out.append(Lang.tr("zone {0}: {1} min", Integer.valueOf(iter + 1),
                        Integer.valueOf(minutes[iter])));
            }
        }
        return out.toString();
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(CN.convertToPixels(30), CN.convertToPixels(1.4f));
    }

    @Override
    public void paint(Graphics g) {
        if (total == 0) {
            return;
        }
        int x = getX();
        int width = getWidth();
        int height = getHeight();
        int gap = Math.max(1, CN.convertToPixels(0.4f));
        int used = 0;
        g.setAntiAliased(true);
        for (int iter = 0; iter < minutes.length; iter++) {
            if (minutes[iter] == 0) {
                continue;
            }
            int start = x + used * width / total;
            used += minutes[iter];
            int end = x + used * width / total;
            g.setColor(COLORS[iter]);
            g.fillRoundRect(start, getY(), Math.max(1, end - start - gap), height, height, height);
        }
    }
}
