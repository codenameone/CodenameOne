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
#ifdef CN1_USE_METAL
#import <QuartzCore/QuartzCore.h>
@import Metal;
@import simd;

#import "METALView.h"
#import "CN1Metalcompat.h"
#import "ExecutableOp.h"
#import "CodenameOne_GLViewController.h"
#include "com_codename1_impl_ios_IOSImplementation.h"
#include "xmlvm.h"
#include "com_codename1_impl_ios_TextEditUtil.h"

static BOOL firstTime=YES;
extern float scaleValue;
extern void stringEdit(int finished, int cursorPos, NSString* text);
extern UIView *editingComponent;
extern BOOL isVKBAlwaysOpen();
extern void repaintUI();

@implementation METALView

@synthesize commandQueue;
@synthesize commandBuffer;
@synthesize renderPassDescriptor;
@synthesize renderCommandEncoder;
@synthesize drawable;
@synthesize screenTexture;
@synthesize peerComponentsLayer;
@synthesize framebufferWidth;
@synthesize framebufferHeight;
@synthesize projectionMatrix;

static simd_float4x4 CN1MetalOrtho(float left, float right, float bottom, float top, float near, float far) {
    // Metal NDC: x,y in [-1,1], z in [0,1]. Column-major construction matching Apple's conventions.
    float rl = 1.0f / (right - left);
    float tb = 1.0f / (top - bottom);
    float fn = 1.0f / (far - near);
    simd_float4x4 m = (simd_float4x4){{
        { 2.0f * rl,                 0.0f,                    0.0f,        0.0f },
        { 0.0f,                      2.0f * tb,               0.0f,        0.0f },
        { 0.0f,                      0.0f,                    -fn,         0.0f },
        { -(right + left) * rl,      -(top + bottom) * tb,    -near * fn,  1.0f }
    }};
    return m;
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
        metalLayer.device = MTLCreateSystemDefaultDevice();
        metalLayer.opaque = TRUE;
        metalLayer.pixelFormat = MTLPixelFormatBGRA8Unorm;
        metalLayer.framebufferOnly = YES;
        // sRGB colourspace so colours match the GL path's CAEAGLLayer
        // output. Without this, CG-rasterised images and gradients
        // (DeviceRGB-tagged in their CGBitmapContext) display slightly
        // brighter on Metal because the layer treats their bytes as
        // linear-RGB instead of sRGB-encoded.
        CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
        if (cs != NULL) {
            metalLayer.colorspace = cs;
            CGColorSpaceRelease(cs);
        }
        // Cap drawable pool to 3 so the GPU has at most one render in
        // flight while CPU prepares the next two. Higher counts trade
        // smoothness for latency and memory; 3 is the iOS default for
        // most CAMetalLayer use cases. Combined with our nextDrawable
        // skip-frame fallback in presentFramebuffer this keeps the
        // pipeline non-blocking under pressure.
        metalLayer.maximumDrawableCount = 3;
        // `makeCommandQueue` is the Swift name; Objective-C uses `newCommandQueue`.
        // newCommandQueue returns +1 (NARC family); release the local after
        // the synthesized retain setter takes its own retain so we end up at
        // +1 owned by the property, not +2.
        id<MTLCommandQueue> newQueue = [metalLayer.device newCommandQueue];
        self.commandQueue = newQueue;
#ifndef CN1_USE_ARC
        [newQueue release];
#endif
        CGSize sz = self.bounds.size;
        CGFloat s = self.contentScaleFactor;
        [self updateFrameBufferSize:(int)(sz.width * s) h:(int)(sz.height * s)];

        // Drop the glyph atlas + text cache + gradient cache on memory
        // pressure. Pipeline state cache stays — those are precious to
        // rebuild and small. The screen texture also stays; updateFrame-
        // BufferSize: handles its replacement on resize.
        [[NSNotificationCenter defaultCenter]
            addObserver:self
               selector:@selector(memoryWarning)
                   name:UIApplicationDidReceiveMemoryWarningNotification
                 object:nil];
    }

    return self;
}

