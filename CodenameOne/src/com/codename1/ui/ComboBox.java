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

import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionSource;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.util.Vector;

/// A `ComboBox` is a list that allows only one selection at a time, when a user clicks
/// the `ComboBox` a popup button with the full list of elements allows the selection of
/// a single element. The `ComboBox` is driven by the list model and allows all the renderer
/// features of the List as well.
///
/// The `ComboBox` is notoriously hard to style properly as it relies on a complex dynamic of
/// popup renderer and instantly visible renderer. The UIID for the `ComboBox`  is "`ComboBox`"
/// however if you set it to something else all the other UIID's will also change their prefix. E.g. the "`ComboBoxPopup`"
/// UIID will become "`MyNewUIIDPopup`".
///
/// The combo box defines the following UIID's by default:
///
///
/// - `ComboBox`
///
/// - `ComboBoxItem`
///
/// - `ComboBoxFocus`
///
/// - `PopupContentPane`
///
/// - `PopupItem`
///
/// - `PopupFocus`
///
/// This class also defines theme constants that allow some native themes to manipulate its behavior e.g.:
///
///
/// - popupTitleBool - shows the "label for" value as the title of the popup dialog
///
/// - popupCancelBodyBool - Adds a cancel button into the popup dialog
///
/// - centeredPopupBool - shows the popup dialog in the center of the screen instead of under the popup
///
/// - otherPopupRendererBool - Uses a different list cell render for the popup than the one used for the `ComboBox`
/// itself. When this is false `PopupItem` & `PopupFocus`  become irrelevant. Notice that the
/// Android native theme defines this to true.
///
/// **iOS doesn't use combo boxes as part of its UI paradigm**. Its available there mostly in web applications and feels
/// unnatural in iOS which is why we recommend using the `com.codename1.ui.spinner.Picker` class.
///
/// The sample code below uses the `com.codename1.ui.list.GenericListCellRenderer` to create a richer
/// `ComboBox` UI.
///
/// ```java
/// public void showForm() {
///   Form hi = new Form("ComboBox", new BoxLayout(BoxLayout.Y_AXIS));
///   ComboBox> combo = new ComboBox<> (
///           createListEntry("A Game of Thrones", "1996"),
///           createListEntry("A Clash Of Kings", "1998"),
///           createListEntry("A Storm Of Swords", "2000"),
///           createListEntry("A Feast For Crows", "2005"),
///           createListEntry("A Dance With Dragons", "2011"),
///           createListEntry("The Winds of Winter", "2016 (please, please, please)"),
///           createListEntry("A Dream of Spring", "Ugh"));
///
///   combo.setRenderer(new GenericListCellRenderer<>(new MultiButton(), new MultiButton()));
///   hi.show();
/// }
///
/// private Map createListEntry(String name, String date) {
///     Map entry = new HashMap<>();
///     entry.put("Line1", name);
///     entry.put("Line2", date);
///     return entry;
/// }
/// ```
///
/// @author Chen Fishbein
///
/// #### See also
///
/// - List
public class ComboBox<T> extends List<T> implements ActionSource {

    private static boolean defaultActAsSpinnerDialog;
    /// Indicates whethe the soft buttons for select/cancel should appear for the combo box by default
    private static boolean defaultIncludeSelectCancel = true;
    private Image comboBoxImage;
    private boolean actAsSpinnerDialog = defaultActAsSpinnerDialog;
    /// Indicates whethe the soft buttons for select/cancel should appear for the combo box
    private boolean includeSelectCancel = defaultIncludeSelectCancel;
    private boolean showingPopupDialog;

    /// Creates a new instance of ComboBox
    ///
    /// #### Parameters
    ///
    /// - `items`: set of items placed into the combo box model
    public ComboBox(Vector<T> items) {
        this(new DefaultListModel(items));
    }

    /// Creates a new instance of ComboBox
    ///
    /// #### Parameters
    ///
    /// - `items`: set of items placed into the combo box model
    public ComboBox(Object... items) {
        this(new DefaultListModel(items));
    }

    /// Constructs an empty combo box
    public ComboBox() {
        this(new DefaultListModel<T>());
    }

