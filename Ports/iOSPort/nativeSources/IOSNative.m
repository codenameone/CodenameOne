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
#include "xmlvm.h"
#include "java_lang_String.h"

#include "xmlvm-util.h"
#import <UIKit/UIKit.h>
#import "CodenameOne_GLViewController.h"
#import "NetworkConnectionImpl.h"
#include "com_codename1_impl_ios_IOSImplementation.h"
#include "com_codename1_ui_Display.h"
#include "com_codename1_ui_Component.h"
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

extern void initVMImpl();

extern void deinitVMImpl();

extern int Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl();

extern int Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl();

extern void Java_com_codename1_impl_ios_IOSImplementation_flushBufferImpl
(void* peer, int x, int y, int width, int height);

extern void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingMutableImpl
(int x, int y, int width, int height, int clipApplied);

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
(int color, int alpha, void* fontPeer, const char* str, int strLen, int x, int y);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringMutableImpl
(int color, int alpha, void* fontPeer, const char* str, int strLen, int x, int y);


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

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeFillArcGlobalImpl
(int color, int alpha, int x, int y, int width, int height, int startAngle, int angle);

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawArcGlobalImpl
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


extern void* Java_com_codename1_impl_ios_IOSImplementation_createSystemFontImpl
(int face, int style, int size);

extern void loadResourceFile(const char* name, int nameLen, const char* type, int typeLen, void* data);

extern int getResourceSize(const char* name, int nameLen, const char* type, int typeLen);

extern int isPainted();

extern void Java_com_codename1_impl_ios_IOSImplementation_imageRgbToIntArrayImpl
(void* peer, int* arr, int x, int y, int width, int height);

extern void* Java_com_codename1_impl_ios_IOSImplementation_createImageFromARGBImpl
(int* arr, int width, int height);

extern void Java_com_codename1_impl_ios_IOSImplementation_editStringAtImpl
(int x, int y, int w, int h, void* peer, int isSingleLine, int rows, int maxSize, int constraint, const char* str, int len);

extern void Java_com_codename1_impl_ios_IOSImplementation_resetAffineGlobal();

extern void Java_com_codename1_impl_ios_IOSImplementation_scale(float x, float y);

extern int isIPad();

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

    JAVA_INT len = byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
    char* cs = XMLVM_ATOMIC_MALLOC(len + 1);
    memcpy(cs, data, len);
    cs[len] = '\0';
    return cs;
}

void com_codename1_impl_ios_IOSNative_initVM__()
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_initVM__]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    int retVal = UIApplicationMain(0, nil, nil, @"CodenameOne_GLAppDelegate");
    [pool release];
    
    //UIWindow* wnd = [UIWindow alloc];
    //CodenameOne_GLViewController *cnt = [[CodenameOne_GLViewController alloc] init];
    
    //wnd.rootViewController = cnt;
    //[cnt awakeFromNib];
    //[wnd makeKeyAndVisible];
    //[CodenameOne_GLViewController initialize];
    //[[UIScreen mainScreen] 
    //[[CodenameOne_GLViewController instance] awakeFromNib];
    //[UIApplication sharedApplication].delegate.window 
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_deinitializeVM__()
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_deinitializeVM__]
    deinitVMImpl();
    //XMLVM_END_WRAPPER
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isPainted__()
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_isPainted__]
    return isPainted();
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplayWidth__()
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_getDisplayWidth__]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl();
    [pool release];
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplayHeight__()
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_getDisplayHeight__]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl();
    [pool release];
    return i;
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_editStringAt___int_int_int_int_long_boolean_int_int_int_java_lang_String(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_LONG n5, JAVA_BOOLEAN n6, JAVA_INT n7, JAVA_INT n8, JAVA_INT n9, JAVA_OBJECT n10)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_editStringAt___int_int_int_int_long_boolean_int_int_int_java_lang_String]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_editStringAtImpl(n1, n2, n3, n4, n5, n6, n7, n8, n9, stringToUTF8(n10), 0);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_flushBuffer___long_int_int_int_int(JAVA_LONG n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_flushBuffer___long_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_flushBufferImpl(n1, n2, n3, n4, n5);
    [pool release];
    //XMLVM_END_WRAPPER
}


