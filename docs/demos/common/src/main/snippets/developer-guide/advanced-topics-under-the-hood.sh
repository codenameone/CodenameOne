// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::advanced-topics-under-the-hood-bash-001[]
mvn archetype:generate \
  -DarchetypeArtifactId=cn1lib-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion=LATEST \
  -DgroupId=com.example.mylib \
  -DartifactId=mylib \
  -Dversion=1.0-SNAPSHOT \
  -DinteractiveMode=false
// end::advanced-topics-under-the-hood-bash-001[]

// tag::advanced-topics-under-the-hood-bash-002[]
java -jar ~/.codenameone/UpdateCodenameOne.jar path_to_my_codenameone_project
// end::advanced-topics-under-the-hood-bash-002[]

// tag::advanced-topics-under-the-hood-bash-003[]
java -jar ~/.codenameone/UpdateCodenameOne.jar path_to_my_codenameone_project
// end::advanced-topics-under-the-hood-bash-003[]
