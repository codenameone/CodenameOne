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
package androidx.recyclerview.widget;
import android.view.View;
import android.view.ViewGroup;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class GridRowSizeTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    @Test public void verticalRowsEqualizeWrapContentItems() { check(GridLayoutManager.VERTICAL); }
    @Test public void horizontalRowsEqualizeWrapContentItems() { check(GridLayoutManager.HORIZONTAL); }
    private void check(final int orientation) {
        RecyclerView grid = new RecyclerView(AndroidTestSupport.context());
        GridLayoutManager manager = new GridLayoutManager(grid.getContext(), 2, orientation, false);
        grid.setLayoutManager(manager);
        grid.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
                View view = new View(parent.getContext());
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                        orientation == GridLayoutManager.VERTICAL ? -1 : -2,
                        orientation == GridLayoutManager.VERTICAL ? -2 : -1);
                lp.setMargins(2, 2, 3, 3);
                view.setLayoutParams(lp);
                return new RecyclerView.ViewHolder(view) { };
            }
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                holder.itemView.setMinimumWidth(position % 2 == 0 ? 20 : 50);
                holder.itemView.setMinimumHeight(position % 2 == 0 ? 20 : 50);
            }
            public int getItemCount() { return 4; }
        });
        int spec = View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY);
        grid.measure(spec, spec);
        grid.layout(0, 0, 300, 300);
        View first = manager.findViewByPosition(0);
        View second = manager.findViewByPosition(1);
        View next = manager.findViewByPosition(2);
        assertNotNull(first);
        assertNotNull(second);
        assertNotNull(next);
        if (orientation == GridLayoutManager.VERTICAL) {
            assertEquals(second.getMeasuredHeight(), first.getMeasuredHeight());
            assertEquals(second.getBottom(), first.getBottom());
            assertEquals(first.getBottom() + 5, next.getTop());
        } else {
            assertEquals(second.getMeasuredWidth(), first.getMeasuredWidth());
            assertEquals(second.getRight(), first.getRight());
            assertEquals(first.getRight() + 5, next.getLeft());
        }
    }
}
