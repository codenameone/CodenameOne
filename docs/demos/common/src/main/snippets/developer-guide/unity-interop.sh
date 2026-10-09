// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::unity-interop-bash-001[]
mvn cn1:import-unity-project -Dsource=/path/to/MyUnityGame
// end::unity-interop-bash-001[]

// tag::unity-interop-bash-002[]
mvn cn1:run
mvn cn1:build -Dcodename1.platform=ios
// end::unity-interop-bash-002[]

// tag::unity-interop-bash-003[]
dotnet --list-sdks
mvn package -Dcn1.unity.dotnet=/opt/dotnet/dotnet
// end::unity-interop-bash-003[]

// tag::unity-interop-bash-004[]
./gradlew run
./gradlew buildIos -Pcn1.unity.dotnet=/opt/dotnet/dotnet
// end::unity-interop-bash-004[]

// tag::unity-interop-bash-005[]
mvn package
// end::unity-interop-bash-005[]

// tag::unity-interop-bash-006[]
mvn cn1:build -Dcodename1.platform=android
mvn cn1:build -Dcodename1.platform=ios
mvn cn1:build -Dcodename1.platform=javascript
// end::unity-interop-bash-006[]

// tag::unity-interop-bash-007[]
mvn package -Dcn1.unity.skip=true
// end::unity-interop-bash-007[]

// tag::unity-interop-bash-008[]
less common/target/unity/dotnet.log
less common/target/unity/translate.log
less common/target/unity/scene-compiler.log
// end::unity-interop-bash-008[]
