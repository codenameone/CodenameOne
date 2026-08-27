/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
#import <TargetConditionals.h>
#if TARGET_OS_OSX

#import "METALView.h"
#import "CN1AppKitCompat.h"
#import "CN1MacTextInput.h"
#import "CN1Metalcompat.h"
#include "cn1_globals.h"

/// Orthographic projection with the origin at the top left.
///
/// Passing bottom=h and top=0 makes y=0 land at the top of the drawable, which
/// is where Codename One expects it. The alternative is a flip in the model-view
/// matrix and a compensating translate, which the OpenGL path had to do and
/// which is a permanent source of off-by-one at the edges.
static simd_float4x4 CN1MacOrtho(float left, float right, float bottom, float top,
                                 float nearZ, float farZ) {
    float ral = right + left, rsl = right - left;
    float tab = top + bottom, tsb = top - bottom;
    float fan = farZ + nearZ, fsn = farZ - nearZ;
    simd_float4x4 m;
    m.columns[0] = (simd_float4){ 2.0f / rsl, 0.0f, 0.0f, 0.0f };
    m.columns[1] = (simd_float4){ 0.0f, 2.0f / tsb, 0.0f, 0.0f };
    m.columns[2] = (simd_float4){ 0.0f, 0.0f, -2.0f / fsn, 0.0f };
    m.columns[3] = (simd_float4){ -ral / rsl, -tab / tsb, -fan / fsn, 1.0f };
    return m;
}

@implementation METALView {
    NSTrackingArea *cn1TrackingArea;
    NSMutableIndexSet *consumedKeys;
    NSUInteger selectionAnchor;
    BOOL selectionAnchorValid;
    /// The magnify gesture's factor since it began. AppKit reports increments;
    /// pinch() wants the total, so it is accumulated across the phase.
    double cn1PinchScale;
}

@synthesize commandQueue;
@synthesize commandBuffer;
@synthesize renderPassDescriptor;
@synthesize renderCommandEncoder;
@synthesize screenTexture;
@synthesize stencilTexture;
@synthesize peerComponentsLayer;
@synthesize framebufferWidth;
@synthesize framebufferHeight;
@synthesize projectionMatrix;
@synthesize cn1WindowId;
@synthesize cn1InputEnabled;

// ---- view configuration -------------------------------------------------

/// Layer-hosted rather than layer-backed. Returning the CAMetalLayer from here
/// means AppKit adopts it as the view's own layer and never draws into it, so
/// Metal is the only thing that touches those pixels.
- (CALayer *)makeBackingLayer {
    return [CAMetalLayer layer];
}

- (BOOL)wantsUpdateLayer {
    return YES;
}

/// Top-left origin, matching Codename One's coordinate space. This is the single
/// highest-leverage line in the port: without it every boundary -- mouse
/// locations, tracking areas, peer frames, the text input client's rects --
/// needs its own vertical flip, and each new one is a fresh chance to get it
/// wrong.
- (BOOL)isFlipped {
    return YES;
}

- (BOOL)isOpaque {
    return YES;
}

- (BOOL)acceptsFirstResponder {
    return YES;
}

/// A click that activates the window should also reach the app, which is what a
/// Codename One user expects from a tap and is not AppKit's default.
- (BOOL)acceptsFirstMouse:(NSEvent *)event {
    return YES;
}

- (instancetype)initWithFrame:(NSRect)frameRect {
    self = [super initWithFrame:frameRect];
    if (self) {
        // Negative means "the application's own window", which has no framework
        // window id. A secondary window overwrites this at creation.
        self.cn1WindowId = -1;
        self.cn1InputEnabled = YES;
        [self cn1SetupMetal];
    }
    return self;
}

- (void)cn1SetupMetal {
    self.wantsLayer = YES;
    self.layerContentsRedrawPolicy = NSViewLayerContentsRedrawDuringViewResize;
    // Without TopLeft the layer scales its last frame while the user drags a
    // resize, which reads as a smear rather than a redraw.
    self.layerContentsPlacement = NSViewLayerContentsPlacementTopLeft;

    CAMetalLayer *metalLayer = (CAMetalLayer *)self.layer;
    // The FIRST view to get here decides the device and the command queue for
    // the whole process; every later window reuses them.
    //
    // Not a micro-optimisation. Mutable-image work commits to the queue
    // CN1Metalcompat publishes and relies on FIFO ordering against the screen
    // render, so a second window that minted its own queue and republished it
    // would leave the first window's screen render on a queue Metal orders
    // against nothing -- a mutable sampled before its writes land, which shows
    // up as a stale or torn frame rather than as an error. A texture is also
    // bound to the device that made it, so two devices cannot share one.
    id<MTLDevice> device = CN1MetalDevice();
    BOOL ownsDevice = NO;
    if (device == nil) {
        device = MTLCreateSystemDefaultDevice();
        ownsDevice = YES;
    }
    metalLayer.device = device;
    metalLayer.opaque = YES;
    metalLayer.pixelFormat = MTLPixelFormatBGRA8Unorm;
    // Must be NO: presentFramebuffer blits screenTexture into the drawable, and
    // Metal's blit validation rejects a framebufferOnly destination.
    metalLayer.framebufferOnly = NO;
    metalLayer.maximumDrawableCount = 3;

    // sRGB so CoreGraphics-rasterised images and gradients, which carry
    // DeviceRGB bytes, are not treated as linear and displayed too bright.
    CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
    if (cs != NULL) {
        metalLayer.colorspace = cs;
        CGColorSpaceRelease(cs);
    }

    id<MTLCommandQueue> queue = CN1MetalCommandQueue();
    if (queue == nil) {
        queue = [device newCommandQueue];
        self.commandQueue = queue;
#ifndef CN1_USE_ARC
        [queue release];
#endif
    } else {
        self.commandQueue = queue;
    }
    // Published once, on the main thread, so CN1Metalcompat's accessors are
    // cheap static reads from the EDT and any background queue rather than a
    // dereference of this view's layer. A repeat call with the values already
    // held is a no-op, which is what every window after the first makes.
    CN1MetalSetDeviceAndCommandQueue(device, self.commandQueue);
#ifndef CN1_USE_ARC
    if (ownsDevice) {
        // MTLCreateSystemDefaultDevice returns +1; the layer and the compat
        // cache both hold their own retain by now.
        [device release];
    }
#else
    (void)ownsDevice;
#endif

    [self updateBackingSize];
}

- (void)updateBackingSize {
    CGFloat s = CN1AppKitBackingScale(self);
    NSSize sz = self.bounds.size;
    [self updateFrameBufferSize:(int)(sz.width * s) h:(int)(sz.height * s)];
}

/// Re-asked rather than cached: dragging a window between a Retina and a
/// non-Retina display changes the answer while the app is running.
- (void)viewDidChangeBackingProperties {
    [super viewDidChangeBackingProperties];
    // The shared point<->pixel global as well as this view's framebuffer. The
    // two are read by different code -- the renderer re-asks the view, while the
    // peer layout and the native pickers read the global -- so refreshing only
    // one leaves them disagreeing after the window moves to a display with a
    // different backing scale. The host resolves it against the MAIN window, so
    // a secondary view calling this cannot claim the value.
    extern void CN1MacRefreshScaleValue(void);
    CN1MacRefreshScaleValue();
    [self updateBackingSize];
}

- (void)setFrameSize:(NSSize)newSize {
    [super setFrameSize:newSize];
    [self updateBackingSize];
}

// ---- framebuffer --------------------------------------------------------

- (void)updateFrameBufferSize:(int)w h:(int)h {
    int pw = w, ph = h;
    if (pw <= 0 || ph <= 0) {
        CGFloat s = CN1AppKitBackingScale(self);
        pw = (int)(self.bounds.size.width * s);
        ph = (int)(self.bounds.size.height * s);
    }
    if (pw <= 0 || ph <= 0 || (pw == framebufferWidth && ph == framebufferHeight)) {
        return;
    }
    // An encoder may be mid-frame. Tear it down rather than letting draws land
    // on a texture about to be replaced, which would also leave the stale
    // dimensions cached inside CN1Metalcompat and break scissor clamping.
    if (self.renderCommandEncoder != nil) {
        CN1MetalEndFrame();
        [self.renderCommandEncoder endEncoding];
        self.renderCommandEncoder = nil;
    }
    if (self.commandBuffer != nil) {
        [self.commandBuffer commit];
        self.commandBuffer = nil;
        self.renderPassDescriptor = nil;
    }

    BOOL sizeWasKnown = framebufferWidth > 0 && framebufferHeight > 0;
    framebufferWidth = pw;
    framebufferHeight = ph;
    projectionMatrix = CN1MacOrtho(0.0f, (float)pw, (float)ph, 0.0f, -1.0f, 1.0f);

    CAMetalLayer *layer = (CAMetalLayer *)self.layer;
    layer.contentsScale = CN1AppKitBackingScale(self);
    layer.drawableSize = CGSizeMake(pw, ph);

    // The persistent target. Codename One only queues the operations that
    // changed since the previous frame, so it has to survive between them; a
    // Metal drawable does not, being cleared on acquire.
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
        width:pw height:ph mipmapped:NO];
    desc.usage = MTLTextureUsageRenderTarget | MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModePrivate;
    id<MTLTexture> newScreen = [layer.device newTextureWithDescriptor:desc];
    self.screenTexture = newScreen;
#ifndef CN1_USE_ARC
    [newScreen release];
#endif

    // Private storage comes back uninitialised, and the first frame loads
    // rather than clears, so anything not yet drawn would sample garbage.
    id<MTLCommandBuffer> clearCb = [self.commandQueue commandBuffer];
    MTLRenderPassDescriptor *clearPass = [MTLRenderPassDescriptor renderPassDescriptor];
    clearPass.colorAttachments[0].texture = self.screenTexture;
    clearPass.colorAttachments[0].loadAction = MTLLoadActionClear;
    clearPass.colorAttachments[0].storeAction = MTLStoreActionStore;
    clearPass.colorAttachments[0].clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0);
    id<MTLRenderCommandEncoder> clearEnc =
        [clearCb renderCommandEncoderWithDescriptor:clearPass];
    [clearEnc endEncoding];
    [clearCb commit];

    // Every pipeline in the cache declares a Stencil8 stencil format for
    // polygon clipping, and Metal aborts if a pass binds one without a matching
    // attachment -- so this exists whether or not the frame actually clips.
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

    // The framework has to hear about it, not just the framebuffer. Resizing the
    // main NSWindow reallocated the Metal target and stopped there, so the
    // current Form kept its old bounds: laid out and repainted for a size the
    // window no longer has. Secondary windows never had this problem because
    // their delegate calls CN1MacWindowDeliverResize; window 0 has no such
    // delegate and this is its equivalent.
    //
    // Skipped for the first sizing, where there is no previous size to have
    // changed from and the framework has not been told a size at all yet -- and
    // skipped until Java exists, since this runs from AppKit before the app's
    // main thread has constructed anything.
    // The MAIN view only. screenSizeChanged() sets the framework's one display
    // size, so a secondary window running through here would resize the main
    // Form to the auxiliary window's dimensions. Those windows have their own
    // channel: CN1MacWindowRecord.windowDidResize: already sends
    // CN1MacWindowDeliverResize for them, which is per window by construction.
    // cn1WindowId is -1 for the host's view and >= 0 for every created window,
    // the same test the pointer and key paths use.
    extern void screenSizeChanged(int width, int height);
    extern BOOL cn1MacRuntimeIsJavaReady(void);
    if (sizeWasKnown && self.cn1WindowId < 0 && cn1MacRuntimeIsJavaReady()) {
        screenSizeChanged(pw, ph);
    }
}

- (void)createRenderPassDescriptor {
    if (self.screenTexture == nil) {
        self.renderPassDescriptor = nil;
        return;
    }
    self.renderPassDescriptor = [MTLRenderPassDescriptor renderPassDescriptor];
    MTLRenderPassColorAttachmentDescriptor *color =
        self.renderPassDescriptor.colorAttachments[0];
    color.texture = self.screenTexture;
    // Load, not clear: Codename One queues only the operations that changed, so
    // the previous frame's pixels have to still be there.
    if (clearRetainedFramebufferOnNextFrame) {
        color.loadAction = MTLLoadActionClear;
        color.clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0);
        clearRetainedFramebufferOnNextFrame = NO;
    } else {
        color.loadAction = MTLLoadActionLoad;
    }
    color.storeAction = MTLStoreActionStore;
    if (self.stencilTexture != nil) {
        MTLRenderPassStencilAttachmentDescriptor *stencil =
            self.renderPassDescriptor.stencilAttachment;
        stencil.texture = self.stencilTexture;
        stencil.loadAction = MTLLoadActionClear;
        stencil.storeAction = MTLStoreActionDontCare;
        stencil.clearStencil = 0;
    }
}

