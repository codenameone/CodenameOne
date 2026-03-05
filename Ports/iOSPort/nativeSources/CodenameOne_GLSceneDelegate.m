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
#import "CodenameOne_GLSceneDelegate.h"

#ifdef CN1_USE_UI_SCENE
@implementation CodenameOne_GLSceneDelegate

@synthesize window=_window;

- (void)scene:(UIScene *)scene willConnectToSession:(UISceneSession *)session options:(UISceneConnectionOptions *)connectionOptions API_AVAILABLE(ios(13.0))
{
    if (![scene isKindOfClass:[UIWindowScene class]]) {
        return;
    }
    UIWindow *window = [[UIWindow alloc] initWithWindowScene:(UIWindowScene *)scene];
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1InstallRootViewControllerIntoWindow:window];
    self.window = window;
#ifndef CN1_USE_ARC
    [window release];
#endif

    UIOpenURLContext *urlContext = [connectionOptions.URLContexts anyObject];
    if (urlContext != nil) {
        [appDelegate cn1OpenURL:[UIApplication sharedApplication] url:urlContext.URL sourceApplication:urlContext.options.sourceApplication annotation:urlContext.options.annotation];
    }
    NSUserActivity *userActivity = [connectionOptions.userActivities anyObject];
    if (userActivity != nil) {
        [appDelegate cn1ContinueUserActivity:userActivity];
    }
}

- (void)sceneDidBecomeActive:(UIScene *)scene API_AVAILABLE(ios(13.0))
{
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1ApplicationDidBecomeActive];
}

- (void)sceneWillResignActive:(UIScene *)scene API_AVAILABLE(ios(13.0))
{
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1ApplicationWillResignActive];
}

- (void)sceneWillEnterForeground:(UIScene *)scene API_AVAILABLE(ios(13.0))
{
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1ApplicationWillEnterForeground];
}

- (void)sceneDidEnterBackground:(UIScene *)scene API_AVAILABLE(ios(13.0))
{
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1ApplicationDidEnterBackground];
}

- (void)scene:(UIScene *)scene openURLContexts:(NSSet<UIOpenURLContext *> *)URLContexts API_AVAILABLE(ios(13.0))
{
    UIOpenURLContext *urlContext = [URLContexts anyObject];
    if (urlContext == nil) {
        return;
    }
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1OpenURL:[UIApplication sharedApplication] url:urlContext.URL sourceApplication:urlContext.options.sourceApplication annotation:urlContext.options.annotation];
}

- (void)scene:(UIScene *)scene continueUserActivity:(NSUserActivity *)userActivity API_AVAILABLE(ios(13.0))
{
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1ContinueUserActivity:userActivity];
}

@end
#endif
