/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

#import "CN1GL3D.h"
#import "xmlvm.h"

#ifdef CN1_USE_METAL
#import "com_codename1_impl_ios_IOSGLSurface.h"

// Pixel format of the depth attachment shared by all 3D pipelines.
static const MTLPixelFormat CN1GL3D_DEPTH_FORMAT = MTLPixelFormatDepth32Float;

// ---------------------------------------------------------------------------
// Cached pipeline state variant.
// ---------------------------------------------------------------------------
@interface CN1GL3DPipeline : NSObject
@property (nonatomic, strong) id<MTLRenderPipelineState> pipelineState;
@property (nonatomic, strong) id<MTLDepthStencilState> depthStencilState;
@property (nonatomic, assign) MTLCullMode cullMode;
@end

@implementation CN1GL3DPipeline
@end

// ---------------------------------------------------------------------------
// The Metal 3D view / context.
// ---------------------------------------------------------------------------
@interface CN1GL3DView () {
    BOOL _pendingClearColor;
    BOOL _pendingClearDepth;
    MTLClearColor _clearColor;
    MTLViewport _viewport;
    BOOL _hasViewport;
    BOOL _continuous;
    int _depthWidth;
    int _depthHeight;
}
@property (nonatomic, strong) id<MTLTexture> depthTexture;
@property (nonatomic, strong) CADisplayLink *displayLink;
@property (nonatomic, strong) NSMutableDictionary<NSString *, CN1GL3DPipeline *> *pipelineCache;
@property (nonatomic, strong) id<MTLRenderCommandEncoder> currentEncoder;
- (id<MTLRenderCommandEncoder>)activeEncoder;
- (void)teardown;
- (CN1GL3DPipeline *)pipelineForKey:(NSString *)key source:(NSString *)mslSource
        blendMode:(int)blendMode cullMode:(int)cullMode
        depthTest:(int)depthTest depthWrite:(int)depthWrite strideBytes:(int)strideBytes;
@end

@implementation CN1GL3DView

+ (Class)layerClass {
    return [CAMetalLayer class];
}

- (instancetype)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        _device = MTLCreateSystemDefaultDevice();
        if (_device == nil) {
            return nil;
        }
        _commandQueue = [_device newCommandQueue];
        _pipelineCache = [NSMutableDictionary dictionary];
        _pendingClearColor = YES;
        _pendingClearDepth = YES;
        _clearColor = MTLClearColorMake(0, 0, 0, 1);
        _hasViewport = NO;
        _continuous = NO;

        CAMetalLayer *layer = (CAMetalLayer *) self.layer;
        layer.device = _device;
        layer.pixelFormat = MTLPixelFormatBGRA8Unorm;
        layer.framebufferOnly = YES;
        layer.opaque = YES;
        // Match the device scale. traitCollection.displayScale avoids the
        // deprecated UIScreen.mainScreen; it falls back to 2.0 before the view
        // is attached to a window (layoutSubviews re-derives the real scale).
        CGFloat scale = self.traitCollection.displayScale;
        self.contentScaleFactor = scale > 0.0 ? scale : 2.0;
    }
    return self;
}

- (void)layoutSubviews {
    [super layoutSubviews];
    CAMetalLayer *layer = (CAMetalLayer *) self.layer;
    CGFloat scale = self.contentScaleFactor;
    int pw = (int)(self.bounds.size.width * scale);
    int ph = (int)(self.bounds.size.height * scale);
    if (pw < 1) pw = 1;
    if (ph < 1) ph = 1;
    layer.drawableSize = CGSizeMake(pw, ph);
    [self ensureDepth:pw h:ph];
    if (!_continuous) {
        [self requestRender];
    }
}

- (void)ensureDepth:(int)w h:(int)h {
    if (_depthTexture != nil && _depthWidth == w && _depthHeight == h) {
        return;
    }
    MTLTextureDescriptor *dd = [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:CN1GL3D_DEPTH_FORMAT
                                                                                  width:w height:h mipmapped:NO];
    dd.usage = MTLTextureUsageRenderTarget;
    dd.storageMode = MTLStorageModePrivate;
    _depthTexture = [_device newTextureWithDescriptor:dd];
    _depthWidth = w;
    _depthHeight = h;
}

