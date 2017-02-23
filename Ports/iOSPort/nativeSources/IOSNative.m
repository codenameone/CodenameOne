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
#import "CN1ES2compat.h"

#ifndef NEW_CODENAME_ONE_VM
#include "xmlvm-util.h"
#else
#include "cn1_globals.h"
#endif

#import <UIKit/UIKit.h>
#import "CodenameOne_GLViewController.h"
#import "NetworkConnectionImpl.h"
#include "com_codename1_impl_ios_IOSImplementation.h"
#include "com_codename1_ui_Display.h"
#include "com_codename1_ui_Component.h"
#include "java_lang_Throwable.h"
#import "AudioPlayer.h"
#import "DrawGradient.h"
#import <MediaPlayer/MediaPlayer.h>
#import <CoreLocation/CoreLocation.h>
#import <MobileCoreServices/UTCoreTypes.h>
#import <Foundation/Foundation.h>
#import <MessageUI/MFMailComposeViewController.h>
#import <AddressBookUI/AddressBookUI.h>
#import <MessageUI/MFMessageComposeViewController.h>
#import "UIWebViewEventDelegate.h"
#include <sqlite3.h>
#include "OpenUDID.h"
#import "StoreKit/StoreKit.h"
#include "com_codename1_contacts_Contact.h"
#include "com_codename1_contacts_Address.h"
#include "java_util_Hashtable.h"
#include "com_codename1_ui_Image.h"
#include "com_codename1_impl_ios_IOSImplementation_NativeImage.h"
#import "SocketImpl.h"
#import "com_codename1_ui_geom_Rectangle.h"
#import <MobileCoreServices/MobileCoreServices.h>
#include "com_codename1_ui_plaf_Style.h"
#import "RadialGradientPaint.h"
//#import "QRCodeReaderOC.h"
#define AUTO_PLAY_VIDEO

#ifdef INCLUDE_ZOOZ
#import "ZooZ.h"
#endif
#import "Rotate.h"
extern int popoverSupported();

#define INCLUDE_CN1_PUSH2

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

extern UIView *editingComponent;

extern void initVMImpl();

extern void deinitVMImpl();

extern int Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl();

extern int Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl();

extern void Java_com_codename1_impl_ios_IOSImplementation_flushBufferImpl
(void* peer, int x, int y, int width, int height);

extern void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingMutableImpl
(int x, int y, int width, int height, int clipApplied);

extern void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingShapeMutableImpl
(int numCommands, JAVA_OBJECT commands, int numPoints, JAVA_OBJECT points);

extern void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingGlobalImpl
(int x, int y, int width, int height, int clipApplied);

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
(void* peer, int alpha, int x, int y, int width, int height);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl
(void* peer, int alpha, int x, int y, int width, int height);

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

extern void Java_com_codename1_impl_ios_IOSImplementation_imageRgbToIntArrayImpl
(void* peer, int* arr, int x, int y, int width, int height, int imgWidth, int imgHeight);

extern void* Java_com_codename1_impl_ios_IOSImplementation_createImageFromARGBImpl
(int* arr, int width, int height);

extern void Java_com_codename1_impl_ios_IOSImplementation_editStringAtImpl
(CN1_THREAD_STATE_MULTI_ARG int x, int y, int w, int h, void* peer, int isSingleLine, int rows, int maxSize,
 int constraint, const char* str, int len, BOOL dialogHeight, int color, JAVA_LONG imagePeer,
 int padTop, int padBottom, int padLeft, int padRight, NSString* hintString, BOOL showToolbar, BOOL blockCopyPaste);

extern void Java_com_codename1_impl_ios_IOSImplementation_resetAffineGlobal();

extern void Java_com_codename1_impl_ios_IOSImplementation_scale(float x, float y);

extern int isIPad();
extern int isIOS7();
extern int isIOS8();

NSString* fixFilePath(NSString* ns) {
    if([ns hasPrefix:@"file:"]) {
        ns = [ns substringFromIndex:5];
        while([ns hasPrefix:@"//"]) {
            ns = [ns substringFromIndex:1];
        }
    }
    return ns;
}

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
    POOL_BEGIN();
    int retVal = UIApplicationMain(0, nil, nil, @"CodenameOne_GLAppDelegate");
    POOL_END();
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

void retainCN1(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT o){
    com_codename1_impl_ios_IOSImplementation_retain___java_lang_Object(CN1_THREAD_STATE_PASS_ARG o);
}

void releaseCN1(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT o){
    com_codename1_impl_ios_IOSImplementation_release___java_lang_Object(CN1_THREAD_STATE_PASS_ARG o);
}

#ifndef NEW_CODENAME_ONE_VM
NSString* toNSString(JAVA_OBJECT str) {
    if(str == JAVA_NULL) {
        return 0;
    }
    // accessing internal state since toCharArray performs an allocation which can be REALLY expensive
    int offset = ((java_lang_String*) str)->fields.java_lang_String.offset_;
    org_xmlvm_runtime_XMLVMArray* cArr = ((java_lang_String*) str)->fields.java_lang_String.value_;
    //NSLog(@"cArr pointer is: %i", cArr);
    if(cArr == JAVA_NULL) {
        const char* chrs = stringToUTF8(str);
        NSString* st = [NSString stringWithUTF8String:chrs];
        //NSLog(@"Unicode chars: %@ over %i at offset %i", st, chrArr[iter], iter);
        return st;
    }
    
    JAVA_ARRAY_CHAR* chrArr = (JAVA_ARRAY_CHAR*)cArr->fields.org_xmlvm_runtime_XMLVMArray.array_;
    //NSLog(@"chrArr pointer is: %i", chrArr);
    
    int length = ((java_lang_String*) str)->fields.java_lang_String.count_;
    
    for(int iter = offset ; iter < length ; iter++) {
        if(chrArr[iter] > 127) {
            const char* chrs = stringToUTF8(str);
            NSString* st = [NSString stringWithUTF8String:chrs];
            //NSLog(@"Unicode chars: %@ over %i at offset %i", st, chrArr[iter], iter);
            return st;
        }
    }
    if(offset > 0) {
        return [[NSString stringWithCharacters:chrArr length:length+offset] substringFromIndex:offset];
    }
    return [NSString stringWithCharacters:chrArr length:length];
}
#endif

void com_codename1_impl_ios_IOSNative_editStringAt___int_int_int_int_long_boolean_int_int_int_java_lang_String_boolean_int_long_int_int_int_int_java_lang_String_boolean_boolean(CN1_THREAD_STATE_MULTI_ARG
                                                                                                                                                                         JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_LONG n5, JAVA_BOOLEAN n6, JAVA_INT n7,
                                                                                                                                                                         JAVA_INT n8, JAVA_INT n9, JAVA_OBJECT n10, JAVA_BOOLEAN forceSlide,
                                                                                                                                                                         JAVA_INT color, JAVA_LONG imagePeer, JAVA_INT padTop, JAVA_INT padBottom, JAVA_INT padLeft, JAVA_INT padRight, JAVA_OBJECT hint, JAVA_BOOLEAN showToolbar, JAVA_BOOLEAN blockCopyPaste)
{
    POOL_BEGIN();
    const char* chr = stringToUTF8(CN1_THREAD_STATE_PASS_ARG n10);
    int l = strlen(chr) + 1;
    char cc[l];
    memcpy(cc, chr, l);
    Java_com_codename1_impl_ios_IOSImplementation_editStringAtImpl(CN1_THREAD_STATE_PASS_ARG n1, n2, n3, n4, n5, n6, n7, n8, n9, cc, 0, forceSlide, color, imagePeer,
                                                                   padTop, padBottom, padLeft, padRight, toNSString(CN1_THREAD_STATE_PASS_ARG hint), showToolbar, blockCopyPaste);
    POOL_END();
}
extern float scaleValue;
extern int editComponentPadTop, editComponentPadLeft;
extern float editCompoentX, editCompoentY, editCompoentW, editCompoentH;
void com_codename1_impl_ios_IOSNative_resizeNativeTextView___int_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_INT padTop, JAVA_INT padRight, JAVA_INT padBottom, JAVA_INT padLeft) {
    POOL_BEGIN();
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if(editingComponent != nil) {
            float scale = scaleValue;
            CGRect existingBounds = editingComponent.frame;
            NSString *currText = ((UITextField*)editingComponent).text;

            editCompoentX = (x + padLeft) / scale;
            editCompoentY = (y + padTop) / scale;
            editComponentPadTop = padTop;
            editComponentPadLeft = padLeft;
            if (scale > 1) {
                editCompoentY -= 1.5;
            } else {
                editCompoentY -= 1;
            }
            editCompoentW = (w - padLeft - padRight) / scale;
            editCompoentH = (h - padTop - padBottom) / scale;
            CGRect rect = CGRectMake(editCompoentX, editCompoentY, editCompoentW, editCompoentH);
            //NSLog(@"Changing bounds %f,%f,%f,%f to %f,%f,%f,%f", existingBounds.origin.x, existingBounds.origin.y, existingBounds.size.width, existingBounds.size.height, rect.origin.x, rect.origin.y, rect.size.width, rect.size.height);
            if (fabs(existingBounds.size.width - rect.size.width) > 1 || fabs(existingBounds.size.height - rect.size.height) > 1 ||
                fabs(existingBounds.origin.x - rect.origin.x) > 1 || fabs(existingBounds.origin.y - 1.5 - rect.origin.y) > 1
                ) {
                //NSLog(@"Changing bounds %f,%f,%f,%f to %f,%f,%f,%f", existingBounds.origin.x, existingBounds.origin.y, existingBounds.size.width, existingBounds.size.height, rect.origin.x, rect.origin.y, rect.size.width, rect.size.height);
                editingComponent.frame = rect;
            }
            
            
        }
        POOL_END();
    });
    
    POOL_END();
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


void com_codename1_impl_ios_IOSNative_flushBuffer___long_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_flushBuffer___long_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_flushBufferImpl((void *)n1, n2, n3, n4, n5);
    POOL_END();
    //XMLVM_END_WRAPPER
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
    JAVA_ARRAY_LONG i = Java_com_codename1_impl_ios_IOSImplementation_createImageFromARGBImpl((void *)data, n2, n3);
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
    UIImage* img = [UIImage imageWithData:nd];
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

