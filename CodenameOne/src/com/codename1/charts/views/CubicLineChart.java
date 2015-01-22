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

import java.util.List;
import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.charts.compat.PathMeasure;

import com.codename1.charts.models.Point;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.ui.geom.GeneralPath;



/**
 * The interpolated (cubic) line chart rendering class.
 */
public class CubicLineChart extends LineChart {
  /** The chart type. */
  public static final String TYPE = "Cubic";

  private float mFirstMultiplier;

  private float mSecondMultiplier;
  /** A path measure for retrieving the points on the path. */
  private PathMeasure mPathMeasure;

  public CubicLineChart() {
    // default is to have first control point at about 33% of the distance,
    mFirstMultiplier = 0.33f;
    // and the next at 66% of the distance.
    mSecondMultiplier = 1 - mFirstMultiplier;
  }

  /**
   * Builds a cubic line chart.
   * 
   * @param dataset the dataset
   * @param renderer the renderer
   * @param smoothness smoothness determines how smooth the curve should be,
   *          range [0->0.5] super smooth, 0.5, means that it might not get
   *          close to control points if you have random data // less smooth,
   *          (close to 0) means that it will most likely touch all control //
   *          points
   */
  public CubicLineChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer,
      float smoothness) {
    super(dataset, renderer);
    mFirstMultiplier = smoothness;
    mSecondMultiplier = 1 - mFirstMultiplier;
  }

  @Override
  protected void drawPath(Canvas canvas, List<Float> points, Paint paint, boolean circular) {
    GeneralPath p = new GeneralPath();
    float x = points.get(0);
    float y = points.get(1);
    p.moveTo(x, y);

    int length = points.size();
    if (circular) {
      length -= 4;
    }

    Point p1 = new Point();
    Point p2 = new Point();
    Point p3 = new Point();
    for (int i = 0; i < length; i += 2) {
      int nextIndex = i + 2 < length ? i + 2 : i;
      int nextNextIndex = i + 4 < length ? i + 4 : nextIndex;
      calc(points, p1, i, nextIndex, mSecondMultiplier);
      p2.setX(points.get(nextIndex));
      p2.setY(points.get(nextIndex + 1));
      calc(points, p3, nextIndex, nextNextIndex, mFirstMultiplier);
      // From last point, approaching x1/y1 and x2/y2 and ends up at x3/y3
      p.curveTo(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
    }
    mPathMeasure = new PathMeasure(p, false);
    if (circular) {
      for (int i = length; i < length + 4; i += 2) {
        p.lineTo(points.get(i), points.get(i + 1));
      }
      p.lineTo(points.get(0), points.get(1));
    }
    canvas.drawPath(p, paint);
  }

  private void calc(List<Float> points, Point result, int index1, int index2, final float multiplier) {
    float p1x = points.get(index1);
    float p1y = points.get(index1 + 1);
    float p2x = points.get(index2);
    float p2y = points.get(index2 + 1);

    float diffX = p2x - p1x; // p2.x - p1.x;
    float diffY = p2y - p1y; // p2.y - p1.y;
    result.setX(p1x + (diffX * multiplier));
    result.setY(p1y + (diffY * multiplier));
  }

  /**
   * Draws the series points.
   * 
   * @param canvas the canvas
   * @param paint the paint object
   * @param pointsList the points to be rendered
   * @param seriesRenderer the series renderer
   * @param yAxisValue the y axis value in pixels
   * @param seriesIndex the series index
   * @param startIndex the start index of the rendering points
   */
  protected void drawPoints(Canvas canvas, Paint paint, List<Float> pointsList,
      XYSeriesRenderer seriesRenderer, float yAxisValue, int seriesIndex, int startIndex) {
    if (isRenderPoints(seriesRenderer)) {
      ScatterChart pointsChart = getPointsChart();
      if (pointsChart != null) {
        int length = (int) mPathMeasure.getLength();
        int pointsLength = pointsList.size();
        float[] coords = new float[2];
        for (int i = 0; i < length; i++) {
          mPathMeasure.getPosTan(i, coords, null);
          double prevDiff = Double.MAX_VALUE;
          boolean ok = true;
          for (int j = 0; j < pointsLength && ok; j += 2) {
            double diff = Math.abs(pointsList.get(j) - coords[0]);
            if (diff < 1) {
              pointsList.set(j + 1, coords[1]);
              prevDiff = diff;
            }
            ok = prevDiff > diff;
          }
        }
        pointsChart.drawSeries(canvas, paint, pointsList, seriesRenderer, yAxisValue, seriesIndex,
            startIndex);
      }
    }
  }

  /**
   * Returns the chart type identifier.
   * 
   * @return the chart type
   */
  public String getChartType() {
    return TYPE;
  }

}
