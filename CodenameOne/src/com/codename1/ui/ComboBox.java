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
package com.codename1.ui;

import com.codename1.cloud.BindTarget;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import java.util.Vector;

/**
 * A combo box is a list that allows only one selection at a time, when a user clicks
 * the combo box a popup button with the full list of elements allows the selection of
 * a single element. The combo box is driven by the list model and allows all the renderer
 * features of the List as well. 
 * 
 * @see List
 * @author Chen Fishbein
 */
public class ComboBox<T> extends List<T> {
    private static boolean defaultActAsSpinnerDialog;

    /**
     * When this flag is active the combo box acts as a button that opens a dialog that looks like a spinner
     * this allows creating user interfaces for touch devices where a spinner UI approach is more common than
     * a combo box paradigm.
     * @return the defaultActAsSpinnerDialog
     */
    public static boolean isDefaultActAsSpinnerDialog() {
        return defaultActAsSpinnerDialog;
    }

    /**
     * When this flag is active the combo box acts as a button that opens a dialog that looks like a spinner
     * this allows creating user interfaces for touch devices where a spinner UI approach is more common than
     * a combo box paradigm.
     * @param aDefaultActAsSpinnerDialog the defaultActAsSpinnerDialog to set
     */
    public static void setDefaultActAsSpinnerDialog(boolean aDefaultActAsSpinnerDialog) {
        defaultActAsSpinnerDialog = aDefaultActAsSpinnerDialog;
    }

    private boolean actAsSpinnerDialog = defaultActAsSpinnerDialog;

    /**
     * Indicates whethe the soft buttons for select/cancel should appear for the combo box by default
     */
    private static boolean defaultIncludeSelectCancel = true;

    /**
     * Indicates whethe the soft buttons for select/cancel should appear for the combo box
     */
    private boolean includeSelectCancel = defaultIncludeSelectCancel;

    /** 
     * Creates a new instance of ComboBox 
     * 
     * @param items set of items placed into the combo box model
     */
    public ComboBox(Vector<T> items) {
        this(new DefaultListModel(items));
    }

    /** 
     * Creates a new instance of ComboBox 
     * 
     * @param items set of items placed into the combo box model
     */
    public ComboBox(Object... items) {
        this(new DefaultListModel(items));
    }

    /** 
     * Constructs an empty combo box
     */
    public ComboBox() {
        this(new DefaultListModel<T>());
    }

    /**
     * Creates a new instance of ComboBox 
     * 
     * @param model the model for the combo box elements and selection
     */
    public ComboBox(ListModel<T> model) {
        super(model);
        super.setUIID("ComboBox");
        ((DefaultListCellRenderer) super.getRenderer()).setShowNumbers(false);
        setInputOnFocus(false);
        setIsScrollVisible(false);
        setFixedSelection(FIXED_NONE_CYCLIC);
        ListCellRenderer<T> r = getRenderer();
        if(r instanceof Component) {
            Component c = (Component) getRenderer();
            c.setUIID("ComboBoxItem");
        }
        Component c = getRenderer().getListFocusComponent(this);
        if(c != null){
            c.setUIID("ComboBoxFocus");
        }
    }

    /**
     * @inheritDoc
     */
    public void setUIID(String uiid) {
        super.setUIID(uiid);
        ListCellRenderer r = getRenderer();
        if(r instanceof Component) {
            Component c = (Component) getRenderer();
            c.setUIID(uiid + "Item");
        }
        Component c = getRenderer().getListFocusComponent(this);
        if(c != null){
            c.setUIID(uiid + "Focus");
        }
    }

    /**
     * @inheritDoc
     */
    public int getBaseline(int width, int height) {
        Component selected;
        if (getRenderingPrototype() != null) {
            selected = getRenderer().getListCellRendererComponent(this, getRenderingPrototype(), 0, true);
        }
        if (getModel().getSize() > 0) {
            selected = getRenderer().getListCellRendererComponent(this, getModel().getItemAt(0), 0, true);
        } else {
            selected = getRenderer().getListCellRendererComponent(this, "XXXXXXXXXXX", 0, true);
        }
        return getHeight() - getStyle().getPadding(false, BOTTOM) - selected.getStyle().getPadding(false, BOTTOM);
    }