JAVA_LONG com_codename1_impl_ios_IOSNative_scale___long_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT n2, JAVA_INT n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_scale___long_int_int]
    POOL_BEGIN();
    JAVA_LONG i = (JAVA_LONG)Java_com_codename1_impl_ios_IOSImplementation_scaleImpl(n1, n2, n3);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gausianBlurImage___long_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_FLOAT radius) {
    POOL_BEGIN();

    GLUIImage* glu = (BRIDGE_CAST GLUIImage*)n1;
    if(((BRIDGE_CAST void*)[CodenameOne_GLViewController instance].currentMutableImage) == glu) {
        Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl();
    }

    UIImage* original = [glu getImage];
    
    // taken from: http://stackoverflow.com/a/19433086/756809
    CIFilter *gaussianBlurFilter = [CIFilter filterWithName:@"CIGaussianBlur"];
    [gaussianBlurFilter setDefaults];
    CIImage *inputImage = [CIImage imageWithCGImage:[original CGImage]];
    [gaussianBlurFilter setValue:inputImage forKey:kCIInputImageKey];
    NSNumber *radiusNumber = [NSNumber numberWithFloat:radius];
    [gaussianBlurFilter setValue:radiusNumber forKey:kCIInputRadiusKey];
    
    CIImage *outputImage = [gaussianBlurFilter outputImage];
    CIContext *context   = [CIContext contextWithOptions:nil];
    CGImageRef cgimg     = [context createCGImage:outputImage fromRect:[inputImage extent]];
    UIImage *image       = [UIImage imageWithCGImage:cgimg];
    CGImageRelease(cgimg);
    GLUIImage* gl = [[GLUIImage alloc] initWithImage:image];
    
    POOL_END();
    return (BRIDGE_CAST void*)gl;
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




extern CGContextRef Java_com_codename1_impl_ios_IOSImplementation_drawPath(CN1_THREAD_STATE_MULTI_ARG JAVA_INT commandsLen, JAVA_OBJECT commandsArr, JAVA_INT pointsLen, JAVA_OBJECT pointsArr);

static CGContextRef drawPath(CN1_THREAD_STATE_MULTI_ARG JAVA_INT commandsLen, JAVA_OBJECT commandsArr, JAVA_INT pointsLen, JAVA_OBJECT pointsArr) {

    return Java_com_codename1_impl_ios_IOSImplementation_drawPath(CN1_THREAD_STATE_PASS_ARG commandsLen, commandsArr, pointsLen, pointsArr);
    
   

}


//native void nativeFillShapeMutable(int color, int alpha, int commandsLen, byte[] commandsArr, int pointsLen, float[] pointsArr);
void com_codename1_impl_ios_IOSNative_nativeFillShapeMutable___int_int_int_byte_1ARRAY_int_float_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT color, JAVA_INT alpha, JAVA_INT commandsLen, JAVA_OBJECT commandsArr, JAVA_INT pointsLen, JAVA_OBJECT pointsArr) {
    POOL_BEGIN();
    [UIColorFromRGB(color, alpha) set];
    CGContextRef context = drawPath(CN1_THREAD_STATE_PASS_ARG commandsLen, commandsArr, pointsLen, pointsArr);
    CGContextFillPath(context);
    POOL_END();
    
}

void com_codename1_impl_ios_IOSNative_nativeDrawShapeMutable___int_int_int_byte_1ARRAY_int_float_1ARRAY_float_int_int_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT color, JAVA_INT alpha, JAVA_INT commandsLen, JAVA_OBJECT commandsArr, JAVA_INT pointsLen, JAVA_OBJECT pointsArr, JAVA_FLOAT lineWidth, JAVA_INT capStyle, JAVA_INT joinStyle, JAVA_FLOAT mitreLimit) {
    POOL_BEGIN();
    if ([CodenameOne_GLViewController isCurrentMutableTransformSet]) {
        CGContextSaveGState(UIGraphicsGetCurrentContext());
        CGContextConcatCTM(UIGraphicsGetCurrentContext(), [CodenameOne_GLViewController currentMutableTransform]);
    }
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
    if ([CodenameOne_GLViewController isCurrentMutableTransformSet]) {
        CGContextRestoreGState(context);
    }
    POOL_END();
}



void com_codename1_impl_ios_IOSNative_nativeDrawStringMutable___int_int_long_java_lang_String_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_LONG n3, JAVA_OBJECT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawStringMutable___int_int_long_java_lang_String_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringMutableImpl(n1, n2, n3, toNSString(CN1_THREAD_STATE_PASS_ARG n4), n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawStringGlobal___int_int_long_java_lang_String_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_LONG n3, JAVA_OBJECT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawStringGlobal___int_int_long_java_lang_String_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringGlobalImpl(n1, n2, n3, toNSString(CN1_THREAD_STATE_PASS_ARG n4), n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawImageMutable___long_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT alpha, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawImageMutable___long_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageMutableImpl((void *)n1, alpha, n2, n3, n4, n5);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawImageGlobal___long_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT alpha, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawImageGlobal___long_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl((void *)n1, alpha, n2, n3, n4, n5);
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
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_createSystemFontImpl((void *)n1, n2, n3);
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
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_createNativeMutableImageImpl((void *)n1, n2, n3);
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
    JAVA_LONG i = (void *)Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl();
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_deleteNativePeer___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG n1)
{
    if(n1 != 0) {
        // this prevents a race condition where the gc might be invoked too soon
        dispatch_async(dispatch_get_main_queue(), ^{
            NSObject* n = (NSObject*)n1;
            [n release];
        });
    }
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
        NSLog(@"Error writeToFile: %@ for the file %@", [error localizedDescription], ns);
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
        NSLog(@"Error getFileSize: %@ for the file %@", [error localizedDescription], ns);
    }
    UInt32 result = (UInt32)[attrs fileSize];
#ifndef CN1_USE_ARC
    [man release];
#endif
    POOL_END();
    return result;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getFileLastModified___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    POOL_BEGIN();
    NSString* ns = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    if([ns hasPrefix:@"file:"]) {
        ns = [ns substringFromIndex:5];
    }
    NSFileManager *man = [[NSFileManager alloc] init];
    NSError *error = nil;
    NSDictionary *attrs = [man attributesOfItemAtPath:ns error:&error];
    if(error != nil) {
        NSLog(@"Error getFileLastModified: %@ for the file %@", [error localizedDescription], ns);
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
        NSLog(@"Error in deleteFile: %@ for the file %@", [error localizedDescription], ns);
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
        NSLog(@"Error in fileCountInDir: %@", [error localizedDescription]);
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
        NSLog(@"Error in listing files: %@", [error localizedDescription]);
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
    if (![fm createDirectoryAtPath:ns attributes:nil]) {
        NSLog(@"Failed to create directory %@", ns);
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
        NSLog(@"Error in moving file: %@", [error localizedDescription]);
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

void com_codename1_impl_ios_IOSNative_setChunkedStreamingMode___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT len) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    [impl setChunkedStreamingLen:len];
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
#ifndef NEW_CODENAME_ONE_VM
    if (!__TIB_byte.classInitialized) __INIT_byte();
    org_xmlvm_runtime_XMLVMArray* byteArray = XMLVMArray_createSingleDimension(__CLASS_byte, [data length]);
    [data getBytes:byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_];
    com_codename1_impl_ios_IOSImplementation_appendData___long_byte_1ARRAY(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_LONG)peer, byteArray);
#else
    struct ThreadLocalData* threadStateData = getThreadLocalData();
    enteringNativeAllocations();
    if([data length] > 65536) {
        int offset = 0;
        while(offset < [data length]) {
            int currentLength = MIN(65536, [data length] - offset);
            JAVA_OBJECT byteArray = __NEW_ARRAY_JAVA_BYTE(threadStateData, currentLength);
            [data getBytes:((JAVA_ARRAY)byteArray)->data range:NSMakeRange(offset, currentLength)];
            com_codename1_impl_ios_IOSImplementation_appendData___long_byte_1ARRAY(threadStateData, (JAVA_LONG)peer, byteArray);
            offset += 65536;
        }
    } else {
        JAVA_OBJECT byteArray = __NEW_ARRAY_JAVA_BYTE(threadStateData, [data length]);
        [data getBytes:((JAVA_ARRAY)byteArray)->data];
        com_codename1_impl_ios_IOSImplementation_appendData___long_byte_1ARRAY(threadStateData, (JAVA_LONG)peer, byteArray);
    }
    finishedNativeAllocations();
#endif
}

void connectionError(void* peer, NSString* message) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    JAVA_OBJECT str = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG message);
    com_codename1_impl_ios_IOSImplementation_networkError___long_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG peer, str);
    POOL_END();
}


void com_codename1_impl_ios_IOSNative_closeConnection___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
#ifndef CN1_USE_ARC
    [impl release];
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_canExecute___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT url) {
    __block JAVA_BOOLEAN result;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSString* ns = toNSString(CN1_THREAD_GET_STATE_PASS_ARG url);
        result = [[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:ns]];
        POOL_END();
    });
    return result;
}

void com_codename1_impl_ios_IOSNative_execute___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT n1)
{
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
            [[UIApplication sharedApplication] openURL:[NSURL URLWithString:ns]];
        }
#ifdef CN1_USE_ARC
        [ns release];
#endif
        POOL_END();
    });
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
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_lockOrientation___boolean]
    if(n1) {
        orientationLock = 1;
    } else {
        orientationLock = 2;
    }
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_unlockOrientation__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    orientationLock = 0;
}

void com_codename1_impl_ios_IOSNative_lockScreen__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    [UIApplication sharedApplication].idleTimerDisabled = YES;
}

void com_codename1_impl_ios_IOSNative_unlockScreen__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    [UIApplication sharedApplication].idleTimerDisabled = NO;
}

extern void vibrateDevice();
void com_codename1_impl_ios_IOSNative_vibrate___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT duration) {
    vibrateDevice();
}

// Peer Component methods

void com_codename1_impl_ios_IOSNative_calcPreferredSize___long_int_int_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT w, JAVA_INT h, JAVA_OBJECT response) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIView* v = (BRIDGE_CAST UIView*)((void *)peer);
        CGSize s = [v sizeThatFits:CGSizeMake(w, h)];
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* intArray = response;
        JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
        JAVA_ARRAY_INT* data = (JAVA_INT*)((JAVA_ARRAY)response)->data;
#endif
        data[0] = (JAVA_INT)s.width;
        data[1] = (JAVA_INT)s.height;
        POOL_END();
    });
}

extern float scaleValue;

void com_codename1_impl_ios_IOSNative_updatePeerPositionSize___long_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIView* v = (BRIDGE_CAST UIView*)((void *)peer);
        float scale = scaleValue;
        float xpos = x / scale;
        float ypos = y / scale;
        float wpos = w / scale;
        float hpos = h / scale;
        [v setFrame:CGRectMake(xpos, ypos, wpos, hpos)];
        [v setNeedsDisplay];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_peerSetVisible___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN b) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIView* v = (BRIDGE_CAST UIView*)((void *)peer);
        if(!b) {
            if([v superview] != nil) {
                [v removeFromSuperview];
            }
        } else {
            if([v superview] == nil) {
                [[CodenameOne_GLViewController instance].view addPeerComponent:v];
            }
        }
        POOL_END();
    });
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createPeerImage___long_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT arr) {
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* intArray = arr;
    __block JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)arr)->data;
#endif
    __block GLUIImage* g = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIView* v = (BRIDGE_CAST UIView*)((void *)peer);
        if(v.bounds.size.width > 0 && v.bounds.size.height > 0) {
            UIGraphicsBeginImageContextWithOptions(v.bounds.size, v.opaque, 0.0);
            [v.layer renderInContext:UIGraphicsGetCurrentContext()];
            
            UIImage* image = UIGraphicsGetImageFromCurrentImageContext();
            
            UIGraphicsEndImageContext();
            g = [[GLUIImage alloc] initWithImage:image];
            data[0] = (JAVA_INT)v.bounds.size.width;
            data[1] = (JAVA_INT)v.bounds.size.height;
        }
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)g);
}

