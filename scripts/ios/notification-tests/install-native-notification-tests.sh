#!/usr/bin/env bash
# Inject native XCTest sources into the generated iOS Xcode project.
# This keeps tests in-repo under scripts/ios/notification-tests/native-tests
# while attaching them to the generated HelloCodenameOneTests target.

set -euo pipefail

PROJECT_DIR="${1:-}"
TEST_SOURCES_DIR="${2:-scripts/ios/notification-tests/native-tests}"

if [ -z "$PROJECT_DIR" ] || [ ! -d "$PROJECT_DIR" ]; then
  echo "[install-native-notification-tests] project directory missing: $PROJECT_DIR" >&2
  exit 2
fi
if [ ! -d "$TEST_SOURCES_DIR" ]; then
  echo "[install-native-notification-tests] test sources directory missing: $TEST_SOURCES_DIR" >&2
  exit 2
fi

PROJECT_DIR="$(cd "$PROJECT_DIR" && pwd)"
TEST_SOURCES_DIR="$(cd "$TEST_SOURCES_DIR" && pwd)"
DEST_DIR="$PROJECT_DIR/NativeNotificationTests"
mkdir -p "$DEST_DIR"

# Copy source files so they live alongside generated project sources.
find "$TEST_SOURCES_DIR" -type f \( -name '*.m' -o -name '*.mm' -o -name '*.swift' -o -name '*.h' \) -print0 |
  while IFS= read -r -d '' src; do
    cp "$src" "$DEST_DIR/$(basename "$src")"
  done

# Attach copied files to the unit-test target using xcodeproj gem.
ruby - "$PROJECT_DIR" "$DEST_DIR" <<'RUBY'
require 'xcodeproj'
require 'fileutils'

project_dir = ARGV[0]
dest_dir = ARGV[1]
project_path = Dir[File.join(project_dir, '*.xcodeproj')].first
abort("No .xcodeproj found under #{project_dir}") unless project_path

project = Xcodeproj::Project.open(project_path)
test_target = project.targets.find { |t| t.product_type == 'com.apple.product-type.bundle.unit-test' } ||
              project.targets.find { |t| t.name.end_with?('Tests') }
abort("No unit-test target found in #{project_path}") unless test_target
app_target = project.targets.find { |t| t.product_type == 'com.apple.product-type.application' } ||
             project.targets.find { |t| t.name == test_target.name.sub(/Tests$/, '') }
abort("No app target found in #{project_path}") unless app_target

# Ensure the unit-test target has a concrete Info.plist file in generated projects.
# Some generated projects point to a plist path that doesn't exist on disk.
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

# Configure XCTest as a hosted test bundle so APIs that require app process
# context (e.g. UNUserNotificationCenter) are available during tests.
test_target.build_configurations.each do |config|
  app_product = app_target.product_name || app_target.name
  host_path = "$(BUILT_PRODUCTS_DIR)/#{app_product}.app/#{app_product}"
  config.build_settings['TEST_TARGET_NAME'] = app_target.name
  config.build_settings['TEST_HOST'] = host_path
  config.build_settings['BUNDLE_LOADER'] = host_path
  config.build_settings['INFOPLIST_FILE'] = plist_rel
end

group = project.main_group.find_subpath('NativeNotificationTests', true)
source_files = Dir[File.join(dest_dir, '*.{m,mm,swift}')].sort
abort("No test source files found in #{dest_dir}") if source_files.empty?

source_files.each do |source|
  rel_path = File.join('NativeNotificationTests', File.basename(source))
  file_ref = group.files.find { |f| f.path == rel_path } || group.new_file(rel_path)
  unless test_target.source_build_phase.files_references.include?(file_ref)
    test_target.source_build_phase.add_file_reference(file_ref, true)
  end
end

framework_group = project.frameworks_group || project.main_group['Frameworks'] || project.main_group.new_group('Frameworks')
user_notifications_ref = framework_group.files.find { |f| f.path == 'System/Library/Frameworks/UserNotifications.framework' } ||
                         framework_group.new_file('System/Library/Frameworks/UserNotifications.framework')
unless test_target.frameworks_build_phase.files_references.include?(user_notifications_ref)
  test_target.frameworks_build_phase.add_file_reference(user_notifications_ref, true)
end

project.save
puts "[install-native-notification-tests] Installed #{source_files.length} native notification test source file(s) into #{test_target.name}"
RUBY
