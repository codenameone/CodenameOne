// CN1MetalGlyphAtlas.m
//
// Phase 4 implementation. See header for design rationale.

#ifdef CN1_USE_METAL
#import "CN1MetalGlyphAtlas.h"

#define CN1_METAL_ATLAS_INITIAL_W 1024
#define CN1_METAL_ATLAS_INITIAL_H 1024
#define CN1_METAL_ATLAS_MAX_W     2048
#define CN1_METAL_ATLAS_MAX_H     2048
#define CN1_METAL_ATLAS_PADDING   1   // 1px around each glyph for AA bleed

// Forward decl: CN1Metalcompat owns the device. Re-using its accessor avoids
// reaching for MTLCreateSystemDefaultDevice() and getting a different device
// than the one METALView is rendering on.
extern id<MTLDevice> CN1MetalDevice(void);

// Per-(fontName, pointSize) atlas cache. CTFontRef equality isn't a useful
// key (each CTFontCreate returns a distinct ref); the (name, size) pair is
// the natural identity. UIFont with different attributes (bold/italic) has
// a different fontName so it gets its own atlas correctly.
static NSMutableDictionary<NSString *, CN1MetalGlyphAtlas *> *atlasCache = nil;

@implementation CN1MetalGlyphSlot
@end

@interface CN1MetalGlyphAtlas () {
    NSString                                *_fontKey;
    CTFontRef                                _ctFont;
    int                                      _textureWidth;
    int                                      _textureHeight;
    id<MTLTexture>                           _texture;
    NSMutableDictionary<NSNumber *,
                        CN1MetalGlyphSlot *> *_slots;
    // Shelf packer state.
    int                                      _shelfY;       // top of current shelf
    int                                      _shelfHeight;  // tallest glyph on it so far
    int                                      _cursorX;      // next free X within shelf
}
@end

@implementation CN1MetalGlyphAtlas

+ (nullable instancetype)atlasForFont:(nonnull UIFont *)font {
    if (atlasCache == nil) atlasCache = [NSMutableDictionary dictionary];
    NSString *key = [NSString stringWithFormat:@"%@|%g", font.fontName, (double)font.pointSize];
    CN1MetalGlyphAtlas *cached = atlasCache[key];
    if (cached != nil) return cached;
    CN1MetalGlyphAtlas *fresh = [[CN1MetalGlyphAtlas alloc] initWithFont:font key:key];
    if (fresh == nil) return nil;
    atlasCache[key] = fresh;
    return fresh;
}

- (instancetype)initWithFont:(UIFont *)uifont key:(NSString *)key {
    self = [super init];
    if (self == nil) return nil;
    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) return nil;

    _fontKey = [key copy];
    _ctFont = CTFontCreateWithName((__bridge CFStringRef)uifont.fontName, uifont.pointSize, NULL);
    if (_ctFont == NULL) return nil;

    _textureWidth = CN1_METAL_ATLAS_INITIAL_W;
    _textureHeight = CN1_METAL_ATLAS_INITIAL_H;

    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatR8Unorm
        width:(NSUInteger)_textureWidth
        height:(NSUInteger)_textureHeight
        mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    _texture = [device newTextureWithDescriptor:desc];
    if (_texture == nil) {
        CFRelease(_ctFont);
        _ctFont = NULL;
        return nil;
    }

    _slots = [NSMutableDictionary dictionary];
    // Start packing 1px in so adjacent glyphs (or atlas-edge sampler clamp)
    // never sample stale fixed-content from neighbours.
    _shelfY = CN1_METAL_ATLAS_PADDING;
    _shelfHeight = 0;
    _cursorX = CN1_METAL_ATLAS_PADDING;

    return self;
}

- (void)dealloc {
    if (_ctFont != NULL) {
        CFRelease(_ctFont);
        _ctFont = NULL;
    }
}

- (id<MTLTexture>)texture           { return _texture; }
- (int)textureWidth                 { return _textureWidth; }
- (int)textureHeight                { return _textureHeight; }
- (CTFontRef)ctFont                 { return _ctFont; }

- (BOOL)tryGrowAtlas {
    // One-shot grow: 1024 -> 2048. Past that we give up.
    if (_textureWidth >= CN1_METAL_ATLAS_MAX_W && _textureHeight >= CN1_METAL_ATLAS_MAX_H) {
        return NO;
    }
    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) return NO;

    int newW = MIN(_textureWidth * 2, CN1_METAL_ATLAS_MAX_W);
    int newH = MIN(_textureHeight * 2, CN1_METAL_ATLAS_MAX_H);

    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatR8Unorm
        width:(NSUInteger)newW height:(NSUInteger)newH mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    id<MTLTexture> newTex = [device newTextureWithDescriptor:desc];
    if (newTex == nil) return NO;

    // Discard old slots — the next reference to each glyph re-rasterises.
    // Simpler than copying region-by-region and avoids needing a blit
    // command buffer at exactly the wrong moment in the frame.
    [_slots removeAllObjects];
    _shelfY = CN1_METAL_ATLAS_PADDING;
    _shelfHeight = 0;
    _cursorX = CN1_METAL_ATLAS_PADDING;
    _texture = newTex;
    _textureWidth = newW;
    _textureHeight = newH;
    return YES;
}

