#!/usr/bin/env bash
# Build a sample "Hello Codename One" iOS application using the locally-built Codename One iOS port
set -euo pipefail

bia_log() { echo "[build-ios-app] $1"; }

# Pin Xcode so CN1’s Java subprocess sees xcodebuild
export DEVELOPER_DIR="/Applications/Xcode_16.4.app/Contents/Developer"
export PATH="$DEVELOPER_DIR/usr/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
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

# Local Maven repo + command wrapper (define BEFORE using it)
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

# --- Build iOS project (ios-source) ---
DERIVED_DATA_DIR="${TMPDIR}/codenameone-ios-derived"
rm -rf "$DERIVED_DATA_DIR"; mkdir -p "$DERIVED_DATA_DIR"

xcodebuild -version

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

# --- Ensure a real UITest source file exists on disk ---
UITEST_TEMPLATE="$SCRIPT_DIR/ios/tests/HelloCodenameOneUITests.m.tmpl"
UITEST_DIR="$PROJECT_DIR/HelloCodenameOneUITests"
UITEST_SOURCE="$UITEST_DIR/HelloCodenameOneUITests.m"
if [ -f "$UITEST_TEMPLATE" ]; then
  mkdir -p "$UITEST_DIR"
  cp -f "$UITEST_TEMPLATE" "$UITEST_SOURCE"
  bia_log "Installed UITest source: $UITEST_SOURCE"
else
  bia_log "UITest template missing at $UITEST_TEMPLATE"; exit 1
fi

# --- Ruby/gem environment (xcodeproj) ---
if ! command -v ruby >/dev/null; then
  bia_log "ruby not found on PATH"; exit 1
fi
USER_GEM_BIN="$(ruby -e 'print Gem.user_dir')/bin"
export PATH="$USER_GEM_BIN:$PATH"
if ! ruby -rrubygems -e 'exit(Gem::Specification.find_all_by_name("xcodeproj").empty? ? 1 : 0)'; then
  bia_log "Installing xcodeproj gem for current ruby"
  gem install xcodeproj --no-document --user-install
fi
ruby -rrubygems -e 'abort("xcodeproj gem still missing") if Gem::Specification.find_all_by_name("xcodeproj").empty?'

# --- Locate the .xcodeproj and pass its path to Ruby ---
XCODEPROJ="$PROJECT_DIR/HelloCodenameOne.xcodeproj"
if [ ! -d "$XCODEPROJ" ]; then
  XCODEPROJ="$(/bin/ls -1d "$PROJECT_DIR"/*.xcodeproj 2>/dev/null | head -n1 || true)"
fi
if [ -z "$XCODEPROJ" ] || [ ! -d "$XCODEPROJ" ]; then
  bia_log "Failed to locate .xcodeproj under $PROJECT_DIR"; exit 1
fi
export XCODEPROJ
bia_log "Using Xcode project: $XCODEPROJ"

# --- Ensure UITests target + CI scheme (save_as gets a PATH, not a Project) ---
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


# Ensure a group and file reference exist, then add to the UITest target
proj_dir   = File.dirname(proj_path)
ui_dir     = File.join(proj_dir, ui_name)
ui_file    = File.join(ui_dir, "#{ui_name}.m")
ui_group   = proj.main_group.find_subpath(ui_name, true)
ui_group.set_source_tree("<group>")
file_ref = ui_group.files.find { |f| File.expand_path(f.path, proj_dir) == ui_file }
file_ref ||= ui_group.new_file(ui_file)
ui_target.add_file_references([file_ref]) unless ui_target.source_build_phase.files_references.include?(file_ref)

# Ensure required system frameworks (e.g. UIKit for UIImage helpers) are linked
frameworks_group = proj.frameworks_group || proj.main_group.find_subpath("Frameworks", true)
frameworks_group.set_source_tree("<group>") if frameworks_group.respond_to?(:set_source_tree)
{
  "UIKit.framework" => "System/Library/Frameworks/UIKit.framework"
}.each do |name, path|
  ref = frameworks_group.files.find do |f|
    f.path == name || f.path == path ||
      (f.respond_to?(:real_path) && File.expand_path(f.real_path.to_s) == File.expand_path(path, "/"))
  end
  unless ref
    ref = frameworks_group.new_reference(path)
  end
  ref.name = name if ref.respond_to?(:name=)
  ref.set_source_tree('SDKROOT') if ref.respond_to?(:set_source_tree)
  ref.path = path if ref.respond_to?(:path=)
  ref.last_known_file_type = 'wrapper.framework' if ref.respond_to?(:last_known_file_type=)
  phase = ui_target.frameworks_build_phase
  unless phase.files_references.include?(ref)
    phase.add_file_reference(ref)
  end
end

