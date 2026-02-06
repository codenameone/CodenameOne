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

import com.codename1.ui.Font;

import java.util.ArrayList;
import java.util.List;


/// An abstract renderer to be extended by the multiple series classes.
public class DefaultRenderer { // PMD Fix: UnusedPrivateField removed unused text font constant
    /// A no color constant.
    public static final int NO_COLOR = 0;
    /// The default background color.
    public static final int BACKGROUND_COLOR = 0x0;
    /// The default color for text.
    public static final int TEXT_COLOR = 0xEAEAEA;
    /// The simple renderers that are included in this multiple series renderer.
    private final List<SimpleSeriesRenderer> mRenderers = new ArrayList<SimpleSeriesRenderer>();
    /// The chart title.
    private String mChartTitle = "";
    /// The chart title text size.
    private float mChartTitleTextSize = 15;
    /// The typeface name for the texts.
    private int mTextTypefaceName = Font.FACE_SYSTEM;
    /// The typeface style for the texts.
    private int mTextTypefaceStyle = Font.STYLE_PLAIN;
    /// The typeface for the texts
    private Font mTextTypeface;
    /// The chart background color.
    private int mBackgroundColor;
    /// If the background color is applied.
    private boolean mApplyBackgroundColor;
    /// If the axes are visible.
    private boolean mShowAxes = true;
    /// The Y axis color.
    private int mYAxisColor = TEXT_COLOR;
    /// The X axis color.
    private int mXAxisColor = TEXT_COLOR;
    /// If the labels are visible.
    private boolean mShowLabels = true;
    /// If the tick marks are visible.
    private boolean mShowTickMarks = true;
    /// The labels color.
    private int mLabelsColor = TEXT_COLOR;
    /// The labels text size.
    private float mLabelsTextSize = 10;
    /// If the legend is visible.
    private boolean mShowLegend = true;
    /// The legend text size.
    private float mLegendTextSize = 12;
    /// If the legend should size to fit.
    private boolean mFitLegend = false;
    /// If the X axis grid should be displayed.
    private boolean mShowGridX = false;
    /// If the Y axis grid should be displayed.
    private boolean mShowGridY = false;
    /// If the custom text grid should be displayed on the X axis.
    private boolean mShowCustomTextGridX = false;
    /// If the custom text grid should be displayed on the Y axis.
    private boolean mShowCustomTextGridY = false;
    /// The antialiasing flag.
    private boolean mAntialiasing = true;
    /// The legend height.
    private int mLegendHeight = 0;
    /// The margins size.
    private int[] mMargins = new int[]{20, 30, 10, 20};
    /// A value to be used for scaling the chart.
    private float mScale = 1;
    /// The original chart scale.
    private final float mOriginalScale = mScale;
    /// A flag for enabling the pan.
    private boolean mPanEnabled = true;
    /// A flag for enabling the zoom.
    private boolean mZoomEnabled = true;
    /// A flag for enabling the visibility of the zoom buttons.
    private boolean mZoomButtonsVisible = false;
    /// The zoom rate.
    private float mZoomRate = 1.5f;
    /// A flag for enabling the external zoom.
    private boolean mExternalZoomEnabled = false;
    /// A flag for enabling the click on elements.
    private boolean mClickEnabled = false;
    /// The selectable radius around a clickable point.
    private int selectableBuffer = 15;
    /// If the chart should display the values (available for pie chart).
    private boolean mDisplayValues;

    /// A flag to be set if the chart is inside a scroll and doesn't need to shrink
    /// when not enough space.
    private boolean mInScroll;
    /// The start angle for circular charts such as pie, doughnut, etc.
    private float mStartAngle = 0;

    /// Returns the chart title.
    ///
    /// #### Returns
    ///
    /// the chart title
    public String getChartTitle() {
        return mChartTitle;
    }

    /// Sets the chart title.
    ///
    /// #### Parameters
    ///
    /// - `title`: the chart title
    public void setChartTitle(String title) {
        mChartTitle = title;
    }

    /// Returns the chart title text size.
    ///
    /// #### Returns
    ///
    /// the chart title text size
    public float getChartTitleTextSize() {
        return mChartTitleTextSize;
    }


    /// Sets the chart title text size in pixels.  Consider using `#setChartTitleTextFont(com.codename1.ui.Font)`
    /// instead of this method to allow the text to be sized appropriately for the
    /// device resolution.
    ///
    /// #### Parameters
    ///
    /// - `textSize`: the chart title text size
    public void setChartTitleTextSize(float textSize) {
        mChartTitleTextSize = textSize;
    }