void com_codename1_impl_ios_IOSNative_imageRgbToIntArray___long_int_1ARRAY_int_int_int_int(JAVA_LONG n1, JAVA_OBJECT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_imageRgbToIntArray___long_int_1ARRAY_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    org_xmlvm_runtime_XMLVMArray* intArray = n2;
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    Java_com_codename1_impl_ios_IOSImplementation_imageRgbToIntArrayImpl(n1, data, n3, n4, n5, n6);
    [pool release];
    //XMLVM_END_WRAPPER
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageFromARGB___int_1ARRAY_int_int(JAVA_OBJECT n1, JAVA_INT n2, JAVA_INT n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_createImageFromARGB___int_1ARRAY_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    org_xmlvm_runtime_XMLVMArray* intArray = n1;
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_createImageFromARGBImpl(data, n2, n3);
    [pool release];
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImage___byte_1ARRAY_int_1ARRAY(JAVA_OBJECT n1, JAVA_OBJECT n2)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_createImage___byte_1ARRAY_int_1ARRAY]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    org_xmlvm_runtime_XMLVMArray* byteArray = n1;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    org_xmlvm_runtime_XMLVMArray* intArray = n2;
    JAVA_ARRAY_INT* data2 = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_createImageImpl(data, byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_, data2);
    [pool release];
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_LONG com_codename1_impl_ios_IOSNative_scale___long_int_int(JAVA_LONG n1, JAVA_INT n2, JAVA_INT n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_scale___long_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_scaleImpl(n1, n2, n3);
    [pool release];
    return i;
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_setNativeClippingMutable___int_int_int_int_boolean(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_BOOLEAN n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_setNativeClippingMutable___int_int_int_int_boolean]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingMutableImpl(n1, n2, n3, n4, n5);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_setNativeClippingGlobal___int_int_int_int_boolean(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_BOOLEAN n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_setNativeClippingGlobal___int_int_int_int_boolean]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingGlobalImpl(n1, n2, n3, n4, n5);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawLineMutable___int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawLineMutable___int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineMutableImpl(n1, n2, n3, n4, n5, n6);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawLineGlobal___int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawLineGlobal___int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineGlobalImpl(n1, n2, n3, n4, n5, n6);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillRectMutable___int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillRectMutable___int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectMutableImpl(n1, n2, n3, n4, n5, n6);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillRectGlobal___int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillRectGlobal___int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectGlobalImpl(n1, n2, n3, n4, n5, n6);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawRectMutable___int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawRectMutable___int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectMutableImpl(n1, n2, n3, n4, n5, n6);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawRectGlobal___int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawRectGlobal___int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectGlobalImpl(n1, n2, n3, n4, n5, n6);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawRoundRectMutable___int_int_int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawRoundRectMutable___int_int_int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRoundRectMutableImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawRoundRectGlobal___int_int_int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawRoundRectGlobal___int_int_int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRoundRectGlobalImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillRoundRectMutable___int_int_int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillRoundRectMutable___int_int_int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillRoundRectMutableImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillRoundRectGlobal___int_int_int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillRoundRectGlobal___int_int_int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillRoundRectGlobalImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillArcMutable___int_int_int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillArcMutable___int_int_int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillArcMutableImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawArcMutable___int_int_int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawArcMutable___int_int_int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawArcMutableImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillArcGlobal___int_int_int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillArcGlobal___int_int_int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillArcGlobalImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawArcGlobal___int_int_int_int_int_int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawArcGlobal___int_int_int_int_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawArcGlobalImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawStringMutable___int_int_long_java_lang_String_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_LONG n3, JAVA_OBJECT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawStringMutable___int_int_long_java_lang_String_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    const char* chr = stringToUTF8(n4);
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringMutableImpl(n1, n2, n3, chr, strlen(chr), n5, n6);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawStringGlobal___int_int_long_java_lang_String_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_LONG n3, JAVA_OBJECT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawStringGlobal___int_int_long_java_lang_String_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    const char* chr = stringToUTF8(n4);
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringGlobalImpl(n1, n2, n3, chr, strlen(chr), n5, n6);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawImageMutable___long_int_int_int_int_int(JAVA_LONG n1, JAVA_INT alpha, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawImageMutable___long_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageMutableImpl(n1, alpha, n2, n3, n4, n5);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawImageGlobal___long_int_int_int_int_int(JAVA_LONG n1, JAVA_INT alpha, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawImageGlobal___long_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl(n1, alpha, n2, n3, n4, n5);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeTileImageGlobal___long_int_int_int_int_int(JAVA_LONG n1, JAVA_INT alpha, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeTileImageGlobal___long_int_int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_nativeTileImageGlobalImpl(n1, alpha, n2, n3, n4, n5);
    [pool release];
    //XMLVM_END_WRAPPER
}


JAVA_INT com_codename1_impl_ios_IOSNative_stringWidthNative___long_java_lang_String(JAVA_LONG n1, JAVA_OBJECT n2)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_stringWidthNative___long_java_lang_String]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    const char* chr = stringToUTF8(n2);
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_stringWidthNativeImpl(n1, chr, strlen(chr));
    [pool release];
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_charWidthNative___long_char(JAVA_LONG n1, JAVA_CHAR n2)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_charWidthNative___long_char]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_charWidthNativeImpl(n1, n2);
    [pool release];
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_getFontHeightNative___long(JAVA_LONG n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_getFontHeightNative___long]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_getFontHeightNativeImpl(n1);
    [pool release];
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createSystemFont___int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_createSystemFont___int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_createSystemFontImpl(n1, n2, n3);
    [pool release];
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResourceSize___java_lang_String_java_lang_String(JAVA_OBJECT n1, JAVA_OBJECT n2)
{
    //XMLVM_BEGIN_NATIVE[com_codename1_impl_ios_IOSNative_getResourceSize___java_lang_String_java_lang_String]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    JAVA_INT i = getResourceSize(stringToUTF8(n1), 0, stringToUTF8(n2), 0);
    [pool release];
    return i;
    //XMLVM_END_NATIVE
}

void com_codename1_impl_ios_IOSNative_loadResource___java_lang_String_java_lang_String_byte_1ARRAY(JAVA_OBJECT n1, JAVA_OBJECT n2, JAVA_OBJECT n3)
{
    //XMLVM_BEGIN_NATIVE[com_codename1_impl_ios_IOSNative_loadResource___java_lang_String_java_lang_String_byte_1ARRAY]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    org_xmlvm_runtime_XMLVMArray* byteArray = n3;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    loadResourceFile(stringToUTF8(n1), 0, stringToUTF8(n2), 0, data);
    [pool release];
    //XMLVM_END_NATIVE
}


JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeMutableImage___int_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_createNativeMutableImage___int_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_createNativeMutableImageImpl(n1, n2, n3);
    [pool release];
    return i;
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_startDrawingOnImage___int_int_long(JAVA_INT n1, JAVA_INT n2, JAVA_LONG n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_startDrawingOnImage___int_int_long]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_startDrawingOnImageImpl(n1, n2, n3);
    [pool release];
    //XMLVM_END_WRAPPER
}

JAVA_LONG com_codename1_impl_ios_IOSNative_finishDrawingOnImage__()
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_finishDrawingOnImage__]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl();
    [pool release];
    return i;
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_deleteNativePeer___long(JAVA_LONG n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_deleteNativePeer___long]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_deleteNativePeerImpl(n1);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_deleteNativeFontPeer___long(JAVA_LONG n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_deleteNativePeer___long]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_deleteNativeFontPeerImpl(n1);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_resetAffineGlobal__()
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_resetAffineGlobal();
    [pool release];
}

void com_codename1_impl_ios_IOSNative_scaleGlobal___float_float(JAVA_FLOAT x, JAVA_FLOAT y) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Java_com_codename1_impl_ios_IOSImplementation_scale(x, y);
    [pool release];
}

void com_codename1_impl_ios_IOSNative_rotateGlobal___float(JAVA_FLOAT angle) {
}

void com_codename1_impl_ios_IOSNative_rotateGlobal___float_int_int(JAVA_FLOAT angle, JAVA_INT x, JAVA_INT y) {
    
}

void com_codename1_impl_ios_IOSNative_shearGlobal___float_float(JAVA_FLOAT x, JAVA_FLOAT y) {
    
}


void pointerPressed(int* x, int* y, int length) {
    com_codename1_impl_ios_IOSImplementation_pointerPressedCallback___int_int(x[0], y[0]);
}

void pointerDragged(int* x, int* y, int length) {
    com_codename1_impl_ios_IOSImplementation_pointerDraggedCallback___int_int(x[0], y[0]);
}

void pointerReleased(int* x, int* y, int length) {
    com_codename1_impl_ios_IOSImplementation_pointerReleasedCallback___int_int(x[0], y[0]);
}

void screenSizeChanged(int width, int height) {
    com_codename1_impl_ios_IOSImplementation_sizeChangedImpl___int_int(width, height);
}

void stringEdit(int finished, int cursorPos, NSString* text) {
    com_codename1_impl_ios_IOSImplementation_editingUpdate___java_lang_String_int_boolean(
                                                                                          fromNSString(text), cursorPos, finished != 0
                                                                                          );
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isTablet__()
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_isPainted__]
    return isIPad();
    //XMLVM_END_WRAPPER
}

NSString* toNSString(JAVA_OBJECT str) {
    if(str == 0) {
        return 0;
    }
    const char* chrs = stringToUTF8(str);
    return [NSString stringWithUTF8String:chrs];
}

JAVA_INT com_codename1_impl_ios_IOSNative_writeToFile___byte_1ARRAY_java_lang_String(JAVA_OBJECT n1, JAVA_OBJECT path) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    const char* chrs = stringToUTF8(path);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    org_xmlvm_runtime_XMLVMArray* byteArray = n1;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;    
    NSData* d = [NSData dataWithBytes:data length:byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_];
    [d writeToFile:ns atomically:YES];
    [pool release];
    return 0;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getFileSize___java_lang_String(JAVA_OBJECT path) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    const char* chrs = stringToUTF8(path);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    NSFileManager *man = [[NSFileManager alloc] init];
    NSError *error = nil;
    NSDictionary *attrs = [man attributesOfItemAtPath:ns error:&error];
    if(error != nil) {  
        NSLog(@"Error getFileSize: %@", [error localizedDescription]);        
    }
    UInt32 result = [attrs fileSize];
    [man release];
    [pool release];
    return result;
}

