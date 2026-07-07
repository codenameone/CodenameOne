// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::testing-with-junit-bash-001[]
mvn -pl javase test                                     # all JUnit + cn1:test
mvn -pl javase test -Dtest=GreetingFormTest             # one class
mvn -pl javase test -Dtest=GreetingFormTest#formShowsExpectedTitle   # one method
// end::testing-with-junit-bash-001[]

// tag::testing-with-junit-bash-002[]
mvn -pl javase test                            # both runners
mvn -pl javase test -DskipTests                # skip Surefire, cn1:test still runs
mvn -pl javase test -Dtest=NoMatch             # filter Surefire to nothing,
                                               # cn1:test still runs
// end::testing-with-junit-bash-002[]
