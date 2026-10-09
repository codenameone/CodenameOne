Release series evidence, October 9, 2026

Source baseline: ebe0e64be3b77e7b098a2e152c97549cc7b7c8ac (master, refreshed October 8).

Android demo
============
The browser bundle at /android-compatibility-demo/ is a real build of
scripts/android-compat-samples/gallery from that revision. Android source and
resources are unchanged; the manifest package was supplied during import.
The imported Maven project is android-gallery-source.zip. It uses local
8.0-SNAPSHOT artifacts; install that revision's Codename One toolchain first.
The Maven process used Java 8 and the source compiler was forked from JDK 17,
with source/target 8. No cloud build, account credentials or API keys were used.

From the extracted project root:
  mvn package -DskipTests -Dopen=false -Dmaven.compiler.fork=true \
    -Dmaven.compiler.executable="$JAVA17_HOME/bin/javac" \
    -Dcodename1.platform=javascript -Dcodename1.buildTarget=local-javascript
  mvn package -DskipTests -Dopen=false -Dmaven.compiler.fork=true \
    -Dmaven.compiler.executable="$JAVA17_HOME/bin/javac" \
    -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-source

The generated project is ios/target/app-ios-1.0-SNAPSHOT-ios-source/DroidApp.xcodeproj.
Compile with Xcode 27.0 / iPhoneOS 27.0, Release, generic iOS device, arm64:
  xcodebuild -project <project> -scheme DroidApp -configuration Release \
    -sdk iphoneos -destination generic/platform=iOS -derivedDataPath <output> \
    CODE_SIGNING_ALLOWED=NO DEPLOYMENT_POSTPROCESSING=YES \
    STRIP_INSTALLED_PRODUCT=YES STRIP_STYLE=all build

Measure the executable and sum regular file sizes in DroidApp.app. Compress
that directory using ZIP deflate level 9 for the separate compression measure.
android-ios-size.json records exact counts and hash. No physical iPhone run
or App Store packaging is implied. Debug symbols are excluded from the app.
The method/field removal counts are whole-application translator log counts.
The 862 compatibility translation units are com_codename1_androidcompat*.m.

GC charts
=========
The CSV comes from the committed PR #5940 calibration overlay at 6516c1ca47,
reproduced in gc-baseline-overlay.json. These are historical gate calibrations,
not new controlled experiments. Unchanged fields may have been carried forward.
Run render-charts.py with Python, matplotlib and numpy to recreate the charts.
The memory-budget drawing is explicitly conceptual, not measured data.

Playground
==========
Screenshots were captured from the public playground-app/index.html on October 8
in Chromium, 1440 x 920, using the exact article snippet. Its button updated
Orders to 4. Replacing the int with a String produced the shown constructor error.
Single-run timings are observations only, not a before/after performance claim.
The 23,827,851-byte source count is the sum of the 161 files under
scripts/cn1playground/common/src/main/java/bsh/cn1/gen at c582bf581a^.

Licensing
=========
Codename One sources and the gallery: repository LICENSE and NOTICE.
https://github.com/codenameone/CodenameOne/tree/ebe0e64be3
The source archive and demo include the repository license notices.