- (void)setContinuous:(BOOL)continuous {
    _continuous = continuous;
    dispatch_async(dispatch_get_main_queue(), ^{
        if (continuous) {
            if (self.displayLink == nil) {
                self.displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(renderFrame)];
                [self.displayLink addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSRunLoopCommonModes];
            }
            self.displayLink.paused = NO;
        } else {
            self.displayLink.paused = YES;
        }
    });
}

- (void)requestRender {
    if ([NSThread isMainThread]) {
        [self renderFrame];
    } else {
        dispatch_async(dispatch_get_main_queue(), ^{
            [self renderFrame];
        });
    }
}

- (void)renderFrame {
    CAMetalLayer *layer = (CAMetalLayer *) self.layer;
    int w = (int) layer.drawableSize.width;
    int h = (int) layer.drawableSize.height;
    if (w < 1 || h < 1) {
        return;
    }
    [self ensureDepth:w h:h];
    id<CAMetalDrawable> drawable = [layer nextDrawable];
    if (drawable == nil) {
        return;
    }

    MTLRenderPassDescriptor *rpd = [MTLRenderPassDescriptor renderPassDescriptor];
    rpd.colorAttachments[0].texture = drawable.texture;
    rpd.colorAttachments[0].clearColor = _clearColor;
    rpd.colorAttachments[0].loadAction = _pendingClearColor ? MTLLoadActionClear : MTLLoadActionLoad;
    rpd.colorAttachments[0].storeAction = MTLStoreActionStore;
    rpd.depthAttachment.texture = _depthTexture;
    rpd.depthAttachment.clearDepth = 1.0;
    rpd.depthAttachment.loadAction = _pendingClearDepth ? MTLLoadActionClear : MTLLoadActionDontCare;
    rpd.depthAttachment.storeAction = MTLStoreActionDontCare;
    _pendingClearColor = NO;
    _pendingClearDepth = NO;

    id<MTLCommandBuffer> cb = [self.commandQueue commandBuffer];
    id<MTLRenderCommandEncoder> encoder = [cb renderCommandEncoderWithDescriptor:rpd];

    if (!_hasViewport) {
        _viewport = (MTLViewport){0.0, 0.0, (double) w, (double) h, 0.0, 1.0};
    }
    [encoder setViewport:_viewport];
    self.currentEncoder = encoder;

    // Hand control to the Java renderer; its draw calls route back through the
    // gl3dDraw* bridge functions and use self.currentEncoder.
    com_codename1_impl_ios_IOSGLSurface_onFrameNative___long_int_int(
        CN1_THREAD_GET_STATE_PASS_ARG (JAVA_LONG) self.contextHandle, w, h);

    static int cn1gl3dFrameLogCount = 0;
    if (cn1gl3dFrameLogCount < 4) {
        cn1gl3dFrameLogCount++;
        NSLog(@"CN1SS:GL3D:renderFrame w=%d h=%d clearColor=(%.2f,%.2f,%.2f) hasViewport=%d depthTex=%p",
              w, h, _clearColor.red, _clearColor.green, _clearColor.blue, (int) _hasViewport, _depthTexture);
    }
    [encoder endEncoding];
    [cb presentDrawable:drawable];
    [cb commit];
    self.currentEncoder = nil;
}

- (void)recordClear:(int)argb color:(BOOL)clearColor depth:(BOOL)clearDepth {
    if (clearColor) {
        float a = ((argb >> 24) & 0xff) / 255.0f;
        float r = ((argb >> 16) & 0xff) / 255.0f;
        float g = ((argb >> 8) & 0xff) / 255.0f;
        float b = (argb & 0xff) / 255.0f;
        _clearColor = MTLClearColorMake(r, g, b, a);
        _pendingClearColor = YES;
    }
    if (clearDepth) {
        _pendingClearDepth = YES;
    }
}

- (void)recordViewport:(int)x y:(int)y width:(int)width height:(int)height {
    _viewport = (MTLViewport){(double) x, (double) y, (double) width, (double) height, 0.0, 1.0};
    _hasViewport = YES;
}

- (id<MTLRenderCommandEncoder>)activeEncoder {
    return self.currentEncoder;
}

