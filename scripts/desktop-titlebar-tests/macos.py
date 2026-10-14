#!/usr/bin/env python3
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
"""Local AppKit check of production title-control geometry; no windows are shown."""
import pathlib
import subprocess
import tempfile

root = pathlib.Path(__file__).resolve().parents[2]
source = (root / "Ports/MacPort/nativeSources/CN1MacChrome.m").read_text()
start = source.index("static int cn1TitleInsets[5];")
end = source.index("void CN1MacHostSetWindowUndecorated", start)
harness = r'''
#import <AppKit/AppKit.h>
#include <assert.h>
#define CODENAME_ONE_THREAD_STATE void* state
#define JAVA_OBJECT void*
#define JAVA_NULL NULL
typedef struct { int length; int data[5]; } Array;
#define JAVA_ARRAY Array*
#define CN1_ARRAY_DATA(a) (((Array*)a)->data)
@interface CN1MacHost : NSObject
@property(nonatomic, strong) NSWindow* window;
@property(nonatomic, strong) NSView* renderingView;
+ (instancetype)sharedHost;
@end
@implementation CN1MacHost
+ (instancetype)sharedHost { static CN1MacHost* h; if (!h) h = [CN1MacHost new]; return h; }
@end
PRODUCTION
int main(void) {
    @autoreleasepool {
        [NSApplication sharedApplication];
        CN1MacHost* host = [CN1MacHost sharedHost];
        host.window = [[NSWindow alloc] initWithContentRect:NSMakeRect(0,0,800,600)
                styleMask:NSWindowStyleMaskTitled | NSWindowStyleMaskClosable |
                NSWindowStyleMaskMiniaturizable | NSWindowStyleMaskResizable |
                NSWindowStyleMaskFullSizeContentView backing:NSBackingStoreBuffered defer:NO];
        host.window.titlebarAppearsTransparent = YES;
        host.window.titleVisibility = NSWindowTitleHidden;
        host.renderingView = host.window.contentView;
        cn1MacUpdateTitleInsets();
        CGFloat scale = host.window.backingScaleFactor;
        assert(cn1TitleInsets[0] > 60 * scale && cn1TitleInsets[0] < 130 * scale);
        assert(cn1TitleInsets[1] == 0);
        assert(cn1TitleInsets[4] > 10 * scale && cn1TitleInsets[4] < 60 * scale);
        Array result = {5, {0}};
        com_codename1_impl_mac_MacNative_macTitleBarInsets___int_1ARRAY(NULL,NULL,&result);
        assert(result.data[0] == cn1TitleInsets[0]);
        for (NSNumber* type in @[@(NSWindowCloseButton), @(NSWindowMiniaturizeButton), @(NSWindowZoomButton)])
            [host.window standardWindowButton:type.integerValue].hidden = YES;
        cn1MacUpdateTitleInsets();
        assert(cn1TitleInsets[0] == 0 && cn1TitleInsets[4] == 0);
        puts("APPKIT_TITLEBAR_INSETS_OK");
    }
}
'''.replace("PRODUCTION", source[start:end])
with tempfile.TemporaryDirectory(prefix="cn1-titlebar-appkit-") as directory:
    path = pathlib.Path(directory)
    (path / "test.m").write_text(harness)
    subprocess.run(["xcrun", "clang", "-fobjc-arc", "-fblocks", "-framework", "AppKit",
                    str(path / "test.m"), "-o", str(path / "test")], check=True)
    subprocess.run([str(path / "test")], check=True)
