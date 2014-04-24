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

#ifndef NEW_CODENAME_ONE_VM
#include "xmlvm-util.h"
#endif

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
#import "StoreKit/StoreKit.h"
#import "ScanCodeImpl.h"
#include "com_codename1_contacts_Contact.h"
#include "java_util_Hashtable.h"
#include "com_codename1_ui_Image.h"
#include "com_codename1_impl_ios_IOSImplementation_NativeImage.h"
#import "SocketImpl.h"

//#import "QRCodeReaderOC.h"

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
(int x, int y, int w, int h, void* peer, int isSingleLine, int rows, int maxSize, 
        int constraint, const char* str, int len, BOOL dialogHeight, int color, JAVA_LONG imagePeer,
        int padTop, int padBottom, int padLeft, int padRight, NSString* hintString, BOOL showToolbar);

extern void Java_com_codename1_impl_ios_IOSImplementation_resetAffineGlobal();

extern void Java_com_codename1_impl_ios_IOSImplementation_scale(float x, float y);

extern int isIPad();
extern int isIOS7();

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

void com_codename1_impl_ios_IOSNative_initVM__(JAVA_OBJECT instanceObject)
{
    POOL_BEGIN();
    int retVal = UIApplicationMain(0, nil, nil, @"CodenameOne_GLAppDelegate");
    POOL_END();
}

void xmlvm_init_native_com_codename1_impl_ios_IOSNative()
{
}


void com_codename1_impl_ios_IOSNative_deinitializeVM__(JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_deinitializeVM__]
    deinitVMImpl();
    //XMLVM_END_WRAPPER
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isPainted__(JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_isPainted__]
    return isPainted();
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplayWidth__(JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_getDisplayWidth__]
    POOL_BEGIN();
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl();
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplayHeight__(JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_getDisplayHeight__]
    POOL_BEGIN();
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl();
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
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

void com_codename1_impl_ios_IOSNative_editStringAt___int_int_int_int_long_boolean_int_int_int_java_lang_String_boolean_int_long_int_int_int_int_java_lang_String_boolean(
        JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_LONG n5, JAVA_BOOLEAN n6, JAVA_INT n7, 
        JAVA_INT n8, JAVA_INT n9, JAVA_OBJECT n10, JAVA_BOOLEAN forceSlide,
                JAVA_INT color, JAVA_LONG imagePeer, JAVA_INT padTop, JAVA_INT padBottom, JAVA_INT padLeft, JAVA_INT padRight, JAVA_OBJECT hint, JAVA_BOOLEAN showToolbar)
{
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_editStringAtImpl(n1, n2, n3, n4, n5, n6, n7, n8, n9, stringToUTF8(n10), 0, forceSlide, color, imagePeer,
            padTop, padBottom, padLeft, padRight, toNSString(hint), showToolbar);
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_flushBuffer___long_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_flushBuffer___long_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_flushBufferImpl(n1, n2, n3, n4, n5);
    POOL_END();
    //XMLVM_END_WRAPPER
}


void com_codename1_impl_ios_IOSNative_imageRgbToIntArray___long_int_1ARRAY_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_OBJECT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    POOL_BEGIN();
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* intArray = n2;
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else 
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)n2)->data;
#endif
    Java_com_codename1_impl_ios_IOSImplementation_imageRgbToIntArrayImpl(n1, data, n3, n4, n5, n6);
    POOL_END();
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageFromARGB___int_1ARRAY_int_int(JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_INT n2, JAVA_INT n3)
{
    POOL_BEGIN();
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* intArray = n1;
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)n1)->data;
#endif
    JAVA_ARRAY_LONG i = Java_com_codename1_impl_ios_IOSImplementation_createImageFromARGBImpl(data, n2, n3);
    POOL_END();
    return i;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImage___byte_1ARRAY_int_1ARRAY(JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT n2)
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
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_createImageImpl(data, byteArray->length, data2);    
#endif
    return i;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageNSData___long_int_1ARRAY(JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_OBJECT n2)
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

JAVA_LONG com_codename1_impl_ios_IOSNative_scale___long_int_int(JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT n2, JAVA_INT n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_scale___long_int_int]
    POOL_BEGIN();
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_scaleImpl(n1, n2, n3);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_setNativeClippingMutable___int_int_int_int_boolean(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_BOOLEAN n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_setNativeClippingMutable___int_int_int_int_boolean]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingMutableImpl(n1, n2, n3, n4, n5);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_setNativeClippingGlobal___int_int_int_int_boolean(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_BOOLEAN n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_setNativeClippingGlobal___int_int_int_int_boolean]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingGlobalImpl(n1, n2, n3, n4, n5);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawLineMutable___int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawLineMutable___int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineMutableImpl(n1, n2, n3, n4, n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawLineGlobal___int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawLineGlobal___int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineGlobalImpl(n1, n2, n3, n4, n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillRectMutable___int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillRectMutable___int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectMutableImpl(n1, n2, n3, n4, n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillRectGlobal___int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillRectGlobal___int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectGlobalImpl(n1, n2, n3, n4, n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawRectMutable___int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawRectMutable___int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectMutableImpl(n1, n2, n3, n4, n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawRectGlobal___int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawRectGlobal___int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectGlobalImpl(n1, n2, n3, n4, n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawRoundRectMutable___int_int_int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawRoundRectMutable___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRoundRectMutableImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawRoundRectGlobal___int_int_int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawRoundRectGlobal___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRoundRectGlobalImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillRoundRectMutable___int_int_int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillRoundRectMutable___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillRoundRectMutableImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillRoundRectGlobal___int_int_int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillRoundRectGlobal___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillRoundRectGlobalImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillArcMutable___int_int_int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillArcMutable___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillArcMutableImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawArcMutable___int_int_int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawArcMutable___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawArcMutableImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeFillArcGlobal___int_int_int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeFillArcGlobal___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillArcGlobalImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawArcGlobal___int_int_int_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_INT n7, JAVA_INT n8)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawArcGlobal___int_int_int_int_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawArcGlobalImpl(n1, n2, n3, n4, n5, n6, n7, n8);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawStringMutable___int_int_long_java_lang_String_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_LONG n3, JAVA_OBJECT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawStringMutable___int_int_long_java_lang_String_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringMutableImpl(n1, n2, n3, toNSString(n4), n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawStringGlobal___int_int_long_java_lang_String_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_LONG n3, JAVA_OBJECT n4, JAVA_INT n5, JAVA_INT n6)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawStringGlobal___int_int_long_java_lang_String_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringGlobalImpl(n1, n2, n3, toNSString(n4), n5, n6);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawImageMutable___long_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT alpha, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawImageMutable___long_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageMutableImpl(n1, alpha, n2, n3, n4, n5);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeDrawImageGlobal___long_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT alpha, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeDrawImageGlobal___long_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl(n1, alpha, n2, n3, n4, n5);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_nativeTileImageGlobal___long_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT alpha, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_nativeTileImageGlobal___long_int_int_int_int]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_nativeTileImageGlobalImpl(n1, alpha, n2, n3, n4, n5);
    POOL_END();
    //XMLVM_END_WRAPPER
}


JAVA_INT com_codename1_impl_ios_IOSNative_stringWidthNative___long_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_OBJECT n2)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_stringWidthNative___long_java_lang_String]
    POOL_BEGIN();
    const char* chr = stringToUTF8(n2);
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_stringWidthNativeImpl(n1, chr, strlen(chr));
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_charWidthNative___long_char(JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_CHAR n2)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_charWidthNative___long_char]
    POOL_BEGIN();
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_charWidthNativeImpl(n1, n2);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_getFontHeightNative___long(JAVA_OBJECT instanceObject, JAVA_LONG n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_getFontHeightNative___long]
    POOL_BEGIN();
    JAVA_INT i = Java_com_codename1_impl_ios_IOSImplementation_getFontHeightNativeImpl(n1);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createSystemFont___int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_createSystemFont___int_int_int]
    POOL_BEGIN();
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_createSystemFontImpl(n1, n2, n3);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResourceSize___java_lang_String_java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT n2)
{
    //XMLVM_BEGIN_NATIVE[com_codename1_impl_ios_IOSNative_getResourceSize___java_lang_String_java_lang_String]
    POOL_BEGIN();
    JAVA_INT i = getResourceSize(stringToUTF8(n1), 0, stringToUTF8(n2), 0);
    POOL_END();
    return i;
    //XMLVM_END_NATIVE
}

void com_codename1_impl_ios_IOSNative_loadResource___java_lang_String_java_lang_String_byte_1ARRAY(JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT n2, JAVA_OBJECT n3)
{
    POOL_BEGIN();
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* byteArray = n3;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)n3)->data;
#endif
    loadResourceFile(stringToUTF8(n1), 0, stringToUTF8(n2), 0, data);
    POOL_END();
}


JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeMutableImage___int_int_int(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_createNativeMutableImage___int_int_int]
    POOL_BEGIN();
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_createNativeMutableImageImpl(n1, n2, n3);
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_startDrawingOnImage___int_int_long(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_LONG n3)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_startDrawingOnImage___int_int_long]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_startDrawingOnImageImpl(n1, n2, n3);
    POOL_END();
    //XMLVM_END_WRAPPER
}

JAVA_LONG com_codename1_impl_ios_IOSNative_finishDrawingOnImage__(JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_finishDrawingOnImage__]
    POOL_BEGIN();
    JAVA_LONG i = Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl();
    POOL_END();
    return i;
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_deleteNativePeer___long(JAVA_OBJECT instanceObject, JAVA_LONG n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_deleteNativePeer___long]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_deleteNativePeerImpl(n1);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_deleteNativeFontPeer___long(JAVA_OBJECT instanceObject, JAVA_LONG n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_deleteNativePeer___long]
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_deleteNativeFontPeerImpl(n1);
    POOL_END();
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_resetAffineGlobal__(JAVA_OBJECT instanceObject)
{
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_resetAffineGlobal();
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_scaleGlobal___float_float(JAVA_OBJECT instanceObject, JAVA_FLOAT x, JAVA_FLOAT y) {
    POOL_BEGIN();
    Java_com_codename1_impl_ios_IOSImplementation_scale(x, y);
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_rotateGlobal___float(JAVA_OBJECT instanceObject, JAVA_FLOAT angle) {
}

void com_codename1_impl_ios_IOSNative_rotateGlobal___float_int_int(JAVA_OBJECT instanceObject, JAVA_FLOAT angle, JAVA_INT x, JAVA_INT y) {
    Rotate* f = [[Rotate alloc] initWithArgs:angle xx:x yy:y];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
}

void com_codename1_impl_ios_IOSNative_shearGlobal___float_float(JAVA_OBJECT instanceObject, JAVA_FLOAT x, JAVA_FLOAT y) {
    
}



void pointerPressed(int* x, int* y, int length) {
    if(length == 1) {
        com_codename1_impl_ios_IOSImplementation_pointerPressedCallback___int_int(x[0], y[0]);
    } else {
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* xArray = XMLVMArray_createSingleDimension(__CLASS_int, length);
        memcpy(xArray->fields.org_xmlvm_runtime_XMLVMArray.array_, x, length * sizeof(JAVA_INT));
        org_xmlvm_runtime_XMLVMArray* yArray = XMLVMArray_createSingleDimension(__CLASS_int, length);
        memcpy(yArray->fields.org_xmlvm_runtime_XMLVMArray.array_, y, length * sizeof(JAVA_INT));
        com_codename1_impl_ios_IOSImplementation_pointerPressed___int_1ARRAY_int_1ARRAY(com_codename1_impl_ios_IOSImplementation_GET_instance(), xArray, yArray);
#else
        JAVA_OBJECT xArray = __NEW_ARRAY_JAVA_INT(length);
        memcpy(((JAVA_ARRAY)xArray)->data, x, length * sizeof(JAVA_INT));
        JAVA_OBJECT yArray = __NEW_ARRAY_JAVA_INT(length);
        memcpy(((JAVA_ARRAY)yArray)->data, y, length * sizeof(JAVA_INT));
        com_codename1_impl_ios_IOSImplementation_pointerPressed___int_1ARRAY_int_1ARRAY(get_static_com_codename1_impl_ios_IOSImplementation_instance(), xArray, yArray);
#endif
    }
}

void pointerDragged(int* x, int* y, int length) {
    if(length == 1) {
        com_codename1_impl_ios_IOSImplementation_pointerDraggedCallback___int_int(x[0], y[0]);
    } else {
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* xArray = XMLVMArray_createSingleDimension(__CLASS_int, length);
        memcpy(xArray->fields.org_xmlvm_runtime_XMLVMArray.array_, x, length * sizeof(JAVA_INT));
        org_xmlvm_runtime_XMLVMArray* yArray = XMLVMArray_createSingleDimension(__CLASS_int, length);
        memcpy(yArray->fields.org_xmlvm_runtime_XMLVMArray.array_, y, length * sizeof(JAVA_INT));
        com_codename1_impl_ios_IOSImplementation_pointerDragged___int_1ARRAY_int_1ARRAY(com_codename1_impl_ios_IOSImplementation_GET_instance(), xArray, yArray);
#else
        JAVA_OBJECT xArray = __NEW_ARRAY_JAVA_INT(length);
        memcpy(((JAVA_ARRAY)xArray)->data, x, length * sizeof(JAVA_ARRAY_INT));
        JAVA_OBJECT yArray = __NEW_ARRAY_JAVA_INT(length);
        memcpy(((JAVA_ARRAY)yArray)->data, y, length * sizeof(JAVA_ARRAY_INT));
        com_codename1_impl_ios_IOSImplementation_pointerDragged___int_1ARRAY_int_1ARRAY(get_static_com_codename1_impl_ios_IOSImplementation_instance(), xArray, yArray);
#endif
    }
}

void pointerReleased(int* x, int* y, int length) {
    if(length == 1) {
        com_codename1_impl_ios_IOSImplementation_pointerReleasedCallback___int_int(x[0], y[0]);
    } else {
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* xArray = XMLVMArray_createSingleDimension(__CLASS_int, length);
        memcpy(xArray->fields.org_xmlvm_runtime_XMLVMArray.array_, x, length * sizeof(JAVA_INT));
        org_xmlvm_runtime_XMLVMArray* yArray = XMLVMArray_createSingleDimension(__CLASS_int, length);
        memcpy(yArray->fields.org_xmlvm_runtime_XMLVMArray.array_, y, length * sizeof(JAVA_INT));
        com_codename1_impl_ios_IOSImplementation_pointerReleased___int_1ARRAY_int_1ARRAY(com_codename1_impl_ios_IOSImplementation_GET_instance(), xArray, yArray);
#else
        JAVA_OBJECT xArray = __NEW_ARRAY_JAVA_INT(length);
        memcpy(((JAVA_ARRAY)xArray)->data, x, length * sizeof(JAVA_ARRAY_INT));
        JAVA_OBJECT yArray = __NEW_ARRAY_JAVA_INT(length);
        memcpy(((JAVA_ARRAY)yArray)->data, y, length * sizeof(JAVA_ARRAY_INT));
        com_codename1_impl_ios_IOSImplementation_pointerReleased___int_1ARRAY_int_1ARRAY(get_static_com_codename1_impl_ios_IOSImplementation_instance(), xArray, yArray);
#endif
    }
}

void screenSizeChanged(int width, int height) {
    com_codename1_impl_ios_IOSImplementation_sizeChangedImpl___int_int(width, height);
}

void stringEdit(int finished, int cursorPos, NSString* text) {
    com_codename1_impl_ios_IOSImplementation_editingUpdate___java_lang_String_int_boolean(
                                                                                          fromNSString(text), cursorPos, finished != 0
                                                                                          );
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isTablet__(JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_isPainted__]
    return isIPad();
    //XMLVM_END_WRAPPER
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isIOS7__(JAVA_OBJECT instanceObject)
{
    return isIOS7();
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNSData___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    POOL_BEGIN();
    const char* chrs = stringToUTF8(file);
    NSString* ns = [NSString stringWithUTF8String:chrs];
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
            return d;
        }
    }
    NSData* d = [NSData dataWithContentsOfFile:ns];
#ifndef CN1_USE_ARC
    [d retain];
#endif
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)d);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNSDataResource___java_lang_String_java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT name, JAVA_OBJECT type) {
    POOL_BEGIN();
    const char* chrs = stringToUTF8(name);
    NSString* nameNS = [NSString stringWithUTF8String:chrs];
    const char* chrs2 = stringToUTF8(type);
    NSString* typeNS = [NSString stringWithUTF8String:chrs2];
    NSString* path = [[NSBundle mainBundle] pathForResource:nameNS ofType:typeNS];
    NSData* iData = [NSData dataWithContentsOfFile:path];
#ifndef CN1_USE_ARC
    [iData retain];
#endif
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)iData);
}

