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
#import <UIKit/UIKit.h>

#import <OpenGLES/EAGL.h>

#import <OpenGLES/ES1/gl.h>
#import <OpenGLES/ES1/glext.h>
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES2/glext.h>
#import "ExecutableOp.h"
#import "GLUIImage.h"
#import <MessageUI/MFMailComposeViewController.h>
#import <MessageUI/MFMessageComposeViewController.h>
#import <CoreLocation/CoreLocation.h>
#import "StoreKit/StoreKit.h"
#import <AudioToolbox/AudioServices.h>
#import <AVFoundation/AVFoundation.h>
#define INCLUDE_ZOOZ
#ifdef INCLUDE_ZOOZ
#import "ZooZ.h"
#endif

@interface CodenameOne_GLViewController : UIViewController<UIImagePickerControllerDelegate, MFMailComposeViewControllerDelegate, SKProductsRequestDelegate, SKPaymentTransactionObserver, MFMessageComposeViewControllerDelegate, CLLocationManagerDelegate, AVAudioRecorderDelegate 
#ifdef INCLUDE_ZOOZ
        ,ZooZPaymentCallbackDelegate
#endif
> {
@private
    EAGLContext *context;
    GLuint program;
    
    BOOL animating;
    NSInteger animationFrameInterval;
    CADisplayLink *displayLink;

    NSMutableArray* currentTarget;
    NSMutableArray* upcomingTarget;
    BOOL painted;
    GLUIImage* currentMutableImage;
    BOOL drawTextureSupported;
    BOOL keyboardIsShown;
    BOOL modifiedViewHeight;
}

@property (readonly, nonatomic, getter=isAnimating) BOOL animating;
@property (nonatomic) NSInteger animationFrameInterval;
@property (readwrite, assign) GLUIImage* currentMutableImage;

-(void)startAnimation;
-(void)stopAnimation;
+(BOOL)isDrawTextureSupported;

-(void)initVars;

+(void)upcoming:(ExecutableOp*)op;
-(void)upcomingAdd:(ExecutableOp*)op;
-(BOOL)isPaintFinished;
-(void)flushBuffer:(UIImage *)buff x:(int)x y:(int)y width:(int)width height:(int)height;

-(void)drawString:(int)color alpha:(int)alpha font:(UIFont*)font text:(const char*)text  length:(int)length x:(int)x y:(int)y;
- (void)drawScreen;
- (void)drawFrame:(CGRect)rect;

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event;
-(void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event;
-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event;
-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event;

+ (void)initialize;

+(CodenameOne_GLViewController*)instance;

@end
