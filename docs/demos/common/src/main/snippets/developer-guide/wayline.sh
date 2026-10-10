// Snippets of the Wayline chapters of the developer guide. Edit them here, not inline.

// tag::wayline-sh-server[]
cd scripts/wayline
CN1_PROFILE=dev backend/server.sh run
// end::wayline-sh-server[]

// tag::wayline-sh-simulator[]
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::wayline-sh-simulator[]

// tag::wayline-sh-web[]
backend/server.sh web
CN1_PROFILE=dev backend/server.sh run
// end::wayline-sh-web[]

// tag::wayline-sh-tests[]
mvn -pl shared,backend -Dcodename1.platform=backend test   # <1>
mvn -pl shared,backend -Dcodename1.platform=backend test -Dcn1.backend.compiledTests=true   # <2>
./run-e2e.sh   # <3>
./check-template.sh   # <4>
./run-web-e2e.sh   # <5>
// end::wayline-sh-tests[]

// tag::wayline-sh-native[]
backend/server.sh build --native
backend/server.sh run --native --prebuilt --port 8080
// end::wayline-sh-native[]

// tag::wayline-sh-database[]
DATABASE_URL=postgres://wayline:secret@db.internal/wayline backend/server.sh run
// end::wayline-sh-database[]
