// Snippets of the Splat chapter of the developer guide. Edit them here, not inline.

// tag::splat-sh-server[]
CN1_PROFILE=dev backend/server.sh run
// end::splat-sh-server[]

// tag::splat-sh-simulator[]
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::splat-sh-simulator[]

// tag::splat-sh-ios[]
mvn package -DskipTests -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-source -Dopen=false
// end::splat-sh-ios[]

// tag::splat-sh-tests[]
mvn -pl rules test
mvn -pl rules,shared,backend -Dcodename1.platform=backend test
./run-e2e.sh
// end::splat-sh-tests[]
