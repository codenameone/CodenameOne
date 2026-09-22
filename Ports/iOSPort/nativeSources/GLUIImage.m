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
#import "GLUIImage.h"
#import "CodenameOne_GLViewController.h"
#import "CN1AppleUI.h"
#include "xmlvm.h"
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#endif

extern int nextPowerOf2(int val);

@implementation GLUIImage
-(id)initWithImage:(CN1Image*)i {
    img = i;
    name = nil;
#ifndef CN1_USE_ARC
    [img retain];
#endif
    textureWidth = -1;
    textureHeight = -1;
    return self;
}

-(CN1Image*)getImage {
    return img;
}

-(int)getTextureWidth {
    return textureWidth;
}

-(int)getTextureHeight {
    return textureHeight;
}

-(void)setImage:(CN1Image*)i {
    if(img != nil) {
#ifndef CN1_USE_ARC
        [img release];
#endif
    }
    img = i;
#ifndef CN1_USE_ARC
    [img retain];
#endif
#ifdef CN1_USE_METAL
    // Invalidate the cached MTLTexture — it was built from the previous
    // CN1Image's pixels. The CN1MetalTextureFromUIImage assignment
    // transferred a +1 retain; release it explicitly so swapping the
    // backing CN1Image doesn't leak the old GPU texture.
    [mtlTexture release];
    mtlTexture = nil;
#endif
}

-(void)setName:(NSString*)s {
    name = s;
#ifndef CN1_USE_ARC
    [name retain];
#endif
}

#ifdef CN1_USE_METAL
-(id<MTLTexture>)getMTLTexture {
    // Phase 3 v2: a mutable-image render target, if present, is the freshest
    // pixel source. Screen-side DrawImage samples this; the cached CN1Image-
    // derived mtlTexture is only relevant for never-drawn-into images.
    if (mtlMutableTexture != nil) return mtlMutableTexture;
    if (mtlTexture != nil) {
        // issue #5349: the first time this cached read-only texture is sampled
        // after a foreground/resume (or a memory warning), re-decode it from the
        // retained CN1Image. iOS can discard a private-storage texture's contents
        // while the app is suspended, and the leftover bytes render as a
        // violet/magenta fill; re-decoding from the CPU-side CN1Image is cheap and
        // always correct. A plain generation compare is used (no OS purgeable
        // probe) -- setPurgeableState on a texture already referenced by an
        // in-flight command buffer trips Metal's commit-time validation.
        // Every peer recovers the same way now that the backing UIImage is kept:
        // a stale generation drops the texture and it is rebuilt from that image.
        // The no-backing-copy exemption that used to live here only made sense
        // while the image was being released, and it left such a peer returning
        // a texture the OS may already have discarded.
        //
        // Only PRIVATE storage is at risk, which is what the storageMode test
        // below is for. A buffer-backed shared-storage texture is ordinary
        // CPU-visible memory that iOS does not discard, so revalidating one costs
        // a full re-rasterise of the picture -- through CGContextDrawImage, for
        // every image, after every foreground or memory warning -- and buys
        // nothing.
        // A no-backing-copy image has nothing to re-decode FROM, and does not
        // need to: its Java owner bumps a generation on suspend and builds a
        // fresh image, peer and all, the next time the picture is asked for. So
        // the recovery below is both impossible and unnecessary for it.
        int gen = CN1MetalTextureValidateGeneration();
        if (mtlTextureGeneration != gen && !noBackingCopy) {
            mtlTextureGeneration = gen;
            [mtlTexture release];
            mtlTexture = nil;
        } else {
            mtlTextureGeneration = gen;
            return mtlTexture;
        }
    }
    if (img == nil) return nil;
    mtlTexture = CN1MetalTextureFromUIImage(img);
    mtlTextureGeneration = CN1MetalTextureValidateGeneration();
    // The UIImage is NOT released here, even for a no-backing-copy image.
    //
    // Uploading the texture does not make the backing object redundant: the
    // drawing operations read geometry straight off it -- DrawImage and
    // TileImage both take their source size from [img getImage].size -- so a nil
    // img gives them zero dimensions and they silently skip the draw. Dropping
    // it rendered whole screens blank on the iOS and watchOS suites.
    //
    // Saving that second copy needs the size (and anything else the operations
    // reach for) cached on the peer first, and every consumer moved onto it.
    // Until that is done the copy stays.
    if (noBackingCopy && mtlTexture != nil) {
        // The pixels are on the GPU now. Letting the UIImage go takes
        // CoreGraphics' decoded raster with it -- the second copy of this
        // picture -- and the Java side is the recovery path.
#ifndef CN1_USE_ARC
        [img release];
#endif
        img = nil;
    }
    // Track every GPU-backed image (not just mutable render targets) so the
    // suspend backup can drop/rebuild its texture too (issue #5349). The weak
    // registry drops the entry automatically on dealloc.
    CN1MetalRegisterMutableImage(self);
    return mtlTexture;
}

