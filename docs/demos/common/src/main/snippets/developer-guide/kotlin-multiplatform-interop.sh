// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::kotlin-multiplatform-interop-bash-001[]
mvn cn1:import-kmp-project -Dcn1.kmp.import=/path/to/MyKmpApp
// end::kotlin-multiplatform-interop-bash-001[]

// tag::kotlin-multiplatform-interop-bash-002[]
mvn cn1:run                         # the simulator
mvn cn1:build -Dcodename1.platform=ios   # an iOS build on the build server
// end::kotlin-multiplatform-interop-bash-002[]
