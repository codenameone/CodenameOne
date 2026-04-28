// CN1MetalGlyphAtlas.h
//
// Phase 4 of the iOS Metal port. A per-(font, point-size) R8 texture atlas
// of glyph alpha masks. Replaces the whole-string LRU cache that
// CN1Metalcompat.m used in Phase 2.
//
// Why an atlas? The whole-string cache rasterises one MTLTexture per
// (string, font, color) tuple. Long strings, large vocabularies, or
// frequent recolouring all explode the cache and trigger expensive
// per-frame texture rebuilds. A per-glyph atlas amortises rasterisation
// cost across every string that shares a font: the second-and-later use
// of a glyph is a single texture-region sample. Color is decoupled from
// the atlas (atlas is alpha-only); DrawString modulates colour through
// the alpha-mask shader.
//
// Storage: one R8Unorm MTLTexture per (font name + point-size) key,
// shelf-packed lazily. Atlas starts at 1024x1024 and is allowed to grow
// once to 2048x2048 if the first packing fills up. Past that we report
// "atlas full" and the caller skips the glyph (rare in practice).
//
// All entry points must be called from the main thread (where drawFrame
// drains ops). The atlas does not synchronise internally.
//
// Gated by CN1_USE_METAL: this header is only included from
// CN1Metalcompat.m and must not appear in the GL build.

#ifdef CN1_USE_METAL
#import <Foundation/Foundation.h>
#import <CoreText/CoreText.h>
#import <UIKit/UIKit.h>
#import <Metal/Metal.h>

@interface CN1MetalGlyphSlot : NSObject
// Position of the glyph bitmap in the atlas, in atlas pixels (top-left).
@property (nonatomic, assign) int atlasX;
@property (nonatomic, assign) int atlasY;
// Width and height of the rasterised bitmap (includes 1px padding for AA).
@property (nonatomic, assign) int width;
@property (nonatomic, assign) int height;
// Glyph bbox in font space, in CG/CT convention (Y-up, origin = baseline).
// bearingX = bbox.origin.x (left bearing), bearingY = bbox.origin.y (descent
// or, if positive, distance baseline-to-bottom-of-glyph). The TOP of the
// glyph above baseline = bearingY + height-without-padding.
@property (nonatomic, assign) float bearingX;
@property (nonatomic, assign) float bearingY;
// Glyph advance width — kept for sanity; CTLine emits its own positions
// so DrawString does NOT use this for layout.
@property (nonatomic, assign) float advance;
@end

@interface CN1MetalGlyphAtlas : NSObject

// Get-or-create the atlas for the given UIFont. Atlases are keyed on
// (fontName, pointSize) — same UIFont with different colour shares one
// atlas. Returns nil if the underlying MTLDevice isn't ready.
+ (nullable instancetype)atlasForFont:(nonnull UIFont *)font;

// Look up a glyph, rasterising and packing it on first reference. Returns
// nil if the atlas is full and cannot grow further. Slot's `width = 0`
// means "empty glyph" (e.g. space) — callers should skip drawing.
- (nullable CN1MetalGlyphSlot *)slotForGlyph:(CGGlyph)glyph;

// Backing texture (R8Unorm). Shared across all DrawString calls that hit
// this atlas in a given frame.
@property (nonatomic, readonly, nonnull) id<MTLTexture> texture;

// Atlas dimensions in pixels — needed to compute UV from atlasX/atlasY.
@property (nonatomic, readonly) int textureWidth;
@property (nonatomic, readonly) int textureHeight;

// The CTFontRef used for rasterisation and text shaping. Owned by the
// atlas; do NOT CFRelease.
@property (nonatomic, readonly, nonnull) CTFontRef ctFont;

@end

#endif // CN1_USE_METAL