void com_codename1_impl_ios_IOSNative_peerInitialized___long_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, int x, int y, int w, int h) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIView* v = (BRIDGE_CAST UIView*)((void *)peer);
        if([v superview] == nil) {
            [[CodenameOne_GLViewController instance].view addPeerComponent:v];
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
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIView* v = (BRIDGE_CAST UIView*)((void *)peer);
        if(v.superview != nil) {
            [v removeFromSuperview];
            repaintUI();
        }
        POOL_END();
    });
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
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio = 0;
JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___java_lang_String_java_lang_Runnable(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT uri, JAVA_OBJECT onCompletion) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSString* ns = toNSString(CN1_THREAD_GET_STATE_PASS_ARG uri);
        if([ns hasPrefix:@"file:/"]) {
            ns = fixFilePath(ns);
            NSURL* nu = [NSURL fileURLWithPath:ns];
            ns = [nu absoluteString];
        }
        com_codename1_impl_ios_IOSNative_createAudio = (JAVA_LONG)((BRIDGE_CAST void*)[[AudioPlayer alloc] initWithURL:ns callback:onCompletion]);
        POOL_END();
    });
    return com_codename1_impl_ios_IOSNative_createAudio;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___byte_1ARRAY_java_lang_Runnable(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT b, JAVA_OBJECT onCompletion) {
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
        com_codename1_impl_ios_IOSNative_createAudio = (JAVA_LONG)((BRIDGE_CAST void*)[[AudioPlayer alloc] initWithNSData:d callback:onCompletion]);
        POOL_END();
    });
    return com_codename1_impl_ios_IOSNative_createAudio;
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
    int horizontal = 2;
    if(n7) {
        horizontal = 3;
    }
    DrawGradient* d = [[DrawGradient alloc] initWithArgs:horizontal startColorA:n1 endColorA:n2 xA:n3 yA:n4 widthA:n5 heightA:n6 relativeXA:0 relativeYA:0 relativeSizeA:0];
    [CodenameOne_GLViewController upcoming:d];
#ifndef CN1_USE_ARC
    [d release];
#endif
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_fillRectRadialGradientMutable___int_int_int_int_int_int_float_float_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT width, JAVA_INT height, JAVA_FLOAT relativeX, JAVA_FLOAT relativeY, JAVA_FLOAT relativeSize) {
    POOL_BEGIN();
    
    CGFloat components[8] = {
        ((float)((n1 & 0xFF0000) >> 16))/255.0,
        ((float)(n1 & 0xff00 >> 8))/255.0,
        ((float)(n1 & 0xff))/255.0,
        1.0,
        ((float)((n2 & 0xFF0000) >> 16))/255.0,
        ((float)(n2 & 0xff00 >> 8))/255.0,
        ((float)(n2 & 0xff))/255.0,
        1.0 };
    size_t num_locations = 2;
    CGFloat locations[2] = { 0.0, 1.0 };
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGGradientRef myGradient = CGGradientCreateWithColorComponents (colorSpace, components, locations, num_locations);
    [UIColorFromRGB(n2, 255) set];
    CGContextFillRect(UIGraphicsGetCurrentContext(), CGRectMake(0, 0, width, height));
    CGPoint myCentrePoint = CGPointMake(relativeX * width, relativeY * height);
    float myRadius = MIN(width, height) * relativeSize;
    CGContextDrawRadialGradient (UIGraphicsGetCurrentContext(), myGradient, myCentrePoint,
                                 0, myCentrePoint, myRadius,
                                 kCGGradientDrawsAfterEndLocation);
    CGColorSpaceRelease(colorSpace);
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_fillLinearGradientMutable___int_int_int_int_int_int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT width, JAVA_INT height, JAVA_BOOLEAN n7) {
    POOL_BEGIN();
    CGFloat components[8] = {
        ((float)((n1 >> 16) & 0xff))/255.0,
        ((float)((n1 >> 8) & 0xFF))/255.0,
        ((float)(n1 & 0xff))/255.0,
        1.0,
        ((float)((n2 >> 16) & 0xFF))/255.0,
        ((float)((n2 >> 8) & 0xFF))/255.0,
        ((float)(n2 & 0xff))/255.0,
        1.0 };
    
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

UIWebView* com_codename1_impl_ios_IOSNative_createBrowserComponent = nil;
JAVA_LONG com_codename1_impl_ios_IOSNative_createBrowserComponent___java_lang_Object(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT obj) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        com_codename1_impl_ios_IOSNative_createBrowserComponent = [[UIWebView alloc] initWithFrame:CGRectMake(3000, 0, 200, 200)];
        com_codename1_impl_ios_IOSNative_createBrowserComponent.backgroundColor = [UIColor clearColor];
        com_codename1_impl_ios_IOSNative_createBrowserComponent.opaque = NO;
        com_codename1_impl_ios_IOSNative_createBrowserComponent.autoresizesSubviews = YES;
        UIWebViewEventDelegate *del = [[UIWebViewEventDelegate alloc] initWithCallback:obj];
        com_codename1_impl_ios_IOSNative_createBrowserComponent.delegate = del;
        com_codename1_impl_ios_IOSNative_createBrowserComponent.autoresizingMask=(UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth);
        [com_codename1_impl_ios_IOSNative_createBrowserComponent setAllowsInlineMediaPlayback:YES];
#ifndef CN1_USE_ARC
        [com_codename1_impl_ios_IOSNative_createBrowserComponent retain];
#endif
    });
    UIWebView* r = com_codename1_impl_ios_IOSNative_createBrowserComponent;
    com_codename1_impl_ios_IOSNative_createBrowserComponent = nil;
    return (JAVA_LONG)((BRIDGE_CAST void*)r);
}

void com_codename1_impl_ios_IOSNative_setBrowserPage___long_java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT html, JAVA_OBJECT baseUrl) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        [w loadHTMLString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG html) baseURL:[NSURL URLWithString:toNSString(CN1_THREAD_STATE_PASS_ARG baseUrl)]];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setBrowserUserAgent___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT ua) {
    NSString *_ua = toNSString(CN1_THREAD_GET_STATE_PASS_ARG ua);
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        //UIWebView* w = (UIWebView*)peer;
        NSDictionary *dictionary = [NSDictionary dictionaryWithObjectsAndKeys:_ua, @"UserAgent", nil];
        [[NSUserDefaults standardUserDefaults] registerDefaults:dictionary];
        POOL_END();
    });
}


void com_codename1_impl_ios_IOSNative_setPinchToZoomEnabled___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN enabled) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        
        w.scalesPageToFit=enabled;
        
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setNativeBrowserScrollingEnabled___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN enabled) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        
        w.scrollView.scrollEnabled = NO;
        w.scrollView.bounces = NO;
        
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setBrowserURL___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT url) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        NSString *str = toNSString(CN1_THREAD_GET_STATE_PASS_ARG url);
        NSURL* nu = [NSURL URLWithString:str];
        NSURLRequest* r = [NSURLRequest requestWithURL:nu];
        [w loadRequest:r];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_browserBack___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        [w goBack];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_browserStop___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        [w stopLoading];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_browserClearHistory___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
}

void com_codename1_impl_ios_IOSNative_browserExecute___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT javaScript) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        [w stringByEvaluatingJavaScriptFromString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG javaScript)];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_browserForward___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        [w goForward];
        POOL_END();
    });
}

JAVA_BOOLEAN booleanResponse = 0;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_browserHasBack___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        booleanResponse = [w canGoBack];
        POOL_END();
    });
    return booleanResponse;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_browserHasForward___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        booleanResponse = [w canGoForward];
        POOL_END();
    });
    return booleanResponse;
}

void com_codename1_impl_ios_IOSNative_browserReload___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        [w reload];
        POOL_END();
    });
}

JAVA_OBJECT returnString;
JAVA_OBJECT com_codename1_impl_ios_IOSNative_getBrowserTitle___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        NSString* theTitle = [w stringByEvaluatingJavaScriptFromString:@"document.title"];
        returnString = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG theTitle);
        POOL_END();
    });
    return returnString;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getBrowserURL___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        returnString = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG w.request.URL.absoluteString);
        POOL_END();
    });
    return returnString;
}

void registerVideoCallback(CN1_THREAD_STATE_MULTI_ARG MPMoviePlayerController *moviePlayer, JAVA_INT callbackId) {
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

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponent___java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT str, JAVA_INT onCompletionCallbackId) {
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
        [moviePlayerInstance prepareToPlay];
#ifdef AUTO_PLAY_VIDEO
        [moviePlayerInstance play];
#endif
        moviePlayerInstance.controlStyle = MPMovieControlStyleEmbedded;
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
}



void com_codename1_impl_ios_IOSNative_removeNotificationCenterObserver___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG observerPeer) {
    [[NSNotificationCenter defaultCenter] removeObserver:(void *)observerPeer];
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponent___java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT str, JAVA_INT onCompletionCallbackId) {
    __block MPMoviePlayerViewController* moviePlayerInstance;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSURL* u = [NSURL URLWithString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG str)];
        moviePlayerInstance = [[MPMoviePlayerViewController alloc] initWithContentURL:u];
        registerVideoCallback(CN1_THREAD_GET_STATE_PASS_ARG moviePlayerInstance.moviePlayer, onCompletionCallbackId);
#ifndef AUTO_PLAY_VIDEO
        moviePlayerInstance.moviePlayer.shouldAutoplay = NO;
#endif
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponent___byte_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT dataObject, JAVA_INT onCompletionCallbackId) {
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
        moviePlayerInstance.useApplicationAudioSession = NO;
        [moviePlayerInstance prepareToPlay];
#ifdef AUTO_PLAY_VIDEO
        [moviePlayerInstance play];
#endif
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponent___byte_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT dataObject, JAVA_INT onCompletionCallbackId) {
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
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponentNSData___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_INT onCompletionCallbackId) {
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
        moviePlayerInstance.useApplicationAudioSession = NO;
//#ifndef CN1_USE_ARC
//        [moviePlayerInstance retain];
//#endif
        [moviePlayerInstance prepareToPlay];
#ifdef AUTO_PLAY_VIDEO
        [moviePlayerInstance play];
#endif
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponentNSData___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_INT onCompletionCallbackId) {
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
}

void com_codename1_impl_ios_IOSNative_sendEmailMessage___java_lang_String_1ARRAY_java_lang_String_java_lang_String_java_lang_String_1ARRAY_java_lang_String_1ARRAY_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                                                                                                                                           JAVA_OBJECT  recipients, JAVA_OBJECT  subject, JAVA_OBJECT content, JAVA_OBJECT attachment, JAVA_OBJECT attachmentMimeType, JAVA_BOOLEAN htmlMail) {
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
}

void com_codename1_impl_ios_IOSNative_startVideoComponent___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        [m play];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_stopVideoComponent___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        [m stop];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_pauseVideoComponent___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        [m pause];
        POOL_END();
    });
}

JAVA_INT com_codename1_impl_ios_IOSNative_getMediaTimeMS___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    NSObject* obj = (BRIDGE_CAST NSObject*)peer;
    if([obj isKindOfClass:[MPMoviePlayerController class]]) {
        MPMoviePlayerController* m = (MPMoviePlayerController*) obj;
        return (int)m.currentPlaybackTime * 1000;
    }
    return 0;
}

JAVA_INT com_codename1_impl_ios_IOSNative_setMediaTimeMS___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT time) {
    NSObject* obj = (BRIDGE_CAST NSObject*)peer;
    if([obj isKindOfClass:[MPMoviePlayerController class]]) {
        MPMoviePlayerController* m = (MPMoviePlayerController*) obj;
        m.currentPlaybackTime = time/1000;
        return (int)m.currentPlaybackTime * 1000;
    }
    return 0;
}

int responseGetMediaDuration = 0;
JAVA_INT com_codename1_impl_ios_IOSNative_getMediaDuration___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        responseGetMediaDuration = (int)m.duration * 1000;
        POOL_END();
    });
    return responseGetMediaDuration;
}

void com_codename1_impl_ios_IOSNative_setMediaBgArtist___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT artist) {
    NSString *_artist = toNSString(CN1_THREAD_GET_STATE_PASS_ARG artist);
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
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

void com_codename1_impl_ios_IOSNative_setMediaBgAlbumCover___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if ([MPNowPlayingInfoCenter class])  {
            GLUIImage* glll = (BRIDGE_CAST GLUIImage*)((void *)peer);
            UIImage* i = [glll getImage];
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
}


JAVA_BOOLEAN responseIsVideoPlaying = 0;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoPlaying___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        responseIsVideoPlaying = m.playbackState == MPMoviePlaybackStatePlaying;
        POOL_END();
    });
    return responseIsVideoPlaying;
}