void com_codename1_impl_ios_IOSNative_readFile___java_lang_String_byte_1ARRAY(JAVA_OBJECT path, JAVA_OBJECT n1) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    const char* chrs = stringToUTF8(path);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    NSData* d = [NSData dataWithContentsOfFile:ns];
    org_xmlvm_runtime_XMLVMArray* byteArray = n1;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;    
    memcpy(data, d.bytes, d.length);
    [pool release];
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getDocumentsDir__() {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];    
    java_lang_String* str = xmlvm_create_java_string(documentsPath.UTF8String);
    [pool release];
    return str;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getCachesDir__() {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];    
    java_lang_String* str = xmlvm_create_java_string(documentsPath.UTF8String);
    [pool release];
    return str;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResourcesDir__() {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSBundle *bundle = [NSBundle mainBundle];
    NSString *bundlePath = [bundle bundlePath];
    java_lang_String* str = xmlvm_create_java_string(bundlePath.UTF8String);
    [pool release];
    return str;
}

void com_codename1_impl_ios_IOSNative_deleteFile___java_lang_String(JAVA_OBJECT file) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(file);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    NSError *error = nil;
    [fm removeItemAtPath:ns error:&error];
    if(error != nil) {  
        NSLog(@"Error in deleteFile: %@", [error localizedDescription]);        
    }
    [fm release];
    [pool release];
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_fileExists___java_lang_String(JAVA_OBJECT file) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(file);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    JAVA_BOOLEAN b = [fm fileExistsAtPath:ns];
    [fm release];
    [pool release];
    return b;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDirectory___java_lang_String(JAVA_OBJECT file) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(file);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    BOOL b = NO;
    BOOL* isDir = (&b);
    [fm fileExistsAtPath:ns isDirectory:isDir];
    [fm release];
    [pool release];
    return b;
}

JAVA_INT com_codename1_impl_ios_IOSNative_fileCountInDir___java_lang_String(JAVA_OBJECT dir) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(dir);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    NSError *error = nil;
    NSArray* nsArr = [fm contentsOfDirectoryAtPath:ns error:&error];
    if(error != nil) {  
        NSLog(@"Error in recording: %@", [error localizedDescription]);        
    }
    int i = nsArr.count;
    [fm release];
    [pool release];
    return i;   
}

void com_codename1_impl_ios_IOSNative_listFilesInDir___java_lang_String_java_lang_String_1ARRAY(JAVA_OBJECT dir, JAVA_OBJECT files) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(dir);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    NSError *error = nil;
    NSArray* nsArr = [fm contentsOfDirectoryAtPath:ns error:&error];
    if(error != nil) {  
        NSLog(@"Error in listing files: %@", [error localizedDescription]);        
    }
    
    org_xmlvm_runtime_XMLVMArray* byteArray = files;
    JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;    
    
    int count = nsArr.count;
    for(int iter = 0 ; iter < count ; iter++) {
        NSString* currentString = [nsArr objectAtIndex:iter];
        java_lang_String* str = xmlvm_create_java_string(currentString.UTF8String);
        data[iter] = str;
    }
    [fm release];
    [pool release];
}

void com_codename1_impl_ios_IOSNative_createDirectory___java_lang_String(JAVA_OBJECT dir) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(dir);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    [fm currentDirectoryPath:ns];
    [fm release];
    [pool release];
}

void com_codename1_impl_ios_IOSNative_moveFile___java_lang_String_java_lang_String(JAVA_OBJECT src, JAVA_OBJECT dest) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(src);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    const char* chrsDest = stringToUTF8(dest);
    NSString* nsDst = [NSString stringWithUTF8String:chrsDest];
    NSError *error = nil;
    [fm moveItemAtPath:nsSrc toPath:nsDst error:&error];
    if(error != nil) {  
        NSLog(@"Error in moving file: %@", [error localizedDescription]);        
    }
    [fm release];
    [pool release];    
}

extern void Java_com_codename1_impl_ios_IOSImplementation_setImageName(void* nativeImage, const char* name);


void com_codename1_impl_ios_IOSNative_setImageName___long_java_lang_String(JAVA_LONG nativeImage, JAVA_OBJECT name) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    const char* chrs = stringToUTF8(name);
    Java_com_codename1_impl_ios_IOSImplementation_setImageName(nativeImage, chrs);
    [pool release];    
}

JAVA_LONG com_codename1_impl_ios_IOSNative_openConnection___java_lang_String_int(JAVA_OBJECT url, JAVA_INT timeout) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NetworkConnectionImpl* impl = [[NetworkConnectionImpl alloc] init];
    const char* chrs = stringToUTF8(url);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    void* response = [impl openConnection:nsSrc timeout:timeout];
    [pool release];    
    return response;
}

void com_codename1_impl_ios_IOSNative_connect___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    [impl connect];
    [pool release];    
}

void com_codename1_impl_ios_IOSNative_setMethod___long_java_lang_String(JAVA_LONG peer, JAVA_OBJECT mtd) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    const char* chrs = stringToUTF8(mtd);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    [impl setMethod:nsSrc];
    [pool release];    
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResponseCode___long(JAVA_LONG peer) {
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    return [impl getResponseCode];
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseMessage___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    java_lang_String* str = xmlvm_create_java_string([impl getResponseMessage].UTF8String);
    [pool release];    
    return str;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getContentLength___long(JAVA_LONG peer) {
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    return [impl getContentLength];
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseHeader___long_java_lang_String(JAVA_LONG peer, JAVA_OBJECT name) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    const char* chrs = stringToUTF8(name);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    java_lang_String* str = fromNSString([impl getResponseHeader:nsSrc]);
    [pool release];    
    return str;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResponseHeaderCount___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    JAVA_INT i = [impl getResponseHeaderCount]; 
    [pool release];    
    return i;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseHeaderName___long_int(JAVA_LONG peer, JAVA_INT offset) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    JAVA_OBJECT j = fromNSString([impl getResponseHeaderName:offset]);
    [pool release];    
    return j;
}

void com_codename1_impl_ios_IOSNative_addHeader___long_java_lang_String_java_lang_String(JAVA_LONG peer, JAVA_OBJECT key, JAVA_OBJECT value) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    const char* chrs = stringToUTF8(key);
    NSString* nsKey = [NSString stringWithUTF8String:chrs];
    chrs = stringToUTF8(value);
    NSString* nsValue = [NSString stringWithUTF8String:chrs];
    [impl addHeader:nsKey value:nsValue];
    [pool release];    
}

void com_codename1_impl_ios_IOSNative_setBody___long_byte_1ARRAY(JAVA_LONG peer, JAVA_OBJECT arr) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    org_xmlvm_runtime_XMLVMArray* byteArray = arr;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    [impl setBody:data size:byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_];
    [pool release];    
}

void connectionComplete(void* peer) {
    com_codename1_impl_ios_IOSImplementation_streamComplete___long(peer);
}

void connectionReceivedData(void* peer, NSData* data) {
    if (!__TIB_byte.classInitialized) __INIT_byte();
    org_xmlvm_runtime_XMLVMArray* byteArray = XMLVMArray_createSingleDimension(__CLASS_byte, [data length]);
    [data getBytes:byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_];
    com_codename1_impl_ios_IOSImplementation_appendData___long_byte_1ARRAY(peer, byteArray);
}

