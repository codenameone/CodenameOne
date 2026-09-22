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
#import "xmlvm.h"
#import "CN1RenderBackend.h"
#import "DrawPath.h"
#import "CodenameOne_GLViewController.h"
#import "Renderer.h"
#import "Transformer.h"
#import "PathConsumer.h"
#import "AlphaConsumer.h"
#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"
#endif
#define min(a,b) ((a)<(b)?(a):(b))
#define max(a,b) ((a)>(b)?(a):(b))
#define abs(x) ((x)>0?(x):-(x))

@implementation DrawPath


-(id)initWithArgs:(Renderer*)r color:(int)c alpha:(int)a //x:(int)xx y:(int)yy w:(int)ww h:(int)hh
{
    color = c;
    alpha = a;
    renderer = r;
    return self;
}
-(void)execute
{
#if TARGET_OS_WATCH
    JAVA_INT wbounds[4];
    Renderer_getOutputBounds(renderer, (JAVA_INT*)&wbounds);
    JAVA_INT wx = min(wbounds[0], wbounds[2]);
    JAVA_INT wy = min(wbounds[1], wbounds[3]);
    JAVA_INT ww = wbounds[2]-wbounds[0];
    JAVA_INT wh = wbounds[3]-wbounds[1];
    if ( ww < 0 ) ww = -ww;
    if ( wh < 0 ) wh = -wh;
    if (ww == 0 || wh == 0) {
        return;
    }
    AlphaConsumer wac = { wx, wy, ww, wh };
    jbyte wmask[wac.width*wac.height];
    wac.alphas = (JAVA_BYTE*)&wmask;
    Renderer_produceAlphas(renderer, &wac);
    CN1CGFillAlphaMask(color, alpha, wx, wy, ww, wh, (const unsigned char*)wmask);
#elif defined(CN1_USE_METAL)
    // DrawPath was the alpha-mask path of the legacy backend; under Metal
    // shapes are rasterised via DrawTextureAlphaMask + CN1Metalcompat, so
    // there is nothing to do here.
#endif // CN1_USE_METAL
}
-(void)dealloc
{
    Renderer_destroy(renderer);
#ifndef CN1_USE_ARC
    [super dealloc];
#endif

}
@end
