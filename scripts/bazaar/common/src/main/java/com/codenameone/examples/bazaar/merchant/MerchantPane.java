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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.bazaar.api.StoreRoleDto;
import com.codenameone.examples.bazaar.live.LiveChannel;
import com.codenameone.examples.bazaar.ui.DesktopActions;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A merchant section: a bar with the store switcher, the section's title and
/// its actions, over the section's content.
///
/// Detail screens open over the content: on a wide screen inside the pane,
/// with a back arrow in the bar and the sidebar still beside it; on a phone as
/// a screen of their own. Every pane claims the desktop menu actions it
/// supports while it is shown and gives them up when it goes, and listens to
/// the live channel for orders and stock.
abstract class MerchantPane extends Container {
    private final Runnable storeChanged = this::storeChanged;
    private final String title;
    private final Container bar = new Container(new BorderLayout());
    private final Container tools = new Container(new FlowLayout(Component.RIGHT));
    private final Container body = new Container(new BorderLayout());
    private final List<Object[]> stack = new ArrayList<Object[]>();
    private final List<Form> screens = new ArrayList<Form>();
    final boolean wide = Layouts.wide();
    private boolean loaded;

    MerchantPane(String title, String name) {
        super(new BorderLayout());
        this.title = title;
        setName(name);
        setUIID("MeRoot");
        bar.setUIID("MeBar");
        tools.setUIID("Container");
        add(BorderLayout.NORTH, bar);
        add(BorderLayout.CENTER, body);
        body.add(BorderLayout.CENTER, new com.codename1.appkit.Skeleton(6, "SvSkeleton"));
        paintBar();
    }

    /// Fills the pane in for the current store. Called when the store is known,
    /// when the owner switches stores and on Refresh.
    abstract void load();

    /// The section's content, replacing whatever is shown.
    final void content(Component c) {
        stack.clear();
        body.removeAll();
        body.add(BorderLayout.CENTER, c);
        paintBar();
        Ui.relayout(this);
    }

    /// Adds a button to the bar.
    final Button tool(char icon, String text, String name, Runnable run) {
        Button b = new Button(Lang.tr(text), "MeTool");
        b.setName(name);
        FontImage.setMaterialIcon(b, icon, 3.4f);
        if (!wide) {
            // On a phone the bar has room for icons only.
            b.setText("");
        }
        b.addActionListener(e -> run.run());
        tools.add(b);
        return b;
    }

    final void clearTools() {
        tools.removeAll();
    }

    /// Opens a detail screen over the content.
    final void push(String heading, Component detail) {
        if (wide) {
            stack.add(new Object[] {heading, detail});
            body.removeAll();
            body.add(BorderLayout.CENTER, detail);
            paintBar();
            Ui.relayout(this);
            return;
        }
        Form form = Ui.form(Lang.tr(heading), "merchant-detail");
        Ui.back(form, this::pop);
        form.add(BorderLayout.CENTER, detail);
        Layouts.dress(form);
        screens.add(form);
        form.show();
    }

    /// Goes back from the detail screen on top.
    final void pop() {
        if (wide) {
            if (stack.isEmpty()) {
                return;
            }
            stack.remove(stack.size() - 1);
            body.removeAll();
            if (stack.isEmpty()) {
                body.add(BorderLayout.CENTER, root == null ? new Label() : root);
            } else {
                body.add(BorderLayout.CENTER, (Component) stack.get(stack.size() - 1)[1]);
            }
            paintBar();
            Ui.relayout(this);
            return;
        }
        if (!screens.isEmpty()) {
            screens.remove(screens.size() - 1);
        }
        Form back = screens.isEmpty() ? getComponentForm() : screens.get(screens.size() - 1);
        if (back != null) {
            back.showBack();
        }
    }

    /// Back to the list after a detail screen saved or deleted something.
    final void popAll() {
        if (wide) {
            while (!stack.isEmpty()) {
                pop();
            }
            return;
        }
        screens.clear();
        Form home = getComponentForm();
        if (home != null && CN.getCurrentForm() != home) {
            home.showBack();
        }
    }

    private Component root;

    /// The section's own content, kept to come back to from a detail screen.
    final void root(Component c) {
        root = c;
        content(c);
    }

