// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::appendix-goal-generate-app-project-bash-001[]
mvn com.codenameone:codenameone-maven-plugin:$CN1VERSION:generate-app-project \
  -DsourceProject=/path/to/my/ProjectTemplate \
  -DgroupId=com.example \
  -DartifactId=myapp \
  -Dcn1Version=$CN1VERSION
// end::appendix-goal-generate-app-project-bash-001[]