    /// Creates a new instance of ComboBox
    ///
    /// #### Parameters
    ///
    /// - `model`: the model for the combo box elements and selection
    public ComboBox(ListModel<T> model) {
        super(model);
        setUIIDFinal("ComboBox");
        ((DefaultListCellRenderer) super.getRenderer()).setShowNumbers(false);

        setInputOnFocus(false);
        setIsScrollVisible(false);
        setFixedSelection(FIXED_NONE_CYCLIC);
        ListCellRenderer<T> r = getRenderer();
        if (r instanceof Component) {
            Component c = (Component) getRenderer();
            c.setUIID("ComboBoxItem");
        }
        Component c = getRenderer().getListFocusComponent(this);
        if (c != null) {
            c.setUIID("ComboBoxFocus");
        }

        if (UIManager.getInstance().isThemeConstant("comboBoxUseMaterialArrowDropDownBool", false)) {
            Style comboImageStyle = new Style(getStyle());
            comboImageStyle.setBgTransparency(0);
            setComboBoxImage(FontImage.createMaterial(FontImage.MATERIAL_ARROW_DROP_DOWN, comboImageStyle));
        }

    }

    /// When this flag is active the combo box acts as a button that opens a dialog that looks like a spinner
    /// this allows creating user interfaces for touch devices where a spinner UI approach is more common than
    /// a combo box paradigm.
    ///
    /// #### Returns
    ///
    /// the defaultActAsSpinnerDialog
    public static boolean isDefaultActAsSpinnerDialog() {
        return defaultActAsSpinnerDialog;
    }

    /// When this flag is active the combo box acts as a button that opens a dialog that looks like a spinner
    /// this allows creating user interfaces for touch devices where a spinner UI approach is more common than
    /// a combo box paradigm.
    ///
    /// #### Parameters
    ///
    /// - `aDefaultActAsSpinnerDialog`: the defaultActAsSpinnerDialog to set
    public static void setDefaultActAsSpinnerDialog(boolean aDefaultActAsSpinnerDialog) {
        defaultActAsSpinnerDialog = aDefaultActAsSpinnerDialog;
    }

    /// Indicates whethe the soft buttons for select/cancel should appear for the combo box by default
    ///
    /// #### Returns
    ///
    /// true if the soft buttons for select/cancel should appear for the combo box
    public static boolean isDefaultIncludeSelectCancel() {
        return defaultIncludeSelectCancel;
    }

    /// Indicates whethe the soft buttons for select/cancel should appear for the combo box by default
    ///
    /// #### Parameters
    ///
    /// - `aDefaultIncludeSelectCancel`: the new value
    public static void setDefaultIncludeSelectCancel(boolean aDefaultIncludeSelectCancel) {
        defaultIncludeSelectCancel = aDefaultIncludeSelectCancel;
    }

    /// Gets the ComboBox drop down icon
    ///
    /// #### Returns
    ///
    /// the drop down icon
    public Image getComboBoxImage() {
        return comboBoxImage;
    }

    /// Sets the ComboBox drop down icon
    ///
    /// #### Parameters
    ///
    /// - `comboBoxImage`: the drop down icon
    public void setComboBoxImage(Image comboBoxImage) {
        this.comboBoxImage = comboBoxImage;
    }

    /// {@inheritDoc}
    @Override
    public void setUIID(String uiid) {
        super.setUIID(uiid);
        ListCellRenderer r = getRenderer();
        if (r instanceof Component) {
            Component c = (Component) getRenderer();
            c.setUIID(uiid + "Item");
        }
        Component c = getRenderer().getListFocusComponent(this);
        if (c != null) {
            c.setUIID(uiid + "Focus");
        }
        if (UIManager.getInstance().isThemeConstant("comboBoxUseMaterialArrowDropDownBool", false)) {
            Style comboImageStyle = new Style(getStyle());
            comboImageStyle.setBgTransparency(0);
            setComboBoxImage(FontImage.createMaterial(FontImage.MATERIAL_ARROW_DROP_DOWN, comboImageStyle));
        }
    }

    /// {@inheritDoc}
    @Override
    public int getBaseline(int width, int height) {
        Component selected;
        if (getRenderingPrototype() != null) {
            selected = getRenderer().getListCellRendererComponent(this, getRenderingPrototype(), 0, true);
        } else if (getModel().getSize() > 0) {
            selected = getRenderer().getListCellRendererComponent(this, getModel().getItemAt(0), 0, true);
        } else {
            selected = getRenderer().getListCellRendererComponent(this, "XXXXXXXXXXX", 0, true);
        }
        return getHeight() - getStyle().getPaddingBottom() - selected.getStyle().getPaddingBottom();
    }

    /// {@inheritDoc}
    @Override
    protected void laidOut() {
    }

    /// {@inheritDoc}
    @Override
    public Rectangle getSelectedRect() {
        // the implemenation from list doesn't make sense here, restore the component implementation
        return new Rectangle(getAbsoluteX(), getAbsoluteY(), getBounds().getSize());
    }

    /// {@inheritDoc}
    @Override
    protected Rectangle getVisibleBounds() {
        return getBounds();
    }