void com_codename1_impl_ios_IOSNative_setVideoFullScreen___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN fullscreen) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        [m setFullscreen:fullscreen];
        POOL_END();
    });
}

JAVA_BOOLEAN responseIsVideoFullScreen = 0;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoFullScreen___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    responseIsVideoFullScreen = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        responseIsVideoFullScreen = [m isFullscreen];
        POOL_END();
    });
    return responseIsVideoFullScreen;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getVideoViewPeer___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
    return (JAVA_LONG)((BRIDGE_CAST void*)m.view);
}

void com_codename1_impl_ios_IOSNative_showNativePlayerController___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerViewController* m = (BRIDGE_CAST MPMoviePlayerViewController*) ((void *)peer);
        m.moviePlayer.shouldAutoplay = NO;
        [[CodenameOne_GLViewController instance] presentMoviePlayerViewControllerAnimated:m];
        POOL_END();
    });
}


CLLocationManager* com_codename1_impl_ios_IOSNative_createCLLocation = nil;
JAVA_LONG com_codename1_impl_ios_IOSNative_createCLLocation__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
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
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getCurrentLocationObject___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
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
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationLatitude___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.coordinate.latitude;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationAltitude___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.altitude;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationLongtitude___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.coordinate.longitude;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationAccuracy___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.horizontalAccuracy;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationDirection___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.course;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationVelocity___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.speed;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getLocationTimeStamp___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    NSTimeInterval t = [loc.timestamp timeIntervalSince1970];
    return (JAVA_LONG)(t * 1000.0);
}

UIPopoverController* popoverController;
void com_codename1_impl_ios_IOSNative_captureCamera___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN movie) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIImagePickerControllerSourceType sourceType = UIImagePickerControllerSourceTypeCamera; // default
        popoverController = nil;
        
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
            } else {
                pickerController.mediaTypes = [NSArray arrayWithObjects:@"public.image", nil];
            }
            
            if(popoverSupported() && sourceType != UIImagePickerControllerSourceTypeCamera)
            {
#ifndef CN1_USE_ARC
                popoverController = [[[NSClassFromString(@"UIPopoverController") alloc]
                                      initWithContentViewController:pickerController] autorelease];
#else
                popoverController = [[NSClassFromString(@"UIPopoverController") alloc]
                                     initWithContentViewController:pickerController];
#endif
                popoverController.delegate = [CodenameOne_GLViewController instance];
                [popoverController presentPopoverFromRect:CGRectMake(0,32,320,480)
                                                   inView:[[CodenameOne_GLViewController instance] view]
                                 permittedArrowDirections:UIPopoverArrowDirectionAny
                                                 animated:YES];
#ifndef CN1_USE_ARC
                [popoverController retain];
#endif
            }
            else
            {
                [[CodenameOne_GLViewController instance] presentModalViewController:pickerController animated:YES];
            }
            POOL_END();
        }
    });
}

void com_codename1_impl_ios_IOSNative_openGallery___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIImagePickerControllerSourceType sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
        if(![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypePhotoLibrary]) {
            if(![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeSavedPhotosAlbum]) {
                return;
            }
            sourceType = UIImagePickerControllerSourceTypeSavedPhotosAlbum;
        }
        popoverController = nil;
        
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
#ifndef CN1_USE_ARC
            popoverController = [[[NSClassFromString(@"UIPopoverController") alloc]
                                  initWithContentViewController:pickerController] autorelease];
#else
            popoverController = [[NSClassFromString(@"UIPopoverController") alloc]
                                 initWithContentViewController:pickerController];
#endif
            popoverController.delegate = [CodenameOne_GLViewController instance];
            [popoverController presentPopoverFromRect:CGRectMake(0,32,320,480)
                                               inView:[[CodenameOne_GLViewController instance] view]
                             permittedArrowDirections:UIPopoverArrowDirectionAny
                                             animated:YES];
#ifndef CN1_USE_ARC
            [popoverController retain];
#endif
        } else {
            [[CodenameOne_GLViewController instance] presentModalViewController:pickerController animated:YES];
        }
        POOL_END();
    });
}
int popoverSupported()
{
    return ( NSClassFromString(@"UIPopoverController") != nil) &&  (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUDID__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return fromNSString(CN1_THREAD_STATE_PASS_ARG [OpenUDID value]);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getOSVersion__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return fromNSString(CN1_THREAD_STATE_PASS_ARG [[UIDevice currentDevice] systemVersion]);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getDeviceName__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return fromNSString(CN1_THREAD_STATE_PASS_ARG [[UIDevice currentDevice] name]);
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
            if (isIOS7()) {
                l.pausesLocationUpdatesAutomatically = NO;
            }
            break;
        case 1: // MEDIUM PRIORITY
            l.desiredAccuracy = kCLLocationAccuracyHundredMeters;
            l.distanceFilter = 100;
            if (isIOS7()) {
                l.pausesLocationUpdatesAutomatically = YES;
            }
            break;
        case 2 : // LOW PRIORITY
            l.desiredAccuracy = kCLLocationAccuracyThreeKilometers;
            l.distanceFilter = 3000;
            if (isIOS7()) {
                l.pausesLocationUpdatesAutomatically = YES;
            }
            break;
            
        default :
            l.desiredAccuracy = kCLLocationAccuracyHundredMeters;
            l.distanceFilter = kCLDistanceFilterNone;
            if (isIOS7()) {
                l.pausesLocationUpdatesAutomatically = NO;
            }
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
    [l startUpdatingLocation];
}

void com_codename1_impl_ios_IOSNative_stopUpdatingLocation___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
    [l stopUpdatingLocation];
}

void com_codename1_impl_ios_IOSNative_startUpdatingBackgroundLocation___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
    l.delegate = [CodenameOne_GLViewController instance];
    
    [l startMonitoringSignificantLocationChanges];
}

void com_codename1_impl_ios_IOSNative_stopUpdatingBackgroundLocation___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
    [l stopMonitoringSignificantLocationChanges];
}


//native void addGeofencing(long peer, double lat, double lng, double radius, long expiration, String id);
void com_codename1_impl_ios_IOSNative_addGeofencing___long_double_double_double_long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObj, JAVA_LONG peer, JAVA_DOUBLE lat, JAVA_DOUBLE lng, JAVA_DOUBLE radius, JAVA_LONG expires, JAVA_OBJECT geoId) {
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
}


//    native void removeGeofencing(String id);
void com_codename1_impl_ios_IOSNative_removeGeofencing___long_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT geoId) {
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
    for (CLRegion *region in [l monitoredRegions]) {
        if ([[region identifier] isEqualToString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG geoId)]) {
            [l stopMonitoringForRegion:region];
        }
    }
}

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

JAVA_VOID com_codename1_impl_ios_IOSNative_refreshContacts__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    globalAddressBook = nil;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isContactsPermissionGranted__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
    getAddressBook();
    POOL_END();
    return grantedPermission;
}


void throwError(CFErrorRef error) {
    if (error != nil) {
        NSLog(@"error %@", error);
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
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_deleteContact___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT i) {
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
}


JAVA_INT com_codename1_impl_ios_IOSNative_getContactCount___boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_BOOLEAN includeNumbers) {
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
}

JAVA_INT com_codename1_impl_ios_IOSNative_countLinkedContacts___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT recId) {
    POOL_BEGIN();
    ABRecordRef i = ABAddressBookGetPersonWithRecordID(getAddressBook(), recId);
    NSArray *linkedRecordsArray = (__bridge NSArray *)ABPersonCopyArrayOfAllLinkedPeople(i);
    int numLinked = [linkedRecordsArray count];
    [linkedRecordsArray release];
    POOL_END();
    return numLinked;
}

#ifdef NEW_CODENAME_ONE_VM
JAVA_INT com_codename1_impl_ios_IOSNative_countLinkedContacts___int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT recId) {
    return com_codename1_impl_ios_IOSNative_countLinkedContacts___int(CN1_THREAD_STATE_PASS_ARG instanceObject, recId);
}
#endif



void com_codename1_impl_ios_IOSNative_getLinkedContactIds___int_int_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT num, JAVA_INT refId, JAVA_OBJECT out) {
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
}


void com_codename1_impl_ios_IOSNative_getContactRefIds___int_1ARRAY_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT intArray, JAVA_BOOLEAN includeNumbers) {
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
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonFirstName___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    NSString* k = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonFirstNameProperty);
    JAVA_OBJECT ret = fromNSString(CN1_THREAD_STATE_PASS_ARG k);
    POOL_END();
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonSurnameName___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    NSString* k = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonLastNameProperty);
    JAVA_OBJECT ret = fromNSString(CN1_THREAD_STATE_PASS_ARG k);
    POOL_END();
    return ret;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getPersonPhoneCount___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    //POOL_BEGIN();
    //POOL_END();
    return 1;
}

JAVA_OBJECT copyValueAsString(CN1_THREAD_STATE_MULTI_ARG ABMultiValueRef r) {
    JAVA_OBJECT ret = JAVA_NULL;
    if(ABMultiValueGetCount(r) > 0) {
        NSString *k = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(r, 0);
        ret = fromNSString(CN1_THREAD_STATE_PASS_ARG k);
    }
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPhone___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    ABMultiValueRef k = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonPhoneProperty);
    JAVA_OBJECT ret = copyValueAsString(CN1_THREAD_STATE_PASS_ARG k);
    POOL_END();
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPhoneType___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
    POOL_BEGIN();
    //ABRecordRef i = (ABRecordRef)peer;
    //ABMultiValueRef k = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonPhoneMainLabel);
    //JAVA_OBJECT ret = copyValueAsString(k);
    JAVA_OBJECT ret = fromNSString(CN1_THREAD_STATE_PASS_ARG @"work");
    POOL_END();
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPrimaryPhone___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    ABMultiValueRef k = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonPhoneProperty);
    JAVA_OBJECT ret = copyValueAsString(CN1_THREAD_STATE_PASS_ARG k);
    POOL_END();
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonEmail___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    ABMultiValueRef emails = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonEmailProperty);
    JAVA_OBJECT ret = copyValueAsString(CN1_THREAD_STATE_PASS_ARG emails);
    POOL_END();
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonAddress___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    NSString* k = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonAddressProperty);
    JAVA_OBJECT ret = fromNSString(CN1_THREAD_STATE_PASS_ARG k);
    POOL_END();
    return ret;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createPersonPhotoImage___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    GLUIImage* g = nil;
    if(ABPersonHasImageData(i)){
        UIImage* img = [UIImage imageWithData:(BRIDGE_CAST NSData *)ABPersonCopyImageData(i)];
        g = [[GLUIImage alloc] initWithImage:img];
    }
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)g);
}

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

void com_codename1_impl_ios_IOSNative_updatePersonWithRecordID___int_com_codename1_contacts_Contact_boolean_boolean_boolean_boolean_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT recId, JAVA_OBJECT cnt,
                                                                                                                                            JAVA_BOOLEAN includesFullName, JAVA_BOOLEAN includesPicture, JAVA_BOOLEAN includesNumbers, JAVA_BOOLEAN includesEmail, JAVA_BOOLEAN includeAddress) {
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
        //NSLog(@"%@", toNSString(CN1_THREAD_STATE_PASS_ARG com_codename1_contacts_Contact_getDisplayName___R_java_lang_String(CN1_THREAD_STATE_PASS_ARG cnt)));
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
            UIImage* img = [UIImage imageWithData:(BRIDGE_CAST NSData *)ABPersonCopyImageDataWithFormat(i, kABPersonImageFormatThumbnail)];
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
}


