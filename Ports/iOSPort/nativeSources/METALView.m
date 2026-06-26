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
@synthesize stencilTexture;
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

// Shared Metal device + command-queue setup invoked by both the NIB-
// instantiated path (initWithCoder:) and the programmatic-instantiation
// path (initWithFrame:). The Mac Catalyst slice goes through
// initWithFrame: because IBAgent-macOS-UIKit can't compile the iOS
// view-controller XIB and CodenameOne_GLAppDelegate falls back to
// passing nil to initWithNibName:. Without this shared setup the
// CAMetalLayer's device stays nil, CN1MetalSetDeviceAndCommandQueue
// is never published, and CN1MetalGlyphAtlas+atlasForFont: returns nil
// for every font -- which is exactly the "no atlas available" failure
// the Mac CI surfaced.
- (void)cn1SetupMetal {
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
        // framebufferOnly must be NO: presentFramebuffer blits screenTexture
        // into the drawable via copyFromTexture:toTexture:, and Metal's blit
        // validation aborts ("destinationTexture must not be a framebufferOnly
        // texture") when the destination drawable was framebufferOnly. Debug
        // builds with Metal API Validation enabled crash on the first paint;
        // release builds silently produced undefined-behaviour copies on some
        // GPUs. Trading the (small) memoryless-storage benefit for a working
        // present path.
        metalLayer.framebufferOnly = NO;
        // Colour space for the Metal layer. Default is sRGB so colours
        // match the GL path's CAEAGLLayer output: without it, CG-rasterised
        // images and gradients (DeviceRGB-tagged in their CGBitmapContext)
        // display slightly brighter on Metal because the layer treats
        // their bytes as linear-RGB instead of sRGB-encoded.
        //
        // The build hint `ios.metal.colorSpace` selects the value (see
        // IPhoneBuilder, which injects one of the CN1_METAL_COLORSPACE_*
        // defines below). Set the hint to "none" to leave the layer's
        // colorspace property untouched (system default).
#if defined(CN1_METAL_COLORSPACE_NONE)
        // Skip setting metalLayer.colorspace entirely.
#else
  #if defined(CN1_METAL_COLORSPACE_DISPLAY_P3)
        CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceDisplayP3);
  #elif defined(CN1_METAL_COLORSPACE_DEVICE_RGB)
        CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
  #elif defined(CN1_METAL_COLORSPACE_LINEAR_SRGB)
        CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceLinearSRGB);
  #elif defined(CN1_METAL_COLORSPACE_EXTENDED_SRGB)
        CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceExtendedSRGB);
  #elif defined(CN1_METAL_COLORSPACE_EXTENDED_LINEAR_SRGB)
        CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceExtendedLinearSRGB);
  #else
        CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
  #endif
        if (cs != NULL) {
            metalLayer.colorspace = cs;
            CGColorSpaceRelease(cs);
        }
#endif
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
        // Publish the device + queue to CN1Metalcompat so its global
        // accessors don't have to dereference our (UIView) layer from
        // background threads. Doing it on the main thread, exactly once,
        // means CN1MetalDevice / CN1MetalCommandQueue become cheap static
        // reads safe to invoke from the EDT and any background GCD queue.
        CN1MetalSetDeviceAndCommandQueue(metalLayer.device, self.commandQueue);
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

//The EAGL view is stored in the nib file. When it's unarchived it's sent -initWithCoder:.
- (id)initWithCoder:(NSCoder*)coder
{
    self = [super initWithCoder:coder];
    if (self) {
        [self cn1SetupMetal];
    }
    return self;
}

