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
