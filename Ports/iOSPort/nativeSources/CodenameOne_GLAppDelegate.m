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
#import "CodenameOne_GLAppDelegate.h"
#include "xmlvm.h"
#import "EAGLView.h"
#import "CodenameOne_GLViewController.h"
#include "com_codename1_impl_ios_IOSImplementation.h"
#ifdef NEW_CODENAME_ONE_VM
#include "java_lang_System.h"
int mallocWhileSuspended = 0;
#endif



BOOL isAppSuspended = NO;
//GL_APP_DELEGATE_IMPORT
//GL_APP_DELEGATE_INCLUDE

extern UIView *editingComponent;

#define INCLUDE_CN1_PUSH


#ifdef INCLUDE_GOOGLE_CONNECT
#import "GooglePlus.h"
#endif

#ifdef INCLUDE_FACEBOOK_CONNECT
#import "FBSDKCoreKit.h"
#endif


@implementation CodenameOne_GLAppDelegate


@synthesize window=_window;

@synthesize viewController=_viewController;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    //beforeDidFinishLaunchingWithOptionsMarkerEntry
    
    // Override point for customization after application launch.
    self.window.rootViewController = self.viewController;
    NSURL *url = (NSURL *)[launchOptions valueForKey:UIApplicationLaunchOptionsURLKey];
    if(url != nil) {
        JAVA_OBJECT o = com_codename1_ui_Display_getInstance__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        JAVA_OBJECT key = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"AppArg");
        JAVA_OBJECT value;
        if([url isFileURL]) {
            value = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG url.path);
        } else {
            value = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [url absoluteString]);
        }
        com_codename1_ui_Display_setProperty___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG o, key, value);
    }
    com_codename1_impl_ios_IOSImplementation_callback__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    
    if (launchOptions[UIApplicationLaunchOptionsLocalNotificationKey]) {
        NSLog(@"Background notification received");
        UILocalNotification *notification = launchOptions[UIApplicationLaunchOptionsLocalNotificationKey];
        com_codename1_impl_ios_IOSImplementation_localNotificationReceived___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [notification.userInfo valueForKey:@"__ios_id__"]));
        application.applicationIconBadgeNumber = 0;
    }
    
    id locationValue = [launchOptions objectForKey:UIApplicationLaunchOptionsLocationKey];
    if (locationValue) {
        com_codename1_impl_ios_IOSImplementation_appDidLaunchWithLocation__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    }
    
#ifdef INCLUDE_CN1_PUSH
    //[[UIApplication sharedApplication] cancelAllLocalNotifications]; // <-- WHY IS THIS HERE? -- removing it for now
    [UIApplication sharedApplication].applicationIconBadgeNumber = 0;
    if(launchOptions == nil) {
        //afterDidFinishLaunchingWithOptionsMarkerEntry
        return YES;
    }
    NSDictionary* userInfo = [launchOptions valueForKey:@"UIApplicationLaunchOptionsRemoteNotificationKey"];
    if(userInfo == nil) {
        //afterDidFinishLaunchingWithOptionsMarkerEntry
        return YES;
    }
    NSDictionary *apsInfo = [userInfo objectForKey:@"aps"];
    if(apsInfo == nil) {
        //afterDidFinishLaunchingWithOptionsMarkerEntry
        return YES;
    }
    NSLog(@"Received notification on start: %@", userInfo);
    if( [[userInfo valueForKey:@"aps"] valueForKey:@"alert"] != NULL)
    {
        NSString* alertValue = [[userInfo valueForKey:@"aps"] valueForKey:@"alert"];
        com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG alertValue), nil);
    }
    if( [userInfo valueForKey:@"meta"] != NULL)
    {
        NSString* alertValue = [userInfo valueForKey:@"meta"];
        com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG alertValue), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"2"));
    }
#endif

    //afterDidFinishLaunchingWithOptionsMarkerEntry
    
#ifdef INCLUDE_FACEBOOK_CONNECT
    return [[FBSDKApplicationDelegate sharedInstance] application:application
                                    didFinishLaunchingWithOptions:launchOptions];
#else
    return YES;
#endif
}

// implemented this way so this will compile on older versions of xcode
- (void)application:(UIApplication *)application didRegisterUserNotificationSettings:(id)notificationSettings {
    Class uiApp = NSClassFromString(@"UIApplication");
    UIApplication* uiAppInstance = [UIApplication sharedApplication];
    SEL sel = NSSelectorFromString(@"registerForRemoteNotifications");
    //[[UIApplication sharedApplication] registerForRemoteNotifications];
    NSMethodSignature *signature = [uiAppInstance methodSignatureForSelector:sel];
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:signature];
    invocation.selector = sel;
    invocation.target = uiApp;
    [invocation invokeWithTarget:uiAppInstance];
}

// required for URL opening
- (BOOL)application:(UIApplication *)application willFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    isAppSuspended = NO;
    if(launchOptions != nil) {
        NSURL *url = (NSURL *)[launchOptions valueForKey:UIApplicationLaunchOptionsURLKey];
        if(url != nil) {
            JAVA_OBJECT o = com_codename1_ui_Display_getInstance__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
            JAVA_OBJECT key = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"AppArg");
            JAVA_OBJECT value;
            if([url isFileURL]) {
                value = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG url.path);
            } else {
                value = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [url absoluteString]);
            }
            com_codename1_ui_Display_setProperty___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG o, key, value);
        }
    }
    return YES;
}