-(void)setNoBackingCopy:(BOOL)v {
    noBackingCopy = v;
}

-(void)dropReadOnlyCachedTexture {
    // issue #5349: release the cached read-only texture; getMTLTexture rebuilds
    // it from the retained CN1Image on next use. Bumping the generation match is
    // unnecessary -- a nil texture is unconditionally rebuilt.
    [mtlTexture release];
    mtlTexture = nil;
}

-(id<MTLTexture>)mtlMutableTexture { return mtlMutableTexture; }
-(void)setMtlMutableTexture:(id<MTLTexture>)t width:(int)w height:(int)h {
    // Retain new, release old. Under MRR direct ivar assignment doesn't
    // auto-retain; without this the new texture would be autoreleased
    // out from under us when the next pool drains. Same fix pattern as
    // CN1MetalGlyphAtlas (commit b9c5add52). Setting the same texture
    // again is rare in practice but the retain-then-release order is
    // safe regardless.
    [t retain];
    [mtlMutableTexture release];
    mtlMutableTexture = t;
    mtlMutableWidth = w;
    mtlMutableHeight = h;
    // Track live mutable images so their pixels can be backed up before the
    // app is suspended (issue #5153). Registering only on a non-nil texture
    // keeps the registry to images that actually own GPU content; the weak
    // table drops them automatically on dealloc.
    if (t != nil) {
        CN1MetalRegisterMutableImage(self);
    }
    // Stale cached read-only texture: future getMTLTexture should sample
    // mtlMutableTexture instead of the CN1Image-derived one. Release the
    // +1 retain transferred in by getMTLTexture's CN1MetalTextureFromUIImage
    // assignment; without this the read-only texture leaks.
    [mtlTexture release];
    mtlTexture = nil;
}
-(int)mtlMutableWidth { return mtlMutableWidth; }
-(int)mtlMutableHeight { return mtlMutableHeight; }
-(id<MTLCommandBuffer>)mtlMutableCommandBuffer { return mtlMutableCommandBuffer; }
-(void)setMtlMutableCommandBuffer:(id<MTLCommandBuffer>)cb {
    // [queue commandBuffer] returns an autoreleased object; without
    // retaining it here, the cb dangles after the next pool drain and
    // [cb commit] / [cb waitUntilCompleted] crash later. Same MRR
    // discipline as setMtlMutableTexture above.
    [cb retain];
    [mtlMutableCommandBuffer release];
    mtlMutableCommandBuffer = cb;
}
-(int)mtlMutableInitialARGB { return mtlMutableInitialARGB; }
-(void)setMtlMutableInitialARGB:(int)argb { mtlMutableInitialARGB = argb; }
#endif

-(void)dealloc {
    if(name != nil) {
        //CN1Log(@"Deleting image name %@", name);
#ifndef CN1_USE_ARC
        [name release];
#endif
    }
#ifdef CN1_USE_METAL
    // Both ivars hold a +1 MTLTexture retain (newTextureWithDescriptor /
    // CN1MetalTextureFromUIImage both return owned references). Without
    // these explicit releases under MRR every transient Metal-backed image
    // leaks a GPU texture: the animation/transition test suite creates
    // 7 mutable images per test × ~17 tests, and the simulator runs out
    // of Metal device memory mid-suite, hanging the next test.
    // Drop out of the suspend/resume backup registry (issue #5153). The weak
    // table would zero this slot on its own, but unregistering explicitly
    // keeps it tidy and avoids a stale slot lingering until the next compaction.
    CN1MetalUnregisterMutableImage(self);
    [mtlTexture release];               mtlTexture = nil;
    [mtlMutableTexture release];        mtlMutableTexture = nil;
    // Same +1 retain ownership rule for the cached command buffer (the
    // setter retains; dealloc must release).
    [mtlMutableCommandBuffer release];  mtlMutableCommandBuffer = nil;
#endif
#ifndef CN1_USE_ARC
    [img release];
    [super dealloc];
#endif
}
@end