// Programmatic instantiation. Used on Mac Catalyst (and any future
// platform where the iOS XIB is unavailable): CodenameOne_GLView-
// Controller's loadView allocates a METALView via initWithFrame:
// instead of loading from the NIB. Without this override the
// UIView default initWithFrame: runs, which skips cn1SetupMetal and
// leaves CN1MetalDevice() returning nil for the lifetime of the
// process -- the runtime failure mode that surfaced in CI as "no
// atlas available for font" on every CN1MetalDrawString call.
- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        [self cn1SetupMetal];
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
    // Trust caller-supplied physical-pixel dimensions; fall back to bounds
    // only if the caller passes 0. Reading self.bounds alone is unsafe
    // during rotation: viewWillTransitionToSize: in CodenameOne_GLView-
    // Controller fires BEFORE UIKit updates the view's bounds, so a
    // bounds-derived size matches the cached (old) framebuffer dimensions
    // and the early-return below would leave screenTexture, the projection
    // matrix and stencil texture at the previous orientation. CAMetalLayer's
    // drawableSize is auto-resized by UIKit on rotation, so the next
    // drawFrame would blit the old-sized screenTexture into the new-sized
    // drawable -- portrait content lands in a corner of the landscape
    // drawable and the remaining pixels read back uninitialised, surfacing
    // as the smeared/pink frames reported in #4954. The callers in this
    // file (initWithCoder, layoutSubviews) and the GLViewController callers
    // all pass physical pixels.
    int pw = w;
    int ph = h;
    if (pw <= 0 || ph <= 0) {
        CGSize sz = self.bounds.size;
        CGFloat s = self.contentScaleFactor;
        pw = (int)(sz.width * s);
        ph = (int)(sz.height * s);
    }
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
    // Preserve the previously rendered frame across the resize so a rotation
    // never shows black. On the Metal backend, changing layer.drawableSize
    // (below) invalidates the CAMetalLayer's currently displayed drawable, so
    // the layer falls back to its opaque (black) background until the next
    // presentDrawable:. With the CADisplayLink disabled in this port, that
    // next present only arrives once the EDT wakes, re-lays-out and repaints --
    // a gap that is invisible while the app is actively painting but produces a
    // visible black flash during the ~0.3s rotation animation when the app has
    // gone idle (#5162). Capture the old screen texture here; after the new one
    // is built we scale-blit the last frame into it and present once so the
    // layer keeps showing the previous frame (stretched, like UIKit's own
    // rotation snapshot) until the real repaint lands.
    id<MTLTexture> oldScreen = self.screenTexture;
#ifndef CN1_USE_ARC
    // Keep it alive past the self.screenTexture reassignment below: the
    // synthesized retain setter releases the previously held value.
    [oldScreen retain];
#endif
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

    // Initialise the new texture. Private-storage textures come back
    // uninitialised, so the first frame (which uses MTLLoadActionLoad) would
    // sample garbage for any pixel CN1 hasn't drawn yet. When a previous frame
    // exists we scale-blit it in (preserving the last visible content across
    // the resize -- see the oldScreen capture above); otherwise we just clear
    // to opaque black.
    id<MTLCommandBuffer> clearCb = [self.commandQueue commandBuffer];
    MTLRenderPassDescriptor *clearPass = [MTLRenderPassDescriptor renderPassDescriptor];
    clearPass.colorAttachments[0].texture = self.screenTexture;
    clearPass.colorAttachments[0].loadAction = MTLLoadActionClear;
    clearPass.colorAttachments[0].storeAction = MTLStoreActionStore;
    clearPass.colorAttachments[0].clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0);
    // The preserve draw below binds a CN1MetalPipelineCache pipeline, and every
    // pipeline in that cache declares stencilAttachmentPixelFormat=Stencil8
    // (polygon-clip #3921). A render pass that binds such a pipeline MUST attach
    // a Stencil8 texture or Metal aborts in setRenderPipelineState: with a
    // pixel-format mismatch (#5103): "For stencil attachment, the
    // renderPipelineState pixelFormat must be MTLPixelFormatInvalid, as no
    // texture is set." Under MTL_DEBUG_LAYER=assert (the CI Metal screenshot
    // job) that abort is a SIGABRT on the first resize, which drops every
    // screenshot after it. Attach a throwaway clear-on-load stencil exactly
    // like the seed draw in CN1Metalcompat.m -- the preserve draw never engages
    // the stencil test, so its contents are irrelevant. Only needed when there
    // is a previous frame to draw (the plain black clear binds no pipeline).
    id<MTLTexture> clearStencilTex = nil;
    if (oldScreen != nil) {
        MTLTextureDescriptor *clearStencilDesc = [MTLTextureDescriptor
            texture2DDescriptorWithPixelFormat:MTLPixelFormatStencil8
            width:pw height:ph mipmapped:NO];
        clearStencilDesc.usage = MTLTextureUsageRenderTarget;
        clearStencilDesc.storageMode = MTLStorageModePrivate;
        clearStencilTex = [layer.device newTextureWithDescriptor:clearStencilDesc];
        if (clearStencilTex != nil) {
            clearPass.stencilAttachment.texture = clearStencilTex;
            clearPass.stencilAttachment.loadAction = MTLLoadActionClear;
            clearPass.stencilAttachment.storeAction = MTLStoreActionDontCare;
            clearPass.stencilAttachment.clearStencil = 0;
        }
    }
    id<MTLRenderCommandEncoder> clearEnc = [clearCb renderCommandEncoderWithDescriptor:clearPass];