    /// Sets the chart title font size using a Font object instead of a point size.
    /// This method is the preferred way to set font size because it allows you to
    /// more easily have fonts appear in an appropriate size for the target device.
    ///
    /// Alternatively check out `#setChartTitleTextSize(float)` to set the text
    /// size in pixels.
    ///
    /// #### Parameters
    ///
    /// - `font`
    public void setChartTitleTextFont(Font font) {
        setChartTitleTextSize(font.getHeight());

    }

    /// Adds a simple renderer to the multiple renderer.
    ///
    /// #### Parameters
    ///
    /// - `renderer`: the renderer to be added
    public void addSeriesRenderer(SimpleSeriesRenderer renderer) {
        mRenderers.add(renderer);
    }

    /// Adds a simple renderer to the multiple renderer.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index in the renderers list
    ///
    /// - `renderer`: the renderer to be added
    public void addSeriesRenderer(int index, SimpleSeriesRenderer renderer) {
        mRenderers.add(index, renderer);
    }

    /// Removes a simple renderer from the multiple renderer.
    ///
    /// #### Parameters
    ///
    /// - `renderer`: the renderer to be removed
    public void removeSeriesRenderer(SimpleSeriesRenderer renderer) {
        mRenderers.remove(renderer);
    }

    /// Removes all renderers from the multiple renderer.
    public void removeAllRenderers() {
        mRenderers.clear();
    }

    /// Returns the simple renderer from the multiple renderer list.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index in the simple renderers list
    ///
    /// #### Returns
    ///
    /// the simple renderer at the specified index
    public SimpleSeriesRenderer getSeriesRendererAt(int index) {
        return mRenderers.get(index);
    }

    /// Returns the simple renderers count in the multiple renderer list.
    ///
    /// #### Returns
    ///
    /// the simple renderers count
    public int getSeriesRendererCount() {
        return mRenderers.size();
    }

    /// Returns an array of the simple renderers in the multiple renderer list.
    ///
    /// #### Returns
    ///
    /// the simple renderers array
    public SimpleSeriesRenderer[] getSeriesRenderers() {
        SimpleSeriesRenderer[] out = new SimpleSeriesRenderer[mRenderers.size()];
        return mRenderers.toArray(out);
    }

