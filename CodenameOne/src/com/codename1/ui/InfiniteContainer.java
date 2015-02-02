/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import com.codename1.components.InfiniteScrollAdapter;
import com.codename1.ui.layouts.BoxLayout;

/**
 * This abstract Container can scroll indefinitely (or at least until
 * we run out of data).
 * This class uses the InfiniteScrollAdapter to bring more data and the pull to 
 * refresh feature to refresh current displayed data.
 * 
 * @author Chen
 */
public abstract class InfiniteContainer extends Container {

    private int amount = 10;

    private boolean requestingResults;

    /**
     * Creates the InfiniteContainer.
     * The InfiniteContainer is created with BoxLayout Y layout.
     */ 
    public InfiniteContainer() {
        setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        setScrollableY(true);
    }

    /**
     * Creates the InfiniteContainer.
     * The InfiniteContainer is created with BoxLayout Y layout.
     * 
     * @param amount the number of items to fetch in each call to fetchComponents
     */ 
    public InfiniteContainer(int amount) {
        this();
        this.amount = amount;
        if(amount <= 0){
            throw new IllegalArgumentException("amount must be greater then zero");
        }
    }

    @Override
    void resetScroll() {
    }

    
    @Override
    protected void initComponent() {
        super.initComponent();
        createInfiniteScroll();
        addPullToRefresh(new Runnable() {

            public void run() {

                Display.getInstance().invokeAndBlock(new Runnable() {

                    public void run() {
                        requestingResults = true;
                        Component[] components = fetchComponents(0, amount);
                        if (components == null) {
                            components = new Component[0];
                        }
                        final Component[] cmps = components;
                        Display.getInstance().callSerially(new Runnable() {

                            public void run() {
                                removeAll();
                                InfiniteScrollAdapter.addMoreComponents(InfiniteContainer.this, cmps, cmps.length == amount);
                                requestingResults = false;
                            }
                        });
                    }
                });

            }
        });
    }

    private void createInfiniteScroll() {
        InfiniteScrollAdapter.createInfiniteScroll(this, new Runnable() {

            public void run() {
                Display.getInstance().scheduleBackgroundTask(new Runnable() {

                    public void run() {
                        if (requestingResults) {
                            return;
                        }
                        requestingResults = true;
                        Component[] components = fetchComponents(getComponentCount() - 1, amount);
                        if (components == null) {
                            components = new Component[0];
                        }
                        final Component[] cmps = components;
                        Display.getInstance().callSerially(new Runnable() {

                            public void run() {
                                InfiniteScrollAdapter.addMoreComponents(InfiniteContainer.this, cmps, cmps.length == amount);
                                requestingResults = false;
                            }
                        });

                    }
                });

            }
        });

    }

    /**
     * This is an abstract method that should be implemented by the sub classes
     * to fetch the data.
     * This method is invoked on a background thread, sub classes should 
     * preform their networking/data fetching here.
     * 
     * Notice - this method might cause EDT violations warnings, since the 
     * subclasses will need to create the Components not on the EDT, 
     * these warnings are legit and can be ignored.
     * 
     * @param index the index from which to bring data
     * @param amount the size of components to bring
     * 
     * @return Components array of the returned data, size of the array can be the 
     * size of the amount or smaller, if no data to fetch method can return null.
     */ 
    public abstract Component[] fetchComponents(int index, int amount);
}