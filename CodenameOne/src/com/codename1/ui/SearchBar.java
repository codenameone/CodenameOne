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
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;

/**
 * SearchBar Toolbar.
 *
 * @author Chen
 */
class SearchBar extends Toolbar {

    private TextField search;

    private Toolbar parent;
    private float iconSize;

    /**
     * Creates the SearchBar Toolbar
     * 
     * @param parent the Toolbar parent
     */ 
    public SearchBar(Toolbar parent, float iconSize) {
        this.parent = parent;
        this.iconSize = iconSize;
        search = new TextField();
        search.setUIID("TextFieldSearch");
        Image img;
        if(iconSize > 0) {
            img = FontImage.createMaterial(FontImage.MATERIAL_SEARCH, UIManager.getInstance().getComponentStyle("TextHintSearch"), iconSize);
        } else {
            img = FontImage.createMaterial(FontImage.MATERIAL_SEARCH, UIManager.getInstance().getComponentStyle("TextHintSearch"));
        }
        Label hint = new Label("Search", img);
        hint.setUIID("TextHintSearch");
        search.setHintLabelImpl(hint);
        
        search.addDataChangeListener(new DataChangedListener() {

            public void dataChanged(int type, int index) {
                onSearch(search.getText());
            }
        });
        setUIID("ToolbarSearch");        
        search.startEditingAsync();
    }

    void initSearchBar() {
        setTitleComponent(search);
        setBackCommand(new Command("") {

            @Override
            public void actionPerformed(ActionEvent evt) {
                search.stopEditing();
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        onSearch("");
                        final Form f = (Form) SearchBar.this.getParent();
                        f.getAnimationManager().flushAnimation(new Runnable() {
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
        if(iconSize > 0) {
            img = FontImage.createMaterial(FontImage.MATERIAL_CLOSE, UIManager.getInstance().getComponentStyle("TitleCommand"), iconSize);
        } else {
            img = FontImage.createMaterial(FontImage.MATERIAL_CLOSE, UIManager.getInstance().getComponentStyle("TitleCommand"));
        }
        clear.setIcon(img);
        addCommandToRightBar(clear);
    }

    /**
     * This method gets called when a text has changed on the search bar.
     * 
     * @param text the search string
     */
    public void onSearch(String text) {

    }

}
