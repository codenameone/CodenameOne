// Snippets of the Bastion chapters of the developer guide. Edit them here, not inline.

// tag::bastion-sh-impl-1[]
cd scripts/bastion
CN1_PROFILE=dev backend/server.sh run
// end::bastion-sh-impl-1[]

// tag::bastion-sh-impl-2[]
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::bastion-sh-impl-2[]

// tag::bastion-sh-impl-3[]
cd scripts/bastion
# The server's tests on the JVM, then compiled to a native binary
mvn -Dcodename1.platform=backend -pl shared,backend test -Dcn1.backend.compiledTests=true
# The same tests on PostgreSQL or MySQL
CN1_TEST_DATABASE_URL="postgres://user:password@localhost:5432/db?sslmode=disable" \
  ../ci/sample-server-tests-on-database.sh .
# The app's own tests, which run headless as part of the build
mvn -Dcodename1.platform=javase -Djava.awt.headless=true package
# A running server, driven over HTTP the way the app drives it
./run-e2e.sh
./run-e2e.sh --native
# The app in a headless browser, served by the server
./run-web-e2e.sh
# The same, then every screen in the user's guide, into docs/developer-guide/img
./run-web-e2e.sh --guide
// end::bastion-sh-impl-3[]

// tag::bastion-sh-users-1[]
cd scripts/bastion
CN1_PROFILE=dev backend/server.sh run
// end::bastion-sh-users-1[]

// tag::bastion-sh-users-2[]
cd scripts/bastion
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::bastion-sh-users-2[]
