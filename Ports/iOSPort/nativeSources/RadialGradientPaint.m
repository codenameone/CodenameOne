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
#import "RadialGradientPaint.h"
#import "CodenameOne_GLViewController.h"
#include "xmlvm.h"
#ifdef USE_ES2
#import "SetTransform.h"
#endif


@implementation RadialGradientPaint

@synthesize startColor;
@synthesize endColor;
@synthesize x;
@synthesize y;
@synthesize width;
@synthesize height;

-(id)initClear {
    clear = YES;
    return self;
}

-(id)initWithArgs:(int)xx y:(int)yy width:(int)w height:(int)h startColor:(int)sc endColor:(int)ec
{
    x=xx;
    y=yy;
    width=w;
    height=h;
    startColor=sc;
    endColor=ec;
    return self;
}
-(void)execute 
{
    if (clear) {
        [PaintOp setCurrent:NULL];
    } else {
        [PaintOp setCurrent:self];
    }
    
}

-(NSString*)getName {
    return @"RadialGradientPaint";
}


@end
