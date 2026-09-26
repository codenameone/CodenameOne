DEFAULT_CN1_VERSON=$(bash $SCRIPTPATH/../print-version.sh)
if [ -z ${CN1_VERSION} ]; then
  CN1_VERSION=$DEFAULT_CN1_VERSON
fi

if [ ! -z ${SCRIPTPATH} ]; then
  if [ ! -d $SCRIPTPATH/build ]; then
    mkdir $SCRIPTPATH/build
  fi
fi

# The javaVersion to generate archetype projects with, derived from the running
# JDK so each leg tests a pairing that can actually exist. See the helper for
# why this is not simply the archetype's own default.
if [ -z "${CN1_ARCHETYPE_JAVA_VERSION:-}" ]; then
  CN1_ARCHETYPE_JAVA_VERSION="$(bash "$SCRIPTPATH/../../scripts/ci/archetype-java-version.sh")"
fi
export CN1_ARCHETYPE_JAVA_VERSION
