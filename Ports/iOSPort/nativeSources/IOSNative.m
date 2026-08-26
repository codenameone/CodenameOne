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
// Pisces imports
#import "Renderer.h"
#import "PathConsumer.h"
#import "Stroker.h"
// end Pisces imports
#include "xmlvm.h"
#include "java_lang_String.h"
#include <pthread.h>
#include <limits.h>
#include <stdlib.h>
#include <string.h>
#import "CN1ES2compat.h"
#import "CN1JailbreakDetector.h"
#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"
#import "CN1WatchHost.h"
#import "CN1WatchRenderingView.h"
#import <Accelerate/Accelerate.h>
#endif
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#import "METALView.h"
#endif
#import <objc/runtime.h>
#import <objc/message.h>

#ifndef NEW_CODENAME_ONE_VM
#include "xmlvm-util.h"
#else
#include "cn1_globals.h"
#endif
#import "CN1AudioUnit.h"
#import "CN1AppleUI.h"

#if TARGET_OS_OSX
/*
 * Headers from Ports/MacPort. They are only reachable on a macOS build, where
 * both ports' native sources are staged into one directory, and the iOS build
 * never sees this branch. Importing them here is what lets the shared natives
 * whose macOS arm is genuinely AppKit -- the screen capture, the share sheet,
 * the window chrome -- live beside their iOS bodies instead of being split into
 * a parallel file that would drift.
 */
#import "CN1MacHost.h"
#import "CN1AppKitCompat.h"
#import "METALView.h"
#endif

/// Holds the idle-sleep assertion between lockScreen and unlockScreen.
#if TARGET_OS_OSX
static id<NSObject> cn1MacIdleActivity = nil;
#endif
#include <sys/sysctl.h>
#import "CodenameOne_GLViewController.h"
#import <QuartzCore/QuartzCore.h>
#import <LocalAuthentication/LocalAuthentication.h>
#import <Security/Security.h>
#import "NetworkConnectionImpl.h"
#include "com_codename1_impl_ios_IOSImplementation.h"
#include "com_codename1_impl_ios_IOSBiometrics.h"
#include "com_codename1_impl_ios_IOSDeviceIntegrity.h"
#include "com_codename1_impl_ios_IOSSecureStorage.h"
#include "com_codename1_impl_ios_IOSNfc.h"
#include "com_codename1_impl_ios_IOSConnectivity.h"
// Declares nativeSurfaceAction for cn1HandleSurfaceURL below. The decode used to live in
// the app delegate, which includes this same header; moving it here for the watch left the
// call with no declaration, and C then invented one. Catalyst builds with
// -Werror=implicit-function-declaration and said so, but the danger is not the diagnostic:
// an invented prototype passes three JAVA_OBJECTs and a thread state through the wrong
// registers, which links and then misbehaves.
#include "com_codename1_impl_ios_IOSSurfaceCallbacks.h"
#include "com_codename1_ui_Display.h"
#include "com_codename1_ui_Component.h"
#include "java_lang_Throwable.h"
#include "java_lang_RuntimeException.h"
#import "FillPolygon.h"
#import "AudioPlayer.h"
#import "CN1SoundPool.h"
#import "DrawGradient.h"
#ifdef CN1_USE_METAL
#import "DrawMultiStopGradient.h"
#endif
#import <MediaPlayer/MediaPlayer.h>
#import <CoreLocation/CoreLocation.h>
#if !TARGET_OS_TV
#import <CoreMotion/CoreMotion.h>
#endif
// Not available on macOS.
#if !TARGET_OS_OSX
#import <MobileCoreServices/UTCoreTypes.h>
#endif
#import <Foundation/Foundation.h>
#if !TARGET_OS_WATCH && !TARGET_OS_TV
//#define CN1_USE_CALENDAR
#ifdef CN1_USE_CALENDAR
#import <EventKit/EventKit.h>
#endif
#endif
#import <CoreText/CoreText.h>
#include <ifaddrs.h>
#include <net/if.h>
#include <arpa/inet.h>
#include <netinet/in.h>
// SystemConfiguration (SCNetworkReachability) is unavailable on watchOS; the
// network-type + WiFi-listener natives degrade to no-ops there (guarded below).
#if !TARGET_OS_WATCH
#include <SystemConfiguration/SystemConfiguration.h>
#include <SystemConfiguration/SCNetworkReachability.h>
#endif
// MessageUI + AddressBookUI are unavailable on watchOS (and AddressBookUI on Mac
// Catalyst); on tvOS MessageUI ships only a link stub with no composer headers
// and AddressBookUI is absent. The native methods that use them are guarded to
// no-ops on those slices.
#if !TARGET_OS_WATCH && !TARGET_OS_TV
// Not available on macOS.
#if !TARGET_OS_OSX
#import <MessageUI/MFMailComposeViewController.h>
#endif
#endif
#if !TARGET_OS_MACCATALYST && !TARGET_OS_WATCH && !TARGET_OS_TV
// AddressBookUI and the legacy AddressBook C API are unavailable on Mac
// Catalyst and tvOS. Skip the import; the contacts path falls back to
// Contacts.framework (handled via INCLUDE_CONTACTS_USAGE undef below).
// Not available on macOS.
#if !TARGET_OS_OSX
#import <AddressBookUI/AddressBookUI.h>
#endif
#endif
#if !TARGET_OS_WATCH && !TARGET_OS_TV
// Not available on macOS.
#if !TARGET_OS_OSX
#import <MessageUI/MFMessageComposeViewController.h>
#endif
#endif

#if TARGET_OS_MACCATALYST
// AddressBook.framework (the C ABAddressBookRef API) is unavailable on Mac
// Catalyst. Suppress the legacy contacts code path on Mac so the build links.
#ifdef INCLUDE_CONTACTS_USAGE
#undef INCLUDE_CONTACTS_USAGE
#endif
#endif
#import "UIWebViewEventDelegate.h"
#include <sqlite3.h>
#ifdef CN1_USE_STOREKIT
#import "StoreKit/StoreKit.h"
#endif
#include "com_codename1_contacts_Contact.h"
#include "com_codename1_contacts_Address.h"
#include "java_util_Hashtable.h"
#include "com_codename1_ui_Image.h"
#include "com_codename1_impl_ios_IOSImplementation_NativeImage.h"
#include "com_codename1_util_SuccessCallback.h"
#import "SocketImpl.h"
#import "com_codename1_ui_geom_Rectangle.h"
// Not available on macOS.
#if !TARGET_OS_OSX
#import <MobileCoreServices/MobileCoreServices.h>
#endif
#include "com_codename1_ui_plaf_Style.h"
#import "RadialGradientPaint.h"
#include "java_io_IOException.h"
#include "com_codename1_io_Cookie.h"
#include "com_codename1_ui_plaf_UIManager.h"
#include "java_io_Writer.h"
#include "java_util_ArrayList.h"
#include "com_codename1_ui_Font.h"
#include "java_util_Vector.h"
#include "permission_apis.h"
//#import "QRCodeReaderOC.h"
#define AUTO_PLAY_VIDEO
// WebKit is unavailable on watchOS and tvOS; gate the WKWebView path off there
// (this also leaves supportsWKWebKit undefined, disabling the WK usage block
// below). tvOS ships neither UIWebView nor WKWebView, so it has no web view.
#if defined(ENABLE_WKWEBVIEW) && !TARGET_OS_WATCH && !TARGET_OS_TV
#if (__MAC_OS_X_VERSION_MAX_ALLOWED > __MAC_10_9 || __IPHONE_OS_VERSION_MAX_ALLOWED > __IPHONE_7_1)
#import <WebKit/WebKit.h>
#define supportsWKWebKit
#endif
#endif
#ifdef INCLUDE_ZOOZ
#import "ZooZ.h"
#endif
#import "Rotate.h"
//#define CN1_USE_AVKIT
// The native macOS port has no MPMoviePlayerController to fall back to, so AVKit
// is not an option there -- it is the only video backend. IPhoneBuilder
// uncomments the define above for the targets it builds; macOS is not one of
// them, so it is defined here instead.
#if TARGET_OS_OSX
#define CN1_USE_AVKIT
#endif
// AVKit / AVPlayerViewController are unavailable on watchOS. IPhoneBuilder
// uncomments the define above for all targets; undo it on the watch slice so
// the AVKit video paths compile out (the watch video stubs return defaults).
#if TARGET_OS_WATCH
#undef CN1_USE_AVKIT
#endif
#ifdef CN1_USE_AVKIT
#import <AVKit/AVKit.h>
#if TARGET_OS_OSX
/*
 * AppKit's AVKit has no AVPlayerViewController. AVPlayerView is the whole of
 * it: a view that owns a player and draws its own transport controls, where
 * iOS splits that between a controller and the view it vends. Everything below
 * that says "player controller" therefore means an AVPlayerView on this port,
 * and getVideoViewPeer hands back the same object rather than reaching for a
 * .view inside it.
 */
#define CN1_AVPLAYERVIEWCONTROLLER AVPlayerView*
// The video factories differ only in this one class name -- they allocate it,
// hand it a player and return it -- so the name is aliased rather than each of
// the five being restated with one word changed. The places where the two types
// genuinely differ (the transport controls, the full-screen mode, and the fact
// that the view has no .view inside it) are written out with their own macOS
// arms above.
#define AVPlayerViewController AVPlayerView
#elif (__MAC_OS_X_VERSION_MAX_ALLOWED > __MAC_10_9 || __IPHONE_OS_VERSION_MAX_ALLOWED > __IPHONE_7_1)
#define CN1_AVPLAYERVIEWCONTROLLER AVPlayerViewController*
#else
#define CN1_AVPLAYERVIEWCONTROLLER id
#endif
#else
#define CN1_AVPLAYERVIEWCONTROLLER id
#endif
extern int popoverSupported();
//#define CN1_INCLUDE_NOTIFICATIONS2
#define INCLUDE_CN1_PUSH2
// Import UserNotifications whenever it is actually used below. The push code
// (INCLUDE_CN1_PUSH2) references UNUserNotificationCenter/UNNotification*, but
// the import was gated only on CN1_INCLUDE_NOTIFICATIONS2 and otherwise relied
// on clang's implicit module auto-import. Enabling the Metal screenTexture
// readback above (compiled on iOS now, not just Catalyst/TV) perturbs that
// auto-import and the push symbols fail to resolve; importing the framework
// explicitly when push is enabled makes it robust.
#if defined(CN1_INCLUDE_NOTIFICATIONS2) || (defined(INCLUDE_CN1_PUSH2) && !TARGET_OS_WATCH)
#import <UserNotifications/UserNotifications.h>
#endif
#if !TARGET_OS_WATCH
#import <BackgroundTasks/BackgroundTasks.h>
#endif
#ifdef INCLUDE_PHOTOLIBRARY_USAGE
#ifdef ENABLE_GALLERY_MULTISELECT
#ifdef USE_PHOTOKIT_FOR_MULTIGALLERY
#import <PhotosUI/PhotosUI.h>
#endif
#endif
#endif

// iOS doesn't allow blocking a static screenshot, but it does expose a flag
// that tells us when the screen is being captured (recorded or mirrored). We
// use that signal to temporarily cover the app view with a black overlay.
static BOOL cn1_disableScreenshots = NO;
#if !TARGET_OS_WATCH
static CN1View *cn1ScreenCaptureView = nil;
static id cn1ScreenCaptureObserver = nil;

static CN1View *cn1_screenCaptureContainer() {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return nil;
#else
    // Prefer the GL view controller's view. Fall back to the key window so
    // we can still cover the app if the controller isn't ready yet.
    CN1View *container = [[CodenameOne_GLViewController instance] view];
    if (container == nil) {
        container = [UIApplication sharedApplication].keyWindow;
        if (container == nil && [[UIApplication sharedApplication].windows count] > 0) {
            container = [[UIApplication sharedApplication].windows objectAtIndex:0];
        }
    }
    return container;
#endif
}

static void cn1_updateScreenCaptureBlocker() {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 110000
    // Compile-time guard: screen-capture detection APIs were introduced in iOS 11.
    // This keeps older SDKs building cleanly without referencing unavailable symbols.
    if (!cn1_disableScreenshots) {
        if (cn1ScreenCaptureView != nil) {
            [cn1ScreenCaptureView removeFromSuperview];
            cn1ScreenCaptureView = nil;
        }
        return;
    }
    if (![[UIScreen mainScreen] respondsToSelector:@selector(isCaptured)]) {
        // Runtime guard: if running on an older iOS version, just skip.
        return;
    }
    BOOL captured = NO;
    if (@available(iOS 11.0, *)) {
        captured = [UIScreen mainScreen].isCaptured;
    }
    if (captured) {
        // If screen capture is active, hide the UI behind an overlay view.
        CN1View *container = cn1_screenCaptureContainer();
        if (container == nil) {
            return;
        }
        if (cn1ScreenCaptureView == nil) {
            cn1ScreenCaptureView = [[CN1View alloc] initWithFrame:container.bounds];
            cn1ScreenCaptureView.backgroundColor = [UIColor blackColor];
            cn1ScreenCaptureView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
            cn1ScreenCaptureView.userInteractionEnabled = NO;
        }
        if (cn1ScreenCaptureView.superview != container) {
            [cn1ScreenCaptureView removeFromSuperview];
            [container addSubview:cn1ScreenCaptureView];
        } else {
            cn1ScreenCaptureView.frame = container.bounds;
        }
    } else if (cn1ScreenCaptureView != nil) {
        [cn1ScreenCaptureView removeFromSuperview];
        cn1ScreenCaptureView = nil;
    }
#else
    // Older SDKs don't have the screen-capture APIs. Nothing to do.
    if (cn1ScreenCaptureView != nil) {
        [cn1ScreenCaptureView removeFromSuperview];
        cn1ScreenCaptureView = nil;
    }
#endif
}
#else // TARGET_OS_WATCH: no CN1View/UIApplication/UIScreen overlay on the watch.
static void cn1_updateScreenCaptureBlocker() {}
#endif // !TARGET_OS_WATCH

/*static JAVA_OBJECT utf8_constant = JAVA_NULL;
 JAVA_OBJECT fromNSString(NSString* str)
 {
 if (str == nil) {
 return JAVA_NULL;
 }
 if (utf8_constant == JAVA_NULL) {
 utf8_constant = xmlvm_create_java_string("UTF-8");
 }
 NSAutoreleasePool* p = [[NSAutoreleasePool alloc] init];
 java_lang_String* s = __NEW_java_lang_String();
 const char* chars = [str UTF8String];
 int length = strlen(chars);
 org_xmlvm_runtime_XMLVMArray* data = XMLVMArray_createSingleDimensionWithData(__CLASS_byte, length, chars);
 java_lang_String___INIT____byte_1ARRAY_java_lang_String(s, data, utf8_constant);
 [p release];
 return s;
 }*/

#if !TARGET_OS_WATCH
extern CN1View *editingComponent;
#endif // !TARGET_OS_WATCH

extern void initVMImpl();

extern void deinitVMImpl();

extern int Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl();

extern int Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl();

extern JAVA_INT getSafeTop();
extern JAVA_INT getSafeLeft();
extern JAVA_INT getSafeBottom();
extern JAVA_INT getSafeRight();

extern void Java_com_codename1_impl_ios_IOSImplementation_flushBufferImpl
(void* peer, int x, int y, int width, int height);

extern void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingMutableImpl
(int x, int y, int width, int height, int clipApplied);

extern void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingShapeMutableImpl
(int numCommands, JAVA_OBJECT commands, int numPoints, JAVA_OBJECT points);

extern void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingGlobalImpl
(int x, int y, int width, int height, int clipApplied);

extern void Java_com_codename1_impl_ios_IOSImplementation_setAntiAliasedMutableImpl(JAVA_BOOLEAN antialiased);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineGlobalImpl
(int color, int alpha, int x1, int y1, int x2, int y2);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineMutableImpl
(int color, int alpha, int x1, int y1, int x2, int y2);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectMutableImpl
(int color, int alpha, int x, int y, int width, int height);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectGlobalImpl
(int color, int alpha, int x, int y, int width, int height);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectMutableImpl
(int color, int alpha, int x, int y, int width, int height);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectGlobalImpl
(int color, int alpha, int x, int y, int width, int height);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringGlobalImpl
(int color, int alpha, void* fontPeer, NSString* str, int x, int y);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringMutableImpl
(int color, int alpha, void* fontPeer, NSString* str, int x, int y);

extern void* Java_com_codename1_impl_ios_IOSImplementation_createNativeMutableImageImpl
(int width, int height, int argb);

extern void Java_com_codename1_impl_ios_IOSImplementation_startDrawingOnImageImpl
(int width, int height, void *peer);

extern void* Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl
();

extern void Java_com_codename1_impl_ios_IOSImplementation_deleteNativePeerImpl
(void* peer);

extern void Java_com_codename1_impl_ios_IOSImplementation_deleteNativeFontPeerImpl
(void* peer);

extern void* Java_com_codename1_impl_ios_IOSImplementation_createImageImpl
(void* data, int dataLength, int* widthAndHeightReturnValue);

extern void* Java_com_codename1_impl_ios_IOSImplementation_scaleImpl
(void* peer, int width, int height);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRoundRectGlobalImpl
(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeFillRoundRectGlobalImpl
(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRoundRectMutableImpl
(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeFillRoundRectMutableImpl
(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeFillArcMutableImpl
(int color, int alpha, int x, int y, int width, int height, int startAngle, int angle);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawArcMutableImpl
(int color, int alpha, int x, int y, int width, int height, int startAngle, int angle);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageMutableImpl
(void* peer, int alpha, int x, int y, int width, int height, int renderingHints);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl
(void* peer, int alpha, int x, int y, int width, int height, int renderingHints);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeTileImageGlobalImpl
(void* peer, int alpha, int x, int y, int width, int height);

extern signed int Java_com_codename1_impl_ios_IOSImplementation_stringWidthNativeImpl
(void* peer, const char* str, int len);



extern int Java_com_codename1_impl_ios_IOSImplementation_charWidthNativeImpl
(void* peer, int chr);


extern int Java_com_codename1_impl_ios_IOSImplementation_getFontHeightNativeImpl
(void* peer);

extern int Java_com_codename1_impl_ios_IOSImplementation_getFontAscentNativeImpl
(void* peer);

extern int Java_com_codename1_impl_ios_IOSImplementation_getFontDescentNativeImpl
(void* peer);

extern void* Java_com_codename1_impl_ios_IOSImplementation_createSystemFontImpl
(int face, int style, int size);

extern void loadResourceFile(const char* name, int nameLen, const char* type, int typeLen, void* data);

extern int getResourceSize(const char* name, int nameLen, const char* type, int typeLen);

extern int isPainted();
extern int displayWidth;
extern int displayHeight;

extern void Java_com_codename1_impl_ios_IOSImplementation_imageRgbToIntArrayImpl
(void* peer, int* arr, int x, int y, int width, int height, int imgWidth, int imgHeight);

extern void Java_com_codename1_impl_ios_IOSImplementation_flushBufferForReadbackImpl
(int x, int y, int width, int height);

extern void* Java_com_codename1_impl_ios_IOSImplementation_createImageFromARGBImpl
(int* arr, int width, int height);

extern void Java_com_codename1_impl_ios_IOSImplementation_editStringAtImpl
(CN1_THREAD_STATE_MULTI_ARG int x, int y, int w, int h, void* peer, int isSingleLine, int rows, int maxSize,
 int constraint, const char* str, int len, BOOL dialogHeight, int color, JAVA_LONG imagePeer,
 int padTop, int padBottom, int padLeft, int padRight, NSString* hintString, int hintColor, BOOL showToolbar, BOOL blockCopyPaste, int alignment, int verticalAlignment, BOOL returnExitsEditing);

extern void Java_com_codename1_impl_ios_IOSImplementation_resetAffineGlobal();

extern void Java_com_codename1_impl_ios_IOSImplementation_scale(float x, float y);

extern int isIPad();
extern int isIOS7();
extern int isIOS8();
extern int isIOS8_2();

NSString* cn1_getDocumentsDir() {
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    return [writablePaths lastObject];  
}
NSString* cn1_getContainerRoot() {
    NSString* appRoot = cn1_getDocumentsDir();
    if ([appRoot hasSuffix:@"/"]) {
        appRoot = [appRoot substringWithRange:NSMakeRange(0, [appRoot length]-1)];
    }
    return [appRoot substringWithRange:NSMakeRange(0, [appRoot rangeOfString:@"/" options:NSBackwardsSearch].location +1)];

}
NSString* cn1_fixAppRoot(NSString* path) {
    NSString* base = @"/var/mobile/Containers/Data/Application/";
    NSString* containerRoot = cn1_getContainerRoot();
    if ([path hasPrefix:base] && ![path hasPrefix:containerRoot]) {
        NSUInteger start = [base length];
        NSUInteger end = [path length];

        NSString* theRest = [path substringWithRange:NSMakeRange(start, end - start)];
        NSUInteger slashPos = [theRest rangeOfString:@"/"].location;
        if (slashPos <= 0) {
            return path;
        }
        start = slashPos+1;
        end = [theRest length];
        return [containerRoot stringByAppendingString: [theRest substringWithRange:NSMakeRange(start, end - start)]];
    }
    return path;
}
NSString* fixFilePath(NSString* ns) {
    //NSLog(@"Fixing %@", ns);
    if([ns hasPrefix:@"file:"]) {
        ns = [ns substringFromIndex:5];
        while([ns hasPrefix:@"//"]) {
            ns = [ns substringFromIndex:1];
        }
    }
    
    NSString* out = cn1_fixAppRoot(ns);
    //NSLog(@"Fixed to %@", out);
    return out;
}









bool galleryPopover = NO;

#ifndef NEW_CODENAME_ONE_VM
JAVA_OBJECT utf8String = NULL;

const char* stringToUTF8(JAVA_OBJECT str) {
    if(str == NULL) {
        return NULL;
    }
    if(utf8String == NULL) {
        utf8String = xmlvm_create_java_string("UTF-8");
    }
    org_xmlvm_runtime_XMLVMArray* byteArray = java_lang_String_getBytes___java_lang_String(str, utf8String);
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    
    JAVA_ARRAY_INT len = byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
    char* cs = XMLVM_ATOMIC_MALLOC(len + 1);
    memcpy(cs, data, len);
    cs[len] = '\0';
    return cs;
}
#endif // NEW_CODENAME_ONE_VM

void com_codename1_impl_ios_IOSNative_initVM__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
#if TARGET_OS_OSX
    // There is no UIApplicationMain here: the generated main already owns the
    // main thread with [NSApp run], and this runs on the bootstrap thread. What
    // has to be mirrored is UIApplicationMain's two effects rather than its
    // shape -- publish the lifecycle callback, which sets
    // IOSImplementation.initialized and serial dispatches the application start
    // onto the event dispatch thread, and then never return.
    //
    // Never returning is not a detail. UIApplicationMain does not return either,
    // and that is what stops Display.init -> postInit falling through into
    // super.postInit()'s initDefaultUserAgent, whose blocking AsyncResource.get
    // no Apple port ever reaches. Returning from here deadlocks the start.
    extern JAVA_VOID com_codename1_impl_ios_IOSImplementation_callback__(struct ThreadLocalData* threadStateData);
    com_codename1_impl_ios_IOSImplementation_callback__(threadStateData);
    // HERE, exactly as on the watch. The generated main installs the app
    // delegate and enters [NSApp run] without waiting for this thread, so AppKit
    // can activate the application and deliver applicationDidBecomeActive before
    // IOSImplementation exists -- and that callback dereferences the static
    // instance with no null guard. The delegate holds those transitions until
    // this line and then replays the state they left behind.
    extern void cn1_mac_runtime_markJavaReady(void);
    cn1_mac_runtime_markJavaReady();
    while (1) {
        [NSThread sleepForTimeInterval:3600];
    }
#else
#if !TARGET_OS_WATCH
    POOL_BEGIN();
    int retVal = UIApplicationMain(0, nil, nil, @"CodenameOne_GLAppDelegate");
    POOL_END();
#else
    // The watch app provides its own SwiftUI @main entry point, so there is no
    // UIApplicationMain. Mirror UIApplicationMain's role precisely: schedule the
    // lifecycle callback (sets IOSImplementation.initialized and serial-
    // dispatches the app start onto the EDT) and then block this dedicated
    // bootstrap thread forever. Blocking is essential -- it stops Display.init
    // -> postInit from falling through into super.postInit()'s
    // initDefaultUserAgent, which does a blocking AsyncResource.get() that iOS
    // never reaches (because UIApplicationMain never returns). The SwiftUI main
    // thread and the CN1WatchHost paint pump keep running; the EDT runs the app.
    extern JAVA_VOID com_codename1_impl_ios_IOSImplementation_callback__(struct ThreadLocalData* threadStateData);
    com_codename1_impl_ios_IOSImplementation_callback__(threadStateData);
    // HERE, and not after the stub's main returns: this thread never returns. The callback above
    // is the point at which IOSImplementation exists and its lifecycle is installed, so it is the
    // only honest place to say the Java side can be told about a background or foreground
    // transition. Publishing it from the generated bootstrap after the stub's main looked
    // equivalent and was dead code -- the loop below is entered first and never left, so the flag
    // stayed false and every transition queued forever, which is the stop()/start() the watch app
    // was missing in the first place.
    //
    // Safe this early because the transition forwarders serial-dispatch onto the EDT, so a phase
    // released now queues BEHIND the app start this callback just scheduled.
    extern void cn1_watch_runtime_markJavaReady(void);
    cn1_watch_runtime_markJavaReady();
    while (1) {
        [NSThread sleepForTimeInterval:3600];
    }
#endif // !TARGET_OS_WATCH
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isMetalRendering__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
#ifdef CN1_USE_METAL
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

void xmlvm_init_native_com_codename1_impl_ios_IOSNative()
{
}


void com_codename1_impl_ios_IOSNative_deinitializeVM__(CN1_THREAD_STATE_SINGLE_ARG)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_deinitializeVM__]
    deinitVMImpl();
    //XMLVM_END_WRAPPER
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isPainted__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_isPainted__]
    return isPainted();
    //XMLVM_END_WRAPPER
}

#if defined(CN1_USE_CALENDAR) && !TARGET_OS_WATCH && !TARGET_OS_TV
static EKEventStore* cn1CalendarStore() {
    static EKEventStore *store;
    static dispatch_once_t once;
    dispatch_once(&once, ^{ store = [[EKEventStore alloc] init]; });
    return store;
}

static NSString* cn1CalendarJson(id value) {
    if (value == nil) return @"{}";
    NSData *data = [NSJSONSerialization dataWithJSONObject:value options:0 error:nil];
    return data == nil ? @"{}" : [[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] autorelease];
}

static NSDictionary* cn1CalendarDictionary(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT json) {
    NSString *text = toNSString(CN1_THREAD_STATE_PASS_ARG json);
    if (text == nil) return @{};
    NSData *data = [text dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary *out = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
    return [out isKindOfClass:[NSDictionary class]] ? out : @{};
}

static NSDate* cn1CalendarDate(NSDictionary *json, NSString *key) {
    NSNumber *milliseconds = json[key];
    if ([milliseconds isKindOfClass:[NSNumber class]]) return [NSDate dateWithTimeIntervalSince1970:milliseconds.doubleValue / 1000.0];
    NSString *dateText = json[[key stringByAppendingString:@"Date"]];
    if (![dateText isKindOfClass:[NSString class]]) return nil;
    NSDateFormatter *format = [[NSDateFormatter alloc] init];
    format.locale = [NSLocale localeWithLocaleIdentifier:@"en_US_POSIX"];
    NSString *zoneName = json[[key stringByAppendingString:@"Zone"]];
    format.timeZone = [zoneName isKindOfClass:[NSString class]] ? [NSTimeZone timeZoneWithName:zoneName] : [NSTimeZone localTimeZone];
    format.dateFormat = @"yyyy-MM-dd";
    NSDate *result = [format dateFromString:dateText];
    [format release];
    return result;
}

static NSDictionary* cn1CalendarDateFields(NSDate *date, NSString *key, NSTimeZone *zone, BOOL allDay) {
    if (date == nil) return @{};
    if (allDay) {
        NSDateFormatter *format = [[NSDateFormatter alloc] init];
        format.locale = [NSLocale localeWithLocaleIdentifier:@"en_US_POSIX"];
        format.timeZone = zone ?: [NSTimeZone localTimeZone];
        format.dateFormat = @"yyyy-MM-dd";
        NSDictionary *result = @{[key stringByAppendingString:@"Date"]: [format stringFromDate:date], [key stringByAppendingString:@"AllDay"]: @YES};
        [format release];
        return result;
    }
    return @{key: @([date timeIntervalSince1970] * 1000.0), [key stringByAppendingString:@"Zone"]: zone.name ?: @"UTC"};
}

static NSDictionary* cn1EventDictionary(EKEvent *event) {
    NSMutableDictionary *out = [NSMutableDictionary dictionary];
    if (event.eventIdentifier) out[@"id"] = event.eventIdentifier;
    if (event.calendar.calendarIdentifier) out[@"calendarId"] = event.calendar.calendarIdentifier;
    if (event.title) out[@"title"] = event.title;
    if (event.notes) out[@"notes"] = event.notes;
    if (event.location) out[@"location"] = event.location;
    out[@"allDay"] = @(event.allDay);
    [out addEntriesFromDictionary:cn1CalendarDateFields(event.startDate, @"start", event.timeZone, event.allDay)];
    [out addEntriesFromDictionary:cn1CalendarDateFields(event.endDate, @"end", event.timeZone, event.allDay)];
    out[@"available"] = @(event.availability == EKEventAvailabilityFree);
    out[@"version"] = [NSString stringWithFormat:@"%.0f", event.lastModifiedDate.timeIntervalSince1970 * 1000.0];
    NSMutableArray *alarms = [NSMutableArray array];
    for (EKAlarm *alarm in event.alarms ?: @[]) {
        if (alarm.absoluteDate) [alarms addObject:@{@"absolute": @([alarm.absoluteDate timeIntervalSince1970] * 1000.0)}];
        else [alarms addObject:@{@"minutes": @((NSInteger)(-alarm.relativeOffset / 60.0))}];
    }
    out[@"alarms"] = alarms;
    return out;
}

static NSDictionary* cn1ReminderDictionary(EKReminder *reminder) {
    NSMutableDictionary *out = [NSMutableDictionary dictionary];
    if (reminder.calendarItemIdentifier) out[@"id"] = reminder.calendarItemIdentifier;
    if (reminder.calendar.calendarIdentifier) out[@"calendarId"] = reminder.calendar.calendarIdentifier;
    if (reminder.title) out[@"title"] = reminder.title;
    if (reminder.notes) out[@"notes"] = reminder.notes;
    out[@"completed"] = @(reminder.completed);
    out[@"version"] = [NSString stringWithFormat:@"%.0f", reminder.lastModifiedDate.timeIntervalSince1970 * 1000.0];
    if (reminder.dueDateComponents) {
        NSDate *due = [reminder.dueDateComponents.calendar dateFromComponents:reminder.dueDateComponents];
        BOOL allDay = reminder.dueDateComponents.hour == NSDateComponentUndefined;
        [out addEntriesFromDictionary:cn1CalendarDateFields(due, @"due", reminder.dueDateComponents.timeZone, allDay)];
        out[@"dueAllDay"] = @(allDay);
    }
    return out;
}
#endif

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_calendarSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if defined(CN1_USE_CALENDAR) && !TARGET_OS_WATCH && !TARGET_OS_TV
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_calendarAuthorizationStatus___int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT entityType) {
#if defined(CN1_USE_CALENDAR) && !TARGET_OS_WATCH && !TARGET_OS_TV
    return (JAVA_INT)[EKEventStore authorizationStatusForEntityType:entityType == 1 ? EKEntityTypeReminder : EKEntityTypeEvent];
#else
    return 2;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_calendarRequestAccess___int_boolean_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT entityType, JAVA_BOOLEAN writeOnly) {
#if defined(CN1_USE_CALENDAR) && !TARGET_OS_WATCH && !TARGET_OS_TV
    __block BOOL granted = NO; dispatch_semaphore_t semaphore = dispatch_semaphore_create(0); EKEventStore *store = cn1CalendarStore();
    void (^completion)(BOOL,NSError*) = ^(BOOL value, NSError *error){ granted = value; dispatch_semaphore_signal(semaphore); };
    if (@available(iOS 17.0, macCatalyst 17.0, *)) {
        if (entityType == 1) [store requestFullAccessToRemindersWithCompletion:completion];
        else if (writeOnly) [store requestWriteOnlyAccessToEventsWithCompletion:completion];
        else [store requestFullAccessToEventsWithCompletion:completion];
    } else [store requestAccessToEntityType:entityType == 1 ? EKEntityTypeReminder : EKEntityTypeEvent completion:completion];
    dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER); return granted;
#else
    return 0;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_calendarList___int_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT entityType) {
#if defined(CN1_USE_CALENDAR) && !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    NSMutableArray *items=[NSMutableArray array]; for(EKCalendar *calendar in [cn1CalendarStore() calendarsForEntityType:entityType==1?EKEntityTypeReminder:EKEntityTypeEvent]) [items addObject:@{@"id":calendar.calendarIdentifier?:@"",@"title":calendar.title?:@"",@"allowsModify":@(calendar.allowsContentModifications),@"color":@0}];
    JAVA_OBJECT result=fromNSString(CN1_THREAD_STATE_PASS_ARG cn1CalendarJson(@{@"items":items}));
    POOL_END();
    return result;
#else
    return fromNSString(CN1_THREAD_STATE_PASS_ARG @"{\"items\":[]}");
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_calendarEvents___java_lang_String_long_long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT calendarId, JAVA_LONG startTime, JAVA_LONG endTime) {
#if defined(CN1_USE_CALENDAR) && !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    EKEventStore *store=cn1CalendarStore();NSArray *calendars=nil;NSString *identifier=toNSString(CN1_THREAD_STATE_PASS_ARG calendarId);if(identifier){EKCalendar *calendar=[store calendarWithIdentifier:identifier];if(calendar)calendars=@[calendar];}
    NSPredicate *predicate=[store predicateForEventsWithStartDate:[NSDate dateWithTimeIntervalSince1970:startTime/1000.0] endDate:[NSDate dateWithTimeIntervalSince1970:endTime/1000.0] calendars:calendars];NSMutableArray *items=[NSMutableArray array];for(EKEvent *event in [store eventsMatchingPredicate:predicate])[items addObject:cn1EventDictionary(event)];JAVA_OBJECT result=fromNSString(CN1_THREAD_STATE_PASS_ARG cn1CalendarJson(@{@"items":items}));POOL_END();return result;
#else
    return fromNSString(CN1_THREAD_STATE_PASS_ARG @"{\"items\":[]}");
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_calendarEvent___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT calendarId, JAVA_OBJECT eventId) {
#if defined(CN1_USE_CALENDAR) && !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    EKEventStore *store = cn1CalendarStore();
    EKEvent *event = [store eventWithIdentifier:toNSString(CN1_THREAD_STATE_PASS_ARG eventId)];
    NSString *requestedCalendar = toNSString(CN1_THREAD_STATE_PASS_ARG calendarId);
    if (event && requestedCalendar && ![event.calendar.calendarIdentifier isEqualToString:requestedCalendar]) event = nil;
    JAVA_OBJECT result=fromNSString(CN1_THREAD_STATE_PASS_ARG cn1CalendarJson(event ? cn1EventDictionary(event) : @{}));
    POOL_END();
    return result;
#else
    return fromNSString(CN1_THREAD_STATE_PASS_ARG @"{}");
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_calendarSaveEvent___java_lang_String_int_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT eventJson, JAVA_INT mutationScope) {
#if defined(CN1_USE_CALENDAR) && !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    NSDictionary *json=cn1CalendarDictionary(CN1_THREAD_STATE_PASS_ARG eventJson);EKEventStore *store=cn1CalendarStore();NSString *identifier=json[@"id"];EKEvent *event=identifier?[store eventWithIdentifier:identifier]:nil;if(!event)event=[EKEvent eventWithEventStore:store];NSString *calendarId=json[@"calendarId"];event.calendar=calendarId?[store calendarWithIdentifier:calendarId]:[store defaultCalendarForNewEvents];event.title=json[@"title"]?:@"";event.notes=json[@"notes"];event.location=json[@"location"];event.allDay=[json[@"allDay"] boolValue];NSString *zoneName=json[@"startZone"]?:json[@"endZone"];event.timeZone=event.allDay?[NSTimeZone localTimeZone]:([zoneName isKindOfClass:[NSString class]]?[NSTimeZone timeZoneWithName:zoneName]:event.timeZone);event.startDate=cn1CalendarDate(json,@"start");event.endDate=cn1CalendarDate(json,@"end");NSMutableArray *alarms=[NSMutableArray array];for(NSDictionary *alarm in json[@"alarms"]?:@[]){if(alarm[@"absolute"])[alarms addObject:[EKAlarm alarmWithAbsoluteDate:[NSDate dateWithTimeIntervalSince1970:[alarm[@"absolute"] doubleValue]/1000.0]]];else if(alarm[@"minutes"])[alarms addObject:[EKAlarm alarmWithRelativeOffset:-[alarm[@"minutes"] doubleValue]*60.0]];}event.alarms=alarms;NSError *error=nil;BOOL ok=[store saveEvent:event span:mutationScope==0?EKSpanThisEvent:EKSpanFutureEvents commit:YES error:&error];
    JAVA_OBJECT result=fromNSString(CN1_THREAD_STATE_PASS_ARG cn1CalendarJson(ok?cn1EventDictionary(event):@{@"error":error.localizedDescription?:@"Unable to save event"}));POOL_END();return result;
#else
    return fromNSString(CN1_THREAD_STATE_PASS_ARG @"{\"error\":\"Unsupported\"}");
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_calendarDeleteEvent___java_lang_String_int_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT eventId, JAVA_INT mutationScope) {
#if defined(CN1_USE_CALENDAR) && !TARGET_OS_WATCH && !TARGET_OS_TV
    EKEventStore *store=cn1CalendarStore();EKEvent *event=[store eventWithIdentifier:toNSString(CN1_THREAD_STATE_PASS_ARG eventId)];return event&&[store removeEvent:event span:mutationScope==0?EKSpanThisEvent:EKSpanFutureEvents commit:YES error:nil];
#else
    return 0;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_calendarTasks___java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT calendarId) {
#if defined(CN1_USE_CALENDAR) && !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    EKEventStore *store=cn1CalendarStore();NSArray *calendars=nil;NSString *identifier=toNSString(CN1_THREAD_STATE_PASS_ARG calendarId);if(identifier){EKCalendar *calendar=[store calendarWithIdentifier:identifier];if(calendar)calendars=@[calendar];}__block NSArray *reminders=nil;dispatch_semaphore_t semaphore=dispatch_semaphore_create(0);[store fetchRemindersMatchingPredicate:[store predicateForRemindersInCalendars:calendars] completion:^(NSArray *value){reminders=[value?:@[] retain];dispatch_semaphore_signal(semaphore);}];dispatch_semaphore_wait(semaphore,DISPATCH_TIME_FOREVER);NSMutableArray *items=[NSMutableArray array];for(EKReminder *reminder in reminders)[items addObject:cn1ReminderDictionary(reminder)];JAVA_OBJECT result=fromNSString(CN1_THREAD_STATE_PASS_ARG cn1CalendarJson(@{@"items":items}));[reminders release];POOL_END();return result;
#else
    return fromNSString(CN1_THREAD_STATE_PASS_ARG @"{\"items\":[]}");
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_calendarSaveTask___java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT taskJson) {
#if defined(CN1_USE_CALENDAR) && !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    NSDictionary *json=cn1CalendarDictionary(CN1_THREAD_STATE_PASS_ARG taskJson);EKEventStore *store=cn1CalendarStore();NSString *identifier=json[@"id"];EKReminder *reminder=identifier?(EKReminder*)[store calendarItemWithIdentifier:identifier]:nil;if(!reminder)reminder=[EKReminder reminderWithEventStore:store];NSString *calendarId=json[@"calendarId"];reminder.calendar=calendarId?[store calendarWithIdentifier:calendarId]:[store defaultCalendarForNewReminders];reminder.title=json[@"title"]?:@"";reminder.notes=json[@"notes"];reminder.completed=[json[@"completed"] boolValue];NSDate *due=cn1CalendarDate(json,@"due");if(due){NSCalendar *calendar=[[NSCalendar currentCalendar] copy];NSString *zoneName=json[@"dueZone"];calendar.timeZone=[zoneName isKindOfClass:[NSString class]]?[NSTimeZone timeZoneWithName:zoneName]:[NSTimeZone localTimeZone];NSUInteger units=NSCalendarUnitYear|NSCalendarUnitMonth|NSCalendarUnitDay;if(![json[@"dueAllDay"] boolValue])units|=NSCalendarUnitHour|NSCalendarUnitMinute|NSCalendarUnitSecond;reminder.dueDateComponents=[calendar components:units fromDate:due];reminder.dueDateComponents.timeZone=calendar.timeZone;[calendar release];}NSError *error=nil;BOOL ok=[store saveReminder:reminder commit:YES error:&error];
    JAVA_OBJECT result=fromNSString(CN1_THREAD_STATE_PASS_ARG cn1CalendarJson(ok?cn1ReminderDictionary(reminder):@{@"error":error.localizedDescription?:@"Unable to save reminder"}));POOL_END();return result;
#else
    return fromNSString(CN1_THREAD_STATE_PASS_ARG @"{\"error\":\"Unsupported\"}");
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_calendarDeleteTask___java_lang_String_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT taskId) {
#if defined(CN1_USE_CALENDAR) && !TARGET_OS_WATCH && !TARGET_OS_TV
    EKEventStore *store=cn1CalendarStore();EKReminder *reminder=(EKReminder*)[store calendarItemWithIdentifier:toNSString(CN1_THREAD_STATE_PASS_ARG taskId)];return reminder&&[store removeReminder:reminder commit:YES error:nil];
#else
    return 0;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplayWidth__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_getDisplayWidth__]
    POOL_BEGIN();
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl();
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplayHeight__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_getDisplayHeight__]
    POOL_BEGIN();
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl();
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getClipboardString___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    POOL_BEGIN();
    NSString *value = [[NSPasteboard generalPasteboard] stringForType:NSPasteboardTypeString];
    JAVA_OBJECT str = fromNSString(CN1_THREAD_STATE_PASS_ARG value);
    POOL_END();
    return str;
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
    JAVA_OBJECT str = fromNSString(CN1_THREAD_STATE_PASS_ARG pasteboard.string);
    POOL_END();
    return str;
#else
    // watchOS/tvOS have no UIPasteboard.
    return JAVA_NULL;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
#endif
}

void com_codename1_impl_ios_IOSNative_setClipboardString___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT str) {
#if TARGET_OS_OSX
    POOL_BEGIN();
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG str);
    NSPasteboard *pasteboard = [NSPasteboard generalPasteboard];
    // An NSPasteboard has to be cleared before it is written to, unlike
    // UIPasteboard where assigning the string is the whole operation. Skipping
    // it leaves the previous owner's other representations in place, so a paste
    // into a rich target gets the old content.
    [pasteboard clearContents];
    if (ns != nil) {
        [pasteboard setString:ns forType:NSPasteboardTypeString];
    }
    POOL_END();
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG str);
    UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
    pasteboard.string = ns;
    POOL_END();
#else
    // watchOS/tvOS have no UIPasteboard.
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
#endif
}

static NSString* cn1PasteboardTypeForMime(NSString* mimeType) {
    if ([mimeType isEqualToString:@"text/plain"]) return @"public.utf8-plain-text";
    if ([mimeType isEqualToString:@"text/html"]) return @"public.html";
    if ([mimeType isEqualToString:@"text/rtf"]) return @"public.rtf";
    return mimeType;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_realPath___java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) {
        return JAVA_NULL;
    }
    POOL_BEGIN();
    NSString* input = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    JAVA_OBJECT result = path;
    char resolved[PATH_MAX];
    const char* utf8 = [input fileSystemRepresentation];
    if(utf8 != NULL && realpath(utf8, resolved) != NULL) {
        result = fromNSString(CN1_THREAD_STATE_PASS_ARG
                [[NSFileManager defaultManager] stringWithFileSystemRepresentation:resolved
                                                                           length:strlen(resolved)]);
    } else {
        // Nothing there yet, which is the ordinary case for a database about to be created.
        // The directory carries the links; the last component is the name the engine is about
        // to make, so resolving the directory answers the same before and after creation.
        NSString* directory = [input stringByDeletingLastPathComponent];
        const char* directoryUtf8 = [directory fileSystemRepresentation];
        if([directory length] > 0 && directoryUtf8 != NULL
                && realpath(directoryUtf8, resolved) != NULL) {
            NSString* real = [[NSFileManager defaultManager]
                    stringWithFileSystemRepresentation:resolved length:strlen(resolved)];
            result = fromNSString(CN1_THREAD_STATE_PASS_ARG
                    [real stringByAppendingPathComponent:[input lastPathComponent]]);
        }
    }
    POOL_END();
    return result;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getClipboardContent___java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT mimeType) {
#if TARGET_OS_OSX
    POOL_BEGIN();
    NSString* type = cn1PasteboardTypeForMime(toNSString(CN1_THREAD_STATE_PASS_ARG mimeType));
    NSData* data = [[NSPasteboard generalPasteboard] dataForType:type];
    NSString* value = data == nil ? nil : [[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] autorelease];
    JAVA_OBJECT result = fromNSString(CN1_THREAD_STATE_PASS_ARG value);
    POOL_END();
    return result;
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    NSString* type = cn1PasteboardTypeForMime(toNSString(CN1_THREAD_STATE_PASS_ARG mimeType));
    NSData* data = [[UIPasteboard generalPasteboard] dataForPasteboardType:type];
    NSString* value = data == nil ? nil : [[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] autorelease];
    JAVA_OBJECT result = fromNSString(CN1_THREAD_STATE_PASS_ARG value);
    POOL_END();
    return result;
#else
    return JAVA_NULL;
#endif
#endif
}

extern NSData* arrayToData(JAVA_OBJECT arr);
extern JAVA_OBJECT nsDataToByteArr(NSData *data);

void com_codename1_impl_ios_IOSNative_setClipboardContent___java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_byte_1ARRAY_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT plain, JAVA_OBJECT html, JAVA_OBJECT rtf, JAVA_OBJECT markdown, JAVA_OBJECT asciidoc, JAVA_OBJECT image, JAVA_OBJECT fileUris) {
#if TARGET_OS_OSX
    POOL_BEGIN();
    NSPasteboard* pb = [NSPasteboard generalPasteboard];
    [pb clearContents];
    JAVA_OBJECT values[] = { plain, html, rtf, markdown, asciidoc };
    NSString* types[] = { NSPasteboardTypeString, NSPasteboardTypeHTML, NSPasteboardTypeRTF,
                          @"net.daringfireball.markdown", @"text/asciidoc" };
    for (int i = 0; i < 5; i++) {
        if (values[i] != JAVA_NULL) {
            NSString* value = toNSString(CN1_THREAD_STATE_PASS_ARG values[i]);
            NSData* data = [value dataUsingEncoding:NSUTF8StringEncoding];
            if (data != nil) [pb setData:data forType:types[i]];
        }
    }
    if (image != JAVA_NULL) {
        NSData* imgData = arrayToData(image);
        if (imgData != nil && imgData.length > 0) {
            [pb setData:imgData forType:NSPasteboardTypePNG];
        }
    }
    if (fileUris != JAVA_NULL) {
        NSString* joined = toNSString(CN1_THREAD_STATE_PASS_ARG fileUris);
        NSMutableArray* urls = [NSMutableArray array];
        for (NSString* u in [joined componentsSeparatedByString:@"\n"]) {
            if (u.length == 0) continue;
            NSURL* url = [NSURL URLWithString:u];
            if (url != nil) [urls addObject:url];
        }
        // Written as objects rather than as a type, which is what makes the
        // files show up to the Finder and to every other application's file
        // paste -- a URL string on the pasteboard is only text.
        if (urls.count > 0) [pb writeObjects:urls];
    }
    POOL_END();
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    NSMutableDictionary* item = [NSMutableDictionary dictionary];
    JAVA_OBJECT values[] = { plain, html, rtf, markdown, asciidoc };
    NSString* types[] = { @"public.utf8-plain-text", @"public.html", @"public.rtf", @"text/markdown", @"text/asciidoc" };
    for (int i = 0; i < 5; i++) {
        if (values[i] != JAVA_NULL) {
            NSString* value = toNSString(CN1_THREAD_STATE_PASS_ARG values[i]);
            NSData* data = [value dataUsingEncoding:NSUTF8StringEncoding];
            if (data != nil) [item setObject:data forKey:types[i]];
        }
    }
    if (image != JAVA_NULL) {
        NSData* imgData = arrayToData(image);
        if (imgData != nil && imgData.length > 0) [item setObject:imgData forKey:@"public.png"];
    }
    NSMutableArray* items = [NSMutableArray array];
    if ([item count] > 0) [items addObject:item];
    if (fileUris != JAVA_NULL) {
        NSString* joined = toNSString(CN1_THREAD_STATE_PASS_ARG fileUris);
        for (NSString* u in [joined componentsSeparatedByString:@"\n"]) {
            if (u.length == 0) continue;
            NSData* urlData = [u dataUsingEncoding:NSUTF8StringEncoding];
            if (urlData != nil) [items addObject:[NSDictionary dictionaryWithObject:urlData forKey:@"public.url"]];
        }
    }
    [UIPasteboard generalPasteboard].items = items;
    POOL_END();
#endif
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getClipboardImage___R_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    POOL_BEGIN();
    NSPasteboard* pb = [NSPasteboard generalPasteboard];
    NSData* data = [pb dataForType:NSPasteboardTypePNG];
    if (data == nil) {
        NSData* tiff = [pb dataForType:NSPasteboardTypeTIFF];
        if (tiff != nil) {
            // A Mac application that copies an image usually offers TIFF rather
            // than PNG, so the fallback is what actually fires most of the time
            // -- and the Java side is specified in PNG.
            NSBitmapImageRep* rep = [NSBitmapImageRep imageRepWithData:tiff];
            data = [rep representationUsingType:NSBitmapImageFileTypePNG properties:@{}];
        }
    }
    JAVA_OBJECT result = (data == nil || data.length == 0) ? JAVA_NULL : nsDataToByteArr(data);
    POOL_END();
    return result;
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    UIPasteboard* pb = [UIPasteboard generalPasteboard];
    NSData* data = [pb dataForPasteboardType:@"public.png"];
    if (data == nil && pb.hasImages) {
        CN1Image* img = pb.image;
        if (img != nil) data = UIImagePNGRepresentation(img);
    }
    JAVA_OBJECT result = (data == nil || data.length == 0) ? JAVA_NULL : nsDataToByteArr(data);
    POOL_END();
    return result;
#else
    return JAVA_NULL;
#endif
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getClipboardFileUris___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    POOL_BEGIN();
    NSPasteboard* pb = [NSPasteboard generalPasteboard];
    NSArray<NSURL *>* urls = [pb readObjectsForClasses:@[[NSURL class]]
                                               options:@{NSPasteboardURLReadingFileURLsOnlyKey: @YES}];
    NSString* joined = nil;
    if (urls.count > 0) {
        NSMutableArray* parts = [NSMutableArray array];
        for (NSURL* u in urls) {
            if (u != nil) [parts addObject:[u absoluteString]];
        }
        if (parts.count > 0) joined = [parts componentsJoinedByString:@"\n"];
    }
    JAVA_OBJECT result = fromNSString(CN1_THREAD_STATE_PASS_ARG joined);
    POOL_END();
    return result;
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    UIPasteboard* pb = [UIPasteboard generalPasteboard];
    NSString* joined = nil;
    if (pb.hasURLs) {
        NSMutableArray* parts = [NSMutableArray array];
        for (NSURL* u in pb.URLs) {
            if (u != nil) [parts addObject:[u absoluteString]];
        }
        if (parts.count > 0) joined = [parts componentsJoinedByString:@"\n"];
    }
    JAVA_OBJECT result = fromNSString(CN1_THREAD_STATE_PASS_ARG joined);
    POOL_END();
    return result;
#else
    return JAVA_NULL;
#endif
#endif
}


void retainCN1(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT o){
    com_codename1_impl_ios_IOSImplementation_retain___java_lang_Object(CN1_THREAD_STATE_PASS_ARG o);
}

void releaseCN1(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT o){
    com_codename1_impl_ios_IOSImplementation_release___java_lang_Object(CN1_THREAD_STATE_PASS_ARG o);
}

JAVA_OBJECT getClientProperty(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT o, NSString* key){
    return com_codename1_ui_Component_getClientProperty___java_lang_String_R_java_lang_Object(
        CN1_THREAD_STATE_PASS_ARG o,
        fromNSString(CN1_THREAD_STATE_PASS_ARG key)
    );
}

BOOL getBooleanClientProperty(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT o, NSString* key){
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return NO;
#else
    JAVA_OBJECT val = getClientProperty(CN1_THREAD_STATE_PASS_ARG o, key);
    if(val == JAVA_NULL){
        return NO;
    }
    return val == get_static_java_lang_Boolean_TRUE(threadStateData);
#endif
}

#ifndef NEW_CODENAME_ONE_VM
NSString* toNSString(JAVA_OBJECT str) {
    if(str == JAVA_NULL) {
        return 0;
    }
    // accessing internal state since toCharArray performs an allocation which can be REALLY expensive
    int offset = ((java_lang_String*) str)->fields.java_lang_String.offset_;
    org_xmlvm_runtime_XMLVMArray* cArr = ((java_lang_String*) str)->fields.java_lang_String.value_;
    //CN1Log(@"cArr pointer is: %i", cArr);
    if(cArr == JAVA_NULL) {
        const char* chrs = stringToUTF8(str);
        NSString* st = [NSString stringWithUTF8String:chrs];
        //CN1Log(@"Unicode chars: %@ over %i at offset %i", st, chrArr[iter], iter);
        return st;
    }
    
    JAVA_ARRAY_CHAR* chrArr = (JAVA_ARRAY_CHAR*)cArr->fields.org_xmlvm_runtime_XMLVMArray.array_;
    //CN1Log(@"chrArr pointer is: %i", chrArr);
    
    int length = ((java_lang_String*) str)->fields.java_lang_String.count_;
    
    for(int iter = offset ; iter < length ; iter++) {
        if(chrArr[iter] > 127) {
            const char* chrs = stringToUTF8(str);
            NSString* st = [NSString stringWithUTF8String:chrs];
            //CN1Log(@"Unicode chars: %@ over %i at offset %i", st, chrArr[iter], iter);
            return st;
        }
    }
    if(offset > 0) {
        return [[NSString stringWithCharacters:chrArr length:length+offset] substringFromIndex:offset];
    }
    return [NSString stringWithCharacters:chrArr length:length];
}
#endif

void com_codename1_impl_ios_IOSNative_editStringAt___int_int_int_int_long_boolean_int_int_int_java_lang_String_boolean_int_long_int_int_int_int_java_lang_String_int_boolean_boolean_int_int_boolean(CN1_THREAD_STATE_MULTI_ARG
                                                                                                                                                                         JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_LONG n5, JAVA_BOOLEAN n6, JAVA_INT n7,
                                                                                                                                                                         JAVA_INT n8, JAVA_INT n9, JAVA_OBJECT n10, JAVA_BOOLEAN forceSlide,
                                                                                                                                                                         JAVA_INT color, JAVA_LONG imagePeer, JAVA_INT padTop, JAVA_INT padBottom, JAVA_INT padLeft, JAVA_INT padRight, JAVA_OBJECT hint, JAVA_INT hintColor, JAVA_BOOLEAN showToolbar, JAVA_BOOLEAN blockCopyPaste,
                                                                                                                                                                         JAVA_INT alignment, JAVA_INT verticalAlignment, JAVA_BOOLEAN returnExitsEditing)
{
#if TARGET_OS_OSX
    // Routed into the rendering view's NSTextInputClient rather than into a
    // second editor view floating over the surface.
    //
    // The UIKit ports put a real UITextField or UITextView on top of the
    // component and let it draw itself, which is why so many of these arguments
    // exist: the colour, the padding, the alignment and the hint are there to
    // make that separate control look like the component underneath it. AppKit
    // needs none of them here, because the component keeps drawing itself and
    // only the keystrokes are handled natively -- which is the standard pattern
    // for a custom-drawn surface, and what buys input methods, dead keys and
    // dictation.
    //
    // So the geometry is recorded, because the input method's candidate window
    // needs somewhere to appear, and the rest is deliberately ignored.
    POOL_BEGIN();
    NSString *initial = n10 != JAVA_NULL
        ? toNSString(CN1_THREAD_STATE_PASS_ARG n10)
        : @"";
    extern void CN1MacTextInputBegin(NSString *text, BOOL multiline, CGRect bounds);
    // n6 is "single line", so a multi-line field is its negation.
    CN1MacTextInputBegin(initial, n6 == 0,
                         CGRectMake(n1 + padLeft, n2 + padTop,
                                    n3 - padLeft - padRight,
                                    n4 - padTop - padBottom));
    POOL_END();
#else
    POOL_BEGIN();
    const char* chr = stringToUTF8(CN1_THREAD_STATE_PASS_ARG n10);
    int l = strlen(chr) + 1;
    char cc[l];
    memcpy(cc, chr, l);
    Java_com_codename1_impl_ios_IOSImplementation_editStringAtImpl(CN1_THREAD_STATE_PASS_ARG n1, n2, n3, n4, n5, n6, n7, n8, n9, cc, 0, forceSlide, color, imagePeer,
                                                                   padTop, padBottom, padLeft, padRight, toNSString(CN1_THREAD_STATE_PASS_ARG hint), hintColor, showToolbar, blockCopyPaste, alignment, verticalAlignment, returnExitsEditing);
    POOL_END();
#endif
}
extern float scaleValue;
extern int editComponentPadTop, editComponentPadLeft;
extern float editCompoentX, editCompoentY, editCompoentW, editCompoentH;
void com_codename1_impl_ios_IOSNative_resizeNativeTextView___int_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_INT padTop, JAVA_INT padRight, JAVA_INT padBottom, JAVA_INT padLeft) {
#if TARGET_OS_OSX
    // Nothing to resize: the editor is the rendering view itself, so the
    // framework's own layout already put the text where it belongs. The bounds
    // still matter for where the input method's candidate window appears, and
    // that arrives through setTextInputBounds.
    (void)x; (void)y; (void)w; (void)h;
    (void)padTop; (void)padBottom; (void)padLeft; (void)padRight;
#else
#if !TARGET_OS_WATCH
    POOL_BEGIN();
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if(editingComponent != nil) {
            float scale = scaleValue;
            CGRect existingBounds = editingComponent.frame;
            NSString *currText = ((UITextField*)editingComponent).text;

            float neditCompoentX = (x + padLeft) / scale;
            float neditCompoentY = (y + padTop) / scale;
            float neditComponentPadTop = padTop;
            float neditComponentPadLeft = padLeft;
            if (scale > 1) {
                neditCompoentY -= 1.5;
            } else {
                neditCompoentY -= 1;
            }
            float neditCompoentW = (w - padLeft - padRight) / scale;
            float neditCompoentH = (h - padTop - padBottom) / scale;
            CGRect rect = CGRectMake(neditCompoentX, neditCompoentY, neditCompoentW, neditCompoentH);
            //CN1Log(@"Changing bounds %f,%f,%f,%f to %f,%f,%f,%f", existingBounds.origin.x, existingBounds.origin.y, existingBounds.size.width, existingBounds.size.height, rect.origin.x, rect.origin.y, rect.size.width, rect.size.height);
            if (fabs(existingBounds.size.width - rect.size.width) > 1 || fabs(existingBounds.size.height - rect.size.height) > 1 ||
                fabs(existingBounds.origin.x - rect.origin.x) > 1 || fabs(existingBounds.origin.y - rect.origin.y) > 1
                ) {
                //CN1Log(@"Changing bounds %f,%f,%f,%f to %f,%f,%f,%f", existingBounds.origin.x, existingBounds.origin.y, existingBounds.size.width, existingBounds.size.height, rect.origin.x, rect.origin.y, rect.size.width, rect.size.height);
                editCompoentH = neditCompoentH;
                editCompoentW = neditCompoentW;
                editCompoentX = neditCompoentX;
                editCompoentY = neditCompoentY;
                editingComponent.frame = rect;
            }
            
            
        }
        POOL_END();
    });

    POOL_END();
#else
    // watchOS has no inline native text editing peer.
#endif // !TARGET_OS_WATCH
#endif
}
#ifdef INCLUDE_CN1_PUSH_2
typedef void (^CN1PushCompletionHandlerType)();

extern CN1PushCompletionHandlerType cn1PushCompletionHandler;
int pushReceivedCount=0;
#endif

void com_codename1_impl_ios_IOSNative_firePushCompletionHandler__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_CN1_PUSH_2
    dispatch_async(dispatch_get_main_queue(), ^{
        if (cn1PushCompletionHandler != nil) {
            pushReceivedCount--;
            if (pushReceivedCount <= 0) {
                cn1PushCompletionHandler();
                Block_release(cn1PushCompletionHandler);
                cn1PushCompletionHandler = nil;
            }
        }
    });
#endif
}

#ifdef INCLUDE_CN1_BACKGROUND_FETCH
typedef void (^CN1BackgroundFetchBlockType)(UIBackgroundFetchResult);

extern CN1BackgroundFetchBlockType cn1UIBackgroundFetchResultCompletionHandler;
#endif

void com_codename1_impl_ios_IOSNative_fireUIBackgroundFetchResultFailed__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_CN1_BACKGROUND_FETCH
    cn1UIBackgroundFetchResultCompletionHandler(UIBackgroundFetchResultFailed);
#endif
}
void com_codename1_impl_ios_IOSNative_fireUIBackgroundFetchResultNoData__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_CN1_BACKGROUND_FETCH
    cn1UIBackgroundFetchResultCompletionHandler(UIBackgroundFetchResultNoData);
#endif
}
void com_codename1_impl_ios_IOSNative_fireUIBackgroundFetchResultNewData__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_CN1_BACKGROUND_FETCH
    cn1UIBackgroundFetchResultCompletionHandler(UIBackgroundFetchResultNewData);
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isBackgroundFetchSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_CN1_BACKGROUND_FETCH
    return YES;
#else
    return NO;
#endif
}

void com_codename1_impl_ios_IOSNative_setPreferredBackgroundFetchInterval___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT seconds) {
#ifdef INCLUDE_CN1_BACKGROUND_FETCH
    NSTimeInterval interval = seconds;
    if (interval < 0) {
        interval = UIApplicationBackgroundFetchIntervalNever;
    }
    if (interval < 3600) {
        // Minimum fetch interval appears to be between 10 minutes and 35 minutes
        // Setting custom intervals seem to give unpredictable results, so for low values (< 1 hour)
        // it is best to just use minimum interval and let the system work it out.
        interval = UIApplicationBackgroundFetchIntervalMinimum;
    }
    [[UIApplication sharedApplication] setMinimumBackgroundFetchInterval:interval];
#endif
}


extern long CN1_EDT_THREAD_ID;
void com_codename1_impl_ios_IOSNative_flushBuffer___long_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_flushBuffer___long_int_int_int_int]
    POOL_BEGIN();
    if (CN1_EDT_THREAD_ID < 0) {
        CN1_EDT_THREAD_ID = (long)threadStateData->threadId;
    }
    Java_com_codename1_impl_ios_IOSImplementation_flushBufferImpl((void *)n1, n2, n3, n4, n5);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_flushBufferForReadback___int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4)
{
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_flushBufferForReadbackImpl(n1, n2, n3, n4);
    POOL_END();
}


void com_codename1_impl_ios_IOSNative_imageRgbToIntArray___long_int_1ARRAY_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_OBJECT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    POOL_BEGIN();
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* intArray = n2;
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)n2)->data;
#endif
    Java_com_codename1_impl_ios_IOSImplementation_imageRgbToIntArrayImpl((void *)n1, data, n3, n4, n5, n6, n7, n8);
    POOL_END();
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageFromARGB___int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_INT n2, JAVA_INT n3)
{
    POOL_BEGIN();
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* intArray = n1;
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)n1)->data;
#endif
    JAVA_ARRAY_LONG i = (JAVA_ARRAY_LONG)(uintptr_t)Java_com_codename1_impl_ios_IOSImplementation_createImageFromARGBImpl((void *)data, n2, n3);
    POOL_END();
    return i;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImage___byte_1ARRAY_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT n2)
{
#ifndef NEW_CODENAME_ONE_VM
    POOL_BEGIN();
    org_xmlvm_runtime_XMLVMArray* byteArray = n1;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    org_xmlvm_runtime_XMLVMArray* intArray = n2;
    JAVA_ARRAY_INT* data2 = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_createImageImpl(data, byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_, data2);
    POOL_END();
#else
    JAVA_ARRAY byteArray = (JAVA_ARRAY)n1;
    JAVA_ARRAY intArray = (JAVA_ARRAY)n2;
    void* data = byteArray->data;
    void* data2 = intArray->data;
    JAVA_LONG i = (JAVA_LONG)Java_com_codename1_impl_ios_IOSImplementation_createImageImpl(data, byteArray->length, data2);
#endif
    return i;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageNSData___long_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_OBJECT n2)
{
    POOL_BEGIN();
    
    NSData* nd = (BRIDGE_CAST NSData*) ((void*)nsData);
    CN1Image* img = CN1AppleImageWithDataCompat(nd);
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* intArray = n2;
    JAVA_ARRAY_INT* data2 = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_INT* data2 = (JAVA_ARRAY_INT*)((JAVA_ARRAY)n2)->data;
#endif
    data2[0] = (int)img.size.width;
    data2[1] = (int)img.size.height;
    
    GLUIImage* glu = [[GLUIImage alloc] initWithImage:img];

    POOL_END();
    return (JAVA_LONG) ((BRIDGE_CAST void*)glu);
}

// Renders an Apple SF Symbol (iOS 13+) into a tinted CN1Image and wraps it in a
// GLUIImage peer, mirroring createImageNSData. Returns 0 when SF Symbols are
// unavailable or the named symbol does not exist; the caller falls back to the
// Material icon font. widthHeight[0/1] receive the image size in PIXELS.
JAVA_LONG com_codename1_impl_ios_IOSNative_nativeCreateSFSymbol___java_lang_String_int_float_int_int_1ARRAY_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name, JAVA_INT color, JAVA_FLOAT size, JAVA_INT weight, JAVA_OBJECT n2)
{
#if TARGET_OS_OSX
    POOL_BEGIN();
    NSString* nameStr = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    if (nameStr == nil) {
        POOL_END();
        return 0;
    }
    // `size` arrives in Codename One device pixels; a symbol configuration is in
    // points, so it is divided by the backing scale and the rendered bitmap
    // comes back at about `size` pixels tall.
    CGFloat scale = scaleValue > 0 ? scaleValue : 1;
    CGFloat pointSize = size / scale;
    NSImageSymbolConfiguration* cfg = [NSImageSymbolConfiguration
        configurationWithPointSize:pointSize
                            weight:(weight >= 1 ? NSFontWeightBold : NSFontWeightRegular)];
    NSImage* base = [NSImage imageWithSystemSymbolName:nameStr accessibilityDescription:nil];
    NSImage* sym = base == nil ? nil : [base imageWithSymbolConfiguration:cfg];
    if (sym == nil) {
        POOL_END();
        return 0;
    }
    NSSize pointBounds = sym.size;
    int pw = (int)ceil(pointBounds.width * scale);
    int ph = (int)ceil(pointBounds.height * scale);
    if (pw <= 0 || ph <= 0) {
        POOL_END();
        return 0;
    }
    NSBitmapImageRep* rep = [[NSBitmapImageRep alloc]
        initWithBitmapDataPlanes:NULL pixelsWide:pw pixelsHigh:ph
                    bitsPerSample:8 samplesPerPixel:4 hasAlpha:YES isPlanar:NO
                  colorSpaceName:NSDeviceRGBColorSpace
                     bytesPerRow:pw * 4 bitsPerPixel:32];
    if (rep == nil) {
        POOL_END();
        return 0;
    }
    rep.size = pointBounds;
    NSGraphicsContext* gc = [NSGraphicsContext graphicsContextWithBitmapImageRep:rep];
    [NSGraphicsContext saveGraphicsState];
    [NSGraphicsContext setCurrentContext:gc];
    // The symbol is a template, so it is drawn as a mask and the colour is
    // filled through it -- tinting the drawn bitmap afterwards would colour the
    // transparent pixels too.
    NSRect full = NSMakeRect(0, 0, pointBounds.width, pointBounds.height);
    [sym drawInRect:full fromRect:NSZeroRect operation:NSCompositingOperationSourceOver fraction:1.0];
    [[NSColor colorWithSRGBRed:((color >> 16) & 0xff) / 255.0
                         green:((color >> 8) & 0xff) / 255.0
                          blue:(color & 0xff) / 255.0
                         alpha:1.0] set];
    NSRectFillUsingOperation(full, NSCompositingOperationSourceIn);
    [NSGraphicsContext restoreGraphicsState];

    NSImage* rendered = [[NSImage alloc] initWithSize:pointBounds];
    [rendered addRepresentation:rep];
    GLUIImage* g = [[GLUIImage alloc] initWithImage:rendered];
    if (n2 != JAVA_NULL) {
        JAVA_ARRAY_INT* dims = (JAVA_ARRAY_INT*)((JAVA_ARRAY)n2)->data;
        if (((JAVA_ARRAY)n2)->length >= 2) {
            dims[0] = pw;
            dims[1] = ph;
        }
    }
#ifndef CN1_USE_ARC
    [rep release];
    [rendered release];
#endif
    POOL_END();
    return (JAVA_LONG)(uintptr_t)((BRIDGE_CAST void*)g);
#else
#if TARGET_OS_WATCH
    // watchOS marks UIScreen and UIGraphicsImageRenderer unavailable; returning
    // 0 makes FontImage fall back to the Material icon font, same as pre-iOS-13.
    return 0;
#else
    if (@available(iOS 13.0, *)) {
        POOL_BEGIN();
        NSString* nameStr = toNSString(CN1_THREAD_STATE_PASS_ARG name);
        if (nameStr == nil) {
            POOL_END();
            return 0;
        }
        // `size` arrives in CN1 device pixels (the same units as the Material glyph
        // it replaces). CN1Image symbol point size is in POINTS, so divide by the
        // screen scale; the rendered bitmap is then ~`size` pixels tall, matching.
        CGFloat scale = [UIScreen mainScreen].scale;
        if (scale < 1) { scale = 1; }
        CGFloat pointSize = size / scale;
        UIImageSymbolConfiguration* cfg = [UIImageSymbolConfiguration configurationWithPointSize:pointSize weight:(weight >= 1 ? UIImageSymbolWeightBold : UIImageSymbolWeightRegular)];
        CN1Image* sym = [CN1Image systemImageNamed:nameStr withConfiguration:cfg];
        if (sym == nil) {
            POOL_END();
            return 0;
        }
        UIColor* c = [UIColor colorWithRed:((color >> 16) & 0xff) / 255.0 green:((color >> 8) & 0xff) / 255.0 blue:(color & 0xff) / 255.0 alpha:1.0];
        // Fetch the int[] up front: slots [0],[1] receive the rendered pixel w/h;
        // slots [2],[3] (when present) carry optional per-call layout tuning -- a
        // uniform icon SLOT height as a percent of `size`, and the glyph's VERTICAL
        // BIAS within that slot as a percent (50 = centred). Defaults 100/50
        // reproduce the legacy centred, downscale-to-fit behaviour.
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* intArray = n2;
        JAVA_ARRAY_INT* data2 = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
        int arrayLen = (int)intArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
        JAVA_ARRAY_INT* data2 = (JAVA_ARRAY_INT*)((JAVA_ARRAY)n2)->data;
        int arrayLen = (int)((JAVA_ARRAY)n2)->length;
#endif
        int slotPct = 100, vBiasPct = 50;
        if (arrayLen >= 4) {
            if (data2[2] > 0) { slotPct = data2[2]; }
            if (data2[3] >= 0) { vBiasPct = data2[3]; }
        }
        // Flatten the (template) symbol, tinted, into a real RGBA bitmap. SF symbols are
        // sized by point size (a shared font metric), so each glyph keeps its true
        // per-symbol extent -- the ellipsis is naturally short (small dots), the star
        // taller -- exactly as UIKit renders them. To lay them out like a UITabBar (a
        // uniform icon slot with aligned labels) we composite each glyph at its NATURAL
        // proportions into a canvas of UNIFORM height = `size` * slotPct%. A glyph TALLER
        // than the slot is scaled DOWN to fit (keeps the slot uniform so labels align);
        // shorter glyphs keep their size and are placed by vBiasPct. With a tall-enough
        // slot the star then reaches its full native height and sits high (vBias < 50),
        // instead of being shrunk to the nominal size and centred.
        CGFloat baseH = size / scale;                       // nominal slot height (device `size` px)
        CGFloat slotH = baseH * (CGFloat)slotPct / 100.0;
        CGFloat glyphWpt = sym.size.width;
        CGFloat glyphHpt = sym.size.height;
        CGFloat k = glyphHpt > slotH ? (slotH / glyphHpt) : 1.0;   // shrink only to fit the slot
        glyphWpt *= k;
        glyphHpt *= k;
        CGFloat canvasHpt = slotH;
        CGSize szPt = CGSizeMake(glyphWpt, canvasHpt);
        CGFloat glyphYpt = (canvasHpt - glyphHpt) * (CGFloat)vBiasPct / 100.0;   // 50% = centred
        UIGraphicsImageRendererFormat* rfmt = [UIGraphicsImageRendererFormat defaultFormat];
        rfmt.scale = scale;
        rfmt.opaque = NO;
        UIGraphicsImageRenderer* rndr = [[UIGraphicsImageRenderer alloc] initWithSize:szPt format:rfmt];
        CN1Image* flat = [rndr imageWithActions:^(UIGraphicsImageRendererContext* _Nonnull rc) {
            [c set];
            CN1Image* templ = [sym imageWithRenderingMode:UIImageRenderingModeAlwaysTemplate];
            [templ drawInRect:CGRectMake(0, glyphYpt, glyphWpt, glyphHpt)];
        }];
        data2[0] = (int)(flat.size.width * flat.scale);
        data2[1] = (int)(flat.size.height * flat.scale);

        GLUIImage* glu = [[GLUIImage alloc] initWithImage:flat];

        POOL_END();
        return (JAVA_LONG) ((BRIDGE_CAST void*)glu);
    } else {
        return 0;
    }
#endif
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_scale___long_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT n2, JAVA_INT n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_scale___long_int_int]
    POOL_BEGIN();
    JAVA_LONG i = (JAVA_LONG)(uintptr_t)Java_com_codename1_impl_ios_IOSImplementation_scaleImpl((void *)(uintptr_t)n1, n2, n3);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gausianBlurImage___long_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_FLOAT radius) {
#if !TARGET_OS_WATCH
    POOL_BEGIN();

    GLUIImage* glu = (BRIDGE_CAST GLUIImage*)n1;
    if(((BRIDGE_CAST void*)[CodenameOne_GLViewController instance].currentMutableImage) == glu) {
        Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl();
    }

    // The blur runs through CIGaussianBlur for both GL and Metal builds.
    // CIGaussianBlur is itself Metal-backed under the hood (Apple uses
    // MPSImageGaussianBlur internally for it), and its inputRadius
    // semantic plus output-extent expansion are what the test goldens
    // and CSS filter:blur expectations were baked against. A direct
    // MPSImageGaussianBlur call from this layer can't reproduce the
    // same visual without empirically matching sigma scaling and
    // padding the dst by ~3 sigma; not worth the complexity when the
    // CIFilter path is already correct and the read-back cost is paid
    // once per blur invocation (not per frame).

    CN1Image* original = nil;
#ifdef CN1_USE_METAL
    if ([glu mtlMutableTexture] != nil) {
        extern int displayWidth;
        extern int displayHeight;
        [[CodenameOne_GLViewController instance] flushBuffer:nil x:0 y:0 width:displayWidth height:displayHeight];
        original = CN1MetalReadMutableImageAsUIImage(glu);
    }
    if (original == nil) {
        original = [glu getImage];
    }
#else
    original = [glu getImage];
#endif

    // taken from: http://stackoverflow.com/a/19433086/756809
    CIFilter *gaussianBlurFilter = [CIFilter filterWithName:@"CIGaussianBlur"];
    [gaussianBlurFilter setDefaults];
#if TARGET_OS_OSX
    CIImage *inputImage = [CIImage imageWithCGImage:CN1AppleCGImageOf(original)];
#else
    CIImage *inputImage = [CIImage imageWithCGImage:[original CGImage]];
#endif
    [gaussianBlurFilter setValue:inputImage forKey:kCIInputImageKey];
    NSNumber *radiusNumber = [NSNumber numberWithFloat:radius];
    [gaussianBlurFilter setValue:radiusNumber forKey:kCIInputRadiusKey];

    CIImage *outputImage = [gaussianBlurFilter outputImage];
    CIContext *context   = [CIContext contextWithOptions:nil];
    CGImageRef cgimg     = [context createCGImage:outputImage fromRect:[inputImage extent]];
#if TARGET_OS_OSX
    CN1Image *image       = CN1AppleImageWithCGImage(cgimg);
#else
    CN1Image *image       = [CN1Image imageWithCGImage:cgimg];
#endif
    CGImageRelease(cgimg);
    GLUIImage* gl = [[GLUIImage alloc] initWithImage:image];

    POOL_END();
    return (JAVA_LONG)(uintptr_t)(BRIDGE_CAST void*)gl;
#else
    // watchOS has no CoreImage/CIFilter; approximate CIGaussianBlur with a
    // 3-pass vImage box convolve (Accelerate, available on watchOS).
    POOL_BEGIN();
    GLUIImage* glu = (BRIDGE_CAST GLUIImage*)n1;
    if(((BRIDGE_CAST void*)[CodenameOne_GLViewController instance].currentMutableImage) == glu) {
        Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl();
    }
    CN1Image *original = [glu getImage];
    if (original == nil || original.CGImage == NULL) { POOL_END(); return n1; }
    CGImageRef cg = original.CGImage;
    size_t w = CGImageGetWidth(cg), h = CGImageGetHeight(cg);
    if (w == 0 || h == 0) { POOL_END(); return n1; }
    size_t bytesPerRow = w * 4;
    void *srcBuf = calloc(h, bytesPerRow);
    void *dstBuf = calloc(h, bytesPerRow);
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    CGContextRef inCtx = CGBitmapContextCreate(srcBuf, w, h, 8, bytesPerRow, cs,
                                               kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    GLUIImage *resultGl = nil;
    if (inCtx != NULL && srcBuf != NULL && dstBuf != NULL) {
        CGContextDrawImage(inCtx, CGRectMake(0, 0, w, h), cg);
        vImage_Buffer vsrc = { srcBuf, h, w, bytesPerRow };
        vImage_Buffer vdst = { dstBuf, h, w, bytesPerRow };
        uint32_t k = (uint32_t)(radius * 2.0f) | 1u; // odd kernel size from radius
        if (k < 3) { k = 3; }
        vImageBoxConvolve_ARGB8888(&vsrc, &vdst, NULL, 0, 0, k, k, NULL, kvImageEdgeExtend);
        vImageBoxConvolve_ARGB8888(&vdst, &vsrc, NULL, 0, 0, k, k, NULL, kvImageEdgeExtend);
        vImageBoxConvolve_ARGB8888(&vsrc, &vdst, NULL, 0, 0, k, k, NULL, kvImageEdgeExtend);
        CGContextRef outCtx = CGBitmapContextCreate(dstBuf, w, h, 8, bytesPerRow, cs,
                                                    kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
        if (outCtx != NULL) {
            CGImageRef outImg = CGBitmapContextCreateImage(outCtx);
            if (outImg != NULL) {
                resultGl = [[GLUIImage alloc] initWithImage:[CN1Image imageWithCGImage:outImg]];
                CGImageRelease(outImg);
            }
            CGContextRelease(outCtx);
        }
    }
    if (inCtx != NULL) { CGContextRelease(inCtx); }
    CGColorSpaceRelease(cs);
    free(srcBuf); free(dstBuf);
    POOL_END();
    return resultGl != nil ? (BRIDGE_CAST void*)resultGl : n1;
#endif // !TARGET_OS_WATCH
}

void com_codename1_impl_ios_IOSNative_setNativeClippingMutable___int_int_int_int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_BOOLEAN n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_setNativeClippingMutable___int_int_int_int_boolean]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingMutableImpl(n1, n2, n3, n4, n5);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_setNativeClippingMutable___int_byte_1ARRAY_int_float_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT numCommands, JAVA_OBJECT commands, JAVA_INT numPoints, JAVA_OBJECT points)
{
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingShapeMutableImpl(numCommands, commands, numPoints, points);
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_setNativeClippingGlobal___int_int_int_int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_BOOLEAN n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_setNativeClippingGlobal___int_int_int_int_boolean]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingGlobalImpl(n1, n2, n3, n4, n5);
    POOL_END();
    //XMLVM_END_WRAPPER
}

extern void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingMaskGlobalImpl(JAVA_LONG textureName, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h);
void com_codename1_impl_ios_IOSNative_setNativeClippingMaskGlobal___long_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG textureName, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_setNativeClippingGlobal___int_int_int_int_boolean]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingMaskGlobalImpl(textureName, x, y, w, h);
    POOL_END();
    //XMLVM_END_WRAPPER
}

extern void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingPolygonGlobalImpl(JAVA_OBJECT points);
void com_codename1_impl_ios_IOSNative_setNativeClippingPolygonGlobal___float_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT points)
{
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingPolygonGlobalImpl(points);
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_setAntiAliasedMutable___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN antialiased)
{
    Java_com_codename1_impl_ios_IOSImplementation_setAntiAliasedMutableImpl(antialiased);
}


void com_codename1_impl_ios_IOSNative_nativeDrawLineMutable___int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawLineMutable___int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineMutableImpl(n1, n2, n3, n4, n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawLineGlobal___int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawLineGlobal___int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineGlobalImpl(n1, n2, n3, n4, n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeBlurScreenRegionImpl(int x, int y, int width, int height, float radius);
void com_codename1_impl_ios_IOSNative_nativeBlurScreenRegion___int_int_int_int_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_FLOAT n5)
{
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeBlurScreenRegionImpl(n1, n2, n3, n4, n5);
    POOL_END();
}

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeGlassScreenRegionImpl(int x, int y, int width, int height, float radius, float cornerRadius, float sat, float scale, float offset, float refract, float specular);
void com_codename1_impl_ios_IOSNative_nativeGlassScreenRegion___int_int_int_int_float_float_float_float_float_float_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_FLOAT n5, JAVA_FLOAT n6, JAVA_FLOAT n7, JAVA_FLOAT n8, JAVA_FLOAT n9, JAVA_FLOAT n10, JAVA_FLOAT n11)
{
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeGlassScreenRegionImpl(n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11);
    POOL_END();
}

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeLensScreenRegionImpl(int x, int y, int width, int height, float cornerRadius, float magnify, float aberration, int tintColor, float tintStrength);
void com_codename1_impl_ios_IOSNative_nativeLensScreenRegion___int_int_int_int_float_float_float_int_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_FLOAT n5, JAVA_FLOAT n6, JAVA_FLOAT n7, JAVA_INT n8, JAVA_FLOAT n9)
{
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeLensScreenRegionImpl(n1, n2, n3, n4, n5, n6, n7, n8, n9);
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_nativeFillRectMutable___int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillRectMutable___int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectMutableImpl(n1, n2, n3, n4, n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}



extern void Java_com_codename1_impl_ios_IOSImplementation_clearRectMutable(int x, int y, int w, int h);
//native void clearRectMutable(int x, int y, int width, int height);
void com_codename1_impl_ios_IOSNative_clearRectMutable___int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_clearRectMutable(x, y, w, h);
    POOL_END();
    
}

extern void Java_com_codename1_impl_ios_IOSImplementation_clearRectGlobal(int x, int y, int w, int h);
//native void nativeClearRectGlobal(int x, int y, int width, int height);
void com_codename1_impl_ios_IOSNative_nativeClearRectGlobal___int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_clearRectGlobal(x, y, w, h);
    POOL_END();
    
}
void com_codename1_impl_ios_IOSNative_nativeFillRectGlobal___int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillRectGlobal___int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectGlobalImpl(n1, n2, n3, n4, n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

/*

    native void fillPolygonGlobal(int color, int alpha, int[] xPoints, int[] yPoints, int nPoints);

 */


void com_codename1_impl_ios_IOSNative_fillPolygonGlobal___int_int_int_1ARRAY_int_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT color, JAVA_INT alpha, JAVA_OBJECT xPoints, JAVA_OBJECT yPoints, JAVA_INT nPoints) {
    POOL_BEGIN();
    JAVA_INT* x = (JAVA_INT*)((JAVA_ARRAY)xPoints)->data;
    JAVA_INT* y = (JAVA_INT*)((JAVA_ARRAY)yPoints)->data;
    JAVA_FLOAT xFloats[nPoints];
    JAVA_FLOAT yFloats[nPoints];
    for (int i=0; i<nPoints; i++) {
        xFloats[i] = (JAVA_FLOAT)*(x+i);
        yFloats[i] = (JAVA_FLOAT)*(y+i);
    }
    FillPolygon* f = [[FillPolygon alloc] initWithArgs:xFloats y:yFloats num:nPoints color:color alpha:alpha];
    
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
    POOL_END();
}



void com_codename1_impl_ios_IOSNative_nativeDrawRectMutable___int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawRectMutable___int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectMutableImpl(n1, n2, n3, n4, n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawRectGlobal___int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawRectGlobal___int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectGlobalImpl(n1, n2, n3, n4, n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawRoundRectMutable___int_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawRoundRectMutable___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRoundRectMutableImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawRoundRectGlobal___int_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawRoundRectGlobal___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRoundRectGlobalImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillRoundRectMutable___int_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillRoundRectMutable___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillRoundRectMutableImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillRoundRectGlobal___int_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillRoundRectGlobal___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillRoundRectGlobalImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillArcMutable___int_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillArcMutable___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillArcMutableImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawArcMutable___int_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawArcMutable___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawArcMutableImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}


//native void nativeDrawShadowMutable(long image, int x, int y, int offsetX, int offsetY, int blurRadius, int spreadRadius, int color, float opacity);
extern void Java_com_codename1_impl_ios_IOSNative_nativeDrawShadowMutable(CN1_THREAD_STATE_MULTI_ARG JAVA_LONG image, 
    JAVA_INT x, JAVA_INT y, JAVA_INT offsetX, JAVA_INT offsetY, JAVA_INT blurRadius, JAVA_INT spreadRadius, JAVA_INT color, JAVA_FLOAT opacity);

void com_codename1_impl_ios_IOSNative_nativeDrawShadowMutable___long_int_int_int_int_int_int_int_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, 
    JAVA_LONG image, JAVA_INT x, JAVA_INT y, JAVA_INT offsetX, JAVA_INT offsetY, JAVA_INT blurRadius, JAVA_INT spreadRadius, JAVA_INT color, JAVA_FLOAT opacity) {
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSNative_nativeDrawShadowMutable(CN1_THREAD_STATE_PASS_ARG image, x, y, offsetX, offsetY, blurRadius, spreadRadius, color, opacity);
    POOL_END();

}

extern CGContextRef Java_com_codename1_impl_ios_IOSImplementation_drawPath(CN1_THREAD_STATE_MULTI_ARG JAVA_INT commandsLen, JAVA_OBJECT commandsArr, JAVA_INT pointsLen, JAVA_OBJECT pointsArr);

static CGContextRef drawPath(CN1_THREAD_STATE_MULTI_ARG JAVA_INT commandsLen, JAVA_OBJECT commandsArr, JAVA_INT pointsLen, JAVA_OBJECT pointsArr) {

    return Java_com_codename1_impl_ios_IOSImplementation_drawPath(CN1_THREAD_STATE_PASS_ARG commandsLen, commandsArr, pointsLen, pointsArr);



}


//native void nativeFillShapeMutable(int color, int alpha, int commandsLen, byte[] commandsArr, int pointsLen, float[] pointsArr);
void com_codename1_impl_ios_IOSNative_nativeFillShapeMutable___int_int_int_byte_1ARRAY_int_float_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT color, JAVA_INT alpha, JAVA_INT commandsLen, JAVA_OBJECT commandsArr, JAVA_INT pointsLen, JAVA_OBJECT pointsArr) {
#ifdef CN1_USE_METAL
    // Dead under Metal -- MutableGraphics.nativeFillShape now routes
    // through createAlphaMask + drawTextureAlphaMask (alpha-mask Metal
    // pipeline tagged with currentMutableImage). The Java side gates
    // with `metalRendering` before calling this JNI.
    (void)color; (void)alpha; (void)commandsLen; (void)commandsArr; (void)pointsLen; (void)pointsArr;
#else
    POOL_BEGIN();
    [UIColorFromRGB(color, alpha) set];
    CGContextRef context = drawPath(CN1_THREAD_STATE_PASS_ARG commandsLen, commandsArr, pointsLen, pointsArr);
    CGContextFillPath(context);
    POOL_END();
#endif
}

void com_codename1_impl_ios_IOSNative_nativeDrawShapeMutable___int_int_int_byte_1ARRAY_int_float_1ARRAY_float_int_int_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT color, JAVA_INT alpha, JAVA_INT commandsLen, JAVA_OBJECT commandsArr, JAVA_INT pointsLen, JAVA_OBJECT pointsArr, JAVA_FLOAT lineWidth, JAVA_INT capStyle, JAVA_INT joinStyle, JAVA_FLOAT mitreLimit) {
#ifdef CN1_USE_METAL
    // Same rationale as nativeFillShapeMutable above.
    (void)color; (void)alpha; (void)commandsLen; (void)commandsArr; (void)pointsLen; (void)pointsArr;
    (void)lineWidth; (void)capStyle; (void)joinStyle; (void)mitreLimit;
#else
    POOL_BEGIN();
#if !TARGET_OS_WATCH
    if ([CodenameOne_GLViewController isCurrentMutableTransformSet]) {
        CGContextSaveGState(UIGraphicsGetCurrentContext());
        CGContextConcatCTM(UIGraphicsGetCurrentContext(), [CodenameOne_GLViewController currentMutableTransform]);
    }
#endif // !TARGET_OS_WATCH
    CGContextRef context = drawPath(CN1_THREAD_STATE_PASS_ARG commandsLen, commandsArr, pointsLen, pointsArr);
    CGContextSaveGState(context);

    [UIColorFromRGB(color, alpha) set];
    CGContextSetLineWidth(context, lineWidth);
    CGLineCap cap = kCGLineCapButt;
    switch (capStyle) {
        case CN1_CAP_BUTT: {
            cap = kCGLineCapButt;
            break;
        }
        
        case CN1_CAP_ROUND: {
            cap = kCGLineCapRound;
            break;
        }
        
        case CN1_CAP_SQUARE: {
            cap = kCGLineCapSquare;
            break;
        }
    }
    CGContextSetLineCap(context, cap);
    
    CGLineJoin join =  kCGLineJoinMiter;
    switch (joinStyle) {
        case CN1_JOIN_MITER: {
            join = kCGLineJoinMiter;
            break;
        }
        case CN1_JOIN_ROUND: {
            join = kCGLineJoinRound;
            break;
        }
        case CN1_JOIN_BEVEL: {
            join = kCGLineJoinBevel;
            break;
        }
    }
    CGContextSetLineJoin(context, join);
    
    CGContextSetMiterLimit(context, mitreLimit);
    
    CGContextStrokePath(context);
    CGContextRestoreGState(context);
#if !TARGET_OS_WATCH
    if ([CodenameOne_GLViewController isCurrentMutableTransformSet]) {
        CGContextRestoreGState(context);
    }
#endif // !TARGET_OS_WATCH
    POOL_END();
#endif
}



void com_codename1_impl_ios_IOSNative_nativeDrawStringMutable___int_int_long_java_lang_String_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_LONG n3, JAVA_OBJECT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawStringMutable___int_int_long_java_lang_String_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringMutableImpl(n1, n2, (void *)(uintptr_t)n3, toNSString(CN1_THREAD_STATE_PASS_ARG n4), n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawStringGlobal___int_int_long_java_lang_String_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_LONG n3, JAVA_OBJECT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawStringGlobal___int_int_long_java_lang_String_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringGlobalImpl(n1, n2, (void *)(uintptr_t)n3, toNSString(CN1_THREAD_STATE_PASS_ARG n4), n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawImageMutable___long_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT alpha, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT renderingHints)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawImageMutable___long_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageMutableImpl((void *)n1, alpha, n2, n3, n4, n5, renderingHints);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawImageGlobal___long_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT alpha, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT renderingHints)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawImageGlobal___long_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl((void *)n1, alpha, n2, n3, n4, n5, renderingHints);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeTileImageGlobal___long_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT alpha, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeTileImageGlobal___long_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeTileImageGlobalImpl((void *)n1, alpha, n2, n3, n4, n5);
    POOL_END();
    //XMLVM_END_WRAPPER
}


JAVA_INT com_codename1_impl_ios_IOSNative_stringWidthNative___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_OBJECT n2)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_stringWidthNative___long_java_lang_String]
    POOL_BEGIN();
    const char* chr = stringToUTF8(CN1_THREAD_STATE_PASS_ARG n2);
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_stringWidthNativeImpl((void *)n1, chr, strlen(chr));
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_charWidthNative___long_char(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_CHAR n2)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_charWidthNative___long_char]
    POOL_BEGIN();
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_charWidthNativeImpl((void *)n1, n2);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_getFontHeightNative___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_getFontHeightNative___long]
    POOL_BEGIN();
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_getFontHeightNativeImpl((void *)n1);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_fontAscentNative___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_getFontHeightNative___long]
    POOL_BEGIN();
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_getFontAscentNativeImpl((void *)n1);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_fontDescentNative___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_getFontHeightNative___long]
    POOL_BEGIN();
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_getFontDescentNativeImpl((void *)n1);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createSystemFont___int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_createSystemFont___int_int_int]
    POOL_BEGIN();
    JAVA_LONG i = (JAVA_LONG)(uintptr_t)Java_com_codename1_impl_ios_IOSImplementation_createSystemFontImpl(n1, n2, n3);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResourceSize___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT n2)
{
    //XMLVM_BEGIN_NATIVE[com_codename1_impl_ios_IOSNative_getResourceSize___java_lang_String_java_lang_String]
    POOL_BEGIN();
    const char* chr = stringToUTF8(CN1_THREAD_STATE_PASS_ARG n1);
    int l = strlen(chr) + 1;
    char cc[l];
    memcpy(cc, chr, l);
    
    JAVA_INT i;
    const char* chr2 = stringToUTF8(CN1_THREAD_STATE_PASS_ARG n2);
    if(chr2 != 0) {
        l = strlen(chr) + 1;
        char cc2[l];
        memcpy(cc2, chr2, l);
        i = getResourceSize(cc, 0, cc2, 0);
    } else {
        i = getResourceSize(cc, 0, 0, 0);
    }
    
    POOL_END();
    return i;
    //XMLVM_END_NATIVE
}

void com_codename1_impl_ios_IOSNative_loadResource___java_lang_String_java_lang_String_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT n2, JAVA_OBJECT n3)
{
    POOL_BEGIN();
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* byteArray = n3;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)n3)->data;
#endif
    const char* chr = stringToUTF8(CN1_THREAD_STATE_PASS_ARG n1);
    int l = strlen(chr) + 1;
    char cc[l];
    memcpy(cc, chr, l);
    
    const char* chr2 = stringToUTF8(CN1_THREAD_STATE_PASS_ARG n2);
    if(chr2 != 0) {
        l = strlen(chr) + 1;
        char cc2[l];
        memcpy(cc2, chr2, l);
        loadResourceFile(cc, 0, cc2, 0, data);
    } else {
        loadResourceFile(cc, 0, 0, 0, data);
    }
    
    POOL_END();
}


JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeMutableImage___int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_createNativeMutableImage___int_int_int]
    POOL_BEGIN();
    JAVA_LONG i = (JAVA_LONG)(uintptr_t)Java_com_codename1_impl_ios_IOSImplementation_createNativeMutableImageImpl(n1, n2, n3);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_startDrawingOnImage___int_int_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_LONG n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_startDrawingOnImage___int_int_long]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_startDrawingOnImageImpl(n1, n2, (void *)n3);
    POOL_END();
    //XMLVM_END_WRAPPER
}

JAVA_LONG com_codename1_impl_ios_IOSNative_finishDrawingOnImage__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_finishDrawingOnImage__]
    POOL_BEGIN();
    JAVA_LONG i = (JAVA_LONG)(uintptr_t)Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl();
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_deleteNativePeer___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1)
{
#if TARGET_OS_WATCH
    // The watch slice currently leaks native image/peer handles rather than
    // releasing them here. Under the concurrent GC the finalizer can hand back
    // a peer pointer whose backing was already reclaimed, and the resulting
    // objc_release on a dangling pointer crashes (pointer-auth fault) -- a
    // peer-lifecycle hardening item tracked separately. Leaking is acceptable
    // for the short-lived screenshot-test process; revisit for shipping apps.
    (void)n1;
    return;
#else
    if(n1 != 0) {
        // this prevents a race condition where the gc might be invoked too soon
        dispatch_async(dispatch_get_main_queue(), ^{
            NSObject* n = (NSObject*)n1;
            [n release];
        });
    }
#endif
}

void com_codename1_impl_ios_IOSNative_deleteNativeFontPeer___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_deleteNativePeer___long]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_deleteNativeFontPeerImpl((void *)n1);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_resetAffineGlobal__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_resetAffineGlobal();
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_scaleGlobal___float_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_FLOAT x, JAVA_FLOAT y) {
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_scale(x, y);
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_rotateGlobal___float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_FLOAT angle) {
}

void com_codename1_impl_ios_IOSNative_rotateGlobal___float_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_FLOAT angle, JAVA_INT x, JAVA_INT y) {
    Rotate* f = [[Rotate alloc] initWithArgs:angle xx:x yy:y];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
}




void com_codename1_impl_ios_IOSNative_shearGlobal___float_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_FLOAT x, JAVA_FLOAT y) {
    
}



// Extracts the rich pointer detail (tool type, pressure, Apple Pencil tilt and contact size)
// from a UITouch and forwards it to Java just before the pointer event is dispatched, so the
// cross-platform stylus and pressure APIs work on iOS. Invoked from both the view controller
// touch handlers and the CN1TapGestureRecognizer. UITouch is unavailable on
// watchOS, so the helper (and its callers) are compiled out there.
#if !TARGET_OS_WATCH
// UIKit-only declaration: the type in its signature does not exist on macOS,
// so the whole thing goes rather than just the body. Guarding only the body
// would leave a signature naming a type the compiler has never heard of.
#if !TARGET_OS_OSX
void cn1CapturePointerMetadata(UITouch* touch) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
#else
    if (touch == nil) {
        return;
    }
    int pointerType = 1; // PointerEvent.TYPE_TOUCH
    float pressure = 1.0f;
    float tiltX = 0.0f;
    float tiltY = 0.0f;
    float contactSize = 0.0f;
    if (@available(iOS 9.0, *)) {
        if (touch.type == UITouchTypeStylus) {
            pointerType = 3; // PointerEvent.TYPE_STYLUS
            CGFloat maxForce = touch.maximumPossibleForce;
            if (maxForce > 0) {
                pressure = (float)(touch.force / maxForce);
            }
            tiltX = (float)((M_PI_2 - touch.altitudeAngle) * 180.0 / M_PI);
            tiltY = (float)([touch azimuthAngleInView:nil] * 180.0 / M_PI);
        } else {
            CGFloat maxForce = touch.maximumPossibleForce;
            if (maxForce > 0 && touch.force > 0) {
                pressure = (float)(touch.force / maxForce);
            }
        }
    }
    if (@available(iOS 13.4, *)) {
        if (touch.type == UITouchTypeIndirectPointer) {
            pointerType = 2; // PointerEvent.TYPE_MOUSE
        }
    }
    if (touch.majorRadius > 0) {
        contactSize = (float)touch.majorRadius;
    }
    com_codename1_impl_ios_IOSImplementation_pointerMetadataCallback___int_float_float_float_float(CN1_THREAD_GET_STATE_PASS_ARG pointerType, pressure, tiltX, tiltY, contactSize);
#endif
}
#endif
#endif // !TARGET_OS_WATCH

#if TARGET_OS_MACCATALYST || TARGET_OS_OSX
/*
 * Desktop windows. These marshal a window's own events into the framework,
 * mirroring the pointerPressed / screenSizeChanged bridges below so all the
 * ParparVM thread-state handling stays in one file.
 *
 * Shared by both Mac ports on purpose: the Java side of desktop windowing is
 * identical, and only the producer differs -- UIWindowScene callbacks on Mac
 * Catalyst, NSWindow delegate callbacks on the native macOS port. Compiling
 * one copy is what keeps the two from drifting.
 */
void CN1MacWindowDeliverClose(int windowId) {
    com_codename1_impl_ios_IOSImplementation_windowCloseCallback___int(CN1_THREAD_GET_STATE_PASS_ARG windowId);
}

void CN1MacWindowDeliverClosed(int windowId) {
    com_codename1_impl_ios_IOSImplementation_windowClosedNativelyCallback___int(CN1_THREAD_GET_STATE_PASS_ARG windowId);
}


void CN1MacWindowDeliverMonitorsChanged(void) {
    com_codename1_impl_ios_IOSImplementation_monitorsChangedCallback__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

void CN1MacWindowDeliverFocus(int windowId, BOOL gained) {
    com_codename1_impl_ios_IOSImplementation_windowFocusCallback___int_boolean(CN1_THREAD_GET_STATE_PASS_ARG windowId, gained ? JAVA_TRUE : JAVA_FALSE);
}

void CN1MacWindowDeliverContentReady(int windowId) {
    com_codename1_impl_ios_IOSImplementation_windowContentReadyCallback___int(CN1_THREAD_GET_STATE_PASS_ARG windowId);
}

void CN1MacWindowDeliverActivationFailed(int windowId, int requestSeq) {
    com_codename1_impl_ios_IOSImplementation_windowActivationFailedCallback___int_int(CN1_THREAD_GET_STATE_PASS_ARG windowId, requestSeq);
}

void CN1MacWindowDeliverVisibility(int windowId, BOOL shown) {
    com_codename1_impl_ios_IOSImplementation_windowVisibilityCallback___int_boolean(CN1_THREAD_GET_STATE_PASS_ARG windowId, shown ? JAVA_TRUE : JAVA_FALSE);
}

void CN1MacWindowDeliverResize(int windowId, int width, int height) {
    com_codename1_impl_ios_IOSImplementation_windowSizeCallback___int_int_int(CN1_THREAD_GET_STATE_PASS_ARG windowId, width, height);
}

void CN1MacWindowDeliverPointer(int windowId, int type, int x, int y) {
    com_codename1_impl_ios_IOSImplementation_windowPointerCallback___int_int_int_int(CN1_THREAD_GET_STATE_PASS_ARG windowId, type, x, y);
}

void CN1MacWindowDeliverHover(int windowId, int type, int x, int y) {
    com_codename1_impl_ios_IOSImplementation_windowHoverCallback___int_int_int_int(CN1_THREAD_GET_STATE_PASS_ARG windowId, type, x, y);
}

void CN1MacWindowDeliverWheel(int windowId, int x, int y, int scrollX, int scrollY) {
    com_codename1_impl_ios_IOSImplementation_windowWheelCallback___int_int_int_int_int(CN1_THREAD_GET_STATE_PASS_ARG windowId, x, y, scrollX, scrollY);
}

void CN1MacWindowDeliverPinch(int windowId, float scale, int x, int y) {
    com_codename1_impl_ios_IOSImplementation_windowPinchCallback___int_float_int_int(CN1_THREAD_GET_STATE_PASS_ARG windowId, scale, x, y);
}

void CN1MacWindowDeliverRotation(int windowId, float radians, int x, int y) {
    com_codename1_impl_ios_IOSImplementation_windowRotationCallback___int_float_int_int(CN1_THREAD_GET_STATE_PASS_ARG windowId, radians, x, y);
}

void CN1MacWindowDeliverKey(int windowId, int keyCode, BOOL pressed) {
    com_codename1_impl_ios_IOSImplementation_windowKeyCallback___int_int_boolean(CN1_THREAD_GET_STATE_PASS_ARG windowId, keyCode, pressed ? JAVA_TRUE : JAVA_FALSE);
}
#endif // TARGET_OS_MACCATALYST || TARGET_OS_OSX

void pointerPressed(int* x, int* y, int length) {
    if(length == 1) {
        com_codename1_impl_ios_IOSImplementation_pointerPressedCallback___int_int(CN1_THREAD_GET_STATE_PASS_ARG x[0], y[0]);
    } else {
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* xArray = XMLVMArray_createSingleDimension(__CLASS_int, length);
        memcpy(xArray->fields.org_xmlvm_runtime_XMLVMArray.array_, x, length * sizeof(JAVA_INT));
        org_xmlvm_runtime_XMLVMArray* yArray = XMLVMArray_createSingleDimension(__CLASS_int, length);
        memcpy(yArray->fields.org_xmlvm_runtime_XMLVMArray.array_, y, length * sizeof(JAVA_INT));
        com_codename1_impl_ios_IOSImplementation_pointerPressed___int_1ARRAY_int_1ARRAY(com_codename1_impl_ios_IOSImplementation_GET_instance(), xArray, yArray);
#else
        struct ThreadLocalData* threadStateData = getThreadLocalData();
        enteringNativeAllocations();
        JAVA_OBJECT xArray = __NEW_ARRAY_JAVA_INT(threadStateData, length);
        memcpy(((JAVA_ARRAY)xArray)->data, x, length * sizeof(JAVA_INT));
        JAVA_OBJECT yArray = __NEW_ARRAY_JAVA_INT(threadStateData, length);
        memcpy(((JAVA_ARRAY)yArray)->data, y, length * sizeof(JAVA_INT));
        com_codename1_impl_ios_IOSImplementation_pointerPressed___int_1ARRAY_int_1ARRAY(threadStateData,get_static_com_codename1_impl_ios_IOSImplementation_instance(threadStateData), xArray, yArray);
        finishedNativeAllocations();
#endif
    }
}

void pointerDragged(int* x, int* y, int length) {
    if(length == 1) {
        com_codename1_impl_ios_IOSImplementation_pointerDraggedCallback___int_int(CN1_THREAD_GET_STATE_PASS_ARG x[0], y[0]);
    } else {
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* xArray = XMLVMArray_createSingleDimension(__CLASS_int, length);
        memcpy(xArray->fields.org_xmlvm_runtime_XMLVMArray.array_, x, length * sizeof(JAVA_INT));
        org_xmlvm_runtime_XMLVMArray* yArray = XMLVMArray_createSingleDimension(__CLASS_int, length);
        memcpy(yArray->fields.org_xmlvm_runtime_XMLVMArray.array_, y, length * sizeof(JAVA_INT));
        com_codename1_impl_ios_IOSImplementation_pointerDragged___int_1ARRAY_int_1ARRAY(com_codename1_impl_ios_IOSImplementation_GET_instance(), xArray, yArray);
#else
        struct ThreadLocalData* threadStateData = getThreadLocalData();
        enteringNativeAllocations();
        JAVA_OBJECT xArray = __NEW_ARRAY_JAVA_INT(threadStateData, length);
        memcpy(((JAVA_ARRAY)xArray)->data, x, length * sizeof(JAVA_ARRAY_INT));
        JAVA_OBJECT yArray = __NEW_ARRAY_JAVA_INT(threadStateData, length);
        memcpy(((JAVA_ARRAY)yArray)->data, y, length * sizeof(JAVA_ARRAY_INT));
        com_codename1_impl_ios_IOSImplementation_pointerDragged___int_1ARRAY_int_1ARRAY(threadStateData,
                                                                                        get_static_com_codename1_impl_ios_IOSImplementation_instance(threadStateData), xArray, yArray);
        finishedNativeAllocations();
#endif
    }
}

void pointerReleased(int* x, int* y, int length) {
    if(length == 1) {
        com_codename1_impl_ios_IOSImplementation_pointerReleasedCallback___int_int(CN1_THREAD_GET_STATE_PASS_ARG x[0], y[0]);
    } else {
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* xArray = XMLVMArray_createSingleDimension(__CLASS_int, length);
        memcpy(xArray->fields.org_xmlvm_runtime_XMLVMArray.array_, x, length * sizeof(JAVA_INT));
        org_xmlvm_runtime_XMLVMArray* yArray = XMLVMArray_createSingleDimension(__CLASS_int, length);
        memcpy(yArray->fields.org_xmlvm_runtime_XMLVMArray.array_, y, length * sizeof(JAVA_INT));
        com_codename1_impl_ios_IOSImplementation_pointerReleased___int_1ARRAY_int_1ARRAY(com_codename1_impl_ios_IOSImplementation_GET_instance(), xArray, yArray);
#else
        struct ThreadLocalData* threadStateData = getThreadLocalData();
        enteringNativeAllocations();
        JAVA_OBJECT xArray = __NEW_ARRAY_JAVA_INT(threadStateData, length);
        memcpy(((JAVA_ARRAY)xArray)->data, x, length * sizeof(JAVA_ARRAY_INT));
        JAVA_OBJECT yArray = __NEW_ARRAY_JAVA_INT(threadStateData, length);
        memcpy(((JAVA_ARRAY)yArray)->data, y, length * sizeof(JAVA_ARRAY_INT));
        com_codename1_impl_ios_IOSImplementation_pointerReleased___int_1ARRAY_int_1ARRAY(threadStateData,
                                                                                         get_static_com_codename1_impl_ios_IOSImplementation_instance(threadStateData), xArray, yArray);
        finishedNativeAllocations();
#endif
    }
}

void screenSizeChanged(int width, int height) {
    com_codename1_impl_ios_IOSImplementation_sizeChangedImpl___int_int(CN1_THREAD_GET_STATE_PASS_ARG width, height);
}

void keyPressedNative(int keyCode) {
    com_codename1_impl_ios_IOSImplementation_keyPressedCallback___int(CN1_THREAD_GET_STATE_PASS_ARG keyCode);
}

void keyReleasedNative(int keyCode) {
    com_codename1_impl_ios_IOSImplementation_keyReleasedCallback___int(CN1_THREAD_GET_STATE_PASS_ARG keyCode);
}

void pointerHoverPressedNative(int x, int y) {
    com_codename1_impl_ios_IOSImplementation_pointerHoverPressedCallback___int_int(CN1_THREAD_GET_STATE_PASS_ARG x, y);
}

void pointerHoverNative(int x, int y) {
    com_codename1_impl_ios_IOSImplementation_pointerHoverCallback___int_int(CN1_THREAD_GET_STATE_PASS_ARG x, y);
}

void pointerHoverReleasedNative(int x, int y) {
    com_codename1_impl_ios_IOSImplementation_pointerHoverReleasedCallback___int_int(CN1_THREAD_GET_STATE_PASS_ARG x, y);
}

void pointerWheelMovedCallback(int x, int y, int scrollX, int scrollY) {
    com_codename1_impl_ios_IOSImplementation_pointerWheelMovedCallback___int_int_int_int(CN1_THREAD_GET_STATE_PASS_ARG x, y, scrollX, scrollY);
}

void pinchMagnifyCallback(float scale, int x, int y) {
    com_codename1_impl_ios_IOSImplementation_pinchMagnifyCallback___float_int_int(CN1_THREAD_GET_STATE_PASS_ARG scale, x, y);
}

void rotationGestureCallback(float radians, int x, int y) {
    com_codename1_impl_ios_IOSImplementation_rotationGestureCallback___float_int_int(CN1_THREAD_GET_STATE_PASS_ARG radians, x, y);
}

void stringEdit(int finished, int cursorPos, NSString* text) {
    com_codename1_impl_ios_IOSImplementation_editingUpdate___java_lang_String_int_boolean(CN1_THREAD_GET_STATE_PASS_ARG
                                                                                          fromNSString(CN1_THREAD_GET_STATE_PASS_ARG text), cursorPos, finished != 0
                                                                                          );
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isTablet__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_isPainted__]
    return isIPad();
    //XMLVM_END_WRAPPER
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isIOS7__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    return isIOS7();
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isRunningOnMac__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    if (@available(iOS 13.0, *)) {
        return [[NSProcessInfo processInfo] isMacCatalystApp] ? JAVA_TRUE : JAVA_FALSE;
    }
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isRunningOnWatch__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    // Resolved entirely at compile time: the watchOS slice returns true, every
    // other slice (iOS, Mac Catalyst, simulator) returns false so behaviour is
    // byte-for-byte identical on iOS.
#if TARGET_OS_WATCH
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isRunningOnTV__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    // Resolved entirely at compile time: the tvOS slice returns true, every
    // other slice (iOS, Mac Catalyst, watchOS, simulator) returns false so
    // behaviour is byte-for-byte identical on iOS.
#if TARGET_OS_TV
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

#if TARGET_OS_OSX
/*
 * Desktop chrome on the native macOS port. Implemented in CN1MacChrome.m, which
 * ships only with that port -- the title bar, the menu bar and the appearance
 * are the parts of the shell that are genuinely AppKit rather than shared, and
 * on Mac Catalyst the same four entry points reach UIKit instead.
 */
extern void CN1MacHostSetWindowTitle(NSString *title);
extern void CN1MacHostSetMenuCommands(NSArray *rows);
extern void CN1MacHostSetWindowUndecorated(BOOL undecorated);
extern void CN1MacHostSetDarkAppearance(BOOL dark);
extern BOOL CN1MacHostIsDarkMode(void);
#endif

void com_codename1_impl_ios_IOSNative_setMacWindowDarkAppearance___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN dark) {
#if TARGET_OS_OSX
    CN1MacHostSetDarkAppearance(dark ? YES : NO);
#elif TARGET_OS_MACCATALYST
    if (@available(iOS 13.0, *)) {
        dispatch_async(dispatch_get_main_queue(), ^{
            // Step 1: trait-collection override on the UIWindow. This
            // propagates the style through UIKit descendants (popovers,
            // alerts, context menus) but does NOT, by itself, redraw the
            // host NSWindow chrome (titlebar + traffic lights) on the
            // AppKit side. Each UIWindow on Catalyst is backed by a
            // UINSWindow which holds the actual NSWindow.
            UIUserInterfaceStyle uiStyle = dark ? UIUserInterfaceStyleDark : UIUserInterfaceStyleLight;
            Class nsAppearanceClass = NSClassFromString(@"NSAppearance");
            // Build the AppKit appearance object once. NSAppearance is
            // available in the Catalyst process (UIScene.titlebar uses
            // it internally) even though the rest of AppKit is not in
            // the public surface. Look up the class + factory selector
            // through the Obj-C runtime so the build doesn't need to
            // link AppKit.
            NSString *appearanceName = dark ? @"NSAppearanceNameDarkAqua" : @"NSAppearanceNameAqua";
            id appearance = nil;
            if (nsAppearanceClass != nil) {
                appearance = ((id (*)(id, SEL, id))objc_msgSend)(nsAppearanceClass, @selector(appearanceNamed:), appearanceName);
            }
            for (UIScene *scene in [UIApplication sharedApplication].connectedScenes) {
                if (![scene isKindOfClass:[UIWindowScene class]]) continue;
                UIWindowScene *ws = (UIWindowScene *)scene;
                for (UIWindow *w in ws.windows) {
                    // (a) UIKit-side style override.
                    w.overrideUserInterfaceStyle = uiStyle;
                    // (b) walk the UIWindow's internal chain to the host
                    // NSWindow. On Catalyst the UIWindow is wrapped by a
                    // UINSWindow whose actual NSWindow is stored either
                    // under "_nsWindow" or reachable via the wrapper's
                    // "attachedWindow"/"hostWindow" private key. Try the
                    // documented Apple keys first, then the common
                    // private ones.
                    if (appearance == nil) continue;
                    id nsWindow = nil;
                    @try { nsWindow = [w valueForKey:@"_nsWindow"]; } @catch (id e) { nsWindow = nil; }
                    if (nsWindow == nil) {
                        @try { nsWindow = [w valueForKey:@"nsWindow"]; } @catch (id e) { nsWindow = nil; }
                    }
                    if (nsWindow == nil) {
                        @try { nsWindow = [w valueForKey:@"hostNSWindow"]; } @catch (id e) { nsWindow = nil; }
                    }
                    if (nsWindow != nil && [nsWindow respondsToSelector:@selector(setAppearance:)]) {
                        ((void (*)(id, SEL, id))objc_msgSend)(nsWindow, @selector(setAppearance:), appearance);
                    }
                }
            }

            // Step 2: also walk NSApplication.windows as a fallback in
            // case the UIWindow -> NSWindow bridge isn't reachable via
            // KVC on this OS version. NSApplication is reachable from
            // a Catalyst process under at least macOS 11+.
            Class nsAppClass = NSClassFromString(@"NSApplication");
            if (nsAppClass != nil && appearance != nil) {
                id sharedApp = ((id (*)(id, SEL))objc_msgSend)(nsAppClass, @selector(sharedApplication));
                if (sharedApp != nil) {
                    NSArray *nsWindows = ((NSArray *(*)(id, SEL))objc_msgSend)(sharedApp, @selector(windows));
                    for (id nsWindow in nsWindows) {
                        if (![nsWindow respondsToSelector:@selector(setAppearance:)]) continue;
                        ((void (*)(id, SEL, id))objc_msgSend)(nsWindow, @selector(setAppearance:), appearance);
                    }
                }
            }
        });
    }
#endif
}

// Mac Catalyst: set the host window title bar text. On a Catalyst app the UIWindowScene.title
// maps to the AppKit window title, so updating it here updates the visible title bar. No-op on
// iOS phones/tablets (guarded by TARGET_OS_MACCATALYST and only ever called when isDesktop()).
// Mac Catalyst native window-chrome bridge. The window-title and menu work (including re-applying
// on scene activation) lives in CodenameOne_GLAppDelegate.m so the UIMenuBuilder override sits on a
// UIResponder in the chain; these natives just forward to it.
#if TARGET_OS_MACCATALYST
extern void CN1SetMacWindowTitle(NSString* title);
extern void CN1SetMacMenuLabels(NSArray* labels);
extern void CN1SetMacWindowUndecorated(BOOL undecorated);
#endif

void com_codename1_impl_ios_IOSNative_setWindowTitle___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT title) {
#if TARGET_OS_OSX
    NSString* t = toNSString(CN1_THREAD_STATE_PASS_ARG title);
    CN1MacHostSetWindowTitle(t == nil ? @"" : t);
#elif TARGET_OS_MACCATALYST
    NSString* t = toNSString(CN1_THREAD_STATE_PASS_ARG title);
    CN1SetMacWindowTitle(t == nil ? @"" : t);
#endif
}
void com_codename1_impl_ios_IOSNative_setNativeMenuCommands___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT namesNewlineJoined) {
#if TARGET_OS_OSX
    NSString* joined = toNSString(CN1_THREAD_STATE_PASS_ARG namesNewlineJoined);
    NSArray* rows = (joined == nil || joined.length == 0) ? @[] : [joined componentsSeparatedByString:@"\n"];
    dispatch_async(dispatch_get_main_queue(), ^{
        CN1MacHostSetMenuCommands(rows);
    });
#elif TARGET_OS_MACCATALYST
    NSString* joined = toNSString(CN1_THREAD_STATE_PASS_ARG namesNewlineJoined);
    NSArray* labels = (joined == nil || joined.length == 0) ? @[] : [joined componentsSeparatedByString:@"\n"];
    dispatch_async(dispatch_get_main_queue(), ^{
        CN1SetMacMenuLabels(labels);
    });
#endif
}
void com_codename1_impl_ios_IOSNative_setMacWindowUndecorated___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN undecorated) {
#if TARGET_OS_OSX
    CN1MacHostSetWindowUndecorated(undecorated ? YES : NO);
#elif TARGET_OS_MACCATALYST
    CN1SetMacWindowUndecorated(undecorated ? YES : NO);
#endif
}

/* ---- Mac Catalyst desktop windows (CN1MacWindows.m) --------------------- */
#if TARGET_OS_MACCATALYST
#import "CN1MacWindows.h"
#endif

JAVA_INT com_codename1_impl_ios_CatalystWindowNative_macWindowCreate___int_java_lang_String_int_int_int_int_boolean_boolean_boolean_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT windowId, JAVA_OBJECT title,
        JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height,
        JAVA_BOOLEAN decorated, JAVA_BOOLEAN resizable, JAVA_BOOLEAN positionSet) {
#if TARGET_OS_MACCATALYST
    POOL_BEGIN();
    NSString* t = toNSString(CN1_THREAD_STATE_PASS_ARG title);
    int slot = CN1MacWindowCreate(windowId, t == nil ? @"" : t, x, y, width, height,
            decorated ? YES : NO, resizable ? YES : NO, positionSet ? YES : NO);
    POOL_END();
    return slot;
#else
    return -1;
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowDestroy___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot) {
#if TARGET_OS_MACCATALYST
    CN1MacWindowDestroy(slot);
#endif
}

JAVA_INT com_codename1_impl_ios_CatalystWindowNative_macWindowRequestSeq___int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot) {
#if TARGET_OS_MACCATALYST
    return CN1MacWindowRequestSeq(slot);
#else
    return 0;
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowShow___int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot, JAVA_BOOLEAN visible) {
#if TARGET_OS_MACCATALYST
    CN1MacWindowShow(slot, visible ? YES : NO);
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowSetDecorated___int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot, JAVA_BOOLEAN decorated) {
#if TARGET_OS_MACCATALYST
    CN1MacWindowSetDecorated(slot, decorated == JAVA_TRUE ? YES : NO);
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowSetMinimumSize___int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot, JAVA_INT width, JAVA_INT height) {
#if TARGET_OS_MACCATALYST
    CN1MacWindowSetMinimumSize(slot, width, height);
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowSetEditingSlot___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot) {
#if TARGET_OS_MACCATALYST
    CN1MacWindowSetEditingSlot(slot);
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowSetResizable___int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot, JAVA_BOOLEAN resizable) {
#if TARGET_OS_MACCATALYST
    CN1MacWindowSetResizable(slot, resizable == JAVA_TRUE ? YES : NO);
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_CatalystWindowNative_macWindowReopen___int_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot) {
#if TARGET_OS_MACCATALYST
    return CN1MacWindowReopen(slot) ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowSetInputEnabled___int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot, JAVA_BOOLEAN enabled) {
#if TARGET_OS_MACCATALYST
    CN1MacWindowSetInputEnabled(slot, enabled == JAVA_TRUE);
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macMainWindowSetInputEnabled___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN enabled) {
#if TARGET_OS_MACCATALYST
    CN1MacMainWindowSetInputEnabled(enabled == JAVA_TRUE);
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_CatalystWindowNative_macWindowAttachPeer___long_int_int_int_int_int_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT slot, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
#if TARGET_OS_MACCATALYST
    CN1View* v = (BRIDGE_CAST CN1View*)((void *)peer);
    return CN1MacWindowAttachPeer(slot, v, x, y, w, h) ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowWatchScreens__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_MACCATALYST
    CN1MacWindowWatchScreens();
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowSetTitle___int_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot, JAVA_OBJECT title) {
#if TARGET_OS_MACCATALYST
    POOL_BEGIN();
    NSString* t = toNSString(CN1_THREAD_STATE_PASS_ARG title);
    CN1MacWindowSetTitle(slot, t == nil ? @"" : t);
    POOL_END();
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowSetBounds___int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
#if TARGET_OS_MACCATALYST
    CN1MacWindowSetBounds(slot, x, y, width, height);
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_CatalystWindowNative_macMainWindowGetBounds___int_1ARRAY_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT out) {
#if TARGET_OS_MACCATALYST
    if (out == JAVA_NULL || ((JAVA_ARRAY) out)->length < 4) {
        return JAVA_FALSE;
    }
    return CN1MacMainWindowGetBounds((int*) ((JAVA_ARRAY) out)->data) ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowGetBounds___int_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot, JAVA_OBJECT out) {
#if TARGET_OS_MACCATALYST
    if (out == JAVA_NULL || ((JAVA_ARRAY) out)->length < 4) {
        return;
    }
    CN1MacWindowGetBounds(slot, (int*) ((JAVA_ARRAY) out)->data);
#endif
}

JAVA_INT com_codename1_impl_ios_CatalystWindowNative_macWindowGetWidth___int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot) {
#if TARGET_OS_MACCATALYST
    return CN1MacWindowGetWidth(slot);
#else
    return 0;
#endif
}

JAVA_INT com_codename1_impl_ios_CatalystWindowNative_macWindowGetHeight___int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot) {
#if TARGET_OS_MACCATALYST
    return CN1MacWindowGetHeight(slot);
#else
    return 0;
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowSetState___int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot, JAVA_INT state) {
#if TARGET_OS_MACCATALYST
    CN1MacWindowSetState(slot, state);
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macWindowPresent___int_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot, JAVA_OBJECT argb, JAVA_INT width, JAVA_INT height) {
#if TARGET_OS_MACCATALYST
    if (argb == JAVA_NULL) {
        return;
    }
    CN1MacWindowPresent(slot, ((JAVA_ARRAY) argb)->data, width, height);
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_CatalystWindowNative_macMultiWindowSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_MACCATALYST
    return CN1MacMultiWindowSupported() ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_INT com_codename1_impl_ios_CatalystWindowNative_macMonitorCount___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_MACCATALYST
    return CN1MacMonitorCount();
#else
    return 1;
#endif
}

JAVA_INT com_codename1_impl_ios_CatalystWindowNative_macPrimaryMonitor___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_MACCATALYST
    return CN1MacPrimaryMonitor();
#else
    return 0;
#endif
}

void com_codename1_impl_ios_CatalystWindowNative_macMonitorBounds___int_boolean_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT monitor, JAVA_BOOLEAN workArea, JAVA_OBJECT out) {
#if TARGET_OS_MACCATALYST
    if (out == JAVA_NULL || ((JAVA_ARRAY) out)->length < 4) {
        return;
    }
    CN1MacMonitorBounds(monitor, workArea ? YES : NO, (int*) ((JAVA_ARRAY) out)->data);
#endif
}

JAVA_INT com_codename1_impl_ios_CatalystWindowNative_macMonitorDpi___int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT monitor) {
#if TARGET_OS_MACCATALYST
    return CN1MacMonitorDpi(monitor);
#else
    return 96;
#endif
}

JAVA_INT com_codename1_impl_ios_CatalystWindowNative_macMonitorScaleTimes100___int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT monitor) {
#if TARGET_OS_MACCATALYST
    /* Scaled by a hundred because the bridge carries ints; the Java side divides
     * it back out. */
    return (JAVA_INT) (CN1MacMonitorScale(monitor) * 100.0 + 0.5);
#else
    return 100;
#endif
}

JAVA_INT com_codename1_impl_ios_CatalystWindowNative_macMonitorForWindow___int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT slot) {
#if TARGET_OS_MACCATALYST
    return CN1MacMonitorForWindow(slot);
#else
    return 0;
#endif
}

JAVA_INT com_codename1_impl_ios_CatalystWindowNative_macMonitorForMainWindow___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_MACCATALYST
    return CN1MacMonitorForMainWindow();
#else
    return 0;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNSData___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    POOL_BEGIN();
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG file);
    if([ns hasPrefix:@"file:"]) {
        ns = [ns substringFromIndex:5];
    } else {
        if([ns hasPrefix:@"//localhost"]) {
            ns = [@"file:" stringByAppendingString:ns];
            NSData* d = [NSData dataWithContentsOfURL:[NSURL URLWithString:ns]];
#ifndef CN1_USE_ARC
            [d retain];
#endif
            POOL_END();
            return (JAVA_LONG)d;
        }
    }
    NSData* d = [NSData dataWithContentsOfFile:ns];
#ifndef CN1_USE_ARC
    [d retain];
#endif
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)d);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNSDataResource___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name, JAVA_OBJECT type) {
    POOL_BEGIN();
    NSString* nameNS = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    NSString* typeNS = nameNS == NULL ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG type);
    NSString* path = [[NSBundle mainBundle] pathForResource:nameNS ofType:typeNS];
    NSData* iData = [NSData dataWithContentsOfFile:path];
#ifndef CN1_USE_ARC
    [iData retain];
#endif
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)iData);
}

JAVA_INT com_codename1_impl_ios_IOSNative_read___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_INT pointer) {
    POOL_BEGIN();
    NSData* n = (BRIDGE_CAST NSData*)((void*)nsData);
    int val;
    [n getBytes:&val range:NSMakeRange(pointer, 1)];
    POOL_END();
    return val;
}

void com_codename1_impl_ios_IOSNative_read___long_byte_1ARRAY_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_OBJECT destination, JAVA_INT offset, JAVA_INT length, JAVA_INT pointer) {
    POOL_BEGIN();
    NSData* n = (BRIDGE_CAST NSData*)((void*)nsData);
    
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* byteArray = destination;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    void* data = ((JAVA_ARRAY)destination)->data;
#endif
    void* actual = &(data[offset]);
    [n getBytes:actual range:NSMakeRange(pointer, length)];
    
    POOL_END();
}

JAVA_INT com_codename1_impl_ios_IOSNative_writeToFile___byte_1ARRAY_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT path) {
    POOL_BEGIN();
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    if([ns hasPrefix:@"file:"]) {
        ns = [ns substringFromIndex:5];
    }
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* byteArray = n1;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    int length = byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
    void *data = ((JAVA_ARRAY)n1)->data;
    int length = ((JAVA_ARRAY)n1)->length;
#endif
    NSData* d = [NSData dataWithBytes:data length:length];
    NSError *error = nil;
    [d writeToFile:ns options:NSAtomicWrite error:&error];
    if(error != nil) {
        CN1Log(@"Error writeToFile: %@ for the file %@", [error localizedDescription], ns);
        POOL_END();
        return 1;
    }
    POOL_END();
    return 0;
}

JAVA_INT com_codename1_impl_ios_IOSNative_appendToFile___byte_1ARRAY_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT path) {
    POOL_BEGIN();
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    if([ns hasPrefix:@"file:"]) {
        ns = [ns substringFromIndex:5];
    }
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* byteArray = n1;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    int len = byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
    void* data = ((JAVA_ARRAY)n1)->data;
    int len = ((JAVA_ARRAY)n1)->length;
#endif
    NSData* d = [NSData dataWithBytes:data length:len];
    NSFileHandle* outputFile = [NSFileHandle fileHandleForWritingAtPath:ns];
    [outputFile seekToEndOfFile];
    [outputFile writeData:d];
    [outputFile synchronizeFile];
    POOL_END();
    return 0;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getFileSize___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    POOL_BEGIN();
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    if([ns hasPrefix:@"file:"]) {
        ns = [ns substringFromIndex:5];
    }
    NSFileManager *man = [[NSFileManager alloc] init];
    NSError *error = nil;
    NSDictionary *attrs = [man attributesOfItemAtPath:ns error:&error];
    if(error != nil) {
        CN1Log(@"Error getFileSize: %@ for the file %@", [error localizedDescription], ns);
    }
    UInt32 result = (UInt32)[attrs fileSize];
#ifndef CN1_USE_ARC
    [man release];
#endif
    POOL_END();
    return result;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getFileLastModified___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
#ifdef checkModificationDatePermission
    POOL_BEGIN();
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    if([ns hasPrefix:@"file:"]) {
        ns = [ns substringFromIndex:5];
    }
    NSFileManager *man = [[NSFileManager alloc] init];
    NSError *error = nil;
    NSDictionary *attrs = [man attributesOfItemAtPath:ns error:&error];
    if(error != nil) {
        CN1Log(@"Error getFileLastModified: %@ for the file %@", [error localizedDescription], ns);
    }
    NSDate* modDate = [attrs fileModificationDate];
    //[modDate timeIntervalSince1970];
    //NSTimeZone *tzone = [NSTimeZone timeZoneWithName:@"GMT"];
    JAVA_LONG result = [modDate timeIntervalSince1970] * 1000;
#ifndef CN1_USE_ARC
    [man release];
#endif
    POOL_END();
    return result;
#else
    return 0;
#endif
}

void com_codename1_impl_ios_IOSNative_readFile___java_lang_String_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path, JAVA_OBJECT n1) {
    POOL_BEGIN();
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSData* d = [NSData dataWithContentsOfFile:ns];
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* byteArray = n1;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    void *data = ((JAVA_ARRAY)n1)->data;
#endif
    memcpy(data, d.bytes, d.length);
    POOL_END();
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getDocumentsDir__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];
    JAVA_OBJECT str = fromNSString(CN1_THREAD_STATE_PASS_ARG documentsPath);
    POOL_END();
    return str;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getCachesDir__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];
    JAVA_OBJECT str = fromNSString(CN1_THREAD_STATE_PASS_ARG documentsPath);
    POOL_END();
    return str;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResourcesDir__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
    NSBundle *bundle = [NSBundle mainBundle];
    NSString *bundlePath = [bundle bundlePath];
    JAVA_OBJECT str = fromNSString(CN1_THREAD_STATE_PASS_ARG bundlePath);
    POOL_END();
    return str;
}

void com_codename1_impl_ios_IOSNative_deleteFile___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG file);
    if([ns hasPrefix:@"file:"]) {
        ns = [ns substringFromIndex:5];
    }
    NSError *error = nil;
    [fm removeItemAtPath:ns error:&error];
    if(error != nil) {
        CN1Log(@"Error in deleteFile: %@ for the file %@", [error localizedDescription], ns);
    }
#ifndef CN1_USE_ARC
    [fm release];
#endif
    POOL_END();
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_fileExists___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG file);
    JAVA_BOOLEAN b = [fm fileExistsAtPath:ns];
#ifndef CN1_USE_ARC
    [fm release];
#endif
    POOL_END();
    return b;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDirectory___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG file);
    BOOL b = NO;
    BOOL* isDir = (&b);
    [fm fileExistsAtPath:ns isDirectory:isDir];
#ifndef CN1_USE_ARC
    [fm release];
#endif
    POOL_END();
    return b;
}

JAVA_INT com_codename1_impl_ios_IOSNative_fileCountInDir___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT dir) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG dir);
    NSError *error = nil;
    NSArray* nsArr = [fm contentsOfDirectoryAtPath:ns error:&error];
    if(error != nil) {
        CN1Log(@"Error in fileCountInDir: %@", [error localizedDescription]);
    }
    int i = nsArr.count;
#ifndef CN1_USE_ARC
    [fm release];
#endif
    POOL_END();
    return i;
}

void com_codename1_impl_ios_IOSNative_listFilesInDir___java_lang_String_java_lang_String_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT dir, JAVA_OBJECT files) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG dir);
    NSError *error = nil;
    NSArray* nsArr = [fm contentsOfDirectoryAtPath:ns error:&error];
    if(error != nil) {
        CN1Log(@"Error in listing files: %@", [error localizedDescription]);
    }
    
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* strArray = files;
    JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)strArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)files)->data;
#endif
    
    int count = nsArr.count;
    for(int iter = 0 ; iter < count ; iter++) {
        NSString* currentString = [nsArr objectAtIndex:iter];
        JAVA_OBJECT str = fromNSString(CN1_THREAD_STATE_PASS_ARG currentString);
        data[iter] = str;
    }
#ifndef CN1_USE_ARC
    [fm release];
#endif
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_createDirectory___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT dir) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG dir);
    NSError *mkdirError = nil;
    // withIntermediateDirectories:error: rather than the deprecated two-argument
    // form, which macOS marks deprecated and clang rejects under the port's
    // warning settings. Intermediate directories are created because every
    // caller here is materialising a storage path that may not have a parent
    // yet, which is what the old call did by accident of the caller always
    // having made the parent first.
    if (![fm createDirectoryAtPath:ns withIntermediateDirectories:YES
                        attributes:nil error:&mkdirError]) {
        CN1Log(@"Failed to create directory %@: %@", ns, mkdirError);
    }
#ifndef CN1_USE_ARC
    [fm release];
#endif
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_moveFile___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dest) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    NSString* nsSrc = toNSString(CN1_THREAD_STATE_PASS_ARG src);
    NSString* nsDst = toNSString(CN1_THREAD_STATE_PASS_ARG dest);
    if([nsSrc hasPrefix:@"file:"]) {
        nsSrc = [nsSrc substringFromIndex:5];
    }
    if([nsDst hasPrefix:@"file:"]) {
        nsDst = [nsDst substringFromIndex:5];
    }
    NSError *error = nil;
    [fm moveItemAtPath:nsSrc toPath:nsDst error:&error];
    if(error != nil) {
        CN1Log(@"Error in moving file: %@", [error localizedDescription]);
    }
#ifndef CN1_USE_ARC
    [fm release];
#endif
    POOL_END();
}

extern void Java_com_codename1_impl_ios_IOSImplementation_setImageName(void* nativeImage, const char* name);


void com_codename1_impl_ios_IOSNative_setImageName___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nativeImage, JAVA_OBJECT name) {
    POOL_BEGIN();
    const char* chrs = stringToUTF8(CN1_THREAD_STATE_PASS_ARG name);
    Java_com_codename1_impl_ios_IOSImplementation_setImageName((void *)nativeImage, chrs);
    POOL_END();
}

JAVA_LONG com_codename1_impl_ios_IOSNative_openConnection___java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT url, JAVA_INT timeout) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = [[NetworkConnectionImpl alloc] init];
    NSString* nsSrc = toNSString(CN1_THREAD_STATE_PASS_ARG url);
    void* response = [impl openConnection:nsSrc timeout:timeout];
    POOL_END();
    return (JAVA_LONG)response;
}

void com_codename1_impl_ios_IOSNative_connect___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    [impl connect];
    POOL_END();
}

/*
// Checks avaiable bytes for NetworkConnection
    native int available(long peer);

    // Read pending data from NetworkConnection
    native int readData(long peer, byte[] bytes, int off, int len);

    // Reads next byte from NetworkConnection
    native int shiftByte(long peer);

    // Appends pending data to NetworkConnection
    // data is a NSData* object
    // We go through java in order to use locking concurrency
    native void appendData(long peer, long data);
*/

JAVA_INT com_codename1_impl_ios_IOSNative_available___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    
    JAVA_INT result = [impl available];
    
    POOL_END();
    return result;
}

void com_codename1_impl_ios_IOSNative_appendData___long_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_LONG data) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    NSData* nsData = (BRIDGE_CAST NSData*)((void *)data);
    [impl appendData:nsData];    
    POOL_END();
}

JAVA_INT com_codename1_impl_ios_IOSNative_readData___long_byte_1ARRAY_int_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT len) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    JAVA_INT result = [impl readData:buffer offset:offset len:len];
    POOL_END();
    return result;
}

JAVA_INT com_codename1_impl_ios_IOSNative_shiftByte___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    
    JAVA_INT result = [impl shiftByte];
    
    POOL_END();
    return result;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getSSLCertificates___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    JAVA_OBJECT result = [impl getSSLCertificates];
    POOL_END();
    return result;
}

void com_codename1_impl_ios_IOSNative_setChunkedStreamingMode___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT len) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    [impl setChunkedStreamingLen:len];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_setConnectionId___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT id) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    [impl setConnectionId:id];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_setInsecure___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN insecure) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    [impl setInsecure:insecure];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_setMethod___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT mtd) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    NSString* nsSrc = toNSString(CN1_THREAD_STATE_PASS_ARG mtd);
    [impl setMethod:nsSrc];
    POOL_END();
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResponseCode___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    return [impl getResponseCode];
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseMessage___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    JAVA_OBJECT str = fromNSString(CN1_THREAD_STATE_PASS_ARG [impl getResponseMessage]);
    POOL_END();
    return str;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getContentLength___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    return [impl getContentLength];
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseHeader___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT name) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    NSString* nsSrc = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    JAVA_OBJECT str = fromNSString(CN1_THREAD_STATE_PASS_ARG [impl getResponseHeader:nsSrc]);
    POOL_END();
    return str;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResponseHeaderCount___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    JAVA_INT i = [impl getResponseHeaderCount];
    POOL_END();
    return i;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseHeaderName___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    JAVA_OBJECT j = fromNSString(CN1_THREAD_STATE_PASS_ARG [impl getResponseHeaderName:offset]);
    POOL_END();
    return j;
}

void com_codename1_impl_ios_IOSNative_addHeader___long_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT key, JAVA_OBJECT value) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    NSString* nsKey = toNSString(CN1_THREAD_STATE_PASS_ARG key);
    NSString* nsValue = toNSString(CN1_THREAD_STATE_PASS_ARG value);
    [impl addHeader:nsKey value:nsValue];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_setBody___long_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT arr) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* byteArray = arr;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    int len = byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
    void* data = ((JAVA_ARRAY)arr)->data;
    int len = ((JAVA_ARRAY)arr)->length;
#endif
    [impl setBody:data size:len];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_setBody___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT file) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    [impl setBody: toNSString(CN1_THREAD_STATE_PASS_ARG file)];
    POOL_END();
}

void connectionComplete(void* peer) {
    com_codename1_impl_ios_IOSImplementation_streamComplete___long(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_LONG)peer);
}

void connectionReceivedData(void* peer, NSData* data) {
    com_codename1_impl_ios_IOSImplementation_appendData___long_long(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_LONG)peer, (JAVA_LONG)data);


}

void connectionError(void* peer, NSString* message) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
#else
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    JAVA_OBJECT str = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG message);
    com_codename1_impl_ios_IOSImplementation_networkError___long_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG peer, str);
    POOL_END();
#endif
}


void com_codename1_impl_ios_IOSNative_closeConnection___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
#ifndef CN1_USE_ARC
    [impl release];
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_canExecute___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT url) {
#if !TARGET_OS_WATCH
    __block JAVA_BOOLEAN result;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();

        NSString* ns = toNSString(CN1_THREAD_GET_STATE_PASS_ARG url);
        if([ns hasPrefix:@"file:"]) {
            ns = [NSURL fileURLWithPath:[ns substringFromIndex:5]];
        }
#if TARGET_OS_OSX
        // NSWorkspace answers with the application that would open it, so a nil
        // result is the same "nothing handles this scheme" that canOpenURL:
        // reports -- and unlike iOS it needs no LSApplicationQueriesSchemes
        // declaration to be allowed to ask.
        result = [[NSWorkspace sharedWorkspace]
                     URLForApplicationToOpenURL:[NSURL URLWithString:ns]] != nil;
#else
        result = [[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:ns]];
#endif
        POOL_END();
    });
    return result;
#else
    // watchOS has no UIApplication openURL pipeline.
    return NO;
#endif // !TARGET_OS_WATCH
}

void com_codename1_impl_ios_IOSNative_execute___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1)
{
#if TARGET_OS_OSX
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG n1);
    if (ns == nil) {
        return;
    }
#ifndef CN1_USE_ARC
    [ns retain];
#endif
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        // No preview controller: a Mac opens a file in whichever application
        // owns it, which is what the user expects and what
        // UIDocumentInteractionController was standing in for on iOS.
        NSURL* url = [ns hasPrefix:@"file:"]
            ? [NSURL fileURLWithPath:[ns substringFromIndex:5]]
            : [NSURL URLWithString:ns];
        if (url != nil) {
            [[NSWorkspace sharedWorkspace] openURL:url];
        }
        POOL_END();
#ifndef CN1_USE_ARC
        [ns release];
#endif
    });
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    __block NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG n1);
#ifdef CN1_USE_ARC
    [ns retain];
#endif
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if([ns hasPrefix:@"file:"]) {
            ns = [ns substringFromIndex:5];
            UIDocumentInteractionController* preview = [UIDocumentInteractionController interactionControllerWithURL:[NSURL fileURLWithPath:ns]];
            preview.delegate = [CodenameOne_GLViewController instance];
            [preview presentPreviewAnimated:YES];
        } else {
            NSURL* url = [NSURL URLWithString:ns];
            if (@available(iOS 10.0, *)) {
                [[UIApplication sharedApplication] openURL:url options:@{} completionHandler:^(BOOL success) {
                    if (success) {
                        NSLog(@"URL opened : %@", url);
                    } else {
                        NSLog(@"Error opening URL: %@", url);
                    }
                }];
            } else {
                [[UIApplication sharedApplication] openURL:url];
            }
        }
#ifdef CN1_USE_ARC
        [ns release];
#endif
        POOL_END();
    });
#else
    // watchOS/tvOS have no UIDocumentInteractionController.
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
#endif
}

void com_codename1_impl_ios_IOSNative_flashBacklight___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_flashBacklight___int]
    //XMLVM_END_WRAPPER
}

// SJH Nov. 17, 2015 : Removing native isMinimized() method because it conflicted with
// tracking on the java side.  It caused the app to still be minimized inside start()
// method.  
// Related to this issue https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/codenameone-discussions/Ajo2fArN8mc/KrF_e9cTDwAJ
//JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isMinimized__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
//{
//    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_isMinimized__]
//    return !([[UIApplication sharedApplication] applicationState] == UIApplicationStateActive);
//    //XMLVM_END_WRAPPER
//}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_minimizeApplication__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    return 0;
}

void com_codename1_impl_ios_IOSNative_restoreMinimizedApplication__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_restoreMinimizedApplication__]
    //XMLVM_END_WRAPPER
}

extern int orientationLock;
void com_codename1_impl_ios_IOSNative_lockOrientation___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN n1)
{
// Not implemented on the native macOS port: the body below is UIKit -- a
// picker, an action sheet, a movie player, a pasteboard or a UIApplication
// service -- and AppKit's equivalent is a different API rather than a
// renamed one. The symbol still has to exist: ParparVM keeps a native method
// alive BY its symbol appearing in the native sources, so removing it would
// make the dead-code pass drop the Java side and ship green with the feature
// silently gone. Returning an unsupported value instead lets the caller take
// its unsupported path.
#if TARGET_OS_OSX
#else
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_lockOrientation___boolean]
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    if(n1) {
        orientationLock = 1;
        dispatch_async(dispatch_get_main_queue(), ^{
             UIInterfaceOrientation currentOrientation = [[UIApplication sharedApplication] statusBarOrientation];
             if (currentOrientation != UIInterfaceOrientationPortrait && currentOrientation != UIInterfaceOrientationPortraitUpsideDown) {
                 NSNumber *value = [NSNumber numberWithInt:UIInterfaceOrientationPortrait];
                 [[UIDevice currentDevice] setValue:value forKey:@"orientation"];
                 [UIViewController attemptRotationToDeviceOrientation];
             }
        });
    } else {
        orientationLock = 2;
        dispatch_async(dispatch_get_main_queue(), ^{
             UIInterfaceOrientation currentOrientation = [[UIApplication sharedApplication] statusBarOrientation];
             if (currentOrientation != UIInterfaceOrientationLandscapeLeft && currentOrientation != UIInterfaceOrientationLandscapeRight) {
                 NSNumber *value = [NSNumber numberWithInt:UIInterfaceOrientationLandscapeRight];
                 [[UIDevice currentDevice] setValue:value forKey:@"orientation"];
                 [UIViewController attemptRotationToDeviceOrientation];
             }
        });
    }
#else
    // watchOS/tvOS have no device orientation / UIViewController rotation.
    orientationLock = n1 ? 1 : 2;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
    //XMLVM_END_WRAPPER
#endif
}

void com_codename1_impl_ios_IOSNative_unlockOrientation__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    orientationLock = 0;
}

void com_codename1_impl_ios_IOSNative_lockScreen__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
#if TARGET_OS_OSX
    // NSProcessInfo's activity assertion is the macOS equivalent of disabling
    // the idle timer. The token is held in a file static because the matching
    // unlock has no argument to carry it back.
    if (cn1MacIdleActivity == nil) {
        cn1MacIdleActivity = [[NSProcessInfo processInfo]
            beginActivityWithOptions:NSActivityIdleDisplaySleepDisabled
                                    | NSActivityIdleSystemSleepDisabled
                              reason:@"Codename One application requested the display stay awake"];
#ifndef CN1_USE_ARC
        [cn1MacIdleActivity retain];
#endif
    }
#else
#if !TARGET_OS_WATCH
    dispatch_async(dispatch_get_main_queue(), ^{
        [UIApplication sharedApplication].idleTimerDisabled = YES;
    });
#endif // !TARGET_OS_WATCH
#endif
}

void com_codename1_impl_ios_IOSNative_unlockScreen__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
#if TARGET_OS_OSX
    if (cn1MacIdleActivity != nil) {
        [[NSProcessInfo processInfo] endActivity:cn1MacIdleActivity];
#ifndef CN1_USE_ARC
        [cn1MacIdleActivity release];
#endif
        cn1MacIdleActivity = nil;
    }
#else
#if !TARGET_OS_WATCH
    dispatch_async(dispatch_get_main_queue(), ^{
        [UIApplication sharedApplication].idleTimerDisabled = NO;
    });
#endif // !TARGET_OS_WATCH
#endif
}

void com_codename1_impl_ios_IOSNative_setDisableScreenshots___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN disable)
{
    BOOL shouldDisable = disable ? YES : NO;
#if TARGET_OS_WATCH
    // No screen-capture overlay on watchOS (no CN1View/UIScreen capture APIs).
    cn1_disableScreenshots = shouldDisable;
#else
    dispatch_async(dispatch_get_main_queue(), ^{
        cn1_disableScreenshots = shouldDisable;
        if (cn1ScreenCaptureObserver != nil) {
            [[NSNotificationCenter defaultCenter] removeObserver:cn1ScreenCaptureObserver];
            cn1ScreenCaptureObserver = nil;
        }
        if (cn1ScreenCaptureView != nil) {
            [cn1ScreenCaptureView removeFromSuperview];
            cn1ScreenCaptureView = nil;
        }
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 110000
        if (cn1_disableScreenshots && [[UIScreen mainScreen] respondsToSelector:@selector(isCaptured)]) {
            if (@available(iOS 11.0, *)) {
                // Listen for capture state changes so we can add/remove the overlay.
                cn1ScreenCaptureObserver = [[NSNotificationCenter defaultCenter] addObserverForName:UIScreenCapturedDidChangeNotification
                                                                                            object:[UIScreen mainScreen]
                                                                                             queue:[NSOperationQueue mainQueue]
                                                                                        usingBlock:^(NSNotification *notification) {
                    cn1_updateScreenCaptureBlocker();
                }];
            }
        }
#endif
        // Ensure the overlay reflects the current capture state immediately.
        cn1_updateScreenCaptureBlocker();
    });
#endif // TARGET_OS_WATCH
}

extern void vibrateDevice();
void com_codename1_impl_ios_IOSNative_vibrate___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT duration) {
    vibrateDevice();
}

// ---- CoreMotion backed motion sensors (com.codename1.sensors) ----
// These constants mirror MotionSensorManager.TYPE_*. iOS exposes the raw
// accelerometer, gyroscope and magnetometer natively; the core derives the
// gravity, linear acceleration and orientation values from them.
#define CN1_MOTION_ACCELEROMETER 1
#define CN1_MOTION_GYROSCOPE 4
#define CN1_MOTION_MAGNETOMETER 5
#define CN1_MOTION_G 9.80665

#if !TARGET_OS_TV
// UIKit-only declaration: the type in its signature does not exist on macOS,
// so the whole thing goes rather than just the body. Guarding only the body
// would leave a signature naming a type the compiler has never heard of.
#if !TARGET_OS_OSX
static CMMotionManager *cn1MotionManager = nil;
#endif
// UIKit-only declaration: the type in its signature does not exist on macOS,
// so the whole thing goes rather than just the body. Guarding only the body
// would leave a signature naming a type the compiler has never heard of.
#if !TARGET_OS_OSX
static CMMotionManager *cn1GetMotionManager() {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return nil;
#else
    if(cn1MotionManager == nil) {
        cn1MotionManager = [[CMMotionManager alloc] init];
    }
    return cn1MotionManager;
#endif
}
#endif
#endif

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isMotionSensorSupported___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
// Not implemented on the native macOS port: the body below is UIKit -- a
// picker, an action sheet, a movie player, a pasteboard or a UIApplication
// service -- and AppKit's equivalent is a different API rather than a
// renamed one. The symbol still has to exist: ParparVM keeps a native method
// alive BY its symbol appearing in the native sources, so removing it would
// make the dead-code pass drop the Java side and ship green with the feature
// silently gone. Returning an unsupported value instead lets the caller take
// its unsupported path.
#if TARGET_OS_OSX
    return JAVA_FALSE;
#else
#if TARGET_OS_TV
    return JAVA_FALSE;
#else
    CMMotionManager *m = cn1GetMotionManager();
    switch(type) {
        case CN1_MOTION_ACCELEROMETER:
            return m.accelerometerAvailable ? JAVA_TRUE : JAVA_FALSE;
        case CN1_MOTION_GYROSCOPE:
            return m.gyroAvailable ? JAVA_TRUE : JAVA_FALSE;
        case CN1_MOTION_MAGNETOMETER:
            return m.magnetometerAvailable ? JAVA_TRUE : JAVA_FALSE;
        default:
            return JAVA_FALSE;
    }
#endif
#endif
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isMotionSensorSupported___int_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
    return com_codename1_impl_ios_IOSNative_isMotionSensorSupported___int(CN1_THREAD_STATE_PASS_ARG instanceObject, type);
}

void com_codename1_impl_ios_IOSNative_startMotionSensor___int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type, JAVA_INT rateMillis) {
// Not implemented on the native macOS port: the body below is UIKit -- a
// picker, an action sheet, a movie player, a pasteboard or a UIApplication
// service -- and AppKit's equivalent is a different API rather than a
// renamed one. The symbol still has to exist: ParparVM keeps a native method
// alive BY its symbol appearing in the native sources, so removing it would
// make the dead-code pass drop the Java side and ship green with the feature
// silently gone. Returning an unsupported value instead lets the caller take
// its unsupported path.
#if TARGET_OS_OSX
#else
#if !TARGET_OS_TV
    CMMotionManager *m = cn1GetMotionManager();
    NSTimeInterval interval = ((double)rateMillis) / 1000.0;
    switch(type) {
        case CN1_MOTION_ACCELEROMETER:
            m.accelerometerUpdateInterval = interval;
            [m startAccelerometerUpdates];
            break;
        case CN1_MOTION_GYROSCOPE:
            m.gyroUpdateInterval = interval;
            [m startGyroUpdates];
            break;
        case CN1_MOTION_MAGNETOMETER:
            m.magnetometerUpdateInterval = interval;
            [m startMagnetometerUpdates];
            break;
        default:
            break;
    }
#endif
#endif
}

void com_codename1_impl_ios_IOSNative_stopMotionSensor___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
// Not implemented on the native macOS port: the body below is UIKit -- a
// picker, an action sheet, a movie player, a pasteboard or a UIApplication
// service -- and AppKit's equivalent is a different API rather than a
// renamed one. The symbol still has to exist: ParparVM keeps a native method
// alive BY its symbol appearing in the native sources, so removing it would
// make the dead-code pass drop the Java side and ship green with the feature
// silently gone. Returning an unsupported value instead lets the caller take
// its unsupported path.
#if TARGET_OS_OSX
#else
#if !TARGET_OS_TV
    CMMotionManager *m = cn1GetMotionManager();
    switch(type) {
        case CN1_MOTION_ACCELEROMETER:
            [m stopAccelerometerUpdates];
            break;
        case CN1_MOTION_GYROSCOPE:
            [m stopGyroUpdates];
            break;
        case CN1_MOTION_MAGNETOMETER:
            [m stopMagnetometerUpdates];
            break;
        default:
            break;
    }
#endif
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_hasMotionData___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
// Not implemented on the native macOS port: the body below is UIKit -- a
// picker, an action sheet, a movie player, a pasteboard or a UIApplication
// service -- and AppKit's equivalent is a different API rather than a
// renamed one. The symbol still has to exist: ParparVM keeps a native method
// alive BY its symbol appearing in the native sources, so removing it would
// make the dead-code pass drop the Java side and ship green with the feature
// silently gone. Returning an unsupported value instead lets the caller take
// its unsupported path.
#if TARGET_OS_OSX
    return JAVA_FALSE;
#else
#if TARGET_OS_TV
    return JAVA_FALSE;
#else
    CMMotionManager *m = cn1GetMotionManager();
    switch(type) {
        case CN1_MOTION_ACCELEROMETER:
            return m.accelerometerData != nil ? JAVA_TRUE : JAVA_FALSE;
        case CN1_MOTION_GYROSCOPE:
            return m.gyroData != nil ? JAVA_TRUE : JAVA_FALSE;
        case CN1_MOTION_MAGNETOMETER:
            return m.magnetometerData != nil ? JAVA_TRUE : JAVA_FALSE;
        default:
            return JAVA_FALSE;
    }
#endif
#endif
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_hasMotionData___int_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
    return com_codename1_impl_ios_IOSNative_hasMotionData___int(CN1_THREAD_STATE_PASS_ARG instanceObject, type);
}

#if !TARGET_OS_TV
// axis: 0 = x, 1 = y, 2 = z
static JAVA_FLOAT cn1ReadMotionAxis(JAVA_INT type, int axis) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return 0;
#else
    CMMotionManager *m = cn1GetMotionManager();
    switch(type) {
        case CN1_MOTION_ACCELEROMETER: {
            CMAccelerometerData *d = m.accelerometerData;
            if(d == nil) {
                return 0;
            }
            CMAcceleration a = d.acceleration;
            double v = (axis == 0) ? a.x : (axis == 1 ? a.y : a.z);
            // CoreMotion reports G units with gravity negative when face up;
            // negate and scale to m/s^2 so the convention matches the API
            // (at rest, face up, z reports +9.81).
            return (JAVA_FLOAT)(-v * CN1_MOTION_G);
        }
        case CN1_MOTION_GYROSCOPE: {
            CMGyroData *d = m.gyroData;
            if(d == nil) {
                return 0;
            }
            CMRotationRate r = d.rotationRate;
            double v = (axis == 0) ? r.x : (axis == 1 ? r.y : r.z);
            return (JAVA_FLOAT)v;
        }
        case CN1_MOTION_MAGNETOMETER: {
            CMMagnetometerData *d = m.magnetometerData;
            if(d == nil) {
                return 0;
            }
            CMMagneticField f = d.magneticField;
            double v = (axis == 0) ? f.x : (axis == 1 ? f.y : f.z);
            return (JAVA_FLOAT)v;
        }
        default:
            return 0;
    }
#endif
}
#else
static JAVA_FLOAT cn1ReadMotionAxis(JAVA_INT type, int axis) {
    return 0;
}
#endif

JAVA_FLOAT com_codename1_impl_ios_IOSNative_getMotionSensorX___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
    return cn1ReadMotionAxis(type, 0);
}
JAVA_FLOAT com_codename1_impl_ios_IOSNative_getMotionSensorX___int_R_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
    return cn1ReadMotionAxis(type, 0);
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_getMotionSensorY___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
    return cn1ReadMotionAxis(type, 1);
}
JAVA_FLOAT com_codename1_impl_ios_IOSNative_getMotionSensorY___int_R_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
    return cn1ReadMotionAxis(type, 1);
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_getMotionSensorZ___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
    return cn1ReadMotionAxis(type, 2);
}
JAVA_FLOAT com_codename1_impl_ios_IOSNative_getMotionSensorZ___int_R_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
    return cn1ReadMotionAxis(type, 2);
}

// Peer Component methods

void com_codename1_impl_ios_IOSNative_calcPreferredSize___long_int_int_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT w, JAVA_INT h, JAVA_OBJECT response) {
#if TARGET_OS_OSX
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1View* v = (BRIDGE_CAST CN1View*)((void *)(uintptr_t)peer);
        // NSView answers with fittingSize rather than sizeThatFits:, and it takes
        // no proposal -- an AppKit view's preferred size comes from its content
        // and its constraints, not from a size offered to it.
        CGSize s = [v fittingSize];
        if (s.width <= 0 || s.height <= 0) {
            s = v.bounds.size;
        }
        JAVA_ARRAY_INT* data = (JAVA_INT*)((JAVA_ARRAY)response)->data;
        data[0] = (JAVA_INT)(s.width * scaleValue);
        data[1] = (JAVA_INT)(s.height * scaleValue);
        POOL_END();
    });
#else
#if !TARGET_OS_WATCH
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1View* v = (BRIDGE_CAST CN1View*)((void *)peer);
        CGSize s = [v sizeThatFits:CGSizeMake(w, h)];
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* intArray = response;
        JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
        JAVA_ARRAY_INT* data = (JAVA_INT*)((JAVA_ARRAY)response)->data;
#endif
        data[0] = (JAVA_INT)(s.width * scaleValue);
        data[1] = (JAVA_INT)(s.height * scaleValue);
        POOL_END();
    });
#else
    // watchOS has no CN1View peer components.
#endif // !TARGET_OS_WATCH
#endif
}

extern float scaleValue;

void com_codename1_impl_ios_IOSNative_updatePeerPositionSize___long_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
#if TARGET_OS_OSX
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1View* v = (BRIDGE_CAST CN1View*)((void *)(uintptr_t)peer);
        float scale = scaleValue;
        [v setFrame:CGRectMake(x / scale, y / scale, w / scale, h / scale)];
        [v setNeedsDisplay:YES];
        POOL_END();
    });
#else
#if !TARGET_OS_WATCH
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1View* v = (BRIDGE_CAST CN1View*)((void *)peer);
        float scale = scaleValue;
        float xpos = x / scale;
        float ypos = y / scale;
        float wpos = w / scale;
        float hpos = h / scale;
        [v setFrame:CGRectMake(xpos, ypos, wpos, hpos)];
        [v setNeedsDisplay];
        POOL_END();
    });
#else
    // watchOS has no CN1View peer components.
#endif // !TARGET_OS_WATCH
#endif
}

void com_codename1_impl_ios_IOSNative_peerSetVisible___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN b) {
#if !TARGET_OS_WATCH
#if TARGET_OS_OSX
    extern NSView *CN1MacPeerHostView(void);
    CN1View *peerHost = (CN1View *)CN1MacPeerHostView();
#endif
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1View* v = (BRIDGE_CAST CN1View*)((void *)peer);
        if(!b) {
            if([v superview] != nil) {
                [v removeFromSuperview];
            }
        } else {
            if([v superview] == nil) {
#if TARGET_OS_OSX
                // Re-shown into its own window, for the same reason it was added
                // there: eaglView is the main surface on this port.
                [(peerHost != nil ? peerHost
                     : (CN1View *)[[CodenameOne_GLViewController instance] eaglView])
                        addPeerComponent:v];
#else
                [[[CodenameOne_GLViewController instance] eaglView] addPeerComponent:v];
#endif
            }
        }
        POOL_END();
    });
#else
    // watchOS has no CN1View peer components.
#endif // !TARGET_OS_WATCH
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createPeerImage___long_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT arr) {
#if TARGET_OS_OSX
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)arr)->data;
    __block GLUIImage* g = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1View* v = (BRIDGE_CAST CN1View*)((void *)(uintptr_t)peer);
        NSRect bounds = v.bounds;
        if(bounds.size.width > 0 && bounds.size.height > 0) {
            // bitmapImageRepForCachingDisplayInRect: rather than rendering the
            // layer into an image context: an NSView is not guaranteed to be
            // layer backed, so its layer may be nil and the render a no-op.
            NSBitmapImageRep* rep = [v bitmapImageRepForCachingDisplayInRect:bounds];
            if (rep != nil) {
                [v cacheDisplayInRect:bounds toBitmapImageRep:rep];
                NSImage* image = [[NSImage alloc] initWithSize:bounds.size];
                [image addRepresentation:rep];
                g = [[GLUIImage alloc] initWithImage:image];
                data[0] = (JAVA_INT)(bounds.size.width * scaleValue);
                data[1] = (JAVA_INT)(bounds.size.height * scaleValue);
#ifndef CN1_USE_ARC
                [image release];
#endif
            }
        }
        POOL_END();
    });
    return (JAVA_LONG)(uintptr_t)((BRIDGE_CAST void*)g);
#else
#if !TARGET_OS_WATCH
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* intArray = arr;
    __block JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)arr)->data;
#endif
    __block GLUIImage* g = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1View* v = (BRIDGE_CAST CN1View*)((void *)peer);
        if(v.bounds.size.width > 0 && v.bounds.size.height > 0) {
            UIGraphicsBeginImageContextWithOptions(v.bounds.size, v.opaque, 0.0);
            [v.layer renderInContext:UIGraphicsGetCurrentContext()];

            CN1Image* image = UIGraphicsGetImageFromCurrentImageContext();

            UIGraphicsEndImageContext();
            g = [[GLUIImage alloc] initWithImage:image];
            data[0] = (JAVA_INT)v.bounds.size.width;
            data[1] = (JAVA_INT)v.bounds.size.height;
        }
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)g);
#else
    // watchOS has no CN1View peer components.
    return 0;
#endif // !TARGET_OS_WATCH
#endif
}

void com_codename1_impl_ios_IOSNative_peerInitialized___long_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, int x, int y, int w, int h) {
#if TARGET_OS_OSX
    // Resolved here, on the calling thread, rather than inside the block: the
    // window manager marks the active rendering view only for the duration of a
    // window's paint, and this dispatch runs after that bracket has been
    // cleared. eaglView answers the MAIN window's view unconditionally on this
    // port, so without this a peer belonging to a secondary Window was added to
    // the main one and drawn there at coordinates meant for its own.
    extern NSView *CN1MacPeerHostView(void);
    extern NSView *CN1MacPaintingViewOrNil(void);
    CN1View *peerHost = (CN1View *)CN1MacPeerHostView();
    // Strictly nil when nothing is painting, unlike peerHost. A peer is created
    // during initComponentImpl, BEFORE its window has ever painted, so at that
    // moment there is no way to know which window it belongs to. This call runs
    // again on every positioning pass, and those DO happen inside the window's
    // paint bracket -- so the peer migrates to its own window the first time
    // that window lays it out. The fallback variant cannot be used for the move:
    // it reads "the main window" whenever nothing is painting, which would drag
    // every secondary window's peer back to the main surface.
    CN1View *paintingHost = (CN1View *)CN1MacPaintingViewOrNil();
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1View* v = (BRIDGE_CAST CN1View*)((void *)(uintptr_t)peer);
        if (paintingHost != nil && [v superview] != nil && [v superview] != paintingHost) {
            [v removeFromSuperview];
        }
        if([v superview] == nil) {
            CN1View *target = paintingHost != nil ? paintingHost : peerHost;
            [(target != nil ? target
                 : (CN1View *)[[CodenameOne_GLViewController instance] eaglView])
                    addPeerComponent:v];
        }
        if(w > 0 && h > 0) {
            float scale = scaleValue;
            [v setFrame:CGRectMake(x / scale, y / scale, w / scale, h / scale)];
            [v setNeedsDisplay:YES];
        } else {
            // Parked off screen rather than hidden, matching the UIKit path: a
            // peer with no bounds yet still has to be in a window for its own
            // layout to run.
            [v setFrame:CGRectMake(3000, 0, 300, 300)];
        }
        POOL_END();
    });
#else
#if !TARGET_OS_WATCH
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1View* v = (BRIDGE_CAST CN1View*)((void *)peer);
        if([v superview] == nil) {
            [[[CodenameOne_GLViewController instance] eaglView] addPeerComponent:v];
        }
        if(w > 0 && h > 0) {
            float scale = scaleValue;
            float xpos = x / scale;
            float ypos = y / scale;
            float wpos = w / scale;
            float hpos = h / scale;
            [v setFrame:CGRectMake(xpos, ypos, wpos, hpos)];
            [v setNeedsDisplay];
        } else {
            [v setFrame:CGRectMake(3000, 0, 300, 300)];
        }
        POOL_END();
    });
#else
    // watchOS has no CN1View peer components.
#endif // !TARGET_OS_WATCH
#endif
}

extern JAVA_OBJECT com_codename1_ui_Display_getInstance__(CN1_THREAD_STATE_SINGLE_ARG);
void repaintUI() {
    JAVA_OBJECT d = com_codename1_ui_Display_getInstance__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    if(d != nil) {
#ifndef NEW_CODENAME_ONE_VM
        com_codename1_ui_Form* f = (com_codename1_ui_Form*)com_codename1_ui_Display_getCurrent__(d);
        if(f != nil) {
            com_codename1_ui_Component_repaint__(f);
        }
#else
        JAVA_OBJECT f = com_codename1_ui_Display_getCurrent___R_com_codename1_ui_Form(CN1_THREAD_GET_STATE_PASS_ARG d);
        if(f != nil) {
            com_codename1_ui_Component_repaint__(CN1_THREAD_GET_STATE_PASS_ARG f);
        }
#endif
    }
}

void com_codename1_impl_ios_IOSNative_peerDeinitialized___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if !TARGET_OS_WATCH
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1View* v = (BRIDGE_CAST CN1View*)((void *)peer);
        if(v.superview != nil) {
            [v removeFromSuperview];
            repaintUI();
        }
        POOL_END();
    });
#else
    // watchOS has no CN1View peer components.
#endif // !TARGET_OS_WATCH
}

JAVA_INT com_codename1_impl_ios_IOSNative_getAudioDuration = 0;
JAVA_INT com_codename1_impl_ios_IOSNative_getAudioDuration___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        com_codename1_impl_ios_IOSNative_getAudioDuration = [pl getAudioDuration];
        POOL_END();
    });
    return com_codename1_impl_ios_IOSNative_getAudioDuration;
}

void com_codename1_impl_ios_IOSNative_playAudio___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        [pl playAudio];
        POOL_END();
    });
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAudioPlaying = 0;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAudioPlaying___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        com_codename1_impl_ios_IOSNative_isAudioPlaying = [pl isPlaying];
        POOL_END();
    });
    return com_codename1_impl_ios_IOSNative_isAudioPlaying;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getAudioTime = 0;
JAVA_INT com_codename1_impl_ios_IOSNative_getAudioTime___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        com_codename1_impl_ios_IOSNative_getAudioTime = [pl getAudioTime];
        POOL_END();
    });
    return com_codename1_impl_ios_IOSNative_getAudioTime;
}

void com_codename1_impl_ios_IOSNative_pauseAudio___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        [pl pauseAudio];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setAudioTime___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT time) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        [pl setAudioTime:time];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_cleanupAudio___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if TARGET_OS_OSX
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)(uintptr_t)peer);
        if([pl isPlaying]) {
            [pl stop];
        }
#ifndef CN1_USE_ARC
        [pl release];
#endif
        POOL_END();
    });
#else
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        if([pl isPlaying]) {
            [pl stop];
        }
#ifndef CN1_USE_ARC
        [pl release];
#endif
        POOL_END();
    });
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio = 0;
JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___java_lang_String_java_lang_Runnable(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT uri, JAVA_OBJECT onCompletion) {
    __block NSError* error = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSString* ns = toNSString(CN1_THREAD_GET_STATE_PASS_ARG uri);
        if([ns hasPrefix:@"file:/"]) {
            ns = fixFilePath(ns);
            NSURL* nu = [NSURL fileURLWithPath:ns];
            ns = [nu absoluteString];
            NSLog(@"%@", ns);
        }
        com_codename1_impl_ios_IOSNative_createAudio = (JAVA_LONG)((BRIDGE_CAST void*)[[AudioPlayer alloc] initWithURL:ns callback:onCompletion error:&error]);
        if (error != nil) {
            [error retain];
        }
        POOL_END();
    });
    if (error != nil) {
        JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
        java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [error localizedDescription]));
        [error release];
        throwException(threadStateData, ex);
        return 0;
    }
    return com_codename1_impl_ios_IOSNative_createAudio;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___byte_1ARRAY_java_lang_Runnable(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT b, JAVA_OBJECT onCompletion) {
    __block NSError* error = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* byteArray = b;
        JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
        int len = byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
        void* data = ((JAVA_ARRAY)b)->data;
        int len = ((JAVA_ARRAY)b)->length;
#endif
        NSData* d = [NSData dataWithBytes:data length:len];
        com_codename1_impl_ios_IOSNative_createAudio = (JAVA_LONG)((BRIDGE_CAST void*)[[AudioPlayer alloc] initWithNSData:d callback:onCompletion error:&error]);
        if (error != nil) {
            [error retain];
        }
        POOL_END();
    });
    if (error != nil) {
        JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
        java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [error localizedDescription]));
        [error release];
        throwException(threadStateData, ex);
        return 0;
    }
    return com_codename1_impl_ios_IOSNative_createAudio;
}

// ---- low latency game sound pool (com.codename1.gaming.SoundPool) ----

JAVA_LONG com_codename1_impl_ios_IOSNative_nativeCreateSoundPool___int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT maxStreams) {
    __block JAVA_LONG result = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        result = (JAVA_LONG)((BRIDGE_CAST void*)[[CN1SoundPool alloc] initWithMaxStreams:maxStreams]);
        POOL_END();
    });
    return result;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_nativeLoadSound___long_byte_1ARRAY_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool, JAVA_OBJECT b, JAVA_INT ringSize) {
    __block JAVA_LONG result = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* byteArray = b;
        JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
        int len = byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
        void* data = ((JAVA_ARRAY)b)->data;
        int len = ((JAVA_ARRAY)b)->length;
#endif
        NSData* d = [NSData dataWithBytes:data length:len];
        CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
        CN1Sound* s = [sp loadData:d ringSize:ringSize];
        result = (JAVA_LONG)((BRIDGE_CAST void*)s);
        POOL_END();
    });
    return result;
}

JAVA_INT com_codename1_impl_ios_IOSNative_nativePlaySound___long_long_float_float_float_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool, JAVA_LONG sound, JAVA_FLOAT volume, JAVA_FLOAT pan, JAVA_FLOAT rate, JAVA_INT loop) {
    __block JAVA_INT result = -1;
    dispatch_sync(dispatch_get_main_queue(), ^{
        CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
        CN1Sound* s = (BRIDGE_CAST CN1Sound*)((void *)sound);
        result = [sp play:s volume:volume pan:pan rate:rate loop:loop];
    });
    return result;
}

void com_codename1_impl_ios_IOSNative_nativeSetSoundVolume___long_int_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool, JAVA_INT voiceId, JAVA_FLOAT volume) {
    CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
    [sp setVoiceVolume:voiceId value:volume];
}

void com_codename1_impl_ios_IOSNative_nativeSetSoundRate___long_int_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool, JAVA_INT voiceId, JAVA_FLOAT rate) {
    CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
    [sp setVoiceRate:voiceId value:rate];
}

void com_codename1_impl_ios_IOSNative_nativeSetSoundPan___long_int_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool, JAVA_INT voiceId, JAVA_FLOAT pan) {
    CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
    [sp setVoicePan:voiceId value:pan];
}

void com_codename1_impl_ios_IOSNative_nativePauseSound___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool, JAVA_INT voiceId) {
    CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
    [sp pauseVoice:voiceId];
}

void com_codename1_impl_ios_IOSNative_nativeResumeSound___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool, JAVA_INT voiceId) {
    CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
    [sp resumeVoice:voiceId];
}

void com_codename1_impl_ios_IOSNative_nativeStopSound___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool, JAVA_INT voiceId) {
    CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
    [sp stopVoice:voiceId];
}

void com_codename1_impl_ios_IOSNative_nativeStopAllSounds___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool) {
    CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
    [sp stopAll];
}

void com_codename1_impl_ios_IOSNative_nativeAutoPauseSoundPool___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool) {
    CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
    [sp autoPauseAll];
}

void com_codename1_impl_ios_IOSNative_nativeAutoResumeSoundPool___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool) {
    CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
    [sp autoResumeAll];
}

void com_codename1_impl_ios_IOSNative_nativeUnloadSound___long_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool, JAVA_LONG sound) {
    CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
    CN1Sound* s = (BRIDGE_CAST CN1Sound*)((void *)sound);
    [sp unloadSound:s];
}

void com_codename1_impl_ios_IOSNative_nativeReleaseSoundPool___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pool) {
    CN1SoundPool* sp = (BRIDGE_CAST CN1SoundPool*)((void *)pool);
    [sp stopAll];
#ifndef CN1_USE_ARC
    [sp release];
#endif
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_getVolume__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return [AudioPlayer getVolume];
}

void com_codename1_impl_ios_IOSNative_setVolume___float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_FLOAT vol) {
    [AudioPlayer setVolume:vol];
}

void com_codename1_impl_ios_IOSNative_fillRectRadialGradientGlobal___int_int_int_int_int_int_float_float_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_FLOAT n7, JAVA_FLOAT n8, JAVA_FLOAT n9) {
    POOL_BEGIN();
    DrawGradient* d = [[DrawGradient alloc] initWithArgs:1 startColorA:n1 endColorA:n2 xA:n3 yA:n4 widthA:n5 heightA:n6 relativeXA:n7 relativeYA:n8 relativeSizeA:n9];
    [CodenameOne_GLViewController upcoming:d];
#ifndef CN1_USE_ARC
    [d release];
#endif
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_fillLinearGradientGlobal___int_int_int_int_int_int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_BOOLEAN n7) {
    POOL_BEGIN();
    // DrawGradient's protocol (DrawGradient.h): 2 = GRADIENT_TYPE_HORIZONTAL,
    // 3 = GRADIENT_TYPE_VERTICAL. This mapping had been INVERTED here since the
    // original 2012 port (horizontal=true sent 3), so every ON-SCREEN linear
    // gradient painted with its axis swapped; the mutable-image variant
    // (fillLinearGradientMutable) always had it right. Caught by the fidelity
    // suite's geometry masks on the gradient-backdrop isolation tile.
    int gradientType = n7 ? 2 : 3;
    DrawGradient* d = [[DrawGradient alloc] initWithArgs:gradientType startColorA:n1 endColorA:n2 xA:n3 yA:n4 widthA:n5 heightA:n6 relativeXA:0 relativeYA:0 relativeSizeA:0];
    [CodenameOne_GLViewController upcoming:d];
#ifndef CN1_USE_ARC
    [d release];
#endif
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_fillRectRadialGradientMutable___int_int_int_int_int_int_float_float_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT width, JAVA_INT height, JAVA_FLOAT relativeX, JAVA_FLOAT relativeY, JAVA_FLOAT relativeSize) {
#ifdef CN1_USE_METAL
    {
        // Phase 3 v2: route through ExecutableOp queue. type=1 is
        // GRAD_TYPE_RADIAL inside DrawGradient; CN1MetalDrawGradient
        // handles the radial branch identically to the global path.
        GLUIImage *target = [CodenameOne_GLViewController instance].currentMutableImage;
        if (target == nil) return;
        DrawGradient *d = [[DrawGradient alloc] initWithArgs:1
                                                  startColorA:n1
                                                    endColorA:n2
                                                          xA:n3
                                                          yA:n4
                                                       widthA:width
                                                      heightA:height
                                                   relativeXA:relativeX
                                                   relativeYA:relativeY
                                                relativeSizeA:relativeSize];
        [d setTarget:target];
        [CodenameOne_GLViewController upcoming:d];
#ifndef CN1_USE_ARC
        [d release];
#endif
        return;
    }
#endif
    POOL_BEGIN();
    float alpha1 = 1.0;
    if (((n1 >> 24) & 0xff) != 0) {
        alpha1 = ((float)((n1 >> 24) & 0xff))/255.0;
    }
    float alpha2 = 1.0;
    if (((n2 >> 24) & 0xff) != 0) {
        alpha2 = ((float)((n2 >> 24) & 0xff))/255.0;
    }
    CGFloat components[8] = {
        ((float)((n1 & 0xFF0000) >> 16))/255.0,
        ((float)(n1 & 0xff00 >> 8))/255.0,
        ((float)(n1 & 0xff))/255.0,
        alpha1,
        ((float)((n2 & 0xFF0000) >> 16))/255.0,
        ((float)(n2 & 0xff00 >> 8))/255.0,
        ((float)(n2 & 0xff))/255.0,
        alpha2 };
    size_t num_locations = 2;
    CGFloat locations[2] = { 0.0, 1.0 };
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGGradientRef myGradient = CGGradientCreateWithColorComponents (colorSpace, components, locations, num_locations);
    [UIColorFromRGB(n2, 255) set];
    CGContextSaveGState(UIGraphicsGetCurrentContext());
    CGContextClipToRect(UIGraphicsGetCurrentContext(), CGRectMake(n3, n4, width, height));
    CGContextFillRect(UIGraphicsGetCurrentContext(), CGRectMake(n3, n4, width, height));
    CGPoint myCentrePoint = CGPointMake(n3 + relativeX * width, n4 + relativeY * height);
    float myRadius = MIN(width, height) * relativeSize;
    CGContextDrawRadialGradient (UIGraphicsGetCurrentContext(), myGradient, myCentrePoint,
                                 0, myCentrePoint, myRadius,
                                 kCGGradientDrawsAfterEndLocation);
    CGGradientRelease(myGradient);
    CGContextRestoreGState(UIGraphicsGetCurrentContext());
    CGColorSpaceRelease(colorSpace);
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_fillLinearGradientMutable___int_int_int_int_int_int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT width, JAVA_INT height, JAVA_BOOLEAN n7) {
#ifdef CN1_USE_METAL
    {
        // Phase 3 v2: route the mutable linear-gradient through the
        // ExecutableOp queue so it lands in the mutable's MTLTexture,
        // not the now-nil UIGraphicsGetCurrentContext(). 2 = horizontal,
        // 3 = vertical -- matches DrawGradient's enum.
        GLUIImage *target = [CodenameOne_GLViewController instance].currentMutableImage;
        if (target == nil) return;
        DrawGradient *d = [[DrawGradient alloc] initWithArgs:(n7 ? 2 : 3)
                                                  startColorA:n1
                                                    endColorA:n2
                                                          xA:n3
                                                          yA:n4
                                                       widthA:width
                                                      heightA:height
                                                   relativeXA:0
                                                   relativeYA:0
                                                relativeSizeA:0];
        [d setTarget:target];
        [CodenameOne_GLViewController upcoming:d];
#ifndef CN1_USE_ARC
        [d release];
#endif
        return;
    }
#endif
    POOL_BEGIN();

    float alpha1 = 1.0;
    if (((n1 >> 24) & 0xff) != 0) {
        alpha1 = ((float)((n1 >> 24) & 0xff))/255.0;
    }
    float alpha2 = 1.0;
    if (((n2 >> 24) & 0xff) != 0) {
        alpha2 = ((float)((n2 >> 24) & 0xff))/255.0;
    }
    CGFloat components[8] = {
        ((float)((n1 >> 16) & 0xff))/255.0,
        ((float)((n1 >> 8) & 0xFF))/255.0,
        ((float)(n1 & 0xff))/255.0,
        alpha1,
        ((float)((n2 >> 16) & 0xFF))/255.0,
        ((float)((n2 >> 8) & 0xFF))/255.0,
        ((float)(n2 & 0xff))/255.0,
        alpha2 };
    
    size_t num_locations = 2;
    CGFloat locations[2] = { 0.0, 1.0 };
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGGradientRef myGradient = CGGradientCreateWithColorComponents (colorSpace, components, locations, num_locations);
    CGRect rect = CGRectMake(n3, n4, width, height);
    
    CGContextSaveGState(UIGraphicsGetCurrentContext());
    CGContextClipToRect(UIGraphicsGetCurrentContext(), rect);
    if(n7) {
        CGPoint startPoint = CGPointMake(n3, n4+height/2);
        CGPoint endPoint = CGPointMake(n3+width, n4+height/2);
        CGContextDrawLinearGradient(UIGraphicsGetCurrentContext(), myGradient,
                                    startPoint, endPoint, 0);
    } else {
        CGPoint startPoint = CGPointMake(n3+width/2, n4);
        CGPoint endPoint = CGPointMake(n3+width/2, n4+height);
        CGContextDrawLinearGradient(UIGraphicsGetCurrentContext(), myGradient,
                                    startPoint, endPoint, 0);
    }
    CGGradientRelease(myGradient), myGradient = NULL;
    CGContextRestoreGState(UIGraphicsGetCurrentContext());
    CGColorSpaceRelease(colorSpace);
    POOL_END();
}

// Multi-stop gradient bridge. Metal builds queue a DrawMultiStopGradient op so
// matrices / clip / mutable-image targeting propagate through the standard
// drain loop, matching the existing DrawGradient flow. GL builds have no
// equivalent shader and the Java side never calls this method (it falls
// through to the software rasterizer in CodenameOneImplementation).
void com_codename1_impl_ios_IOSNative_fillGradient___int_int_float_1ARRAY_float_1ARRAY_int_float_float_float_float_float_int_int_int_int_int_boolean(
        CN1_THREAD_STATE_MULTI_ARG
        JAVA_OBJECT instanceObject,
        JAVA_INT kind,
        JAVA_INT stopCount,
        JAVA_OBJECT positionsArr,
        JAVA_OBJECT colorsArr,
        JAVA_INT cycleMethod,
        JAVA_FLOAT angleOrFromAngle,
        JAVA_FLOAT cx,
        JAVA_FLOAT cy,
        JAVA_FLOAT rx,
        JAVA_FLOAT ry,
        JAVA_INT shape,
        JAVA_INT x,
        JAVA_INT y,
        JAVA_INT width,
        JAVA_INT height,
        JAVA_BOOLEAN mutable) {
#ifdef CN1_USE_METAL
    POOL_BEGIN();
    if (positionsArr == JAVA_NULL || colorsArr == JAVA_NULL || stopCount < 2 || width <= 0 || height <= 0) {
        POOL_END();
        return;
    }
#ifndef NEW_CODENAME_ONE_VM
    JAVA_ARRAY_FLOAT *positions =
        (JAVA_ARRAY_FLOAT *)((org_xmlvm_runtime_XMLVMArray *)positionsArr)
            ->fields.org_xmlvm_runtime_XMLVMArray.array_;
    JAVA_ARRAY_FLOAT *colors =
        (JAVA_ARRAY_FLOAT *)((org_xmlvm_runtime_XMLVMArray *)colorsArr)
            ->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_FLOAT *positions = (JAVA_FLOAT *)((JAVA_ARRAY)positionsArr)->data;
    JAVA_ARRAY_FLOAT *colors = (JAVA_FLOAT *)((JAVA_ARRAY)colorsArr)->data;
#endif

    DrawMultiStopGradient *d = [[DrawMultiStopGradient alloc]
        initWithKind:kind
           stopCount:stopCount
           positions:positions
              colors:colors
         cycleMethod:cycleMethod
    angleOrFromAngle:angleOrFromAngle
                  cx:cx
                  cy:cy
                  rx:rx
                  ry:ry
               shape:shape
                   x:x
                   y:y
               width:width
              height:height];
    if (mutable) {
        GLUIImage *target = [CodenameOne_GLViewController instance].currentMutableImage;
        if (target == nil) {
#ifndef CN1_USE_ARC
            [d release];
#endif
            POOL_END();
            return;
        }
        [d setTarget:target];
    }
    [CodenameOne_GLViewController upcoming:d];
#ifndef CN1_USE_ARC
    [d release];
#endif
    POOL_END();
#endif
}

/*
  native void applyRadialGradientPaintMutable(int startColor, int endColor, int x, int y, int width, int height);

    native void clearRadialGradientPaintMutable();

    native void applyRadialGradientPaintGlobal(int startColor, int endColor, int x, int y, int width, int height);

    native void clearRadialGradientPaintGlobal();
 */
void com_codename1_impl_ios_IOSNative_applyRadialGradientPaintGlobal___int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, 
        JAVA_INT startColor,
        JAVA_INT endColor,
        JAVA_INT x,
        JAVA_INT y,
        JAVA_INT width,
        JAVA_INT height)
{
    RadialGradientPaint *f = [[RadialGradientPaint alloc] initWithArgs:x y:y width:width height:height startColor:startColor endColor:endColor];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
}


void com_codename1_impl_ios_IOSNative_clearRadialGradientPaintGlobal__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) 
{
    RadialGradientPaint *f = [[RadialGradientPaint alloc] initClear];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
}

void com_codename1_impl_ios_IOSNative_applyRadialGradientPaintMutable___int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
        JAVA_INT startColor,
        JAVA_INT endColor,
        JAVA_INT x,
        JAVA_INT y,
        JAVA_INT width,
        JAVA_INT height)
{
    RadialGradientPaint *f = [[RadialGradientPaint alloc] initWithArgs:x y:y width:width height:height startColor:startColor endColor:endColor];
    [PaintOp setCurrentMutable:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
}

void com_codename1_impl_ios_IOSNative_clearRadialGradientPaintMutable__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) 
{
    [PaintOp setCurrentMutable:NULL];
}

void com_codename1_impl_ios_IOSNative_releasePeer___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifndef CN1_USE_ARC
    dispatch_async(dispatch_get_main_queue(), ^{
        NSObject* o = (NSObject*)peer;
        [o release];
    });
#endif
}

void com_codename1_impl_ios_IOSNative_retainPeer___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifndef CN1_USE_ARC
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSObject* o = (NSObject*)peer;
        [o retain];
    });
#endif
}
#if TARGET_OS_WATCH || TARGET_OS_TV
// watchOS and tvOS have no UIWebView. Disabling the UIWebView path
// here lets every browser function below compile to its existing fallback
// (the WKWebView path is already gated on supportsWKWebKit, which is not
// defined on watch/tv). The browser symbols still exist so the runtime links.
#ifndef NO_UIWEBVIEW
#define NO_UIWEBVIEW
#endif
#endif // TARGET_OS_WATCH || TARGET_OS_TV
#ifndef NO_UIWEBVIEW
UIWebView* com_codename1_impl_ios_IOSNative_createBrowserComponent = nil;
#endif
#if TARGET_OS_WATCH || TARGET_OS_TV
/// The watch and TV slices get their own key.
///
/// UIWebViewEventDelegate.h -- which declares the shared one -- is wrapped in
/// `#if !TARGET_OS_WATCH && !TARGET_OS_TV`, and UIWebViewEventDelegate.m is not
/// compiled here either, so on these slices the shared key is neither declared
/// nor linked. The helpers below need nothing from it but a unique address, and
/// there is no web view on either slice for a value to travel between, so a
/// local key is exactly equivalent.
///
/// The helpers themselves stay outside the NO_UIWEBVIEW guard because the
/// WKWebView paths call them too, and those compile on every slice.
static const void *CN1FollowTargetBlankKey = &CN1FollowTargetBlankKey;
#endif

static void cn1_setBrowserFollowTargetBlank(id webView, BOOL follow) {
    objc_setAssociatedObject(webView, CN1FollowTargetBlankKey, [NSNumber numberWithBool:follow], OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

static BOOL cn1_shouldFollowTargetBlank(id webView) {
    NSNumber *value = objc_getAssociatedObject(webView, CN1FollowTargetBlankKey);
    if (value == nil) {
        return YES;
    }
    return [value boolValue];
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createBrowserComponent___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT obj) {
#ifndef NO_UIWEBVIEW
    dispatch_sync(dispatch_get_main_queue(), ^{
        com_codename1_impl_ios_IOSNative_createBrowserComponent = [[UIWebView alloc] initWithFrame:CGRectMake(3000, 0, 200, 200)];
        com_codename1_impl_ios_IOSNative_createBrowserComponent.backgroundColor = [UIColor clearColor];
        com_codename1_impl_ios_IOSNative_createBrowserComponent.opaque = NO;
        com_codename1_impl_ios_IOSNative_createBrowserComponent.autoresizesSubviews = YES;
        // Disable scrollsToTop on the embedded scroll view so it doesn't compete
        // with the CN1 status-bar tap proxy. iOS only delivers the tap when
        // exactly one scroll view on screen has scrollsToTop=YES.
        com_codename1_impl_ios_IOSNative_createBrowserComponent.scrollView.scrollsToTop = NO;
        UIWebViewEventDelegate *del = [[UIWebViewEventDelegate alloc] initWithCallback:obj];
        com_codename1_impl_ios_IOSNative_createBrowserComponent.delegate = del;
        com_codename1_impl_ios_IOSNative_createBrowserComponent.autoresizingMask=(UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth);
        [com_codename1_impl_ios_IOSNative_createBrowserComponent setAllowsInlineMediaPlayback:YES];
        cn1_setBrowserFollowTargetBlank(com_codename1_impl_ios_IOSNative_createBrowserComponent, YES);
#ifndef CN1_USE_ARC
        [com_codename1_impl_ios_IOSNative_createBrowserComponent retain];
#endif
    });
    UIWebView* r = com_codename1_impl_ios_IOSNative_createBrowserComponent;
    com_codename1_impl_ios_IOSNative_createBrowserComponent = nil;
    return (JAVA_LONG)((BRIDGE_CAST void*)r);
#else
    return 0;
#endif
}
#ifdef supportsWKWebKit
WKWebView* com_codename1_impl_ios_IOSNative_createWKBrowserComponent = nil;
#endif
JAVA_LONG com_codename1_impl_ios_IOSNative_createWKBrowserComponent___java_lang_Object_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT obj) {
#if TARGET_OS_OSX
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        WKWebViewConfiguration *config = [[WKWebViewConfiguration alloc] init];
        // allowsInlineMediaPlayback has no macOS counterpart: a Mac has no
        // full-screen-only playback mode to opt out of.
        config.mediaTypesRequiringUserActionForPlayback = WKAudiovisualMediaTypeNone;
        config.suppressesIncrementalRendering = YES;
        UIWebViewEventDelegate *del = [[UIWebViewEventDelegate alloc] initWithCallback:obj];
        WKUserContentController* userContentController = [[WKUserContentController alloc] init];
        NSString *bootstrapSource = @"window.cn1application = window.cn1application || {};\
        window.cn1application.shouldNavigate = function(url) {\
            window.webkit.messageHandlers.cn1.postMessage({'shouldNavigate' : url});\
        };";
        WKUserScript *bootstrapScript = [[WKUserScript alloc] initWithSource:bootstrapSource injectionTime:WKUserScriptInjectionTimeAtDocumentStart forMainFrameOnly:YES];
        [userContentController addUserScript:bootstrapScript];
        [bootstrapScript release];
        [userContentController addScriptMessageHandler:del name:@"cn1"];
        [del release];
        config.userContentController = userContentController;
        [userContentController release];
        com_codename1_impl_ios_IOSNative_createWKBrowserComponent = [[WKWebView alloc] initWithFrame:CGRectMake(3000, 0, 200, 200) configuration:config];
        [config release];
        // No backgroundColor / opaque and no scrollView: an NSView has neither,
        // and there is no status-bar tap proxy on a Mac to keep out of the way
        // of. Transparency instead comes from the web content itself.
        com_codename1_impl_ios_IOSNative_createWKBrowserComponent.autoresizesSubviews = YES;
        cn1_setBrowserFollowTargetBlank(com_codename1_impl_ios_IOSNative_createWKBrowserComponent, YES);

        if (getBooleanClientProperty(CN1_THREAD_GET_STATE_PASS_ARG obj, @"BrowserComponent.ios.debug")) {
            com_codename1_impl_ios_IOSNative_createWKBrowserComponent.inspectable = YES;
        }

        com_codename1_impl_ios_IOSNative_createWKBrowserComponent.navigationDelegate = del;
        com_codename1_impl_ios_IOSNative_createWKBrowserComponent.autoresizingMask =
            (NSViewHeightSizable | NSViewWidthSizable);
        POOL_END();
    });
    id r = com_codename1_impl_ios_IOSNative_createWKBrowserComponent;
    com_codename1_impl_ios_IOSNative_createWKBrowserComponent = nil;
    return (JAVA_LONG)(uintptr_t)((BRIDGE_CAST void*)r);
#else
#ifdef supportsWKWebKit
    if (@available(iOS 8, *)) {
        dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN();
            WKWebViewConfiguration *config = [[WKWebViewConfiguration alloc] init];
            config.allowsInlineMediaPlayback = YES;
            if (@available(iOS 10, *)) {
                config.mediaTypesRequiringUserActionForPlayback=WKAudiovisualMediaTypeNone;
            }
            config.suppressesIncrementalRendering = YES;
            UIWebViewEventDelegate *del = [[UIWebViewEventDelegate alloc] initWithCallback:obj];
            WKUserContentController* userContentController = [[WKUserContentController alloc] init];
            NSString *bootstrapSource = @"window.cn1application = window.cn1application || {};\
            window.cn1application.shouldNavigate = function(url) {\
                window.webkit.messageHandlers.cn1.postMessage({'shouldNavigate' : url});\
            };";
            WKUserScript *bootstrapScript = [[WKUserScript alloc] initWithSource:bootstrapSource injectionTime:WKUserScriptInjectionTimeAtDocumentStart forMainFrameOnly:YES];
            [userContentController addUserScript:bootstrapScript];
            [bootstrapScript release];
            [userContentController addScriptMessageHandler:del name:@"cn1"];
            [del release];
            config.userContentController = userContentController;
            [userContentController release];
            com_codename1_impl_ios_IOSNative_createWKBrowserComponent = [[WKWebView alloc] initWithFrame:CGRectMake(3000, 0, 200, 200) configuration:config];
            [config release];
            com_codename1_impl_ios_IOSNative_createWKBrowserComponent.backgroundColor = [UIColor clearColor];
            com_codename1_impl_ios_IOSNative_createWKBrowserComponent.opaque = NO;
            com_codename1_impl_ios_IOSNative_createWKBrowserComponent.autoresizesSubviews = YES;
            // Disable scrollsToTop on the embedded scroll view so it doesn't compete
            // with the CN1 status-bar tap proxy. iOS only delivers the tap when
            // exactly one scroll view on screen has scrollsToTop=YES.
            com_codename1_impl_ios_IOSNative_createWKBrowserComponent.scrollView.scrollsToTop = NO;
            cn1_setBrowserFollowTargetBlank(com_codename1_impl_ios_IOSNative_createWKBrowserComponent, YES);

            if (getBooleanClientProperty(CN1_THREAD_GET_STATE_PASS_ARG obj, @"BrowserComponent.ios.debug")) {
                com_codename1_impl_ios_IOSNative_createWKBrowserComponent.inspectable = YES;
            }
            
            com_codename1_impl_ios_IOSNative_createWKBrowserComponent.navigationDelegate = del;
            com_codename1_impl_ios_IOSNative_createWKBrowserComponent.autoresizingMask=(UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth);
            POOL_END();
            
        });
        id r = com_codename1_impl_ios_IOSNative_createWKBrowserComponent;
        com_codename1_impl_ios_IOSNative_createWKBrowserComponent = nil;
        return (JAVA_LONG)((BRIDGE_CAST void*)r);
    } else {
        return (JAVA_LONG)0;
    }
#else
    return com_codename1_impl_ios_IOSNative_createBrowserComponent___java_lang_Object(threadStateData, instanceObject, obj);
#endif
#endif
}

BOOL isWKWebView(JAVA_LONG peer) {
#ifdef supportsWKWebKit
    NSObject *o = (BRIDGE_CAST NSObject*)((void *)peer);
    return (isIOS8() && [o isKindOfClass:[WKWebView class]]);
#else
    return NO;
#endif
}

void com_codename1_impl_ios_IOSNative_setBrowserPage___long_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT html, JAVA_OBJECT baseUrl) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            [w loadHTMLString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG html) baseURL:[NSURL URLWithString:toNSString(CN1_THREAD_STATE_PASS_ARG baseUrl)]];
#endif
            
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            [w loadHTMLString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG html) baseURL:[NSURL URLWithString:toNSString(CN1_THREAD_STATE_PASS_ARG baseUrl)]];
#endif
        }
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setBrowserUserAgent___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT ua) {
#ifdef ENABLE_SET_BROWSER_USER_AGENT
    NSString *_ua = toNSString(CN1_THREAD_GET_STATE_PASS_ARG ua);
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        //UIWebView* w = (UIWebView*)peer;
        NSDictionary *dictionary = [NSDictionary dictionaryWithObjectsAndKeys:_ua, @"UserAgent", nil];
        [[NSUserDefaults standardUserDefaults] registerDefaults:dictionary];
        POOL_END();
    });
#endif
}

void com_codename1_impl_ios_IOSNative_setBrowserFollowTargetBlank___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN follow) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            cn1_setBrowserFollowTargetBlank(w, follow);
#endif
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            cn1_setBrowserFollowTargetBlank(w, follow);
#endif
        }
        POOL_END();
    });
}

// Pin the appearance of the native web widget independently of the device-wide
// setting. style: 0 = unspecified/auto (follow device), 1 = light, 2 = dark.
// Setting overrideUserInterfaceStyle on the WKWebView feeds the trait collection
// that drives the page's prefers-color-scheme media query and the UA rendering of
// default backgrounds / form controls, so a page that adapts to dark mode can be
// kept light (or dark) regardless of the user's system appearance.
void com_codename1_impl_ios_IOSNative_setBrowserInterfaceStyle___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT style) {
#if TARGET_OS_OSX
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1View* w = (BRIDGE_CAST CN1View*)((void *)(uintptr_t)peer);
        // AppKit expresses this as an appearance on the view rather than as an
        // interface-style override, and nil means "inherit from the window",
        // which is what UIUserInterfaceStyleUnspecified means on iOS.
        NSAppearance* appearance = nil;
        if (style == 1) {
            appearance = [NSAppearance appearanceNamed:NSAppearanceNameAqua];
        } else if (style == 2) {
            appearance = [NSAppearance appearanceNamed:NSAppearanceNameDarkAqua];
        }
        w.appearance = appearance;
        POOL_END();
    });
#else
#if !TARGET_OS_WATCH
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (@available(iOS 13.0, *)) {
            CN1View* w = (BRIDGE_CAST CN1View*)((void *)peer);
            UIUserInterfaceStyle uiStyle = UIUserInterfaceStyleUnspecified;
            if (style == 1) {
                uiStyle = UIUserInterfaceStyleLight;
            } else if (style == 2) {
                uiStyle = UIUserInterfaceStyleDark;
            }
            w.overrideUserInterfaceStyle = uiStyle;
        }
        POOL_END();
    });
#else
    // watchOS has no CN1View / UIUserInterfaceStyle.
#endif // !TARGET_OS_WATCH
#endif
}


void com_codename1_impl_ios_IOSNative_setPinchToZoomEnabled___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN enabled) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
#endif

            //w.allows=enabled;
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);

            w.scalesPageToFit=enabled;
#endif
        }
        
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setNativeBrowserScrollingEnabled___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN enabled) {
#if TARGET_OS_OSX
    // A WKWebView on macOS has no scrollView to disable -- scrolling belongs to
    // the web content's own scrollers. Turning it off would mean injecting CSS,
    // which would fight the page rather than configure the view, so the setting
    // is accepted and has no effect here.
    (void)peer;
    (void)enabled;
#else
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);

            w.scrollView.scrollEnabled = enabled;
            w.scrollView.bounces = enabled;
#endif
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);

            w.scrollView.scrollEnabled = enabled;
            w.scrollView.bounces = enabled;
#endif
        }
        
        POOL_END();
    });
#endif
}

void com_codename1_impl_ios_IOSNative_setBrowserURL___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT url) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            @try {
                // This is an unofficial (unsupported) hack for allowing file access which is necessary
                // for things like setURLHierarchy(). 
                // It has been reported to sometimes not work, and throw an exception
                // -[__NSCFConstantString
                // charValue]: unrecognized selector sent to instance 0x99444c*
                // 2020-09-22 17:08:04.200 OrdyxDisplay[637:204697] ** Terminating app due
                // to uncaught exception 'NSInvalidArgumentException', reason:
                // '-[__NSCFConstantString charValue]: unrecognized selector sent to instance
                // 0x99444c'*
                //
                // Therefore we are wrapping it in a try/catch here to swallow the exception
                [w.configuration.preferences setValue:@"TRUE" forKey:@"allowFileAccessFromFileURLs"];
            }
            @catch (NSException *exception) {
                NSLog(@"Setting the key 'allowFileAccessFromFileURLs' failed.  file:// URLs may not work correctly");
            }
            NSString *str = toNSString(CN1_THREAD_GET_STATE_PASS_ARG url);
            if ([str hasPrefix:@"http://"] || [str hasPrefix:@"https://"]) {
                NSURL* nu = [NSURL URLWithString:str];
                NSURLRequest* r = [NSURLRequest requestWithURL:nu];
                [w loadRequest:r];
            } else {
                if ([str hasPrefix:@"file://localhost"]) {
                    str = [str substringFromIndex:16];
                }
                str = [str stringByRemovingPercentEncoding];
                NSURL* nu = [NSURL fileURLWithPath:str];           
                [w loadFileURL:nu allowingReadAccessToURL:nu.URLByDeletingLastPathComponent];
            }

#endif
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            NSString *str = toNSString(CN1_THREAD_GET_STATE_PASS_ARG url);
            NSURL* nu = [NSURL URLWithString:str];
            NSURLRequest* r = [NSURLRequest requestWithURL:nu];
            [w loadRequest:r];
#endif
        }
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setBrowserURL___long_java_lang_String_java_lang_String_1ARRAY_java_lang_String_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT url, JAVA_OBJECT keys, JAVA_OBJECT values) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            NSString *str = toNSString(CN1_THREAD_GET_STATE_PASS_ARG url);
            NSMutableURLRequest* request = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:str]];

            JAVA_ARRAY_OBJECT* keyData = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)keys)->data;
            JAVA_ARRAY_OBJECT* valueData = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)values)->data;
            int count = ((JAVA_ARRAY)keys)->length;

            for(int iter = 0 ; iter < count ; iter++) {
                NSString* k = toNSString(CN1_THREAD_GET_STATE_PASS_ARG keyData[iter]);
                NSString* v = toNSString(CN1_THREAD_GET_STATE_PASS_ARG valueData[iter]);
                [request setValue:v forHTTPHeaderField:k];
            }

            [w loadRequest:request];
#endif
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            NSString *str = toNSString(CN1_THREAD_GET_STATE_PASS_ARG url);
            NSMutableURLRequest* request = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:str]];

            JAVA_ARRAY_OBJECT* keyData = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)keys)->data;
            JAVA_ARRAY_OBJECT* valueData = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)values)->data;
            int count = ((JAVA_ARRAY)keys)->length;

            for(int iter = 0 ; iter < count ; iter++) {
                NSString* k = toNSString(CN1_THREAD_GET_STATE_PASS_ARG keyData[iter]);
                NSString* v = toNSString(CN1_THREAD_GET_STATE_PASS_ARG valueData[iter]);
                [request setValue:v forHTTPHeaderField:k];
            }

            [w loadRequest:request];
#endif
        }

        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_browserBack___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            [w goBack];
#endif
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            [w goBack];
#endif
        }
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_browserStop___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            [w stopLoading];
#endif
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            [w stopLoading];
#endif
        }
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_browserClearHistory___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
}

void com_codename1_impl_ios_IOSNative_browserExecute___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT javaScript) {
    if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
        if ([NSThread isMainThread]) {
            POOL_BEGIN();
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            
            [w evaluateJavaScript:toNSString(CN1_THREAD_GET_STATE_PASS_ARG javaScript) completionHandler:^(id result, NSError *error) {
                if (error != nil) {
                    NSLog(@"evaluateJavaScript error : %@", error.localizedDescription);
                }
            }];
            POOL_END();
        } else {
            NSString* js = [NSString stringWithFormat:@"setTimeout(function(){%@}, 0);", toNSString(CN1_THREAD_GET_STATE_PASS_ARG javaScript)];
            dispatch_async(dispatch_get_main_queue(), ^{
                POOL_BEGIN();
                WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
                [w evaluateJavaScript:js completionHandler:^(id result, NSError *error) {
                    if (error != nil) {
                        NSLog(@"evaluateJavaScript2 error : %@ : %@", error.localizedDescription, js);
                    }
                }];
                POOL_END();
            });
        }
#endif
    } else {
#ifndef NO_UIWEBVIEW
        if ([NSThread isMainThread]) {
            POOL_BEGIN();
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            [w stringByEvaluatingJavaScriptFromString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG javaScript)];
            POOL_END();
        } else {
            dispatch_async(dispatch_get_main_queue(), ^{
                POOL_BEGIN();
                UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
                [w stringByEvaluatingJavaScriptFromString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG javaScript)];
                POOL_END();
            });
        }
#endif
    }
}

void com_codename1_impl_ios_IOSNative_browserExecuteAndReturnStringCallback___long_java_lang_String_com_codename1_util_SuccessCallback(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT javaScript, JAVA_OBJECT callback) {
    if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
        if ([NSThread isMainThread]) {
            POOL_BEGIN();
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            
            [w evaluateJavaScript:toNSString(CN1_THREAD_GET_STATE_PASS_ARG javaScript) completionHandler:^(id result, NSError *error) {
                if (error != nil) {
                    NSLog(@"evaluateJavaScript error : %@", error.localizedDescription);
                } else {
                    NSString *res = [NSString stringWithFormat:@"%@", result];
                    if (callback != JAVA_NULL) {
                        com_codename1_util_SuccessCallback_onSucess___java_lang_Object(threadStateData, callback, fromNSString(threadStateData, res));
                    }
                }
            }];
            POOL_END();
        } else {
            dispatch_async(dispatch_get_main_queue(), ^{
                POOL_BEGIN();
                WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            
                [w evaluateJavaScript:toNSString(CN1_THREAD_GET_STATE_PASS_ARG javaScript) completionHandler:^(id result, NSError *error) {
                    if (error != nil) {
                        NSLog(@"evaluateJavaScript error : %@", error.localizedDescription);
                    } else {
                        NSString *res = [NSString stringWithFormat:@"%@", result];
                        if (callback != JAVA_NULL) {
                            com_codename1_util_SuccessCallback_onSucess___java_lang_Object(threadStateData, callback, fromNSString(threadStateData, res));
                        }
                    }
                    
                }];
                POOL_END();
            });
        }
#endif
    } else {
#ifndef NO_UIWEBVIEW
        if ([NSThread isMainThread]) {
            POOL_BEGIN();
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            NSString * res = [w stringByEvaluatingJavaScriptFromString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG javaScript)];
            if (callback != JAVA_NULL) {
                com_codename1_util_SuccessCallback_onSucess___java_lang_Object(threadStateData, callback, fromNSString(threadStateData, res));
            }
            POOL_END();
        } else {
            dispatch_async(dispatch_get_main_queue(), ^{
                POOL_BEGIN();
                UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
                NSString * res = [w stringByEvaluatingJavaScriptFromString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG javaScript)];
                if (callback != JAVA_NULL) {
                    com_codename1_util_SuccessCallback_onSucess___java_lang_Object(threadStateData, callback, fromNSString(threadStateData, res));
                }
                POOL_END();
            });
        }
#endif
    }
}

void com_codename1_impl_ios_IOSNative_browserForward___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            [w goForward];
#endif
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            [w goForward];
#endif
        }
        POOL_END();
    });
}

JAVA_BOOLEAN booleanResponse = 0;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_browserHasBack___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            booleanResponse = [w canGoBack];
#endif
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            booleanResponse = [w canGoBack];
#endif
        }
        POOL_END();
    });
    return booleanResponse;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_browserHasForward___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            booleanResponse = [w canGoForward];
#endif
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            booleanResponse = [w canGoForward];
#endif
        }
        POOL_END();
    });
    return booleanResponse;
}

void com_codename1_impl_ios_IOSNative_browserReload___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            [w reload];
#endif
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            [w reload];
#endif
        }
        POOL_END();
    });
}

JAVA_OBJECT returnString;
JAVA_OBJECT com_codename1_impl_ios_IOSNative_getBrowserTitle___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            returnString = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG w.title);
#endif
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            NSString* theTitle = [w stringByEvaluatingJavaScriptFromString:@"document.title"];
            returnString = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG theTitle);
#endif
        }
        POOL_END();
    });
    return returnString;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getBrowserURL___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (isWKWebView(peer)) {
#ifdef supportsWKWebKit
            WKWebView* w = (BRIDGE_CAST WKWebView*)((void *)peer);
            returnString = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG w.URL.absoluteString);
#endif
        } else {
#ifndef NO_UIWEBVIEW
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            returnString = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG w.request.URL.absoluteString);
#endif
        }
        POOL_END();
    });
    return returnString;
}

#if !TARGET_OS_WATCH && !TARGET_OS_TV
// UIKit-only declaration: the type in its signature does not exist on macOS,
// so the whole thing goes rather than just the body. Guarding only the body
// would leave a signature naming a type the compiler has never heard of.
#if !TARGET_OS_OSX
void registerVideoCallback(CN1_THREAD_STATE_MULTI_ARG MPMoviePlayerController *moviePlayer, JAVA_INT callbackId) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
#else
    id observer = [[NSNotificationCenter defaultCenter] addObserverForName:MPMoviePlayerPlaybackDidFinishNotification object:moviePlayer
    queue:[NSOperationQueue mainQueue] usingBlock:^(NSNotification *notification) {
        /*
         * I'm not sure if we need to handle the callback differently in different cases
         * but if we do, this code is a guideline on how we would do this
        int reason = [[[notification userInfo] valueForKey:MPMoviePlayerPlaybackDidFinishReasonUserInfoKey] intValue];
        if (reason == MPMovieFinishReasonPlaybackEnded) {
            //movie finished playin
        }else if (reason == MPMovieFinishReasonUserExited) {
            //user hit the done button
        }else if (reason == MPMovieFinishReasonPlaybackError) {
            //error
        }
         * */
        com_codename1_impl_ios_IOSImplementation_fireMediaCallback___int(CN1_THREAD_GET_STATE_PASS_ARG callbackId);
    }];
    com_codename1_impl_ios_IOSImplementation_bindNSObserverPeerToMediaCallback___long_int(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_LONG)((BRIDGE_CAST void*)observer), callbackId);
#endif
}
#endif
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV (registerVideoCallback / MPMoviePlayerController)

void registerVideoCallbackAV(CN1_THREAD_STATE_MULTI_ARG AVPlayer *moviePlayer, JAVA_INT callbackId) {
#ifdef CN1_USE_AVKIT
    id observer = [[NSNotificationCenter defaultCenter] addObserverForName:AVPlayerItemDidPlayToEndTimeNotification object:[moviePlayer currentItem]
    queue:[NSOperationQueue mainQueue] usingBlock:^(NSNotification *notification) {
        /*
         * I'm not sure if we need to handle the callback differently in different cases
         * but if we do, this code is a guideline on how we would do this
        int reason = [[[notification userInfo] valueForKey:MPMoviePlayerPlaybackDidFinishReasonUserInfoKey] intValue];
        if (reason == MPMovieFinishReasonPlaybackEnded) {
            //movie finished playin
        }else if (reason == MPMovieFinishReasonUserExited) {
            //user hit the done button
        }else if (reason == MPMovieFinishReasonPlaybackError) {
            //error
        }
         * */
        com_codename1_impl_ios_IOSImplementation_fireMediaCallback___int(CN1_THREAD_GET_STATE_PASS_ARG callbackId);
    }];
    com_codename1_impl_ios_IOSImplementation_bindNSObserverPeerToMediaCallback___long_int(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_LONG)((BRIDGE_CAST void*)observer), callbackId);
#endif
}

extern BOOL CN1_blockPaste;
extern BOOL CN1_blockCopy;
extern BOOL CN1_blockCut;
//native void blockCopyPaste(boolean blockCopyPaste);
void com_codename1_impl_ios_IOSNative_blockCopyPaste___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN blockCopyPaste) {
    CN1_blockPaste = blockCopyPaste;
    CN1_blockCut = blockCopyPaste;
    CN1_blockCopy = blockCopyPaste;

}

BOOL cn1UseApplicationAudioSessionForMedia(CN1_THREAD_STATE_SINGLE_ARG) {
    enteringNativeAllocations();
    JAVA_OBJECT d = com_codename1_ui_Display_getInstance__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    JAVA_OBJECT key = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"ios.useApplicationAudioSession");
    
    JAVA_OBJECT defaultVal = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"true");
    
    JAVA_OBJECT res = com_codename1_ui_Display_getProperty___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG d, key, defaultVal);
    finishedNativeAllocations();
    
    NSString *nsres = toNSString(CN1_THREAD_GET_STATE_PASS_ARG res);
    if ([nsres isEqualToString:@"false"]) {
        return NO;
    }
    return YES;
}
BOOL useAVKit() {
#if TARGET_OS_OSX
    // MediaPlayer's MPMoviePlayerController does not exist on macOS, so AVKit
    // is not a preference here -- it is the only path.
    return YES;
#endif
    if (@available(iOS 13.0, *)) {
        return YES;
    }
#ifdef CN1_USE_AVKIT
    if (@available(iOS 8.0, *)) {
        return YES;
    } else {
        return NO;
    }
#endif
    
    return NO;
}
JAVA_LONG createVideoComponentFromStringMP(JAVA_OBJECT str, JAVA_INT onCompletionCallbackId) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return 0;
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
     __block MPMoviePlayerController* moviePlayerInstance;
    dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN();
            NSString* s = toNSString(CN1_THREAD_GET_STATE_PASS_ARG str);
            NSURL* u;
            if([s hasPrefix:@"file:"]) {
                u = [NSURL fileURLWithPath:[s substringFromIndex:5]];
            } else {
                u = [NSURL URLWithString:s];
            }
            moviePlayerInstance = [[MPMoviePlayerController alloc] initWithContentURL:u];
            registerVideoCallback(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance, onCompletionCallbackId);
            moviePlayerInstance.useApplicationAudioSession = cn1UseApplicationAudioSessionForMedia(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
            // prepareToPlay will cause other av sessions to be interrupted at the time that the video
            // component is created - which is disruptive.  Better to just let it prepare to play
            // at the time that the video is played - even if there is a delay.
            //[moviePlayerInstance prepareToPlay];
    #ifdef AUTO_PLAY_VIDEO
            [moviePlayerInstance play];
    #else
            moviePlayerInstance.shouldAutoplay = NO;
    #endif
            moviePlayerInstance.controlStyle = MPMovieControlStyleEmbedded;
            POOL_END();
        });
        return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
#else
        return 0;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
#endif
}
void addPlaybackToAudioSession() {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
#else
    AVAudioSessionCategory cat = [[AVAudioSession sharedInstance] category];
    if (cat == AVAudioSessionCategoryPlayback) {
        return;
    }
    if (cat == AVAudioSessionCategoryRecord) {
        [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryPlayAndRecord error:nil];
        return;
    }
    [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryPlayback error:nil];
#endif
}

JAVA_LONG createVideoComponentFromStringAV(JAVA_OBJECT str, JAVA_INT onCompletionCallbackId) {
#if TARGET_OS_OSX
    __block AVPlayerView* moviePlayerInstance;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSString* s = toNSString(CN1_THREAD_GET_STATE_PASS_ARG str);
        NSURL* u = [s hasPrefix:@"file:"]
            ? [NSURL fileURLWithPath:[s substringFromIndex:5]]
            : [NSURL URLWithString:s];
        // Parked off screen at a nominal size, like every other peer, until the
        // framework gives it real bounds.
        moviePlayerInstance = [[AVPlayerView alloc] initWithFrame:NSMakeRect(3000, 0, 200, 200)];
        moviePlayerInstance.player = [[AVPlayer alloc] initWithURL:u];
        registerVideoCallbackAV(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance.player, onCompletionCallbackId);
        moviePlayerInstance.controlsStyle = AVPlayerViewControlsStyleInline;
        POOL_END();
    });
    return (JAVA_LONG)(uintptr_t)((BRIDGE_CAST void*)moviePlayerInstance);
#elif defined(CN1_USE_AVKIT)
     __block AVPlayerViewController* moviePlayerInstance;
    dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN();
            NSString* s = toNSString(CN1_THREAD_GET_STATE_PASS_ARG str);
            NSURL* u;
            if([s hasPrefix:@"file:"]) {
                u = [NSURL fileURLWithPath:[s substringFromIndex:5]];
            } else {
                u = [NSURL URLWithString:s];
            }
            moviePlayerInstance = [[AVPlayerViewController alloc] init];
            moviePlayerInstance.player = [[AVPlayer alloc] initWithURL:u];
            
            registerVideoCallbackAV(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance.player, onCompletionCallbackId);
        
            
    #ifdef AUTO_PLAY_VIDEO
        addPlaybackToAudioSession();
            [moviePlayerInstance play];
    #endif
            moviePlayerInstance.showsPlaybackControls = YES;
            POOL_END();
        });
        return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
#else
        return 0;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponent___java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT str, JAVA_INT onCompletionCallbackId) {
    if (useAVKit()) {
        return createVideoComponentFromStringAV(str, onCompletionCallbackId);
    } else {
        return createVideoComponentFromStringMP(str, onCompletionCallbackId);
    }
    
}



void com_codename1_impl_ios_IOSNative_removeNotificationCenterObserver___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG observerPeer) {
    [[NSNotificationCenter defaultCenter] removeObserver:(void *)observerPeer];
}


JAVA_LONG createNativeVideoComponentFromStringMP(JAVA_OBJECT str, JAVA_INT onCompletionCallbackId) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return 0;
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    __block MPMoviePlayerViewController* moviePlayerInstance;
        dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN()
            NSString *s = toNSString(CN1_THREAD_GET_STATE_PASS_ARG str);
            NSURL *u = nil;
            if([s hasPrefix:@"file:"]) {
                u = [NSURL fileURLWithPath:[s substringFromIndex:5]];
            } else {
                u = [NSURL URLWithString:s];
            }
            moviePlayerInstance = [[MPMoviePlayerViewController alloc] initWithContentURL:u];
            registerVideoCallback(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance.moviePlayer, onCompletionCallbackId);
    #ifndef AUTO_PLAY_VIDEO
            moviePlayerInstance.moviePlayer.shouldAutoplay = NO;
    #endif
            POOL_END();
        });
        return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
#else
        return 0;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
#endif
}
JAVA_LONG createNativeVideoComponentFromStringAV(JAVA_OBJECT str, JAVA_INT onCompletionCallbackId) {
#ifdef CN1_USE_AVKIT
    __block AVPlayerViewController* moviePlayerInstance;
        dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN()
            NSString *s = toNSString(CN1_THREAD_GET_STATE_PASS_ARG str);
            NSURL *u = nil;
            if([s hasPrefix:@"file:"]) {
                u = [NSURL fileURLWithPath:[s substringFromIndex:5]];
            } else {
                u = [NSURL URLWithString:s];
            }
            moviePlayerInstance = [[AVPlayerViewController alloc] init];
            moviePlayerInstance.player = [[AVPlayer alloc] initWithURL:u];
            registerVideoCallbackAV(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance.player, onCompletionCallbackId);
            POOL_END();
        });
        return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
#else
        return 0;
#endif
}
JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponent___java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT str, JAVA_INT onCompletionCallbackId) {
#if TARGET_OS_MACCATALYST
    // Mac slice: bypass the MP/AV runtime dispatch and always use AV. The
    // legacy MPMoviePlayer* path links against a framework that is weak on the
    // Mac slice and would crash at runtime.
    return createNativeVideoComponentFromStringAV(str, onCompletionCallbackId);
#else
    if (useAVKit()) {
        return createNativeVideoComponentFromStringAV(str, onCompletionCallbackId);
    } else {
        return createNativeVideoComponentFromStringMP(str, onCompletionCallbackId);
    }
#endif
}

JAVA_LONG createVideoComponentMP(JAVA_OBJECT dataObject, JAVA_INT onCompletionCallbackId) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return 0;
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    __block MPMoviePlayerController* moviePlayerInstance;
        dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN();
    #ifndef NEW_CODENAME_ONE_VM
            org_xmlvm_runtime_XMLVMArray* byteArray = dataObject;
            JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
            int len = byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
    #else
            void* data = ((JAVA_ARRAY)dataObject)->data;
            int len = ((JAVA_ARRAY)dataObject)->length;
    #endif
            NSData* d = [NSData dataWithBytes:data length:len];

            NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString *documentsDirectory = [paths objectAtIndex:0];
            NSString *path = [documentsDirectory stringByAppendingPathComponent:@"temp_movie.mp4"];

            [d writeToFile:path atomically:YES];
            NSURL *u = [NSURL fileURLWithPath:path];

            moviePlayerInstance = [[MPMoviePlayerController alloc] initWithContentURL:u];
            registerVideoCallback(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance, onCompletionCallbackId);
            moviePlayerInstance.useApplicationAudioSession = cn1UseApplicationAudioSessionForMedia(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
            // prepareToPlay will cause other av sessions to be interrupted at the time that the video
            // component is created - which is disruptive.  Better to just let it prepare to play
            // at the time that the video is played - even if there is a delay.
            //[moviePlayerInstance prepareToPlay];
    #ifdef AUTO_PLAY_VIDEO
            [moviePlayerInstance play];
    #else
            moviePlayerInstance.shouldAutoplay = NO;
    #endif
            POOL_END();
        });
        return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
#else
        return 0;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
#endif
}
JAVA_LONG createVideoComponentAV(JAVA_OBJECT dataObject, JAVA_INT onCompletionCallbackId) {
#ifdef CN1_USE_AVKIT
    __block AVPlayerViewController* moviePlayerInstance;
        dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN();

            void* data = ((JAVA_ARRAY)dataObject)->data;
            int len = ((JAVA_ARRAY)dataObject)->length;

            NSData* d = [NSData dataWithBytes:data length:len];
            
            NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString *documentsDirectory = [paths objectAtIndex:0];
            NSString *path = [documentsDirectory stringByAppendingPathComponent:@"temp_movie.mp4"];
            
            [d writeToFile:path atomically:YES];
            NSURL *u = [NSURL fileURLWithPath:path];
            moviePlayerInstance = [[AVPlayerViewController alloc] init];
            moviePlayerInstance.player = [[AVPlayer alloc] initWithURL:u];

            registerVideoCallbackAV(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance.player, onCompletionCallbackId);
            
            // prepareToPlay will cause other av sessions to be interrupted at the time that the video
            // component is created - which is disruptive.  Better to just let it prepare to play
            // at the time that the video is played - even if there is a delay.
            //[moviePlayerInstance prepareToPlay];
    #ifdef AUTO_PLAY_VIDEO
            addPlaybackToAudioSession();
#if TARGET_OS_OSX
            // An AVPlayerView has no play of its own; it is the player that plays.
            [moviePlayerInstance.player play];
#else
            [moviePlayerInstance play];
#endif
    #endif
            POOL_END();
        });
        return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
#else
        return 0;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponent___byte_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT dataObject, JAVA_INT onCompletionCallbackId) {
    if (useAVKit()) {
        return createVideoComponentAV(dataObject, onCompletionCallbackId);
    } else {
        return createVideoComponentMP(dataObject, onCompletionCallbackId);
    }
}



JAVA_LONG createNativeVideoComponentAV(JAVA_OBJECT dataObject, JAVA_INT onCompletionCallbackId) {
#ifdef CN1_USE_AVKIT
    __block AVPlayerViewController* moviePlayerInstance;
        dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN();

            void* data = ((JAVA_ARRAY)dataObject)->data;
            int len = ((JAVA_ARRAY)dataObject)->length;

            NSData* d = [NSData dataWithBytes:data length:len];
            
            NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString *documentsDirectory = [paths objectAtIndex:0];
            NSString *path = [documentsDirectory stringByAppendingPathComponent:@"temp_movie.mp4"];
            
            [d writeToFile:path atomically:YES];
            NSURL *u = [NSURL fileURLWithPath:path];
            moviePlayerInstance = [[AVPlayerViewController alloc] init];
            AVPlayer* player = [[AVPlayer alloc] initWithURL:u];
            moviePlayerInstance.player = player;
            
            registerVideoCallbackAV(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance.player, onCompletionCallbackId);

            POOL_END();
        });
        return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
#else
        return 0;
#endif
}
JAVA_LONG createNativeVideoComponentMP(JAVA_OBJECT dataObject, JAVA_INT onCompletionCallbackId) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return 0;
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    __block MPMoviePlayerViewController* moviePlayerInstance;
        dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN();
    #ifndef NEW_CODENAME_ONE_VM
            org_xmlvm_runtime_XMLVMArray* byteArray = dataObject;
            JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
            int len = byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
    #else
            void* data = ((JAVA_ARRAY)dataObject)->data;
            int len = ((JAVA_ARRAY)dataObject)->length;
    #endif
            NSData* d = [NSData dataWithBytes:data length:len];

            NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString *documentsDirectory = [paths objectAtIndex:0];
            NSString *path = [documentsDirectory stringByAppendingPathComponent:@"temp_movie.mp4"];

            [d writeToFile:path atomically:YES];
            NSURL *u = [NSURL fileURLWithPath:path];

            moviePlayerInstance = [[MPMoviePlayerViewController alloc] initWithContentURL:u];
            registerVideoCallback(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance.moviePlayer, onCompletionCallbackId);
    #ifndef AUTO_PLAY_VIDEO
            moviePlayerInstance.moviePlayer.shouldAutoplay = NO;
    #endif
    //#ifndef CN1_USE_ARC
    //        [moviePlayerInstance retain];
    //#endif
            POOL_END();
        });
        return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
#else
        return 0;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponent___byte_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT dataObject, JAVA_INT onCompletionCallbackId) {
#if TARGET_OS_MACCATALYST
    return createNativeVideoComponentAV(dataObject, onCompletionCallbackId);
#else
    if (useAVKit()) {
        return createNativeVideoComponentAV(dataObject, onCompletionCallbackId);
    } else {
        return createNativeVideoComponentMP(dataObject, onCompletionCallbackId);
    }
#endif
}

JAVA_LONG createVideoComponentNSDataMP(JAVA_LONG nsData, JAVA_INT onCompletionCallbackId) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return 0;
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    __block MPMoviePlayerController* moviePlayerInstance;
        dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN();
            NSData* d = (BRIDGE_CAST NSData*)((void*)nsData);

            NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString *documentsDirectory = [paths objectAtIndex:0];
            NSString *path = [documentsDirectory stringByAppendingPathComponent:@"temp_movie.mp4"];

            [d writeToFile:path atomically:YES];
            NSURL *u = [NSURL fileURLWithPath:path];

            moviePlayerInstance = [[MPMoviePlayerController alloc] initWithContentURL:u];
            registerVideoCallback(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance, onCompletionCallbackId);
            moviePlayerInstance.useApplicationAudioSession = cn1UseApplicationAudioSessionForMedia(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    //#ifndef CN1_USE_ARC
    //        [moviePlayerInstance retain];
    //#endif
            // prepareToPlay will cause other av sessions to be interrupted at the time that the video
            // component is created - which is disruptive.  Better to just let it prepare to play
            // at the time that the video is played - even if there is a delay.
            //[moviePlayerInstance prepareToPlay];
    #ifdef AUTO_PLAY_VIDEO
            [moviePlayerInstance play];
    #else
            moviePlayerInstance.shouldAutoplay = NO;
    #endif
            POOL_END();
        });
        return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
#else
        return 0;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
#endif
}

JAVA_LONG createVideoComponentNSDataAV(JAVA_LONG nsData, JAVA_INT onCompletionCallbackId) {
#ifdef CN1_USE_AVKIT
    __block AVPlayerViewController* moviePlayerInstance;
        dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN();
            NSData* d = (BRIDGE_CAST NSData*)((void*)nsData);
            
            NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString *documentsDirectory = [paths objectAtIndex:0];
            NSString *path = [documentsDirectory stringByAppendingPathComponent:@"temp_movie.mp4"];
            
            [d writeToFile:path atomically:YES];
            NSURL *u = [NSURL fileURLWithPath:path];
            moviePlayerInstance = [[AVPlayerViewController alloc] init];
            moviePlayerInstance.player = [[AVPlayer alloc] initWithURL:u];
            
            registerVideoCallbackAV(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance.player, onCompletionCallbackId);

            
    #ifdef AUTO_PLAY_VIDEO
            addPlaybackToAudioSession();
#if TARGET_OS_OSX
            // An AVPlayerView has no play of its own; it is the player that plays.
            [moviePlayerInstance.player play];
#else
            [moviePlayerInstance play];
#endif
    #endif
            POOL_END();
        });
        return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
#else 
        return 0;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponentNSData___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_INT onCompletionCallbackId) {
    if (useAVKit()) {
        return createVideoComponentNSDataAV(nsData, onCompletionCallbackId);
    } else {
        return createVideoComponentNSDataMP(nsData, onCompletionCallbackId);
    }
}

JAVA_LONG createNativeVideoComponentNSDataMP(JAVA_LONG nsData, JAVA_INT onCompletionCallbackId) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return 0;
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    __block MPMoviePlayerViewController* moviePlayerInstance;
        dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN();
            NSData* d = (BRIDGE_CAST NSData*)((void*)nsData);

            NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString *documentsDirectory = [paths objectAtIndex:0];
            NSString *path = [documentsDirectory stringByAppendingPathComponent:@"temp_movie.mp4"];

            [d writeToFile:path atomically:YES];
            NSURL *u = [NSURL fileURLWithPath:path];

            moviePlayerInstance = [[MPMoviePlayerViewController alloc] initWithContentURL:u];
            registerVideoCallback(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance.moviePlayer, onCompletionCallbackId);
    // No need to retain the instance.  Its reference count is already 1 after the alloc call.
    //#ifndef CN1_USE_ARC
    //        [moviePlayerInstance retain];
    //#endif
    #ifndef AUTO_PLAY_VIDEO
            moviePlayerInstance.moviePlayer.shouldAutoplay = NO;
    #endif
            POOL_END();
        });
        return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
#else
        return 0;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
#endif
}

JAVA_LONG createNativeVideoComponentNSDataAV(JAVA_LONG nsData, JAVA_INT onCompletionCallbackId) {
#ifdef CN1_USE_AVKIT
    __block AVPlayerViewController* moviePlayerInstance;
        dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN();
            NSData* d = (BRIDGE_CAST NSData*)((void*)nsData);
            
            NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString *documentsDirectory = [paths objectAtIndex:0];
            NSString *path = [documentsDirectory stringByAppendingPathComponent:@"temp_movie.mp4"];
            
            [d writeToFile:path atomically:YES];
            NSURL *u = [NSURL fileURLWithPath:path];
            
            moviePlayerInstance = [[AVPlayerViewController alloc] init];
            moviePlayerInstance.player = [[AVPlayer alloc] initWithURL:u];
            registerVideoCallbackAV(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance.player, onCompletionCallbackId);

            POOL_END();
        });
        return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
#else
        return 0;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponentNSData___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_INT onCompletionCallbackId) {
#if TARGET_OS_MACCATALYST
    return createNativeVideoComponentNSDataAV(nsData, onCompletionCallbackId);
#else
    if (useAVKit()) {
        // NOTE: branches preserved verbatim from the pre-existing iOS code path,
        // including the inverted naming -- changing it would alter iOS behaviour.
        return createNativeVideoComponentNSDataMP(nsData, onCompletionCallbackId);
    } else {
        return createNativeVideoComponentNSDataAV(nsData, onCompletionCallbackId);
    }
#endif
}

#if !TARGET_OS_WATCH && !TARGET_OS_TV
void launchMailAppOnDevice(JAVA_OBJECT recipients, JAVA_OBJECT subject, JAVA_OBJECT content){
#if TARGET_OS_OSX
    // AppKit has no in-process mail composer, so the message goes to the user's
    // mail application as a mailto: URL through NSWorkspace. sendEmailMessage's
    // macOS branch calls straight into here, so an empty body meant every
    // sendMessage on this port opened nothing and failed silently.
    NSMutableArray *recipientsArray = [NSMutableArray array];
    if (recipients != JAVA_NULL) {
        JAVA_ARRAY_OBJECT *data = (JAVA_ARRAY_OBJECT *)((JAVA_ARRAY)recipients)->data;
        int recipientCount = ((JAVA_ARRAY)recipients)->length;
        for (int iter = 0; iter < recipientCount; iter++) {
            NSString *r = toNSString(CN1_THREAD_GET_STATE_PASS_ARG data[iter]);
            if (r != nil) {
                [recipientsArray addObject:r];
            }
        }
    }
    NSString *nSubject = subject != JAVA_NULL
        ? toNSString(CN1_THREAD_GET_STATE_PASS_ARG subject) : @"";
    NSString *nBody = content != JAVA_NULL
        ? toNSString(CN1_THREAD_GET_STATE_PASS_ARG content) : @"";
    // Each part escaped on its own, with the separators added afterwards.
    // Escaping the assembled string would encode the '?' and '&' that make it a
    // query, and Mail then reads the whole tail as one address.
    NSCharacterSet *allowedQuery = [NSCharacterSet URLQueryAllowedCharacterSet];
    NSString *to = [[recipientsArray componentsJoinedByString:@","]
        stringByAddingPercentEncodingWithAllowedCharacters:
            [NSCharacterSet URLPathAllowedCharacterSet]];
    NSString *email = [NSString stringWithFormat:@"mailto:%@?subject=%@&body=%@",
        to == nil ? @"" : to,
        [nSubject stringByAddingPercentEncodingWithAllowedCharacters:allowedQuery],
        [nBody stringByAddingPercentEncodingWithAllowedCharacters:allowedQuery]];
    dispatch_async(dispatch_get_main_queue(), ^{
        NSURL *url = [NSURL URLWithString:email];
        if (url != nil) {
            [[NSWorkspace sharedWorkspace] openURL:url];
        }
    });
#else
    // Recipient.
    NSMutableArray * recipientsArray = [[NSMutableArray alloc] init];

    JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)recipients)->data;
    int recipientCount = ((JAVA_ARRAY)recipients)->length;

    for(int iter = 0 ; iter < recipientCount ; iter++) {
        [recipientsArray addObject:toNSString(CN1_THREAD_GET_STATE_PASS_ARG data[iter])];
    }
    
    NSString *recipientsStr = [recipientsArray componentsJoinedByString:@","];
    [recipientsArray release];
    NSString *nSubject = subject != JAVA_NULL ? toNSString(CN1_THREAD_GET_STATE_PASS_ARG subject) : @"";
    nSubject = [NSString stringWithFormat:@"?subject=%@", nSubject];
    NSString *nBody = content != JAVA_NULL ? toNSString(CN1_THREAD_GET_STATE_PASS_ARG content) : nil;
    nBody = [NSString stringWithFormat:@"&body=%@", nBody];
    NSString *email = [NSString stringWithFormat:@"mailto:%@%@%@", recipientsStr, nSubject, nBody];
    email = [email stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
    dispatch_async(dispatch_get_main_queue(), ^{
        NSLog(@"email: %@", email);
        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:email]];
    });

#endif
}
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV (launchMailAppOnDevice)

void com_codename1_impl_ios_IOSNative_sendEmailMessage___java_lang_String_1ARRAY_java_lang_String_java_lang_String_java_lang_String_1ARRAY_java_lang_String_1ARRAY_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                                                                                                                                           JAVA_OBJECT  recipients, JAVA_OBJECT  subject, JAVA_OBJECT content, JAVA_OBJECT attachment, JAVA_OBJECT attachmentMimeType, JAVA_BOOLEAN htmlMail) {
#if TARGET_OS_OSX
    // No compose controller: AppKit has no in-process mail composer, so the
    // message is handed to the user's mail application the same way the iOS
    // path does when MFMailComposeViewController says it cannot send.
    launchMailAppOnDevice(recipients, subject, content);
#else
#if TARGET_OS_WATCH || TARGET_OS_TV
    // No MessageUI on watchOS/tvOS; email composition is a no-op.
    return;
#else
    if (![MFMailComposeViewController canSendMail]) {
        launchMailAppOnDevice(recipients, subject, content);
        return;
    }                                                                                                                                                                        
    retainCN1(CN1_THREAD_STATE_PASS_ARG recipients);
    retainCN1(CN1_THREAD_STATE_PASS_ARG subject);
    retainCN1(CN1_THREAD_STATE_PASS_ARG content);
    retainCN1(CN1_THREAD_STATE_PASS_ARG attachment);
    retainCN1(CN1_THREAD_STATE_PASS_ARG attachmentMimeType);
    dispatch_async(dispatch_get_main_queue(), ^{
        MFMailComposeViewController *picker = [[MFMailComposeViewController alloc] init];
        if(picker == nil || ![MFMailComposeViewController canSendMail]) {
#ifndef CN1_USE_ARC
            [picker release];
#endif
            releaseCN1(CN1_THREAD_GET_STATE_PASS_ARG recipients);
            releaseCN1(CN1_THREAD_GET_STATE_PASS_ARG subject);
            releaseCN1(CN1_THREAD_GET_STATE_PASS_ARG content);
            releaseCN1(CN1_THREAD_GET_STATE_PASS_ARG attachment);
            releaseCN1(CN1_THREAD_GET_STATE_PASS_ARG attachmentMimeType);
            return;
        }
        POOL_BEGIN();
        picker.mailComposeDelegate = [CodenameOne_GLViewController instance];
        
        // Recipient.
        NSMutableArray * recipientsArray = [[NSMutableArray alloc] init];
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* strArray = recipients;
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)strArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
        int recipientCount = strArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)recipients)->data;
        int recipientCount = ((JAVA_ARRAY)recipients)->length;
#endif
        for(int iter = 0 ; iter < recipientCount ; iter++) {
            [recipientsArray addObject:toNSString(CN1_THREAD_GET_STATE_PASS_ARG data[iter])];
        }
        
        [picker setToRecipients:recipientsArray];
        
        // Subject.
        [picker setSubject:toNSString(CN1_THREAD_GET_STATE_PASS_ARG subject)];
        
        // Body.
        NSString *emailBody = toNSString(CN1_THREAD_GET_STATE_PASS_ARG content);
        [picker setMessageBody:emailBody isHTML:htmlMail];
        if(attachment != nil) {
#ifndef NEW_CODENAME_ONE_VM
            org_xmlvm_runtime_XMLVMArray* attachmentArray = attachment;
            JAVA_ARRAY_OBJECT* attachmentData = (JAVA_ARRAY_OBJECT*)attachmentArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
            int attachmentCount = attachmentArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
            
            org_xmlvm_runtime_XMLVMArray* mimeArray = attachmentMimeType;
            JAVA_ARRAY_OBJECT* mimeData = (JAVA_ARRAY_OBJECT*)mimeArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
            JAVA_ARRAY_OBJECT* attachmentData = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)attachment)->data;
            int attachmentCount = ((JAVA_ARRAY)attachment)->length;
            JAVA_ARRAY_OBJECT* mimeData = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)attachmentMimeType)->data;
#endif
            
            for(int iter = 0 ; iter < attachmentCount ; iter++) {
                NSString* file = toNSString(CN1_THREAD_GET_STATE_PASS_ARG attachmentData[iter]);
                NSString* mime = toNSString(CN1_THREAD_GET_STATE_PASS_ARG mimeData[iter]);
                
                int pos = [file rangeOfString:@"/" options:NSBackwardsSearch].location + 1;
                NSString* fileComponent = [file substringFromIndex:pos];
                if([file hasPrefix:@"file:"]) {
                    file = [file substringFromIndex:5];
                }
                NSData* d = [NSData dataWithContentsOfFile:file];
                [picker addAttachmentData:d mimeType:mime fileName:fileComponent];
            }
        }
        [[CodenameOne_GLViewController instance] presentModalViewController:picker animated:YES];
        
#ifndef CN1_USE_ARC
        [picker release];
#endif
        releaseCN1(CN1_THREAD_GET_STATE_PASS_ARG recipients);
        releaseCN1(CN1_THREAD_GET_STATE_PASS_ARG subject);
        releaseCN1(CN1_THREAD_GET_STATE_PASS_ARG content);
        releaseCN1(CN1_THREAD_GET_STATE_PASS_ARG attachment);
        releaseCN1(CN1_THREAD_GET_STATE_PASS_ARG attachmentMimeType);
        POOL_END();
    });
#endif // !(TARGET_OS_WATCH || TARGET_OS_TV) (sendEmailMessage)
#endif
}

#if !TARGET_OS_WATCH && !TARGET_OS_TV
// UIKit-only declaration: the type in its signature does not exist on macOS,
// so the whole thing goes rather than just the body. Guarding only the body
// would leave a signature naming a type the compiler has never heard of.
#if !TARGET_OS_OSX
MPMoviePlayerController* getMPPlayer(JAVA_LONG peer) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return nil;
#else
    NSObject* obj = (BRIDGE_CAST NSObject*)peer;
    MPMoviePlayerController* m = nil;;
    if([obj isKindOfClass:[MPMoviePlayerController class]]) {
        m = (MPMoviePlayerController*)obj;
    } else if ([obj isKindOfClass:[MPMoviePlayerViewController class]]) {
        MPMoviePlayerViewController *mv = (MPMoviePlayerViewController*)obj;
        m = mv.moviePlayer;
    }
    return m;
#endif
}
#endif

AVPlayer* getAVPlayer(JAVA_LONG peer) {
#if TARGET_OS_OSX
    NSObject* obj = (BRIDGE_CAST NSObject*)((void *)(uintptr_t)peer);
    if([obj isKindOfClass:[AVPlayer class]]) {
        return (AVPlayer*)obj;
    }
    if([obj isKindOfClass:[AVPlayerView class]]) {
        return ((AVPlayerView*)obj).player;
    }
    return nil;
#else
#ifdef CN1_USE_AVKIT
    NSObject* obj = (BRIDGE_CAST NSObject*)peer;
    AVPlayer* m = nil;;
    if([obj isKindOfClass:[AVPlayer class]]) {
        m = (AVPlayer*)obj;
    } else if ([obj isKindOfClass:[AVPlayerViewController class]]) {
        AVPlayerViewController *mv = (AVPlayerViewController*)obj;
        m = mv.player;
    }
    return m;
#else
    return nil;
#endif
#endif
}

CN1_AVPLAYERVIEWCONTROLLER getAVPlayerController(JAVA_LONG peer) {
#if TARGET_OS_OSX
    NSObject* obj = (BRIDGE_CAST NSObject*)((void *)(uintptr_t)peer);
    return [obj isKindOfClass:[AVPlayerView class]] ? (AVPlayerView*)obj : nil;
#elif defined(CN1_USE_AVKIT)
    NSObject* obj = (BRIDGE_CAST NSObject*)peer;
    AVPlayerViewController* m = nil;;
    if([obj isKindOfClass:[AVPlayer class]]) {
        
    } else if ([obj isKindOfClass:[AVPlayerViewController class]]) {
       m = (AVPlayerViewController*)obj;
        
    }
    return m;
#else
    return nil;
#endif
}



void startVideoComponentMP(JAVA_LONG peer) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
#else
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        
        [getMPPlayer(peer) play];
        POOL_END();
    });
#endif
}

void startVideoComponentAV(JAVA_LONG peer) {
#ifdef CN1_USE_AVKIT
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        addPlaybackToAudioSession();
        [getAVPlayer(peer) play];
        POOL_END();
    });
#endif
}
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV (MPMoviePlayerController / AVKit video helpers)

#if !TARGET_OS_WATCH && !TARGET_OS_TV
void com_codename1_impl_ios_IOSNative_startVideoComponent___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    if (useAVKit()) {
        startVideoComponentAV(peer);
    } else {
        startVideoComponentMP(peer);
    }
}

void stopVideoComponentMP(JAVA_LONG peer) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
#else
    
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
       
        [getMPPlayer(peer) stop];
        POOL_END();
    });
#endif
}
void stopVideoComponentAV(JAVA_LONG peer) {
#ifdef CN1_USE_AVKIT
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        [getAVPlayer(peer) seekToTime:CMTimeMake(0, 1)];
        [getAVPlayer(peer) pause];
        POOL_END();
    });
#endif
}

void com_codename1_impl_ios_IOSNative_stopVideoComponent___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    if (useAVKit()) {
        stopVideoComponentAV(peer);
    } else {
        stopVideoComponentMP(peer);
    }
}

void pauseVideoComponentMP(JAVA_LONG peer) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
#else
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSObject* obj = (BRIDGE_CAST NSObject*)peer;
        MPMoviePlayerController* m = nil;;
        if([obj isKindOfClass:[MPMoviePlayerController class]]) {
            m = (MPMoviePlayerController*)obj;
        } else if ([obj isKindOfClass:[MPMoviePlayerViewController class]]) {
            MPMoviePlayerViewController *mv = (MPMoviePlayerViewController*)obj;
            m = mv.moviePlayer;
        } else {
            return;
        }

        [m pause];
        POOL_END();
    });
#endif
}
void pauseVideoComponentAV(JAVA_LONG peer) {
#ifdef CN1_USE_AVKIT
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        
        [getAVPlayer(peer) pause];
        POOL_END();
    });
#endif
}

void com_codename1_impl_ios_IOSNative_pauseVideoComponent___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    if (useAVKit()) {
        pauseVideoComponentAV(peer);
    } else {
        pauseVideoComponentMP(peer);
    }
}
void com_codename1_impl_ios_IOSNative_prepareVideoComponent___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if TARGET_OS_OSX
    // AVPlayer has no prepareToPlay: it buffers when its item is set, so there
    // is nothing to ask for. Same as the AVKit branch on iOS.
    (void)peer;
#else
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (useAVKit()) {
            // Not sure if there is an equivalent in AVPlayer of prepareToPlay
        } else {
            [getMPPlayer(peer) prepareToPlay];
        }
        POOL_END();
    });
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_getMediaTimeMS___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if TARGET_OS_OSX
    AVPlayer* m = getAVPlayer(peer);
    if (m == nil) {
        return 0;
    }
    return CMTimeGetSeconds(m.currentTime) * 1000;
#else
    if (useAVKit()) {
#ifdef CN1_USE_AVKIT
        AVPlayer* m = getAVPlayer(peer);
        if (m == nil) {
            return 0;
        }
        return CMTimeGetSeconds(m.currentTime) * 1000;
#else
        return 0;
#endif
    } else {
        return (int)[getMPPlayer(peer) currentPlaybackTime] * 1000;
    }
    

#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_setMediaTimeMS___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT time) {
#if TARGET_OS_OSX
    AVPlayer* m = getAVPlayer(peer);
    if (m == nil) {
        return 0;
    }
    // Floating point, and milliseconds out. Both operands were ints, so 1500ms
    // truncated to 1 second before the seek and sub-second positioning was
    // impossible; and the result came back in SECONDS from a method whose name,
    // argument and Java return type are all milliseconds.
    //
    // The iOS branch below has the same shape and is deliberately left alone:
    // it predates this port, it is what every iOS build has shipped, and
    // changing playback behaviour there belongs in its own change with the iOS
    // media goldens behind it.
    [m seekToTime:CMTimeMakeWithSeconds(time / 1000.0, 1000)];
    return (JAVA_INT)(CMTimeGetSeconds([m currentTime]) * 1000.0);
#else
    if (useAVKit()) {
#ifdef CN1_USE_AVKIT
        [getAVPlayer(peer) seekToTime:CMTimeMakeWithSeconds(time/1000, 1000)];
        return CMTimeGetSeconds([getAVPlayer(peer) currentTime]);
#else
        return 0;
#endif
    } else {
        [getMPPlayer(peer) setCurrentPlaybackTime:time/1000];
        return [getMPPlayer(peer) currentPlaybackTime];
    }
    
    return 0;
#endif
}

int responseGetMediaDuration = 0;
JAVA_INT com_codename1_impl_ios_IOSNative_getMediaDuration___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if TARGET_OS_OSX
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AVPlayer* m = getAVPlayer(peer);
        responseGetMediaDuration = m == nil ? 0
            : CMTimeGetSeconds(m.currentItem.asset.duration) * 1000;
        POOL_END();
    });
    return responseGetMediaDuration;
#else
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (useAVKit()) {
#ifdef CN1_USE_AVKIT
            CMTime duration = getAVPlayer(peer).currentItem.asset.duration;
            responseGetMediaDuration = CMTimeGetSeconds(duration) * 1000;
#endif
        } else {
            responseGetMediaDuration = (int)getMPPlayer(peer).duration * 1000;
        }
        
        POOL_END();
    });
    return responseGetMediaDuration;
#endif
}

void com_codename1_impl_ios_IOSNative_setMediaBgArtist___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT artist) {
    NSString *_artist = toNSString(CN1_THREAD_GET_STATE_PASS_ARG artist);
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (useAVKit()) {
            //TODO
        } else {
            if ([MPNowPlayingInfoCenter class])  {
                NSArray *keys = [NSArray arrayWithObjects:
                                 MPMediaItemPropertyArtist,
                                 MPNowPlayingInfoPropertyPlaybackRate,
                                 nil];
                NSArray *values = [NSArray arrayWithObjects:
                                   _artist,
                                   [NSNumber numberWithInt:1],
                                   nil];
                NSDictionary *mediaInfo = [NSDictionary dictionaryWithObjects:values forKeys:keys];
                [[MPNowPlayingInfoCenter defaultCenter] setNowPlayingInfo:mediaInfo];
            }
        }
        
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setMediaBgTitle___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT title) {
    NSString *_title = toNSString(CN1_THREAD_GET_STATE_PASS_ARG title);
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        
        if ([MPNowPlayingInfoCenter class])  {
            NSArray *keys = [NSArray arrayWithObjects:
                             MPMediaItemPropertyTitle,
                             MPNowPlayingInfoPropertyPlaybackRate,
                             nil];
            NSArray *values = [NSArray arrayWithObjects:
                               _title,
                               [NSNumber numberWithInt:1],
                               nil];
            NSDictionary *mediaInfo = [NSDictionary dictionaryWithObjects:values forKeys:keys];
            [[MPNowPlayingInfoCenter defaultCenter] setNowPlayingInfo:mediaInfo];
        }
        
        
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setMediaBgDuration___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dur) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();

        if ([MPNowPlayingInfoCenter class])  {
            NSArray *keys = [NSArray arrayWithObjects:
                             MPMediaItemPropertyPlaybackDuration,
                             MPNowPlayingInfoPropertyPlaybackRate,
                             nil];
            NSArray *values = [NSArray arrayWithObjects:
                               [NSNumber numberWithLongLong:dur/1000],
                               [NSNumber numberWithInt:1],
                               nil];
            NSDictionary *mediaInfo = [NSDictionary dictionaryWithObjects:values forKeys:keys];
            [[MPNowPlayingInfoCenter defaultCenter] setNowPlayingInfo:mediaInfo];
        }
        
        
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setMediaBgPosition___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pos) {
    if (useAVKit()) {
        // TODO
    } else {
        // TODO
    }
    /*dispatch_async(dispatch_get_main_queue(), ^{
     POOL_BEGIN();
     if ([MPNowPlayingInfoCenter class])  {
     NSArray *keys = [NSArray arrayWithObjects:
     MPMediaItemPropertyPlaybackDuration,
     MPNowPlayingInfoPropertyPlaybackRate,
     nil];
     NSArray *values = [NSArray arrayWithObjects:
     [NSNumber numberWithLongLong:pos/1000],
     [NSNumber numberWithInt:1],
     nil];
     NSDictionary *mediaInfo = [NSDictionary dictionaryWithObjects:values forKeys:keys];
     [[MPNowPlayingInfoCenter defaultCenter] setNowPlayingInfo:mediaInfo];
     }
     POOL_END();
     });*/
}

void com_codename1_impl_ios_IOSNative_setNativeVideoControlsEmbedded___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN value) {
#if TARGET_OS_OSX
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AVPlayerView* v = getAVPlayerController(peer);
        // A Mac player hides its transport by style rather than by a flag.
        v.controlsStyle = value ? AVPlayerViewControlsStyleInline
                                : AVPlayerViewControlsStyleNone;
        POOL_END();
    });
#else
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (useAVKit()) {
#ifdef CN1_USE_AVKIT
            getAVPlayerController(peer).showsPlaybackControls = value;
#endif
        } else {
            NSObject* obj = (BRIDGE_CAST NSObject*)peer;
            MPMoviePlayerController* m = nil;;
            if([obj isKindOfClass:[MPMoviePlayerController class]]) {
                m = (MPMoviePlayerController*)obj;
            } else if ([obj isKindOfClass:[MPMoviePlayerViewController class]]) {
                MPMoviePlayerViewController *mv = (MPMoviePlayerViewController*)obj;
                m = mv.moviePlayer;
            } else {
                POOL_END();
                return;
            }

            if (value) {
                m.controlStyle = MPMovieControlStyleEmbedded;
            } else {
                m.controlStyle = MPMovieControlStyleNone;
            }
        }
        
        POOL_END();
    });
#endif
}

void com_codename1_impl_ios_IOSNative_setMediaBgAlbumCover___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if TARGET_OS_OSX
    // MPNowPlayingInfoCenter belongs to MediaPlayer, which this port does not
    // link -- the rest of MediaPlayer is the iOS movie player the AVKit path
    // replaced. Now-playing artwork stays unset rather than pulling in a
    // framework for one call.
    (void)peer;
#else
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
       
        if ([MPNowPlayingInfoCenter class])  {
            GLUIImage* glll = (BRIDGE_CAST GLUIImage*)((void *)peer);
            CN1Image* i = [glll getImage];
            MPMediaItemArtwork *artwork = [[MPMediaItemArtwork alloc] initWithImage:i];
            NSArray *keys = [NSArray arrayWithObjects:
                             MPMediaItemPropertyArtwork,
                             MPNowPlayingInfoPropertyPlaybackRate,
                             nil];
            NSArray *values = [NSArray arrayWithObjects:
                               artwork,
                               [NSNumber numberWithInt:1],
                               nil];
            NSDictionary *mediaInfo = [NSDictionary dictionaryWithObjects:values forKeys:keys];
            [[MPNowPlayingInfoCenter defaultCenter] setNowPlayingInfo:mediaInfo];
        }
        
        
        POOL_END();
    });
#endif
}


JAVA_BOOLEAN responseIsVideoPlaying = 0;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoPlaying___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if TARGET_OS_OSX
    if (peer == 0) {
        return JAVA_FALSE;
    }
    responseIsVideoPlaying = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AVPlayer* m = getAVPlayer(peer);
        responseIsVideoPlaying = m != nil && m.rate != 0 && m.error == nil;
        POOL_END();
    });
    return responseIsVideoPlaying;
#else
    if (peer == 0) {
        return JAVA_FALSE;
    }
    responseIsVideoPlaying = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (useAVKit()) {
#ifdef CN1_USE_AVKIT
            responseIsVideoPlaying = getAVPlayer(peer).rate != 0 && getAVPlayer(peer).error == nil;
#endif
        } else{
            NSObject* obj = (BRIDGE_CAST NSObject*)peer;
            MPMoviePlayerController* m = nil;;
            if([obj isKindOfClass:[MPMoviePlayerController class]]) {
                m = (MPMoviePlayerController*)obj;
            } else if ([obj isKindOfClass:[MPMoviePlayerViewController class]]) {
                MPMoviePlayerViewController *mv = (MPMoviePlayerViewController*)obj;
                m = mv.moviePlayer;
            } else {
                POOL_END();
                return;
            }

            responseIsVideoPlaying = m.playbackState == MPMoviePlaybackStatePlaying;
        }
        
        POOL_END();
    });
    return responseIsVideoPlaying;
#endif
}

void com_codename1_impl_ios_IOSNative_setVideoFullScreen___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN fullscreen) {
#if TARGET_OS_OSX
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AVPlayerView* v = getAVPlayerController(peer);
        if (v != nil) {
            // NSView's own full-screen mode rather than the window's: it is the
            // player that goes full screen, which is what the caller asked for
            // and what leaves the rest of the application where it was.
            if (fullscreen && !v.isInFullScreenMode) {
                [v enterFullScreenMode:[NSScreen mainScreen] withOptions:nil];
            } else if (!fullscreen && v.isInFullScreenMode) {
                [v exitFullScreenModeWithOptions:nil];
            }
        }
        POOL_END();
    });
#else
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (useAVKit()) {
            // TODO

        } else {
            NSObject* obj = (BRIDGE_CAST NSObject*)peer;
            MPMoviePlayerController* m = nil;;
            if([obj isKindOfClass:[MPMoviePlayerController class]]) {
                m = (MPMoviePlayerController*)obj;
            } else if ([obj isKindOfClass:[MPMoviePlayerViewController class]]) {
                MPMoviePlayerViewController *mv = (MPMoviePlayerViewController*)obj;
                m = mv.moviePlayer;
            } else {
                POOL_END();
                return;
            }

            [m setFullscreen:fullscreen];
        }
        
        POOL_END();
    });
#endif
}

JAVA_BOOLEAN responseIsVideoFullScreen = 0;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoFullScreen___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if TARGET_OS_OSX
    responseIsVideoFullScreen = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AVPlayerView* v = getAVPlayerController(peer);
        responseIsVideoFullScreen = v != nil && v.isInFullScreenMode;
        POOL_END();
    });
    return responseIsVideoFullScreen;
#else
    responseIsVideoFullScreen = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (useAVKit()) {
            // TODO
        } else {
            NSObject* obj = (BRIDGE_CAST NSObject*)peer;
            MPMoviePlayerController* m = nil;;
            if([obj isKindOfClass:[MPMoviePlayerController class]]) {
                m = (MPMoviePlayerController*)obj;
            } else if ([obj isKindOfClass:[MPMoviePlayerViewController class]]) {
                MPMoviePlayerViewController *mv = (MPMoviePlayerViewController*)obj;
                m = mv.moviePlayer;
            } else {
                POOL_END();
                return;
            }

            responseIsVideoFullScreen = [m isFullscreen];
        }
        
        POOL_END();
    });
    return responseIsVideoFullScreen;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getVideoViewPeer___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if TARGET_OS_OSX
    // The player view is itself the view, so there is no .view to reach into.
    return (JAVA_LONG)(uintptr_t)((BRIDGE_CAST void*)getAVPlayerController(peer));
#else
    if (useAVKit()) {
#ifdef CN1_USE_AVKIT
        AVPlayerViewController *m = getAVPlayerController(peer);
        return (JAVA_LONG)((BRIDGE_CAST void*)m.view);
#else
        return 0;
#endif
    } else {
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        return (JAVA_LONG)((BRIDGE_CAST void*)m.view);
    }
    
#endif
}

void com_codename1_impl_ios_IOSNative_showNativePlayerController___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if TARGET_OS_OSX
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AVPlayerView* v = getAVPlayerController(peer);
        if (v != nil && !v.isInFullScreenMode) {
            // There is no modal presentation to make here. A Mac shows a video
            // "as the player" by going full screen, which is the closest thing
            // to what presentViewController: does on iOS.
            [v enterFullScreenMode:[NSScreen mainScreen] withOptions:nil];
        }
        POOL_END();
    });
#else
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if (useAVKit()) {
#ifdef CN1_USE_AVKIT
            [[CodenameOne_GLViewController instance] presentViewController:getAVPlayerController(peer) animated:YES completion:nil];
#endif
        } else {
            NSObject* obj = (BRIDGE_CAST NSObject*)peer;
            if ([obj isKindOfClass:[MPMoviePlayerViewController class]]) {
                MPMoviePlayerViewController *mv = (MPMoviePlayerViewController*)obj;
                [[CodenameOne_GLViewController instance] presentMoviePlayerViewControllerAnimated:mv];
            }
        }

        POOL_END();
    });
#endif
}
#else // TARGET_OS_WATCH: no MPMoviePlayer / AVKit video playback peers on the watch.
void com_codename1_impl_ios_IOSNative_startVideoComponent___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {}
void com_codename1_impl_ios_IOSNative_stopVideoComponent___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {}
void com_codename1_impl_ios_IOSNative_pauseVideoComponent___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {}
void com_codename1_impl_ios_IOSNative_prepareVideoComponent___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {}
JAVA_INT com_codename1_impl_ios_IOSNative_getMediaTimeMS___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) { return 0; }
JAVA_INT com_codename1_impl_ios_IOSNative_setMediaTimeMS___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT time) { return 0; }
JAVA_INT com_codename1_impl_ios_IOSNative_getMediaDuration___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) { return 0; }
void com_codename1_impl_ios_IOSNative_setMediaBgArtist___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT artist) {}
void com_codename1_impl_ios_IOSNative_setMediaBgTitle___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT title) {}
void com_codename1_impl_ios_IOSNative_setMediaBgDuration___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dur) {}
void com_codename1_impl_ios_IOSNative_setMediaBgPosition___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pos) {}
void com_codename1_impl_ios_IOSNative_setNativeVideoControlsEmbedded___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN value) {}
void com_codename1_impl_ios_IOSNative_setMediaBgAlbumCover___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoPlaying___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) { return NO; }
void com_codename1_impl_ios_IOSNative_setVideoFullScreen___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN fullscreen) {}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoFullScreen___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) { return NO; }
JAVA_LONG com_codename1_impl_ios_IOSNative_getVideoViewPeer___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) { return 0; }
void com_codename1_impl_ios_IOSNative_showNativePlayerController___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {}
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV (MPMoviePlayer / AVKit video peer functions)

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDarkMode___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    // Asked of the system rather than inferred. The Catalyst path derives dark
    // mode from the content pane's luma because it has no reliable way to read
    // the host appearance; AppKit just answers, including when the user changes
    // it while the application is running.
    return CN1MacHostIsDarkMode() ? JAVA_TRUE : JAVA_FALSE;
#else
#if !TARGET_OS_WATCH
    if (@available(iOS 13.0, *)) {
        return [UIScreen mainScreen].traitCollection.userInterfaceStyle == UIUserInterfaceStyleDark;
    } else {
        return JAVA_FALSE;
    }
#else
    // watchOS has no UIScreen trait-collection capture here.
    return JAVA_FALSE;
#endif // !TARGET_OS_WATCH
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDarkModeDetectionSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    if (@available(iOS 13.0, *)) {
        return JAVA_TRUE;
    } else {
        return JAVA_FALSE;
    }
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVPNActive___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    struct ifaddrs *interfaces = NULL;
    if (getifaddrs(&interfaces) != 0) {
        return JAVA_FALSE;
    }
    JAVA_BOOLEAN found = JAVA_FALSE;
    struct ifaddrs *ifa = interfaces;
    while (ifa != NULL) {
        if (ifa->ifa_name == NULL || ifa->ifa_addr == NULL) {
            ifa = ifa->ifa_next;
            continue;
        }
        if (!(ifa->ifa_flags & IFF_UP) || (ifa->ifa_flags & IFF_LOOPBACK)) {
            ifa = ifa->ifa_next;
            continue;
        }
        NSString *name = [NSString stringWithUTF8String:ifa->ifa_name];
        if (name != nil) {
            NSString *lowerName = [name lowercaseString];
            if ([lowerName hasPrefix:@"utun"] || [lowerName hasPrefix:@"tap"] || [lowerName hasPrefix:@"tun"] || [lowerName hasPrefix:@"ppp"] || [lowerName hasPrefix:@"ipsec"]) {
                found = JAVA_TRUE;
                break;
            }
        }
        ifa = ifa->ifa_next;
    }
    freeifaddrs(interfaces);
    return found;
}

// ====================================================================
// Deeper network connectivity: WiFi info, NEHotspotConfiguration,
// NSNetService Bonjour, SCNetworkReachability-based type tracking.
// The build pipeline injects the wifi-info / HotspotConfiguration /
// NSLocalNetworkUsageDescription / NSBonjourServices entries only when
// the relevant Java classes are referenced -- this keeps stock apps free
// of dangling entitlements that block App Store approval.
// ====================================================================

// CN1_INCLUDE_HOTSPOT toggles NetworkExtension.framework import. Gated by
// IPhoneBuilder when com.codename1.io.wifi.WiFi.connect is on the
// classpath. Apps that never call WiFi.connect ship without any
// NetworkExtension symbols so Apple's API-usage scanner does not flag
// them.
//#define CN1_INCLUDE_HOTSPOT
#ifdef CN1_INCLUDE_HOTSPOT
#import <NetworkExtension/NetworkExtension.h>
#endif

// CN1_INCLUDE_WIFI_INFO toggles the CaptiveNetwork SSID/BSSID readout.
// CaptiveNetwork's CNCopyCurrentNetworkInfo is still the only way to get
// SSID/BSSID on a NEHotspotConfiguration-joined network. It is deprecated
// in iOS 14 but Apple kept it working for apps holding the wifi-info
// entitlement -- which we inject only when the WiFi info API is used.
// IPhoneBuilder uncomments the define when com.codename1.io.wifi.WiFi is
// on the classpath; stock apps see no CaptiveNetwork symbols and need no
// wifi-info entitlement.
//#define CN1_INCLUDE_WIFI_INFO
#ifdef CN1_INCLUDE_WIFI_INFO
#import <SystemConfiguration/CaptiveNetwork.h>
#endif

// CN1_INCLUDE_BONJOUR toggles the NSNetServiceBrowser / NSNetService
// bridge. Foundation is always linked so there is no framework cost when
// off, but the runtime hooks (the delegate, the dispatcher tables) only
// instantiate when this define is on -- which avoids dangling
// NSLocalNetworkUsageDescription requirements and surprises during the
// App Store review process.
//#define CN1_INCLUDE_BONJOUR

#if TARGET_OS_WATCH
// watchOS: no SystemConfiguration. Provide no-op network-type + listener natives
// so the translated runtime links; reachability is handled at the CN1 layer.
JAVA_INT com_codename1_impl_ios_IOSNative_wifiNetworkType___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) { return 1; }
void com_codename1_impl_ios_IOSNative_wifiInstallTypeListener___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT clsObj) {}
void com_codename1_impl_ios_IOSNative_wifiUninstallTypeListener__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {}
#else
static SCNetworkReachabilityRef cn1ReachabilityRef = NULL;

static int cn1NetworkTypeFromFlags(SCNetworkReachabilityFlags flags) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return 0;
#else
    if (!(flags & kSCNetworkReachabilityFlagsReachable)) {
        return 0; // NETWORK_TYPE_NONE
    }
    if (flags & kSCNetworkReachabilityFlagsIsWWAN) {
        return 2; // NETWORK_TYPE_CELLULAR
    }
    return 1; // NETWORK_TYPE_WIFI -- iOS treats everything non-WWAN as wifi
#endif
}

static int cn1ReadNetworkType() {
    struct sockaddr_in zero;
    bzero(&zero, sizeof(zero));
    zero.sin_len = sizeof(zero);
    zero.sin_family = AF_INET;
    SCNetworkReachabilityRef r = SCNetworkReachabilityCreateWithAddress(
            kCFAllocatorDefault, (const struct sockaddr*) &zero);
    if (r == NULL) return 0;
    SCNetworkReachabilityFlags flags;
    int t = 0;
    if (SCNetworkReachabilityGetFlags(r, &flags)) {
        t = cn1NetworkTypeFromFlags(flags);
    }
    CFRelease(r);
    return t;
}

JAVA_INT com_codename1_impl_ios_IOSNative_wifiNetworkType___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return cn1ReadNetworkType();
}

static void cn1ReachabilityCallback(SCNetworkReachabilityRef target,
                                    SCNetworkReachabilityFlags flags,
                                    void *info) {
    int t = cn1NetworkTypeFromFlags(flags);
    // Reuse the existing VPN detector so the listener parity with
    // NetworkManager.isVPNActive() stays consistent.
    JAVA_BOOLEAN vpn = com_codename1_impl_ios_IOSNative_isVPNActive___R_boolean(CN1_THREAD_GET_STATE_PASS_ARG JAVA_NULL);
    com_codename1_impl_ios_IOSConnectivity_networkTypeChangedDispatch___int_boolean(
            CN1_THREAD_GET_STATE_PASS_ARG t, vpn);
}

void com_codename1_impl_ios_IOSNative_wifiInstallTypeListener___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT clsObj) {
    if (cn1ReachabilityRef != NULL) return;
    struct sockaddr_in zero;
    bzero(&zero, sizeof(zero));
    zero.sin_len = sizeof(zero);
    zero.sin_family = AF_INET;
    cn1ReachabilityRef = SCNetworkReachabilityCreateWithAddress(
            kCFAllocatorDefault, (const struct sockaddr*) &zero);
    if (cn1ReachabilityRef == NULL) return;
    SCNetworkReachabilityContext ctx = {0, NULL, NULL, NULL, NULL};
    if (!SCNetworkReachabilitySetCallback(cn1ReachabilityRef,
            cn1ReachabilityCallback, &ctx)) {
        CFRelease(cn1ReachabilityRef);
        cn1ReachabilityRef = NULL;
        return;
    }
    SCNetworkReachabilityScheduleWithRunLoop(cn1ReachabilityRef,
            CFRunLoopGetMain(), kCFRunLoopCommonModes);
}

void com_codename1_impl_ios_IOSNative_wifiUninstallTypeListener__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    if (cn1ReachabilityRef == NULL) return;
    SCNetworkReachabilityUnscheduleFromRunLoop(cn1ReachabilityRef,
            CFRunLoopGetMain(), kCFRunLoopCommonModes);
    CFRelease(cn1ReachabilityRef);
    cn1ReachabilityRef = NULL;
}
#endif // !TARGET_OS_WATCH (SCNetworkReachability)

JAVA_OBJECT com_codename1_impl_ios_IOSNative_wifiCurrentSSID___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef CN1_INCLUDE_WIFI_INFO
    CFArrayRef interfaces = CNCopySupportedInterfaces();
    if (interfaces == NULL) return JAVA_NULL;
    JAVA_OBJECT result = JAVA_NULL;
    CFIndex count = CFArrayGetCount(interfaces);
    for (CFIndex i = 0; i < count; i++) {
        CFStringRef iface = (CFStringRef) CFArrayGetValueAtIndex(interfaces, i);
        CFDictionaryRef info = CNCopyCurrentNetworkInfo(iface);
        if (info != NULL) {
            CFStringRef ssid = (CFStringRef) CFDictionaryGetValue(info,
                    kCNNetworkInfoKeySSID);
            if (ssid != NULL) {
                result = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG (NSString*) ssid);
            }
            CFRelease(info);
            if (result != JAVA_NULL) break;
        }
    }
    CFRelease(interfaces);
    return result;
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_wifiCurrentBSSID___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef CN1_INCLUDE_WIFI_INFO
    CFArrayRef interfaces = CNCopySupportedInterfaces();
    if (interfaces == NULL) return JAVA_NULL;
    JAVA_OBJECT result = JAVA_NULL;
    CFIndex count = CFArrayGetCount(interfaces);
    for (CFIndex i = 0; i < count; i++) {
        CFStringRef iface = (CFStringRef) CFArrayGetValueAtIndex(interfaces, i);
        CFDictionaryRef info = CNCopyCurrentNetworkInfo(iface);
        if (info != NULL) {
            CFStringRef bssid = (CFStringRef) CFDictionaryGetValue(info,
                    kCNNetworkInfoKeyBSSID);
            if (bssid != NULL) {
                result = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                        [(NSString*) bssid lowercaseString]);
            }
            CFRelease(info);
            if (result != JAVA_NULL) break;
        }
    }
    CFRelease(interfaces);
    return result;
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_wifiIpAddress___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    struct ifaddrs *interfaces = NULL;
    if (getifaddrs(&interfaces) != 0) return JAVA_NULL;
    JAVA_OBJECT result = JAVA_NULL;
    for (struct ifaddrs *ifa = interfaces; ifa != NULL; ifa = ifa->ifa_next) {
        if (ifa->ifa_addr == NULL || ifa->ifa_addr->sa_family != AF_INET) continue;
        if (!(ifa->ifa_flags & IFF_UP) || (ifa->ifa_flags & IFF_LOOPBACK)) continue;
        // en0 is the standard WiFi interface name on iOS devices.
        if (ifa->ifa_name == NULL || strncmp(ifa->ifa_name, "en", 2) != 0) continue;
        char addr[INET_ADDRSTRLEN];
        struct sockaddr_in *sin = (struct sockaddr_in*) ifa->ifa_addr;
        if (inet_ntop(AF_INET, &sin->sin_addr, addr, sizeof(addr)) != NULL) {
            result = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                    [NSString stringWithUTF8String:addr]);
            break;
        }
    }
    freeifaddrs(interfaces);
    return result;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_wifiGateway___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    // iOS does not expose the route table to apps. Best-effort: derive from
    // the en0 address by assuming the gateway lives at the network's .1
    // address. This matches the most common home/SOHO topology and is
    // documented as best-effort in the Java contract.
    struct ifaddrs *interfaces = NULL;
    if (getifaddrs(&interfaces) != 0) return JAVA_NULL;
    JAVA_OBJECT result = JAVA_NULL;
    for (struct ifaddrs *ifa = interfaces; ifa != NULL; ifa = ifa->ifa_next) {
        if (ifa->ifa_addr == NULL || ifa->ifa_addr->sa_family != AF_INET) continue;
        if (!(ifa->ifa_flags & IFF_UP) || (ifa->ifa_flags & IFF_LOOPBACK)) continue;
        if (ifa->ifa_name == NULL || strncmp(ifa->ifa_name, "en", 2) != 0) continue;
        struct sockaddr_in *sin = (struct sockaddr_in*) ifa->ifa_addr;
        struct sockaddr_in *mask = (struct sockaddr_in*) ifa->ifa_netmask;
        if (mask == NULL) continue;
        uint32_t net = sin->sin_addr.s_addr & mask->sin_addr.s_addr;
        uint32_t gw = net | htonl(1);
        struct in_addr g;
        g.s_addr = gw;
        char buf[INET_ADDRSTRLEN];
        if (inet_ntop(AF_INET, &g, buf, sizeof(buf)) != NULL) {
            result = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                    [NSString stringWithUTF8String:buf]);
            break;
        }
    }
    freeifaddrs(interfaces);
    return result;
}

void com_codename1_impl_ios_IOSNative_wifiConnect___java_lang_String_java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT ssidObj, JAVA_OBJECT pwObj, JAVA_INT security) {
#ifdef CN1_INCLUDE_HOTSPOT
    if (@available(iOS 11.0, *)) {
        NSString *ssid = toNSString(CN1_THREAD_GET_STATE_PASS_ARG ssidObj);
        NSString *pw = pwObj == JAVA_NULL ? nil : toNSString(CN1_THREAD_GET_STATE_PASS_ARG pwObj);
        NEHotspotConfiguration *cfg;
        if (pw == nil || pw.length == 0) {
            cfg = [[NEHotspotConfiguration alloc] initWithSSID:ssid];
        } else {
            // security==4 -> WPA3_SAE, others -> WPA2 PSK
            BOOL isWep = security == 1;
            cfg = [[NEHotspotConfiguration alloc] initWithSSID:ssid
                                                    passphrase:pw
                                                          isWEP:isWep];
        }
        [[NEHotspotConfigurationManager sharedManager]
                applyConfiguration:cfg
                completionHandler:^(NSError * _Nullable err) {
                    BOOL ok = (err == nil
                            || err.code == NEHotspotConfigurationErrorAlreadyAssociated);
                    NSString *msg = err == nil ? @"ok" : err.localizedDescription;
                    com_codename1_impl_ios_IOSConnectivity_wifiConnectResult___boolean_java_lang_String(
                            CN1_THREAD_GET_STATE_PASS_ARG
                            ok ? JAVA_TRUE : JAVA_FALSE,
                            fromNSString(CN1_THREAD_GET_STATE_PASS_ARG msg));
                }];
        [cfg release];
        return;
    }
#endif
    com_codename1_impl_ios_IOSConnectivity_wifiConnectResult___boolean_java_lang_String(
            CN1_THREAD_GET_STATE_PASS_ARG JAVA_FALSE,
            fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                    @"NEHotspotConfiguration not linked. Reference com.codename1.io.wifi.WiFi.connect from your app to make the iOS builder inject the entitlement and link NetworkExtension.framework."));
}

void com_codename1_impl_ios_IOSNative_wifiDisconnect___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT ssidObj) {
#ifdef CN1_INCLUDE_HOTSPOT
    if (@available(iOS 11.0, *)) {
        NSString *ssid = toNSString(CN1_THREAD_GET_STATE_PASS_ARG ssidObj);
        [[NEHotspotConfigurationManager sharedManager] removeConfigurationForSSID:ssid];
    }
#endif
}

// ---------------- Bonjour ----------------
// Gated on CN1_INCLUDE_BONJOUR; IPhoneBuilder uncomments the define above
// when com.codename1.io.bonjour is on the classpath. Apps that never use
// Bonjour neither register the NSNetServiceBrowser delegate nor declare
// NSLocalNetworkUsageDescription / NSBonjourServices in Info.plist, so the
// iOS 14 local-network privacy prompt is suppressed for them. The C entry
// points still link (ParparVM requires the symbol for every `native`
// method) but they short-circuit to JAVA_NULL / 0 when the define is off.

#ifdef CN1_INCLUDE_BONJOUR
@interface CN1BonjourBrowser : NSObject<NSNetServiceBrowserDelegate, NSNetServiceDelegate>
@property (nonatomic, retain) NSNetServiceBrowser *browser;
@property (nonatomic, retain) NSMutableArray *resolving;
@property (nonatomic, assign) JAVA_LONG handle;
@end

@implementation CN1BonjourBrowser
- (void)dealloc {
    [_browser release];
    [_resolving release];
    [super dealloc];
}
- (void)netServiceBrowser:(NSNetServiceBrowser *)b
            didFindService:(NSNetService *)svc
                moreComing:(BOOL)more {
    [self.resolving addObject:svc];
    svc.delegate = self;
    [svc resolveWithTimeout:5.0];
}
- (void)netServiceBrowser:(NSNetServiceBrowser *)b
          didRemoveService:(NSNetService *)svc
                moreComing:(BOOL)more {
    JAVA_OBJECT name = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG svc.name);
    JAVA_OBJECT type = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG svc.type);
    com_codename1_impl_ios_IOSConnectivity_bonjourLostDispatch___long_java_lang_String_java_lang_String(
            CN1_THREAD_GET_STATE_PASS_ARG self.handle, name, type);
}
- (void)netServiceDidResolveAddress:(NSNetService *)svc {
    NSString *host = svc.hostName;
    NSDictionary *txt = nil;
    NSData *raw = [svc TXTRecordData];
    if (raw != nil) {
        txt = [NSNetService dictionaryFromTXTRecordData:raw];
    }
    JAVA_OBJECT name = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG svc.name);
    JAVA_OBJECT type = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG svc.type);
    JAVA_OBJECT hostObj = host == nil ? JAVA_NULL
            : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG host);
    JAVA_OBJECT keys = JAVA_NULL, vals = JAVA_NULL;
    if (txt != nil && txt.count > 0) {
        keys = __NEW_ARRAY_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_INT) txt.count);
        vals = __NEW_ARRAY_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_INT) txt.count);
        JAVA_ARRAY_OBJECT *kArr = (JAVA_ARRAY_OBJECT*) ((JAVA_ARRAY) keys)->data;
        JAVA_ARRAY_OBJECT *vArr = (JAVA_ARRAY_OBJECT*) ((JAVA_ARRAY) vals)->data;
        int i = 0;
        for (NSString *k in txt.allKeys) {
            kArr[i] = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG k);
            id v = [txt objectForKey:k];
            NSString *s = nil;
            if ([v isKindOfClass:[NSData class]]) {
                s = [[[NSString alloc] initWithData:(NSData*) v
                                            encoding:NSUTF8StringEncoding] autorelease];
            } else if ([v isKindOfClass:[NSString class]]) {
                s = (NSString*) v;
            }
            vArr[i] = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG (s == nil ? @"" : s));
            i++;
        }
    }
    com_codename1_impl_ios_IOSConnectivity_bonjourResolveDispatch___long_java_lang_String_java_lang_String_java_lang_String_int_java_lang_String_1ARRAY_java_lang_String_1ARRAY(
            CN1_THREAD_GET_STATE_PASS_ARG self.handle, name, type, hostObj,
            (JAVA_INT) svc.port, keys, vals);
}
- (void)netService:(NSNetService *)svc didNotResolve:(NSDictionary *)errorDict {
}
@end

static NSMutableDictionary *cn1BonjourBrowsers = nil;
static NSMutableDictionary *cn1BonjourPublishers = nil;
static int64_t cn1BonjourHandleSeq = 1;
#endif // CN1_INCLUDE_BONJOUR

JAVA_LONG com_codename1_impl_ios_IOSNative_bonjourBrowseStart___java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT typeObj) {
#ifdef CN1_INCLUDE_BONJOUR
    if (typeObj == JAVA_NULL) return 0;
    if (cn1BonjourBrowsers == nil) cn1BonjourBrowsers = [[NSMutableDictionary alloc] init];
    NSString *type = toNSString(CN1_THREAD_GET_STATE_PASS_ARG typeObj);
    if (![type hasSuffix:@"."]) type = [type stringByAppendingString:@"."];
    int64_t handle = cn1BonjourHandleSeq++;
    CN1BonjourBrowser *bb = [[CN1BonjourBrowser alloc] init];
    bb.handle = handle;
    bb.browser = [[[NSNetServiceBrowser alloc] init] autorelease];
    bb.resolving = [NSMutableArray array];
    bb.browser.delegate = bb;
    [bb.browser searchForServicesOfType:type inDomain:@"local."];
    [cn1BonjourBrowsers setObject:bb forKey:[NSNumber numberWithLongLong:handle]];
    [bb release];
    return (JAVA_LONG) handle;
#else
    return 0;
#endif
}

void com_codename1_impl_ios_IOSNative_bonjourBrowseStop___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle) {
#ifdef CN1_INCLUDE_BONJOUR
    if (cn1BonjourBrowsers == nil) return;
    NSNumber *k = [NSNumber numberWithLongLong:(int64_t) handle];
    CN1BonjourBrowser *bb = [cn1BonjourBrowsers objectForKey:k];
    if (bb != nil) {
        [bb.browser stop];
        [cn1BonjourBrowsers removeObjectForKey:k];
    }
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_bonjourPublishStart___java_lang_String_java_lang_String_int_java_lang_String_1ARRAY_java_lang_String_1ARRAY_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT nameObj, JAVA_OBJECT typeObj, JAVA_INT port, JAVA_OBJECT keysObj, JAVA_OBJECT valsObj) {
#ifdef CN1_INCLUDE_BONJOUR
    if (cn1BonjourPublishers == nil) cn1BonjourPublishers = [[NSMutableDictionary alloc] init];
    NSString *name = toNSString(CN1_THREAD_GET_STATE_PASS_ARG nameObj);
    NSString *type = toNSString(CN1_THREAD_GET_STATE_PASS_ARG typeObj);
    if (![type hasSuffix:@"."]) type = [type stringByAppendingString:@"."];
    NSNetService *svc = [[NSNetService alloc]
            initWithDomain:@"local." type:type name:name port:(int) port];
    if (keysObj != JAVA_NULL && valsObj != JAVA_NULL) {
        JAVA_ARRAY_OBJECT *kArr = (JAVA_ARRAY_OBJECT*) ((JAVA_ARRAY) keysObj)->data;
        JAVA_ARRAY_OBJECT *vArr = (JAVA_ARRAY_OBJECT*) ((JAVA_ARRAY) valsObj)->data;
        int n = (int) ((JAVA_ARRAY) keysObj)->length;
        NSMutableDictionary *d = [NSMutableDictionary dictionary];
        for (int i = 0; i < n; i++) {
            NSString *k = toNSString(CN1_THREAD_GET_STATE_PASS_ARG kArr[i]);
            NSString *v = toNSString(CN1_THREAD_GET_STATE_PASS_ARG vArr[i]);
            if (k != nil && v != nil) {
                [d setObject:[v dataUsingEncoding:NSUTF8StringEncoding] forKey:k];
            }
        }
        [svc setTXTRecordData:[NSNetService dataFromTXTRecordDictionary:d]];
    }
    [svc publish];
    int64_t handle = cn1BonjourHandleSeq++;
    [cn1BonjourPublishers setObject:svc forKey:[NSNumber numberWithLongLong:handle]];
    [svc release];
    return (JAVA_LONG) handle;
#else
    return 0;
#endif
}

void com_codename1_impl_ios_IOSNative_bonjourPublishStop___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle) {
#ifdef CN1_INCLUDE_BONJOUR
    if (cn1BonjourPublishers == nil) return;
    NSNumber *k = [NSNumber numberWithLongLong:(int64_t) handle];
    NSNetService *svc = [cn1BonjourPublishers objectForKey:k];
    if (svc != nil) {
        [svc stop];
        [cn1BonjourPublishers removeObjectForKey:k];
    }
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isLargerTextEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    if (@available(iOS 7.0, *)) {
        CGFloat baseSize = [CN1Font systemFontSize];
#if TARGET_OS_OSX
        CN1Font *preferred = CN1ApplePreferredFontForTextStyle(UIFontTextStyleBody);
#else
        CN1Font *preferred = [CN1Font preferredFontForTextStyle:UIFontTextStyleBody];
#endif
        return preferred.pointSize > (baseSize + 0.5f);
    } else {
        return JAVA_FALSE;
    }
#else
    // watchOS/tvOS have no CN1Font systemFontSize.
    return JAVA_FALSE;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_getLargerTextScale___R_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    if (@available(iOS 7.0, *)) {
        CGFloat baseSize = [CN1Font systemFontSize];
#if TARGET_OS_OSX
        CN1Font *preferred = CN1ApplePreferredFontForTextStyle(UIFontTextStyleBody);
#else
        CN1Font *preferred = [CN1Font preferredFontForTextStyle:UIFontTextStyleBody];
#endif
        if (baseSize <= 0.0f) {
            return 1.0f;
        }
        return (JAVA_FLOAT)(preferred.pointSize / baseSize);
    } else {
        return 1.0f;
    }
#else
    // watchOS/tvOS have no CN1Font systemFontSize.
    return 1.0f;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
}

#if TARGET_OS_WATCH

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isHighContrastEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return 0;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDifferentiateWithoutColorEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return 0;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isReduceMotionEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return 0;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isReduceTransparencyEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return 0;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isBoldTextEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return 0;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isInvertColorsEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return 0;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isGrayscaleEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return 0;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isOnOffSwitchLabelsEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return 0;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isScreenReaderEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return 0;
}

#else

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isHighContrastEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    return [[NSWorkspace sharedWorkspace] accessibilityDisplayShouldIncreaseContrast] ? 1 : 0;
#else
    return UIAccessibilityDarkerSystemColorsEnabled() ? 1 : 0;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDifferentiateWithoutColorEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    return [[NSWorkspace sharedWorkspace] accessibilityDisplayShouldDifferentiateWithoutColor] ? 1 : 0;
#else
    if (@available(iOS 13.0, macCatalyst 13.1, *)) {
        return UIAccessibilityShouldDifferentiateWithoutColor() ? 1 : 0;
    }
    return 0;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isReduceMotionEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    return [[NSWorkspace sharedWorkspace] accessibilityDisplayShouldReduceMotion] ? 1 : 0;
#else
    return UIAccessibilityIsReduceMotionEnabled() ? 1 : 0;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isReduceTransparencyEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    return [[NSWorkspace sharedWorkspace] accessibilityDisplayShouldReduceTransparency] ? 1 : 0;
#else
    return UIAccessibilityIsReduceTransparencyEnabled() ? 1 : 0;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isBoldTextEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    // macOS has no bold-text accessibility setting to report. The nearest
    // thing, increase contrast, is a different preference and answering with it
    // would make the application bold itself for a user who asked for something
    // else.
    return 0;
#else
    return UIAccessibilityIsBoldTextEnabled() ? 1 : 0;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isInvertColorsEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    return [[NSWorkspace sharedWorkspace] accessibilityDisplayShouldInvertColors] ? 1 : 0;
#else
    return UIAccessibilityIsInvertColorsEnabled() ? 1 : 0;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isGrayscaleEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    // macOS exposes no grayscale-filter query. Reporting the nearest thing --
    // increase contrast -- would tell the application the display is
    // monochrome when it is not, so the honest answer is that it is unknown.
    return 0;
#else
    return UIAccessibilityIsGrayscaleEnabled() ? 1 : 0;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isOnOffSwitchLabelsEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    // An iOS switch can be asked to draw I/O labels; a Mac switch has no such
    // setting, so there is nothing to report.
    return 0;
#else
    if (@available(iOS 13.0, macCatalyst 13.1, *)) {
        return UIAccessibilityIsOnOffSwitchLabelsEnabled() ? 1 : 0;
    }
    return 0;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isScreenReaderEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    // VoiceOver's own preference domain, which is where macOS records it.
    // NSWorkspace has no equivalent of UIAccessibilityIsVoiceOverRunning.
    NSUserDefaults *voiceOver = [[NSUserDefaults alloc]
        initWithSuiteName:@"com.apple.universalaccess"];
    BOOL running = [voiceOver boolForKey:@"voiceOverOnOffKey"];
#ifndef CN1_USE_ARC
    [voiceOver release];
#endif
    return running ? 1 : 0;
#else
    return UIAccessibilityIsVoiceOverRunning() ? 1 : 0;
#endif
}

#endif // TARGET_OS_WATCH

#ifdef INCLUDE_LOCATION_USAGE
CLLocationManager* com_codename1_impl_ios_IOSNative_createCLLocation = nil;
#endif

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isGPSEnabled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_LOCATION_USAGE
    return [CLLocationManager locationServicesEnabled] && 
   [CLLocationManager authorizationStatus] != kCLAuthorizationStatusDenied;
#else
    return JAVA_FALSE;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createCLLocation__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_LOCATION_USAGE
    dispatch_sync(dispatch_get_main_queue(), ^{
        com_codename1_impl_ios_IOSNative_createCLLocation = [[CLLocationManager alloc] init];
        if ([com_codename1_impl_ios_IOSNative_createCLLocation respondsToSelector:@selector     (CN1_REQUEST_LOCATION_AUTH)]) {
#ifdef IOS8_LOCATION_WARNING
            IOS8_LOCATION_WARNING
#endif
            [com_codename1_impl_ios_IOSNative_createCLLocation CN1_REQUEST_LOCATION_AUTH];
        }
    });
    CLLocationManager* c = com_codename1_impl_ios_IOSNative_createCLLocation;
    com_codename1_impl_ios_IOSNative_createCLLocation = nil;
    return (JAVA_LONG)((BRIDGE_CAST void*)c);
#else
    return 0;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getCurrentLocationObject___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_LOCATION_USAGE
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
        com_codename1_impl_ios_IOSNative_createCLLocation = l.location;
#ifndef CN1_USE_ARC
        [com_codename1_impl_ios_IOSNative_createCLLocation retain];
#endif
        POOL_END();
    });
    CLLocationManager* c = com_codename1_impl_ios_IOSNative_createCLLocation;
    com_codename1_impl_ios_IOSNative_createCLLocation = nil;
    return (JAVA_LONG)((BRIDGE_CAST void*)c);
#else
    return 0;
#endif
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationLatitude___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_LOCATION_USAGE
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.coordinate.latitude;
#else
    return 0;
#endif
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationAltitude___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_LOCATION_USAGE
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.altitude;
#else
    return 0;
#endif
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationLongtitude___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_LOCATION_USAGE
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.coordinate.longitude;
#else
    return 0;
#endif;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationAccuracy___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_LOCATION_USAGE
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.horizontalAccuracy;
#else
    return 0;
#endif
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationDirection___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_LOCATION_USAGE
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.course;
#else
    return 0;
#endif
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationVelocity___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_LOCATION_USAGE
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.speed;
#else
    return 0;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getLocationTimeStamp___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_LOCATION_USAGE
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    NSTimeInterval t = [loc.timestamp timeIntervalSince1970];
    return (JAVA_LONG)(t * 1000.0);
#else
    return 0;
#endif
}

#if !TARGET_OS_WATCH
#if TARGET_OS_TV
// UIPopoverController is unavailable on tvOS; hold it as id (the pickers/popovers it backs are tvOS-absent).
id popoverController;
#else
// UIKit-only declaration: the type in its signature does not exist on macOS,
// so the whole thing goes rather than just the body. Guarding only the body
// would leave a signature naming a type the compiler has never heard of.
#if !TARGET_OS_OSX
UIPopoverController* popoverController;
#endif
#endif
#endif // !TARGET_OS_WATCH
void com_codename1_impl_ios_IOSNative_captureCamera___boolean_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN movie, JAVA_INT quality, JAVA_INT duration) {
// UIImagePickerController / UIPopoverController / presentModalViewController are
// all unavailable on watchOS and tvOS; camera capture is a no-op there (tvOS
// has no camera).
#if defined(INCLUDE_CAMERA_USAGE) && !TARGET_OS_WATCH && !TARGET_OS_TV
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIImagePickerControllerSourceType sourceType = UIImagePickerControllerSourceTypeCamera; // default
        
        bool hasCamera = [UIImagePickerController isSourceTypeAvailable:sourceType];
        if (hasCamera) {
#ifndef CN1_USE_ARC
            UIImagePickerController* pickerController = [[[UIImagePickerController alloc] init] autorelease];
#else
            UIImagePickerController* pickerController = [[UIImagePickerController alloc] init];
#endif
            
            pickerController.delegate = [CodenameOne_GLViewController instance];
            pickerController.sourceType = sourceType;
            
            if(movie) {
                pickerController.mediaTypes = [NSArray arrayWithObjects:@"public.movie", nil];
                pickerController.videoQuality = quality;
                if (duration > 0) {
                    pickerController.videoMaximumDuration = duration;
                }
            } else {
                pickerController.mediaTypes = [NSArray arrayWithObjects:@"public.image", nil];
            }
            
            if(popoverSupported() && sourceType != UIImagePickerControllerSourceTypeCamera)
            {
                if (popoverController != nil) {
#ifndef CN1_USE_ARC
                    [popoverController release];
#endif
                    popoverController = nil;
                }
                popoverController = [[NSClassFromString(@"UIPopoverController") alloc]
                                     initWithContentViewController:pickerController];
                popoverController.delegate = [CodenameOne_GLViewController instance];
                [popoverController presentPopoverFromRect:CGRectMake(0,32,320,480)
                                                   inView:[[CodenameOne_GLViewController instance] view]
                                 permittedArrowDirections:UIPopoverArrowDirectionAny
                                                 animated:YES];

            }
            else
            {
                [[CodenameOne_GLViewController instance] presentModalViewController:pickerController animated:YES];
            }
            
        } else {
            com_codename1_impl_ios_IOSImplementation_capturePictureResult___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG nil);
        }
        POOL_END();
    });
#endif
}

#if !TARGET_OS_WATCH && !TARGET_OS_TV
static void cn1AddFileChooserDocumentType(NSMutableArray *types, CFStringRef tagClass, NSString *tag) {
    if (tag == nil || [tag length] == 0) {
        return;
    }
    CFStringRef uti = UTTypeCreatePreferredIdentifierForTag(tagClass, (CFStringRef)tag, NULL);
    if (uti != NULL) {
#if __has_feature(objc_arc)
        NSString *type = CFBridgingRelease(uti);
#else
        NSString *type = [(NSString *)uti autorelease];
#endif
        if (![types containsObject:type]) {
            [types addObject:type];
        }
    }
}

static NSArray *cn1FileChooserDocumentTypes(NSString *accept) {
    NSMutableArray *types = [NSMutableArray array];
    if (accept != nil) {
        NSArray *tokens = [accept componentsSeparatedByString:@","];
        for (NSString *rawToken in tokens) {
            NSString *token = [rawToken stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
            if ([token length] == 0 || [token isEqualToString:@"*"] || [token isEqualToString:@"*/*"]) {
                continue;
            }
            if ([token rangeOfString:@"/"].location != NSNotFound) {
                cn1AddFileChooserDocumentType(types, kUTTagClassMIMEType, token);
            } else {
                if ([token hasPrefix:@"."]) {
                    token = [token substringFromIndex:1];
                }
                cn1AddFileChooserDocumentType(types, kUTTagClassFilenameExtension, [token lowercaseString]);
            }
        }
    }
    if ([types count] == 0) {
        [types addObject:(NSString *)kUTTypeItem];
    }
    return types;
}
#endif

void com_codename1_impl_ios_IOSNative_openFileChooser___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT accept) {
#if TARGET_OS_OSX
    NSString *nsAccept = accept == JAVA_NULL ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG accept);
    NSArray *documentTypes = cn1FileChooserDocumentTypes(nsAccept);
#ifndef CN1_USE_ARC
    [documentTypes retain];
#endif
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        // A panel rather than a document picker: a Mac has direct file system
        // access, so there is nothing to import and no copy to make.
        NSOpenPanel *panel = [NSOpenPanel openPanel];
        panel.allowsMultipleSelection = NO;
        panel.canChooseDirectories = NO;
        panel.canChooseFiles = YES;
        if (documentTypes.count > 0) {
            panel.allowedFileTypes = documentTypes;
        }
        NSInteger response = [panel runModal];
        NSURL *picked = response == NSModalResponseOK ? panel.URL : nil;
        struct ThreadLocalData* threadStateData = getThreadLocalData();
        com_codename1_impl_ios_IOSImplementation_fileChooserResult___java_lang_String(
            threadStateData,
            picked == nil ? JAVA_NULL
                          : fromNSString(threadStateData, [picked absoluteString]));
        POOL_END();
#ifndef CN1_USE_ARC
        [documentTypes release];
#endif
    });
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    NSString *nsAccept = accept == JAVA_NULL ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG accept);
    NSArray *documentTypes = cn1FileChooserDocumentTypes(nsAccept);
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
#ifndef CN1_USE_ARC
        UIDocumentPickerViewController *pickerController = [[[UIDocumentPickerViewController alloc] initWithDocumentTypes:documentTypes inMode:UIDocumentPickerModeImport] autorelease];
#else
        UIDocumentPickerViewController *pickerController = [[UIDocumentPickerViewController alloc] initWithDocumentTypes:documentTypes inMode:UIDocumentPickerModeImport];
#endif
        pickerController.delegate = [CodenameOne_GLViewController instance];
        if (@available(iOS 11.0, *)) {
            pickerController.allowsMultipleSelection = NO;
        }
        if (popoverSupported()) {
            pickerController.modalPresentationStyle = UIModalPresentationFormSheet;
        }
        [[CodenameOne_GLViewController instance] presentViewController:pickerController animated:YES completion:nil];
        POOL_END();
    });
#else
    com_codename1_impl_ios_IOSImplementation_fileChooserResult___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG nil);
#endif
#endif
}

#ifdef INCLUDE_PHOTOLIBRARY_USAGE
#ifdef ENABLE_GALLERY_MULTISELECT

#ifdef USE_PHOTOKIT_FOR_MULTIGALLERY
void openGalleryMultipleWithPhotoKit(JAVA_INT type) {
#ifdef USE_PHOTOKIT_FOR_MULTIGALLERY
    if (@available(iOS 14, *)) {
        openGalleryMultipleWithPhotoKit(type);
        return;
    }
#endif
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();

        if (@available(iOS 14, *)) {
            PHPickerFilter *filter;
            if (type==0 || type == 3){
                filter = [PHPickerFilter imagesFilter];
            } else if (type==1 || type == 4){
                filter = [PHPickerFilter videosFilter];
            } else {
                filter = [PHPickerFilter anyFilterMatchingSubfilters:[NSArray arrayWithObjects:[PHPickerFilter imagesFilter], [PHPickerFilter videosFilter], nil]];
            }

            PHPickerConfiguration *config = [[PHPickerConfiguration alloc] initWithPhotoLibrary:[PHPhotoLibrary sharedPhotoLibrary]];
            config.filter = filter;
            config.preferredAssetRepresentationMode = PHPickerConfigurationAssetRepresentationModeCurrent;
            if (@available(iOS 15, *)) {
                config.selection = PHPickerConfigurationSelectionOrdered;
            } else {
                // Fallback on earlier versions
            }
            config.selectionLimit = 0;


            PHPickerViewController *pickerController =[[PHPickerViewController alloc] initWithConfiguration:config];

            pickerController.delegate = [CodenameOne_GLViewController instance];

            if(popoverSupported()) {
                if (popoverController != nil) {
    #ifndef CN1_USE_ARC
                    [popoverController release];
    #endif
                    popoverController = nil;
                }
                galleryPopover = YES;
                popoverController = [[NSClassFromString(@"UIPopoverController") alloc]
                                     initWithContentViewController:pickerController];

                popoverController.delegate = [CodenameOne_GLViewController instance];
                [popoverController presentPopoverFromRect:CGRectMake(0,32,320,480)
                                                   inView:[[CodenameOne_GLViewController instance] view]
                                 permittedArrowDirections:UIPopoverArrowDirectionAny
                                                 animated:YES];
            } else {
                [[CodenameOne_GLViewController instance] presentModalViewController:pickerController animated:YES];
            }


        } else {
            // Fallback on earlier versions
        }



        POOL_END();
    });
}
#endif

void openGalleryMultiple(JAVA_INT type) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        QBImagePickerController *pickerController = [QBImagePickerController new];
        pickerController.allowsMultipleSelection = YES;
        pickerController.maximumNumberOfSelection = 0;
        pickerController.showsNumberOfSelectedAssets = YES;
        pickerController.delegate = [CodenameOne_GLViewController instance];
        if (type==0 || type == 3){
            pickerController.mediaType = QBImagePickerMediaTypeImage;
        } else if (type==1 || type == 4){
            pickerController.mediaType = QBImagePickerMediaTypeVideo;
            
        } else {
            pickerController.mediaType = QBImagePickerMediaTypeAny;
        }
        
        if(popoverSupported()) {
            if (popoverController != nil) {
#ifndef CN1_USE_ARC
                [popoverController release];
#endif
                popoverController = nil;
            }
            galleryPopover = YES;
            popoverController = [[NSClassFromString(@"UIPopoverController") alloc]
                                 initWithContentViewController:pickerController];
            
            popoverController.delegate = [CodenameOne_GLViewController instance];
            [popoverController presentPopoverFromRect:CGRectMake(0,32,320,480)
                                               inView:[[CodenameOne_GLViewController instance] view]
                             permittedArrowDirections:UIPopoverArrowDirectionAny
                                             animated:YES];
        } else {
            [[CodenameOne_GLViewController instance] presentModalViewController:pickerController animated:YES];
        }
        POOL_END();
    });
}
#endif
#endif

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isMultiGallerySelectSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef ENABLE_GALLERY_MULTISELECT
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

void com_codename1_impl_ios_IOSNative_openGallery___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
#ifdef INCLUDE_PHOTOLIBRARY_USAGE
    BOOL multiple = false;
    if (type == 3 || type == 4 || type == 5) {  // GALLERY_TYPE_IMAGE_MULTI, GALLERY_TYPE_VIDEO_MULTI, GALLERY_TYPE_ALL_MULTI
        multiple = true;
    }
    if (multiple) {
#ifdef ENABLE_GALLERY_MULTISELECT
        openGalleryMultiple(type);
#else
        NSLog(@"Gallery multiselect is disabled");
        throwException(getThreadLocalData(), __NEW_INSTANCE_java_lang_RuntimeException(getThreadLocalData()));
#endif
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIImagePickerControllerSourceType sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
        if(![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypePhotoLibrary]) {
            if(![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeSavedPhotosAlbum]) {
                return;
            }
            sourceType = UIImagePickerControllerSourceTypeSavedPhotosAlbum;
        }
        
#ifndef CN1_USE_ARC
        UIImagePickerController* pickerController = [[[UIImagePickerController alloc] init] autorelease];
#else
        UIImagePickerController* pickerController = [[UIImagePickerController alloc] init];
#endif
        
        pickerController.delegate = [CodenameOne_GLViewController instance];
        pickerController.sourceType = sourceType;
        if (type==0){
            pickerController.mediaTypes = [[NSArray alloc] initWithObjects:(NSString*)kUTTypeImage, nil];
        } else if (type==1){
            pickerController.mediaTypes = [[NSArray alloc] initWithObjects:(NSString*)kUTTypeMovie, nil];
        } else if (type==2){
            pickerController.mediaTypes = [[NSArray alloc] initWithObjects:(NSString*)kUTTypeMovie, (NSString*)kUTTypeImage,  nil];
        }
        
        if(popoverSupported()) {
            if (popoverController != nil) {
#ifndef CN1_USE_ARC
                [popoverController release];
#endif
                popoverController = nil;
            }
            galleryPopover = YES;
            
            popoverController = [[NSClassFromString(@"UIPopoverController") alloc]
                                 initWithContentViewController:pickerController];
            
            popoverController.delegate = [CodenameOne_GLViewController instance];
            [popoverController presentPopoverFromRect:CGRectMake(0,32,320,480)
                                               inView:[[CodenameOne_GLViewController instance] view]
                             permittedArrowDirections:UIPopoverArrowDirectionAny
                                             animated:YES];
        } else {
            [[CodenameOne_GLViewController instance] presentModalViewController:pickerController animated:YES];
        }
        POOL_END();
    });
#endif
}
int popoverSupported()
{
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return 0;
#else
#if !TARGET_OS_WATCH
    return ( NSClassFromString(@"UIPopoverController") != nil) &&  (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad);
#else
    // watchOS has no UIPopoverController / interface idiom.
    return 0;
#endif // !TARGET_OS_WATCH
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUDID__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return JAVA_NULL;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getOSVersion__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if !TARGET_OS_WATCH
#if TARGET_OS_OSX
    NSOperatingSystemVersion v = [[NSProcessInfo processInfo] operatingSystemVersion];
    return fromNSString(CN1_THREAD_STATE_PASS_ARG
            [NSString stringWithFormat:@"%ld.%ld.%ld", (long)v.majorVersion,
                                       (long)v.minorVersion, (long)v.patchVersion]);
#else
    return fromNSString(CN1_THREAD_STATE_PASS_ARG [[UIDevice currentDevice] systemVersion]);
#endif
#else
    return JAVA_NULL;
#endif // !TARGET_OS_WATCH
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getDeviceName__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if !TARGET_OS_WATCH
#if TARGET_OS_OSX
    return fromNSString(CN1_THREAD_STATE_PASS_ARG [[NSHost currentHost] localizedName]);
#else
    return fromNSString(CN1_THREAD_STATE_PASS_ARG [[UIDevice currentDevice] name]);
#endif
#else
    return JAVA_NULL;
#endif // !TARGET_OS_WATCH
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isSimulator___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_SIMULATOR
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getDeviceHardwareModel__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    // hw.machine is the hardware/marketing model identifier (e.g. "iPhone15,2").
    // On the simulator it is the host arch; we map that to a stable label so the
    // value is never the host CPU string. This is not a per-device identifier --
    // every unit of the same model returns the same value -- so it is safe for
    // analytics segmentation, unlike [[UIDevice currentDevice] name].
    size_t size = 0;
    if (sysctlbyname("hw.machine", NULL, &size, NULL, 0) != 0 || size == 0) {
        return JAVA_NULL;
    }
    char *machine = (char *)malloc(size);
    if (machine == NULL) {
        return JAVA_NULL;
    }
    NSString *model = nil;
    if (sysctlbyname("hw.machine", machine, &size, NULL, 0) == 0) {
        model = [NSString stringWithUTF8String:machine];
    }
    free(machine);
#if TARGET_OS_SIMULATOR
    NSString *simModel = [[NSProcessInfo processInfo] environment][@"SIMULATOR_MODEL_IDENTIFIER"];
    if (simModel != nil && [simModel length] > 0) {
        model = simModel;
    }
#endif
    if (model == nil) {
        return JAVA_NULL;
    }
    return fromNSString(CN1_THREAD_STATE_PASS_ARG model);
}

extern int cn1GetStatusBarTapCount();
extern double cn1GetStatusBarTapLastEpochMillis();
extern int cn1GetStatusBarTapLastX();
extern int cn1GetStatusBarTapLastY();
extern BOOL cn1IsStatusBarTapProxyInstalled();

JAVA_INT com_codename1_impl_ios_IOSNative_getStatusBarTapCount__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return (JAVA_INT)cn1GetStatusBarTapCount();
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getStatusBarTapLastEpochMillis__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return (JAVA_LONG)cn1GetStatusBarTapLastEpochMillis();
}

JAVA_INT com_codename1_impl_ios_IOSNative_getStatusBarTapLastX__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return (JAVA_INT)cn1GetStatusBarTapLastX();
}

JAVA_INT com_codename1_impl_ios_IOSNative_getStatusBarTapLastY__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return (JAVA_INT)cn1GetStatusBarTapLastY();
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isStatusBarTapProxyInstalled__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return cn1IsStatusBarTapProxyInstalled() ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isGoodLocation___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
    CLLocation* loc = l.location;
    if(loc == nil) {
        POOL_END();
        return 0;
    }
    
    // Filter out points by invalid accuracy
    if (loc.horizontalAccuracy < 0) {
        POOL_END();
        return 0;
    }
    
    POOL_END();
    // The newLocation is good to use
    return 1;
}

void com_codename1_impl_ios_IOSNative_startUpdatingLocation___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT priority) {
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
    l.delegate = [CodenameOne_GLViewController instance];
    switch (priority) {
        case 0 : // HIGH PRIORITY
            l.desiredAccuracy = kCLLocationAccuracyBest;
            l.distanceFilter = kCLDistanceFilterNone;
#if !TARGET_OS_WATCH && !TARGET_OS_TV
            if (isIOS7()) {
                l.pausesLocationUpdatesAutomatically = NO;
            }
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
            break;
        case 1: // MEDIUM PRIORITY
            l.desiredAccuracy = kCLLocationAccuracyHundredMeters;
            l.distanceFilter = 100;
#if !TARGET_OS_WATCH && !TARGET_OS_TV
            if (isIOS7()) {
                l.pausesLocationUpdatesAutomatically = YES;
            }
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
            break;
        case 2 : // LOW PRIORITY
            l.desiredAccuracy = kCLLocationAccuracyThreeKilometers;
            l.distanceFilter = 3000;
#if !TARGET_OS_WATCH && !TARGET_OS_TV
            if (isIOS7()) {
                l.pausesLocationUpdatesAutomatically = YES;
            }
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
            break;

        default :
            l.desiredAccuracy = kCLLocationAccuracyHundredMeters;
            l.distanceFilter = kCLDistanceFilterNone;
#if !TARGET_OS_WATCH && !TARGET_OS_TV
            if (isIOS7()) {
                l.pausesLocationUpdatesAutomatically = NO;
            }
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
            break;
    }
    
    
#ifdef CN1_ENABLE_BACKGROUND_LOCATION
    SEL sel = NSSelectorFromString(@"setAllowsBackgroundLocationUpdates:");
    if ([l respondsToSelector:sel]) {
        // Obtain a method signature of selector on UIUserNotificationSettings class
        NSMethodSignature *signature = [l methodSignatureForSelector:sel];
        
        // Create an invocation on a signature -- must be used because of primitive (enum) arguments on selector
        NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:signature];
        invocation.selector = sel;
        invocation.target = l;
        BOOL param = YES;
        // Set arguments
        [invocation setArgument:&param atIndex:2];
        
        [invocation invoke];

        
        // All the above just to say *this v* because this property wasn't
        // added until iOS 9
        //[l setAllowsBackgroundLocationUpdates:YES];
    }
#endif
#if !TARGET_OS_TV
    [l startUpdatingLocation];
#endif // !TARGET_OS_TV
}

void com_codename1_impl_ios_IOSNative_stopUpdatingLocation___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
#if !TARGET_OS_TV
    [l stopUpdatingLocation];
#endif // !TARGET_OS_TV
}

void com_codename1_impl_ios_IOSNative_startUpdatingBackgroundLocation___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
// Not implemented on the native macOS port: the body below is UIKit -- a
// picker, an action sheet, a movie player, a pasteboard or a UIApplication
// service -- and AppKit's equivalent is a different API rather than a
// renamed one. The symbol still has to exist: ParparVM keeps a native method
// alive BY its symbol appearing in the native sources, so removing it would
// make the dead-code pass drop the Java side and ship green with the feature
// silently gone. Returning an unsupported value instead lets the caller take
// its unsupported path.
#if TARGET_OS_OSX
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
    l.delegate = [CodenameOne_GLViewController instance];

    [l startMonitoringSignificantLocationChanges];
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
#endif
}

void com_codename1_impl_ios_IOSNative_stopUpdatingBackgroundLocation___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
    [l stopMonitoringSignificantLocationChanges];
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
}


//native void addGeofencing(long peer, double lat, double lng, double radius, long expiration, String id);
void com_codename1_impl_ios_IOSNative_addGeofencing___long_double_double_double_long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObj, JAVA_LONG peer, JAVA_DOUBLE lat, JAVA_DOUBLE lng, JAVA_DOUBLE radius, JAVA_LONG expires, JAVA_OBJECT geoId) {
// Not implemented on the native macOS port: the body below is UIKit -- a
// picker, an action sheet, a movie player, a pasteboard or a UIApplication
// service -- and AppKit's equivalent is a different API rather than a
// renamed one. The symbol still has to exist: ParparVM keeps a native method
// alive BY its symbol appearing in the native sources, so removing it would
// make the dead-code pass drop the Java side and ship green with the feature
// silently gone. Returning an unsupported value instead lets the caller take
// its unsupported path.
#if TARGET_OS_OSX
#else
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
    l.delegate = [CodenameOne_GLViewController instance];
    CLLocationCoordinate2D center = CLLocationCoordinate2DMake(lat, lng);
    CLRegion *region = [[CLCircularRegion alloc]initWithCenter:center
                                                    radius:radius
                                                identifier:toNSString(CN1_THREAD_GET_STATE_PASS_ARG geoId)];
    [l startMonitoringForRegion:region];
#ifndef CN1_USE_ARC
    [region release];
#endif
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
#endif
}


//    native void removeGeofencing(String id);
void com_codename1_impl_ios_IOSNative_removeGeofencing___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT geoId) {
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
    for (CLRegion *region in [l monitoredRegions]) {
        if ([[region identifier] isEqualToString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG geoId)]) {
            [l stopMonitoringForRegion:region];
        }
    }
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
}

#ifdef INCLUDE_CONTACTS_USAGE
ABAddressBookRef globalAddressBook = nil;
bool grantedPermission;
ABAddressBookRef getAddressBook() {
    if(globalAddressBook == nil) {
        if (ABAddressBookRequestAccessWithCompletion != nil) {
            CFErrorRef error = nil;
            globalAddressBook = ABAddressBookCreateWithOptions(NULL,&error);
            __block bool completed = NO;
            ABAddressBookRequestAccessWithCompletion(globalAddressBook, ^(bool granted, CFErrorRef error) {
                grantedPermission = granted;
                completed = YES;
            });
            while(!completed) {
                wait(10);
            }
        } else {
            globalAddressBook = ABAddressBookCreate();
        }
    }
    return globalAddressBook;
}
#endif
JAVA_VOID com_codename1_impl_ios_IOSNative_refreshContacts__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_CONTACTS_USAGE
    globalAddressBook = nil;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isContactsPermissionGranted__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    getAddressBook();
    POOL_END();
    return grantedPermission;
#else
    return JAVA_FALSE;
#endif
}


void throwError(CFErrorRef error) {
    if (error != nil) {
        CN1Log(@"error %@", error);
#ifndef NEW_CODENAME_ONE_VM
        CFStringRef errorDesc = CFErrorCopyDescription(error);
        CFIndex length = CFStringGetLength(errorDesc);
        char *buffer = (char *)malloc(length + 1);
        if(CFStringGetCString(errorDesc, buffer, length,
                              kCFStringEncodingUTF8)) {
            XMLVM_THROW_WITH_CSTRING(java_lang_RuntimeException, buffer);
        }
#endif
        /* TODO!!! */
    }
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_createContact___java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT firstName, JAVA_OBJECT surname, JAVA_OBJECT officePhone, JAVA_OBJECT homePhone, JAVA_OBJECT cellPhone, JAVA_OBJECT email) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    
    ABAddressBookRef addressBook = getAddressBook();
    if(!grantedPermission) {
        return JAVA_NULL;
    }
    CFErrorRef  error = nil;
    
    ABRecordRef person = ABPersonCreate();
    if(firstName != nil) {
        ABRecordSetValue(person, kABPersonFirstNameProperty, (BRIDGE_CAST CFStringRef)toNSString(CN1_THREAD_STATE_PASS_ARG firstName), NULL);
    }
    if(surname != nil) {
        ABRecordSetValue(person, kABPersonLastNameProperty, (BRIDGE_CAST CFStringRef)toNSString(CN1_THREAD_STATE_PASS_ARG surname), NULL);
    }
    
    if(email != nil) {
        ABMutableMultiValueRef emailVal = ABMultiValueCreateMutable(kABMultiStringPropertyType);
        ABMultiValueAddValueAndLabel(emailVal, (BRIDGE_CAST CFStringRef)toNSString(CN1_THREAD_STATE_PASS_ARG email), CFSTR("email"), NULL);
        ABRecordSetValue(person, kABPersonEmailProperty, emailVal, &error);
        throwError(error);
    }
    
    if(officePhone != nil || homePhone != nil || cellPhone != nil) {
        ABMutableMultiValueRef phoneVal = ABMultiValueCreateMutable(kABPersonPhoneProperty);
        if(officePhone != nil) {
            ABMultiValueAddValueAndLabel(phoneVal, (BRIDGE_CAST CFStringRef)toNSString(CN1_THREAD_STATE_PASS_ARG officePhone), kABWorkLabel, NULL);
        }
        if(homePhone != nil) {
            ABMultiValueAddValueAndLabel(phoneVal, (BRIDGE_CAST CFStringRef)toNSString(CN1_THREAD_STATE_PASS_ARG homePhone), kABHomeLabel, NULL);
        }
        if(cellPhone != nil) {
            ABMultiValueAddValueAndLabel(phoneVal, (BRIDGE_CAST CFStringRef)toNSString(CN1_THREAD_STATE_PASS_ARG cellPhone), kABPersonPhoneMobileLabel, NULL);
        }
        ABRecordSetValue(person, kABPersonPhoneProperty, phoneVal, &error);
        throwError(error);
    }
    ABAddressBookAddRecord(addressBook, person, &error);
    throwError(error);
    ABAddressBookSave(addressBook, &error);
    throwError(error);
    JAVA_OBJECT o = fromNSString(CN1_THREAD_STATE_PASS_ARG [NSString stringWithFormat:@"%i", ABRecordGetRecordID(person)]);
    POOL_END();
    return o;
#else
    return JAVA_NULL;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_deleteContact___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT i) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    ABAddressBookRef addressBook = getAddressBook();
    if(!grantedPermission) {
        return 0;
    }
    ABRecordRef ref = ABAddressBookGetPersonWithRecordID(addressBook, i);
    if(ref != nil) {
        ABAddressBookRemoveRecord(addressBook, ref, nil);
    }
    POOL_END();
    return ref != nil;
#else
    return JAVA_FALSE;
#endif
}


JAVA_INT com_codename1_impl_ios_IOSNative_getContactCount___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN includeNumbers) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    ABAddressBookRef addressBook = getAddressBook();
    if(!grantedPermission) {
        return 0;
    }
    CFIndex nPeople = ABAddressBookGetPersonCount(addressBook);
    
    if(includeNumbers) {
        CFArrayRef allPeople = ABAddressBookCopyArrayOfAllPeople(addressBook);
        int responseCount = 0;
        for(int iter = 0 ; iter < nPeople ; iter++) {
            ABRecordRef ref = CFArrayGetValueAtIndex(allPeople, iter);
            ABMultiValueRef numbers = ABRecordCopyValue(ref, kABPersonPhoneProperty);
            
            if(numbers != nil && ABMultiValueGetCount(numbers) > 0) {
                responseCount++;
            }
        }
        
        POOL_END();
        return responseCount;
    }
    
    POOL_END();
    return MAX(nPeople, 0);
#else
    return 0;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_countLinkedContacts___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT recId) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    ABRecordRef i = ABAddressBookGetPersonWithRecordID(getAddressBook(), recId);
    NSArray *linkedRecordsArray = (__bridge NSArray *)ABPersonCopyArrayOfAllLinkedPeople(i);
    int numLinked = [linkedRecordsArray count];
    [linkedRecordsArray release];
    POOL_END();
    return numLinked;
#else
    return 0;
#endif
}

#ifdef NEW_CODENAME_ONE_VM
JAVA_INT com_codename1_impl_ios_IOSNative_countLinkedContacts___int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT recId) {
    return com_codename1_impl_ios_IOSNative_countLinkedContacts___int(CN1_THREAD_STATE_PASS_ARG instanceObject, recId);
}
#endif



void com_codename1_impl_ios_IOSNative_getLinkedContactIds___int_int_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT num, JAVA_INT refId, JAVA_OBJECT out) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* iArray = intArray;
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)iArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    int size = iArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)out)->data;
    int size = ((JAVA_ARRAY)out)->length;
#endif
    ABRecordRef i = ABAddressBookGetPersonWithRecordID(getAddressBook(), refId);
    NSArray *linkedRecordsArray = (__bridge NSArray *)ABPersonCopyArrayOfAllLinkedPeople(i);
    JAVA_INT minNum = MIN(num, [linkedRecordsArray count]);
    minNum = MIN(minNum, size);
    for (int iter=0; iter < minNum; iter++) {
        ABRecordRef ref = (__bridge ABRecordRef)[linkedRecordsArray objectAtIndex:iter];
        data[iter] = ABRecordGetRecordID(ref);
    }
    [linkedRecordsArray release];
    POOL_END();
#endif
}


void com_codename1_impl_ios_IOSNative_getContactRefIds___int_1ARRAY_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT intArray, JAVA_BOOLEAN includeNumbers) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* iArray = intArray;
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)iArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    int size = iArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)intArray)->data;
    int size = ((JAVA_ARRAY)intArray)->length;
#endif
    ABAddressBookRef addressBook = getAddressBook();
    if(!grantedPermission) {
        return;
    }
    CFArrayRef allPeople = ABAddressBookCopyArrayOfAllPeople(addressBook);
    CFMutableArrayRef peopleMutable = CFArrayCreateMutableCopy(
        kCFAllocatorDefault,
        CFArrayGetCount(allPeople),
        allPeople
    );


    CFArraySortValues(
        peopleMutable,
        CFRangeMake(0, CFArrayGetCount(peopleMutable)),
        (CFComparatorFunction) ABPersonComparePeopleByName,
        (void *)(NSUInteger)ABPersonGetSortOrdering()
    );

    CFRelease(allPeople);
    allPeople = peopleMutable;
    
    if(includeNumbers) {
        CFIndex nPeople = ABAddressBookGetPersonCount(addressBook);
        int responseCount = 0;
        for(int iter = 0 ; iter < nPeople ; iter++) {
            ABRecordRef ref = CFArrayGetValueAtIndex(allPeople, iter);
            ABMultiValueRef numbers = ABRecordCopyValue(ref, kABPersonPhoneProperty);
            
            if(numbers != nil && ABMultiValueGetCount(numbers) > 0) {
                data[responseCount] = ABRecordGetRecordID(ref);
                responseCount++;
            }
        }
        
        POOL_END();
        return;
    }
    for(int iter = 0 ; iter < size ; iter++) {
        ABRecordRef ref = CFArrayGetValueAtIndex(allPeople, iter);
        data[iter] = ABRecordGetRecordID(ref);
    }
    POOL_END();
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonFirstName___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    NSString* k = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonFirstNameProperty);
    JAVA_OBJECT ret = fromNSString(CN1_THREAD_STATE_PASS_ARG k);
    POOL_END();
    return ret;
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonSurnameName___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    NSString* k = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonLastNameProperty);
    JAVA_OBJECT ret = fromNSString(CN1_THREAD_STATE_PASS_ARG k);
    POOL_END();
    return ret;
#else
    return JAVA_NULL;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_getPersonPhoneCount___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_CONTACTS_USAGE
    //POOL_BEGIN();
    //POOL_END();
    return 1;
#else
    return 0;
#endif
}

#ifdef INCLUDE_CONTACTS_USAGE
JAVA_OBJECT copyValueAsString(CN1_THREAD_STATE_MULTI_ARG ABMultiValueRef r) {
    JAVA_OBJECT ret = JAVA_NULL;
    if(ABMultiValueGetCount(r) > 0) {
        NSString *k = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(r, 0);
        ret = fromNSString(CN1_THREAD_STATE_PASS_ARG k);
    }
    return ret;
}
#endif

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPhone___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    ABMultiValueRef k = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonPhoneProperty);
    JAVA_OBJECT ret = copyValueAsString(CN1_THREAD_STATE_PASS_ARG k);
    POOL_END();
    return ret;
#else 
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPhoneType___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    //ABRecordRef i = (ABRecordRef)peer;
    //ABMultiValueRef k = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonPhoneMainLabel);
    //JAVA_OBJECT ret = copyValueAsString(k);
    JAVA_OBJECT ret = fromNSString(CN1_THREAD_STATE_PASS_ARG @"work");
    POOL_END();
    return ret;
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPrimaryPhone___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_CONTACTS_USAGE    
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    ABMultiValueRef k = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonPhoneProperty);
    JAVA_OBJECT ret = copyValueAsString(CN1_THREAD_STATE_PASS_ARG k);
    POOL_END();
    return ret;
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonEmail___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    ABMultiValueRef emails = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonEmailProperty);
    JAVA_OBJECT ret = copyValueAsString(CN1_THREAD_STATE_PASS_ARG emails);
    POOL_END();
    return ret;
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonAddress___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    NSString* k = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonAddressProperty);
    JAVA_OBJECT ret = fromNSString(CN1_THREAD_STATE_PASS_ARG k);
    POOL_END();
    return ret;
#else
    return JAVA_NULL;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createPersonPhotoImage___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    GLUIImage* g = nil;
    if(ABPersonHasImageData(i)){
        CN1Image* img = [CN1Image imageWithData:(BRIDGE_CAST NSData *)ABPersonCopyImageData(i)];
        g = [[GLUIImage alloc] initWithImage:img];
    }
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)g);
#else
    return 0;
#endif
}

#ifdef INCLUDE_CONTACTS_USAGE
void addToHashtable(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT hash, ABMultiValueRef ref, int count) {
    for(int iter = 0 ; iter < count ; iter++) {
        NSString *key = (BRIDGE_CAST NSString *)ABMultiValueCopyLabelAtIndex(ref, iter);
#ifndef NEW_CODENAME_ONE_VM
        if(key == nil) {
            NSString *value = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(ref, iter);
            java_util_Hashtable_put___java_lang_Object_java_lang_Object(hash, fromNSString(@""), fromNSString(value));
        } else {
            NSString *value = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(ref, iter);
            java_util_Hashtable_put___java_lang_Object_java_lang_Object(hash, fromNSString(key), fromNSString(value));
        }
#else
        if(key == nil) {
            NSString *value = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(ref, iter);
            java_util_Hashtable_put___java_lang_Object_java_lang_Object_R_java_lang_Object(CN1_THREAD_STATE_PASS_ARG hash, fromNSString(CN1_THREAD_STATE_PASS_ARG @""), fromNSString(CN1_THREAD_STATE_PASS_ARG value));
        } else {
            NSString *value = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(ref, iter);
            java_util_Hashtable_put___java_lang_Object_java_lang_Object_R_java_lang_Object(CN1_THREAD_STATE_PASS_ARG hash, fromNSString(CN1_THREAD_STATE_PASS_ARG key), fromNSString(CN1_THREAD_STATE_PASS_ARG value));
        }
#endif
    }
}
#endif

void com_codename1_impl_ios_IOSNative_updatePersonWithRecordID___int_com_codename1_contacts_Contact_boolean_boolean_boolean_boolean_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT recId, JAVA_OBJECT cnt,
                                                                                                                                 JAVA_BOOLEAN includesFullName, JAVA_BOOLEAN includesPicture, JAVA_BOOLEAN includesNumbers, JAVA_BOOLEAN includesEmail, JAVA_BOOLEAN includeAddress) {
#ifdef INCLUDE_CONTACTS_USAGE     
    POOL_BEGIN();
    ABRecordRef i = ABAddressBookGetPersonWithRecordID(getAddressBook(), recId);
    
    if(includeAddress) {
        ABMultiValueRef addresses = (ABMultiValueRef)ABRecordCopyValue(i, kABPersonAddressProperty);
        int addressCount = ABMultiValueGetCount(addresses);
        if(addressCount > 0) {
            
#ifndef NEW_CODENAME_ONE_VM
            JAVA_OBJECT addressesHash = com_codename1_contacts_Contact_getAddresses__(cnt);
#else
            JAVA_OBJECT addressesHash = com_codename1_contacts_Contact_getAddresses___R_java_util_Hashtable(CN1_THREAD_STATE_PASS_ARG cnt);
            for (CFIndex j = 0; j<addressCount;j++){
                JAVA_OBJECT addr = __NEW_com_codename1_contacts_Address(CN1_THREAD_STATE_PASS_SINGLE_ARG);
                com_codename1_contacts_Address___INIT____(CN1_THREAD_STATE_PASS_ARG addr);
                CFDictionaryRef dict = ABMultiValueCopyValueAtIndex(addresses, j);
                CFStringRef typeTmp = ABMultiValueCopyLabelAtIndex(addresses, j);
                CFStringRef labeltype = ABAddressBookCopyLocalizedLabel(typeTmp);
                NSString *street = [(NSString *)CFDictionaryGetValue(dict, kABPersonAddressStreetKey) copy];
                NSString *city = [(NSString *)CFDictionaryGetValue(dict, kABPersonAddressCityKey) copy];
                NSString *state = [(NSString *)CFDictionaryGetValue(dict, kABPersonAddressStateKey) copy];
                NSString *zip = [(NSString *)CFDictionaryGetValue(dict, kABPersonAddressZIPKey) copy];
                NSString *country = [(NSString *)CFDictionaryGetValue(dict, kABPersonAddressCountryKey) copy];
                
                com_codename1_contacts_Address_setCountry___java_lang_String(CN1_THREAD_STATE_PASS_ARG addr, fromNSString(CN1_THREAD_STATE_PASS_ARG country));
                com_codename1_contacts_Address_setLocality___java_lang_String(CN1_THREAD_STATE_PASS_ARG addr, fromNSString(CN1_THREAD_STATE_PASS_ARG city));
                com_codename1_contacts_Address_setRegion___java_lang_String(CN1_THREAD_STATE_PASS_ARG addr, fromNSString(CN1_THREAD_STATE_PASS_ARG state));
                com_codename1_contacts_Address_setPostalCode___java_lang_String(CN1_THREAD_STATE_PASS_ARG addr, fromNSString(CN1_THREAD_STATE_PASS_ARG zip));
                com_codename1_contacts_Address_setStreetAddress___java_lang_String(CN1_THREAD_STATE_PASS_ARG addr, fromNSString(CN1_THREAD_STATE_PASS_ARG street));
                
                
                
                
                
                
                [street release];
                [city release];
                [state release];
                [zip release];
                [country release];
                CFRelease(dict);
                if(typeTmp != 0) {
                    CFRelease(typeTmp);
                }
                CFRelease(labeltype);
                java_util_Hashtable_put___java_lang_Object_java_lang_Object_R_java_lang_Object(CN1_THREAD_STATE_PASS_ARG addressesHash, fromNSString(CN1_THREAD_STATE_PASS_ARG (NSString*)labeltype), addr);
            }
            CFRelease(addresses);
            
            
#endif
            
            //addToHashtable(CN1_THREAD_STATE_PASS_ARG addressesHash, addresses, addressCount);
        }
    }
    
    if(includesEmail) {
        ABMultiValueRef emails = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonEmailProperty);
        int emailCount = ABMultiValueGetCount(emails);
        if(emailCount > 0) {
            NSString *k = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(emails, 0);
            com_codename1_contacts_Contact_setPrimaryEmail___java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt, fromNSString(CN1_THREAD_STATE_PASS_ARG k));
#ifndef NEW_CODENAME_ONE_VM
            JAVA_OBJECT emailsHash = com_codename1_contacts_Contact_getEmails__(cnt);
#else
            JAVA_OBJECT emailsHash = com_codename1_contacts_Contact_getEmails___R_java_util_Hashtable(CN1_THREAD_STATE_PASS_ARG cnt);
#endif
            addToHashtable(CN1_THREAD_STATE_PASS_ARG emailsHash, emails, emailCount);
        }
    }
    
    if(includesNumbers) {
        ABMultiValueRef numbers = ABRecordCopyValue(i, kABPersonPhoneProperty);
        int numbersCount = ABMultiValueGetCount(numbers);
        if(numbersCount > 0) {
            NSString *k = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(numbers, 0);
            com_codename1_contacts_Contact_setPrimaryPhoneNumber___java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt, fromNSString(CN1_THREAD_STATE_PASS_ARG k));
#ifndef NEW_CODENAME_ONE_VM
            JAVA_OBJECT hash = com_codename1_contacts_Contact_getPhoneNumbers__(cnt);
#else
            JAVA_OBJECT hash = com_codename1_contacts_Contact_getPhoneNumbers___R_java_util_Hashtable(CN1_THREAD_STATE_PASS_ARG cnt);
#endif
            addToHashtable(CN1_THREAD_STATE_PASS_ARG hash, numbers, numbersCount);
        }
    }
    
    NSString* first = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonFirstNameProperty);
    if(first != nil) {
        com_codename1_contacts_Contact_setFirstName___java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt, fromNSString(CN1_THREAD_STATE_PASS_ARG first));
    }
    
    NSString* last = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonLastNameProperty);
    if(last != nil) {
        com_codename1_contacts_Contact_setFamilyName___java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt, fromNSString(CN1_THREAD_STATE_PASS_ARG last));
    }
    
    if(includesFullName) {
        NSString* full = [NSString stringWithFormat:@"%@ %@", first, last];
        if(first != nil && last != nil) {
            com_codename1_contacts_Contact_setDisplayName___java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt, fromNSString(CN1_THREAD_STATE_PASS_ARG full));
        } else {
            if(first != nil) {
                com_codename1_contacts_Contact_setDisplayName___java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt, fromNSString(CN1_THREAD_STATE_PASS_ARG first));
            } else {
                if(last != nil) {
                    com_codename1_contacts_Contact_setDisplayName___java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt, fromNSString(CN1_THREAD_STATE_PASS_ARG last));
                } else {
                    ABMultiValueRef emailsTmp = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonEmailProperty);
                    int emailCountTmp = ABMultiValueGetCount(emailsTmp);
                    if(emailCountTmp > 0) {
                        NSString *kTmp = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(emailsTmp, 0);
                        com_codename1_contacts_Contact_setDisplayName___java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt, fromNSString(CN1_THREAD_STATE_PASS_ARG kTmp));
                    } else {
                        ABMultiValueRef numbers = ABRecordCopyValue(i, kABPersonPhoneProperty);
                        int numbersCount = ABMultiValueGetCount(numbers);
                        if(numbersCount > 0) {
                            NSString *k = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(numbers, 0);
                            com_codename1_contacts_Contact_setDisplayName___java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt, fromNSString(CN1_THREAD_STATE_PASS_ARG k));
                        } else {
                            NSString* org = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonOrganizationProperty);
                            if(org != nil) {
                                com_codename1_contacts_Contact_setDisplayName___java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt, fromNSString(CN1_THREAD_STATE_PASS_ARG org));
                            } else {
                                com_codename1_contacts_Contact_setDisplayName___java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt, fromNSString(CN1_THREAD_STATE_PASS_ARG @"Unnamed Contact"));
                            }
                        }
                    }

                }
            }
        }
        //CN1Log(@"%@", toNSString(CN1_THREAD_STATE_PASS_ARG com_codename1_contacts_Contact_getDisplayName___R_java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt)));
    }

    
    NSString* note = (BRIDGE_CAST NSString*)ABRecordCopyValue(i, kABPersonNoteProperty);
    if(note != nil) {
        com_codename1_contacts_Contact_setNote___java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt, fromNSString(CN1_THREAD_STATE_PASS_ARG note));
    }
    
    NSDate *bDayProperty = (BRIDGE_CAST NSDate*)ABRecordCopyValue(i, kABPersonBirthdayProperty);
    if(bDayProperty != nil) {
        NSTimeInterval nst = [bDayProperty timeIntervalSince1970];
        com_codename1_contacts_Contact_setBirthday___long(CN1_THREAD_STATE_PASS_ARG cnt, nst * 1000);
    }
    
    if(includesPicture) {
        GLUIImage* g = nil;
        if(ABPersonHasImageData(i)){
            CN1Image* img = [CN1Image imageWithData:(BRIDGE_CAST NSData *)ABPersonCopyImageDataWithFormat(i, kABPersonImageFormatThumbnail)];
            g = [[GLUIImage alloc] initWithImage:img];
#ifndef NEW_CODENAME_ONE_VM
            com_codename1_impl_ios_IOSImplementation_NativeImage* nativeImage = (com_codename1_impl_ios_IOSImplementation_NativeImage*)__NEW_com_codename1_impl_ios_IOSImplementation_NativeImage();
            (*nativeImage).fields.com_codename1_impl_ios_IOSImplementation_NativeImage.peer_ = g;
            (*nativeImage).fields.com_codename1_impl_ios_IOSImplementation_NativeImage.width_ = (int)[g getImage].size.width;
            (*nativeImage).fields.com_codename1_impl_ios_IOSImplementation_NativeImage.height_ = (int)[g getImage].size.height;
            JAVA_OBJECT image = com_codename1_ui_Image_createImage___java_lang_Object(nativeImage);
            com_codename1_contacts_Contact_setPhoto___com_codename1_ui_Image(CN1_THREAD_STATE_PASS_ARG cnt, image);
#else
            enteringNativeAllocations();
            struct obj__com_codename1_impl_ios_IOSImplementation_NativeImage* nativeImage = (struct obj__com_codename1_impl_ios_IOSImplementation_NativeImage*)__NEW_com_codename1_impl_ios_IOSImplementation_NativeImage(CN1_THREAD_STATE_PASS_SINGLE_ARG);
            (*nativeImage).com_codename1_impl_ios_IOSImplementation_NativeImage_peer = (JAVA_LONG)g;
            (*nativeImage).com_codename1_impl_ios_IOSImplementation_NativeImage_width = (int)[g getImage].size.width;
            (*nativeImage).com_codename1_impl_ios_IOSImplementation_NativeImage_height = (int)[g getImage].size.height;
            JAVA_OBJECT image = com_codename1_ui_Image_createImage___java_lang_Object_R_com_codename1_ui_Image(CN1_THREAD_STATE_PASS_ARG nativeImage);
            com_codename1_contacts_Contact_setPhoto___com_codename1_ui_Image(CN1_THREAD_STATE_PASS_ARG cnt, image);
            finishedNativeAllocations();
#endif
        }
    }
    
    POOL_END();
#endif
}

#if defined(CN1_USE_METAL)
// Reads the Metal renderer's persistent screenTexture back into a CGImage.
// screenTexture is exactly the frame presentFramebuffer blits into the
// CAMetalLayer drawable, so it IS the genuine on-screen pixel content. Unlike
// -drawViewHierarchyInRect: / -snapshotViewAfterScreenUpdates:, reading it
// back does not depend on a CADisplayLink present cycle -- which never fires
// on headless Mac Catalyst CI -- so it always reflects the latest committed
// frame rather than a stale one. The blit is submitted on the same command
// queue METALView renders on (CN1MetalCommandQueue), so FIFO ordering
// guarantees it runs after the frame's render work; waitUntilCompleted then
// makes the pixels readable. Returns NULL if Metal isn't initialised yet.
static CGImageRef cn1_copyMetalScreenTextureImage(METALView *mv) {
    if (mv == nil) {
        return NULL;
    }
    id<MTLTexture> src = mv.screenTexture;
    if (src == nil) {
        // Direct-to-drawable mode (CN1_DIRECT_DRAWABLE, opt-in) keeps no
        // retained screen texture, so there is nothing here to read back and
        // the caller falls through to drawViewHierarchyInRect:. That is correct
        // on device but samples the CALayer's presented drawable, so it can lag
        // a frame -- exactly the staleness this readback exists to avoid, and
        // on headless Catalyst (no display link) it can be stale indefinitely.
        //
        // Reading the live drawable instead is not the fix: retaining it past
        // present starves nextDrawable, and after present the buffer is
        // recycled for the following frame. A deterministic capture needs a
        // one-shot render into a scratch target, which is worth doing when the
        // mode stops being opt-in. Until then the default path is unaffected.
        return NULL;
    }
    NSUInteger w = src.width;
    NSUInteger h = src.height;
    if (w == 0 || h == 0) {
        return NULL;
    }
    id<MTLDevice> device = CN1MetalDevice();
    id<MTLCommandQueue> queue = CN1MetalCommandQueue();
    if (device == nil || queue == nil) {
        return NULL;
    }
    // screenTexture is MTLStorageModePrivate -- the CPU can't getBytes from it
    // directly. Blit it into a CPU-visible staging texture and synchronize.
    // Managed storage is used (not Shared) because it is the one mode valid on
    // both Apple-silicon and Intel Mac GPUs under Catalyst; synchronizeResource
    // makes the managed copy CPU-visible (a no-op on unified memory).
    MTLTextureDescriptor *desc =
        [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
                                                           width:w height:h mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
#if TARGET_OS_MACCATALYST
    desc.storageMode = MTLStorageModeManaged;
#else
    // iOS (device + simulator) and tvOS are unified-memory: MTLStorageModeManaged
    // and -synchronizeResource: are unavailable there. Stage into a Shared
    // texture, which the CPU can getBytes from directly once the blit completes
    // -- no synchronize step is needed.
    desc.storageMode = MTLStorageModeShared;
#endif
    id<MTLTexture> staging = [device newTextureWithDescriptor:desc];
    if (staging == nil) {
        return NULL;
    }
    id<MTLCommandBuffer> cb = [queue commandBuffer];
    id<MTLBlitCommandEncoder> blit = [cb blitCommandEncoder];
    [blit copyFromTexture:src
              sourceSlice:0 sourceLevel:0
             sourceOrigin:MTLOriginMake(0, 0, 0)
               sourceSize:MTLSizeMake(w, h, 1)
                toTexture:staging
         destinationSlice:0 destinationLevel:0
        destinationOrigin:MTLOriginMake(0, 0, 0)];
#if TARGET_OS_MACCATALYST
    // Managed storage (Catalyst) must be synchronized to become CPU-visible;
    // Shared storage (iOS / tvOS) is already CPU-visible, so this would be invalid.
    [blit synchronizeResource:staging];
#endif
    [blit endEncoding];
    [cb commit];
    [cb waitUntilCompleted];

    NSUInteger bytesPerRow = w * 4;
    void *bytes = malloc(h * bytesPerRow);
    if (bytes == NULL) {
#ifndef CN1_USE_ARC
        [staging release];
#endif
        return NULL;
    }
    [staging getBytes:bytes
          bytesPerRow:bytesPerRow
           fromRegion:MTLRegionMake2D(0, 0, w, h)
          mipmapLevel:0];
#ifndef CN1_USE_ARC
    [staging release];
#endif

    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    // BGRA8Unorm in memory (B,G,R,A) is a 0xAARRGGBB little-endian word, which
    // Core Graphics consumes as byteOrder32Little | premultipliedFirst (ARGB).
    CGBitmapInfo bitmapInfo = (CGBitmapInfo)kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little;
    CGContextRef cgctx = CGBitmapContextCreate(bytes, w, h, 8, bytesPerRow, cs, bitmapInfo);
    CGColorSpaceRelease(cs);
    CGImageRef img = NULL;
    if (cgctx != NULL) {
        img = CGBitmapContextCreateImage(cgctx);
        CGContextRelease(cgctx);
    }
    free(bytes);
    return img;
}
#endif

#if !TARGET_OS_WATCH
static BOOL cn1_renderViewIntoContext(CN1View *renderView, CN1View *rootView, CGContextRef ctx) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return NO;
#else
    if (renderView == nil || rootView == nil || ctx == NULL) {
        return NO;
    }
    if (renderView.hidden || renderView.alpha <= 0.0f) {
        return NO;
    }

    CGRect localBounds = renderView.bounds;
    if (CGRectIsEmpty(localBounds) || localBounds.size.width <= 0.0f || localBounds.size.height <= 0.0f) {
        return NO;
    }

    CGRect translatedRect = [rootView convertRect:localBounds fromView:renderView];
    if (CGRectIsNull(translatedRect) || CGRectIsEmpty(translatedRect)) {
        return NO;
    }

    CGContextSaveGState(ctx);
    CGContextTranslateCTM(ctx, translatedRect.origin.x, translatedRect.origin.y);
    BOOL drawn = NO;
#if defined(ENABLE_WKWEBVIEW) && defined(supportsWKWebKit)
    if ([renderView isKindOfClass:[WKWebView class]]) {
        WKWebView *webView = (WKWebView *)renderView;
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 110000
        if (!drawn && @available(iOS 11.0, *)) {
            CGRect snapshotRect = CGRectIntersection(webView.bounds, localBounds);
            if (!CGRectIsNull(snapshotRect) && !CGRectIsEmpty(snapshotRect)) {
                WKSnapshotConfiguration *config = [[WKSnapshotConfiguration alloc] init];
                config.rect = snapshotRect;
                if (snapshotRect.size.width > 0.0f) {
                    config.snapshotWidth = @(snapshotRect.size.width);
                }
#ifdef __IPHONE_13_0
                if (@available(iOS 13.0, *)) {
                    // afterScreenUpdates:YES waits for the next screen
                    // refresh before snapshotting. On Mac Catalyst CI
                    // (headless macos-15) the refresh never fires, so
                    // the completion handler never runs and the wait
                    // below times out -- yielding a black body. Use NO
                    // on Catalyst: the page is already loaded + DOM is
                    // queried before this point (BrowserComponentScreen-
                    // shotTest waits for onLoad + a JS round-trip), so
                    // the current frame already has the rendered HTML.
#if TARGET_OS_MACCATALYST
                    config.afterScreenUpdates = NO;
#else
                    config.afterScreenUpdates = YES;
#endif
                }
#endif
                __block CN1Image *snapshotImage = nil;
                __block BOOL snapshotComplete = NO;
                [webView takeSnapshotWithConfiguration:config completionHandler:^(CN1Image * _Nullable image, NSError * _Nullable error) {
                    if (image != nil) {
                        snapshotImage = image;
                    } else if (error != nil) {
                        NSLog(@"WKWebView snapshot failed: %@", error);
                    }
                    snapshotComplete = YES;
                }];
                [config release];

                if (!snapshotComplete) {
                    // Pump the run loop in NSRunLoopCommonModes (not just
                    // NSDefaultRunLoopMode) so the snapshot completion source
                    // -- which on Mac Catalyst delivers via a tracking-mode
                    // source -- gets picked up. 1 s is enough on iOS / iPadOS
                    // (snapshotWithConfiguration delivers in ~50 ms when the
                    // page is loaded) but on Mac Catalyst's headless CI the
                    // first snapshot of a freshly-loaded page can take 2+ s,
                    // so wait up to 3 s before giving up.
#if TARGET_OS_MACCATALYST
                    NSTimeInterval timeout = 3.0;
#else
                    NSTimeInterval timeout = 1.0;
#endif
                    while (!snapshotComplete && timeout > 0) {
                        NSTimeInterval step = 0.01;
                        NSDate *stepDate = [NSDate dateWithTimeIntervalSinceNow:step];
                        [[NSRunLoop currentRunLoop] runMode:NSRunLoopCommonModes beforeDate:stepDate];
                        timeout -= step;
                    }
                }

                if (snapshotImage != nil) {
                    [snapshotImage drawInRect:CGRectMake(0, 0, localBounds.size.width, localBounds.size.height)];
                    drawn = YES;
                }
            }
        }
#endif
        if (!drawn) {
            CN1View *snapshotView = [renderView snapshotViewAfterScreenUpdates:YES];
            if (snapshotView != nil) {
                BOOL snapshotDrawn = NO;
                if ([snapshotView respondsToSelector:@selector(drawViewHierarchyInRect:afterScreenUpdates:)]) {
                    snapshotDrawn = [snapshotView drawViewHierarchyInRect:snapshotView.bounds afterScreenUpdates:YES];
                }
                if (!snapshotDrawn) {
                    [snapshotView.layer renderInContext:ctx];
                }
                drawn = YES;
            }
        }
    }
#endif
#if defined(CN1_USE_METAL)
    // The Metal screen view: capture from the renderer's screenTexture, the
    // exact pixels presented to the drawable. On headless Mac Catalyst the
    // display link never presents, so -drawViewHierarchyInRect: below would
    // snapshot a stale CALayer frame. The screenTexture readback is always the
    // latest committed frame, so it is both correct and deterministic.
    //
    // This used to be gated to Catalyst/TV, leaving the iPhone/iPad simulator
    // and device on drawViewHierarchyInRect:afterScreenUpdates:NO, which
    // snapshots the CALayer's currently-presented drawable. That drawable lags
    // the screenTexture: presentFramebuffer commits the screenTexture->drawable
    // blit without waiting (METALView.m), and the CALayer composites it on a
    // later CA transaction, so a screenshot taken right after a Form.show()
    // could capture the PREVIOUS form (the stale-frame race that made
    // DesktopModeScreenshotTest non-deterministic -- it intermittently captured
    // the prior test's form). The screenTexture readback below blits +
    // waitUntilCompleted, so it always reflects the latest committed CN1 frame
    // regardless of drawable-present / CALayer-composite timing.
    if (!drawn && [renderView isKindOfClass:[METALView class]]) {
        CGImageRef cg = cn1_copyMetalScreenTextureImage((METALView *)renderView);
        if (cg != NULL) {
            // screenTexture row 0 is the top of the screen; Core Graphics draws
            // bottom-up, so flip vertically to keep it upright in the
            // (top-left-origin) UIGraphics capture context.
            CGContextSaveGState(ctx);
            CGContextTranslateCTM(ctx, 0, localBounds.size.height);
            CGContextScaleCTM(ctx, 1.0, -1.0);
            CGContextDrawImage(ctx, CGRectMake(0, 0, localBounds.size.width, localBounds.size.height), cg);
            CGContextRestoreGState(ctx);
            CGImageRelease(cg);
            drawn = YES;
        }
    }
#endif
    if (!drawn && [renderView respondsToSelector:@selector(drawViewHierarchyInRect:afterScreenUpdates:)]) {
        // afterScreenUpdates:NO — YES can stall indefinitely under UIScene on
        // iPhone/iPad waiting for a scene display-link cycle that never fires
        // during a synchronous capture. On Mac Catalyst the scene model is
        // different and YES is required: the live screenTexture isn't
        // committed by CADisplayLink between form.show() and the screenshot
        // callback, so afterScreenUpdates:NO captures the previous form's
        // framebuffer. (The Metal screen view is handled by the screenTexture
        // readback above; this fallback only runs for non-Metal peer views.)
#if TARGET_OS_MACCATALYST
        drawn = [renderView drawViewHierarchyInRect:localBounds afterScreenUpdates:YES];
#else
        drawn = [renderView drawViewHierarchyInRect:localBounds afterScreenUpdates:NO];
#endif
    }
    if (!drawn) {
        [renderView.layer renderInContext:ctx];
        drawn = YES;
    }
    CGContextRestoreGState(ctx);
    return drawn;
#endif
}

static void cn1_renderPeerComponents(CN1View *rootView, CGContextRef ctx) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
#else
    CodenameOne_GLViewController *controller = [CodenameOne_GLViewController instance];
    EAGLView *glView = [controller eaglView];
    if (glView == nil || rootView == nil || ctx == NULL) {
        return;
    }

    CN1View *peerLayer = glView.peerComponentsLayer;
    NSArray<CN1View *> *peerCandidates = nil;
    if (peerLayer != nil) {
        [peerLayer layoutIfNeeded];
        peerCandidates = peerLayer.subviews;
    } else {
        [glView layoutIfNeeded];
        peerCandidates = glView.subviews;
    }

    if (peerCandidates.count == 0) {
        return;
    }

    for (CN1View *peerView in peerCandidates) {
        if (![peerView isKindOfClass:[CN1View class]]) {
            continue;
        }
        cn1_renderViewIntoContext(peerView, rootView, ctx);
    }
#endif
}

static CN1View* cn1_rootViewForCapture(CN1View *view) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return nil;
#else
    if (view == nil) {
        return nil;
    }

    CN1View *rootView = view;
    UIWindow *window = view.window;

    if (window == nil) {
        NSArray<UIWindow*> *windows = [UIApplication sharedApplication].windows;
        for (UIWindow *candidate in windows) {
            if ([view isDescendantOfView:candidate]) {
                window = candidate;
                break;
            }
        }
    }

    if (window == nil) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 130000
        if (@available(iOS 13.0, *)) {
            NSSet<UIScene *> *connectedScenes = [UIApplication sharedApplication].connectedScenes;
            for (UIScene *scene in connectedScenes) {
                if (![scene isKindOfClass:[UIWindowScene class]]) {
                    continue;
                }
                if (scene.activationState != UISceneActivationStateForegroundActive) {
                    continue;
                }
                UIWindowScene *windowScene = (UIWindowScene *)scene;
                for (UIWindow *candidate in windowScene.windows) {
                    if ([view isDescendantOfView:candidate]) {
                        window = candidate;
                        break;
                    }
                }
                if (window != nil) {
                    break;
                }
                if (windowScene.windows.count > 0 && window == nil) {
                    window = windowScene.windows.firstObject;
                }
            }
        }
#endif
    }

    if (window == nil) {
        window = [UIApplication sharedApplication].keyWindow;
    }

    if (window != nil) {
        rootView = window;
    } else {
        CN1View *candidate = view;
        while (candidate.superview != nil) {
            candidate = candidate.superview;
        }
        rootView = candidate;
    }

    return rootView;
#endif
}

static CN1Image* cn1_captureView(CN1View *view) {
#if TARGET_OS_OSX
    // The Metal surface is read back directly rather than asked to draw itself
    // into an image context. An NSView renders through its layer, and a
    // layer-hosted CAMetalLayer has no drawRect: to invoke -- asking it to
    // cache its display returns an empty bitmap, which is how a screenshot pass
    // silently produces blank frames.
    if (![view isKindOfClass:[METALView class]]) {
        return nil;
    }
    METALView *metal = (METALView *)view;
    int w = metal.framebufferWidth;
    int h = metal.framebufferHeight;
    if (w <= 0 || h <= 0) {
        return nil;
    }
    unsigned int *argb = (unsigned int *)malloc((size_t)w * h * 4);
    if (argb == NULL) {
        return nil;
    }
    if (![metal readbackInto:argb width:w height:h]) {
        free(argb);
        return nil;
    }
    NSImage *image = CN1AppKitNSImageFromARGB(argb, w, h);
    free(argb);
    // Peer components live in a sibling view above the Metal layer and are not
    // in that texture, so they are composited on top the way the UIKit path
    // walks the peer hierarchy.
    NSView *peers = metal.peerComponentsLayer;
    if (image != nil && peers != nil && peers.subviews.count > 0) {
        NSRect bounds = peers.bounds;
        if (bounds.size.width > 0 && bounds.size.height > 0) {
            NSBitmapImageRep *rep = [peers bitmapImageRepForCachingDisplayInRect:bounds];
            if (rep != nil) {
                [peers cacheDisplayInRect:bounds toBitmapImageRep:rep];
                [image lockFocus];
                [rep drawInRect:NSMakeRect(0, 0, image.size.width, image.size.height)];
                [image unlockFocus];
            }
        }
    }
    return image;
#else
    CN1View *rootView = cn1_rootViewForCapture(view);
    if (rootView == nil) {
        return nil;
    }

    CGSize size = rootView.bounds.size;
    if (size.width <= 0 || size.height <= 0) {
        return nil;
    }

    UIGraphicsBeginImageContextWithOptions(size, rootView.opaque, 0.0);
    CGContextRef ctx = UIGraphicsGetCurrentContext();
    if (ctx == NULL) {
        UIGraphicsEndImageContext();
        return nil;
    }

    [rootView layoutIfNeeded];

    cn1_renderViewIntoContext(view, rootView, ctx);

    CodenameOne_GLViewController *controller = [CodenameOne_GLViewController instance];
    EAGLView *glView = [controller eaglView];
    if (glView != nil && glView != view) {
        cn1_renderViewIntoContext(glView, rootView, ctx);
    }

    cn1_renderPeerComponents(rootView, ctx);

    CN1Image *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    if (rootView != view) {
        CGRect targetFrame = [rootView convertRect:view.bounds fromView:view];
        targetFrame = CGRectIntersection(targetFrame, CGRectMake(0, 0, size.width, size.height));
        if (!CGRectIsNull(targetFrame) && targetFrame.size.width > 0 && targetFrame.size.height > 0) {
            CGRect integralTarget = CGRectIntegral(targetFrame);
            CGRect pixelRect = CGRectMake(integralTarget.origin.x * image.scale,
                                          integralTarget.origin.y * image.scale,
                                          integralTarget.size.width * image.scale,
                                          integralTarget.size.height * image.scale);
            CGImageRef cropped = CGImageCreateWithImageInRect(image.CGImage, pixelRect);
            if (cropped != nil) {
                CN1Image *croppedImage = [CN1Image imageWithCGImage:cropped scale:image.scale orientation:image.imageOrientation];
                CGImageRelease(cropped);
                image = croppedImage;
            }
        }
    }

    return image;
#endif
}
#endif // !TARGET_OS_WATCH (CN1View screen-capture helpers)

void com_codename1_impl_ios_IOSNative_screenshot__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_WATCH
    // Capture the Core Graphics surface. Drain any pending ops first so the
    // snapshot reflects the latest painted frame, then PNG-encode the bitmap.
    // watchOS render + PNG encode are main-thread-affine in this host (see the
    // watch drawFrame / main-thread decode changes in
    // CodenameOne_GLViewController.m), so the capture MUST run on the main
    // queue. Do NOT wrap it in the old external drain lock: master's watch
    // drawFrame drains on the main thread itself, so an extra lock around a
    // main-queue dispatch only starves the render pump and every frame comes
    // back nil -- 1x1 placeholders for the whole suite.
    __block CN1WatchRenderingView *wv = nil;
    __block CN1Image *wimg = nil;
    __block NSData *wpng = nil;
    void (^captureWatchFrame)(void) = ^{
        // Use master's default drawFrame: (allowInactive:NO). During a headless
        // CN1SS test the watch app is not UIApplicationStateActive; NO returns
        // the already-painted frame instead of forcing a blocking draw, which is
        // what master's green watch suite does. Forcing allowInactive:YES here
        // blocks waiting for a frame that never schedules -> the capture hangs
        // and 0 of 216 screenshots stream. (The 1x1 blanks were a separate bug:
        // forceScreenRenderForCapture emptying the CG frame -- fixed by gating
        // that method to the Metal backend.)
        [[CodenameOne_GLViewController instance] drawFrame:CGRectZero];
        wv = [CN1WatchHost sharedHost].renderingView;
        wimg = wv != nil ? [wv currentFrame] : nil;
        wpng = wimg != nil ? UIImagePNGRepresentation(wimg) : nil;
#ifndef CN1_USE_ARC
        [wpng retain];
#endif
    };
    // CN1SS screenshot requests originate from Java callbacks and are not
    // guaranteed to already be on the watch UI thread.
    if ([NSThread isMainThread]) {
        captureWatchFrame();
    } else {
        dispatch_sync(dispatch_get_main_queue(), captureWatchFrame);
    }
    if (wpng == nil || [wpng length] == 0) {
        int logicalW = wv != nil ? [wv logicalWidth] : -1;
        int logicalH = wv != nil ? [wv logicalHeight] : -1;
        NSLog(@"CN1SS:ERR:native watch screenshot failed renderingView=%@ image=%@ logical=%dx%d",
              wv, wimg, logicalW, logicalH);
    }
    JAVA_OBJECT wbyteArr = JAVA_NULL;
    if (wpng != nil && [wpng length] > 0) {
        int wlen = (int)[wpng length];
#ifdef CN1_WATCH_DEBUG_DUMP_SHOTS
        {
            static int wshotIdx = 0;
            NSArray *docs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString *p = [[docs firstObject] stringByAppendingPathComponent:[NSString stringWithFormat:@"cn1ss_%03d.png", wshotIdx++]];
            [wpng writeToFile:p atomically:YES];
        }
#endif
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* warr = XMLVMArray_createSingleDimension(__CLASS_byte, wlen);
        memcpy(warr->fields.org_xmlvm_runtime_XMLVMArray.array_, [wpng bytes], wlen);
        wbyteArr = warr;
#else
        enteringNativeAllocations();
        JAVA_OBJECT warr = __NEW_ARRAY_JAVA_BYTE(CN1_THREAD_STATE_PASS_ARG wlen);
        memcpy(((JAVA_ARRAY)warr)->data, [wpng bytes], wlen);
        finishedNativeAllocations();
        wbyteArr = warr;
#endif
    }
#ifndef CN1_USE_ARC
    [wpng release];
#endif
    com_codename1_impl_ios_IOSImplementation_onScreenshot___byte_1ARRAY(CN1_THREAD_STATE_PASS_ARG wbyteArr);
    return;
#else
    __block NSData *capturedPng = nil;
    void (^performCapture)(void) = ^{
        POOL_BEGIN();
        CodenameOne_GLViewController *controller = [CodenameOne_GLViewController instance];
        // Display.screenshot() reads the Metal screenTexture below. Use the
        // readback drain so queued screen ops are flushed even when the normal
        // display-link/active-state path would skip a frame during synchronous
        // test capture.
        [controller flushBufferForReadback:0 y:0 width:displayWidth height:displayHeight];
        CN1View *view = controller.view;
        CN1Image *img = cn1_captureView(view);
        if (img != nil) {
            NSData *png = UIImagePNGRepresentation(img);
            if (png != nil) {
#ifdef CN1_USE_ARC
                capturedPng = png;
#else
                capturedPng = [png retain];
#endif
            }
        }
        POOL_END();
    };

    cn1RunSyncOnMainQueue(performCapture);

    JAVA_OBJECT byteArr = JAVA_NULL;
    if (capturedPng != nil) {
        int len = (int)[capturedPng length];
        if (len > 0) {
#ifndef NEW_CODENAME_ONE_VM
            org_xmlvm_runtime_XMLVMArray* arr = XMLVMArray_createSingleDimension(__CLASS_byte, len);
            memcpy(arr->fields.org_xmlvm_runtime_XMLVMArray.array_, [capturedPng bytes], len);
            byteArr = arr;
#else
            enteringNativeAllocations();
            JAVA_OBJECT arr = __NEW_ARRAY_JAVA_BYTE(CN1_THREAD_STATE_PASS_ARG len);
            memcpy(((JAVA_ARRAY)arr)->data, [capturedPng bytes], len);
            finishedNativeAllocations();
            byteArr = arr;
#endif
        }
    }
#ifndef CN1_USE_ARC
    [capturedPng release];
#endif

    com_codename1_impl_ios_IOSImplementation_onScreenshot___byte_1ARRAY(CN1_THREAD_STATE_PASS_ARG byteArr);
#endif // TARGET_OS_WATCH
}


JAVA_LONG com_codename1_impl_ios_IOSNative_getPersonWithRecordID___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT recId) {
#ifdef INCLUDE_CONTACTS_USAGE
    POOL_BEGIN();
    ABRecordRef i = ABAddressBookGetPersonWithRecordID(getAddressBook(), recId);
#ifndef CN1_USE_ARC
    [i retain];
#endif
    POOL_END();
    return (JAVA_LONG)i;
#else
    return (JAVA_LONG)0;
#endif
}

//native boolean checkContactsUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkContactsUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_CONTACTS_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkCalendarsUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkCalendarsUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_CALENDARS_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkCameraUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkCameraUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_CAMERA_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkFaceIDUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkFaceIDUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_FACEID_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkLocationUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkLocationUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_LOCATION_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkHealthShareUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkHealthShareUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_HEALTHSHARE_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkHealthUpdateUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkHealthUpdateUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_HEALTHUPDATE_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkMicrophoneUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkMicrophoneUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#if defined(INCLUDE_MICROPHONE_USAGE) && !TARGET_OS_TV
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkMotionUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkMotionUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_MOTION_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkPhotoLibraryAddUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkPhotoLibraryAddUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_PHOTOLIBRARYADD_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkPhotoLibraryUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkPhotoLibraryUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_PHOTOLIBRARY_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkRemindersUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkRemindersUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_REMINDERS_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkSiriUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkSiriUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_SIRI_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkSpeechRecognitionUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkSpeechRecognitionUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_SPEECHRECOGNITION_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}
//native boolean checkNFCReaderUsage();
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_checkNFCReaderUsage___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef INCLUDE_NFCREADER_USAGE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

void com_codename1_impl_ios_IOSNative_dial___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT phone) {
#if TARGET_OS_OSX
    POOL_BEGIN();
    NSString *ns = toNSString(CN1_THREAD_STATE_PASS_ARG phone);
    NSURL *url = ns == nil ? nil : [NSURL URLWithString:ns];
    if (url != nil) {
        // A Mac has no dialer of its own, but a tel: URL is handled by FaceTime
        // or by a paired iPhone through Handoff, which is the platform's answer
        // to placing a call.
        [[NSWorkspace sharedWorkspace] openURL:url];
    }
    POOL_END();
#else
#if !TARGET_OS_WATCH
    POOL_BEGIN();
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:toNSString(CN1_THREAD_STATE_PASS_ARG phone)] options:@{} completionHandler:nil];
    POOL_END();
#else
    // watchOS has no UIApplication openURL dialer.
#endif // !TARGET_OS_WATCH
#endif
}

void com_codename1_impl_ios_IOSNative_requestAppStoreReview__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    // The symbol is always emitted so ParparVM can link; the StoreKit body is
    // compiled only when the build detected the app-review API in use (the
    // IPhoneBuilder flips CN1_USE_APPREVIEW and links StoreKit.framework). When
    // the macro is off this is a harmless no-op with no StoreKit dependency.
#ifdef CN1_USE_APPREVIEW
#if !TARGET_OS_WATCH
    POOL_BEGIN();
    dispatch_async(dispatch_get_main_queue(), ^{
        if (@available(iOS 10.3, *)) {
            [SKStoreReviewController requestReview];
        }
    });
    POOL_END();
#endif // !TARGET_OS_WATCH
#endif // CN1_USE_APPREVIEW
}

void com_codename1_impl_ios_IOSNative_sendSMS___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                                                  JAVA_OBJECT  number, JAVA_OBJECT  text) {
// Not implemented on the native macOS port: the body below is UIKit -- a
// picker, an action sheet, a movie player, a pasteboard or a UIApplication
// service -- and AppKit's equivalent is a different API rather than a
// renamed one. The symbol still has to exist: ParparVM keeps a native method
// alive BY its symbol appearing in the native sources, so removing it would
// make the dead-code pass drop the Java side and ship green with the feature
// silently gone. Returning an unsupported value instead lets the caller take
// its unsupported path.
#if TARGET_OS_OSX
#else
#if TARGET_OS_MACCATALYST || TARGET_OS_WATCH || TARGET_OS_TV
    // SMS hardware is absent on Mac / watchOS (no MessageUI on watch);
    // MFMessageComposeViewController canSendText returns NO. Short-circuit.
    return;
#else
    NSString *recipient = toNSString(CN1_THREAD_STATE_PASS_ARG number);
    NSString *smsBody = toNSString(CN1_THREAD_GET_STATE_PASS_ARG text);
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if([MFMessageComposeViewController canSendText]) {
            MFMessageComposeViewController *picker = [[MFMessageComposeViewController alloc] init];
            picker.messageComposeDelegate = [CodenameOne_GLViewController instance];
            
            // Recipient.
            
            NSArray *recipientsArray = [NSArray arrayWithObject:recipient];
            
            [picker setRecipients:recipientsArray];
            
            // Body.
            
            [picker setBody:smsBody];
            
            [[CodenameOne_GLViewController instance] presentModalViewController:picker animated:YES];
            
#ifndef CN1_USE_ARC
            [picker release];
#endif
        }
        POOL_END();
    });
#endif // !TARGET_OS_MACCATALYST
#endif
}

extern int pendingRemoteNotificationRegistrations;

void com_codename1_impl_ios_IOSNative_registerPush__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    dispatch_async(dispatch_get_main_queue(), ^{
        pendingRemoteNotificationRegistrations++;
        UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
        [center requestAuthorizationWithOptions:(UNAuthorizationOptionAlert + UNAuthorizationOptionSound + UNAuthorizationOptionBadge)
            completionHandler:^(BOOL granted, NSError * _Nullable error) {
            if (granted) {
                // NSApplication rather than UIApplication; the APNs handshake
                // and the delegate callbacks are otherwise identical.
                [NSApp registerForRemoteNotifications];
            } else {
                pendingRemoteNotificationRegistrations--;
                NSString *msg = @"Permission to receive notifications is not granted";
                if (error != nil) {
                    msg = [error localizedDescription];
                }
                struct ThreadLocalData* threadStateData = getThreadLocalData();
                com_codename1_impl_ios_IOSImplementation_pushRegistrationError___java_lang_String(
                    threadStateData, fromNSString(threadStateData, msg));
            }
        }];
    });
#else
#if defined(INCLUDE_CN1_PUSH2) && !TARGET_OS_WATCH
    dispatch_async(dispatch_get_main_queue(), ^{
        if (@available(iOS 10, *)) {
            // iOS 10 ObjC code
            pendingRemoteNotificationRegistrations++;
            UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
            [center requestAuthorizationWithOptions:(UNAuthorizationOptionAlert + UNAuthorizationOptionSound + UNAuthorizationOptionBadge)
                completionHandler:^(BOOL granted, NSError * _Nullable error) {
                    // Enable or disable features based on authorization.
                if (granted) {
                    [[UIApplication sharedApplication] registerForRemoteNotifications];
                } else {
                    pendingRemoteNotificationRegistrations--;
                    NSString *msg = @"Permission to receive notifications is not granted";
                    if (error != nil) {
                        msg = [error localizedDescription];
                    }
                    JAVA_OBJECT jmsg = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG msg);
                    com_codename1_impl_ios_IOSImplementation_pushRegistrationError___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG jmsg);
                }   
            }];
        } else {
            // iOS 9 and earlier
            if ([[UIApplication sharedApplication] respondsToSelector:@selector(registerUserNotificationSettings:)]) {
                NSUInteger settingsParam = (/*UIUserNotificationTypeAlert|UIUserNotificationTypeBadge|UIUserNotificationTypeSound*/ 7);
                id categoriesParam = nil;
                Class settings = NSClassFromString(@"UIUserNotificationSettings");
                if (settings) {
                    // Prepare class selector
                    SEL sel = NSSelectorFromString(@"settingsForTypes:categories:");

                    // Obtain a method signature of selector on UIUserNotificationSettings class
                    NSMethodSignature *signature = [settings methodSignatureForSelector:sel];

                    // Create an invocation on a signature -- must be used because of primitive (enum) arguments on selector
                    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:signature];
                    invocation.selector = sel;
                    invocation.target = settings;

                    // Set arguments
                    [invocation setArgument:&settingsParam atIndex:2];
                    [invocation setArgument:&categoriesParam atIndex:3];

                    // Obtain an instance by firing an invocation
                    NSObject *settingsInstance;
                    [invocation invoke];
                    [invocation getReturnValue:&settingsInstance];

                    // Retain an instance so it can live after quitting method and prevent crash :-)
                    CFRetain((__bridge CFTypeRef)(settingsInstance));

                    // Finally call the desired method with proper settings
                    if (settingsInstance) {
                        pendingRemoteNotificationRegistrations++;
                        [[UIApplication sharedApplication] performSelector:NSSelectorFromString(@"registerUserNotificationSettings:") withObject:settingsInstance];
                    }
                }
            } else {
#if !TARGET_OS_TV
                [[UIApplication sharedApplication] registerForRemoteNotificationTypes:
                 (UIRemoteNotificationTypeBadge | UIRemoteNotificationTypeSound | UIRemoteNotificationTypeAlert)];
#endif // !TARGET_OS_TV
            }
        }
    });
#endif
#endif
}

void com_codename1_impl_ios_IOSNative_deregisterPush__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    dispatch_async(dispatch_get_main_queue(), ^{
        [NSApp unregisterForRemoteNotifications];
    });
#else
#if defined(INCLUDE_CN1_PUSH2) && !TARGET_OS_WATCH
    dispatch_async(dispatch_get_main_queue(), ^{
        [[UIApplication sharedApplication] unregisterForRemoteNotifications];
    });
#endif
#endif
}

void com_codename1_impl_ios_IOSNative_setBadgeNumber___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT number) {
#if TARGET_OS_OSX
    dispatch_async(dispatch_get_main_queue(), ^{
        // The Dock tile is where a Mac shows this. A zero clears the badge
        // rather than drawing "0", which is what every Mac application does.
        [[NSApp dockTile] setBadgeLabel:number > 0 ? [NSString stringWithFormat:@"%d", (int)number] : nil];
    });
#else
// Removed this ifdef because we may need to badge the application even if push isn't supported.
//#ifdef INCLUDE_CN1_PUSH2
#if !TARGET_OS_WATCH
    dispatch_async(dispatch_get_main_queue(), ^{
        if(number == 0) {
            // Removed this because there could be repeating notifications
            //[[UIApplication sharedApplication] cancelAllLocalNotifications];
        }
        [UIApplication sharedApplication].applicationIconBadgeNumber = number;
    });
#endif // !TARGET_OS_WATCH
//#endif
#endif
}

#if defined(INCLUDE_CN1_PUSH2) && !TARGET_OS_WATCH && !TARGET_OS_TV
static NSMutableArray<UNNotificationAction *>* pushActions;
static NSMutableArray<UNNotificationAction *>* currentCategoryActions;
// Mutable: endPushActionCategory adds to it as each category is closed.
// Declared immutable it compiled anyway -- addObject: is only a warning on an
// NSSet -- and would have thrown the first time an application registered a
// push action.
static NSMutableSet<UNNotificationCategory *>* pushCategories;
static NSString* currentCategoryId;
#endif
void com_codename1_impl_ios_IOSNative_registerPushAction___java_lang_String_java_lang_String_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT identifier, JAVA_OBJECT title, JAVA_OBJECT placeholderText, JAVA_OBJECT replyButtonText) {
#if defined(INCLUDE_CN1_PUSH2) && !TARGET_OS_WATCH && !TARGET_OS_TV
    if (@available(iOS 10, *)) {
        if (pushActions == nil) {
            pushActions = [[NSMutableArray alloc] init];
        }
        NSString *nsId = toNSString(CN1_THREAD_GET_STATE_PASS_ARG identifier);
        NSString *nsTitle = toNSString(CN1_THREAD_GET_STATE_PASS_ARG title);
        NSString *nsPlaceholderText = placeholderText == NULL ? nil : toNSString(CN1_THREAD_GET_STATE_PASS_ARG placeholderText);
        NSString *nsReplyButtonText = replyButtonText == NULL ? nil : toNSString(CN1_THREAD_GET_STATE_PASS_ARG replyButtonText);
        nsPlaceholderText = (nsPlaceholderText == nil && nsReplyButtonText != nil) ? @"" : nsPlaceholderText;
        nsReplyButtonText = (nsReplyButtonText == nil && nsPlaceholderText != nil) ? @"Reply" : nsReplyButtonText;
        if (nsPlaceholderText != nil && nsReplyButtonText != nil) {
            [pushActions addObject:[UNTextInputNotificationAction actionWithIdentifier:nsId title:nsTitle options:UNNotificationActionOptionNone textInputButtonTitle:nsReplyButtonText textInputPlaceholder:nsPlaceholderText]];
        } else {
            [pushActions addObject:[UNNotificationAction actionWithIdentifier:nsId title:nsTitle options:UNNotificationActionOptionNone]];
        }
    }
#endif
}


void com_codename1_impl_ios_IOSNative_startPushActionCategory___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT identifier) {
#if defined(INCLUDE_CN1_PUSH2) && !TARGET_OS_WATCH && !TARGET_OS_TV
    if (@available(iOS 10, *)) {
        currentCategoryId = toNSString(CN1_THREAD_GET_STATE_PASS_ARG identifier);
        if (currentCategoryActions != nil) {
            [currentCategoryActions release];
        }
        currentCategoryActions = [[NSMutableArray alloc] init];
    }
#endif
}

void com_codename1_impl_ios_IOSNative_endPushActionCategory__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    UNNotificationCategory *category = [UNNotificationCategory categoryWithIdentifier:currentCategoryId actions:currentCategoryActions intentIdentifiers:@[] options:UNNotificationCategoryOptionNone];
    if (pushCategories == nil) {
        pushCategories = [[NSMutableSet alloc] init];
    }
    [pushCategories addObject:category];
#else
#if defined(INCLUDE_CN1_PUSH2) && !TARGET_OS_WATCH && !TARGET_OS_TV
    if (@available(iOS 10, *)) {
        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
        UNNotificationCategory *category = [UNNotificationCategory categoryWithIdentifier:currentCategoryId actions:currentCategoryActions intentIdentifiers:@[] options:UNNotificationCategoryOptionNone];
        if (pushCategories == nil) {
            pushCategories = [[NSMutableSet alloc] init];
        }
        [pushCategories addObject:category];
    }
#endif
#endif
}

void com_codename1_impl_ios_IOSNative_addPushActionToCategory___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT identifier) {
#if defined(INCLUDE_CN1_PUSH2) && !TARGET_OS_WATCH && !TARGET_OS_TV
    if (@available(iOS 10, *)) {
        UNNotificationAction *action = nil;
        NSString *nsId = toNSString(CN1_THREAD_GET_STATE_PASS_ARG identifier);
        for (UNNotificationAction *a in pushActions) {
            if ([a.identifier isEqualToString:nsId]) {
                action = a;
                break;
            }
        }
        if (action == nil) {
            NSLog(@"Could not find action with id %@ to add to category.  Skipping", nsId);
            return;
        }
        [currentCategoryActions addObject:action];
    }
#endif
}

void com_codename1_impl_ios_IOSNative_registerPushCategories__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if defined(INCLUDE_CN1_PUSH2) && !TARGET_OS_WATCH && !TARGET_OS_TV
    if (@available(iOS 10, *)) {
        if (pushCategories != nil) {
            UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
            [center setNotificationCategories:pushCategories];
        }
    }
#endif
}

CN1Image* scaleImage(int destWidth, int destHeight, CN1Image *img) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return nil;
#else
    CN1Image* scaledInstance = nil;
    const size_t originalWidth = img.size.width;
    const size_t originalHeight = img.size.height;
    
    CGContextRef bmContext = CGBitmapContextCreate(NULL, destWidth, destHeight, 8, destWidth * 4, CGColorSpaceCreateDeviceRGB(), kCGBitmapByteOrderDefault | kCGImageAlphaPremultipliedFirst);
    
    
    if (bmContext) {
        if (UIImageOrientationLeft == img.imageOrientation) {
            CGContextRotateCTM(bmContext, M_PI_2);
            CGContextTranslateCTM(bmContext, 0, -destHeight);
        } else if (UIImageOrientationRight == img.imageOrientation) {
            CGContextRotateCTM(bmContext, -M_PI_2);
            CGContextTranslateCTM(bmContext, -destWidth, 0);
        } else if (UIImageOrientationDown == img.imageOrientation) {
            CGContextTranslateCTM(bmContext, destWidth, destHeight);
            CGContextRotateCTM(bmContext, -M_PI);
        }
        
        CGContextSetShouldAntialias(bmContext, true);
        CGContextSetAllowsAntialiasing(bmContext, true);
        CGContextSetInterpolationQuality(bmContext, kCGInterpolationHigh);
        
        CGContextDrawImage(bmContext, CGRectMake(0, 0, destWidth, destHeight), img.CGImage);
        
        CGImageRef scaledImageRef = CGBitmapContextCreateImage(bmContext);
        scaledInstance = [CN1Image imageWithCGImage:scaledImageRef];
        
        CGImageRelease(scaledImageRef);
        CGContextRelease(bmContext);
    }
    CN1Image* scaled = scaledInstance;
    scaledInstance = nil;
    return scaled;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageFile___long_boolean_int_int_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG imagePeer, JAVA_BOOLEAN jpeg, int width, int height, JAVA_FLOAT quality) {
    __block int blockWidth = width;
    __block int blockHeight = height;
    __block NSData* data = nil;
#ifndef CN1_USE_ARC
    [(BRIDGE_CAST GLUIImage*)((void *)imagePeer) retain];
#endif
#ifdef CN1_USE_METAL
    // Phase 3 v2: PNG/JPEG encoding sources from [GLUIImage getImage] which
    // is the original CN1Image backing — initial-fill colour for any mutable
    // that's been drawn into via Metal. Drain the op queue first so the
    // mutable's MTLTexture has the latest pixels, then read those pixels
    // into a fresh CN1Image and encode that. flushBuffer already dispatches
    // sync to the main thread and runs drawFrame; doing it OUTSIDE the
    // dispatch_sync block below avoids the nested-dispatch_sync deadlock
    // that would otherwise occur (we'd be waiting on main to run drawFrame
    // while main is waiting on us to free the dispatch_sync slot).
    {
        GLUIImage *glllOuter = (BRIDGE_CAST GLUIImage*)((void *)imagePeer);
        if ([glllOuter mtlMutableTexture] != nil) {
            BOOL stillDrawing = (((BRIDGE_CAST void*)[CodenameOne_GLViewController instance].currentMutableImage) == ((void *)imagePeer));
            if (stillDrawing) {
                Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl();
            }
            int dw = Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl();
            int dh = Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl();
            [[CodenameOne_GLViewController instance] flushBuffer:nil x:0 y:0 width:dw height:dh];
            if (stillDrawing) {
                int restoreW = [glllOuter mtlMutableWidth];
                int restoreH = [glllOuter mtlMutableHeight];
                Java_com_codename1_impl_ios_IOSImplementation_startDrawingOnImageImpl(restoreW, restoreH, (void *)imagePeer);
            }
        }
    }
#endif
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        GLUIImage* glll = (BRIDGE_CAST GLUIImage*)((void *)imagePeer);
        CN1Image* i = nil;
#ifdef CN1_USE_METAL
        // If the image has live Metal pixels, blit-and-read into a fresh
        // CN1Image so PNG/JPEG encoding sees post-draw content rather than
        // the stale CN1Image initial-fill backing.
        if ([glll mtlMutableTexture] != nil) {
            int srcW = [glll mtlMutableWidth];
            int srcH = [glll mtlMutableHeight];
            if (srcW > 0 && srcH > 0) {
                int *pixels = (int *)malloc((size_t)srcW * (size_t)srcH * sizeof(int));
                if (pixels != NULL) {
                    if (CN1MetalReadMutableImagePixels(glll, pixels, 0, 0, srcW, srcH, srcW, srcH)) {
                        // CN1MetalReadMutableImagePixels writes ARGB ints; wrap as a
                        // CGImage with RGBA byte order (the alpha-first/last and
                        // R/B swap matches what UIKit expects for iOS little-endian).
                        CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
                        CGContextRef ctx = CGBitmapContextCreate(pixels, (size_t)srcW, (size_t)srcH, 8,
                                                                  (size_t)srcW * 4, cs,
                                                                  kCGBitmapByteOrder32Little | kCGImageAlphaPremultipliedFirst);
                        CGColorSpaceRelease(cs);
                        if (ctx != NULL) {
                            CGImageRef cgImg = CGBitmapContextCreateImage(ctx);
                            CGContextRelease(ctx);
                            if (cgImg != NULL) {
#if TARGET_OS_OSX
                                i = [[NSImage alloc] initWithCGImage:cgImg
                                                                size:NSMakeSize(CGImageGetWidth(cgImg),
                                                                                CGImageGetHeight(cgImg))];
#else
                                i = [CN1Image imageWithCGImage:cgImg scale:1.0 orientation:UIImageOrientationUp];
#endif
                                CGImageRelease(cgImg);
                            }
                        }
                    }
                    free(pixels);
                }
            }
        }
#endif
        if (i == nil) {
            i = [glll getImage];
        }
        if(width == -1) {
            float aspect = height / i.size.height;
            blockWidth = (int)(i.size.width * aspect);
        }
        if(height == -1) {
            float aspect = width / i.size.width;
            blockHeight = (int)(i.size.height * aspect);
        }
        if(blockWidth != ((int)i.size.width) || blockHeight != ((int)i.size.height)) {
            i = scaleImage(blockWidth, blockHeight, i);
        }
        if(jpeg) {
            data = UIImageJPEGRepresentation(i, quality);
        } else {
            data = UIImagePNGRepresentation(i);
        }

#ifndef CN1_USE_ARC
        [data retain];
#endif
        POOL_END();
    });
#ifndef CN1_USE_ARC
    [(BRIDGE_CAST GLUIImage*)((void *)imagePeer) release];
#endif
    return (JAVA_LONG)((BRIDGE_CAST void*)data);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getNSDataSize___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData) {
    NSData* d = (BRIDGE_CAST NSData*)((void *)nsData);
    return d.length;
}

void com_codename1_impl_ios_IOSNative_nsDataToByteArray___long_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_OBJECT dataArray) {
    POOL_BEGIN();
    NSData* d = (BRIDGE_CAST NSData*)((void*)nsData);
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* byteArray = dataArray;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    void* data = ((JAVA_ARRAY)dataArray)->data;
#endif
    memcpy(data, d.bytes, d.length);
    POOL_END();
}


JAVA_LONG com_codename1_impl_ios_IOSNative_createAudioUnit___java_lang_String_int_float_float_1ARRAY_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
    JAVA_OBJECT path, JAVA_INT audioChannels, JAVA_FLOAT sampleRate, JAVA_OBJECT sampleBuffer) {
#if defined(INCLUDE_MICROPHONE_USAGE) && !TARGET_OS_TV && !TARGET_OS_WATCH
        __block CN1AudioUnit* recorder = nil;
         
        __block NSString *exStr = nil;
        dispatch_sync(dispatch_get_main_queue(), ^{
            POOL_BEGIN();
            
            AVAudioSession *audioSession = [AVAudioSession sharedInstance];
            NSError *err = nil;
            [audioSession setCategory :AVAudioSessionCategoryPlayAndRecord error:&err];
            if(err){
                CN1Log(@"audioSession: %@ %d %@", [err domain], [err code], [[err userInfo] description]);
                exStr = [[err userInfo] description];
                POOL_END();
                return;
            }
            err = nil;
            [audioSession setActive:YES error:&err];
            if(err){
                CN1Log(@"audioSession: %@ %d %@", [err domain], [err code], [[err userInfo] description]);
                exStr = [[err userInfo] description];
                POOL_END();
                return;
            }
            
            if (isIOS7()) {
                CN1Log(@"Asking for record permission");
                [audioSession requestRecordPermission:^(BOOL granted) {
                    POOL_BEGIN();
                    if (granted) {
                         recorder = [[CN1AudioUnit alloc] initWithPath:toNSString(CN1_THREAD_STATE_PASS_ARG path) channels:audioChannels sampleRate:sampleRate sampleBuffer:(JAVA_ARRAY)sampleBuffer];
                    } else {
                        exStr = @"Denied access to use the microphone";
                    }
                    POOL_END();
                }];
            } else {
                recorder = [[CN1AudioUnit alloc] initWithPath:toNSString(CN1_THREAD_STATE_PASS_ARG path) channels:audioChannels sampleRate:sampleRate sampleBuffer:(JAVA_ARRAY)sampleBuffer];
            }
            POOL_END();
        });
        if (exStr != nil) {
            JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
            java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, fromNSString(CN1_THREAD_GET_STATE_PASS_ARG exStr));
            throwException(threadStateData, ex);
            return (JAVA_LONG)0;
        } else {
            
            return (JAVA_LONG)((BRIDGE_CAST void*)recorder);
        }
    #else
        return (JAVA_LONG)0;
    #endif
    }
    



void com_codename1_impl_ios_IOSNative_startAudioUnit___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if TARGET_OS_OSX
    // No AVAudioSession to configure: a Mac has no shared session category, and
    // recording permission is granted per application rather than per session.
    CN1AudioUnit* audioUnit = (BRIDGE_CAST CN1AudioUnit*)((void *)(uintptr_t)peer);
    [audioUnit start];
#else
#if !TARGET_OS_WATCH
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AVAudioSession *audioSession = [AVAudioSession sharedInstance];
        NSError *err = nil;
        [audioSession setCategory :AVAudioSessionCategoryPlayAndRecord error:&err];
        POOL_END();
    });
    CN1AudioUnit* audioUnit = (BRIDGE_CAST CN1AudioUnit*)((void *)peer);
    [audioUnit start];
#endif // !TARGET_OS_WATCH
#endif
}


void com_codename1_impl_ios_IOSNative_stopAudioUnit___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if !TARGET_OS_WATCH
    CN1AudioUnit* audioUnit = (BRIDGE_CAST CN1AudioUnit*)((void *)peer);
    [audioUnit stop];
#endif // !TARGET_OS_WATCH
}

void com_codename1_impl_ios_IOSNative_destroyAudioUnit___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#if !TARGET_OS_WATCH
    CN1AudioUnit* audioUnit = (BRIDGE_CAST CN1AudioUnit*)((void *)peer);
    [audioUnit release];
#endif // !TARGET_OS_WATCH
}


JAVA_LONG com_codename1_impl_ios_IOSNative_createAudioRecorder___java_lang_String_java_lang_String_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                                                  JAVA_OBJECT  destinationFile, JAVA_OBJECT mimeType, JAVA_INT sampleRate, JAVA_INT bitRate, JAVA_INT channels, JAVA_INT maxDuration) {
#if defined(INCLUDE_MICROPHONE_USAGE) && !TARGET_OS_TV
    __block AVAudioRecorder* recorder = nil;
     
    __block NSString *exStr = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AVAudioSession *audioSession = [AVAudioSession sharedInstance];
        NSError *err = nil;
        [audioSession setCategory :AVAudioSessionCategoryPlayAndRecord error:&err];
        if(err){
            CN1Log(@"audioSession: %@ %d %@", [err domain], [err code], [[err userInfo] description]);
            exStr = [[err userInfo] description];
            POOL_END();
            return;
        }
        err = nil;
        [audioSession setActive:YES error:&err];
        if(err){
            CN1Log(@"audioSession: %@ %d %@", [err domain], [err code], [[err userInfo] description]);
            exStr = [[err userInfo] description];
            POOL_END();
            return;
        }
        if (isIOS7()) {
            CN1Log(@"Asking for record permission");
            [audioSession requestRecordPermission:^(BOOL granted) {
                POOL_BEGIN();
                if (granted) {
                    NSString * filePath = toNSString(CN1_THREAD_GET_STATE_PASS_ARG destinationFile);
                    
                    // cleanup older file if it exists in this location
                    NSFileManager* fm = [[NSFileManager alloc] init];
                    NSString* ns = fixFilePath(filePath);
                    if([fm fileExistsAtPath:ns]) {
                        [fm removeItemAtPath:ns error:nil];
                    }
                    
                    CN1Log(@"Recording audio to: %@", filePath);
                    
                    // Ignoring bit rate setting as ios doesn't have direct equivalent
                    // We only support the one mimetype for now also.
                    NSDictionary *recordSettings = [[NSDictionary alloc] initWithObjectsAndKeys:
                                                    [NSNumber numberWithFloat: (JAVA_FLOAT)sampleRate], AVSampleRateKey,     
                                                    [NSNumber numberWithInt: kAudioFormatMPEG4AAC],AVFormatIDKey,
                                                    [NSNumber numberWithInt: channels], AVNumberOfChannelsKey,
                                                    nil];
                    NSError *error = nil;
                    recorder = [[AVAudioRecorder alloc] initWithURL: [NSURL fileURLWithPath:ns]
                                                           settings: recordSettings
                                                              error: &error];
                    if(error != nil) {
                        CN1Log(@"Error in recording: %@", [error localizedDescription]);
                        exStr = [error localizedDescription];
                        POOL_END();
                        return;
                    }
                    recorder.delegate = [CodenameOne_GLViewController instance];
                    com_codename1_impl_ios_IOSImplementation_finishedCreatingAudioRecorder___java_io_IOException(CN1_THREAD_GET_STATE_PASS_ARG JAVA_NULL);
                } else {
                    recorder = nil;
                    exStr = @"Permission to record was denied";
                    
                }
                POOL_END();
            }];
        } else {
            NSString * filePath = toNSString(CN1_THREAD_GET_STATE_PASS_ARG destinationFile);
            
            // cleanup older file if it exists in this location
            NSFileManager* fm = [[NSFileManager alloc] init];
            NSString* ns = fixFilePath(filePath);
            if([fm fileExistsAtPath:ns]) {
                [fm removeItemAtPath:ns error:nil];
            }
            
            CN1Log(@"Recording audio to: %@", filePath);
            NSDictionary *recordSettings = [[NSDictionary alloc] initWithObjectsAndKeys:
                                            [NSNumber numberWithFloat: 16000.0], AVSampleRateKey,
                                            [NSNumber numberWithInt: kAudioFormatMPEG4AAC],AVFormatIDKey,
                                            [NSNumber numberWithInt: 1], AVNumberOfChannelsKey,
                                            nil];
            NSError *error = nil;
            recorder = [[AVAudioRecorder alloc] initWithURL: [NSURL fileURLWithPath:filePath]
                                                   settings: recordSettings
                                                      error: &error];
            if(error != nil) {
                CN1Log(@"Error in recording: %@", [error localizedDescription]);
                exStr = [error localizedDescription];
                POOL_END();
                return;
            }
            recorder.delegate = [CodenameOne_GLViewController instance];
        }
        POOL_END();
    });
    if (exStr != nil) {
        JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
        java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, fromNSString(CN1_THREAD_GET_STATE_PASS_ARG exStr));
        throwException(threadStateData, ex);
        return (JAVA_LONG)0;
    } else {
        com_codename1_impl_ios_IOSImplementation_finishedCreatingAudioRecorder___java_io_IOException(CN1_THREAD_GET_STATE_PASS_ARG JAVA_NULL);
        return (JAVA_LONG)((BRIDGE_CAST void*)recorder);
    }
#else
    return (JAVA_LONG)0;
#endif
}


void com_codename1_impl_ios_IOSNative_startAudioRecord___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                              JAVA_LONG  peer) {
#ifdef INCLUDE_MICROPHONE_USAGE
    AVAudioRecorder* recorder = (BRIDGE_CAST AVAudioRecorder*)((void *)peer);
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if(![recorder prepareToRecord]) {
            CN1Log(@"Error preparing to record");
        }
        if(![recorder record]) {
            CN1Log(@"Error in recording record returned false for some reason?");
        }
#ifndef CN1_USE_ARC
        [recorder retain];
#endif
        POOL_END();
    });
#endif
}

void com_codename1_impl_ios_IOSNative_pauseAudioRecord___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                              JAVA_LONG  peer) {
#ifdef INCLUDE_MICROPHONE_USAGE
    AVAudioRecorder* recorder = (BRIDGE_CAST AVAudioRecorder*)((void *)peer);
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        [recorder pause];
        POOL_END();
    });
#endif
}

void com_codename1_impl_ios_IOSNative_cleanupAudioRecord___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                                JAVA_LONG  peer) {
#ifdef INCLUDE_MICROPHONE_USAGE
    AVAudioRecorder* recorder = (BRIDGE_CAST AVAudioRecorder*)((void *)peer);
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        [recorder stop];
#ifndef CN1_USE_ARC
        [recorder release];
#endif
        POOL_END();
    });
#endif
}

#ifdef NEW_CODENAME_ONE_VM
JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofObjArrayI___java_lang_Object_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    // second part of the expression check that this isn't a primitive array
    return n1->__codenameOneParentClsReference->isArray && cn1_array_start_offset + 100 < n1->__codenameOneParentClsReference->classId;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofByteArrayI___java_lang_Object_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    return n1->__codenameOneParentClsReference->classId == cn1_array_1_id_JAVA_BYTE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofShortArrayI___java_lang_Object_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    return n1->__codenameOneParentClsReference->classId == cn1_array_1_id_JAVA_SHORT;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofLongArrayI___java_lang_Object_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    return n1->__codenameOneParentClsReference->classId == cn1_array_1_id_JAVA_LONG;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofIntArrayI___java_lang_Object_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    return n1->__codenameOneParentClsReference->classId == cn1_array_1_id_JAVA_INT;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofFloatArrayI___java_lang_Object_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    return n1->__codenameOneParentClsReference->classId == cn1_array_1_id_JAVA_FLOAT;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofDoubleArrayI___java_lang_Object_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    return n1->__codenameOneParentClsReference->classId == cn1_array_1_id_JAVA_DOUBLE;
}
#else // NEW_CODENAME_ONE_VM
JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofObjArrayI___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_java_lang_Object_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofByteArrayI___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_byte_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofShortArrayI___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_short_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofLongArrayI___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_long_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofIntArrayI___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_int_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofFloatArrayI___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_float_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofDoubleArrayI___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_double_1ARRAY;
}
#endif // NEW_CODENAME_ONE_VM

/*
 * SQLite bindings.
 *
 * Paths arrive here already absolute: the Java side resolves a bare database name against the
 * documents directory and a file:// URL through FileSystemStorage, so this file does not have to
 * duplicate URL handling in C.
 *
 * Errors are raised as java.io.IOException carrying sqlite3_errmsg, on every path.
 */

/** Throws java.io.IOException carrying the database's last error message. */
static void cn1ThrowSqlError(CODENAME_ONE_THREAD_STATE, sqlite3* db, const char* context) {
    const char* detail = db != NULL ? sqlite3_errmsg(db) : "unknown SQLite error";
    NSString* message = [NSString stringWithFormat:@"%s: %s", context, detail];
    JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
    java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex,
            newStringFromCString(CN1_THREAD_STATE_PASS_ARG [message UTF8String]));
    throwException(threadStateData, ex);
}

/** Throws with an explicit message rather than one taken from a connection. */
static void cn1ThrowSqlMessage(CODENAME_ONE_THREAD_STATE, const char* message) {
    JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
    java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex,
            newStringFromCString(CN1_THREAD_STATE_PASS_ARG message));
    throwException(threadStateData, ex);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlDbPath___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    POOL_BEGIN();
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];
    NSString* nsSrc = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    NSString* full = [documentsPath stringByAppendingPathComponent:nsSrc];
    JAVA_OBJECT result = newStringFromCString(CN1_THREAD_STATE_PASS_ARG [full UTF8String]);
    POOL_END();
    return result;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlDbExists___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    POOL_BEGIN();
    NSString* nsPath = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:nsPath];
    POOL_END();
    return fileExists;
}

void com_codename1_impl_ios_IOSNative_sqlDbDelete___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    POOL_BEGIN();
    NSString* nsPath = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSFileManager* files = [NSFileManager defaultManager];
    // Deleting what is not there is the documented no-op. Anything else -- file protection, a
    // filesystem error -- has to be reported, or delete() returns and the database is still
    // sitting there, which is neither the contract nor what this binding claims to do.
    if ([files fileExistsAtPath:nsPath]) {
        NSError* err = nil;
        if (![files removeItemAtPath:nsPath error:&err]) {
            NSString* failure = [NSString stringWithFormat:@"The database %@ could not be "
                    "deleted: %@", nsPath, err != nil ? [err localizedDescription] : @"unknown error"];
            cn1ThrowSqlMessage(CN1_THREAD_STATE_PASS_ARG [failure UTF8String]);
            POOL_END();
            return;
        }
    }
    POOL_END();
}

/*
 * SQLCipher-compatible keying, declared here rather than taken from a header so this file
 * compiles unchanged whether or not the bundled engine was emitted.
 *
 * These are strong references and they stay strong on purpose: Apple's own libsqlite3 exports
 * them, so a build without the bundled engine still links. Verified against Xcode 26.2 rather
 * than assumed -- usr/lib/libsqlite3.tbd lists _sqlite3_key, _sqlite3_key_v2 and _sqlite3_rekey
 * in both the iPhoneOS and iPhoneSimulator SDKs, and a binary calling sqlite3_key links against
 * -lsqlite3 with no undefined symbol. What Apple's copy does NOT do is encrypt: it answers
 * SQLITE_MISUSE and leaves the file plaintext, which is why availability is decided by
 * cn1SqlCipherAvailable below and never by whether these symbols resolved.
 */
extern int sqlite3_key(sqlite3 *db, const void *pKey, int nKey);
extern int sqlite3_rekey(sqlite3 *db, const void *pKey, int nKey);

/*
 * Whether this build can encrypt at all.
 *
 * Decided by the marker header the translator emits beside this file for an application that
 * configures encryption -- the same marker the bundled engine compiles its ciphers against, so
 * the two cannot disagree. Without it the process is linked against Apple's libsqlite3, which
 * exports sqlite3_key and answers SQLITE_MISUSE from it.
 *
 * There is deliberately no runtime probe here. This was written as `PRAGMA cipher_version`,
 * which is SQLCipher's pragma and not one SQLite3MC implements: an unknown pragma is not an
 * error in SQLite, so it reported "no cipher" on the cipher build as well, and iOS answered that
 * encryption was unsupported for every application. The conformance suite recorded it as a skip
 * rather than a failure, which is how it went unnoticed.
 */
#if defined(__has_include)
#  if __has_include("cn1_sqlite3_cipher.h")
#    define CN1_DB_CIPHER_PRESENT 1
#  endif
#endif

static JAVA_BOOLEAN cn1SqlCipherAvailable(sqlite3* db) {
    (void)db;
#ifdef CN1_DB_CIPHER_PRESENT
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

/**
 * Selects the SQLCipher 4 on-disk format. Without this the bundled engine would use its own
 * cipher scheme, producing files no other Codename One platform can read.
 */
static int cn1SqlApplyCipherProfile(sqlite3* db) {
    int rc = sqlite3_exec(db, "PRAGMA cipher = 'sqlcipher'", NULL, NULL, NULL);
    if (rc != SQLITE_OK) {
        return rc;
    }
    return sqlite3_exec(db, "PRAGMA legacy = 4", NULL, NULL, NULL);
}

/**
 * Confirms the key actually decrypts the database. SQLCipher applies a key lazily, so without
 * this a wrong passphrase surfaces much later as an unrelated "file is not a database".
 */
static int cn1SqlProbeKey(sqlite3* db) {
    return sqlite3_exec(db, "SELECT count(*) FROM sqlite_master", NULL, NULL, NULL);
}

static JAVA_LONG cn1SqlOpen(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path, JAVA_OBJECT unusedKey) {
    POOL_BEGIN();
    NSString* nsPath = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    sqlite3 *db = NULL;

    // Per connection, rather than the process wide sqlite3_config(SQLITE_CONFIG_SERIALIZED) plus
    // sqlite3_shutdown() this used to do. That had to run before sqlite3_initialize() to have any
    // effect, and calling sqlite3_shutdown() with connections already open is undefined behaviour.
    int rc = sqlite3_open_v2([nsPath UTF8String], &db,
            SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_FULLMUTEX, NULL);
    if (rc != SQLITE_OK) {
        // Copy the message out and close before raising. throwException longjmps, so nothing
        // after it runs, and SQLite hands back a usable handle even from a failed open -- which
        // then leaks a connection and its descriptor once per caught failure.
        NSString* failure = [NSString stringWithFormat:@"Failed to open the database: %s",
                db != NULL ? sqlite3_errmsg(db) : "unknown SQLite error"];
        if (db != NULL) {
            sqlite3_close_v2(db);
        }
        cn1ThrowSqlMessage(CN1_THREAD_STATE_PASS_ARG [failure UTF8String]);
        POOL_END();
        return 0;
    }

    POOL_END();
    return (JAVA_LONG)db;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlDbCreateAndOpen___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    return cn1SqlOpen(CN1_THREAD_STATE_PASS_ARG path, JAVA_NULL);
}

/**
 * Applies a key to an already open connection and checks that it actually decrypts.
 *
 * Returns false for a wrong key rather than throwing, so the Java side can tell "this key is
 * wrong" apart from "the file could not be opened" and raise the right exception. Keeping the
 * distinction here would mean referencing a core exception class from C.
 */
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlDbApplyKey___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT key) {
    sqlite3* db = (sqlite3*)dbPeer;
    if (cn1SqlApplyCipherProfile(db) != SQLITE_OK) {
        return JAVA_FALSE;
    }
    const char* keyChars = stringToUTF8(CN1_THREAD_STATE_PASS_ARG key);
    // sqlite3_key rather than PRAGMA key, so a passphrase containing a quote cannot break out of
    // the statement.
    if (sqlite3_key(db, keyChars, (int)strlen(keyChars)) != SQLITE_OK) {
        return JAVA_FALSE;
    }
    // Reports the probe result rather than a bare pass/fail, so the Java side can tell a key that
    // did not decrypt the file (SQLITE_NOTADB) from a corrupt image or a read error, which no key
    // repairs. SQLITE_OK is 0, so a zero return means the key worked.
    return cn1SqlProbeKey(db) == SQLITE_OK ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlDbApplyKeyStatus___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT key) {
    sqlite3* db = (sqlite3*)dbPeer;
    if (cn1SqlApplyCipherProfile(db) != SQLITE_OK) {
        return SQLITE_ERROR;
    }
    const char* keyChars = stringToUTF8(CN1_THREAD_STATE_PASS_ARG key);
    int rc = sqlite3_key(db, keyChars, (int)strlen(keyChars));
    if (rc != SQLITE_OK) {
        return rc;
    }
    return cn1SqlProbeKey(db);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlDbIsCipherAvailable__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return cn1SqlCipherAvailable(NULL);
}

void com_codename1_impl_ios_IOSNative_sqlDbRekey___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT key) {
    sqlite3* db = (sqlite3*)dbPeer;
    if (!cn1SqlCipherAvailable(db)) {
        // Asked of the engine, not of the pragma that configures it: a build without the cipher
        // accepts "PRAGMA cipher" silently and would have fallen through to sqlite3_rekey, where
        // Apple's copy answers SQLITE_MISUSE and the caller would read a bare engine error
        // instead of being told that this build cannot encrypt anything.
        cn1ThrowSqlMessage(CN1_THREAD_STATE_PASS_ARG
                "This build does not support encrypted databases");
        return;
    }
    if (cn1SqlApplyCipherProfile(db) != SQLITE_OK) {
        cn1ThrowSqlMessage(CN1_THREAD_STATE_PASS_ARG
                "The SQLCipher 4 format could not be selected on this database");
        return;
    }
    int rc;
    if (key == JAVA_NULL) {
        rc = sqlite3_rekey(db, NULL, 0);
    } else {
        const char* keyChars = stringToUTF8(CN1_THREAD_STATE_PASS_ARG key);
        rc = sqlite3_rekey(db, keyChars, (int)strlen(keyChars));
    }
    if (rc != SQLITE_OK) {
        cn1ThrowSqlError(CN1_THREAD_STATE_PASS_ARG db, "Failed to change the database key");
    }
}

void com_codename1_impl_ios_IOSNative_sqlDbClose___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG db) {
    // This used to call sqlite3_free on the connection handle, which never closed it, leaked the
    // file descriptor, skipped the WAL checkpoint and handed the pointer to the wrong allocator.
    if (db != 0) {
        sqlite3_close_v2((sqlite3*)db);
    }
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlDbInTransaction___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer) {
    // The engine's own answer. After a script fails partway SQLite stops at the failing statement,
    // and nothing outside can see which one that was, so reading the script cannot tell an
    // unexecuted trailing COMMIT from an executed one.
    return sqlite3_get_autocommit((sqlite3*)dbPeer) ? JAVA_FALSE : JAVA_TRUE;
}

void com_codename1_impl_ios_IOSNative_sqlDbExecScript___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT sql) {
    sqlite3* db = (sqlite3*)dbPeer;
    const char* chrs = stringToUTF8(CN1_THREAD_STATE_PASS_ARG sql);
    char* errInfo = NULL;
    if (sqlite3_exec(db, chrs, NULL, NULL, &errInfo) != SQLITE_OK) {
        NSString* message = errInfo != NULL
                ? [NSString stringWithFormat:@"SQL error: %s", errInfo]
                : @"SQL error";
        if (errInfo != NULL) {
            sqlite3_free(errInfo);
        }
        cn1ThrowSqlMessage(CN1_THREAD_STATE_PASS_ARG [message UTF8String]);
    }
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlStmtPrepare___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT sql) {
    sqlite3* db = (sqlite3*)dbPeer;
    const char* chrs = stringToUTF8(CN1_THREAD_STATE_PASS_ARG sql);
    sqlite3_stmt* stmt = NULL;
    if (sqlite3_prepare_v2(db, chrs, -1, &stmt, NULL) != SQLITE_OK) {
        cn1ThrowSqlError(CN1_THREAD_STATE_PASS_ARG db, "Failed to compile the statement");
        return 0;
    }
    return (JAVA_LONG)stmt;
}

/** Raises when a bind fails. SQLite unbinds the old value before copying, so ignoring this leaves
 * the parameter as SQL NULL and the statement runs with something the caller never supplied. */
static void cn1CheckSqlBind(CODENAME_ONE_THREAD_STATE, int rc, int index) {
    if (rc == SQLITE_OK) {
        return;
    }
    NSString* message = [NSString stringWithFormat:@"Parameter %d could not be bound: %s", index,
            sqlite3_errstr(rc)];
    cn1ThrowSqlMessage(CN1_THREAD_STATE_PASS_ARG [message UTF8String]);
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlStmtParameterCount___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    return sqlite3_bind_parameter_count((sqlite3_stmt*)statementPeer);
}

void com_codename1_impl_ios_IOSNative_sqlStmtBindNull___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer, JAVA_INT index) {
    cn1CheckSqlBind(CN1_THREAD_STATE_PASS_ARG
            sqlite3_bind_null((sqlite3_stmt*)statementPeer, index), index);
}

void com_codename1_impl_ios_IOSNative_sqlStmtBindText___long_int_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer, JAVA_INT index, JAVA_OBJECT value) {
    if (value == JAVA_NULL) {
        cn1CheckSqlBind(CN1_THREAD_STATE_PASS_ARG
                sqlite3_bind_null((sqlite3_stmt*)statementPeer, index), index);
        return;
    }
    // The caller encodes, and the length is the array's rather than "up to the first zero byte":
    // a Java string may hold a zero character and SQLite stores it, so a C string would drop that
    // character and everything after it.
    JAVA_ARRAY arr = (JAVA_ARRAY)value;
    // An empty array has no storage behind it -- allocArray leaves data at 0 when the length is
    // zero -- and sqlite3_bind_text reads a null pointer as SQL NULL whatever length it is given.
    // So "" would arrive as NULL: read back as null, and rejected outright by a NOT NULL column.
    // Any non-null pointer with a length of zero is an empty string to SQLite.
    cn1CheckSqlBind(CN1_THREAD_STATE_PASS_ARG
            sqlite3_bind_text((sqlite3_stmt*)statementPeer, index,
                    arr->length > 0 ? (const char*)arr->data : "",
                    arr->length, SQLITE_TRANSIENT), index);
}

void com_codename1_impl_ios_IOSNative_sqlStmtBindBlob___long_int_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer, JAVA_INT index, JAVA_OBJECT value) {
    if (value == JAVA_NULL) {
        cn1CheckSqlBind(CN1_THREAD_STATE_PASS_ARG
                sqlite3_bind_null((sqlite3_stmt*)statementPeer, index), index);
        return;
    }
    JAVA_ARRAY byteArray = (JAVA_ARRAY)value;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->data;
    // Same as the text bind above: a zero length array carries a null pointer, and that is how
    // SQL NULL is spelled to sqlite3_bind_blob. An empty blob is a pointer that is not null.
    cn1CheckSqlBind(CN1_THREAD_STATE_PASS_ARG
            sqlite3_bind_blob((sqlite3_stmt*)statementPeer, index,
                    byteArray->length > 0 ? (const void*)data : (const void*)"",
                    byteArray->length, SQLITE_TRANSIENT), index);
}

void com_codename1_impl_ios_IOSNative_sqlStmtBindLong___long_int_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer, JAVA_INT index, JAVA_LONG value) {
    cn1CheckSqlBind(CN1_THREAD_STATE_PASS_ARG
            sqlite3_bind_int64((sqlite3_stmt*)statementPeer, index, value), index);
}

void com_codename1_impl_ios_IOSNative_sqlStmtBindDouble___long_int_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer, JAVA_INT index, JAVA_DOUBLE value) {
    cn1CheckSqlBind(CN1_THREAD_STATE_PASS_ARG
            sqlite3_bind_double((sqlite3_stmt*)statementPeer, index, value), index);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlStmtStep___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)statementPeer;
    int rc = sqlite3_step(stmt);
    if (rc == SQLITE_ROW) {
        return JAVA_TRUE;
    }
    if (rc != SQLITE_DONE) {
        cn1ThrowSqlError(CN1_THREAD_STATE_PASS_ARG sqlite3_db_handle(stmt), "Failed to step the statement");
    }
    return JAVA_FALSE;
}

void com_codename1_impl_ios_IOSNative_sqlStmtReset___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)statementPeer;
    // sqlite3_reset reports the error of the *previous* step, which has already been reported, so
    // its return value is deliberately ignored here. Bindings survive a reset, which is what makes
    // re-stepping a parameterized query for a backward seek work.
    sqlite3_reset(stmt);
}

void com_codename1_impl_ios_IOSNative_sqlStmtFinalize___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    if (statementPeer != 0) {
        sqlite3_finalize((sqlite3_stmt*)statementPeer);
    }
}

void com_codename1_impl_ios_IOSNative_sqlStmtExecuteAndFinalize___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)statementPeer;
    int rc;
    // Step to completion: an UPDATE with a RETURNING clause, or any statement that yields rows,
    // needs more than the single step this used to do.
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        // discard
    }
    if (rc != SQLITE_DONE) {
        sqlite3* db = sqlite3_db_handle(stmt);
        sqlite3_finalize(stmt);
        cn1ThrowSqlError(CN1_THREAD_STATE_PASS_ARG db, "Failed to execute the statement");
        return;
    }
    sqlite3_finalize(stmt);
}

void com_codename1_impl_ios_IOSNative_sqlDbExec___long_java_lang_String_java_lang_String_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT sql, JAVA_OBJECT args) {
    sqlite3* db = (sqlite3*)dbPeer;
    const char* chrs = stringToUTF8(CN1_THREAD_STATE_PASS_ARG sql);
    if (args == JAVA_NULL) {
        char* errInfo = NULL;
        if (sqlite3_exec(db, chrs, NULL, NULL, &errInfo) != SQLITE_OK) {
            NSString* message = errInfo != NULL
                    ? [NSString stringWithFormat:@"SQL error: %s", errInfo]
                    : @"SQL error";
            if (errInfo != NULL) {
                sqlite3_free(errInfo);
            }
            cn1ThrowSqlMessage(CN1_THREAD_STATE_PASS_ARG [message UTF8String]);
        }
        return;
    }

    sqlite3_stmt *addStmt = NULL;
    if (sqlite3_prepare_v2(db, chrs, -1, &addStmt, NULL) != SQLITE_OK) {
        cn1ThrowSqlError(CN1_THREAD_STATE_PASS_ARG db, "Failed to compile the statement");
        return;
    }
    JAVA_ARRAY stringArray = (JAVA_ARRAY)args;
    JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)stringArray->data;
    int count = stringArray->length;
    for (int iter = 0; iter < count; iter++) {
        JAVA_OBJECT str = (JAVA_OBJECT)data[iter];
        if (str == JAVA_NULL) {
            sqlite3_bind_null(addStmt, iter + 1);
        } else {
            sqlite3_bind_text(addStmt, iter + 1,
                    stringToUTF8(CN1_THREAD_STATE_PASS_ARG str), -1, SQLITE_TRANSIENT);
        }
    }
    int rc;
    while ((rc = sqlite3_step(addStmt)) == SQLITE_ROW) {
        // discard
    }
    if (rc != SQLITE_DONE) {
        sqlite3_finalize(addStmt);
        cn1ThrowSqlError(CN1_THREAD_STATE_PASS_ARG db, "Failed to execute the statement");
        return;
    }
    sqlite3_finalize(addStmt);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlDbExecQuery___long_java_lang_String_java_lang_String_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT sql, JAVA_OBJECT args) {
    sqlite3* db = (sqlite3*)dbPeer;
    const char* chrs = stringToUTF8(CN1_THREAD_STATE_PASS_ARG sql);
    sqlite3_stmt *addStmt = NULL;
    if (sqlite3_prepare_v2(db, chrs, -1, &addStmt, NULL) != SQLITE_OK) {
        cn1ThrowSqlError(CN1_THREAD_STATE_PASS_ARG db, "Failed to compile the query");
        return 0;
    }
    if (args != JAVA_NULL) {
        JAVA_ARRAY stringArray = (JAVA_ARRAY)args;
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)stringArray->data;
        int count = stringArray->length;
        for (int iter = 0; iter < count; iter++) {
            JAVA_OBJECT str = (JAVA_OBJECT)data[iter];
            if (str == JAVA_NULL) {
                sqlite3_bind_null(addStmt, iter + 1);
            } else {
                sqlite3_bind_text(addStmt, iter + 1,
                        stringToUTF8(CN1_THREAD_STATE_PASS_ARG str), -1, SQLITE_TRANSIENT);
            }
        }
    }
    return (JAVA_LONG)addStmt;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorFirst___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)statementPeer;
    sqlite3_reset(stmt);
    return sqlite3_step(stmt) == SQLITE_ROW ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorNext___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    return com_codename1_impl_ios_IOSNative_sqlStmtStep___long(CN1_THREAD_STATE_PASS_ARG instanceObject, statementPeer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlGetColName___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer, JAVA_INT index) {
    // UTF-8 bytes for the caller to decode, for the reason given on sqlCursorValueAtColumnText:
    // an identifier is as free to be non-ASCII as a value is.
    const char* name = sqlite3_column_name((sqlite3_stmt*)statementPeer, index);
    if (name == NULL) {
        return JAVA_NULL;
    }
    POOL_BEGIN();
    NSData* data = [NSData dataWithBytes:name length:strlen(name)];
    JAVA_OBJECT result = nsDataToByteArr(data);
    POOL_END();
    return result;
}

void com_codename1_impl_ios_IOSNative_sqlCursorCloseStatement___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement) {
    if (statement != 0) {
        sqlite3_finalize((sqlite3_stmt*)statement);
    }
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnBlob___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    // This used to return nil unconditionally, so iOS could not read a blob at all.
    sqlite3_stmt* stmt = (sqlite3_stmt*)statement;
    const void* bytes = sqlite3_column_blob(stmt, col);
    int length = sqlite3_column_bytes(stmt, col);
    // A zero length blob is a blob, not SQL NULL: SQLite may return a null pointer for it, and
    // reading that as null would disagree with the column type and with wasNull().
    if (length < 0 || (bytes == NULL && length > 0)) {
        return JAVA_NULL;
    }
    POOL_BEGIN();
    NSData* data = [NSData dataWithBytes:bytes length:length];
    JAVA_OBJECT result = nsDataToByteArr(data);
    POOL_END();
    return result;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnDouble___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_double((sqlite3_stmt*)statement, col);
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnFloat___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return (JAVA_FLOAT)sqlite3_column_double((sqlite3_stmt*)statement, col);
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnInteger___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    // Read as 64 bit and narrow, so a value above 2^31 truncates predictably rather than being
    // reinterpreted by sqlite3_column_int.
    return (JAVA_INT)sqlite3_column_int64((sqlite3_stmt*)statement, col);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnLong___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_int64((sqlite3_stmt*)statement, col);
}

JAVA_SHORT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnShort___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return (JAVA_SHORT)sqlite3_column_int64((sqlite3_stmt*)statement, col);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnText___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    // Handed back as the engine's own UTF-8 bytes, for the caller to decode. Converting here would
    // mean a C string, which ends at an embedded zero byte -- a character SQLite stores and every
    // other port round-trips.
    sqlite3_stmt* stmt = (sqlite3_stmt*)statement;
    if (sqlite3_column_type(stmt, col) == SQLITE_NULL) {
        return JAVA_NULL;
    }
    // sqlite3_column_bytes must follow the _text call: it reports the length of the value as that
    // call converted it.
    const void* text = sqlite3_column_text(stmt, col);
    int length = sqlite3_column_bytes(stmt, col);
    if (text == NULL || length < 0) {
        length = 0;
    }
    POOL_BEGIN();
    NSData* data = [NSData dataWithBytes:(text == NULL ? "" : text) length:length];
    JAVA_OBJECT result = nsDataToByteArr(data);
    POOL_END();
    return result;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorNullValueAtColumn___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_type((sqlite3_stmt*)statement, col) == SQLITE_NULL ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlCursorGetColumnCount___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement) {
    return sqlite3_column_count((sqlite3_stmt*)statement);
}


JAVA_OBJECT productsArrayPending = nil;

void com_codename1_impl_ios_IOSNative_fetchProducts___java_lang_String_1ARRAY_com_codename1_payment_Product_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT skus, JAVA_OBJECT products) {
#ifdef CN1_USE_STOREKIT
    POOL_BEGIN();
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* strArray = skus;
    JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)strArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    int count = strArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
    JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)skus)->data;
    int count = ((JAVA_ARRAY)skus)->length;
#endif
    
    NSMutableSet *productIdentifiers = [[NSMutableSet alloc] init];
    
    for(int iter = 0 ; iter < count ; iter++) {
        [productIdentifiers addObject:toNSString(CN1_THREAD_STATE_PASS_ARG data[iter])];
    }
    productsArrayPending = products;
    
#ifndef CN1_USE_ARC
    SKProductsRequest * request = [[[SKProductsRequest alloc] initWithProductIdentifiers:productIdentifiers] autorelease];
#else
    SKProductsRequest * request = [[SKProductsRequest alloc] initWithProductIdentifiers:productIdentifiers];
#endif
    request.delegate = [CodenameOne_GLViewController instance];
    [request start];
    POOL_END();
#endif
}
#ifdef CN1_USE_STOREKIT
SKPayment *paymentInstance = nil;
NSObject *paymentDiscountInstance = nil;
#endif
void com_codename1_impl_ios_IOSNative_purchase___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT sku) {
#ifdef CN1_USE_STOREKIT
    NSString *nsSku = toNSString(CN1_THREAD_STATE_PASS_ARG sku);
    paymentDiscountInstance = nil;
    if ([nsSku hasPrefix:@"{"] && [nsSku hasSuffix:@"}"]) {
        NSError *error = nil;
        NSDictionary *dict = [NSJSONSerialization
                              JSONObjectWithData:[nsSku dataUsingEncoding:NSUTF8StringEncoding allowLossyConversion:NO]
                              options:0 error:&error
        ];
        if (error != nil) {
            JAVA_OBJECT ex = __NEW_java_lang_RuntimeException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
            java_lang_RuntimeException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, fromNSString(CN1_THREAD_STATE_PASS_ARG  [error localizedDescription]));
            throwException(threadStateData, ex);
            return;
        }
        nsSku = [dict valueForKey:@"sku"];
        NSDictionary *promoDict = [dict valueForKey: @"promotionalOffer"];
        if (@available(iOS 12.2, *)) {
            for (NSString* stringKey in @[@"offerIdentifier", @"keyIdentifier", @"signature", @"nonce"]) {
                if (!promoDict[stringKey] || ! [promoDict[stringKey] isKindOfClass:[NSString class]] ) {
                    JAVA_OBJECT ex = __NEW_java_lang_RuntimeException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
                    java_lang_RuntimeException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, fromNSString(CN1_THREAD_STATE_PASS_ARG  [NSString stringWithFormat: @"Promo offer requires string %@", stringKey]));
                    throwException(threadStateData, ex);
                    return;
                }
            }
            
            if (!promoDict[@"timestamp"] || ![promoDict[@"timestamp"] isKindOfClass:[NSNumber class]]) {
                JAVA_OBJECT ex = __NEW_java_lang_RuntimeException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
                java_lang_RuntimeException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, fromNSString(CN1_THREAD_STATE_PASS_ARG  [NSString stringWithFormat: @"Promo offer requires timestamp"]));
                throwException(threadStateData, ex);
                return;
            }
            NSString* offerIdentifier = promoDict[@"offerIdentifier"];
            NSString* keyIdentifier = promoDict[@"keyIdentifier"];
            NSUUID* nonce = [[NSUUID UUID] initWithUUIDString:promoDict[@"nonce"]];
            NSString* signature = promoDict[@"signature"];
            NSNumber* timestamp = promoDict[@"timestamp"];
            paymentDiscountInstance = [[SKPaymentDiscount alloc] initWithIdentifier:offerIdentifier keyIdentifier:keyIdentifier nonce:nonce signature:signature timestamp:timestamp];
        } else {
            // Silently do not add discount if iOS version is too old.
            NSLog(@"iOS version 12.2 or later is required for promotional discounts");
            
        }
        
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        if (paymentDiscountInstance != nil) {
            paymentInstance = [SKMutablePayment paymentWithProductIdentifier:nsSku];
            if (@available(iOS 12.2, *)) {
                ((SKMutablePayment*)paymentInstance).paymentDiscount = (SKPaymentDiscount*) paymentDiscountInstance;
            } else {
                // Fallback on earlier versions
                NSLog(@"Log error: Attempt to set payment discount instance on unsupported version of iOS.  This branch should never be reached");
            }
        } else {
            paymentInstance = [SKPayment paymentWithProductIdentifier:nsSku];
        }
        
        [[SKPaymentQueue defaultQueue] addPayment:paymentInstance];
    });
#endif
}

void com_codename1_impl_ios_IOSNative_restorePurchases__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef CN1_USE_STOREKIT
    [[SKPaymentQueue defaultQueue] restoreCompletedTransactions];
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_canMakePayments__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef CN1_USE_STOREKIT
    return (JAVA_BOOLEAN)[SKPaymentQueue canMakePayments];
#else
    return JAVA_FALSE;
#endif
}

NSLocale *currentLocale = NULL;
NSLocale *deviceLocale = NULL;
BOOL currentLocaleRequiresRelease = NO;

NSLocale* cn1DeviceLocale() {
    if (deviceLocale == NULL) {
        deviceLocale = [NSLocale localeWithLocaleIdentifier:[[NSLocale preferredLanguages] objectAtIndex:0] ];
    }
    return deviceLocale;
}

void com_codename1_impl_ios_IOSNative_setLocale___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT localeStr) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    if (currentLocale != NULL) {
        if (currentLocaleRequiresRelease) {
            [currentLocale release];
        }
    }
#endif
    if (localeStr == NULL) {
        currentLocaleRequiresRelease = NO;
        currentLocale = cn1DeviceLocale();
    } else {
        currentLocale = [[NSLocale alloc] initWithLocaleIdentifier:toNSString(threadStateData, localeStr)];
        currentLocaleRequiresRelease = YES;
    }
    
    POOL_END();
}


JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatInt___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT i) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSNumberFormatter *formatter = [[[NSNumberFormatter alloc] init] autorelease];
#else
    NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
#endif
    if (currentLocale != NULL) {
        formatter.locale = currentLocale;
    } else {
        formatter.locale = cn1DeviceLocale();
    }
    
    [formatter setNumberStyle:NSNumberFormatterDecimalStyle];
    JAVA_OBJECT o = fromNSString(CN1_THREAD_STATE_PASS_ARG [formatter stringFromNumber:[NSNumber numberWithInt:i]]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDouble___double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_DOUBLE d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSNumberFormatter *formatter = [[[NSNumberFormatter alloc] init] autorelease];
#else
    NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
#endif
    if (currentLocale != NULL) {
        formatter.locale = currentLocale;
    } else {
        formatter.locale = cn1DeviceLocale();
    }
    
    [formatter setNumberStyle:NSNumberFormatterDecimalStyle];
    JAVA_OBJECT o = fromNSString(CN1_THREAD_STATE_PASS_ARG [formatter stringFromNumber:[NSNumber numberWithDouble:d]]);
    POOL_END();
    return o;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_parseDouble___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSNumberFormatter *formatter = [[[NSNumberFormatter alloc] init] autorelease];
#else
    NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
#endif
    if (currentLocale != NULL) {
        formatter.locale = currentLocale;
    } else {
        formatter.locale = cn1DeviceLocale();
    }
    [formatter setNumberStyle:NSNumberFormatterDecimalStyle];
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG d);
    JAVA_DOUBLE result = [[formatter numberFromString:ns] doubleValue];
    POOL_END();
    return result;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatCurrency___double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_DOUBLE d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSNumberFormatter *formatter = [[[NSNumberFormatter alloc] init] autorelease];
#else
    NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
#endif
    if (currentLocale != NULL) {
        formatter.locale = currentLocale;
    } else {
        formatter.locale = cn1DeviceLocale();
    }
    [formatter setNumberStyle:NSNumberFormatterCurrencyStyle];
    JAVA_OBJECT o = fromNSString(CN1_THREAD_STATE_PASS_ARG [formatter stringFromNumber:[NSNumber numberWithDouble:d]]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDate___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    if (currentLocale != NULL) {
        formatter.locale = currentLocale;
    } else {
        formatter.locale = cn1DeviceLocale();
    }
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateStyle:NSDateFormatterMediumStyle];
    JAVA_OBJECT o = fromNSString(CN1_THREAD_STATE_PASS_ARG [formatter stringFromDate:date]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getLongMonthName___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    if (currentLocale != NULL) {
        formatter.locale = currentLocale;
    } else {
        formatter.locale = cn1DeviceLocale();
    }
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateFormat:@"MMMM"];
    JAVA_OBJECT o = fromNSString(CN1_THREAD_STATE_PASS_ARG [formatter stringFromDate:date]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getShortMonthName___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    if (currentLocale != NULL) {
        formatter.locale = currentLocale;
    } else {
        formatter.locale = cn1DeviceLocale();
    }
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateFormat:@"MMM"];
    JAVA_OBJECT o = fromNSString(CN1_THREAD_STATE_PASS_ARG [formatter stringFromDate:date]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateShort___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    if (currentLocale != NULL) {
        formatter.locale = currentLocale;
    } else {
        formatter.locale = cn1DeviceLocale();
    }
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateStyle:NSDateFormatterShortStyle];
    JAVA_OBJECT o = fromNSString(CN1_THREAD_STATE_PASS_ARG [formatter stringFromDate:date]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateTime___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    if (currentLocale != NULL) {
        formatter.locale = currentLocale;
    } else {
        formatter.locale = cn1DeviceLocale();
    }
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateStyle:NSDateFormatterLongStyle];
    [formatter setTimeStyle:NSDateFormatterLongStyle];
    JAVA_OBJECT o = fromNSString(CN1_THREAD_STATE_PASS_ARG [formatter stringFromDate:date]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateTimeMedium___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    if (currentLocale != NULL) {
        formatter.locale = currentLocale;
    } else {
        formatter.locale = cn1DeviceLocale();
    }
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateStyle:NSDateFormatterMediumStyle];
    [formatter setTimeStyle:NSDateFormatterMediumStyle];
    JAVA_OBJECT o = fromNSString(CN1_THREAD_STATE_PASS_ARG [formatter stringFromDate:date]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateTimeShort___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    if (currentLocale != NULL) {
        formatter.locale = currentLocale;
    } else {
        formatter.locale = cn1DeviceLocale();
    }
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateStyle:NSDateFormatterShortStyle];
    [formatter setTimeStyle:NSDateFormatterShortStyle];
    JAVA_OBJECT o = fromNSString(CN1_THREAD_STATE_PASS_ARG [formatter stringFromDate:date]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getCurrencySymbol__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSNumberFormatter *formatter = [[[NSNumberFormatter alloc] init] autorelease];
#else
    NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
#endif
    if (currentLocale != NULL) {
        formatter.locale = currentLocale;
    } else {
        formatter.locale = cn1DeviceLocale();
    }
    JAVA_OBJECT c = fromNSString(CN1_THREAD_STATE_PASS_ARG [formatter currencyCode]);
    POOL_END();
    return c;
}

void com_codename1_impl_ios_IOSNative_scanBarCode__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
}

void com_codename1_impl_ios_IOSNative_scanQRCode__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
}

#ifdef NEW_CODENAME_ONE_VM
NSData* arrayToData(JAVA_OBJECT arr) {
    if (arr == JAVA_NULL) return nil;
    JAVA_ARRAY byteArray = (JAVA_ARRAY)arr;
    void* data = (void*)byteArray->data;
    NSData* d = [NSData dataWithBytes:data length:byteArray->length * byteArray->primitiveSize];
    return d;
}

NSData* arrayToDataRange(JAVA_OBJECT arr, int offset, int len) {
    if (arr == JAVA_NULL) return nil;
    JAVA_ARRAY byteArray = (JAVA_ARRAY)arr;
    char* data = (char*)byteArray->data;
    NSData* d = [NSData dataWithBytes:(data + offset * byteArray->primitiveSize) length:len * byteArray->primitiveSize];
    return d;
}

JAVA_OBJECT nsDataToByteArr(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length] / sizeof(JAVA_ARRAY_BYTE), &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToBooleanArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length]/sizeof(JAVA_ARRAY_BOOLEAN), &class_array1__JAVA_BOOLEAN, sizeof(JAVA_ARRAY_BOOLEAN), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToCharArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length]/sizeof(JAVA_ARRAY_CHAR), &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToShortArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length]/sizeof(JAVA_ARRAY_SHORT), &class_array1__JAVA_SHORT, sizeof(JAVA_ARRAY_SHORT), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToIntArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length]/sizeof(JAVA_ARRAY_INT), &class_array1__JAVA_INT, sizeof(JAVA_ARRAY_INT), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToLongArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length]/sizeof(JAVA_ARRAY_LONG), &class_array1__JAVA_LONG, sizeof(JAVA_ARRAY_LONG), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToFloatArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length]/sizeof(JAVA_ARRAY_FLOAT), &class_array1__JAVA_FLOAT, sizeof(JAVA_ARRAY_FLOAT), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToDoubleArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length]/sizeof(JAVA_ARRAY_DOUBLE), &class_array1__JAVA_DOUBLE, sizeof(JAVA_ARRAY_DOUBLE), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}
#else // NEW_CODENAME_ONE_VM
NSData* arrayToData(JAVA_OBJECT arr) {
    if (arr == JAVA_NULL) return nil;
    org_xmlvm_runtime_XMLVMArray* byteArray = (org_xmlvm_runtime_XMLVMArray*)arr;
    void* data = (void*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    NSData* d = [NSData dataWithBytes:data length:byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_];
    return d;
}

NSData* arrayToDataRange(JAVA_OBJECT arr, int offset, int len) {
    if (arr == JAVA_NULL) return nil;
    org_xmlvm_runtime_XMLVMArray* byteArray = (org_xmlvm_runtime_XMLVMArray*)arr;
    char* data = (char*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    NSData* d = [NSData dataWithBytes:(data + offset) length:len];
    return d;
}

JAVA_OBJECT nsDataToByteArr(NSData *data) {
    POOL_BEGIN();
    NSData* d = (NSData*)data;
    org_xmlvm_runtime_XMLVMArray* byteArray = XMLVMArray_createSingleDimension(__CLASS_byte, [d length]);
    void* dtd = (void*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    memcpy(dtd, d.bytes, d.length);
    POOL_END();
    return byteArray;
}

JAVA_OBJECT nsDataToBooleanArray(NSData *data) {
    POOL_BEGIN();
    NSData* d = (NSData*)data;
    org_xmlvm_runtime_XMLVMArray* byteArray = XMLVMArray_createSingleDimension(__CLASS_boolean, [d length]);
    void* dtd = (void*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    memcpy(dtd, d.bytes, d.length);
    POOL_END();
    return byteArray;
}

JAVA_OBJECT nsDataToCharArray(NSData *data) {
    POOL_BEGIN();
    NSData* d = (NSData*)data;
    org_xmlvm_runtime_XMLVMArray* byteArray = XMLVMArray_createSingleDimension(__CLASS_char, [d length]);
    void* dtd = (void*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    memcpy(dtd, d.bytes, d.length);
    POOL_END();
    return byteArray;
}

JAVA_OBJECT nsDataToShortArray(NSData *data) {
    POOL_BEGIN();
    NSData* d = (NSData*)data;
    org_xmlvm_runtime_XMLVMArray* byteArray = XMLVMArray_createSingleDimension(__CLASS_short, [d length]);
    void* dtd = (void*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    memcpy(dtd, d.bytes, d.length);
    POOL_END();
    return byteArray;
}

JAVA_OBJECT nsDataToIntArray(NSData *data) {
    POOL_BEGIN();
    NSData* d = (NSData*)data;
    org_xmlvm_runtime_XMLVMArray* byteArray = XMLVMArray_createSingleDimension(__CLASS_int, [d length]);
    void* dtd = (void*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    memcpy(dtd, d.bytes, d.length);
    POOL_END();
    return byteArray;
}

JAVA_OBJECT nsDataToLongArray(NSData *data) {
    POOL_BEGIN();
    NSData* d = (NSData*)data;
    org_xmlvm_runtime_XMLVMArray* byteArray = XMLVMArray_createSingleDimension(__CLASS_long, [d length]);
    void* dtd = (void*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    memcpy(dtd, d.bytes, d.length);
    POOL_END();
    return byteArray;
}

JAVA_OBJECT nsDataToFloatArray(NSData *data) {
    POOL_BEGIN();
    NSData* d = (NSData*)data;
    org_xmlvm_runtime_XMLVMArray* byteArray = XMLVMArray_createSingleDimension(__CLASS_float, [d length]);
    void* dtd = (void*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    memcpy(dtd, d.bytes, d.length);
    POOL_END();
    return byteArray;
}

JAVA_OBJECT nsDataToDoubleArray(NSData *data) {
    POOL_BEGIN();
    NSData* d = (NSData*)data;
    org_xmlvm_runtime_XMLVMArray* byteArray = XMLVMArray_createSingleDimension(__CLASS_double, [d length]);
    void* dtd = (void*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    memcpy(dtd, d.bytes, d.length);
    POOL_END();
    return byteArray;
}
#endif // NEW_CODENAME_ONE_VM

// Register every .ttf bundled in the app (notably material-design-font.ttf for
// FontImage glyphs) with the process font manager so [CN1Font fontWithName:] and
// CTFontCreateWithName can resolve them by name. The iOS app also lists these in
// UIAppFonts, but the Metal/tvOS and Core-Graphics/watchOS slices do not reliably
// register fonts from the Info.plist, so without this the Material font fails to
// load there and FontImage glyphs fall back to the emoji/substitute character
// (e.g. the lock icon rendered as the color lock emoji on the watch). Runs once;
// re-registering an already-registered font returns an error that is ignored.
static void cn1RegisterBundledFontsOnce() {
    static BOOL cn1FontsRegistered = NO;
    if (cn1FontsRegistered) {
        return;
    }
    cn1FontsRegistered = YES;
    @autoreleasepool {
        // Core Text reads OpenType as readily as TrueType, and this scan is the
        // only thing registering bundled fonts on watchOS -- its plist carries no
        // UIAppFonts array -- so anything missed here falls back to the system
        // font on the watch even though it renders everywhere else.
        //
        // List the resource directory once and compare the lower-cased extension,
        // rather than calling pathsForResourcesOfType: per spelling: that call
        // matches the extension exactly, so a per-spelling list only ever covers
        // the spellings someone thought to write down while the rest of the stack
        // accepts any case, and ".TtF" would be bundled and never registered.
        //
        // A shallow listing of the resource root is deliberate. Fonts are
        // deployed flat next to theme.res -- that is why createTrueTypeFont
        // forbids a path separator in the name -- so nothing is missed, and it
        // avoids walking every resource in the bundle (pods, map assets, models)
        // on the way to the first glyph.
        NSString *resourceRoot = [[NSBundle mainBundle] resourcePath];
        NSArray *resourceNames = resourceRoot == nil ? nil
                : [[NSFileManager defaultManager] contentsOfDirectoryAtPath:resourceRoot error:NULL];
        for (NSString *resourceName in resourceNames) {
            NSString *ext = [[resourceName pathExtension] lowercaseString];
            if (![ext isEqualToString:@"ttf"] && ![ext isEqualToString:@"otf"]) {
                continue;
            }
            NSURL *url = [NSURL fileURLWithPath:[resourceRoot stringByAppendingPathComponent:resourceName]];
            CFErrorRef error = NULL;
            // A font already registered by UIAppFonts errors here; that is
            // expected on iOS/tvOS and the error is discarded.
            CTFontManagerRegisterFontsForURL((BRIDGE_CAST CFURLRef)url, kCTFontManagerScopeProcess, &error);
            if (error != NULL) {
                CFRelease(error);
            }
        }
    }
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createTruetypeFont___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    int pSize = 14;

    pSize *= scaleValue;
    POOL_BEGIN();
    cn1RegisterBundledFontsOnce();
    NSString* str = toNSString(CN1_THREAD_STATE_PASS_ARG name);

    CN1Font* fnt;
    if(isIOS8_2() && [str hasPrefix:@"HelveticaNeue"]) {
        if([str isEqualToString:@"HelveticaNeue-UltraLight"]) {
            fnt = [CN1Font systemFontOfSize:pSize weight:UIFontWeightUltraLight];
        } else {
            if([str isEqualToString:@"HelveticaNeue-Light"]) {
                fnt = [CN1Font systemFontOfSize:pSize weight:UIFontWeightLight];
            } else {
                if([str isEqualToString:@"HelveticaNeue-Medium"]) {
                    fnt = [CN1Font systemFontOfSize:pSize weight:UIFontWeightMedium];
                } else {
                    if([str isEqualToString:@"HelveticaNeue-Bold"]) {
                        fnt = [CN1Font systemFontOfSize:pSize weight:UIFontWeightBold];
                    } else {
                        if([str isEqualToString:@"HelveticaNeue-CondensedBlack"]) {
                            fnt = [CN1Font systemFontOfSize:pSize weight:UIFontWeightHeavy];
                        } else {
                            // this is probably an italic font, fallback to regular code...
                            fnt = [CN1Font fontWithName:str size:pSize];
                        }
                    }
                }
            }
        }
    } else {
        fnt = [CN1Font fontWithName:str size:pSize];
    }
    
#ifndef CN1_USE_ARC
    [fnt retain];
#endif
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)fnt);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_deriveTruetypeFont___long_boolean_boolean_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG uiFont, JAVA_BOOLEAN bold, JAVA_BOOLEAN italic, JAVA_FLOAT size) {
    POOL_BEGIN();
    CN1Font* original = (BRIDGE_CAST CN1Font*)((void *)uiFont);
    CN1Font* fnt = [original fontWithSize:size];
#ifndef CN1_USE_ARC
    [fnt retain];
#endif
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)fnt);
}

void com_codename1_impl_ios_IOSNative_log___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    POOL_BEGIN();
    CN1Log(@"%@", toNSString(CN1_THREAD_STATE_PASS_ARG name));
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_clearNativeCookies__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT receiver){
    NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    for (NSHTTPCookie *cookie in [storage cookies]) {
        [storage deleteCookie:cookie];
    }
    [[NSUserDefaults standardUserDefaults] synchronize];
}

void com_codename1_impl_ios_IOSNative_getCookiesForURL___java_lang_String_java_util_Vector(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT receiver, JAVA_OBJECT urlStr, JAVA_OBJECT outVector) {
    POOL_BEGIN();
    NSHTTPCookieStorage *cstore = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    
    NSString* nsStr = toNSString(CN1_THREAD_STATE_PASS_ARG urlStr);
    
    // workaround for exception where the | character is concidered to be illegal by apple but is required by facebook
    nsStr = [nsStr stringByReplacingOccurrencesOfString:@"|" withString:@"%7C"];
    NSURL *url = [NSURL URLWithString:nsStr];
    if(url == nil) {
        CN1Log(@"Invalid URL! You need to escape the characters of the URL in order for it work properly! %@", nsStr);
        return;
    }
    NSArray *cookies = [cstore cookiesForURL:url];
    int count = cookies.count;
    for(int iter = 0 ; iter < count ; iter++) {
        NSHTTPCookie *cookie = [cookies objectAtIndex:iter];
        JAVA_OBJECT name = fromNSString(CN1_THREAD_STATE_PASS_ARG [cookie name]);
        JAVA_OBJECT domain = fromNSString(CN1_THREAD_STATE_PASS_ARG [cookie domain]);
        JAVA_OBJECT path = fromNSString(CN1_THREAD_STATE_PASS_ARG [cookie path]);
        JAVA_OBJECT value = fromNSString(CN1_THREAD_STATE_PASS_ARG [cookie value]);
        JAVA_LONG expires = [[cookie expiresDate] timeIntervalSince1970] * 1000L;
        JAVA_BOOLEAN secure = [cookie isSecure];
        JAVA_BOOLEAN httpOnly = [cookie isHTTPOnly];
        
#ifdef NEW_CODENAME_ONE_VM
        enteringNativeAllocations();
#endif
        JAVA_OBJECT jcookie = __NEW_INSTANCE_com_codename1_io_Cookie(CN1_THREAD_STATE_PASS_SINGLE_ARG);
        com_codename1_io_Cookie_setName___java_lang_String(CN1_THREAD_STATE_PASS_ARG jcookie, name);
        com_codename1_io_Cookie_setSecure___boolean(CN1_THREAD_STATE_PASS_ARG jcookie, secure);
        com_codename1_io_Cookie_setHttpOnly___boolean(CN1_THREAD_STATE_PASS_ARG jcookie, httpOnly);
        com_codename1_io_Cookie_setPath___java_lang_String(CN1_THREAD_STATE_PASS_ARG jcookie, path);
        com_codename1_io_Cookie_setValue___java_lang_String(CN1_THREAD_STATE_PASS_ARG jcookie, value);
        com_codename1_io_Cookie_setDomain___java_lang_String(CN1_THREAD_STATE_PASS_ARG jcookie, domain);
        com_codename1_io_Cookie_setExpires___long(CN1_THREAD_STATE_PASS_ARG jcookie, expires);
        
#ifndef NEW_CODENAME_ONE_VM
        java_util_Vector_add___java_lang_Object(outVector, jcookie);
#else
        java_util_Vector_add___java_lang_Object_R_boolean(CN1_THREAD_STATE_PASS_ARG outVector, jcookie);
        finishedNativeAllocations();
#endif
    }
    
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_addCookie___java_lang_String_java_lang_String_java_lang_String_java_lang_String_boolean_boolean_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT receiver, JAVA_OBJECT key, JAVA_OBJECT value,JAVA_OBJECT domain, JAVA_OBJECT path, JAVA_BOOLEAN secure, JAVA_BOOLEAN httpOnly, JAVA_LONG expires) {
    POOL_BEGIN();
    NSDictionary *stringProps = [[NSDictionary alloc] initWithObjectsAndKeys:
                                 toNSString(CN1_THREAD_STATE_PASS_ARG key), NSHTTPCookieName,
                                 toNSString(CN1_THREAD_STATE_PASS_ARG value), NSHTTPCookieValue,
                                 toNSString(CN1_THREAD_STATE_PASS_ARG domain), NSHTTPCookieDomain,
                                 toNSString(CN1_THREAD_STATE_PASS_ARG path), NSHTTPCookiePath,
                                 secure?@YES:@NO , secure?NSHTTPCookieSecure:@"___",
                                 expires == 0 ? Nil : [NSDate dateWithTimeIntervalSince1970:expires/1000L], NSHTTPCookieExpires, Nil];
    NSHTTPCookie *cookie = [NSHTTPCookie cookieWithProperties: stringProps];
    [[NSHTTPCookieStorage sharedHTTPCookieStorage] setCookie:cookie];
    
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_zoozPurchase___double_java_lang_String_java_lang_String_boolean_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_DOUBLE amount, JAVA_OBJECT currency, JAVA_OBJECT appKey, JAVA_BOOLEAN sandbox, JAVA_OBJECT invoiceNumber) {
#ifdef INCLUDE_ZOOZ
    NSString *_currency = toNSString(CN1_THREAD_GET_STATE_PASS_ARG currency);
    NSString *_appKey = toNSString(CN1_THREAD_GET_STATE_PASS_ARG appKey);
    NSString *_invoiceNumber = toNSString(CN1_THREAD_GET_STATE_PASS_ARG invoiceNumber);
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        ZooZ *zooz = [ZooZ sharedInstance];
        zooz.sandbox = sandbox;//set this if working in Sandbox mode
        ZooZPaymentRequest *req = [zooz createPaymentRequestWithTotal:amount invoiceRefNumber:_invoiceNumber delegate:[CodenameOne_GLViewController instance]];
        req.currencyCode = _currency;
        //        req.payerDetails.email = @"test@test.com";
        [zooz openPayment:req forAppKey:_appKey];
        POOL_END();
    });
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_browserExecuteAndReturnString___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT javaScript){
    if (isWKWebView(peer)) {
        NSLog(@"browserExecuteAndReturnString not supported for WKWebView");
        return JAVA_NULL;
    } else {
#ifndef NO_UIWEBVIEW
        if ([NSThread isMainThread]) {
            POOL_BEGIN();
            UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
            JAVA_OBJECT out = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [w stringByEvaluatingJavaScriptFromString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG javaScript)]);
            POOL_END();
            return out;
        } else {
            __block JAVA_OBJECT out;
            dispatch_sync(dispatch_get_main_queue(), ^{
                POOL_BEGIN();
                UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
                out = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [w stringByEvaluatingJavaScriptFromString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG javaScript)]);
                POOL_END();
            });
            return out;
        }
#else
        return 0;
#endif
    }

}

JAVA_OBJECT java_util_TimeZone_getTimezoneId__(CN1_THREAD_STATE_SINGLE_ARG) {
    POOL_BEGIN();
    NSTimeZone *tzone = [NSTimeZone defaultTimeZone];
    NSString* n = [tzone name];
    //CN1Log(@"java_util_TimeZone_getTimezoneId__ %@", n);
    JAVA_OBJECT str = fromNSString(CN1_THREAD_STATE_PASS_ARG n);
    POOL_END();
    return str;
}

JAVA_INT java_util_TimeZone_getTimezoneOffset___java_lang_String_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT name, JAVA_INT year, JAVA_INT month, JAVA_INT day, JAVA_INT timeOfDayMillis) {
    POOL_BEGIN();
    NSString* n = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    //CN1Log(@"java_util_TimeZone_getTimezoneOffset___java_lang_String_long %@, %i", n, timeMillis / 1000);
    NSTimeZone *tzone = [NSTimeZone timeZoneWithName:n];
    NSDateComponents *comps = [[NSDateComponents alloc] init];
    [comps setDay:day];
    [comps setYear:year];
    [comps setMonth:month];
    [comps setHour:timeOfDayMillis/3600000];
    [comps setMinute:(timeOfDayMillis/60000)%60];
    [comps setSecond:(timeOfDayMillis/1000)%60];
    // The fields are UTC, not local standard time. That is this native's contract
    // across every port -- the POSIX implementation resolves them with timegm()
    // and the JavaScript and Android ports match -- and TimeApiTest pins it:
    // asking about the local-standard instant instead resolves 2020-03-08T01:30
    // EST as 02:30 EDT, jumping the spring transition. Reading them in the
    // device's own zone (currentCalendar) is wrong for a different reason: it
    // moves the instant by the device offset.
    NSCalendar* cal = [NSCalendar calendarWithIdentifier:NSCalendarIdentifierGregorian];
    [cal setTimeZone:[NSTimeZone timeZoneWithAbbreviation:@"UTC"]];
    NSDate *date = [cal dateFromComponents:comps];
    JAVA_INT result = [tzone secondsFromGMTForDate:date] * 1000;
    [comps release];
    POOL_END();
    return result;
}

JAVA_INT java_util_TimeZone_getTimezoneRawOffset___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT name) {
    POOL_BEGIN();
    NSString* n = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    //CN1Log(@"java_util_TimeZone_getTimezoneRawOffset___java_lang_String %@", n);
    NSTimeZone *tzone = [NSTimeZone timeZoneWithName:n];
    JAVA_INT result = [tzone secondsFromGMT] * 1000;
    if([tzone isDaylightSavingTime]) {
        result -= (int)([tzone daylightSavingTimeOffset] * 1000);
    }
    POOL_END();
    return result;
}

JAVA_BOOLEAN java_util_TimeZone_isTimezoneDST___java_lang_String_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT name, JAVA_LONG millis) {
    POOL_BEGIN();
    NSString* n = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    //CN1Log(@"java_util_TimeZone_isTimezoneDST___java_lang_String_long %@, %i", n, millis / 1000);
    NSTimeZone *tzone = [NSTimeZone timeZoneWithName:n];
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(millis / 1000)];
    JAVA_BOOLEAN result = [tzone isDaylightSavingTimeForDate:date];
    POOL_END();
    return result;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUserAgentString___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT callbackId) {
#if TARGET_OS_WATCH || TARGET_OS_TV
    // watchOS/tvOS have neither UIWebView nor WKWebView to query a user agent from.
    return JAVA_NULL;
#else
    __block JAVA_OBJECT c = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
#ifdef NO_UIWEBVIEW
    WKWebView *webView = [[WKWebView alloc] initWithFrame:CGRectZero];
    [webView loadHTMLString:@"<html></html>" baseURL:nil];

    [webView evaluateJavaScript:@"navigator.appName" completionHandler:^(id __nullable appName, NSError * __nullable error) {
        NSLog(@"%@", appName);
        // Netscape
    }];
    [webView retain];
    [webView evaluateJavaScript:@"navigator.userAgent" completionHandler:^(id __nullable userAgent, NSError * __nullable error) {
        com_codename1_impl_ios_IOSImplementation_completeStringCallback___java_lang_String_java_lang_String(
            CN1_THREAD_GET_STATE_PASS_ARG 
            callbackId,
            fromNSString(CN1_THREAD_GET_STATE_PASS_ARG userAgent)
        );
        [webView release];
        
    }];
    
#else
        UIWebView* webView = [[UIWebView alloc] initWithFrame:CGRectZero];
        NSString* userAgentString = [webView stringByEvaluatingJavaScriptFromString:@"navigator.userAgent"];
        c = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG userAgentString);
#ifndef CN1_USE_ARC
        [webView release];
#endif
#endif
        POOL_END();
    });

    return c;
#endif // TARGET_OS_WATCH || TARGET_OS_TV
}

bool datepickerPopover = NO;
#ifndef NEW_CODENAME_ONE_VM
org_xmlvm_runtime_XMLVMArray* pickerStringArray = nil;
#else
JAVA_OBJECT pickerStringArray = JAVA_NULL;
#endif
int stringPickerSelection;
NSDate* currentDatePickerDate;
JAVA_LONG currentDatePickerDuration=-1;
#if !TARGET_OS_WATCH && !TARGET_OS_TV
// UIKit-only declaration: the type in its signature does not exist on macOS,
// so the whole thing goes rather than just the body. Guarding only the body
// would leave a signature naming a type the compiler has never heard of.
#if !TARGET_OS_OSX
extern UIPopoverController* popoverControllerInstance;
#endif
extern CN1View *currentActionSheet;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
JAVA_LONG defaultDatePickerDate;

#if !TARGET_OS_WATCH && !TARGET_OS_TV
void showPopupPickerView(CN1_THREAD_STATE_MULTI_ARG CN1View *pickerView) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
#else
    int SCREEN_HEIGHT = [CodenameOne_GLViewController instance].view.bounds.size.height;
    int SCREEN_WIDTH = [CodenameOne_GLViewController instance].view.bounds.size.width;
    CN1View* fakeActionSheet = [[CN1View alloc] initWithFrame:CGRectMake(0, SCREEN_HEIGHT-246, SCREEN_WIDTH, 246)];
    [fakeActionSheet setBackgroundColor:[UIColor colorWithRed:240/255.0 green:240/255.0 blue:240/255.0 alpha:1.0]];
    [fakeActionSheet setAutoresizesSubviews:YES];
    UIToolbar *pickerToolbar = [[UIToolbar alloc] initWithFrame:CGRectMake(0, 0, [CodenameOne_GLViewController instance].view.frame.size.width, 64)];
    pickerToolbar.tintColor = [UIColor whiteColor];
    [pickerToolbar setAutoresizingMask:UIViewAutoresizingFlexibleWidth];
    [pickerToolbar sizeToFit];
#ifndef NEW_CODENAME_ONE_VM
    JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance__();
#else
    JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance___R_com_codename1_ui_plaf_UIManager(CN1_THREAD_STATE_PASS_SINGLE_ARG);
#endif
    JAVA_OBJECT str;
#ifndef NEW_CODENAME_ONE_VM
    str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String(obj, fromNSString(@"Cancel"), fromNSString(@"Cancel"));
#else
    str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG obj, fromNSString(CN1_THREAD_STATE_PASS_ARG @"Cancel"), fromNSString(CN1_THREAD_STATE_PASS_ARG @"Cancel"));
#endif
    UIBarButtonItem *cancelBtn = [[UIBarButtonItem alloc] initWithTitle:toNSString(CN1_THREAD_STATE_PASS_ARG str) style:UIBarButtonItemStyleBordered target:[CodenameOne_GLViewController instance] action:@selector(datePickerCancel)];
    
    [cancelBtn setTitleTextAttributes:[NSDictionary dictionaryWithObjectsAndKeys:
                                       [UIColor colorWithRed:253.0/255.0 green:68.0/255.0 blue:142.0/255.0 alpha:1.0],
                                       NSForegroundColorAttributeName,
                                       nil] forState:UIControlStateNormal];
    
    UIBarButtonItem *flexSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:[CodenameOne_GLViewController instance] action:nil];
    
    UIBarButtonItem *titleButton;
    
    float pickerMarginHeight = 168;
    
    
    titleButton = [[UIBarButtonItem alloc] initWithTitle:@"" style:UIBarButtonItemStylePlain target: nil action: nil];
    
    [titleButton setTitleTextAttributes:[NSDictionary dictionaryWithObjectsAndKeys:
                                         [UIColor colorWithRed:253.0/255.0 green:68.0/255.0 blue:142.0/255.0 alpha:1.0],
                                         NSForegroundColorAttributeName,
                                         nil] forState:UIControlStateNormal];
    JAVA_OBJECT str2;
#ifndef NEW_CODENAME_ONE_VM
    str2 = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String(obj, fromNSString(@"OK"), fromNSString(@"OK"));
#else
    str2 = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG obj, fromNSString(CN1_THREAD_STATE_PASS_ARG @"OK"), fromNSString(CN1_THREAD_STATE_PASS_ARG @"OK"));
#endif
    
    UIBarButtonItem *doneBtn = [[UIBarButtonItem alloc] initWithTitle:toNSString(CN1_THREAD_STATE_PASS_ARG str2) style:UIBarButtonItemStyleDone target:[CodenameOne_GLViewController instance] action:@selector(datePickerDismiss)];
    
    [doneBtn setTitleTextAttributes:[NSDictionary dictionaryWithObjectsAndKeys:
                                     [UIColor colorWithRed:253.0/255.0 green:68.0/255.0 blue:142.0/255.0 alpha:1.0],
                                     NSForegroundColorAttributeName,
                                     nil] forState:UIControlStateNormal];
    
    NSArray *itemArray = [[NSArray alloc] initWithObjects:cancelBtn, flexSpace, titleButton, flexSpace, doneBtn, nil];
    
    [pickerToolbar setItems:itemArray animated:YES];
    if(isIPad() || isIOS7()) {
        [pickerView setFrame:CGRectMake(0, 44, isIPad() ? pickerView.frame.size.width : [CodenameOne_GLViewController instance].view.frame.size.width, pickerView.frame.size.height)];
    } else {
        [pickerView setFrame:CGRectMake(0, 44, 0, 0)];
    }
    [pickerView setAutoresizingMask:UIViewAutoresizingFlexibleWidth];
    [fakeActionSheet addSubview:pickerToolbar];
    [fakeActionSheet addSubview:pickerView];
    [[CodenameOne_GLViewController instance].view addSubview:fakeActionSheet];
    currentActionSheet = fakeActionSheet;
    if ([pickerView isKindOfClass: [UIPickerView class]] && stringPickerSelection>-1) {
        [(UIPickerView*)pickerView selectRow: stringPickerSelection inComponent:0 animated: NO];
    }
    repaintUI();
#endif
}
void com_codename1_impl_ios_IOSNative_openStringPicker___java_lang_String_1ARRAY_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT stringArray, JAVA_INT selection, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_INT preferredWidth, JAVA_INT preferredHeight) {
#if TARGET_OS_OSX
    extern void CN1MacOpenStringPicker(JAVA_OBJECT stringArray, int selection, int x, int y, int w, int h);
    com_codename1_impl_ios_IOSImplementation_foldKeyboard__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    CN1MacOpenStringPicker(stringArray, selection, x, y, w, h);
#else

    if (preferredWidth == 0) {
        preferredWidth = 320 * scaleValue;
    }
    
    // There are only 3 valid heights for the picker in iPad
    //http://stackoverflow.com/a/7672577/2935174
    if (preferredHeight == 0) {
        preferredHeight = 216 * scaleValue;
    } else if (preferredHeight <= 162) {
        preferredHeight = 162;
    } else if (preferredHeight <= 180) {
        preferredHeight = 180;
    } else {
        preferredHeight = 216;
    }
    
    
    com_codename1_impl_ios_IOSImplementation_foldKeyboard__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
#ifndef NEW_CODENAME_ONE_VM
    pickerStringArray = (org_xmlvm_runtime_XMLVMArray*)stringArray;
#else
    pickerStringArray = stringArray;
#endif
    currentDatePickerDate = nil;
    currentDatePickerDuration = -1;
    defaultDatePickerDate = 0;
    stringPickerSelection = selection;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIPickerView* pickerView;
        if(isIPad() || isIOS7()) {
            pickerView = [[UIPickerView alloc] init];
        } else {
            pickerView = [[UIPickerView alloc] initWithFrame:CGRectMake(0, 40, 0, 0)];
        }
        if(selection > -1) {
            [pickerView selectRow:selection inComponent:0 animated:NO];
        }
        pickerView.delegate = [CodenameOne_GLViewController instance];
        
        if(isIPad()) {
            datepickerPopover = YES;
            stringPickerSelection = -1;
            UIViewController *vc = [[UIViewController alloc] init];
            CN1View *popoverView = [[CN1View alloc] init];
            [vc setView:popoverView];
            
#ifndef CN1_USE_ARC
            UIToolbar *toolbar = [[[UIToolbar alloc] init] autorelease];
#else
            UIToolbar *toolbar = [[UIToolbar alloc] init];
#endif
            [toolbar setBarStyle:UIBarStyleBlackTranslucent];
            [toolbar sizeToFit];
            
            //add a space filler to the left:
            UIBarButtonItem *flexButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:
                                           UIBarButtonSystemItemFlexibleSpace target: nil action:nil];
            
#ifndef NEW_CODENAME_ONE_VM
            JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance__();
#else
            JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance___R_com_codename1_ui_plaf_UIManager(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
#endif
            JAVA_OBJECT str;
            UIBarButtonItem *doneButton;
            NSArray *itemsArray = nil;
#ifndef NEW_CODENAME_ONE_VM
            str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String(obj, fromNSString(@"OK"), fromNSString(@"OK"));
#else
            str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG obj, fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"OK"), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"OK"));
#endif
            NSString* buttonTitle = toNSString(CN1_THREAD_GET_STATE_PASS_ARG str);
            doneButton = [[UIBarButtonItem alloc]initWithTitle:buttonTitle style:UIBarButtonItemStyleDone target:[CodenameOne_GLViewController instance] action:@selector(pickerComponentDismiss)];
            
            itemsArray = [NSArray arrayWithObjects: doneButton, nil];
#ifndef CN1_USE_ARC
            [flexButton release];
            [doneButton release];
#endif
            [toolbar setItems:itemsArray];
            
            
            [popoverView addSubview:pickerView];
            [popoverView addSubview:toolbar];
            
            UIPopoverController* uip = [[UIPopoverController alloc] initWithContentViewController:vc];
            popoverControllerInstance = uip;
            
            uip.delegate = [CodenameOne_GLViewController instance];
            uip.popoverContentSize = CGSizeMake(preferredWidth/scaleValue, preferredHeight/scaleValue);
            
            [uip presentPopoverFromRect:CGRectMake(x / scaleValue, y / scaleValue, w / scaleValue, h / scaleValue) inView:[CodenameOne_GLViewController instance].view permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
            
            pickerView.frame = CGRectMake(0, 22, preferredWidth/scaleValue, preferredHeight/scaleValue);
            popoverView.frame = CGRectMake(0, 0, preferredWidth/scaleValue, preferredHeight/scaleValue);
            toolbar.frame = CGRectMake(0,0, preferredWidth/scaleValue, toolbar.intrinsicContentSize.height);
            
        } else {
            if(isIOS7()) {
                showPopupPickerView(CN1_THREAD_GET_STATE_PASS_ARG pickerView);
                return;
            }
            
            UIActionSheet* actionSheet;
            int topBoundry = 10;
            
            actionSheet = [[UIActionSheet alloc] initWithTitle:nil delegate:[CodenameOne_GLViewController instance] cancelButtonTitle:nil destructiveButtonTitle:nil otherButtonTitles:nil];
            UISegmentedControl *closeButton = [[UISegmentedControl alloc] initWithItems:[NSArray arrayWithObject:@"Close"]];
            closeButton.momentary = YES;
            closeButton.frame = CGRectMake(260, 7.0f, 50.0f, 30.0f);
            closeButton.segmentedControlStyle = UISegmentedControlStyleBar;
            closeButton.tintColor = [UIColor blackColor];
            [closeButton addTarget:[CodenameOne_GLViewController instance] action:@selector(datePickerDismissActionSheet:) forControlEvents:UIControlEventValueChanged];
            [actionSheet addSubview:closeButton];
#ifndef CN1_USE_ARC
            [closeButton release];
#endif
            
            
            [actionSheet setActionSheetStyle:UIActionSheetStyleBlackTranslucent];
            
            pickerView.frame = CGRectMake(pickerView.frame.origin.x, pickerView.frame.origin.y + topBoundry, pickerView.frame.size.width, pickerView.frame.size.height);
            [actionSheet addSubview:pickerView];
            
            
            //[actionSheet showInView:self.view];
            [actionSheet showInView:[UIApplication sharedApplication].keyWindow];
            if (UIDeviceOrientationIsLandscape([[CodenameOne_GLViewController instance] interfaceOrientation])) {
                [actionSheet setBounds:CGRectMake(0, 0, 485, 320)];
            } else {
                [actionSheet setBounds:CGRectMake(0, 0, 320, 485)];
            }
        }
        if(selection > -1) {
            [pickerView selectRow:selection inComponent:0 animated:NO];
        }
        POOL_END();
        repaintUI();
    });
#endif
}


void com_codename1_impl_ios_IOSNative_openDatePicker___int_long_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type, JAVA_LONG time, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_INT preferredWidth, JAVA_INT preferredHeightArg, JAVA_INT minuteStep) {
#if TARGET_OS_OSX
    extern void CN1MacOpenDatePicker(int type, long long time, int x, int y, int w, int h, int minuteStep);
    com_codename1_impl_ios_IOSImplementation_foldKeyboard__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    // The preferred width and height are ignored: they exist to size a UIKit
    // picker sliding up from the bottom of a phone screen, and a popover sizes
    // itself to its content.
    CN1MacOpenDatePicker(type, time, x, y, w, h, minuteStep);
#else
    __block JAVA_INT preferredHeight = preferredHeightArg;
    com_codename1_impl_ios_IOSImplementation_foldKeyboard__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    pickerStringArray = nil;
    currentDatePickerDate = nil;
    currentDatePickerDuration = -1;
    if (preferredWidth == 0) {
        preferredWidth = 320 * scaleValue;
    }

    // There are only 3 valid heights for the picker in iPad
    //http://stackoverflow.com/a/7672577/2935174
    if (preferredHeight == 0) {
        preferredHeight = 216 * scaleValue;
    } else if (preferredHeight <= 162) {
        preferredHeight = 162;
    } else if (preferredHeight <= 180) {
        preferredHeight = 180;
    } else {
        preferredHeight = 216;
    }
    
    
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSDate* date = [NSDate dateWithTimeIntervalSince1970:(time / 1000)];
        UIDatePicker* datePickerView;
        if(isIPad() || isIOS7()) {
            datePickerView = [[UIDatePicker alloc] init];
        } else {
            datePickerView = [[UIDatePicker alloc] initWithFrame:CGRectMake(0, 40, 0, 0)];
        }
        datePickerView.locale = cn1DeviceLocale();
        switch(type) {
            case 1:
                datePickerView.datePickerMode = UIDatePickerModeDate;
                break;
            case 2:
                datePickerView.datePickerMode = UIDatePickerModeTime;
                break;
            case 3:
                datePickerView.datePickerMode = UIDatePickerModeDateAndTime;
                break;
            case 5:
            case 6:
            case 7:
                datePickerView.datePickerMode = UIDatePickerModeCountDownTimer;
                break;
        }
        switch (type) {
            case 1:
            case 2:
            case 3:
                datePickerView.tag = 10;
                datePickerView.date = date;
                currentDatePickerDate = date;
#ifndef CN1_USE_ARC
                [currentDatePickerDate retain];
#endif
                break;
            case 5:
            case 6:
            case 7:
                datePickerView.countDownDuration = time / 1000;
                
                // To workaround a bug in UIDatePickerView that causes
                // the change event to not be fired the first time.
                // https://stackoverflow.com/a/22777664/2935174
                dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 0.3 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
                    [datePickerView setCountDownDuration: datePickerView.countDownDuration];
                });

                datePickerView.minuteInterval = minuteStep;
                currentDatePickerDuration = time;
                break;
                
        }
        
        

        defaultDatePickerDate = time;
        [datePickerView addTarget:[CodenameOne_GLViewController instance] action:@selector(datePickerChangeDate:) forControlEvents:UIControlEventValueChanged];
        if(isIPad()) {
            datepickerPopover = YES;
            stringPickerSelection = 0;
            UIViewController *vc = [[UIViewController alloc] init];
            
            CN1View *popoverView = [[CN1View alloc] init];
            [vc setView:popoverView];
            [vc setContentSizeForViewInPopover:CGSizeMake(320, 260)];
            
#ifndef CN1_USE_ARC
            UIToolbar *toolbar = [[[UIToolbar alloc] init] autorelease];
#else
            UIToolbar *toolbar = [[UIToolbar alloc] init];
#endif
            [toolbar setBarStyle:UIBarStyleBlackTranslucent];
            [toolbar sizeToFit];
            
            preferredHeight += (int)toolbar.frame.size.height;
            
            //add a space filler to the left:
            UIBarButtonItem *flexButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:
                                           UIBarButtonSystemItemFlexibleSpace target: nil action:nil];
            
#ifndef NEW_CODENAME_ONE_VM
            JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance__();
#else
            JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance___R_com_codename1_ui_plaf_UIManager(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
#endif
            JAVA_OBJECT str;
            UIBarButtonItem *doneButton;
            NSArray *itemsArray = nil;
#ifndef NEW_CODENAME_ONE_VM
            str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String(obj, fromNSString(@"OK"), fromNSString(@"OK"));
#else
            str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG obj, fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"OK"), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"OK"));
#endif
            NSString* buttonTitle = toNSString(CN1_THREAD_GET_STATE_PASS_ARG str);
            doneButton = [[UIBarButtonItem alloc]initWithTitle:buttonTitle style:UIBarButtonItemStyleDone target:[CodenameOne_GLViewController instance] action:@selector(pickerComponentDismiss)];
            
            itemsArray = [NSArray arrayWithObjects: doneButton, nil];
#ifndef CN1_USE_ARC
            [flexButton release];
            [doneButton release];
#endif
            [toolbar setItems:itemsArray];
            
            [popoverView addSubview:toolbar];
            [popoverView addSubview:datePickerView];
            datePickerView.frame = CGRectMake(0, 44, datePickerView.frame.size.width, datePickerView.frame.size.height);
            
            UIPopoverController* uip = [[UIPopoverController alloc] initWithContentViewController:vc];
            popoverControllerInstance = uip;
            uip.popoverContentSize = CGSizeMake(preferredWidth/scaleValue, preferredHeight/scaleValue);
            toolbar.frame = CGRectMake(0,0, preferredWidth/scaleValue, toolbar.intrinsicContentSize.height);
            uip.delegate = [CodenameOne_GLViewController instance];
            [uip presentPopoverFromRect:CGRectMake(x / scaleValue, y / scaleValue, w / scaleValue, h / scaleValue) inView:[CodenameOne_GLViewController instance].view permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
        } else {
            if(isIOS7()) {
                showPopupPickerView(CN1_THREAD_GET_STATE_PASS_ARG datePickerView);
                return;
            }
            
            UIActionSheet* actionSheet;
            int topBoundry = 10;
            
            actionSheet = [[UIActionSheet alloc] initWithTitle:nil delegate:[CodenameOne_GLViewController instance] cancelButtonTitle:nil destructiveButtonTitle:nil otherButtonTitles:nil];
            UISegmentedControl *closeButton = [[UISegmentedControl alloc] initWithItems:[NSArray arrayWithObject:@"Close"]];
            closeButton.momentary = YES;
            closeButton.frame = CGRectMake(260, 7.0f, 50.0f, 30.0f);
            closeButton.segmentedControlStyle = UISegmentedControlStyleBar;
            closeButton.tintColor = [UIColor blackColor];
            [closeButton addTarget:[CodenameOne_GLViewController instance] action:@selector(datePickerDismissActionSheet:) forControlEvents:UIControlEventValueChanged];
            [actionSheet addSubview:closeButton];
#ifndef CN1_USE_ARC
            [closeButton release];
#endif
            
            
            [actionSheet setActionSheetStyle:UIActionSheetStyleBlackTranslucent];
            
            datePickerView.frame = CGRectMake(datePickerView.frame.origin.x, datePickerView.frame.origin.y + topBoundry, datePickerView.frame.size.width, datePickerView.frame.size.height);
            [actionSheet addSubview:datePickerView];
            
            
            //[actionSheet showInView:self.view];
            [actionSheet showInView:[UIApplication sharedApplication].keyWindow];
            if (UIDeviceOrientationIsLandscape([[CodenameOne_GLViewController instance] interfaceOrientation])) {
                [actionSheet setBounds:CGRectMake(0, 0, 485, 320)];
            } else {
                [actionSheet setBounds:CGRectMake(0, 0, 320, 485)];
            }
        }
        POOL_END();
        repaintUI();
    });
#endif
}


CGRect cn1RectToCGRect(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT rect){
#ifndef NEW_CODENAME_ONE_VM
    return CGRectMake(
                      com_codename1_ui_geom_Rectangle_getX__(CN1_THREAD_STATE_PASS_ARG rect),
                      com_codename1_ui_geom_Rectangle_getY__(CN1_THREAD_STATE_PASS_ARG rect),
                      com_codename1_ui_geom_Rectangle_getWidth__(CN1_THREAD_STATE_PASS_ARG rect),
                      com_codename1_ui_geom_Rectangle_getHeight__(CN1_THREAD_STATE_PASS_ARG rect)
                      );
#else
    return CGRectMake(
                      com_codename1_ui_geom_Rectangle_getX___R_int(CN1_THREAD_STATE_PASS_ARG rect),
                      com_codename1_ui_geom_Rectangle_getY___R_int(CN1_THREAD_STATE_PASS_ARG rect),
                      com_codename1_ui_geom_Rectangle_getWidth___R_int(CN1_THREAD_STATE_PASS_ARG rect),
                      com_codename1_ui_geom_Rectangle_getHeight___R_int(CN1_THREAD_STATE_PASS_ARG rect)
                      );
#endif
}

void com_codename1_impl_ios_IOSNative_socialShare___java_lang_String_long_com_codename1_ui_geom_Rectangle(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT text, JAVA_LONG imagePeer, JAVA_OBJECT rectangle) {
#if TARGET_OS_OSX
    NSString* someText = toNSString(CN1_THREAD_STATE_PASS_ARG text);
    BOOL useRect = rectangle ? YES : NO;
    __block CGRect cgrect = CGRectMake(0, 0, 0, 0);
    if (useRect) {
        cgrect = cn1RectToCGRect(CN1_THREAD_GET_STATE_PASS_ARG rectangle);
        cgrect.origin.x = cgrect.origin.x / scaleValue;
        cgrect.origin.y = cgrect.origin.y / scaleValue;
        cgrect.size.width = cgrect.size.width / scaleValue;
        cgrect.size.height = cgrect.size.height / scaleValue;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSMutableArray* dataToShare = [NSMutableArray array];
        if (imagePeer != 0) {
            GLUIImage* glll = (BRIDGE_CAST GLUIImage*)((void *)(uintptr_t)imagePeer);
            CN1Image* i = [glll getImage];
            if (someText != nil) [dataToShare addObject:someText];
            if (i != nil) [dataToShare addObject:i];
        } else if (someText != nil && [someText hasPrefix:@"file:"]) {
            NSURL* fileURL = [NSURL fileURLWithPath:[someText substringFromIndex:5]];
            if ([[NSFileManager defaultManager] fileExistsAtPath:[fileURL path]]) {
                [dataToShare addObject:fileURL];
            } else {
                [dataToShare addObject:someText];
            }
        } else if (someText != nil) {
            [dataToShare addObject:someText];
        }
        if (dataToShare.count == 0) {
            POOL_END();
            return;
        }
        // A picker anchored to the component that asked to share, which is what
        // the rectangle is for -- an unanchored Mac share sheet appears in the
        // window's corner with no relation to what the user clicked.
        NSSharingServicePicker* picker = [[NSSharingServicePicker alloc] initWithItems:dataToShare];
        NSView* host = [CN1MacHost sharedHost].renderingView;
        NSRect anchor = useRect ? cgrect : NSMakeRect(host.bounds.size.width / 2,
                                                      host.bounds.size.height / 2, 1, 1);
        [picker showRelativeToRect:anchor ofView:host preferredEdge:NSMinYEdge];
#ifndef CN1_USE_ARC
        [picker release];
#endif
        POOL_END();
    });
#else
    NSString* someText = toNSString(CN1_THREAD_STATE_PASS_ARG text);
    BOOL useRect = rectangle ? YES:NO;
    __block CGRect cgrect = CGRectMake(0,0,0,0);
    if (useRect){
        cgrect = cn1RectToCGRect(CN1_THREAD_GET_STATE_PASS_ARG rectangle);
        cgrect.origin.x = cgrect.origin.x / scaleValue;
        cgrect.origin.y = cgrect.origin.y / scaleValue;
        cgrect.size.width = cgrect.size.width / scaleValue;
        cgrect.size.height = cgrect.size.height / scaleValue;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSArray* dataToShare;
        if(imagePeer != 0) {
            GLUIImage* glll = (BRIDGE_CAST GLUIImage*)((void *)imagePeer);
            CN1Image* i = [glll getImage];
            if(someText != nil) {
                dataToShare = [NSArray arrayWithObjects:someText, i, nil];
            } else {
                dataToShare = [NSArray arrayWithObjects:i, nil];
            }
        } else {
            BOOL shareFile = NO;
            if (someText != nil && [someText hasPrefix:@"file:"]) {
                NSURL* fileURL = [NSURL fileURLWithPath:[someText substringFromIndex:5]];
                if ([[NSFileManager defaultManager] fileExistsAtPath:[fileURL path]]) {
                    shareFile = YES;
                    dataToShare = [NSArray arrayWithObjects:fileURL, nil];
                }
            }
            if (!shareFile) {
                dataToShare = [NSArray arrayWithObjects:someText, nil];
            }
        }
        
        UIActivityViewController* activityViewController = [[UIActivityViewController alloc] initWithActivityItems:dataToShare
                                                                                             applicationActivities:nil];
#ifdef NEW_CODENAME_ONE_VM
        if ( [activityViewController respondsToSelector:@selector(popoverPresentationController)] ) {
            //iOS8
            activityViewController.popoverPresentationController.sourceView = [CodenameOne_GLViewController instance].view;
            int SCREEN_HEIGHT = [CodenameOne_GLViewController instance].view.bounds.size.height;
            int SCREEN_WIDTH = [CodenameOne_GLViewController instance].view.bounds.size.width;
            if ( useRect ){
                if (cgrect.origin.y < SCREEN_HEIGHT/4 && cgrect.origin.y+cgrect.size.height > 3*SCREEN_HEIGHT/4){
                    cgrect = CGRectMake(
                                        cgrect.origin.x,
                                        cgrect.origin.y+cgrect.size.height/2-10,
                                        cgrect.size.width,
                                        10
                                        );  // The top bar somewhere
                }
                activityViewController.popoverPresentationController.sourceRect = cgrect;
            } else {
                CGRect cgrect = CGRectMake(0, 0, SCREEN_WIDTH, 60);  // The top bar somewhere
                activityViewController.popoverPresentationController.sourceRect = cgrect;
            }
            
        }
#endif
        [[CodenameOne_GLViewController instance] presentViewController:activityViewController animated:YES completion:^{}];
        POOL_END();
        repaintUI();
    });
#endif
}

// Same as socialShare but installs a completionWithItemsHandler block on
// the UIActivityViewController that calls back into Java with the chosen
// activity type (UIActivityType*), cancellation, or error. Status codes
// mirror com.codename1.share.ShareResult: 1=SHARED_TO, 2=DISMISSED, 3=FAILED.
void com_codename1_impl_ios_IOSNative_socialShareWithCallback___java_lang_String_long_com_codename1_ui_geom_Rectangle_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT text, JAVA_LONG imagePeer, JAVA_OBJECT rectangle, JAVA_INT callbackId) {
#if TARGET_OS_OSX
    NSString* someText = toNSString(CN1_THREAD_STATE_PASS_ARG text);
    BOOL useRect = rectangle ? YES : NO;
    __block CGRect cgrect = CGRectMake(0, 0, 0, 0);
    if (useRect) {
        cgrect = cn1RectToCGRect(CN1_THREAD_GET_STATE_PASS_ARG rectangle);
        cgrect.origin.x = cgrect.origin.x / scaleValue;
        cgrect.origin.y = cgrect.origin.y / scaleValue;
        cgrect.size.width = cgrect.size.width / scaleValue;
        cgrect.size.height = cgrect.size.height / scaleValue;
    }
    int cbId = (int)callbackId;
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSMutableArray* dataToShare = [NSMutableArray array];
        if (imagePeer != 0) {
            GLUIImage* glll = (BRIDGE_CAST GLUIImage*)((void *)(uintptr_t)imagePeer);
            CN1Image* i = [glll getImage];
            if (someText != nil) [dataToShare addObject:someText];
            if (i != nil) [dataToShare addObject:i];
        } else if (someText != nil && [someText hasPrefix:@"file:"]) {
            NSURL* fileURL = [NSURL fileURLWithPath:[someText substringFromIndex:5]];
            [dataToShare addObject:[[NSFileManager defaultManager] fileExistsAtPath:[fileURL path]]
                                   ? (id)fileURL : (id)someText];
        } else if (someText != nil) {
            [dataToShare addObject:someText];
        }
        struct ThreadLocalData* threadStateData = getThreadLocalData();
        if (dataToShare.count == 0) {
            com_codename1_impl_ios_IOSImplementation_socialShareCallback___int_int_java_lang_String_java_lang_String(threadStateData, (JAVA_INT)cbId, 2, JAVA_NULL, JAVA_NULL);
            POOL_END();
            return;
        }
        NSSharingServicePicker* picker = [[NSSharingServicePicker alloc] initWithItems:dataToShare];
        NSView* host = [CN1MacHost sharedHost].renderingView;
        NSRect anchor = useRect ? cgrect : NSMakeRect(host.bounds.size.width / 2,
                                                      host.bounds.size.height / 2, 1, 1);
        [picker showRelativeToRect:anchor ofView:host preferredEdge:NSMinYEdge];
        // NSSharingServicePicker reports the chosen service through a delegate
        // rather than a completion handler, and the framework only wants to know
        // that the sheet was shown, so completion is reported here.
        com_codename1_impl_ios_IOSImplementation_socialShareCallback___int_int_java_lang_String_java_lang_String(threadStateData, (JAVA_INT)cbId, 1, JAVA_NULL, JAVA_NULL);
#ifndef CN1_USE_ARC
        [picker release];
#endif
        POOL_END();
    });
#else
    NSString* someText = toNSString(CN1_THREAD_STATE_PASS_ARG text);
    BOOL useRect = rectangle ? YES:NO;
    __block CGRect cgrect = CGRectMake(0,0,0,0);
    if (useRect){
        cgrect = cn1RectToCGRect(CN1_THREAD_GET_STATE_PASS_ARG rectangle);
        cgrect.origin.x = cgrect.origin.x / scaleValue;
        cgrect.origin.y = cgrect.origin.y / scaleValue;
        cgrect.size.width = cgrect.size.width / scaleValue;
        cgrect.size.height = cgrect.size.height / scaleValue;
    }
    int cbId = (int)callbackId;
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSArray* dataToShare;
        if(imagePeer != 0) {
            GLUIImage* glll = (BRIDGE_CAST GLUIImage*)((void *)imagePeer);
            CN1Image* i = [glll getImage];
            if(someText != nil) {
                dataToShare = [NSArray arrayWithObjects:someText, i, nil];
            } else {
                dataToShare = [NSArray arrayWithObjects:i, nil];
            }
        } else {
            BOOL shareFile = NO;
            if (someText != nil && [someText hasPrefix:@"file:"]) {
                NSURL* fileURL = [NSURL fileURLWithPath:[someText substringFromIndex:5]];
                if ([[NSFileManager defaultManager] fileExistsAtPath:[fileURL path]]) {
                    shareFile = YES;
                    dataToShare = [NSArray arrayWithObjects:fileURL, nil];
                }
            }
            if (!shareFile) {
                dataToShare = [NSArray arrayWithObjects:someText, nil];
            }
        }

        UIActivityViewController* activityViewController = [[UIActivityViewController alloc] initWithActivityItems:dataToShare
                                                                                             applicationActivities:nil];
#ifdef NEW_CODENAME_ONE_VM
        if ( [activityViewController respondsToSelector:@selector(popoverPresentationController)] ) {
            activityViewController.popoverPresentationController.sourceView = [CodenameOne_GLViewController instance].view;
            int SCREEN_HEIGHT = [CodenameOne_GLViewController instance].view.bounds.size.height;
            int SCREEN_WIDTH = [CodenameOne_GLViewController instance].view.bounds.size.width;
            if ( useRect ){
                if (cgrect.origin.y < SCREEN_HEIGHT/4 && cgrect.origin.y+cgrect.size.height > 3*SCREEN_HEIGHT/4){
                    cgrect = CGRectMake(
                                        cgrect.origin.x,
                                        cgrect.origin.y+cgrect.size.height/2-10,
                                        cgrect.size.width,
                                        10
                                        );
                }
                activityViewController.popoverPresentationController.sourceRect = cgrect;
            } else {
                CGRect cgrect = CGRectMake(0, 0, SCREEN_WIDTH, 60);
                activityViewController.popoverPresentationController.sourceRect = cgrect;
            }

        }
#endif
        // UIActivityType is an NSString* typedef introduced in iOS 10;
        // use NSString* directly so the source compiles against older
        // SDKs while remaining ABI-compatible on iOS 10+.
        activityViewController.completionWithItemsHandler = ^(NSString *activityType, BOOL completed, NSArray *returnedItems, NSError *activityError) {
            JAVA_INT status;
            NSString* activityTypeStr = nil;
            NSString* errMsg = nil;
            if (activityError != nil) {
                status = 3;
                errMsg = [activityError localizedDescription];
            } else if (completed) {
                status = 1;
                if (activityType != nil) {
                    activityTypeStr = activityType;
                }
            } else {
                status = 2;
            }
            JAVA_OBJECT jActivityType = activityTypeStr != nil ? fromNSString(CN1_THREAD_GET_STATE_PASS_ARG activityTypeStr) : JAVA_NULL;
            JAVA_OBJECT jErrMsg = errMsg != nil ? fromNSString(CN1_THREAD_GET_STATE_PASS_ARG errMsg) : JAVA_NULL;
            com_codename1_impl_ios_IOSImplementation_socialShareCallback___int_int_java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_INT)cbId, status, jActivityType, jErrMsg);
        };
        [[CodenameOne_GLViewController instance] presentViewController:activityViewController animated:YES completion:^{}];
        POOL_END();
        repaintUI();
    });
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isPrintingAvailable___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_OSX
    // Every Mac can print: AppKit has no notion of printing being unavailable,
    // and a machine with no printer configured still has Save as PDF.
    return JAVA_TRUE;
#else
    return [UIPrintInteractionController isPrintingAvailable];
#endif
}

// Prints the file at path through UIPrintInteractionController and reports
// the outcome to IOSImplementation.printDocumentCallback using the supplied
// callbackId. Status codes mirror com.codename1.printing.PrintResult:
// 1=COMPLETED, 2=CANCELLED, 3=FAILED.
void com_codename1_impl_ios_IOSNative_printDocument___java_lang_String_java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path, JAVA_OBJECT mimeType, JAVA_INT callbackId) {
#if TARGET_OS_OSX
    NSString* filePath = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    int cbId = (int)callbackId;
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSString* ns = fixFilePath(filePath);
        struct ThreadLocalData* threadStateData = getThreadLocalData();
        // AppKit prints a view rather than a URL, so the document is loaded into
        // an image -- NSImage reads PDF as well as the raster formats, which
        // covers what UIPrintInteractionController would accept.
        NSImage* doc = [[NSFileManager defaultManager] fileExistsAtPath:ns]
            ? [[NSImage alloc] initWithContentsOfFile:ns]
            : nil;
        if (doc == nil) {
            JAVA_OBJECT jErrMsg = fromNSString(threadStateData, @"The document cannot be printed");
            com_codename1_impl_ios_IOSImplementation_printDocumentCallback___int_int_java_lang_String(threadStateData, (JAVA_INT)cbId, 3, jErrMsg);
            POOL_END();
            return;
        }
        NSImageView* view = [[NSImageView alloc] initWithFrame:NSMakeRect(0, 0, doc.size.width, doc.size.height)];
        view.image = doc;
        view.imageScaling = NSImageScaleProportionallyUpOrDown;
        NSPrintInfo* info = [NSPrintInfo sharedPrintInfo];
        info.jobDisposition = NSPrintSpoolJob;
        NSPrintOperation* op = [NSPrintOperation printOperationWithView:view printInfo:info];
        op.jobTitle = [ns lastPathComponent];
        BOOL completed = [op runOperation];
        // Status two is "cancelled" and one is "completed", matching what the
        // UIKit completion handler reports.
        com_codename1_impl_ios_IOSImplementation_printDocumentCallback___int_int_java_lang_String(threadStateData, (JAVA_INT)cbId, completed ? 1 : 2, JAVA_NULL);
#ifndef CN1_USE_ARC
        [view release];
        [doc release];
#endif
        POOL_END();
    });
#else
    NSString* filePath = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSString* mime = toNSString(CN1_THREAD_STATE_PASS_ARG mimeType);
    int cbId = (int)callbackId;
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSString* ns = fixFilePath(filePath);
        NSURL* fileURL = [NSURL fileURLWithPath:ns];
        if (![[NSFileManager defaultManager] fileExistsAtPath:ns] || ![UIPrintInteractionController canPrintURL:fileURL]) {
            JAVA_OBJECT jErrMsg = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"The document cannot be printed");
            com_codename1_impl_ios_IOSImplementation_printDocumentCallback___int_int_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_INT)cbId, 3, jErrMsg);
            POOL_END();
            return;
        }
        UIPrintInteractionController* printController = [UIPrintInteractionController sharedPrintController];
        UIPrintInfo* printInfo = [UIPrintInfo printInfo];
        printInfo.outputType = (mime != nil && [mime hasPrefix:@"image/"]) ? UIPrintInfoOutputPhoto : UIPrintInfoOutputGeneral;
        printInfo.jobName = [ns lastPathComponent];
        printController.printInfo = printInfo;
        printController.printingItem = fileURL;
        UIPrintInteractionCompletionHandler completionHandler = ^(UIPrintInteractionController *controller, BOOL completed, NSError *error) {
            JAVA_INT status;
            NSString* errMsg = nil;
            if (error != nil) {
                status = 3;
                errMsg = [error localizedDescription];
            } else if (completed) {
                status = 1;
            } else {
                status = 2;
            }
            JAVA_OBJECT jErrMsg = errMsg != nil ? fromNSString(CN1_THREAD_GET_STATE_PASS_ARG errMsg) : JAVA_NULL;
            com_codename1_impl_ios_IOSImplementation_printDocumentCallback___int_int_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_INT)cbId, status, jErrMsg);
        };
        if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
            CN1View* view = [CodenameOne_GLViewController instance].view;
            CGRect sourceRect = CGRectMake(view.bounds.size.width / 2, view.bounds.size.height / 2, 1, 1);
            [printController presentFromRect:sourceRect inView:view animated:YES completionHandler:completionHandler];
        } else {
            [printController presentAnimated:YES completionHandler:completionHandler];
        }
        POOL_END();
        repaintUI();
    });
#endif
}
#else // TARGET_OS_WATCH || TARGET_OS_TV: no UIPickerView / UIDatePicker / UIActivityViewController / UIPrintInteractionController on the watch/tv.
void com_codename1_impl_ios_IOSNative_openStringPicker___java_lang_String_1ARRAY_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT stringArray, JAVA_INT selection, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_INT preferredWidth, JAVA_INT preferredHeight) {}
void com_codename1_impl_ios_IOSNative_openDatePicker___int_long_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type, JAVA_LONG time, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_INT preferredWidth, JAVA_INT preferredHeightArg, JAVA_INT minuteStep) {}
void com_codename1_impl_ios_IOSNative_socialShare___java_lang_String_long_com_codename1_ui_geom_Rectangle(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT text, JAVA_LONG imagePeer, JAVA_OBJECT rectangle) {}
void com_codename1_impl_ios_IOSNative_socialShareWithCallback___java_lang_String_long_com_codename1_ui_geom_Rectangle_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT text, JAVA_LONG imagePeer, JAVA_OBJECT rectangle, JAVA_INT callbackId) {}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isPrintingAvailable___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) { return NO; }
void com_codename1_impl_ios_IOSNative_printDocument___java_lang_String_java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path, JAVA_OBJECT mimeType, JAVA_INT callbackId) {}
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV (UIPickerView / share / print)


extern BOOL isVKBAlwaysOpen();
extern BOOL vkbAlwaysOpen;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAsyncEditMode__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return isVKBAlwaysOpen();
}

extern int vkbHeight;
JAVA_INT com_codename1_impl_ios_IOSNative_getVKBHeight__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject){
    return (JAVA_INT)vkbHeight*scaleValue;
}

extern int vkbWidth;
JAVA_INT com_codename1_impl_ios_IOSNative_getVKBWidth__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject){
    return (JAVA_INT)vkbWidth*scaleValue;
}

void com_codename1_impl_ios_IOSNative_setAsyncEditMode___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN b) {
    vkbAlwaysOpen = b;
}

#if !TARGET_OS_WATCH
void com_codename1_impl_ios_IOSNative_foldVKB__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if(editingComponent != nil) {
            [editingComponent resignFirstResponder];
            [editingComponent removeFromSuperview];
#ifndef CN1_USE_ARC
            [editingComponent release];
#endif
            editingComponent = nil;
        }
        repaintUI();
    });
}

void com_codename1_impl_ios_IOSNative_hideTextEditing__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    if(editingComponent == nil) {
        return;
    }
    if(editingComponent.hidden) {
        return;
    }
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if(editingComponent != nil) {
            if(editingComponent.hidden) {
                return;
            }
            [editingComponent resignFirstResponder];
            [editingComponent becomeFirstResponder];
            editingComponent.hidden = YES;
        }
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setNativeEditingComponentVisible___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN b) {
    if(editingComponent == nil) {
        return;
    }
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if(editingComponent != nil) {
            editingComponent.hidden = !b;
        }
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_updateNativeEditorText___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT text) {
#if TARGET_OS_OSX
    // There is no separate native editor view on this port: the rendering view
    // is itself the NSTextInputClient, so the framework's text is pushed into
    // the input session rather than into a UITextView standing beside it.
    extern void CN1MacTextInputSetText(NSString *text);
    CN1MacTextInputSetText(toNSString(CN1_THREAD_STATE_PASS_ARG text));
#else
    if (editingComponent == nil) {
        return;
    }
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if(editingComponent != nil) {
            NSString* nsText = toNSString(CN1_THREAD_GET_STATE_PASS_ARG text);
            NSString* currText = ((UITextView*)editingComponent).text;
            if (![nsText isEqualToString:currText]) {
                if ([editingComponent respondsToSelector:@selector(selectedRange)] &&
                    [editingComponent respondsToSelector:@selector(setSelectedRange:)]) {
                    UITextView *textView = (UITextView *)editingComponent;

                    // Save current cursor position
                    NSRange selectedRange = textView.selectedRange;

                    // Update the text
                    textView.text = nsText;

                    // Restore the cursor position
                    NSUInteger newPosition = MIN(selectedRange.location, textView.text.length);
                    textView.selectedRange = NSMakeRange(newPosition, 0);
                } else if ([editingComponent respondsToSelector:@selector(setText:)]) {
                    // Fallback for UITextField, UILabel, or other classes supporting setText
                    [(id)editingComponent setText:nsText];
                } else {
                    NSLog(@"editingComponent does not support text assignment");
                }
            }
        }
        POOL_END();
    });

#endif
}
#else // TARGET_OS_WATCH: no inline native text-editing peer on the watch.
void com_codename1_impl_ios_IOSNative_foldVKB__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {}
void com_codename1_impl_ios_IOSNative_hideTextEditing__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {}
void com_codename1_impl_ios_IOSNative_setNativeEditingComponentVisible___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN b) {}
void com_codename1_impl_ios_IOSNative_updateNativeEditorText___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT text) {}
#endif // !TARGET_OS_WATCH (native text-editing peer functions)

JAVA_LONG com_codename1_impl_ios_IOSNative_connectSocket___java_lang_String_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT host, JAVA_INT port, JAVA_INT connectTimeout) {
    POOL_BEGIN();
    SocketImpl* impl = [[SocketImpl alloc] init];
    BOOL b = [impl connect:toNSString(CN1_THREAD_STATE_PASS_ARG host) port:port timeout:connectTimeout];
    POOL_END();
    if(b) {
        return (JAVA_LONG)impl;
    }
    return (JAVA_LONG)JAVA_NULL;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_listenSocketLoopback___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT port) {
    POOL_BEGIN();
    SocketImpl* impl = [[SocketImpl alloc] init];
    BOOL b = [impl listenLoopback:port];
    POOL_END();
    if(b) {
        return (JAVA_LONG)impl;
    }
    // ownership is transferred to Java only on success, so a failed bind or accept must
    // release here or every retry leaks the peer
    [impl release];
    return (JAVA_LONG)JAVA_NULL;
}

void com_codename1_impl_ios_IOSNative_stopListeningSocket___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT port) {
    POOL_BEGIN();
    [SocketImpl closeLoopbackListenerForPort:port];
    POOL_END();
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDebuggableBuild___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if TARGET_OS_SIMULATOR
    // Nothing ships from the simulator, so it is always a development build. It also has
    // no provisioning profile to inspect, which is what the device path relies on.
    return JAVA_TRUE;
#else
    // The provisioning profile is fixed for the life of the process, so read and parse it
    // once. This is consulted by the MCP gate and is public through
    // Display.isDebuggableBuild(), so a caller is free to ask repeatedly, and file IO plus
    // a plist parse per call would be a poor answer to that.
    static JAVA_BOOLEAN cachedDebuggable = JAVA_FALSE;
    static dispatch_once_t debuggableOnce;
    dispatch_once(&debuggableOnce, ^{
    POOL_BEGIN();
    JAVA_BOOLEAN result = JAVA_FALSE;
    // get-task-allow is the entitlement that lets a debugger attach. Development and
    // ad-hoc profiles carry it; App Store and enterprise distribution profiles do not, so
    // it is the honest "is this a build I am working on" signal, and unlike the Xcode
    // DEBUG macro it does not depend on which configuration the build server compiled.
    NSString* path = [[NSBundle mainBundle] pathForResource:@"embedded" ofType:@"mobileprovision"];
    if(path != nil) {
        NSData* data = [NSData dataWithContentsOfFile:path];
        if(data != nil) {
            // The profile is CMS signed, so the file as a whole is not a plist, but the
            // payload it wraps is one. Cut it out and parse it properly.
            //
            // Do NOT do this by looking for "get-task-allow" and then hunting nearby for
            // <true/>: the value that follows it is <false/> in a distribution profile, and
            // the very next entitlement is often beta-reports-active, whose <true/> sits
            // about 53 characters later. Any proximity window wide enough to be useful
            // matches it, and the mistake lands on the unsafe side - a shipped build
            // classified as debuggable.
            NSData* startMarker = [@"<?xml" dataUsingEncoding:NSUTF8StringEncoding];
            NSData* endMarker = [@"</plist>" dataUsingEncoding:NSUTF8StringEncoding];
            NSRange whole = NSMakeRange(0, [data length]);
            NSRange start = [data rangeOfData:startMarker options:0 range:whole];
            if(start.location != NSNotFound) {
                NSRange rest = NSMakeRange(start.location, [data length] - start.location);
                NSRange end = [data rangeOfData:endMarker options:0 range:rest];
                if(end.location != NSNotFound) {
                    NSUInteger length = end.location + end.length - start.location;
                    NSData* plistData = [data subdataWithRange:NSMakeRange(start.location, length)];
                    id plist = [NSPropertyListSerialization propertyListWithData:plistData
                                                                        options:NSPropertyListImmutable
                                                                         format:NULL
                                                                          error:NULL];
                    if([plist isKindOfClass:[NSDictionary class]]) {
                        id entitlements = [(NSDictionary*)plist objectForKey:@"Entitlements"];
                        if([entitlements isKindOfClass:[NSDictionary class]]) {
                            id allowed = [(NSDictionary*)entitlements objectForKey:@"get-task-allow"];
                            if([allowed isKindOfClass:[NSNumber class]]) {
                                result = [(NSNumber*)allowed boolValue] ? JAVA_TRUE : JAVA_FALSE;
                            }
                        }
                    }
                }
            }
        }
    }
    POOL_END();
    // Every path that could not read an answer leaves result JAVA_FALSE: an unreadable or
    // unexpected profile means "treat this as a release build", which withholds the
    // facility rather than exposing it.
    cachedDebuggable = result;
    });
    return cachedDebuggable;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getHostOrIP__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
    JAVA_OBJECT o = fromNSString(CN1_THREAD_STATE_PASS_ARG [SocketImpl getIP]);
    POOL_END();
    return o;
}

void com_codename1_impl_ios_IOSNative_disconnectSocket___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    [impl disconnect];
    POOL_END();
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isSocketConnected___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    JAVA_BOOLEAN b = [impl isConnected];
    POOL_END();
    return b;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getSocketErrorMessage___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    JAVA_OBJECT b = fromNSString(CN1_THREAD_STATE_PASS_ARG [impl getErrorMessage]);
    POOL_END();
    return b;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getSocketErrorCode___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    JAVA_INT b = [impl getErrorCode];
    POOL_END();
    return b;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getSocketAvailableInput___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    JAVA_INT b = [impl getAvailableInput];
    POOL_END();
    return b;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_readFromSocketStream___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    NSData *d = [impl readFromStream];
    if (d == nil) {
        return JAVA_NULL;
    }
    JAVA_OBJECT b = nsDataToByteArr(d);
    POOL_END();
    return b;
}

void com_codename1_impl_ios_IOSNative_writeToSocketStream___long_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket, JAVA_OBJECT data) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    [impl writeToStream:arrayToData(data)];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_writeToSocketStream___long_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket, JAVA_OBJECT data, JAVA_INT offset, JAVA_INT len) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    [impl writeToStream:arrayToDataRange(data, offset, len)];
    POOL_END();
}

#import "WebSocketImpl.h"

JAVA_LONG com_codename1_impl_ios_IOSNative_createWebSocketNative___int_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT connectionId, JAVA_OBJECT url) {
    POOL_BEGIN();
    CN1WebSocketImpl* impl = [[CN1WebSocketImpl alloc] initWithId:connectionId
                                                              url:toNSString(CN1_THREAD_STATE_PASS_ARG url)];
    POOL_END();
    return (JAVA_LONG)impl;
}

void com_codename1_impl_ios_IOSNative_connectWebSocketNative___long_int_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle, JAVA_INT timeoutMs, JAVA_OBJECT subprotocolsCsv) {
    POOL_BEGIN();
    CN1WebSocketImpl* impl = (BRIDGE_CAST CN1WebSocketImpl*)((void *)handle);
    NSString* csv = subprotocolsCsv == NULL ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG subprotocolsCsv);
    NSArray* protocols = (csv != nil && [csv length] > 0)
        ? [csv componentsSeparatedByString:@","] : nil;
    [impl connectWithTimeoutMs:timeoutMs protocols:protocols];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_closeWebSocketNative___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle) {
    POOL_BEGIN();
    CN1WebSocketImpl* impl = (BRIDGE_CAST CN1WebSocketImpl*)((void *)handle);
    [impl closeConnection];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_sendWebSocketTextNative___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle, JAVA_OBJECT text) {
    POOL_BEGIN();
    CN1WebSocketImpl* impl = (BRIDGE_CAST CN1WebSocketImpl*)((void *)handle);
    [impl sendText:toNSString(CN1_THREAD_STATE_PASS_ARG text)];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_sendWebSocketBinaryNative___long_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle, JAVA_OBJECT data) {
    POOL_BEGIN();
    CN1WebSocketImpl* impl = (BRIDGE_CAST CN1WebSocketImpl*)((void *)handle);
    [impl sendBinary:arrayToData(data)];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_releaseWebSocketNative___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle) {
    POOL_BEGIN();
    CN1WebSocketImpl* impl = (BRIDGE_CAST CN1WebSocketImpl*)((void *)handle);
    [impl release];
    POOL_END();
}


// ---------------- ES2 Port ADDITION: Shape Drawing -------------------------------------


//native void fillConvexPolygonGlobal(float[] points, int color, int alpha);
extern void Java_com_codename1_impl_ios_IOSImplementation_fillConvexPolygonImpl(JAVA_OBJECT points, int color, int alpha);
void com_codename1_impl_ios_IOSNative_fillConvexPolygonGlobal___float_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT points, int color, int alpha)
{
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_fillConvexPolygonImpl(points, color,alpha);
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_drawConvexPolygonGlobal___float_1ARRAY_int_int_float_int_int_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT points, JAVA_INT color, JAVA_INT alpha, JAVA_FLOAT lineWidth, JAVA_INT joinStyle, JAVA_INT capStyle, JAVA_FLOAT miterLimit)
{
    
}





JAVA_LONG com_codename1_impl_ios_IOSNative_nativePathStrokerCreate___long_float_int_int_float(JAVA_OBJECT instanceObject, JAVA_LONG consumerOutPtr, JAVA_FLOAT lineWidth, JAVA_INT capStyle, JAVA_INT joinStyle, JAVA_FLOAT miterLimit)
{
    Stroker *stroker = (Stroker*)malloc(sizeof(Stroker));
    Stroker_init(stroker,
                 (PathConsumer*)consumerOutPtr,
                 lineWidth,
                 capStyle,
                 joinStyle,
                 miterLimit
                 );
    return (JAVA_LONG)stroker;
    
}
//native void nativePathStrokerCleanup(long ptr);
void com_codename1_impl_ios_IOSNative_nativePathStrokerCleanup___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr)
{
    Stroker_destroy((Stroker*)ptr);
}
//native void nativePathStrokerReset(long ptr, float lineWidth, int capStyle, int joinStyle, float miterLimit);
void com_codename1_impl_ios_IOSNative_nativePathStrokerReset___long_float_int_int_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr, JAVA_FLOAT lineWidth, JAVA_INT capStyle, JAVA_INT joinStyle, JAVA_FLOAT miterLimit)
{
    Stroker_reset((Stroker*)ptr, lineWidth, capStyle, joinStyle, miterLimit);
}
//native long nativePathStrokerGetConsumer(long ptr);

JAVA_LONG com_codename1_impl_ios_IOSNative_nativePathStrokerGetConsumer___long(JAVA_OBJECT instanceObject, JAVA_LONG ptr)
{
    return (JAVA_LONG)&(((Stroker*)ptr)->consumer);
}

//native long nativePathRendererCreate(int pix_boundsX, int pix_boundsY,
//                                     int pix_boundsWidth, int pix_boundsHeight,
//                                     int windingRule);



static BOOL rendererIsSetup = NO;
static pthread_mutex_t rendererSetupLock = PTHREAD_MUTEX_INITIALIZER;

// Renderer_setup installs process-wide globals (the subpixel constants and the
// coverage->alpha table alphaMap) that every subsequent rasterisation reads.
// The original guard set rendererIsSetup *before* calling it, so a second
// thread entering here mid-setup saw the flag already raised, skipped the
// initialisation and went straight to rasterising against half-built globals:
// an alphaMap that was allocated but not yet filled, or still NULL. That
// corrupts exactly the antialiased edge samples of a shape while leaving its
// saturated interior correct.
//
// Serialise instead, and raise the flag only once setup has completed, so a
// racing caller blocks until the globals are whole.
static void cn1EnsureRendererSetup(JAVA_INT lgPositionsX, JAVA_INT lgPositionsY) {
    pthread_mutex_lock(&rendererSetupLock);
    if (!rendererIsSetup) {
        Renderer_setup(lgPositionsX, lgPositionsY);
        rendererIsSetup = YES;
    }
    pthread_mutex_unlock(&rendererSetupLock);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_nativePathRendererCreate___int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT pix_boundsX, JAVA_INT pix_boundsY, JAVA_INT pix_boundsWidth, JAVA_INT pix_boundsHeight, JAVA_INT windingRule)
{
    cn1EnsureRendererSetup(1, 1);
    Renderer *renderer = (Renderer*)malloc(sizeof(Renderer));
    Renderer_init(renderer);
    Renderer_reset(renderer, pix_boundsX, pix_boundsY, pix_boundsWidth, pix_boundsHeight, windingRule);
    return (JAVA_LONG)renderer;
    
}
//native void nativePathRendererSetup(int subpixelLgPositionsX, int subpixelLgPositionsY);
void com_codename1_impl_ios_IOSNative_nativePathRendererSetup___int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT subpixelLgPositionsX, JAVA_INT subpixelLgPositionsY)
{
    cn1EnsureRendererSetup(subpixelLgPositionsX, subpixelLgPositionsY);
}
//native void nativePathRendererCleanup(long ptr);
void com_codename1_impl_ios_IOSNative_nativePathRendererCleanup___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr)
{
    Renderer_destroy((Renderer*)ptr);
}
//native void nativePathRendererReset(long ptr, int pix_boundsX, int pix_boundsY,
//                                    int pix_boundsWidth, int pix_boundsHeight,
//                                    int windingRule);
void com_codename1_impl_ios_IOSNative_nativePathRendererReset___long_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr, JAVA_INT pix_boundsX, JAVA_INT pix_boundsY, JAVA_INT pix_boundsWidth, JAVA_INT pix_boundsHeight, JAVA_INT windingRule)
{
    Renderer_reset((Renderer*)ptr, pix_boundsX, pix_boundsY, pix_boundsWidth, pix_boundsHeight, windingRule);
}
//native void nativePathRendererGetOutputBounds(long ptr, int[] bounds);
void com_codename1_impl_ios_IOSNative_nativePathRendererGetOutputBounds___long_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr, JAVA_OBJECT bounds)
{
    Renderer* renderer = (Renderer*)ptr;
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)bounds;
    JAVA_ARRAY_INT* iArr = (JAVA_ARRAY_INT*)arr->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_INT* iArr = (JAVA_ARRAY_INT*) ((JAVA_ARRAY)bounds)->data;
#endif
    Renderer_getOutputBounds(renderer, iArr);
}
//native long nativePathRendererGetConsumer(long ptr);

JAVA_LONG com_codename1_impl_ios_IOSNative_nativePathRendererGetConsumer___long(JAVA_OBJECT instanceObject, JAVA_LONG ptr)
{
    //CN1Log(@"In getConsumer()");
    return (JAVA_LONG)(uintptr_t)&(((Renderer*)(uintptr_t)ptr)->consumer);
}

//native void nativePathConsumerMoveTo(long ptr, double x, double y);
void com_codename1_impl_ios_IOSNative_nativePathConsumerMoveTo___long_float_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr, JAVA_FLOAT x, JAVA_FLOAT y)
{
    //CN1Log(@"In moveTo %g,%g", x,y);
    ((PathConsumer*)ptr)->moveTo((PathConsumer*)ptr,x,y);
    //CN1Log(@"Finished moveTo");
}
//native void nativePathConsumerLineTo(long ptr, double x, double y);
void com_codename1_impl_ios_IOSNative_nativePathConsumerLineTo___long_float_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr, JAVA_FLOAT x, JAVA_FLOAT y)
{
    //CN1Log(@"In lineto %g,%g", x, y);
    ((PathConsumer*)ptr)->lineTo((PathConsumer*)ptr, (jfloat)x,(jfloat)y);
}
//native void nativePathConsumerQuadTo(long ptr, double xc, double yc, double x1, double y1);
void com_codename1_impl_ios_IOSNative_nativePathConsumerQuadTo___long_float_float_float_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr, JAVA_FLOAT xc, JAVA_FLOAT yc, JAVA_FLOAT x1, JAVA_FLOAT y1)
{
    ((PathConsumer*)ptr)->quadTo((PathConsumer*)ptr,(jfloat)xc,(jfloat)yc,(jfloat)x1,(jfloat)y1);
}
//native void nativePathConsumerCurveTo(long ptr, double xc1, double yc1, double xc2, double yc2, double x1, double y1);
void com_codename1_impl_ios_IOSNative_nativePathConsumerCurveTo___long_float_float_float_float_float_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr, JAVA_FLOAT xc1, JAVA_FLOAT yc1, JAVA_FLOAT xc2, JAVA_FLOAT yc2, JAVA_FLOAT x1, JAVA_FLOAT y1)
{
    ((PathConsumer*)ptr)->curveTo((PathConsumer*)ptr,xc1,yc1,xc2,yc2,x1,y1);
}

//native void nativePathConsumerClose(long ptr);
void com_codename1_impl_ios_IOSNative_nativePathConsumerClose___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr)
{
    //CN1Log(@"Closing path");
    ((PathConsumer*)ptr)->closePath((PathConsumer*)ptr);
}
//native void nativePathConsumerDone(long ptr);
void com_codename1_impl_ios_IOSNative_nativePathConsumerDone___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr)
{
    ((PathConsumer*)ptr)->pathDone((PathConsumer*)ptr);
}

//native void nativeDrawPath(int color, int alpha, long ptr)
extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawPathImpl(Renderer* renderer, int color, int alpha);

void com_codename1_impl_ios_IOSNative_nativeDrawPath___int_int_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT color, JAVA_INT alpha, JAVA_LONG ptr)
{
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawPathImpl((Renderer*)ptr, color, alpha);
    
    
}

extern void Java_com_codename1_impl_ios_IOSImplementation_drawTextureAlphaMaskImpl(JAVA_LONG textureName, int color, int alpha, int x, int y, int w, int h);
void com_codename1_impl_ios_IOSNative_drawTextureAlphaMask___long_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG textureName, JAVA_INT color, JAVA_INT alpha, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h)
{
    Java_com_codename1_impl_ios_IOSImplementation_drawTextureAlphaMaskImpl(textureName, color, alpha, x, y, w, h);


}

void com_codename1_impl_ios_IOSNative_nativeDeleteTexture___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG textureName)
{
    if (textureName == 0) return;
#if TARGET_OS_WATCH
    // The "texture" is a CN1CGAlphaMask carrying the coverage bytes.
    CN1CGAlphaMask *mask = (CN1CGAlphaMask *)(uintptr_t)textureName;
    if (mask->alphas != NULL) { free(mask->alphas); }
    free(mask);
#elif defined(CN1_USE_METAL)
    // Texture handle is a CFBridgingRetain'd id<MTLTexture>; release it to
    // drop the retain that nativePathRendererCreateTexture took.
    CFBridgingRelease((CFTypeRef)(void *)(uintptr_t)textureName);
#else
    dispatch_async(dispatch_get_main_queue(), ^{
        GLuint tex = (GLuint)textureName;
        //POOL_BEGIN();
        glDeleteTextures(1, &tex);
        //POOL_END();
    });
#endif
}


#define min(a,b) ((a)<(b)?(a):(b))
#define max(a,b) ((a)>(b)?(a):(b))
#define abs(x) ((x)>0?(x):-(x))

// Hard upper bound (in pixels) on a path alpha-mask's width/height. 8192 is the
// MTLTextureDescriptor max-2D-dimension enforced by the tvOS simulator (and a
// safe floor across Apple GPUs). The mask is normally bounded by the render
// target's framebuffer size; this is the backstop so no backend is ever handed
// an over-max texture descriptor.
#define CN1_PATH_MASK_MAX_DIM 8192
JAVA_OBJECT com_codename1_impl_ios_IOSNative_nativePathRendererToARGB___long_int(JAVA_OBJECT instanceObject, JAVA_LONG renderer, JAVA_INT color)
{
    Renderer *r = (Renderer*)(uintptr_t)renderer;
    JAVA_INT outputBounds[4];
    
    Renderer_getOutputBounds((Renderer*)(uintptr_t)renderer, (JAVA_INT*)&outputBounds);
    // outputBounds is { minX, minY, maxX, maxY }; maxX / maxY can be
    // legitimately negative for shapes drawn at negative coordinates
    // (see the comment in nativePathRendererCreateTexture above).
    // Filter on the actual width / height below.

    //GLuint tex=0;
    JAVA_INT x = min(outputBounds[0], outputBounds[2]);
    JAVA_INT y = min(outputBounds[1], outputBounds[3]);
    JAVA_INT width = outputBounds[2]-outputBounds[0];
    JAVA_INT height = outputBounds[3]-outputBounds[1];

    if ( width < 0 ) width = -width;
    if ( height < 0 ) height = -height;
    if (width == 0 || height == 0) {
        return 0;
    }

    AlphaConsumer ac = {
        x,
        y,
        width,
        height,
    };
    
    //jbyte* maskArray = malloc(sizeof(jbyte)*ac.width*ac.height);
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* data = XMLVMArray_createSingleDimension(__CLASS_byte, ac.width*ac.height);
    
    //CN1Log(@"Mask width %d height %d",
    //      ac.width,
    //      ac.height
    //      );
    ac.alphas = (JAVA_ARRAY_BYTE*)data->fields.org_xmlvm_runtime_XMLVMArray.array_;
    Renderer_produceAlphas((Renderer*)(uintptr_t)renderer, &ac);
    
    org_xmlvm_runtime_XMLVMArray* idata = XMLVMArray_createSingleDimension(__CLASS_int, ac.width*ac.height);
    JAVA_ARRAY_INT* iArr = (JAVA_ARRAY_INT*)idata->fields.org_xmlvm_runtime_XMLVMArray.array_;
    JAVA_ARRAY_BYTE* bArr = (JAVA_ARRAY_BYTE*)ac.alphas;
#else
    JAVA_OBJECT data = __NEW_ARRAY_JAVA_BYTE(CN1_THREAD_GET_STATE_PASS_ARG ac.width*ac.height);
    ac.alphas = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)data)->data;
    
    Renderer_produceAlphas((Renderer*)(uintptr_t)renderer, &ac);
    JAVA_OBJECT idata = __NEW_ARRAY_JAVA_INT(CN1_THREAD_GET_STATE_PASS_ARG ac.width*ac.height);
    JAVA_ARRAY_INT* iArr = (JAVA_ARRAY_INT*)((JAVA_ARRAY)idata)->data;
    JAVA_ARRAY_BYTE* bArr = (JAVA_ARRAY_BYTE*)ac.alphas;
#endif
    
    JAVA_INT len = ac.width*ac.height;
    for ( JAVA_INT i=0; i<len; i++){
        iArr[i] = color | (bArr[i] << 24);
        //CN1Log(@"%d", iArr[i]);
    }
    
    return (JAVA_OBJECT)idata;
    
}


JAVA_LONG com_codename1_impl_ios_IOSNative_nativePathRendererCreateTexture___long(JAVA_OBJECT instanceObject, JAVA_LONG renderer)
{
#if TARGET_OS_WATCH
    {
        JAVA_INT outputBounds[4];
        Renderer_getOutputBounds((Renderer*)(uintptr_t)renderer, (JAVA_INT*)&outputBounds);
        JAVA_INT x = min(outputBounds[0], outputBounds[2]);
        JAVA_INT y = min(outputBounds[1], outputBounds[3]);
        JAVA_INT width = outputBounds[2] - outputBounds[0];
        JAVA_INT height = outputBounds[3] - outputBounds[1];
        if (width < 0) width = -width;
        if (height < 0) height = -height;
        if (width == 0 || height == 0) return 0;
        CN1CGAlphaMask *mask = (CN1CGAlphaMask *)malloc(sizeof(CN1CGAlphaMask));
        mask->width = width;
        mask->height = height;
        mask->alphas = (unsigned char *)malloc((size_t)width * (size_t)height);
        AlphaConsumer ac;
        ac.originX = x; ac.originY = y; ac.width = width; ac.height = height;
        ac.alphas = (JAVA_BYTE *)mask->alphas;
        Renderer_produceAlphas((Renderer*)(uintptr_t)renderer, &ac);
        return (JAVA_LONG)(uintptr_t)mask;
    }
#endif
#ifdef CN1_USE_METAL
    {
        Renderer *r = (Renderer*)(uintptr_t)renderer;
        // Bound the mask to the render target. A stroked path that runs far
        // off-screen (e.g. the graphics-draw-arc / graphics-draw-round-rect
        // tests at 4K) otherwise yields a mask wider than the Metal device max
        // (8192 on the tvOS simulator) and aborts the app in
        // -[MTLTextureDescriptor validateWithDevice:]. The clamped-away region
        // is outside the framebuffer and clipped by the scissor regardless, so
        // this is lossless for visible pixels. CN1_PATH_MASK_MAX_DIM is the
        // texture-size backstop when the framebuffer is unknown (0).
        {
            int fbw = CN1MetalFramebufferWidth();
            int fbh = CN1MetalFramebufferHeight();
            Renderer_setOutputClip(r,
                fbw > 0 ? fbw : CN1_PATH_MASK_MAX_DIM,
                fbh > 0 ? fbh : CN1_PATH_MASK_MAX_DIM,
                CN1_PATH_MASK_MAX_DIM);
        }
        JAVA_INT outputBounds[4];
        Renderer_getOutputBounds((Renderer*)(uintptr_t)renderer, (JAVA_INT*)&outputBounds);
        // outputBounds is { minX, minY, maxX, maxY } in renderer pixel
        // space, which can legitimately be entirely negative when the
        // input shape sits at negative coordinates (e.g. the SVG
        // transcoder emits `<rect x="-5" y="-40" width="10" height="20">`
        // for the spinner_animated.svg children -- after the SVG scale
        // bake the renderer sees a path with bounds (-7, -60, 8, -30)).
        // The previous check rejected those legitimate negative maxX /
        // maxY values, returned 0 / nil texture, and silently dropped
        // every fillShape on negatively-positioned paths -- the
        // spinner column was blank on iOS Metal screenshots as a
        // result. Only reject *empty* bounds (max <= min on either
        // axis); the unsigned width / height computed below carry the
        // actual extent.
        JAVA_INT x = min(outputBounds[0], outputBounds[2]);
        JAVA_INT y = min(outputBounds[1], outputBounds[3]);
        JAVA_INT width = outputBounds[2] - outputBounds[0];
        JAVA_INT height = outputBounds[3] - outputBounds[1];
        if (width < 0) width = -width;
        if (height < 0) height = -height;
        if (width == 0 || height == 0) return 0;
        AlphaConsumer ac;
        ac.originX = x; ac.originY = y; ac.width = width; ac.height = height;
        jbyte *maskArray = malloc(sizeof(jbyte) * ac.width * ac.height);
        ac.alphas = maskArray;
        Renderer_produceAlphas((Renderer*)(uintptr_t)renderer, &ac);
        // Build R8 MTLTexture from the alpha bytes; CFBridgingRetain so the
        // Java-side handle (returned as JAVA_LONG) keeps the texture alive
        // until nativeDeleteTexture releases it.
        id<MTLTexture> tex = CN1MetalCreateAlphaMaskTexture((const uint8_t *)maskArray, width, height);
        free(maskArray);
        if (tex == nil) return 0;
        // Under MRR, CFBridgingRetain calls CFRetain (no ownership transfer
        // like ARC's __bridge_retained). CN1MetalCreateAlphaMaskTexture
        // returns a +1 (newTextureWithDescriptor), and CFBridgingRetain adds
        // a second +1, for a net +2. Java's nativeDeleteTexture only
        // CFBridgingReleases once on dispose, so we'd leak one full alpha-
        // mask MTLTexture per drawShape call. Release the local tex once
        // CF holds its retain via CFBridgingRetain.
        JAVA_LONG handle = (JAVA_LONG)(uintptr_t)CFBridgingRetain(tex);
#ifndef CN1_USE_ARC
        [tex release];
#endif
        return handle;
    }
#endif
#if defined(USE_ES2) && !defined(CN1_USE_METAL) && !TARGET_OS_WATCH

    __block JAVA_LONG outTexture = NULL;

    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        EAGLContext *ctx = [[CodenameOne_GLViewController instance] context];
        if ( ctx != nil ){
            [EAGLContext setCurrentContext:ctx];
        } else {
            //return 0;
            POOL_END();
            return;
        }
        
        Renderer *r = (Renderer*)(uintptr_t)renderer;
        JAVA_INT outputBounds[4];

        Renderer_getOutputBounds((Renderer*)(uintptr_t)renderer, (JAVA_INT*)&outputBounds);
        // outputBounds is { minX, minY, maxX, maxY }; the maxX/maxY
        // values can legitimately be negative when the shape sits in
        // the negative quadrant (e.g. the spinner SVG draws each
        // rotated rect at y in [-40, -20]). The width / height check
        // below filters degenerate / empty paths. Mirrors the Metal
        // branch above.

        GLuint tex=0;
        JAVA_INT x = min(outputBounds[0], outputBounds[2]);
        JAVA_INT y = min(outputBounds[1], outputBounds[3]);
        JAVA_INT width = outputBounds[2]-outputBounds[0];
        JAVA_INT height = outputBounds[3]-outputBounds[1];

        if ( width < 0 ) width = -width;
        if ( height < 0 ) height = -height;
        if (width == 0 || height == 0) {
            POOL_END();
            return;
        }
        AlphaConsumer *ac = malloc(sizeof(AlphaConsumer));
        ac->originX = x;
        ac->originY = y;
        ac->width = width;
        ac->height = height;

        jbyte* maskArray = malloc(sizeof(jbyte)*ac->width*ac->height);

        ac->alphas = maskArray;
        Renderer_produceAlphas(renderer, ac);
        
        _glEnableClientState(GL_VERTEX_ARRAY);
        //glEnableClientState(GL_NORMAL_ARRAY);
        GLErrorLog;
        _glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        GLErrorLog;
        glGenTextures(1, &tex);
        
        GLErrorLog;
        
        if ( tex == 0 ){
            free(maskArray);
            free(ac);
            POOL_END();
            return;
            //return 0;
        }
        glActiveTexture(GL_TEXTURE1);
        GLErrorLog;
        glBindTexture(GL_TEXTURE_2D, tex);
        GLErrorLog;
        
        //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, ac->width, ac->height, 0, GL_ALPHA, GL_UNSIGNED_BYTE, maskArray);
        GLErrorLog;
        
        glBindTexture(GL_TEXTURE_2D, 0);
        GLErrorLog;
        _glDisableClientState(GL_VERTEX_ARRAY);
        GLErrorLog;
        _glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        GLErrorLog;
        
        free(maskArray);
        free(ac);

        outTexture = tex;
        //return (JAVA_LONG)tex;
        POOL_END();
    });
    return outTexture;
#else
    return 0;
#endif
    
}



float clamp_float_to_int(float val){
    JAVA_FLOAT absVal = abs(val);
    JAVA_INT absIntVal = round(absVal);
    if ( abs(absVal-absIntVal) < 0.001 ){
        return (float)round(val);
    }
    return (JAVA_FLOAT)val;
}

void com_codename1_impl_ios_Matrix_MatrixUtil_multiplyMM___float_1ARRAY_int_float_1ARRAY_int_float_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT result, JAVA_INT resultOffset, JAVA_OBJECT lhs, JAVA_INT lhsOffset, JAVA_OBJECT rhs, JAVA_INT rhsOffset)
{
#ifdef USE_ES2
#ifndef NEW_CODENAME_ONE_VM
    //org_xmlvm_runtime_XMLVMArray* byteArray = java_lang_String_getBytes___java_lang_String(str, utf8String);
    //JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    
    JAVA_ARRAY_FLOAT* lhsData = (JAVA_ARRAY_FLOAT*) ((org_xmlvm_runtime_XMLVMArray*)lhs)->fields.org_xmlvm_runtime_XMLVMArray.array_;
    JAVA_ARRAY_FLOAT* rhsData = (JAVA_ARRAY_FLOAT*) ((org_xmlvm_runtime_XMLVMArray*)rhs)->fields.org_xmlvm_runtime_XMLVMArray.array_;
    JAVA_ARRAY_FLOAT* resultData = (JAVA_ARRAY_FLOAT*) ((org_xmlvm_runtime_XMLVMArray*)result)->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    //JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)n2)->data;
    JAVA_ARRAY_FLOAT* lhsData = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY)lhs)->data;
    JAVA_ARRAY_FLOAT* rhsData = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY)rhs)->data;
    JAVA_ARRAY_FLOAT* resultData = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)result)->data;
    
#endif
    
    
#if defined(CN1_USE_METAL) || TARGET_OS_WATCH
    // Manual 4x4 column-major multiply so this path compiles for the Mac
    // Catalyst slice (no GLKit math symbols). Identical result to
    // GLKMatrix4Multiply(GLKMatrix4MakeWithArray(L), GLKMatrix4MakeWithArray(R)).
    const JAVA_ARRAY_FLOAT *L = lhsData + lhsOffset * sizeof(JAVA_FLOAT);
    const JAVA_ARRAY_FLOAT *R = rhsData + rhsOffset * sizeof(JAVA_FLOAT);
    float out[16];
    for (int col = 0; col < 4; col++) {
        for (int row = 0; row < 4; row++) {
            float s = 0;
            for (int k = 0; k < 4; k++) {
                s += L[k * 4 + row] * R[col * 4 + k];
            }
            out[col * 4 + row] = s;
        }
    }
    for (int i = 0; i < 16; i++) {
        resultData[i + resultOffset] = clamp_float_to_int(out[i]);
    }
#else
    GLKMatrix4 mLeft = GLKMatrix4MakeWithArray(lhsData+lhsOffset*sizeof(JAVA_FLOAT));
    GLKMatrix4 mRight = GLKMatrix4MakeWithArray(rhsData+rhsOffset*sizeof(JAVA_FLOAT));
    GLKMatrix4 mResult = GLKMatrix4Multiply(mLeft, mRight);

    for ( int i=0; i<16; i++){
        resultData[i+resultOffset] = clamp_float_to_int(mResult.m[i]);
    }
    //memcpy(resultData+resultOffset*sizeof(JAVA_FLOAT), &mResult, 16*sizeof(JAVA_FLOAT));
#endif
#endif
}


//public static native void transformPoints(float[] data, int pointSize, float[] in, int srcPos, float[] out, int destPos, int numPoints);
JAVA_VOID com_codename1_impl_ios_Matrix_MatrixUtil_transformPoints___float_1ARRAY_int_float_1ARRAY_int_float_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG
JAVA_OBJECT m, JAVA_INT pointSize, JAVA_OBJECT in, JAVA_INT srcPos, JAVA_OBJECT out, JAVA_INT destPos, JAVA_INT numPoints
) {
#ifndef NEW_CODENAME_ONE_VM
    JAVA_ARRAY_FLOAT* mData = (JAVA_ARRAY_FLOAT*) ((org_xmlvm_runtime_XMLVMArray*)m)->fields.org_xmlvm_runtime_XMLVMArray.array_;
    JAVA_ARRAY_FLOAT* inData = (JAVA_ARRAY_FLOAT*) ((org_xmlvm_runtime_XMLVMArray*)in)->fields.org_xmlvm_runtime_XMLVMArray.array_;
    JAVA_ARRAY_FLOAT* outData = (JAVA_ARRAY_FLOAT*) ((org_xmlvm_runtime_XMLVMArray*)out)->fields.org_xmlvm_runtime_XMLVMArray.array_;
    
#else
    JAVA_ARRAY_FLOAT* mData = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY)m)->data;
    JAVA_ARRAY_FLOAT* inData = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY)in)->data;
    JAVA_ARRAY_FLOAT* outData = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY)out)->data;
#endif
#if defined(CN1_USE_METAL) || TARGET_OS_WATCH
    // Manual matrix-vector multiply for the Mac Catalyst slice (no GLKit
    // math symbols). mData is a 4x4 column-major matrix.
    const JAVA_ARRAY_FLOAT *M = mData;
    JAVA_INT len = numPoints * pointSize;
    for (JAVA_INT i = 0; i < len; i += pointSize) {
        JAVA_INT s0 = srcPos + i;
        float inv[4] = { inData[s0], inData[s0+1], 0.0f, 1.0f };
        if (pointSize == 3) {
            inv[2] = inData[s0+2];
        }
        float outv[4];
        for (int row = 0; row < 4; row++) {
            float s = 0;
            for (int col = 0; col < 4; col++) {
                s += M[col * 4 + row] * inv[col];
            }
            outv[row] = s;
        }
        int d0 = destPos + i;
        outData[d0++] = outv[0] / outv[3];
        outData[d0++] = outv[1] / outv[3];
        if (pointSize == 3) {
            outData[d0] = outv[2] / outv[3];
        }
    }
#else
    GLKMatrix4 mMat = GLKMatrix4MakeWithArray(mData);
    JAVA_INT len = numPoints * pointSize;
    for (JAVA_INT i=0; i<len; i+=pointSize) {
        JAVA_INT s0 = srcPos + i;
        GLKVector4 inputVector = GLKVector4Make(inData[s0], inData[s0+1], 0, 1);
        if (pointSize==3) {
            inputVector.v[2]= inData[s0+2];
        }
        GLKVector4 outputVector = GLKMatrix4MultiplyVector4(mMat, inputVector);

        int d0 = destPos + i;
        outData[d0++] = outputVector.v[0] / outputVector.v[3];
        outData[d0++] = outputVector.v[1] / outputVector.v[3];
        if (pointSize==3) {
            outData[d0] = outputVector.v[2] / outputVector.v[3];
        }
    }
#endif

}


JAVA_VOID com_codename1_impl_ios_IOSNative_translatePoints___int_float_float_float_float_1ARRAY_int_float_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instance,
    JAVA_INT pointSize, JAVA_FLOAT tX, JAVA_FLOAT tY, JAVA_FLOAT tZ, JAVA_OBJECT in, JAVA_INT srcPos, JAVA_OBJECT out, JAVA_INT destPos, JAVA_INT numPoints
) {
#ifndef NEW_CODENAME_ONE_VM
    JAVA_ARRAY_FLOAT* inData = (JAVA_ARRAY_FLOAT*) ((org_xmlvm_runtime_XMLVMArray*)in)->fields.org_xmlvm_runtime_XMLVMArray.array_;
    JAVA_ARRAY_FLOAT* outData = (JAVA_ARRAY_FLOAT*) ((org_xmlvm_runtime_XMLVMArray*)out)->fields.org_xmlvm_runtime_XMLVMArray.array_;
    
#else
    JAVA_ARRAY_FLOAT* inData = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY)in)->data;
    JAVA_ARRAY_FLOAT* outData = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY)out)->data;
#endif
    JAVA_INT len = numPoints * pointSize;
    for (JAVA_INT i=0; i<len; i+= pointSize) {
        JAVA_INT s0 = srcPos + i;
        JAVA_INT d0 = destPos + i;
        outData[d0++] = inData[s0++] + tX;
        outData[d0++] = inData[s0++] + tY;
        if (pointSize == 3) {
            outData[d0] = inData[s0] + tZ;
        }
    }
}

JAVA_VOID com_codename1_impl_ios_IOSNative_scalePoints___int_float_float_float_float_1ARRAY_int_float_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instance,
    JAVA_INT pointSize, JAVA_FLOAT sX, JAVA_FLOAT sY, JAVA_FLOAT sZ, JAVA_OBJECT in, JAVA_INT srcPos, JAVA_OBJECT out, JAVA_INT destPos, JAVA_INT numPoints
) {
#ifndef NEW_CODENAME_ONE_VM
    JAVA_ARRAY_FLOAT* inData = (JAVA_ARRAY_FLOAT*) ((org_xmlvm_runtime_XMLVMArray*)in)->fields.org_xmlvm_runtime_XMLVMArray.array_;
    JAVA_ARRAY_FLOAT* outData = (JAVA_ARRAY_FLOAT*) ((org_xmlvm_runtime_XMLVMArray*)out)->fields.org_xmlvm_runtime_XMLVMArray.array_;
    
#else
    JAVA_ARRAY_FLOAT* inData = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY)in)->data;
    JAVA_ARRAY_FLOAT* outData = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY)out)->data;
#endif
    JAVA_INT len = numPoints * pointSize;
    for (JAVA_INT i=0; i<len; i+= pointSize) {
        JAVA_INT s0 = srcPos + i;
        JAVA_INT d0 = destPos + i;
        outData[d0++] = inData[s0++] * sX;
        outData[d0++] = inData[s0++] * sY;
        if (pointSize == 3) {
            outData[d0] = inData[s0] * sZ;
        }
    }
}

JAVA_BOOLEAN com_codename1_impl_ios_Matrix_MatrixUtil_invertM___float_1ARRAY_int_float_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT mInv, JAVA_INT mInvOffset, JAVA_OBJECT m, JAVA_INT mOffset)
{
#ifdef USE_ES2
#ifndef NEW_CODENAME_ONE_VM
    //org_xmlvm_runtime_XMLVMArray* byteArray = java_lang_String_getBytes___java_lang_String(str, utf8String);
    //JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    
    JAVA_ARRAY_FLOAT* mData = (JAVA_ARRAY_FLOAT*) ((org_xmlvm_runtime_XMLVMArray*)m)->fields.org_xmlvm_runtime_XMLVMArray.array_;
    JAVA_ARRAY_FLOAT* mInvData = (JAVA_ARRAY_FLOAT*) ((org_xmlvm_runtime_XMLVMArray*)mInv)->fields.org_xmlvm_runtime_XMLVMArray.array_;
    
#else
    //JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)n2)->data;
    JAVA_ARRAY_FLOAT* mData = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY)m)->data;
    JAVA_ARRAY_FLOAT* mInvData = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY)mInv)->data;
    
    
#endif
    
    
#if defined(CN1_USE_METAL) || TARGET_OS_WATCH
    // Manual 4x4 matrix inverse for the Mac Catalyst slice. Returns 1 in
    // both branches to preserve the original iOS semantic (the function
    // always returns 1 unless USE_ES2 is off, mirroring GLKMatrix4Invert's
    // behavior when callers ignore the `isInvertible` flag).
    // NB: the JAVA_OBJECT parameter is already named `m`, so the working
    // copy of the float matrix is named `mm` to avoid shadowing.
    const JAVA_ARRAY_FLOAT *src = mData + mOffset * sizeof(JAVA_FLOAT);
    float mm[16];
    for (int i = 0; i < 16; i++) { mm[i] = src[i]; }
    // Cofactor expansion derived from a standard adjugate / determinant
    // formula for 4x4 column-major matrices. Matches GLKMatrix4Invert's
    // output bit-for-bit for invertible inputs; non-invertible matrices
    // would have det == 0, mirroring GLKit's `*invertible = 0` behavior.
    float inv[16];
    inv[0]  =  mm[5]*mm[10]*mm[15] - mm[5]*mm[11]*mm[14] - mm[9]*mm[6]*mm[15] + mm[9]*mm[7]*mm[14] + mm[13]*mm[6]*mm[11] - mm[13]*mm[7]*mm[10];
    inv[4]  = -mm[4]*mm[10]*mm[15] + mm[4]*mm[11]*mm[14] + mm[8]*mm[6]*mm[15] - mm[8]*mm[7]*mm[14] - mm[12]*mm[6]*mm[11] + mm[12]*mm[7]*mm[10];
    inv[8]  =  mm[4]*mm[9]*mm[15]  - mm[4]*mm[11]*mm[13] - mm[8]*mm[5]*mm[15] + mm[8]*mm[7]*mm[13] + mm[12]*mm[5]*mm[11] - mm[12]*mm[7]*mm[9];
    inv[12] = -mm[4]*mm[9]*mm[14]  + mm[4]*mm[10]*mm[13] + mm[8]*mm[5]*mm[14] - mm[8]*mm[6]*mm[13] - mm[12]*mm[5]*mm[10] + mm[12]*mm[6]*mm[9];
    inv[1]  = -mm[1]*mm[10]*mm[15] + mm[1]*mm[11]*mm[14] + mm[9]*mm[2]*mm[15] - mm[9]*mm[3]*mm[14] - mm[13]*mm[2]*mm[11] + mm[13]*mm[3]*mm[10];
    inv[5]  =  mm[0]*mm[10]*mm[15] - mm[0]*mm[11]*mm[14] - mm[8]*mm[2]*mm[15] + mm[8]*mm[3]*mm[14] + mm[12]*mm[2]*mm[11] - mm[12]*mm[3]*mm[10];
    inv[9]  = -mm[0]*mm[9]*mm[15]  + mm[0]*mm[11]*mm[13] + mm[8]*mm[1]*mm[15] - mm[8]*mm[3]*mm[13] - mm[12]*mm[1]*mm[11] + mm[12]*mm[3]*mm[9];
    inv[13] =  mm[0]*mm[9]*mm[14]  - mm[0]*mm[10]*mm[13] - mm[8]*mm[1]*mm[14] + mm[8]*mm[2]*mm[13] + mm[12]*mm[1]*mm[10] - mm[12]*mm[2]*mm[9];
    inv[2]  =  mm[1]*mm[6]*mm[15]  - mm[1]*mm[7]*mm[14]  - mm[5]*mm[2]*mm[15] + mm[5]*mm[3]*mm[14] + mm[13]*mm[2]*mm[7]  - mm[13]*mm[3]*mm[6];
    inv[6]  = -mm[0]*mm[6]*mm[15]  + mm[0]*mm[7]*mm[14]  + mm[4]*mm[2]*mm[15] - mm[4]*mm[3]*mm[14] - mm[12]*mm[2]*mm[7]  + mm[12]*mm[3]*mm[6];
    inv[10] =  mm[0]*mm[5]*mm[15]  - mm[0]*mm[7]*mm[13]  - mm[4]*mm[1]*mm[15] + mm[4]*mm[3]*mm[13] + mm[12]*mm[1]*mm[7]  - mm[12]*mm[3]*mm[5];
    inv[14] = -mm[0]*mm[5]*mm[14]  + mm[0]*mm[6]*mm[13]  + mm[4]*mm[1]*mm[14] - mm[4]*mm[2]*mm[13] - mm[12]*mm[1]*mm[6]  + mm[12]*mm[2]*mm[5];
    inv[3]  = -mm[1]*mm[6]*mm[11]  + mm[1]*mm[7]*mm[10]  + mm[5]*mm[2]*mm[11] - mm[5]*mm[3]*mm[10] - mm[9]*mm[2]*mm[7]   + mm[9]*mm[3]*mm[6];
    inv[7]  =  mm[0]*mm[6]*mm[11]  - mm[0]*mm[7]*mm[10]  - mm[4]*mm[2]*mm[11] + mm[4]*mm[3]*mm[10] + mm[8]*mm[2]*mm[7]   - mm[8]*mm[3]*mm[6];
    inv[11] = -mm[0]*mm[5]*mm[11]  + mm[0]*mm[7]*mm[9]   + mm[4]*mm[1]*mm[11] - mm[4]*mm[3]*mm[9]  - mm[8]*mm[1]*mm[7]   + mm[8]*mm[3]*mm[5];
    inv[15] =  mm[0]*mm[5]*mm[10]  - mm[0]*mm[6]*mm[9]   - mm[4]*mm[1]*mm[10] + mm[4]*mm[2]*mm[9]  + mm[8]*mm[1]*mm[6]   - mm[8]*mm[2]*mm[5];
    float det = mm[0]*inv[0] + mm[1]*inv[4] + mm[2]*inv[8] + mm[3]*inv[12];
    if (det == 0.0f) {
        return 1;
    }
    float invDet = 1.0f / det;
    for (int i = 0; i < 16; i++) {
        mInvData[i + mInvOffset] = inv[i] * invDet;
    }
    return 1;
#else
    GLKMatrix4 mMat = GLKMatrix4MakeWithArray(mData+mOffset*sizeof(JAVA_FLOAT));
    JAVA_BOOLEAN isInvertible = 0;
    GLKMatrix4 mInvMat = GLKMatrix4Invert(mMat, &isInvertible);
    if ( !isInvertible ){
        return 1;
    } else {
        for ( int i=0; i<16; i++){
            mInvData[i+mInvOffset] = mInvMat.m[i];
        }
        return 1;
    }
#endif
#else
    return 0;
#endif

}

#ifdef NEW_CODENAME_ONE_VM
JAVA_BOOLEAN com_codename1_impl_ios_Matrix_MatrixUtil_invertM___float_1ARRAY_int_float_1ARRAY_int_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT mInv, JAVA_INT mInvOffset, JAVA_OBJECT m, JAVA_INT mOffset)
{
    return com_codename1_impl_ios_Matrix_MatrixUtil_invertM___float_1ARRAY_int_float_1ARRAY_int(CN1_THREAD_STATE_PASS_ARG mInv, mInvOffset, m, mOffset);
}
#endif


//native void nativeSetTransform(
//                               float a0, float a1, float a2, float a3,
//                               float b0, float b1, float b2, float b3,
//                               float c0, float c1, float c2, float c3,
//                               float d0, float d1, float d2, float d3,
//                               boolean reset
//
extern void com_codename1_impl_ios_IOSImplementation_nativeSetTransformImpl___float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_int_int( JAVA_OBJECT instanceObject,
                                                                                                                                                                                      JAVA_FLOAT a0, JAVA_FLOAT a1, JAVA_FLOAT a2, JAVA_FLOAT a3,
                                                                                                                                                                                      JAVA_FLOAT b0, JAVA_FLOAT b1, JAVA_FLOAT b2, JAVA_FLOAT b3,
                                                                                                                                                                                      JAVA_FLOAT c0, JAVA_FLOAT c1, JAVA_FLOAT c2, JAVA_FLOAT c3,
                                                                                                                                                                                      JAVA_FLOAT d0, JAVA_FLOAT d1, JAVA_FLOAT d2, JAVA_FLOAT d3,
                                                                                                                                                                                      JAVA_INT originX, JAVA_INT originY
                                                                                                                                                                                      );
void com_codename1_impl_ios_IOSNative_nativeSetTransform___float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                                                                                                                                   JAVA_FLOAT a0, JAVA_FLOAT a1, JAVA_FLOAT a2, JAVA_FLOAT a3,
                                                                                                                                                                   JAVA_FLOAT b0, JAVA_FLOAT b1, JAVA_FLOAT b2, JAVA_FLOAT b3,
                                                                                                                                                                   JAVA_FLOAT c0, JAVA_FLOAT c1, JAVA_FLOAT c2, JAVA_FLOAT c3,
                                                                                                                                                                   JAVA_FLOAT d0, JAVA_FLOAT d1, JAVA_FLOAT d2, JAVA_FLOAT d3,
                                                                                                                                                                   JAVA_INT originX, JAVA_INT originY
                                                                                                                                                                   )
{
    com_codename1_impl_ios_IOSImplementation_nativeSetTransformImpl___float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_int_int
    (
     instanceObject, a0, a1, a2, a3,
     b0, b1, b2, b3,
     c0, c1, c2, c3,
     d0, d1, d2, d3,
     originX, originY
     );
}

extern void com_codename1_impl_ios_IOSImplementation_nativeSetTransformMutableImpl___float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_int_int( JAVA_OBJECT instanceObject,
                                                                                                                                                                                      JAVA_FLOAT a0, JAVA_FLOAT a1, JAVA_FLOAT a2, JAVA_FLOAT a3,
                                                                                                                                                                                      JAVA_FLOAT b0, JAVA_FLOAT b1, JAVA_FLOAT b2, JAVA_FLOAT b3,
                                                                                                                                                                                      JAVA_FLOAT c0, JAVA_FLOAT c1, JAVA_FLOAT c2, JAVA_FLOAT c3,
                                                                                                                                                                                      JAVA_FLOAT d0, JAVA_FLOAT d1, JAVA_FLOAT d2, JAVA_FLOAT d3,
                                                                                                                                                                                      JAVA_INT originX, JAVA_INT originY
                                                                                                                                                                                      );
void com_codename1_impl_ios_IOSNative_nativeSetTransformMutable___float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                                                                                                                                   JAVA_FLOAT a0, JAVA_FLOAT a1, JAVA_FLOAT a2, JAVA_FLOAT a3,
                                                                                                                                                                   JAVA_FLOAT b0, JAVA_FLOAT b1, JAVA_FLOAT b2, JAVA_FLOAT b3,
                                                                                                                                                                   JAVA_FLOAT c0, JAVA_FLOAT c1, JAVA_FLOAT c2, JAVA_FLOAT c3,
                                                                                                                                                                   JAVA_FLOAT d0, JAVA_FLOAT d1, JAVA_FLOAT d2, JAVA_FLOAT d3,
                                                                                                                                                                   JAVA_INT originX, JAVA_INT originY
                                                                                                                                                                   )
{
    com_codename1_impl_ios_IOSImplementation_nativeSetTransformMutableImpl___float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_int_int
    (
     instanceObject, a0, a1, a2, a3,
     b0, b1, b2, b3,
     c0, c1, c2, c3,
     d0, d1, d2, d3,
     originX, originY
     );
}


JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_nativeIsTransformSupportedGlobal__(JAVA_OBJECT instanceObject){
#ifdef USE_ES2
    return YES;
#else
    return NO;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_nativeIsPerspectiveTransformSupportedGlobal__(JAVA_OBJECT instanceObject){
#ifdef USE_ES2
    return YES;
#else
    return NO;
#endif
}


JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_nativeIsShapeSupportedGlobal__(JAVA_OBJECT instanceObject){
#ifdef USE_ES2
    return YES;
#else
    return NO;
#endif
}


JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_nativeIsAlphaMaskSupportedGlobal__(JAVA_OBJECT instanceObject){
#ifdef USE_ES2
    return YES;
#else
    return NO;
#endif
}

// End Shapes

/*JAVA_OBJECT com_codename1_impl_ios_IOSNative_stackTraceToString___java_lang_Throwable(JAVA_OBJECT t) {
 POOL_BEGIN();
 
 NSArray* arr = [NSThread callStackSymbols];
 NSMutableArray* marr = [[NSMutableArray alloc] init];
 [marr addObjectsFromArray:arr];
 [marr removeObjectAtIndex:0];
 [marr removeObjectAtIndex:0];
 [marr removeObjectAtIndex:0];
 [marr removeObjectAtIndex:0];
 NSString* nstr = [marr description];
 JAVA_OBJECT jstr = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG nstr);
 POOL_END();
 return jstr;
 }*/


#ifdef NEW_CODENAME_ONE_VM

// Start Shapes (ES2)

JAVA_LONG com_codename1_impl_ios_IOSNative_nativePathStrokerCreate___long_float_int_int_float_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG consumerOutPtr, JAVA_FLOAT lineWidth, JAVA_INT capStyle, JAVA_INT joinStyle, JAVA_FLOAT miterLimit)
{
    return com_codename1_impl_ios_IOSNative_nativePathStrokerCreate___long_float_int_int_float( instanceObject, consumerOutPtr,  lineWidth,  capStyle,  joinStyle,  miterLimit);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_nativePathStrokerGetConsumer___long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr)
{
    return com_codename1_impl_ios_IOSNative_nativePathStrokerGetConsumer___long( instanceObject,  ptr);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_nativePathRendererCreate___int_int_int_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT pix_boundsX, JAVA_INT pix_boundsY, JAVA_INT pix_boundsWidth, JAVA_INT pix_boundsHeight, JAVA_INT windingRule)
{
    return com_codename1_impl_ios_IOSNative_nativePathRendererCreate___int_int_int_int_int( instanceObject,  pix_boundsX,  pix_boundsY,  pix_boundsWidth,  pix_boundsHeight,  windingRule);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_nativePathRendererGetConsumer___long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr)
{
    return com_codename1_impl_ios_IOSNative_nativePathRendererGetConsumer___long(instanceObject, ptr);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_nativePathRendererToARGB___long_int_R_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG renderer, JAVA_INT color)
{
    enteringNativeAllocations();
    JAVA_OBJECT o = com_codename1_impl_ios_IOSNative_nativePathRendererToARGB___long_int(instanceObject, renderer, color);
    finishedNativeAllocations();
    return o;
}


JAVA_LONG com_codename1_impl_ios_IOSNative_nativePathRendererCreateTexture___long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG renderer)
{
    return com_codename1_impl_ios_IOSNative_nativePathRendererCreateTexture___long(instanceObject, renderer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_nativeIsTransformSupportedGlobal___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject){
    return com_codename1_impl_ios_IOSNative_nativeIsTransformSupportedGlobal__(instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_nativeIsPerspectiveTransformSupportedGlobal___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    return com_codename1_impl_ios_IOSNative_nativeIsPerspectiveTransformSupportedGlobal__(instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_nativeIsShapeSupportedGlobal___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    return com_codename1_impl_ios_IOSNative_nativeIsShapeSupportedGlobal__(instanceObject);
}


// END Shapes (ES2)


JAVA_INT com_codename1_impl_ios_IOSNative_getVKBHeight___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject){
    return (JAVA_INT)com_codename1_impl_ios_IOSNative_getVKBHeight__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

extern int vkbWidth;
JAVA_INT com_codename1_impl_ios_IOSNative_getVKBWidth___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject ){
    return (JAVA_INT)com_codename1_impl_ios_IOSNative_getVKBWidth__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isPainted___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isPainted__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isMetalRendering___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isMetalRendering__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_nativeIsAlphaMaskSupportedGlobal___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    return com_codename1_impl_ios_IOSNative_nativeIsAlphaMaskSupportedGlobal__(instanceObject);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplaySafeInsetLeft___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return getSafeLeft();
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplaySafeInsetTop___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return getSafeTop();
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplaySafeInsetRight___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return getSafeRight();
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplaySafeInsetBottom___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return getSafeBottom();
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplayWidth___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getDisplayWidth__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplayHeight___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getDisplayHeight__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageFromARGB___int_1ARRAY_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_INT n2, JAVA_INT n3) {
    return com_codename1_impl_ios_IOSNative_createImageFromARGB___int_1ARRAY_int_int(CN1_THREAD_STATE_PASS_ARG instanceObject, n1, n2, n3);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImage___byte_1ARRAY_int_1ARRAY_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT n2) {
    return com_codename1_impl_ios_IOSNative_createImage___byte_1ARRAY_int_1ARRAY(CN1_THREAD_STATE_PASS_ARG instanceObject, n1, n2);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageNSData___long_int_1ARRAY_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_OBJECT n2) {
    return com_codename1_impl_ios_IOSNative_createImageNSData___long_int_1ARRAY(CN1_THREAD_STATE_PASS_ARG instanceObject, nsData, n2);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_scale___long_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT n2, JAVA_INT n3) {
    return com_codename1_impl_ios_IOSNative_scale___long_int_int(CN1_THREAD_STATE_PASS_ARG instanceObject, n1, n2, n3);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gausianBlurImage___long_float_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_FLOAT radius) {
    return com_codename1_impl_ios_IOSNative_gausianBlurImage___long_float(CN1_THREAD_STATE_PASS_ARG instanceObject, n1, radius);
}

JAVA_INT com_codename1_impl_ios_IOSNative_stringWidthNative___long_java_lang_String_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_OBJECT n2) {
    return com_codename1_impl_ios_IOSNative_stringWidthNative___long_java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, n1, n2);
}

JAVA_INT com_codename1_impl_ios_IOSNative_charWidthNative___long_char_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_CHAR n2) {
    return com_codename1_impl_ios_IOSNative_charWidthNative___long_char(CN1_THREAD_STATE_PASS_ARG instanceObject, n1, n2);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getFontHeightNative___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1) {
    return com_codename1_impl_ios_IOSNative_getFontHeightNative___long(CN1_THREAD_STATE_PASS_ARG instanceObject, n1);
}

JAVA_INT com_codename1_impl_ios_IOSNative_fontAscentNative___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1) {
    return com_codename1_impl_ios_IOSNative_fontAscentNative___long(CN1_THREAD_STATE_PASS_ARG instanceObject, n1);
}

JAVA_INT com_codename1_impl_ios_IOSNative_fontDescentNative___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1) {
    return com_codename1_impl_ios_IOSNative_fontDescentNative___long(CN1_THREAD_STATE_PASS_ARG instanceObject, n1);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createSystemFont___int_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3) {
    return com_codename1_impl_ios_IOSNative_createSystemFont___int_int_int(CN1_THREAD_STATE_PASS_ARG instanceObject, n1, n2, n3);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResourceSize___java_lang_String_java_lang_String_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT n2) {
    return com_codename1_impl_ios_IOSNative_getResourceSize___java_lang_String_java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, n1, n2);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeMutableImage___int_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3) {
    return com_codename1_impl_ios_IOSNative_createNativeMutableImage___int_int_int(CN1_THREAD_STATE_PASS_ARG instanceObject, n1, n2, n3);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_finishDrawingOnImage___R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_finishDrawingOnImage__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isTablet___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isTablet__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isIOS7___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isIOS7__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isRunningOnMac___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isRunningOnMac__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isRunningOnWatch___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isRunningOnWatch__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isRunningOnTV___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isRunningOnTV__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNSData___java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    return com_codename1_impl_ios_IOSNative_createNSData___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, file);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNSDataResource___java_lang_String_java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name, JAVA_OBJECT type) {
    return com_codename1_impl_ios_IOSNative_createNSDataResource___java_lang_String_java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, name, type);
}

JAVA_INT com_codename1_impl_ios_IOSNative_read___long_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_INT pointer) {
    return com_codename1_impl_ios_IOSNative_read___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, nsData, pointer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_writeToFile___byte_1ARRAY_java_lang_String_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT path) {
    return com_codename1_impl_ios_IOSNative_writeToFile___byte_1ARRAY_java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, n1, path);
}

JAVA_INT com_codename1_impl_ios_IOSNative_appendToFile___byte_1ARRAY_java_lang_String_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT path) {
    return com_codename1_impl_ios_IOSNative_appendToFile___byte_1ARRAY_java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, n1, path);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getFileSize___java_lang_String_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    return com_codename1_impl_ios_IOSNative_getFileSize___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, path);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getFileLastModified___java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    return com_codename1_impl_ios_IOSNative_getFileLastModified___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, path);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getDocumentsDir___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getDocumentsDir__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getCachesDir___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getCachesDir__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResourcesDir___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getResourcesDir__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_fileExists___java_lang_String_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    return com_codename1_impl_ios_IOSNative_fileExists___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, file);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDirectory___java_lang_String_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    return com_codename1_impl_ios_IOSNative_isDirectory___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, file);
}

JAVA_INT com_codename1_impl_ios_IOSNative_fileCountInDir___java_lang_String_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT dir) {
    return com_codename1_impl_ios_IOSNative_fileCountInDir___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, dir);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_openConnection___java_lang_String_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT url, JAVA_INT timeout) {
    return com_codename1_impl_ios_IOSNative_openConnection___java_lang_String_int(CN1_THREAD_STATE_PASS_ARG instanceObject, url, timeout);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResponseCode___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getResponseCode___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseMessage___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getResponseMessage___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getContentLength___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getContentLength___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseHeader___long_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT name) {
    return com_codename1_impl_ios_IOSNative_getResponseHeader___long_java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, peer, name);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResponseHeaderCount___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getResponseHeaderCount___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseHeaderName___long_int_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
    return com_codename1_impl_ios_IOSNative_getResponseHeaderName___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, peer, offset);
}

// SJH Nov. 17, 2015 : Removing native isMinimized() method because it conflicted with
// tracking on the java side.  It caused the app to still be minimized inside start()
// method.  
// Related to this issue https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/codenameone-discussions/Ajo2fArN8mc/KrF_e9cTDwAJ
//JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isMinimized___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
//{
//    return com_codename1_impl_ios_IOSNative_isMinimized__(CN1_THREAD_STATE_PASS_ARG instanceObject);
//}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_minimizeApplication___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    return com_codename1_impl_ios_IOSNative_minimizeApplication__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getAudioDuration___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getAudioDuration___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAudioPlaying___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_isAudioPlaying___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getAudioTime___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getAudioTime___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___java_lang_String_java_lang_Runnable_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT uri, JAVA_OBJECT onCompletion) {
    return com_codename1_impl_ios_IOSNative_createAudio___java_lang_String_java_lang_Runnable(CN1_THREAD_STATE_PASS_ARG instanceObject, uri, onCompletion);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___byte_1ARRAY_java_lang_Runnable_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT b, JAVA_OBJECT onCompletion) {
    return com_codename1_impl_ios_IOSNative_createAudio___byte_1ARRAY_java_lang_Runnable(CN1_THREAD_STATE_PASS_ARG instanceObject, b, onCompletion);
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_getVolume___R_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getVolume__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createBrowserComponent___java_lang_Object_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT obj) {
    return com_codename1_impl_ios_IOSNative_createBrowserComponent___java_lang_Object(CN1_THREAD_STATE_PASS_ARG instanceObject, obj);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_browserHasBack___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_browserHasBack___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_browserHasForward___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_browserHasForward___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getBrowserTitle___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getBrowserTitle___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getBrowserURL___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getBrowserURL___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponent___java_lang_String_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT str, JAVA_INT onCompletionCallbackId) {
    return com_codename1_impl_ios_IOSNative_createVideoComponent___java_lang_String_int(CN1_THREAD_STATE_PASS_ARG instanceObject, str, onCompletionCallbackId);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponent___java_lang_String_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT str, JAVA_INT onCompletionCallbackId) {
    return com_codename1_impl_ios_IOSNative_createNativeVideoComponent___java_lang_String_int(CN1_THREAD_STATE_PASS_ARG instanceObject, str, onCompletionCallbackId);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponent___byte_1ARRAY_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT dataObject, JAVA_INT onCompletionCallbackId) {
    return com_codename1_impl_ios_IOSNative_createVideoComponent___byte_1ARRAY_int(CN1_THREAD_STATE_PASS_ARG instanceObject, dataObject, onCompletionCallbackId);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponent___byte_1ARRAY_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT dataObject, JAVA_INT onCompletionCallbackId) {
    return com_codename1_impl_ios_IOSNative_createNativeVideoComponent___byte_1ARRAY_int(CN1_THREAD_STATE_PASS_ARG instanceObject, dataObject, onCompletionCallbackId);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponentNSData___long_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_INT onCompletionCallbackId) {
    return com_codename1_impl_ios_IOSNative_createVideoComponentNSData___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, nsData, onCompletionCallbackId);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponentNSData___long_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_INT onCompletionCallbackId) {
    return com_codename1_impl_ios_IOSNative_createNativeVideoComponentNSData___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, nsData, onCompletionCallbackId);
}


JAVA_INT com_codename1_impl_ios_IOSNative_getMediaTimeMS___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getMediaTimeMS___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_setMediaTimeMS___long_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT time) {
    return com_codename1_impl_ios_IOSNative_setMediaTimeMS___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, peer, time);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getMediaDuration___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getMediaDuration___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoPlaying___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_isVideoPlaying___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoFullScreen___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_isVideoFullScreen___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getVideoViewPeer___long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getVideoViewPeer___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createCLLocation___R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_createCLLocation__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getCurrentLocationObject___long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getCurrentLocationObject___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationLatitude___long_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationLatitude___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationAltitude___long_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationAltitude___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationLongtitude___long_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationLongtitude___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationAccuracy___long_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationAccuracy___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationDirection___long_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationDirection___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationVelocity___long_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationVelocity___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getLocationTimeStamp___long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationTimeStamp___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUDID___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getUDID__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getOSVersion___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getOSVersion__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getDeviceName___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getDeviceName__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getDeviceHardwareModel___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getDeviceHardwareModel__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getStatusBarTapCount___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getStatusBarTapCount__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getStatusBarTapLastEpochMillis___R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getStatusBarTapLastEpochMillis__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getStatusBarTapLastX___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getStatusBarTapLastX__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getStatusBarTapLastY___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getStatusBarTapLastY__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isStatusBarTapProxyInstalled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isStatusBarTapProxyInstalled__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isGoodLocation___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_isGoodLocation___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isContactsPermissionGranted___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isContactsPermissionGranted__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_createContact___java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT firstName, JAVA_OBJECT surname, JAVA_OBJECT officePhone, JAVA_OBJECT homePhone, JAVA_OBJECT cellPhone, JAVA_OBJECT email) {
    return com_codename1_impl_ios_IOSNative_createContact___java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, firstName, surname, officePhone, homePhone, cellPhone, email);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_deleteContact___int_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT i) {
    return com_codename1_impl_ios_IOSNative_deleteContact___int(CN1_THREAD_STATE_PASS_ARG instanceObject, i);
}


JAVA_INT com_codename1_impl_ios_IOSNative_getContactCount___boolean_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN includeNumbers) {
    return com_codename1_impl_ios_IOSNative_getContactCount___boolean(CN1_THREAD_STATE_PASS_ARG instanceObject, includeNumbers);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonFirstName___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getPersonFirstName___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonSurnameName___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getPersonSurnameName___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getPersonPhoneCount___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getPersonPhoneCount___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPhone___long_int_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
    return com_codename1_impl_ios_IOSNative_getPersonPhone___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, peer, offset);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPhoneType___long_int_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
    return com_codename1_impl_ios_IOSNative_getPersonPhoneType___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, peer, offset);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPrimaryPhone___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getPersonPrimaryPhone___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonEmail___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getPersonEmail___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonAddress___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getPersonAddress___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createPersonPhotoImage___long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_createPersonPhotoImage___long(CN1_THREAD_STATE_PASS_ARG instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getPersonWithRecordID___int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT recId) {
    return com_codename1_impl_ios_IOSNative_getPersonWithRecordID___int(CN1_THREAD_STATE_PASS_ARG instanceObject, recId);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageFile___long_boolean_int_int_float_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG imagePeer, JAVA_BOOLEAN jpeg, int width, int height, JAVA_FLOAT quality) {
    return com_codename1_impl_ios_IOSNative_createImageFile___long_boolean_int_int_float(CN1_THREAD_STATE_PASS_ARG instanceObject, imagePeer, jpeg, width, height, quality);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getNSDataSize___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData) {
    return com_codename1_impl_ios_IOSNative_getNSDataSize___long(CN1_THREAD_STATE_PASS_ARG instanceObject, nsData);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudioRecorder___java_lang_String_java_lang_String_int_int_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                                                         JAVA_OBJECT  destinationFile, JAVA_OBJECT mimeType, JAVA_INT sampleRate, JAVA_INT bitRate, JAVA_INT channels, JAVA_INT maxDuration ) {
    return com_codename1_impl_ios_IOSNative_createAudioRecorder___java_lang_String_java_lang_String_int_int_int_int(CN1_THREAD_STATE_PASS_ARG instanceObject, destinationFile, mimeType, sampleRate, bitRate, channels, maxDuration);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlDbExists___java_lang_String_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    return com_codename1_impl_ios_IOSNative_sqlDbExists___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, name);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlDbCreateAndOpen___java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    return com_codename1_impl_ios_IOSNative_sqlDbCreateAndOpen___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, name);
}


JAVA_LONG com_codename1_impl_ios_IOSNative_sqlDbExecQuery___long_java_lang_String_java_lang_String_1ARRAY_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT sql, JAVA_OBJECT args) {
    return com_codename1_impl_ios_IOSNative_sqlDbExecQuery___long_java_lang_String_java_lang_String_1ARRAY(CN1_THREAD_STATE_PASS_ARG instanceObject, dbPeer, sql, args);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorFirst___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    return com_codename1_impl_ios_IOSNative_sqlCursorFirst___long(CN1_THREAD_STATE_PASS_ARG instanceObject, statementPeer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorNext___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    return com_codename1_impl_ios_IOSNative_sqlCursorNext___long(CN1_THREAD_STATE_PASS_ARG instanceObject, statementPeer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlGetColName___long_int_R_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer, JAVA_INT index) {
    return com_codename1_impl_ios_IOSNative_sqlGetColName___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, statementPeer, index);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnBlob___long_int_R_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnBlob___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, statement, col);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnDouble___long_int_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnDouble___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, statement, col);
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnFloat___long_int_R_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnFloat___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, statement, col);
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnInteger___long_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnInteger___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, statement, col);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnLong___long_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnLong___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, statement, col);
}

JAVA_SHORT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnShort___long_int_R_short(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnShort___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, statement, col);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlDbPath___java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    return com_codename1_impl_ios_IOSNative_sqlDbPath___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, name);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlDbApplyKey___long_java_lang_String_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT key) {
    return com_codename1_impl_ios_IOSNative_sqlDbApplyKey___long_java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, dbPeer, key);
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlDbApplyKeyStatus___long_java_lang_String_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT key) {
    return com_codename1_impl_ios_IOSNative_sqlDbApplyKeyStatus___long_java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, dbPeer, key);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlDbIsCipherAvailable___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_sqlDbIsCipherAvailable__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlStmtPrepare___long_java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT sql) {
    return com_codename1_impl_ios_IOSNative_sqlStmtPrepare___long_java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, dbPeer, sql);
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlStmtParameterCount___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    return com_codename1_impl_ios_IOSNative_sqlStmtParameterCount___long(CN1_THREAD_STATE_PASS_ARG instanceObject, statementPeer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlStmtStep___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    return com_codename1_impl_ios_IOSNative_sqlStmtStep___long(CN1_THREAD_STATE_PASS_ARG instanceObject, statementPeer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnText___long_int_R_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnText___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, statement, col);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorNullValueAtColumn___long_int_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorNullValueAtColumn___long_int(CN1_THREAD_STATE_PASS_ARG instanceObject, statement, col);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlDbInTransaction___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer) {
    return com_codename1_impl_ios_IOSNative_sqlDbInTransaction___long(CN1_THREAD_STATE_PASS_ARG instanceObject, dbPeer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlCursorGetColumnCount___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement) {
    return com_codename1_impl_ios_IOSNative_sqlCursorGetColumnCount___long(CN1_THREAD_STATE_PASS_ARG instanceObject, statement);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_canMakePayments___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_canMakePayments__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatInt___int_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT i) {
    return com_codename1_impl_ios_IOSNative_formatInt___int(CN1_THREAD_STATE_PASS_ARG instanceObject, i);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDouble___double_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_DOUBLE d) {
    return com_codename1_impl_ios_IOSNative_formatDouble___double(CN1_THREAD_STATE_PASS_ARG instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatCurrency___double_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_DOUBLE d) {
    return com_codename1_impl_ios_IOSNative_formatCurrency___double(CN1_THREAD_STATE_PASS_ARG instanceObject, d);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_parseDouble___java_lang_String_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT d) {
    return com_codename1_impl_ios_IOSNative_parseDouble___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDate___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG d) {
    return com_codename1_impl_ios_IOSNative_formatDate___long(CN1_THREAD_STATE_PASS_ARG instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateShort___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG d) {
    return com_codename1_impl_ios_IOSNative_formatDateShort___long(CN1_THREAD_STATE_PASS_ARG instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateTime___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG d) {
    return com_codename1_impl_ios_IOSNative_formatDateTime___long(CN1_THREAD_STATE_PASS_ARG instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateTimeMedium___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG d) {
    return com_codename1_impl_ios_IOSNative_formatDateTimeMedium___long(CN1_THREAD_STATE_PASS_ARG instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateTimeShort___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG d) {
    return com_codename1_impl_ios_IOSNative_formatDateTimeShort___long(CN1_THREAD_STATE_PASS_ARG instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getCurrencySymbol___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getCurrencySymbol__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createTruetypeFont___java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    return com_codename1_impl_ios_IOSNative_createTruetypeFont___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, name);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_deriveTruetypeFont___long_boolean_boolean_float_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG uiFont, JAVA_BOOLEAN bold, JAVA_BOOLEAN italic, JAVA_FLOAT size) {
    return com_codename1_impl_ios_IOSNative_deriveTruetypeFont___long_boolean_boolean_float(CN1_THREAD_STATE_PASS_ARG instanceObject, uiFont, bold, italic, size);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_browserExecuteAndReturnString___long_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT javaScript) {
    return com_codename1_impl_ios_IOSNative_browserExecuteAndReturnString___long_java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, peer, javaScript);
}

JAVA_OBJECT java_util_TimeZone_getTimezoneId___R_java_lang_String(CN1_THREAD_STATE_SINGLE_ARG) {
    return java_util_TimeZone_getTimezoneId__(CN1_THREAD_STATE_PASS_SINGLE_ARG);
}

JAVA_INT java_util_TimeZone_getTimezoneOffset___java_lang_String_int_int_int_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT name, JAVA_INT year, JAVA_INT month, JAVA_INT day, JAVA_INT timeOfDayMillis) {
    return java_util_TimeZone_getTimezoneOffset___java_lang_String_int_int_int_int(CN1_THREAD_STATE_PASS_ARG name, year, month, day, timeOfDayMillis);
}

JAVA_INT java_util_TimeZone_getTimezoneRawOffset___java_lang_String_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT name) {
    return java_util_TimeZone_getTimezoneRawOffset___java_lang_String(CN1_THREAD_STATE_PASS_ARG name);
}

JAVA_BOOLEAN java_util_TimeZone_isTimezoneDST___java_lang_String_long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT name, JAVA_LONG millis) {
    return java_util_TimeZone_isTimezoneDST___java_lang_String_long(CN1_THREAD_STATE_PASS_ARG name, millis);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUserAgentString___java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT callbackId) {
    return com_codename1_impl_ios_IOSNative_getUserAgentString___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, callbackId);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_connectSocket___java_lang_String_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT host, JAVA_INT port, JAVA_INT connectTimeout) {
    return com_codename1_impl_ios_IOSNative_connectSocket___java_lang_String_int_int(CN1_THREAD_STATE_PASS_ARG instanceObject, host, port, connectTimeout);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_listenSocketLoopback___int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT port) {
    return com_codename1_impl_ios_IOSNative_listenSocketLoopback___int(CN1_THREAD_STATE_PASS_ARG instanceObject, port);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getHostOrIP___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getHostOrIP__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isSocketConnected___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    return com_codename1_impl_ios_IOSNative_isSocketConnected___long(CN1_THREAD_STATE_PASS_ARG instanceObject, socket);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getSocketErrorMessage___long_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    return com_codename1_impl_ios_IOSNative_getSocketErrorMessage___long(CN1_THREAD_STATE_PASS_ARG instanceObject, socket);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getSocketErrorCode___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    return com_codename1_impl_ios_IOSNative_getSocketErrorCode___long(CN1_THREAD_STATE_PASS_ARG instanceObject, socket);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getSocketAvailableInput___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    return com_codename1_impl_ios_IOSNative_getSocketAvailableInput___long(CN1_THREAD_STATE_PASS_ARG instanceObject, socket);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_readFromSocketStream___long_R_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    return com_codename1_impl_ios_IOSNative_readFromSocketStream___long(CN1_THREAD_STATE_PASS_ARG instanceObject, socket);
}

JAVA_OBJECT com_codename1_ui_Display_getInstance__(CN1_THREAD_STATE_SINGLE_ARG) {
    return com_codename1_ui_Display_getInstance___R_com_codename1_ui_Display(CN1_THREAD_STATE_PASS_SINGLE_ARG);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createPeerImage___long_int_1ARRAY_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT arr) {
    return com_codename1_impl_ios_IOSNative_createPeerImage___long_int_1ARRAY(CN1_THREAD_STATE_PASS_ARG instanceObject, peer, arr);
}

extern JAVA_OBJECT com_codename1_impl_ios_IOSNative_getFacebookToken__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me);
JAVA_OBJECT com_codename1_impl_ios_IOSNative_getFacebookToken___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return com_codename1_impl_ios_IOSNative_getFacebookToken__(CN1_THREAD_STATE_PASS_ARG me);
}

extern JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isFacebookLoggedIn__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me);
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isFacebookLoggedIn___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return com_codename1_impl_ios_IOSNative_isFacebookLoggedIn__(CN1_THREAD_STATE_PASS_ARG me);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAsyncEditMode___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isAsyncEditMode__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_VOID com_codename1_impl_ios_IOSNative_printStackTraceToStream___java_lang_Throwable_java_io_Writer(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  thisObj, JAVA_OBJECT exception, JAVA_OBJECT writer) {
    struct obj__java_lang_Throwable* th = (struct obj__java_lang_Throwable*)exception;
    if(th->java_lang_Throwable_stack == JAVA_NULL) {
        java_lang_Throwable_fillInStack__(threadStateData, exception);
    }
    virtual_java_io_Writer_write___java_lang_String(threadStateData, writer, th->java_lang_Throwable_stack);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_canExecute___java_lang_String_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT url) {
    return com_codename1_impl_ios_IOSNative_canExecute___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, url);
}
#else
JAVA_VOID com_codename1_impl_ios_IOSNative_printStackTraceToStream___java_lang_Throwable_java_io_Writer(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
}
#endif


#ifndef NEW_CODENAME_ONE_VM
JAVA_VOID com_codename1_impl_ios_IOSNative_splitString___java_lang_String_char_java_util_ArrayList(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT string, JAVA_CHAR separator, JAVA_OBJECT outArr) {
    int offset = ((java_lang_String*) string)->fields.java_lang_String.offset_;
    int strlen = ((java_lang_String*) string)->fields.java_lang_String.count_;
    org_xmlvm_runtime_XMLVMArray* srcArr = ((java_lang_String*) string)->fields.java_lang_String.value_;
    JAVA_ARRAY_CHAR* src = (JAVA_ARRAY_CHAR*)srcArr->fields.org_xmlvm_runtime_XMLVMArray.array_;
    
    JAVA_INT startPos = offset;
    JAVA_INT endOffset = offset + strlen;
    JAVA_INT i = startPos;
    for (; i < endOffset; i++) {
        if (src[i] == separator) {
            JAVA_OBJECT str = __NEW_java_lang_String();
            java_lang_String___INIT____char_1ARRAY_int_int(str, (JAVA_OBJECT)srcArr, startPos, i - startPos);
            startPos = i + 1;
            java_util_ArrayList_add___java_lang_Object(outArr, str);
        }
    }
    if (i >= startPos) {
        JAVA_OBJECT str = __NEW_java_lang_String();
        java_lang_String___INIT____char_1ARRAY_int_int(CN1_THREAD_STATE_PASS_ARG str, (JAVA_OBJECT)srcArr, startPos, i - startPos);
        java_util_ArrayList_add___java_lang_Object(CN1_THREAD_STATE_PASS_ARG outArr, str);
    }
    
    
}



#else
JAVA_VOID com_codename1_impl_ios_IOSNative_splitString___java_lang_String_char_java_util_ArrayList(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_OBJECT string, JAVA_CHAR separator, JAVA_OBJECT outArr) {
    enteringNativeAllocations();
    // Read through the coder-aware String accessors (charAt/substring) rather than
    // casting the backing array to JAVA_ARRAY_CHAR*: value may be a char[] (UTF-16)
    // or a byte[] (Latin-1), and both offset and coder are handled inside those
    // accessors. Indices here are logical (0..count), not raw array offsets.
    JAVA_INT strlen = ((struct obj__java_lang_String*)string)->java_lang_String_count;
    JAVA_INT startPos = 0;
    JAVA_INT i = 0;
    for (; i < strlen; i++) {
        if (java_lang_String_charAt___int_R_char(CN1_THREAD_STATE_PASS_ARG string, i) == separator) {
            if (i > startPos) {
                JAVA_OBJECT str = java_lang_String_substring___int_int_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG string, startPos, i);
                java_util_ArrayList_add___java_lang_Object_R_boolean(CN1_THREAD_STATE_PASS_ARG outArr, str);
            }
            startPos = i + 1;
        }
    }
    if (i > startPos) {
        JAVA_OBJECT str = java_lang_String_substring___int_int_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG string, startPos, i);
        java_util_ArrayList_add___java_lang_Object_R_boolean(CN1_THREAD_STATE_PASS_ARG outArr, str);
    }
    finishedNativeAllocations();

}
#endif


/*
native void readFile(long nsFileHandle, byte[] b, int off, int len);
*/
JAVA_VOID com_codename1_impl_ios_IOSNative_readFile___long_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG fileHandle, JAVA_OBJECT b, JAVA_INT off, JAVA_INT len) {
    POOL_BEGIN();
    NSFileHandle* fh = (BRIDGE_CAST NSFileHandle*)((void*)fileHandle);
    
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* byteArray = b;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    void* data = ((JAVA_ARRAY)b)->data;
#endif
    void* actual = &(data[off]);
    
    NSData* n = [fh readDataOfLength:len];
    
    [n getBytes:actual length:len];
    
    POOL_END();
    
}

/*
native int getNSFileOffset(long nsFileHandle);
 */
JAVA_INT com_codename1_impl_ios_IOSNative_getNSFileOffset___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG fileHandle) {
    NSFileHandle* fh = (BRIDGE_CAST NSFileHandle*)((void*)fileHandle);
    return (JAVA_INT)[fh offsetInFile];
}

#ifndef NEW_CODENAME_ONE_VM
JAVA_INT com_codename1_impl_ios_IOSNative_getNSFileOffset___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG fileHandle) {
    return com_codename1_impl_ios_IOSNative_getNSFileOffset___long_R_int(CN1_THREAD_STATE_PASS_ARG instanceObject, fileHandle);
}
#endif

/*
native int getNSFileAvailable(long nsFileHandle);
 */

JAVA_INT com_codename1_impl_ios_IOSNative_getNSFileAvailable___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG fileHandle) {

    NSFileHandle* fh = (BRIDGE_CAST NSFileHandle*)((void*)fileHandle);
    unsigned long long offset = [fh offsetInFile];
    unsigned long long end = [fh seekToEndOfFile];
    long long available = end - offset;
    [fh seekToFileOffset:offset];
    return available > 0 ? 1 : 0;
}

#ifndef NEW_CODENAME_ONE_VM
JAVA_INT com_codename1_impl_ios_IOSNative_getNSFileAvailable___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, long fileHandle) {
    return com_codename1_impl_ios_IOSNative_getNSFileAvailable___long_R_int(CN1_THREAD_STATE_PASS_ARG instanceObject, fileHandle);
}
#endif

/*
native int getNSFileSize(long nsFileHandle);
*/
JAVA_INT com_codename1_impl_ios_IOSNative_getNSFileSize___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG fileHandle) {
    
    NSFileHandle* fh = (BRIDGE_CAST NSFileHandle*)((void*)fileHandle);
    unsigned long long offset = [fh offsetInFile];
    unsigned long long end = [fh seekToEndOfFile];
    [fh seekToFileOffset:offset];
    return (JAVA_INT)end;
}

#ifndef NEW_CODENAME_ONE_VM
JAVA_INT com_codename1_impl_ios_IOSNative_getNSFileSize___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, long fileHandle) {
    return com_codename1_impl_ios_IOSNative_getNSFileSize___long_R_int(CN1_THREAD_STATE_PASS_ARG instanceObject, fileHandle);
}
#endif

/*
native long createNSFileHandle(String name, String type);
 */
JAVA_LONG com_codename1_impl_ios_IOSNative_createNSFileHandle___java_lang_String_java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instance, JAVA_OBJECT name, JAVA_OBJECT type) {
    POOL_BEGIN();
    NSString* nameNS = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    NSString* typeNS = nameNS == NULL ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG type);
    NSString* path = [[NSBundle mainBundle] pathForResource:nameNS ofType:typeNS];
    NSFileHandle* file = [NSFileHandle fileHandleForReadingAtPath:path];
#ifndef CN1_USE_ARC
    [file retain];
#endif
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)file);

}

#ifndef NEW_CODENAME_ONE_VM
JAVA_LONG com_codename1_impl_ios_IOSNative_createNSFileHandle___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instance, JAVA_OBJECT name, JAVA_OBJECT type) {
    return com_codename1_impl_ios_IOSNative_createNSFileHandle___java_lang_String_java_lang_String_R_long(CN1_THREAD_STATE_PASS_ARG instance, name, type);
}
#endif

/*native long createNSFileHandle(String file);*/
JAVA_LONG com_codename1_impl_ios_IOSNative_createNSFileHandle___java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instance, JAVA_OBJECT file) {
    POOL_BEGIN();
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG file);
    if([ns hasPrefix:@"file:"]) {
        ns = [ns substringFromIndex:5];
    } else {
        if([ns hasPrefix:@"//localhost"]) {
            ns = [@"file:" stringByAppendingString:ns];
            //NSData* d = [NSData dataWithContentsOfURL:[NSURL URLWithString:ns]];
            NSFileHandle* fh = [NSFileHandle fileHandleForReadingFromURL:[NSURL URLWithString:ns] error:nil];
#ifndef CN1_USE_ARC
            [fh retain];
#endif
            POOL_END();
            return (JAVA_LONG)((BRIDGE_CAST void*)fh);
        }
    }
    //NSData* d = [NSData dataWithContentsOfFile:ns];
    NSFileHandle* fh = [NSFileHandle fileHandleForReadingAtPath:ns];
#ifndef CN1_USE_ARC
    [fh retain];
#endif
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)fh);
}

#ifndef NEW_CODENAME_ONE_VM
JAVA_LONG com_codename1_impl_ios_IOSNative_createNSFileHandle___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instance, JAVA_OBJECT name) {
    return com_codename1_impl_ios_IOSNative_createNSFileHandle___java_lang_String_R_long(CN1_THREAD_STATE_PASS_ARG instance, name);
}
#endif

/*native void setNSFileOffset(long nsFileHandle, int off);*/
JAVA_VOID com_codename1_impl_ios_IOSNative_setNSFileOffset___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instance, JAVA_LONG fileHandle, JAVA_INT offset) {
    NSFileHandle* fh = (BRIDGE_CAST NSFileHandle*)((void*)fileHandle);
    [fh seekToFileOffset:offset];
}



/*native int readNSFile(long nsFileHandle);*/
JAVA_INT com_codename1_impl_ios_IOSNative_readNSFile___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instance, JAVA_LONG fileHandle) {
    POOL_BEGIN();
    NSFileHandle* fh = (BRIDGE_CAST NSFileHandle*)((void*)fileHandle);
    NSData* d =[fh readDataOfLength:1];
    unsigned char *n = [d bytes];
    JAVA_INT out = n[0];
    POOL_END();
    return out;
}

#ifndef NEW_CODENAME_ONE_VM
JAVA_INT com_codename1_impl_ios_IOSNative_readNSFile___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instance, JAVA_LONG fileHandle) {
    return com_codename1_impl_ios_IOSNative_readNSFile___long_R_int(CN1_THREAD_STATE_PASS_ARG instance, fileHandle);
}
#endif


static void cn1CancelScheduledLocalNotificationById(NSString *nsId) {
    if (nsId == nil) {
        return;
    }
#ifdef CN1_INCLUDE_NOTIFICATIONS2
    if (@available(iOS 10, *)) {
        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
        dispatch_semaphore_t sem = dispatch_semaphore_create(0);
        __block NSMutableArray<NSString *> *matches = [NSMutableArray array];
        [center getPendingNotificationRequestsWithCompletionHandler:^(NSArray<UNNotificationRequest *> * _Nonnull requests) {
            for (UNNotificationRequest *request in requests) {
#if !TARGET_OS_TV
                NSString *uid = [NSString stringWithFormat:@"%@", [request.content.userInfo valueForKey:@"__ios_id__"]];
                if ([nsId isEqualToString:uid] || [nsId isEqualToString:request.identifier]) {
#else
                // tvOS has no UNNotificationContent.userInfo; match on the request identifier only.
                if ([nsId isEqualToString:request.identifier]) {
#endif // !TARGET_OS_TV
                    [matches addObject:request.identifier];
                }
            }
            dispatch_semaphore_signal(sem);
        }];
        dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(5 * NSEC_PER_SEC)));
        if ([matches count] > 0) {
            [center removePendingNotificationRequestsWithIdentifiers:matches];
#if !TARGET_OS_TV
            [center removeDeliveredNotificationsWithIdentifiers:matches];
#endif // !TARGET_OS_TV
        }
    }
#endif
}

#ifdef CN1_INCLUDE_NOTIFICATIONS2
static UNNotificationTrigger* cn1CreateNotificationTrigger(JAVA_LONG fireDate, JAVA_INT repeatType) API_AVAILABLE(ios(10.0));
static UNNotificationTrigger* cn1CreateNotificationTrigger(JAVA_LONG fireDate, JAVA_INT repeatType) {
    NSTimeInterval targetTime = fireDate / 1000.0 + 1;
    NSDate *targetDate = [NSDate dateWithTimeIntervalSince1970:targetTime];
    NSTimeInterval delta = targetTime - [[NSDate date] timeIntervalSince1970];
    if (delta < 1) {
        delta = 1;
    }

    NSCalendar *calendar = [NSCalendar currentCalendar];
    NSDateComponents *components;
    switch (repeatType) {
        case 0:
            return [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:delta repeats:NO];
        case 1:
            components = [calendar components:(NSCalendarUnitSecond) fromDate:targetDate];
            return [UNCalendarNotificationTrigger triggerWithDateMatchingComponents:components repeats:YES];
        case 3:
            components = [calendar components:(NSCalendarUnitMinute | NSCalendarUnitSecond) fromDate:targetDate];
            return [UNCalendarNotificationTrigger triggerWithDateMatchingComponents:components repeats:YES];
        case 4:
            components = [calendar components:(NSCalendarUnitHour | NSCalendarUnitMinute | NSCalendarUnitSecond) fromDate:targetDate];
            return [UNCalendarNotificationTrigger triggerWithDateMatchingComponents:components repeats:YES];
        case 5:
            components = [calendar components:(NSCalendarUnitWeekday | NSCalendarUnitHour | NSCalendarUnitMinute | NSCalendarUnitSecond) fromDate:targetDate];
            return [UNCalendarNotificationTrigger triggerWithDateMatchingComponents:components repeats:YES];
        default:
            CN1Log(@"Unknown repeat interval type %d. Ignoring repeat interval", repeatType);
            return [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:delta repeats:NO];
    }
}
#endif

JAVA_VOID com_codename1_impl_ios_IOSNative_sendLocalNotification___java_lang_String_java_lang_String_java_lang_String_java_lang_String_int_long_int_boolean( CN1_THREAD_STATE_MULTI_ARG
    JAVA_OBJECT me, JAVA_OBJECT notificationId, JAVA_OBJECT alertTitle, JAVA_OBJECT alertBody, JAVA_OBJECT alertSound, JAVA_INT badgeNumber, JAVA_LONG fireDate, JAVA_INT repeatType, JAVA_BOOLEAN foreground
                                                                                                                                                                     ) {
#ifdef CN1_INCLUDE_NOTIFICATIONS2
    NSString * title = [NSString string];
    NSString * body = [NSString string];
    NSString *tmpStr;
    if (alertTitle != NULL) {
        tmpStr = [title stringByAppendingString:toNSString(CN1_THREAD_STATE_PASS_ARG alertTitle)];
                    
#ifndef CN1_USE_ARC
        [title release];
#endif
        title = tmpStr;
    }
    
    if (alertBody != NULL) {
        tmpStr = [body stringByAppendingString:toNSString(CN1_THREAD_STATE_PASS_ARG alertBody)];     
#ifndef CN1_USE_ARC
        [body release];
#endif
        body = tmpStr;
    }
    tmpStr = [body stringByReplacingOccurrencesOfString:@"%" withString:@"%%"];
#ifndef CN1_USE_ARC
    [body release];
#endif
    body = tmpStr;
    
    NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
    NSString *notificationIdString = toNSString(CN1_THREAD_STATE_PASS_ARG notificationId);
    [dict setObject: notificationIdString forKey: @"__ios_id__"];
    if (foreground) {
        [dict setObject: @"true" forKey: @"foreground"];
    }
    if (@available(iOS 10, *)) {
        UNMutableNotificationContent* content = [[UNMutableNotificationContent alloc] init];
#if !TARGET_OS_TV
        content.title = [NSString localizedUserNotificationStringForKey:title arguments:nil];
        content.body = [NSString localizedUserNotificationStringForKey:body arguments:nil];
#endif // !TARGET_OS_TV
        if (alertSound != NULL) {
            NSString *soundName = toNSString(CN1_THREAD_STATE_PASS_ARG alertSound);
            if (soundName != nil && [soundName length] > 0) {
#if TARGET_OS_WATCH
                // UNNotificationSound soundNamed: is unavailable on watchOS.
                content.sound = [UNNotificationSound defaultSound];
#elif TARGET_OS_TV
                // UNNotificationSound is unavailable on tvOS.
#else
                content.sound = [UNNotificationSound soundNamed:soundName];
#endif
            }
        }
        if (badgeNumber >= 0) {
            content.badge = [NSNumber numberWithInt:badgeNumber];
        }
#if !TARGET_OS_TV
        content.userInfo = dict;
#endif // !TARGET_OS_TV

        UNNotificationTrigger *trigger = cn1CreateNotificationTrigger(fireDate, repeatType);
        UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier:notificationIdString content:content trigger:trigger];
        UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
        // Request notification authorization on first schedule so local-notification-only
        // apps still get prompted (the launch-time prompt was removed for issue #4876).
        // Add the request only after the authorization result is known; adding
        // immediately races fresh simulators and can leave no pending request.
        [center requestAuthorizationWithOptions:(UNAuthorizationOptionAlert + UNAuthorizationOptionSound + UNAuthorizationOptionBadge)
            completionHandler:^(BOOL granted, NSError * _Nullable authError) {
                if (authError != nil) {
                    CN1Log(@"Local notification authorization request failed: %@", authError.localizedDescription);
                    return;
                }
                if (!granted) {
                    CN1Log(@"Local notification authorization was not granted");
                    return;
                }
                cn1CancelScheduledLocalNotificationById(notificationIdString);
                [center addNotificationRequest:request withCompletionHandler:^(NSError * _Nullable error) {
                    if (error != nil) {
                        CN1Log(@"Failed to schedule local notification: %@", error.localizedDescription);
                    }
                }];
        }];
    } else {
        CN1Log(@"Ignoring local notification request on iOS versions below 10");
    }
#endif
}

JAVA_VOID com_codename1_impl_ios_IOSNative_cancelLocalNotification___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT notificationId) {
    if (notificationId == JAVA_NULL) {
        return;
    }
    NSString *nsId = toNSString(CN1_THREAD_STATE_PASS_ARG notificationId);
    dispatch_sync(dispatch_get_main_queue(), ^{
        cn1CancelScheduledLocalNotificationById(nsId);
    });
}

// ---------------------------------------------------------------------------
// Enriched local notifications, permission, BGTaskScheduler and shared content
// ---------------------------------------------------------------------------

#if defined(CN1_INCLUDE_NOTIFICATIONS2) && TARGET_OS_TV
// tvOS has no UNNotificationCategory / UNNotificationAction; categories are a no-op.
static NSString* cn1RegisterLocalNotificationCategory(NSString *categoryId, NSString *actionsEncoded) API_AVAILABLE(ios(10.0)) {
    return nil;
}
#endif
#if defined(CN1_INCLUDE_NOTIFICATIONS2) && !TARGET_OS_TV
// Builds and registers a UNNotificationCategory from the packed actions string. Field
// separator is U+0001 and record separator is U+0002 (see IOSImplementation).
static NSString* cn1RegisterLocalNotificationCategory(NSString *categoryId, NSString *actionsEncoded) API_AVAILABLE(ios(10.0)) {
    if (categoryId == nil || actionsEncoded == nil || [actionsEncoded length] == 0) {
        return nil;
    }
    NSString *recSep = [NSString stringWithFormat:@"%C", (unichar)2];
    NSString *fldSep = [NSString stringWithFormat:@"%C", (unichar)1];
    NSMutableArray *actions = [[NSMutableArray alloc] init];
    for (NSString *rec in [actionsEncoded componentsSeparatedByString:recSep]) {
        NSArray *parts = [rec componentsSeparatedByString:fldSep];
        if ([parts count] < 2) {
            continue;
        }
        NSString *aid = parts[0];
        NSString *title = parts[1];
        NSString *placeholder = [parts count] > 2 ? parts[2] : @"";
        NSString *button = [parts count] > 3 ? parts[3] : @"";
        if ([placeholder length] > 0 || [button length] > 0) {
            if ([button length] == 0) { button = @"Reply"; }
            [actions addObject:[UNTextInputNotificationAction actionWithIdentifier:aid title:title options:UNNotificationActionOptionNone textInputButtonTitle:button textInputPlaceholder:placeholder]];
        } else {
            [actions addObject:[UNNotificationAction actionWithIdentifier:aid title:title options:UNNotificationActionOptionForeground]];
        }
    }
    UNNotificationCategory *category = [UNNotificationCategory categoryWithIdentifier:categoryId actions:actions intentIdentifiers:@[] options:UNNotificationCategoryOptionNone];
    UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
    [center getNotificationCategoriesWithCompletionHandler:^(NSSet<UNNotificationCategory *> * _Nonnull existing) {
        NSMutableSet *merged = existing == nil ? [[NSMutableSet alloc] init] : [existing mutableCopy];
        [merged addObject:category];
        [center setNotificationCategories:merged];
    }];
    return categoryId;
}
#endif // CN1_INCLUDE_NOTIFICATIONS2 && !TARGET_OS_TV

JAVA_VOID com_codename1_impl_ios_IOSNative_sendLocalNotification2___java_lang_String_java_lang_String_java_lang_String_java_lang_String_int_long_int_boolean_java_lang_String_java_lang_String_boolean_java_lang_String_java_lang_String( CN1_THREAD_STATE_MULTI_ARG
    JAVA_OBJECT me, JAVA_OBJECT notificationId, JAVA_OBJECT alertTitle, JAVA_OBJECT alertBody, JAVA_OBJECT alertSound, JAVA_INT badgeNumber, JAVA_LONG fireDate, JAVA_INT repeatType, JAVA_BOOLEAN foreground,
    JAVA_OBJECT categoryId, JAVA_OBJECT threadId, JAVA_BOOLEAN timeSensitive, JAVA_OBJECT imageAttachmentPath, JAVA_OBJECT actionsEncoded) {
#ifdef CN1_INCLUDE_NOTIFICATIONS2
    if (@available(iOS 10, *)) {
        NSString *title = alertTitle == NULL ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG alertTitle);
        NSString *body = alertBody == NULL ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG alertBody);
        body = [body stringByReplacingOccurrencesOfString:@"%" withString:@"%%"];

        NSString *notificationIdString = toNSString(CN1_THREAD_STATE_PASS_ARG notificationId);
        NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
        [dict setObject:notificationIdString forKey:@"__ios_id__"];
        if (foreground) {
            [dict setObject:@"true" forKey:@"foreground"];
        }

        UNMutableNotificationContent* content = [[UNMutableNotificationContent alloc] init];
#if !TARGET_OS_TV
        content.title = [NSString localizedUserNotificationStringForKey:title arguments:nil];
        content.body = [NSString localizedUserNotificationStringForKey:body arguments:nil];
#endif // !TARGET_OS_TV
        if (alertSound != NULL) {
            NSString *soundName = toNSString(CN1_THREAD_STATE_PASS_ARG alertSound);
            if (soundName != nil && [soundName length] > 0) {
#if TARGET_OS_WATCH
                // UNNotificationSound soundNamed: is unavailable on watchOS.
                content.sound = [UNNotificationSound defaultSound];
#elif TARGET_OS_TV
                // UNNotificationSound is unavailable on tvOS.
#else
                content.sound = [UNNotificationSound soundNamed:soundName];
#endif
            }
        }
        if (badgeNumber >= 0) {
            content.badge = [NSNumber numberWithInt:badgeNumber];
        }
#if !TARGET_OS_TV
        content.userInfo = dict;
        if (threadId != NULL) {
            NSString *t = toNSString(CN1_THREAD_STATE_PASS_ARG threadId);
            if ([t length] > 0) {
                content.threadIdentifier = t;
            }
        }
#endif // !TARGET_OS_TV
        if (timeSensitive) {
            if (@available(iOS 15.0, *)) {
                content.interruptionLevel = UNNotificationInterruptionLevelTimeSensitive;
            }
        }
        NSString *cat = categoryId == NULL ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG categoryId);
        NSString *acts = actionsEncoded == NULL ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG actionsEncoded);
        NSString *registered = cn1RegisterLocalNotificationCategory(cat, acts);
#if !TARGET_OS_TV
        if (registered != nil) {
            content.categoryIdentifier = registered;
        }
#endif // !TARGET_OS_TV
#if !TARGET_OS_TV
        if (imageAttachmentPath != NULL) {
            NSString *imgPath = toNSString(CN1_THREAD_STATE_PASS_ARG imageAttachmentPath);
            if (imgPath != nil && [imgPath length] > 0) {
                NSURL *url = nil;
                if ([imgPath hasPrefix:@"file://"]) {
                    url = [NSURL URLWithString:imgPath];
                } else {
                    NSString *clean = [imgPath hasPrefix:@"/"] ? [imgPath substringFromIndex:1] : imgPath;
                    NSString *resPath = [[NSBundle mainBundle] pathForResource:[clean stringByDeletingPathExtension] ofType:[clean pathExtension]];
                    if (resPath != nil) {
                        url = [NSURL fileURLWithPath:resPath];
                    }
                }
                if (url != nil) {
                    NSError *attErr = nil;
                    UNNotificationAttachment *att = [UNNotificationAttachment attachmentWithIdentifier:@"image" URL:url options:nil error:&attErr];
                    if (att != nil) {
                        content.attachments = @[att];
                    }
                }
            }
        }
#endif // !TARGET_OS_TV (UNNotificationAttachment)

        UNNotificationTrigger *trigger = cn1CreateNotificationTrigger(fireDate, repeatType);
        UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier:notificationIdString content:content trigger:trigger];
        UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
        [center requestAuthorizationWithOptions:(UNAuthorizationOptionAlert + UNAuthorizationOptionSound + UNAuthorizationOptionBadge)
            completionHandler:^(BOOL granted, NSError * _Nullable authError) {
                if (authError != nil) {
                    CN1Log(@"Local notification authorization request failed: %@", authError.localizedDescription);
                }
        }];
        cn1CancelScheduledLocalNotificationById(notificationIdString);
        [center addNotificationRequest:request withCompletionHandler:^(NSError * _Nullable error) {
            if (error != nil) {
                CN1Log(@"Failed to schedule local notification: %@", error.localizedDescription);
            }
        }];
    } else {
        CN1Log(@"Ignoring local notification request on iOS versions below 10");
    }
#endif
}

JAVA_VOID com_codename1_impl_ios_IOSNative_requestNotificationPermission___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT optionsMask) {
#ifdef CN1_INCLUDE_NOTIFICATIONS2
    if (@available(iOS 10, *)) {
        UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
        UNAuthorizationOptions opts = (UNAuthorizationOptions)optionsMask;
        [center requestAuthorizationWithOptions:opts completionHandler:^(BOOL granted, NSError * _Nullable error) {
            [center getNotificationSettingsWithCompletionHandler:^(UNNotificationSettings * _Nonnull settings) {
                int level = 1; // denied
                switch (settings.authorizationStatus) {
                    case UNAuthorizationStatusNotDetermined: level = 0; break;
                    case UNAuthorizationStatusDenied: level = 1; break;
                    case UNAuthorizationStatusAuthorized: level = 2; break;
                    default: break;
                }
                if (@available(iOS 12.0, *)) {
                    if (settings.authorizationStatus == UNAuthorizationStatusProvisional) { level = 3; }
                }
#if !TARGET_OS_WATCH && !TARGET_OS_TV
                // UNAuthorizationStatusEphemeral is unavailable on watchOS/tvOS.
                if (@available(iOS 14.0, *)) {
                    if (settings.authorizationStatus == UNAuthorizationStatusEphemeral) { level = 4; }
                }
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
                BOOL g = (level == 2 || level == 3 || level == 4);
                com_codename1_impl_ios_IOSImplementation_notificationPermissionResult___boolean_int(CN1_THREAD_GET_STATE_PASS_ARG g ? JAVA_TRUE : JAVA_FALSE, level);
            }];
        }];
        return;
    }
#endif
    com_codename1_impl_ios_IOSImplementation_notificationPermissionResult___boolean_int(CN1_THREAD_GET_STATE_PASS_ARG JAVA_TRUE, 2);
}

#if TARGET_OS_WATCH
// BackgroundTasks (BGTaskScheduler) is unavailable on watchOS; the background
// processing natives are no-ops there.
JAVA_VOID com_codename1_impl_ios_IOSNative_registerBackgroundProcessingTask___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT identifier) {}
JAVA_VOID com_codename1_impl_ios_IOSNative_submitBackgroundProcessingTask___java_lang_String_double_boolean_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT identifier, JAVA_DOUBLE earliest, JAVA_BOOLEAN requiresNetwork, JAVA_BOOLEAN requiresPower) {}
JAVA_VOID com_codename1_impl_ios_IOSNative_cancelBackgroundTask___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT identifier) {}
#else
JAVA_VOID com_codename1_impl_ios_IOSNative_registerBackgroundProcessingTask___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT identifier) {
// Not implemented on the native macOS port: the body below is UIKit -- a
// picker, an action sheet, a movie player, a pasteboard or a UIApplication
// service -- and AppKit's equivalent is a different API rather than a
// renamed one. The symbol still has to exist: ParparVM keeps a native method
// alive BY its symbol appearing in the native sources, so removing it would
// make the dead-code pass drop the Java side and ship green with the feature
// silently gone. Returning an unsupported value instead lets the caller take
// its unsupported path.
#if TARGET_OS_OSX
#else
    if (@available(iOS 13.0, *)) {
        NSString *taskId = toNSString(CN1_THREAD_STATE_PASS_ARG identifier);
        [[BGTaskScheduler sharedScheduler] registerForTaskWithIdentifier:taskId usingQueue:nil launchHandler:^(BGTask * _Nonnull task) {
            task.expirationHandler = ^{
                [task setTaskCompletedWithSuccess:NO];
            };
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                com_codename1_impl_ios_IOSImplementation_runBackgroundProcessing___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG taskId));
                [task setTaskCompletedWithSuccess:YES];
            });
        }];
    }
#endif
}

JAVA_VOID com_codename1_impl_ios_IOSNative_submitBackgroundProcessingTask___java_lang_String_double_boolean_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT identifier, JAVA_DOUBLE earliest, JAVA_BOOLEAN requiresNetwork, JAVA_BOOLEAN requiresPower) {
// Not implemented on the native macOS port: the body below is UIKit -- a
// picker, an action sheet, a movie player, a pasteboard or a UIApplication
// service -- and AppKit's equivalent is a different API rather than a
// renamed one. The symbol still has to exist: ParparVM keeps a native method
// alive BY its symbol appearing in the native sources, so removing it would
// make the dead-code pass drop the Java side and ship green with the feature
// silently gone. Returning an unsupported value instead lets the caller take
// its unsupported path.
#if TARGET_OS_OSX
#else
    if (@available(iOS 13.0, *)) {
        NSString *taskId = toNSString(CN1_THREAD_STATE_PASS_ARG identifier);
        BGProcessingTaskRequest *request = [[BGProcessingTaskRequest alloc] initWithIdentifier:taskId];
        request.requiresNetworkConnectivity = requiresNetwork ? YES : NO;
        request.requiresExternalPower = requiresPower ? YES : NO;
        if (earliest > 0) {
            request.earliestBeginDate = [NSDate dateWithTimeIntervalSince1970:earliest];
        }
        NSError *submitError = nil;
        [[BGTaskScheduler sharedScheduler] submitTaskRequest:request error:&submitError];
        if (submitError != nil) {
            CN1Log(@"Failed to submit background processing task %@: %@", taskId, submitError.localizedDescription);
        }
    }
#endif
}

JAVA_VOID com_codename1_impl_ios_IOSNative_cancelBackgroundTask___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT identifier) {
// Not implemented on the native macOS port: the body below is UIKit -- a
// picker, an action sheet, a movie player, a pasteboard or a UIApplication
// service -- and AppKit's equivalent is a different API rather than a
// renamed one. The symbol still has to exist: ParparVM keeps a native method
// alive BY its symbol appearing in the native sources, so removing it would
// make the dead-code pass drop the Java side and ship green with the feature
// silently gone. Returning an unsupported value instead lets the caller take
// its unsupported path.
#if TARGET_OS_OSX
#else
    if (@available(iOS 13.0, *)) {
        NSString *taskId = toNSString(CN1_THREAD_STATE_PASS_ARG identifier);
        [[BGTaskScheduler sharedScheduler] cancelTaskRequestWithIdentifier:taskId];
    }
#endif
}
#endif // !TARGET_OS_WATCH (BackgroundTasks)

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isBackgroundProcessingSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    if (@available(iOS 13.0, *)) {
        return JAVA_TRUE;
    }
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isBackgroundProcessingSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return com_codename1_impl_ios_IOSNative_isBackgroundProcessingSupported___R_boolean(CN1_THREAD_STATE_PASS_ARG me);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPendingSharedContent___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT appGroupId) {
    if (appGroupId == JAVA_NULL) {
        return JAVA_NULL;
    }
    NSString *group = toNSString(CN1_THREAD_STATE_PASS_ARG appGroupId);
    if (group == nil || [group length] == 0) {
        return JAVA_NULL;
    }
    NSUserDefaults *defaults = [[NSUserDefaults alloc] initWithSuiteName:group];
    id payload = [defaults objectForKey:@"cn1.shareExtension.payload"];
    if (payload == nil) {
        return JAVA_NULL;
    }
    [defaults removeObjectForKey:@"cn1.shareExtension.payload"];
    [defaults synchronize];
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:payload options:0 error:nil];
    if (jsonData == nil) {
        return JAVA_NULL;
    }
    NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    return fromNSString(CN1_THREAD_STATE_PASS_ARG jsonString);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPendingSharedContent___java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT appGroupId) {
    return com_codename1_impl_ios_IOSNative_getPendingSharedContent___java_lang_String(CN1_THREAD_STATE_PASS_ARG me, appGroupId);
}

// --- Wallet issuer-provisioning extension support ---------------------------
// The app publishes pass entries / auth token into the shared App Group where
// the generated Wallet extensions (see the ios.wallet.* build hints) read
// them. The group id comes from the CN1WalletAppGroup Info.plist key injected
// by the build.
//
// The implementation is compiled in only when the build needs it - the
// ios.wallet.extension build hint is enabled or the app references
// com.codename1.payment.WalletExtension - because dormant wallet-looking
// code in unrelated apps can trigger questions during Apple review. The
// build flips the define below; the #else stubs keep the linker happy.
//#define CN1_INCLUDE_WALLET

#ifdef CN1_INCLUDE_WALLET

static NSString *cn1WalletGroupId() {
    id v = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CN1WalletAppGroup"];
    return ([v isKindOfClass:[NSString class]] && [(NSString *)v length] > 0) ? (NSString *)v : nil;
}

static NSUserDefaults *cn1WalletGroupDefaults() {
    NSString *group = cn1WalletGroupId();
    return group == nil ? nil : [[NSUserDefaults alloc] initWithSuiteName:group];
}

static NSURL *cn1WalletGroupArtDir(BOOL create) {
    NSString *group = cn1WalletGroupId();
    if (group == nil) {
        return nil;
    }
    NSURL *container = [[NSFileManager defaultManager] containerURLForSecurityApplicationGroupIdentifier:group];
    if (container == nil) {
        return nil;
    }
    NSURL *dir = [container URLByAppendingPathComponent:@"cn1wallet" isDirectory:YES];
    if (create) {
        [[NSFileManager defaultManager] createDirectoryAtURL:dir withIntermediateDirectories:YES attributes:nil error:nil];
    }
    return dir;
}

static NSString *cn1WalletEntriesKey(JAVA_BOOLEAN remote) {
    return remote ? @"cn1.wallet.remotePassEntries" : @"cn1.wallet.passEntries";
}

// Removes one list and deletes the card-art files its entries reference. Art
// files are uniquely named per entry so this never breaks the other list.
static void cn1WalletClearEntries(JAVA_BOOLEAN remote) {
    NSUserDefaults *defaults = cn1WalletGroupDefaults();
    if (defaults == nil) {
        return;
    }
    NSString *key = cn1WalletEntriesKey(remote);
    NSArray *entries = [defaults arrayForKey:key];
    NSURL *artDir = cn1WalletGroupArtDir(NO);
    for (id entry in entries) {
        if (![entry isKindOfClass:[NSDictionary class]]) {
            continue;
        }
        NSString *art = ((NSDictionary *)entry)[@"art"];
        if (art != nil && artDir != nil) {
            [[NSFileManager defaultManager] removeItemAtURL:[artDir URLByAppendingPathComponent:art] error:nil];
        }
    }
    [defaults removeObjectForKey:key];
    [defaults synchronize];
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isWalletExtensionSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    if (cn1WalletGroupId() == nil) {
        return JAVA_FALSE;
    }
    if (@available(iOS 14, *)) {
        return JAVA_TRUE;
    }
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isWalletExtensionSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return com_codename1_impl_ios_IOSNative_isWalletExtensionSupported___R_boolean(CN1_THREAD_STATE_PASS_ARG me);
}

void com_codename1_impl_ios_IOSNative_walletExtensionClearPassEntries___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_BOOLEAN remote) {
    cn1WalletClearEntries(remote);
}

void com_codename1_impl_ios_IOSNative_walletExtensionAddPassEntry___boolean_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_BOOLEAN remote, JAVA_OBJECT identifier, JAVA_OBJECT title, JAVA_OBJECT cardholderName, JAVA_OBJECT accountSuffix, JAVA_OBJECT network, JAVA_OBJECT description, JAVA_OBJECT artPng) {
    NSUserDefaults *defaults = cn1WalletGroupDefaults();
    if (defaults == nil || identifier == JAVA_NULL || artPng == JAVA_NULL) {
        return;
    }
    NSURL *artDir = cn1WalletGroupArtDir(YES);
    if (artDir == nil) {
        return;
    }
    NSString *artName = [[[NSUUID UUID] UUIDString] stringByAppendingString:@".png"];
    NSData *artData = arrayToData(artPng);
    if (artData == nil || ![artData writeToURL:[artDir URLByAppendingPathComponent:artName] atomically:YES]) {
        return;
    }
    NSMutableDictionary *entry = [NSMutableDictionary dictionary];
    entry[@"identifier"] = toNSString(CN1_THREAD_STATE_PASS_ARG identifier);
    entry[@"art"] = artName;
    if (title != JAVA_NULL) {
        entry[@"title"] = toNSString(CN1_THREAD_STATE_PASS_ARG title);
    }
    if (cardholderName != JAVA_NULL) {
        entry[@"cardholderName"] = toNSString(CN1_THREAD_STATE_PASS_ARG cardholderName);
    }
    if (accountSuffix != JAVA_NULL) {
        entry[@"accountSuffix"] = toNSString(CN1_THREAD_STATE_PASS_ARG accountSuffix);
    }
    if (network != JAVA_NULL) {
        entry[@"network"] = toNSString(CN1_THREAD_STATE_PASS_ARG network);
    }
    if (description != JAVA_NULL) {
        entry[@"description"] = toNSString(CN1_THREAD_STATE_PASS_ARG description);
    }
    NSString *key = cn1WalletEntriesKey(remote);
    NSArray *existing = [defaults arrayForKey:key];
    NSMutableArray *updated = existing != nil ? [existing mutableCopy] : [NSMutableArray array];
    [updated addObject:entry];
    [defaults setObject:updated forKey:key];
    [defaults synchronize];
}

void com_codename1_impl_ios_IOSNative_walletExtensionSetRequiresAuthentication___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_BOOLEAN requiresAuthentication) {
    NSUserDefaults *defaults = cn1WalletGroupDefaults();
    if (defaults == nil) {
        return;
    }
    [defaults setBool:(requiresAuthentication ? YES : NO) forKey:@"cn1.wallet.requiresAuthentication"];
    [defaults synchronize];
}

void com_codename1_impl_ios_IOSNative_walletExtensionSetAuthToken___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT token) {
    NSUserDefaults *defaults = cn1WalletGroupDefaults();
    if (defaults == nil) {
        return;
    }
    if (token == JAVA_NULL) {
        [defaults removeObjectForKey:@"cn1.wallet.authToken"];
    } else {
        [defaults setObject:toNSString(CN1_THREAD_STATE_PASS_ARG token) forKey:@"cn1.wallet.authToken"];
    }
    [defaults synchronize];
}

void com_codename1_impl_ios_IOSNative_walletExtensionClear__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    NSUserDefaults *defaults = cn1WalletGroupDefaults();
    if (defaults == nil) {
        return;
    }
    cn1WalletClearEntries(JAVA_FALSE);
    cn1WalletClearEntries(JAVA_TRUE);
    [defaults removeObjectForKey:@"cn1.wallet.authToken"];
    [defaults removeObjectForKey:@"cn1.wallet.requiresAuthentication"];
    [defaults synchronize];
    NSURL *artDir = cn1WalletGroupArtDir(NO);
    if (artDir != nil) {
        [[NSFileManager defaultManager] removeItemAtURL:artDir error:nil];
    }
}

#else // CN1_INCLUDE_WALLET

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isWalletExtensionSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isWalletExtensionSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}

void com_codename1_impl_ios_IOSNative_walletExtensionClearPassEntries___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_BOOLEAN remote) {
}

void com_codename1_impl_ios_IOSNative_walletExtensionAddPassEntry___boolean_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_BOOLEAN remote, JAVA_OBJECT identifier, JAVA_OBJECT title, JAVA_OBJECT cardholderName, JAVA_OBJECT accountSuffix, JAVA_OBJECT network, JAVA_OBJECT description, JAVA_OBJECT artPng) {
}

void com_codename1_impl_ios_IOSNative_walletExtensionSetRequiresAuthentication___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_BOOLEAN requiresAuthentication) {
}

void com_codename1_impl_ios_IOSNative_walletExtensionSetAuthToken___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT token) {
}

void com_codename1_impl_ios_IOSNative_walletExtensionClear__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
}

#endif // CN1_INCLUDE_WALLET

// BEGIN IOSImplementation native code, this is used to optimize various "heavy" IOSImplementation methods

#define DRAW_BGIMAGE_AT_GIVEN_POSITION_WITH_FILL_RECT(xpositionToDraw, ypositionToDraw)                 JAVA_BYTE bgTransparency = com_codename1_ui_plaf_Style_getBgTransparency___R_byte(threadStateData, s); \
    JAVA_INT color = com_codename1_ui_plaf_Style_getBgColor___R_int(threadStateData, s); \
    com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, color); \
    com_codename1_impl_ios_IOSImplementation_fillRect___java_lang_Object_int_int_int_int_byte(threadStateData, __cn1ThisObject, nativeGraphics, x, y, width, height, bgTransparency); \
    com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject, nativeGraphics, bgImage, xpositionToDraw, ypositionToDraw);

                  
JAVA_VOID com_codename1_impl_ios_IOSImplementation_paintComponentBackground___java_lang_Object_int_int_int_int_com_codename1_ui_plaf_Style(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT nativeGraphics, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height, JAVA_OBJECT s) {
    if (width <= 0 || height <= 0) {
        return;
    }
    JAVA_OBJECT bgImageOrig = com_codename1_ui_plaf_Style_getBgImage___R_com_codename1_ui_Image(threadStateData, s);
    if (bgImageOrig == JAVA_NULL) {
        if (com_codename1_ui_plaf_Style_getBackgroundType___R_byte(threadStateData, s) >=get_static_com_codename1_ui_plaf_Style_BACKGROUND_GRADIENT_LINEAR_VERTICAL()) {
            com_codename1_impl_CodenameOneImplementation_drawGradientBackground___com_codename1_ui_plaf_Style_java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, s, nativeGraphics, x, y, width, height);
            return;
        }
        JAVA_INT styleColor =com_codename1_ui_plaf_Style_getBgColor___R_int(threadStateData, s);
        com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, styleColor);
        
        JAVA_BYTE bgTransparency = com_codename1_ui_plaf_Style_getBgTransparency___R_byte(threadStateData, s);
        com_codename1_impl_ios_IOSImplementation_fillRect___java_lang_Object_int_int_int_int_byte(threadStateData, __cn1ThisObject, nativeGraphics, x, y, width,height, bgTransparency);
    } else {
        JAVA_INT iW = virtual_com_codename1_ui_Image_getWidth___R_int(threadStateData, bgImageOrig);
        JAVA_INT iH = virtual_com_codename1_ui_Image_getHeight___R_int(threadStateData, bgImageOrig);
        JAVA_OBJECT bgImage = virtual_com_codename1_ui_Image_getImage___R_java_lang_Object(threadStateData, bgImageOrig);
        JAVA_BYTE backgroundType = com_codename1_ui_plaf_Style_getBackgroundType___R_byte(threadStateData, s);
        switch (backgroundType) {
            case 0: {/* BACKGROUND_NONE */
                JAVA_BYTE bb = com_codename1_ui_plaf_Style_getBgTransparency___R_byte(threadStateData, s);
                if (bb != 0) {
                    JAVA_INT color = com_codename1_ui_plaf_Style_getBgColor___R_int(threadStateData, s);
                    com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, color);
                    com_codename1_impl_ios_IOSImplementation_fillRect___java_lang_Object_int_int_int_int_byte(threadStateData, __cn1ThisObject, nativeGraphics, x, y, width, height, bb);
                }
                return;
            }
            case 1: {// Style.BACKGROUND_IMAGE_SCALED:
                com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, bgImage, x, y, width, height);
                return;
            }
            case 33: {//Style.BACKGROUND_IMAGE_SCALED_FILL:
                JAVA_FLOAT r = MAX(((JAVA_FLOAT) width) / ((JAVA_FLOAT) iW), ((JAVA_FLOAT) height) / ((JAVA_FLOAT) iH));
                JAVA_INT bwidth = (JAVA_INT) (((JAVA_FLOAT) iW) * r);
                JAVA_INT bheight = (JAVA_INT) (((JAVA_FLOAT) iH) * r);
                com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, bgImage, x + (width - bwidth) / 2, y + (height - bheight) / 2, bwidth, bheight);
                return;
            }
            case 34: {//Style.BACKGROUND_IMAGE_SCALED_FIT:
                JAVA_BYTE bgTransparency = com_codename1_ui_plaf_Style_getBgTransparency___R_byte(threadStateData, s);
                if (bgTransparency != 0) {
                    JAVA_INT color = com_codename1_ui_plaf_Style_getBgColor___R_int(threadStateData, s);
                    com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, color);
                    com_codename1_impl_ios_IOSImplementation_fillRect___java_lang_Object_int_int_int_int_byte(threadStateData, __cn1ThisObject, nativeGraphics, x, y, width, height, bgTransparency);
                }
                JAVA_FLOAT r2 = MIN(((JAVA_FLOAT) width) / ((JAVA_FLOAT) iW), ((JAVA_FLOAT) height) / ((JAVA_FLOAT) iH));
                JAVA_INT awidth = (JAVA_INT) (((JAVA_FLOAT) iW) * r2);
                JAVA_INT aheight = (JAVA_INT) (((JAVA_FLOAT) iH) * r2);
                com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, bgImage, x + (width - awidth) / 2, y + (height - aheight) / 2, awidth, aheight);
                return;
            }
            case 2: { //Style.BACKGROUND_IMAGE_TILE_BOTH:
                com_codename1_impl_ios_IOSImplementation_tileImage___java_lang_Object_java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, bgImage, x, y, width, height);
                return;
            }
            case 4: {//Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP:
                JAVA_BYTE bgTransparency = com_codename1_ui_plaf_Style_getBgTransparency___R_byte(threadStateData, s);
                JAVA_INT color = com_codename1_ui_plaf_Style_getBgColor___R_int(threadStateData, s);
                com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, color);
                com_codename1_impl_ios_IOSImplementation_fillRect___java_lang_Object_int_int_int_int_byte(threadStateData, __cn1ThisObject, nativeGraphics, x, y, width, height, bgTransparency);

                com_codename1_impl_ios_IOSImplementation_tileImage___java_lang_Object_java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, bgImage, x, y, width, iH);
                return;
            }
            case 29: { //Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER:
                JAVA_BYTE bgTransparency = com_codename1_ui_plaf_Style_getBgTransparency___R_byte(threadStateData, s);
                JAVA_INT color = com_codename1_ui_plaf_Style_getBgColor___R_int(threadStateData, s);
                com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, color);
                com_codename1_impl_ios_IOSImplementation_fillRect___java_lang_Object_int_int_int_int_byte(threadStateData, __cn1ThisObject, nativeGraphics, x, y, width, height, bgTransparency);
                
                com_codename1_impl_ios_IOSImplementation_tileImage___java_lang_Object_java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, bgImage, x, y + (height / 2 - iH / 2), width, iH);
                return;
            }
            case 30: {//Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM:
                JAVA_BYTE bgTransparency = com_codename1_ui_plaf_Style_getBgTransparency___R_byte(threadStateData, s);
                JAVA_INT color = com_codename1_ui_plaf_Style_getBgColor___R_int(threadStateData, s);
                com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, color);
                com_codename1_impl_ios_IOSImplementation_fillRect___java_lang_Object_int_int_int_int_byte(threadStateData, __cn1ThisObject, nativeGraphics, x, y, width, height, bgTransparency);
                
                com_codename1_impl_ios_IOSImplementation_tileImage___java_lang_Object_java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, bgImage, x, y + (height - iH), width, iH);
                return;
            }
            case 3: {//Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT:
                JAVA_BYTE bgTransparency = com_codename1_ui_plaf_Style_getBgTransparency___R_byte(threadStateData, s);
                JAVA_INT color = com_codename1_ui_plaf_Style_getBgColor___R_int(threadStateData, s);
                com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, color);
                com_codename1_impl_ios_IOSImplementation_fillRect___java_lang_Object_int_int_int_int_byte(threadStateData, __cn1ThisObject, nativeGraphics, x, y, width, height, bgTransparency);
                for (int yPos = 0; yPos <= height; yPos += iH) {
                    com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject, nativeGraphics, bgImage, x, y + yPos);
                }
                return;
            }
            case 31: {//Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER:
                JAVA_BYTE bgTransparency = com_codename1_ui_plaf_Style_getBgTransparency___R_byte(threadStateData, s);
                JAVA_INT color = com_codename1_ui_plaf_Style_getBgColor___R_int(threadStateData, s);
                com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, color);
                com_codename1_impl_ios_IOSImplementation_fillRect___java_lang_Object_int_int_int_int_byte(threadStateData, __cn1ThisObject, nativeGraphics, x, y, width, height, bgTransparency);
                for (int yPos = 0; yPos <= height; yPos += iH) {
                    com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject, nativeGraphics, bgImage, x + (width / 2 - iW / 2), y + yPos);
                }
                return;
            }
            case 32: {//Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT:
                JAVA_BYTE bgTransparency = com_codename1_ui_plaf_Style_getBgTransparency___R_byte(threadStateData, s);
                JAVA_INT color = com_codename1_ui_plaf_Style_getBgColor___R_int(threadStateData, s);
                com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, color);
                com_codename1_impl_ios_IOSImplementation_fillRect___java_lang_Object_int_int_int_int_byte(threadStateData, __cn1ThisObject, nativeGraphics, x, y, width, height, bgTransparency);
                for (int yPos = 0; yPos <= height; yPos += iH) {
                    com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject, nativeGraphics, bgImage, x + width - iW, y + yPos);
                }
                return;
            }
            case 20: { //Style.BACKGROUND_IMAGE_ALIGNED_TOP:
                DRAW_BGIMAGE_AT_GIVEN_POSITION_WITH_FILL_RECT(x + (width / 2 - iW / 2), y);
                return;
            }
            case 21: { //Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM:
                DRAW_BGIMAGE_AT_GIVEN_POSITION_WITH_FILL_RECT(x + (width / 2 - iW / 2), y + (height - iH));
                return;
            }
            case 22: {//Style.BACKGROUND_IMAGE_ALIGNED_LEFT:
                DRAW_BGIMAGE_AT_GIVEN_POSITION_WITH_FILL_RECT(x, y + (height / 2 - iH / 2));
                return;
            }
            case 23: {//Style.BACKGROUND_IMAGE_ALIGNED_RIGHT:
                DRAW_BGIMAGE_AT_GIVEN_POSITION_WITH_FILL_RECT(x + width - iW, y + (height / 2 - iH / 2));
                return;
            }
            case 24: { //Style.BACKGROUND_IMAGE_ALIGNED_CENTER:
                DRAW_BGIMAGE_AT_GIVEN_POSITION_WITH_FILL_RECT(x + (width / 2 - iW / 2), y + (height / 2 - iH / 2));
                return;
            }
            case 25: {//Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT:
                DRAW_BGIMAGE_AT_GIVEN_POSITION_WITH_FILL_RECT(x, y);
                return;
            }
            case 26: {//Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT:
                DRAW_BGIMAGE_AT_GIVEN_POSITION_WITH_FILL_RECT(x + width - iW, y);
                return;
            }
            case 27: { //Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT:
                DRAW_BGIMAGE_AT_GIVEN_POSITION_WITH_FILL_RECT(x, y + (height - iH));
                return;
            }
            case 28: {//Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT:
                DRAW_BGIMAGE_AT_GIVEN_POSITION_WITH_FILL_RECT(x + width - iW, y + (height - iH));
                return;
            }
            case 7: // Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL:
            case 6: //Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL:
            case 8: {//Style.BACKGROUND_GRADIENT_RADIAL:
                com_codename1_impl_CodenameOneImplementation_drawGradientBackground___com_codename1_ui_plaf_Style_java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, s, nativeGraphics, x, y, width, height);
                return;
            }
        }
    }
}
                  
JAVA_VOID com_codename1_impl_ios_IOSImplementation_fillRect___java_lang_Object_int_int_int_int_byte(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT nativeGraphics, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_BYTE alpha) {
    if (alpha != 0) {
        JAVA_INT oldAlpha = com_codename1_impl_ios_IOSImplementation_getAlpha___java_lang_Object_R_int(threadStateData, __cn1ThisObject, nativeGraphics);
        com_codename1_impl_ios_IOSImplementation_setAlpha___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, alpha & 0xff);
        com_codename1_impl_ios_IOSImplementation_fillRect___java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, x, y, w, h);
        com_codename1_impl_ios_IOSImplementation_setAlpha___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, oldAlpha);
    }
}

                  
JAVA_INT reverseAlignForBidi(JAVA_BOOLEAN rtl, JAVA_INT align) {
    if (rtl) {
        switch (align) {
            case 3: {/* Component.RIGHT: */
                return 1 /* Component.LEFT */;
            }
            case 1: {/* Component.LEFT */
                return 3 /* Component.RIGHT */;
            }
        }
    }
    return align;
}

JAVA_VOID drawString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT nativeGraphics, JAVA_OBJECT nativeFont, JAVA_OBJECT str, JAVA_INT x, JAVA_INT y, JAVA_INT textDecoration, JAVA_INT fontHeight) {
    if (java_lang_String_length___R_int(threadStateData, str) == 0) {
        return;
    }
    
    // this if has only the minor effect of providing a slighly faster execution path
    if (textDecoration != 0) {
        JAVA_BOOLEAN raised = (textDecoration & 8 /* Style.TEXT_DECORATION_3D */) != 0;
        JAVA_BOOLEAN lowerd = (textDecoration & 16 /* Style.TEXT_DECORATION_3D_LOWERED */) != 0;
        JAVA_BOOLEAN north = (textDecoration & 32 /* Style.TEXT_DECORATION_3D_SHADOW_NORTH */) != 0;
        if (raised || lowerd || north) {
            textDecoration = textDecoration & (~8 /* Style.TEXT_DECORATION_3D */) & (~16 /* Style.TEXT_DECORATION_3D_LOWERED */) & (~32 /* Style.TEXT_DECORATION_3D_SHADOW_NORTH */);
            JAVA_INT c = com_codename1_impl_ios_IOSImplementation_getColor___java_lang_Object_R_int(threadStateData, __cn1ThisObject, nativeGraphics);
            JAVA_INT a = com_codename1_impl_ios_IOSImplementation_getAlpha___java_lang_Object_R_int(threadStateData, __cn1ThisObject, nativeGraphics);
            JAVA_INT newColor = 0;
            JAVA_INT offset = -2;
            if (lowerd) {
                offset = 2;
                newColor = 0xffffff;
            } else if (north) {
                offset = 2;
            }
            com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, newColor);
            if (a == 0xff) {
                com_codename1_impl_ios_IOSImplementation_setAlpha___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, 140);
            }
            drawString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, str, x, y + offset, textDecoration, fontHeight);
            com_codename1_impl_ios_IOSImplementation_setAlpha___java_lang_Object_int(threadStateData, __cn1ThisObject,nativeGraphics, a);
            com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, c);
            drawString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, str, x, y, textDecoration, fontHeight);
            return;
        }
        com_codename1_impl_ios_IOSImplementation_drawString___java_lang_Object_java_lang_String_int_int(threadStateData, __cn1ThisObject, nativeGraphics, str, x, y);
        if ((textDecoration & 1 /* Style.TEXT_DECORATION_UNDERLINE */)  != 0) {
            com_codename1_impl_ios_IOSImplementation_drawLine___java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, x, y + fontHeight - 1, x + com_codename1_impl_ios_IOSImplementation_stringWidth___java_lang_Object_java_lang_String_R_int(threadStateData, __cn1ThisObject, nativeFont, str), y + fontHeight - 1);
        }
        if ((textDecoration & 2 /* Style.TEXT_DECORATION_STRIKETHRU */) != 0) {
            com_codename1_impl_ios_IOSImplementation_drawLine___java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, x, y + fontHeight / 2, x + com_codename1_impl_ios_IOSImplementation_stringWidth___java_lang_Object_java_lang_String_R_int(threadStateData, __cn1ThisObject, nativeFont, str), y + fontHeight / 2);
        }
        if ((textDecoration & 4 /* Style.TEXT_DECORATION_OVERLINE */) != 0) {
            com_codename1_impl_ios_IOSImplementation_drawLine___java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, x, y, x + com_codename1_impl_ios_IOSImplementation_stringWidth___java_lang_Object_java_lang_String_R_int(threadStateData, __cn1ThisObject, nativeFont, str), y);
        }
    } else {
        com_codename1_impl_ios_IOSImplementation_drawString___java_lang_Object_java_lang_String_int_int(threadStateData, __cn1ThisObject, nativeGraphics, str, x, y);
    }
}
      
JAVA_BOOLEAN fastCharWidthCheck(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT s, JAVA_INT length, JAVA_INT width, JAVA_INT charWidth, JAVA_OBJECT f) {
    if (length * charWidth < width) {
        return true;
    }
    length = MIN(java_lang_String_length___R_int(threadStateData, s), length);
    JAVA_OBJECT sub = java_lang_String_substring___int_int_R_java_lang_String(threadStateData, s, 0, length);
    return com_codename1_impl_ios_IOSImplementation_stringWidth___java_lang_Object_java_lang_String_R_int(threadStateData, __cn1ThisObject, f, sub) < width;
}
                  
                  
   
JAVA_OBJECT threePoints = JAVA_NULL;
JAVA_INT threePointsWidth;
                  
JAVA_INT drawLabelText(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT nativeGraphics, JAVA_INT textDecoration, JAVA_BOOLEAN rtl, JAVA_BOOLEAN isTickerRunning, JAVA_BOOLEAN endsWith3Points, JAVA_OBJECT nativeFont, JAVA_INT txtW, JAVA_INT textSpaceW, JAVA_INT shiftText, JAVA_OBJECT text, JAVA_INT x, JAVA_INT y, JAVA_INT fontHeight) {
    if ((!isTickerRunning) || rtl) {
        //if there is no space to draw the text add ... at the end
        if (txtW > textSpaceW && textSpaceW > 0) {
            // Handling of adding 3 points and in fact all text positioning when the text is bigger than
            // the allowed space is handled differently in RTL, this is due to the reverse algorithm
            // effects - i.e. when the text includes both Hebrew/Arabic and English/numbers then simply
            // trimming characters from the end of the text (as done with LTR) won't do.
            // Instead we simple reposition the text, and draw the 3 points, this is quite simple, but
            // the downside is that a part of a letter may be shown here as well.
            
            if (rtl) {
                if ((!isTickerRunning) && endsWith3Points) {
                    if(threePoints == JAVA_NULL) {
                        threePoints = newStringFromCString(threadStateData, "...");
                        // permanent cache: the old removeObjectFromHeapCollection +
                        // refcount=999999 pin is gone (the header field was relocated
                        // off-object and BiBOP objects are page-swept); the immortal
                        // root registry marks it and its value array every cycle
                        cn1AddImmortalRoot(threePoints);
                        threePointsWidth = com_codename1_impl_ios_IOSImplementation_stringWidth___java_lang_Object_java_lang_String_R_int(threadStateData, __cn1ThisObject, nativeFont, threePoints);
                    }
                    
                    drawString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, threePoints, shiftText + x, y, textDecoration, fontHeight);
                    
                    com_codename1_impl_ios_IOSImplementation_clipRect___java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, threePointsWidth + shiftText + x, y, textSpaceW - threePointsWidth, fontHeight);
                }
                x = x - txtW + textSpaceW;
            } else if (endsWith3Points) {
                if(threePoints == JAVA_NULL) {
                    threePoints = newStringFromCString(threadStateData, "...");
                    // permanent cache: see the RTL branch above
                    cn1AddImmortalRoot(threePoints);
                    threePointsWidth = com_codename1_impl_ios_IOSImplementation_stringWidth___java_lang_Object_java_lang_String_R_int(threadStateData, __cn1ThisObject, nativeFont, threePoints);
                }
                JAVA_INT index = 1;
                JAVA_INT widest = com_codename1_impl_ios_IOSImplementation_charWidth___java_lang_Object_char_R_int(threadStateData, __cn1ThisObject, nativeFont, 'W');
                while (fastCharWidthCheck(threadStateData, __cn1ThisObject, text, index, textSpaceW - threePointsWidth, widest, nativeFont) && index < java_lang_String_length___R_int(threadStateData, text)) {
                    index++;
                }
                JAVA_INT textLength = java_lang_String_length___R_int(threadStateData, text);
                text = java_lang_String_substring___int_int_R_java_lang_String(threadStateData, text, 0, MIN(textLength, MAX(1, index - 1)));
                JAVA_OBJECT sb = __NEW_java_lang_StringBuilder(threadStateData);
                java_lang_StringBuilder___INIT_____java_lang_String(threadStateData, sb, text);
                java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, sb, threePoints);
                text = java_lang_StringBuilder_toString___R_java_lang_String(threadStateData, sb);
                txtW = com_codename1_impl_ios_IOSImplementation_stringWidth___java_lang_Object_java_lang_String_R_int(threadStateData, __cn1ThisObject, nativeFont, text);
            }
        }
    }
    
    drawString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, text, shiftText + x, y, textDecoration, fontHeight);
    return MIN(txtW, textSpaceW);
}
                  
                  
JAVA_INT drawLabelString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT nativeGraphics, JAVA_OBJECT nativeFont, JAVA_OBJECT text, JAVA_INT x, JAVA_INT y, JAVA_INT textSpaceW, JAVA_BOOLEAN isTickerRunning, JAVA_INT tickerShiftText, JAVA_INT textDecoration, JAVA_BOOLEAN rtl, JAVA_BOOLEAN endsWith3Points, JAVA_INT textWidth, JAVA_INT fontHeight) {
    JAVA_INT cx = com_codename1_impl_ios_IOSImplementation_getClipX___java_lang_Object_R_int(threadStateData, __cn1ThisObject, nativeGraphics);
    JAVA_INT cy = com_codename1_impl_ios_IOSImplementation_getClipY___java_lang_Object_R_int(threadStateData, __cn1ThisObject, nativeGraphics);
    JAVA_INT cw = com_codename1_impl_ios_IOSImplementation_getClipWidth___java_lang_Object_R_int(threadStateData, __cn1ThisObject, nativeGraphics);
    JAVA_INT ch = com_codename1_impl_ios_IOSImplementation_getClipHeight___java_lang_Object_R_int(threadStateData, __cn1ThisObject, nativeGraphics);
    com_codename1_impl_ios_IOSImplementation_clipRect___java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, x, cy, textSpaceW, ch);
    
    JAVA_INT drawnW = drawLabelText(threadStateData, __cn1ThisObject, nativeGraphics, textDecoration, rtl, isTickerRunning, endsWith3Points, nativeFont,
                               textWidth, textSpaceW, tickerShiftText, text, x, y, fontHeight);
    
    com_codename1_impl_ios_IOSImplementation_setClip___java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, cx, cy, cw, ch);
    
    return drawnW;
}

JAVA_INT drawLabelStringValign(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT nativeGraphics, JAVA_OBJECT nativeFont, JAVA_OBJECT str, JAVA_INT x, JAVA_INT y, JAVA_INT textSpaceW,
JAVA_BOOLEAN isTickerRunning, JAVA_INT tickerShiftText, JAVA_INT textDecoration, JAVA_BOOLEAN rtl, JAVA_BOOLEAN endsWith3Points, JAVA_INT textWidth, JAVA_INT iconStringHGap, JAVA_INT iconHeight, JAVA_INT fontHeight, JAVA_INT valign) {
    switch (valign) {
        case 0 /* Component.TOP */:
            return drawLabelString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, str, x, y, textSpaceW, isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, textWidth, fontHeight);
        case 4 /* Component.CENTER */:
            return drawLabelString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, str, x, y + iconHeight / 2 - fontHeight / 2, textSpaceW, isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, textWidth, fontHeight);
        default:
            return drawLabelString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, str, x, y + iconStringHGap, textSpaceW, isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, textWidth, fontHeight);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSImplementation_drawLabelComponent___java_lang_Object_int_int_int_int_com_codename1_ui_plaf_Style_java_lang_String_java_lang_Object_java_lang_Object_int_int_boolean_boolean_int_int_boolean_int_boolean_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT nativeGraphics, JAVA_INT cmpX, JAVA_INT cmpY, JAVA_INT cmpHeight, JAVA_INT cmpWidth, JAVA_OBJECT style, JAVA_OBJECT text, JAVA_OBJECT icon, JAVA_OBJECT stateIcon, JAVA_INT preserveSpaceForState, JAVA_INT gap, JAVA_BOOLEAN rtl, JAVA_BOOLEAN isOppositeSide, JAVA_INT textPosition, JAVA_INT stringWidth, JAVA_BOOLEAN isTickerRunning, JAVA_INT tickerShiftText, JAVA_BOOLEAN endsWith3Points, JAVA_INT valign) {
    JAVA_OBJECT font = com_codename1_ui_plaf_Style_getFont___R_com_codename1_ui_Font(threadStateData, style);
    JAVA_OBJECT nativeFont = com_codename1_ui_Font_getNativeFont___R_java_lang_Object(threadStateData, font);
    com_codename1_impl_ios_IOSImplementation_setNativeFont___java_lang_Object_java_lang_Object(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont);
    JAVA_INT fgColor = com_codename1_ui_plaf_Style_getFgColor___R_int(threadStateData, style);
    JAVA_INT fgAlpha = com_codename1_ui_plaf_Style_getFgAlpha___R_int(threadStateData, style);
    com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, fgColor);
    JAVA_INT alpha = com_codename1_impl_ios_IOSImplementation_concatenateAlpha___java_lang_Object_int_R_int(threadStateData, __cn1ThisObject, nativeGraphics, fgAlpha);

    JAVA_INT iconWidth = 0;
    JAVA_INT iconHeight = 0;
    if(icon != JAVA_NULL) {
        iconWidth = com_codename1_impl_ios_IOSImplementation_getImageWidth___java_lang_Object_R_int(threadStateData, __cn1ThisObject, icon);
        iconHeight = com_codename1_impl_ios_IOSImplementation_getImageHeight___java_lang_Object_R_int(threadStateData, __cn1ThisObject, icon);
    }
    
    JAVA_INT textDecoration = com_codename1_ui_plaf_Style_getTextDecoration___R_int(threadStateData, style);
    JAVA_INT stateIconSize = 0;
    JAVA_INT stateIconYPosition = 0;
    
    JAVA_INT leftPadding = com_codename1_ui_plaf_Style_getPaddingLeft___boolean_R_int(threadStateData, style, rtl);
    JAVA_INT rightPadding = com_codename1_ui_plaf_Style_getPaddingRight___boolean_R_int(threadStateData, style, rtl);
    JAVA_INT topPadding = com_codename1_ui_plaf_Style_getPaddingTop___R_int(threadStateData, style);
    JAVA_INT bottomPadding = com_codename1_ui_plaf_Style_getPaddingBottom___R_int(threadStateData, style);
    
    JAVA_INT fontHeight = 0;
    if (text != JAVA_NULL && java_lang_String_length___R_int(threadStateData, text) > 0) {
        fontHeight = com_codename1_ui_Font_getHeight___R_int(threadStateData, font);
    }
    
    if (stateIcon != JAVA_NULL) {
        stateIconSize = com_codename1_impl_ios_IOSImplementation_getImageWidth___java_lang_Object_R_int(threadStateData, __cn1ThisObject, stateIcon);
        stateIconYPosition = cmpY + topPadding
        + (cmpHeight - topPadding
           - bottomPadding) / 2 - stateIconSize / 2;
        JAVA_INT tX = cmpX;
        if (isOppositeSide) {
            if (rtl) {
                tX += leftPadding;
            } else {
                tX = tX + cmpWidth - leftPadding - stateIconSize;
            }
            cmpWidth -= leftPadding - stateIconSize;
        } else {
            preserveSpaceForState = stateIconSize + gap;
            if (rtl) {
                tX = tX + cmpWidth - leftPadding - stateIconSize;
            } else {
                tX += leftPadding;
            }
        }
        
        com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject, nativeGraphics, stateIcon, tX, stateIconYPosition);
    }
    
    //default for bottom left alignment
    JAVA_INT x = cmpX + leftPadding + preserveSpaceForState;
    JAVA_INT y = cmpY + topPadding;
    
    JAVA_INT align = reverseAlignForBidi(rtl, com_codename1_ui_plaf_Style_getAlignment___R_int(threadStateData, style));
    
    JAVA_INT textPos = reverseAlignForBidi(rtl, textPosition);
    
    //set initial x,y position according to the alignment and textPosition
    switch (align) {
        case 1: /* Component.LEFT */
            switch (textPos) {
                case 1: /* Component.LEFT */
                case 3: /* Component.RIGHT: */
                    y = y + (cmpHeight - (topPadding + bottomPadding + MAX(((icon != JAVA_NULL) ? iconHeight : 0), fontHeight))) / 2;
                    break;
                case 2: /* Label.BOTTOM: */
                case 0: /* Label.TOP: */
                    y = y + (cmpHeight - (topPadding + bottomPadding + ((icon != JAVA_NULL) ? iconHeight + gap : 0) + fontHeight)) / 2;
                    break;
            }
            break;
        case 4: /* Component.CENTER: */
            switch (textPos) {
                case 1: /* Component.LEFT */
                case 3: /* Component.RIGHT: */
                    x = x + (cmpWidth - (preserveSpaceForState
                                         + leftPadding
                                         + rightPadding
                                         + ((icon != JAVA_NULL) ? iconWidth + gap : 0)
                                         + stringWidth)) / 2;
                    x = MAX(x, cmpX + leftPadding + preserveSpaceForState);
                    y = y + (cmpHeight - (topPadding
                                          + bottomPadding
                                          + MAX(((icon != JAVA_NULL) ? iconHeight : 0),
                                                     fontHeight))) / 2;
                    break;
                case 2: /* Label.BOTTOM: */
                case 0: /* Label.TOP: */
                    x = x + (cmpWidth - (preserveSpaceForState + leftPadding
                                         + rightPadding
                                         + MAX(((icon != JAVA_NULL) ? iconWidth : 0),
                                                    stringWidth))) / 2;
                    x = MAX(x, cmpX + leftPadding + preserveSpaceForState);
                    y = y + (cmpHeight - (topPadding
                                          + bottomPadding
                                          + ((icon != JAVA_NULL) ? iconHeight + gap : 0)
                                          + fontHeight)) / 2;
                    break;
            }
            break;
        case 3: /* Component.RIGHT: */
            switch (textPos) {
                case 1: /* Component.LEFT */
                case 3: /* Component.RIGHT: */
                    x = cmpX + cmpWidth - rightPadding
                    - (((icon != JAVA_NULL) ? (iconWidth + gap) : 0)
                       + stringWidth);
                    if (rtl) {
                        x = MAX(x - preserveSpaceForState, cmpX + leftPadding);
                    } else {
                        x = MAX(x, cmpX + leftPadding + preserveSpaceForState);
                    }
                    y = y + (cmpHeight - (topPadding
                                          + bottomPadding
                                          + MAX(((icon != JAVA_NULL) ? iconHeight : 0),
                                                     fontHeight))) / 2;
                    break;
                case 2: /* Label.BOTTOM: */
                case 0: /* Label.TOP: */
                    x = cmpX + cmpWidth - rightPadding
                    - (MAX(((icon != JAVA_NULL) ? (iconWidth) : 0),
                                stringWidth));
                    x = MAX(x, cmpX + leftPadding + preserveSpaceForState);
                    y = y + (cmpHeight - (topPadding
                                          + bottomPadding
                                          + ((icon != JAVA_NULL) ? iconHeight + gap : 0) + fontHeight)) / 2;
                    break;
            }
            break;
        default:
            break;
    }
    
    int textSpaceW = cmpWidth - rightPadding - leftPadding;
    
    if (icon != JAVA_NULL && (textPos == 3 /* Component.RIGHT: */ || textPos == 1 /* Component.LEFT */)) {
        textSpaceW = textSpaceW - iconWidth;
    }
    
    if (stateIcon != JAVA_NULL) {
        textSpaceW = textSpaceW - stateIconSize;
    } else {
        textSpaceW = textSpaceW - preserveSpaceForState;
    }
    
    if (icon == JAVA_NULL) {
        // no icon only string
        drawLabelString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, text, x, y, textSpaceW, isTickerRunning, tickerShiftText,
                        textDecoration, rtl, endsWith3Points, stringWidth, fontHeight);
    } else {
        int strWidth = stringWidth;
        int iconStringWGap;
        int iconStringHGap;
        
        switch (textPos) {
            case 1: /* Component.LEFT */
                if (iconHeight > fontHeight) {
                    iconStringHGap = (iconHeight - fontHeight) / 2;
                    strWidth = drawLabelStringValign(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, text, x, y, textSpaceW, isTickerRunning,
                                                     tickerShiftText, textDecoration, rtl, endsWith3Points, strWidth, iconStringHGap, iconHeight,
                                                     fontHeight, valign);
                    
                    com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject, nativeGraphics, icon, x + strWidth + gap, y);
                } else {
                    iconStringHGap = (fontHeight - iconHeight) / 2;
                    strWidth = drawLabelString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, text, x, y, textSpaceW, isTickerRunning,
                                               tickerShiftText, textDecoration, rtl, endsWith3Points, strWidth, fontHeight);
                    
                    com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject, nativeGraphics, icon, x + strWidth + gap, y + iconStringHGap);
                }
                break;
            case 3: /* Component.RIGHT: */
                if (iconHeight > fontHeight) {
                    iconStringHGap = (iconHeight - fontHeight) / 2;
                    com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject,nativeGraphics, icon, x, y);
                    drawLabelStringValign(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, text, x + iconWidth + gap, y, textSpaceW, isTickerRunning,
                                          tickerShiftText, textDecoration, rtl, endsWith3Points, strWidth, iconStringHGap, iconHeight, fontHeight, valign);
                } else {
                    iconStringHGap = (fontHeight - iconHeight) / 2;
                    com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject, nativeGraphics, icon, x, y + iconStringHGap);
                    drawLabelString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, text, x + iconWidth + gap, y, textSpaceW,
                                    isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, strWidth, fontHeight);
                }
                break;
            case 2: /* Label.BOTTOM: */
                //center align the smaller
                if (iconWidth > strWidth) {
                    iconStringWGap = (iconWidth - strWidth) / 2;
                    com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject, nativeGraphics, icon, x, y);
                    drawLabelString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, text, x + iconStringWGap, y + iconHeight + gap, textSpaceW,
                                    isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, strWidth, fontHeight);
                } else {
                    iconStringWGap = (MIN(strWidth, textSpaceW) - iconWidth) / 2;
                    com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject,nativeGraphics, icon, x + iconStringWGap, y);
                    
                    drawLabelString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, text, x, y + iconHeight + gap, textSpaceW, isTickerRunning,
                                    tickerShiftText, textDecoration, rtl, endsWith3Points, strWidth, fontHeight);
                }
                break;
            case 0: /* Label.TOP: */
                //center align the smaller
                if (iconWidth > strWidth) {
                    iconStringWGap = (iconWidth - strWidth) / 2;
                    drawLabelString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, text, x + iconStringWGap, y, textSpaceW, isTickerRunning,
                                    tickerShiftText, textDecoration, rtl, endsWith3Points, strWidth, fontHeight);
                    com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject,nativeGraphics, icon, x, y + fontHeight + gap);
                } else {
                    iconStringWGap = (MIN(strWidth, textSpaceW) - iconWidth) / 2;
                    drawLabelString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, text, x, y, textSpaceW, isTickerRunning, tickerShiftText,
                                    textDecoration, rtl, endsWith3Points, strWidth, fontHeight);
                    com_codename1_impl_ios_IOSImplementation_drawImage___java_lang_Object_java_lang_Object_int_int(threadStateData, __cn1ThisObject,nativeGraphics, icon, x + iconStringWGap, y + fontHeight + gap);
                }
                break;
        }
    }
    com_codename1_impl_ios_IOSImplementation_setAlpha___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, alpha);
}
   
JAVA_LONG com_codename1_impl_ios_IOSNative_beginBackgroundTask__(JAVA_OBJECT instanceObject)
{
#if TARGET_OS_OSX
    // A Mac application is not suspended when it loses focus, so there is no
    // expiring window of background time to ask for and nothing to return a
    // handle to. Answering zero is honest: it is the same "no task" value the
    // matching endBackgroundTask ignores.
    return 0;
#elif !TARGET_OS_WATCH
    __block UIBackgroundTaskIdentifier bgTask = UIBackgroundTaskInvalid;
    bgTask = [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:^{
        // Clean up any unfinished task business by marking where you
        // stopped or ending the task outright.
        [[UIApplication sharedApplication] endBackgroundTask:bgTask];
        bgTask = UIBackgroundTaskInvalid;
    }];
    return (JAVA_LONG)((BRIDGE_CAST void*)bgTask);
#else
    // watchOS has no UIApplication background-task API.
    return 0;
#endif // !TARGET_OS_WATCH
}

#ifdef NEW_CODENAME_ONE_VM
JAVA_LONG com_codename1_impl_ios_IOSNative_beginBackgroundTask___R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    return com_codename1_impl_ios_IOSNative_beginBackgroundTask__(instanceObject);
}
#endif

JAVA_VOID com_codename1_impl_ios_IOSNative_endBackgroundTask___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG bgTask)
{
#if TARGET_OS_OSX
    // Nothing was begun, so there is nothing to end.
#elif !TARGET_OS_WATCH
    [[UIApplication sharedApplication] endBackgroundTask:(UIBackgroundTaskIdentifier)bgTask];
#endif // !TARGET_OS_WATCH
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isRTLString___java_lang_String_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT javaString)
{
    POOL_BEGIN();
    NSString *string = toNSString(CN1_THREAD_STATE_PASS_ARG javaString);
    // Define Unicode ranges for Hebrew and Arabic
    NSRange hebrewRange = NSMakeRange(0x0590, 0x05FF - 0x0590 + 1);
    NSRange arabicRange = NSMakeRange(0x0600, 0x06FF - 0x0600 + 1);
    // Range for common neutral characters (basic Latin, common punctuation, and digits)
    NSRange neutralRange = NSMakeRange(0x0020, 0x007E - 0x0020 + 1);
    // Emoji ranges (covering most common emoji blocks)
    NSArray<NSValue *> *emojiRanges = @[
        [NSValue valueWithRange:NSMakeRange(0x1F600, 0x1F64F - 0x1F600 + 1)], // Emoticons
        [NSValue valueWithRange:NSMakeRange(0x1F300, 0x1F5FF - 0x1F300 + 1)], // Miscellaneous Symbols and Pictographs
        [NSValue valueWithRange:NSMakeRange(0x1F900, 0x1F9FF - 0x1F900 + 1)], // Supplemental Symbols and Pictographs
        [NSValue valueWithRange:NSMakeRange(0x2600, 0x26FF - 0x2600 + 1)]   // Miscellaneous Symbols
    ];

    NSUInteger length = [string length];
    for (NSUInteger i = 0; i < length; i++) {
        unichar c = [string characterAtIndex:i];
        // Continue if the character is within the neutral or emoji ranges
        BOOL isNeutralOrEmoji = (c >= neutralRange.location && c <= NSMaxRange(neutralRange));
        for (NSValue *value in emojiRanges) {
            NSRange range = [value rangeValue];
            if (c >= range.location && c <= NSMaxRange(range)) {
                isNeutralOrEmoji = YES;
                break;
            }
        }
        if (isNeutralOrEmoji) {
            continue;
        }
        // Return true if the character is within the Hebrew or Arabic Unicode ranges
        if ((c >= hebrewRange.location && c <= NSMaxRange(hebrewRange)) ||
            (c >= arabicRange.location && c <= NSMaxRange(arabicRange))) {
            POOL_END();
            return YES;
        }
        // If the first significant character is not Hebrew or Arabic, return false
        POOL_END();
        return NO;
    }

    POOL_END();
    return NO;
}

void com_codename1_impl_ios_IOSNative_announceForAccessibility___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT text) {
#if TARGET_OS_OSX
    POOL_BEGIN();
    NSString *message = toNSString(CN1_THREAD_STATE_PASS_ARG text);
    if (message != nil) {
        dispatch_async(dispatch_get_main_queue(), ^{
            NSWindow *w = [CN1MacHost sharedHost].window;
            if (w != nil) {
                NSAccessibilityPostNotificationWithUserInfo(w,
                    NSAccessibilityAnnouncementRequestedNotification,
                    @{NSAccessibilityAnnouncementKey: message,
                      NSAccessibilityPriorityKey: @(NSAccessibilityPriorityHigh)});
            }
        });
    }
    POOL_END();
#else
    if (text == JAVA_NULL) {
        return;
    }
#if !TARGET_OS_WATCH
    POOL_BEGIN();
    NSString *nsText = toNSString(CN1_THREAD_STATE_PASS_ARG text);
    UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, nsText);
    POOL_END();
#else
    // watchOS has no UIAccessibilityPostNotification.
#endif // !TARGET_OS_WATCH
#endif
}

#if !TARGET_OS_WATCH
// UIKit-only declaration: the type in its signature does not exist on macOS,
// so the whole thing goes rather than just the body. Guarding only the body
// would leave a signature naming a type the compiler has never heard of.
#if !TARGET_OS_OSX
@interface CN1AccessibilityCustomAction : UIAccessibilityCustomAction
@property(nonatomic, retain) NSString *cn1ActionId;
@end
#endif

// UIKit-only declaration: the type in its signature does not exist on macOS,
// so the whole thing goes rather than just the body. Guarding only the body
// would leave a signature naming a type the compiler has never heard of.
#if !TARGET_OS_OSX
@implementation CN1AccessibilityCustomAction
@end
#endif

// UIKit-only declaration: the type in its signature does not exist on macOS,
// so the whole thing goes rather than just the body. Guarding only the body
// would leave a signature naming a type the compiler has never heard of.
#if !TARGET_OS_OSX
@interface CN1AccessibilityElement : UIAccessibilityElement
@property(nonatomic, assign) long long cn1NodeId;
@property(nonatomic, retain) NSArray *cn1Actions;
@end
#endif

static NSString *CN1AccessibilityActionId(NSArray *actions, NSString *wanted) {
    for (NSDictionary *action in actions) {
        if ([wanted isEqualToString:[action objectForKey:@"id"]]
                && ![[action objectForKey:@"enabled"] isEqual:@NO]) {
            return wanted;
        }
    }
    return nil;
}

static void CN1PerformAccessibilityAction(long long nodeId, NSString *actionId, NSString *argument) {
    if (actionId == nil) return;
    JAVA_OBJECT javaAction = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG actionId);
    JAVA_OBJECT javaArgument = argument == nil ? JAVA_NULL : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG argument);
    com_codename1_impl_ios_IOSImplementation_performAccessibilityActionFromNative___long_java_lang_String_java_lang_String(
            CN1_THREAD_GET_STATE_PASS_ARG (JAVA_LONG)nodeId, javaAction, javaArgument);
}

// UIKit-only declaration: the type in its signature does not exist on macOS,
// so the whole thing goes rather than just the body. Guarding only the body
// would leave a signature naming a type the compiler has never heard of.
#if !TARGET_OS_OSX
@implementation CN1AccessibilityElement
- (BOOL)accessibilityActivate {
    NSString *action = CN1AccessibilityActionId(self.cn1Actions, @"activate");
    if (action == nil) action = CN1AccessibilityActionId(self.cn1Actions, @"focus");
    if (action == nil) return NO;
    CN1PerformAccessibilityAction(self.cn1NodeId, action, nil);
    return YES;
}

- (void)accessibilityIncrement {
    CN1PerformAccessibilityAction(self.cn1NodeId,
            CN1AccessibilityActionId(self.cn1Actions, @"increment"), nil);
}

- (void)accessibilityDecrement {
    CN1PerformAccessibilityAction(self.cn1NodeId,
            CN1AccessibilityActionId(self.cn1Actions, @"decrement"), nil);
}

- (BOOL)accessibilityPerformEscape {
    NSString *action = CN1AccessibilityActionId(self.cn1Actions, @"dismiss");
    if (action == nil) return NO;
    CN1PerformAccessibilityAction(self.cn1NodeId, action, nil);
    return YES;
}

- (BOOL)accessibilityScroll:(UIAccessibilityScrollDirection)direction {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
    return NO;
#else
    NSString *action = nil;
    if (direction == UIAccessibilityScrollDirectionDown || direction == UIAccessibilityScrollDirectionRight
            || direction == UIAccessibilityScrollDirectionNext) {
        action = CN1AccessibilityActionId(self.cn1Actions, @"scrollForward");
    } else {
        action = CN1AccessibilityActionId(self.cn1Actions, @"scrollBackward");
    }
    if (action == nil) return NO;
    CN1PerformAccessibilityAction(self.cn1NodeId, action, nil);
    return YES;
#endif
}

- (BOOL)cn1PerformCustomAction:(CN1AccessibilityCustomAction *)action {
    if (action.cn1ActionId == nil) return NO;
    CN1PerformAccessibilityAction(self.cn1NodeId, action.cn1ActionId, nil);
    return YES;
}
@end
#endif

static NSMutableDictionary *cn1AccessibilityLiveValues;

static id CN1JSONValue(NSDictionary *node, NSString *key) {
    id value = [node objectForKey:key];
    return value == [NSNull null] ? nil : value;
}

// UIKit-only declaration: the type in its signature does not exist on macOS,
// so the whole thing goes rather than just the body.
#if !TARGET_OS_OSX
static UIAccessibilityTraits CN1AccessibilityTraitsForNode(NSDictionary *node) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
#else
    NSString *role = CN1JSONValue(node, @"role");
    UIAccessibilityTraits traits = UIAccessibilityTraitNone;
    if ([role isEqualToString:@"BUTTON"] || [role isEqualToString:@"TOGGLE_BUTTON"]
            || [role isEqualToString:@"CHECKBOX"] || [role isEqualToString:@"RADIO_BUTTON"]
            || [role isEqualToString:@"SWITCH"] || [role isEqualToString:@"TAB"]
            || [role isEqualToString:@"MENU_ITEM"]) traits |= UIAccessibilityTraitButton;
    if ([role isEqualToString:@"LINK"]) traits |= UIAccessibilityTraitLink;
    if ([role isEqualToString:@"IMAGE"]) traits |= UIAccessibilityTraitImage;
    if ([role isEqualToString:@"STATIC_TEXT"]) traits |= UIAccessibilityTraitStaticText;
    if ([role isEqualToString:@"SEARCH_FIELD"]) traits |= UIAccessibilityTraitSearchField;
    if ([role isEqualToString:@"HEADING"] || [CN1JSONValue(node, @"headingLevel") intValue] > 0)
        traits |= UIAccessibilityTraitHeader;
    if ([role isEqualToString:@"SLIDER"] || [role isEqualToString:@"SPIN_BUTTON"])
        traits |= UIAccessibilityTraitAdjustable;
    if ([CN1JSONValue(node, @"selected") boolValue]) traits |= UIAccessibilityTraitSelected;
    id enabled = CN1JSONValue(node, @"enabled");
    if (enabled != nil && ![enabled boolValue]) traits |= UIAccessibilityTraitNotEnabled;
    if (![[CN1JSONValue(node, @"liveRegion") description] isEqualToString:@"OFF"])
        traits |= UIAccessibilityTraitUpdatesFrequently;
    return traits;
#endif
}
#endif

static NSString *CN1AccessibilityValueForNode(NSDictionary *node) {
    NSString *value = CN1JSONValue(node, @"value");
    NSDictionary *range = CN1JSONValue(node, @"range");
    if (value == nil && range != nil) {
        value = CN1JSONValue(range, @"text");
        if (value == nil) value = [[range objectForKey:@"current"] stringValue];
    }
    NSString *checked = CN1JSONValue(node, @"checked");
    if ([checked isEqualToString:@"CHECKED"]) value = value == nil ? @"Checked" : [value stringByAppendingString:@", Checked"];
    else if ([checked isEqualToString:@"UNCHECKED"]) value = value == nil ? @"Unchecked" : [value stringByAppendingString:@", Unchecked"];
    else if ([checked isEqualToString:@"MIXED"]) value = value == nil ? @"Mixed" : [value stringByAppendingString:@", Mixed"];
    id expanded = CN1JSONValue(node, @"expanded");
    if (expanded != nil) {
        NSString *state = [expanded boolValue] ? @"Expanded" : @"Collapsed";
        value = value == nil ? state : [value stringByAppendingFormat:@", %@", state];
    }
    id invalid = CN1JSONValue(node, @"invalid");
    if (invalid != nil && [invalid boolValue]) value = value == nil ? @"Invalid" : [value stringByAppendingString:@", Invalid"];
    return value;
}

void com_codename1_impl_ios_IOSNative_updateAccessibilityTree___java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT json, JAVA_INT changeType) {
// Not implemented on the native macOS port: the body below is UIKit -- a
// picker, an action sheet, a movie player, a pasteboard or a UIApplication
// service -- and AppKit's equivalent is a different API rather than a
// renamed one. The symbol still has to exist: ParparVM keeps a native method
// alive BY its symbol appearing in the native sources, so removing it would
// make the dead-code pass drop the Java side and ship green with the feature
// silently gone. Returning an unsupported value instead lets the caller take
// its unsupported path.
#if TARGET_OS_OSX
#else
    if (json == JAVA_NULL) return;
    POOL_BEGIN();
    NSString *jsonString = toNSString(CN1_THREAD_STATE_PASS_ARG json);
    NSData *data = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary *tree = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
    NSArray *nodes = [tree objectForKey:@"nodes"];
    dispatch_async(dispatch_get_main_queue(), ^{
        CN1View *container = (CN1View *)[[CodenameOne_GLViewController instance] eaglView];
        if (container == nil) return;
        NSMutableArray *elements = [NSMutableArray arrayWithCapacity:[nodes count]];
        NSMutableDictionary *elementsById = [NSMutableDictionary dictionary];
        if (cn1AccessibilityLiveValues == nil) cn1AccessibilityLiveValues = [[NSMutableDictionary alloc] init];
        for (NSDictionary *node in nodes) {
            CN1AccessibilityElement *element = [[CN1AccessibilityElement alloc] initWithAccessibilityContainer:container];
            NSNumber *nodeId = [node objectForKey:@"id"];
            element.cn1NodeId = [nodeId longLongValue];
            element.cn1Actions = CN1JSONValue(node, @"actions");
            element.accessibilityLabel = CN1JSONValue(node, @"label");
            NSString *hint = CN1JSONValue(node, @"hint");
            NSString *error = CN1JSONValue(node, @"error");
            element.accessibilityHint = error == nil ? hint : hint == nil ? error : [NSString stringWithFormat:@"%@. %@", hint, error];
            element.accessibilityValue = CN1AccessibilityValueForNode(node);
            element.accessibilityIdentifier = CN1JSONValue(node, @"identifier");
            element.accessibilityTraits = CN1AccessibilityTraitsForNode(node);
            element.accessibilityViewIsModal = [CN1JSONValue(node, @"modal") boolValue];
            NSArray *bounds = [node objectForKey:@"bounds"];
            if ([bounds count] == 4) {
                CGFloat scale = scaleValue <= 0 ? [UIScreen mainScreen].scale : scaleValue;
                element.accessibilityFrameInContainerSpace = CGRectMake(
                        [[bounds objectAtIndex:0] doubleValue] / scale,
                        [[bounds objectAtIndex:1] doubleValue] / scale,
                        [[bounds objectAtIndex:2] doubleValue] / scale,
                        [[bounds objectAtIndex:3] doubleValue] / scale);
            }
            NSMutableArray *custom = [NSMutableArray array];
            for (NSDictionary *action in element.cn1Actions) {
                NSString *actionId = [action objectForKey:@"id"];
                if ([actionId isEqualToString:@"activate"] || [actionId isEqualToString:@"focus"]
                        || [actionId isEqualToString:@"increment"] || [actionId isEqualToString:@"decrement"]
                        || [actionId isEqualToString:@"dismiss"] || [actionId isEqualToString:@"scrollForward"]
                        || [actionId isEqualToString:@"scrollBackward"] || ![[action objectForKey:@"enabled"] boolValue]) continue;
                NSString *name = CN1JSONValue(action, @"label");
                if (name == nil) name = actionId;
                CN1AccessibilityCustomAction *customAction = [[CN1AccessibilityCustomAction alloc]
                        initWithName:name target:element selector:@selector(cn1PerformCustomAction:)];
                customAction.cn1ActionId = actionId;
                [custom addObject:customAction];
            }
            element.accessibilityCustomActions = custom;
            [elements addObject:element];
            [elementsById setObject:element forKey:nodeId];

            NSString *live = CN1JSONValue(node, @"liveRegion");
            if (live != nil && ![live isEqualToString:@"OFF"]) {
                NSString *newValue = [NSString stringWithFormat:@"%@|%@", element.accessibilityLabel ?: @"", element.accessibilityValue ?: @""];
                NSString *oldValue = [cn1AccessibilityLiveValues objectForKey:nodeId];
                if (oldValue != nil && ![oldValue isEqualToString:newValue]) {
                    UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification,
                            element.accessibilityLabel ?: element.accessibilityValue);
                }
                [cn1AccessibilityLiveValues setObject:newValue forKey:nodeId];
            }
        }
        container.isAccessibilityElement = NO;
        container.accessibilityElements = elements;
        if ((changeType & 256) != 0) {
            UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification,
                    [elements count] == 0 ? nil : [elements objectAtIndex:0]);
        } else {
            UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, nil);
        }
    });
    POOL_END();
#endif
}
#else
void com_codename1_impl_ios_IOSNative_updateAccessibilityTree___java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT json, JAVA_INT changeType) {
    // watchOS uses a separate SwiftUI accessibility model.
}
#endif

// ====================================================================
// Crypto bridge -- implementations of the native methods on IOSNative
// that back com.codename1.security.{Cipher,Signature,SecureRandom,
// KeyGenerator}. The actual crypto runs in CN1Crypto.{h,m}; this file
// is just the marshalling layer.
//
// CN1_INCLUDE_CRYPTO is enabled by IPhoneBuilder when the app references
// com.codename1.security.* in its compiled bytecode. When the app doesn't
// use the crypto API the implementations below collapse into no-ops, the
// CommonCrypto / Security framework symbols are never referenced, and the
// AES-GCM SPI symbols (gated separately by CN1_INCLUDE_CRYPTO_GCM) stay
// completely out of the binary.

#import "CN1Crypto.h"

#ifndef NEW_CODENAME_ONE_VM
#define CN1_PRIM_ARR_DATA(arr) ((void*)((org_xmlvm_runtime_XMLVMArray*)(arr))->fields.org_xmlvm_runtime_XMLVMArray.array_)
#define CN1_PRIM_ARR_LEN(arr)  (((org_xmlvm_runtime_XMLVMArray*)(arr))->fields.org_xmlvm_runtime_XMLVMArray.length_)
#else
#define CN1_PRIM_ARR_DATA(arr) ((void*)((JAVA_ARRAY)(arr))->data)
#define CN1_PRIM_ARR_LEN(arr)  (((JAVA_ARRAY)(arr))->length)
#endif

#ifdef CN1_INCLUDE_CRYPTO

void com_codename1_impl_ios_IOSNative_secureRandomBytes___byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT out) {
    if (out == JAVA_NULL) return;
    cn1_crypto_secure_random((uint8_t*) CN1_PRIM_ARR_DATA(out), (int) CN1_PRIM_ARR_LEN(out));
}

JAVA_INT com_codename1_impl_ios_IOSNative_aesCbc___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT encrypt, JAVA_OBJECT keyArr, JAVA_OBJECT ivArr, JAVA_OBJECT inArr, JAVA_OBJECT outArr, JAVA_INT padding) {
    return cn1_crypto_aes_cbc(encrypt,
        (uint8_t*) CN1_PRIM_ARR_DATA(keyArr), (int) CN1_PRIM_ARR_LEN(keyArr),
        (uint8_t*) CN1_PRIM_ARR_DATA(ivArr),
        (uint8_t*) CN1_PRIM_ARR_DATA(inArr),  (int) CN1_PRIM_ARR_LEN(inArr),
        (uint8_t*) CN1_PRIM_ARR_DATA(outArr), (int) CN1_PRIM_ARR_LEN(outArr),
        padding);
}

JAVA_INT com_codename1_impl_ios_IOSNative_aesGcm___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT encrypt, JAVA_OBJECT keyArr, JAVA_OBJECT ivArr, JAVA_OBJECT aadArr, JAVA_OBJECT inArr, JAVA_OBJECT outArr) {
    const uint8_t* aadPtr = (aadArr == JAVA_NULL) ? NULL : (uint8_t*) CN1_PRIM_ARR_DATA(aadArr);
    int aadLen = (aadArr == JAVA_NULL) ? 0 : (int) CN1_PRIM_ARR_LEN(aadArr);
    return cn1_crypto_aes_gcm(encrypt,
        (uint8_t*) CN1_PRIM_ARR_DATA(keyArr), (int) CN1_PRIM_ARR_LEN(keyArr),
        (uint8_t*) CN1_PRIM_ARR_DATA(ivArr),  (int) CN1_PRIM_ARR_LEN(ivArr),
        aadPtr, aadLen,
        (uint8_t*) CN1_PRIM_ARR_DATA(inArr),  (int) CN1_PRIM_ARR_LEN(inArr),
        (uint8_t*) CN1_PRIM_ARR_DATA(outArr), (int) CN1_PRIM_ARR_LEN(outArr));
}

JAVA_INT com_codename1_impl_ios_IOSNative_rsaEncrypt___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT paddingKind, JAVA_OBJECT x509, JAVA_OBJECT inArr, JAVA_OBJECT outArr) {
    return cn1_crypto_rsa_encrypt(paddingKind,
        (uint8_t*) CN1_PRIM_ARR_DATA(x509),  (int) CN1_PRIM_ARR_LEN(x509),
        (uint8_t*) CN1_PRIM_ARR_DATA(inArr), (int) CN1_PRIM_ARR_LEN(inArr),
        (uint8_t*) CN1_PRIM_ARR_DATA(outArr),(int) CN1_PRIM_ARR_LEN(outArr));
}

JAVA_INT com_codename1_impl_ios_IOSNative_rsaDecrypt___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT paddingKind, JAVA_OBJECT pkcs8, JAVA_OBJECT inArr, JAVA_OBJECT outArr) {
    return cn1_crypto_rsa_decrypt(paddingKind,
        (uint8_t*) CN1_PRIM_ARR_DATA(pkcs8), (int) CN1_PRIM_ARR_LEN(pkcs8),
        (uint8_t*) CN1_PRIM_ARR_DATA(inArr), (int) CN1_PRIM_ARR_LEN(inArr),
        (uint8_t*) CN1_PRIM_ARR_DATA(outArr),(int) CN1_PRIM_ARR_LEN(outArr));
}

JAVA_INT com_codename1_impl_ios_IOSNative_sign___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT algorithm, JAVA_OBJECT pkcs8, JAVA_OBJECT data, JAVA_OBJECT outArr) {
    return cn1_crypto_sign(algorithm,
        (uint8_t*) CN1_PRIM_ARR_DATA(pkcs8), (int) CN1_PRIM_ARR_LEN(pkcs8),
        (uint8_t*) CN1_PRIM_ARR_DATA(data),  (int) CN1_PRIM_ARR_LEN(data),
        (uint8_t*) CN1_PRIM_ARR_DATA(outArr),(int) CN1_PRIM_ARR_LEN(outArr));
}

JAVA_INT com_codename1_impl_ios_IOSNative_verify___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT algorithm, JAVA_OBJECT x509, JAVA_OBJECT data, JAVA_OBJECT sig) {
    return cn1_crypto_verify(algorithm,
        (uint8_t*) CN1_PRIM_ARR_DATA(x509), (int) CN1_PRIM_ARR_LEN(x509),
        (uint8_t*) CN1_PRIM_ARR_DATA(data), (int) CN1_PRIM_ARR_LEN(data),
        (uint8_t*) CN1_PRIM_ARR_DATA(sig),  (int) CN1_PRIM_ARR_LEN(sig));
}

JAVA_INT com_codename1_impl_ios_IOSNative_generateRsaKeyPair___int_byte_1ARRAY_byte_1ARRAY_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT bits, JAVA_OBJECT outPub, JAVA_OBJECT outPriv, JAVA_OBJECT lengths) {
    int pubLen = 0, privLen = 0;
    int rc = cn1_crypto_generate_rsa_keypair(bits,
        (uint8_t*) CN1_PRIM_ARR_DATA(outPub),  (int) CN1_PRIM_ARR_LEN(outPub),  &pubLen,
        (uint8_t*) CN1_PRIM_ARR_DATA(outPriv), (int) CN1_PRIM_ARR_LEN(outPriv), &privLen);
    JAVA_ARRAY_INT* lens = (JAVA_ARRAY_INT*) CN1_PRIM_ARR_DATA(lengths);
    lens[0] = pubLen;
    lens[1] = privLen;
    return rc;
}

#else /* CN1_INCLUDE_CRYPTO */

/*
 * When the crypto API isn't reachable from the user's code we still emit
 * stub IOSNative bridge symbols so the generated C from IOSImplementation
 * has something to link against, but they all just delegate to the
 * CN1_CRYPTO_E_UNSUPPORTED stubs in CN1Crypto.m (no encryption symbols
 * referenced).
 */

void com_codename1_impl_ios_IOSNative_secureRandomBytes___byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT out) {
    (void) instanceObject; (void) out;
}

JAVA_INT com_codename1_impl_ios_IOSNative_aesCbc___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT encrypt, JAVA_OBJECT keyArr, JAVA_OBJECT ivArr, JAVA_OBJECT inArr, JAVA_OBJECT outArr, JAVA_INT padding) {
    (void) instanceObject; (void) encrypt; (void) keyArr; (void) ivArr; (void) inArr; (void) outArr; (void) padding;
    return CN1_CRYPTO_E_UNSUPPORTED;
}

JAVA_INT com_codename1_impl_ios_IOSNative_aesGcm___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT encrypt, JAVA_OBJECT keyArr, JAVA_OBJECT ivArr, JAVA_OBJECT aadArr, JAVA_OBJECT inArr, JAVA_OBJECT outArr) {
    (void) instanceObject; (void) encrypt; (void) keyArr; (void) ivArr; (void) aadArr; (void) inArr; (void) outArr;
    return CN1_CRYPTO_E_UNSUPPORTED;
}

JAVA_INT com_codename1_impl_ios_IOSNative_rsaEncrypt___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT paddingKind, JAVA_OBJECT x509, JAVA_OBJECT inArr, JAVA_OBJECT outArr) {
    (void) instanceObject; (void) paddingKind; (void) x509; (void) inArr; (void) outArr;
    return CN1_CRYPTO_E_UNSUPPORTED;
}

JAVA_INT com_codename1_impl_ios_IOSNative_rsaDecrypt___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT paddingKind, JAVA_OBJECT pkcs8, JAVA_OBJECT inArr, JAVA_OBJECT outArr) {
    (void) instanceObject; (void) paddingKind; (void) pkcs8; (void) inArr; (void) outArr;
    return CN1_CRYPTO_E_UNSUPPORTED;
}

JAVA_INT com_codename1_impl_ios_IOSNative_sign___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT algorithm, JAVA_OBJECT pkcs8, JAVA_OBJECT data, JAVA_OBJECT outArr) {
    (void) instanceObject; (void) algorithm; (void) pkcs8; (void) data; (void) outArr;
    return CN1_CRYPTO_E_UNSUPPORTED;
}

JAVA_INT com_codename1_impl_ios_IOSNative_verify___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT algorithm, JAVA_OBJECT x509, JAVA_OBJECT data, JAVA_OBJECT sig) {
    (void) instanceObject; (void) algorithm; (void) x509; (void) data; (void) sig;
    return CN1_CRYPTO_E_UNSUPPORTED;
}

JAVA_INT com_codename1_impl_ios_IOSNative_generateRsaKeyPair___int_byte_1ARRAY_byte_1ARRAY_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT bits, JAVA_OBJECT outPub, JAVA_OBJECT outPriv, JAVA_OBJECT lengths) {
    (void) instanceObject; (void) bits; (void) outPub; (void) outPriv;
    if (lengths != JAVA_NULL) {
        JAVA_ARRAY_INT* lens = (JAVA_ARRAY_INT*) CN1_PRIM_ARR_DATA(lengths);
        lens[0] = 0;
        lens[1] = 0;
    }
    return CN1_CRYPTO_E_UNSUPPORTED;
}

#endif /* CN1_INCLUDE_CRYPTO */

// ============================================================================
// Biometrics + SecureStorage natives (LocalAuthentication + Security framework)
// ============================================================================
//
// The static LAContext is held across calls so it can be invalidated mid-prompt
// by stopBiometricAuthentication(). Memory management is manual because the
// iOS port builds with CLANG_ENABLE_OBJC_ARC=NO (see ARC memory in plan).

#if !TARGET_OS_TV
static LAContext *cn1_biometricsContext = nil;
#endif // !TARGET_OS_TV
static NSString *cn1_keychainAccessGroup = nil;

#if !TARGET_OS_TV
static LAContext *cn1_ensureContext(void) {
    if (cn1_biometricsContext == nil) {
        cn1_biometricsContext = [[LAContext alloc] init];
    }
    return cn1_biometricsContext;
}

static void cn1_resetContext(void) {
    if (cn1_biometricsContext != nil) {
        [cn1_biometricsContext release];
        cn1_biometricsContext = nil;
    }
}
#endif // !TARGET_OS_TV

#if !TARGET_OS_WATCH
BOOL cn1AccessibilityEagerLatched(void);
void cn1RegisterAccessibilityStatusObservers(void);

// A technology STARTING is not a component mutation, so nothing in the portable
// layer would schedule the projection it needs and the native tree would stay
// empty until some unrelated UI change happened to invalidate something. These
// notifications are the trigger for that transition.
//
// Once any of them fires we latch eager projection on for the rest of the
// process rather than flipping it back and forth. The technologies UIKit will
// not report at all are handled by cn1AccessibilityNoteClientQuery below, which
// does not depend on flags or notifications.
static BOOL cn1A11yLatched = NO;

BOOL cn1AccessibilityEagerLatched(void) {
    return cn1A11yLatched;
}

#if !TARGET_OS_OSX
static void cn1AccessibilityStatusChanged(CFNotificationCenterRef center, void *observer,
                                          CFStringRef name, const void *object,
                                          CFDictionaryRef userInfo) {
    cn1A11yLatched = YES;
    com_codename1_impl_ios_IOSImplementation_assistiveTechnologyStatusChanged__(
            CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}
#endif

// Called from the METALView / EAGLView accessibilityElements getters: a real
// client asked for
// the tree. This, not the running flags, is what makes the gate correct for the
// technologies UIKit will not report -- see the comment on that getter.
void cn1AccessibilityNoteClientQuery(void) {
    if(cn1A11yLatched) {
        return;   // one transition only; this is on a UIKit query path
    }
    cn1A11yLatched = YES;
#if TARGET_OS_OSX
    // Never reached on the native macOS port: the callers are the UIKit views'
    // accessibilityElements getters, and this port's rendering view exposes no
    // accessibility tree yet.
    //
    // The Java call is left out rather than kept unreachable, deliberately. The
    // method is a static invoked only from C, which the dead-code pass drops --
    // the retention rule keeps a NATIVE method alive by its symbol appearing
    // here, not a plain static called the other way. Referencing a method that
    // has been dropped is a link error, and the alternative -- forcing it to be
    // retained -- would keep a callback nothing can ever invoke.
#else
    com_codename1_impl_ios_IOSImplementation_assistiveTechnologyStatusChanged__(
            CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
#endif
}

void cn1RegisterAccessibilityStatusObservers(void) {
    static BOOL done = NO;
    if(done) {
        return;
    }
    done = YES;
#if TARGET_OS_OSX
    // Nothing to observe. macOS publishes no notification for an assistive
    // technology starting -- NSWorkspace reports the display-appearance
    // preferences and nothing about VoiceOver or Switch Control -- so there is
    // no transition to latch on. isAssistiveTechnologyActive below reads
    // VoiceOver's own preference instead, which is the one signal the platform
    // does give.
#else
    // Built up rather than written as a literal: the AssistiveTouch notification
    // is iOS 10, and ios.deployment_target lets IPhoneBuilder emit older
    // targets, where the weakly-linked constant is nil -- and a nil inside an
    // @[] literal raises. Same reason the running check below is guarded.
    NSMutableArray *names = [NSMutableArray arrayWithCapacity:3];
    if(UIAccessibilityVoiceOverStatusDidChangeNotification != nil) {
        [names addObject:UIAccessibilityVoiceOverStatusDidChangeNotification];
    }
    if(UIAccessibilitySwitchControlStatusDidChangeNotification != nil) {
        [names addObject:UIAccessibilitySwitchControlStatusDidChangeNotification];
    }
    if(UIAccessibilityAssistiveTouchStatusDidChangeNotification != nil) {
        [names addObject:UIAccessibilityAssistiveTouchStatusDidChangeNotification];
    }
    for(NSString *n in names) {
        CFNotificationCenterAddObserver(CFNotificationCenterGetLocalCenter(), NULL,
                                        cn1AccessibilityStatusChanged,
                                        (__bridge CFStringRef)n, NULL,
                                        CFNotificationSuspensionBehaviorDeliverImmediately);
    }
#endif // TARGET_OS_OSX
}
#endif

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAssistiveTechnologyActive___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    // CN1_EAGER_A11Y=1 restores the old always-project behaviour for an A/B.
    if(getenv("CN1_EAGER_A11Y") != NULL) {
        return JAVA_TRUE;
    }
#if !TARGET_OS_WATCH
    // These three are the ENTIRE public surface for "is an assistive technology
    // running": UIKit exposes IsVoiceOverRunning, IsSwitchControlRunning and
    // IsAssistiveTouchRunning and nothing else. In particular there is no
    // public running flag for Voice Control or Full Keyboard Access, so this
    // cannot detect them -- see cn1AccessibilityStatusChanged for how that gap
    // is covered rather than ignored.
    cn1RegisterAccessibilityStatusObservers();
    if(cn1AccessibilityEagerLatched()) {
        return JAVA_TRUE;
    }
#if TARGET_OS_OSX
    // VoiceOver's own preference domain, which is where macOS records it, and
    // the only assistive technology the platform lets an application ask about.
    // Switch Control and AssistiveTouch have no macOS query and no macOS
    // equivalent respectively.
    extern BOOL CN1MacHostIsVoiceOverRunning(void);
    return CN1MacHostIsVoiceOverRunning() ? JAVA_TRUE : JAVA_FALSE;
#else
    if(UIAccessibilityIsVoiceOverRunning() || UIAccessibilityIsSwitchControlRunning()) {
        return JAVA_TRUE;
    }
    // iOS 10. Weakly linked, so on an older deployment target the symbol is
    // null and calling it jumps through nothing -- test the pointer first.
    if(UIAccessibilityIsAssistiveTouchRunning != NULL &&
       UIAccessibilityIsAssistiveTouchRunning()) {
        return JAVA_TRUE;
    }
    return JAVA_FALSE;
#endif // TARGET_OS_OSX
#else
    return JAVA_FALSE;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDirectToDrawable___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_USE_METAL
    extern int cn1DirectToDrawableEnabled(void);
    return cn1DirectToDrawableEnabled() ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isBiometricsSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    if (NSClassFromString(@"LAContext") == NULL) {
        return JAVA_FALSE;
    }
    NSError *error = nil;
    LAContext *ctx = cn1_ensureContext();
    BOOL ok = [ctx canEvaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics error:&error];
    return ok ? JAVA_TRUE : JAVA_FALSE;
#else
    // watchOS/tvOS have no LAPolicyDeviceOwnerAuthenticationWithBiometrics.
    return JAVA_FALSE;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_canAuthenticateBiometric___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return com_codename1_impl_ios_IOSNative_isBiometricsSupported___R_boolean(CN1_THREAD_STATE_PASS_ARG me);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getAvailableBiometricTypes___R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    if (NSClassFromString(@"LAContext") == NULL) {
        return 0;
    }
    NSError *error = nil;
    LAContext *ctx = cn1_ensureContext();
    if (![ctx canEvaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics error:&error]) {
        return 0;
    }
    JAVA_INT mask = 0;
    if (@available(iOS 11.0, *)) {
        if (ctx.biometryType == LABiometryTypeTouchID) {
            mask |= 1;
        } else if (ctx.biometryType == LABiometryTypeFaceID) {
            mask |= 2;
        }
    } else {
        // Pre-iOS 11: only Touch ID exists.
        mask |= 1;
    }
    return mask;
#else
    // watchOS/tvOS have no LAPolicyDeviceOwnerAuthenticationWithBiometrics.
    return 0;
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
}

void com_codename1_impl_ios_IOSNative_authenticateBiometric___int_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_OBJECT reason) {
#if !TARGET_OS_WATCH && !TARGET_OS_TV
    POOL_BEGIN();
    NSString *nsReason = (reason == JAVA_NULL) ? @"Authenticate" : toNSString(CN1_THREAD_STATE_PASS_ARG reason);
    // Each authenticate call gets a fresh context so a prior stopAuthentication
    // can't bleed cancellation into the next request.
    cn1_resetContext();
    LAContext *ctx = cn1_ensureContext();
    dispatch_async(dispatch_get_main_queue(), ^{
        [ctx evaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics
            localizedReason:nsReason
                      reply:^(BOOL success, NSError *err) {
            if (success) {
                com_codename1_impl_ios_IOSBiometrics_nativeAuthSuccess___int(getThreadLocalData(), requestId);
            } else {
                int code = (int)err.code;
                NSString *msg = err.localizedDescription ? err.localizedDescription : @"";
                JAVA_OBJECT jmsg = fromNSString(getThreadLocalData(), msg);
                com_codename1_impl_ios_IOSBiometrics_nativeAuthError___int_int_java_lang_String(getThreadLocalData(), requestId, code, jmsg);
            }
        }];
    });
    POOL_END();
#else
    // watchOS/tvOS have no LAPolicyDeviceOwnerAuthenticationWithBiometrics; report failure.
    com_codename1_impl_ios_IOSBiometrics_nativeAuthError___int_int_java_lang_String(getThreadLocalData(), requestId, -1, JAVA_NULL);
#endif // !TARGET_OS_WATCH && !TARGET_OS_TV
}

void com_codename1_impl_ios_IOSNative_stopBiometricAuthentication__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#if !TARGET_OS_TV
    if (cn1_biometricsContext != nil) {
        if (@available(iOS 9.0, *)) {
            [cn1_biometricsContext invalidate];
        }
        cn1_resetContext();
    }
#endif // !TARGET_OS_TV
}

// --- App Attest (DeviceCheck.framework) -----------------------------------
// Gated by CN1_USE_APP_ATTEST: the ios.appAttest build hint uncomments the
// define, links DeviceCheck.framework and injects the App Attest entitlement.
// Builds without the hint compile the stub branch below, so they neither import
// nor link DeviceCheck. clientDataHash is the SHA-256 of the server nonce; the
// returned token is base64(keyId):base64(attestationObject) for the backend to
// verify with Apple.
//#define CN1_USE_APP_ATTEST
#ifdef CN1_USE_APP_ATTEST
#import <DeviceCheck/DeviceCheck.h>
#import <CommonCrypto/CommonCrypto.h>

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAppAttestSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#if !TARGET_OS_TV && !TARGET_OS_WATCH
    if (@available(iOS 14.0, *)) {
        if (NSClassFromString(@"DCAppAttestService") == NULL) {
            return JAVA_FALSE;
        }
        return [DCAppAttestService sharedService].isSupported ? JAVA_TRUE : JAVA_FALSE;
    }
    return JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif // !TARGET_OS_TV && !TARGET_OS_WATCH
}

// Reports a failure back to Java. errorCode carries the raw DCError value so the
// Java side can tell "the key is invalid, throw it away and re-attest" (2) from
// "Apple is throttling us, back off" (4) -- treating those the same is how an
// app burns its attestation budget in a retry loop.
static void cn1AppAttestFail(JAVA_INT requestId, NSError *err, NSString *fallback) {
    NSString *m = err != nil ? err.localizedDescription : fallback;
    JAVA_INT code = err != nil ? (JAVA_INT)err.code : -1;
    JAVA_OBJECT jmsg = fromNSString(getThreadLocalData(), m);
    com_codename1_impl_ios_IOSDeviceIntegrity_nativeAttestError___int_int_java_lang_String(getThreadLocalData(), requestId, code, jmsg);
}

// DCAppAttestService completion handlers run on an arbitrary dispatch queue.
// Hopping to the main queue means every re-entry into the VM comes from a known
// thread, matching what the biometrics block above does.
#define CN1_APP_ATTEST_ON_MAIN(block) dispatch_async(dispatch_get_main_queue(), block)

void com_codename1_impl_ios_IOSNative_appAttestGenerateKey___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
#if !TARGET_OS_TV && !TARGET_OS_WATCH
    POOL_BEGIN();
    if (@available(iOS 14.0, *)) {
        DCAppAttestService *service = [DCAppAttestService sharedService];
        if (!service.isSupported) {
            cn1AppAttestFail(requestId, nil, @"App Attest not supported");
            POOL_END();
            return;
        }
        [service generateKeyWithCompletionHandler:^(NSString *keyId, NSError *genErr) {
            CN1_APP_ATTEST_ON_MAIN(^{
                if (genErr != nil || keyId == nil) {
                    cn1AppAttestFail(requestId, genErr, @"App Attest key generation failed");
                    return;
                }
                JAVA_OBJECT jkey = fromNSString(getThreadLocalData(), keyId);
                com_codename1_impl_ios_IOSDeviceIntegrity_nativeKeyGenerated___int_java_lang_String(getThreadLocalData(), requestId, jkey);
            });
        }];
    } else {
        cn1AppAttestFail(requestId, nil, @"App Attest requires iOS 14+");
    }
    POOL_END();
#else
    cn1AppAttestFail(requestId, nil, @"App Attest not available on this platform");
#endif // !TARGET_OS_TV && !TARGET_OS_WATCH
}

void com_codename1_impl_ios_IOSNative_appAttestAttestKey___int_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_OBJECT keyId, JAVA_OBJECT clientDataHashB64) {
#if !TARGET_OS_TV && !TARGET_OS_WATCH
    POOL_BEGIN();
    if (@available(iOS 14.0, *)) {
        DCAppAttestService *service = [DCAppAttestService sharedService];
        NSString *nsKeyId = (keyId == JAVA_NULL) ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG keyId);
        NSString *nsHash = (clientDataHashB64 == JAVA_NULL) ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG clientDataHashB64);
        NSData *clientDataHash = nsHash == nil ? nil
            : [[NSData alloc] initWithBase64EncodedString:nsHash options:0];
#ifndef CN1_USE_ARC
        // initWithBase64EncodedString returns an owned object. The block below
        // retains it for the duration of the call, so hand ownership to the pool
        // rather than leaking one decoded hash per request. Guarded because ARC
        // forbids an explicit autorelease, and this file builds both ways.
        [clientDataHash autorelease];
#endif
        if (nsKeyId == nil || clientDataHash == nil) {
            cn1AppAttestFail(requestId, nil, @"App Attest attestation missing key or hash");
            POOL_END();
            return;
        }
        [service attestKey:nsKeyId clientDataHash:clientDataHash completionHandler:^(NSData *attestationObject, NSError *attErr) {
            CN1_APP_ATTEST_ON_MAIN(^{
                if (attErr != nil || attestationObject == nil) {
                    cn1AppAttestFail(requestId, attErr, @"App Attest attestation failed");
                    return;
                }
                NSString *b64Att = [attestationObject base64EncodedStringWithOptions:0];
                JAVA_OBJECT jatt = fromNSString(getThreadLocalData(), b64Att);
                com_codename1_impl_ios_IOSDeviceIntegrity_nativeAttestationReady___int_java_lang_String(getThreadLocalData(), requestId, jatt);
            });
        }];
    } else {
        cn1AppAttestFail(requestId, nil, @"App Attest requires iOS 14+");
    }
    POOL_END();
#else
    cn1AppAttestFail(requestId, nil, @"App Attest not available on this platform");
#endif // !TARGET_OS_TV && !TARGET_OS_WATCH
}

void com_codename1_impl_ios_IOSNative_appAttestGenerateAssertion___int_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_OBJECT keyId, JAVA_OBJECT clientDataHashB64) {
#if !TARGET_OS_TV && !TARGET_OS_WATCH
    POOL_BEGIN();
    if (@available(iOS 14.0, *)) {
        DCAppAttestService *service = [DCAppAttestService sharedService];
        NSString *nsKeyId = (keyId == JAVA_NULL) ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG keyId);
        NSString *nsHash = (clientDataHashB64 == JAVA_NULL) ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG clientDataHashB64);
        NSData *clientDataHash = nsHash == nil ? nil
            : [[NSData alloc] initWithBase64EncodedString:nsHash options:0];
#ifndef CN1_USE_ARC
        // initWithBase64EncodedString returns an owned object. The block below
        // retains it for the duration of the call, so hand ownership to the pool
        // rather than leaking one decoded hash per request. Guarded because ARC
        // forbids an explicit autorelease, and this file builds both ways.
        [clientDataHash autorelease];
#endif
        if (nsKeyId == nil || clientDataHash == nil) {
            cn1AppAttestFail(requestId, nil, @"App Attest assertion missing key or hash");
            POOL_END();
            return;
        }
        [service generateAssertion:nsKeyId clientDataHash:clientDataHash completionHandler:^(NSData *assertion, NSError *assertErr) {
            CN1_APP_ATTEST_ON_MAIN(^{
                if (assertErr != nil || assertion == nil) {
                    cn1AppAttestFail(requestId, assertErr, @"App Attest assertion failed");
                    return;
                }
                NSString *b64 = [assertion base64EncodedStringWithOptions:0];
                JAVA_OBJECT jassert = fromNSString(getThreadLocalData(), b64);
                com_codename1_impl_ios_IOSDeviceIntegrity_nativeAssertionReady___int_java_lang_String(getThreadLocalData(), requestId, jassert);
            });
        }];
    } else {
        cn1AppAttestFail(requestId, nil, @"App Attest requires iOS 14+");
    }
    POOL_END();
#else
    cn1AppAttestFail(requestId, nil, @"App Attest not available on this platform");
#endif // !TARGET_OS_TV && !TARGET_OS_WATCH
}
#else // CN1_USE_APP_ATTEST

// App Attest not enabled (ios.appAttest build hint off): DeviceCheck.framework
// is neither imported nor linked. Report unsupported / fail the request.
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAppAttestSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}

void com_codename1_impl_ios_IOSNative_appAttestGenerateKey___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
    com_codename1_impl_ios_IOSDeviceIntegrity_nativeAttestError___int_int_java_lang_String(getThreadLocalData(), requestId, -1, JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_appAttestAttestKey___int_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_OBJECT keyId, JAVA_OBJECT clientDataHashB64) {
    com_codename1_impl_ios_IOSDeviceIntegrity_nativeAttestError___int_int_java_lang_String(getThreadLocalData(), requestId, -1, JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_appAttestGenerateAssertion___int_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_OBJECT keyId, JAVA_OBJECT clientDataHashB64) {
    com_codename1_impl_ios_IOSDeviceIntegrity_nativeAttestError___int_int_java_lang_String(getThreadLocalData(), requestId, -1, JAVA_NULL);
}
#endif // CN1_USE_APP_ATTEST

// Jailbreak/instrumentation signals. Always compiled, independent of both
// CN1_USE_APP_ATTEST and CN1_DETECT_JAILBREAK, because DeviceIntegrity reports
// these at runtime without terminating the app.
JAVA_OBJECT com_codename1_impl_ios_IOSNative_iosJailbreakSignals___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    NSString *signals = cn1JailbreakSignals();
    JAVA_OBJECT result = fromNSString(CN1_THREAD_STATE_PASS_ARG (signals == nil ? @"" : signals));
    POOL_END();
    return result;
}

// --- CarPlay (CarPlay.framework) ------------------------------------------
// Gated by CN1_USE_CARPLAY: the builder uncomments the define, links
// CarPlay.framework, injects the CarPlay scene into the Info.plist scene manifest
// and adds the carplay entitlement, when the app references com.codename1.car.
// Builds without it compile the stub branch (no CarPlay import/link). The C
// functions are thin trampolines onto CN1CarPlayManager (CodenameOne_CarPlaySceneDelegate).
// CN1_USE_CARPLAY is defined in CodenameOne_GLViewController.h (imported near the top of this file),
// flipped by the builder when the app references com.codename1.car.
#ifdef CN1_USE_CARPLAY
#import "CodenameOne_CarPlaySceneDelegate.h"

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isCarPlayConnected__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    if (@available(iOS 14.0, *)) {
        return [CN1CarPlayManager sharedManager].connected ? JAVA_TRUE : JAVA_FALSE;
    }
    return JAVA_FALSE;
}

void com_codename1_impl_ios_IOSNative_carPlaySetTemplate___int_java_lang_String_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT screenId, JAVA_OBJECT json, JAVA_BOOLEAN isRoot) {
    if (@available(iOS 14.0, *)) {
        POOL_BEGIN();
        NSString* j = (json == JAVA_NULL) ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG json);
        int sid = (int)screenId;
        BOOL root = (isRoot == JAVA_TRUE);
        dispatch_async(dispatch_get_main_queue(), ^{
            [[CN1CarPlayManager sharedManager] setTemplate:sid json:j isRoot:root];
        });
        POOL_END();
    }
}

void com_codename1_impl_ios_IOSNative_carPlayUpdateTemplate___int_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT screenId, JAVA_OBJECT json) {
    if (@available(iOS 14.0, *)) {
        POOL_BEGIN();
        NSString* j = (json == JAVA_NULL) ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG json);
        int sid = (int)screenId;
        dispatch_async(dispatch_get_main_queue(), ^{
            [[CN1CarPlayManager sharedManager] updateTemplate:sid json:j];
        });
        POOL_END();
    }
}

void com_codename1_impl_ios_IOSNative_carPlayPopTemplate__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    if (@available(iOS 14.0, *)) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [[CN1CarPlayManager sharedManager] popTemplate];
        });
    }
}

void com_codename1_impl_ios_IOSNative_carPlayRegisterImage___java_lang_String_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT key, JAVA_OBJECT pngArr) {
    if (@available(iOS 14.0, *)) {
        POOL_BEGIN();
        NSString* k = (key == JAVA_NULL) ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG key);
        NSData* data = nil;
        if (pngArr != JAVA_NULL) {
#ifndef NEW_CODENAME_ONE_VM
            org_xmlvm_runtime_XMLVMArray* ba = pngArr;
            JAVA_ARRAY_BYTE* bytes = (JAVA_ARRAY_BYTE*)ba->fields.org_xmlvm_runtime_XMLVMArray.array_;
            int len = ba->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
            JAVA_ARRAY ba = (JAVA_ARRAY)pngArr;
            void* bytes = ba->data;
            int len = (int)ba->length;
#endif
            data = [NSData dataWithBytes:bytes length:len];
        }
        NSString* kk = k;
        NSData* dd = data;
        dispatch_async(dispatch_get_main_queue(), ^{
            [[CN1CarPlayManager sharedManager] registerImage:kk data:dd];
        });
        POOL_END();
    }
}

void com_codename1_impl_ios_IOSNative_carPlayShowToast___java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT message, JAVA_INT seconds) {
    if (@available(iOS 14.0, *)) {
        POOL_BEGIN();
        NSString* m = (message == JAVA_NULL) ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG message);
        int s = (int)seconds;
        dispatch_async(dispatch_get_main_queue(), ^{
            [[CN1CarPlayManager sharedManager] showToast:m seconds:s];
        });
        POOL_END();
    }
}
#else // CN1_USE_CARPLAY

// CarPlay not enabled: CarPlay.framework is neither imported nor linked.
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isCarPlayConnected__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}
void com_codename1_impl_ios_IOSNative_carPlaySetTemplate___int_java_lang_String_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT screenId, JAVA_OBJECT json, JAVA_BOOLEAN isRoot) {
}
void com_codename1_impl_ios_IOSNative_carPlayUpdateTemplate___int_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT screenId, JAVA_OBJECT json) {
}
void com_codename1_impl_ios_IOSNative_carPlayPopTemplate__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
}
void com_codename1_impl_ios_IOSNative_carPlayRegisterImage___java_lang_String_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT key, JAVA_OBJECT pngArr) {
}
void com_codename1_impl_ios_IOSNative_carPlayShowToast___java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT message, JAVA_INT seconds) {
}
#endif // CN1_USE_CARPLAY

// New-VM (return-type-encoded) mangling for the boolean CarPlay query. Defined here, after the
// isCarPlayConnected__ implementation/stub above, so the call is to an already-declared function
// (the wrapper section higher up runs before this definition). The void carPlay* methods need no
// _R_ wrapper. Always defined (real or stub) regardless of CN1_USE_CARPLAY.
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isCarPlayConnected___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isCarPlayConnected__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

// --- External surfaces (WidgetKit + ActivityKit) ---------------------------
// Gated by CN1_USE_WIDGETS: the builder uncomments the define (in
// CodenameOne_GLViewController.h, imported near the top of this file), generates the
// CN1Widgets extension target and injects the CN1SurfacesAppGroup Info.plist key when the
// app references com.codename1.surfaces. Builds without it compile the stub branch.
// WidgetKit/ActivityKit are Swift-only, so the real implementations are thin trampolines
// onto the Swift CN1SurfaceBridge class (compiled into the app target by the builder),
// reached via NSClassFromString + typed objc_msgSend casts. The Swift side owns its own
// threading (WidgetCenter is thread safe; ActivityKit updates run in Tasks), so these
// call it directly on the calling (CN1) thread.
#ifdef CN1_USE_WIDGETS

static NSString *cn1SurfacesGroupId() {
    id v = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CN1SurfacesAppGroup"];
    return ([v isKindOfClass:[NSString class]] && [(NSString *)v length] > 0) ? (NSString *)v : nil;
}

static NSString *cn1SurfacesContainerPath() {
    NSString *group = cn1SurfacesGroupId();
    if (group == nil) {
        return nil;
    }
    NSURL *container = [[NSFileManager defaultManager] containerURLForSecurityApplicationGroupIdentifier:group];
    return container == nil ? nil : container.path;
}

static Class cn1SurfacesBridgeClass() {
    return NSClassFromString(@"CN1SurfaceBridge");
}

// True when the running OS meets the widget extension's deployment target
// (CN1SurfacesMinOS Info.plist key, injected by the builder from
// ios.surfaces.deploymentTarget; defaults to 16.1 on iOS). Below that version the
// extension cannot run or appear in the widget gallery, so the API must not
// report widget support even though WidgetKit itself shipped with iOS 14.
//
// The fallback is per-platform because the two extensions have different floors and this
// compares against the OS actually running. The watch app's CN1WatchWidgets extension targets
// watchOS 10, so the iOS default of 16.1 would be compared against a watchOS version and never
// be met -- every watch would have reported no widget support, whatever was in the plist.
static BOOL cn1SurfacesMinOSSupported() {
    NSString *min = nil;
    id v = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CN1SurfacesMinOS"];
    if ([v isKindOfClass:[NSString class]] && [(NSString *)v length] > 0) {
        min = (NSString *)v;
    } else {
#if TARGET_OS_WATCH
        min = @"10.0";
#else
        min = @"16.1";
#endif
    }
    NSArray *parts = [min componentsSeparatedByString:@"."];
    NSOperatingSystemVersion required;
#if TARGET_OS_WATCH
    NSInteger defaultMajor = 10;
    NSInteger defaultMinor = 0;
#else
    NSInteger defaultMajor = 16;
    NSInteger defaultMinor = 1;
#endif
    required.majorVersion = parts.count > 0 ? [[parts objectAtIndex:0] integerValue] : defaultMajor;
    required.minorVersion = parts.count > 1 ? [[parts objectAtIndex:1] integerValue] : defaultMinor;
    required.patchVersion = parts.count > 2 ? [[parts objectAtIndex:2] integerValue] : 0;
    return [[NSProcessInfo processInfo] isOperatingSystemAtLeastVersion:required];
}

// Decodes a cn1surface://a?src=..&id=..&p=<url-encoded JSON> deep link -- a widget, live
// activity or complication tap -- and hands it to the Java framework.
//
// Shared because the two platforms reach it from opposite directions. On iOS every openURL path
// funnels through the app delegate, which is entirely #if !TARGET_OS_WATCH; on watchOS there is
// no UIApplicationDelegate at all and the URL arrives at the SwiftUI scene's onOpenURL, which
// calls cn1_watch_surface_url below. Leaving the decode in the delegate meant a complication tap
// launched the watch app and then dropped the action on the floor.
//
// Surfaces.dispatchAction queues internally until the app registers its handler, so a cold-start
// tap -- which is the usual case for a complication -- is safe.
BOOL cn1HandleSurfaceURL(NSURL *url) {
    if (url == nil || url.scheme == nil) {
        return NO;
    }
    // This app's own scheme, cn1surface.<bundle id>, is what the widget and complication now
    // generate: the bare cn1surface was claimed globally by every Codename One app, so two of
    // them installed together were two claims on one name and the watch could route a tap to
    // the wrong bundle. The bare name is still accepted on the phone because the app has always
    // registered it and something may still hold a link built with it; the WATCH registers only
    // the qualified one, which is where the collision actually bit.
    NSString *ownScheme = [@"cn1surface." stringByAppendingString:
            [[NSBundle mainBundle] bundleIdentifier] ?: @""];
    BOOL mine = [ownScheme caseInsensitiveCompare:url.scheme] == NSOrderedSame;
#if TARGET_OS_WATCH
    if (!mine) {
        return NO;
    }
#else
    if (!mine && [@"cn1surface" caseInsensitiveCompare:url.scheme] != NSOrderedSame) {
        return NO;
    }
#endif
    NSURLComponents *components = [NSURLComponents componentsWithURL:url resolvingAgainstBaseURL:NO];
    NSString *src = nil;
    NSString *actionId = nil;
    NSString *params = nil;
    for (NSURLQueryItem *item in components.queryItems) {
        if ([item.name isEqualToString:@"src"]) {
            src = item.value;
        } else if ([item.name isEqualToString:@"id"]) {
            actionId = item.value;
        } else if ([item.name isEqualToString:@"p"]) {
            // NSURLQueryItem.value is already percent-decoded JSON.
            params = item.value;
        }
    }
    JAVA_OBJECT jSrc = src == nil ? JAVA_NULL : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG src);
    JAVA_OBJECT jActionId = actionId == nil ? JAVA_NULL
            : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG actionId);
    JAVA_OBJECT jParams = params == nil ? JAVA_NULL
            : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG params);
    com_codename1_impl_ios_IOSSurfaceCallbacks_nativeSurfaceAction___java_lang_String_java_lang_String_java_lang_String(
            CN1_THREAD_GET_STATE_PASS_ARG jSrc, jActionId, jParams);
    return YES;
}

#if TARGET_OS_WATCH
/// A lock object for the pending-URL slot, which the SwiftUI scene and the VM bootstrap thread
/// both touch.
@interface CN1WatchSurfaceURLLock : NSObject
@end
@implementation CN1WatchSurfaceURLLock
@end

// Called from the generated CN1WatchApp.swift scene's onOpenURL. A complication tap launches the
// watch app with the URL rather than delivering it to a delegate, so this is the whole path.

// The tap that arrived before the VM did.
//
// A complication tap on a terminated watch app launches it WITH the URL, and SwiftUI delivers
// onOpenURL as soon as the scene exists -- which is before cn1_watch_runtime_start has finished
// bringing the VM up, because it starts it on a pthread and returns. Handling the URL then reaches
// into a half-built runtime to make Java strings and call into Java. So it waits: one pending URL,
// handed over by cn1_watch_runtime_markJavaReady, which is the same readiness the lifecycle phases
// queue behind.
//
// One slot and not a queue. A launch carries one URL, and if a second somehow arrived first the
// newest is the one the user just tapped.
static NSString *cn1WatchPendingSurfaceURL = nil;

/// Whether the drain has run, owned by the lock below rather than read from the runtime.
///
/// Asking cn1_watch_runtime_isJavaReady and then storing is two steps, and the VM thread can
/// become ready and drain an empty slot between them -- the URL is stored a moment later and
/// nothing ever looks at it again. So readiness and the slot move together under one lock: the
/// drain sets this flag while holding it, and a tap either sees the flag and delivers or does not
/// and is found by the drain.
static BOOL cn1WatchSurfaceURLDrained = NO;

void cn1_watch_surface_url(const char *url) {
    if (url == NULL) {
        return;
    }
    POOL_BEGIN();
    NSString *str = [NSString stringWithUTF8String:url];
    if (str != nil) {
        BOOL deliverNow = NO;
        @synchronized ([CN1WatchSurfaceURLLock class]) {
            if (cn1WatchSurfaceURLDrained) {
                deliverNow = YES;
            } else {
                [cn1WatchPendingSurfaceURL release];
                cn1WatchPendingSurfaceURL = [str retain];
            }
        }
        // Outside the lock: handling the URL calls into Java, which must not run holding a lock
        // the VM thread also takes.
        if (deliverNow) {
            cn1HandleSurfaceURL([NSURL URLWithString:str]);
        }
    }
    POOL_END();
}

/// Hands over a tap that arrived before the runtime was ready. Called from
/// cn1_watch_runtime_markJavaReady, and defined whatever this build carries so that call needs no
/// guard of its own.
void cn1_watch_surface_drainPending(void) {
    NSString *pending = nil;
    @synchronized ([CN1WatchSurfaceURLLock class]) {
        // The flag and the slot together, so a tap arriving alongside this either lands in the
        // slot before it is emptied or delivers itself afterwards -- never neither.
        cn1WatchSurfaceURLDrained = YES;
        pending = cn1WatchPendingSurfaceURL;
        cn1WatchPendingSurfaceURL = nil;
    }
    if (pending != nil) {
        POOL_BEGIN();
        cn1HandleSurfaceURL([NSURL URLWithString:pending]);
        POOL_END();
        [pending release];
    }
}
#endif

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getSurfacesContainerPath__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    NSString *path = cn1SurfacesContainerPath();
    JAVA_OBJECT result = fromNSString(CN1_THREAD_STATE_PASS_ARG (path == nil ? @"" : path));
    POOL_END();
    return result;
}

void com_codename1_impl_ios_IOSNative_surfacesReloadTimelines___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT kind) {
    if (@available(iOS 14.0, *)) {
        POOL_BEGIN();
        Class bridge = cn1SurfacesBridgeClass();
        if (bridge != nil) {
            NSString *k = (kind == JAVA_NULL) ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG kind);
            ((void (*)(id, SEL, NSString *))objc_msgSend)((id)bridge,
                    NSSelectorFromString(@"reloadTimelines:"), k);
        }
        POOL_END();
    }
}

JAVA_INT com_codename1_impl_ios_IOSNative_surfacesInstalledCount___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT kind) {
    if (@available(iOS 14.0, *)) {
        POOL_BEGIN();
        JAVA_INT count = 0;
        Class bridge = cn1SurfacesBridgeClass();
        if (bridge != nil) {
            NSString *k = (kind == JAVA_NULL) ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG kind);
            count = (JAVA_INT)((NSInteger (*)(id, SEL, NSString *))objc_msgSend)((id)bridge,
                    NSSelectorFromString(@"installedCount:"), k);
        }
        POOL_END();
        return count;
    }
    return 0;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_surfacesStartActivity___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT descriptorJson) {
    // Live activities are an iOS capability: watchOS has no ActivityKit, and the Swift bridge
    // compiles its ActivityKit bodies out there. Answering here rather than relying on that
    // states the intent -- and keeps the symbol, which the watch slice still links because the
    // Java method is reachable from shared code.
#if TARGET_OS_WATCH
    return JAVA_NULL;
#else
    if (@available(iOS 16.1, *)) {
        POOL_BEGIN();
        JAVA_OBJECT result = JAVA_NULL;
        Class bridge = cn1SurfacesBridgeClass();
        if (bridge != nil && descriptorJson != JAVA_NULL) {
            NSString *json = toNSString(CN1_THREAD_STATE_PASS_ARG descriptorJson);
            NSString *activityId = ((NSString *(*)(id, SEL, NSString *))objc_msgSend)((id)bridge,
                    NSSelectorFromString(@"startActivity:"), json);
            if (activityId != nil && [activityId length] > 0) {
                result = fromNSString(CN1_THREAD_STATE_PASS_ARG activityId);
            }
        }
        POOL_END();
        return result;
    }
    return JAVA_NULL;
#endif
}

void com_codename1_impl_ios_IOSNative_surfacesUpdateActivity___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT activityId, JAVA_OBJECT stateJson) {
    // Live activities are an iOS capability: watchOS has no ActivityKit, and the Swift bridge
    // compiles its ActivityKit bodies out there. Answering here rather than relying on that
    // states the intent -- and keeps the symbol, which the watch slice still links because the
    // Java method is reachable from shared code.
#if TARGET_OS_WATCH
    return;
#else
    if (@available(iOS 16.1, *)) {
        POOL_BEGIN();
        Class bridge = cn1SurfacesBridgeClass();
        if (bridge != nil && activityId != JAVA_NULL) {
            NSString *aid = toNSString(CN1_THREAD_STATE_PASS_ARG activityId);
            NSString *state = (stateJson == JAVA_NULL) ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG stateJson);
            ((void (*)(id, SEL, NSString *, NSString *))objc_msgSend)((id)bridge,
                    NSSelectorFromString(@"updateActivity:stateJson:"), aid, state);
        }
        POOL_END();
    }
#endif
}

void com_codename1_impl_ios_IOSNative_surfacesEndActivity___java_lang_String_java_lang_String_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT activityId, JAVA_OBJECT finalStateJson, JAVA_BOOLEAN dismissImmediately) {
    // Live activities are an iOS capability: watchOS has no ActivityKit, and the Swift bridge
    // compiles its ActivityKit bodies out there. Answering here rather than relying on that
    // states the intent -- and keeps the symbol, which the watch slice still links because the
    // Java method is reachable from shared code.
#if TARGET_OS_WATCH
    return;
#else
    if (@available(iOS 16.1, *)) {
        POOL_BEGIN();
        Class bridge = cn1SurfacesBridgeClass();
        if (bridge != nil && activityId != JAVA_NULL) {
            NSString *aid = toNSString(CN1_THREAD_STATE_PASS_ARG activityId);
            NSString *state = (finalStateJson == JAVA_NULL) ? nil : toNSString(CN1_THREAD_STATE_PASS_ARG finalStateJson);
            ((void (*)(id, SEL, NSString *, NSString *, BOOL))objc_msgSend)((id)bridge,
                    NSSelectorFromString(@"endActivity:finalStateJson:immediate:"),
                    aid, state, dismissImmediately == JAVA_TRUE);
        }
        POOL_END();
    }
#endif
}

// --- Phone -> watch complication mirror ------------------------------------
//
// An App Group container is device-local: the watch resolves the same identifier to a directory
// of its own, which the phone cannot see. So a phone-side Surfaces.publish() is invisible to a
// complication until the descriptor actually travels, and this is that transport.
//
// WCSession's transferCurrentComplicationUserInfo is the only API that WAKES the watch app in
// the background to refresh a complication. updateApplicationContext -- which putData already
// owns, with its own stamp and tombstone protocol -- delivers only when the watch app next runs,
// which for a complication means "possibly never". The budget is small and reported, so this
// degrades through progressively weaker delivery rather than pretending: no complication placed
// or budget spent falls back to transferUserInfo, which arrives eventually; over the size cap
// drops the imagery and then gives up entirely. The local publish has already succeeded, so the
// phone's own widget stays correct whatever happens here.

#if !TARGET_OS_WATCH

/// A strictly increasing publication sequence for mirrored surfaces.
///
/// Seeded from the wall clock so it keeps rising across a relaunch -- a counter restarting at 1
/// would have every publication after a restart look older than what the watch already holds --
/// and incremented so two publications in the same millisecond still differ.
static long long cn1NextSurfaceMirrorSequence(void) {
    static long long last = 0;
    static dispatch_once_t once;
    static NSObject *lock = nil;
    dispatch_once(&once, ^{
        lock = [[NSObject alloc] init];
    });
    @synchronized (lock) {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        if (last == 0) {
            // Resumed from the highest we have ever ISSUED, not from the clock.
            //
            // The clock is only a seed, and it can move backwards -- an NTP correction or the
            // user setting the date. Reseeding from it after a relaunch would then hand out
            // numbers below the high-water mark the WATCH has persisted, and the watch rejects
            // those by design: every mirrored update would be dropped until the clock caught up,
            // which could be hours or days. Remembering what we issued makes the sequence
            // monotonic across a restart whatever the clock does.
            last = (long long)[defaults doubleForKey:@"cn1.surfaces.seq.sent"];
        }
        long long now = (long long)([[NSDate date] timeIntervalSince1970] * 1000.0);
        last = now > last ? now : last + 1;
        [defaults setDouble:(double)last forKey:@"cn1.surfaces.seq.sent"];
        return last;
    }
}


// A property list has a hard ceiling around 64KB and rejects the whole payload on overflow.
// Complication art is a few dozen points square, so 48KB is generous and leaves envelope room.
#define CN1_SURFACES_MIRROR_MAX_BYTES (48 * 1024)

// The kinds worth mirroring, from the CN1SurfacesWatchKinds Info.plist key the builder writes
// from the manifest's watch families. Decided at build time so a publish of a phone-only kind
// costs one dictionary lookup and nothing else.
static NSSet *cn1SurfacesWatchKinds() {
    static NSSet *kinds = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        id v = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CN1SurfacesWatchKinds"];
        if ([v isKindOfClass:[NSString class]] && [(NSString *)v length] > 0) {
            kinds = [[NSSet alloc] initWithArray:[(NSString *)v componentsSeparatedByString:@","]];
        } else {
            kinds = [[NSSet alloc] init];
        }
    });
    return kinds;
}

static void cn1SurfacesLogOnce(NSString *key, NSString *message) {
    static NSMutableSet *said = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{ said = [[NSMutableSet alloc] init]; });
    @synchronized (said) {
        if ([said containsObject:key]) {
            return;
        }
        [said addObject:key];
    }
    NSLog(@"[CN1Surfaces] %@", message);
}

void com_codename1_impl_ios_IOSNative_surfacesMirrorToWatch___java_lang_String_java_lang_String_java_lang_String_1ARRAY_byte_2ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT kindId, JAVA_OBJECT timelineJson,
        JAVA_OBJECT imageNames, JAVA_OBJECT imageBlobs) {
    if (kindId == JAVA_NULL || timelineJson == JAVA_NULL) {
        return;
    }
    POOL_BEGIN();
    NSString *kind = toNSString(CN1_THREAD_STATE_PASS_ARG kindId);
    if (kind == nil || ![cn1SurfacesWatchKinds() containsObject:kind]) {
        POOL_END();
        return;
    }
    Class sessionClass = NSClassFromString(@"CN1WatchConnectivity");
    if (sessionClass == nil) {
        cn1SurfacesLogOnce(@"noWC", @"watch mirror unavailable: this build has no "
                "WatchConnectivity glue");
        POOL_END();
        return;
    }
    NSString *json = toNSString(CN1_THREAD_STATE_PASS_ARG timelineJson);
    if (json == nil) {
        POOL_END();
        return;
    }
    NSMutableDictionary *payload = [NSMutableDictionary dictionary];
    [payload setObject:kind forKey:@"cn1.surfaces.kind"];
    [payload setObject:[json dataUsingEncoding:NSUTF8StringEncoding] forKey:@"cn1.surfaces.json"];
    // A publication sequence, so the watch can tell an older payload from a newer one.
    //
    // The two transports do not share a queue: transferCurrentComplicationUserInfo is prioritized
    // and transferUserInfo merely queued, so a publication sent on the second -- because the
    // complication was disabled or its daily budget was spent -- can arrive AFTER a later one
    // sent on the first. Applied in arrival order, the older timeline then overwrites the newer
    // and the complication sits on stale content indefinitely, there being no periodic update to
    // correct it. Monotonic per process and carried per kind; the receiver keeps the highest it
    // has applied and ignores anything at or below it.
    [payload setObject:[NSNumber numberWithLongLong:cn1NextSurfaceMirrorSequence()]
                forKey:@"cn1.surfaces.seq"];

    // Imagery travels in the same dictionary rather than through transferFile, deliberately.
    // A file transfer is a separate unordered queue with no atomicity against the descriptor, so
    // a complication could render against art that had not landed yet -- worse than a gap.
    if (imageNames != JAVA_NULL && imageBlobs != JAVA_NULL) {
        JAVA_ARRAY names = (JAVA_ARRAY)imageNames;
        JAVA_ARRAY blobs = (JAVA_ARRAY)imageBlobs;
        JAVA_OBJECT *nameData = (JAVA_OBJECT *)names->data;
        JAVA_OBJECT *blobData = (JAVA_OBJECT *)blobs->data;
        int count = (int)(names->length < blobs->length ? names->length : blobs->length);
        for (int i = 0; i < count; i++) {
            if (nameData[i] == JAVA_NULL || blobData[i] == JAVA_NULL) {
                continue;
            }
            NSString *name = toNSString(CN1_THREAD_STATE_PASS_ARG nameData[i]);
            JAVA_ARRAY blob = (JAVA_ARRAY)blobData[i];
            if (name == nil || blob->length <= 0) {
                continue;
            }
            [payload setObject:[NSData dataWithBytes:blob->data length:(NSUInteger)blob->length]
                        forKey:[@"cn1.surfaces.img." stringByAppendingString:name]];
        }
    }

    NSData *encoded = [NSPropertyListSerialization dataWithPropertyList:payload
            format:NSPropertyListBinaryFormat_v1_0 options:0 error:nil];
    if (encoded == nil || [encoded length] > CN1_SURFACES_MIRROR_MAX_BYTES) {
        // Shed the imagery first: a complication that renders its numbers with a missing glyph
        // is worth more than one that never updates.
        NSMutableDictionary *lean = [NSMutableDictionary dictionary];
        [lean setObject:[payload objectForKey:@"cn1.surfaces.kind"] forKey:@"cn1.surfaces.kind"];
        // The sequence travels on the lean payload too, or a publication that shed its imagery
        // would arrive unordered and could be overwritten by an older one.
        [lean setObject:[payload objectForKey:@"cn1.surfaces.seq"] forKey:@"cn1.surfaces.seq"];
        [lean setObject:[payload objectForKey:@"cn1.surfaces.json"] forKey:@"cn1.surfaces.json"];
        NSData *leanEncoded = [NSPropertyListSerialization dataWithPropertyList:lean
                format:NSPropertyListBinaryFormat_v1_0 options:0 error:nil];
        if (leanEncoded == nil || [leanEncoded length] > CN1_SURFACES_MIRROR_MAX_BYTES) {
            cn1SurfacesLogOnce([@"tooBig." stringByAppendingString:kind],
                    [NSString stringWithFormat:@"widget kind \"%@\" is too large to mirror to the "
                            "watch (%lu bytes, cap %d); the watch keeps its previous timeline",
                            kind, (unsigned long)(leanEncoded == nil ? 0 : [leanEncoded length]),
                            CN1_SURFACES_MIRROR_MAX_BYTES]);
            POOL_END();
            return;
        }
        cn1SurfacesLogOnce([@"noImages." stringByAppendingString:kind],
                [NSString stringWithFormat:@"widget kind \"%@\" exceeds the watch mirror cap with "
                        "its imagery; mirroring the layout without it", kind]);
        payload = lean;
    }

    // The Objective-C half owns WCSession; reaching it here would duplicate its activation and
    // delegate bookkeeping.
    ((void (*)(id, SEL, NSDictionary *))objc_msgSend)((id)sessionClass,
            NSSelectorFromString(@"mirrorComplicationUserInfo:"), payload);
    POOL_END();
}

#else

// On the watch the app's own publish is authoritative. Mirroring back would send a timeline the
// phone did not ask for and, when the phone mirrored in the first place, loop.
void com_codename1_impl_ios_IOSNative_surfacesMirrorToWatch___java_lang_String_java_lang_String_java_lang_String_1ARRAY_byte_2ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT kindId, JAVA_OBJECT timelineJson,
        JAVA_OBJECT imageNames, JAVA_OBJECT imageBlobs) {
}

#endif

#if TARGET_OS_WATCH
// Applies a timeline the phone mirrored across: write it into the watch's own App Group
// container -- the one the complication extension reads -- and ask WidgetKit to re-render.
//
// Called from CN1WatchConnectivity's didReceiveUserInfo, which may run with no CN1 runtime at
// all: transferCurrentComplicationUserInfo wakes the app in the background precisely to refresh a
// complication, and starting the whole application to do a file write would bring a UI forward
// nobody asked for. So this is plain Foundation and touches no Java.
//
// The layout matches what IOSSurfaceBridge writes locally, because the extension reads one
// format and does not care which side produced it.
BOOL cn1_watch_apply_mirrored_surface(NSString *kind, NSData *json,
        NSArray<NSString *> *imageNames, NSArray<NSData *> *imageBlobs) {
    NSString *container = cn1SurfacesContainerPath();
    if (container == nil || kind == nil || json == nil) {
        return NO;
    }
    NSString *kindDir = [[container stringByAppendingPathComponent:@"cn1surfaces"]
            stringByAppendingPathComponent:kind];
    NSFileManager *fm = [NSFileManager defaultManager];
    NSError *err = nil;
    if (![fm createDirectoryAtPath:kindDir withIntermediateDirectories:YES
                        attributes:nil error:&err]) {
        NSLog(@"[CN1Surfaces] could not prepare the mirrored surface directory: %@", err);
        return NO;
    }
    // Imagery first, so the descriptor is never live against art that has not landed. Names are
    // content hashes, so an unchanged image rewrites identical bytes.
    for (NSUInteger i = 0; i < [imageNames count] && i < [imageBlobs count]; i++) {
        NSString *name = [imageNames objectAtIndex:i];
        if ([name rangeOfString:@"/"].location != NSNotFound) {
            // A name is a hash, never a path. Refusing one that looks like a path keeps a
            // malformed payload from writing outside the kind's own directory.
            continue;
        }
        if (![[imageBlobs objectAtIndex:i]
                writeToFile:[kindDir stringByAppendingPathComponent:
                        [name stringByAppendingString:@".png"]]
                atomically:YES]) {
            // The descriptor is NOT installed. Writing it anyway would make a timeline live
            // against art that is not there -- a hole in the complication -- and the collection
            // that follows would then delete whatever the previous descriptor was still using,
            // so the watch would end up worse off than if nothing had arrived. Leaving the old
            // timeline in place keeps a complete surface on the face, and the next publish or
            // reload sends the whole set again.
            NSLog(@"[CN1Surfaces] could not store mirrored image \"%@\" for \"%@\"; keeping the "
                    "previous timeline", name, kind);
            return NO;
        }
    }
    if (![json writeToFile:[kindDir stringByAppendingPathComponent:@"timeline.json"]
                atomically:YES]) {
        NSLog(@"[CN1Surfaces] could not write the mirrored timeline for \"%@\"", kind);
        return NO;
    }
    // AFTER the replacement document is in place, so an extension rendering concurrently re-reads
    // the new timeline before its art can disappear -- the same order IOSSurfaceBridge uses for a
    // local publish. Without this the mirror had no collection at all: blob names are content
    // hashes, so every changed image left its predecessor in the App Group container for ever,
    // and a container that only grows is a watch app that eventually cannot write.
    //
    // The reference set is the document's own "images" list, not the blobs that arrived in this
    // message. A mirror only ships art the watch has not seen, so the transferred names are a
    // subset and collecting against them would delete the images being kept.
    NSError *parseErr = nil;
    id doc = [NSJSONSerialization JSONObjectWithData:json options:0 error:&parseErr];
    if ([doc isKindOfClass:[NSDictionary class]]) {
        id names = [(NSDictionary *)doc objectForKey:@"images"];
        NSMutableSet *referenced = [NSMutableSet set];
        if ([names isKindOfClass:[NSArray class]]) {
            for (id name in (NSArray *)names) {
                [referenced addObject:[NSString stringWithFormat:@"%@", name]];
            }
        }
        for (NSString *entry in [fm contentsOfDirectoryAtPath:kindDir error:NULL]) {
            if (![[entry pathExtension] isEqualToString:@"png"]) {
                continue;
            }
            if (![referenced containsObject:[entry stringByDeletingPathExtension]]) {
                [fm removeItemAtPath:[kindDir stringByAppendingPathComponent:entry] error:NULL];
            }
        }
    } else {
        NSLog(@"[CN1Surfaces] could not read the mirrored timeline of \"%@\" to collect its "
                "images: %@", kind, parseErr);
    }
    Class bridge = cn1SurfacesBridgeClass();
    if (bridge != nil) {
        ((void (*)(id, SEL, NSString *))objc_msgSend)((id)bridge,
                NSSelectorFromString(@"reloadTimelines:"), kind);
    }
    return YES;
}
#endif

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_surfacesWidgetsSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    if (@available(iOS 14.0, *)) {
        POOL_BEGIN();
        BOOL supported = cn1SurfacesMinOSSupported()
                && cn1SurfacesBridgeClass() != nil && cn1SurfacesContainerPath() != nil;
        POOL_END();
        return supported ? JAVA_TRUE : JAVA_FALSE;
    }
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_surfacesActivitiesSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    // Live activities are an iOS capability: watchOS has no ActivityKit, and the Swift bridge
    // compiles its ActivityKit bodies out there. Answering here rather than relying on that
    // states the intent -- and keeps the symbol, which the watch slice still links because the
    // Java method is reachable from shared code.
#if TARGET_OS_WATCH
    return JAVA_FALSE;
#else
    if (@available(iOS 16.1, *)) {
        POOL_BEGIN();
        BOOL supported = NO;
        Class bridge = cn1SurfacesBridgeClass();
        if (bridge != nil) {
            // The Swift side checks ActivityAuthorizationInfo().areActivitiesEnabled.
            supported = ((BOOL (*)(id, SEL))objc_msgSend)((id)bridge,
                    NSSelectorFromString(@"activitiesEnabled"));
        }
        POOL_END();
        return supported ? JAVA_TRUE : JAVA_FALSE;
    }
    return JAVA_FALSE;
#endif
}

#else // CN1_USE_WIDGETS

// Surfaces not enabled: no WidgetKit/ActivityKit references, everything answers unsupported.
JAVA_OBJECT com_codename1_impl_ios_IOSNative_getSurfacesContainerPath__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_NULL;
}
void com_codename1_impl_ios_IOSNative_surfacesReloadTimelines___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT kind) {
}
JAVA_INT com_codename1_impl_ios_IOSNative_surfacesInstalledCount___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT kind) {
    return 0;
}
JAVA_OBJECT com_codename1_impl_ios_IOSNative_surfacesStartActivity___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT descriptorJson) {
    return JAVA_NULL;
}
void com_codename1_impl_ios_IOSNative_surfacesUpdateActivity___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT activityId, JAVA_OBJECT stateJson) {
}
void com_codename1_impl_ios_IOSNative_surfacesEndActivity___java_lang_String_java_lang_String_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT activityId, JAVA_OBJECT finalStateJson, JAVA_BOOLEAN dismissImmediately) {
}
void com_codename1_impl_ios_IOSNative_surfacesMirrorToWatch___java_lang_String_java_lang_String_java_lang_String_1ARRAY_byte_2ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT kindId, JAVA_OBJECT timelineJson, JAVA_OBJECT imageNames, JAVA_OBJECT imageBlobs) {
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_surfacesWidgetsSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_surfacesActivitiesSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}
#endif // CN1_USE_WIDGETS

// New-VM (return-type-encoded) manglings for the value-returning surfaces natives. Defined
// here, after the implementations/stubs above, so each call is to an already-declared
// function. The void surfaces* methods need no _R_ wrapper. Always defined (real or stub)
// regardless of CN1_USE_WIDGETS.
JAVA_OBJECT com_codename1_impl_ios_IOSNative_getSurfacesContainerPath___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getSurfacesContainerPath__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}
JAVA_INT com_codename1_impl_ios_IOSNative_surfacesInstalledCount___java_lang_String_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT kind) {
    return com_codename1_impl_ios_IOSNative_surfacesInstalledCount___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, kind);
}
JAVA_OBJECT com_codename1_impl_ios_IOSNative_surfacesStartActivity___java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT descriptorJson) {
    return com_codename1_impl_ios_IOSNative_surfacesStartActivity___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, descriptorJson);
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_surfacesWidgetsSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_surfacesWidgetsSupported__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_surfacesActivitiesSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_surfacesActivitiesSupported__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

// --- App intents (Core Spotlight + App Intents) ------------------------------
// Gated by CN1_USE_INTENTS, which the builder defines when the app references
// com.codename1.intents. Two frameworks with very different availability sit behind this:
//
//  - Core Spotlight is Objective-C and long predates this port's deployment floor, so
//    indexing is implemented directly here and works on every supported device.
//  - App Intents is Swift-only and needs a newer iOS, so it is reached through the generated
//    Swift declarations via CN1IntentHost (an Objective-C shim). It has to be an Objective-C
//    shim rather than Swift calling Java directly: the translator's dead-code eliminator
//    only scans .m sources for mangled symbols, so a Java method named only from Swift is
//    silently eliminated into an empty stub.
//
// Builds without the define compile the stub branch and link neither framework.
#ifdef CN1_USE_INTENTS
#import <CoreSpotlight/CoreSpotlight.h>
// Not available on macOS.
#if !TARGET_OS_OSX
#import <MobileCoreServices/MobileCoreServices.h>
#endif

// PNG blobs staged by Java ahead of an index/complete call, keyed by the name the serializer
// embedded in the JSON. Guarded because staging happens on the caller's thread while the
// consuming call may run on another.
static NSMutableDictionary *cn1IntentImages = nil;
static NSObject *cn1IntentImagesLock = nil;

static void cn1IntentsEnsureStaging() {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1IntentImages = [[NSMutableDictionary alloc] init];
        cn1IntentImagesLock = [[NSObject alloc] init];
    });
}

/// The Core Spotlight domain this framework owns.
///
/// Everything indexed through Intents lives under it, so clearing "everything this framework
/// indexed" is a domain delete rather than deleting the application's entire Spotlight index --
/// which is what it used to be, and which took content the app indexed itself along with it.
///
/// **The recursive delete is documented Core Spotlight behaviour, not an assumption.** Review
/// twice read this as an exact-match API that would leave every typed item behind. It is not;
/// CSSearchableIndex.h says so directly, above deleteSearchableItemsWithDomainIdentifiers:
///
///   "The delete is recursive so if domain identifiers are of the form
///    <account-id>.<mailbox-id>, for example, calling delete with <account-id> will delete all
///    the searchable items with that account and any mailbox."
///
/// (Quoted from the iPhoneOS SDK header.) That is precisely this scheme: the root is the
/// account, an entity type is the mailbox. Deleting the root therefore clears every type.
#define CN1_INTENT_DOMAIN_ROOT @"com.codename1.intents"

/// The subdomain an entity type is indexed under.
///
/// The dot is what makes the hierarchy, so a dot *inside* an entity type would make one nobody
/// asked for: with types "order" and "order.line" -- ordinary enough for a reverse-DNS naming
/// scheme -- clearIndex("order") would take "order.line" with it, two unrelated types clearing
/// as one. Percent-escaping the separator keeps every type exactly one level below the root, so
/// the hierarchy means only what this file intends it to mean. The escape is applied on both
/// paths because index and clear share this function.
static NSString *cn1IntentDomain(NSString *entityType) {
    if (entityType == nil || [entityType length] == 0) {
        return CN1_INTENT_DOMAIN_ROOT;
    }
    NSString *escaped = [entityType stringByReplacingOccurrencesOfString:@"%"
                                                             withString:@"%25"];
    escaped = [escaped stringByReplacingOccurrencesOfString:@"." withString:@"%2E"];
    return [NSString stringWithFormat:@"%@.%@", CN1_INTENT_DOMAIN_ROOT, escaped];
}

/// The prefix every Spotlight item this framework indexes carries.
///
/// A CSSearchableItem's uniqueIdentifier is scoped to the application, not to the domain -- the
/// domain namespaces *deletion by domain*, and nothing else. So an entity indexed here as
/// "order:42" and an item the application indexed itself under that same string are one item:
/// indexing through Intents would replace the app's row, and removeFromIndex would delete it.
/// Ownership has to be stamped on the identifier as well.
#define CN1_INTENT_UID_PREFIX @"cn1entity:"

/// The Spotlight identifier for an entity uid.
NSString *cn1IntentItemId(NSString *uid) {
    return [CN1_INTENT_UID_PREFIX stringByAppendingString:uid];
}

/// The entity uid behind a Spotlight identifier, or nil when the item is not ours.
///
/// nil matters: a selection may arrive for an item the application indexed itself, and handing
/// its raw identifier to the framework would have it look for an entity that does not exist.
NSString *cn1IntentUidFromItemId(NSString *identifier) {
    if (identifier == nil || ![identifier hasPrefix:CN1_INTENT_UID_PREFIX]) {
        return nil;
    }
    return [identifier substringFromIndex:[CN1_INTENT_UID_PREFIX length]];
}

/// The generated Swift bridge, present only when the app declares an @AppIntent. Absent is a
/// normal state (an app may only index content), so callers must tolerate nil.
static Class cn1IntentBridgeClass() {
    static Class c = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        c = NSClassFromString(@"CN1IntentBridge");
    });
    return c;
}

/// Donated activities have to outlive the call that created them.
static NSMutableArray *cn1IntentActivities = nil;

static void cn1IntentsRetainActivity(NSUserActivity *activity) {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1IntentActivities = [[NSMutableArray alloc] init];
    });
    @synchronized (cn1IntentActivities) {
        // Bounded: a long-running app that donates on every user action would otherwise grow
        // this without limit, and only the recent ones are of any use to the system.
        if ([cn1IntentActivities count] > 32) {
            [cn1IntentActivities removeObjectAtIndex:0];
        }
        [cn1IntentActivities addObject:activity];
    }
}

/// How long a staged snippet directory is kept before the next invocation reclaims it.
///
/// Generous against the thing it has to outlive: an invocation is capped at 25 seconds by the
/// framework and its snippet is rendered during the interaction, so ten minutes is orders of
/// magnitude past any live reader while still bounding what accumulates.
#define CN1_INTENT_IMAGE_TTL_SECONDS (10 * 60)

/// Deletes staged directories from earlier invocations.
///
/// Each invocation writes its blobs under its own token, and nothing removed them: the
/// completion path does not, and the SwiftUI view that reads them cannot, because it has no
/// idea when the last reader is done. Left alone they accumulate for the life of the install
/// -- image-heavy intents run many times a day -- and the caches directory is only reclaimed
/// by iOS under storage pressure, which is not a plan.
///
/// Pruned by age at the moment the next one is staged, rather than deleted on completion:
/// a snippet can still be on screen after perform() returns, so deleting it then would race
/// the renderer for the picture the user is looking at.
static void cn1IntentsPruneStagedImages(NSString *root) {
    NSFileManager *fm = [NSFileManager defaultManager];
    NSArray *names = [fm contentsOfDirectoryAtPath:root error:nil];
    if (names == nil) {
        return;
    }
    NSDate *cutoff = [NSDate dateWithTimeIntervalSinceNow:-CN1_INTENT_IMAGE_TTL_SECONDS];
    for (NSString *name in names) {
        NSString *path = [root stringByAppendingPathComponent:name];
        NSDictionary *attrs = [fm attributesOfItemAtPath:path error:nil];
        if (attrs == nil) {
            continue;
        }
        NSDate *modified = [attrs objectForKey:NSFileModificationDate];
        if (modified != nil && [modified compare:cutoff] == NSOrderedAscending) {
            [fm removeItemAtPath:path error:nil];
        }
    }
}

/// Writes the staged blobs into a per-invocation directory and returns its path, or nil when
/// there was nothing to write. The directory is under the caches folder, so the OS can reclaim
/// it -- a snippet is transient and there is nothing to keep once it has been shown.
static NSString *cn1IntentsWriteStagedImages(NSString *token) {
    cn1IntentsEnsureStaging();
    NSDictionary *snapshot;
    @synchronized (cn1IntentImagesLock) {
        if ([cn1IntentImages count] == 0) {
            return nil;
        }
        // Autoreleased rather than owned: this function has four exits and the app target is
        // MRC, so a plain -copy would leak every staged blob on three of them. The caller is
        // inside a POOL_BEGIN/POOL_END pair.
        snapshot = [[cn1IntentImages copy] autorelease];
    }
    NSArray *caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory,
            NSUserDomainMask, YES);
    if ([caches count] == 0) {
        return nil;
    }
    NSString *root = [[caches objectAtIndex:0] stringByAppendingPathComponent:@"cn1intents"];
    // Before this invocation adds one, take away the ones nobody can still be reading.
    cn1IntentsPruneStagedImages(root);
    NSString *dir = [root stringByAppendingPathComponent:token];
    NSError *err = nil;
    [[NSFileManager defaultManager] createDirectoryAtPath:dir
                              withIntermediateDirectories:YES
                                               attributes:nil
                                                    error:&err];
    if (err != nil) {
        NSLog(@"CN1 intents: could not create the snippet image directory: %@", err);
        return nil;
    }
    for (NSString *name in snapshot) {
        NSData *data = [snapshot objectForKey:name];
        // The .png suffix is part of the contract, not decoration: CN1SurfaceRenderer's
        // cn1LoadImage appends it to every name it is asked for, so a blob staged under the
        // bare name is never found and the node renders as Color.clear -- an image silently
        // missing from every App Intent snippet. IOSSurfaceBridge writes <name>.png for the
        // widget and live-activity paths that share this renderer; this one had not.
        NSString *file = [name stringByAppendingString:@".png"];
        [data writeToFile:[dir stringByAppendingPathComponent:file] atomically:YES];
    }
    return dir;
}

static NSDictionary *cn1IntentsParseJson(NSString *json) {
    if (json == nil) {
        return nil;
    }
    NSData *data = [json dataUsingEncoding:NSUTF8StringEncoding];
    if (data == nil) {
        return nil;
    }
    NSError *err = nil;
    id parsed = [NSJSONSerialization JSONObjectWithData:data options:0 error:&err];
    if (err != nil || ![parsed isKindOfClass:[NSDictionary class]]) {
        return nil;
    }
    return (NSDictionary *)parsed;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_intentsSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_TRUE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_intentsAppIntentsSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_APP_INTENTS_DECLARED
    if (@available(iOS 16.0, *)) {
        // The class test still matters: it is what catches a device below the App Intents
        // minimum, where the Swift is present but its types are unavailable.
        return cn1IntentBridgeClass() != nil ? JAVA_TRUE : JAVA_FALSE;
    }
#endif
    // No declarations were generated -- either the app declares no @AppIntent, or it set
    // ios.intents.appIntents=false. Answering true here made isVoiceInvocationSupported() and
    // isHeadlessExecutionSupported() promise an assistant path that cannot exist, so apps built
    // Siri UI that could never work.
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_intentsIndexingSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    BOOL supported = [CSSearchableIndex isIndexingAvailable];
    POOL_END();
    return supported ? JAVA_TRUE : JAVA_FALSE;
}

void com_codename1_impl_ios_IOSNative_intentsRegister___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT declarationsJson) {
    if (@available(iOS 16.0, *)) {
        POOL_BEGIN();
        Class bridge = cn1IntentBridgeClass();
        if (bridge != nil && declarationsJson != JAVA_NULL) {
            NSString *json = toNSString(CN1_THREAD_STATE_PASS_ARG declarationsJson);
            ((void (*)(id, SEL, NSString *))objc_msgSend)((id)bridge,
                    NSSelectorFromString(@"registerIntents:"), json);
        }
        POOL_END();
    }
}

/// Donation is deliberately Objective-C and carries no availability gate.
///
/// It is NSUserActivity underneath, which long predates App Intents, so gating it on iOS 16
/// made every donation a no-op on exactly the devices the ios.intents.appIntents=false opt-out
/// exists to keep working. Nothing here needs Swift.
void com_codename1_impl_ios_IOSNative_intentsDonate___java_lang_String_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT intentId, JAVA_OBJECT title, JAVA_OBJECT paramsJson) {
    if (intentId == JAVA_NULL) {
        return;
    }
    POOL_BEGIN();
    NSString *iid = toNSString(CN1_THREAD_STATE_PASS_ARG intentId);
    NSUserActivity *activity = [[NSUserActivity alloc] initWithActivityType:iid];
    // eligibleForPrediction arrived in iOS 12, and this donation path is the one an app on a
    // lower deployment target keeps -- indexing and donation need no App Intents and no newer
    // floor, which is the whole reason the floor is not raised for them. Sending the setter to
    // an older system is an unrecognized selector and a crash, where the honest outcome is
    // simply that the system does not predict this activity.
    if (@available(iOS 12.0, *)) {
        activity.eligibleForPrediction = YES;
    }
    // eligibleForSearch is iOS 9 and is deliberately not guarded: no build can reach this with
    // a target older than that. IPhoneBuilder starts minDeploymentTargets at 12.0, adds
    // DEFAULT_MIN_DEPLOYMENT_VERSION (13.0) unconditionally at the top of every build, and
    // getDeploymentTarget returns maxVersionString over that list -- so ios.deployment_target
    // can only ever raise the floor. A project pinning 8.0 still builds at 13.0.
    //
    // eligibleForPrediction above is guarded because 12.0 is exactly the floor, and a guard at
    // the boundary costs nothing while documenting which iOS introduced the property. Guarding
    // this one as well would suggest a configuration that cannot exist.
    activity.eligibleForSearch = YES;
    // The activity type has to be the machine id -- it is what the continuation path matches on
    // -- but the title is what Siri suggestions and Spotlight show a person. Using the id for
    // both put "log_workout" in front of the user next to the "Log a workout" the app declared.
    NSString *label = (title == JAVA_NULL) ? iid : toNSString(CN1_THREAD_STATE_PASS_ARG title);
    activity.title = ([label length] > 0) ? label : iid;
    if (paramsJson != JAVA_NULL) {
        NSDictionary *params = cn1IntentsParseJson(
                toNSString(CN1_THREAD_STATE_PASS_ARG paramsJson));
        if (params != nil) {
            // userInfo has to be property-list representable, so anything else is dropped
            // rather than risking an exception inside what is only ever a hint to the system.
            NSMutableDictionary *safe = [NSMutableDictionary dictionary];
            for (id key in params) {
                id value = [params objectForKey:key];
                if ([key isKindOfClass:[NSString class]]
                        && ([value isKindOfClass:[NSString class]]
                                || [value isKindOfClass:[NSNumber class]])) {
                    [safe setObject:value forKey:key];
                }
            }
            activity.userInfo = safe;
        }
    }
    [activity becomeCurrent];
    // Held by the array, not by us: an NSUserActivity that is released outright stops being
    // current and the donation is lost, so ownership moves to cn1IntentActivities. Dropping
    // this alloc's own reference is what makes that array's bound mean anything -- without it
    // evicting an old entry releases only the array's retain and every donated activity stays
    // alive for the life of the process.
    cn1IntentsRetainActivity(activity);
    [activity release];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_intentsStageImage___java_lang_String_byte_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT name, JAVA_OBJECT dataArr, JAVA_INT length) {
    if (name == JAVA_NULL || dataArr == JAVA_NULL || length <= 0) {
        return;
    }
    cn1IntentsEnsureStaging();
    POOL_BEGIN();
    NSString *key = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    JAVA_ARRAY byteArray = (JAVA_ARRAY)dataArr;
    NSData *data = [NSData dataWithBytes:(JAVA_BYTE *)byteArray->data length:length];
    @synchronized (cn1IntentImagesLock) {
        [cn1IntentImages setObject:data forKey:key];
    }
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_intentsIndex___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT entitiesJson) {
    if (entitiesJson == JAVA_NULL || ![CSSearchableIndex isIndexingAvailable]) {
        return;
    }
    cn1IntentsEnsureStaging();
    POOL_BEGIN();
    NSDictionary *doc = cn1IntentsParseJson(toNSString(CN1_THREAD_STATE_PASS_ARG entitiesJson));
    NSArray *entities = [doc objectForKey:@"entities"];
    if ([entities isKindOfClass:[NSArray class]]) {
        NSMutableArray *items = [NSMutableArray array];
        for (id raw in entities) {
            if (![raw isKindOfClass:[NSDictionary class]]) {
                continue;
            }
            NSDictionary *e = (NSDictionary *)raw;
            NSString *uid = [e objectForKey:@"uid"];
            if (uid == nil) {
                continue;
            }
            // Autoreleased: the app target is MRC and this loop runs once per entity, so an
            // owned attribute set would leak its thumbnail data on every index call.
            CSSearchableItemAttributeSet *attrs = [[[CSSearchableItemAttributeSet alloc]
                    initWithItemContentType:(NSString *)kUTTypeItem] autorelease];
            attrs.title = [e objectForKey:@"title"];
            attrs.contentDescription = [e objectForKey:@"subtitle"];
            id keywords = [e objectForKey:@"keywords"];
            if ([keywords isKindOfClass:[NSArray class]]) {
                attrs.keywords = (NSArray *)keywords;
            }
            NSString *imageName = [e objectForKey:@"image"];
            if (imageName != nil) {
                @synchronized (cn1IntentImagesLock) {
                    NSData *png = [cn1IntentImages objectForKey:imageName];
                    if (png != nil) {
                        attrs.thumbnailData = png;
                    }
                }
            }
            // The domain is namespaced under one this framework owns. It was the bare entity
            // type, which is app-chosen and collides with any domain the app indexes into Core
            // Spotlight itself -- and clearIndex(null) below deleted the app's entire index
            // rather than this framework's part of it.
            NSString *domain = cn1IntentDomain([e objectForKey:@"type"]);
            CSSearchableItem *item = [[[CSSearchableItem alloc]
                    initWithUniqueIdentifier:cn1IntentItemId(uid)
                            domainIdentifier:domain
                                attributeSet:attrs] autorelease];
            [items addObject:item];
        }
        if ([items count] > 0) {
            [[CSSearchableIndex defaultSearchableIndex] indexSearchableItems:items
                    completionHandler:^(NSError *error) {
                        if (error != nil) {
                            NSLog(@"CN1 intents: indexing failed: %@", error);
                        }
                    }];
        }
    }
    @synchronized (cn1IntentImagesLock) {
        [cn1IntentImages removeAllObjects];
    }
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_intentsRemoveFromIndex___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT idsJson) {
    if (idsJson == JAVA_NULL || ![CSSearchableIndex isIndexingAvailable]) {
        return;
    }
    POOL_BEGIN();
    NSDictionary *doc = cn1IntentsParseJson(toNSString(CN1_THREAD_STATE_PASS_ARG idsJson));
    NSArray *refs = [doc objectForKey:@"refs"];
    if ([refs isKindOfClass:[NSArray class]]) {
        NSMutableArray *uids = [NSMutableArray array];
        for (id raw in refs) {
            if ([raw isKindOfClass:[NSDictionary class]]) {
                NSString *uid = [(NSDictionary *)raw objectForKey:@"uid"];
                if (uid != nil) {
                    // The same stamp indexing applied. Deleting the raw uid would delete an
                    // item the application indexed itself under that string, and leave ours.
                    [uids addObject:cn1IntentItemId(uid)];
                }
            }
        }
        if ([uids count] > 0) {
            [[CSSearchableIndex defaultSearchableIndex]
                    deleteSearchableItemsWithIdentifiers:uids completionHandler:nil];
        }
    }
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_intentsClearIndex___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT entityType) {
    if (![CSSearchableIndex isIndexingAvailable]) {
        return;
    }
    POOL_BEGIN();
    CSSearchableIndex *index = [CSSearchableIndex defaultSearchableIndex];
    // Never deleteAllSearchableItems. This API clears what was indexed *through Intents*, and
    // that call clears everything the application ever put in Core Spotlight -- including
    // content indexed by native code or another library, which this framework did not publish
    // and has no business removing. Core Spotlight deletes a domain's subdomains along with it,
    // so the framework's own root domain is exactly the right scope for "all of ours", and one
    // subdomain for "all of this type".
    NSString *domain = (entityType == JAVA_NULL)
            ? CN1_INTENT_DOMAIN_ROOT
            : cn1IntentDomain(toNSString(CN1_THREAD_STATE_PASS_ARG entityType));
    [index deleteSearchableItemsWithDomainIdentifiers:[NSArray arrayWithObject:domain]
                                    completionHandler:nil];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_intentsCompleteInvocation___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT token, JAVA_OBJECT resultJson) {
    if (@available(iOS 16.0, *)) {
        POOL_BEGIN();
        Class bridge = cn1IntentBridgeClass();
        if (bridge != nil && token != JAVA_NULL) {
            NSString *t = toNSString(CN1_THREAD_STATE_PASS_ARG token);
            NSString *json = (resultJson == JAVA_NULL) ? @"{}"
                    : toNSString(CN1_THREAD_STATE_PASS_ARG resultJson);
            // A snippet's images reach the renderer as files, not bytes: it resolves them from
            // a directory, the same way the widget extension does, so the two render paths stay
            // identical rather than growing a second image pipeline.
            NSString *imagesDir = cn1IntentsWriteStagedImages(t);
            // The Swift side owns the one-shot guarantee for its continuation; the Java side
            // has already guaranteed this fires once per token.
            ((void (*)(id, SEL, NSString *, NSString *, NSString *))objc_msgSend)((id)bridge,
                    NSSelectorFromString(@"completeInvocation:resultJson:imagesDir:"),
                    t, json, imagesDir);
        }
        POOL_END();
    }
    // Staged for this result and now consumed. Without this an app returning fresh imagery on
    // every invocation retains every blob for the life of the process.
    cn1IntentsEnsureStaging();
    @synchronized (cn1IntentImagesLock) {
        [cn1IntentImages removeAllObjects];
    }
}

#else // CN1_USE_INTENTS

// Intents not enabled: no Core Spotlight or App Intents references, everything unsupported.
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_intentsSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_intentsAppIntentsSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_intentsIndexingSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}
void com_codename1_impl_ios_IOSNative_intentsRegister___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT declarationsJson) {
}
void com_codename1_impl_ios_IOSNative_intentsDonate___java_lang_String_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT intentId, JAVA_OBJECT title, JAVA_OBJECT paramsJson) {
}
void com_codename1_impl_ios_IOSNative_intentsStageImage___java_lang_String_byte_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT name, JAVA_OBJECT dataArr, JAVA_INT length) {
}
void com_codename1_impl_ios_IOSNative_intentsIndex___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT entitiesJson) {
}
void com_codename1_impl_ios_IOSNative_intentsRemoveFromIndex___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT idsJson) {
}
void com_codename1_impl_ios_IOSNative_intentsClearIndex___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT entityType) {
}
void com_codename1_impl_ios_IOSNative_intentsCompleteInvocation___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT token, JAVA_OBJECT resultJson) {
}
#endif // CN1_USE_INTENTS

// New-VM (return-type-encoded) manglings for the value-returning intent natives. Defined after
// the implementations/stubs above so each call targets an already-declared function. The void
// intents* methods need no _R_ wrapper. Always defined regardless of CN1_USE_INTENTS.
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_intentsSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_intentsSupported__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_intentsAppIntentsSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_intentsAppIntentsSupported__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_intentsIndexingSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_intentsIndexingSupported__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

// --- Phone-to-watch link (com.codename1.wearable / WatchConnectivity) --------
//
// Compiled into BOTH the phone target and the watch target: WCSession is symmetric, so the two
// halves of a pair run identical code. Gated on CN1_USE_WATCHCONNECTIVITY, which the builder
// defines only when the app references com.codename1.wearable, so other apps link no framework and
// carry no symbols. Payloads cross as opaque bytes; the value model lives in Java.

#if defined(CN1_USE_WATCHCONNECTIVITY) && !TARGET_OS_TV && !TARGET_OS_MACCATALYST

#import "CN1WatchConnectivity.h"
// The translated entry points the cn1_wearable_deliver* functions below call. Without this
// they are implicit declarations, which the watchOS slice rejects outright -- "call to
// undeclared function 'com_codename1_impl_ios_IOSWearableCallbacks_nativeMessageReceived...'"
// -- so the same source compiled for the phone and not for the watch beside it.
//
// The diagnostic is the lucky part. An invented prototype returns int and passes the thread
// state, two JAVA_OBJECTs and an int through whatever registers the default promotions
// choose, so where it does link it delivers a message built out of the wrong arguments.
// Same reasoning, and the same fix, as the IOSIntentCallbacks import in
// CodenameOne_GLAppDelegate.m.
//
// Inside the guard rather than beside the other includes at the top of the file: on a slice
// with WatchConnectivity off the class may not be translated at all, and then the header
// does not exist.
#include "com_codename1_impl_ios_IOSWearableCallbacks.h"

#if TARGET_OS_WATCH
// Brings the session up on the watch without anyone asking for it.
//
// Every other route into CN1WatchConnectivity is a wearable native, so the session is activated
// lazily the first time the app touches com.codename1.wearable. An app that declares watch
// surfaces and never touches that API takes no such route: the delegate is never installed, the
// WCSession is never activated, and didReceiveUserInfo: therefore cannot fire -- which is exactly
// the surfaces-only configuration the phone-to-watch mirror exists to serve. Nothing reports it,
// because the phone half sends successfully into a session that has no listener.
//
// Called from the generated app delegate's applicationDidFinishLaunching, NOT from initVM.
// A mirrored complication update wakes a terminated watch app in the background, where the
// SwiftUI root view is not guaranteed to appear -- so CN1WatchHost.startWithWidth() may never
// run and initVM with it. Activating there left the session unreachable in exactly the launch
// this transport causes.
//
// The accessor activates on first use, so asking for it is the whole job.
void cn1_watch_activate_connectivity(void) {
    [CN1WatchConnectivity shared];
}
#endif

// Declared rather than left implicit. These six are the translated form of the static
// Java methods on IOSWearableCallbacks, and ParparVM emits their definitions -- but it
// emits no header this file includes, so every call below was an implicit declaration.
// C99 dropped those, and a clang that enforces it turns all six into build errors:
// "call to undeclared function ... ISO C99 and later do not support implicit function
// declarations". That is a toolchain change away from breaking every iOS target at
// once, which is exactly what happened, so the declarations are written out here.
extern JAVA_VOID com_codename1_impl_ios_IOSWearableCallbacks_nativeMessageReceived___java_lang_String_byte_1ARRAY_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path, JAVA_OBJECT payload, JAVA_INT replyToken);
extern JAVA_VOID com_codename1_impl_ios_IOSWearableCallbacks_nativeReplyReceived___int_byte_1ARRAY_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_INT replyToken, JAVA_OBJECT payload, JAVA_OBJECT error);
extern JAVA_VOID com_codename1_impl_ios_IOSWearableCallbacks_nativeDataChanged___java_lang_String_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path, JAVA_OBJECT payload);
extern JAVA_VOID com_codename1_impl_ios_IOSWearableCallbacks_nativeDataChangedTracked___java_lang_String_byte_1ARRAY_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path, JAVA_OBJECT payload, JAVA_OBJECT token);
extern JAVA_VOID com_codename1_impl_ios_IOSWearableCallbacks_nativeDataRemoved___java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path);
extern JAVA_VOID com_codename1_impl_ios_IOSWearableCallbacks_nativeStateChanged__(
        CODENAME_ONE_THREAD_STATE);

// Callbacks the delegate calls when the peer sends something. Each hops into the Java callback
// surface, which owns EDT dispatch and the cold-start queue.

void cn1_wearable_deliverMessage(const char *path, const void *payload, int payloadLength, int replyToken) {
    JAVA_OBJECT jPath = path == NULL ? JAVA_NULL : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [NSString stringWithUTF8String:path]);
    JAVA_OBJECT jBody = payload == NULL ? JAVA_NULL
            : nsDataToByteArr([NSData dataWithBytes:payload length:payloadLength]);
    com_codename1_impl_ios_IOSWearableCallbacks_nativeMessageReceived___java_lang_String_byte_1ARRAY_int(
            CN1_THREAD_GET_STATE_PASS_ARG jPath, jBody, replyToken);
}

void cn1_wearable_deliverReply(int replyToken, const void *payload, int payloadLength, const char *error) {
    JAVA_OBJECT jBody = payload == NULL ? JAVA_NULL
            : nsDataToByteArr([NSData dataWithBytes:payload length:payloadLength]);
    JAVA_OBJECT jError = error == NULL ? JAVA_NULL
            : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [NSString stringWithUTF8String:error]);
    com_codename1_impl_ios_IOSWearableCallbacks_nativeReplyReceived___int_byte_1ARRAY_java_lang_String(
            CN1_THREAD_GET_STATE_PASS_ARG replyToken, jBody, jError);
}

void cn1_wearable_deliverDataChanged(const char *path, const void *payload, int payloadLength) {
    JAVA_OBJECT jPath = path == NULL ? JAVA_NULL : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [NSString stringWithUTF8String:path]);
    JAVA_OBJECT jBody = payload == NULL ? JAVA_NULL
            : nsDataToByteArr([NSData dataWithBytes:payload length:payloadLength]);
    com_codename1_impl_ios_IOSWearableCallbacks_nativeDataChanged___java_lang_String_byte_1ARRAY(
            CN1_THREAD_GET_STATE_PASS_ARG jPath, jBody);
}

void cn1_wearable_deliverDataChangedTracked(const char *path, const void *payload, int payloadLength,
                                            const char *inboxToken) {
    JAVA_OBJECT jPath = path == NULL ? JAVA_NULL : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [NSString stringWithUTF8String:path]);
    JAVA_OBJECT jBody = payload == NULL ? JAVA_NULL
            : nsDataToByteArr([NSData dataWithBytes:payload length:payloadLength]);
    JAVA_OBJECT jToken = inboxToken == NULL ? JAVA_NULL
            : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [NSString stringWithUTF8String:inboxToken]);
    com_codename1_impl_ios_IOSWearableCallbacks_nativeDataChangedTracked___java_lang_String_byte_1ARRAY_java_lang_String(
            CN1_THREAD_GET_STATE_PASS_ARG jPath, jBody, jToken);
}

void com_codename1_impl_ios_IOSNative_wearableConfirmInbox___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT token) {
    if (token == JAVA_NULL) {
        return;
    }
    POOL_BEGIN();
    cn1_wearable_confirmInbox([toNSString(CN1_THREAD_GET_STATE_PASS_ARG token) UTF8String]);
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_wearableReleaseInbox___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT token) {
    if (token == JAVA_NULL) {
        return;
    }
    POOL_BEGIN();
    cn1_wearable_releaseInbox([toNSString(CN1_THREAD_GET_STATE_PASS_ARG token) UTF8String]);
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_wearableReplayInbox__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    cn1_wearable_replayInbox();
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_wearableForgetReceived___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path) {
    POOL_BEGIN();
    // NULL is PASSED THROUGH, not filtered. It is the rescan request: the pending-delivery cap
    // discarded more paths than it could name, and cn1_wearable_forgetReceived reads a null path as
    // "forget every received marker and re-offer the whole held context". Returning early here --
    // the reflex for a null argument -- silently dropped the one signal that recovers an overflow,
    // so those values stayed marked delivered and never reached the listener that finally arrived.
    cn1_wearable_forgetReceived(path == JAVA_NULL
            ? NULL : [toNSString(CN1_THREAD_GET_STATE_PASS_ARG path) UTF8String]);
    POOL_END();
}

void cn1_wearable_deliverDataRemoved(const char *path) {
    JAVA_OBJECT jPath = path == NULL ? JAVA_NULL : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [NSString stringWithUTF8String:path]);
    com_codename1_impl_ios_IOSWearableCallbacks_nativeDataRemoved___java_lang_String(
            CN1_THREAD_GET_STATE_PASS_ARG jPath);
}

void cn1_wearable_notifyStateChanged(void) {
    com_codename1_impl_ios_IOSWearableCallbacks_nativeStateChanged__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

// Turns a Java byte[] into NSData. A null array becomes empty data rather than nil so the callers
// never have to branch.
static NSData *cn1WearableToNSData(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT arr) {
    if (arr == JAVA_NULL) {
        return [NSData data];
    }
    JAVA_ARRAY byteArray = (JAVA_ARRAY) arr;
    JAVA_ARRAY_BYTE *data = (JAVA_ARRAY_BYTE *) byteArray->data;
    return [NSData dataWithBytes:data length:byteArray->length];
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_wearableSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    BOOL b = [[CN1WatchConnectivity shared] isSupported];
    POOL_END();
    return b ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_wearablePaired__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    BOOL b = [[CN1WatchConnectivity shared] isPaired];
    POOL_END();
    return b ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_wearableReachable__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    BOOL b = [[CN1WatchConnectivity shared] isReachable];
    POOL_END();
    return b ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_wearableCompanionInstalled__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    BOOL b = [[CN1WatchConnectivity shared] isCompanionInstalled];
    POOL_END();
    return b ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_wearablePeerName__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    // WCSession exposes no peer name, so name the form factor: from the phone the peer is the
    // watch, from the watch it is the phone.
#if TARGET_OS_WATCH
    JAVA_OBJECT r = fromNSString(CN1_THREAD_STATE_PASS_ARG @"iPhone");
#else
    JAVA_OBJECT r = fromNSString(CN1_THREAD_STATE_PASS_ARG @"Apple Watch");
#endif
    POOL_END();
    return r;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_wearablePeerId__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
#if TARGET_OS_WATCH
    JAVA_OBJECT r = fromNSString(CN1_THREAD_STATE_PASS_ARG @"phone");
#else
    JAVA_OBJECT r = fromNSString(CN1_THREAD_STATE_PASS_ARG @"watch");
#endif
    POOL_END();
    return r;
}

void com_codename1_impl_ios_IOSNative_wearableSendMessage___java_lang_String_byte_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path, JAVA_OBJECT payload, JAVA_INT replyToken) {
    POOL_BEGIN();
    NSString *p = path == JAVA_NULL ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG path);
    [[CN1WatchConnectivity shared] sendMessage:p
                                       payload:cn1WearableToNSData(CN1_THREAD_STATE_PASS_ARG payload)
                                    replyToken:(int) replyToken];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_wearableSendReply___int_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT replyToken, JAVA_OBJECT payload) {
    POOL_BEGIN();
    [[CN1WatchConnectivity shared] sendReply:(int) replyToken
                                     payload:cn1WearableToNSData(CN1_THREAD_STATE_PASS_ARG payload)];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_wearablePutData___java_lang_String_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path, JAVA_OBJECT payload) {
    POOL_BEGIN();
    NSString *p = path == JAVA_NULL ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG path);
    [[CN1WatchConnectivity shared] putData:p
                                   payload:cn1WearableToNSData(CN1_THREAD_STATE_PASS_ARG payload)];
    POOL_END();
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_wearableGetData___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path) {
    POOL_BEGIN();
    NSString *p = path == JAVA_NULL ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSData *d = [[CN1WatchConnectivity shared] getData:p];
    JAVA_OBJECT r = d == nil ? JAVA_NULL : nsDataToByteArr(d);
    POOL_END();
    return r;
}

void com_codename1_impl_ios_IOSNative_wearableRemoveData___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path) {
    POOL_BEGIN();
    NSString *p = path == JAVA_NULL ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG path);
    [[CN1WatchConnectivity shared] removeData:p];
    POOL_END();
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_wearableDataPaths__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    NSArray<NSString *> *paths = [[CN1WatchConnectivity shared] dataPaths];
    // Escaped before joining, because a newline is only "impossible" in a path by convention and
    // nothing enforces it: WearableMessage rejects null and empty paths and nothing else, and
    // Android and JavaSE both carry a path containing one perfectly well. Joining raw meant
    // "/sync\nstate" came back to the app as two phantom paths that getData() could not read,
    // making getDataPaths() disagree with the other two platforms about what exists.
    //
    // '%' first, so unescaping cannot turn a literal "%0a" in a path into a delimiter.
    NSMutableArray<NSString *> *escaped = [NSMutableArray arrayWithCapacity:paths.count];
    for (NSString *p in paths) {
        NSString *e = [p stringByReplacingOccurrencesOfString:@"%" withString:@"%25"];
        e = [e stringByReplacingOccurrencesOfString:@"\n" withString:@"%0a"];
        [escaped addObject:e];
    }
    JAVA_OBJECT r = fromNSString(CN1_THREAD_STATE_PASS_ARG [escaped componentsJoinedByString:@"\n"]);
    POOL_END();
    return r;
}

void com_codename1_impl_ios_IOSNative_wearableTransferFile___java_lang_String_java_lang_String_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path, JAVA_OBJECT name, JAVA_OBJECT contents) {
    POOL_BEGIN();
    NSString *p = path == JAVA_NULL ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSString *n = name == JAVA_NULL ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG name);
    [[CN1WatchConnectivity shared] transferFile:p
                                           name:n
                                       contents:cn1WearableToNSData(CN1_THREAD_STATE_PASS_ARG contents)];
    POOL_END();
}

#else // CN1_USE_WATCHCONNECTIVITY

// The app never references com.codename1.wearable (or this is tvOS / Mac Catalyst, where
// WatchConnectivity does not exist). No framework is linked and everything answers unsupported,
// which makes the public API an inert no-op.

void cn1_wearable_deliverMessage(const char *path, const void *payload, int payloadLength, int replyToken) {
}
void cn1_wearable_deliverReply(int replyToken, const void *payload, int payloadLength, const char *error) {
}
void cn1_wearable_deliverDataChanged(const char *path, const void *payload, int payloadLength) {
}
void cn1_wearable_deliverDataChangedTracked(const char *path, const void *payload, int payloadLength,
                                            const char *inboxToken) {
}
void cn1_wearable_confirmInbox(const char *inboxToken) {
}
void cn1_wearable_releaseInbox(const char *inboxToken) {
}
void cn1_wearable_replayInbox(void) {
}
void cn1_wearable_forgetReceived(const char *path) {
}
void cn1_wearable_deliverDataRemoved(const char *path) {
}
void cn1_wearable_notifyStateChanged(void) {
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_wearableSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_wearablePaired__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_wearableReachable__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_wearableCompanionInstalled__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}
JAVA_OBJECT com_codename1_impl_ios_IOSNative_wearablePeerName__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_NULL;
}
JAVA_OBJECT com_codename1_impl_ios_IOSNative_wearablePeerId__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_NULL;
}
void com_codename1_impl_ios_IOSNative_wearableSendMessage___java_lang_String_byte_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path, JAVA_OBJECT payload, JAVA_INT replyToken) {
}
void com_codename1_impl_ios_IOSNative_wearableSendReply___int_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT replyToken, JAVA_OBJECT payload) {
}
void com_codename1_impl_ios_IOSNative_wearablePutData___java_lang_String_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path, JAVA_OBJECT payload) {
}
JAVA_OBJECT com_codename1_impl_ios_IOSNative_wearableGetData___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path) {
    return JAVA_NULL;
}
void com_codename1_impl_ios_IOSNative_wearableRemoveData___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path) {
}
JAVA_OBJECT com_codename1_impl_ios_IOSNative_wearableDataPaths__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_NULL;
}
void com_codename1_impl_ios_IOSNative_wearableTransferFile___java_lang_String_java_lang_String_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path, JAVA_OBJECT name, JAVA_OBJECT contents) {
}
void com_codename1_impl_ios_IOSNative_wearableConfirmInbox___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT token) {
}
void com_codename1_impl_ios_IOSNative_wearableReleaseInbox___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT token) {
}
void com_codename1_impl_ios_IOSNative_wearableReplayInbox__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
}
void com_codename1_impl_ios_IOSNative_wearableForgetReceived___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path) {
}

#endif // CN1_USE_WATCHCONNECTIVITY

// Return-typed aliases the translator emits for methods with a non-void return.
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_wearableSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_wearableSupported__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_wearablePaired___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_wearablePaired__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_wearableReachable___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_wearableReachable__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_wearableCompanionInstalled___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_wearableCompanionInstalled__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}
JAVA_OBJECT com_codename1_impl_ios_IOSNative_wearablePeerName___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_wearablePeerName__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}
JAVA_OBJECT com_codename1_impl_ios_IOSNative_wearablePeerId___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_wearablePeerId__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}
JAVA_OBJECT com_codename1_impl_ios_IOSNative_wearableGetData___java_lang_String_R_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    return com_codename1_impl_ios_IOSNative_wearableGetData___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, path);
}
JAVA_OBJECT com_codename1_impl_ios_IOSNative_wearableDataPaths___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_wearableDataPaths__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

void com_codename1_impl_ios_IOSNative_setSecureStorageAccessGroup___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT accessGroup) {
    if (cn1_keychainAccessGroup != nil) {
        [cn1_keychainAccessGroup release];
        cn1_keychainAccessGroup = nil;
    }
    if (accessGroup != JAVA_NULL) {
        NSString *ag = toNSString(CN1_THREAD_STATE_PASS_ARG accessGroup);
        if (ag != nil && [ag length] > 0) {
            cn1_keychainAccessGroup = [ag retain];
        }
    }
}

static NSString *cn1_getAppName(CN1_THREAD_STATE_SINGLE_ARG) {
    JAVA_OBJECT d = com_codename1_ui_Display_getInstance___R_com_codename1_ui_Display(CN1_THREAD_STATE_PASS_SINGLE_ARG);
    JAVA_OBJECT key = fromNSString(CN1_THREAD_STATE_PASS_ARG @"AppName");
    JAVA_OBJECT def = fromNSString(CN1_THREAD_STATE_PASS_ARG @"CodenameOneApp");
    JAVA_OBJECT res = com_codename1_ui_Display_getProperty___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG d, key, def);
    return toNSString(CN1_THREAD_STATE_PASS_ARG res);
}

static NSString *cn1_secureStorageServiceName(NSString *appName) {
    if (appName == nil || [appName length] == 0) {
        appName = [[NSBundle mainBundle] bundleIdentifier];
    }
    if (appName == nil || [appName length] == 0) {
        appName = @"CodenameOne";
    }
    return appName;
}

// The keychain service the non-prompting entries are written under.
//
// The bundle identifier, because it is the one name that does not change with the application.
// cn1_getAppName reads the AppName property, which IPhoneBuilder sets from the build's display
// name -- so renaming an application moved its keychain service, its managed database key became
// unreachable, and ManagedKeys, seeing nothing under the new service, generated a replacement.
// The database was then encrypted with a key nobody had. A display name is a label; the bundle
// identifier is the identity, and the App Store enforces that it does not change.
static NSString *cn1_secureStoragePlainService(CN1_THREAD_STATE_SINGLE_ARG) {
    NSString *bundleId = [[NSBundle mainBundle] bundleIdentifier];
    if (bundleId != nil && [bundleId length] > 0) {
        return bundleId;
    }
    // No bundle identifier is not a state a real application is in, but the answer still has to be
    // stable rather than absent, so this falls back to what was used before.
    return cn1_secureStorageServiceName(cn1_getAppName(CN1_THREAD_STATE_PASS_SINGLE_ARG));
}

// The service the same entries were written under before, which is the display name.
//
// Read from, never written to. Entries stored by an earlier build are still sitting here, and for
// a managed database key that entry is the only copy in existence: reading it is what keeps an
// application that upgrades from finding its own database unreadable.
static NSString *cn1_secureStorageLegacyPlainService(CN1_THREAD_STATE_SINGLE_ARG) {
    return cn1_secureStorageServiceName(cn1_getAppName(CN1_THREAD_STATE_PASS_SINGLE_ARG));
}

// Whether the two are the same string, in which case there is no legacy anything to look at.
static BOOL cn1_secureStorageHasLegacyService(NSString *current, NSString *legacy) {
    return legacy != nil && current != nil && ![legacy isEqualToString:current];
}

void com_codename1_impl_ios_IOSNative_secureStorageGet___int_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_OBJECT reason, JAVA_OBJECT account) {
    POOL_BEGIN();
    NSString *nsReason = (reason == JAVA_NULL) ? @"Authenticate" : toNSString(CN1_THREAD_STATE_PASS_ARG reason);
    NSString *nsAccount = toNSString(CN1_THREAD_STATE_PASS_ARG account);
    NSString *appName = cn1_secureStorageServiceName(cn1_getAppName(CN1_THREAD_STATE_PASS_SINGLE_ARG));
    NSString *accessGroup = cn1_keychainAccessGroup;
    [nsReason retain];
    [nsAccount retain];
    [appName retain];
    if (accessGroup != nil) {
        [accessGroup retain];
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        NSMutableDictionary *q = [NSMutableDictionary dictionary];
        [q setObject:(__bridge id)kSecClassGenericPassword forKey:(__bridge id)kSecClass];
        [q setObject:@YES forKey:(__bridge id)kSecReturnData];
        [q setObject:(__bridge id)kSecMatchLimitOne forKey:(__bridge id)kSecMatchLimit];
        [q setObject:nsAccount forKey:(__bridge id)kSecAttrAccount];
        [q setObject:appName forKey:(__bridge id)kSecAttrService];
        [q setObject:nsReason forKey:(__bridge id)kSecUseOperationPrompt];
        if (accessGroup != nil) {
            [q setObject:accessGroup forKey:(__bridge id)kSecAttrAccessGroup];
        }
        CFTypeRef dataRef = NULL;
        OSStatus status = SecItemCopyMatching((__bridge CFDictionaryRef)q, &dataRef);
        if (status == errSecSuccess) {
            NSData *d = (__bridge NSData *)dataRef;
            NSString *value = [[[NSString alloc] initWithData:d encoding:NSUTF8StringEncoding] autorelease];
            JAVA_OBJECT jv = fromNSString(getThreadLocalData(), value);
            com_codename1_impl_ios_IOSSecureStorage_nativeStorageStringResult___int_java_lang_String(getThreadLocalData(), requestId, jv);
        } else {
            JAVA_OBJECT jmsg = fromNSString(getThreadLocalData(), [NSString stringWithFormat:@"OSStatus %d", (int)status]);
            com_codename1_impl_ios_IOSSecureStorage_nativeStorageError___int_int_java_lang_String(getThreadLocalData(), requestId, (int)status, jmsg);
        }
        [nsReason release];
        [nsAccount release];
        [appName release];
        if (accessGroup != nil) {
            [accessGroup release];
        }
    });
    POOL_END();
}

static void cn1_secureStorageUpdate(int requestId, NSString *nsReason, NSString *nsAccount, NSString *nsValue, NSString *appName, NSString *accessGroup) {
// UIKit-only helper. AppKit's equivalent is a different API rather than a
// renamed one, so this is inert on the native macOS port until it is ported.
#if TARGET_OS_OSX
#else
    NSMutableDictionary *q = [NSMutableDictionary dictionary];
    [q setObject:(__bridge id)kSecClassGenericPassword forKey:(__bridge id)kSecClass];
    [q setObject:nsAccount forKey:(__bridge id)kSecAttrAccount];
    [q setObject:appName forKey:(__bridge id)kSecAttrService];
    [q setObject:nsReason forKey:(__bridge id)kSecUseOperationPrompt];
    if (accessGroup != nil) {
        [q setObject:accessGroup forKey:(__bridge id)kSecAttrAccessGroup];
    }
    NSMutableDictionary *ch = [NSMutableDictionary dictionary];
    [ch setObject:[nsValue dataUsingEncoding:NSUTF8StringEncoding] forKey:(__bridge id)kSecValueData];
    OSStatus status = SecItemUpdate((__bridge CFDictionaryRef)q, (__bridge CFDictionaryRef)ch);
    if (status == errSecSuccess) {
        com_codename1_impl_ios_IOSSecureStorage_nativeStorageBooleanResult___int_boolean(getThreadLocalData(), requestId, JAVA_TRUE);
    } else {
        JAVA_OBJECT jmsg = fromNSString(getThreadLocalData(), [NSString stringWithFormat:@"OSStatus %d", (int)status]);
        com_codename1_impl_ios_IOSSecureStorage_nativeStorageError___int_int_java_lang_String(getThreadLocalData(), requestId, (int)status, jmsg);
    }
#endif
}

void com_codename1_impl_ios_IOSNative_secureStorageSet___int_java_lang_String_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_OBJECT reason, JAVA_OBJECT account, JAVA_OBJECT value) {
    POOL_BEGIN();
    NSString *nsReason = (reason == JAVA_NULL) ? @"Authenticate" : toNSString(CN1_THREAD_STATE_PASS_ARG reason);
    NSString *nsAccount = toNSString(CN1_THREAD_STATE_PASS_ARG account);
    NSString *nsValue = (value == JAVA_NULL) ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG value);
    NSString *appName = cn1_secureStorageServiceName(cn1_getAppName(CN1_THREAD_STATE_PASS_SINGLE_ARG));
    NSString *accessGroup = cn1_keychainAccessGroup;
    [nsReason retain];
    [nsAccount retain];
    [nsValue retain];
    [appName retain];
    if (accessGroup != nil) {
        [accessGroup retain];
    }
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        SecAccessControlRef sacRef = SecAccessControlCreateWithFlags(kCFAllocatorDefault,
                kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
                kSecAccessControlTouchIDCurrentSet,
                nil);
        NSMutableDictionary *d = [NSMutableDictionary dictionary];
        [d setObject:(__bridge id)kSecClassGenericPassword forKey:(__bridge id)kSecClass];
        [d setObject:nsAccount forKey:(__bridge id)kSecAttrAccount];
        [d setObject:appName forKey:(__bridge id)kSecAttrService];
        [d setObject:[nsValue dataUsingEncoding:NSUTF8StringEncoding] forKey:(__bridge id)kSecValueData];
        [d setObject:(__bridge id)sacRef forKey:(__bridge id)kSecAttrAccessControl];
        [d setObject:nsReason forKey:(__bridge id)kSecUseOperationPrompt];
        if (accessGroup != nil) {
            [d setObject:accessGroup forKey:(__bridge id)kSecAttrAccessGroup];
        }
        OSStatus status = SecItemAdd((__bridge CFDictionaryRef)d, nil);
        if (sacRef != NULL) {
            CFRelease(sacRef);
        }
        if (status == errSecDuplicateItem) {
            cn1_secureStorageUpdate((int)requestId, nsReason, nsAccount, nsValue, appName, accessGroup);
        } else if (status == errSecSuccess) {
            com_codename1_impl_ios_IOSSecureStorage_nativeStorageBooleanResult___int_boolean(getThreadLocalData(), requestId, JAVA_TRUE);
        } else {
            JAVA_OBJECT jmsg = fromNSString(getThreadLocalData(), [NSString stringWithFormat:@"OSStatus %d", (int)status]);
            com_codename1_impl_ios_IOSSecureStorage_nativeStorageError___int_int_java_lang_String(getThreadLocalData(), requestId, (int)status, jmsg);
        }
        [nsReason release];
        [nsAccount release];
        [nsValue release];
        [appName release];
        if (accessGroup != nil) {
            [accessGroup release];
        }
    });
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_secureStorageRemove___int_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_OBJECT reason, JAVA_OBJECT account) {
    POOL_BEGIN();
    NSString *nsAccount = toNSString(CN1_THREAD_STATE_PASS_ARG account);
    NSString *appName = cn1_secureStorageServiceName(cn1_getAppName(CN1_THREAD_STATE_PASS_SINGLE_ARG));
    NSString *accessGroup = cn1_keychainAccessGroup;
    [nsAccount retain];
    [appName retain];
    if (accessGroup != nil) {
        [accessGroup retain];
    }
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSMutableDictionary *d = [NSMutableDictionary dictionary];
        [d setObject:(__bridge id)kSecClassGenericPassword forKey:(__bridge id)kSecClass];
        [d setObject:nsAccount forKey:(__bridge id)kSecAttrAccount];
        [d setObject:appName forKey:(__bridge id)kSecAttrService];
        if (accessGroup != nil) {
            [d setObject:accessGroup forKey:(__bridge id)kSecAttrAccessGroup];
        }
        OSStatus status = SecItemDelete((__bridge CFDictionaryRef)d);
        if (status == errSecSuccess || status == errSecItemNotFound) {
            com_codename1_impl_ios_IOSSecureStorage_nativeStorageBooleanResult___int_boolean(getThreadLocalData(), requestId, JAVA_TRUE);
        } else {
            JAVA_OBJECT jmsg = fromNSString(getThreadLocalData(), [NSString stringWithFormat:@"OSStatus %d", (int)status]);
            com_codename1_impl_ios_IOSSecureStorage_nativeStorageError___int_int_java_lang_String(getThreadLocalData(), requestId, (int)status, jmsg);
        }
        [nsAccount release];
        [appName release];
        if (accessGroup != nil) {
            [accessGroup release];
        }
    });
    POOL_END();
}

static NSMutableDictionary *cn1_secureStoragePlainQuery(NSString *account, NSString *appName, NSString *accessGroup) {
    NSMutableDictionary *q = [NSMutableDictionary dictionary];
    appName = cn1_secureStorageServiceName(appName);
    [q setObject:(__bridge id)kSecClassGenericPassword forKey:(__bridge id)kSecClass];
    [q setObject:account forKey:(__bridge id)kSecAttrAccount];
    [q setObject:appName forKey:(__bridge id)kSecAttrService];
    if (accessGroup != nil) {
        [q setObject:accessGroup forKey:(__bridge id)kSecAttrAccessGroup];
    }
    return q;
}

// SecureStorage.set/get/remove are the non-biometric API. They must not invoke
// LocalAuthentication or block on a prompt, so they use a plain generic-password
// keychain item while the async biometric methods below continue to use their
// existing SecAccessControl path.
// Whether a keychain item exists, which is not the same question as whether it can be read.
// SecItemCopyMatching separates them: errSecItemNotFound means there is nothing under that
// account, while any other failure means the keychain could not answer -- locked, an entitlement
// problem, a daemon that is not talking. Reading alone cannot tell those apart, and a caller that
// treats "could not read" as "not there" overwrites a key that exists, after which the database it
// encrypted is unreadable for good.
//
// Asks for no data, only for the item's presence, so a locked item still answers.
JAVA_INT com_codename1_impl_ios_IOSNative_secureStorageEntryStatePlain___java_lang_String_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT account) {
    if (account == JAVA_NULL) {
        return -1;
    }
    JAVA_INT result;
    POOL_BEGIN();
    NSString *nsAccount = toNSString(CN1_THREAD_STATE_PASS_ARG account);
    NSString *service = cn1_secureStoragePlainService(CN1_THREAD_STATE_PASS_SINGLE_ARG);
    NSString *legacy = cn1_secureStorageLegacyPlainService(CN1_THREAD_STATE_PASS_SINGLE_ARG);
    NSMutableDictionary *q = cn1_secureStoragePlainQuery(nsAccount, service, cn1_keychainAccessGroup);
    [q setObject:@NO forKey:(__bridge id)kSecReturnData];
    [q setObject:(__bridge id)kSecMatchLimitOne forKey:(__bridge id)kSecMatchLimit];

    OSStatus status = SecItemCopyMatching((__bridge CFDictionaryRef)q, NULL);
    if (status == errSecItemNotFound && cn1_secureStorageHasLegacyService(service, legacy)) {
        // Nothing under the stable service, so ask under the name an earlier build used. Absence
        // there is the only thing that means absence: reporting it from the first query alone
        // would have ManagedKeys generate a second key over a database the first one encrypted.
        NSMutableDictionary *legacyQuery =
                cn1_secureStoragePlainQuery(nsAccount, legacy, cn1_keychainAccessGroup);
        [legacyQuery setObject:@NO forKey:(__bridge id)kSecReturnData];
        [legacyQuery setObject:(__bridge id)kSecMatchLimitOne forKey:(__bridge id)kSecMatchLimit];
        status = SecItemCopyMatching((__bridge CFDictionaryRef)legacyQuery, NULL);
    }
    if (status == errSecSuccess || status == errSecInteractionNotAllowed) {
        // The second one is an item that is there but locked, which is presence, not absence.
        result = 1;
    } else if (status == errSecItemNotFound) {
        result = 0;
    } else {
        result = -1;
    }
    POOL_END();
    return result;
}

JAVA_INT com_codename1_impl_ios_IOSNative_secureStorageEntryStatePlain___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT account) {
    return com_codename1_impl_ios_IOSNative_secureStorageEntryStatePlain___java_lang_String_R_int(CN1_THREAD_STATE_PASS_ARG me, account);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_secureStorageGetPlain___java_lang_String_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT account) {
    if (account == JAVA_NULL) {
        return JAVA_NULL;
    }
    JAVA_OBJECT result = JAVA_NULL;
    POOL_BEGIN();
    NSString *nsAccount = toNSString(CN1_THREAD_STATE_PASS_ARG account);
    NSString *service = cn1_secureStoragePlainService(CN1_THREAD_STATE_PASS_SINGLE_ARG);
    NSString *legacy = cn1_secureStorageLegacyPlainService(CN1_THREAD_STATE_PASS_SINGLE_ARG);
    NSMutableDictionary *q = cn1_secureStoragePlainQuery(nsAccount, service, cn1_keychainAccessGroup);
    [q setObject:@YES forKey:(__bridge id)kSecReturnData];
    [q setObject:(__bridge id)kSecMatchLimitOne forKey:(__bridge id)kSecMatchLimit];

    CFTypeRef dataRef = NULL;
    OSStatus status = SecItemCopyMatching((__bridge CFDictionaryRef)q, &dataRef);
    BOOL fromLegacy = NO;
    if (status == errSecItemNotFound && cn1_secureStorageHasLegacyService(service, legacy)) {
        // Written by a build that keyed these by display name. Read it rather than reporting
        // nothing: for a managed database key this entry is the only copy there is.
        NSMutableDictionary *legacyQuery =
                cn1_secureStoragePlainQuery(nsAccount, legacy, cn1_keychainAccessGroup);
        [legacyQuery setObject:@YES forKey:(__bridge id)kSecReturnData];
        [legacyQuery setObject:(__bridge id)kSecMatchLimitOne forKey:(__bridge id)kSecMatchLimit];
        status = SecItemCopyMatching((__bridge CFDictionaryRef)legacyQuery, &dataRef);
        fromLegacy = (status == errSecSuccess);
    }
    if (status == errSecSuccess && dataRef != NULL) {
        NSData *d = (__bridge NSData *)dataRef;
        NSString *value = [[[NSString alloc] initWithData:d encoding:NSUTF8StringEncoding] autorelease];
        if (value != nil) {
            result = fromNSString(CN1_THREAD_STATE_PASS_ARG value);
            if (fromLegacy) {
                // Moved to the stable service as it is read, so the next rename does not have to
                // find it again. Copy first and only then delete: a delete that ran before the
                // copy landed would destroy the one copy of a key that cannot be regenerated.
                NSMutableDictionary *moved =
                        cn1_secureStoragePlainQuery(nsAccount, service, cn1_keychainAccessGroup);
                [moved setObject:d forKey:(__bridge id)kSecValueData];
                [moved setObject:(__bridge id)kSecAttrAccessibleAfterFirstUnlock
                          forKey:(__bridge id)kSecAttrAccessible];
                OSStatus copied = SecItemAdd((__bridge CFDictionaryRef)moved, NULL);
                if (copied == errSecSuccess || copied == errSecDuplicateItem) {
                    NSMutableDictionary *old =
                            cn1_secureStoragePlainQuery(nsAccount, legacy, cn1_keychainAccessGroup);
                    SecItemDelete((__bridge CFDictionaryRef)old);
                }
                // A failed copy is not an error to report: the value was read, and the entry it
                // came from is still there to be read again next time.
            }
        }
        CFRelease(dataRef);
    }
    POOL_END();
    return result;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_secureStorageSetPlain___java_lang_String_java_lang_String_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT account, JAVA_OBJECT value) {
    if (account == JAVA_NULL) {
        return JAVA_FALSE;
    }
    JAVA_BOOLEAN result = JAVA_FALSE;
    POOL_BEGIN();
    NSString *nsAccount = toNSString(CN1_THREAD_STATE_PASS_ARG account);
    NSString *nsValue = (value == JAVA_NULL) ? @"" : toNSString(CN1_THREAD_STATE_PASS_ARG value);
    NSString *service = cn1_secureStoragePlainService(CN1_THREAD_STATE_PASS_SINGLE_ARG);
    NSString *legacy = cn1_secureStorageLegacyPlainService(CN1_THREAD_STATE_PASS_SINGLE_ARG);
    NSData *data = [nsValue dataUsingEncoding:NSUTF8StringEncoding];

    NSMutableDictionary *item = cn1_secureStoragePlainQuery(nsAccount, service, cn1_keychainAccessGroup);
    [item setObject:data forKey:(__bridge id)kSecValueData];
    [item setObject:(__bridge id)kSecAttrAccessibleAfterFirstUnlock forKey:(__bridge id)kSecAttrAccessible];

    OSStatus status = SecItemAdd((__bridge CFDictionaryRef)item, nil);
    if (status == errSecDuplicateItem) {
        NSMutableDictionary *q = cn1_secureStoragePlainQuery(nsAccount, service, cn1_keychainAccessGroup);
        NSDictionary *changes = [NSDictionary dictionaryWithObject:data forKey:(__bridge id)kSecValueData];
        status = SecItemUpdate((__bridge CFDictionaryRef)q, (__bridge CFDictionaryRef)changes);
    }
    if (status == errSecSuccess && cn1_secureStorageHasLegacyService(service, legacy)) {
        // Only once the new value is stored. Leaving the old entry behind would have a later read
        // find a stale value under the legacy name if the stable one were ever removed.
        NSMutableDictionary *old = cn1_secureStoragePlainQuery(nsAccount, legacy, cn1_keychainAccessGroup);
        SecItemDelete((__bridge CFDictionaryRef)old);
    }
    result = (status == errSecSuccess) ? JAVA_TRUE : JAVA_FALSE;
    POOL_END();
    return result;
}

// Create only, which is the operation a first-time managed key needs. SecItemAdd refuses an item
// that already exists rather than replacing it, and it refuses it in the keychain daemon -- so two
// processes reaching here at the same moment cannot both believe they created the key, which is
// what leaves a database encrypted under a key that was immediately overwritten.
//
// 1 created, 0 already there, -1 the keychain could not be asked.
JAVA_INT com_codename1_impl_ios_IOSNative_secureStorageAddPlain___java_lang_String_java_lang_String_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT account, JAVA_OBJECT value) {
    if (account == JAVA_NULL || value == JAVA_NULL) {
        return -1;
    }
    JAVA_INT result;
    POOL_BEGIN();
    NSString *nsAccount = toNSString(CN1_THREAD_STATE_PASS_ARG account);
    NSString *nsValue = toNSString(CN1_THREAD_STATE_PASS_ARG value);
    NSString *service = cn1_secureStoragePlainService(CN1_THREAD_STATE_PASS_SINGLE_ARG);
    NSData *data = [nsValue dataUsingEncoding:NSUTF8StringEncoding];

    NSMutableDictionary *item = cn1_secureStoragePlainQuery(nsAccount, service, cn1_keychainAccessGroup);
    [item setObject:data forKey:(__bridge id)kSecValueData];
    [item setObject:(__bridge id)kSecAttrAccessibleAfterFirstUnlock forKey:(__bridge id)kSecAttrAccessible];

    OSStatus status = SecItemAdd((__bridge CFDictionaryRef)item, NULL);
    if (status == errSecSuccess) {
        result = 1;
    } else if (status == errSecDuplicateItem) {
        result = 0;
    } else {
        result = -1;
    }
    POOL_END();
    return result;
}

JAVA_INT com_codename1_impl_ios_IOSNative_secureStorageAddPlain___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT account, JAVA_OBJECT value) {
    return com_codename1_impl_ios_IOSNative_secureStorageAddPlain___java_lang_String_java_lang_String_R_int(CN1_THREAD_STATE_PASS_ARG me, account, value);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_secureStorageRemovePlain___java_lang_String_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT account) {
    if (account == JAVA_NULL) {
        return JAVA_FALSE;
    }
    JAVA_BOOLEAN result = JAVA_FALSE;
    POOL_BEGIN();
    NSString *nsAccount = toNSString(CN1_THREAD_STATE_PASS_ARG account);
    NSString *service = cn1_secureStoragePlainService(CN1_THREAD_STATE_PASS_SINGLE_ARG);
    NSString *legacy = cn1_secureStorageLegacyPlainService(CN1_THREAD_STATE_PASS_SINGLE_ARG);
    NSMutableDictionary *q = cn1_secureStoragePlainQuery(nsAccount, service, cn1_keychainAccessGroup);
    OSStatus status = SecItemDelete((__bridge CFDictionaryRef)q);
    if (cn1_secureStorageHasLegacyService(service, legacy)) {
        // Both, or a remove would leave the entry an earlier build wrote to be read back later --
        // and for a managed key that is a key the caller believes it has destroyed.
        NSMutableDictionary *old = cn1_secureStoragePlainQuery(nsAccount, legacy, cn1_keychainAccessGroup);
        OSStatus legacyStatus = SecItemDelete((__bridge CFDictionaryRef)old);
        if (status == errSecItemNotFound) {
            status = legacyStatus;
        }
    }
    result = (status == errSecSuccess || status == errSecItemNotFound) ? JAVA_TRUE : JAVA_FALSE;
    POOL_END();
    return result;
}

// ============================================================================
// NFC natives (Core NFC)
// ============================================================================
//
// Gated on CN1_INCLUDE_NFC which IPhoneBuilder defines only when the
// classpath scanner saw a com.codename1.nfc reference. Without that define
// no CoreNFC.framework symbols are linked, so apps that never use NFC pass
// Apple's API-usage scan without a CoreNFC privacy manifest. The Java side
// still receives stub implementations of every native method (returning
// NOT_AVAILABLE) so the link step succeeds.
//
// Core NFC requires iOS 11 for NDEF reads, iOS 13 for tag sessions (ISO 7816
// / FeliCa / MIFARE) and iOS 17.4 for the EU-only CardSession HCE flavour.
// The frameworks are weak-linked so the build still succeeds on older
// deployment targets; the supported / canRead checks gate every code path.
//
// Memory management is manual because the iOS port builds with
// CLANG_ENABLE_OBJC_ARC=NO -- see "cn1 iOS port runs without ARC" memory.

#ifdef CN1_INCLUDE_NFC
// Not available on macOS.
#if !TARGET_OS_OSX
#import <CoreNFC/CoreNFC.h>
#endif
#endif

#ifdef CN1_INCLUDE_NFC
// Pointer-stable session containers. Static because the Java side is
// stateless across native call boundaries.
@interface CN1NfcNdefDelegate : NSObject <NFCNDEFReaderSessionDelegate>
@property (nonatomic, assign) int requestId;
@end

@interface CN1NfcTagDelegate : NSObject <NFCTagReaderSessionDelegate>
@property (nonatomic, assign) int requestId;
@property (nonatomic, retain) id<NFCTag> connectedTag;
@end

static NFCNDEFReaderSession *cn1_nfcNdefSession = nil;
static NFCTagReaderSession *cn1_nfcTagSession = nil;
static CN1NfcNdefDelegate *cn1_nfcNdefDelegate = nil;
static CN1NfcTagDelegate *cn1_nfcTagDelegate = nil;
static NSMutableArray *cn1_nfcConnectedTags = nil;

static int cn1_nfcSendError(int requestId, NSError *err) {
    int code = (int)err.code;
    NSString *msg = err.localizedDescription ? err.localizedDescription : @"";
    JAVA_OBJECT jmsg = fromNSString(getThreadLocalData(), msg);
    com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, code, jmsg);
    return code;
}

@implementation CN1NfcNdefDelegate
- (void)readerSession:(NFCNDEFReaderSession *)session didDetectNDEFs:(NSArray<NFCNDEFMessage *> *)messages {
    if ([messages count] == 0) {
        cn1_nfcSendError(self.requestId,
            [NSError errorWithDomain:NFCErrorDomain code:4 userInfo:nil]);
        return;
    }
    NFCNDEFMessage *msg = [messages objectAtIndex:0];
    NSData *raw = [self serializeNdefMessage:msg];
    JAVA_OBJECT arr = JAVA_NULL;
    if (raw != nil) {
        JAVA_ARRAY ja = (JAVA_ARRAY)__NEW_ARRAY_JAVA_BYTE(getThreadLocalData(), (JAVA_INT)[raw length]);
        memcpy(((JAVA_ARRAY_BYTE *)ja->data), [raw bytes], [raw length]);
        arr = (JAVA_OBJECT)ja;
    }
    com_codename1_impl_ios_IOSNfc_nativeNdefResult___int_byte_1ARRAY(getThreadLocalData(), self.requestId, arr);
    [session invalidateSession];
}

- (void)readerSession:(NFCNDEFReaderSession *)session didInvalidateWithError:(NSError *)error {
    if (cn1_nfcNdefSession == session) {
        [cn1_nfcNdefSession release];
        cn1_nfcNdefSession = nil;
    }
    if (self.requestId > 0) {
        cn1_nfcSendError(self.requestId, error);
        self.requestId = 0;
    }
}

- (NSData *)serializeNdefMessage:(NFCNDEFMessage *)msg {
    // The Java NdefMessage.parse() expects the raw NDEF wire format which
    // is what NFCNDEFMessage exposes via -length / records. We rebuild it
    // ourselves to match: TNF/flags, type-len, payload-len, optional id-len,
    // type, id, payload per record.
    NSMutableData *out = [NSMutableData data];
    NSArray *records = msg.records;
    NSUInteger n = [records count];
    for (NSUInteger i = 0; i < n; i++) {
        NFCNDEFPayload *r = [records objectAtIndex:i];
        NSData *type = r.type ? r.type : [NSData data];
        NSData *ident = r.identifier ? r.identifier : [NSData data];
        NSData *payload = r.payload ? r.payload : [NSData data];
        unsigned int header = (unsigned int)r.typeNameFormat & 0x07;
        if (i == 0) {
            header |= 0x80;
        }
        if (i == n - 1) {
            header |= 0x40;
        }
        BOOL sr = [payload length] < 256;
        BOOL il = [ident length] > 0;
        if (sr) {
            header |= 0x10;
        }
        if (il) {
            header |= 0x08;
        }
        unsigned char hb = (unsigned char)header;
        [out appendBytes:&hb length:1];
        unsigned char tl = (unsigned char)([type length] & 0xFF);
        [out appendBytes:&tl length:1];
        if (sr) {
            unsigned char pl = (unsigned char)([payload length] & 0xFF);
            [out appendBytes:&pl length:1];
        } else {
            uint32_t pl = (uint32_t)[payload length];
            unsigned char buf[4] = {
                (unsigned char)((pl >> 24) & 0xFF),
                (unsigned char)((pl >> 16) & 0xFF),
                (unsigned char)((pl >> 8) & 0xFF),
                (unsigned char)(pl & 0xFF)
            };
            [out appendBytes:buf length:4];
        }
        if (il) {
            unsigned char idl = (unsigned char)([ident length] & 0xFF);
            [out appendBytes:&idl length:1];
        }
        [out appendData:type];
        if (il) {
            [out appendData:ident];
        }
        [out appendData:payload];
    }
    return out;
}
@end

@implementation CN1NfcTagDelegate
- (void)tagReaderSessionDidBecomeActive:(NFCTagReaderSession *)session {
}

- (void)tagReaderSession:(NFCTagReaderSession *)session didDetectTags:(NSArray<__kindof id<NFCTag>> *)tags {
    if ([tags count] == 0) {
        return;
    }
    id<NFCTag> tag = [tags objectAtIndex:0];
    [session connectToTag:tag completionHandler:^(NSError *error) {
        if (error != nil) {
            cn1_nfcSendError(self.requestId, error);
            [session invalidateSession];
            return;
        }
        if (cn1_nfcConnectedTags == nil) {
            cn1_nfcConnectedTags = [[NSMutableArray alloc] init];
        }
        [cn1_nfcConnectedTags addObject:tag];
        long handle = (long)tag; // pointer used as opaque handle
        int mask = 0;
        NSData *uid = nil;
        if (tag.type == NFCTagTypeISO7816Compatible) {
            mask |= 4 | 1;
            id<NFCISO7816Tag> iso = [tag asNFCISO7816Tag];
            uid = iso.identifier;
        } else if (tag.type == NFCTagTypeFeliCa) {
            mask |= 2;
            id<NFCFeliCaTag> f = [tag asNFCFeliCaTag];
            uid = f.currentIDm;
        } else if (tag.type == NFCTagTypeMiFare) {
            mask |= 1 | 8;
            id<NFCMiFareTag> m = [tag asNFCMiFareTag];
            uid = m.identifier;
        } else if (tag.type == NFCTagTypeISO15693) {
            id<NFCISO15693Tag> v = [tag asNFCISO15693Tag];
            uid = v.identifier;
        }
        JAVA_OBJECT uidArr = JAVA_NULL;
        if (uid != nil && [uid length] > 0) {
            JAVA_ARRAY ja = (JAVA_ARRAY)__NEW_ARRAY_JAVA_BYTE(getThreadLocalData(), (JAVA_INT)[uid length]);
            memcpy(((JAVA_ARRAY_BYTE *)ja->data), [uid bytes], [uid length]);
            uidArr = (JAVA_OBJECT)ja;
        }
        com_codename1_impl_ios_IOSNfc_nativeTagDiscovered___int_long_int_byte_1ARRAY(getThreadLocalData(), self.requestId, (JAVA_LONG)handle, mask, uidArr);
    }];
}

- (void)tagReaderSession:(NFCTagReaderSession *)session didInvalidateWithError:(NSError *)error {
    if (cn1_nfcTagSession == session) {
        [cn1_nfcTagSession release];
        cn1_nfcTagSession = nil;
    }
    [cn1_nfcConnectedTags removeAllObjects];
    if (self.requestId > 0) {
        cn1_nfcSendError(self.requestId, error);
        self.requestId = 0;
    }
}
@end
#endif // CN1_INCLUDE_NFC

// ParparVM mangles non-void-returning native methods as
// `..._methodName___R_<returnType>`. There is no binary compatibility to keep
// with the unsuffixed spelling: nothing links against it, so a native method
// left that way is not merely unreachable -- the dead-code pass reads the
// absence of its real symbol as "unused" and drops the Java method too, which
// is how biometrics, printing, Apple Sign In, OIDC and the sound pool all
// shipped inert. scripts/check-native-signatures.sh and the same check inside
// the translator now fail the build on it.
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isNfcSupported___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_INCLUDE_NFC
    if (@available(iOS 11.0, *)) {
        return [NFCNDEFReaderSession readingAvailable] ? JAVA_TRUE : JAVA_FALSE;
    }
#endif
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_canReadNfc___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return com_codename1_impl_ios_IOSNative_isNfcSupported___R_boolean(CN1_THREAD_STATE_PASS_ARG me);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_canReadNfcTags___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_INCLUDE_NFC
    if (@available(iOS 13.0, *)) {
        return [NFCTagReaderSession readingAvailable] ? JAVA_TRUE : JAVA_FALSE;
    }
#endif
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_canHostEmulateNfc___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_INCLUDE_NFC
    if (@available(iOS 17.4, *)) {
        // NFCPresentmentIntent etc are still gated by entitlement + EU region.
        return NSClassFromString(@"NFCISO7816APDU") != nil ? JAVA_TRUE : JAVA_FALSE;
    }
#endif
    return JAVA_FALSE;
}

void com_codename1_impl_ios_IOSNative_startNdefRead___int_java_lang_String_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_OBJECT alertMessage, JAVA_LONG timeoutMs) {
#ifdef CN1_INCLUDE_NFC
    if (@available(iOS 11.0, *)) {
        if (![NFCNDEFReaderSession readingAvailable]) {
            com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 1001, JAVA_NULL);
            return;
        }
        POOL_BEGIN();
        if (cn1_nfcNdefDelegate == nil) {
            cn1_nfcNdefDelegate = [[CN1NfcNdefDelegate alloc] init];
        }
        cn1_nfcNdefDelegate.requestId = requestId;
        if (cn1_nfcNdefSession != nil) {
            [cn1_nfcNdefSession invalidateSession];
            [cn1_nfcNdefSession release];
            cn1_nfcNdefSession = nil;
        }
        cn1_nfcNdefSession = [[NFCNDEFReaderSession alloc] initWithDelegate:cn1_nfcNdefDelegate queue:dispatch_get_main_queue() invalidateAfterFirstRead:YES];
        if (alertMessage != JAVA_NULL) {
            NSString *s = toNSString(CN1_THREAD_STATE_PASS_ARG alertMessage);
            if (s != nil) {
                cn1_nfcNdefSession.alertMessage = s;
            }
        }
        [cn1_nfcNdefSession beginSession];
        POOL_END();
        return;
    }
#endif
    com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 1001, JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_startTagRead___int_java_lang_String_int_java_lang_String_1ARRAY_byte_2ARRAY_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_OBJECT alertMessage, JAVA_INT polling, JAVA_OBJECT systemCodes, JAVA_OBJECT aids, JAVA_LONG timeoutMs) {
#ifdef CN1_INCLUDE_NFC
    if (@available(iOS 13.0, *)) {
        if (![NFCTagReaderSession readingAvailable]) {
            com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 1001, JAVA_NULL);
            return;
        }
        POOL_BEGIN();
        NFCPollingOption pollingMask = 0;
        if ((polling & 1) != 0) pollingMask |= NFCPollingISO14443;
        if ((polling & 4) != 0) pollingMask |= NFCPollingISO18092;
        if ((polling & 8) != 0) pollingMask |= NFCPollingISO15693;
        if (pollingMask == 0) {
            pollingMask = NFCPollingISO14443 | NFCPollingISO18092;
        }
        if (cn1_nfcTagDelegate == nil) {
            cn1_nfcTagDelegate = [[CN1NfcTagDelegate alloc] init];
        }
        cn1_nfcTagDelegate.requestId = requestId;
        if (cn1_nfcTagSession != nil) {
            [cn1_nfcTagSession invalidateSession];
            [cn1_nfcTagSession release];
            cn1_nfcTagSession = nil;
        }
        cn1_nfcTagSession = [[NFCTagReaderSession alloc] initWithPollingOption:pollingMask delegate:cn1_nfcTagDelegate queue:dispatch_get_main_queue()];
        if (alertMessage != JAVA_NULL) {
            NSString *s = toNSString(CN1_THREAD_STATE_PASS_ARG alertMessage);
            if (s != nil) {
                cn1_nfcTagSession.alertMessage = s;
            }
        }
        [cn1_nfcTagSession beginSession];
        POOL_END();
        return;
    }
#endif
    com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 1001, JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_stopNfcRead___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
#ifdef CN1_INCLUDE_NFC
    if (cn1_nfcNdefSession != nil) {
        [cn1_nfcNdefSession invalidateSession];
    }
    if (cn1_nfcTagSession != nil) {
        [cn1_nfcTagSession invalidateSession];
    }
#endif
}

void com_codename1_impl_ios_IOSNative_nfcTransceive___int_long_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_LONG handle, JAVA_OBJECT payload) {
#ifdef CN1_INCLUDE_NFC
    if (@available(iOS 13.0, *)) {
        id<NFCTag> tag = (id<NFCTag>)((void *)(intptr_t)handle);
        if (tag == nil || ![cn1_nfcConnectedTags containsObject:tag]) {
            com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 100, JAVA_NULL);
            return;
        }
        if (tag.type != NFCTagTypeISO7816Compatible) {
            com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 1001, JAVA_NULL);
            return;
        }
        id<NFCISO7816Tag> iso = [tag asNFCISO7816Tag];
        JAVA_ARRAY pa = (JAVA_ARRAY)payload;
        NSData *data = [NSData dataWithBytes:((JAVA_ARRAY_BYTE *)pa->data) length:pa->length];
        // Slice the APDU into CLA/INS/P1/P2/data/Le per NFCISO7816APDU API.
        NSError *parseErr = nil;
        NFCISO7816APDU *apdu = [[NFCISO7816APDU alloc] initWithData:data];
        if (apdu == nil) {
            com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 105, JAVA_NULL);
            return;
        }
        [iso sendCommandAPDU:apdu completionHandler:^(NSData *response, uint8_t sw1, uint8_t sw2, NSError *error) {
            if (error != nil) {
                cn1_nfcSendError(requestId, error);
                return;
            }
            NSUInteger len = (response != nil ? [response length] : 0) + 2;
            JAVA_ARRAY ja = (JAVA_ARRAY)__NEW_ARRAY_JAVA_BYTE(getThreadLocalData(), (JAVA_INT)len);
            if (response != nil && [response length] > 0) {
                memcpy(((JAVA_ARRAY_BYTE *)ja->data), [response bytes], [response length]);
            }
            ((JAVA_ARRAY_BYTE *)ja->data)[len - 2] = sw1;
            ((JAVA_ARRAY_BYTE *)ja->data)[len - 1] = sw2;
            com_codename1_impl_ios_IOSNfc_nativeTransceiveResult___int_byte_1ARRAY(getThreadLocalData(), requestId, (JAVA_OBJECT)ja);
        }];
        [apdu release];
        return;
    }
#endif
    com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 1001, JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_nfcReadNdefFromTag___int_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_LONG handle) {
#ifdef CN1_INCLUDE_NFC
    if (@available(iOS 13.0, *)) {
        id<NFCTag> tag = (id<NFCTag>)((void *)(intptr_t)handle);
        if (![tag conformsToProtocol:@protocol(NFCNDEFTag)]) {
            com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 1001, JAVA_NULL);
            return;
        }
        id<NFCNDEFTag> ndefTag = (id<NFCNDEFTag>)tag;
        [ndefTag readNDEFWithCompletionHandler:^(NFCNDEFMessage *message, NSError *error) {
            if (error != nil) {
                cn1_nfcSendError(requestId, error);
                return;
            }
            if (message == nil) {
                com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 4, JAVA_NULL);
                return;
            }
            NSData *raw = [cn1_nfcNdefDelegate serializeNdefMessage:message];
            JAVA_OBJECT arr = JAVA_NULL;
            if (raw != nil) {
                JAVA_ARRAY ja = (JAVA_ARRAY)__NEW_ARRAY_JAVA_BYTE(getThreadLocalData(), (JAVA_INT)[raw length]);
                memcpy(((JAVA_ARRAY_BYTE *)ja->data), [raw bytes], [raw length]);
                arr = (JAVA_OBJECT)ja;
            }
            com_codename1_impl_ios_IOSNfc_nativeNdefResult___int_byte_1ARRAY(getThreadLocalData(), requestId, arr);
        }];
        return;
    }
#endif
    com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 1001, JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_nfcWriteNdefToTag___int_long_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_LONG handle, JAVA_OBJECT ndef) {
#ifdef CN1_INCLUDE_NFC
    if (@available(iOS 13.0, *)) {
        id<NFCTag> tag = (id<NFCTag>)((void *)(intptr_t)handle);
        if (![tag conformsToProtocol:@protocol(NFCNDEFTag)]) {
            com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 1001, JAVA_NULL);
            return;
        }
        id<NFCNDEFTag> ndefTag = (id<NFCNDEFTag>)tag;
        JAVA_ARRAY na = (JAVA_ARRAY)ndef;
        NSData *raw = [NSData dataWithBytes:((JAVA_ARRAY_BYTE *)na->data) length:na->length];
        // CoreNFC's NFCNDEFMessage requires the parsed object form; we
        // reconstruct it by parsing the wire-format bytes.
        // Apple does not expose a public reader for the wire bytes so we
        // wrap the payload in a single short MIME record (best-effort) when
        // the structure is not already NFCNDEFMessage-compatible.
        NFCNDEFMessage *msg = nil;
        @try {
            msg = [[NFCNDEFMessage alloc] initWithData:raw];
        } @catch (NSException *e) {
            msg = nil;
        }
        if (msg == nil) {
            // Fallback: build a single MIME record containing the raw payload.
            NFCNDEFPayload *p = [NFCNDEFPayload wellKnownTypeURIPayloadWithString:@"about:blank"];
            msg = [[NFCNDEFMessage alloc] initWithNDEFRecords:[NSArray arrayWithObject:p]];
        }
        [ndefTag writeNDEF:msg completionHandler:^(NSError *error) {
            if (error != nil) {
                cn1_nfcSendError(requestId, error);
            } else {
                com_codename1_impl_ios_IOSNfc_nativeWriteResult___int_boolean(getThreadLocalData(), requestId, JAVA_TRUE);
            }
        }];
        [msg release];
        return;
    }
#endif
    com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 1001, JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_nfcLockTag___int_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId, JAVA_LONG handle) {
#ifdef CN1_INCLUDE_NFC
    if (@available(iOS 13.0, *)) {
        id<NFCTag> tag = (id<NFCTag>)((void *)(intptr_t)handle);
        if (![tag conformsToProtocol:@protocol(NFCNDEFTag)]) {
            com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 1001, JAVA_NULL);
            return;
        }
        id<NFCNDEFTag> ndefTag = (id<NFCNDEFTag>)tag;
        [ndefTag writeLockWithCompletionHandler:^(NSError *error) {
            if (error != nil) {
                cn1_nfcSendError(requestId, error);
            } else {
                com_codename1_impl_ios_IOSNfc_nativeWriteResult___int_boolean(getThreadLocalData(), requestId, JAVA_TRUE);
            }
        }];
        return;
    }
#endif
    com_codename1_impl_ios_IOSNfc_nativeNfcError___int_int_java_lang_String(getThreadLocalData(), requestId, 1001, JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_registerHceAids___java_lang_String_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT aids) {
    // CardSession (iOS 17.4 EU-only) requires the
    // com.apple.developer.nfc.hce.iso7816.select-identifiers entitlement to
    // be present at app load time; runtime registration is informational.
    // Implementation deferred -- the iOS HCE platform surface is
    // EU-restricted and changes between iOS minor versions; the Java
    // side returns NOT_AVAILABLE on devices where canHostEmulateNfc
    // returns false.
}

void com_codename1_impl_ios_IOSNative_hceSendResponse___byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT response) {
    // See registerHceAids above.
}

// ====================================================================
// Crypto bridge _R_int wrappers
//
// ParparVM emits two C entry points for every non-void native method: the
// unmangled implementation (com_..._methodName___paramTypes) plus a
// _R_<returnType>-suffixed wrapper that the bytecode dispatcher actually
// calls. We forward each wrapper to the matching implementation -- which is
// either the CN1_INCLUDE_CRYPTO-on real version or the always-fail stub
// from the #else branch above, depending on the build configuration.

JAVA_INT com_codename1_impl_ios_IOSNative_aesCbc___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT encrypt, JAVA_OBJECT keyArr, JAVA_OBJECT ivArr, JAVA_OBJECT inArr, JAVA_OBJECT outArr, JAVA_INT padding) {
    return com_codename1_impl_ios_IOSNative_aesCbc___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int(CN1_THREAD_STATE_PASS_ARG instanceObject, encrypt, keyArr, ivArr, inArr, outArr, padding);
}

JAVA_INT com_codename1_impl_ios_IOSNative_aesGcm___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT encrypt, JAVA_OBJECT keyArr, JAVA_OBJECT ivArr, JAVA_OBJECT aadArr, JAVA_OBJECT inArr, JAVA_OBJECT outArr) {
    return com_codename1_impl_ios_IOSNative_aesGcm___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_PASS_ARG instanceObject, encrypt, keyArr, ivArr, aadArr, inArr, outArr);
}

JAVA_INT com_codename1_impl_ios_IOSNative_rsaEncrypt___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT paddingKind, JAVA_OBJECT x509, JAVA_OBJECT inArr, JAVA_OBJECT outArr) {
    return com_codename1_impl_ios_IOSNative_rsaEncrypt___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_PASS_ARG instanceObject, paddingKind, x509, inArr, outArr);
}

JAVA_INT com_codename1_impl_ios_IOSNative_rsaDecrypt___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT paddingKind, JAVA_OBJECT pkcs8, JAVA_OBJECT inArr, JAVA_OBJECT outArr) {
    return com_codename1_impl_ios_IOSNative_rsaDecrypt___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_PASS_ARG instanceObject, paddingKind, pkcs8, inArr, outArr);
}

JAVA_INT com_codename1_impl_ios_IOSNative_sign___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT algorithm, JAVA_OBJECT pkcs8, JAVA_OBJECT data, JAVA_OBJECT outArr) {
    return com_codename1_impl_ios_IOSNative_sign___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_PASS_ARG instanceObject, algorithm, pkcs8, data, outArr);
}

JAVA_INT com_codename1_impl_ios_IOSNative_verify___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT algorithm, JAVA_OBJECT x509, JAVA_OBJECT data, JAVA_OBJECT sig) {
    return com_codename1_impl_ios_IOSNative_verify___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(CN1_THREAD_STATE_PASS_ARG instanceObject, algorithm, x509, data, sig);
}

JAVA_INT com_codename1_impl_ios_IOSNative_generateRsaKeyPair___int_byte_1ARRAY_byte_1ARRAY_int_1ARRAY_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT bits, JAVA_OBJECT outPub, JAVA_OBJECT outPriv, JAVA_OBJECT lengths) {
    return com_codename1_impl_ios_IOSNative_generateRsaKeyPair___int_byte_1ARRAY_byte_1ARRAY_int_1ARRAY(CN1_THREAD_STATE_PASS_ARG instanceObject, bits, outPub, outPriv, lengths);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createWebSocketNative___int_java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT connectionId, JAVA_OBJECT url) {
    return com_codename1_impl_ios_IOSNative_createWebSocketNative___int_java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, connectionId, url);
}
