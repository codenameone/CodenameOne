DEFAULT_CN1_VERSON=$(bash $SCRIPTPATH/../print-version.sh)
if [ -z ${CN1_VERSION} ]; then
  CN1_VERSION=$DEFAULT_CN1_VERSON
fi

if [ ! -z ${SCRIPTPATH} ]; then
  if [ ! -d $SCRIPTPATH/build ]; then
    mkdir $SCRIPTPATH/build
  fi
fi
