/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
#import "CN1ES2compat.h"
#ifdef CN1_USE_METAL
#import "CN1MetalPipelineCache.h"

@interface CN1MetalPipelineCache () {
    id<MTLDevice> _device;
    id<MTLRenderPipelineState> _states[CN1MetalPipelineCount];
}
@end

@implementation CN1MetalPipelineCache

- (instancetype)initWithDevice:(id<MTLDevice>)device {
    if ((self = [super init])) {
        _device = device;
        for (int i = 0; i < CN1MetalPipelineCount; i++) {
            _states[i] = nil;
        }
    }
    return self;
}

// Configures the standard color attachment: BGRA8Unorm to match
// CAMetalLayer.pixelFormat, premultiplied-alpha blending so the output
// of our shaders (which multiply color by alpha on the CPU for solid
// fills and use `texture * tint` for textured draws) composites correctly.
static void configureBlendPremultiplied(MTLRenderPipelineColorAttachmentDescriptor *a) {
    a.pixelFormat = MTLPixelFormatBGRA8Unorm;
    a.blendingEnabled = YES;
    a.rgbBlendOperation = MTLBlendOperationAdd;
    a.alphaBlendOperation = MTLBlendOperationAdd;
    a.sourceRGBBlendFactor = MTLBlendFactorOne;
    a.destinationRGBBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
    a.sourceAlphaBlendFactor = MTLBlendFactorOne;
    a.destinationAlphaBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
}

static void configureBlendDisabled(MTLRenderPipelineColorAttachmentDescriptor *a) {
    a.pixelFormat = MTLPixelFormatBGRA8Unorm;
    a.blendingEnabled = NO;
}

- (id<MTLRenderPipelineState>)buildPipeline:(CN1MetalPipeline)pipeline library:(id<MTLLibrary>)library {
    MTLRenderPipelineDescriptor *desc = [[MTLRenderPipelineDescriptor alloc] init];

    switch (pipeline) {
        case CN1MetalPipelineSolidColor:
            desc.vertexFunction = [library newFunctionWithName:@"cn1_vs_solid"];
            desc.fragmentFunction = [library newFunctionWithName:@"cn1_fs_solid"];
            configureBlendPremultiplied(desc.colorAttachments[0]);
            break;
        case CN1MetalPipelineTexturedRGBA:
            desc.vertexFunction = [library newFunctionWithName:@"cn1_vs_textured"];
            desc.fragmentFunction = [library newFunctionWithName:@"cn1_fs_textured"];
            configureBlendPremultiplied(desc.colorAttachments[0]);
            break;
        case CN1MetalPipelineAlphaMask:
            desc.vertexFunction = [library newFunctionWithName:@"cn1_vs_textured"];
            desc.fragmentFunction = [library newFunctionWithName:@"cn1_fs_alpha_mask"];
            configureBlendPremultiplied(desc.colorAttachments[0]);
            break;
        case CN1MetalPipelineClearPunch:
            desc.vertexFunction = [library newFunctionWithName:@"cn1_vs_solid"];
            desc.fragmentFunction = [library newFunctionWithName:@"cn1_fs_clear"];
            configureBlendDisabled(desc.colorAttachments[0]);
            break;
        case CN1MetalPipelineAlphaMaskRadial:
            desc.vertexFunction = [library newFunctionWithName:@"cn1_vs_textured"];
            desc.fragmentFunction = [library newFunctionWithName:@"cn1_fs_alpha_mask_radial"];
            configureBlendPremultiplied(desc.colorAttachments[0]);
            break;
        case CN1MetalPipelineLinearGradient:
            // Pure GPU linear gradient -- vertex stage feeds per-corner 0..1
            // texcoords; fragment lerps startColor->endColor along whichever
            // axis the caller picks. Replaces the CG-rasterise + upload
            // path the iOS Metal port had carried over from Phase 2.
            desc.vertexFunction = [library newFunctionWithName:@"cn1_vs_textured"];
            desc.fragmentFunction = [library newFunctionWithName:@"cn1_fs_linear_gradient"];
            configureBlendPremultiplied(desc.colorAttachments[0]);
            break;
        case CN1MetalPipelineRadialGradient:
            // Pure GPU radial gradient -- same vertex stage as the linear
            // variant. Replaces CGContextDrawRadialGradient + bitmap upload.
            desc.vertexFunction = [library newFunctionWithName:@"cn1_vs_textured"];
            desc.fragmentFunction = [library newFunctionWithName:@"cn1_fs_radial_gradient"];
            configureBlendPremultiplied(desc.colorAttachments[0]);
            break;
        default:
            return nil;
    }
    if (desc.vertexFunction == nil || desc.fragmentFunction == nil) {
        NSLog(@"CN1MetalPipelineCache: shader function missing for pipeline %ld", (long)pipeline);
        return nil;
    }
    NSError *err = nil;
    id<MTLRenderPipelineState> state = [_device newRenderPipelineStateWithDescriptor:desc error:&err];
    if (state == nil) {
        NSLog(@"CN1MetalPipelineCache: failed to create pipeline %ld: %@", (long)pipeline, err);
    }
    return state;
}

- (id<MTLRenderPipelineState>)pipelineFor:(CN1MetalPipeline)pipeline {
    if (pipeline < 0 || pipeline >= CN1MetalPipelineCount) return nil;
    if (_states[pipeline] != nil) return _states[pipeline];

    id<MTLLibrary> library = [_device newDefaultLibrary];
    if (library == nil) {
        NSLog(@"CN1MetalPipelineCache: device has no default.metallib — is CN1MetalShaders.metal in the Xcode project?");
        return nil;
    }
    _states[pipeline] = [self buildPipeline:pipeline library:library];
    return _states[pipeline];
}

@end

#endif /* CN1_USE_METAL */
