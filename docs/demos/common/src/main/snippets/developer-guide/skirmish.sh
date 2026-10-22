// Snippets of the Skirmish chapters of the developer guide. Edit them here, not inline.

// tag::skirmish-sh-server[]
CN1_PROFILE=dev backend/server.sh run
// end::skirmish-sh-server[]

// tag::skirmish-sh-simulator[]
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::skirmish-sh-simulator[]

// tag::skirmish-sh-ios[]
mvn package -DskipTests -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-source
// end::skirmish-sh-ios[]

// tag::skirmish-sh-tests[]
mvn -pl rules test
mvn -pl rules,shared,backend -Dcodename1.platform=backend test
./run-e2e.sh
// end::skirmish-sh-tests[]

// tag::skirmish-sh-portraits[]
mvn -pl common test-compile surefire:test -Dtest=PortraitStudio \
    -Dskirmish.portraits=common/src/main/resources
// end::skirmish-sh-portraits[]
