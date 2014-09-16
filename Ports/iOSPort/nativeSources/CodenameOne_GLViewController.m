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
#import <QuartzCore/QuartzCore.h>
#import "CodenameOne_GLViewController.h"
#import "EAGLView.h"
#import "ExecutableOp.h"
#import "FillRect.h"
#import "ClipRect.h"
#import "DrawLine.h"
#import "DrawRect.h"
#import "FillPolygon.h"
#import "DrawString.h"
#import "DrawPath.h"
#import "DrawTextureAlphaMask.h"
#import "SetTransform.h"
#import "DrawImage.h"
#import "TileImage.h"
#import "GLUIImage.h"
#import "ResetAffine.h"
#import "Scale.h"
#import "Rotate.h"
#import <AudioToolbox/AudioToolbox.h>
#import "DrawGradientTextureCache.h"
#import "DrawStringTextureCache.h"
#import <CoreLocation/CoreLocation.h>
#include "com_codename1_impl_ios_IOSImplementation.h"
#import <MobileCoreServices/UTCoreTypes.h>
#include "com_codename1_payment_Product.h"
#include "com_codename1_ui_Display.h"
#include "com_codename1_impl_CodenameOneImplementation.h"
#include "com_codename1_ui_Component.h"
#import "CN1ES2compat.h"
#ifdef USE_ES2
#import <GLKit/GLKit.h>
#endif
#include "java_lang_System.h"

extern void repaintUI();
extern NSDate* currentDatePickerDate;
extern bool datepickerPopover;
//int lastWindowSize = -1;
extern void stringEdit(int finished, int cursorPos, NSString* text);
BOOL vkbAlwaysOpen = NO;
BOOL viewDidAppearRepaint = YES;
NSMutableArray* touchesArray = nil;

int nextPowerOf2(int val) {
    int i;
    for(i = 8 ; i < val ; i *= 2);
    return i;
}

int displayWidth = -1;
int displayHeight = -1;
UIView *editingComponent;
float editCompoentX, editCompoentY, editCompoentW, editCompoentH;
int editComponentPadTop, editComponentPadLeft;
BOOL firstTime = YES;
BOOL retinaBug;
float scaleValue = 1;
BOOL forceSlideUpField;

// 1 for portrait lock, and 2 for landscape lock
int orientationLock = 0;
int upsideDownMultiplier = -1;
int currentlyEditingMaxLength;

#ifndef CN1_USE_ARC
NSAutoreleasePool *globalCodenameOnePool;
#endif
void initVMImpl() {
#ifndef CN1_USE_ARC
    // initialize an auto release pool for the CodenameOne main thread
    globalCodenameOnePool = [[NSAutoreleasePool alloc] init];
#endif
}

void deinitVMImpl() {
#ifndef CN1_USE_ARC
    [globalCodenameOnePool release];
#endif
}

extern void pointerPressed(int* x, int* y, int length);
extern void pointerDragged(int* x, int* y, int length);
extern void pointerReleased(int* x, int* y, int length);
extern void screenSizeChanged(int width, int height);

void pointerPressedC(int* x, int* y, int length) {
    //NSLog(@"pointerPressedC started");
    pointerPressed(x, y, length);
    //NSLog(@"pointerPressedC finished");
}

void pointerDraggedC(int* x, int* y, int length) {
    //NSLog(@"pointerDraggedC started");
    pointerDragged(x, y, length);
    //NSLog(@"pointerDraggedC finished");
}
void pointerReleasedC(int* x, int* y, int length) {
    //NSLog(@"pointerReleasedC started");
    pointerReleased(x, y, length);
    //NSLog(@"pointerReleasedC finished");
}
void screenSizeChangedC(int width, int height) {
    //NSLog(@"screenSizeChangedC started");
    screenSizeChanged(width, height);
    //NSLog(@"screenSizeChangedC finished");
}

void* Java_com_codename1_impl_ios_IOSImplementation_createImageImpl
(void* data, int dataLength, int* widthAndHeightReturnValue) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_createImageImpl started for dataLength %i", dataLength);
    NSData* nd = [NSData dataWithBytes:data length:dataLength];
    UIImage* img = [UIImage imageWithData:nd];
    widthAndHeightReturnValue[0] = (int)img.size.width;
    widthAndHeightReturnValue[1] = (int)img.size.height;
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_createImageImpl finished width %i, height %i", (int)widthAndHeightReturnValue[0], (int)widthAndHeightReturnValue[1]);
    
#ifndef CN1_USE_ARC
    return [[GLUIImage alloc] initWithImage:img];
#else
    return (__bridge void*)[[GLUIImage alloc] initWithImage:img];
#endif
}

void Java_com_codename1_impl_ios_IOSImplementation_setImageName(void* nativeImage, const char* name) {
#ifndef CN1_USE_ARC
    GLUIImage* img = (GLUIImage*)nativeImage;
#else
    GLUIImage* img = (__bridge GLUIImage*)nativeImage;
#endif
    if(name != nil) {
        [img setName:[NSString stringWithUTF8String:name]];
    }
}

int isIPad() {
    return UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad;
}

void Java_com_codename1_impl_ios_IOSImplementation_editStringAtImpl
(CN1_THREAD_STATE_MULTI_ARG int x, int y, int w, int h, void* font, int isSingleLine, int rows, int maxSize,
 int constraint, const char* str, int len, BOOL forceSlideUp,
 int color, JAVA_LONG imagePeer, int padTop, int padBottom, int padLeft, int padRight, NSString* hintString, BOOL showToolbar) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_editStringAtImpl");
    currentlyEditingMaxLength = maxSize;
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(editingComponent != nil) {
            [editingComponent resignFirstResponder];
            [editingComponent removeFromSuperview];
#ifndef CN1_USE_ARC
            [editingComponent release];
#endif
            editingComponent = nil;
            if(vkbAlwaysOpen) {
                repaintUI();
            }
        }
        float scale = scaleValue;
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
        forceSlideUpField = forceSlideUp;
        CGRect rect = CGRectMake(editCompoentX, editCompoentY, editCompoentW, editCompoentH);
        if(isSingleLine) {
            UITextField* utf = [[UITextField alloc] initWithFrame:rect];
            editingComponent = utf;
            [utf setTextColor:UIColorFromRGB(color, 255)];
            
            if(hintString != nil) {
                utf.placeholder = hintString;
            }
            
            if((constraint & 0x80000) == 0x80000) {
                utf.autocorrectionType = UITextAutocorrectionTypeNo;
            }
            
            // PASSWORD
            if((constraint & 0x10000) == 0x10000) {
                utf.secureTextEntry = YES;
            }
            
            // EMAILADDR
            int cccc = constraint & 0xff;
            if(cccc == 1) {
                utf.keyboardType = UIKeyboardTypeEmailAddress;
                utf.autocapitalizationType = UITextAutocapitalizationTypeNone;
            } else {
                // NUMERIC
                if(cccc == 2) {
                    utf.keyboardType = UIKeyboardTypeNumberPad;
                } else {
                    // PHONENUMBER
                    if(cccc == 3) {
                        utf.keyboardType = UIKeyboardTypePhonePad;
                    } else {
                        // URL
                        if(cccc == 4) {
                            utf.keyboardType = UIKeyboardTypeURL;
                            utf.autocapitalizationType = UITextAutocapitalizationTypeNone;
                        } else {
                            // DECIMAL
                            if(cccc == 5) {
                                utf.keyboardType = UIKeyboardTypeDecimalPad;
                            }
                        }
                    }
                }
            }
            if(scale != 1) {
                float s = ((BRIDGE_CAST UIFont*)font).pointSize / scale;
                utf.font = [((BRIDGE_CAST UIFont*)font) fontWithSize:s];
            } else {
                utf.font = (BRIDGE_CAST UIFont*)font;
            }
            utf.text = [NSString stringWithUTF8String:str];
            utf.delegate = (EAGLView*)[CodenameOne_GLViewController instance].view;
            [utf setBackgroundColor:[UIColor clearColor]];
            
#ifndef NEW_CODENAME_ONE_VM
            JAVA_BOOLEAN isLastEdit = com_codename1_impl_ios_TextEditUtil_isLastEditComponent__();
#else
            JAVA_BOOLEAN isLastEdit = com_codename1_impl_ios_TextEditUtil_isLastEditComponent___R_boolean(CN1_THREAD_STATE_PASS_SINGLE_ARG);
#endif
            if (isLastEdit) {
                utf.returnKeyType = UIReturnKeyDone;
            } else {
                utf.returnKeyType = UIReturnKeyNext;
                if(vkbAlwaysOpen) {
                    if(utf.keyboardType != UIKeyboardTypeDecimalPad
                       && utf.keyboardType != UIKeyboardTypePhonePad
                       && utf.keyboardType != UIKeyboardTypeNumberPad) {
                        isLastEdit = YES;
                    }
                }
            }
            
            utf.borderStyle = UITextBorderStyleNone;
            [[NSNotificationCenter defaultCenter]
             addObserver:utf.delegate
             selector:@selector(textFieldDidChange)
             name:UITextFieldTextDidChangeNotification
             object:utf];
            
            if ((utf.keyboardType == UIKeyboardTypeDecimalPad
                 || utf.keyboardType == UIKeyboardTypePhonePad
                 || utf.keyboardType == UIKeyboardTypeNumberPad
                 || (utf.returnKeyType == UIReturnKeyNext && vkbAlwaysOpen)) && !isIPad()) {
                //add navigation toolbar to the top of the keyboard
                if(showToolbar) {
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
                    
                    NSString* buttonTitle;
                    
#ifndef NEW_CODENAME_ONE_VM
                    JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance__();
#else
                    JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance___R_com_codename1_ui_plaf_UIManager(CN1_THREAD_STATE_PASS_SINGLE_ARG);
#endif
                    JAVA_OBJECT str;
                    UIBarButtonItem *doneButton;
                    NSArray *itemsArray = nil;
                    if (isLastEdit) {
#ifndef NEW_CODENAME_ONE_VM
                        str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String(obj, fromNSString(@"done"), fromNSString(@"Done"));
#else
                        str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG obj, fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"done"), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"Done"));
#endif
                        buttonTitle = toNSString(CN1_THREAD_GET_STATE_PASS_ARG str);
                        doneButton = [[UIBarButtonItem alloc]initWithTitle:buttonTitle style:UIBarButtonItemStyleDone target:utf.delegate action:@selector(keyboardDoneClicked)];
                    } else {
#ifndef NEW_CODENAME_ONE_VM
                        str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String(obj, fromNSString(@"next"), fromNSString(@"Next"));
#else
                        str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG obj, fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"next"), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"Next"));
#endif
                        buttonTitle = toNSString(CN1_THREAD_GET_STATE_PASS_ARG str);
                        doneButton = [[UIBarButtonItem alloc]initWithTitle:buttonTitle style:UIBarButtonItemStyleDone target:utf.delegate action:@selector(keyboardNextClicked)];
                        if(vkbAlwaysOpen && (utf.keyboardType == UIKeyboardTypeDecimalPad
                                             || utf.keyboardType == UIKeyboardTypePhonePad
                                             || utf.keyboardType == UIKeyboardTypeNumberPad)) {
                            // we need both done and next
#ifndef NEW_CODENAME_ONE_VM
                            str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String(obj, fromNSString(@"done"), fromNSString(@"Done"));
#else
                            str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG obj, fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"done"), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"Done"));
#endif
                            buttonTitle = toNSString(CN1_THREAD_GET_STATE_PASS_ARG str);
                            UIBarButtonItem *anotherButton = [[UIBarButtonItem alloc]initWithTitle:buttonTitle style:UIBarButtonItemStyleDone target:utf.delegate action:@selector(keyboardDoneClicked)];
                            itemsArray = [NSArray arrayWithObjects: flexButton, anotherButton, doneButton, nil];
#ifndef CN1_USE_ARC
                            [anotherButton release];
#endif
                        }
                    }
                    
                    if(itemsArray == nil) {
                        itemsArray = [NSArray arrayWithObjects: flexButton, doneButton, nil];
                    }
                    
#ifndef CN1_USE_ARC
                    [flexButton release];
                    [doneButton release];
