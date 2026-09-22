DEFAULT_CN1_VERSON=$(bash $SCRIPTPATH/../print-version.sh)
if [ -z ${CN1_VERSION} ]; then
  CN1_VERSION=$DEFAULT_CN1_VERSON
fi

if [ ! -z ${SCRIPTPATH} ]; then
  if [ ! -d $SCRIPTPATH/build ]; then
    mkdir $SCRIPTPATH/build
  fi
fi

# The javaVersion to generate archetype projects with.
#
# The archetype defaults this to 17 (Flutter support needs it, and it matches
# start.codenameone.com). These suites build the project they generate with
# whatever JDK is running, and several legs deliberately run on JDK 8 -- so a
# generated project that targets 17 dies there with "invalid target release:
# 17", a failure about the generated project that reads like a broken
# archetype.
#
# Deriving it from the running JDK keeps each leg testing a combination that
# can actually exist: a JDK 8 leg generates and builds an 8 project, a modern
# leg generates and builds a 17 one. Override with CN1_ARCHETYPE_JAVA_VERSION
# to test a specific pairing.
if [ -z "${CN1_ARCHETYPE_JAVA_VERSION}" ]; then
  _cn1_jdk_major="$("${JAVA_HOME:+$JAVA_HOME/bin/}java" -version 2>&1 \
      | head -1 | sed -E 's/.*"([0-9]+)(\.([0-9]+))?.*/\1 \3/')"
  set -- ${_cn1_jdk_major}
  if [ "$1" = "1" ]; then
    _cn1_jdk_major="$2"
  else
    _cn1_jdk_major="$1"
  fi
  if [ -n "${_cn1_jdk_major}" ] && [ "${_cn1_jdk_major}" -ge 17 ] 2>/dev/null; then
    CN1_ARCHETYPE_JAVA_VERSION=17
  else
    CN1_ARCHETYPE_JAVA_VERSION=8
  fi
fi
export CN1_ARCHETYPE_JAVA_VERSION
