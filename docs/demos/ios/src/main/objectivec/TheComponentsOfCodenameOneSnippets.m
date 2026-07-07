// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::the-components-of-codename-one-objective-c-001[]
// Inside an iOS native interface
NSUserDefaults* shared =
    [[NSUserDefaults alloc] initWithSuiteName:@"group.com.example.myapp.shared"];
NSDictionary* payload = [shared dictionaryForKey:@"cn1.shareExtension.payload"];
// end::the-components-of-codename-one-objective-c-001[]
