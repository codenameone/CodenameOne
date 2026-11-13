// Snippets of the Slate chapters of the developer guide. Edit them here, not inline.

// tag::slate-sh-server[]
cd scripts/slate
CN1_PROFILE=dev backend/server.sh run
// end::slate-sh-server[]

// tag::slate-sh-simulator[]
cd scripts/slate
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::slate-sh-simulator[]

// tag::slate-sh-web[]
cd scripts/slate
backend/server.sh web
CN1_PROFILE=dev backend/server.sh run
// end::slate-sh-web[]

// tag::slate-sh-tests[]
cd scripts/slate
# The server's tests on the JVM, then compiled to a native binary
mvn -Dcodename1.platform=backend -pl shared,backend test -Dcn1.backend.compiledTests=true
# The same tests on PostgreSQL or MySQL
CN1_TEST_DATABASE_URL="postgres://user:password@localhost:5432/db?sslmode=disable" \
  ../ci/sample-server-tests-on-database.sh .
# The app against a running server, in the simulator
./run-e2e.sh
./run-e2e.sh --native
# The app in a headless browser, served by the server
./run-web-e2e.sh
// end::slate-sh-tests[]
