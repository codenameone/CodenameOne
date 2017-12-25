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
#import "DrawGradientTextureCache.h"
#import "ExecutableOp.h"
#include "xmlvm.h"

@implementation DrawGradientTextureCache

static NSMutableArray* cachedGradients = nil;
static NSMutableArray* pendingDeleteGradients = nil;
static int MAX_CACHE_SIZE = 5;

-(id)initWithGradient:(int)typeA startColorA:(int)startColorA endColorA:(int)endColorA widthA:(int)widthA heightA:(int)heightA relativeXA:(float)relativeXA relativeYA:(float)relativeYA relativeSizeA:(float)relativeSizeA tA:(GLuint)tA {
#ifndef CN1_USE_ARC
    lastAccess = [[NSDate date] retain];
#endif
    textureName = tA;
    type = typeA;
    startColor = startColorA;
    endColor = endColorA;
    width = widthA;
    height = heightA;
    relativeX = relativeXA;
    relativeY = relativeYA;
    relativeSize = relativeSizeA;
    return self;
}

+(void)flushDeleted {
    [pendingDeleteGradients removeAllObjects];
}

+(void)cache:(int)typeA startColorA:(int)startColorA endColorA:(int)endColorA widthA:(int)widthA heightA:(int)heightA relativeXA:(float)relativeXA relativeYA:(float)relativeYA relativeSizeA:(float)relativeSizeA tA:(GLuint)tA {
    DrawGradientTextureCache* d = [[DrawGradientTextureCache alloc] initWithGradient:typeA startColorA:startColorA endColorA:endColorA widthA:widthA heightA:heightA relativeXA:relativeXA relativeYA:relativeYA relativeSizeA:relativeSizeA tA:tA];
    if(cachedGradients == nil) {
        cachedGradients = [[NSMutableArray alloc] init];
        pendingDeleteGradients = [[NSMutableArray alloc] init];
        [cachedGradients addObject:d];
#ifndef CN1_USE_ARC
        [d release];
#endif
        return;
    }
    if([cachedGradients count] < MAX_CACHE_SIZE) {
        [cachedGradients addObject:d];
#ifndef CN1_USE_ARC
        [d release];
#endif
    } else {
        // need to pick an element in the array to remove
        DrawGradientTextureCache* oldest = d;
        for(DrawGradientTextureCache* entry in cachedGradients) {
            if([oldest->lastAccess compare:entry->lastAccess] == NSOrderedDescending) {
                oldest = entry;
            }
        }
        [pendingDeleteGradients addObject:oldest];
        [cachedGradients removeObject:oldest];
        [cachedGradients addObject:d];
#ifndef CN1_USE_ARC
        [d release];
#endif
    }
}

-(BOOL)isEqual:(id)object {
    if(object == self) {
        return YES;
    }
    if(!object || ![object isKindOfClass:[self class]]) {
        return NO;
    }
    DrawGradientTextureCache* o = (DrawGradientTextureCache*)object;
    return type == o->type &&
        startColor == o->startColor &&
        endColor == o->endColor &&
        width == o->width &&
        height == o->height &&
        relativeX == o->relativeX &&
        relativeY == o->relativeY &&
        relativeSize == o->relativeSize;
}

+(GLuint)checkCache:(int)typeA startColorA:(int)startColorA endColorA:(int)endColorA widthA:(int)widthA heightA:(int)heightA relativeXA:(float)relativeXA relativeYA:(float)relativeYA relativeSizeA:(float)relativeSizeA {
    DrawGradientTextureCache* tmp = [[DrawGradientTextureCache alloc] initWithGradient:typeA startColorA:startColorA endColorA:endColorA widthA:widthA heightA:heightA relativeXA:relativeXA relativeYA:relativeYA relativeSizeA:relativeSizeA tA:0];
    for(DrawGradientTextureCache* d in cachedGradients) {
        if([tmp isEqual:d]) {
#ifndef CN1_USE_ARC
            [d->lastAccess release];
#endif
            d->lastAccess = [NSDate date];
#ifndef CN1_USE_ARC
            [d->lastAccess retain];
            [tmp release];
#endif
            //CN1Log(@"Gradient cache hit size %i!", [cachedGradients count]);
            return d->textureName;
        }
    }
    //CN1Log(@"Gradient cache miss size %i for typeA:%i startColorA:%i endColorA:%i widthA:%i heightA:%i relativeXA:%f relativeYA:%f relativeSizeA:%f!", [cachedGradients count], typeA, startColorA, endColorA, widthA, heightA, relativeXA, relativeYA, relativeSizeA);
#ifndef CN1_USE_ARC
    [tmp release];
#endif
    return 0;
}

-(void)dealloc {
#ifndef CN1_USE_ARC
    [lastAccess release];
#endif
    glDeleteTextures(1, &textureName);
    GLErrorLog;
#ifndef CN1_USE_ARC
    [super dealloc];
#endif
}

@end



