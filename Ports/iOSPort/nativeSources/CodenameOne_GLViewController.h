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

void cn1RunSyncOnMainQueue(void (^block)(void));
// MessageUI (mail/SMS composer) is unavailable on watchOS, and on tvOS it ships
// only a link stub with no composer headers; the email/SMS native methods are
// guarded the same way in IOSNative.m, and the matching delegate conformances
// below are likewise dropped on those slices.
#if !TARGET_OS_WATCH && !TARGET_OS_TV
#import <MessageUI/MFMailComposeViewController.h>
#import <MessageUI/MFMessageComposeViewController.h>
#endif
#import <CoreLocation/CoreLocation.h>
//#define CN1_USE_STOREKIT
//#define CN1_USE_APPREVIEW
#if defined(CN1_USE_STOREKIT) || defined(CN1_USE_APPREVIEW)
#import "StoreKit/StoreKit.h"
#endif
#if !TARGET_OS_WATCH
#import <AudioToolbox/AudioServices.h>
#endif
#import <AVFoundation/AVFoundation.h>
//#define CN1_BLOCK_SCREENSHOTS_ON_ENTER_BACKGROUND
//#define ENABLE_WKWEBVIEW
//#define NO_UIWEBVIEW
//#define GOOGLE_SIGNIN
//#define GOOGLE_CONNECT_PODS
//#define INCLUDE_GOOGLE_CONNECT
#if TARGET_OS_WATCH
// Neither Google SDK ships a watchOS slice, and the watch app cannot present a sign-in web flow
// anyway. Both defines are switched on by the builder EDITING THIS FILE, so they arrive on every
// slice of the project at once -- the watch translation stages the same header and then failed to
// build with "'GoogleSignIn/GoogleSignIn.h' file not found", from a native source the watch never
// calls into. Undefining here rather than guarding each import turns off the implementation blocks
// in the .m files too, since every one of them is gated on the same two macros.
#undef GOOGLE_SIGNIN
#undef INCLUDE_GOOGLE_CONNECT
#endif
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
// Flipped automatically by IPhoneBuilder from the matching
// ios.NSHealth*UsageDescription build hints, via the generic
// NS<Key>UsageDescription -> INCLUDE_<KEY>_USAGE rule.
//#define INCLUDE_HEALTHSHARE_USAGE
//#define INCLUDE_HEALTHUPDATE_USAGE

// CN1_INCLUDE_NFC gates the com.codename1.nfc native bridge (CoreNFC.framework
// import, NFCNDEFReaderSession / NFCTagReaderSession code). IPhoneBuilder
// uncomments this only when the classpath scanner saw com.codename1.nfc.*,
// so apps that never touch NFC ship without any CoreNFC symbols and pass
// Apple's API-usage scan without declaring an NFC privacy manifest.
//#define CN1_INCLUDE_NFC

// INCLUDE_CN1_CAMERA gates the low-level com.codename1.camera native bridge
// (CN1Camera.{h,m}: AVFoundation AVCaptureSession preview/frames/photo/video).
// IPhoneBuilder uncomments this only when the classpath scanner saw
// com.codename1.camera.*, so apps that use the OLD modal Capture API (which
// only needs INCLUDE_CAMERA_USAGE) do NOT drag in the new AVFoundation-based
// natives. Keep this independent of INCLUDE_CAMERA_USAGE on purpose.
//#define INCLUDE_CN1_CAMERA
// The AVFoundation capture stack (CN1Camera.{h,m} + IOSNative camera natives)
// is unavailable on watchOS. IPhoneBuilder uncomments the define above for all
// targets; undo it on the watch slice from this central header (included first
// by every camera TU) so the whole camera path compiles out consistently.
#if TARGET_OS_WATCH
#undef INCLUDE_CN1_CAMERA
#endif

