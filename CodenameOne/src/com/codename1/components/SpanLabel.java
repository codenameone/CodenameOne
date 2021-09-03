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
package com.codename1.components;

import static com.codename1.ui.CN.EAST;
import static com.codename1.ui.CN.WEST;
import com.codename1.ui.Component;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.IconHolder;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextHolder;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/**
 * <p>A multi line label component that can be easily localized, this is simply based
 * on a text area combined with a label.</p>
 * <script src="https://gist.github.com/codenameone/55b73c621fea0263638a.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-spanlabel.png" alt="SpanLabel Sample" />
 *
 * @author Shai Almog
 */
public class SpanLabel extends Container implements IconHolder, TextHolder {
    private Label icon;
    private int gap;
    private Container iconWrapper;
    private TextArea text;
    private boolean shouldLocalize = true;

    /**
     * Default constructor will be useful when adding this to the GUI builder
     */
    public SpanLabel() {
        this("");
    }

    /**
     * Constructor accepting default text and uiid for the text
     * @param txt the text
     * @param textUiid the new text UIID
     */
    public SpanLabel(String txt, String textUiid) {
        this(txt);
        text.setUIID(textUiid);

        updateGap();
    }

    /**
     * Constructor accepting default text
     */
    public SpanLabel(String txt) {
        setUIID("Container");
        setLayout(new BorderLayout());
        text = new TextArea(getUIManager().localize(txt, txt));
        text.setActAsLabel(true);
        text.setColumns(text.getText().length() + 1);
        text.setGrowByContent(true);
        text.setUIID("Label");
        text.setEditable(false);
        text.setFocusable(false);
        icon = new Label();
        icon.setUIID("icon");
        iconWrapper = new Container(new FlowLayout(CENTER, CENTER));
        iconWrapper.getAllStyles().stripMarginAndPadding();
        iconWrapper.add(icon);
        addComponent(BorderLayout.WEST, iconWrapper);
        addComponent(BorderLayout.CENTER, BoxLayout.encloseYCenter(text));
        updateGap();
    }

    @Override
    public void styleChanged(String propertyName, Style source) {
        super.styleChanged(propertyName, source);
        if (Style.ICON_GAP.equals(propertyName)) {
            int gap = source.getIconGap();
            if (gap >= 0 && gap != getGap()) {
                setGap(gap);
                updateGap();
            }
        }
    }

    @Override
    protected void initUnselectedStyle(Style unselectedStyle) {
        super.initUnselectedStyle(unselectedStyle);
        if (unselectedStyle.getIconGap() > 0) {
            int gap = unselectedStyle.getIconGap();
            if (gap != getGap()) {
                setGap(unselectedStyle.getIconGap());
                updateGap();
            }
        }
    }

    @Override
    protected void initLaf(UIManager uim) {
        if(uim == getUIManager() && isInitialized()){
            return;
        }
        super.initLaf(uim);
        String uiid = getUIID();
        if (uiid != null && uiid.length() > 0) {
            String iconUiid = uim.getIconUIIDFor(uiid);
            if (iconUiid != null) {
                setIconUIID(iconUiid);

            }
        }
    }

    private int preferredW=-1;

    /**
     * {@inheritDoc }
     */
    @Override
    public void setPreferredW(int preferredW) {
        //We need to override preferredW for this component because the default
        // implementation will prevent calcPreferredSize() from ever being called,
        // and we still want to calculate the preferred height based on this preferred width.
        this.preferredW = preferredW;
    }

    /**
     *
     * {@inheritDoc }
     */
    @Override
    public int getPreferredW() {
        if (preferredW != -1) {
            return preferredW;
        }
        return super.getPreferredW();
    }

    /**
     * Gets the component used for styling font icons on this SpanLabel.
     * @return The component used for styling font icons on this SpanLabel.
     * @since 7.0
     */
    public Component getIconStyleComponent() {
        return icon.getIconStyleComponent();
    }

    /**
     * Returns the TextArea holding the actual text
     * @return the component
     */
    public TextArea getTextComponent() {
        return text;
    }

    @Override
    public void setUIID(String id) {
        super.setUIID(id);
        if (id != null && id.length() > 0) {
            String iconUIID = getUIManager().getIconUIIDFor(id);
            if (iconUIID != null) {
                setIconUIID(iconUIID);
            }
        }
    }

