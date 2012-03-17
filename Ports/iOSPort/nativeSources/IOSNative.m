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
    Java_com_codename1_impl_ios_IOSImplementation_editStringAtImpl(n1, n2, n3, n4, n5, n6, n7, n8, n9, xmlvm_java_string_to_const_char(n10), 0);
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
    const char* chr = xmlvm_java_string_to_const_char(n4);
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringMutableImpl(n1, n2, n3, chr, strlen(chr), n5, n6);
    [pool release];
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawStringGlobal___int_int_long_java_lang_String_int_int(JAVA_INT n1, JAVA_INT n2, JAVA_LONG n3, JAVA_OBJECT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawStringGlobal___int_int_long_java_lang_String_int_int]
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    const char* chr = xmlvm_java_string_to_const_char(n4);
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
    const char* chr = xmlvm_java_string_to_const_char(n2);
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
    JAVA_INT i = getResourceSize(xmlvm_java_string_to_const_char(n1), 0, xmlvm_java_string_to_const_char(n2), 0);
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
    loadResourceFile(xmlvm_java_string_to_const_char(n1), 0, xmlvm_java_string_to_const_char(n2), 0, data);
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

void stringEdit(int finished, int cursorPos, const char* text) {
    com_codename1_impl_ios_IOSImplementation_editingUpdate___java_lang_String_int_boolean(
        xmlvm_create_java_string(text), cursorPos, finished != 0
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
    const char* chrs = xmlvm_java_string_to_const_char(str);
    return [NSString stringWithUTF8String:chrs];
}

JAVA_INT com_codename1_impl_ios_IOSNative_writeToFile___byte_1ARRAY_java_lang_String(JAVA_OBJECT n1, JAVA_OBJECT path) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    const char* chrs = xmlvm_java_string_to_const_char(path);
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
    const char* chrs = xmlvm_java_string_to_const_char(path);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    NSFileManager *man = [[NSFileManager alloc] init];
    NSDictionary *attrs = [man attributesOfItemAtPath:ns error:nil];
    UInt32 result = [attrs fileSize];
    [man release];
    [pool release];
    return result;
}

void com_codename1_impl_ios_IOSNative_readFile___java_lang_String_byte_1ARRAY(JAVA_OBJECT path, JAVA_OBJECT n1) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    const char* chrs = xmlvm_java_string_to_const_char(path);
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
    const char* chrs = xmlvm_java_string_to_const_char(file);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    [fm removeItemAtPath:ns error:nil];
    [fm release];
    [pool release];
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_fileExists___java_lang_String(JAVA_OBJECT file) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = xmlvm_java_string_to_const_char(file);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    JAVA_BOOLEAN b = [fm fileExistsAtPath:ns];
    [fm release];
    [pool release];
    return b;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDirectory___java_lang_String(JAVA_OBJECT file) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = xmlvm_java_string_to_const_char(file);
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
    const char* chrs = xmlvm_java_string_to_const_char(dir);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    NSArray* nsArr = [fm contentsOfDirectoryAtPath:ns];
    int i = nsArr.count;
    [fm release];
    [pool release];
    return i;   
}

void com_codename1_impl_ios_IOSNative_listFilesInDir___java_lang_String_java_lang_String_1ARRAY(JAVA_OBJECT dir, JAVA_OBJECT files) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = xmlvm_java_string_to_const_char(dir);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    NSArray* nsArr = [fm contentsOfDirectoryAtPath:ns];

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
    const char* chrs = xmlvm_java_string_to_const_char(dir);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    [fm currentDirectoryPath:ns];
    [fm release];
    [pool release];
}

void com_codename1_impl_ios_IOSNative_moveFile___java_lang_String_java_lang_String(JAVA_OBJECT src, JAVA_OBJECT dest) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = xmlvm_java_string_to_const_char(src);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    const char* chrsDest = xmlvm_java_string_to_const_char(dest);
    NSString* nsDst = [NSString stringWithUTF8String:chrsDest];
    [fm moveItemAtPath:nsSrc toPath:nsDst error:nil];
    [fm release];
    [pool release];    
}

extern void Java_com_codename1_impl_ios_IOSImplementation_setImageName(void* nativeImage, const char* name);


void com_codename1_impl_ios_IOSNative_setImageName___long_java_lang_String(JAVA_LONG nativeImage, JAVA_OBJECT name) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    const char* chrs = xmlvm_java_string_to_const_char(name);
    Java_com_codename1_impl_ios_IOSImplementation_setImageName(nativeImage, chrs);
    [pool release];    
}

JAVA_LONG com_codename1_impl_ios_IOSNative_openConnection___java_lang_String_int(JAVA_OBJECT url, JAVA_INT timeout) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NetworkConnectionImpl* impl = [[NetworkConnectionImpl alloc] init];
    const char* chrs = xmlvm_java_string_to_const_char(url);
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
    const char* chrs = xmlvm_java_string_to_const_char(mtd);
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
    const char* chrs = xmlvm_java_string_to_const_char(name);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    java_lang_String* str = fromNSString([impl getResponseHeader:nsSrc]);
    [pool release];    
    return str;
}

