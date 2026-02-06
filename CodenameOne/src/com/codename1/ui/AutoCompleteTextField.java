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
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.FilterProxyListModel;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;

/// An editable `com.codename1.ui.TextField` with completion suggestions
/// that show up in a drop down menu while the user types in text.
///
/// This class uses the "`TextField`" UIID by default as well as "`AutoCompletePopup`" &
/// "`AutoCompleteList`" for the popup list details.
///
/// The sample below shows the more trivial use case for this widget:
///
/// ```java
/// Form hi = new Form("Auto Complete", new BoxLayout(BoxLayout.Y_AXIS));
/// AutoCompleteTextField ac = new AutoCompleteTextField("Short", "Shock", "Sholder", "Shrek");
/// ac.setMinimumElementsShownInPopup(5);
/// hi.add(ac);
/// ```
///
/// The following sample shows more dynamic usage of the class where the auto-complete model is mutated
/// based on webservice results.
///
/// ```java
/// public void showForm() {
///   final DefaultListModel options = new DefaultListModel<>();
///   AutoCompleteTextField ac = new AutoCompleteTextField(options) {
/// @Override
///       protected boolean filter(String text) {
///           if(text.length() == 0) {
///               return false;
///           }
///           String[] l = searchLocations(text);
///           if(l == null || l.length == 0) {
///               return false;
///           }
///
///           options.removeAll();
///           for(String s : l) {
///               options.addItem(s);
///           }
///           return true;
///       }
///
///   };
///   ac.setMinimumElementsShownInPopup(5);
///   hi.add(ac);
///   hi.add(new SpanLabel("This demo requires a valid google API key to be set below "
///            + "you can get this key for the webservice (not the native key) by following the instructions here: "
///            + "https://developers.google.com/places/web-service/get-api-key"));
///   hi.add(apiKey);
///   hi.getToolbar().addCommandToRightBar("Get Key", null, e -> Display.getInstance().execute("https://developers.google.com/places/web-service/get-api-key"));
///   hi.show();
/// }
///
/// TextField apiKey = new TextField();
///
/// String[] searchLocations(String text) {
///     try {
///         if(text.length() > 0) {
///             ConnectionRequest r = new ConnectionRequest();
///             r.setPost(false);
///             r.setUrl("https://maps.googleapis.com/maps/api/place/autocomplete/json");
///             r.addArgument("key", apiKey.getText());
///             r.addArgument("input", text);
///             NetworkManager.getInstance().addToQueueAndWait(r);
///             Map result = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
///             String[] res = Result.fromContent(result).getAsStringArray("//description");
///             return res;
///         }
///     } catch(Exception err) {
///         Log.e(err);
///     }
///     return null;
/// }
/// ```
/// @author Chen
public class AutoCompleteTextField extends TextField {

    public static final int POPUP_POSITION_AUTO = 0;
    public static final int POPUP_POSITION_OVER = 1;
    public static final int POPUP_POSITION_UNDER = 2;
    private final Container popup;
    private final ActionListener listener = new FormPointerListener();
    private final ActionListener pressListener = new FormPointerPressListener();
    private final ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
    boolean pressInBounds;
    private boolean dontCalcSize = false;
    private FilterProxyListModel<String> filter;
    private ListCellRenderer completionRenderer;
    private String pickedText;
    private int minimumLength;
    private int popupPosition = POPUP_POSITION_AUTO;
    /// The number of elements shown for the auto complete popup
    private int minimumElementsShownInPopup = -1;

    /// Constructor with completion suggestions
    ///
    /// #### Parameters
    ///
    /// - `completion`: a String array of suggestion for completion
    public AutoCompleteTextField(String... completion) {
        this(new DefaultListModel<String>(completion));
    }

