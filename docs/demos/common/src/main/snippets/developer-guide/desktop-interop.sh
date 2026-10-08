// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::desktop-interop-bash-001[]
mvn cn1:import-desktop-project -Dcn1.desktop.import=/path/to/MyDesktopApp
// end::desktop-interop-bash-001[]

// tag::desktop-interop-bash-002[]
mvn cn1:import-desktop-project -Dcn1.desktop.import=/path/to/MyDesktopApp \
    -Dcn1.desktop.module=app \
    -Dcn1.desktop.mainClass=com.example.Main
// end::desktop-interop-bash-002[]

// tag::desktop-interop-bash-003[]
mvn cn1:run
mvn cn1:build -Dcodename1.platform=ios
// end::desktop-interop-bash-003[]
