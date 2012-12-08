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

extern UIView *editingComponent;


@implementation CodenameOne_GLAppDelegate


@synthesize window=_window;

@synthesize viewController=_viewController;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    // Override point for customization after application launch.
    self.window.rootViewController = self.viewController;
    com_codename1_impl_ios_IOSImplementation_callback__();
    return YES;
}

// required for URL opening
- (BOOL)application:(UIApplication *)application willFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    return YES;
}

- (BOOL)application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
    java_lang_String* str1 = fromNSString(url);
    java_lang_String* str2 = fromNSString(sourceApplication);
    return com_codename1_impl_ios_IOSImplementation_shouldApplicationHandleURL___java_lang_String_java_lang_String(str1, str2);
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
        [editingComponent release];
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
    
    /*
     Called when the application is about to terminate.
     Save data if appropriate.
     See also applicationDidEnterBackground:.
     */
//    [self.viewController stopAnimation];
    com_codename1_impl_ios_IOSImplementation_applicationWillTerminate__();
}

- (void)application:(UIApplication*)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken {
    NSString * tokenAsString = [[[deviceToken description] stringByTrimmingCharactersInSet:[NSCharacterSet characterSetWithCharactersInString:@"<>"]] 
                stringByReplacingOccurrencesOfString:@" " withString:@""];
    java_lang_String* str = fromNSString(tokenAsString);
    com_codename1_impl_ios_IOSImplementation_pushRegistered___java_lang_String(str);
}
 
- (void)application:(UIApplication*)application didFailToRegisterForRemoteNotificationsWithError:(NSError*)error {
	NSLog(@"Failed to get token, error: %@", error);
    java_lang_String* str = fromNSString([error localizedDescription]);
    com_codename1_impl_ios_IOSImplementation_pushRegistrationError___java_lang_String(str);
}

- (void)application:(UIApplication*)application didReceiveRemoteNotification:(NSDictionary*)userInfo {
	NSLog(@"Received notification: %@", userInfo);
	NSString* alertValue = [[userInfo valueForKey:@"aps"] valueForKey:@"alert"];
        com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String(fromNSString(alertValue));
}

- (void)dealloc
{
    [_window release];
    [_viewController release];
    [super dealloc];
}

@end
