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
#import "CN1ES2compat.h"
#ifdef CN1_USE_METAL
#import <QuartzCore/QuartzCore.h>
@import Metal;
@import simd;
@import CoreImage;

#import "METALView.h"
#import "CN1Metalcompat.h"
#import "ExecutableOp.h"
#import "CodenameOne_GLViewController.h"
#include "com_codename1_impl_ios_IOSImplementation.h"
#include "xmlvm.h"
#include "com_codename1_impl_ios_TextEditUtil.h"

static BOOL firstTime=YES;
extern float scaleValue;
extern void stringEdit(int finished, int cursorPos, NSString* text);
extern UIView *editingComponent;
extern BOOL isVKBAlwaysOpen();
extern void repaintUI();

#include <math.h>

// ---------------------------------------------------------------------------
// Live-screen "Liquid Glass" material helpers. Faithful C ports of the proven
// offscreen recipe in IOSImplementation (glassMaterialInPlace, sampleBilinear,
// applyGlassOptics) so the running app produces the SAME glass as the fidelity
// tiles. The whole pipeline runs in one top-down ARGB integer buffer (no
// CIImage/CG round-trip) to avoid orientation ambiguity; glassScreenRegionX
// below ties them together against the live screenTexture.
// ---------------------------------------------------------------------------

// One separable box-blur iteration (horizontal then vertical) of the given
// radius via a sliding running-sum: O(w*h) REGARDLESS of radius, edge-clamped.
static void glassBoxBlurOnce(uint32_t *buf, uint32_t *tmp, int w, int h, int r) {
    if (r < 1) { return; }
    float norm = 1.0f / (float)(2 * r + 1);
    for (int y = 0; y < h; y++) {
        uint32_t *row = buf + (size_t)y * w;
        uint32_t *trow = tmp + (size_t)y * w;
        int sr = 0, sg = 0, sb = 0;
        for (int k = -r; k <= r; k++) {
            int xx = k < 0 ? 0 : (k >= w ? w - 1 : k);
            uint32_t p = row[xx]; sr += (p >> 16) & 0xff; sg += (p >> 8) & 0xff; sb += p & 0xff;
        }
        for (int x = 0; x < w; x++) {
            trow[x] = 0xff000000u | ((uint32_t)(int)(sr * norm + 0.5f) << 16) | ((uint32_t)(int)(sg * norm + 0.5f) << 8) | (uint32_t)(int)(sb * norm + 0.5f);
            int xo = x - r; if (xo < 0) xo = 0;
            int xi = x + r + 1; if (xi >= w) xi = w - 1;
            uint32_t po = row[xo], pi = row[xi];
            sr += (int)((pi >> 16) & 0xff) - (int)((po >> 16) & 0xff);
            sg += (int)((pi >> 8) & 0xff) - (int)((po >> 8) & 0xff);
            sb += (int)(pi & 0xff) - (int)(po & 0xff);
        }
    }
    for (int x = 0; x < w; x++) {
        int sr = 0, sg = 0, sb = 0;
        for (int k = -r; k <= r; k++) {
            int yy = k < 0 ? 0 : (k >= h ? h - 1 : k);
            uint32_t p = tmp[(size_t)yy * w + x]; sr += (p >> 16) & 0xff; sg += (p >> 8) & 0xff; sb += p & 0xff;
        }
        for (int y = 0; y < h; y++) {
            buf[(size_t)y * w + x] = 0xff000000u | ((uint32_t)(int)(sr * norm + 0.5f) << 16) | ((uint32_t)(int)(sg * norm + 0.5f) << 8) | (uint32_t)(int)(sb * norm + 0.5f);
            int yo = y - r; if (yo < 0) yo = 0;
            int yi = y + r + 1; if (yi >= h) yi = h - 1;
            uint32_t po = tmp[(size_t)yo * w + x], pi = tmp[(size_t)yi * w + x];
            sr += (int)((pi >> 16) & 0xff) - (int)((po >> 16) & 0xff);
            sg += (int)((pi >> 8) & 0xff) - (int)((po >> 8) & 0xff);
            sb += (int)(pi & 0xff) - (int)(po & 0xff);
        }
    }
}

// Triple box blur ~ Gaussian of sigma ~= radius (Jarosz). RADIUS-INDEPENDENT cost
// so the large (radius ~64px) nav/tab bar glass blurs stay cheap -- a true
// Gaussian kernel here was hundreds of ms per call and timed the suite out.
// Edge-clamped, in place. Alpha assumed opaque (backdrop) and kept at 0xff.
static void glassGaussianBlur(uint32_t *buf, int w, int h, float radius) {
    if (radius < 0.75f || w <= 0 || h <= 0) { return; }
    int r = (int)(radius + 0.5f);
    if (r < 1) { r = 1; }
    uint32_t *tmp = (uint32_t *)malloc((size_t)w * (size_t)h * 4);
    if (tmp == NULL) { return; }
    glassBoxBlurOnce(buf, tmp, w, h, r);
    glassBoxBlurOnce(buf, tmp, w, h, r);
    glassBoxBlurOnce(buf, tmp, w, h, r);
    free(tmp);
}

static inline int glassBilerp(int c00, int c10, int c01, int c11, float tx, float ty) {
    float top = c00 + (c10 - c00) * tx;
    float bot = c01 + (c11 - c01) * tx;
    return (int)(top + (bot - top) * ty + 0.5f);
}

static uint32_t glassSampleBilinear(uint32_t *buf, int w, int h, float fx, float fy) {
    if (fx < 0.0f) fx = 0.0f; else if (fx > w - 1) fx = w - 1;
    if (fy < 0.0f) fy = 0.0f; else if (fy > h - 1) fy = h - 1;
    int x0 = (int)fx, y0 = (int)fy;
    int x1 = x0 + 1 < w ? x0 + 1 : x0, y1 = y0 + 1 < h ? y0 + 1 : y0;
    float tx = fx - x0, ty = fy - y0;
    uint32_t p00 = buf[(size_t)y0 * w + x0], p10 = buf[(size_t)y0 * w + x1];
    uint32_t p01 = buf[(size_t)y1 * w + x0], p11 = buf[(size_t)y1 * w + x1];
    int r = glassBilerp((p00 >> 16) & 0xff, (p10 >> 16) & 0xff, (p01 >> 16) & 0xff, (p11 >> 16) & 0xff, tx, ty);
    int g = glassBilerp((p00 >> 8) & 0xff, (p10 >> 8) & 0xff, (p01 >> 8) & 0xff, (p11 >> 8) & 0xff, tx, ty);
    int b = glassBilerp(p00 & 0xff, p10 & 0xff, p01 & 0xff, p11 & 0xff, tx, ty);
    return ((uint32_t)r << 16) | ((uint32_t)g << 8) | (uint32_t)b;
}

// Rounded-rect SDF mask + edge refraction + specular rim. Reads the blurred
// padded buffer src (component at offset (pad,pad)), writes a PREMULTIPLIED
// ARGB patch (rw x rh) with transparent corners. s = contentScaleFactor (logical
// lengths -- cornerRadius, rim width -- scale to physical px). cornerRadius < 0
// means capsule.
static void glassApplyOptics(uint32_t *src, int bw, int bh, int pad, uint32_t *out,
        int rw, int rh, float cornerRadius, float refract, float specular, float s) {
    float hw = rw / 2.0f, hh = rh / 2.0f;
    float minhh = hw < hh ? hw : hh;
    float r;
    if (cornerRadius < 0.0f) { r = minhh; }
    else { r = cornerRadius * s; if (r > minhh) r = minhh; }
    if (r < 0.0f) r = 0.0f;
    float band = minhh * 0.6f;
    float rimW = 3.0f * s;
    for (int y = 0; y < rh; y++) {
        float py = y + 0.5f;
        for (int x = 0; x < rw; x++) {
            float px = x + 0.5f;
            float dx = fabsf(px - hw) - (hw - r);
            float dy = fabsf(py - hh) - (hh - r);
            float axx = dx > 0 ? dx : 0, ayy = dy > 0 ? dy : 0;
            float outside = sqrtf(axx * axx + ayy * ayy);
            float mxv = dx > dy ? dx : dy;
            float inside = mxv < 0 ? mxv : 0;
            float sdf = outside + inside - r;
            float depth = -sdf;
            if (depth <= 0.0f) { out[(size_t)y * rw + x] = 0; continue; }
            float alpha = depth >= 1.0f ? 1.0f : depth;
            // Bottom-edge feather for rectangular chrome bars (Toolbar/TitleArea,
            // cornerRadius == 0): a native nav bar's glass fades into the content
            // below instead of stopping at a hard rectangular edge. Ramp the glass
            // alpha down over the bottom ~22% so the blurred bar blends into the
            // sharp backdrop beneath it. Capsules (-1) and rounded panels (>0) keep
            // their crisp shape (unaffected).
            if (cornerRadius == 0.0f) {
                float fb = rh * 0.22f;
                if (fb > 1.0f && py > rh - fb) {
                    float fade = (rh - py) / fb;
                    if (fade < 0.0f) fade = 0.0f;
                    alpha *= fade;
                }
            }
            float sx = x, sy = y;
            if (refract > 0.0f && band > 0.0f && depth < band) {
                float t = 1.0f - depth / band;
                float distortion = 1.0f - sqrtf(fmaxf(0.0f, 1.0f - t * t));
                sx = x - (px - hw) * distortion * refract;
                sy = y - (py - hh) * distortion * refract;
            }
            uint32_t col = glassSampleBilinear(src, bw, bh, sx + pad, sy + pad);
            int rr = (col >> 16) & 0xff, gg = (col >> 8) & 0xff, bb = col & 0xff;
            if (specular > 0.0f && depth < rimW) {
                float rim = 1.0f - depth / rimW;
                float topBias = 0.55f + 0.45f * (1.0f - py / rh);
                int add = (int)(specular * rim * topBias * 70.0f);
                rr = rr + add > 255 ? 255 : rr + add;
                gg = gg + add > 255 ? 255 : gg + add;
                bb = bb + add > 255 ? 255 : bb + add;
            }
            int a = (int)(alpha * 255.0f);
            int pr = rr * a / 255, pg = gg * a / 255, pb = bb * a / 255;
            out[(size_t)y * rw + x] = ((uint32_t)a << 24) | ((uint32_t)pr << 16) | ((uint32_t)pg << 8) | (uint32_t)pb;
        }
    }
}

