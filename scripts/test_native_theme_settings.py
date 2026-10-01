#!/usr/bin/env python3
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
"""Local native-theme bridge checks: AppKit runtime, UIKit and Windows compilation."""
import pathlib
import os
import shutil
import shlex
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]


class NativeThemeSettingsTest(unittest.TestCase):
    @unittest.skipUnless(shutil.which("xcrun"), "Apple toolchain unavailable")
    def test_appkit_snapshot_and_notifications(self):
        source = (ROOT / "Ports/iOSPort/nativeSources/IOSNative.m").read_text()
        start = source.index("static void cn1NativeThemeDidChange(")
        end = source.index("JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isLargerTextEnabled", start)
        bridge = source[start:end]
        start = source.index("JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDarkMode___R_boolean")
        end = source.index("JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDarkModeDetectionSupported", start)
        bridge += source[start:end]
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
#define JAVA_BOOLEAN BOOL
#define JAVA_TRUE YES
#define JAVA_FALSE NO
static BOOL CN1MacHostIsDarkMode(void) { return YES; }
#define POOL_BEGIN()
#define POOL_END()
static float scaleValue = 2;
static int notifications;
static void com_codename1_impl_ios_IOSImplementation_nativeThemeSettingsChanged__(void) { notifications++; }
static id fromNSString(NSString* value) { return [[value copy] autorelease]; }
BRIDGE
int main(void) {
    @autoreleasepool {
        NSAppearance* previous = [NSAppearance appearanceNamed:NSAppearanceNameDarkAqua];
        [NSAppearance setCurrentAppearance:previous];
        // Appearance-only apps never request native font/palette snapshots.
        assert(com_codename1_impl_ios_IOSNative_isDarkMode___R_boolean(nil));
        [[NSNotificationCenter defaultCenter] postNotificationName:NSSystemColorsDidChangeNotification object:nil];
        assert(notifications == 1);
        NSString* settings = com_codename1_impl_ios_IOSNative_nativeThemeSettings___R_java_lang_String(nil);
        assert([NSAppearance currentAppearance] == previous);
        assert([settings containsString:@"fontFamily=native:\n"]);
        NSString* size = [NSString stringWithFormat:@"fontSize=%g\n", [NSFont systemFontSize] * 2];
        assert([settings containsString:size]);
        assert([settings containsString:@"accent-color="]);
        assert([settings containsString:@"accent-color-dark="]);
        assert([settings containsString:@"text-color="]);
        assert([settings containsString:@"text-color-dark="]);
        [[NSNotificationCenter defaultCenter] postNotificationName:NSSystemColorsDidChangeNotification object:nil];
        assert(notifications == 2);
        // Repeated snapshots must not install duplicate observers.
        com_codename1_impl_ios_IOSNative_nativeThemeSettings___R_java_lang_String(nil);
        [[NSNotificationCenter defaultCenter] postNotificationName:NSSystemColorsDidChangeNotification object:nil];
        assert(notifications == 3);
        puts("AppKit native theme snapshot and notification checks passed");
    }
    return 0;
}
'''.replace("BRIDGE", bridge)
        with tempfile.TemporaryDirectory(prefix="cn1-theme-settings-") as directory:
            path = pathlib.Path(directory)
            source_path = path / "settings.m"
            executable = path / "settings"
            # Force the Catalina fallback on the current host as well as checking availability at compile time.
            for variant in (harness, harness.replace("if (@available(macOS 11.0, *))", "if (@available(macOS 99.0, *))")):
                source_path.write_text(variant)
                subprocess.run(["xcrun", "clang", "-fblocks", "-framework", "AppKit",
                                "-mmacosx-version-min=10.15", "-Werror=unguarded-availability",
                                str(source_path), "-o", str(executable)], check=True)
                subprocess.run([str(executable)], check=True)

    @unittest.skipUnless(shutil.which("xcrun"), "Apple toolchain unavailable")
    def test_dynamic_type_notifications_without_native_snapshot(self):
        source = (ROOT / "Ports/iOSPort/nativeSources/IOSNative.m").read_text()
        start = source.index("static void cn1NativeThemeDidChange(")
        end = source.index("JAVA_OBJECT com_codename1_impl_ios_IOSNative_nativeThemeSettings", start)
        helpers = source[start:end]
        start = source.index("JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isLargerTextEnabled")
        end = source.index("\n#if TARGET_OS_WATCH", start)
        # Run the actual iOS observer and accessibility entry points with a fake
        # UIFont on Foundation. No palette snapshot is called anywhere in this test.
        harness = r'''
