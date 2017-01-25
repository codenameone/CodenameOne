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

#import "EAGLView.h"
#import "ExecutableOp.h"
#import "CodenameOne_GLViewController.h"
#include "com_codename1_impl_ios_IOSImplementation.h"
#include "xmlvm.h"
#include "CN1ES2compat.h"

static BOOL firstTime=YES;
extern float scaleValue;
extern void stringEdit(int finished, int cursorPos, NSString* text);
extern UIView *editingComponent;
extern BOOL isVKBAlwaysOpen();
extern void repaintUI();

// A view to store all peer components.  
@interface CN1PeerWrapper : UIView {
    
}

@end

@implementation CN1PeerWrapper

// Override pointInside so that events that occur on the front buffer are not blocked
// by the native peers that are rendered underneath it
-(BOOL)pointInside:(CGPoint)point withEvent:(UIEvent *)event
{
    //Using code from http://stackoverflow.com/questions/1042830/retrieving-a-pixel-alpha-value-for-a-uiimage
    BOOL inSubView = NO;
    for (UIView *subview in self.subviews)
    {
        inSubView = [subview pointInside:point withEvent:event];
        if (inSubView) {
            break;
        }
    }
    
    if (!inSubView) {
        return NO;
    }
    
    UIImageView* v = ((EAGLView*)[CodenameOne_GLViewController instance].view).topLayerView;
    if (v == nil || v.isHidden) {
        return inSubView;
    }
    
    
    
    unsigned char pixel[1] = {0};
    CGContextRef context = CGBitmapContextCreate(pixel,
                                                 1, 1, 8, 1, NULL,
                                                 kCGImageAlphaOnly);
    
    UIImage* image = v.image;
    if (image == nil) {
        return inSubView;
    }
    
    UIGraphicsPushContext(context);
    [image drawAtPoint:CGPointMake(-point.x * scaleValue, -point.y * scaleValue)];
    UIGraphicsPopContext();
    CGContextRelease(context);
    CGFloat alpha = pixel[0]/255.0f;
    BOOL transparent = alpha < 0.01f;
    
    return transparent && inSubView;
}
@end

@interface EAGLView (PrivateMethods)
- (void)createFramebuffer;
- (void)deleteFramebuffer;
@end

@implementation EAGLView

@synthesize context;
@synthesize topLayerView;
@synthesize peerComponentsLayer;

// You must implement this method
+ (Class)layerClass
{
    return [CAEAGLLayer class];
}

extern BOOL isRetina();
extern BOOL isRetinaBug();

// Initializes the UIImageView that is used to display the front graphics
-(void) initTopLayerView {
    self.topLayerView = [[UIImageView alloc] initWithFrame:self.bounds];
    self.topLayerView.hidden = FALSE;
    self.topLayerView.opaque = FALSE;

    //self.topLayerView.backgroundColor = [UIColor blueColor];
    self.topLayerView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    self.topLayerView.userInteractionEnabled = FALSE;
    [self addSubview:self.topLayerView];
    [self bringSubviewToFront:self.topLayerView];
    self.topLayerView.clearsContextBeforeDrawing = NO;
}

// Shows the front graphics layer.  This is only used
// when setting display property useFrontGraphics to "true"
-(void) showFrontGraphics {
   
    if (self.topLayerView == nil) {
        [self initTopLayerView];
    }
    self.topLayerView.hidden = FALSE;
}

// Hides front graphics layer.  This is only used when setting display property
// useFrontGraphics to "false"
-(void) hideFrontGraphics {
    if (self.topLayerView != nil) {
        self.topLayerView.hidden = TRUE;
    }
}

// Sets the image that should be shown in the Front graphics.
-(void) setTopLayer:(GLUIImage*)img x:(int)x y:(int)y w:(int)w h:(int)h {
    UIImage* newImage = [img getImage];
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.topLayerView == nil) {
            [self initTopLayerView];
        }
        self.topLayerView.image = newImage;
    });
    
}

// Adds a peer component to the view.  Peer components are added to the peerComponentsLayer subview
-(void) addPeerComponent:(UIView*) view {
    if (self.peerComponentsLayer == nil) {
        self.peerComponentsLayer = [[CN1PeerWrapper alloc] initWithFrame:self.bounds];
        self.peerComponentsLayer.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        self.peerComponentsLayer.opaque = FALSE;
        self.peerComponentsLayer.userInteractionEnabled = TRUE;
        //self.peerComponentsLayer.backgroundColor = [UIColor blueColor];
        [self addSubview:self.peerComponentsLayer];
    }
    [self.peerComponentsLayer addSubview:view];
     
    //[self addSubview:view];
    
}

