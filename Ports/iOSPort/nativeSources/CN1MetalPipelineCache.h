/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
#ifndef CN1MetalPipelineCache_h
#define CN1MetalPipelineCache_h

#import "CN1ES2compat.h"
#ifdef CN1_USE_METAL
#import <Foundation/Foundation.h>
@import Metal;
#import "CN1Metalcompat.h"

// Caches one MTLRenderPipelineState per CN1MetalPipeline variant.
// Built lazily on first use from the default.metallib that Xcode produces
// from CN1MetalShaders.metal.
@interface CN1MetalPipelineCache : NSObject

- (instancetype)initWithDevice:(id<MTLDevice>)device;
- (id<MTLRenderPipelineState>)pipelineFor:(CN1MetalPipeline)pipeline;

@end

#endif /* CN1_USE_METAL */
#endif /* CN1MetalPipelineCache_h */