#import <Foundation/Foundation.h>
#include <assert.h>
#undef TARGET_OS_OSX
#define TARGET_OS_OSX 0
#define TARGET_OS_WATCH 0
#define TARGET_OS_TV 0
#define CN1_THREAD_STATE_MULTI_ARG
#define CN1_THREAD_GET_STATE_PASS_SINGLE_ARG
#define JAVA_OBJECT id
#define JAVA_BOOLEAN BOOL
#define JAVA_FLOAT float
#define JAVA_FALSE NO
static NSString* UIContentSizeCategoryDidChangeNotification = @"ContentSizeChanged";
static NSString* UIFontTextStyleBody = @"Body";
static CGFloat preferredSize = 17;
@interface CN1Font : NSObject
@property(nonatomic, readonly) CGFloat pointSize;
+ (CGFloat)systemFontSize;
+ (CN1Font*)preferredFontForTextStyle:(NSString*)style;
@end
@implementation CN1Font
+ (CGFloat)systemFontSize { return 17; }
+ (CN1Font*)preferredFontForTextStyle:(NSString*)style { return [[[CN1Font alloc] init] autorelease]; }
- (CGFloat)pointSize { return preferredSize; }
@end
static int notifications;
static void com_codename1_impl_ios_IOSImplementation_nativeThemeSettingsChanged__(void) { notifications++; }
HELPERS
ACCESSIBILITY
int main(void) {
    @autoreleasepool {
        // Registration must happen even when the initial size is not enlarged.
        assert(!com_codename1_impl_ios_IOSNative_isLargerTextEnabled___R_boolean(nil));
        preferredSize = 34;
        [[NSNotificationCenter defaultCenter] postNotificationName:UIContentSizeCategoryDidChangeNotification object:nil];
        assert(notifications == 1);
        assert(com_codename1_impl_ios_IOSNative_isLargerTextEnabled___R_boolean(nil));
        assert(com_codename1_impl_ios_IOSNative_getLargerTextScale___R_float(nil) == 2);
        // Querying both entry points must still install only one observer.
        preferredSize = 17;
        [[NSNotificationCenter defaultCenter] postNotificationName:UIContentSizeCategoryDidChangeNotification object:nil];
        assert(notifications == 2);
        assert(!com_codename1_impl_ios_IOSNative_isLargerTextEnabled___R_boolean(nil));
        assert(com_codename1_impl_ios_IOSNative_getLargerTextScale___R_float(nil) == 1);
    }
    return 0;
}
'''.replace("HELPERS", helpers).replace("ACCESSIBILITY", source[start:end])
        with tempfile.TemporaryDirectory(prefix="cn1-dynamic-type-") as directory:
            path = pathlib.Path(directory)
            (path / "settings.m").write_text(harness)
            executable = path / "settings"
            subprocess.run(["xcrun", "clang", "-fblocks", "-framework", "Foundation",
                            str(path / "settings.m"), "-o", str(executable)], check=True)
            subprocess.run([str(executable)], check=True)

    @unittest.skipUnless(shutil.which("xcrun"), "Apple toolchain unavailable")
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

    @unittest.skipUnless(shutil.which("c++"), "C++ compiler unavailable")
    def test_windows_message_font_keeps_device_pixel_size(self):
        source = (ROOT / "Ports/WindowsPort/nativeSources/cn1_windows_winrt.cpp").read_text()
        helpers = source[source.index("typedef struct CN1Buf"):source.index("static void cn1BufAppendChar")]
        start = source.index("    CN1Buf buffer;", source.index("WindowsNative_nativeThemeSettings"))
        end = source.index("    HRESULT initialized", start)
        # Exercise the production serialization against Windows message-font metrics
        # at 100%, 150% and 200% scaling without requiring a Windows runtime.
        harness = r'''
