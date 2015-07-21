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

#import "CodenameOne_GLViewController.h"
#include "xmlvm.h"
#ifndef NEW_CODENAME_ONE_VM
#include "xmlvm-util.h"
#endif
#import <UIKit/UIKit.h>




#ifdef INCLUDE_GOOGLE_CONNECT
#import "GooglePlus.h"
#include "com_codename1_social_GoogleConnect.h"
#include "com_codename1_social_LoginCallback.h"
#include "com_codename1_social_GoogleImpl.h"

#ifdef NEW_CODENAME_ONE_VM
extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str);
extern void retainCN1(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o);
extern void releaseCN1(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o);
#else
extern JAVA_OBJECT fromNSString(NSString* str);
extern void retainCN1(JAVA_OBJECT o);
extern void releaseCN1(JAVA_OBJECT o);
#endif

static JAVA_OBJECT googleLoginCallback = NULL;
static NSString *accessToken;


void com_codename1_impl_ios_IOSNative_googleLogin___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT instance) {
    if (googleLoginCallback == NULL) {
        googleLoginCallback = instance;
        GPPSignIn *signIn = [GPPSignIn sharedInstance];
        JAVA_OBJECT d = com_codename1_ui_Display_getInstance__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        JAVA_OBJECT jClientID = virtual_com_codename1_ui_Display_getProperty___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG d, fromNSString(CN1_THREAD_STATE_PASS_ARG @"ios.gplus.clientId"), JAVA_NULL);
        if (jClientID == JAVA_NULL) {
            googleLoginCallback = NULL;
            NSLog(@"Could not login to Google Plus because 'ios.gplus.clientId' property is not set.  Ensure that the ios.gplus.clientId build hint is set");
            return;
        }
        signIn.clientID = toNSString(CN1_THREAD_STATE_PASS_ARG jClientID);
        NSString *scope = toNSString(CN1_THREAD_STATE_PASS_ARG get_field_com_codename1_social_GoogleConnect_scope(googleLoginCallback));

        if (scope != nil) {
            signIn.scopes = @[scope];
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            [signIn authenticate];
        });
    } 
}


void com_codename1_impl_ios_GoogleConnectImpl_finishedWithAuth(GTMOAuth2Authentication *auth, NSError *error) {

    if (accessToken != nil) {
        [accessToken release];
    }
    accessToken = [auth accessToken];
    if (accessToken != nil) {
        [accessToken retain];
    }
      
    
    if (googleLoginCallback != NULL) {
        if (error != nil) {
            set_field_com_codename1_social_GoogleImpl_loginMessage(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [error localizedDescription]), googleLoginCallback);
        } else {
            set_field_com_codename1_social_GoogleImpl_loginMessage(CN1_THREAD_GET_STATE_PASS_ARG JAVA_NULL, googleLoginCallback);
        }
        set_field_com_codename1_social_GoogleImpl_loginCompleted(CN1_THREAD_GET_STATE_PASS_ARG JAVA_TRUE, googleLoginCallback);
        googleLoginCallback = NULL;
    }
    
    
    
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isGoogleLoggedIn___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return [[GPPSignIn sharedInstance] authentication];
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getGoogleToken___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    if (accessToken == nil) {
        return JAVA_NULL;
    } else {
        return fromNSString(CN1_THREAD_STATE_PASS_ARG accessToken);
    }

}

void com_codename1_impl_ios_IOSNative_googleLogout__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    [[GPPSignIn sharedInstance] disconnect];
    if (accessToken != nil) {
        [accessToken release];
        accessToken = nil;
    }
}
#else
void com_codename1_impl_ios_IOSNative_googleLogin___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT instance) {}

#ifdef NEW_CODENAME_ONE_VM
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isGoogleLoggedIn___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return 0;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getGoogleToken___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {return JAVA_NULL;}
#else
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isGoogleLoggedIn__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return 0;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getGoogleToken__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {return JAVA_NULL;}
#endif

void com_codename1_impl_ios_IOSNative_googleLogout__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {}

#endif