    /// Returns the background color.
    ///
    /// #### Returns
    ///
    /// the background color
    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    /// Sets the background color.
    ///
    /// #### Parameters
    ///
    /// - `color`: the background color
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
    }

    /// Returns if the background color should be applied.
    ///
    /// #### Returns
    ///
    /// the apply flag for the background color.
    public boolean isApplyBackgroundColor() {
        return mApplyBackgroundColor;
    }

    /// Sets if the background color should be applied.
    ///
    /// #### Parameters
    ///
    /// - `apply`: the apply flag for the background color
    public void setApplyBackgroundColor(boolean apply) {
        mApplyBackgroundColor = apply;
    }

    /// Returns the axes color.
    ///
    /// #### Returns
    ///
    /// the axes color
    public int getAxesColor() {
        if (mXAxisColor != TEXT_COLOR) {
            return mXAxisColor;
        } else {
            return mYAxisColor;
        }
    }

    /// Sets the axes color.
    ///
    /// #### Parameters
    ///
    /// - `color`: the axes color
    public void setAxesColor(int color) {
        this.setXAxisColor(color);
        this.setYAxisColor(color);
    }

    /// Returns the color of the Y axis
    ///
    /// #### Returns
    ///
    /// the Y axis color
    public int getYAxisColor() {
        return mYAxisColor;
    }

    /// Sets the Y axis color.
    ///
    /// #### Parameters
    ///
    /// - `color`: the Y axis color
    public void setYAxisColor(int color) {
        mYAxisColor = color;
    }

    /// Returns the color of the X axis
    ///
    /// #### Returns
    ///
    /// the X axis color
    public int getXAxisColor() {
        return mXAxisColor;
    }

    /// Sets the X axis color.
    ///
    /// #### Parameters
    ///
    /// - `color`: the X axis color
    public void setXAxisColor(int color) {
        mXAxisColor = color;
    }

    /// Returns the labels color.
    ///
    /// #### Returns
    ///
    /// the labels color
    public int getLabelsColor() {
        return mLabelsColor;
    }

    /// Sets the labels color.
    ///
    /// #### Parameters
    ///
    /// - `color`: the labels color
    public void setLabelsColor(int color) {
        mLabelsColor = color;
    }

    /// Returns the labels text size.
    ///
    /// #### Returns
    ///
    /// the labels text size
    public float getLabelsTextSize() {
        return mLabelsTextSize;
    }

    /// Sets the labels text size.  Consider using  `#setLabelsTextFont(com.codename1.ui.Font)`
    /// instead to allow the font size to to be adjusted appropriately for the display
    /// resolution.
    ///
    /// #### Parameters
    ///
    /// - `textSize`: the labels text size
    public void setLabelsTextSize(float textSize) {
        mLabelsTextSize = textSize;
    }

    /// Sets the label title font size using a Font object instead of a point size.
    /// This method is the preferred way to set font size because it allows you to
    /// more easily have fonts appear in an appropriate size for the target device.
    ///
    /// Alternatively check out `#setLabelsTextSize(float)` to set the text
    /// size in pixels.
    ///
    /// #### Parameters
    ///
    /// - `font`
    public void setLabelsTextFont(Font font) {
        setLabelsTextSize(font.getHeight());
    }

    /// Returns if the axes should be visible.
    ///
    /// #### Returns
    ///
    /// the visibility flag for the axes
    public boolean isShowAxes() {
        return mShowAxes;
    }

    /// Sets if the axes should be visible.
    ///
    /// #### Parameters
    ///
    /// - `showAxes`: the visibility flag for the axes
    public void setShowAxes(boolean showAxes) {
        mShowAxes = showAxes;
    }

    /// Returns if the labels should be visible.
    ///
    /// #### Returns
    ///
    /// the visibility flag for the labels
    public boolean isShowLabels() {
        return mShowLabels;
    }

    /// Sets if the labels should be visible.
    ///
    /// #### Parameters
    ///
    /// - `showLabels`: the visibility flag for the labels
    public void setShowLabels(boolean showLabels) {
        mShowLabels = showLabels;
    }

    /// Returns if the tick marks should be visible.
    public boolean isShowTickMarks() {
        return mShowTickMarks;
    }

    /// Sets if the tick marks should be visible.
    ///
    /// #### Parameters
    ///
    /// - `showTickMarks`: the visibility flag for the tick marks
    public void setShowTickMarks(boolean mShowTickMarks) {
        this.mShowTickMarks = mShowTickMarks;
    }

    /// Returns if the X axis grid should be visible.
    ///
    /// #### Returns
    ///
    /// the visibility flag for the X axis grid
    public boolean isShowGridX() {
        return mShowGridX;
    }

    /// Sets if the X axis grid should be visible.
    ///
    /// #### Parameters
    ///
    /// - `showGrid`: the visibility flag for the X axis grid
    public void setShowGridX(boolean showGrid) {
        mShowGridX = showGrid;
    }

    /// Returns if the Y axis grid should be visible.
    ///
    /// #### Returns
    ///
    /// the visibility flag for the Y axis grid
    public boolean isShowGridY() {
        return mShowGridY;
    }

    /// Sets if the Y axis grid should be visible.
    ///
    /// #### Parameters
    ///
    /// - `showGrid`: the visibility flag for the Y axis grid
    public void setShowGridY(boolean showGrid) {
        mShowGridY = showGrid;
    }

    /// Sets if the grid should be visible.
    ///
    /// #### Parameters
    ///
    /// - `showGrid`: the visibility flag for the grid
    public void setShowGrid(boolean showGrid) {
        setShowGridX(showGrid);
        setShowGridY(showGrid);
    }

    /// Returns if the X axis custom text grid should be visible.
    ///
    /// #### Returns
    ///
    /// the visibility flag for the X axis custom text grid
    public boolean isShowCustomTextGridX() {
        return mShowCustomTextGridX;
    }

    /// Sets if the X axis custom text grid should be visible.
    ///
    /// #### Parameters
    ///
    /// - `showGrid`: the visibility flag for the X axis custom text grid
    public void setShowCustomTextGridX(boolean showGrid) {
        mShowCustomTextGridX = showGrid;
    }

    /// Returns if the Y axis custom text grid should be visible.
    ///
    /// #### Returns
    ///
    /// the visibility flag for the custom text Y axis grid
    public boolean isShowCustomTextGridY() {
        return mShowCustomTextGridY;
    }

    /// Sets if the Y axis custom text grid should be visible.
    ///
    /// #### Parameters
    ///
    /// - `showGrid`: the visibility flag for the Y axis custom text grid
    public void setShowCustomTextGridY(boolean showGrid) {
        mShowCustomTextGridY = showGrid;
    }

    /// Sets if the grid for custom X or Y labels should be visible.
    ///
    /// #### Parameters
    ///
    /// - `showGrid`: the visibility flag for the custom text grid
    public void setShowCustomTextGrid(boolean showGrid) {
        setShowCustomTextGridX(showGrid);
        setShowCustomTextGridY(showGrid);
    }

    /// Returns if the legend should be visible.
    ///
    /// #### Returns
    ///
    /// the visibility flag for the legend
    public boolean isShowLegend() {
        return mShowLegend;
    }

    /// Sets if the legend should be visible.
    ///
    /// #### Parameters
    ///
    /// - `showLegend`: the visibility flag for the legend
    public void setShowLegend(boolean showLegend) {
        mShowLegend = showLegend;
    }

    /// Returns if the legend should size to fit.
    ///
    /// #### Returns
    ///
    /// the fit behavior
    public boolean isFitLegend() {
        return mFitLegend;
    }

    /// Sets if the legend should size to fit.
    ///
    /// #### Parameters
    ///
    /// - `fit`: the fit behavior
    public void setFitLegend(boolean fit) {
        mFitLegend = fit;
    }

    /// Returns the text typeface name.
    ///
    /// #### Returns
    ///
    /// the text typeface name
    public int getTextTypefaceName() {
        return mTextTypefaceName;
    }

    /// Returns the text typeface style.
    ///
    /// #### Returns
    ///
    /// the text typeface style
    public int getTextTypefaceStyle() {
        return mTextTypefaceStyle;
    }

    /// Returns the text typeface.
    ///
    /// #### Returns
    ///
    /// the text typeface
    public Font getTextTypeface() {
        return mTextTypeface;
    }

    /// Sets the text typeface.
    ///
    /// #### Parameters
    ///
    /// - `typeface`: the typeface
    public void setTextTypeface(Font typeface) {
        mTextTypeface = typeface;
    }

    /// Returns the legend text size.
    ///
    /// #### Returns
    ///
    /// the legend text size
    public float getLegendTextSize() {
        return mLegendTextSize;
    }

    /// Sets the legend text size. Consider using  `#setLegendTextFont(com.codename1.ui.Font)`
    /// instead to allow the font size to to be adjusted appropriately for the display
    /// resolution.
    ///
    /// #### Parameters
    ///
    /// - `textSize`: the legend text size
    public void setLegendTextSize(float textSize) {
        mLegendTextSize = textSize;
    }

    /// Sets the legend text font size using a Font object instead of a point size.
    /// This method is the preferred way to set font size because it allows you to
    /// more easily have fonts appear in an appropriate size for the target device.
    ///
    /// Alternatively check out `#setLegendTextSize(float)` to set the text
    /// size in pixels.
    ///
    /// #### Parameters
    ///
    /// - `font`
    public void setLegendTextFont(Font font) {
        setLegendTextSize(font.getHeight());
    }

    /// Sets the text typeface name and style.
    ///
    /// #### Parameters
    ///
    /// - `typefaceName`: the text typeface name
    ///
    /// - `style`: the text typeface style
    public void setTextTypeface(int typefaceName, int style) {
        mTextTypefaceName = typefaceName;
        mTextTypefaceStyle = style;
    }

    /// Returns the antialiasing flag value.
    ///
    /// #### Returns
    ///
    /// the antialiasing value
    public boolean isAntialiasing() {
        return mAntialiasing;
    }

    /// Sets the antialiasing value.
    ///
    /// #### Parameters
    ///
    /// - `antialiasing`: the antialiasing
    public void setAntialiasing(boolean antialiasing) {
        mAntialiasing = antialiasing;
    }

    /// Returns the value to be used for scaling the chart.
    ///
    /// #### Returns
    ///
    /// the scale value
    public float getScale() {
        return mScale;
    }

    /// Sets the value to be used for scaling the chart. It works on some charts
    /// like pie, doughnut, dial.
    ///
    /// #### Parameters
    ///
    /// - `scale`: the scale value
    public void setScale(float scale) {
        mScale = scale;
    }

    /// Returns the original value to be used for scaling the chart.
    ///
    /// #### Returns
    ///
    /// the original scale value
    public float getOriginalScale() {
        return mOriginalScale;
    }

    /// Returns the enabled state of the zoom.
    ///
    /// #### Returns
    ///
    /// if zoom is enabled
    public boolean isZoomEnabled() {
        return mZoomEnabled;
    }

    /// Sets the enabled state of the zoom.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: zoom enabled
    public void setZoomEnabled(boolean enabled) {
        mZoomEnabled = enabled;
    }

    /// Returns the visible state of the zoom buttons.
    ///
    /// #### Returns
    ///
    /// if zoom buttons are visible
    public boolean isZoomButtonsVisible() {
        return mZoomButtonsVisible;
    }

    /// Sets the visible state of the zoom buttons.
    ///
    /// #### Parameters
    ///
    /// - `visible`: if the zoom buttons are visible
    public void setZoomButtonsVisible(boolean visible) {
        mZoomButtonsVisible = visible;
    }

    /// Returns the enabled state of the external (application implemented) zoom.
    ///
    /// #### Returns
    ///
    /// if external zoom is enabled
    public boolean isExternalZoomEnabled() {
        return mExternalZoomEnabled;
    }

    /// Sets the enabled state of the external (application implemented) zoom.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: external zoom enabled
    public void setExternalZoomEnabled(boolean enabled) {
        mExternalZoomEnabled = enabled;
    }

    /// Returns the zoom rate.
    ///
    /// #### Returns
    ///
    /// the zoom rate
    public float getZoomRate() {
        return mZoomRate;
    }

    /// Sets the zoom rate.
    ///
    /// #### Parameters
    ///
    /// - `rate`: the zoom rate
    public void setZoomRate(float rate) {
        mZoomRate = rate;
    }

    /// Returns the enabled state of the pan.
    ///
    /// #### Returns
    ///
    /// if pan is enabled
    public boolean isPanEnabled() {
        return mPanEnabled;
    }

    /// Sets the enabled state of the pan.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: pan enabled
    public void setPanEnabled(boolean enabled) {
        mPanEnabled = enabled;
    }

    /// Returns the enabled state of the click.
    ///
    /// #### Returns
    ///
    /// if click is enabled
    public boolean isClickEnabled() {
        return mClickEnabled;
    }

    /// Sets the enabled state of the click.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: click enabled
    public void setClickEnabled(boolean enabled) {
        mClickEnabled = enabled;
    }

    /// Returns the selectable radius value around clickable points.
    ///
    /// #### Returns
    ///
    /// the selectable radius
    public int getSelectableBuffer() {
        return selectableBuffer;
    }

    /// Sets the selectable radius value around clickable points.
    ///
    /// #### Parameters
    ///
    /// - `buffer`: the selectable radius
    public void setSelectableBuffer(int buffer) {
        selectableBuffer = buffer;
    }

    /// Returns the legend height.
    ///
    /// #### Returns
    ///
    /// the legend height
    public int getLegendHeight() {
        return mLegendHeight;
    }

    /// Sets the legend height, in pixels.
    ///
    /// #### Parameters
    ///
    /// - `height`: the legend height
    public void setLegendHeight(int height) {
        mLegendHeight = height;
    }

    /// Returns the margin sizes. An array containing the margins in this order:
    /// top, left, bottom, right
    ///
    /// #### Returns
    ///
    /// the margin sizes
    public int[] getMargins() {
        return mMargins;
    }

    /// Sets the margins, in pixels.
    ///
    /// #### Parameters
    ///
    /// - `margins`: @param margins an array containing the margin size values, in this order:
    ///                top, left, bottom, right
    public void setMargins(int[] margins) {
        mMargins = margins;
    }

    /// Returns if the chart is inside a scroll view and doesn't need to shrink.
    ///
    /// #### Returns
    ///
    /// if it is inside a scroll view
    public boolean isInScroll() {
        return mInScroll;
    }

    /// To be set if the chart is inside a scroll view and doesn't need to shrink
    /// when not enough space.
    ///
    /// #### Parameters
    ///
    /// - `inScroll`: if it is inside a scroll view
    public void setInScroll(boolean inScroll) {
        mInScroll = inScroll;
    }

    /// Returns the start angle for circular charts such as pie, doughnut. An angle
    /// of 0 degrees correspond to the geometric angle of 0 degrees (3 o'clock on a
    /// watch.)
    ///
    /// #### Returns
    ///
    /// the start angle in degrees
    public float getStartAngle() {
        return mStartAngle;
    }

    /// Sets the start angle for circular charts such as pie, doughnut, etc. An
    /// angle of 0 degrees correspond to the geometric angle of 0 degrees (3
    /// o'clock on a watch.)
    ///
    /// #### Parameters
    ///
    /// - `startAngle`: the start angle in degrees
    public void setStartAngle(float startAngle) {
        while (startAngle < 0) {
            startAngle += 360;
        }
        mStartAngle = startAngle;
    }

    /// Returns if the values should be displayed as text.
    ///
    /// #### Returns
    ///
    /// if the values should be displayed as text
    public boolean isDisplayValues() {
        return mDisplayValues;
    }

    /// Sets if the values should be displayed as text (supported by pie chart).
    ///
    /// #### Parameters
    ///
    /// - `display`: if the values should be displayed as text
    public void setDisplayValues(boolean display) {
        mDisplayValues = display;
    }

}