// CPU REFERENCE for the iOS-26 selection-drop lens, kept in sync with
// JavaSEPort.applyLensBuffer and the cn1_fs_lens Metal shader (the live device path is
// the GPU shader -- see lensScreenRegionX -- so this is no longer called; it is retained
// as the readable reference for the optics and for the host-side numeric cross-check).
// Superellipse (rounded-rect / capsule when cornerRadius<0) AA mask; PREMULTIPLIED out.
static inline float glassSmoothstep(float a, float b, float x) {
    float t = (x - a) / (b - a);
    if (t < 0.0f) t = 0.0f; else if (t > 1.0f) t = 1.0f;
    return t * t * (3.0f - 2.0f * t);
}

// Lens optics constants -- MUST stay in sync with JavaSEPort.applyLensBuffer so the
// simulator and the device render the identical drop. See that method for the
// per-effect rationale.
#define LENS_MAG_FLAT       0.75f   // uniform-magnify fraction of the (elliptical) radius
#define LENS_TINT_HI        150.0f  // luminance >= HI: no tint
#define LENS_TINT_LO        55.0f   // luminance <= LO: full dark->accent tint
#define LENS_LIFT_COEF      0.40f   // upward pull of the content under the drop
#define LENS_GLARE          0.09f   // specular sheen strength
#define LENS_RIM            0.06f   // edge-rim brightness
#define LENS_RIM_W          0.06f   // rim band width (fraction of half-height)
#define LENS_REFRACT        0.16f   // edge lensing: content bends at the rim
#define LENS_EDGE_SHADOW    0.12f   // soft dark band just inside the rim
#define LENS_RIM_SCALE      0.84f   // periphery shrinks (< 1) while the centre enlarges
#define LENS_GLASS_TINT     0xbcd8ff /* faint cool-blue cast through the whole glass */
#define LENS_GLASS_TINT_STR 0.10f
#define LENS_SAT_BOOST      1.32f   // push the tinted blue glyph more vivid

__attribute__((unused))
static void glassApplyLens(uint32_t *src, int rw, int rh, uint32_t *out,
        float cornerRadius, float magnify, float aberration, int tintColor,
        float tintStrength, float s) {
    float hw = rw / 2.0f, hh = rh / 2.0f;
    float minhh = hw < hh ? hw : hh;
    float r;
    if (cornerRadius < 0.0f) { r = minhh; }
    else { r = cornerRadius * s; if (r > minhh) r = minhh; }
    if (r < 0.0f) r = 0.0f;
    int tr = (tintColor >> 16) & 0xff, tg = (tintColor >> 8) & 0xff, tb = tintColor & 0xff;
    int gtR = (LENS_GLASS_TINT >> 16) & 0xff, gtG = (LENS_GLASS_TINT >> 8) & 0xff, gtB = LENS_GLASS_TINT & 0xff;
    float liftMax = LENS_LIFT_COEF * (magnify - 1.0f) * hh;
    // The 3D-glass cues belong to the morph droplet, not the settled pill -- fade them
    // out by how magnified the drop is, so a resting selection is a flat subtle pill.
    float glassAmt = glassSmoothstep(1.085f, 1.25f, magnify);
    for (int y = 0; y < rh; y++) {
        float py = (y + 0.5f) - hh;
        for (int x = 0; x < rw; x++) {
            float px = (x + 0.5f) - hw;
            float dxe = fabsf(px) - (hw - r);
            float dye = fabsf(py) - (hh - r);
            float axx = dxe > 0 ? dxe : 0, ayy = dye > 0 ? dye : 0;
            float outside = sqrtf(axx * axx + ayy * ayy);
            float mxv = dxe > dye ? dxe : dye;
            float inside = mxv < 0 ? mxv : 0;
            float depth = -(outside + inside - r);
            if (depth <= 0.0f) { out[(size_t)y * rw + x] = 0; continue; }
            float alpha = depth >= 1.0f ? 1.0f : depth;
            float rd = sqrtf((px * px) / (hw * hw) + (py * py) / (hh * hh));   // elliptical 0..1
            if (rd > 1.0f) rd = 1.0f;
            // Centre ENLARGES (magnify); periphery SHRINKS toward RIM_SCALE (< 1) so the
            // bar/other tabs seen through the drop read minified while the central glyph
            // stays big. No shrink when settled (rimScale -> 1 as glassAmt -> 0).
            float edge = glassSmoothstep(LENS_MAG_FLAT, 1.0f, rd);
            float rimScale = 1.0f + (LENS_RIM_SCALE - 1.0f) * glassAmt;
            float mag = magnify + (rimScale - magnify) * edge;
            if (mag < 0.2f) mag = 0.2f;
            float ab = aberration * edge;
            float magR = mag * (1.0f - ab), magB = mag * (1.0f + ab);
            if (magR < 0.05f) magR = 0.05f;
            if (magB < 0.05f) magB = 0.05f;
            float lift = liftMax * (1.0f - rd * rd);                            // upward pull
            float refr = 1.0f + LENS_REFRACT * glassAmt * glassSmoothstep(0.70f, 1.0f, rd);
            int sr = (glassSampleBilinear(src, rw, rh, hw + (px / magR) * refr, hh + (py / magR) * refr + lift) >> 16) & 0xff;
            int sg = (glassSampleBilinear(src, rw, rh, hw + (px / mag) * refr, hh + (py / mag) * refr + lift) >> 8) & 0xff;
            int sb = glassSampleBilinear(src, rw, rh, hw + (px / magB) * refr, hh + (py / magB) * refr + lift) & 0xff;
            float lum = 0.2126f * sr + 0.7152f * sg + 0.0722f * sb;
            float t = tintStrength * glassSmoothstep(LENS_TINT_HI, LENS_TINT_LO, lum);
            float fr = sr + (tr - sr) * t;
            float fg = sg + (tg - sg) * t;
            float fb = sb + (tb - sb) * t;
            // faint cool tint through the whole glass (fades when settled)
            float gt = LENS_GLASS_TINT_STR * glassAmt;
            fr += (gtR - fr) * gt;
            fg += (gtG - fg) * gt;
            fb += (gtB - fb) * gt;
            // saturation boost: vivid blue glyph, neutral greys barely touched
            float sl = 0.2126f * fr + 0.7152f * fg + 0.0722f * fb;
            fr = sl + (fr - sl) * LENS_SAT_BOOST;
            fg = sl + (fg - sl) * LENS_SAT_BOOST;
            fb = sl + (fb - sl) * LENS_SAT_BOOST;
            // 3D glass: specular glare near the top + a bright edge rim
            float gx = px / hw, gy = (py + 0.42f * hh) / hh;
            float glare = LENS_GLARE * glassAmt * expf(-(gx * gx * 1.15f + gy * gy * 2.6f) * 2.1f);
            float rimW = LENS_RIM_W * hh; if (rimW < 2.0f) rimW = 2.0f;
            float rim = depth < rimW ? (1.0f - depth / rimW) * LENS_RIM : 0.0f;
            float bright = glare + rim;
            if (bright > 0.0f) {
                fr += bright * (255.0f - fr);
                fg += bright * (255.0f - fg);
                fb += bright * (255.0f - fb);
            }
            // soft dark band just inside the rim (glass depth), morph-only
            float esW = 0.13f * minhh; if (esW < 2.0f) esW = 2.0f;
            if (depth < esW) {
                float es = (1.0f - depth / esW) * LENS_EDGE_SHADOW * glassAmt;
                fr *= (1.0f - es);
                fg *= (1.0f - es);
                fb *= (1.0f - es);
            }
            if (fr < 0.0f) fr = 0.0f; else if (fr > 255.0f) fr = 255.0f;
            if (fg < 0.0f) fg = 0.0f; else if (fg > 255.0f) fg = 255.0f;
            if (fb < 0.0f) fb = 0.0f; else if (fb > 255.0f) fb = 255.0f;
            int a = (int)(alpha * 255.0f);
            int pr = (int)fr * a / 255, pg = (int)fg * a / 255, pb = (int)fb * a / 255;
            out[(size_t)y * rw + x] = ((uint32_t)a << 24) | ((uint32_t)pr << 16) | ((uint32_t)pg << 8) | (uint32_t)pb;
        }
    }
}

