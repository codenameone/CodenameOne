/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package android.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// An adapter over a list of objects, each shown in a TextView by its
/// `toString()` (or as the CharSequence it is).
public class ArrayAdapter<T> extends BaseAdapter implements Filterable {

    private final LayoutInflater mInflater;
    private final Context mContext;
    private final int mResource;
    private int mDropDownResource;
    private int mFieldId;
    private List<T> mObjects;
    private ArrayList<T> mOriginalValues;
    private boolean mNotifyOnChange = true;
    private ArrayFilter mFilter;

    public ArrayAdapter(Context context, int resource) {
        this(context, resource, 0, new ArrayList<T>());
    }

    public ArrayAdapter(Context context, int resource, int textViewResourceId) {
        this(context, resource, textViewResourceId, new ArrayList<T>());
    }

    public ArrayAdapter(Context context, int resource, T[] objects) {
        this(context, resource, 0, new ArrayList<T>(Arrays.asList(objects)));
    }

    public ArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
        this(context, resource, textViewResourceId, new ArrayList<T>(Arrays.asList(objects)));
    }

    public ArrayAdapter(Context context, int resource, List<T> objects) {
        this(context, resource, 0, objects);
    }

    public ArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mResource = resource;
        mDropDownResource = resource;
        mObjects = objects;
        mFieldId = textViewResourceId;
    }

    /// An adapter over the string array resource `textArrayResId`.
    public static ArrayAdapter<CharSequence> createFromResource(Context context, int textArrayResId,
                                                                int textViewResId) {
        CharSequence[] strings = context.getResources().getTextArray(textArrayResId);
        return new ArrayAdapter<CharSequence>(context, textViewResId, 0, new ArrayList<CharSequence>(Arrays.asList(strings)));
    }

    private List<T> target() {
        return mOriginalValues != null ? mOriginalValues : mObjects;
    }

    public void add(T object) {
        target().add(object);
        if (mOriginalValues != null) {
            mObjects.add(object);
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void addAll(Collection<? extends T> collection) {
        target().addAll(collection);
        if (mOriginalValues != null) {
            mObjects.addAll(collection);
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void addAll(T... items) {
        addAll(Arrays.asList(items));
    }

    public void insert(T object, int index) {
        target().add(index, object);
        if (mOriginalValues != null) {
            mObjects.add(Math.min(index, mObjects.size()), object);
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void remove(T object) {
        target().remove(object);
        if (mOriginalValues != null) {
            mObjects.remove(object);
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void clear() {
        target().clear();
        if (mOriginalValues != null) {
            mObjects.clear();
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void sort(Comparator<? super T> comparator) {
        Collections.sort(target(), comparator);
        if (mOriginalValues != null) {
            Collections.sort(mObjects, comparator);
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mNotifyOnChange = true;
    }

    public void setNotifyOnChange(boolean notifyOnChange) {
        mNotifyOnChange = notifyOnChange;
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public int getCount() {
        return mObjects.size();
    }

    @Override
    public T getItem(int position) {
        return mObjects.get(position);
    }

    public int getPosition(T item) {
        return mObjects.indexOf(item);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(mInflater, position, convertView, parent, mResource);
    }

    private View createViewFromResource(LayoutInflater inflater, int position, View convertView, ViewGroup parent,
                                        int resource) {
        final View view = convertView == null ? inflater.inflate(resource, parent, false) : convertView;
        final TextView text;
        if (mFieldId == 0) {
            if (!(view instanceof TextView)) {
                throw new IllegalStateException("ArrayAdapter requires the resource ID to be a TextView");
            }
            text = (TextView) view;
        } else {
            View found = view.findViewById(mFieldId);
            if (!(found instanceof TextView)) {
                throw new IllegalStateException("Failed to find a TextView with id "
                        + mContext.getResources().getResourceEntryName(mFieldId) + " in item layout");
            }
            text = (TextView) found;
        }
        final T item = getItem(position);
        if (item instanceof CharSequence) {
            text.setText((CharSequence) item);
        } else {
            text.setText(String.valueOf(item));
        }
        return view;
    }

    public void setDropDownViewResource(int resource) {
        mDropDownResource = resource;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(mInflater, position, convertView, parent, mDropDownResource);
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    /// Keeps the items whose text, or any word of it, starts with the
    /// constraint, ignoring case.
    private final class ArrayFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
            if (mOriginalValues == null) {
                mOriginalValues = new ArrayList<T>(mObjects);
            }
            ArrayList<T> values = new ArrayList<T>(mOriginalValues);
            if (prefix == null || prefix.length() == 0) {
                results.values = values;
                results.count = values.size();
                return results;
            }
            String p = prefix.toString();
            ArrayList<T> kept = new ArrayList<T>();
            for (T v : values) {
                String text = String.valueOf(v);
                if (text.regionMatches(true, 0, p, 0, p.length())) {
                    kept.add(v);
                    continue;
                }
                for (int i = 0; i < text.length(); i++) {
                    if (text.charAt(i) == ' ' && text.regionMatches(true, i + 1, p, 0, p.length())) {
                        kept.add(v);
                        break;
                    }
                }
            }
            results.values = kept;
            results.count = kept.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results.values instanceof List) {
                mObjects = (List<T>) results.values;
            }
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