// INCLUDE_CN1_AR gates the com.codename1.ar native bridge (CN1AR.{h,m}:
// ARKit ARSession world/image/face tracking composited through an ARSCNView).
// IPhoneBuilder uncomments this only when the classpath scanner saw
// com.codename1.ar.*, so apps that never touch AR ship without any ARKit or
// SceneKit symbols and pass Apple's API-usage scan.
//#define INCLUDE_CN1_AR
// ARKit is unavailable on watchOS and tvOS; undo the define on those slices
// from this central header (included first by every AR TU) so the whole AR
// path compiles out consistently.
#if TARGET_OS_WATCH || TARGET_OS_TV
#undef INCLUDE_CN1_AR
#endif

// INCLUDE_CN1_VISION gates Apple Vision/Core Image analysis. It is enabled
// only when the app references com.codename1.ai.vision.
//#define INCLUDE_CN1_VISION
//#define INCLUDE_CN1_LANGUAGE
//#define INCLUDE_CN1_INFERENCE
#if TARGET_OS_WATCH || TARGET_OS_TV
#undef INCLUDE_CN1_VISION
#undef INCLUDE_CN1_LANGUAGE
#undef INCLUDE_CN1_INFERENCE
#endif

// CN1_USE_CARPLAY gates the Apple CarPlay native bridge
// (CodenameOne_CarPlaySceneDelegate.{h,m} + the IOSNative carPlay* trampolines:
// CarPlay.framework, the CPTemplate translation). IPhoneBuilder uncomments this
// only when the classpath scanner saw com.codename1.car.*, so apps that never
// build an in-car experience ship without any CarPlay symbols and need no
// CarPlay entitlement. Lives in this central header (included first by every
// CarPlay TU) so the define is visible across translation units.
//#define CN1_USE_CARPLAY
// CarPlay is unavailable on watchOS / tvOS; undo the define on those slices.
#if TARGET_OS_WATCH || TARGET_OS_TV
#undef CN1_USE_CARPLAY
#endif

// CN1_USE_WIDGETS gates the external surfaces native bridge (the IOSNative surfaces*
// trampolines into the Swift CN1SurfaceBridge class plus the cn1surface:// deep link handling
// in CodenameOne_GLAppDelegate). IPhoneBuilder uncomments this only when the classpath scanner
// saw com.codename1.surfaces.*, so apps that never publish widgets or live activities ship
// without any WidgetKit/ActivityKit references and need no app group. Lives in this central
// header (included first by every surfaces TU) so the define is visible across translation
// units, mirroring CN1_USE_CARPLAY.
//#define CN1_USE_WIDGETS
// tvOS has no WidgetKit at all, so the define is undone there.
//
// watchOS deliberately KEEPS it. A complication is a WidgetKit widget in an accessory family,
// hosted by the watch app's own CN1WatchWidgets extension and fed from the watch's own App
// Group container -- the same identifier as the phone's, but a separate container on the
// device, which is why the watch has to publish for itself rather than reading what the phone
// wrote. The surfaces natives below are pure Foundation and resolve the Swift bridge through
// NSClassFromString, so they are exactly as real on the watch as on the phone. While this
// undef also covered watchOS, Surfaces.publish() from a watch app compiled to the unsupported
// stub and silently did nothing.
#if TARGET_OS_TV
#undef CN1_USE_WIDGETS
#endif

#ifdef CN1_USE_WIDGETS
// Decodes a cn1surface:// deep link -- a widget, live activity or complication tap -- and
// dispatches it to the Java framework. Implemented in IOSNative.m rather than in the app
// delegate because the delegate is #if !TARGET_OS_WATCH and the watch reaches the same link
// through its SwiftUI scene. Returns YES when the URL was ours and has been consumed.
BOOL cn1HandleSurfaceURL(NSURL *url);

#if TARGET_OS_WATCH
// Applies a timeline the phone mirrored across into the watch's own App Group container and
// re-renders. Called from CN1WatchConnectivity's didReceiveUserInfo, which may run with no CN1
// runtime at all -- the whole point of the background wake is to refresh a complication, not to
// start an application -- so this touches no Java.
BOOL cn1_watch_apply_mirrored_surface(NSString *kind, NSData *json,
        NSArray<NSString *> *imageNames, NSArray<NSData *> *imageBlobs);