JAVA_INT com_codename1_impl_ios_IOSNative_read___long_int(JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_INT pointer) {
    POOL_BEGIN();
    NSData* n = (BRIDGE_CAST NSData*)((void*)nsData);
    int val;
    [n getBytes:&val range:NSMakeRange(pointer, 1)];
    POOL_END();
    return val;
}

void com_codename1_impl_ios_IOSNative_read___long_byte_1ARRAY_int_int_int(JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_OBJECT destination, JAVA_INT offset, JAVA_INT length, JAVA_INT pointer) {
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

JAVA_INT com_codename1_impl_ios_IOSNative_writeToFile___byte_1ARRAY_java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT path) {
    POOL_BEGIN();
    const char* chrs = stringToUTF8(path);
    NSString* ns = [NSString stringWithUTF8String:chrs];
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

JAVA_INT com_codename1_impl_ios_IOSNative_appendToFile___byte_1ARRAY_java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT path) {
    POOL_BEGIN();
    const char* chrs = stringToUTF8(path);
    NSString* ns = [NSString stringWithUTF8String:chrs];
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

JAVA_INT com_codename1_impl_ios_IOSNative_getFileSize___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    POOL_BEGIN();
    const char* chrs = stringToUTF8(path);
    NSString* ns = [NSString stringWithUTF8String:chrs];
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

JAVA_LONG com_codename1_impl_ios_IOSNative_getFileLastModified___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    POOL_BEGIN();
    const char* chrs = stringToUTF8(path);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    if([ns hasPrefix:@"file:"]) {
        ns = [ns substringFromIndex:5];
    }
    NSFileManager *man = [[NSFileManager alloc] init];
    NSError *error = nil;
    NSDictionary *attrs = [man attributesOfItemAtPath:ns error:&error];
    if(error != nil) {  
        NSLog(@"Error getFileLastModified: %@ for the file %@", [error localizedDescription], ns);        
    }
    NSDate* modDate = [attrs objectForKey:NSFileModificationDate];
    NSTimeZone *tzone = [NSTimeZone timeZoneWithName:@"GMT"];
    JAVA_LONG result = [tzone secondsFromGMTForDate:modDate] * 1000;
#ifndef CN1_USE_ARC
    [man release];
#endif
    POOL_END();
    return result;
}

void com_codename1_impl_ios_IOSNative_readFile___java_lang_String_byte_1ARRAY(JAVA_OBJECT instanceObject, JAVA_OBJECT path, JAVA_OBJECT n1) {
    POOL_BEGIN();
    const char* chrs = stringToUTF8(path);
    NSString* ns = [NSString stringWithUTF8String:chrs];
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

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getDocumentsDir__(JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];    
    JAVA_OBJECT str = fromNSString(documentsPath);
    POOL_END();
    return str;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getCachesDir__(JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];    
    JAVA_OBJECT str = fromNSString(documentsPath);
    POOL_END();
    return str;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResourcesDir__(JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
    NSBundle *bundle = [NSBundle mainBundle];
    NSString *bundlePath = [bundle bundlePath];
    JAVA_OBJECT str = fromNSString(bundlePath);
    POOL_END();
    return str;
}

void com_codename1_impl_ios_IOSNative_deleteFile___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(file);
    NSString* ns = [NSString stringWithUTF8String:chrs];
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

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_fileExists___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(file);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    JAVA_BOOLEAN b = [fm fileExistsAtPath:ns];
#ifndef CN1_USE_ARC
    [fm release];
#endif
    POOL_END();
    return b;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDirectory___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(file);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    BOOL b = NO;
    BOOL* isDir = (&b);
    [fm fileExistsAtPath:ns isDirectory:isDir];
#ifndef CN1_USE_ARC
    [fm release];
#endif
    POOL_END();
    return b;
}

JAVA_INT com_codename1_impl_ios_IOSNative_fileCountInDir___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT dir) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(dir);
    NSString* ns = [NSString stringWithUTF8String:chrs];
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

void com_codename1_impl_ios_IOSNative_listFilesInDir___java_lang_String_java_lang_String_1ARRAY(JAVA_OBJECT instanceObject, JAVA_OBJECT dir, JAVA_OBJECT files) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(dir);
    NSString* ns = [NSString stringWithUTF8String:chrs];
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
        JAVA_OBJECT str = fromNSString(currentString);
        data[iter] = str;
    }
#ifndef CN1_USE_ARC
    [fm release];
#endif
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_createDirectory___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT dir) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(dir);
    NSString* ns = [NSString stringWithUTF8String:chrs];
    [fm createDirectoryAtPath:ns attributes:nil];
#ifndef CN1_USE_ARC
    [fm release];
#endif
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_moveFile___java_lang_String_java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dest) {
    POOL_BEGIN();
    NSFileManager* fm = [[NSFileManager alloc] init];
    const char* chrs = stringToUTF8(src);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    const char* chrsDest = stringToUTF8(dest);
    NSString* nsDst = [NSString stringWithUTF8String:chrsDest];
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


void com_codename1_impl_ios_IOSNative_setImageName___long_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG nativeImage, JAVA_OBJECT name) {
    POOL_BEGIN();
    const char* chrs = stringToUTF8(name);
    Java_com_codename1_impl_ios_IOSImplementation_setImageName(nativeImage, chrs);
    POOL_END();    
}

JAVA_LONG com_codename1_impl_ios_IOSNative_openConnection___java_lang_String_int(JAVA_OBJECT instanceObject, JAVA_OBJECT url, JAVA_INT timeout) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = [[NetworkConnectionImpl alloc] init];
    const char* chrs = stringToUTF8(url);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    void* response = [impl openConnection:nsSrc timeout:timeout];
    POOL_END();    
    return response;
}

void com_codename1_impl_ios_IOSNative_connect___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    [impl connect];
    POOL_END();    
}

void com_codename1_impl_ios_IOSNative_setMethod___long_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT mtd) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    const char* chrs = stringToUTF8(mtd);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    [impl setMethod:nsSrc];
    POOL_END();    
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResponseCode___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    return [impl getResponseCode];
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseMessage___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    JAVA_OBJECT str = fromNSString([impl getResponseMessage]);
    POOL_END();    
    return str;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getContentLength___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    return [impl getContentLength];
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseHeader___long_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT name) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    const char* chrs = stringToUTF8(name);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    JAVA_OBJECT str = fromNSString([impl getResponseHeader:nsSrc]);
    POOL_END();    
    return str;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResponseHeaderCount___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    JAVA_INT i = [impl getResponseHeaderCount]; 
    POOL_END();    
    return i;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseHeaderName___long_int(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    JAVA_OBJECT j = fromNSString([impl getResponseHeaderName:offset]);
    POOL_END();    
    return j;
}

void com_codename1_impl_ios_IOSNative_addHeader___long_java_lang_String_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT key, JAVA_OBJECT value) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    const char* chrs = stringToUTF8(key);
    NSString* nsKey = [NSString stringWithUTF8String:chrs];
    chrs = stringToUTF8(value);
    NSString* nsValue = [NSString stringWithUTF8String:chrs];
    [impl addHeader:nsKey value:nsValue];
    POOL_END();    
}

void com_codename1_impl_ios_IOSNative_setBody___long_byte_1ARRAY(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT arr) {
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

void connectionComplete(void* peer) {
    com_codename1_impl_ios_IOSImplementation_streamComplete___long(peer);
}

void connectionReceivedData(void* peer, NSData* data) {
#ifndef NEW_CODENAME_ONE_VM
    if (!__TIB_byte.classInitialized) __INIT_byte();
    org_xmlvm_runtime_XMLVMArray* byteArray = XMLVMArray_createSingleDimension(__CLASS_byte, [data length]);
    [data getBytes:byteArray->fields.org_xmlvm_runtime_XMLVMArray.array_];
#else 
    JAVA_OBJECT byteArray = __NEW_ARRAY_JAVA_BYTE([data length]);
    [data getBytes:((JAVA_ARRAY)byteArray)->data];
#endif
    com_codename1_impl_ios_IOSImplementation_appendData___long_byte_1ARRAY(peer, byteArray);
}

void connectionError(void* peer, NSString* message) {
    POOL_BEGIN();
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
    JAVA_OBJECT str = fromNSString(message);
    com_codename1_impl_ios_IOSImplementation_networkError___long_java_lang_String(peer, str);
    POOL_END();    
}


void com_codename1_impl_ios_IOSNative_closeConnection___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    NetworkConnectionImpl* impl = (BRIDGE_CAST NetworkConnectionImpl*)((void *)peer);
#ifndef CN1_USE_ARC
    [impl release];
#endif
}

void com_codename1_impl_ios_IOSNative_execute___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT n1)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSString* ns = toNSString(n1);
        if([ns hasPrefix:@"file:"]) {
            ns = [ns substringFromIndex:5];
            UIDocumentInteractionController* preview = [UIDocumentInteractionController interactionControllerWithURL:[NSURL fileURLWithPath:ns]];
            [preview presentPreviewAnimated:YES];
        } else {
            [[UIApplication sharedApplication] openURL:[NSURL URLWithString:ns]];
        }
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_flashBacklight___int(JAVA_OBJECT instanceObject, JAVA_INT n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_flashBacklight___int]
    //XMLVM_END_WRAPPER
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isMinimized__(JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_isMinimized__]
    return false;
    //XMLVM_END_WRAPPER
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_minimizeApplication__(JAVA_OBJECT instanceObject)
{
    return 0;
}

void com_codename1_impl_ios_IOSNative_restoreMinimizedApplication__(JAVA_OBJECT instanceObject)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_restoreMinimizedApplication__]
    //XMLVM_END_WRAPPER
}

extern int orientationLock;
void com_codename1_impl_ios_IOSNative_lockOrientation___boolean(JAVA_OBJECT instanceObject, JAVA_BOOLEAN n1)
{
    //XMLVM_BEGIN_WRAPPER[com_codename1_impl_ios_IOSNative_lockOrientation___boolean]
    if(n1) {
        orientationLock = 1;
    } else {
        orientationLock = 2;
    }
    //XMLVM_END_WRAPPER
}

void com_codename1_impl_ios_IOSNative_unlockOrientation__(JAVA_OBJECT instanceObject)
{
    orientationLock = 0;
}

