/**
 * Copyright (C) 2019 dj6082013
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codename1.charts.views;

import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.charts.compat.Paint.Style;
import com.codename1.charts.models.AreaSeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.ui.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The radar chart rendering class.
 */
public class RadarChart extends RoundChart {

    /**
     * The series dataset.
     */
    private AreaSeries mDataset;

    /**
     * A step variable to control the size of the legend shape.
     */
    private int mStep;

    /**
     * Builds a new radar chart instance.
     *
     * @param dataset the series dataset
     * @param renderer the series renderer
     */
    public RadarChart(AreaSeries dataset, DefaultRenderer renderer) {
        super(null, renderer);
        mDataset = dataset;
    }

    /**
     * The graphical representation of the radar chart.
     *
     * @param canvas the canvas to paint to
     * @param x the top left x value of the view to draw to
     * @param y the top left y value of the view to draw to
     * @param width the width of the view to draw to
     * @param height the height of the view to draw to
     * @param paint the paint
     */
    @Override
    public void draw(Canvas canvas, int x, int y, int width, int height, Paint paint) {
        paint.setAntiAlias(mRenderer.isAntialiasing());
        paint.setStyle(Style.FILL);
        paint.setTextSize(mRenderer.getLabelsTextSize());
        int legendSize = getLegendSize(mRenderer, height / 5, 0);
        int left = x;
        int top = y;
        int right = x + width;

        int cLength = mDataset.getCategoriesCount();
        String[] categories = mDataset.getCategories();

        int bottom = y + height - legendSize;
        drawBackground(mRenderer, canvas, x, y, width, height, paint, false, DefaultRenderer.NO_COLOR);
        mStep = SHAPE_WIDTH * 3 / 4;

        int mRadius = Math.min(Math.abs(right - left), Math.abs(bottom - top));
        double rCoef = 0.35 * mRenderer.getScale();
        double decCoef = 0.2;
        int radius = (int) (mRadius * rCoef);
        if (autoCalculateCenter || mCenterX == NO_VALUE) {
            mCenterX = (left + right) / 2;
        }
        if (autoCalculateCenter || mCenterY == NO_VALUE) {
            mCenterY = (bottom + top) / 2;
        }
        float shortRadius = radius;
        float longRadius = radius * 1.1f;
        List<Rectangle2D> prevLabelsBounds = new ArrayList<Rectangle2D>();

        float currentAngle = mRenderer.getStartAngle();
        float angle = 360f / cLength;

// Draw web
        float centerX = (left + right) / 2, centerY = (top + bottom) / 2;
        for (int i = 0; i < cLength; i++) {
            paint.setColor(ColorUtil.GRAY);
            float thisRad = (float) Math.toRadians(90 - currentAngle);
            float nextRad = (float) Math.toRadians(90 - (currentAngle + angle));
            for (float level = 0; level <= 1f; level += decCoef) {
                float thisX = (float) (centerX - Math.sin(thisRad) * radius * level);
                float thisY = (float) (centerY - Math.cos(thisRad) * radius * level);
                float nextX = (float) (centerX - Math.sin(nextRad) * radius * level);
                float nextY = (float) (centerY - Math.cos(nextRad) * radius * level);
                canvas.drawLine(thisX, thisY, nextX, nextY, paint);
            }
            canvas.drawLine(centerX, centerY, centerX - (float) Math.sin(thisRad) * radius, centerY - (float) Math.cos(thisRad) * radius, paint);

            paint.setColor(ColorUtil.GRAY);
            drawLabel(canvas, categories[i], mRenderer, prevLabelsBounds, mCenterX, mCenterY,
                    shortRadius, longRadius, currentAngle, angle, left, right, mRenderer.getLabelsColor(),
                    paint, true, false);

            currentAngle += angle;
        }

// Draw area
        int sLength = mDataset.getSeriesCount();
        for (int i = 0; i < sLength; i++) {
            currentAngle = mRenderer.getStartAngle();
            paint.setColor(mRenderer.getSeriesRendererAt(i).getColor());
            for (int j = 0; j < cLength; j++) {
                float thisValue = (float) mDataset.getValue(i, categories[j]);
                float nextValue = (float) mDataset.getValue(i, categories[(j + 1) % sLength]);
                float thisRad = (float) Math.toRadians(90 - currentAngle);
                float nextRad = (float) Math.toRadians(90 - (currentAngle + angle));
                float thisX = (float) (centerX - Math.sin(thisRad) * radius * thisValue);
                float thisY = (float) (centerY - Math.cos(thisRad) * radius * thisValue);
                float nextX = (float) (centerX - Math.sin(nextRad) * radius * nextValue);
                float nextY = (float) (centerY - Math.cos(nextRad) * radius * nextValue);

                canvas.drawLine(thisX, thisY, nextX, nextY, paint);
                currentAngle += angle;
            }
        }
// Draw Background
        if (mRenderer.getBackgroundColor() != 0) {
            paint.setColor(mRenderer.getBackgroundColor());
        } else {
            paint.setColor(ColorUtil.WHITE);
        }

        prevLabelsBounds.clear();
        drawLegend(canvas, mRenderer, mDataset.getSeries(), left, right, y, width, height, legendSize, paint,
                false);
        drawTitle(canvas, x, y, width, paint);
    }

    /**
     * Returns the legend shape width.
     *
     * @param seriesIndex the series index
     * @return the legend shape width
     */
    @Override
    public int getLegendShapeWidth(int seriesIndex) {
        return SHAPE_WIDTH;
    }

    /**
     * The graphical representation of the legend shape.
     *
     * @param canvas the canvas to paint to
     * @param renderer the series renderer
     * @param x the x value of the point the shape should be drawn at
     * @param y the y value of the point the shape should be drawn at
     * @param seriesIndex the series index
     * @param paint the paint to be used for drawing
     */
    @Override
    public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y, int seriesIndex, Paint paint) {
        canvas.drawCircle(x + SHAPE_WIDTH - mStep, y, mStep, paint);
        mStep--;
    }

}
