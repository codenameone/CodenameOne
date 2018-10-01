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
#import "CN1TapGestureRecognizer.h"
#include "com_codename1_impl_ios_IOSImplementation.h"
#include "com_codename1_push_PushContent.h"
#include "com_codename1_ui_Display.h"
#ifdef NEW_CODENAME_ONE_VM
#include "java_lang_System.h"
int mallocWhileSuspended = 0;
#endif

extern BOOL isIOS10();
int pendingRemoteNotificationRegistrations = 0;

BOOL isAppSuspended = NO;
//GL_APP_DELEGATE_IMPORT
//GL_APP_DELEGATE_INCLUDE

extern UIView *editingComponent;

#define INCLUDE_CN1_PUSH

#ifdef INCLUDE_CN1_PUSH
#include "com_codename1_push_PushContent.h"
#endif

#ifdef INCLUDE_GOOGLE_CONNECT
#ifndef GOOGLE_SIGNIN
#ifdef GOOGLE_CONNECT_PODS
#import <GooglePlus/GooglePlus.h>
#else
#import "GooglePlus.h"
#endif
#else
#import <GoogleSignIn/GoogleSignIn.h>
#endif

#endif

#ifdef INCLUDE_FACEBOOK_CONNECT
#ifdef USE_FACEBOOK_CONNECT_PODS
#import <FBSDKCoreKit/FBSDKCoreKit.h>
#else
#import "FBSDKCoreKit.h"
#endif
#endif

#import "java_lang_NullPointerException.h"
#import "java_lang_RuntimeException.h"

// A signal handler to handle bad accesses.  This will throw NPEs that we can catch
// rather than crashing the app.
// See http://www.cocoawithlove.com/2010/05/handling-unhandled-exceptions-and.html
//
// NOTE: This handler WILL NOT WORK while using the debugger
static void SignalHandler(int sig)
{
    if (sig == 11) {
        // We received an EXEC_BAD_ACCESS.  This generally happens if we try to use an object
        // that is null, so let's convert it into a null pointer exception.
        throwException(getThreadLocalData(),  __NEW_INSTANCE_java_lang_NullPointerException(getThreadLocalData()));
    } else {
        // We received one of the other kinds of signals.  So let's raise it as a RuntimeException
        throwException(getThreadLocalData(), __NEW_INSTANCE_java_lang_RuntimeException(getThreadLocalData()));
    }
    // Log something just in case the exception handling is foobar'd
    NSLog(@"We had a signal %d", sig);
    //signal(sig, SIG_DFL);
}

static void installSignalHandlers() {
    signal(SIGABRT, SignalHandler);
    signal(SIGILL, SignalHandler);
    signal(SIGSEGV, SignalHandler);
    signal(SIGFPE, SignalHandler);
    signal(SIGBUS, SignalHandler);
    signal(SIGPIPE, SignalHandler);

}


@implementation CodenameOne_GLAppDelegate


@synthesize window=_window;

@synthesize viewController=_viewController;




- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    //beforeDidFinishLaunchingWithOptionsMarkerEntry
    
    // Override point for customization after application launch.
    
    // Install signal handlers so that rather than the app crashing upon a BAD_ACCESS, the 
    // app will throw an NPE.
    installSignalHandlers();
    self.window.rootViewController = self.viewController;
    CN1TapGestureRecognizer* recognizer = [[CN1TapGestureRecognizer alloc] initWithTarget:nil action:nil];
    [recognizer install:self.viewController];
    [recognizer release];
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
        CN1Log(@"Background notification received");
        UILocalNotification *notification = launchOptions[UIApplicationLaunchOptionsLocalNotificationKey];
        com_codename1_impl_ios_IOSImplementation_localNotificationReceived___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [notification.userInfo valueForKey:@"__ios_id__"]));
        application.applicationIconBadgeNumber = 0;
    }
    
    id locationValue = [launchOptions objectForKey:UIApplicationLaunchOptionsLocationKey];
    if (locationValue) {
        com_codename1_impl_ios_IOSImplementation_appDidLaunchWithLocation__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    }
    
#ifdef INCLUDE_CN1_BACKGROUND_FETCH
    [application setMinimumBackgroundFetchInterval:UIApplicationBackgroundFetchIntervalMinimum];
#endif
    
