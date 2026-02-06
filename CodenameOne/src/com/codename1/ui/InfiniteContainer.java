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

/// This abstract Container can scroll indefinitely (or at least until
/// we run out of data).
/// This class uses the `com.codename1.components.InfiniteScrollAdapter` to bring more data and the pull to
/// refresh feature to refresh current displayed data.
///
/// The sample code shows the usage of the nestoria API to fill out an infinitely scrolling list.
///
/// ```java
/// public void showForm() {
///     Form hi = new Form("InfiniteContainer", new BorderLayout());
///
///     Style s = UIManager.getInstance().getComponentStyle("MultiLine1");
///     FontImage p = FontImage.createMaterial(FontImage.MATERIAL_PORTRAIT, s);
///     EncodedImage placeholder = EncodedImage.createFromImage(p.scaled(p.getWidth() * 3, p.getHeight() * 3), false);
///
///     InfiniteContainer ic = new InfiniteContainer() {
/// @Override
///         public Component[] fetchComponents(int index, int amount) {
///             java.util.List> data = fetchPropertyData("Leeds");
///             MultiButton[] cmps = new MultiButton[data.size()];
///             for(int iter = 0 ; iter  currentListing = data.get(iter);
///                 if(currentListing == null) {
///                     return null;
///                 }
///                 String thumb_url = (String)currentListing.get("thumb_url");
///                 String guid = (String)currentListing.get("guid");
///                 String summary = (String)currentListing.get("summary");
///                 cmps[iter] = new MultiButton(summary);
///                 cmps[iter].setIcon(URLImage.createToStorage(placeholder, guid, thumb_url));
///             }
///             return cmps;
///         }
///     };
///     hi.add(BorderLayout.CENTER, ic);
///     hi.show();
/// }
/// int pageNumber = 1;
/// java.util.List> fetchPropertyData(String text) {
///     try {
///         ConnectionRequest r = new ConnectionRequest();
///         r.setPost(false);
///         r.setUrl("http://api.nestoria.co.uk/api");
///         r.addArgument("pretty", "0");
///         r.addArgument("action", "search_listings");
///         r.addArgument("encoding", "json");
///         r.addArgument("listing_type", "buy");
///         r.addArgument("page", "" + pageNumber);
///         pageNumber++;
///         r.addArgument("country", "uk");
///         r.addArgument("place_name", text);
///         NetworkManager.getInstance().addToQueueAndWait(r);
///         Map result = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
///         Map response = (Map)result.get("response");
///         return (java.util.List>)response.get("listings");
///     } catch(Exception err) {
///         Log.e(err);
///         return null;
///     }
/// }
/// ```
///
/// ```java
/// int pageNumber = 1;
/// java.util.List> fetchPropertyData(String text) {
///     try {
///         ConnectionRequest r = new ConnectionRequest();
///         r.setPost(false);
///         r.setUrl("http://api.nestoria.co.uk/api");
///         r.addArgument("pretty", "0");
///         r.addArgument("action", "search_listings");
///         r.addArgument("encoding", "json");
///         r.addArgument("listing_type", "buy");
///         r.addArgument("page", "" + pageNumber);
///         pageNumber++;
///         r.addArgument("country", "uk");
///         r.addArgument("place_name", text);
///         NetworkManager.getInstance().addToQueueAndWait(r);
///         Map result = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
///         Map response = (Map)result.get("response");
///         return (java.util.List>)response.get("listings");
///     } catch(Exception err) {
///         Log.e(err);
///         return null;
///     }
/// }
/// ```
/// @author Chen
public abstract class InfiniteContainer extends Container {

    private int amount = 10;
    private boolean amountSet;

    private boolean requestingResults;

    private InfiniteScrollAdapter adapter;
    private boolean initialized;

    /// Creates the InfiniteContainer.
    /// The InfiniteContainer is created with BoxLayout Y layout.
    public InfiniteContainer() {
        setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        setScrollableY(true);
    }

