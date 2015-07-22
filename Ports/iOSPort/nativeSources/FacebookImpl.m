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

JAVA_BOOLEAN publishPermission = 0;

#ifdef INCLUDE_FACEBOOK_CONNECT
#include "com_codename1_social_FacebookConnect.h"
#include "com_codename1_social_LoginCallback.h"
#include "com_codename1_social_FacebookImpl.h"
#import "FBSDKLoginKit.h"
#import "FBSDKAppInviteContent.h"
#import "FBSDKAppInviteDialog.h"


#ifdef NEW_CODENAME_ONE_VM
extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str);
extern void retainCN1(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o);
extern void releaseCN1(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o);
#else
extern JAVA_OBJECT fromNSString(NSString* str);
extern void retainCN1(JAVA_OBJECT o);
extern void releaseCN1(JAVA_OBJECT o);
#endif

void com_codename1_impl_ios_IOSNative_facebookLogin___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT instance) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        FBSDKLoginManager *login = [[FBSDKLoginManager alloc] init];
        [login logInWithReadPermissions:@[@"public_profile", @"email", @"user_friends"]  handler:^(FBSDKLoginManagerLoginResult *result, NSError *error) {
            if (result.isCancelled) {
                // Handle cancellations
#ifdef NEW_CODENAME_ONE_VM
                set_field_com_codename1_social_FacebookImpl_loginCancelled(threadStateData, JAVA_TRUE, instance);
#else
                com_codename1_social_FacebookImpl* impl = (com_codename1_social_FacebookImpl*)instance;
                impl->fields.com_codename1_social_FacebookImpl.loginCancelled_ = TRUE;
#endif
            } else {
#ifdef NEW_CODENAME_ONE_VM
                set_field_com_codename1_social_FacebookImpl_loginCompleted(threadStateData, JAVA_TRUE, instance);
#else
                com_codename1_social_FacebookImpl* impl = (com_codename1_social_FacebookImpl*)instance;
#endif
                
            }
        }];
        POOL_END();
    });
    
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isFacebookLoggedIn__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    __block JAVA_BOOLEAN val = FALSE;
    dispatch_sync(dispatch_get_main_queue(), ^{
        val = [FBSDKAccessToken currentAccessToken] != nil;
    });
    return val;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getFacebookToken__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    __block JAVA_OBJECT str = JAVA_NULL;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSString *accessToken = [[FBSDKAccessToken currentAccessToken] tokenString];
        str = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG accessToken);
        POOL_END();
    });
    return str;
}

void com_codename1_impl_ios_IOSNative_facebookLogout__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        FBSDKLoginManager *mgr = [[FBSDKLoginManager alloc] init];
        [mgr logOut];
    });
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_askPublishPermissions___com_codename1_social_LoginCallback(JAVA_OBJECT me, JAVA_OBJECT callback) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        FBSDKAccessToken *tok = [FBSDKAccessToken currentAccessToken];
        if (tok != nil && [tok hasGranted:@"publish_actions"]) {
            publishPermission = 1;
        } else {
            FBSDKLoginManager *mgr = [[FBSDKLoginManager alloc] init];
            [mgr logInWithPublishPermissions:@[@"publish_actions"] handler:^(FBSDKLoginManagerLoginResult *result, NSError *error) {
                if(callback != JAVA_NULL) {
#ifdef NEW_CODENAME_ONE_VM
                    if(error == nil) {
                        virtual_com_codename1_social_LoginCallback_loginSuccessful__(CN1_THREAD_GET_STATE_PASS_ARG callback);
                        publishPermission = 1;
                    } else {
                        virtual_com_codename1_social_LoginCallback_loginFailed___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG callback, JAVA_NULL);
                    }
#else
                    if(error == nil) {
                        (*(void (*)(JAVA_OBJECT)) ((com_codename1_social_LoginCallback*) callback)->tib->vtable[7])(callback);
                        //com_codename1_social_LoginCallback_loginSuccessful__(callback);
                        publishPermission = 1;
                    } else {
                        //com_codename1_social_LoginCallback_loginFailed___java_lang_String(callback, JAVA_NULL);
                        (*(void (*)(JAVA_OBJECT, JAVA_OBJECT)) ((com_codename1_social_LoginCallback*) callback)->tib->vtable[6])(callback, JAVA_NULL);
                    }
#endif
                }
            }];
        }
        POOL_END();
    });
    return publishPermission;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_hasPublishPermissions__(JAVA_OBJECT me) {
    return publishPermission;
}

JAVA_VOID com_codename1_impl_ios_IOSNative_inviteFriends___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT appLinkUrl, JAVA_OBJECT previewImageUrl) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        FBSDKAppInviteContent *content =[[FBSDKAppInviteContent alloc] init];
        content.appLinkURL = [NSURL URLWithString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG appLinkUrl)];
        //optionally set previewImageURL
        content.appInvitePreviewImageURL = [NSURL URLWithString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG previewImageUrl)];
        
        // present the dialog. Assumes self implements protocol `FBSDKAppInviteDialogDelegate`
        [FBSDKAppInviteDialog showWithContent:content
                                     delegate:[CodenameOne_GLViewController instance]];
        POOL_END();
    });
}


#else

JAVA_VOID com_codename1_impl_ios_IOSNative_inviteFriends___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT appLinkUrl, JAVA_OBJECT previewImageUrl) {
    
}

void com_codename1_impl_ios_IOSNative_facebookLogin___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT instance) {
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isFacebookLoggedIn__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return FALSE;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getFacebookToken__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_NULL;
}

void com_codename1_impl_ios_IOSNative_facebookLogout__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_askPublishPermissions___com_codename1_social_LoginCallback(JAVA_OBJECT me, JAVA_OBJECT callback) {
    return 0;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_hasPublishPermissions__(JAVA_OBJECT me) {
    return 0;
}
#endif

#ifdef NEW_CODENAME_ONE_VM
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_hasPublishPermissions___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return com_codename1_impl_ios_IOSNative_hasPublishPermissions__(me);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_askPublishPermissions___com_codename1_social_LoginCallback_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT callback) {
    return com_codename1_impl_ios_IOSNative_askPublishPermissions___com_codename1_social_LoginCallback(me, callback);
}

#endif