- (void)setFramebuffer {
    // Tolerates being called more than once per frame, as the UIKit backend
    // does: creating a second encoder would discard everything queued against
    // the first. Only presentFramebuffer ends and commits.
    if (self.renderCommandEncoder != nil) {
        return;
    }
    self.commandBuffer = [self.commandQueue commandBuffer];
    [self createRenderPassDescriptor];
    if (self.renderPassDescriptor == nil) {
        self.renderCommandEncoder = nil;
        return;
    }
    self.renderCommandEncoder =
        [self.commandBuffer renderCommandEncoderWithDescriptor:self.renderPassDescriptor];
    [self.renderCommandEncoder setViewport:(MTLViewport){
        0.0, 0.0, (double)framebufferWidth, (double)framebufferHeight, 0.0, 1.0 }];
    // Publish encoder and projection so each ExecutableOp's Metal branch finds
    // them. The global is correct here because the event dispatch thread paints
    // one window at a time.
    CN1MetalBeginFrame(self.renderCommandEncoder, projectionMatrix,
                       framebufferWidth, framebufferHeight);
}

- (BOOL)presentFramebuffer {
    if (self.renderCommandEncoder == nil) {
        self.commandBuffer = nil;
        return NO;
    }
    CN1MetalEndFrame();
    [self.renderCommandEncoder endEncoding];
    self.renderCommandEncoder = nil;
    self.renderPassDescriptor = nil;

    // Acquired here rather than in setFramebuffer to keep its dwell time short:
    // holding a drawable across the whole encoding phase stalls nextDrawable for
    // the frames behind it.
    CAMetalLayer *layer = (CAMetalLayer *)self.layer;
    id<CAMetalDrawable> dr = [layer nextDrawable];
    if (dr == nil) {
        [self.commandBuffer commit];
        self.commandBuffer = nil;
        return NO;
    }
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
    self.commandBuffer = nil;
    return YES;
}

- (void)deleteFramebuffer {
    self.screenTexture = nil;
    self.stencilTexture = nil;
    framebufferWidth = 0;
    framebufferHeight = 0;
}

- (void)invalidateRetainedFramebuffer {
    retainedFramebufferInvalid = YES;
    // Cleared unconditionally on the next frame rather than waiting for a
    // repaint that covers the whole screen: a partial repaint routinely lands
    // first, loads the stale texture, and pins whatever the discarded contents
    // left behind until something dirties the region again.
    clearRetainedFramebufferOnNextFrame = YES;
}

- (void)prepareRetainedFramebufferForDrawRect:(CGRect)rect displayWidth:(int)displayWidth
                                displayHeight:(int)displayHeight {
    // Only when a clear is actually owed. A full-screen repaint is the moment
    // it is safe to take one, not a reason to: the framework queues only the
    // operations that changed, so wiping on every full repaint erases
    // everything drawn in an earlier frame that this one does not touch.
    if (!retainedFramebufferInvalid) {
        return;
    }
    // The retained target's own size, not the reported display size: the two
    // can disagree briefly while a resize settles, and consuming the
    // invalidation against the wrong one clears a target the repaint does not
    // actually cover.
    int targetWidth = framebufferWidth > 0 ? framebufferWidth : displayWidth;
    int targetHeight = framebufferHeight > 0 ? framebufferHeight : displayHeight;
    if (targetWidth <= 0 || targetHeight <= 0) {
        return;
    }
    if (rect.origin.x <= 0 && rect.origin.y <= 0
            && rect.origin.x + rect.size.width >= targetWidth
            && rect.origin.y + rect.size.height >= targetHeight) {
        clearRetainedFramebufferOnNextFrame = YES;
        retainedFramebufferInvalid = NO;
    }
}

// ---- readback -----------------------------------------------------------