- (void)memoryWarning {
    extern void CN1MetalReleaseCaches(void);
    CN1MetalReleaseCaches();
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
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
    // Ignore the passed w/h -- CodenameOne_GLViewController.m calls this with
    // logical points (self.view.bounds.size), but the Metal drawable and
    // projection must be in physical pixels. The GL path tolerates the
    // logical-point argument because EAGLView.updateFrameBufferSize: is a
    // no-op (dimensions get read back from the renderbuffer after the layer
    // is bound). For Metal we always compute from our own layer bounds.
    CGSize sz = self.bounds.size;
    CGFloat s = self.contentScaleFactor;
    int pw = (int)(sz.width * s);
    int ph = (int)(sz.height * s);
    if (pw <= 0 || ph <= 0) return;
    if (pw == framebufferWidth && ph == framebufferHeight) {
        return;
    }
    // An encoder may be mid-frame (awakeFromNib fires setFramebuffer before
    // layoutSubviews, so the first encoder references the xib's placeholder
    // bounds). Tear it down cleanly so the next setFramebuffer creates a
    // fresh encoder against the new screenTexture. Otherwise draws land on
    // a texture we're about to replace, and the stale dimensions get cached
    // inside CN1Metalcompat (breaking scissor clamping etc.).
    if (self.renderCommandEncoder != nil) {
        CN1MetalEndFrame();
        [self.renderCommandEncoder endEncoding];
        self.renderCommandEncoder = nil;
    }
    if (self.commandBuffer != nil) {
        [self.commandBuffer commit];
        self.commandBuffer = nil;
        self.renderPassDescriptor = nil;
        self.drawable = nil;
    }
    framebufferWidth = pw;
    framebufferHeight = ph;
    // Match iOS UIKit's Y-down convention: origin at top-left.
    // Passing bottom=h, top=0 makes y_ndc = 1 - 2*y_input/h, so y_input=0
    // maps to NDC y=+1 (top of the drawable) and y_input=h maps to NDC y=-1
    // (bottom). That avoids the _glScalef(1,-1,1) + _glTranslatef(0,-h,0)
    // workaround the GL path does in CodenameOne_GLViewController.drawFrame.
    projectionMatrix = CN1MetalOrtho(0.0f, (float)pw, (float)ph, 0.0f, -1.0f, 1.0f);
    CAMetalLayer *layer = (CAMetalLayer*)self.layer;
    layer.drawableSize = CGSizeMake(pw, ph);

    // Rebuild the persistent screen render target at the new size. Anything
    // previously rendered into the old texture is lost; the next frame will
    // re-clear from black as CN1 repaints -- Form.paint() always issues a
    // full-screen background fill on layout changes so this is safe.
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
        width:pw height:ph mipmapped:NO];
    desc.usage = MTLTextureUsageRenderTarget | MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModePrivate;
    // newTextureWithDescriptor returns +1 (NARC family); the synthesized
    // retain setter adds another +1 for a net +2 under MRR. Release the
    // local once the property holds its own retain so we don't leak the
    // previous screenTexture every time the framebuffer is resized
    // (rotation, window resize, etc.).
    id<MTLTexture> newScreen = [layer.device newTextureWithDescriptor:desc];
    self.screenTexture = newScreen;
#ifndef CN1_USE_ARC
    [newScreen release];
#endif

    // Prime the texture to opaque black: private-storage textures come back
    // uninitialised, so the first frame (which uses MTLLoadActionLoad) would
    // sample garbage for any pixel CN1 hasn't drawn yet.
    id<MTLCommandBuffer> clearCb = [self.commandQueue commandBuffer];
    MTLRenderPassDescriptor *clearPass = [MTLRenderPassDescriptor renderPassDescriptor];
    clearPass.colorAttachments[0].texture = self.screenTexture;
    clearPass.colorAttachments[0].loadAction = MTLLoadActionClear;
    clearPass.colorAttachments[0].storeAction = MTLStoreActionStore;
    clearPass.colorAttachments[0].clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0);
    [[clearCb renderCommandEncoderWithDescriptor:clearPass] endEncoding];
    [clearCb commit];
}

