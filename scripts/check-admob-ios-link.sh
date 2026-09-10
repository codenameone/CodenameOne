#!/usr/bin/env bash
# Link a real Objective-C app against the shipped bridge and pinned SDK.
# Usage: scripts/check-admob-ios-link.sh [output-directory] [iphoneos|iphonesimulator]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROBE="${1:-$(mktemp -d /tmp/cn1-admob-link.XXXXXX)}"
SDK="${2:-iphoneos}"
mkdir -p "$PROBE/Sources"
cp "$ROOT"/maven/cn1-admob/ios/src/main/objectivec/* "$PROBE/Sources/"
cp "$ROOT"/vm/ByteCodeTranslator/src/{cn1_globals.h,cn1_virtual_thread.h} "$PROBE/"
printf '#pragma once\n' > "$PROBE/cn1_class_method_index.h"
cat > "$PROBE/Prefix.pch" <<'PCH'
#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#include "cn1_globals.h"
PCH
cat > "$PROBE/Sources/main.m" <<'OBJC'
#import "com_codename1_ads_admob_AdMobNativeImpl.h"
// Only generated Java runtime entry points are stubbed. All SDK references
// must resolve through the real CocoaPods integration and final app linker.
struct ThreadLocalData* getThreadLocalData(void) { return NULL; }
JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str) { return JAVA_NULL; }
void com_codename1_ads_admob_AdMobCallback_fire___int_int_int_java_lang_String_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_INT handle, JAVA_INT event, JAVA_INT code,
        JAVA_OBJECT message, JAVA_OBJECT rewardType, JAVA_INT rewardAmount) {}
int main(int argc, char** argv) {
    @autoreleasepool {
        return [[[com_codename1_ads_admob_AdMobNativeImpl alloc] init] isSupported] ? 0 : 1;
    }
}
OBJC
python3 - "$ROOT" "$PROBE" <<'PY'
import json, pathlib, re, sys
root, probe = map(pathlib.Path, sys.argv[1:])
props = root / 'maven/cn1-admob/common'
pod = re.search(r'^codename1.arg.ios.pods=(.+)$', (props / 'codenameone_library_required.properties').read_text(), re.M).group(1)
name, version = pod.split(' ', 1)
(probe / 'Podfile').write_text("platform :ios, '14.0'\ntarget 'AdMobLinkProbe' do\n  use_frameworks!\n  pod '%s', '%s'\nend\n" % (name, version))
# Consume the same search paths and library hints as a generated CN1 app.
template = (root / 'vm/ByteCodeTranslator/src/template/template.xcodeproj/project.pbxproj').read_text()
blocks = re.findall(r'LIBRARY_SEARCH_PATHS = \((.*?)\);', template, re.S)
paths = re.findall(r'"([^"]+)"', blocks[0])
assert all(re.findall(r'"([^"]+)"', block) == paths for block in blocks)
paths = [p for p in paths if 'template-src' not in p]
appended = (props / 'codenameone_library_appended.properties').read_text()
match = re.search(r'^codename1.arg.ios.add_libs=(.*)$', appended, re.M)
libs = [] if not match else [v for v in match.group(1).split(';') if v]
settings = {'CLANG_ENABLE_MODULES': 'YES', 'CLANG_ENABLE_OBJC_ARC': 'NO',
            'CODE_SIGNING_ALLOWED': 'NO', 'GENERATE_INFOPLIST_FILE': 'YES',
            'GCC_PREFIX_HEADER': 'Prefix.pch', 'GCC_PRECOMPILE_PREFIX_HEADER': 'NO',
            'HEADER_SEARCH_PATHS': ['$(inherited)', '$(SRCROOT)'],
            'LIBRARY_SEARCH_PATHS': paths, 'OTHER_LDFLAGS': ['$(inherited)', '-ObjC'],
            'DEAD_CODE_STRIPPING': 'NO'}
project = {'name': 'AdMobLinkProbe', 'options': {'bundleIdPrefix': 'com.codenameone.test', 'deploymentTarget': {'iOS': '14.0'}},
           'targets': {'AdMobLinkProbe': {'type': 'application', 'platform': 'iOS', 'sources': ['Sources'],
                       'dependencies': [{'sdk': lib} for lib in libs], 'settings': {'base': settings}}}}
(probe / 'project.yml').write_text(json.dumps(project, indent=2))
PY
(cd "$PROBE" && xcodegen generate && pod install)
xcodebuild -workspace "$PROBE/AdMobLinkProbe.xcworkspace" -scheme AdMobLinkProbe \
    -configuration Release -sdk "$SDK" -derivedDataPath "$PROBE/build-$SDK" \
    CODE_SIGNING_ALLOWED=NO build
