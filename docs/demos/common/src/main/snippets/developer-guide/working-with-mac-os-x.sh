// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::working-with-mac-os-x-shell-001[]
mvn -B -Dcodename1.platform=ios -Dcodename1.buildTarget=mac-source package
// end::working-with-mac-os-x-shell-001[]

// tag::working-with-mac-os-x-shell-002[]
mvn -B -Dcodename1.platform=ios -Dcodename1.buildTarget=mac-os-x-native package
// end::working-with-mac-os-x-shell-002[]

// tag::working-with-mac-os-x-shell-003[]
xcodebuild -exportArchive \
  -archivePath build/<MainClass>.xcarchive \
  -exportOptionsPlist dist/ExportOptions-AppStore-Mac.plist \
  -exportPath build/export
// end::working-with-mac-os-x-shell-003[]

// tag::working-with-mac-os-x-shell-004[]
# Catalyst is an iOS build. Turn it on with the hint, in
# codenameone_settings.properties, and build the iOS project target:
#   codename1.arg.macNative.enabled=true
mvn -B -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-source package
// end::working-with-mac-os-x-shell-004[]

// tag::working-with-mac-os-x-shell-005[]
mvn -B -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-device package
// end::working-with-mac-os-x-shell-005[]
