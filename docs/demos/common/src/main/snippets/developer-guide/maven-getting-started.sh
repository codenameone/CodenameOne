// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::maven-getting-started-bash-001[]
java -version
mvn -v
// end::maven-getting-started-bash-001[]

// tag::maven-getting-started-bash-002[]
mvn archetype:generate \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeVersion=LATEST \
  -DgroupId=YOUR_GROUP_ID \
  -DartifactId=YOUR_ARTIFACT_ID \
  -Dversion=1.0-SNAPSHOT \
  -DmainName=YOUR_MAIN_NAME \
  -DinteractiveMode=false
// end::maven-getting-started-bash-002[]

// tag::maven-getting-started-bash-003[]
mvn com.codenameone:codenameone-maven-plugin:{cn1-plugin-release-version}:generate-app-project \
  -DarchetypeGroupId=$archetypeGroupId \
  -DarchetypeArtifactId=$archetypeArtifactId \
  -DarchetypeVersion=$archetypeVersion \
  -DartifactId=$artifactId \
  -DgroupId=$groupId \
  -Dversion=$version \
  -DmainName=$mainName \
  -DinteractiveMode=false \
  -DsourceProject=/path/to/kotlin-example-app
// end::maven-getting-started-bash-003[]

// tag::maven-getting-started-bash-004[]
# Specify your the version of the codenameone-maven-plugin.
# Find the latest version at
# https://search.maven.org/search?q=a:codenameone-maven-plugin
CN1VERSION={cn1-plugin-release-version}
mvn com.codenameone:codenameone-maven-plugin:$CN1VERSION:generate-app-project \
  -DgroupId=YOUR_GROUP_ID \
  -DartifactId=YOUR_ARTIFACT_ID \
  -DsourceProject=/path/to/your/project \
  -Dcn1Version=$CN1VERSION
// end::maven-getting-started-bash-004[]

// tag::maven-getting-started-bash-005[]
cd myapp
./run.sh
// end::maven-getting-started-bash-005[]

// tag::maven-getting-started-bash-006[]
CN1_VERSION={cn1-plugin-release-version}
curl -L https://github.com/codenameone/KitchenSink/archive/v1.0-cn7.0.11.zip > master.zip
unzip master.zip
rm master.zip
mvn com.codenameone:codenameone-maven-plugin:${CN1_VERSION}:generate-app-project \
  -DarchetypeGroupId=com.codename1 \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeVersion=${CN1_VERSION} \
  -DartifactId=kitchensink \
  -DgroupId=com.example \
  -Dversion=1.0-SNAPSHOT \
  -DinteractiveMode=false \
  -DsourceProject=KitchenSink-1.0-cn7.0.11
// end::maven-getting-started-bash-006[]

// tag::maven-getting-started-bash-007[]
mvn cn1:install-cn1lib -Dfile=/path/to/yourlibrary.cn1lib
// end::maven-getting-started-bash-007[]
