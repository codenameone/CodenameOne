/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui.spinner;

import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.ImageFactory;
import com.codename1.ui.List;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Effects;

import java.util.HashMap;
import java.util.Map;

/// Spinner renderer that can automatically simulate the iOS perspective transform behavior
///
/// @author Shai Almog
class SpinnerRenderer<T> extends DefaultListCellRenderer<T> {
    private static final int PERSPECTIVES = 9;
    private static final int FRONT_ANGLE = 4;
    // Measured against the native UIPickerView: off-selection rows keep their
    // glyph shapes (no horizontal taper worth speaking of) and compress
    // vertically as they curve away -- the old 0.5..0.95 tapers warped each
    // glyph into a wedge, which read as broken letter-spacing.
    private static final float[] TOP_SCALE = {0.90f, 0.90f, 0.95f, 0.99f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] BOTTOM_SCALE = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.99f, 0.95f, 0.90f, 0.90f};
    private static final float[] VERTICAL_SHRINK = {0.55f, 0.55f, 0.78f, 0.92f, 1.0f, 0.92f, 0.78f, 0.55f, 0.55f};
    static boolean iOS7Mode;
    int perspective;
    // Per-perspective cache of fully composed ROW images (string -> image).
    // The row is rendered and perspective-transformed as ONE image so glyph
    // spacing survives the transform; per-character transforms sheared each
    // glyph separately (the native picker curves the whole row).
    private Map<String, Image>[] imageCache;

    public SpinnerRenderer() {
        super(false);
    }

    @Override
    public Component getCellRendererComponent(Component list, Object model, T value, int index, boolean isSelected) {
        if (iOS7Mode) {
            perspective = -1;
            // calculate perspective
            int idx = ((List) list).getCurrentSelected();
            if (idx == index) {
                perspective = FRONT_ANGLE;
            } else {
                int count = ((List) list).getModel().getSize();
                int directDistance = Math.abs(idx - index);
                int indirect;
                if (index > idx) {
                    indirect = count - index + idx;
                } else {
                    indirect = count - idx + index;
                }
                // Perspective indices run 0..8 with FRONT_ANGLE=4 flat in the
                // middle and the distortion arrays growing toward the ENDS -- so
                // a row DISTANCE d from the selection maps to FRONT_ANGLE -/+ d
                // (adjacent row = mildest tilt, farthest = strongest). The old
                // mapping used the raw distance as the index, handing the
                // ADJACENT row the heaviest wedge and the far row the mild one.
                if (indirect < directDistance) {
                    if (indirect < FRONT_ANGLE) {
                        if (index < idx) {
                            perspective = FRONT_ANGLE - indirect;
                        } else {
                            perspective = FRONT_ANGLE + indirect;
                        }
                    }
                } else {
                    if (directDistance < FRONT_ANGLE) {
                        if (index < idx) {
                            perspective = FRONT_ANGLE - directDistance;
                        } else {
                            perspective = FRONT_ANGLE + directDistance;
                        }
                    }
                }
            }
        }
        return super.getCellRendererComponent(list, model, value, index, isSelected);
    }

    @Override
    public void paint(Graphics g) {
        if (!iOS7Mode || perspective == FRONT_ANGLE) {
            super.paint(g);
        } else {
            if (!isInClippingRegion(g)) {
                return;
            }
            Style s = getStyle();
            // Centre the perspective row horizontally AND keep the row's vertical
            // centre where the untransformed row would sit (the transform shrinks
            // the image height; drawing at paddingTop floated the shrunk row).
            String text = getText();
            Image row = rowPerspectiveImage(text);
            if (row == null) {
                return;
            }
            int cx = getX() + Math.max(s.getPaddingLeftNoRTL(), (getWidth() - row.getWidth()) / 2);
            int fullH = s.getFont().getHeight();
            int cy = getY() + s.getPaddingTop() + (fullH - row.getHeight()) / 2;
            g.drawImage(row, cx, cy);
        }
    }

    /// Renders the whole row string once, dims it to the native off-row grey and
    /// applies the vertical perspective to the composed row (per-character
    /// transforms sheared each glyph separately). Cached per perspective+string;
    /// the spinner shows a handful of repeating values so the cache stays tiny.
    private Image rowPerspectiveImage(String text) {
        if (perspective < 0 || perspective >= PERSPECTIVES) {
            return null;
        }
        if (imageCache == null) {
            imageCache = new HashMap[PERSPECTIVES];
            for (int iter = 0; iter < PERSPECTIVES; iter++) {
                if (iter != FRONT_ANGLE) {
                    imageCache[iter] = new HashMap<String, Image>();
                }
            }
        }
        Image i = imageCache[perspective].get(text);
        if (i == null) {
            Font f = getStyle().getFont();
            int w = Math.max(1, f.stringWidth(text));
            int h = f.getHeight();
            i = ImageFactory.createImage(this, w, h, 0);
            Graphics ig = i.getGraphics();
            UIManager.getInstance().getLookAndFeel().setFG(ig, this);
            // The native picker dims every off-selection row to roughly the SAME
            // tertiary grey (sampled ~0.32 of the label colour over the sheet,
            // barely darker near the selection band) -- not a steep distance ramp.
            int depth = Math.abs(perspective - FRONT_ANGLE);
            float fade = Math.max(0.28f, 0.36f - 0.02f * depth);
            int alpha = ig.concatenateAlpha((int) (getStyle().getFgAlpha() * fade));
            ig.drawString(text, 0, 0);
            ig.setAlpha(alpha);
            i = Effects.verticalPerspective(i, TOP_SCALE[perspective], BOTTOM_SCALE[perspective], VERTICAL_SHRINK[perspective]);
            imageCache[perspective].put(text, i);
        }
        return i;
    }
}
