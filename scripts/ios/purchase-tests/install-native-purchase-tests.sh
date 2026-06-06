#!/usr/bin/env bash
# Inject the native StoreKitTest XCTest sources into the generated iOS Xcode
# project. Keeps tests in-repo under scripts/ios/purchase-tests/native-tests
# while attaching them to the generated HelloCodenameOneTests target, links the
# StoreKit + StoreKitTest frameworks, and bundles Products.storekit as a test
# resource so SKTestSession can load it.

set -euo pipefail

PROJECT_DIR="${1:-}"
TEST_SOURCES_DIR="${2:-scripts/ios/purchase-tests/native-tests}"
STOREKIT_CONFIG="${3:-scripts/ios/purchase-tests/Products.storekit}"

log() { echo "[install-native-purchase-tests] $1"; }

if [ -z "$PROJECT_DIR" ] || [ ! -d "$PROJECT_DIR" ]; then
  log "project directory missing: $PROJECT_DIR" >&2
  exit 2
fi
if [ ! -d "$TEST_SOURCES_DIR" ]; then
  log "test sources directory missing: $TEST_SOURCES_DIR" >&2
  exit 2
fi
if [ ! -f "$STOREKIT_CONFIG" ]; then
  log "StoreKit configuration missing: $STOREKIT_CONFIG" >&2
  exit 2
fi

PROJECT_DIR="$(cd "$PROJECT_DIR" && pwd)"
TEST_SOURCES_DIR="$(cd "$TEST_SOURCES_DIR" && pwd)"
STOREKIT_CONFIG="$(cd "$(dirname "$STOREKIT_CONFIG")" && pwd)/$(basename "$STOREKIT_CONFIG")"
DEST_DIR="$PROJECT_DIR/NativePurchaseTests"
mkdir -p "$DEST_DIR"

# Copy test sources + the StoreKit config so they live alongside the project.
find "$TEST_SOURCES_DIR" -type f \( -name '*.m' -o -name '*.mm' -o -name '*.swift' -o -name '*.h' \) -print0 |
  while IFS= read -r -d '' src; do
    cp "$src" "$DEST_DIR/$(basename "$src")"
  done
cp "$STOREKIT_CONFIG" "$DEST_DIR/$(basename "$STOREKIT_CONFIG")"

ruby - "$PROJECT_DIR" "$DEST_DIR" "$(basename "$STOREKIT_CONFIG")" <<'RUBY'
require 'xcodeproj'
require 'fileutils'

project_dir = ARGV[0]
dest_dir = ARGV[1]
storekit_name = ARGV[2]
project_path = Dir[File.join(project_dir, '*.xcodeproj')].first
abort("No .xcodeproj found under #{project_dir}") unless project_path

project = Xcodeproj::Project.open(project_path)
test_target = project.targets.find { |t| t.product_type == 'com.apple.product-type.bundle.unit-test' } ||
              project.targets.find { |t| t.name.end_with?('Tests') }
abort("No unit-test target found in #{project_path}") unless test_target
app_target = project.targets.find { |t| t.product_type == 'com.apple.product-type.application' } ||
             project.targets.find { |t| t.name == test_target.name.sub(/Tests$/, '') }
abort("No app target found in #{project_path}") unless app_target

# Ensure the unit-test target has a concrete Info.plist in generated projects.
plist_name = "#{test_target.name}-Info.plist"
plist_rel = File.join(test_target.name, plist_name)
plist_abs = File.join(project_dir, plist_rel)
FileUtils.mkdir_p(File.dirname(plist_abs))
unless File.exist?(plist_abs)
  File.write(plist_abs, <<~PLIST)
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    <plist version="1.0">
    <dict>
      <key>CFBundleDevelopmentRegion</key>
      <string>$(DEVELOPMENT_LANGUAGE)</string>
      <key>CFBundleExecutable</key>
      <string>$(EXECUTABLE_NAME)</string>
      <key>CFBundleIdentifier</key>
      <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
      <key>CFBundleInfoDictionaryVersion</key>
      <string>6.0</string>
      <key>CFBundleName</key>
      <string>$(PRODUCT_NAME)</string>
      <key>CFBundlePackageType</key>
      <string>BNDL</string>
      <key>CFBundleShortVersionString</key>
      <string>1.0</string>
      <key>CFBundleVersion</key>
      <string>1</string>
    </dict>
    </plist>
  PLIST
end

# Hosted test bundle: StoreKitTest + the CN1 VM both need the app process.
test_target.build_configurations.each do |config|
  app_product = app_target.product_name || app_target.name
  host_path = "$(BUILT_PRODUCTS_DIR)/#{app_product}.app/#{app_product}"
  config.build_settings['TEST_TARGET_NAME'] = app_target.name
  config.build_settings['TEST_HOST'] = host_path
  config.build_settings['BUNDLE_LOADER'] = host_path
  config.build_settings['INFOPLIST_FILE'] = plist_rel
end

group = project.main_group.find_subpath('NativePurchaseTests', true)

# Attach .m/.mm/.swift sources to the test target's compile phase.
source_files = Dir[File.join(dest_dir, '*.{m,mm,swift}')].sort
abort("No test source files found in #{dest_dir}") if source_files.empty?
source_files.each do |source|
  rel_path = File.join('NativePurchaseTests', File.basename(source))
  file_ref = group.files.find { |f| f.path == rel_path } || group.new_file(rel_path)
  unless test_target.source_build_phase.files_references.include?(file_ref)
    test_target.source_build_phase.add_file_reference(file_ref, true)
  end
end

# Bundle the StoreKit configuration as a test resource so
# SKTestSession initWithConfigurationFileNamed: can find it.
sk_rel = File.join('NativePurchaseTests', storekit_name)
sk_ref = group.files.find { |f| f.path == sk_rel } || group.new_file(sk_rel)
unless test_target.resources_build_phase.files_references.include?(sk_ref)
  test_target.resources_build_phase.add_file_reference(sk_ref, true)
end

# Link StoreKit + StoreKitTest into the test target.
framework_group = project.frameworks_group || project.main_group['Frameworks'] || project.main_group.new_group('Frameworks')
['System/Library/Frameworks/StoreKit.framework',
 'System/Library/Frameworks/StoreKitTest.framework'].each do |fw|
  ref = framework_group.files.find { |f| f.path == fw } || framework_group.new_file(fw)
  unless test_target.frameworks_build_phase.files_references.include?(ref)
    test_target.frameworks_build_phase.add_file_reference(ref, true)
  end
end

project.save
puts "[install-native-purchase-tests] Installed #{source_files.length} test source file(s) + #{storekit_name} into #{test_target.name}"
RUBY