#ifndef CN1_USE_ARC
    // renderCommandEncoderWithDescriptor: retains the attachment for the pass.
    [clearStencilTex release];
#endif
    if (oldScreen != nil) {
        [clearEnc setViewport:(MTLViewport){ 0.0, 0.0, (double)pw, (double)ph, 0.0, 1.0 }];
        CN1MetalBeginFrame(clearEnc, projectionMatrix, pw, ph);
        // Stretch the whole previous frame to fill the new drawable. A pure
        // scale (no rotation) is imperfect across a portrait<->landscape swap
        // but is only on screen for the rotation animation and is far less
        // jarring than a black flash. screenTexture is rendered with the same
        // y-down projection, so CN1MetalDrawImage's V=0-at-top mapping keeps
        // the old top at the new top (no vertical flip needed).
        // Drawn to the screen drawable (not into a mutable target), so the
        // Catalyst mutable-into-mutable V-flip does not apply here: pass NO.
        CN1MetalDrawImage(oldScreen, 255, 0, 0, pw, ph, NO);
        CN1MetalEndFrame();
    }
    [clearEnc endEncoding];
    [clearCb commit];

    // Build a matching Stencil8 attachment for polygon-shape clipping
    // (#3921). Private storage rather than Memoryless because Memoryless
    // is only supported on tile-based deferred GPUs (iOS Simulator on
    // older Intel-Mac CI runners doesn't accept it). The stencil is
    // ephemeral conceptually but Private works on all GPU families and
    // the size cost is tiny (1 byte/pixel).
    MTLTextureDescriptor *stencilDesc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatStencil8
        width:pw height:ph mipmapped:NO];
    stencilDesc.usage = MTLTextureUsageRenderTarget;
    stencilDesc.storageMode = MTLStorageModePrivate;
    id<MTLTexture> newStencil = [layer.device newTextureWithDescriptor:stencilDesc];
    self.stencilTexture = newStencil;
#ifndef CN1_USE_ARC
    [newStencil release];
#endif

    // Push the preserved frame onto the layer so the rotation never shows
    // black (#5162) -- but NOT synchronously here. updateFrameBufferSize: runs
    // from viewWillTransitionToSize: on the main thread, inside UIKit's
    // rotation CATransaction, with the layer.drawableSize change above still
    // pending/uncommitted in that transaction. Calling [layer nextDrawable]
    // now blocks: the layer cannot vend a drawable until the resize transaction
    // commits, and that transaction cannot commit until viewWillTransitionToSize:
    // returns -- which it cannot, because we are blocked in nextDrawable. On the
    // simulator CoreAnimation tolerates this (which is why the original #5162
    // fix, verified only in the simulator, appeared to work); on a real device
    // the render server wedges and, because the EDT renders via
    // dispatch_sync(main), the EDT blocks on the stalled main thread forever --
    // the hard rotation freeze (#5171).
    //
    // Deferring the present to the next main-runloop turn breaks the cycle: by
    // then viewWillTransitionToSize: has returned and the implicit drawableSize
    // transaction has committed, so nextDrawable vends a correctly-sized
    // drawable exactly as the normal presentFramebuffer path does -- no
    // in-transaction wedge, no freeze. The needsResizePresent guard makes this
    // self-tuning: when the app is idle (the #5162 case) the EDT is asleep, so
    // the deferred block runs and fills the rotation gap with the preserved
    // frame; when the app is actively painting (the #5171 case) the EDT repaints
    // first, presentFramebuffer clears the guard, and the deferred block becomes
    // a no-op -- so it never contends for a drawable in exactly the scenario
    // that used to deadlock.
    if (oldScreen != nil) {
        needsResizePresent = YES;
        dispatch_async(dispatch_get_main_queue(), ^{
            [self presentPreservedFrameIfNeeded];
        });
    }