void connectionError(void* peer, NSString* message) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    java_lang_String* str = xmlvm_create_java_string(message.UTF8String);
    com_codename1_impl_ios_IOSImplementation_networkError___long_java_lang_String(peer, str);
    [pool release];    
}


void com_codename1_impl_ios_IOSNative_closeConnection___long(JAVA_LONG peer) {
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    [impl release];
}

void com_codename1_impl_ios_IOSNative_execute___java_lang_String(JAVA_OBJECT n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_execute___java_lang_String]
    const char* chrs = stringToUTF8(n1);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:ns]];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_flashBacklight___int(JAVA_INT n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_flashBacklight___int]
    //XMLVM_END_WRAPPER
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isMinimized__()
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_isMinimized__]
    return false;
    //XMLVM_END_WRAPPER
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_minimizeApplication__()
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_minimizeApplication__]
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_restoreMinimizedApplication__()
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_restoreMinimizedApplication__]
    //XMLVM_END_WRAPPER
}

extern int orientationLock;
void com_codename1_impl_ios_IOSNative_lockOrientation___boolean(JAVA_BOOLEAN n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_lockOrientation___boolean]
    if(n1) {
        orientationLock = 1;
    } else {
        orientationLock = 2;
    }
    //XMLVM_END_WRAPPER
}


extern void vibrateDevice();
void com_codename1_impl_ios_IOSNative_vibrate___int(JAVA_INT duration) {
    vibrateDevice();
}

// Peer Component methods

void com_codename1_impl_ios_IOSNative_calcPreferredSize___long_int_int_int_1ARRAY(JAVA_LONG peer, JAVA_INT w, JAVA_INT h, JAVA_OBJECT response) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIView* v = (UIView*)peer;
        CGSize s = [v sizeThatFits:CGSizeMake(w, h)];
        org_xmlvm_runtime_XMLVMArray* intArray = response;
        JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
        data[0] = (JAVA_INT)s.width;
        data[1] = (JAVA_INT)s.height;
        [pool release];
    });
}

void com_codename1_impl_ios_IOSNative_updatePeerPositionSize___long_int_int_int_int(JAVA_LONG peer, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIView* v = (UIView*)peer;
        float scale = [UIScreen mainScreen].scale;
        float xpos = x / scale;
        float ypos = y / scale;
        float wpos = w / scale;
        float hpos = h / scale;
        [v setFrame:CGRectMake(xpos, ypos, wpos, hpos)];
        [v setNeedsDisplay]; 
        [pool release];    
    });
}

void com_codename1_impl_ios_IOSNative_peerSetVisible___long_boolean(JAVA_LONG peer, JAVA_BOOLEAN b) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIView* v = (UIView*)peer;
        if(!b) {
            if([v superview] != nil) {
                [v removeFromSuperview];
            }
        } else {
            if([v superview] == nil) {
                [[CodenameOne_GLViewController instance].view addSubview:v];
            }
        }
        [pool release];    
    });
}

void com_codename1_impl_ios_IOSNative_peerInitialized___long_int_int_int_int(JAVA_LONG peer, int x, int y, int w, int h) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIView* v = (UIView*)peer;
        if([v superview] == nil) {
            [[CodenameOne_GLViewController instance].view addSubview:v];
        }
        if(w > 0 && h > 0) {
            float scale = [UIScreen mainScreen].scale;
            float xpos = x / scale;
            float ypos = y / scale;
            float wpos = w / scale;
            float hpos = h / scale;
            [v setFrame:CGRectMake(xpos, ypos, wpos, hpos)];
            [v setNeedsDisplay]; 
        } else {
            [v setFrame:CGRectMake(3000, 0, 300, 300)];
        }
        [pool release];    
    });
}

void repaintUI() {
    com_codename1_ui_Display* d = (com_codename1_ui_Display*) com_codename1_ui_Display_getInstance__();
    com_codename1_ui_Form* f = (com_codename1_ui_Form*)com_codename1_ui_Display_getCurrent__(d);
    com_codename1_ui_Component_repaint__(f);
}

void com_codename1_impl_ios_IOSNative_peerDeinitialized___long(JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIView* v = (UIView*)peer;
        if(v.superview != nil) {
            [v removeFromSuperview];
            repaintUI();
        } 
        [pool release];    
    });
}

JAVA_INT com_codename1_impl_ios_IOSNative_getAudioDuration___long(JAVA_LONG peer) {
    __block JAVA_INT dur = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        AudioPlayer* pl = (AudioPlayer*)peer;
        dur = [pl getAudioDuration];
        [pool release];
    });
    return dur;
}

void com_codename1_impl_ios_IOSNative_playAudio___long(JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        AudioPlayer* pl = (AudioPlayer*)peer;
        [pl playAudio];
        [pool release];
    });
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAudioPlaying___long(JAVA_LONG peer) {
    __block JAVA_BOOLEAN result = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        AudioPlayer* pl = (AudioPlayer*)peer;
        result = [pl isPlaying];
        [pool release];
    });
    return result;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getAudioTime___long(JAVA_LONG peer) {
    __block JAVA_INT dur = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        AudioPlayer* pl = (AudioPlayer*)peer;
        dur = [pl getAudioTime];
        [pool release];
    });
    return dur;
}

void com_codename1_impl_ios_IOSNative_pauseAudio___long(JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        AudioPlayer* pl = (AudioPlayer*)peer;
        [pl pauseAudio];
        [pool release];
    });
}

void com_codename1_impl_ios_IOSNative_setAudioTime___long_int(JAVA_LONG peer, JAVA_INT time) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        AudioPlayer* pl = (AudioPlayer*)peer;
        [pl setAudioTime:time];
        [pool release];
    });
}

void com_codename1_impl_ios_IOSNative_cleanupAudio___long(JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        AudioPlayer* pl = (AudioPlayer*)peer;
        [pl release];
        [pool release];
    });
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___java_lang_String_java_lang_Runnable(JAVA_OBJECT uri, JAVA_OBJECT onCompletion) {
    __block JAVA_LONG p = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        const char* chrs = stringToUTF8(uri);
        NSString* ns = [NSString stringWithUTF8String:chrs];
        p = [[AudioPlayer alloc] initWithURL:uri callback:onCompletion];
        [pool release];
    });
    return p;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___byte_1ARRAY_java_lang_Runnable(JAVA_OBJECT b, JAVA_OBJECT onCompletion) {
    __block JAVA_LONG p = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        org_xmlvm_runtime_XMLVMArray* byteArray = b;
        JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;    
        NSData* d = [NSData dataWithBytes:data length:byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_];
        p = [[AudioPlayer alloc] initWithNSData:d callback:onCompletion];
        [pool release];
    });
    return p;
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_getVolume__() {
    return [AudioPlayer getVolume];    
}

void com_codename1_impl_ios_IOSNative_setVolume___float(JAVA_FLOAT vol) {
    [AudioPlayer setVolume:vol];    
}

void com_codename1_impl_ios_IOSNative_fillRectRadialGradientGlobal___int_int_int_int_int_int_float_float_float(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_FLOAT n7, JAVA_FLOAT n8, JAVA_FLOAT n9) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    DrawGradient* d = [[DrawGradient alloc] initWithArgs:1 startColorA:n1 endColorA:n2 xA:n3 yA:n4 widthA:n5 heightA:n6 relativeXA:n7 relativeYA:n8 relativeSizeA:n9];
    [CodenameOne_GLViewController upcoming:d];
    [d release];
    [pool release];
}