    /**
     * Sets the UIID for the actual text
     * @param uiid the uiid
     */
    public void setTextUIID(String uiid) {
        text.setUIID(uiid);
    }

    /**
     * Returns the uiid of the actual text
     * @return the uiid
     */
    public String getTextUIID() {
        return text.getUIID();
    }

    /**
     * Returns the Style proxy object for the text of this span button.
     * @return The Style object for the text of this span button.
     */
    public Style getTextAllStyles() {
        return text.getAllStyles();
    }

    /**
     * Returns the text elements style object
     * @return the style object
     */
    public Style getTextUnselectedStyle() {
        return text.getUnselectedStyle();
    }

    /**
     * The text elements style object
     * @param t the style object
     */
    public void setTextUnselectedStyle(Style t) {
        text.setUnselectedStyle(t);
    }

    /**
     * Returns the text elements style object
     * @return the style object
     */
    public Style getTextSelectedStyle() {
        return text.getSelectedStyle();
    }

    /**
     * The text elements style object
     * @param t the style object
     */
    public void setTextSelectedStyle(Style t) {
        text.setSelectedStyle(t);
    }

    /**
     * Sets the uiid for the icon if present
     * @param uiid the uiid for the icon
     */
    public void setIconUIID(String uiid) {
        icon.setUIID(uiid);
        updateGap();
    }

    /**
     * Returns the UIID for the icon
     * @return the uiid
     */
    public String getIconUIID() {
        return icon.getUIID();
    }

    /**
     * Set the text of the label
     * @param t text of the label
     */
    public void setText(String t) {
        t = shouldLocalize ? getUIManager().localize(t, t) : t;
        text.setText(t);

        // We need to update the columns for rendering, otherwise it will still wrap at the old number of columns.
        text.setColumns(text.getText().length() + 1);
    }

    /**
     * Sets the icon for the label
     * @param i the icon
     */
    public void setIcon(Image i) {
        icon.setIcon(i);
        updateGap();
    }

    /**
     * Sets the vertical alignment of the icon with respect to the text of the span label.  Default value is {@link #CENTER}
     * @param align One of {@link #TOP}, {@link #BOTTOM}, or {@link #CENTER}.
     * @since 7.0
     */
    public void setIconValign(int align) {
        ((FlowLayout)iconWrapper.getLayout()).setValign(align);
    }

    /**
     * Gets the vertical alignment of the icon with respect to the text of the span label.
     * @return The alignment.  One of {@link #TOP}, {@link #BOTTOM}, or {@link #CENTER}.
     * @since 7.0
     */
    public int getIconValign() {
        return ((FlowLayout)iconWrapper.getLayout()).getValign();
    }

    /**
     * Returns the text of the label
     * @return the text
     */
    public String getText() {
        return text.getText();
    }

    /**
     * Indicates the alignment of the whole text block, this is different from setting the alignment of the text within
     * the block since the UIID might have a border or other design element that won't be affected by such alignment.
     * The default is none (-1) which means no alignment takes place and the text block takes the whole width.
     * @param align valid values are Component.LEFT, Component.RIGHT, Component.CENTER. Anything else will
     * stretch the text block
     */
    public void setTextBlockAlign(int align) {
        switch(align) {
            case LEFT:
            case RIGHT:
            case CENTER:
                wrapText(align);
                return;
            default:
                if(text.getParent() != this) {
                    removeComponent(text.getParent());
                    text.getParent().removeAll();
                    addComponent(BorderLayout.CENTER, BoxLayout.encloseYCenter(text));
                }
        }
    }

    /**
     * Returns the alignment of the whole text block and not the text within it!
     *
     * @return -1 for unaligned otherwise one of Component.LEFT/RIGHT/CENTER
     */
    public int getTextBlockAlign() {
        if(text.getParent() == this) {
            return -1;
        }
        return ((FlowLayout)text.getParent().getLayout()).getAlign();
    }

