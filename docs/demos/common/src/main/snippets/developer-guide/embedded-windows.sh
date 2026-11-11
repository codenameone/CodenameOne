// Generated from docs/developer-guide/Embedded-Windows.asciidoc source blocks. Edit the guide snippets here, not inline.

// tag::embedded-windows-properties-001[]
codename1.arg.windows.embedded=true
codename1.arg.windows.embedded.board=imx8
codename1.arg.windows.embedded.rotation=90
// end::embedded-windows-properties-001[]

// tag::embedded-windows-bash-001[]
mvn cn1:deploy-device -Dcn1.device.host=kiosk.local -Dcn1.device.user=Administrator
gradle cn1DeployDevice -Pcn1.device.host=kiosk.local -Pcn1.device.user=Administrator
// end::embedded-windows-bash-001[]

// tag::embedded-windows-bash-002[]
mvn cn1:deploy-device -Dcn1.device.host=kiosk.local -Dcn1.device.user=Administrator \
    -Dcn1.device.kioskUser=kiosk -Dcn1.device.keyboardFilter=true
// end::embedded-windows-bash-002[]