void com_codename1_impl_ios_IOSNative_fillLinearGradientGlobal___int_int_int_int_int_int_boolean(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_BOOLEAN n7) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    int horizontal = 2;
    if(n7) {
        horizontal = 3;
    }
    DrawGradient* d = [[DrawGradient alloc] initWithArgs:horizontal startColorA:n1 endColorA:n2 xA:n3 yA:n4 widthA:n5 heightA:n6 relativeXA:0 relativeYA:0 relativeSizeA:0];
    [CodenameOne_GLViewController upcoming:d];
    [d release];
    [pool release];
}

void com_codename1_impl_ios_IOSNative_fillRectRadialGradientMutable___int_int_int_int_int_int_float_float_float(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT width, JAVA_INT height, JAVA_FLOAT relativeX, JAVA_FLOAT relativeY, JAVA_FLOAT relativeSize) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    
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
    [pool release];
}

void com_codename1_impl_ios_IOSNative_fillLinearGradientMutable___int_int_int_int_int_int_boolean(JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT width, JAVA_INT height, JAVA_BOOLEAN n7) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
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
    
    if(n7) {
        CGContextDrawLinearGradient(UIGraphicsGetCurrentContext(), myGradient, 
                                    CGPointMake(0, 0), CGPointMake(0, width), kCGGradientDrawsBeforeStartLocation | kCGGradientDrawsAfterEndLocation);
    } else {
        CGContextDrawLinearGradient(UIGraphicsGetCurrentContext(), myGradient, 
                                    CGPointMake(0, 0), CGPointMake(height, 0), kCGGradientDrawsBeforeStartLocation | kCGGradientDrawsAfterEndLocation);                        
    }
    CGColorSpaceRelease(colorSpace);
    [pool release];
}

void com_codename1_impl_ios_IOSNative_releasePeer___long(JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSObject* o = (NSObject*)peer;
        [o release];
    });
}

void com_codename1_impl_ios_IOSNative_retainPeer___long(JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSObject* o = (NSObject*)peer;
        [o retain];
    });
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createBrowserComponent___java_lang_Object(JAVA_OBJECT obj) {
    __block UIWebView* response = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        response = [[UIWebView alloc] initWithFrame:CGRectMake(3000, 0, 200, 200)];
        response.backgroundColor = [UIColor whiteColor];
        response.autoresizesSubviews = YES;
        UIWebViewEventDelegate *del = [[UIWebViewEventDelegate alloc] initWithCallback:obj];
        response.delegate = del;
        response.autoresizingMask=(UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth);
        [response setAllowsInlineMediaPlayback:YES];
        [response retain];
    });
    return response;
}

void com_codename1_impl_ios_IOSNative_setBrowserPage___long_java_lang_String_java_lang_String(JAVA_LONG peer, JAVA_OBJECT html, JAVA_OBJECT baseUrl) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIWebView* w = (UIWebView*)peer;
        //[w loadHTMLString:toNSString(html) baseURL:toNSString(baseUrl)];
        
        // passing anything other than nil crashes the app, no idea why???
        [w loadHTMLString:toNSString(html) baseURL:nil];
        [pool release];
    });
}

void com_codename1_impl_ios_IOSNative_setBrowserURL___long_java_lang_String(JAVA_LONG peer, JAVA_OBJECT url) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIWebView* w = (UIWebView*)peer;
        NSString *str = toNSString(url);
        NSURL* nu = [NSURL URLWithString:str];
        NSURLRequest* r = [NSURLRequest requestWithURL:nu];
        [w loadRequest:r];
        [pool release];
    });
}

void com_codename1_impl_ios_IOSNative_browserBack___long(JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIWebView* w = (UIWebView*)peer;
        [w goBack];
        [pool release];
    });
}

void com_codename1_impl_ios_IOSNative_browserStop___long(JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIWebView* w = (UIWebView*)peer;
        [w stopLoading];
        [pool release];
    });
}

void com_codename1_impl_ios_IOSNative_browserClearHistory___long(JAVA_LONG peer) {
}

void com_codename1_impl_ios_IOSNative_browserExecute___long_java_lang_String(JAVA_LONG peer, JAVA_OBJECT javaScript) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIWebView* w = (UIWebView*)peer;
        [w stringByEvaluatingJavaScriptFromString:toNSString(javaScript)];
        [pool release];
    });
}

void com_codename1_impl_ios_IOSNative_browserForward___long(JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIWebView* w = (UIWebView*)peer;
        [w goForward];
        [pool release];
    });
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_browserHasBack___long(JAVA_LONG peer) {
    __block JAVA_BOOLEAN booleanResponse = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIWebView* w = (UIWebView*)peer;
        booleanResponse = [w canGoBack];
        [pool release];
    });
    return booleanResponse;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_browserHasForward___long(JAVA_LONG peer) {
    __block JAVA_BOOLEAN booleanResponse = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIWebView* w = (UIWebView*)peer;
        booleanResponse = [w canGoForward];
        [pool release];
    });
    return booleanResponse;
}

void com_codename1_impl_ios_IOSNative_browserReload___long(JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIWebView* w = (UIWebView*)peer;
        [w reload];
        [pool release];
    });
}

java_lang_String* returnString;
JAVA_OBJECT com_codename1_impl_ios_IOSNative_getBrowserTitle___long(JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIWebView* w = (UIWebView*)peer;
        NSString* theTitle = [w stringByEvaluatingJavaScriptFromString:@"document.title"];
        returnString = xmlvm_create_java_string(theTitle.UTF8String);
        [w reload];
        [pool release];
    });
    return returnString;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getBrowserURL___long(JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIWebView* w = (UIWebView*)peer;
        returnString = xmlvm_create_java_string(w.request.URL.absoluteString.UTF8String);
        [w reload];
        [pool release];
    });
    return returnString;
}


JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponent___java_lang_String(JAVA_OBJECT str) {
    __block MPMoviePlayerController* moviePlayerInstance;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        NSURL* u = [NSURL URLWithString:toNSString(str)];
        moviePlayerInstance = [[MPMoviePlayerController alloc] initWithContentURL:u];
        [pool release];
    });
    return moviePlayerInstance;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponent___byte_1ARRAY(JAVA_OBJECT dataObject) {
    __block MPMoviePlayerController* moviePlayerInstance;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        org_xmlvm_runtime_XMLVMArray* byteArray = dataObject;
        JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;    
        NSData* d = [NSData dataWithBytes:data length:byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_];
        
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *documentsDirectory = [paths objectAtIndex:0];
        NSString *path = [documentsDirectory stringByAppendingPathComponent:@"temp_movie.mp4"];
        
        [d writeToFile:path atomically:YES];
        NSURL *u = [NSURL fileURLWithPath:path];        
        
        moviePlayerInstance = [[MPMoviePlayerController alloc] initWithContentURL:u];
        [pool release];
    });
    return moviePlayerInstance;
}