#endif
                    [toolbar setItems:itemsArray];
                    [utf setInputAccessoryView:toolbar];
                }
            }
        } else {
            UITextView* utv = [[UITextView alloc] initWithFrame:rect];
            [utv setBackgroundColor:[UIColor clearColor]];
            [utv.layer setBorderColor:[[UIColor clearColor] CGColor]];
            [utv.layer setBorderWidth:0];
            editingComponent = utv;
            if(scale != 1) {
                float s = ((BRIDGE_CAST UIFont*)font).pointSize / scale;
                utv.font = [((BRIDGE_CAST UIFont*)font) fontWithSize:s];
            } else {
                utv.font = (BRIDGE_CAST UIFont*)font;
            }
            utv.text = [NSString stringWithUTF8String:str];
            utv.delegate = (EAGLView*)[CodenameOne_GLViewController instance].view;
            
            if(showToolbar) {
                //add navigation toolbar to the top of the keyboard
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
                
                NSString* buttonTitle;
                
#ifndef NEW_CODENAME_ONE_VM
                JAVA_BOOLEAN isLastEdit = com_codename1_impl_ios_TextEditUtil_isLastEditComponent__();
                JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance__();
#else
                JAVA_BOOLEAN isLastEdit = com_codename1_impl_ios_TextEditUtil_isLastEditComponent___R_boolean(CN1_THREAD_STATE_PASS_SINGLE_ARG);
                JAVA_OBJECT obj = com_codename1_ui_plaf_UIManager_getInstance___R_com_codename1_ui_plaf_UIManager(CN1_THREAD_STATE_PASS_SINGLE_ARG);
#endif
                JAVA_OBJECT str;
                UIBarButtonItem *doneButton;
                if (isLastEdit) {
#ifndef NEW_CODENAME_ONE_VM
                    str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String(obj, fromNSString(@"done"), fromNSString(@"Done"));
#else
                    str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG obj, fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"done"), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"Done"));
#endif
                    buttonTitle = toNSString(CN1_THREAD_GET_STATE_PASS_ARG str);
                    doneButton = [[UIBarButtonItem alloc]initWithTitle:buttonTitle style:UIBarButtonItemStyleDone target:utv.delegate action:@selector(keyboardDoneClicked)];
                } else {
#ifndef NEW_CODENAME_ONE_VM
                    str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String(obj, fromNSString(@"next"), fromNSString(@"Next"));
#else
                    str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG obj, fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"next"), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @"Next"));
#endif
                    buttonTitle = toNSString(CN1_THREAD_GET_STATE_PASS_ARG str);
                    doneButton = [[UIBarButtonItem alloc]initWithTitle:buttonTitle style:UIBarButtonItemStyleDone target:utv.delegate action:@selector(keyboardNextClicked)];
                }
                NSArray *itemsArray = [NSArray arrayWithObjects: flexButton, doneButton, nil];
                
#ifndef CN1_USE_ARC
                [flexButton release];
                [doneButton release];
#endif
                [toolbar setItems:itemsArray];
                [utv setInputAccessoryView:toolbar];
            }
        }
        editingComponent.opaque = NO;
        [[CodenameOne_GLViewController instance].view addSubview:editingComponent];
        [editingComponent becomeFirstResponder];
        [[CodenameOne_GLViewController instance].view resignFirstResponder];
        [editingComponent setNeedsDisplay];
        
    });
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_editStringAtImpl finished");
}


#define SYSTEM_VERSION_LESS_THAN(v)                 ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] == NSOrderedAscending)

BOOL isRetinaBug() {
    return isIPad() && [[UIScreen mainScreen] scale] == 2 && SYSTEM_VERSION_LESS_THAN(@"6.0");
}

BOOL isRetina() {
    if([[UIScreen mainScreen] scale] > 1) {
        return !isRetinaBug();
    }
    return NO;
}

BOOL isIOS7() {
    return !SYSTEM_VERSION_LESS_THAN(@"7.0");
}




void* Java_com_codename1_impl_ios_IOSImplementation_createImageFromARGBImpl
(int* buffer, int width, int height) {
    size_t bufferLength = width * height * 4;
    size_t bitsPerComponent = 8;
    size_t bitsPerPixel = 32;
    size_t bytesPerRow = 4 * width;
    
    
    CGDataProviderRef provider = CGDataProviderCreateWithData(NULL, buffer, bufferLength, NULL);
    
    CGColorSpaceRef colorSpaceRef = CGColorSpaceCreateDeviceRGB();
    
    if(colorSpaceRef == NULL) {
        NSLog(@"Error allocating color space");
        CGDataProviderRelease(provider);
        return nil;
    }
    
    CGBitmapInfo bitmapInfo = kCGBitmapByteOrder32Little | kCGImageAlphaFirst;
    CGColorRenderingIntent renderingIntent = kCGRenderingIntentDefault;
    
    CGImageRef iref = CGImageCreate(width,
                                    height,
                                    bitsPerComponent,
                                    bitsPerPixel,
                                    bytesPerRow,
                                    colorSpaceRef,
                                    bitmapInfo,
                                    provider,	// data provider
                                    NULL,	// decode
                                    NO,	// should interpolate
                                    renderingIntent);
    
    uint32_t* pixels = (uint32_t*)malloc(bufferLength);
    
    if(pixels == NULL) {
        NSLog(@"Error: Memory not allocated for bitmap");
        CGDataProviderRelease(provider);
        CGColorSpaceRelease(colorSpaceRef);
        CGImageRelease(iref);
        return nil;
    }
    memset(pixels, 0, bufferLength);
    
    CGContextRef context = CGBitmapContextCreate(pixels,
                                                 width,
                                                 height,
                                                 bitsPerComponent,
                                                 bytesPerRow,
                                                 colorSpaceRef,
                                                 kCGBitmapByteOrder32Little | kCGImageAlphaPremultipliedFirst);
    
    if(context == NULL) {
        NSLog(@"Error context not created");
        free(pixels);
        return NULL;
    }
    
    UIImage *image = nil;
    if(context) {
        
        CGContextDrawImage(context, CGRectMake(0.0f, 0.0f, width, height), iref);
        
        CGImageRef imageRef = CGBitmapContextCreateImage(context);
        
        image = [UIImage imageWithCGImage:imageRef];
        
        CGImageRelease(imageRef);
        CGContextRelease(context);
    }
    
    CGColorSpaceRelease(colorSpaceRef);
    CGImageRelease(iref);
    CGDataProviderRelease(provider);
    
    if(pixels) {
        free(pixels);
    }
    
    return (BRIDGE_CAST void*) [[GLUIImage alloc] initWithImage:image];
}

void* Java_com_codename1_impl_ios_createImageFromAlphaMask(JAVA_BYTE* buffer, int width, int height, int color)
{
    size_t obufferLength = width * height * 4;
    size_t obitsPerComponent = 8;
    size_t obitsPerPixel = 32;
    size_t obytesPerRow = 4 * width;
    
    
    uint32_t* opixels = (uint32_t*)malloc(obufferLength);
    
    
    size_t ibufferLength = width * height;
    for ( size_t i=0; i<ibufferLength; i++){
        opixels[i] = color & (((uint32_t)buffer[i]) << 6);
    }
    void* out = Java_com_codename1_impl_ios_IOSImplementation_createImageFromARGBImpl(opixels, width, height);
    free(opixels);
    return out;
}

void* Java_com_codename1_impl_ios_IOSImplementation_scaleImpl
(void* peer, int width, int height) {
    // NOT used
    //UIImage* img = (UIImage*)peer;
    return 0;
}

int maxVal(int a, int b) {
    if(a > b) {
        return a;
    }
    return b;
}

CGContextRef roundRect(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
    [UIColorFromRGB(color, alpha) set];
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    CGRect rrect = CGRectMake(x, y, width, height);
    CGFloat radius = MAX(arcWidth, arcHeight);
    CGFloat minx = CGRectGetMinX(rrect), midx = CGRectGetMidX(rrect), maxx = CGRectGetMaxX(rrect);
    CGFloat miny = CGRectGetMinY(rrect), midy = CGRectGetMidY(rrect), maxy = CGRectGetMaxY(rrect);
    CGContextMoveToPoint(context, minx, midy);
    CGContextAddArcToPoint(context, minx, miny, midx, miny, radius);
    CGContextAddArcToPoint(context, maxx, miny, maxx, midy, radius);
    CGContextAddArcToPoint(context, maxx, maxy, midx, maxy, radius);
    CGContextAddArcToPoint(context, minx, maxy, minx, midy, radius);
    CGContextClosePath(context);
    return context;
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRoundRectMutableImpl
(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
    CGContextStrokePath(roundRect(color, alpha, x, y, width, height, arcWidth, arcHeight));
}

void Java_com_codename1_impl_ios_IOSImplementation_resetAffineGlobal() {
    ResetAffine* f = [[ResetAffine alloc] init];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
}

void Java_com_codename1_impl_ios_IOSImplementation_scale(float x, float y) {
    Scale* f = [[Scale alloc] initWithArgs:x yy:y];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
}

extern void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl
(void* peer, int alpha, int x, int y, int width, int height);


void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRoundRectGlobalImpl
(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), NO, 1.0);
    CGContextStrokePath(roundRect(color, alpha, 0, 0, width, height, arcWidth, arcHeight));
    UIImage* img = UIGraphicsGetImageFromCurrentImageContext();
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl %i", ((int)img));
    UIGraphicsEndImageContext();
    
    GLUIImage* glu = [[GLUIImage alloc] initWithImage:img];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl((BRIDGE_CAST void*) glu, 255, x, y, width, height);
#ifndef CN1_USE_ARC
    [glu release];
#endif
}


void Java_com_codename1_impl_ios_IOSImplementation_nativeFillRoundRectMutableImpl
(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
    CGContextFillPath(roundRect(color, alpha, x, y, width, height, arcWidth, arcHeight));
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeFillRoundRectGlobalImpl
(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), NO, 1.0);
    CGContextFillPath(roundRect(color, alpha, 0, 0, width, height, arcWidth, arcHeight));
    UIImage* img = UIGraphicsGetImageFromCurrentImageContext();
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl %i", ((int)img));
    UIGraphicsEndImageContext();
    
    GLUIImage* glu = [[GLUIImage alloc] initWithImage:img];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl((BRIDGE_CAST void*)glu, 255, x, y, width, height);
#ifndef CN1_USE_ARC
    [glu release];
#endif
}

#define PI 3.14159265358979323846
CGContextRef drawArc(int color, int alpha, int x, int y, int width, int height, int startAngle, int angle) {
    [UIColorFromRGB(color, alpha) set];
    CGContextRef context = UIGraphicsGetCurrentContext();
    if(width == height) {
        int radius = MIN(width, height) / 2;
        CGContextAddArc (context, x + radius, y + radius, radius, startAngle * PI / 180, (startAngle + angle) * PI / 180, 1);
    } else {
        CGRect rect = CGRectMake(x, y, width, height);
        CGContextAddEllipseInRect(context, rect);
    }
    return context;
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawArcMutableImpl
(int color, int alpha, int x, int y, int width, int height, int startAngle, int angle) {
    CGContextStrokePath(drawArc(color, alpha, x, y, width, height, startAngle, angle));
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeFillArcMutableImpl
(int color, int alpha, int x, int y, int width, int height, int startAngle, int angle) {
    CGContextFillPath(drawArc(color, alpha, x, y, width, height, startAngle, angle));
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawArcGlobalImpl
(int color, int alpha, int x, int y, int width, int height, int startAngle, int angle) {
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), NO, 1.0);
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawArcMutableImpl(color, alpha, 0, 0, width, height, startAngle, angle);
    UIImage* img = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    GLUIImage* glu = [[GLUIImage alloc] initWithImage:img];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl((BRIDGE_CAST void*)glu, 255, x, y, width, height);
#ifndef CN1_USE_ARC
    [glu release];
#endif
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeFillArcGlobalImpl
(int color, int alpha, int x, int y, int width, int height, int startAngle, int angle) {
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), NO, 1.0);
    Java_com_codename1_impl_ios_IOSImplementation_nativeFillArcMutableImpl(color, alpha, 0, 0, width, height, startAngle, angle);
    UIImage* img = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    GLUIImage* glu = [[GLUIImage alloc] initWithImage:img];
    Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl((BRIDGE_CAST void*)glu, 255, x, y, width, height);
#ifndef CN1_USE_ARC
    [glu release];
#endif
}