- (CN1GL3DPipeline *)pipelineForKey:(NSString *)key source:(NSString *)mslSource
        blendMode:(int)blendMode cullMode:(int)cullMode
        depthTest:(int)depthTest depthWrite:(int)depthWrite strideBytes:(int)strideBytes {
    CN1GL3DPipeline *cached = self.pipelineCache[key];
    if (cached != nil) {
        return cached;
    }

    NSError *err = nil;
    id<MTLLibrary> lib = [self.device newLibraryWithSource:mslSource options:nil error:&err];
    if (lib == nil) {
        NSLog(@"[CN1GL3D] shader compile failed for %@: %@", key, err);
        return nil;
    }
    id<MTLFunction> vfn = [lib newFunctionWithName:@"cn1_vertex_main"];
    id<MTLFunction> ffn = [lib newFunctionWithName:@"cn1_fragment_main"];
    if (vfn == nil || ffn == nil) {
        NSLog(@"[CN1GL3D] missing shader entry points for %@", key);
        return nil;
    }

    MTLRenderPipelineDescriptor *desc = [[MTLRenderPipelineDescriptor alloc] init];
    desc.vertexFunction = vfn;
    desc.fragmentFunction = ffn;
    desc.colorAttachments[0].pixelFormat = MTLPixelFormatBGRA8Unorm;
    desc.depthAttachmentPixelFormat = CN1GL3D_DEPTH_FORMAT;

    if (blendMode == 1) { // ALPHA (source-over)
        desc.colorAttachments[0].blendingEnabled = YES;
        desc.colorAttachments[0].rgbBlendOperation = MTLBlendOperationAdd;
        desc.colorAttachments[0].alphaBlendOperation = MTLBlendOperationAdd;
        desc.colorAttachments[0].sourceRGBBlendFactor = MTLBlendFactorSourceAlpha;
        desc.colorAttachments[0].sourceAlphaBlendFactor = MTLBlendFactorSourceAlpha;
        desc.colorAttachments[0].destinationRGBBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
        desc.colorAttachments[0].destinationAlphaBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
    } else if (blendMode == 2) { // ADDITIVE
        desc.colorAttachments[0].blendingEnabled = YES;
        desc.colorAttachments[0].rgbBlendOperation = MTLBlendOperationAdd;
        desc.colorAttachments[0].alphaBlendOperation = MTLBlendOperationAdd;
        desc.colorAttachments[0].sourceRGBBlendFactor = MTLBlendFactorSourceAlpha;
        desc.colorAttachments[0].sourceAlphaBlendFactor = MTLBlendFactorOne;
        desc.colorAttachments[0].destinationRGBBlendFactor = MTLBlendFactorOne;
        desc.colorAttachments[0].destinationAlphaBlendFactor = MTLBlendFactorOne;
    } else {
        desc.colorAttachments[0].blendingEnabled = NO;
    }

    // Vertex layout decoded from the stride. We declare position at attribute(0)
    // and, depending on the canonical interleaved layout implied by the stride,
    // normal and/or texcoord at their float-offset attribute indices (matching
    // the [[attribute(n)]] indices the MSL generator emits). Attributes not
    // referenced by the compiled shader are ignored by Metal.
    MTLVertexDescriptor *vd = [MTLVertexDescriptor vertexDescriptor];
    int strideFloats = strideBytes / 4;
    vd.attributes[0].format = MTLVertexFormatFloat3; // position
    vd.attributes[0].offset = 0;
    vd.attributes[0].bufferIndex = 0;
    if (strideFloats == 5) {
        // position + texcoord: texcoord at float offset 3
        vd.attributes[3].format = MTLVertexFormatFloat2;
        vd.attributes[3].offset = 12;
        vd.attributes[3].bufferIndex = 0;
    } else if (strideFloats == 6) {
        // position + normal: normal at float offset 3
        vd.attributes[3].format = MTLVertexFormatFloat3;
        vd.attributes[3].offset = 12;
        vd.attributes[3].bufferIndex = 0;
    } else if (strideFloats == 8) {
        // position + normal + texcoord
        vd.attributes[3].format = MTLVertexFormatFloat3;
        vd.attributes[3].offset = 12;
        vd.attributes[3].bufferIndex = 0;
        vd.attributes[6].format = MTLVertexFormatFloat2;
        vd.attributes[6].offset = 24;
        vd.attributes[6].bufferIndex = 0;
    }
    vd.layouts[0].stride = strideBytes;
    vd.layouts[0].stepFunction = MTLVertexStepFunctionPerVertex;
    desc.vertexDescriptor = vd;

    id<MTLRenderPipelineState> pso = [self.device newRenderPipelineStateWithDescriptor:desc error:&err];
    if (pso == nil) {
        NSLog(@"[CN1GL3D] pipeline state creation failed for %@: %@", key, err);
        return nil;
    }

    MTLDepthStencilDescriptor *dsd = [[MTLDepthStencilDescriptor alloc] init];
    dsd.depthCompareFunction = depthTest ? MTLCompareFunctionLess : MTLCompareFunctionAlways;
    dsd.depthWriteEnabled = depthWrite ? YES : NO;
    id<MTLDepthStencilState> dss = [self.device newDepthStencilStateWithDescriptor:dsd];

    CN1GL3DPipeline *p = [[CN1GL3DPipeline alloc] init];
    p.pipelineState = pso;
    p.depthStencilState = dss;
    p.cullMode = cullMode == 1 ? MTLCullModeBack : (cullMode == 2 ? MTLCullModeFront : MTLCullModeNone);
    self.pipelineCache[key] = p;
    return p;
}