@implementation METALView

@synthesize commandQueue;
@synthesize commandBuffer;
@synthesize renderPassDescriptor;
@synthesize renderCommandEncoder;
@synthesize drawable;
@synthesize screenTexture;
@synthesize stencilTexture;
@synthesize peerComponentsLayer;
@synthesize framebufferWidth;
@synthesize framebufferHeight;
@synthesize projectionMatrix;

static simd_float4x4 CN1MetalOrtho(float left, float right, float bottom, float top, float near, float far) {
    // Metal NDC: x,y in [-1,1], z in [0,1]. Column-major construction matching Apple's conventions.
    float rl = 1.0f / (right - left);
    float tb = 1.0f / (top - bottom);
    float fn = 1.0f / (far - near);
    simd_float4x4 m = (simd_float4x4){{
        { 2.0f * rl,                 0.0f,                    0.0f,        0.0f },
        { 0.0f,                      2.0f * tb,               0.0f,        0.0f },
        { 0.0f,                      0.0f,                    -fn,         0.0f },
        { -(right + left) * rl,      -(top + bottom) * tb,    -near * fn,  1.0f }
    }};
    return m;
}

// You must implement this method
+ (Class)layerClass
{
    return [CAMetalLayer class];
}

extern BOOL isRetina();
extern BOOL isRetinaBug();

-(BOOL)isPaintPeersBehindEnabled {
    return com_codename1_impl_ios_IOSImplementation_isPaintPeersBehindEnabled___R_boolean(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

// Adds a peer component to the view.  Peer components are added to the peerComponentsLayer subview
-(void) addPeerComponent:(UIView*) view {
    if ([self isPaintPeersBehindEnabled]) {
        if (self.peerComponentsLayer == nil) {
            UIView *newRoot = [[UIView alloc] initWithFrame:self.bounds];
            newRoot.autoresizesSubviews = YES;
            newRoot.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
            self.peerComponentsLayer = [[UIView alloc] initWithFrame:self.bounds];
            self.peerComponentsLayer.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
            self.peerComponentsLayer.opaque = TRUE;
            self.peerComponentsLayer.userInteractionEnabled = TRUE;
            CAMetalLayer *metalLayer = (CAMetalLayer *)self.layer;
            metalLayer.opaque = FALSE;
            self.opaque = FALSE;
            self.backgroundColor = [UIColor clearColor];
            UIView* parent = [self superview];
            [CodenameOne_GLViewController instance].view = newRoot;
            [self removeFromSuperview];
            [newRoot addSubview:self.peerComponentsLayer];
            [newRoot addSubview:self];
            [parent addSubview:newRoot];
            
        }
        [self.peerComponentsLayer addSubview:view];
    } else {
        [self addSubview:view];
    }
    
}

-(BOOL)pointInside:(CGPoint)point withEvent:(UIEvent *)event
{
    if ([self isPaintPeersBehindEnabled]) {
        return com_codename1_impl_ios_IOSImplementation_hitTest___int_int_R_boolean(CN1_THREAD_GET_STATE_PASS_ARG point.x * scaleValue, point.y * scaleValue);
    } else {
        return YES;
    }
}

// Shared Metal device + command-queue setup invoked by both the NIB-
// instantiated path (initWithCoder:) and the programmatic-instantiation
// path (initWithFrame:). The Mac Catalyst slice goes through
// initWithFrame: because IBAgent-macOS-UIKit can't compile the iOS
// view-controller XIB and CodenameOne_GLAppDelegate falls back to
// passing nil to initWithNibName:. Without this shared setup the
// CAMetalLayer's device stays nil, CN1MetalSetDeviceAndCommandQueue
// is never published, and CN1MetalGlyphAtlas+atlasForFont: returns nil
// for every font -- which is exactly the "no atlas available" failure
// the Mac CI surfaced.
- (void)cn1SetupMetal {
    self.clearsContextBeforeDrawing = NO;
    if ([[UIScreen mainScreen] respondsToSelector:@selector(scale)] && isRetina()) {
        if(isRetinaBug()) {
            self.contentScaleFactor = 1.0;
        } else {
            self.contentScaleFactor = [[UIScreen mainScreen] scale];
        }
    }
    CAMetalLayer *metalLayer = (CAMetalLayer *)self.layer;
    metalLayer.device = MTLCreateSystemDefaultDevice();
    metalLayer.opaque = TRUE;
    metalLayer.pixelFormat = MTLPixelFormatBGRA8Unorm;
        // framebufferOnly must be NO: presentFramebuffer blits screenTexture
        // into the drawable via copyFromTexture:toTexture:, and Metal's blit
        // validation aborts ("destinationTexture must not be a framebufferOnly
        // texture") when the destination drawable was framebufferOnly. Debug
        // builds with Metal API Validation enabled crash on the first paint;
        // release builds silently produced undefined-behaviour copies on some
        // GPUs. Trading the (small) memoryless-storage benefit for a working
        // present path.
        metalLayer.framebufferOnly = NO;
        // Colour space for the Metal layer. Default is sRGB so colours
        // match the GL path's CAEAGLLayer output: without it, CG-rasterised
        // images and gradients (DeviceRGB-tagged in their CGBitmapContext)
        // display slightly brighter on Metal because the layer treats
        // their bytes as linear-RGB instead of sRGB-encoded.
        //
        // The build hint `ios.metal.colorSpace` selects the value (see
        // IPhoneBuilder, which injects one of the CN1_METAL_COLORSPACE_*
        // defines below). Set the hint to "none" to leave the layer's
        // colorspace property untouched (system default).
#if defined(CN1_METAL_COLORSPACE_NONE)
        // Skip setting metalLayer.colorspace entirely.
#else
  #if defined(CN1_METAL_COLORSPACE_DISPLAY_P3)
        CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceDisplayP3);
  #elif defined(CN1_METAL_COLORSPACE_DEVICE_RGB)
        CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
  #elif defined(CN1_METAL_COLORSPACE_LINEAR_SRGB)
        CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceLinearSRGB);
  #elif defined(CN1_METAL_COLORSPACE_EXTENDED_SRGB)
        CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceExtendedSRGB);
  #elif defined(CN1_METAL_COLORSPACE_EXTENDED_LINEAR_SRGB)
        CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceExtendedLinearSRGB);
  #else
        CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
  #endif
        if (cs != NULL) {
            metalLayer.colorspace = cs;
            CGColorSpaceRelease(cs);
        }
#endif
        // Cap drawable pool to 3 so the GPU has at most one render in
        // flight while CPU prepares the next two. Higher counts trade
        // smoothness for latency and memory; 3 is the iOS default for
        // most CAMetalLayer use cases. Combined with our nextDrawable
        // skip-frame fallback in presentFramebuffer this keeps the
        // pipeline non-blocking under pressure.
        metalLayer.maximumDrawableCount = 3;
        // `makeCommandQueue` is the Swift name; Objective-C uses `newCommandQueue`.
        // newCommandQueue returns +1 (NARC family); release the local after
        // the synthesized retain setter takes its own retain so we end up at
        // +1 owned by the property, not +2.
        id<MTLCommandQueue> newQueue = [metalLayer.device newCommandQueue];
        self.commandQueue = newQueue;
#ifndef CN1_USE_ARC
        [newQueue release];
#endif
        // Publish the device + queue to CN1Metalcompat so its global
        // accessors don't have to dereference our (UIView) layer from
        // background threads. Doing it on the main thread, exactly once,
        // means CN1MetalDevice / CN1MetalCommandQueue become cheap static
        // reads safe to invoke from the EDT and any background GCD queue.
        CN1MetalSetDeviceAndCommandQueue(metalLayer.device, self.commandQueue);
        CGSize sz = self.bounds.size;
        CGFloat s = self.contentScaleFactor;
        [self updateFrameBufferSize:(int)(sz.width * s) h:(int)(sz.height * s)];

    // Drop the glyph atlas + text cache + gradient cache on memory
    // pressure. Pipeline state cache stays — those are precious to
    // rebuild and small. The screen texture also stays; updateFrame-
    // BufferSize: handles its replacement on resize.
    [[NSNotificationCenter defaultCenter]
        addObserver:self
           selector:@selector(memoryWarning)
               name:UIApplicationDidReceiveMemoryWarningNotification
             object:nil];
}

//The EAGL view is stored in the nib file. When it's unarchived it's sent -initWithCoder:.
- (id)initWithCoder:(NSCoder*)coder
{
    self = [super initWithCoder:coder];
    if (self) {
        [self cn1SetupMetal];
    }
    return self;
}

// Programmatic instantiation. Used on Mac Catalyst (and any future
// platform where the iOS XIB is unavailable): CodenameOne_GLView-
// Controller's loadView allocates a METALView via initWithFrame:
// instead of loading from the NIB. Without this override the
// UIView default initWithFrame: runs, which skips cn1SetupMetal and
// leaves CN1MetalDevice() returning nil for the lifetime of the
// process -- the runtime failure mode that surfaced in CI as "no
// atlas available for font" on every CN1MetalDrawString call.
- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        [self cn1SetupMetal];
    }
    return self;
}