void com_codename1_impl_ios_IOSNative_lockScreen__(JAVA_OBJECT instanceObject)
{
    [UIApplication sharedApplication].idleTimerDisabled = YES;
}

void com_codename1_impl_ios_IOSNative_unlockScreen__(JAVA_OBJECT instanceObject)
{
    [UIApplication sharedApplication].idleTimerDisabled = NO;
}

extern void vibrateDevice();
void com_codename1_impl_ios_IOSNative_vibrate___int(JAVA_OBJECT instanceObject, JAVA_INT duration) {
    vibrateDevice();
}

// Peer Component methods

void com_codename1_impl_ios_IOSNative_calcPreferredSize___long_int_int_int_1ARRAY(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT w, JAVA_INT h, JAVA_OBJECT response) {
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

void com_codename1_impl_ios_IOSNative_updatePeerPositionSize___long_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
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

void com_codename1_impl_ios_IOSNative_peerSetVisible___long_boolean(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN b) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIView* v = (BRIDGE_CAST UIView*)((void *)peer);
        if(!b) {
            if([v superview] != nil) {
                [v removeFromSuperview];
            }
        } else {
            if([v superview] == nil) {
                [[CodenameOne_GLViewController instance].view addSubview:v];
            }
        }
        POOL_END();    
    });
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createPeerImage___long_int_1ARRAY(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT arr) {
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

void com_codename1_impl_ios_IOSNative_peerInitialized___long_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_LONG peer, int x, int y, int w, int h) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIView* v = (BRIDGE_CAST UIView*)((void *)peer);
        if([v superview] == nil) {
            [[CodenameOne_GLViewController instance].view addSubview:v];
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

extern JAVA_OBJECT com_codename1_ui_Display_getInstance__();
void repaintUI() {
    JAVA_OBJECT d = com_codename1_ui_Display_getInstance__();
    if(d != nil) {
#ifndef NEW_CODENAME_ONE_VM
        com_codename1_ui_Form* f = (com_codename1_ui_Form*)com_codename1_ui_Display_getCurrent__(d);
        if(f != nil) {
            com_codename1_ui_Component_repaint__(f);
        }
#else
        JAVA_OBJECT f = com_codename1_ui_Display_getCurrent___R_com_codename1_ui_Form(d);
        if(f != nil) {
            com_codename1_ui_Component_repaint__(f);
        }
#endif
    }
}

void com_codename1_impl_ios_IOSNative_peerDeinitialized___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
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
JAVA_INT com_codename1_impl_ios_IOSNative_getAudioDuration___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        com_codename1_impl_ios_IOSNative_getAudioDuration = [pl getAudioDuration];
        POOL_END();
    });
    return com_codename1_impl_ios_IOSNative_getAudioDuration;
}

void com_codename1_impl_ios_IOSNative_playAudio___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        [pl playAudio];
        POOL_END();
    });
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAudioPlaying = 0;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAudioPlaying___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        com_codename1_impl_ios_IOSNative_isAudioPlaying = [pl isPlaying];
        POOL_END();
    });
    return com_codename1_impl_ios_IOSNative_isAudioPlaying;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getAudioTime = 0;
JAVA_INT com_codename1_impl_ios_IOSNative_getAudioTime___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        com_codename1_impl_ios_IOSNative_getAudioTime = [pl getAudioTime];
        POOL_END();
    });
    return com_codename1_impl_ios_IOSNative_getAudioTime;
}

void com_codename1_impl_ios_IOSNative_pauseAudio___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        [pl pauseAudio];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setAudioTime___long_int(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT time) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        [pl setAudioTime:time];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_cleanupAudio___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        AudioPlayer* pl = (BRIDGE_CAST AudioPlayer*)((void *)peer);
        if([pl isPlaying]) {
            return;
        }
#ifndef CN1_USE_ARC
        [pl release];
#endif
        POOL_END();
    });
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio = 0;
JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___java_lang_String_java_lang_Runnable(JAVA_OBJECT instanceObject, JAVA_OBJECT uri, JAVA_OBJECT onCompletion) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        const char* chrs = stringToUTF8(uri);
        NSString* ns = [NSString stringWithUTF8String:chrs];
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

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___byte_1ARRAY_java_lang_Runnable(JAVA_OBJECT instanceObject, JAVA_OBJECT b, JAVA_OBJECT onCompletion) {
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

JAVA_FLOAT com_codename1_impl_ios_IOSNative_getVolume__(JAVA_OBJECT instanceObject) {
    return [AudioPlayer getVolume];    
}

void com_codename1_impl_ios_IOSNative_setVolume___float(JAVA_OBJECT instanceObject, JAVA_FLOAT vol) {
    [AudioPlayer setVolume:vol];    
}

void com_codename1_impl_ios_IOSNative_fillRectRadialGradientGlobal___int_int_int_int_int_int_float_float_float(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_FLOAT n7, JAVA_FLOAT n8, JAVA_FLOAT n9) {
    POOL_BEGIN();
    DrawGradient* d = [[DrawGradient alloc] initWithArgs:1 startColorA:n1 endColorA:n2 xA:n3 yA:n4 widthA:n5 heightA:n6 relativeXA:n7 relativeYA:n8 relativeSizeA:n9];
    [CodenameOne_GLViewController upcoming:d];
#ifndef CN1_USE_ARC
    [d release];
#endif
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_fillLinearGradientGlobal___int_int_int_int_int_int_boolean(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT n5, JAVA_INT n6, JAVA_BOOLEAN n7) {
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

void com_codename1_impl_ios_IOSNative_fillRectRadialGradientMutable___int_int_int_int_int_int_float_float_float(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT width, JAVA_INT height, JAVA_FLOAT relativeX, JAVA_FLOAT relativeY, JAVA_FLOAT relativeSize) {
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

void com_codename1_impl_ios_IOSNative_fillLinearGradientMutable___int_int_int_int_int_int_boolean(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3, JAVA_INT n4, JAVA_INT width, JAVA_INT height, JAVA_BOOLEAN n7) {
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
void com_codename1_impl_ios_IOSNative_releasePeer___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifndef CN1_USE_ARC
    dispatch_async(dispatch_get_main_queue(), ^{
        NSObject* o = (NSObject*)peer;
        [o release];
    });
#endif
}

void com_codename1_impl_ios_IOSNative_retainPeer___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
#ifndef CN1_USE_ARC
    dispatch_sync(dispatch_get_main_queue(), ^{
        NSObject* o = (NSObject*)peer;
        [o retain];
    });
#endif
}

UIWebView* com_codename1_impl_ios_IOSNative_createBrowserComponent = nil;
JAVA_LONG com_codename1_impl_ios_IOSNative_createBrowserComponent___java_lang_Object(JAVA_OBJECT instanceObject, JAVA_OBJECT obj) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        com_codename1_impl_ios_IOSNative_createBrowserComponent = [[UIWebView alloc] initWithFrame:CGRectMake(3000, 0, 200, 200)];
        com_codename1_impl_ios_IOSNative_createBrowserComponent.backgroundColor = [UIColor whiteColor];
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

void com_codename1_impl_ios_IOSNative_setBrowserPage___long_java_lang_String_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT html, JAVA_OBJECT baseUrl) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        [w loadHTMLString:toNSString(html) baseURL:[NSURL URLWithString:toNSString(baseUrl)]];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setBrowserUserAgent___long_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT ua) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        //UIWebView* w = (UIWebView*)peer;
        NSDictionary *dictionary = [NSDictionary dictionaryWithObjectsAndKeys:toNSString(ua), @"UserAgent", nil];
        [[NSUserDefaults standardUserDefaults] registerDefaults:dictionary];        
        POOL_END();
    });
}


void com_codename1_impl_ios_IOSNative_setPinchToZoomEnabled___long_boolean(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN enabled) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        
        w.scalesPageToFit=enabled;
        
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setNativeBrowserScrollingEnabled___long_boolean(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN enabled) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        
        w.scrollView.scrollEnabled = NO;
        w.scrollView.bounces = NO;
        
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setBrowserURL___long_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT url) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        NSString *str = toNSString(url);
        NSURL* nu = [NSURL URLWithString:str];
        NSURLRequest* r = [NSURLRequest requestWithURL:nu];
        [w loadRequest:r];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_browserBack___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        [w goBack];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_browserStop___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        [w stopLoading];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_browserClearHistory___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
}

void com_codename1_impl_ios_IOSNative_browserExecute___long_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT javaScript) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        [w stringByEvaluatingJavaScriptFromString:toNSString(javaScript)];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_browserForward___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        [w goForward];
        POOL_END();
    });
}

JAVA_BOOLEAN booleanResponse = 0;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_browserHasBack___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        booleanResponse = [w canGoBack];
        POOL_END();
    });
    return booleanResponse;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_browserHasForward___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        booleanResponse = [w canGoForward];
        POOL_END();
    });
    return booleanResponse;
}

void com_codename1_impl_ios_IOSNative_browserReload___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        [w reload];
        POOL_END();
    });
}

JAVA_OBJECT returnString;
JAVA_OBJECT com_codename1_impl_ios_IOSNative_getBrowserTitle___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        NSString* theTitle = [w stringByEvaluatingJavaScriptFromString:@"document.title"];
        returnString = fromNSString(theTitle);
        POOL_END();
    });
    return returnString;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getBrowserURL___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        returnString = fromNSString(w.request.URL.absoluteString);
        POOL_END();
    });
    return returnString;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponent___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT str) {
    __block MPMoviePlayerController* moviePlayerInstance;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSURL* u = [NSURL URLWithString:toNSString(str)];
        moviePlayerInstance = [[MPMoviePlayerController alloc] initWithContentURL:u];
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponent___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT str) {
    __block MPMoviePlayerViewController* moviePlayerInstance;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        NSURL* u = [NSURL URLWithString:toNSString(str)];
        moviePlayerInstance = [[MPMoviePlayerViewController alloc] initWithContentURL:u];
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponent___byte_1ARRAY(JAVA_OBJECT instanceObject, JAVA_OBJECT dataObject) {
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
        moviePlayerInstance.useApplicationAudioSession = NO;
#ifndef CN1_USE_ARC
        [moviePlayerInstance retain];
#endif
        [moviePlayerInstance prepareToPlay];
        [moviePlayerInstance play];
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponent___byte_1ARRAY(JAVA_OBJECT instanceObject, JAVA_OBJECT dataObject) {
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
#ifndef CN1_USE_ARC
        [moviePlayerInstance retain];
#endif
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponentNSData___long(JAVA_OBJECT instanceObject, JAVA_LONG nsData) {
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
        moviePlayerInstance.useApplicationAudioSession = NO;
#ifndef CN1_USE_ARC
        [moviePlayerInstance retain];
#endif
        [moviePlayerInstance prepareToPlay];
        [moviePlayerInstance play];
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponentNSData___long(JAVA_OBJECT instanceObject, JAVA_LONG nsData) {
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
#ifndef CN1_USE_ARC
        [moviePlayerInstance retain];
#endif
        POOL_END();
    });
    return (JAVA_LONG)((BRIDGE_CAST void*)moviePlayerInstance);
}

void com_codename1_impl_ios_IOSNative_sendEmailMessage___java_lang_String_1ARRAY_java_lang_String_java_lang_String_java_lang_String_1ARRAY_java_lang_String_1ARRAY_boolean(JAVA_OBJECT instanceObject,
    JAVA_OBJECT  recipients, JAVA_OBJECT  subject, JAVA_OBJECT content, JAVA_OBJECT attachment, JAVA_OBJECT attachmentMimeType, JAVA_BOOLEAN htmlMail) {
    dispatch_async(dispatch_get_main_queue(), ^{
        MFMailComposeViewController *picker = [[MFMailComposeViewController alloc] init];
        if(picker == nil || ![MFMailComposeViewController canSendMail]) {
#ifndef CN1_USE_ARC
            [picker release];
#endif
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
            [recipientsArray addObject:toNSString(data[iter])];
        }
        
        [picker setToRecipients:recipientsArray];
        
        // Subject.
        [picker setSubject:toNSString(subject)];
        
        // Body.
        NSString *emailBody = toNSString(content);
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
                NSString* file = toNSString(attachmentData[iter]);
                NSString* mime = toNSString(mimeData[iter]);

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
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_startVideoComponent___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        [m play];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_stopVideoComponent___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        [m stop];
        POOL_END();
    });
}

JAVA_INT com_codename1_impl_ios_IOSNative_getMediaTimeMS___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    // unsupported by API for some reason???
    return 0;
}

JAVA_INT com_codename1_impl_ios_IOSNative_setMediaTimeMS___long_int(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT time) {
    // unsupported by API for some reason???
    return 0;
}

int responseGetMediaDuration = 0;
JAVA_INT com_codename1_impl_ios_IOSNative_getMediaDuration___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        responseGetMediaDuration = (int)m.duration * 1000;
        POOL_END();
    });
    return responseGetMediaDuration;
}

