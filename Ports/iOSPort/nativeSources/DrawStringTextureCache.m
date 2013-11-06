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
#import "DrawStringTextureCache.h"
#import "ExecutableOp.h"


@implementation DrawStringTextureCache
static int MAX_CACHE_SIZE = 100;
-(id)initWithString:(NSString*)s f:(UIFont*)f t:(GLuint)t c:(int)c a:(int)a {
    str = s;
    [str retain];
    lastAccess = [[NSDate date] retain];
    font = f;
    [font retain];
    textureName = t;
    color = c;
    alpha = a;
    if(UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
        MAX_CACHE_SIZE = 200;
    }
    return self;
}

static NSMutableArray* cachedStrings = nil;
static NSMutableArray* pendingDeleteStrings = nil;

+(void)cache:(NSString*)s f:(UIFont*)f t:(GLuint)t c:(int)c a:(int)a {
    DrawStringTextureCache* d = [[DrawStringTextureCache alloc] initWithString:s f:f t:t c:c a:a];
    if(cachedStrings == nil) {
        cachedStrings = [[NSMutableArray alloc] init];
        [cachedStrings addObject:d];
        pendingDeleteStrings = [[NSMutableArray alloc] init];
        [d release];
        return;
    }
    if([cachedStrings count] < MAX_CACHE_SIZE) {
        [cachedStrings addObject:d];
        [d release];
    } else {
        // need to pick an element in the array to remove
        DrawStringTextureCache* oldest = d;
        for(DrawStringTextureCache* entry in cachedStrings) {
            if([oldest->lastAccess compare:entry->lastAccess] == NSOrderedDescending) {
                oldest = entry;
            }
        }
        [pendingDeleteStrings addObject:oldest];
        [cachedStrings removeObject:oldest];
        [cachedStrings addObject:d];
        [d release];
    }
}

+(void)flushDeleted {
    [pendingDeleteStrings removeAllObjects];
}

-(BOOL)isEqual:(id)object {
    if(object == self) {
        return YES;
    }
    DrawStringTextureCache* o = (DrawStringTextureCache*)object;
    return color == o->color &&
    alpha == o->alpha && [str isEqualToString:o->str] &&
    [font isEqual:o->font];
}

+(GLuint)checkCache:(NSString*)s f:(UIFont*)f c:(int)c a:(int)a {
    DrawStringTextureCache* tmp = [[DrawStringTextureCache alloc] initWithString:s f:f t:0 c:c a:a];
    for(DrawStringTextureCache* d in cachedStrings) {
        if([tmp isEqual:d]) {
            [d->lastAccess release];
            d->lastAccess = [NSDate date];
            [d->lastAccess retain];
            [tmp release];
            return d->textureName;
        }
    }
    [tmp release];
    return 0;
}

-(void)dealloc {
    [str release];
    [font release];
    [lastAccess release];
    glDeleteTextures(1, &textureName);
    GLErrorLog;
    [super dealloc];
}

@end
