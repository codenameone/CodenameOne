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
#import "CodenameOne_GLViewController.h"
#import <mach/mach.h>
#import <mach/mach_host.h>


extern void logGlErrorAt(const char *f, int l) {
    GLenum err = glGetError();
    if(err != GL_NO_ERROR) {
        switch(err) {
            case GL_INVALID_ENUM:
                NSLog(@"GL Error at %@:%i - GL_INVALID_ENUM", [NSString stringWithUTF8String:f], l);
                break;
            case GL_INVALID_VALUE:
                NSLog(@"GL Error at %@:%i - GL_INVALID_VALUE", [NSString stringWithUTF8String:f], l);
                break;
            case GL_INVALID_OPERATION:
                NSLog(@"GL Error at %@:%i - GL_INVALID_OPERATION", [NSString stringWithUTF8String:f], l);
                break;
            default:
                NSLog(@"GL Error at %@:%i - %i", [NSString stringWithUTF8String:f], l, err);
                break;
        }
    }
}

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

-(void)dealloc {
	[super dealloc];
}

+(natural_t) get_free_memory {
    mach_port_t host_port;
    mach_msg_type_number_t host_size;
    vm_size_t pagesize;
    host_port = mach_host_self();
    host_size = sizeof(vm_statistics_data_t) / sizeof(integer_t);
    host_page_size(host_port, &pagesize);
    vm_statistics_data_t vm_stat;
    if (host_statistics(host_port, HOST_VM_INFO, (host_info_t)&vm_stat, &host_size) != KERN_SUCCESS) {
        NSLog(@"Failed to fetch vm statistics");
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
        NSLog(@"Error with task_info(): %s", mach_error_string(kerr));
    }
    return 0;
}


-(void)executeWithLog {
    if(blockDrawing) {
        NSLog(@"%@ was blocked due to clipping", [self getName]);
        return;
    }
    natural_t mem = [ExecutableOp get_free_memory];
    NSLog(@"%@ starting total memory %i bytes", [self getName], mem);
    NSDate *start = [NSDate date];
    [self executeWithClipping];
    NSDate *finish = [NSDate date];
    NSTimeInterval t = [finish timeIntervalSinceDate:start];
    NSLog(@"%@ took %f seconds, and %i bytes", [self getName], t, [ExecutableOp get_free_memory] - mem);
}

-(NSString*)getName {
    return nil;
}

@end
