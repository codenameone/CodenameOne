/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cef.browser;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserFactory;
import org.cef.browser.CefRequestContext;

/**
 *
 * @author shannah
 */
public class CN1CefBrowserFactory extends CefBrowserFactory {

    @Override
    public CefBrowser create(CefClient client, String url, boolean isOffscreenRendered, boolean isTransparent, CefRequestContext context) {
        return new CN1CefBrowser(client, url, isTransparent, context);
    }
    
}