// START ES2 ADDITION: Drawing Shapes ------------------------------------------------------------------------------

void Java_com_codename1_impl_ios_IOSImplementation_fillConvexPolygonImpl(JAVA_OBJECT points, int color, int alpha)
{
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* pArray = points;
    JAVA_ARRAY_FLOAT* data = (JAVA_ARRAY_FLOAT*)pArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    int len = pArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
    JAVA_ARRAY_FLOAT* data = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)points)->data;
    int len = ((JAVA_ARRAY)points)->length;
#endif
    //NSLog(@"Len is %d", len);
    JAVA_FLOAT x[len/2];
    JAVA_FLOAT y[len/2];
    
    int j = 0;
    for ( int i=0; i<len; i+=2){
        
        x[j] = data[i];
        y[j] = data[i+1];
        j++;
    }
    
    
    
    FillPolygon *f = [[FillPolygon alloc] initWithArgs:x y:y num:(int)len/2 color:(int)color alpha:(int)alpha];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
}


void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawPathImpl
(Renderer * renderer, int color, int alpha, int x, int y, int w, int h)
{
    
    DrawPath *f = [[DrawPath alloc] initWithArgs:renderer color:color alpha:alpha];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
    // add to pipeline here
    
}


void Java_com_codename1_impl_ios_IOSImplementation_drawTextureAlphaMaskImpl(GLuint textureName, int color, int alpha, int x, int y, int w, int h)
{
    
    DrawTextureAlphaMask *f = [[DrawTextureAlphaMask alloc] initWithArgs:textureName color:color alpha:alpha x:x y:y w:w h:h];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
    
}

// END ES2 ADDITION -------------------------------------------------------------------------------------------------
void com_codename1_impl_ios_IOSImplementation_nativeSetTransformImpl___float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_float_int_int(JAVA_OBJECT instanceObject,
                                                                                                                                                                               JAVA_FLOAT a0, JAVA_FLOAT a1, JAVA_FLOAT a2, JAVA_FLOAT a3,
                                                                                                                                                                               JAVA_FLOAT b0, JAVA_FLOAT b1, JAVA_FLOAT b2, JAVA_FLOAT b3,
                                                                                                                                                                               JAVA_FLOAT c0, JAVA_FLOAT c1, JAVA_FLOAT c2, JAVA_FLOAT c3,
                                                                                                                                                                               JAVA_FLOAT d0, JAVA_FLOAT d1, JAVA_FLOAT d2, JAVA_FLOAT d3,
                                                                                                                                                                               JAVA_INT originX, JAVA_INT originY
                                                                                                                                                                               )
{
#ifdef USE_ES2
    //    dispatch_async(dispatch_get_main_queue(), ^{
    GLKMatrix4 m = GLKMatrix4MakeAndTranspose(a0,a1,a2,a3,
                                              b0,b1,b2,b3,
                                              c0,c1,c2,c3,
                                              d0,d1,d2,d3);
    
    SetTransform *f = [[SetTransform alloc] initWithArgs:m originX:originX originY:originY];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
    //    });
#endif
}


void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageMutableImpl
(void* peer, int alpha, int x, int y, int width, int height) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageMutableImpl %i started at %i, %i", (int)peer, x, y);
    UIImage* i = [(BRIDGE_CAST GLUIImage*)peer getImage];
    [i drawInRect:CGRectMake(x, y, width, height)];
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageMutableImpl finished");
}

int Java_com_codename1_impl_ios_IOSImplementation_stringWidthNativeImpl
(void* peer, const char* str, int len) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_stringWidthNativeImpl started");
    if(len == 0 || str == NULL) {
        return 0;
    }
    UIFont* f = (BRIDGE_CAST UIFont*)peer;
	NSString* s = [NSString stringWithUTF8String:str];
    //NSLog(@"String is %@", s);
    //NSLog(@"Font is %i", (int)f);
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_stringWidthNativeImpl finished");
    return (int)[s sizeWithFont:f].width;
}


int Java_com_codename1_impl_ios_IOSImplementation_charWidthNativeImpl
(void* peer, int chr) {
    UIFont* f = (BRIDGE_CAST UIFont*)peer;
    return [[NSString stringWithCharacters:((const unichar *)&chr) length:1] sizeWithFont:f].width;
}


int Java_com_codename1_impl_ios_IOSImplementation_getFontHeightNativeImpl
(void* peer) {
    UIFont* f = (BRIDGE_CAST UIFont*)peer;
    return (int)[f lineHeight];
}

void vibrateDevice() {
    AudioServicesPlayAlertSound(kSystemSoundID_Vibrate);
}

void* Java_com_codename1_impl_ios_IOSImplementation_createSystemFontImpl
(int face, int style, int size) {
	POOL_BEGIN();
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_createSystemFontImpl started");
    int pSize = 14;
    
    // size small
    if(size == 8) {
        pSize = 11;
    } else {
        // large
        if(size == 16) {
            pSize = 20;
        }
    }
    
    pSize *= scaleValue;
    
    UIFont* fnt;
    
    // if this is a monospace font
    if((face & 32) == 32) {
        fnt = [UIFont fontWithName:@"Courier" size:pSize];
    } else {
        // bold
        if((style & 1) == 1) {
            fnt = [UIFont boldSystemFontOfSize:pSize];
        } else {
            // italic
            if((style & 2) == 2) {
                fnt = [UIFont italicSystemFontOfSize:pSize];
            } else {
                fnt = [UIFont systemFontOfSize:pSize];
            }
        }
    }
#ifndef CN1_USE_ARC
    [fnt retain];
#endif
    POOL_END();
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_createSystemFontImpl finished %i", (int)fnt);
    return (BRIDGE_CAST void*)fnt;
}


/*
 * Class:     com_codename1_impl_ios_IOSImplementation
 * Method:    getDisplayWidth
 * Signature: ()I
 */
int Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl() {
    //if(displayWidth <= 0) {
    displayWidth = [CodenameOne_GLViewController instance].view.bounds.size.width * scaleValue;
    //}
    //NSLog(@"Display width %i", displayWidth);
    return displayWidth;
}

/*
 * Class:     com_codename1_impl_ios_IOSImplementation
 * Method:    getDisplayHeight
 * Signature: ()I
 */
int
Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl() {
    //GET_DIPLAY_HEIGHT_MARKER
    
    //if(displayHeight <= 0) {
    displayHeight = [CodenameOne_GLViewController instance].view.bounds.size.height * scaleValue;
    //}
    return displayHeight;
}


void Java_com_codename1_impl_ios_IOSImplementation_flushBufferImpl
(void* peer, int x, int y, int width, int height) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_flushBufferImpl started");
    [[CodenameOne_GLViewController instance] flushBuffer:(BRIDGE_CAST UIImage *)peer x:x y:y width:width height:height];
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_flushBufferImpl finished");
}


void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingMutableImpl
(int x, int y, int width, int height, int clipApplied) {
    CGContextRef context = UIGraphicsGetCurrentContext();
    //NSLog(@"Native mutable clipping applied %i on context %i x: %i y: %i width: %i height: %i", clipApplied, (int)context, x, y, width, height);
    //if(clipApplied) {
    CGContextRestoreGState(context);
    //}
    CGContextSaveGState(context);
    UIRectClip(CGRectMake(x, y, width, height));
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingMutableImpl finished");
}

void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingGlobalImpl
(int x, int y, int width, int height, int clipApplied) {
    //    NSLog(@"Native global clipping applied: %i x: %i y: %i width: %i height: %i", clipApplied, x, y, width, height);
    ClipRect* f = [[ClipRect alloc] initWithArgs:x ypos:y w:width h:height f:clipApplied];
    [[CodenameOne_GLViewController instance] upcomingAddClip:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingGlobalImpl finished");
}

void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingMaskGlobalImpl(JAVA_LONG textureName, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h)
{
#ifdef USE_ES2
    ClipRect* f = [[ClipRect alloc] initWithArgs:x ypos:y w:w h:h f:0 texture:textureName];
    [[CodenameOne_GLViewController instance] upcomingAddClip:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
#endif
}

void Java_com_codename1_impl_ios_IOSImplementation_setNativeClippingPolygonGlobalImpl(JAVA_OBJECT points)
{
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* pArray = points;
    JAVA_ARRAY_FLOAT* data = (JAVA_ARRAY_FLOAT*)pArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    int len = pArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
    JAVA_ARRAY_FLOAT* data = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)points)->data;
    int len = ((JAVA_ARRAY)points)->length;
#endif
    //NSLog(@"Len is %d", len);
    JAVA_FLOAT x[len/2];
    JAVA_FLOAT y[len/2];
    
    int j = 0;
    for ( int i=0; i<len; i+=2){
        
        x[j] = data[i];
        y[j] = data[i+1];
        j++;
    }
    
    ClipRect* f = [[ClipRect alloc] initWithPolygon:x y:y length:len/2];
    [[CodenameOne_GLViewController instance] upcomingAddClip:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
}


void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineMutableImpl
(int color, int alpha, int x1, int y1, int x2, int y2) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineMutableImpl started");
    [UIColorFromRGB(color, alpha) set];
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextMoveToPoint(context, x1, y1);
    CGContextAddLineToPoint(context, x2, y2);
    CGContextStrokePath(context);
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineMutableImpl finished");
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineGlobalImpl
(int color, int alpha, int x1, int y1, int x2, int y2) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineGlobalImpl started");
    DrawLine* f = [[DrawLine alloc] initWithArgs:color a:alpha xpos1:x1 ypos1:y1 xpos2:x2 ypos2:y2];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineGlobalImpl finished");
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeRotateGlobalImpl
(float angle, int x, int y) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineGlobalImpl started");
    Rotate* f = [[Rotate alloc] initWithArgs:angle xx:x yy:y];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawLineGlobalImpl finished");
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectMutableImpl
(int color, int alpha, int x, int y, int width, int height) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectMutableImpl started");
    [UIColorFromRGB(color, alpha) set];
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextFillRect(context, CGRectMake(x, y, width, height));
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectMutableImpl finished");
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectGlobalImpl
(int color, int alpha, int x, int y, int width, int height) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectGlobalImpl started");
    FillRect* f = [[FillRect alloc] initWithArgs:color a:alpha xpos:x ypos:y w:width h:height];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeFillRectGlobalImpl finished");
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectMutableImpl
(int color, int alpha, int x, int y, int width, int height) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectMutableImpl started");
    [UIColorFromRGB(color, alpha) set];
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextStrokeRect(context, CGRectMake(x, y, width, height));
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectMutableImpl finished");
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectGlobalImpl
(int color, int alpha, int x, int y, int width, int height) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectImpl started");
    DrawRect* f = [[DrawRect alloc] initWithArgs:color a:alpha xpos:x ypos:y w:width h:height];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawRectImpl finished");
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringMutableImpl
(int color, int alpha, void* fontPeer, NSString* str, int x, int y) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringMutableImpl started");
    [[CodenameOne_GLViewController instance] drawString:color alpha:alpha font:(BRIDGE_CAST UIFont*)fontPeer str:str x:x y:y];
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringMutableImpl finished");
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringGlobalImpl
(int color, int alpha, void* fontPeer, NSString* str, int x, int y) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringImpl started");
    DrawString* f = [[DrawString alloc] initWithArgs:color a:alpha xpos:x ypos:y s:str f:(BRIDGE_CAST UIFont*)fontPeer];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawStringImpl finished");
}

void* Java_com_codename1_impl_ios_IOSImplementation_createNativeMutableImageImpl
(int width, int height, int argb) {
    //NSLog(@"createNativeMutableImageImpl");
    BOOL opaque = ((argb & 0xff000000) == 0xff000000);
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), opaque, 1.0);
    [UIColorFromARGB(argb) set];
    UIRectFill(CGRectMake(0, 0, width, height));
    UIImage* img = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    //[img retain];
    //NSLog(@"createNativeMutableImageImpl finished %i ", (int)img);
    GLUIImage* gl = [[GLUIImage alloc] initWithImage:img];
    return (BRIDGE_CAST void*)gl;
}

