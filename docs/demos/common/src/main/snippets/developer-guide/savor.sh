// Snippets of the Savor chapters of the developer guide. Edit them here, not inline.

// tag::savor-sh-server[]
CN1_PROFILE=dev backend/server.sh run
// end::savor-sh-server[]

// tag::savor-sh-simulator[]
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::savor-sh-simulator[]

// tag::savor-sh-tests[]
mvn -pl shared,backend -Dcodename1.platform=backend test
mvn -pl shared,backend -Dcodename1.platform=backend test -Dcn1.backend.compiledTests=true
./run-e2e.sh
./run-web-e2e.sh
// end::savor-sh-tests[]
