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
#ifdef CN1_USE_METAL
#import <QuartzCore/QuartzCore.h>

#import "METALView.h"
#import "ExecutableOp.h"
#import "CodenameOne_GLViewController.h"
#import "CN1METALTransform.h"
#include "com_codename1_impl_ios_IOSImplementation.h"
#include "xmlvm.h"
#include "com_codename1_impl_ios_TextEditUtil.h"

static BOOL firstTime=YES;
extern float scaleValue;
extern void stringEdit(int finished, int cursorPos, NSString* text);
extern UIView *editingComponent;
extern BOOL isVKBAlwaysOpen();
extern void repaintUI();

@interface METALView (PrivateMethods)
- (void)createFramebuffer;
- (void)deleteFramebuffer;
@end

@implementation METALView

@synthesize device;
@synthesize commandQueue;
@synthesize commandBuffer;
@synthesize renderPassDescriptor;
@synthesize drawable;
@synthesize peerComponentsLayer;
@synthesize currentEncoder;

- (CGSize)drawableSize {
    CAMetalLayer *layer = (CAMetalLayer *)self.layer;
    return layer.drawableSize;
}

// You must implement this method
+ (Class)layerClass
{
    return [CAMetalLayer class];
}

extern BOOL isRetina();
extern BOOL isRetinaBug();

-(BOOL)isPaintPeersBehindEnabled {
    return com_codename1_impl_ios_IOSImplementation_isPaintPeersBehindEnabled___R_boolean(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

// Adds a peer component to the view.  Peer components are added to the peerComponentsLayer subview
-(void) addPeerComponent:(UIView*) view {
    if ([self isPaintPeersBehindEnabled]) {
        if (self.peerComponentsLayer == nil) {
            UIView *newRoot = [[UIView alloc] initWithFrame:self.bounds];
            newRoot.autoresizesSubviews = YES;
            newRoot.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
            self.peerComponentsLayer = [[UIView alloc] initWithFrame:self.bounds];
            self.peerComponentsLayer.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
            self.peerComponentsLayer.opaque = TRUE;
            self.peerComponentsLayer.userInteractionEnabled = TRUE;
            CAMetalLayer *metalLayer = (CAMetalLayer *)self.layer;
            metalLayer.opaque = FALSE;
            self.opaque = FALSE;
            self.backgroundColor = [UIColor clearColor];
            UIView* parent = [self superview];
            [CodenameOne_GLViewController instance].view = newRoot;
            [self removeFromSuperview];
            [newRoot addSubview:self.peerComponentsLayer];
            [newRoot addSubview:self];
            [parent addSubview:newRoot];
            
        }
        [self.peerComponentsLayer addSubview:view];
    } else {
        [self addSubview:view];
    }
    
}

-(BOOL)pointInside:(CGPoint)point withEvent:(UIEvent *)event
{
    if ([self isPaintPeersBehindEnabled]) {
        return com_codename1_impl_ios_IOSImplementation_hitTest___int_int_R_boolean(CN1_THREAD_GET_STATE_PASS_ARG point.x * scaleValue, point.y * scaleValue);
    } else {
        return YES;
    }
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
        CAMetalLayer *metalLayer = (CAMetalLayer *)self.layer;
        self.device = MTLCreateSystemDefaultDevice();
        metalLayer.device = self.device;
        metalLayer.opaque = TRUE;
        metalLayer.pixelFormat = MTLPixelFormatBGRA8Unorm;
        metalLayer.framebufferOnly = YES;
        self.commandQueue = [self.device newCommandQueue];
        
    }
    
    return self;
}

- (void)dealloc
{
#ifndef CN1_USE_ARC
    [super dealloc];
#endif
}


- (void)deleteFramebuffer
{
    if(editingComponent != nil) {
        return;
    }
    
}


-(void)updateFrameBufferSize:(int)w h:(int)h {
    
}

-(void)createRenderPassDescriptor {
    CAMetalLayer *layer = (CAMetalLayer*)self.layer;
    self.drawable = [layer nextDrawable];
    if (self.drawable == nil) {
        NSLog(@"METALView: Failed to get drawable");
        return;
    }

    self.renderPassDescriptor = [MTLRenderPassDescriptor renderPassDescriptor];
    MTLRenderPassColorAttachmentDescriptor* colorAttachment = self.renderPassDescriptor.colorAttachments[0];
    colorAttachment.texture = self.drawable.texture;
    colorAttachment.loadAction = MTLLoadActionClear;
    colorAttachment.clearColor = MTLClearColorMake(1.0, 1.0, 1.0, 1.0);
    colorAttachment.storeAction = MTLStoreActionStore;

    // Note: Blending is configured on MTLRenderPipelineDescriptor, not here
    // Each ExecutableOp configures blending on its pipeline state
}

- (void)beginFrame
{
    [self setFramebuffer];

    // Create render command encoder for this frame
    if (self.renderPassDescriptor != nil && self.commandBuffer != nil) {
        self.currentEncoder = [self.commandBuffer renderCommandEncoderWithDescriptor:self.renderPassDescriptor];

        // Apply scissor rectangle if enabled
        if (self.scissorEnabled && self.currentEncoder != nil) {
            [self.currentEncoder setScissorRect:self.scissorRect];
        }
    }
}

- (void)setFramebuffer
{
    CAMetalLayer *layer = (CAMetalLayer*)self.layer;

    // Create command buffer for this frame
    self.commandBuffer = [self.commandQueue commandBuffer];

    // Get drawable and create render pass descriptor
    [self createRenderPassDescriptor];

    // Update framebuffer size
    framebufferWidth = (int)layer.drawableSize.width;
    framebufferHeight = (int)layer.drawableSize.height;

    // Initialize matrices for Metal rendering (only if size is valid)
    if (framebufferWidth > 0 && framebufferHeight > 0) {
        CN1_Metal_InitMatrices(framebufferWidth, framebufferHeight);
    }
}

- (BOOL)presentFramebuffer
{
    // End encoding before presenting
    if (self.currentEncoder) {
        [self.currentEncoder endEncoding];
        self.currentEncoder = nil;
    }

    if (self.commandBuffer && self.drawable) {
        [self.commandBuffer presentDrawable:self.drawable];
        [self.commandBuffer commit];

        // Clear for next frame
        self.commandBuffer = nil;
        self.renderPassDescriptor = nil;
        self.drawable = nil;

        return YES;
    }

    return NO;
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

-(id<MTLRenderCommandEncoder>)makeRenderCommandEncoder {
    // Return the current encoder created in beginFrame
    // This allows all ExecutableOps in a frame to use the same encoder
    return self.currentEncoder;
}

@end
#endif