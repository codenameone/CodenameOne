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
package com.codenameone.examples.dash.run;

import com.codename1.maps.LatLng;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import java.util.List;

/// The line a city route draws on the street map, kept for the run's minimap:
/// the route's shape, north up, with the stretch already run lit.
///
/// It is cosmetic. The level is built from the route's legs alone
/// ([com.codenameone.examples.dash.rules.CityRoute]), so a replay without
/// this line plays the same and only goes without a minimap.
public final class CityPath {
    private final float[] x;
    private final float[] y;
    private final float[] along;

    private CityPath(float[] x, float[] y, float[] along) {
        this.x = x;
        this.y = y;
        this.along = along;
    }

    /// The route's vertices projected flat around their middle, scaled into
    /// a unit square and centred in it.
    public static CityPath of(List<LatLng> points) {
        int n = points.size();
        if (n < 2) {
            return null;
        }
        double lat0 = 0;
        for (int i = 0; i < n; i++) {
            lat0 += points.get(i).getLatitude();
        }
        double k = Math.cos(Math.toRadians(lat0 / n));
        double[] px = new double[n];
        double[] py = new double[n];
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            px[i] = points.get(i).getLongitude() * k;
            // North up: latitude grows up the screen, y grows down it.
            py[i] = -points.get(i).getLatitude();
            minX = Math.min(minX, px[i]);
            minY = Math.min(minY, py[i]);
            maxX = Math.max(maxX, px[i]);
            maxY = Math.max(maxY, py[i]);
        }
        double span = Math.max(1e-9, Math.max(maxX - minX, maxY - minY));
        double offX = (span - (maxX - minX)) / 2;
        double offY = (span - (maxY - minY)) / 2;
        float[] x = new float[n];
        float[] y = new float[n];
        float[] along = new float[n];
        for (int i = 0; i < n; i++) {
            x[i] = (float) ((px[i] - minX + offX) / span);
            y[i] = (float) ((py[i] - minY + offY) / span);
            if (i > 0) {
                double dx = x[i] - x[i - 1];
                double dy = y[i] - y[i - 1];
                along[i] = along[i - 1] + (float) Math.sqrt(dx * dx + dy * dy);
            }
        }
        return new CityPath(x, y, along);
    }

    /// A small square that draws the path, lit up to `progress`.
    public static final class Minimap extends Component {
        private final CityPath path;
        private float progress;

        public Minimap(CityPath path) {
            this.path = path;
            setUIID("CityMinimap");
            setFocusable(false);
        }

        /// How far along the route the runner is, 0 to 1.
        public void setProgress(float p) {
            float clamped = Math.max(0f, Math.min(1f, p));
            if (Math.abs(clamped - progress) > 0.002f) {
                progress = clamped;
                repaint();
            }
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            int s = com.codename1.ui.CN.convertToPixels(17);
            return new com.codename1.ui.geom.Dimension(s, s);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            int pad = com.codename1.ui.CN.convertToPixels(1.8f);
            int ox = getX() + pad;
            int oy = getY() + pad;
            int size = Math.min(getWidth(), getHeight()) - pad * 2;
            if (size <= 0) {
                return;
            }
            g.setAntiAliased(true);
            float total = path.along[path.along.length - 1];
            float lit = total * progress;
            GeneralPath all = new GeneralPath();
            GeneralPath done = new GeneralPath();
            float hx = path.x[0];
            float hy = path.y[0];
            all.moveTo(ox + path.x[0] * size, oy + path.y[0] * size);
            done.moveTo(ox + path.x[0] * size, oy + path.y[0] * size);
            for (int i = 1; i < path.x.length; i++) {
                all.lineTo(ox + path.x[i] * size, oy + path.y[i] * size);
                if (path.along[i] <= lit) {
                    done.lineTo(ox + path.x[i] * size, oy + path.y[i] * size);
                    hx = path.x[i];
                    hy = path.y[i];
                } else if (path.along[i - 1] < lit) {
                    float t = (lit - path.along[i - 1]) / (path.along[i] - path.along[i - 1]);
                    hx = path.x[i - 1] + (path.x[i] - path.x[i - 1]) * t;
                    hy = path.y[i - 1] + (path.y[i] - path.y[i - 1]) * t;
                    done.lineTo(ox + hx * size, oy + hy * size);
                }
            }
            int w = Math.max(2, com.codename1.ui.CN.convertToPixels(0.6f));
            int alpha = g.getAlpha();
            g.setColor(0x6b7bd6);
            g.setAlpha(170);
            g.drawShape(all, new Stroke(w, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 1f));
            g.setAlpha(255);
            g.setColor(0xd4ff3a);
            g.drawShape(done, new Stroke(w + 1, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 1f));
            // The finish, and the runner as a dot on the line.
            int fx = (int) (ox + path.x[path.x.length - 1] * size);
            int fy = (int) (oy + path.y[path.y.length - 1] * size);
            g.setColor(0xff4f9a);
            g.fillArc(fx - w * 2, fy - w * 2, w * 4, w * 4, 0, 360);
            g.setColor(0xffffff);
            g.fillArc((int) (ox + hx * size) - w * 2, (int) (oy + hy * size) - w * 2, w * 4, w * 4, 0, 360);
            g.setAlpha(alpha);
        }
    }
}