#ifdef INCLUDE_CN1_PUSH
    if (@available(iOS 10, *)) {
        if (isIOS10()) {
            UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
            center.delegate = self;
            
            com_codename1_impl_ios_IOSImplementation_initPushActionCategories__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        } 
    }
    
    //[[UIApplication sharedApplication] cancelAllLocalNotifications]; // <-- WHY IS THIS HERE? -- removing it for now
    [UIApplication sharedApplication].applicationIconBadgeNumber = 0;
    if(launchOptions == nil) {
        //afterDidFinishLaunchingWithOptionsMarkerEntry
        return YES;
    }
    NSDictionary* userInfo = [launchOptions valueForKey:@"UIApplicationLaunchOptionsRemoteNotificationKey"];
    [self cn1RoutePush:userInfo];
    
    
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
    if (pendingRemoteNotificationRegistrations > 0) {
        pendingRemoteNotificationRegistrations--;
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
#ifndef GOOGLE_SIGNIN
    // Handle Google Plus Login
    BOOL res = [GPPURLHandler handleURL:url
           sourceApplication:sourceApplication
                  annotation:annotation];
    if (res) {
        return res;
    }
#else
    BOOL res = [[GIDSignIn sharedInstance] handleURL:url sourceApplication:sourceApplication
                                           annotation:annotation];
    if (res) {
        return res;
    }
#endif
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
    
    //openURLMarkerEntry
    
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

#ifdef INCLUDE_CN1_BACKGROUND_FETCH
typedef void (^CN1BackgroundFetchBlockType)(UIBackgroundFetchResult);
CN1BackgroundFetchBlockType cn1UIBackgroundFetchResultCompletionHandler = 0;
-(void)application:(UIApplication *)application performFetchWithCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler{
    cn1UIBackgroundFetchResultCompletionHandler = Block_copy(completionHandler);
    com_codename1_impl_ios_IOSImplementation_performBackgroundFetch__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}
#endif

#ifdef INCLUDE_CN1_PUSH
UNNotificationResponse* currentNotificationResponse = nil;
- (void)application:(UIApplication*)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken {
    NSString * tokenAsString = [[[deviceToken description] stringByTrimmingCharactersInSet:[NSCharacterSet characterSetWithCharactersInString:@"<>"]] 
                stringByReplacingOccurrencesOfString:@" " withString:@""];
    JAVA_OBJECT str = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG tokenAsString);
    com_codename1_impl_ios_IOSImplementation_pushRegistered___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG str);
}
 
- (void)application:(UIApplication*)application didFailToRegisterForRemoteNotificationsWithError:(NSError*)error {
	CN1Log(@"Failed to get token, error: %@", error);
    JAVA_OBJECT str = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [error localizedDescription]);
    com_codename1_impl_ios_IOSImplementation_pushRegistrationError___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG str);
}

