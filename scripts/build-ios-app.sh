#!/usr/bin/env bash
# Build a sample "Hello Codename One" iOS application using the locally-built Codename One iOS port
set -euo pipefail

bia_log() { echo "[build-ios-app] $1"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR%/}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
EXTRA_MVN_ARGS=("$@")

ENV_FILE="$ENV_DIR/env.sh"
bia_log "Loading workspace environment from $ENV_FILE"
if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  bia_log "Loaded environment: JAVA_HOME=${JAVA_HOME:-<unset>} JAVA17_HOME=${JAVA17_HOME:-<unset>} MAVEN_HOME=${MAVEN_HOME:-<unset>}"
else
  bia_log "Workspace tools not found. Run scripts/setup-workspace.sh before this script." >&2
  exit 1
fi

# --- Tool validations ---
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  bia_log "JAVA_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  bia_log "JAVA17_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${MAVEN_HOME:-}" ] || [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  bia_log "Maven is not available. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if ! command -v xcodebuild >/dev/null 2>&1; then
  bia_log "xcodebuild not found. Install Xcode command-line tools." >&2
  exit 1
fi
if ! command -v pod >/dev/null 2>&1; then
  bia_log "CocoaPods (pod) command not found. Install cocoapods before running this script." >&2
  exit 1
fi

export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

bia_log "Using JAVA_HOME at $JAVA_HOME"
bia_log "Using JAVA17_HOME at $JAVA17_HOME"
bia_log "Using Maven installation at $MAVEN_HOME"
bia_log "Using CocoaPods version $(pod --version 2>/dev/null || echo '<unknown>')"

CN1_VERSION=$(awk -F'[<>]' '/<version>/{print $3; exit}' maven/pom.xml)
bia_log "Detected Codename One version $CN1_VERSION"

WORK_DIR="$TMPDIR/cn1-hello-ios"
rm -rf "$WORK_DIR"; mkdir -p "$WORK_DIR"

GROUP_ID="com.codenameone.examples"
ARTIFACT_ID="hello-codenameone-ios"
MAIN_NAME="HelloCodenameOne"
PACKAGE_NAME="$GROUP_ID"

SOURCE_PROJECT="$REPO_ROOT/Samples/SampleProjectTemplate"
if [ ! -d "$SOURCE_PROJECT" ]; then
  bia_log "Source project template not found at $SOURCE_PROJECT" >&2
  exit 1
fi
bia_log "Using source project template at $SOURCE_PROJECT"

LOCAL_MAVEN_REPO="${LOCAL_MAVEN_REPO:-$HOME/.m2/repository}"
bia_log "Using local Maven repository at $LOCAL_MAVEN_REPO"
mkdir -p "$LOCAL_MAVEN_REPO"
MAVEN_CMD=(
  "$MAVEN_HOME/bin/mvn" -B -ntp
  -Dmaven.repo.local="$LOCAL_MAVEN_REPO"
  -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
)

# --- Generate app skeleton ---
bia_log "Generating Codename One application skeleton via codenameone-maven-plugin"
(
  cd "$WORK_DIR"
  "${MAVEN_CMD[@]}" -q \
    com.codenameone:codenameone-maven-plugin:7.0.204:generate-app-project \
    -DgroupId="$GROUP_ID" \
    -DartifactId="$ARTIFACT_ID" \
    -Dversion=1.0-SNAPSHOT \
    -DsourceProject="$SOURCE_PROJECT" \
    -Dcn1Version="$CN1_VERSION" \
    "${EXTRA_MVN_ARGS[@]}"
)

APP_DIR="$WORK_DIR/$ARTIFACT_ID"
[ -d "$APP_DIR" ] || { bia_log "Failed to create Codename One application project" >&2; exit 1; }
[ -f "$APP_DIR/build.sh" ] && chmod +x "$APP_DIR/build.sh"

SETTINGS_FILE="$APP_DIR/common/codenameone_settings.properties"
if [ ! -f "$SETTINGS_FILE" ]; then
  bia_log "codenameone_settings.properties not found at $SETTINGS_FILE" >&2
  exit 1
