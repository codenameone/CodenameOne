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

#import "UIWebViewEventDelegate.h"
#include "com_codename1_impl_ios_IOSImplementation.h"
#include "com_codename1_ui_events_BrowserNavigationCallback.h"
#include "xmlvm.h"

extern int connections;

@implementation UIWebViewEventDelegate

- (id)initWithCallback:(void*)callback {
    self = [super init];
    if (self) {
        c = callback;
    }
    
    return self;
}

- (void)webView:(UIWebView *)webView didFailLoadWithError:(NSError *)error {
     com_codename1_impl_ios_IOSImplementation_fireWebViewError___com_codename1_ui_BrowserComponent(c);
     connections--;
     if(connections < 1) {
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
     }
}

- (void)webViewDidStartLoad:(UIWebView *)webView {
     connections++;
     [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:YES];
}

- (BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request navigationType:(UIWebViewNavigationType)navigationType {
    JAVA_OBJECT navigateCallback = com_codename1_ui_BrowserComponent_getBrowserNavigationCallback__(c);
    if(navigateCallback != NULL) {
        BOOL result = (*(JAVA_BOOLEAN (*)(JAVA_OBJECT, JAVA_OBJECT)) *(((java_lang_Object*)navigateCallback)->tib->itableBegin)[XMLVM_ITABLE_IDX_com_codename1_ui_events_BrowserNavigationCallback_shouldNavigate___java_lang_String])(navigateCallback, xmlvm_create_java_string([request.URL.absoluteString stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding].UTF8String));
        if(result) {
           com_codename1_impl_ios_IOSImplementation_fireWebViewDidStartLoad___com_codename1_ui_BrowserComponent_java_lang_String(c, xmlvm_create_java_string(request.URL.absoluteString.UTF8String));
        }
        return result;
    } else {
        com_codename1_impl_ios_IOSImplementation_fireWebViewDidStartLoad___com_codename1_ui_BrowserComponent_java_lang_String(c, xmlvm_create_java_string(request.URL.absoluteString.UTF8String));
        return YES;
    }
}

- (void)webViewDidFinishLoad:(UIWebView *)webView {
     connections--;
     if(connections < 1) {
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
     }
     com_codename1_impl_ios_IOSImplementation_fireWebViewDidFinishLoad___com_codename1_ui_BrowserComponent_java_lang_String(c, xmlvm_create_java_string(webView.request.URL.absoluteString.UTF8String));
}

@end
