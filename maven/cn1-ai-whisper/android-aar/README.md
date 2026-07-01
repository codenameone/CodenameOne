# cn1-ai-whisper Android AAR

This project builds the JNI bridge used by the Android `NativeWhisperRecognizerImpl`.
It produces an AAR containing `libcn1aiwhisper.so` for Android ABIs.

Build from this directory with either a whisper.cpp checkout:

```sh
WHISPER_CPP_DIR=/path/to/whisper.cpp ./build-aar.sh
```

Or with prebuilt Android `libwhisper.so` slices:

```sh
WHISPER_PREBUILT_DIR=/path/to/jniLibs \
WHISPER_INCLUDE_DIR=/path/to/whisper.cpp/include \
./build-aar.sh
```

`WHISPER_PREBUILT_DIR` should contain ABI subdirectories such as
`arm64-v8a/libwhisper.so`. The build script copies the resulting AAR to
`../android/src/main/resources/cn1-ai-whisper-android.aar`, where the cn1lib
packager and Android builder already know how to pick it up.