    /**
     * @inheritDoc
     */
    protected void laidOut() {
    }

    /**
     * @inheritDoc
     */
    public Rectangle getSelectedRect() {
        // the implemenation from list doesn't make sense here, restore the component implementation
        return new Rectangle(getAbsoluteX(), getAbsoluteY(), getBounds().getSize());
    }

    /**
     * @inheritDoc
     */
    protected Rectangle getVisibleBounds() {
        return getBounds();
    }
    
    /**
     * @inheritDoc
     */
    public void setSelectedIndex(int selection) {
        super.setSelectedIndex(selection, false);
    }

    /**
     * @inheritDoc
     */
    public void setSelectedIndex(int selection, boolean scroll) {
        super.setSelectedIndex(selection, false);
    }

    /**
     * @inheritDoc
     */
    public void pointerHover(int[] x, int[] y) {
    }

    /**
     * @inheritDoc
     */
    public void pointerHoverReleased(int[] x, int[] y) {
    }

    /**
     * Subclasses can override this method to change the creation of the dialog
     *
     * @param l the list of the popup
     * @return a dialog instance
     */
    protected Dialog createPopupDialog(List<T> l) {
        Dialog popupDialog = new Dialog(getUIID() + "Popup", getUIID() + "PopupTitle"){

            void sizeChangedInternal(int w, int h) {
                //if only height changed it's the virtual keyboard, no need to
                //resize the popup just resize the parent form
                if(getWidth() == w && getHeight() != h){
                    Form frm = getPreviousForm();
                    if(frm != null){
                        frm.sizeChangedInternal(w, h);
                    }        
                    setSize(new Dimension(w, h));
                    repaint();
                }else{
                    dispose();
                }
            }
            
        };
        popupDialog.setScrollable(false);
        popupDialog.getContentPane().setAlwaysTensile(false);
        popupDialog.setAlwaysTensile(false);
        popupDialog.getContentPane().setUIID("PopupContentPane");
        popupDialog.setDisposeWhenPointerOutOfBounds(true);
        popupDialog.setTransitionInAnimator(CommonTransitions.createEmpty());
        popupDialog.setTransitionOutAnimator(CommonTransitions.createEmpty());
        popupDialog.setLayout(new BorderLayout());
        popupDialog.addComponent(BorderLayout.CENTER, l);
        return popupDialog;
    }

