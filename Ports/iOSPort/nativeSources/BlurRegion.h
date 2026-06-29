/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
#import <Foundation/Foundation.h>
#import "ExecutableOp.h"

// Queued op for CSS backdrop-filter:blur on the LIVE screen (real "Liquid
// Glass"). Enqueued in paint order; during the drain (after the backdrop ops,
// before the glass component's foreground) it asks the METALView to blur the
// already-drawn screenTexture region and draw it back. Screen-only (target nil).
@interface BlurRegion : ExecutableOp {
    int x;
    int y;
    int width;
    int height;
    float radius;
    // When glass is YES this op runs the full Liquid Glass material recipe
    // (glassScreenRegionX) carrying these params instead of a plain blur.
    BOOL glass;
    float cornerRadius;
    float sat;
    float scale;
    float offset;
    float refract;
    float specular;
}

-(id)initWithArgs:(int)xpos ypos:(int)ypos w:(int)w h:(int)h r:(float)r;
-(id)initWithGlassArgs:(int)xpos ypos:(int)ypos w:(int)w h:(int)h r:(float)r
          cornerRadius:(float)cr sat:(float)st scale:(float)sc offset:(float)of
               refract:(float)rf specular:(float)sp;

@end
