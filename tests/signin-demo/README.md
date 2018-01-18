# Google Signin Tests

This test is adapted from the [SigninDemo](https://github.com/codenameone/codenameone-demos/tree/master/SignIn).  In addition it
adds FireBase support (see the native/ios/GoogleService-Info.plist) file.

The key to this test is to ensure that FireBase is able to start correctly.   Firebase support was added by generating the GoogleService-Info.plist file using
[these instructions](https://firebase.google.com/docs/ios/setup).   If the test begins to fail, then you may need to look at the 
native/ios/GoogleService-Info.plist file to make sure that it is valid.

