/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.shop;

import com.codename1.ui.Container;
import com.codename1.ui.layouts.Layout;
import com.codenameone.examples.bazaar.live.LiveChannel;
import com.codenameone.examples.bazaar.ui.DesktopActions;
import com.codenameone.examples.bazaar.ui.Layouts;
import java.util.Map;

/// The root of a shopper's section. It claims the desktop menu's Find and
/// Refresh and the live channel while it is on screen, and gives them up when
/// it leaves -- [com.codenameone.examples.bazaar.ui.Shell] swaps sections
/// without telling them, but a component always hears that it was taken off
/// the form.
abstract class ShopPane extends Container {
    private final Runnable cartChanged = this::cartChanged;
    private final Runnable storeChanged = this::storeChanged;

    ShopPane(Layout layout) {
        super(layout);
    }

    /// On a wide screen every section stops at [Layouts#wideColumn()], in the
    /// middle of the room beside the navigation: a grid or a list does not
    /// run to the edges of a desktop monitor.
    @Override
    public void layoutContainer() {
        if (Layouts.wide()) {
            Layouts.cap(this, Layouts.wideColumn());
        }
        super.layoutContainer();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        claim();
        ShopState.onCart(cartChanged);
        ShopState.onStore(storeChanged);
        if (ShopState.store() == null && !(this instanceof HomePane)) {
            ShopState.restoreStore();
        }
        ShopState.syncIfNeeded(null);
    }

    @Override
    protected void deinitialize() {
        ShopState.offCart(cartChanged);
        ShopState.offStore(storeChanged);
        DesktopActions.clear(DesktopActions.FIND);
        DesktopActions.clear(DesktopActions.REFRESH);
        super.deinitialize();
    }

    /// Takes the desktop actions and the live channel again: on first show,
    /// and when a pushed screen (a product, the checkout) comes back here.
    final void claim() {
        DesktopActions.set(DesktopActions.FIND, this::find);
        DesktopActions.set(DesktopActions.REFRESH, this::reload);
        LiveChannel.listen(new LiveChannel.Adapter() {
            @Override
            public void orderChanged(String id, String status) {
                ShopPane.this.orderChanged(id, status);
            }

            @Override
            public void stockChanged(String variantId, String locationId) {
                ShopPane.this.stockChanged(variantId);
            }

            @Override
            public void changed(String type, Map<String, Object> frame) {
            }
        });
    }

    /// Fetches what is shown again.
    abstract void reload();

    /// Cmd+F / Ctrl+F: by default, to the search section.
    void find() {
        ShopSections.search(null);
    }

    void cartChanged() {
    }

    void storeChanged() {
        reload();
    }

    void orderChanged(String id, String status) {
    }

    void stockChanged(String variantId) {
    }
}
