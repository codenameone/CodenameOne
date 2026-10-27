// Snippets of the Reel chapters of the developer guide. Edit them here, not inline.

// tag::reel-sh-run[]
cd scripts/reel
tools/fetch-demo-media.sh --small      # once: download and package the films
CN1_PROFILE=dev backend/server.sh run
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::reel-sh-run[]

// tag::reel-sh-drm[]
reel.drm.widevine.url=https://license.example.com/widevine
reel.drm.widevine.header=Authorization: Bearer ...
reel.drm.fairplay.url=https://license.example.com/fairplay
reel.drm.fairplay.certificate=https://license.example.com/fairplay.cer
// end::reel-sh-drm[]

// tag::reel-sh-worker[]
java -jar tools/packager/target/reel-packager-1.0-SNAPSHOT.jar \
    worker --server https://reel.example.com --token "$REEL_WORKER_TOKEN"
// end::reel-sh-worker[]

// tag::reel-sh-tests[]
mvn -pl shared,backend -Dcodename1.platform=backend test
mvn -pl shared,backend -Dcodename1.platform=backend test -Dcn1.backend.compiledTests=true
./run-e2e.sh
./run-web-e2e.sh
./check-template.sh
// end::reel-sh-tests[]
