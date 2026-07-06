// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::appendix-goal-generate-graphql-bash-001[]
mvn -pl common cn1:generate-graphql \
    -Dcn1.graphql.schema=schema.graphqls \
    -Dcn1.graphql.operations=operations.graphql \
    -Dcn1.graphql.basePackage=com.example.starwars
// end::appendix-goal-generate-graphql-bash-001[]