#endif
#endif

// CN1_USE_INTENTS gates the app intents native bridge: the IOSNative intents* implementations
// (Core Spotlight directly, App Intents through the generated Swift CN1IntentBridge via the
// CN1IntentHost Objective-C shim) plus the non-browsing NSUserActivity handling in
// CodenameOne_GLAppDelegate. IPhoneBuilder uncomments this only when the classpath scanner saw
// com.codename1.intents.*, so apps that expose nothing to Siri or device search link neither
// framework. Lives in this central header so the define is visible across translation units,
// mirroring CN1_USE_WIDGETS.
//#define CN1_USE_INTENTS

// CN1_APP_INTENTS_DECLARED is the narrower question: did the build actually generate App Intent
// declarations? CN1_USE_INTENTS only says the app references the package, and an app can use
// indexing and donation while switching declarations off with ios.intents.appIntents=false.
// Both cases still compile CN1IntentBridge.swift -- it carries the donation and query plumbing
// too -- so testing for that class answers "is the Swift here", not "can this app run an App
// Intent". Reporting the latter from the former made an opted-out app advertise Siri support it
// had explicitly removed.
//#define CN1_APP_INTENTS_DECLARED

// Core Spotlight and App Intents are unavailable on watchOS / tvOS; undo the defines there.
#if TARGET_OS_WATCH || TARGET_OS_TV
#undef CN1_USE_INTENTS
#undef CN1_APP_INTENTS_DECLARED
#endif

// CN1_USE_WATCHCONNECTIVITY gates the phone-to-watch link (CN1WatchConnectivity.{h,m} + the
// IOSNative wearable* trampolines) backing com.codename1.wearable. IPhoneBuilder uncomments this
// only when the classpath scanner saw com.codename1.wearable.*, so apps that never talk to a watch
// ship without any WatchConnectivity symbols and link no framework. Unlike the defines above this
// one deliberately SURVIVES on watchOS: WCSession is symmetric, and the watch half of a pair needs
// exactly the same code as the phone half. It does not exist on tvOS or Mac Catalyst.
//#define CN1_USE_WATCHCONNECTIVITY
#if TARGET_OS_TV || TARGET_OS_MACCATALYST
#undef CN1_USE_WATCHCONNECTIVITY
#endif

// CN1_INCLUDE_OIDC gates the com.codename1.io.oidc native bridge
// (AuthenticationServices.framework import, ASWebAuthenticationSession code
// in CN1OidcBrowser.m). IPhoneBuilder uncomments this only when the
// classpath scanner saw com.codename1.io.oidc.*, so apps that never use
// OidcClient ship without the AuthenticationServices link dependency.
//#define CN1_INCLUDE_OIDC

// CN1_INCLUDE_APPLESIGNIN gates the com.codename1.social.AppleSignIn native
// bridge (ASAuthorizationAppleIDProvider code in CN1AppleSignIn.m).
// IPhoneBuilder uncomments this only when the scanner saw AppleSignIn
// references; without it the .m's body compiles to nothing and apps that
// never reference AppleSignIn don't need the `com.apple.developer.applesignin`
// entitlement.
//#define CN1_INCLUDE_APPLESIGNIN

// CN1_INCLUDE_WEBAUTHN gates the com.codename1.io.webauthn native bridge
// (ASAuthorizationPlatformPublicKeyCredentialProvider code in CN1WebAuthn.m,
// iOS 16+). IPhoneBuilder uncomments this only when the scanner saw
// com.codename1.io.webauthn.*; apps that never use passkeys ship without
// any passkey symbols.
//#define CN1_INCLUDE_WEBAUTHN

// CN1_INCLUDE_BLUETOOTH gates the com.codename1.bluetooth native bridge
// (CN1Bluetooth.{h,m}: CoreBluetooth CBCentralManager / CBPeripheralManager,
// GATT client + server, advertising and L2CAP channels). IPhoneBuilder
// uncomments this only when the classpath scanner saw
// com.codename1.bluetooth.*, so apps that never touch Bluetooth ship
// without CoreBluetooth symbols and need no NSBluetoothAlwaysUsageDescription
// privacy entry. The BLE peripheral role (CBPeripheralManager) is
// unavailable on tvOS / watchOS; CN1Bluetooth.m compiles that section out
// per target slice and isBlePeripheralSupported reports false there.
//#define CN1_INCLUDE_BLUETOOTH

