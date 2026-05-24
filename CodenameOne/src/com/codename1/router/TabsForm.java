/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.router;

import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Tabs;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.List;

/// A `Form` whose body is a `Tabs` where **each tab keeps its own navigation stack**.
///
/// Equivalent to the bottom-tab navigators in Flutter (`PersistentTabView`), React
/// Navigation, and iOS UITabBarController: switching tabs preserves the stack of
/// pages that the user pushed inside each tab; back navigates *within* the active
/// tab's stack before exiting the form.
///
/// #### Example
///
/// ```java
/// TabsForm shell = new TabsForm();
/// int home = shell.addTab("Home", null, new HomeContent());
/// int chat = shell.addTab("Chat", null, new ChatList());
/// shell.show();
/// shell.switchToTab(chat);
/// shell.pushInActiveTab(new ConversationView(chatId));  // stacked inside Chat tab
/// // Hardware back / toolbar back: pops the conversation view, leaving the
/// // chat list visible. Tapping the Home tab and coming back: conversation
/// // view is still on top.
/// ```
///
/// #### Router integration
///
/// `TabsForm` is independent of `Router`. When used together, register the shell
/// route with a builder that returns the same `TabsForm` instance for the lifetime
/// of the shell, and route child paths to call `pushInActiveTab` on it:
///
/// ```java
/// final TabsForm shell = new TabsForm();
/// // ... addTab calls ...
/// Router.getInstance()
///     .route("/main", new RouteBuilder() { public Form build(RouteContext c) { return shell; } })
///     .route("/main/chat/:id", new RouteBuilder() {
///         public Form build(RouteContext c) {
///             shell.pushInActiveTab(new ConversationView(c.param("id")));
///             return shell;
///         }
///     });
/// ```
///
/// #### Threading
///
/// All TabsForm methods must be called on the EDT.
///
/// #### Since 8.0
public class TabsForm extends Form {

    private final Tabs tabs;
    private final List<TabStack> stacks = new ArrayList<TabStack>();

    /// Creates an empty TabsForm. Add tabs with #addTab.
    public TabsForm() {
        super(new BorderLayout());
        this.tabs = new Tabs();
        super.addComponent(BorderLayout.CENTER, this.tabs);
        installBackCommand();
    }

    /// Creates a TabsForm with the given title.
    public TabsForm(String title) {
        super(title, new BorderLayout());
        this.tabs = new Tabs();
        super.addComponent(BorderLayout.CENTER, this.tabs);
        installBackCommand();
    }

    /// Returns the underlying `Tabs` component if direct manipulation is required.
    /// Prefer the methods on this class -- adding tabs directly on the returned
    /// `Tabs` will skip stack bookkeeping.
    public Tabs getTabs() {
        return tabs;
    }

    /// Adds a tab whose root component is `root`. Returns the tab index.
    /// The component is wrapped in an internal holder so this class can swap in
    /// pushed children without touching `Tabs`'s own children list.
    public int addTab(String title, Image icon, Component root) {
        if (root == null) { throw new IllegalArgumentException("root cannot be null"); }
        Container holder = new Container(new BorderLayout());
        holder.add(BorderLayout.CENTER, root);
        tabs.addTab(title, icon, holder);
        stacks.add(new TabStack(holder, root));
        return stacks.size() - 1;
    }

    /// Convenience overload for icon-less tabs.
    public int addTab(String title, Component root) {
        return addTab(title, null, root);
    }

    /// Switches to the tab at `index`, preserving each tab's pushed stack.
    public void switchToTab(int index) {
        if (index < 0 || index >= stacks.size()) {
            throw new IndexOutOfBoundsException("Tab index " + index + " out of range");
        }
        tabs.setSelectedIndex(index);
    }

    /// Returns the currently selected tab index.
    public int getActiveTabIndex() {
        return tabs.getSelectedIndex();
    }

    /// Returns the number of tabs.
    public int getTabCount() {
        return stacks.size();
    }

    /// Pushes a component onto the active tab's stack. The component becomes the
    /// visible content for that tab. Existing pushed content is preserved
    /// underneath and will reappear on `popInActiveTab`.
    public void pushInActiveTab(Component c) {
        if (c == null) { throw new IllegalArgumentException("component cannot be null"); }
        TabStack ts = activeStack();
        ts.push(c);
    }

    /// Pops the active tab's stack. Returns `true` if a frame was popped, `false`
    /// if the tab was already at its root.
    public boolean popInActiveTab() {
        return activeStack().pop();
    }

    /// Returns the depth of the active tab's stack. 1 means we're at the tab root.
    public int getActiveStackDepth() {
        return activeStack().depth();
    }

    /// Returns the depth of an arbitrary tab.
    public int getStackDepth(int tabIndex) {
        return stacks.get(tabIndex).depth();
    }

    /// Adds a tab-selection listener. Mirrors `Tabs#addSelectionListener` so app
    /// code can subscribe through the shell directly without unwrapping the tabs.
    public void addTabSelectionListener(SelectionListener l) {
        tabs.addSelectionListener(l);
    }

    /// Removes a tab-selection listener.
    public void removeTabSelectionListener(SelectionListener l) {
        tabs.removeSelectionListener(l);
    }

    private void installBackCommand() {
        setBackCommand(Command.create("Back", null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Pop within the active tab first. Only if the tab is already at
                // its root do we fall through to exiting the form: by default we
                // simply do nothing, leaving the user in the shell. Callers that
                // want the form to exit on back when all stacks are empty can
                // override #onShellBack.
                if (popInActiveTab()) {
                    return;
                }
                onShellBack();
            }
        }));
    }

    /// Called when the back command fires and the active tab is already at its
    /// root. Default implementation does nothing (sticky shell). Override to
    /// `Router.pop()` or to `previousForm.showBack()` if you want the shell to
    /// exit on a second back.
    protected void onShellBack() {
        // Default: no-op. The bottom-tab shell is sticky.
    }

    private TabStack activeStack() {
        int idx = tabs.getSelectedIndex();
        if (idx < 0 || idx >= stacks.size()) {
            throw new IllegalStateException("No active tab");
        }
        return stacks.get(idx);
    }

    private static final class TabStack {
        final Container holder;
        final List<Component> entries = new ArrayList<Component>();

        TabStack(Container holder, Component root) {
            this.holder = holder;
            this.entries.add(root);
        }

        int depth() { return entries.size(); }

        void push(Component c) {
            Component current = entries.get(entries.size() - 1);
            holder.replace(current, c, null);
            entries.add(c);
        }

        boolean pop() {
            if (entries.size() <= 1) { return false; }
            Component current = entries.remove(entries.size() - 1);
            Component prev = entries.get(entries.size() - 1);
            holder.replace(current, prev, null);
            return true;
        }
    }
}
