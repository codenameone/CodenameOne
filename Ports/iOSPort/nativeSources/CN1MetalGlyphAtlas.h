// CN1MetalGlyphAtlas.h
//
// Phase 4 of the iOS Metal port. Per-(font, point-size) glyph atlas: each
// CGGlyph rasterised once into an MTLTexture region (R8 alpha for normal
// fonts, premultiplied BGRA8 for colour/emoji fonts) and reused across
// every DrawString that hits the same font.
//
// CN1MetalDrawString uses this to amortise the per-frame texture rebuild
// cost of the Phase-2 whole-string LRU cache. For monochrome fonts the
// atlas stores alpha only (R8) and colourisation happens in
// cn1_fs_alpha_mask at draw time. For COLOR fonts (Apple Color Emoji and
// other sbix/COLR fonts, isColor == YES) the atlas stores premultiplied
// BGRA and DrawString draws it through the TexturedRGBA pipeline without
// tinting — emoji keep their own colours.
//
// Single-threaded — all entry points run on the main thread inside
// drawFrame's op drain.
//
// This header MUST be `#import "CN1ES2compat.h"`-ed before its body so
// the CN1_USE_METAL macro is visible (the PCH does not include it; see
// METALView.h for the same pattern).

#import "CN1ES2compat.h"
#ifdef CN1_USE_METAL
#import <Foundation/Foundation.h>
#import <CoreText/CoreText.h>
#import <UIKit/UIKit.h>
#import <Metal/Metal.h>

@interface CN1MetalGlyphSlot : NSObject
@property (nonatomic, assign) int   atlasX;
@property (nonatomic, assign) int   atlasY;
@property (nonatomic, assign) int   width;       // includes 2px padding for AA bleed
@property (nonatomic, assign) int   height;      // includes 2px padding for AA bleed
@property (nonatomic, assign) float bearingX;    // bbox.origin.x — left bearing in font space
@property (nonatomic, assign) float bearingY;    // bbox.origin.y — distance baseline → bbox bottom (CT y-up)
@property (nonatomic, assign) float bboxWidth;   // bbox.size.width  (no padding)
@property (nonatomic, assign) float bboxHeight;  // bbox.size.height (no padding)
@end

@interface CN1MetalGlyphAtlas : NSObject

// Get-or-create the atlas for the given UIFont. Atlases are cached on
// (PostScript name, pointSize). Returns nil if the underlying MTLDevice is
// unavailable or the CTFont can't be created — DrawString then falls
// back to the whole-string path.
+ (nullable instancetype)atlasForFont:(nonnull UIFont *)font;

// Get-or-create the atlas for a specific CTFont. CN1MetalDrawString uses
// this with the font CoreText actually resolved for each glyph run, so a
// run that font-substitutes to (say) Apple Color Emoji rasterises its
// glyph ids against THAT font instead of the base text font — fixing the
// "wrong glyph / random CJK" corruption that happened when substituted-run
// glyph ids were rasterised against the base font's glyph table. The
// CTFont is retained for the atlas's lifetime.
+ (nullable instancetype)atlasForCTFont:(nonnull CTFontRef)ctFont;

// Look up a glyph, rasterising and packing it on first reference.
// Returns nil if the atlas is full and cannot grow further. Slot's
// `width = 0` signals an empty glyph (e.g. space) — callers should
// skip the quad and just consume the run advance.
- (nullable CN1MetalGlyphSlot *)slotForGlyph:(CGGlyph)glyph;

// Backing texture (R8 for monochrome fonts, premultiplied BGRA8 when
// isColor) and its current dimensions.
@property (nonatomic, readonly, nonnull) id<MTLTexture> texture;
@property (nonatomic, readonly) int textureWidth;
@property (nonatomic, readonly) int textureHeight;

// YES when the backing font has colour glyph tables (sbix/COLR, e.g. Apple
// Color Emoji). DrawString then samples the BGRA atlas through the
// TexturedRGBA pipeline instead of colourising an alpha mask.
@property (nonatomic, readonly) BOOL isColor;

// CTFontRef used for shaping AND rasterisation. Owned by the atlas.
@property (nonatomic, readonly, nonnull) CTFontRef ctFont;

@end

#endif // CN1_USE_METAL