    /// Creates the InfiniteContainer.
    /// The InfiniteContainer is created with BoxLayout Y layout.
    ///
    /// #### Parameters
    ///
    /// - `amount`: the number of items to fetch in each call to fetchComponents
    public InfiniteContainer(int amount) {
        this();
        this.amount = amount;
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater then zero");
        }
        amountSet = true;
    }

    @Override
    void resetScroll() {
    }

    boolean shouldContinue(Component[] cmps) {
        if (amountSet) {
            return cmps.length == amount;
        } else {
            return cmps != null && cmps.length > 0;
        }
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (initialized) {
            return;
        }
        initialized = true;
        createInfiniteScroll();
        addPullToRefresh(new Runnable() {

            @Override
            public void run() {
                refresh();
            }
        });
    }

    /// This refreshes the UI in a similar way to the "pull to refresh" functionality
    public void refresh() {
        // prevent exception when refresh() is invoked too soon
        if (!isInitialized()) {
            if (getClientProperty("cn1$infinite") == null) {
                return;
            }
        }
        if (isAsync()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    refreshImpl();
                }
            });
            return;
        }
        refreshImpl();
    }

    void refreshImpl() {
        if (requestingResults) {
            return;
        }
        requestingResults = true;
        Component[] components;
        try {
            components = fetchComponents(0, amount);
        } catch (RuntimeException err) {
            requestingResults = false;
            throw err;
        }
        if (components == null) {
            components = new Component[0];
        }
        final Component[] cmps = components;
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {

                @Override
                public void run() {
                    removeAll();
                    InfiniteScrollAdapter.addMoreComponents(InfiniteContainer.this, cmps, shouldContinue(cmps));
                    requestingResults = false;
                }
            });
        } else {
            removeAll();
            InfiniteScrollAdapter.addMoreComponents(this, cmps, shouldContinue(cmps));
            requestingResults = false;
        }
    }

    /// If we previously added returned null when fetching components this
    /// method can continue the process of fetching. This is useful in case of
    /// a networking error. You can end fetching and then restart it based on
    /// user interaction see https://github.com/codenameone/CodenameOne/issues/2721
    public void continueFetching() {
        InfiniteScrollAdapter.continueFetching(this);
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
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    InfiniteScrollAdapter.addMoreComponents(InfiniteContainer.this, cmps, shouldContinue(cmps));
                    requestingResults = false;
                }
            });
        } else {
            InfiniteScrollAdapter.addMoreComponents(this, cmps, shouldContinue(cmps));
            requestingResults = false;
        }
    }

    private void createInfiniteScroll() {
        adapter = InfiniteScrollAdapter.createInfiniteScroll(this, new Runnable() {

            @Override
            public void run() {
                if (isAsync()) {
                    Display.getInstance().scheduleBackgroundTask(new Runnable() {
                        @Override
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

    /// Indicates whether `int)` should be invoked asynchronously off the EDT
    ///
    /// #### Returns
    ///
    /// this is set to true for compatibility with older versions of the infinite container
    protected boolean isAsync() {
        return false;
    }

    /// This is an abstract method that should be implemented by the sub classes
    /// to fetch the data.
    ///
    /// **When `#isAsync()` is overriden to return true this method is invoked on a background thread**.
    /// Notice that in this case the method might cause EDT violations warnings, since the
    /// subclasses will need to create the Components off the EDT. While these are EDT violations they
    /// probably won't cause problems for more code.
    ///
    /// Sub classes should preform their networking/data fetching here.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index from which to bring data
    ///
    /// - `amount`: the size of components to bring
    ///
    /// #### Returns
    ///
    /// @return Components array of the returned data, size of the array can be the
    /// size of the amount or smaller, if there is no more data to fetch the method should return null.
    public abstract Component[] fetchComponents(int index, int amount);


    /// Lets us manipulate the infinite progress object e.g. set the animation image etc.
    ///
    /// #### Returns
    ///
    /// the infinite progress component underlying this container
    public InfiniteProgress getInfiniteProgress() {
        return adapter.getInfiniteProgress();
    }
}
