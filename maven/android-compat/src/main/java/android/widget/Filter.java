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

/// Filters a data set by a constraint. Filtering runs synchronously on the
/// calling thread and publishes on the event dispatch thread, which keeps
/// the adapter single-threaded.
public abstract class Filter {

    public interface FilterListener {
        void onFilterComplete(int count);
    }

    protected static class FilterResults {
        public Object values;
        public int count;

        public FilterResults() {
        }
    }

    public Filter() {
    }

    public final void filter(CharSequence constraint) {
        filter(constraint, null);
    }

    public final void filter(final CharSequence constraint, final FilterListener listener) {
        final FilterResults results = performFiltering(constraint);
        Runnable publish = new Runnable() {
            @Override
            public void run() {
                publishResults(constraint, results);
                if (listener != null) {
                    listener.onFilterComplete(results == null ? -1 : results.count);
                }
            }
        };
        if (com.codename1.ui.Display.getInstance().isEdt()) {
            publish.run();
        } else {
            com.codename1.ui.CN.callSerially(publish);
        }
    }

    protected abstract FilterResults performFiltering(CharSequence constraint);

    protected abstract void publishResults(CharSequence constraint, FilterResults results);

    public CharSequence convertResultToString(Object resultValue) {
        return resultValue == null ? "" : resultValue.toString();
    }
}
