// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::backend-web-hosting[]
mvn -pl backend -Dcodename1.platform=backend cn1:backend-webapp <1>
mvn -pl backend -am -Dcodename1.platform=backend cn1:backend <2>
// end::backend-web-hosting[]

// tag::backend-web-hosting-gradle[]
./gradlew :backend:backendWebApp <1>
./gradlew :backend:runBackend <2>
// end::backend-web-hosting-gradle[]

// tag::backend-vt-stack-sh-001[]
CN1_HTTP_VT_STACK=1048576 ./server
// end::backend-vt-stack-sh-001[]
