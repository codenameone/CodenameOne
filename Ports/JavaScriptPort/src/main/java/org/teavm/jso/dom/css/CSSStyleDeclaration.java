/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.css;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface CSSStyleDeclaration extends JSObject {
    @JSProperty String getWidth();
    @JSProperty void setWidth(String width);
    @JSProperty String getHeight();
    @JSProperty void setHeight(String height);
    @JSProperty String getDisplay();
    @JSProperty void setDisplay(String display);
    @JSProperty String getPosition();
    @JSProperty void setPosition(String position);
    @JSProperty String getOverflow();
    @JSProperty void setOverflow(String overflow);
    @JSProperty String getBackgroundColor();
    @JSProperty void setBackgroundColor(String color);
    @JSProperty String getColor();
    @JSProperty void setColor(String color);
    @JSProperty String getFont();
    @JSProperty void setFont(String font);
    @JSProperty String getOpacity();
    @JSProperty void setOpacity(String opacity);
    @JSProperty String getZIndex();
    @JSProperty void setZIndex(String zIndex);
    @JSProperty String getCursor();
    @JSProperty void setCursor(String cursor);
    @JSProperty String getVisibility();
    @JSProperty void setVisibility(String visibility);
    @JSProperty String getTransform();
    @JSProperty void setTransform(String transform);
    @JSProperty String getTransformOrigin();
    @JSProperty void setTransformOrigin(String transformOrigin);
    String getProperty(String name);
    void setProperty(String name, String value);
    void setProperty(String name, String value, String priority);
    String removeProperty(String name);
    int getLength();
    String item(int index);
}