// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::appendix-goal-generate-openapi-bash-001[]
mvn -pl common cn1:generate-openapi \
    -Dcn1.openapi.spec=petstore.json \
    -Dcn1.openapi.basePackage=com.example.petstore
// end::appendix-goal-generate-openapi-bash-001[]
