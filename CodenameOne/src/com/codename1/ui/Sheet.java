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

import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.ComponentSelector.ComponentClosure;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;

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
    private EventDispatcher closeListeners = new EventDispatcher();
    private EventDispatcher backListeners = new EventDispatcher();
    private Button backButton = new Button(FontImage.MATERIAL_CLOSE);
    private final Label title = new Label();
    private Container commandsContainer = new Container(BoxLayout.x());
    private boolean allowClose = true;
    private Container titleBar = BorderLayout.center(LayeredLayout.encloseIn(
            BorderLayout.center(FlowLayout.encloseCenterMiddle(title)),
            BorderLayout.centerEastWest(null, commandsContainer, backButton)
    ));
    private Container contentPane = new Container(BoxLayout.y());
    private static int DEFAULT_TRANSITION_DURATION=300;
    
    /**
     * The position on the screen where the sheet is displayed on phones.
     * One of {@link BorderLayout#CENTER}, {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
     * {@link BorderLayout#WEST}. {@link BorderLayout#EAST.  Default is {@link BorderLayout#SOUTH}.
     * 
     * @see #setPosition(java.lang.String) 
     * @see #setPosition(java.lang.String, java.lang.String) 
     */
    private String position = BorderLayout.SOUTH;
    /**
     * The position on the screen where the sheet is displayed on tablets.
     * One of {@link BorderLayout#CENTER}, {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
     * {@link BorderLayout#WEST}. {@link BorderLayout#EAST.  Default is {@link BorderLayout#SOUTH}.
     * 
     * @see #setPosition(java.lang.String) 
     * @see #setPosition(java.lang.String, java.lang.String) 
     */
    private String tabletPosition = position;
    
    
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
                evt.consume();
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
        if (parent != null) {
            allowClose = parent.allowClose;
            position = parent.position;
            tabletPosition = parent.tabletPosition;
        }
        if (uiid == null) {
            uiid = "Sheet";
        }
        $(this).addTags("Sheet");
        setGrabsPointerEvents(true);
        this.setUIID(uiid);       
        this.title.setUIID(uiid+"Title");
        titleBar.setUIID(uiid+"TitleBar");
        backButton.setUIID(uiid+"BackButton");
       
        this.parentSheet = parent;
        this.title.setText(title);
        initUI();
        updateBorderForPosition();
        
    }
    
    /**
     * Sets whether the user is able to close this sheet.  Default is true.  If you set
     * this value to false, then there will be no close button, and pressing outside of the sheet
     * will have no effect.
     * 
     * <p>Child sheets will assume the settings of the parent.  The back button will still work,
     * but the top level sheet will not include a close button.</p>
     * 
     * @param allowClose True to allow user to close the sheet.  False to prevent it.
     * @since 7.0
     */
    public void setAllowClose(boolean allowClose) {
        if (allowClose != this.allowClose) {
            this.allowClose = allowClose;
            if (!allowClose && isInitialized()) {
                form.removePointerPressedListener(formPointerListener);
            } else if (allowClose && isInitialized()) {
                form.addPointerPressedListener(formPointerListener);
            }
            if (parentSheet == null) {
                backButton.setVisible(allowClose);
                backButton.setEnabled(allowClose);
            }
        }
    }
    
    /**
     * Checks whether the user is allowed to close this sheet.
     * @return True if user can close the sheet.
     * 
     */
    public boolean isAllowClose() {
        return allowClose;
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
        contentPane.setSafeArea(true);
        titleBar.setSafeArea(true);
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
     * Gets the current sheet on the current form or null if no sheet is currently being displayed.
     * @return The current sheet or null.
     * @since 7.0
     */
    public static Sheet getCurrentSheet() {
        if (CN.getCurrentForm() == null) {
            return null;
        }
        Container cnt = CN.getCurrentForm().getFormLayeredPaneIfExists();
        if (cnt == null) {
            return null;
        }
        class Result {
            Sheet found;
        }
        for (Component cmp : $(".Sheet", cnt)) {
            if (cmp instanceof Sheet) {
                return (Sheet)cmp;
            }
        }
        return null;
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
        
        // We need to add some margin to the title  to prevent overlap with the 
        // back button and the commaneds.
        int titleMargin = Math.max(
                commandsContainer.getPreferredW() + commandsContainer.getStyle().getHorizontalMargins(), 
                backButton.getPreferredW() + backButton.getStyle().getHorizontalMargins()
        );

        // Set the padding in the content pane to match the corner radius
        Style s = getStyle();
        Style titleParentStyle = title.getParent().getStyle();
        titleParentStyle.setMarginLeft(titleMargin);
        titleParentStyle.setMarginRight(titleMargin);
        Border border = s.getBorder();
        if (border instanceof RoundRectBorder) {
            RoundRectBorder b = (RoundRectBorder)border;
            
            $(contentPane).setPaddingMillimeters(b.getCornerRadius());
        }
        
        // Deal with iPhoneX notch.
        UIManager uim = UIManager.getInstance();
        
        Style statusBarStyle =  uim.getComponentStyle("StatusBar");
        Style titleAreaStyle = uim.getComponentStyle("TitleArea");
        
        int topPadding = statusBarStyle.getPaddingTop() + statusBarStyle.getPaddingBottom() + titleAreaStyle.getPaddingTop();
        int positionInt = getPositionInt();
        Rectangle displaySafeArea = new Rectangle();
        Display.getInstance().getDisplaySafeArea(displaySafeArea);
        int bottomPadding = s.getPaddingBottom();
        int safeAreaBottomPadding = CN.getDisplayHeight() - (displaySafeArea.getY() + displaySafeArea.getHeight());
        bottomPadding = bottomPadding + safeAreaBottomPadding;
        if (positionInt == S || positionInt == C) {
            // For Center and South position we use margin to 
            // prevent overlap with top notch.  This looks better as overlap is only
            // an edge case that occurs when the sheet is the full screen height.
            $(this).setMargin(topPadding, 0 , 0, 0);
            $(this).setPadding(s.getPaddingTop(), s.getPaddingRightNoRTL(), bottomPadding, s.getPaddingLeftNoRTL());
        } else {
            // For other cases we use padding to prevent overlap with top notch.  This looks
            // better as it appears that the sheet bleeds all the way to the top edge of the screen,
            // but the content is not obscured by the notch.


            $(this).setPadding(topPadding, s.getPaddingRightNoRTL(), bottomPadding, s.getPaddingLeftNoRTL());
        }
       
        // END Deal with iPhoneX notch
        
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
            cnt.setLayout(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));
            
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
            $(".Sheet", cnt).each(new ComponentClosure() {
                @Override
                public void call(Component c) {
                    if (c instanceof Sheet) {
                        Sheet s = (Sheet)c;
                        if (s.isAncestorSheetOf(Sheet.this) || s == Sheet.this) {
                            // If the sheet is an ancestor of
                            // ours then we don't need to fire a close event
                            // yet.  We fire it when it is closed
                            // without possibility of returning 
                            // via a back chain
                            return;
                        }
                        s.fireCloseEvent(false);
                        
                        // Hiding this sheet may eliminate the possibility of 
                        // its parent sheets from being shown again,
                        // so their close events should also be fired in this case.
                        Sheet sp = s.getParentSheet();
                        while (sp != null) {
                            if (sp == Sheet.this) {
                                break;
                            }
                            if (!sp.isAncestorSheetOf(Sheet.this)) {
                                sp.fireCloseEvent(false);
                            }
                            sp = sp.getParentSheet();
                            
                        }
                    }
                    
                    
                }
                
            });
            Component existing = cnt.getComponentAt(0);
            cnt.replace(existing, this, null);
            cnt.animateLayout(duration);
        } else {
            cnt.add(getPosition(), this);
            
            this.setWidth(getPreferredW(cnt));
            this.setHeight(getPreferredH(cnt));
            this.setX(getHiddenX(cnt));
            this.setY(getHiddenY(cnt));
            cnt.animateLayout(duration);
        }
    }
    
    /**
     * Gets the position where the Sheet is to be displayed. 
     * One of {@link BorderLayout#CENTER}, {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
     * {@link BorderLayout#WEST}. {@link BorderLayout#EAST.  Default is {@link BorderLayout#SOUTH}.
     * 
     * @see #setPosition(java.lang.String) 
     * @see #setPosition(java.lang.String, java.lang.String) 
     */
    public String getPosition() {
        if (CN.isTablet()) {
            return tabletPosition;
        }
        return position;
    }
    
    /**
     * Sets the position where the Sheet is to be displayed. 
     * One of {@link BorderLayout#CENTER}, {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
     * {@link BorderLayout#WEST}. {@link BorderLayout#EAST.  Default is {@link BorderLayout#SOUTH}.
     * 
     * @param position One of {@link BorderLayout#CENTER}, {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
     * {@link BorderLayout#WEST}. {@link BorderLayout#EAST.
     * @see #setPosition(java.lang.String) 
     * @see #setPosition(java.lang.String, java.lang.String) 
     * @since 7.0
     */
    public void setPosition(String position) {
        if (CN.isTablet()) {
            if (!position.equals(tabletPosition)) {
                tabletPosition = position;
                updateBorderForPosition();
            }
        } else {
            if (!position.equals(this.position)) {
                this.position = position;
                updateBorderForPosition();
            }
        }
    }
    
    private void updateBorderForPosition() {
        Border border = getStyle().getBorder();
        if (border instanceof RoundRectBorder) {
            RoundRectBorder b = (RoundRectBorder)border;
            RoundRectBorder nb = RoundRectBorder.create();
            nb.bezierCorners(b.isBezierCorners());
            nb.bottomLeftMode(b.isBottomLeft());
            nb.bottomRightMode(b.isBottomRight());
            nb.topRightMode(b.isTopRight());
            nb.topLeftMode(b.isTopLeft());
            nb.cornerRadius(b.getCornerRadius());
            nb.shadowBlur(b.getShadowBlur());
            nb.shadowColor(b.getShadowColor());
            nb.shadowOpacity(b.getShadowOpacity());
            nb.shadowSpread(b.getShadowSpread());
            nb.shadowX(b.getShadowX());
            nb.shadowY(b.getShadowY());
            nb.strokeColor(b.getStrokeColor());
            nb.strokeOpacity(b.getStrokeOpacity());
            nb.stroke(b.getStrokeThickness(), b.isStrokeMM());
            b = nb;
            switch (getPositionInt()) {
                case C:
                    b.bottomRightMode(true);
                    b.bottomLeftMode(true);
                    b.topLeftMode(true);
                    b.topRightMode(true);
                    break;
                case E:
                    b.bottomLeftMode(true);
                    b.topLeftMode(true);
                    b.topRightMode(false);
                    b.bottomRightMode(false);
                    break;
                case W:
                    b.bottomLeftMode(false);
                    b.bottomRightMode(true);
                    b.topLeftMode(false);
                    b.topRightMode(true);
                    break;
                case S:
                    b.topLeftMode(true);
                    b.topRightMode(true);
                    b.bottomLeftMode(false);
                    b.bottomRightMode(false);
                    break;
                    
                case N:
                    b.topLeftMode(false);
                    b.topRightMode(false);
                    b.bottomLeftMode(true);
                    b.bottomRightMode(true);
                    break;
                   
            }
            getStyle().setBorder(b);
            
        }
        
    }
    
    /**
     * Sets the position where the Sheet is to be displayed. 
     * One of {@link BorderLayout#CENTER}, {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
     * {@link BorderLayout#WEST}. {@link BorderLayout#EAST.  Default is {@link BorderLayout#SOUTH}.
     * 
     * @param phonePosition Position to use on a phone (i.e. non-tablet). One of {@link BorderLayout#CENTER}, {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
     * {@link BorderLayout#WEST}. {@link BorderLayout#EAST.
     * @param tabletPosition Position to use on a tablet and desktop. One of {@link BorderLayout#CENTER}, {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
     * {@link BorderLayout#WEST}. {@link BorderLayout#EAST.
     * @see #setPosition(java.lang.String) 
     * @see #setPosition(java.lang.String, java.lang.String) 
     * @since 7.0
     */
    public void setPosition(String phonePosition, String tabletPosition) {
        boolean changed = false;
        if (CN.isTablet() && !tabletPosition.equals(this.tabletPosition)) {
            changed = true;
        } else if (!CN.isTablet() && !phonePosition.equals(position)) {
            changed = true;
        }
        position = phonePosition;
        this.tabletPosition = tabletPosition;
        if (changed) {
            updateBorderForPosition();
        }
    }
    
    /**
     * Gets X-coordinate of the sheet when it is hidden off-screen.  This will be different
     * depending on the position of the sheet.
     * @param cnt The container in the FormLayeredPane where the sheet is to be rendered.
     * @return 
     */
    private int getHiddenX(Container cnt) {
        switch (getPositionInt()) {
            case S:
            case N:
                return 0;
            case C:
                return (cnt.getWidth() - getPreferredW(cnt))/2;
            case E:
                return cnt.getWidth();
            case W:
                return -getPreferredW(cnt);
        }
        return 0;
    }
    
    /**
     * Gets Y-coordinate of the sheet when it is hidden off-screen.  This will be different
     * depending on the position of the sheet.
     * @param cnt The container in the FormLayeredPane where the sheet is to be rendered.
     * @return 
     */
    private int getHiddenY(Container cnt) {
        switch (getPositionInt()) {
            case S:
            case C:
                return cnt.getHeight();
            case W:
            case E:
                return 0;
            case N:
                return -getPreferredH(cnt); 
        }
        return 0;
    }
    
    /**
     * Gets the preferred width of the sheet.  Will depend on where it is rendered.  If position is CENTER,
     * then the preferred width will be the natural preferred width of the sheet.  But NORTH or SOUTH,
     * the preferred width will be the full container width.
     * 
     * @param cnt The container in the FormLayeredPane where the sheet is to be rendered.
     * @return 
     */
    private int getPreferredW(Container cnt) {
        switch (getPositionInt()) {
            case N:
            case S:
                return cnt.getWidth();
            case C:
            case W:
            case E:
                return Math.min(getPreferredW() + (backButton.getPreferredW() + backButton.getStyle().getHorizontalMargins())* 2, cnt.getWidth()) ;
            
  
        }
        return getPreferredW();
        
    }
    private static final int N=0;
    private static final int S=1;
    private static final int E=2;
    private static final int W=3;
    private static final int C=4;
    
    
    private int getPositionInt() {
        String pos = getPosition();
        if (BorderLayout.NORTH.equals(pos)) {
            return N;
        }
        if (BorderLayout.SOUTH.equals(pos)) {
            return S;
        }
        if (BorderLayout.EAST.equals(pos)) {
            return E;
        }
        if (BorderLayout.WEST.equals(pos)) {
            return W;
        }
        if (BorderLayout.CENTER.equals(pos)) {
            return C;
        }
        return S;
    }
    
    /**
     * Gets the preferred height of the sheet.  Will depend on where it is rendered.  If position is CENTER, NORTH, or SOUTH
     * then the preferred height will be the natural preferred width of the sheet.  But WEST or EAST,
     * the preferred height will be the full container height.
     * 
     * @param cnt The container in the FormLayeredPane where the sheet is to be rendered.
     * @return 
     */
    private int getPreferredH(Container cnt) {
        switch(getPositionInt()) {
            case W:
            case E:
                return cnt.getHeight();
            default:
                return Math.min(getPreferredH(), cnt.getHeight());
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
            fireBackEvent();
            this.parentSheet.show(duration);
        } else {
            hide(duration);
        }
    }
    

    
    
    private void hide(int duration) {
        final Container cnt = CN.getCurrentForm().getFormLayeredPane(Sheet.class, true);
        setX(getHiddenX(cnt));
        setY(getHiddenY(cnt));
        cnt.animateUnlayout(duration, 255, new Runnable() {
            public void run() {
                Container parent = cnt.getParent();
                
                if (parent != null && parent.getComponentForm() != null) {
                    cnt.remove();
                    parent.getComponentForm().revalidateLater();
                    fireCloseEvent(true);
                    
                    
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
        if (form != null && allowClose) {
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
    
    
    /**
     * Finds Sheet containing this component if it is currently part of a Sheet.
     * @param cmp The component to check.
     * @return The sheet containing the component, or null if it is not on a sheet.
     * @since 7.0
     */
    public static Sheet findContainingSheet(Component cmp) {
        Container parent = cmp.getParent();
        while (parent != null) {
            if (parent instanceof Sheet) {
                return (Sheet)parent;
            }
            parent = parent.getParent();
        }
        return null;
    }
    
    /**
     * Checks if the current sheet is an ancestor sheet of the given sheet.
     * @param sheet The sheet to check
     * @return True if the current sheet is an ancestor of sheet.
     * @since 7.0
     */
    public boolean isAncestorSheetOf(Sheet sheet) {
        sheet = sheet.getParentSheet();
        if (sheet == this) {
            return true;
        } else if (sheet == null) {
            return false;
        } else {
            return isAncestorSheetOf(sheet);
        }
    }
    
    /**
     * Adds listener notified when the sheet is closed.  This event is only fired
     * when the sheet is closed without the possibility of being reopened.  E.g. if a 
     * child sheet is opened (causing this sheet to be hidden), the close event won't be
     * fired until either that child sheet is hidden (without going back),
     * or the sheet itself is hidden, or goes back.
     * @param l 
     * @since 7.0
     */
    public void addCloseListener(ActionListener l) {
        closeListeners.addListener(l);
    }
    
    /**
     * Removes a close listener.
     * @param l The close listener
     */
    public void removeCloseListener(ActionListener l) {
        closeListeners.removeListener(l);
    }
    
    private void fireCloseEvent(boolean parentsToo) {
        closeListeners.fireActionEvent(new ActionEvent(this));
        if (parentsToo && parentSheet != null) {
            parentSheet.fireCloseEvent(true);
        }
    }
    
    /**
     * Adds listener to be notified when user goes back to the parent.  This is not
     * fired if the sheet is simply closed.  Only if the "back" button is pressed, 
     * 
     * @param l Listener
     * @since 7.0
     */
    public void addBackListener(ActionListener l) {
        backListeners.addListener(l);
    }
    
    /**
     * Removes a back listener.
     * @param l The close listener
     */
    public void removeBackListener(ActionListener l) {
        backListeners.removeListener(l);
    }
    
    private void fireBackEvent() {
        backListeners.fireActionEvent(new ActionEvent(this));
        
    }
    
    
    
    
    
}