void com_codename1_impl_ios_IOSNative_sendEmailMessage___java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String(
    JAVA_OBJECT  recipients, JAVA_OBJECT  subject, JAVA_OBJECT content, JAVA_OBJECT attachment, JAVA_OBJECT attachmentMimeType) {
    dispatch_async(dispatch_get_main_queue(), ^{
        MFMailComposeViewController *picker = [[MFMailComposeViewController alloc] init];
        picker.mailComposeDelegate = [CodenameOne_GLViewController instance];
        
        // Recipient.
        NSString *recipient = toNSString(recipients);
        NSArray *recipientsArray = [NSArray arrayWithObject:recipient];
        [picker setToRecipients:recipientsArray];
        
        // Subject.
        [picker setSubject:toNSString(subject)];
        
        // Body.
        NSString *emailBody = toNSString(content);
        [picker setMessageBody:emailBody isHTML:NO];
        
        [[CodenameOne_GLViewController instance] presentModalViewController:picker animated:YES];
        
        [picker release];
    });
}

void com_codename1_impl_ios_IOSNative_startVideoComponent___long(JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        MPMoviePlayerController* m = (MPMoviePlayerController*) peer;
        [m play];
        [pool release];
    });
}

void com_codename1_impl_ios_IOSNative_stopVideoComponent___long(JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        MPMoviePlayerController* m = (MPMoviePlayerController*) peer;
        [m stop];
        [pool release];
    });
}

JAVA_INT com_codename1_impl_ios_IOSNative_getMediaTimeMS___long(JAVA_LONG peer) {
    // unsupported by API for some reason???
    return 0;
}

JAVA_INT com_codename1_impl_ios_IOSNative_setMediaTimeMS___long_int(JAVA_LONG peer, JAVA_INT time) {
    // unsupported by API for some reason???
    return 0;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getMediaDuration___long(JAVA_LONG peer) {
    __block int response = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        MPMoviePlayerController* m = (MPMoviePlayerController*) peer;
        response = (int)m.duration * 1000;
        [pool release];
    });
    return response;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoPlaying___long(JAVA_LONG peer) {
    __block JAVA_BOOLEAN response = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        MPMoviePlayerController* m = (MPMoviePlayerController*) peer;
        response = m.playbackState == MPMoviePlaybackStatePlaying;
        [pool release];
    });
    return response;
}

void com_codename1_impl_ios_IOSNative_setVideoFullScreen___long_boolean(JAVA_LONG peer, JAVA_BOOLEAN fullscreen) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        MPMoviePlayerController* m = (MPMoviePlayerController*) peer;
        [m setFullscreen:fullscreen];
        [pool release];
    });
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoFullScreen___long(JAVA_LONG peer) {
    __block JAVA_BOOLEAN response = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        MPMoviePlayerController* m = (MPMoviePlayerController*) peer;
        response = [m isFullscreen];
        [pool release];
    });
    return response;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getVideoViewPeer___long(JAVA_LONG peer) {
    MPMoviePlayerController* m = (MPMoviePlayerController*) peer;
    return m.view;
}

void com_codename1_impl_ios_IOSNative_showNativePlayerController___long(JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        MPMoviePlayerController* m = (MPMoviePlayerController*) peer;
        [[CodenameOne_GLViewController instance] presentModalViewController:m animated:YES];
        [pool release];
    });
}


JAVA_LONG com_codename1_impl_ios_IOSNative_createCLLocation__() {
    __block CLLocationManager* loc = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        loc = [[CLLocationManager alloc] init];
    });
    return loc;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getCurrentLocationObject___long(JAVA_LONG peer) {
    __block CLLocation* loc;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        CLLocationManager* l = (CLLocationManager*)peer;
        loc = l.location;
        [loc retain];
        [pool release];
    });
    return loc;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationLatitude___long(JAVA_LONG peer) {
    CLLocation* loc = (CLLocation*)peer;
    return loc.coordinate.latitude;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationAltitude___long(JAVA_LONG peer) {
    CLLocation* loc = (CLLocation*)peer;
    return loc.altitude;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationLongtitude___long(JAVA_LONG peer) {
    CLLocation* loc = (CLLocation*)peer;
    return loc.coordinate.longitude;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationAccuracy___long(JAVA_LONG peer) {
    CLLocation* loc = (CLLocation*)peer;
    return loc.horizontalAccuracy;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationDirection___long(JAVA_LONG peer) {
    CLLocation* loc = (CLLocation*)peer;
    return loc.course;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationVelocity___long(JAVA_LONG peer) {
    CLLocation* loc = (CLLocation*)peer;
    return loc.speed;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getLocationTimeStamp___long(JAVA_LONG peer) {
    CLLocation* loc = (CLLocation*)peer;
    NSTimeInterval t = [loc.timestamp timeIntervalSince1970];
    return (JAVA_LONG)(t * 1000.0);
}

UIPopoverController* popoverController;
void com_codename1_impl_ios_IOSNative_captureCamera___boolean(JAVA_BOOLEAN movie) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        UIImagePickerControllerSourceType sourceType = UIImagePickerControllerSourceTypeCamera; // default
        popoverController = nil;
        
        bool hasCamera = [UIImagePickerController isSourceTypeAvailable:sourceType];
        if (hasCamera) {
            UIImagePickerController* pickerController = [[[UIImagePickerController alloc] init] autorelease];
            
            pickerController.delegate = [CodenameOne_GLViewController instance];
            pickerController.sourceType = sourceType;
            
            if(movie) {
                pickerController.mediaTypes = [NSArray arrayWithObjects:@"public.movie", nil];
            } else {
                pickerController.mediaTypes = [NSArray arrayWithObjects:@"public.image", nil];
            }
            
            if(popoverSupported() && sourceType != UIImagePickerControllerSourceTypeCamera)
            {
                popoverController = [[[NSClassFromString(@"UIPopoverController") alloc] 
                                      initWithContentViewController:pickerController] autorelease]; 
                popoverController.delegate = [CodenameOne_GLViewController instance];
                [popoverController presentPopoverFromRect:CGRectMake(0,32,320,480)
                                                   inView:[CodenameOne_GLViewController instance]
                                 permittedArrowDirections:UIPopoverArrowDirectionAny 
                                                 animated:YES]; 
            }
            else 
            { 
                [[CodenameOne_GLViewController instance] presentModalViewController:pickerController animated:YES]; 
            }
            [pool release];
        }
    });
}

int popoverSupported()
{
	return ( NSClassFromString(@"UIPopoverController") != nil) &&  (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUDID__() {
    return fromNSString([OpenUDID value]);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isGoodLocation___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    CLLocationManager* l = (CLLocationManager*)peer;
    CLLocation* loc = l.location;
    if(loc == nil) {
        [pool release];
        return 0;
    }
    
    // Filter out points by invalid accuracy
    if (loc.horizontalAccuracy < 0) {
        [pool release];
        return 0;
    }
    
    [pool release];
    // The newLocation is good to use
    return 1;    
}

void com_codename1_impl_ios_IOSNative_startUpdatingLocation___long(JAVA_LONG peer) {
    CLLocationManager* l = (CLLocationManager*)peer;
    l.delegate = [CodenameOne_GLViewController instance];
    [l startUpdatingLocation];
}

void com_codename1_impl_ios_IOSNative_stopUpdatingLocation___long(JAVA_LONG peer) {
    CLLocationManager* l = (CLLocationManager*)peer;
    [l stopUpdatingLocation];    
}

ABAddressBookRef globalAddressBook = nil;
ABAddressBookRef getAddressBook() {
    if(globalAddressBook == nil) {
        globalAddressBook = ABAddressBookCreate();
    }
    return globalAddressBook;
}


JAVA_INT com_codename1_impl_ios_IOSNative_getContactCount___boolean(JAVA_BOOLEAN includeNumbers) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    ABAddressBookRef addressBook = getAddressBook();
    CFIndex nPeople = ABAddressBookGetPersonCount(addressBook);
    [pool release];
    return nPeople;
}

void com_codename1_impl_ios_IOSNative_getContactRefIds___int_1ARRAY_boolean(JAVA_OBJECT intArray, JAVA_BOOLEAN includeNumbers) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    org_xmlvm_runtime_XMLVMArray* iArray = intArray;
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)iArray->fields.org_xmlvm_runtime_XMLVMArray.array_;    
    int size = (JAVA_ARRAY_INT*)iArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
    ABAddressBookRef addressBook = getAddressBook();
    CFArrayRef allPeople = ABAddressBookCopyArrayOfAllPeople(addressBook);
    for(int iter = 0 ; iter < size ; iter++) {
        ABRecordRef ref = CFArrayGetValueAtIndex(allPeople, iter);
        data[iter] = ABRecordGetRecordID(ref);
    }
    [pool release];
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonFirstName___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    ABRecordRef i = (ABRecordRef)peer;
    NSString* k = (NSString*)ABRecordCopyValue(i,kABPersonFirstNameProperty);    
    JAVA_OBJECT ret = fromNSString(k);
    [pool release];
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonSurnameName___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    ABRecordRef i = (ABRecordRef)peer;
    NSString* k = (NSString*)ABRecordCopyValue(i,kABPersonLastNameProperty);    
    JAVA_OBJECT ret = fromNSString(k);
    [pool release];
    return ret;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getPersonPhoneCount___long(JAVA_LONG peer) {
    //NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    //[pool release];
    return 1;
}

JAVA_OBJECT copyValueAsString(ABMultiValueRef r) {
    JAVA_OBJECT ret = JAVA_NULL;
    if(ABMultiValueGetCount(r) > 0) {
        NSString *k = (NSString *)ABMultiValueCopyValueAtIndex(r, 0);
        ret = fromNSString(k);
    }
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPhone___long_int(JAVA_LONG peer, JAVA_INT offset) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    ABRecordRef i = (ABRecordRef)peer;
    ABMultiValueRef k = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonPhoneProperty);    
    JAVA_OBJECT ret = copyValueAsString(k);
    [pool release];
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPhoneType___long_int(JAVA_LONG peer, JAVA_INT offset) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    //ABRecordRef i = (ABRecordRef)peer;
    //ABMultiValueRef k = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonPhoneMainLabel);    
    //JAVA_OBJECT ret = copyValueAsString(k);
    JAVA_OBJECT ret = fromNSString(@"work");
    [pool release];
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPrimaryPhone___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    ABRecordRef i = (ABRecordRef)peer;
    ABMultiValueRef k = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonPhoneProperty);    
    JAVA_OBJECT ret = copyValueAsString(k);
    [pool release];
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonEmail___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    ABRecordRef i = (ABRecordRef)peer;
    ABMultiValueRef emails = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonEmailProperty);    
    JAVA_OBJECT ret = copyValueAsString(emails);
    [pool release];
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonAddress___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    ABRecordRef i = (ABRecordRef)peer;
    NSString* k = (NSString*)ABRecordCopyValue(i,kABPersonAddressProperty);    
    JAVA_OBJECT ret = fromNSString(k);
    [pool release];
    return ret;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createPersonPhotoImage___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    ABRecordRef i = (ABRecordRef)peer;
    GLUIImage* g = nil;
    if(ABPersonHasImageData(i)){
        UIImage* img = [UIImage imageWithData:(NSData *)ABPersonCopyImageData(i)];
        g = [[GLUIImage alloc] initWithImage:img];
    }    
    [pool release];
    return g;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getPersonWithRecordID___int(JAVA_INT recId) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    ABRecordRef i = ABAddressBookGetPersonWithRecordID(getAddressBook(), recId);
    [i retain];
    [pool release];
    return i;
}