fi

set_property() {
  local key="$1" value="$2"
  if grep -q "^${key}=" "$SETTINGS_FILE"; then
    if sed --version >/dev/null 2>&1; then
      sed -i -E "s|^${key}=.*$|${key}=${value}|" "$SETTINGS_FILE"
    else
      sed -i '' -E "s|^${key}=.*$|${key}=${value}|" "$SETTINGS_FILE"
    fi
  else
    printf '\n%s=%s\n' "$key" "$value" >> "$SETTINGS_FILE"
  fi
}

set_property "codename1.packageName" "$PACKAGE_NAME"
set_property "codename1.mainName" "$MAIN_NAME"

# Ensure trailing newline
tail -c1 "$SETTINGS_FILE" | read -r _ || echo >> "$SETTINGS_FILE"

PACKAGE_PATH="${PACKAGE_NAME//.//}"
JAVA_DIR="$APP_DIR/common/src/main/java/${PACKAGE_PATH}"
mkdir -p "$JAVA_DIR"
MAIN_FILE="$JAVA_DIR/${MAIN_NAME}.java"
TEMPLATE="$SCRIPT_DIR/templates/HelloCodenameOne.java.tmpl"
if [ ! -f "$TEMPLATE" ]; then
  bia_log "Template not found: $TEMPLATE" >&2
  exit 1
fi

sed -e "s|@PACKAGE@|$PACKAGE_NAME|g" \
    -e "s|@MAIN_NAME@|$MAIN_NAME|g" \
    "$TEMPLATE" > "$MAIN_FILE"

bia_log "Wrote main application class to $MAIN_FILE"

# --- Build iOS project ---
DERIVED_DATA_DIR="${TMPDIR}/codenameone-ios-derived"
rm -rf "$DERIVED_DATA_DIR"
mkdir -p "$DERIVED_DATA_DIR"

bia_log "Building iOS Xcode project using Codename One port"
"${MAVEN_CMD[@]}" -q -f "$APP_DIR/pom.xml" package \
  -DskipTests \
  -Dcodename1.platform=ios \
  -Dcodename1.buildTarget=ios-source \
  -Dopen=false \
  -Dcodenameone.version="$CN1_VERSION" \
  "${EXTRA_MVN_ARGS[@]}"

IOS_TARGET_DIR="$APP_DIR/ios/target"
if [ ! -d "$IOS_TARGET_DIR" ]; then
  bia_log "iOS target directory not found at $IOS_TARGET_DIR" >&2
  exit 1
fi

