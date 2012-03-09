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

@implementation DrawGradientTextureCache

static NSMutableArray* cachedGradients = nil;
static NSMutableArray* pendingDeleteGradients = nil;
static int MAX_CACHE_SIZE = 5;

-(id)initWithGradient:(int)typeA startColorA:(int)startColorA endColorA:(int)endColorA widthA:(int)widthA heightA:(int)heightA relativeXA:(float)relativeXA relativeYA:(float)relativeYA relativeSizeA:(float)relativeSizeA tA:(GLuint)tA {
    lastAccess = [[NSDate date] retain];
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
        [d release];
        return;
    }
    if([cachedGradients count] < MAX_CACHE_SIZE) {
        [cachedGradients addObject:d];
        [d release];
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
        [d release];
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
            [d->lastAccess release];
            d->lastAccess = [NSDate date];
            [d->lastAccess retain];
            [tmp release];
            //NSLog(@"Gradient cache hit size %i!", [cachedGradients count]);
            return d->textureName;
        }
    }
    //NSLog(@"Gradient cache miss size %i for typeA:%i startColorA:%i endColorA:%i widthA:%i heightA:%i relativeXA:%f relativeYA:%f relativeSizeA:%f!", [cachedGradients count], typeA, startColorA, endColorA, widthA, heightA, relativeXA, relativeYA, relativeSizeA);
    [tmp release];
    return 0;
}

-(void)dealloc {
    glDeleteTextures(1, &textureName);
    GLErrorLog;
    [super dealloc];
}

@end



