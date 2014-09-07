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

//#define INCLUDE_FACEBOOK
#ifdef INCLUDE_FACEBOOK
#include "com_codename1_social_FacebookConnect.h"
#include "com_codename1_social_LoginCallback.h"
#include "com_codename1_social_FacebookImpl.h"
#import "FBSession.h"


#ifdef NEW_CODENAME_ONE_VM
extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str);
#else
extern JAVA_OBJECT fromNSString(NSString* str);
#endif

void com_codename1_impl_ios_IOSNative_facebookLogin___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT instance) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        FBSession* s = [FBSession activeSession];
        if(s == nil || !s.isOpen) {
            [FBSession openActiveSessionWithReadPermissions:@[@"public_profile", @"email", @"user_friends"] allowLoginUI:YES
                                completionHandler:^(FBSession *session,
                                           FBSessionState status,
                                           NSError *error) {
                [FBSession setActiveSession:session];
                if(status == FBSessionStateClosedLoginFailed || status == FBSessionStateOpen) {
#ifdef NEW_CODENAME_ONE_VM
                    set_field_com_codename1_social_FacebookImpl_loginCompleted(threadStateData, JAVA_TRUE, instance);
#else
                    com_codename1_social_FacebookImpl* impl = (com_codename1_social_FacebookImpl*)instance;
                    impl->fields.com_codename1_social_FacebookImpl.loginCompleted_ = TRUE;
#endif
                    return;
                }
            }];
        }
        POOL_END();
    });
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isFacebookLoggedIn__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    __block JAVA_BOOLEAN val = FALSE;
    dispatch_sync(dispatch_get_main_queue(), ^{
        FBSession* s = [FBSession activeSession];
        val = s != nil && s.isOpen;
    });
    return val;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getFacebookToken__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    __block JAVA_OBJECT str = JAVA_NULL;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSString *accessToken = [[[FBSession activeSession] accessTokenData] accessToken];
        str = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG accessToken);
        POOL_END();
    });
    return str;
}

void com_codename1_impl_ios_IOSNative_facebookLogout__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        [FBSession.activeSession closeAndClearTokenInformation];
    });
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_askPublishPermissions___com_codename1_social_LoginCallback(JAVA_OBJECT me, JAVA_OBJECT callback) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        FBSession* s = [FBSession activeSession];
        if(s != nil && s.isOpen) {
            NSArray *permissions = @[@"publish_actions"];
            [s requestNewPublishPermissions:permissions
                            defaultAudience:FBSessionDefaultAudienceEveryone
                          completionHandler:^(FBSession *session,
                                              NSError *error) {
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
        } else {
            [FBSession openActiveSessionWithPublishPermissions:[NSArray arrayWithObjects:@"publish_actions", nil] defaultAudience:FBSessionDefaultAudienceEveryone allowLoginUI:YES completionHandler:^(FBSession *session, FBSessionState state, NSError *error) {
#ifdef NEW_CODENAME_ONE_VM
                if(error) {
                    if(callback != JAVA_NULL) {
                        virtual_com_codename1_social_LoginCallback_loginFailed___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG callback, JAVA_NULL);
                    }
                    return;
                }
                if (FBSession.activeSession.isOpen) {
                    if(callback != JAVA_NULL) {
                        virtual_com_codename1_social_LoginCallback_loginSuccessful__(CN1_THREAD_GET_STATE_PASS_ARG callback);
                    }
                    publishPermission = 1;
                }
#else
                    if(error) {
                        if(callback != JAVA_NULL) {
                            //com_codename1_social_LoginCallback_loginFailed___java_lang_String(callback, JAVA_NULL);
                            (*(void (*)(JAVA_OBJECT, JAVA_OBJECT)) ((com_codename1_social_LoginCallback*) callback)->tib->vtable[6])(callback, JAVA_NULL);
                        }
                        return;
                    }
                    if (FBSession.activeSession.isOpen) {
                        if(callback != JAVA_NULL) {
                            (*(void (*)(JAVA_OBJECT)) ((com_codename1_social_LoginCallback*) callback)->tib->vtable[7])(callback);
                            //com_codename1_social_LoginCallback_loginSuccessful__(callback);
                        }
                        publishPermission = 1;
                    }
#endif
                }];
        }
        POOL_END();
    });
    return publishPermission;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_hasPublishPermissions__(JAVA_OBJECT me) {
    return publishPermission;
}

#else

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