void Java_com_codename1_impl_ios_IOSImplementation_startDrawingOnImageImpl
(int width, int height, void *peer) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_startDrawingOnImageImpl");
    UIImage* original = [(BRIDGE_CAST GLUIImage*)peer getImage];
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), NO, 1.0);
    if(original != NULL) {
        [original drawAtPoint:CGPointZero];
    }
    
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextSaveGState(context);
    [CodenameOne_GLViewController instance].currentMutableImage = (BRIDGE_CAST GLUIImage*)peer;
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_startDrawingOnImageImpl finished");
}

void* Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl() {
    UIImage* img = UIGraphicsGetImageFromCurrentImageContext();
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl %i", ((int)img));
    UIGraphicsEndImageContext();
    GLUIImage *gl = [CodenameOne_GLViewController instance].currentMutableImage;
    [gl setImage:img];
    [CodenameOne_GLViewController instance].currentMutableImage = nil;
    return (BRIDGE_CAST void*)gl;
}

void Java_com_codename1_impl_ios_IOSImplementation_imageRgbToIntArrayImpl
(void* peer, int* arr, int x, int y, int width, int height) {
    if(((BRIDGE_CAST void*)[CodenameOne_GLViewController instance].currentMutableImage) == peer) {
        Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl();
    }
    // set all pixels to transparent white to solve http://code.google.com/p/codenameone/issues/detail?id=923
    for(int iter = 0 ; iter < width * height ; iter++) {
        arr[iter] = 0xffffff;
    }
    UIImage* img = [(BRIDGE_CAST GLUIImage*)peer getImage];
    CGColorSpaceRef coloSpaceRgb = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(arr, width, height, 8, width * 4,
                                                 coloSpaceRgb,
                                                 kCGBitmapByteOrder32Little | kCGImageAlphaPremultipliedFirst);
    CGRect r = CGRectMake(-x, -(img.size.height - y - height), img.size.width, img.size.height);
    CGImageRef cgImg = [img CGImage];
    CGContextDrawImage(context, r, cgImg);
    CGColorSpaceRelease(coloSpaceRgb);
    CGContextRelease(context);
}


void Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl
(void* peer, int alpha, int x, int y, int width, int height) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl %i started at %i, %i", (int)peer, x, y);
    if(((BRIDGE_CAST void*)[CodenameOne_GLViewController instance].currentMutableImage) == peer) {
        Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl();
    }
    DrawImage* f = [[DrawImage alloc] initWithArgs:alpha xpos:x ypos:y i:(BRIDGE_CAST GLUIImage*)peer w:width h:height];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeDrawImageGlobalImpl finished");
}

void Java_com_codename1_impl_ios_IOSImplementation_nativeTileImageGlobalImpl
(void* peer, int alpha, int x, int y, int width, int height) {
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeTileImageGlobalImpl %i started at %i, %i", (int)peer, x, y);
    if(((BRIDGE_CAST void*)[CodenameOne_GLViewController instance].currentMutableImage) == peer) {
        Java_com_codename1_impl_ios_IOSImplementation_finishDrawingOnImageImpl();
    }
    TileImage* f = [[TileImage alloc] initWithArgs:alpha xpos:x ypos:y i:(BRIDGE_CAST GLUIImage*)peer w:width h:height];
    [CodenameOne_GLViewController upcoming:f];
#ifndef CN1_USE_ARC
    [f release];
#endif
    //NSLog(@"Java_com_codename1_impl_ios_IOSImplementation_nativeTileImageGlobalImpl finished");
}

void Java_com_codename1_impl_ios_IOSImplementation_deleteNativePeerImpl(void* peer) {
    GLUIImage* original = (BRIDGE_CAST GLUIImage*)peer;
    //NSLog(@"deleteNativePeerImpl retainCount: %i", [original retainCount]);
#ifndef CN1_USE_ARC
    [original release];
#endif
}

void Java_com_codename1_impl_ios_IOSImplementation_deleteNativeFontPeerImpl(void* peer) {
    UIFont* original = (BRIDGE_CAST UIFont*)peer;
    //NSLog(@"deleteNativeFontPeerImpl retainCount: %i", [original retainCount]);
#ifndef CN1_USE_ARC
    [original release];
#endif
}

void loadResourceFile
(const char* name, int nameLen, const char* type, int typeLen, void* data) {
    //NSLog(@"loadResourceFile started");
    NSString* path = [[NSBundle mainBundle] pathForResource:[NSString stringWithUTF8String:name] ofType:[NSString stringWithUTF8String:type]];
    NSData* iData = [NSData dataWithContentsOfFile:path];
    [iData getBytes:data];
    //NSLog(@"loadResourceFile finished");
}

int getResourceSize(const char* name, int nameLen, const char* type, int typeLen) {
    NSString* nameNS = [NSString stringWithUTF8String:name];
    NSString* typeNS = [NSString stringWithUTF8String:type];
    //NSLog(@"getResourceSize %@ %@ started", nameNS, typeNS);
    NSString* path = [[NSBundle mainBundle] pathForResource:nameNS ofType:typeNS];
    if(path == nil) {
        return -1;
    }
    NSData* iData = [NSData dataWithContentsOfFile:path];
    int size = [iData length];
    //NSLog(@"getResourceSize %i finished", size);
    return size;
}


int isPainted() {
    if([[CodenameOne_GLViewController instance] isPaintFinished]) {
        return 1;
    }
    return 0;
}

// Uniform index.
enum {
    UNIFORM_TRANSLATE,
    NUM_UNIFORMS
};
GLint uniforms[NUM_UNIFORMS];

// Attribute index.
enum {
    ATTRIB_VERTEX,
    ATTRIB_COLOR,
    NUM_ATTRIBUTES
};

@interface CodenameOne_GLViewController ()
@property (nonatomic, retain) EAGLContext *context;
@property (nonatomic, assign) CADisplayLink *displayLink;
- (BOOL)loadShaders;
- (BOOL)compileShader:(GLuint *)shader type:(GLenum)type file:(NSString *)file;
- (BOOL)linkProgram:(GLuint)prog;
- (BOOL)validateProgram:(GLuint)prog;
@end

@implementation CodenameOne_GLViewController

@synthesize context, displayLink, currentMutableImage, animating;
static CodenameOne_GLViewController *sharedSingleton;
+(BOOL)isDrawTextureSupported {
    return sharedSingleton->drawTextureSupported;
}

#ifdef INCLUDE_MOPUB
@synthesize adView;
- (void)viewDidLoad {
#ifndef CN1_USE_ARC
    if(isIPad()) {
        self.adView = [[[MPAdView alloc] initWithAdUnitId:MOPUB_TABLET_AD_UNIT
                                                     size:MOPUB_TABLET_AD_SIZE] autorelease];
    } else {
        self.adView = [[[MPAdView alloc] initWithAdUnitId:MOPUB_AD_UNIT
                                                     size:MOPUB_AD_SIZE] autorelease];
    }
#else
    if(isIPad()) {
        self.adView = [[MPAdView alloc] initWithAdUnitId:MOPUB_TABLET_AD_UNIT
                                                    size:MOPUB_TABLET_AD_SIZE];
    } else {
        self.adView = [[MPAdView alloc] initWithAdUnitId:MOPUB_AD_UNIT
                                                    size:MOPUB_AD_SIZE];
    }
#endif
    self.adView.delegate = self;
    CGRect frame = self.adView.frame;
    CGSize size = [self.adView adContentViewSize];
    frame.origin.y = [[UIScreen mainScreen] applicationFrame].size.height - size.height;
    self.adView.frame = frame;
    [self.view addSubview:self.adView];
    [self.adView loadAd];
    [super viewDidLoad];
    //replaceViewDidLoad
}

#pragma mark - <MPAdViewDelegate>
- (UIViewController *)viewControllerForPresentingModalView {
    return self;
}
#else
- (void)viewDidLoad {
    [super viewDidLoad];
    //replaceViewDidLoad
}
#endif

bool lockDrawing;
- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self becomeFirstResponder];
    if(viewDidAppearRepaint) {
        if(animated) {
            // postpone this to the next edt cycle to prevent a black screen
            dispatch_async(dispatch_get_main_queue(), ^{
                repaintUI();
            });
        }
    }
    if(touchesArray != nil) {
        [touchesArray removeAllObjects];
    }
    int currentWidth = (int)self.view.bounds.size.width * scaleValue;
    if(currentWidth != displayWidth) {
        [(EAGLView *)self.view updateFrameBufferSize:(int)self.view.bounds.size.width h:(int)self.view.bounds.size.height];
        displayWidth = currentWidth;
        displayHeight = (int)self.view.bounds.size.height * scaleValue;
        screenSizeChanged(displayWidth, displayHeight);
    }
    
    //replaceViewDidAppear
}

- (BOOL)canBecomeFirstResponder {
    return YES;
}

- (void)remoteControlReceivedWithEvent:(UIEvent *)receivedEvent {
    if (receivedEvent.type == UIEventTypeRemoteControl) {
        JAVA_OBJECT o = com_codename1_ui_Display_getInstance__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
#ifndef NEW_CODENAME_ONE_VM
        o = com_codename1_ui_Display_getImplementation__(o);
#else
        o = com_codename1_ui_Display_getImplementation___R_com_codename1_impl_CodenameOneImplementation(CN1_THREAD_GET_STATE_PASS_ARG o);
#endif
        switch (receivedEvent.subtype) {
            case UIEventSubtypeRemoteControlPlay:
            case UIEventSubtypeRemoteControlPause:
            case UIEventSubtypeRemoteControlTogglePlayPause:
                NSLog(@"Play or stop invoked");
                com_codename1_impl_CodenameOneImplementation_keyPressed___int(CN1_THREAD_GET_STATE_PASS_ARG o, -24);
                com_codename1_impl_CodenameOneImplementation_keyReleased___int(CN1_THREAD_GET_STATE_PASS_ARG o, -24);
                break;
                
            case UIEventSubtypeRemoteControlPreviousTrack:
                NSLog(@"Previous invoked");
                com_codename1_impl_CodenameOneImplementation_keyPressed___int(CN1_THREAD_GET_STATE_PASS_ARG o, -21);
                com_codename1_impl_CodenameOneImplementation_keyReleased___int(CN1_THREAD_GET_STATE_PASS_ARG o, -21);
                break;
                
            case UIEventSubtypeRemoteControlNextTrack:
                NSLog(@"Next invoked");
                com_codename1_impl_CodenameOneImplementation_keyPressed___int(CN1_THREAD_GET_STATE_PASS_ARG o, -20);
                com_codename1_impl_CodenameOneImplementation_keyReleased___int(CN1_THREAD_GET_STATE_PASS_ARG o, -20);
                break;
                
            default:
                break;
                
        }
    }
}

#ifdef USE_ES2
extern GLKMatrix4 CN1transformMatrix;
#endif

- (void)awakeFromNib
{
#ifdef USE_ES2
    CN1transformMatrix = GLKMatrix4Identity;
#endif
    retinaBug = isRetinaBug();
    if(retinaBug) {
        scaleValue = 1;
    } else {
        scaleValue = [UIScreen mainScreen].scale;
    }
    sharedSingleton = self;
    [self initVars];
#ifdef USE_ES2
    EAGLContext *aContext = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
#else
    EAGLContext *aContext = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES1];
    
    if (!aContext) {
        aContext = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES1];
    }
#endif
    if (!aContext)
        NSLog(@"Failed to create ES context");
    else if (![EAGLContext setCurrentContext:aContext])
        NSLog(@"Failed to set ES context current");
    
	self.context = aContext;
#ifndef CN1_USE_ARC
    [aContext release];