JAVA_LONG com_codename1_impl_ios_IOSNative_getPersonWithRecordID___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT recId) {
    POOL_BEGIN();
    ABRecordRef i = ABAddressBookGetPersonWithRecordID(getAddressBook(), recId);
#ifndef CN1_USE_ARC
    [i retain];
#endif
    POOL_END();
    return (JAVA_LONG)i;
}

void com_codename1_impl_ios_IOSNative_dial___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT phone) {
    POOL_BEGIN();
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:toNSString(CN1_THREAD_STATE_PASS_ARG phone)]];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_sendSMS___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                                                  JAVA_OBJECT  number, JAVA_OBJECT  text) {
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
}

extern int pendingRemoteNotificationRegistrations;

void com_codename1_impl_ios_IOSNative_registerPush__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_CN1_PUSH2
    dispatch_async(dispatch_get_main_queue(), ^{
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
            [[UIApplication sharedApplication] registerForRemoteNotificationTypes:
             (UIRemoteNotificationTypeBadge | UIRemoteNotificationTypeSound | UIRemoteNotificationTypeAlert)];
        }
    });
#endif
}

void com_codename1_impl_ios_IOSNative_deregisterPush__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_CN1_PUSH2
    dispatch_async(dispatch_get_main_queue(), ^{
        [[UIApplication sharedApplication] unregisterForRemoteNotifications];
    });
#endif
}

void com_codename1_impl_ios_IOSNative_setBadgeNumber___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT number) {
// Removed this ifdef because we may need to badge the application even if push isn't supported.
//#ifdef INCLUDE_CN1_PUSH2
    dispatch_async(dispatch_get_main_queue(), ^{
        if(number == 0) {
            // Removed this because there could be repeating notifications
            //[[UIApplication sharedApplication] cancelAllLocalNotifications];
        }
        [UIApplication sharedApplication].applicationIconBadgeNumber = number;
    });
//#endif
}


UIImage* scaleImage(int destWidth, int destHeight, UIImage *img) {
    UIImage* scaledInstance = nil;
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
        scaledInstance = [UIImage imageWithCGImage:scaledImageRef];
        
        CGImageRelease(scaledImageRef);
        CGContextRelease(bmContext);
    }
    UIImage* scaled = scaledInstance;
    scaledInstance = nil;
    return scaled;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageFile___long_boolean_int_int_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG imagePeer, JAVA_BOOLEAN jpeg, int width, int height, JAVA_FLOAT quality) {
    __block int blockWidth = width;
    __block int blockHeight = height;
    __block NSData* data = nil;
#ifndef CN1_USE_ARC
    [(BRIDGE_CAST GLUIImage*)((void *)imagePeer) retain];
#endif
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        GLUIImage* glll = (BRIDGE_CAST GLUIImage*)((void *)imagePeer);
        UIImage* i = [glll getImage];
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


JAVA_LONG com_codename1_impl_ios_IOSNative_createAudioRecorder___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                                                  JAVA_OBJECT  destinationFile) {
    __block AVAudioRecorder* recorder = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AVAudioSession *audioSession = [AVAudioSession sharedInstance];
        NSError *err = nil;
        [audioSession setCategory :AVAudioSessionCategoryPlayAndRecord error:&err];
        if(err){
            NSLog(@"audioSession: %@ %d %@", [err domain], [err code], [[err userInfo] description]);
            return;
        }
        err = nil;
        [audioSession setActive:YES error:&err];
        if(err){
            NSLog(@"audioSession: %@ %d %@", [err domain], [err code], [[err userInfo] description]);
            return;
        }
        if (isIOS7()) {
            NSLog(@"Asking for record permission");
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
                    
                    NSLog(@"Recording audio to: %@", filePath);
                    NSDictionary *recordSettings = [[NSDictionary alloc] initWithObjectsAndKeys:
                                                    [NSNumber numberWithFloat: 16000.0], AVSampleRateKey,
                                                    [NSNumber numberWithInt: kAudioFormatMPEG4AAC],AVFormatIDKey,
                                                    [NSNumber numberWithInt: 1], AVNumberOfChannelsKey,
                                                    nil];
                    NSError *error = nil;
                    recorder = [[AVAudioRecorder alloc] initWithURL: [NSURL fileURLWithPath:ns]
                                                           settings: recordSettings
                                                              error: &error];
                    if(error != nil) {
                        NSLog(@"Error in recording: %@", [error localizedDescription]);
                    }
                    recorder.delegate = [CodenameOne_GLViewController instance];
                } else {
                    recorder = nil;
                }
                POOL_END();
            }];
        } else {
            NSString * filePath = toNSString(CN1_THREAD_GET_STATE_PASS_ARG destinationFile);
            
            // cleanup older file if it exists in this location
            NSFileManager* fm = [[NSFileManager alloc] init];
            NSString* ns = fixFilePath(ns);
            if([fm fileExistsAtPath:ns]) {
                [fm removeItemAtPath:ns error:nil];
            }
            
            NSLog(@"Recording audio to: %@", filePath);
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
                NSLog(@"Error in recording: %@", [error localizedDescription]);
            }
            recorder.delegate = [CodenameOne_GLViewController instance];
        }
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)recorder);
}

void com_codename1_impl_ios_IOSNative_startAudioRecord___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                              JAVA_LONG  peer) {
    AVAudioRecorder* recorder = (BRIDGE_CAST AVAudioRecorder*)((void *)peer);
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if(![recorder prepareToRecord]) {
            NSLog(@"Error preparing to record");
        }
        if(![recorder record]) {
            NSLog(@"Error in recording record returned false for some reason?");
        }
#ifndef CN1_USE_ARC
        [recorder retain];
#endif
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_pauseAudioRecord___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                              JAVA_LONG  peer) {
    AVAudioRecorder* recorder = (BRIDGE_CAST AVAudioRecorder*)((void *)peer);
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        [recorder pause];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_cleanupAudioRecord___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                                JAVA_LONG  peer) {
    AVAudioRecorder* recorder = (BRIDGE_CAST AVAudioRecorder*)((void *)peer);
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        [recorder stop];
#ifndef CN1_USE_ARC
        [recorder release];
#endif
        POOL_END();
    });
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

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlDbExists___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    POOL_BEGIN();
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];
    
    NSString* nsSrc = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    
    NSString* foofile = [documentsPath stringByAppendingPathComponent:nsSrc];
    BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:foofile];
    POOL_END();
    return fileExists;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlDbCreateAndOpen___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    POOL_BEGIN();
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];
    
    NSString* nsSrc = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    
    NSString* foofile = [documentsPath stringByAppendingPathComponent:nsSrc];
    
    sqlite3 *db;
    int rc = sqlite3_open([foofile UTF8String], &db);
    
    POOL_END();
    return (JAVA_LONG)db;
}

void com_codename1_impl_ios_IOSNative_sqlDbDelete___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    POOL_BEGIN();
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];
    
    NSString* nsSrc = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    
    NSString* foofile = [documentsPath stringByAppendingPathComponent:nsSrc];
    [[NSFileManager defaultManager] removeItemAtPath:foofile error:nil];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_sqlDbClose___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG db) {
    sqlite3_free((sqlite3*)db);
}

void com_codename1_impl_ios_IOSNative_sqlDbExec___long_java_lang_String_java_lang_String_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT sql, JAVA_OBJECT args) {
    sqlite3* db = (sqlite3*)dbPeer;
    const char* chrs = stringToUTF8(CN1_THREAD_STATE_PASS_ARG sql);
    
    if(args != nil) {
        sqlite3_stmt *addStmt = nil;
        char* errInfo;
        int result = sqlite3_prepare_v2(db, chrs, -1, &addStmt, 0);
#ifdef NEW_CODENAME_ONE_VM
        if (result != SQLITE_OK) {
            //NSString *errStr = [NSString stringWithUTF8String:errInfo];
            //NSLog(@"Error : %@", errStr);
            JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
            java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, newStringFromCString(CN1_THREAD_STATE_PASS_ARG sqlite3_errmsg(db)));
            throwException(threadStateData, ex);
            return;
        }
#endif
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* stringArray = args;
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)stringArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
        int count = stringArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
        JAVA_ARRAY_OBJECT* data = (JAVA_OBJECT*)((JAVA_ARRAY)args)->data;
        int count = ((JAVA_ARRAY)args)->length;
#endif
        
        for(int iter = 0 ; iter < count ; iter++) {
            JAVA_OBJECT str = (JAVA_OBJECT)data[iter];
            const char* chrs = stringToUTF8(CN1_THREAD_STATE_PASS_ARG str);
            sqlite3_bind_text(addStmt, iter + 1, chrs, -1, SQLITE_TRANSIENT);
        }
        result = sqlite3_step(addStmt);
#ifdef NEW_CODENAME_ONE_VM
        if (result != SQLITE_ROW && result != SQLITE_DONE && result != SQLITE_OK) {
            //NSString *errStr = [NSString stringWithUTF8String:errInfo];
            //NSLog(@"Error : %@", errStr);
            JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
            java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, fromNSString(CN1_THREAD_STATE_PASS_ARG [NSString stringWithFormat:@"SQL error in step.  Code: %@", [NSString stringWithUTF8String:sqlite3_errmsg(db)]]));
            throwException(threadStateData, ex);
            return;
        }
#endif
        result = sqlite3_finalize(addStmt);
#ifdef NEW_CODENAME_ONE_VM
        if (result != SQLITE_OK) {
            //NSString *errStr = [NSString stringWithUTF8String:errInfo];
            //NSLog(@"Error : %@", errStr);
            JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
            java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, fromNSString(CN1_THREAD_STATE_PASS_ARG [NSString stringWithFormat:@"SQL error in step.  Code: %@", [NSString stringWithUTF8String:sqlite3_errmsg(db)]]));
            throwException(threadStateData, ex);
            return;
        }
#endif
    } else {
        char * errInfo;
        int result = sqlite3_exec(db, chrs, 0, 0, &errInfo);
        
#ifdef NEW_CODENAME_ONE_VM
        if (result != SQLITE_OK) {
            //NSString *errStr = [NSString stringWithUTF8String:errInfo];
            //NSLog(@"Error : %@", errStr);
            JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
            java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, newStringFromCString(CN1_THREAD_STATE_PASS_ARG errInfo));
            throwException(threadStateData, ex);
            return;
        }
#endif
    }
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlDbExecQuery___long_java_lang_String_java_lang_String_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT sql, JAVA_OBJECT args) {
    sqlite3* db = (sqlite3*)dbPeer;
    const char* chrs = stringToUTF8(CN1_THREAD_STATE_PASS_ARG sql);
    sqlite3_stmt *addStmt = nil;
    int result = sqlite3_prepare_v2(db, chrs, -1, &addStmt, 0);
    
#ifdef NEW_CODENAME_ONE_VM
    if (result != SQLITE_OK) {
        //NSString *errStr = [NSString stringWithUTF8String:errInfo];
        //NSLog(@"Error : %@", errStr);
        JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
        java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, newStringFromCString(CN1_THREAD_STATE_PASS_ARG sqlite3_errmsg(db)));
        throwException(threadStateData, ex);
        return 0;
    }
#endif
    
    if(args != nil) {
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* stringArray = args;
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)stringArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
        int count = stringArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)args)->data;
        int count = ((JAVA_ARRAY)args)->length;
