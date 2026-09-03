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

## whisper.cpp version

The checked-in AAR is built from whisper.cpp `v1.9.1-77-g0874de3e`:

```sh
git clone https://github.com/ggml-org/whisper.cpp.git
git -C whisper.cpp checkout 0874de3e
```

Record the new commit here whenever the AAR is rebuilt against a different one.
Nothing in the tree derives it, and the only reason it was recoverable the last
time was that the version and the abbreviated hash happened to survive as
strings inside `libcn1aiwhisper.so`.

## 16 KB memory pages

Google Play requires every **64-bit** shared library in an upload to be linked
for 16 KB memory pages -- for API 35+ uploads since 2025-11-01, and for Wear OS
uploads containing native code from 2026-09-15. In ELF terms every `PT_LOAD`
segment needs `p_align` of `0x4000` or more. See
<https://developer.android.com/guide/practices/page-sizes>.

This is decided at **link** time. AGP aligns the ZIP entries and bundletool
reports the outcome, but neither can move a segment inside a prebuilt `.so`, so
an AAR that gets this wrong cannot be repaired by the application that consumes
it -- and the failure is silent until Play review or a real 16 KB device,
because the app builds and runs on every 4 KB device.

Two things keep it right, and both are needed:

- `build.gradle` pins **NDK r28** (`28.2.13676358`). r28 is the first release
  that links this way by default, and, more to the point, the first whose
  prebuilt `libc++_shared.so` and `libomp.so` are aligned. Those two are copied
  into the AAR from the NDK itself, so no flag set in this project can fix them
  on r26/r27.
- `src/main/cpp/CMakeLists.txt` passes `-Wl,-z,max-page-size=16384` and
  `-Wl,-z,common-page-size=16384`, which covers `libcn1aiwhisper.so` if this
  project is ever driven by an older NDK or a standalone CMake invocation.

The library also declares `minSdk 21` now. That is not a consequence of the NDK
bump: the NDK's own floor has been API 21 since r26, so every slice this project
has ever shipped carries API 21 in its `.note.android.ident` while the AAR
manifest claimed 19. The old value was a claim the native code never honoured,
and an API 19 install would have failed at `System.loadLibrary`. Codename One's
default `android.min_sdk_version` is 19, so `PlatformFeatureCatalog` raises an
application's floor to 21 when it references `com.codename1.ai.whisper`;
without that the manifest merger rejects the build.

Verify a rebuild before committing it:

```sh
scripts/check-16k-page-alignment.py --verbose
```

It runs on every PR as `.github/workflows/check-16k-page-alignment.yml` and
covers every native artifact in the tree, not just this one. It finds
containers and libraries by magic bytes rather than by extension, and applies
the alignment rule only to 64-bit libraries under Android packaging -- a
desktop `.so` shipped through `nativelinux`/`nativese` is correct at 0x1000.
