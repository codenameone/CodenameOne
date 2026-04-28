// CN1MetalGlyphAtlas.m
//
// Phase 4 implementation. See header for design rationale.
//
// Rasterisation pattern: CGBitmapContext with DeviceGray colorspace +
// kCGImageAlphaNone + white fill. CTFontDrawGlyphs in default Y-up CG
// renders the glyph into the bitmap such that memory_row_0 holds the
// padded TOP of the slot and memory_row_(gh-1) the padded BOTTOM —
// i.e. right-side-up in raster memory order, ready for V=0-at-top
// sampling.

#import "CN1ES2compat.h"
#ifdef CN1_USE_METAL
#import "CN1MetalGlyphAtlas.h"

#define CN1_METAL_ATLAS_INITIAL_W 1024
#define CN1_METAL_ATLAS_INITIAL_H 1024
#define CN1_METAL_ATLAS_MAX_W     2048
#define CN1_METAL_ATLAS_MAX_H     2048
#define CN1_METAL_ATLAS_PADDING   1
#define CN1_METAL_ATLAS_GLYPH_MAX 256  // sanity limit on per-glyph bitmap dim

extern id<MTLDevice> CN1MetalDevice(void);

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
    int                                      _shelfY;
    int                                      _shelfHeight;
    int                                      _cursorX;
}
@end

@implementation CN1MetalGlyphAtlas

+ (nullable instancetype)atlasForFont:(nonnull UIFont *)font {
    static int s_top = 0;
    if (s_top < 100) { NSLog(@"CN1SS:METAL_DIAG atlasForFont top #%d", s_top); s_top++; }
    if (atlasCache == nil) atlasCache = [NSMutableDictionary dictionary];
    static int s_keyPre = 0;
    if (s_keyPre < 100) { NSLog(@"CN1SS:METAL_DIAG atlasForFont keyPre #%d fontName=%@ pt=%g", s_keyPre, font.fontName, (double)font.pointSize); s_keyPre++; }
    NSString *key = [NSString stringWithFormat:@"%@|%g", font.fontName, (double)font.pointSize];
    static int s_keyPost = 0;
    if (s_keyPost < 100) { NSLog(@"CN1SS:METAL_DIAG atlasForFont keyPost #%d key=%@", s_keyPost, key); s_keyPost++; }
    CN1MetalGlyphAtlas *cached = atlasCache[key];
    static int s_lookup = 0;
    if (s_lookup < 100) { NSLog(@"CN1SS:METAL_DIAG atlasForFont lookup #%d hit=%d", s_lookup, cached != nil); s_lookup++; }
    if (cached != nil) return cached;
    NSLog(@"CN1SS:METAL_DIAG atlasForFont enter key=%@", key);
    CN1MetalGlyphAtlas *fresh = [[CN1MetalGlyphAtlas alloc] initWithFont:font key:key];
    NSLog(@"CN1SS:METAL_DIAG atlasForFont %@ key=%@", fresh ? @"created" : @"FAILED", key);
    if (fresh == nil) return nil;
    atlasCache[key] = fresh;
    return fresh;
}

- (instancetype)initWithFont:(UIFont *)uifont key:(NSString *)key {
    self = [super init];
    if (self == nil) return nil;
    NSLog(@"CN1SS:METAL_DIAG atlas init step=device key=%@", key);
    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) { NSLog(@"CN1SS:METAL_DIAG atlas init FAIL: device=nil"); return nil; }

    _fontKey = [key copy];
    NSLog(@"CN1SS:METAL_DIAG atlas init step=ctfont name=%@ size=%g", uifont.fontName, (double)uifont.pointSize);
    _ctFont = CTFontCreateWithName((__bridge CFStringRef)uifont.fontName, uifont.pointSize, NULL);
    if (_ctFont == NULL) { NSLog(@"CN1SS:METAL_DIAG atlas init FAIL: ctFont=NULL"); return nil; }

    _textureWidth = CN1_METAL_ATLAS_INITIAL_W;
    _textureHeight = CN1_METAL_ATLAS_INITIAL_H;

    NSLog(@"CN1SS:METAL_DIAG atlas init step=texture %dx%d", _textureWidth, _textureHeight);
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatR8Unorm
        width:(NSUInteger)_textureWidth
        height:(NSUInteger)_textureHeight
        mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    _texture = [device newTextureWithDescriptor:desc];
    if (_texture == nil) {
        NSLog(@"CN1SS:METAL_DIAG atlas init FAIL: newTextureWithDescriptor=nil");
        CFRelease(_ctFont);
        _ctFont = NULL;
        return nil;
    }
    NSLog(@"CN1SS:METAL_DIAG atlas init done key=%@", key);

    _slots = [NSMutableDictionary dictionary];
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