- (BOOL)application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
    JAVA_OBJECT str1 = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [url absoluteString]);
    JAVA_OBJECT str2 = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG sourceApplication);
    
#ifdef INCLUDE_GOOGLE_CONNECT
    
    // Handle Google Plus Login
    BOOL res = [GPPURLHandler handleURL:url
           sourceApplication:sourceApplication
                  annotation:annotation];
    if (res) {
        return res;
    }
    
#endif
#ifdef INCLUDE_FACEBOOK_CONNECT
    BOOL fbRes = [[FBSDKApplicationDelegate sharedInstance] application:application
                                                          openURL:url
                                                sourceApplication:sourceApplication
                                                       annotation:annotation];
    if (fbRes) {
        return fbRes;
    }
#endif
    
#ifdef NEW_CODENAME_ONE_VM
    JAVA_BOOLEAN b = com_codename1_impl_ios_IOSImplementation_shouldApplicationHandleURL___java_lang_String_java_lang_String_R_boolean(CN1_THREAD_GET_STATE_PASS_ARG str1, str2);
#else
    JAVA_BOOLEAN b = com_codename1_impl_ios_IOSImplementation_shouldApplicationHandleURL___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG str1, str2);
#endif

    return b;
}

- (BOOL)application:(UIApplication *)application handleOpenURL:(NSURL *)url
{
  return [self application:application openURL:url sourceApplication:nil annotation:nil];
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    /*
     Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
     Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
     */
    com_codename1_impl_ios_IOSImplementation_applicationWillResignActive__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    //[self.viewController stopAnimation];
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    if(editingComponent != nil) {
        [editingComponent resignFirstResponder];
        [editingComponent removeFromSuperview];
#ifndef CN1_USE_ARC
        [editingComponent release];
#endif
        editingComponent = nil;
    }
    /*
     Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
     If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
     */
    com_codename1_impl_ios_IOSImplementation_applicationDidEnterBackground__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    //----application_will_resign_active
    isAppSuspended = YES;
#ifdef NEW_CODENAME_ONE_VM
    java_lang_System_stopGC__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    mallocWhileSuspended = 0;
#endif
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    /*
     Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
     */
    com_codename1_impl_ios_IOSImplementation_applicationWillEnterForeground__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
#ifdef INCLUDE_CN1_PUSH
     [UIApplication sharedApplication].applicationIconBadgeNumber = 0;
#endif
    /*
     Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
     */
    //[self.viewController startAnimation];
    com_codename1_impl_ios_IOSImplementation_applicationDidBecomeActive__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    //DELEGATE_applicationDidBecomeActive
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    com_codename1_impl_ios_IOSImplementation_applicationWillTerminate__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

#ifdef INCLUDE_CN1_PUSH
- (void)application:(UIApplication*)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken {
    NSString * tokenAsString = [[[deviceToken description] stringByTrimmingCharactersInSet:[NSCharacterSet characterSetWithCharactersInString:@"<>"]] 
                stringByReplacingOccurrencesOfString:@" " withString:@""];
    JAVA_OBJECT str = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG tokenAsString);
    com_codename1_impl_ios_IOSImplementation_pushRegistered___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG str);
}
 
- (void)application:(UIApplication*)application didFailToRegisterForRemoteNotificationsWithError:(NSError*)error {
	NSLog(@"Failed to get token, error: %@", error);
    JAVA_OBJECT str = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [error localizedDescription]);
    com_codename1_impl_ios_IOSImplementation_pushRegistrationError___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG str);
}

- (void)application:(UIApplication*)application didReceiveRemoteNotification:(NSDictionary*)userInfo {
    NSLog(@"Received notification while running: %@", userInfo);
    if( [[userInfo valueForKey:@"aps"] valueForKey:@"alert"] != NULL)
    {
	NSString* alertValue = [[userInfo valueForKey:@"aps"] valueForKey:@"alert"];
        com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG alertValue), nil);
    }
    if( [userInfo valueForKey:@"meta"] != NULL)
    {
        NSString* alertValue = [userInfo valueForKey:@"meta"];
        com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG alertValue), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"2"));
    }
}
#endif

extern void repaintUI();

-(void)application:(UIApplication*)application didChangeStatusBarFrame:(CGRect)oldStatusBarFrame {
    repaintUI();
}

- (void)application:(UIApplication*)application didReceiveLocalNotification:(UILocalNotification*)notification {
    NSLog(@"Received local notification while running: %@", notification);
    if( [notification.userInfo valueForKey:@"__ios_id__"] != NULL)
    {
        NSString* alertValue = [notification.userInfo valueForKey:@"__ios_id__"];
        com_codename1_impl_ios_IOSImplementation_localNotificationReceived___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG alertValue));
    }
}

#ifndef CN1_USE_ARC
- (void)dealloc
{
    [_window release];
    [_viewController release];
    [super dealloc];
}
#endif

//GL_APP_DELEGATE_BODY
@end