    private void wrapText(int alignment) {
        Container parent = text.getParent();
        if(parent == this) {
            parent.removeComponent(text);
            parent = new Container(new FlowLayout(alignment, CENTER));
            parent.getAllStyles().stripMarginAndPadding();
            parent.addComponent(text);
            addComponent(BorderLayout.CENTER, parent);
        } else if (parent.getLayout() instanceof BoxLayout) {
            removeComponent(parent);
            parent.removeComponent(text);
            parent = new Container(new FlowLayout(alignment, CENTER));
            parent.getAllStyles().stripMarginAndPadding();
            parent.addComponent(text);
            addComponent(BorderLayout.CENTER, parent);
        } else {
            ((FlowLayout)parent.getLayout()).setAlign(alignment);
        }
    }

    /**
     * Returns the image of the icon
     * @return the icon
     */
    public Image getIcon() {
        return icon.getIcon();
    }

    /**
     * Sets the icon position based on border layout constraints
     *
     * @param t position either North/South/East/West
     */
    public void setIconPosition(String t) {
        removeComponent(iconWrapper);
        addComponent(t, iconWrapper);
        updateGap();
        revalidateLater();

    }

    /**
     * Returns the icon position based on border layout constraints
     *
     * @return position either North/South/East/West
     */
    public String getIconPosition() {
        return (String)getLayout().getComponentConstraint(iconWrapper);
    }


    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {
                "text", "icon", "iconPosition", "textUiid", "iconUiid"
        };
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
        return new Class[] {
                String.class, // text
                Image.class, // icon
                String.class, // iconPosition
                String.class,
                String.class
        };
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"String", "Image", "String", "String", "String"};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("text")) {
            return getText();
        }
        if(name.equals("icon")) {
            return getIcon();
        }
        if(name.equals("iconPosition")) {
            return getIconPosition();
        }
        if(name.equals("textUiid")) {
            return getTextUIID();
        }
        if(name.equals("iconUiid")) {
            return getIconUIID();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("text")) {
            setText((String)value);
            return null;
        }
        if(name.equals("icon")) {
            setIcon((Image)value);
            return null;
        }
        if(name.equals("iconPosition")) {
            setIconPosition((String)value);
            return null;
        }
        if(name.equals("textUiid")) {
            setTextUIID((String)value);
            return null;
        }
        if(name.equals("iconUiid")) {
            setIconUIID((String)value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * Indicates if text should be localized when set to the component, by default
     * all text is localized so this allows disabling automatic localization for
     * a specific component.
     * @return the shouldLocalize value
     */
    public boolean isShouldLocalize() {
        return shouldLocalize;
    }

    /**
     * Indicates if text should be localized when set to the component, by default
     * all text is localized so this allows disabling automatic localization for
     * a specific component.
     * @param shouldLocalize the shouldLocalize to set
     */
    public void setShouldLocalize(boolean shouldLocalize) {
        this.shouldLocalize = shouldLocalize;
    }

    /**
     * Enables or disables text selection on this span label.  Default is off.
     * @param enabled True to enable text selection on this label.
     * @since 7.0
     */
    public void setTextSelectionEnabled(boolean enabled) {
        text.setTextSelectionEnabled(enabled);
    }

    /**
     * Checks if text selection is enabled on this SpanLabel.  Note that the Form must also have text selection enabled
     * for text selection to work.
     * @return True if text selection is enabled on this element.
     * @since 7.0
     * @see Form#getTextSelection()
     * @see TextSelection#setEnabled(boolean)
     */
    public boolean isTextSelectionEnabled() {
        return text.isTextSelectionEnabled();
    }




    /**
     * {@inheritDoc }
     *
     */
    @Override
    public void setWidth(int width) {
        int w = getWidth();
        if (w != width) {

            // We need to update the textarea width whenever we set the width
            // so that preferred height will be calculated correctly.
            int newTextW = width;
            String iconPos = getIconPosition();
            if (getIcon() != null && EAST.equals(iconPos) || WEST.equals(iconPos)) {
                newTextW -= iconWrapper.getOuterWidth();
            }
            newTextW -= getStyle().getHorizontalPadding();
            newTextW -= text.getStyle().getHorizontalMargins();
            text.setWidth(newTextW);

            super.setWidth(width);
            setShouldCalcPreferredSize(true);
        }

    }

    /**
     * {@inheritDoc }
     * @since 7.0
     *
     */
    @Override
    public void setGap(int gap) {
        if (gap != this.gap) {
            this.gap = gap;
            updateGap();
        }
    }

    /**
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public int getGap() {
        return gap;
    }

    /**
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public void setTextPosition(int textPosition) {
        switch (textPosition) {
            case Component.TOP:
                setIconPosition(BorderLayout.SOUTH);
                break;
            case Component.BOTTOM:
                setIconPosition(BorderLayout.NORTH);
                break;
            case Component.LEFT:
                setIconPosition(BorderLayout.EAST);
                break;
            case Component.RIGHT:
                setIconPosition(BorderLayout.WEST);
                break;
            default:
                setIconPosition(BorderLayout.EAST);
        }

    }

    /**
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public int getTextPosition() {
        String iconPosition = getIconPosition();
        if (BorderLayout.NORTH.equals(iconPosition)) {
            return Component.BOTTOM;
        }
        if (BorderLayout.SOUTH.equals(iconPosition)) {
            return Component.TOP;
        }
        if (BorderLayout.EAST.equals(iconPosition)) {
            return Component.LEFT;
        }
        if (BorderLayout.WEST.equals(iconPosition)) {
            return Component.RIGHT;
        }
        return Component.LEFT;

    }


    private void updateGap() {
        if (getIcon() == null) {
            $(icon).setMargin(0);
        } else if (BorderLayout.NORTH.equals(getIconPosition())) {
            $(icon).selectAllStyles().setMargin(0, 0, gap, 0);
        } else if (BorderLayout.SOUTH.equals(getIconPosition())) {
            $(icon).selectAllStyles().setMargin(gap, 0, 0, 0);
        } else if (BorderLayout.EAST.equals(getIconPosition())) {
            $(icon).selectAllStyles().setMargin(0, 0, 0, gap);
        } else if (BorderLayout.WEST.equals(getIconPosition())) {
            $(icon).selectAllStyles().setMargin(0, gap, 0, 0);
        }
    }

    /**
     * This method is shorthand for {@link com.codename1.ui.FontImage#setMaterialIcon(com.codename1.ui.Label, char)}
     * @param c one of the constants from {@link com.codename1.ui.FontImage}
     */
    public void setMaterialIcon(char c) {
        icon.setMaterialIcon(c);
    }

    /**
     * This method is shorthand for {@link com.codename1.ui.FontImage#setMaterialIcon(com.codename1.ui.Label, com.codename1.ui.Font, char)}
     * @param c one of the constants from the font
     */
    public void setFontIcon(Font font, char c) {
        icon.setFontIcon(font, c);
    }

    /**
     * This method is shorthand for {@link com.codename1.ui.FontImage#setMaterialIcon(com.codename1.ui.Label, char, float)}
     * @param c one of the constants from {@link com.codename1.ui.FontImage}
     * @param size the size of the icon in millimeters
     */
    public void setMaterialIcon(char c, float size) {
        icon.setMaterialIcon(c, size);
    }

    /**
     * This method is shorthand for {@link com.codename1.ui.FontImage#setFontIcon(com.codename1.ui.Label, com.codename1.ui.Font, char, float)}
     * @param c one of the constants from the font
     * @param size the size of the icon in millimeters
     */
    public void setFontIcon(Font font, char c, float size) {
        icon.setFontIcon(font, c, size);
    }

    /**
     * Returns the material icon assigned to this component or 0 if not applicable
     * @return the material icon
     */
    public char getMaterialIcon() {
        return icon.getMaterialIcon();
    }

    /**
     * Returns the font icon assigned to this component or 0 if not applicable
     * @return the material icon
     */
    public char getFontIcon() {
        return icon.getFontIcon();
    }

    /**
     * Returns the material icon size assigned to this component or 0/-1 if
     * not applicable
     * @return the material icon size
     */
    public float getMaterialIconSize() {
        return icon.getMaterialIconSize();
    }

    /**
     * Returns the icon size assigned to this component or 0/-1 if
     * not applicable
     * @return the icon size
     */
    public float getFontIconSize() {
        return icon.getFontIconSize();
    }

    /**
     * Returns the font for the icon font or null if not font set
     * @return the material icon size
     */
    public Font getIconFont() {
        return icon.getIconFont();
    }

}
