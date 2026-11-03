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
package com.codenameone.examples.pulse.ui;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// A screen pushed on top of a home screen, which on a wide window shows the
/// home screen's menu docked down its side.
///
/// The menu is docked as the screen is shown, once the screen has its
/// content: what was in the content pane moves into a body beside the menu,
/// with the place each piece had. Everything the screen adds or takes away
/// after that goes to the body, so a screen is written the same way for a
/// phone and a desktop and never knows the menu is there.
final class Screen extends Form {
    private Container body;

    Screen(String title) {
        super(title, new BorderLayout());
    }

    @Override
    public void show() {
        Menu.dock(this);
        super.show();
    }

    @Override
    public void showBack() {
        Menu.dock(this);
        super.showBack();
    }

    /// Moves the content beside `side`. Called once, by the menu.
    void dockBeside(Container side) {
        Container content = getContentPane();
        Container moved = new Container(new BorderLayout());
        while (content.getComponentCount() > 0) {
            Component child = content.getComponentAt(0);
            Object where = content.getLayout().getComponentConstraint(child);
            content.removeComponent(child);
            if (where == null) {
                moved.add(child);
            } else {
                moved.add(where, child);
            }
        }
        content.add(BorderLayout.WEST, side);
        content.add(BorderLayout.CENTER, moved);
        body = moved;
    }

    @Override
    public void addComponent(Component cmp) {
        if (body == null) {
            super.addComponent(cmp);
        } else {
            body.addComponent(cmp);
        }
    }

    @Override
    public void addComponent(Object constraints, Component cmp) {
        if (body == null) {
            super.addComponent(constraints, cmp);
        } else {
            body.addComponent(constraints, cmp);
        }
    }

    @Override
    public void addComponent(int index, Object constraints, Component cmp) {
        if (body == null) {
            super.addComponent(index, constraints, cmp);
        } else {
            body.addComponent(index, constraints, cmp);
        }
    }

    @Override
    public void addComponent(int index, Component cmp) {
        if (body == null) {
            super.addComponent(index, cmp);
        } else {
            body.addComponent(index, cmp);
        }
    }

    @Override
    public void removeComponent(Component cmp) {
        if (body != null && cmp.getParent() == body) {
            body.removeComponent(cmp);
        } else {
            super.removeComponent(cmp);
        }
    }

    @Override
    public void removeAll() {
        if (body == null) {
            super.removeAll();
        } else {
            body.removeAll();
        }
    }
}
