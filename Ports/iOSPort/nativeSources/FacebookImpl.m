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

//#define INCLUDE_FACEBOOK
#ifdef INCLUDE_FACEBOOK
#include "com_codename1_social_FacebookImpl.h"
#import "FBSession.h"


extern JAVA_OBJECT fromNSString(NSString* str);

void com_codename1_impl_ios_IOSNative_facebookLogin___java_lang_Object(JAVA_OBJECT me, JAVA_OBJECT instance) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        FBSession* s = [FBSession activeSession];
        if(s == nil || !s.isOpen) {
            [FBSession openActiveSessionWithReadPermissions:@[@"basic_info"] allowLoginUI:YES
                                completionHandler:^(FBSession *session,
                                           FBSessionState status,
                                           NSError *error) {
                [FBSession setActiveSession:session];
                if(status == FBSessionStateClosedLoginFailed || status == FBSessionStateOpen) {
                    com_codename1_social_FacebookImpl* impl = (com_codename1_social_FacebookImpl*)instance;
                    impl->fields.com_codename1_social_FacebookImpl.loginCompleted_ = TRUE;
                    return;
                }
            }];
        }
        POOL_END();
    });
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isFacebookLoggedIn__(JAVA_OBJECT me) {
    __block JAVA_BOOLEAN val = FALSE;
    dispatch_sync(dispatch_get_main_queue(), ^{
        FBSession* s = [FBSession activeSession];
        val = s != nil && s.isOpen;
    });
    return val;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getFacebookToken__(JAVA_OBJECT me) {
    __block JAVA_OBJECT str = JAVA_NULL;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSString *accessToken = [[[FBSession activeSession] accessTokenData] accessToken];
        str = fromNSString(accessToken);
        POOL_END();
    });
    return str;
}

void com_codename1_impl_ios_IOSNative_facebookLogout__(JAVA_OBJECT me) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        [FBSession.activeSession closeAndClearTokenInformation];
    });
}

#else

void com_codename1_impl_ios_IOSNative_facebookLogin___java_lang_Object(JAVA_OBJECT me, JAVA_OBJECT instance) {
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isFacebookLoggedIn__(JAVA_OBJECT me) {
    return FALSE;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getFacebookToken__(JAVA_OBJECT me) {
    return JAVA_NULL;
}

void com_codename1_impl_ios_IOSNative_facebookLogout__(JAVA_OBJECT me) {
}
#endif