// CN1_INCLUDE_HEALTH gates the com.codename1.health native bridge
// (CN1Health.{h,m}: HKHealthStore queries, HKObserverQuery background
// delivery and, on the watch slice, HKWorkoutSession). IPhoneBuilder
// uncomments this only when the classpath scanner saw health classes
// OUTSIDE com.codename1.health.sensors -- the sensor layer is ordinary
// CoreBluetooth and must not pull HealthKit in. Apps that never touch
// health data therefore ship without HealthKit symbols, need no
// com.apple.developer.healthkit entitlement, and are not subject to
// Apple's health-data review.
//#define CN1_INCLUDE_HEALTH
// HealthKit does not exist on tvOS and is unavailable to Mac Catalyst
// apps. Undoing the define here, in the header every health translation
// unit includes first, compiles the whole path out on those slices.
#if TARGET_OS_TV || TARGET_OS_MACCATALYST
#undef CN1_INCLUDE_HEALTH
#endif
// HKWorkoutSession and HKLiveWorkoutBuilder are watchOS-only. The iOS
// slice records workouts through the portable RecordedWorkoutSession
// instead, and isLiveSessionSupported() reports false there.
#if TARGET_OS_WATCH && defined(CN1_INCLUDE_HEALTH)
#define CN1_HEALTH_WORKOUT_SESSION 1
#endif

// CN1_INCLUDE_HOMEKIT gates the com.codename1.home native bridge
// (CN1SmartHome.{h,m}: HMHomeManager, the accessory graph, characteristic
// reads and writes, notifications and action sets). IPhoneBuilder uncomments
// this only when the classpath scanner saw com.codename1.home.*, so apps that
// never touch a smart home ship without HomeKit symbols and need no
// com.apple.developer.homekit entitlement -- which matters more here than for
// most features, because that entitlement is one Apple has to grant on the App
// ID and an app carrying it without cause fails codesigning for no reason.
//#define CN1_INCLUDE_HOMEKIT

// CN1_INCLUDE_NEARBY gates the com.codename1.nearby native bridge
// (CN1Nearby.{h,m}: Nearby Interaction ranging, MultipeerConnectivity
// transport and AccessorySetupKit association). IPhoneBuilder uncomments this
// only when the classpath scanner saw com.codename1.nearby.*, so an app that
// never asks how far away anything is ships without those symbols and without
// the privacy strings they oblige.
//#define CN1_INCLUDE_NEARBY

// The three halves are gated separately because they are available on
// different slices, and because an app that references one package must not
// link the frameworks the other two need. IPhoneBuilder uncomments each from
// its own scanner flag.
//#define CN1_NEARBY_RANGING
//#define CN1_NEARBY_TRANSPORT
//#define CN1_NEARBY_COMPANION

// NearbyInteraction does not exist on tvOS, on the watchOS slice or under Mac
// Catalyst, and neither does AccessorySetupKit. MultipeerConnectivity is
// absent on watchOS. Undoing the defines here, in the header every nearby
// translation unit includes first, compiles those halves out rather than
// leaving each function to guard itself -- and the public API then reports
// them unsupported, which is the answer an app on an Apple TV should get.
#if TARGET_OS_TV || TARGET_OS_WATCH || TARGET_OS_MACCATALYST || TARGET_OS_OSX
#undef CN1_NEARBY_RANGING
#undef CN1_NEARBY_COMPANION
#endif
#if TARGET_OS_WATCH
#undef CN1_NEARBY_TRANSPORT
#endif

