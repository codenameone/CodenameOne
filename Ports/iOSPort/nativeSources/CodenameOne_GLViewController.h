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
#import "CN1ES2compat.h"
#import <UIKit/UIKit.h>

#import <OpenGLES/EAGL.h>
#import "EAGLView.h"
#import <OpenGLES/ES1/gl.h>
#import <OpenGLES/ES1/glext.h>
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES2/glext.h>
#import "ExecutableOp.h"
#import "PaintOp.h"
#import "GLUIImage.h"
#import <MessageUI/MFMailComposeViewController.h>
#import <MessageUI/MFMessageComposeViewController.h>
#import <CoreLocation/CoreLocation.h>
//#define CN1_USE_STOREKIT
#ifdef CN1_USE_STOREKIT
#import "StoreKit/StoreKit.h"
#endif
#import <AudioToolbox/AudioServices.h>
#import <AVFoundation/AVFoundation.h>
//#define CN1_BLOCK_SCREENSHOTS_ON_ENTER_BACKGROUND
//#define ENABLE_WKWEBVIEW
//#define NO_UIWEBVIEW
//#define GOOGLE_SIGNIN
//#define GOOGLE_CONNECT_PODS
//#define INCLUDE_GOOGLE_CONNECT
#ifndef GOOGLE_SIGNIN
#ifdef INCLUDE_GOOGLE_CONNECT
#ifdef GOOGLE_CONNECT_PODS
#import <GooglePlus/GooglePlus.h>
#else
#import "GooglePlus.h"
#endif
#endif
#else
#import <GoogleSignIn/GoogleSignIn.h>
#endif
//#define GLUIIMAGE_AUTOSCALE_LARGE_TEXTURES
//#define CN1_USE_JAVASCRIPTCORE
//#define ENABLE_GALLERY_MULTISELECT
//#define USE_PHOTOKIT_FOR_MULTIGALLERY
//#define INCLUDE_CONTACTS_USAGE
//#define INCLUDE_CALENDARS_USAGE
//#define INCLUDE_CAMERA_USAGE
//#define INCLUDE_FACEID_USAGE
//#define INCLUDE_LOCATION_USAGE
//#define INCLUDE_MICROPHONE_USAGE
//#define INCLUDE_MOTION_USAGE
//#define INCLUDE_PHOTOLIBRARYADD_USAGE
//#define INCLUDE_PHOTOLIBRARY_USAGE
//#define INCLUDE_REMINDERS_USAGE
//#define INCLUDE_SIRI_USAGE
//#define INCLUDE_SPEECHRECOGNITION_USAGE
//#define INCLUDE_NFCREADER_USAGE

//#define INCLUDE_CN1_BACKGROUND_FETCH
//#define INCLUDE_FACEBOOK_CONNECT
//#define USE_FACEBOOK_CONNECT_PODS
#ifdef INCLUDE_FACEBOOK_CONNECT
#ifdef USE_FACEBOOK_CONNECT_PODS
#import <FBSDKCoreKit/FBSDKCoreKit.h>
#else
#import "FBSDKCoreKit.h"
#import "FBSDKAppInviteDialog.h"
#endif
#endif

//#define CN1_HANDLE_UNIVERSAL_LINKS

#ifdef INCLUDE_PHOTOLIBRARY_USAGE
#ifdef ENABLE_GALLERY_MULTISELECT
#import "QBImagePickerController.h"

#ifdef USE_PHOTOKIT_FOR_MULTIGALLERY
#import "PhotosUI/PhotosUI.h"
#endif

#endif
#endif

#define NOT_INCLUDE_ZOOZ
#ifdef INCLUDE_ZOOZ
#import "ZooZ.h"
#endif

// Flag to enable experimental new keyboard handling.
#define CN1_NEW_KEYBOARD_HANDLING 1

//#define BACKGROUND_LOCATION_ENABLED
#define CN1_REQUEST_LOCATION_AUTH requestWhenInUseAuthorization

//#define CN1Log(str,...) printf([[NSString stringWithFormat:str,##__VA_ARGS__] UTF8String])
#define CN1Log(str,...) NSLog(str,##__VA_ARGS__)
#define IOS8_LOCATION_WARNING CN1Log(@"As of iOS8, location services requires the ios.locationUsageDescription build hint to be set.");
//#define CN1_ENABLE_BACKGROUND_LOCATION 1

//#define INCLUDE_MOPUB
#ifdef INCLUDE_MOPUB
#define MOPUB_AD_UNIT
#define MOPUB_AD_SIZE
#define MOPUB_TABLET_AD_UNIT
#define MOPUB_TABLET_AD_SIZE
#import "MPAdView.h"
#endif

#include "xmlvm.h"

#ifdef CN1_USE_ARC
#define POOL_BEGIN() 
#define POOL_END()
#define BRIDGE_CAST __bridge 
#else
#define BRIDGE_CAST
#define POOL_BEGIN() NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
#define POOL_END() [pool release];
#endif

