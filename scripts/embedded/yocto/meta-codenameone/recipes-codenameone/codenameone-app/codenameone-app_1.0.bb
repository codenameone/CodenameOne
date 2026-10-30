SUMMARY = "Example Codename One embedded application"
DESCRIPTION = "Replace SRC_URI with your own translated application: run \
scripts/embedded/translate.sh, tar the dist directory, and point at it."
LICENSE = "CLOSED"

SRC_URI = "file://dist.tar.gz"

CN1_APP = "EmbeddedDemo"
CN1_ENVIRONMENT = "CN1_ROTATION=0"

inherit codenameone-app
