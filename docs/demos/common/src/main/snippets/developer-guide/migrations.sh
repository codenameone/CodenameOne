// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::migrations-bash-001[]
mvn cn1:migrate-info
mvn cn1:migrate
mvn cn1:migrate-validate
mvn cn1:migrate-repair
mvn cn1:migrate-baseline -Dcn1.flyway.baselineVersion=12
// end::migrations-bash-001[]

// tag::migrations-bash-002[]
mvn cn1:migrate -Dcn1.datasource.url=postgres://app:secret@db.internal/app
// end::migrations-bash-002[]
