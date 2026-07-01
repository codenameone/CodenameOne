// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::appendix-goal-generate-grpc-bash-001[]
mvn -pl common cn1:generate-grpc \
    -Dcn1.grpc.proto=helloworld.proto \
    -Dcn1.grpc.basePackage=com.example.hello
// end::appendix-goal-generate-grpc-bash-001[]