#
# Required settings so Xcode creates a non-empty .xctest and a proper "-Runner.app"
# PRODUCT_NAME feeds the bundle name; TEST_TARGET_NAME feeds the runner name.
# We also keep signing off and auto-Info.plist for simulator CI.
#
%w[Debug Release].each do |cfg|
  xc = ui_target.build_configuration_list[cfg]
  next unless xc
  bs = xc.build_settings
  bs["GENERATE_INFOPLIST_FILE"]      = "YES"
  bs["CODE_SIGNING_ALLOWED"]         = "NO"
  bs["CODE_SIGNING_REQUIRED"]        = "NO"
  bs["PRODUCT_BUNDLE_IDENTIFIER"]  ||= "com.codenameone.examples.uitests"
  bs["PRODUCT_NAME"]               ||= ui_name
  bs["TEST_TARGET_NAME"]           ||= app_target&.name || "HelloCodenameOne"
  # Optional but harmless on simulators; avoids other edge cases:
  bs["TARGETED_DEVICE_FAMILY"] ||= "1,2"
end

proj.save

ws_dir = File.join(File.dirname(proj_path), "HelloCodenameOne.xcworkspace")
schemes_root = if File.directory?(ws_dir)
  File.join(ws_dir, "xcshareddata", "xcschemes")
else
  File.join(File.dirname(proj_path), "xcshareddata", "xcschemes")
end
FileUtils.mkdir_p(schemes_root)

scheme = Xcodeproj::XCScheme.new
scheme.build_action.entries = []
scheme.add_build_target(app_target) if app_target
scheme.test_action = Xcodeproj::XCScheme::TestAction.new
scheme.test_action.xml_element.elements.delete_all("EnvironmentVariables")
envs = Xcodeproj::XCScheme::EnvironmentVariables.new
envs.assign_variable(key: "CN1SS_OUTPUT_DIR",  value: "__CN1SS_OUTPUT_DIR__",  enabled: true)
envs.assign_variable(key: "CN1SS_PREVIEW_DIR", value: "__CN1SS_PREVIEW_DIR__", enabled: true)
scheme.test_action.environment_variables = envs
scheme.test_action.xml_element.elements.delete_all("Testables")
scheme.add_test_target(ui_target)
scheme.launch_action.build_configuration = "Debug"
scheme.test_action.build_configuration   = "Debug"

save_root = File.directory?(ws_dir) ? ws_dir : File.dirname(proj_path)
scheme.save_as(save_root, "HelloCodenameOne-CI", true)
'

# Show which scheme file we ended up with
WS_XCSCHEME="$PROJECT_DIR/HelloCodenameOne.xcworkspace/xcshareddata/xcschemes/HelloCodenameOne-CI.xcscheme"
PRJ_XCSCHEME="$PROJECT_DIR/xcshareddata/xcschemes/HelloCodenameOne-CI.xcscheme"
if [ -f "$WS_XCSCHEME" ]; then
  bia_log "CI scheme (workspace): $WS_XCSCHEME"; grep -n "BlueprintName" "$WS_XCSCHEME" || true
elif [ -f "$PRJ_XCSCHEME" ]; then
  bia_log "CI scheme (project):   $PRJ_XCSCHEME"; grep -n "BlueprintName" "$PRJ_XCSCHEME" || true
else
  bia_log "Warning: CI scheme not found after generation"
fi

# Patch PBX TEST_HOST (remove any "-src" suffix that can break unit-tests)
PBXPROJ="$PROJECT_DIR/HelloCodenameOne.xcodeproj/project.pbxproj"
if [ -f "$PBXPROJ" ]; then
  bia_log "Patching TEST_HOST in $PBXPROJ (remove '-src' suffix)"
  cp "$PBXPROJ" "$PBXPROJ.bak"
  perl -0777 -pe 's/(TEST_HOST = .*?\.app\/)([^"\/]+)-src(";\n)/$1$2$3/s' \
    "$PBXPROJ.bak" > "$PBXPROJ"
  grep -n "TEST_HOST =" "$PBXPROJ" || true
fi

# CocoaPods (project contains a Podfile but usually empty — fine)
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

# Remove any user schemes that could shadow the shared CI scheme
rm -rf "$PROJECT_DIR"/xcuserdata 2>/dev/null || true
find "$PROJECT_DIR" -maxdepth 1 -name "*.xcworkspace" -type d -exec rm -rf {}/xcuserdata \; 2>/dev/null || true

# Locate workspace for the next step
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
bia_log "Found xcworkspace: $WORKSPACE"

SCHEME="${MAIN_NAME}-CI"

# Make these visible to the next GH Actions step
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "workspace=$WORKSPACE"
    echo "scheme=$SCHEME"
  } >> "$GITHUB_OUTPUT"
fi

bia_log "Emitted outputs -> workspace=$WORKSPACE, scheme=$SCHEME"

# (Optional) dump xcodebuild -list for debugging
ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/artifacts}"
mkdir -p "$ARTIFACTS_DIR"
xcodebuild -workspace "$WORKSPACE" -list > "$ARTIFACTS_DIR/xcodebuild-list.txt" 2>&1 || true

exit 0