- (void)teardown {
    self.displayLink.paused = YES;
    [self.displayLink invalidate];
    self.displayLink = nil;
    [self.pipelineCache removeAllObjects];
}

@end

// ---------------------------------------------------------------------------
// IOSNative bridge functions. Each `native ... gl3d*` on IOSNative.java has a
// matching C function. Buffer/texture handles are Objective-C object pointers
// cast to JAVA_LONG; retained via __bridge_retained, released in dispose*.
// Pipelines are owned by the view's cache (pointers are non-owning).
// ---------------------------------------------------------------------------

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateContext___R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    __block CN1GL3DView *view = nil;
    void (^create)(void) = ^{
        view = [[CN1GL3DView alloc] initWithFrame:CGRectMake(0, 0, 1, 1)];
    };
    if ([NSThread isMainThread]) {
        create();
    } else {
        dispatch_sync(dispatch_get_main_queue(), create);
    }
    if (view == nil) {
        return 0;
    }
    long handle = (long)(__bridge_retained void *) view;
    view.contextHandle = handle;
    return (JAVA_LONG) handle;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dGetViewPeer___long_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer) {
    if (contextPeer == 0) return 0;
    CN1GL3DView *view = (__bridge CN1GL3DView *)(void *) contextPeer;
    return (JAVA_LONG)(__bridge_retained void *) view;
}

void com_codename1_impl_ios_IOSNative_gl3dDestroyContext___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer) {
    if (contextPeer == 0) return;
    CN1GL3DView *view = (__bridge_transfer CN1GL3DView *)(void *) contextPeer;
    dispatch_async(dispatch_get_main_queue(), ^{
        [view teardown];
    });
    view = nil; // released by __bridge_transfer
}

void com_codename1_impl_ios_IOSNative_gl3dSetContinuous___long_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer, JAVA_BOOLEAN continuous) {
    if (contextPeer == 0) return;
    CN1GL3DView *view = (__bridge CN1GL3DView *)(void *) contextPeer;
    [view setContinuous:continuous ? YES : NO];
}

void com_codename1_impl_ios_IOSNative_gl3dRequestRender___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer) {
    if (contextPeer == 0) return;
    CN1GL3DView *view = (__bridge CN1GL3DView *)(void *) contextPeer;
    [view requestRender];
}

// Builds a MTLBuffer over the SIMD aligned Java array. The payload sits at
// ((JAVA_ARRAY)arr)->data. newBufferWithBytesNoCopy needs page (4096 byte)
// alignment, which the 16-byte SIMD allocator does not guarantee, so we use a
// single cheap copy unless the pointer happens to be page aligned (true zero
// copy path).
static id<MTLBuffer> CN1GL3DMakeBuffer(id<MTLDevice> device, void *ptr, int byteLength) {
    if (byteLength <= 0) {
        return [device newBufferWithLength:16 options:MTLResourceStorageModeShared];
    }
    NSUInteger pageSize = (NSUInteger) getpagesize();
    if (((uintptr_t) ptr % pageSize) == 0) {
        id<MTLBuffer> b = [device newBufferWithBytesNoCopy:ptr length:byteLength
                                                  options:MTLResourceStorageModeShared
                                              deallocator:nil];
        if (b != nil) {
            return b;
        }
    }
    return [device newBufferWithBytes:ptr length:byteLength options:MTLResourceStorageModeShared];
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateFloatBuffer___float_1ARRAY_int_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT data, JAVA_INT floatCount) {
    JAVA_ARRAY_FLOAT *ptr = (JAVA_ARRAY_FLOAT *)((JAVA_ARRAY) data)->data;
    id<MTLDevice> device = MTLCreateSystemDefaultDevice();
    id<MTLBuffer> buf = CN1GL3DMakeBuffer(device, ptr, (int)(floatCount * sizeof(JAVA_ARRAY_FLOAT)));
    return (JAVA_LONG)(__bridge_retained void *) buf;
}

