// Snippets of the Bazaar chapters of the developer guide. Edit them here, not inline.

// tag::bazaar-sh-server[]
CN1_PROFILE=dev backend/server.sh run
// end::bazaar-sh-server[]

// tag::bazaar-sh-simulator[]
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::bazaar-sh-simulator[]

// tag::bazaar-sh-tests[]
mvn -pl shared,backend -Dcodename1.platform=backend test
mvn -pl shared,backend -Dcodename1.platform=backend test -Dcn1.backend.compiledTests=true
./run-e2e.sh
./run-web-e2e.sh
// end::bazaar-sh-tests[]
