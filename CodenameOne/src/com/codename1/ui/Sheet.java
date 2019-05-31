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
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;

/**
 * A light-weight dialog that slides up from the bottom of the screen on mobile devices. 
 * Sheets include a "title" bar, with a back/close button, a title label, and a "commands container" ({@link #getCommandsContainer() })
 * which allows you to insert your own custom components (usually buttons) in the upper right.
 * 
 * <p>Custom content should be placed inside the content pane which can be retrieved via {@link #getContentPane() }</p>
 * 
 * <h3>Usage</h3>
 * 
 * <p>The general usage is to create new Sheet instance (or subclass), then call {@link #show() }
 * to make it appear over the current form.  If a different sheet that is currently being displayed, then
 * calling {@link #show() } will replace it.</p>
 * 
 * <h4>Inter-Sheet Navigation</h4>
 * 
 * <p>The {@link #Sheet(com.codename1.ui.Sheet, java.lang.String) } constructor can take another
 * Sheet object as a parameter, which will act as a "parent" sheet ({@link #getParentSheet() }.  If the parent
 * sheet is not null, then {@literal this} sheet will have a "Back" button instead of a "Close" button.  THe
 * "Back" button will navigate back to the parent sheet.</p>
 * 
 * <p>When navigating between sheets, the sheet will be resized with a smooth slide animation to the preferred
 * height of the destination sheet.</p>
 * 
 * <p><strong>Example</strong></p>
 * 
 * <p><pre>{@code 
 *  public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form hi = new Form("Hi World", new BorderLayout());


        Button b = new Button("Open Sheet");
        b.addActionListener(e-&gt;{
            new MySheet(null).show();
            
            
        });
        hi.add(BorderLayout.NORTH, b);
        hi.show();
    }
    
    private class MySheet extends Sheet {
        MySheet(Sheet parent) {
            super(parent, "My Sheet");
            Container cnt = getContentPane();
            cnt.setLayout(BoxLayout.y());
            Button gotoSheet2 = new Button("Goto Sheet 2");
            gotoSheet2.addActionListener(e-&gt;{
                new MySheet2(this).show(300);
            });
            cnt.add(gotoSheet2);
            for (String t : new String[]{"Red", "Green", "Blue", "Orange"}) {
                cnt.add(new Label(t));
            }
        }
    }
    
    private class MySheet2 extends Sheet {
        MySheet2(Sheet parent) {
            super(parent, "Sheet 2");
            Container cnt = getContentPane();
            cnt.setLayout(BoxLayout.y());
            cnt.setScrollableY(true);
            for (int i=0; i&lt;2; i++) {
                for (String t : new String[]{"Red", "Green", "Blue", "Orange"}) {
                    cnt.add(new Label(t));
                }
            }
        }
    }

 * }</pre></p>
 * 
 * <h4>Video Sample</h4>
 * 
 * <p><a href="https://youtu.be/3okEj_JW3-k">Screen cast of the SheetSample demo</a></p>
 * <p>View source for this sample <a href="https://github.com/codenameone/CodenameOne/tree/master/Samples/samples/SheetSample">here</a>.  
 * This sample can be run directly in the <a href="https://github.com/codenameone/CodenameOne/tree/master/Samples/">SampleRunner</a>.</p>
 * 
 * @author shannah
 * @since 7.0
 */
