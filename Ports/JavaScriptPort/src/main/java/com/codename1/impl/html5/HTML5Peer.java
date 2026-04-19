/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import com.codename1.charts.util.ColorUtil;
import com.codename1.impl.html5.HTML5Implementation.NativeFont;
import com.codename1.teavm.jso.util.JS;
import com.codename1.ui.PeerComponent;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.CSSStyleDeclaration;
import com.codename1.html5.js.dom.HTMLElement;
import static com.codename1.impl.html5.HTML5Implementation.scaleCoord;
import static com.codename1.impl.html5.HTML5Implementation.unscaleCoord;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.events.StyleListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.dom.HTMLOptionElement;
import com.codename1.html5.js.dom.NodeList;
import org.w3c.dom.html.HTMLStyleElement;


/**
 *
 * @author shannah
 */
public class HTML5Peer extends PeerComponent {
    
    //HTMLElement el;
    
    // A flag that is set if the CN1 styles should be propagated
    // to this element.  This is handy for peer text fields and selects
    // that you want to be able to style using CN1 standard styles.
    // This will be triggered by adding an HTML element to the native html element
    // to avoid native interfaces needing to call into Java which is a hassle.
    private boolean matchCN1Style;
    
    public HTML5Peer(HTMLElement el){
        super(el);
        //this.el = el;
        String cssClass = el.getAttribute("class");
        if (cssClass == null){
            cssClass = "";
        }
        cssClass += "cn1-native-peer";
        if (HTML5Implementation.getInstance().paintNativePeersBehind()) {
            el.getStyle().setProperty("z-index", "-1000");
        }
        el.setAttribute("class", cssClass);
        if (el.hasAttribute("data-cn1-match-style")) {
            matchCN1Style = true;
            applyStyle(getStyle());
        }
        
        
    }

    protected HTMLElement el() {
        return (HTMLElement)getNativePeer();
    }
    
    private void applyStyle(Style source) {
        HTMLElement el = el();
        Font f = source.getFont();
        NativeFont nf = (NativeFont)f.getNativeFont();
        el.getStyle().setProperty("font", nf.getScaledCSS());
        
        el.getStyle().setProperty("padding", 
                    unscaleCoord(source.getPaddingTop())+"px "
                    +unscaleCoord(source.getPaddingRight(false))+"px "
                    +unscaleCoord(source.getPaddingBottom())+"px "
                    +unscaleCoord(source.getPaddingLeft(false))+"px");
        int fgColor = source.getFgColor();
        int r = ColorUtil.red(fgColor);
        int g = ColorUtil.green(fgColor);
        int b = ColorUtil.blue(fgColor);
        
        el.getStyle().setProperty("color", "rgb("+r+","+g+","+b+")");
    }
    
