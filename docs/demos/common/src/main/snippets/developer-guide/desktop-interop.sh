// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::desktop-interop-bash-001[]
mvn cn1:import-desktop-project -Dcn1.desktop.import=/path/to/MyDesktopApp
// end::desktop-interop-bash-001[]

// tag::desktop-interop-bash-002[]
mvn cn1:import-desktop-project -Dcn1.desktop.import=/path/to/MyDesktopApp \
    -Dcn1.desktop.module=app \
    -Dcn1.desktop.mainClass=com.example.Main
// end::desktop-interop-bash-002[]

// tag::desktop-interop-bash-003[]
mvn cn1:run
mvn cn1:build -Dcodename1.platform=ios
// end::desktop-interop-bash-003[]

// tag::desktop-interop-bash-004[]
./gradlew run            # the simulator
./gradlew buildIos       # an iOS build on the build server
./gradlew buildLinuxDevice
// end::desktop-interop-bash-004[]

// tag::desktop-interop-bash-005[]
mvn cn1:run                                    # the simulator
mvn cn1:buildIos                               # iOS, on the build server
mvn cn1:buildAndroid                           # Android, on the build server
mvn cn1:buildJavascript                        # the JavaScript port
mvn package -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-source          # an Xcode project
mvn package -Dcodename1.platform=android -Dcodename1.buildTarget=android-source  # an Android Studio project
// end::desktop-interop-bash-005[]

// tag::desktop-interop-bash-006[]
# Linux: a native executable, built on a Linux machine
mvn package -Dcodename1.platform=linux -Dcodename1.buildTarget=local-linux-device

# macOS: a native .app, built on a Mac with Xcode
mvn package -Dcodename1.platform=ios -Dcodename1.buildTarget=local-mac-device

# Windows: a native .exe, built on Windows or cross-compiled
mvn package -Dcodename1.platform=win -Dcodename1.buildTarget=local-windows-device
// end::desktop-interop-bash-006[]

// tag::desktop-interop-bash-007[]
mvn package -Dcn1.desktop.report=port.md       # Maven
./gradlew build -Pcn1.desktop.report=port.md   # Gradle
// end::desktop-interop-bash-007[]

// tag::desktop-interop-bash-008[]
# From a checkout of the Codename One sources, against a built application:
xvfb-run -a maven/integration-tests/desktop-compat-realport/run.sh \
    /path/to/MyApp/common /path/to/orders.txt /path/to/output
// end::desktop-interop-bash-008[]

// tag::desktop-interop-bash-009[]
scripts/desktop-compat-benchmarks/bench.sh --app /path/to/MyDesktopApp --name myapp \
    --kind swing --main com.example.Main --work /path/to/work
scripts/desktop-compat-benchmarks/report.py /path/to/work/myapp/result.json
// end::desktop-interop-bash-009[]

// tag::desktop-interop-bash-010[]
scripts/desktop-compat-benchmarks/mobile-size.sh --target ios \
    --project /path/to/work/myapp/cn1/app --work /path/to/work/myapp-ios
scripts/desktop-compat-benchmarks/mobile-size.sh --target android \
    --project /path/to/work/myapp/cn1/app --work /path/to/work/myapp-android
// end::desktop-interop-bash-010[]
