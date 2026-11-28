// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::device-runtime-bash-001[]
mvn compile cn1:push
mvn compile cn1:push -Dcn1.push.main=com.example.Other
// end::device-runtime-bash-001[]

// tag::device-runtime-push[]
mvn compile cn1:push                              # the iOS simulator, or a phone over USB
mvn compile cn1:push -Dcn1.push.lan               # a phone or tablet on the same Wi-Fi
mvn compile cn1:push -Dcn1.push.device=192.168.1.50
mvn cn1:push-stop -Dcn1.push.lan                  # stop what the device is running

# Gradle: the same properties, passed with -P
./gradlew cn1Push
./gradlew cn1Push -Pcn1.push.lan
./gradlew cn1PushStop -Pcn1.push.lan
// end::device-runtime-push[]