    @Override
    public void styleChanged(String propertyName, Style source) {
        super.styleChanged(propertyName, source);
        if (!matchCN1Style || getParent() == null) {
            return;
        }
        
        if (source != getParent().getStyle()) {
            return;
        }
        HTMLElement el = el();
        if (Style.FONT.equals(propertyName)) {
            Font f = source.getFont();
            NativeFont nf = (NativeFont)f.getNativeFont();
            el.getStyle().setProperty("font", nf.getScaledCSS());
            getStyle().setFont(f);
            return;
        }
        
        if (Style.PADDING.equals(propertyName)) {
            el.getStyle().setProperty("padding", 
                    unscaleCoord(source.getPaddingTop())+"px "
                    +unscaleCoord(source.getPaddingRight(false))+"px "
                    +unscaleCoord(source.getPaddingBottom())+"px "
                    +unscaleCoord(source.getPaddingLeft(false))+"px");
            getStyle().setPaddingLeft(source.getPaddingLeft(true));
            getStyle().setPaddingRight(source.getPaddingRight(true));
            getStyle().setPaddingTop(source.getPaddingTop());
            getStyle().setPaddingBottom(source.getPaddingBottom());
            return;
        }
        if (Style.FG_COLOR.equals(propertyName)) {
            int fgColor = source.getFgColor();
            int r = ColorUtil.red(fgColor);
            int g = ColorUtil.green(fgColor);
            int b = ColorUtil.blue(fgColor);

            el.getStyle().setProperty("color", "rgb("+r+","+g+","+b+")");
            getStyle().setFgColor(fgColor);
            return;
        }
        
        if (Style.BG_COLOR.equals(propertyName)) {
            int fgColor = source.getBgColor();
            int r = ColorUtil.red(fgColor);
            int g = ColorUtil.green(fgColor);
            int b = ColorUtil.blue(fgColor);

            el.getStyle().setProperty("background-color", "rgb("+r+","+g+","+b+")");
            getStyle().setBgColor(fgColor);
            return;
        }
        
        if (Style.BORDER.equals(propertyName)) {
            Border border = source.getBorder();
            if (border instanceof RoundRectBorder) {
                RoundRectBorder rrb = (RoundRectBorder)border;
                el.getStyle().setProperty("border-radius", unscaleCoord(CN.convertToPixels(rrb.getCornerRadius()))+"px");
                el.getStyle().setProperty("border-width", unscaleCoord(CN.convertToPixels(rrb.getStrokeThickness()))+"px");
                int fgColor = rrb.getStrokeColor();
                int r = ColorUtil.red(fgColor);
                int g = ColorUtil.green(fgColor);
                int b = ColorUtil.blue(fgColor);
                int a = rrb.getStrokeOpacity();

                el.getStyle().setProperty("border-color", "rgba("+r+","+g+","+b+","+a+")");
            } else {
                el.getStyle().setProperty("border-radius", "0");
                el.getStyle().setProperty("border-width", unscaleCoord(border.getThickness())+"px");
                int fgColor = (int)border.getProperty("ColorA");
                int r = ColorUtil.red(fgColor);
                int g = ColorUtil.green(fgColor);
                int b = ColorUtil.blue(fgColor);
                el.getStyle().setProperty("border-color", "rgb("+r+","+g+","+b+")");
                el.getStyle().setProperty("border-color", propertyName);
            }
        }
        
        
        
    }
    
    

    @Override
    public Style getPressedStyle() {
        
        return super.getPressedStyle(); //To change body of generated methods, choose Tools | Templates.
    }

    
    
    @JSBody(params={"el"}, script="return jQuery(el).outerWidth()")
    private native static int outerWidth(HTMLElement el);
    
    
    @JSBody(params={"el"}, script="return jQuery(el).outerHeight()")
    private native static int outerHeight(HTMLElement el);
    
    private static boolean isTextInputType(String type) {
        return ("text".equals(type) || "email".equals(type) || "password".equals(type) || "search".equals(type) || "tel".equals(type) || "url".equals(type));
    }
    
    @JSBody(params={"el"}, script="return el.tagName")
    private native static String tagName(HTMLElement el);
    
