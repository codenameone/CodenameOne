# tag::backend-zip-download[]
export CN1_BACKEND_DEMO=demo/zipdownload
export CN1_BACKEND_STANDALONE_DEMO=1
export JDK_8_HOME=/path/to/jdk8
export JAVA_HOME="$JDK_8_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
cd vm/backend
./build.sh ZipDownload com.demo /tmp/zipdownload
/tmp/zipdownload
# end::backend-zip-download[]
