// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::appendix-goal-generate-native-interfaces-bash-001[]
mvn cn1:generate-native-interfaces
// end::appendix-goal-generate-native-interfaces-bash-001[]

// tag::appendix-goal-generate-native-interfaces-bash-002[]
mvn cn1:generate-native-interfaces \
  -Dcn1.generateNativeInterfaces.swift=true \
  -Dcn1.generateNativeInterfaces.kotlin=true
// end::appendix-goal-generate-native-interfaces-bash-002[]
