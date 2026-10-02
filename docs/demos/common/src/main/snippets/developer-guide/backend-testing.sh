// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::backend-testing-bash-001[]
mvn -pl backend -Dcodename1.platform=backend test
// end::backend-testing-bash-001[]

// tag::backend-testing-bash-002[]
mvn -pl backend -Dcodename1.platform=backend test -Dcn1.backend.compiledTests=true
// end::backend-testing-bash-002[]
