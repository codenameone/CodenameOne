/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.impl.html5.components;

import com.codename1.impl.html5.HTML5Implementation;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.HeavyButtonImpl;
import com.codename1.ui.TextSelection;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.PointerEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Border;

public class ContextMenu extends Container implements ActionListener {
    private HeavyButtonImpl copy = new HeavyButtonImpl("Copy");
    private Button selectAll = new Button("Select All");
    
    private ContextMenu() {
        initUI();
    }
    
    private void initUI() {
        setLayout(BoxLayout.y());
        copy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent t) {
                String selectedText = HTML5Implementation.getInstance().getSelectedText();
                if (selectedText == null || selectedText.isEmpty()) {
                    return;
                }
                HTML5Implementation.getInstance().copySelectionToClipboard(null);
                
            }
            
        });
        
        selectAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent t) {
                HTML5Implementation.callSerially(new Runnable() {
                    public void run() {
                        Form f = CN.getCurrentForm();
                        if (f == null) {
                            return;
                        }
                        TextSelection sel = f.getTextSelection();
                        if (sel == null || !sel.isEnabled()) {
                            return;
                        }
                        sel.selectAll();
                    }
                });
            }
            
        });
        
        $(copy, selectAll).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent t) {
                HTML5Implementation.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        close();
                    }
                    
                });
            }
            
        });
        
        String textSelection = HTML5Implementation.getInstance().getSelectedText();
        if (textSelection != null && !textSelection.isEmpty()) {
            add(copy);
        }
        add(selectAll);
        $(this).selectAllStyles()
                .setBorder(Border.createLineBorder(1, 0x666666))
                .setBgColor(0xffffff)
                .setBgTransparency(0xff);
        $(copy, selectAll).setFgColor(0x0);
                
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        getComponentForm().addPointerPressedListener(this);
    }

    @Override
    protected void deinitialize() {
        getComponentForm().removePointerPressedListener(this);
        close();
        super.deinitialize();
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(max(copy.getOuterPreferredW(), selectAll.getOuterPreferredW()) + getStyle().getHorizontalPadding(), copy.getOuterPreferredH() + selectAll.getOuterPreferredH() + getStyle().getVerticalPadding());
    }
    
    private int max(int... ints) {
        Integer out = null;
        for (int i : ints) {
            out = out == null ? i : Math.max(i, out);
        }
        return out == null ? 0 : out;
    }
    
    
    private static Container getLayeredPane() {
        return CN.getCurrentForm().getFormLayeredPane(ContextMenu.class, true);
    }
    
    public static ContextMenu showAt(final int x, final int y) {
        Form f = CN.getCurrentForm();
        if (f == null) {
            return null;
        }
        final ContextMenu menu = new ContextMenu();
        // A menu already open is closed first, and the pane fetched again afterwards: closing
        // removes the pane from the form, and a menu added to the old one was never painted --
        // a second right-click showed nothing.
        getLayeredPane().removeAll();
        // After that close, not before: closing the old menu switches selection handling back
        // on, and the new menu's Copy then found the selection cleared by its own press.
        f.getTextSelection().setIgnoreEvents(true);
        Container layeredPane = getLayeredPane();
        layeredPane.add(menu);
        layeredPane.setLayout(new Layout() {
            @Override
            public void layoutContainer(Container cntnr) {
               menu.setX(x);
               menu.setY(y);
               menu.setWidth(menu.getPreferredW());
               menu.setHeight(menu.getPreferredH());
               
               if (menu.getWidth() + menu.getX() > CN.getDisplayWidth()) {
                   menu.setX(x - menu.getWidth() - CN.convertToPixels(4));
               }
               if (menu.getHeight() + menu.getY() > CN.getDisplayHeight()) {
                   menu.setY(y - menu.getHeight() - CN.convertToPixels(4));
               }
            }

            @Override
            public Dimension getPreferredSize(Container cntnr) {
                return new Dimension(CN.getDisplayWidth(), CN.getDisplayHeight());
            }
            
        });
        
        layeredPane.revalidateWithAnimationSafety();
        return menu;
        
    }
    private boolean closed;
    public void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        Form f = getComponentForm();
        f.getTextSelection().setIgnoreEvents(false);
        Container layeredPane = getLayeredPane();
        remove();
        layeredPane.remove();
        
        if (f != null) {
            f.revalidateWithAnimationSafety();
        }
    }

    @Override
    public void actionPerformed(ActionEvent t) {
        // Not the right-button press: the browser raises contextmenu straight after its
        // pointerdown, and that press reaches the form behind the worker bridge -- often after
        // this menu is already up -- so closing on it took the menu down the moment it opened.
        // A right-click elsewhere raises contextmenu again and moves the menu there anyway.
        if (Display.getInstance().getPointerButton() == PointerEvent.BUTTON_SECONDARY) {
            return;
        }
        if (!contains(t.getX(), t.getY())) {
            close();
        }
    }
    
    
}
