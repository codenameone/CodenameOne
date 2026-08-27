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
        // A search bar lives in a Toolbar and a Toolbar belongs to a Form, so the
        // form is the only top level it can be in and getComponentForm() is the
        // right question to ask.
        if (parent.getComponentForm() == Display.INSTANCE.getCurrent()) { //NOPMD CompareObjectsWithEquals
            search.startEditingAsync();
        } else {
            // setEditOnShow is a Form method, so a search bar inside a desktop
            // Window has nowhere to defer to -- getComponentForm() is null
            // there. Edit it directly instead of dropping the request, which is
            // what the null check used to do.
            Form parentForm = parent.getComponentForm();
            if (parentForm != null) {
                parentForm.setEditOnShow(search);
            } else if (parent.getTopLevelContainer() != null) {
                search.startEditingAsync();
            }
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
                        // getComponentForm() rather than a cast of getParent(): a
                        // search bar is not always a direct child of its form, and
                        // ParparVM does not check CHECKCAST, so that cast would not
                        // fail as a ClassCastException anything could catch.
                        final Form f = SearchBar.this.getComponentForm();
                        if (f == null) {
                            return;
                        }
                        f.getAnimationManager().flushAnimation(new Runnable() {
                            @Override
                            public void run() {
                                f.removeComponentFromForm(SearchBar.this);
                                f.setToolbar(parent);
                                parent.setHidden(false);
                                f.animateLayout(100);
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