#include <cassert>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cwchar>
#define CP_UTF8 65001
#define SPI_GETNONCLIENTMETRICS 41
#define LOGPIXELSY 90
struct NONCLIENTMETRICSW {
    unsigned cbSize;
    struct { int lfHeight; wchar_t lfFaceName[32]; } lfMessageFont;
};
static int dpi, messageHeight;
static struct { float dpiScale; } cn1Win = {1};
typedef void* HDC;
static HDC GetDC(void*) { return reinterpret_cast<HDC>(1); }
static int GetDeviceCaps(HDC, int) { return dpi; }
static void ReleaseDC(void*, HDC) {}
static bool SystemParametersInfoW(unsigned, unsigned, NONCLIENTMETRICSW* m, unsigned) {
    m->lfMessageFont.lfHeight = -messageHeight;
    wcscpy(m->lfMessageFont.lfFaceName, L"Segoe UI");
    return true;
}
static int WideCharToMultiByte(unsigned, unsigned, const wchar_t* family, int,
        char* output, int capacity, void*, void*) {
    return snprintf(output, capacity, "%ls", family) + 1;
}
HELPERS
static void check() {
BODY
    char expected[80];
    snprintf(expected, sizeof(expected), "fontFamily=Segoe UI\nfontSize=%d\n", messageHeight);
    assert(buffer.data && strcmp(buffer.data, expected) == 0);
    free(buffer.data);
}
int main() {
    const int dpis[] = {96, 144, 192};
    const int heights[] = {13, 20, 26};
    for (int i = 0; i < 3; ++i) {
        dpi = dpis[i]; messageHeight = heights[i]; check();
    }
}
'''.replace("HELPERS", helpers).replace("BODY", source[start:end])
        with tempfile.TemporaryDirectory(prefix="cn1-theme-dpi-") as directory:
            path = pathlib.Path(directory)
            (path / "settings.cpp").write_text(harness)
            executable = path / "settings"
            subprocess.run(["c++", "-std=c++11", str(path / "settings.cpp"), "-o", str(executable)], check=True)
            subprocess.run([str(executable)], check=True)

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

    def test_gnome_appearance_notifications_without_native_snapshot(self):
        if not shutil.which("pkg-config") or not shutil.which("glib-compile-schemas") or subprocess.call(
                ["pkg-config", "--exists", "gio-2.0"]) != 0:
            self.skipTest("GLib development tools unavailable")
        source = (ROOT / "Ports/LinuxPort/nativeSources/cn1_linux_window.c").read_text()
        start = source.index("static gint cn1DesktopColorScheme =")
        end = source.index("/* Settings and GTK style contexts", start)
        harness = r'''
#include <gio/gio.h>
#include <assert.h>
#include <string.h>
#define JAVA_INT int
#define CODENAME_ONE_THREAD_STATE void* threadStateData
#define CN1_EVENT_THEME_SETTINGS_CHANGED 24
static int notifications, mainCalls;
static void cn1LinuxPushEvent(int type, int x, int y, int key) {
    assert(type == CN1_EVENT_THEME_SETTINGS_CHANGED);
    notifications++;
}
static void cn1LinuxRunOnMainAndWait(void (*fn)(void*), void* arg) { mainCalls++; fn(arg); }
BRIDGE
static void drain(void) { while (g_main_context_pending(NULL)) g_main_context_iteration(NULL, FALSE); }
int main(int argc, char** argv) {
    int initial = com_codename1_impl_linux_LinuxNative_systemColorScheme___R_int(NULL);
    assert(mainCalls == 1);
    if (argc > 1) {
        assert(initial == -1);
        assert(notifications == 0);
        return 0;
    }
    assert(initial == 0);
    GSettings* writer = g_settings_new("org.gnome.desktop.interface");
    g_settings_set_string(writer, "color-scheme", "prefer-dark");
    drain();
    assert(notifications == 1);
    assert(com_codename1_impl_linux_LinuxNative_systemColorScheme___R_int(NULL) == 1);
    assert(com_codename1_impl_linux_LinuxNative_systemColorScheme___R_int(NULL) == 1);
    g_settings_set_string(writer, "color-scheme", "default");
    drain();
    assert(notifications == 2); // Repeated reads must not duplicate observers.
    assert(com_codename1_impl_linux_LinuxNative_systemColorScheme___R_int(NULL) == 0);
    assert(mainCalls == 1); // Frequent style queries must not block on GTK.
    g_object_unref(writer);
    return 0;
}
'''.replace("BRIDGE", source[start:end])
        flags = shlex.split(subprocess.check_output(["pkg-config", "--cflags", "--libs", "gio-2.0"], text=True))
        with tempfile.TemporaryDirectory(prefix="cn1-gnome-settings-") as directory:
            path = pathlib.Path(directory)
            (path / "settings.c").write_text(harness)
            executable = path / "settings"
            subprocess.run(["cc", str(path / "settings.c"), *flags, "-o", str(executable)], check=True)
            for variant in ("supported", "old-schema", "no-schema"):
                schemas = path / variant
                schemas.mkdir()
                schema_id = "unrelated" if variant == "no-schema" else "org.gnome.desktop.interface"
                key = "color-scheme" if variant == "supported" else "old-key"
                (schemas / "test.gschema.xml").write_text(
                    '<schemalist><schema id="%s" path="/org/gnome/desktop/interface/">'
                    '<key name="%s" type="s"><default>"default"</default></key>'
                    '</schema></schemalist>' % (schema_id, key))
                subprocess.run(["glib-compile-schemas", str(schemas)], check=True)
                env = dict(os.environ, GSETTINGS_BACKEND="memory", GSETTINGS_SCHEMA_DIR=str(schemas),
                           XDG_DATA_DIRS=str(schemas))
                subprocess.run([str(executable)] + ([] if variant == "supported" else ["unavailable"]),
                               env=env, check=True)

    def test_gtk_dispatch_during_startup_and_from_worker(self):
        if not shutil.which("pkg-config") or subprocess.call(["pkg-config", "--exists", "glib-2.0"]) != 0:
            self.skipTest("GLib development tools unavailable")
        source = (ROOT / "Ports/LinuxPort/nativeSources/cn1_linux_window.c").read_text()
        start = source.index("typedef struct {", source.index("/* Posts fn(arg) onto the GTK main loop"))
        end = source.index("/* (Re)allocates the back-buffer", start)
        harness = r'''