#endif
	
    [(EAGLView *)self.view setContext:context];
    [(EAGLView *)self.view setFramebuffer];
    //self.view.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    //self.view.autoresizesSubviews = YES;
    
    //    if ([context API] == kEAGLRenderingAPIOpenGLES2)
    //        [self loadShaders];
    
    
    animating = FALSE;
    animationFrameInterval = 1;
    self.displayLink = nil;
    
    const char* extensions = (const char*)glGetString(GL_EXTENSIONS);
    drawTextureSupported = extensions == 0 || strstr(extensions, "OES_draw_texture") != 0;
    //NSLog(@"Draw texture extension %i", (int)drawTextureSupported);
    
    // register for keyboard notifications
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(keyboardWillShow:)
                                                 name:UIKeyboardWillShowNotification
                                               object:self.view.window];
    // register for keyboard notifications
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(keyboardWillHide:)
                                                 name:UIKeyboardWillHideNotification
                                               object:self.view.window];
    
    //detect orientation by statusBarOrientation
    UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];
    bool isPortrait = (orientation == UIInterfaceOrientationPortrait || orientation == UIInterfaceOrientationPortraitUpsideDown);
    
    UIImage *img = nil;
    if(isIPad()) {
        if(scaleValue > 1) {
            if(!isPortrait) {
                img = [UIImage imageNamed:@"Default-Landscape@2x.png"];
            } else {
                img = [UIImage imageNamed:@"Default-Portrait@2x.png"];
            }
        } else {
            if(!isPortrait) {
                NSString* str = [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:@"Default-Landscape.png"];
                NSData* iData = [NSData dataWithContentsOfFile:str];
                img = [[UIImage alloc] initWithData:iData];
            } else {
                NSString* str = [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:@"Default-Portrait.png"];
                NSData* iData = [NSData dataWithContentsOfFile:str];
                img = [[UIImage alloc] initWithData:iData];
            }
        }
    } else {
        /*if([UIScreen mainScreen].scale > 1) {
         if(Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl() > 960) {
         img = [UIImage imageNamed:@"Default@2x.png"];
         } else {
         img = [UIImage imageNamed:@"Default@2x.png"];
         }
         } else {
         img = [UIImage imageNamed:@"Default.png"];
         }*/
        if(Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl() > 960) {
            int largest = MAX(Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl(), Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl());
            
            // iphone 5x
            if(largest < 1200) {
                img = [UIImage imageNamed:@"Default-568h@2x.png"];
            } else {
                // iphone 6
                if(largest < 1400) {
                    img = [UIImage imageNamed:@"Default-667h@2x.png"];
                } else {
                    bool isPortrait = (orientation == UIInterfaceOrientationPortrait || orientation == UIInterfaceOrientationPortraitUpsideDown);
                    // iphone 6+
                    if(isPortrait) {
                        img = [UIImage imageNamed:@"Default-736h@3x.png"];
                    } else {
                        img = [UIImage imageNamed:@"Default-736h-Landscape@3x.png"];
                    }
                }
            }
        } else {
            img = [UIImage imageNamed:@"Default.png"];
        }
    }
    [self.view setMultipleTouchEnabled:YES];
    if(img != nil) {
        float scale = scaleValue;
        DrawImage* dr;
        GLUIImage* gl;
        
        int wi = Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl();
        int he = Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl();
        
        //some hacking to scale launch image so that it will be drawn correctly
        GLfloat xScale = 1;
        int statusbarHeight = 20;
        if(isIOS7()) {
            statusbarHeight = 0;
        }
        if(isPortrait) {
            //add statusbar fix 20 pix only if not an iPad a iPad Launch images height is without statusbar
            CGImageRef imageRef = CGImageCreateWithImageInRect([img CGImage], CGRectMake(0, statusbarHeight * scale, wi, he));
            img = [UIImage imageWithCGImage:imageRef];
            CGImageRelease(imageRef);
            
            gl = [[GLUIImage alloc] initWithImage:img];
            dr = [[DrawImage alloc] initWithArgs:255 xpos:0 ypos:0 i:gl w:img.size.width h:img.size.height];
            [(EAGLView *)self.view setFramebuffer];
        } else {
            //add statusbar fix 20 pix only if not an iPad a iPad Launch images height is without statusbar
            CGImageRef imageRef = CGImageCreateWithImageInRect([img CGImage], CGRectMake(0, statusbarHeight * scale, he, wi));
            img = [UIImage imageWithCGImage:imageRef];
            CGImageRelease(imageRef);
            
            gl = [[GLUIImage alloc] initWithImage:img];
            int imgHeight = img.size.height;
            int imgWidth = img.size.width;
            dr = [[DrawImage alloc] initWithArgs:255 xpos:0 ypos:0 i:gl w:imgHeight h:imgWidth];
            NSLog(@"Drew image on %i, %i for display %i, %i", imgHeight, imgWidth, wi, he);
            [(EAGLView *)self.view setFramebuffer];
        }
        
        GLErrorLog;
        
        _glScalef(xScale, -1, 1);
        GLErrorLog;
        _glTranslatef(0, -he, 0);
        GLErrorLog;
        
        [dr execute];
#ifndef CN1_USE_ARC
        [gl release];
#endif
        
        _glTranslatef(0, he, 0);
        GLErrorLog;
        
        _glScalef(xScale, -1, 1);
        GLErrorLog;
        
        [(EAGLView *)self.view presentFramebuffer];
        GLErrorLog;
    }
    [[SKPaymentQueue defaultQueue] addTransactionObserver:[CodenameOne_GLViewController instance]];
}

BOOL patch = NO;
int keyboardSlideOffset;
int keyboardHeight;
- (void)keyboardWillHide:(NSNotification *)n
{
    @synchronized([CodenameOne_GLViewController instance]) {
        [currentTarget removeAllObjects];
    }
    keyboardIsShown = NO;
    if(!modifiedViewHeight || vkbAlwaysOpen) {
        return;
    }
    com_codename1_impl_ios_IOSImplementation_paintNow__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    NSDictionary* userInfo = [n userInfo];
    
    // get the size of the keyboard
    NSValue* boundsValue = [userInfo objectForKey:UIKeyboardBoundsUserInfoKey];
    CGSize keyboardSize = [boundsValue CGRectValue].size;
    
    
    // resize the scrollview
    CGRect viewFrame = self.view.frame;
    // I'm also subtracting a constant kTabBarHeight because my UIScrollView was offset by the UITabBar so really only the portion of the keyboard that is leftover pass the UITabBar is obscuring my UIScrollView.
    
    keyboardSlideOffset = 0;
    if(patch) {
        if(displayHeight > displayWidth) {
            viewFrame.origin.y += keyboardSize.height / 3 * upsideDownMultiplier;
        } else {
            viewFrame.origin.x -= keyboardSize.height / 3 * upsideDownMultiplier;
        }
    } else {
        if(displayHeight > displayWidth) {
            viewFrame.origin.y += keyboardSize.height * upsideDownMultiplier;
        } else {
            viewFrame.origin.x -= keyboardSize.height * upsideDownMultiplier;
        }
    }
    /*float y = editingComponent.frame.origin.y;
     y += keyboardSize.height;
     editingComponent.frame = CGRectMake(editCompoentX, y, editCompoentW, editCompoentH);*/
    
    [UIView beginAnimations:nil context:NULL];
    [UIView setAnimationBeginsFromCurrentState:YES];
    [UIView setAnimationDuration:0.3];
    [self.view setFrame:viewFrame];
    [UIView commitAnimations];
}

- (void)keyboardWillShow:(NSNotification *)n
{
    NSDictionary* userInfo = [n userInfo];
    
    // get the size of the keyboard
    NSValue* boundsValue = [userInfo objectForKey:UIKeyboardBoundsUserInfoKey];
    CGSize keyboardSize = [boundsValue CGRectValue].size;
    keyboardHeight = keyboardSize.height;
    
    // This is an ivar I'm using to ensure that we do not do the frame size adjustment on the UIScrollView if the keyboard is already shown.  This can happen if the user, after fixing editing a UITextField, scrolls the resized UIScrollView to another UITextField and attempts to edit the next UITextField.  If we were to resize the UIScrollView again, it would be disastrous.  NOTE: The keyboard notification will fire even when the keyboard is already shown.
    if (keyboardIsShown || vkbAlwaysOpen) {
        return;
    }
    
    // resize the noteView
    CGRect viewFrame = self.view.frame;
    // I'm also subtracting a constant kTabBarHeight because my UIScrollView was offset by the UITabBar so really only the portion of the keyboard that is leftover pass the UITabBar is obscuring my UIScrollView.
    
    patch = NO;
    keyboardSlideOffset = 0;
    if(editCompoentY + editCompoentH < displayHeight / scaleValue - keyboardSize.height) {
        if(!forceSlideUpField) {
            modifiedViewHeight = NO;
            return;
        } else {
            patch = YES;
        }
    } else {
        if(editCompoentY < keyboardSize.height) {
            patch = YES;
        }
    }
    modifiedViewHeight = YES;
    
    if(patch) {
        if(displayHeight > displayWidth) {
            viewFrame.origin.y -= (keyboardSize.height / 3) * upsideDownMultiplier;
            keyboardSlideOffset = -(keyboardSize.height / 3) * upsideDownMultiplier;
        } else {
            viewFrame.origin.x += (keyboardSize.height / 3) * upsideDownMultiplier;
            keyboardSlideOffset = (keyboardSize.height / 3) * upsideDownMultiplier;
        }
    } else {
        if(displayHeight > displayWidth) {
            viewFrame.origin.y -= keyboardSize.height * upsideDownMultiplier;
            keyboardSlideOffset = -keyboardSize.height * upsideDownMultiplier;
        } else {
            viewFrame.origin.x += keyboardSize.height * upsideDownMultiplier;
            keyboardSlideOffset = keyboardSize.height * upsideDownMultiplier;
        }
    }
    
    /*float y = editingComponent.frame.origin.y;
     y -= keyboardSize.height;
     editingComponent.frame = CGRectMake(editCompoentX, y, editCompoentW, editCompoentH);*/
    
    [UIView beginAnimations:nil context:NULL];
    [UIView setAnimationBeginsFromCurrentState:YES];
    [UIView setAnimationDuration:0.3];
    [self.view setFrame:viewFrame];
    [UIView commitAnimations];
    
    keyboardIsShown = YES;
}


- (void)dealloc
{
    if (program) {
        glDeleteProgram(program);
        program = 0;
    }
    
    // Tear down context.
    if ([EAGLContext currentContext] == context)
        [EAGLContext setCurrentContext:nil];
    
#ifndef CN1_USE_ARC
    [context release];
#endif
    
#ifdef INCLUDE_MOPUB
    self.adView = nil;
#endif
    
#ifndef CN1_USE_ARC
    [super dealloc];
#endif
}

- (void)didReceiveMemoryWarning
{
    // Releases the view if it doesn't have a superview.
    //[super didReceiveMemoryWarning];
    com_codename1_impl_ios_IOSImplementation_flushSoftRefMap__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
#ifndef NEW_CODENAME_ONE_VM
    GC_gcollect_and_unmap();
#else
    java_lang_System_gc__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
#endif
    
    // Release any cached data, images, etc. that aren't in use.
}

