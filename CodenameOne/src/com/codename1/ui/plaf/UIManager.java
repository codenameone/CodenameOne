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
package com.codename1.ui.plaf;

import com.codename1.ui.*;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.Resources;
import com.codename1.util.StringUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

/**
 * Central point singleton managing the look of the application, this class allows us to
 * customize the styles (themes) as well as the look instance.
 *
 * @author Chen Fishbein
 */
public class UIManager {

    private LookAndFeel current;
    private HashMap<String, Style> styles = new HashMap<String, Style>();
    private HashMap<String, Style> selectedStyles = new HashMap<String, Style>();
    private HashMap<String, Object> themeProps;
    private HashMap<String, Object> themeConstants = new HashMap<String, Object>();
    static UIManager instance;
    private Style defaultStyle = new Style();
    private Style defaultSelectedStyle = new Style();
    /**
     * This member is used by the resource editor
     */
    static boolean accessible = true;

    /**
     * This member is used by the resource editor
     */
    static boolean localeAccessible = true;
    
    /**
     * Useful for caching theme images so they are not loaded twice in case 
     * an image reference is used it two places in the theme (e.g. same background
     * to title and menu bar).
     */
    private HashMap<String, Image> imageCache = new HashMap<String, Image>();
    /**
     * The resource bundle allows us to implicitly localize the UI on the fly, once its
     * installed all internal application strings query the resource bundle and extract
     * their values from this table if applicable.
     */
    private Hashtable resourceBundle;
    private Map<String, String> bundle;
    
    private boolean wasThemeInstalled;
    
    /**
     * This EventDispatcher holds all listeners who would like to register to
     * Theme refreshed event
     */
    private EventDispatcher themelisteners;

    UIManager() {
        // Lazy initialization of instance for js port compatibility.  We will
        // do a double-lazy initialization to try to best prevent regressions
        // from other projects that may be out of sync.  E.g. the Designer project
        // uses the "instance" property directly.  This should guarantee that
        // instance will be set
        if (instance == null) {
            instance = this;
        }
        current = new DefaultLookAndFeel(this);    
        resetThemeProps(null);
    }
    
    /**
     * Indicates if a theme was previously installed since the last reset
     * @return true if setThemeProps was invoked
     */
    public boolean wasThemeInstalled() {
        return wasThemeInstalled;
    }

    /**
     * Singleton instance method
     * 
     * @return Instance of the ui manager
     */
    public static UIManager getInstance() {
        if (instance == null) {
            instance = new UIManager();
        }
        return instance;
    }

    /**
     * This factory method allows creating a new UIManager instance, this is usefull where an application 
     * has some screens with different context
     * @return a new UIManager instance
     * @see Formt#setUIManager(UIManager) 
     */
    public static UIManager createInstance() {
        return new UIManager();
    }
    
    /**
     * Returns the currently installed look and feel
     * 
     * @return the currently installed look and feel
     */
    public LookAndFeel getLookAndFeel() {
        return current;
    }

    /**
     * Sets the currently installed look and feel
     * 
     * @param plaf the look and feel for the application
     */
    public void setLookAndFeel(LookAndFeel plaf) {
        current.uninstall();
        current = plaf;
    }

    /**
     * Allows a developer to programmatically install a style into the UI manager
     * 
     * @param id the component id matching the given style
     * @param style the style object to install
     */
    public void setComponentStyle(String id, Style style) {
        if (id == null || id.length() == 0) {
            //if no id return the default style
            id = "";
        } else {
            id = id + ".";
        }

        styles.put(id, style);
    }

    /**
     * Allows a developer to programmatically install a style into the UI manager
     * 
     * @param id the component id matching the given style
     * @param style the style object to install
     * @param type press, dis or other custom type
     */
    public void setComponentStyle(String id, Style style, String type) {
        if(type != null && type.length() > 0) {
            if (id == null || id.length() == 0) {
                //if no id return the default style
                id = type + "#";
            } else {
                id = id + "." + type + "#";
            }
        } else {
            if (id == null || id.length() == 0) {
                //if no id return the default style
                id = "";
            } else {
                id = id + ".";
            }
        }

        styles.put(id, style);
    }

    /**
     * Allows a developer to programmatically install a style into the UI manager
     *
     * @param id the component id matching the given style
     * @param style the style object to install
     */
    public void setComponentSelectedStyle(String id, Style style) {
        if (id == null || id.length() == 0) {
            //if no id return the default style
            id = "";
        } else {
            id = id + ".";
        }

        selectedStyles.put(id, style);
    }

    /**
     * Returns the style of the component with the given id or a <b>new instance</b> of the default
     * style.
     * This method will always return a new style instance to prevent modification of the global
     * style object.
     * 
     * @param id the component id whose style we want
     * @return the appropriate style (this method never returns null)
     */
    public Style getComponentStyle(String id) {
        return getComponentStyleImpl(id, false, "");
    }

    /**
     * Returns the selected style of the component with the given id or a <b>new instance</b> of the default
     * style.
     * This method will always return a new style instance to prevent modification of the global
     * style object.
     *
     * @param id the component id whose selected style we want
     * @return the appropriate style (this method never returns null)
     */
    public Style getComponentSelectedStyle(String id) {
        return getComponentStyleImpl(id, true, "sel#");
    }

    /**
     * Returns a custom style for the component with the given id, this method always returns a
     * new instance. Custom styles allow us to install application specific or component specific
     * style attributes such as pressed, disabled, hover etc.
     *
     * @param id the component id whose custom style we want
     * @param type the style type
     * @return the appropriate style (this method never returns null)
     */
    public Style getComponentCustomStyle(String id, String type) {
        return getComponentStyleImpl(id, false, type + "#");
    }

    private Style getComponentStyleImpl(String id, boolean selected, String prefix) {
        try {
            Style style = null;

            if (id == null || id.length() == 0) {
                //if no id return the default style
                id = "";
            } else {
                id = id + ".";
            }

            if (selected) {
                style = (Style) selectedStyles.get(id);

                if (style == null) {
                    style = createStyle(id, prefix, true);
                    selectedStyles.put(id, style);
                }
            } else {
                if (prefix.length() == 0) {
                    style = (Style) styles.get(id);

                    if (style == null) {
                        style = createStyle(id, prefix, false);
                        styles.put(id, style);
                    }
                } else {
                    return createStyle(id, prefix, false);
                }
            }

            return new Style(style);
        } catch(Throwable err) {
            // fail gracefully for an illegal style, this is useful for the resource editor
            System.out.println("Error creating style " + id + " selected: " + selected + " prefix: " + prefix);
            err.printStackTrace();
            return new Style(defaultStyle);
        }
    }

    /**
     * @return the name of the current theme for theme switching UI's
     */
    public String getThemeName() {
        if (themeProps != null) {
            return (String) themeProps.get("name");
        }
        return null;
    }

    // for internal use by the resource editor
    HashMap<String, Object> getThemeProps() {
        return themeProps;
    }