void com_codename1_impl_ios_IOSNative_dial___java_lang_String(JAVA_OBJECT phone) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:toNSString(phone)]];
    [pool release];
}

void com_codename1_impl_ios_IOSNative_sendSMS___java_lang_String_java_lang_String(
    JAVA_OBJECT  number, JAVA_OBJECT  text) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        MFMessageComposeViewController *picker = [[MFMessageComposeViewController alloc] init];
        picker.messageComposeDelegate = [CodenameOne_GLViewController instance];
        
        // Recipient.
        NSString *recipient = toNSString(number);
        NSArray *recipientsArray = [NSArray arrayWithObject:recipient];
        
        [picker setRecipients:recipientsArray];
        
        // Body.
        NSString *smsBody = toNSString(text);
        [picker setBody:smsBody];
        
        [[CodenameOne_GLViewController instance] presentModalViewController:picker animated:YES];
        
        [picker release];
        [pool release];
    });
}


void com_codename1_impl_ios_IOSNative_registerPush__() {
    [[UIApplication sharedApplication] registerForRemoteNotificationTypes:
		(UIRemoteNotificationTypeBadge | UIRemoteNotificationTypeSound | UIRemoteNotificationTypeAlert)];
}

void com_codename1_impl_ios_IOSNative_deregisterPush__() {
    [[UIApplication sharedApplication] unregisterForRemoteNotifications];
}

UIImage* scaleImage(int destWidth, int destHeight, UIImage *img) {
    const size_t originalWidth = img.size.width;
    const size_t originalHeight = img.size.height;
    
    CGContextRef bmContext = CGBitmapContextCreate(NULL, destWidth, destHeight, 8, destWidth * 4, CGColorSpaceCreateDeviceRGB(), kCGBitmapByteOrderDefault | kCGImageAlphaPremultipliedFirst);
    

    if (!bmContext) {
        return nil;
    }
    
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
    UIImage* scaled = [UIImage imageWithCGImage:scaledImageRef];
    
    CGImageRelease(scaledImageRef);
    CGContextRelease(bmContext);
    
    return scaled;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageFile___long_boolean_int_int_float(JAVA_LONG imagePeer, JAVA_BOOLEAN jpeg, int width, int height, JAVA_FLOAT quality) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    UIImage* i = [(GLUIImage*)imagePeer getImage];
    if(width == -1) {
        width = (int)i.size.width;
    }
    if(height == -1) {
        height = (int)i.size.width;
    }
    NSData* data;
    if(width != ((int)i.size.width) || height != ((int)i.size.height)) {
        i = scaleImage(width, height, i);
    } 
    if(jpeg) {
        data = UIImageJPEGRepresentation(i, quality);
    } else {
        data = UIImagePNGRepresentation(i);
    }
    
    [data retain];
    [pool release];
    return data;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getNSDataSize___long(JAVA_LONG nsData) {
    NSData* d = (NSData*)nsData;
    return d.length;
} 

void com_codename1_impl_ios_IOSNative_nsDataToByteArray___long_byte_1ARRAY(JAVA_LONG nsData, JAVA_OBJECT dataArray) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSData* d = (NSData*)nsData;
    org_xmlvm_runtime_XMLVMArray* byteArray = dataArray;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;    
    memcpy(data, d.bytes, d.length);
    [pool release];
}



