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
package com.example.droid;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RecyclerActivity extends Activity {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    static final class Item {
        final int id;
        boolean starred;

        Item(int id) {
            this.id = id;
        }
    }

    private final List<Item> items = new ArrayList<Item>();
    private RecyclerView list;
    private TextView status;
    private boolean grid;
    private int created;
    private DividerItemDecoration divider;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_recycler);
        for (int i = 0; i < 200; i++) {
            items.add(new Item(i + 1));
        }
        status = findViewById(R.id.rv_status);
        list = findViewById(R.id.rv_list);
        divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        list.addItemDecoration(divider);
        final Adapter adapter = new Adapter();
        list.setAdapter(adapter);
        findViewById(R.id.rv_grid).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                grid = !grid;
                if (grid) {
                    GridLayoutManager glm = new GridLayoutManager(RecyclerActivity.this, 3);
                    glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                        @Override
                        public int getSpanSize(int position) {
                            return position == 0 ? 3 : 1;
                        }
                    });
                    list.removeItemDecoration(divider);
                    list.setLayoutManager(glm);
                } else {
                    list.addItemDecoration(divider);
                    list.setLayoutManager(new LinearLayoutManager(RecyclerActivity.this));
                }
                status.setText(grid ? "Grid of 3" : "List");
            }
        });
        findViewById(R.id.rv_shuffle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<Item> old = new ArrayList<Item>(items);
                Collections.shuffle(items, new Random(7));
                DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return old.size() + 1;
                    }

                    @Override
                    public int getNewListSize() {
                        return items.size() + 1;
                    }

                    @Override
                    public boolean areItemsTheSame(int o, int n) {
                        if (o == 0 || n == 0) {
                            return o == n;
                        }
                        return old.get(o - 1).id == items.get(n - 1).id;
                    }

                    @Override
                    public boolean areContentsTheSame(int o, int n) {
                        return o == 0 || old.get(o - 1).starred == items.get(n - 1).starred;
                    }
                }).dispatchUpdatesTo(adapter);
                status.setText("Shuffled");
            }
        });
        findViewById(R.id.rv_top).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.smoothScrollToPosition(0);
            }
        });
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && rv.getLayoutManager() instanceof LinearLayoutManager) {
                    LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                    status.setText("First visible " + lm.findFirstVisibleItemPosition() + ", views created "
                            + created);
                }
            }
        });
    }

    final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_HEADER : TYPE_ITEM;
        }

        @Override
        public int getItemCount() {
            return items.size() + 1;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            created++;
            if (viewType == TYPE_HEADER) {
                TextView header = new TextView(parent.getContext());
                header.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                header.setPadding(32, 32, 32, 32);
                header.setTextSize(20);
                header.setBackgroundColor(0xFFFFE3EC);
                header.setTextColor(Color.BLACK);
                return new RecyclerView.ViewHolder(header) {
                };
            }
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_recycler, parent, false);
            return new RowHolder(row);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == TYPE_HEADER) {
                ((TextView) holder.itemView).setText(items.size() + " items");
                return;
            }
            Item item = items.get(position - 1);
            RowHolder row = (RowHolder) holder;
            row.title.setText("Item " + item.id);
            row.star.setAlpha(item.starred ? 1f : 0.2f);
        }
    }

    final class RowHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final ImageView star;

        RowHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.rv_title);
            star = itemView.findViewById(R.id.rv_star);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) {
                        return;
                    }
                    Item item = items.get(pos - 1);
                    item.starred = !item.starred;
                    list.getAdapter().notifyItemChanged(pos);
                    status.setText((item.starred ? "Starred " : "Unstarred ") + "item " + item.id);
                }
            });
            itemView.findViewById(R.id.rv_remove).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) {
                        return;
                    }
                    Item item = items.remove(pos - 1);
                    list.getAdapter().notifyItemRemoved(pos);
                    list.getAdapter().notifyItemChanged(0);
                    status.setText("Removed item " + item.id);
                }
            });
        }
    }
}