    @Override
    protected Dimension calcPreferredSize() {
        HTMLElement el = el();
        HTML5Implementation._debugObj(el);
        if (el == null || JS.isUndefined(el)) {
            return super.calcPreferredSize();
        }
        
        if ("iframe".equalsIgnoreCase(tagName(el))) {
            return new Dimension(CN.getDisplayWidth(), CN.getDisplayHeight());
        }
        
        if ("video".equalsIgnoreCase(tagName(el))) {
            
            return new Dimension(640, 480);
        }
        
        if ("audio".equalsIgnoreCase(tagName(el))) {
            return new Dimension(640, getStyle().getFont().getHeight() * 2);
        }
        
        if ("input".equalsIgnoreCase(tagName(el))) {
            com.codename1.ui.Font f = getStyle().getFont();
            int h = (int)Math.round(f.getHeight() * 1.8 + getStyle().getVerticalPadding());
            int charW = f.charWidth('M');
            int w = charW * 30;
            if (isTextInputType(el.getAttribute("type")) && el.hasAttribute("size")) {
                w = charW * Integer.parseInt(el.getAttribute("size"));
            }
            w += getStyle().getHorizontalPadding();
            return new Dimension(w, h);
        }
        
        if ("select".equalsIgnoreCase(tagName(el))) {
            com.codename1.ui.Font f = getStyle().getFont();
            int h = (int)Math.round(f.getHeight() * 1.8 + getStyle().getVerticalPadding());
            int charW = f.charWidth('M');
            int w = charW * 30;
            NodeList opts = el.querySelectorAll("option");
            int len = opts.getLength();
            int maxOptionLength = 0;
            for (int i=0; i<len; i++) {
                HTMLOptionElement opt = (HTMLOptionElement)opts.item(i);
                maxOptionLength = Math.max(maxOptionLength, opt.getText().length());
            }
            w = charW * maxOptionLength;
            w += getStyle().getHorizontalPadding();
            return new Dimension(w, h);
        }
        HTMLElement cl = (HTMLElement)el.cloneNode(true);
        Window.current().getDocument().getBody().appendChild(cl);
        cl.getStyle().setProperty("width", "auto");
        cl.getStyle().setProperty("height", "auto");
        cl.getStyle().setProperty("position", "static");
        int w = outerWidth(cl);
        int h = outerHeight(cl);
        cl.getParentNode().removeChild(cl);
        
        HTML5Implementation._debugObj(el);
        Dimension out = new Dimension(
                HTML5Implementation.unscaleCoord(w), 
                HTML5Implementation.unscaleCoord(h)
        );
        return out;
    }

    
    

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        HTMLElement el = el();
        el.setHidden(!visible);
        
        
    }

    @Override
    protected void focusGained() {
        super.focusGained();
        HTMLElement el =el();
        el.focus();
    }

    @Override
    protected void focusLost() {
        super.focusLost();
        HTMLElement el = el();
        el.blur();
    }
    
    
    @Override
        protected void onPositionSizeChange() {
            
            layoutPeer();
        }
    
    protected void layoutPeer(){
        HTMLElement el = el();
        if (el.getParentNode() == null || JS.isUndefined(el.getParentNode())) {
            
            Window.current().getDocument().getBody().appendChild(el);
        }
        CSSStyleDeclaration style = el.getStyle();
        style.setProperty("top", scaleCoord(this.getAbsoluteY())+"px");
        style.setProperty("left", scaleCoord(this.getAbsoluteX())+"px");
        style.setProperty("width", scaleCoord(this.getWidth())+"px");
        style.setProperty("height", scaleCoord(this.getHeight())+"px"); 
        
        
        
    }

    @JSBody(
            params={"el"},
            script="var doc = el ? el.ownerDocument : null;"
                    + "if (!doc) { return false; }"
                    + "if (doc.documentElement && typeof doc.documentElement.contains === 'function') {"
                    + "  return doc.documentElement.contains(el);"
                    + "}"
                    + "if (doc.body && typeof doc.body.contains === 'function') {"
                    + "  return doc.body.contains(el);"
                    + "}"
                    + "return !!el.isConnected;")
    private native static boolean documentContains(HTMLElement el);
    
    @Override
    protected void initComponent() {
        super.initComponent();
        HTMLElement el = el();
        if (!documentContains(el)) {
            //Window.current().getDocument().getBody().appendChild(el);
            HTML5Implementation.getInstance().peersContainer.appendChild(el);
        } else {
            el.getStyle().setProperty("display", "block");
        }
    }
    
    
    private boolean getClientPropertyRecursive(String key, boolean defaultValue) {
        Component c = this;
        while (c != null) {
            Object val = c.getClientProperty(key);
            if (val != null && val instanceof Boolean) {
                return (Boolean)val;
            }
            c = c.getParent();
        }
        return defaultValue;
    }

    @Override
    protected void deinitialize() {
        HTMLElement el = el();
        if (documentContains(el)) {
            // The HTML5Peer.removeOnDeinitialize flag can be set to false in the BrowserComponent
            // to prevent removing the peer on deinitialize.  This is necessary in some cases
            // because removing from the DOM breaks things like RTC.
            if (getClientPropertyRecursive("HTML5Peer.removeOnDeinitialize", true)) {
                el.getParentNode().removeChild(el);
            } else {
                el.getStyle().setProperty("display", "none");
            }
        }
        super.deinitialize();
    }

    @Override
    public void putClientProperty(String key, Object value) {
        super.putClientProperty(key, value);
        
        // The HTML5Peer.removeOnDeinitialize flag is used to prevent removing the iframe
        // on deinitialize.  Usually this flag is set in the BrowserComponent.
        if ("HTML5Peer.removeOnDeinitialize".equals(key) && (value == null || Boolean.TRUE.equals(value))) {
            if (!isInitialized() && documentContains(el())) {
                el().getParentNode().removeChild(el());
            }
        }
    }


    
    
}
