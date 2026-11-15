// Snippets of the Fieldline chapter of the developer guide. Edit them here, not inline.

// tag::fieldline-sh-server[]
cd scripts/fieldline
CN1_PROFILE=dev backend/server.sh run
// end::fieldline-sh-server[]

// tag::fieldline-sh-simulator[]
cd scripts/fieldline
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::fieldline-sh-simulator[]
