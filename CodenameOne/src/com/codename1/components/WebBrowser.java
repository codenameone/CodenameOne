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

package com.codename1.components;
import com.codename1.ui.html.HTMLComponent;
import com.codename1.io.html.AsyncDocumentRequestHandlerImpl;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;

/**
 * A simple browser view that encapsulates a usable version of HTMLComponent or BrowserComponent and
 * automatically picks the right component for the platform preferring BrowserComponent whenever it
 * is supported.
 *
 * @author Shai Almog
 */
public class WebBrowser extends Container {
    private Component internal;
    private boolean isNative;
    private String page;
    
    /**
     * Default constructor
     */
    public WebBrowser() {
        super(new BorderLayout());
        if(BrowserComponent.isNativeBrowserSupported()) {
            isNative = true;
            internal = new BrowserComponent();
        } else {
            isNative = false;
            internal = new HTMLComponent(new AsyncDocumentRequestHandlerImpl());
        }
        addComponent(BorderLayout.CENTER, internal);
    }

    /**
     * Since the internal component can be either an HTMLComponent or a BrowserComponent one of them
     * will be returned. If you are targeting modern smartphones only you can rely on this method 
     * returning a BrowserComponent instance.
     * @return BrowserComponent or HTMLComponent
     */
    public Component getInternal() {
        return internal;
    }
    
    /**
     * Returns the title for the browser page
     * @return the title
     */
    public String getTitle() {
        if(isNative) {
            return ((BrowserComponent)internal).getTitle();
        } else {
            return ((HTMLComponent)internal).getTitle();
        }
    }

    /**
     * The page URL
     * @return the URL
     */
    public String getURL() {
        if(isNative) {
            return ((BrowserComponent)internal).getURL();
        } else {
            return ((HTMLComponent)internal).getPageURL();
        }
    }

    /**
     * Sets the page URL, jar: URL's must be supported by the implementation
     * @param url  the URL
     */
    public void setURL(String url) {
        if(isNative) {
            ((BrowserComponent)internal).setURL(url);
        } else {
            ((HTMLComponent)internal).setPage(url);
        }
    }

    /**
     * Reload the current page
     */
    public void reload() {
        if(isNative) {
            ((BrowserComponent)internal).reload();
        } else {
            ((HTMLComponent)internal).refreshDOM();
        }
    }

    /**
     * Shows the given HTML in the native viewer
     *
     * @param html HTML web page
     * @param baseUrl base URL to associate with the HTML
     */
    public void setPage(String html, String baseUrl) {
        page = html;
        if(isNative) {
            ((BrowserComponent)internal).setPage(html, baseUrl);
        } else {
            ((HTMLComponent)internal).setHTML(html, "UTF-8", null, true);
        }
    }    
    
    /**
     * Returns the page set by getPage for the GUI builder
     * 
     * @return the HTML page set manually
     */
    public String getPage() {
        return page;
    }


    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {"url", "html"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {String.class, String.class};
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if(name.equals("url")) {
            return getURL();
        }
        if(name.equals("html")) {
            return getPage();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("url")) {
            setURL((String)value);
            return null;
        }
        if(name.equals("page")) {
            setURL((String)value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }
}