- (void)viewWillAppear:(BOOL)animated
{
    [self startAnimation];
    [super viewWillAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated
{
    [self stopAnimation];
    
    [super viewWillDisappear:animated];
}

- (void)viewDidUnload
{
	[super viewDidUnload];
	
    if (program) {
        glDeleteProgram(program);
        program = 0;
    }
    
    // Tear down context.
    if ([EAGLContext currentContext] == context)
        [EAGLContext setCurrentContext:nil];
	self.context = nil;
}

- (NSInteger)animationFrameInterval
{
    return animationFrameInterval;
}

- (void)setAnimationFrameInterval:(NSInteger)frameInterval
{
    if (frameInterval >= 1) {
        animationFrameInterval = frameInterval;
        
        if (animating) {
            [self stopAnimation];
            [self startAnimation];
        }
    }
}

- (void)startAnimation
{
    /*if (!animating) {
     CADisplayLink *aDisplayLink = [[UIScreen mainScreen] displayLinkWithTarget:self selector:@selector(drawScreen)];
     [aDisplayLink setFrameInterval:animationFrameInterval];
     [aDisplayLink addToRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
     self.displayLink = aDisplayLink;
     
     animating = TRUE;
     }*/
}

- (void)stopAnimation
{
    if (animating) {
        [self.displayLink invalidate];
        self.displayLink = nil;
        animating = FALSE;
    }
}

- (void)drawScreen {
    [self drawFrame:[CodenameOne_GLViewController instance].view.bounds];
}

- (void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration {
    if(firstTime) {
        return;
    }
    //WILL_ROTATE_TO_INTERFACE_MARKER
    if(editingComponent != nil) {
        if([editingComponent isKindOfClass:[UITextView class]]) {
            UITextView* v = (UITextView*)editingComponent;
            stringEdit(YES, -1, v.text);
        } else {
            UITextField* v = (UITextField*)editingComponent;
            stringEdit(YES, -1, v.text);
        }
        [editingComponent resignFirstResponder];
        [editingComponent removeFromSuperview];
#ifndef CN1_USE_ARC
        [editingComponent release];
#endif
        editingComponent = nil;
        displayWidth = (int)self.view.bounds.size.width * scaleValue;
        displayHeight = (int)self.view.bounds.size.height * scaleValue;
    }
    @synchronized([CodenameOne_GLViewController instance]) {
        [currentTarget removeAllObjects];
        lockDrawing = YES;
    }
    com_codename1_impl_ios_IOSImplementation_paintNow__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    @synchronized([CodenameOne_GLViewController instance]) {
        [currentTarget addObjectsFromArray:upcomingTarget];
        [upcomingTarget removeAllObjects];
    }
    [self drawFrame:CGRectMake(0, 0, Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl(), Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl())];
#ifdef INCLUDE_MOPUB
    [self.adView rotateToOrientation:toInterfaceOrientation];
#endif
}

- (BOOL)shouldAutorotate {
    UIInterfaceOrientation interfaceOrientation = [[UIDevice currentDevice] orientation];
    upsideDownMultiplier = 1;
    switch (orientationLock) {
        case 0:
            if(interfaceOrientation == UIInterfaceOrientationLandscapeLeft || interfaceOrientation == UIInterfaceOrientationMaskPortraitUpsideDown) {
                upsideDownMultiplier = -1;
            }
            return YES;
            
        case 1:
            if(interfaceOrientation == UIInterfaceOrientationPortrait) {
                if(interfaceOrientation == UIInterfaceOrientationLandscapeLeft || interfaceOrientation == UIInterfaceOrientationMaskPortraitUpsideDown) {
                    upsideDownMultiplier = -1;
                }
                return YES;
            }
            return NO;
            
        default:
            if(interfaceOrientation == UIInterfaceOrientationLandscapeLeft || interfaceOrientation == UIInterfaceOrientationLandscapeRight) {
                if(interfaceOrientation == UIInterfaceOrientationLandscapeLeft || interfaceOrientation == UIInterfaceOrientationMaskPortraitUpsideDown) {
                    upsideDownMultiplier = -1;
                }
                return YES;
            }
    }
    return NO;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    upsideDownMultiplier = 1;
    switch (orientationLock) {
        case 0:
            if(interfaceOrientation == UIInterfaceOrientationLandscapeLeft || interfaceOrientation == UIInterfaceOrientationMaskPortraitUpsideDown) {
                upsideDownMultiplier = -1;
            }
            return YES;
            
        case 1:
            if(interfaceOrientation == UIInterfaceOrientationPortrait) {
                if(interfaceOrientation == UIInterfaceOrientationLandscapeLeft || interfaceOrientation == UIInterfaceOrientationMaskPortraitUpsideDown) {
                    upsideDownMultiplier = -1;
                }
                return YES;
            }
            return NO;
            
        default:
            if(interfaceOrientation == UIInterfaceOrientationLandscapeLeft || interfaceOrientation == UIInterfaceOrientationLandscapeRight) {
                if(interfaceOrientation == UIInterfaceOrientationLandscapeLeft || interfaceOrientation == UIInterfaceOrientationMaskPortraitUpsideDown) {
                    upsideDownMultiplier = -1;
                }
                return YES;
            }
    }
    return NO;
}

-(void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation {
    [(EAGLView *)self.view updateFrameBufferSize:(int)self.view.bounds.size.width h:(int)self.view.bounds.size.height];
    displayWidth = (int)self.view.bounds.size.width * scaleValue;
    displayHeight = (int)self.view.bounds.size.height * scaleValue;
    lockDrawing = NO;
    screenSizeChanged(displayWidth, displayHeight);
#ifdef INCLUDE_MOPUB
    CGSize size = [self.adView adContentViewSize];
    CGFloat centeredX = (self.view.bounds.size.width - size.width) / 2;
    CGFloat bottomAlignedY = self.view.bounds.size.height - size.height;
    self.adView.frame = CGRectMake(centeredX, bottomAlignedY, size.width, size.height);
#endif
    
    //DID_ROTATE_FROM_INTERFACE_MARKER
}

//INJECT_METHODS_MARKER

//static UIImage *img = nil;
//static GLUIImage* glut = nil;

- (void)drawFrame:(CGRect)rect
{
    if([UIApplication sharedApplication].applicationState != UIApplicationStateActive) {
        return;
    }
    [(EAGLView *)self.view setFramebuffer];
    GLErrorLog;
    if(currentTarget != nil) {
        if([currentTarget count] > 0) {
            [ClipRect setDrawRect:rect];
            //NSLog(@"Clipping rect to: %i, %i, %i %i", (int)rect.origin.x, (int)rect.origin.y, (int)rect.size.width, (int)rect.size.height );
            _glScalef(1, -1, 1);
            GLErrorLog;
            _glTranslatef(0, -displayHeight, 0);
            GLErrorLog;
            
            /*if(((int)rect.size.width) != displayWidth || ((int)rect.size.height) != displayHeight) {
             glScissor(rect.origin.x, displayHeight - rect.origin.y - rect.size.height, rect.size.width, rect.size.height);
             glEnable(GL_SCISSOR_TEST);
             glClearColor(1, 1, 1, 1);
             glClear(GL_COLOR_BUFFER_BIT);
             }*/
            
            //NSLog(@"self.view.bounds.size.height %i displayHeight %i", (int)self.view.bounds.size.height, displayHeight);
            NSMutableArray* cp = nil;
            @synchronized([CodenameOne_GLViewController instance]) {
                cp = [currentTarget copy];
                [currentTarget removeAllObjects];
            }
            GLErrorLog;
            for(ExecutableOp* ex in cp) {
                [ex executeWithClipping];
                //[ex executeWithLog];
                GLErrorLog;
            }
            //NSLog(@"Total memory is: %i", [ExecutableOp get_free_memory]);
#ifndef CN1_USE_ARC
            [cp release];
#endif
        	_glTranslatef(0, displayHeight, 0);
            GLErrorLog;
            _glScalef(1, -1, 1);
            GLErrorLog;
            
            [DrawGradientTextureCache flushDeleted];
            [DrawStringTextureCache flushDeleted];
            if(firstTime) {
#ifndef NEW_CODENAME_ONE_VM
                GC_enable();
#endif
                firstTime = NO;
            }
        }
        
        // update the position of the edit component during drag events, we have to do this here
        // since the animation might run for a while
        if(vkbAlwaysOpen && editingComponent != nil && !editingComponent.hidden) {
#ifndef NEW_CODENAME_ONE_VM
            com_codename1_impl_ios_IOSImplementation* impl = (com_codename1_impl_ios_IOSImplementation*)com_codename1_impl_ios_IOSImplementation_GET_instance();
            com_codename1_ui_Component* comp = (com_codename1_ui_Component*)impl->fields.com_codename1_impl_ios_IOSImplementation.currentEditing_;
#else
            struct obj__com_codename1_impl_ios_IOSImplementation* impl = (struct obj__com_codename1_impl_ios_IOSImplementation*)get_static_com_codename1_impl_ios_IOSImplementation_instance(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
            JAVA_OBJECT comp = impl->com_codename1_impl_ios_IOSImplementation_currentEditing;
#endif
            if(comp != NULL) {
#ifndef NEW_CODENAME_ONE_VM
                float newEditCompoentX = (com_codename1_ui_Component_getAbsoluteX__(comp) + editComponentPadLeft) / scaleValue;
                float newEditCompoentY = (com_codename1_ui_Component_getAbsoluteY__(comp) + editComponentPadTop) / scaleValue;
#else
                float newEditCompoentX = (com_codename1_ui_Component_getAbsoluteX___R_int(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_OBJECT)comp) + editComponentPadLeft) / scaleValue;
                float newEditCompoentY = (com_codename1_ui_Component_getAbsoluteY___R_int(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_OBJECT)comp) + editComponentPadTop) / scaleValue;
#endif
                if(newEditCompoentX != editCompoentX || newEditCompoentY != editCompoentY) {
                    for (UIWindow *window in [[UIApplication sharedApplication] windows])
                    {
                        NSString* windowDescription = [[window class] description];
                        if([windowDescription isEqualToString:@"UITextEffectsWindow"]) {
                            for(UIView *v in window.subviews) {
                                if([[[v class] description] isEqualToString:@"UIAutocorrectInlinePrompt"]) {
                                    v.frame = CGRectMake(newEditCompoentX,
                                                         newEditCompoentY,
                                                         v.frame.size.width, v.frame.size.height);
                                    
                                    float vkbPos = displayHeight / scaleValue - keyboardHeight;
                                    if(newEditCompoentY  > vkbPos) {
                                        v.hidden = YES;
                                    } else {
                                        v.hidden = NO;
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    editCompoentX = newEditCompoentX;
                    editCompoentY = newEditCompoentY;
                    editingComponent.frame = CGRectMake(editCompoentX, editCompoentY, editCompoentW, editCompoentH);
                }
            }
        }
    }
    GLErrorLog;
    
    [(EAGLView *)self.view presentFramebuffer];
    GLErrorLog;
}


-(void)searchHierarchy:(UIView*)view {
    if(view.subviews != nil) {
        for(UIView *v in view.subviews) {
            NSLog(@"Found entry: %@", [[v class] description]);
            [self searchHierarchy:v];
        }
    }
}

- (BOOL)compileShader:(GLuint *)shader type:(GLenum)type file:(NSString *)file
{
    GLint status;
    const GLchar *source;
    
    source = (GLchar *)[[NSString stringWithContentsOfFile:file encoding:NSUTF8StringEncoding error:nil] UTF8String];
    if (!source)
    {
        NSLog(@"Failed to load vertex shader");
        return FALSE;
    }
    
    *shader = glCreateShader(type);
    glShaderSource(*shader, 1, &source, NULL);
    glCompileShader(*shader);
    
#if defined(DEBUG)
    GLint logLength;
    glGetShaderiv(*shader, GL_INFO_LOG_LENGTH, &logLength);
    if (logLength > 0)
    {
        GLchar *log = (GLchar *)malloc(logLength);
        glGetShaderInfoLog(*shader, logLength, &logLength, log);
        NSLog(@"Shader compile log:\n%s", log);
        free(log);
    }
#endif
    
    glGetShaderiv(*shader, GL_COMPILE_STATUS, &status);
    if (status == 0)
    {
        glDeleteShader(*shader);
        return FALSE;
    }
    
    return TRUE;
}

- (BOOL)linkProgram:(GLuint)prog
{
    GLint status;
    
    glLinkProgram(prog);
    
#if defined(DEBUG)
    GLint logLength;
    glGetProgramiv(prog, GL_INFO_LOG_LENGTH, &logLength);
    if (logLength > 0)
    {
        GLchar *log = (GLchar *)malloc(logLength);
        glGetProgramInfoLog(prog, logLength, &logLength, log);
        NSLog(@"Program link log:\n%s", log);
        free(log);
    }
#endif
    
    glGetProgramiv(prog, GL_LINK_STATUS, &status);
    if (status == 0)
        return FALSE;
    
    return TRUE;
}

- (BOOL)validateProgram:(GLuint)prog
{
    GLint logLength, status;
    
    glValidateProgram(prog);
    glGetProgramiv(prog, GL_INFO_LOG_LENGTH, &logLength);
    if (logLength > 0)
    {
        GLchar *log = (GLchar *)malloc(logLength);
        glGetProgramInfoLog(prog, logLength, &logLength, log);
        NSLog(@"Program validate log:\n%s", log);
        free(log);
    }
    
    glGetProgramiv(prog, GL_VALIDATE_STATUS, &status);
    if (status == 0)
        return FALSE;
    
    return TRUE;
}

- (BOOL)loadShaders
{
    GLuint vertShader, fragShader;
    NSString *vertShaderPathname, *fragShaderPathname;
    
    // Create shader program.
    program = glCreateProgram();
    
    // Create and compile vertex shader.
    vertShaderPathname = [[NSBundle mainBundle] pathForResource:@"Shader" ofType:@"vsh"];
    if (![self compileShader:&vertShader type:GL_VERTEX_SHADER file:vertShaderPathname])
    {
        NSLog(@"Failed to compile vertex shader");
        return FALSE;
    }
    
    // Create and compile fragment shader.
    fragShaderPathname = [[NSBundle mainBundle] pathForResource:@"Shader" ofType:@"fsh"];
    if (![self compileShader:&fragShader type:GL_FRAGMENT_SHADER file:fragShaderPathname])
    {
        NSLog(@"Failed to compile fragment shader");
        return FALSE;
    }
    
    // Attach vertex shader to program.
    glAttachShader(program, vertShader);
    
    // Attach fragment shader to program.
    glAttachShader(program, fragShader);
    
    // Bind attribute locations.
    // This needs to be done prior to linking.
    glBindAttribLocation(program, ATTRIB_VERTEX, "position");
    glBindAttribLocation(program, ATTRIB_COLOR, "color");
    
    // Link program.
    if (![self linkProgram:program])
    {
        NSLog(@"Failed to link program: %d", program);
        
        if (vertShader)
        {
            glDeleteShader(vertShader);
            vertShader = 0;
        }
        if (fragShader)
        {
            glDeleteShader(fragShader);
            fragShader = 0;
        }
        if (program)
        {
            glDeleteProgram(program);
            program = 0;
        }
        
        return FALSE;
    }
    
    // Get uniform locations.
    uniforms[UNIFORM_TRANSLATE] = glGetUniformLocation(program, "translate");
    
    // Release vertex and fragment shaders.
    if (vertShader)
        glDeleteShader(vertShader);
    if (fragShader)
        glDeleteShader(fragShader);
    
    return TRUE;
}


-(BOOL)isPaintFinished {
    return painted;
}

-(void)flushBuffer:(UIImage *)buff x:(int)x y:(int)y width:(int)width height:(int)height {
    /*if(editingComponent != nil) {
     return;
     }*/
    //currentBackBuffer = buff;
    if(lockDrawing) {
        return;
    }
    CGRect rect = CGRectMake(x, y, width, height);
    painted = NO;
	//[self performSelectorOnMainThread:@selector(setNeedsDisplay) withObject:0 waitUntilDone:NO];
    dispatch_sync(dispatch_get_main_queue(), ^{
        @synchronized([CodenameOne_GLViewController instance]) {
            if([currentTarget count] > 0) {
                [currentTarget addObjectsFromArray:upcomingTarget];
                [upcomingTarget removeAllObjects];
            } else {
                NSMutableArray* tmp = currentTarget;
                currentTarget = upcomingTarget;
                upcomingTarget = tmp;
            }
            //[layerDelegate updateArray:currentTarget];
        }
        //[self setNeedsDisplayInRect:rect];
        [self drawFrame:rect];
    });
    /*int timeout = 5;
     while (!painted && timeout > 0) {
     sleep(5);
     timeout--;
     }*/
}

-(void)drawString:(int)color alpha:(int)alpha font:(UIFont*)font str:(NSString*)str x:(int)x y:(int)y {
	POOL_BEGIN();
    UIColor* col = UIColorFromRGB(color,alpha);
    [col set];
	[str drawAtPoint:CGPointMake(x, y) withFont:font];
    //NSLog(@"Drawing the string %@ at %i, %i", str, x, y);
	POOL_END();
}


+ (void)initialize
{
    static BOOL initialized = NO;
    if(!initialized)
    {
        initialized = YES;
        //sharedSingleton = [[CodenameOne_GLViewController alloc] init];
        //[sharedSingleton initVars];
    }
}

-(void)initVars {
    currentTarget = [[NSMutableArray alloc] init];
    upcomingTarget = [[NSMutableArray alloc] init];
}

+(CodenameOne_GLViewController*)instance {
	return sharedSingleton;
}

+(void)upcoming:(ExecutableOp*)op {
    [sharedSingleton upcomingAdd:op];
}

-(void)upcomingAdd:(ExecutableOp*)op {
    @synchronized([CodenameOne_GLViewController instance]) {
        [upcomingTarget addObject:op];
    }
}

-(void)upcomingAddClip:(ExecutableOp*)op {
    @synchronized([CodenameOne_GLViewController instance]) {
        int count = upcomingTarget.count;
        if(count > 0 && [[upcomingTarget lastObject] isKindOfClass:[ClipRect class]]) {
            [upcomingTarget replaceObjectAtIndex:count-1 withObject:op];
        } else {
            [upcomingTarget addObject:op];
        }
    }
}

static BOOL skipNextTouch = NO;

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
	POOL_BEGIN();
    if(touchesArray == nil) {
        touchesArray = [[NSMutableArray alloc] init];
    }
    UITouch* touch = [touches anyObject];
    NSArray *ts = [touches allObjects];
    [touchesArray addObjectsFromArray:ts];
    int xArray[[touches count]];
    int yArray[[touches count]];
    CGPoint point = [touch locationInView:self.view];
    if([touches count] > 1) {
        for(int iter = 0 ; iter < [ts count] ; iter++) {
            UITouch* currentTouch = [ts objectAtIndex:iter];
            CGPoint currentPoint = [currentTouch locationInView:self.view];
            xArray[iter] = (int)currentPoint.x * scaleValue;
            yArray[iter] = (int)currentPoint.y * scaleValue - keyboardSlideOffset;
        }
    } else {
        xArray[0] = (int)point.x * scaleValue;
        yArray[0] = (int)point.y * scaleValue;
    }
    pointerPressedC(xArray, yArray, [touches count]);
    POOL_END();
}

-(void)foldKeyboard:(CGPoint) point {
    POOL_BEGIN();
    if(editingComponent != nil) {
        if(!(editCompoentX <= point.x && editCompoentY <= point.y && editCompoentW + editCompoentX >= point.x &&
             editCompoentY + editCompoentH >= point.y)) {
            if([editingComponent isKindOfClass:[UITextView class]]) {
                UITextView* v = (UITextView*)editingComponent;
                stringEdit(YES, -1, v.text);
            } else {
                UITextField* v = (UITextField*)editingComponent;
                stringEdit(YES, -1, v.text);
            }
            [editingComponent resignFirstResponder];
            [editingComponent removeFromSuperview];
#ifndef CN1_USE_ARC
            [editingComponent release];
#endif
            editingComponent = nil;
            displayWidth = (int)self.view.bounds.size.width * scaleValue;
            displayHeight = (int)self.view.bounds.size.height * scaleValue;
            //screenSizeChanged(displayWidth, displayHeight);
            repaintUI();
            
            //skipNextTouch = YES;
            //return;
        }
    }
    POOL_END();
}

-(void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event {
    if(skipNextTouch) {
        skipNextTouch = NO;
        return;
    }
	POOL_BEGIN();
    NSArray *ts = [touches allObjects];
    [touchesArray removeObjectsInArray:ts];
    UITouch* touch = [touches anyObject];
    int xArray[[touches count]];
    int yArray[[touches count]];
    CGPoint point = [touch locationInView:self.view];
    if([touches count] > 1) {
        for(int iter = 0 ; iter < [ts count] ; iter++) {
            UITouch* currentTouch = [ts objectAtIndex:iter];
            CGPoint currentPoint = [currentTouch locationInView:self.view];
            xArray[iter] = (int)currentPoint.x * scaleValue;
            yArray[iter] = (int)currentPoint.y * scaleValue;
        }
    } else {
        xArray[0] = (int)point.x * scaleValue;
        yArray[0] = (int)point.y * scaleValue;
    }
    if(!vkbAlwaysOpen) {
        [self foldKeyboard:point];
    }
    pointerReleasedC(xArray, yArray, [touches count]);
    POOL_END();
}

-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event {
    if(skipNextTouch) {
        skipNextTouch = NO;
        return;
    }
	POOL_BEGIN();
    NSArray *ts = [touches allObjects];
    [touchesArray removeObjectsInArray:ts];
    if([touchesArray count] > 0) {
        POOL_END();
        return;
    }
    UITouch* touch = [touches anyObject];
    int xArray[[touches count]];
    int yArray[[touches count]];
    CGPoint point = [touch locationInView:self.view];
    //NSLog(@"Released %i fingers", [touches count]);
    if([touches count] > 1) {
        for(int iter = 0 ; iter < [ts count] ; iter++) {
            UITouch* currentTouch = [ts objectAtIndex:iter];
            CGPoint currentPoint = [currentTouch locationInView:self.view];
            xArray[iter] = (int)currentPoint.x * scaleValue;
            yArray[iter] = (int)currentPoint.y * scaleValue;
        }
    } else {
        xArray[0] = (int)point.x * scaleValue;
        yArray[0] = (int)point.y * scaleValue;
    }
    if(!vkbAlwaysOpen) {
        [self foldKeyboard:point];
    }
    pointerReleasedC(xArray, yArray, [touches count]);
    POOL_END();
}

-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event {
    if(skipNextTouch || (editingComponent != nil && !vkbAlwaysOpen)) {
        return;
    }
	POOL_BEGIN();
    UITouch* touch = [touches anyObject];
    int xArray[[touchesArray count]];
    int yArray[[touchesArray count]];
    CGPoint point = [touch locationInView:self.view];
    if([touchesArray count] > 1) {
        for(int iter = 0 ; iter < [touchesArray count] ; iter++) {
            UITouch* currentTouch = [touchesArray objectAtIndex:iter];
            CGPoint currentPoint = [currentTouch locationInView:self.view];
            xArray[iter] = (int)currentPoint.x * scaleValue;
            yArray[iter] = (int)currentPoint.y * scaleValue;
            //NSLog(@"Dragging x: %i y: %i id: %i", xArray[iter], yArray[iter], currentTouch);
        }
        pointerDraggedC(xArray, yArray, [touchesArray count]);
    } else {
        xArray[0] = (int)point.x * scaleValue;
        yArray[0] = (int)point.y * scaleValue;
        pointerDraggedC(xArray, yArray, [touches count]);
    }
    POOL_END();
}

- (void) locationManager:(CLLocationManager *)manager
     didUpdateToLocation:(CLLocation *)newLocation
            fromLocation:(CLLocation *)oldLocation{
    com_codename1_impl_ios_IOSImplementation_locationUpdate__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}


extern UIPopoverController* popoverController;
extern int popoverSupported();

-(void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    //[self dismissModalViewControllerAnimated:YES];
    com_codename1_impl_ios_IOSImplementation_capturePictureResult___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG nil);
    [picker dismissModalViewControllerAnimated:YES];
}

//#define LOW_MEM_CAMERA
- (void)imagePickerController:(UIImagePickerController*)picker didFinishPickingMediaWithInfo:(NSDictionary*)info {
	POOL_BEGIN();
	NSString* mediaType = [info objectForKey:UIImagePickerControllerMediaType];
	if ([mediaType isEqualToString:@"public.image"]) {
		// get the image
		UIImage* originalImage = [info objectForKey:UIImagePickerControllerOriginalImage];
#ifndef CN1_USE_ARC
        [originalImage retain];
#endif
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            POOL_BEGIN();
            UIImage* image = originalImage;
            BOOL releaseImage = YES;
#ifndef LOW_MEM_CAMERA
            if (image.imageOrientation != UIImageOrientationUp) {
                UIGraphicsBeginImageContextWithOptions(image.size, NO, image.scale);
                [image drawInRect:(CGRect){0, 0, image.size}];
                releaseImage = NO;
#ifndef CN1_USE_ARC
                [originalImage release];
#endif
                image = UIGraphicsGetImageFromCurrentImageContext();
                UIGraphicsEndImageContext();
            }
#endif
            
            NSData* data = UIImageJPEGRepresentation(image, 90 / 100.0f);
            
            NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString *documentsDirectory = [paths objectAtIndex:0];
            NSString *path = [documentsDirectory stringByAppendingPathComponent:@"temp_image.jpg"];
            [data writeToFile:path atomically:YES];
            if(releaseImage) {
#ifndef CN1_USE_ARC
                [originalImage release];
#endif
            }
            com_codename1_impl_ios_IOSImplementation_capturePictureResult___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG path));
            POOL_END();
        });
        
	} else {
        // was movie type
        NSString *moviePath = [[info objectForKey: UIImagePickerControllerMediaURL] absoluteString];
        com_codename1_impl_ios_IOSImplementation_captureMovieResult___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG moviePath));
    }
	
	if(popoverSupported() && popoverController != nil) {
		[popoverController dismissPopoverAnimated:YES];
        popoverController.delegate = nil;
        popoverController = nil;
	} else {
#ifdef LOW_MEM_CAMERA
		[picker dismissModalViewControllerAnimated:NO];
#else
		[picker dismissModalViewControllerAnimated:YES];
#endif
	}
    
	//picker.delegate = nil;
    //picker = nil;
    POOL_END();
}

-(void) mailComposeController:(MFMailComposeViewController*)controller didFinishWithResult:(MFMailComposeResult)result error:(NSError*)error {
	[self dismissModalViewControllerAnimated:YES];
}

-(void) messageComposeViewController:(MFMessageComposeViewController*)controller didFinishWithResult:(MessageComposeResult)result {
	[self dismissModalViewControllerAnimated:YES];
}

extern JAVA_OBJECT productsArrayPending;

- (void)productsRequest:(SKProductsRequest *)request didReceiveResponse:(SKProductsResponse *)response {
	POOL_BEGIN();
    if(productsArrayPending != nil) {
#ifndef NEW_CODENAME_ONE_VM
        org_xmlvm_runtime_XMLVMArray* pArray = productsArrayPending;
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)pArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
        JAVA_ARRAY pArray = (JAVA_ARRAY)productsArrayPending;
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)pArray->data;
#endif
        NSArray* arr = response.products;
        int count = arr.count;
        for(int iter = 0 ; iter < count ; iter++) {
            SKProduct* prod = [arr objectAtIndex:iter];
            NSString* sku = [prod productIdentifier];
            com_codename1_payment_Product_setSku___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG data[iter], fromNSString(CN1_THREAD_GET_STATE_PASS_ARG sku));
            
            NSNumberFormatter *numberFormatter = [[NSNumberFormatter alloc] init];
            [numberFormatter setFormatterBehavior:NSNumberFormatterBehavior10_4];
            [numberFormatter setNumberStyle:NSNumberFormatterCurrencyStyle];
            [numberFormatter setLocale:prod.priceLocale];
            NSString *formattedString = [numberFormatter stringFromNumber:prod.price];
            com_codename1_payment_Product_setLocalizedPrice___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG data[iter], fromNSString(CN1_THREAD_GET_STATE_PASS_ARG formattedString));
            
            
            com_codename1_payment_Product_setDescription___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG data[iter], fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [prod localizedDescription]));
            com_codename1_payment_Product_setDisplayName___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG data[iter], fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [prod localizedTitle]));
        }
    }
    productsArrayPending = nil;
    POOL_END();
}