- (void)application:(UIApplication*)application didReceiveRemoteNotification:(NSDictionary*)userInfo {
    CN1Log(@"Received notification while running: %@", userInfo);
    [self cn1RoutePush:userInfo];
}
-(void)cn1RoutePush:(NSDictionary*)userInfo {
    [self cn1RoutePush:userInfo withAction:nil withCompletionHandler:nil];
}
-(void)cn1RoutePush:(NSDictionary*)userInfo withAction:(NSString*)actionId {
    [self cn1RoutePush:userInfo withAction:actionId withCompletionHandler:nil];
}
typedef void (^CN1PushCompletionHandlerType)();
CN1PushCompletionHandlerType cn1PushCompletionHandler = 0;
int pushReceivedCount=0;
-(void)cn1RoutePush:(NSDictionary*)userInfo withAction:(NSString*)actionId withCompletionHandler:(void (^)())completionHandler{
    NSDictionary *apsInfo = [userInfo objectForKey:@"aps"];
    if(apsInfo == nil) {
        //afterDidFinishLaunchingWithOptionsMarkerEntry
        if (completionHandler != nil) {
            completionHandler();
        }
        return;
    }
    com_codename1_push_PushContent_reset__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    
    BOOL pushIncludedBody = NO;
    if ([userInfo valueForKey:@"media-url"] != NULL) {
        com_codename1_push_PushContent_setImageUrl___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [userInfo valueForKey:@"media-url"]));
    }
    if ([userInfo valueForKey:@"meta"] != NULL) {
        com_codename1_push_PushContent_setMetaData___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [userInfo valueForKey:@"meta"]));
    }
    if ([apsInfo valueForKey:@"category"] != NULL) {
        com_codename1_push_PushContent_setCategory___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [apsInfo valueForKey:@"category"]));
    }
    if (actionId != nil) {
        com_codename1_push_PushContent_setActionId___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG actionId));
        if (currentNotificationResponse != nil && [currentNotificationResponse isKindOfClass:[UNTextInputNotificationResponse class]]) {
            UNTextInputNotificationResponse* textResponse = (UNTextInputNotificationResponse*)currentNotificationResponse;
            if (textResponse.userText != nil) {
                com_codename1_push_PushContent_setTextResponse___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG textResponse.userText));
            }
        }
    }
    pushReceivedCount=0;
    if( [apsInfo valueForKey:@"alert"] != NULL)
    {
        pushIncludedBody = YES;
        id alertValue0 = [[userInfo valueForKey:@"aps"] valueForKey:@"alert"];
        NSString *alertValue = nil;
        
        if ([alertValue0 isKindOfClass:[NSDictionary class]]) {
            NSDictionary *alertValueD = (NSDictionary*)alertValue0;
            if ([alertValueD valueForKey:@"title"] != NULL && [alertValueD valueForKey:@"body"] != NULL) {
                com_codename1_push_PushContent_setTitle___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [alertValueD valueForKey:@"title"]));
                com_codename1_push_PushContent_setBody___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [alertValueD valueForKey:@"body"]));
                alertValue = [NSString stringWithFormat:@"%@;%@", [alertValueD valueForKey:@"title"], [alertValueD valueForKey:@"body"]];
                if (completionHandler != nil) {
                    cn1PushCompletionHandler = Block_copy(completionHandler);
                }
                pushReceivedCount++;
                com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG alertValue), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"4"));
            } else {
                CN1Log(@"Received push type 4 but missing either title or body");
            }
            
        } else {
            alertValue = (NSString*)alertValue0;
            // Find out the push type
            NSString *pushType = @"1";
            com_codename1_push_PushContent_setBody___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG alertValue));
            if ([userInfo valueForKey:@"meta"] != NULL) {
                // If there was a meta argument, then this is a type 3 push
                if (completionHandler != nil) {
                    cn1PushCompletionHandler = Block_copy(completionHandler);
                }
                pushReceivedCount++;
                com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG alertValue), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"3"));
            } else {
                // If there was no meta argument, then this is a type 1
                if (completionHandler != nil) {
                    cn1PushCompletionHandler = Block_copy(completionHandler);
                }
                pushReceivedCount++;
                com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG alertValue), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"1"));
            }
        }
    }
    if( [userInfo valueForKey:@"meta"] != NULL)
    {
        NSString* alertValue = [userInfo valueForKey:@"meta"];
        if (pushIncludedBody) {
            if (completionHandler != nil) {
                cn1PushCompletionHandler = Block_copy(completionHandler);
            }
            pushReceivedCount++;
            // If the push included a body, then this is a type 3 push (we don't need to set type here because it was set when the body was sent to the push callback)
            com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG alertValue), nil);
        } else {
            // If the push did not include a body, then it is a type 2 push
            if (completionHandler != nil) {
                cn1PushCompletionHandler = Block_copy(completionHandler);
            }
            pushReceivedCount++;
            com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG alertValue), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"2"));
        }
    }
    if (pushReceivedCount == 0 && completionHandler != nil) {
        completionHandler();
    }
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification withCompletionHandler:(void (^)(UNNotificationPresentationOptions options))completionHandler {
    NSLog( @"Handle push from foreground" );
    // custom code to handle push while app is in the foreground
    NSLog(@"%@", notification.request.content.userInfo);
    NSDictionary *userInfo = notification.request.content.userInfo;
    [self cn1RoutePush:userInfo];
    
}


- (void)userNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response withCompletionHandler:(void (^)())completionHandler {
    NSLog( @"Handle push from background or closed" );
    // if you set a member variable in didReceiveRemoteNotification, you  will know if this is from closed or background
    NSLog(@"%@", response.notification.request.content.userInfo);
    currentNotificationResponse = response;
    [self cn1RoutePush:response.notification.request.content.userInfo withAction:response.actionIdentifier withCompletionHandler:completionHandler];
    
    // TODO:  Need to pass the completion handler somehow to the push callback to be called after that
    // For now this hack to buy the EDT some time to run the push callback.
    
    //[NSTimer scheduledTimerWithTimeInterval:1000 repeats:NO block:^(NSTimer *timer) {
    //    completionHandler();
    //}];
}



#endif

extern void repaintUI();

-(void)application:(UIApplication*)application didChangeStatusBarFrame:(CGRect)oldStatusBarFrame {
    repaintUI();
}

- (void)application:(UIApplication*)application didReceiveLocalNotification:(UILocalNotification*)notification {
    CN1Log(@"Received local notification while running: %@", notification);
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