    /// Constructor with completion suggestions, filtering is automatic in this case
    ///
    /// #### Parameters
    ///
    /// - `listModel`: a list model containing potential string suggestions
    public AutoCompleteTextField(ListModel<String> listModel) {
        popup = new Container(new BoxLayout(BoxLayout.Y_AXIS)) {

            @Override
            public void setShouldCalcPreferredSize(boolean shouldCalcPreferredSize) {
                if (dontCalcSize) {
                    return;
                }
                super.setShouldCalcPreferredSize(shouldCalcPreferredSize);
            }

            @Override
            public void refreshTheme(boolean merge) {
            }

        };
        popup.setOwner(this);
        filter = new FilterProxyListModel<String>(listModel);
        popup.setScrollable(false);
        popup.setUIID("AutoCompletePopup");
        setConstraint(TextArea.NON_PREDICTIVE);
        addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popup.isVisible()) {
                    popup.setVisible(false);
                    popup.setEnabled(false);
                    Form f = getComponentForm();
                    if (f != null) {
                        f.revalidateLater();
                    }
                }
            }
        });

    }

    /// The default constructor is useful for cases of filter subclasses overriding the
    /// getSuggestionModel value as well as for the GUI builder
    public AutoCompleteTextField() {
        this(new DefaultListModel(""));
    }

    /// {@inheritDoc}
    @Override
    protected void initComponent() {
        super.initComponent();
        getComponentForm().addPointerPressedListener(pressListener);
        getComponentForm().addPointerReleasedListener(listener);
        Display.getInstance().callSerially(new Runnable() {

            @Override
            public void run() {
                addPopup(true);
            }
        });
    }

    /// {@inheritDoc}
    @Override
    protected void deinitialize() {
        super.deinitialize();
        getComponentForm().removePointerPressedListener(pressListener);
        getComponentForm().removePointerReleasedListener(listener);
        Display.getInstance().callSerially(new Runnable() {

            @Override
            public void run() {
                removePopup();
            }
        });
    }

    /// Causes the popup UI to show
    public void showPopup() {
        if (shouldShowPopup()) {
            pressInBounds = true;
            requestFocus();
            int m = minimumLength;
            minimumLength = 0;
            setTextImpl(getText(), true);
            minimumLength = m;

        }
    }

    void setParentText(String text) {
        super.setText(text);
    }

    /// {@inheritDoc}
    @Override
    public void setText(String text) {
        setTextImpl(text, false);
    }

    private void setTextImpl(String text, boolean forceUpdate) {
        String old = getText();
        super.setText(text);
        if (!forceUpdate) {
            if (text == null || text.equals(old) || (pickedText != null && pickedText.equals(text))) {
                pickedText = null;
                return;
            }
        }
        pickedText = null;
        Form f = getComponentForm();
        if (f != null && filterImpl(text)) {
            updateFilterList();
        }
    }

    /// In a case of an asynchronous filter this method can be invoked to refresh the completion list
    protected void updateFilterList() {
        Form f = getComponentForm();
        boolean v = filter.getSize() > 0 && getText().length() >= minimumLength;
        if (v != popup.isVisible()) {
            if (popup.getComponentCount() > 0) {
                popup.getComponentAt(0).setScrollY(0);
            }
            if (v && popup.getAbsoluteX() != getAbsoluteX()) {
                removePopup();
                addPopup(false);
            }
            popup.setVisible(v);
            popup.setEnabled(v);
            f.revalidate();
        }
        if (v && popup.getComponentCount() > 0) {
            int popupHeight = calcPopupHeight((List) popup.getComponentAt(0));
            popup.setHeight(popupHeight);
            dontCalcSize = false;
            popup.forceRevalidate();
            dontCalcSize = true;
        }
        if (f != null) {
            f.revalidate();
        }
        if (f != null) {
            dontCalcSize = false;
            f.revalidate();
            dontCalcSize = true;
        }

    }

    /// Subclasses can override this method to perform more elaborate filter operations
    ///
    /// #### Parameters
    ///
    /// - `text`: the text to filter
    ///
    /// #### Returns
    ///
    /// true if the filter has changed the list, false if it hasn't or is working asynchronously
    protected boolean filter(String text) {
        if (filter != null) {
            filter.filter(text);
            return true;
        }
        return false;
    }

    private boolean filterImpl(String text) {
        boolean res = filter(text);
        if (filter != null && popup != null) {
            boolean v = filter.getSize() > 0 && text.length() >= minimumLength;
            if (v != popup.isVisible() && popup.getComponentCount() > 0) {
                popup.getComponentAt(0).setScrollY(0);
                if (v && popup.getAbsoluteX() != getAbsoluteX()) {
                    removePopup();
                    addPopup(false);
                }
                popup.setVisible(v);
                popup.setEnabled(v);
            }
            Form f = getComponentForm();

            if (popup.getComponentCount() > 0) {
                int popupHeight = calcPopupHeight((List) popup.getComponentAt(0));
                popup.setHeight(popupHeight);
                dontCalcSize = false;
                popup.forceRevalidate();
                dontCalcSize = true;
            }
            if (f != null) {
                f.revalidate();
            }
        }
        return res;
    }

    /// Returns the list model to show within the completion list
    ///
    /// #### Returns
    ///
    /// the list model can be anything
    protected ListModel<String> getSuggestionModel() {
        return filter;
    }

    /// Sets a custom renderer to the completion suggestions list.
    ///
    /// #### Parameters
    ///
    /// - `completionRenderer`: a ListCellRenderer for the suggestions List
    public void setCompletionRenderer(ListCellRenderer completionRenderer) {
        this.completionRenderer = completionRenderer;
    }

    /// {@inheritDoc}
    @Override
    public void keyPressed(int k) {
        if (popup != null && popup.getParent() != null && popup.getComponentCount() > 0) {
            int game = Display.getInstance().getGameAction(k);
            if (game == Display.GAME_DOWN || game == Display.GAME_UP || game == Display.GAME_FIRE) {
                popup.getComponentAt(0).keyPressed(k);
                return;
            }
        }
        super.keyPressed(k);
    }

    /// {@inheritDoc}
    @Override
    public void keyReleased(int k) {
        if (popup != null && popup.getParent() != null && popup.getComponentCount() > 0) {
            int game = Display.getInstance().getGameAction(k);
            if (game == Display.GAME_DOWN || game == Display.GAME_UP || game == Display.GAME_FIRE) {
                popup.getComponentAt(0).keyReleased(k);
                return;
            }
        }
        super.keyReleased(k);
    }

    private void removePopup() {
        Form f = getComponentForm();
        if (f == null && popup != null) {
            f = popup.getComponentForm();
        }
        if (f != null) {
            Container lay = f.getLayeredPane(getClass(), true);
            Container parent = popup.getParent();
            if (parent != null) {
                lay.removeComponent(parent);
                popup.remove();
                f.revalidateLater();
            }

        }
    }

    /// Adds an action listener that fires an event when an entry in the auto-complete list is selected.
    /// Notice that this method will only take effect when the popup is reshown, if it is invoked when
    /// a popup is already showing it will have no effect.
    ///
    /// #### Parameters
    ///
    /// - `a`: the listener
    public void addListListener(ActionListener a) {
        listeners.add(a);
    }

    /// Removes an action listener that fires an event when an entry in the auto-complete list is selected.
    /// Notice that this method will only take effect when the popup is reshown, if it is invoked when
    /// a popup is already showing it will have no effect.
    ///
    /// #### Parameters
    ///
    /// - `a`: the listener
    public void removeListListener(ActionListener a) {
        listeners.remove(a);
    }

    private void addPopup(boolean updateFilter) {
        final Form f = getComponentForm();
        popup.removeAll();
        popup.setVisible(false);
        popup.setEnabled(false);
        if (updateFilter) {
            filter(getText());
        }
        final List l = new List(getSuggestionModel());
        if (getMinimumElementsShownInPopup() > 0) {
            l.setMinElementHeight(getMinimumElementsShownInPopup());
        }
        l.setScrollToSelected(false);
        l.setItemGap(0);
        for (ActionListener al : listeners) {
            l.addActionListener(al);
        }
        if (completionRenderer == null) {
            ((DefaultListCellRenderer<String>) l.getRenderer()).setShowNumbers(false);
        } else {
            l.setRenderer(completionRenderer);
        }
        l.setUIID("AutoCompleteList");
        l.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                if (shouldShowPopup()) {
                    pickedText = (String) l.getSelectedItem();
                    setParentText(pickedText);
                    fireActionEvent();
                    // relaunch text editing if we are still editing
                    if (Display.getInstance().isTextEditing(AutoCompleteTextField.this)) {
                        Display.getInstance().editString(AutoCompleteTextField.this, getMaxSize(), getConstraint(), (String) l.getSelectedItem());
                    }
                    popup.setVisible(false);
                    popup.setEnabled(false);
                    f.revalidate();
                }
            }
        });

        byte[] units = popup.getStyle().getMarginUnit();
        if (units != null) {
            units[Component.LEFT] = Style.UNIT_TYPE_PIXELS;
            units[Component.TOP] = Style.UNIT_TYPE_PIXELS;
            popup.getAllStyles().setMarginUnit(units);
        }

        int leftMargin = isRTL() ?
                Math.max(0, f.getWidth() - getAbsoluteX() - getWidth()) :
                Math.max(0, getAbsoluteX());

        popup.getAllStyles().setMargin(LEFT, leftMargin);

        int popupHeight = calcPopupHeight(l);

        popup.setPreferredW(getWidth());
        popup.setHeight(popupHeight);
        popup.setWidth(getWidth());

        popup.addComponent(l);
        popup.layoutContainer();
        //block the reflow of this popup, which can cause painting problems
        dontCalcSize = true;

        if (f != null) {
            if (popup.getParent() == null) {
                Container lay = f.getLayeredPane(getClass(), true);
                lay.setLayout(new LayeredLayout());
                Container wrapper = new Container();
                wrapper.add(popup);
                lay.addComponent(wrapper);
            }
            f.revalidate();
        }
    }

    /// Indicates the minimum length of text in the field in order for a popup to show
    /// the default is 0 where a popup is shown immediately for all text length if the number
    /// is 2 a popup will only appear when there are two characters or more.
    ///
    /// #### Returns
    ///
    /// the minimumLength
    public int getMinimumLength() {
        return minimumLength;
    }

    /// Indicates the minimum length of text in the field in order for a popup to show
    /// the default is 0 where a popup is shown immediately for all text length if the number
    /// is 2 a popup will only appear when there are two characters or more.
    ///
    /// #### Parameters
    ///
    /// - `minimumLength`: the minimumLength to set
    public void setMinimumLength(int minimumLength) {
        this.minimumLength = minimumLength;
    }

    /// The number of elements shown for the auto complete popup
    ///
    /// #### Returns
    ///
    /// the minimumElementsShownInPopup
    public int getMinimumElementsShownInPopup() {
        return minimumElementsShownInPopup;
    }

    /// The number of elements shown for the auto complete popup
    ///
    /// #### Parameters
    ///
    /// - `minimumElementsShownInPopup`: the minimumElementsShownInPopup to set
    public void setMinimumElementsShownInPopup(int minimumElementsShownInPopup) {
        this.minimumElementsShownInPopup = minimumElementsShownInPopup;
    }

    /// Set the autocomplete popup position in respect of the text field;
    /// POPUP_POSITION_AUTO is the default and it means that the popup is placed
    /// according to the available space.
    ///
    /// #### Parameters
    ///
    /// - `popupPosition`: @param popupPosition on of POPUP_POSITION_AUTO, POPUP_POSITION_OVER,
    /// POPUP_POSITION_UNDER
    public void setPopupPosition(int popupPosition) {
        this.popupPosition = popupPosition;
    }

    private int calcPopupHeight(List l) {
        int y = getAbsoluteY();
        int topMargin;
        int popupHeight;
        int items = l.getModel().getSize();
        final Form f = getComponentForm();
        if (f == null) {
            // for some reason this happens in the GUI builder
            return 10;
        }
        if (l.getModel() instanceof FilterProxyListModel) {
            items = ((FilterProxyListModel) l.getModel()).getUnderlying().getSize();
        }
        int listHeight = items * l.getElementSize(false, true).getHeight();
        if (popupPosition == POPUP_POSITION_UNDER || popupPosition == POPUP_POSITION_AUTO && y < f.getContentPane().getHeight() / 2) {
            topMargin = y - f.getTitleArea().getHeight() + getHeight();
            popupHeight = Math.min(listHeight, f.getContentPane().getHeight() / 2);
        } else {
            popupHeight = Math.min(listHeight, f.getContentPane().getHeight() / 2);
            popupHeight = Math.min(popupHeight, y - f.getTitleArea().getHeight());
            topMargin = y - f.getTitleArea().getHeight() - popupHeight;
        }
        popup.getAllStyles().setMargin(TOP, Math.max(0, topMargin));
        popup.setPreferredH(popupHeight);
        return popupHeight;
    }

    /// Callback that allows subclasses to block the popup from showing
    ///
    /// #### Returns
    ///
    /// true to allow the popup if applicable, false to block it
    protected boolean shouldShowPopup() {
        return true;
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyNames() {
        return new String[]{"completion"};
    }

    /// {@inheritDoc}
    @Override
    public Class[] getPropertyTypes() {
        return new Class[]{com.codename1.impl.CodenameOneImplementation.getStringArrayClass()};
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyTypeNames() {
        return new String[]{"String[]"};
    }

    /// {@inheritDoc}
    @Override
    public Object getPropertyValue(String name) {
        if ("completion".equals(name)) {
            return getCompletion();
        }
        return null;
    }

    /// Returns the completion values
    ///
    /// #### Returns
    ///
    /// array of completion entries
    public String[] getCompletion() {
        String[] r = new String[filter.getUnderlying().getSize()];
        int rlen = r.length;
        for (int iter = 0; iter < rlen; iter++) {
            r[iter] = (String) filter.getUnderlying().getItemAt(iter);
        }
        return r;
    }

    /// Sets the completion values
    ///
    /// #### Parameters
    ///
    /// - `completion`: the completion values
    public void setCompletion(String... completion) {
        filter = new FilterProxyListModel<String>(new DefaultListModel<String>(completion));
    }

    /// {@inheritDoc}
    @Override
    public String setPropertyValue(String name, Object value) {
        if ("completion".equals(name)) {
            filter = new FilterProxyListModel<String>(new DefaultListModel<String>((String[]) value));
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /// When enabled this makes the filter check that the string starts with rather than within the index
    ///
    /// #### Returns
    ///
    /// the startsWithMode
    public boolean isStartsWithMode() {
        return filter.isStartsWithMode();
    }

    /// When enabled this makes the filter check that the string starts with rather than within the index
    ///
    /// #### Parameters
    ///
    /// - `startsWithMode`: the startsWithMode to set
    public void setStartsWithMode(boolean startsWithMode) {
        filter.setStartsWithMode(startsWithMode);
    }

    class FormPointerPressListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            pressInBounds = false;
            final Form f = getComponentForm();
            Container layered = f.getLayeredPane(AutoCompleteTextField.this.getClass(), true);

            for (int i = 0; i < layered.getComponentCount(); i++) {
                Container wrap = (Container) layered.getComponentAt(i);
                Component pop = wrap.getComponentAt(0);
                if (pop.isVisible()) {
                    if (pop.contains(evt.getX(), evt.getY())) {
                        pressInBounds = true;
                    }
                }
            }
        }
    }

    class FormPointerListener implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final Form f = getComponentForm();
            Container layered = f.getLayeredPane(AutoCompleteTextField.this.getClass(), true);

            boolean canOpenPopup = shouldShowPopup();

            for (int i = 0; i < layered.getComponentCount(); i++) {
                Container wrap = (Container) layered.getComponentAt(i);
                Component pop = wrap.getComponentAt(0);
                if (pop.isVisible()) {
                    if (!pressInBounds && !pop.contains(evt.getX(), evt.getY())) {
                        pop.setVisible(false);
                        pop.setEnabled(false);
                        f.revalidateLater();
                        evt.consume();
                    } else {
                        canOpenPopup = false;
                    }
                }
            }

            if (!canOpenPopup || getText().length() < getMinimumLength()) {
                return;
            }

            if (contains(evt.getX(), evt.getY())) {
                //if the suggestions are empty don't show the no need to show the popup
                if (popup.getComponentCount() == 0) {
                    return;
                }
                if (((List) popup.getComponentAt(0)).getModel().getSize() == 0) {
                    return;
                }
                //something went wrong re-init the popup
                if (popup.getAbsoluteX() != getAbsoluteX()) {
                    removePopup();
                    addPopup(true);
                }
                evt.consume();
                popup.getComponentAt(0).setScrollY(0);
                popup.setVisible(true);
                popup.setEnabled(true);
                popup.revalidate();
                dontCalcSize = false;
                f.revalidate();
                dontCalcSize = true;
                Display.getInstance().callSerially(new Runnable() {

                    @Override
                    public void run() {
                        pointerReleased(evt.getX(), evt.getY());
                    }
                });
            }
        }
    }
}