extern SKPayment *paymentInstance;
- (void)paymentQueue:(SKPaymentQueue *)queue updatedTransactions:(NSArray *)transactions
{
    for (SKPaymentTransaction *transaction in transactions)
    {
        if(paymentInstance == nil) {
            [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
            continue;
        }
        switch (transaction.transactionState)
        {
            case SKPaymentTransactionStatePurchased:
                
                [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
                com_codename1_impl_ios_IOSImplementation_itemPurchased___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG transaction.payment.productIdentifier));
                continue;
            case SKPaymentTransactionStateFailed:
                if (transaction.error.code != SKErrorPaymentCancelled) {
                    NSLog(@"Transaction error %@", transaction.error);
                    com_codename1_impl_ios_IOSImplementation_itemPurchaseError___java_lang_String_java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG transaction.payment.productIdentifier), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG transaction.error.localizedDescription));
                }
                [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
                continue;
                
            case SKPaymentTransactionStateRestored:
                NSLog(@"Transaction restored SKPaymentTransactionStateRestored");
                
                [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
                com_codename1_impl_ios_IOSImplementation_itemRestored___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG transaction.payment.productIdentifier));
                continue;
            case SKPaymentTransactionStatePurchasing:
                NSLog(@"SKPaymentTransactionStatePurchasing");
                continue;
            default:
                NSLog(@"Transaction error %i", transaction.transactionState);
                [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
                continue;
        }
    }
}