//The EAGL view is stored in the nib file. When it's unarchived it's sent -initWithCoder:.
- (id)initWithCoder:(NSCoder*)coder
{
    self = [super initWithCoder:coder];
	if (self) {
        self.clearsContextBeforeDrawing = NO;
        if ([[UIScreen mainScreen] respondsToSelector:@selector(scale)] && isRetina()) {
            if(isRetinaBug()) {
                self.contentScaleFactor = 1.0;
            } else {
                self.contentScaleFactor = [[UIScreen mainScreen] scale];
            }
        }
        CAEAGLLayer *eaglLayer = (CAEAGLLayer *)self.layer;
        
        eaglLayer.opaque = TRUE;
        eaglLayer.drawableProperties = [NSDictionary dictionaryWithObjectsAndKeys:
                                        [NSNumber numberWithBool:YES], kEAGLDrawablePropertyRetainedBacking,
                                        kEAGLColorFormatRGBA8, kEAGLDrawablePropertyColorFormat,
                                        nil];
    }
    
    return self;
}

- (void)dealloc
{
    [self deleteFramebuffer];
#ifndef CN1_USE_ARC
    [context release];
    [super dealloc];
#endif    
}

- (void)setContext:(EAGLContext *)newContext
{
    if (context != newContext) {
        [self deleteFramebuffer];
        
#ifndef CN1_USE_ARC
        [context release];
        context = [newContext retain];
#endif
        
        [EAGLContext setCurrentContext:nil];
    }
}

- (void)createFramebuffer
{
    if (context && !defaultFramebuffer) {
        [EAGLContext setCurrentContext:context];
        
        GLErrorLog;
        // Create default framebuffer object.
        glGenFramebuffers(1, &defaultFramebuffer);
        GLErrorLog;
        glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebuffer);
        GLErrorLog;
        
        // Create color render buffer and allocate backing store.
        glGenRenderbuffers(1, &colorRenderbuffer);
        GLErrorLog;
        glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbuffer);
        GLErrorLog;

        [context renderbufferStorage:GL_RENDERBUFFER fromDrawable:(CAEAGLLayer *)self.layer];
        GLErrorLog;
        glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH, &framebufferWidth);
        GLErrorLog;
        glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT, &framebufferHeight);
        GLErrorLog;
        
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, colorRenderbuffer);
        GLErrorLog;
        
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            NSLog(@"Failed to make complete framebuffer object %x", glCheckFramebufferStatus(GL_FRAMEBUFFER));
        //NSLog(@"Created framebuffer: %i %i", (int)framebufferWidth, (int)framebufferHeight);
        glClearColor(0, 0, 0, 1.0f);
#if USE_ES2
        
        
        GLuint stencil;
        glGenRenderbuffersOES(1, &stencil);
        GLErrorLog;
        glBindRenderbuffer(GL_RENDERBUFFER_OES, stencil);
        GLErrorLog;
        glRenderbufferStorageOES(GL_RENDERBUFFER_OES, GL_STENCIL_INDEX8_OES, framebufferWidth, framebufferHeight);
        GLErrorLog;
        glFramebufferRenderbufferOES(GL_FRAMEBUFFER_OES, GL_STENCIL_ATTACHMENT_OES,
                                     GL_RENDERBUFFER_OES, stencil);
        GLErrorLog;
        /*
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8_OES, framebufferWidth, framebufferHeight);
        GLErrorLog;
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, defaultDepthBuffer);
        GLErrorLog;

        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, defaultDepthBuffer);
        */
         glClearStencil(0x0);
        GLErrorLog;
        glClear(GL_COLOR_BUFFER_BIT|GL_STENCIL_BUFFER_BIT);
        GLErrorLog;
        //glClearStencil(0x1);
        //GLErrorLog;
         
        
        
        
#else
        
        glClear(GL_COLOR_BUFFER_BIT);
#endif
        
    }
}

- (void)deleteFramebuffer
{
    if(editingComponent != nil) {
        return;
    }
    if (context) {
        [EAGLContext setCurrentContext:context];
        GLErrorLog;
        
        if (defaultFramebuffer) {
            glDeleteFramebuffers(1, &defaultFramebuffer);
            defaultFramebuffer = 0;
        }
        GLErrorLog;
        
        if (colorRenderbuffer) {
            glDeleteRenderbuffers(1, &colorRenderbuffer);
            colorRenderbuffer = 0;
        }
        GLErrorLog;
    }
}


-(void)updateFrameBufferSize:(int)w h:(int)h {
    /*[self deleteFramebuffer];
     framebufferWidth = w;
     framebufferHeight = h;
     //[self.layer setBounds:CGRectMake(0, 0, w, h)];
     //[self setBounds:CGRectMake(0, 0, w, h)];
     NSLog(@"Deleted framebuffer: %i %i", w, h);*/
}

- (void)setFramebuffer
{
    if (context) {
        GLErrorLog;
        [EAGLContext setCurrentContext:context];
        GLErrorLog;
        
        if (!defaultFramebuffer)
            [self createFramebuffer];
        
        glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebuffer);
        GLErrorLog;
        
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        GLErrorLog;
        _glDisable(GL_DEPTH_TEST);
        GLErrorLog;
        
        glViewport(0, 0, framebufferWidth, framebufferHeight);
        GLErrorLog;
        _glMatrixMode(GL_PROJECTION);
        GLErrorLog;
        _glLoadIdentity();
        GLErrorLog;
        _glOrthof(0, framebufferWidth, 0, framebufferHeight, -1, 1);
        //NSLog(@"glOrtho %i, %i  bounds.size.height: %i", framebufferWidth, framebufferHeight, (int)self.bounds.size.height);
        GLErrorLog;
        _glMatrixMode(GL_MODELVIEW);
        GLErrorLog;
        _glLoadIdentity();
        GLErrorLog;
        _glEnable(GL_BLEND);
        GLErrorLog;
        _glDisable(GL_ALPHA_TEST);
        GLErrorLog;
    }
}