public class Sheet extends Container {
    private final Sheet parentSheet;
    private Button backButton = new Button(FontImage.MATERIAL_CLOSE);
    private final Label title = new Label();
    private Container commandsContainer = new Container(BoxLayout.x());
    private Container titleBar = BorderLayout.center(LayeredLayout.encloseIn(
            BorderLayout.center(FlowLayout.encloseCenterMiddle(title)),
            BorderLayout.centerEastWest(null, commandsContainer, backButton)
    ));
    private Container contentPane = new Container(BoxLayout.y());
    private static int DEFAULT_TRANSITION_DURATION=300;
    private ActionListener formPointerListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            Form f = getComponentForm();
            if (f == null) {
                return;
            }
            if (Display.impl.isScrollWheeling()) {
                return;
            }
            Component cmp = f.getComponentAt(evt.getX(), evt.getY());
            if (cmp == null) {
                return;
            }
            if (Sheet.this.contains(cmp) || Sheet.this == cmp || cmp.isOwnedBy(Sheet.this)) {
                // do nothing.
            } else {
                hide(DEFAULT_TRANSITION_DURATION);
            }
        }
        
    };
    
    /**
     * Creates a new sheet with the specified parent and title.
     * @param parent Optional parent sheet.  If non-null, then this sheet will have a "back" button instead of a "close" button.  The "back" button will return to the parent sheet.
     * @param title The title to display in the title bar of the sheet.
     */
    public Sheet(Sheet parent, String title) {
        this(parent, title, "Sheet");
    }
    
    /**
     * Creates a new sheet with the specified parent and title.
     * @param parent Optional parent sheet.  If non-null, then this sheet will have a "back" button instead of a "close" button.  The "back" button will return to the parent sheet.
     * @param title The title to display in the title bar of the sheet.
     * @param uiid Optional UIID for the sheet.  If non-null, then the Sheet's uiid will be {@literal uiid}, the title label's UIID will be {@literal uiid + "Title"},
     * the title bar's UIID will be {@literal uiid + "TitleBar"}, and the back/close button's UIID will be {@literal uiid + "BackButton"}.
     */
    public Sheet(Sheet parent, String title, String uiid) {
        if (uiid == null) {
            uiid = "Sheet";
        }
        setGrabsPointerEvents(true);
        this.setUIID(uiid);       
        this.title.setUIID(uiid+"Title");
        titleBar.setUIID(uiid+"TitleBar");
        backButton.setUIID(uiid+"BackButton");
       
        this.parentSheet = parent;
        this.title.setText(title);
        initUI();
        
    }

    /**
     * Gets the content pane of the sheet.  All sheet content should be added to the content pane
     * and not directly to the sheet.
     * @return The content pane.
     */
    public Container getContentPane() {
        return contentPane;
    }
    
    /**
     * Hides the back button.
     */
    public void hideBackButton() {
        backButton.setVisible(false);
    }
    
    /**
     * Shows the back button.
     */
    public void showBackButton() {
        backButton.setVisible(true);
    }
    
    /**
     * Gets the container that is rendered on the top right bar of the sheet.  Use this
     * to add buttons and other content you wish to appear in the title bar.  Best not to
     * overload this with too many things.
     * @return 
     */
    public Container getCommandsContainer() {
        return commandsContainer;
    }

    private void initUI() {
        setLayout(new BorderLayout());
        add(BorderLayout.NORTH, titleBar);
        if (parentSheet != null) {
            FontImage.setMaterialIcon(backButton, FontImage.MATERIAL_ARROW_BACK);
        }
        add(BorderLayout.CENTER, contentPane);
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                back(DEFAULT_TRANSITION_DURATION);
            }
        });
        
    }
    
    /**
     * Shows the sheet with the default (300ms) transition duration.  
     * @see #show(int) 
     */
    public void show() {
        show(DEFAULT_TRANSITION_DURATION);
    }
    
   
    /**
     * Shows the sheet over the current form using a slide-up transition with given duration in milliseconds.
     * 
     * <p>If another sheet is currently being shown, then this will replace that sheet, and use an appropriate slide 
     * animation to adjust the size.</p>
     * @param duration The duration of the slide transition in milliseconds.
     * @see #show() 
     */
    public void show(final int duration) {
        Form f = CN.getCurrentForm();
        if (f.getAnimationManager().isAnimating()) {
            f.getAnimationManager().flushAnimation(new Runnable() {
                public void run() {
                    show(duration);
                }
            });
            return;
        }
        if (getParent() != null) {
            remove();
        }
        Container cnt = CN.getCurrentForm().getFormLayeredPane(Sheet.class, true);
        if (!(cnt.getLayout() instanceof BorderLayout)) {
            cnt.setLayout(new BorderLayout());
            
            cnt.getStyle().setBgPainter(new Painter() {
                @Override
                public void paint(Graphics g, Rectangle rect) {
                    int alph = g.getAlpha();
                    g.setAlpha((int)(alph * 30/100.0));
                    g.setColor(0x0);
                    g.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                    g.setAlpha(alph);
                }
                
            });

            cnt.revalidate();
            
        }
        if (cnt.getComponentCount() > 0) {
            Component existing = cnt.getComponentAt(0);
            cnt.replace(existing, this, null);
            cnt.animateLayout(duration);
        } else {
            cnt.add(BorderLayout.SOUTH, this);
            this.setWidth(cnt.getWidth());
            this.setHeight(getPreferredH());
            this.setX(0);
            this.setY(cnt.getHeight());
            cnt.animateLayout(duration);
        }
    }
    
    /**
     * Goes back to the parent sheet with a default (300ms) slide animation.  If there
     * is no parent sheet, then this will close the sheet.
     * @see #back(int) 
     */
    public void back() {
        back(DEFAULT_TRANSITION_DURATION);
    }
    
    /**
     * Goes back to the parent sheet with a slide animation of given duration.  If there
     * is no parent sheet, then this will close the sheet.
     * @param duration Duration of the slide transition in milliseconds.
     */
    public void back(int duration) {
        if (this.parentSheet != null) {
            this.parentSheet.show(duration);
        } else {
            hide(duration);
        }
    }
    

    private void hide(int duration) {
        final Container cnt = CN.getCurrentForm().getFormLayeredPane(Sheet.class, true);
        setY(cnt.getHeight());
        cnt.animateUnlayout(duration, 255, new Runnable() {
            public void run() {
                Container parent = cnt.getParent();
                
                if (parent != null && parent.getComponentForm() != null) {
                    cnt.remove();
                    parent.getComponentForm().revalidateWithAnimationSafety();
                    
                }
                
            }
        });
        
    }
    
    
    /**
     * Gets the parent sheet or null if there is none.
     * @return The parent sheet or null.
     */
    public Sheet getParentSheet() {
        return parentSheet;
    }

    private Form form;
    @Override
    protected void initComponent() {
        super.initComponent();
        form = getComponentForm();
        if (form != null) {
            form.addPointerPressedListener(formPointerListener);
        }
    }

    @Override
    protected void deinitialize() {
        if (form != null) {
            form.removePointerPressedListener(formPointerListener);
            form = null;
        }
        super.deinitialize(); 
    }
    
    
    
    
    
    
}
