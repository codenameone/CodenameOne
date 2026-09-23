#!/usr/bin/env python3
"""Run the native macOS alias resolver and font binding against real AppKit."""
import pathlib
import subprocess
import tempfile
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]


class MacNativeFontTest(unittest.TestCase):
    def test_native_aliases_use_appkit_family_weights_and_italics(self):
        source = (ROOT / "Ports/iOSPort/nativeSources/IOSNative.m").read_text()
        start = source.index("static NSFont *cn1MacSystemFontForAlias(")
        end = source.index("\n#endif", start)
        resolver = source[start:end]
        start = source.index("JAVA_LONG com_codename1_impl_ios_IOSNative_createTruetypeFont___java_lang_String(")
        end = source.index("\nJAVA_LONG ", start + 1)
        binding = source[start:end]
        harness = r'''
#import <AppKit/AppKit.h>
#include <assert.h>
#define CN1_THREAD_STATE_MULTI_ARG
#define CN1_THREAD_STATE_PASS_ARG
#define JAVA_LONG intptr_t
#define JAVA_OBJECT id
#define CN1_USE_ARC 1
#define POOL_BEGIN()
#define POOL_END()
#define BRIDGE_CAST __bridge
#define toNSString(value) ((NSString *)(value))
#define UIFontWeightUltraLight NSFontWeightUltraLight
#define UIFontWeightLight NSFontWeightLight
#define UIFontWeightMedium NSFontWeightMedium
#define UIFontWeightBold NSFontWeightBold
#define UIFontWeightHeavy NSFontWeightHeavy
typedef NSFont CN1Font;
static int scaleValue = 1;
static int registrations = 0;
static int isIOS8_2(void) { return 1; }
static void cn1RegisterBundledFontsOnce(void) { registrations++; }
RESOLVER
BINDING
int main(void) {
    @autoreleasepool {
        NSArray *weights = @[@"Thin", @"Light", @"Regular", @"Bold", @"Black"];
        CGFloat values[] = {NSFontWeightThin, NSFontWeightLight, NSFontWeightRegular,
                            NSFontWeightBold, NSFontWeightBlack};
        for (NSUInteger i = 0; i < [weights count]; i++) {
            for (int italic = 0; italic <= 1; italic++) {
                NSString *alias = [NSString stringWithFormat:@"native:%@%@",
                                   italic ? @"Italic" : @"Main", weights[i]];
                for (int size = 11; size <= 22; size += 11) {
                    NSFont *actual = cn1MacSystemFontForAlias(alias, size);
                    intptr_t peer = com_codename1_impl_ios_IOSNative_createTruetypeFont___java_lang_String(nil, alias);
                    NSFont *loaded = (__bridge NSFont *)(void *)peer;
                    assert([loaded.fontName isEqualToString:actual.fontName]);
                    assert(loaded.pointSize == 14);
                    NSFont *expected = [NSFont systemFontOfSize:size weight:values[i]];
                    if (italic) {
                        expected = [[NSFontManager sharedFontManager] convertFont:expected
                                    toHaveTrait:NSItalicFontMask];
                    }
                    assert(actual != nil);
                    assert([actual.fontName isEqualToString:expected.fontName]);
                    assert(actual.pointSize == size);
                    NSFontTraitMask traits = [[NSFontManager sharedFontManager] traitsOfFont:actual];
                    assert(((traits & NSItalicFontMask) != 0) == italic);
                    assert([actual.familyName isEqualToString:expected.familyName]);
                }
            }
        }
        assert(registrations == 0);
        intptr_t namedPeer = com_codename1_impl_ios_IOSNative_createTruetypeFont___java_lang_String(nil, @"HelveticaNeue-Medium");
        NSFont *named = (__bridge NSFont *)(void *)namedPeer;
        assert([named.fontName isEqualToString:[NSFont systemFontOfSize:14 weight:NSFontWeightMedium].fontName]);
        assert(cn1MacSystemFontForAlias(@"HelveticaNeue-Medium", 14) == nil);
        assert(cn1MacSystemFontForAlias(@"Material Icons", 14) == nil);
        assert(cn1MacSystemFontForAlias(@"native:MainUnknown", 14) == nil);
        assert(cn1MacSystemFontForAlias(nil, 14) == nil);
    }
    return 0;
}
'''
        with tempfile.TemporaryDirectory(prefix="cn1-mac-font-") as directory:
            path = pathlib.Path(directory)
            source_file = path / "font-test.m"
            binary = path / "font-test"
            source_file.write_text(harness.replace("RESOLVER", resolver).replace("BINDING", binding))
            subprocess.run(["xcrun", "clang", "-fobjc-arc", "-Wall", "-Wextra", "-Werror",
                            "-Wno-unused-parameter", "-framework", "AppKit", str(source_file), "-o", str(binary)], check=True)
            subprocess.run([str(binary)], check=True)


if __name__ == "__main__":
    unittest.main()