/**
 * User clicked Done or Next button above the keyboard
 */
-(void) keyboardDoneClicked {
    if(editingComponent != nil) {
        if([editingComponent isKindOfClass:[UITextView class]]) {
            stringEdit(YES, -2, ((UITextView*)editingComponent).text);
        } else {
            stringEdit(YES, -2, ((UITextField*)editingComponent).text);
        }
        if(isVKBAlwaysOpen()) {
            com_codename1_impl_ios_IOSImplementation_foldKeyboard__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        } else {
            [editingComponent resignFirstResponder];
            [editingComponent removeFromSuperview];
#ifndef CN1_USE_ARC
            [editingComponent release];
#endif
            editingComponent = nil;
        }
        repaintUI();
        
    }
}

/**
 * User clicked Done or Next button above the keyboard
 */
-(void) keyboardNextClicked {
    if(editingComponent != nil) {
        if([editingComponent isKindOfClass:[UITextView class]]) {
            stringEdit(YES, -2, ((UITextView*)editingComponent).text);
        } else {
            stringEdit(YES, -2, ((UITextField*)editingComponent).text);
        }
        if(isVKBAlwaysOpen()) {
            com_codename1_impl_ios_TextEditUtil_editNextTextArea__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        } else {
            [editingComponent resignFirstResponder];
            [editingComponent removeFromSuperview];
#ifndef CN1_USE_ARC
            [editingComponent release];
#endif
            editingComponent = nil;
        }
        repaintUI();
        
    }
}

-(void)textViewDidChange:(UITextView *)textView {
    if(editingComponent.hidden) {
        editingComponent.hidden = NO;
        com_codename1_impl_ios_IOSImplementation_showTextEditorAgain__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    }
    if([editingComponent isKindOfClass:[UITextView class]]) {
        stringEdit(NO, -1, ((UITextView*)editingComponent).text);
    } else {
        stringEdit(NO, -1, ((UITextField*)editingComponent).text);
    }
    com_codename1_impl_ios_IOSImplementation_resizeNativeTextComponentCallback__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

-(void)textFieldDidChange {
    if(editingComponent.hidden) {
        editingComponent.hidden = NO;
        com_codename1_impl_ios_IOSImplementation_showTextEditorAgain__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    }
    if([editingComponent isKindOfClass:[UITextView class]]) {
        stringEdit(NO, -1, ((UITextView*)editingComponent).text);
    } else {
        stringEdit(NO, -1, ((UITextField*)editingComponent).text);
    }
    
    com_codename1_impl_ios_IOSImplementation_resizeNativeTextComponentCallback__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

extern int currentlyEditingMaxLength;
- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
    NSUInteger newLength = (textField.text.length - range.length) + string.length;
    return (newLength <= currentlyEditingMaxLength);
}

-(BOOL)textView:(UITextView *)textView shouldChangeTextInRange:(NSRange)range replacementText:(NSString *)text {
    NSUInteger newLength = (textView.text.length - range.length) + text.length;
    return (newLength <= currentlyEditingMaxLength);
}


- (BOOL)textFieldShouldReturn:(UITextField *)theTextField {
    if(editingComponent != nil) {
        if([editingComponent isKindOfClass:[UITextView class]]) {
            stringEdit(YES, -2, ((UITextView*)editingComponent).text);
        } else {
            stringEdit(YES, -2, ((UITextField*)editingComponent).text);
        }
        //if there is one then goto the edit next textarea
        //com_codename1_impl_ios_TextEditUtil_editNextTextArea__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        if(isVKBAlwaysOpen()) {
            com_codename1_impl_ios_TextEditUtil_editNextTextArea__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        } else {
            [editingComponent resignFirstResponder];
            [editingComponent removeFromSuperview];
#ifndef CN1_USE_ARC
            [editingComponent release];
#endif
            editingComponent = nil;
        }
        repaintUI();
    }
    return YES;
}

- (BOOL)presentFramebuffer
{
    BOOL success = FALSE;
    
    if (context) {
        [EAGLContext setCurrentContext:context];
        
        glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbuffer);
        GLErrorLog;
        
        success = [context presentRenderbuffer:GL_RENDERBUFFER];
        GLErrorLog;
        
        if(!success) {
            NSLog(@"Present render buffer unsuccessful!");
        }
    }
    
    return success;
}

-(void)layoutSubviews
{
    if (firstTime){
        [self deleteFramebuffer];
        firstTime=NO;
    }
    [super layoutSubviews];
}

/*-(void)drawRect:(CGRect)rect {
 [[CodenameOne_GLViewController instance] drawFrame:rect];
 }*/


@end
