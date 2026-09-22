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
#ifndef CN1MetalPipelineCache_h
#define CN1MetalPipelineCache_h

#import "CN1RenderBackend.h"
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