void com_codename1_impl_ios_IOSNative_setMediaBgArtist___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT artist) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if ([MPNowPlayingInfoCenter class])  {
            NSArray *keys = [NSArray arrayWithObjects:
                             MPMediaItemPropertyArtist,
                             MPNowPlayingInfoPropertyPlaybackRate,
                             nil];
            NSArray *values = [NSArray arrayWithObjects:
                               toNSString(artist),
                               [NSNumber numberWithInt:1],
                               nil];
            NSDictionary *mediaInfo = [NSDictionary dictionaryWithObjects:values forKeys:keys];
            [[MPNowPlayingInfoCenter defaultCenter] setNowPlayingInfo:mediaInfo];
        }
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setMediaBgTitle___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT title) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if ([MPNowPlayingInfoCenter class])  {
            NSArray *keys = [NSArray arrayWithObjects:
                             MPMediaItemPropertyTitle,
                             MPNowPlayingInfoPropertyPlaybackRate,
                             nil];
            NSArray *values = [NSArray arrayWithObjects:
                               toNSString(title),
                               [NSNumber numberWithInt:1],
                               nil];
            NSDictionary *mediaInfo = [NSDictionary dictionaryWithObjects:values forKeys:keys];
            [[MPNowPlayingInfoCenter defaultCenter] setNowPlayingInfo:mediaInfo];
        }
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_setMediaBgDuration___long(JAVA_OBJECT instanceObject, JAVA_LONG dur) {
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

void com_codename1_impl_ios_IOSNative_setMediaBgPosition___long(JAVA_OBJECT instanceObject, JAVA_LONG pos) {
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

void com_codename1_impl_ios_IOSNative_setMediaBgAlbumCover___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if ([MPNowPlayingInfoCenter class])  {
            GLUIImage* glll = (BRIDGE_CAST GLUIImage*)((void *)peer);
            UIImage* i = [glll getImage];        
            NSArray *keys = [NSArray arrayWithObjects:
                             MPMediaItemPropertyArtwork,
                             MPNowPlayingInfoPropertyPlaybackRate,
                             nil];
            NSArray *values = [NSArray arrayWithObjects:
                               i,
                               [NSNumber numberWithInt:1],
                               nil];
            NSDictionary *mediaInfo = [NSDictionary dictionaryWithObjects:values forKeys:keys];
            [[MPNowPlayingInfoCenter defaultCenter] setNowPlayingInfo:mediaInfo];
        }
        POOL_END();
    });
}


JAVA_BOOLEAN responseIsVideoPlaying = 0;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoPlaying___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        responseIsVideoPlaying = m.playbackState == MPMoviePlaybackStatePlaying;
        POOL_END();
    });
    return responseIsVideoPlaying;
}

void com_codename1_impl_ios_IOSNative_setVideoFullScreen___long_boolean(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_BOOLEAN fullscreen) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        [m setFullscreen:fullscreen];
        POOL_END();
    });
}

JAVA_BOOLEAN responseIsVideoFullScreen = 0;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoFullScreen___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    responseIsVideoFullScreen = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
        responseIsVideoFullScreen = [m isFullscreen];
        POOL_END();
    });
    return responseIsVideoFullScreen;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getVideoViewPeer___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    MPMoviePlayerController* m = (BRIDGE_CAST MPMoviePlayerController*) ((void *)peer);
    return (JAVA_LONG)((BRIDGE_CAST void*)m.view);
}

void com_codename1_impl_ios_IOSNative_showNativePlayerController___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        MPMoviePlayerViewController* m = (BRIDGE_CAST MPMoviePlayerViewController*) ((void *)peer);
        m.moviePlayer.shouldAutoplay = NO;
        [[CodenameOne_GLViewController instance] presentMoviePlayerViewControllerAnimated:m];
        POOL_END();
    });
}


CLLocationManager* com_codename1_impl_ios_IOSNative_createCLLocation = nil;
JAVA_LONG com_codename1_impl_ios_IOSNative_createCLLocation__(JAVA_OBJECT instanceObject) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        com_codename1_impl_ios_IOSNative_createCLLocation = [[CLLocationManager alloc] init];
    });
    CLLocationManager* c = com_codename1_impl_ios_IOSNative_createCLLocation;
    com_codename1_impl_ios_IOSNative_createCLLocation = nil;
    return (JAVA_LONG)((BRIDGE_CAST void*)c);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getCurrentLocationObject___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
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

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationLatitude___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.coordinate.latitude;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationAltitude___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.altitude;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationLongtitude___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.coordinate.longitude;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationAccuracy___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.horizontalAccuracy;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationDirection___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.course;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationVelocity___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    return loc.speed;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getLocationTimeStamp___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocation* loc = (BRIDGE_CAST CLLocation*)((void *)peer);
    NSTimeInterval t = [loc.timestamp timeIntervalSince1970];
    return (JAVA_LONG)(t * 1000.0);
}

UIPopoverController* popoverController;
void com_codename1_impl_ios_IOSNative_captureCamera___boolean(JAVA_OBJECT instanceObject, JAVA_BOOLEAN movie) {
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

void com_codename1_impl_ios_IOSNative_openImageGallery__(JAVA_OBJECT instanceObject) {
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

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUDID__(JAVA_OBJECT instanceObject) {
    return fromNSString([OpenUDID value]);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getOSVersion__(JAVA_OBJECT instanceObject) {
    return fromNSString([[UIDevice currentDevice] systemVersion]);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isGoodLocation___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
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

void com_codename1_impl_ios_IOSNative_startUpdatingLocation___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
    l.delegate = [CodenameOne_GLViewController instance];
    [l startUpdatingLocation];
}

void com_codename1_impl_ios_IOSNative_stopUpdatingLocation___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    CLLocationManager* l = (BRIDGE_CAST CLLocationManager*)((void *)peer);
    [l stopUpdatingLocation];    
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

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isContactsPermissionGranted__(JAVA_OBJECT instanceObject) {
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

JAVA_OBJECT com_codename1_impl_ios_IOSNative_createContact___java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT firstName, JAVA_OBJECT surname, JAVA_OBJECT officePhone, JAVA_OBJECT homePhone, JAVA_OBJECT cellPhone, JAVA_OBJECT email) {
    POOL_BEGIN();
    
    ABAddressBookRef addressBook = getAddressBook();
    if(!grantedPermission) {
        return JAVA_NULL;
    }
    CFErrorRef  error = nil;
    
    ABRecordRef person = ABPersonCreate();
    if(firstName != nil) {
        ABRecordSetValue(person, kABPersonFirstNameProperty, (BRIDGE_CAST CFStringRef)toNSString(firstName), NULL);
    }
    if(surname != nil) {
        ABRecordSetValue(person, kABPersonLastNameProperty, (BRIDGE_CAST CFStringRef)toNSString(surname), NULL);
    }
    
    if(email != nil) {
        ABMutableMultiValueRef emailVal = ABMultiValueCreateMutable(kABMultiStringPropertyType);
        ABMultiValueAddValueAndLabel(emailVal, (BRIDGE_CAST CFStringRef)toNSString(email), CFSTR("email"), NULL);
        ABRecordSetValue(person, kABPersonEmailProperty, emailVal, &error);
        throwError(error);
    }

    if(officePhone != nil || homePhone != nil || cellPhone != nil) {
        ABMutableMultiValueRef phoneVal = ABMultiValueCreateMutable(kABPersonPhoneProperty);
        if(officePhone != nil) {
            ABMultiValueAddValueAndLabel(phoneVal, (BRIDGE_CAST CFStringRef)toNSString(officePhone), kABWorkLabel, NULL);
        }
        if(homePhone != nil) {
            ABMultiValueAddValueAndLabel(phoneVal, (BRIDGE_CAST CFStringRef)toNSString(homePhone), kABHomeLabel, NULL);
        }
        if(cellPhone != nil) {
            ABMultiValueAddValueAndLabel(phoneVal, (BRIDGE_CAST CFStringRef)toNSString(cellPhone), kABPersonPhoneMobileLabel, NULL);
        }
        ABRecordSetValue(person, kABPersonPhoneProperty, phoneVal, &error);
        throwError(error);
    }
    ABAddressBookAddRecord(addressBook, person, &error);
    throwError(error);
    ABAddressBookSave(addressBook, &error);
    throwError(error);
    JAVA_OBJECT o = fromNSString([NSString stringWithFormat:@"%i", ABRecordGetRecordID(person)]);
    POOL_END();
    return o;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_deleteContact___int(JAVA_OBJECT instanceObject, JAVA_INT i) {
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


JAVA_INT com_codename1_impl_ios_IOSNative_getContactCount___boolean(JAVA_OBJECT instanceObject, JAVA_BOOLEAN includeNumbers) {
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

void com_codename1_impl_ios_IOSNative_getContactRefIds___int_1ARRAY_boolean(JAVA_OBJECT instanceObject, JAVA_OBJECT intArray, JAVA_BOOLEAN includeNumbers) {
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
    if(includeNumbers) {
        CFIndex nPeople = ABAddressBookGetPersonCount(addressBook);
        int responseCount = 0;
        for(int iter = 0 ; iter < nPeople ; iter++) {
            ABRecordRef ref = CFArrayGetValueAtIndex(allPeople, iter);
            ABMultiValueRef numbers = ABRecordCopyValue(ref, kABPersonPhoneProperty);
            
            if(numbers != nil && ABMultiValueGetCount(numbers) > 0) {
                responseCount++;
                data[responseCount] = ABRecordGetRecordID(ref);
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

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonFirstName___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    NSString* k = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonFirstNameProperty);    
    JAVA_OBJECT ret = fromNSString(k);
    POOL_END();
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonSurnameName___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    NSString* k = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonLastNameProperty);    
    JAVA_OBJECT ret = fromNSString(k);
    POOL_END();
    return ret;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getPersonPhoneCount___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    //POOL_BEGIN();
    //POOL_END();
    return 1;
}

JAVA_OBJECT copyValueAsString(ABMultiValueRef r) {
    JAVA_OBJECT ret = JAVA_NULL;
    if(ABMultiValueGetCount(r) > 0) {
        NSString *k = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(r, 0);
        ret = fromNSString(k);
    }
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPhone___long_int(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    ABMultiValueRef k = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonPhoneProperty);    
    JAVA_OBJECT ret = copyValueAsString(k);
    POOL_END();
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPhoneType___long_int(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
    POOL_BEGIN();
    //ABRecordRef i = (ABRecordRef)peer;
    //ABMultiValueRef k = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonPhoneMainLabel);    
    //JAVA_OBJECT ret = copyValueAsString(k);
    JAVA_OBJECT ret = fromNSString(@"work");
    POOL_END();
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPrimaryPhone___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    ABMultiValueRef k = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonPhoneProperty);    
    JAVA_OBJECT ret = copyValueAsString(k);
    POOL_END();
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonEmail___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    ABMultiValueRef emails = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonEmailProperty);    
    JAVA_OBJECT ret = copyValueAsString(emails);
    POOL_END();
    return ret;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonAddress___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    POOL_BEGIN();
    ABRecordRef i = (ABRecordRef)peer;
    NSString* k = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonAddressProperty);    
    JAVA_OBJECT ret = fromNSString(k);
    POOL_END();
    return ret;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createPersonPhotoImage___long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
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

void addToHashtable(JAVA_OBJECT hash, ABMultiValueRef ref, int count) {
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
            java_util_Hashtable_put___java_lang_Object_java_lang_Object_R_java_lang_Object(hash, fromNSString(@""), fromNSString(value));
        } else {
            NSString *value = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(ref, iter);
            java_util_Hashtable_put___java_lang_Object_java_lang_Object_R_java_lang_Object(hash, fromNSString(key), fromNSString(value));
        }
#endif
    }
}

void com_codename1_impl_ios_IOSNative_updatePersonWithRecordID___int_com_codename1_contacts_Contact_boolean_boolean_boolean_boolean_boolean(JAVA_OBJECT instanceObject, JAVA_INT recId, JAVA_OBJECT cnt, 
            JAVA_BOOLEAN includesFullName, JAVA_BOOLEAN includesPicture, JAVA_BOOLEAN includesNumbers, JAVA_BOOLEAN includesEmail, JAVA_BOOLEAN includeAddress) {
    POOL_BEGIN();
    ABRecordRef i = ABAddressBookGetPersonWithRecordID(getAddressBook(), recId);
    if(includesEmail) {
        ABMultiValueRef emails = (ABMultiValueRef)ABRecordCopyValue(i,kABPersonEmailProperty);
        int emailCount = ABMultiValueGetCount(emails);
        if(emailCount > 0) {
            NSString *k = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(emails, 0);
            com_codename1_contacts_Contact_setPrimaryEmail___java_lang_String(cnt, fromNSString(k));
#ifndef NEW_CODENAME_ONE_VM
            JAVA_OBJECT emailsHash = com_codename1_contacts_Contact_getEmails__(cnt);
#else
            JAVA_OBJECT emailsHash = com_codename1_contacts_Contact_getEmails___R_java_util_Hashtable(cnt);
#endif
            addToHashtable(emailsHash, emails, emailCount);
        }
    }

    if(includesNumbers) {
        ABMultiValueRef numbers = ABRecordCopyValue(i, kABPersonPhoneProperty);
        int numbersCount = ABMultiValueGetCount(numbers);
        if(numbersCount > 0) {
            NSString *k = (BRIDGE_CAST NSString *)ABMultiValueCopyValueAtIndex(numbers, 0);
            com_codename1_contacts_Contact_setPrimaryPhoneNumber___java_lang_String(cnt, fromNSString(k));
#ifndef NEW_CODENAME_ONE_VM
            JAVA_OBJECT hash = com_codename1_contacts_Contact_getPhoneNumbers__(cnt);
#else
            JAVA_OBJECT hash = com_codename1_contacts_Contact_getPhoneNumbers___R_java_util_Hashtable(cnt);
#endif
            addToHashtable(hash, numbers, numbersCount);
        }
    }
    
    NSString* first = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonFirstNameProperty);
    if(first != nil) {
        com_codename1_contacts_Contact_setFirstName___java_lang_String(cnt, fromNSString(first));
    }

    NSString* last = (BRIDGE_CAST NSString*)ABRecordCopyValue(i,kABPersonLastNameProperty);
    if(last != nil) {
        com_codename1_contacts_Contact_setFamilyName___java_lang_String(cnt, fromNSString(last));
    }

    if(includesFullName) {
        NSString* full = [NSString stringWithFormat:@"%@ %@", first, last];
        if(first != nil && last != nil) {
            com_codename1_contacts_Contact_setDisplayName___java_lang_String(cnt, fromNSString(full));
        } else {
            if(first != nil) {
                com_codename1_contacts_Contact_setDisplayName___java_lang_String(cnt, fromNSString(first));
            } else {
                if(last != nil) {
                    com_codename1_contacts_Contact_setDisplayName___java_lang_String(cnt, fromNSString(last));
                }
            }
        }
    }
    
    NSString* note = (BRIDGE_CAST NSString*)ABRecordCopyValue(i, kABPersonNoteProperty);
    if(note != nil) {
        com_codename1_contacts_Contact_setNote___java_lang_String(cnt, fromNSString(note));
    }

    NSDate *bDayProperty = (BRIDGE_CAST NSDate*)ABRecordCopyValue(i, kABPersonBirthdayProperty);
    if(bDayProperty != nil) {
        NSTimeInterval nst = [bDayProperty timeIntervalSince1970];
        com_codename1_contacts_Contact_setBirthday___long(cnt, nst * 1000);
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
#else
            struct obj__com_codename1_impl_ios_IOSImplementation_NativeImage* nativeImage = (struct obj__com_codename1_impl_ios_IOSImplementation_NativeImage*)__NEW_com_codename1_impl_ios_IOSImplementation_NativeImage();
            (*nativeImage).com_codename1_impl_ios_IOSImplementation_NativeImage_peer = g;
            (*nativeImage).com_codename1_impl_ios_IOSImplementation_NativeImage_width = (int)[g getImage].size.width;
            (*nativeImage).com_codename1_impl_ios_IOSImplementation_NativeImage_height = (int)[g getImage].size.height;
            JAVA_OBJECT image = com_codename1_ui_Image_createImage___java_lang_Object_R_com_codename1_ui_Image(nativeImage);
#endif

            com_codename1_contacts_Contact_setPhoto___com_codename1_ui_Image(cnt, image);
        }
    }
    
    POOL_END();
}
    
JAVA_LONG com_codename1_impl_ios_IOSNative_getPersonWithRecordID___int(JAVA_OBJECT instanceObject, JAVA_INT recId) {
    POOL_BEGIN();
    ABRecordRef i = ABAddressBookGetPersonWithRecordID(getAddressBook(), recId);
#ifndef CN1_USE_ARC
    [i retain];
#endif
    POOL_END();
    return (JAVA_LONG)i;
}

void com_codename1_impl_ios_IOSNative_dial___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT phone) {
    POOL_BEGIN();
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:toNSString(phone)]];
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_sendSMS___java_lang_String_java_lang_String(JAVA_OBJECT instanceObject, 
    JAVA_OBJECT  number, JAVA_OBJECT  text) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        if([MFMessageComposeViewController canSendText]) {
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
            
#ifndef CN1_USE_ARC
            [picker release];
#endif
        }
        POOL_END();
    });
}



