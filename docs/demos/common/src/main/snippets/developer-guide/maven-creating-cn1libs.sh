// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::maven-creating-cn1libs-bash-001[]
mvn archetype:generate \
  -DarchetypeArtifactId=cn1lib-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion=LATEST \
  -DgroupId=com.example.mylib \
  -DartifactId=mylib \
  -Dversion=1.0-SNAPSHOT \
  -DinteractiveMode=false
// end::maven-creating-cn1libs-bash-001[]

// tag::maven-creating-cn1libs-bash-002[]
mvn archetype:generate
// end::maven-creating-cn1libs-bash-002[]

// tag::maven-creating-cn1libs-bash-003[]
mvn archetype:generate -DarchetypeGroupId=com.codenameone \
  -DarchetypeArtifactId=cn1lib-archetype
// end::maven-creating-cn1libs-bash-003[]

// tag::maven-creating-cn1libs-bash-004[]
mvn install
// end::maven-creating-cn1libs-bash-004[]
