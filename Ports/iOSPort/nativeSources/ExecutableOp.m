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
#import "ExecutableOp.h"
#include "xmlvm.h"
#import "CodenameOne_GLViewController.h"
#import <mach/mach.h>
#import <mach/mach_host.h>


#ifndef CN1_USE_METAL
extern void logGlErrorAt(const char *f, int l) {
    GLenum err = glGetError();
    if(err != GL_NO_ERROR) {
        switch(err) {
            case GL_INVALID_ENUM:
                CN1Log(@"GL Error at %@:%i - GL_INVALID_ENUM", [NSString stringWithUTF8String:f], l);
                break;
            case GL_INVALID_VALUE:
                CN1Log(@"GL Error at %@:%i - GL_INVALID_VALUE", [NSString stringWithUTF8String:f], l);
                break;
            case GL_INVALID_OPERATION:
                CN1Log(@"GL Error at %@:%i - GL_INVALID_OPERATION", [NSString stringWithUTF8String:f], l);
                break;
            default:
                CN1Log(@"GL Error at %@:%i - %i", [NSString stringWithUTF8String:f], l, err);
                break;
        }
    }
}
#else
extern void logGlErrorAt(const char *f, int l) {
    // No-op for Metal builds - use Metal validation layers instead
}
#endif

@implementation ExecutableOp
static BOOL blockDrawing = NO;
-(void)clipBlock:(BOOL)b {
    blockDrawing = b;
}

-(void)executeWithClipping {
    if(blockDrawing) {
        return;
    }
    [self execute];
}

-(void)execute {
}

#ifndef CN1_USE_ARC
-(void)dealloc {
	[super dealloc];
}
#endif

+(natural_t) get_free_memory {
    mach_port_t host_port;
    mach_msg_type_number_t host_size;
    vm_size_t pagesize;
    host_port = mach_host_self();
    host_size = sizeof(vm_statistics_data_t) / sizeof(integer_t);
    host_page_size(host_port, &pagesize);
    vm_statistics_data_t vm_stat;
    if (host_statistics(host_port, HOST_VM_INFO, (host_info_t)&vm_stat, &host_size) != KERN_SUCCESS) {
        CN1Log(@"Failed to fetch vm statistics");
        return 0;
    }
    /* Stats in bytes */
    natural_t mem_free = vm_stat.free_count * pagesize;
    return mem_free;
}

+(natural_t) get_memory_in_use {
    struct task_basic_info info;
    mach_msg_type_number_t size = sizeof(info);
    kern_return_t kerr = task_info(mach_task_self(),
                                   TASK_BASIC_INFO,
                                   (task_info_t)&info,
                                   &size);
    if( kerr == KERN_SUCCESS ) {
        return info.resident_size;
    } else {
        CN1Log(@"Error with task_info(): %s", mach_error_string(kerr));
    }
    return 0;
}


-(void)executeWithLog {
    if(blockDrawing) {
        CN1Log(@"%@ was blocked due to clipping", [self getName]);
        return;
    }
    natural_t mem = [ExecutableOp get_free_memory];
    CN1Log(@"%@ starting total memory %i bytes", [self getName], mem);
    NSDate *start = [NSDate date];
    [self executeWithClipping];
    NSDate *finish = [NSDate date];
    NSTimeInterval t = [finish timeIntervalSinceDate:start];
    CN1Log(@"%@ took %f seconds, and %i bytes", [self getName], t, [ExecutableOp get_free_memory] - mem);
}

-(NSString*)getName {
    return nil;
}

#ifdef CN1_USE_METAL
// Metal helper method implementations
#import "METALView.h"
#import "CN1METALTransform.h"

-(id<MTLDevice>)device {
    METALView *metalView = [[CodenameOne_GLViewController instance] metalView];
    return metalView.device;
}

-(id<MTLRenderCommandEncoder>)makeRenderCommandEncoder {
    METALView *metalView = [[CodenameOne_GLViewController instance] metalView];
    return [metalView makeRenderCommandEncoder];
}

-(void)applyClip:(id<MTLRenderCommandEncoder>)encoder {
    // TODO: Implement clipping via scissor rectangle or stencil buffer
    // For now, no clipping is applied
    // This will be implemented when ClipRect ExecutableOp is done
}

-(simd_float4x4)getMVPMatrix {
    simd_float4x4 mvp = CN1_Metal_GetMVPMatrix();
    return mvp;
}

-(simd_float4)colorToFloat4:(int)color alpha:(int)alpha {
    float alph = ((float)alpha) / 255.0;
    return simd_make_float4(
        ((float)((color >> 16) & 0xff)) / 255.0 * alph,
        ((float)((color >> 8) & 0xff)) / 255.0 * alph,
        ((float)(color & 0xff)) / 255.0 * alph,
        alph
    );
}

#endif

@end