#endif
        
        for(int iter = 0 ; iter < count ; iter++) {
            JAVA_OBJECT str = (JAVA_OBJECT)data[iter];
            const char* chrs = stringToUTF8(CN1_THREAD_STATE_PASS_ARG str);
            result = sqlite3_bind_text(addStmt, iter + 1, chrs, -1, SQLITE_TRANSIENT);
#ifdef NEW_CODENAME_ONE_VM
            if (result != SQLITE_OK) {
                //NSString *errStr = [NSString stringWithUTF8String:errInfo];
                //NSLog(@"Error : %@", errStr);
                JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
                java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, newStringFromCString(CN1_THREAD_STATE_PASS_ARG sqlite3_errmsg(db)));
                throwException(threadStateData, ex);
                return 0;
            }
#endif
        }
    }
    return (JAVA_LONG)addStmt;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorFirst___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    int result = sqlite3_reset((sqlite3_stmt *)statementPeer);
#ifdef NEW_CODENAME_ONE_VM
    if (result != SQLITE_OK && result != SQLITE_DONE && result != SQLITE_ROW) {
        //NSString *errStr = [NSString stringWithUTF8String:errInfo];
        //NSLog(@"Error : %@", errStr);
        JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
        java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, fromNSString(CN1_THREAD_STATE_PASS_ARG [NSString stringWithFormat:@"SQL error in step.  Code: %d", result]));
        throwException(threadStateData, ex);
        return 0;
    }
#endif
    return YES;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorNext___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    int result = sqlite3_step((sqlite3_stmt *)statementPeer);
    if (result == SQLITE_ROW) {
        return YES;
    }
#ifdef NEW_CODENAME_ONE_VM
    if (result != SQLITE_DONE && result != SQLITE_OK) {
        //NSString *errStr = [NSString stringWithUTF8String:errInfo];
        //NSLog(@"Error : %@", errStr);
        JAVA_OBJECT ex = __NEW_java_io_IOException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
        java_io_IOException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, fromNSString(CN1_THREAD_STATE_PASS_ARG [NSString stringWithFormat:@"SQL error in step.  Code: %d", result]));
        throwException(threadStateData, ex);
        return 0;
    }
#endif
    return NO;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlGetColName___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer, JAVA_INT index) {
    return xmlvm_create_java_string(CN1_THREAD_STATE_PASS_ARG sqlite3_column_name((sqlite3_stmt*)statementPeer, index));
}

void com_codename1_impl_ios_IOSNative_sqlCursorCloseStatement___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement) {
    sqlite3_finalize((sqlite3_stmt*)statement);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnBlob___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return nil;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnDouble___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_double((sqlite3_stmt*)statement, col);
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnFloat___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_double((sqlite3_stmt*)statement, col);
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnInteger___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_int((sqlite3_stmt*)statement, col);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnLong___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_int64((sqlite3_stmt*)statement, col);
}

JAVA_SHORT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnShort___long_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_int((sqlite3_stmt*)statement, col);
}

#ifdef NEW_CODENAME_ONE_VM
JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnString___long_int_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    enteringNativeAllocations();
    const char* result = (const char*)sqlite3_column_text((sqlite3_stmt*)statement, col);
    if(result == 0) {
        return JAVA_NULL;
    }
    JAVA_OBJECT str = __NEW_INSTANCE_java_lang_String(threadStateData);
    struct obj__java_lang_String* ss = (struct obj__java_lang_String*)str;
    NSString* ns = [NSString stringWithUTF8String:result];
    [ns retain];
    ss->java_lang_String_nsString = (JAVA_LONG)ns;
    
    JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_CHAR(threadStateData, [ns length]);
    ss->java_lang_String_value = destArr;
    ss->java_lang_String_count = [ns length];
    
    __block JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)destArr)->data;
    __block int length = 0;
    [ns enumerateSubstringsInRange:NSMakeRange(0, [ns length])
                           options:NSStringEnumerationByComposedCharacterSequences
                        usingBlock:^(NSString *substring, NSRange substringRange, NSRange enclosingRange, BOOL *stop) {
                            dest[length] = (JAVA_CHAR)[ns characterAtIndex:length];
                            length++;
                        }];
    finishedNativeAllocations();
    
    return str;
}
#else // NEW_CODENAME_ONE_VM
JAVA_OBJECT xmlvm_create_UTF8_java_string(const char* s) {
    if(s == 0) {
        return 0;
    }
    java_lang_String* str = __NEW_java_lang_String();
    int len = strlen(s);
    org_xmlvm_runtime_XMLVMArray* byteArray = XMLVMArray_createSingleDimension(__CLASS_byte, len);
    memcpy(byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_, s, len);
    java_lang_String___INIT____byte_1ARRAY_java_lang_String(str, byteArray, utf8String);
    return XMLVMUtil_getFromStringPool(str);
}


JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnString___long_int(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return xmlvm_create_UTF8_java_string(sqlite3_column_text((sqlite3_stmt*)statement, col));
}
#endif // NEW_CODENAME_ONE_VM

JAVA_INT com_codename1_impl_ios_IOSNative_sqlCursorGetColumnCount___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statement) {
    sqlite3_stmt *stmt = (sqlite3_stmt*)statement;
    return sqlite3_column_count(stmt);
}


JAVA_OBJECT productsArrayPending = nil;

void com_codename1_impl_ios_IOSNative_fetchProducts___java_lang_String_1ARRAY_com_codename1_payment_Product_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT skus, JAVA_OBJECT products) {
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
}

SKPayment *paymentInstance = nil;
void com_codename1_impl_ios_IOSNative_purchase___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT sku) {
    NSString *nsSku = toNSString(CN1_THREAD_STATE_PASS_ARG sku);
    dispatch_async(dispatch_get_main_queue(), ^{
        paymentInstance = [SKPayment paymentWithProductIdentifier:nsSku];
        [[SKPaymentQueue defaultQueue] addPayment:paymentInstance];
    });
}

void com_codename1_impl_ios_IOSNative_restorePurchases__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    [[SKPaymentQueue defaultQueue] restoreCompletedTransactions];
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_canMakePayments__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return (JAVA_BOOLEAN)[SKPaymentQueue canMakePayments];
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatInt___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT i) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSNumberFormatter *formatter = [[[NSNumberFormatter alloc] init] autorelease];
#else
    NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
#endif
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
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateStyle:NSDateFormatterMediumStyle];
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
    JAVA_ARRAY byteArray = (JAVA_ARRAY)arr;
    void* data = (void*)byteArray->data;
    NSData* d = [NSData dataWithBytes:data length:byteArray->length];
    return d;
}

JAVA_OBJECT nsDataToByteArr(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length], &class_array1__JAVA_BYTE, sizeof(JAVA_BYTE), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToBooleanArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length], &class_array1__JAVA_BOOLEAN, sizeof(JAVA_BOOLEAN), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToCharArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length], &class_array1__JAVA_CHAR, sizeof(JAVA_CHAR), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToShortArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length], &class_array1__JAVA_SHORT, sizeof(JAVA_SHORT), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToIntArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length], &class_array1__JAVA_INT, sizeof(JAVA_INT), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToLongArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length], &class_array1__JAVA_LONG, sizeof(JAVA_LONG), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToFloatArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length], &class_array1__JAVA_FLOAT, sizeof(JAVA_FLOAT), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToDoubleArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray(getThreadLocalData(), [d length], &class_array1__JAVA_DOUBLE, sizeof(JAVA_DOUBLE), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}
#else // NEW_CODENAME_ONE_VM
NSData* arrayToData(JAVA_OBJECT arr) {
    org_xmlvm_runtime_XMLVMArray* byteArray = (org_xmlvm_runtime_XMLVMArray*)arr;
    void* data = (void*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    NSData* d = [NSData dataWithBytes:data length:byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_];
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

JAVA_LONG com_codename1_impl_ios_IOSNative_createTruetypeFont___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    int pSize = 14;
    
    pSize *= scaleValue;
    POOL_BEGIN();
    NSString* str = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    UIFont* fnt = [UIFont fontWithName:str size:pSize];
#ifndef CN1_USE_ARC
    [fnt retain];
#endif
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)fnt);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_deriveTruetypeFont___long_boolean_boolean_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG uiFont, JAVA_BOOLEAN bold, JAVA_BOOLEAN italic, JAVA_FLOAT size) {
    POOL_BEGIN();
    UIFont* original = (BRIDGE_CAST UIFont*)((void *)uiFont);
    UIFont* fnt = [original fontWithSize:size];
#ifndef CN1_USE_ARC
    [fnt retain];
#endif
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)fnt);
}

void com_codename1_impl_ios_IOSNative_log___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    POOL_BEGIN();
    NSLog(@"%@", toNSString(CN1_THREAD_STATE_PASS_ARG name));
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
        NSLog(@"Invalid URL! You need to escape the characters of the URL in order for it work properly! %@", nsStr);
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
        JAVA_LONG expires = [[cookie expiresDate] timeIntervalSince1970];
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
                                 (secure ? @"1" : @""), NSHTTPCookieSecure,
                                 [NSDate dateWithTimeIntervalSince1970:expires], NSHTTPCookieExpires, Nil];
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
    __block JAVA_OBJECT out;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        out = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [w stringByEvaluatingJavaScriptFromString:toNSString(CN1_THREAD_GET_STATE_PASS_ARG javaScript)]);
        POOL_END();
    });
    return out;
}

JAVA_OBJECT java_util_TimeZone_getTimezoneId__(CN1_THREAD_STATE_SINGLE_ARG) {
    POOL_BEGIN();
    NSTimeZone *tzone = [NSTimeZone defaultTimeZone];
    NSString* n = [tzone name];
    //NSLog(@"java_util_TimeZone_getTimezoneId__ %@", n);
    JAVA_OBJECT str = fromNSString(CN1_THREAD_STATE_PASS_ARG n);
    POOL_END();
    return str;
}

JAVA_INT java_util_TimeZone_getTimezoneOffset___java_lang_String_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT name, JAVA_INT year, JAVA_INT month, JAVA_INT day, JAVA_INT timeOfDayMillis) {
    POOL_BEGIN();
    NSString* n = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    //NSLog(@"java_util_TimeZone_getTimezoneOffset___java_lang_String_long %@, %i", n, timeMillis / 1000);
    NSTimeZone *tzone = [NSTimeZone timeZoneWithName:n];
    NSDateComponents *comps = [[NSDateComponents alloc] init];
    [comps setDay:day];
    [comps setYear:year];
    [comps setMonth:month];
    [comps setMinute:timeOfDayMillis/60000];
    NSCalendar* cal = [NSCalendar currentCalendar];
    NSDate *date = [cal dateFromComponents:comps];
    JAVA_INT result = [tzone secondsFromGMTForDate:date] * 1000;
    POOL_END();
    return result;
}

JAVA_INT java_util_TimeZone_getTimezoneRawOffset___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT name) {
    POOL_BEGIN();
    NSString* n = toNSString(CN1_THREAD_STATE_PASS_ARG name);
    //NSLog(@"java_util_TimeZone_getTimezoneRawOffset___java_lang_String %@", n);
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
    //NSLog(@"java_util_TimeZone_isTimezoneDST___java_lang_String_long %@, %i", n, millis / 1000);
    NSTimeZone *tzone = [NSTimeZone timeZoneWithName:n];
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(millis / 1000)];
    JAVA_BOOLEAN result = [tzone isDaylightSavingTimeForDate:date];
    POOL_END();
    return result;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUserAgentString__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    __block JAVA_OBJECT c = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* webView = [[UIWebView alloc] initWithFrame:CGRectZero];
        NSString* userAgentString = [webView stringByEvaluatingJavaScriptFromString:@"navigator.userAgent"];
        c = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG userAgentString);