    /**
     * Shows the popup dialog for the combo box and returns the resulting command.
     * This method can be overriden by subclasses to modify the behavior of the class.
     *
     * @param popupDialog the popup dialog
     * @param l the list within
     * @return the selected command
     */
    protected Command showPopupDialog(Dialog popupDialog, List l) {
        if(getUIManager().isThemeConstant("popupTitleBool", false)) {
            if(getLabelForComponent() != null) {
                popupDialog.setTitle(getLabelForComponent().getText());
            }
        }

        if(includeSelectCancel) {
            popupDialog.setBackCommand(popupDialog.getMenuBar().getCancelMenuItem());
            if(Display.getInstance().isTouchScreenDevice()) {
                if(getUIManager().isThemeConstant("popupCancelBodyBool", false)) {
                    popupDialog.placeButtonCommands(new Command[] {popupDialog.getMenuBar().getCancelMenuItem()});
                }
            } else {
                if (Display.getInstance().isThirdSoftButton()) {
                    popupDialog.addCommand(popupDialog.getMenuBar().getSelectMenuItem());
                    popupDialog.addCommand(popupDialog.getMenuBar().getCancelMenuItem());
                } else {
                    popupDialog.addCommand(popupDialog.getMenuBar().getCancelMenuItem());
                    popupDialog.addCommand(popupDialog.getMenuBar().getSelectMenuItem());
                }
            }
        }
        
        if(actAsSpinnerDialog) {
            l.setFixedSelection(List.FIXED_CENTER);
            l.setUIID("Spinner");
            l.spinnerOverlay = getUIManager().getComponentStyle("SpinnerOverlay");
            l.spinnerOverlay.setMargin(0, 0, 0, 0);
            l.setAlwaysTensile(false);
            l.installDefaultPainter(l.spinnerOverlay);
            popupDialog.setDialogUIID("Container");
            popupDialog.setUIID("Container");
            popupDialog.getTitleComponent().setUIID("Container");
            popupDialog.setTransitionInAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, true, 200));
            popupDialog.setTransitionOutAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, false, 200));
            return popupDialog.show(Display.getInstance().getDisplayHeight() - popupDialog.getDialogComponent().getPreferredH(), 0, 0, 0, true, true);
        }

        if(getUIManager().isThemeConstant("centeredPopupBool", false)) {
            return popupDialog.showPacked(BorderLayout.CENTER, true);
        } else {
            int top, bottom, left, right;
            Form parentForm = getComponentForm();

            int listW = Math.max(getWidth() , l.getPreferredW());
            listW = Math.min(listW + l.getSideGap(), parentForm.getContentPane().getWidth());


            Component content = popupDialog.getDialogComponent();
            Style contentStyle = content.getStyle();

            int listH = content.getPreferredH()
                    + contentStyle.getMargin(false, TOP)
                    + contentStyle.getMargin(false, BOTTOM);

            Component title = popupDialog.getTitleArea();
            listH += title.getPreferredH()
                    + title.getStyle().getMargin(false, TOP)
                    + title.getStyle().getMargin(false, BOTTOM);

            bottom = 0;
            top = getAbsoluteY();
            int formHeight = parentForm.getHeight();
            if(parentForm.getSoftButtonCount() > 1) {
                Component c = parentForm.getSoftButton(0).getParent();
                formHeight -= c.getHeight();
                Style s = c.getStyle();
                formHeight -= (s.getMargin(TOP) + s.getMargin(BOTTOM));
            }

            if(listH < formHeight) {
                // pop up or down?
                if(top > formHeight / 2) {
                    bottom = formHeight - top;
                    top = top - listH;
                } else {
                    top +=  getHeight();
                    bottom = formHeight - top - listH;
                }
            } else {
                top = 0;
            }

            left = getAbsoluteX();
            right = parentForm.getWidth() - left - listW;
            if(right < 0) {
                left += right;
                right = 0;
            }
            popupDialog.setBackCommand(popupDialog.getMenuBar().getCancelMenuItem());
            return popupDialog.show(Math.max(top, 0),
                    Math.max(bottom, 0),
                    Math.max(left, 0),
                    Math.max(right, 0), false, true);
        }
    }

    /**
     * @inheritDoc
     */
    protected void fireClicked() {
        List<T> l = createPopupList();
        l.dispatcher = dispatcher;
        l.eventSource = this;
        l.disposeDialogOnSelection = true;
        Form parentForm = getComponentForm();
        //l.getSelectedStyle().setBorder(null);

        int tint = parentForm.getTintColor();
        parentForm.setTintColor(0);
        Dialog popupDialog = createPopupDialog(l);
        int originalSel = getSelectedIndex();
        Form.comboLock = includeSelectCancel;
        Command result = showPopupDialog(popupDialog, l);
        Form.comboLock = false;
        parentForm.setTintColor(tint);
        if(result == popupDialog.getMenuBar().getCancelMenuItem()) {
            setSelectedIndex(originalSel);
        }
    }

    /**
     * Creates the list object used within the popup dialog. This method allows subclasses
     * to customize the list creation for the popup dialog shown when the combo box is pressed.
     * 
     * @return a newly created list object used when the user presses the combo box.
     */
    protected List<T> createPopupList() {
        List<T> l = new List<T>(getModel());
        l.setCommandList(isCommandList());
        l.setSmoothScrolling(isSmoothScrolling());
        l.setFixedSelection(getFixedSelection());
        l.setListCellRenderer(getRenderer());
        l.setItemGap(getItemGap());
        l.setUIID("ComboBoxList");
        if(getUIManager().isThemeConstant("otherPopupRendererBool", false)) {
            DefaultListCellRenderer renderer = new DefaultListCellRenderer();
            renderer.setUIID("PopupItem");
            renderer.getListFocusComponent(l).setUIID("PopupFocus");
            l.setListCellRenderer(renderer);
        }

        return l;
    }


    /**
     * @inheritDoc
     */
    public void keyReleased(int keyCode) {
        // other events are in keyReleased to prevent the next event from reaching the next form
        int gameAction = Display.getInstance().getGameAction(keyCode);
        if (gameAction == Display.GAME_FIRE) {
            fireClicked();
            return;
        }
        super.keyPressed(keyCode);
    }

    /**
     * Prevent the combo box from losing selection in some use cases
     */
    void selectElement(int selectedIndex) {
    }

    /**
     * @inheritDoc
     */
    public void pointerPressed(int x, int y) {
    }

    /**
     * @inheritDoc
     */
    public void pointerDragged(int x, int y) {
    }

    
    /**
     * @inheritDoc
     */
    public void pointerReleased(int x, int y) {
        if(isEnabled()) {
            fireClicked();
        }
    }

    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
        getUIManager().getLookAndFeel().drawComboBox(g, this);
    }

    /**
     * @inheritDoc
     */
    protected Dimension calcPreferredSize() {
        return getUIManager().getLookAndFeel().getComboBoxPreferredSize(this);
    }

    /**
     * @inheritDoc
     */
    public int getOrientation() {
        return COMBO;
    }

    /**
     * Indicates whethe the soft buttons for select/cancel should appear for the combo box
     *
     * @return true if the soft buttons for select/cancel should appear for the combo box
     */
    public boolean isIncludeSelectCancel() {
        return includeSelectCancel;
    }

    /**
     * Indicates whethe the soft buttons for select/cancel should appear for the combo box
     *
     * @param includeSelectCancel the new value
     */
    public void setIncludeSelectCancel(boolean includeSelectCancel) {
        this.includeSelectCancel = includeSelectCancel;
    }


    /**
     * Indicates whethe the soft buttons for select/cancel should appear for the combo box by default
     *
     * @return true if the soft buttons for select/cancel should appear for the combo box
     */
    public static boolean isDefaultIncludeSelectCancel() {
        return defaultIncludeSelectCancel;
    }

    /**
     * Indicates whethe the soft buttons for select/cancel should appear for the combo box by default
     *
     * @param aDefaultIncludeSelectCancel  the new value
     */
    public static void setDefaultIncludeSelectCancel(boolean aDefaultIncludeSelectCancel) {
        defaultIncludeSelectCancel = aDefaultIncludeSelectCancel;
    }

    /**
     * When this flag is active the combo box acts as a button that opens a dialog that looks like a spinner
     * this allows creating user interfaces for touch devices where a spinner UI approach is more common than
     * a combo box paradigm.
     * @return the actAsSpinnerDialog
     */
    public boolean isActAsSpinnerDialog() {
        return actAsSpinnerDialog;
    }

    /**
     * When this flag is active the combo box acts as a button that opens a dialog that looks like a spinner
     * this allows creating user interfaces for touch devices where a spinner UI approach is more common than
     * a combo box paradigm.
     * @param actAsSpinnerDialog the actAsSpinnerDialog to set
     */
    public void setActAsSpinnerDialog(boolean actAsSpinnerDialog) {
        this.actAsSpinnerDialog = actAsSpinnerDialog;
    }
}
