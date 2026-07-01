// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::deep-links-routing-sh-001[]
xcrun simctl openurl booted "https://example.com/users/42"
// end::deep-links-routing-sh-001[]

// tag::deep-links-routing-sh-002[]
adb shell am start -a android.intent.action.VIEW \
    -d "https://example.com/users/42" com.example.app
// end::deep-links-routing-sh-002[]
