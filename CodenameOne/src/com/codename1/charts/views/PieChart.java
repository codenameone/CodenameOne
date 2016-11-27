/**
 * Copyright (C) 2009 - 2013 SC 4ViewSoft SRL
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codename1.charts.views;

import java.util.ArrayList;
import java.util.List;
import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.GradientDrawable;
import com.codename1.charts.compat.GradientDrawable.Orientation;
import com.codename1.charts.compat.Paint;
import com.codename1.charts.compat.Paint.Style;



import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.models.Point;
import com.codename1.charts.models.SeriesSelection;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.ui.geom.Rectangle2D;
import com.codename1.ui.geom.Shape;



/**
 * The pie chart rendering class.
 */
public class PieChart extends RoundChart {
  /** Handles returning values when tapping on PieChart. */
  private PieMapper mPieMapper;

  /**
   * Builds a new pie chart instance.
   * 
   * @param dataset the series dataset
   * @param renderer the series renderer
   */
  public PieChart(CategorySeries dataset, DefaultRenderer renderer) {
    super(dataset, renderer);
    mPieMapper = new PieMapper();
  }

  /**
   * The graphical representation of the pie chart.
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
        int sLength = mDataset.getItemCount();
        double total = 0;
        String[] titles = new String[sLength];
        for (int i = 0; i < sLength; i++) {
            total += mDataset.getValue(i);
            titles[i] = mDataset.getCategory(i);
        }
        if (mRenderer.isFitLegend()) {
            legendSize = drawLegend(canvas, mRenderer, titles, left, right, y, width, height, legendSize,
                    paint, true);
        }
        int bottom = y + height - legendSize;
        drawBackground(mRenderer, canvas, x, y, width, height, paint, false, DefaultRenderer.NO_COLOR);

        float currentAngle = mRenderer.getStartAngle();
        int mRadius = Math.min(Math.abs(right - left), Math.abs(bottom - top));
        int radius = (int) (mRadius * 0.35 * mRenderer.getScale());

        if (autoCalculateCenter || mCenterX == NO_VALUE) {
            mCenterX = (left + right) / 2;
        }
        if (autoCalculateCenter || mCenterY == NO_VALUE) {
            mCenterY = (bottom + top) / 2;
        }

        // Hook in clip detection after center has been calculated
        mPieMapper.setDimensions(radius, mCenterX, mCenterY);
        boolean loadPieCfg = !mPieMapper.areAllSegmentPresent(sLength);
        if (loadPieCfg) {
            mPieMapper.clearPieSegments();
        }

        float shortRadius = radius * 0.9f;
        float longRadius = radius * 1.1f;
        Rectangle2D oval = PkgUtils.makeRect(mCenterX - radius, mCenterY - radius, mCenterX + radius, mCenterY
                + radius);
        List<Rectangle2D> prevLabelsBounds = new ArrayList<Rectangle2D>();

        for (int i = 0; i < sLength; i++) {
            SimpleSeriesRenderer seriesRenderer = mRenderer.getSeriesRendererAt(i);
            boolean gradient = false;
            GradientDrawable gradientDrawable = null;
            if (seriesRenderer.isGradientEnabled()) {
                gradient = true;
                gradientDrawable = new GradientDrawable(Orientation.TOP_BOTTOM, new int[]{ seriesRenderer.getGradientStartColor(), seriesRenderer.getGradientStopColor()});
                paint.setColor(seriesRenderer.getGradientStartColor());
            } else {
                paint.setColor(seriesRenderer.getColor());
            }


            float value = (float) mDataset.getValue(i);
            float angle = (float) (value / total * 360);
            if (seriesRenderer.isHighlighted()) {
                double rAngle = Math.toRadians(90 - (currentAngle + angle / 2));
                float translateX = (float) (radius * 0.1 * Math.sin(rAngle));
                float translateY = (float) (radius * 0.1 * Math.cos(rAngle));
                oval.translate(translateX, translateY);
                if (gradient) {
                    canvas.drawArcWithGradient(oval, currentAngle, angle, true, paint, gradientDrawable);
                } else {
                    canvas.drawArc(oval, currentAngle, angle, true, paint);
                }
                oval.translate(-translateX, -translateY);
            } else {
                if (gradient) {
                    canvas.drawArcWithGradient(oval, currentAngle, angle, true, paint, gradientDrawable);
                } else {
                    canvas.drawArc(oval, currentAngle, angle, true, paint);
                }
            }
            paint.setColor(seriesRenderer.getColor());
            //paint.setShader(null);
            drawLabel(canvas, mDataset.getCategory(i), mRenderer, prevLabelsBounds, mCenterX, mCenterY,
                    shortRadius, longRadius, currentAngle, angle, left, right, mRenderer.getLabelsColor(),
                    paint, true, false);
            if (mRenderer.isDisplayValues()) {
                drawLabel(
                        canvas,
                        getLabel(mRenderer.getSeriesRendererAt(i).getChartValuesFormat(), mDataset.getValue(i)),
                        mRenderer, prevLabelsBounds, mCenterX, mCenterY, shortRadius / 2, longRadius / 2,
                        currentAngle, angle, left, right, mRenderer.getLabelsColor(), paint, false, true);
            }

            // Save details for getSeries functionality
            if (loadPieCfg) {
                mPieMapper.addPieSegment(i, value, currentAngle, angle);
            }
            currentAngle += angle;
        }
        prevLabelsBounds.clear();
        drawLegend(canvas, mRenderer, titles, left, right, y, width, height, legendSize, paint, false);
        drawTitle(canvas, x, y, width, paint);
    }

  public SeriesSelection getSeriesAndPointForScreenCoordinate(Point screenPoint) {
    return mPieMapper.getSeriesAndPointForScreenCoordinate(screenPoint);
  }
  
  
  /**
   * Gets the shape of a pie segment given its point index.
   * @param pointIndex The point index representing the pie segment.
   * @return A shape of the outline of the segment.
   */
  public Shape getSegmentShape(int pointIndex){
      return mPieMapper.getSegmentShape(pointIndex);
  }

}
