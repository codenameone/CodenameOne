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

package com.codename1.components;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.FlowLayout;

/// Allows adapting a scroll container to scroll indefinitely (or at least until
/// running out of data), this effectively works by showing an infinite progress
/// indicator when reaching scroll end then allowing code to fetch additional components.
///
/// **Warning:** If you call `com.codename1.ui.Container#removeAll()`  on the container to which an InfiniteScrollAdapter is
/// installed, it will disable the infinite scrolling behavior.  You can re-enable infinite scrolling by calling `com.codename1.ui.Component[], boolean)`
/// again.
///
/// The sample code shows the usage of the nestoria API to fill out an infinitely scrolling list.
///
/// ```java
/// public void showForm() {
///     Form hi = new Form("InfiniteScrollAdapter", new BoxLayout(BoxLayout.Y_AXIS));
///
///     Style s = UIManager.getInstance().getComponentStyle("MultiLine1");
///     FontImage p = FontImage.createMaterial(FontImage.MATERIAL_PORTRAIT, s);
///     EncodedImage placeholder = EncodedImage.createFromImage(p.scaled(p.getWidth() * 3, p.getHeight() * 3), false);
///
///     InfiniteScrollAdapter.createInfiniteScroll(hi.getContentPane(), () -> {
///         java.util.List> data = fetchPropertyData("Leeds");
///         MultiButton[] cmps = new MultiButton[data.size()];
///         for(int iter = 0 ; iter  currentListing = data.get(iter);
///             if(currentListing == null) {
///                 InfiniteScrollAdapter.addMoreComponents(hi.getContentPane(), new Component[0], false);
///                 return;
///             }
///             String thumb_url = (String)currentListing.get("thumb_url");
///             String guid = (String)currentListing.get("guid");
///             String summary = (String)currentListing.get("summary");
///             cmps[iter] = new MultiButton(summary);
///             cmps[iter].setIcon(URLImage.createToStorage(placeholder, guid, thumb_url));
///         }
///         InfiniteScrollAdapter.addMoreComponents(hi.getContentPane(), cmps, true);
///     }, true);
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
/// @author Shai Almog
public final class InfiniteScrollAdapter {
    private final Component ip;
    private final InfiniteProgress progress;
    private final Component endMarker = new EdgeMarker();
    private Container infiniteContainer;
    private Runnable fetchMore;
    private int componentLimit = -1;

    private InfiniteScrollAdapter() {
        progress = new InfiniteProgress();
        Container p = new Container(new FlowLayout(Component.CENTER));
        p.addComponent(progress);
        ip = p;
    }

    /// Creates an instance of the InfiniteScrollAdapter that will invoke the fetch more
    /// callback to fetch additional components, once that method completes its task it
    /// should add the components via the addMoreComponents() invocation.
    /// Notice that the container MUST be empty when invoking this method, fetchMore
    /// will be invoked immediately and you can add your data when ready.
    ///
    /// #### Parameters
    ///
    /// - `cont`: the container to bind, it MUST be empty and must be scrollable on the Y axis
    ///
    /// - `fetchMore`: a callback that will be invoked on the EDT to fetch more data (do not block this method)
    ///
    /// #### Returns
    ///
    /// an instance of this class that can be used to add components
    public static InfiniteScrollAdapter createInfiniteScroll(Container cont, Runnable fetchMore) {
        return createInfiniteScroll(cont, fetchMore, true);
    }

    /// Creates an instance of the InfiniteScrollAdapter that will invoke the fetch more
    /// callback to fetch additional components, once that method completes its task it
    /// should add the components via the addMoreComponents() invocation.
    /// Notice that the container MUST be empty when invoking this method, fetchMore
    /// will be invoked immediately and you can add your data when ready.
    ///
    /// #### Parameters
    ///
    /// - `cont`: the container to bind, it MUST be empty and must be scrollable on the Y axis
    ///
    /// - `fetchMore`: a callback that will be invoked on the EDT to fetch more data (do not block this method)
    ///
    /// - `fetchOnCreate`: if true the fetchMore callback is called upon calling this method
    ///
    /// #### Returns
    ///
    /// an instance of this class that can be used to add components
    public static InfiniteScrollAdapter createInfiniteScroll(Container cont, Runnable fetchMore, boolean fetchOnCreate) {
        InfiniteScrollAdapter a = new InfiniteScrollAdapter();
        cont.putClientProperty("cn1$infinite", a);
        a.infiniteContainer = cont;
        a.fetchMore = fetchMore;
        if (fetchOnCreate) {
            cont.addComponent(a.ip);
            Display.getInstance().callSerially(fetchMore);
        } else {
            a.infiniteContainer.addComponent(a.endMarker);
        }
        return a;
    }

