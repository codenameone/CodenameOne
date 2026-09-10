#!/usr/bin/env bash
# Link a real Objective-C app against the shipped bridge and pinned SDK.
# Usage: scripts/check-admob-ios-link.sh [output-directory] [iphoneos|iphonesimulator]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROBE="${1:-$(mktemp -d /tmp/cn1-admob-link.XXXXXX)}"
SDK="${2:-iphoneos}"
# Use Java 8, matching the translator build in CI.
mvn -B -f "$ROOT/vm/pom.xml" -pl ByteCodeTranslator -am -DskipTests package
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
# Translate a minimal Java entry point, with the shipped library hint. The
# resulting framework references and library search paths stay intact below.
cat > "$PROBE/AdMobLinkProbe.java" <<'JAVA'
public class AdMobLinkProbe {
    public static void main(String[] args) {}
}
JAVA
javac -d "$PROBE/Sources" "$PROBE/AdMobLinkProbe.java"
LIBS="$(sed -n 's/^codename1\.arg\.ios\.add_libs=;*//p' \
    "$ROOT/maven/cn1-admob/common/codenameone_library_appended.properties")"
java -jar "$ROOT/vm/ByteCodeTranslator/dist/ByteCodeTranslator.jar" ios \
    "$PROBE/Sources" "$PROBE/generated" AdMobLinkProbe com.codenameone.test \
    AdMobLinkProbe 1.0 ios "${LIBS:-none}"
PROJECT="$PROBE/generated/dist"
cp "$PROBE"/{cn1_globals.h,cn1_virtual_thread.h,cn1_class_method_index.h,Prefix.pch} "$PROJECT/"
plutil -convert json -o "$PROBE/project.json" "$PROJECT/AdMobLinkProbe.xcodeproj/project.pbxproj"
python3 - "$ROOT" "$PROBE" "$PROJECT" "$LIBS" <<'PYTHON'
import json, pathlib, plistlib, re, sys
root, probe, project = map(pathlib.Path, sys.argv[1:4])
props = root / 'maven/cn1-admob/common'
pod = re.search(r'^codename1.arg.ios.pods=(.+)$', (props / 'codenameone_library_required.properties').read_text(), re.M).group(1)
name, version = pod.split(' ', 1)
(project / 'Podfile').write_text("platform :ios, '14.0'\ntarget 'AdMobLinkProbe' do\n  use_frameworks!\n  pod '%s', '%s'\nend\n" % (name, version))
data = json.loads((probe / 'project.json').read_text())
objects = data['objects']
# A successful link alone is insufficient: other frameworks may supply C++
# transitively. Verify the shipped hint is an explicit SDK library input before
# trimming the application scaffolding.
app = next(obj for obj in objects.values() if obj['isa'] == 'PBXNativeTarget' and obj['name'] == 'AdMobLinkProbe')
frameworks = next(objects[ref] for ref in app['buildPhases'] if objects[ref]['isa'] == 'PBXFrameworksBuildPhase')
linked = {objects[ref]['fileRef'] for ref in frameworks['files']}
resources = next(objects[ref] for ref in app['buildPhases'] if objects[ref]['isa'] == 'PBXResourcesBuildPhase')
copied = {objects[ref]['fileRef'] for ref in resources['files']}
for lib in filter(None, sys.argv[4].split(';')):
    if lib.endswith('.tbd'):
        matches = [(ref, obj) for ref, obj in objects.items()
                   if obj['isa'] == 'PBXFileReference' and obj.get('name') == lib]
        assert len(matches) == 1, 'Missing SDK library reference: ' + lib
        ref, obj = matches[0]
        assert obj.get('path') == 'usr/lib/' + lib and obj.get('sourceTree') == 'SDKROOT', obj
        assert obj.get('lastKnownFileType') == 'sourcecode.text-based-dylib-definition', obj
        assert ref in linked and ref not in copied, 'Library hint is not a linker input: ' + lib
# Only replace translated runtime sources with the callback stubs above. Keep
# the translator's Frameworks phase, SDK paths and LIBRARY_SEARCH_PATHS verbatim.
source_names = {'main.m', 'com_codename1_ads_admob_AdMobNativeImpl.m'}
for obj in objects.values():
    if obj['isa'] == 'PBXSourcesBuildPhase':
        obj['files'] = [ref for ref in obj['files']
                        if objects[objects[ref]['fileRef']].get('path') in source_names]
    elif obj['isa'] == 'PBXResourcesBuildPhase':
        obj['files'] = []
    elif obj['isa'] == 'XCBuildConfiguration':
        settings = obj['buildSettings']
        settings.pop('INFOPLIST_FILE', None)
        settings.update({'CLANG_ENABLE_MODULES': 'YES', 'CODE_SIGNING_ALLOWED': 'NO',
                         'GENERATE_INFOPLIST_FILE': 'YES', 'IPHONEOS_DEPLOYMENT_TARGET': '14.0',
                         'PRODUCT_BUNDLE_IDENTIFIER': 'com.codenameone.test.AdMobLinkProbe',
                         'GCC_PREFIX_HEADER': 'Prefix.pch', 'GCC_PRECOMPILE_PREFIX_HEADER': 'NO',
                         'HEADER_SEARCH_PATHS': ['$(inherited)', '$(SRCROOT)'],
                         'OTHER_LDFLAGS': ['$(inherited)', '-ObjC'], 'DEAD_CODE_STRIPPING': 'NO'})
# Fail explicitly if staging stopped including the real bridge or entry point.
sources = next(objects[ref] for ref in app['buildPhases'] if objects[ref]['isa'] == 'PBXSourcesBuildPhase')
assert len(sources['files']) == len(source_names), sources
with (project / 'AdMobLinkProbe.xcodeproj/project.pbxproj').open('wb') as output:
    plistlib.dump(data, output)
PYTHON
(cd "$PROJECT" && pod install)
xcodebuild -workspace "$PROJECT/AdMobLinkProbe.xcworkspace" -scheme AdMobLinkProbe \
    -configuration Release -sdk "$SDK" -derivedDataPath "$PROBE/build-$SDK" \
    CODE_SIGNING_ALLOWED=NO build
