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

import com.codename1.components.InfiniteProgress;
import com.codename1.components.InfiniteScrollAdapter;
import com.codename1.ui.layouts.BoxLayout;

/**
 * <p>This abstract Container can scroll indefinitely (or at least until
 * we run out of data).
 * This class uses the {@link com.codename1.components.InfiniteScrollAdapter} to bring more data and the pull to 
 * refresh feature to refresh current displayed data.</p>
 * <p>
 * The sample code shows the usage of the nestoria API to fill out an infinitely scrolling list.
 * </p>
 * <script src="https://gist.github.com/codenameone/9e2f7984beb22d9e372c.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-infinitescrolladapter.png" alt="Sample usage of infinite scroll adapter" />
 * <script src="https://gist.github.com/codenameone/22efe9e04e2b8986dfc3.js"></script>
 * 
 * @author Chen
 */
public abstract class InfiniteContainer extends Container {

    private int amount = 10;
    private boolean amountSet;

    private boolean requestingResults;

    private InfiniteScrollAdapter adapter;
    
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
        amountSet = true;
    }

    @Override
    void resetScroll() {
    }

    boolean shouldContinue(Component[] cmps) {
        if(amountSet) {
            return cmps.length == amount;
        } else {
            return cmps != null && cmps.length > 0;
        }
    }
    
    @Override
    protected void initComponent() {
        super.initComponent();
        createInfiniteScroll();
        addPullToRefresh(new Runnable() {

            public void run() {
                refresh();
            }
        });
    }

    /**
     * This refreshes the UI in a similar way to the "pull to refresh" functionality
     */
    public void refresh() {
        if(isAsync()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    refreshImpl();
                }
            });        
            return;
        }
        refreshImpl();
    }
    
    void refreshImpl() {
        requestingResults = true;
        Component[] components = fetchComponents(0, amount);
        if (components == null) {
            components = new Component[0];
        }
        final Component[] cmps = components;
        if(!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {

                public void run() {
                    removeAll();
                    InfiniteScrollAdapter.addMoreComponents(InfiniteContainer.this, cmps, shouldContinue(cmps));
                    requestingResults = false;
                }
            });
        } else {
            removeAll();
            InfiniteScrollAdapter.addMoreComponents(InfiniteContainer.this, cmps, shouldContinue(cmps));
            requestingResults = false;
        }
    }

    void fetchMore() {
        if (requestingResults) {
            return;
        }
        requestingResults = true;
        Component[] components = fetchComponents(getComponentCount() - 1, amount);
        if (components == null) {
            components = new Component[0];
        }
        final Component[] cmps = components;
        if(!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    InfiniteScrollAdapter.addMoreComponents(InfiniteContainer.this, cmps, shouldContinue(cmps));
                    requestingResults = false;
                }
            });
        } else {
            InfiniteScrollAdapter.addMoreComponents(InfiniteContainer.this, cmps, shouldContinue(cmps));
            requestingResults = false;
        }
    }
    
    private void createInfiniteScroll() {
        adapter = InfiniteScrollAdapter.createInfiniteScroll(this, new Runnable() {

            public void run() {
                if(isAsync()) {
                    Display.getInstance().scheduleBackgroundTask(new Runnable() {
                        public void run() {
                            fetchMore();
                        }
                    });
                } else {
                    fetchMore();
                }
            }
        });

    }

    /**
     * Indicates whether {@link #fetchComponents(int, int)} should be invoked asynchronously off the EDT
     * @return this is set to true for compatibility with older versions of the infinite container
     */
    protected boolean isAsync() {
        return true;
    }
    
    /**
     * <p>This is an abstract method that should be implemented by the sub classes
     * to fetch the data.</p>
     * <p><b>When {@link #isAsync()} is overriden to return true this method is invoked on a background thread</b>.
     * Notice that in this case the method might cause EDT violations warnings, since the 
     * subclasses will need to create the Components off the EDT. While these are EDT violations they
     * probably won't cause problems for more code.</p>
     * <p>Sub classes should preform their networking/data fetching here.</p>
     * 
     * 
     * @param index the index from which to bring data
     * @param amount the size of components to bring
     * 
     * @return Components array of the returned data, size of the array can be the 
     * size of the amount or smaller, if no data to fetch method can return null.
     */ 
    public abstract Component[] fetchComponents(int index, int amount);


    /**
     * Lets us manipulate the infinite progress object e.g. set the animation image etc.
     * @return the infinite progress component underlying this container
     */
    public InfiniteProgress getInfiniteProgress() {
        return adapter.getInfiniteProgress();
    }
}