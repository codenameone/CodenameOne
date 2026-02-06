/// Copyright (C) 2009 - 2013 SC 4ViewSoft SRL
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
/// http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
package com.codename1.charts.renderers;

import com.codename1.charts.renderers.XYSeriesRenderer.FillOutsideLine.Type;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.PointStyle;
import com.codename1.ui.Component;
import com.codename1.ui.Font;

import java.util.ArrayList;
import java.util.List;


/// A renderer for the XY type series.
public class XYSeriesRenderer extends SimpleSeriesRenderer { // PMD Fix: UnnecessarySemicolon cleaned extra delimiter
    /// If the chart should be filled outside its line.
    private final List<FillOutsideLine> mFillBelowLine = new ArrayList<FillOutsideLine>();
    /// If the chart points should be filled.
    private boolean mFillPoints = false;
    /// The point style.
    private PointStyle mPointStyle = PointStyle.POINT;
    /// The point stroke width
    private float mPointStrokeWidth = 1;
    /// The chart line width.
    private float mLineWidth = 1;
    /// If the values should be displayed above the chart points.
    private boolean mDisplayChartValues;
    /// The minimum distance between displaying chart values.
    private int mDisplayChartValuesDistance = 100;
    /// The chart values text size.
    private float mChartValuesTextSize = 10;
    /// The chart values text alignment.
    private int mChartValuesTextAlign = Component.CENTER;
    /// The chart values spacing from the data point.
    private float mChartValuesSpacing = 5f;
    /// The annotations text size.
    private float mAnnotationsTextSize = 10;
    /// The annotations text alignment.
    private int mAnnotationsTextAlign = Component.CENTER;
    /// The annotations color.
    private int mAnnotationsColor = DefaultRenderer.TEXT_COLOR;

    /// Returns if the chart should be filled below the line.
    ///
    /// #### Returns
    ///
    /// the fill below line status
    ///
    /// #### Deprecated
    ///
    /// Use `#getFillOutsideLine()` instead.
    @Deprecated
    public boolean isFillBelowLine() {
        return !mFillBelowLine.isEmpty();
    }

    /// Sets if the line chart should be filled below its line. Filling below the
    /// line transforms a line chart into an area chart.
    ///
    /// #### Parameters
    ///
    /// - `fill`: the fill below line flag value
    ///
    /// #### Deprecated
    ///
    /// Use `#addFillOutsideLine(FillOutsideLine)` instead.
    @Deprecated
    public void setFillBelowLine(boolean fill) {
        mFillBelowLine.clear();
        if (fill) {
            mFillBelowLine.add(new FillOutsideLine(Type.BOUNDS_ALL));
        } else {
            mFillBelowLine.add(new FillOutsideLine(Type.NONE));
        }
    }

    /// Returns the type of the outside fill of the line.
    ///
    /// #### Returns
    ///
    /// the type of the outside fill of the line.
    public FillOutsideLine[] getFillOutsideLine() {
        FillOutsideLine[] out = new FillOutsideLine[mFillBelowLine.size()];
        return mFillBelowLine.toArray(out);
    }

    /// Sets if the line chart should be filled outside its line. Filling outside
    /// with FillOutsideLine.INTEGRAL the line transforms a line chart into an area
    /// chart.
    ///
    /// #### Parameters
    ///
    /// - `fill`: the type of the filling
    public void addFillOutsideLine(FillOutsideLine fill) {
        mFillBelowLine.add(fill);
    }

    /// Returns if the chart points should be filled.
    ///
    /// #### Returns
    ///
    /// the points fill status
    public boolean isFillPoints() {
        return mFillPoints;
    }

    /// Sets if the chart points should be filled.
    ///
    /// #### Parameters
    ///
    /// - `fill`: the points fill flag value
    public void setFillPoints(boolean fill) {
        mFillPoints = fill;
    }