- (CN1MetalGlyphSlot *)slotForGlyph:(CGGlyph)glyph {
    NSNumber *key = @(glyph);
    CN1MetalGlyphSlot *cached = _slots[key];
    if (cached != nil) return cached;

    // Measure the glyph in font space. Empty advance + empty bbox = skipped
    // glyph (whitespace etc) — record a zero-width slot so we don't
    // re-measure on every reference.
    CGRect bbox = CGRectZero;
    CGSize advance = CGSizeZero;
    CTFontGetBoundingRectsForGlyphs(_ctFont, kCTFontOrientationHorizontal,
                                    &glyph, &bbox, 1);
    CTFontGetAdvancesForGlyphs(_ctFont, kCTFontOrientationHorizontal,
                               &glyph, &advance, 1);

    if (bbox.size.width <= 0 || bbox.size.height <= 0) {
        CN1MetalGlyphSlot *empty = [[CN1MetalGlyphSlot alloc] init];
        empty.atlasX = 0; empty.atlasY = 0;
        empty.width = 0; empty.height = 0;
        empty.bearingX = 0; empty.bearingY = 0;
        empty.advance = (float)advance.width;
        _slots[key] = empty;
        return empty;
    }

    int gw = (int)ceilf((float)bbox.size.width)  + 2 * CN1_METAL_ATLAS_PADDING;
    int gh = (int)ceilf((float)bbox.size.height) + 2 * CN1_METAL_ATLAS_PADDING;

    // Shelf-pack: open a new shelf if current one is too narrow.
    if (_cursorX + gw > _textureWidth - CN1_METAL_ATLAS_PADDING) {
        _shelfY += _shelfHeight + CN1_METAL_ATLAS_PADDING;
        _cursorX = CN1_METAL_ATLAS_PADDING;
        _shelfHeight = 0;
    }
    if (_shelfY + gh > _textureHeight - CN1_METAL_ATLAS_PADDING) {
        // Atlas full. Try grow; if grow fails, refuse the glyph.
        if (![self tryGrowAtlas]) return nil;
        // tryGrowAtlas reset the packer; retry placement.
        if (_cursorX + gw > _textureWidth - CN1_METAL_ATLAS_PADDING ||
            _shelfY + gh > _textureHeight - CN1_METAL_ATLAS_PADDING) {
            return nil;  // single glyph too big even for grown atlas
        }
    }
    if (gh > _shelfHeight) _shelfHeight = gh;

    int slotX = _cursorX;
    int slotY = _shelfY;
    _cursorX += gw + CN1_METAL_ATLAS_PADDING;

    // Rasterise into a local R8 buffer with default Y-up CG. With the
    // glyph drawn at user-y = padding..padding+bbox.height, the top
    // padding row lands at memory_row_0 (empty), the glyph's visual TOP
    // row at memory_row_padding, the visual BOTTOM at memory_row_padding+
    // bbox.height-1, and the bottom padding band at memory_row_(gh-padding..
    // gh-1). memory_row_0 is therefore the slot's TOP edge; CN1MetalDrawString
    // samples this with V=0-at-top texcoords and renders the glyph right-
    // side-up on screen without any V flip.
    size_t bytesPerRow = (size_t)gw;
    void *pixels = calloc((size_t)gh * bytesPerRow, 1);
    if (pixels == NULL) return nil;
    CGContextRef ctx = CGBitmapContextCreate(pixels, (size_t)gw, (size_t)gh, 8,
                                             bytesPerRow, NULL,
                                             (CGBitmapInfo)kCGImageAlphaOnly);
    if (ctx == NULL) {
        free(pixels);
        return nil;
    }
    // Position the glyph so its bbox top-left lands at (padding, padding) in
    // the bitmap. Glyph origin in CG = baseline anchor; bbox.origin gives the
    // bbox's bottom-left relative to the origin. So origin position in bitmap
    // (Y-up) = (padding - bbox.origin.x, padding - bbox.origin.y).
    CGPoint origin = CGPointMake((CGFloat)CN1_METAL_ATLAS_PADDING - bbox.origin.x,
                                 (CGFloat)CN1_METAL_ATLAS_PADDING - bbox.origin.y);
    CTFontDrawGlyphs(_ctFont, &glyph, &origin, 1, ctx);
    CGContextRelease(ctx);

    [_texture replaceRegion:MTLRegionMake2D((NSUInteger)slotX, (NSUInteger)slotY,
                                            (NSUInteger)gw, (NSUInteger)gh)
                mipmapLevel:0
                  withBytes:pixels
                bytesPerRow:bytesPerRow];
    free(pixels);

    CN1MetalGlyphSlot *slot = [[CN1MetalGlyphSlot alloc] init];
    slot.atlasX = slotX;
    slot.atlasY = slotY;
    slot.width = gw;
    slot.height = gh;
    slot.bearingX = (float)bbox.origin.x;
    slot.bearingY = (float)bbox.origin.y;
    slot.advance = (float)advance.width;
    _slots[key] = slot;
    return slot;
}

@end

#endif // CN1_USE_METAL
