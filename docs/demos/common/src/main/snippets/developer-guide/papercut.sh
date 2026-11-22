// Snippets of the Papercut chapter of the developer guide. Edit them here, not inline.

// tag::papercut-sh-server[]
CN1_PROFILE=dev backend/server.sh run
// end::papercut-sh-server[]

// tag::papercut-sh-simulator[]
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::papercut-sh-simulator[]

// tag::papercut-sh-ios[]
mvn package -DskipTests -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-source -Dopen=false
// end::papercut-sh-ios[]

// tag::papercut-sh-art[]
python3 -I tools/make-art.py ~/papercut-sources
// end::papercut-sh-art[]

// tag::papercut-sh-tests[]
./run-e2e.sh
mvn -pl shared,backend -Dcodename1.platform=backend test
// end::papercut-sh-tests[]
