// Snippets of the Dash chapter of the developer guide. Edit them here, not inline.

// tag::dash-sh-server[]
CN1_PROFILE=dev backend/server.sh run
// end::dash-sh-server[]

// tag::dash-sh-simulator[]
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::dash-sh-simulator[]

// tag::dash-sh-ios[]
mvn package -DskipTests -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-source -Dopen=false
// end::dash-sh-ios[]

// tag::dash-sh-tests[]
mvn -pl rules test
mvn -pl rules,shared,backend -Dcodename1.platform=backend test
./run-e2e.sh
// end::dash-sh-tests[]