PROJECT_DIR=""
for candidate in "$IOS_TARGET_DIR"/*-ios-source; do
  if [ -d "$candidate" ]; then
    PROJECT_DIR="$candidate"
    break
  fi
done
if [ -z "$PROJECT_DIR" ]; then
  bia_log "Failed to locate generated iOS project under $IOS_TARGET_DIR" >&2
  find "$IOS_TARGET_DIR" -type d -print >&2 || true
  exit 1
fi

bia_log "Found generated iOS project at $PROJECT_DIR"

# --- Ruby/gem environment alignment (make sure this ruby sees xcodeproj) ---
if ! command -v ruby >/dev/null; then
  bia_log "ruby not found on PATH"; exit 1
fi
export PATH="$(ruby -e 'print Gem.user_dir')/bin:$PATH"
if ! ruby -rrubygems -e 'exit(Gem::Specification.find_all_by_name("xcodeproj").empty? ? 1 : 0)'; then
  bia_log "Installing xcodeproj gem for current ruby"
  gem install xcodeproj --no-document --user-install
fi
ruby -rrubygems -e 'abort("xcodeproj gem still missing") if Gem::Specification.find_all_by_name("xcodeproj").empty?'

# --- Locate the .xcodeproj and pass it to Ruby explicitly ---
XCODEPROJ="$PROJECT_DIR/HelloCodenameOne.xcodeproj"
if [ ! -d "$XCODEPROJ" ]; then
  # fallback: first .xcodeproj in the dir
  XCODEPROJ="$(/bin/ls -1d "$PROJECT_DIR"/*.xcodeproj 2>/dev/null | head -n1 || true)"
fi
if [ -z "$XCODEPROJ" ] || [ ! -d "$XCODEPROJ" ]; then
  bia_log "Failed to locate .xcodeproj under $PROJECT_DIR"; exit 1
fi
export XCODEPROJ
bia_log "Using Xcode project: $XCODEPROJ"

# --- Ensure a UITests target exists and a shared CI scheme includes only that testable ---
ruby -rrubygems -rxcodeproj -e '
require "fileutils"
proj_path = ENV["XCODEPROJ"] or abort("XCODEPROJ env not set")
proj = Xcodeproj::Project.open(proj_path)

app_target = proj.targets.find { |t| t.product_type == "com.apple.product-type.application" } || proj.targets.first
ui_name    = "HelloCodenameOneUITests"
ui_target  = proj.targets.find { |t| t.name == ui_name }

unless ui_target
  ui_target = proj.new_target(:ui_test_bundle, ui_name, :ios, "18.0")
  ui_target.product_reference.name = "#{ui_name}.xctest"
  ui_target.add_dependency(app_target) if app_target
end

proj.save

# Create/update a shared scheme that only contains the UITests testable
ws_dir = File.join(File.dirname(proj_path), "HelloCodenameOne.xcworkspace")
schemes_root = if File.directory?(ws_dir)
  File.join(ws_dir, "xcshareddata", "xcschemes")
else
  File.join(File.dirname(proj_path), "xcshareddata", "xcschemes")
end
FileUtils.mkdir_p(schemes_root)
scheme_path = File.join(schemes_root, "HelloCodenameOne-CI.xcscheme")

scheme = if File.exist?(scheme_path)
  Xcodeproj::XCScheme.new(scheme_path)
else
  Xcodeproj::XCScheme.new
end

# Build action: keep app only (no unit-test bundles)
scheme.build_action.entries = []
if app_target
  scheme.add_build_target(app_target)
end

# Test action: only the UITests bundle
scheme.test_action = Xcodeproj::XCScheme::TestAction.new
scheme.test_action.xml_element.elements.delete_all("Testables")
scheme.add_test_target(ui_target)

scheme.launch_action.build_configuration = "Debug"
scheme.test_action.build_configuration   = "Debug"
scheme.save_as(proj, "HelloCodenameOne-CI", true)
'

# Show what we created
WS_XCSCHEME="$PROJECT_DIR/HelloCodenameOne.xcworkspace/xcshareddata/xcschemes/HelloCodenameOne-CI.xcscheme"
PRJ_XCSCHEME="$PROJECT_DIR/xcshareddata/xcschemes/HelloCodenameOne-CI.xcscheme"
if [ -f "$WS_XCSCHEME" ]; then
  bia_log "CI scheme (workspace): $WS_XCSCHEME"; grep -n "BlueprintName" "$WS_XCSCHEME" || true
elif [ -f "$PRJ_XCSCHEME" ]; then
  bia_log "CI scheme (project):   $PRJ_XCSCHEME"; grep -n "BlueprintName" "$PRJ_XCSCHEME" || true
else
  bia_log "Warning: CI scheme not found after generation"
fi

# Ensure we’re using the same Ruby/gems that CI installed
if ! command -v ruby >/dev/null; then
  bia_log "ruby not found on PATH"; exit 1
fi

# Make sure user gem bin dir is on PATH (works for both setup styles)
USER_GEM_BIN="$(ruby -e 'print Gem.user_dir')/bin"
export PATH="$USER_GEM_BIN:$PATH"

# Verify xcodeproj gem is available to *this* ruby
if ! ruby -rrubygems -e 'exit(Gem::Specification.find_all_by_name("xcodeproj").empty? ? 1 : 0)'; then
  # Last resort: install to user gem dir (no sudo) for this Ruby
  bia_log "Installing xcodeproj gem for current ruby"
  gem install xcodeproj --no-document --user-install
fi

# Re-check
ruby -rrubygems -e 'abort("xcodeproj gem still missing") if Gem::Specification.find_all_by_name("xcodeproj").empty?'

# --- Ensure a UI Tests target exists and is hooked into the CI scheme ---

PRJ="$PROJECT_DIR/HelloCodenameOne.xcodeproj"
PBX="$PRJ/project.pbxproj"
APP_TARGET_NAME="HelloCodenameOne"
UIT_TARGET_NAME="${APP_TARGET_NAME}UITests"
UIT_DIR="$PROJECT_DIR/${UIT_TARGET_NAME}"

# 1) Create UITests target (if missing) using the xcodeproj Ruby gem
ruby -rrubygems -rxcodeproj -e '
require "xcodeproj"
prj_path   = ENV["PRJ"]
app_name   = ENV["APP_TARGET_NAME"]
uit_name   = ENV["UIT_TARGET_NAME"]

project    = Xcodeproj::Project.open(prj_path)
app_tgt    = project.targets.find { |t| t.name == app_name }
raise "App target #{app_name} not found" unless app_tgt

uit_tgt = project.targets.find { |t| t.name == uit_name }
if uit_tgt.nil?
  uit_tgt = project.new_target(:ui_testing_bundle, uit_name, :ios, "18.0")
  uit_tgt.product_name = uit_name
  uit_tgt.build_configurations.each do |cfg|
    s = cfg.build_settings
    s["SWIFT_VERSION"] = "5.0"
    s["INFOPLIST_FILE"] = "#{uit_name}/Info.plist"
    s["BUNDLE_LOADER"] = "$(TEST_HOST)"
    s["TEST_HOST"] = "$(BUILT_PRODUCTS_DIR)/$(WRAPPER_NAME)/$(EXECUTABLE_NAME)"
    s["IPHONEOS_DEPLOYMENT_TARGET"] = "15.0"
    s["CODE_SIGNING_ALLOWED"] = "NO"
    s["CODE_SIGNING_REQUIRED"] = "NO"
  end

  # File group for UITests
  group = project.main_group.find_subpath(uit_name, true)
  group.clear_sources_references

  # Add Info.plist
  plist_path = File.join(uit_name, "Info.plist")
  FileUtils.mkdir_p(File.dirname(plist_path))
  unless File.exist?(plist_path)
    File.write(plist_path, %q{<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>CFBundleIdentifier</key><string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
  <key>CFBundleName</key><string>$(PRODUCT_NAME)</string>
</dict></plist>})
  end
  plist_ref = group.new_file(plist_path)

  # Add a simple Swift test file
  test_swift = File.join(uit_name, "#{uit_name}.swift")
  unless File.exist?(test_swift)
    File.write(test_swift, %Q{
import XCTest

final class #{uit_name}: XCTestCase {
    func testLaunchAndScreenshot() {
        let app = XCUIApplication()
        app.launch()
        sleep(1)
        let shot = XCUIScreen.main.screenshot()
        let data = shot.pngRepresentation
        let dir = ProcessInfo.processInfo.environment["CN1SS_OUTPUT_DIR"] ?? NSTemporaryDirectory()
        let url = URL(fileURLWithPath: dir).appendingPathComponent("HelloCodenameOneUITests.testLaunchAndScreenshot.png")
        try? data.write(to: url)
        XCTAssertTrue(data.count > 0)
    }
}
})
  end
  swift_ref = group.new_file(test_swift)

  # Hook files to target
  uit_tgt.add_file_references([plist_ref, swift_ref])

  # Make sure UITests target depends on building the app
  uit_tgt.add_dependency(app_tgt)
end

project.save
' \
PRJ="$PRJ" APP_TARGET_NAME="$APP_TARGET_NAME" UIT_TARGET_NAME="$UIT_TARGET_NAME"

# 2) Ensure a shared CI scheme exists under the WORKSPACE and has TestAction -> UITests
WS=""
for w in "$PROJECT_DIR"/*.xcworkspace; do [ -d "$w" ] && WS="$w" && break; done
SCHEME_PATH="$WS/xcshareddata/xcschemes/${APP_TARGET_NAME}-CI.xcscheme"

# If your create-shared-scheme.py already writes this scheme, keep it.
# Here we *guarantee* TestAction exists and references the UITests target.
if [ -f "$SCHEME_PATH" ]; then
  /usr/bin/python3 - "$SCHEME_PATH" "$APP_TARGET_NAME" "$UIT_TARGET_NAME" <<'PY'
import sys, xml.etree.ElementTree as ET
scheme, app, uit = sys.argv[1:4]
t = ET.parse(scheme); r = t.getroot()
def q(n):
    if r.tag.startswith('{'):
        ns = r.tag.split('}')[0].strip('{'); return f'{{{ns}}}{n}'
    return n

# Ensure TestAction
ta = r.find(q("TestAction"))
if ta is None:
    ta = ET.SubElement(r, q("TestAction"), {
        "buildConfiguration":"Debug",
        "selectedDebuggerIdentifier":"Xcode.DebuggerFoundation.Debugger.LLDB",
        "selectedLauncherIdentifier":"Xcode.DebuggerFoundation.Launcher.LLDB",
        "shouldUseLaunchSchemeArgsEnv":"YES"
    })

# Ensure MacroExpansion (target app) so UITests know which app to launch
me = ta.find(q("MacroExpansion"))
if me is None:
    me = ET.SubElement(ta, q("MacroExpansion"))
# Replace any existing BuildableReference under MacroExpansion with the app target ref
for c in list(me): me.remove(c)
ET.SubElement(me, q("BuildableReference"), {
    "BuildableIdentifier":"primary",
    "BlueprintName": app,
    "ReferencedContainer":"container:HelloCodenameOne.xcodeproj"
})

# Ensure Testables with a UITests reference
ts = ta.find(q("Testables")) or ET.SubElement(ta, q("Testables"))
# Drop non-UI testables
for n in list(ts):
    br = n.find(q("BuildableReference"))
    name = (br.get("BlueprintName") if br is not None else "") or ""
    if name.endswith("Tests") and not name.endswith("UITests"):
        ts.remove(n)
# Add UITests if missing
have_ui = any(
    (n.find(q("BuildableReference")).get("BlueprintName") if n.find(q("BuildableReference")) is not None else "") == uit
    for n in ts
)
if not have_ui:
    tref = ET.SubElement(ts, q("TestableReference"), {"skipped":"NO"})
    ET.SubElement(tref, q("BuildableReference"), {
        "BuildableIdentifier":"primary",
        "BlueprintName": uit,
        "ReferencedContainer":"container:HelloCodenameOne.xcodeproj"
    })

t.write(scheme, encoding="UTF-8", xml_declaration=True)
PY
else
  bia_log "Warning: expected CI scheme not found at $SCHEME_PATH"
fi

# 3) Show we really have a TestAction + UITests now
grep -n "<TestAction" "$SCHEME_PATH" || true
grep -n 'BlueprintName="'"$UIT_TARGET_NAME"'"' "$SCHEME_PATH" || true

PBXPROJ="$PROJECT_DIR/HelloCodenameOne.xcodeproj/project.pbxproj"
if [ -f "$PBXPROJ" ]; then
  bia_log "Patching TEST_HOST in $PBXPROJ (remove '-src' suffix)"
  cp "$PBXPROJ" "$PBXPROJ.bak"
  perl -0777 -pe 's/(TEST_HOST = .*?\.app\/)([^"\/]+)-src(";\n)/$1$2$3/s' \
    "$PBXPROJ.bak" > "$PBXPROJ"
  grep -n "TEST_HOST =" "$PBXPROJ" || true
fi

UITEST_TEMPLATE="$SCRIPT_DIR/ios/tests/HelloCodenameOneUITests.swift.tmpl"
if [ -f "$UITEST_TEMPLATE" ]; then
  IOS_UITEST_DIR="$(find "$PROJECT_DIR" -maxdepth 1 -type d -name '*UITests' -print -quit 2>/dev/null || true)"
  if [ -n "$IOS_UITEST_DIR" ]; then
    UI_TEST_DEST="$IOS_UITEST_DIR/templateUITests.swift"
    bia_log "Installing UI test template at $UI_TEST_DEST"
    cp "$UITEST_TEMPLATE" "$UI_TEST_DEST"
  else
    bia_log "Warning: Could not locate a *UITests target directory under $PROJECT_DIR; UI tests will be skipped"
  fi
fi

if [ -f "$PROJECT_DIR/Podfile" ]; then
  bia_log "Installing CocoaPods dependencies"
  (
    cd "$PROJECT_DIR"
    if ! pod install --repo-update; then
      bia_log "pod install --repo-update failed; retrying without repo update"
      pod install
    fi
  )
else
  bia_log "Podfile not found in generated project; skipping pod install"
fi

SCHEME_HELPER="$SCRIPT_DIR/ios/create-shared-scheme.py"
if [ -f "$SCHEME_HELPER" ]; then
  bia_log "Ensuring shared Xcode scheme exposes UI tests"
  if command -v python3 >/dev/null 2>&1; then
    # Create a shared scheme named "<AppName>-CI" so it cannot be shadowed by any user scheme
    if ! python3 "$SCHEME_HELPER" "$PROJECT_DIR" "$MAIN_NAME-CI"; then
      bia_log "Warning: Failed to configure shared Xcode scheme" >&2
    fi
  else
    bia_log "Warning: python3 is not available; skipping shared scheme configuration" >&2
  fi
else
  bia_log "Warning: Missing scheme helper script at $SCHEME_HELPER" >&2
fi

# Remove any user schemes that could shadow the shared CI scheme
rm -rf "$PROJECT_DIR"/xcuserdata 2>/dev/null || true
# Also remove user schemes under the workspace, if any
find "$PROJECT_DIR" -maxdepth 1 -name "*.xcworkspace" -type d -exec rm -rf {}/xcuserdata \; 2>/dev/null || true

_prune_scheme() {
  local scheme_path="$1"
  [ -f "$scheme_path" ] || return 0
  bia_log "Pruning non-UI testables and build entries in $scheme_path"
  cp "$scheme_path" "$scheme_path.bak"

  /usr/bin/python3 - "$scheme_path" <<'PY'
import sys, xml.etree.ElementTree as ET
p = sys.argv[1]
t = ET.parse(p); r = t.getroot()
def q(n):
    if r.tag.startswith('{'):
        ns = r.tag.split('}')[0].strip('{')
        return f'{{{ns}}}{n}'
    return n

# --- Drop non-UI tests from TestAction/Testables
ta = r.find(q('TestAction'))
ts = ta.find(q('Testables')) if ta is not None else None
if ts is not None:
    for node in list(ts):
        br = node.find(q('BuildableReference'))
        name = (br.get('BlueprintName') if br is not None else '') or ''
        if name.endswith('Tests') and not name.endswith('UITests'):
            ts.remove(node)

# --- Drop non-UI test bundles from BuildAction (so Xcode doesn't prep them)
ba = r.find(q('BuildAction'))
bas = ba.find(q('BuildActionEntries')) if ba is not None else None
if bas is not None:
    for entry in list(bas):
        br = entry.find(q('BuildableReference'))
        name = (br.get('BlueprintName') if br is not None else '') or ''
        if name.endswith('Tests') and not name.endswith('UITests'):
            bas.remove(entry)

t.write(p, encoding='UTF-8', xml_declaration=True)
PY
}

# Project-level shared scheme
PRJ_SCHEME="$PROJECT_DIR/xcshareddata/xcschemes/${MAIN_NAME}-CI.xcscheme"
# Workspace-level shared scheme (if Pods created a workspace)
WS=""
for w in "$PROJECT_DIR"/*.xcworkspace; do
  [ -d "$w" ] && WS="$w" && break
done
WS_SCHEME=""
if [ -n "$WS" ]; then
  WS_SCHEME="$WS/xcshareddata/xcschemes/${MAIN_NAME}-CI.xcscheme"
fi

_prune_scheme "$PRJ_SCHEME"
[ -n "$WS_SCHEME" ] && _prune_scheme "$WS_SCHEME"

# Debug: show remaining testables in both scheme files
grep -n "BlueprintName" "$PRJ_SCHEME" 2>/dev/null || true
[ -n "$WS_SCHEME" ] && grep -n "BlueprintName" "$WS_SCHEME" 2>/dev/null || true

XCSCHEME_DIR="$PROJECT_DIR/xcshareddata/xcschemes"
XCSCHEME="$XCSCHEME_DIR/${MAIN_NAME}-CI.xcscheme"

if [ -f "$XCSCHEME" ]; then
  bia_log "Pruning unit-test testables from CI scheme at $XCSCHEME"

# --- Ensure the CI scheme has a TestAction with HelloCodenameOneUITests ---

PRJ="$PROJECT_DIR/HelloCodenameOne.xcodeproj"
PBX="$PRJ/project.pbxproj"

# Resolve the UITests target id
UIT_ID="$(awk '
  $0 ~ /\/\* Begin PBXNativeTarget section \*\// {inT=1}
  inT && $0 ~ /^[0-9A-F]{24} \/\* .* \*\/ = \{/ {last=$1}
  inT && $0 ~ /name = '"${MAIN_NAME}"'UITests;/ {print last; exit}
' "$PBX")"

if [ -z "$UIT_ID" ]; then
  bia_log "Error: ${MAIN_NAME}UITests target not found in $PBX"
  exit 1
fi

# Workspace-level scheme path
WS_SCHEME="$WORKSPACE/xcshareddata/xcschemes/${MAIN_NAME}-CI.xcscheme"
# Project-level shared scheme path (guard both)
PRJ_SCHEME="$PROJECT_DIR/xcshareddata/xcschemes/${MAIN_NAME}-CI.xcscheme"

_add_or_fix_testaction() {
  local scheme="$1"
  [ -f "$scheme" ] || return 0
  bia_log "Ensuring TestAction exists and references ${MAIN_NAME}UITests in $scheme"
  cp "$scheme" "$scheme.bak"

  /usr/bin/python3 - "$scheme" "$UIT_ID" "${MAIN_NAME}" <<'PY'
import sys, xml.etree.ElementTree as ET
scheme, uit_id, main = sys.argv[1:4]
t = ET.parse(scheme); r = t.getroot()
def q(n):
    if r.tag.startswith('{'):
        ns = r.tag.split('}')[0].strip('{')
        return f'{{{ns}}}{n}'
    return n

# 1) Drop non-UI testables from BuildAction (prevents prep of unit-tests)
ba = r.find(q('BuildAction'))
bas = ba.find(q('BuildActionEntries')) if ba is not None else None
if bas is not None:
    for e in list(bas):
        br = e.find(q('BuildableReference'))
        name = (br.get('BlueprintName') if br is not None else '') or ''
        if name.endswith('Tests') and not name.endswith('UITests'):
            bas.remove(e)

# 2) Ensure TestAction exists
ta = r.find(q('TestAction'))
if ta is None:
    ta = ET.SubElement(r, q('TestAction'), {
        'buildConfiguration':'Debug',
        'selectedDebuggerIdentifier':'Xcode.DebuggerFoundation.Debugger.LLDB',
        'selectedLauncherIdentifier':'Xcode.DebuggerFoundation.Launcher.LLDB',
        'shouldUseLaunchSchemeArgsEnv':'YES'
    })

# 3) Ensure Testables exists
ts = ta.find(q('Testables'))
if ts is None:
    ts = ET.SubElement(ta, q('Testables'))

# 4) Remove non-UI testables; ensure exactly one UITests
has_ui = False
for n in list(ts):
    br = n.find(q('BuildableReference'))
    name = (br.get('BlueprintName') if br is not None else '') or ''
    if name.endswith('Tests') and not name.endswith('UITests'):
        ts.remove(n)
    elif name == f"{main}UITests":
        has_ui = True

if not has_ui:
    tref = ET.SubElement(ts, q('TestableReference'), {'skipped':'NO'})
    ET.SubElement(tref, q('BuildableReference'), {
        'BuildableIdentifier':'primary',
        'BlueprintIdentifier': uit_id,
        'BlueprintName': f"{main}UITests",
        'ReferencedContainer': 'container:HelloCodenameOne.xcodeproj'
    })

t.write(scheme, encoding='UTF-8', xml_declaration=True)
PY
}

_add_or_fix_testaction "$WS_SCHEME"
_add_or_fix_testaction "$PRJ_SCHEME"

# Debug: prove the scheme now has a TestAction and the UITests testable
grep -n "<TestAction" "$WS_SCHEME" 2>/dev/null || true
grep -n 'BlueprintName="'"${MAIN_NAME}"'UITests"' "$WS_SCHEME" 2>/dev/null || true

  # Backup for debugging
  cp "$XCSCHEME" "$XCSCHEME.bak"

  /usr/bin/python3 - "$XCSCHEME" <<'PY'
import sys, xml.etree.ElementTree as ET
path = sys.argv[1]
tree = ET.parse(path)
root = tree.getroot()

# Handle (or not) namespace
def q(name):
    if root.tag.startswith('{'):
        ns = root.tag.split('}')[0].strip('{')
        return f'{{{ns}}}{name}'
    return name

test_action   = root.find(q('TestAction'))
testables     = test_action.find(q('Testables')) if test_action is not None else None
if testables is not None:
    removed = 0
    for t in list(testables):
        br = t.find(q('BuildableReference'))
        # Keep only UITests; drop everything else named *Tests but not *UITests
        if br is not None:
            bp = br.get('BlueprintName') or ''
            if bp.endswith('Tests') and not bp.endswith('UITests'):
                testables.remove(t)
                removed += 1
    if removed:
        # Ensure Testables exists even if empty (fine).
        pass

# Also make sure only UITests are marked as testables; nothing else sneaks in.
tree.write(path, encoding='UTF-8', xml_declaration=True)
PY

  # (Optional) show what remains in Testables
  grep -n "BuildableReference" "$XCSCHEME" || true
else
  bia_log "Warning: CI scheme not found at $XCSCHEME"
fi

WORKSPACE=""
for candidate in "$PROJECT_DIR"/*.xcworkspace; do
  if [ -d "$candidate" ]; then
    WORKSPACE="$candidate"
    break
  fi
done
if [ -z "$WORKSPACE" ]; then
  bia_log "Failed to locate xcworkspace in $PROJECT_DIR" >&2
  ls "$PROJECT_DIR" >&2 || true
  exit 1
fi

# ... after this block in build-ios-app.sh:
# if [ -z "$WORKSPACE" ]; then
#   bia_log "Failed to locate xcworkspace in $PROJECT_DIR" >&2
#   ls "$PROJECT_DIR" >&2 || true
#   exit 1
# fi

bia_log "Found xcworkspace: $WORKSPACE"

SCHEME="${MAIN_NAME}-CI"   # create-shared-scheme.py created this

# Make these visible to the next GH Actions step
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "workspace=$WORKSPACE"
    echo "scheme=$SCHEME"
  } >> "$GITHUB_OUTPUT"
fi

bia_log "Emitted outputs -> workspace=$WORKSPACE, scheme=$SCHEME"
bia_log "Final CI scheme files:"
[ -f "$PRJ_SCHEME" ] && ls -l "$PRJ_SCHEME"
[ -n "$WS_SCHEME" ] && [ -f "$WS_SCHEME" ] && ls -l "$WS_SCHEME"

exit 0