void com_codename1_impl_ios_IOSNative_registerPush__(JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_CN1_PUSH2
    dispatch_async(dispatch_get_main_queue(), ^{
        [[UIApplication sharedApplication] registerForRemoteNotificationTypes:
                    (UIRemoteNotificationTypeBadge | UIRemoteNotificationTypeSound | UIRemoteNotificationTypeAlert)];
    });
#endif
}

void com_codename1_impl_ios_IOSNative_deregisterPush__(JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_CN1_PUSH2
    dispatch_async(dispatch_get_main_queue(), ^{
        [[UIApplication sharedApplication] unregisterForRemoteNotifications];
    });
#endif
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

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageFile___long_boolean_int_int_float(JAVA_OBJECT instanceObject, JAVA_LONG imagePeer, JAVA_BOOLEAN jpeg, int width, int height, JAVA_FLOAT quality) {
    __block int blockWidth = width;
    __block int blockHeight = height;
    __block NSData* data = nil;
#ifndef CN1_USE_ARC
    [imagePeer retain];
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
    [imagePeer release];
#endif
    return (JAVA_LONG)((BRIDGE_CAST void*)data);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getNSDataSize___long(JAVA_OBJECT instanceObject, JAVA_LONG nsData) {
    NSData* d = (BRIDGE_CAST NSData*)((void *)nsData);
    return d.length;
} 

void com_codename1_impl_ios_IOSNative_nsDataToByteArray___long_byte_1ARRAY(JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_OBJECT dataArray) {
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


JAVA_LONG com_codename1_impl_ios_IOSNative_createAudioRecorder___java_lang_String(JAVA_OBJECT instanceObject,
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
                    NSString * filePath = toNSString(destinationFile);
                    
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
            NSString * filePath = toNSString(destinationFile);
            
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

void com_codename1_impl_ios_IOSNative_startAudioRecord___long(JAVA_OBJECT instanceObject, 
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

void com_codename1_impl_ios_IOSNative_pauseAudioRecord___long(JAVA_OBJECT instanceObject, 
    JAVA_LONG  peer) {
    AVAudioRecorder* recorder = (BRIDGE_CAST AVAudioRecorder*)((void *)peer);
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        [recorder pause];
        POOL_END();
    });
}

void com_codename1_impl_ios_IOSNative_cleanupAudioRecord___long(JAVA_OBJECT instanceObject, 
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
JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofObjArrayI___java_lang_Object_R_boolean(JAVA_OBJECT n1)
{
    return n1->__codenameOneParentClsReference->isArray;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofByteArrayI___java_lang_Object_R_boolean(JAVA_OBJECT n1)
{    
    return n1->__codenameOneParentClsReference->classId == cn1_array_1_id_JAVA_BYTE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofShortArrayI___java_lang_Object_R_boolean(JAVA_OBJECT n1)
{
    return n1->__codenameOneParentClsReference->classId == cn1_array_1_id_JAVA_SHORT;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofLongArrayI___java_lang_Object_R_boolean(JAVA_OBJECT n1)
{
    return n1->__codenameOneParentClsReference->classId == cn1_array_1_id_JAVA_LONG;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofIntArrayI___java_lang_Object_R_boolean(JAVA_OBJECT n1)
{
    return n1->__codenameOneParentClsReference->classId == cn1_array_1_id_JAVA_INT;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofFloatArrayI___java_lang_Object_R_boolean(JAVA_OBJECT n1)
{
    return n1->__codenameOneParentClsReference->classId == cn1_array_1_id_JAVA_FLOAT;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSImplementation_instanceofDoubleArrayI___java_lang_Object_R_boolean(JAVA_OBJECT n1)
{
    return n1->__codenameOneParentClsReference->classId == cn1_array_1_id_JAVA_DOUBLE;
}
#else // NEW_CODENAME_ONE_VM
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
#endif // NEW_CODENAME_ONE_VM

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlDbExists___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    POOL_BEGIN();
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];    
    
    const char* chrs = stringToUTF8(name);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    
    NSString* foofile = [documentsPath stringByAppendingPathComponent:nsSrc];
    BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:foofile];
    POOL_END();    
    return fileExists;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlDbCreateAndOpen___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    POOL_BEGIN();
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];    
    
    const char* chrs = stringToUTF8(name);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    
    NSString* foofile = [documentsPath stringByAppendingPathComponent:nsSrc];

    sqlite3 *db;
    int rc = sqlite3_open([foofile UTF8String], &db);
    
    POOL_END();    
    return (JAVA_LONG)db;
}

void com_codename1_impl_ios_IOSNative_sqlDbDelete___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    POOL_BEGIN();
    NSArray *writablePaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsPath = [writablePaths lastObject];    
    
    const char* chrs = stringToUTF8(name);
    NSString* nsSrc = [NSString stringWithUTF8String:chrs];
    
    NSString* foofile = [documentsPath stringByAppendingPathComponent:nsSrc];
    [[NSFileManager defaultManager] removeItemAtPath:foofile error:nil];
    POOL_END();    
}

void com_codename1_impl_ios_IOSNative_sqlDbClose___long(JAVA_OBJECT instanceObject, JAVA_LONG db) {
    sqlite3_free((sqlite3*)db);
}

void com_codename1_impl_ios_IOSNative_sqlDbExec___long_java_lang_String_java_lang_String_1ARRAY(JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT sql, JAVA_OBJECT args) {
    sqlite3* db = (sqlite3*)dbPeer;
    const char* chrs = stringToUTF8(sql);
    if(args != nil) {
        sqlite3_stmt *addStmt = nil;
        sqlite3_prepare_v2(db, chrs, -1, &addStmt, nil);
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
            const char* chrs = stringToUTF8(str);
            sqlite3_bind_text(addStmt, iter + 1, chrs, -1, SQLITE_TRANSIENT);
        }
        sqlite3_step(addStmt);
        sqlite3_finalize(addStmt);
    } else {
        sqlite3_exec(db, chrs, 0, 0, 0);
    }
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlDbExecQuery___long_java_lang_String_java_lang_String_1ARRAY(JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT sql, JAVA_OBJECT args) {
    sqlite3* db = (sqlite3*)dbPeer;
    const char* chrs = stringToUTF8(sql);
    sqlite3_stmt *addStmt = nil;
    sqlite3_prepare_v2(db, chrs, -1, &addStmt, nil);

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
            const char* chrs = stringToUTF8(str);
            sqlite3_bind_text(addStmt, iter + 1, chrs, -1, SQLITE_TRANSIENT);
        }
    }
    return (JAVA_LONG)addStmt;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorFirst___long(JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    sqlite3_reset((sqlite3_stmt *)statementPeer);
    return YES;
}
    
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorNext___long(JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    return sqlite3_step((sqlite3_stmt *)statementPeer) == SQLITE_ROW;    
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlGetColName___long_int(JAVA_OBJECT instanceObject, JAVA_LONG statementPeer, JAVA_INT index) {
    return xmlvm_create_java_string(sqlite3_column_name((sqlite3_stmt*)statementPeer, index));
}

void com_codename1_impl_ios_IOSNative_sqlCursorCloseStatement___long(JAVA_OBJECT instanceObject, JAVA_LONG statement) {
    sqlite3_finalize((sqlite3_stmt*)statement);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnBlob___long_int(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return nil;
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnDouble___long_int(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_double((sqlite3_stmt*)statement, col);
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnFloat___long_int(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_double((sqlite3_stmt*)statement, col);
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnInteger___long_int(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_int((sqlite3_stmt*)statement, col);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnLong___long_int(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_int64((sqlite3_stmt*)statement, col);
}

JAVA_SHORT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnShort___long_int(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return sqlite3_column_int((sqlite3_stmt*)statement, col);
}

#ifdef NEW_CODENAME_ONE_VM
JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnString___long_int_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return newStringFromCString(sqlite3_column_text((sqlite3_stmt*)statement, col));
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

JAVA_INT com_codename1_impl_ios_IOSNative_sqlCursorGetColumnCount___long(JAVA_OBJECT instanceObject, JAVA_LONG statement) {
    sqlite3_stmt *stmt = (sqlite3_stmt*)statement;
    return sqlite3_column_count(stmt);
}

JAVA_OBJECT productsArrayPending = nil;

void com_codename1_impl_ios_IOSNative_fetchProducts___java_lang_String_1ARRAY_com_codename1_payment_Product_1ARRAY(JAVA_OBJECT instanceObject, JAVA_OBJECT skus, JAVA_OBJECT products) {
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
        [productIdentifiers addObject:toNSString(data[iter])];
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
void com_codename1_impl_ios_IOSNative_purchase___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT sku) {
    dispatch_async(dispatch_get_main_queue(), ^{
        paymentInstance = [SKPayment paymentWithProductIdentifier:toNSString(sku)];
        [[SKPaymentQueue defaultQueue] addPayment:paymentInstance];
    });
}

void com_codename1_impl_ios_IOSNative_restorePurchases__(JAVA_OBJECT instanceObject) {
    [[SKPaymentQueue defaultQueue] restoreCompletedTransactions];
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_canMakePayments__(JAVA_OBJECT instanceObject) {
    return (JAVA_BOOLEAN)[SKPaymentQueue canMakePayments];
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatInt___int(JAVA_OBJECT instanceObject, JAVA_INT i) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSNumberFormatter *formatter = [[[NSNumberFormatter alloc] init] autorelease];
#else
    NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
#endif
    [formatter setNumberStyle:NSNumberFormatterDecimalStyle];
    JAVA_OBJECT o = fromNSString([formatter stringFromNumber:[NSNumber numberWithInt:i]]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDouble___double(JAVA_OBJECT instanceObject, JAVA_DOUBLE d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSNumberFormatter *formatter = [[[NSNumberFormatter alloc] init] autorelease];
#else
    NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
#endif
    [formatter setNumberStyle:NSNumberFormatterDecimalStyle];
    JAVA_OBJECT o = fromNSString([formatter stringFromNumber:[NSNumber numberWithDouble:d]]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatCurrency___double(JAVA_OBJECT instanceObject, JAVA_DOUBLE d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSNumberFormatter *formatter = [[[NSNumberFormatter alloc] init] autorelease];
#else
    NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
#endif
    [formatter setNumberStyle:NSNumberFormatterCurrencyStyle];
    JAVA_OBJECT o = fromNSString([formatter stringFromNumber:[NSNumber numberWithDouble:d]]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDate___long(JAVA_OBJECT instanceObject, JAVA_LONG d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateStyle:NSDateFormatterMediumStyle];
    JAVA_OBJECT o = fromNSString([formatter stringFromDate:date]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateShort___long(JAVA_OBJECT instanceObject, JAVA_LONG d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateStyle:NSDateFormatterShortStyle];
    JAVA_OBJECT o = fromNSString([formatter stringFromDate:date]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateTime___long(JAVA_OBJECT instanceObject, JAVA_LONG d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateStyle:NSDateFormatterLongStyle];
    [formatter setTimeStyle:NSDateFormatterLongStyle];
    JAVA_OBJECT o = fromNSString([formatter stringFromDate:date]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateTimeMedium___long(JAVA_OBJECT instanceObject, JAVA_LONG d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateStyle:NSDateFormatterMediumStyle];
    [formatter setTimeStyle:NSDateFormatterMediumStyle];
    JAVA_OBJECT o = fromNSString([formatter stringFromDate:date]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateTimeShort___long(JAVA_OBJECT instanceObject, JAVA_LONG d) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(d / 1000)];
    [formatter setDateStyle:NSDateFormatterShortStyle];
    [formatter setTimeStyle:NSDateFormatterShortStyle];
    JAVA_OBJECT o = fromNSString([formatter stringFromDate:date]);
    POOL_END();
    return o;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getCurrencySymbol__(JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSNumberFormatter *formatter = [[[NSNumberFormatter alloc] init] autorelease];
#else
    NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
#endif
    JAVA_OBJECT c = fromNSString([formatter currencyCode]);
    POOL_END();
    return c;
}

void com_codename1_impl_ios_IOSNative_scanBarCode__(JAVA_OBJECT instanceObject) {
#if !TARGET_IPHONE_SIMULATOR
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CVZBarReaderViewController *reader = [CVZBarReaderViewController new];
        ScanCodeImpl* scanCall = [[ScanCodeImpl alloc] init];
        reader.readerDelegate = scanCall;
        reader.supportedOrientationsMask = ZBarOrientationMaskAll;
        
        //ZBAR_CONFIGURATIONS

        ZBarImageScanner *scanner = reader.scanner;
        // TODO: (optional) additional reader configuration here
        
        // EXAMPLE: disable rarely used I2/5 to improve performance
        [scanner setSymbology: ZBAR_I25
                       config: ZBAR_CFG_ENABLE
                           to: 0];
        
        // present and release the controller
        [[CodenameOne_GLViewController instance] presentModalViewController:reader animated:NO];
#ifndef CN1_USE_ARC
        [reader release];
#endif
        POOL_END();
    });
#endif
}

void com_codename1_impl_ios_IOSNative_scanQRCode__(JAVA_OBJECT instanceObject) {
    /*dispatch_sync(dispatch_get_main_queue(), ^{
     ScanCodeImpl* scanCall = [[ScanCodeImpl alloc] init];
     ZXingWidgetController *widController = [[ZXingWidgetController alloc] initWithDelegate:scanCall showCancel:YES OneDMode:NO];
     
     NSMutableSet *readers = [[NSMutableSet alloc ] init];
     
     QRCodeReader* qrcodeReader = [[QRCodeReader alloc] init];
     [readers addObject:qrcodeReader];
#ifndef CN1_USE_ARC
     [qrcodeReader release];
#endif
     
     widController.readers = readers;
#ifndef CN1_USE_ARC
     [readers release];
#endif
     
     [[CodenameOne_GLViewController instance] presentModalViewController:widController animated:YES];
     [widController release];
     });*/
    com_codename1_impl_ios_IOSNative_scanBarCode__(instanceObject);
}

#ifdef NEW_CODENAME_ONE_VM
NSData* arrayToData(JAVA_OBJECT arr) {
    JAVA_ARRAY byteArray = (JAVA_ARRAY*)arr;
    void* data = (void*)byteArray->data;    
    NSData* d = [NSData dataWithBytes:data length:byteArray->length];
    return d;
}

JAVA_OBJECT nsDataToByteArr(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray([d length], &class_array1__JAVA_BYTE, sizeof(JAVA_BYTE), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;    
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToBooleanArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray([d length], &class_array1__JAVA_BOOLEAN, sizeof(JAVA_BOOLEAN), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;    
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToCharArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray([d length], &class_array1__JAVA_CHAR, sizeof(JAVA_CHAR), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;    
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToShortArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray([d length], &class_array1__JAVA_SHORT, sizeof(JAVA_SHORT), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;    
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToIntArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray([d length], &class_array1__JAVA_INT, sizeof(JAVA_INT), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;    
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToLongArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray([d length], &class_array1__JAVA_LONG, sizeof(JAVA_LONG), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;    
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToFloatArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray([d length], &class_array1__JAVA_FLOAT, sizeof(JAVA_FLOAT), 1);
    void* dtd = (void*)((JAVA_ARRAY)byteArray)->data;    
    memcpy(dtd, d.bytes, d.length);
    return byteArray;
}

JAVA_OBJECT nsDataToDoubleArray(NSData *data) {
    NSData* d = data;
    JAVA_OBJECT byteArray = allocArray([d length], &class_array1__JAVA_DOUBLE, sizeof(JAVA_DOUBLE), 1);
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

JAVA_LONG com_codename1_impl_ios_IOSNative_createTruetypeFont___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    int pSize = 14;
    
    pSize *= scaleValue;
    POOL_BEGIN();
    NSString* str = toNSString(name);
    UIFont* fnt = [UIFont fontWithName:str size:pSize];
#ifndef CN1_USE_ARC
    [fnt retain];
#endif
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)fnt);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_deriveTruetypeFont___long_boolean_boolean_float(JAVA_OBJECT instanceObject, JAVA_LONG uiFont, JAVA_BOOLEAN bold, JAVA_BOOLEAN italic, JAVA_FLOAT size) {
    POOL_BEGIN();
    UIFont* original = (BRIDGE_CAST UIFont*)((void *)uiFont);
    UIFont* fnt = [original fontWithSize:size];
#ifndef CN1_USE_ARC
    [fnt retain];
#endif
    POOL_END();
    return (JAVA_LONG)((BRIDGE_CAST void*)fnt);
}

void com_codename1_impl_ios_IOSNative_log___java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    POOL_BEGIN();
    NSLog(toNSString(name));
    POOL_END();
}

void com_codename1_impl_ios_IOSNative_getCookiesForURL___java_lang_String_java_util_Vector(JAVA_OBJECT receiver, JAVA_OBJECT urlStr, JAVA_OBJECT outVector) {
    POOL_BEGIN();
    NSHTTPCookieStorage *cstore = [NSHTTPCookieStorage sharedHTTPCookieStorage];

    NSString* nsStr = toNSString(urlStr);
    
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
        JAVA_OBJECT name = fromNSString([cookie name]);
        JAVA_OBJECT domain = fromNSString([cookie domain]);
        JAVA_OBJECT path = fromNSString([cookie path]);
        JAVA_OBJECT value = fromNSString([cookie value]);
        JAVA_LONG expires = [[cookie expiresDate] timeIntervalSince1970];
        JAVA_BOOLEAN secure = [cookie isSecure];
        JAVA_BOOLEAN httpOnly = [cookie isHTTPOnly];

        JAVA_OBJECT jcookie = __NEW_INSTANCE_com_codename1_io_Cookie();
        com_codename1_io_Cookie_setName___java_lang_String(jcookie, name);
        com_codename1_io_Cookie_setSecure___boolean(jcookie, secure);
        com_codename1_io_Cookie_setHttpOnly___boolean(jcookie, httpOnly);
        com_codename1_io_Cookie_setPath___java_lang_String(jcookie, path);
        com_codename1_io_Cookie_setValue___java_lang_String(jcookie, value);
        com_codename1_io_Cookie_setDomain___java_lang_String(jcookie, domain);
        com_codename1_io_Cookie_setExpires___long(jcookie, expires);

#ifndef NEW_CODENAME_ONE_VM
        java_util_Vector_add___java_lang_Object(outVector, jcookie);
#else
        java_util_Vector_add___java_lang_Object_R_boolean(outVector, jcookie);
#endif
    }

    POOL_END();
}

void com_codename1_impl_ios_IOSNative_addCookie___java_lang_String_java_lang_String_java_lang_String_java_lang_String_boolean_boolean_long(JAVA_OBJECT receiver, JAVA_OBJECT key, JAVA_OBJECT value,JAVA_OBJECT domain, JAVA_OBJECT path, JAVA_BOOLEAN secure, JAVA_BOOLEAN httpOnly, JAVA_LONG expires) {
    POOL_BEGIN();
    NSDictionary *stringProps = [[NSDictionary alloc] initWithObjectsAndKeys:
                    toNSString(key), NSHTTPCookieName,
                    toNSString(value), NSHTTPCookieValue,
                    toNSString(domain), NSHTTPCookieDomain,
                    toNSString(path), NSHTTPCookiePath,
                    (secure ? @"1" : @""), NSHTTPCookieSecure,
                    [NSDate dateWithTimeIntervalSince1970:expires], NSHTTPCookieExpires, Nil];
    NSHTTPCookie *cookie = [NSHTTPCookie cookieWithProperties: stringProps];
    [[NSHTTPCookieStorage sharedHTTPCookieStorage] setCookie:cookie];

    POOL_END();
}

void com_codename1_impl_ios_IOSNative_zoozPurchase___double_java_lang_String_java_lang_String_boolean_java_lang_String(JAVA_OBJECT instanceObject, JAVA_DOUBLE amount, JAVA_OBJECT currency, JAVA_OBJECT appKey, JAVA_BOOLEAN sandbox, JAVA_OBJECT invoiceNumber) {
#ifdef INCLUDE_ZOOZ
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        ZooZ *zooz = [ZooZ sharedInstance];
        zooz.sandbox = sandbox;//set this if working in Sandbox mode
        ZooZPaymentRequest *req = [zooz createPaymentRequestWithTotal:amount invoiceRefNumber:toNSString(invoiceNumber) delegate:[CodenameOne_GLViewController instance]];
        req.currencyCode = toNSString(currency);
//        req.payerDetails.email = @"test@test.com";
        [zooz openPayment:req forAppKey:toNSString(appKey)];
        POOL_END();
    });
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_browserExecuteAndReturnString___long_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT javaScript){
    __block JAVA_OBJECT out;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* w = (BRIDGE_CAST UIWebView*)((void *)peer);
        out = fromNSString([w stringByEvaluatingJavaScriptFromString:toNSString(javaScript)]);
        POOL_END();
    });
    return out;
}

JAVA_OBJECT java_util_TimeZone_getTimezoneId__() {
    POOL_BEGIN();
    NSTimeZone *tzone = [NSTimeZone defaultTimeZone];
    NSString* n = [tzone name];
    //NSLog(@"java_util_TimeZone_getTimezoneId__ %@", n);
    JAVA_OBJECT str = fromNSString(n);
    POOL_END();
    return str;
}

JAVA_INT java_util_TimeZone_getTimezoneOffset___java_lang_String_int_int_int_int(JAVA_OBJECT name, JAVA_INT year, JAVA_INT month, JAVA_INT day, JAVA_INT timeOfDayMillis) {
    POOL_BEGIN();
    NSString* n = toNSString(name);
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

JAVA_INT java_util_TimeZone_getTimezoneRawOffset___java_lang_String(JAVA_OBJECT name) {
    POOL_BEGIN();
    NSString* n = toNSString(name);
    //NSLog(@"java_util_TimeZone_getTimezoneRawOffset___java_lang_String %@", n);
    NSTimeZone *tzone = [NSTimeZone timeZoneWithName:n];
    JAVA_INT result = [tzone secondsFromGMT] * 1000;
    if([tzone isDaylightSavingTime]) {
        result -= (int)([tzone daylightSavingTimeOffset] * 1000);
    }
    POOL_END();
    return result;
}

JAVA_BOOLEAN java_util_TimeZone_isTimezoneDST___java_lang_String_long(JAVA_OBJECT name, JAVA_LONG millis) {
    POOL_BEGIN();
    NSString* n = toNSString(name);
    //NSLog(@"java_util_TimeZone_isTimezoneDST___java_lang_String_long %@, %i", n, millis / 1000);
    NSTimeZone *tzone = [NSTimeZone timeZoneWithName:n];
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(millis / 1000)];
    JAVA_BOOLEAN result = [tzone isDaylightSavingTimeForDate:date];
    POOL_END();
    return result;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUserAgentString__(JAVA_OBJECT instanceObject) {
    __block JAVA_OBJECT c = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        UIWebView* webView = [[UIWebView alloc] initWithFrame:CGRectZero];
        NSString* userAgentString = [webView stringByEvaluatingJavaScriptFromString:@"navigator.userAgent"];
        c = fromNSString(userAgentString);
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
JAVA_LONG defaultDatePickerDate;

void com_codename1_impl_ios_IOSNative_openStringPicker___java_lang_String_1ARRAY_int_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_OBJECT stringArray, JAVA_INT selection, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
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
            [vc setContentSizeForViewInPopover:CGSizeMake(320, 260)];
            
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
            JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance___R_com_codename1_ui_plaf_UIManager();
#endif
            JAVA_OBJECT str;
            UIBarButtonItem *doneButton;
            NSArray *itemsArray = nil;
#ifndef NEW_CODENAME_ONE_VM
            str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String(obj, fromNSString(@"OK"), fromNSString(@"OK"));
#else
            str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String_R_java_lang_String(obj, fromNSString(@"OK"), fromNSString(@"OK"));
#endif
            NSString* buttonTitle = toNSString(str);
            doneButton = [[UIBarButtonItem alloc]initWithTitle:buttonTitle style:UIBarButtonItemStyleDone target:[CodenameOne_GLViewController instance] action:@selector(pickerComponentDismiss)];
            
            itemsArray = [NSArray arrayWithObjects: doneButton, nil];
#ifndef CN1_USE_ARC
            [flexButton release];
            [doneButton release];
#endif
            [toolbar setItems:itemsArray];
            
            [popoverView addSubview:toolbar];
            [popoverView addSubview:pickerView];
            pickerView.frame = CGRectMake(0, 44, pickerView.frame.size.width, pickerView.frame.size.height);
            
            UIPopoverController* uip = [[UIPopoverController alloc] initWithContentViewController:vc];
            popoverControllerInstance = uip;

            uip.delegate = [CodenameOne_GLViewController instance];
            [uip presentPopoverFromRect:CGRectMake(x / scaleValue, y / scaleValue, w / scaleValue, h / scaleValue) inView:[CodenameOne_GLViewController instance].view permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
        } else {
            UIActionSheet* actionSheet;
            int topBoundry = 10;
            if(isIOS7()) {
                actionSheet = [[UIActionSheet alloc] initWithTitle:nil delegate:[CodenameOne_GLViewController instance] cancelButtonTitle:nil destructiveButtonTitle:nil otherButtonTitles:nil];
                [actionSheet addButtonWithTitle:@"OK"];
                topBoundry = 40;
            } else {
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
            }
            
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

void com_codename1_impl_ios_IOSNative_openDatePicker___int_long_int_int_int_int(JAVA_OBJECT instanceObject, JAVA_INT type, JAVA_LONG time, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    pickerStringArray = nil;
    currentDatePickerDate = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
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
        NSDate* date = [NSDate dateWithTimeIntervalSince1970:(time / 1000)];
        datePickerView.tag = 10;
        datePickerView.date = date;
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
            
            //add a space filler to the left:
            UIBarButtonItem *flexButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:
                                           UIBarButtonSystemItemFlexibleSpace target: nil action:nil];
            
#ifndef NEW_CODENAME_ONE_VM
            JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance__();
#else
            JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance___R_com_codename1_ui_plaf_UIManager();
#endif
            JAVA_OBJECT str;
            UIBarButtonItem *doneButton;
            NSArray *itemsArray = nil;
#ifndef NEW_CODENAME_ONE_VM
            str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String(obj, fromNSString(@"OK"), fromNSString(@"OK"));
#else
            str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String_R_java_lang_String(obj, fromNSString(@"OK"), fromNSString(@"OK"));
#endif
            NSString* buttonTitle = toNSString(str);
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
            
            uip.delegate = [CodenameOne_GLViewController instance];
            [uip presentPopoverFromRect:CGRectMake(x / scaleValue, y / scaleValue, w / scaleValue, h / scaleValue) inView:[CodenameOne_GLViewController instance].view permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
        } else {
            UIActionSheet* actionSheet;
            int topBoundry = 10;
            if(isIOS7()) {
                actionSheet = [[UIActionSheet alloc] initWithTitle:nil delegate:[CodenameOne_GLViewController instance] cancelButtonTitle:nil destructiveButtonTitle:nil otherButtonTitles:nil];
                [actionSheet addButtonWithTitle:@"OK"];
                topBoundry = 40;
            } else {
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
            }
            
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

void com_codename1_impl_ios_IOSNative_socialShare___java_lang_String_long(JAVA_OBJECT me, JAVA_OBJECT text, JAVA_LONG imagePeer) {
    NSString* someText = toNSString(text);
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
        [[CodenameOne_GLViewController instance] presentViewController:activityViewController animated:YES completion:^{}];
        POOL_END();
        repaintUI();
    });
}

extern BOOL vkbAlwaysOpen;
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAsyncEditMode__(JAVA_OBJECT instanceObject) {
    return vkbAlwaysOpen;
}

void com_codename1_impl_ios_IOSNative_setAsyncEditMode___boolean(JAVA_OBJECT instanceObject, JAVA_BOOLEAN b) {
    vkbAlwaysOpen = b;
}

void com_codename1_impl_ios_IOSNative_foldVKB__(JAVA_OBJECT instanceObject) {
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

void com_codename1_impl_ios_IOSNative_hideTextEditing__(JAVA_OBJECT instanceObject) {
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

JAVA_LONG com_codename1_impl_ios_IOSNative_connectSocket___java_lang_String_int(JAVA_OBJECT instanceObject, JAVA_OBJECT host, JAVA_INT port) {
    POOL_BEGIN();
    SocketImpl* impl = [[SocketImpl alloc] init];
    BOOL b = [impl connect:toNSString(host) port:port];
    POOL_END();
    if(b) {
        return impl;
    }
    return JAVA_NULL;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getHostOrIP__(JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
    JAVA_OBJECT o = fromNSString([SocketImpl getIP]);
    POOL_END();
    return o;
}

void com_codename1_impl_ios_IOSNative_disconnectSocket___long(JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    [impl disconnect];
    POOL_END();
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isSocketConnected___long(JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    JAVA_BOOLEAN b = [impl isConnected];
    POOL_END();
    return b;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getSocketErrorMessage___long(JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    JAVA_OBJECT b = fromNSString([impl getErrorMessage]);
    POOL_END();
    return b;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getSocketErrorCode___long(JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    JAVA_INT b = [impl getErrorCode];
    POOL_END();
    return b;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getSocketAvailableInput___long(JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    JAVA_INT b = [impl getAvailableInput];
    POOL_END();
    return b;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_readFromSocketStream___long(JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    JAVA_OBJECT b = nsDataToByteArr([impl readFromStream]);
    POOL_END();
    return b;
}

void com_codename1_impl_ios_IOSNative_writeToSocketStream___long_byte_1ARRAY(JAVA_OBJECT instanceObject, JAVA_LONG socket, JAVA_OBJECT data) {
    POOL_BEGIN();
    SocketImpl* impl = (BRIDGE_CAST SocketImpl*)((void *)socket);
    [impl writeToStream:arrayToData(data)];
    POOL_END();
}


#ifdef NEW_CODENAME_ONE_VM
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isPainted___R_boolean(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isPainted__(instanceObject);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplayWidth___R_int(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getDisplayWidth__(instanceObject);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplayHeight___R_int(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getDisplayHeight__(instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageFromARGB___int_1ARRAY_int_int_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_INT n2, JAVA_INT n3) {
    return com_codename1_impl_ios_IOSNative_createImageFromARGB___int_1ARRAY_int_int(instanceObject, n1, n2, n3);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImage___byte_1ARRAY_int_1ARRAY_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT n2) {
    return com_codename1_impl_ios_IOSNative_createImage___byte_1ARRAY_int_1ARRAY(instanceObject, n1, n2);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageNSData___long_int_1ARRAY_R_long(JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_OBJECT n2) {
    return com_codename1_impl_ios_IOSNative_createImageNSData___long_int_1ARRAY(instanceObject, nsData, n2);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_scale___long_int_int_R_long(JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_INT n2, JAVA_INT n3) {
    return com_codename1_impl_ios_IOSNative_scale___long_int_int(instanceObject, n1, n2, n3);
}

JAVA_INT com_codename1_impl_ios_IOSNative_stringWidthNative___long_java_lang_String_R_int(JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_OBJECT n2) {
    return com_codename1_impl_ios_IOSNative_stringWidthNative___long_java_lang_String(instanceObject, n1, n2);
}

JAVA_INT com_codename1_impl_ios_IOSNative_charWidthNative___long_char_R_int(JAVA_OBJECT instanceObject, JAVA_LONG n1, JAVA_CHAR n2) {
    return com_codename1_impl_ios_IOSNative_charWidthNative___long_char(instanceObject, n1, n2);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getFontHeightNative___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG n1) {
    return com_codename1_impl_ios_IOSNative_getFontHeightNative___long(instanceObject, n1);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createSystemFont___int_int_int_R_long(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3) {
    return com_codename1_impl_ios_IOSNative_createSystemFont___int_int_int(instanceObject, n1, n2, n3);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResourceSize___java_lang_String_java_lang_String_R_int(JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT n2) {
    return com_codename1_impl_ios_IOSNative_getResourceSize___java_lang_String_java_lang_String(instanceObject, n1, n2);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeMutableImage___int_int_int_R_long(JAVA_OBJECT instanceObject, JAVA_INT n1, JAVA_INT n2, JAVA_INT n3) {
    return com_codename1_impl_ios_IOSNative_createNativeMutableImage___int_int_int(instanceObject, n1, n2, n3);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_finishDrawingOnImage___R_long(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_finishDrawingOnImage__(instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isTablet___R_boolean(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isTablet__(instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isIOS7___R_boolean(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isIOS7__(instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNSData___java_lang_String_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    return com_codename1_impl_ios_IOSNative_createNSData___java_lang_String(instanceObject, file);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNSDataResource___java_lang_String_java_lang_String_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT name, JAVA_OBJECT type) {
    return com_codename1_impl_ios_IOSNative_createNSDataResource___java_lang_String_java_lang_String(instanceObject, name, type);
}

JAVA_INT com_codename1_impl_ios_IOSNative_read___long_int_R_int(JAVA_OBJECT instanceObject, JAVA_LONG nsData, JAVA_INT pointer) {
    return com_codename1_impl_ios_IOSNative_read___long_int(instanceObject, nsData, pointer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_writeToFile___byte_1ARRAY_java_lang_String_R_int(JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT path) {
    return com_codename1_impl_ios_IOSNative_writeToFile___byte_1ARRAY_java_lang_String(instanceObject, n1, path);
}

JAVA_INT com_codename1_impl_ios_IOSNative_appendToFile___byte_1ARRAY_java_lang_String_R_int(JAVA_OBJECT instanceObject, JAVA_OBJECT n1, JAVA_OBJECT path) {
    return com_codename1_impl_ios_IOSNative_appendToFile___byte_1ARRAY_java_lang_String(instanceObject, n1, path);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getFileSize___java_lang_String_R_int(JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    return com_codename1_impl_ios_IOSNative_getFileSize___java_lang_String(instanceObject, path);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getFileLastModified___java_lang_String_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT path) {
    return com_codename1_impl_ios_IOSNative_getFileLastModified___java_lang_String(instanceObject, path);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getDocumentsDir___R_java_lang_String(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getDocumentsDir__(instanceObject);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getCachesDir___R_java_lang_String(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getCachesDir__(instanceObject);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResourcesDir___R_java_lang_String(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getResourcesDir__(instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_fileExists___java_lang_String_R_boolean(JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    return com_codename1_impl_ios_IOSNative_fileExists___java_lang_String(instanceObject, file);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDirectory___java_lang_String_R_boolean(JAVA_OBJECT instanceObject, JAVA_OBJECT file) {
    return com_codename1_impl_ios_IOSNative_isDirectory___java_lang_String(instanceObject, file);
}

JAVA_INT com_codename1_impl_ios_IOSNative_fileCountInDir___java_lang_String_R_int(JAVA_OBJECT instanceObject, JAVA_OBJECT dir) {
    return com_codename1_impl_ios_IOSNative_fileCountInDir___java_lang_String(instanceObject, dir);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_openConnection___java_lang_String_int_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT url, JAVA_INT timeout) {
    return com_codename1_impl_ios_IOSNative_openConnection___java_lang_String_int(instanceObject, url, timeout);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResponseCode___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getResponseCode___long(instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseMessage___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getResponseMessage___long(instanceObject, peer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getContentLength___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getContentLength___long(instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseHeader___long_java_lang_String_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT name) {
    return com_codename1_impl_ios_IOSNative_getResponseHeader___long_java_lang_String(instanceObject, peer, name);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getResponseHeaderCount___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getResponseHeaderCount___long(instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getResponseHeaderName___long_int_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
    return com_codename1_impl_ios_IOSNative_getResponseHeaderName___long_int(instanceObject, peer, offset);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isMinimized___R_boolean(JAVA_OBJECT instanceObject)
{
    return com_codename1_impl_ios_IOSNative_isMinimized__(instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_minimizeApplication___R_boolean(JAVA_OBJECT instanceObject)
{
    return com_codename1_impl_ios_IOSNative_minimizeApplication__(instanceObject);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getAudioDuration___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getAudioDuration___long(instanceObject, peer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAudioPlaying___long_R_boolean(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_isAudioPlaying___long(instanceObject, peer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getAudioTime___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getAudioTime___long(instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___java_lang_String_java_lang_Runnable_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT uri, JAVA_OBJECT onCompletion) {
    return com_codename1_impl_ios_IOSNative_createAudio___java_lang_String_java_lang_Runnable(instanceObject, uri, onCompletion);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudio___byte_1ARRAY_java_lang_Runnable_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT b, JAVA_OBJECT onCompletion) {
    return com_codename1_impl_ios_IOSNative_createAudio___byte_1ARRAY_java_lang_Runnable(instanceObject, b, onCompletion);
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_getVolume___R_float(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getVolume__(instanceObject);    
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createBrowserComponent___java_lang_Object_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT obj) {
    return com_codename1_impl_ios_IOSNative_createBrowserComponent___java_lang_Object(instanceObject, obj);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_browserHasBack___long_R_boolean(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_browserHasBack___long(instanceObject, peer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_browserHasForward___long_R_boolean(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_browserHasForward___long(instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getBrowserTitle___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getBrowserTitle___long(instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getBrowserURL___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getBrowserURL___long(instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponent___java_lang_String_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT str) {
    return com_codename1_impl_ios_IOSNative_createVideoComponent___java_lang_String(instanceObject, str);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponent___java_lang_String_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT str) {
    return com_codename1_impl_ios_IOSNative_createNativeVideoComponent___java_lang_String(instanceObject, str);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponent___byte_1ARRAY_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT dataObject) {
    return com_codename1_impl_ios_IOSNative_createVideoComponent___byte_1ARRAY(instanceObject, dataObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponent___byte_1ARRAY_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT dataObject) {
    return com_codename1_impl_ios_IOSNative_createNativeVideoComponent___byte_1ARRAY(instanceObject, dataObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createVideoComponentNSData___long_R_long(JAVA_OBJECT instanceObject, JAVA_LONG nsData) {
    return com_codename1_impl_ios_IOSNative_createVideoComponentNSData___long(instanceObject, nsData);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createNativeVideoComponentNSData___long_R_long(JAVA_OBJECT instanceObject, JAVA_LONG nsData) {
    return com_codename1_impl_ios_IOSNative_createNativeVideoComponentNSData___long(instanceObject, nsData);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getMediaTimeMS___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getMediaTimeMS___long(instanceObject, peer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_setMediaTimeMS___long_int_R_int(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT time) {
    return com_codename1_impl_ios_IOSNative_setMediaTimeMS___long_int(instanceObject, peer, time);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getMediaDuration___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getMediaDuration___long(instanceObject, peer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoPlaying___long_R_boolean(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_isVideoPlaying___long(instanceObject, peer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isVideoFullScreen___long_R_boolean(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_isVideoFullScreen___long(instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getVideoViewPeer___long_R_long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getVideoViewPeer___long(instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createCLLocation___R_long(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_createCLLocation__(instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getCurrentLocationObject___long_R_long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getCurrentLocationObject___long(instanceObject, peer);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationLatitude___long_R_double(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationLatitude___long(instanceObject, peer);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationAltitude___long_R_double(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationAltitude___long(instanceObject, peer);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationLongtitude___long_R_double(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationLongtitude___long(instanceObject, peer);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationAccuracy___long_R_double(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationAccuracy___long(instanceObject, peer);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationDirection___long_R_double(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationDirection___long(instanceObject, peer);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_getLocationVelocity___long_R_double(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationVelocity___long(instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getLocationTimeStamp___long_R_long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getLocationTimeStamp___long(instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUDID___R_java_lang_String(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getUDID__(instanceObject);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getOSVersion___R_java_lang_String(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getOSVersion__(instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isGoodLocation___long_R_boolean(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_isGoodLocation___long(instanceObject, peer);    
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isContactsPermissionGranted___R_boolean(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isContactsPermissionGranted__(instanceObject);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_createContact___java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_OBJECT firstName, JAVA_OBJECT surname, JAVA_OBJECT officePhone, JAVA_OBJECT homePhone, JAVA_OBJECT cellPhone, JAVA_OBJECT email) {
    return com_codename1_impl_ios_IOSNative_createContact___java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String(instanceObject, firstName, surname, officePhone, homePhone, cellPhone, email);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_deleteContact___int_R_boolean(JAVA_OBJECT instanceObject, JAVA_INT i) {
    return com_codename1_impl_ios_IOSNative_deleteContact___int(instanceObject, i);
}


JAVA_INT com_codename1_impl_ios_IOSNative_getContactCount___boolean_R_int(JAVA_OBJECT instanceObject, JAVA_BOOLEAN includeNumbers) {
    return com_codename1_impl_ios_IOSNative_getContactCount___boolean(instanceObject, includeNumbers);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonFirstName___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getPersonFirstName___long(instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonSurnameName___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getPersonSurnameName___long(instanceObject, peer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getPersonPhoneCount___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getPersonPhoneCount___long(instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPhone___long_int_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
    return com_codename1_impl_ios_IOSNative_getPersonPhone___long_int(instanceObject, peer, offset);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPhoneType___long_int_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT offset) {
    return com_codename1_impl_ios_IOSNative_getPersonPhoneType___long_int(instanceObject, peer, offset);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonPrimaryPhone___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getPersonPrimaryPhone___long(instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonEmail___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getPersonEmail___long(instanceObject, peer);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getPersonAddress___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_getPersonAddress___long(instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createPersonPhotoImage___long_R_long(JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_createPersonPhotoImage___long(instanceObject, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_getPersonWithRecordID___int_R_long(JAVA_OBJECT instanceObject, JAVA_INT recId) {
    return com_codename1_impl_ios_IOSNative_getPersonWithRecordID___int(instanceObject, recId);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageFile___long_boolean_int_int_float_R_long(JAVA_OBJECT instanceObject, JAVA_LONG imagePeer, JAVA_BOOLEAN jpeg, int width, int height, JAVA_FLOAT quality) {
    return com_codename1_impl_ios_IOSNative_createImageFile___long_boolean_int_int_float(instanceObject, imagePeer, jpeg, width, height, quality);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getNSDataSize___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG nsData) {
    return com_codename1_impl_ios_IOSNative_getNSDataSize___long(instanceObject, nsData);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createAudioRecorder___java_lang_String_R_long(JAVA_OBJECT instanceObject,
    JAVA_OBJECT  destinationFile) {
    return com_codename1_impl_ios_IOSNative_createAudioRecorder___java_lang_String(instanceObject, destinationFile);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlDbExists___java_lang_String_R_boolean(JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    return com_codename1_impl_ios_IOSNative_sqlDbExists___java_lang_String(instanceObject, name);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlDbCreateAndOpen___java_lang_String_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    return com_codename1_impl_ios_IOSNative_sqlDbCreateAndOpen___java_lang_String(instanceObject, name);
}


JAVA_LONG com_codename1_impl_ios_IOSNative_sqlDbExecQuery___long_java_lang_String_java_lang_String_1ARRAY_R_long(JAVA_OBJECT instanceObject, JAVA_LONG dbPeer, JAVA_OBJECT sql, JAVA_OBJECT args) {
    return com_codename1_impl_ios_IOSNative_sqlDbExecQuery___long_java_lang_String_java_lang_String_1ARRAY(instanceObject, dbPeer, sql, args);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorFirst___long_R_boolean(JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    return com_codename1_impl_ios_IOSNative_sqlCursorFirst___long(instanceObject, statementPeer);
}
    
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_sqlCursorNext___long_R_boolean(JAVA_OBJECT instanceObject, JAVA_LONG statementPeer) {
    return com_codename1_impl_ios_IOSNative_sqlCursorNext___long(instanceObject, statementPeer);    
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlGetColName___long_int_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG statementPeer, JAVA_INT index) {
    return com_codename1_impl_ios_IOSNative_sqlGetColName___long_int(instanceObject, statementPeer, index);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnBlob___long_int_R_byte_1ARRAY(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnBlob___long_int(instanceObject, statement, col);
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnDouble___long_int_R_double(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnDouble___long_int(instanceObject, statement, col);
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnFloat___long_int_R_float(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnFloat___long_int(instanceObject, statement, col);
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnInteger___long_int_R_int(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnInteger___long_int(instanceObject, statement, col);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnLong___long_int_R_long(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnLong___long_int(instanceObject, statement, col);
}

JAVA_SHORT com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnShort___long_int_R_short(JAVA_OBJECT instanceObject, JAVA_LONG statement, JAVA_INT col) {
    return com_codename1_impl_ios_IOSNative_sqlCursorValueAtColumnShort___long_int(instanceObject, statement, col);
}

JAVA_INT com_codename1_impl_ios_IOSNative_sqlCursorGetColumnCount___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG statement) {
    return com_codename1_impl_ios_IOSNative_sqlCursorGetColumnCount___long(instanceObject, statement);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_canMakePayments___R_boolean(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_canMakePayments__(instanceObject);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatInt___int_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_INT i) {
    return com_codename1_impl_ios_IOSNative_formatInt___int(instanceObject, i);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDouble___double_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_DOUBLE d) {
    return com_codename1_impl_ios_IOSNative_formatDouble___double(instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatCurrency___double_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_DOUBLE d) {
    return com_codename1_impl_ios_IOSNative_formatCurrency___double(instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDate___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG d) {
    return com_codename1_impl_ios_IOSNative_formatDate___long(instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateShort___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG d) {
    return com_codename1_impl_ios_IOSNative_formatDateShort___long(instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateTime___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG d) {
    return com_codename1_impl_ios_IOSNative_formatDateTime___long(instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateTimeMedium___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG d) {
    return com_codename1_impl_ios_IOSNative_formatDateTimeMedium___long(instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_formatDateTimeShort___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG d) {
    return com_codename1_impl_ios_IOSNative_formatDateTimeShort___long(instanceObject, d);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getCurrencySymbol___R_java_lang_String(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getCurrencySymbol__(instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createTruetypeFont___java_lang_String_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    return com_codename1_impl_ios_IOSNative_createTruetypeFont___java_lang_String(instanceObject, name);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_deriveTruetypeFont___long_boolean_boolean_float_R_long(JAVA_OBJECT instanceObject, JAVA_LONG uiFont, JAVA_BOOLEAN bold, JAVA_BOOLEAN italic, JAVA_FLOAT size) {
    return com_codename1_impl_ios_IOSNative_deriveTruetypeFont___long_boolean_boolean_float(instanceObject, uiFont, bold, italic, size);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_browserExecuteAndReturnString___long_java_lang_String_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT javaScript) {
    return com_codename1_impl_ios_IOSNative_browserExecuteAndReturnString___long_java_lang_String(instanceObject, peer, javaScript);
}

JAVA_OBJECT java_util_TimeZone_getTimezoneId___R_java_lang_String() {
    return java_util_TimeZone_getTimezoneId__();
}

JAVA_INT java_util_TimeZone_getTimezoneOffset___java_lang_String_int_int_int_int_R_int(JAVA_OBJECT name, JAVA_INT year, JAVA_INT month, JAVA_INT day, JAVA_INT timeOfDayMillis) {
    return java_util_TimeZone_getTimezoneOffset___java_lang_String_int_int_int_int(name, year, month, day, timeOfDayMillis);
}

JAVA_INT java_util_TimeZone_getTimezoneRawOffset___java_lang_String_R_int(JAVA_OBJECT name) {
    return java_util_TimeZone_getTimezoneRawOffset___java_lang_String(name);
}

JAVA_BOOLEAN java_util_TimeZone_isTimezoneDST___java_lang_String_long_R_boolean(JAVA_OBJECT name, JAVA_LONG millis) {
    return java_util_TimeZone_isTimezoneDST___java_lang_String_long(name, millis);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUserAgentString___R_java_lang_String(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getUserAgentString__(instanceObject);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_connectSocket___java_lang_String_int_R_long(JAVA_OBJECT instanceObject, JAVA_OBJECT host, JAVA_INT port) {
    return com_codename1_impl_ios_IOSNative_connectSocket___java_lang_String_int(instanceObject, host, port);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getHostOrIP___R_java_lang_String(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_getHostOrIP__(instanceObject);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isSocketConnected___long_R_boolean(JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    return com_codename1_impl_ios_IOSNative_isSocketConnected___long(instanceObject, socket);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getSocketErrorMessage___long_R_java_lang_String(JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    return com_codename1_impl_ios_IOSNative_getSocketErrorMessage___long(instanceObject, socket);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getSocketErrorCode___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    return com_codename1_impl_ios_IOSNative_getSocketErrorCode___long(instanceObject, socket);
}

JAVA_INT com_codename1_impl_ios_IOSNative_getSocketAvailableInput___long_R_int(JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    return com_codename1_impl_ios_IOSNative_getSocketAvailableInput___long(instanceObject, socket);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_readFromSocketStream___long_R_byte_1ARRAY(JAVA_OBJECT instanceObject, JAVA_LONG socket) {
    return com_codename1_impl_ios_IOSNative_readFromSocketStream___long(instanceObject, socket);
}

JAVA_OBJECT com_codename1_ui_Display_getInstance__() {
    return com_codename1_ui_Display_getInstance___R_com_codename1_ui_Display();
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createPeerImage___long_int_1ARRAY_R_long(JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT arr) {
    return com_codename1_impl_ios_IOSNative_createPeerImage___long_int_1ARRAY(instanceObject, peer, arr);
}

extern JAVA_OBJECT com_codename1_impl_ios_IOSNative_getFacebookToken__(JAVA_OBJECT me);
JAVA_OBJECT com_codename1_impl_ios_IOSNative_getFacebookToken___R_java_lang_String(JAVA_OBJECT me) {
    return com_codename1_impl_ios_IOSNative_getFacebookToken__(me);
}

extern JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isFacebookLoggedIn__(JAVA_OBJECT me);
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isFacebookLoggedIn___R_boolean(JAVA_OBJECT me) {
    return com_codename1_impl_ios_IOSNative_isFacebookLoggedIn__(me);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isAsyncEditMode___R_boolean(JAVA_OBJECT instanceObject) {
    return com_codename1_impl_ios_IOSNative_isAsyncEditMode__(instanceObject);
}

#endif