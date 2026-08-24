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

import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.plaf.UIManager;

/// SearchBar Toolbar.
///
/// @author Chen
class SearchBar extends Toolbar {

    private final TextField search;

    private final Toolbar parent;
    private final float iconSize;

    /// Creates the SearchBar Toolbar
    ///
    /// #### Parameters
    ///
    /// - `parent`: the Toolbar parent
    public SearchBar(Toolbar parent, float iconSize) {
        this.parent = parent;
        this.iconSize = iconSize;
        search = new TextField();
        search.putClientProperty("searchField", Boolean.TRUE);
        search.setUIID("TextFieldSearch");
        Image img;
        if (iconSize > 0) {
            img = FontImage.createMaterial(FontImage.MATERIAL_SEARCH, UIManager.getInstance().getComponentStyle("TextHintSearch"), iconSize);
        } else {
            img = FontImage.createMaterial(FontImage.MATERIAL_SEARCH, UIManager.getInstance().getComponentStyle("TextHintSearch"));
        }
        String s = getUIManager().localize("m.search", "Search");
        Label hint = new Label(s, img);
        hint.setUIID("TextHintSearch");
        search.setHint(s);
        search.setHintLabelImpl(hint);

        search.addDataChangedListener(new DataChangedListener() {

            @Override
            public void dataChanged(int type, int index) {
                onSearch(search.getText());
            }
        });
        setUIIDFinal("ToolbarSearch");
        // A search bar swapped into a Window's toolbar is already on screen, so it
        // starts editing straight away. setEditOnShow is a Form-only deferral for a
        // toolbar being prepared before its form is shown, and there is no Window
        // equivalent because a Window's search bar is only ever installed live.
        TopLevelContainer top = parent.getTopLevelContainer();
        if (top == Display.INSTANCE.getCurrent() //NOPMD CompareObjectsWithEquals
                || (top instanceof Window && ((Window) top).isWindowShowing())) {
            search.startEditingAsync();
        } else if (top instanceof Form) {
            ((Form) top).setEditOnShow(search);
        }
    }

    void initSearchBar() {
        setTitleComponent(search);
        setBackCommand(new Command("") {

            @Override
            public void actionPerformed(ActionEvent evt) {
                search.stopEditing();
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        onSearch("");
                        // Through the top level rather than a cast of getParent() to
                        // Form: inside a Window the toolbar hangs off the title area,
                        // so that cast named the wrong type. ParparVM does not check
                        // CHECKCAST, so on Mac Catalyst that was a native crash rather
                        // than a ClassCastException anything could catch.
                        final TopLevelContainer f = SearchBar.this.getTopLevelContainer();
                        if (f == null) {
                            return;
                        }
                        f.getAnimationManager().flushAnimation(new Runnable() {
                            @Override
                            public void run() {
                                if (!(f instanceof Form)) {
                                    // A search bar lives in a Toolbar, and only a Form
                                    // has one.
                                    return;
                                }
                                ((Form) f).removeComponentFromForm(SearchBar.this);
                                ((Form) f).setToolbar(parent);
                                parent.setHidden(false);
                                f.asContainer().animateLayout(100);
                            }
                        });
                    }
                });
            }

        }, BackCommandPolicy.AS_ARROW, iconSize);
        Command clear = new Command("") {

            @Override
            public void actionPerformed(ActionEvent evt) {
                search.clear();
            }

        };
        Image img;
        if (iconSize > 0) {
            img = FontImage.createMaterial(FontImage.MATERIAL_CLOSE, UIManager.getInstance().getComponentStyle("TitleCommand"), iconSize);
        } else {
            img = FontImage.createMaterial(FontImage.MATERIAL_CLOSE, UIManager.getInstance().getComponentStyle("TitleCommand"));
        }
        clear.setIcon(img);
        addCommandToRightBar(clear);
    }

    /// This method gets called when a text has changed on the search bar.
    ///
    /// #### Parameters
    ///
    /// - `text`: the search string
    public void onSearch(String text) {

    }

}