    /// Sets the fill below the line color.
    ///
    /// #### Parameters
    ///
    /// - `color`: the fill below line color
    ///
    /// #### Deprecated
    ///
    /// Use FillOutsideLine.setColor instead
    @Deprecated
    public void setFillBelowLineColor(int color) {
        if (!mFillBelowLine.isEmpty()) {
            mFillBelowLine.get(0).setColor(color);
        }
    }

    /// Returns the point style.
    ///
    /// #### Returns
    ///
    /// the point style
    public PointStyle getPointStyle() {
        return mPointStyle;
    }

    /// Sets the point style.
    ///
    /// #### Parameters
    ///
    /// - `style`: the point style
    public void setPointStyle(PointStyle style) {
        mPointStyle = style;
    }

    /// Returns the point stroke width in pixels.
    ///
    /// #### Returns
    ///
    /// the point stroke width in pixels
    public float getPointStrokeWidth() {
        return mPointStrokeWidth;
    }

    /// Sets the point stroke width in pixels.
    ///
    /// #### Parameters
    ///
    /// - `strokeWidth`: the point stroke width in pixels
    public void setPointStrokeWidth(float strokeWidth) {
        mPointStrokeWidth = strokeWidth;
    }

    /// Returns the chart line width.
    ///
    /// #### Returns
    ///
    /// the line width
    public float getLineWidth() {
        return mLineWidth;
    }

    /// Sets the chart line width.
    ///
    /// #### Parameters
    ///
    /// - `lineWidth`: the line width
    public void setLineWidth(float lineWidth) {
        mLineWidth = lineWidth;
    }

    /// Returns if the chart point values should be displayed as text.
    ///
    /// #### Returns
    ///
    /// if the chart point values should be displayed as text
    public boolean isDisplayChartValues() {
        return mDisplayChartValues;
    }

    /// Sets if the chart point values should be displayed as text.
    ///
    /// #### Parameters
    ///
    /// - `display`: if the chart point values should be displayed as text
    public void setDisplayChartValues(boolean display) {
        mDisplayChartValues = display;
    }

    /// Returns the chart values minimum distance.
    ///
    /// #### Returns
    ///
    /// the chart values minimum distance
    public int getDisplayChartValuesDistance() {
        return mDisplayChartValuesDistance;
    }

    /// Sets chart values minimum distance.
    ///
    /// #### Parameters
    ///
    /// - `distance`: the chart values minimum distance
    public void setDisplayChartValuesDistance(int distance) {
        mDisplayChartValuesDistance = distance;
    }

    /// Returns the chart values text size.
    ///
    /// #### Returns
    ///
    /// the chart values text size
    public float getChartValuesTextSize() {
        return mChartValuesTextSize;
    }

    /// Sets the chart values text size. Consider using  `#setChartValuesTextFont(com.codename1.ui.Font)`
    /// instead to allow the font size to to be adjusted appropriately for the display
    /// resolution.
    ///
    /// #### Parameters
    ///
    /// - `textSize`: the chart values text size
    public void setChartValuesTextSize(float textSize) {
        mChartValuesTextSize = textSize;
    }

    /// Sets the chart values title text font size using a Font object instead of a point size.
    /// This method is the preferred way to set font size because it allows you to
    /// more easily have fonts appear in an appropriate size for the target device.
    ///
    /// Alternatively check out `#setChartValuesTextSize(float)` to set the text
    /// size in pixels.
    ///
    /// #### Parameters
    ///
    /// - `font`
    public void setChartValuesTextFont(Font font) {
        setChartValuesTextSize(font.getHeight());
    }

    /// Returns the chart values text align.
    ///
    /// #### Returns
    ///
    /// the chart values text align
    public int getChartValuesTextAlign() {
        return mChartValuesTextAlign;
    }

    /// Sets the chart values text align.
    ///
    /// #### Parameters
    ///
    /// - `align`: the chart values text align
    public void setChartValuesTextAlign(int align) {
        mChartValuesTextAlign = align;
    }