// CN1_INCLUDE_CALL gates the com.codename1.call native bridge (CN1Call.{h,m}:
// CallKit's provider and call controller, PushKit's VoIP registry, and the
// Call Directory data store). IPhoneBuilder uncomments this only when the
// classpath scanner saw com.codename1.call.*, so an app that never rings
// anything ships without CallKit symbols -- which matters here more than for
// most features, because CallKit is one of the frameworks App Store review
// looks at and an app carrying it with no calling feature invites questions.
//#define CN1_INCLUDE_CALL

// The two expensive halves are gated separately, because each costs something
// an ordinary calling app must not pay. CN1_CALL_VOIP links PushKit and earns
// the voip background mode, which Apple rejects an app for carrying without a
// working call implementation. CN1_CALL_DIRECTORY generates a Call Directory
// app extension and its App Group. IPhoneBuilder uncomments each from its own
// scanner flag.
//#define CN1_CALL_VOIP
//#define CN1_CALL_DIRECTORY

// CallKit does not exist on tvOS or watchOS, and PushKit does not exist on
// watchOS. Undoing the defines here, in the header every call translation unit
// includes first, compiles those halves out rather than leaving each function
// to guard itself -- and the public API then reports them unsupported, which
// is the answer an app on an Apple TV should get.
//
// Mac Catalyst DOES have CallKit and is deliberately left alone.
#if TARGET_OS_TV || TARGET_OS_WATCH
#undef CN1_INCLUDE_CALL
#undef CN1_CALL_VOIP
#undef CN1_CALL_DIRECTORY
#endif

// CN1_INCLUDE_VPN gates the com.codename1.vpn native bridge (CN1Vpn.{h,m}:
// NEVPNManager and the IKEv2/IPsec protocol configuration). Note this is a
// different thing from the VPN DETECTION in IOSNative.m, which is always
// compiled in, needs no entitlement, and answers whether some VPN is carrying
// this device's traffic.
//#define CN1_INCLUDE_VPN

// There is deliberately no CN1_VPN_TUNNEL. A packet tunnel runs in a Network
// Extension: a separate process with no ParparVM in it, so the tunnel body
// could not be written in this framework even if the target were generated.

// NetworkExtension's VPN manager is unavailable on tvOS and watchOS.
#if TARGET_OS_TV || TARGET_OS_WATCH
#undef CN1_INCLUDE_VPN
#endif

// CN1_INCLUDE_MATTER_SETUP gates the MatterSupport add-device flow, which is
// much more expensive than the rest: it needs its own app-extension target,
// the com.apple.developer.matter.allow-setup-payload entitlement, an app group
// and a Swift shim, because MatterSupport has no Objective-C interface. The
// builder uncomments it only for apps that reference
// com.codename1.home.commissioning -- which is why that lives in a package of
// its own, since the scanner matches on a prefix and cannot express an
// exclusion.
//#define CN1_INCLUDE_MATTER_SETUP
// CN1_MATTER_OWN_FABRIC says the generated extension commissions the accessory
// onto a Matter fabric this app owns, as a second administrator beside the
// user's home. The builder uncomments it for a build whose
// CommissioningRequest.setCommissionToThisApp(true) it saw, and it is what
// lets a successful flow report wasCommissionedToThisApp() as true: the
// extension's commissioning step throwing is what would have failed the flow,
// so a flow that finished is one where the fabric gained the accessory.
//#define CN1_MATTER_OWN_FABRIC
// MatterSupport is iOS and iPadOS only. Undoing the define here, in the header
// every smart-home translation unit includes first, compiles the flow out on
// the other slices; Commissioner.getStyle() then reports NONE and the public
// API sends the user to the Home app instead.
#if TARGET_OS_TV || TARGET_OS_WATCH || TARGET_OS_MACCATALYST || TARGET_OS_OSX
#undef CN1_INCLUDE_MATTER_SETUP
#undef CN1_MATTER_OWN_FABRIC
#endif

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

// Launch placeholder shown over the GL/Metal view between makeKeyAndVisible
// and the first EDT-painted frame; see CodenameOne_GLViewController.m. UIWindow
// is unavailable on watchOS and the launch placeholder is iOS-only.
#if !TARGET_OS_WATCH
void CN1ShowLaunchPlaceholder(UIWindow *window);
void CN1DismissLaunchPlaceholder(void);
#endif

