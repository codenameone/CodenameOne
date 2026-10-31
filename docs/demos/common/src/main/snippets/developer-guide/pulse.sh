// Snippets of the Pulse chapters of the developer guide. Edit them here, not inline.

// tag::pulse-sh-server[]
cd scripts/pulse
CN1_PROFILE=dev backend/server.sh run
// end::pulse-sh-server[]

// tag::pulse-sh-simulator[]
cd scripts/pulse
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::pulse-sh-simulator[]

// tag::pulse-sh-web[]
cd scripts/pulse
backend/server.sh web
CN1_PROFILE=dev backend/server.sh run
// end::pulse-sh-web[]

// tag::pulse-sh-tests[]
cd scripts/pulse
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
// end::pulse-sh-tests[]