- (BOOL)readbackInto:(unsigned int *)argb width:(int)w height:(int)h {
    if (argb == NULL || self.screenTexture == nil
            || w != framebufferWidth || h != framebufferHeight) {
        return NO;
    }
    // Blit into shared storage first: the screen texture is private, so the CPU
    // cannot read it directly.
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
        width:w height:h mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModeShared;
    id<MTLTexture> staging = [((CAMetalLayer *)self.layer).device newTextureWithDescriptor:desc];
    if (staging == nil) {
        return NO;
    }
    id<MTLCommandBuffer> cb = [self.commandQueue commandBuffer];
    id<MTLBlitCommandEncoder> blit = [cb blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture
              sourceSlice:0 sourceLevel:0
             sourceOrigin:MTLOriginMake(0, 0, 0)
               sourceSize:MTLSizeMake(w, h, 1)
                toTexture:staging
         destinationSlice:0 destinationLevel:0
        destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [cb commit];
    [cb waitUntilCompleted];
    [staging getBytes:argb bytesPerRow:(NSUInteger)w * 4
           fromRegion:MTLRegionMake2D(0, 0, w, h) mipmapLevel:0];
#ifndef CN1_USE_ARC
    [staging release];
#endif
    return YES;
}

// ---- peer components ----------------------------------------------------

- (void)addPeerComponent:(CN1View *)view {
    if (view == nil) {
        return;
    }
    if (self.peerComponentsLayer == nil) {
        NSView *host = [[NSView alloc] initWithFrame:self.bounds];
        host.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
        self.peerComponentsLayer = host;
        [self addSubview:host];
#ifndef CN1_USE_ARC
        [host release];
#endif
    }
    [self.peerComponentsLayer addSubview:view];
}

// ---- input --------------------------------------------------------------

// The framework's own C bridges. Reused rather than replaced: the Java side of
// input is identical on every Apple port, and the only thing that differs is
// what produces the events -- UITouch there, NSEvent here.
extern void pointerPressed(int *x, int *y, int length);
extern void pointerDragged(int *x, int *y, int length);
extern void pointerReleased(int *x, int *y, int length);
extern void CN1MacPointerButton(int button, int mask, int modifiers);
extern void CN1MacCopyTextSelection(void);
extern void CN1MacPinchBegin(void);
extern void CN1MacPinchRelease(int x, int y);
extern void pointerHoverNative(int x, int y);
extern void pointerHoverPressedNative(int x, int y);
extern void pointerHoverReleasedNative(int x, int y);
extern void keyPressedNative(int keyCode);
extern void keyReleasedNative(int keyCode);
extern void pointerWheelMovedCallback(int x, int y, int scrollX, int scrollY,
                                      int precise, int modifiers);
extern void pinchMagnifyCallback(float scale, int x, int y);
extern void rotationGestureCallback(float radians, int x, int y);

// The per-window equivalents, shared with Mac Catalyst. A secondary window's
// events carry its id so Desktop can route them without a lookup on AppKit's
// thread; the main window has no id and uses the bridges above, which is also
// what keeps the single window screenshot baselines exercising the identical
// Java path they always did.
extern void CN1MacWindowDeliverPointer(int windowId, int type, int x, int y);
extern void CN1MacWindowDeliverHover(int windowId, int type, int x, int y);
extern void CN1MacWindowDeliverWheel(int windowId, int x, int y, int scrollX, int scrollY,
                                     int precise, int modifiers);
extern void CN1MacWindowDeliverPinch(int windowId, float scale, int x, int y);
extern void CN1MacWindowDeliverRotation(int windowId, float radians, int x, int y);
extern void CN1MacWindowDeliverKey(int windowId, int keyCode, BOOL pressed);

- (CGPoint)cn1PointFromEvent:(NSEvent *)event {
    NSPoint p = [self convertPoint:event.locationInWindow fromView:nil];
    // The view is flipped, so p is already top-left based. Only the scale to
    // device pixels remains, which is what Codename One lays out in.
    CGFloat s = CN1AppKitBackingScale(self);
    return CGPointMake(p.x * s, p.y * s);
}

/// Pointer event types as Desktop.windowPointerCallback reads them.
#define CN1_POINTER_PRESSED  1
#define CN1_POINTER_RELEASED 2
#define CN1_POINTER_DRAGGED  3

- (void)cn1Deliver:(NSEvent *)event to:(void (*)(int *, int *, int))fn type:(int)type {
    if (!self.cn1InputEnabled) {
        return;
    }
    CGPoint p = [self cn1PointFromEvent:event];
    int x = (int)p.x, y = (int)p.y;
    if (self.cn1WindowId >= 0) {
        CN1MacWindowDeliverPointer(self.cn1WindowId, type, x, y);
        return;
    }
    fn(&x, &y, 1);
}

// PointerEvent constants, kept here rather than imported because the Java side
// is what defines them and this file cannot see it.
#define CN1_PE_BUTTON_PRIMARY   0
#define CN1_PE_BUTTON_SECONDARY 1
#define CN1_PE_MASK_PRIMARY     (1 << 0)
#define CN1_PE_MASK_SECONDARY   (1 << 1)
#define CN1_PE_BUTTON_MIDDLE    2
#define CN1_PE_MASK_MIDDLE      (1 << 2)
// Back and forward are first-class in PointerEvent -- BUTTON_BACK/FORWARD and
// MASK_BACK/FORWARD -- so a mouse that has them must not have them reported as
// the middle button.
#define CN1_PE_BUTTON_NONE      (-1)
#define CN1_PE_BUTTON_BACK      3
#define CN1_PE_BUTTON_FORWARD   4
#define CN1_PE_MASK_BACK        (1 << 3)
#define CN1_PE_MASK_FORWARD     (1 << 4)
#define CN1_PE_MODIFIER_SHIFT   (1 << 0)
#define CN1_PE_MODIFIER_CONTROL (1 << 1)
#define CN1_PE_MODIFIER_ALT     (1 << 2)
#define CN1_PE_MODIFIER_META    (1 << 3)

/// PointerEvent's modifier mask for the keys held during this event.
static int cn1ModifiersOf(NSEvent *event) {
    NSEventModifierFlags flags = event.modifierFlags;
    int m = 0;
    if (flags & NSEventModifierFlagShift)   { m |= CN1_PE_MODIFIER_SHIFT; }
    if (flags & NSEventModifierFlagControl) { m |= CN1_PE_MODIFIER_CONTROL; }
    // Option is what Alt is called on a Mac keyboard, and Command is Meta.
    if (flags & NSEventModifierFlagOption)  { m |= CN1_PE_MODIFIER_ALT; }
    if (flags & NSEventModifierFlagCommand) { m |= CN1_PE_MODIFIER_META; }
    return m;
}

/// The buttons currently HELD, which is what getButtonMask() answers.
///
/// Read from AppKit rather than staged from the event that triggered the call:
/// on a release the triggering button is no longer down, and reporting it as
/// held told every release listener the opposite of what had just happened.
static int cn1HeldButtonMask(void) {
    NSUInteger pressed = [NSEvent pressedMouseButtons];
    int mask = 0;
    if (pressed & (1 << 0)) { mask |= CN1_PE_MASK_PRIMARY; }
    if (pressed & (1 << 1)) { mask |= CN1_PE_MASK_SECONDARY; }
    if (pressed & (1 << 2)) { mask |= CN1_PE_MASK_MIDDLE; }
    if (pressed & (1 << 3)) { mask |= CN1_PE_MASK_BACK; }
    if (pressed & (1 << 4)) { mask |= CN1_PE_MASK_FORWARD; }
    return mask;
}

/// The Codename One button for an AppKit buttonNumber.
///
/// AppKit routes everything that is not left or right through otherMouse*, so
/// the number is the only thing that distinguishes middle from back from
/// forward. Labelling them all BUTTON_MIDDLE meant an application could not tell
/// a navigation button from a middle click and ran middle-click behaviour for
/// it. A number beyond forward has no PointerEvent equivalent, so it reports
/// BUTTON_NONE rather than claiming to be a button it is not.
static int cn1ButtonFromNumber(NSInteger buttonNumber) {
    switch (buttonNumber) {
        case 0: return CN1_PE_BUTTON_PRIMARY;
        case 1: return CN1_PE_BUTTON_SECONDARY;
        case 2: return CN1_PE_BUTTON_MIDDLE;
        case 3: return CN1_PE_BUTTON_BACK;
        case 4: return CN1_PE_BUTTON_FORWARD;
        default: return CN1_PE_BUTTON_NONE;
    }
}

- (void)mouseDown:(NSEvent *)event {
    CN1MacPointerButton(CN1_PE_BUTTON_PRIMARY, cn1HeldButtonMask(), cn1ModifiersOf(event));
    [self cn1Deliver:event to:pointerPressed type:CN1_POINTER_PRESSED];
}

- (void)mouseDragged:(NSEvent *)event {
    CN1MacPointerButton(CN1_PE_BUTTON_PRIMARY, cn1HeldButtonMask(), cn1ModifiersOf(event));
    [self cn1Deliver:event to:pointerDragged type:CN1_POINTER_DRAGGED];
}

- (void)mouseUp:(NSEvent *)event {
    CN1MacPointerButton(CN1_PE_BUTTON_PRIMARY, cn1HeldButtonMask(), cn1ModifiersOf(event));
    [self cn1Deliver:event to:pointerReleased type:CN1_POINTER_RELEASED];
}

// Every button that is not left or right. AppKit routes them all through
// otherMouse*, so without these three a middle click produced no pointer event
// at all -- and reporting them all as the middle button, which is what this did
// first, is the same mistake one step in: buttonNumber is what tells middle from
// back from forward.
- (void)otherMouseDown:(NSEvent *)event {
    CN1MacPointerButton(cn1ButtonFromNumber(event.buttonNumber), cn1HeldButtonMask(),
                        cn1ModifiersOf(event));
    [self cn1Deliver:event to:pointerPressed type:CN1_POINTER_PRESSED];
}

- (void)otherMouseDragged:(NSEvent *)event {
    CN1MacPointerButton(cn1ButtonFromNumber(event.buttonNumber), cn1HeldButtonMask(),
                        cn1ModifiersOf(event));
    [self cn1Deliver:event to:pointerDragged type:CN1_POINTER_DRAGGED];
}

- (void)otherMouseUp:(NSEvent *)event {
    CN1MacPointerButton(cn1ButtonFromNumber(event.buttonNumber), cn1HeldButtonMask(),
                        cn1ModifiersOf(event));
    [self cn1Deliver:event to:pointerReleased type:CN1_POINTER_RELEASED];
}

// Right and middle buttons reach Codename One as ordinary pointer events,
// because its input model has one pointer. What distinguishes them is the
// metadata recorded first: without it every click read as primary, so
// PointerEvent.isSecondaryButton() was false for a right click and the
// framework's context-menu and selection logic could not tell the two apart.
// The Linux and native Windows ports set the same metadata from their own input
// handlers. A context menu, if the application wants one, still comes from
// menuForEvent: rather than from a second button here.
//
// Display.isRightMouseButtonDown() is a separate, older API that reads no
// metadata; the base implementation answers false and only JavaSE overrides it,
// so it stays false here as it does on Linux and Windows.
- (void)rightMouseDown:(NSEvent *)event {
    CN1MacPointerButton(CN1_PE_BUTTON_SECONDARY, cn1HeldButtonMask(), cn1ModifiersOf(event));
    [self cn1Deliver:event to:pointerPressed type:CN1_POINTER_PRESSED];
}

- (void)rightMouseDragged:(NSEvent *)event {
    CN1MacPointerButton(CN1_PE_BUTTON_SECONDARY, cn1HeldButtonMask(), cn1ModifiersOf(event));
    [self cn1Deliver:event to:pointerDragged type:CN1_POINTER_DRAGGED];
}

- (void)rightMouseUp:(NSEvent *)event {
    CN1MacPointerButton(CN1_PE_BUTTON_SECONDARY, cn1HeldButtonMask(), cn1ModifiersOf(event));
    [self cn1Deliver:event to:pointerReleased type:CN1_POINTER_RELEASED];
}

- (void)mouseMoved:(NSEvent *)event {
    if (!self.cn1InputEnabled) {
        return;
    }
    // Staged for the hover too, not only for clicks. The metadata is sticky --
    // it is whatever the last press left behind -- so moving the mouse after a
    // right click reported a hovering SECONDARY button with that click's
    // modifiers, and before any click at all it reported TYPE_UNKNOWN with the
    // default primary button. PointerEvent documents hover as BUTTON_NONE, and
    // the mask still carries whatever is genuinely held so a drag-hover is not
    // misreported as nothing held.
    CN1MacPointerButton(CN1_PE_BUTTON_NONE, cn1HeldButtonMask(), cn1ModifiersOf(event));
    CGPoint p = [self cn1PointFromEvent:event];
    if (self.cn1WindowId >= 0) {
        CN1MacWindowDeliverHover(self.cn1WindowId, CN1_POINTER_DRAGGED, (int)p.x, (int)p.y);
        return;
    }
    pointerHoverNative((int)p.x, (int)p.y);
}

/// The pointer entered the view. The tracking area below asks for this, and
/// without a handler an application overriding pointerHoverPressed() never heard
/// about a pointer arriving -- on the main form or in a secondary window.
- (void)mouseEntered:(NSEvent *)event {
    if (!self.cn1InputEnabled) {
        return;
    }
    // Staged for the hover too, not only for clicks. The metadata is sticky --
    // it is whatever the last press left behind -- so moving the mouse after a
    // right click reported a hovering SECONDARY button with that click's
    // modifiers, and before any click at all it reported TYPE_UNKNOWN with the
    // default primary button. PointerEvent documents hover as BUTTON_NONE, and
    // the mask still carries whatever is genuinely held so a drag-hover is not
    // misreported as nothing held.
    CN1MacPointerButton(CN1_PE_BUTTON_NONE, cn1HeldButtonMask(), cn1ModifiersOf(event));
    CGPoint p = [self cn1PointFromEvent:event];
    if (self.cn1WindowId >= 0) {
        CN1MacWindowDeliverHover(self.cn1WindowId, CN1_POINTER_PRESSED, (int)p.x, (int)p.y);
        return;
    }
    pointerHoverPressedNative((int)p.x, (int)p.y);
}

/// The pointer left the view, which is the other half of the hover lifecycle.
- (void)mouseExited:(NSEvent *)event {
    if (!self.cn1InputEnabled) {
        return;
    }
    // Staged for the hover too, not only for clicks. The metadata is sticky --
    // it is whatever the last press left behind -- so moving the mouse after a
    // right click reported a hovering SECONDARY button with that click's
    // modifiers, and before any click at all it reported TYPE_UNKNOWN with the
    // default primary button. PointerEvent documents hover as BUTTON_NONE, and
    // the mask still carries whatever is genuinely held so a drag-hover is not
    // misreported as nothing held.
    CN1MacPointerButton(CN1_PE_BUTTON_NONE, cn1HeldButtonMask(), cn1ModifiersOf(event));
    CGPoint p = [self cn1PointFromEvent:event];
    if (self.cn1WindowId >= 0) {
        CN1MacWindowDeliverHover(self.cn1WindowId, CN1_POINTER_RELEASED, (int)p.x, (int)p.y);
        return;
    }
    pointerHoverReleasedNative((int)p.x, (int)p.y);
}

/// mouseMoved: is not delivered without a tracking area, and the area has to be
/// rebuilt whenever the view resizes -- InVisibleRect keeps it in step.
- (void)updateTrackingAreas {
    [super updateTrackingAreas];
    if (cn1TrackingArea != nil) {
        [self removeTrackingArea:cn1TrackingArea];
#ifndef CN1_USE_ARC
        [cn1TrackingArea release];
#endif
        cn1TrackingArea = nil;
    }
    cn1TrackingArea = [[NSTrackingArea alloc]
        initWithRect:NSZeroRect
             options:NSTrackingMouseMoved | NSTrackingMouseEnteredAndExited
                     | NSTrackingActiveInKeyWindow | NSTrackingInVisibleRect
               owner:self
            userInfo:nil];
    [self addTrackingArea:cn1TrackingArea];
}

- (void)scrollWheel:(NSEvent *)event {
    if (!self.cn1InputEnabled) {
        return;
    }
    CGFloat dx = event.scrollingDeltaX;
    CGFloat dy = event.scrollingDeltaY;
    if (dx == 0 && dy == 0) {
        return;
    }
    if (!event.hasPreciseScrollingDeltas) {
        // A discrete wheel reports lines where a trackpad reports points. Left
        // alike, a wheel notch scrolls about a twentieth of what the user
        // expects.
        dx *= 16;
        dy *= 16;
    }
    if (event.isDirectionInvertedFromDevice) {
        dx = -dx;
        dy = -dy;
    }
    // Momentum arrives as further scrollWheel: events with a momentumPhase set,
    // so it is forwarded like any other delta rather than being recomputed on
    // top of what the system already sent -- doubling it is what makes a port
    // scroll twice as far as every other Mac app.
    CGFloat s = CN1AppKitBackingScale(self);
    CGPoint p = [self cn1PointFromEvent:event];
    // Forwarded from the event, not assumed. hasPreciseScrollingDeltas is the
    // same flag the delta scaling above already branches on -- reporting a
    // notched wheel as a trackpad while multiplying its delta by 16 told the
    // framework two contradictory things about one event. And the modifier mask
    // is what a wheel listener needs to implement control-wheel-to-zoom, which
    // is the gesture the richer overload exists for; dropping it meant no
    // listener on this port could ever see a held key.
    int precise = event.hasPreciseScrollingDeltas ? 1 : 0;
    int modifiers = cn1ModifiersOf(event);
    if (self.cn1WindowId >= 0) {
        CN1MacWindowDeliverWheel(self.cn1WindowId, (int)p.x, (int)p.y,
                                 (int)(-dx * s), (int)(-dy * s), precise, modifiers);
        return;
    }
    pointerWheelMovedCallback((int)p.x, (int)p.y, (int)(-dx * s), (int)(-dy * s),
                              precise, modifiers);
}

- (void)magnifyWithEvent:(NSEvent *)event {
    // Gated like the mouse, wheel and key paths. A window the modal
    // stack has declared non-interactive could still be pinched and
    // rotated, which is content the user is not supposed to be able to
    // touch -- and unlike a click there is no framework-side filter
    // behind this one.
    if (!self.cn1InputEnabled) {
        return;
    }
    CGPoint p = [self cn1PointFromEvent:event];
    // AppKit reports the increment since the LAST event. Codename One's pinch()
    // wants the factor since the gesture STARTED: ImageViewer computes
    // currentZoom * scale against the zoom it saved when the gesture began, so
    // feeding it increments left the image parked at one increment's worth of
    // zoom however far the user pinched.
    if (event.phase == NSEventPhaseBegan) {
        cn1PinchScale = 1.0;
        // Reported so the framework can keep the whole gesture on one component
        // instead of resolving it from the coordinates on every update, and so a
        // previous gesture whose end never arrived cannot strand this one.
        CN1MacPinchBegin();
    }
    cn1PinchScale *= (1.0 + event.magnification);
    float scale = (float)cn1PinchScale;
    if (event.phase == NSEventPhaseEnded || event.phase == NSEventPhaseCancelled) {
        // A trackpad gesture produces no pointer events, so the two-pointer path
        // that normally ends a pinch never runs here and the component would
        // stay in its pinching state after the user's fingers left.
        cn1PinchScale = 1.0;
        CN1MacPinchRelease((int)p.x, (int)p.y);
        return;
    }
    if (self.cn1WindowId >= 0) {
        CN1MacWindowDeliverPinch(self.cn1WindowId, scale, (int)p.x, (int)p.y);
        return;
    }
    pinchMagnifyCallback(scale, (int)p.x, (int)p.y);
}

- (void)rotateWithEvent:(NSEvent *)event {
    // Gated like the mouse, wheel and key paths. A window the modal
    // stack has declared non-interactive could still be pinched and
    // rotated, which is content the user is not supposed to be able to
    // touch -- and unlike a click there is no framework-side filter
    // behind this one.
    if (!self.cn1InputEnabled) {
        return;
    }
    CGPoint p = [self cn1PointFromEvent:event];
    // AppKit gives degrees counter-clockwise, the callback wants radians
    // clockwise.
    float radians = (float)(-event.rotation * M_PI / 180.0);
    if (self.cn1WindowId >= 0) {
        CN1MacWindowDeliverRotation(self.cn1WindowId, radians, (int)p.x, (int)p.y);
        return;
    }
    rotationGestureCallback(radians, (int)p.x, (int)p.y);
}

/// Maps an NSEvent key to the code Codename One expects. The printable keys are
/// their own character; the rest follow the framework's negative game-action
/// convention, which is what Form's key handling matches on.
static int CN1MacKeyCode(NSEvent *event) {
    NSString *chars = event.charactersIgnoringModifiers;
    if (chars.length == 0) {
        return 0;
    }
    unichar c = [chars characterAtIndex:0];
    switch (c) {
        case NSUpArrowFunctionKey:    return -91;
        case NSDownArrowFunctionKey:  return -92;
        case NSLeftArrowFunctionKey:  return -93;
        case NSRightArrowFunctionKey: return -94;
        case NSDeleteFunctionKey:     return 127;
        case 0x7f:                    return 8;   // backspace
        case 0x0d: case 0x03:         return 10;  // return / enter
        case 0x1b:                    return 27;  // escape
        default:                      return (int)c;
    }
}

/// Hardware key codes whose press this view swallowed, so the matching release
/// can be swallowed too. Keyed on event.keyCode rather than the character,
/// because the character changes with the modifiers between down and up.
///
/// Needed because the two are gated independently in time: a press consumed by
/// an input session is followed by a release that arrives after the session has
/// ended -- pressing Return in a single-line field finishes editing, and the
/// unmatched release then reached Form.keyReleased() and fired the form's
/// default command. Gating keyUp: on the CURRENT state instead would swallow
/// the release of a key pressed before the session began.
- (NSMutableIndexSet *)cn1ConsumedKeys {
    if (consumedKeys == nil) {
        consumedKeys = [[NSMutableIndexSet alloc] init];
    }
    return consumedKeys;
}

- (void)keyDown:(NSEvent *)event {
    if (!self.cn1InputEnabled) {
        [[self cn1ConsumedKeys] addIndex:event.keyCode];
        return;
    }
    if ([CN1MacTextInputSession sharedSession].active) {
        // Hands the key to the input context, which is what turns a keystroke
        // into insertText:, setMarkedText: or doCommandBySelector:. Doing this
        // rather than reading characters directly is what buys dead keys, CJK
        // input methods, dictation and the Emacs key bindings for free.
        [[self cn1ConsumedKeys] addIndex:event.keyCode];
        [self interpretKeyEvents:@[event]];
        return;
    }
    int code = CN1MacKeyCode(event);
    if (code == 0) {
        return;
    }
    if (self.cn1WindowId >= 0) {
        CN1MacWindowDeliverKey(self.cn1WindowId, code, YES);
        return;
    }
    keyPressedNative(code);
}

// ---- NSTextInputClient ---------------------------------------------------

/// Applies an edit and answers how many characters were ACTUALLY inserted, or
/// -1 when the edit was refused.
///
/// Not a BOOL: a length-limited field truncates what it accepts, and a caller
/// that places the caret using the length it asked for puts it past the end of
/// the text. The next edit then fails the range guard and every text-input
/// query sees an invalid selection.
- (NSInteger)cn1ReplaceRange:(NSRange)range withString:(NSString *)string {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (range.location == NSNotFound) {
        return -1;
    }
    if (NSMaxRange(range) > session.text.length) {
        return -1;
    }
    NSString *inserted = string != nil ? string : @"";
    // The field's maximum, enforced here because nothing after this point does.
    // TextArea.setText() RAISES maxSize to fit an over-long value instead of
    // refusing it, so one paste past the limit disables the limit permanently.
    // Truncated rather than rejected outright, which is what a Mac text field
    // does with a paste that does not fit.
    if (session.maxSize > 0) {
        NSUInteger remaining = session.text.length - range.length;
        if (remaining >= session.maxSize) {
            // Already full, and this edit adds rather than replaces.
            if (inserted.length > 0) {
                return -1;
            }
        } else if (inserted.length > session.maxSize - remaining) {
            inserted = [inserted substringToIndex:session.maxSize - remaining];
        }
    }
    session.text = [session.text stringByReplacingCharactersInRange:range
                                                         withString:inserted];
    // Reported as the operation it is. The pure editor owns its own document and
    // cannot take a whole-text replacement; the legacy path ignores this and
    // sends the document at commit instead.
    CN1MacTextInputNotifyReplace(range, inserted);
    return (NSInteger)inserted.length;
}

/// The range an edit applies to: an explicit one if the input method gave one,
/// otherwise the marked text, otherwise the selection.
- (NSRange)cn1EffectiveRange:(NSRange)replacementRange {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (replacementRange.location != NSNotFound) {
        return replacementRange;
    }
    if (session.markedRange.location != NSNotFound) {
        return session.markedRange;
    }
    return session.selectedRange;
}

- (void)insertText:(id)string replacementRange:(NSRange)replacementRange {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (!session.active) {
        return;
    }
    [self cn1ForgetAnchor];
    NSString *inserted = [string isKindOfClass:[NSAttributedString class]]
        ? [(NSAttributedString *)string string]
        : (NSString *)string;
    if (inserted == nil) {
        return;
    }
    if (!session.multiline && [inserted rangeOfString:@"\n"].location != NSNotFound) {
        // A single line field takes a newline as "done", so a pasted paragraph
        // becomes one line rather than silently losing everything after the
        // first break.
        inserted = [[inserted componentsSeparatedByCharactersInSet:
                        [NSCharacterSet newlineCharacterSet]]
                    componentsJoinedByString:@" "];
    }
    NSRange range = [self cn1EffectiveRange:replacementRange];
    NSInteger accepted = [self cn1ReplaceRange:range withString:inserted];
    if (accepted < 0) {
        return;
    }
    session.markedRange = NSMakeRange(NSNotFound, 0);
    // The ACCEPTED length, not the requested one: a length-limited field
    // truncates a paste, and counting the original left the caret past the end
    // of the text that actually landed.
    session.selectedRange = NSMakeRange(range.location + (NSUInteger)accepted, 0);
    [session commitFinished:NO];
}

- (void)setMarkedText:(id)string
        selectedRange:(NSRange)selectedRange
     replacementRange:(NSRange)replacementRange {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (!session.active) {
        return;
    }
    NSString *marked = [string isKindOfClass:[NSAttributedString class]]
        ? [(NSAttributedString *)string string]
        : (NSString *)string;
    if (marked == nil) {
        marked = @"";
    }
    NSRange range = [self cn1EffectiveRange:replacementRange];
    NSInteger acceptedMarked = [self cn1ReplaceRange:range withString:marked];
    if (acceptedMarked < 0) {
        return;
    }
    if ((NSUInteger)acceptedMarked < marked.length) {
        // Truncated by the field's maximum, so the composing run is only as long
        // as what landed.
        marked = [marked substringToIndex:(NSUInteger)acceptedMarked];
    }
    // The marked run is pushed into the framework's text so a composition is
    // visible as it is typed. Codename One has no way to express the system
    // underline that normally distinguishes uncommitted text, so composing
    // characters read as ordinary ones until they are committed.
    session.markedRange = marked.length > 0
        ? NSMakeRange(range.location, marked.length)
        : NSMakeRange(NSNotFound, 0);
    // Clamped to what was ACCEPTED, not to what the input method proposed. The
    // two differ whenever the field's maxSize truncated the marked run: the IME
    // reports a caret inside the composition it offered, so accepting one unit
    // of a two-unit composition with the caret at offset 2 would put
    // selectedRange past the end of the document. Every later replacement-range
    // check and text-input query is validated against the document length, so
    // they all start failing and the field stops accepting input.
    NSUInteger selStart = selectedRange.location;
    if (selStart > marked.length) {
        selStart = marked.length;
    }
    NSUInteger selLength = selectedRange.length;
    if (selStart + selLength > marked.length) {
        selLength = marked.length - selStart;
    }
    session.selectedRange = NSMakeRange(range.location + selStart, selLength);
    // The composing run, so the pure editor can render it as uncommitted. The
    // replacement above already told it what changed; this says the change is
    // still being composed. The clamped caret, for the same reason.
    CN1MacTextInputNotifyComposing(marked, (NSInteger)selStart);
    [session commitFinished:NO];
}

- (void)unmarkText {
    CN1MacTextInputNotifyFinishComposing();
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    session.markedRange = NSMakeRange(NSNotFound, 0);
    [[NSTextInputContext currentInputContext] discardMarkedText];
}

- (NSRange)selectedRange {
    return [CN1MacTextInputSession sharedSession].selectedRange;
}

- (NSRange)markedRange {
    return [CN1MacTextInputSession sharedSession].markedRange;
}

- (BOOL)hasMarkedText {
    return [CN1MacTextInputSession sharedSession].markedRange.location != NSNotFound;
}

- (NSAttributedString *)attributedSubstringForProposedRange:(NSRange)range
                                                actualRange:(NSRangePointer)actualRange {
    NSString *text = [CN1MacTextInputSession sharedSession].text;
    NSRange clamped = NSIntersectionRange(range, NSMakeRange(0, text.length));
    if (actualRange != NULL) {
        *actualRange = clamped;
    }
    if (clamped.length == 0) {
        return nil;
    }
    NSAttributedString *result =
        [[NSAttributedString alloc] initWithString:[text substringWithRange:clamped]];
    // AppKit does not take ownership of what this returns, and an input method
    // asks for it repeatedly while composing -- so a +1 return under this
    // project's manual retain/release leaks once per query, for as long as the
    // user is typing.
#ifndef CN1_USE_ARC
    [result autorelease];
#endif
    return result;
}

- (NSArray<NSAttributedStringKey> *)validAttributesForMarkedText {
    return @[];
}

- (NSRect)firstRectForCharacterRange:(NSRange)range
                         actualRange:(NSRangePointer)actualRange {
    if (actualRange != NULL) {
        *actualRange = range;
    }
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    CGRect caret = session.caretRect;
    CGFloat scale = CN1AppKitBackingScale(self);
    // The framework reports the caret in device pixels in its own top left
    // space; the view is flipped, so this is a scale into points and then the
    // two conversions AppKit asks for. This must end in SCREEN coordinates --
    // returning window ones puts the candidate window in the right place on a
    // single display and the wrong place on every other one, which is close to
    // impossible to notice on a development machine.
    NSRect inView = NSMakeRect(caret.origin.x / scale, caret.origin.y / scale,
                               caret.size.width / scale, caret.size.height / scale);
    NSRect inWindow = [self convertRect:inView toView:nil];
    return [self.window convertRectToScreen:inWindow];
}

- (NSUInteger)characterIndexForPoint:(NSPoint)point {
    // Used by the dictionary lookup and writing direction menus. Codename One
    // does not expose a character index for a point, and guessing one would
    // make those features point at the wrong word rather than do nothing.
    return NSNotFound;
}

/// The end of the selection the keyboard is moving, given where the anchor is.
///
/// Not "the left edge when going left": that loses which end is fixed, so
/// reversing direction grew the selection from the other side instead of
/// contracting it. From caret 5, Shift-Left gives [4,5); Shift-Right then has to
/// come back to 5, not run on to [4,6).
static NSUInteger cn1ActiveEdge(NSRange sel, NSUInteger anchor) {
    return anchor == sel.location ? NSMaxRange(sel) : sel.location;
}

/// The offset of the first character on the line holding `at`.
- (NSUInteger)cn1LineStart:(NSUInteger)at inText:(NSString *)text {
    if (text == nil || at == 0) {
        return 0;
    }
    NSUInteger limit = MIN(at, text.length);
    NSRange before = NSMakeRange(0, limit);
    NSRange nl = [text rangeOfCharacterFromSet:[NSCharacterSet newlineCharacterSet]
                                       options:NSBackwardsSearch
                                         range:before];
    return nl.location == NSNotFound ? 0 : NSMaxRange(nl);
}

/// The offset just past the last character on the line holding `at`.
- (NSUInteger)cn1LineEnd:(NSUInteger)at inText:(NSString *)text {
    if (text == nil) {
        return 0;
    }
    NSUInteger from = MIN(at, text.length);
    NSRange rest = NSMakeRange(from, text.length - from);
    NSRange nl = [text rangeOfCharacterFromSet:[NSCharacterSet newlineCharacterSet]
                                       options:0
                                         range:rest];
    return nl.location == NSNotFound ? text.length : nl.location;
}

/// Where Up or Down puts the caret: the same column on the neighbouring line,
/// clamped to that line's length.
- (NSUInteger)cn1VerticalTarget:(NSRange)sel up:(BOOL)up inText:(NSString *)text {
    if (text == nil) {
        return 0;
    }
    NSUInteger at = up ? sel.location : NSMaxRange(sel);
    NSUInteger lineStart = [self cn1LineStart:at inText:text];
    NSUInteger column = at - lineStart;
    if (up) {
        if (lineStart == 0) {
            // Already on the first line: Up goes to the very start, which is
            // what a Mac text field does rather than nothing at all.
            return 0;
        }
        NSUInteger prevEnd = lineStart - 1;
        NSUInteger prevStart = [self cn1LineStart:prevEnd inText:text];
        return MIN(prevStart + column, prevEnd);
    }
    NSUInteger lineEnd = [self cn1LineEnd:at inText:text];
    if (lineEnd >= text.length) {
        return text.length;
    }
    NSUInteger nextStart = lineEnd + 1;
    NSUInteger nextEnd = [self cn1LineEnd:nextStart inText:text];
    return MIN(nextStart + column, nextEnd);
}

/// The fixed end of the selection the keyboard is building.
///
/// Remembered across keystrokes, because the range alone cannot say which end is
/// anchored: [4,5) is the same range whether the user shift-lefted from 5 or
/// shift-righted from 4, and the next keystroke has to grow from the other end
/// in each case. Forgotten whenever the selection stops matching what was
/// recorded, which is how an ordinary click or an insertion starts a fresh one.
- (NSUInteger)cn1SelectionAnchor:(NSRange)sel {
    if (selectionAnchorValid
            && (selectionAnchor == sel.location || selectionAnchor == NSMaxRange(sel))) {
        return selectionAnchor;
    }
    // No usable memory: a collapsed caret anchors on itself, and a selection made
    // some other way anchors at its start, which is what AppKit does.
    return sel.location;
}

- (void)cn1RememberAnchor:(NSUInteger)anchor {
    selectionAnchor = anchor;
    selectionAnchorValid = YES;
}

/// The index one composed character before `at`.
///
/// Not `at - 1`: an emoji, a flag or a combining sequence is several UTF-16
/// units, and a caret parked inside one splits a surrogate pair the moment
/// anything is inserted or deleted there -- committing text the framework cannot
/// render. The deletion handlers already move by composed sequence; the caret
/// has to agree with them.
- (NSUInteger)cn1IndexBefore:(NSUInteger)at inText:(NSString *)text {
    if (at == 0 || text == nil || at > text.length) {
        return 0;
    }
    NSRange seq = [text rangeOfComposedCharacterSequenceAtIndex:at - 1];
    return seq.location;
}

/// The index one composed character after `at`.
- (NSUInteger)cn1IndexAfter:(NSUInteger)at inText:(NSString *)text {
    if (text == nil || at >= text.length) {
        return text == nil ? 0 : text.length;
    }
    NSRange seq = [text rangeOfComposedCharacterSequenceAtIndex:at];
    return NSMaxRange(seq);
}

/// Grows or shrinks a selection towards `to`, keeping the far edge anchored.
- (NSRange)cn1Extend:(NSRange)sel to:(NSUInteger)to anchor:(NSUInteger)anchor {
    NSUInteger lo = MIN(anchor, to);
    NSUInteger hi = MAX(anchor, to);
    return NSMakeRange(lo, hi - lo);
}

/// Anything that is not a shift-arrow ends the selection gesture, so the next
/// one starts from wherever the caret ended up rather than from a stale anchor.
- (void)cn1ForgetAnchor {
    selectionAnchorValid = NO;
}

/// Moves the caret or selection and tells the pure editor. A caret move is not
/// an edit, so it never reaches cn1ReplaceRange -- without this the editor's own
/// caret stayed where it was while the shadow moved, and the next insertion
/// landed at the wrong offset.
- (void)cn1SetSelection:(NSRange)sel {
    [CN1MacTextInputSession sharedSession].selectedRange = sel;
    CN1MacTextInputNotifySelection(sel);
}

- (void)doCommandBySelector:(SEL)selector {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (!session.active) {
        [super doCommandBySelector:selector];
        return;
    }
    NSRange sel = session.selectedRange;
    NSUInteger len = session.text.length;

    if (selector == @selector(insertNewline:)) {
        if (session.multiline) {
            [self insertText:@"\n" replacementRange:NSMakeRange(NSNotFound, 0)];
        } else {
            [session commitFinished:YES];
        }
        return;
    }
    if (selector == @selector(insertTab:) || selector == @selector(insertBacktab:)) {
        // Tab moves focus rather than inserting, which is the AppKit
        // convention and matches what the framework does with a done event.
        [session commitFinished:YES];
        return;
    }
    if (selector == @selector(cancelOperation:)) {
        [session commitFinished:YES];
        return;
    }
    if (selector == @selector(deleteBackward:)) {
        if (sel.length > 0) {
            [self cn1ReplaceRange:sel withString:@""];
            [self cn1SetSelection:NSMakeRange(sel.location, 0)];
        } else if (sel.location > 0) {
            // Steps by composed character so deleting an emoji or an accented
            // letter removes the whole thing rather than half of it.
            NSRange back = [session.text rangeOfComposedCharacterSequenceAtIndex:sel.location - 1];
            [self cn1ReplaceRange:back withString:@""];
            [self cn1SetSelection:NSMakeRange(back.location, 0)];
        }
        [session commitFinished:NO];
        return;
    }
    if (selector == @selector(deleteForward:)) {
        if (sel.length > 0) {
            [self cn1ReplaceRange:sel withString:@""];
            [self cn1SetSelection:NSMakeRange(sel.location, 0)];
        } else if (sel.location < len) {
            NSRange fwd = [session.text rangeOfComposedCharacterSequenceAtIndex:sel.location];
            [self cn1ReplaceRange:fwd withString:@""];
            [self cn1SetSelection:NSMakeRange(fwd.location, 0)];
        }
        [session commitFinished:NO];
        return;
    }
    // Forgotten INSIDE the plain-movement branches, not before them. These two
    // resets ran unconditionally, so by the time a ...AndModifySelection:
    // selector was matched further down the anchor it depends on had already
    // been discarded: a second Shift-Left re-derived the anchor from the current
    // selection and collapsed it instead of extending, so holding Shift and
    // pressing an arrow twice from offset 5 gave [4,5) and then 4 rather than
    // [3,5). A plain arrow genuinely ends the selection gesture and still
    // forgets; an extending one must not.
    if (selector == @selector(moveLeft:)) {
        NSUInteger at = sel.length > 0 ? sel.location
            : [self cn1IndexBefore:sel.location inText:session.text];
        [self cn1SetSelection:NSMakeRange(at, 0)];
        [self cn1ForgetAnchor];
        return;
    }
    if (selector == @selector(moveRight:)) {
        NSUInteger at = sel.length > 0 ? NSMaxRange(sel)
            : [self cn1IndexAfter:sel.location inText:session.text];
        [self cn1SetSelection:NSMakeRange(at, 0)];
        [self cn1ForgetAnchor];
        return;
    }
    // Shift-arrow and its line/document variants. AppKit turns them into these
    // ...AndModifySelection: selectors, and the fallback below used to swallow
    // every one -- so a keyboard user could not select text at all, in any
    // TextField or TextArea on the port.
    if (selector == @selector(moveLeftAndModifySelection:)
        || selector == @selector(moveBackwardAndModifySelection:)) {
        NSUInteger anchor = [self cn1SelectionAnchor:sel];
        NSUInteger to = [self cn1IndexBefore:cn1ActiveEdge(sel, anchor) inText:session.text];
        [self cn1SetSelection:[self cn1Extend:sel to:to anchor:anchor]];
        [self cn1RememberAnchor:anchor];
        return;
    }
    if (selector == @selector(moveRightAndModifySelection:)
        || selector == @selector(moveForwardAndModifySelection:)) {
        NSUInteger anchor = [self cn1SelectionAnchor:sel];
        NSUInteger to = [self cn1IndexAfter:cn1ActiveEdge(sel, anchor) inText:session.text];
        [self cn1SetSelection:[self cn1Extend:sel to:to anchor:anchor]];
        [self cn1RememberAnchor:anchor];
        return;
    }
    // Split from the document selectors below for the same reason the
    // unmodified line commands further down are: grouped together,
    // Shift-Home on the third line of a TextArea selected back through every
    // preceding line instead of stopping where the line starts. The bug was
    // fixed for the plain movement and left standing for the selecting
    // variant, which is the same key with Shift held.
    if (selector == @selector(moveToBeginningOfLineAndModifySelection:)
        || selector == @selector(moveToBeginningOfParagraphAndModifySelection:)) {
        NSUInteger anchor = [self cn1SelectionAnchor:sel];
        NSUInteger to = [self cn1LineStart:cn1ActiveEdge(sel, anchor) inText:session.text];
        [self cn1SetSelection:[self cn1Extend:sel to:to anchor:anchor]];
        [self cn1RememberAnchor:anchor];
        return;
    }
    if (selector == @selector(moveToEndOfLineAndModifySelection:)
        || selector == @selector(moveToEndOfParagraphAndModifySelection:)) {
        NSUInteger anchor = [self cn1SelectionAnchor:sel];
        NSUInteger to = [self cn1LineEnd:cn1ActiveEdge(sel, anchor) inText:session.text];
        [self cn1SetSelection:[self cn1Extend:sel to:to anchor:anchor]];
        [self cn1RememberAnchor:anchor];
        return;
    }
    if (selector == @selector(moveToBeginningOfDocumentAndModifySelection:)) {
        NSUInteger anchor = [self cn1SelectionAnchor:sel];
        [self cn1SetSelection:[self cn1Extend:sel to:0 anchor:anchor]];
        [self cn1RememberAnchor:anchor];
        return;
    }
    if (selector == @selector(moveToEndOfDocumentAndModifySelection:)) {
        NSUInteger anchor = [self cn1SelectionAnchor:sel];
        [self cn1SetSelection:[self cn1Extend:sel to:len anchor:anchor]];
        [self cn1RememberAnchor:anchor];
        return;
    }
    // Line and paragraph commands stay on the line. They used to be grouped with
    // the document ones, so Home on the third line of a TextArea jumped to
    // offset 0 and End jumped to the end of the whole field -- which is a
    // different key's behaviour, not a rough approximation of this one.
    if (selector == @selector(moveToBeginningOfLine:)
        || selector == @selector(moveToBeginningOfParagraph:)) {
        [self cn1SetSelection:NSMakeRange([self cn1LineStart:sel.location
                                                     inText:session.text], 0)];
        return;
    }
    if (selector == @selector(moveToEndOfLine:)
        || selector == @selector(moveToEndOfParagraph:)) {
        [self cn1SetSelection:NSMakeRange([self cn1LineEnd:NSMaxRange(sel)
                                                   inText:session.text], 0)];
        return;
    }
    if (selector == @selector(moveToBeginningOfDocument:)) {
        [self cn1SetSelection:NSMakeRange(0, 0)];
        return;
    }
    if (selector == @selector(moveToEndOfDocument:)) {
        [self cn1SetSelection:NSMakeRange(len, 0)];
        return;
    }
    // Up and Down. Swallowed entirely before this, so the caret could not move
    // between lines of a TextArea at all -- the one thing a multiline editor is
    // for. The column is taken from the current line and reapplied to the
    // neighbouring one, clamped to its length, which is what every Mac text
    // control does.
    if (selector == @selector(moveUp:) || selector == @selector(moveDown:)) {
        BOOL up = selector == @selector(moveUp:);
        [self cn1SetSelection:NSMakeRange([self cn1VerticalTarget:sel up:up
                                                           inText:session.text], 0)];
        return;
    }
    if (selector == @selector(moveUpAndModifySelection:)
        || selector == @selector(moveDownAndModifySelection:)) {
        BOOL up = selector == @selector(moveUpAndModifySelection:);
        NSUInteger anchor = [self cn1SelectionAnchor:sel];
        NSRange active = NSMakeRange(cn1ActiveEdge(sel, anchor), 0);
        NSUInteger to = [self cn1VerticalTarget:active up:up inText:session.text];
        session.selectedRange = [self cn1Extend:sel to:to anchor:anchor];
        CN1MacTextInputNotifySelection(session.selectedRange);
        [self cn1RememberAnchor:anchor];
        return;
    }
    if (selector == @selector(selectAll:)) {
        [self cn1SetSelection:NSMakeRange(0, len)];
        return;
    }
    if (selector == @selector(deleteToEndOfLine:) || selector == @selector(deleteToEndOfParagraph:)) {
        // To the end of THIS line, not of the text. Control-K in a multiline
        // editor deleted every remaining line, because the range ran to len.
        NSRange target;
        if (sel.length > 0) {
            // A selection is what Control-K acts on when there is one, the same
            // as every other delete command here.
            target = sel;
        } else {
            NSUInteger lineEnd = [self cn1LineEnd:sel.location inText:session.text];
            if (lineEnd <= sel.location) {
                // Already at the end of the line. AppKit's own behaviour is to
                // take the newline itself, joining this line with the next.
                lineEnd = MIN(sel.location + 1, len);
            }
            target = NSMakeRange(sel.location, lineEnd - sel.location);
        }
        if (target.length == 0) {
            return;
        }
        [self cn1ReplaceRange:target withString:@""];
        [self cn1SetSelection:NSMakeRange(target.location, 0)];
        [session commitFinished:NO];
        return;
    }
    // Anything left is a binding with no meaning for a single field editor.
    // Swallowed rather than passed up, because NSResponder answers an unhandled
    // text command with a beep.
}


- (void)keyUp:(NSEvent *)event {
    if ([[self cn1ConsumedKeys] containsIndex:event.keyCode]) {
        [[self cn1ConsumedKeys] removeIndex:event.keyCode];
        return;
    }
    if (!self.cn1InputEnabled) {
        return;
    }
    int code = CN1MacKeyCode(event);
    if (code == 0) {
        return;
    }
    if (self.cn1WindowId >= 0) {
        CN1MacWindowDeliverKey(self.cn1WindowId, code, NO);
        return;
    }
    keyReleasedNative(code);
}

// ---- Edit menu -----------------------------------------------------------
//
// The menu bar's Edit items have nil targets, which is how AppKit is meant to
// work: it walks the responder chain and whoever is first responder answers.
// This view IS the first responder while a text field is being edited, and it
// answered none of them -- so the whole Edit menu and its Command-X/C/V stayed
// greyed out during text entry, on a port whose point is being a real Mac app.
//
// The edits go through the same cn1EffectiveRange/cn1ReplaceRange/commitFinished
// path insertText: uses, so the framework sees a paste exactly as it sees typing.

/// The selection, or an empty range when there is no session.
- (NSRange)cn1SelectionForEditing {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (!session.active) {
        return NSMakeRange(NSNotFound, 0);
    }
    NSRange sel = session.selectedRange;
    if (sel.location == NSNotFound || NSMaxRange(sel) > session.text.length) {
        return NSMakeRange(NSNotFound, 0);
    }
    return sel;
}

- (BOOL)validateUserInterfaceItem:(id<NSValidatedUserInterfaceItem>)item {
    SEL action = [item action];
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (action == @selector(undo:) || action == @selector(redo:)) {
        // Disabled honestly rather than shown enabled and doing nothing: the
        // input session keeps no undo stack, and the pure Codename One editor
        // (EditField/CodeEditor) owns its own undo rather than routing it here.
        return NO;
    }
    if (action == @selector(paste:)) {
        return session.active
            && [[NSPasteboard generalPasteboard] canReadObjectForClasses:@[[NSString class]]
                                                                 options:nil];
    }
    if (action == @selector(selectAll:)) {
        return session.active && session.text.length > 0;
    }
    if (action == @selector(cut:) || action == @selector(delete:)) {
        // Editing commands, so they need an editing session. A lightweight
        // selection is a selectable Label -- there is nothing to cut out of it.
        return [self cn1SelectionForEditing].length > 0;
    }
    if (action == @selector(copy:)) {
        if (session.active) {
            return [self cn1SelectionForEditing].length > 0;
        }
        // With no session the form's own TextSelection may hold one -- a
        // selectable Label or SpanLabel, which the desktop selects by pressing
        // rather than through the portable floating Copy menu. Enabled without
        // asking whether it is empty: the answer lives in the component
        // hierarchy, which belongs to the event dispatch thread, and this is
        // AppKit's main thread asking synchronously while the menu opens. A Copy
        // that turns out to have nothing to copy does nothing, which is a great
        // deal better than a Copy that is permanently grey over selected text.
        return YES;
    }
    return [super validateUserInterfaceItem:item];
}

- (void)copy:(id)sender {
    if (![CN1MacTextInputSession sharedSession].active) {
        // The form's own selection, copied on the event dispatch thread where
        // the hierarchy can be walked safely.
        CN1MacCopyTextSelection();
        return;
    }
    NSRange sel = [self cn1SelectionForEditing];
    if (sel.length == 0) {
        return;
    }
    NSPasteboard *pb = [NSPasteboard generalPasteboard];
    [pb clearContents];
    [pb writeObjects:@[[[CN1MacTextInputSession sharedSession].text
                            substringWithRange:sel]]];
}

- (void)cut:(id)sender {
    NSRange sel = [self cn1SelectionForEditing];
    if (sel.length == 0) {
        return;
    }
    [self copy:sender];
    [self delete:sender];
}

- (void)delete:(id)sender {
    NSRange sel = [self cn1SelectionForEditing];
    if (sel.length == 0) {
        return;
    }
    // Only -1 is a refusal. cn1ReplaceRange: answers the number of characters
    // it ACCEPTED, and a deletion accepts none -- so testing it as a boolean
    // took the failure path on every successful delete, after the shadow text
    // had already been mutated: the legacy editor never got commitFinished:
    // and the pure editor kept a selection over characters that no longer
    // exist, which then corrupts the next edit.
    if ([self cn1ReplaceRange:sel withString:@""] < 0) {
        return;
    }
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    session.markedRange = NSMakeRange(NSNotFound, 0);
    session.selectedRange = NSMakeRange(sel.location, 0);
    [session commitFinished:NO];
}

- (void)paste:(id)sender {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (!session.active) {
        return;
    }
    NSString *pasted = [[NSPasteboard generalPasteboard]
                            stringForType:NSPasteboardTypeString];
    if (pasted == nil) {
        return;
    }
    // Through insertText:, so a multi-line paste into a single-line field is
    // flattened the same way a typed newline is rather than being truncated.
    [self insertText:pasted replacementRange:NSMakeRange(NSNotFound, 0)];
}

- (void)selectAll:(id)sender {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (!session.active) {
        return;
    }
    session.markedRange = NSMakeRange(NSNotFound, 0);
    // Through the helper, so the pure editor hears about it. Command-A and the
    // Edit menu land here directly rather than through doCommandBySelector, so
    // assigning the shadow alone left the Java editor with no selection to
    // render -- and a following Cut or Copy then acted on nothing.
    [self cn1SetSelection:NSMakeRange(0, session.text.length)];
}

/// An I-beam over the view. Its absence is the loudest "this is not really a Mac
/// app" tell after the menu bar.
- (void)resetCursorRects {
    [super resetCursorRects];
    [self addCursorRect:self.bounds cursor:[NSCursor arrowCursor]];
}

// ---- protocol members with no AppKit meaning ----------------------------

// AppKit has no software keyboard and no UITextField delegate. These exist to
// satisfy CN1RenderingView, which the UIKit ports need them for.
- (void)keyboardDoneClicked {
}

- (void)keyboardNextClicked {
}

- (void)textFieldDidChange {
}

/// Whether the renderer draws straight to the drawable instead of accumulating
/// into a retained target.
///
/// Always no here. The UIKit backend offers it behind CN1_DIRECT_DRAWABLE, and
/// it only works because that backend pairs it with repainting the whole Form
/// every frame; this driver accumulates into a persistent screen texture, which
/// is what makes a partial flush cheap. IOSNative asks because the Java paint
/// model has to follow the renderer's choice rather than decide it, so the
/// answer has to come from the renderer that is actually running.
int cn1DirectToDrawableEnabled(void) {
    return 0;
}

// ---- materials ----------------------------------------------------------

/// The shared drawing pipeline's Codename One pixels to AppKit points factor.
/// Set by CN1MacHost when the window is built.
extern float scaleValue;


// ---------------------------------------------------------------------------
// Live-screen "Liquid Glass" material helpers. Faithful C ports of the proven
// offscreen recipe in IOSImplementation (glassMaterialInPlace, sampleBilinear,
// applyGlassOptics) so the running app produces the SAME glass as the fidelity
// tiles. The whole pipeline runs in one top-down ARGB integer buffer (no
// CIImage/CG round-trip) to avoid orientation ambiguity; glassScreenRegionX
// below ties them together against the live screenTexture.
// ---------------------------------------------------------------------------

// One separable box-blur iteration (horizontal then vertical) of the given
// radius via a sliding running-sum: O(w*h) REGARDLESS of radius, edge-clamped.
static void glassBoxBlurOnce(uint32_t *buf, uint32_t *tmp, int w, int h, int r) {
    if (r < 1) { return; }
    float norm = 1.0f / (float)(2 * r + 1);
    for (int y = 0; y < h; y++) {
        uint32_t *row = buf + (size_t)y * w;
        uint32_t *trow = tmp + (size_t)y * w;
        int sr = 0, sg = 0, sb = 0;
        for (int k = -r; k <= r; k++) {
            int xx = k < 0 ? 0 : (k >= w ? w - 1 : k);
            uint32_t p = row[xx]; sr += (p >> 16) & 0xff; sg += (p >> 8) & 0xff; sb += p & 0xff;
        }
        for (int x = 0; x < w; x++) {
            trow[x] = 0xff000000u | ((uint32_t)(int)(sr * norm + 0.5f) << 16) | ((uint32_t)(int)(sg * norm + 0.5f) << 8) | (uint32_t)(int)(sb * norm + 0.5f);
            int xo = x - r; if (xo < 0) xo = 0;
            int xi = x + r + 1; if (xi >= w) xi = w - 1;
            uint32_t po = row[xo], pi = row[xi];
            sr += (int)((pi >> 16) & 0xff) - (int)((po >> 16) & 0xff);
            sg += (int)((pi >> 8) & 0xff) - (int)((po >> 8) & 0xff);
            sb += (int)(pi & 0xff) - (int)(po & 0xff);
        }
    }
    for (int x = 0; x < w; x++) {
        int sr = 0, sg = 0, sb = 0;
        for (int k = -r; k <= r; k++) {
            int yy = k < 0 ? 0 : (k >= h ? h - 1 : k);
            uint32_t p = tmp[(size_t)yy * w + x]; sr += (p >> 16) & 0xff; sg += (p >> 8) & 0xff; sb += p & 0xff;
        }
        for (int y = 0; y < h; y++) {
            buf[(size_t)y * w + x] = 0xff000000u | ((uint32_t)(int)(sr * norm + 0.5f) << 16) | ((uint32_t)(int)(sg * norm + 0.5f) << 8) | (uint32_t)(int)(sb * norm + 0.5f);
            int yo = y - r; if (yo < 0) yo = 0;
            int yi = y + r + 1; if (yi >= h) yi = h - 1;
            uint32_t po = tmp[(size_t)yo * w + x], pi = tmp[(size_t)yi * w + x];
            sr += (int)((pi >> 16) & 0xff) - (int)((po >> 16) & 0xff);
            sg += (int)((pi >> 8) & 0xff) - (int)((po >> 8) & 0xff);
            sb += (int)(pi & 0xff) - (int)(po & 0xff);
        }
    }
}

// The two raster helpers the glass material is built from. Copied rather than
// shared because they are file-static in the UIKit backend's METALView.m, which
// this port excludes -- and they are plain integer arithmetic over a pixel
// buffer, with nothing platform-specific to diverge.


// Triple box blur ~ Gaussian of sigma ~= radius (Jarosz). RADIUS-INDEPENDENT cost
// so the large (radius ~64px) nav/tab bar glass blurs stay cheap -- a true
// Gaussian kernel here was hundreds of ms per call and timed the suite out.
// Edge-clamped, in place. Alpha assumed opaque (backdrop) and kept at 0xff.
static void glassGaussianBlur(uint32_t *buf, int w, int h, float radius) {
    if (radius < 0.75f || w <= 0 || h <= 0) { return; }
    int r = (int)(radius + 0.5f);
    if (r < 1) { r = 1; }
    uint32_t *tmp = (uint32_t *)malloc((size_t)w * (size_t)h * 4);
    if (tmp == NULL) { return; }
    glassBoxBlurOnce(buf, tmp, w, h, r);
    glassBoxBlurOnce(buf, tmp, w, h, r);
    glassBoxBlurOnce(buf, tmp, w, h, r);
    free(tmp);
}

static inline int glassBilerp(int c00, int c10, int c01, int c11, float tx, float ty) {
    float top = c00 + (c10 - c00) * tx;
    float bot = c01 + (c11 - c01) * tx;
    return (int)(top + (bot - top) * ty + 0.5f);
}

static uint32_t glassSampleBilinear(uint32_t *buf, int w, int h, float fx, float fy) {
    if (fx < 0.0f) fx = 0.0f; else if (fx > w - 1) fx = w - 1;
    if (fy < 0.0f) fy = 0.0f; else if (fy > h - 1) fy = h - 1;
    int x0 = (int)fx, y0 = (int)fy;
    int x1 = x0 + 1 < w ? x0 + 1 : x0, y1 = y0 + 1 < h ? y0 + 1 : y0;
    float tx = fx - x0, ty = fy - y0;
    uint32_t p00 = buf[(size_t)y0 * w + x0], p10 = buf[(size_t)y0 * w + x1];
    uint32_t p01 = buf[(size_t)y1 * w + x0], p11 = buf[(size_t)y1 * w + x1];
    int r = glassBilerp((p00 >> 16) & 0xff, (p10 >> 16) & 0xff, (p01 >> 16) & 0xff, (p11 >> 16) & 0xff, tx, ty);
    int g = glassBilerp((p00 >> 8) & 0xff, (p10 >> 8) & 0xff, (p01 >> 8) & 0xff, (p11 >> 8) & 0xff, tx, ty);
    int b = glassBilerp(p00 & 0xff, p10 & 0xff, p01 & 0xff, p11 & 0xff, tx, ty);
    return ((uint32_t)r << 16) | ((uint32_t)g << 8) | (uint32_t)b;
}

// Rounded-rect SDF mask + edge refraction + specular rim. Reads the blurred
// padded buffer src (component at offset (pad,pad)), writes a PREMULTIPLIED
// ARGB patch (rw x rh) with transparent corners. s = contentScaleFactor (logical
// lengths -- cornerRadius, rim width -- scale to physical px). cornerRadius < 0
// means capsule.
static void glassApplyOptics(uint32_t *src, int bw, int bh, int pad, uint32_t *out,
        int rw, int rh, float cornerRadius, float refract, float specular, float s) {
    float hw = rw / 2.0f, hh = rh / 2.0f;
    float minhh = hw < hh ? hw : hh;
    float r;
    if (cornerRadius < 0.0f) { r = minhh; }
    else { r = cornerRadius * s; if (r > minhh) r = minhh; }
    if (r < 0.0f) r = 0.0f;
    float band = minhh * 0.6f;
    float rimW = 3.0f * s;
    for (int y = 0; y < rh; y++) {
        float py = y + 0.5f;
        for (int x = 0; x < rw; x++) {
            float px = x + 0.5f;
            float dx = fabsf(px - hw) - (hw - r);
            float dy = fabsf(py - hh) - (hh - r);
            float axx = dx > 0 ? dx : 0, ayy = dy > 0 ? dy : 0;
            float outside = sqrtf(axx * axx + ayy * ayy);
            float mxv = dx > dy ? dx : dy;
            float inside = mxv < 0 ? mxv : 0;
            float sdf = outside + inside - r;
            float depth = -sdf;
            if (depth <= 0.0f) { out[(size_t)y * rw + x] = 0; continue; }
            float alpha = depth >= 1.0f ? 1.0f : depth;
            // Bottom-edge feather for rectangular chrome bars (Toolbar/TitleArea,
            // cornerRadius == 0): a native nav bar's glass fades into the content
            // below instead of stopping at a hard rectangular edge. Ramp the glass
            // alpha down over the bottom ~22% so the blurred bar blends into the
            // sharp backdrop beneath it. Capsules (-1) and rounded panels (>0) keep
            // their crisp shape (unaffected).
            if (cornerRadius == 0.0f) {
                float fb = rh * 0.22f;
                if (fb > 1.0f && py > rh - fb) {
                    float fade = (rh - py) / fb;
                    if (fade < 0.0f) fade = 0.0f;
                    alpha *= fade;
                }
            }
            float sx = x, sy = y;
            if (refract > 0.0f && band > 0.0f && depth < band) {
                float t = 1.0f - depth / band;
                float distortion = 1.0f - sqrtf(fmaxf(0.0f, 1.0f - t * t));
                sx = x - (px - hw) * distortion * refract;
                sy = y - (py - hh) * distortion * refract;
            }
            uint32_t col = glassSampleBilinear(src, bw, bh, sx + pad, sy + pad);
            int rr = (col >> 16) & 0xff, gg = (col >> 8) & 0xff, bb = col & 0xff;
            if (specular > 0.0f && depth < rimW) {
                float rim = 1.0f - depth / rimW;
                float topBias = 0.55f + 0.45f * (1.0f - py / rh);
                int add = (int)(specular * rim * topBias * 70.0f);
                rr = rr + add > 255 ? 255 : rr + add;
                gg = gg + add > 255 ? 255 : gg + add;
                bb = bb + add > 255 ? 255 : bb + add;
            }
            int a = (int)(alpha * 255.0f);
            int pr = rr * a / 255, pg = gg * a / 255, pb = bb * a / 255;
            out[(size_t)y * rw + x] = ((uint32_t)a << 24) | ((uint32_t)pr << 16) | ((uint32_t)pg << 8) | (uint32_t)pb;
        }
    }
}

//
// The blur, glass and lens backdrop materials, shared with the UIKit backend
// almost line for line: the recipe is Metal for the capture and CoreImage for
// the filter, and both are the same framework on either platform. The two
// differences are the backing scale, which an NSView reports under a different
// name, and building an image from a CGImage, which NSImage does through an
// initializer rather than a class method.

/// Releases a per-frame scratch texture on an early-exit path.
///
/// newTextureWithDescriptor: follows the "new" convention and hands back a +1
/// texture, and this port is compiled without ARC, so a scratch that is not
/// released is a GPU allocation the size of the blurred region leaked once per
/// repaint -- an animated blur walks through memory quickly. The three
/// long-lived textures in this file (the screen, the stencil and the readback
/// staging texture) were already released; these per-frame ones were not.
///
/// Written as a macro because every one of these methods leaves through several
/// inline `if (...) { ...; return; }` guards, and spelling the #ifndef out at
/// each of them would bury the guard it is attached to.
#ifdef CN1_USE_ARC
#define CN1_SCRATCH_RELEASE
#else
#define CN1_SCRATCH_RELEASE [scratch release];
#endif

- (void)blurScreenRegionX:(int)x y:(int)y w:(int)w h:(int)h radius:(float)radius {
    if (self.screenTexture == nil || w <= 0 || h <= 0 || radius <= 0.0f) {
        return;
    }
    CGFloat s = CN1AppKitBackingScale(self);
    int texW = (int)self.screenTexture.width, texH = (int)self.screenTexture.height;
    int fx = (int)(x * s), fy = (int)(y * s), fw = (int)(w * s), fh = (int)(h * s);
    if (fx < 0) { fw += fx; fx = 0; }
    if (fy < 0) { fh += fy; fy = 0; }
    if (fx + fw > texW) { fw = texW - fx; }
    if (fy + fh > texH) { fh = texH - fy; }
    if (fw <= 0 || fh <= 0) { return; }

    // 1) End + commit the screen encoder so screenTexture holds the backdrop
    //    drawn so far this frame, then wait so the blit-read sees it.
    if (self.renderCommandEncoder != nil) {
        CN1MetalEndFrame();
        [self.renderCommandEncoder endEncoding];
        self.renderCommandEncoder = nil;
    }
    id<MTLCommandBuffer> cb = self.commandBuffer;
    self.commandBuffer = nil;
    if (cb != nil) {
        [cb commit];
        [cb waitUntilCompleted];
    }

    // 2) Blit the region into a shared scratch texture and read its bytes.
    id<MTLDevice> device = CN1MetalDevice();
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm width:fw height:fh mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModeShared;
    id<MTLTexture> scratch = [device newTextureWithDescriptor:desc];
    id<MTLCommandBuffer> blitCb = [self.commandQueue commandBuffer];
    id<MTLBlitCommandEncoder> blit = [blitCb blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture sourceSlice:0 sourceLevel:0
              sourceOrigin:MTLOriginMake(fx, fy, 0) sourceSize:MTLSizeMake(fw, fh, 1)
                 toTexture:scratch destinationSlice:0 destinationLevel:0
         destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [blitCb commit];
    [blitCb waitUntilCompleted];

    NSUInteger rowBytes = (NSUInteger)fw * 4;
    uint8_t *bytes = (uint8_t *)malloc(rowBytes * (NSUInteger)fh);
    if (bytes == NULL) { [self setFramebuffer]; CN1_SCRATCH_RELEASE return; }
    [scratch getBytes:bytes bytesPerRow:rowBytes fromRegion:MTLRegionMake2D(0, 0, fw, fh) mipmapLevel:0];

    // 3) CIGaussianBlur + saturation (the UIBlurEffect-style material recipe).
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    CGContextRef bmp = CGBitmapContextCreate(bytes, fw, fh, 8, rowBytes, cs,
        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little);
    CGImageRef srcCg = CGBitmapContextCreateImage(bmp);
    CIImage *ci = [CIImage imageWithCGImage:srcCg];
    CIFilter *sat = [CIFilter filterWithName:@"CIColorControls"];
    [sat setValue:ci forKey:kCIInputImageKey];
    [sat setValue:@(1.8) forKey:@"inputSaturation"];
    CIFilter *gb = [CIFilter filterWithName:@"CIGaussianBlur"];
    [gb setValue:[sat outputImage] forKey:kCIInputImageKey];
    [gb setValue:@(radius * s) forKey:kCIInputRadiusKey];
    CIImage *clamped = [[gb outputImage] imageByClampingToExtent];
    // Retain the cached context: under MRC the autoreleased CIContext would
    // dangle and crash on the next glass paint (a static Foundation cache must
    // be +1 retained). The retain is a harmless no-op under ARC.
    static CIContext *ciCtx = nil;
    if (ciCtx == nil) {
        ciCtx = [CIContext contextWithMTLDevice:device];
#ifndef CN1_USE_ARC
        [ciCtx retain];
#endif
    }
    CGImageRef outCg = [ciCtx createCGImage:clamped fromRect:CGRectMake(0, 0, fw, fh)];
    CN1Image *blurredImage = outCg ? CN1AppKitNSImageFromCGImage(outCg) : nil;
    if (srcCg) { CGImageRelease(srcCg); }
    if (outCg) { CGImageRelease(outCg); }
    CGContextRelease(bmp);
    CGColorSpaceRelease(cs);
    free(bytes);

    // 4) Restart the screen encoder (loadAction Load preserves screenTexture).
    [self setFramebuffer];

    // 5) Draw the blurred patch back over the region (display coords).
    if (blurredImage != nil) {
        id<MTLTexture> blurredTex = CN1MetalTextureFromUIImage(blurredImage);
        if (blurredTex != nil) {
            CN1MetalDrawImage(blurredTex, 255, x, y, w, h);
        }
    }
#ifndef CN1_USE_ARC
    [scratch release];
#endif
}

// Live-screen "Liquid Glass" MATERIAL: the full backdrop-filter recipe matching
// the offscreen IOSImplementation.glassRegion that drives the fidelity tiles.
// 1) read a screenTexture region PADDED by 3*radius (edge-replicated so the blur
//    never fades into the component edge), 2) apply the affine colour material,
// 3) Gaussian-blur, 4) apply optics (rounded-rect SDF mask + edge refraction +
// specular rim), 5) draw the pill-shaped translucent glass patch back over the
// backdrop so the component's fill + foreground (queued next) paint on top. Runs
// ---- live-glass patch cache ------------------------------------------------
// Caching/invalidation policy for the live-screen glass materials (review):
//   * A glass surface only pays at all when it REPAINTS; a static chrome bar
//     over static content costs nothing between repaints.
//   * When it does repaint, the backdrop readback (commit + waitUntilCompleted
//     + blit + getBytes) is unavoidable for correctness -- the material is a
//     function of the pixels behind the glass. What CAN be skipped is the
//     expensive composition: the per-pixel colour transform, the Gaussian
//     blur and the edge optics.
//   * So the composed patch is cached per glass rect: while the rect, the
//     material parameters AND a hash of the backdrop bytes are unchanged
//     (i.e. "backdrop and bounds are stable"), the cached patch is redrawn
//     directly. When the backdrop changes -- scrolling content under the bar,
//     an animation behind the glass -- the hash misses and the patch is
//     recomposed that frame; there is no stale-glass failure mode because the
//     decision is taken from the actual backdrop bytes, not from heuristics.
//   * The travelling selection LENS never takes this path: it is a pure GPU
//     fragment shader on the frame's own command buffer (lensScreenRegionX),
//     with no sync and no readback, so it needs no cache.
// Define CN1_GLASS_PROFILE to NSLog per-paint timing + cache hit/miss so the
// frame-cost evidence is reproducible on any device/simulator build.
#define CN1_GLASS_PATCH_CACHE_SLOTS 8
typedef struct {
    int valid;
    int fx, fy, fw, fh;
    float rad, cornerRadius, sat, scale, offset, refract, specular;
    uint64_t backdropHash;
    uint32_t *patch;       // composed premultiplied glass patch (fw*fh), malloc'd
} CN1GlassPatchCacheEntry;
static CN1GlassPatchCacheEntry cn1GlassPatchCache[CN1_GLASS_PATCH_CACHE_SLOTS];
static int cn1GlassPatchCacheNext = 0;

// FNV-1a over the backdrop words -- a fraction of the cost of the blur pass it
// can save, and any real backdrop change flips it.
static uint64_t cn1GlassBackdropHash(const uint8_t *bytes, size_t len) {
    const uint32_t *words = (const uint32_t *)bytes;
    size_t n = len / 4;
    uint64_t hsh = 1469598103934665603ULL;
    for (size_t i = 0; i < n; i++) {
        hsh ^= words[i];
        hsh *= 1099511628211ULL;
    }
    return hsh;
}

// during the drain like blurScreenRegionX; one GPU sync per glass paint.
- (void)glassScreenRegionX:(int)x y:(int)y w:(int)w h:(int)h radius:(float)radius
              cornerRadius:(float)cornerRadius sat:(float)sat scale:(float)scale
                    offset:(float)offset refract:(float)refract specular:(float)specular {
    if (self.screenTexture == nil || w <= 0 || h <= 0 || radius <= 0.0f) {
        return;
    }
    // CN1-logical -> framebuffer-pixel scale. NOT contentScaleFactor alone:
    // scaleValue maps UIKit-points -> CN1-logical (1 in a normal app, but e.g. 3
    // in the fidelity app which runs logical==physical pixel coords). The real
    // logical->pixel ratio is contentScaleFactor/scaleValue (= 3/3 = 1 there,
    // 3/1 = 3 in a normal retina app). Using raw contentScaleFactor triple-scaled
    // the region in the fidelity app (wrong screenTexture slice + 3x radius).
    float sv = scaleValue > 0.0f ? scaleValue : 1.0f;
    CGFloat s = CN1AppKitBackingScale(self) / sv;
    int texW = (int)self.screenTexture.width, texH = (int)self.screenTexture.height;
    int fx = (int)(x * s), fy = (int)(y * s), fw = (int)(w * s), fh = (int)(h * s);
    if (fx < 0) { fw += fx; fx = 0; }
    if (fy < 0) { fh += fy; fy = 0; }
    if (fx + fw > texW) { fw = texW - fx; }
    if (fy + fh > texH) { fh = texH - fy; }
    if (fw <= 0 || fh <= 0) { return; }
    float rad = radius * (float)s;
    int pad = (int)ceilf(rad) * 3 + 1;
    int bw = fw + 2 * pad, bh = fh + 2 * pad;

    // 1) End + commit the screen encoder so screenTexture holds the backdrop.
    if (self.renderCommandEncoder != nil) {
        CN1MetalEndFrame();
        [self.renderCommandEncoder endEncoding];
        self.renderCommandEncoder = nil;
    }
    id<MTLCommandBuffer> cb = self.commandBuffer;
    self.commandBuffer = nil;
    if (cb != nil) { [cb commit]; [cb waitUntilCompleted]; }

    // 2) Blit the clamped padded region and read its bytes.
    int ax0 = fx - pad; if (ax0 < 0) ax0 = 0;
    int ay0 = fy - pad; if (ay0 < 0) ay0 = 0;
    int ax1 = fx + fw + pad; if (ax1 > texW) ax1 = texW;
    int ay1 = fy + fh + pad; if (ay1 > texH) ay1 = texH;
    int aw = ax1 - ax0, ah = ay1 - ay0;
    if (aw <= 0 || ah <= 0) { [self setFramebuffer]; return; }
    id<MTLDevice> device = CN1MetalDevice();
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm width:aw height:ah mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModeShared;
    id<MTLTexture> scratch = [device newTextureWithDescriptor:desc];
    id<MTLCommandBuffer> blitCb = [self.commandQueue commandBuffer];
    id<MTLBlitCommandEncoder> blit = [blitCb blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture sourceSlice:0 sourceLevel:0
              sourceOrigin:MTLOriginMake(ax0, ay0, 0) sourceSize:MTLSizeMake(aw, ah, 1)
                 toTexture:scratch destinationSlice:0 destinationLevel:0
         destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [blitCb commit];
    [blitCb waitUntilCompleted];
    NSUInteger availRow = (NSUInteger)aw * 4;
    uint8_t *avail = (uint8_t *)malloc(availRow * (NSUInteger)ah);
    if (avail == NULL) { [self setFramebuffer]; CN1_SCRATCH_RELEASE return; }
    [scratch getBytes:avail bytesPerRow:availRow fromRegion:MTLRegionMake2D(0, 0, aw, ah) mipmapLevel:0];

#ifdef CN1_GLASS_PROFILE
    CFTimeInterval cn1gpT0 = CACurrentMediaTime();
#endif
    // 2b) Patch cache: when this glass rect, its material params AND the
    //     backdrop bytes are unchanged since the last composition, redraw the
    //     cached patch and skip the transform + blur + optics entirely (see
    //     the policy comment above the cache).
    uint64_t backdropHash = cn1GlassBackdropHash(avail, availRow * (NSUInteger)ah);
    int cacheSlot = -1;
    for (int ci = 0; ci < CN1_GLASS_PATCH_CACHE_SLOTS; ci++) {
        CN1GlassPatchCacheEntry *e = &cn1GlassPatchCache[ci];
        if (e->valid && e->fx == fx && e->fy == fy && e->fw == fw && e->fh == fh
                && e->rad == rad && e->cornerRadius == cornerRadius && e->sat == sat
                && e->scale == scale && e->offset == offset && e->refract == refract
                && e->specular == specular) {
            cacheSlot = ci;
            if (e->backdropHash == backdropHash && e->patch != NULL) {
                free(avail);
                [self setFramebuffer];
                [self drawGlassPatch:e->patch fw:fw fh:fh x:x y:y w:w h:h];
#ifdef CN1_GLASS_PROFILE
                NSLog(@"CN1GLASSPROF hit rect=%d,%d %dx%d hash=%016llx %.2fms",
                      fx, fy, fw, fh, (unsigned long long)backdropHash,
                      (CACurrentMediaTime() - cn1gpT0) * 1000.0);
#endif
                CN1_SCRATCH_RELEASE return;
            }
            break;
        }
    }

    // 3) Edge-replicate into a padded buffer and apply the colour material.
    uint32_t *prgb = (uint32_t *)malloc((size_t)bw * (size_t)bh * 4);
    if (prgb == NULL) { free(avail); [self setFramebuffer]; CN1_SCRATCH_RELEASE return; }
    for (int by = 0; by < bh; by++) {
        int ay = (fy - pad + by) - ay0; if (ay < 0) ay = 0; else if (ay >= ah) ay = ah - 1;
        for (int bx = 0; bx < bw; bx++) {
            int axc = (fx - pad + bx) - ax0; if (axc < 0) axc = 0; else if (axc >= aw) axc = aw - 1;
            uint8_t *p = avail + (size_t)ay * availRow + (size_t)axc * 4;
            float bch = p[0], gch = p[1], rch = p[2];   // BGRA premult-first (backdrop opaque)
            float lum = 0.2126f * rch + 0.7152f * gch + 0.0722f * bch;
            float rr = (lum + (rch - lum) * sat) * scale + offset;
            float gg = (lum + (gch - lum) * sat) * scale + offset;
            float bb = (lum + (bch - lum) * sat) * scale + offset;
            int ri = rr < 0 ? 0 : (rr > 255 ? 255 : (int)rr);
            int gi = gg < 0 ? 0 : (gg > 255 ? 255 : (int)gg);
            int bi = bb < 0 ? 0 : (bb > 255 ? 255 : (int)bb);
            prgb[(size_t)by * bw + bx] = 0xff000000u | ((uint32_t)ri << 16) | ((uint32_t)gi << 8) | (uint32_t)bi;
        }
    }
    free(avail);

    // 4) Blur the padded material buffer, then optics -> premultiplied patch.
    glassGaussianBlur(prgb, bw, bh, rad);
    uint32_t *out = (uint32_t *)malloc((size_t)fw * (size_t)fh * 4);
    if (out == NULL) { free(prgb); [self setFramebuffer]; CN1_SCRATCH_RELEASE return; }
    glassApplyOptics(prgb, bw, bh, pad, out, fw, fh, cornerRadius, refract, specular, (float)s);
    free(prgb);

    // 4b) Store the composed patch in the cache (the cache owns the buffer).
    if (cacheSlot < 0) {
        cacheSlot = cn1GlassPatchCacheNext;
        cn1GlassPatchCacheNext = (cn1GlassPatchCacheNext + 1) % CN1_GLASS_PATCH_CACHE_SLOTS;
    }
    CN1GlassPatchCacheEntry *entry = &cn1GlassPatchCache[cacheSlot];
    if (entry->patch != NULL) {
        free(entry->patch);
    }
    entry->valid = 1;
    entry->fx = fx; entry->fy = fy; entry->fw = fw; entry->fh = fh;
    entry->rad = rad; entry->cornerRadius = cornerRadius; entry->sat = sat;
    entry->scale = scale; entry->offset = offset; entry->refract = refract;
    entry->specular = specular;
    entry->backdropHash = backdropHash;
    entry->patch = out;

    // 5) Restart the screen encoder, then draw the glass patch back (display coords).
    [self setFramebuffer];
    [self drawGlassPatch:out fw:fw fh:fh x:x y:y w:w h:h];
#ifdef CN1_GLASS_PROFILE
    NSLog(@"CN1GLASSPROF miss rect=%d,%d %dx%d hash=%016llx %.2fms",
          fx, fy, fw, fh, (unsigned long long)backdropHash,
          (CACurrentMediaTime() - cn1gpT0) * 1000.0);
#endif
#ifndef CN1_USE_ARC
    [scratch release];
#endif
}

// Uploads a composed premultiplied-BGRA glass patch and draws it at the given
// CN1-logical rect. The patch buffer is NOT consumed (the cache owns it).
- (void)drawGlassPatch:(uint32_t *)patch fw:(int)fw fh:(int)fh x:(int)x y:(int)y w:(int)w h:(int)h {
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    CGContextRef bmp = CGBitmapContextCreate(patch, fw, fh, 8, (size_t)fw * 4, cs,
        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little);
    CGImageRef outCg = bmp ? CGBitmapContextCreateImage(bmp) : NULL;
    if (outCg != NULL) {
        CN1Image *glassImage = CN1AppKitNSImageFromCGImage(outCg);
        id<MTLTexture> glassTex = CN1MetalTextureFromUIImage(glassImage);
        if (glassTex != nil) { CN1MetalDrawImage(glassTex, 255, x, y, w, h); }
        CGImageRelease(outCg);
    }
    if (bmp != NULL) { CGContextRelease(bmp); }
    CGColorSpaceRelease(cs);
}

// Live-screen iOS 26 selection "drop" LENS. Unlike glassScreenRegionX (a frosted
// blur behind the content) this is painted OVER the bar + the black glyphs and
// reads them back: it magnifies, chromatically aberrates and dark->accent tints
// the live content beneath it (see glassApplyLens). No padding/blur -- the lens
// samples within its own bounds. Runs during the drain like the glass op.
- (void)lensScreenRegionX:(int)x y:(int)y w:(int)w h:(int)h cornerRadius:(float)cornerRadius
                  magnify:(float)magnify aberration:(float)aberration tintColor:(int)tintColor tintStrength:(float)tintStrength {
    if (self.screenTexture == nil || w <= 0 || h <= 0) {
        return;
    }
    float sv = scaleValue > 0.0f ? scaleValue : 1.0f;
    CGFloat s = CN1AppKitBackingScale(self) / sv;
    int texW = (int)self.screenTexture.width, texH = (int)self.screenTexture.height;
    int fx = (int)(x * s), fy = (int)(y * s), fw = (int)(w * s), fh = (int)(h * s);
    if (fx < 0) { fw += fx; fx = 0; }
    if (fy < 0) { fh += fy; fy = 0; }
    if (fx + fw > texW) { fw = texW - fx; }
    if (fy + fh > texH) { fh = texH - fy; }
    if (fw <= 0 || fh <= 0) { return; }

    // GPU LENS: blit the bar region to a scratch texture and draw the drop quad with the
    // cn1_fs_lens shader sampling it -- entirely on the GPU. The old path read the region
    // back to the CPU (2x waitUntilCompleted stalls + getBytes + a CN1Image->texture upload)
    // EVERY frame, capping the morph at ~6fps; this keeps it at frame rate.
    //
    // 1) End the current render encoder so the bar draws are flushed into screenTexture, but
    //    KEEP the frame's command buffer: the blit + lens draw go on the SAME buffer so the
    //    GPU executes bar-draw -> blit -> lens-draw in order (Metal tracks texture hazards),
    //    with no CPU sync.
    if (self.renderCommandEncoder != nil) {
        CN1MetalEndFrame();
        [self.renderCommandEncoder endEncoding];
        self.renderCommandEncoder = nil;
    }
    if (self.commandBuffer == nil) {
        // A prior op already committed it; screenTexture holds the bar, so a fresh buffer is fine.
        self.commandBuffer = [self.commandQueue commandBuffer];
    }

    // 2) Scratch texture (Private = GPU-only; ShaderRead for the fragment sample).
    id<MTLDevice> device = CN1MetalDevice();
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm width:fw height:fh mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModePrivate;
    id<MTLTexture> scratch = [device newTextureWithDescriptor:desc];
    if (scratch == nil) { [self setFramebuffer]; CN1_SCRATCH_RELEASE return; }

    // 3) Blit the bar region screenTexture -> scratch on the frame's command buffer.
    id<MTLBlitCommandEncoder> blit = [self.commandBuffer blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture sourceSlice:0 sourceLevel:0
              sourceOrigin:MTLOriginMake(fx, fy, 0) sourceSize:MTLSizeMake(fw, fh, 1)
                 toTexture:scratch destinationSlice:0 destinationLevel:0
         destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];

    // 4) Restart a render encoder on the SAME command buffer (loadAction Load preserves the
    //    bar) and re-publish it to the CN1Metalcompat draw layer.
    [self createRenderPassDescriptor];
    if (self.renderPassDescriptor == nil) { CN1_SCRATCH_RELEASE return; }
    self.renderCommandEncoder = [self.commandBuffer renderCommandEncoderWithDescriptor:self.renderPassDescriptor];
    [self.renderCommandEncoder setViewport:(MTLViewport){ 0.0, 0.0, (double)framebufferWidth, (double)framebufferHeight, 0.0, 1.0 }];
    CN1MetalBeginFrame(self.renderCommandEncoder, projectionMatrix, framebufferWidth, framebufferHeight);

    // 5) Draw the lens quad sampling scratch (cornerRadius logical -> physical px; < 0 = capsule).
    float crPx = cornerRadius < 0.0f ? -1.0f : cornerRadius * (float)s;
    CN1MetalDrawLens(scratch, x, y, w, h, fw, fh, magnify, aberration, tintColor, tintStrength, crPx);
#ifndef CN1_USE_ARC
    [scratch release];
#endif
}

@end

#endif
