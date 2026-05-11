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
#import <UIKit/UIKit.h>
#ifdef CN1_USE_UI_SCENE
#import <UIKit/UIScene.h>
#endif
//#define CN1_INCLUDE_NOTIFICATIONS
#ifdef CN1_INCLUDE_NOTIFICATIONS
#import <UserNotifications/UserNotifications.h>
#endif
// Legacy compatibility flag (off by default). When defined, the AppDelegate calls
// requestAuthorizationWithOptions in didFinishLaunchingWithOptions, restoring the
// pre-issue-#4876 behavior where the system notification prompt fires at launch.
// Enable from the build hint ios.notificationPermissionAtLaunch=true.
//#define CN1_NOTIFICATION_PERMISSION_AT_LAUNCH

@class CodenameOne_GLViewController;

@interface CodenameOne_GLAppDelegate : NSObject <UIApplicationDelegate
#ifdef CN1_INCLUDE_NOTIFICATIONS
,UNUserNotificationCenterDelegate
#endif
> {

}

@property (nonatomic, retain) IBOutlet UIWindow *window;

@property (nonatomic, retain) IBOutlet CodenameOne_GLViewController *viewController;

#ifdef CN1_USE_UI_SCENE
- (void)cn1InstallRootViewControllerIntoWindow:(UIWindow *)window;
- (void)cn1ApplicationWillResignActive;
- (void)cn1ApplicationDidEnterBackground;
- (void)cn1ApplicationWillEnterForeground;
- (void)cn1ApplicationDidBecomeActive;
- (BOOL)cn1ContinueUserActivity:(NSUserActivity *)userActivity;
- (BOOL)cn1OpenURL:(UIApplication *)application
               url:(NSURL *)url
 sourceApplication:(NSString *)sourceApplication
        annotation:(id)annotation;
#endif

@end