void com_codename1_impl_ios_IOSNative_gl3dUpdateFloatBuffer___long_float_1ARRAY_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG bufferPeer, JAVA_OBJECT data, JAVA_INT floatCount) {
    if (bufferPeer == 0) return;
    id<MTLBuffer> buf = (__bridge id<MTLBuffer>)(void *) bufferPeer;
    JAVA_ARRAY_FLOAT *ptr = (JAVA_ARRAY_FLOAT *)((JAVA_ARRAY) data)->data;
    int byteLength = (int)(floatCount * sizeof(JAVA_ARRAY_FLOAT));
    if ((int) buf.length >= byteLength && buf.contents != NULL) {
        memcpy(buf.contents, ptr, byteLength);
    }
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateShortBuffer___short_1ARRAY_int_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT data, JAVA_INT indexCount) {
    JAVA_ARRAY_SHORT *ptr = (JAVA_ARRAY_SHORT *)((JAVA_ARRAY) data)->data;
    id<MTLDevice> device = MTLCreateSystemDefaultDevice();
    id<MTLBuffer> buf = CN1GL3DMakeBuffer(device, ptr, (int)(indexCount * sizeof(JAVA_ARRAY_SHORT)));
    return (JAVA_LONG)(__bridge_retained void *) buf;
}

void com_codename1_impl_ios_IOSNative_gl3dUpdateShortBuffer___long_short_1ARRAY_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG bufferPeer, JAVA_OBJECT data, JAVA_INT indexCount) {
    if (bufferPeer == 0) return;
    id<MTLBuffer> buf = (__bridge id<MTLBuffer>)(void *) bufferPeer;
    JAVA_ARRAY_SHORT *ptr = (JAVA_ARRAY_SHORT *)((JAVA_ARRAY) data)->data;
    int byteLength = (int)(indexCount * sizeof(JAVA_ARRAY_SHORT));
    if ((int) buf.length >= byteLength && buf.contents != NULL) {
        memcpy(buf.contents, ptr, byteLength);
    }
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateTexture___int_1ARRAY_int_int_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT argb, JAVA_INT width, JAVA_INT height) {
    if (width <= 0 || height <= 0) return 0;
    JAVA_ARRAY_INT *src = (JAVA_ARRAY_INT *)((JAVA_ARRAY) argb)->data;
    id<MTLDevice> device = MTLCreateSystemDefaultDevice();
    MTLTextureDescriptor *td = [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
                                                                                  width:width height:height mipmapped:NO];
    td.usage = MTLTextureUsageShaderRead;
    id<MTLTexture> tex = [device newTextureWithDescriptor:td];
    // Codename One stores pixels as packed ARGB ints. On little endian the int
    // 0xAARRGGBB has bytes B,G,R,A in memory, which is exactly BGRA8Unorm, so
    // the int array maps directly to the texture with no swizzle.
    [tex replaceRegion:MTLRegionMake2D(0, 0, width, height)
           mipmapLevel:0
             withBytes:src
           bytesPerRow:width * 4];
    return (JAVA_LONG)(__bridge_retained void *) tex;
}

void com_codename1_impl_ios_IOSNative_gl3dDisposeBuffer___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG bufferPeer) {
    if (bufferPeer == 0) return;
    id<MTLBuffer> buf = (__bridge_transfer id<MTLBuffer>)(void *) bufferPeer;
    buf = nil;
}

void com_codename1_impl_ios_IOSNative_gl3dDisposeTexture___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG texturePeer) {
    if (texturePeer == 0) return;
    id<MTLTexture> tex = (__bridge_transfer id<MTLTexture>)(void *) texturePeer;
    tex = nil;
}

void com_codename1_impl_ios_IOSNative_gl3dDisposePipeline___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pipelinePeer) {
    // Pipelines are owned by the view's cache; nothing to release per handle.
}