    /// Returns the chart values spacing from the data point.
    ///
    /// #### Returns
    ///
    /// the chart values spacing
    public float getChartValuesSpacing() {
        return mChartValuesSpacing;
    }

    /// Sets the chart values spacing from the data point.
    ///
    /// #### Parameters
    ///
    /// - `spacing`: @param spacing the chart values spacing (in pixels) from the chart data
    /// point
    public void setChartValuesSpacing(float spacing) {
        mChartValuesSpacing = spacing;
    }

    /// Returns the annotations text size.
    ///
    /// #### Returns
    ///
    /// the annotations text size
    public float getAnnotationsTextSize() {
        return mAnnotationsTextSize;
    }

    /// Sets the annotations text size. Consider using  `#setAnnotationsTextFont(com.codename1.ui.Font)`
    /// instead to allow the font size to to be adjusted appropriately for the display
    /// resolution.
    ///
    /// #### Parameters
    ///
    /// - `textSize`: the annotations text size
    public void setAnnotationsTextSize(float textSize) {
        mAnnotationsTextSize = textSize;
    }

    /// Sets the annotations text font size using a Font object instead of a point size.
    /// This method is the preferred way to set font size because it allows you to
    /// more easily have fonts appear in an appropriate size for the target device.
    ///
    /// Alternatively check out `#setAnnotationsTextSize(float)` to set the text
    /// size in pixels.
    ///
    /// #### Parameters
    ///
    /// - `font`
    public void setAnnotationsTextFont(Font font) {
        setAnnotationsTextSize(font.getHeight());
    }

    /// Returns the annotations text align.
    ///
    /// #### Returns
    ///
    /// the annotations text align
    public int getAnnotationsTextAlign() {
        return mAnnotationsTextAlign;
    }

    /// Sets the annotations text align.
    ///
    /// #### Parameters
    ///
    /// - `align`: the chart values text align
    public void setAnnotationsTextAlign(int align) {
        mAnnotationsTextAlign = align;
    }

    /// Returns the annotations color.
    ///
    /// #### Returns
    ///
    /// the annotations color
    public int getAnnotationsColor() {
        return mAnnotationsColor;
    }

    /// Sets the annotations color.
    ///
    /// #### Parameters
    ///
    /// - `color`: the annotations color
    public void setAnnotationsColor(int color) {
        mAnnotationsColor = color;
    }

    /// A descriptor for the line fill behavior.
    public static class FillOutsideLine {
        /// The fill type.
        private final Type mType;
        /// The fill color.
        private int mColor = ColorUtil.argb(125, 0, 0, 200);
        /// The fill points index range.
        private int[] mFillRange;

        /// The line fill behavior.
        ///
        /// #### Parameters
        ///
        /// - `type`: the fill type
        public FillOutsideLine(Type type) {
            this.mType = type;
        }

        /// Returns the fill color.
        ///
        /// #### Returns
        ///
        /// the fill color
        public int getColor() {
            return mColor;
        }

        /// Sets the fill color
        ///
        /// #### Parameters
        ///
        /// - `color`: the fill color
        public void setColor(int color) {
            mColor = color;
        }

        /// Returns the fill type.
        ///
        /// #### Returns
        ///
        /// the fill type
        public Type getType() {
            return mType;
        }

        /// Returns the fill range which is the minimum and maximum data index values
        /// for the fill.
        ///
        /// #### Returns
        ///
        /// the fill range
        public int[] getFillRange() {
            return mFillRange;
        }

        /// Sets the fill range which is the minimum and maximum data index values
        /// for the fill.
        ///
        /// #### Parameters
        ///
        /// - `range`: the fill range
        public void setFillRange(int[] range) {
            mFillRange = range;
        }

        public enum Type {
            NONE, BOUNDS_ALL, BOUNDS_BELOW, BOUNDS_ABOVE, BELOW, ABOVE
        }
    }

}