#include <glib.h>
#include <pthread.h>
#include <assert.h>
#include <unistd.h>
static void* cn1Window;
static pthread_t cn1GtkThread;
static gint parked, resumed, finished;
static int scheduled, callbacks;
#define CN1_YIELD_THREAD g_atomic_int_inc(&parked)
#define CN1_RESUME_THREAD g_atomic_int_inc(&resumed)
static guint gdk_threads_add_idle(GSourceFunc callback, gpointer data) {
    scheduled++;
    return g_idle_add(callback, data);
}
BRIDGE
static void callback(void* arg) {
    assert(pthread_equal(pthread_self(), cn1GtkThread));
    assert(arg == &callbacks);
    callbacks++;
}
static void* worker(void* unused) {
    cn1LinuxRunOnMainAndWait(callback, &callbacks);
    g_atomic_int_set(&finished, 1);
    return NULL;
}
int main(void) {
    alarm(5); // A same-thread dispatch deadlock must fail promptly.
    cn1GtkThread = pthread_self();
    cn1LinuxRunOnMainAndWait(callback, &callbacks); // Headless, no window.
    assert(callbacks == 1 && scheduled == 0);
    cn1Window = &callbacks;
    // Display.init installs the theme here, before there is an event loop.
    cn1LinuxRunOnMainAndWait(callback, &callbacks);
    assert(callbacks == 2 && scheduled == 0);
    pthread_t thread;
    assert(pthread_create(&thread, NULL, worker, NULL) == 0);
    while (!g_atomic_int_get(&finished)) {
        g_main_context_iteration(NULL, FALSE);
        g_usleep(1000);
    }
    pthread_join(thread, NULL);
    assert(callbacks == 3 && scheduled == 1);
    assert(parked == 1 && resumed == 1); // Retain the VM's GC handshake on worker waits.
    return 0;
}
'''.replace("BRIDGE", source[start:end])
        flags = shlex.split(subprocess.check_output(["pkg-config", "--cflags", "--libs", "glib-2.0"], text=True))
        with tempfile.TemporaryDirectory(prefix="cn1-gtk-dispatch-") as directory:
            path = pathlib.Path(directory)
            executable = path / "dispatch"
            (path / "dispatch.c").write_text(harness)
            subprocess.run(["cc", "-pthread", str(path / "dispatch.c"), *flags, "-o", str(executable)], check=True)
            subprocess.run([str(executable)], check=True, timeout=10)


if __name__ == "__main__":
    unittest.main()
