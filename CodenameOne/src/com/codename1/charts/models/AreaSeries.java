/**
 * Copyright (C) 2019 dj6082013
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
package com.codename1.charts.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A series for the category charts like the radar ones.
 */
public class AreaSeries {

    /**
     * The series data table.
     */
    private Map<String, HashMap<Integer, Double>> mTable = new HashMap<String, HashMap<Integer, Double>>();

    /**
     * The categories.
     */
    private List<String> mCategories = new ArrayList<String>();
    /**
     * The series.
     */
    private List<String> mSeries = new ArrayList<String>();

    /**
     * Adds a new Category series to the table.
     *
     * @param series the Category series to add
     */
    public synchronized void addSeries(CategorySeries series) {
        int length = series.getItemCount();
        for (int i = 0; i < length; i++) {
            String category = series.getCategory(i);
            HashMap<Integer, Double> col;
            if (!mTable.containsKey(category)) {
                mCategories.add(category);
                col = new HashMap<Integer, Double>();
            } else {
                col = mTable.get(category);
            }
            col.put(mSeries.size(), series.getValue(i));
            mTable.put(category, col);
        }
        mSeries.add(series.getTitle());
    }

    /**
     * Removes all data.
     */
    public synchronized void clear() {
        mTable.clear();
        mCategories.clear();
        mSeries.clear();
    }

    /**
     * Returns the Categories count.
     *
     * @return the Categories count
     */
    public synchronized int getCategoriesCount() {
        return mCategories.size();
    }

    /**
     * Returns the Series count.
     *
     * @return the Series series count
     */
    public synchronized int getSeriesCount() {
        return mSeries.size();
    }

    /**
     * Returns an array of the Categories.
     *
     * @return the Categories array
     */
    public synchronized String[] getCategories() {
        return mCategories.toArray(new String[mCategories.size()]);
    }

    /**
     * Returns an array of the Series.
     *
     * @return the Series array
     */
    public synchronized String[] getSeries() {
        return mSeries.toArray(new String[mSeries.size()]);
    }

    /**
     * Returns the value at the specific index and category.
     *
     * @param index the index
     * @param category the category
     * @return the value
     */
    public synchronized double getValue(int index, String category) {
        if (!mTable.containsKey(category)) {
            return 0;
        }
        if (!mTable.get(category).containsKey(index)) {
            return 0;
        }
        return mTable.get(category).get(index);
    }
}
