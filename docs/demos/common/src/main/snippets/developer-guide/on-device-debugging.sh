// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::on-device-debugging-bash-001[]
# Terminal 1 — start the proxy
mvn cn1:ios-on-device-debugging

# Terminal 2 — attach jdb
jdb -attach localhost:8000
// end::on-device-debugging-bash-001[]

// tag::on-device-debugging-bash-002[]
jdb -attach localhost:8000 \
    -sourcepath src/main/java:$HOME/.m2/repository/com/codenameone/codenameone-core/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT-sources.jar
// end::on-device-debugging-bash-002[]