-(void)paymentQueueRestoreCompletedTransactionsFinished:(SKPaymentQueue *)queue
{
    NSLog(@"Restore transactions finished");
    com_codename1_impl_ios_IOSImplementation_restoreRequestComplete__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}
-(void)paymentQueue:(SKPaymentQueue *)queue restoreCompletedTransactionsFailedWithError:(NSError *)error
{
    NSLog(@"Restore error");
    com_codename1_impl_ios_IOSImplementation_restoreRequestError___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG error.localizedDescription));
}

- (void)audioRecorderDidFinishRecording:(AVAudioRecorder *)recorder successfully:(BOOL)flag {
    NSLog(@"audioRecorderDidFinishRecording: %i", (int)flag);
}

- (void)audioRecorderEncodeErrorDidOccur:(AVAudioRecorder *)recorder error:(NSError *)error {
    if(error != nil) {
        NSLog(@"audioRecorderEncodeErrorDidOccur: %@", [error localizedDescription]);
    } else {
        NSLog(@"audioRecorderEncodeErrorDidOccur with null argument");
    }
}

#ifdef INCLUDE_ZOOZ
-(void)paymentSuccessWithResponse: (ZooZPaymentResponse *)response{
    NSString* tid = response.transactionID;
    JAVA_FLOAT amount = response.paidAmount;
    com_codename1_impl_ios_ZoozPurchase_paymentSuccessWithResponse___java_lang_String_float(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG tid), amount);
    dispatch_async(dispatch_get_main_queue(), ^{
        repaintUI();
    });
}

// Zooz callback methods
-(void)paymentSuccessDialogClosed{
    dispatch_async(dispatch_get_main_queue(), ^{
        repaintUI();
    });
}
-(void)paymentCanceled{
    com_codename1_impl_ios_ZoozPurchase_paymentCanceledOrFailed___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG JAVA_NULL);
    //User decided to close the dialog and not to pay
    dispatch_async(dispatch_get_main_queue(), ^{
        repaintUI();
    });
}
-(void)openPaymentRequestFailed: (ZooZPaymentRequest *)request withErrorCode:
(int)errorCode andErrorMessage: (NSString *)errorMessage{
    com_codename1_impl_ios_ZoozPurchase_paymentCanceledOrFailed___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG errorMessage));
    //Some error occurred with opening the request to ZooZ servers, usually a network issue or wrong credentials issue
    dispatch_async(dispatch_get_main_queue(), ^{
        repaintUI();
    });
}
#endif

- (void) popoverControllerDidDismissPopover:(UIPopoverController *) popoverController {
    if(datepickerPopover) {
        if(currentDatePickerDate != nil) {
#ifndef CN1_USE_ARC
            [currentDatePickerDate release];
#endif
            currentDatePickerDate = nil;
        }
        com_codename1_impl_ios_IOSImplementation_datePickerResult___long(CN1_THREAD_GET_STATE_PASS_ARG -1);
        datepickerPopover = NO;
    }
}

- (void)datePickerChangeDate:(UIDatePicker *)sender {
    if(currentDatePickerDate != nil) {
#ifndef CN1_USE_ARC
        [currentDatePickerDate release];
#endif
    }
    currentDatePickerDate = sender.date;
#ifndef CN1_USE_ARC
    [currentDatePickerDate retain];
#endif
}

extern int stringPickerSelection;
#ifndef NEW_CODENAME_ONE_VM
extern org_xmlvm_runtime_XMLVMArray* pickerStringArray;
#else
extern JAVA_OBJECT pickerStringArray;
#endif
extern JAVA_LONG defaultDatePickerDate;
- (void)actionSheet:(UIActionSheet *)actionSheet didDismissWithButtonIndex:(NSInteger)buttonIndex {
    if(currentDatePickerDate == nil) {
        if(pickerStringArray == nil) {
            com_codename1_impl_ios_IOSImplementation_datePickerResult___long(CN1_THREAD_GET_STATE_PASS_ARG -1);
        } else {
            com_codename1_impl_ios_IOSImplementation_datePickerResult___long(CN1_THREAD_GET_STATE_PASS_ARG stringPickerSelection);
#ifndef NEW_CODENAME_ONE_VM
            pickerStringArray = nil;
#else
            pickerStringArray = JAVA_NULL;
#endif
        }
    } else {
        com_codename1_impl_ios_IOSImplementation_datePickerResult___long(CN1_THREAD_GET_STATE_PASS_ARG [currentDatePickerDate timeIntervalSince1970] * 1000);
#ifndef CN1_USE_ARC
        [currentDatePickerDate release];
#endif
        currentDatePickerDate = nil;
    }
}

- (void)datePickerDismissActionSheet:(id)sender {
    UISegmentedControl* s = sender;
    UIActionSheet* sheet = (UIActionSheet*)[s superview];
    [sheet dismissWithClickedButtonIndex:0 animated:YES];
    if(currentDatePickerDate == nil) {
        if(pickerStringArray == nil) {
            com_codename1_impl_ios_IOSImplementation_datePickerResult___long(CN1_THREAD_GET_STATE_PASS_ARG -1);
        } else {
            com_codename1_impl_ios_IOSImplementation_datePickerResult___long(CN1_THREAD_GET_STATE_PASS_ARG stringPickerSelection);
#ifndef NEW_CODENAME_ONE_VM
            pickerStringArray = nil;
#else
            pickerStringArray = JAVA_NULL;
#endif
        }
    } else {
        com_codename1_impl_ios_IOSImplementation_datePickerResult___long(CN1_THREAD_GET_STATE_PASS_ARG [currentDatePickerDate timeIntervalSince1970] * 1000);
        currentDatePickerDate = nil;
    }
}

UIPopoverController* popoverControllerInstance;
- (void)pickerComponentDismiss {
    if(popoverControllerInstance != nil) {
        [popoverControllerInstance dismissPopoverAnimated:YES];
        popoverControllerInstance = nil;
        if(currentDatePickerDate == nil) {
            if(pickerStringArray == nil) {
                if(defaultDatePickerDate != 0) {
                    com_codename1_impl_ios_IOSImplementation_datePickerResult___long(CN1_THREAD_GET_STATE_PASS_ARG defaultDatePickerDate);
                    defaultDatePickerDate = 0;
                    currentDatePickerDate = nil;
                } else {
                    com_codename1_impl_ios_IOSImplementation_datePickerResult___long(CN1_THREAD_GET_STATE_PASS_ARG -1);
                }
            } else {
                com_codename1_impl_ios_IOSImplementation_datePickerResult___long(CN1_THREAD_GET_STATE_PASS_ARG stringPickerSelection);
                defaultDatePickerDate = nil;
#ifndef NEW_CODENAME_ONE_VM
                pickerStringArray = nil;
#else
                pickerStringArray = JAVA_NULL;
#endif
            }
        } else {
            com_codename1_impl_ios_IOSImplementation_datePickerResult___long(CN1_THREAD_GET_STATE_PASS_ARG [currentDatePickerDate timeIntervalSince1970] * 1000);
            defaultDatePickerDate = nil;
            currentDatePickerDate = nil;
        }
    }
}

// Doesn't work for some reason
//-(void)actionSheetCancel:(UIActionSheet *)actionSheet {
//}

- (void)pickerView:(UIPickerView *)pickerView didSelectRow: (NSInteger)row inComponent:(NSInteger)component {
    stringPickerSelection = row;
}

// tell the picker how many rows are available for a given component
- (NSInteger)pickerView:(UIPickerView *)pickerView numberOfRowsInComponent:(NSInteger)component {
#ifndef NEW_CODENAME_ONE_VM
    JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)pickerStringArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    return pickerStringArray->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
    JAVA_ARRAY arr = (JAVA_ARRAY)pickerStringArray;
    return arr->length;
#endif
}

// tell the picker how many components it will have
- (NSInteger)numberOfComponentsInPickerView:(UIPickerView *)pickerView {
    return 1;
}

// tell the picker the title for a given component
- (NSString *)pickerView:(UIPickerView *)pickerView titleForRow:(NSInteger)row forComponent:(NSInteger)component {
#ifndef NEW_CODENAME_ONE_VM
    JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)pickerStringArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
    return toNSString(data[row]);
#else
    JAVA_ARRAY arr = (JAVA_ARRAY)pickerStringArray;
    JAVA_ARRAY_OBJECT* o = (JAVA_ARRAY_OBJECT*)arr->data;
    return toNSString(CN1_THREAD_GET_STATE_PASS_ARG o[row]);
#endif
}

// tell the picker the width of each row for a given component
- (CGFloat)pickerView:(UIPickerView *)pickerView widthForComponent:(NSInteger)component {
    int sectionWidth = 300;
    return sectionWidth;
}
@end