void com_codename1_impl_ios_IOSNative_addHeader___long_java_lang_String_java_lang_String(JAVA_LONG peer, JAVA_OBJECT key, JAVA_OBJECT value) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NetworkConnectionImpl* impl = (NetworkConnectionImpl*)peer;
    const char* chrs = xmlvm_java_string_to_const_char(key);
    NSString* nsKey = [NSString stringWithUTF8String:chrs];
    chrs = xmlvm_java_string_to_const_char(value);
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
    const char* chrs = xmlvm_java_string_to_const_char(n1);
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
    dispatch_sync(dispatch_get_main_queue(), ^{
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
    dispatch_sync(dispatch_get_main_queue(), ^{
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
    dispatch_sync(dispatch_get_main_queue(), ^{
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
    dispatch_sync(dispatch_get_main_queue(), ^{
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
    dispatch_sync(dispatch_get_main_queue(), ^{
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
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    AudioPlayer* pl = (AudioPlayer*)peer;
    int dur = [pl getAudioDuration];
    [pool release];
    return dur;
}

void com_codename1_impl_ios_IOSNative_playAudio___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    AudioPlayer* pl = (AudioPlayer*)peer;
    [pl playAudio];
    [pool release];
}


JAVA_INT com_codename1_impl_ios_IOSNative_getAudioTime___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    AudioPlayer* pl = (AudioPlayer*)peer;
    int dur = [pl getAudioTime];
    [pool release];
    return dur;
}

void com_codename1_impl_ios_IOSNative_pauseAudio___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    AudioPlayer* pl = (AudioPlayer*)peer;
    [pl pauseAudio];
    [pool release];
}

void com_codename1_impl_ios_IOSNative_setAudioTime___long_int(JAVA_LONG peer, JAVA_INT time) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    AudioPlayer* pl = (AudioPlayer*)peer;
    [pl setAudioTime:time];
    [pool release];
}

void com_codename1_impl_ios_IOSNative_cleanupAudio___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    AudioPlayer* pl = (AudioPlayer*)peer;
    [pl release];
    [pool release];
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___java_lang_String_java_lang_Runnable(JAVA_OBJECT uri, JAVA_OBJECT onCompletion) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    const char* chrs = xmlvm_java_string_to_const_char(uri);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    AudioPlayer* pl = [[AudioPlayer alloc] initWithURL:uri callback:onCompletion];
    [pool release];
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___byte_1ARRAY_java_lang_Runnable(JAVA_OBJECT b, JAVA_OBJECT onCompletion) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    org_xmlvm_runtime_XMLVMArray* byteArray = b;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;    
    NSData* d = [NSData dataWithBytes:data length:byteArray->fields.org_xmlvm_runtime_XMLVMArray.length_];
    AudioPlayer* pl = [[AudioPlayer alloc] initWithURL:d callback:onCompletion];
    [pool release];
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

JAVA_LONG com_codename1_impl_ios_IOSNative_createBrowserComponent__() {
    __block UIWebView* response = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        response = [[UIWebView alloc] initWithFrame:CGRectMake(3000, 0, 200, 200)];
        response.backgroundColor = [UIColor whiteColor];
        response.autoresizesSubviews = YES;
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

JAVA_LONG com_codename1_impl_ios_IOSNative_createCLLocation__() {
    return [[CLLocationManager alloc] init];
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getCurrentLocationObject___long(JAVA_LONG peer) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    CLLocationManager* l = (CLLocationManager*)peer;
    CLLocation* loc = l.location;
    [loc retain];
    [pool release];
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
JAVA_OBJECT com_codename1_impl_ios_IOSNative_captureCamera___boolean(JAVA_BOOLEAN movie) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	UIImagePickerControllerSourceType sourceType = UIImagePickerControllerSourceTypeCamera; // default
    popoverController = nil;
    
	bool hasCamera = [UIImagePickerController isSourceTypeAvailable:sourceType];
	if (!hasCamera) {
		return nil;
	} else {        
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
            [[CodenameOne_GLViewController instance]
             presentModalViewController:pickerController animated:YES]; 
        }
        [pool release];
    }
}

int popoverSupported()
{
	return ( NSClassFromString(@"UIPopoverController") != nil) &&  (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUDID__() {
    return fromNSString([[UIDevice currentDevice] uniqueIdentifier]);

}

void com_codename1_impl_ios_IOSNative_startUpdatingLocation___long(JAVA_LONG peer) {
    CLLocationManager* l = (CLLocationManager*)peer;
    [l startUpdatingHeading];
}

void com_codename1_impl_ios_IOSNative_stopUpdatingLocation___long(JAVA_LONG peer) {
    CLLocationManager* l = (CLLocationManager*)peer;
    [l stopUpdatingLocation];    
}
