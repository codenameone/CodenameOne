// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::android-interop-bash-001[]
mvn cn1:import-android-project -Dcn1.android.import=/path/to/MyAndroidApp
// end::android-interop-bash-001[]

// tag::android-interop-bash-002[]
mvn cn1:run
mvn cn1:build -Dcodename1.platform=ios
// end::android-interop-bash-002[]