    /// {@inheritDoc}
    @Override
    public void setSelectedIndex(int selection) {
        super.setSelectedIndex(selection, false);
    }

    /// {@inheritDoc}
    @Override
    public void setSelectedIndex(int selection, boolean scroll) {
        super.setSelectedIndex(selection, false);
    }

    /// {@inheritDoc}
    @Override
    public void pointerHover(int[] x, int[] y) {
    }

    /// {@inheritDoc}
    @Override
    public void pointerHoverReleased(int[] x, int[] y) {
    }

    /// Subclasses can override this method to change the creation of the dialog
    ///
    /// #### Parameters
    ///
    /// - `l`: the list of the popup
    ///
    /// #### Returns
    ///
    /// a dialog instance
    protected Dialog createPopupDialog(List<T> l) {
        Dialog popupDialog = new Dialog(getUIID() + "Popup", getUIID() + "PopupTitle") {

            @Override
            void sizeChangedInternal(int w, int h) {
                //if only height changed it's the virtual keyboard, no need to
                //resize the popup just resize the parent form
                if (getWidth() == w && getHeight() != h) {
                    Form frm = getPreviousForm();
                    if (frm != null) {
                        frm.sizeChangedInternal(w, h);
                    }
                    setSize(new Dimension(w, h));
                    repaint();
                } else {
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

    /// Returns true if the popup dialog is currently showing for this combobox.
    ///
    /// #### Since
    ///
    /// 8.0
    public boolean isShowingPopupDialog() {
        return showingPopupDialog;
    }

    /// Shows the popup dialog for the combo box and returns the resulting command.
    /// This method can be overriden by subclasses to modify the behavior of the class.
    ///
    /// #### Parameters
    ///
    /// - `popupDialog`: the popup dialog
    ///
    /// - `l`: the list within
    ///
    /// #### Returns
    ///
    /// the selected command
    protected Command showPopupDialog(Dialog popupDialog, List l) {
        if (getUIManager().isThemeConstant("popupTitleBool", false)) {
            if (getLabelForComponent() != null) {
                popupDialog.setTitle(getLabelForComponent().getText());
            }
        }

        if (includeSelectCancel) {
            popupDialog.setBackCommand(popupDialog.getMenuBar().getCancelMenuItem());
            if (Display.getInstance().isTouchScreenDevice()) {
                if (getUIManager().isThemeConstant("popupCancelBodyBool", false)) {
                    popupDialog.placeButtonCommands(new Command[]{popupDialog.getMenuBar().getCancelMenuItem()});
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

        if (actAsSpinnerDialog) {
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
            showingPopupDialog = true;
            Command out = popupDialog.show(Display.getInstance().getDisplayHeight() - popupDialog.getDialogComponent().getPreferredH(), 0, 0, 0, true, true);
            showingPopupDialog = false;
            return out;
        }

        if (getUIManager().isThemeConstant("centeredPopupBool", false)) {
            showingPopupDialog = true;
            Command out = popupDialog.showPacked(BorderLayout.CENTER, true);
            showingPopupDialog = false;
            return out;
        } else {
            int top;
            int bottom;
            int left;
            int right;
            Form parentForm = getComponentForm();

            int listW = Math.max(getWidth(), l.getPreferredW());
            listW = Math.min(listW + l.getSideGap(), parentForm.getContentPane().getWidth());


            Component content = popupDialog.getDialogComponent();
            Style contentStyle = content.getStyle();

            int listH = content.getPreferredH()
                    + contentStyle.getVerticalMargins();

            Component title = popupDialog.getTitleArea();
            listH += title.getPreferredH()
                    + title.getStyle().getVerticalMargins();

            bottom = 0;
            top = getAbsoluteY();
            int formHeight = parentForm.getHeight();
            if (parentForm.getSoftButtonCount() > 1) {
                Component c = parentForm.getSoftButton(0).getParent();
                formHeight -= c.getHeight();
                Style s = c.getStyle();
                formHeight -= (s.getVerticalMargins());
            }

            if (listH < formHeight) {
                // pop up or down?
                if (top > formHeight / 2) {
                    bottom = formHeight - top;
                    top = top - listH;
                } else {
                    top += getHeight();
                    bottom = formHeight - top - listH;
                }
            } else {
                top = 0;
            }

            left = getAbsoluteX();
            right = parentForm.getWidth() - left - listW;
            if (right < 0) {
                left += right;
                right = 0;
            }
            popupDialog.setBackCommand(popupDialog.getMenuBar().getCancelMenuItem());
            showingPopupDialog = true;
            Command out = popupDialog.show(Math.max(top, 0),
                    Math.max(bottom, 0),
                    Math.max(left, 0),
                    Math.max(right, 0), false, true);
            showingPopupDialog = false;
            return out;
        }
    }

    /// {@inheritDoc}
    @Override
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
        float rr = Dialog.getDefaultBlurBackgroundRadius();
        Dialog.setDefaultBlurBackgroundRadius(-1);
        Command result = showPopupDialog(popupDialog, l);
        Dialog.setDefaultBlurBackgroundRadius(rr);
        Form.comboLock = false;
        parentForm.setTintColor(tint);
        if (result == popupDialog.getMenuBar().getCancelMenuItem() || popupDialog.wasDisposedDueToOutOfBoundsTouch() || //NOPMD CompareObjectsWithEquals
                popupDialog.wasDisposedDueToRotation()) {
            setSelectedIndex(originalSel);
        }
    }

    /// Creates the list object used within the popup dialog. This method allows subclasses
    /// to customize the list creation for the popup dialog shown when the combo box is pressed.
    ///
    /// #### Returns
    ///
    /// a newly created list object used when the user presses the combo box.
    protected List<T> createPopupList() {
        List<T> l = new List<T>(getModel());
        l.setCommandList(isCommandList());
        l.setSmoothScrolling(isSmoothScrolling());
        l.setFixedSelection(getFixedSelection());
        l.setListCellRenderer(getRenderer());
        l.setItemGap(getItemGap());
        l.setUIID("ComboBoxList");
        if (getUIManager().isThemeConstant("otherPopupRendererBool", false)) {
            DefaultListCellRenderer renderer = new DefaultListCellRenderer();
            renderer.setUIID("PopupItem");
            renderer.getListFocusComponent(l).setUIID("PopupFocus");
            l.setListCellRenderer(renderer);
        }

        return l;
    }

    /// {@inheritDoc}
    @Override
    public void keyReleased(int keyCode) {
        // other events are in keyReleased to prevent the next event from reaching the next form
        int gameAction = Display.getInstance().getGameAction(keyCode);
        if (gameAction == Display.GAME_FIRE) {
            fireClicked();
            return;
        }
        super.keyPressed(keyCode);
    }

    /// Prevent the combo box from losing selection in some use cases
    @Override
    void selectElement(int selectedIndex) {
    }

    /// {@inheritDoc}
    @Override
    public void pointerPressed(int x, int y) {
    }

    /// {@inheritDoc}
    @Override
    public void pointerDragged(int x, int y) {
    }

    /// {@inheritDoc}
    @Override
    public void pointerReleased(int x, int y) {
        if (isEnabled() && !Display.impl.isScrollWheeling()) {
            fireClicked();
        }
    }

    /// {@inheritDoc}
    @Override
    public void paint(Graphics g) {
        getUIManager().getLookAndFeel().drawComboBox(g, this);
    }

    /// {@inheritDoc}
    @Override
    protected Dimension calcPreferredSize() {
        return getUIManager().getLookAndFeel().getComboBoxPreferredSize(this);
    }

    /// {@inheritDoc}
    @Override
    public int getOrientation() {
        return COMBO;
    }

    /// Indicates whethe the soft buttons for select/cancel should appear for the combo box
    ///
    /// #### Returns
    ///
    /// true if the soft buttons for select/cancel should appear for the combo box
    public boolean isIncludeSelectCancel() {
        return includeSelectCancel;
    }

    /// Indicates whethe the soft buttons for select/cancel should appear for the combo box
    ///
    /// #### Parameters
    ///
    /// - `includeSelectCancel`: the new value
    public void setIncludeSelectCancel(boolean includeSelectCancel) {
        this.includeSelectCancel = includeSelectCancel;
    }

    /// When this flag is active the combo box acts as a button that opens a dialog that looks like a spinner
    /// this allows creating user interfaces for touch devices where a spinner UI approach is more common than
    /// a combo box paradigm.
    ///
    /// #### Returns
    ///
    /// the actAsSpinnerDialog
    public boolean isActAsSpinnerDialog() {
        return actAsSpinnerDialog;
    }

    /// When this flag is active the combo box acts as a button that opens a dialog that looks like a spinner
    /// this allows creating user interfaces for touch devices where a spinner UI approach is more common than
    /// a combo box paradigm.
    ///
    /// #### Parameters
    ///
    /// - `actAsSpinnerDialog`: the actAsSpinnerDialog to set
    public void setActAsSpinnerDialog(boolean actAsSpinnerDialog) {
        this.actAsSpinnerDialog = actAsSpinnerDialog;
    }
}
