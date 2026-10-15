#!/usr/bin/env python3
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
"""Local production text-state/geometry publication check (macOS + Xcode).

Uses real Foundation strings and the main dispatch queue. Only UIKit's screen
scale and the Java publication callback are stubbed; no simulator is launched.
"""
import os
from pathlib import Path
import subprocess
import tempfile

if os.environ.get("CI"):
    raise SystemExit("Native checks are local only")
root = Path(__file__).resolve().parents[3]
source = (root / "Ports/iOSPort/nativeSources/CN1TextInputView.m").read_text()
start = source.index("- (BOOL)cn1SyncText:")
end = source.index("- (void)cn1ResetComposition", start)
bridge_start = source.index("void com_codename1_impl_ios_IOSNative_updateTextInputState_")
bridge_end = source.index("void com_codename1_impl_ios_IOSNative_stopTextInput__", bridge_start)
harness = r'''
#import <Foundation/Foundation.h>
#import <CoreGraphics/CoreGraphics.h>
#include <assert.h>
#define CN1_THREAD_STATE_MULTI_ARG
#define CN1_THREAD_GET_STATE_PASS_ARG
#define JAVA_OBJECT NSString*
#define JAVA_INT int
#define JAVA_BOOLEAN BOOL
@interface UIScreen : NSObject
+ (instancetype)mainScreen;
- (CGFloat)scale;
@end
@implementation UIScreen
+ (instancetype)mainScreen { static UIScreen* screen; if (!screen) screen = [UIScreen new]; return screen; }
- (CGFloat)scale { return 2; }
@end
@interface CN1TextInputView : NSObject {
@public
    NSMutableString* _shadow;
    NSInteger _editSeq;
    NSRange _markedRange;
    NSRange _selectedRange;
    CGRect _caretRectPoints;
}
@end
@implementation CN1TextInputView
/*SYNC*/
@end
static CN1TextInputView* cn1TextInputView;
static NSString* toNSString(NSString* text) { return text; }
static int published;
static BOOL accepted;
static NSString* expectedText;
static NSRange expectedSelection;
void com_codename1_impl_ios_IOSImplementation_tiPublishGeometry___int_boolean(int captureId, BOOL matches) {
    assert([NSThread isMainThread]);
    published = captureId;
    accepted = matches;
    if (matches) {
        // This is when Java publishes the capture: the native shadow must already match.
        assert([cn1TextInputView->_shadow isEqualToString:expectedText]);
        assert(NSEqualRanges(cn1TextInputView->_selectedRange, expectedSelection));
    }
}
/*BRIDGE*/
static void drainUntil(int captureId) {
    NSDate* deadline = [NSDate dateWithTimeIntervalSinceNow:2];
    while (published != captureId && [deadline timeIntervalSinceNow] > 0)
        [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.01]];
    assert(published == captureId);
}
int main(void) {
    @autoreleasepool {
        cn1TextInputView = [CN1TextInputView new];
        cn1TextInputView->_shadow = [@"old" mutableCopy];
        cn1TextInputView->_selectedRange = NSMakeRange(1, 0);
        cn1TextInputView->_markedRange = NSMakeRange(NSNotFound, 0);
        cn1TextInputView->_editSeq = 4;
        expectedText = @"new document";
        expectedSelection = NSMakeRange(2, 3);
        com_codename1_impl_ios_IOSNative_updateTextInputState___java_lang_String_int_int_int_int_int_int_int_int(
                nil, expectedText, 2, 5, 10, 20, 4, 16, 4, 71);
        assert(published == 0 && [cn1TextInputView->_shadow isEqualToString:@"old"]);
        drainUntil(71);
        assert(accepted);
        assert(CGRectEqualToRect(cn1TextInputView->_caretRectPoints, CGRectMake(5, 10, 2, 8)));
        // A newer native keystroke makes an older Java echo stale, geometry included.
        cn1TextInputView->_editSeq = 5;
        com_codename1_impl_ios_IOSNative_updateTextInputState___java_lang_String_int_int_int_int_int_int_int_int(
                nil, @"stale", 0, 0, 80, 90, 4, 16, 4, 72);
        drainUntil(72);
        assert(!accepted && [cn1TextInputView->_shadow isEqualToString:expectedText]);
        assert(CGRectEqualToRect(cn1TextInputView->_caretRectPoints, CGRectMake(5, 10, 2, 8)));
        cn1TextInputView->_markedRange = NSMakeRange(2, 3);
        assert(![cn1TextInputView cn1SyncText:@"other" selStart:2 selEnd:5 caretRect:CGRectZero seq:5]);
        // A matching IME echo may update geometry without replacing the marked text.
        assert([cn1TextInputView cn1SyncText:expectedText selStart:2 selEnd:5
                caretRect:CGRectMake(20, 30, 4, 16) seq:5]);
        assert(NSEqualRanges(cn1TextInputView->_markedRange, NSMakeRange(2, 3)));
        assert(CGRectEqualToRect(cn1TextInputView->_caretRectPoints, CGRectMake(10, 15, 2, 8)));
        puts("TEXT_INPUT_STATE_GEOMETRY_OK");
    }
}
'''.replace("/*SYNC*/", source[start:end]).replace("/*BRIDGE*/", source[bridge_start:bridge_end])
with tempfile.TemporaryDirectory(prefix="cn1-text-input-state-") as directory:
    path = Path(directory)
    (path / "test.m").write_text(harness)
    subprocess.run(["xcrun", "clang", "-fobjc-arc", "-fblocks", "-Wall", "-Werror",
                    "-framework", "Foundation", "-framework", "CoreGraphics",
                    str(path / "test.m"), "-o", str(path / "test")], check=True)
    subprocess.run([str(path / "test")], check=True, timeout=10)
