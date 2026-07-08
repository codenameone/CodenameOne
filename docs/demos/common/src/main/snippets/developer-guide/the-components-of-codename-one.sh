// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::the-components-of-codename-one-bash-001[]
#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export LD_LIBRARY_PATH=$JAVA_HOME/lib:$JAVA_HOME/lib/server${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}
exec /path/to/your/ide "$@"
// end::the-components-of-codename-one-bash-001[]