#ifndef CN1_USE_ARC
        [webView release];
#endif
        POOL_END();
    });
    
    return c;
}


bool datepickerPopover = NO;
#ifndef NEW_CODENAME_ONE_VM
org_xmlvm_runtime_XMLVMArray* pickerStringArray = nil;
#else
JAVA_OBJECT pickerStringArray = JAVA_NULL;
#endif
int stringPickerSelection;
NSDate* currentDatePickerDate;
UIPopoverController* popoverControllerInstance;
extern UIView *currentActionSheet;
JAVA_LONG defaultDatePickerDate;

void showPopupPickerView(CN1_THREAD_STATE_MULTI_ARG UIView *pickerView) {
    int SCREEN_HEIGHT = [CodenameOne_GLViewController instance].view.bounds.size.height;
    int SCREEN_WIDTH = [CodenameOne_GLViewController instance].view.bounds.size.width;
    UIView* fakeActionSheet = [[UIView alloc] initWithFrame:CGRectMake(0, SCREEN_HEIGHT-246, SCREEN_WIDTH, 246)];
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
        [pickerView setFrame:CGRectMake(0, 44, pickerView.frame.size.width, pickerView.frame.size.height)];
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
}
void com_codename1_impl_ios_IOSNative_openStringPicker___java_lang_String_1ARRAY_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT stringArray, JAVA_INT selection, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_INT preferredWidth, JAVA_INT preferredHeight) {

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
            UIView *popoverView = [[UIView alloc] init];
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
}


void com_codename1_impl_ios_IOSNative_openDatePicker___int_long_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT type, JAVA_LONG time, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_INT preferredWidth, JAVA_INT preferredHeightArg) {
    __block JAVA_INT preferredHeight = preferredHeightArg;
    com_codename1_impl_ios_IOSImplementation_foldKeyboard__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    pickerStringArray = nil;
    currentDatePickerDate = nil;
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
        }
        datePickerView.tag = 10;
        datePickerView.date = date;
        currentDatePickerDate = date;
#ifndef CN1_USE_ARC
        [currentDatePickerDate retain];
#endif
        defaultDatePickerDate = time;
        [datePickerView addTarget:[CodenameOne_GLViewController instance] action:@selector(datePickerChangeDate:) forControlEvents:UIControlEventValueChanged];
        if(isIPad()) {
            datepickerPopover = YES;
            stringPickerSelection = 0;
            UIViewController *vc = [[UIViewController alloc] init];
            
            UIView *popoverView = [[UIView alloc] init];
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
    NSString* someText = toNSString(CN1_THREAD_STATE_PASS_ARG text);
    BOOL useRect = rectangle ? YES:NO;
    __block CGRect cgrect = CGRectMake(0,0,0,0);
    if (useRect){
        cgrect = cn1RectToCGRect(CN1_THREAD_GET_STATE_PASS_ARG rectangle);
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSArray* dataToShare;
        if(imagePeer != 0) {
            GLUIImage* glll = (BRIDGE_CAST GLUIImage*)((void *)imagePeer);
            UIImage* i = [glll getImage];
            if(someText != nil) {
                dataToShare = [NSArray arrayWithObjects:someText, i, nil];
            } else {
                dataToShare = [NSArray arrayWithObjects:i, nil];
            }
        } else {
            dataToShare = [NSArray arrayWithObjects:someText, nil];
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
}


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
    if (editingComponent == nil) {
        return;
    }
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if(editingComponent != nil) {
            NSString* nsText = toNSString(CN1_THREAD_GET_STATE_PASS_ARG text);
            NSString* currText = ((UITextView*)editingComponent).text;
            if (![nsText isEqualToString:currText]) {
                ((UITextView*)editingComponent).text = nsText;
            }
        }
        POOL_END();
    });
    
}

JAVA_LONG com_codename1_impl_ios_IOSNative_connectSocket___java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT host, JAVA_INT port) {
    POOL_BEGIN();
    SocketImpl* impl = [[SocketImpl alloc] init];
    BOOL b = [impl connect:toNSString(CN1_THREAD_STATE_PASS_ARG host) port:port];
    POOL_END();
    if(b) {
        return (JAVA_LONG)impl;
    }
    return (JAVA_LONG)JAVA_NULL;
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
JAVA_LONG com_codename1_impl_ios_IOSNative_nativePathRendererCreate___int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT pix_boundsX, JAVA_INT pix_boundsY, JAVA_INT pix_boundsWidth, JAVA_INT pix_boundsHeight, JAVA_INT windingRule)
{
    if ( !rendererIsSetup ){
        rendererIsSetup = YES;
        Renderer_setup(1,1);
    }
    Renderer *renderer = (Renderer*)malloc(sizeof(Renderer));
    Renderer_init(renderer);
    Renderer_reset(renderer, pix_boundsX, pix_boundsY, pix_boundsWidth, pix_boundsHeight, windingRule);
    return (JAVA_LONG)renderer;
    
}
//native void nativePathRendererSetup(int subpixelLgPositionsX, int subpixelLgPositionsY);
void com_codename1_impl_ios_IOSNative_nativePathRendererSetup___int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT subpixelLgPositionsX, JAVA_INT subpixelLgPositionsY)
{
    if ( !rendererIsSetup ){
        rendererIsSetup = YES;
        
        Renderer_setup(subpixelLgPositionsX, subpixelLgPositionsY);
    }
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
    //NSLog(@"In getConsumer()");
    return &(((Renderer*)ptr)->consumer);
}

//native void nativePathConsumerMoveTo(long ptr, double x, double y);
void com_codename1_impl_ios_IOSNative_nativePathConsumerMoveTo___long_float_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr, JAVA_FLOAT x, JAVA_FLOAT y)
{
    //NSLog(@"In moveTo %g,%g", x,y);
    ((PathConsumer*)ptr)->moveTo((PathConsumer*)ptr,x,y);
    //NSLog(@"Finished moveTo");
}
//native void nativePathConsumerLineTo(long ptr, double x, double y);
void com_codename1_impl_ios_IOSNative_nativePathConsumerLineTo___long_float_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG ptr, JAVA_FLOAT x, JAVA_FLOAT y)
{
    //NSLog(@"In lineto %g,%g", x, y);
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
    //NSLog(@"Closing path");
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

extern void Java_com_codename1_impl_ios_IOSImplementation_drawTextureAlphaMaskImpl(GLuint textureName, int color, int alpha, int x, int y, int w, int h);
void com_codename1_impl_ios_IOSNative_drawTextureAlphaMask___long_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG textureName, JAVA_INT color, JAVA_INT alpha, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h)
{
    Java_com_codename1_impl_ios_IOSImplementation_drawTextureAlphaMaskImpl((GLuint)textureName, color, alpha, x, y, w, h);
    
    
}

void com_codename1_impl_ios_IOSNative_nativeDeleteTexture___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG textureName)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        GLuint tex = (GLuint)textureName;
        //POOL_BEGIN();
        glDeleteTextures(1, &tex);
        //POOL_END();
    });
}


#define min(a,b) ((a)<(b)?(a):(b))
#define max(a,b) ((a)>(b)?(a):(b))
#define abs(x) ((x)>0?(x):-(x))
JAVA_OBJECT com_codename1_impl_ios_IOSNative_nativePathRendererToARGB___long_int(JAVA_OBJECT instanceObject, JAVA_LONG renderer, JAVA_INT color)
{
    Renderer *r = (Renderer*)renderer;
    JAVA_INT outputBounds[4];
    
    Renderer_getOutputBounds(renderer, (JAVA_INT*)&outputBounds);
    if ( outputBounds[2] < 0 || outputBounds[3] < 0 ){
        return 0;
    }
    
    //GLuint tex=0;
    JAVA_INT x = min(outputBounds[0], outputBounds[2]);
    JAVA_INT y = min(outputBounds[1], outputBounds[3]);
    JAVA_INT width = outputBounds[2]-outputBounds[0];
    JAVA_INT height = outputBounds[3]-outputBounds[1];
    
    if ( width < 0 ) width = -width;
    if ( height < 0 ) height = -height;
    
    AlphaConsumer ac = {
        x,
        y,
        width,
        height,
    };
    
    //jbyte* maskArray = malloc(sizeof(jbyte)*ac.width*ac.height);
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* data = XMLVMArray_createSingleDimension(__CLASS_byte, ac.width*ac.height);
    
    //NSLog(@"Mask width %d height %d",
    //      ac.width,
    //      ac.height
    //      );
    ac.alphas = (JAVA_ARRAY_BYTE*)data->fields.org_xmlvm_runtime_XMLVMArray.array_;
    Renderer_produceAlphas(renderer, &ac);
    
    org_xmlvm_runtime_XMLVMArray* idata = XMLVMArray_createSingleDimension(__CLASS_int, ac.width*ac.height);
    JAVA_ARRAY_INT* iArr = (JAVA_ARRAY_INT*)idata->fields.org_xmlvm_runtime_XMLVMArray.array_;
    JAVA_ARRAY_BYTE* bArr = (JAVA_ARRAY_BYTE*)ac.alphas;
#else
    JAVA_OBJECT data = __NEW_ARRAY_JAVA_BYTE(CN1_THREAD_GET_STATE_PASS_ARG ac.width*ac.height);
    ac.alphas = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)data)->data;
    
    Renderer_produceAlphas(renderer, &ac);
    JAVA_OBJECT idata = __NEW_ARRAY_JAVA_INT(CN1_THREAD_GET_STATE_PASS_ARG ac.width*ac.height);
    JAVA_ARRAY_INT* iArr = (JAVA_ARRAY_INT*)((JAVA_ARRAY)idata)->data;
    JAVA_ARRAY_BYTE* bArr = (JAVA_ARRAY_BYTE*)ac.alphas;
#endif
    
    JAVA_INT len = ac.width*ac.height;
    for ( JAVA_INT i=0; i<len; i++){
        iArr[i] = color | (bArr[i] << 24);
        //NSLog(@"%d", iArr[i]);
    }
    
    return (JAVA_OBJECT)idata;
    
}


