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

//GL_APP_DELEGATE_INCLUDE

extern UIView *editingComponent;

#define INCLUDE_CN1_PUSH

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
        JAVA_OBJECT o = com_codename1_ui_Display_getInstance__();
        JAVA_OBJECT key = fromNSString(@"AppArg");
        JAVA_OBJECT value;
        if([url isFileURL]) {
            value = fromNSString(url.path);
        } else {
            value = fromNSString([url absoluteString]);
        }
        com_codename1_ui_Display_setProperty___java_lang_String_java_lang_String(o, key, value);
    }
    com_codename1_impl_ios_IOSImplementation_callback__();
#ifdef INCLUDE_CN1_PUSH
    [[UIApplication sharedApplication] cancelAllLocalNotifications];
    [UIApplication sharedApplication].applicationIconBadgeNumber = 0;
    NSDictionary* userInfo = [launchOptions valueForKey:@"UIApplicationLaunchOptionsRemoteNotificationKey"];
    NSDictionary *apsInfo = [userInfo objectForKey:@"aps"];
    if(userInfo == nil || apsInfo == nil) {
        return YES;
    }
    NSLog(@"Received notification on start: %@", userInfo);
    if( [[userInfo valueForKey:@"aps"] valueForKey:@"alert"] != NULL)
    {
        NSString* alertValue = [[userInfo valueForKey:@"aps"] valueForKey:@"alert"];
        com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(fromNSString(alertValue), nil);
    }
    if( [userInfo valueForKey:@"meta"] != NULL)
    {
        NSString* alertValue = [userInfo valueForKey:@"meta"];
        com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(fromNSString(alertValue), fromNSString(@"2"));
    }
#endif

    //afterDidFinishLaunchingWithOptionsMarkerEntry
    return YES;
}

// required for URL opening
- (BOOL)application:(UIApplication *)application willFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    return YES;
}

- (BOOL)application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
    JAVA_OBJECT str1 = fromNSString([url absoluteString]);
    JAVA_OBJECT str2 = fromNSString(sourceApplication);
#ifdef NEW_CODENAME_ONE_VM
    return com_codename1_impl_ios_IOSImplementation_shouldApplicationHandleURL___java_lang_String_java_lang_String_R_boolean(str1, str2);
#else
    return com_codename1_impl_ios_IOSImplementation_shouldApplicationHandleURL___java_lang_String_java_lang_String(str1, str2);
#endif
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
    com_codename1_impl_ios_IOSImplementation_applicationWillResignActive__();
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
    com_codename1_impl_ios_IOSImplementation_applicationDidEnterBackground__();
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    /*
     Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
     */
    com_codename1_impl_ios_IOSImplementation_applicationWillEnterForeground__();
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    /*
     Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
     */
    //[self.viewController startAnimation];
    com_codename1_impl_ios_IOSImplementation_applicationDidBecomeActive__();
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    com_codename1_impl_ios_IOSImplementation_applicationWillTerminate__();
}

#ifdef INCLUDE_CN1_PUSH
- (void)application:(UIApplication*)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken {
    NSString * tokenAsString = [[[deviceToken description] stringByTrimmingCharactersInSet:[NSCharacterSet characterSetWithCharactersInString:@"<>"]] 
                stringByReplacingOccurrencesOfString:@" " withString:@""];
    JAVA_OBJECT str = fromNSString(tokenAsString);
    com_codename1_impl_ios_IOSImplementation_pushRegistered___java_lang_String(str);
}
 
- (void)application:(UIApplication*)application didFailToRegisterForRemoteNotificationsWithError:(NSError*)error {
	NSLog(@"Failed to get token, error: %@", error);
    JAVA_OBJECT str = fromNSString([error localizedDescription]);
    com_codename1_impl_ios_IOSImplementation_pushRegistrationError___java_lang_String(str);
}

- (void)application:(UIApplication*)application didReceiveRemoteNotification:(NSDictionary*)userInfo {
    NSLog(@"Received notification while running: %@", userInfo);
    if( [[userInfo valueForKey:@"aps"] valueForKey:@"alert"] != NULL)
    {
	NSString* alertValue = [[userInfo valueForKey:@"aps"] valueForKey:@"alert"];
        com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(fromNSString(alertValue), nil);
    }
    if( [userInfo valueForKey:@"meta"] != NULL)
    {
        NSString* alertValue = [userInfo valueForKey:@"meta"];
        com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(fromNSString(alertValue), fromNSString(@"2"));
    }
}
#endif

extern void repaintUI();

-(void)application:(UIApplication*)application didChangeStatusBarFrame:(CGRect)oldStatusBarFrame {
    repaintUI();
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