// The real implementation lives in the plain (un-suffixed) symbol; the
// _R_<rettype> form below is a thin wrapper. This matches the convention of the
// existing String-argument non-void natives in IOSNative.m (createVideoComponent,
// getResourceSize): ParparVM dispatches the call through the plain symbol.
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dGetOrCreatePipeline___long_java_lang_String_java_lang_String_int_int_int_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer,
        JAVA_OBJECT key, JAVA_OBJECT mslSource, JAVA_INT blendMode, JAVA_INT cullMode,
        JAVA_INT depthTest, JAVA_INT depthWrite) {
    if (contextPeer == 0) return 0;
    CN1GL3DView *view = (__bridge CN1GL3DView *)(void *) contextPeer;
    NSString *keyStr = toNSString(CN1_THREAD_GET_STATE_PASS_ARG key);
    NSString *srcStr = toNSString(CN1_THREAD_GET_STATE_PASS_ARG mslSource);
    // Recover the stride from the key encoding "...|sNN|..." so we can build the
    // vertex descriptor. Falls back to position-only on parse failure.
    int strideBytes = 12;
    NSRange r = [keyStr rangeOfString:@"|s"];
    if (r.location != NSNotFound) {
        NSString *tail = [keyStr substringFromIndex:r.location + 2];
        int parsed = (int)[tail intValue];
        if (parsed > 0) strideBytes = parsed;
    }
    CN1GL3DPipeline *p = [view pipelineForKey:keyStr source:srcStr
                                    blendMode:blendMode cullMode:cullMode
                                    depthTest:depthTest depthWrite:depthWrite strideBytes:strideBytes];
    if (p == nil) {
        return 0;
    }
    return (JAVA_LONG)(__bridge void *) p;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dGetOrCreatePipeline___long_java_lang_String_java_lang_String_int_int_int_int_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer,
        JAVA_OBJECT key, JAVA_OBJECT mslSource, JAVA_INT blendMode, JAVA_INT cullMode,
        JAVA_INT depthTest, JAVA_INT depthWrite) {
    return com_codename1_impl_ios_IOSNative_gl3dGetOrCreatePipeline___long_java_lang_String_java_lang_String_int_int_int_int(
            CN1_THREAD_STATE_PASS_ARG instanceObject, contextPeer, key, mslSource,
            blendMode, cullMode, depthTest, depthWrite);
}

void com_codename1_impl_ios_IOSNative_gl3dClear___long_int_boolean_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer,
        JAVA_INT argbColor, JAVA_BOOLEAN clearColor, JAVA_BOOLEAN clearDepth) {
    if (contextPeer == 0) return;
    CN1GL3DView *view = (__bridge CN1GL3DView *)(void *) contextPeer;
    [view recordClear:argbColor color:clearColor ? YES : NO depth:clearDepth ? YES : NO];
}

void com_codename1_impl_ios_IOSNative_gl3dSetViewport___long_int_int_int_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer,
        JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    if (contextPeer == 0) return;
    CN1GL3DView *view = (__bridge CN1GL3DView *)(void *) contextPeer;
    [view recordViewport:x y:y width:width height:height];
    id<MTLRenderCommandEncoder> enc = [view activeEncoder];
    if (enc != nil) {
        MTLViewport vp = (MTLViewport){(double) x, (double) y, (double) width, (double) height, 0.0, 1.0};
        [enc setViewport:vp];
    }
}

static MTLPrimitiveType CN1GL3DPrimitive(int primitive) {
    switch (primitive) {
        case 0: return MTLPrimitiveTypePoint;
        case 1: return MTLPrimitiveTypeLine;
        case 2: return MTLPrimitiveTypeLineStrip;
        case 4: return MTLPrimitiveTypeTriangleStrip;
        case 3:
        default: return MTLPrimitiveTypeTriangle;
    }
}

