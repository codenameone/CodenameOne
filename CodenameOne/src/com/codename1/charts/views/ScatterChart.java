/**
 * Copyright (C) 2009 - 2013 SC 4ViewSoft SRL
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;

import java.util.List;


/**
 * The scatter chart rendering class.
 */
public class ScatterChart extends XYChart {
    /** The constant to identify this chart type. */
    public static final String TYPE = "Scatter";
    /** The default point shape size. */
    private static final float SIZE = 3;
    /** The legend shape width. */
    private static final int SHAPE_WIDTH = 10;
    /** The point shape size. */
    private float size = SIZE;

    ScatterChart() {
    }

    /**
     * Builds a new scatter chart instance.
     *
     * @param dataset the multiple series dataset
     * @param renderer the multiple series renderer
     */
    public ScatterChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
        super(dataset, renderer);
        size = renderer.getPointSize();
    }

    // TODO: javadoc
    protected void setDatasetRenderer(XYMultipleSeriesDataset dataset,
                                      XYMultipleSeriesRenderer renderer) {
        super.setDatasetRenderer(dataset, renderer);
        size = renderer.getPointSize();
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
    public void drawSeries(Canvas canvas, Paint paint, List<Float> points,
                           XYSeriesRenderer renderer, float yAxisValue, int seriesIndex, int startIndex) {
        paint.setColor(renderer.getColor());
        final float stroke = paint.getStrokeWidth();
        if (renderer.isFillPoints()) {
            paint.setStyle(Style.FILL);
        } else {
            paint.setStrokeWidth(renderer.getPointStrokeWidth());
            paint.setStyle(Style.STROKE);
        }
        int length = points.size();
        // switch on ENUM's generates reflection code that screws up J2ME
        PointStyle ps = renderer.getPointStyle();
        if (ps == PointStyle.X) {
            paint.setStrokeWidth(renderer.getPointStrokeWidth());
            for (int i = 0; i < length; i += 2) {
                drawX(canvas, paint, points.get(i), points.get(i + 1));
            }
        } else {
            if (ps == PointStyle.CIRCLE) {
                for (int i = 0; i < length; i += 2) {
                    drawCircle(canvas, paint, points.get(i), points.get(i + 1));
                }
            } else {
                if (ps == PointStyle.TRIANGLE) {
                    float[] path = new float[6];
                    for (int i = 0; i < length; i += 2) {
                        drawTriangle(canvas, paint, path, points.get(i), points.get(i + 1));
                    }
                } else {
                    if (ps == PointStyle.SQUARE) {
                        for (int i = 0; i < length; i += 2) {
                            drawSquare(canvas, paint, points.get(i), points.get(i + 1));
                        }
                    } else {
                        if (ps == PointStyle.DIAMOND) {
                            float[] path = new float[8];
                            for (int i = 0; i < length; i += 2) {
                                drawDiamond(canvas, paint, path, points.get(i), points.get(i + 1));
                            }
                        } else {
                            if (ps == PointStyle.POINT) {
                                for (int i = 0; i < length; i += 2) {
                                    canvas.drawPoint(points.get(i), points.get(i + 1), paint);
                                }
                            }
                        }
                    }
                }
            }
        }

    /*switch (renderer.getPointStyle()) {
    case X:
      paint.setStrokeWidth(renderer.getPointStrokeWidth());
      for (int i = 0; i < length; i += 2) {
        drawX(canvas, paint, points.get(i), points.get(i + 1));
      }
      break;
    case CIRCLE:
      for (int i = 0; i < length; i += 2) {
        drawCircle(canvas, paint, points.get(i), points.get(i + 1));
      }
      break;
    case TRIANGLE:
      float[] path = new float[6];
      for (int i = 0; i < length; i += 2) {
        drawTriangle(canvas, paint, path, points.get(i), points.get(i + 1));
      }
      break;
    case SQUARE:
      for (int i = 0; i < length; i += 2) {
        drawSquare(canvas, paint, points.get(i), points.get(i + 1));
      }
      break;
    case DIAMOND:
      path = new float[8];
      for (int i = 0; i < length; i += 2) {
        drawDiamond(canvas, paint, path, points.get(i), points.get(i + 1));
      }
      break;
    case POINT:
      for (int i = 0; i < length; i += 2) {
        canvas.drawPoint(points.get(i), points.get(i + 1), paint);
      }
      break;
    }*/
        paint.setStrokeWidth(stroke);
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
        if (((XYSeriesRenderer) renderer).isFillPoints()) {
            paint.setStyle(Style.FILL);
        } else {
            paint.setStyle(Style.STROKE);
        }

        // switch on ENUM's generates reflection code that screws up J2ME
        PointStyle ps = ((XYSeriesRenderer) renderer).getPointStyle();
        if (ps == PointStyle.X) {
            drawX(canvas, paint, x + SHAPE_WIDTH, y);
        } else {
            if (ps == PointStyle.CIRCLE) {
                drawCircle(canvas, paint, x + SHAPE_WIDTH, y);
            } else {
                if (ps == PointStyle.TRIANGLE) {
                    drawTriangle(canvas, paint, new float[6], x + SHAPE_WIDTH, y);
                } else {
                    if (ps == PointStyle.SQUARE) {
                        drawSquare(canvas, paint, x + SHAPE_WIDTH, y);
                    } else {
                        if (ps == PointStyle.DIAMOND) {
                            drawDiamond(canvas, paint, new float[8], x + SHAPE_WIDTH, y);
                        } else {
                            if (ps == PointStyle.POINT) {
                                drawDiamond(canvas, paint, new float[8], x + SHAPE_WIDTH, y);
                            }
                        }
                    }
                }
            }
        }
    /*switch (((XYSeriesRenderer) renderer).getPointStyle()) {
    case X:
      drawX(canvas, paint, x + SHAPE_WIDTH, y);
      break;
    case CIRCLE:
      drawCircle(canvas, paint, x + SHAPE_WIDTH, y);
      break;
    case TRIANGLE:
      drawTriangle(canvas, paint, new float[6], x + SHAPE_WIDTH, y);
      break;
    case SQUARE:
      drawSquare(canvas, paint, x + SHAPE_WIDTH, y);
      break;
    case DIAMOND:
      drawDiamond(canvas, paint, new float[8], x + SHAPE_WIDTH, y);
      break;
    case POINT:
      canvas.drawPoint(x + SHAPE_WIDTH, y, paint);
      break;
    }*/
    }

    /**
     * The graphical representation of an X point shape.
     *
     * @param canvas the canvas to paint to
     * @param paint the paint to be used for drawing
     * @param x the x value of the point the shape should be drawn at
     * @param y the y value of the point the shape should be drawn at
     */
    private void drawX(Canvas canvas, Paint paint, float x, float y) {
        canvas.drawLine(x - size, y - size, x + size, y + size, paint);
        canvas.drawLine(x + size, y - size, x - size, y + size, paint);
    }

    /**
     * The graphical representation of a circle point shape.
     *
     * @param canvas the canvas to paint to
     * @param paint the paint to be used for drawing
     * @param x the x value of the point the shape should be drawn at
     * @param y the y value of the point the shape should be drawn at
     */
    private void drawCircle(Canvas canvas, Paint paint, float x, float y) {
        canvas.drawCircle(x, y, size, paint);
    }

    /**
     * The graphical representation of a triangle point shape.
     *
     * @param canvas the canvas to paint to
     * @param paint the paint to be used for drawing
     * @param path the triangle path
     * @param x the x value of the point the shape should be drawn at
     * @param y the y value of the point the shape should be drawn at
     */
    private void drawTriangle(Canvas canvas, Paint paint, float[] path, float x, float y) {
        path[0] = x;
        path[1] = y - size - size / 2;
        path[2] = x - size;
        path[3] = y + size;
        path[4] = x + size;
        path[5] = path[3];
        drawPath(canvas, path, paint, true);
    }

    /**
     * The graphical representation of a square point shape.
     *
     * @param canvas the canvas to paint to
     * @param paint the paint to be used for drawing
     * @param x the x value of the point the shape should be drawn at
     * @param y the y value of the point the shape should be drawn at
     */
    private void drawSquare(Canvas canvas, Paint paint, float x, float y) {
        canvas.drawRect(x - size, y - size, x + size, y + size, paint);
    }

    /**
     * The graphical representation of a diamond point shape.
     *
     * @param canvas the canvas to paint to
     * @param paint the paint to be used for drawing
     * @param path the diamond path
     * @param x the x value of the point the shape should be drawn at
     * @param y the y value of the point the shape should be drawn at
     */
    private void drawDiamond(Canvas canvas, Paint paint, float[] path, float x, float y) {
        path[0] = x;
        path[1] = y - size;
        path[2] = x - size;
        path[3] = y;
        path[4] = x;
        path[5] = y + size;
        path[6] = x + size;
        path[7] = y;
        drawPath(canvas, path, paint, true);
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