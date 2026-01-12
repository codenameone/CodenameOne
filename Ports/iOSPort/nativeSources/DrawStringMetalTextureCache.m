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
#import "DrawStringMetalTextureCache.h"

#ifdef CN1_USE_METAL

#import "ExecutableOp.h"
#include "xmlvm.h"

@implementation DrawStringMetalTextureCache

static int MAX_CACHE_SIZE = 100;

-(id)initWithString:(NSString*)s f:(UIFont*)f t:(id<MTLTexture>)t c:(int)c a:(int)a {
    stringWidth = -1;
    str = s;
    font = f;
#ifndef CN1_USE_ARC
    [str retain];
    [font retain];
    lastAccess = [[NSDate date] retain];
    [t retain];
#else
    lastAccess = [NSDate date];
#endif
    texture = t;
    color = c;
    alpha = a;
    if(UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
        MAX_CACHE_SIZE = 200;
    }
    return self;
}

static NSMutableArray* cachedStrings = nil;
static NSMutableArray* pendingDeleteStrings = nil;

-(int)stringWidth {
    if (stringWidth == -1) {
        stringWidth = [str sizeWithAttributes:@{NSFontAttributeName: font}].width;
    }
    return stringWidth;
}

-(id<MTLTexture>)texture {
    return texture;
}

+(void)cache:(NSString*)s f:(UIFont*)f t:(id<MTLTexture>)t c:(int)c a:(int)a {
    DrawStringMetalTextureCache* d = [[DrawStringMetalTextureCache alloc] initWithString:s f:f t:t c:c a:a];
    if(cachedStrings == nil) {
        cachedStrings = [[NSMutableArray alloc] init];
        [cachedStrings addObject:d];
        pendingDeleteStrings = [[NSMutableArray alloc] init];
#ifndef CN1_USE_ARC
        [d release];
#endif
        return;
    }
    if([cachedStrings count] < MAX_CACHE_SIZE) {
        [cachedStrings addObject:d];
#ifndef CN1_USE_ARC
        [d release];
#endif
    } else {
        // need to pick an element in the array to remove
        DrawStringMetalTextureCache* oldest = d;
        for(DrawStringMetalTextureCache* entry in cachedStrings) {
            if([oldest->lastAccess compare:entry->lastAccess] == NSOrderedDescending) {
                oldest = entry;
            }
        }
        [pendingDeleteStrings addObject:oldest];
        [cachedStrings removeObject:oldest];
        [cachedStrings addObject:d];
#ifndef CN1_USE_ARC
        [d release];
#endif
    }
}

+(void)flushDeleted {
    [pendingDeleteStrings removeAllObjects];
}

-(BOOL)isEqual:(id)object {
    if(object == self) {
        return YES;
    }
    DrawStringMetalTextureCache* o = (DrawStringMetalTextureCache*)object;
    return color == o->color &&
    alpha == o->alpha && [str isEqualToString:o->str] &&
    [font isEqual:o->font];
}

+(DrawStringMetalTextureCache*)checkCache:(NSString*)s f:(UIFont*)f c:(int)c a:(int)a {
    DrawStringMetalTextureCache* tmp = [[DrawStringMetalTextureCache alloc] initWithString:s f:f t:nil c:c a:a];
    for(DrawStringMetalTextureCache* d in cachedStrings) {
        if([tmp isEqual:d]) {
#ifndef CN1_USE_ARC
            [d->lastAccess release];
#endif
            d->lastAccess = [NSDate date];
#ifndef CN1_USE_ARC
            [d->lastAccess retain];
            [tmp release];
#endif
            return d;
        }
    }
#ifndef CN1_USE_ARC
    [tmp release];
#endif
    return nil;
}

#ifndef CN1_USE_ARC
-(void)dealloc {
    [str release];
    [font release];
    [lastAccess release];
    [texture release];
    [super dealloc];
}
#endif

@end

#endif