JAVA_LONG com_codename1_impl_ios_IOSNative_createAudioRecorder___java_lang_String(
    JAVA_OBJECT  destinationFile) {
    __block AVAudioRecorder* recorder = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        NSString * filePath = toNSString(destinationFile);
        NSLog(@"Recording audio to: %@", filePath);
        NSDictionary *recordSettings = [[NSDictionary alloc] initWithObjectsAndKeys:
        [NSNumber numberWithFloat: 44100.0], AVSampleRateKey,
        [NSNumber numberWithInt: kAudioFormatMPEG4AAC],AVFormatIDKey,
        [NSNumber numberWithInt: 1], AVNumberOfChannelsKey,
        [NSNumber numberWithInt: AVAudioQualityMax], AVEncoderAudioQualityKey,nil];
        NSError *error = nil;
        recorder = [[AVAudioRecorder alloc] initWithURL: [NSURL fileURLWithPath:filePath]
             settings: recordSettings
             error: &error];
        if(error != nil) {  
            NSLog(@"Error in recording: %@", [error localizedDescription]);        
        }
        [pool release];
    });
    return recorder;
}

void com_codename1_impl_ios_IOSNative_startAudioRecord___long(
    JAVA_LONG  peer) {
    AVAudioRecorder* recorder = (AVAudioRecorder*)peer;
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        [recorder prepareToRecord];
        [recorder record];
        [recorder retain];
        [pool release];
    });
}

void com_codename1_impl_ios_IOSNative_pauseAudioRecord___long(
    JAVA_LONG  peer) {
    AVAudioRecorder* recorder = (AVAudioRecorder*)peer;
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        [recorder pause];
        [pool release];
    });
}

void com_codename1_impl_ios_IOSNative_cleanupAudioRecord___long(
    JAVA_LONG  peer) {
    AVAudioRecorder* recorder = (AVAudioRecorder*)peer;
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        [recorder stop];
        [recorder release];
        [pool release];
    });
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofObjArrayI___java_lang_Object(JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_java_lang_Object_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofByteArrayI___java_lang_Object(JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_byte_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofShortArrayI___java_lang_Object(JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_short_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofLongArrayI___java_lang_Object(JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_long_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofIntArrayI___java_lang_Object(JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_int_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofFloatArrayI___java_lang_Object(JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_float_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofDoubleArrayI___java_lang_Object(JAVA_OBJECT n1)
{
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)n1;
    return arr->fields.org_xmlvm_runtime_XMLVMArray.type_ == __CLASS_double_1ARRAY;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlDbExists___java_lang_String(JAVA_OBJECT name) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];    
    
    const char* chrs = stringToUTF8(name);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    
    NSString* foofile = [documentsPath stringByAppendingPathComponent:nsSrc];
    BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:foofile];
    [pool release];    
    return fileExists;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlDbCreateAndOpen___java_lang_String(JAVA_OBJECT name) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];    
    
    const char* chrs = stringToUTF8(name);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    
    NSString* foofile = [documentsPath stringByAppendingPathComponent:nsSrc];

    sqlite3 *db;
    int rc = sqlite3_open([foofile UTF8String], &db);
    
    [pool release];    
    return db;
}

void com_codename1_impl_ios_IOSNative_sqlDbDelete___java_lang_String(JAVA_OBJECT name) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];    
    
    const char* chrs = stringToUTF8(name);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    
    NSString* foofile = [documentsPath stringByAppendingPathComponent:nsSrc];
    [[NSFileManager defaultManager] removeItemAtPath:foofile error:nil];
    [pool release];    
}

void com_codename1_impl_ios_IOSNative_sqlDbClose___long(JAVA_LONG db) {
    sqlite3_free((sqlite3*)db);
}

void com_codename1_impl_ios_IOSNative_sqlDbExec___long_java_lang_String_java_lang_String_1ARRAY(JAVA_LONG dbPeer, JAVA_OBJECT sql, JAVA_OBJECT args) {
    sqlite3* db = (sqlite3*)dbPeer;
    const char* chrs = stringToUTF8(sql);
    if(args != nil) {
        sqlite3_stmt *addStmt = nil;
        sqlite3_prepare_v2(db, chrs, -1, &addStmt, nil);
        org_xmlvm_runtime_XMLVMArray* stringArray = args;
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)stringArray->fields.org_xmlvm_runtime_XMLVMArray.array_;    

        int count = stringArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
        for(int iter = 0 ; iter < count ; iter++) {
            java_lang_String* str = (java_lang_String*)data[iter];
            const char* chrs = stringToUTF8(str);
            sqlite3_bind_text(addStmt, iter + 1, chrs, -1, SQLITE_TRANSIENT);
        }
        sqlite3_step(addStmt);
        sqlite3_finalize(addStmt);
    } else {
        sqlite3_exec(db, chrs, 0, 0, 0);
    }
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlDbExecQuery___long_java_lang_String_java_lang_String_1ARRAY(JAVA_LONG dbPeer, JAVA_OBJECT sql, JAVA_OBJECT args) {
    sqlite3* db = (sqlite3*)dbPeer;
    const char* chrs = stringToUTF8(sql);
    sqlite3_stmt *addStmt = nil;
    sqlite3_prepare_v2(db, chrs, -1, &addStmt, nil);

    if(args != nil) {
        org_xmlvm_runtime_XMLVMArray* stringArray = args;
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)stringArray->fields.org_xmlvm_runtime_XMLVMArray.array_;    
        int count = stringArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
        for(int iter = 0 ; iter < count ; iter++) {
            java_lang_String* str = (java_lang_String*)data[iter];
            const char* chrs = stringToUTF8(str);
            sqlite3_bind_text(addStmt, iter + 1, chrs, -1, SQLITE_TRANSIENT);
        }
    }
    return addStmt;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorFirst___long(JAVA_LONG statementPeer) {
    sqlite3_reset((sqlite3_stmt *)statementPeer);
    return YES;
}
    
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorNext___long(JAVA_LONG statementPeer) {
    return sqlite3_step((sqlite3_stmt *)statementPeer) == SQLITE_ROW;    
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlGetColName___long_int(JAVA_LONG statementPeer, JAVA_INT index) {
    return xmlvm_create_java_string(sqlite3_column_name((sqlite3_stmt*)statementPeer, index));
}

void com_codename1_impl_ios_IOSNative_sqlCursorCloseStatement___long(JAVA_LONG statement) {
    sqlite3_finalize((sqlite3_stmt*)statement);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnBlob___long_int(JAVA_LONG statement, JAVA_INT col) {
    return nil;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnDouble___long_int(JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_double((sqlite3_stmt*)statement, col);
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnFloat___long_int(JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_double((sqlite3_stmt*)statement, col);
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnInteger___long_int(JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_int((sqlite3_stmt*)statement, col);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnLong___long_int(JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_int64((sqlite3_stmt*)statement, col);
}

JAVA_SHORT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnShort___long_int(JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_int((sqlite3_stmt*)statement, col);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnString___long_int(JAVA_LONG statement, JAVA_INT col) {
    return xmlvm_create_java_string(sqlite3_column_text((sqlite3_stmt*)statement, col));
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlCursorGetColumnCount___long(JAVA_LONG statement) {
    sqlite3_stmt *stmt = (sqlite3_stmt*)statement;
    return sqlite3_column_count(stmt);
}
