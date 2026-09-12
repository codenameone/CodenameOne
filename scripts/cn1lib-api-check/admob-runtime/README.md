# AdMob banner measurement regression

With JDK 17, Android SDK 36, Gradle 8.13, and a running Android emulator:

```sh
gradle -p scripts/cn1lib-api-check/admob-runtime connectedDebugAndroidTest
```

Compiles the shipped native implementation against its declared Google SDKs.
Checks all five banner formats have their SDK pixel dimensions before CN1 wraps
or attaches the view, without requesting ads. Only the CN1 activity/thread bridge
and Java callback sink are replaced; Android and AdView are real.
