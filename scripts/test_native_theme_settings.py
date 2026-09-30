#!/usr/bin/env python3
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
"""Local native-theme bridge checks: AppKit runtime, UIKit and Windows compilation."""
import pathlib
import shutil
import shlex
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]


class NativeThemeSettingsTest(unittest.TestCase):
    def test_appkit_snapshot_and_notifications(self):
        source = (ROOT / "Ports/iOSPort/nativeSources/IOSNative.m").read_text()
        start = source.index("static void cn1NativeThemeDidChange(")
        end = source.index("JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isLargerTextEnabled", start)
        bridge = source[start:end]
        harness = r'''
#import <AppKit/AppKit.h>
#include <assert.h>
#include <math.h>
#define TARGET_OS_OSX 1
#define TARGET_OS_WATCH 0
#define TARGET_OS_TV 0
#define CN1_THREAD_STATE_MULTI_ARG
#define CN1_THREAD_STATE_PASS_ARG
#define CN1_THREAD_GET_STATE_PASS_SINGLE_ARG
#define JAVA_OBJECT id
#define POOL_BEGIN()
#define POOL_END()
static float scaleValue = 2;
static int notifications;
static void com_codename1_impl_ios_IOSImplementation_nativeThemeSettingsChanged__(void) { notifications++; }
static id fromNSString(NSString* value) { return [[value copy] autorelease]; }
BRIDGE
int main(void) {
    @autoreleasepool {
        NSString* settings = com_codename1_impl_ios_IOSNative_nativeThemeSettings___R_java_lang_String(nil);
        assert([settings containsString:@"fontFamily=native:\n"]);
        NSString* size = [NSString stringWithFormat:@"fontSize=%g\n", [NSFont systemFontSize] * 2];
        assert([settings containsString:size]);
        assert([settings containsString:@"accent-color="]);
        assert([settings containsString:@"accent-color-dark="]);
        assert([settings containsString:@"text-color="]);
        assert([settings containsString:@"text-color-dark="]);
        [[NSNotificationCenter defaultCenter] postNotificationName:NSSystemColorsDidChangeNotification object:nil];
        assert(notifications == 1);
        // Repeated snapshots must not install duplicate observers.
        com_codename1_impl_ios_IOSNative_nativeThemeSettings___R_java_lang_String(nil);
        [[NSNotificationCenter defaultCenter] postNotificationName:NSSystemColorsDidChangeNotification object:nil];
        assert(notifications == 2);
        puts("AppKit native theme snapshot and notification checks passed");
    }
    return 0;
}
'''.replace("BRIDGE", bridge)
        with tempfile.TemporaryDirectory(prefix="cn1-theme-settings-") as directory:
            path = pathlib.Path(directory)
            source_path = path / "settings.m"
            source_path.write_text(harness)
            executable = path / "settings"
            subprocess.run(["xcrun", "clang", "-fblocks", "-framework", "AppKit",
                            str(source_path), "-o", str(executable)], check=True)
            subprocess.run([str(executable)], check=True)

    def test_uikit_bridge_compiles(self):
        source = (ROOT / "Ports/iOSPort/nativeSources/IOSNative.m").read_text()
        start = source.index("static void cn1NativeThemeDidChange(")
        end = source.index("JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isLargerTextEnabled", start)
        prelude = """
#import <UIKit/UIKit.h>
#include <math.h>
#define CN1_THREAD_STATE_MULTI_ARG
#define CN1_THREAD_STATE_PASS_ARG
#define CN1_THREAD_GET_STATE_PASS_SINGLE_ARG
#define JAVA_OBJECT id
#define POOL_BEGIN()
#define POOL_END()
static float scaleValue = 2;
static void com_codename1_impl_ios_IOSImplementation_nativeThemeSettingsChanged__(void) {}
static id fromNSString(NSString* value) { return value; }
"""
        sdk = subprocess.check_output(["xcrun", "--sdk", "iphonesimulator", "--show-sdk-path"], text=True).strip()
        with tempfile.TemporaryDirectory(prefix="cn1-theme-uikit-") as directory:
            path = pathlib.Path(directory) / "settings.m"
            path.write_text(prelude + source[start:end])
            subprocess.run(["xcrun", "clang", "-fblocks", "-fsyntax-only", "-isysroot", sdk,
                            "-target", "arm64-apple-ios15.0-simulator", str(path)], check=True)

    @unittest.skipUnless(shutil.which("x86_64-w64-mingw32-g++"), "MinGW cross compiler unavailable")
    def test_windows_bridge_compiles(self):
        source = (ROOT / "Ports/WindowsPort/nativeSources/cn1_windows_winrt.cpp").read_text()
        start = source.index("typedef struct CN1Buf")
        end = source.index("static void cn1BufAppendChar", start)
        helpers = source[start:end]
        start = source.index("JAVA_OBJECT com_codename1_impl_windows_WindowsNative_nativeThemeSettings")
        end = source.index('} /* extern "C" */', start)
        prelude = """
#include <windows.h>
#include <roapi.h>
#include <wrl.h>
#include <wrl/wrappers/corewrappers.h>
#include <windows.ui.viewmanagement.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
using namespace Microsoft::WRL;
using namespace Microsoft::WRL::Wrappers;
#define JAVA_OBJECT void*
#define CODENAME_ONE_THREAD_STATE void* threadStateData
extern void* newStringFromCString(void*, const char*);
struct { float dpiScale; } cn1Win;
"""
        with tempfile.TemporaryDirectory(prefix="cn1-theme-win32-") as directory:
            path = pathlib.Path(directory) / "settings.cpp"
            path.write_text(prelude + helpers + source[start:end])
            # MinGW 14's generated header defines IReference<BYTE> and
            # IReference<boolean> with the same C++ unsigned-char specialization.
            # This bridge uses neither; suppress that duplicate header definition.
            subprocess.run(["x86_64-w64-mingw32-g++", "-std=c++17",
                            "-D____FIReference_1_boolean_INTERFACE_DEFINED__",
                            "-fsyntax-only", str(path)], check=True)

    def test_gtk_bridge_compiles(self):
        if not shutil.which("pkg-config") or subprocess.call(
                ["pkg-config", "--exists", "gtk+-3.0"]) != 0:
            self.skipTest("GTK3 development headers unavailable")
        source = (ROOT / "Ports/LinuxPort/nativeSources/cn1_linux_window.c").read_text()
        start = source.index("static void cn1ThemeSettingsNotify(")
        prelude = """
#include <gtk/gtk.h>
#define JAVA_OBJECT void*
#define CODENAME_ONE_THREAD_STATE void* threadStateData
#define CN1_EVENT_THEME_SETTINGS_CHANGED 24
extern void* newStringFromCString(void*, const char*);
extern void cn1LinuxPushEvent(int, int, int, int);
extern void cn1LinuxRunOnMainAndWait(void (*fn)(void*), void* arg);
static GtkWidget* cn1Window;
"""
        flags = shlex.split(subprocess.check_output(["pkg-config", "--cflags", "gtk+-3.0"], text=True))
        with tempfile.TemporaryDirectory(prefix="cn1-theme-gtk-") as directory:
            path = pathlib.Path(directory) / "settings.c"
            path.write_text(prelude + source[start:])
            subprocess.run(["cc", "-fsyntax-only", "-Werror=implicit-function-declaration", *flags, str(path)], check=True)


if __name__ == "__main__":
    unittest.main()
