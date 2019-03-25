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
#include "com_codename1_ui_BrowserComponent.h"
#include "xmlvm.h"
#import "CodenameOne_GLViewController.h"

#ifdef CN1_USE_JAVASCRIPTCORE
#import <JavaScriptCore/JSContext.h>
#endif
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
     if ([error code] != NSURLErrorCancelled) {
        com_codename1_impl_ios_IOSImplementation_fireWebViewError___com_codename1_ui_BrowserComponent_int(CN1_THREAD_GET_STATE_PASS_ARG c, [error code]);
     }
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
    JAVA_BOOLEAN result = com_codename1_ui_BrowserComponent_fireBrowserNavigationCallbacks___java_lang_String_R_boolean(CN1_THREAD_GET_STATE_PASS_ARG c, xmlvm_create_java_string(CN1_THREAD_GET_STATE_PASS_ARG request.URL.absoluteString.UTF8String));
    if(result) {
        
        com_codename1_impl_ios_IOSImplementation_fireWebViewDidStartLoad___com_codename1_ui_BrowserComponent_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG c, xmlvm_create_java_string(CN1_THREAD_GET_STATE_PASS_ARG request.URL.absoluteString.UTF8String));
    } 
    return result;
}

- (void)webViewDidFinishLoad:(UIWebView *)webView {
     connections--;
     if(connections < 1) {
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
     }
#ifdef CN1_USE_JAVASCRIPTCORE
    JSContext *context =  [webView valueForKeyPath:@"documentView.webView.mainFrame.javaScriptContext"];
    
    context[@"cn1ShouldNavigate"] = ^(NSString *url) {
        JAVA_BOOLEAN result = com_codename1_ui_BrowserComponent_fireBrowserNavigationCallbacks___java_lang_String_R_boolean(CN1_THREAD_GET_STATE_PASS_ARG c, xmlvm_create_java_string(CN1_THREAD_GET_STATE_PASS_ARG url.UTF8String));
        
        return (BOOL)result;
    };
    [context evaluateScript:@"window.cn1application=window.cn1application||{};window.cn1application.shouldNavigate=cn1ShouldNavigate;"];
#endif
    com_codename1_impl_ios_IOSImplementation_fireWebViewDidFinishLoad___com_codename1_ui_BrowserComponent_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG c, xmlvm_create_java_string(CN1_THREAD_GET_STATE_PASS_ARG webView.request.URL.absoluteString.UTF8String));
}


#ifdef ENABLE_WKWEBVIEW
- (void)webView:(WKWebView *)webView didFinishNavigation:(WKNavigation *)navigation {
         connections--;
     if(connections < 1) {
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
     }
#ifdef CN1_USE_JAVASCRIPTCORE
         // SUCKS!!  WKWebView is out of process so we can't access Javascript context directly anymore.
         // So we have to do browser navigation callbacks the old fashioned way.
#endif
    com_codename1_impl_ios_IOSImplementation_fireWebViewDidFinishLoad___com_codename1_ui_BrowserComponent_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG c, xmlvm_create_java_string(CN1_THREAD_GET_STATE_PASS_ARG webView.URL.absoluteString.UTF8String));
}


- (void)webView:(WKWebView *)webView decidePolicyForNavigationAction:(WKNavigationAction *)navigationAction decisionHandler:(void (^)(WKNavigationActionPolicy))decisionHandler {
    JAVA_BOOLEAN result = com_codename1_ui_BrowserComponent_fireBrowserNavigationCallbacks___java_lang_String_R_boolean(CN1_THREAD_GET_STATE_PASS_ARG c, xmlvm_create_java_string(CN1_THREAD_GET_STATE_PASS_ARG navigationAction.request.URL.absoluteString.UTF8String));
    if(result) {
        decisionHandler(WKNavigationActionPolicyAllow);
        com_codename1_impl_ios_IOSImplementation_fireWebViewDidStartLoad___com_codename1_ui_BrowserComponent_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG c, xmlvm_create_java_string(CN1_THREAD_GET_STATE_PASS_ARG navigationAction.request.URL.absoluteString.UTF8String));
    } else {
        decisionHandler(WKNavigationActionPolicyCancel);
    }
    
}

- (void)webView:(WKWebView *)webView didCommitNavigation:(WKNavigation *)navigation {
    connections++;
    [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:YES];
}

- (void)webView:(WKWebView *)webView didFailNavigation:(WKNavigation *)navigation withError:(NSError *)error {
    if ([error code] != NSURLErrorCancelled) {
        com_codename1_impl_ios_IOSImplementation_fireWebViewError___com_codename1_ui_BrowserComponent_int(CN1_THREAD_GET_STATE_PASS_ARG c, [error code]);
    }
    connections--;
    if(connections < 1) {
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
    }
}

#endif


@end