- (void)memoryWarning {
    extern void CN1MetalReleaseCaches(void);
    CN1MetalReleaseCaches();
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
#ifndef CN1_USE_ARC
    [super dealloc];
#endif
}


- (void)deleteFramebuffer
{
    if(editingComponent != nil) {
        return;
    }
    
}


-(void)updateFrameBufferSize:(int)w h:(int)h {
    // Trust caller-supplied physical-pixel dimensions; fall back to bounds
    // only if the caller passes 0. Reading self.bounds alone is unsafe
    // during rotation: viewWillTransitionToSize: in CodenameOne_GLView-
    // Controller fires BEFORE UIKit updates the view's bounds, so a
    // bounds-derived size matches the cached (old) framebuffer dimensions
    // and the early-return below would leave screenTexture, the projection
    // matrix and stencil texture at the previous orientation. CAMetalLayer's
    // drawableSize is auto-resized by UIKit on rotation, so the next
    // drawFrame would blit the old-sized screenTexture into the new-sized
    // drawable -- portrait content lands in a corner of the landscape
    // drawable and the remaining pixels read back uninitialised, surfacing
    // as the smeared/pink frames reported in #4954. The callers in this
    // file (initWithCoder, layoutSubviews) and the GLViewController callers
    // all pass physical pixels.
    int pw = w;
    int ph = h;
    if (pw <= 0 || ph <= 0) {
        CGSize sz = self.bounds.size;
        CGFloat s = self.contentScaleFactor;
        pw = (int)(sz.width * s);
        ph = (int)(sz.height * s);
    }
    if (pw <= 0 || ph <= 0) return;
    if (pw == framebufferWidth && ph == framebufferHeight) {
        return;
    }
    // An encoder may be mid-frame (awakeFromNib fires setFramebuffer before
    // layoutSubviews, so the first encoder references the xib's placeholder
    // bounds). Tear it down cleanly so the next setFramebuffer creates a
    // fresh encoder against the new screenTexture. Otherwise draws land on
    // a texture we're about to replace, and the stale dimensions get cached
    // inside CN1Metalcompat (breaking scissor clamping etc.).
    if (self.renderCommandEncoder != nil) {
        CN1MetalEndFrame();
        [self.renderCommandEncoder endEncoding];
        self.renderCommandEncoder = nil;
    }
    if (self.commandBuffer != nil) {
        [self.commandBuffer commit];
        self.commandBuffer = nil;
        self.renderPassDescriptor = nil;
        self.drawable = nil;
    }
    // Preserve the previously rendered frame across the resize so a rotation
    // never shows black. On the Metal backend, changing layer.drawableSize
    // (below) invalidates the CAMetalLayer's currently displayed drawable, so
    // the layer falls back to its opaque (black) background until the next
    // presentDrawable:. With the CADisplayLink disabled in this port, that
    // next present only arrives once the EDT wakes, re-lays-out and repaints --
    // a gap that is invisible while the app is actively painting but produces a
    // visible black flash during the ~0.3s rotation animation when the app has
    // gone idle (#5162). Capture the old screen texture here; after the new one
    // is built we scale-blit the last frame into it and present once so the
    // layer keeps showing the previous frame (stretched, like UIKit's own
    // rotation snapshot) until the real repaint lands.
    id<MTLTexture> oldScreen = self.screenTexture;
#ifndef CN1_USE_ARC
    // Keep it alive past the self.screenTexture reassignment below: the
    // synthesized retain setter releases the previously held value.
    [oldScreen retain];
#endif
    framebufferWidth = pw;
    framebufferHeight = ph;
    // Match iOS UIKit's Y-down convention: origin at top-left.
    // Passing bottom=h, top=0 makes y_ndc = 1 - 2*y_input/h, so y_input=0
    // maps to NDC y=+1 (top of the drawable) and y_input=h maps to NDC y=-1
    // (bottom). That avoids the _glScalef(1,-1,1) + _glTranslatef(0,-h,0)
    // workaround the GL path does in CodenameOne_GLViewController.drawFrame.
    projectionMatrix = CN1MetalOrtho(0.0f, (float)pw, (float)ph, 0.0f, -1.0f, 1.0f);
    CAMetalLayer *layer = (CAMetalLayer*)self.layer;
    layer.drawableSize = CGSizeMake(pw, ph);

    // Rebuild the persistent screen render target at the new size. Anything
    // previously rendered into the old texture is lost; the next frame will
    // re-clear from black as CN1 repaints -- Form.paint() always issues a
    // full-screen background fill on layout changes so this is safe.
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
        width:pw height:ph mipmapped:NO];
    desc.usage = MTLTextureUsageRenderTarget | MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModePrivate;
    // newTextureWithDescriptor returns +1 (NARC family); the synthesized
    // retain setter adds another +1 for a net +2 under MRR. Release the
    // local once the property holds its own retain so we don't leak the
    // previous screenTexture every time the framebuffer is resized
    // (rotation, window resize, etc.).
    id<MTLTexture> newScreen = [layer.device newTextureWithDescriptor:desc];
    self.screenTexture = newScreen;
#ifndef CN1_USE_ARC
    [newScreen release];
#endif

    // Initialise the new texture. Private-storage textures come back
    // uninitialised, so the first frame (which uses MTLLoadActionLoad) would
    // sample garbage for any pixel CN1 hasn't drawn yet. When a previous frame
    // exists we scale-blit it in (preserving the last visible content across
    // the resize -- see the oldScreen capture above); otherwise we just clear
    // to opaque black.
    id<MTLCommandBuffer> clearCb = [self.commandQueue commandBuffer];
    MTLRenderPassDescriptor *clearPass = [MTLRenderPassDescriptor renderPassDescriptor];
    clearPass.colorAttachments[0].texture = self.screenTexture;
    clearPass.colorAttachments[0].loadAction = MTLLoadActionClear;
    clearPass.colorAttachments[0].storeAction = MTLStoreActionStore;
    clearPass.colorAttachments[0].clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0);
    // The preserve draw below binds a CN1MetalPipelineCache pipeline, and every
    // pipeline in that cache declares stencilAttachmentPixelFormat=Stencil8
    // (polygon-clip #3921). A render pass that binds such a pipeline MUST attach
    // a Stencil8 texture or Metal aborts in setRenderPipelineState: with a
    // pixel-format mismatch (#5103): "For stencil attachment, the
    // renderPipelineState pixelFormat must be MTLPixelFormatInvalid, as no
    // texture is set." Under MTL_DEBUG_LAYER=assert (the CI Metal screenshot
    // job) that abort is a SIGABRT on the first resize, which drops every
    // screenshot after it. Attach a throwaway clear-on-load stencil exactly
    // like the seed draw in CN1Metalcompat.m -- the preserve draw never engages
    // the stencil test, so its contents are irrelevant. Only needed when there
    // is a previous frame to draw (the plain black clear binds no pipeline).
    id<MTLTexture> clearStencilTex = nil;
    if (oldScreen != nil) {
        MTLTextureDescriptor *clearStencilDesc = [MTLTextureDescriptor
            texture2DDescriptorWithPixelFormat:MTLPixelFormatStencil8
            width:pw height:ph mipmapped:NO];
        clearStencilDesc.usage = MTLTextureUsageRenderTarget;
        clearStencilDesc.storageMode = MTLStorageModePrivate;
        clearStencilTex = [layer.device newTextureWithDescriptor:clearStencilDesc];
        if (clearStencilTex != nil) {
            clearPass.stencilAttachment.texture = clearStencilTex;
            clearPass.stencilAttachment.loadAction = MTLLoadActionClear;
            clearPass.stencilAttachment.storeAction = MTLStoreActionDontCare;
            clearPass.stencilAttachment.clearStencil = 0;
        }
    }
    id<MTLRenderCommandEncoder> clearEnc = [clearCb renderCommandEncoderWithDescriptor:clearPass];
#ifndef CN1_USE_ARC
    // renderCommandEncoderWithDescriptor: retains the attachment for the pass.
    [clearStencilTex release];
#endif
    if (oldScreen != nil) {
        [clearEnc setViewport:(MTLViewport){ 0.0, 0.0, (double)pw, (double)ph, 0.0, 1.0 }];
        CN1MetalBeginFrame(clearEnc, projectionMatrix, pw, ph);
        // Stretch the whole previous frame to fill the new drawable. A pure
        // scale (no rotation) is imperfect across a portrait<->landscape swap
        // but is only on screen for the rotation animation and is far less
        // jarring than a black flash. screenTexture is rendered with the same
        // y-down projection, so CN1MetalDrawImage's V=0-at-top mapping keeps
        // the old top at the new top (no vertical flip needed).
        CN1MetalDrawImage(oldScreen, 255, 0, 0, pw, ph);
        CN1MetalEndFrame();
    }
    [clearEnc endEncoding];
    [clearCb commit];

    // Build a matching Stencil8 attachment for polygon-shape clipping
    // (#3921). Private storage rather than Memoryless because Memoryless
    // is only supported on tile-based deferred GPUs (iOS Simulator on
    // older Intel-Mac CI runners doesn't accept it). The stencil is
    // ephemeral conceptually but Private works on all GPU families and
    // the size cost is tiny (1 byte/pixel).
    MTLTextureDescriptor *stencilDesc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatStencil8
        width:pw height:ph mipmapped:NO];
    stencilDesc.usage = MTLTextureUsageRenderTarget;
    stencilDesc.storageMode = MTLStorageModePrivate;
    id<MTLTexture> newStencil = [layer.device newTextureWithDescriptor:stencilDesc];
    self.stencilTexture = newStencil;
