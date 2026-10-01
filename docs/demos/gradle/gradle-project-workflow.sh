# Command snippets for the Gradle Project Workflow chapter of the developer guide.

# tag::gradle-convert[]
cd MyApp          # the Maven or Ant project
mvn com.codenameone:codenameone-maven-plugin:VERSION:convert-to-gradle
cd ../MyApp-gradle
./gradlew run
# end::gradle-convert[]

# tag::gradle-convert-options[]
mvn com.codenameone:codenameone-maven-plugin:VERSION:convert-to-gradle \
  -Dcn1.sourceProject=/path/to/MyApp \
  -Dcn1.outputDir=/path/to/MyApp-gradle \
  -Dcn1.includeBackend=true
# end::gradle-convert-options[]

# tag::gradle-generate-app-project[]
mvn com.codenameone:codenameone-maven-plugin:VERSION:generate-app-project \
  -DgroupId=com.example.myapp \
  -DartifactId=myapp \
  -Dversion=1.0-SNAPSHOT \
  -DmainName=MyApp \
  -DsourceProject=/path/to/AntProject \
  -Dcn1.buildTool=gradle \
  -DinteractiveMode=false
# end::gradle-generate-app-project[]

# tag::gradle-run[]
./gradlew run      # the simulator
./gradlew debug    # the simulator, waiting for a debugger on port 5005
./gradlew cn1Test  # the unit tests, in the simulator's test runner
# end::gradle-run[]

# tag::gradle-build-targets[]
./gradlew buildAndroid           # send an Android build to the build server
./gradlew buildIos               # send an iOS debug build
./gradlew buildIosXcodeProject   # generate an Xcode project locally
./gradlew buildJavascriptLocal   # build the JavaScript port locally
# end::gradle-build-targets[]

# tag::gradle-cn1build[]
./gradlew cn1Build -Pcodename1.platform=ios -Pcodename1.buildTarget=ios-source
./gradlew buildAndroid -Pcodename1.stageOnly=true
# end::gradle-cn1build[]

# tag::gradle-hints-cli[]
./gradlew buildIos -Pcodename1.arg.ios.newStorageLocation=true
# end::gradle-hints-cli[]

# tag::gradle-native-generate[]
./gradlew generateNativeInterfaces
# end::gradle-native-generate[]

# tag::gradle-native-options[]
./gradlew generateNativeInterfaces -Pcn1.nativeInterface=com.example.myapp.MyNative \
  -Pcn1.swift=true -Pcn1.kotlin=true
# end::gradle-native-options[]

# tag::gradle-cn1lib-publish[]
./gradlew publishToMavenLocal   # install into ~/.m2 for local testing
./gradlew publish               # publish to the repositories the build declares
# end::gradle-cn1lib-publish[]

# tag::gradle-backend[]
./gradlew addBackend                          # create backend/ once
CN1_PROFILE=dev ./gradlew :backend:runBackend  # run it on this JVM
./gradlew :backend:backendPackage              # build the native binary
# end::gradle-backend[]

# tag::gradle-backend-only[]
CN1_PROFILE=dev ./gradlew runBackend
./gradlew backendPackage
CN1_PROFILE=dev PORT=9000 ./build/MyService
# end::gradle-backend-only[]

# tag::gradle-tools[]
./gradlew settings            # Codename One Settings
./gradlew guibuilder          # the GUI Builder
./gradlew gameBuilder         # the Game Builder
./gradlew certificateWizard   # the iOS Certificate Wizard
# end::gradle-tools[]

# tag::gradle-update[]
./gradlew cn1Update
./gradlew cn1Update -Pcodename1.updateTo=VERSION
# end::gradle-update[]
