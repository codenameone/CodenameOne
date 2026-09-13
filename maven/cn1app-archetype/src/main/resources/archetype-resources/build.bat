@echo off
setlocal DisableDelayedExpansion
setlocal EnableExtensions



pushd "%~dp0" || exit /b 1
set "MVNW=mvnw.cmd"

SET "CMD=%~1"
if "%CMD%"=="" (
  echo No target selected: building a LOCAL JAR. For a cloud build, run .\build.bat javascript_cloud.
  set CMD=jar
)
goto %CMD%

goto :finish
:mac_desktop
call "%MVNW%" package -DskipTests -Dcodename1.platform^=javase -Dcodename1.buildTarget^=mac-os-x-desktop -U -e

goto :finish
:mac_native
call "%MVNW%" package -DskipTests -Dcodename1.platform^=ios -Dcodename1.buildTarget^=mac-os-x-native -U -e

goto :finish
:windows_desktop
call "%MVNW%" package -DskipTests -Dcodename1.platform^=javase -Dcodename1.buildTarget^=windows-desktop -U -e

goto :finish
:windows_device
call "%MVNW%" package -DskipTests -Dcodename1.platform^=win -Dcodename1.buildTarget^=windows-device -U -e

goto :finish
:linux_device
call "%MVNW%" package -DskipTests -Dcodename1.platform^=linux -Dcodename1.buildTarget^=linux-device -U -e

goto :finish
:javascript
call "%MVNW%" package -DskipTests -Dcodename1.platform^=javascript -Dcodename1.buildTarget^=local-javascript -U -e

goto :finish
:javascript_cloud
call "%MVNW%" package -DskipTests -Dcodename1.platform^=javascript -Dcodename1.buildTarget^=javascript -U -e

goto :finish
:android
call "%MVNW%" package -DskipTests -Dcodename1.platform^=android -Dcodename1.buildTarget^=android-device -U -e

goto :finish
:xcode
call "%MVNW%" package -DskipTests -Dcodename1.platform^=ios -Dcodename1.buildTarget^=ios-source -U -e

goto :finish
:ios_source
goto :xcode

:android_source
call "%MVNW%" package -DskipTests -Dcodename1.platform^=android -Dcodename1.buildTarget^=android-source -U -e

goto :finish
:ios
call "%MVNW%" package -DskipTests -Dcodename1.platform^=ios -Dcodename1.buildTarget^=ios-device -U -e

goto :finish
:ios_release
call "%MVNW%" package -DskipTests -Dcodename1.platform^=ios -Dcodename1.buildTarget^=ios-device-release -U -e

goto :finish
:jar
call "%MVNW%" -Pexecutable-jar package -Dcodename1.platform^=javase -DskipTests -U -e

goto :finish
:help
echo .\build.bat [COMMAND]
echo Local Build Commands:
echo   The following commands will build the app locally ^(i.e. does NOT use the Codename One build server^)
echo 
echo   jar
echo     Builds app as desktop app executable jar file to javase/target directory
echo   android_source
echo     Generates an android gradle project that can be opened in Android studio
echo     *Requires android development tools installed.
echo     *Requires ANDROID_HOME environment variable
echo     *Requires either GRADLE_HOME environment variable^, or for gradle to be in PATH
echo   ios_source
echo     Generates an Xcode Project that you can open and build using Apple^'s development tools
echo     *Requires a Mac with Xcode installed
echo   javascript
echo     Builds the web app locally.
echo 
echo Build Server Commands:
echo   The following commands will build the app using the Codename One build server^, and require
echo   a Codename One account.  See https://www.codenameone.com
echo 
echo   ios
echo     Builds iOS app.
echo   ios_release
echo     Builds iOS app for submission to Apple appstore.
echo   android
echo     Builds android app.
echo   mac_desktop
echo     Builds Mac OS desktop app.
echo     *Mac OS Desktop builds are a Pro user feature.
echo   mac_native
echo     Builds a native Mac app ^(no JVM^).
echo   windows_desktop
echo     Builds Windows desktop app.
echo     *Windows Desktop builds are a Pro user feature.
echo   windows_device
echo     Builds a native Windows app ^(no JVM^).
echo   linux_device
echo     Builds a native Linux app ^(ELF^, no JVM^).
echo   javascript_cloud
echo     Builds the web app using the build server.

goto :finish
:settings
call "%MVNW%" cn1:settings -U -e

:finish
set "CN1_EXIT_CODE=%errorlevel%"
popd
exit /b %CN1_EXIT_CODE%