#ifndef CN1_USE_ARC
    [newStencil release];
#endif

    // Push the preserved frame onto the layer so the rotation never shows
    // black (#5162) -- but NOT synchronously here. updateFrameBufferSize: runs
    // from viewWillTransitionToSize: on the main thread, inside UIKit's
    // rotation CATransaction, with the layer.drawableSize change above still
    // pending/uncommitted in that transaction. Calling [layer nextDrawable]
    // now blocks: the layer cannot vend a drawable until the resize transaction
    // commits, and that transaction cannot commit until viewWillTransitionToSize:
    // returns -- which it cannot, because we are blocked in nextDrawable. On the
    // simulator CoreAnimation tolerates this (which is why the original #5162
    // fix, verified only in the simulator, appeared to work); on a real device
    // the render server wedges and, because the EDT renders via
    // dispatch_sync(main), the EDT blocks on the stalled main thread forever --
    // the hard rotation freeze (#5171).
    //
    // Deferring the present to the next main-runloop turn breaks the cycle: by
    // then viewWillTransitionToSize: has returned and the implicit drawableSize
    // transaction has committed, so nextDrawable vends a correctly-sized
    // drawable exactly as the normal presentFramebuffer path does -- no
    // in-transaction wedge, no freeze. The needsResizePresent guard makes this
    // self-tuning: when the app is idle (the #5162 case) the EDT is asleep, so
    // the deferred block runs and fills the rotation gap with the preserved
    // frame; when the app is actively painting (the #5171 case) the EDT repaints
    // first, presentFramebuffer clears the guard, and the deferred block becomes
    // a no-op -- so it never contends for a drawable in exactly the scenario
    // that used to deadlock.
    if (oldScreen != nil) {
        needsResizePresent = YES;
        dispatch_async(dispatch_get_main_queue(), ^{
            [self presentPreservedFrameIfNeeded];
        });
    }
#ifndef CN1_USE_ARC
    [oldScreen release];
#endif
}