    /// Invoke this method to add additional components to the container, if you use
    /// addComponent/removeComponent you will get undefined behavior.
    /// This is a convenience method saving the need to keep the InfiniteScrollAdapter as a variable
    ///
    /// #### Parameters
    ///
    /// - `cnt`: container to add the components to
    ///
    /// - `components`: the components to add
    ///
    /// - `areThereMore`: whether additional components exist
    public static void addMoreComponents(Container cnt, Component[] components, boolean areThereMore) {
        InfiniteScrollAdapter ia = (InfiniteScrollAdapter) cnt.getClientProperty("cn1$infinite");
        ia.addMoreComponents(components, areThereMore);
    }

    /// If we previously added components with false for are there more this
    /// method can continue the process of fetching. This is useful in case of
    /// a networking error. You can end fetching and then restart it based on
    /// user interaction see https://github.com/codenameone/CodenameOne/issues/2721
    ///
    /// #### Parameters
    ///
    /// - `cnt`: the container associated with the infinite scroll adapter
    public static void continueFetching(Container cnt) {
        InfiniteScrollAdapter ia = (InfiniteScrollAdapter) cnt.getClientProperty("cn1$infinite");
        ia.continueFetching();
    }

    void reachedEnd() {
        if (infiniteContainer != null) {
            infiniteContainer.removeComponent(endMarker);
            infiniteContainer.addComponent(ip);
            infiniteContainer.revalidate();
        }
        if (fetchMore != null) {
            Display.getInstance().callSerially(fetchMore);
        }
    }

    /// Invoke this method to add additional components to the container, if you use
    /// addComponent/removeComponent you will get undefined behavior.
    ///
    /// #### Parameters
    ///
    /// - `components`: the components to add
    ///
    /// - `areThereMore`: whether additional components exist
    public void addMoreComponents(Component[] components, boolean areThereMore) {
        if (infiniteContainer == null) {
            return;
        }
        infiniteContainer.removeComponent(ip);
        infiniteContainer.removeComponent(endMarker);
        for (Component c : components) {
            c.setY(infiniteContainer.getHeight());
            infiniteContainer.addComponent(c);
        }

        if (componentLimit > 0) {
            int diff = infiniteContainer.getComponentCount() - componentLimit;
            while (diff > 0) {
                infiniteContainer.removeComponent(infiniteContainer.getComponentAt(0));
                diff--;
            }
        }

        infiniteContainer.revalidate();
        if (areThereMore) {
            // if this is animated we get redundant calls to reached end
            infiniteContainer.addComponent(endMarker);
            infiniteContainer.revalidate();
        }
    }

    /// If we previously added components with false for are there more this
    /// method can continue the process of fetching. This is useful in case of
    /// a networking error. You can end fetching and then restart it based on
    /// user interaction see https://github.com/codenameone/CodenameOne/issues/2721
    public void continueFetching() {
        if (endMarker.getParent() == null && fetchMore != null) {
            fetchMore.run();
        }
    }

    /// The component limit defines the number of components that should be within the infinite scroll adapter,
    /// if more than component limit is added then the appropriate number of components is removed from the top.
    /// This prevents running out of memory or performance overhead with too many components...
    /// Notice that -1 is a special case value for no component limit.
    ///
    /// #### Returns
    ///
    /// the componentLimit
    ///
    /// #### Deprecated
    ///
    /// this feature has some inherent problems and doesn't work as expected
    public int getComponentLimit() {
        return componentLimit;
    }

    /// The component limit defines the number of components that should be within the infinite scroll adapter,
    /// if more than component limit is added then the appropriate number of components is removed from the top.
    /// This prevents running out of memory or performance overhead with too many components...
    /// Notice that -1 is a special case value for no component limit.
    ///
    /// #### Parameters
    ///
    /// - `componentLimit`: the componentLimit to set
    ///
    /// #### Deprecated
    ///
    /// this feature has some inherent problems and doesn't work as expected
    public void setComponentLimit(int componentLimit) {
        this.componentLimit = componentLimit;
    }

    /// Lets us manipulate the infinite progress object e.g. set the animation image etc.
    ///
    /// #### Returns
    ///
    /// the infinite progress component underlying this adapter
    public InfiniteProgress getInfiniteProgress() {
        return progress;
    }

    class EdgeMarker extends Component {
        public EdgeMarker() {
        }

        @Override
        public Dimension calcPreferredSize() {
            return new Dimension(1, 1);
        }

        @Override
        public void paint(Graphics g) {
            if (getParent() != null && isInClippingRegion(g)) {
                reachedEnd();
            }
        }
    }
}
