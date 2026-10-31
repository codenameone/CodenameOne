// Generated from docs/developer-guide/Embedded-Linux.asciidoc source blocks. Edit the guide snippets here, not inline.

// tag::embedded-linux-bash-001[]
mvn cn1:deploy-device -Dcn1.device.host=pi.local -Dcn1.device.user=pi
gradle cn1DeployDevice -Pcn1.device.host=pi.local -Pcn1.device.user=pi
// end::embedded-linux-bash-001[]

// tag::embedded-linux-bash-002[]
mvn cn1:runOnDevice -Dcn1.board=rpi5 -Dcn1.device.host=pi.local -Dcn1.device.user=pi
gradle cn1RunOnDevice -Pcn1.board=rpi5 -Pcn1.device.host=pi.local -Pcn1.device.user=pi
// end::embedded-linux-bash-002[]
