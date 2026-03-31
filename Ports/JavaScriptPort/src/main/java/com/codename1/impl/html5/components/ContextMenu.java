/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.components;

import com.codename1.impl.html5.HTML5Implementation;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.HeavyButtonImpl;
import com.codename1.ui.TextSelection;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Border;

/**
 *
 * @author shannah
 */
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
        TextSelection sel = f.getTextSelection();
        sel.setIgnoreEvents(true);
        final ContextMenu menu = new ContextMenu();
        Container layeredPane = getLayeredPane();
        layeredPane.removeAll();
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
        if (!contains(t.getX(), t.getY())) {
            close();
        }
    }
    
    
}
