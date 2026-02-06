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
package com.codename1.charts.models;

import com.codename1.charts.util.MathHelper;

import java.util.ArrayList;
import java.util.List;

/// An extension of the XY series which adds a third dimension. It is used for XY
/// charts like bubble.
public class XYValueSeries extends XYSeries {
    /// A list to contain the series values.
    private final List<Double> mValue = new ArrayList<Double>();
    /// The minimum value.
    private double mMinValue = MathHelper.NULL_VALUE;
    /// The maximum value.
    private double mMaxValue = MathHelper.NULL_VALUE;

    /// Builds a new XY value series.
    ///
    /// #### Parameters
    ///
    /// - `title`: the series title.
    public XYValueSeries(String title) {
        super(title);
    }

    /// Adds a new value to the series.
    ///
    /// #### Parameters
    ///
    /// - `x`: the value for the X axis
    ///
    /// - `y`: the value for the Y axis
    ///
    /// - `value`: the value
    public void add(double x, double y, double value) {
        super.add(x, y);
        mValue.add(value);
        updateRange(value);
    }

    /// Initializes the values range.
    private void initRange() {
        mMinValue = MathHelper.NULL_VALUE;
        mMaxValue = MathHelper.NULL_VALUE;
        int length = getItemCount();
        for (int k = 0; k < length; k++) {
            updateRange(getValue(k));
        }
    }

    /// Updates the values range.
    ///
    /// #### Parameters
    ///
    /// - `value`: the new value
    private void updateRange(double value) {
        mMinValue = mMinValue == MathHelper.NULL_VALUE ? value : Math.min(mMinValue, value);
        mMaxValue = mMaxValue == MathHelper.NULL_VALUE ? value : Math.max(mMaxValue, value);
    }

    /// Adds a new value to the series.
    ///
    /// #### Parameters
    ///
    /// - `x`: the value for the X axis
    ///
    /// - `y`: the value for the Y axis
    @Override
    public void add(double x, double y) {
        add(x, y, 0d);
    }

    /// Removes an existing value from the series.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index in the series of the value to remove
    @Override
    public void remove(int index) {
        super.remove(index);
        double removedValue = mValue.remove(index);
        if (com.codename1.util.MathUtil.compare(removedValue, mMinValue) == 0 || com.codename1.util.MathUtil.compare(removedValue, mMaxValue) == 0) {
            initRange();
        }
    }

    /// Removes all the values from the series.
    @Override
    public void clear() {
        super.clear();
        mValue.clear();
        initRange();
    }

    /// Returns the value at the specified index.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index
    ///
    /// #### Returns
    ///
    /// the value
    public double getValue(int index) {
        return mValue.get(index);
    }

    /// Returns the minimum value.
    ///
    /// #### Returns
    ///
    /// the minimum value
    public double getMinValue() {
        return mMinValue;
    }

    /// Returns the maximum value.
    ///
    /// #### Returns
    ///
    /// the maximum value
    public double getMaxValue() {
        return mMaxValue;
    }

}
