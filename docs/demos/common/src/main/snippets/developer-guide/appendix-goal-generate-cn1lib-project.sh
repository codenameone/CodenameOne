// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::appendix-goal-generate-cn1lib-project-bash-001[]
mvn com.codenameone:codenameone-maven-plugin:$CN1VERSION:generate-cn1lib-project \
  -DsourceProject=/path/to/MyLegacyAntLibraryProject \
  -DgroupId=com.example \
  -DartifactId=my-maven-lib \
  -Dversion=1.0-SNAPSHOT \
  -U
// end::appendix-goal-generate-cn1lib-project-bash-001[]

// tag::appendix-goal-generate-cn1lib-project-bash-002[]
cd my-maven-lib
mvn install
// end::appendix-goal-generate-cn1lib-project-bash-002[]