- (id<MTLTexture>)texture { return _texture; }
- (int)textureWidth      { return _textureWidth; }
- (int)textureHeight     { return _textureHeight; }
- (CTFontRef)ctFont      { return _ctFont; }

- (BOOL)tryGrowAtlas {
    if (_textureWidth >= CN1_METAL_ATLAS_MAX_W && _textureHeight >= CN1_METAL_ATLAS_MAX_H) return NO;
    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) return NO;

    int newW = MIN(_textureWidth * 2,  CN1_METAL_ATLAS_MAX_W);
    int newH = MIN(_textureHeight * 2, CN1_METAL_ATLAS_MAX_H);

    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatR8Unorm
        width:(NSUInteger)newW height:(NSUInteger)newH mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    id<MTLTexture> newTex = [device newTextureWithDescriptor:desc];
    if (newTex == nil) return NO;

    // Drop slots; next reference re-rasterises into the larger atlas.
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

    static int slotCount = 0;
    if (slotCount < 800) {
        NSLog(@"CN1SS:METAL_DIAG slotForGlyph #%d glyph=%u key=%@", slotCount, (unsigned)glyph, _fontKey);
        slotCount++;
    }

    CGRect bbox = CGRectZero;
    CGSize advance = CGSizeZero;
    CTFontGetBoundingRectsForGlyphs(_ctFont, kCTFontOrientationHorizontal, &glyph, &bbox, 1);
    CTFontGetAdvancesForGlyphs(_ctFont, kCTFontOrientationHorizontal, &glyph, &advance, 1);

    // Empty glyph (space, control char, etc): record zero-width slot so
    // subsequent calls hit the cache fast and DrawString just consumes
    // the advance without emitting a quad.
    if (bbox.size.width <= 0 || bbox.size.height <= 0) {
        CN1MetalGlyphSlot *empty = [[CN1MetalGlyphSlot alloc] init];
        empty.atlasX = 0; empty.atlasY = 0;
        empty.width = 0;  empty.height = 0;
        empty.bearingX = 0; empty.bearingY = 0;
        empty.bboxWidth = 0; empty.bboxHeight = 0;
        _slots[key] = empty;
        return empty;
    }

    int gw = (int)ceilf((float)bbox.size.width)  + 2 * CN1_METAL_ATLAS_PADDING;
    int gh = (int)ceilf((float)bbox.size.height) + 2 * CN1_METAL_ATLAS_PADDING;
    // Refuse oversized glyphs: protects the bitmap allocation if a font
    // returns absurd metrics. A 256x256 cap is plenty for system text.
    if (gw > CN1_METAL_ATLAS_GLYPH_MAX || gh > CN1_METAL_ATLAS_GLYPH_MAX) return nil;

    // Shelf-pack: open a new shelf if current one is too narrow to fit.
    if (_cursorX + gw > _textureWidth - CN1_METAL_ATLAS_PADDING) {
        _shelfY += _shelfHeight + CN1_METAL_ATLAS_PADDING;
        _cursorX = CN1_METAL_ATLAS_PADDING;
        _shelfHeight = 0;
    }
    if (_shelfY + gh > _textureHeight - CN1_METAL_ATLAS_PADDING) {
        if (![self tryGrowAtlas]) return nil;
        if (_cursorX + gw > _textureWidth - CN1_METAL_ATLAS_PADDING ||
            _shelfY + gh > _textureHeight - CN1_METAL_ATLAS_PADDING) {
            return nil;
        }
    }
    if (gh > _shelfHeight) _shelfHeight = gh;

    int slotX = _cursorX;
    int slotY = _shelfY;
    _cursorX += gw + CN1_METAL_ATLAS_PADDING;

    // Rasterise into a local R8 buffer using DeviceGray + kCGImageAlphaNone
    // + white fill. CTFontDrawGlyphs renders the glyph paths in white
    // (== 0xff in the R8 pixel) on a black (== 0x00) background; sampled
    // through cn1_fs_alpha_mask the .r channel becomes alpha coverage.
    //
    // Y-up CG default: drawing the glyph at user origin
    // (padding - bearingX, padding - bearingY) places the bbox at user
    // x ∈ [padding, padding + bbox.width], y ∈ [padding, padding + bbox.height].
    // Memory layout (Apple bitmap convention: memory_row_0 at TOP of bitmap)
    // maps user-y=padding+bbox.height → memory_row_(padding-1) and
    // user-y=padding → memory_row_(gh-1-padding); i.e. the glyph occupies
    // memory rows [padding-1 .. gh-1-padding] right-side-up. memory_row_0
    // is the TOP padding band, memory_row_(gh-1) the BOTTOM padding band.
    size_t bytesPerRow = (size_t)gw;
    void *pixels = calloc((size_t)gh * bytesPerRow, 1);
    if (pixels == NULL) return nil;
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceGray();
    CGContextRef ctx = CGBitmapContextCreate(pixels, (size_t)gw, (size_t)gh, 8,
                                             bytesPerRow, cs,
                                             (CGBitmapInfo)kCGImageAlphaNone);
    CGColorSpaceRelease(cs);
    if (ctx == NULL) {
        free(pixels);
        return nil;
    }
    CGContextSetGrayFillColor(ctx, 1.0, 1.0);

    CGPoint origin = CGPointMake((CGFloat)CN1_METAL_ATLAS_PADDING - bbox.origin.x,
                                 (CGFloat)CN1_METAL_ATLAS_PADDING - bbox.origin.y);
    static int drawCount = 0;
    if (drawCount < 800) {
        NSLog(@"CN1SS:METAL_DIAG CTFontDrawGlyphs entering #%d glyph=%u gw=%d gh=%d key=%@",
              drawCount, (unsigned)glyph, gw, gh, _fontKey);
    }
    CTFontDrawGlyphs(_ctFont, &glyph, &origin, 1, ctx);
    if (drawCount < 800) {
        NSLog(@"CN1SS:METAL_DIAG CTFontDrawGlyphs returned #%d", drawCount);
        drawCount++;
    }
    CGContextRelease(ctx);
    static int uploadCount = 0;
    if (uploadCount < 800) {
        NSLog(@"CN1SS:METAL_DIAG replaceRegion entering #%d at (%d,%d %dx%d)",
              uploadCount, slotX, slotY, gw, gh);
    }

    [_texture replaceRegion:MTLRegionMake2D((NSUInteger)slotX, (NSUInteger)slotY,
                                            (NSUInteger)gw, (NSUInteger)gh)
                mipmapLevel:0
                  withBytes:pixels
                bytesPerRow:bytesPerRow];
    if (uploadCount < 800) {
        NSLog(@"CN1SS:METAL_DIAG replaceRegion returned #%d", uploadCount);
        uploadCount++;
    }
    free(pixels);

    CN1MetalGlyphSlot *slot = [[CN1MetalGlyphSlot alloc] init];
    slot.atlasX = slotX;
    slot.atlasY = slotY;
    slot.width = gw;
    slot.height = gh;
    slot.bearingX = (float)bbox.origin.x;
    slot.bearingY = (float)bbox.origin.y;
    slot.bboxWidth = (float)bbox.size.width;
    slot.bboxHeight = (float)bbox.size.height;
    _slots[key] = slot;
    return slot;
}

@end

#endif // CN1_USE_METAL
