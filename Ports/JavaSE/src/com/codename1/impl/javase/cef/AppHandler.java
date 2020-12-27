// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package com.codename1.impl.javase.cef;

import java.util.HashMap;
import java.util.Map;
import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

public class AppHandler extends CefAppHandlerAdapter  {
    
   
    
    
    
    // We're registering our own schemes to demonstrate how to use
    // CefAppHandler.onRegisterCustomSchemes() in combination with
    // CefApp.registerSchemeHandlerFactory().
    public AppHandler(String[] args) {
        super(args);
    }
    
    
    /**
     * Register a custom scheme. This method should not be called for the built-in
     * HTTP, HTTPS, FILE, FTP, ABOUT and DATA schemes.
     *
     * If |isStandard| is true the scheme will be treated as a standard scheme.
     * Standard schemes are subject to URL canonicalization and parsing rules as
     * defined in the Common Internet Scheme Syntax RFC 1738 Section 3.1 available
     * at http://www.ietf.org/rfc/rfc1738.txt
     *
     * In particular, the syntax for standard scheme URLs must be of the form:
     * <pre>
     *  [scheme]://[username]:[password]@[host]:[port]/[url-path]
     * </pre>
     * Standard scheme URLs must have a host component that is a fully qualified
     * domain name as defined in Section 3.5 of RFC 1034 [13] and Section 2.1 of
     * RFC 1123. These URLs will be canonicalized to "scheme://host/path" in the
     * simplest case and "scheme://username:password@host:port/path" in the most
     * explicit case. For example, "scheme:host/path" and "scheme:///host/path"
     * will both be canonicalized to "scheme://host/path". The origin of a
     * standard scheme URL is the combination of scheme, host and port (i.e.,
     * "scheme://host:port" in the most explicit case).
     *
     * For non-standard scheme URLs only the "scheme:" component is parsed and
     * canonicalized. The remainder of the URL will be passed to the handler
     * as-is. For example, "scheme:///some%20text" will remain the same.
     * Non-standard scheme URLs cannot be used as a target for form submission.
     *
     * If |isLocal| is true the scheme will be treated with the same security
     * rules as those applied to "file" URLs. Normal pages cannot link to or
     * access local URLs. Also, by default, local URLs can only perform
     * XMLHttpRequest calls to the same URL (origin + path) that originated the
     * request. To allow XMLHttpRequest calls from a local URL to other URLs with
     * the same origin set the CefSettings.file_access_from_file_urls_allowed
     * value to true. To allow XMLHttpRequest calls from a local URL to all
     * origins set the CefSettings.universal_access_from_file_urls_allowed value
     * to true.
     *
     * If |isDisplayIsolated| is true the scheme can only be displayed from
     * other content hosted with the same scheme. For example, pages in other
     * origins cannot create iframes or hyperlinks to URLs with the scheme. For
     * schemes that must be accessible from other schemes set this value to false,
     * set |is_cors_enabled| to true, and use CORS "Access-Control-Allow-Origin"
     * headers to further restrict access.
     *
     * If |isSecure| is true the scheme will be treated with the same security
     * rules as those applied to "https" URLs. For example, loading this scheme
     * from other secure schemes will not trigger mixed content warnings.
     *
     * If |isCorsEnabled| is true the scheme that can be sent CORS requests.
     * This value should be true in most cases where |isStandard| is true.
     *
     * If |isCspBypassing| is true the scheme can bypass Content-Security-Policy
     * (CSP) checks. This value should be false in most cases where |isStandard|
     * is true.
     * 
     * If |is_fetch_enabled| is true the scheme can perform Fetch API requests.
     *
     * This function may be called on any thread. It should only be called once
     * per unique |schemeName| value. If |schemeName| is already registered or
     * if an error occurs this method will return false.
     */

    // (1) First of all we have to register our custom schemes by implementing
    //     the method "onRegisterCustomSchemes. The scheme names are added by
    //     calling CefSchemeRegistrar.addCustomScheme.
    @Override
    public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
        if (registrar.addCustomScheme(
                    SearchSchemeHandler.scheme, true, false, false, false, true, false, false)) {
            System.out.println("Added scheme " + SearchSchemeHandler.scheme + "://");
        }
        if (registrar.addCustomScheme(
                    ClientSchemeHandler.scheme, true, false, false, false, true, false, false)) {
            System.out.println("Added scheme " + ClientSchemeHandler.scheme + "://");
        }
        if (registrar.addCustomScheme(
                    InputStreamSchemeHandler.scheme, 
                false, // This is a non-standard URL.  Scheme is like cn1stream://streams/id
                false, // Don't treat this as a local scheme  (trying to avoid hitting security restrictions)
                false, // This can be displayed from other schemes (not isolated).
                true, // isSecure - we don't want mixed content warnings
                true, // isCorsEnabled - 
                true, // isCspBypassing - we can bypass content restrictions
                false // Fetch is not enabled
        )) {
            
            System.out.println("Added scheme " + InputStreamSchemeHandler.scheme + "://");
        }
    }

    // (2) At the next step we have to register a SchemeHandlerFactory which is
    //     called if an user enters our registered scheme.
    //
    //     This is done via the CefApp.registerSchemeHandlerFactory() method.
    //     A good place to call this function is from
    //     CefAppHandler.onContextInitialized().
    //
    //     The empty |domain_name| value will cause the factory to match all
    //     domain names. A set |domain_name| will only be valid for the entered
    //     domain.
    @Override
    public void onContextInitialized() {
        CefApp cefApp = CefApp.getInstance();
        cefApp.registerSchemeHandlerFactory(
                SearchSchemeHandler.scheme, SearchSchemeHandler.domain, new SchemeHandlerFactory());
        cefApp.registerSchemeHandlerFactory(
                ClientSchemeHandler.scheme, ClientSchemeHandler.domain, new SchemeHandlerFactory());
        cefApp.registerSchemeHandlerFactory(
                InputStreamSchemeHandler.scheme, InputStreamSchemeHandler.domain, new SchemeHandlerFactory());
        
    }

    // (3) The SchemeHandlerFactory creates a new ResourceHandler instance for each
    //     request the user has send to the browser. The ResourceHandler is the
    //     responsible class to process and return the result of a received
    //     request.
    private class SchemeHandlerFactory implements CefSchemeHandlerFactory {
        @Override
        public CefResourceHandler create(
                CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
            if (schemeName.equals(SearchSchemeHandler.scheme))
                return new SearchSchemeHandler(browser);
            else if (schemeName.equals(ClientSchemeHandler.scheme))
                return new ClientSchemeHandler();
            else if (schemeName.equals(InputStreamSchemeHandler.scheme)) 
                return new InputStreamSchemeHandler();
            return null;
        }
    }

    @Override
    public void stateHasChanged(CefAppState state) {
        System.out.println("AppHandler.stateHasChanged: " + state);
        if (state == CefAppState.TERMINATED || state == CefAppState.SHUTTING_DOWN) System.exit(0);
    }
}