#ifndef CN1_USE_ARC
    [oldScreen release];
#endif
}

// Push the frame currently held in screenTexture (the stretched previous frame
// preserved across a resize by updateFrameBufferSize:) onto the CAMetalLayer,
// so an idle rotation shows the last content rather than the layer's black
// background until the EDT repaints (#5162). A no-op once needsResizePresent
// has been cleared -- either because a normal presentFramebuffer already put a
// real frame up, or because a later resize superseded this one. Runs on the
// main thread (scheduled via dispatch_async from updateFrameBufferSize:),
// outside the rotation CATransaction, so nextDrawable here behaves exactly like
// the normal present path and cannot deadlock (#5171).
-(void)presentPreservedFrameIfNeeded {
    if (!needsResizePresent) {
        return;
    }
    needsResizePresent = NO;
    if (self.screenTexture == nil) {
        return;
    }
    // An encoder may be mid-frame if the EDT started painting between the resize
    // and this turn; in that case the normal present path owns the drawable and
    // will have cleared needsResizePresent already, so we would have returned
    // above. Guard anyway: never present while an encoder is open.
    if (self.renderCommandEncoder != nil) {
        return;
    }
    CAMetalLayer *layer = (CAMetalLayer*)self.layer;
    id<CAMetalDrawable> dr = [layer nextDrawable];
    if (dr == nil) {
        return;
    }
    id<MTLCommandBuffer> presentCb = [self.commandQueue commandBuffer];
    id<MTLBlitCommandEncoder> blit = [presentCb blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture
              sourceSlice:0 sourceLevel:0
             sourceOrigin:MTLOriginMake(0, 0, 0)
               sourceSize:MTLSizeMake(framebufferWidth, framebufferHeight, 1)
                toTexture:dr.texture
         destinationSlice:0 destinationLevel:0
        destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [presentCb presentDrawable:dr];
    [presentCb commit];
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
    // Attach the Stencil8 texture for polygon-shape clipping (#3921).
    // Cleared at the start of every frame and discarded at the end --
    // stencil values from previous frames are never referenced, and the
    // reference-value counter in CN1Metalcompat resets per encoder, so
    // a fresh clear is the right semantics.
    if (self.stencilTexture != nil) {
        MTLRenderPassStencilAttachmentDescriptor* stencilAttachment = self.renderPassDescriptor.stencilAttachment;
        stencilAttachment.texture = self.stencilTexture;
        stencilAttachment.loadAction = MTLLoadActionClear;
        stencilAttachment.storeAction = MTLStoreActionDontCare;
        stencilAttachment.clearStencil = 0;
    }
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
        // previous present). Nothing to do. Leave needsResizePresent set: the
        // gap-filler is still wanted because no real frame is being presented.
        self.commandBuffer = nil;
        return NO;
    }
    // A real, correctly-laid-out frame is about to reach the layer, so the
    // post-resize gap-filler is no longer needed; clear the guard so the
    // deferred presentPreservedFrameIfNeeded does not later present the stale
    // stretched frame on top of this one (which would look like a backwards
    // flicker). See updateFrameBufferSize: (#5162/#5171).
    needsResizePresent = NO;
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
extern BOOL currentlyReturnExitsEditing;
- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
    NSUInteger newLength = (textField.text.length - range.length) + string.length;
    return (newLength <= currentlyEditingMaxLength);
}

-(BOOL)textView:(UITextView *)textView shouldChangeTextInRange:(NSRange)range replacementText:(NSString *)text {
    // iosReturnExitsEditing: treat a Return keypress on a multi-line text view as Done.
    // Only intercept a single "\n" replacement so pasted text containing newlines is
    // unaffected.
    if (currentlyReturnExitsEditing && [text isEqualToString:@"\n"]) {
        [self keyboardDoneClicked];
        return NO;
    }
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