    private void paintBar() {
        bar.removeAll();
        Container start = new Container(BoxLayout.x());
        if (wide && !stack.isEmpty()) {
            Button back = new Button("", "MeTool");
            back.setName("merchant-back");
            FontImage.setMaterialIcon(back, FontImage.MATERIAL_ARROW_BACK, 3.6f);
            back.addActionListener(e -> pop());
            start.add(back);
            start.add(Ui.plain(Lang.tr((String) stack.get(stack.size() - 1)[0]), "MeTitle"));
        } else {
            start.add(Ui.plain(Lang.tr(title), "MeTitle"));
        }
        bar.add(BorderLayout.WEST, start);
        Container end = new Container(BoxLayout.x());
        // The tools outlive the bar: they are still inside the previous
        // repaint's container, which removeAll only took off the bar, and
        // adding them again while they have a parent throws. A store change
        // repaints the bar of a pane that is already showing.
        tools.remove();
        if (stack.isEmpty()) {
            end.add(tools);
        }
        Component switcher = switcher();
        if (switcher != null) {
            end.add(switcher);
        }
        bar.add(BorderLayout.EAST, end);
    }

    /// The store switcher: a button with the store's name that lists the
    /// merchant's other stores. Nothing when there is only one.
    private Component switcher() {
        final List<StoreRoleDto> stores = MerchantState.stores();
        if (MerchantState.store() == null) {
            return null;
        }
        Button b = new Button(MerchantState.store().name(), "MeStore");
        b.setName("store-switcher");
        FontImage.setMaterialIcon(b, FontImage.MATERIAL_STORE, 3.2f);
        if (stores.size() < 2) {
            b.setEnabled(false);
            return b;
        }
        b.addActionListener(e -> {
            Command[] items = new Command[stores.size()];
            for (int iter = 0; iter < stores.size(); iter++) {
                final StoreRoleDto s = stores.get(iter);
                items[iter] = Command.create(s.store().name() + Ui.DOT + Lang.tr(Words.role(s.role())),
                        null, ev -> MerchantState.use(s));
            }
            com.codename1.ui.ContextMenu.show(b, b.getWidth() / 2, b.getHeight(), items);
        });
        return b;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        MerchantState.onStore(storeChanged);
        claim();
        if (!loaded) {
            loaded = true;
            MerchantState.whenReady(() -> {
                paintBar();
                load();
            });
        }
    }

    @Override
    protected void deinitialize() {
        MerchantState.offStore(storeChanged);
        DesktopActions.clear(DesktopActions.FIND);
        DesktopActions.clear(DesktopActions.REFRESH);
        DesktopActions.clear(DesktopActions.EXPORT_CSV);
        DesktopActions.clear(DesktopActions.IMPORT_CSV);
        DesktopActions.clear(DesktopActions.NEW_PRODUCT);
        DesktopActions.clear(DesktopActions.PRINT);
        super.deinitialize();
    }

    /// Claims the desktop actions this pane supports. Panes add theirs by
    /// overriding `actions` and calling `DesktopActions.set`.
    private void claim() {
        DesktopActions.set(DesktopActions.REFRESH, this::load);
        DesktopActions.set(DesktopActions.FIND, this::find);
        DesktopActions.set(DesktopActions.PRINT, () -> Ui.say("Nothing to print on this screen"));
        actions();
        LiveChannel.listen(new LiveChannel.Adapter() {
            @Override
            public void orderChanged(String id, String status) {
                MerchantPane.this.orderChanged(id, status);
            }

            @Override
            public void stockChanged(String variantId, String locationId) {
                MerchantPane.this.stockChanged(variantId, locationId);
            }

            @Override
            public void changed(String type, Map<String, Object> frame) {
                MerchantPane.this.liveChanged(type, frame);
            }
        });
    }

    void actions() {
    }

    void find() {
        Ui.say("Nothing to search on this screen");
    }

    void storeChanged() {
        paintBar();
        load();
    }

    void orderChanged(String id, String status) {
    }

    void stockChanged(String variantId, String locationId) {
    }

    /// Any other frame of the live channel (a cart reminded, for one).
    void liveChanged(String type, Map<String, Object> frame) {
    }
}