JAVA_LONG com_codename1_impl_ios_IOSNative_nativePathRendererCreateTexture___long(JAVA_OBJECT instanceObject, JAVA_LONG renderer)
{
#ifdef USE_ES2
    
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
        
        Renderer *r = (Renderer*)renderer;
        JAVA_INT outputBounds[4];
        
        Renderer_getOutputBounds(renderer, (JAVA_INT*)&outputBounds);
        if ( outputBounds[2] < 0 || outputBounds[3] < 0 ){
            //return 0;
            POOL_END();
            return;
        }
        
        GLuint tex=0;
        JAVA_INT x = min(outputBounds[0], outputBounds[2]);
        JAVA_INT y = min(outputBounds[1], outputBounds[3]);
        JAVA_INT width = outputBounds[2]-outputBounds[0];
        JAVA_INT height = outputBounds[3]-outputBounds[1];
        
        if ( width < 0 ) width = -width;
        if ( height < 0 ) height = -height;
        
        AlphaConsumer *ac = malloc(sizeof(AlphaConsumer));
        ac->originX = x;
        ac->originY = y;
        ac->width = width;
        ac->height = height;
        
        
        //NSLog(@"AC Width %d", ac.width);
        
        //jbyte maskArray[ac.width*ac.height];
        jbyte* maskArray = malloc(sizeof(jbyte)*ac->width*ac->height);
        
        //NSLog(@"Mask width %d height %d",
        //      ac.width,
        //      ac.height
        //      );
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
        free(maskArray);
        free(ac);
        glBindTexture(GL_TEXTURE_2D, 0);
        GLErrorLog;
        _glDisableClientState(GL_VERTEX_ARRAY);
        GLErrorLog;
        _glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        GLErrorLog;
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
    
    
    GLKMatrix4 mLeft = GLKMatrix4MakeWithArray(lhsData+lhsOffset*sizeof(JAVA_FLOAT));
    GLKMatrix4 mRight = GLKMatrix4MakeWithArray(rhsData+rhsOffset*sizeof(JAVA_FLOAT));
    GLKMatrix4 mResult = GLKMatrix4Multiply(mLeft, mRight);
    
    for ( int i=0; i<16; i++){
        resultData[i+resultOffset] = clamp_float_to_int(mResult.m[i]);
    }
    //memcpy(resultData+resultOffset*sizeof(JAVA_FLOAT), &mResult, 16*sizeof(JAVA_FLOAT));
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
        outData[d0++] = outputVector.v[0];
        outData[d0++] = outputVector.v[1];
        if (pointSize==3) {
            outData[d0] = outputVector.v[2];
        }     
    }
    
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

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_nativeIsAlphaMaskSupportedGlobal___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    return com_codename1_impl_ios_IOSNative_nativeIsAlphaMaskSupportedGlobal__(instanceObject);
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

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponentNSData___long__int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_INT onCompletionCallbackId) {
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

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudioRecorder___java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
                                                                                         JAVA_OBJECT  destinationFile) {
    return com_codename1_impl_ios_IOSNative_createAudioRecorder___java_lang_String(CN1_THREAD_STATE_PASS_ARG instanceObject, destinationFile);
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

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlGetColName___long_int_R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG statementPeer, JAVA_INT index) {
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

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUserAgentString___R_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getUserAgentString__(CN1_THREAD_STATE_PASS_ARG instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_connectSocket___java_lang_String_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT host, JAVA_INT port) {
    return com_codename1_impl_ios_IOSNative_connectSocket___java_lang_String_int(CN1_THREAD_STATE_PASS_ARG instanceObject, host, port);
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
JAVA_VOID com_codename1_impl_ios_IOSNative_printStackTraceToStream___java_lang_Throwable_java_io_Writer(JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
}
#endif


#ifndef NEW_CODENAME_ONE_VM
JAVA_VOID com_codename1_impl_ios_IOSNative_splitString___java_lang_String_char_java_util_ArrayList(JAVA_OBJECT instanceObject, JAVA_OBJECT string, JAVA_CHAR separator, JAVA_OBJECT outArr) {
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
    struct obj__java_lang_String* encString = (struct obj__java_lang_String*)string;
    JAVA_INT strlen = encString->java_lang_String_count;
    JAVA_INT offset = get_field_java_lang_String_offset(string);
    JAVA_ARRAY srcArr = (JAVA_ARRAY)get_field_java_lang_String_value(string);
    JAVA_ARRAY_CHAR* src = (JAVA_ARRAY_CHAR*)srcArr->data;
    JAVA_INT startPos = offset;
    JAVA_INT endOffset = offset + strlen;
    JAVA_INT i = startPos;
    for (; i < endOffset; i++) {
        if (src[i] == separator) {
            if (i > startPos) {
                JAVA_OBJECT str = __NEW_java_lang_String(CN1_THREAD_STATE_PASS_SINGLE_ARG);
                java_lang_String___INIT_____char_1ARRAY_int_int(CN1_THREAD_STATE_PASS_ARG str, (JAVA_OBJECT)srcArr, startPos, i - startPos);

                java_util_ArrayList_add___java_lang_Object_R_boolean(CN1_THREAD_STATE_PASS_ARG outArr, str);
            }
            startPos = i + 1;
        }
    }
    if (i > startPos) {
        JAVA_OBJECT str = __NEW_java_lang_String(CN1_THREAD_STATE_PASS_SINGLE_ARG);
        java_lang_String___INIT_____char_1ARRAY_int_int(CN1_THREAD_STATE_PASS_ARG str, (JAVA_OBJECT)srcArr, startPos, i - startPos);
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


JAVA_VOID com_codename1_impl_ios_IOSNative_sendLocalNotification___java_lang_String_java_lang_String_java_lang_String_java_lang_String_int_long_int( CN1_THREAD_STATE_MULTI_ARG
    JAVA_OBJECT me, JAVA_OBJECT notificationId, JAVA_OBJECT alertTitle, JAVA_OBJECT alertBody, JAVA_OBJECT alertSound, JAVA_INT badgeNumber, JAVA_LONG fireDate, JAVA_INT repeatType
                                                                                                                                                                     ) {
    
    UILocalNotification *notification = [[UILocalNotification alloc] init];
    NSString * msg = [NSString string];
    NSString *tmpStr;
    if (alertTitle != NULL) {
        tmpStr = [msg stringByAppendingString:toNSString(CN1_THREAD_STATE_PASS_ARG alertTitle)];
        
#ifndef CN1_USE_ARC
        [msg release];
#endif
        msg = tmpStr;
    }
    if (alertBody != NULL) {
        
        tmpStr = [msg stringByAppendingFormat:@"\n%@", toNSString(CN1_THREAD_STATE_PASS_ARG alertBody)];
#ifndef CN1_USE_ARC
        [msg release];
#endif
        msg = tmpStr;
    }
    tmpStr = [msg stringByReplacingOccurrencesOfString:@"%" withString:@"%%"];
#ifndef CN1_USE_ARC
    [msg release];
#endif
    msg = tmpStr;
    notification.alertBody = msg;

    notification.soundName= toNSString(CN1_THREAD_STATE_PASS_ARG alertSound);
    notification.fireDate = [NSDate dateWithTimeIntervalSince1970: fireDate/1000 + 1];
    notification.timeZone = [NSTimeZone defaultTimeZone];
    notification.applicationIconBadgeNumber = badgeNumber;
    switch (repeatType) {
        case 0:
            notification.repeatInterval = nil;
            break;
        case 1:
            notification.repeatInterval = NSMinuteCalendarUnit;
            break;
        case 3:
            notification.repeatInterval = NSHourCalendarUnit;
            break;
        case 4:
            notification.repeatInterval = NSDayCalendarUnit;
            break;
        case 5:
            notification.repeatInterval = NSWeekCalendarUnit;
            break;
        default:
            NSLog(@"Unknown repeat interval type %d.  Ignoring repeat interval", repeatType);
            notification.repeatInterval = nil;
    }
    
    NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
    [dict setObject: toNSString(CN1_THREAD_STATE_PASS_ARG notificationId) forKey: @"__ios_id__"];
    
    notification.userInfo = dict;
    
    
    dispatch_sync(dispatch_get_main_queue(), ^{
#ifdef __IPHONE_8_0
        if ([UIApplication instancesRespondToSelector:@selector(registerUserNotificationSettings:)]){
            [[UIApplication sharedApplication] registerUserNotificationSettings:[UIUserNotificationSettings settingsForTypes:UIUserNotificationTypeAlert|UIUserNotificationTypeBadge|UIUserNotificationTypeSound categories:nil]];
            //[[UIApplication sharedApplication] registerUserNotificationSettings:[UIUserNotificationSettings
            //                                             settingsForTypes:UIUserNotificationTypeAlert|UIUserNotificationTypeBadge|
            //                                           UIUserNotificationTypeSound categories:nil]];
        }
#endif
        
        [[UIApplication sharedApplication] scheduleLocalNotification: notification];
        
    });
}

JAVA_VOID com_codename1_impl_ios_IOSNative_cancelLocalNotification___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT notificationId) {
    if (notificationId == JAVA_NULL) {
        return;
    }
    NSString *nsId = toNSString(CN1_THREAD_STATE_PASS_ARG notificationId);
    dispatch_sync(dispatch_get_main_queue(), ^{
        UIApplication *app = [UIApplication sharedApplication];
        NSArray *eventArray = [app scheduledLocalNotifications];
        for (int i=0; i<[eventArray count]; i++) {
            UILocalNotification *n = [eventArray objectAtIndex:i];
            NSDictionary *userInfo = n.userInfo;
            NSString *uid = [NSString stringWithFormat:@"%@", [userInfo valueForKey: @"__ios_id__"]];
            if ([nsId isEqualToString:uid]) {
                [app cancelLocalNotification:n];
            }
        }
    });
}

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
        if (com_codename1_ui_plaf_Style_getBackgroundType___R_byte(threadStateData, s) ==get_static_com_codename1_ui_plaf_Style_BACKGROUND_GRADIENT_LINEAR_VERTICAL()) {
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
                        removeObjectFromHeapCollection(threadStateData, threePoints);
                        removeObjectFromHeapCollection(threadStateData, ((struct obj__java_lang_String*)threePoints)->java_lang_String_value);
                        ((struct obj__java_lang_String*)threePoints)->java_lang_String_value->__codenameOneReferenceCount = 999999;
                        threePoints->__codenameOneReferenceCount = 999999;
                        threePointsWidth = com_codename1_impl_ios_IOSImplementation_stringWidth___java_lang_Object_java_lang_String_R_int(threadStateData, __cn1ThisObject, nativeFont, threePoints);
                    }
                    
                    drawString(threadStateData, __cn1ThisObject, nativeGraphics, nativeFont, threePoints, shiftText + x, y, textDecoration, fontHeight);
                    
                    com_codename1_impl_ios_IOSImplementation_clipRect___java_lang_Object_int_int_int_int(threadStateData, __cn1ThisObject, nativeGraphics, threePointsWidth + shiftText + x, y, textSpaceW - threePointsWidth, fontHeight);
                }
                x = x - txtW + textSpaceW;
            } else if (endsWith3Points) {
                if(threePoints == JAVA_NULL) {
                    threePoints = newStringFromCString(threadStateData, "...");
                    removeObjectFromHeapCollection(threadStateData, threePoints);
                    removeObjectFromHeapCollection(threadStateData, ((struct obj__java_lang_String*)threePoints)->java_lang_String_value);
                    ((struct obj__java_lang_String*)threePoints)->java_lang_String_value->__codenameOneReferenceCount = 999999;
                    threePoints->__codenameOneReferenceCount = 999999;
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
    com_codename1_impl_ios_IOSImplementation_setColor___java_lang_Object_int(threadStateData, __cn1ThisObject, nativeGraphics, fgColor);
    
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
                                         + MAX(((icon != JAVA_NULL) ? iconWidth + gap : 0),
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
}
   
JAVA_LONG com_codename1_impl_ios_IOSNative_beginBackgroundTask__(JAVA_OBJECT instanceObject)
{
    __block UIBackgroundTaskIdentifier bgTask = UIBackgroundTaskInvalid;
    bgTask = [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:^{
        // Clean up any unfinished task business by marking where you
        // stopped or ending the task outright.
        [[UIApplication sharedApplication] endBackgroundTask:bgTask];
        bgTask = UIBackgroundTaskInvalid;
    }];
    return (JAVA_LONG)((BRIDGE_CAST void*)bgTask);
}

#ifdef NEW_CODENAME_ONE_VM
JAVA_LONG com_codename1_impl_ios_IOSNative_beginBackgroundTask___R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject)
{
    return com_codename1_impl_ios_IOSNative_beginBackgroundTask__(instanceObject);
}
#endif

JAVA_VOID com_codename1_impl_ios_IOSNative_endBackgroundTask___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG bgTask)
{
    [[UIApplication sharedApplication] endBackgroundTask:(UIBackgroundTaskIdentifier)bgTask];
}