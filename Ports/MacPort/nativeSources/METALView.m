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
    metalLayer.device = MTLCreateSystemDefaultDevice();
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

    id<MTLCommandQueue> newQueue = [metalLayer.device newCommandQueue];
    self.commandQueue = newQueue;
#ifndef CN1_USE_ARC
    [newQueue release];
#endif
    // Publish device and queue once, on the main thread, so CN1Metalcompat's
    // accessors are cheap static reads from the EDT and any background queue
    // rather than a dereference of this view's layer.
    CN1MetalSetDeviceAndCommandQueue(metalLayer.device, self.commandQueue);

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
    clearRetainedFramebufferOnNextFrame = YES;
}

- (void)prepareRetainedFramebufferForDrawRect:(CGRect)rect displayWidth:(int)displayWidth
                                displayHeight:(int)displayHeight {
    // A full-screen repaint is the only case where the retained target's
    // contents are known to be about to be replaced entirely.
    if (rect.origin.x <= 0 && rect.origin.y <= 0
            && rect.size.width >= displayWidth && rect.size.height >= displayHeight) {
        clearRetainedFramebufferOnNextFrame = YES;
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
extern void pointerHoverNative(int x, int y);
extern void keyPressedNative(int keyCode);
extern void keyReleasedNative(int keyCode);
extern void pointerWheelMovedCallback(int x, int y, int scrollX, int scrollY);
extern void pinchMagnifyCallback(float scale, int x, int y);
extern void rotationGestureCallback(float radians, int x, int y);

// The per-window equivalents, shared with Mac Catalyst. A secondary window's
// events carry its id so Desktop can route them without a lookup on AppKit's
// thread; the main window has no id and uses the bridges above, which is also
// what keeps the single window screenshot baselines exercising the identical
// Java path they always did.
extern void CN1MacWindowDeliverPointer(int windowId, int type, int x, int y);
extern void CN1MacWindowDeliverHover(int windowId, int type, int x, int y);
extern void CN1MacWindowDeliverWheel(int windowId, int x, int y, int scrollX, int scrollY);
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

- (void)mouseDown:(NSEvent *)event {
    [self cn1Deliver:event to:pointerPressed type:CN1_POINTER_PRESSED];
}

- (void)mouseDragged:(NSEvent *)event {
    [self cn1Deliver:event to:pointerDragged type:CN1_POINTER_DRAGGED];
}

- (void)mouseUp:(NSEvent *)event {
    [self cn1Deliver:event to:pointerReleased type:CN1_POINTER_RELEASED];
}

// Right and middle buttons reach Codename One as ordinary pointer events,
// because its input model has one pointer. A context menu, if the application
// wants one, comes from menuForEvent: rather than from a second button here.
- (void)rightMouseDown:(NSEvent *)event {
    [self cn1Deliver:event to:pointerPressed type:CN1_POINTER_PRESSED];
}

- (void)rightMouseDragged:(NSEvent *)event {
    [self cn1Deliver:event to:pointerDragged type:CN1_POINTER_DRAGGED];
}

- (void)rightMouseUp:(NSEvent *)event {
    [self cn1Deliver:event to:pointerReleased type:CN1_POINTER_RELEASED];
}

- (void)mouseMoved:(NSEvent *)event {
    if (!self.cn1InputEnabled) {
        return;
    }
    CGPoint p = [self cn1PointFromEvent:event];
    if (self.cn1WindowId >= 0) {
        CN1MacWindowDeliverHover(self.cn1WindowId, CN1_POINTER_DRAGGED, (int)p.x, (int)p.y);
        return;
    }
    pointerHoverNative((int)p.x, (int)p.y);
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
    if (self.cn1WindowId >= 0) {
        CN1MacWindowDeliverWheel(self.cn1WindowId, (int)p.x, (int)p.y,
                                 (int)(-dx * s), (int)(-dy * s));
        return;
    }
    pointerWheelMovedCallback((int)p.x, (int)p.y, (int)(-dx * s), (int)(-dy * s));
}

- (void)magnifyWithEvent:(NSEvent *)event {
    CGPoint p = [self cn1PointFromEvent:event];
    // AppKit reports the increment since the last event; Codename One's pinch
    // wants a factor, and 1 + increment is that factor.
    float scale = (float)(1.0 + event.magnification);
    if (self.cn1WindowId >= 0) {
        CN1MacWindowDeliverPinch(self.cn1WindowId, scale, (int)p.x, (int)p.y);
        return;
    }
    pinchMagnifyCallback(scale, (int)p.x, (int)p.y);
}

- (void)rotateWithEvent:(NSEvent *)event {
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

- (void)keyDown:(NSEvent *)event {
    if (!self.cn1InputEnabled) {
        return;
    }
    if ([CN1MacTextInputSession sharedSession].active) {
        // Hands the key to the input context, which is what turns a keystroke
        // into insertText:, setMarkedText: or doCommandBySelector:. Doing this
        // rather than reading characters directly is what buys dead keys, CJK
        // input methods, dictation and the Emacs key bindings for free.
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

- (BOOL)cn1ReplaceRange:(NSRange)range withString:(NSString *)string {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (range.location == NSNotFound) {
        return NO;
    }
    if (NSMaxRange(range) > session.text.length) {
        return NO;
    }
    session.text = [session.text stringByReplacingCharactersInRange:range
                                                         withString:string];
    return YES;
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
    if (![self cn1ReplaceRange:range withString:inserted]) {
        return;
    }
    session.markedRange = NSMakeRange(NSNotFound, 0);
    session.selectedRange = NSMakeRange(range.location + inserted.length, 0);
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
    if (![self cn1ReplaceRange:range withString:marked]) {
        return;
    }
    // The marked run is pushed into the framework's text so a composition is
    // visible as it is typed. Codename One has no way to express the system
    // underline that normally distinguishes uncommitted text, so composing
    // characters read as ordinary ones until they are committed.
    session.markedRange = marked.length > 0
        ? NSMakeRange(range.location, marked.length)
        : NSMakeRange(NSNotFound, 0);
    session.selectedRange = NSMakeRange(range.location + selectedRange.location,
                                        selectedRange.length);
    [session commitFinished:NO];
}

- (void)unmarkText {
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
    return [[NSAttributedString alloc] initWithString:[text substringWithRange:clamped]];
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
            session.selectedRange = NSMakeRange(sel.location, 0);
        } else if (sel.location > 0) {
            // Steps by composed character so deleting an emoji or an accented
            // letter removes the whole thing rather than half of it.
            NSRange back = [session.text rangeOfComposedCharacterSequenceAtIndex:sel.location - 1];
            [self cn1ReplaceRange:back withString:@""];
            session.selectedRange = NSMakeRange(back.location, 0);
        }
        [session commitFinished:NO];
        return;
    }
    if (selector == @selector(deleteForward:)) {
        if (sel.length > 0) {
            [self cn1ReplaceRange:sel withString:@""];
            session.selectedRange = NSMakeRange(sel.location, 0);
        } else if (sel.location < len) {
            NSRange fwd = [session.text rangeOfComposedCharacterSequenceAtIndex:sel.location];
            [self cn1ReplaceRange:fwd withString:@""];
            session.selectedRange = NSMakeRange(fwd.location, 0);
        }
        [session commitFinished:NO];
        return;
    }
    if (selector == @selector(moveLeft:)) {
        NSUInteger at = sel.length > 0 ? sel.location : (sel.location > 0 ? sel.location - 1 : 0);
        session.selectedRange = NSMakeRange(at, 0);
        return;
    }
    if (selector == @selector(moveRight:)) {
        NSUInteger at = sel.length > 0 ? NSMaxRange(sel) : MIN(sel.location + 1, len);
        session.selectedRange = NSMakeRange(at, 0);
        return;
    }
    if (selector == @selector(moveToBeginningOfLine:)
        || selector == @selector(moveToBeginningOfParagraph:)
        || selector == @selector(moveToBeginningOfDocument:)) {
        session.selectedRange = NSMakeRange(0, 0);
        return;
    }
    if (selector == @selector(moveToEndOfLine:)
        || selector == @selector(moveToEndOfParagraph:)
        || selector == @selector(moveToEndOfDocument:)) {
        session.selectedRange = NSMakeRange(len, 0);
        return;
    }
    if (selector == @selector(selectAll:)) {
        session.selectedRange = NSMakeRange(0, len);
        return;
    }
    if (selector == @selector(deleteToEndOfLine:) || selector == @selector(deleteToEndOfParagraph:)) {
        NSRange tail = NSMakeRange(sel.location, len - sel.location);
        [self cn1ReplaceRange:tail withString:@""];
        session.selectedRange = NSMakeRange(sel.location, 0);
        [session commitFinished:NO];
        return;
    }
    // Anything left is a binding with no meaning for a single field editor.
    // Swallowed rather than passed up, because NSResponder answers an unhandled
    // text command with a beep.
}


- (void)keyUp:(NSEvent *)event {
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

// ---- materials ----------------------------------------------------------

// The blur, glass and lens materials are not implemented yet. They read the
// screen texture back, run a CoreImage pipeline over it and upload the result,
// and each needs its own verification against a golden. Doing nothing leaves the
// region showing whatever was already drawn there, which is the same thing the
// UIKit backend does when its own capture fails -- it never leaves a hole.
- (void)blurScreenRegionX:(int)x y:(int)y w:(int)w h:(int)h radius:(float)radius {
}

- (void)glassScreenRegionX:(int)x y:(int)y w:(int)w h:(int)h radius:(float)radius
              cornerRadius:(float)cornerRadius sat:(float)sat scale:(float)scale
                    offset:(float)offset refract:(float)refract specular:(float)specular {
}

- (void)lensScreenRegionX:(int)x y:(int)y w:(int)w h:(int)h cornerRadius:(float)cornerRadius
                  magnify:(float)magnify aberration:(float)aberration
                tintColor:(int)tintColor tintStrength:(float)tintStrength {
}

@end

#endif