    /**
     * Initializes the theme properties with the current "defaults"
     *
     * @param installedTheme the theme to be installed or null, this is used
     * to check if style inheritance is used in which case we must NOT init
     * style defaults for that particular component
     */
    private void resetThemeProps(Hashtable installedTheme) {
        themeProps = new HashMap<String, Object>();
        wasThemeInstalled = false;
        String disabledColor = Integer.toHexString(getLookAndFeel().getDisableColor());
        Integer centerAlign = new Integer(Component.CENTER);
        Integer rightAlign = new Integer(Component.RIGHT);
        Integer leftAlign = new Integer(Component.LEFT);

        // global settings
        themeProps.put("sel#transparency", "255");
        themeProps.put("dis#fgColor", disabledColor);

        Font thinFont = Font.getDefaultFont();
        Font lightFont = thinFont;
        Font italic = Font.createSystemFont(Font.FACE_SYSTEM,
                    Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
        if(Font.isNativeFontSchemeSupported()) {
            int size = Display.getInstance().convertToPixels(2.5f);
            thinFont = Font.createTrueTypeFont("native:MainThin", "native:MainThin").derive(size, Font.STYLE_PLAIN);
            lightFont = Font.createTrueTypeFont("native:MainLight", "native:MainLight").derive(size, Font.STYLE_PLAIN);
            italic = Font.createTrueTypeFont("native:ItalicLight", "native:ItalicLight").derive(size, Font.STYLE_ITALIC);
        }
        
        // component specific settings
        if (installedTheme == null || !installedTheme.containsKey("ToolbarSearch.derive")) {
            themeProps.put("ToolbarSearch.derive", "Toolbar");
        }
        
        if (installedTheme == null || !installedTheme.containsKey("TextFieldSearch.derive")) {
            themeProps.put("TextFieldSearch.derive", "Title");
            themeProps.put("TextFieldSearch.align", leftAlign);
            themeProps.put("TextFieldSearch.sel#align", leftAlign);
            themeProps.put("TextFieldSearch.press#align", leftAlign);
            themeProps.put("TextFieldSearch.dis#align", leftAlign);
            themeProps.put("TextFieldSearch.sel#derive", "TextFieldSearch");
            themeProps.put("TextFieldSearch.press#derive", "TextFieldSearch");
            themeProps.put("TextFieldSearch.dis#derive", "TextFieldSearch");
        }
        
        if (installedTheme == null || !installedTheme.containsKey("TextHintSearch.derive")) {
            themeProps.put("TextHintSearch.derive", "TextHint");
            themeProps.put("TextHintSearch.transparency", "0");            
        }

        if (installedTheme == null || !installedTheme.containsKey("AccordionItem.derive")) {
            themeProps.put("AccordionItem.margin", "0,0,0,0");
            themeProps.put("AccordionItem.padding", "1,1,1,1");
            themeProps.put("AccordionItem.border", Border.createLineBorder(1));
        }
        
        if (installedTheme == null || !installedTheme.containsKey("AccordionHeader.derive")) {
            themeProps.put("AccordionHeader.margin", "0,0,0,0");
            themeProps.put("AccordionHeader.sel#margin", "0,0,0,0");
            themeProps.put("AccordionHeader.press#margin", "0,0,0,0");
            themeProps.put("AccordionHeader.padding", "0,0,0,0");
            themeProps.put("AccordionHeader.sel#padding", "0,0,0,0");
            themeProps.put("AccordionHeader.press#padding", "0,0,0,0");
        }
        
        if (installedTheme == null || !installedTheme.containsKey("SignatureButton.derive")) {
            themeProps.put("SignatureButton.align", centerAlign);
            themeProps.put("SignatureButton.sel#derive", "SignatureButton");
            themeProps.put("SignatureButton.press#derive", "SignatureButton");
            themeProps.put("SignatureButton.dis#derive", "SignatureButton");
        }
        
        if (installedTheme == null || !installedTheme.containsKey("ToastBar.derive")) {
            themeProps.put("ToastBar.margin", "0,0,0,0");
            themeProps.put("ToastBar.bgColor", "0");
            themeProps.put("ToastBar.transparency", "200");
            themeProps.put("ToastBar.bgType", new Byte(Style.BACKGROUND_NONE));
            themeProps.put("ToastBar.border", Border.createEmpty());
            themeProps.put("ToastBar.sel#derive", "ToastBar");
            themeProps.put("ToastBar.press#derive", "ToastBar");
            themeProps.put("ToastBar.dis#derive", "ToastBar");
        }
        
        if (installedTheme == null || !installedTheme.containsKey("ToastBarMessage.derive")) {
            
            themeProps.put("ToastBarMessage.font", lightFont);
            themeProps.put("ToastBarMessage.transparency", "0");
            themeProps.put("ToastBarMessage.fgColor", "FFFFFF");
            themeProps.put("ToastBarMessage.bgType", new Byte(Style.BACKGROUND_NONE));
            themeProps.put("ToastBarMessage.border", Border.createEmpty());
            themeProps.put("ToastBarMessage.sel#derive", "ToastBarMessage");
            themeProps.put("ToastBarMessage.press#derive", "ToastBarMessage");
            themeProps.put("ToastBarMessage.dis#derive", "ToastBarMessage");
        }
        
        
        if(installedTheme == null || !installedTheme.containsKey("Button.derive")) {
            themeProps.put("Button.border", Border.getDefaultBorder());
            themeProps.put("Button.padding", "4,4,4,4");
        }
        if(installedTheme == null || !installedTheme.containsKey("Button.sel#derive")) {
            themeProps.put("Button.sel#border", Border.getDefaultBorder());
            themeProps.put("Button.sel#bgColor", "a0a0a0");
            themeProps.put("Button.sel#padding", "4,4,4,4");
        }
        
        if(installedTheme == null || !installedTheme.containsKey("Button.press#derive")) {
            themeProps.put("Button.press#border", Border.getDefaultBorder().createPressedVersion());
            themeProps.put("Button.press#derive", "Button");
            themeProps.put("Button.press#padding", "4,4,4,4");
        }
        themeProps.put("Button.dis#derive", "Button");

        if(installedTheme == null || !installedTheme.containsKey("CalendarTitle.derive")) {
            themeProps.put("CalendarTitle.align", centerAlign);
        }

        if(installedTheme == null || !installedTheme.containsKey("CalendarSelectedDay.derive")) {
            themeProps.put("CalendarSelectedDay.border", Border.getDefaultBorder());
            themeProps.put("CalendarSelectedDay.align", centerAlign);
        }
        themeProps.put("CalendarSelectedDay.sel#derive", "CalendarSelectedDay");

        if(installedTheme == null || !installedTheme.containsKey("CalendarDay.derive")) {
            themeProps.put("CalendarDay.align", centerAlign);
        }
        themeProps.put("CalendarDay.dis#derive", "CalendarDay");
        themeProps.put("CalendarDay.press#derive", "CalendarDay");

        if(installedTheme == null || !installedTheme.containsKey("CalendarDay.sel#derive")) {
            themeProps.put("CalendarDay.sel#align", centerAlign);
        }

        if(installedTheme == null || !installedTheme.containsKey("ComboBox.derive")) {
            themeProps.put("ComboBox.border", Border.getDefaultBorder());
        }
        themeProps.put("ComboBox.sel#derive", "ComboBox");

        if(installedTheme == null || !installedTheme.containsKey("MenuButton.derive")) {
            themeProps.put("MenuButton.transparency", "0");
        }
        
        if(installedTheme == null || !installedTheme.containsKey("ComboBoxItem.derive")) {
            themeProps.put("ComboBoxItem.margin", "0,0,0,0");
            themeProps.put("ComboBoxItem.transparency", "0");
        }
        themeProps.put("ComboBoxItem.sel#derive", "ComboBoxItem");
        themeProps.put("ComboBoxItem.dis#derive", "ComboBoxItem");

        if(installedTheme == null || !installedTheme.containsKey("ComboBoxList.derive")) {
            themeProps.put("ComboBoxList.margin", "2,2,2,2");
            themeProps.put("ComboBoxList.padding", "0,0,0,0");
            themeProps.put("ComboBoxList.transparency", "0");
        }
        if(installedTheme == null || !installedTheme.containsKey("ComboBoxList.sel#derive")) {
            themeProps.put("ComboBoxList.sel#margin", "2,2,2,2");
            themeProps.put("ComboBoxList.sel#padding", "0,0,0,0");
            themeProps.put("ComboBoxList.sel#transparency", "0");
        }

        if(installedTheme == null || !installedTheme.containsKey("ComboBoxPopup.derive")) {
            themeProps.put("ComboBoxPopup.border", Border.getDefaultBorder());
        }
        themeProps.put("ComboBoxPopup.sel#derive", "ComboBoxPopup");

        if(installedTheme == null || !installedTheme.containsKey("Command.derive")) {
            themeProps.put("Command.margin", "0,0,0,0");
            themeProps.put("Command.transparency", "0");
        }
        themeProps.put("Command.sel#derive", "Command");
        themeProps.put("Command.dis#derive", "Command");

        if(installedTheme == null || !installedTheme.containsKey("CommandList.derive")) {
            themeProps.put("CommandList.margin", "0,0,0,0");
            themeProps.put("CommandList.padding", "0,0,0,0");
            themeProps.put("CommandList.transparency", "0");
        }
        themeProps.put("CommandList.sel#derive", "CommandList");

        if(installedTheme == null || !installedTheme.containsKey("ComponentGroup.derive")) {
            themeProps.put("ComponentGroup.derive", "Container");
        }

        if(installedTheme == null || !installedTheme.containsKey("Container.derive")) {
            themeProps.put("Container.transparency", "0");
            themeProps.put("Container.margin", "0,0,0,0");
            themeProps.put("Container.padding", "0,0,0,0");
        }
        themeProps.put("Container.sel#derive", "Container");
        themeProps.put("Container.dis#derive", "Container");
        themeProps.put("Container.press#derive", "Container");

        if(installedTheme == null || !installedTheme.containsKey("OnOffSwitch.derive")) {
            themeProps.put("OnOffSwitch.transparency", "255");
            themeProps.put("OnOffSwitch.bgColor", "222222");
            themeProps.put("OnOffSwitch.padding", "0,0,0,0");
            themeProps.put("OnOffSwitch.font", lightFont);
        }
        if(installedTheme == null || !installedTheme.containsKey("OnOffSwitch.sel#derive")) {
            themeProps.put("OnOffSwitch.sel#transparency", "255");
            themeProps.put("OnOffSwitch.sel#bgColor", "222222");
            themeProps.put("OnOffSwitch.sel#padding", "0,0,0,0");
            themeProps.put("OnOffSwitch.sel#font", lightFont);
        }
        
        if(installedTheme == null || !installedTheme.containsKey("ContentPane.derive")) {
            themeProps.put("ContentPane.transparency", "0");
            themeProps.put("ContentPane.margin", "0,0,0,0");
            themeProps.put("ContentPane.padding", "0,0,0,0");
        }
        themeProps.put("ContentPane.sel#derive", "ContentPane");

        if(installedTheme == null || !installedTheme.containsKey("DialogContentPane.derive")) {
            themeProps.put("DialogContentPane.margin", "0,0,0,0");
            themeProps.put("DialogContentPane.padding", "0,0,0,0");
            themeProps.put("DialogContentPane.transparency", "0");
        }

        if(installedTheme == null || !installedTheme.containsKey("DialogTitle.derive")) {
            themeProps.put("DialogTitle.align", centerAlign);
        }

        if(installedTheme == null || !installedTheme.containsKey("Form.derive")) {
            themeProps.put("Form.padding", "0,0,0,0");
            themeProps.put("Form.margin", "0,0,0,0");
        }
        themeProps.put("Form.sel#derive", "Form");

        if(installedTheme == null || !installedTheme.containsKey("HorizontalScroll.derive")) {
            themeProps.put("HorizontalScroll.margin", "0,0,0,0");
            themeProps.put("HorizontalScroll.padding", "1,1,1,1");
        }

        if(installedTheme == null || !installedTheme.containsKey("HorizontalScrollThumb.derive")) {
            themeProps.put("HorizontalScrollThumb.padding", "0,0,0,0");
            themeProps.put("HorizontalScrollThumb.bgColor", "0");
            themeProps.put("HorizontalScrollThumb.margin", "0,0,0,0");
        }

        if(installedTheme == null || !installedTheme.containsKey("List.derive")) {
            themeProps.put("List.transparency", "0");
            themeProps.put("List.margin", "0,0,0,0");
        }
        themeProps.put("List.sel#derive", "List");

        if(installedTheme == null || !installedTheme.containsKey("ListRenderer.derive")) {
            themeProps.put("ListRenderer.transparency", "0");
        }
        if(installedTheme == null || !installedTheme.containsKey("ListRenderer.sel#derive")) {
            themeProps.put("ListRenderer.sel#transparency", "100");
        }
        themeProps.put("ListRenderer.dis#derive", "ListRenderer");

        if(installedTheme == null || !installedTheme.containsKey("Menu.derive")) {
            themeProps.put("Menu.padding", "0,0,0,0");
        }
        themeProps.put("Menu.sel#derive", "Menu");

        if(installedTheme == null || !installedTheme.containsKey("PopupContentPane.derive")) {
            themeProps.put("PopupContentPane.transparency", "0");
        }

        if(installedTheme == null || !installedTheme.containsKey("Scroll.derive")) {
            themeProps.put("Scroll.margin", "0,0,0,0");
            int halfMM = Display.getInstance().convertToPixels(10, true) / 20;
            halfMM = Math.max(1, halfMM);
            themeProps.put("Scroll.padding", halfMM + "," + halfMM + "," + halfMM + "," + halfMM);
        }

        if(installedTheme == null || !installedTheme.containsKey("ScrollThumb.derive")) {
            themeProps.put("ScrollThumb.padding", "0,0,0,0");
            themeProps.put("ScrollThumb.margin", "0,0,0,0");
            themeProps.put("ScrollThumb.bgColor", "0");
        }

        if(installedTheme == null || !installedTheme.containsKey("SliderFull.derive")) {
            themeProps.put("SliderFull.bgColor", "0");
        }
        themeProps.put("SliderFull.sel#derive", "SliderFull");

        if(installedTheme == null || !installedTheme.containsKey("SoftButton.derive")) {
            themeProps.put("SoftButton.transparency", "255");
            themeProps.put("SoftButton.margin", "0,0,0,0");
            themeProps.put("SoftButton.padding", "0,0,0,0");
        }
        themeProps.put("SoftButton.sel#derive", "SoftButton");

        if(installedTheme == null || !installedTheme.containsKey("SoftButtonCenter.derive")) {
            themeProps.put("SoftButtonCenter.align", centerAlign);
            themeProps.put("SoftButtonCenter.transparency", "0");
            themeProps.put("SoftButtonCenter.derive", "SoftButton");
            themeProps.put("SoftButtonCenter.padding", "4,4,4,4");
        }
        themeProps.put("SoftButtonCenter.sel#derive", "SoftButtonCenter");
        themeProps.put("SoftButtonCenter.press#derive", "SoftButtonCenter");
        themeProps.put("SoftButtonCenter.dis#derive", "SoftButtonCenter");

        if(installedTheme == null || !installedTheme.containsKey("SoftButtonLeft.derive")) {
            themeProps.put("SoftButtonLeft.transparency", "0");
            themeProps.put("SoftButtonLeft.derive", "SoftButton");
            themeProps.put("SoftButtonLeft.padding", "4,4,4,4");
        }
        themeProps.put("SoftButtonLeft.sel#derive", "SoftButtonLeft");
        themeProps.put("SoftButtonLeft.press#derive", "SoftButtonLeft");
        themeProps.put("SoftButtonLeft.dis#derive", "SoftButtonLeft");

        if(installedTheme == null || !installedTheme.containsKey("SoftButtonRight.derive")) {
            themeProps.put("SoftButtonRight.align", rightAlign);
            themeProps.put("SoftButtonRight.transparency", "0");
            themeProps.put("SoftButtonRight.derive", "SoftButton");
            themeProps.put("SoftButtonRight.padding", "4,4,4,4");
        }
        themeProps.put("SoftButtonRight.sel#derive", "SoftButtonRight");
        themeProps.put("SoftButtonRight.press#derive", "SoftButtonRight");
        themeProps.put("SoftButtonRight.dis#derive", "SoftButtonRight");

        if(installedTheme == null || !installedTheme.containsKey("Spinner.derive")) {
            themeProps.put("Spinner.border", Border.getDefaultBorder());
        }
        themeProps.put("Spinner.sel#derive", "Spinner");

        if(installedTheme == null || !installedTheme.containsKey("SpinnerOverlay.derive")) {
            themeProps.put("SpinnerOverlay.transparency", "0");
        }

        if(installedTheme == null || !installedTheme.containsKey("Tab.derive")) {
            themeProps.put("Tab.margin", "1,1,1,1");
        }

        if(installedTheme == null || !installedTheme.containsKey("Tab.sel#derive")) {
            themeProps.put("Tab.sel#derive", "Tab");
            themeProps.put("Tab.sel#border", Border.createLineBorder(1));
        }

        // deprecated so there is no need to referesh this....
        themeProps.put("TabbedPane.margin", "0,0,0,0");
        themeProps.put("TabbedPane.padding", "0,0,0,0");
        themeProps.put("TabbedPane.transparency", "0");
        themeProps.put("TabbedPane.sel#margin", "0,0,0,0");
        themeProps.put("TabbedPane.sel#padding", "0,0,0,0");


        if(installedTheme == null || !installedTheme.containsKey("Table.derive")) {
            themeProps.put("Table.border", Border.getDefaultBorder());
        }
        themeProps.put("Table.sel#derive", "Table");

        if(installedTheme == null || !installedTheme.containsKey("TableCell.derive")) {
            themeProps.put("TableCell.transparency", "0");
        }
        themeProps.put("TableCell.sel#derive", "TableCell");

        if(installedTheme == null || !installedTheme.containsKey("TableHeader.derive")) {
            themeProps.put("TableHeader.transparency", "0");
        }
        themeProps.put("TableHeader.sel#derive", "TableHeader");

        if(installedTheme == null || !installedTheme.containsKey("Tabs.derive")) {
            themeProps.put("Tabs.bgColor", "a0a0a0");
            themeProps.put("Tabs.padding", "0,0,0,0");
        }

        if(installedTheme == null || !installedTheme.containsKey("TabsContainer.derive")) {
            themeProps.put("TabsContainer.padding", "0,0,0,0");
            themeProps.put("TabsContainer.margin", "0,0,0,0");
            themeProps.put("TabsContainer.bgColor", "a0a0a0");
        }

        if(installedTheme == null || !installedTheme.containsKey("TextArea.derive")) {
            themeProps.put("TextArea.border", Border.getDefaultBorder());
        }
        themeProps.put("TextArea.sel#derive", "TextArea");
        themeProps.put("TextArea.dis#derive", "TextArea");

        if(installedTheme == null || !installedTheme.containsKey("TextField.derive")) {
            themeProps.put("TextField.border", Border.getDefaultBorder());
        }
        
        themeProps.put("InvalidEmblem.derive", "Label");
        themeProps.put("InvalidEmblem.fgColor", "ff0000");
        themeProps.put("InvalidEmblem.transparency", "0");
        
        themeProps.put("TextField.sel#derive", "TextField");
        themeProps.put("TextField.dis#derive", "TextField");
        if(installedTheme == null || !installedTheme.containsKey("TextFieldInvalid.derive")) {
            themeProps.put("TextFieldInvalid.derive", "TextField");
            themeProps.put("TextFieldInvalid.fgColor", "ff0000");
            themeProps.put("TextFieldInvalid.sel#fgColor", "ff0000");
            themeProps.put("TextFieldInvalid.sel#fgColor", "ff0000");
            themeProps.put("TextFieldInvalid.sel#derive", "TextField");
            themeProps.put("TextFieldInvalid.dis#derive", "TextField");
        }

        if(installedTheme == null || !installedTheme.containsKey("TextAreaInvalid.derive")) {
            themeProps.put("TextAreaInvalid.derive", "TextArea");
            themeProps.put("TextAreaInvalid.fgColor", "ff0000");
            themeProps.put("TextAreaInvalid.sel#fgColor", "ff0000");
            themeProps.put("TextAreaInvalid.sel#fgColor", "ff0000");
            themeProps.put("TextAreaInvalid.sel#derive", "TextArea");
            themeProps.put("TextAreaInvalid.dis#derive", "TextArea");
        }

        if(installedTheme == null || !installedTheme.containsKey("TextHint.derive")) {
            themeProps.put("TextHint.transparency", "0");
            themeProps.put("TextHint.fgColor", "cccccc");
            themeProps.put("TextHint.font", italic);
        }

        if(installedTheme == null || !installedTheme.containsKey("Title.derive")) {
            themeProps.put("Title.margin", "0,0,0,0");
            themeProps.put("Title.transparency", "255");
            themeProps.put("Title.align", centerAlign);
        }
        themeProps.put("Title.sel#derive", "Title");

        if(installedTheme == null || !installedTheme.containsKey("TitleArea.derive")) {
            themeProps.put("TitleArea.transparency", "0");
            themeProps.put("TitleArea.margin", "0,0,0,0");
            themeProps.put("TitleArea.padding", "0,0,0,0");
        }

        if(installedTheme == null || !installedTheme.containsKey("TouchCommand.derive")) {
            themeProps.put("TouchCommand.border", Border.getDefaultBorder());
            themeProps.put("TouchCommand.padding", "10,10,10,10");
            themeProps.put("TouchCommand.margin", "0,0,0,0");
            themeProps.put("TouchCommand.align", centerAlign);
        }
        if(installedTheme == null || !installedTheme.containsKey("TouchCommand.press#derive")) {
            themeProps.put("TouchCommand.press#border", Border.getDefaultBorder().createPressedVersion());
            themeProps.put("TouchCommand.press#derive", "TouchCommand");
        }
        themeProps.put("TouchCommand.sel#derive", "TouchCommand");
        if(installedTheme == null || !installedTheme.containsKey("TouchCommand.dis#derive")) {
            themeProps.put("TouchCommand.dis#derive", "TouchCommand");
            themeProps.put("TouchCommand.dis#fgColor", disabledColor);
        }



        if(installedTheme == null || !installedTheme.containsKey("AdsComponent.sel#derive")) {
            themeProps.put("AdsComponent.sel#border", Border.getDefaultBorder());
            themeProps.put("AdsComponent.sel#padding", "2,2,2,2");
            themeProps.put("AdsComponent.sel#transparency", "0");

        }
        themeProps.put("AdsComponent#derive", "Container");
        themeProps.put("WebBrowser#derive", "Container");
        
        if (installedTheme == null || !installedTheme.containsKey("MapZoomOut.derive")) {
            themeProps.put("MapZoomOut.derive", "Button");
        }
        themeProps.put("MapZoomOut.sel#derive", "Button.sel");
        themeProps.put("MapZoomOut.press#derive", "Button.press");
        
        if (installedTheme == null || !installedTheme.containsKey("MapZoomIn.derive")) {
            themeProps.put("MapZoomIn.derive", "Button");
        }
        themeProps.put("MapZoomIn.sel#derive", "Button.sel");
        themeProps.put("MapZoomIn.press#derive", "Button.press");        

        if (installedTheme == null || !installedTheme.containsKey("SideCommand.derive")) {
            themeProps.put("SideCommand.derive", "TouchCommand");
            themeProps.put("SideCommand.align", leftAlign);
        }
        if (installedTheme == null || !installedTheme.containsKey("SideCommand.sel#.derive")) {
            themeProps.put("SideCommand.sel#derive", "TouchCommand.sel");
            themeProps.put("SideCommand.sel#align", leftAlign);
        }
        if (installedTheme == null || !installedTheme.containsKey("SideCommand.press#.derive")) {
            themeProps.put("SideCommand.press#derive", "TouchCommand.press");
            themeProps.put("SideCommand.press#align", leftAlign);
        }
        
        if(installedTheme == null || !installedTheme.containsKey("SideNavigationPanel.derive")) {
            themeProps.put("SideNavigationPanel.padding", "0,0,0,0");
            themeProps.put("SideNavigationPanel.margin", "0,0,0,0");
            themeProps.put("SideNavigationPanel.bgColor", "343434");
            themeProps.put("SideNavigationPanel.transparency", "255");
        }
        
        if(installedTheme == null || !installedTheme.containsKey("PullToRefresh.derive")) {
            themeProps.put("PullToRefresh.padding", "0,0,0,0");
            themeProps.put("PullToRefresh.margin", "0,0,0,0");
            themeProps.put("PullToRefresh.align", centerAlign);
            themeProps.put("PullToRefresh.transparency", "0");
            themeProps.put("PullToRefresh.fgColor", "0");
       }
    
        if (installedTheme == null || !installedTheme.containsKey("AutoCompletePopup.derive")) {
            themeProps.put("AutoCompletePopup.transparency", "255");
            themeProps.put("AutoCompletePopup.padding", "0,0,0,0");
            themeProps.put("AutoCompletePopup.border", Border.createLineBorder(1));       
        }
        if (installedTheme == null || !installedTheme.containsKey("AutoCompletePopup.sel#derive")) {
            themeProps.put("AutoCompletePopup.sel#transparency", "255");
            themeProps.put("AutoCompletePopup.sel#padding", "0,0,0,0");
            themeProps.put("AutoCompletePopup.sel#border", Border.createLineBorder(1));
        }
        if (installedTheme == null || !installedTheme.containsKey("AutoCompleteList.derive")) {
            themeProps.put("AutoCompleteList.margin", "1,1,1,1");
            themeProps.put("AutoCompleteList.padding", "0,0,0,0");
        }
        if (installedTheme == null || !installedTheme.containsKey("AutoCompleteList.sel#derive")) {
            themeProps.put("AutoCompleteList.sel#margin", "1,1,1,1");
            themeProps.put("AutoCompleteList.sel#padding", "0,0,0,0");
        }
        if (installedTheme == null || !installedTheme.containsKey("AutoCompleteList.press#derive")) {
            themeProps.put("AutoCompleteList.press#margin", "1,1,1,1");
            themeProps.put("AutoCompleteList.press#padding", "0,0,0,0");
        }

        
        if (installedTheme == null || !installedTheme.containsKey("CommandList.derive")) {
            themeProps.put("CommandList.transparency", "255");
            themeProps.put("CommandList.border", Border.createLineBorder(1));       
        }
        if (installedTheme == null || !installedTheme.containsKey("CommandList.sel#derive")) {
            themeProps.put("CommandList.sel#transparency", "255");
            themeProps.put("CommandList.sel#border", Border.createLineBorder(1));
        }

        if (installedTheme == null || !installedTheme.containsKey("Toolbar.derive")) {
            themeProps.put("Toolbar.derive", "TitleArea");
        }
        if(installedTheme == null || !installedTheme.containsKey("FloatingActionButton.derive")) {
            themeProps.put("FloatingActionButton.fgColor", "ffffff");
            themeProps.put("FloatingActionButton.bgColor", "d32f2f");
            themeProps.put("FloatingActionButton.transparency", "0");
            themeProps.put("FloatingActionButton.marUnit", new byte[]{Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS});
            themeProps.put("FloatingActionButton.margin", "0,2,1,2");
        }
        if(installedTheme == null || !installedTheme.containsKey("FloatingActionButton.press#derive")) {
            themeProps.put("FloatingActionButton.press#fgColor", "ffffff");
            themeProps.put("FloatingActionButton.press#bgColor", "b71c1c");
            themeProps.put("FloatingActionButton.sel#transparency", "0");
            themeProps.put("FloatingActionButton.press#marUnit", new byte[]{Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS});
            themeProps.put("FloatingActionButton.press#margin", "0,2,1,2");
        }
        if(installedTheme == null || !installedTheme.containsKey("FloatingActionButton.sel#derive")) {
            themeProps.put("FloatingActionButton.sel#fgColor", "ffffff");
            themeProps.put("FloatingActionButton.sel#transparency", "0");
            themeProps.put("FloatingActionButton.sel#bgColor", "b71c1c");
            themeProps.put("FloatingActionButton.sel#marUnit", new byte[]{Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS});
            themeProps.put("FloatingActionButton.sel#margin", "0,2,1,2");
        }
        if(installedTheme == null || !installedTheme.containsKey("FloatingActionButton.derive")) {
            themeProps.put("Badge.fgColor", "ffffff");
            themeProps.put("Badge.bgColor", "d32f2f");
            themeProps.put("Badge.press#fgColor", "ffffff");
            themeProps.put("Badge.press#bgColor", "b71c1c");
            themeProps.put("Badge.sel#fgColor", "ffffff");
            themeProps.put("Badge.sel#bgColor", "b71c1c");
            themeProps.put("Badge#padUnit", new byte[]{Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS});
            themeProps.put("Badge#padding", "0,0,0,0");
            themeProps.put("Badge.sel#padUnit", new byte[]{Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS});
            themeProps.put("Badge.sel#padding", "0,0,0,0");
            themeProps.put("Badge.press#padUnit", new byte[]{Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS});
            themeProps.put("Badge.press#padding", "0,0,0,0");
            if(Font.isNativeFontSchemeSupported()) {
                Font fnt = lightFont.derive(Display.getInstance().convertToPixels(1.5f), Font.STYLE_PLAIN);
                themeProps.put("Badge.font", fnt);
                themeProps.put("Badge.sel#font", fnt);
                themeProps.put("Badge.press#font", fnt);
            }
            themeProps.put("Badge.align", centerAlign);
            themeProps.put("Badge.sel#align", centerAlign);
            themeProps.put("Badge.press#align", centerAlign);
        }
        if(installedTheme == null || !installedTheme.containsKey("FloatingActionText.derive")) {
            themeProps.put("FloatingActionText.bgColor", "ffffff");
            themeProps.put("FloatingActionText.fgColor", "a0a0a0");
            themeProps.put("FloatingActionText.align", rightAlign);
        }
        
        
    }

    /**
     * Allows manual theme loading from a hashtable of key/value pairs
     * 
     * @param themeProps the properties of the given theme
     */
    public void setThemeProps(Hashtable themeProps) {
        if (accessible) {
            setThemePropsImpl(themeProps);
        }
        wasThemeInstalled = true;
    }

    /**
     * Adds the given theme properties on top of the existing properties without
     * clearing the existing theme first
     *
     * @param themeProps the properties of the given theme
     */
    public void addThemeProps(Hashtable themeProps) {
        if (accessible) {
            buildTheme(themeProps);
            styles.clear();
            selectedStyles.clear();
            imageCache.clear();
            current.refreshTheme(false);
        }
    }

    /**
     * Returns a theme constant defined in the resource editor
     *
     * @param constantName the name of the constant
     * @param def default value
     * @return the value of the constant or the default if the constant isn't in the theme
     */
    public int getThemeConstant(String constantName, int def) {
        String v = (String) themeConstants.get(constantName);
        if (v != null) {
            try {
                return Integer.parseInt(v);
            } catch(NumberFormatException err) {
                err.printStackTrace();
            }
        }
        return def;
    }

    /**
     * Returns a theme constant defined in the resource editor
     *
     * @param constantName the name of the constant
     * @param def default value
     * @return the value of the constant or the default if the constant isn't in the theme
     */
    public String getThemeConstant(String constantName, String def) {
        String v = (String) themeConstants.get(constantName);
        if (v != null) {
            return v;
        }
        return def;
    }

    /**
     * Returns a theme constant defined in the resource editor as a boolean value
     *
     * @param constantName the name of the constant
     * @param def default value
     * @return the value of the constant or the default if the constant isn't in the theme
     */
    public boolean isThemeConstant(String constantName, boolean def) {
        String c = getThemeConstant(constantName, null);
        if (c == null) {
            return def;
        }
        return c.equalsIgnoreCase("true") || c.equals("1");
    }

    /**
     * Returns a theme constant defined in the resource editor as a boolean value or null if the constant isn't defined
     *
     * @param constantName the name of the constant
     * @return the value of the constant or null if the constant isn't in the theme
     */
    public Boolean isThemeConstant(String constantName) {
        String c = getThemeConstant(constantName, null);
        if (c == null) {
            return null;
        }
        if(c.equalsIgnoreCase("true") || c.equals("1")) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Returns a theme constant defined in the resource editor
     *
     * @param constantName the name of the constant
     * @return the image if defined
     */
    public Image getThemeImageConstant(String constantName) {
        return (Image) themeConstants.get(constantName);
    }

    /**
     * Returns a theme mask constant
     *
     * @param constantName the name of the constant
     * @return the mask if defined
     */
    public Object getThemeMaskConstant(String constantName) {
        Object o = themeConstants.get(constantName + "Mask");
        if(o != null) {
            return o;
        }
        Image i = (Image) themeConstants.get(constantName);
        if(i == null) {
            return null;
        }
        o = i.createMask();
        themeConstants.put(constantName + "Mask", o);
        return o;
    }

    void setThemePropsImpl(Hashtable themeProps) {
        resetThemeProps(themeProps);
        styles.clear();
        themeConstants.clear();
        selectedStyles.clear();
        imageCache.clear();
        if (themelisteners != null) {
            themelisteners.fireActionEvent(new ActionEvent(themeProps,ActionEvent.Type.Theme));
        }
        buildTheme(themeProps);
        current.refreshTheme(true);
    }

    private void buildTheme(Hashtable themeProps) {
        String con = (String)themeProps.get("@includeNativeBool");
        if(con != null && con.equalsIgnoreCase("true") && Display.getInstance().hasNativeTheme()) {
            boolean a = accessible;
            accessible = true;
            Display.getInstance().installNativeTheme();
            accessible = a;
        }
        Enumeration e = themeProps.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();

            // this is a constant not a theme entry
            if (key.startsWith("@")) {
                themeConstants.put(key.substring(1, key.length()), themeProps.get(key));
                continue;
            }
            this.themeProps.put(key, themeProps.get(key));
        }

        // necessary to clear up the style so we don't get resedue from the previous UI
        defaultStyle = new Style();

        //create's the default style
        defaultStyle = createStyle("", "", false);
        defaultSelectedStyle = new Style(defaultStyle);
        defaultSelectedStyle = createStyle("", "sel#", true);
        
        String overlayThemes = (String)themeProps.get("@OverlayThemes");      
        if (overlayThemes != null) {
            java.util.List<String> overlayThemesArr = StringUtil.tokenize(overlayThemes, ',');
            for (String th : overlayThemesArr) {
                th = th.trim();
                if (th.length() == 0) {
                    continue;
                }
                try {
                    Resources res = Resources.openLayered("/"+th);
                    boolean a = accessible;
                    accessible = true;
                    addThemeProps(res.getTheme(res.getThemeResourceNames()[0]));
                    accessible = a;
                } catch (Exception ex) {
                    System.err.println("Failed to load overlay theme file specified by @overlayThemes theme constant: "+th);
                    ex.printStackTrace();
                }
            }
        }
        
    }
    
    Style createStyle(String id, String prefix, boolean selected) {
        Style style;
        String originalId = id;
        if (prefix != null && prefix.length() > 0) {
            id += prefix;
        }
        String baseStyle = (String) themeProps.get(id + "derive");
        if (baseStyle != null) {
            if(baseStyle.indexOf('.') > -1 && baseStyle.indexOf('#') < 0) {
                baseStyle += "#";
            }
            // probably a theme mistake ignore
            if(!(baseStyle + ".").equals(id)) {
                int pos = baseStyle.indexOf('.');
                if (pos > -1) {
                    String baseId = baseStyle.substring(0, pos);
                    String basePrefix = baseStyle.substring(pos + 1);
                    style = new Style(getComponentStyleImpl(baseId, basePrefix.indexOf("sel") > -1, basePrefix));
                } else {
                    style = new Style(getComponentStyle(baseStyle));
                }
            } else {
                baseStyle = null;
                if (selected) {
                    style = new Style(defaultSelectedStyle);
                } else {
                    style = new Style(defaultStyle);
                }
            }
        } else {
            if (selected) {
                style = new Style(defaultSelectedStyle);
            } else {
                style = new Style(defaultStyle);
            }
        }
        if (themeProps != null) {
            String bgColor;
            String fgColor;
            Object border;

            bgColor = (String) themeProps.get(id + Style.BG_COLOR);
            fgColor = (String) themeProps.get(id + Style.FG_COLOR);
            border = themeProps.get(id + Style.BORDER);
            Object bgImage = themeProps.get(id + Style.BG_IMAGE);
            String transperency = (String) themeProps.get(id + Style.TRANSPARENCY);
            String opacity = (String) themeProps.get(id + Style.OPACITY);
            String margin = (String) themeProps.get(id + Style.MARGIN);
            String padding = (String) themeProps.get(id + Style.PADDING);
            Object font = themeProps.get(id + Style.FONT);
            Integer alignment = (Integer) themeProps.get(id + Style.ALIGNMENT);
            Integer textDecoration = (Integer) themeProps.get(id + Style.TEXT_DECORATION);

            Byte backgroundType = (Byte) themeProps.get(id + Style.BACKGROUND_TYPE);
            Object[] backgroundGradient = (Object[]) themeProps.get(id + Style.BACKGROUND_GRADIENT);
            byte[] paddingUnit = (byte[])themeProps.get(id + Style.PADDING_UNIT);
            byte[] marginUnit = (byte[])themeProps.get(id + Style.MARGIN_UNIT);

            if (bgColor != null) {
                style.setBgColor(Integer.valueOf(bgColor, 16).intValue());
            }
            if (fgColor != null) {
                style.setFgColor(Integer.valueOf(fgColor, 16).intValue());
            }
            if (transperency != null) {
                style.setBgTransparency(Integer.valueOf(transperency).intValue());
            } else {
                if (selected) {
                    transperency = (String) themeProps.get(originalId + Style.TRANSPARENCY);
                    if (transperency != null) {
                        style.setBgTransparency(Integer.valueOf(transperency).intValue());
                    }
                }
            }
            if (opacity != null) {
                style.setOpacity(Integer.valueOf(opacity).intValue());
            } else {
                if (selected) {
                    opacity = (String) themeProps.get(originalId + Style.OPACITY);
                    if (opacity != null) {
                        style.setBgTransparency(Integer.valueOf(opacity).intValue());
                    }
                }
            }
            if (margin != null) {
                int[] marginArr = toIntArray(margin.trim());
                style.setMargin(marginArr[0], marginArr[1], marginArr[2], marginArr[3]);
            }
            if (padding != null) {
                int[] paddingArr = toIntArray(padding.trim());
                style.setPadding(paddingArr[0], paddingArr[1], paddingArr[2], paddingArr[3]);
            }
            if(paddingUnit != null) {
                style.setPaddingUnit(paddingUnit);
            } else {
                // special case for pixel based padding
                if(padding != null) {
                    style.setPaddingUnit(null);
                }
            }
            if(marginUnit != null) {
                style.setMarginUnit(marginUnit);
            } else {
                // special case for pixel based margin
                if(margin != null) {
                    style.setMarginUnit(null);
                }
            }
            if (alignment != null) {
                style.setAlignment(alignment.intValue());
            }
            if (textDecoration != null) {
                style.setTextDecoration(textDecoration.intValue());
            }
            if (backgroundType != null) {
                style.setBackgroundType(backgroundType.byteValue());
            }
            if (backgroundGradient != null) {
                if (backgroundGradient.length < 5) {
                    Object[] a = new Object[5];
                    System.arraycopy(backgroundGradient, 0, a, 0, backgroundGradient.length);
                    backgroundGradient = a;
                    backgroundGradient[4] = new Float(1);
                }
                style.setBackgroundGradient(backgroundGradient);
            }
            if (bgImage != null) {
                Image im = null;
                if (bgImage instanceof String) {
                    try {
                        String bgImageStr = (String) bgImage;
                        if (imageCache.containsKey(bgImageStr)) {
                            im = (Image) imageCache.get(bgImageStr);
                        } else {
                            if (bgImageStr.startsWith("/")) {
                                im = Image.createImage(bgImageStr);
                            } else {
                                im = parseImage((String) bgImage);
                            }
                            imageCache.put(bgImageStr, im);
                        }
                        themeProps.put(id + Style.BG_IMAGE, im);
                    } catch (IOException ex) {
                        System.out.println("failed to parse image for id = " + id + Style.BG_IMAGE);
                    }
                } else {
                    // we shouldn't normally but we might get a multi-image from the resource editor
                    if(bgImage instanceof Image) {
                        im = (Image) bgImage;
                    }
                }
                // this code should not excute in the resource editor!
                if (id.indexOf("Form") > -1) {
                    if ((im.getWidth() != Display.getInstance().getDisplayWidth() ||
                            im.getHeight() != Display.getInstance().getDisplayHeight()) && style.getBackgroundType() == Style.BACKGROUND_IMAGE_SCALED && accessible) {
                        im.scale(Display.getInstance().getDisplayWidth(),
                                Display.getInstance().getDisplayHeight());
                    }
                }
                style.setBgImage(im);
            }
            if (font != null) {
                if (font instanceof String) {
                    style.setFont(parseFont((String) font));
                } else {
                    style.setFont((com.codename1.ui.Font) font);
                }
            }
            if (border != null) {
                style.setBorder((Border) border);
            }
            style.resetModifiedFlag();
        }

        return style;
    }

    /**
     * This method is used to parse the margin and the padding
     * @param str
     * @return
     */
    private int[] toIntArray(String str) {
        int[] retVal = new int[4];
        str = str + ",";
        int rlen = retVal.length;
        for (int i = 0; i < rlen; i++) {
            retVal[i] = Integer.parseInt(str.substring(0, str.indexOf(",")));
            str = str.substring(str.indexOf(",") + 1, str.length());
        }
        return retVal;
    }

    private static Image parseImage(String value) throws IOException {
        int index = 0;
        byte[] imageData = new byte[value.length() / 2];
        int vlen = value.length();
        while (index < vlen) {
            String byteStr = value.substring(index, index + 2);
            imageData[index / 2] = Integer.valueOf(byteStr, 16).byteValue();
            index += 2;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(imageData);
        Image image = Image.createImage(in);
        in.close();
        return image;
    }

    private static com.codename1.ui.Font parseFont(String fontStr) {
        if (fontStr.startsWith("System")) {
            int face = 0;
            int style = 0;
            int size = 0;
             String 
             faceStr, styleStr,sizeStr   ;
            String sysFont = fontStr.substring(fontStr.indexOf("{") + 1, fontStr.indexOf("}"));
            faceStr = sysFont.substring(0, sysFont.indexOf(";"));
            sysFont = sysFont.substring(sysFont.indexOf(";") + 1, sysFont.length());
            styleStr = sysFont.substring(0, sysFont.indexOf(";"));
            sizeStr = sysFont.substring(sysFont.indexOf(";") + 1, sysFont.length());

            if (faceStr.indexOf("FACE_SYSTEM") > -1) {
                face = Font.FACE_SYSTEM;
            } else if (faceStr.indexOf("FACE_MONOSPACE") > -1) {
                face = Font.FACE_MONOSPACE;
            } else if (faceStr.indexOf("FACE_PROPORTIONAL") > -1) {
                face = Font.FACE_PROPORTIONAL;
            }

            if (styleStr.indexOf("STYLE_PLAIN") > -1) {
                style = Font.STYLE_PLAIN;
            } else {
                if (styleStr.indexOf("STYLE_BOLD") > -1) {
                    style = Font.STYLE_BOLD;
                }
                if (styleStr.indexOf("STYLE_ITALIC") > -1) {
                    style = style | Font.STYLE_ITALIC;
                }
                if (styleStr.indexOf("STYLE_UNDERLINED") > -1) {
                    style = style | Font.STYLE_UNDERLINED;
                }
            }

            if (sizeStr.indexOf("SIZE_SMALL") > -1) {
                size = Font.SIZE_SMALL;
            } else if (sizeStr.indexOf("SIZE_MEDIUM") > -1) {
                size = Font.SIZE_MEDIUM;
            } else if (sizeStr.indexOf("SIZE_LARGE") > -1) {
                size = Font.SIZE_LARGE;
            }


            return com.codename1.ui.Font.createSystemFont(face, style, size);
        } else {
            if (fontStr.toLowerCase().startsWith("bitmap")) {
                try {
                    String bitmapFont = fontStr.substring(fontStr.indexOf("{") + 1, fontStr.indexOf("}"));
                    String nameStr;
                    nameStr = bitmapFont.substring(0, bitmapFont.length());


                    if (nameStr.toLowerCase().startsWith("highcontrast")) {
                        nameStr = nameStr.substring(nameStr.indexOf(";") + 1, nameStr.length());
                        com.codename1.ui.Font f = com.codename1.ui.Font.getBitmapFont(nameStr);
                        f.addContrast((byte) 30);
                        return f;
                    }

                    return com.codename1.ui.Font.getBitmapFont(nameStr);
                } catch (Exception ex) {
                    // illegal argument exception?
                    ex.printStackTrace();
                }
            }
        }
        // illegal argument?
        return null;
    }

    /**
     * The resource bundle allows us to implicitly localize the UI on the fly, once its
     * installed all internal application strings query the resource bundle and extract
     * their values from this table if applicable.
     * 
     * @return the localization bundle
     * @deprecated this method uses the old resource bundle hashtable, use the new getBundle() method
     */
    public Hashtable getResourceBundle() {
        if(resourceBundle == null && bundle != null) {
            resourceBundle = new Hashtable(bundle);
        }
        return resourceBundle;
    }

    /**
     * The resource bundle allows us to implicitly localize the UI on the fly, once its
     * installed all internal application strings query the resource bundle and extract
     * their values from this table if applicable.
     * 
     * @return the localization bundle
     */
    public Map<String, String> getBundle() {
        return bundle;
    }

    /**
     * The resource bundle allows us to implicitly localize the UI on the fly, once its
     * installed all internal application strings query the resource bundle and extract
     * their values from this table if applicable.
     * 
     * @param resourceBundle the localization bundle
     */
    public void setBundle(Map<String, String> bundle) {
        if(localeAccessible) {
            this.bundle = bundle;
            if(bundle != null) {
                String v = (String)bundle.get("@rtl");
                if(v != null) {
                    getLookAndFeel().setRTL(v.equalsIgnoreCase("true"));
                    
                    // update some "bidi sensitive" variables in the LaF
                    current.refreshTheme(false);
                }
                String textFieldInputMode = (String)bundle.get("@im");
                if(textFieldInputMode != null && textFieldInputMode.length() > 0) {
                    String[] tokenized = toStringArray(StringUtil.tokenizeString(textFieldInputMode, '|'));
                    TextField.setDefaultInputModeOrder(tokenized);
                    int tlen = tokenized.length;
                    for(int iter = 0 ; iter < tlen; iter++) {
                        String val = tokenized[iter];
                        String actual = (String)bundle.get("@im-" + val);
                        // val can be null for builtin input mode types...
                        if(actual != null) {
                            TextField.addInputMode(val, parseTextFieldInputMode(actual), Character.isUpperCase(val.charAt(0)));
                        }
                    }
                }
            }
        }
    }
    
    /**
     * The resource bundle allows us to implicitly localize the UI on the fly, once its
     * installed all internal application strings query the resource bundle and extract
     * their values from this table if applicable.
     * 
     * @param resourceBundle the localization bundle
     * @deprecated this method uses the old resource bundle hashtable, use the new setBundle() method
     */
    public void setResourceBundle(Hashtable resourceBundle) {
        if(localeAccessible) {
            this.resourceBundle = resourceBundle;
            if(resourceBundle != null) {
                String v = (String)resourceBundle.get("@rtl");
                if(v != null) {
                    getLookAndFeel().setRTL(v.equalsIgnoreCase("true"));
                    
                    // update some "bidi sensitive" variables in the LaF
                    current.refreshTheme(false);
                }
                String vkbInputMode = (String)resourceBundle.get("@vkb");
                if(vkbInputMode != null && vkbInputMode.length() > 0) {
                    String[] tokenized = toStringArray(StringUtil.tokenizeString(vkbInputMode, '|'));
                    VirtualKeyboard.setDefaultInputModeOrder(tokenized);
                    int tlen = tokenized.length;
                    for(int iter = 0 ; iter < tlen ; iter++) {
                        String val = tokenized[iter];
                        String[][] res = getInputMode("@vkb-", tokenized[iter], resourceBundle);
                        if(res != null) {
                            VirtualKeyboard.addDefaultInputMode(val, res);
                        }
                    }
                }
                String textFieldInputMode = (String)resourceBundle.get("@im");
                if(textFieldInputMode != null && textFieldInputMode.length() > 0) {
                    String[] tokenized = toStringArray(StringUtil.tokenizeString(textFieldInputMode, '|'));
                    TextField.setDefaultInputModeOrder(tokenized);
                    int tlen = tokenized.length;
                    for(int iter = 0 ; iter < tlen ; iter++) {
                        String val = tokenized[iter];
                        String actual = (String)resourceBundle.get("@im-" + val);
                        // val can be null for builtin input mode types...
                        if(actual != null) {
                            TextField.addInputMode(val, parseTextFieldInputMode(actual), Character.isUpperCase(val.charAt(0)));
                        }
                    }
                }
                bundle = new HashMap<String, String>((Hashtable<String, String>)resourceBundle);
            } else {
                bundle = null;
            }
        }
    }

    private Hashtable parseTextFieldInputMode(String s) {
        Vector tokens = StringUtil.tokenizeString(s, '|');
        Hashtable response = new Hashtable();
        int count = tokens.size();
        for(int iter = 0 ; iter < count ; iter++) {
            String t = (String)tokens.elementAt(iter);
            int pos = t.indexOf('=');
            String key = t.substring(0, pos);
            String val = t.substring(pos + 1);
            response.put(Integer.valueOf(key), val);
        }
        return response;
    }

    private String[][] getInputMode(String prefix, String val, Hashtable resourceBundle) {
        if(resourceBundle.containsKey(prefix + val)) {
            return tokenizeMultiArray((String)resourceBundle.get(prefix + val), '|', '\n');
        }
        return null;
    }

    private String[] toStringArray(Vector v) {
        String[] arr = new String[v.size()];
        int alen = arr.length;
        for(int iter = 0 ; iter < alen ; iter++) {
            arr[iter] = (String)v.elementAt(iter);
        }
        return arr;
    }

    private String[][] tokenizeMultiArray(String s, char separator, char lineBreak) {
        Vector lines = StringUtil.tokenizeString(s, lineBreak);
        int lineCount = lines.size();
        String[][] result = new String[lineCount][];
        for(int iter = 0 ; iter < lineCount ; iter++) {
            String currentString = (String)lines.elementAt(iter);
            result[iter] = toStringArray(StringUtil.tokenizeString(currentString, separator));
        }
        return result;
    }

    /**
     * Localizes the given string from the resource bundle if such a String exists in the
     * resource bundle. If no key exists in the bundle then or a bundle is not installed
     * the default value is returned.
     * 
     * @param key The key used to lookup in the resource bundle
     * @param defaultValue the value returned if no such key exists
     * @return either default value or the appropriate value
     */
    public String localize(String key, String defaultValue) {
        if(bundle != null && key != null) {
            Object o = bundle.get(key);
            if (o != null) {
                return (String) o;
            }
        }
        return defaultValue;
    }

    /**
     * Adds a Theme refresh listener.
     * The listenres will get a callback when setThemeProps method is invoked.
     * 
     * @param l an ActionListener to be added
     */
    public void addThemeRefreshListener(ActionListener l) {

        if (themelisteners == null) {
            themelisteners = new EventDispatcher();
        }
        themelisteners.addListener(l);
    }

    /**
     * Removes a Theme refresh listener.
     * 
     * @param l an ActionListener to be removed
     */
    public void removeThemeRefreshListener(ActionListener l) {

        if (themelisteners == null) {
            return;
        }
        themelisteners.removeListener(l);
    }
    
    /**
     * This is a shorthand notation for boilerplate code for initializing the first theme in the given resource file
     * and catching/doing nothing with the IOException since this would be invoked too early in the program
     * where we would be out of options if something like that happens. Effectively this is the same as writing:
     * <pre>
        try {
            theme = Resources.openLayered(resourceFile);
            UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
        } catch(IOException e){
            e.printStackTrace();
        }
     * </pre>
     * @param resourceFile the name of the resource file starting with / and without the res extension
     * @return the resource file or null in case of a failure
     */
    public static Resources initFirstTheme(String resourceFile) {
        try {
            Resources theme = Resources.openLayered(resourceFile);
            UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
            Resources.setGlobalResources(theme);
            return theme;
        } catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }

    
    /**
     * Same as the initFirstTheme method, but unlike that method this allows specifying the theme resource name
     * @param resourceFile the name of the resource file starting with / and without the res extension
     * @param resName the name of the theme to use from the file if it contains more than one theme
     * @return the resource file or null in case of a failure
     */
    public static Resources initNamedTheme(String resourceFile, String resName) {
        try {
            Resources theme = Resources.openLayered(resourceFile);
            UIManager.getInstance().setThemeProps(theme.getTheme(resName));
            Resources.setGlobalResources(theme);
            return theme;
        } catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }
}
