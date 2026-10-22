// Snippets of the Parley chapters of the developer guide. Edit them here, not inline.

// tag::parley-sh-server[]
CN1_PROFILE=dev backend/server.sh run
// end::parley-sh-server[]

// tag::parley-sh-simulator[]
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::parley-sh-simulator[]

// tag::parley-sh-tests[]
mvn -pl shared,backend -Dcodename1.platform=backend test
mvn -pl shared,backend -Dcodename1.platform=backend test -Dcn1.backend.compiledTests=true
./run-e2e.sh
./run-web-e2e.sh
// end::parley-sh-tests[]

// tag::parley-sh-turn[]
parley.ice.stun=stun:stun.example.org:3478
parley.ice.turn=turn:turn.example.org:3478,turns:turn.example.org:5349
parley.ice.secret=${PARLEY_ICE_SECRET}
parley.ice.ttl=86400
// end::parley-sh-turn[]

// tag::parley-sh-bridges[]
parley.telegram.token=${PARLEY_TELEGRAM_TOKEN}
parley.telegram.secret=${PARLEY_TELEGRAM_SECRET}
parley.telegram.bot=example_parley_bot
parley.whatsapp.token=${PARLEY_WHATSAPP_TOKEN}
parley.whatsapp.phone=${PARLEY_WHATSAPP_PHONE}
parley.whatsapp.secret=${PARLEY_WHATSAPP_SECRET}
parley.whatsapp.verify=${PARLEY_WHATSAPP_VERIFY}
// end::parley-sh-bridges[]
