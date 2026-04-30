/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import static com.codename1.impl.html5.HTML5Implementation.scaleCoord;
import com.codename1.ui.CN1Constants;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.TextArea;
import com.codename1.ui.plaf.Style;
import com.codename1.html5.js.dom.HTMLElement;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;
import com.codename1.html5.js.JSMethod;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.dom.HTMLInputElement;
import com.codename1.html5.js.dom.HTMLDocument;



/**
 *
 * @author shannah
 */
abstract class NativePicker {
    
    protected final int type;
    protected final Component source;
    protected final Object currentValue;
    protected final Object data;
    protected HTMLElement el;
    
    
    
    protected NativePicker(int type, Component source, Object currentValue, Object data) {
        this.type = type;
        this.source = source;
        this.currentValue = currentValue;
        this.data = data;
    }

    static NativePicker createNativePicker(int type, Component source, Object currentValue, Object data) {
        switch (type) {
            case Display.PICKER_TYPE_STRINGS:
                return new NativePickerStrings(type, source, currentValue, data);
        }
        throw new IllegalArgumentException("Unsupported native picker type "+type);
    }
    
    static boolean isNativePickerTypeSupported(int type) {
        return type == Display.PICKER_TYPE_STRINGS;
    }
    
    abstract Object show();

    private int lastEditorTop, lastEditorLeft, lastEditorWidth, lastEditorHeight;
    
    void resizeNativeElement() {
        if (el == null) {
            return;
        }
        HTMLElement inputEl = el;
        Component cmp = source;
        Style taStyle = cmp.getStyle();

        int paddingTop = taStyle.getPaddingTop();
        int paddingLeft = taStyle.getPadding(cmp.isRTL(), Component.LEFT);
        int paddingRight = taStyle.getPadding(cmp.isRTL(), Component.RIGHT);
        int paddingBottom = taStyle.getPadding(Component.BOTTOM);

        int newTop = scaleCoord(cmp.getAbsoluteY()+cmp.getScrollY());
        int newLeft = scaleCoord(cmp.getAbsoluteX()+cmp.getScrollX());
        int newWidth = scaleCoord(cmp.getWidth());
        int newHeight = scaleCoord(cmp.getHeight());

        if (lastEditorTop != newTop || lastEditorLeft != newLeft || lastEditorWidth != newWidth || lastEditorHeight != newHeight) {
            //String msg = "Resizing editor from "+lastEditorTop+","+lastEditorLeft+","+lastEditorWidth+","+lastEditorHeight+" to "+newTop+","+newLeft+","+newWidth+","+newHeight;
            //consoleLog(msg);
            inputEl.getStyle().setProperty("padding-top", scaleCoord(paddingTop)+"px");
            inputEl.getStyle().setProperty("padding-left", scaleCoord(paddingLeft)+"px");
            inputEl.getStyle().setProperty("padding-bottom", scaleCoord(paddingBottom)+"px");
            inputEl.getStyle().setProperty("padding-right", scaleCoord(paddingRight)+"px");
            inputEl.getStyle().setProperty("top", newTop+"px");
            inputEl.getStyle().setProperty("left", newLeft+"px");
            inputEl.getStyle().setProperty("width", newWidth+"px");
            inputEl.getStyle().setProperty("height", newHeight+"px");
            inputEl.getStyle().setProperty("border", "none");
            inputEl.getStyle().setProperty("margin", "0");

            lastEditorTop = newTop;
            lastEditorLeft = newLeft;
            lastEditorWidth = newWidth;
            lastEditorHeight = newHeight;
        }
        
    }
    
    static interface HTMLOptionsCollection extends JSObject {
        @JSProperty
        int getLength();
        
        @JSMethod
        HTMLOptionElement item(int index);
        
        @JSMethod
        HTMLOptionElement namedItem(String name);
    }
    
    static interface HTMLOptionElement extends HTMLElement {
        @JSProperty
        boolean isDefaultSelected();
        
        @JSProperty
        String getLabel();
        
        @JSProperty
        void setLabel(String text);
        
        @JSProperty
        boolean isSelected();
        
        @JSProperty
        void setSelected(boolean sel);
        
        @JSProperty
        String getText();
        
        @JSProperty
        void setText(String text);
        
        @JSProperty
        String getValue();
        
        @JSProperty
        void setValue(String value);
        
        
        
    }
    @JSBody(params={"text", "value", "defaultSelected", "selected"}, script="return new Option(text, value, defaultSelected, selected);")
    native static HTMLOptionElement createOption(String text, String value, boolean defaultSelected, boolean selected);
    
    protected final HTMLDocument document = HTML5Implementation.getInstance().window.getDocument();
    
    static HTMLSelectElement createSelect(String[] options, String selectedValue) {
        HTMLSelectElement el = (HTMLSelectElement)HTML5Implementation.getInstance().window.getDocument().createElement("select");
        int len = options.length;
        for (int i=0; i<len; i++) {
            String val = options[i];
            boolean selected = val.equals(selectedValue);
            HTMLOptionElement opt = createOption(val, String.valueOf(i), selected, selected);
            el.add(opt, i);
        }
        return el;
    }
        
    static interface HTMLSelectElement extends HTMLElement {
        @JSProperty
        int getLength();
        
        @JSProperty
        boolean isMultiple();
        
        @JSProperty
        void setMultiple(boolean multiple);
        
        @JSProperty
        int getSelectedIndex();
        
        @JSProperty
        void setSelectedIndex(int index);
        
        @JSProperty
        int getSize();
        
        @JSProperty
        void setSize(int size);
        
        @JSProperty
        String getValue();
        
        @JSProperty
        void setValue(String value);
        
        @JSMethod
        void add(HTMLOptionElement el, int index);
        
        @JSMethod
        void remove(int index);
        
        @JSProperty
        HTMLOptionsCollection getOptions();
        
        
    }
    
}
