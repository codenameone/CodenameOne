/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.teavm.ext.bootstrap;

import com.codename1.teavm.ext.jquery.JQuery;

import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.dom.HTMLElement;

/**
 *
 * @author shannah
 */
public class Popover {
    private HTMLElement el;
    
    private int x, y, w, h;
    private String position;
    private String contents;
    private String title;
    
    
    private void init() {
        if (el == null) {
            el = (HTMLElement)JQuery.create("<div class='cn1-popover-div'></div>").get(0);
        }
    }
    
    private HTMLElement getEl() {
        init();
        return el;
    }
    
    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
    
    public void setPosition(String position) {
        this.position = position;
    }
    
    public void setContents(String contents) {
        this.contents = contents;
    }
    
    
    
    private static void setBounds(HTMLElement el, int x, int y, int w, int h) {
        JQuery.create(el).css(new String[]{
            "top", y+"px",
            "left", x+"px",
            "width", w+"px",
            "height", h+"px",
            "position", "absolute"
        });
    }
    
    @JSBody(params={"el","params"}, script="jQuery(el).popover(params)")
    private native static void popover(HTMLElement el, JSObject params);
    
    @JSBody(params={"el","param"}, script="jQuery(el).popover(param)")
    private native static void popover(HTMLElement el, String param);
    
    public void show() {
        JQuery body = JQuery.create("body");
        final JQuery el = JQuery.create(getEl());
        body.append(el);
        setBounds(getEl(), x, y, w, h);
        JSObject opts = JQuery.createObject(new String[]{
            "content" , contents,
            "title",title == null ? "" : title,
            
            "placement", "auto",
            "delay", "0"
        });
        JQuery.extendObject(opts, "html", true);
        
        popover(getEl(), opts);
        el.on("hidden.bs.popover", new JQuery.Callback() {

            @Override
            public void callback() {
                el.remove();
            }
        });
        popover(getEl(), "show");
        
        
    }
    
    public void hide() {
        popover(el, "destroy");
    }
    
    
}