//ADD_INCLUDE

#if TARGET_OS_WATCH
// watchOS has no UIViewController/UIView/CADisplayLink (the SDK marks them
// API_UNAVAILABLE(watchos)). The watch slice replaces the GL view controller
// with a plain NSObject render-driver (CN1WatchViewController.m) that owns the
// same ExecutableOp queue and drives drawFrame into the Core Graphics surface
// (CN1WatchRenderingView). Same class name so the ~10 callers + the translated
// runtime resolve unchanged.
@interface CodenameOne_GLViewController : NSObject {
@private
    GLUIImage* currentMutableImage;
    NSMutableArray* currentTarget;
    NSMutableArray* upcomingTarget;
    BOOL painted;
}
@property (nonatomic) NSInteger animationFrameInterval;
@property (readwrite, assign) GLUIImage* currentMutableImage;
+(CodenameOne_GLViewController*)instance;
-(id)eaglView;
-(id)view;
-(void)startAnimation;
-(void)stopAnimation;
+(BOOL)isDrawTextureSupported;
-(void)initVars;
+(void)upcoming:(ExecutableOp*)op;
-(void)upcomingAdd:(ExecutableOp*)op;
-(void)upcomingAddClip:(ExecutableOp*)op;
-(BOOL)isPaintFinished;
-(void)flushBuffer:(UIImage *)buff x:(int)x y:(int)y width:(int)width height:(int)height;
-(void)drawString:(int)color alpha:(int)alpha font:(UIFont*)font str:(NSString*)str x:(int)x y:(int)y;
-(void)drawScreen;
-(void)drawFrame:(CGRect)rect;
-(void)drawFrame:(CGRect)rect allowInactive:(BOOL)allowInactive;
@end
#else
@interface CodenameOne_GLViewController : UIViewController<
#if !TARGET_OS_TV
UIImagePickerControllerDelegate,
#endif
#if !TARGET_OS_WATCH && !TARGET_OS_TV
MFMailComposeViewControllerDelegate,
#endif
UIScrollViewDelegate,
#ifdef CN1_USE_STOREKIT
SKProductsRequestDelegate, SKPaymentTransactionObserver,
#endif
#if !TARGET_OS_WATCH && !TARGET_OS_TV
MFMessageComposeViewControllerDelegate, UIActionSheetDelegate, UIPopoverControllerDelegate,
#endif
CLLocationManagerDelegate, AVAudioRecorderDelegate
#if !TARGET_OS_TV
, UIPickerViewDelegate, UIDocumentInteractionControllerDelegate
#endif
#if !TARGET_OS_WATCH && !TARGET_OS_TV
, UIDocumentPickerDelegate
#endif
#ifdef INCLUDE_ZOOZ
        ,ZooZPaymentCallbackDelegate
#endif
#ifdef INCLUDE_MOPUB
        ,MPAdViewDelegate
#endif
// GoogleSignIn 7 declares no GIDSignInDelegate -- the sign-in result goes to a
// completion handler instead -- so only the legacy Google+ path adopts a
// Google protocol here.
#ifdef INCLUDE_GOOGLE_CONNECT
#ifndef GOOGLE_SIGNIN
        ,GPPSignInDelegate
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
- (void)drawFrame:(CGRect)rect allowInactive:(BOOL)allowInactive;

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event;
-(void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event;
-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event;
-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event;

#if !TARGET_OS_TV
- (void)datePickerChangeDate:(UIDatePicker *)sender;
#endif
-(void)datePickerDismiss;
-(void)datePickerCancel;

+ (void)initialize;

+(CodenameOne_GLViewController*)instance;
-(void)upcomingAddClip:(ExecutableOp*)op;

+(BOOL)isCurrentMutableTransformSet;

+(CGAffineTransform) currentMutableTransform;
-(void)updateCanvas:(BOOL)animated;
@end
#endif // TARGET_OS_WATCH (GL view controller interface variant)