-(void)createRenderPassDescriptor {
    if (self.screenTexture == nil) {
        self.renderPassDescriptor = nil;
        return;
    }
    self.renderPassDescriptor = [MTLRenderPassDescriptor renderPassDescriptor];
    MTLRenderPassColorAttachmentDescriptor* colorAttachment = self.renderPassDescriptor.colorAttachments[0];
    // Render into the persistent screen texture so incremental draws from
    // subsequent drawFrame calls accumulate on top of whatever was there
    // before. MTLLoadActionLoad preserves previous pixels (vs MTLLoadActionClear
    // which would wipe everything each frame) — CN1 only queues diff ops
    // per frame; the OpenGL path relies on its renderbuffer persisting.
    colorAttachment.texture = self.screenTexture;
    colorAttachment.loadAction = MTLLoadActionLoad;
    colorAttachment.storeAction = MTLStoreActionStore;
}

- (void)setFramebuffer
{
    // setFramebuffer may be called multiple times per frame (awakeFromNib
    // issues one unpaired call during init; drawFrame can be invoked
    // out-of-band alongside the CADisplayLink path). The GL backend tolerates
    // this because binding the same framebuffer twice is a no-op. For Metal
    // we keep the same encoder alive across those extra calls -- creating a
    // fresh encoder each time would throw away any ops queued between setup
    // and presentFramebuffer. Only presentFramebuffer ends+commits+presents.
    if (self.renderCommandEncoder != nil) {
        return;
    }
    CAMetalLayer *layer = (CAMetalLayer*)self.layer;
    self.commandBuffer = [self.commandQueue commandBuffer];
    [self createRenderPassDescriptor];
    if (self.renderPassDescriptor == nil) {
        // nextDrawable returned nil; skip this frame.
        self.renderCommandEncoder = nil;
        return;
    }
    self.renderCommandEncoder = [self.commandBuffer renderCommandEncoderWithDescriptor:self.renderPassDescriptor];
    [self.renderCommandEncoder setViewport: (MTLViewport){ 0.0, 0.0, (double)framebufferWidth, (double)framebufferHeight, 0.0, 1.0 }];
    // Publish the encoder + projection to the CN1Metalcompat layer; each
    // ExecutableOp's Metal branch pulls the encoder from there.
    CN1MetalBeginFrame(self.renderCommandEncoder, projectionMatrix, framebufferWidth, framebufferHeight);
}

- (BOOL)presentFramebuffer
{
    if (self.renderCommandEncoder == nil) {
        // Nothing was encoded (setFramebuffer was not called after the
        // previous present). Nothing to do.
        self.commandBuffer = nil;
        return NO;
    }
    CN1MetalEndFrame();
    [self.renderCommandEncoder endEncoding];
    self.renderCommandEncoder = nil;
    self.renderPassDescriptor = nil;

    // Acquire the drawable here (not in setFramebuffer) to minimise its
    // dwell time -- holding a drawable across the whole op-encoding phase
    // stalls nextDrawable for subsequent frames.
    CAMetalLayer *layer = (CAMetalLayer*)self.layer;
    id<CAMetalDrawable> dr = [layer nextDrawable];
    if (dr == nil) {
        // Memory pressure dropped the drawable. Commit render work so
        // screenTexture still updates; skip this frame's present.
        [self.commandBuffer commit];
        self.commandBuffer = nil;
        return NO;
    }
    self.drawable = dr;
    id<MTLBlitCommandEncoder> blit = [self.commandBuffer blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture
              sourceSlice:0 sourceLevel:0
             sourceOrigin:MTLOriginMake(0, 0, 0)
               sourceSize:MTLSizeMake(framebufferWidth, framebufferHeight, 1)
                toTexture:dr.texture
         destinationSlice:0 destinationLevel:0
        destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [self.commandBuffer presentDrawable:dr];
    [self.commandBuffer commit];
    self.drawable = nil;
    self.commandBuffer = nil;
    return YES;
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
    // Keep the Metal drawable + projection in sync with the actual runtime
    // view size. initWithCoder runs with the xib's default size (often the
    // legacy 320x480 placeholder), so without this the projection stays
    // scaled to that default and anything drawn outside those bounds gets
    // clipped at NDC edges -- the Form ends up only covering a portion of
    // the screen.
    CGSize sz = self.bounds.size;
    CGFloat s = self.contentScaleFactor;
    int w = (int)(sz.width * s);
    int h = (int)(sz.height * s);
    if (w > 0 && h > 0) {
        [self updateFrameBufferSize:w h:h];
    }
    [super layoutSubviews];
}

/*-(void)drawRect:(CGRect)rect {
 [[CodenameOne_GLViewController instance] drawFrame:rect];
 }*/


@end
#endif