static void CN1GL3DBindCommon(CN1GL3DView *view, CN1GL3DPipeline *p,
        id<MTLBuffer> vbo, JAVA_OBJECT uniforms, int uniformFloats,
        long texturePeer, int texFilter, int texWrap) {
    id<MTLRenderCommandEncoder> enc = [view activeEncoder];
    [enc setRenderPipelineState:p.pipelineState];
    [enc setDepthStencilState:p.depthStencilState];
    [enc setCullMode:p.cullMode];
    [enc setFrontFacingWinding:MTLWindingCounterClockwise];
    [enc setVertexBuffer:vbo offset:0 atIndex:0];

    JAVA_ARRAY_FLOAT *uptr = (JAVA_ARRAY_FLOAT *)((JAVA_ARRAY) uniforms)->data;
    int ubytes = (int)(uniformFloats * sizeof(JAVA_ARRAY_FLOAT));
    [enc setVertexBytes:uptr length:ubytes atIndex:1];
    [enc setFragmentBytes:uptr length:ubytes atIndex:1];

    if (texturePeer != 0) {
        id<MTLTexture> tex = (__bridge id<MTLTexture>)(void *) texturePeer;
        [enc setFragmentTexture:tex atIndex:0];
        MTLSamplerDescriptor *sd = [[MTLSamplerDescriptor alloc] init];
        sd.minFilter = texFilter ? MTLSamplerMinMagFilterLinear : MTLSamplerMinMagFilterNearest;
        sd.magFilter = texFilter ? MTLSamplerMinMagFilterLinear : MTLSamplerMinMagFilterNearest;
        sd.sAddressMode = texWrap ? MTLSamplerAddressModeRepeat : MTLSamplerAddressModeClampToEdge;
        sd.tAddressMode = texWrap ? MTLSamplerAddressModeRepeat : MTLSamplerAddressModeClampToEdge;
        id<MTLSamplerState> sampler = [view.device newSamplerStateWithDescriptor:sd];
        [enc setFragmentSamplerState:sampler atIndex:0];
    }
}

void com_codename1_impl_ios_IOSNative_gl3dDrawIndexed___long_long_long_int_long_int_int_float_1ARRAY_int_long_int_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer,
        JAVA_LONG pipelinePeer, JAVA_LONG vboPeer, JAVA_INT strideBytes, JAVA_LONG iboPeer,
        JAVA_INT indexCount, JAVA_INT primitive, JAVA_OBJECT uniforms, JAVA_INT uniformFloats,
        JAVA_LONG texturePeer, JAVA_INT texFilter, JAVA_INT texWrap) {
    if (contextPeer == 0 || pipelinePeer == 0 || vboPeer == 0 || iboPeer == 0) return;
    CN1GL3DView *view = (__bridge CN1GL3DView *)(void *) contextPeer;
    if ([view activeEncoder] == nil) return;
    CN1GL3DPipeline *p = (__bridge CN1GL3DPipeline *)(void *) pipelinePeer;
    id<MTLBuffer> vbo = (__bridge id<MTLBuffer>)(void *) vboPeer;
    id<MTLBuffer> ibo = (__bridge id<MTLBuffer>)(void *) iboPeer;
    CN1GL3DBindCommon(view, p, vbo, uniforms, uniformFloats, (long) texturePeer, texFilter, texWrap);
    static int cn1gl3dDrawLogCount = 0;
    if (cn1gl3dDrawLogCount < 4) {
        cn1gl3dDrawLogCount++;
        JAVA_ARRAY_FLOAT *u = (JAVA_ARRAY_FLOAT *)((JAVA_ARRAY) uniforms)->data;
        NSLog(@"CN1SS:GL3D:drawIndexed count=%d prim=%d enc=%p pso=%p vbo=%p(len=%lu) ibo=%p(len=%lu) mvp0=%.3f mvp5=%.3f mvp15=%.3f",
              (int) indexCount, (int) primitive, [view activeEncoder], p.pipelineState,
              vbo, (unsigned long) vbo.length, ibo, (unsigned long) ibo.length,
              u[0], u[5], u[15]);
    }
    [[view activeEncoder] drawIndexedPrimitives:CN1GL3DPrimitive(primitive)
                                     indexCount:indexCount
                                      indexType:MTLIndexTypeUInt16
                                    indexBuffer:ibo
                              indexBufferOffset:0];
}

void com_codename1_impl_ios_IOSNative_gl3dDrawArrays___long_long_long_int_int_int_float_1ARRAY_int_long_int_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer,
        JAVA_LONG pipelinePeer, JAVA_LONG vboPeer, JAVA_INT strideBytes, JAVA_INT vertexCount,
        JAVA_INT primitive, JAVA_OBJECT uniforms, JAVA_INT uniformFloats,
        JAVA_LONG texturePeer, JAVA_INT texFilter, JAVA_INT texWrap) {
    if (contextPeer == 0 || pipelinePeer == 0 || vboPeer == 0) return;
    CN1GL3DView *view = (__bridge CN1GL3DView *)(void *) contextPeer;
    if ([view activeEncoder] == nil) return;
    CN1GL3DPipeline *p = (__bridge CN1GL3DPipeline *)(void *) pipelinePeer;
    id<MTLBuffer> vbo = (__bridge id<MTLBuffer>)(void *) vboPeer;
    CN1GL3DBindCommon(view, p, vbo, uniforms, uniformFloats, (long) texturePeer, texFilter, texWrap);
    [[view activeEncoder] drawPrimitives:CN1GL3DPrimitive(primitive) vertexStart:0 vertexCount:vertexCount];
}

