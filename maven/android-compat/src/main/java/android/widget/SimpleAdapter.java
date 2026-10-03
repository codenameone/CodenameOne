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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.Map;

/// Binds a list of maps to item views: the value under `from[i]` goes into
/// the view with id `to[i]` (text into TextViews, images into ImageViews).
public class SimpleAdapter extends BaseAdapter implements Filterable {

    public interface ViewBinder {
        boolean setViewValue(View view, Object data, String textRepresentation);
    }

    private final LayoutInflater mInflater;
    private final List<? extends Map<String, ?>> mData;
    private final int mResource;
    private int mDropDownResource;
    private final String[] mFrom;
    private final int[] mTo;
    private ViewBinder mViewBinder;

    public SimpleAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
        mData = data;
        mResource = resource;
        mDropDownResource = resource;
        mFrom = from;
        mTo = to;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mDropDownResource);
    }

    public void setDropDownViewResource(int resource) {
        mDropDownResource = resource;
    }

    private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
        View v = convertView == null ? mInflater.inflate(resource, parent, false) : convertView;
        bindView(position, v);
        return v;
    }

    private void bindView(int position, View view) {
        Map<String, ?> dataSet = mData.get(position);
        if (dataSet == null) {
            return;
        }
        for (int i = 0; i < mTo.length; i++) {
            View v = view.findViewById(mTo[i]);
            if (v == null) {
                continue;
            }
            Object data = dataSet.get(mFrom[i]);
            String text = data == null ? "" : data.toString();
            if (mViewBinder != null && mViewBinder.setViewValue(v, data, text)) {
                continue;
            }
            if (v instanceof TextView) {
                ((TextView) v).setText(text);
            } else if (v instanceof ImageView) {
                ImageView iv = (ImageView) v;
                if (data instanceof Integer) {
                    iv.setImageResource(((Integer) data).intValue());
                } else if (data instanceof Bitmap) {
                    iv.setImageBitmap((Bitmap) data);
                } else if (data instanceof Drawable) {
                    iv.setImageDrawable((Drawable) data);
                } else {
                    setViewImage(iv, text);
                }
            } else {
                throw new IllegalStateException(v.getClass().getName()
                        + " is not a view that can be bounds by this SimpleAdapter");
            }
        }
    }

    public void setViewImage(ImageView v, int value) {
        v.setImageResource(value);
    }

    public void setViewImage(ImageView v, String value) {
        try {
            v.setImageResource(Integer.parseInt(value));
        } catch (NumberFormatException nfe) {
            v.setImageURI(Uri.parse(value));
        }
    }

    public void setViewText(TextView v, String text) {
        v.setText(text);
    }

    public ViewBinder getViewBinder() {
        return mViewBinder;
    }

    public void setViewBinder(ViewBinder viewBinder) {
        mViewBinder = viewBinder;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults r = new FilterResults();
                r.count = mData.size();
                r.values = mData;
                return r;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        };
    }
}