#ifndef CN1_THREAD_STATE_SINGLE_ARG
#define CN1_THREAD_STATE_SINGLE_ARG 
#define CN1_THREAD_STATE_MULTI_ARG 
#define CN1_THREAD_STATE_PASS_ARG 
#define CN1_THREAD_STATE_PASS_SINGLE_ARG
#define CN1_THREAD_GET_STATE_PASS_ARG 
#define CN1_THREAD_GET_STATE_PASS_SINGLE_ARG 
#endif

#define CN1_SEG_MOVETO 0
#define CN1_SEG_LINETO 1
#define CN1_SEG_QUADTO 2
#define CN1_SEG_CUBICTO 3
#define CN1_SEG_CLOSE 4
#define CN1_JOIN_MITER 0
#define CN1_JOIN_ROUND 1
#define CN1_JOIN_BEVEL 2
#define CN1_CAP_BUTT 0
#define CN1_CAP_ROUND 1
#define CN1_CAP_SQUARE 2

#define EAGLVIEW [[CodenameOne_GLViewController instance] eaglView]

//ADD_INCLUDE

@interface CodenameOne_GLViewController : UIViewController<UIImagePickerControllerDelegate, MFMailComposeViewControllerDelegate, 
#ifdef CN1_USE_STOREKIT
SKProductsRequestDelegate, SKPaymentTransactionObserver, 
#endif
MFMessageComposeViewControllerDelegate, CLLocationManagerDelegate, AVAudioRecorderDelegate, UIActionSheetDelegate, UIPopoverControllerDelegate, UIPickerViewDelegate, UIDocumentInteractionControllerDelegate
#ifdef INCLUDE_ZOOZ
        ,ZooZPaymentCallbackDelegate
#endif
#ifdef INCLUDE_MOPUB
        ,MPAdViewDelegate
#endif
#ifdef INCLUDE_GOOGLE_CONNECT
#ifndef GOOGLE_SIGNIN
        ,GPPSignInDelegate
#else
        ,GIDSignInDelegate
#endif
#endif
#ifdef INCLUDE_PHOTOLIBRARY_USAGE
#ifdef ENABLE_GALLERY_MULTISELECT
        ,QBImagePickerControllerDelegate
#ifdef USE_PHOTOKIT_FOR_MULTIGALLERY
        ,PHPickerViewControllerDelegate
#endif
#endif
#endif
> {
@private
    EAGLContext *context;
    GLuint program;
    
    BOOL animating;
    NSInteger animationFrameInterval;

#ifdef CN1_USE_ARC
    __unsafe_unretained GLUIImage* currentMutableImage;
    __unsafe_unretained CADisplayLink *displayLink;
#else
    GLUIImage* currentMutableImage;
    CADisplayLink *displayLink;
#endif

    NSMutableArray* currentTarget;
    NSMutableArray* upcomingTarget;
    BOOL painted;
    BOOL drawTextureSupported;
    BOOL keyboardIsShown;
    BOOL modifiedViewHeight;

    //ADD_VARIABLES
}

#ifdef INCLUDE_MOPUB
@property (nonatomic, retain) MPAdView *adView;
#endif

@property (readonly, nonatomic, getter=isAnimating) BOOL animating;
@property (nonatomic) NSInteger animationFrameInterval;
@property (readwrite, assign) GLUIImage* currentMutableImage;
#ifdef GOOGLE_SIGNIN
- (void)signIn:(GIDSignIn *)signIn didSignInForUser:(GIDGoogleUser *)user withError:(NSError *)error;
- (void)signIn:(GIDSignIn *)signIn didDisconnectWithUser:(GIDGoogleUser *)user withError:(NSError *)error;
#endif

-(EAGLView*)eaglView;
-(void)startAnimation;
-(void)stopAnimation;
+(BOOL)isDrawTextureSupported;
-(void)initVars;

+(void)upcoming:(ExecutableOp*)op;
-(void)upcomingAdd:(ExecutableOp*)op;
-(BOOL)isPaintFinished;
-(void)flushBuffer:(UIImage *)buff x:(int)x y:(int)y width:(int)width height:(int)height;

-(void)drawString:(int)color alpha:(int)alpha font:(UIFont*)font str:(NSString*)str x:(int)x y:(int)y;
- (void)drawScreen;
- (void)drawFrame:(CGRect)rect;

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event;
-(void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event;
-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event;
-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event;

- (void)datePickerChangeDate:(UIDatePicker *)sender;
-(void)datePickerDismiss;
-(void)datePickerCancel;

+ (void)initialize;

+(CodenameOne_GLViewController*)instance;
-(void)upcomingAddClip:(ExecutableOp*)op;

+(BOOL)isCurrentMutableTransformSet;

+(CGAffineTransform) currentMutableTransform;
-(void)updateCanvas:(BOOL)animated;
@end
