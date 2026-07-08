// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::working-with-codenameone-sources-bash-001[]
$ git clone https://github.com/codenameone/CodenameOne
$ cd CodenameOne/maven
$ mvn install
// end::working-with-codenameone-sources-bash-001[]

// tag::working-with-codenameone-sources-bash-002[]
$ git clone https://github.com/shannah/cn1-maven-archetypes
$ cd cn1-maven-archetypes
$ mvn install
// end::working-with-codenameone-sources-bash-002[]

// tag::working-with-codenameone-sources-bash-003[]
$ mvn -pl core-unittests test
// end::working-with-codenameone-sources-bash-003[]

// tag::working-with-codenameone-sources-bash-004[]
$ mvn -pl tests -am verify
// end::working-with-codenameone-sources-bash-004[]
