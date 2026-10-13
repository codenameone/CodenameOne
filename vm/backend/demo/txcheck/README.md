# Transaction completion and isolation probe

Run this probe locally on Java 8 and ParparVM. It covers completion ordering,
resource suspension, nested rollback, callback failures, commit failure, and
connection reuse. SQLite is the default. Set `CN1_TXCHECK_URL` to an isolated
PostgreSQL or MySQL database to exercise that engine; the probe creates/drops
`cn1_sync_*` tables. Commit-failure checks use deferred constraints and run only
on SQLite and PostgreSQL.

From the repository root on macOS:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export JDK_8_HOME="$JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
CN1_BACKEND_DEMO=demo/txcheck CN1_BACKEND_JAVA="$JAVA_HOME" \
  vm/backend/run-javase.sh com.demo.TransactionCheck
CN1_BACKEND_DEMO=demo/txcheck CN1_BACKEND_STANDALONE_DEMO=1 \
  vm/backend/build.sh TransactionCheck com.demo /tmp/cn1-transaction-check
/tmp/cn1-transaction-check
```

`run-javase.sh` uses the existing backend demo contract generation prerequisites.
Both runs must print `TXCHECK OK` with the same check count for the same database.
No hosted native-test job is needed or permitted.