#else // !CN1_USE_METAL

// Non-Metal builds still need the bridge symbols so ParparVM links. They report
// 3D as unavailable (context creation returns 0) and every op is a no-op.

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateContext___R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) { return 0; }
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dGetViewPeer___long_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer) { return 0; }
void com_codename1_impl_ios_IOSNative_gl3dDestroyContext___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer) {}
void com_codename1_impl_ios_IOSNative_gl3dSetContinuous___long_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer, JAVA_BOOLEAN continuous) {}
void com_codename1_impl_ios_IOSNative_gl3dRequestRender___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer) {}
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateFloatBuffer___float_1ARRAY_int_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT data, JAVA_INT floatCount) { return 0; }
void com_codename1_impl_ios_IOSNative_gl3dUpdateFloatBuffer___long_float_1ARRAY_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG bufferPeer, JAVA_OBJECT data, JAVA_INT floatCount) {}
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateShortBuffer___short_1ARRAY_int_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT data, JAVA_INT indexCount) { return 0; }
void com_codename1_impl_ios_IOSNative_gl3dUpdateShortBuffer___long_short_1ARRAY_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG bufferPeer, JAVA_OBJECT data, JAVA_INT indexCount) {}
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateTexture___int_1ARRAY_int_int_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT argb, JAVA_INT width, JAVA_INT height) { return 0; }
void com_codename1_impl_ios_IOSNative_gl3dDisposeBuffer___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG bufferPeer) {}
void com_codename1_impl_ios_IOSNative_gl3dDisposeTexture___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG texturePeer) {}
void com_codename1_impl_ios_IOSNative_gl3dDisposePipeline___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pipelinePeer) {}
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dGetOrCreatePipeline___long_java_lang_String_java_lang_String_int_int_int_int_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer,
        JAVA_OBJECT key, JAVA_OBJECT mslSource, JAVA_INT blendMode, JAVA_INT cullMode,
        JAVA_INT depthTest, JAVA_INT depthWrite) { return 0; }
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dGetOrCreatePipeline___long_java_lang_String_java_lang_String_int_int_int_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer,
        JAVA_OBJECT key, JAVA_OBJECT mslSource, JAVA_INT blendMode, JAVA_INT cullMode,
        JAVA_INT depthTest, JAVA_INT depthWrite) { return 0; }
void com_codename1_impl_ios_IOSNative_gl3dClear___long_int_boolean_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer,
        JAVA_INT argbColor, JAVA_BOOLEAN clearColor, JAVA_BOOLEAN clearDepth) {}
void com_codename1_impl_ios_IOSNative_gl3dSetViewport___long_int_int_int_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer,
        JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {}
void com_codename1_impl_ios_IOSNative_gl3dDrawIndexed___long_long_long_int_long_int_int_float_1ARRAY_int_long_int_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer,
        JAVA_LONG pipelinePeer, JAVA_LONG vboPeer, JAVA_INT strideBytes, JAVA_LONG iboPeer,
        JAVA_INT indexCount, JAVA_INT primitive, JAVA_OBJECT uniforms, JAVA_INT uniformFloats,
        JAVA_LONG texturePeer, JAVA_INT texFilter, JAVA_INT texWrap) {}
void com_codename1_impl_ios_IOSNative_gl3dDrawArrays___long_long_long_int_int_int_float_1ARRAY_int_long_int_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer,
        JAVA_LONG pipelinePeer, JAVA_LONG vboPeer, JAVA_INT strideBytes, JAVA_INT vertexCount,
        JAVA_INT primitive, JAVA_OBJECT uniforms, JAVA_INT uniformFloats,
        JAVA_LONG texturePeer, JAVA_INT texFilter, JAVA_INT texWrap) {}

#endif /* CN1_USE_METAL */