// Push the frame currently held in screenTexture (the stretched previous frame
// preserved across a resize by updateFrameBufferSize:) onto the CAMetalLayer,
// so an idle rotation shows the last content rather than the layer's black
// background until the EDT repaints (#5162). A no-op once needsResizePresent
// has been cleared -- either because a normal presentFramebuffer already put a
// real frame up, or because a later resize superseded this one. Runs on the
// main thread (scheduled via dispatch_async from updateFrameBufferSize:),
// outside the rotation CATransaction, so nextDrawable here behaves exactly like
// the normal present path and cannot deadlock (#5171).
-(void)presentPreservedFrameIfNeeded {
    if (!needsResizePresent) {
        return;
    }
    needsResizePresent = NO;
    if (self.screenTexture == nil) {
        return;
    }
    // An encoder may be mid-frame if the EDT started painting between the resize
    // and this turn; in that case the normal present path owns the drawable and
    // will have cleared needsResizePresent already, so we would have returned
    // above. Guard anyway: never present while an encoder is open.
    if (self.renderCommandEncoder != nil) {
        return;
    }
    CAMetalLayer *layer = (CAMetalLayer*)self.layer;
    id<CAMetalDrawable> dr = [layer nextDrawable];
    if (dr == nil) {
        return;
    }
    id<MTLCommandBuffer> presentCb = [self.commandQueue commandBuffer];
    id<MTLBlitCommandEncoder> blit = [presentCb blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture
              sourceSlice:0 sourceLevel:0
             sourceOrigin:MTLOriginMake(0, 0, 0)
               sourceSize:MTLSizeMake(framebufferWidth, framebufferHeight, 1)
                toTexture:dr.texture
         destinationSlice:0 destinationLevel:0
        destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [presentCb presentDrawable:dr];
    [presentCb commit];
}

-(void)createRenderPassDescriptor {
    if (self.screenTexture == nil) {
        self.renderPassDescriptor = nil;
        return;
    }
    self.renderPassDescriptor = [MTLRenderPassDescriptor renderPassDescriptor];
    MTLRenderPassColorAttachmentDescriptor* colorAttachment = self.renderPassDescriptor.colorAttachments[0];
    // Render into the persistent screen texture so incremental draws from
    // subsequent drawFrame calls accumulate on top of whatever was there
    // before. MTLLoadActionLoad preserves previous pixels (vs MTLLoadActionClear
    // which would wipe everything each frame) — CN1 only queues diff ops
    // per frame; the OpenGL path relies on its renderbuffer persisting.
    colorAttachment.texture = self.screenTexture;
    colorAttachment.loadAction = MTLLoadActionLoad;
    colorAttachment.storeAction = MTLStoreActionStore;
    // Attach the Stencil8 texture for polygon-shape clipping (#3921).
    // Cleared at the start of every frame and discarded at the end --
    // stencil values from previous frames are never referenced, and the
    // reference-value counter in CN1Metalcompat resets per encoder, so
    // a fresh clear is the right semantics.
    if (self.stencilTexture != nil) {
        MTLRenderPassStencilAttachmentDescriptor* stencilAttachment = self.renderPassDescriptor.stencilAttachment;
        stencilAttachment.texture = self.stencilTexture;
        stencilAttachment.loadAction = MTLLoadActionClear;
        stencilAttachment.storeAction = MTLStoreActionDontCare;
        stencilAttachment.clearStencil = 0;
    }
}

- (void)setFramebuffer
{
    // setFramebuffer may be called multiple times per frame (awakeFromNib
    // issues one unpaired call during init; drawFrame can be invoked
    // out-of-band alongside the CADisplayLink path). The GL backend tolerates
    // this because binding the same framebuffer twice is a no-op. For Metal
    // we keep the same encoder alive across those extra calls -- creating a
    // fresh encoder each time would throw away any ops queued between setup
    // and presentFramebuffer. Only presentFramebuffer ends+commits+presents.
    if (self.renderCommandEncoder != nil) {
        return;
    }
    CAMetalLayer *layer = (CAMetalLayer*)self.layer;
    self.commandBuffer = [self.commandQueue commandBuffer];
    [self createRenderPassDescriptor];
    if (self.renderPassDescriptor == nil) {
        // nextDrawable returned nil; skip this frame.
        self.renderCommandEncoder = nil;
        return;
    }
    self.renderCommandEncoder = [self.commandBuffer renderCommandEncoderWithDescriptor:self.renderPassDescriptor];
    [self.renderCommandEncoder setViewport: (MTLViewport){ 0.0, 0.0, (double)framebufferWidth, (double)framebufferHeight, 0.0, 1.0 }];
    // Publish the encoder + projection to the CN1Metalcompat layer; each
    // ExecutableOp's Metal branch pulls the encoder from there.
    CN1MetalBeginFrame(self.renderCommandEncoder, projectionMatrix, framebufferWidth, framebufferHeight);
}

// Live-screen "Liquid Glass": blur the region of screenTexture that has already
// been drawn this frame (the backdrop behind a glass component) and draw the
// blurred + vibrancy-boosted result back, so the component's foreground (queued
// after this op) paints on top. Runs during the drain, against the live screen
// command buffer -- the only path that produces real glass on a running app
// (the offscreen-image blur only covered fidelity tiles). Costs a GPU sync per
// glass paint; acceptable for the small, mostly-static nav/tab bars.
- (void)blurScreenRegionX:(int)x y:(int)y w:(int)w h:(int)h radius:(float)radius {
    if (self.screenTexture == nil || w <= 0 || h <= 0 || radius <= 0.0f) {
        return;
    }
    CGFloat s = self.contentScaleFactor;
    int texW = (int)self.screenTexture.width, texH = (int)self.screenTexture.height;
    int fx = (int)(x * s), fy = (int)(y * s), fw = (int)(w * s), fh = (int)(h * s);
    if (fx < 0) { fw += fx; fx = 0; }
    if (fy < 0) { fh += fy; fy = 0; }
    if (fx + fw > texW) { fw = texW - fx; }
    if (fy + fh > texH) { fh = texH - fy; }
    if (fw <= 0 || fh <= 0) { return; }

    // 1) End + commit the screen encoder so screenTexture holds the backdrop
    //    drawn so far this frame, then wait so the blit-read sees it.
    if (self.renderCommandEncoder != nil) {
        CN1MetalEndFrame();
        [self.renderCommandEncoder endEncoding];
        self.renderCommandEncoder = nil;
    }
    id<MTLCommandBuffer> cb = self.commandBuffer;
    self.commandBuffer = nil;
    if (cb != nil) {
        [cb commit];
        [cb waitUntilCompleted];
    }

    // 2) Blit the region into a shared scratch texture and read its bytes.
    id<MTLDevice> device = CN1MetalDevice();
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm width:fw height:fh mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModeShared;
    id<MTLTexture> scratch = [device newTextureWithDescriptor:desc];
    id<MTLCommandBuffer> blitCb = [self.commandQueue commandBuffer];
    id<MTLBlitCommandEncoder> blit = [blitCb blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture sourceSlice:0 sourceLevel:0
              sourceOrigin:MTLOriginMake(fx, fy, 0) sourceSize:MTLSizeMake(fw, fh, 1)
                 toTexture:scratch destinationSlice:0 destinationLevel:0
         destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [blitCb commit];
    [blitCb waitUntilCompleted];

    NSUInteger rowBytes = (NSUInteger)fw * 4;
    uint8_t *bytes = (uint8_t *)malloc(rowBytes * (NSUInteger)fh);
    if (bytes == NULL) { [self setFramebuffer]; return; }
    [scratch getBytes:bytes bytesPerRow:rowBytes fromRegion:MTLRegionMake2D(0, 0, fw, fh) mipmapLevel:0];

    // 3) CIGaussianBlur + saturation (the UIBlurEffect-style material recipe).
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    CGContextRef bmp = CGBitmapContextCreate(bytes, fw, fh, 8, rowBytes, cs,
        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little);
    CGImageRef srcCg = CGBitmapContextCreateImage(bmp);
    CIImage *ci = [CIImage imageWithCGImage:srcCg];
    CIFilter *sat = [CIFilter filterWithName:@"CIColorControls"];
    [sat setValue:ci forKey:kCIInputImageKey];
    [sat setValue:@(1.8) forKey:@"inputSaturation"];
    CIFilter *gb = [CIFilter filterWithName:@"CIGaussianBlur"];
    [gb setValue:[sat outputImage] forKey:kCIInputImageKey];
    [gb setValue:@(radius * s) forKey:kCIInputRadiusKey];
    CIImage *clamped = [[gb outputImage] imageByClampingToExtent];
    // Retain the cached context: under MRC the autoreleased CIContext would
    // dangle and crash on the next glass paint (a static Foundation cache must
    // be +1 retained). The retain is a harmless no-op under ARC.
    static CIContext *ciCtx = nil;
    if (ciCtx == nil) {
        ciCtx = [CIContext contextWithMTLDevice:device];
#ifndef CN1_USE_ARC
        [ciCtx retain];
#endif
    }
    CGImageRef outCg = [ciCtx createCGImage:clamped fromRect:CGRectMake(0, 0, fw, fh)];
    UIImage *blurredImage = outCg ? [UIImage imageWithCGImage:outCg] : nil;
    if (srcCg) { CGImageRelease(srcCg); }
    if (outCg) { CGImageRelease(outCg); }
    CGContextRelease(bmp);
    CGColorSpaceRelease(cs);
    free(bytes);

    // 4) Restart the screen encoder (loadAction Load preserves screenTexture).
    [self setFramebuffer];

    // 5) Draw the blurred patch back over the region (display coords).
    if (blurredImage != nil) {
        id<MTLTexture> blurredTex = CN1MetalTextureFromUIImage(blurredImage);
        if (blurredTex != nil) {
            CN1MetalDrawImage(blurredTex, 255, x, y, w, h);
        }
    }
}

// Live-screen "Liquid Glass" MATERIAL: the full backdrop-filter recipe matching
// the offscreen IOSImplementation.glassRegion that drives the fidelity tiles.
// 1) read a screenTexture region PADDED by 3*radius (edge-replicated so the blur
//    never fades into the component edge), 2) apply the affine colour material,
// 3) Gaussian-blur, 4) apply optics (rounded-rect SDF mask + edge refraction +
// specular rim), 5) draw the pill-shaped translucent glass patch back over the
// backdrop so the component's fill + foreground (queued next) paint on top. Runs
// ---- live-glass patch cache ------------------------------------------------
// Caching/invalidation policy for the live-screen glass materials (review):
//   * A glass surface only pays at all when it REPAINTS; a static chrome bar
//     over static content costs nothing between repaints.
//   * When it does repaint, the backdrop readback (commit + waitUntilCompleted
//     + blit + getBytes) is unavoidable for correctness -- the material is a
//     function of the pixels behind the glass. What CAN be skipped is the
//     expensive composition: the per-pixel colour transform, the Gaussian
//     blur and the edge optics.
//   * So the composed patch is cached per glass rect: while the rect, the
//     material parameters AND a hash of the backdrop bytes are unchanged
//     (i.e. "backdrop and bounds are stable"), the cached patch is redrawn
//     directly. When the backdrop changes -- scrolling content under the bar,
//     an animation behind the glass -- the hash misses and the patch is
//     recomposed that frame; there is no stale-glass failure mode because the
//     decision is taken from the actual backdrop bytes, not from heuristics.
//   * The travelling selection LENS never takes this path: it is a pure GPU
//     fragment shader on the frame's own command buffer (lensScreenRegionX),
//     with no sync and no readback, so it needs no cache.
// Define CN1_GLASS_PROFILE to NSLog per-paint timing + cache hit/miss so the
// frame-cost evidence is reproducible on any device/simulator build.
#define CN1_GLASS_PATCH_CACHE_SLOTS 8
typedef struct {
    int valid;
    int fx, fy, fw, fh;
    float rad, cornerRadius, sat, scale, offset, refract, specular;
    uint64_t backdropHash;
    uint32_t *patch;       // composed premultiplied glass patch (fw*fh), malloc'd
} CN1GlassPatchCacheEntry;
static CN1GlassPatchCacheEntry cn1GlassPatchCache[CN1_GLASS_PATCH_CACHE_SLOTS];
static int cn1GlassPatchCacheNext = 0;

// FNV-1a over the backdrop words -- a fraction of the cost of the blur pass it
// can save, and any real backdrop change flips it.
static uint64_t cn1GlassBackdropHash(const uint8_t *bytes, size_t len) {
    const uint32_t *words = (const uint32_t *)bytes;
    size_t n = len / 4;
    uint64_t hsh = 1469598103934665603ULL;
    for (size_t i = 0; i < n; i++) {
        hsh ^= words[i];
        hsh *= 1099511628211ULL;
    }
    return hsh;
}

// during the drain like blurScreenRegionX; one GPU sync per glass paint.
- (void)glassScreenRegionX:(int)x y:(int)y w:(int)w h:(int)h radius:(float)radius
              cornerRadius:(float)cornerRadius sat:(float)sat scale:(float)scale
                    offset:(float)offset refract:(float)refract specular:(float)specular {
    if (self.screenTexture == nil || w <= 0 || h <= 0 || radius <= 0.0f) {
        return;
    }
    // CN1-logical -> framebuffer-pixel scale. NOT contentScaleFactor alone:
    // scaleValue maps UIKit-points -> CN1-logical (1 in a normal app, but e.g. 3
    // in the fidelity app which runs logical==physical pixel coords). The real
    // logical->pixel ratio is contentScaleFactor/scaleValue (= 3/3 = 1 there,
    // 3/1 = 3 in a normal retina app). Using raw contentScaleFactor triple-scaled
    // the region in the fidelity app (wrong screenTexture slice + 3x radius).
    float sv = scaleValue > 0.0f ? scaleValue : 1.0f;
    CGFloat s = self.contentScaleFactor / sv;
    int texW = (int)self.screenTexture.width, texH = (int)self.screenTexture.height;
    int fx = (int)(x * s), fy = (int)(y * s), fw = (int)(w * s), fh = (int)(h * s);
    if (fx < 0) { fw += fx; fx = 0; }
    if (fy < 0) { fh += fy; fy = 0; }
    if (fx + fw > texW) { fw = texW - fx; }
    if (fy + fh > texH) { fh = texH - fy; }
    if (fw <= 0 || fh <= 0) { return; }
    float rad = radius * (float)s;
    int pad = (int)ceilf(rad) * 3 + 1;
    int bw = fw + 2 * pad, bh = fh + 2 * pad;

    // 1) End + commit the screen encoder so screenTexture holds the backdrop.
    if (self.renderCommandEncoder != nil) {
        CN1MetalEndFrame();
        [self.renderCommandEncoder endEncoding];
        self.renderCommandEncoder = nil;
    }
    id<MTLCommandBuffer> cb = self.commandBuffer;
    self.commandBuffer = nil;
    if (cb != nil) { [cb commit]; [cb waitUntilCompleted]; }

    // 2) Blit the clamped padded region and read its bytes.
    int ax0 = fx - pad; if (ax0 < 0) ax0 = 0;
    int ay0 = fy - pad; if (ay0 < 0) ay0 = 0;
    int ax1 = fx + fw + pad; if (ax1 > texW) ax1 = texW;
    int ay1 = fy + fh + pad; if (ay1 > texH) ay1 = texH;
    int aw = ax1 - ax0, ah = ay1 - ay0;
    if (aw <= 0 || ah <= 0) { [self setFramebuffer]; return; }
    id<MTLDevice> device = CN1MetalDevice();
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm width:aw height:ah mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModeShared;
    id<MTLTexture> scratch = [device newTextureWithDescriptor:desc];
    id<MTLCommandBuffer> blitCb = [self.commandQueue commandBuffer];
    id<MTLBlitCommandEncoder> blit = [blitCb blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture sourceSlice:0 sourceLevel:0
              sourceOrigin:MTLOriginMake(ax0, ay0, 0) sourceSize:MTLSizeMake(aw, ah, 1)
                 toTexture:scratch destinationSlice:0 destinationLevel:0
         destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [blitCb commit];
    [blitCb waitUntilCompleted];
    NSUInteger availRow = (NSUInteger)aw * 4;
    uint8_t *avail = (uint8_t *)malloc(availRow * (NSUInteger)ah);
    if (avail == NULL) { [self setFramebuffer]; return; }
    [scratch getBytes:avail bytesPerRow:availRow fromRegion:MTLRegionMake2D(0, 0, aw, ah) mipmapLevel:0];

#ifdef CN1_GLASS_PROFILE
    CFTimeInterval cn1gpT0 = CACurrentMediaTime();
#endif
    // 2b) Patch cache: when this glass rect, its material params AND the
    //     backdrop bytes are unchanged since the last composition, redraw the
    //     cached patch and skip the transform + blur + optics entirely (see
    //     the policy comment above the cache).
    uint64_t backdropHash = cn1GlassBackdropHash(avail, availRow * (NSUInteger)ah);
    int cacheSlot = -1;
    for (int ci = 0; ci < CN1_GLASS_PATCH_CACHE_SLOTS; ci++) {
        CN1GlassPatchCacheEntry *e = &cn1GlassPatchCache[ci];
        if (e->valid && e->fx == fx && e->fy == fy && e->fw == fw && e->fh == fh
                && e->rad == rad && e->cornerRadius == cornerRadius && e->sat == sat
                && e->scale == scale && e->offset == offset && e->refract == refract
                && e->specular == specular) {
            cacheSlot = ci;
            if (e->backdropHash == backdropHash && e->patch != NULL) {
                free(avail);
                [self setFramebuffer];
                [self drawGlassPatch:e->patch fw:fw fh:fh x:x y:y w:w h:h];
#ifdef CN1_GLASS_PROFILE
                NSLog(@"CN1GLASSPROF hit rect=%d,%d %dx%d hash=%016llx %.2fms",
                      fx, fy, fw, fh, (unsigned long long)backdropHash,
                      (CACurrentMediaTime() - cn1gpT0) * 1000.0);
#endif
                return;
            }
            break;
        }
    }

    // 3) Edge-replicate into a padded buffer and apply the colour material.
    uint32_t *prgb = (uint32_t *)malloc((size_t)bw * (size_t)bh * 4);
    if (prgb == NULL) { free(avail); [self setFramebuffer]; return; }
    for (int by = 0; by < bh; by++) {
        int ay = (fy - pad + by) - ay0; if (ay < 0) ay = 0; else if (ay >= ah) ay = ah - 1;
        for (int bx = 0; bx < bw; bx++) {
            int axc = (fx - pad + bx) - ax0; if (axc < 0) axc = 0; else if (axc >= aw) axc = aw - 1;
            uint8_t *p = avail + (size_t)ay * availRow + (size_t)axc * 4;
            float bch = p[0], gch = p[1], rch = p[2];   // BGRA premult-first (backdrop opaque)
            float lum = 0.2126f * rch + 0.7152f * gch + 0.0722f * bch;
            float rr = (lum + (rch - lum) * sat) * scale + offset;
            float gg = (lum + (gch - lum) * sat) * scale + offset;
            float bb = (lum + (bch - lum) * sat) * scale + offset;
            int ri = rr < 0 ? 0 : (rr > 255 ? 255 : (int)rr);
            int gi = gg < 0 ? 0 : (gg > 255 ? 255 : (int)gg);
            int bi = bb < 0 ? 0 : (bb > 255 ? 255 : (int)bb);
            prgb[(size_t)by * bw + bx] = 0xff000000u | ((uint32_t)ri << 16) | ((uint32_t)gi << 8) | (uint32_t)bi;
        }
    }
    free(avail);

    // 4) Blur the padded material buffer, then optics -> premultiplied patch.
    glassGaussianBlur(prgb, bw, bh, rad);
    uint32_t *out = (uint32_t *)malloc((size_t)fw * (size_t)fh * 4);
    if (out == NULL) { free(prgb); [self setFramebuffer]; return; }
    glassApplyOptics(prgb, bw, bh, pad, out, fw, fh, cornerRadius, refract, specular, (float)s);
    free(prgb);

    // 4b) Store the composed patch in the cache (the cache owns the buffer).
    if (cacheSlot < 0) {
        cacheSlot = cn1GlassPatchCacheNext;
        cn1GlassPatchCacheNext = (cn1GlassPatchCacheNext + 1) % CN1_GLASS_PATCH_CACHE_SLOTS;
    }
    CN1GlassPatchCacheEntry *entry = &cn1GlassPatchCache[cacheSlot];
    if (entry->patch != NULL) {
        free(entry->patch);
    }
    entry->valid = 1;
    entry->fx = fx; entry->fy = fy; entry->fw = fw; entry->fh = fh;
    entry->rad = rad; entry->cornerRadius = cornerRadius; entry->sat = sat;
    entry->scale = scale; entry->offset = offset; entry->refract = refract;
    entry->specular = specular;
    entry->backdropHash = backdropHash;
    entry->patch = out;

    // 5) Restart the screen encoder, then draw the glass patch back (display coords).
    [self setFramebuffer];
    [self drawGlassPatch:out fw:fw fh:fh x:x y:y w:w h:h];
#ifdef CN1_GLASS_PROFILE
    NSLog(@"CN1GLASSPROF miss rect=%d,%d %dx%d hash=%016llx %.2fms",
          fx, fy, fw, fh, (unsigned long long)backdropHash,
          (CACurrentMediaTime() - cn1gpT0) * 1000.0);
#endif
}

// Uploads a composed premultiplied-BGRA glass patch and draws it at the given
// CN1-logical rect. The patch buffer is NOT consumed (the cache owns it).
- (void)drawGlassPatch:(uint32_t *)patch fw:(int)fw fh:(int)fh x:(int)x y:(int)y w:(int)w h:(int)h {
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    CGContextRef bmp = CGBitmapContextCreate(patch, fw, fh, 8, (size_t)fw * 4, cs,
        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little);
    CGImageRef outCg = bmp ? CGBitmapContextCreateImage(bmp) : NULL;
    if (outCg != NULL) {
        UIImage *glassImage = [UIImage imageWithCGImage:outCg];
        id<MTLTexture> glassTex = CN1MetalTextureFromUIImage(glassImage);
        if (glassTex != nil) { CN1MetalDrawImage(glassTex, 255, x, y, w, h); }
        CGImageRelease(outCg);
    }
    if (bmp != NULL) { CGContextRelease(bmp); }
    CGColorSpaceRelease(cs);
}

// Live-screen iOS 26 selection "drop" LENS. Unlike glassScreenRegionX (a frosted
// blur behind the content) this is painted OVER the bar + the black glyphs and
// reads them back: it magnifies, chromatically aberrates and dark->accent tints
// the live content beneath it (see glassApplyLens). No padding/blur -- the lens
// samples within its own bounds. Runs during the drain like the glass op.
- (void)lensScreenRegionX:(int)x y:(int)y w:(int)w h:(int)h cornerRadius:(float)cornerRadius
                  magnify:(float)magnify aberration:(float)aberration tintColor:(int)tintColor tintStrength:(float)tintStrength {
    if (self.screenTexture == nil || w <= 0 || h <= 0) {
        return;
    }
    float sv = scaleValue > 0.0f ? scaleValue : 1.0f;
    CGFloat s = self.contentScaleFactor / sv;
    int texW = (int)self.screenTexture.width, texH = (int)self.screenTexture.height;
    int fx = (int)(x * s), fy = (int)(y * s), fw = (int)(w * s), fh = (int)(h * s);
    if (fx < 0) { fw += fx; fx = 0; }
    if (fy < 0) { fh += fy; fy = 0; }
    if (fx + fw > texW) { fw = texW - fx; }
    if (fy + fh > texH) { fh = texH - fy; }
    if (fw <= 0 || fh <= 0) { return; }

    // GPU LENS: blit the bar region to a scratch texture and draw the drop quad with the
    // cn1_fs_lens shader sampling it -- entirely on the GPU. The old path read the region
    // back to the CPU (2x waitUntilCompleted stalls + getBytes + a UIImage->texture upload)
    // EVERY frame, capping the morph at ~6fps; this keeps it at frame rate.
    //
    // 1) End the current render encoder so the bar draws are flushed into screenTexture, but
    //    KEEP the frame's command buffer: the blit + lens draw go on the SAME buffer so the
    //    GPU executes bar-draw -> blit -> lens-draw in order (Metal tracks texture hazards),
    //    with no CPU sync.
    if (self.renderCommandEncoder != nil) {
        CN1MetalEndFrame();
        [self.renderCommandEncoder endEncoding];
        self.renderCommandEncoder = nil;
    }
    if (self.commandBuffer == nil) {
        // A prior op already committed it; screenTexture holds the bar, so a fresh buffer is fine.
        self.commandBuffer = [self.commandQueue commandBuffer];
    }

    // 2) Scratch texture (Private = GPU-only; ShaderRead for the fragment sample).
    id<MTLDevice> device = CN1MetalDevice();
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm width:fw height:fh mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModePrivate;
    id<MTLTexture> scratch = [device newTextureWithDescriptor:desc];
    if (scratch == nil) { [self setFramebuffer]; return; }

    // 3) Blit the bar region screenTexture -> scratch on the frame's command buffer.
    id<MTLBlitCommandEncoder> blit = [self.commandBuffer blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture sourceSlice:0 sourceLevel:0
              sourceOrigin:MTLOriginMake(fx, fy, 0) sourceSize:MTLSizeMake(fw, fh, 1)
                 toTexture:scratch destinationSlice:0 destinationLevel:0
         destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];

    // 4) Restart a render encoder on the SAME command buffer (loadAction Load preserves the
    //    bar) and re-publish it to the CN1Metalcompat draw layer.
    [self createRenderPassDescriptor];
    if (self.renderPassDescriptor == nil) { return; }
    self.renderCommandEncoder = [self.commandBuffer renderCommandEncoderWithDescriptor:self.renderPassDescriptor];
    [self.renderCommandEncoder setViewport:(MTLViewport){ 0.0, 0.0, (double)framebufferWidth, (double)framebufferHeight, 0.0, 1.0 }];
    CN1MetalBeginFrame(self.renderCommandEncoder, projectionMatrix, framebufferWidth, framebufferHeight);

    // 5) Draw the lens quad sampling scratch (cornerRadius logical -> physical px; < 0 = capsule).
    float crPx = cornerRadius < 0.0f ? -1.0f : cornerRadius * (float)s;
    CN1MetalDrawLens(scratch, x, y, w, h, fw, fh, magnify, aberration, tintColor, tintStrength, crPx);
}

- (BOOL)presentFramebuffer
{
    if (self.renderCommandEncoder == nil) {
        // Nothing was encoded (setFramebuffer was not called after the
        // previous present). Nothing to do. Leave needsResizePresent set: the
        // gap-filler is still wanted because no real frame is being presented.
        self.commandBuffer = nil;
        return NO;
    }
    // A real, correctly-laid-out frame is about to reach the layer, so the
    // post-resize gap-filler is no longer needed; clear the guard so the
    // deferred presentPreservedFrameIfNeeded does not later present the stale
    // stretched frame on top of this one (which would look like a backwards
    // flicker). See updateFrameBufferSize: (#5162/#5171).
    needsResizePresent = NO;
    CN1MetalEndFrame();
    [self.renderCommandEncoder endEncoding];
    self.renderCommandEncoder = nil;
    self.renderPassDescriptor = nil;

    // Acquire the drawable here (not in setFramebuffer) to minimise its
    // dwell time -- holding a drawable across the whole op-encoding phase
    // stalls nextDrawable for subsequent frames.
    CAMetalLayer *layer = (CAMetalLayer*)self.layer;
    id<CAMetalDrawable> dr = [layer nextDrawable];
    if (dr == nil) {
        // Memory pressure dropped the drawable. Commit render work so
        // screenTexture still updates; skip this frame's present.
        [self.commandBuffer commit];
        self.commandBuffer = nil;
        return NO;
    }
    self.drawable = dr;
    id<MTLBlitCommandEncoder> blit = [self.commandBuffer blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture
              sourceSlice:0 sourceLevel:0
             sourceOrigin:MTLOriginMake(0, 0, 0)
               sourceSize:MTLSizeMake(framebufferWidth, framebufferHeight, 1)
                toTexture:dr.texture
         destinationSlice:0 destinationLevel:0
        destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [self.commandBuffer presentDrawable:dr];
    [self.commandBuffer commit];
    self.drawable = nil;
    self.commandBuffer = nil;
    return YES;
}

/**
 * User clicked Done or Next button above the keyboard
 */
-(void) keyboardDoneClicked {
    if(editingComponent != nil) {
        if([editingComponent isKindOfClass:[UITextView class]]) {
            stringEdit(YES, -2, ((UITextView*)editingComponent).text);
        } else {
            stringEdit(YES, -2, ((UITextField*)editingComponent).text);
        }
        if(isVKBAlwaysOpen()) {
            com_codename1_impl_ios_IOSImplementation_foldKeyboard__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        } else {
            [editingComponent resignFirstResponder];
            [editingComponent removeFromSuperview];
#ifndef CN1_USE_ARC
            [editingComponent release];
#endif
            editingComponent = nil;
        }
        repaintUI();
        
    }
}

/**
 * User clicked Done or Next button above the keyboard
 */
-(void) keyboardNextClicked {
    if(editingComponent != nil) {
        if([editingComponent isKindOfClass:[UITextView class]]) {
            stringEdit(YES, -2, ((UITextView*)editingComponent).text);
        } else {
            stringEdit(YES, -2, ((UITextField*)editingComponent).text);
        }
        if(isVKBAlwaysOpen()) {
            com_codename1_impl_ios_TextEditUtil_editNextTextArea__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        } else {
            [editingComponent resignFirstResponder];
            [editingComponent removeFromSuperview];
#ifndef CN1_USE_ARC
            [editingComponent release];
#endif
            editingComponent = nil;
        }
        repaintUI();
        
    }
}

-(void)textViewDidChange:(UITextView *)textView {
    if(editingComponent.hidden) {
        editingComponent.hidden = NO;
        com_codename1_impl_ios_IOSImplementation_showTextEditorAgain__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    }
    if([editingComponent isKindOfClass:[UITextView class]]) {
        stringEdit(NO, -1, ((UITextView*)editingComponent).text);
    } else {
        stringEdit(NO, -1, ((UITextField*)editingComponent).text);
    }
    com_codename1_impl_ios_IOSImplementation_resizeNativeTextComponentCallback__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

-(void)textFieldDidChange {
    if(editingComponent.hidden) {
        editingComponent.hidden = NO;
        com_codename1_impl_ios_IOSImplementation_showTextEditorAgain__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    }
    if([editingComponent isKindOfClass:[UITextView class]]) {
        stringEdit(NO, -1, ((UITextView*)editingComponent).text);
    } else {
        stringEdit(NO, -1, ((UITextField*)editingComponent).text);
    }
    
    com_codename1_impl_ios_IOSImplementation_resizeNativeTextComponentCallback__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

extern int currentlyEditingMaxLength;
extern BOOL currentlyReturnExitsEditing;
- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
    NSUInteger newLength = (textField.text.length - range.length) + string.length;
    return (newLength <= currentlyEditingMaxLength);
}

-(BOOL)textView:(UITextView *)textView shouldChangeTextInRange:(NSRange)range replacementText:(NSString *)text {
    // iosReturnExitsEditing: treat a Return keypress on a multi-line text view as Done.
    // Only intercept a single "\n" replacement so pasted text containing newlines is
    // unaffected.
    if (currentlyReturnExitsEditing && [text isEqualToString:@"\n"]) {
        [self keyboardDoneClicked];
        return NO;
    }
    NSUInteger newLength = (textView.text.length - range.length) + text.length;
    return (newLength <= currentlyEditingMaxLength);
}


- (BOOL)textFieldShouldReturn:(UITextField *)theTextField {
    if(editingComponent != nil) {
        if([editingComponent isKindOfClass:[UITextView class]]) {
            stringEdit(YES, -2, ((UITextView*)editingComponent).text);
        } else {
            stringEdit(YES, -2, ((UITextField*)editingComponent).text);
        }
        //if there is one then goto the edit next textarea
        //com_codename1_impl_ios_TextEditUtil_editNextTextArea__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        if(isVKBAlwaysOpen()) {
            com_codename1_impl_ios_TextEditUtil_editNextTextArea__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        } else {
            [editingComponent resignFirstResponder];
            [editingComponent removeFromSuperview];
#ifndef CN1_USE_ARC
            [editingComponent release];
#endif
            editingComponent = nil;
        }
        repaintUI();
    }
    return YES;
}



-(void)layoutSubviews
{
    if (firstTime){
        [self deleteFramebuffer];
        firstTime=NO;
    }
    // Keep the Metal drawable + projection in sync with the actual runtime
    // view size. initWithCoder runs with the xib's default size (often the
    // legacy 320x480 placeholder), so without this the projection stays
    // scaled to that default and anything drawn outside those bounds gets
    // clipped at NDC edges -- the Form ends up only covering a portion of
    // the screen.
    CGSize sz = self.bounds.size;
    CGFloat s = self.contentScaleFactor;
    int w = (int)(sz.width * s);
    int h = (int)(sz.height * s);
    if (w > 0 && h > 0) {
        [self updateFrameBufferSize:w h:h];
    }
    [super layoutSubviews];
}

/*-(void)drawRect:(CGRect)rect {
 [[CodenameOne_GLViewController instance] drawFrame:rect];
 }*/


@end
#endif