// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::on-device-debugging-android-bash-001[]
adb pair 192.168.1.42:37051
# When prompted, enter the 6-digit code shown on the device.
// end::on-device-debugging-android-bash-001[]

// tag::on-device-debugging-android-bash-002[]
adb connect 192.168.1.42:5555
// end::on-device-debugging-android-bash-002[]

// tag::on-device-debugging-android-bash-003[]
mvn cn1:android-on-device-debugging \
    -Dcn1.android.onDeviceDebug.wireless=192.168.1.42:5555
// end::on-device-debugging-android-bash-003[]

// tag::on-device-debugging-android-bash-004[]
adb tcpip 5555
adb connect 192.168.1.42:5555
// end::on-device-debugging-android-bash-004[]

// tag::on-device-debugging-android-bash-005[]
# Terminal 1 — build the APK once
mvn cn1:buildAndroidOnDeviceDebug

# Terminal 2 — install, launch, forward JDWP, tail logcat
mvn cn1:android-on-device-debugging
// end::on-device-debugging-android-bash-005[]

// tag::on-device-debugging-android-bash-006[]
jdb -attach localhost:5005 \
    -sourcepath src/main/java:$HOME/.m2/repository/com/codenameone/codenameone-core/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT-sources.jar
// end::on-device-debugging-android-bash-006[]
