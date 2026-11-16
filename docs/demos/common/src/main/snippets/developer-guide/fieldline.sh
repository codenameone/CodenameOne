// Snippets of the Fieldline chapter of the developer guide. Edit them here, not inline.

// tag::fieldline-sh-server[]
cd scripts/fieldline
CN1_PROFILE=dev backend/server.sh run
// end::fieldline-sh-server[]

// tag::fieldline-sh-simulator[]
cd scripts/fieldline
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
// end::fieldline-sh-simulator[]

// tag::fieldline-sh-web[]
cd scripts/fieldline
backend/server.sh web
CN1_PROFILE=dev backend/server.sh run
// end::fieldline-sh-web[]

// tag::fieldline-sh-production[]
export CN1_PROFILE=prod
export DATABASE_URL=postgres://fieldline:secret@db.example.com:5432/fieldline
export FIELDLINE_ADMIN_EMAIL=you@example.com
export FIELDLINE_ADMIN_PASSWORD=change-me
export FIELDLINE_AI_KEY=sk-ant-your-key
export FIELDLINE_PUSH_APP=your-push-app-id
export FIELDLINE_PUSH_KEY=your-push-server-key
export FIELDLINE_ZONE=America/Los_Angeles
backend/server.sh run --native
// end::fieldline-sh-production[]

// tag::fieldline-sh-tests[]
cd scripts/fieldline
./run-e2e.sh
./run-web-e2e.sh
./check-template.sh
// end::fieldline-sh-tests[]
