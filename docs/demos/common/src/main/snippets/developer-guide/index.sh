// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::index-bash-001[]
mvn archetype:generate \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeVersion=LATEST \
  -DgroupId=YOUR_GROUP_ID \
  -DartifactId=YOUR_ARTIFACT_ID \
  -Dversion=1.0-SNAPSHOT \
  -DmainName=YOUR_MAIN_NAME \
  -DinteractiveMode=false
// end::index-bash-001[]

// tag::index-bash-002[]
mvn cn1:certificatewizard
// end::index-bash-002[]
