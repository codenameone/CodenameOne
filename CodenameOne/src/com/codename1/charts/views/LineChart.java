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
import com.codename1.charts.compat.Paint;
import com.codename1.charts.compat.Paint.Style;

import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer.FillOutsideLine;



/**
 * The line chart rendering class.
 */
public class LineChart extends XYChart {
  /** The constant to identify this chart type. */
  public static final String TYPE = "Line";
  /** The legend shape width. */
  private static final int SHAPE_WIDTH = 30;
  /** The scatter chart to be used to draw the data points. */
  private ScatterChart pointsChart;

  LineChart() {
  }

  /**
   * Builds a new line chart instance.
   * 
   * @param dataset the multiple series dataset
   * @param renderer the multiple series renderer
   */
  public LineChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
    super(dataset, renderer);
    pointsChart = new ScatterChart(dataset, renderer);
  }

  /**
   * Sets the series and the renderer.
   * 
   * @param dataset the series dataset
   * @param renderer the series renderer
   */
  protected void setDatasetRenderer(XYMultipleSeriesDataset dataset,
      XYMultipleSeriesRenderer renderer) {
    super.setDatasetRenderer(dataset, renderer);
    pointsChart = new ScatterChart(dataset, renderer);
  }

  /**
   * The graphical representation of a series.
   * 
   * @param canvas the canvas to paint to
   * @param paint the paint to be used for drawing
   * @param points the array of points to be used for drawing the series
   * @param seriesRenderer the series renderer
   * @param yAxisValue the minimum value of the y axis
   * @param seriesIndex the index of the series currently being drawn
   * @param startIndex the start index of the rendering points
   */
  @Override
  public void drawSeries(Canvas canvas, Paint paint, List<Float> points, XYSeriesRenderer renderer,
      float yAxisValue, int seriesIndex, int startIndex) {
    float lineWidth = paint.getStrokeWidth();
    paint.setStrokeWidth(renderer.getLineWidth());
    final FillOutsideLine[] fillOutsideLine = renderer.getFillOutsideLine();

    for (FillOutsideLine fill : fillOutsideLine) {
      if (fill.getType() != FillOutsideLine.Type.NONE) {
        paint.setColor(fill.getColor());
        // TODO: find a way to do area charts without duplicating data
        List<Float> fillPoints = new ArrayList<Float>();
        int[] range = fill.getFillRange();
        if (range == null) {
          fillPoints.addAll(points);
        } else {
          if (points.size() > range[0] * 2 && points.size() > range[1] * 2) {
            fillPoints.addAll(points.subList(range[0] * 2, range[1] * 2));
          }
        }

        final float referencePoint;
        switch (fill.getType()) {
        case BOUNDS_ALL:
          referencePoint = yAxisValue;
          break;
        case BOUNDS_BELOW:
          referencePoint = yAxisValue;
          break;
        case BOUNDS_ABOVE:
          referencePoint = yAxisValue;
          break;
        case BELOW:
          referencePoint = canvas.getHeight();
          break;
        case ABOVE:
          referencePoint = 0;
          break;
        default:
          throw new RuntimeException(
              "You have added a new type of filling but have not implemented.");
        }
        if (fill.getType() == FillOutsideLine.Type.BOUNDS_ABOVE
            || fill.getType() == FillOutsideLine.Type.BOUNDS_BELOW) {
          List<Float> boundsPoints = new ArrayList<Float>();
          boolean add = false;
          int length = fillPoints.size();
          if (length > 0 && fill.getType() == FillOutsideLine.Type.BOUNDS_ABOVE
              && fillPoints.get(1) < referencePoint
              || fill.getType() == FillOutsideLine.Type.BOUNDS_BELOW
              && fillPoints.get(1) > referencePoint) {
            boundsPoints.add(fillPoints.get(0));
            boundsPoints.add(fillPoints.get(1));
            add = true;
          }

          for (int i = 3; i < length; i += 2) {
            float prevValue = fillPoints.get(i - 2);
            float value = fillPoints.get(i);

            if (prevValue < referencePoint && value > referencePoint || prevValue > referencePoint
                && value < referencePoint) {
              float prevX = fillPoints.get(i - 3);
              float x = fillPoints.get(i - 1);
              boundsPoints.add(prevX + (x - prevX) * (referencePoint - prevValue)
                  / (value - prevValue));
              boundsPoints.add(referencePoint);
              if (fill.getType() == FillOutsideLine.Type.BOUNDS_ABOVE && value > referencePoint
                  || fill.getType() == FillOutsideLine.Type.BOUNDS_BELOW && value < referencePoint) {
                i += 2;
                add = false;
              } else {
                boundsPoints.add(x);
                boundsPoints.add(value);
                add = true;
              }
            } else {
              if (add || fill.getType() == FillOutsideLine.Type.BOUNDS_ABOVE
                  && value < referencePoint || fill.getType() == FillOutsideLine.Type.BOUNDS_BELOW
                  && value > referencePoint) {
                boundsPoints.add(fillPoints.get(i - 1));
                boundsPoints.add(value);
              }
            }
          }

          fillPoints.clear();
          fillPoints.addAll(boundsPoints);
        }
        int length = fillPoints.size();
        if (length > 0) {
          fillPoints.set(0, fillPoints.get(0) + 1);
          fillPoints.add(fillPoints.get(length - 2));
          fillPoints.add(referencePoint);
          fillPoints.add(fillPoints.get(0));
          fillPoints.add(fillPoints.get(length + 1));
          for (int i = 0; i < length + 4; i += 2) {
            if (fillPoints.get(i + 1) < 0) {
              fillPoints.set(i + 1, 0f);
            }
          }

          paint.setStyle(Style.FILL);
          drawPath(canvas, fillPoints, paint, true);
        }
      }
    }
    paint.setColor(renderer.getColor());
    paint.setStyle(Style.STROKE);
    drawPath(canvas, points, paint, false);
    paint.setStrokeWidth(lineWidth);
  }

  @Override
  protected ClickableArea[] clickableAreasForPoints(List<Float> points, List<Double> values,
      float yAxisValue, int seriesIndex, int startIndex) {
    int length = points.size();
    ClickableArea[] ret = new ClickableArea[length / 2];
    for (int i = 0; i < length; i += 2) {
      int selectableBuffer = mRenderer.getSelectableBuffer();
      ret[i / 2] = new ClickableArea(PkgUtils.makeRect(points.get(i) - selectableBuffer, points.get(i + 1)
          - selectableBuffer, points.get(i) + selectableBuffer, points.get(i + 1)
          + selectableBuffer), values.get(i), values.get(i + 1));
    }
    return ret;
  }

  /**
   * Returns the legend shape width.
   * 
   * @param seriesIndex the series index
   * @return the legend shape width
   */
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
  public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y,
      int seriesIndex, Paint paint) {
    canvas.drawLine(x, y, x + SHAPE_WIDTH, y, paint);
    if (isRenderPoints(renderer)) {
      pointsChart.drawLegendShape(canvas, renderer, x + 5, y, seriesIndex, paint);
    }
  }

  /**
   * Returns if the chart should display the points as a certain shape.
   * 
   * @param renderer the series renderer
   */
  public boolean isRenderPoints(SimpleSeriesRenderer renderer) {
    return ((XYSeriesRenderer) renderer).getPointStyle() != PointStyle.POINT;
  }

  /**
   * Returns the scatter chart to be used for drawing the data points.
   * 
   * @return the data points scatter chart
   */
  public ScatterChart getPointsChart() {
    return pointsChart;
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
